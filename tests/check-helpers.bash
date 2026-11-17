#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
source "${ROOT}/tests/helpers.bash"
fail() { echo "ERROR: $*" >&2; exit 1; }

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cat > "${tmp}/flat.yaml" <<'EOF'
port: 8111
profile_name: anka-agent-connect
EOF
[[ "$(yaml_value "${tmp}/flat.yaml" port)" == "8111" ]] || fail "yaml port"
[[ "$(yaml_value "${tmp}/flat.yaml" profile_name)" == "anka-agent-connect" ]] || fail "yaml name"

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
props = {item["name"]: item["value"] for item in data["parameters"]["property"]}
assert data["id"] == "anka-agent-connect"
assert data["cloudType"] == "anka"
assert props["clouds.anka.url"] == "http://10.0.0.2"
assert props["profileServerUrl"] == "http://10.0.0.3:8111"
assert props["ssh_password"] == "secret"
assert props["image_id"] == "d792c6f6-198c-470f-9526-9c998efe7ab4"
assert props["VM_NAME_TEMPLATE"] == "tc-test-$ts"
assert "auth.method" not in props
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
echo "helpers ok"