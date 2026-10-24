#!/usr/bin/env bash
# Merges a PR into the main branch, only when it passes the whole quality gate (ADR-0010).
# This is the ONLY allowed way to merge into main, for people and AI alike.
#
# Usage:       scripts/merge-if-green.sh <PR number> [--dry-run | --update]
#              --dry-run: check the conditions only, do not merge.
#              --update:  a branch behind main is updated by GitHub (merge of main into it), then exit 2.
# Exit codes:  0 merged; also "GitHub answered the merge with an error but the commit is on main" and "the PR is
#                already on main" (L-054): both are reported and mean there is nothing left to merge
#              1 refused (a condition is not met, see the message)
#              2 checks still running, or (--update) branch just updated: wait for CI, then call again
#              3 branch is behind main (without --update): update it, wait for CI, then call again
#
# Uses only the GitHub REST API through `gh api`.
# Changing this file changes the quality gate: high-risk plan, cross-review required.
# Messages are in Vietnamese for the team; comments are in English.

set -euo pipefail

BASE_BRANCH="main"
# Checks that must be green. Names match the job names in .github/workflows/pr-check.yml.
REQUIRED_CHECKS=("changes" "build-and-test")
# Checks that may be skipped (when the PR does not touch code that runs in the app).
SKIPPABLE_CHECKS=("ui-test")

die()  { echo "TỪ CHỐI: $*" >&2; exit 1; }
wait_() { echo "CHƯA XONG: $*" >&2; exit 2; }

# Strict argument check: any unknown argument (including a mistyped --dry-run) is refused,
# so a real merge never happens when the caller only wanted a dry run.
DRY_RUN="no"
UPDATE="no"
case "$#" in
  1) ;;
  2) case "$2" in
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

PR_JSON="$(gh api "repos/$REPO/pulls/$PR")"
STATE="$(jq -r .state <<<"$PR_JSON")"
DRAFT="$(jq -r .draft <<<"$PR_JSON")"
BASE="$(jq -r .base.ref <<<"$PR_JSON")"
HEAD_SHA="$(jq -r .head.sha <<<"$PR_JSON")"
HEAD_REF="$(jq -r .head.ref <<<"$PR_JSON")"
TITLE="$(jq -r .title <<<"$PR_JSON")"

[[ "$STATE" == "open" ]] || die "PR #$PR không ở trạng thái mở ($STATE)."

# The squash commit of this very PR on main: first line "<title> (#<PR>)" and the file tree of the PR head (L-054).
# GitHub once answered a merge with an error although the commit reached main (PR #67). Both reads are best effort:
# a read that fails counts as "not seen", and the script goes on as before. Prints the commit sha, or nothing.
HEAD_TREE="$(gh api "repos/$REPO/git/commits/$HEAD_SHA" --jq .tree.sha 2>/dev/null || true)"
merged_on_main() {
  [[ -n "$HEAD_TREE" ]] || return 0
  { gh api "repos/$REPO/commits?sha=$BASE_BRANCH&per_page=30" 2>/dev/null || true; } \
    | jq -r --arg title "$TITLE (#$PR)" --arg tree "$HEAD_TREE" \
        '[.[]? | select((.commit.message | split("\n")[0]) == $title and .commit.tree.sha == $tree)][0].sha // empty' 2>/dev/null \
    || true
}
ALREADY="$(merged_on_main)"
if [[ -n "$ALREADY" ]]; then
  echo "ĐÃ MERGE TRƯỚC ĐÓ: PR #$PR đã nằm trên nhánh chính ở commit ${ALREADY:0:7} (GitHub chưa đóng PR). Không merge lần hai; đóng PR trên web."
  exit 0
fi
[[ "$DRAFT" == "false" ]] || die "PR #$PR đang là bản nháp."
# A PR that an auto-run session prepared for a person (docs/auto-run.md, section 3, "Khi dừng") carries the
# needs-human label. Only a person removes it, after reviewing the PR; until then nobody merges it.
if jq -e '[(.labels // [])[].name] | index("needs-human")' <<<"$PR_JSON" > /dev/null; then
  die "PR #$PR mang nhãn needs-human: phiên tự chạy soạn để người xem. Người xem xong, gỡ nhãn rồi mới merge."
