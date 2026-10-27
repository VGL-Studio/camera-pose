#!/usr/bin/env bash
# Pushes a task branch so it does not live only on the temporary cloud machine, and bundles it when the push
# fails (L-081).
#
# Usage:   scripts/save-branch.sh <branch>                push (retrying network errors); bundle if that fails
#          scripts/save-branch.sh --bundle-only <branch>  bundle only, no push (the environment refused the push call)
# Exit:    0 pushed. 3 not pushed (with a bundle when the branch has commits beyond origin/main). 2 bad arguments.
# Env:     SAVE_BRANCH_DIR            where bundles go (default $HOME/rescue, outside any worktree)
#          SAVE_BRANCH_RETRY_SECONDS  waits between retries of a network error (default "60 120 240")
#          SAVE_BRANCH_REPORT_DIR     worktree of the report branch (default: the worktree on claude/RUN-*-report,
#                                     or a temporary one on the local claude/RUN-*-report branch)
#          SAVE_BRANCH_PATCH_MAX_BYTES  largest patch kept on the report branch (default 1048576)
# Hand-off as text (camera-pose L-086, T-068): when GitHub itself refuses the push because the token may not change
# .github/workflows/ (the message below), the commits are also written as a patch on the report branch, which is
# pushed, so a person can review and apply them after the cloud machine is gone; the bundle stays on that machine.
# This is the only case: never with --bundle-only (the environment refused the push call, and writing the same
# content elsewhere would be pushing it again in another form), never for any other refusal. The patch is an mbox
# (git format-patch) when replaying it rebuilds the branch exactly, else one combined diff (a branch with a merge
# of main); it is checked by applying it in a temporary worktree. It is left out when that check fails, when it
# is larger than the limit, when it is not UTF-8 text, or when scripts/secret-scan.py finds a secret in it.
# Never retries a push the environment refused: that call never reaches this script, so the session itself must
# not push again in another form (auto-run.md, "Khi không đẩy được").
# Runs in the current repository, so it works from a worktree.
# Messages are in Vietnamese for the team; comments are in English.

set -uo pipefail

bundle_only=no
patch_handoff=no
# GitHub's refusal of a push that changes .github/workflows/ without the workflow scope (L-086).
workflow_scope_pattern='refusing to allow an? (OAuth App|GitHub App|Personal Access Token)[^)]*workflow'
if [ "${1:-}" = "--bundle-only" ]; then
  bundle_only=yes
  shift
