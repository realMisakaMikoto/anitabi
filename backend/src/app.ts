import Fastify, { type FastifyInstance, type FastifyRequest } from "fastify";
import type {
  MatrixRequest,
  NavigationReservationRequest,
  RouteRequest,
} from "./contract.js";
import { ApiError, errorBody } from "./errors.js";
import type { FirebaseTokenVerifier } from "./auth/firebase.js";
import type { RoutesProvider } from "./google/routes.js";
import { latencyBucket, type SafeLogger } from "./logging.js";
import type { QuotaLedger } from "./quota/ledger.js";
import { TokenBucketLimiter } from "./rate-limit.js";
import {
  matrixBodySchema,
  navigationReservationBodySchema,
  routeBodySchema,
} from "./schemas.js";

export type AppDependencies = Readonly<{
  auth: FirebaseTokenVerifier;
  routes: RoutesProvider;
  quota: QuotaLedger;
  rateLimiter: TokenBucketLimiter;
  logger: SafeLogger;
  allowInsecureForTests?: boolean;
  nowMillis?: () => number;
}>;

export function buildApp(dependencies: AppDependencies): FastifyInstance {
  const app = Fastify({
    logger: false,
    bodyLimit: 16 * 1_024,
    // The container port is published on host loopback only. The configured
    // reverse proxy overwrites forwarded headers, so the container trusts that hop.
    trustProxy: true,
  });
  const requestStartedAt = new WeakMap<FastifyRequest, number>();
  const requestErrorCode = new WeakMap<FastifyRequest, string>();

  app.addHook("onRequest", async (request) => {
    requestStartedAt.set(request, performance.now());
  });

  app.addHook("preHandler", async (request) => {
    if (request.method !== "POST") return;
    if (!dependencies.allowInsecureForTests && request.protocol !== "https") {
      throw new ApiError("INVALID_ARGUMENT");
    }
    if (!isJsonContentType(request.headers["content-type"])) {
      throw new ApiError("INVALID_ARGUMENT");
    }

    const authorization = request.headers.authorization;
    if (typeof authorization !== "string" || !authorization.startsWith("Bearer ")) {
      throw new ApiError("UNAUTHENTICATED");
    }
    const token = authorization.slice("Bearer ".length);
    if (token.length === 0 || token.length > 8_192) throw new ApiError("UNAUTHENTICATED");
    await dependencies.auth.verify(token);
    if (!dependencies.rateLimiter.consume(request.ip)) {
      throw new ApiError("RATE_LIMITED", { retryAfterSeconds: 1 });
    }
  });

  app.get("/v1/health", async (_request, reply) => {
    const health = dependencies.quota.health();
    const available = health.healthy && health.billingEnabled;
    return reply.status(available ? 200 : 503).send({
      service: available ? "ok" : "unavailable",
      database: health.healthy ? "ok" : "unavailable",
    });
  });

  app.post<{ Body: MatrixRequest }>(
    "/v1/matrix",
    { schema: { body: matrixBodySchema } },
    async (request) => {
      const elementCount = request.body.coordinates.length ** 2;
      dependencies.quota.reserve({ bucket: "matrix", units: elementCount });
      return dependencies.routes.matrix(request.body);
    },
  );

  app.post<{ Body: RouteRequest }>(
    "/v1/route",
    { schema: { body: routeBodySchema } },
    async (request) => {
      if (request.body.mode === "TRANSIT" && request.body.locations.length !== 2) {
        throw new ApiError("INVALID_ARGUMENT");
      }
      validateTransitTimeWindow(request.body, dependencies.nowMillis?.() ?? Date.now());
      dependencies.quota.reserve({ bucket: "route", units: 1 });
      return dependencies.routes.route(request.body);
    },
  );

  app.post<{ Body: NavigationReservationRequest }>(
    "/v1/navigation/reserve",
    { schema: { body: navigationReservationBodySchema } },
    async (request) => {
      dependencies.quota.reserve({
        bucket: "navigation",
        units: request.body.destinationCount,
      });
      return {
        reservedDestinations: request.body.destinationCount,
        // Public v0.2.3 clients require this legacy field while deserializing.
        remainingToday: LEGACY_UNBOUNDED_REMAINING_TODAY,
      };
    },
  );

  app.setErrorHandler((error, request, reply) => {
    const apiError = normalizeError(error);
    requestErrorCode.set(request, apiError.code);
    if (apiError.retryAfterSeconds !== undefined) {
      reply.header("Retry-After", String(apiError.retryAfterSeconds));
    }
    void reply.status(apiError.statusCode).send(errorBody(apiError));
  });

  app.addHook("onResponse", async (request, reply) => {
    const start = requestStartedAt.get(request) ?? performance.now();
    const errorCode = requestErrorCode.get(request);
    dependencies.logger.write({
      level: reply.statusCode >= 500 ? "error" : reply.statusCode >= 400 ? "warn" : "info",
      event: "request_complete",
      endpoint: routeTemplate(request),
      statusCode: reply.statusCode,
      latencyBucket: latencyBucket(performance.now() - start),
      ...(errorCode === undefined ? {} : { errorCode }),
    });
  });

  app.addHook("onClose", async () => {
    dependencies.quota.close();
  });

  return app;
}

const DAY_MILLIS = 24 * 60 * 60 * 1_000;
const LEGACY_UNBOUNDED_REMAINING_TODAY = 2_147_483_647;

function validateTransitTimeWindow(request: RouteRequest, nowMillis: number): void {
  if (request.mode !== "TRANSIT") return;
  const value = request.departureTime ?? request.arrivalTime;
  if (value === undefined) return;
  const timeMillis = Date.parse(value);
  if (
    !Number.isFinite(timeMillis) ||
    timeMillis < nowMillis - 7 * DAY_MILLIS ||
    timeMillis > nowMillis + 100 * DAY_MILLIS
  ) {
    throw new ApiError("INVALID_ARGUMENT");
  }
}

function normalizeError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;
  if (
    typeof error === "object" &&
    error !== null &&
    ("validation" in error || ("code" in error && error.code === "FST_ERR_CTP_BODY_TOO_LARGE"))
  ) {
    return new ApiError("INVALID_ARGUMENT", { cause: error });
  }
  return new ApiError("BACKEND_UNAVAILABLE", { cause: error });
}

function isJsonContentType(value: string | undefined): boolean {
  return value?.split(";", 1)[0]?.trim().toLowerCase() === "application/json";
}

function routeTemplate(request: FastifyRequest): string {
  return request.routeOptions.url ?? "unknown";
}
