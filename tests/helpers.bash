#!/usr/bin/env bash
yaml_host_value() {
  local file="$1"
  local host="$2"
  local key="$3"
  python3 - "$file" "$host" "$key" <<'PY'
import sys
path, host, key = sys.argv[1], sys.argv[2], sys.argv[3]
in_host = False
host_indent = None
with open(path, encoding="utf-8") as handle:
    for raw in handle:
        line = raw.split("#", 1)[0].rstrip()
        if not line.strip():
            continue
        indent = len(line) - len(line.lstrip(" "))
        stripped = line.strip()
        if stripped.startswith("- name:"):
            name = stripped.split(":", 1)[1].strip().strip('"').strip("'")
            in_host = name == host
            host_indent = indent
            continue
        if not in_host or indent <= host_indent:
            in_host = False
            continue
        name, sep, value = stripped.partition(":")
        if sep and name.strip() == key:
            print(value.strip().strip('"').strip("'"))
            sys.exit(0)
sys.exit(1)
PY
}

json_has_name() {
  python3 - "$1" "$2" <<'PY'
import sys
body, name = sys.argv[1], sys.argv[2]
sys.exit(0 if f'"name":"{name}"' in body or f'"name": "{name}"' in body else 1)
PY
}

json_has_tag() {
  python3 - "$1" "$2" <<'PY'
import sys
sys.exit(0 if sys.argv[2] in sys.argv[1] else 1)
PY
}

build_profile_json() {
  python3 - "$@" <<'PY'
import json, sys
names = [
    "controller_url", "teamcity_url", "ssh_user", "ssh_password", "ssh_port",
    "agent_path", "template_name", "template_tag", "template_id",
    "max_instances", "profile_name", "vm_name_template",
]
user = sys.argv[1:]
if len(user) < 12 or len(user) > 18:
    sys.exit("build_profile_json: expected 12 arguments")
values = dict(zip(names, user[:12]))
if any(value == "" for value in values.values()):
    sys.exit("build_profile_json: empty argument")
pairs = [
    ("enabled", "true"),
    ("profileId", values["profile_name"]),
    ("name", values["profile_name"]),
    ("description", ""),
    ("cloud-code", "anka"),
    ("total-work-time", ""),
    ("next-hour", ""),
    ("clouds.anka.url", values["controller_url"]),
    ("skipTLSVerification", "false"),
    ("profileServerUrl", values["teamcity_url"]),
    ("ssh_user", values["ssh_user"]),
    ("ssh_password", values["ssh_password"]),
    ("sshForwardingPort", values["ssh_port"]),
    ("agent_path", values["agent_path"]),
    ("TEMPLATE_NAME", values["template_name"]),
    ("TEMPLATE_TAG", values["template_tag"]),
    ("image_id", values["template_id"]),
    ("clouds.anka.maxInstances", values["max_instances"]),
    ("VM_NAME_TEMPLATE", values["vm_name_template"]),
]
optional_names = [
    "VCPU_COUNT",
    "RAM_SIZE",
    "group_id",
    "agentPoolId",
    "terminate-after-build",
    "terminate-idle-time",
]
for name, value in zip(optional_names, user[12:]):
    if value != "":
        pairs.append((name, value))
print(json.dumps({
    "id": values["profile_name"],
    "type": "CloudProfile",
    "properties": {"property": [{"name": name, "value": value} for name, value in pairs]},
}))
PY
}

build_start_instance_json() {
  local profile_name="$1"
  local template_id="$2"
  python3 - "$profile_name" "$template_id" <<'PY'
import json, sys
profile, template_id = sys.argv[1], sys.argv[2]
print(json.dumps({"image": {"id": f"profileId:{profile},id:{template_id}"}}))
PY
}

instance_state() {
  python3 - "$1" <<'PY'
import json, sys
print(json.loads(sys.argv[1]).get("state", ""))
PY
}

agent_connected() {
  python3 - "$1" <<'PY'
import json, sys
try:
    data = json.loads(sys.argv[1]) if sys.argv[1].strip() else {}
except json.JSONDecodeError:
    sys.exit(1)
agent = data.get("agent") or {}
sys.exit(0 if agent.get("connected") is True else 1)
PY
}

agent_authorized() {
  python3 - "$1" <<'PY'
import json, sys
try:
    data = json.loads(sys.argv[1]) if sys.argv[1].strip() else {}
except json.JSONDecodeError:
    sys.exit(1)
agent = data.get("agent") or {}
sys.exit(0 if agent.get("authorized") is True else 1)
PY
}

report_line() {
  local status="$1"
  local reason="${2:-}"
  local test_id="${TEST_ID:-agent-connects}"
  local check="${CHECK_NAME:-connected}"
  printf '%s|%s|%s|%s\n' "$status" "$test_id" "$check" "$reason"
}

begin_live_test() {
  MANIFEST="${1:?}"
  : "${TEST_ID:=agent-connects}"
  : "${CHECK_NAME:=connected}"
  : "${REPORT_PATH:=/tmp/test-reports/test-report-teamcity.txt}"
  : "${IMAGE_WAIT_SECONDS:=300}"
  : "${AGENT_WAIT_SECONDS:=900}"
  : "${BUILD_WAIT_SECONDS:=900}"
  : "${REMOVE_WAIT_SECONDS:=600}"
  : "${SSH_GONE_WAIT_SECONDS:=180}"
  : "${PATH_WAIT_SECONDS:=90}"
  : "${HOLD_SECONDS:=180}"
  : "${POLL_SECONDS:=5}"
  : "${PROFILE_SETTINGS:=/plugins/anka-build-cloud-teamcity-plugin/profile-settings.html}"
  if [[ -z "${VM_NAME_TEMPLATE:-}" ]]; then
    VM_NAME_TEMPLATE='tc-test-$ts'
  fi
  INSTANCE_HREF="${INSTANCE_HREF:-}"
  INSTANCE_BODY="${INSTANCE_BODY:-}"
  mkdir -p "$(dirname "$REPORT_PATH")"
  read_manifest
  require_runner_env
}

