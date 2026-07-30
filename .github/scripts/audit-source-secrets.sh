#!/usr/bin/env bash
set -euo pipefail

if git ls-files --error-unmatch app/google-services.json >/dev/null 2>&1; then
  echo "app/google-services.json must remain ignored and untracked" >&2
  exit 1
fi

secret_pattern='AIza[0-9A-Za-z_-]{35}|"private_key_id"[[:space:]]*:[[:space:]]*"[0-9a-f]{20,}"|"private_key"[[:space:]]*:[[:space:]]*"-----BEGIN|^-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|ANITABI_GOOGLE_SERVICES_JSON_BASE64[[:space:]]*='
if git grep -I -q -E "$secret_pattern" -- . ':(exclude).github/scripts/audit-source-secrets.sh'; then
  echo "A tracked credential pattern was detected" >&2
  exit 1
fi

echo "Tracked-source credential audit passed"
