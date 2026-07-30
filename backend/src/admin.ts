import Database from "better-sqlite3";
import {
  copyFileSync,
  mkdirSync,
  readdirSync,
  renameSync,
  rmSync,
  statSync,
} from "node:fs";
import { basename, dirname, join, resolve } from "node:path";
import { SqliteQuotaLedger } from "./quota/ledger.js";

const databasePath = process.env["ANITABI_DATABASE_PATH"] ?? "/data/anitabi.sqlite";
const command = process.argv[2];

switch (command) {
  case "backup":
    await backup();
    break;
  case "disable-billing":
    setBilling(false);
    break;
  case "enable-billing-after-audit":
    if (process.argv[3] !== "CONFIRM_QUOTA_LEDGER_AUDITED") process.exitCode = 2;
    else setBilling(true);
    break;
  case "restore":
    restore(process.argv[3], process.argv[4]);
    break;
  default:
    process.exitCode = 2;
}

async function backup(): Promise<void> {
  const source = new Database(databasePath);
  requireIntegrity(source);
  const backupDirectory = join(dirname(databasePath), "backups");
  mkdirSync(backupDirectory, { recursive: true, mode: 0o700 });
  const timestamp = new Date().toISOString().replaceAll(":", "-").replaceAll(".", "-");
  const destination = join(backupDirectory, `anitabi-quota-${timestamp}.sqlite`);
  await source.backup(destination);
  source.close();

  const copy = new Database(destination, { readonly: true });
  requireIntegrity(copy);
  copy.close();
  pruneBackups(backupDirectory, Date.now() - 7 * 24 * 60 * 60 * 1_000);
  process.stdout.write(`${JSON.stringify({ status: "ok", backup: basename(destination) })}\n`);
}

function restore(sourceArgument: string | undefined, confirmation: string | undefined): void {
  if (sourceArgument === undefined || confirmation !== "CONFIRM_RESTORE_AND_DISABLE_BILLING") {
    process.exitCode = 2;
    return;
  }
  const sourcePath = resolve(sourceArgument);
  const allowedDirectory = resolve(dirname(databasePath), "backups");
  if (dirname(sourcePath) !== allowedDirectory || !basename(sourcePath).startsWith("anitabi-quota-")) {
    process.exitCode = 2;
    return;
  }
  const source = new Database(sourcePath, { readonly: true });
  requireIntegrity(source);
  source.close();

  const rollbackCopy = `${databasePath}.pre-restore-${Date.now()}`;
  renameSync(databasePath, rollbackCopy);
  try {
    copyFileSync(sourcePath, databasePath);
    const ledger = new SqliteQuotaLedger(new Database(databasePath));
    ledger.setBillingEnabled(false);
    ledger.close();
  } catch (error) {
    rmSync(databasePath, { force: true });
    renameSync(rollbackCopy, databasePath);
    throw error;
  }
  process.stdout.write(`${JSON.stringify({ status: "restored", billing: "disabled" })}\n`);
}

function setBilling(enabled: boolean): void {
  const ledger = new SqliteQuotaLedger(new Database(databasePath));
  ledger.setBillingEnabled(enabled);
  ledger.close();
  process.stdout.write(`${JSON.stringify({ status: "ok", billing: enabled ? "enabled" : "disabled" })}\n`);
}

function requireIntegrity(database: Database.Database): void {
  const rows = database.pragma("integrity_check") as Array<Record<string, string>>;
  const values = rows.flatMap((row) => Object.values(row));
  if (values.length !== 1 || values[0] !== "ok") throw new Error("SQLite integrity check failed");
}

function pruneBackups(directory: string, oldestAllowedMillis: number): void {
  for (const name of readdirSync(directory)) {
    if (!name.startsWith("anitabi-quota-") || !name.endsWith(".sqlite")) continue;
    const path = join(directory, name);
    if (statSync(path).mtimeMs < oldestAllowedMillis) rmSync(path, { force: true });
  }
}
