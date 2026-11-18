#!/usr/bin/env bash
# Install Azul Zulu JRE 21 into this VM. The TeamCity agent accepts Java 21 through 25.
set -eu
vm_dir="${JAVA_VM_DIR:-/Library/Java/JavaVirtualMachines}"

java_major() {
  "$1" -version 2>&1 | sed -n 's/.* version "\([0-9][0-9]*\).*/\1/p' | head -n 1
}

if [ -d "$vm_dir" ]; then
  current="$(find "$vm_dir" -type f -path '*/Contents/Home/bin/java' | head -n 1 || true)"
  if [ -n "$current" ] && [ -x "$current" ]; then
    major="$(java_major "$current" || true)"
    if [ -n "${major:-}" ] && [ "$major" -ge 21 ] && [ "$major" -lt 26 ]; then
      echo "Azul JRE already present: $current"
      exit 0
    fi
  fi
fi

case "$(uname -m)" in
  arm64|aarch64) azul_arch=arm ;;
  x86_64) azul_arch=x86 ;;
  *)
    echo "unsupported cpu $(uname -m)" >&2
    exit 1
    ;;
esac

api="https://api.azul.com/metadata/v1/zulu/packages/?java_version=21&os=macos&arch=${azul_arch}&hw_bitness=64&archive_type=tar.gz&java_package_type=jre&javafx_bundled=false&latest=true&release_status=ga&availability_types=CA"
meta="$(curl -fsSL "$api")"
url="$(printf '%s' "$meta" | sed -n 's/.*"download_url" *: *"\([^"]*\)".*/\1/p' | head -n 1)"
if [ -z "$url" ]; then
  echo "Azul metadata has no download_url" >&2
  exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
curl -fL --retry 3 --retry-delay 2 -sS -o "$work/jre.tar.gz" "$url"
tar -xzf "$work/jre.tar.gz" -C "$work"
java_file="$(find "$work" -type f -path '*/Contents/Home/bin/java' | head -n 1 || true)"
if [ -z "$java_file" ]; then
  echo "Azul archive has no Contents/Home/bin/java" >&2
  exit 1
fi
bundle="$(cd "$(dirname "$java_file")/../../.." && pwd)"
base="$(basename "$bundle")"
case "$base" in
  *.jdk) dest_name="$base" ;;
  *) dest_name="zulu-21.jdk" ;;
esac
mkdir -p "$vm_dir"
rm -rf "${vm_dir}/${dest_name}"
mv "$bundle" "${vm_dir}/${dest_name}"
chmod +x "${vm_dir}/${dest_name}/Contents/Home/bin/java"
major="$(java_major "${vm_dir}/${dest_name}/Contents/Home/bin/java" || true)"
if [ -z "${major:-}" ] || [ "$major" -lt 21 ] || [ "$major" -ge 26 ]; then
  echo "installed Azul JRE major is ${major:-missing}" >&2
  exit 1
fi
if [ "$vm_dir" = "/Library/Java/JavaVirtualMachines" ] && [ -x /usr/libexec/java_home ]; then
  home="$(/usr/libexec/java_home -v 21 || true)"
  if [ "$home" != "${vm_dir}/${dest_name}/Contents/Home" ]; then
    echo "java_home -v 21 returned ${home:-empty}" >&2
    exit 1
  fi
fi
echo "Installed Azul JRE ${major} at ${vm_dir}/${dest_name}"