assign_optional_manifest_value() {
  local host="$1"
  local key="$2"
  local var_name="$3"
  local value
  value="$(yaml_host_value "$MANIFEST" "$host" "$key" || true)"
  if [[ -n "$value" ]]; then
    printf -v "$var_name" '%s' "$value"
  fi
}

read_manifest() {
  TEMPLATE_NAME="$(yaml_host_value "$MANIFEST" node template_name)"
  TEMPLATE_TAG="$(yaml_host_value "$MANIFEST" node template_tag)"
  SSH_USER="$(yaml_host_value "$MANIFEST" node ssh_user)"
  SSH_PASSWORD="$(yaml_host_value "$MANIFEST" node ssh_password)"
  SSH_PORT="$(yaml_host_value "$MANIFEST" node ssh_port)"
  AGENT_PATH="$(yaml_host_value "$MANIFEST" node agent_path)"
  PROFILE_NAME="$(yaml_host_value "$MANIFEST" teamcity profile_name)"
  MAX_INSTANCES="$(yaml_host_value "$MANIFEST" teamcity max_instances)"
  assign_optional_manifest_value teamcity vm_name_template VM_NAME_TEMPLATE
  assign_optional_manifest_value teamcity vcpu_count PROFILE_VCPU
  assign_optional_manifest_value teamcity ram_size PROFILE_RAM
  assign_optional_manifest_value teamcity terminate_after_build PROFILE_TERMINATE_AFTER_BUILD
  assign_optional_manifest_value teamcity terminate_idle_time PROFILE_TERMINATE_IDLE_TIME
}

require_runner_env() {
  : "${TEAMCITY_URL:?}"
  : "${TEAMCITY_TOKEN:?}"
  : "${CONTROLLER_URL:?}"
}

finish_fail() {
  local reason="$1"
  report_line FAIL "$reason" > "$REPORT_PATH"
  echo "FAIL: ${reason}" >&2
  exit 1
}

teamcity_request() {
  local method="$1"
  local path="$2"
  local body="${3:-}"
  if [[ "${TEAMCITY_DRY_RUN:-}" == "1" ]]; then
    dry_run_body "$method" "$path"
    return 0
  fi
  local response_file http_code
  response_file="$(mktemp)"
  local args=(-sS -o "$response_file" -w '%{http_code}' -X "$method" -H "Authorization: Bearer ${TEAMCITY_TOKEN}" -H "Accept: application/json")
  if [[ -n "$body" ]]; then
    args+=(-H "Content-Type: application/json" --data "$body")
  fi
  http_code="$(curl "${args[@]}" "${TEAMCITY_URL}${path}" || true)"
  if [[ ! "$http_code" =~ ^2 ]]; then
    {
      printf 'HTTP_ERROR %s %s %s\n' "${http_code:-000}" "$method" "$path"
      cat "$response_file"
      printf '\n'
    } | tee -a /tmp/teamcity-http-error.txt >&2
    rm -f "$response_file"
    return 22
  fi
  cat "$response_file"
  rm -f "$response_file"
}

# Each fixture key has a cursor. Cursor 1 reads <key>.body.
# advance_fixture moves that key to the next numbered file.
fixture_cursor_name() {
  local key="$1"
  printf 'FIXTURE_CURSOR_%s' "${key//-/_}"
}

fixture_cursor_get() {
  local name value
  name="$(fixture_cursor_name "$1")"
  value="${!name-}"
  if [[ -z "$value" ]]; then
    printf '%s\n' "1"
  else
    printf '%s\n' "$value"
  fi
}

advance_fixture() {
  local key="$1"
  local name current
  name="$(fixture_cursor_name "$key")"
  current="$(fixture_cursor_get "$key")"
  printf -v "$name" '%s' "$((current + 1))"
}

fixture_key() {
  local method="$1"
  local path="$2"
  if [[ "$path" == *"/actions/forceStop"* ]]; then
    printf '%s\n' "force-stop"
    return 0
  fi
  if [[ "$path" == *"get=templates"* ]]; then
    printf '%s\n' "templates"
    return 0
  fi
  if [[ "$path" == *"get=tags"* ]]; then
    printf '%s\n' "tags"
    return 0
  fi
  if [[ "$path" == *"/app/rest/cloud/images"* ]]; then
    printf '%s\n' "images"
    return 0
  fi
  if [[ "$path" == *"/app/rest/agents/"* ]]; then
    printf '%s\n' "agent"
    return 0
  fi
  if [[ "$method" == "POST" && "$path" == "/app/rest/cloud/instances" ]]; then
    printf '%s\n' "instances-start"
    return 0
  fi
  if [[ "$method" == "GET" && ( "$path" == "/app/rest/cloud/instances" || "$path" == /app/rest/cloud/instances\?* ) ]]; then
    printf '%s\n' "instances"
    return 0
  fi
  if [[ "$method" == "POST" && "$path" == "/app/rest/agentPools" ]]; then
    printf '%s\n' "agent-pools"
    return 0
  fi
  if [[ "$method" == "GET" && "$path" == "/app/rest/agentPools"* ]]; then
    printf '%s\n' "agent-pools-list"
    return 0
  fi
  if [[ "$method" == "POST" && "$path" == "/app/rest/buildTypes" ]]; then
    printf '%s\n' "build-types"
    return 0
  fi
  if [[ "$method" == "POST" && "$path" == "/app/rest/buildQueue" ]]; then
    printf '%s\n' "build-queue-post"
    return 0
  fi
  if [[ "$method" == "GET" && "$path" == *"/app/rest/buildQueue"* ]]; then
    printf '%s\n' "build-queue"
    return 0
  fi
  if [[ "$method" == "GET" && "$path" == *"/app/rest/builds"* ]]; then
    printf '%s\n' "builds"
    return 0
  fi
  if [[ "$method" == "POST" && "$path" == "/api/v1/group" ]]; then
    printf '%s\n' "group"
    return 0
  fi
  if [[ "$method" == "GET" && "$path" == "/api/v1/group"* ]]; then
    printf '%s\n' "group-list"
    return 0
  fi
  if [[ "$path" == *"/api/v1/node/group"* ]]; then
    printf '%s\n' "node-group"
    return 0
  fi
  if [[ "$method" == "GET" && "$path" == "/api/v1/node"* ]]; then
    printf '%s\n' "nodes"
    return 0
  fi
  if [[ "$method" == "GET" && "$path" == *"/api/v1/vm?id="* ]]; then
    printf '%s\n' "vm-one"
    return 0
  fi
  if [[ "$method" == "GET" && "$path" == "/api/v1/vm" ]]; then
    printf '%s\n' "vm-list"
    return 0
  fi
  printf '%s\n' "instance"
}

