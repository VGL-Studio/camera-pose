#!/usr/bin/env bash
# Checks that a self-hosted runner is set up as docs/ops/self-hosted-runner.md says, before anything is set up or
# built (ADR-0042). The heavy jobs of pr-check.yml and main-guard.yml run it only on a self-hosted runner, in the step
# right after checkout (if: runner.environment == 'self-hosted').
# It catches configuration mistakes of the machine or of the repository. It is not a safety fence: a PR can change
# this very script. The fences are the GitHub settings and the one-shot container (docs/ops/self-hosted-runner.md).
#
# Usage:  scripts/runner-check.sh [--emulator]
#   --emulator   also checks KVM and ANDROID_HOME (the ui-test job)
# Checks every point, not only up to the first problem:
#   1. REPO_VISIBILITY (the workflow passes github.event.repository.visibility) is private or internal;
#   2. VGL_RUNNER_EPHEMERAL is 1, the mark of a container built as the doc says;
#   3. the Gradle home (GRADLE_USER_HOME, else ~/.gradle) has no init.d, init.gradle, init.gradle.kts or
#      gradle.properties (setup-gradle writes its own init scripts there later);
#   4. the work directory is a git repository with no file outside git, ignored files included;
#   5. the work directory has at least RUNNER_CHECK_MIN_FREE_GB GB free (default 20, 30 with --emulator);
#   6. with --emulator: KVM (RUNNER_CHECK_KVM, default /dev/kvm) can be opened to read and write, and ANDROID_HOME
#      is set and writable (the emulator action installs packages there).
# Exit codes: 0 usable (one line RUNNER DÙNG ĐƯỢC), 1 not usable (one line RUNNER KHÔNG DÙNG ĐƯỢC per problem),
#             2 wrong arguments.
# RUNNER_CHECK_LOG, when set, is a file the result lines are also appended to, after every check has run, so a file in
# the work directory (pr-check.yml passes build.log, which its failure comment prints) does not make the script refuse
# its own directory. A file that cannot be written gives a warning on stderr and keeps the exit code. A usage error
# (exit 2) writes nothing.
# RUNNER_CHECK_MIN_FREE_GB and RUNNER_CHECK_KVM exist only for the tests to override. No network, no secret.
# Messages are in Vietnamese for the team; comments are in English.
#
# Changing this file changes the quality gate: high-risk plan, cross-review required.

set -euo pipefail

emulator=0
if [ "$#" -eq 1 ] && [ "$1" = "--emulator" ]; then
  emulator=1
elif [ "$#" -ne 0 ]; then
  echo "LỖI: cách dùng: scripts/runner-check.sh [--emulator]" >&2
  exit 2
fi

problems=0
report=""
say() { # prints one result line and keeps it for RUNNER_CHECK_LOG
  echo "$*"
  report="$report$*"$'\n'
}
refuse() {
  say "RUNNER KHÔNG DÙNG ĐƯỢC: $* (docs/ops/self-hosted-runner.md)."
  problems=$((problems + 1))
}
finish() { # <exit code>; appends the result lines to RUNNER_CHECK_LOG when it is set, then exits
  if [ -n "${RUNNER_CHECK_LOG:-}" ]; then
    if ! printf '%s' "$report" >> "$RUNNER_CHECK_LOG"; then
      echo "CHÚ Ý: không ghi được $RUNNER_CHECK_LOG." >&2
    fi
  fi
  exit "$1"
}

visibility="${REPO_VISIBILITY:-}"
case "$visibility" in
  private | internal) ;;
  *) refuse "repo không phải private (REPO_VISIBILITY='$visibility'); runner tự host chỉ dành cho repo private" ;;
esac

[ "${VGL_RUNNER_EPHEMERAL:-}" = "1" ] || refuse "container không có dấu VGL_RUNNER_EPHEMERAL=1"

gradle_home="${GRADLE_USER_HOME:-$HOME/.gradle}"
for item in init.d init.gradle init.gradle.kts gradle.properties; do
  if [ -e "$gradle_home/$item" ]; then refuse "$gradle_home/$item có từ trước"; fi
done

if status="$(git status --porcelain --ignored 2> /dev/null)"; then
  if [ -n "$status" ]; then
    first="$(head -n1 <<< "$status")"
    refuse "thư mục làm việc có file ngoài git: ${first:3}"
  fi
else
  refuse "thư mục làm việc không phải repo git"
fi

if [ "$emulator" = 1 ]; then min_free="${RUNNER_CHECK_MIN_FREE_GB:-30}"; else min_free="${RUNNER_CHECK_MIN_FREE_GB:-20}"; fi
free_kb="$(df -Pk . | awk 'NR == 2 { print $4 }')"
free_gb=$((${free_kb:-0} / 1048576))
[ "$free_gb" -ge "$min_free" ] || refuse "đĩa trống $free_gb GB, cần ít nhất $min_free GB"

if [ "$emulator" = 1 ]; then
  kvm="${RUNNER_CHECK_KVM:-/dev/kvm}"
  [ -r "$kvm" ] && [ -w "$kvm" ] || refuse "$kvm không mở được để đọc và ghi"
  [ -n "${ANDROID_HOME:-}" ] && [ -w "$ANDROID_HOME" ] || refuse "ANDROID_HOME trống hay không ghi được"
fi

[ "$problems" = 0 ] || finish 1
if [ "$emulator" = 1 ]; then
  say "RUNNER DÙNG ĐƯỢC: repo $visibility, đĩa trống $free_gb GB, KVM và ANDROID_HOME dùng được."
else
  say "RUNNER DÙNG ĐƯỢC: repo $visibility, đĩa trống $free_gb GB."
fi
finish 0
