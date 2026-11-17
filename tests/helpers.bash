#!/usr/bin/env bash
yaml_value() {
  local file="$1"
  local key="$2"
  python3 - "$file" "$key" <<'PY'
import sys
path, key = sys.argv[1], sys.argv[2]
with open(path, encoding="utf-8") as handle:
    for raw in handle:
        line = raw.split("#", 1)[0].rstrip()
        if not line.strip() or line[:1] in (" ", "\t", "-"):
            continue
        name, sep, value = line.partition(":")
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
    "name": values["profile_name"],
    "cloudType": "anka",
    "enabled": True,
    "project": {"id": "_Root"},
    "parameters": {"property": [{"name": name, "value": value} for name, value in pairs]},
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
  printf '%s|agent-connects|connected|%s\n' "$status" "$reason"
}