#!/usr/bin/env bash
# Merges a PR into the main branch, only when it passes the whole quality gate (ADR-0010, ADR-0025).
# This is the ONLY allowed way to merge into main, for people and AI alike.
#
# Usage:       scripts/merge-if-green.sh <PR number> [--dry-run | --update]
#              --dry-run: check the conditions only, do not merge.
#              --update:  when the branch is behind main, have GitHub update it (merge main into the branch),
#                         wait for CI on the new commit, then merge; also waits for checks still running.
#                         Waits at most MERGE_UPDATE_TIMEOUT_SECONDS (default 1800).
# Exit codes:  0 merged (with --dry-run: ready to merge); also "GitHub answered the merge with an error but the
#                commit is on main" and "the PR is already on main" (L-080): both are reported, nothing is left to merge
#              1 refused (a condition is not met, see the message)
#              2 checks still running (with --update: still running when the wait ran out), call again later,
#                including a branch behind main by docs only whose CI has not finished, and a skipped check left from
#                a draft
#              3 branch is behind main: update it, wait for CI, then call again (or call with --update)
#
# Every condition is checked again after each wait, so a PR that turns red, becomes a draft or gets a label,
# or a main branch that breaks while waiting, is still refused. The merge is pinned to the checked commit.
#
# A branch behind main is merged without updating (ADR-0041) when, since the split, main changed only docs by the rule
# of main-guard (scripts/main-guard-verified.sh --docs-only), no file changed on both sides, GitHub says the PR does
# not conflict, and pr-check ran scripts/check.sh on the PR head (--gate-ran). The merge commit then differs from the
# checked commit only in main's docs, and main-guard checks it with scripts/tests/run.sh. A PR with docs only, or any
# answer that cannot be read, updates the branch as before. So does a PR whose files are all in the paths-ignore of
# main-guard.yml (--guard-ignores), even a docs file Gradle reads: main-guard would not run on its merge commit at all.
#
# A cancelled check run with a newer run of the same check (same name and app) on the same commit is no result: the
# concurrency of the workflow cancels a duplicate run (L-164). The newest run still counts, cancelled or not.
#
# Uses only the GitHub REST API through `gh api`.
# Changing this file changes the quality gate: high-risk plan, cross-review required.
# Messages are in Vietnamese for the team; comments are in English.

set -euo pipefail

BASE_BRANCH="main"
# Checks that must be green. Names match the job names in .github/workflows/pr-check.yml. The emulator tests run after
# the merge, in the ui-test job of main-guard.yml (ADR-0043): a PR has no ui-test to wait for, but a red one left from
# before is still refused below like any red check.
# pr-check has one job (T-076): the path filter is a step of build-and-test.
REQUIRED_CHECKS=("build-and-test")
# Waiting with --update; tests shorten both.
UPDATE_TIMEOUT_SECONDS="${MERGE_UPDATE_TIMEOUT_SECONDS:-1800}"
POLL_SECONDS="${MERGE_POLL_SECONDS:-30}"
# Needing more updates than this means main keeps moving; a person should look.
MAX_UPDATES=3
# A head commit younger than this gets a warning: someone may still be pushing to the branch (app lesson L-018).
RECENT_MINUTES="${MERGE_RECENT_MINUTES:-10}"
# The rule of main-guard for "docs only" and "the PR ran check.sh" (ADR-0041). Tests point it at a broken copy.
MAIN_GUARD_VERIFIED="${MAIN_GUARD_VERIFIED_SCRIPT:-$(dirname "$0")/main-guard-verified.sh}"
# The compare API lists at most 300 files: a list that long may be cut, so it is not trusted.
COMPARE_FILES_LIMIT=300
# GitHub computes "mergeable" in the background (null until then): read again a few times, then wait.
MERGEABLE_TRIES="${MERGE_MERGEABLE_TRIES:-3}"
MERGEABLE_WAIT_SECONDS="${MERGE_MERGEABLE_WAIT_SECONDS:-5}"
# After a failed delete of the merged branch, how many times it is read again, and how long apart (L-149).
BRANCH_GONE_TRIES="${MERGE_BRANCH_TRIES:-3}"
BRANCH_GONE_WAIT_SECONDS="${MERGE_BRANCH_WAIT_SECONDS:-5}"

die()  { echo "TỪ CHỐI: $*" >&2; exit 1; }
# A condition not met yet: recorded for the main loop, which waits (--update) or stops with exit 2.
pending() { RESULT="pending"; MESSAGE="$*"; }