fixture_file_for() {
  local key="$1"
  local cursor="$2"
  local dir="${FIXTURE_DIR:?}"
  local exact best n
  if [[ "$cursor" -eq 1 ]]; then
    exact="${dir}/${key}.body"
  else
    exact="${dir}/${key}.${cursor}.body"
  fi
  if [[ -f "$exact" ]]; then
    printf '%s\n' "$exact"
    return 0
  fi
  best=""
  if [[ -f "${dir}/${key}.body" ]]; then
    best="${dir}/${key}.body"
  fi
  for n in 2 3 4 5 6 7 8 9; do
    if [[ -f "${dir}/${key}.${n}.body" ]]; then
      best="${dir}/${key}.${n}.body"
    fi
  done
  if [[ -n "$best" ]]; then
    printf '%s\n' "$best"
    return 0
  fi
  if [[ "$key" == "instances" && -f "${dir}/instance.body" ]]; then
    printf '%s\n' "${dir}/instance.body"
    return 0
  fi
  return 1
}

dry_run_body() {
  local method="$1"
  local path="${2:-}"
  local key cursor file
  if [[ -z "$path" ]]; then
    path="$method"
    method="GET"
  fi
  key="$(fixture_key "$method" "$path")"
  if [[ "$key" == "force-stop" ]]; then
    return 0
  fi
  cursor="$(fixture_cursor_get "$key")"
  if ! file="$(fixture_file_for "$key" "$cursor")"; then
    return 0
  fi
  cat "$file"
}

stop_instance() {
  if [[ -n "${INSTANCE_HREF:-}" ]]; then
    teamcity_request POST "${INSTANCE_HREF}/actions/forceStop" || true
  fi
}

find_template_id() {
  local templates id
  templates="$(teamcity_request POST "${PROFILE_SETTINGS}?get=templates&prop:clouds.anka.url=${CONTROLLER_URL}")"
  json_has_name "$templates" "$TEMPLATE_NAME" || finish_fail "template name absent from list"
  id="$(template_id_for_name "$templates" "$TEMPLATE_NAME")" || finish_fail "template id missing"
  printf '%s\n' "$id"
}

confirm_template_tag() {
  local template_id="$1"
  local tags
  tags="$(teamcity_request POST "${PROFILE_SETTINGS}?get=tags&templateId=${template_id}&prop:clouds.anka.url=${CONTROLLER_URL}")"
  json_has_tag "$tags" "$TEMPLATE_TAG" || finish_fail "template tag absent from list"
}

save_cloud_profile() {
  local template_id="$1"
  local profile_json vm_name
  vm_name="${VM_NAME_TEMPLATE:-}"
  if [[ -z "$vm_name" ]]; then
    vm_name='tc-test-$ts'
  fi
  profile_json="$(build_profile_json \
    "$CONTROLLER_URL" "$TEAMCITY_URL" "$SSH_USER" "$SSH_PASSWORD" "$SSH_PORT" \
    "$AGENT_PATH" "$TEMPLATE_NAME" "$TEMPLATE_TAG" "$template_id" "$MAX_INSTANCES" \
    "$PROFILE_NAME" "$vm_name" \
    "${PROFILE_VCPU:-}" "${PROFILE_RAM:-}" "${PROFILE_GROUP_ID:-}" \
    "${PROFILE_AGENT_POOL_ID:-}" "${PROFILE_TERMINATE_AFTER_BUILD:-}" \
    "${PROFILE_TERMINATE_IDLE_TIME:-}")"
  if [[ "${TEAMCITY_DRY_RUN:-}" == "1" ]]; then
    CLOUD_PROFILE_ID="$PROFILE_NAME"
    teamcity_request POST "/app/rest/projects/id:_Root/projectFeatures" "$profile_json" >/dev/null
    return 0
  fi
  local response
  response="$(teamcity_request POST "/app/rest/projects/id:_Root/projectFeatures" "$profile_json")"
  CLOUD_PROFILE_ID="$(python3 -c 'import json,sys; print(json.loads(sys.argv[1])["id"])' "$response")"
}