fi
branch="${1:-}"
case "${branch}" in
  claude/*) ;;
  *) echo "LỖI: nhánh phải có dạng claude/... (nhận '${branch}')." >&2; exit 2 ;;
esac
if ! git rev-parse --verify -q "refs/heads/${branch}" > /dev/null; then
  echo "LỖI: không có nhánh cục bộ ${branch}." >&2
  exit 2
fi
head_sha="$(git rev-parse "refs/heads/${branch}")"
short_sha="$(git rev-parse --short=7 "${head_sha}")"

if [ "${bundle_only}" = "no" ]; then
  retry_waits="${SAVE_BRANCH_RETRY_SECONDS:-60 120 240}"
  # Messages git prints for a network problem; anything else (a refusal, no permission) is not retried.
  network_pattern='Could not resolve host|Connection timed out|Connection reset|The requested URL returned error: 5|RPC failed|early EOF|hung up'
  read -r -a waits <<< "${retry_waits}"
  attempt=0
  while :; do
    if err="$(git push -u origin "${branch}" 2>&1)"; then
      echo "ĐÃ ĐẨY: ${branch} (${short_sha})"
      exit 0
    fi
    if grep -qE "${network_pattern}" <<< "${err}" && [ "${attempt}" -lt "${#waits[@]}" ]; then
      echo "Lỗi mạng khi đẩy ${branch}, thử lại sau ${waits[${attempt}]} giây." >&2
      sleep "${waits[${attempt}]}"
      attempt=$((attempt + 1))
      continue
    fi
    echo "${err}" >&2
    if grep -qE "${workflow_scope_pattern}" <<< "${err}"; then
      patch_handoff=yes
    fi
    break
  done
fi

# Not pushed: keep the commits beyond origin/main in one file outside the worktree.
count="$(git rev-list --count "${branch}" --not origin/main 2> /dev/null || echo 0)"
if [ "${count}" -eq 0 ]; then
  echo "CHƯA ĐẨY: ${branch} (không cần bundle: nhánh chưa có commit nào ngoài origin/main)."
  exit 3
fi
dir="${SAVE_BRANCH_DIR:-${HOME}/rescue}"
mkdir -p "${dir}"
bundle="${dir}/${branch//\//__}.bundle"
if ! git bundle create "${bundle}" "${branch}" --not origin/main > /dev/null 2>&1 || ! git bundle verify "${bundle}" > /dev/null 2>&1; then
  echo "LỖI: không tạo được bundle ${bundle}. CHƯA ĐẨY: ${branch} (${short_sha}), ${count} commit chỉ có trên máy này." >&2
  exit 3
fi
size="$(wc -c < "${bundle}" | tr -d ' ')"
echo "CHƯA ĐẨY: ${branch} (${short_sha}). Đã giữ ${count} commit trong bundle."

# Writes the hand-off patch into $1 (a temporary folder) and prints its mode, mbox or diff, when replaying it on
# the base in a scratch worktree gives exactly the branch's tree; prints nothing otherwise.
make_patch() {
  local dir="$1" scratch mode
  scratch="${dir}/verify"
  for mode in mbox diff; do
    rm -rf "${scratch}"
    if ! git worktree add -q --detach "${scratch}" "${base}" > /dev/null 2>&1; then
      echo "noworktree"
      return 0
    fi
    if [ "${mode}" = mbox ]; then
      git format-patch --binary --stdout "${base}..${branch}" > "${dir}/patch" 2> /dev/null || { rm -f "${dir}/patch"; }
      [ -s "${dir}/patch" ] && git -C "${scratch}" -c user.name=save-branch -c user.email=save-branch@localhost \
        -c commit.gpgsign=false am -q < "${dir}/patch" > /dev/null 2>&1 \
        || { git -C "${scratch}" am --abort > /dev/null 2>&1; rm -f "${dir}/patch"; }
    else
      git diff --binary "${base}" "${branch}" > "${dir}/patch" 2> /dev/null || rm -f "${dir}/patch"
      [ -s "${dir}/patch" ] && git -C "${scratch}" apply --index "${dir}/patch" > /dev/null 2>&1 || rm -f "${dir}/patch"
    fi
    if [ -s "${dir}/patch" ] && [ "$(git -C "${scratch}" write-tree 2> /dev/null)" = "$(git rev-parse "${branch}^{tree}")" ]; then
      git worktree remove --force "${scratch}" > /dev/null 2>&1
      echo "${mode}"
      return 0
    fi
    rm -f "${dir}/patch"
    git worktree remove --force "${scratch}" > /dev/null 2>&1
  done
}

patch_note=""
patch_lines=""
temp_report=""
work_dir=""
# Temporary worktrees and folders go even when the script is stopped half way; a stale worktree entry is pruned.
cleanup() {
  if [ -n "${work_dir}" ]; then
    git worktree remove --force "${work_dir}/verify" > /dev/null 2>&1
    rm -rf "${work_dir}"
  fi
  if [ -n "${temp_report}" ]; then
    git worktree remove --force "${temp_report}" > /dev/null 2>&1
    rm -rf "$(dirname "${temp_report}")"
  fi
  git worktree prune > /dev/null 2>&1
}
trap cleanup EXIT
if [ "${bundle_only}" = "yes" ]; then
  patch_note="Không ghi patch: lệnh đẩy bị môi trường từ chối (--bundle-only), không đẩy lại dưới dạng khác (auto-run.md, Khi không đẩy được)."
elif [ "${patch_handoff}" = "no" ]; then
  patch_note="Không ghi patch: chỉ ghi khi GitHub từ chối vì quyền workflow (L-086); lần từ chối này khác."
else
  report_dir="${SAVE_BRANCH_REPORT_DIR:-}"
  many_reports=no
  if [ -z "${report_dir}" ]; then
    found="$(git worktree list --porcelain | awk '
      /^worktree / { dir = substr($0, 10) }
      /^branch refs\/heads\/claude\/RUN-[0-9]+-[0-9]+-report$/ { print dir }')"
    if [ "$(grep -c . <<< "${found}")" -gt 1 ]; then
      many_reports=yes
    else
      report_dir="${found}"
    fi
  fi
  if [ -z "${report_dir}" ] && [ "${many_reports}" = "no" ]; then
    local_reports="$(git for-each-ref --format='%(refname:short)' 'refs/heads/claude/RUN-*-report')"
    if [ "$(grep -c . <<< "${local_reports}")" -gt 1 ]; then
      many_reports=yes
    elif [ -n "${local_reports}" ]; then
      temp_report="$(mktemp -d)/report"
      git worktree add -q "${temp_report}" "${local_reports}" > /dev/null 2>&1 && report_dir="${temp_report}"
    fi
  fi
  report_branch=""
  if [ -n "${report_dir}" ]; then
    report_branch="$(git -C "${report_dir}" symbolic-ref -q --short HEAD 2> /dev/null || true)"
  fi
  if [ "${many_reports}" = "yes" ]; then
    patch_note="Có nhiều nhánh báo cáo (claude/RUN-...-report) trên máy này, không biết ghi vào nhánh nào; đặt SAVE_BRANCH_REPORT_DIR. Chỉ có bundle."
  elif [[ ! "${report_branch}" =~ ^claude/(RUN-[0-9]{8}-[0-9]{2})-report$ ]]; then
    patch_note="Không có nhánh báo cáo (claude/RUN-...-report) trên máy này, nên không ghi patch; chỉ có bundle."
  elif [ "${branch}" = "${report_branch}" ]; then
    patch_note="Nhánh chưa đẩy là chính nhánh báo cáo: không ghi patch lên nó; chỉ có bundle."
  else
    run_id="${BASH_REMATCH[1]}"
    base="$(git merge-base origin/main "${branch}")"
    work_dir="$(mktemp -d)"
    mode="$(make_patch "${work_dir}")"
    max_bytes="${SAVE_BRANCH_PATCH_MAX_BYTES:-1048576}"
    script_dir="$(cd "$(dirname "$0")" && pwd)"
    if [ "${mode}" = "noworktree" ]; then
      patch_note="Không mở được worktree tạm để kiểm patch; chỉ có bundle."
    elif [ -z "${mode}" ]; then
      patch_note="Không dựng được patch áp lại đúng nhánh (git format-patch hay git diff, kiểm trong worktree tạm); chỉ có bundle."
    else
      rel="docs/runs/patches/${run_id}/${branch//\//__}.${mode}"
      patch_size="$(wc -c < "${work_dir}/patch" | tr -d ' ')"
      mkdir -p "${work_dir}/scan" && mv "${work_dir}/patch" "${work_dir}/scan/patch"
      if [ "${patch_size}" -gt "${max_bytes}" ]; then
        patch_note="Patch ${patch_size} byte, lớn hơn giới hạn ${max_bytes} byte (SAVE_BRANCH_PATCH_MAX_BYTES), nên không ghi lên nhánh báo cáo; chỉ có bundle."
      elif ! python3 -I -c 'import sys; open(sys.argv[1], encoding="utf-8").read()' "${work_dir}/scan/patch" 2> /dev/null; then
        patch_note="Patch có byte không phải UTF-8 nên scripts/secret-scan.py không đọc được; không ghi lên nhánh báo cáo, chỉ có bundle. Người xem lại nhánh trước khi đẩy."
      elif ! SECRET_SCAN_ROOT="${work_dir}/scan" python3 -I "${script_dir}/secret-scan.py" > /dev/null 2>&1; then
        patch_note="scripts/secret-scan.py thấy chuỗi giống bí mật trong patch, nên không ghi lên nhánh báo cáo; chỉ có bundle. Người xem lại nhánh trước khi đẩy."
      elif git -C "${report_dir}" fetch -q origin "${report_branch}" > /dev/null 2>&1 \
          && ! git -C "${report_dir}" merge-base --is-ancestor FETCH_HEAD HEAD > /dev/null 2>&1; then
        patch_note="Nhánh báo cáo trên máy này và trên GitHub đã khác nhau (có commit không có ở bên kia): không ghi patch, để phiên gộp nhánh báo cáo trước; chỉ có bundle."
      else
        target="${report_dir}/${rel}"
        committed=yes
        was_tracked=no
        git -C "${report_dir}" ls-files --error-unmatch -- "${rel}" > /dev/null 2>&1 && was_tracked=yes
        if ! cmp -s "${work_dir}/scan/patch" "${target}" 2> /dev/null || ! git -C "${report_dir}" ls-files --error-unmatch -- "${rel}" > /dev/null 2>&1; then
          mkdir -p "$(dirname "${target}")"
          cp "${work_dir}/scan/patch" "${target}"
          git -C "${report_dir}" add -- "${rel}"
          if ! git -C "${report_dir}" commit -q -m "${run_id}: patch của nhánh chưa đẩy ${branch} (${short_sha})" -- "${rel}" > /dev/null 2>&1; then
            git -C "${report_dir}" reset -q -- "${rel}" > /dev/null 2>&1
            if [ "${was_tracked}" = "yes" ]; then
              git -C "${report_dir}" checkout -q -- "${rel}" > /dev/null 2>&1
            else
              rm -f "${target}"
            fi
            committed=no
          fi
        fi
        if [ "${committed}" = "no" ]; then
          patch_note="Không commit được patch trên nhánh báo cáo (${report_dir}); chỉ có bundle."
        elif ! push_err="$(git -C "${report_dir}" push -q origin "HEAD:${report_branch}" 2>&1)"; then
          echo "${push_err}" >&2
          patch_note="Đã commit patch \`${rel}\` trên nhánh báo cáo, nhưng nhánh báo cáo cũng không đẩy được: patch chỉ có trên máy này, như bundle."
        else
          if [ "${mode}" = mbox ]; then
            apply_cmd="git show origin/${report_branch}:${rel} | git am"
          else
            apply_cmd="git show origin/${report_branch}:${rel} | git apply --index && git commit -m \"${branch}: ${count} commit gộp từ patch\""
          fi
          patch_lines="- Patch trên nhánh báo cáo, để người xem rồi tự quyết áp hay không: \`${rel}\` (${patch_size} byte, gốc \`$(git rev-parse --short=7 "${base}")\`). Lấy lại không cần máy cloud:

\`\`\`
git fetch origin ${report_branch} main
git switch -c ${branch} $(git rev-parse --short=12 "${base}")
${apply_cmd}
git push -u origin ${branch}
\`\`\`

  Sau khi PR báo cáo đã merge, thay \`origin/${report_branch}\` bằng \`origin/main\` trong lệnh \`git show\`."
        fi
      fi
    fi
  fi
fi
cat << EOF

### Nhánh chưa đẩy

- Nhánh \`${branch}\`, commit đầu nhánh \`${short_sha}\`, ${count} commit, bundle \`${bundle}\` (${size} byte).
- Người lấy lại, trong một bản sao có nhánh chính, trước khi máy cloud bị thu hồi:

\`\`\`
git fetch ${bundle} ${branch}:${branch}
git push -u origin ${branch}
\`\`\`
EOF
if [ -n "${patch_lines}" ]; then
  printf '%s\n' "${patch_lines}"
else
  echo "- ${patch_note}"
fi
exit 3
