import assert from "node:assert/strict";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { test } from "node:test";
import { Worker } from "node:worker_threads";
import Database from "better-sqlite3";
import { SqliteQuotaLedger } from "../src/quota/ledger.js";

test("quota ledger enforces daily and monthly boundaries and UTC period changes", () => {
  const fixture = createFixture();
  try {
    const now = new Date("2026-07-31T23:59:59Z");
    const first = fixture.ledger.reserve({ bucket: "navigation", units: 20, uid: "uid-a", now });
    assert.equal(first.dailyRemaining, 0);
    assert.throws(
      () => fixture.ledger.reserve({ bucket: "navigation", units: 1, uid: "uid-a", now }),
      hasCode("QUOTA_EXHAUSTED"),
    );
    assert.doesNotThrow(() =>
      fixture.ledger.reserve({
        bucket: "navigation",
        units: 20,
        uid: "uid-a",
        now: new Date("2026-08-01T00:00:00Z"),
      }),
    );

    for (let uidIndex = 0; uidIndex < 9; uidIndex += 1) {
      for (let requestIndex = 0; requestIndex < 10; requestIndex += 1) {
        fixture.ledger.reserve({ bucket: "matrix", units: 100, uid: `uid-${uidIndex}`, now });
      }
    }
    assert.throws(
      () => fixture.ledger.reserve({ bucket: "matrix", units: 1, uid: "last-uid", now }),
      hasCode("QUOTA_EXHAUSTED"),
    );
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
      () => fixture.ledger.reserve({ bucket: "route", units: 1, uid: "uid-a" }),
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
        uid: `worker-${index}`,
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
