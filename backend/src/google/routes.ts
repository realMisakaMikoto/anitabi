import type {
  Coordinate,
  MatrixRequest,
  NormalizedMatrix,
  NormalizedMatrixElement,
  NormalizedRoute,
  NormalizedRouteLeg,
  NormalizedRouteStep,
  NormalizedTransitDetails,
  RouteRequest,
  TravelMode,
} from "../contract.js";
import { ApiError } from "../errors.js";
import type { OAuthTokenProvider } from "./oauth.js";

export const GOOGLE_MATRIX_URL =
  "https://routes.googleapis.com/distanceMatrix/v2:computeRouteMatrix";
export const GOOGLE_ROUTE_URL = "https://routes.googleapis.com/directions/v2:computeRoutes";

const MATRIX_FIELD_MASK =
  "originIndex,destinationIndex,status,condition,distanceMeters,duration";
const ROUTE_FIELD_MASK = [
  "routes.distanceMeters",
  "routes.duration",
  "routes.polyline.encodedPolyline",
  "routes.legs.distanceMeters",
  "routes.legs.duration",
  "routes.legs.polyline.encodedPolyline",
  "routes.legs.steps.distanceMeters",
  "routes.legs.steps.staticDuration",
  "routes.legs.steps.polyline.encodedPolyline",
  "routes.legs.steps.navigationInstruction.instructions",
  "routes.legs.steps.navigationInstruction.maneuver",
  "routes.legs.steps.travelMode",
  "routes.legs.steps.transitDetails.stopDetails",
  "routes.legs.steps.transitDetails.localizedValues.departureTime.timeZone",
  "routes.legs.steps.transitDetails.localizedValues.arrivalTime.timeZone",
  "routes.legs.steps.transitDetails.transitLine.name",
  "routes.legs.steps.transitDetails.transitLine.nameShort",
  "routes.legs.steps.transitDetails.transitLine.vehicle.name.text",
  "routes.legs.steps.transitDetails.transitLine.vehicle.type",
  "routes.legs.steps.transitDetails.headsign",
  "routes.legs.steps.transitDetails.stopCount",
].join(",");

export interface RoutesProvider {
  matrix(request: MatrixRequest): Promise<NormalizedMatrix>;
  route(request: RouteRequest): Promise<NormalizedRoute>;
}

export type GoogleRoutesClientOptions = Readonly<{
  projectId: string;
  oauth: OAuthTokenProvider;
  fetch?: typeof fetch;
  timeoutMillis?: number;
}>;

export class GoogleRoutesClient implements RoutesProvider {
  private readonly fetchImplementation: typeof fetch;
  private readonly timeoutMillis: number;

  constructor(private readonly options: GoogleRoutesClientOptions) {
    if (!options.projectId || options.projectId.length > 128) {
      throw new Error("A Google Cloud project ID is required");
    }
    this.fetchImplementation = options.fetch ?? fetch;
    this.timeoutMillis = options.timeoutMillis ?? 8_000;
  }

  async matrix(request: MatrixRequest): Promise<NormalizedMatrix> {
    const locations = request.coordinates.map(toGoogleWaypoint);
    const body: Record<string, unknown> = {
      origins: locations.map((waypoint) => ({ waypoint })),
      destinations: locations.map((waypoint) => ({ waypoint })),
      travelMode: request.mode,
      languageCode: "zh-CN",
      units: "METRIC",
    };
    if (request.mode === "DRIVE") body["routingPreference"] = "TRAFFIC_UNAWARE";

    const payload = await this.postJson(GOOGLE_MATRIX_URL, MATRIX_FIELD_MASK, body);
    if (!Array.isArray(payload)) throw new ApiError("UPSTREAM_UNAVAILABLE");
    const elements = payload.map(normalizeMatrixElement);
    if (!elements.some((element) => element.status === "OK" && element.originIndex !== element.destinationIndex)) {
      throw new ApiError("NO_ROUTE");
    }
    return { elements };
  }

