#!/usr/bin/env bash
# Says whether main-guard passed on the newest commit of main that it has to check (L-055, plan T-041).
#
# Usage:  scripts/main-guard-status.sh [<sha>]     (default: origin/main after a fetch)
# Walks the first-parent history back from <sha> and stops at the first commit that has a main-guard run or
# changes code (main-guard ignores docs-only commits, paths-ignore of main-guard.yml):
#   0  green run                      XANH
#   1  red run                        ĐỎ
#   2  run still going                CHỜ (with the time the workflow run was created, for wait-run.sh --since)
#   3  code but no run (no push event from GitHub, L-054), a run that ended without a verdict (cancelled,
#      timed out, ...), or no run within MAX_DEPTH commits                                   CHƯA KIỂM
#   4  GitHub did not answer (unknown commit, network), or a wrong argument                  LỖI
# A commit is skipped as docs-only only when GitHub lists its files, all of them docs, and it has one parent.
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
  [[ $# -eq 1 && "$1" =~ ^[0-9a-f]{7,40}$ ]] || { echo "LỖI: cách dùng: scripts/main-guard-status.sh [<sha>]" >&2; exit 4; }
  HEAD_SHA="$1"
else
  git fetch -q origin main && HEAD_SHA="$(git rev-parse origin/main)" ||
    { echo "LỖI: không lấy được nhánh chính (git fetch origin main)." >&2; exit 4; }
fi

# Based on guard_state of find-breaking-commit.sh (copied, so that script is not changed): the newest main-guard
# run on a commit, as success | failure | pending | none, or "other <conclusion>" for a run that ended without a
# verdict (cancelled, timed_out, skipped, neutral, action_required, stale).
guard_state() {
  local sha="$1" runs
  runs="$(gh api "repos/$REPO/commits/$sha/check-runs?check_name=$CHECK_NAME&per_page=20" --jq '.check_runs')" || return 1
  [[ -n "$runs" ]] || return 1
  jq -r '
    if length == 0 then "none"
    else (sort_by(.started_at) | last) as $r
      | if $r.status != "completed" then "pending"
        elif $r.conclusion == "success" then "success"
        elif $r.conclusion == "failure" then "failure"
        else "other " + ($r.conclusion // "unknown") end
    end' <<<"$runs"
}

# When the newest main-guard workflow run of a commit was created: wait-run.sh --since keeps runs created after
# the time given, and a run is created before its check run starts.
created_at() {
  gh api "repos/$REPO/actions/workflows/main-guard.yml/runs?head_sha=$1&per_page=1" --jq '.workflow_runs[0].created_at // empty'
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
  read -r state detail <<<"$answer"
  short="${current:0:7}"
  case "$state" in
    success) say "XANH: main-guard qua trên $short."; exit 0 ;;
    failure) say "ĐỎ: main-guard không qua trên $short. Không bắt đầu phiên khi nhánh chính đỏ."; exit 1 ;;
    pending)
      created="$(created_at "$current" 2>/dev/null)" || created=""
      say "CHỜ: main-guard đang chạy trên $short (lần chạy tạo lúc ${created:-không rõ}). Chờ xong rồi kích hoạt lại; phiên chờ bằng scripts/wait-run.sh --workflow main-guard.yml --since ${created:-<giờ tạo lần chạy>}."
      exit 2 ;;
    other)
      say "CHƯA KIỂM: $short có lần chạy main-guard kết thúc mà không có kết quả ($detail). $HOW_TO_RUN"
      exit 3 ;;
  esac
  # Files outside docs/ and *.md: main-guard runs for them (paths-ignore of main-guard.yml). No file list (GitHub
  # cuts very large commits) or a merge commit (files listed against the first parent only) counts as code.
  shape="$(gh api "repos/$REPO/commits/$current" --jq '"\([.files[].filename | select((startswith("docs/") or endswith(".md")) | not)] | length) \(.files | length) \(.parents | length)"' 2>/dev/null)" ||
    unreadable "$current"
  read -r code_files all_files parents <<<"$shape"
  if [[ "$code_files" != "0" || "$all_files" == "0" || "${parents:-0}" -gt 1 ]]; then
    say "CHƯA KIỂM: $short có thay đổi code nhưng main-guard chưa chạy (GitHub có thể đã không phát sự kiện push, L-054). $HOW_TO_RUN"
    exit 3
  fi
  current="$(parent_of "$current" 2>/dev/null)" || unreadable "$current"
done

say "CHƯA KIỂM: không thấy lần chạy main-guard nào trong $MAX_DEPTH commit gần nhất của main từ ${HEAD_SHA:0:7}. $HOW_TO_RUN"
exit 3
