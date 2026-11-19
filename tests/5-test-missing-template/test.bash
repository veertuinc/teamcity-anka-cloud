#!/usr/bin/env bash
# The controller list has the real template and the real tag.
# It does not have missing-template-name or missing-tag-name.
# This test does not start a VM.
set -euo pipefail
TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${TEST_DIR}/../helpers.bash"

###############################################################################
# Read the manifest. The real template name stays in the manifest for prepare.
###############################################################################
TEST_ID="missing-template"
CHECK_NAME="absent"
begin_live_test "${TEST_DIR}/manifest.yaml"

templates="$(teamcity_request POST "${PROFILE_SETTINGS}?get=templates&prop:clouds.anka.url=${CONTROLLER_URL}")"
json_has_name "$templates" "$TEMPLATE_NAME" || finish_fail "template name absent from list"
if json_has_name "$templates" "missing-template-name"; then
  finish_fail "missing template name was listed"
fi

template_id="$(template_id_for_name "$templates" "$TEMPLATE_NAME")" || finish_fail "template id missing"
tags="$(teamcity_request POST "${PROFILE_SETTINGS}?get=tags&templateId=${template_id}&prop:clouds.anka.url=${CONTROLLER_URL}")"
json_has_tag "$tags" "$TEMPLATE_TAG" || finish_fail "template tag absent from list"
if json_has_tag "$tags" "missing-tag-name"; then
  finish_fail "missing tag was listed"
fi

report_line PASS "" > "$REPORT_PATH"
echo "PASS"
