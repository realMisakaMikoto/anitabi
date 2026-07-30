import { mkdirSync } from "node:fs";
import { dirname } from "node:path";
import { buildApp } from "./app.js";
import { FirebaseJwtVerifier } from "./auth/firebase.js";
import { loadConfig } from "./config.js";
import { GoogleOAuthTokenProvider } from "./google/oauth.js";
import { GoogleRoutesClient } from "./google/routes.js";
import { JsonSafeLogger } from "./logging.js";
import { SqliteQuotaLedger } from "./quota/ledger.js";
import { TokenBucketLimiter } from "./rate-limit.js";

const logger = new JsonSafeLogger();
const config = loadConfig();
mkdirSync(dirname(config.databasePath), { recursive: true, mode: 0o700 });

const quota = SqliteQuotaLedger.open(config.databasePath);
const oauth = GoogleOAuthTokenProvider.fromFile(config.serviceAccountFile);
const app = buildApp({
  auth: new FirebaseJwtVerifier(config.projectId),
  routes: new GoogleRoutesClient({ projectId: config.projectId, oauth }),
  quota,
  rateLimiter: new TokenBucketLimiter({
    uidCapacity: 10,
    uidRefillPerSecond: 1,
    ipCapacity: 60,
    ipRefillPerSecond: 5,
    ipHmacKey: config.ipHmacKey,
  }),
  logger,
});

await app.listen({ host: config.host, port: config.port });
logger.write({ level: "info", event: "startup" });

for (const signal of ["SIGINT", "SIGTERM"] as const) {
  process.once(signal, () => {
    void app.close().finally(() => {
      logger.write({ level: "info", event: "shutdown" });
      process.exit(0);
    });
  });
}
