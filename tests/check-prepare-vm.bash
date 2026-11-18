#!/usr/bin/env bash
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
fail() { echo "ERROR: $*" >&2; exit 1; }
guest="${here}/prepare-vm.bash"
[[ -f "$guest" ]] || fail "missing prepare-vm.bash"

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cat > "${tmp}/curl" <<'EOF'
#!/bin/sh
out=""
url=""
while [ $# -gt 0 ]; do
  case "$1" in
    -o) out="$2"; shift 2 ;;
    -*) shift ;;
    *) url="$1"; shift ;;
  esac
done
printf '%s\n' "$url" >> "$(dirname "$0")/curl-urls"
if [ -n "$out" ]; then
  stage="$(mktemp -d)"
  mkdir -p "$stage/zulu21.54-ca-jre21-macosx_aarch64/Contents/Home/bin"
  cat > "$stage/zulu21.54-ca-jre21-macosx_aarch64/Contents/Home/bin/java" <<'JAVA'
#!/bin/sh
echo 'openjdk version "21.0.13" 2025-10-21' >&2
JAVA
  chmod +x "$stage/zulu21.54-ca-jre21-macosx_aarch64/Contents/Home/bin/java"
  tar -czf "$out" -C "$stage" zulu21.54-ca-jre21-macosx_aarch64
  rm -rf "$stage"
else
  printf '%s\n' '{"download_url":"https://cdn.azul.com/zulu/bin/fake-jre.tar.gz"}'
fi
EOF
chmod +x "${tmp}/curl"

run_prep() {
  PATH="${tmp}:/usr/bin:/bin:/usr/sbin:/sbin" JAVA_VM_DIR="${tmp}/jvms" bash "$guest"
}
run_prep
[[ -x "${tmp}/jvms/zulu-21.jdk/Contents/Home/bin/java" ]] || fail "jre missing"
[[ ! -d "${tmp}/jvms/zulu21.54-ca-jre21-macosx_aarch64" ]] || fail "archive name left in place"
grep -q 'java_package_type=jre' "${tmp}/curl-urls" || fail "package type"
grep -q 'os=macos' "${tmp}/curl-urls" || fail "os"
cpu="$(uname -m)"
if [[ "$cpu" == "arm64" || "$cpu" == "aarch64" ]]; then
  grep -q 'arch=arm' "${tmp}/curl-urls" || fail "arm arch"
elif [[ "$cpu" == "x86_64" ]]; then
  grep -q 'arch=x86' "${tmp}/curl-urls" || fail "x86 arch"
fi
downloads="$(wc -l < "${tmp}/curl-urls" | tr -d ' ')"
[[ "$downloads" == "2" ]] || fail "download count ${downloads}"
run_prep
downloads="$(wc -l < "${tmp}/curl-urls" | tr -d ' ')"
[[ "$downloads" == "2" ]] || fail "second run downloaded again"
echo "prepare-vm ok"
