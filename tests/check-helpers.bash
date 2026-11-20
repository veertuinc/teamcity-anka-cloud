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
(
  CLOUD_PROFILE_ID="anka-1"
  INSTANCE_LIST_WAIT_SECONDS=1
  post_calls=0
  teamcity_request() {
    local method="$1"
    local path="$2"
    if [[ "$method" == "POST" && "$path" == "/app/rest/cloud/instances" ]]; then
      post_calls=$((post_calls + 1))
      printf 'HTTP_ERROR 500 POST /app/rest/cloud/instances\n' >&2
      return 22
    fi
    printf '%s' "$profile_instances"
  }
  start_one_vm "d792c6f6-198c-470f-9526-9c998efe7ab4"
  [[ "$INSTANCE_HREF" == "/new" ]]
  [[ "$post_calls" -eq 0 ]]
) || fail "listed instance"
(
  CLOUD_PROFILE_ID="anka-1"
  INSTANCE_LIST_WAIT_SECONDS=3
  REPORT_PATH="${tmp}/delayed-instance-report.txt"
  TEST_ID="agent-connects"
  CHECK_NAME="connected"
  post_calls=0
  get_count_file="${tmp}/delayed-gets"
  : > "$get_count_file"
  teamcity_request() {
    local method="$1"
    local path="$2"
    if [[ "$method" == "GET" && "$path" == "/app/rest/cloud/instances" ]]; then
      local seen
      seen="$(wc -l < "$get_count_file" | tr -d ' ')"
      printf 'x\n' >> "$get_count_file"
      if [[ "$seen" -eq 0 ]]; then
        printf '%s' '{"count":0}'
        return 0
      fi
      printf '%s' "$profile_instances"
      return 0
    fi
    if [[ "$method" == "POST" && "$path" == "/app/rest/cloud/instances" ]]; then
      post_calls=$((post_calls + 1))
      return 0
    fi
    return 22
  }
  start_one_vm "d792c6f6-198c-470f-9526-9c998efe7ab4"
  [[ "$INSTANCE_HREF" == "/new" ]]
  [[ "$post_calls" -eq 0 ]]
) || fail "delayed automatic instance"
(
  CLOUD_PROFILE_ID="anka-1"
  INSTANCE_LIST_WAIT_SECONDS=1
  REPORT_PATH="${tmp}/two-instances-report.txt"
  TEST_ID="agent-connects"
  CHECK_NAME="connected"
  two='{"count":2,"cloudInstance":[{"id":"id:one,imageId:img,profileId:anka-1","href":"/one","state":"scheduled_to_start"},{"id":"id:two,imageId:img,profileId:anka-1","href":"/two","state":"scheduled_to_start"}]}'
  teamcity_request() {
    printf '%s' "$two"
  }
  set +e
  (
    start_one_vm "d792c6f6-198c-470f-9526-9c998efe7ab4"
  )
  code=$?
  set -e
  [[ "$code" -eq 1 ]]
  grep -q 'FAIL|agent-connects|connected|instance count 2' "$REPORT_PATH"
) || fail "two instances"
(
  CLOUD_PROFILE_ID="anka-1"
  INSTANCE_LIST_WAIT_SECONDS=1
  REPORT_PATH="${tmp}/already-starting-report.txt"
  TEST_ID="agent-connects"
  CHECK_NAME="connected"
  post_calls=0
  teamcity_request() {
    local method="$1"
    local path="$2"
    if [[ "$method" == "GET" && "$path" == "/app/rest/cloud/instances" ]]; then
      if [[ "${post_calls:-0}" -eq 0 ]]; then
        printf '%s' '{"count":0}'
        return 0
      fi
      printf '%s' "$profile_instances"
      return 0
    fi
    if [[ "$method" == "POST" && "$path" == "/app/rest/cloud/instances" ]]; then
      post_calls=$((post_calls + 1))
      printf 'HTTP_ERROR 500 POST /app/rest/cloud/instances\n{"message":"Unable to start new agent: Waiting for 1 instance(s) to finish starting"}\n' >&2
      return 22
    fi
    return 22
  }
  start_one_vm "d792c6f6-198c-470f-9526-9c998efe7ab4"
  [[ "$INSTANCE_HREF" == "/new" ]]
  [[ "$post_calls" -eq 1 ]]
) || fail "already starting instance"
(
  CLOUD_PROFILE_ID="anka-1"
  INSTANCE_LIST_WAIT_SECONDS=1
  REPORT_PATH="${tmp}/start-failed-report.txt"
  TEST_ID="agent-connects"
  CHECK_NAME="connected"
  teamcity_request() {
    local method="$1"
    local path="$2"
    if [[ "$method" == "GET" && "$path" == "/app/rest/cloud/instances" ]]; then
      printf '%s' '{"count":0}'
      return 0
    fi
    printf 'HTTP_ERROR 500 POST /app/rest/cloud/instances\n{"message":"Max instances limit reached"}\n' >&2
    return 22
  }
  set +e
  (
    start_one_vm "d792c6f6-198c-470f-9526-9c998efe7ab4"
  )
  code=$?
  set -e
  [[ "$code" -eq 1 ]]
  grep -q 'FAIL|agent-connects|connected|start instance failed' "$REPORT_PATH"
) || fail "other start error"
(
  CLOUD_PROFILE_ID="anka-1"
  INSTANCE_LIST_WAIT_SECONDS=1
  REPORT_PATH="${tmp}/new-start-present-report.txt"
  TEST_ID="agent-connects"
  CHECK_NAME="connected"
  teamcity_request() {
    printf '%s' "$profile_instances"
  }
  set +e
  (
    start_new_vm "d792c6f6-198c-470f-9526-9c998efe7ab4"
  )
  code=$?
  set -e
  [[ "$code" -eq 1 ]]
  grep -q 'FAIL|agent-connects|connected|instance still present before start' "$REPORT_PATH"
) || fail "new start while instance present"
(
  CLOUD_PROFILE_ID="anka-1"
  INSTANCE_LIST_WAIT_SECONDS=1
  REPORT_PATH="${tmp}/new-start-waiting-report.txt"
  TEST_ID="agent-connects"
  CHECK_NAME="connected"
  teamcity_request() {
    local method="$1"
    local path="$2"
    if [[ "$method" == "GET" && "$path" == "/app/rest/cloud/instances" ]]; then
      printf '%s' '{"count":0}'
      return 0
    fi
    printf 'HTTP_ERROR 500 POST /app/rest/cloud/instances\n{"message":"Unable to start new agent: Waiting for 1 instance(s) to finish starting"}\n' >&2
    return 22
  }
  set +e
  (
    start_new_vm "d792c6f6-198c-470f-9526-9c998efe7ab4"
  )
  code=$?
  set -e
  [[ "$code" -eq 1 ]]
  grep -q 'FAIL|agent-connects|connected|instance still starting' "$REPORT_PATH"
) || fail "new start while instance starting"
(
  CLOUD_PROFILE_ID="anka-1"
  INSTANCE_LIST_WAIT_SECONDS=1
  post_calls=0
  teamcity_request() {
    local method="$1"
    local path="$2"
    if [[ "$method" == "GET" && "$path" == "/app/rest/cloud/instances" ]]; then
      if [[ "$post_calls" -eq 0 ]]; then
        printf '%s' '{"count":0}'
        return 0
      fi
      printf '%s' "$profile_instances"
      return 0
    fi
    if [[ "$method" == "POST" && "$path" == "/app/rest/cloud/instances" ]]; then
      post_calls=$((post_calls + 1))
      return 0
    fi
    return 22
  }
  start_new_vm "d792c6f6-198c-470f-9526-9c998efe7ab4"
  [[ "$INSTANCE_HREF" == "/new" ]]
  [[ "$post_calls" -eq 1 ]]
) || fail "new start sends one request"
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

