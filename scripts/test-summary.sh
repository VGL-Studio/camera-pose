#!/usr/bin/env bash
# Summarises JUnit XML results. In CI it also prints a ::notice:: annotation, which can be read
# through the check-runs API without downloading logs.
#
# Usage: scripts/test-summary.sh jvm        # JVM unit/Robolectric/screenshot tests
#        scripts/test-summary.sh android    # emulator tests; exits 1 when no test ran
set -euo pipefail
cd "$(dirname "$0")/.."

# Worktrees nested in the repository (agent worktrees under .claude/worktrees/, L-137) keep their own
# build results and screenshots; counting them would report another checkout's tests as this one's.
skip=(-not -path './.claude/*')
root="$(pwd -P)" # git prints worktree paths with symlinks resolved (/tmp is /private/tmp on macOS)
while IFS= read -r worktree; do
  case "${worktree}" in
    "${root}"/*) skip+=(-not -path "./${worktree#"${root}"/}/*") ;;
  esac
done < <(git worktree list --porcelain 2>/dev/null | sed -n 's/^worktree //p')

mode="${1:-jvm}"
case "${mode}" in
  jvm) title="Test JVM"; dirs="$(find . -type d -path '*/build/test-results/test*UnitTest' -not -path './build-logic/*' "${skip[@]}" | sort)" ;;
  android) title="Test emulator"; dirs="$(find . -type d -path '*/build/outputs/androidTest-results' "${skip[@]}" | sort)" ;;
  *) echo "cách dùng: scripts/test-summary.sh jvm|android" >&2; exit 1 ;;
esac

# First numeric value of attribute $1 in file $2; 0 when the attribute is missing.
attr() {
  local v
  v="$(grep -o "$1=\"[0-9]*\"" "$2" | head -1 | tr -dc 0-9 || true)"
  echo "${v:-0}"
}

total=0
failed=0
lines=()
while IFS= read -r dir; do
  [ -n "${dir}" ] || continue
  module="${dir%%/build/*}"
  t=0
  f=0
  while IFS= read -r -d '' xml; do
    grep -q '<testsuite' "${xml}" || continue
    t=$((t + $(attr tests "${xml}")))
    f=$((f + $(attr failures "${xml}") + $(attr errors "${xml}")))
  done < <(find "${dir}" -name '*.xml' -print0)
  total=$((total + t))
  failed=$((failed + f))
  lines+=("${module}: ${t} test, ${f} lỗi")
done <<< "${dirs}"

summary="${title}: ${total} test, ${failed} lỗi."
if [ "${mode}" = "jvm" ]; then
  shots="$(find . -path '*/src/test/screenshots/*.png' -not -path '*/build/*' "${skip[@]}" | wc -l | tr -d ' ')"
  summary="${summary} Ảnh screenshot: ${shots}."
fi
summary="${summary} $(IFS=';'; echo "${lines[*]:-không có kết quả}")"

echo "${summary}"
if [ -n "${GITHUB_ACTIONS:-}" ]; then
  echo "::notice title=${title}::${summary}"
fi

if [ "${mode}" = "android" ] && [ "${total}" -eq 0 ]; then
  echo "::error::Không có test nào chạy trên emulator."
  exit 1
fi
