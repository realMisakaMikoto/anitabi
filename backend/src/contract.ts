export type Coordinate = Readonly<{
  latitude: number;
  longitude: number;
}>;

export type RoadMode = "DRIVE" | "BICYCLE" | "WALK";
export type TravelMode = RoadMode | "TRANSIT";
export type RouteObjective = "FASTEST" | "SHORTEST";

export type MatrixRequest = Readonly<{
  mode: RoadMode;
  coordinates: Coordinate[];
  departureTime?: string;
  objective: RouteObjective;
}>;

export type RouteRequest = Readonly<{
  mode: TravelMode;
  locations: Coordinate[];
  departureTime?: string;
}>;

export type NavigationReservationRequest = Readonly<{
  destinationCount: number;
}>;

export type NormalizedMatrixElement = Readonly<{
  originIndex: number;
  destinationIndex: number;
  status: "OK" | "UNREACHABLE";
  distanceMeters?: number;
  durationSeconds?: number;
}>;

export type NormalizedMatrix = Readonly<{
  elements: NormalizedMatrixElement[];
}>;

export type NormalizedTransitDetails = Readonly<{
  departureStop?: string;
  arrivalStop?: string;
  departureTime?: string;
  arrivalTime?: string;
  lineName?: string;
  lineShortName?: string;
  headsign?: string;
  vehicleName?: string;
  vehicleType?: string;
  stopCount?: number;
}>;

export type NormalizedRouteStep = Readonly<{
  travelMode: TravelMode;
  distanceMeters: number;
  durationSeconds: number;
  encodedPolyline?: string;
  instruction?: string;
  maneuver?: string;
  transit?: NormalizedTransitDetails;
}>;

export type NormalizedRouteLeg = Readonly<{
  distanceMeters: number;
  durationSeconds: number;
  encodedPolyline?: string;
  steps: NormalizedRouteStep[];
}>;

export type NormalizedRoute = Readonly<{
  distanceMeters: number;
  durationSeconds: number;
  encodedPolyline?: string;
  legs: NormalizedRouteLeg[];
}>;
