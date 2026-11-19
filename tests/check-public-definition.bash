#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fail() { echo "ERROR: $*" >&2; exit 1; }

# shellcheck disable=SC1091
source "${ROOT}/tests/helpers.bash"

check_manifest() {
  local dir="$1"
  local profile_name="$2"
  local manifest="${ROOT}/tests/${dir}/manifest.yaml"
  [[ -f "$manifest" ]] || fail "${dir} missing manifest.yaml"
  [[ -f "${ROOT}/tests/${dir}/test.bash" ]] || fail "${dir} missing test.bash"
  for old in teamcity.yaml controller.yaml node.yaml; do
    [[ ! -f "${ROOT}/tests/${dir}/${old}" ]] || fail "${dir} remove ${old}"
  done
  grep -q 'teamcity_version: "2026.2.2"' "$manifest" || fail "${dir} teamcity version"
  grep -q 'name: teamcity' "$manifest" || fail "${dir} teamcity host"
  grep -q 'tester: true' "$manifest" || fail "${dir} tester flag"
  grep -q 'name: controller' "$manifest" || fail "${dir} controller host"
  grep -q 'name: node' "$manifest" || fail "${dir} node host"
  grep -q 'id: ubuntu-linux' "$manifest" || fail "${dir} linux image id"
  grep -q 'id: 13-L-ARM-macos' "$manifest" || fail "${dir} mac image id"
  [[ "$(yaml_host_value "$manifest" teamcity port)" == "8111" ]] || fail "${dir} teamcity port"
  [[ "$(yaml_host_value "$manifest" teamcity profile_name)" == "$profile_name" ]] || fail "${dir} profile name"
  [[ "$(yaml_host_value "$manifest" teamcity max_instances)" == "1" ]] || fail "${dir} max instances"
  [[ "$(yaml_host_value "$manifest" controller port)" == "80" ]] || fail "${dir} controller port"
  [[ "$(yaml_host_value "$manifest" controller registry_port)" == "8089" ]] || fail "${dir} registry port"
  [[ "$(yaml_host_value "$manifest" controller auth)" == "none" ]] || fail "${dir} auth"
  [[ "$(yaml_host_value "$manifest" node template_name)" == "26.1-arm64" ]] || fail "${dir} template name"
  [[ "$(yaml_host_value "$manifest" node template_tag)" == "vanilla+port-forward-22+brew-git+azul-jre-21" ]] || fail "${dir} template tag"
  [[ "$(yaml_host_value "$manifest" node ssh_user)" == "anka" ]] || fail "${dir} ssh user"
  [[ "$(yaml_host_value "$manifest" node ssh_password)" == "admin" ]] || fail "${dir} ssh password"
  [[ "$(yaml_host_value "$manifest" node ssh_port)" == "22" ]] || fail "${dir} ssh port"
  [[ "$(yaml_host_value "$manifest" node agent_path)" == "/Users/anka/buildAgent" ]] || fail "${dir} agent path"
}

check_manifest "1-test-agent-connects" "anka-agent-connect"
check_manifest "2-test-build-stops-vm" "anka-build-stops"
check_manifest "3-test-max-instances" "anka-max-instances"
check_manifest "4-test-bad-ssh" "anka-bad-ssh-password"
check_manifest "5-test-missing-template" "anka-missing-template"

build_manifest="${ROOT}/tests/2-test-build-stops-vm/manifest.yaml"
[[ "$(yaml_host_value "$build_manifest" teamcity vm_name_template)" == 'tc-build-$ts' ]] || fail "vm name template"
[[ "$(yaml_host_value "$build_manifest" teamcity vcpu_count)" == "2" ]] || fail "vcpu"
[[ "$(yaml_host_value "$build_manifest" teamcity ram_size)" == "4096" ]] || fail "ram"
[[ "$(yaml_host_value "$build_manifest" teamcity terminate_after_build)" == "true" ]] || fail "terminate after build"
[[ "$(yaml_host_value "$build_manifest" teamcity terminate_idle_time)" == "1" ]] || fail "terminate idle time"

if grep -R -n -E 'private_key_file|10\.8\.1\.200|COMPANY_REGISTRY' "${ROOT}/tests" --exclude='check-public-definition.bash'; then
  fail "public tests contain private data"
fi
echo "public definition ok"
