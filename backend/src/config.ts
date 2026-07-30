import { readFileSync } from "node:fs";
import { ApiError } from "./errors.js";

export type RuntimeConfig = Readonly<{
  projectId: string;
  serviceAccountFile: string;
  ipHmacKey: Uint8Array;
  databasePath: string;
  host: string;
  port: number;
}>;

export function loadConfig(environment: NodeJS.ProcessEnv = process.env): RuntimeConfig {
  const firebaseProjectId = required(environment, "ANITABI_FIREBASE_PROJECT_ID");
  const googleProjectId = required(environment, "ANITABI_GOOGLE_PROJECT_ID");
  if (firebaseProjectId !== googleProjectId) {
    throw new ApiError("BACKEND_UNAVAILABLE");
  }
  const hmacFile = environment["ANITABI_IP_HMAC_KEY_FILE"] ?? "/run/secrets/ip_hmac_key";
  let ipHmacKey: Uint8Array;
  try {
    ipHmacKey = readFileSync(hmacFile);
  } catch (error) {
    throw new ApiError("BACKEND_UNAVAILABLE", { cause: error });
  }
  if (ipHmacKey.byteLength < 32) throw new ApiError("BACKEND_UNAVAILABLE");

  const port = Number(environment["ANITABI_PORT"] ?? "8787");
  if (!Number.isSafeInteger(port) || port < 1 || port > 65_535) {
    throw new ApiError("BACKEND_UNAVAILABLE");
  }
  return {
    projectId: googleProjectId,
    serviceAccountFile:
      environment["ANITABI_SERVICE_ACCOUNT_FILE"] ?? "/run/secrets/google_service_account.json",
    ipHmacKey,
    databasePath: environment["ANITABI_DATABASE_PATH"] ?? "/data/anitabi.sqlite",
    host: environment["ANITABI_HOST"] ?? "0.0.0.0",
    port,
  };
}

function required(environment: NodeJS.ProcessEnv, name: string): string {
  const value = environment[name];
  if (value === undefined || value.length === 0 || value.length > 128) {
    throw new ApiError("BACKEND_UNAVAILABLE");
  }
  return value;
}
