#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
source "${ROOT}/tests/helpers.bash"
fail() { echo "ERROR: $*" >&2; exit 1; }

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cat > "${tmp}/hosts.yaml" <<'EOF'
tests:
  - name: "Agent connects"
    hosts:
      - name: teamcity
        port: 8111
        profile_name: anka-agent-connect
      - name: controller
        port: 80
EOF
[[ "$(yaml_host_value "${tmp}/hosts.yaml" teamcity port)" == "8111" ]] || fail "teamcity port"
[[ "$(yaml_host_value "${tmp}/hosts.yaml" controller port)" == "80" ]] || fail "controller port"
[[ "$(yaml_host_value "${tmp}/hosts.yaml" teamcity profile_name)" == "anka-agent-connect" ]] || fail "profile name"

body='[{"id":"abc","name":"26.1-arm64"}]'
json_has_name "$body" "26.1-arm64" || fail "name"
json_has_tag '["vanilla+port-forward-22+brew-git"]' "vanilla+port-forward-22+brew-git" || fail "tag"
if json_has_name "$body" "missing"; then fail "false name"; fi

json="$(build_profile_json \
  "http://10.0.0.2" "http://10.0.0.3:8111" "anka" "secret" "22" \
  "/Users/anka/buildAgent" "26.1-arm64" "vanilla+port-forward-22+brew-git" \
  "d792c6f6-198c-470f-9526-9c998efe7ab4" "1" "anka-agent-connect" 'tc-test-$ts')"
python3 - "$json" <<'PY' || fail "profile json"
import json, sys
data = json.loads(sys.argv[1])
props = {item["name"]: item["value"] for item in data["properties"]["property"]}
assert data["id"] == "anka-agent-connect"
assert data["type"] == "CloudProfile"
assert props["cloud-code"] == "anka"
assert props["name"] == "anka-agent-connect"
assert props["clouds.anka.url"] == "http://10.0.0.2"
assert props["profileServerUrl"] == "http://10.0.0.3:8111"
assert props["ssh_password"] == "secret"
assert props["image_id"] == "d792c6f6-198c-470f-9526-9c998efe7ab4"
assert props["VM_NAME_TEMPLATE"] == "tc-test-$ts"
assert "auth.method" not in props
assert "VCPU_COUNT" not in props
PY

start="$(build_start_instance_json anka-agent-connect d792c6f6-198c-470f-9526-9c998efe7ab4)"
[[ "$start" == *'profileId:anka-agent-connect,id:d792c6f6-198c-470f-9526-9c998efe7ab4'* ]] || fail "start json"

ready='{"state":"running","href":"/app/rest/cloud/instances/id:1","agent":{"id":9,"connected":true,"authorized":true}}'
[[ "$(instance_state "$ready")" == "running" ]] || fail "state"
agent_connected "$ready" || fail "connected"
agent_authorized "$ready" || fail "authorized"
pending='{"state":"starting","agent":{"id":9,"connected":true,"authorized":false}}'
if agent_authorized "$pending"; then fail "pending should be unauthorized"; fi
[[ "$(report_line PASS "")" == "PASS|agent-connects|connected|" ]] || fail "pass line"
[[ "$(report_line FAIL "agent still disconnected after 15 minutes")" == "FAIL|agent-connects|connected|agent still disconnected after 15 minutes" ]] || fail "fail line"
(
  TEST_ID="other-test"
  CHECK_NAME="ready"
  [[ "$(report_line PASS "")" == "PASS|other-test|ready|" ]]
) || fail "custom report"

