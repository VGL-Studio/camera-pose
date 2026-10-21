#!/usr/bin/env bash
# Checks, at the start of an auto-run session, that the cloud environment was built from the current
# scripts/cloud-setup.sh (auto-run.md, section 2). An environment built from an older copy fails downloads
# for reasons no amount of waiting fixes (RUN-20261016-01), so the session stops at once instead.
# Exit 0: the environment is current. Exit 1: it is not; the message says what the person must do.
# Messages are in Vietnamese for the team; comments are in English.
set -euo pipefail
cd "$(dirname "$0")/.."

expected="$(sed -n 's/^SETUP_VERSION=\([0-9][0-9]*\)$/\1/p' scripts/cloud-setup.sh)"
[ -n "${expected}" ] || { echo "LỖI: scripts/cloud-setup.sh không có dòng SETUP_VERSION=<số>." >&2; exit 1; }

version_file="${CLOUD_SETUP_VERSION_FILE:-${HOME}/.cloud-setup-version}"
actual="$(cat "${version_file}" 2>/dev/null || echo "không có")"
init_script="${GRADLE_USER_HOME:-${HOME}/.gradle}/init.d/maven-central-mirror.gradle"

problems=()
[ "${actual}" = "${expected}" ] ||
  problems+=("môi trường dựng từ setup script phiên bản ${actual}, nhánh chính đang ở phiên bản ${expected}")
[ -f "${init_script}" ] || problems+=("thiếu ${init_script}")

if [ "${#problems[@]}" -gt 0 ]; then
  echo "Môi trường cloud chưa dùng scripts/cloud-setup.sh mới nhất:" >&2
  printf -- '- %s\n' "${problems[@]}" >&2
  echo "Người cần làm: dán lại toàn bộ scripts/cloud-setup.sh vào Setup script của môi trường" >&2
  echo "(docs/ops/staging-and-runs.md, mục 2.1, bước 2), rồi lập danh sách chạy mới." >&2
  exit 1
fi
echo "Môi trường cloud dùng setup script phiên bản ${expected}."
