#!/usr/bin/env bash
set -euo pipefail
TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${TEST_DIR}/../helpers.bash"

TEMPLATE_NAME="$(yaml_value "${TEST_DIR}/node.yaml" template_name)"
TEMPLATE_TAG="$(yaml_value "${TEST_DIR}/node.yaml" template_tag)"
SSH_USER="$(yaml_value "${TEST_DIR}/node.yaml" ssh_user)"
SSH_PORT="$(yaml_value "${TEST_DIR}/node.yaml" ssh_port)"
AGENT_PATH="$(yaml_value "${TEST_DIR}/node.yaml" agent_path)"
PROFILE_NAME="$(yaml_value "${TEST_DIR}/teamcity.yaml" profile_name)"
MAX_INSTANCES="$(yaml_value "${TEST_DIR}/teamcity.yaml" max_instances)"
REPORT_PATH="${REPORT_PATH:-/tmp/test-reports/test-report-teamcity.txt}"
IMAGE_WAIT_SECONDS="${IMAGE_WAIT_SECONDS:-300}"
AGENT_WAIT_SECONDS="${AGENT_WAIT_SECONDS:-900}"
INSTANCE_HREF=""

mkdir -p "$(dirname "$REPORT_PATH")"

finish_fail() {
  local reason="$1"
  report_line FAIL "$reason" > "$REPORT_PATH"
  echo "FAIL: ${reason}" >&2
  exit 1
}

http_request() {
  local method="$1"
  local path="$2"
  local body="${3:-}"
  if [[ "${TEAMCITY_DRY_RUN:-}" == "1" ]]; then
    local fixture="instance.body"
    case "$path" in
      *get=templates*) fixture="templates.body" ;;
      *get=tags*) fixture="tags.body" ;;
      */app/rest/cloud/images*) fixture="images.body" ;;
      */actions/forceStop*) return 0 ;;
    esac
    cat "${FIXTURE_DIR}/${fixture}"
    return 0
  fi
  local args=(-fsS -X "$method" -H "Authorization: Bearer ${TEAMCITY_TOKEN}" -H "Accept: application/json")
  if [[ -n "$body" ]]; then
    args+=(-H "Content-Type: application/json" --data "$body")
  fi
  curl "${args[@]}" "${TEAMCITY_URL}${path}"
}

cleanup() {
  if [[ -n "$INSTANCE_HREF" ]]; then
    http_request POST "${INSTANCE_HREF}/actions/forceStop" || true
  fi
}
trap cleanup EXIT

: "${TEAMCITY_URL:?}"
: "${TEAMCITY_TOKEN:?}"
: "${CONTROLLER_URL:?}"
: "${ANKA_SSH_PASSWORD:?}"

templates="$(http_request POST "/plugins/anka-build-cloud-teamcity-plugin/profile-settings.html?get=templates&prop:clouds.anka.url=${CONTROLLER_URL}")"
json_has_name "$templates" "$TEMPLATE_NAME" || finish_fail "template name absent from list"
template_id="$(python3 - "$templates" "$TEMPLATE_NAME" <<'PY'
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
)" || finish_fail "template id missing"
tags="$(http_request POST "/plugins/anka-build-cloud-teamcity-plugin/profile-settings.html?get=tags&templateId=${template_id}")"
json_has_tag "$tags" "$TEMPLATE_TAG" || finish_fail "template tag absent from list"

profile_json="$(build_profile_json \
  "$CONTROLLER_URL" "$TEAMCITY_URL" "$SSH_USER" "$ANKA_SSH_PASSWORD" "$SSH_PORT" \
  "$AGENT_PATH" "$TEMPLATE_NAME" "$TEMPLATE_TAG" "$template_id" "$MAX_INSTANCES" \
  "$PROFILE_NAME" 'tc-test-$ts')"
http_request POST "/app/rest/cloud/profiles" "$profile_json" >/dev/null

images=""
start_epoch="$(date +%s)"
while true; do
  images="$(http_request GET "/app/rest/cloud/images")"
  if json_has_name "$images" "$TEMPLATE_NAME"; then
    break
  fi
  now="$(date +%s)"
  if (( now - start_epoch >= IMAGE_WAIT_SECONDS )); then
    finish_fail "template name absent from saved images"
  fi
  sleep 1
done

start_json="$(build_start_instance_json "$PROFILE_NAME" "$template_id")"
instance="$(http_request POST "/app/rest/cloud/instances" "$start_json")"
INSTANCE_HREF="$(python3 - "$instance" <<'PY'
import json, sys
print(json.loads(sys.argv[1])["href"])
PY
)"

start_epoch="$(date +%s)"
while true; do
  state="$(instance_state "$instance")"
  if [[ "$state" == "error" ]]; then
    finish_fail "instance state error"
  fi
  if agent_connected "$instance"; then
    if agent_authorized "$instance"; then
      break
    fi
    agent_id="$(python3 - "$instance" <<'PY'
import json, sys
print((json.loads(sys.argv[1]).get("agent") or {}).get("id", ""))
PY
)"
    if [[ "${TEAMCITY_DRY_RUN:-}" != "1" ]]; then
      curl -fsS -X PUT -H "Authorization: Bearer ${TEAMCITY_TOKEN}" \
        -H "Content-Type: text/plain" --data "true" \
        "${TEAMCITY_URL}/app/rest/agents/id:${agent_id}/authorized" >/dev/null
    fi
  fi
  now="$(date +%s)"
  if (( now - start_epoch >= AGENT_WAIT_SECONDS )); then
    finish_fail "agent still disconnected after 15 minutes"
  fi
  sleep 1
  instance="$(http_request GET "${INSTANCE_HREF}")"
done

report_line PASS "" > "$REPORT_PATH"
echo "PASS"