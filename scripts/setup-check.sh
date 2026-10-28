#!/usr/bin/env bash
# Checks the parts of an app's setup that otherwise only fail at the end of an auto-run session, or on a
# tester's phone (app lessons L-002, L-011, L-012, L-027 of camera-pose; template lesson L-059):
#   1. every file listed in docs/ops/required-assets.txt answers 200 at the staging address on R2 (curl -I);
#   2. the host of that address resolves through 1.1.1.1 and 8.8.8.8, not only through the local resolver: those
#      resolvers validate DNSSEC, and a zone whose DNSSEC fails there does not resolve on many phones;
#   3. with --staging: the STAGING_GOOGLE_SERVICES_JSON secret is not the fake file and has the
#      <applicationId>.staging app, and every App Distribution group in STAGING_TESTER_GROUPS (default "team")
#      exists, checked with the STAGING_APP_DISTRIBUTION_CREDENTIALS service account when gcloud is available;
#   4. the app's own checks in scripts/setup-check-app.sh, when the app has that file (the template does not). It is
#      sourced at the end and may use ok, problem, note, http_status, cdn_hint, $STAGING, $work and $base_url.
#      SETUP_CHECK_APP_CHECKS replaces the path; "none" (no such file) turns it off, as the script tests do.
# Every R2 request that gets 000 (no connection), 429 or 5xx is asked once more after SETUP_CHECK_RETRY_SECONDS
# (default 5), so one passing hiccup of R2 does not open setup-broken for a whole day. A required file that answers
# 404 or 403 is asked once more with a cache-busting query: a 200 then means the file is on R2 but the CDN still
# serves an old answer (camera-pose, 2026-10-27: a 404 cached for a year after the upload; template lesson L-113).
#
# Usage:  scripts/setup-check.sh [--staging]
#   Without --staging it needs no secret: a developer runs it at the end of docs/ops/setup-new-app.md.
#   With --staging it reads the two secrets from the environment variables GOOGLE_SERVICES and
#   DISTRIBUTION_CREDENTIALS (start-run.yml, env-canary.yml). Secret values are never printed.
# Runs on a GitHub runner or a developer machine, not in the claude --cloud environment: its proxy blocks R2.
# Exit 0 all good, 1 something is wrong (each problem on its own line).
# Messages are in Vietnamese for the team; comments are in English.
set -euo pipefail
cd "$(dirname "$0")/.."

STAGING="no"
case "${1:-}" in
  "") ;;
  --staging) STAGING="yes" ;;
  *) echo "Cách dùng: scripts/setup-check.sh [--staging]" >&2; exit 1 ;;
esac

# Tests replace the inputs read from the repository.
assets_list="${SETUP_CHECK_ASSETS_LIST:-docs/ops/required-assets.txt}"
base_url="${SETUP_CHECK_ASSETS_BASE_URL:-$(grep -E '"ASSETS_BASE_URL", *"\\"https://' app/build.gradle.kts \
  | head -n 1 | grep -oE 'https://[^\\"]+' || true)}"
package="${SETUP_CHECK_PACKAGE:-$(sed -n 's/^ *applicationId = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1)}"
groups="${STAGING_TESTER_GROUPS:-team}"
app_checks="${SETUP_CHECK_APP_CHECKS:-scripts/setup-check-app.sh}"
retry_seconds="${SETUP_CHECK_RETRY_SECONDS:-5}"

problems=0
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
umask 077

