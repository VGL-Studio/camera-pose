#!/usr/bin/env bash
# Says whether the ui-test job of main-guard.yml runs the emulator tests for a commit of main (ADR-0043).
#
# Usage:  scripts/ui-test-needed.sh <sha> [<event>]   (main-guard.yml passes GITHUB_SHA and GITHUB_EVENT_NAME)
# Prints exactly one word (yes or no) on stdout and the reason on stderr. Always exits 0: anything it cannot read gives
# yes, so a doubt costs emulator minutes, never a missed check.
#
# yes when:
#   - the event is not push (a run by hand always runs the emulator);
#   - the changes of the commit against its parent cannot be read (unknown commit, no parent in the fetch depth);
#   - the commit changes a file under UI_PATHS: code that runs in the app or changes how it is built, the scripts the
#     ui-test job runs, and main-guard.yml itself (the former ui filter of pr-check.yml).
# The diff lists both the old and the new path of a renamed file (--no-renames): code moved into docs/ is still code.
# Adding a module directory or a script the ui-test job runs means adding it to UI_PATHS and to its test.
#
# Changing this file changes the quality gate: high-risk plan, cross-review required.

set -euo pipefail

# A directory ends with "/" (every file below it); "*.gradle.kts" is a build file at the root only; anything else is
# one exact path.
UI_PATHS=(app/ core/ feature/ build-logic/ gradle/ '*.gradle.kts' gradle.properties gradlew scripts/test-summary.sh
  scripts/ui-test-on-emulator.sh scripts/ui-test-verdict.sh scripts/ui-test-needed.sh .github/workflows/main-guard.yml)

answer() { # <yes|no> <reason>
  echo "$2" >&2
  echo "$1"
  exit 0
}

is_ui_path() { # <path>: true when the path is under UI_PATHS
  local path="$1" entry
  for entry in "${UI_PATHS[@]}"; do
    case "$entry" in
      */)
        case "$path" in "$entry"*) return 0 ;; esac
        ;;
      '*.gradle.kts')
        case "$path" in */*) ;; *.gradle.kts) return 0 ;; esac
        ;;
      *)
        [[ "$path" == "$entry" ]] && return 0
        ;;
    esac
  done
  return 1
}

[[ $# -ge 1 && $# -le 2 && -n "$1" ]] || answer yes "CHẠY EMULATOR: cách dùng: scripts/ui-test-needed.sh <sha> [<sự kiện>]."
SHA="$1"
EVENT="${2:-push}"

[[ "$EVENT" == "push" ]] || answer yes "CHẠY EMULATOR: chạy tay (sự kiện $EVENT)."

changed="$(git -c core.quotePath=false diff --no-renames --name-only "$SHA^" "$SHA" 2> /dev/null)" \
  || answer yes "CHẠY EMULATOR: không đọc được thay đổi của ${SHA:0:7} so với commit cha."

while IFS= read -r path; do
  [[ -n "$path" ]] || continue
  if is_ui_path "$path"; then
    answer yes "CHẠY EMULATOR: commit ${SHA:0:7} đổi $path (code của app hay của job ui-test)."
  fi
done <<< "$changed"

answer no "BỎ QUA EMULATOR: commit ${SHA:0:7} không đổi code của app hay của job ui-test."