fi
[[ "$BASE" == "$BASE_BRANCH" ]] || die "PR #$PR nhắm vào '$BASE', không phải '$BASE_BRANCH'."

# While main is broken (an open issue labelled main-broken, created by main-guard.yml), nothing is merged
# (ADR-0010), with one exception (ADR-1002, point 3): a PR that is exactly the revert of the blamed commit.
# The content is checked, not the branch name: one commit right on top of the latest main whose file tree is
# what `git revert <culprit>` gives there (scripts/is-exact-revert.sh). Every other condition below still applies.
# IS_EXACT_REVERT replaces that script in the tests only.
BROKEN_JSON="$(gh api "repos/$REPO/issues?state=open&labels=main-broken&per_page=10" --jq '[.[] | {number, body}]')"
BROKEN="$(jq -r '[.[].number] | join(",")' <<<"$BROKEN_JSON")"
EXACT_REVERT="no"
if [[ -n "$BROKEN" ]]; then
  REFUSE_BROKEN="nhánh chính đang đỏ (mục mang nhãn main-broken: #$BROKEN). Người phụ trách xử lý và đóng mục đó rồi mới merge tiếp."
  [[ "$(jq length <<<"$BROKEN_JSON")" == "1" ]] || die "$REFUSE_BROKEN"
  CULPRIT="$(jq -r '.[0].body // "" | capture("<!-- culprit:(?<s>[0-9a-f]+) -->").s // ""' <<<"$BROKEN_JSON")"
  [[ -n "$CULPRIT" ]] || die "$REFUSE_BROKEN Mục #$BROKEN không ghi commit gây lỗi."
  # The PR head by its pull ref; if the proxy of a cloud session refuses that ref, by its branch name (plan T-028, 3.4).
  git fetch -q origin "+refs/pull/$PR/head:refs/merge-if-green/pr" "+refs/heads/$BASE_BRANCH:refs/merge-if-green/main" 2>/dev/null \
    || git fetch -q origin "+refs/heads/$HEAD_REF:refs/merge-if-green/pr" "+refs/heads/$BASE_BRANCH:refs/merge-if-green/main" 2>/dev/null \
    || die "$REFUSE_BROKEN Không lấy được commit của PR #$PR để kiểm có phải bản revert không."
  [[ "$(git rev-parse refs/merge-if-green/pr)" == "$HEAD_SHA" ]] \
    || die "$REFUSE_BROKEN Commit lấy về của PR #$PR khác ${HEAD_SHA:0:7}; gọi lại sau."
  if ! REVERT_CHECK="$("${IS_EXACT_REVERT:-$(dirname "$0")/is-exact-revert.sh}" "$CULPRIT" "$HEAD_SHA" refs/merge-if-green/main)"; then
    die "$REFUSE_BROKEN $REVERT_CHECK"
  fi
  echo "BẢN REVERT ĐÚNG: PR #$PR là bản revert của ${CULPRIT:0:7}; vẫn kiểm mọi điều kiện còn lại." >&2
  EXACT_REVERT="yes"
fi

# While an auto-run session runs, main must not move: only that session's own PRs (its task branches and its
# report branch) are merged (docs/auto-run.md, section 1), plus the exact revert checked above, which the session
# merges to bring main back (ADR-1002, point 4). A session is running when its report branch holds its run file
# with status "running". Stale report branches of finished sessions do not block anything.
RUN_BRANCHES="$(gh api "repos/$REPO/branches?per_page=100" --paginate \
  --jq '.[].name | select(test("^claude/RUN-[0-9]{8}-[0-9]{2}-report$"))')"
