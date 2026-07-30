import Database from "better-sqlite3";
import { ApiError } from "../errors.js";

export const QUOTA_LIMITS = {
  matrix: { monthly: 9_000, dailyPerUid: 2_000 },
  route: { monthly: 9_000, dailyPerUid: 200 },
  navigation: { monthly: 900, dailyPerUid: 20 },
} as const;

export type QuotaBucket = keyof typeof QUOTA_LIMITS;

export type QuotaReservation = Readonly<{
  bucket: QuotaBucket;
  units: number;
  uid: string;
  now?: Date;
}>;

export type QuotaReservationResult = Readonly<{
  monthlyUsed: number;
  monthlyRemaining: number;
  dailyUsed: number;
  dailyRemaining: number;
}>;

export type LedgerHealth = Readonly<{
  healthy: boolean;
  billingEnabled: boolean;
}>;

export interface QuotaLedger {
  reserve(reservation: QuotaReservation): QuotaReservationResult;
  health(): LedgerHealth;
  close(): void;
}

type UsageRow = { used: number };
type MetadataRow = { value: string };

export class SqliteQuotaLedger implements QuotaLedger {
  private healthy = true;
  private readonly reserveTransaction: (
    reservation: Required<QuotaReservation>,
  ) => QuotaReservationResult;

  constructor(private readonly database: Database.Database) {
    try {
      database.pragma("journal_mode = WAL");
      database.pragma("synchronous = FULL");
      database.pragma("busy_timeout = 5000");
      database.exec(`
        CREATE TABLE IF NOT EXISTS quota_usage (
          dimension TEXT NOT NULL CHECK (dimension IN ('global', 'uid')),
          bucket TEXT NOT NULL CHECK (bucket IN ('matrix', 'route', 'navigation')),
          subject TEXT NOT NULL,
          period TEXT NOT NULL,
          used INTEGER NOT NULL CHECK (used >= 0),
          PRIMARY KEY (dimension, bucket, subject, period)
        ) STRICT;

        CREATE TABLE IF NOT EXISTS quota_metadata (
          key TEXT PRIMARY KEY,
          value TEXT NOT NULL
        ) STRICT;

        INSERT OR IGNORE INTO quota_metadata(key, value)
        VALUES ('billing_enabled', '1');
      `);
      const integrityRows = database.pragma("integrity_check") as Array<Record<string, string>>;
      const integrityValues = integrityRows.flatMap((row) => Object.values(row));
      if (integrityValues.length !== 1 || integrityValues[0] !== "ok") {
        throw new Error("SQLite integrity check failed");
      }
    } catch (error) {
      this.healthy = false;
      try {
        database.close();
      } catch {}
      throw new ApiError("BACKEND_UNAVAILABLE", { cause: error });
    }

    this.reserveTransaction = database.transaction(
      (reservation: Required<QuotaReservation>): QuotaReservationResult => {
        if (!this.billingEnabled()) throw new ApiError("BACKEND_UNAVAILABLE");

        const limits = QUOTA_LIMITS[reservation.bucket];
        const month = reservation.now.toISOString().slice(0, 7);
        const day = reservation.now.toISOString().slice(0, 10);
        const monthlyUsed = this.readUsage("global", reservation.bucket, "*", month);
        const dailyUsed = this.readUsage("uid", reservation.bucket, reservation.uid, day);

        if (
          monthlyUsed + reservation.units > limits.monthly ||
          dailyUsed + reservation.units > limits.dailyPerUid
        ) {
          throw new ApiError("QUOTA_EXHAUSTED");
        }

        this.addUsage("global", reservation.bucket, "*", month, reservation.units);
        this.addUsage("uid", reservation.bucket, reservation.uid, day, reservation.units);

        const nextMonthly = monthlyUsed + reservation.units;
        const nextDaily = dailyUsed + reservation.units;
        return {
          monthlyUsed: nextMonthly,
          monthlyRemaining: limits.monthly - nextMonthly,
          dailyUsed: nextDaily,
          dailyRemaining: limits.dailyPerUid - nextDaily,
        };
      },
    ).immediate;
  }

  static open(path: string): QuotaLedger {
    try {
      return new SqliteQuotaLedger(new Database(path));
    } catch {
      return new UnavailableQuotaLedger();
    }
  }

  reserve(reservation: QuotaReservation): QuotaReservationResult {
    if (!this.healthy) throw new ApiError("BACKEND_UNAVAILABLE");
    if (!Number.isSafeInteger(reservation.units) || reservation.units <= 0) {
      throw new ApiError("INVALID_ARGUMENT");
    }
    if (reservation.uid.length === 0 || reservation.uid.length > 128) {
      throw new ApiError("UNAUTHENTICATED");
    }

    try {
      return this.reserveTransaction({
        ...reservation,
        now: reservation.now ?? new Date(),
      });
    } catch (error) {
      if (error instanceof ApiError) throw error;
      this.healthy = false;
      throw new ApiError("BACKEND_UNAVAILABLE", { cause: error });
    }
  }

  health(): LedgerHealth {
    if (!this.healthy) return { healthy: false, billingEnabled: false };
    try {
      this.database.prepare("SELECT 1").get();
      return { healthy: true, billingEnabled: this.billingEnabled() };
    } catch {
      this.healthy = false;
      return { healthy: false, billingEnabled: false };
    }
  }

  setBillingEnabled(enabled: boolean): void {
    if (!this.healthy) throw new ApiError("BACKEND_UNAVAILABLE");
    try {
      this.database
        .prepare("UPDATE quota_metadata SET value = ? WHERE key = 'billing_enabled'")
        .run(enabled ? "1" : "0");
    } catch (error) {
      this.healthy = false;
      throw new ApiError("BACKEND_UNAVAILABLE", { cause: error });
    }
  }

  close(): void {
    this.database.close();
  }

  private billingEnabled(): boolean {
    const row = this.database
      .prepare("SELECT value FROM quota_metadata WHERE key = 'billing_enabled'")
      .get() as MetadataRow | undefined;
    return row?.value === "1";
  }

  private readUsage(
    dimension: "global" | "uid",
    bucket: QuotaBucket,
    subject: string,
    period: string,
  ): number {
    const row = this.database
      .prepare(
        "SELECT used FROM quota_usage WHERE dimension = ? AND bucket = ? AND subject = ? AND period = ?",
      )
      .get(dimension, bucket, subject, period) as UsageRow | undefined;
    return row?.used ?? 0;
  }

  private addUsage(
    dimension: "global" | "uid",
    bucket: QuotaBucket,
    subject: string,
    period: string,
    units: number,
  ): void {
    this.database
      .prepare(`
        INSERT INTO quota_usage(dimension, bucket, subject, period, used)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(dimension, bucket, subject, period)
        DO UPDATE SET used = used + excluded.used
      `)
      .run(dimension, bucket, subject, period, units);
  }
}

export class UnavailableQuotaLedger implements QuotaLedger {
  reserve(_reservation: QuotaReservation): never {
    throw new ApiError("BACKEND_UNAVAILABLE");
  }

  health(): LedgerHealth {
    return { healthy: false, billingEnabled: false };
  }

  close(): void {}
}
