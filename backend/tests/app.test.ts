import assert from "node:assert/strict";
import { test } from "node:test";
import { buildApp } from "../src/app.js";
import type { FirebaseTokenVerifier } from "../src/auth/firebase.js";
import type { MatrixRequest, RouteRequest } from "../src/contract.js";
import { ApiError } from "../src/errors.js";
import type { RoutesProvider } from "../src/google/routes.js";
import type { SafeLogEvent } from "../src/logging.js";
import type {
  LedgerHealth,
  QuotaLedger,
  QuotaReservation,
  QuotaReservationResult,
} from "../src/quota/ledger.js";
import { TokenBucketLimiter } from "../src/rate-limit.js";

test("POST endpoints require authentication, JSON, valid bounds, and transit pairs", async () => {
  const fixture = createApp();
  try {
    const unauthenticated = await fixture.app.inject({
      method: "POST",
      url: "/v1/matrix",
      payload: validMatrix(),
    });
    assert.equal(unauthenticated.statusCode, 401);
    assert.equal(unauthenticated.json().error.code, "UNAUTHENTICATED");

    const invalidCoordinate = await fixture.app.inject(authenticated({
      method: "POST",
      url: "/v1/matrix",
      payload: { ...validMatrix(), coordinates: [{ latitude: 91, longitude: 0 }, { latitude: 0, longitude: 0 }] },
    }));
    assert.equal(invalidCoordinate.statusCode, 400);
    assert.equal(invalidCoordinate.json().error.code, "INVALID_ARGUMENT");

    const tooManyTransitLocations = await fixture.app.inject(authenticated({
      method: "POST",
      url: "/v1/route",
      payload: {
        mode: "TRANSIT",
        locations: [
          { latitude: 35, longitude: 139 },
          { latitude: 35.1, longitude: 139.1 },
          { latitude: 35.2, longitude: 139.2 },
        ],
      },
    }));
    assert.equal(tooManyTransitLocations.statusCode, 400);
    assert.equal(tooManyTransitLocations.json().error.code, "INVALID_ARGUMENT");

    const nonJson = await fixture.app.inject({
      method: "POST",
      url: "/v1/navigation/reserve",
      headers: { authorization: "Bearer valid-token", "content-type": "text/plain" },
      payload: "plain text",
    });
    assert.equal(nonJson.statusCode, 400);
    assert.equal(nonJson.json().error.code, "INVALID_ARGUMENT");
  } finally {
    await fixture.app.close();
  }
});

test("matrix reserves billable elements and navigation reserves destinations", async () => {
  const fixture = createApp();
  try {
    const matrix = await fixture.app.inject(authenticated({
      method: "POST",
      url: "/v1/matrix",
      payload: validMatrix(),
    }));
    assert.equal(matrix.statusCode, 200);
    assert.deepEqual(fixture.ledger.reservations[0], {
      bucket: "matrix",
      units: 4,
      uid: "anonymous-uid",
    });

    const navigation = await fixture.app.inject(authenticated({
      method: "POST",
      url: "/v1/navigation/reserve",
      payload: { destinationCount: 25 },
    }));
    assert.equal(navigation.statusCode, 200);
    assert.deepEqual(navigation.json(), { reservedDestinations: 25, remainingToday: 75 });
  } finally {
    await fixture.app.close();
  }
});

test("health exposes only service/database state and logs never contain sensitive request data", async () => {
  const fixture = createApp();
  try {
    const secretToken = "private-token-value";
    const coordinate = "35.123456";
    const response = await fixture.app.inject({
      method: "POST",
      url: "/v1/matrix",
      remoteAddress: "192.0.2.123",
      headers: { authorization: `Bearer ${secretToken}`, "content-type": "application/json" },
      payload: {
        mode: "DRIVE",
        coordinates: [
          { latitude: Number(coordinate), longitude: 139 },
          { latitude: 35.2, longitude: 139.2 },
        ],
        objective: "FASTEST",
      },
    });
    assert.equal(response.statusCode, 200);

    const health = await fixture.app.inject({ method: "GET", url: "/v1/health" });
    assert.deepEqual(health.json(), { service: "ok", database: "ok" });
    const logs = JSON.stringify(fixture.logs);
    assert.equal(logs.includes(secretToken), false);
    assert.equal(logs.includes("192.0.2.123"), false);
    assert.equal(logs.includes(coordinate), false);
  } finally {
    await fixture.app.close();
  }
});

test("quota uncertainty fails closed before the Google provider is called", async () => {
  const fixture = createApp();
  fixture.ledger.reserveError = new ApiError("BACKEND_UNAVAILABLE");
  try {
    const response = await fixture.app.inject(authenticated({
      method: "POST",
      url: "/v1/route",
      payload: {
        mode: "WALK",
        locations: [
          { latitude: 35, longitude: 139 },
          { latitude: 35.1, longitude: 139.1 },
        ],
      },
    }));
    assert.equal(response.statusCode, 503);
    assert.equal(response.json().error.code, "BACKEND_UNAVAILABLE");
    assert.equal(fixture.routes.routeCalls, 0);
  } finally {
    await fixture.app.close();
  }
});

function authenticated(options: {
  method: "POST";
  url: string;
  payload: unknown;
}): typeof options & { headers: Record<string, string> } {
  return {
    ...options,
    headers: { authorization: "Bearer valid-token", "content-type": "application/json" },
  };
}

function validMatrix(): MatrixRequest {
  return {
    mode: "DRIVE",
    coordinates: [
      { latitude: 35, longitude: 139 },
      { latitude: 35.1, longitude: 139.1 },
    ],
    objective: "FASTEST",
  };
}

function createApp(): {
  app: ReturnType<typeof buildApp>;
  ledger: FakeLedger;
  routes: FakeRoutes;
  logs: SafeLogEvent[];
} {
  const ledger = new FakeLedger();
  const routes = new FakeRoutes();
  const logs: SafeLogEvent[] = [];
  const auth: FirebaseTokenVerifier = {
    verify: async (token) => {
      if (token !== "valid-token" && token !== "private-token-value") {
        throw new ApiError("UNAUTHENTICATED");
      }
      return "anonymous-uid";
    },
  };
  return {
    ledger,
    routes,
    logs,
    app: buildApp({
      auth,
      routes,
      quota: ledger,
      rateLimiter: new TokenBucketLimiter({
        uidCapacity: 100,
        uidRefillPerSecond: 100,
        ipCapacity: 100,
        ipRefillPerSecond: 100,
        ipHmacKey: new Uint8Array(32).fill(3),
      }),
      logger: { write: (event) => logs.push(event) },
      allowInsecureForTests: true,
    }),
  };
}

class FakeLedger implements QuotaLedger {
  readonly reservations: QuotaReservation[] = [];
  reserveError?: ApiError;

  reserve(reservation: QuotaReservation): QuotaReservationResult {
    if (this.reserveError !== undefined) throw this.reserveError;
    this.reservations.push(reservation);
    return { monthlyUsed: reservation.units, monthlyRemaining: 100, dailyUsed: reservation.units, dailyRemaining: 100 - reservation.units };
  }

  health(): LedgerHealth {
    return { healthy: true, billingEnabled: true };
  }

  close(): void {}
}

class FakeRoutes implements RoutesProvider {
  routeCalls = 0;

  async matrix(_request: MatrixRequest) {
    return { elements: [] };
  }

  async route(_request: RouteRequest) {
    this.routeCalls += 1;
    return { distanceMeters: 1, durationSeconds: 1, legs: [] };
  }
}