# The squash commit of this very PR on main: first line "<title> (#<PR>)" and the file tree of the PR head (L-080),
# or, after a merge without update (ADR-0041), that tree plus docs main got since the split (docs_only_merge).
# GitHub once answered a merge with an error although the commit reached main. Both reads are best effort: a read
# that fails counts as "not seen", and the script goes on as before. Prints the commit sha, or nothing, exit 0.
# A commit with the same title that cannot be shown to be this PR: prints its sha and returns 3; the caller refuses.
# Called as `if ! x="$(merged_on_main)"`, which turns errexit off here: every command checks its own status.
merged_on_main() {
  local tree same latest
  tree="$(gh api "repos/$REPO/git/commits/$HEAD_SHA" --jq .tree.sha 2>/dev/null || true)"
  [[ -n "$tree" ]] || return 0
  same="$( { gh api "repos/$REPO/commits?sha=$BASE_BRANCH&per_page=30" 2>/dev/null || true; } \
    | jq -c --arg title "$TITLE (#$PR)" \
        '[.[]? | select((.commit.message | split("\n")[0]) == $title) | {sha, tree: .commit.tree.sha}]' 2>/dev/null \
    || true)"
  [[ -n "$same" ]] || return 0
  if jq -e --arg tree "$tree" 'any(.[]; .tree == $tree)' <<<"$same" > /dev/null 2>&1; then
    jq -r --arg tree "$tree" '[.[] | select(.tree == $tree)][0].sha' <<<"$same"
    return 0
  fi
  latest="$(jq -r '.[0].sha // empty' <<<"$same" 2>/dev/null || true)"
  [[ -n "$latest" ]] || return 0
  echo "$latest"
  if docs_only_merge "$latest"; then
    return 0
  fi
  return 3
}

# <commit C on main with the PR's title>: true when C is this PR merged without update: C is on main, and the trees of
# the PR head and of C differ only in docs (by the rule of main-guard) that main changed between the merge base and
# C's parent, none of them changed by the PR too. The PR head comes by its pull ref, or by its branch name when a
# proxy (cloud session) refuses that ref, like the main-broken check below. Anything unread is false.
docs_only_merge() {
  local c="$1" base diff main_changed pr_changed answer file
  git fetch -q origin "+refs/pull/$PR/head:refs/merge-if-green/pr" "+refs/heads/$BASE_BRANCH:refs/merge-if-green/main" > /dev/null 2>&1 \
    || git fetch -q origin "+refs/heads/$HEAD_REF:refs/merge-if-green/pr" "+refs/heads/$BASE_BRANCH:refs/merge-if-green/main" > /dev/null 2>&1 \
    || return 1
  [[ "$(git rev-parse refs/merge-if-green/pr 2>/dev/null)" == "$HEAD_SHA" ]] || return 1
  git merge-base --is-ancestor "$c" refs/merge-if-green/main > /dev/null 2>&1 || return 1
  base="$(git merge-base "$HEAD_SHA" "$c^1" 2>/dev/null)" || return 1
  [[ -n "$base" ]] || return 1
  diff="$(git -c core.quotePath=false diff --no-renames --name-only "$HEAD_SHA" "$c" 2>/dev/null)" || return 1
  answer="$("$MAIN_GUARD_VERIFIED" --docs-only <<<"$diff" 2>/dev/null)" || return 1
  [[ "$answer" == "yes" ]] || return 1
  main_changed="$(git -c core.quotePath=false diff --no-renames --name-only "$base" "$c^1" 2>/dev/null)" || return 1
  pr_changed="$(git -c core.quotePath=false diff --no-renames --name-only "$base" "$HEAD_SHA" 2>/dev/null)" || return 1
  while IFS= read -r file; do
    [[ -n "$file" ]] || continue
    grep -qxF -- "$file" <<<"$main_changed" || return 1
    if grep -qxF -- "$file" <<<"$pr_changed"; then return 1; fi
  done <<<"$diff"
  return 0
}

# <sha> [after-merge]: refuses because a commit on main has the PR's title but cannot be shown to be this PR. Never
# merges again; a person looks. After a merge GitHub answered with an error, that commit may be the merge itself and
# main-guard may not have run on it, so main-guard is started by hand first (a run by hand is full, ADR-0039).
refuse_same_title() {
  local message since
  message="có commit ${1:0:7} cùng tiêu đề '$TITLE (#$PR)' trên nhánh chính nhưng cây khác commit của PR ngoài tài liệu; không merge lại, người xem."
  if [[ "${2:-}" == "after-merge" ]]; then
    since="$(date -u +%Y-%m-%dT%H:%M:00Z)"
    if gh api -X POST "repos/$REPO/actions/workflows/main-guard.yml/dispatches" -f ref="$BASE_BRANCH" > /dev/null 2>&1; then
      message="$message Đã yêu cầu chạy main-guard; chờ: scripts/wait-run.sh --workflow main-guard.yml --since $since."
    else
      message="$message Không yêu cầu được main-guard; chạy tay trên tab Actions."
    fi
  fi
  die "$message"
}

