#!/usr/bin/env bash
# maxInstances is 1. Two builds are queued. Only one VM starts.
set -euo pipefail
TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${TEST_DIR}/../helpers.bash"

###############################################################################
# Read the manifest. Stop the VM on exit, including after a pass.
###############################################################################
TEST_ID="max-instances"
CHECK_NAME="one-vm"
begin_live_test "${TEST_DIR}/manifest.yaml"

on_exit_max_instances() {
  local code=$?
  trap - EXIT
  stop_instance || true
  exit "$code"
}
trap on_exit_max_instances EXIT

###############################################################################
# Save the profile with maxInstances 1.
# TeamCity may start that one VM to read the agent details.
# Queue a long build, then a second build.
###############################################################################
TEMPLATE_ID="$(find_template_id)"
confirm_template_tag "$TEMPLATE_ID"
save_cloud_profile "$TEMPLATE_ID"
wait_for_saved_image
hold_type_id="$(create_build_type "max-hold" "sleep 180")"
second_type_id="$(create_build_type "max-second" "echo second-build")"
queue_build "$hold_type_id" >/dev/null
second_build_id="$(queue_build "$second_type_id")"

###############################################################################
# While the first VM exists, the instance count stays 1.
# The second build stays in the queue.
###############################################################################
wait_for_one_vm
hold_one_vm "$second_build_id"
report_line PASS "" > "$REPORT_PATH"
echo "PASS"