for RUN_BRANCH in $RUN_BRANCHES; do
  RUN_ID="${RUN_BRANCH#claude/}"
  RUN_ID="${RUN_ID%-report}"
  RUN_STATUS="$( { gh api "repos/$REPO/contents/docs/runs/$RUN_ID.md?ref=$RUN_BRANCH" \
    -H "Accept: application/vnd.github.raw" 2>/dev/null || true; } \
    | sed -n '/^---$/,/^---$/s/^status:[[:space:]]*\([a-z]*\).*/\1/p' | head -n 1)"
  [[ "$RUN_STATUS" == "running" ]] || continue
  if [[ "$HEAD_REF" != "$RUN_BRANCH" && "$HEAD_REF" != claude/T-[0-9]* && "$EXACT_REVERT" != "yes" ]]; then
    die "phiên $RUN_ID đang chạy. Trong lúc phiên chạy chỉ merge PR của chính phiên đó (docs/auto-run.md, mục 1). Chờ phiên kết thúc rồi gọi lại."
  fi
done

# The PR branch must contain the latest commit of main.
BEHIND_BY="$(gh api "repos/$REPO/compare/$BASE_BRANCH...$HEAD_SHA" --jq .behind_by)"
if [[ "$BEHIND_BY" != "0" ]]; then
  # --update (plan T-029, L-017): GitHub merges main into the branch, pinned to the checked head. A conflict makes
  # GitHub refuse (422); nothing is merged by hand here. The new commit needs its own CI run, so exit 2.
  if [[ "$UPDATE" == "yes" ]]; then
    gh api "repos/$REPO/pulls/$PR/update-branch" -X PUT -f expected_head_sha="$HEAD_SHA" >/dev/null \
      || die "GitHub không cập nhật được nhánh '$HEAD_REF' (xung đột với nhánh chính?). Cập nhật bằng tay: gộp '$BASE_BRANCH' vào nhánh rồi đẩy."
    wait_ "đã cập nhật nhánh '$HEAD_REF' theo '$BASE_BRANCH'. Chờ CI của commit mới (scripts/wait-run.sh --pr $PR) rồi gọi lại."
  fi
  echo "CẦN CẬP NHẬT: nhánh '$HEAD_REF' chậm $BEHIND_BY commit so với '$BASE_BRANCH'. Cập nhật nhánh, chờ CI chạy lại rồi gọi lại script." >&2
  exit 3
fi

# Check results on the exact head commit of the PR.
RUNS="$(gh api "repos/$REPO/commits/$HEAD_SHA/check-runs?per_page=100" --jq '.check_runs')"

# No failed check is accepted, including checks not in the lists above.
FAILED="$(jq -r '[.[] | select(.status=="completed" and (.conclusion|IN("failure","cancelled","timed_out","action_required","startup_failure"))) | .name] | unique | join(", ")' <<<"$RUNS")"
[[ -z "$FAILED" ]] || die "có kiểm tra không qua: $FAILED."

check_one() {
  local name="$1" allow_skip="$2" latest status conclusion
  latest="$(jq -c --arg n "$name" '[.[] | select(.name==$n)] | sort_by(.started_at) | last // empty' <<<"$RUNS")"
  [[ -n "$latest" ]] || wait_ "chưa thấy kiểm tra '$name' trên commit ${HEAD_SHA:0:7}."
  status="$(jq -r .status <<<"$latest")"
  conclusion="$(jq -r .conclusion <<<"$latest")"
  if [[ "$status" != "completed" ]]; then
    # A check run can stay queued or in_progress after its workflow run finished (L-017). Trust it only when the
    # check suite holding it completed with success; any other suite state still means "wait".
    suite="$(jq -r '.check_suite.id // empty' <<<"$latest")"
    if [[ -n "$suite" ]] && gh api "repos/$REPO/check-suites/$suite" --jq '[.status, .conclusion] | join(" ")' 2>/dev/null \
        | grep -qx 'completed success'; then
      echo "CHÚ Ý: kiểm tra '$name' còn ghi '$status' nhưng lần chạy chứa nó đã xong và xanh; coi là xanh." >&2
      return 0
    fi
    wait_ "kiểm tra '$name' đang ở trạng thái $status."
  fi
  if [[ "$conclusion" == "success" ]]; then return 0; fi
  if [[ "$allow_skip" == "yes" && "$conclusion" == "skipped" ]]; then return 0; fi
  die "kiểm tra '$name' kết thúc với kết quả '$conclusion'."
}

