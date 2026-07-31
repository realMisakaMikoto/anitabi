import assert from "node:assert/strict";
import { test } from "node:test";
import type { OAuthTokenProvider } from "../src/google/oauth.js";
import {
  GOOGLE_MATRIX_URL,
  GOOGLE_ROUTE_URL,
  GoogleRoutesClient,
} from "../src/google/routes.js";

const oauth: OAuthTokenProvider = { getAccessToken: async () => "test-access-token" };

test("matrix client uses only the fixed upstream and returns normalized reachable states", async () => {
  let capturedBody: Record<string, unknown> | undefined;
  const client = new GoogleRoutesClient({
    projectId: "anitabi-test",
    oauth,
    fetch: async (input, init) => {
      assert.equal(input, GOOGLE_MATRIX_URL);
      assert.equal(init?.method, "POST");
      const headers = new Headers(init?.headers);
      assert.equal(headers.get("authorization"), "Bearer test-access-token");
      assert.equal(headers.get("x-goog-user-project"), "anitabi-test");
      assert.equal(
        headers.get("x-goog-fieldmask"),
        "originIndex,destinationIndex,status,condition,distanceMeters,duration",
      );
      capturedBody = JSON.parse(init?.body as string) as Record<string, unknown>;
      return Response.json([
        {
          originIndex: 0,
          destinationIndex: 0,
          condition: "ROUTE_EXISTS",
          duration: "0s",
        },
        {
          originIndex: 0,
          destinationIndex: 1,
          condition: "ROUTE_EXISTS",
          distanceMeters: 1234,
          duration: "456.5s",
        },
        { originIndex: 1, destinationIndex: 0, condition: "ROUTE_NOT_FOUND" },
      ]);
    },
  });

  const result = await client.matrix({
    mode: "DRIVE",
    coordinates: [
      { latitude: 35, longitude: 139 },
      { latitude: 35.1, longitude: 139.1 },
    ],
    departureTime: "2026-07-30T00:00:00Z",
    objective: "FASTEST",
  });

  assert.equal(capturedBody?.["travelMode"], "DRIVE");
  assert.equal(capturedBody?.["routingPreference"], "TRAFFIC_UNAWARE");
  const origins = capturedBody?.["origins"] as Array<Record<string, unknown>>;
  assert.equal(origins.length, 2);
  assert.equal(origins.every((origin) => !("routeModifiers" in origin)), true);
  assert.equal((capturedBody?.["destinations"] as unknown[]).length, 2);
  assert.deepEqual(result, {
    elements: [
      {
        originIndex: 0,
        destinationIndex: 0,
        status: "OK",
        distanceMeters: 0,
        durationSeconds: 0,
      },
      {
        originIndex: 0,
        destinationIndex: 1,
        status: "OK",
        distanceMeters: 1234,
        durationSeconds: 456.5,
      },
      { originIndex: 1, destinationIndex: 0, status: "UNREACHABLE" },
    ],
  });
});

