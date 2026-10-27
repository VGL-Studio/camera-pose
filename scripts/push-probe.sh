#!/usr/bin/env bash
# Checks at the start of a session that this environment may push a change to .github/workflows/ (L-086 of
# camera-pose). A token without the GitHub `workflow` scope pushes ordinary commits but refuses any commit that
# touches a workflow file, so a task that edits a workflow only learns it after all its work is done.
#
# Usage:   scripts/push-probe.sh <RUN-YYYYMMDD-NN>
# Exit:    0 the push went through (the probe branch is deleted again, or a warning says it is left).
#          3 the refusal is a `bo-qua-loai` row of the failure playbook (PB-13: no `workflow` scope): the session
#            skips the tasks of that kind (docs/auto-run.md, mục 2).
#          4 no clear answer (another refusal, or a network error that did not clear): the session records it and
#            runs its tasks as usual. 2 bad arguments, or nothing to probe.
# What it does: commits one comment line to a workflow of origin/main that runs on no branch push (dispatch or
#          schedule only, else the first one), in a temporary worktree, and pushes it to a new branch
#          claude/probe-<RUN>-<time>-<pid>. No workflow runs on a push to that branch. The current checkout, branch
#          and files never change. Each call uses a new branch, so a probe branch left by a refused delete never
#          blocks the next call.
# Env:     PUSH_PROBE_PLAYBOOK        failure playbook table (default docs/ops/failure-playbook.md; tests)
#          PUSH_PROBE_RETRY_SECONDS   waits between retries of a network error (default "60 120 240")
# Runs in the current repository, so it works from a worktree.
# Messages are in Vietnamese for the team; comments are in English.

set -uo pipefail

run="${1:-}"
if ! [[ "${run}" =~ ^RUN-[0-9]{8}-[0-9]{2}$ ]]; then
  echo "LỖI: cách dùng: scripts/push-probe.sh RUN-YYYYMMDD-NN (nhận '${run}')." >&2
  exit 2
fi
here="$(cd "$(dirname "$0")" && pwd)"
branch="claude/probe-${run}-$(date -u +%H%M%S)-$$"

if ! git rev-parse --verify -q origin/main > /dev/null; then
  echo "LỖI: không có origin/main; chạy git fetch origin main trước." >&2
  exit 2
fi
workflows="$(git ls-tree --name-only origin/main .github/workflows/ | grep -E '\.ya?ml$')"
if [ -z "${workflows}" ]; then
  echo "LỖI: origin/main không có file nào trong .github/workflows/, không có gì để thử." >&2
  exit 2
fi
# Prefer a workflow without a push trigger: even a stray trigger can then never run on the probe branch.
workflow=""
while read -r candidate; do
  if ! git show "origin/main:${candidate}" | grep -qE '^[[:space:]]+push:'; then
    workflow="${candidate}"
    break
  fi
done <<< "${workflows}"
[ -n "${workflow}" ] || workflow="$(head -n 1 <<< "${workflows}")"

tmp="$(mktemp -d)"
cleanup() {
  git worktree remove --force "${tmp}/probe" > /dev/null 2>&1 || true
  rm -rf "${tmp}"
}
trap cleanup EXIT

git worktree add -q --detach "${tmp}/probe" origin/main || { echo "LỖI: không tạo được worktree tạm." >&2; exit 4; }
printf '# push probe %s (scripts/push-probe.sh); this branch is deleted right after the push\n' "${run}" \
  >> "${tmp}/probe/${workflow}"
git -C "${tmp}/probe" -c user.name="push-probe" -c user.email="push-probe@invalid" -c commit.gpgsign=false \
  commit -q --no-verify -m "Thử đẩy thay đổi workflow (${run}), nhánh xóa ngay" -- "${workflow}" \
  || { echo "LỖI: không commit được trong worktree tạm." >&2; exit 4; }

# Messages git prints for a network problem, as in save-branch.sh; anything else is not retried.
network_pattern='Could not resolve host|Connection timed out|Connection reset|The requested URL returned error: 5|RPC failed|early EOF|hung up'
read -r -a waits <<< "${PUSH_PROBE_RETRY_SECONDS:-60 120 240}"
attempt=0
while :; do
  output="$(git -C "${tmp}/probe" push origin "HEAD:refs/heads/${branch}" 2>&1)"
  code=$?
  [ "${code}" -ne 0 ] || break
  # One pattern for every refusal the session knows: the failure playbook (PB-13 for the workflow scope).
  printf '%s\n' "${output}" > "${tmp}/push.log"
  kind="$("${here}/failure-kind.sh" --playbook "${tmp}/push.log" ${PUSH_PROBE_PLAYBOOK:+"${PUSH_PROBE_PLAYBOOK}"} 2> /dev/null)"
  if [[ "${kind}" =~ ^PB-[0-9]+\ bo-qua-loai ]]; then
    echo "TỪ CHỐI: ${kind%%:*}: GitHub không nhận thay đổi .github/workflows/ từ phiên này."
    echo "Bỏ qua mọi task mà scripts/plan-check.py --touches .github/workflows/ <plan> trả 0, trước khi tạo nhánh; làm các task khác (auto-run.md, mục 2)."
    grep -m1 -E 'refusing to allow|remote rejected' <<< "${output}"
    exit 3
  fi
  if grep -qE "${network_pattern}" <<< "${output}" && [ "${attempt}" -lt "${#waits[@]}" ]; then
    sleep "${waits[${attempt}]}"
    attempt=$((attempt + 1))
    continue
  fi
  echo "CHƯA RÕ: không đẩy được nhánh thử ${branch}, không phải lỗi đã biết trong bảng tra lỗi. Ghi vào mục Kiểm tra đầu phiên, làm các task như thường; không đẩy lại dưới dạng khác."
  grep -E '^ ?! |^(error|fatal|remote):' <<< "${output}" | tail -n 3
  exit 4
done

if git -C "${tmp}/probe" push -q origin --delete "${branch}" > /dev/null 2>&1; then
  echo "QUA: phiên đẩy được thay đổi .github/workflows/ (đã thử với ${workflow}, nhánh ${branch} đã xóa)."
else
  echo "QUA: phiên đẩy được thay đổi .github/workflows/ (đã thử với ${workflow})."
  echo "CHÚ Ý: không xóa được nhánh ${branch}; người xóa sau phiên (git push origin --delete ${branch})."
fi
exit 0
