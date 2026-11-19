#!/usr/bin/env bash
# A bad password and a bad port make the plugin remove the VM.
# The path /System/missing-agent cannot be created. The VM stays up.
# This script then calls forceStop for that VM.
set -euo pipefail
TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${TEST_DIR}/../helpers.bash"

###############################################################################
# Read the manifest. Stop a leftover VM when the report is not PASS.
###############################################################################
TEST_ID="bad-ssh"
CHECK_NAME="terminated"
begin_live_test "${TEST_DIR}/manifest.yaml"

on_exit_bad_ssh() {
  local code=$?
  trap - EXIT
  if ! report_is_pass; then
    stop_instance || true
  fi
  exit "$code"
}
trap on_exit_bad_ssh EXIT

reset_ssh_from_manifest() {
  SSH_USER="$(yaml_host_value "$MANIFEST" node ssh_user)"
  SSH_PASSWORD="$(yaml_host_value "$MANIFEST" node ssh_password)"
  SSH_PORT="$(yaml_host_value "$MANIFEST" node ssh_port)"
  AGENT_PATH="$(yaml_host_value "$MANIFEST" node agent_path)"
}

run_bad_ssh_case() {
  local profile_name="$1"
  local mode="$2"
  reset_ssh_from_manifest
  PROFILE_NAME="$profile_name"
  case "$mode" in
    password) SSH_PASSWORD="wrong-password" ;;
    port) SSH_PORT="2222" ;;
    path) AGENT_PATH="/System/missing-agent" ;;
  esac
  save_cloud_profile "$TEMPLATE_ID"
  wait_for_saved_image
  start_one_vm "$TEMPLATE_ID"
  if [[ "$mode" == "path" ]]; then
    assert_path_vm_stays_up
    stop_instance
    advance_fixture vm-list
    wait_until_controller_profile_gone "$profile_name"
    return 0
  fi
  wait_until_bad_ssh_vm_gone "$profile_name"
}

###############################################################################
# Run the password case, then the port case, then the blocked path.
# Each TeamCity instance list is the next fixture cursor.
###############################################################################
TEMPLATE_ID="$(find_template_id)"
confirm_template_tag "$TEMPLATE_ID"
run_bad_ssh_case "anka-bad-ssh-password" password
advance_fixture instances
run_bad_ssh_case "anka-bad-ssh-port" port
advance_fixture instances
run_bad_ssh_case "anka-bad-ssh-path" path
report_line PASS "" > "$REPORT_PATH"
echo "PASS"