ok() { echo "ok   - $*"; }
problem() {
  problems=$((problems + 1))
  if [ -n "${GITHUB_ACTIONS:-}" ]; then echo "::error::$*"; else echo "LỖI - $*"; fi
}
note() {
  if [ -n "${GITHUB_ACTIONS:-}" ]; then echo "::warning::$*"; else echo "CHÚ Ý - $*"; fi
}
# HTTP status of a URL (curl options after the URL). 000, 429 and 5xx are asked once more. Prints the code curl
# gave, empty when curl printed none.
http_status() { # <url> [curl options]
  local url="$1" code
  shift
  code="$(curl -sS --max-time 20 -w '%{http_code}' "$@" "$url" 2> /dev/null || true)"
  if [[ -z "$code" || "$code" == "000" || "$code" == "429" || "$code" == 5?? ]]; then
    sleep "$retry_seconds"
    code="$(curl -sS --max-time 20 -w '%{http_code}' "$@" "$url" 2> /dev/null || true)"
  fi
  printf '%s' "$code"
}
# For a file that answered 404 or 403: prints a hint when the same file answers 200 with a cache-busting query,
# that is, the file is on R2 and the CDN still serves an old answer for the plain URL. Other codes get no hint.
cdn_hint() { # <url> <code of the plain URL>
  local code
  case "${2:-}" in 404 | 403) ;; *) return 0 ;; esac
  code="$(http_status "$1?setup-check-nocache=$(date +%s)$RANDOM" -o /dev/null -I)"
  if [ "$code" = "200" ]; then
    printf ' File đã có trên R2 nhưng CDN còn giữ câu trả lời cũ cho đúng địa chỉ này: xóa bộ nhớ đệm của URL này (Cloudflare: Caching, Configuration, Custom Purge), và đặt Cache Rule cho mã 404 với thời hạn ngắn.'
  fi
}

