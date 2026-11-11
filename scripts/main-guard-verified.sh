#!/usr/bin/env bash
# Chooses how main-guard.yml checks a commit of main (ADR-0039):
#   full   run scripts/check.sh, as before
#   tests  run only scripts/tests/run.sh: the commit differs from what pr-check checked only in documentation
#   skip   run nothing: the commit has the very tree on which pr-check already ran scripts/check.sh green
#
# Usage:  scripts/main-guard-verified.sh <sha> [<event> [<run attempt>]]   (main-guard.yml passes GITHUB_SHA,
#         GITHUB_EVENT_NAME and GITHUB_RUN_ATTEMPT)
# Prints exactly one word (full, tests or skip) on stdout and the reason on stderr. Always exits 0: anything it cannot
# read or decide gives full.
#
# Three more questions, for scripts/merge-if-green.sh, which merges a PR behind main without updating it when main
# changed only docs since the split (ADR-0041). All print yes or no on stdout, the reason on stderr, and exit 0:
#   scripts/main-guard-verified.sh --docs-only        are all paths on stdin (one per line) docs by the rule below
#                                                      that picks tests? An empty list is yes.
#   scripts/main-guard-verified.sh --guard-ignores    are all paths on stdin (one per line) in the paths-ignore of
#                                                      main-guard.yml (docs/** and **/*.md), docs files Gradle reads
#                                                      included? An empty list is yes.
#   scripts/main-guard-verified.sh --gate-ran <sha>   did the newest pr-check run on that commit end green with
#                                                      GATE_STEP of GATE_JOB success? Anything unread is no.
# "Does main-guard run on a commit at all" is --guard-ignores; "how does main-guard check that commit" is --docs-only
# and the choice below.
#
# skip and tests need every one of these, else full:
#   - the event is push and this is the first attempt of the run (a re-run is someone asking for a real check, for
#     example to close main-broken, ADR-0025 point 2);
#   - the commit changes no file the guard job runs or relies on (GATE_FILES, with scripts/ui-test-needed.sh that its
#     step ui runs, ADR-0043, and scripts/runner-check.sh that checks a self-hosted runner, ADR-0042): a changed gate
#     first runs for real here;
#   - the commit is the merge commit of a PR into main;
#   - the newest pr-check run on the PR head is green and its GATE_JOB ran the step GATE_STEP with success (a PR
#     without code only runs scripts/tests/run.sh there, L-040);
#   - the PR head can be fetched (refs/pull/<n>/head): on GitHub the squash commit does not descend from it.
# Then the trees of the PR head and of the commit are compared: same tree is skip; only docs/** and *.md files differ,
# none of them a docs file Gradle reads (GRADLE_READ_DOCS), is tests; anything else is full.
# Both diffs list the old and the new path of a renamed file (--no-renames): code moved into docs/ is still code.
# The trees match, or differ only in docs that main got since the split: merge-if-green.sh merges a PR that is behind
# main only when main changed nothing but docs since then (ADR-0041), pinned to the checked sha.
# Uses only the GitHub REST API through `gh api`, and git.
#
# Changing this file changes the quality gate: high-risk plan, cross-review required.

set -euo pipefail

GATE_FILES=(.github/workflows/pr-check.yml .github/workflows/main-guard.yml scripts/check.sh scripts/failure-kind.sh
  scripts/find-breaking-commit.sh scripts/main-guard-verified.sh scripts/tests/run.sh scripts/tests/fake-gh
  scripts/ui-test-needed.sh scripts/runner-check.sh)
GATE_JOB="build-and-test"
GATE_STEP="Cổng chất lượng (scripts/check.sh)"
# The docs files that a Gradle test reads (rootProject.file("docs/...") in a *.gradle.kts); scripts/tests/run.sh keeps
# this list in step with the build files, but only for that form: a build file reading docs/ another way must be added here.
GRADLE_READ_DOCS=(docs/remote-config-keys.md docs/remote-config-template.json docs/design-system/tokens.json)
PULLS_TRIES="${PULLS_TRIES:-3}"
PULLS_WAIT_SECONDS="${PULLS_WAIT_SECONDS:-5}"

