# 巡礼手帳 API 后端

The v0.2.1 backend is a single Node.js 24 LTS / Fastify process backed by SQLite WAL. It accepts only Firebase Anonymous ID tokens, normalizes Google Routes responses, and reserves quota before every billable operation.

## Endpoints

- `POST /v1/matrix`: 2-10 coordinates. A 10-coordinate square matrix reserves 100 Matrix Essentials elements.
- `POST /v1/route`: 2-12 road locations or exactly two transit locations. Transit accepts either `departureTime` or `arrivalTime`, the allowlisted `LESS_WALKING` / `FEWER_TRANSFERS` preference, and `transitTravelModes` with 1-5 unique values from `BUS`, `SUBWAY`, `TRAIN`, `LIGHT_RAIL`, and `RAIL`. Each accepted call reserves one Compute Routes request.
- `POST /v1/navigation/reserve`: reserves 1-25 Navigation SDK destinations before the Android SDK is called.
- `GET /v1/health`: returns only service and database health.

POST bodies are capped at 16 KiB. Production requests must arrive through HTTPS. The only Google upstreams are `routes.googleapis.com` and `oauth2.googleapis.com`; the field masks and timeouts are fixed in source.

## Quotas

Reservations use `BEGIN IMMEDIATE` SQLite transactions and UTC day/month boundaries:

| Bucket | Monthly global | Daily per anonymous UID |
| --- | ---: | ---: |
| Matrix Essentials | 9,000 elements | 2,000 elements |
| Compute Routes Essentials | 9,000 requests | 200 requests |
| Navigation Request | 900 destinations | 20 destinations |

Reservations are deliberately not refunded after an upstream failure. Database errors and a disabled billing flag fail closed. Restores always disable billing until an operator compares the restored ledger with Google usage and runs the explicit audited-enable command.

## Local verification

```text
npm ci
npm test
docker build -t anitabi-api:0.2.1 .
```

Tests use generated keys and simulated Google responses. No Google credential is needed.

## VPS layout

- Application: `/opt/anitabi-api`
- Data and seven-day backups: `/var/lib/anitabi-api`
- Read-only secrets: `/etc/anitabi-api/secrets`

The Compose file publishes the container only on host `127.0.0.1:8787` by default. Set the non-secret `ANITABI_HOST_PORT` Compose variable when that host port is already occupied; the container still listens on port 8787. The container runs as the non-root `node` user, drops all capabilities, uses a read-only root filesystem, and mounts only the data and secret paths. `deploy/Caddyfile.api` and `deploy/nginx-api.conf` are additive virtual hosts; use only the one matching the inventoried reverse proxy.

Install `deploy/anitabi-api-backup.service` and `deploy/anitabi-api-backup.timer` as a pair to run the existing integrity-checked backup script daily with a randomized delay.

Do not place credentials in `.env`. The Google project ID and optional loopback host port are the only Compose variables. Create the service-account JSON and a random 32-byte-or-longer HMAC key directly in `/etc/anitabi-api/secrets` with owner-only permissions.
