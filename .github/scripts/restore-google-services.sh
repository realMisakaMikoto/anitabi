#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
destination="$repo_root/app/google-services.json"

if [[ -n "${ANITABI_GOOGLE_SERVICES_JSON_BASE64:-}" ]]; then
  printf '%s' "$ANITABI_GOOGLE_SERVICES_JSON_BASE64" | base64 --decode > "$destination"
  exit 0
fi

if [[ "${1:-}" == "--require-real" ]]; then
  echo "ANITABI_GOOGLE_SERVICES_JSON_BASE64 is required for release builds" >&2
  exit 1
fi

if [[ "${CI:-}" != "true" ]]; then
  echo "Refusing to replace a local Firebase configuration with the CI placeholder" >&2
  exit 1
fi

cat > "$destination" <<'JSON'
{
  "project_info": {
    "project_number": "0",
    "project_id": "anitabi-ci-placeholder",
    "storage_bucket": "anitabi-ci-placeholder.invalid"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:0:android:0000000000000000",
        "android_client_info": {
          "package_name": "cn.anitabi.navigator"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "ci-placeholder-not-a-real-key"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
JSON
