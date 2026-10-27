#!/usr/bin/env bash
# Installs, on a developer machine (macOS or Linux, no sudo), the JDK and the Android SDK packages that
# scripts/check.sh needs, so a plan trial runs the same command as the session and CI (L-075, docs/CLAUDE.md).
# Developer machines only: a cloud environment is built by scripts/cloud-setup.sh, and this script refuses to run
# there (it finds ~/.cloud-setup-version).
# Versions have one source each:
#   - Android SDK: the command-line tools build and the package list are read from scripts/cloud-setup.sh
#     (that file is pasted into the cloud environment as is, so it cannot read another file);
#   - JDK: JDK_RELEASE below (the cloud environment ships its own JDK; scripts/java-check.sh checks the range).
# Checksums: the JDK archive (GitHub) is checked against the SHA-256 that the Adoptium API (another host) gives;
# the command-line tools zip against the SHA-1 in Google's repository2-3.xml (same host: catches a broken
# download, not a tampered host); the SDK packages are checked by sdkmanager itself.
# Safe to run any number of times: a step that is already done is skipped. Downloads unpack in a temp dir inside
# the target directory and move into place in one step, so an interrupted run never leaves a half-installed JDK.
#
# Usage:   scripts/local-setup.sh [--prefix <dir>] [--sdk <dir>] [--check] [--print-versions]
#            --prefix          absolute dir for the JDK and env.sh (default ${XDG_DATA_HOME:-~/.local/share}/camera-pose)
#            --sdk             absolute Android SDK dir (default ~/android-sdk, like scripts/cloud-setup.sh)
#            --check           only report what is installed, no network; exit 1 when something is missing
#            --print-versions  print the versions this script installs and exit
# Then:    source <prefix>/env.sh && scripts/check.sh
# Exit:    0 ready, 1 failed or (with --check) something missing, 2 wrong arguments.
# Test seams (scripts/tests/run.sh): CURL, LOCAL_SETUP_CLOUD_SETUP, LOCAL_SETUP_OS, LOCAL_SETUP_ARCH,
#          LOCAL_SETUP_ADOPTIUM_API, LOCAL_SETUP_GOOGLE_REPO, LOCAL_SETUP_TOOLS (tools that must be on PATH).
# Messages are in Vietnamese for the team; comments are in English.
set -euo pipefail

JDK_RELEASE="jdk-21.0.13+9"
JDK_LINK_PREFIX="https://github.com/adoptium/"
PACKAGE_PATTERN='^[a-z-]+(;[A-Za-z0-9._-]+)*$'