# Drop the profile from the previous case.
# A second profile with the same image keeps the old profile id on the instance list.
delete_saved_cloud_profile() {
  if [[ -z "${CLOUD_PROFILE_ID:-}" ]]; then
    return 0
  fi
  teamcity_request DELETE "/app/rest/projects/id:_Root/projectFeatures/id:${CLOUD_PROFILE_ID}" >/dev/null
  CLOUD_PROFILE_ID=""
}

wait_for_saved_image() {
  local images start_epoch now
  local limit="${IMAGE_WAIT_SECONDS:-300}"
  start_epoch="$(date +%s)"
  while true; do
    images="$(teamcity_request GET "/app/rest/cloud/images")"
    if json_has_name "$images" "$TEMPLATE_NAME"; then
      return 0
    fi
    now="$(date +%s)"
    if (( now - start_epoch >= limit )); then
      finish_fail "template name absent from saved images"
    fi
    sleep 1
  done
}

# Return 0 after storing the one instance for this profile.
# Return 1 when the list has no matching instance.
# Exit when the list has more than one matching instance.
listed_profile_instance() {
  local list count
  list="$(teamcity_request GET "/app/rest/cloud/instances")" || return 1
  count="$(profile_instance_count "$list" "${CLOUD_PROFILE_ID:?}")" || return 1
  if [[ "$count" -gt 1 ]]; then
    finish_fail "instance count ${count}"
  fi
  if [[ "$count" -ne 1 ]]; then
    return 1
  fi
  INSTANCE_BODY="$(instance_for_profile "$list" "${CLOUD_PROFILE_ID}")" || return 1
  INSTANCE_HREF="$(json_href "$INSTANCE_BODY")"
  ANKA_VM_ID="$(anka_vm_id_from_body "$INSTANCE_BODY")"
}

start_error_is_instance_already_starting() {
  grep -E -q 'Waiting for [0-9]+ instance\(s\) to finish starting' "$1"
}

# Return 0 when one instance is listed.
# Return 1 when the list stays empty until the wait ends.
poll_for_listed_instance() {
  local start_epoch now
  local limit="${INSTANCE_LIST_WAIT_SECONDS:-60}"
  start_epoch="$(date +%s)"
  while true; do
    if listed_profile_instance; then
      return 0
    fi
    now="$(date +%s)"
    if (( now - start_epoch >= limit )); then
      return 1
    fi
    sleep 1
  done
}

wait_for_listed_instance() {
  poll_for_listed_instance || finish_fail "started instance absent from list"
}

# Return 0 when TeamCity accepts the start.
# Return 2 when an instance is already starting.
# Return 1 for any other start error. The error text is already printed.
send_start_request() {
  local template_id="$1"
  local start_json post_error
  start_json="$(build_start_instance_json "${CLOUD_PROFILE_ID:?}" "$template_id")"
  post_error="$(mktemp)"
  if teamcity_request POST "/app/rest/cloud/instances" "$start_json" >/dev/null 2>"$post_error"; then
    rm -f "$post_error"
    return 0
  fi
  cat "$post_error" >&2
  if start_error_is_instance_already_starting "$post_error"; then
    rm -f "$post_error"
    return 2
  fi
  rm -f "$post_error"
  return 1
}

# TeamCity starts one instance after the profile save to read the agent details.
# Wait for that instance. Start one only when the list stays empty.
start_one_vm() {
  local template_id="$1"
  local start_status
  if poll_for_listed_instance; then
    return 0
  fi
  if send_start_request "$template_id"; then
    start_status=0
  else
    start_status=$?
  fi
  if [[ "$start_status" -eq 2 ]]; then
    echo "The instance is already starting. This test uses that instance." >&2
  elif [[ "$start_status" -ne 0 ]]; then
    finish_fail "start instance failed"
  fi
  wait_for_listed_instance
}

# The instance list must be empty. TeamCity must accept this start.
# This does not use an instance that is already present or still starting.
start_new_vm() {
  local template_id="$1"
  local start_status
  if listed_profile_instance; then
    finish_fail "instance still present before start"
  fi
  if send_start_request "$template_id"; then
    start_status=0
  else
    start_status=$?
  fi
  if [[ "$start_status" -eq 2 ]]; then
    finish_fail "instance still starting"
  fi
  if [[ "$start_status" -ne 0 ]]; then
    finish_fail "start instance failed"
  fi
  if [[ "${TEAMCITY_DRY_RUN:-}" == "1" ]]; then
    advance_fixture instances
  fi
  wait_for_listed_instance
}

wait_until_agent_is_connected() {
  local start_epoch now state
  local limit="${AGENT_WAIT_SECONDS:-900}"
  # The default instance document has an agent link with id, name, and href.
  # connected and authorized are returned only when the request names them.
  local agent_fields="state,agent(id,href,connected,authorized)"
  start_epoch="$(date +%s)"
  while true; do
    INSTANCE_BODY="$(teamcity_request GET "${INSTANCE_HREF}?fields=${agent_fields}")"
    state="$(instance_state "$INSTANCE_BODY")"
    if [[ "$state" == "error" ]]; then
      finish_fail "instance state error"
    fi
    if agent_connected "$INSTANCE_BODY" && agent_authorized "$INSTANCE_BODY"; then
      return 0
    fi
    if agent_connected "$INSTANCE_BODY"; then
      authorize_agent
    fi
    now="$(date +%s)"
    if (( now - start_epoch >= limit )); then
      finish_fail "agent still disconnected after 15 minutes"
    fi
    sleep 1
  done
}

authorize_agent() {
  local agent_id
  agent_id="$(json_agent_id "$INSTANCE_BODY")"
  if [[ "${TEAMCITY_DRY_RUN:-}" == "1" ]]; then
    return 0
  fi
  curl -fsS -X PUT -H "Authorization: Bearer ${TEAMCITY_TOKEN}" \
    -H "Content-Type: text/plain" --data "true" \
    "${TEAMCITY_URL}/app/rest/agents/id:${agent_id}/authorized" >/dev/null
}