test("transit route uses exactly one origin/destination pair and strips the Google response", async () => {
  let capturedBody: Record<string, unknown> | undefined;
  const client = new GoogleRoutesClient({
    projectId: "anitabi-test",
    oauth,
    fetch: async (input, init) => {
      assert.equal(input, GOOGLE_ROUTE_URL);
      const fieldMask = new Headers(init?.headers).get("x-goog-fieldmask") ?? "";
      assert.equal(
        fieldMask.split(",").includes(
          "routes.legs.steps.transitDetails.localizedValues.departureTime.timeZone",
        ),
        true,
      );
      assert.equal(
        fieldMask.split(",").includes(
          "routes.legs.steps.transitDetails.localizedValues.arrivalTime.timeZone",
        ),
        true,
      );
      assert.equal(fieldMask.includes("localizedValues.departureTime.time.text"), false);
      assert.equal(fieldMask.includes("localizedValues.arrivalTime.time.text"), false);
      capturedBody = JSON.parse(init?.body as string) as Record<string, unknown>;
      return Response.json({
        routes: [
          {
            distanceMeters: 3210,
            duration: "900s",
            polyline: { encodedPolyline: "route-polyline" },
            legs: [
              {
                distanceMeters: 3210,
                duration: "900s",
                polyline: { encodedPolyline: "leg-polyline" },
                steps: [
                  {
                    travelMode: "TRANSIT",
                    distanceMeters: 3000,
                    staticDuration: "800s",
                    transitDetails: {
                      stopDetails: {
                        departureStop: { name: "Tokyo" },
                        arrivalStop: { name: "Ueno" },
                        departureTime: "2026-07-30T01:00:00Z",
                        arrivalTime: "2026-07-30T01:13:00Z",
                      },
                      localizedValues: {
                        departureTime: {
                          time: { text: "localized-text-must-not-pass-through" },
                          timeZone: "Asia/Tokyo",
                        },
                        arrivalTime: {
                          time: { text: "localized-text-must-not-pass-through" },
                          timeZone: "Asia/Tokyo",
                        },
                      },
                      transitLine: {
                        name: "Yamanote Line",
                        nameShort: "JY",
                        vehicle: { name: "Train", type: "HEAVY_RAIL" },
                        agencies: [{ name: "must-not-pass-through" }],
                      },
                      headsign: "Ueno",
                      stopCount: 5,
                      internalField: "must-not-pass-through",
                    },
                  },
                ],
              },
            ],
            unknownResponseBody: "must-not-pass-through",
          },
        ],
      });
    },
  });

  const result = await client.route({
    mode: "TRANSIT",
    locations: [
      { latitude: 35.6812, longitude: 139.7671 },
      { latitude: 35.7142, longitude: 139.7774 },
    ],
    departureTime: "2026-07-30T01:00:00Z",
  });

  assert.equal(capturedBody?.["travelMode"], "TRANSIT");
  assert.deepEqual(capturedBody?.["intermediates"], []);
  assert.equal(capturedBody?.["departureTime"], "2026-07-30T01:00:00Z");
  assert.equal(JSON.stringify(result).includes("must-not-pass-through"), false);
  assert.deepEqual(result.legs[0]?.steps[0]?.transit, {
    departureStop: "Tokyo",
    arrivalStop: "Ueno",
    departureTime: "2026-07-30T01:00:00Z",
    arrivalTime: "2026-07-30T01:13:00Z",
    departureTimeZone: "Asia/Tokyo",
    arrivalTimeZone: "Asia/Tokyo",
    lineName: "Yamanote Line",
    lineShortName: "JY",
    headsign: "Ueno",
    vehicleName: "Train",
    vehicleType: "HEAVY_RAIL",
    stopCount: 5,
  });
});

test("transit route maps arrival time and combined transit preferences to Google", async () => {
  let capturedBody: Record<string, unknown> | undefined;
  const client = new GoogleRoutesClient({
    projectId: "anitabi-test",
    oauth,
    fetch: async (input, init) => {
      assert.equal(input, GOOGLE_ROUTE_URL);
      capturedBody = JSON.parse(init?.body as string) as Record<string, unknown>;
      return Response.json({ routes: [{ legs: [{}] }] });
    },
  });

  await client.route({
    mode: "TRANSIT",
    locations: [
      { latitude: 35.6812, longitude: 139.7671 },
      { latitude: 35.7142, longitude: 139.7774 },
    ],
    arrivalTime: "2026-07-30T02:00:00Z",
    transitRoutingPreference: "LESS_WALKING",
    transitTravelModes: ["BUS", "SUBWAY", "TRAIN", "LIGHT_RAIL", "RAIL"],
  });

  assert.equal(capturedBody?.["arrivalTime"], "2026-07-30T02:00:00Z");
  assert.equal("departureTime" in (capturedBody ?? {}), false);
  assert.deepEqual(capturedBody?.["transitPreferences"], {
    routingPreference: "LESS_WALKING",
    allowedTravelModes: ["BUS", "SUBWAY", "TRAIN", "LIGHT_RAIL", "RAIL"],
  });
});

