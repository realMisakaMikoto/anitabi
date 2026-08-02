import assert from "node:assert/strict";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { test } from "node:test";
import { Worker } from "node:worker_threads";
import Database from "better-sqlite3";
import { SqliteQuotaLedger } from "../src/quota/ledger.js";

test("quota ledger ignores legacy UID rows and enforces only shared UTC monthly limits", () => {
  const fixture = createFixture();
  try {
    const now = new Date("2026-07-31T23:59:59Z");
    const legacy = new Database(fixture.path);
    const insertLegacy = legacy.prepare(`
      INSERT INTO quota_usage(dimension, bucket, subject, period, used)
      VALUES ('uid', ?, 'legacy-uid', '2026-07-31', ?)
    `);
    insertLegacy.run("matrix", 2_000);
    insertLegacy.run("route", 200);
    insertLegacy.run("navigation", 20);
    legacy.close();

    assert.equal(
      fixture.ledger.reserve({ bucket: "matrix", units: 2_001, now }).monthlyRemaining,
      6_999,
    );
    assert.equal(
      fixture.ledger.reserve({ bucket: "route", units: 201, now }).monthlyRemaining,
      8_799,
    );
    assert.equal(
      fixture.ledger.reserve({ bucket: "navigation", units: 21, now }).monthlyRemaining,
      879,
    );

    fixture.ledger.reserve({ bucket: "route", units: 8_799, now });
    assert.throws(
      () => fixture.ledger.reserve({ bucket: "route", units: 1, now }),
      hasCode("QUOTA_EXHAUSTED"),
    );
    assert.doesNotThrow(() =>
      fixture.ledger.reserve({
        bucket: "route",
        units: 1,
        now: new Date("2026-08-01T00:00:00Z"),
      }),
    );

    const audit = new Database(fixture.path, { readonly: true });
    const legacyRows = audit
      .prepare("SELECT bucket, used FROM quota_usage WHERE dimension = 'uid' ORDER BY bucket")
      .all();
    audit.close();
    assert.deepEqual(legacyRows, [
      { bucket: "matrix", used: 2_000 },
      { bucket: "navigation", used: 20 },
      { bucket: "route", used: 200 },
    ]);
  } finally {
    fixture.close();
  }
});

test("quota ledger fails closed when billing is disabled", () => {
  const fixture = createFixture();
  try {
    fixture.ledger.setBillingEnabled(false);
    assert.deepEqual(fixture.ledger.health(), { healthy: true, billingEnabled: false });
    assert.throws(
      () => fixture.ledger.reserve({ bucket: "route", units: 1 }),
      hasCode("BACKEND_UNAVAILABLE"),
    );
  } finally {
    fixture.close();
  }
});

test("independent concurrent SQLite connections never exceed the monthly matrix cap", async () => {
  const fixture = createFixture();
  const databasePath = fixture.path;
  fixture.ledger.close();
  try {
    const workers = Array.from({ length: 12 }, (_, index) =>
      runWorker({
        path: databasePath,
        attempts: 10,
        units: 100,
        now: "2026-07-30T00:00:00Z",
      }),
    );
    const successCount = (await Promise.all(workers)).reduce((sum, value) => sum + value, 0);
    assert.equal(successCount, 90);

    const database = new Database(databasePath, { readonly: true });
    const row = database
      .prepare("SELECT used FROM quota_usage WHERE dimension='global' AND bucket='matrix'")
      .get() as { used: number };
    database.close();
    assert.equal(row.used, 9_000);
  } finally {
    rmSync(fixture.directory, { recursive: true, force: true });
  }
});

function createFixture(): {
  directory: string;
  path: string;
  ledger: SqliteQuotaLedger;
  close: () => void;
} {
  const directory = mkdtempSync(join(tmpdir(), "anitabi-quota-"));
  const path = join(directory, "quota.sqlite");
  const ledger = new SqliteQuotaLedger(new Database(path));
  return {
    directory,
    path,
    ledger,
    close: () => {
      ledger.close();
      rmSync(directory, { recursive: true, force: true });
    },
  };
}

function runWorker(workerData: object): Promise<number> {
  return new Promise((resolve, reject) => {
    const worker = new Worker(new URL("./quota-worker.mjs", import.meta.url), { workerData });
    worker.once("message", resolve);
    worker.once("error", reject);
    worker.once("exit", (code) => {
      if (code !== 0) reject(new Error(`Quota worker exited with code ${code}`));
    });
  });
}

function hasCode(code: string): (error: unknown) => boolean {
  return (error) => typeof error === "object" && error !== null && "code" in error && error.code === code;
}