# <args>...: runs main-guard-verified.sh, stdin passed on; MGV_ANSWER is its stdout ("" when it cannot run), MGV_REASON
# its stderr on one line.
mgv_ask() {
  local err
  err="$(mktemp)"
  MGV_ANSWER="$("$MAIN_GUARD_VERIFIED" "$@" 2> "$err")" || MGV_ANSWER=""
  MGV_REASON="$(tr '\n' ' ' < "$err" | sed 's/ *$//')"
  rm -f "$err"
}

# A branch behind main (ADR-0041). Sets DOCS_ONLY_BEHIND to yes when, since the split, main changed only docs, no file
# changed on both sides and GitHub says the PR does not conflict; else no, with the first reason in BEHIND_REASON.
# "mergeable" still unknown after a few reads: pending. The check of mergeable comes last, so a fixture without the
# compare of the PR head with main never reads the PR again.
docs_only_behind() { # <JSON of the PR>
  local compare main_files pr_files_list file mergeable try
  DOCS_ONLY_BEHIND="no"
  BEHIND_REASON=""
  if ! compare="$(gh api "repos/$REPO/compare/$HEAD_SHA...$BASE_BRANCH" 2>/dev/null)"; then
    BEHIND_REASON="không đọc được các file nhánh chính đổi từ điểm tách"
    return 0
  fi
  if ! main_files="$(jq -r --argjson limit "$COMPARE_FILES_LIMIT" \
    'if (.files | type) == "array" and (.files | length) < $limit then .files[] | .filename, (.previous_filename // empty)
     else error("files") end' <<<"$compare" 2>/dev/null)"; then
    BEHIND_REASON="GitHub không trả đủ danh sách file nhánh chính đổi từ điểm tách"
    return 0
  fi
  mgv_ask --docs-only <<<"$main_files"
  if [[ "$MGV_ANSWER" != "yes" ]]; then
    BEHIND_REASON="nhánh chính từ điểm tách đổi ${MGV_REASON:-file ngoài tài liệu}"
    return 0
  fi
  if ! pr_files_list="$(gh_all "repos/$REPO/pulls/$PR/files?per_page=100" 2>/dev/null \
    | jq -r '.filename, (.previous_filename // empty)' 2>/dev/null)"; then
    BEHIND_REASON="không đọc được danh sách file của PR"
    return 0
  fi
  # pr-check's code filter also takes *.md under app/, core/, feature/ and scripts/, so such a PR ran check.sh, but
  # its merge commit has only such files and main-guard does not run at all: it must be updated. This is the rule of
  # paths-ignore (--guard-ignores), not the rule by which main-guard picks tests (--docs-only): a docs file Gradle
  # reads is code for the latter but still skipped by paths-ignore (L-152). Only "no" goes on; any other answer
  # (empty, full from an older main-guard-verified.sh) updates, the safe side.
  mgv_ask --guard-ignores <<<"$pr_files_list"
  if [[ "$MGV_ANSWER" == "yes" ]]; then
    BEHIND_REASON="PR chỉ đổi file mà main-guard bỏ qua"
    return 0
  fi
  if [[ "$MGV_ANSWER" != "no" ]]; then
    BEHIND_REASON="không đọc được PR có chỉ đổi file mà main-guard bỏ qua không"
    return 0
  fi
  while IFS= read -r file; do
    [[ -n "$file" ]] || continue
    if grep -qxF -- "$file" <<<"$pr_files_list"; then
      BEHIND_REASON="cả PR và nhánh chính cùng đổi $file"
      return 0
    fi
  done <<<"$main_files"
  # The re-reads only look at mergeable, not at the head: the merge is pinned to the sha of the first read.
  mergeable="$(jq -r '.mergeable | if . == null then "null" else tostring end' <<<"$1")"
  try=1
  while [[ "$mergeable" == "null" && "$try" -le "$MERGEABLE_TRIES" ]]; do
    sleep "$MERGEABLE_WAIT_SECONDS"
    mergeable="$(gh api "repos/$REPO/pulls/$PR" --jq '.mergeable | if . == null then "null" else tostring end' 2>/dev/null \
      || echo null)"
    try=$((try + 1))
  done
  case "$mergeable" in
    true) DOCS_ONLY_BEHIND="yes" ;;
    false) BEHIND_REASON="PR xung đột với nhánh chính" ;;
    null) pending "GitHub chưa tính xong PR có xung đột không (nhánh '$HEAD_REF' chậm hơn '$BASE_BRANCH', nhánh chính chỉ đổi tài liệu)." ;;
    *) BEHIND_REASON="GitHub trả trạng thái xung đột lạ '$mergeable'" ;;
  esac
  return 0
}

