#!/usr/bin/env bash
# Bump the plugin version in every file that hardcodes it.
# Usage: ./scripts/bump-version.sh <new-version> [old-version]
# If old-version is omitted, it is read from the root pom.xml.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

NEW_VERSION="${1:-}"
OLD_VERSION="${2:-}"

if [[ -z "$NEW_VERSION" ]]; then
  echo "Usage: $0 <new-version> [old-version]" >&2
  echo "Example: $0 1.12.0" >&2
  exit 1
fi

if [[ ! "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-].+)?$ ]]; then
  echo "Error: new version '$NEW_VERSION' does not look like MAJOR.MINOR.PATCH" >&2
  exit 1
fi

if [[ -z "$OLD_VERSION" ]]; then
  OLD_VERSION="$(sed -n 's|^[[:space:]]*<version>\([^<]*\)</version>.*|\1|p' pom.xml | head -1)"
fi

if [[ -z "$OLD_VERSION" ]]; then
  echo "Error: could not determine current version from pom.xml" >&2
  exit 1
fi

if [[ "$OLD_VERSION" == "$NEW_VERSION" ]]; then
  echo "Error: old and new version are the same ($NEW_VERSION)" >&2
  exit 1
fi

FILES=(
  pom.xml
  server/pom.xml
  agent/pom.xml
  common/pom.xml
  build/pom.xml
  teamcity-plugin.xml
  README.md
)

# Escape regex metacharacters in the old version for sed (versions are mostly digits/dots)
OLD_ESCAPED="${OLD_VERSION//./\\.}"
OLD_ESCAPED="${OLD_ESCAPED//+/\\+}"

replace_in_file() {
  local file="$1"
  if [[ "$(uname -s)" == Darwin ]]; then
    sed -i '' "s/${OLD_ESCAPED}/${NEW_VERSION}/g" "$file"
  else
    sed -i "s/${OLD_ESCAPED}/${NEW_VERSION}/g" "$file"
  fi
}

echo "Bumping plugin version: ${OLD_VERSION} -> ${NEW_VERSION}"

for file in "${FILES[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "  skip (missing): $file"
    continue
  fi
  if ! grep -qF "$OLD_VERSION" "$file"; then
    echo "  warn (no match): $file"
    continue
  fi
  replace_in_file "$file"
  echo "  updated: $file"
done

echo "Done. Run: mvn package"
echo "Expected zip: target/anka-build-cloud-teamcity-plugin-${NEW_VERSION}.zip"