template_id_for_name() {
  python3 - "$1" "$2" <<'PY'
import json, sys
body, name = sys.argv[1], sys.argv[2]
start = body.find("[")
end = body.rfind("]")
data = json.loads(body[start:end + 1])
for item in data:
    if item.get("name") == name:
        print(item["id"])
        sys.exit(0)
sys.exit(1)
PY
}

json_href() {
  python3 - "$1" <<'PY'
import json, sys
print(json.loads(sys.argv[1])["href"])
PY
}

# Print a JSON array of instances for this profile.
# A bare object with href counts as one instance.
# Return 1 when the body is not an instance document.
profile_instance_matches() {
  python3 - "$1" "$2" <<'PY'
import json, sys
body, profile_id = sys.argv[1], sys.argv[2]
try:
    data = json.loads(body)
except json.JSONDecodeError:
    sys.exit(1)
if not isinstance(data, dict):
    sys.exit(1)
if "count" in data or "cloudInstance" in data:
    items = data.get("cloudInstance", [])
else:
    if data.get("href"):
        print(json.dumps([data]))
        sys.exit(0)
    sys.exit(1)
if isinstance(items, dict):
    items = [items]
elif not isinstance(items, list):
    sys.exit(1)
needle = f"profileId:{profile_id}"
matches = [item for item in items if needle in str(item.get("id") or "").split(",") and item.get("href")]
print(json.dumps(matches))
PY
}

profile_instance_count() {
  local matches
  matches="$(profile_instance_matches "$1" "$2")" || return 1
  python3 -c 'import json,sys; print(len(json.loads(sys.argv[1])))' "$matches"
}

instance_for_profile() {
  local matches
  matches="$(profile_instance_matches "$1" "$2")" || return 1
  python3 - "$matches" <<'PY'
import json, sys
items = json.loads(sys.argv[1])
if len(items) != 1:
    sys.exit(1)
print(json.dumps(items[0]))
PY
}

json_agent_id() {
  python3 - "$1" <<'PY'
import json, sys
print((json.loads(sys.argv[1]).get("agent") or {}).get("id", ""))
PY
}

json_status_ok() {
  python3 - "$1" <<'PY'
import json, sys
raw = sys.argv[1].strip()
if not raw:
    sys.exit(0)
try:
    data = json.loads(raw)
except json.JSONDecodeError:
    sys.exit(1)
if isinstance(data, dict) and "status" in data and data["status"] != "OK":
    sys.exit(1)
sys.exit(0)
PY
}

json_get() {
  python3 - "$1" "$2" <<'PY'
import json, sys
raw, path = sys.argv[1], sys.argv[2]
try:
    cur = json.loads(raw) if raw.strip() else {}
except json.JSONDecodeError:
    sys.exit(0)
for part in path.split("."):
    if isinstance(cur, dict):
        cur = cur.get(part, "")
    else:
        cur = ""
        break
if cur is None:
    print("")
elif isinstance(cur, bool):
    print("true" if cur else "false")
else:
    print(cur)
PY
}

json_property() {
  python3 - "$1" "$2" <<'PY'
import json, sys
raw, want = sys.argv[1], sys.argv[2]
try:
    data = json.loads(raw) if raw.strip() else {}
except json.JSONDecodeError:
    sys.exit(0)
props = data.get("properties") or {}
if isinstance(props, dict):
    props = props.get("property") or []
if isinstance(props, dict):
    props = [props]
if not isinstance(props, list):
    sys.exit(0)
for item in props:
    if isinstance(item, dict) and item.get("name") == want:
        print(item.get("value", ""))
        break
PY
}

anka_vm_id_from_body() {
  python3 - "$1" <<'PY'
import json, sys
try:
    data = json.loads(sys.argv[1])
except json.JSONDecodeError:
    sys.exit(0)
raw = str(data.get("id") or "")
for segment in raw.split(","):
    if segment.startswith("id:"):
        print(segment[3:])
        break
PY
}

teamcity_instance_count() {
  local list
  list="$(teamcity_request GET "/app/rest/cloud/instances")" || finish_fail "teamcity request failed"
  python3 - "$list" "${CLOUD_PROFILE_ID:?}" <<'PY'
import json, sys
body, profile_id = sys.argv[1], sys.argv[2]
try:
    data = json.loads(body)
except json.JSONDecodeError:
    print(0)
    sys.exit(0)
items = data.get("cloudInstance", []) if isinstance(data, dict) else []
if isinstance(items, dict):
    items = [items]
needle = f"profileId:{profile_id}"
count = 0
for item in items:
    if needle in str(item.get("id") or "").split(","):
        count += 1
print(count)
PY
}

controller_request() {
  local method="$1"
  local path="$2"
  local body="${3:-}"
  if [[ "${TEAMCITY_DRY_RUN:-}" == "1" ]]; then
    dry_run_body "$method" "$path"
    return 0
  fi
  local response_file http_code payload
  response_file="$(mktemp)"
  local args=(-sS -o "$response_file" -w '%{http_code}' -X "$method" -H "Accept: application/json")
  if [[ -n "$body" ]]; then
    args+=(-H "Content-Type: application/json" --data "$body")
  fi
  http_code="$(curl "${args[@]}" "${CONTROLLER_URL}${path}" || true)"
  payload="$(cat "$response_file")"
  rm -f "$response_file"
  if [[ ! "$http_code" =~ ^2 ]]; then
    printf 'HTTP_ERROR %s %s %s\n%s\n' "${http_code:-000}" "$method" "$path" "$payload" >&2
    return 22
  fi
  if ! json_status_ok "$payload"; then
    printf 'CONTROLLER_ERROR %s %s\n%s\n' "$method" "$path" "$payload" >&2
    return 22
  fi
  printf '%s' "$payload"
}