# Every item of a list endpoint, one compact JSON per line. Pages are read with "&page=N" on the repos/<owner>/<repo>
# path: `gh api --paginate` follows GitHub's Link header, which names the repository by numeric id, and the proxy of
# a cloud session refuses such paths (L-092). The path must already end in "per_page=100".
gh_all() {
  local page=1 body
  while (( page <= 50 )); do
    body="$(gh api "$1&page=$page")" || return 1
    jq -c '.[]' <<<"$body"
    (( $(jq length <<<"$body") >= 100 )) || return 0
    page=$((page + 1))
  done
}

# Strict argument check: any unknown argument (including a mistyped --dry-run) is refused,
# so a real merge never happens when the caller only wanted a dry run.
DRY_RUN="no"
UPDATE="no"
case "$#" in
  1) ;;
  2)
    case "$2" in
      --dry-run) DRY_RUN="yes" ;;
      --update) UPDATE="yes" ;;
      *) die "tham số thứ hai không hợp lệ: '$2'. Chỉ chấp nhận --dry-run hoặc --update." ;;
    esac ;;
  *) die "cách dùng: scripts/merge-if-green.sh <số PR> [--dry-run | --update]" ;;
esac
[[ "$1" =~ ^[0-9]+$ ]] || die "tham số đầu tiên phải là số PR, nhận được '$1'."
PR="$1"

command -v gh >/dev/null || die "không tìm thấy gh."
command -v jq >/dev/null || die "không tìm thấy jq."

# owner/repo from the origin remote.
REMOTE_URL="$(git config --get remote.origin.url)"
REPO="$(echo "$REMOTE_URL" | sed -E 's#(git@github.com:|https://github.com/)##; s#\.git$##')"
[[ "$REPO" =~ ^[^/]+/[^/]+$ ]] || die "không xác định được repo từ remote: $REMOTE_URL"

# Result of one check on HEAD_SHA. A check whose job still shows queued or in_progress while its workflow run
# has completed with success counts as green: GitHub sometimes leaves the job status behind (app lesson L-017).
check_one() {
  local name="$1" latest status conclusion suite run
  # A run not started yet (started_at null) is the newest.
  latest="$(jq -c --arg n "$name" '[.[] | select(.name==$n)] | sort_by([.started_at == null, .started_at]) | last // empty' <<<"$RUNS")"
  if [[ -z "$latest" ]]; then
    pending "chưa thấy kiểm tra '$name' trên commit ${HEAD_SHA:0:7}: CI chưa chạy trên commit này."
    return 0
  fi
  status="$(jq -r .status <<<"$latest")"
  conclusion="$(jq -r .conclusion <<<"$latest")"
  if [[ "$status" != "completed" ]]; then
    suite="$(jq -r '.check_suite.id // empty' <<<"$latest")"
    run=""
    if [[ "$suite" =~ ^[0-9]+$ ]]; then
      run="$( { gh api "repos/$REPO/actions/runs?head_sha=$HEAD_SHA&per_page=50" 2>/dev/null || true; } \
        | jq -r --argjson suite "$suite" \
          '[.workflow_runs[]? | select(.check_suite_id == $suite)] | first // empty | "\(.status) \(.conclusion) \(.id)"' \
          2>/dev/null || true)"
    fi
    if [[ "$run" == "completed success "* ]]; then
      echo "CHÚ Ý: kiểm tra '$name' còn ghi '$status' nhưng lần chạy ${run##* } chứa nó đã xong và xanh; coi là xanh." >&2
      return 0
    fi
    pending "kiểm tra '$name' trên commit ${HEAD_SHA:0:7} đang ở trạng thái $status: CI chưa chạy xong trên commit này."
    return 0
  fi
  if [[ "$conclusion" == "success" ]]; then return 0; fi
  # build-and-test is skipped only for drafts (ADR-0041): on a PR that is no draft, the run of ready_for_review has
  # not come yet. Never green.
  if [[ "$conclusion" == "skipped" ]]; then
    pending "kiểm tra '$name' trên commit ${HEAD_SHA:0:7} bị bỏ qua (lần chạy lúc PR còn nháp); chờ pr-check chạy sau khi PR chuyển sang sẵn sàng (ADR-0041)."
    return 0
  fi
  die "kiểm tra '$name' kết thúc với kết quả '$conclusion'."
}

