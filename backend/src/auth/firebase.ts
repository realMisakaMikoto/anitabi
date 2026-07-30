import {
  createRemoteJWKSet,
  jwtVerify,
  type JWTVerifyGetKey,
  type JWTPayload,
} from "jose";
import { ApiError } from "../errors.js";

const FIREBASE_JWKS_URL = new URL(
  "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com",
);

export interface FirebaseTokenVerifier {
  verify(idToken: string): Promise<string>;
}

export class FirebaseJwtVerifier implements FirebaseTokenVerifier {
  private readonly keySet: JWTVerifyGetKey;

  constructor(
    private readonly projectId: string,
    keySet?: JWTVerifyGetKey,
  ) {
    if (!projectId || projectId.length > 128) throw new Error("A Firebase project ID is required");
    this.keySet = keySet ?? createRemoteJWKSet(FIREBASE_JWKS_URL, {
      cooldownDuration: 30_000,
      cacheMaxAge: 60 * 60 * 1_000,
      timeoutDuration: 5_000,
    });
  }

  async verify(idToken: string): Promise<string> {
    try {
      const result = await jwtVerify(idToken, this.keySet, {
        algorithms: ["RS256"],
        audience: this.projectId,
        issuer: `https://securetoken.google.com/${this.projectId}`,
        clockTolerance: 5,
      });
      return requireAnonymousUid(result.payload);
    } catch (error) {
      if (error instanceof ApiError) throw error;
      throw new ApiError("UNAUTHENTICATED", { cause: error });
    }
  }
}

function requireAnonymousUid(payload: JWTPayload): string {
  const uid = payload.sub;
  const firebase = payload["firebase"];
  const provider =
    typeof firebase === "object" && firebase !== null
      ? (firebase as Record<string, unknown>)["sign_in_provider"]
      : undefined;
  if (typeof uid !== "string" || uid.length === 0 || uid.length > 128 || provider !== "anonymous") {
    throw new ApiError("UNAUTHENTICATED");
  }
  return uid;
}
