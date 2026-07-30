import { readFileSync } from "node:fs";
import { ApiError } from "../errors.js";

const TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
const CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

type ServiceAccountFile = Readonly<{
  type: string;
  project_id: string;
  private_key: string;
  client_email: string;
  token_uri: string;
}>;

type CachedToken = Readonly<{
  value: string;
  expiresAtMillis: number;
}>;

export interface OAuthTokenProvider {
  getAccessToken(): Promise<string>;
}

export type OAuthProviderOptions = Readonly<{
  credentials: ServiceAccountFile;
  fetch?: typeof fetch;
  now?: () => number;
}>;

export class GoogleOAuthTokenProvider implements OAuthTokenProvider {
  private cached?: CachedToken;
  private inFlight: Promise<CachedToken> | undefined;
  private readonly fetchImplementation: typeof fetch;
  private readonly now: () => number;

  constructor(private readonly options: OAuthProviderOptions) {
    validateCredentials(options.credentials);
    this.fetchImplementation = options.fetch ?? fetch;
    this.now = options.now ?? Date.now;
  }

  static fromFile(path: string): GoogleOAuthTokenProvider {
    let value: unknown;
    try {
      value = JSON.parse(readFileSync(path, "utf8"));
    } catch (error) {
      throw new ApiError("BACKEND_UNAVAILABLE", { cause: error });
    }
    return new GoogleOAuthTokenProvider({ credentials: value as ServiceAccountFile });
  }

  async getAccessToken(): Promise<string> {
    if (this.cached !== undefined && this.cached.expiresAtMillis - this.now() > 60_000) {
      return this.cached.value;
    }
    if (this.inFlight === undefined) {
      this.inFlight = this.refresh().finally(() => {
        this.inFlight = undefined;
      });
    }
    this.cached = await this.inFlight;
    return this.cached.value;
  }

  private async refresh(): Promise<CachedToken> {
    const assertion = await createServiceAccountAssertion(this.options.credentials, this.now());
    let response: Response;
    try {
      response = await this.fetchImplementation(TOKEN_ENDPOINT, {
        method: "POST",
        headers: { "content-type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
          grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
          assertion,
        }),
        signal: AbortSignal.timeout(5_000),
      });
    } catch (error) {
      throw new ApiError("BACKEND_UNAVAILABLE", { cause: error });
    }
    if (!response.ok) throw new ApiError("BACKEND_UNAVAILABLE");

    let payload: unknown;
    try {
      payload = await response.json();
    } catch (error) {
      throw new ApiError("BACKEND_UNAVAILABLE", { cause: error });
    }
    const record = payload as Record<string, unknown>;
    const accessToken = record["access_token"];
    const expiresIn = record["expires_in"];
    if (
      typeof accessToken !== "string" ||
      accessToken.length === 0 ||
      typeof expiresIn !== "number" ||
      !Number.isFinite(expiresIn) ||
      expiresIn <= 60
    ) {
      throw new ApiError("BACKEND_UNAVAILABLE");
    }
    return {
      value: accessToken,
      expiresAtMillis: this.now() + expiresIn * 1_000,
    };
  }
}

async function createServiceAccountAssertion(
  credentials: ServiceAccountFile,
  nowMillis: number,
): Promise<string> {
  const issuedAt = Math.floor(nowMillis / 1_000);
  const header = base64UrlJson({ alg: "RS256", typ: "JWT" });
  const claims = base64UrlJson({
    iss: credentials.client_email,
    scope: CLOUD_PLATFORM_SCOPE,
    aud: TOKEN_ENDPOINT,
    iat: issuedAt,
    exp: issuedAt + 3_600,
  });
  const signingInput = `${header}.${claims}`;

  try {
    const key = await crypto.subtle.importKey(
      "pkcs8",
      pemToDer(credentials.private_key),
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["sign"],
    );
    const signature = await crypto.subtle.sign(
      "RSASSA-PKCS1-v1_5",
      key,
      new TextEncoder().encode(signingInput),
    );
    return `${signingInput}.${Buffer.from(signature).toString("base64url")}`;
  } catch (error) {
    throw new ApiError("BACKEND_UNAVAILABLE", { cause: error });
  }
}

function base64UrlJson(value: object): string {
  return Buffer.from(JSON.stringify(value), "utf8").toString("base64url");
}

function pemToDer(pem: string): ArrayBuffer {
  const base64 = pem
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");
  const buffer = Buffer.from(base64, "base64");
  return buffer.buffer.slice(buffer.byteOffset, buffer.byteOffset + buffer.byteLength);
}

function validateCredentials(credentials: ServiceAccountFile): void {
  if (
    credentials.type !== "service_account" ||
    !credentials.project_id ||
    !credentials.client_email ||
    !credentials.private_key ||
    credentials.token_uri !== TOKEN_ENDPOINT
  ) {
    throw new ApiError("BACKEND_UNAVAILABLE");
  }
}
