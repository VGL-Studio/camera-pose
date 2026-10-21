#!/usr/bin/env bash
# Finds the commit that broke the main branch (ADR-0010).
#
# Usage:  scripts/find-breaking-commit.sh <red commit sha>
# Prints: the sha of the oldest red commit after the most recent green commit on the
#         first-parent history of <red commit sha>. That is the commit to blame and revert.
#
# Each commit on main gets its own main-guard run (job "guard"). Walking back from the red commit:
#   - green run          -> stop: everything older is fine.
#   - red run            -> this older commit is red too; it becomes the new suspect, keep walking.
#   - run still pending  -> wait for it to finish, then decide.
#   - no run, docs-only  -> main-guard ignores such commits; skip over it.
#   - no run, has code   -> the run has not been created yet; wait for it.
# Uses only the GitHub REST API through `gh api`.
#
# Changing this file changes the quality gate: high-risk plan, cross-review required.

set -euo pipefail

CHECK_NAME="guard"
MAX_DEPTH="${MAX_DEPTH:-30}"
POLL_SECONDS="${POLL_SECONDS:-30}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-2400}"

[[ $# -eq 1 && "$1" =~ ^[0-9a-f]{7,40}$ ]] || { echo "cách dùng: scripts/find-breaking-commit.sh <sha>" >&2; exit 1; }
RED_SHA="$1"

REPO="${GITHUB_REPOSITORY:-}"
if [[ -z "$REPO" ]]; then
  REMOTE_URL="$(git config --get remote.origin.url)"
  REPO="$(echo "$REMOTE_URL" | sed -E 's#(git@github.com:|https://github.com/)##; s#\.git$##')"
fi

# Prints success | failure | pending | none for the main-guard run on a commit.
guard_state() {
  local sha="$1" runs
  runs="$(gh api "repos/$REPO/commits/$sha/check-runs?check_name=$CHECK_NAME&per_page=20" --jq '.check_runs')"
  jq -r '
    if length == 0 then "none"
    else (sort_by(.started_at) | last) as $r
      | if $r.status != "completed" then "pending"
        elif $r.conclusion == "success" then "success"
        elif $r.conclusion == "failure" then "failure"
        else "pending" end
    end' <<<"$runs"
}

# True when every file changed by the commit is documentation, which main-guard ignores.
docs_only() {
  local sha="$1"
  local non_docs
  non_docs="$(gh api "repos/$REPO/commits/$sha" --jq '[.files[].filename | select((startswith("docs/") or endswith(".md")) | not)] | length')"
  [[ "$non_docs" == "0" ]]
}

parent_of() {
  gh api "repos/$REPO/commits/$1" --jq '.parents[0].sha // empty'
}

suspect="$RED_SHA"
current="$(parent_of "$RED_SHA")"
depth=0
while [[ -n "$current" && "$depth" -lt "$MAX_DEPTH" ]]; do
  depth=$((depth + 1))
  waited=0
  while :; do
    state="$(guard_state "$current")"
    if [[ "$state" == "none" ]] && docs_only "$current"; then
      state="skip"
    fi
    if [[ "$state" == "pending" || "$state" == "none" ]]; then
      if [[ "$waited" -ge "$MAX_WAIT_SECONDS" ]]; then
        echo "CẢNH BÁO: chờ quá lâu kết quả kiểm tra của ${current:0:7}, coi như chưa xác định được; dừng lần ngược." >&2
        echo "$suspect"
        exit 0
      fi
      sleep "$POLL_SECONDS"
      waited=$((waited + POLL_SECONDS))
      continue
    fi
    break
  done

  case "$state" in
    success) break ;;
    failure) suspect="$current" ;;
    skip) ;;
  esac
  current="$(parent_of "$current")"
done

echo "$suspect"
