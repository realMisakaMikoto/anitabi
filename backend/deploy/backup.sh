#!/bin/sh
set -eu

cd /opt/anitabi-api
docker compose exec -T api node dist/admin.js backup