# Checks every condition once. Sets RESULT to ready, pending or behind (MESSAGE says why); refusals exit at once.
evaluate() {
  RESULT="ready"
  MESSAGE=""
  NO_UPDATE_NOTE=""
  DOCS_ONLY_BEHIND="no"
  local pr_json state draft base already broken broken_json refuse_broken culprit revert_check run_branches run_branch run_id run_text run_status task_re task behind_by failed c

  pr_json="$(gh api "repos/$REPO/pulls/$PR")"
  state="$(jq -r .state <<<"$pr_json")"
  draft="$(jq -r .draft <<<"$pr_json")"
  base="$(jq -r .base.ref <<<"$pr_json")"
  HEAD_SHA="$(jq -r .head.sha <<<"$pr_json")"
  HEAD_REF="$(jq -r .head.ref <<<"$pr_json")"
  TITLE="$(jq -r .title <<<"$pr_json")"

  [[ "$state" == "open" ]] || die "PR #$PR không ở trạng thái mở ($state)."
  # Already on main (an earlier merge that GitHub answered with an error): report it, never merge twice. This comes
  # before every other condition, since none of them matters for a PR that has nothing left to merge.
  if ! already="$(merged_on_main)"; then
    refuse_same_title "$already"
  fi
  if [[ -n "$already" ]]; then
    echo "ĐÃ MERGE TRƯỚC ĐÓ: PR #$PR đã nằm trên nhánh chính ở commit ${already:0:7} (GitHub chưa đóng PR). Không merge lần hai; đóng PR trên web."
    exit 0
  fi
  [[ "$draft" == "false" ]] || die "PR #$PR đang là bản nháp."
  # A PR that an auto-run session prepared for a person (docs/auto-run.md, section 3, "Khi dừng") carries the
  # needs-human label. Only a person removes it, after reviewing the PR; until then nobody merges it.
  if jq -e '[(.labels // [])[].name] | index("needs-human")' <<<"$pr_json" > /dev/null; then
    die "PR #$PR mang nhãn needs-human: phiên tự chạy soạn để người xem. Người xem xong, gỡ nhãn rồi mới merge."
  fi
  [[ "$base" == "$BASE_BRANCH" ]] || die "PR #$PR nhắm vào '$base', không phải '$BASE_BRANCH'."

  # While main is broken (an open issue labelled main-broken, created by main-guard.yml), nothing is merged
  # (ADR-0010), with one exception (ADR-0025, point 3): a PR that is exactly the revert of the blamed commit.
  # The content is checked, not the branch name: one commit right on top of the latest main whose file tree is
  # what `git revert <culprit>` gives there (scripts/is-exact-revert.sh). Every other condition below still applies.
  # IS_EXACT_REVERT replaces that script in the tests only.
  broken_json="$(gh api "repos/$REPO/issues?state=open&labels=main-broken&per_page=10" --jq '[.[] | {number, body}]')"
  broken="$(jq -r '[.[].number] | join(",")' <<<"$broken_json")"
  EXACT_REVERT="no"
  if [[ -n "$broken" ]]; then
    refuse_broken="nhánh chính đang đỏ (mục mang nhãn main-broken: #$broken). Người phụ trách xử lý và đóng mục đó rồi mới merge tiếp."
    [[ "$(jq length <<<"$broken_json")" == "1" ]] || die "$refuse_broken"
    culprit="$(jq -r '.[0].body // "" | capture("<!-- culprit:(?<s>[0-9a-f]+) -->").s // ""' <<<"$broken_json")"
    [[ -n "$culprit" ]] || die "$refuse_broken Mục #$broken không ghi commit gây lỗi."
    # The PR head by its pull ref; if a proxy (cloud session) refuses that ref, by its branch name.
    git fetch -q origin "+refs/pull/$PR/head:refs/merge-if-green/pr" "+refs/heads/$BASE_BRANCH:refs/merge-if-green/main" 2>/dev/null \
      || git fetch -q origin "+refs/heads/$HEAD_REF:refs/merge-if-green/pr" "+refs/heads/$BASE_BRANCH:refs/merge-if-green/main" 2>/dev/null \
      || die "$refuse_broken Không lấy được commit của PR #$PR để kiểm có phải bản revert không."
    [[ "$(git rev-parse refs/merge-if-green/pr)" == "$HEAD_SHA" ]] \
      || die "$refuse_broken Commit lấy về của PR #$PR khác ${HEAD_SHA:0:7}; gọi lại sau."
    if ! revert_check="$("${IS_EXACT_REVERT:-$(dirname "$0")/is-exact-revert.sh}" "$culprit" "$HEAD_SHA" refs/merge-if-green/main)"; then
      die "$refuse_broken $revert_check"
    fi
    echo "BẢN REVERT ĐÚNG: PR #$PR là bản revert của ${culprit:0:7}; vẫn kiểm mọi điều kiện còn lại." >&2
    EXACT_REVERT="yes"
  fi

  # While an auto-run session runs, main must not move: only that session's own PRs are merged (docs/auto-run.md,
  # section 1): its report branch, the task branch claude/T-NNN-... and the fix branch claude/fix-T-NNN-... of a
  # ticket in the table of its run list, plus the exact revert checked above, which the session merges to bring main
  # back (ADR-0025, point 4). A session is running when its report branch holds its run file with status "running";
  # with two running sessions a PR must belong to both. Stale report branches of finished sessions do not block
  # anything. The table row and the branch name are read with the patterns of scripts/resume-check.py and
  # scripts/run-watchdog.py (^\|\s*\d+\s*\|\s*(T-\d+)\s*\| and ^claude/(?:fix-)?(T-\d+)-): change all three together.
  task_re='^claude/(fix-)?(T-[0-9]+)-'
  run_branches="$(gh_all "repos/$REPO/branches?per_page=100" \
    | jq -r '.name | select(test("^claude/RUN-[0-9]{8}-[0-9]{2}-report$"))')"
  for run_branch in $run_branches; do
    run_id="${run_branch#claude/}"
    run_id="${run_id%-report}"
    run_text="$(gh api "repos/$REPO/contents/docs/runs/$run_id.md?ref=$run_branch" \
      -H "Accept: application/vnd.github.raw" 2>/dev/null || true)"
    run_status="$(sed -n '/^---$/,/^---$/s/^status:[[:space:]]*\([a-z]*\).*/\1/p' <<<"$run_text" | head -n 1)"
    [[ "$run_status" == "running" ]] || continue
    [[ "$HEAD_REF" != "$run_branch" && "$EXACT_REVERT" != "yes" ]] || continue
    if [[ "$HEAD_REF" =~ $task_re ]]; then
      task="${BASH_REMATCH[2]}"
      # task is "T-" and digits only, so it needs no escaping in the pattern.
      grep -qE -- "^\|[[:space:]]*[0-9]+[[:space:]]*\|[[:space:]]*$task[[:space:]]*\|" <<<"$run_text" && continue
      die "phiên $run_id đang chạy; task $task (nhánh '$HEAD_REF') không có trong bảng danh sách chạy docs/runs/$run_id.md trên nhánh $run_branch. Trong lúc phiên chạy chỉ merge PR của chính phiên đó (docs/auto-run.md, mục 1). Chờ phiên kết thúc rồi gọi lại; việc gấp thì người dừng phiên trước (docs/auto-run.md, mục 4, Tiếp tục phiên sau khi hết hạn mức)."
    fi
    die "phiên $run_id đang chạy. Trong lúc phiên chạy chỉ merge PR của chính phiên đó (docs/auto-run.md, mục 1). Chờ phiên kết thúc rồi gọi lại."
  done

  # The PR branch must contain the latest commit of main, unless main changed only docs since the split (ADR-0041).
  behind_by="$(gh api "repos/$REPO/compare/$BASE_BRANCH...$HEAD_SHA" --jq .behind_by)"
  if [[ "$behind_by" != "0" ]]; then
    docs_only_behind "$pr_json"
    [[ "$RESULT" != "pending" ]] || return 0
    if [[ "$DOCS_ONLY_BEHIND" != "yes" ]]; then
      RESULT="behind"
      MESSAGE="nhánh '$HEAD_REF' chậm $behind_by commit so với '$BASE_BRANCH' ($BEHIND_REASON)."
      return 0
    fi
  fi

  # Check results on the exact head commit of the PR.
  RUNS="$(gh api "repos/$REPO/commits/$HEAD_SHA/check-runs?per_page=100" --jq '.check_runs')"

  # No failed check is accepted, including checks not in the lists above. A cancelled run is skipped only when the
  # same check (same app and name) has another run that started later, or has not started yet: check_one then looks
  # at that newest run. Any other red result refuses even with a newer green run. The index i keeps a run from
  # matching itself.
  failed_filter='
    def key: "\(.app.id // "")/\(.name)";
    def later($a; $b): ($b.started_at == null and $a.started_at != null)
      or ($a.started_at != null and $b.started_at != null and $b.started_at > $a.started_at);
    [to_entries[] | .value + {i: .key}] as $all
    | [ $all[]
        | select(.status == "completed" and (.conclusion | IN("failure","cancelled","timed_out","action_required","startup_failure")))
        | . as $c
        | select($c.conclusion != "cancelled"
            or ([$all[] | select(.i != $c.i and key == ($c | key) and later($c; .))] | length) == 0)
        | .name ]
    | unique | join(", ")
  ' # end of failed_filter
  failed="$(jq -r "$failed_filter" <<<"$RUNS")"
  [[ -z "$failed" ]] || die "có kiểm tra không qua: $failed."

  for c in "${REQUIRED_CHECKS[@]}"; do
    check_one "$c"
    [[ "$RESULT" == "ready" ]] || return 0
  done

  # Behind by docs only, every check green: merge without updating only when pr-check ran check.sh on this head. A PR
  # with docs only skips that step and is always updated.
  if [[ "$DOCS_ONLY_BEHIND" == "yes" ]]; then
    if [[ "$(GITHUB_REPOSITORY="$REPO" "$MAIN_GUARD_VERIFIED" --gate-ran "$HEAD_SHA" 2>/dev/null || true)" == "yes" ]]; then
      NO_UPDATE_NOTE="KHÔNG CẬP NHẬT NHÁNH: nhánh '$HEAD_REF' chậm $behind_by commit, nhưng từ điểm tách nhánh chính chỉ đổi tài liệu và PR đã chạy check.sh; merge không cập nhật, main-guard chạy scripts/tests/run.sh sau merge (ADR-0041)."
    else
      RESULT="behind"
      MESSAGE="nhánh '$HEAD_REF' chậm $behind_by commit so với '$BASE_BRANCH' (PR không chạy check.sh ở pr-check: PR chỉ tài liệu luôn cập nhật nhánh, ADR-0041)."
    fi
  fi
}

