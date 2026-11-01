#!/usr/bin/env bash
# Finds the commit that broke the main branch (ADR-0010).
#
# Usage:  scripts/find-breaking-commit.sh [--check guard|ui-test] <red commit sha>
# Prints: the sha of the oldest red commit after the most recent green commit on the
#         first-parent history of <red commit sha>. That is the commit to blame and revert.
# --check names the check run of main-guard.yml to read: guard (the default) or ui-test (the emulator job, ADR-0043).
#
# Each commit on main gets its own main-guard run (job "guard"). Walking back from the red commit:
#   - green run          -> stop: everything older is fine.
#   - red run            -> this older commit is red too; it becomes the new suspect, keep walking.
#   - run still pending  -> wait for it to finish, then decide.
#   - no run, docs-only  -> main-guard ignores such commits; skip over it.
#   - no run, has code   -> the run has not been created yet; wait for it.
# With --check ui-test (ADR-0043), on top of that:
#   - skipped            -> unknown (no app code, or guard was red so the emulator did not run); skip over it.
#   - red, with an annotation starting "EMULATOR HẠ TẦNG (" (verdict infra of scripts/ui-test-verdict.sh)
#                        -> unknown, skip over it: an infrastructure failure is never blamed. Annotations that cannot
#                           be read leave the commit red.
#   - any other end (cancelled, timed_out, neutral...) -> unknown, skip over it at once instead of waiting.
#   - no run, has code, and the main-guard.yml of that commit has no ui-test job -> stop as green: the commit is from
#     before ADR-0043, its emulator tests ran on the PR.
# Uses only the GitHub REST API through `gh api`.
#
# Changing this file changes the quality gate: high-risk plan, cross-review required.

set -euo pipefail

CHECK_NAME="guard"
MAX_DEPTH="${MAX_DEPTH:-30}"
POLL_SECONDS="${POLL_SECONDS:-30}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-2400}"

usage() { echo "cách dùng: scripts/find-breaking-commit.sh [--check guard|ui-test] <sha>" >&2; exit 1; }
if [[ $# -eq 3 && "$1" == "--check" ]]; then
  CHECK_NAME="$2"
  shift 2
fi
[[ "$CHECK_NAME" == "guard" || "$CHECK_NAME" == "ui-test" ]] || usage
[[ $# -eq 1 && "$1" =~ ^[0-9a-f]{7,40}$ ]] || usage
RED_SHA="$1"

REPO="${GITHUB_REPOSITORY:-}"
if [[ -z "$REPO" ]]; then
  REMOTE_URL="$(git config --get remote.origin.url)"
  REPO="$(echo "$REMOTE_URL" | sed -E 's#(git@github.com:|https://github.com/)##; s#\.git$##')"
fi

# Prints success | failure | pending | none for the main-guard run on a commit (check run guard).
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

# Prints success | failure | skip | pending | none for the check run ui-test on a commit (ADR-0043).
ui_test_state() {
  local sha="$1" runs latest infra
  runs="$(gh api "repos/$REPO/commits/$sha/check-runs?check_name=$CHECK_NAME&per_page=20" --jq '.check_runs')"
  latest="$(jq -r '
    if length == 0 then "none"
    else (sort_by(.started_at) | last) as $r
      | if $r.status != "completed" then "pending"
        elif $r.conclusion == "success" then "success"
        elif $r.conclusion == "failure" then "failure \($r.id)"
        else "skip" end
    end' <<<"$runs")"
  if [[ "$latest" == failure* ]]; then
    # The check step of an infra verdict prints "EMULATOR HẠ TẦNG (...)" as an error annotation.
    infra="$(gh api "repos/$REPO/check-runs/${latest#failure }/annotations?per_page=100" \
      --jq '[.[] | select(.message | startswith("EMULATOR HẠ TẦNG ("))] | length' 2> /dev/null || echo 0)"
    if [[ "$infra" =~ ^[1-9][0-9]*$ ]]; then echo skip; else echo failure; fi
    return 0
  fi
  echo "$latest"
}

# True when the main-guard.yml of the commit has no ui-test job: a commit from before ADR-0043. A file that cannot be
# read counts as having the job (wait, as before).
no_ui_test_job() {
  local workflow
  workflow="$(gh api -H "Accept: application/vnd.github.raw" "repos/$REPO/contents/.github/workflows/main-guard.yml?ref=$1" \
    2> /dev/null)" || return 1
  ! grep -qE '^  ui-test:$' <<<"$workflow"
}

check_state() {
  if [[ "$CHECK_NAME" == "ui-test" ]]; then ui_test_state "$1"; else guard_state "$1"; fi
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
    state="$(check_state "$current")"
    if [[ "$state" == "none" ]] && docs_only "$current"; then
      state="skip"
    elif [[ "$state" == "none" && "$CHECK_NAME" == "ui-test" ]] && no_ui_test_job "$current"; then
      state="success"
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
