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
if len(sys.argv) != 13:
    sys.exit("build_profile_json: expected 12 arguments")
values = dict(zip(names, sys.argv[1:]))
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
agent = json.loads(sys.argv[1]).get("agent") or {}
sys.exit(0 if agent.get("connected") is True else 1)
PY
}

agent_authorized() {
  python3 - "$1" <<'PY'
import json, sys
agent = json.loads(sys.argv[1]).get("agent") or {}
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

read_manifest() {
  TEMPLATE_NAME="$(yaml_host_value "$MANIFEST" node template_name)"
  TEMPLATE_TAG="$(yaml_host_value "$MANIFEST" node template_tag)"
  SSH_USER="$(yaml_host_value "$MANIFEST" node ssh_user)"
  SSH_PASSWORD="$(yaml_host_value "$MANIFEST" node ssh_password)"
  SSH_PORT="$(yaml_host_value "$MANIFEST" node ssh_port)"
  AGENT_PATH="$(yaml_host_value "$MANIFEST" node agent_path)"
  PROFILE_NAME="$(yaml_host_value "$MANIFEST" teamcity profile_name)"
  MAX_INSTANCES="$(yaml_host_value "$MANIFEST" teamcity max_instances)"
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
    dry_run_body "$path"
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

dry_run_body() {
  local path="$1"
  local fixture="instance.body"
  case "$path" in
    *get=templates*) fixture="templates.body" ;;
    *get=tags*) fixture="tags.body" ;;
    */app/rest/cloud/images*) fixture="images.body" ;;
    */actions/forceStop*) return 0 ;;
  esac
  cat "${FIXTURE_DIR}/${fixture}"
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
    "$PROFILE_NAME" "$vm_name")"
  if [[ "${TEAMCITY_DRY_RUN:-}" == "1" ]]; then
    CLOUD_PROFILE_ID="$PROFILE_NAME"
    teamcity_request POST "/app/rest/projects/id:_Root/projectFeatures" "$profile_json" >/dev/null
    return 0
  fi
  local response
  response="$(teamcity_request POST "/app/rest/projects/id:_Root/projectFeatures" "$profile_json")"
  CLOUD_PROFILE_ID="$(python3 -c 'import json,sys; print(json.loads(sys.argv[1])["id"])' "$response")"
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

start_one_vm() {
  local template_id="$1"
  local start_json list start_epoch now
  local limit="${INSTANCE_LIST_WAIT_SECONDS:-60}"
  start_json="$(build_start_instance_json "${CLOUD_PROFILE_ID:?}" "$template_id")"
  teamcity_request POST "/app/rest/cloud/instances" "$start_json" >/dev/null
  start_epoch="$(date +%s)"
  while true; do
    list="$(teamcity_request GET "/app/rest/cloud/instances")"
    if INSTANCE_BODY="$(instance_for_profile "$list" "$CLOUD_PROFILE_ID")"; then
      INSTANCE_HREF="$(json_href "$INSTANCE_BODY")"
      return 0
    fi
    now="$(date +%s)"
    if (( now - start_epoch >= limit )); then
      finish_fail "started instance absent from list"
    fi
    sleep 1
  done
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

instance_for_profile() {
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
        print(json.dumps(data))
        sys.exit(0)
    sys.exit(1)
if isinstance(items, dict):
    items = [items]
elif not isinstance(items, list):
    sys.exit(1)
needle = f"profileId:{profile_id}"
matches = [item for item in items if needle in str(item.get("id") or "").split(",") and item.get("href")]
if len(matches) != 1:
    sys.exit(1)
print(json.dumps(matches[0]))
PY
}

json_agent_id() {
  python3 - "$1" <<'PY'
import json, sys
print((json.loads(sys.argv[1]).get("agent") or {}).get("id", ""))
PY
}