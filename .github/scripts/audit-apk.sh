#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ! -f "$1" ]]; then
  echo "Usage: audit-apk.sh <apk>" >&2
  exit 2
fi

apk="$1"
audit_dir="$(mktemp -d)"
trap 'rm -rf "$audit_dir"' EXIT

if command -v unzip >/dev/null 2>&1; then
  unzip -qq -o "$apk" -d "$audit_dir"
elif command -v python3 >/dev/null 2>&1; then
  python3 -m zipfile -e "$apk" "$audit_dir"
else
  echo "APK audit requires unzip or Python 3" >&2
  exit 2
fi

forbidden_pattern='api\.openrouteservice\.org|api\.heigit\.org|api\.transitous\.org|tiles\.openfreemap\.org|org\.maplibre|organicmaps|"type":"service_account"|"private_key":|ANITABI_STORE_PASSWORD|ANITABI_KEY_PASSWORD|GOOGLE_SERVICE_ACCOUNT_JSON|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY'
if LC_ALL=C grep -a -E -r -q -- "$forbidden_pattern" "$audit_dir"; then
  echo "Forbidden legacy endpoint, SDK, server credential, or private-key marker found in APK" >&2
  LC_ALL=C grep -a -E -r -l -- "$forbidden_pattern" "$audit_dir" | sed -n '1,20p' >&2
  exit 1
fi

required_pattern='api\.anitabi\.afunnypersonlol0\.site'
if ! LC_ALL=C grep -a -E -r -q -- "$required_pattern" "$audit_dir"; then
  echo "The fixed Anitabi HTTPS backend endpoint is missing from the APK" >&2
  exit 1
fi

if find "$audit_dir" -type f \( -iname '*.jks' -o -iname '*.keystore' -o -iname '*.p12' -o -iname '*.pem' -o -iname '*.key' \) -print -quit | grep -q .; then
  echo "A key or keystore file was packaged in the APK" >&2
  exit 1
fi

echo "APK content audit passed"
