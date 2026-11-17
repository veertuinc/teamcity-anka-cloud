#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEST_DIR="${ROOT}/tests/1-test-agent-connects"
export TEAMCITY_DRY_RUN=1
export TEAMCITY_URL="http://10.0.0.3:8111"
export TEAMCITY_TOKEN="token"
export CONTROLLER_URL="http://10.0.0.2"
export IMAGE_WAIT_SECONDS=1
export AGENT_WAIT_SECONDS=1
export REPORT_PATH="${ROOT}/tests/.report-pass.txt"
export FIXTURE_DIR="${TEST_DIR}/fixtures/pass"
"$TEST_DIR/test.bash"
grep -q '^PASS|agent-connects|connected|$' "$REPORT_PATH"
export REPORT_PATH="${ROOT}/tests/.report-fail.txt"
export FIXTURE_DIR="${TEST_DIR}/fixtures/error"
set +e
"$TEST_DIR/test.bash"
code=$?
set -e
[[ "$code" -ne 0 ]]
grep -q '^FAIL|agent-connects|connected|instance state error$' "$REPORT_PATH"
rm -f "${ROOT}/tests/.report-pass.txt" "${ROOT}/tests/.report-fail.txt"
echo "test.bash ok"