test("drive route accepts protobuf JSON with omitted default-valued route fields", async () => {
  let capturedBody: Record<string, unknown> | undefined;
  const client = new GoogleRoutesClient({
    projectId: "anitabi-test",
    oauth,
    fetch: async (input, init) => {
      assert.equal(input, GOOGLE_ROUTE_URL);
      capturedBody = JSON.parse(init?.body as string) as Record<string, unknown>;
      return Response.json({
        routes: [
          {
            legs: [
              {
                steps: [{ travelMode: "DRIVE" }],
              },
            ],
          },
        ],
      });
    },
  });

  const result = await client.route({
    mode: "DRIVE",
    locations: [
      { latitude: 35, longitude: 139 },
      { latitude: 35, longitude: 139 },
    ],
  });

  assert.equal(capturedBody?.["travelMode"], "DRIVE");
  assert.equal(capturedBody?.["routingPreference"], "TRAFFIC_UNAWARE");
  assert.deepEqual(capturedBody?.["routeModifiers"], {
    avoidTolls: false,
    avoidHighways: false,
    avoidFerries: false,
  });
  assert.deepEqual(result, {
    distanceMeters: 0,
    durationSeconds: 0,
    legs: [
      {
        distanceMeters: 0,
        durationSeconds: 0,
        steps: [
          {
            travelMode: "DRIVE",
            distanceMeters: 0,
            durationSeconds: 0,
          },
        ],
      },
    ],
  });
});

test("upstream failures map to a safe unified error without reading the response body", async () => {
  const client = new GoogleRoutesClient({
    projectId: "anitabi-test",
    oauth,
    fetch: async () => new Response("secret upstream body", { status: 429 }),
  });
  await assert.rejects(
    client.route({
      mode: "WALK",
      locations: [
        { latitude: 35, longitude: 139 },
        { latitude: 35.1, longitude: 139.1 },
      ],
    }),
    hasCode("UPSTREAM_UNAVAILABLE"),
  );
});

test("upstream HTTP 404 is unavailable rather than a no-route result", async () => {
  const client = new GoogleRoutesClient({
    projectId: "anitabi-test",
    oauth,
    fetch: async () => new Response("secret upstream body", { status: 404 }),
  });

  await assert.rejects(
    client.route({
      mode: "TRANSIT",
      locations: [
        { latitude: 35, longitude: 139 },
        { latitude: 35.1, longitude: 139.1 },
      ],
      departureTime: "2026-07-30T01:00:00Z",
    }),
    hasCode("UPSTREAM_UNAVAILABLE"),
  );
});

test("successful response with an empty routes array is a no-route result", async () => {
  const client = new GoogleRoutesClient({
    projectId: "anitabi-test",
    oauth,
    fetch: async () => Response.json({ routes: [] }),
  });

  await assert.rejects(
    client.route({
      mode: "TRANSIT",
      locations: [
        { latitude: 35, longitude: 139 },
        { latitude: 35.1, longitude: 139.1 },
      ],
      departureTime: "2026-07-30T01:00:00Z",
    }),
    hasCode("NO_ROUTE"),
  );
});

test("successful malformed response without a routes array is unavailable", async () => {
  const client = new GoogleRoutesClient({
    projectId: "anitabi-test",
    oauth,
    fetch: async () => Response.json({}),
  });

  await assert.rejects(
    client.route({
      mode: "WALK",
      locations: [
        { latitude: 35, longitude: 139 },
        { latitude: 35.1, longitude: 139.1 },
      ],
    }),
    hasCode("UPSTREAM_UNAVAILABLE"),
  );
});

function hasCode(code: string): (error: unknown) => boolean {
  return (error) => typeof error === "object" && error !== null && "code" in error && error.code === code;
}