echo "== File bắt buộc trên R2 staging"
if ! [[ "$base_url" =~ ^https://[^/]+/.*/$|^https://[^/]+/$ ]]; then
  problem "không đọc được địa chỉ R2 staging (ASSETS_BASE_URL của bản staging trong app/build.gradle.kts)."
elif [ ! -f "$assets_list" ]; then
  problem "không có $assets_list (danh sách file R2 bắt buộc)."
else
  while IFS= read -r line || [ -n "$line" ]; do
    path="${line%%#*}"
    path="$(echo "$path" | tr -d '[:space:]')"
    [ -n "$path" ] || continue
    code="$(http_status "${base_url}${path}" -o /dev/null -I)"
    if [ "$code" = "200" ]; then
      ok "${base_url}${path}"
    else
      problem "${base_url}${path} trả '${code:-không kết nối được}', cần 200. Đưa file lên R2 staging (docs/ops/setup-new-app.md, phần 3).$(cdn_hint "${base_url}${path}" "$code")"
    fi
  done < "$assets_list"
fi

echo "== Phân giải tên miền file tĩnh qua DNS công cộng"
host="${base_url#https://}"
host="${host%%/*}"
resolve() { # <resolver> <host>; prints the IPv4 addresses
  if command -v dig > /dev/null 2>&1; then
    dig +short +time=5 +tries=2 "@$1" "$2" A 2> /dev/null || true
  else
    nslookup "$2" "$1" 2> /dev/null | awk '/^Name:/ {name = 1} name && /^Address/ {print $NF}' || true
  fi
}
if [ -n "$host" ]; then
  for resolver in 1.1.1.1 8.8.8.8; do
    if resolve "$resolver" "$host" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$'; then
      ok "$host phân giải được qua $resolver"
    else
      problem "$host không phân giải được qua $resolver. Thường do DNSSEC của vùng tên miền lỗi: máy dùng DNS này (nhiều điện thoại) không tải được file. Kiểm bằng 'dig @$resolver $host', chọn tên miền thuộc vùng có DNSSEC ổn định (docs/ops/setup-new-app.md, phần 3)."
    fi
  done
fi

if [ "$STAGING" = "yes" ]; then
  echo "== Bí mật Firebase staging"
  staging_package="${package}.staging"
  if [ -z "${GOOGLE_SERVICES:-}" ]; then
    problem "thiếu bí mật STAGING_GOOGLE_SERVICES_JSON (docs/ops/staging-and-runs.md, mục 1.3)."
  else
    printf '%s' "$GOOGLE_SERVICES" > "$work/google-services.json"
    if ! jq -e . "$work/google-services.json" > /dev/null 2>&1; then
      problem "STAGING_GOOGLE_SERVICES_JSON không phải JSON hợp lệ."
    elif jq -e '.project_info.project_id == "fake-staging-project"' "$work/google-services.json" > /dev/null; then
      problem "STAGING_GOOGLE_SERVICES_JSON đang là file cấu hình giả. Thêm google-services.json của project Firebase staging thật (docs/ops/staging-and-runs.md, mục 1.1 và 1.3)."
    elif ! jq -e --arg pkg "$staging_package" \
      '[.client[]? | select(.client_info.android_client_info.package_name == $pkg)] | length > 0' \
      "$work/google-services.json" > /dev/null; then
      problem "STAGING_GOOGLE_SERVICES_JSON không có app $staging_package. Thêm app này vào project Firebase staging rồi tải lại google-services.json."
    else
      ok "STAGING_GOOGLE_SERVICES_JSON có app $staging_package"
    fi
  fi

  echo "== Nhóm người test trên App Distribution"
  project_number="$(jq -r '.project_info.project_number // empty' "$work/google-services.json" 2> /dev/null || true)"
  token=""
  if [ -z "${DISTRIBUTION_CREDENTIALS:-}" ]; then
    problem "thiếu bí mật STAGING_APP_DISTRIBUTION_CREDENTIALS (docs/ops/staging-and-runs.md, mục 1.3)."
  elif [ -z "$project_number" ]; then
    note "không kiểm được nhóm người test: chưa có google-services.json hợp lệ."
  elif ! command -v gcloud > /dev/null 2>&1; then
    note "không kiểm được nhóm người test: máy không có gcloud."
  else
    printf '%s' "$DISTRIBUTION_CREDENTIALS" > "$work/credentials.json"
    # A private gcloud config, so the service account never stays signed in on the machine.
    if CLOUDSDK_CONFIG="$work/gcloud" gcloud auth activate-service-account --key-file="$work/credentials.json" \
      --quiet > /dev/null 2>&1; then
      token="$(CLOUDSDK_CONFIG="$work/gcloud" gcloud auth print-access-token 2> /dev/null || true)"
    fi
    [ -n "$token" ] || problem "không đăng nhập được bằng STAGING_APP_DISTRIBUTION_CREDENTIALS. Kiểm tra khóa của tài khoản dịch vụ (docs/ops/staging-and-runs.md, mục 1.1)."
  fi
  if [ -n "$token" ]; then
    # The token goes through a header file, so it never appears in the command line.
    printf 'Authorization: Bearer %s\n' "$token" > "$work/auth-header"
    IFS=',' read -r -a group_list <<< "$groups"
    for group in "${group_list[@]}"; do
      group="$(echo "$group" | tr -d '[:space:]')"
      [ -n "$group" ] || continue
      code="$(curl -sS -o /dev/null --max-time 20 -w '%{http_code}' -H @"$work/auth-header" \
        "https://firebaseappdistribution.googleapis.com/v1/projects/${project_number}/groups/${group}" 2> /dev/null || true)"
      case "$code" in
        200) ok "nhóm người test '$group' có trên App Distribution" ;;
        404) problem "App Distribution chưa có nhóm người test '$group' (biến STAGING_TESTER_GROUPS, mặc định team). Tạo nhóm trong Firebase, App Distribution, tab Testers & Groups (docs/ops/staging-and-runs.md, mục 1.1)." ;;
        *) note "không kiểm được nhóm người test '$group' (HTTP ${code:-không kết nối được})." ;;
      esac
    done
  fi
fi

if [ "$app_checks" != "none" ] && [ -f "$app_checks" ]; then
  # shellcheck source=/dev/null
  . "$app_checks"
fi

if [ "$problems" -gt 0 ]; then
  echo "== Thiết lập còn $problems lỗi."
  exit 1
fi
echo "== Thiết lập: QUA"
