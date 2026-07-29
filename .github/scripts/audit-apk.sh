#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ! -f "$1" ]]; then
  echo "Usage: audit-apk.sh <apk>" >&2
  exit 2
fi

apk="$1"
audit_dir="$(mktemp -d)"
trap 'rm -rf "$audit_dir"' EXIT

unzip -qq -o "$apk" -d "$audit_dir"

forbidden_pattern='api\.openrouteservice\.org|organicmaps|com\.google\.android\.gms\.analytics|com\.android\.billingclient|com\.google\.firebase|ANITABI_STORE_PASSWORD|ANITABI_KEY_PASSWORD|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY'
if find "$audit_dir" -type f -print0 | xargs -0 strings -a | grep -Eiq "$forbidden_pattern"; then
  echo "Forbidden endpoint, SDK, secret name, or private-key marker found in APK" >&2
  find "$audit_dir" -type f -print0 | xargs -0 strings -a | grep -Ei "$forbidden_pattern" | head -20 >&2
  exit 1
fi

if find "$audit_dir" -type f \( -iname '*.jks' -o -iname '*.keystore' -o -iname '*.p12' -o -iname '*.pem' -o -iname '*.key' \) -print -quit | grep -q .; then
  echo "A key or keystore file was packaged in the APK" >&2
  exit 1
fi

echo "APK content audit passed"
