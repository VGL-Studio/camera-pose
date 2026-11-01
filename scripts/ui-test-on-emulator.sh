#!/usr/bin/env bash
# Runs the emulator tests of the app for the ui-test job of main-guard.yml (ADR-0043), inside
# reactivecircus/android-emulator-runner (its script: line). Run from the root of the repository.
#
# One run: clear the device log, record it in the background (ADR-0019), run :app:connectedStagingDebugAndroidTest
# with its output in a log, stop the device log.
#   - first run green: exit 0 with ui-test.log and logcat.txt.
#   - first run red: keep its files as ui-test-first.log, logcat-first.txt and ui-test-first-results/ (a copy of the
#     test results), then run once more on the same emulator (as ADR-0025 point 1 does for check.sh) and exit with the
#     code of that second run, which writes ui-test.log and logcat.txt again.
# scripts/ui-test-verdict.sh reads these files afterwards.
#
# UI_TEST_ADB and UI_TEST_GRADLEW replace adb and ./gradlew (the tests use fakes).
# Changing this file changes the quality gate: high-risk plan, cross-review required.

# No -e: the code of the first run decides whether to run again.
set -uo pipefail

ADB="${UI_TEST_ADB:-adb}"
GRADLEW="${UI_TEST_GRADLEW:-./gradlew}"
RESULTS_DIR="app/build/outputs/androidTest-results"

run_once() { # <Gradle log> <device log>; returns the exit code of Gradle
  local log="$1" device_log="$2" logcat_pid status
  "$ADB" logcat -c
  "$ADB" logcat -v threadtime > "$device_log" 2>&1 &
  logcat_pid=$!
  "$GRADLEW" --no-daemon --console=plain :app:connectedStagingDebugAndroidTest 2>&1 | tee "$log"
  status=$?
  kill "$logcat_pid" 2> /dev/null
  wait "$logcat_pid" 2> /dev/null
  return "$status"
}

rm -rf ui-test-first.log logcat-first.txt ui-test-first-results

if run_once ui-test.log logcat.txt; then
  exit 0
fi

mv ui-test.log ui-test-first.log
mv logcat.txt logcat-first.txt
if [[ -d "$RESULTS_DIR" ]]; then
  cp -R "$RESULTS_DIR" ui-test-first-results
fi
echo "Lần đầu đỏ, chạy lại một lần trên cùng emulator (ADR-0043)."
run_once ui-test.log logcat.txt
exit $?
