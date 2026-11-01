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
#              2 checks still running (with --update: still running when the wait ran out), call again later
#              3 branch is behind main: update it, wait for CI, then call again (or call with --update)
#
# Every condition is checked again after each wait, so a PR that turns red, becomes a draft or gets a label,
# or a main branch that breaks while waiting, is still refused. The merge is pinned to the checked commit.
#
# Uses only the GitHub REST API through `gh api`.
# Changing this file changes the quality gate: high-risk plan, cross-review required.
# Messages are in Vietnamese for the team; comments are in English.

set -euo pipefail

BASE_BRANCH="main"
# Checks that must be green. Names match the job names in .github/workflows/pr-check.yml. The emulator tests run after
# the merge, in the ui-test job of main-guard.yml (ADR-0043): a PR has no ui-test to wait for, but a red one left from
# before is still refused below like any red check.
REQUIRED_CHECKS=("changes" "build-and-test")
# Waiting with --update; tests shorten both.
UPDATE_TIMEOUT_SECONDS="${MERGE_UPDATE_TIMEOUT_SECONDS:-1800}"
POLL_SECONDS="${MERGE_POLL_SECONDS:-30}"
# Needing more updates than this means main keeps moving; a person should look.
MAX_UPDATES=3
# A head commit younger than this gets a warning: someone may still be pushing to the branch (app lesson L-018).
RECENT_MINUTES="${MERGE_RECENT_MINUTES:-10}"

die()  { echo "TỪ CHỐI: $*" >&2; exit 1; }
# A condition not met yet: recorded for the main loop, which waits (--update) or stops with exit 2.
pending() { RESULT="pending"; MESSAGE="$*"; }

# The squash commit of this very PR on main: first line "<title> (#<PR>)" and the file tree of the PR head (L-080).
# GitHub once answered a merge with an error although the commit reached main. Both reads are best effort: a read
# that fails counts as "not seen", and the script goes on as before. Prints the commit sha, or nothing.
merged_on_main() {
  local tree
  tree="$(gh api "repos/$REPO/git/commits/$HEAD_SHA" --jq .tree.sha 2>/dev/null || true)"
  [[ -n "$tree" ]] || return 0
  { gh api "repos/$REPO/commits?sha=$BASE_BRANCH&per_page=30" 2>/dev/null || true; } \
    | jq -r --arg title "$TITLE (#$PR)" --arg tree "$tree" \
        '[.[]? | select((.commit.message | split("\n")[0]) == $title and .commit.tree.sha == $tree)][0].sha // empty' 2>/dev/null \
    || true
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
  latest="$(jq -c --arg n "$name" '[.[] | select(.name==$n)] | sort_by(.started_at) | last // empty' <<<"$RUNS")"
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
  die "kiểm tra '$name' kết thúc với kết quả '$conclusion'."
}

# Checks every condition once. Sets RESULT to ready, pending or behind (MESSAGE says why); refusals exit at once.
evaluate() {
  RESULT="ready"
  MESSAGE=""
  local pr_json state draft base already broken broken_json refuse_broken culprit revert_check run_branches run_branch run_id run_status behind_by failed c

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
  already="$(merged_on_main)"
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

  # While an auto-run session runs, main must not move: only that session's own PRs (its task branches and its
  # report branch) are merged (docs/auto-run.md, section 1), plus the exact revert checked above, which the session
  # merges to bring main back (ADR-0025, point 4). A session is running when its report branch holds its run file
  # with status "running". Stale report branches of finished sessions do not block anything.
  run_branches="$(gh_all "repos/$REPO/branches?per_page=100" \
    | jq -r '.name | select(test("^claude/RUN-[0-9]{8}-[0-9]{2}-report$"))')"
  for run_branch in $run_branches; do
    run_id="${run_branch#claude/}"
    run_id="${run_id%-report}"
    run_status="$( { gh api "repos/$REPO/contents/docs/runs/$run_id.md?ref=$run_branch" \
      -H "Accept: application/vnd.github.raw" 2>/dev/null || true; } \
      | sed -n '/^---$/,/^---$/s/^status:[[:space:]]*\([a-z]*\).*/\1/p' | head -n 1)"
    [[ "$run_status" == "running" ]] || continue
    if [[ "$HEAD_REF" != "$run_branch" && "$HEAD_REF" != claude/T-[0-9]* && "$EXACT_REVERT" != "yes" ]]; then
      die "phiên $run_id đang chạy. Trong lúc phiên chạy chỉ merge PR của chính phiên đó (docs/auto-run.md, mục 1). Chờ phiên kết thúc rồi gọi lại."
    fi
  done

  # The PR branch must contain the latest commit of main.
  behind_by="$(gh api "repos/$REPO/compare/$BASE_BRANCH...$HEAD_SHA" --jq .behind_by)"
  if [[ "$behind_by" != "0" ]]; then
    RESULT="behind"
    MESSAGE="nhánh '$HEAD_REF' chậm $behind_by commit so với '$BASE_BRANCH'."
    return 0
  fi

  # Check results on the exact head commit of the PR.
  RUNS="$(gh api "repos/$REPO/commits/$HEAD_SHA/check-runs?per_page=100" --jq '.check_runs')"

  # No failed check is accepted, including checks not in the lists above.
  failed="$(jq -r '[.[] | select(.status=="completed" and (.conclusion|IN("failure","cancelled","timed_out","action_required","startup_failure"))) | .name] | unique | join(", ")' <<<"$RUNS")"
  [[ -z "$failed" ]] || die "có kiểm tra không qua: $failed."

  for c in "${REQUIRED_CHECKS[@]}"; do
    check_one "$c"
    [[ "$RESULT" == "ready" ]] || return 0
  done
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
    found="$(merged_on_main)"
    [[ -z "$found" && "$attempt" -lt 3 ]] || break
    sleep "${MERGE_RECHECK_SECONDS:-10}"
  done
  [[ -n "$found" ]] || die "GitHub từ chối merge PR #$PR."
fi

# Delete the merged branch if it belongs to an auto-run session. The repository setting "Automatically delete head
# branches" usually deleted it already; a branch still there after a failed delete gets a warning, because branches
# that pile up past 100 once broke this script on cloud sessions (L-092).
if [[ "$HEAD_REF" == claude/* ]]; then
  if ! gh api -X DELETE "repos/$REPO/git/refs/heads/$HEAD_REF" >/dev/null 2>&1 \
    && gh api "repos/$REPO/git/ref/heads/$HEAD_REF" >/dev/null 2>&1; then
    echo "CHÚ Ý: không xóa được nhánh '$HEAD_REF' sau khi merge; người xóa tay để số nhánh không tăng (L-092)." >&2
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