controller_vm_count() {
  local profile_id="$1"
  local list
  list="$(controller_request GET "/api/v1/vm")" || finish_fail "controller request failed"
  python3 - "$list" "$profile_id" <<'PY'
import json, sys
raw, profile_id = sys.argv[1], sys.argv[2]
try:
    data = json.loads(raw) if raw.strip() else {}
except json.JSONDecodeError:
    print(0)
    sys.exit(0)
items = data.get("body", []) if isinstance(data, dict) else []
if isinstance(items, dict):
    items = [items]
if not isinstance(items, list):
    print(0)
    sys.exit(0)
print(sum(1 for item in items if str(item.get("external_id") or "") == profile_id))
PY
}

controller_instance_state() {
  local vm_id="$1"
  local payload
  payload="$(controller_request GET "/api/v1/vm?id=${vm_id}")" || finish_fail "path vm state "
  json_get "$payload" "body.instance_state"
}

require_field() {
  local name="$1"
  local expected="$2"
  local actual="$3"
  if [[ "$actual" != "$expected" ]]; then
    finish_fail "field ${name} expected ${expected} got ${actual}"
  fi
}

require_field_prefix() {
  local name="$1"
  local prefix="$2"
  local actual="$3"
  if [[ "$actual" != "$prefix"* ]]; then
    finish_fail "field ${name} expected ${prefix} got ${actual}"
  fi
}

report_is_pass() {
  [[ -f "${REPORT_PATH:-}" ]] && grep -q '^PASS|' "$REPORT_PATH"
}

prepare_node_group() {
  local list existing response
  list="$(controller_request GET "/api/v1/group")" || finish_fail "group create failed"
  existing="$(python3 - "$list" <<'PY'
import json, sys
try:
    data = json.loads(sys.argv[1]) if sys.argv[1].strip() else {}
except json.JSONDecodeError:
    sys.exit(0)
items = data.get("body", []) if isinstance(data, dict) else []
if isinstance(items, dict):
    items = [items]
for item in items:
    if item.get("name") == "live-test":
        print(item.get("id", ""))
        break
PY
)"
  if [[ -n "$existing" ]]; then
    controller_request DELETE "/api/v1/group?id=${existing}" >/dev/null || finish_fail "group create failed"
  fi
  response="$(controller_request POST "/api/v1/group" '{"name":"live-test","description":"live test"}')" || finish_fail "group create failed"
  GROUP_ID="$(json_get "$response" "body.id")"
  [[ -n "$GROUP_ID" ]] || finish_fail "group create failed"
  list="$(controller_request GET "/api/v1/node")" || finish_fail "node count 0"
  local node_result
  node_result="$(python3 - "$list" <<'PY'
import json, sys
try:
    data = json.loads(sys.argv[1]) if sys.argv[1].strip() else {}
except json.JSONDecodeError:
    print("count:0")
    sys.exit(0)
items = data.get("body", []) if isinstance(data, dict) else []
if not isinstance(items, list) or len(items) != 1:
    count = len(items) if isinstance(items, list) else 0
    print(f"count:{count}")
    sys.exit(0)
print("id:" + str(items[0].get("node_id", "")))
PY
)"
  if [[ "$node_result" == count:* || -z "${node_result#id:}" ]]; then
    finish_fail "node count ${node_result#count:}"
  fi
  NODE_ID="${node_result#id:}"
  controller_request POST "/api/v1/node/group" "{\"group_ids\":[\"${GROUP_ID}\"],\"node_ids\":[\"${NODE_ID}\"]}" >/dev/null || finish_fail "group create failed"
}

prepare_agent_pool() {
  local list existing response
  list="$(teamcity_request GET "/app/rest/agentPools")" || finish_fail "agent pool create failed"
  existing="$(python3 - "$list" <<'PY'
import json, sys
try:
    data = json.loads(sys.argv[1]) if sys.argv[1].strip() else {}
except json.JSONDecodeError:
    sys.exit(0)
items = data.get("agentPool", []) if isinstance(data, dict) else []
if isinstance(items, dict):
    items = [items]
for item in items:
    if item.get("name") == "anka-live-pool":
        print(item.get("id", ""))
        break
PY
)"
  if [[ -n "$existing" ]]; then
    teamcity_request DELETE "/app/rest/agentPools/id:${existing}" >/dev/null || finish_fail "agent pool create failed"
  fi
  response="$(teamcity_request POST "/app/rest/agentPools" '{"name":"anka-live-pool"}')" || finish_fail "agent pool create failed"
  AGENT_POOL_ID="$(json_get "$response" "id")"
  [[ -n "$AGENT_POOL_ID" ]] || finish_fail "agent pool create failed"
}

cleanup_group_and_pool() {
  local failed=0
  if [[ -n "${GROUP_ID:-}" && -n "${NODE_ID:-}" ]]; then
    controller_request DELETE "/api/v1/node/group" "{\"group_ids\":[\"${GROUP_ID}\"],\"node_ids\":[\"${NODE_ID}\"]}" >/dev/null || failed=1
  fi
  if [[ -n "${GROUP_ID:-}" ]]; then
    controller_request DELETE "/api/v1/group?id=${GROUP_ID}" >/dev/null || failed=1
  fi
  if [[ -n "${AGENT_POOL_ID:-}" ]]; then
    teamcity_request DELETE "/app/rest/agentPools/id:${AGENT_POOL_ID}" >/dev/null || failed=1
  fi
  return "$failed"
}