# Runs once without --update. With --update, updates the branch when it is behind and waits for CI, within the limit.
deadline=$(( $(date +%s) + UPDATE_TIMEOUT_SECONDS ))
updates=0
updated_from=""
while true; do
  evaluate
  if [[ "$RESULT" == "behind" && -n "$updated_from" && "$HEAD_SHA" == "$updated_from" ]]; then
    pending "GitHub chưa cập nhật xong nhánh '$HEAD_REF'."
  fi
  case "$RESULT" in
    ready) break ;;
    behind)
      if [[ "$UPDATE" != "yes" ]]; then
        echo "CẦN CẬP NHẬT: $MESSAGE Cập nhật nhánh, chờ CI chạy lại rồi gọi lại script, hoặc gọi với --update." >&2
        exit 3
      fi
      [[ "$updates" -lt "$MAX_UPDATES" ]] ||
        die "$MESSAGE Đã cập nhật nhánh $updates lần mà nhánh chính vẫn đi tiếp; người xem rồi gọi lại."
      gh api -X PUT "repos/$REPO/pulls/$PR/update-branch" -f expected_head_sha="$HEAD_SHA" > /dev/null ||
        die "GitHub không cập nhật được nhánh '$HEAD_REF' theo '$BASE_BRANCH' (thường do xung đột). Gộp bằng tay rồi gọi lại."
      updates=$((updates + 1))
      updated_from="$HEAD_SHA"
      echo "ĐÃ CẬP NHẬT: $MESSAGE GitHub gộp '$BASE_BRANCH' vào nhánh; chờ CI trên commit mới." >&2
      ;;
    pending)
      if [[ "$UPDATE" != "yes" ]]; then
        echo "CHƯA XONG: $MESSAGE" >&2
        exit 2
      fi
      ;;
  esac
  if [[ "$(date +%s)" -ge "$deadline" ]]; then
    echo "CHƯA XONG: $MESSAGE Đã chờ hết ${UPDATE_TIMEOUT_SECONDS} giây; gọi lại script." >&2
    exit 2
  fi
  [[ "$RESULT" != "pending" ]] || echo "Chờ: $MESSAGE" >&2
  sleep "$POLL_SECONDS"
