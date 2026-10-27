---
ticket: T-069
title: "`scripts/local-setup.sh`: cài JDK và Android SDK trên máy dev để chạy thử bằng đúng `scripts/check.sh`"
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 6.3M         # cỡ S, số gợi ý của scripts/token-usage.py estimate (2026-10-27), không nâng tay (camera-pose L-081)
blocking_points: [process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-069: `SCRIPTS/LOCAL-SETUP.SH` CÀI JDK VÀ ANDROID SDK TRÊN MÁY DEV

## 1. Tóm tắt

Thêm `scripts/local-setup.sh`, chỉ cho máy dev: trên macOS hay Linux, không cần sudo, cài JDK Temurin `jdk-21.0.13+9` vào `~/.local/share/camera-pose/jdk/` và các gói Android SDK mà `scripts/cloud-setup.sh` cài (đọc thẳng từ file đó, không chép phiên bản; tên gói phải đúng dạng gói của `sdkmanager`) vào `~/android-sdk`, kiểm mã băm của file tải về trước khi giải nén vào chỗ (thư mục tạm nằm trong thư mục đích), rồi ghi `env.sh` để `source` trước `scripts/check.sh`. Chạy lại không tải gì; JDK cài dở (không chạy được) thì cài lại; `--check` chỉ báo còn thiếu gì. Script từ chối chạy trong môi trường cloud (có `~/.cloud-setup-version`) và từ chối đường dẫn tương đối. Bản cài thử đã chạy thật trên máy dev (Mac arm64): cài vào một HOME tạm 113 giây, lần hai 1 giây, `scripts/check.sh` qua với `env.sh` đó; sau khi sửa theo rà soát, cài lại JDK và command-line tools 65 giây (mục 4.1). 60 test mới trong `scripts/tests/run.sh` (curl, Adoptium, danh sách gói của Google và `sdkmanager` giả; một test chạy `scripts/cloud-setup.sh` thật trong HOME tạm): 41 thất bại đúng lý do trên khung rỗng, 19 đối chứng qua; với bản cài thử cả 60 qua; 16 đột biến đều bị bắt.

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là đề xuất 4e sau RUN-20261027-01, Tuấn chọn ngày 2026-10-27. Bằng chứng: máy dev không có JDK (`/usr/libexec/java_home` báo "Unable to locate a Java Runtime"); các lần chạy thử trước khi duyệt của T-064, T-065 và lần kiểm PR #179 phải tự tải Temurin 21 và Android SDK vào thư mục tạm của phiên (`env64.sh`), còn T-063, T-065 bản đầu chỉ chạy được một khối của `scripts/tests/run.sh` thay cho `check.sh` vì "máy không có JDK". `docs/CLAUDE.md` đòi chạy thử bằng đúng lệnh phiên dùng (`scripts/check.sh`, L-001, L-010).

| Yêu cầu | Tiêu chí | Cách hiểu của AI |
| --- | --- | --- |
| Một nguồn phiên bản | Bản build command-line tools và danh sách gói SDK đọc từ `scripts/cloud-setup.sh` (dòng `CMDLINE_TOOLS_ZIP="commandlinetools-linux-<số>_latest.zip"` và các dòng `"<gói>"` sau `--install \`); `cloud-setup.sh` đổi thì `local-setup.sh` cài theo, không sửa gì. Mỗi tên gói phải khớp `^[a-z-]+(;[A-Za-z0-9._-]+)*$`, không thì dừng (dạng của `cloud-setup.sh` đổi, ví dụ mất `> /dev/null` sau gói cuối, thì không đưa dòng `systemProp...` cho `sdkmanager`); test chạy `cloud-setup.sh` thật với `sdkmanager` giả và so đúng các gói nó cài | `cloud-setup.sh` được dán nguyên vào môi trường cloud nên không đọc được file khác; vì vậy `local-setup.sh` đọc nó, không có tệp phiên bản thứ ba. JDK không có ở `cloud-setup.sh` (môi trường cloud có sẵn JDK) nên `JDK_RELEASE` nằm ở `local-setup.sh`; `scripts/java-check.sh` kiểm JDK đó trong khoảng Gradle chạy được |
| Không cần sudo, cài vào thư mục người dùng, chỉ máy dev | JDK ở `${XDG_DATA_HOME:-~/.local/share}/camera-pose/jdk/<bản>`, SDK ở `~/android-sdk` (như `cloud-setup.sh`), đổi được bằng `--prefix`, `--sdk` (đường dẫn tuyệt đối; tương đối thì mã 2). Có `~/.cloud-setup-version` (môi trường cloud) thì dừng, mã 1, chỉ về dòng `export ANDROID_HOME` của `CLAUDE.md`. Thiếu `curl`, `jq`, `unzip`, `tar`, `python3`, `shasum` thì dừng với tên công cụ, cả với `--check` | Không đụng `/Library/Java`, Homebrew hay `~/.zshrc`; người tự `source` tệp `env.sh`. Phiên tự chạy đọc `docs/CLAUDE.md` khi soạn plan; câu mới nói rõ chỉ máy dev, và script tự từ chối nếu phiên vẫn chạy nó |
| Kiểm mã băm | JDK: SHA-256 do API Adoptium trả cho đúng bản và kiến trúc, đường tải phải bắt đầu bằng `https://github.com/adoptium/`; command-line tools: SHA-1 trong `repository2-3.xml` của Google (đọc bằng `python3 -I`, theo đúng `<url>` của zip). Sai thì dừng, mã 1, không để lại thư mục cài dở | `curl --proto '=https' --connect-timeout 20 --retry 2`. Mục 8 câu 2 nói mỗi mã băm bảo vệ được gì. Thư mục tạm nằm trong thư mục đích (`mktemp -d <đích>/.tmp.XXXXXX`) nên bước chuyển vào chỗ là đổi tên trên cùng ổ đĩa, không phải chép rồi xóa (Linux có `/tmp` trên `tmpfs`); JDK chỉ coi là có khi `java -version` chạy được |
| Chạy lại an toàn | Lần hai không tải gì, in hai dòng "Bỏ qua:", `env.sh` giữ nguyên nội dung, không còn thư mục `.tmp.*`. JDK có mà không chạy được (cài dở) thì in "không chạy được (cài dở?): cài lại" và chỉ tải lại JDK | Bước nào xong (`java -version` chạy, có `sdkmanager`) thì bỏ qua; `sdkmanager --install` tự bỏ gói đã có; giấy phép SDK được chấp nhận mỗi lần và in một dòng |
| Dùng được cho `check.sh` | `source <prefix>/env.sh && scripts/check.sh` qua trên máy dev; `scripts/java-check.sh` nhận JDK vừa cài (không nhận thì dừng, mã 1, bảo xóa thư mục JDK rồi chạy lại). `scripts/java-check.sh` không đổi | `env.sh` đặt `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`, thêm `bin` của JDK và `platform-tools` vào `PATH`. Gradle dùng `~/.gradle` thật của máy, không đổi |
| macOS và Linux, arm64 và x86_64 | Mac: `JAVA_HOME` là `<bản>/Contents/Home`, tải `commandlinetools-mac-<số>`; Linux: `JAVA_HOME` là `<bản>`, tải `commandlinetools-linux-<số>`; hệ khác thì dừng với thông báo | Kiến trúc `arm64`/`aarch64` thành `aarch64`, `x86_64`/`amd64` thành `x64` (tên của Adoptium) |

Thiết kế tham chiếu: Không có thiết kế riêng (script).

ADR liên quan: ADR-0009 (phiên chạy dưới tài khoản và môi trường của người, không thêm bí mật), ADR-0030 (đồng bộ dự án mẫu: `scripts/` là phần của mẫu, file mới của app không bị đè).

## 3. Giải pháp

### 3.1. Cách làm

Theo mẫu của `scripts/cloud-setup.sh` (cài SDK vào `~/android-sdk`, giải nén trong thư mục tạm rồi chuyển vào chỗ, `yes | sdkmanager --licenses`, `sdkmanager --install`) và của `scripts/cloud-env-check.sh` (đọc `SETUP_VERSION` từ `cloud-setup.sh` bằng `sed`).

**Bước 3 (commit test):** khung rỗng `scripts/local-setup.sh` (đủ để test biên dịch và đọc `JDK_RELEASE`), cùng khối test ở mục 4 chèn vào `scripts/tests/run.sh` ngay sau test `cloud-env-check.sh names compileSdk when it cannot be read` (khối này dùng lại hàm `fake_java` của khối `cloud-env-check` ngay trên):

```bash
#!/usr/bin/env bash
# Skeleton of T-069, step 3: the real script comes in step 4.
JDK_RELEASE="jdk-21.0.13+9"
echo "LỖI: scripts/local-setup.sh chưa làm (T-069)." >&2
exit 1
```

**Bước 4 (commit code):** `scripts/local-setup.sh` đúng như bản cài thử đã chạy (mục 4.1):

```bash
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
```

**`CLAUDE.md`**, mục Lệnh, thêm ngay sau dòng "Môi trường cloud chưa có `ANDROID_HOME` ...":

```markdown
- Máy dev chưa có JDK hay Android SDK: `scripts/local-setup.sh` cài vào thư mục người dùng, không cần sudo (JDK Temurin, gói SDK đọc từ `scripts/cloud-setup.sh`, kiểm mã băm), rồi `source` tệp `env.sh` mà script in ra trước `scripts/check.sh`. `scripts/local-setup.sh --check` xem còn thiếu gì, không tải. Chỉ dùng trên máy dev; môi trường cloud dùng `scripts/cloud-setup.sh` và dòng `export ANDROID_HOME` ở trên.
```

**`docs/CLAUDE.md`**, mục Khi viết plan, dòng "Kỹ thuật hay thư viện lần đầu dùng trong dự án (tiêu chí E từ 2): ...", thêm vào cuối dòng:

```markdown
 Máy dev chưa có JDK hay SDK thì chạy `scripts/local-setup.sh` rồi `source` tệp `env.sh` nó in ra, không tự tải JDK vào thư mục tạm hay chỉ chạy một khối test thay cho `check.sh` (T-069). Chỉ trên máy dev: phiên cloud không chạy script này (script tự từ chối); thiếu `ANDROID_HOME` thì làm theo dòng `export` trong `CLAUDE.md`, mục Lệnh.
```

**`docs/ops/setup-new-app.md`**, phần 1, bước 1 ("Cài Android Studio. Dùng JDK đi kèm ..."), thêm một gạch đầu dòng con sau dòng `export JAVA_HOME=...` của macOS:

```markdown
   - Không cài Android Studio: `scripts/local-setup.sh` cài JDK và Android SDK vào thư mục người dùng (cùng gói SDK với môi trường cloud, kiểm mã băm), rồi `source` tệp `env.sh` mà script in ra; `scripts/local-setup.sh --check` xem còn thiếu gì. `scripts/java-check.sh` kiểm JDK này như mọi JDK khác.
```

**Đề xuất về dự án mẫu (`docs/ops/from-app.md`):** `scripts/local-setup.sh` và khối test của nó có ích cho mọi app của mẫu (máy dev nào cũng cần chạy thử bằng `check.sh`). Phiên ghi việc này vào mục Cần người kiểm tra của báo cáo; người mở issue `from-app`. Trong lúc chờ, đồng bộ mẫu lần sau không đè `scripts/local-setup.sh` (mẫu không có file cùng tên), nhưng khối test nằm trong `scripts/tests/run.sh` của mẫu: khi xung đột ở `run.sh`, người đồng bộ giữ khối `local-setup.sh` (ghi trong mô tả PR của task). Bản đề xuất bỏ tên app khỏi đường cài mặc định (ví dụ `${XDG_DATA_HOME:-~/.local/share}/<tên repo>` đọc từ thư mục gốc của repo), và đổi các câu nhắc `CLAUDE.md` của app cho hợp với mẫu.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/local-setup.sh` | Bước 3: khung rỗng; bước 4: script ở mục 3.1 (quyền thực thi) |
| Sửa | `scripts/tests/run.sh` | Khối test `local-setup.sh` (mục 4), sau khối `cloud-env-check.sh` |
| Sửa | `CLAUDE.md` | Mục Lệnh: một dòng (mục 3.1) |
| Sửa | `docs/CLAUDE.md` | Mục Khi viết plan, dòng tiêu chí E từ 2: hai câu (mục 3.1) |
| Sửa | `docs/ops/setup-new-app.md` | Phần 1, bước 1: một gạch đầu dòng con (mục 3.1) |
| Sửa | `docs/features/quality-gate/tickets/T-069-local-setup.md` | Trạng thái theo quy trình |
| Sửa | `docs/features/quality-gate/plans/T-069-local-setup.md` | Nhật ký thực hiện |

Nơi đọc file bị đổi (L-020 của mẫu): `grep -rn 'cloud-setup.sh' scripts/ .github/` cho thấy `cloud-env-check.sh`, `env-canary.sh` và khối test `cloud-env-check` đọc `SETUP_VERSION`, chỉ dòng đó; `local-setup.sh` chỉ đọc hai dòng khác của `cloud-setup.sh` và không sửa nó, nên không file nào đổi theo. `CLAUDE.md` và `docs/CLAUDE.md` không có script nào đọc từng dòng (chỉ `scripts/foundation.py` đọc ADR). `scripts/java-check.sh` không đổi. `docs/ops/setup-new-app.md`: không script nào đọc nội dung; `.github/workflows/env-canary.yml` (dòng 72), `.github/workflows/start-run.yml` (dòng 96) và `scripts/setup-check.sh` (dòng 12, 63, 83) chỉ dẫn tên file và phần 3 trong chú thích hay thông báo; plan chỉ thêm một gạch con ở phần 1, không đổi tên hay số phần. Các file khác nhắc tới ba file bị sửa chỉ nhắc tên hay tên mục trong chú thích hoặc thông báo, plan không đổi tên mục nào, nên không bị ảnh hưởng: `.github/workflows/pr-check.yml` (dòng 4, 53, 93 đến 100: chạy `scripts/tests/run.sh` và kiểm có `python3`, `jq`; khối mới không cần công cụ nào khác), `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt` và `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt` (chú thích dẫn `CLAUDE.md`, bước 10; dòng mới nằm ở mục Lệnh), `scripts/device-session.sh` (thông báo dẫn `docs/CLAUDE.md`, mục Khi phân tích một phiên thử), `scripts/merge-if-green.sh` và `scripts/status.py` (dẫn `docs/CLAUDE.md`, mục Trang tiến độ); câu mới nằm ở mục Khi viết plan. `scripts/tests/run.sh` chạy trong `scripts/check.sh` và `pr-check.yml`; khối mới chỉ dùng `bash`, `jq`, `python3`, `tar`, `unzip`, `shasum` (có trên runner Ubuntu và macOS), không gọi mạng; nó chạy cả `scripts/cloud-setup.sh` thật với HOME tạm và `sdkmanager` giả có sẵn, nên `cloud-setup.sh` không tải gì và chỉ ghi vào HOME tạm (`.bashrc`, `.gradle/`, `.cloud-setup-version`).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm vào app (`gradle/libs.versions.toml` không đổi). Script tải JDK Temurin (Eclipse Adoptium, GPLv2 kèm Classpath Exception) và Android command-line tools (Android Software Development Kit License, `sdkmanager --licenses` chấp nhận như `cloud-setup.sh`) về máy dev; không vào bản build.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** API Adoptium hay `repository2-3.xml` của Google đổi dạng (khóa JSON, thẻ `<checksum>`), hoặc bản `jdk-21.0.13+9` bị gỡ.
- **Điều kiện chuyển:** test qua (các test dùng câu trả lời giả) nhưng lần chạy thật trên máy dev ở bước 5 dừng với "LỖI: Adoptium không trả đường tải hay mã SHA-256" hay "danh sách gói của Google không có mã SHA-1".
- **Cách làm thay:** không có trong phiên: phiên không chạy lệnh tải thật của `local-setup.sh` (bước 5 chỉ chạy `scripts/check.sh` của môi trường cloud), nên điểm này chỉ lộ ra trên máy dev sau khi merge. Người ghi phản hồi; plan sau đổi `JDK_RELEASE` hay cách đọc mã băm. Tiêu chí và test giữ nguyên.

## 4. Kiểm thử

Khối test chèn vào `scripts/tests/run.sh` (bước 3), ngay sau test `cloud-env-check.sh names compileSdk when it cannot be read`:

```bash
# local-setup.sh installs on a developer machine the JDK and the SDK packages of cloud-setup.sh, checking every
# download against its published checksum (T-069). curl, the Adoptium API, Google's package list and sdkmanager
# are fakes; the JDK in the fake archive answers like fake_java above.
ls_dir="$work/local-setup"
mkdir -p "$ls_dir/bin" "$ls_dir/build"
ls_release="$(sed -n 's/^JDK_RELEASE="\(.*\)"$/\1/p' scripts/local-setup.sh)"
ls_tools="$(sed -nE 's/^CMDLINE_TOOLS_ZIP="commandlinetools-linux-([0-9]+)_latest\.zip"$/\1/p' scripts/cloud-setup.sh)"
# Fake curl: `curl -fsSL <url> -o <file>`; answers from $ls_dir by the shape of the URL and logs every URL.
cat > "$ls_dir/bin/curl" << 'SH'
#!/usr/bin/env bash
url="" out=""
while [ "$#" -gt 0 ]; do case "$1" in -o) out="$2"; shift 2 ;; --proto | --connect-timeout | --retry) shift 2 ;; -*) shift ;; *) url="$1"; shift ;; esac; done
echo "$url" >> "$LS_DIR/curl.log"
echo "$*" > /dev/null
case "$url" in
  *assets/release_name/*) src="$LS_DIR/jdk.json" ;;
  *repository2-3.xml) src="$LS_DIR/repository.xml" ;;
  *commandlinetools-mac-*_latest.zip) src="$LS_DIR/tools-mac.zip" ;;
  *commandlinetools-linux-*_latest.zip) src="$LS_DIR/tools-linux.zip" ;;
  *.tar.gz) src="$LS_DIR/jdk.tar.gz" ;;
  *) exit 22 ;;
esac
[ -f "$src" ] || exit 22
cp "$src" "$out"
SH
chmod +x "$ls_dir/bin/curl"
# ls_jdk <mac|linux> <Java major>: writes the fake JDK archive and the Adoptium answer (right checksum).
ls_jdk() {
  local home="$ls_dir/build/jdk/$ls_release"
  rm -rf "$ls_dir/build/jdk"
  [ "$1" = mac ] && home="$home/Contents/Home"
  mkdir -p "$home/bin"
  fake_java "$2"
  cp "$work/fake-jdk/bin/java" "$home/bin/java"
  tar -czf "$ls_dir/jdk.tar.gz" -C "$ls_dir/build/jdk" "$ls_release"
  jq -n --arg sum "$(shasum -a 256 "$ls_dir/jdk.tar.gz" | cut -d' ' -f1)" \
    '{binaries: [{package: {link: "https://github.com/adoptium/temurin21-binaries/releases/download/fake/jdk.tar.gz", checksum: $sum}}]}' > "$ls_dir/jdk.json"
}
# The fake command-line tools: sdkmanager logs its arguments and creates the package directories.
mkdir -p "$ls_dir/build/tools/cmdline-tools/bin"
cat > "$ls_dir/build/tools/cmdline-tools/bin/sdkmanager" << 'SH'
#!/usr/bin/env bash
echo "$*" >> "$LS_DIR/sdkmanager.log"
if [ "$1" = --install ]; then shift; for p in "$@"; do mkdir -p "$ANDROID_HOME/${p//;//}"; done; fi
SH
chmod +x "$ls_dir/build/tools/cmdline-tools/bin/sdkmanager"
# Two zips with different content, so each OS has its own SHA-1 in the fake package list.
for ls_os in mac linux; do
  (cd "$ls_dir/build/tools" && LS_OS_NAME="$ls_os" python3 -c 'import os, zipfile
with zipfile.ZipFile("../../tools-" + os.environ["LS_OS_NAME"] + ".zip", "w") as z:
    i = zipfile.ZipInfo("cmdline-tools/bin/sdkmanager"); i.external_attr = 0o755 << 16
    z.writestr(i, open("cmdline-tools/bin/sdkmanager").read())
    z.writestr("cmdline-tools/" + os.environ["LS_OS_NAME"] + ".txt", os.environ["LS_OS_NAME"])')
done
ls_repo() { # <sha1 for the mac zip> <sha1 for the linux zip>
  printf '<sdk>\n  <archive>\n    <complete>\n      <size>1</size>\n      <checksum type="sha1">%s</checksum>\n      <url>commandlinetools-mac-%s_latest.zip</url>\n    </complete>\n  </archive>\n  <archive>\n    <complete>\n      <size>1</size>\n      <checksum type="sha1">%s</checksum>\n      <url>commandlinetools-linux-%s_latest.zip</url>\n    </complete>\n  </archive>\n</sdk>\n' \
    "$1" "$ls_tools" "${2:-$1}" "$ls_tools" > "$ls_dir/repository.xml"
}
ls_calls() { if [ -f "$ls_dir/curl.log" ]; then wc -l < "$ls_dir/curl.log" | tr -d ' '; else echo 0; fi; }
ls_mac_sha1="$(shasum -a 1 "$ls_dir/tools-mac.zip" | cut -d' ' -f1)"
ls_linux_sha1="$(shasum -a 1 "$ls_dir/tools-linux.zip" | cut -d' ' -f1)"
# ls_run <home> [args...]: runs local-setup.sh with the fakes as a Mac on arm64 unless LS_OS, LS_ARCH say otherwise;
# prints the exit code; output in $ls_dir/out.
ls_run() {
  local home="$1"; shift
  set +e
  HOME="$home" XDG_DATA_HOME="" LS_DIR="$ls_dir" CURL="$ls_dir/bin/curl" \
    LOCAL_SETUP_OS="${LS_OS:-Darwin}" LOCAL_SETUP_ARCH="${LS_ARCH:-arm64}" \
    LOCAL_SETUP_ADOPTIUM_API=https://api.example.test/v3 LOCAL_SETUP_GOOGLE_REPO=https://dl.example.test/repository \
    scripts/local-setup.sh "$@" > "$ls_dir/out" 2>&1
  echo $?
  set -e
}
ls_jdk mac 21
ls_repo "$ls_mac_sha1" "$ls_linux_sha1"

expect "local-setup.sh has a JDK release" yes "$([[ "$ls_release" == jdk-* ]] && echo yes || echo no)"
expect "local-setup.sh reads the command-line tools build from cloud-setup.sh" "command-line tools: $ls_tools" \
  "$(scripts/local-setup.sh --print-versions | sed -n 's/^command-line tools: /command-line tools: /p')"
printf 'CMDLINE_TOOLS_ZIP="commandlinetools-linux-42_latest.zip"\n"${SDKMANAGER}" --install \\\n  "platform-tools" \\\n  "platforms;android-99" > /dev/null\n' \
  > "$ls_dir/cloud-setup-fixture.sh"
expect "local-setup.sh reads the package list after --install" "gói SDK: platform-tools platforms;android-99" \
  "$(LOCAL_SETUP_CLOUD_SETUP="$ls_dir/cloud-setup-fixture.sh" scripts/local-setup.sh --print-versions | grep '^gói SDK')"
printf 'echo nothing here\n' > "$ls_dir/cloud-setup-broken.sh"
expect "local-setup.sh stops when cloud-setup.sh has no versions" 1 \
  "$(LOCAL_SETUP_CLOUD_SETUP="$ls_dir/cloud-setup-broken.sh" ls_run "$ls_dir/home-broken" --print-versions)"
expect "local-setup.sh names cloud-setup.sh when it cannot read the versions" 1 \
  "$(grep -c '^LỖI: không đọc được phiên bản command-line tools' "$ls_dir/out" || true)"
expect "local-setup.sh refuses an unknown argument" 2 "$(ls_run "$ls_dir/home-args" --prefx /tmp)"
expect "local-setup.sh refuses --prefix without a directory" 2 "$(ls_run "$ls_dir/home-args" --prefix)"

expect "local-setup.sh --check on an empty machine reports missing" 1 "$(ls_run "$ls_dir/home-mac" --check)"
expect "local-setup.sh --check makes no download" 0 "$(ls_calls)"
rm -f "$ls_dir/curl.log" "$ls_dir/sdkmanager.log"
expect "local-setup.sh installs on a Mac" 0 "$(ls_run "$ls_dir/home-mac")"
expect "local-setup.sh downloads the JDK info, the JDK, the package list and the tools" 4 \
  "$(ls_calls)"
expect "local-setup.sh asks Adoptium for the mac aarch64 JDK" 1 \
  "$(grep -c "release_name/eclipse/${ls_release//+/%2B}?architecture=aarch64&image_type=jdk&os=mac" "$ls_dir/curl.log" || true)"
expect "local-setup.sh downloads the mac command-line tools" 1 \
  "$(grep -c "/commandlinetools-mac-${ls_tools}_latest.zip$" "$ls_dir/curl.log" || true)"
expect "local-setup.sh installs the packages of cloud-setup.sh" \
  "--install $(scripts/local-setup.sh --print-versions | sed -n 's/^gói SDK: //p')" \
  "$(grep '^--install' "$ls_dir/sdkmanager.log")"
ls_env="$ls_dir/home-mac/.local/share/camera-pose/env.sh"
expect "local-setup.sh writes JAVA_HOME to Contents/Home on a Mac" 1 \
  "$(grep -c "^export JAVA_HOME=\"$ls_dir/home-mac/.local/share/camera-pose/jdk/$ls_release/Contents/Home\"$" "$ls_env" || true)"
expect "local-setup.sh writes ANDROID_HOME" 1 "$(grep -c "^export ANDROID_HOME=\"$ls_dir/home-mac/android-sdk\"$" "$ls_env" || true)"
expect "local-setup.sh writes ANDROID_SDK_ROOT" 1 "$(grep -c "^export ANDROID_SDK_ROOT=\"$ls_dir/home-mac/android-sdk\"$" "$ls_env" || true)"
expect "local-setup.sh puts the JDK and platform-tools on PATH" 1 \
  "$(grep -cF "/jdk/$ls_release/Contents/Home/bin:$ls_dir/home-mac/android-sdk/platform-tools:\${PATH}\"" "$ls_env" || true)"
expect "local-setup.sh accepts the SDK licences" 1 "$(grep -c '^--licenses$' "$ls_dir/sdkmanager.log" || true)"
expect "local-setup.sh leaves no temp dir behind" 0 \
  "$(find "$ls_dir/home-mac" -name '.tmp.*' 2> /dev/null | wc -l | tr -d ' ')"
expect "env.sh gives a Java that java-check.sh accepts" 0 \
  "$(set +e; (. "$ls_env"; scripts/java-check.sh > /dev/null 2>&1); echo $?)"
cp "$ls_env" "$ls_dir/env-first.sh" 2> /dev/null || : > "$ls_dir/env-first.sh"
rm -f "$ls_dir/curl.log"
expect "local-setup.sh runs again without error" 0 "$(ls_run "$ls_dir/home-mac")"
expect "local-setup.sh downloads nothing the second time" 0 "$(ls_calls)"
expect "local-setup.sh says both steps were skipped" 2 "$(grep -c '^Bỏ qua:' "$ls_dir/out" || true)"
expect "local-setup.sh leaves env.sh as it was" same "$(cmp -s "$ls_dir/env-first.sh" "$ls_env" && echo same || echo changed)"
expect "local-setup.sh --check after the install passes" 0 "$(ls_run "$ls_dir/home-mac" --check)"
expect "local-setup.sh --check lists every package" 3 "$(grep -c '^QUA: \(platform-tools\|platforms;\|build-tools;\)' "$ls_dir/out" || true)"

ls_pkg="$(scripts/local-setup.sh --print-versions | sed -n 's/^gói SDK: //p' | awk '{print $NF}' || true)"
mv "$ls_dir/home-mac/android-sdk/${ls_pkg//;//}" "$ls_dir/pkg-aside" 2> /dev/null || true
expect "local-setup.sh --check fails when one SDK package is missing" 1 "$(ls_run "$ls_dir/home-mac" --check)"
expect "local-setup.sh --check names the missing package" 1 "$(grep -cxF "THIẾU: $ls_pkg" "$ls_dir/out" || true)"
mv "$ls_dir/pkg-aside" "$ls_dir/home-mac/android-sdk/${ls_pkg//;//}" 2> /dev/null || true
# A JDK left half-installed (java does not run) is installed again, not taken as present.
printf '#!/bin/sh\nexit 1\n' > "$ls_dir/home-mac/.local/share/camera-pose/jdk/$ls_release/Contents/Home/bin/java" 2> /dev/null || true
rm -f "$ls_dir/curl.log"
expect "local-setup.sh installs a JDK that does not run again" 0 "$(ls_run "$ls_dir/home-mac")"
expect "local-setup.sh says the broken JDK is installed again" 1 "$(grep -c 'không chạy được (cài dở?): cài lại' "$ls_dir/out" || true)"
expect "local-setup.sh downloads only the JDK again" 2 "$(ls_calls)"
expect "local-setup.sh refuses a relative --prefix" 2 "$(ls_run "$ls_dir/home-args" --prefix relative/dir)"
expect "local-setup.sh refuses a relative --sdk" 2 "$(ls_run "$ls_dir/home-args" --sdk sdk)"
mkdir -p "$ls_dir/home-cloud" && echo 3 > "$ls_dir/home-cloud/.cloud-setup-version"
expect "local-setup.sh refuses to run in a cloud environment" 1 "$(ls_run "$ls_dir/home-cloud")"
expect "local-setup.sh points a cloud environment at CLAUDE.md" 1 "$(grep -c 'chỉ dành cho máy dev' "$ls_dir/out" || true)"
expect "local-setup.sh makes no download in a cloud environment" no "$([ -e "$ls_dir/home-cloud/android-sdk" ] && echo yes || echo no)"
expect "local-setup.sh refuses a machine without a needed tool" 1 \
  "$(LOCAL_SETUP_TOOLS="curl no-such-tool-t069" ls_run "$ls_dir/home-tools" --check)"
expect "local-setup.sh names the missing tool" 1 "$(grep -c '^LỖI: máy thiếu công cụ: no-such-tool-t069' "$ls_dir/out" || true)"
printf 'CMDLINE_TOOLS_ZIP="commandlinetools-linux-42_latest.zip"\n"${SDKMANAGER}" --install \\\n  "platform-tools" \\\n  "platforms;android-99"\n  "systemProp.org.gradle.internal.repository.max.retries=8" \\\n' \
  > "$ls_dir/cloud-setup-shape.sh"
expect "local-setup.sh refuses a package name that is not an SDK package" 1 \
  "$(LOCAL_SETUP_CLOUD_SETUP="$ls_dir/cloud-setup-shape.sh" ls_run "$ls_dir/home-shape" --print-versions)"
expect "local-setup.sh names the bad package" 1 "$(grep -c 'gói không hợp lệ: systemProp' "$ls_dir/out" || true)"
# The packages read from cloud-setup.sh are the ones cloud-setup.sh itself passes to sdkmanager: run the real
# cloud-setup.sh with a temp HOME and a fake sdkmanager already in place (so it downloads nothing).
mkdir -p "$ls_dir/home-cloudsetup/android-sdk/cmdline-tools/latest/bin"
printf '#!/bin/sh\n[ "$1" = --install ] && { shift; echo "$*" > "$HOME/install-args"; }\nexit 0\n' \
  > "$ls_dir/home-cloudsetup/android-sdk/cmdline-tools/latest/bin/sdkmanager"
chmod +x "$ls_dir/home-cloudsetup/android-sdk/cmdline-tools/latest/bin/sdkmanager"
HOME="$ls_dir/home-cloudsetup" bash scripts/cloud-setup.sh > /dev/null 2>&1 || true
expect "local-setup.sh installs exactly what cloud-setup.sh installs" \
  "$(cat "$ls_dir/home-cloudsetup/install-args" 2> /dev/null || echo 'cloud-setup.sh did not run')" \
  "$(scripts/local-setup.sh --print-versions 2> /dev/null | sed -n 's/^gói SDK: //p' || true)"
# A wrong checksum stops before anything is unpacked into place.
jq '.binaries[0].package.checksum = "0000"' "$ls_dir/jdk.json" > "$ls_dir/jdk.json.new" && mv "$ls_dir/jdk.json.new" "$ls_dir/jdk.json"
expect "local-setup.sh refuses a JDK with a wrong SHA-256" 1 "$(ls_run "$ls_dir/home-badjdk")"
expect "local-setup.sh names the SHA-256 mismatch" 1 "$(grep -c '^LỖI: mã SHA-256 của JDK không khớp' "$ls_dir/out" || true)"
expect "local-setup.sh leaves no JDK after a wrong SHA-256" no \
  "$([ -e "$ls_dir/home-badjdk/.local/share/camera-pose/jdk/$ls_release" ] && echo yes || echo no)"
ls_jdk mac 21
jq '.binaries[0].package.link = "https://evil.example.test/jdk.tar.gz"' "$ls_dir/jdk.json" > "$ls_dir/jdk.json.new" && mv "$ls_dir/jdk.json.new" "$ls_dir/jdk.json"
expect "local-setup.sh refuses a JDK link outside github.com/adoptium" 1 "$(ls_run "$ls_dir/home-badlink")"
expect "local-setup.sh names the JDK link" 1 "$(grep -c '^LỖI: đường tải JDK không nằm ở https://github.com/adoptium/' "$ls_dir/out" || true)"
ls_jdk mac 21
ls_repo 0000000000000000000000000000000000000000 0000000000000000000000000000000000000000
expect "local-setup.sh refuses command-line tools with a wrong SHA-1" 1 "$(ls_run "$ls_dir/home-badtools")"
expect "local-setup.sh names the SHA-1 mismatch" 1 "$(grep -c '^LỖI: mã SHA-1 của commandlinetools-mac-' "$ls_dir/out" || true)"
expect "local-setup.sh leaves no command-line tools after a wrong SHA-1" no \
  "$([ -e "$ls_dir/home-badtools/android-sdk/cmdline-tools/latest" ] && echo yes || echo no)"
ls_repo "$ls_mac_sha1" "$ls_linux_sha1"
printf '<sdk/>\n' > "$ls_dir/repository.xml"
expect "local-setup.sh stops when Google's list has no checksum for the tools" 1 "$(ls_run "$ls_dir/home-nosum")"
ls_repo "$ls_mac_sha1" "$ls_linux_sha1"

# Linux x86_64: other URLs, JAVA_HOME at the top of the JDK.
ls_jdk linux 21
rm -f "$ls_dir/curl.log"
expect "local-setup.sh installs on Linux x86_64" 0 "$(LS_OS=Linux LS_ARCH=x86_64 ls_run "$ls_dir/home-linux")"
expect "local-setup.sh asks Adoptium for the linux x64 JDK" 1 "$(grep -c 'architecture=x64&image_type=jdk&os=linux' "$ls_dir/curl.log" || true)"
expect "local-setup.sh downloads the linux command-line tools" 1 \
  "$(grep -c "/commandlinetools-linux-${ls_tools}_latest.zip$" "$ls_dir/curl.log" || true)"
expect "local-setup.sh writes JAVA_HOME at the top of the JDK on Linux" 1 \
  "$(grep -c "^export JAVA_HOME=\"$ls_dir/home-linux/.local/share/camera-pose/jdk/$ls_release\"$" \
    "$ls_dir/home-linux/.local/share/camera-pose/env.sh" || true)"
expect "local-setup.sh refuses another system" 1 "$(LS_OS=FreeBSD ls_run "$ls_dir/home-bsd")"

# A JDK that java-check.sh does not accept is reported, not left silently in env.sh.
ls_jdk mac 26
expect "local-setup.sh stops when java-check.sh refuses the JDK" 1 "$(ls_run "$ls_dir/home-java26")"
expect "local-setup.sh names java-check.sh and the JDK dir to delete" 1 \
  "$(grep -c "^LỖI: scripts/java-check.sh không nhận JDK vừa cài. Xem lại JDK_RELEASE, xóa $ls_dir/home-java26/.local/share/camera-pose/jdk/$ls_release" "$ls_dir/out" || true)"
ls_jdk mac 21
# --prefix and --sdk move the install.
expect "local-setup.sh honours --prefix and --sdk" 0 "$(ls_run "$ls_dir/home-custom" --prefix "$ls_dir/custom" --sdk "$ls_dir/custom-sdk")"
expect "local-setup.sh writes env.sh under --prefix" 1 "$(grep -c "^export ANDROID_HOME=\"$ls_dir/custom-sdk\"$" "$ls_dir/custom/env.sh" || true)"
```

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Một nguồn phiên bản | Test script | `local-setup.sh reads the command-line tools build from cloud-setup.sh`, `... reads the package list after --install`, `... installs the packages of cloud-setup.sh`, `... stops when cloud-setup.sh has no versions`, `... names cloud-setup.sh when it cannot read the versions` |
| Tham số | Test script | `... refuses an unknown argument`, `... refuses --prefix without a directory`, `... honours --prefix and --sdk`, `... writes env.sh under --prefix` |
| Cài trên Mac, `env.sh` đúng | Test script | `... installs on a Mac`, `... downloads the JDK info, the JDK, the package list and the tools`, `... asks Adoptium for the mac aarch64 JDK`, `... downloads the mac command-line tools`, `... writes JAVA_HOME to Contents/Home on a Mac`, `... writes ANDROID_HOME`, `env.sh gives a Java that java-check.sh accepts` |
| Chạy lại an toàn | Test script | `... runs again without error`, `... downloads nothing the second time`, `... says both steps were skipped`, `... leaves env.sh as it was` |
| `--check` | Test script | `... --check on an empty machine reports missing`, `... --check makes no download`, `... --check after the install passes`, `... --check lists every package`, `... --check fails when one SDK package is missing`, `... --check names the missing package` |
| Kiểm mã băm, không cài dở | Test script | `... refuses a JDK with a wrong SHA-256`, `... names the SHA-256 mismatch`, `... leaves no JDK after a wrong SHA-256`, `... refuses command-line tools with a wrong SHA-1`, `... names the SHA-1 mismatch`, `... leaves no command-line tools after a wrong SHA-1`, `... stops when Google's list has no checksum for the tools` |
| Chỉ máy dev, đường dẫn tuyệt đối, đủ công cụ | Test script | `... refuses to run in a cloud environment`, `... points a cloud environment at CLAUDE.md`, `... makes no download in a cloud environment`, `... refuses a relative --prefix`, `... refuses a relative --sdk`, `... refuses a machine without a needed tool`, `... names the missing tool` |
| Dạng gói đúng, đúng gói của `cloud-setup.sh` | Test script | `... refuses a package name that is not an SDK package`, `... names the bad package`, `... installs exactly what cloud-setup.sh installs` |
| JDK cài dở, đường tải lạ, không để lại thư mục tạm, `env.sh` đủ | Test script | `... installs a JDK that does not run again`, `... says the broken JDK is installed again`, `... downloads only the JDK again`, `... refuses a JDK link outside github.com/adoptium`, `... names the JDK link`, `... leaves no temp dir behind`, `... writes ANDROID_SDK_ROOT`, `... puts the JDK and platform-tools on PATH`, `... accepts the SDK licences` |
| Linux x86_64, hệ khác | Test script | `... installs on Linux x86_64`, `... asks Adoptium for the linux x64 JDK`, `... downloads the linux command-line tools`, `... writes JAVA_HOME at the top of the JDK on Linux`, `... refuses another system` |
| JDK ngoài khoảng của `java-check.sh` | Test script | `... stops when java-check.sh refuses the JDK`, `... names java-check.sh and the JDK dir to delete` |
| Cài thật trên máy dev và `check.sh` qua | Kiểm tra thủ công (người: tải thật JDK và SDK khoảng 350 MB trên máy dev; phiên cloud không chạy lệnh tải này) | Sau khi merge, trên máy dev: `scripts/local-setup.sh`, chạy lại lần hai (in "Bỏ qua:" hai lần, không tải), `scripts/local-setup.sh --check` mã 0, rồi `source ~/.local/share/camera-pose/env.sh && scripts/check.sh` qua. Đã chạy với bản cài thử (mục 4.1) |
| Tài liệu chỉ đúng lệnh | Kiểm tra thủ công (người: đọc tài liệu) | `CLAUDE.md` mục Lệnh, `docs/CLAUDE.md` dòng tiêu chí E và `docs/ops/setup-new-app.md` phần 1 có câu ở mục 3.1 |

**Ngoại lệ viết test trước:** ba chỗ tài liệu (`CLAUDE.md`, `docs/CLAUDE.md`, `docs/ops/setup-new-app.md`) không có test. Lệnh tải thật (Adoptium, Google) không chạy trong test (test không gọi mạng): kiểm bằng lần chạy thật trên máy dev (mục 4.1 và dòng kiểm tra thủ công).

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-27, trên nhánh chính `084d366`, bản cài thử của `scripts/local-setup.sh` (không commit), máy dev macOS arm64 (không có JDK). Lần cài thật đầu và `check.sh` dùng bản trước rà soát; bản ở mục 3.1 là bản sau rà soát, đã cài lại thật (dòng Sửa theo rà soát).
- **Lệnh và kết quả cài thật** (HOME tạm, tải thật từ Adoptium và Google):
  - `HOME=<tạm> scripts/local-setup.sh`: mã 0, 113 giây; tải `OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.13_9.tar.gz` (SHA-256 `bf036b35…d04a` khớp API) và `commandlinetools-mac-13114758_latest.zip` (SHA-1 `c3e06a19…1d24` khớp `repository2-3.xml`); JDK 335 MB, SDK 517 MB.
  - Chạy lại: mã 0, 1 giây, hai dòng "Bỏ qua:", không tải.
  - `--check`: mã 0, sáu dòng `QUA` (JDK, command-line tools, `platform-tools`, `platforms;android-36`, `build-tools;36.0.0`, `env.sh`).
  - `--print-versions`: `JDK: jdk-21.0.13+9 (Temurin)`, `command-line tools: 13114758`, `gói SDK: platform-tools platforms;android-36 build-tools;36.0.0`.
  - `source <tạm>/.local/share/camera-pose/env.sh && scripts/check.sh` (HOME thật, `~/.gradle` có sẵn): mã 0 sau 462 giây (Gradle `BUILD SUCCESSFUL in 3m 55s`, nhiều tác vụ lấy từ bộ nhớ đệm), 1183 test JVM, 0 lỗi, 97 ảnh screenshot, "Cổng chất lượng: QUA".
  - Cả `scripts/tests/run.sh` có khối test mới và bản cài thử, cùng `env.sh` đó: mã 0 sau 344 giây, 783 ok, 0 FAIL.
- **Sửa theo rà soát độc lập (2026-10-27):** chỉ máy dev (từ chối khi có `~/.cloud-setup-version`), đường dẫn tuyệt đối, kiểm dạng tên gói, thư mục tạm trong thư mục đích, JDK chỉ coi là có khi `java -version` chạy, đường tải JDK phải ở `https://github.com/adoptium/`, `curl --proto '=https' --connect-timeout 20 --retry 2`, đọc mã SHA-1 bằng `python3 -I`, kiểm công cụ cần có, dòng báo đã chấp nhận giấy phép. Cài lại thật vào HOME tạm cũ sau khi xóa JDK và command-line tools (gói SDK giữ): mã 0, 65 giây (tải lại JDK và zip, cả hai mã băm khớp, `python3` đọc đúng SHA-1 trong `repository2-3.xml` thật); chạy lại 2 giây, "Bỏ qua:" hai lần; `--check` mã 0; không còn thư mục `.tmp.*`; `java -version` in `openjdk version "21.0.13"`, `scripts/java-check.sh` nhận (Java 21, cần 17 đến 24). `check.sh` không chạy lại: phần sửa chỉ ở script cài, không đổi JDK hay SDK mà `check.sh` dùng.
- **Test của script (L-075):** khối test ở mục 4 chạy trên đầu `scripts/tests/run.sh` (dòng 1 đến 26) cộng hàm `fake_java`, trong một khối tạm:
  - Trên khung rỗng của bước 3: 41 thất bại đúng lý do (khung in "chưa làm" và trả 1: không đọc được phiên bản, không cài, không có `env.sh`, không có dòng thông báo được chờ; `installs exactly what cloud-setup.sh installs` so các gói `cloud-setup.sh` thật đưa cho `sdkmanager` giả với chuỗi rỗng), 19 đối chứng qua: `local-setup.sh has a JDK release`, `local-setup.sh stops when cloud-setup.sh has no versions`, `local-setup.sh --check on an empty machine reports missing`, `local-setup.sh --check makes no download`, `local-setup.sh leaves no temp dir behind`, `local-setup.sh downloads nothing the second time`, `local-setup.sh --check fails when one SDK package is missing`, `local-setup.sh refuses to run in a cloud environment`, `local-setup.sh makes no download in a cloud environment`, `local-setup.sh refuses a machine without a needed tool`, `local-setup.sh refuses a package name that is not an SDK package`, `local-setup.sh refuses a JDK with a wrong SHA-256`, `local-setup.sh leaves no JDK after a wrong SHA-256`, `local-setup.sh refuses a JDK link outside github.com/adoptium`, `local-setup.sh refuses command-line tools with a wrong SHA-1`, `local-setup.sh leaves no command-line tools after a wrong SHA-1`, `local-setup.sh stops when Google's list has no checksum for the tools`, `local-setup.sh refuses another system`, `local-setup.sh stops when java-check.sh refuses the JDK`. Các đối chứng là trường hợp mã 1 hay không tải gì; dòng thông báo đi kèm của chúng (`names ...`, `points ...`) đều thất bại trên khung rỗng.
  - Với bản cài thử: 60/60 qua, `failures=0`. Cả `scripts/tests/run.sh` có khối mới, với `env.sh` của lần cài thật: mã 0 sau 352 giây, 802 ok, 0 FAIL.
  - Đột biến (mỗi lần sửa một chỗ của bản cài thử rồi chạy lại khối test), số test thất bại: bỏ kiểm SHA-256 của JDK 3; bỏ kiểm SHA-1 của command-line tools 3; bỏ `Contents/Home` trên Mac 10; luôn tải lại JDK 2; đổi tên kiến trúc `x64` thành `amd64` 1; bỏ `java-check.sh` sau khi cài 2; `--check` không báo gói SDK thiếu 1; giải nén JDK vào chỗ trước khi kiểm mã băm 1; bỏ kiểm dạng tên gói 2; không từ chối môi trường cloud 3; nhận đường dẫn tương đối 2; coi JDK là có chỉ vì `bin/java` thực thi được 3; bỏ kiểm đường tải JDK 2; bỏ `sdkmanager --licenses` 1; tải zip của Mac trên Linux 1; bỏ kiểm công cụ 1. Cả 16 bị bắt. Đột biến "nhận đường dẫn tương đối" tạo thư mục rác `relative/`, `sdk/` ở gốc repo của bản chép thử: đúng lỗi mà quy tắc đường dẫn tuyệt đối chặn; đã xóa.
  - Chưa có test riêng cho "thư mục tạm trong thư mục đích" (đột biến đưa thư mục tạm về `${TMPDIR:-/tmp}` vẫn qua: kết quả giống nhau trên máy test, chỉ khác khi `/tmp` là ổ khác); test `leaves no temp dir behind` kiểm không còn thư mục tạm.
- **Ghi chú:** bản cài thử ban đầu có `sub(/>.*/, "")` thừa trong lệnh `awk` đọc danh sách gói; đã bỏ.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (quy trình phiên tự chạy): sửa quy tắc chạy thử trước khi duyệt trong `docs/CLAUDE.md`. Mức Cao, `risk_score` để trống, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script mới, một khối test, ba chỗ tài liệu |
| B. Độ rõ của yêu cầu | 1 | Đề xuất 4e rõ; AI chọn chỗ cài, nguồn mã băm, bản JDK (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi nhánh có test với tải giả; lần tải thật chỉ kiểm được trên máy dev |
| D. Ảnh hưởng chéo | 1 | Đọc `cloud-setup.sh` mà không sửa; thêm khối vào `run.sh` dùng chung |
| E. Độ mới của kỹ thuật | 1 | Biến thể của `cloud-setup.sh`; cài thật đã chạy thử |
| **Tổng** | 5 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `scripts/tests/run.sh` (khối mới, sau khối `cloud-env-check`); `CLAUDE.md`, `docs/CLAUDE.md` (một câu mỗi file).
- Task khác có thể xung đột: T-065 (`approved`) sửa `scripts/tests/run.sh` ở khối `setup-check`, `CLAUDE.md` mục Lệnh (dòng `setup-check.sh`) và `docs/template-sync.txt`: khối và dòng khác nhau, chạy lần lượt. Các ticket đề xuất sau RUN-20261027-01 (T-066 đến T-068) sửa `docs/auto-run.md`, bảng tra lỗi, `run-check.py`, `save-branch.sh`, workflow: không trùng file của T-069, trừ `scripts/tests/run.sh` (khối khác) và có thể `docs/CLAUDE.md` (mục khác).
- Đề xuất thứ tự chạy: độc lập, chạy ở vị trí nào cũng được; nên chạy sớm để các plan sau chạy thử bằng `check.sh` trên máy dev.

## 7. Điều kiện dừng riêng

Không có.

## 8. Câu hỏi cho người duyệt

1. Chỗ cài: JDK ở `~/.local/share/camera-pose/jdk/`, SDK ở `~/android-sdk` (như môi trường cloud), không sửa `~/.zshrc`, người tự `source` tệp `env.sh`. Đề xuất của AI: đồng ý; ai muốn mặc định thì tự thêm dòng `source` vào `~/.zshrc`.
2. Mã băm lấy từ nơi nhà phát hành công bố, không ghi cứng trong script, để `cloud-setup.sh` đổi bản command-line tools thì `local-setup.sh` không phải sửa. Mỗi mã băm bảo vệ được: JDK tải từ `github.com/adoptium`, mã SHA-256 từ `api.adoptium.net` (hai máy chủ khác nhau: chặn được file tải về bị hỏng hay bị thay ở một bên); command-line tools và mã SHA-1 cùng ở `dl.google.com` (chỉ chặn file tải về bị hỏng, không chặn máy chủ bị chiếm); các gói SDK do `sdkmanager` tự kiểm. Đề xuất của AI: đồng ý; ghi cứng mã băm của command-line tools thì chặn được cả máy chủ bị chiếm nhưng phải sửa hai nơi mỗi lần nâng cấp.
3. Bản JDK: `jdk-21.0.13+9` (LTS, bản các lần chạy thử ngày 26, 27 đã dùng với `check.sh` qua; `java-check.sh` cho 17 đến 24). Đề xuất của AI: đồng ý.
4. Đề xuất về dự án mẫu sau khi merge (mục 3.1). Đề xuất của AI: đồng ý.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
