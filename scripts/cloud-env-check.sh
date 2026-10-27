#!/usr/bin/env bash
# Gate of the start-of-session check (auto-run.md, section 2). Checks, in seconds, what scripts/check.sh needs:
# the environment was built from the current scripts/cloud-setup.sh, Java can run the project's Gradle,
# the Gradle wrapper starts, and the Android SDK has the compileSdk platform. An environment that fails one of
# these fails check.sh too, for reasons no amount of waiting fixes (RUN-20261016-01; lesson L-075, from app
# lessons L-049, L-056 of camera-pose), so the session stops at once instead of after the 15 minutes of check.sh.
# JAVA_CHECK_CMD, GRADLEW_CMD, GRADLEW_TIMEOUT (seconds) and VERSIONS_TOML replace the Java check, the wrapper, its time
# limit and the version catalog in tests.
# Exit 0: the environment is ready. Exit 1: it is not; the message says what the person must do.
# Messages are in Vietnamese for the team; comments are in English.
set -euo pipefail
cd "$(dirname "$0")/.."

expected="$(sed -n 's/^SETUP_VERSION=\([0-9][0-9]*\)$/\1/p' scripts/cloud-setup.sh)"
[ -n "${expected}" ] || { echo "LỖI: scripts/cloud-setup.sh không có dòng SETUP_VERSION=<số>." >&2; exit 1; }

version_file="${CLOUD_SETUP_VERSION_FILE:-${HOME}/.cloud-setup-version}"
actual="$(cat "${version_file}" 2>/dev/null || echo "không có")"
init_script="${GRADLE_USER_HOME:-${HOME}/.gradle}/init.d/maven-central-mirror.gradle"

# The routine runs this script from a non-interactive shell, which does not read ~/.bashrc where
# scripts/cloud-setup.sh records the SDK location (same as env-canary.sh).
if [[ -z "${ANDROID_HOME:-}" && -d "${HOME}/android-sdk" ]]; then
  export ANDROID_HOME="${HOME}/android-sdk"
fi
java_check="${JAVA_CHECK_CMD:-scripts/java-check.sh}"
gradlew="${GRADLEW_CMD:-./gradlew}"
gradlew_timeout="${GRADLEW_TIMEOUT:-300}"
versions_toml="${VERSIONS_TOML:-gradle/libs.versions.toml}"
compile_sdk="$(sed -n 's/^compileSdk = "\([0-9][0-9]*\)"$/\1/p' "${versions_toml}" 2>/dev/null || true)"
# Without it the SDK platform cannot be checked; skipping that check would let a session start that check.sh stops.
[ -n "${compile_sdk}" ] ||
  { echo "LỖI: không đọc được compileSdk từ ${versions_toml} (cần dòng compileSdk = \"<số>\"), nên không kiểm được Android SDK." >&2; exit 1; }

# setup_problems need the setup script pasted again; tool_problems need the environment variables looked at.
setup_problems=()
tool_problems=()
[ "${actual}" = "${expected}" ] ||
  setup_problems+=("môi trường dựng từ setup script phiên bản ${actual}, nhánh chính đang ở phiên bản ${expected}")
[ -f "${init_script}" ] || setup_problems+=("thiếu ${init_script}")

if [ ! -d "${ANDROID_HOME:-}/platforms/android-${compile_sdk}" ]; then
  setup_problems+=("Android SDK thiếu platforms;android-${compile_sdk} (ANDROID_HOME=${ANDROID_HOME:-chưa đặt})")
fi

if java_out="$("${java_check}" 2>&1)"; then :; else
  tool_problems+=("Java của môi trường không chạy được Gradle của dự án: $(grep -m1 '^LỖI:' <<< "${java_out}" || head -n 1 <<< "${java_out}")")
fi

# --version starts the wrapper and downloads the distribution through the proxy, but runs no project task.
wrapper_cmd=("${gradlew}" --version -q)
if command -v timeout > /dev/null; then wrapper_cmd=(timeout "${gradlew_timeout}" "${wrapper_cmd[@]}"); fi
if ! wrapper_out="$("${wrapper_cmd[@]}" 2>&1)"; then
  tool_problems+=("Gradle wrapper không khởi động được (giới hạn ${gradlew_timeout} giây); 5 dòng cuối: $(tail -n 5 <<< "${wrapper_out}" | tr '\n' ' ')")
fi

if [ "${#setup_problems[@]}" -gt 0 ] || [ "${#tool_problems[@]}" -gt 0 ]; then
  echo "Môi trường cloud báo lỗi ở kiểm tra đầu phiên:" >&2
  # ${a[@]+"${a[@]}"}: bash 3.2 (macOS) treats an empty array as unbound under set -u (L-075).
  printf -- '- %s\n' ${setup_problems[@]+"${setup_problems[@]}"} ${tool_problems[@]+"${tool_problems[@]}"} >&2
  if [ "${#setup_problems[@]}" -gt 0 ]; then
    echo "Người cần làm: dán lại toàn bộ scripts/cloud-setup.sh vào Setup script của môi trường" >&2
    echo "(docs/ops/staging-and-runs.md, mục 2.1, bước 2), rồi lập danh sách chạy mới." >&2
  fi
  if [ "${#tool_problems[@]}" -gt 0 ]; then
    echo "Người cần làm: xem biến môi trường của môi trường cloud (JAVA_TOOL_OPTIONS, proxy) và scripts/cloud-setup.sh," >&2
    echo "dựa vào thông báo gốc ở trên." >&2
  fi
  exit 1
fi
echo "Môi trường cloud dùng setup script phiên bản ${expected}; Java, Gradle wrapper và SDK (android-${compile_sdk}) chạy được."
