#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEST_DIR="${ROOT}/tests/1-test-agent-connects"
fail() { echo "ERROR: $*" >&2; exit 1; }

# shellcheck disable=SC1091
source "${ROOT}/tests/helpers.bash"
MANIFEST="${TEST_DIR}/manifest.yaml"

[[ -f "$MANIFEST" ]] || fail "missing manifest.yaml"
for old in teamcity.yaml controller.yaml node.yaml; do
  [[ ! -f "${TEST_DIR}/${old}" ]] || fail "remove ${old}"
done

grep -q 'teamcity_version: "2026.2.2"' "$MANIFEST" || fail "teamcity version"
grep -q 'name: teamcity' "$MANIFEST" || fail "teamcity host"
grep -q 'tester: true' "$MANIFEST" || fail "tester flag"
grep -q 'name: controller' "$MANIFEST" || fail "controller host"
grep -q 'name: node' "$MANIFEST" || fail "node host"
grep -q 'id: ubuntu-linux' "$MANIFEST" || fail "linux image id"
grep -q 'id: 13-L-ARM-macos' "$MANIFEST" || fail "mac image id"

[[ "$(yaml_host_value "$MANIFEST" teamcity port)" == "8111" ]] || fail "teamcity port"
[[ "$(yaml_host_value "$MANIFEST" teamcity profile_name)" == "anka-agent-connect" ]] || fail "profile name"
[[ "$(yaml_host_value "$MANIFEST" teamcity max_instances)" == "1" ]] || fail "max instances"
[[ "$(yaml_host_value "$MANIFEST" controller port)" == "80" ]] || fail "controller port"
[[ "$(yaml_host_value "$MANIFEST" controller registry_port)" == "8089" ]] || fail "registry port"
[[ "$(yaml_host_value "$MANIFEST" controller auth)" == "none" ]] || fail "auth"
[[ "$(yaml_host_value "$MANIFEST" node template_name)" == "26.1-arm64" ]] || fail "template name"
[[ "$(yaml_host_value "$MANIFEST" node template_tag)" == "vanilla+port-forward-22+brew-git" ]] || fail "template tag"
[[ "$(yaml_host_value "$MANIFEST" node ssh_user)" == "anka" ]] || fail "ssh user"
[[ "$(yaml_host_value "$MANIFEST" node ssh_password)" == "admin" ]] || fail "ssh password"
[[ "$(yaml_host_value "$MANIFEST" node ssh_port)" == "22" ]] || fail "ssh port"
[[ "$(yaml_host_value "$MANIFEST" node agent_path)" == "/Users/anka/buildAgent" ]] || fail "agent path"

if grep -R -n -E 'private_key_file|10\.8\.1\.200|COMPANY_REGISTRY' "${ROOT}/tests" --exclude='check-public-definition.bash'; then
  fail "public tests contain private data"
fi
echo "public definition ok"