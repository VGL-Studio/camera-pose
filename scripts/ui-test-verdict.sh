#!/usr/bin/env bash
# Concludes the ui-test job of main-guard.yml from the files the emulator step and the summary step leave (ADR-0043).
#
# Usage:  scripts/ui-test-verdict.sh <outcome of the emulator step> [<directory>]   (default: the current directory)
# Prints exactly one word on stdout and the reason on stderr. Always exits 0.
#   green  the last run is green: the first run was green, or red only for infrastructure or for no reason found
#   flaky  the first run failed for real (a test failed, the app crashed), the run again was green
#   infra  not a failure of the code: main-broken is not opened (a person or the session runs the job again)
#   red    a real failure: main-broken is opened
# For infra it also writes ui-verdict.txt: a first line "EMULATOR HẠ TẦNG (<PB-NN or label>): <reason>...", then for
# each pattern of that playbook row the first log line that matches it. The check step prints the file, so that
# `gh run view --log-failed` of the job holds the lines scripts/failure-kind.sh --playbook looks for, and its annotation
# starts with "EMULATOR HẠ TẦNG (", which scripts/find-breaking-commit.sh --check ui-test does not blame.
# The prefix is not "HẠ TẦNG:", the pattern of PB-02 (GitHub did not start the job).
#
# Files: ui-test.log and logcat.txt (the last run), ui-test-first.log and logcat-first.txt (a red first run, kept by
# scripts/ui-test-on-emulator.sh), ui-summary.log (scripts/test-summary.sh android, about the last run only).
# The log of one run is its Gradle log, plus ui-summary.log for the last run, plus the VGL and AndroidRuntime lines of
# its device log (ADR-0019); it is looked up in docs/ops/failure-playbook.md with scripts/failure-kind.sh --playbook.
#
# Outcome success:  0 tests (or no summary) is infra (the summary step never fails the job, so this must show);
#                   no first run log is green; a real first failure is flaky; anything else green.
# Outcome failure, first true rule wins:
#   1. no ui-test.log: red when the commit changes the job's own scripts or main-guard.yml (the job itself is
#      broken), else infra (the emulator did not start, the script did not run: PB-15 usually ends here);
#   2. the first run failed for real and its log matches no row of INFRA_PB: red (a rerun failing on infrastructure
#      does not hide it);
#   3. the log of the last run has CRASH_RE: red, even with 0 tests (the app stops at start, L-051);
#   4. 0 tests: infra when the log matches a row of INFRA_PB or DEVICE_RE, else red;
#   5. some tests: infra when the log matches a row of INFRA_PB, else red.
# Any other outcome (cancelled, skipped, empty) is infra.
#
# Changing this file changes the quality gate: high-risk plan, cross-review required.

set -uo pipefail

# Rows of docs/ops/failure-playbook.md that are failures not caused by the code of the ui-test job. Any other row,
# even one of kind lich-thu-lai such as PB-09, is red.
INFRA_PB=(PB-01 PB-11 PB-13 PB-15)
# The emulator or the device is broken.
DEVICE_RE='No connected devices|device offline|could not connect to TCP port'
# A test that failed, in the Gradle log once its colour codes are removed: AGP prints
# "<class> > <test>[<device>] \e[31mFAILED \e[0m". Lines with "> Task " are failed Gradle tasks, not tests.
TEST_FAILED_RE=' > .+ FAILED[[:space:]]*$'
# The summary line AGP prints for the device when some tests failed.
TESTS_FAILED_RE='Tests on .* failed: There (was|were) [0-9]+ failure'
# The process of the app stopped, in the Gradle log. The device log adds a line "AndroidRuntime: Process: <id>" for the
# applicationId read from app/build.gradle.kts, so that a crash of a system process (gms) does not count.
GRADLE_CRASH_RE='Instrumentation run failed|Process crashed'
# The device log lines kept in the log of a run (ADR-0019).
DEVICE_LOG_RE=' (VGL[A-Za-z0-9_.]*|AndroidRuntime) *:'

