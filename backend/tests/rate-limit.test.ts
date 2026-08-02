import assert from "node:assert/strict";
import { test } from "node:test";
import { TokenBucketLimiter } from "../src/rate-limit.js";

test("IP token buckets limit bursts and refill over time", () => {
  let now = 1_000;
  const limiter = new TokenBucketLimiter({
    ipCapacity: 2,
    ipRefillPerSecond: 1,
    ipHmacKey: new Uint8Array(32).fill(7),
    now: () => now,
  });
  assert.equal(limiter.consume("192.0.2.10"), true);
  assert.equal(limiter.consume("192.0.2.10"), true);
  assert.equal(limiter.consume("192.0.2.10"), false);
  now += 1_000;
  assert.equal(limiter.consume("192.0.2.10"), true);
});

test("different IP addresses have independent buckets", () => {
  const limiter = new TokenBucketLimiter({
    ipCapacity: 1,
    ipRefillPerSecond: 0,
    ipHmacKey: new Uint8Array(32).fill(9),
  });
  assert.equal(limiter.consume("192.0.2.10"), true);
  assert.equal(limiter.consume("192.0.2.10"), false);
  assert.equal(limiter.consume("192.0.2.11"), true);
});

test("short HMAC keys are rejected", () => {
  assert.throws(
    () =>
      new TokenBucketLimiter({
        ipCapacity: 1,
        ipRefillPerSecond: 1,
        ipHmacKey: new Uint8Array(31),
      }),
    /at least 32 bytes/,
  );
});
