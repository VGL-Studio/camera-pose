#!/usr/bin/env bash
# Pushes a task branch each time it gets a new commit, for a bounded time (L-090, L-096).
#
# A sub-session working in a worktree only reports when it is done, so the main session runs this in the background
# instead of writing its own loop: every commit of the sub-session leaves the cloud machine within a minute.
#
# Usage:   scripts/push-on-commit.sh <branch> [--minutes N] [--interval S]
#          --minutes   how long to watch (default 120); the script then exits 0 by itself
#          --interval  seconds between two reads of the branch (default 60)
# Exit:    0 the time is up. 3 a push failed (scripts/save-branch.sh exit 3): follow "Khi không đẩy được" of
#          docs/auto-run.md. 2 bad arguments.
# Env:     PUSH_ON_COMMIT_SAVE     command that pushes (default scripts/save-branch.sh next to this script; tests)
#          PUSH_ON_COMMIT_SECONDS  watch time in seconds, overriding --minutes (tests)
# Stop it early with the session's own tool for background tasks, never with pkill -f: a pattern also matches the
# command line of the shell that runs pkill (L-082, L-096).
# Messages are in Vietnamese for the team; comments are in English.

set -uo pipefail

branch="${1:-}"
case "${branch}" in
  claude/*) ;;
  *) echo "LỖI: nhánh phải có dạng claude/... (nhận '${branch}')." >&2; exit 2 ;;
esac
shift
minutes=120
interval=60
while [ $# -gt 0 ]; do
  case "$1" in
    --minutes) minutes="${2:-}"; shift 2 ;;
    --interval) interval="${2:-}"; shift 2 ;;
    *) echo "LỖI: không hiểu tham số '$1'." >&2; exit 2 ;;
  esac
done
for value in "${minutes}" "${interval}"; do
  case "${value}" in
    '' | *[!0-9]*) echo "LỖI: --minutes và --interval là số nguyên dương." >&2; exit 2 ;;
  esac
done
if ! git rev-parse --verify -q "refs/heads/${branch}" > /dev/null; then
  echo "LỖI: không có nhánh cục bộ ${branch}." >&2
  exit 2
fi

save="${PUSH_ON_COMMIT_SAVE:-$(dirname "$0")/save-branch.sh}"
seconds="${PUSH_ON_COMMIT_SECONDS:-$((minutes * 60))}"
deadline=$(($(date +%s) + seconds))
# Start from what the remote has, so commits made before the script started are pushed on the first read.
pushed="$(git rev-parse -q --verify "refs/remotes/origin/${branch}" 2> /dev/null || true)"

echo "Theo dõi ${branch} tới $(date -u -r "${deadline}" +%H:%MZ 2> /dev/null || date -u -d "@${deadline}" +%H:%MZ), đọc mỗi ${interval} giây."
while :; do
  head="$(git rev-parse "refs/heads/${branch}")"
  if [ "${head}" != "${pushed}" ]; then
    if "${save}" "${branch}"; then
      pushed="${head}"
      echo "ĐÃ ĐẨY: ${branch} tại $(git rev-parse --short=7 "${head}") ($(date -u +%H:%MZ))."
    else
      code=$?
      if [ "${code}" -eq 3 ]; then
        echo "KHÔNG ĐẨY ĐƯỢC: ${branch} tại $(git rev-parse --short=7 "${head}"). Làm theo \"Khi không đẩy được\" (auto-run.md, sau bước 7)."
        exit 3
      fi
      echo "LỖI: lệnh đẩy trả mã ${code}." >&2
      exit "${code}"
    fi
  fi
  now="$(date +%s)"
  if [ "${now}" -ge "${deadline}" ]; then
    echo "HẾT GIỜ: dừng theo dõi ${branch}."
    exit 0
  fi
  wait_for=$((deadline - now))
  [ "${wait_for}" -gt "${interval}" ] && wait_for="${interval}"
  sleep "${wait_for}"
done
