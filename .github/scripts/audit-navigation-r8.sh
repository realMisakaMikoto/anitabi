#!/usr/bin/env bash
set -euo pipefail

mapping_dir="${1:-app/build/outputs/mapping/release}"
class_name="com.google.android.gms.maps.internal.CreatorImpl"
mapping_file="$mapping_dir/mapping.txt"
seeds_file="$mapping_dir/seeds.txt"
usage_file="$mapping_dir/usage.txt"
configuration_file="$mapping_dir/configuration.txt"

for required_file in "$mapping_file" "$seeds_file" "$usage_file" "$configuration_file"; do
  if [[ ! -s "$required_file" ]]; then
    echo "Missing release R8 output: $required_file" >&2
    exit 1
  fi
done

mapping_header="$class_name -> $class_name:"
if ! grep -Fqx "$mapping_header" "$mapping_file"; then
  echo "Navigation map creator class name was not retained by R8" >&2
  exit 1
fi

mapping_block="$({
  awk -v header="$mapping_header" '
    $0 == header { inside = 1; next }
    inside && $0 !~ /^[[:space:]#]/ { exit }
    inside { print }
  ' "$mapping_file"
} || true)"
if ! grep -Eq 'void <init>\(\).* -> <init>$' <<<"$mapping_block"; then
  echo "Navigation map creator zero-argument constructor is absent after R8" >&2
  exit 1
fi

if ! grep -Fq "$class_name: CreatorImpl()" "$seeds_file"; then
  echo "Navigation map creator constructor is not an R8 seed" >&2
  exit 1
fi

removed_constructor="$({
  awk -v header="$class_name:" '
    $0 == header { inside = 1; next }
    inside && $0 !~ /^[[:space:]]/ { inside = 0 }
    inside && $0 ~ /^[[:space:]]+public void <init>\(\)$/ { print }
  ' "$usage_file"
} || true)"
if [[ -n "$removed_constructor" ]]; then
  echo "Navigation map creator constructor was removed by R8" >&2
  exit 1
fi

for disabled_optimization in \
  '!class/merging/horizontal' \
  '!class/merging/vertical'; do
  if ! grep -Eq "^[[:space:]]*-optimizations[[:space:]]+${disabled_optimization}([[:space:]]|$)" \
    "$configuration_file"; then
    echo "Missing Navigation SDK R8 class-merging exclusion" >&2
    exit 1
  fi
done

registry_name="com.google.android.libraries.navigation.internal.als.ax"
if ! grep -Fqx "$registry_name -> $registry_name:" "$mapping_file"; then
  echo "Navigation SDK reflective registry moved out of its package" >&2
  exit 1
fi

if grep -R -n --include='*.kt' 'MapsInitializer' app/src/main; then
  echo "Application source must not call MapsInitializer with Navigation SDK" >&2
  exit 1
fi

echo "Navigation R8 reflection audit passed"
