#!/usr/bin/env bash
# Daily check of the cloud environment of the auto-run routine (docs/ops/staging-and-runs.md, section 2.4).
# Run by the env-canary routine, which .github/workflows/env-canary.yml fires every morning, so an environment
# problem shows up during the working day instead of stopping the next overnight session (RUN-20261015-02,
# RUN-20261016-01).
#
# Checks the main branch with scripts/cloud-env-check.sh and scripts/check.sh (one retry that downloads
# again). Red: opens an issue labelled env-broken, or comments on the open one; start-run.yml refuses to
# start a session while it is open. Green: closes open env-broken issues.
# Exit 0 green, 1 red. Uses only the GitHub REST API through `gh api`.
# Messages are in Vietnamese for the team; comments are in English.
set -euo pipefail
cd "$(dirname "$0")/.."

LABEL="env-broken"
# Tests replace the two checks and the wait, and skip the checkout.
ENV_CHECK_CMD="${ENV_CHECK_CMD:-scripts/cloud-env-check.sh}"
CHECK_CMD="${CHECK_CMD:-scripts/check.sh}"
RETRY_WAIT_SECONDS="${RETRY_WAIT_SECONDS:-120}"

if [[ "${CANARY_CHECKOUT:-yes}" == "yes" ]]; then
  git fetch -q origin main
  git checkout -q --detach origin/main
fi

# The routine runs this script from a non-interactive shell, which does not read ~/.bashrc where
# scripts/cloud-setup.sh records the SDK location (first canary run, issue #37).
if [[ -z "${ANDROID_HOME:-}" && -d "${HOME}/android-sdk" ]]; then
  export ANDROID_HOME="${HOME}/android-sdk"
fi

REMOTE_URL="$(git config --get remote.origin.url)"
REPO="$(echo "$REMOTE_URL" | sed -E 's#(git@github.com:|https://github.com/)##; s#\.git$##')"
COMMIT="$(git rev-parse --short HEAD)"
log="$(mktemp)"
trap 'rm -f "$log"' EXIT

reason=""
if ! "$ENV_CHECK_CMD" > "$log" 2>&1; then
  reason="\`scripts/cloud-env-check.sh\` báo lỗi môi trường cloud."
elif ! "$CHECK_CMD" > "$log" 2>&1; then
  echo "scripts/check.sh đỏ, thử lại sau ${RETRY_WAIT_SECONDS} giây và tải lại thư viện."
  sleep "$RETRY_WAIT_SECONDS"
  if ! GRADLE_REFRESH_DEPENDENCIES=true "$CHECK_CMD" > "$log" 2>&1; then
    reason="\`scripts/check.sh\` đỏ hai lần trên nhánh chính trong môi trường cloud (lần hai tải lại thư viện)."
  fi
fi

open_issues="$(gh api "repos/$REPO/issues?state=open&labels=$LABEL&per_page=10" --jq '[.[].number] | join(" ")')"

if [[ -z "$reason" ]]; then
  for number in $open_issues; do
    gh api "repos/$REPO/issues/$number/comments" -X POST \
      -f body="Kiểm tra môi trường hằng ngày xanh lại trên \`$COMMIT\`. Đóng mục này." > /dev/null
    gh api "repos/$REPO/issues/$number" -X PATCH -f state=closed > /dev/null
  done
  echo "Môi trường cloud xanh trên $COMMIT."
  exit 0
fi

# The backticks below are Markdown for the issue, not command substitution.
# shellcheck disable=SC2016
body="$(printf '%s\n\nCommit: `%s`. Trong lúc mục này còn mở, `start-run.yml` không bắt đầu phiên tự chạy.\n\nNgười cần làm: xem log, sửa môi trường (thường là dán lại `scripts/cloud-setup.sh`, docs/ops/staging-and-runs.md mục 2.1) hoặc sửa nguyên nhân, rồi chạy lại workflow `env-canary`. Lần chạy xanh tự đóng mục này.\n\n80 dòng cuối của log:\n\n```text\n%s\n```\n' \
  "$reason" "$COMMIT" "$(tail -n 80 "$log")")"
if [[ -z "$open_issues" ]]; then
  gh api "repos/$REPO/issues" -X POST -f title="Môi trường cloud của phiên tự chạy đang lỗi" \
    -f body="$body" -f "labels[]=$LABEL" > /dev/null
else
  gh api "repos/$REPO/issues/${open_issues%% *}/comments" -X POST -f body="$body" > /dev/null
fi
echo "Môi trường cloud lỗi: $reason" >&2
exit 1
