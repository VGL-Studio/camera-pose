#!/usr/bin/env bash
# Tells what made scripts/check.sh fail, from its log (ADR-1002, point 1).
#
# Usage:   scripts/failure-kind.sh <build.log>
# Prints:  tests  every failed Gradle task is a test task (*UnitTest, verifyRoborazzi*) and the log
#                 says "There were failing tests": main-guard.yml runs check.sh once more;
#          other  anything else, including a log without a Gradle failure (a rule of check.sh, a script
#                 test, an error before Gradle) or a missing log: main-guard.yml treats it as red.
# Always exits 0, so the workflow reads the answer instead of failing on this step.
# Changing this file changes the quality gate: high-risk plan, cross-review required.

set -uo pipefail

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