# TeamCity 2026.2 rejects a build configuration in the Root project.
# The cloud profile stays on _Root. The child project inherits that profile.
link_build_project_to_pool() {
  if [[ -z "${AGENT_POOL_ID:-}" ]]; then
    return 0
  fi
  teamcity_request POST "/app/rest/agentPools/id:${AGENT_POOL_ID}/projects" "{\"id\":\"${BUILD_PROJECT_ID}\"}" >/dev/null || finish_fail "project create failed"
}

prepare_build_project() {
  if [[ -n "${BUILD_PROJECT_ID:-}" ]]; then
    return 0
  fi
  local payload response existing
  payload="$(python3 - <<'PY'
import json
print(json.dumps({
    "name": "anka-live",
    "id": "AnkaLive",
    "parentProject": {"locator": "id:_Root"},
}))
PY
)"
  if [[ "${TEAMCITY_DRY_RUN:-}" == "1" ]]; then
    BUILD_PROJECT_ID="AnkaLive"
    teamcity_request POST "/app/rest/projects" "$payload" >/dev/null
    link_build_project_to_pool
    return 0
  fi
  response="$(teamcity_request GET "/app/rest/projects?locator=id:AnkaLive")" || finish_fail "project create failed"
  existing="$(python3 - "$response" <<'PY'
import json, sys
try:
    data = json.loads(sys.argv[1]) if sys.argv[1].strip() else {}
except json.JSONDecodeError:
    sys.exit(0)
items = data.get("project", []) if isinstance(data, dict) else []
if isinstance(items, dict):
    items = [items]
for item in items:
    if item.get("id") == "AnkaLive":
        print("AnkaLive")
        break
PY
)"
  if [[ -n "$existing" ]]; then
    teamcity_request DELETE "/app/rest/projects/id:${existing}" >/dev/null || finish_fail "project create failed"
  fi
  response="$(teamcity_request POST "/app/rest/projects" "$payload")" || finish_fail "project create failed"
  BUILD_PROJECT_ID="$(json_get "$response" "id")"
  [[ -n "$BUILD_PROJECT_ID" ]] || finish_fail "project create failed"
  link_build_project_to_pool
}

create_build_type() {
  local name="$1"
  local script="$2"
  local payload response
  prepare_build_project
  payload="$(python3 - "$name" "$script" "$BUILD_PROJECT_ID" <<'PY'
import json, sys
name, script, project_id = sys.argv[1], sys.argv[2], sys.argv[3]
print(json.dumps({
    "name": name,
    "project": {"id": project_id},
    "steps": {"step": [{
        "type": "simpleRunner",
        "properties": {"property": [
            {"name": "script.content", "value": script},
            {"name": "teamcity.step.mode", "value": "default"},
            {"name": "use.custom.script", "value": "true"},
        ]},
    }]},
}))
PY
)"
  response="$(teamcity_request POST "/app/rest/buildTypes" "$payload")" || finish_fail "build type create failed"
  local build_type_id
  build_type_id="$(json_get "$response" "id")"
  [[ -n "$build_type_id" ]] || finish_fail "build type create failed"
  printf '%s\n' "$build_type_id"
}

queue_build() {
  local build_type_id="$1"
  local response
  response="$(teamcity_request POST "/app/rest/buildQueue" "{\"buildType\":{\"id\":\"${build_type_id}\"}}")" || finish_fail "build queue failed"
  local queued_build_id
  queued_build_id="$(json_get "$response" "id")"
  [[ -n "$queued_build_id" ]] || finish_fail "build queue failed"
  printf '%s\n' "$queued_build_id"
}

queue_has_build() {
  python3 - "$1" "$2" <<'PY'
import json, sys
raw, build_id = sys.argv[1], sys.argv[2]
try:
    data = json.loads(raw) if raw.strip() else {}
except json.JSONDecodeError:
    sys.exit(1)
if isinstance(data, dict) and data.get("count") == 0:
    sys.exit(1)
if str(data.get("id")) == build_id:
    sys.exit(0)
sys.exit(1)
PY
}

wait_for_one_vm() {
  local start_epoch now tc_count vm_count list
  local limit="${AGENT_WAIT_SECONDS:-900}"
  start_epoch="$(date +%s)"
  while true; do
    tc_count="$(teamcity_instance_count)"
    vm_count="$(controller_vm_count "$CLOUD_PROFILE_ID")"
    if [[ "$tc_count" == "1" && "$vm_count" == "1" ]]; then
      list="$(teamcity_request GET "/app/rest/cloud/instances")"
      INSTANCE_BODY="$(instance_for_profile "$list" "$CLOUD_PROFILE_ID")"
      INSTANCE_HREF="$(json_href "$INSTANCE_BODY")"
      ANKA_VM_ID="$(anka_vm_id_from_body "$INSTANCE_BODY")"
      return 0
    fi
    now="$(date +%s)"
    if (( now - start_epoch >= limit )); then
      finish_fail "started instance absent from list"
    fi
    sleep 1
  done
}