  async route(request: RouteRequest): Promise<NormalizedRoute> {
    const [origin, ...tail] = request.locations;
    const destination = tail.at(-1);
    if (origin === undefined || destination === undefined) throw new ApiError("INVALID_ARGUMENT");
    const intermediates = tail.slice(0, -1);
    const body: Record<string, unknown> = {
      origin: toGoogleWaypoint(origin),
      destination: toGoogleWaypoint(destination),
      travelMode: request.mode,
      computeAlternativeRoutes: false,
      languageCode: "zh-CN",
      units: "METRIC",
    };
    if (request.mode !== "TRANSIT") {
      body["intermediates"] = intermediates.map(toGoogleWaypoint);
    }
    if (request.mode === "DRIVE") {
      body["routingPreference"] = "TRAFFIC_UNAWARE";
      body["routeModifiers"] = fixedRouteModifiers();
    }
    if (request.mode === "TRANSIT") {
      if (request.departureTime !== undefined) body["departureTime"] = request.departureTime;
      if (request.arrivalTime !== undefined) body["arrivalTime"] = request.arrivalTime;
      const transitPreferences: Record<string, unknown> = {};
      if (request.transitRoutingPreference !== undefined) {
        transitPreferences["routingPreference"] = request.transitRoutingPreference;
      }
      if (request.transitTravelModes !== undefined) {
        transitPreferences["allowedTravelModes"] = request.transitTravelModes;
      }
      if (Object.keys(transitPreferences).length > 0) {
        body["transitPreferences"] = transitPreferences;
      }
    }

    const payload = await this.postJson(GOOGLE_ROUTE_URL, ROUTE_FIELD_MASK, body);
    const routes = getRecord(payload)["routes"];
    // ProtoJSON omits empty repeated fields. Google therefore represents some
    // successful no-route responses as an empty object instead of routes: [].
    if (routes === undefined) throw new ApiError("NO_ROUTE");
    if (!Array.isArray(routes)) throw new ApiError("UPSTREAM_UNAVAILABLE");
    if (routes.length === 0) throw new ApiError("NO_ROUTE");
    return normalizeRoute(routes[0]);
  }

  private async postJson(url: string, fieldMask: string, body: object): Promise<unknown> {
    const accessToken = await this.options.oauth.getAccessToken();
    let response: Response;
    try {
      response = await this.fetchImplementation(url, {
        method: "POST",
        headers: {
          authorization: `Bearer ${accessToken}`,
          "content-type": "application/json",
          "x-goog-fieldmask": fieldMask,
          "x-goog-user-project": this.options.projectId,
        },
        body: JSON.stringify(body),
        signal: AbortSignal.timeout(this.timeoutMillis),
      });
    } catch (error) {
      throw new ApiError("UPSTREAM_UNAVAILABLE", { cause: error });
    }
    if (!response.ok) throw new ApiError("UPSTREAM_UNAVAILABLE");
    try {
      return await response.json();
    } catch (error) {
      throw new ApiError("UPSTREAM_UNAVAILABLE", { cause: error });
    }
  }
}

function toGoogleWaypoint(coordinate: Coordinate): object {
  return {
    location: {
      latLng: {
        latitude: coordinate.latitude,
        longitude: coordinate.longitude,
      },
    },
  };
}

function fixedRouteModifiers(): object {
  return {
    avoidTolls: false,
    avoidHighways: false,
    avoidFerries: false,
  };
}

function normalizeMatrixElement(value: unknown): NormalizedMatrixElement {
  const record = getRecord(value);
  const originIndex = requireNonNegativeInteger(record["originIndex"]);
  const destinationIndex = requireNonNegativeInteger(record["destinationIndex"]);
  const condition = record["condition"];
  const status = condition === "ROUTE_EXISTS" ? "OK" : "UNREACHABLE";
  if (status === "UNREACHABLE") return { originIndex, destinationIndex, status };
  return {
    originIndex,
    destinationIndex,
    status,
    // Protobuf JSON omits numeric fields at their default value. A matrix
    // diagonal therefore has no distanceMeters field and represents 0 m.
    distanceMeters: optionalNonNegativeNumber(record["distanceMeters"]) ?? 0,
    durationSeconds: parseDuration(record["duration"]),
  };
}

function normalizeRoute(value: unknown): NormalizedRoute {
  const record = getRecord(value);
  const legsValue = record["legs"];
  if (!Array.isArray(legsValue) || legsValue.length === 0) throw new ApiError("UPSTREAM_UNAVAILABLE");
  const legs = legsValue.map(normalizeLeg);
  return withOptionalPolyline(
    {
      distanceMeters: nonNegativeNumberOrZero(record["distanceMeters"]),
      durationSeconds: durationOrZero(record["duration"]),
      legs,
    },
    record["polyline"],
  );
}

function normalizeLeg(value: unknown): NormalizedRouteLeg {
  const record = getRecord(value);
  const stepsValue = record["steps"];
  const steps = Array.isArray(stepsValue) ? stepsValue.map(normalizeStep) : [];
  return withOptionalPolyline(
    {
      distanceMeters: nonNegativeNumberOrZero(record["distanceMeters"]),
      durationSeconds: durationOrZero(record["duration"]),
      steps,
    },
    record["polyline"],
  );
}