# TeamCity 2026.2 rejects a build configuration in _Root.
# The build type goes in child project AnkaLive. A pool is linked when one exists.
(
  TEST_ID="build-stops-vm"
  CHECK_NAME="removed"
  REPORT_PATH="${tmp}/build-type-report.txt"
  calls_file="${tmp}/build-type-calls"
  project_list_body='{"count":0}'
  teamcity_request() {
    local method="$1"
    local path="$2"
    local body="${3:-}"
    printf '%s %s %s\n' "$method" "$path" "$body" >> "$calls_file"
    if [[ "$method" == "GET" && "$path" == "/app/rest/projects?locator=id:AnkaLive" ]]; then
      printf '%s' "$project_list_body"
      return 0
    fi
    if [[ "$method" == "DELETE" && "$path" == "/app/rest/projects/id:AnkaLive" ]]; then
      return 0
    fi
    if [[ "$method" == "POST" && "$path" == "/app/rest/projects" ]]; then
      printf '%s' '{"id":"AnkaLive","name":"anka-live"}'
      return 0
    fi
    if [[ "$method" == "POST" && "$path" == "/app/rest/agentPools/id:7/projects" ]]; then
      printf '%s' '{"id":"AnkaLive"}'
      return 0
    fi
    if [[ "$method" == "POST" && "$path" == "/app/rest/buildTypes" ]]; then
      printf '%s' '{"id":"bt-live"}'
      return 0
    fi
    return 22
  }
  check_build_type_calls() {
    python3 - "$calls_file" "$1" "$2" <<'PY'
import json, sys
path, expect_delete, with_pool = sys.argv[1:]
rows = []
for line in open(path, encoding="utf-8"):
    method, rest = line.rstrip("\n").split(" ", 1)
    req_path, _, body = rest.partition(" ")
    rows.append((method, req_path, body))

def one(method, req_path):
    matches = [row for row in rows if row[0] == method and row[1] == req_path]
    if len(matches) != 1:
        raise SystemExit(f"expected one {method} {req_path}, got {len(matches)}")
    return matches[0]

project = json.loads(one("POST", "/app/rest/projects")[2])
assert project["id"] == "AnkaLive", project
assert project["name"] == "anka-live", project
assert project["parentProject"]["locator"] == "id:_Root", project
build = json.loads(one("POST", "/app/rest/buildTypes")[2])
assert build["name"] == "live-build", build
assert build["project"]["id"] == "AnkaLive", build
step = build["steps"]["step"][0]
props = {item["name"]: item["value"] for item in step["properties"]["property"]}
assert step["type"] == "simpleRunner"
assert props["script.content"] == "sleep 20; echo live-build-ok"
assert props["teamcity.step.mode"] == "default"
assert props["use.custom.script"] == "true"
deletes = [row for row in rows if row[0] == "DELETE" and row[1] == "/app/rest/projects/id:AnkaLive"]
if expect_delete == "yes":
    if len(deletes) != 1:
        raise SystemExit("missing project delete")
    if rows.index(deletes[0]) > rows.index(one("POST", "/app/rest/projects")):
        raise SystemExit("project delete after create")
elif deletes:
    raise SystemExit("unexpected project delete")
pools = [row for row in rows if row[0] == "POST" and row[1] == "/app/rest/agentPools/id:7/projects"]
if with_pool == "yes":
    if len(pools) != 1 or json.loads(pools[0][2])["id"] != "AnkaLive":
        raise SystemExit("missing pool link")
    if not (rows.index(one("POST", "/app/rest/projects")) < rows.index(pools[0]) < rows.index(one("POST", "/app/rest/buildTypes"))):
        raise SystemExit("pool link order")
elif pools:
    raise SystemExit("unexpected pool link")
PY
  }
  : > "$calls_file"
  BUILD_PROJECT_ID=""
  AGENT_POOL_ID="7"
  prepare_build_project || exit 1
  [[ "$(create_build_type "live-build" "sleep 20; echo live-build-ok")" == "bt-live" ]] || exit 1
  check_build_type_calls no yes || exit 1
  : > "$calls_file"
  [[ "$(create_build_type "max-hold" "sleep 180")" == "bt-live" ]] || exit 1
  python3 - "$calls_file" <<'PY' || exit 1
import sys
text = open(sys.argv[1], encoding="utf-8").read()
if "POST /app/rest/projects " in text or "GET /app/rest/projects" in text:
    raise SystemExit("second build type created another project")
if "POST /app/rest/buildTypes " not in text:
    raise SystemExit("second build type was not created")
PY
  : > "$calls_file"
  BUILD_PROJECT_ID=""
  AGENT_POOL_ID=""
  project_list_body='{"count":1,"project":{"id":"AnkaLive","name":"anka-live"}}'
  prepare_build_project || exit 1
  [[ "$(create_build_type "live-build" "sleep 20; echo live-build-ok")" == "bt-live" ]] || exit 1
  check_build_type_calls yes no || exit 1
) || fail "build type project"
echo "helpers ok"