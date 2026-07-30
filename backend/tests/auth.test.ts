import assert from "node:assert/strict";
import { test } from "node:test";
import { createLocalJWKSet, exportJWK, generateKeyPair, SignJWT } from "jose";
import { FirebaseJwtVerifier } from "../src/auth/firebase.js";

test("Firebase verifier accepts only a valid anonymous token for the configured project", async () => {
  const { privateKey, publicKey } = await generateKeyPair("RS256");
  const jwk = await exportJWK(publicKey);
  jwk.kid = "test-key";
  const keySet = createLocalJWKSet({ keys: [jwk] });
  const verifier = new FirebaseJwtVerifier("anitabi-test", keySet);
  const now = Math.floor(Date.now() / 1_000);
  const token = await new SignJWT({ firebase: { sign_in_provider: "anonymous" } })
    .setProtectedHeader({ alg: "RS256", kid: "test-key" })
    .setSubject("anonymous-uid")
    .setAudience("anitabi-test")
    .setIssuer("https://securetoken.google.com/anitabi-test")
    .setIssuedAt(now)
    .setExpirationTime(now + 3_600)
    .sign(privateKey);

  assert.equal(await verifier.verify(token), "anonymous-uid");
});

test("Firebase verifier rejects wrong project, expired, and non-anonymous tokens", async () => {
  const { privateKey, publicKey } = await generateKeyPair("RS256");
  const jwk = await exportJWK(publicKey);
  jwk.kid = "test-key";
  const verifier = new FirebaseJwtVerifier("anitabi-test", createLocalJWKSet({ keys: [jwk] }));
  const now = Math.floor(Date.now() / 1_000);

  async function sign(overrides: {
    audience?: string;
    expiresAt?: number;
    provider?: string;
  }): Promise<string> {
    return new SignJWT({ firebase: { sign_in_provider: overrides.provider ?? "anonymous" } })
      .setProtectedHeader({ alg: "RS256", kid: "test-key" })
      .setSubject("anonymous-uid")
      .setAudience(overrides.audience ?? "anitabi-test")
      .setIssuer("https://securetoken.google.com/anitabi-test")
      .setIssuedAt(now - 100)
      .setExpirationTime(overrides.expiresAt ?? now + 3_600)
      .sign(privateKey);
  }

  await assert.rejects(verifier.verify(await sign({ audience: "other-project" })), hasCode("UNAUTHENTICATED"));
  await assert.rejects(verifier.verify(await sign({ expiresAt: now - 10 })), hasCode("UNAUTHENTICATED"));
  await assert.rejects(verifier.verify(await sign({ provider: "password" })), hasCode("UNAUTHENTICATED"));
});

function hasCode(code: string): (error: unknown) => boolean {
  return (error) => typeof error === "object" && error !== null && "code" in error && error.code === code;
}
