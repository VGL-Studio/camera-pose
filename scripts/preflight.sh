#!/usr/bin/env bash
# Checks, before an auto-run session, what used to show up only once the session ran (plan T-030):
#   - the staging Firebase secret is a real file with the staging app (L-002, L-012);
#   - the App Distribution tester groups exist (L-012);
#   - the static files domain resolves through public DNS servers (L-027);
#   - every required file on R2 answers 200 (L-011).
# Runs on GitHub (preflight.yml, start-run.yml): the claude --cloud proxy blocks R2 (L-005).
# No arguments; inputs come from the environment:
#   GOOGLE_SERVICES_FILE           file the workflow wrote from STAGING_GOOGLE_SERVICES_JSON
#   DISTRIBUTION_CREDENTIALS_FILE  file the workflow wrote from STAGING_APP_DISTRIBUTION_CREDENTIALS
#   STAGING_APPLICATION_ID         default com.vglstudio.camera.pose.line.guide.staging
#   TESTER_GROUPS                  comma-separated groups, default "team"
#   ASSETS_BASE_URL                default: the staging value in app/build.gradle.kts
#   DNS_SERVERS                    default "1.1.1.1 8.8.8.8"
#   REQUIRED_ASSETS                default scripts/required-assets.txt
#   DIG_CMD, CURL_CMD, FIREBASE_CMD, PREFLIGHT_RETRY_SECONDS replace the real commands and wait in tests.
# Exit codes: 0 every check passed (warnings allowed), 1 at least one check failed.
# Secret values are never printed: jq results are only used as conditions, command output is discarded.
# Changing this file changes how a session starts: high-risk plan, cross-review required.

set -euo pipefail
cd "$(dirname "$0")/.."

STAGING_APPLICATION_ID="${STAGING_APPLICATION_ID:-com.vglstudio.camera.pose.line.guide.staging}"
TESTER_GROUPS="${TESTER_GROUPS:-team}"
DNS_SERVERS="${DNS_SERVERS:-1.1.1.1 8.8.8.8}"
REQUIRED_ASSETS="${REQUIRED_ASSETS:-scripts/required-assets.txt}"
RETRY_SECONDS="${PREFLIGHT_RETRY_SECONDS:-5}"
read -r -a DIG <<< "${DIG_CMD:-dig}"
read -r -a CURL <<< "${CURL_CMD:-curl}"
read -r -a FIREBASE <<< "${FIREBASE_CMD:-npx --yes firebase-tools@15.32.1}"

passed=0
failed=0
lines=()
say() { echo "$1"; lines+=("$1"); }
pass() { say "QUA: $1"; passed=$((passed + 1)); }
fail() { say "LỖI: $1"; failed=$((failed + 1)); }
warn() { say "CHÚ Ý: $1"; }

# 1. Staging Firebase config (same two conditions as the "Đưa cấu hình staging vào" step of staging.yml).
check_google_services() {
  local file="${GOOGLE_SERVICES_FILE:-}"
  if [[ -z "$file" || ! -s "$file" ]]; then
    fail "thiếu bí mật STAGING_GOOGLE_SERVICES_JSON."
    return
  fi
  if ! jq -e . "$file" > /dev/null 2>&1; then
    fail "bí mật STAGING_GOOGLE_SERVICES_JSON không phải JSON."
    return
  fi
  if jq -e '.project_info.project_id == "fake-staging-project"' "$file" > /dev/null 2>&1; then
    fail "STAGING_GOOGLE_SERVICES_JSON đang là file cấu hình giả, cần file của project staging thật (L-002)."
    return
  fi
  if ! jq -e --arg pkg "$STAGING_APPLICATION_ID" \
      'any(.client[]?; .client_info.android_client_info.package_name == $pkg)' "$file" > /dev/null 2>&1; then
    fail "STAGING_GOOGLE_SERVICES_JSON không có app $STAGING_APPLICATION_ID."
    return
  fi
  pass "bí mật STAGING_GOOGLE_SERVICES_JSON là file thật, có app $STAGING_APPLICATION_ID."
}

