import { parentPort, workerData } from "node:worker_threads";
import Database from "better-sqlite3";
import { SqliteQuotaLedger } from "../dist/quota/ledger.js";

const ledger = new SqliteQuotaLedger(new Database(workerData.path));
let successes = 0;
for (let index = 0; index < workerData.attempts; index += 1) {
  try {
    ledger.reserve({
      bucket: "matrix",
      units: workerData.units,
      uid: workerData.uid,
      now: new Date(workerData.now),
    });
    successes += 1;
  } catch (error) {
    if (error?.code !== "QUOTA_EXHAUSTED") throw error;
  }
}
ledger.close();
parentPort.postMessage(successes);
