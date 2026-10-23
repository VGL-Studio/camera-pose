#!/usr/bin/env bash
# Merges a PR into the main branch, only when it passes the whole quality gate (ADR-0010).
# This is the ONLY allowed way to merge into main, for people and AI alike.
#
# Usage:       scripts/merge-if-green.sh <PR number> [--dry-run]
#              --dry-run: check the conditions only, do not merge.
# Exit codes:  0 merged
#              1 refused (a condition is not met, see the message)
#              2 checks still running, call again later
#              3 branch is behind main: update it, wait for CI, then call again
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
case "$#" in
  1) ;;
  2) [[ "$2" == "--dry-run" ]] || die "tham số thứ hai không hợp lệ: '$2'. Chỉ chấp nhận --dry-run." ; DRY_RUN="yes" ;;
  *) die "cách dùng: scripts/merge-if-green.sh <số PR> [--dry-run]" ;;
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
  [[ "$status" == "completed" ]] || wait_ "kiểm tra '$name' đang ở trạng thái $status."
  if [[ "$conclusion" == "success" ]]; then return 0; fi
  if [[ "$allow_skip" == "yes" && "$conclusion" == "skipped" ]]; then return 0; fi
  die "kiểm tra '$name' kết thúc với kết quả '$conclusion'."
}

for c in "${REQUIRED_CHECKS[@]}"; do check_one "$c" no; done
for c in "${SKIPPABLE_CHECKS[@]}"; do check_one "$c" yes; done

if [[ "$DRY_RUN" == "yes" ]]; then
  echo "SẴN SÀNG: PR #$PR ($TITLE) tại commit ${HEAD_SHA:0:7} đủ điều kiện merge (chạy thử, chưa merge)."
  exit 0
fi

# Squash merge pinned to the checked sha: GitHub refuses if the PR got a new commit.
gh api -X PUT "repos/$REPO/pulls/$PR/merge" \
  -f merge_method=squash \
  -f sha="$HEAD_SHA" \
  -f commit_title="$TITLE (#$PR)" >/dev/null \
  || die "GitHub từ chối merge PR #$PR."

# Delete the merged branch if it belongs to an auto-run session.
if [[ "$HEAD_REF" == claude/* ]]; then
  gh api -X DELETE "repos/$REPO/git/refs/heads/$HEAD_REF" >/dev/null 2>&1 || true
fi

echo "ĐÃ MERGE: PR #$PR ($TITLE) tại commit ${HEAD_SHA:0:7}."