done

[[ -z "$NO_UPDATE_NOTE" ]] || echo "$NO_UPDATE_NOTE" >&2

# The commit that will be merged, with a warning when it is very recent: a commit pushed after the merge never
# reaches main (app lesson L-018). The merge below is pinned to this commit.
commit_info="$( { gh api "repos/$REPO/commits/$HEAD_SHA" 2>/dev/null || true; } \
  | jq -r --argjson now "${MERGE_NOW_EPOCH:-$(date +%s)}" \
    '"\((($now - (.commit.committer.date | fromdateiso8601)) / 60) | floor) \(.commit.message | split("\n")[0])"' \
    2>/dev/null || true)"
age_minutes="${commit_info%% *}"
if [[ "$age_minutes" =~ ^-?[0-9]+$ ]]; then
  echo "Commit sẽ merge: ${HEAD_SHA:0:7} (${age_minutes} phút trước) ${commit_info#* }"
  if [[ "$age_minutes" -lt "$RECENT_MINUTES" && "$updates" -eq 0 ]]; then
    echo "CHÚ Ý: commit cuối của nhánh '$HEAD_REF' mới ${age_minutes} phút trước. Còn ai định đẩy thêm commit lên nhánh này thì dừng lại: commit đẩy sau khi merge không vào '$BASE_BRANCH'; khi đó làm tiếp trên một nhánh mới." >&2
  fi
else
  echo "Commit sẽ merge: ${HEAD_SHA:0:7}"