function normalizeStep(value: unknown): NormalizedRouteStep {
  const record = getRecord(value);
  const travelMode = normalizeTravelMode(record["travelMode"]);
  const base: NormalizedRouteStep = {
    travelMode,
    distanceMeters: optionalNonNegativeNumber(record["distanceMeters"]) ?? 0,
    durationSeconds: optionalDuration(record["staticDuration"]) ?? 0,
  };
  const navigation = getOptionalRecord(record["navigationInstruction"]);
  const transit = getOptionalRecord(record["transitDetails"]);
  const instruction = optionalString(navigation?.["instructions"]);
  const maneuver = optionalString(navigation?.["maneuver"]);
  return {
    ...withOptionalPolyline(base, record["polyline"]),
    ...(instruction === undefined ? {} : { instruction }),
    ...(maneuver === undefined ? {} : { maneuver }),
    ...(transit === undefined ? {} : { transit: normalizeTransit(transit) }),
  };
}

function normalizeTransit(record: Record<string, unknown>): NormalizedTransitDetails {
  const stops = getOptionalRecord(record["stopDetails"]);
  const departureStop = getOptionalRecord(stops?.["departureStop"]);
  const arrivalStop = getOptionalRecord(stops?.["arrivalStop"]);
  const line = getOptionalRecord(record["transitLine"]);
  const vehicle = getOptionalRecord(line?.["vehicle"]);
  const vehicleName = getOptionalRecord(vehicle?.["name"]);
  const localizedValues = getOptionalRecord(record["localizedValues"]);
  const localizedDepartureTime = getOptionalRecord(localizedValues?.["departureTime"]);
  const localizedArrivalTime = getOptionalRecord(localizedValues?.["arrivalTime"]);
  const values = {
    departureStop: optionalString(departureStop?.["name"]),
    arrivalStop: optionalString(arrivalStop?.["name"]),
    departureTime: optionalString(stops?.["departureTime"]),
    arrivalTime: optionalString(stops?.["arrivalTime"]),
    departureTimeZone: optionalString(localizedDepartureTime?.["timeZone"]),
    arrivalTimeZone: optionalString(localizedArrivalTime?.["timeZone"]),
    lineName: optionalString(line?.["name"]),
    lineShortName: optionalString(line?.["nameShort"]),
    headsign: optionalString(record["headsign"]),
    vehicleName: optionalString(vehicleName?.["text"]),
    vehicleType: optionalString(vehicle?.["type"]),
    stopCount: optionalNonNegativeInteger(record["stopCount"]),
  };
  return Object.fromEntries(
    Object.entries(values).filter(([, entry]) => entry !== undefined),
  ) as NormalizedTransitDetails;
}

function withOptionalPolyline<T extends object>(base: T, value: unknown): T & { encodedPolyline?: string } {
  const polyline = getOptionalRecord(value);
  const encodedPolyline = optionalString(polyline?.["encodedPolyline"]);
  return (encodedPolyline === undefined ? base : { ...base, encodedPolyline }) as T & {
    encodedPolyline?: string;
  };
}

function normalizeTravelMode(value: unknown): TravelMode {
  if (value === "DRIVE" || value === "BICYCLE" || value === "WALK" || value === "TRANSIT") {
    return value;
  }
  throw new ApiError("UPSTREAM_UNAVAILABLE");
}

function parseDuration(value: unknown): number {
  const parsed = optionalDuration(value);
  if (parsed === undefined) throw new ApiError("UPSTREAM_UNAVAILABLE");
  return parsed;
}

function optionalDuration(value: unknown): number | undefined {
  if (typeof value !== "string" || !/^\d+(?:\.\d+)?s$/.test(value)) return undefined;
  const parsed = Number(value.slice(0, -1));
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : undefined;
}

function durationOrZero(value: unknown): number {
  return value === undefined ? 0 : parseDuration(value);
}

function nonNegativeNumberOrZero(value: unknown): number {
  return value === undefined ? 0 : requireNonNegativeNumber(value);
}

function requireNonNegativeNumber(value: unknown): number {
  const parsed = optionalNonNegativeNumber(value);
  if (parsed === undefined) throw new ApiError("UPSTREAM_UNAVAILABLE");
  return parsed;
}

function optionalNonNegativeNumber(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value) && value >= 0 ? value : undefined;
}

function requireNonNegativeInteger(value: unknown): number {
  const parsed = optionalNonNegativeInteger(value);
  if (parsed === undefined) throw new ApiError("UPSTREAM_UNAVAILABLE");
  return parsed;
}

function optionalNonNegativeInteger(value: unknown): number | undefined {
  return Number.isSafeInteger(value) && (value as number) >= 0 ? (value as number) : undefined;
}

function optionalString(value: unknown): string | undefined {
  return typeof value === "string" && value.length > 0 ? value : undefined;
}

function getRecord(value: unknown): Record<string, unknown> {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    throw new ApiError("UPSTREAM_UNAVAILABLE");
  }
  return value as Record<string, unknown>;
}

function getOptionalRecord(value: unknown): Record<string, unknown> | undefined {
  return typeof value === "object" && value !== null && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : undefined;
}