prefix="${XDG_DATA_HOME:-${HOME}/.local/share}/camera-pose"
sdk_root="${HOME}/android-sdk"
mode="install"
usage="Cách dùng: scripts/local-setup.sh [--prefix <thư mục tuyệt đối>] [--sdk <thư mục tuyệt đối>] [--check] [--print-versions]"
while [ "$#" -gt 0 ]; do
  case "$1" in
    --prefix | --sdk)
      [ "$#" -ge 2 ] || { echo "LỖI: $1 cần một thư mục. ${usage}" >&2; exit 2; }
      case "$2" in /*) ;; *) echo "LỖI: $1 cần đường dẫn tuyệt đối (được '$2'). ${usage}" >&2; exit 2 ;; esac
      if [ "$1" = --prefix ]; then prefix="$2"; else sdk_root="$2"; fi
      shift 2 ;;
    --check) mode="check"; shift ;;
    --print-versions) mode="versions"; shift ;;
    *) echo "LỖI: không hiểu tham số '$1'. ${usage}" >&2; exit 2 ;;
  esac
done
cd "$(dirname "$0")/.."

curl_cmd="${CURL:-curl}"
curl_opts=(-fsSL --proto '=https' --connect-timeout 20 --retry 2)
cloud_setup="${LOCAL_SETUP_CLOUD_SETUP:-scripts/cloud-setup.sh}"
adoptium_api="${LOCAL_SETUP_ADOPTIUM_API:-https://api.adoptium.net/v3}"
google_repo="${LOCAL_SETUP_GOOGLE_REPO:-https://dl.google.com/android/repository}"

# The two values that cloud-setup.sh owns. Every package name must look like an sdkmanager package, so a change
# in the shape of cloud-setup.sh stops here instead of passing junk to sdkmanager.
tools_build="$(sed -nE 's/^CMDLINE_TOOLS_ZIP="commandlinetools-linux-([0-9]+)_latest\.zip"$/\1/p' "$cloud_setup" 2> /dev/null || true)"
packages="$(awk '/--install \\$/ { on = 1; next } on { print } on && /> \/dev\/null/ { on = 0 }' "$cloud_setup" 2> /dev/null \
  | sed -nE 's/^[[:space:]]*"([^"]+)".*$/\1/p' | tr '\n' ' ' | sed 's/ $//')"
bad_package=""
for package in $packages; do
  [[ "$package" =~ $PACKAGE_PATTERN ]] || bad_package="$package"
done
if [ -z "$tools_build" ] || [ -z "$packages" ] || [ -n "$bad_package" ]; then
  echo "LỖI: không đọc được phiên bản command-line tools hay danh sách gói SDK trong ${cloud_setup}" \
    "(dòng CMDLINE_TOOLS_ZIP=\"commandlinetools-linux-<số>_latest.zip\" và các dòng \"<gói>\" sau --install)${bad_package:+; gói không hợp lệ: ${bad_package}}." >&2
  exit 1
fi

if [ "$mode" = "versions" ]; then
  echo "JDK: ${JDK_RELEASE} (Temurin)"
  echo "command-line tools: ${tools_build}"
  echo "gói SDK: ${packages}"
  exit 0
fi

if [ -f "${HOME}/.cloud-setup-version" ]; then
  echo "LỖI: máy này là môi trường cloud (có ~/.cloud-setup-version). scripts/local-setup.sh chỉ dành cho máy dev;" \
    "môi trường cloud dựng bằng scripts/cloud-setup.sh, thiếu ANDROID_HOME thì làm theo dòng export trong CLAUDE.md." >&2
  exit 1
fi

missing_tools=""
for tool in ${LOCAL_SETUP_TOOLS:-curl jq unzip tar python3}; do command -v "$tool" > /dev/null || missing_tools="${missing_tools} ${tool}"; done
command -v shasum > /dev/null || command -v sha256sum > /dev/null || missing_tools="${missing_tools} shasum"
if [ -n "$missing_tools" ]; then
  echo "LỖI: máy thiếu công cụ:${missing_tools}. Cài các công cụ này rồi chạy lại." >&2
  exit 1
fi

case "${LOCAL_SETUP_OS:-$(uname -s)}" in
  Darwin) os="mac" ;;
  Linux) os="linux" ;;
  *) echo "LỖI: chỉ hỗ trợ macOS và Linux (máy này: ${LOCAL_SETUP_OS:-$(uname -s)})." >&2; exit 1 ;;
esac
case "${LOCAL_SETUP_ARCH:-$(uname -m)}" in
  arm64 | aarch64) arch="aarch64" ;;
  x86_64 | amd64) arch="x64" ;;
  *) echo "LỖI: chưa hỗ trợ kiến trúc ${LOCAL_SETUP_ARCH:-$(uname -m)}." >&2; exit 1 ;;
esac

jdk_dir="${prefix}/jdk/${JDK_RELEASE}"
if [ "$os" = "mac" ]; then java_home="${jdk_dir}/Contents/Home"; else java_home="${jdk_dir}"; fi
sdkmanager="${sdk_root}/cmdline-tools/latest/bin/sdkmanager"
env_file="${prefix}/env.sh"

sha_of() { # <algorithm 1|256> <file>
  if command -v shasum > /dev/null; then shasum -a "$1" "$2" | cut -d' ' -f1; else "sha${1}sum" "$2" | cut -d' ' -f1; fi
}
jdk_runs() { [ -x "${java_home}/bin/java" ] && "${java_home}/bin/java" -version > /dev/null 2>&1; }

if [ "$mode" = "check" ]; then
  missing=0
  if jdk_runs; then echo "QUA: JDK ${JDK_RELEASE} ở ${java_home}"; else echo "THIẾU: JDK ${JDK_RELEASE} (${java_home})"; missing=1; fi
  if [ -x "$sdkmanager" ]; then echo "QUA: command-line tools ở ${sdk_root}"; else echo "THIẾU: command-line tools (${sdkmanager})"; missing=1; fi
  for package in $packages; do
    if [ -d "${sdk_root}/${package//;//}" ]; then echo "QUA: ${package}"; else echo "THIẾU: ${package}"; missing=1; fi
  done
  if [ -f "$env_file" ]; then echo "QUA: ${env_file}"; else echo "THIẾU: ${env_file}"; missing=1; fi
  [ "$missing" -eq 0 ] || echo "Chạy scripts/local-setup.sh (không có --check) để cài phần còn thiếu."
  exit "$missing"
fi

# Temp dirs live inside the target directories, so the final mv is a rename on the same file system.
mkdir -p "${prefix}/jdk" "${sdk_root}/cmdline-tools"
jdk_tmp="$(mktemp -d "${prefix}/jdk/.tmp.XXXXXX")"
sdk_tmp="$(mktemp -d "${sdk_root}/cmdline-tools/.tmp.XXXXXX")"
trap 'rm -rf "${jdk_tmp}" "${sdk_tmp}"' EXIT

# 1. JDK.
if jdk_runs; then
  echo "Bỏ qua: JDK ${JDK_RELEASE} đã có ở ${java_home}."
else
  [ -e "$jdk_dir" ] && echo "JDK ở ${jdk_dir} không chạy được (cài dở?): cài lại."
  release_url="${adoptium_api}/assets/release_name/eclipse/${JDK_RELEASE//+/%2B}?architecture=${arch}&image_type=jdk&os=${os}&project=jdk"
  "$curl_cmd" "${curl_opts[@]}" "$release_url" -o "${jdk_tmp}/jdk.json" \
    || { echo "LỖI: không đọc được thông tin ${JDK_RELEASE} cho ${os}/${arch} từ Adoptium (${release_url})." >&2; exit 1; }
  jdk_link="$(jq -r '.binaries[0].package.link // empty' "${jdk_tmp}/jdk.json")"
  jdk_sum="$(jq -r '.binaries[0].package.checksum // empty' "${jdk_tmp}/jdk.json")"
  [ -n "$jdk_link" ] && [ -n "$jdk_sum" ] \
    || { echo "LỖI: Adoptium không trả đường tải hay mã SHA-256 cho ${JDK_RELEASE} (${os}/${arch})." >&2; exit 1; }
  case "$jdk_link" in
    "${JDK_LINK_PREFIX}"*) ;;
    *) echo "LỖI: đường tải JDK không nằm ở ${JDK_LINK_PREFIX} (được ${jdk_link}). Không tải." >&2; exit 1 ;;
  esac
  echo "Tải JDK ${JDK_RELEASE} (${os}/${arch})..."
  "$curl_cmd" "${curl_opts[@]}" "$jdk_link" -o "${jdk_tmp}/jdk.tar.gz" || { echo "LỖI: tải JDK thất bại (${jdk_link})." >&2; exit 1; }
  got="$(sha_of 256 "${jdk_tmp}/jdk.tar.gz")"
  [ "$got" = "$jdk_sum" ] || { echo "LỖI: mã SHA-256 của JDK không khớp (cần ${jdk_sum}, được ${got}). Không cài." >&2; exit 1; }
  mkdir -p "${jdk_tmp}/unpacked"
  tar -xzf "${jdk_tmp}/jdk.tar.gz" -C "${jdk_tmp}/unpacked"
  [ -d "${jdk_tmp}/unpacked/${JDK_RELEASE}" ] || { echo "LỖI: gói JDK không có thư mục ${JDK_RELEASE}." >&2; exit 1; }
  rm -rf "$jdk_dir"
  mv "${jdk_tmp}/unpacked/${JDK_RELEASE}" "$jdk_dir"
  echo "Đã cài JDK ở ${java_home}."
fi
export JAVA_HOME="$java_home"
export PATH="${JAVA_HOME}/bin:${PATH}"

# 2. Android command-line tools, then the SDK packages of cloud-setup.sh.
tools_zip="commandlinetools-${os}-${tools_build}_latest.zip"
if [ -x "$sdkmanager" ]; then
  echo "Bỏ qua: command-line tools đã có ở ${sdk_root}."
else
  "$curl_cmd" "${curl_opts[@]}" "${google_repo}/repository2-3.xml" -o "${sdk_tmp}/repository.xml" \
    || { echo "LỖI: không đọc được danh sách gói của Google (${google_repo}/repository2-3.xml)." >&2; exit 1; }
  tools_sum="$(python3 -I - "${sdk_tmp}/repository.xml" "$tools_zip" << 'PY' 2> /dev/null || true
import re, sys
import xml.etree.ElementTree as ET
# The checksum of the <complete> archive whose <url> is the zip; tags are matched without their namespace.
for complete in ET.parse(sys.argv[1]).iter():
    if not complete.tag.endswith("complete"):
        continue
    url = next((e.text for e in complete if e.tag.endswith("url")), None)
    checksum = next((e for e in complete if e.tag.endswith("checksum") and e.get("type") == "sha1"), None)
    if url == sys.argv[2] and checksum is not None and re.fullmatch(r"[0-9a-f]{40}", checksum.text or ""):
        print(checksum.text)
        break
PY
)"
  [ -n "$tools_sum" ] || { echo "LỖI: danh sách gói của Google không có mã SHA-1 cho ${tools_zip}." >&2; exit 1; }
  echo "Tải ${tools_zip}..."
  "$curl_cmd" "${curl_opts[@]}" "${google_repo}/${tools_zip}" -o "${sdk_tmp}/tools.zip" \
    || { echo "LỖI: tải command-line tools thất bại (${google_repo}/${tools_zip})." >&2; exit 1; }
  got="$(sha_of 1 "${sdk_tmp}/tools.zip")"
  [ "$got" = "$tools_sum" ] || { echo "LỖI: mã SHA-1 của ${tools_zip} không khớp (cần ${tools_sum}, được ${got}). Không cài." >&2; exit 1; }
  unzip -q -o "${sdk_tmp}/tools.zip" -d "${sdk_tmp}/unpacked"
  rm -rf "${sdk_root}/cmdline-tools/latest"
  mv "${sdk_tmp}/unpacked/cmdline-tools" "${sdk_root}/cmdline-tools/latest"
fi
export ANDROID_HOME="$sdk_root"
yes | "$sdkmanager" --licenses > /dev/null 2>&1 || true
echo "Đã chấp nhận giấy phép Android SDK (sdkmanager --licenses), như scripts/cloud-setup.sh."
# sdkmanager skips packages that are already installed.
# shellcheck disable=SC2086 # the package list is split on purpose
"$sdkmanager" --install $packages > "${sdk_tmp}/sdkmanager.log" 2>&1 \
  || { echo "LỖI: sdkmanager không cài được ${packages}. 20 dòng cuối:" >&2; tail -n 20 "${sdk_tmp}/sdkmanager.log" >&2; exit 1; }

# 3. env.sh, rewritten only when its content changes.
cat > "${jdk_tmp}/env.sh" << ENV
# Written by scripts/local-setup.sh. Use: source ${env_file}
export JAVA_HOME="${java_home}"
export ANDROID_HOME="${sdk_root}"
export ANDROID_SDK_ROOT="${sdk_root}"
export PATH="${java_home}/bin:${sdk_root}/platform-tools:\${PATH}"
ENV
if ! cmp -s "${jdk_tmp}/env.sh" "$env_file" 2> /dev/null; then cp "${jdk_tmp}/env.sh" "$env_file"; fi

scripts/java-check.sh > /dev/null \
  || { echo "LỖI: scripts/java-check.sh không nhận JDK vừa cài. Xem lại JDK_RELEASE, xóa ${jdk_dir} rồi chạy lại." >&2; exit 1; }
echo "Sẵn sàng. Chạy: source ${env_file} && scripts/check.sh"