for c in "${REQUIRED_CHECKS[@]}"; do check_one "$c" no; done
for c in "${SKIPPABLE_CHECKS[@]}"; do check_one "$c" yes; done

# A last commit pushed less than 3 minutes ago: warn, do not refuse (plan T-029, L-018). A commit pushed after the
# merge does not reach main, so a caller still pushing should wait. MERGE_IF_GREEN_NOW replaces the clock in tests.
PUSHED_AT="$(gh api "repos/$REPO/commits/$HEAD_SHA" --jq '.commit.committer.date // empty' 2>/dev/null || true)"
if [[ -n "$PUSHED_AT" ]]; then
  AGE="$(jq -rn --arg d "$PUSHED_AT" --argjson now "${MERGE_IF_GREEN_NOW:-$(date -u +%s)}" '$now - ($d | fromdateiso8601)' 2>/dev/null || true)"
  if [[ "$AGE" =~ ^-?[0-9]+$ ]] && (( AGE < 180 )); then
    echo "CHÚ Ý: commit cuối ${HEAD_SHA:0:7} của '$HEAD_REF' đẩy lúc $PUSHED_AT, chưa tới 3 phút. Còn commit sắp đẩy thì chờ, vì commit đẩy sau khi merge không vào nhánh chính (L-018)." >&2
  fi
fi

if [[ "$DRY_RUN" == "yes" ]]; then
  echo "SẴN SÀNG: PR #$PR ($TITLE) tại commit ${HEAD_SHA:0:7} đủ điều kiện merge (chạy thử, chưa merge)."
  exit 0
fi

# Squash merge pinned to the checked sha: GitHub refuses if the PR got a new commit.
gh api -X PUT "repos/$REPO/pulls/$PR/merge" \
  -f merge_method=squash \
  -f sha="$HEAD_SHA" \
  -f commit_title="$TITLE (#$PR)" >/dev/null && MERGE_FAILED="no" || MERGE_FAILED="yes"

if [[ "$MERGE_FAILED" == "yes" ]]; then
  # The answer was an error: look for the commit on main before refusing (GitHub may write it a few seconds later).
  FOUND=""
  for attempt in 1 2 3; do
    FOUND="$(merged_on_main)"
    [[ -z "$FOUND" ]] || break
    sleep "${MERGE_IF_GREEN_POLL_SECONDS:-10}"
  done
  [[ -n "$FOUND" ]] || die "GitHub từ chối merge PR #$PR."
fi

# Delete the merged branch if it belongs to an auto-run session.
if [[ "$HEAD_REF" == claude/* ]]; then
  gh api -X DELETE "repos/$REPO/git/refs/heads/$HEAD_REF" >/dev/null 2>&1 || true
fi

if [[ "$MERGE_FAILED" == "yes" ]]; then
  # GitHub may not send the push event for such a commit, so main-guard is started by hand (L-054).
  SINCE="$(date -u +%Y-%m-%dT%H:%M:00Z)"
  echo "ĐÃ MERGE (GitHub báo lỗi): PR #$PR ($TITLE) tại commit ${FOUND:0:7}; GitHub báo lỗi nhưng commit đã vào nhánh chính (L-054)."
  if gh api -X POST "repos/$REPO/actions/workflows/main-guard.yml/dispatches" -f ref="$BASE_BRANCH" >/dev/null 2>&1; then
    echo "Đã yêu cầu chạy main-guard; chờ: scripts/wait-run.sh --workflow main-guard.yml --since $SINCE."
  else
    echo "CHÚ Ý: không yêu cầu được main-guard; chạy tay trên tab Actions." >&2
  fi
  exit 0
fi

echo "ĐÃ MERGE: PR #$PR ($TITLE) tại commit ${HEAD_SHA:0:7}."
