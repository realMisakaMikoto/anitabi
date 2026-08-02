import { createHmac } from "node:crypto";

type Bucket = {
  tokens: number;
  updatedAtMillis: number;
};

export type TokenBucketOptions = Readonly<{
  ipCapacity: number;
  ipRefillPerSecond: number;
  ipHmacKey: Uint8Array;
  now?: () => number;
}>;

export class TokenBucketLimiter {
  private readonly ipBuckets = new Map<string, Bucket>();
  private readonly now: () => number;

  constructor(private readonly options: TokenBucketOptions) {
    if (options.ipHmacKey.byteLength < 32) {
      throw new Error("The IP HMAC key must contain at least 32 bytes");
    }
    this.now = options.now ?? Date.now;
  }

  consume(rawIp: string): boolean {
    const now = this.now();
    const ipKey = createHmac("sha256", this.options.ipHmacKey).update(rawIp).digest("hex");
    return this.take(
      this.ipBuckets,
      ipKey,
      this.options.ipCapacity,
      this.options.ipRefillPerSecond,
      now,
    );
  }

  private take(
    buckets: Map<string, Bucket>,
    key: string,
    capacity: number,
    refillPerSecond: number,
    now: number,
  ): boolean {
    const existing = buckets.get(key) ?? { tokens: capacity, updatedAtMillis: now };
    const elapsedSeconds = Math.max(0, now - existing.updatedAtMillis) / 1_000;
    const tokens = Math.min(capacity, existing.tokens + elapsedSeconds * refillPerSecond);
    if (tokens < 1) {
      buckets.set(key, { tokens, updatedAtMillis: now });
      return false;
    }
    buckets.set(key, { tokens: tokens - 1, updatedAtMillis: now });
    return true;
  }
}
