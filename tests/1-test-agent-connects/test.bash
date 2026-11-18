#!/usr/bin/env bash
# The plugin must list one template, start one VM, and the agent must connect.
# This test does not queue a build.
set -euo pipefail
TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${TEST_DIR}/../helpers.bash"

###############################################################################
# Read the public manifest and require the runner environment.
# The trap stops the VM when a later step fails.
###############################################################################
TEST_ID="agent-connects"
CHECK_NAME="connected"
begin_live_test "${TEST_DIR}/manifest.yaml"
trap stop_instance EXIT

###############################################################################
# The open controller must list template 26.1-arm64 and its tag.
###############################################################################
template_id="$(find_template_id)"
confirm_template_tag "$template_id"

###############################################################################
# Save the cloud profile, then wait until TeamCity shows that image.
###############################################################################
save_cloud_profile "$template_id"
wait_for_saved_image

###############################################################################
# Start one VM. The start response has an empty body.
# Read the instance from the instance list.
# Wait until the agent is connected and authorized.
###############################################################################
start_one_vm "$template_id"
wait_until_agent_is_connected

###############################################################################
# Record the pass line: PASS|agent-connects|connected|
###############################################################################
report_line PASS "" > "$REPORT_PATH"
echo "PASS"
