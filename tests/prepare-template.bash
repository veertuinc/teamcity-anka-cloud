#!/usr/bin/env bash
# Start the template, run guest scripts, and save the tag.
# With no extra arguments, the guest script is prepare-vm.bash beside this file.
set -euo pipefail
template_id="${1:?}"
template_tag="${2:?}"
shift 2
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ $# -eq 0 ]]; then
  set -- "${here}/prepare-vm.bash"
fi
anka="${ANKA_BIN:-/usr/local/bin/anka}"
stopped=0
cleanup() {
  if [[ "$stopped" -eq 0 ]]; then
    "$anka" stop "$template_id" || true
  fi
}
trap cleanup EXIT
if ! "$anka" show "$template_id" | grep -q '| running'; then
  "$anka" start "$template_id"
fi
for guest_script in "$@"; do
  [[ -f "$guest_script" ]] || { echo "missing guest script ${guest_script}" >&2; exit 1; }
  guest_name="$(basename "$guest_script")"
  "$anka" cp "$guest_script" "${template_id}:/tmp/${guest_name}"
  if [[ -n "${GUEST_SUDO_PASSWORD:-}" ]]; then
    "$anka" run -e "GUEST_SUDO_PASSWORD=${GUEST_SUDO_PASSWORD}" "$template_id" \
      /bin/bash -lc "printf '%s\n' \"\$GUEST_SUDO_PASSWORD\" | /usr/bin/sudo -S -p '' /bin/bash /tmp/${guest_name}"
  else
    "$anka" run "$template_id" /usr/bin/sudo -n /bin/bash "/tmp/${guest_name}"
  fi
done
"$anka" stop "$template_id"
stopped=1
"$anka" delete --yes -t "$template_tag" "$template_id" || true
"$anka" push --local -t "$template_tag" "$template_id"
