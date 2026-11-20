#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export TEAMCITY_DRY_RUN=1
export TEAMCITY_URL="http://10.0.0.3:8111"
export TEAMCITY_TOKEN="token"
export CONTROLLER_URL="http://10.0.0.2"
export IMAGE_WAIT_SECONDS=1
export AGENT_WAIT_SECONDS=1
export BUILD_WAIT_SECONDS=1
export REMOVE_WAIT_SECONDS=1
export SSH_GONE_WAIT_SECONDS=1
export PATH_WAIT_SECONDS=1
export HOLD_SECONDS=1
export POLL_SECONDS=1
export INSTANCE_LIST_WAIT_SECONDS=1

run_case() {
  local dir="$1"
  local pass_line="$2"
  local fail_line="$3"
  local test_dir="${ROOT}/tests/${dir}"
  export REPORT_PATH="${ROOT}/tests/.report-pass.txt"
  export FIXTURE_DIR="${test_dir}/fixtures/pass"
  "$test_dir/test.bash"
  grep -q "^${pass_line}$" "$REPORT_PATH"
  export REPORT_PATH="${ROOT}/tests/.report-fail.txt"
  export FIXTURE_DIR="${test_dir}/fixtures/error"
  set +e
  "$test_dir/test.bash"
  local code=$?
  set -e
  [[ "$code" -ne 0 ]]
  grep -q "^${fail_line}$" "$REPORT_PATH"
  rm -f "${ROOT}/tests/.report-pass.txt" "${ROOT}/tests/.report-fail.txt"
}

run_case "1-test-agent-connects" "PASS|agent-connects|connected|" "FAIL|agent-connects|connected|instance state error"
run_case "2-test-build-stops-vm" "PASS|build-stops-vm|removed|" "FAIL|build-stops-vm|removed|vm still present after 10 minutes"
run_case "3-test-max-instances" "PASS|max-instances|one-vm|" "FAIL|max-instances|one-vm|second build left the queue"
run_case "4-test-bad-ssh" "PASS|bad-ssh|terminated|" "FAIL|bad-ssh|terminated|agent connected for anka-bad-ssh-password"

# The next profile is saved only after the previous TeamCity profile is deleted.
# Two profiles with the same image make the instance list keep the old profile id.
assert_bad_ssh_drops_previous_profile() {
  local test_dir="${ROOT}/tests/4-test-bad-ssh"
  local trace_file
  trace_file="$(mktemp)"
  export REPORT_PATH="${ROOT}/tests/.report-pass.txt"
  export FIXTURE_DIR="${test_dir}/fixtures/pass"
  bash -x "${test_dir}/test.bash" >"${ROOT}/tests/.report-pass.txt" 2>"$trace_file"
  python3 - "$trace_file" <<'PY'
import sys
text = open(sys.argv[1], encoding="utf-8").read()

def require_before(later, earlier):
    later_at = text.find(later)
    earlier_at = text.find(earlier)
    if later_at < 0:
        raise SystemExit("missing " + later)
    if earlier_at < 0 or earlier_at > later_at:
        raise SystemExit("missing " + earlier + " before " + later)

require_before(
    "PROFILE_NAME=anka-bad-ssh-port",
    "projectFeatures/id:anka-bad-ssh-password",
)
require_before(
    "PROFILE_NAME=anka-bad-ssh-path",
    "projectFeatures/id:anka-bad-ssh-port",
)
import re
port_at = text.find("PROFILE_NAME=anka-bad-ssh-port")
hrefs = list(re.finditer(r"INSTANCE_HREF=(\S*)", text[:port_at]))
if not hrefs or hrefs[-1].group(1) != "":
    raise SystemExit("INSTANCE_HREF still set when the port profile is saved")
PY
  rm -f "$trace_file" "${ROOT}/tests/.report-pass.txt"
}

assert_bad_ssh_drops_previous_profile
run_case "5-test-missing-template" "PASS|missing-template|absent|" "FAIL|missing-template|absent|missing template name was listed"
echo "test.bash ok"
