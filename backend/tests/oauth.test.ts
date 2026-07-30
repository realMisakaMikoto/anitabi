import assert from "node:assert/strict";
import { generateKeyPairSync } from "node:crypto";
import { test } from "node:test";
import { jwtVerify } from "jose";
import { GoogleOAuthTokenProvider } from "../src/google/oauth.js";

test("OAuth refresh uses a Web Crypto service-account JWT and single-flight caching", async () => {
  const { privateKey, publicKey } = generateKeyPairSync("rsa", { modulusLength: 2048 });
  const privatePem = privateKey.export({ type: "pkcs8", format: "pem" }).toString();
  let calls = 0;
  let now = Date.parse("2026-07-30T00:00:00Z");

  const provider = new GoogleOAuthTokenProvider({
    credentials: {
      type: "service_account",
      project_id: "anitabi-test",
      client_email: "routes@anitabi-test.iam.gserviceaccount.com",
      private_key: privatePem,
      token_uri: "https://oauth2.googleapis.com/token",
    },
    now: () => now,
    fetch: async (input, init) => {
      calls += 1;
      assert.equal(input, "https://oauth2.googleapis.com/token");
      assert.equal(init?.method, "POST");
      const params = init?.body as URLSearchParams;
      const assertion = params.get("assertion");
      assert.ok(assertion);
      const verified = await jwtVerify(assertion, publicKey, {
        algorithms: ["RS256"],
        audience: "https://oauth2.googleapis.com/token",
        issuer: "routes@anitabi-test.iam.gserviceaccount.com",
        currentDate: new Date(now),
      });
      assert.equal(verified.payload.scope, "https://www.googleapis.com/auth/cloud-platform");
      await new Promise((resolve) => setTimeout(resolve, 10));
      return Response.json({ access_token: `access-${calls}`, expires_in: 3_600 });
    },
  });

  const tokens = await Promise.all(Array.from({ length: 30 }, () => provider.getAccessToken()));
  assert.equal(calls, 1);
  assert.deepEqual(new Set(tokens), new Set(["access-1"]));

  now += 3_541_000;
  assert.equal(await provider.getAccessToken(), "access-2");
  assert.equal(calls, 2);
});

test("OAuth rejects a non-Google token endpoint before any request", () => {
  const { privateKey } = generateKeyPairSync("rsa", { modulusLength: 2048 });
  assert.throws(
    () =>
      new GoogleOAuthTokenProvider({
        credentials: {
          type: "service_account",
          project_id: "anitabi-test",
          client_email: "routes@anitabi-test.iam.gserviceaccount.com",
          private_key: privateKey.export({ type: "pkcs8", format: "pem" }).toString(),
          token_uri: "https://example.invalid/token",
        },
      }),
    hasCode("BACKEND_UNAVAILABLE"),
  );
});

function hasCode(code: string): (error: unknown) => boolean {
  return (error) => typeof error === "object" && error !== null && "code" in error && error.code === code;
}
