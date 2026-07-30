export type SafeLogEvent = Readonly<{
  level: "info" | "warn" | "error";
  event: "request_complete" | "startup" | "shutdown";
  endpoint?: string;
  statusCode?: number;
  latencyBucket?: "lt_100ms" | "100_499ms" | "500_1999ms" | "gte_2000ms";
  errorCode?: string;
}>;

export interface SafeLogger {
  write(event: SafeLogEvent): void;
}

export type LatencyBucket = "lt_100ms" | "100_499ms" | "500_1999ms" | "gte_2000ms";

export class JsonSafeLogger implements SafeLogger {
  constructor(private readonly sink: (line: string) => void = console.log) {}

  write(event: SafeLogEvent): void {
    this.sink(JSON.stringify(event));
  }
}

export function latencyBucket(milliseconds: number): LatencyBucket {
  if (milliseconds < 100) return "lt_100ms";
  if (milliseconds < 500) return "100_499ms";
  if (milliseconds < 2_000) return "500_1999ms";
  return "gte_2000ms";
}