OUTCOME="${1:-}"
DIR="${2:-.}"
SCRIPTS="$(cd "$(dirname "$0")" && pwd)"
PLAYBOOK="$SCRIPTS/../docs/ops/failure-playbook.md"
VERDICT_FILE="$DIR/ui-verdict.txt"

tmp="$(mktemp -d "${TMPDIR:-/tmp}/ui-verdict.XXXXXX")"
trap 'rm -rf "$tmp"' EXIT
rm -f "$VERDICT_FILE"

app_id="$(sed -n -E 's/^[[:space:]]*applicationId[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$DIR/app/build.gradle.kts" \
  2> /dev/null | head -n 1)"
if [[ -n "$app_id" ]]; then
  CRASH_RE="$GRADLE_CRASH_RE|AndroidRuntime: Process: ${app_id//./\\.}[.,]"
else
  echo "CẢNH BÁO: không đọc được applicationId trong app/build.gradle.kts; chỉ dùng dòng dừng app của Gradle." >&2
  CRASH_RE="$GRADLE_CRASH_RE"
fi

say() { # <verdict> <reason>
  echo "KẾT LUẬN $1: $2" >&2
  echo "$1"
  exit 0
}

# Writes ui-verdict.txt and concludes infra.
infra() { # <PB-NN or label> <reason> [<log whose matching lines are shown>]
  local label="$1" reason="$2" log="${3:-}" action pattern line
  if [[ "$label" == PB-* ]]; then
    action="Chạy lại job đỏ theo dòng $label của docs/ops/failure-playbook.md (ADR-0043)."
  else
    action="Chạy lại riêng job đỏ theo lịch của PB-15, không revert (docs/auto-run.md bước 9, ADR-0043)."
  fi
  {
    echo "EMULATOR HẠ TẦNG ($label): $reason; không mở main-broken. $action"
    if [[ -n "$log" && -f "$log" ]]; then
      if [[ "$label" == PB-* ]]; then
        while IFS= read -r pattern; do
          [[ -n "$pattern" ]] || continue
          line="$(grep -m1 -E -- "$pattern" "$log" 2> /dev/null)"
          [[ -z "$line" ]] || echo "$line"
        done < <(row_patterns "$label")
      elif [[ "$label" == "thiết bị" ]]; then
        line="$(grep -m1 -E -- "$DEVICE_RE" "$log" 2> /dev/null)"
        [[ -z "$line" ]] || echo "$line"
      fi
    fi
  } > "$VERDICT_FILE"
  say infra "$reason"
}

row_patterns() { # <PB-NN>: the patterns of that row of the playbook, one per line ("\|" in the table is "|")
  python3 -I - "$PLAYBOOK" "$1" << 'PY'
import re, sys
table, code = sys.argv[1], sys.argv[2]
for line in open(table, encoding="utf-8"):
    if line.startswith(f"| {code} "):
        cells = [c.strip() for c in re.split(r"(?<!\\)\|", line.strip().strip("|"))]
        for p in re.findall(r"`([^`]+)`", cells[1]) or [cells[1]]:
            print(p.replace("\\|", "|"))
        break
PY
}

run_log() { # <name> <Gradle log> <device log> [<summary>]: writes the log of one run to $tmp/<name>, prints its path
  local out="$tmp/$1"
  : > "$out"
  [[ ! -f "$DIR/$2" ]] || cat "$DIR/$2" >> "$out"
  [[ -z "${4:-}" || ! -f "$DIR/$4" ]] || cat "$DIR/$4" >> "$out"
  [[ ! -f "$DIR/$3" ]] || grep -E -- "$DEVICE_LOG_RE" "$DIR/$3" >> "$out"
  echo "$out"
}

playbook_row() { # <log>: the first line failure-kind.sh --playbook prints for it
  "$SCRIPTS/failure-kind.sh" --playbook "$1" 2> /dev/null | head -n 1
}