assert_vm_and_agent_fields() {
  local vm_payload agent_payload actual
  vm_payload="$(controller_request GET "/api/v1/vm?id=${ANKA_VM_ID}")" || finish_fail "field group_id expected ${GROUP_ID} got "
  actual="$(json_get "$vm_payload" "body.group_id")"
  require_field "group_id" "$GROUP_ID" "$actual"
  actual="$(json_get "$vm_payload" "body.vminfo.name")"
  require_field_prefix "vminfo.name" "mgmtManaged-tc-build-" "$actual"
  actual="$(json_get "$vm_payload" "body.vminfo.cpu_cores")"
  require_field "cpu_cores" "2" "$actual"
  actual="$(json_get "$vm_payload" "body.vminfo.ram")"
  require_field "ram" "4G" "$actual"
  agent_payload="$(teamcity_request GET "/app/rest/agents/id:$(json_agent_id "$INSTANCE_BODY")?fields=name,pool(id),properties(property(name,value))")" || finish_fail "field name expected 26.1-arm64_${ANKA_VM_ID} got "
  actual="$(json_get "$agent_payload" "name")"
  require_field "name" "26.1-arm64_${ANKA_VM_ID}" "$actual"
  actual="$(json_get "$agent_payload" "pool.id")"
  require_field "pool.id" "$AGENT_POOL_ID" "$actual"
  actual="$(json_property "$agent_payload" "env.ANKA_INSTANCE_ID")"
  require_field "env.ANKA_INSTANCE_ID" "$ANKA_VM_ID" "$actual"
  actual="$(json_property "$agent_payload" "env.ANKA_TEMPLATE_ID")"
  require_field "env.ANKA_TEMPLATE_ID" "$TEMPLATE_ID" "$actual"
  actual="$(json_property "$agent_payload" "env.ANKA_CLOUD")"
  require_field "env.ANKA_CLOUD" "ANKA_BUILD_CLOUD" "$actual"
}

wait_for_build_success() {
  local build_id="$1"
  local start_epoch now payload state status
  local limit="${BUILD_WAIT_SECONDS:-900}"
  start_epoch="$(date +%s)"
  while true; do
    payload="$(teamcity_request GET "/app/rest/builds/id:${build_id}?fields=state,status")" || finish_fail "build status  "
    state="$(json_get "$payload" "state")"
    status="$(json_get "$payload" "status")"
    if [[ "$state" == "finished" && "$status" == "SUCCESS" ]]; then
      return 0
    fi
    now="$(date +%s)"
    if (( now - start_epoch >= limit )); then
      finish_fail "build status ${state} ${status}"
    fi
    sleep 1
  done
}

wait_until_profile_vms_gone() {
  local start_epoch now tc_count vm_count
  local limit="${REMOVE_WAIT_SECONDS:-600}"
  start_epoch="$(date +%s)"
  while true; do
    tc_count="$(teamcity_instance_count)"
    vm_count="$(controller_vm_count "$CLOUD_PROFILE_ID")"
    if [[ "$tc_count" == "0" && "$vm_count" == "0" ]]; then
      return 0
    fi
    now="$(date +%s)"
    if (( now - start_epoch >= limit )); then
      finish_fail "vm still present after 10 minutes"
    fi
    sleep 1
  done
}

hold_one_vm() {
  local second_build_id="$1"
  local start_epoch now tc_count vm_count queue_body
  local limit="${HOLD_SECONDS:-180}"
  local pause="${POLL_SECONDS:-5}"
  start_epoch="$(date +%s)"
  while true; do
    tc_count="$(teamcity_instance_count)"
    vm_count="$(controller_vm_count "$CLOUD_PROFILE_ID")"
    if [[ "$tc_count" != "1" ]]; then
      finish_fail "instance count ${tc_count}"
    fi
    if [[ "$vm_count" != "1" ]]; then
      finish_fail "instance count ${vm_count}"
    fi
    queue_body="$(teamcity_request GET "/app/rest/buildQueue/id:${second_build_id}")" || finish_fail "second build left the queue"
    queue_has_build "$queue_body" "$second_build_id" || finish_fail "second build left the queue"
    now="$(date +%s)"
    if (( now - start_epoch >= limit )); then
      return 0
    fi
    sleep "$pause"
  done
}

wait_until_bad_ssh_vm_gone() {
  local profile_name="$1"
  local start_epoch now count
  local limit="${SSH_GONE_WAIT_SECONDS:-180}"
  local agent_fields="state,agent(id,href,connected,authorized)"
  start_epoch="$(date +%s)"
  while true; do
    INSTANCE_BODY="$(teamcity_request GET "${INSTANCE_HREF}?fields=${agent_fields}")" || true
    if agent_connected "$INSTANCE_BODY"; then
      finish_fail "agent connected for ${profile_name}"
    fi
    count="$(controller_vm_count "$CLOUD_PROFILE_ID")"
    if [[ "$count" == "0" ]]; then
      return 0
    fi
    now="$(date +%s)"
    if (( now - start_epoch >= limit )); then
      finish_fail "vm still present for ${profile_name}"
    fi
    sleep 1
  done
}

wait_until_controller_profile_gone() {
  local profile_name="$1"
  local start_epoch now count
  local limit="${SSH_GONE_WAIT_SECONDS:-180}"
  start_epoch="$(date +%s)"
  while true; do
    count="$(controller_vm_count "$CLOUD_PROFILE_ID")"
    if [[ "$count" == "0" ]]; then
      return 0
    fi
    now="$(date +%s)"
    if (( now - start_epoch >= limit )); then
      finish_fail "vm still present for ${profile_name}"
    fi
    sleep 1
  done
}

assert_path_vm_stays_up() {
  local state
  local agent_fields="state,agent(id,href,connected,authorized)"
  sleep "${PATH_WAIT_SECONDS:-90}"
  INSTANCE_BODY="$(teamcity_request GET "${INSTANCE_HREF}?fields=${agent_fields}")" || true
  if agent_connected "$INSTANCE_BODY"; then
    finish_fail "agent connected for anka-bad-ssh-path"
  fi
  state="$(controller_instance_state "$ANKA_VM_ID")"
  if [[ "$state" != "Started" ]]; then
    finish_fail "path vm state ${state}"
  fi
}