#!/usr/bin/env bash
# One build runs. The VM and the agent match the profile.
# TeamCity then removes the VM. This script does not call forceStop on success.
set -euo pipefail
TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${TEST_DIR}/../helpers.bash"

###############################################################################
# Read the manifest. On failure, stop the VM and remove the group and pool.
# On success, leave the VM stop to TeamCity and still remove the group and pool.
###############################################################################
TEST_ID="build-stops-vm"
CHECK_NAME="removed"
begin_live_test "${TEST_DIR}/manifest.yaml"

on_exit_build_stops() {
  local code=$?
  trap - EXIT
  if ! report_is_pass; then
    stop_instance || true
  fi
  if ! cleanup_group_and_pool; then
    echo "cleanup failed" >&2
    exit 1
  fi
  exit "$code"
}
trap on_exit_build_stops EXIT

###############################################################################
# Create the node group and the agent pool, then save the cloud profile.
###############################################################################
prepare_node_group
prepare_agent_pool
PROFILE_GROUP_ID="$GROUP_ID"
PROFILE_AGENT_POOL_ID="$AGENT_POOL_ID"
TEMPLATE_ID="$(find_template_id)"
confirm_template_tag "$TEMPLATE_ID"
save_cloud_profile "$TEMPLATE_ID"
wait_for_saved_image

###############################################################################
# Queue one build. TeamCity starts the VM. Check the fields while it runs.
###############################################################################
build_type_id="$(create_build_type "live-build" "sleep 20; echo live-build-ok")"
build_id="$(queue_build "$build_type_id")"
wait_for_one_vm
wait_until_agent_is_connected
assert_vm_and_agent_fields
wait_for_build_success "$build_id"

###############################################################################
# The next fixture cursor is the empty VM list.
# Wait until TeamCity and the controller both have no VM for this profile.
###############################################################################
advance_fixture instances
advance_fixture vm-list
wait_until_profile_vms_gone
report_line PASS "" > "$REPORT_PATH"
echo "PASS"