infra_row() { # <playbook line>: its PB-NN when that row is in INFRA_PB, else nothing
  local code="${1%% *}" c
  for c in "${INFRA_PB[@]}"; do
    if [[ "$code" == "$c" ]]; then
      echo "$code"
      return 0
    fi
  done
}

real_failure() { # <log>: true when the app crashed or a test failed (colour codes removed first)
  local plain
  plain="$(sed $'s/\033\\[[0-9;]*m//g' "$1")"
  grep -qE -- "$CRASH_RE" <<<"$plain" && return 0
  grep -qE -- "$TESTS_FAILED_RE" <<<"$plain" && return 0
  [[ "$(grep -vF '> Task ' <<<"$plain" | grep -cE -- "$TEST_FAILED_RE")" != 0 ]]
}

# True when the checked-out commit changes the scripts the ui-test job runs or main-guard.yml. Not ui-test-needed.sh:
# it runs in guard. A commit changing main-guard.yml whose emulator does not start (PB-15) is red: accepted risk.
own_job_changed() {
  local changed
  changed="$(git -C "$DIR" -c core.quotePath=false diff --no-renames --name-only HEAD^ HEAD 2> /dev/null)" || return 1
  grep -qE '^(scripts/ui-test-(on-emulator|verdict)\.sh|\.github/workflows/main-guard\.yml)$' <<<"$changed"
}

tests_run() { # the number of tests in ui-summary.log, 0 without one
  local n
  n="$(sed -n -E 's/.*Test emulator: ([0-9]+) test.*/\1/p' "$DIR/ui-summary.log" 2> /dev/null | head -n 1)"
  echo "${n:-0}"
}

# A red first run that matches no row of INFRA_PB and shows a real failure. Sets first_row to its playbook line.
first_real_failure() {
  local log
  first_row=""
  [[ -f "$DIR/ui-test-first.log" ]] || return 1
  log="$(run_log first ui-test-first.log logcat-first.txt)"
  first_row="$(playbook_row "$log")"
  [[ -z "$(infra_row "$first_row")" ]] || return 1
  real_failure "$log"
}

case "$OUTCOME" in
  success)
    n="$(tests_run)"
    [[ "$n" -gt 0 ]] || infra "không có test" "bước emulator xanh mà không có test nào chạy (ui-summary.log: ${n} test)"
    [[ -f "$DIR/ui-test-first.log" ]] || say green "lần chạy đầu xanh, $n test."
    if first_real_failure; then
      say flaky "lần đầu đỏ thật, lần chạy lại xanh: test chập chờn."
    fi
    say green "lần đầu đỏ vì hạ tầng hay không rõ lý do (${first_row%%:*}), lần chạy lại xanh."
    ;;
  failure)
    if [[ ! -f "$DIR/ui-test.log" ]]; then
      if own_job_changed; then
        say red "không có ui-test.log và commit đổi script hay workflow của chính job ui-test."
      fi
      infra "không có log" "không có ui-test.log: emulator không khởi động hay script không chạy"
    fi
    if first_real_failure; then
      say red "lần đầu đỏ thật (${first_row%%:*}), lần chạy lại cũng đỏ."
    fi
    log="$(run_log last ui-test.log logcat.txt ui-summary.log)"
    if grep -qE -- "$CRASH_RE" "$log"; then
      say red "tiến trình của app dừng: $(grep -m1 -E -- "$CRASH_RE" "$log")"
    fi
    n="$(tests_run)"
    row="$(playbook_row "$log")"
    code="$(infra_row "$row")"
    [[ -z "$code" ]] || infra "$code" "log khớp dòng ${row%%:*} của bảng tra lỗi, $n test" "$log"
    if [[ "$n" -eq 0 ]] && grep -qE -- "$DEVICE_RE" "$log"; then
      infra "thiết bị" "emulator hay thiết bị hỏng, không có test nào chạy" "$log"
    fi
    say red "$n test, log không khớp lỗi hạ tầng nào của emulator (${row%%:*})."
    ;;
  *)
    infra "kết quả lạ" "bước emulator kết thúc với kết quả '${OUTCOME:-rỗng}'"
    ;;
esac
