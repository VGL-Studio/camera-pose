#!/usr/bin/env bash
# Checks with git alone that a commit is exactly the revert of the blamed commit, made on the latest main
# (ADR-1002, point 3). scripts/merge-if-green.sh calls it while a main-broken issue is open: the branch name
# proves nothing, the content does.
#
# Usage:   scripts/is-exact-revert.sh <culprit sha> <head sha> <base ref>
# Exit:    0  <head> is one commit right on top of <base>, and its file tree is what
#             `git revert <culprit>` gives on <base>
#          1  anything else; the reason is printed (in Vietnamese, for the team)
# Changing this file changes the quality gate: high-risk plan, cross-review required.

set -euo pipefail

no() { echo "KHÔNG PHẢI BẢN REVERT ĐÚNG: $*"; exit 1; }

[ "$#" -eq 3 ] || no "cách dùng: scripts/is-exact-revert.sh <commit gây lỗi> <commit của PR> <nhánh chính>"
culprit="$1" head="$2" base="$3"

base_sha="$(git rev-parse --verify --quiet "${base}^{commit}")" || no "không tìm thấy ${base}."
head_sha="$(git rev-parse --verify --quiet "${head}^{commit}")" || no "không tìm thấy commit ${head}."
culprit_sha="$(git rev-parse --verify --quiet "${culprit}^{commit}")" || no "không tìm thấy commit gây lỗi ${culprit}."

count="$(git rev-list --count "${base_sha}..${head_sha}")"
[ "${count}" = "1" ] || no "PR có ${count} commit so với nhánh chính, phải đúng 1."
parent="$(git rev-parse --verify --quiet "${head_sha}^")" || no "commit ${head_sha::7} không có commit cha."
[ "${parent}" = "${base_sha}" ] || no "commit ${head_sha::7} không nằm ngay trên nhánh chính mới nhất."

# Build the expected revert in a throwaway worktree, so the caller's checkout is never touched.
tmp="$(mktemp -d)"
cleanup() {
  git worktree remove --force "${tmp}/tree" > /dev/null 2>&1 || true
  rm -rf "${tmp}"
}
trap cleanup EXIT
git worktree add --quiet --detach "${tmp}/tree" "${base_sha}" > /dev/null 2>&1 || no "không tạo được worktree tạm."
if ! git -C "${tmp}/tree" revert --no-edit --no-commit "${culprit_sha}" > /dev/null 2>&1; then
  no "git revert ${culprit_sha::7} trên nhánh chính bị xung đột."
fi
expected="$(git -C "${tmp}/tree" write-tree)"
actual="$(git rev-parse "${head_sha}^{tree}")"
[ "${expected}" = "${actual}" ] || no "cây file của ${head_sha::7} khác kết quả git revert ${culprit_sha::7} trên nhánh chính."
echo "BẢN REVERT ĐÚNG: ${head_sha::7} là git revert ${culprit_sha::7} trên ${base_sha::7}."