full() { # <reason>
  echo "KIỂM ĐẦY ĐỦ: $1." >&2
  echo full
  exit 0
}

# Paths on stdin, one per line (empty lines skipped): prints nothing when every path is under docs/ or ends in .md and
# none is a docs file Gradle reads, else the first reason.
docs_only_reason() {
  local file read_doc
  while IFS= read -r file; do
    [[ -n "$file" ]] || continue
    if [[ "$file" != docs/* && "$file" != *.md ]]; then
      echo "$file"
      return 0
    fi
    for read_doc in "${GRADLE_READ_DOCS[@]}"; do
      if [[ "$file" == "$read_doc" ]]; then
        echo "$file, file mà test Gradle đọc"
        return 0
      fi
    done
  done
  return 0
}

# Paths on stdin, one per line (empty lines skipped): prints nothing when every path is in the paths-ignore of
# main-guard.yml, that is under docs/ or ending in .md (bash's * also matches /, so *.md is **/*.md, README.md too),
# else the first path that is not.
ignored_reason() {
  local file
  while IFS= read -r file; do
    [[ -n "$file" ]] || continue
    if [[ "$file" != docs/* && "$file" != *.md ]]; then
      echo "$file"
      return 0
    fi
  done
  return 0
}

# <head sha> <name of the PR>: prints nothing when the newest pr-check run on that commit is green and its GATE_JOB
# ran the step GATE_STEP with success, else the reason.
gate_reason() {
  local head="$1" name="$2" run run_id conclusion step
  if ! run="$(gh api "repos/$REPO/actions/workflows/pr-check.yml/runs?head_sha=$head&per_page=10" \
    --jq '.workflow_runs | sort_by(.created_at) | last | if . == null then "" else "\(.id) \(.conclusion)" end' 2>/dev/null)"; then
    echo "không đọc được lần chạy pr-check của $name"
    return 0
  fi
  read -r run_id conclusion <<<"$run"
  if [[ -z "${run_id:-}" ]]; then
    echo "$name không có lần chạy pr-check trên ${head:0:7}"
    return 0
  fi
  if [[ "$conclusion" != "success" ]]; then
    echo "lần chạy pr-check mới nhất của $name kết thúc '${conclusion:-chưa xong}'"
    return 0
  fi
  if ! step="$(gh api "repos/$REPO/actions/runs/$run_id/jobs?per_page=50" \
    --jq "[.jobs[] | select(.name == \"$GATE_JOB\") | .steps[] | select(.name == \"$GATE_STEP\") | .conclusion][0] // \"\"" \
    2>/dev/null)"; then
    echo "không đọc được các bước của pr-check #$run_id"
    return 0
  fi
  [[ "$step" == "success" ]] || echo "$name chưa chạy check.sh ở pr-check (bước '$GATE_STEP': '${step:-không có}')"
  return 0
}

REPO="${GITHUB_REPOSITORY:-}"
if [[ -z "$REPO" ]]; then
  REPO="$(git config --get remote.origin.url 2>/dev/null | sed -E 's#(git@github.com:|https://github.com/)##; s#\.git$##')" || REPO=""
fi

# The three questions of merge-if-green.sh (ADR-0041): yes or no, never full, always exit 0.
if [[ "${1:-}" == "--docs-only" ]]; then
  if [[ $# -ne 1 ]]; then
    echo "cách dùng: scripts/main-guard-verified.sh --docs-only (đường dẫn ở stdin)" >&2
    echo no
    exit 0
  fi
  reason="$(docs_only_reason)"
  if [[ -n "$reason" ]]; then
    echo "$reason" >&2
    echo no
  else
    echo yes
  fi
  exit 0
fi
if [[ "${1:-}" == "--guard-ignores" ]]; then
  if [[ $# -ne 1 ]]; then
    echo "cách dùng: scripts/main-guard-verified.sh --guard-ignores (đường dẫn ở stdin)" >&2
    echo no
    exit 0
  fi
  reason="$(ignored_reason)"
  if [[ -n "$reason" ]]; then
    echo "$reason, file mà main-guard không bỏ qua" >&2
    echo no
  else
    echo yes
  fi
  exit 0
fi
if [[ "${1:-}" == "--gate-ran" ]]; then
  if [[ $# -ne 2 || ! "${2:-}" =~ ^[0-9a-f]{7,40}$ ]]; then
    echo "cách dùng: scripts/main-guard-verified.sh --gate-ran <sha>" >&2
    echo no
    exit 0
  fi
  if [[ -z "$REPO" ]]; then
    echo "không biết repo (GITHUB_REPOSITORY, remote origin)" >&2
    echo no
    exit 0
  fi
  reason="$(gate_reason "$2" "commit ${2:0:7}")"
  if [[ -n "$reason" ]]; then
    echo "$reason" >&2
    echo no
  else
    echo yes
  fi
  exit 0
fi

[[ $# -ge 1 && $# -le 3 && "$1" =~ ^[0-9a-f]{7,40}$ ]] || full "cách dùng: scripts/main-guard-verified.sh <sha> [<sự kiện> [<lần chạy thứ>]]"
SHA="$1"
EVENT="${2:-push}"
ATTEMPT="${3:-1}"

[[ -n "$REPO" ]] || full "không biết repo (GITHUB_REPOSITORY, remote origin)"

[[ "$EVENT" == "push" ]] || full "chạy bằng sự kiện $EVENT, không phải push"
[[ "$ATTEMPT" == "1" ]] || full "lần chạy lại thứ $ATTEMPT"

changed="$(git -c core.quotePath=false diff --no-renames --name-only "$SHA^" "$SHA" 2>/dev/null)" || full "không đọc được thay đổi của ${SHA:0:7} so với commit cha"
for gate in "${GATE_FILES[@]}"; do
  if grep -qxF -- "$gate" <<<"$changed"; then
    full "commit đổi $gate, một file mà job guard chạy hay dựa vào"
  fi
done

# GitHub may link the commit to its PR a few seconds after the push.
pr=""
for ((try = 1; try <= PULLS_TRIES; try++)); do
  pr="$(gh api "repos/$REPO/commits/$SHA/pulls" --jq "[.[] | select(.merged_at != null and .base.ref == \"main\"
    and .merge_commit_sha == \"$SHA\")][0] | if . == null then \"\" else \"\(.number) \(.head.sha)\" end" 2>/dev/null)" ||
    full "không đọc được PR của ${SHA:0:7} từ GitHub"
  [[ -n "$pr" ]] && break
  [[ "$try" -lt "$PULLS_TRIES" ]] && sleep "$PULLS_WAIT_SECONDS"
done
[[ -n "$pr" ]] || full "${SHA:0:7} không phải commit merge của một PR vào main"
read -r number head <<<"$pr"
[[ "$number" =~ ^[0-9]+$ && "$head" =~ ^[0-9a-f]{40}$ ]] || full "GitHub trả PR không đọc được"

reason="$(gate_reason "$head" "PR #$number")"
[[ -z "$reason" ]] || full "$reason"

if ! git cat-file -e "$head^{commit}" 2>/dev/null; then
  git fetch -q --no-tags --depth=1 origin "refs/pull/$number/head" 2>/dev/null ||
    full "không lấy được refs/pull/$number/head"
  [[ "$(git rev-parse FETCH_HEAD 2>/dev/null)" == "$head" ]] ||
    full "refs/pull/$number/head không trỏ commit đã kiểm ${head:0:7}"
fi

diff="$(git -c core.quotePath=false diff --no-renames --name-only "$head" "$SHA" 2>/dev/null)" || full "không so được ${head:0:7} với ${SHA:0:7}"
if [[ -z "$diff" ]]; then
  echo "ĐÃ KIỂM Ở PR #$number (commit ${head:0:7}): cây file giống hệt, không chạy lại check.sh (ADR-0039)." >&2
  echo skip
  exit 0
fi
reason="$(docs_only_reason <<<"$diff")"
[[ -z "$reason" ]] || full "khác commit đã kiểm của PR #$number ở $reason"
echo "CHỈ KHÁC TÀI LIỆU so với PR #$number: chỉ chạy scripts/tests/run.sh (ADR-0039)." >&2
echo tests
