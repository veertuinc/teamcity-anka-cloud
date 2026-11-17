#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEST_DIR="${ROOT}/tests/1-test-agent-connects"
fail() { echo "ERROR: $*" >&2; exit 1; }

[[ -f "${TEST_DIR}/manifest.yaml" ]] || fail "missing manifest.yaml"
[[ -f "${TEST_DIR}/teamcity.yaml" ]] || fail "missing teamcity.yaml"
[[ -f "${TEST_DIR}/controller.yaml" ]] || fail "missing controller.yaml"
[[ -f "${TEST_DIR}/node.yaml" ]] || fail "missing node.yaml"

grep -q 'teamcity_version: "2026.2.2"' "${TEST_DIR}/manifest.yaml" || fail "teamcity version"
grep -q 'name: teamcity' "${TEST_DIR}/manifest.yaml" || fail "teamcity host"
grep -q 'tester: true' "${TEST_DIR}/manifest.yaml" || fail "tester flag"
grep -q 'name: controller' "${TEST_DIR}/manifest.yaml" || fail "controller host"
grep -q 'name: node' "${TEST_DIR}/manifest.yaml" || fail "node host"
grep -q 'id: ubuntu-22.04-linux' "${TEST_DIR}/manifest.yaml" || fail "linux image id"
grep -q 'id: 13-L-ARM-macos' "${TEST_DIR}/manifest.yaml" || fail "mac image id"

grep -q 'port: 8111' "${TEST_DIR}/teamcity.yaml" || fail "teamcity port"
grep -q 'profile_name: anka-agent-connect' "${TEST_DIR}/teamcity.yaml" || fail "profile name"
grep -q 'max_instances: 1' "${TEST_DIR}/teamcity.yaml" || fail "max instances"

grep -q 'port: 80' "${TEST_DIR}/controller.yaml" || fail "controller port"
grep -q 'registry_port: 8089' "${TEST_DIR}/controller.yaml" || fail "registry port"
grep -q 'auth: none' "${TEST_DIR}/controller.yaml" || fail "auth"

grep -q 'template_name: 26.1-arm64' "${TEST_DIR}/node.yaml" || fail "template name"
grep -q 'template_tag: vanilla+port-forward-22+brew-git' "${TEST_DIR}/node.yaml" || fail "template tag"
grep -q 'ssh_user: anka' "${TEST_DIR}/node.yaml" || fail "ssh user"
grep -q 'ssh_port: 22' "${TEST_DIR}/node.yaml" || fail "ssh port"
grep -q 'agent_path: /Users/anka/buildAgent' "${TEST_DIR}/node.yaml" || fail "agent path"

if grep -R -n -E 'private_key_file|10\.8\.1\.200|ANKA_SSH_PASSWORD|COMPANY_REGISTRY' "${ROOT}/tests" --exclude='check-public-definition.bash'; then
  fail "public tests contain private data"
fi
echo "public definition ok"