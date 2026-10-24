#!/usr/bin/env bash
# Tells what made scripts/check.sh fail, from its log (ADR-1002, point 1).
#
# Usage:   scripts/failure-kind.sh <build.log>
# Prints:  tests  every failed Gradle task is a test task (*UnitTest, verifyRoborazzi*) and the log
#                 says "There were failing tests": main-guard.yml runs check.sh once more;
#          other  anything else, including a log without a Gradle failure (a rule of check.sh, a script
#                 test, an error before Gradle) or a missing log: main-guard.yml treats it as red.
# Always exits 0, so the workflow reads the answer instead of failing on this step.
#
# Usage:   scripts/failure-kind.sh --playbook <log> [<table>]
# Prints:  one line "PB-NN <kind>: <what to do> (<source>)" for the first row of docs/ops/failure-playbook.md
#          (or <table>) whose pattern matches the log, or "khong-co: ..." when no row matches (T-039, L-057).
#          A pattern grep -E rejects is skipped and reported as "LỖI BẢNG" on stderr. Always exits 0.
# Changing this file changes the quality gate: high-risk plan, cross-review required.

set -uo pipefail

if [ "${1:-}" = "--playbook" ]; then
  playbook_log="${2:-}"
  table="${3:-$(dirname "$0")/../docs/ops/failure-playbook.md}"
  if [ -z "${playbook_log}" ] || [ ! -f "${playbook_log}" ]; then
    echo "khong-co: không có log để tra bảng (${playbook_log:-không đưa đường dẫn}). Xử lý như lỗi thật (auto-run.md bước 8, tối đa 3 lần sửa)."
    exit 0
  fi
  # Markdown cells are split at every "|" not preceded by a backslash; "\|" inside a pattern becomes "|".
  python3 -I - "${table}" "${playbook_log}" << 'PY'
import re, subprocess, sys
table, log = sys.argv[1], sys.argv[2]
inside = False
for line in open(table, encoding="utf-8"):
    if "<!-- playbook:start -->" in line:
        inside = True
        continue
    if "<!-- playbook:end -->" in line:
        break
    if not inside or not re.match(r"\| PB-\d+ ", line):
        continue
    cells = [c.strip() for c in re.split(r"(?<!\\)\|", line.strip().strip("|"))]
    if len(cells) < 6:
        continue
    code, pattern, _, kind, action, source = cells[:6]
    pattern = pattern.strip("`").replace("\\|", "|")
    kind = kind.strip("`")
    result = subprocess.run(["grep", "-Eq", "--", pattern, log], capture_output=True)
    if result.returncode == 2:
        print(f"LỖI BẢNG: {code}: mẫu không hợp lệ với grep -E: {pattern}", file=sys.stderr)
    elif result.returncode == 0:
        print(f"{code} {kind}: {action} ({source})")
        sys.exit(0)
print("khong-co: lỗi chưa có trong docs/ops/failure-playbook.md. Đọc log, xử lý như lỗi thật (auto-run.md bước 8, tối đa 3 lần sửa).")
PY
  exit 0
fi

log="${1:-}"
if [ -z "${log}" ] || [ ! -f "${log}" ]; then
  echo other
  exit 0
fi

# ':feature:camera:testDebugUnitTest' -> testDebugUnitTest, one per failed task.
tasks="$(sed -nE "s/^Execution failed for task '([^']+)'\.$/\1/p" "${log}" | sed -E 's/.*://' | sort -u)"
if [ -z "${tasks}" ] || ! grep -q 'There were failing tests' "${log}"; then
  echo other
  exit 0
fi

while IFS= read -r task; do
  case "${task}" in
    *UnitTest | verifyRoborazzi*) ;;
    *) echo other; exit 0 ;;
  esac
done <<< "${tasks}"

# Each failed task must have failed because of its tests, not for another reason (a test JVM that did not start).
failed_count="$(grep -c "^Execution failed for task '" "${log}")"
tests_count="$(grep -A1 "^Execution failed for task '" "${log}" | grep -c '^> There were failing tests')"
if [ "${tests_count}" -lt "${failed_count}" ]; then
  echo other
  exit 0
fi
echo tests