listed='note[{"id":"abc","name":"26.1-arm64"}]tail'
[[ "$(template_id_for_name "$listed" "26.1-arm64")" == "abc" ]] || fail "template id"
[[ "$(json_href "$ready")" == "/app/rest/cloud/instances/id:1" ]] || fail "href"
[[ "$(json_href "$(instance_for_profile "$ready" "anka-1")")" == "/app/rest/cloud/instances/id:1" ]] || fail "bare instance"
profile_instances='{"count":2,"cloudInstance":[{"id":"id:old,imageId:img,profileId:anka-10","href":"/old","state":"running"},{"id":"id:new,imageId:img,profileId:anka-1","href":"/new","state":"scheduled_to_start"}]}'
[[ "$(json_href "$(instance_for_profile "$profile_instances" "anka-1")")" == "/new" ]] || fail "profile instance"
single='{"count":1,"cloudInstance":{"id":"id:one,imageId:img,profileId:anka-1","href":"/one","state":"running"}}'
[[ "$(json_href "$(instance_for_profile "$single" "anka-1")")" == "/one" ]] || fail "single object"
if instance_for_profile '{"count":0}' "anka-1"; then fail "empty list"; fi
if instance_for_profile '{"count":0,"href":"/app/rest/cloud/instances"}' "anka-1"; then fail "empty collection"; fi
(
  TEAMCITY_DRY_RUN=1
  CLOUD_PROFILE_ID="anka-1"
  INSTANCE_LIST_WAIT_SECONDS=1
  REPORT_PATH="${tmp}/empty-start-report.txt"
  calls=0
  dry_run_body() {
    calls=$((calls + 1))
    if [[ "$calls" -eq 1 ]]; then
      return 0
    fi
    printf '%s' "$profile_instances"
  }
  start_one_vm "d792c6f6-198c-470f-9526-9c998efe7ab4"
  [[ "$INSTANCE_HREF" == "/new" ]]
) || fail "empty start response"
[[ "$(json_agent_id "$ready")" == "9" ]] || fail "agent id"
[[ "$(json_agent_id '{"state":"starting"}')" == "" ]] || fail "missing agent id"

extra="$(build_profile_json \
  "http://10.0.0.2" "http://10.0.0.3:8111" "anka" "secret" "22" \
  "/Users/anka/buildAgent" "26.1-arm64" "vanilla+port-forward-22+brew-git" \
  "d792c6f6-198c-470f-9526-9c998efe7ab4" "1" "anka-agent-connect" 'tc-test-$ts' \
  "2" "4096" "group-1" "7" "true" "1")"
python3 - "$extra" <<'PY' || fail "extra profile json"
import json, sys
data = json.loads(sys.argv[1])
props = {item["name"]: item["value"] for item in data["properties"]["property"]}
assert props["VCPU_COUNT"] == "2"
assert props["RAM_SIZE"] == "4096"
assert props["group_id"] == "group-1"
assert props["agentPoolId"] == "7"
assert props["terminate-after-build"] == "true"
assert props["terminate-idle-time"] == "1"
assert props["VM_NAME_TEMPLATE"] == "tc-test-$ts"
PY

(
  TEST_ID="build-stops-vm"
  CHECK_NAME="removed"
  [[ "$(report_line PASS "")" == "PASS|build-stops-vm|removed|" ]]
) || fail "build report"
(
  TEST_ID="max-instances"
  CHECK_NAME="one-vm"
  [[ "$(report_line PASS "")" == "PASS|max-instances|one-vm|" ]]
) || fail "max report"
(
  TEST_ID="bad-ssh"
  CHECK_NAME="terminated"
  [[ "$(report_line PASS "")" == "PASS|bad-ssh|terminated|" ]]
) || fail "ssh report"
(
  TEST_ID="missing-template"
  CHECK_NAME="absent"
  [[ "$(report_line PASS "")" == "PASS|missing-template|absent|" ]]
) || fail "missing report"

(
  printf 'one\n' > "${tmp}/instances.body"
  printf 'two\n' > "${tmp}/instances.2.body"
  FIXTURE_DIR="$tmp"
  [[ "$(dry_run_body GET /app/rest/cloud/instances)" == "one" ]] || exit 1
  [[ "$(dry_run_body GET /app/rest/cloud/instances)" == "one" ]] || exit 1
  advance_fixture instances
  [[ "$(dry_run_body GET /app/rest/cloud/instances)" == "two" ]] || exit 1
) || fail "fixture cursor"
echo "helpers ok"