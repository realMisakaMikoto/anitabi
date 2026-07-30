import assert from "node:assert/strict";
import { test } from "node:test";
import { TokenBucketLimiter } from "../src/rate-limit.js";

test("token buckets limit UID bursts and refill over time", () => {
  let now = 1_000;
  const limiter = new TokenBucketLimiter({
    uidCapacity: 2,
    uidRefillPerSecond: 1,
    ipCapacity: 10,
    ipRefillPerSecond: 10,
    ipHmacKey: new Uint8Array(32).fill(7),
    now: () => now,
  });
  assert.equal(limiter.consume("uid-a", "192.0.2.10"), true);
  assert.equal(limiter.consume("uid-a", "192.0.2.10"), true);
  assert.equal(limiter.consume("uid-a", "192.0.2.10"), false);
  now += 1_000;
  assert.equal(limiter.consume("uid-a", "192.0.2.10"), true);
});

test("the wider auxiliary IP bucket applies across UIDs", () => {
  const limiter = new TokenBucketLimiter({
    uidCapacity: 10,
    uidRefillPerSecond: 0,
    ipCapacity: 2,
    ipRefillPerSecond: 0,
    ipHmacKey: new Uint8Array(32).fill(9),
  });
  assert.equal(limiter.consume("uid-a", "192.0.2.10"), true);
  assert.equal(limiter.consume("uid-b", "192.0.2.10"), true);
  assert.equal(limiter.consume("uid-c", "192.0.2.10"), false);
  assert.equal(limiter.consume("uid-c", "192.0.2.11"), true);
});

test("short HMAC keys are rejected", () => {
  assert.throws(
    () =>
      new TokenBucketLimiter({
        uidCapacity: 1,
        uidRefillPerSecond: 1,
        ipCapacity: 1,
        ipRefillPerSecond: 1,
        ipHmacKey: new Uint8Array(31),
      }),
    /at least 32 bytes/,
  );
});
