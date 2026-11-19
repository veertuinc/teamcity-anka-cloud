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
run_case "5-test-missing-template" "PASS|missing-template|absent|" "FAIL|missing-template|absent|missing template name was listed"
echo "test.bash ok"