fi
local_branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || true)"
if [[ "$local_branch" == "$HEAD_REF" && "$updates" -eq 0 && "$(git rev-parse HEAD 2>/dev/null || true)" != "$HEAD_SHA" ]]; then
  echo "CHÚ Ý: nhánh '$HEAD_REF' trên máy này không ở commit ${HEAD_SHA:0:7} của GitHub: còn commit chưa đẩy, hoặc chưa kéo về." >&2
fi

# docs/STATUS.md is not in git: status-page.yml builds it on the status branch (docs/CLAUDE.md, section Trang tiến độ).
pr_files="$( { gh_all "repos/$REPO/pulls/$PR/files?per_page=100" 2>/dev/null || true; } | jq -r '.filename' 2>/dev/null || true)"
if grep -qx 'docs/STATUS.md' <<<"$pr_files"; then
  echo "CHÚ Ý: PR có docs/STATUS.md. Trang tiến độ không có trong git; status-page.yml sinh nó trên nhánh status sau khi merge. Bỏ file khỏi PR (git rm --cached docs/STATUS.md) để PR không xung đột nhau (docs/CLAUDE.md, mục Trang tiến độ)." >&2
fi

if [[ "$DRY_RUN" == "yes" ]]; then
  echo "SẴN SÀNG: PR #$PR ($TITLE) tại commit ${HEAD_SHA:0:7} đủ điều kiện merge (chạy thử, chưa merge)."
  exit 0
fi

# Squash merge pinned to the checked sha: GitHub refuses if the PR got a new commit.
gh api -X PUT "repos/$REPO/pulls/$PR/merge" \
  -f merge_method=squash \
  -f sha="$HEAD_SHA" \
  -f commit_title="$TITLE (#$PR)" >/dev/null && merge_failed="no" || merge_failed="yes"

if [[ "$merge_failed" == "yes" ]]; then
  # The answer was an error: look for the commit on main before refusing (GitHub may write it a few seconds later).
  found=""
  for attempt in 1 2 3; do
    if ! found="$(merged_on_main)"; then
      refuse_same_title "$found" after-merge
    fi
    [[ -z "$found" && "$attempt" -lt 3 ]] || break
    sleep "${MERGE_RECHECK_SECONDS:-10}"
  done
  [[ -n "$found" ]] || die "GitHub từ chối merge PR #$PR."
fi

# Delete the merged branch if it belongs to an auto-run session. The repository setting "Automatically delete head
# branches" usually deletes it a few seconds after the merge, while the delete of a cloud session may be refused by its
# proxy (L-149): after a failed delete the branch is read again up to BRANCH_GONE_TRIES times, and only a branch still
# there after every read gets a warning, because branches that pile up past 100 once broke this script on cloud
# sessions (L-092). A read that fails (404 or any other error) counts as gone, as before.
if [[ "$HEAD_REF" == claude/* ]]; then
  if ! gh api -X DELETE "repos/$REPO/git/refs/heads/$HEAD_REF" >/dev/null 2>&1; then
    branch_left="no"
    for ((try = 1; try <= BRANCH_GONE_TRIES; try++)); do
      if ! gh api "repos/$REPO/git/ref/heads/$HEAD_REF" >/dev/null 2>&1; then
        branch_left="no"
        break
      fi
      branch_left="yes"
      [[ "$try" -lt "$BRANCH_GONE_TRIES" ]] && sleep "$BRANCH_GONE_WAIT_SECONDS"
    done
    if [[ "$branch_left" == "yes" ]]; then
      echo "CHÚ Ý: không xóa được nhánh '$HEAD_REF' sau khi merge; người xóa tay để số nhánh không tăng (L-092)." >&2
    fi
  fi
fi

if [[ "$merge_failed" == "yes" ]]; then
  # GitHub may not send the push event for such a commit, so main-guard is started by hand (L-080).
  since="$(date -u +%Y-%m-%dT%H:%M:00Z)"
  echo "ĐÃ MERGE (GitHub báo lỗi): PR #$PR ($TITLE) tại commit ${found:0:7}; GitHub báo lỗi nhưng commit đã vào nhánh chính (L-080)."
  if gh api -X POST "repos/$REPO/actions/workflows/main-guard.yml/dispatches" -f ref="$BASE_BRANCH" >/dev/null 2>&1; then
    echo "Đã yêu cầu chạy main-guard; chờ: scripts/wait-run.sh --workflow main-guard.yml --since $since."
  else
    echo "CHÚ Ý: không yêu cầu được main-guard; chạy tay trên tab Actions." >&2
  fi
  exit 0
fi

echo "ĐÃ MERGE: PR #$PR ($TITLE) tại commit ${HEAD_SHA:0:7}."
