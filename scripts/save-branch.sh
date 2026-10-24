#!/usr/bin/env bash
# Pushes a task branch so it does not live only on the temporary cloud machine, and bundles it when the push
# fails (plan T-037, L-051).
#
# Usage:   scripts/save-branch.sh <branch>                push (retrying network errors); bundle if that fails
#          scripts/save-branch.sh --bundle-only <branch>  bundle only, no push (the environment refused the push call)
# Exit:    0 pushed. 3 not pushed (with a bundle when the branch has commits beyond origin/main). 2 bad arguments.
# Env:     SAVE_BRANCH_DIR            where bundles go (default $HOME/rescue, outside any worktree)
#          SAVE_BRANCH_RETRY_SECONDS  waits between retries of a network error (default "60 120 240")
# Never retries a push the environment refused: that call never reaches this script, so the session itself must
# not push again in another form (auto-run.md, "Khi không đẩy được").
# Runs in the current repository, so it works from a worktree.
# Messages are in Vietnamese for the team; comments are in English.

set -uo pipefail

bundle_only=no
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
cat << EOF

### Nhánh chưa đẩy

- Nhánh \`${branch}\`, commit đầu nhánh \`${short_sha}\`, ${count} commit, bundle \`${bundle}\` (${size} byte).
- Người lấy lại, trong một bản sao có nhánh chính, trước khi máy cloud bị thu hồi:

\`\`\`
git fetch ${bundle} ${branch}:${branch}
git push -u origin ${branch}
\`\`\`
EOF
exit 3