# 2. App Distribution tester groups. A command that cannot run only warns (plan T-030, section 3.4).
check_tester_groups() {
  local file="${GOOGLE_SERVICES_FILE:-}" credentials="${DISTRIBUTION_CREDENTIALS_FILE:-}" project groups_json aliases missing=()
  if [[ -z "$credentials" || ! -s "$credentials" ]]; then
    fail "thiếu bí mật STAGING_APP_DISTRIBUTION_CREDENTIALS."
    return
  fi
  project="$(jq -r '.project_info.project_number // empty' "$file" 2>/dev/null || true)"
  if [[ -z "$project" ]]; then
    warn "không kiểm được nhóm người test: bí mật Firebase không có số project."
    return
  fi
  if ! groups_json="$(GOOGLE_APPLICATION_CREDENTIALS="$credentials" \
      "${FIREBASE[@]}" appdistribution:groups:list --project "$project" --json 2> /dev/null)"; then
    warn "không kiểm được nhóm người test (lệnh firebase-tools không chạy được); bước phát staging sẽ báo nếu thiếu nhóm."
    return
  fi
  # A group is {"name": "projects/<n>/groups/<alias>"} in the API, or carries "alias".
  if ! aliases="$(jq -r '[.. | objects | (.alias? // empty),
      ((.name? // "") | strings | capture("/groups/(?<a>[^/]+)$").a? // empty)] | unique | .[]' \
      <<< "$groups_json" 2> /dev/null)"; then
    warn "không kiểm được nhóm người test (kết quả của firebase-tools không đọc được)."
    return
  fi
  IFS=',' read -r -a wanted <<< "$TESTER_GROUPS"
  for group in "${wanted[@]}"; do
    group="$(tr -d '[:space:]' <<< "$group")"
    [[ -z "$group" ]] && continue
    grep -qxF "$group" <<< "$aliases" || missing+=("$group")
  done
  if [[ "${#missing[@]}" -gt 0 ]]; then
    fail "App Distribution chưa có nhóm ${missing[*]} (biến STAGING_TESTER_GROUPS, L-012)."
    return
  fi
  pass "App Distribution có nhóm người test $TESTER_GROUPS."
}

base_url="${ASSETS_BASE_URL:-$(sed -nE 's/.*"ASSETS_BASE_URL", "\\"(https:\/\/[^"\\]*)\\"".*/\1/p' app/build.gradle.kts | head -n 1)}"
domain="$(sed -E 's#^https://##; s#/.*##' <<< "$base_url")"

# 3. Public DNS (L-027): a domain the runner resolves may still fail on phones using another resolver.
check_dns() {
  local bad=()
  if [[ -z "$domain" ]]; then
    fail "không đọc được địa chỉ ASSETS_BASE_URL của bản staging trong app/build.gradle.kts."
    return
  fi
  for server in $DNS_SERVERS; do
    if ! "${DIG[@]}" +short +time=5 +tries=2 "@$server" "$domain" A 2> /dev/null \
        | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$'; then
      fail "$domain không phân giải được qua $server (L-027, docs/ops/setup-new-app.md mục 3)."
      bad+=("$server")
    fi
  done
  [[ "${#bad[@]}" -eq 0 ]] && pass "$domain phân giải được qua $DNS_SERVERS."
  return 0
}

# 4. Required files on R2 (L-011). 000 (no connection) is retried once before counting as a failure.
check_assets() {
  local count=0 bad=0 code
  if [[ -z "$base_url" ]]; then
    fail "không có địa chỉ gốc để kiểm file R2."
    return
  fi
  if [[ ! -f "$REQUIRED_ASSETS" ]]; then
    fail "không có danh sách file R2 bắt buộc $REQUIRED_ASSETS."
    return
  fi
  while IFS= read -r path || [[ -n "$path" ]]; do
    path="$(sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//' <<< "$path")"
    [[ -z "$path" || "$path" == \#* ]] && continue
    count=$((count + 1))
    code="$("${CURL[@]}" -sS -o /dev/null -I --max-time 20 -w '%{http_code}' "${base_url%/}/$path" 2> /dev/null || true)"
    if [[ "$code" == "000" || -z "$code" ]]; then
      sleep "$RETRY_SECONDS"
      code="$("${CURL[@]}" -sS -o /dev/null -I --max-time 20 -w '%{http_code}' "${base_url%/}/$path" 2> /dev/null || true)"
    fi
    if [[ "$code" != "200" ]]; then
      fail "$path trả ${code:-000} (cần 200; L-011)."
      bad=$((bad + 1))
    fi
  done < "$REQUIRED_ASSETS"
  [[ "$bad" -eq 0 ]] && pass "$count file R2 bắt buộc đều trả 200."
  return 0
}

check_google_services
check_tester_groups
check_dns
check_assets

say "Tổng: $passed điều qua, $failed điều lỗi."
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "### Kiểm tra môi trường trước phiên"
    echo
    for line in "${lines[@]}"; do echo "- $line"; done
  } >> "$GITHUB_STEP_SUMMARY"
fi
[[ "$failed" -eq 0 ]]
