#!/usr/bin/env bash
# JVM quality gate (ADR-0010, ADR-0011). Shared by:
#   - the AI at step 5 of an auto-run session (local check),
#   - the build-and-test job of .github/workflows/pr-check.yml,
#   - .github/workflows/main-guard.yml.
# Emulator tests are not here; they run in the ui-test job of CI.
#
# Screenshot tests are handled per image file. By convention every screenshot test passes an
# explicit path: captureRoboImage("src/test/screenshots/<Name>.png"). From those paths:
#   - every expected image exists: verify them all;
#   - some images are missing:
#       in CI   -> fail (reference images must be committed with the PR);
#       locally -> record the module, then restore the existing reference images byte for byte,
#                  so only the missing images are added and existing ones are never overwritten.
#
# Everything is checked in ONE Gradle invocation, so unit tests run only once
# (the Roborazzi task reuses that test run).
#
# Changing this file changes the quality gate: high-risk plan, cross-review required.

set -euo pipefail
cd "$(dirname "$0")/.."

in_ci() { [ -n "${GITHUB_ACTIONS:-}" ]; }
GRADLE=(./gradlew --no-daemon --console=plain)

# A Java that Gradle cannot run on stops Gradle with an unreadable error; say which Java to use instead.
echo "== Java"
scripts/java-check.sh

echo "== Script tests"
if command -v python3 > /dev/null && command -v jq > /dev/null; then
  scripts/tests/run.sh
elif in_ci; then
  echo "::error::CI thiếu python3 hoặc jq để chạy scripts/tests/run.sh."
  exit 1
else
  echo "CHÚ Ý: thiếu python3 hoặc jq, bỏ qua test của script (CI vẫn chạy)."
fi

screenshot_tasks=()
added=()
missing=()
# :app has product flavors, so its Roborazzi tasks carry the staging variant (recordRoborazziStagingDebug).
for dir in app/ core/*/ feature/*/; do
  dir="${dir%/}"
  variant="Debug"; [ "${dir}" = "app" ] && variant="StagingDebug"
  [ -f "${dir}/build.gradle.kts" ] || continue
  # Test source sets: src/test, and variant ones such as src/testStaging of :app (staging-only screens).
  test_dirs=()
  for test_dir in "${dir}"/src/test "${dir}"/src/test[A-Z]*; do [ -d "${test_dir}" ] && test_dirs+=("${test_dir}"); done
  [ "${#test_dirs[@]}" -gt 0 ] || continue
  # grep exits 1 when nothing matches; under pipefail that must not stop the script.
  calls="$({ grep -rho 'captureRoboImage(' "${test_dirs[@]}" || true; } | wc -l | tr -d ' ')"
  [ "${calls}" -gt 0 ] || continue
  module=":${dir//\//:}"

  expected="$({ grep -rhoE 'captureRoboImage\("src/test/screenshots/[^"]+\.png"' "${test_dirs[@]}" || true; } \
    | sed -E 's/^captureRoboImage\("//; s/"$//' | sort -u)"
  named="$({ grep -rhoE 'captureRoboImage\("src/test/screenshots/[^"]+\.png"' "${test_dirs[@]}" || true; } | wc -l | tr -d ' ')"
  if [ "${named}" -ne "${calls}" ]; then
    echo "::error::${module}: mọi captureRoboImage phải ghi rõ đường dẫn \"src/test/screenshots/<Tên>.png\" (ADR-0011)."
    exit 1
  fi

  module_missing=()
  while IFS= read -r image; do
    [ -n "${image}" ] || continue
    [ -f "${dir}/${image}" ] || module_missing+=("${dir}/${image}")
  done <<< "${expected}"

  if [ "${#module_missing[@]}" -eq 0 ]; then
    screenshot_tasks+=("${module}:verifyRoborazzi${variant}")
  elif in_ci; then
    missing+=("${module_missing[@]}")
  else
    # Keep a copy of the existing reference images, record the module, then put the copies back.
    backup="$(mktemp -d)"
    cp -p "${dir}"/src/test/screenshots/*.png "${backup}/" 2> /dev/null || true
    "${GRADLE[@]}" "${module}:recordRoborazzi${variant}"
    cp -p "${backup}"/*.png "${dir}/src/test/screenshots/" 2> /dev/null || true
    rm -rf "${backup}"
    added+=("${module_missing[@]}")
    screenshot_tasks+=("${module}:verifyRoborazzi${variant}")
  fi
done

if [ "${#missing[@]}" -gt 0 ]; then
  for image in "${missing[@]}"; do
    echo "::error::Thiếu ảnh chuẩn ${image}. Chạy scripts/check.sh trên máy để ghi ảnh mới, xem ảnh và commit cùng PR."
  done
  exit 1
fi

echo "== Quality gate: module rules, ktlint, unit tests, screenshots, lint, staging build"
"${GRADLE[@]}" --continue \
  checkModuleDependencies \
  ktlintCheck \
  testDebugUnitTest \
  testStagingDebugUnitTest \
  ${screenshot_tasks[@]+"${screenshot_tasks[@]}"} \
  :app:lintStagingDebug \
  :app:assembleStagingDebug

scripts/test-summary.sh jvm

if [ "${#added[@]}" -gt 0 ]; then
  echo "CHÚ Ý: đã ghi ảnh chuẩn mới: ${added[*]}. Xem từng ảnh và commit cùng thay đổi."
fi
echo "== Cổng chất lượng: QUA"
