#!/usr/bin/env bash
# Says whether main-guard passed on the newest commit of main that it has to check (L-055, plan T-041).
#
# Usage:  scripts/main-guard-status.sh [<sha>]     (default: origin/main after a fetch)
# Walks the first-parent history back from <sha> and stops at the first commit that has a main-guard run or
# changes code (main-guard ignores docs-only commits, paths-ignore of main-guard.yml):
#   0  green run                      XANH
#   1  red run                        ĐỎ
#   2  run still going                CHỜ (with the time the run started, for wait-run.sh --since)
#   3  code but no run (no push event from GitHub, L-054), or no run within MAX_DEPTH commits   CHƯA KIỂM
#   4  GitHub did not answer (unknown commit, network)   LỖI
# start-run.yml calls it before firing the routine; the session calls it at its start (docs/auto-run.md, section 2).
# Uses only the GitHub REST API through `gh api`.
#
# Changing this file changes the quality gate: high-risk plan, cross-review required.

set -euo pipefail

CHECK_NAME="guard"
MAX_DEPTH="${MAX_DEPTH:-30}"

REPO="${GITHUB_REPOSITORY:-}"
if [[ -z "$REPO" ]]; then
  REMOTE_URL="$(git config --get remote.origin.url)"
  REPO="$(echo "$REMOTE_URL" | sed -E 's#(git@github.com:|https://github.com/)##; s#\.git$##')"
fi

if [[ $# -ge 1 ]]; then
  [[ $# -eq 1 && "$1" =~ ^[0-9a-f]{7,40}$ ]] || { echo "cách dùng: scripts/main-guard-status.sh [<sha>]" >&2; exit 1; }
  HEAD_SHA="$1"
else
  git fetch -q origin main
  HEAD_SHA="$(git rev-parse origin/main)"
fi

# guard_state of find-breaking-commit.sh (copied, so that script is not changed), plus the start time: the newest
# main-guard run on a commit, as success | failure | pending | none, then when it started.
guard_state() {
  local sha="$1" runs
  runs="$(gh api "repos/$REPO/commits/$sha/check-runs?check_name=$CHECK_NAME&per_page=20" --jq '.check_runs')" || return 1
  [[ -n "$runs" ]] || return 1
  jq -r '
    if length == 0 then "none"
    else (sort_by(.started_at) | last) as $r
      | (if $r.status != "completed" then "pending"
         elif $r.conclusion == "success" then "success"
         elif $r.conclusion == "failure" then "failure"
         else "pending" end) + " " + ($r.started_at // "")
    end' <<<"$runs"
}

parent_of() {
  gh api "repos/$REPO/commits/$1" --jq '.parents[0].sha // empty'
}

say() { # <message>: an annotation inside GitHub Actions, plain text elsewhere
  if [[ -n "${GITHUB_ACTIONS:-}" && "$1" != XANH* ]]; then echo "::error::$1"; else echo "$1"; fi
}

unreadable() { # <sha>
  say "LỖI: không đọc được commit ${1:0:7} hay lần chạy main-guard của nó từ GitHub. Kiểm tra mạng và quyền đọc (checks: read), rồi chạy lại."
  exit 4
}

HOW_TO_RUN="Chạy main-guard trên main: gh api -X POST repos/$REPO/actions/workflows/main-guard.yml/dispatches -f ref=main (hoặc Actions > main-guard > Run workflow), chờ xanh rồi kích hoạt lại."

current="$HEAD_SHA"
depth=0
while [[ -n "$current" && "$depth" -lt "$MAX_DEPTH" ]]; do
  depth=$((depth + 1))
  answer="$(guard_state "$current" 2>/dev/null)" || unreadable "$current"
  read -r state started <<<"$answer"
  short="${current:0:7}"
  case "$state" in
    success) say "XANH: main-guard qua trên $short."; exit 0 ;;
    failure) say "ĐỎ: main-guard không qua trên $short. Không bắt đầu phiên khi nhánh chính đỏ."; exit 1 ;;
    pending) say "CHỜ: main-guard đang chạy trên $short (bắt đầu ${started:-không rõ}). Chờ xong rồi kích hoạt lại; phiên chờ bằng scripts/wait-run.sh --workflow main-guard.yml --since ${started:-<giờ bắt đầu>}."; exit 2 ;;
  esac
  # Files outside docs/ and *.md: main-guard runs for them (paths-ignore of main-guard.yml).
  files="$(gh api "repos/$REPO/commits/$current" --jq '[.files[].filename | select((startswith("docs/") or endswith(".md")) | not)] | length' 2>/dev/null)" ||
    unreadable "$current"
  if [[ "$files" != "0" ]]; then
    say "CHƯA KIỂM: $short có thay đổi code nhưng main-guard chưa chạy (GitHub có thể đã không phát sự kiện push, L-054). $HOW_TO_RUN"
    exit 3
  fi
  current="$(parent_of "$current" 2>/dev/null)" || unreadable "$current"
done

say "CHƯA KIỂM: không thấy lần chạy main-guard nào trong $MAX_DEPTH commit gần nhất của main từ ${HEAD_SHA:0:7}. $HOW_TO_RUN"
exit 3
