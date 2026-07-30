const coordinateSchema = {
  type: "object",
  additionalProperties: false,
  required: ["latitude", "longitude"],
  properties: {
    latitude: { type: "number", minimum: -90, maximum: 90 },
    longitude: { type: "number", minimum: -180, maximum: 180 },
  },
} as const;

export const matrixBodySchema = {
  type: "object",
  additionalProperties: false,
  required: ["mode", "coordinates", "objective"],
  properties: {
    mode: { type: "string", enum: ["DRIVE", "BICYCLE", "WALK"] },
    coordinates: {
      type: "array",
      minItems: 2,
      maxItems: 10,
      items: coordinateSchema,
    },
    departureTime: { type: "string", format: "date-time" },
    objective: { type: "string", enum: ["FASTEST", "SHORTEST"] },
  },
} as const;

export const routeBodySchema = {
  type: "object",
  additionalProperties: false,
  required: ["mode", "locations"],
  properties: {
    mode: { type: "string", enum: ["DRIVE", "BICYCLE", "WALK", "TRANSIT"] },
    locations: {
      type: "array",
      minItems: 2,
      maxItems: 12,
      items: coordinateSchema,
    },
    departureTime: { type: "string", format: "date-time" },
  },
} as const;

export const navigationReservationBodySchema = {
  type: "object",
  additionalProperties: false,
  required: ["destinationCount"],
  properties: {
    destinationCount: { type: "integer", minimum: 1, maximum: 25 },
  },
} as const;
