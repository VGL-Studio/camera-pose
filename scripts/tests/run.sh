#!/usr/bin/env bash
# Tests for the shell scripts of the quality gate. Needs bash, python3 and jq.
# An app made from the template runs them from its first scripts/check.sh: a test that reads this repository's own
# docs must also pass, or print a `skip - ...` line, with no run list and an empty lessons log, as in a new app (L-134).
set -euo pipefail
cd "$(dirname "$0")/../.."

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/bin"
cp scripts/tests/fake-gh "$work/bin/gh"
chmod +x "$work/bin/gh"

failures=0
# Every machine runs these tests like a GitHub runner, which has no global git configuration: no user name, no
# init.defaultBranch. A block that passes only with the developer's ~/.gitconfig fails later in CI (L-113).
export GIT_CONFIG_GLOBAL=/dev/null
export GIT_AUTHOR_NAME=test GIT_AUTHOR_EMAIL=test@example.com GIT_COMMITTER_NAME=test GIT_COMMITTER_EMAIL=test@example.com
# merge-if-green.sh, wait-run.sh and env-canary.sh read owner/repo from the origin remote. A new app has no remote
# until someone pushes it, so give every git command of the tests a fake one (git reads GIT_CONFIG_* as -c options).
export GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=remote.origin.url GIT_CONFIG_VALUE_0=https://github.com/o/r.git
# A block that pushes to a real local bare repo must unset these three variables first and restore them at its end
# (see the save-branch block), or every push goes to the fake URL and fails (L-082).
expect() {
  local name="$1" expected="$2" actual="$3"
  if [[ "$actual" == "$expected" ]]; then
    echo "ok   - $name"
  else
    echo "FAIL - $name: expected '$expected', got '$actual'"
    failures=$((failures + 1))
  fi
}

# Commits: A=aaaaaa1, B=bbbbbb2, C=ccccc03, D=ddddd04 (D is the red head).
find_breaking() {
  rm -f "$work/fx.json.calls"
  echo "$1" > "$work/fx.json"
  PATH="$work/bin:$PATH" FIXTURE="$work/fx.json" GITHUB_REPOSITORY=o/r POLL_SECONDS=0 MAX_WAIT_SECONDS=1 \
    scripts/find-breaking-commit.sh ddddd04 2>/dev/null
}

expect "parent green: blame the head" ddddd04 "$(find_breaking '{
  "aaaaaa1":{"parent":"","states":["success"]},
  "ddddd04":{"parent":"aaaaaa1","states":["failure"]}}')"

expect "older red commits: blame the oldest red after the last green" bbbbbb2 "$(find_breaking '{
  "aaaaaa1":{"parent":"","states":["success"]},
  "bbbbbb2":{"parent":"aaaaaa1","states":["failure"]},
  "ccccc03":{"parent":"bbbbbb2","states":["failure"]},
  "ddddd04":{"parent":"ccccc03","states":["failure"]}}')"

expect "pending parent that turns red is blamed" bbbbbb2 "$(find_breaking '{
  "aaaaaa1":{"parent":"","states":["success"]},
  "bbbbbb2":{"parent":"aaaaaa1","states":["pending","failure"]},
  "ddddd04":{"parent":"bbbbbb2","states":["failure"]}}')"

expect "pending parent that turns green clears it" ddddd04 "$(find_breaking '{
  "aaaaaa1":{"parent":"","states":["success"]},
  "bbbbbb2":{"parent":"aaaaaa1","states":["pending","success"]},
  "ddddd04":{"parent":"bbbbbb2","states":["failure"]}}')"

expect "docs-only commit is skipped" bbbbbb2 "$(find_breaking '{
  "aaaaaa1":{"parent":"","states":["success"]},
  "bbbbbb2":{"parent":"aaaaaa1","states":["failure"]},
  "ccccc03":{"parent":"bbbbbb2","files":["docs/a.md"]},
  "ddddd04":{"parent":"ccccc03","states":["failure"]}}')"

expect "run not created yet is waited for" ccccc03 "$(find_breaking '{
  "aaaaaa1":{"parent":"","states":["success"]},
  "bbbbbb2":{"parent":"aaaaaa1","states":["success"]},
  "ccccc03":{"parent":"bbbbbb2","states":[null,"failure"]},
  "ddddd04":{"parent":"ccccc03","states":["failure"]}}')"

expect "start of history: blame the oldest red" bbbbbb2 "$(find_breaking '{
  "bbbbbb2":{"parent":"","states":["failure"]},
  "ddddd04":{"parent":"bbbbbb2","states":["failure"]}}')"

# find-breaking-commit.sh --check ui-test (ADR-0043, T-075): the same walk over the check run ui-test of main-guard.yml.
# Every ui-test answer is a route with check_name=ui-test, so a script asking for another name gets the guard states.
# POLL_SECONDS=1 and MAX_WAIT_SECONDS=1: a commit the script wrongly waits for ends in one second with the warning.
ui_run() { # <check run id> <conclusion>: the check-runs answer with one completed run
  printf '{"check_runs": [{"id": %s, "started_at": "2026-01-01T00:00:00Z", "status": "completed", "conclusion": "%s"}]}' "$1" "$2"
}
ui_route() { # <sha> <check run id> <conclusion>: the route of the check run ui-test of that commit
  printf '"commits/%s/check-runs?check_name=ui-test&per_page=20": %s' "$1" "$(ui_run "$2" "$3")"
}
find_breaking_ui() { # <fixture> [args before the sha]: prints stdout; stderr goes to $work/fbc-ui.err
  rm -f "$work/fxu.json.calls" "$work/fxu.json.seen"
  echo "$1" > "$work/fxu.json"
  shift
  PATH="$work/bin:$PATH" FIXTURE="$work/fxu.json" GITHUB_REPOSITORY=o/r POLL_SECONDS=1 MAX_WAIT_SECONDS=1 \
    scripts/find-breaking-commit.sh "$@" ddddd04 2> "$work/fbc-ui.err" || true
}
fbc_waited() { grep -c 'CẢNH BÁO: chờ quá lâu' "$work/fbc-ui.err" || true; }
# The guard states say something else: C green, so without --check the head is blamed.
fbc_guard='"aaaaaa1":{"parent":"","states":["failure"]},
  "bbbbbb2":{"parent":"aaaaaa1","states":["success"]},
  "ccccc03":{"parent":"bbbbbb2","states":["success"]},
  "ddddd04":{"parent":"ccccc03","states":["failure"]}'
fx_ui='{"routes": {'"$(ui_route aaaaaa1 11 success), $(ui_route bbbbbb2 21 failure), $(ui_route ccccc03 31 skipped)"',
  '"$(ui_route ddddd04 41 failure)"', "check-runs/21/annotations?per_page=100": [], "check-runs/41/annotations?per_page=100": []},
  '"$fbc_guard"'}'
expect "find-breaking-commit --check ui-test: blames the oldest red ui-test after the newest green" bbbbbb2 \
  "$(find_breaking_ui "$fx_ui" --check ui-test)"
expect "find-breaking-commit --check ui-test: without --check the guard states decide" ddddd04 "$(find_breaking_ui "$fx_ui")"
# An infrastructure failure (the check step printed EMULATOR HẠ TẦNG) is not blamed; anything else red is.
fbc_infra() { # <annotations answer of C, or "error">: D and C red, B green
  local c_ann='"check-runs/31/annotations?per_page=100": '"$1"','
  [[ "$1" != error ]] || c_ann=''
  echo '{"errors": {'"$([[ "$1" == error ]] && echo '"check-runs/31/annotations?per_page=100": "HTTP 500: Server Error"')"'},
  "routes": {'"$(ui_route bbbbbb2 21 success), $(ui_route ccccc03 31 failure), $(ui_route ddddd04 41 failure)"',
  '"$c_ann"' "check-runs/41/annotations?per_page=100": []},
  '"$fbc_guard"'}'
}
fx_ann_infra="$(fbc_infra '[{"message": "EMULATOR HẠ TẦNG (PB-11): Kho Maven trả 403; không mở main-broken."}]')"
fx_ann_none="$(fbc_infra '[]')"
fx_ann_old="$(fbc_infra '[{"message": "HẠ TẦNG: GitHub không cấp máy."}]')"
fx_ann_error="$(fbc_infra error)"
expect "find-breaking-commit --check ui-test: an infrastructure failure is not blamed" ddddd04 \
  "$(find_breaking_ui "$fx_ann_infra" --check ui-test)"
expect "find-breaking-commit --check ui-test: a red ui-test without annotation is blamed" ccccc03 \
  "$(find_breaking_ui "$fx_ann_none" --check ui-test)"
expect "find-breaking-commit --check ui-test: the PB-02 prefix HẠ TẦNG: is not infrastructure of the emulator" ccccc03 \
  "$(find_breaking_ui "$fx_ann_old" --check ui-test)"
expect "find-breaking-commit --check ui-test: annotations that cannot be read make the red commit a suspect" ccccc03 \
  "$(find_breaking_ui "$fx_ann_error" --check ui-test)"
# A cancelled or timed-out ui-test is unknown: walk on at once instead of waiting until MAX_WAIT_SECONDS.
fx_cancelled='{"routes": {'"$(ui_route bbbbbb2 21 success), $(ui_route ccccc03 31 cancelled), $(ui_route ddddd04 41 failure)"'},
  '"$fbc_guard"'}'
fx_timed_out='{"routes": {'"$(ui_route bbbbbb2 21 success), $(ui_route ccccc03 31 timed_out), $(ui_route ddddd04 41 failure)"'},
  '"$fbc_guard"'}'
expect "find-breaking-commit --check ui-test: a cancelled or timed-out ui-test is walked over without waiting" \
  "ddddd04 0 ddddd04 0" \
  "$(find_breaking_ui "$fx_cancelled" --check ui-test) $(fbc_waited) $(find_breaking_ui "$fx_timed_out" --check ui-test) $(fbc_waited)"
# A commit from before T-075 has no ui-test check run: its main-guard.yml had no ui-test job, the emulator ran on the PR.
fbc_no_run() { # <main-guard.yml of C>
  echo '{"routes": {"commits/ccccc03/check-runs?check_name=ui-test&per_page=20": {"check_runs": []},
  "contents/.github/workflows/main-guard.yml?ref=ccccc03": '"$(jq -Rs . <<<"$1")"',
  '"$(ui_route ddddd04 41 failure)"'},
  '"$fbc_guard"'}'
}
fx_before="$(fbc_no_run $'jobs:\n  guard:\n    runs-on: ubuntu-latest')"
fx_after="$(fbc_no_run $'jobs:\n  guard:\n    runs-on: ubuntu-latest\n  ui-test:\n    needs: guard')"
expect "find-breaking-commit --check ui-test: stops at a commit whose main-guard.yml has no ui-test job" "ddddd04 0" \
  "$(find_breaking_ui "$fx_before" --check ui-test) $(fbc_waited)"
expect "find-breaking-commit --check ui-test: waits for a commit whose main-guard.yml has the ui-test job" "ddddd04 1" \
  "$(find_breaking_ui "$fx_after" --check ui-test) $(fbc_waited)"
fbc_code=0
PATH="$work/bin:$PATH" FIXTURE="$work/fxu.json" GITHUB_REPOSITORY=o/r scripts/find-breaking-commit.sh --check foo ddddd04 \
  > /dev/null 2>&1 || fbc_code=$?
fx_guard_only='{"aaaaaa1":{"parent":"","states":["success"]},
  "bbbbbb2":{"parent":"aaaaaa1","states":["failure"]},
  "ccccc03":{"parent":"bbbbbb2","states":["failure"]},
  "ddddd04":{"parent":"ccccc03","states":["failure"]}}'
expect "find-breaking-commit: --check accepts only guard and ui-test" "1 bbbbbb2 bbbbbb2" \
  "$fbc_code $(find_breaking_ui "$fx_guard_only" --check guard) $(find_breaking_ui "$fx_guard_only")"

# main-guard-status.sh: start-run.yml and the session start only when main-guard passed on the newest commit of main
# with code (L-076). Same fixture as find-breaking-commit.sh; D=ddddd04 is the head of main.
main_guard_status() { # <fixture> [env...]: prints "<exit code> <output on one line>"
  rm -f "$work/mgs.json.calls"
  echo "$1" > "$work/mgs.json"
  shift
  local out code=0
  # Without GITHUB_ACTIONS, so that the tests read the same lines on a dev machine and on CI (which sets it).
  out="$(env -u GITHUB_ACTIONS PATH="$work/bin:$PATH" FIXTURE="$work/mgs.json" GITHUB_REPOSITORY=o/r "$@" \
    scripts/main-guard-status.sh ddddd04 2>&1)" || code=$?
  echo "$code $(tr '\n' ' ' <<<"$out")"
}
mgs="$(main_guard_status '{
  "ccccc03":{"parent":"","states":["success"]},
  "ddddd04":{"parent":"ccccc03","states":["success"]}}')"
expect "main-guard-status: green run on the head passes" 1 "$(grep -c '^0 XANH' <<<"$mgs")"
# App camera-pose, RUN-20261023-03: a docs-only commit (no run) on top of a code commit with no run (GitHub sent no push).
mgs="$(main_guard_status '{
  "bbbbbb2":{"parent":"","states":["success"]},
  "ccccc03":{"parent":"bbbbbb2","files":["scripts/run-check.py"]},
  "ddddd04":{"parent":"ccccc03","files":["docs/runs/RUN-1.md"]}}')"
expect "main-guard-status: code commit without a run is refused" 1 "$(grep -c '^3 CHƯA KIỂM: ccccc03' <<<"$mgs")"
expect "main-guard-status: the refusal says how to run main-guard" 1 \
  "$(grep -c 'actions/workflows/main-guard.yml/dispatches' <<<"$mgs")"
mgs="$(main_guard_status '{
  "ccccc03":{"parent":"","states":["success"]},
  "ddddd04":{"parent":"ccccc03","files":["docs/a.md"]}}')"
expect "main-guard-status: docs-only head after a green code commit passes" 1 "$(grep -c '^0 XANH' <<<"$mgs")"
expect "main-guard-status: names the green code commit" 1 "$(grep -c 'ccccc03' <<<"$mgs")"
mgs="$(main_guard_status '{
  "ccccc03":{"parent":""},
  "ddddd04":{"parent":"ccccc03","files":["docs/a.md"],"states":["success"]}}')"
expect "main-guard-status: a run on a docs-only head counts" 1 "$(grep -c '^0 XANH: main-guard qua trên ddddd04' <<<"$mgs")"
mgs="$(main_guard_status '{
  "routes":{"actions/workflows/main-guard.yml/runs?head_sha=ddddd04&per_page=1":
    {"workflow_runs":[{"created_at":"2025-12-31T23:59:00Z"}]}},
  "ddddd04":{"parent":"","states":["pending"]}}')"
expect "main-guard-status: pending run asks to wait" 1 "$(grep -c '^2 CHỜ' <<<"$mgs")"
# wait-run.sh --since keeps runs created after the time given, so the time is the workflow run's creation, which
# comes before the check run starts (2026-01-01T00:00:00Z in fake-gh).
expect "main-guard-status: pending gives the creation time of the workflow run for --since" 1 \
  "$(grep -c -- '--since 2025-12-31T23:59:00Z' <<<"$mgs")"
broken_route='"issues?state=open&labels=main-broken&per_page=10"'
mgs="$(main_guard_status '{"routes":{'"$broken_route"':[{"number":5}]},
  "ddddd04":{"parent":"","states":["failure"]}}')"
expect "main-guard-status: red run with an open main-broken issue is refused" 1 "$(grep -c '^1 ĐỎ' <<<"$mgs")"
mgs="$(main_guard_status '{"routes":{'"$broken_route"':[{"number":5}]},
  "ddddd04":{"parent":"","states":["failure"]}}' GITHUB_ACTIONS=true)"
expect "main-guard-status: in GitHub Actions a refusal is an ::error:: line" 1 "$(grep -c '^1 ::error::ĐỎ' <<<"$mgs")"
# The guard's job also fails on infrastructure (checkout, Java, network), and then opens no main-broken issue:
# not a red main, so ask to run main-guard again instead of saying ĐỎ with no reason.
mgs="$(main_guard_status '{"routes":{'"$broken_route"':[]},
  "ddddd04":{"parent":"","states":["failure"]}}')"
expect "main-guard-status: a failed run without main-broken is not red (code 3)" 1 \
  "$(grep -c '^3 CHƯA KIỂM: main-guard thất bại trên ddddd04 nhưng không có issue main-broken' <<<"$mgs")"
expect "main-guard-status: a failed run without main-broken asks to run main-guard again" 2 \
  "$(grep -o 'Re-run all jobs\|actions/workflows/main-guard.yml/dispatches' <<<"$mgs" | wc -l | tr -d ' ')"
mgs="$(main_guard_status '{
  "ddddd04":{"parent":"","states":["failure"]}}')"
expect "main-guard-status: unreadable main-broken issues stop with code 4" 1 "$(grep -c '^4 LỖI: không đọc được' <<<"$mgs")"
mgs="$(main_guard_status '{
  "bbbbbb2":{"parent":"","states":["success"]},
  "ccccc03":{"parent":"bbbbbb2","files":["docs/b.md"]},
  "ddddd04":{"parent":"ccccc03","files":["docs/a.md"]}}' MAX_DEPTH=2)"
expect "main-guard-status: stops after MAX_DEPTH" 3 "${mgs%% *}"
# A run that ended without a verdict (cancelled, timed out, ...) proves nothing and will not change: run it again.
mgs="$(main_guard_status '{
  "ddddd04":{"parent":"","states":["cancelled"]}}')"
expect "main-guard-status: a cancelled run asks to run main-guard again" 1 "$(grep -c '^3 CHƯA KIỂM: ddddd04.*cancelled' <<<"$mgs")"
# A commit is skipped only when it surely changes docs alone: no file list (GitHub cuts very large commits) or a
# merge commit (its files are against the first parent only) counts as code.
mgs="$(main_guard_status '{
  "ccccc03":{"parent":"","states":["success"]},
  "ddddd04":{"parent":"ccccc03","files":[]}}')"
expect "main-guard-status: a commit without a file list counts as code" 1 "$(grep -c '^3 CHƯA KIỂM: ddddd04' <<<"$mgs")"
mgs="$(main_guard_status '{
  "bbbbbb2":{"parent":"","states":["success"]},
  "ccccc03":{"parent":"","states":["success"]},
  "ddddd04":{"parents":["ccccc03","bbbbbb2"],"files":["docs/a.md"]}}')"
expect "main-guard-status: a merge commit counts as code" 1 "$(grep -c '^3 CHƯA KIỂM: ddddd04' <<<"$mgs")"
expect "main-guard-status: a wrong argument is an error (code 4)" 4 \
  "$(PATH="$work/bin:$PATH" scripts/main-guard-status.sh not-a-sha > /dev/null 2>&1; echo $?)"
# GitHub not answering (unknown commit, network) is not "code without a run": stop with its own code.
mgs="$(main_guard_status '{
  "ddddd04":{"parent":"ccccc03","files":["docs/a.md"]}}')"
expect "main-guard-status: an unreadable commit stops with code 4" 1 "$(grep -c '^4 LỖI: không đọc được' <<<"$mgs")"
# start-run.yml checks main-guard after the run-file checks and before firing the routine; main-guard.yml can be
# run by hand for a commit that got no push event (L-076), keeping its push trigger as it was.
start_wf=.github/workflows/start-run.yml
expect "start-run: main-guard step runs after the run-file checks and before the routine" "yes yes 0" \
  "$(awk '/main-broken/ && !b { b = NR } /scripts\/main-guard-status.sh/ && !s { s = NR }
     /name: Kiểm tra thiết lập staging, file R2 và DNS/ { e = NR } /name: Kích hoạt routine/ { r = NR }
     END { print ((b && s > b && s < e && e < r) ? "yes" : "no") }' "$start_wf") $(grep -qE '^  checks: read$' "$start_wf" && echo yes || echo no) $(grep -c 'continue-on-error' "$start_wf")"
# start-run.yml closes an old setup-broken issue once the setup check is green and before the routine starts, in a
# job of its own: only that job may write issues (L-113). The checks never get a GitHub token in .git/config or env.
expect "start-run: only close-setup-broken writes issues, between the checks and the routine" "yes 1 1 1" \
  "$(grep -qE '^  issues: read$' "$start_wf" && echo yes || echo no) $(grep -cE '^      issues: write$' "$start_wf") $(awk '/^  close-setup-broken:$/ { c = 1 } c && /^    needs: check$/ { n++ } END { print n + 0 }' "$start_wf") $(grep -cE '^    needs: \[check, close-setup-broken\]$' "$start_wf")"
expect "start-run: the setup check runs without a GitHub token" "1 1" \
  "$(grep -c 'persist-credentials: false' "$start_wf") $(grep -c 'env -u GH_TOKEN -u GITHUB_TOKEN scripts/setup-check.sh --staging' "$start_wf")"
canary_wf=.github/workflows/env-canary.yml
# env-canary.yml checks a red setup once more after 90 seconds, and setup_only runs the setup check without the routine.
expect "env-canary: a red setup check is run again after 90 seconds, without a GitHub token" "1 2 1" \
  "$(grep -c '^            sleep 90$' "$canary_wf") $(grep -c 'env -u GH_TOKEN -u GITHUB_TOKEN scripts/setup-check.sh --staging' "$canary_wf") $(grep -c 'persist-credentials: false' "$canary_wf")"
expect "env-canary: setup_only skips the routine" "1 1" \
  "$(grep -cE '^      setup_only:$' "$canary_wf") $(awk '/^  fire:$/ { f = 1; next } f && /^    if: \$\{\{ !inputs.setup_only \}\}$/ { n++ } f && /^    steps:$/ { f = 0 } END { print n + 0 }' "$canary_wf")"
guard_wf=.github/workflows/main-guard.yml
# Run by hand on another branch, the guard would open main-broken, push a revert or close an issue for that branch.
expect "main-guard: has workflow_dispatch and keeps the push trigger with the same paths-ignore" "1 1 1 1 1" \
  "$(grep -cE '^  workflow_dispatch:' "$guard_wf") $(grep -cE '^  push:$' "$guard_wf") $(grep -cE "^      - 'docs/\*\*'$" "$guard_wf") $(grep -cE "^      - '\*\*/\*\.md'$" "$guard_wf") $(awk '/^  guard:$/ { g = 1; next } g && /^    if: github.ref == .refs\/heads\/main.$/ { n++ } g && /^    steps:$/ { g = 0 } END { print n + 0 }' "$guard_wf")"

# pr-check.yml: a PR without code still runs scripts/tests/run.sh in build-and-test, without Gradle or the Android SDK,
# and the job keeps its name and is skipped only for drafts (ADR-0041), so merge-if-green.sh still requires it green (L-040).
# Each check takes the workflow path, so it also runs on a broken copy in $work (L-055).
pr_wf=.github/workflows/pr-check.yml
pr_steps() { # <workflow>; prints each step of build-and-test on one line, its lines joined by tabs
  awk '/^  build-and-test:$/ { j = 1; next }
    j && /^  [^ ]/ { j = 0 }
    j && /^      - / { if (s != "") print s; s = $0; next }
    j && s != "" && /^        / { s = s "\t" $0 }
    END { if (s != "") print s }' "$1"
}
pr_docs_steps() { # <workflow>; counts the steps that run scripts/tests/run.sh only when the PR changes no code
  pr_steps "$1" | grep -F "if: steps.filter.outputs.code != 'true'" | grep -cF 'scripts/tests/run.sh 2>&1 | tee build.log' || true
}
pr_unconditional() { # <workflow> <uses>; "yes" when the step with that action has no if:, "missing" with no such step
  local step
  step="$(pr_steps "$1" | grep -F "uses: $2" || true)"
  if [[ -z "$step" ]]; then echo missing; elif grep -q 'if:' <<<"$step"; then echo no; else echo yes; fi
}
pr_gradle_gated() { # <workflow>; "yes" when every step using Gradle or the Android SDK runs only for code
  # The path filter step runs for every PR and its patterns name gradlew and scripts/check.sh: skip that step only.
  local bad tab=$'\t'
  bad="$(pr_steps "$1" | grep -vE "(^      - |$tab        )uses: dorny/paths-filter@v3($tab|$)" \
    | grep -E 'setup-gradle|gradlew|scripts/check\.sh|ANDROID_HOME|android-emulator-runner' \
    | grep -vcE "if: [^$tab]*steps\.filter\.outputs\.code == 'true'" || true)"
  [[ "$bad" == 0 ]] && echo yes || echo no
}
pr_docs_step_tools() { # <workflow>; the docs-only step checks its tools and has its own timeout
  local step
  step="$(pr_steps "$1" | grep -F "if: steps.filter.outputs.code != 'true'" | grep -F 'scripts/tests/run.sh' || true)"
  local t n=0
  for t in 'command -v' python3 jq java 'timeout-minutes: 10'; do
    grep -qF -- "$t" <<<"$step" && n=$((n + 1))
  done
  echo "$n"
}
pr_job_if_draft() { # <workflow>; "<job-level if: lines of build-and-test> <yes when the only one skips drafts>"
  awk '/^  build-and-test:$/ { j = 1; next } j && /^    steps:$/ { j = 0 } j && /^    if:/ { n++; if ($0 == "    if: github.event.pull_request.draft == false") d++ } END { print n + 0, (n == 1 && d == 1 ? "yes" : "no") }' "$1"
}
pr_types() { # <workflow>; "<lines of the four event types> <lines of branches: [main]>" in on.pull_request
  awk '/^  pull_request:$/ { p = 1; next } p && /^(  )?[^ ]/ { p = 0 }
    p && $0 == "    types: [opened, synchronize, reopened, ready_for_review]" { t++ }
    p && $0 == "    branches: [main]" { b++ } END { print t + 0, b + 0 }' "$1"
}
pr_failure_comment() { # <workflow>; "yes" when the failure comment runs on any failure and shows run.sh failures
  local step tab=$'\t'
  step="$(pr_steps "$1" | grep -F 'name: Bình luận lỗi vào PR' || true)"
  if grep -qE "if: failure\(\)($tab|$)" <<<"$step" && grep -qF '^FAIL - ' <<<"$step"; then echo yes; else echo no; fi
}
pr_missing_jobs() { # <workflow>; names of merge-if-green.sh's required and skippable checks with no job in the workflow
  local names name missing="" count=0
  names="$(sed -n -E 's/^(REQUIRED|SKIPPABLE)_CHECKS=\((.*)\)$/\2/p' scripts/merge-if-green.sh | tr -d '"')"
  for name in $names; do
    count=$((count + 1))
    grep -qE "^  $name:$" "$1" || missing="$missing $name"
  done
  echo "names=$count missing=${missing:- none}"
}
expect "pr-check: build-and-test runs scripts/tests/run.sh when the PR changes no code" 1 "$(pr_docs_steps "$pr_wf")"
expect "pr-check: checkout and Java run for every PR" "yes yes" \
  "$(pr_unconditional "$pr_wf" actions/checkout@v5) $(pr_unconditional "$pr_wf" actions/setup-java@v5)"
expect "pr-check: the docs-only path of build-and-test uses no Gradle and no Android SDK" yes "$(pr_gradle_gated "$pr_wf")"
mkdir -p "$work/prwf"
grep -v 'uses: actions/setup-java@v5' "$pr_wf" > "$work/prwf/no-java.yml"
expect "pr-check: the unconditional check names a step that is missing" missing \
  "$(pr_unconditional "$work/prwf/no-java.yml" actions/setup-java@v5)"
perl -0pe "s/- if: steps.filter.outputs.code == 'true'\n        uses: gradle\/actions\/setup-gradle/- uses: gradle\/actions\/setup-gradle/" \
  "$pr_wf" > "$work/prwf/gradle.yml"
expect "pr-check: the Gradle check fails on a copy whose setup-gradle runs for every PR" no "$(pr_gradle_gated "$work/prwf/gradle.yml")"
expect "pr-check: the docs-only step checks python3, jq and java first and has its own timeout" 5 "$(pr_docs_step_tools "$pr_wf")"
# A draft PR runs no step of build-and-test (ADR-0041); every PR that is not a draft still runs it, even without code.
expect "pr-check: build-and-test has one job-level if, and it only skips drafts" "1 yes" "$(pr_job_if_draft "$pr_wf")"
awk '{ print } /^  build-and-test:$/ { print "    if: github.event_name == '\''pull_request'\''" }' \
  "$pr_wf" > "$work/prwf/job-if.yml"
expect "pr-check: the job-level if check fails on a copy with a second job-level if" "2 no" \
  "$(pr_job_if_draft "$work/prwf/job-if.yml")"
perl -pe 's/^    if: github\.event\.pull_request\.draft == false$/    if: github.event.pull_request.draft == false || github.event_name == \x27push\x27/' \
  "$pr_wf" > "$work/prwf/job-if-odd.yml"
expect "pr-check: the job-level if check fails on a copy whose condition does more than skip drafts" "1 no" \
  "$(pr_job_if_draft "$work/prwf/job-if-odd.yml")"
grep -vxF '    if: github.event.pull_request.draft == false' "$pr_wf" > "$work/prwf/job-if-none.yml" || true
expect "pr-check: the job-level if check fails on a copy without the draft condition" "0 no" \
  "$(pr_job_if_draft "$work/prwf/job-if-none.yml")"
# Declaring types drops GitHub's default set: synchronize must stay, or new commits of a PR go unchecked.
expect "pr-check: runs on opened, synchronize, reopened and ready_for_review" "1 1" "$(pr_types "$pr_wf")"
sed 's/synchronize, //' "$pr_wf" > "$work/prwf/no-sync.yml"
expect "pr-check: the event types check fails on a copy without synchronize" "0 1" "$(pr_types "$work/prwf/no-sync.yml")"
expect "pr-check: the failure comment of build-and-test also runs for docs-only PRs" yes "$(pr_failure_comment "$pr_wf")"
expect "pr-check: every check merge-if-green.sh requires or may skip is a job of pr-check.yml" "names=1 missing= none" \
  "$(pr_missing_jobs "$pr_wf")"
sed 's/^  build-and-test:$/  build:/' "$pr_wf" > "$work/prwf/renamed.yml"
expect "pr-check: the job name check names a renamed job" "names=1 missing= build-and-test" \
  "$(pr_missing_jobs "$work/prwf/renamed.yml")"

# pr-check has one job (T-076): the path filter (id: filter) is the first step of build-and-test, or comes right after
# checkout, and every later step reads steps.filter.outputs. A filter output that is empty or odd stops the job.
pr_jobs() { # <workflow>; names of the jobs, separated by spaces
  awk '/^jobs:$/ { j = 1; next } j && /^[^ #]/ { j = 0 } j && /^  [A-Za-z0-9_-]+:$/ { sub(/:$/, ""); sub(/^  /, ""); printf "%s%s", (n++ ? " " : ""), $0 } END { print "" }' "$1"
}
pr_step_has() { # <step line> <key: value regex>; true when a key line of the step matches
  local tab=$'\t'
  grep -qE "(^      - |$tab        )$2($tab|$)" <<<"$1"
}
pr_filter_order() { # <workflow>; "(a) (b) (c) (d) <steps reading the filter>", or "missing" without a filter step
  local steps=() i fi=-1 a=no b=no c=yes d=yes n=0
  mapfile -t steps < <(pr_steps "$1")
  for i in "${!steps[@]}"; do
    if pr_step_has "${steps[$i]}" 'id: filter'; then fi=$i; break; fi
  done
  [[ "$fi" -ge 0 ]] || { echo missing; return; }
  if pr_step_has "${steps[$fi]}" 'uses: dorny/paths-filter@v3' && ! pr_step_has "${steps[$fi]}" 'if: .*'; then a=yes; fi
  if [[ $((fi + 1)) -lt ${#steps[@]} ]] && pr_step_has "${steps[$((fi + 1))]}" 'name: Kiểm kết quả lọc đường dẫn' \
    && ! pr_step_has "${steps[$((fi + 1))]}" 'if: .*'; then b=yes; fi
  for ((i = 0; i < fi; i++)); do
    pr_step_has "${steps[$i]}" 'uses: actions/checkout@v5' || c=no
  done
  for i in "${!steps[@]}"; do
    if grep -qF 'steps.filter.outputs' <<<"${steps[$i]}"; then
      n=$((n + 1))
      [[ "$i" -gt "$fi" ]] || d=no
    fi
  done
  echo "$a $b $c $d $n"
}
pr_filter_outputs() { # <workflow>; the filter outputs read, the filters defined, and other step outputs read
  local reads defined other
  reads="$(grep -oE 'steps\.filter\.outputs\.[A-Za-z0-9_-]+' "$1" | sed 's/.*\.//' | sort -u | tr '\n' ' ' | sed 's/ $//')"
  defined="$(awk '/^  build-and-test:$/ { j = 1; next } j && /^  [^ ]/ { j = 0 }
    j && /^      - / { s = 0 } j && /^(      - |        )id: filter$/ { s = 1 }
    j && s && /^          filters: \|$/ { f = 1; next } j && s && f && /^            [A-Za-z0-9_-]+:$/ { sub(/^ +/, ""); sub(/:$/, ""); print; next }
    j && s && f && !/^             / && !/^            [A-Za-z0-9_-]+:$/ { f = 0 }' "$1" | sort -u | tr '\n' ' ' | sed 's/ $//')"
  other="$(grep -oE 'steps\.[A-Za-z0-9_-]+\.outputs' "$1" | grep -vc '^steps\.filter\.outputs$' || true)"
  echo "reads=$reads defined=$defined other=$other"
}
pr_filter_check() { # <workflow> <code> <release>; runs the filter check step like GitHub; the exit code, plus the lines
  # of build.log when it fails
  local dir script code=0
  dir="$(mktemp -d "$work/filter-check.XXXXXX")"
  script="$(awk '/^      - / { s = 0 } /^      - name: Kiểm kết quả lọc đường dẫn$/ { s = 1; next }
    s && /^        run: \|$/ { r = 1; next } r && s && /^ *$/ { print ""; next }
    r && s && /^          / { sub(/^          /, ""); print; next } r { r = 0 }' "$1")"
  (cd "$dir" && CODE="$2" RELEASE="$3" bash -eo pipefail -c "$script") > /dev/null 2>&1 || code=$?
  if [[ "$code" == 0 ]]; then echo 0; else echo "$code $(wc -l < "$dir/build.log" 2>/dev/null | tr -d ' ' || echo 0)"; fi
}
pr_sim() { # <workflow> <path>...; the filter outputs for a sample PR and the steps of build-and-test that run
  # PR_SIM_RUNNER (github-hosted by default, or self-hosted) stands for runner.environment (ADR-0042).
  python3 -I - "$@" << 'PY'
import os, re, sys
lines = open(sys.argv[1], encoding="utf-8").read().split("\n")
paths = sys.argv[2:]
steps, job, job_ifs = [], False, []
for line in lines:
    if line == "  build-and-test:":
        job = True; continue
    if job and re.match(r"^  [^ ]", line):
        break
    if not job:
        continue
    if not steps and line.startswith("    if: "):
        job_ifs.append(line[8:])
    elif line.startswith("      - "):
        steps.append([line[8:]])
    elif steps and line.startswith("        "):
        steps[-1].append(line[8:])
def key(step, name):
    for l in step:
        m = re.match(r"^" + name + r": (.*)$", l)
        if m:
            return m.group(1)
    return None
flt = [s for s in steps if key(s, "id") == "filter"]
if not flt:
    print("ERROR không có bước id: filter trong build-and-test"); sys.exit(0)
# The block ends at the first line indented no deeper than "filters: |" (a sibling key of with:). Inside it, a line
# that is no comment, no filter name and no quoted pattern of a filter is refused, never skipped.
filters, cur, depth = {}, None, None
for l in flt[0]:
    if depth is None:
        if l.strip() == "filters: |":
            depth = len(l) - len(l.lstrip())
        continue
    if l.strip().startswith("#") or not l.strip():
        continue
    if len(l) - len(l.lstrip()) <= depth:
        break
    m = re.match(r"^\s+([A-Za-z0-9_-]+):$", l)
    if m:
        cur = m.group(1); filters[cur] = []; continue
    m = re.match(r"^\s+- '(.+)'$", l)
    if m and cur:
        filters[cur].append(m.group(1)); continue
    print("ERROR mẫu chưa hỗ trợ: " + l.strip()); sys.exit(0)
special = set("*?[]{}!")
def to_regex(p):
    if p.endswith("/**") and not special & set(p[:-3]):
        return "^" + re.escape(p[:-3]) + "/.+$"
    if p.startswith("**/") and not special & set(p[3:]) and "/" not in p[3:]:
        return "^(.*/)?" + re.escape(p[3:]) + "$"
    if p.startswith("*") and "/" not in p and not special & set(p[1:]):
        return "^[^/]*" + re.escape(p[1:]) + "$"
    if not special & set(p):
        return "^" + re.escape(p) + "$"
    return None
values = {}
for name, pats in filters.items():
    regs = []
    for p in pats:
        r = to_regex(p)
        if r is None:
            print("ERROR mẫu chưa hỗ trợ: " + p); sys.exit(0)
        regs.append(r)
    values[name] = "true" if any(re.match(r, path) for r in regs for path in paths) else "false"
runner = os.environ.get("PR_SIM_RUNNER", "github-hosted")
if runner not in ("github-hosted", "self-hosted"):
    print("ERROR PR_SIM_RUNNER lạ: " + runner); sys.exit(0)
out = ["code=%s release=%s" % (values.get("code", ""), values.get("release", ""))]
# The only job-level condition understood is the draft one (ADR-0041); PR_SIM_DRAFT=true makes the sample PR a draft.
if len(job_ifs) > 1:
    print("ERROR nhiều điều kiện cấp job"); sys.exit(0)
if job_ifs and job_ifs[0] != "github.event.pull_request.draft == false":
    print("ERROR điều kiện lạ cấp job: " + job_ifs[0]); sys.exit(0)
if job_ifs and os.environ.get("PR_SIM_DRAFT") == "true":
    print(out[0]); sys.exit(0)
for s in steps:
    cond = key(s, "if")
    if cond is not None:
        expr = re.sub(r"steps\.filter\.outputs\.([A-Za-z0-9_-]+)", lambda m: repr(values.get(m.group(1), "")), cond)
        expr = re.sub(r"\brunner\.environment\b", repr(runner), expr)
        expr = expr.replace("always()", "True").replace("failure()", "False").replace("&&", " and ").replace("||", " or ")
        rest = re.sub(r"'[^']*'|==|!=|\band\b|\bor\b|\bTrue\b|\bFalse\b|[() ]", "", expr)
        if rest:
            print("ERROR điều kiện lạ: " + cond); sys.exit(0)
        if not eval(expr, {"__builtins__": {}}, {}):
            continue
    label = key(s, "name") or key(s, "uses") or key(s, "run") or s[0]
    out.append(label)
print("\n".join(out))
PY
}
pr_sim_summary() { # <workflow> <path>...; "code=.. release=.. gate=.. gradle=.. docs=.. r8=.." or the ERROR line
  local out
  out="$(pr_sim "$@")"
  if grep -q '^ERROR' <<<"$out"; then grep -m1 '^ERROR' <<<"$out"; return; fi
  printf '%s gate=%s gradle=%s docs=%s r8=%s\n' "$(head -n1 <<<"$out")" \
    "$(grep -qxF 'Cổng chất lượng (scripts/check.sh)' <<<"$out" && echo yes || echo no)" \
    "$(grep -qF 'setup-gradle' <<<"$out" && echo yes || echo no)" \
    "$(grep -qxF 'Test của script và tài liệu (scripts/tests/run.sh)' <<<"$out" && echo yes || echo no)" \
    "$(grep -qxF 'Build bản release (R8)' <<<"$out" && echo yes || echo no)"
}
# P1, P2
expect "pr-check: has exactly one job, build-and-test" build-and-test "$(pr_jobs "$pr_wf")"
{ cat "$pr_wf"; printf '%s\n' '' '  extra:' '    runs-on: ubuntu-latest' '    steps:' '      - run: "true"'; } > "$work/prwf/two-jobs.yml"
expect "pr-check: the job count check names a second job" "build-and-test extra" "$(pr_jobs "$work/prwf/two-jobs.yml")"
pr_needs() { grep -cE '^    needs:|needs\.[a-z_-]+\.outputs' "$1" || true; }
expect "pr-check: no job needs another and no step reads needs outputs" 0 "$(pr_needs "$pr_wf")"
awk '{ print } /^  build-and-test:$/ { print "    needs: changes" }' "$pr_wf" > "$work/prwf/needs.yml"
expect "pr-check: the needs check fails on a copy whose build-and-test needs a job" 1 "$(pr_needs "$work/prwf/needs.yml")"
# P3: the filter, then its check, before every step reading them; only checkout may come before (fallback, plan 3.4).
expect "pr-check: the path filter and its check come before every step that reads them" "yes yes yes yes 6" \
  "$(pr_filter_order "$pr_wf")"
awk '/^  build-and-test:$/ { j = 1 } j && /^      - name: Lọc đường dẫn/ { m = 1 } m && /^      - name: Kiểm kết quả lọc đường dẫn$/ { m = 0 }
  m { held = held $0 "\n"; next } { print } END { printf "%s", held }' "$pr_wf" > "$work/prwf/filter-last.yml"
expect "pr-check: the order check fails on a copy with the filter at the end of the job" no \
  "$(pr_filter_order "$work/prwf/filter-last.yml" | cut -d' ' -f4)"
awk '/^      - name: Lọc đường dẫn/ && !d { print "      - uses: actions/setup-java@v5"; print "        with:"; print "          java-version: 17"; print ""; d = 1 } { print }' \
  "$pr_wf" > "$work/prwf/java-first.yml"
expect "pr-check: the order check fails on a copy with setup-java before the filter" no \
  "$(pr_filter_order "$work/prwf/java-first.yml" | cut -d' ' -f3)"
awk '{ print } /^      - name: Kiểm kết quả lọc đường dẫn$/ { print "        if: always()" }' "$pr_wf" > "$work/prwf/check-if.yml"
expect "pr-check: the order check fails on a copy whose filter check has an if" no \
  "$(pr_filter_order "$work/prwf/check-if.yml" | cut -d' ' -f2)"
# P4
expect "pr-check: every filter output read is defined, and every filter is read" \
  "reads=code release defined=code release other=0" "$(pr_filter_outputs "$pr_wf")"
sed '0,/steps\.filter\.outputs\.code/s//steps.filter.outputs.cod/' "$pr_wf" > "$work/prwf/cod.yml"
expect "pr-check: the filter output check fails on a copy reading an undefined output" no \
  "$([[ "$(pr_filter_outputs "$work/prwf/cod.yml")" == "reads=code release defined=code release other=0" ]] && echo yes || echo no)"
sed 's/^        id: filter$/        id: paths/' "$pr_wf" > "$work/prwf/paths-id.yml"
expect "pr-check: the filter output check fails on a copy whose filter step has another id" no \
  "$([[ "$(pr_filter_outputs "$work/prwf/paths-id.yml")" == "reads=code release defined=code release other=0" ]] && echo yes || echo no)"
# P5
expect "pr-check: the filter check stops the job unless both outputs are true or false" "0 0 0 1 1 1 1 1 1" \
  "$(pr_filter_check "$pr_wf" true false) $(pr_filter_check "$pr_wf" false true) $(pr_filter_check "$pr_wf" false false) $(pr_filter_check "$pr_wf" '' false) $(pr_filter_check "$pr_wf" true '') $(pr_filter_check "$pr_wf" yes false)"
expect "pr-check: the filter check reads both filter outputs" "1 1" \
  "$(pr_steps "$pr_wf" | grep -F 'name: Kiểm kết quả lọc đường dẫn' | grep -cF 'CODE: ${{ steps.filter.outputs.code }}' || true) $(pr_steps "$pr_wf" | grep -F 'name: Kiểm kết quả lọc đường dẫn' | grep -cF 'RELEASE: ${{ steps.filter.outputs.release }}' || true)"
# P6: sample PRs through the real filter of the workflow.
expect "pr-check: a PR touching app code runs check.sh, not the docs-only path" \
  "code=true release=false gate=yes gradle=yes docs=no r8=no" "$(pr_sim_summary "$pr_wf" app/src/main/java/A.kt)"
for p in core/ads/src/main/A.kt feature/sample/src/test/B.kt build-logic/convention/C.kt gradle/libs.versions.toml \
  settings.gradle.kts gradle.properties gradlew .editorconfig scripts/merge-if-green.sh scripts/tests/run.sh \
  .github/workflows/pr-check.yml; do
  expect "pr-check: a PR touching $p runs check.sh" "code=true gate=yes docs=no" \
    "$(pr_sim_summary "$pr_wf" "$p" | sed -E 's/ release=[a-z]*//; s/ gradle=[a-z]*//; s/ r8=[a-z]*//')"
done
expect "pr-check: a docs-only PR runs only scripts/tests/run.sh" \
  "code=false release=false gate=no gradle=no docs=yes r8=no" "$(pr_sim_summary "$pr_wf" docs/a.md README.md)"
expect "pr-check: a PR changing another workflow is not code" \
  "code=false release=false gate=no gradle=no docs=yes r8=no" "$(pr_sim_summary "$pr_wf" .github/workflows/staging.yml)"
expect "pr-check: a PR changing a build file also builds the release" \
  "code=true release=true gate=yes gradle=yes docs=no r8=yes code=true release=true gate=yes gradle=yes docs=no r8=yes code=true release=true gate=yes gradle=yes docs=no r8=yes" \
  "$(pr_sim_summary "$pr_wf" app/build.gradle.kts) $(pr_sim_summary "$pr_wf" app/proguard-rules.pro) $(pr_sim_summary "$pr_wf" build.gradle.kts)"
expect "pr-check: **/ also matches a file at the root, *.gradle.kts only at the root" \
  "code=false release=true gate=no gradle=no docs=yes r8=no code=false release=false gate=no gradle=no docs=yes r8=no" \
  "$(pr_sim_summary "$pr_wf" proguard-rules.pro) $(pr_sim_summary "$pr_wf" docs/x.gradle.kts)"
expect "pr-check: one code file among docs makes the PR a code PR" \
  "code=true release=false gate=yes gradle=yes docs=no r8=no" "$(pr_sim_summary "$pr_wf" docs/a.md app/src/main/java/A.kt)"
awk '{ print } /^            code:$/ { print "              - '\''app/{a,b}/**'\''" }' "$pr_wf" > "$work/prwf/brace.yml"
expect "pr-check: the sample PR check refuses a pattern it does not support" "ERROR mẫu chưa hỗ trợ: app/{a,b}/**" \
  "$(pr_sim_summary "$work/prwf/brace.yml" docs/a.md)"
# Every other line of the filters block is refused too, so a pattern written another way is never skipped (T-094).
awk '{ print } $0 == "              - '\''feature/**'\''" { print "              - docs/**" }' "$pr_wf" > "$work/prwf/unquoted.yml"
awk '{ print } $0 == "              - '\''feature/**'\''" { print "              - \"docs/**\"" }' "$pr_wf" > "$work/prwf/dquoted.yml"
expect "pr-check: the sample PR check refuses an unquoted or double-quoted pattern" \
  "ERROR mẫu chưa hỗ trợ: - docs/** | ERROR mẫu chưa hỗ trợ: - \"docs/**\"" \
  "$(pr_sim_summary "$work/prwf/unquoted.yml" docs/a.md) | $(pr_sim_summary "$work/prwf/dquoted.yml" docs/a.md)"
awk '{ print } $0 == "          filters: |" { print "              - '\''docs/**'\''" }' "$pr_wf" > "$work/prwf/before-name.yml"
expect "pr-check: the sample PR check refuses a pattern before any filter name" "ERROR mẫu chưa hỗ trợ: - 'docs/**'" \
  "$(pr_sim_summary "$work/prwf/before-name.yml" docs/a.md)"
awk '{ print }
  $0 == "              - '\''app/**'\''" { print "              "; print "              # x" }
  $0 == "              - '\''.github/workflows/pr-check.yml'\''" && ++n == 2 { print "          list-files: none" }' \
  "$pr_wf" > "$work/prwf/key-after.yml"
expect "pr-check: a key after the filters block ends it, comments and blank lines inside are fine" \
  "code=true release=false gate=yes gradle=yes docs=no r8=no 1 1 1" \
  "$(pr_sim_summary "$work/prwf/key-after.yml" app/src/main/java/A.kt) $(grep -cx '          list-files: none' "$work/prwf/key-after.yml") $(grep -cx '              # x' "$work/prwf/key-after.yml") $(grep -cx '              ' "$work/prwf/key-after.yml")"
# The filter check step test reads its whole run block, blank lines included.
awk '{ print } /^      - / { s = 0 } $0 == "      - name: Kiểm kết quả lọc đường dẫn" { s = 1 }
  s && !d && $0 == "          done" { print ""; print "          exit 3"; d = 1 }' "$pr_wf" > "$work/prwf/run-blank.yml"
expect "pr-check: the filter check test reads the run block past a blank line" "3 1" \
  "$(pr_filter_check "$work/prwf/run-blank.yml" true false | cut -d' ' -f1) $(grep -cx '          exit 3' "$work/prwf/run-blank.yml")"
# P6d: controls on broken copies.
grep -vxF "              - 'scripts/**'" "$pr_wf" > "$work/prwf/no-scripts.yml"
expect "pr-check: the sample PR check sees a pattern removed from the code filter" "code=false" \
  "$(pr_sim_summary "$work/prwf/no-scripts.yml" scripts/merge-if-green.sh | cut -d' ' -f1)"
sed 's/steps\.filter\.outputs/needs.changes.outputs/g' "$pr_wf" > "$work/prwf/needs-outputs.yml"
expect "pr-check: the sample PR check refuses conditions on needs outputs" ERROR \
  "$(pr_sim_summary "$work/prwf/needs-outputs.yml" docs/a.md | cut -d' ' -f1)"
grep -vE '^(      - |        )id: filter$' "$pr_wf" > "$work/prwf/no-filter-id.yml"
expect "pr-check: the sample PR check refuses a workflow without the filter step" ERROR \
  "$(pr_sim_summary "$work/prwf/no-filter-id.yml" docs/a.md | cut -d' ' -f1)"
# A draft PR runs no step of build-and-test; once it is ready for review it runs check.sh (ADR-0041).
expect "pr-check: a draft PR runs no step of build-and-test, a ready one runs check.sh" \
  "code=true release=false gate=no gradle=no docs=no r8=no code=true release=false gate=yes gradle=yes docs=no r8=no" \
  "$(PR_SIM_DRAFT=true pr_sim_summary "$pr_wf" app/src/main/java/A.kt) $(PR_SIM_DRAFT=false pr_sim_summary "$pr_wf" app/src/main/java/A.kt)"
awk '/^    if: / { next } { print } /^  build-and-test:$/ { print "    if: github.event_name == '\''pull_request'\''" }' \
  "$pr_wf" > "$work/prwf/job-odd.yml"
expect "pr-check: the sample PR check refuses another job-level condition" \
  "ERROR điều kiện lạ cấp job: github.event_name == 'pull_request'" "$(pr_sim_summary "$work/prwf/job-odd.yml" app/src/main/java/A.kt)"
expect "pr-check: the sample PR check refuses a second job-level condition" "ERROR nhiều điều kiện cấp job" \
  "$(pr_sim_summary "$work/prwf/job-if.yml" app/src/main/java/A.kt)"
# G1: skipping the filter step must not make the Gradle check always pass.
awk '/^      - name: Kiểm kết quả lọc đường dẫn$/ { k = 1; print; next }
  k && /^      - / { print "      - name: Thử Gradle"; print "        run: ./gradlew help"; print ""; k = 0 } { print }' \
  "$pr_wf" > "$work/prwf/gradle-after-filter.yml"
expect "pr-check: the Gradle check still fails on a copy with an unconditional gradlew step after the filter" no \
  "$(pr_gradle_gated "$work/prwf/gradle-after-filter.yml")"
# The runner check of build-and-test runs only on a self-hosted runner, right after checkout (ADR-0042).
pr_sim_has() { # <workflow> <step name> <path>...; how many lines of the pr_sim output are that step, or the ERROR line
  local wf="$1" name="$2" out
  shift 2
  out="$(pr_sim "$wf" "$@")"
  if grep -q '^ERROR' <<<"$out"; then grep -m1 '^ERROR' <<<"$out"; return; fi
  grep -cxF "$name" <<<"$out" || true
}
pr_runner_step='Kiểm runner tự host (scripts/runner-check.sh)'
expect "pr-check: the sample PR runs the runner check only on a self-hosted runner" "0 1 1" \
  "$(pr_sim_has "$pr_wf" "$pr_runner_step" app/src/main/java/A.kt) $(PR_SIM_RUNNER=self-hosted pr_sim_has "$pr_wf" "$pr_runner_step" app/src/main/java/A.kt) $(PR_SIM_RUNNER=self-hosted pr_sim_has "$pr_wf" "$pr_runner_step" docs/a.md)"
expect "pr-check: on a self-hosted runner the runner check is the step right after checkout" "$pr_runner_step" \
  "$(PR_SIM_RUNNER=self-hosted pr_sim "$pr_wf" app/src/main/java/A.kt | awk 'p { print; exit } $0 == "actions/checkout@v5" { p = 1 }')"
expect "pr-check: a self-hosted runner does not change what a PR with code runs" \
  "code=true release=false gate=yes gradle=yes docs=no r8=no" \
  "$(PR_SIM_RUNNER=self-hosted pr_sim_summary "$pr_wf" app/src/main/java/A.kt)"
pr_runner_copy() { # <copy> <condition>: the workflow with one more runner check step, under that condition, after checkout
  COND="$2" awk '{ print } !d && $0 == "      - uses: actions/checkout@v5" { print ""; print "      - name: Kiểm runner tự host (scripts/runner-check.sh)"; print "        if: " ENVIRON["COND"]; print "        run: scripts/runner-check.sh"; d = 1 }' \
    "$pr_wf" > "$work/prwf/$1"
}
pr_runner_copy runner-typo.yml "runner.enviroment == 'self-hosted'"
pr_runner_copy runner-os.yml "runner.os == 'Linux'"
pr_runner_copy runner-github.yml "runner.environment == 'github-hosted'"
expect "pr-check: the sample PR check refuses a misspelt runner condition" \
  "ERROR điều kiện lạ: runner.enviroment == 'self-hosted'" \
  "$(pr_sim_has "$work/prwf/runner-typo.yml" "$pr_runner_step" app/src/main/java/A.kt)"
expect "pr-check: the sample PR check refuses another runner property" "ERROR điều kiện lạ: runner.os == 'Linux'" \
  "$(pr_sim_has "$work/prwf/runner-os.yml" "$pr_runner_step" app/src/main/java/A.kt)"
expect "pr-check: the sample PR check refuses an unknown PR_SIM_RUNNER" "ERROR PR_SIM_RUNNER lạ: mac" \
  "$(PR_SIM_RUNNER=mac pr_sim_has "$pr_wf" "$pr_runner_step" app/src/main/java/A.kt)"
expect "pr-check: the sample PR check sees a runner check that runs on GitHub runners" 1 \
  "$(pr_sim_has "$work/prwf/runner-github.yml" "$pr_runner_step" app/src/main/java/A.kt)"
# The emulator runs after the merge, in the ui-test job of main-guard.yml (ADR-0043): no PR runs it.
pr_no_emulator() { # <workflow>: "yes" when it has no ui-test job, no emulator step, no ui output and no ui filter
  if grep -qE '^  ui-test:$|android-emulator-runner|ui: \$\{\{|^            ui:$' "$1"; then echo no; else echo yes; fi
}
{ cat "$pr_wf"; printf '%s\n' '' '  ui-test:' '    runs-on: ubuntu-latest' '    steps:' \
  '      - uses: reactivecircus/android-emulator-runner@v2'; } > "$work/prwf/emulator.yml"
expect "pr-check: has no emulator job" "yes no" "$(pr_no_emulator "$pr_wf") $(pr_no_emulator "$work/prwf/emulator.yml")"

# main-guard-verified.sh (ADR-0039, T-074): which check main-guard runs for a commit of main. full re-runs
# scripts/check.sh, tests runs only scripts/tests/run.sh, skip trusts the full check pr-check already ran on the same tree.
# A temporary repo holds A (the parent on main), H (the PR head pr-check checked, A plus the PR's change) and M (the
# squash commit on main, parent A). Its fetch tests go to a local bare repo, so the fake origin URL is unset here.
mgv_git_config=("$GIT_CONFIG_COUNT" "$GIT_CONFIG_KEY_0" "$GIT_CONFIG_VALUE_0")
unset GIT_CONFIG_COUNT GIT_CONFIG_KEY_0 GIT_CONFIG_VALUE_0
mgv_script="$PWD/scripts/main-guard-verified.sh"
mgv_repo="$work/mgv"
mgv_write() { # <path=content>...: writes files in the temporary repo
  local pair
  for pair in "$@"; do
    mkdir -p "$mgv_repo/$(dirname "${pair%%=*}")"
    printf '%s\n' "${pair#*=}" > "$mgv_repo/${pair%%=*}"
  done
}
mgv_setup() { # [<path=content> changed by the PR too]...: a new repo with A and H; sets mgv_a, mgv_h
  rm -rf "$mgv_repo"
  git init -q -b main "$mgv_repo"
  mgv_write app/x.kt=a docs/a.md=a scripts/check.sh=gate
  git -C "$mgv_repo" add -A && git -C "$mgv_repo" commit -q -m A
  mgv_a="$(git -C "$mgv_repo" rev-parse HEAD)"
  mgv_write app/x.kt=h "$@"
  git -C "$mgv_repo" add -A && git -C "$mgv_repo" commit -q -m H
  mgv_h="$(git -C "$mgv_repo" rev-parse HEAD)"
}
mgv_main() { # [<path=content> added on main after the check]...: prints M, a commit on A with H's tree plus those files
  local tree
  mgv_write "$@"
  git -C "$mgv_repo" add -A
  tree="$(git -C "$mgv_repo" write-tree)"
  git -C "$mgv_repo" reset -q --hard "$mgv_h"
  git -C "$mgv_repo" commit-tree "$tree" -p "$mgv_a" -m squash
}
mgv_fx() { # <M> <H> [<jq filter>]: the GitHub answers for a green PR #7 whose build-and-test ran check.sh
  jq -n --arg m "$1" --arg h "$2" '{routes: {
    ("commits/" + $m + "/pulls"): [{number: 7, merged_at: "2026-10-29T00:00:00Z", base: {ref: "main"},
      merge_commit_sha: $m, head: {sha: $h}}],
    ("actions/workflows/pr-check.yml/runs?head_sha=" + $h + "&per_page=10"): {workflow_runs: [
      {id: 91, created_at: "2026-10-29T00:00:00Z", conclusion: "success"}]},
    "actions/runs/91/jobs?per_page=50": {jobs: [{name: "build-and-test", steps: [
      {name: "Set up job", conclusion: "success"},
      {name: "Cổng chất lượng (scripts/check.sh)", conclusion: "success"}]}]}}} | '"${3:-.}"
}
mgv() { # <fixture> <sha> [<event> [<run attempt>]]: prints "<exit code> <stdout>"; stderr goes to $work/mgv.err
  local out code=0
  # A fixture that is not JSON (a broken jq filter) would make every case full: refuse it instead.
  jq -e . <<<"$1" > /dev/null 2>&1 || { echo "bad fixture"; return; }
  printf '%s' "$1" > "$work/mgv.json"
  rm -f "$work/mgv.json.seen"
  out="$(cd "$mgv_repo" && PATH="$work/bin:$PATH" FIXTURE="$work/mgv.json" GITHUB_REPOSITORY=o/r PULLS_WAIT_SECONDS=0 \
    "$mgv_script" "$2" ${3:+"$3"} ${4:+"$4"} 2> "$work/mgv.err")" || code=$?
  echo "$code $out"
}

mgv_setup
m="$(mgv_main)"
expect "main-guard-verified: same tree as a PR that ran check.sh is skip" "0 skip" "$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m")"
expect "main-guard-verified: skip names the PR and the checked commit" "1 1" \
  "$(grep -c 'ĐÃ KIỂM Ở PR #7' "$work/mgv.err") $(grep -c "${mgv_h:0:7}" "$work/mgv.err")"
expect "main-guard-verified: a run of push, first attempt, is skip" "0 skip" "$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m" push 1)"
expect "main-guard-verified: workflow_dispatch is full" "0 full" "$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m" workflow_dispatch)"
expect "main-guard-verified: a re-run is full" "0 full" "$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m" push 2)"

m="$(mgv_main docs/b.md=b)"
expect "main-guard-verified: only docs differ from the checked PR is tests" "0 tests" "$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m")"
expect "main-guard-verified: tests says so" 1 "$(grep -c 'CHỈ KHÁC TÀI LIỆU' "$work/mgv.err")"
m="$(mgv_main README.md=b)"
expect "main-guard-verified: a .md outside docs/ is tests" "0 tests" "$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m")"
m="$(mgv_main docs/remote-config-keys.md=b)"
expect "main-guard-verified: a docs file Gradle reads is full (remote-config-keys.md)" "0 full" \
  "$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m")"
m="$(mgv_main docs/design-system/tokens.json=b)"
expect "main-guard-verified: a docs file Gradle reads is full (tokens.json)" "0 full" "$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m")"
m="$(mgv_main app/y.kt=b)"
expect "main-guard-verified: code differs from the checked PR is full" "0 full" "$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m")"
expect "main-guard-verified: full says why" 1 "$(grep -c 'KIỂM ĐẦY ĐỦ' "$work/mgv.err")"
# git diff names only the new path of a renamed file unless renames are off: code moved into docs/ is still code.
git -C "$mgv_repo" mv app/x.kt docs/x.md
m="$(mgv_main)"
expect "main-guard-verified: code renamed into docs/ is full" "0 full" "$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m")"
mgv_setup
git -C "$mgv_repo" mv scripts/check.sh docs/check.md
git -C "$mgv_repo" commit -q -m H2
mgv_h="$(git -C "$mgv_repo" rev-parse HEAD)"
m="$(mgv_main)"
expect "main-guard-verified: a commit renaming a gate file is full" "0 full" "$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m")"
mgv_setup

m="$(mgv_main)"
step='.routes["actions/runs/91/jobs?per_page=50"].jobs[0].steps[1]'
expect "main-guard-verified: a PR that skipped check.sh is full" "0 full" \
  "$(mgv "$(mgv_fx "$m" "$mgv_h" "$step.conclusion = \"skipped\"")" "$m")"
expect "main-guard-verified: a renamed gate step is full" "0 full" \
  "$(mgv "$(mgv_fx "$m" "$mgv_h" "$step.name = \"Cổng chất lượng\"")" "$m")"
expect "main-guard-verified: a renamed gate job is full" "0 full" \
  "$(mgv "$(mgv_fx "$m" "$mgv_h" '.routes["actions/runs/91/jobs?per_page=50"].jobs[0].name = "build"')" "$m")"
runs='.routes["actions/workflows/pr-check.yml/runs?head_sha=" + $h + "&per_page=10"].workflow_runs'
expect "main-guard-verified: a red pr-check run is full" "0 full" \
  "$(mgv "$(mgv_fx "$m" "$mgv_h" "$runs[0].conclusion = \"failure\"")" "$m")"
expect "main-guard-verified: no pr-check run is full" "0 full" "$(mgv "$(mgv_fx "$m" "$mgv_h" "$runs = []")" "$m")"
# Filters with braces are built in variables first: bash 3.2 (macOS) mangles them inside nested "$(...)" .
newer_red="$runs += [{id: 92, created_at: \"2026-10-29T01:00:00Z\", conclusion: \"failure\"}]"
older_red="$runs = [{id: 90, created_at: \"2026-10-28T00:00:00Z\", conclusion: \"failure\"}] + $runs"
expect "main-guard-verified: the newest pr-check run counts (newer red)" "0 full" "$(mgv "$(mgv_fx "$m" "$mgv_h" "$newer_red")" "$m")"
expect "main-guard-verified: the newest pr-check run counts (older red)" "0 skip" "$(mgv "$(mgv_fx "$m" "$mgv_h" "$older_red")" "$m")"
pulls='.routes["commits/" + $m + "/pulls"]'
expect "main-guard-verified: a commit without a merged PR is full" "0 full" "$(mgv "$(mgv_fx "$m" "$mgv_h" "$pulls = []")" "$m")"
expect "main-guard-verified: a PR merged as another commit is full" "0 full" \
  "$(mgv "$(mgv_fx "$m" "$mgv_h" "$pulls[0].merge_commit_sha = \"$mgv_a\"")" "$m")"
expect "main-guard-verified: a PR that is not merged is full" "0 full" \
  "$(mgv "$(mgv_fx "$m" "$mgv_h" "$pulls[0].merged_at = null")" "$m")"
expect "main-guard-verified: a PR into another branch is full" "0 full" \
  "$(mgv "$(mgv_fx "$m" "$mgv_h" "$pulls[0].base.ref = \"release\"")" "$m")"
late_once="$pulls = {__sequence__: [[], $pulls]}"
late_thrice="$pulls = {__sequence__: [[], [], [], $pulls]}"
expect "main-guard-verified: asks again while GitHub has not linked the PR yet" "0 skip" "$(mgv "$(mgv_fx "$m" "$mgv_h" "$late_once")" "$m")"
expect "main-guard-verified: stops asking after PULLS_TRIES" "0 full" "$(mgv "$(mgv_fx "$m" "$mgv_h" "$late_thrice")" "$m")"
pulls_403='.errors = {("commits/" + $m + "/pulls"): "HTTP 403: Resource not accessible by integration"}'
expect "main-guard-verified: an unreadable PR list is full, exit 0" "0 full" "$(mgv "$(mgv_fx "$m" "$mgv_h" "$pulls_403")" "$m")"
jobs_500='.errors = {"actions/runs/91/jobs?per_page=50": "HTTP 500: Server Error"}'
expect "main-guard-verified: an unreadable job list is full, exit 0" "0 full" "$(mgv "$(mgv_fx "$m" "$mgv_h" "$jobs_500")" "$m")"
expect "main-guard-verified: a commit unknown to git is full, exit 0" "0 full" \
  "$(mgv "$(mgv_fx "$m" "$mgv_h")" 0123456789abcdef0123456789abcdef01234567)"

mgv_gate_files=(.github/workflows/pr-check.yml .github/workflows/main-guard.yml scripts/check.sh scripts/failure-kind.sh
  scripts/find-breaking-commit.sh scripts/main-guard-verified.sh scripts/tests/run.sh scripts/tests/fake-gh
  scripts/ui-test-needed.sh scripts/runner-check.sh)
gate_full=0
for f in "${mgv_gate_files[@]}"; do
  mgv_setup "$f=changed"
  m="$(mgv_main)"
  [[ "$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m")" == "0 full" ]] && gate_full=$((gate_full + 1))
done
expect "main-guard-verified: a commit changing a file the guard job runs is full" "${#mgv_gate_files[@]}" "$gate_full"
mgv_setup scripts/status.py=changed
m="$(mgv_main)"
expect "main-guard-verified: a commit changing another script can be skip" "0 skip" "$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m")"

# On GitHub H is not in the checkout of main (the squash commit does not descend from it): it comes from refs/pull/7/head.
mgv_setup
m="$(mgv_main)"
git -C "$mgv_repo" branch -q -f squash "$m"
rm -rf "$work/mgv-origin.git" "$work/mgv-main"
git init -q --bare "$work/mgv-origin.git"
git init -q -b main "$work/mgv-main"
git -C "$work/mgv-main" fetch -q "$mgv_repo" refs/heads/squash
git -C "$work/mgv-main" reset -q --hard FETCH_HEAD
git -C "$work/mgv-main" remote add origin "file://$work/mgv-origin.git"
mgv_in_main() { # <fixture>: runs the script in the clone without H
  local saved="$mgv_repo" out
  mgv_repo="$work/mgv-main"
  out="$(mgv "$1" "$m")"
  mgv_repo="$saved"
  echo "$out"
}
expect "main-guard-verified: the clone of main does not have H" no \
  "$(git -C "$work/mgv-main" cat-file -e "$mgv_h^{commit}" 2>/dev/null && echo yes || echo no)"
expect "main-guard-verified: no refs/pull/7/head is full" "0 full" "$(mgv_in_main "$(mgv_fx "$m" "$mgv_h")")"
git -C "$mgv_repo" push -q "$work/mgv-origin.git" "$mgv_a:refs/pull/7/head"
expect "main-guard-verified: refs/pull/7/head on another commit is full" "0 full" "$(mgv_in_main "$(mgv_fx "$m" "$mgv_h")")"
git -C "$mgv_repo" push -q -f "$work/mgv-origin.git" "$mgv_h:refs/pull/7/head"
expect "main-guard-verified: fetches H from refs/pull/7/head and skips" "0 skip" "$(mgv_in_main "$(mgv_fx "$m" "$mgv_h")")"

# The names the script reads from pr-check.yml, and the docs files Gradle reads, stay in step with those files.
mgv_gate_names() { # <script> <workflow>: "yes" when the workflow has the script's GATE_JOB with a step named GATE_STEP
  local job step
  job="$(sed -n -E 's/^GATE_JOB="(.*)"$/\1/p' "$1")"
  step="$(sed -n -E 's/^GATE_STEP="(.*)"$/\1/p' "$1")"
  if [[ "$job" == build-and-test && -n "$step" ]] && grep -qE "^  $job:$" "$2" \
    && pr_steps "$2" | grep -qF "name: $step"; then echo yes; else echo no; fi
}
expect "main-guard-verified: pr-check.yml has the job and the step it reads" yes "$(mgv_gate_names "$mgv_script" "$pr_wf")"
sed 's/name: Cổng chất lượng (scripts\/check.sh)/name: Cổng chất lượng/' "$pr_wf" > "$work/prwf/gate-step.yml"
expect "main-guard-verified: the name check fails on a copy with a renamed gate step" no \
  "$(mgv_gate_names "$mgv_script" "$work/prwf/gate-step.yml")"
mgv_gradle_docs() { # <script>: "yes" when GRADLE_READ_DOCS lists exactly the docs files of rootProject.file("docs/...")
  local listed read
  listed="$(sed -n -E 's/^GRADLE_READ_DOCS=\((.*)\)$/\1/p' "$1" | tr ' ' '\n' | sed '/^$/d' | sort)"
  read="$(git grep -hoE 'rootProject\.file\("docs/[^"]+"\)' -- '*.gradle.kts' | sed -E 's/^rootProject\.file\("(.*)"\)$/\1/' | sort -u)"
  [[ -n "$read" && "$listed" == "$read" ]] && echo yes || echo no
}
expect "main-guard-verified: GRADLE_READ_DOCS lists every docs file Gradle reads" yes "$(mgv_gradle_docs "$mgv_script")"
sed 's# docs/design-system/tokens.json##' "$mgv_script" > "$work/mgv-short.sh" 2>/dev/null || true
expect "main-guard-verified: the Gradle docs check fails on a copy missing a file" no "$(mgv_gradle_docs "$work/mgv-short.sh")"

# main-guard.yml chooses the mode before the gate, inside the guard job, so the check run guard still ends success or
# failure and every reader of it (find-breaking-commit.sh, main-guard-status.sh, start-run.yml, release.sh) is unchanged.
mg_steps() { # <workflow>; prints each step of the guard job on one line, its lines joined by tabs
  awk '/^  guard:$/ { j = 1; next }
    j && /^  [^ ]/ { j = 0 }
    j && /^      - / { if (s != "") print s; s = $0; next }
    j && s != "" && /^        / { s = s "\t" $0 }
    END { if (s != "") print s }' "$1"
}
mg_step_has() { # <workflow> <text in the step> <text that must be in the same step>: yes or no
  mg_steps "$1" | grep -F -- "$2" | grep -qF -- "$3" && echo yes || echo no
}
mg_wiring() { # <workflow>; prints yes or no for the points (a) to (j) of the plan T-074, section 4, test 13
  local wf="$1" a b f j
  a="$(awk '/^  guard:$/ { g = 1; next } g && /^    steps:$/ { g = 0 } g && /^    if:/ { n++; if ($0 ~ /github\.ref == .refs\/heads\/main./) r++ }
    END { print ((n == 1 && r == 1) ? "yes" : "no") }' "$wf")"
  b="$(awk '/^  guard:$/ { g = 1 } g && /uses: actions\/checkout/ && !c { c = NR }
    g && /scripts\/main-guard-verified\.sh "\$\{GITHUB_SHA\}" "\$\{GITHUB_EVENT_NAME\}" "\$\{GITHUB_RUN_ATTEMPT\}"/ && !m { m = NR }
    g && /id: first$/ && !f { f = NR }
    END { print ((c && m > c && f > m) ? "yes" : "no") }' "$wf")"
  f="$(mg_steps "$wf" | grep -F 'id: check' | awk -F'\t' '{ s = index($0, "= \"skip\" ]; then"); o = index($0, "steps.first.outcome") }
    END { print ((s && o && s < o) ? "yes" : "no") }')"
  j="$(grep -qE '^  actions: read$' "$wf" && grep -qE '^  pull-requests: read$' "$wf" && echo yes || echo no)"
  echo "$a $b" \
    "$(mg_step_has "$wf" 'uses: actions/setup-java' "if: steps.mode.outputs.mode != 'skip'")" \
    "$(mg_step_has "$wf" 'uses: gradle/actions/setup-gradle' "if: steps.mode.outputs.mode != 'skip' && steps.mode.outputs.mode != 'tests'")" \
    "$( [[ "$(mg_step_has "$wf" 'id: first' "if: steps.mode.outputs.mode != 'skip'") $(mg_step_has "$wf" 'id: first' '= "tests" ]; then scripts/tests/run.sh 2>&1 | tee build.log; else scripts/check.sh 2>&1 | tee build.log; fi')" == "yes yes" ]] && echo yes || echo no)" \
    "$f" \
    "$( [[ "$(mg_step_has "$wf" 'id: kind' "if: steps.first.outcome == 'failure'") $(mg_step_has "$wf" 'id: retry' "if: steps.kind.outputs.kind == 'tests'")" == "yes yes" ]] && echo yes || echo no)" \
    "$(mg_step_has "$wf" 'name: Đánh dấu nhánh chính đang đỏ' "if: failure() && steps.check.outcome == 'failure'")" \
    "$(mg_step_has "$wf" 'name: Đóng issue main-broken khi nhánh chính xanh trở lại' 'if: success()')" \
    "$j"
}
expect "main-guard: chooses the mode before the gate and skips inside the guard job" \
  "yes yes yes yes yes yes yes yes yes yes" "$(mg_wiring "$guard_wf")"
mkdir -p "$work/mgwf"
awk '{ print } /^    if: github.ref == .refs\/heads\/main.$/ { print "    if: steps.mode.outputs.mode != '\''skip'\''" }' \
  "$guard_wf" > "$work/mgwf/a.yml"
grep -vF 'scripts/main-guard-verified.sh' "$guard_wf" > "$work/mgwf/b.yml"
sed "s/steps.mode.outputs.mode != 'skip' \&\& steps.mode.outputs.mode != 'tests'/steps.mode.outputs.mode == 'full'/" \
  "$guard_wf" > "$work/mgwf/d.yml"
sed 's/= "tests" ]; then scripts\/tests\/run.sh/= "full" ]; then scripts\/tests\/run.sh/' "$guard_wf" > "$work/mgwf/e.yml"
sed 's/= "skip" ]; then/= "none" ]; then/' "$guard_wf" > "$work/mgwf/f.yml"
grep -vE '^  pull-requests: read$' "$guard_wf" > "$work/mgwf/j.yml"
expect "main-guard: the wiring check fails on each broken copy (a, b, d, e, f, j)" "no no no no no no" \
  "$(mg_wiring "$work/mgwf/a.yml" | cut -d' ' -f1) $(mg_wiring "$work/mgwf/b.yml" | cut -d' ' -f2) $(mg_wiring "$work/mgwf/d.yml" | cut -d' ' -f4) $(mg_wiring "$work/mgwf/e.yml" | cut -d' ' -f5) $(mg_wiring "$work/mgwf/f.yml" | cut -d' ' -f6) $(mg_wiring "$work/mgwf/j.yml" | cut -d' ' -f10)"

# main-guard.yml runs the emulator in its own job ui-test, after guard and only for app code (ADR-0043, T-075, test 18).
ui_steps() { # <workflow>; prints each step of the ui-test job on one line, its lines joined by tabs
  awk '/^  ui-test:$/ { j = 1; next }
    j && /^  [^ ]/ { j = 0 }
    j && /^      - / { if (s != "") print s; s = $0; next }
    j && s != "" && /^        / { s = s "\t" $0 }
    END { if (s != "") print s }' "$1"
}
step_by_id() { # <id>: from the step lines on stdin, the step with that id
  awk -F'\t' -v id="$1" '{ for (i = 1; i <= NF; i++) if ($i ~ "^ +(- )?id: " id "$") { print; next } }'
}
step_by_name() { # <name>: from the step lines on stdin, the step with exactly that name
  awk -F'\t' -v n="$1" '{ for (i = 1; i <= NF; i++) if ($i ~ /^ +(- )?name: / && substr($i, index($i, "name: ") + 6) == n) { print; next } }'
}
step_text() { grep -qF -- "$2" <<<"$1" && echo yes || echo no; } # <step> <text>
step_has_if() { awk -F'\t' '{ for (i = 1; i <= NF; i++) if ($i ~ /^ +(- )?if:/) f = 1 } END { print (f ? "yes" : "no") }' <<<"$1"; }
step_run() { # <step>: the lines of its "run: |" block, without their indentation
  awk -F'\t' '{ for (i = 1; i <= NF; i++) { if (r) { l = $i; sub(/^          /, "", l); print l } if ($i ~ /^ +run: \|$/) r = 1 } }' <<<"$1"
}
step_all() { # <step> <text>...: yes when the step has every text
  local s="$1" t
  shift
  [[ -n "$s" ]] || { echo no; return; }
  for t in "$@"; do [[ "$(step_text "$s" "$t")" == yes ]] || { echo no; return; }; done
  echo yes
}
ui_own_filter='select(.body | contains("<!-- check:ui-test -->"))'
ui_wiring() { # <workflow>; prints yes or no for the points (a) to (i) of the plan T-075, section 4, test 18
  local wf="$1" a b c d e f g h i step body v code codes=""
  a="$(awk '/^  guard:$/ { g = NR } /^  ui-test:$/ { u = NR; j = 1; next } j && /^    steps:$/ { j = 0 }
    j && /^    needs: guard$/ { n++ } j && /^    if:/ { i++; if ($0 == "    if: needs.guard.outputs.ui != '\''no'\''") r++ }
    END { print ((g && u > g && n == 1 && i == 1 && r == 1) ? "yes" : "no") }' "$wf")"
  b="$(awk '/^  guard:$/ { g = 1 } /^  ui-test:$/ { g = 0 } g && /^    outputs:$/ { o = 1 } g && /^    steps:$/ { o = 0 }
    g && o && /^      ui: \$\{\{ steps\.ui\.outputs\.ui \}\}$/ { u = 1 }
    g && /uses: actions\/checkout/ && !c { c = NR }
    g && /scripts\/ui-test-needed\.sh "\$\{GITHUB_SHA\}" "\$\{GITHUB_EVENT_NAME\}"/ && !s { s = NR }
    END { print ((u && c && s > c) ? "yes" : "no") }' "$wf")"
  step="$(mg_steps "$wf" | step_by_id ui)"
  [[ "$(step_all "$step" 'scripts/ui-test-needed.sh "${GITHUB_SHA}" "${GITHUB_EVENT_NAME}"')" == yes \
    && "$(step_has_if "$step")" == no ]] || b=no
  c="$(step_all "$(ui_steps "$wf" | step_by_id emulator)" 'continue-on-error: true' \
    'uses: reactivecircus/android-emulator-runner@v2' 'script: scripts/ui-test-on-emulator.sh')"
  d="$(step_all "$(ui_steps "$wf" | step_by_id verdict)" 'if: always()' \
    'scripts/ui-test-verdict.sh "${{ steps.emulator.outcome }}"')"
  # The check step is run for each verdict: only green and flaky may exit 0.
  body="$(step_run "$(ui_steps "$wf" | step_by_id check)")"
  mkdir -p "$work/uicheck"
  printf 'EMULATOR HẠ TẦNG (PB-11): x\nlog line\n' > "$work/uicheck/ui-verdict.txt"
  for v in green flaky infra red ''; do
    code=0
    (cd "$work/uicheck" && VERDICT="$v" bash -c "$body") > /dev/null 2>&1 || code=1
    codes="$codes$code"
  done
  [[ -n "$body" && "$codes" == 00111 ]] && e=yes || e=no
  f="$(step_all "$(ui_steps "$wf" | step_by_name 'Ghi test chập chờn')" "if: steps.verdict.outputs.verdict == 'flaky'")"
  g="$(step_all "$(ui_steps "$wf" | step_by_name 'Đánh dấu nhánh chính đang đỏ (emulator)')" \
    "if: failure() && steps.verdict.outputs.verdict == 'red'" 'MAX_WAIT_SECONDS=900' \
    'scripts/find-breaking-commit.sh --check ui-test' 'echo "<!-- check:ui-test -->"' "$ui_own_filter")"
  h="$(step_all "$(ui_steps "$wf" | step_by_name 'Đóng issue main-broken của emulator khi xanh trở lại')" \
    'if: success()' "$ui_own_filter")"
  i="$(awk '/^  ui-test:$/ { j = 1; next } j && /^    steps:$/ { j = 0 } j && /^    timeout-minutes: [0-9]+$/ { t = 1 }
    END { print (t ? "yes" : "no") }' "$wf")"
  echo "$a $b $c $d $e $f $g $h $i"
}
expect "main-guard: ui-test runs after guard only for app code" "yes yes yes yes yes yes yes yes yes" "$(ui_wiring "$guard_wf")"
grep -vE '^    needs: guard$' "$guard_wf" > "$work/mgwf/ui-a1.yml"
sed "s/needs.guard.outputs.ui != 'no'/needs.guard.outputs.ui == 'yes'/" "$guard_wf" > "$work/mgwf/ui-a2.yml"
awk '{ print } /^        id: ui$/ { print "        if: steps.mode.outputs.mode != '\''skip'\''" }' "$guard_wf" > "$work/mgwf/ui-b.yml"
awk '/id: emulator$/ { e = 1 } e && /continue-on-error: true/ { e = 0; next } { print }' "$guard_wf" > "$work/mgwf/ui-c.yml"
awk '/id: verdict$/ { v = 1 } v && /if: always\(\)/ { sub(/always\(\)/, "success()"); v = 0 } { print }' "$guard_wf" \
  > "$work/mgwf/ui-d.yml"
sed 's/green | flaky)/green | flaky | infra)/' "$guard_wf" > "$work/mgwf/ui-e.yml"
sed "s/if: steps.verdict.outputs.verdict == 'flaky'/if: always()/" "$guard_wf" > "$work/mgwf/ui-f.yml"
sed 's/find-breaking-commit.sh --check ui-test/find-breaking-commit.sh/' "$guard_wf" > "$work/mgwf/ui-g1.yml"
perl -0pe 's/\Qselect(.body | contains("<!-- check:ui-test -->"))\E/select(true)/' "$guard_wf" > "$work/mgwf/ui-g2.yml"
perl -0pe '$n = 0; s/\Qselect(.body | contains("<!-- check:ui-test -->"))\E/++$n == 2 ? "select(true)" : $&/ge' \
  "$guard_wf" > "$work/mgwf/ui-h.yml"
awk '/^  ui-test:$/ { u = 1 } u && /^    timeout-minutes:/ { u = 0; next } { print }' "$guard_wf" > "$work/mgwf/ui-i.yml"
ui_point() { ui_wiring "$work/mgwf/ui-$1.yml" | cut -d' ' -f"$2"; } # <copy> <field>
# Closing the issue is a note for people: a GitHub error there must not turn a green ui-test red without annotation,
# which find-breaking-commit.sh --check ui-test would blame (review of T-075, round 1).
expect "main-guard: the close step of ui-test cannot turn the job red" yes \
  "$(step_all "$(ui_steps "$guard_wf" | step_by_name 'Đóng issue main-broken của emulator khi xanh trở lại')" 'continue-on-error: true')"
expect "main-guard: the ui-test wiring check fails on each broken copy (a, a, b, c, d, e, f, g, g, h, i)" \
  "no no no no no no no no no no no" \
  "$(ui_point a1 1) $(ui_point a2 1) $(ui_point b 2) $(ui_point c 3) $(ui_point d 4) $(ui_point e 5) $(ui_point f 6) $(ui_point g1 7) $(ui_point g2 7) $(ui_point h 8) $(ui_point i 9)"
# guard comments on and closes only its own main-broken issues, never the one of ui-test (test 19).
guard_own_filter='select(.body | contains("<!-- check:ui-test -->") | not)'
mg_own_issues() { # <workflow>: yes or no for the mark step and the close step of guard
  echo "$(step_all "$(mg_steps "$1" | step_by_name 'Đánh dấu nhánh chính đang đỏ')" "$guard_own_filter")" \
    "$(step_all "$(mg_steps "$1" | step_by_name 'Đóng issue main-broken khi nhánh chính xanh trở lại')" "$guard_own_filter")"
}
perl -0pe 's/\Qselect(.body | contains("<!-- check:ui-test -->") | not)\E/select(true)/' "$guard_wf" > "$work/mgwf/own-mark.yml"
perl -0pe '$n = 0; s/\Qselect(.body | contains("<!-- check:ui-test -->") | not)\E/++$n == 2 ? "select(true)" : $&/ge' \
  "$guard_wf" > "$work/mgwf/own-close.yml"
expect "main-guard: guard comments on and closes only its own main-broken issues" "yes yes" "$(mg_own_issues "$guard_wf")"
expect "main-guard: the own-issue check fails on a copy without the filter in each step" "no yes yes no" \
  "$(mg_own_issues "$work/mgwf/own-mark.yml") $(mg_own_issues "$work/mgwf/own-close.yml")"
# merge-if-green.sh asks main-guard-verified.sh three questions before it merges a PR that is behind main without
# updating it (ADR-0041): are these paths docs only (--docs-only), are they all in the paths-ignore of main-guard
# (--guard-ignores), and did pr-check run check.sh on this commit (--gate-ran). The answers follow the very rules
# main-guard applies to the merge commit.
mgv_docs_only() { # <path>...: the answer of --docs-only to those paths, one per line on stdin
  printf '%s\n' "$@" | "$mgv_script" --docs-only 2> /dev/null
}
mgv_rows=(docs/a.md README.md .claude/skills/x/SKILL.md docs/remote-config-keys.md docs/remote-config-template.json
  docs/design-system/tokens.json docs/x.json app/x.kt scripts/a.sh docs.kt)
mgv_answers="" mgv_agree=0
for f in "${mgv_rows[@]}"; do
  answer="$(mgv_docs_only "$f")"
  mgv_answers="$mgv_answers${mgv_answers:+ }$answer"
  mgv_setup
  m="$(mgv_main "$f=b")"
  mode="$(mgv "$(mgv_fx "$m" "$mgv_h")" "$m")"
  [[ ("$answer" == yes && "$mode" == "0 tests") || ("$answer" == no && "$mode" == "0 full") ]] && mgv_agree=$((mgv_agree + 1))
done
expect "main-guard-verified: --docs-only agrees with the mode main-guard picks" \
  "yes yes yes no no no yes no no no | agree=10" "$mgv_answers | agree=$mgv_agree"
expect "main-guard-verified: --docs-only of an empty list is yes, with an extra argument no" "yes no" \
  "$(printf '' | "$mgv_script" --docs-only 2> /dev/null) $(printf 'docs/a.md\n' | "$mgv_script" --docs-only x 2> /dev/null)"
# The rows --docs-only accepts are exactly those main-guard ignores: paths-ignore holds only docs/** and **/*.md.
mgv_ignored() { # <workflow>: "patterns=<n> unmatched=<n>" for the rows test 14 expects yes, or "unknown pattern"
  local pats p f bad=0
  pats="$(awk '/^    paths-ignore:$/ { p = 1; next } p && /^      - / { sub(/^      - /, ""); print; next } p { p = 0 }' "$1")"
  while IFS= read -r p; do
    [[ "$p" == "'docs/**'" || "$p" == "'**/*.md'" ]] || { echo "unknown pattern"; return; }
  done <<<"$pats"
  for f in docs/a.md README.md .claude/skills/x/SKILL.md docs/x.json; do
    [[ $f == docs/* || $f == *.md ]] || bad=$((bad + 1))
  done
  echo "patterns=$(grep -c . <<<"$pats") unmatched=$bad"
}
expect "main-guard-verified: --docs-only accepts only what main-guard ignores" "patterns=2 unmatched=0" "$(mgv_ignored "$guard_wf")"
awk '{ print } /^      - .\*\*\/\*\.md.$/ { print "      - '\''scripts/**'\''" }' "$guard_wf" > "$work/mgwf/ignore-scripts.yml"
expect "main-guard-verified: the paths-ignore check names a pattern it does not know" "unknown pattern" \
  "$(mgv_ignored "$work/mgwf/ignore-scripts.yml")"
# --guard-ignores: does main-guard run at all on a commit with only these paths (paths-ignore), whatever it would then
# check (T-094). Unlike --docs-only, the docs files Gradle reads are ignored too.
mgv_guard_ignores() { # <path>...: the answer of --guard-ignores to those paths, one per line on stdin
  printf '%s\n' "$@" | "$mgv_script" --guard-ignores 2> /dev/null
}
mgv_gi_rows=(docs/a.md README.md feature/x/README.md .claude/skills/x/SKILL.md docs/x.json docs/remote-config-keys.md
  docs/remote-config-template.json docs/design-system/tokens.json app/x.kt scripts/a.sh docs.kt)
mgv_gi_answers=""
for f in "${mgv_gi_rows[@]}"; do
  mgv_gi_answers="$mgv_gi_answers${mgv_gi_answers:+ }$(mgv_guard_ignores "$f")"
done
expect "main-guard-verified: --guard-ignores answers by the paths-ignore of main-guard" \
  "yes yes yes yes yes yes yes yes no no no" "$mgv_gi_answers"
expect "main-guard-verified: --guard-ignores of an empty list is yes, with an extra argument no" "yes no" \
  "$(printf '' | "$mgv_script" --guard-ignores 2> /dev/null) $(printf 'docs/a.md\n' | "$mgv_script" --guard-ignores x 2> /dev/null)"
mgv_guard_agree() { # <workflow>: "agree=<n>", the rows of mgv_gi_rows whose answer is yes exactly when a pattern of
  # its paths-ignore matches them, or "unknown pattern"
  local pats p rest f g match answer globs=() agree=0 any_dir='**/' under='/**'
  pats="$(awk '/^    paths-ignore:$/ { p = 1; next } p && /^      - / { sub(/^      - /, ""); print; next } p { p = 0 }' "$1")"
  while IFS= read -r p; do
    [[ -n "$p" ]] || continue
    [[ "$p" == \'*\' ]] || { echo "unknown pattern"; return; }
    p="${p:1:${#p}-2}"
    if [[ "$p" == "$any_dir"* ]]; then
      # **/<name> matches <name> in any directory, the root too: bash's * also matches /.
      rest="${p#"$any_dir"}"
      [[ "$rest" != */* && "$rest" != *'**'* ]] || { echo "unknown pattern"; return; }
      globs+=("$rest")
    elif [[ "$p" == *"$under" ]]; then
      rest="${p%"$under"}"
      [[ "$rest" != *[*?[]* ]] || { echo "unknown pattern"; return; }
      globs+=("$rest/*")
    else
      echo "unknown pattern"
      return
    fi
  done <<<"$pats"
  for f in "${mgv_gi_rows[@]}"; do
    match=no
    for g in "${globs[@]}"; do
      # shellcheck disable=SC2053 # $g is a glob
      if [[ "$f" == $g ]]; then match=yes; fi
    done
    answer="$(mgv_guard_ignores "$f")"
    if [[ ("$match" == yes && "$answer" == yes) || ("$match" == no && "$answer" != yes) ]]; then
      agree=$((agree + 1))
    fi
  done
  echo "agree=$agree"
}
expect "main-guard-verified: --guard-ignores agrees with the paths-ignore patterns" "agree=11" "$(mgv_guard_agree "$guard_wf")"
# A PR merged without updating: A, H = A + the PR's change, D = A + docs on main, M = D's tree plus the PR's change.
mgv_docs_main() { # <path=content> changed on main: sets mgv_d and mgv_m from a new mgv_setup
  local tree
  mgv_setup
  git -C "$mgv_repo" checkout -q --detach "$mgv_a"
  mgv_write "$@"
  git -C "$mgv_repo" add -A && git -C "$mgv_repo" commit -q -m D
  mgv_d="$(git -C "$mgv_repo" rev-parse HEAD)"
  git -C "$mgv_repo" checkout -q --detach "$mgv_h"
  mgv_write "$@"
  git -C "$mgv_repo" add -A
  tree="$(git -C "$mgv_repo" write-tree)"
  git -C "$mgv_repo" reset -q --hard "$mgv_h"
  mgv_m="$(git -C "$mgv_repo" commit-tree "$tree" -p "$mgv_d" -m squash)"
}
mgv_docs_main docs/runs/r.md=r
docs_ok="$(git -C "$mgv_repo" diff --name-only "$mgv_a" "$mgv_d" | "$mgv_script" --docs-only 2> /dev/null) $(mgv "$(mgv_fx "$mgv_m" "$mgv_h")" "$mgv_m")"
mgv_docs_main docs/remote-config-keys.md=b
docs_gradle="$(git -C "$mgv_repo" diff --name-only "$mgv_a" "$mgv_d" | "$mgv_script" --docs-only 2> /dev/null) $(mgv "$(mgv_fx "$mgv_m" "$mgv_h")" "$mgv_m")"
expect "merge-if-green and main-guard: a PR merged without updating is checked with tests" "yes 0 tests no 0 full" \
  "$docs_ok $docs_gradle"
mgv_gate() { # <fixture> [<args>...]: "<exit code> <stdout>" of --gate-ran
  local out code=0
  printf '%s' "$1" > "$work/mgv.json"
  shift
  out="$(PATH="$work/bin:$PATH" FIXTURE="$work/mgv.json" GITHUB_REPOSITORY=o/r "$mgv_script" --gate-ran "$@" 2> "$work/mgv.err")" || code=$?
  echo "$code $out"
}
mgv_setup
m="$(mgv_main)"
step='.routes["actions/runs/91/jobs?per_page=50"].jobs[0].steps[1]'
runs='.routes["actions/workflows/pr-check.yml/runs?head_sha=" + $h + "&per_page=10"].workflow_runs'
expect "main-guard-verified: --gate-ran reads the newest pr-check run" "0 yes|0 no|0 no|0 no|0 no|0 yes" \
  "$(mgv_gate "$(mgv_fx "$m" "$mgv_h")" "$mgv_h")|$(mgv_gate "$(mgv_fx "$m" "$mgv_h" "$step.conclusion = \"skipped\"")" "$mgv_h")|$(mgv_gate "$(mgv_fx "$m" "$mgv_h" "$runs[0].conclusion = \"failure\"")" "$mgv_h")|$(mgv_gate "$(mgv_fx "$m" "$mgv_h" "$jobs_500")" "$mgv_h")|$(mgv_gate "$(mgv_fx "$m" "$mgv_h")")|$(mgv_gate "$(mgv_fx "$m" abc1234)" abc1234)"
export GIT_CONFIG_COUNT="${mgv_git_config[0]}" GIT_CONFIG_KEY_0="${mgv_git_config[1]}" GIT_CONFIG_VALUE_0="${mgv_git_config[2]}"

# merge-if-green.sh must reject any argument other than exactly "<PR> [--dry-run]" before doing anything.
for args in "12 --dryrun" "12 -n" "12 --dry_run" "12 --dry-run extra" "abc" ""; do
  # shellcheck disable=SC2086
  if PATH="$work/bin:$PATH" scripts/merge-if-green.sh $args > /dev/null 2>&1; then
    expect "merge-if-green rejects '$args'" rejected accepted
  else
    expect "merge-if-green rejects '$args'" rejected rejected
  fi
done

# merge-if-green.sh refuses other PRs while an auto-run session is running, but lets that session's PRs through: its
# report branch, the exact revert, and the task and fix branches of the tickets in its run list.
mg_root="$PWD"
# <head ref> <run status> [labels]; prints the exit code and keeps the output in $work/merge.out. The body of the run
# file after its front matter, as a JSON string fragment, is MDR_LIST (default: a table with T-001); the other report
# branch has MDR_OLD_STATUS (default finished) and MDR_OLD_LIST. MDR_DRAFT, MDR_STATE and MDR_BASE shape the PR.
merge_during_run() {
  local head_ref="$1" run_status="$2" labels="${3:-[]}"
  cat > "$work/merge.json" << JSON
{"routes": {
  "pulls/12": {"state": "${MDR_STATE:-open}", "draft": ${MDR_DRAFT:-false}, "base": {"ref": "${MDR_BASE:-main}"}, "head": {"sha": "abc1234", "ref": "$head_ref"}, "title": "t", "labels": $labels},
  "issues?state=open&labels=main-broken&per_page=10": [],
  "branches?per_page=100&page=1": [{"name": "main"}, {"name": "claude/RUN-20261015-01-report"}, {"name": "claude/RUN-20261016-01-report"}],
  "contents/docs/runs/RUN-20261015-01.md?ref=claude/RUN-20261015-01-report": "---\nid: RUN-20261015-01\nstatus: ${MDR_OLD_STATUS:-finished}\n---\n${MDR_OLD_LIST:-}",
  "contents/docs/runs/RUN-20261016-01.md?ref=claude/RUN-20261016-01-report": "---\nid: RUN-20261016-01\nstatus: $run_status   # draft | approved | running | finished\n---\n${MDR_LIST-\n## 1. Danh sách chạy\n\n| 1 | T-001 | demo |\n}",
  "compare/main...abc1234": {"behind_by": 2}
}}
JSON
  local code=0
  PATH="$work/bin:$PATH" FIXTURE="$work/merge.json" scripts/merge-if-green.sh 12 --dry-run > "$work/merge.out" 2>&1 || code=$?
  echo "$code"
}
mdr_count() { grep -cF -- "$1" "$work/merge.out" || true; }
# Exit 3 ("branch is behind main") means the run check let the PR through; 1 means it was refused.
expect "merge-if-green refuses a person's PR while a run is running" 1 "$(merge_during_run claude/template-x running)"
expect "merge-if-green lets the session's task PR through" 3 "$(merge_during_run claude/T-001-loading running)"
expect "merge-if-green lets the session's report PR through" 3 "$(merge_during_run claude/RUN-20261016-01-report running)"
expect "merge-if-green ignores finished runs" 3 "$(merge_during_run claude/template-x finished)"
expect "merge-if-green refuses a PR labelled needs-human" 1 \
  "$(merge_during_run claude/fix-T-002-x finished '[{"name": "needs-human"}]')"
expect "merge-if-green accepts other labels" 3 "$(merge_during_run claude/template-x finished '[{"name": "docs"}]')"
# A task branch passes only when its ticket is in the table of the running list, read like scripts/resume-check.py.
expect "merge-if-green refuses a task PR not in the running list" "1 1 1" \
  "$(merge_during_run claude/T-099-other running) $(mdr_count 'phiên RUN-20261016-01 đang chạy; task T-099') $(mdr_count 'docs/runs/RUN-20261016-01.md')"
expect "merge-if-green: the refusal says how to go on" "1 1 1" \
  "$(mdr_count 'Chờ phiên kết thúc rồi gọi lại') $(mdr_count 'docs/auto-run.md, mục 1') $(mdr_count 'người dừng phiên trước')"
expect "merge-if-green lets a fix branch of a listed task through" 3 "$(merge_during_run claude/fix-T-001-x running)"
expect "merge-if-green refuses a fix branch of a task not in the list" 1 "$(merge_during_run claude/fix-T-099-x running)"
expect "merge-if-green: a task not in any list passes when no run is running" 3 "$(merge_during_run claude/T-099-other finished)"
expect "merge-if-green reads only rows of the run table" "1 3 1" \
  "$(MDR_LIST='\n| T-099 | Đã merge | #1 |\n| Chặn | T-099 | x | y |\n| 1 | T-001 | demo |\n' merge_during_run claude/T-099-other running) $(MDR_LIST='\n|  2  |  T-001  | demo |\n' merge_during_run claude/T-001-loading running) $(MDR_LIST='\n| 1 | T-0010 | demo |\n' merge_during_run claude/T-001-x running)"
expect "merge-if-green refuses a task PR when the running list has no table" 1 \
  "$(MDR_LIST='' merge_during_run claude/T-001-loading running)"
expect "merge-if-green: quick, plan and bare T branches are no task branches" "1 1 1" \
  "$(merge_during_run claude/quick-T-001-x running) $(merge_during_run claude/plan-T-001-x running) $(merge_during_run claude/T-001 running)"
expect "merge-if-green: with two running lists a task must be in both" "1 1 3 1" \
  "$(MDR_OLD_STATUS=running MDR_OLD_LIST='\n| 1 | T-002 | demo |\n' merge_during_run claude/T-001-loading running) $(mdr_count 'phiên RUN-20261015-01 đang chạy; task T-001') $(MDR_OLD_STATUS=running MDR_OLD_LIST='\n| 1 | T-001 | demo |\n' merge_during_run claude/T-001-loading running) $(MDR_OLD_STATUS=running merge_during_run claude/RUN-20261016-01-report running)"
# A listed task still meets every check that comes before the run check.
expect "merge-if-green: a listed task PR that is a draft is refused" "1 1" \
  "$(MDR_DRAFT=true merge_during_run claude/T-001-loading running) $(mdr_count 'bản nháp')"
expect "merge-if-green: a listed fix branch labelled needs-human is refused" "1 1" \
  "$(merge_during_run claude/fix-T-001-x running '[{"name": "needs-human"}]') $(mdr_count 'needs-human')"
expect "merge-if-green: a listed task PR that is closed or aims elsewhere is refused" "1 1 1 1" \
  "$(MDR_STATE=closed merge_during_run claude/T-001-loading running) $(mdr_count 'không ở trạng thái mở') $(MDR_BASE=other merge_during_run claude/T-001-loading running) $(mdr_count "nhắm vào 'other'")"

# merge-if-green.sh prints the commit it merges, warns about a very recent one, waits with --update and accepts a
# job whose status lags behind its finished green run (app lessons L-017, L-018).
merge_routes() { # <head ref> <compare route JSON fields> <check-runs answer> [extra routes]; writes $work/mg.json
  cat > "$work/mg.json" << JSON
{"routes": {
  "pulls/12": {"state": "open", "draft": false, "base": {"ref": "main"}, "head": {"sha": "abc1234", "ref": "$1"}, "title": "t", "labels": []},
  "issues?state=open&labels=main-broken&per_page=10": [],
  "branches?per_page=100&page=1": [{"name": "main"}],
  "compare/main...abc1234": $2,
  "commits/abc1234/check-runs?per_page=100": $3,
  "commits/abc1234": {"commit": {"committer": {"date": "2026-10-18T02:00:00Z"}, "message": "Sửa nút\n\nchi tiết"}},
  "pulls/12/files?per_page=100&page=1": [{"filename": "docs/STATUS.md"}, {"filename": "docs/lessons.md"}]${4:-}
}}
JSON
}
merge_run() { # <args...>; prints the exit code, keeps the output in $work/mg.out and the writes in $work/mg.json.writes
  # UPDATE_TIMEOUT is the wait of --update in seconds (5 by default), like NOW_EPOCH for the clock.
  rm -f "$work/mg.json.writes" "$work/mg.json.seen"
  local code=0
  PATH="$work/bin:$PATH" FIXTURE="$work/mg.json" MERGE_POLL_SECONDS=0 MERGE_UPDATE_TIMEOUT_SECONDS="${UPDATE_TIMEOUT:-5}" \
    MERGE_BRANCH_WAIT_SECONDS=0 MERGE_NOW_EPOCH="${NOW_EPOCH:-1793000000}" scripts/merge-if-green.sh "$@" \
    > "$work/mg.out" 2>&1 || code=$?
  echo "$code"
}
green_runs='{"check_runs": [
  {"name": "build-and-test", "status": "completed", "conclusion": "success", "started_at": "2026-10-18T02:01:00Z", "check_suite": {"id": 5}},
  {"name": "ui-test", "status": "completed", "conclusion": "skipped", "started_at": "2026-10-18T02:01:00Z", "check_suite": {"id": 5}}]}'
lagging_runs="${green_runs/\"status\": \"completed\", \"conclusion\": \"success\"/\"status\": \"in_progress\", \"conclusion\": null}"
merge_routes claude/template-x '{"behind_by": 0}' "$green_runs"
expect "merge-if-green: a green PR is ready" 0 "$(merge_run 12 --dry-run)"
expect "merge-if-green: names the commit it merges" 1 "$(grep -c '^Commit sẽ merge: abc1234 (.* phút trước) Sửa nút$' "$work/mg.out")"
expect "merge-if-green: an old commit gets no warning" 0 "$(grep -c 'phút trước. Còn ai định đẩy' "$work/mg.out" || true)"
expect "merge-if-green: a PR with docs/STATUS.md gets a warning" 1 "$(grep -c 'PR có docs/STATUS.md' "$work/mg.out")"
# 2026-10-18T02:00:00Z is 1792288800; three minutes later.
expect "merge-if-green: a commit pushed minutes ago gets a warning" 1 \
  "$(NOW_EPOCH=1792288980 merge_run 12 --dry-run > /dev/null; grep -c 'mới 3 phút trước' "$work/mg.out")"
# A docs-only PR has this shape: build-and-test runs scripts/tests/run.sh, ui-test skipped (left from before). A red
# build-and-test is refused; a skipped one (left from the PR as a draft, ADR-0041) is never green: it waits, nothing is
# written. The same fixture with build-and-test green is ready (L-040).
red_docs_runs="${green_runs/\"build-and-test\", \"status\": \"completed\", \"conclusion\": \"success\"/\"build-and-test\", \"status\": \"completed\", \"conclusion\": \"failure\"}"
skipped_docs_runs="${green_runs/\"build-and-test\", \"status\": \"completed\", \"conclusion\": \"success\"/\"build-and-test\", \"status\": \"completed\", \"conclusion\": \"skipped\"}"
merge_routes claude/template-x '{"behind_by": 0}' "$green_runs"
expect "merge-if-green: a docs-only PR whose build-and-test passed is ready" 0 "$(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' "$red_docs_runs"
expect "merge-if-green: a docs-only PR whose build-and-test failed is refused" "1 1" \
  "$(merge_run 12 --dry-run) $(grep -c 'có kiểm tra không qua: build-and-test\.' "$work/mg.out" || true)"
merge_routes claude/template-x '{"behind_by": 0}' "$skipped_docs_runs"
mg_merges() { cat "$work/mg.json.writes" 2> /dev/null | grep -c 'PUT pulls/12/merge' || true; }
expect "merge-if-green: a skipped build-and-test is never green: it waits" "2 1 0" \
  "$(merge_run 12) $(grep -c 'bị bỏ qua (lần chạy lúc PR còn nháp)' "$work/mg.out" || true) $(mg_merges)"
expect "merge-if-green --update: a skipped build-and-test waits and never merges" "2 0" "$(merge_run 12 --update) $(mg_merges)"
# The emulator left pr-check (ADR-0043) and pr-check has one job (T-076): a code PR has only build-and-test, the same
# data as one_job_runs below. An ui-test check run left from
# before is not waited for, but a red one is still refused like any red check.
code_runs='{"check_runs": [
  {"name": "build-and-test", "status": "completed", "conclusion": "success", "started_at": "2026-10-18T02:01:00Z", "check_suite": {"id": 5}}]}'
ui_running_runs="${code_runs%]\}}, {\"name\": \"ui-test\", \"status\": \"in_progress\", \"conclusion\": null, \"started_at\": \"2026-10-18T02:01:00Z\", \"check_suite\": {\"id\": 5}}]}"
ui_red_runs="${code_runs%]\}}, {\"name\": \"ui-test\", \"status\": \"completed\", \"conclusion\": \"failure\", \"started_at\": \"2026-10-18T02:01:00Z\", \"check_suite\": {\"id\": 5}}]}"
merge_routes claude/template-x '{"behind_by": 0}' "$code_runs"
expect "merge-if-green: a code PR without ui-test is ready" 0 "$(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' "$ui_running_runs"
expect "merge-if-green: a running ui-test is not waited for" 0 "$(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' "$ui_red_runs"
expect "merge-if-green: a red ui-test left from before is still refused" "1 1" \
  "$(merge_run 12 --dry-run) $(grep -c 'có kiểm tra không qua: ui-test\.' "$work/mg.out" || true)"
# pr-check has one job (T-076): a PR whose only check is build-and-test goes through every check of the script (L-058).
one_job_runs="$code_runs"
one_job_with() { # <name> <status> <conclusion>; one_job_runs with build-and-test replaced by the given check
  printf '{"check_runs": [{"name": "%s", "status": "%s", "conclusion": %s, "started_at": "2026-10-18T02:01:00Z", "check_suite": {"id": 5}}]}' "$@"
}
merge_routes claude/template-x '{"behind_by": 0}' "$one_job_runs"
expect "merge-if-green: a PR whose only check is build-and-test is ready" 0 "$(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' "$(one_job_with build-and-test completed '"failure"')"
expect "merge-if-green: a red build-and-test is refused" "1 1" \
  "$(merge_run 12 --dry-run) $(grep -c 'có kiểm tra không qua: build-and-test\.' "$work/mg.out" || true)"
merge_routes claude/template-x '{"behind_by": 0}' "$(one_job_with build-and-test completed '"skipped"')"
expect "merge-if-green: a skipped build-and-test waits in the one-job shape" "2 1 0" \
  "$(merge_run 12) $(grep -c 'bị bỏ qua (lần chạy lúc PR còn nháp)' "$work/mg.out" || true) $(mg_merges)"
expect "merge-if-green --update: a skipped build-and-test waits in the one-job shape and never merges" "2 0" \
  "$(merge_run 12 --update) $(mg_merges)"
# Control: a copy whose pending line for a skipped check returns green instead merges the same PR.
perl -pe 's/^\s*pending "kiểm tra \x27\$name\x27 trên commit \$\{HEAD_SHA:0:7\} bị bỏ qua.*$/    return 0/' \
  scripts/merge-if-green.sh > "$work/mgi-skip-green.sh"
chmod +x "$work/mgi-skip-green.sh"
skip_copy="$(cmp -s scripts/merge-if-green.sh "$work/mgi-skip-green.sh" && echo same || echo differs)"
rm -f "$work/mg.json.writes" "$work/mg.json.seen"
code=0
PATH="$work/bin:$PATH" FIXTURE="$work/mg.json" MERGE_POLL_SECONDS=0 MERGE_UPDATE_TIMEOUT_SECONDS=5 MERGE_NOW_EPOCH=1793000000 \
  "$work/mgi-skip-green.sh" 12 > "$work/mg.out" 2>&1 || code=$?
expect "merge-if-green: the skipped-check control, a copy that takes a skipped check as green, merges" "differs 0 1" \
  "$skip_copy $code $(mg_merges)"
merge_routes claude/template-x '{"behind_by": 0}' "$(one_job_with build-and-test in_progress null)" \
  ', "actions/runs?head_sha=abc1234&per_page=50": {"workflow_runs": [{"id": 77, "check_suite_id": 5, "status": "in_progress", "conclusion": null}]}'
expect "merge-if-green: build-and-test still running waits" "2 1" \
  "$(merge_run 12 --dry-run) $(grep -c 'đang ở trạng thái in_progress' "$work/mg.out" || true)"
merge_routes claude/template-x '{"behind_by": 0}' '{"check_runs": []}'
expect "merge-if-green: no check yet names build-and-test" "2 1" \
  "$(merge_run 12 --dry-run) $(grep -c "chưa thấy kiểm tra 'build-and-test'" "$work/mg.out" || true)"
merge_routes claude/template-x '{"behind_by": 0}' "$(one_job_with changes completed '"success"')"
expect "merge-if-green: a green changes check alone is not enough" "2 0" \
  "$(merge_run 12) $(cat "$work/mg.json.writes" 2> /dev/null | grep -c 'PUT pulls/12/merge' || true)"
merge_routes claude/template-x '{"behind_by": 0}' \
  "${one_job_runs%]\}}, $(one_job_with changes completed '"failure"' | sed -E 's/^\{"check_runs": \[//; s/\]\}$//')]}"
expect "merge-if-green: a leftover red changes check still refuses" "1 1" \
  "$(merge_run 12 --dry-run) $(grep -c 'có kiểm tra không qua: changes\.' "$work/mg.out" || true)"
merge_routes claude/template-x '{"behind_by": 2}' "$one_job_runs" ',
  "compare/main...def5678": {"behind_by": 0},
  "commits/def5678/check-runs?per_page=100": {"__sequence__": [{"check_runs": []}, '"$one_job_runs"']},
  "commits/def5678": {"commit": {"committer": {"date": "2026-10-18T02:30:00Z"}, "message": "Merge main"}}'
python3 -I - "$work/mg.json" << 'PY2'
import json, sys
fx = json.load(open(sys.argv[1]))
first = fx["routes"]["pulls/12"]
second = json.loads(json.dumps(first)); second["head"]["sha"] = "def5678"
fx["routes"]["pulls/12"] = {"__sequence__": [first, second]}
json.dump(fx, open(sys.argv[1], "w"))
PY2
expect "merge-if-green --update: merges once the single build-and-test check of the new commit is green" \
  "0 PUT pulls/12/update-branch;PUT pulls/12/merge;DELETE git/refs/heads/claude/template-x;" \
  "$(merge_run 12 --update) $(tr '\n' ';' < "$work/mg.json.writes")"
merge_routes claude/RUN-20261018-01-report '{"behind_by": 0}' "$green_runs"
merge_run 12 --dry-run > /dev/null
# The page is not in git (status-page.yml builds it on the status branch, L-066): no PR carries it, the report PR neither.
expect "merge-if-green: the report PR with docs/STATUS.md also gets the warning" 1 "$(grep -c 'PR có docs/STATUS.md' "$work/mg.out" || true)"
merge_routes claude/template-x '{"behind_by": 0}' "$lagging_runs" \
  ', "actions/runs?head_sha=abc1234&per_page=50": {"workflow_runs": [{"id": 77, "check_suite_id": 5, "status": "completed", "conclusion": "success"}]}'
expect "merge-if-green: a lagging job of a green run counts as green" 0 "$(merge_run 12 --dry-run)"
expect "merge-if-green: says the lagging job was taken as green" 1 "$(grep -c 'lần chạy 77 chứa nó đã xong và xanh' "$work/mg.out")"
merge_routes claude/template-x '{"behind_by": 0}' "$lagging_runs" \
  ', "actions/runs?head_sha=abc1234&per_page=50": {"workflow_runs": [{"id": 77, "check_suite_id": 5, "status": "in_progress", "conclusion": null}]}'
expect "merge-if-green: a job of a run still going waits" 2 "$(merge_run 12 --dry-run)"
expect "merge-if-green: says CI has not finished on the commit" 1 "$(grep -c 'CI chưa chạy xong trên commit này' "$work/mg.out")"
expect "merge-if-green: --update gives up when the wait runs out" 2 \
  "$(MERGE_UPDATE_TIMEOUT_SECONDS=0 merge_run 12 --update)"
expect "merge-if-green rejects '--update --dry-run'" 1 "$(merge_run 12 --update --dry-run)"
# --update: the branch is behind, GitHub updates it to def5678, CI starts, turns green, then the PR is merged.
merge_routes claude/template-x '{"behind_by": 2}' "$green_runs" ',
  "compare/main...def5678": {"behind_by": 0},
  "commits/def5678/check-runs?per_page=100": {"__sequence__": [{"check_runs": []}, '"$green_runs"']},
  "commits/def5678": {"commit": {"committer": {"date": "2026-10-18T02:30:00Z"}, "message": "Merge main"}}'
python3 -I - "$work/mg.json" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1]))
first = fx["routes"]["pulls/12"]
second = json.loads(json.dumps(first)); second["head"]["sha"] = "def5678"
fx["routes"]["pulls/12"] = {"__sequence__": [first, second]}
json.dump(fx, open(sys.argv[1], "w"))
PY
expect "merge-if-green --update: updates, waits for CI and merges" 0 "$(merge_run 12 --update)"
expect "merge-if-green --update: asks GitHub to update the branch, then merges" \
  "PUT pulls/12/update-branch;PUT pulls/12/merge;DELETE git/refs/heads/claude/template-x;" \
  "$(tr '\n' ';' < "$work/mg.json.writes")"
expect "merge-if-green --update: merges the new commit" 1 "$(grep -c '^ĐÃ MERGE: PR #12 (t) tại commit def5678' "$work/mg.out")"
expect "merge-if-green: without --update a branch behind main exits 3" 3 \
  "$(python3 -I -c 'import json,sys; fx=json.load(open(sys.argv[1])); fx["routes"]["pulls/12"]=fx["routes"]["pulls/12"]["__sequence__"][0]; json.dump(fx, open(sys.argv[1], "w"))' "$work/mg.json"; merge_run 12)"

# merge-if-green.sh reads every page of the branch list itself, on repos/<owner>/<repo> paths (L-092): a running
# session whose report branch is on page 2 still blocks other PRs.
python3 -I - "$work/merge.json" << 'PY'
import json, sys
fx = {"routes": {
  "pulls/12": {"state": "open", "draft": False, "base": {"ref": "main"}, "head": {"sha": "abc1234", "ref": "claude/template-x"}, "title": "t", "labels": []},
  "issues?state=open&labels=main-broken&per_page=10": [],
  "branches?per_page=100&page=1": [{"name": f"claude/old-{n}"} for n in range(100)],
  "branches?per_page=100&page=2": [{"name": "main"}, {"name": "claude/RUN-20261016-01-report"}],
  "contents/docs/runs/RUN-20261016-01.md?ref=claude/RUN-20261016-01-report": "---\nid: RUN-20261016-01\nstatus: running\n---\n",
  "compare/main...abc1234": {"behind_by": 2}}}
json.dump(fx, open(sys.argv[1], "w"))
PY
code=0
PATH="$work/bin:$PATH" FIXTURE="$work/merge.json" scripts/merge-if-green.sh 12 --dry-run > "$work/mg-paged.out" 2>&1 || code=$?
expect "merge-if-green: a running session on page 2 of the branches still blocks other PRs" "1 1" \
  "$code $(grep -c 'phiên RUN-20261016-01 đang chạy' "$work/mg-paged.out" || true)"

# After the merge, a branch the script could not delete gets a warning, unless GitHub already deleted it (L-092).
merge_routes claude/template-x '{"behind_by": 0}' "$green_runs"
python3 -I -c 'import json,sys; fx=json.load(open(sys.argv[1])); fx["fail_writes"]=["DELETE git/refs/heads/claude/template-x"]; json.dump(fx, open(sys.argv[1], "w"))' "$work/mg.json"
merge_run 12 > /dev/null
expect "merge-if-green: a branch already deleted by GitHub gets no warning" 0 "$(grep -c 'không xóa được nhánh' "$work/mg.out" || true)"
python3 -I -c 'import json,sys; fx=json.load(open(sys.argv[1])); fx["routes"]["git/ref/heads/claude/template-x"]={"ref": "x"}; json.dump(fx, open(sys.argv[1], "w"))' "$work/mg.json"
merge_run 12 > /dev/null
expect "merge-if-green: a branch left after a failed delete gets a warning" 1 "$(grep -c "không xóa được nhánh 'claude/template-x'" "$work/mg.out" || true)"
# GitHub deletes a merged branch a few seconds after the merge, while the delete of a cloud session may be refused by
# its proxy (L-149): after a failed delete the branch is read again a few times, and only a branch still there after
# every read gets the warning. A delete that worked reads nothing.
mg_branch_route='git/ref/heads/claude/template-x'
mg_seen() { # how many times the fixture served the branch route (fake-gh counts the reads of a __sequence__)
  python3 -I -c 'import json, os, sys; p = sys.argv[1]; print(json.load(open(p)).get(sys.argv[2], 0) if os.path.exists(p) else 0)' \
    "$work/mg.json.seen" "$mg_branch_route"
}
mg_branch() { # <JSON of the branch route> [<fail_writes JSON>]: the data of a green PR of claude/template-x
  merge_routes claude/template-x '{"behind_by": 0}' "$green_runs"
  python3 -I -c 'import json, sys; fx = json.load(open(sys.argv[1])); fx["routes"][sys.argv[2]] = json.loads(sys.argv[3]); fx["fail_writes"] = json.loads(sys.argv[4]); json.dump(fx, open(sys.argv[1], "w"))' \
    "$work/mg.json" "$mg_branch_route" "$1" "${2:-[\"DELETE git/refs/heads/claude/template-x\"]}"
}
mg_warned() { grep -c "không xóa được nhánh 'claude/template-x'" "$work/mg.out" || true; }
mg_branch '{"__sequence__": [{"ref": "x"}, {"__error__": "HTTP 404: Not Found"}]}'
merge_run 12 > /dev/null
expect "merge-if-green: a branch gone at the second read after a failed delete gets no warning" "0 2" "$(mg_warned) $(mg_seen)"
mg_branch '{"__sequence__": [{"ref": "x"}]}'
merge_run 12 > /dev/null
expect "merge-if-green: a branch still there after every read gets the warning" "1 3" "$(mg_warned) $(mg_seen)"
mg_branch '{"__sequence__": [{"ref": "x"}]}' '[]'
merge_run 12 > /dev/null
expect "merge-if-green: a branch deleted by the script is not read again" "0 0" "$(mg_warned) $(mg_seen)"
mg_branch '{"__sequence__": [{"ref": "x"}]}'
MERGE_BRANCH_TRIES=1 merge_run 12 > /dev/null
expect "merge-if-green: MERGE_BRANCH_TRIES sets the reads" "1 1" "$(mg_warned) $(mg_seen)"

# merge-if-green.sh notices a PR that is already on main, and a merge that GitHub answered with an error although the
# squash commit reached main (L-080). Every earlier check still comes first (L-058).
# The fixture is built from MIG_* variables; MIG_MAIN is the answer of "commits?sha=main", a JSON value or a
# {"__sequence__": [...]} (one answer per read).
mig_title_line='t (#12)'
mig_on_main='[{"sha": "c49a682c49a682", "commit": {"message": "t (#12)\n\nbody", "tree": {"sha": "tree1"}}}]'
mig_other_tree='[{"sha": "d111111d111111", "commit": {"message": "t (#12)", "tree": {"sha": "tree2"}}}]'
mig_other_title='[{"sha": "e222222e222222", "commit": {"message": "other (#99)", "tree": {"sha": "tree1"}}}]'
# The script runs in a temporary repo without a remote: a commit with the same title and another tree makes it fetch
# from origin, and origin must not be the GitHub repo of this checkout. The fake URL is rewritten to a local path that
# does not exist, so the fetch fails at once, offline, and writes no ref anywhere.
git init -q "$work/mig-repo"
mig_git_config=(GIT_CONFIG_COUNT=2 GIT_CONFIG_KEY_1="url.$work/no-origin.git.insteadOf" GIT_CONFIG_VALUE_1=https://github.com/o/r.git)
mig_run() { # <extra arg or empty>; reads MIG_* (MIG_SCRIPT: the script to run); prints the exit code, output in $work/mig.out
  local checks='{"check_runs": [
    {"name": "build-and-test", "status": "completed", "conclusion": "'"${MIG_BUILD:-success}"'", "started_at": "2026-10-23T08:00:10Z", "check_suite": {"id": 77}},
    {"name": "ui-test", "status": "completed", "conclusion": "skipped", "started_at": "2026-10-23T08:00:10Z", "check_suite": {"id": 77}}]}'
  local main_route=""
  [ -z "${MIG_MAIN:-}" ] || main_route='"commits?sha=main&per_page=30": '"$MIG_MAIN"','
  rm -f "$work/mig.json.writes" "$work/mig.json.seen"
  cat > "$work/mig.json" << JSON
{"fail_writes": ${MIG_FAIL:-[]},
 "routes": {
  "pulls/12": {"state": "${MIG_STATE:-open}", "draft": ${MIG_DRAFT:-false}, "base": {"ref": "${MIG_BASE:-main}"}, "head": {"sha": "abc1234", "ref": "claude/template-x"}, "title": "t", "labels": ${MIG_LABELS:-[]}},
  "issues?state=open&labels=main-broken&per_page=10": ${MIG_ISSUES:-[]},
  "branches?per_page=100&page=1": [{"name": "main"}, {"name": "claude/RUN-20261016-01-report"}],
  "contents/docs/runs/RUN-20261016-01.md?ref=claude/RUN-20261016-01-report": "---\nid: RUN-20261016-01\nstatus: ${MIG_RUN:-finished}\n---\n",
  "compare/main...abc1234": {"behind_by": ${MIG_BEHIND:-0}},
  "git/commits/abc1234": {"tree": {"sha": "tree1"}},
  $main_route
  "commits/abc1234/check-runs?per_page=100": $checks,
  "commits/abc1234": {"sha": "abc1234", "commit": {"committer": {"date": "${MIG_DATE:-2026-10-23T07:30:00Z}"}, "message": "t"}}
}}
JSON
  local code=0
  (cd "$work/mig-repo" && env "${mig_git_config[@]}" PATH="$work/bin:$PATH" FIXTURE="$work/mig.json" MERGE_NOW_EPOCH=1792742400 \
    MERGE_RECHECK_SECONDS=0 MERGE_POLL_SECONDS=0 MERGE_UPDATE_TIMEOUT_SECONDS=5 \
    "${MIG_SCRIPT:-$mg_root/scripts/merge-if-green.sh}" 12 ${1:+"$1"}) > "$work/mig.out" 2>&1 || code=$?
  echo "$code"
}
mig_writes() { cat "$work/mig.json.writes" 2> /dev/null | grep -c -- "$1" || true; }
mig_reads() { jq -r '."commits?sha=main&per_page=30" // 0' "$work/mig.json.seen" 2>/dev/null || echo 0; }
mig_reset() { unset MIG_MAIN MIG_STATE MIG_DRAFT MIG_BASE MIG_LABELS MIG_ISSUES MIG_RUN MIG_BEHIND MIG_FAIL MIG_BUILD MIG_DATE; }
mig_reset

# A PR that is already on main: reported, never merged twice, no other write.
export MIG_MAIN="$mig_on_main"
expect "merge-if-green reports a PR already on main without merging" 0 "$(mig_run)"
expect "merge-if-green says ĐÃ MERGE TRƯỚC ĐÓ" 1 "$(grep -c '^ĐÃ MERGE TRƯỚC ĐÓ: PR #12' "$work/mig.out" || true)"
expect "merge-if-green writes nothing for a PR already on main" 0 "$(mig_writes .)"
expect "merge-if-green --dry-run reports a PR already on main" 0 "$(mig_run --dry-run)"
expect "merge-if-green --dry-run never calls the merge" 0 "$(mig_writes PUT)"
expect "merge-if-green still refuses a closed PR that is on main" 1 "$(MIG_STATE=closed mig_run)"
expect "merge-if-green reports a draft PR already on main without merging" 0 "$(MIG_DRAFT=true mig_run)"
expect "merge-if-green does not merge a draft PR already on main" 0 "$(mig_writes PUT)"
expect "merge-if-green reports a needs-human PR already on main without merging" 0 "$(MIG_LABELS='[{"name": "needs-human"}]' mig_run)"
expect "merge-if-green does not merge a needs-human PR already on main" 0 "$(mig_writes PUT)"
expect "merge-if-green does not reach the merge of a needs-human PR" 1 \
  "$(MIG_MAIN='[]' MIG_LABELS='[{"name": "needs-human"}]' mig_run)"
expect "merge-if-green still refuses a PR into another branch" 1 "$(MIG_MAIN='[]' MIG_BASE=other mig_run)"
expect "merge-if-green reports a PR already on main while main is broken" 0 \
  "$(MIG_ISSUES='[{"number": 7, "body": "Nhánh chính đỏ."}]' mig_run)"
expect "merge-if-green does not merge while main is broken" 0 "$(mig_writes PUT)"
expect "merge-if-green reports a person's PR already on main while a run is running" 0 "$(MIG_RUN=running mig_run)"
expect "merge-if-green does not merge a person's PR while a run is running" 0 "$(mig_writes PUT)"
expect "merge-if-green still refuses a person's PR not on main while a run is running" 1 \
  "$(MIG_MAIN='[]' MIG_RUN=running mig_run)"
expect "merge-if-green does not update a PR already on main" 0 "$(MIG_BEHIND=2 mig_run --update)"
expect "merge-if-green does not call update-branch for a PR already on main" 0 "$(mig_writes update-branch)"
expect "merge-if-green does not merge a red PR when the API fails" 1 \
  "$(MIG_MAIN='[]' MIG_BUILD=failure MIG_FAIL='["PUT pulls/12/merge"]' mig_run)"
expect "merge-if-green does not reach the merge of a red PR" 0 "$(mig_writes PUT)"

# The merge itself: GitHub answers with an error although the squash commit reached main.
export MIG_FAIL='["PUT pulls/12/merge"]' MIG_MAIN='{"__sequence__": [[], '"$mig_on_main"']}'
expect "merge-if-green reports a merge that GitHub answered with an error" 0 "$(mig_run)"
expect "merge-if-green says ĐÃ MERGE (GitHub báo lỗi)" 1 "$(grep -c '^ĐÃ MERGE (GitHub báo lỗi): PR #12' "$work/mig.out" || true)"
expect "merge-if-green asks for main-guard after such a merge" 1 "$(mig_writes '^POST actions/workflows/main-guard.yml/dispatches$')"
expect "merge-if-green deletes the branch after such a merge" 1 "$(mig_writes '^DELETE git/refs/heads/claude/template-x$')"
expect "merge-if-green tells how to wait for main-guard" 1 "$(grep -c 'scripts/wait-run.sh --workflow main-guard.yml --since' "$work/mig.out" || true)"
export MIG_FAIL='["PUT pulls/12/merge", "POST actions/workflows/main-guard.yml/dispatches"]'
expect "merge-if-green still reports the merge when main-guard cannot be started" 0 "$(mig_run)"
expect "merge-if-green warns when main-guard cannot be started" 1 "$(grep -c '^CHÚ Ý: không yêu cầu được main-guard' "$work/mig.out" || true)"
export MIG_FAIL='["PUT pulls/12/merge"]'
for lacking in '[]' "$mig_other_title"; do
  MIG_MAIN="$lacking"
  expect "merge-if-green refuses when the API fails and main lacks the commit" 1 "$(mig_run)"
  expect "merge-if-green says GitHub refused the merge" 1 "$(grep -c '^TỪ CHỐI: GitHub từ chối merge PR #12' "$work/mig.out" || true)"
  expect "merge-if-green does not ask for main-guard when the merge failed" 0 "$(mig_writes dispatches)"
done
# A commit with the same title whose tree the script cannot check (here the fetch fails): never ĐÃ MERGE, never a
# second merge, no main-guard; a person looks (ADR-0041).
mig_same_title="^TỪ CHỐI: có commit d111111 cùng tiêu đề 't (#12)' trên nhánh chính nhưng cây khác commit của PR ngoài tài liệu; không merge lại, người xem\\.$"
expect "merge-if-green refuses, without merging, a same-titled commit on main whose tree it cannot check" "1 1 0 0" \
  "$(MIG_MAIN="$mig_other_tree" mig_run) $(grep -c "$mig_same_title" "$work/mig.out" || true) $(mig_writes '^PUT pulls/12/merge$') $(mig_writes dispatches)"
# Control: a copy whose undecided case (the only line starting with return 3) says "merged" reports ĐÃ MERGE TRƯỚC ĐÓ.
perl -pe 's/^(\s*)return 3\b.*$/${1}return 0/' scripts/merge-if-green.sh > "$work/mgi-same-title.sh"
chmod +x "$work/mgi-same-title.sh"
expect "merge-if-green: the same-title control, a copy that takes an undecided commit as merged, reports it merged" \
  "differs 0 1" \
  "$(cmp -s scripts/merge-if-green.sh "$work/mgi-same-title.sh" && echo same || echo differs) $(MIG_SCRIPT="$work/mgi-same-title.sh" MIG_MAIN="$mig_other_tree" mig_run) $(grep -c '^ĐÃ MERGE TRƯỚC ĐÓ: PR #12' "$work/mig.out" || true)"
export MIG_MAIN='{"__sequence__": [[], [], [], '"$mig_on_main"']}'
expect "merge-if-green finds the commit on a later read" 0 "$(mig_run)"
expect "merge-if-green read main four times before finding it" 4 "$(mig_reads)"
unset MIG_FAIL MIG_MAIN
expect "merge-if-green keeps the old path when the commit list cannot be read" 0 "$(mig_run)"
expect "merge-if-green still says ĐÃ MERGE after a plain merge" 1 "$(grep -c '^ĐÃ MERGE: PR #12' "$work/mig.out" || true)"
export MIG_MAIN='{"__sequence__": [[]]}'
expect "merge-if-green merges a green PR" 0 "$(mig_run)"
expect "merge-if-green merges with a PUT" 1 "$(mig_writes '^PUT pulls/12/merge$')"
expect "merge-if-green does not ask for main-guard after a plain merge" 0 "$(mig_writes dispatches)"
expect "merge-if-green reads main once before a plain merge, not after" 1 "$(mig_reads)"
MIG_DATE=2026-10-23T07:59:00Z mig_run > /dev/null
expect "merge-if-green still warns about a fresh commit before the merge" 1 "$(grep -c '^CHÚ Ý: commit cuối của nhánh' "$work/mig.out" || true)"
mig_reset
# The directory of mig_run fetches nothing from the network: its origin is a local path that does not exist.
mig_lsremote=0
(cd "$work/mig-repo" && env "${mig_git_config[@]}" git ls-remote origin) > /dev/null 2> "$work/mig-ls.err" || mig_lsremote=1
expect "merge-if-green tests fetch nothing from the network: the origin of mig_run is a missing local path" \
  "1 1 https://github.com/o/r.git" \
  "$mig_lsremote $(grep -c 'no-origin.git' "$work/mig-ls.err" || true) $(cd "$work/mig-repo" && env "${mig_git_config[@]}" git config --get-all remote.origin.url | tr '\n' ' ' | sed 's/ $//')"
FIXTURE="$work/mig.json" PATH="$work/bin:$PATH" gh api repos/o/r/git/commits/zzz > "$work/fake404.out" 2> "$work/fake404.err" || fake404=$?
expect "fake-gh answers an unknown path with 404" "1 1 0" \
  "${fake404:-0} $(grep -c 'HTTP 404' "$work/fake404.err" || true) $(grep -c Traceback "$work/fake404.err" || true)"
# An {"__error__": ...} item of a __sequence__ fails that one read, so a branch can be there at one read and gone at
# the next (T-094).
printf '%s' '{"routes": {"x": {"__sequence__": [{"a": 1}, {"__error__": "HTTP 404: Not Found"}, {"a": 2}]}}}' > "$work/fg.json"
rm -f "$work/fg.json.seen"
fg_codes=""
for fg_i in 1 2 3; do
  fg_code=0
  FIXTURE="$work/fg.json" PATH="$work/bin:$PATH" gh api repos/o/r/x > "$work/fg$fg_i.out" 2> "$work/fg$fg_i.err" || fg_code=$?
  fg_codes="$fg_codes$fg_code "
done
expect "fake-gh: an __error__ item of a sequence fails that read" "0 1 0 yes" \
  "$fg_codes$(grep -q 'HTTP 404' "$work/fg2.err" && echo yes || echo no)"

# failure-kind.sh (ADR-0025, point 1): "tests" only when every failed Gradle task is a test task with failing tests.
failure_kind() { printf '%b' "$1" > "$work/fk.log"; scripts/failure-kind.sh "$work/fk.log"; }
gradle_failure() { printf '* What went wrong:\\nExecution failed for task '"'"'%s'"'"'.\\n> %s\\n' "$1" "$2"; }
expect "failure-kind: only failing unit tests -> tests" tests \
  "$(failure_kind "$(gradle_failure :feature:settings:testDebugUnitTest 'There were failing tests. See the report at: x')")"
expect "failure-kind: failing staging unit tests and verifyRoborazzi -> tests" tests \
  "$(failure_kind "FAILURE: Build completed with 2 failures.\\n$(gradle_failure :app:testStagingDebugUnitTest 'There were failing tests.')$(gradle_failure :feature:settings:verifyRoborazziDebug 'There were failing tests.')")"
expect "failure-kind: compile error -> other" other \
  "$(failure_kind "$(gradle_failure :app:compileStagingDebugKotlin 'Compilation error. See log for more details')")"
expect "failure-kind: lint -> other" other \
  "$(failure_kind "$(gradle_failure :app:lintStagingDebug 'Lint found errors in the project; aborting build.')")"
expect "failure-kind: test and lint both failed -> other" other \
  "$(failure_kind "$(gradle_failure :feature:settings:testDebugUnitTest 'There were failing tests.')$(gradle_failure :app:lintStagingDebug 'Lint found errors.')")"
expect "failure-kind: a test task that failed for another reason -> other" other \
  "$(failure_kind "$(gradle_failure :feature:settings:testDebugUnitTest 'Could not start Gradle Test Executor 1.')")"
expect "failure-kind: no Gradle failure line -> other" other \
  "$(failure_kind "FAIL - merge-if-green rejects '12 -n'\\n1 script test(s) failed\\n")"
expect "failure-kind: missing log -> other" other "$(scripts/failure-kind.sh "$work/no-such.log")"

# failure-kind.sh --playbook looks a failure up in docs/ops/failure-playbook.md (L-079).
pb_dir=scripts/tests/playbook
playbook() { scripts/failure-kind.sh --playbook "$@" 2> "$work/pb.err"; }
pb_head() { playbook "$1" ${2:+"$2"} | cut -d: -f1; }
expect "playbook: PB-01 R2 download timeout in ui-test -> chay-lai" "PB-01 chay-lai" "$(pb_head $pb_dir/PB-01.log)"
expect "playbook: PB-01 also matches the first attempt (Task failed, no Assets line)" "PB-01 chay-lai" \
  "$(pb_head $pb_dir/PB-01-attempt1.log)"
expect "playbook: PB-02 GitHub did not start the job -> cho" "PB-02 cho" "$(pb_head $pb_dir/PB-02.log)"
expect "playbook: PB-03 conflicting PR -> gop-nhanh" "PB-03 gop-nhanh" "$(pb_head $pb_dir/PB-03.log)"
expect "playbook: PB-04 merge API error -> kiem-da-merge" "PB-04 kiem-da-merge" "$(pb_head $pb_dir/PB-04.log)"
expect "playbook: PB-05 Java unreadable -> moi-truong" "PB-05 moi-truong" "$(pb_head $pb_dir/PB-05.log)"
expect "playbook: PB-05 also matches Java out of range" "PB-05 moi-truong" "$(pb_head $pb_dir/PB-05-range.log)"
expect "playbook: PB-06 proxy 403 -> moi-truong" "PB-06 moi-truong" "$(pb_head $pb_dir/PB-06.log)"
expect "playbook: PB-07 fake Firebase file -> nguoi" "PB-07 nguoi" "$(pb_head $pb_dir/PB-07.log)"
expect "playbook: PB-08 missing R8 classes -> that" "PB-08 that" "$(pb_head $pb_dir/PB-08.log)"
expect "playbook: PB-09 library download 429 -> lich-thu-lai" "PB-09 lich-thu-lai" "$(pb_head $pb_dir/PB-09.log)"
expect "playbook: PB-10 a UI timeout without R2 lines is a real failure" "PB-10 that" "$(pb_head $pb_dir/PB-10.log)"
expect "playbook: PB-11 Maven 403 on a GitHub runner -> chay-lai" "PB-11 chay-lai" "$(pb_head $pb_dir/PB-11.log)"
expect "playbook: PB-12 Claude org access 403 in a subagent -> cho" "PB-12 cho" "$(pb_head $pb_dir/PB-12.log)"
expect "playbook: PB-13 an R2 file missing (404) in ui-test -> nguoi, before the rerun of PB-01" "PB-13 nguoi" "$(pb_head $pb_dir/PB-13.log)"
expect "playbook: PB-14 push refused for a workflow file (OAuth App, workflow scope) -> bo-qua-loai" "PB-14 bo-qua-loai" "$(pb_head $pb_dir/PB-14.log)"
expect "playbook: PB-15 the emulator package failed to download and no test ran -> lich-thu-lai" "PB-15 lich-thu-lai" \
  "$(pb_head $pb_dir/PB-15.log)"
expect "playbook: PB-16 a job cancelled at its time limit during the release build -> chay-lai, not PB-08" "PB-16 chay-lai" \
  "$(pb_head $pb_dir/PB-16.log)"
# A self-hosted runner that is off, gone or set up wrong is for a person to fix, not a code failure (ADR-0042). The
# same words from a GitHub runner are not these rows.
expect "playbook: PB-17 waiting for a self-hosted runner -> nguoi" "PB-17 nguoi" "$(pb_head $pb_dir/PB-17.log)"
expect "playbook: PB-18 a self-hosted runner lost communication -> nguoi" "PB-18 nguoi" "$(pb_head $pb_dir/PB-18.log)"
expect "playbook: PB-19 runner not usable -> nguoi" "PB-19 nguoi" "$(pb_head $pb_dir/PB-19.log)"
expect "playbook: PB-20 an old merge-if-green.sh waits for the changes check -> chay-lai" "PB-20 chay-lai" \
  "$(pb_head $pb_dir/PB-20.log)"
printf '%s\n' "Chờ: chưa thấy kiểm tra 'build-and-test' trên commit abc1234: CI chưa chạy trên commit này." \
  > "$work/pb20-other-check.log"
expect "playbook: waiting for the build-and-test check is not PB-20" "" "$(pb_head "$work/pb20-other-check.log" | grep PB-20)"
expect "playbook: PB-21 a Gradle daemon disappeared -> chay-lai" "PB-21 chay-lai" "$(pb_head $pb_dir/PB-21.log)"
printf '%s\n' 'The hosted runner: GitHub Actions 2 lost communication with the server.' > "$work/pb-hosted-lost.log"
printf '%s\n' 'The runner has received a shutdown signal. This can happen when the runner service is stopped, or a manually started runner is canceled.' > "$work/pb-shutdown.log"
printf '%s\n' 'No space left on device' > "$work/pb-no-space.log"
expect "playbook: a GitHub runner losing the server, a shutdown signal and a full disk are not PB-17 to PB-19" \
  "khong-co khong-co khong-co" \
  "$(pb_head "$work/pb-hosted-lost.log") $(pb_head "$work/pb-shutdown.log") $(pb_head "$work/pb-no-space.log")"
# R8 prints "WARNING: R8: Missing class ... (referenced from ...)" on a green release build; only its errors are PB-08 (L-138).
grep 'WARNING: R8' $pb_dir/PB-16.log > "$work/pb-r8-warning.log"
expect "playbook: an R8 missing-class warning alone is not PB-08" khong-co "$(pb_head "$work/pb-r8-warning.log")"
printf '%s\n' 'ERROR: Missing class com.example.Gone (referenced from: void com.example.Uses.call())' > "$work/pb-r8-error.log"
expect "playbook: an R8 missing-class error is PB-08" "PB-08 that" "$(pb_head "$work/pb-r8-error.log")"
# A row with two patterns joined by "và" matches only when the log has both: the same download warning on a run whose
# tests did run and fail is a real failure, not infrastructure.
grep -v 'Không có test nào chạy' $pb_dir/PB-15.log > "$work/pb15-tests-ran.log"
echo "FAILED tests: com.example.SomeTest > opensHome[test(AVD) - 14] FAILED" >> "$work/pb15-tests-ran.log"
expect "playbook: PB-15 needs both lines: the warning alone is not PB-15" khong-co "$(pb_head "$work/pb15-tests-ran.log")"
printf '%s\n' '<!-- playbook:start -->' '| PB-01 | `aaa` và `bbb` | x | `cho` | y | z |' '<!-- playbook:end -->' > "$work/pb-and.md"
printf 'aaa\nbbb\n' > "$work/pb-and-both.log"
printf 'aaa\n' > "$work/pb-and-one.log"
expect "playbook: a row with two patterns matches a log with both" "PB-01 cho" "$(pb_head "$work/pb-and-both.log" "$work/pb-and.md")"
expect "playbook: a row with two patterns does not match a log with one" khong-co "$(pb_head "$work/pb-and-one.log" "$work/pb-and.md")"
printf '%s\n' ' ! [remote rejected] claude/T-1 -> claude/T-1 (refusing to allow a GitHub App to create or update workflow `.github/workflows/ci.yml` without `workflows` permission)' > "$work/pb14-app.log"
expect "playbook: PB-14 also matches the GitHub App wording (workflows permission)" "PB-14 bo-qua-loai" "$(pb_head "$work/pb14-app.log")"
printf '%s\n' ' ! [remote rejected] claude/T-1 -> claude/T-1 (refusing to allow a Personal Access Token to create or update workflow `.github/workflows/ci.yml` without `workflow` scope)' > "$work/pb14-pat.log"
expect "playbook: PB-14 also matches the personal access token wording" "PB-14 bo-qua-loai" "$(pb_head "$work/pb14-pat.log")"
printf '%s\n' ' ! [remote rejected] claude/T-1 -> claude/T-1 (protected branch hook declined)' > "$work/pb14-other.log"
expect "playbook: a push refused for another reason is not PB-14" "khong-co" "$(pb_head "$work/pb14-other.log")"
expect "playbook: an unknown failure says khong-co" "khong-co" "$(pb_head $pb_dir/none.log)"
expect "playbook: a missing log says khong-co" "khong-co" "$(pb_head "$work/no-such.log")"
printf 'unknown flag: --allow-escape-sequences\n\nUsage:  gh api <endpoint> [flags]\n' > "$work/gh-help.log"
expect "playbook: the help text of a failed gh call says log-hong, not khong-co" "log-hong" "$(pb_head "$work/gh-help.log")"
printf 'the response contains terminal escape sequences; pass --allow-escape-sequences to output it anyway\n' > "$work/gh-esc.log"
expect "playbook: the escape-sequence refusal of gh says log-hong" "log-hong" "$(pb_head "$work/gh-esc.log")"
expect "playbook: a missing log exits 0" 0 "$(scripts/failure-kind.sh --playbook "$work/no-such.log" > /dev/null 2>&1; echo $?)"
expect "playbook: the line carries the action and the source" "PB-02 cho: GitHub không cấp máy. Làm theo \`auto-run.md\` mục 4, dòng \"GitHub không chạy job\" (camera-pose, GitHub không chạy job vì hết hạn mức Actions (issue #106))" \
  "$(playbook $pb_dir/PB-02.log)"
# Test tables for the parser: an escaped pipe is an alternative; a broken pattern is skipped and reported.
cat > "$work/pb-table.md" <<'MD'
intro
<!-- playbook:start -->
| Mã | Mẫu | Thấy ở | Loại | Việc phải làm | Nguồn |
| --- | --- | --- | --- | --- | --- |
| PB-01 | `aaa\|bbb` | x | `that` | làm A | nguồn A |
<!-- playbook:end -->
MD
echo "only bbb here" > "$work/pb-bbb.log"
expect "playbook: an escaped pipe is an alternative" "PB-01 that" "$(pb_head "$work/pb-bbb.log" "$work/pb-table.md")"
cat > "$work/pb-broken.md" <<'MD'
<!-- playbook:start -->
| Mã | Mẫu | Thấy ở | Loại | Việc phải làm | Nguồn |
| --- | --- | --- | --- | --- | --- |
| PB-01 | `(` | x | `that` | làm A | nguồn A |
| PB-02 | `bbb` | x | `cho` | làm B | nguồn B |
<!-- playbook:end -->
MD
expect "playbook: a broken pattern is skipped" "PB-02 cho" "$(pb_head "$work/pb-bbb.log" "$work/pb-broken.md")"
expect "playbook: a broken pattern is reported" 1 "$(grep -c 'LỖI BẢNG' "$work/pb.err" || true)"
: > "$work/pb-empty.log"
playbook "$work/pb-empty.log" > /dev/null
expect "playbook: every pattern of the real table is valid" 0 "$(grep -c 'LỖI BẢNG' "$work/pb.err" || true)"
pb_rows="$(sed -n '/playbook:start/,/playbook:end/p' docs/ops/failure-playbook.md | grep -oE '^\| PB-[0-9]+' | tr -d '| ' | sort)"
pb_files="$(ls $pb_dir | grep -oE '^PB-[0-9]+' | sort -u)"
expect "playbook: every row has a log file and every log file a row" "$pb_rows" "$pb_files"
# Every kind of the table is described for the session, and a bo-qua-loai row says which tasks it skips (ADR-0032, L8).
pb_kinds="$(sed -n '/playbook:start/,/playbook:end/p' docs/ops/failure-playbook.md | grep -E '^\| PB-[0-9]+ ' | awk -F' \\| ' '{ print $4 }' | tr -d '`' | sort -u)"
pb_known="$(grep -m1 '^Cột Mẫu là' docs/ops/failure-playbook.md | sed -nE 's/.*Cột Loại là một trong: ([^.]*)\..*/\1/p' | grep -oE '`[a-z-]+`' | tr -d '`' | sort -u || true)"
expect "playbook: the kinds sentence lists the kinds (that among them)" yes "$(printf '%s\n' "$pb_known" | grep -qx that && echo yes || echo no)"
expect "playbook: every kind in the table is listed in the kinds sentence" "" "$(comm -23 <(printf '%s\n' "$pb_kinds") <(printf '%s\n' "$pb_known"))"
pb_skip_rows="$(sed -n '/playbook:start/,/playbook:end/p' docs/ops/failure-playbook.md | grep -E '^\| PB-[0-9]+ .*\| `bo-qua-loai` \|' || true)"
expect "playbook: every bo-qua-loai row names the plan-check --touches command" "" \
  "$(printf '%s\n' "$pb_skip_rows" | grep -v 'scripts/plan-check.py --touches ' || true)"
expect "playbook: the table has at least one bo-qua-loai row" yes "$([ -n "$pb_skip_rows" ] && echo yes || echo no)"
expect "playbook: the table has at least 10 rows" yes "$([ "$(printf '%s\n' "$pb_rows" | wc -l)" -ge 10 ] && echo yes || echo no)"
expect "failure-kind: a playbook-only log still says other" other "$(scripts/failure-kind.sh $pb_dir/PB-01.log)"

# test-summary.sh counts only this checkout: agent worktrees in .claude/worktrees/ and any other worktree nested in the
# repository keep their own build results and screenshots (L-137).
ts_repo="$work/ts-repo"
mkdir -p "$ts_repo/scripts"
cp scripts/test-summary.sh "$ts_repo/scripts/"
ts_result() { # <dir> <tests>: one JUnit file and one screenshot in module m under <dir>
  mkdir -p "$1/m/build/test-results/testDebugUnitTest" "$1/m/src/test/screenshots"
  printf '<testsuite tests="%s" failures="0" errors="0"/>\n' "$2" > "$1/m/build/test-results/testDebugUnitTest/TEST-a.xml"
  : > "$1/m/src/test/screenshots/a.png"
}
git -C "$ts_repo" init -q
git -C "$ts_repo" add scripts
git -C "$ts_repo" commit -q -m init
git -C "$ts_repo" worktree add -q "$ts_repo/nested" 2> /dev/null
ts_result "$ts_repo" 3
ts_result "$ts_repo/.claude/worktrees/agent-old" 5
ts_result "$ts_repo/nested" 7
expect "test-summary: tests and screenshots of nested worktrees are not counted" \
  "Test JVM: 3 test, 0 lỗi. Ảnh screenshot: 1. ./m: 3 test, 0 lỗi" "$(GITHUB_ACTIONS='' "$ts_repo/scripts/test-summary.sh" jvm)"

# The emulator tests of main-guard.yml (ADR-0043, T-075): ui-test-needed.sh, ui-test-on-emulator.sh, ui-test-verdict.sh.
# The blocks below commit in temporary repos, so the fake origin URL is unset here and restored at the end (L-082).
uin_git_config=("$GIT_CONFIG_COUNT" "$GIT_CONFIG_KEY_0" "$GIT_CONFIG_VALUE_0")
unset GIT_CONFIG_COUNT GIT_CONFIG_KEY_0 GIT_CONFIG_VALUE_0
uin_script="$PWD/scripts/ui-test-needed.sh"
uin_repo="$work/uin"
rm -rf "$uin_repo"
git init -q -b main "$uin_repo"
echo root > "$uin_repo/README.md"
git -C "$uin_repo" add -A && git -C "$uin_repo" commit -q -m root
uin_root="$(git -C "$uin_repo" rev-parse HEAD)"
uin_run() { # <sha> [<event>]: runs the script (UIN_SCRIPT, default the real one) in the repo; prints "<exit code> <stdout>"
  local out code=0
  out="$(cd "$uin_repo" && "${UIN_SCRIPT:-$uin_script}" "$1" ${2:+"$2"} 2> "$work/uin.err")" || code=$?
  echo "$code $out"
}
uin() { # <event> <file>...: commits a change to those files, then runs the script on that commit
  local event="$1" f
  shift
  for f in "$@"; do
    mkdir -p "$uin_repo/$(dirname "$f")"
    echo x >> "$uin_repo/$f"
  done
  git -C "$uin_repo" add -A && git -C "$uin_repo" commit -q -m change
  uin_run "$(git -C "$uin_repo" rev-parse HEAD)" "$event"
}
uin_count() { # <expected output> <event> <file>...: how many files, each changed in its own commit, give that output
  local want="$1" event="$2" f n=0
  shift 2
  for f in "$@"; do [[ "$(uin "$event" "$f")" == "$want" ]] && n=$((n + 1)); done
  echo "$n"
}
uin_code=(app/x.kt core/c/x.kt feature/f/x.kt build-logic/x.kts gradle/libs.versions.toml build.gradle.kts
  settings.gradle.kts gradle.properties gradlew scripts/test-summary.sh scripts/ui-test-on-emulator.sh
  scripts/ui-test-verdict.sh scripts/ui-test-needed.sh scripts/runner-check.sh .github/workflows/main-guard.yml)
expect "ui-test-needed: a commit changing app code is yes" "${#uin_code[@]}" "$(uin_count '0 yes' push "${uin_code[@]}")"
uin_other=(scripts/status.py docs/a.md README.md .github/workflows/pr-check.yml featurex/a.kt appendix.txt docs/app/x.kt)
expect "ui-test-needed: a commit without app code is no" "${#uin_other[@]}" "$(uin_count '0 no' push "${uin_other[@]}")"
expect "ui-test-needed: a commit with app code among other files is yes" "0 yes" "$(uin push scripts/status.py core/c/x.kt)"
expect "ui-test-needed: workflow_dispatch is yes" "0 yes 0 no" "$(uin workflow_dispatch docs/a.md) $(uin push docs/a.md)"
expect "ui-test-needed: an unreadable commit is yes, exit 0" "0 yes 1 0 yes" \
  "$(uin_run 0123456789abcdef0123456789abcdef01234567) $(grep -c 'không đọc được' "$work/uin.err" || true) $(uin_run "$uin_root")"
git -C "$uin_repo" mv app/x.kt docs/x.kt
git -C "$uin_repo" commit -q -m move
expect "ui-test-needed: code moved into docs/ is yes" "0 yes" "$(uin_run "$(git -C "$uin_repo" rev-parse HEAD)")"
# Every Gradle module of settings.gradle.kts is under UI_PATHS; a copy of the script without core/ misses some.
uin_modules=()
while IFS= read -r module; do
  uin_modules+=("$(tr ':' '/' <<<"${module#:}")/x.kt")
done < <(sed -n -E 's/^include\("(:[^"]+)"\)$/\1/p' settings.gradle.kts)
sed 's# core/##' "$uin_script" > "$work/uin-short.sh" 2> /dev/null || true
chmod +x "$work/uin-short.sh"
expect "ui-test-needed: every Gradle module is under UI_PATHS" "${#uin_modules[@]}" "$(uin_count '0 yes' push "${uin_modules[@]}")"
expect "ui-test-needed: the module check fails on a copy without core/" yes \
  "$([[ "$(UIN_SCRIPT="$work/uin-short.sh" uin_count '0 no' push "${uin_modules[@]}")" -ge 1 ]] && echo yes || echo no)"

# ui-test-on-emulator.sh runs the emulator tests once, and once more on the same emulator when the first run is red.
# adb and gradlew are fakes: adb records its arguments, gradlew follows a scenario (pass or fail, one line per call).
uoe_script="$PWD/scripts/ui-test-on-emulator.sh"
uoe_dir="$work/uoe"
cat > "$work/bin/fake-adb" << 'SH'
#!/usr/bin/env bash
echo "$*" >> "$UOE_DIR/adb.calls"
if [[ "$1" == logcat && "${2:-}" == -v ]]; then echo "I VGL.Test: device log"; fi
SH
cat > "$work/bin/fake-gradlew" << 'SH'
#!/usr/bin/env bash
n=$(( $(cat "$UOE_DIR/gradlew.n" 2> /dev/null || echo 0) + 1 ))
echo "$n" > "$UOE_DIR/gradlew.n"
result="$(sed -n "${n}p" "$UOE_DIR/gradlew.scenario")"
echo "gradlew run $n: ${result:-none} $*"
[[ "$result" == pass ]]
SH
chmod +x "$work/bin/fake-adb" "$work/bin/fake-gradlew"
uoe() { # <pass|fail>...: runs the script in a fresh directory (with test results when UOE_RESULTS is set); prints its exit code
  local code=0
  rm -rf "$uoe_dir"
  mkdir -p "$uoe_dir"
  printf '%s\n' "$@" > "$uoe_dir/gradlew.scenario"
  if [[ -n "${UOE_RESULTS:-}" ]]; then
    mkdir -p "$uoe_dir/app/build/outputs/androidTest-results"
    echo '<testsuite tests="1" failures="1"/>' > "$uoe_dir/app/build/outputs/androidTest-results/a.xml"
  fi
  (cd "$uoe_dir" && UOE_DIR="$uoe_dir" UI_TEST_ADB="$work/bin/fake-adb" UI_TEST_GRADLEW="$work/bin/fake-gradlew" \
    "$uoe_script") > "$work/uoe.out" 2>&1 || code=$?
  echo "$code"
}
uoe_has() { [[ -f "$uoe_dir/$1" ]] && echo yes || echo no; } # <file>
uoe_grep() { grep -qF -- "$2" "$uoe_dir/$1" 2> /dev/null && echo yes || echo no; } # <file> <text>
uoe_clears() { grep -cx 'logcat -c' "$uoe_dir/adb.calls" 2> /dev/null || true; }
uoe_runs() { cat "$uoe_dir/gradlew.n" 2> /dev/null || echo 0; }
expect "ui-test-on-emulator: a green first run runs once" "0 1 yes no 1" \
  "$(uoe pass) $(uoe_runs) $(uoe_has ui-test.log) $(uoe_has ui-test-first.log) $(uoe_clears)"
expect "ui-test-on-emulator: a red first run runs once more and keeps its log" "0 2 yes no yes yes yes yes 2" \
  "$(UOE_RESULTS=yes uoe fail pass) $(uoe_runs) $(uoe_grep ui-test-first.log 'gradlew run 1: fail') $(uoe_grep ui-test-first.log 'gradlew run 2') $(uoe_grep ui-test.log 'gradlew run 2: pass') $(uoe_has logcat-first.txt) $(uoe_has logcat.txt) $(uoe_has ui-test-first-results/a.xml) $(uoe_clears)"
expect "ui-test-on-emulator: says it runs once more" 1 "$(grep -c 'Lần đầu đỏ, chạy lại một lần trên cùng emulator (ADR-0043)' "$work/uoe.out" || true)"
expect "ui-test-on-emulator: two red runs fail" "red 2" \
  "$([[ "$(uoe fail fail pass)" != 0 ]] && echo red || echo green) $(uoe_runs)"

# ui-test-verdict.sh: green, flaky, infra or red from the files the emulator step and the summary step leave.
uv_script="$PWD/scripts/ui-test-verdict.sh"
uv_dir="$work/uv"
uv_reset() { # [<file the last commit changes>]: a fresh job directory, a git repo with app/build.gradle.kts
  local changed="${1:-app/x.kt}"
  rm -rf "$uv_dir"
  mkdir -p "$uv_dir/app"
  git init -q -b main "$uv_dir"
  printf 'android {\n    defaultConfig {\n        applicationId = "com.vglstudio.camera.pose.line.guide"\n    }\n}\n' > "$uv_dir/app/build.gradle.kts"
  echo a > "$uv_dir/README.md"
  git -C "$uv_dir" add -A && git -C "$uv_dir" commit -q -m A
  mkdir -p "$uv_dir/$(dirname "$changed")"
  echo b >> "$uv_dir/$changed"
  git -C "$uv_dir" add -A && git -C "$uv_dir" commit -q -m B
}
uv_summary() { # <tests> <failures>: ui-summary.log as scripts/test-summary.sh android writes it
  printf 'Test emulator: %s test, %s lỗi. ./app: %s test, %s lỗi\n' "$1" "$2" "$1" "$2" > "$uv_dir/ui-summary.log"
  [[ "$1" != 0 ]] || echo '::error::Không có test nào chạy trên emulator.' >> "$uv_dir/ui-summary.log"
}
uv_put() { printf '%s\n' "${@:2}" > "$uv_dir/$1"; } # <file> <line>...
uv() { # <outcome of the emulator step>: runs the script in the job directory; prints "<exit code> <stdout>"
  local out code=0
  out="$(cd "$uv_dir" && "$uv_script" "$1" 2> "$work/uv.err")" || code=$?
  echo "$code $out"
}
uv_said() { grep -qF -- "$1" "$work/uv.err" && echo yes || echo no; } # <text in the reason>
uv_failed_test='com.vglstudio.camera.pose.line.guide.AppLaunchTest > launches[test(AVD) - 14] FAILED'
uv_pb01='W VGL.Loading: Task HELLO failed'

uv_reset
uv_put ui-test.log 'BUILD SUCCESSFUL in 1m'
uv_summary 3 0
expect "ui-test-verdict: a green run is green" "0 green" "$(uv success)"
rm "$uv_dir/ui-summary.log"
uv_without_summary="$(uv success)"
uv_summary 0 0
expect "ui-test-verdict: a green step without a summary, or with 0 tests, is infra" "0 infra 0 infra" \
  "$uv_without_summary $(uv success)"

uv_reset
uv_put ui-test-first.log '> Task :app:connectedStagingDebugAndroidTest FAILED' "$uv_failed_test"
uv_put logcat-first.txt 'I VGL.Home: shown'
uv_put ui-test.log 'BUILD SUCCESSFUL in 1m'
uv_summary 3 0
uv_flaky="$(uv success)"
uv_put logcat-first.txt "$uv_pb01"
expect "ui-test-verdict: red then green is flaky; a first run red for R2 (PB-01) is green" "0 flaky 0 green" \
  "$uv_flaky $(uv success)"

# Infrastructure: no Gradle log, a broken device with 0 tests, or a whitelisted row of the playbook.
uv_reset
uv_infra_a="$(uv failure)"
uv_reset scripts/ui-test-on-emulator.sh
expect "ui-test-verdict: no log is infra, unless the commit changed the job's own script" "0 infra 0 red" \
  "$uv_infra_a $(uv failure)"
uv_reset
uv_summary 0 0
uv_put ui-test.log 'com.android.builder.testing.api.DeviceException: No connected devices!'
expect "ui-test-verdict: 0 tests on a broken device is infra" "0 infra" "$(uv failure)"
uv_reset
uv_summary 0 0
cp "$pb_dir/PB-15.log" "$uv_dir/ui-test.log"
expect "ui-test-verdict: the emulator package failed (PB-15) is infra" "0 infra yes" "$(uv failure) $(uv_said PB-15)"
uv_reset
uv_summary 3 1
uv_put ui-test.log "$uv_failed_test" 'androidx.compose.ui.test.ComposeTimeoutException: Condition still not satisfied after 60000 ms'
uv_put logcat.txt "10-26 16:35:20.437  6250  6250 $uv_pb01"
expect "ui-test-verdict: an R2 download timeout (PB-01) is infra" "0 infra yes" "$(uv failure) $(uv_said PB-01)"
uv_reset
uv_summary 0 0
cp "$pb_dir/PB-11.log" "$uv_dir/ui-test.log"
expect "ui-test-verdict: Maven 403 (PB-11) is infra" "0 infra yes" "$(uv failure) $(uv_said PB-11)"
uv_reset
uv_summary 3 1
uv_put ui-test.log "$uv_failed_test"
cp "$pb_dir/PB-13.log" "$uv_dir/logcat.txt"
expect "ui-test-verdict: an R2 file missing (PB-13) is infra" "0 infra yes" "$(uv failure) $(uv_said PB-13)"

# Real failures: a failed test, PB-10 without R2 lines; the same with a PB-01 line is infra (PB-01 comes first).
uv_reset
uv_summary 3 1
uv_put ui-test.log "$uv_failed_test"
uv_put logcat.txt 'I VGL.Home: shown'
uv_red_test="$(uv failure)"
cp "$pb_dir/PB-10.log" "$uv_dir/ui-test.log"
uv_red_pb10="$(uv failure)"
uv_put logcat.txt "$uv_pb01"
expect "ui-test-verdict: a real test failure is red" "0 red 0 red 0 infra" "$uv_red_test $uv_red_pb10 $(uv failure)"

# An app that crashes at start is red with 0 tests, even next to the lines of PB-15 that alone would be infra.
uv_crash() { # <file> <line>...: 0 tests, the PB-15 log as ui-test.log, plus those lines in that file
  uv_reset
  uv_summary 0 0
  cp "$pb_dir/PB-15.log" "$uv_dir/ui-test.log"
  printf '%s\n' "${@:2}" >> "$uv_dir/$1"
  uv failure
}
uv_crash_app=('E AndroidRuntime: FATAL EXCEPTION: main' 'E AndroidRuntime: Process: com.vglstudio.camera.pose.line.guide.staging, PID: 123')
uv_crash_gms=('E AndroidRuntime: FATAL EXCEPTION: main' 'E AndroidRuntime: Process: com.google.android.gms, PID: 99')
expect "ui-test-verdict: an app that crashes at start is red" "0 red 0 red 0 red" \
  "$(uv_crash ui-test.log 'Tests on test(AVD) - 14 failed: Process crashed.') $(uv_crash ui-test.log "Tests on test(AVD) - 14 failed: Instrumentation run failed due to 'java.lang.IllegalStateException'") $(uv_crash logcat.txt "${uv_crash_app[@]}")"
expect "ui-test-verdict: 0 tests next to PB-15 without a crash is infra" "0 infra" "$(uv_crash logcat.txt 'I VGL.Home: shown')"
uv_reset
uv_summary 0 0
uv_put ui-test.log 'BUILD FAILED in 1m'
expect "ui-test-verdict: 0 tests without any sign is red" "0 red" "$(uv failure)"
uv_reset
uv_summary 3 1
uv_put ui-test.log "$uv_failed_test"
uv_put logcat.txt "${uv_crash_gms[@]}" "$uv_pb01"
expect "ui-test-verdict: a crash of a system process (gms) is not the app" "0 infra" "$(uv failure)"
# The applicationId is a regular expression with its dots escaped; without one, only Gradle's crash lines count.
uv_put logcat.txt 'E AndroidRuntime: FATAL EXCEPTION: main' 'E AndroidRuntime: Process: comXvglstudio.template.staging, PID: 1' "$uv_pb01"
expect "ui-test-verdict: the dots of the applicationId are not any character" "0 infra" "$(uv failure)"
uv_put logcat.txt "${uv_crash_app[@]}" "$uv_pb01"
uv_app_crash="$(uv failure)"
echo 'android { }' > "$uv_dir/app/build.gradle.kts"
expect "ui-test-verdict: without an applicationId only Gradle's crash lines count, with a warning" "0 red 0 infra yes" \
  "$uv_app_crash $(uv failure) $(uv_said 'CẢNH BÁO')"

# Only the emulator infrastructure rows are infra: PB-09 (a timeout in the app's own log) is red.
uv_reset
uv_summary 3 1
uv_put ui-test.log "$uv_failed_test"
uv_put logcat.txt 'E AndroidRuntime: java.net.SocketTimeoutException: Read timed out'
expect "ui-test-verdict: only the emulator infrastructure rows are infra" "0 red" "$(uv failure)"
uv_infra_pb="$(sed -n -E 's/^INFRA_PB=\((.*)\)$/\1/p' "$uv_script" 2> /dev/null || true)"
uv_rows=0
for pb_code in $uv_infra_pb; do
  grep -qE "^\| $pb_code \|" docs/ops/failure-playbook.md && uv_rows=$((uv_rows + 1))
done
expect "ui-test-verdict: INFRA_PB is PB-01 PB-11 PB-13 PB-15, each a row of the playbook" "PB-01 PB-11 PB-13 PB-15 4" \
  "$uv_infra_pb $uv_rows"

# A real failure of the first run stays red when the second run fails on infrastructure.
uv_reset
uv_put ui-test-first.log "$uv_failed_test"
uv_put logcat-first.txt 'I VGL.Home: shown'
cp "$pb_dir/PB-01.log" "$uv_dir/ui-test.log"
uv_put logcat.txt "$uv_pb01"
uv_summary 3 1
uv_first_real="$(uv failure)"
cp "$pb_dir/PB-01.log" "$uv_dir/ui-test-first.log"
uv_put logcat-first.txt "$uv_pb01"
expect "ui-test-verdict: a real first failure stays red when the rerun fails on infrastructure" "0 red 0 infra" \
  "$uv_first_real $(uv failure)"
# Gradle (AGP CustomTestRunListener) prints a failed device test as "<class> > <test>[<device>] \e[31mFAILED \e[0m":
# the colour codes and the trailing space must not hide the failure (review of T-075, round 1).
uv_agp_failed=$'com.vglstudio.camera.pose.line.guide.AppLaunchTest > launches[test(AVD) - 14] \033[31mFAILED \033[0m'
uv_reset
uv_put ui-test-first.log '> Task :app:connectedStagingDebugAndroidTest FAILED' "$uv_agp_failed"
uv_put logcat-first.txt 'I VGL.Home: shown'
uv_put ui-test.log 'BUILD SUCCESSFUL in 1m'
uv_summary 3 0
uv_agp_flaky="$(uv success)"
cp "$pb_dir/PB-01.log" "$uv_dir/ui-test.log"
uv_put logcat.txt "$uv_pb01"
uv_summary 3 1
uv_agp_red="$(uv failure)"
uv_put ui-test-first.log '> Task :app:connectedStagingDebugAndroidTest FAILED' \
  'Tests on test(AVD) - 14 failed: There was 1 failure(s).'
expect "ui-test-verdict: a failed test in Gradle's coloured format is a real first failure (flaky, red, summary line)" \
  "0 flaky 0 red 0 red" "$uv_agp_flaky $uv_agp_red $(uv failure)"
# The flaky step of main-guard.yml reads the test names from the same coloured lines.
uv_flaky_names() { # <workflow>: the test names the tests= line of the flaky step of ui-test finds in ui-test-first.log
  local line
  line="$(step_run "$(ui_steps "$1" | step_by_name 'Ghi test chập chờn')" | grep -E '^tests=' || true)"
  [[ -n "$line" ]] || { echo none; return 0; }
  (cd "$uv_dir" && eval "$line" && printf '%s' "$tests")
}
expect "main-guard: the flaky step of ui-test reads the name of a test in Gradle's coloured format" \
  'com.vglstudio.camera.pose.line.guide.AppLaunchTest > launches[test(AVD) - 14]' \
  "$(uv_put ui-test-first.log '> Task :app:connectedStagingDebugAndroidTest FAILED' "$uv_agp_failed"; uv_flaky_names "$guard_wf")"
# scripts/ui-test-needed.sh runs in guard, not in ui-test: a commit changing only it is not the ui-test job broken.
uv_reset scripts/ui-test-needed.sh
expect "ui-test-verdict: no log after a commit changing only ui-test-needed.sh is infra" "0 infra" "$(uv failure)"

uv_reset
expect "ui-test-verdict: an unknown outcome is infra" "0 infra 0 infra 0 infra" "$(uv cancelled) $(uv skipped) $(uv '')"

# The check step prints ui-verdict.txt: gh run view --log-failed of that step then looks up the right playbook row,
# never PB-02 (GitHub did not start the job), whose pattern is "HẠ TẦNG:".
uv_step_log() { # [<sed expression for the first line>]: the log of the check step as gh run view --log-failed prints it
  local p=$'ui-test\tKết quả test trên emulator\t2026-10-30T00:00:00.0000000Z '
  [[ -f "$uv_dir/ui-verdict.txt" ]] || { echo "${p}##[error]Không có ui-verdict.txt." > "$work/uv-step.log"; return 0; }
  {
    echo "${p}##[error]$(head -n 1 "$uv_dir/ui-verdict.txt" | sed -e "${1:-s/^//}")"
    tail -n +2 "$uv_dir/ui-verdict.txt" | awk -v p="$p" '{ print p $0 }'
  } > "$work/uv-step.log"
}
uv_reset
uv_summary 0 0
cp "$pb_dir/PB-11.log" "$uv_dir/ui-test.log"
uv failure > /dev/null
uv_step_log
uv_lookup_11="$(pb_head "$work/uv-step.log")"
uv_step_log 's/^EMULATOR HẠ TẦNG ([^)]*)/HẠ TẦNG/'
uv_lookup_old="$(pb_head "$work/uv-step.log")"
uv_reset
uv_summary 0 0
cp "$pb_dir/PB-15.log" "$uv_dir/ui-test.log"
uv failure > /dev/null
uv_step_log
expect "ui-test-verdict: the check step log looks up the right playbook row" "PB-11 chay-lai PB-15 lich-thu-lai PB-02 cho" \
  "$uv_lookup_11 $(pb_head "$work/uv-step.log") $uv_lookup_old"
# Infra without a row (a broken device, no log): a fixed label, which the playbook does not read as PB-02.
uv_reset
uv_summary 0 0
uv_put ui-test.log 'com.android.builder.testing.api.DeviceException: No connected devices!'
uv failure > /dev/null
uv_step_log
uv_label_device="$(grep -c 'EMULATOR HẠ TẦNG (thiết bị): ' "$work/uv-step.log" || true) $(pb_head "$work/uv-step.log")"
uv_reset
uv failure > /dev/null
uv_step_log
expect "ui-test-verdict: infra without a playbook row has a fixed label and is not PB-02" "1 khong-co 1 khong-co" \
  "$uv_label_device $(grep -c 'EMULATOR HẠ TẦNG (không có log): ' "$work/uv-step.log" || true) $(pb_head "$work/uv-step.log")"

expect "ui-test scripts are executable in git" "100755 100755 100755" \
  "$(git ls-files -s scripts/ui-test-needed.sh scripts/ui-test-on-emulator.sh scripts/ui-test-verdict.sh | awk '{ print $1 }' | tr '\n' ' ' | sed 's/ $//')"
export GIT_CONFIG_COUNT="${uin_git_config[0]}" GIT_CONFIG_KEY_0="${uin_git_config[1]}" GIT_CONFIG_VALUE_0="${uin_git_config[2]}"

# is-exact-revert.sh (ADR-0025, point 3), on a throwaway repo: A, B (the culprit), C (main).
root="$PWD"
rev="$work/rev"
git_t() { GIT_AUTHOR_NAME=t GIT_AUTHOR_EMAIL=t@t GIT_COMMITTER_NAME=t GIT_COMMITTER_EMAIL=t@t git -C "$rev" "$@"; }
git init -q -b main "$rev"
echo a > "$rev/a"; git_t add a; git_t commit -q -m A
echo b > "$rev/b"; echo a2 > "$rev/a"; git_t add a b; git_t commit -q -m B
culprit="$(git_t rev-parse HEAD)"
echo c > "$rev/c"; git_t add c; git_t commit -q -m C
git_t switch -q -c good main; git_t revert --no-edit "$culprit" > /dev/null
git_t switch -q -c extra main; git_t revert --no-edit "$culprit" > /dev/null
echo x > "$rev/x"; git_t add x; git_t commit -q --amend --no-edit
git_t switch -q -c two main; echo y > "$rev/y"; git_t add y; git_t commit -q -m Y; git_t revert --no-edit "$culprit" > /dev/null
git_t switch -q -c old "$culprit"; git_t revert --no-edit "$culprit" > /dev/null
git_t switch -q -c other main; git_t revert --no-edit main > /dev/null
git_t switch -q main
is_revert() { # <branch>; prints the exit code
  local code=0
  (cd "$rev" && "$root/scripts/is-exact-revert.sh" "$culprit" "$(git_t rev-parse "$1")" main) > /dev/null 2>&1 || code=$?
  echo "$code"
}
expect "is-exact-revert: git revert of the culprit on main passes" 0 "$(is_revert good)"
expect "is-exact-revert: a revert with an extra change fails" 1 "$(is_revert extra)"
expect "is-exact-revert: two commits fail" 1 "$(is_revert two)"
expect "is-exact-revert: a revert on an older main fails" 1 "$(is_revert old)"
expect "is-exact-revert: the revert of another commit fails" 1 "$(is_revert other)"
expect "is-exact-revert: leaves no worktree behind" 1 "$(git_t worktree list | wc -l | tr -d ' ')"

# merge-if-green.sh lets an exact revert through while main is broken (ADR-0025, point 3), and nothing else.
# The PR head is fetched from a local bare copy standing in for GitHub (url.<path>.insteadOf).
git clone -q --bare "$rev" "$work/origin.git"
git -C "$work/origin.git" update-ref refs/pull/12/head "$(git_t rev-parse good)"
printf '#!/bin/sh\nexit 0\n' > "$work/bin/revert-yes"
printf '#!/bin/sh\necho "cây file khác"\nexit 1\n' > "$work/bin/revert-no"
chmod +x "$work/bin/revert-yes" "$work/bin/revert-no"
merge_while_broken() { # <issues JSON> [IS_EXACT_REVERT] [run status] [head ref]; prints the exit code
  cat > "$work/broken.json" << JSON
{"routes": {
  "pulls/12": {"state": "open", "draft": false, "base": {"ref": "main"}, "head": {"sha": "$(git_t rev-parse good)", "ref": "${4:-revert/${culprit::7}}"}, "title": "Revert", "labels": []},
  "issues?state=open&labels=main-broken&per_page=10": $1,
  "branches?per_page=100&page=1": [{"name": "main"}, {"name": "claude/RUN-20261016-01-report"}],
  "contents/docs/runs/RUN-20261016-01.md?ref=claude/RUN-20261016-01-report": "---\nid: RUN-20261016-01\nstatus: ${3:-finished}\n---\n",
  "compare/main...$(git_t rev-parse good)": {"behind_by": 2}
}}
JSON
  local code=0
  (cd "$rev" && PATH="$work/bin:$PATH" FIXTURE="$work/broken.json" IS_EXACT_REVERT="${2:-}" \
    GIT_CONFIG_COUNT=2 GIT_CONFIG_KEY_1="url.$work/origin.git.insteadOf" GIT_CONFIG_VALUE_1=https://github.com/o/r.git \
    "$root/scripts/merge-if-green.sh" 12 --dry-run) > "$work/broken.out" 2>&1 || code=$?
  echo "$code"
}
one_issue='[{"number": 7, "body": "<!-- culprit:'"$culprit"' -->\nNhánh chính không qua cổng chất lượng."}]'
# Exit 3 ("branch is behind main", from the fixture) means the main-broken check let the PR through.
expect "merge-if-green lets an exact revert through while main is broken" 3 \
  "$(merge_while_broken "$one_issue" "$work/bin/revert-yes")"
expect "merge-if-green says why it let the revert through" 1 "$(grep -c '^BẢN REVERT ĐÚNG: PR #12' "$work/broken.out")"
expect "merge-if-green checks the real revert with is-exact-revert.sh" 3 "$(merge_while_broken "$one_issue")"
expect "merge-if-green refuses a PR that is not the revert" 1 "$(merge_while_broken "$one_issue" "$work/bin/revert-no")"
expect "merge-if-green gives the reason of is-exact-revert.sh" 1 "$(grep -c 'cây file khác' "$work/broken.out")"
expect "merge-if-green refuses with two main-broken issues" 1 \
  "$(merge_while_broken '[{"number": 7, "body": "<!-- culprit:'"$culprit"' -->"}, {"number": 8, "body": "<!-- culprit:'"$culprit"' -->"}]' "$work/bin/revert-yes")"
expect "merge-if-green refuses an issue without culprit" 1 \
  "$(merge_while_broken '[{"number": 7, "body": "Nhánh chính đỏ."}]' "$work/bin/revert-yes")"
expect "merge-if-green lets a PR through when main is not broken" 3 "$(merge_while_broken '[]' "$work/bin/revert-no")"
# The run file here has no table, so T-099 is in no list: main-broken still refuses the branch, and only the exact
# revert passes both the main-broken check and the run check.
expect "merge-if-green: main-broken refuses a task branch, the exact revert passes the run check" "1 1 3" \
  "$(merge_while_broken "$one_issue" "$work/bin/revert-no" running claude/T-099-x) $(grep -c 'main-broken' "$work/broken.out") $(merge_while_broken "$one_issue" "$work/bin/revert-yes" running claude/T-099-x)"
expect "merge-if-green lets the exact revert through while a run is running" 3 \
  "$(merge_while_broken "$one_issue" "$work/bin/revert-yes" running)"
expect "merge-if-green still refuses a revert branch that is not checked while a run is running" 1 \
  "$(merge_while_broken '[]' "$work/bin/revert-yes" running)"
# The main-broken issue of the emulator job has one more marker line; its culprit still gets the exact revert (ADR-0043).
ui_issue='[{"number": 7, "body": "<!-- culprit:'"$culprit"' -->\n<!-- check:ui-test -->\nTest trên emulator không qua."}]'
expect "merge-if-green: an exact revert passes a main-broken issue of ui-test" "3 1" \
  "$(merge_while_broken "$ui_issue" "$work/bin/revert-yes") $(merge_while_broken "$ui_issue" "$work/bin/revert-no")"

# merge-if-green: no update when main changed only docs (ADR-0041). A code PR behind main is merged without updating
# its branch when, since the split, main changed nothing but docs (the rule of main-guard: docs/** and *.md, without
# the docs files Gradle reads), no file changed on both sides, the PR ran check.sh in pr-check and GitHub says it does
# not conflict. Every other case updates as before. The real main-guard-verified.sh answers both questions.
nu_routes() { # [<conclusion of the gate step>] [<mergeable JSON>]; the data of a code PR behind main by docs, in $work/mg.json
  merge_routes claude/T-901-x '{"behind_by": 2}' "$code_runs"
  python3 -I - "$work/mg.json" "${1:-success}" "${2:-true}" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1])); r = fx["routes"]
r["pulls/12"]["mergeable"] = json.loads(sys.argv[3])
r["compare/abc1234...main"] = {"files": [{"filename": "docs/runs/RUN-20261030-01.md"}, {"filename": "docs/lessons-2.md"}]}
r["pulls/12/files?per_page=100&page=1"] = [{"filename": "app/src/main/x.kt"}, {"filename": "docs/features/f/plans/T-901.md"}]
r["actions/workflows/pr-check.yml/runs?head_sha=abc1234&per_page=10"] = {"workflow_runs": [
    {"id": 91, "created_at": "2026-10-29T00:00:00Z", "conclusion": "success"}]}
r["actions/runs/91/jobs?per_page=50"] = {"jobs": [{"name": "build-and-test", "steps": [
    {"name": "Set up job", "conclusion": "success"},
    {"name": "Cổng chất lượng (scripts/check.sh)", "conclusion": sys.argv[2]}]}]}
json.dump(fx, open(sys.argv[1], "w"))
PY
}
nu_edit() { # <python statements on fx and r, the routes of $work/mg.json> [<argument>...], read as sys.argv[3:]
  python3 -I - "$work/mg.json" "$@" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1])); r = fx["routes"]
exec(sys.argv[2])
json.dump(fx, open(sys.argv[1], "w"))
PY
}
nu_writes() { cat "$work/mg.json.writes" 2> /dev/null | tr '\n' ';'; }
nu_count() { grep -c -- "$1" "$work/mg.out" || true; } # <text>: lines of $work/mg.out with it
nu_wcount() { cat "$work/mg.json.writes" 2> /dev/null | grep -c -- "$1" || true; } # <text>: writes with it

# Test 5: merged without updating; --dry-run says so; --update does not update either.
nu_routes
expect "merge-if-green: a code PR behind main by docs only merges without updating" \
  "0 PUT pulls/12/merge;DELETE git/refs/heads/claude/T-901-x; 1 1" \
  "$(merge_run 12) $(nu_writes) $(nu_count '^KHÔNG CẬP NHẬT NHÁNH: ') $(nu_count '^ĐÃ MERGE: PR #12 (t) tại commit abc1234')"
expect "merge-if-green --dry-run: a code PR behind main by docs only is ready, without updating" "0 1 1 0" \
  "$(merge_run 12 --dry-run) $(nu_count '^KHÔNG CẬP NHẬT NHÁNH: ') $(nu_count '^SẴN SÀNG: ') $(nu_wcount .)"
expect "merge-if-green --update: a code PR behind main by docs only merges without updating" "0 0 1" \
  "$(merge_run 12 --update) $(nu_wcount update-branch) $(nu_wcount 'PUT pulls/12/merge')"
nu_edit 'r["pulls/12/files?per_page=100&page=1"].append({"filename": "docs/STATUS.md"})'
expect "merge-if-green: a PR merged without updating still gets the fresh-commit and STATUS.md warnings" "0 1 1" \
  "$(NOW_EPOCH=1792288980 merge_run 12 --dry-run) $(nu_count 'mới 3 phút trước') $(nu_count 'PR có docs/STATUS.md')"

# Test 6: main changed code since the split, so the branch is updated (with --update: the old path, def5678).
nu_routes
nu_edit 'r["compare/abc1234...main"]["files"].append({"filename": "core/x.kt"})'
expect "merge-if-green: main changed code since the split, so the branch is updated" "3 1" \
  "$(merge_run 12) $(nu_count 'core/x.kt')"
nu_edit '
r["compare/main...def5678"] = {"behind_by": 0}
r["commits/def5678/check-runs?per_page=100"] = {"__sequence__": [{"check_runs": []}, r["commits/abc1234/check-runs?per_page=100"]]}
r["commits/def5678"] = {"commit": {"committer": {"date": "2026-10-18T02:30:00Z"}, "message": "Merge main"}}
second = json.loads(json.dumps(r["pulls/12"])); second["head"]["sha"] = "def5678"
r["pulls/12"] = {"__sequence__": [r["pulls/12"], second]}'
expect "merge-if-green --update: main changed code since the split, so the branch is updated, then merged" \
  "0 PUT pulls/12/update-branch;PUT pulls/12/merge;DELETE git/refs/heads/claude/T-901-x;" \
  "$(merge_run 12 --update) $(nu_writes)"

# Test 7: a docs file Gradle reads counts as code; another docs file does not.
nu_routes
nu_edit 'r["compare/abc1234...main"]["files"] = [{"filename": "docs/remote-config-keys.md"}]'
expect "merge-if-green: a docs file Gradle reads counts as code (remote-config-keys.md)" 3 "$(merge_run 12)"
nu_edit 'r["compare/abc1234...main"]["files"] = [{"filename": "docs/design-system/tokens.json"}]'
expect "merge-if-green: a docs file Gradle reads counts as code (tokens.json)" 3 "$(merge_run 12)"
nu_edit 'r["compare/abc1234...main"]["files"] = [{"filename": "docs/other.json"}]'
expect "merge-if-green: a docs file Gradle does not read is docs" 0 "$(merge_run 12)"

# Test 8: a PR that did not run check.sh in pr-check is always updated.
nu_routes skipped
nu8="$(merge_run 12) $(nu_count 'PR không chạy check.sh')"
nu_routes
nu_edit 'r["actions/runs/91/jobs?per_page=50"]["jobs"][0]["name"] = "build"'
nu8="$nu8 $(merge_run 12)"
nu_routes
nu_edit 'r["actions/workflows/pr-check.yml/runs?head_sha=abc1234&per_page=10"]["workflow_runs"][0]["conclusion"] = "failure"'
nu8="$nu8 $(merge_run 12)"
nu_routes
nu_edit 'r["pulls/12/files?per_page=100&page=1"] = [{"filename": "feature/x/README.md"}]'
nu8="$nu8 $(merge_run 12) $(nu_count 'PR chỉ đổi file mà main-guard bỏ qua')"
expect "merge-if-green: a PR that did not run check.sh is always updated" "3 1 3 3 3 1" "$nu8"
# A PR with only files that the paths-ignore of main-guard skips is updated, even with a docs file Gradle reads: its
# merge commit would get no main-guard run at all (T-094, L-152).
nu_ignored() { # the data of nu_routes for a PR with feature/x/README.md and docs/remote-config-keys.md
  nu_routes
  nu_edit 'r["pulls/12/files?per_page=100&page=1"] = [{"filename": "feature/x/README.md"}, {"filename": "docs/remote-config-keys.md"}]'
}
nu_ignored
nu_gi="$(merge_run 12) $(nu_count 'PR chỉ đổi file mà main-guard bỏ qua')"
nu_edit 'r["pulls/12/files?per_page=100&page=1"] = [{"filename": "docs/remote-config-keys.md"}]'
nu_gi="$nu_gi $(merge_run 12)"
nu_ignored
nu_gi="$nu_gi $(merge_run 12 --dry-run)"
expect "merge-if-green: a PR with only files main-guard ignores is updated, even a docs file Gradle reads" "3 1 3 3" "$nu_gi"
nu_updated() { # [<check-runs answer of def5678>]: --update moves the PR head to def5678, whose CI first has no run
  nu_edit '
r["compare/main...def5678"] = {"behind_by": 0}
r["commits/def5678/check-runs?per_page=100"] = {"__sequence__": [{"check_runs": []}, json.loads(sys.argv[3])]}
r["commits/def5678"] = {"commit": {"committer": {"date": "2026-10-18T02:30:00Z"}, "message": "Merge main"}}
second = json.loads(json.dumps(r["pulls/12"])); second["head"]["sha"] = "def5678"
r["pulls/12"] = {"__sequence__": [r["pulls/12"], second]}' "${1:-$code_runs}"
}
nu_ignored
nu_updated
expect "merge-if-green --update: a PR with only files main-guard ignores is updated, then merged" \
  "0 PUT pulls/12/update-branch;PUT pulls/12/merge;DELETE git/refs/heads/claude/T-901-x;" \
  "$(UPDATE_TIMEOUT=60 merge_run 12 --update) $(nu_writes)"
nu_routes
nu_edit 'r["pulls/12/files?per_page=100&page=1"] = [{"filename": "app/src/main/x.kt"}, {"filename": "docs/remote-config-keys.md"}]'
expect "merge-if-green: a code PR with a docs file Gradle reads still merges without updating" "0 1" \
  "$(merge_run 12) $(nu_count '^KHÔNG CẬP NHẬT NHÁNH: ')"
# A main-guard-verified.sh that does not know --guard-ignores (full) or answers nothing: the branch is updated.
printf '#!/bin/sh\nif [ "$1" = --docs-only ]; then echo yes; else echo "KIỂM ĐẦY ĐỦ: cách dùng." >&2; echo full; fi\n' \
  > "$work/mgv-gi-full.sh"
printf '#!/bin/sh\nif [ "$1" = --docs-only ]; then echo yes; fi\n' > "$work/mgv-gi-empty.sh"
chmod +x "$work/mgv-gi-full.sh" "$work/mgv-gi-empty.sh"
nu_routes
nu_gi7="$(MAIN_GUARD_VERIFIED_SCRIPT="$work/mgv-gi-full.sh" merge_run 12) $(nu_count 'không đọc được PR có chỉ đổi file mà main-guard bỏ qua không')"
nu_gi7="$nu_gi7 $(MAIN_GUARD_VERIFIED_SCRIPT="$work/mgv-gi-empty.sh" merge_run 12) $(nu_count 'không đọc được PR có chỉ đổi file mà main-guard bỏ qua không')"
expect "merge-if-green: an unreadable --guard-ignores answer updates" "3 1 3 1" "$nu_gi7"
# After the update, the checks on the new head still apply: red is refused, still running waits.
nu_ignored
nu_updated "$(one_job_with build-and-test completed '"failure"')"
expect "merge-if-green --update, only files main-guard ignores: a red check after the update is refused" "1 1 0" \
  "$(UPDATE_TIMEOUT=60 merge_run 12 --update) $(nu_wcount 'PUT pulls/12/update-branch') $(nu_wcount 'PUT pulls/12/merge')"
nu_ignored
nu_updated
nu_edit 'r["commits/def5678/check-runs?per_page=100"] = json.loads(sys.argv[3])' "$(one_job_with build-and-test in_progress null)"
expect "merge-if-green --update, only files main-guard ignores: CI still going after the update waits" "2 1 yes 0" \
  "$(merge_run 12 --update) $(nu_wcount 'PUT pulls/12/update-branch') $([[ "$(nu_count 'đang ở trạng thái in_progress')" -ge 1 ]] && echo yes || echo no) $(nu_wcount 'PUT pulls/12/merge')"
nu_ignored
nu_edit 'fx["fail_writes"] = ["PUT pulls/12/update-branch"]'
expect "merge-if-green --update, only files main-guard ignores: a refused update-branch is refused" "1 1" \
  "$(merge_run 12 --update) $(nu_count 'không cập nhật được nhánh')"
nu_ignored
expect "merge-if-green --update, only files main-guard ignores: a head that does not move waits" "2 1 yes" \
  "$(merge_run 12 --update) $(nu_wcount 'PUT pulls/12/update-branch') $([[ "$(nu_count 'chưa cập nhật xong')" -ge 1 ]] && echo yes || echo no)"
nu_ignored
nu_edit '
heads = ["abc1234", "b100001", "b100002", "b100003"]
copies = []
for h in heads:
    c = json.loads(json.dumps(r["pulls/12"])); c["head"]["sha"] = h; copies.append(c)
    r["compare/main..." + h] = {"behind_by": 2}
    r["compare/" + h + "...main"] = json.loads(json.dumps(r["compare/abc1234...main"]))
r["pulls/12"] = {"__sequence__": copies}'
expect "merge-if-green --update, only files main-guard ignores: three updates without catching up are refused" "1 3 1" \
  "$(UPDATE_TIMEOUT=60 merge_run 12 --update) $(nu_wcount 'PUT pulls/12/update-branch') $(nu_count 'Đã cập nhật nhánh 3 lần')"

# Test 9: a conflicting PR updates; an unknown mergeable state is read again, then waits.
nu_routes success false
nu9="$(merge_run 12)"
nu_routes success null
nu_edit '
p_null = r["pulls/12"]; p_true = json.loads(json.dumps(p_null)); p_true["mergeable"] = True
r["pulls/12"] = {"__sequence__": [p_null, p_null, p_true]}'
nu9="$nu9 $(MERGE_MERGEABLE_WAIT_SECONDS=0 merge_run 12) $(nu_wcount update-branch)"
nu_routes success null
nu9="$nu9 $(MERGE_MERGEABLE_WAIT_SECONDS=0 merge_run 12) $(nu_count 'chưa tính xong')"
nu9="$nu9 $(MERGE_MERGEABLE_WAIT_SECONDS=0 MERGE_UPDATE_TIMEOUT_SECONDS=0 merge_run 12 --update) $(nu_wcount update-branch)"
nu_routes success '"maybe"'
nu9="$nu9 $(merge_run 12)"
expect "merge-if-green: a conflicting PR updates, an unknown mergeable state waits" "3 0 0 2 1 2 0 3" "$nu9"

# Test 10: a file changed on both sides updates, also through a rename in the PR.
nu_routes
nu_edit 'r["compare/abc1234...main"]["files"].append({"filename": "docs/features/f/plans/T-901.md"})'
nu10="$(merge_run 12) $(nu_count 'cả PR và nhánh chính cùng đổi')"
nu_routes
nu_edit 'r["pulls/12/files?per_page=100&page=1"].append({"filename": "docs/lessons-3.md", "previous_filename": "docs/lessons-2.md"})'
nu10="$nu10 $(merge_run 12)"
expect "merge-if-green: a file changed on both sides updates" "3 1 3" "$nu10"

# Test 11: a rename on main counts its old path.
nu_routes
nu_edit 'r["compare/abc1234...main"]["files"] = [{"filename": "docs/x.kt.md", "previous_filename": "app/x.kt", "status": "renamed"}]'
expect "merge-if-green: a rename on main counts its old path" 3 "$(merge_run 12)"

# Test 12: an answer the script cannot read updates.
nu_routes
nu_edit 'fx["errors"] = {"compare/abc1234...main": "HTTP 500: Server Error"}'
nu12="$(merge_run 12)"
nu_routes
nu_edit 'r["compare/abc1234...main"] = {"behind_by": 2}'
nu12="$nu12 $(merge_run 12)"
nu_routes
nu_edit 'r["compare/abc1234...main"]["files"] = [{"filename": "docs/a%d.md" % n} for n in range(300)]'
nu12="$nu12 $(merge_run 12)"
nu_routes
nu_edit 'fx["errors"] = {"pulls/12/files?per_page=100&page=1": "HTTP 500: Server Error"}'
nu12="$nu12 $(merge_run 12)"
nu_routes
nu_edit 'fx["errors"] = {"actions/runs/91/jobs?per_page=50": "HTTP 500: Server Error"}'
nu12="$nu12 $(merge_run 12)"
nu_routes
printf '#!/bin/sh\necho yes\n' > "$work/mgv-noexec.sh"
printf '#!/bin/sh\necho "KIỂM ĐẦY ĐỦ: cách dùng." >&2\necho full\n' > "$work/mgv-old.sh"
chmod +x "$work/mgv-old.sh"
nu12="$nu12 $(MAIN_GUARD_VERIFIED_SCRIPT="$work/mgv-noexec.sh" merge_run 12)"
nu12="$nu12 $(MAIN_GUARD_VERIFIED_SCRIPT="$work/mgv-old.sh" merge_run 12)"
expect "merge-if-green: an unreadable answer updates" "3 3 3 3 3 3 3" "$nu12"

# Test 13: behind by docs with CI still going waits, it does not update.
nu_checks() { nu_edit 'r["commits/abc1234/check-runs?per_page=100"] = json.loads(sys.argv[3])' "$1"; } # <check-runs answer>
nu_routes
nu_checks "$lagging_runs"
nu13="$(merge_run 12) $(nu_count 'CI chưa chạy xong')"
nu_checks '{"check_runs": []}'
nu13="$nu13 $(merge_run 12) $(nu_count 'CI chưa chạy trên commit này')"
expect "merge-if-green: behind by docs with CI still going waits" "2 1 2 1" "$nu13"

# Tests 18 to 26: every check merge-if-green.sh already had still applies to a PR behind main by docs (L-058).
nu_routes
nu_edit 'r["pulls/12"]["state"] = "closed"'
expect "merge-if-green, behind by docs: a closed PR is still refused" 1 "$(merge_run 12)"
nu_routes
nu_edit 'r["pulls/12"]["draft"] = True'
expect "merge-if-green, behind by docs: a draft is still refused" "1 1" "$(merge_run 12) $(nu_count 'đang là bản nháp')"
nu_routes
nu_edit 'r["pulls/12"]["labels"] = [{"name": "needs-human"}]'
expect "merge-if-green, behind by docs: needs-human is still refused" 1 "$(merge_run 12)"
nu_routes
nu_edit 'r["pulls/12"]["base"]["ref"] = "other"'
expect "merge-if-green, behind by docs: a PR into another branch is still refused" 1 "$(merge_run 12)"
nu_sha() { # <sha>: the data of nu_routes for that PR head instead of abc1234
  python3 -I -c 'import sys; p = sys.argv[1]; t = open(p).read(); open(p, "w").write(t.replace("abc1234", sys.argv[2]))' \
    "$work/mg.json" "$1"
}
# main-broken: the PR of the branch good (not the revert, revert-no) behind main by docs, in the repo of is-exact-revert.
nu_routes
nu_sha "$(git_t rev-parse good)"
nu_edit 'r["issues?state=open&labels=main-broken&per_page=10"] = json.loads(sys.argv[3])' "$one_issue"
rm -f "$work/mg.json.writes" "$work/mg.json.seen"
code=0
(cd "$rev" && PATH="$work/bin:$PATH" FIXTURE="$work/mg.json" IS_EXACT_REVERT="$work/bin/revert-no" MERGE_POLL_SECONDS=0 \
  GIT_CONFIG_COUNT=2 GIT_CONFIG_KEY_1="url.$work/origin.git.insteadOf" GIT_CONFIG_VALUE_1=https://github.com/o/r.git \
  "$root/scripts/merge-if-green.sh" 12) > "$work/broken.out" 2>&1 || code=$?
expect "merge-if-green, behind by docs: main-broken still refuses" "1 0 0" \
  "$code $(nu_wcount .) $(grep -c 'KHÔNG CẬP NHẬT NHÁNH' "$work/broken.out" || true)"
nu_routes
nu_edit '
r["branches?per_page=100&page=1"] = [{"name": "main"}, {"name": "claude/RUN-20261016-01-report"}]
r["contents/docs/runs/RUN-20261016-01.md?ref=claude/RUN-20261016-01-report"] = "---\nid: RUN-20261016-01\nstatus: running\n---\n\n| 1 | T-901 | demo |\n"
r["pulls/12"]["head"]["ref"] = "claude/T-902-x"'
nu902="$(merge_run 12) $(nu_wcount .)"
nu_edit 'r["pulls/12"]["head"]["ref"] = "claude/template-x"'
nu24="$(merge_run 12)"
nu_edit 'r["pulls/12"]["head"]["ref"] = "claude/T-901-x"'
expect "merge-if-green, behind by docs: a running session still refuses a person's PR" "1 0" "$nu24 $(merge_run 12)"
expect "merge-if-green, behind by docs: a running session refuses a task not in its list, nothing written" "1 0" "$nu902"

# A cancelled check with a newer run of the same check on the same commit (L-164). GitHub answers check-runs with
# filter=latest, so two check runs of one name come from two workflow runs: each run its own check suite.
runs_of() { # <name|status|conclusion JSON|started_at JSON|suite id[|app id]>...; prints a check-runs answer
  local out="" sep="" spec name status conclusion started suite app app_json
  for spec in "$@"; do
    IFS='|' read -r name status conclusion started suite app <<< "$spec"
    app_json=""
    [[ -z "$app" ]] || app_json=", \"app\": {\"id\": $app}"
    out+="$sep{\"name\": \"$name\", \"status\": \"$status\", \"conclusion\": $conclusion, \"started_at\": $started, \"check_suite\": {\"id\": $suite}$app_json}"
    sep=", "
  done
  printf '{"check_runs": [%s]}' "$out"
}
at_a='"2026-10-18T02:01:00Z"'
at_b='"2026-10-18T02:02:00Z"'
mg_says() { grep -c -- "$1" "$work/mg.out" || true; } # <text>: lines of $work/mg.out with it
two_suites=', "actions/runs?head_sha=abc1234&per_page=50": {"workflow_runs": [
  {"id": 76, "check_suite_id": 5, "status": "completed", "conclusion": "cancelled"},
  {"id": 77, "check_suite_id": 6, "status": "in_progress", "conclusion": null}]}'
l164_runs="$(runs_of "build-and-test|completed|\"cancelled\"|$at_a|5" "build-and-test|in_progress|null|$at_b|6")"
t2_runs="$(runs_of "build-and-test|completed|\"cancelled\"|$at_a|5" "build-and-test|completed|\"success\"|$at_b|6")"
merge_routes claude/template-x '{"behind_by": 0}' "$l164_runs" "$two_suites"
expect "merge-if-green: a cancelled check with a newer run of the same check waits for that run" "2 1 0" \
  "$(merge_run 12 --dry-run) $(mg_says 'đang ở trạng thái in_progress') $(mg_says 'có kiểm tra không qua')"
merge_routes claude/template-x '{"behind_by": 0}' "$t2_runs"
expect "merge-if-green: a cancelled check with a newer green run of the same check is ready" 0 "$(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' \
  "$(runs_of "build-and-test|completed|\"success\"|$at_a|5" "build-and-test|completed|\"cancelled\"|$at_b|6")"
expect "merge-if-green: the newest run of a check cancelled is still refused" "1 1" \
  "$(merge_run 12 --dry-run) $(mg_says 'có kiểm tra không qua: build-and-test\.')"
merge_routes claude/template-x '{"behind_by": 0}' "$(runs_of "build-and-test|completed|\"cancelled\"|$at_a|5")"
expect "merge-if-green: a lone cancelled check is still refused" "1 1" \
  "$(merge_run 12 --dry-run) $(mg_says 'có kiểm tra không qua: build-and-test\.')"
merge_routes claude/template-x '{"behind_by": 0}' \
  "$(runs_of "build-and-test|completed|\"failure\"|$at_a|5" "build-and-test|completed|\"success\"|$at_b|6")"
mg_old_failure="$(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' \
  "$(runs_of "build-and-test|completed|\"timed_out\"|$at_a|5" "build-and-test|completed|\"success\"|$at_b|6")"
expect "merge-if-green: an older failed or timed-out run is not hidden by a newer green run" "1 1" \
  "$mg_old_failure $(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' \
  "$(runs_of "build-and-test|completed|\"cancelled\"|$at_a|5" "build-and-test|queued|null|null|6")"
expect "merge-if-green: a run not started yet counts as newer than a cancelled one" "2 1" \
  "$(merge_run 12 --dry-run) $(mg_says 'đang ở trạng thái queued')"
merge_routes claude/template-x '{"behind_by": 0}' \
  "$(runs_of "build-and-test|completed|\"success\"|$at_a|5" "build-and-test|queued|null|null|6")"
expect "merge-if-green: a run not started yet is the newest run of its check" 2 "$(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' \
  "$(runs_of "build-and-test|completed|\"cancelled\"|$at_a|5|1" "build-and-test|completed|\"success\"|$at_b|6|2")"
mg_other_app="$(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' \
  "$(runs_of "build-and-test|completed|\"cancelled\"|$at_a|5" "other|completed|\"success\"|$at_b|6")"
mg_other_name="$(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' \
  "$(runs_of "build-and-test|completed|\"cancelled\"|$at_a|5" "build-and-test|completed|\"success\"|$at_a|6")"
expect "merge-if-green: a cancelled check is not hidden by another app, another name or a run of the same time" "1 1 1" \
  "$mg_other_app $mg_other_name $(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' \
  "$(runs_of "build-and-test|completed|\"success\"|$at_a|5" "build-and-test|completed|\"cancelled\"|null|6")"
expect "merge-if-green: a cancelled run not started is not newer than itself" 1 "$(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' "{\"__sequence__\": [$l164_runs, $t2_runs]}" "$two_suites"
expect "merge-if-green --update: waits for the newer run of a cancelled check, then merges" "0 1" \
  "$(merge_run 12 --update) $(mg_merges)"
# Control: a copy with the filter of red checks from before (every cancelled check refuses) refuses the same PR.
merge_routes claude/template-x '{"behind_by": 0}' "$t2_runs"
perl -0777 -pe 's/failed_filter=\x27.*?\x27 # end of failed_filter\n/failed_filter=\x27[.[] | select(.status=="completed" and (.conclusion|IN("failure","cancelled","timed_out","action_required","startup_failure"))) | .name] | unique | join(", ")\x27\n/s' \
  scripts/merge-if-green.sh > "$work/mgi-old-filter.sh"
chmod +x "$work/mgi-old-filter.sh"
old_filter_copy="$(cmp -s scripts/merge-if-green.sh "$work/mgi-old-filter.sh" && echo same || echo differs)"
code=0
PATH="$work/bin:$PATH" FIXTURE="$work/mg.json" MERGE_POLL_SECONDS=0 MERGE_UPDATE_TIMEOUT_SECONDS=5 MERGE_NOW_EPOCH=1793000000 \
  "$work/mgi-old-filter.sh" 12 --dry-run > "$work/mg.out" 2>&1 || code=$?
expect "merge-if-green: the cancelled-check control, a copy with the old filter of red checks, refuses" "differs 1" \
  "$old_filter_copy $code"
# Every other check of the script still holds for a PR whose cancelled check has a newer green run.
merge_routes claude/template-x '{"behind_by": 0}' "$t2_runs"
nu_edit 'r["pulls/12"]["draft"] = True'
c_draft="$(merge_run 12 --dry-run) $(mg_says 'đang là bản nháp')"
merge_routes claude/template-x '{"behind_by": 0}' "$t2_runs"
nu_edit 'r["pulls/12"]["labels"] = [{"name": "needs-human"}]'
c_label="$(merge_run 12 --dry-run) $(mg_says 'needs-human')"
merge_routes claude/template-x '{"behind_by": 0}' "$t2_runs"
nu_edit '
r["branches?per_page=100&page=1"] = [{"name": "main"}, {"name": "claude/RUN-20261016-01-report"}]
r["contents/docs/runs/RUN-20261016-01.md?ref=claude/RUN-20261016-01-report"] = "---\nid: RUN-20261016-01\nstatus: running\n---\n\n| 1 | T-901 | demo |\n"'
c_run="$(merge_run 12 --dry-run) $(mg_says 'phiên RUN-20261016-01 đang chạy')"
merge_routes claude/template-x '{"behind_by": 2}' "$t2_runs"
c_behind="$(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' "$t2_runs"
nu_edit 'r["pulls/12"]["state"] = "closed"'
c_closed="$(merge_run 12 --dry-run) $(mg_says 'không ở trạng thái mở')"
merge_routes claude/template-x '{"behind_by": 0}' "$t2_runs"
nu_edit 'r["pulls/12"]["base"]["ref"] = "other"'
c_base="$(merge_run 12 --dry-run) $(mg_says "nhắm vào 'other'")"
expect "merge-if-green: a cancelled check with a newer green run still meets draft, needs-human, running session, behind, open, base" \
  "1 1 1 1 1 1 3 1 1 1 1" "$c_draft $c_label $c_run $c_behind $c_closed $c_base"
# main-broken: the PR of the branch good (not the revert), in the repo of is-exact-revert, like the docs-behind test.
broken_with_t2() { # <IS_EXACT_REVERT>; prints the exit code, output in $work/broken.out
  merge_routes claude/template-x '{"behind_by": 0}' "$t2_runs"
  nu_sha "$(git_t rev-parse good)"
  nu_edit 'r["issues?state=open&labels=main-broken&per_page=10"] = json.loads(sys.argv[3])' "$one_issue"
  rm -f "$work/mg.json.writes" "$work/mg.json.seen"
  local code=0
  (cd "$rev" && PATH="$work/bin:$PATH" FIXTURE="$work/mg.json" IS_EXACT_REVERT="$1" MERGE_POLL_SECONDS=0 \
    GIT_CONFIG_COUNT=2 GIT_CONFIG_KEY_1="url.$work/origin.git.insteadOf" GIT_CONFIG_VALUE_1=https://github.com/o/r.git \
    "$root/scripts/merge-if-green.sh" 12 --dry-run) > "$work/broken.out" 2>&1 || code=$?
  echo "$code"
}
expect "merge-if-green: main-broken still refuses a PR whose cancelled check has a newer green run" "1 1" \
  "$(broken_with_t2 "$work/bin/revert-no") $(grep -c 'main-broken' "$work/broken.out" || true)"
expect "merge-if-green: the exact revert with a cancelled check and a newer green run is ready" 0 \
  "$(broken_with_t2 "$work/bin/revert-yes")"
# Behind main by docs only: --gate-ran reads the newest pr-check run, past an older cancelled one.
nu_routes
nu_edit '
r["commits/abc1234/check-runs?per_page=100"] = json.loads(sys.argv[3])
r["actions/workflows/pr-check.yml/runs?head_sha=abc1234&per_page=10"]["workflow_runs"].append(
    {"id": 90, "created_at": "2026-10-28T23:00:00Z", "conclusion": "cancelled"})' "$t2_runs"
expect "merge-if-green, behind by docs: a cancelled check with a newer green run merges without updating" "0 1" \
  "$(merge_run 12 --dry-run) $(nu_count '^KHÔNG CẬP NHẬT NHÁNH: ')"

# With a running session, a listed task PR still meets the checks after the run check (CI), and an unlisted one is
# refused before the branch is updated; a session that starts while --update waits refuses it on the next round.
mrun_session() { # <python list of report branches for the branches route>; adds the running list with T-001
  nu_edit '
r["branches?per_page=100&page=1"] = json.loads(sys.argv[3])
r["contents/docs/runs/RUN-20261016-01.md?ref=claude/RUN-20261016-01-report"] = "---\nid: RUN-20261016-01\nstatus: running\n---\n\n| 1 | T-001 | demo |\n"' "$1"
}
mrun_two='[{"name": "main"}, {"name": "claude/RUN-20261016-01-report"}]'
red_code_runs="${code_runs/\"conclusion\": \"success\"/\"conclusion\": \"failure\"}"
merge_routes claude/T-001-x '{"behind_by": 0}' "$code_runs"
mrun_session "$mrun_two"
mrun_ready="$(merge_run 12 --dry-run)"
merge_routes claude/T-001-x '{"behind_by": 0}' "$red_code_runs"
mrun_session "$mrun_two"
expect "merge-if-green: a listed task PR is ready when green and refused when red" "0 1 1" \
  "$mrun_ready $(merge_run 12 --dry-run) $(grep -c 'có kiểm tra không qua' "$work/mg.out" || true)"
merge_routes claude/T-099-x '{"behind_by": 2}' "$code_runs"
mrun_session "$mrun_two"
expect "merge-if-green --update: an unlisted task PR is refused before anything is written" "1 0" \
  "$(merge_run 12 --update) $(nu_wcount .)"
merge_routes claude/T-099-x '{"behind_by": 2}' "$code_runs"
mrun_session '{"__sequence__": [[{"name": "main"}], [{"name": "main"}, {"name": "claude/RUN-20261016-01-report"}]]}'
# The wait must outlast the first round on a slow CI machine; the second round refuses at once, so it never runs out.
expect "merge-if-green --update: a session that starts while waiting refuses an unlisted task" "1 1 1" \
  "$(UPDATE_TIMEOUT=60 merge_run 12 --update) $(grep -c 'task T-099' "$work/mg.out" || true) $(nu_wcount 'PUT pulls/12/update-branch')"
# A PR with only files main-guard ignores (nu_ignored): every check that comes before the update still refuses it, with nothing written to GitHub (L-058).
nu_gi8="" nu_gi8w=0
for nu_case in 'r["pulls/12"]["state"] = "closed"' 'r["pulls/12"]["draft"] = True' \
  'r["pulls/12"]["labels"] = [{"name": "needs-human"}]' 'r["pulls/12"]["base"]["ref"] = "other"' '
r["branches?per_page=100&page=1"] = [{"name": "main"}, {"name": "claude/RUN-20261016-01-report"}]
r["contents/docs/runs/RUN-20261016-01.md?ref=claude/RUN-20261016-01-report"] = "---\nid: RUN-20261016-01\nstatus: running\n---\n"
r["pulls/12"]["head"]["ref"] = "claude/template-x"'; do
  nu_ignored
  nu_edit "$nu_case"
  nu_gi8="$nu_gi8$(merge_run 12) "
  nu_gi8w=$((nu_gi8w + $(nu_wcount .)))
done
nu_ignored
nu_sha "$(git_t rev-parse good)"
nu_edit 'r["issues?state=open&labels=main-broken&per_page=10"] = json.loads(sys.argv[3])' "$one_issue"
rm -f "$work/mg.json.writes" "$work/mg.json.seen"
code=0
(cd "$rev" && PATH="$work/bin:$PATH" FIXTURE="$work/mg.json" IS_EXACT_REVERT="$work/bin/revert-no" MERGE_POLL_SECONDS=0 \
  GIT_CONFIG_COUNT=2 GIT_CONFIG_KEY_1="url.$work/origin.git.insteadOf" GIT_CONFIG_VALUE_1=https://github.com/o/r.git \
  "$root/scripts/merge-if-green.sh" 12) > "$work/broken.out" 2>&1 || code=$?
nu_gi8="$nu_gi8$code"
nu_gi8w=$((nu_gi8w + $(nu_wcount .)))
expect "merge-if-green, only files main-guard ignores: every earlier check still refuses" "1 1 1 1 1 1 writes=0 broken=1" \
  "$nu_gi8 writes=$nu_gi8w broken=$(grep -c 'TỪ CHỐI: nhánh chính đang đỏ' "$work/broken.out" || true)"
nu_routes
nu_checks "$(one_job_with build-and-test completed '"failure"')"
nu25="$(merge_run 12) $(nu_count 'có kiểm tra không qua')"
nu_checks "$(one_job_with build-and-test completed '"skipped"')"
expect "merge-if-green, behind by docs: a red check is still refused" "1 1 2 0" "$nu25 $(merge_run 12) $(nu_wcount .)"
nu_routes
nu_checks "$lagging_runs"
nu_edit 'r["actions/runs?head_sha=abc1234&per_page=50"] = {"workflow_runs": [
    {"id": 77, "check_suite_id": 5, "status": "completed", "conclusion": "success"}]}'
expect "merge-if-green, behind by docs: a lagging job of a green run counts as green" 0 "$(merge_run 12)"

# Tests 19 and 27: the squash commit of a merge without update has the PR's tree plus main's docs, so the script must
# still find it on main (L-080), and only then. A temporary repo and its bare origin stand in for GitHub: A; the PR
# head H = A + app/y.kt (refs/pull/12/head); D = A + docs/runs/r.md; M = D's tree + app/y.kt, parent D, on main.
nu_git() { git -C "$work/nu" "$@"; }
nu_put() { # <path=content>...: writes files in $work/nu
  local pair
  for pair in "$@"; do
    mkdir -p "$work/nu/$(dirname "${pair%%=*}")"
    printf '%s\n' "${pair#*=}" > "$work/nu/${pair%%=*}"
  done
}
nu_build() { # <case>: plain, z (M also adds app/z.kt), nopull (origin lacks the PR), c (H1 = H0 + docs/x.md),
  # c-main (docs/x.md is in D, H1 = H0 with no change), d (H1 and D both change docs/lessons-2.md), e (M's parent is
  # a separate root commit). Sets nu_h (the PR head) and nu_m (the commit on main).
  local d tree parent
  rm -rf "$work/nu" "$work/nu-origin.git"
  git init -q -b main "$work/nu"
  nu_put app/x.kt=a docs/lessons-2.md=a
  nu_git add -A && nu_git commit -q -m A
  nu_git switch -q -c pr
  nu_put app/y.kt=y
  nu_git add -A && nu_git commit -q -m H
  case "$1" in
    c) nu_put docs/x.md=x && nu_git add -A && nu_git commit -q -m H1 ;;
    c-main) nu_git commit -q --allow-empty -m H1 ;;
    d) nu_put docs/lessons-2.md=h && nu_git add -A && nu_git commit -q -m H1 ;;
  esac
  nu_h="$(nu_git rev-parse HEAD)"
  nu_git switch -q main
  nu_put docs/runs/r.md=r
  case "$1" in
    c-main) nu_put docs/x.md=x ;;
    d) nu_put docs/lessons-2.md=d ;;
  esac
  nu_git add -A && nu_git commit -q -m D
  d="$(nu_git rev-parse HEAD)"
  nu_put app/y.kt=y
  [[ "$1" != z ]] || nu_put app/z.kt=z
  nu_git add -A
  tree="$(nu_git write-tree)"
  nu_git reset -q --hard "$d"
  parent="$d"
  [[ "$1" != e ]] || parent="$(nu_git commit-tree "$(nu_git rev-parse "$d^{tree}")" -m R)"
  nu_m="$(nu_git commit-tree "$tree" -p "$parent" -m "t (#12)")"
  git init -q --bare "$work/nu-origin.git"
  nu_git push -q "$work/nu-origin.git" "$nu_m:refs/heads/main"
  [[ "$1" == nopull ]] || nu_git push -q "$work/nu-origin.git" "$nu_h:refs/pull/12/head"
}
nu_main_list() { # the answer of commits?sha=main: M, titled "t (#12)", with its tree
  printf '[{"sha": "%s", "commit": {"message": "t (#12)", "tree": {"sha": "%s"}}}]' "$nu_m" "$(nu_git rev-parse "$nu_m^{tree}")"
}
nu_git_routes() { # <answer of commits?sha=main>: nu_routes for the PR head nu_h, behind by 1, with a failing merge
  nu_routes
  nu_sha "$nu_h"
  nu_edit '
r["compare/main..." + sys.argv[3]] = {"behind_by": 1}
r["git/commits/" + sys.argv[3]] = {"tree": {"sha": sys.argv[4]}}
r["commits?sha=main&per_page=30"] = json.loads(sys.argv[5])
fx["fail_writes"] = ["PUT pulls/12/merge"]' "$nu_h" "$(nu_git rev-parse "$nu_h^{tree}")" "$1"
}
nu_git_run() { # <args>...: merge-if-green.sh in $work/nu, origin rewritten to $work/nu-origin.git; prints the exit code
  rm -f "$work/mg.json.writes" "$work/mg.json.seen"
  local code=0
  (cd "$work/nu" && PATH="$work/bin:$PATH" FIXTURE="$work/mg.json" MERGE_RECHECK_SECONDS=0 MERGE_POLL_SECONDS=0 \
    MERGE_NOW_EPOCH=1793000000 GIT_CONFIG_COUNT=2 GIT_CONFIG_KEY_1="url.$work/nu-origin.git.insteadOf" \
    GIT_CONFIG_VALUE_1=https://github.com/o/r.git "$root/scripts/merge-if-green.sh" "$@") > "$work/mg.out" 2>&1 || code=$?
  echo "$code"
}
nu_same="cùng tiêu đề 't (#12)' trên nhánh chính nhưng cây khác commit của PR ngoài tài liệu; không merge lại, người xem."
nu_samec() { grep -cF -- "$nu_same" "$work/mg.out" || true; }
nu_build plain
nu_git_routes "$(nu_main_list)"
expect "merge-if-green, behind by docs: a PR already on main is reported, not merged" "0 1 0" \
  "$(nu_git_run 12) $(nu_count '^ĐÃ MERGE TRƯỚC ĐÓ: PR #12') $(nu_wcount .)"
nu_git_routes "{\"__sequence__\": [[], $(nu_main_list)]}"
expect "merge-if-green: a no-update merge that GitHub answered with an error is found on main" "0 1 1" \
  "$(nu_git_run 12) $(nu_count "^ĐÃ MERGE (GitHub báo lỗi): PR #12 (t) tại commit ${nu_m:0:7}") $(nu_wcount '^POST actions/workflows/main-guard.yml/dispatches$')"
nu_build z
nu_git_routes "{\"__sequence__\": [[], $(nu_main_list)]}"
nu27a="$(nu_git_run 12) $(nu_samec) $(nu_wcount '^POST actions/workflows/main-guard.yml/dispatches$') $(nu_count 'Đã yêu cầu chạy main-guard')"
nu_edit 'fx["fail_writes"].append("POST actions/workflows/main-guard.yml/dispatches")'
expect "merge-if-green: a same-titled commit that differs in code after a failed merge is refused, main-guard asked for" \
  "1 1 1 1 1 1 1" "$nu27a $(nu_git_run 12) $(nu_samec) $(nu_count 'Không yêu cầu được main-guard')"
nu_build nopull
nu_git_routes "{\"__sequence__\": [[], $(nu_main_list)]}"
expect "merge-if-green: a same-titled commit is refused when the PR head cannot be fetched" "1 1" "$(nu_git_run 12) $(nu_samec)"
nu_build c
nu_git_routes "$(nu_main_list)"
expect "merge-if-green: docs the PR added after its first head was merged are not taken for main's docs" "1 1 0 0" \
  "$(nu_git_run 12) $(nu_samec) $(nu_count 'ĐÃ MERGE TRƯỚC ĐÓ') $(nu_wcount .)"
nu_build c-main
nu_git_routes "$(nu_main_list)"
expect "merge-if-green: the same docs added on main instead are main's docs: already merged" "0 1" \
  "$(nu_git_run 12) $(nu_count '^ĐÃ MERGE TRƯỚC ĐÓ: PR #12')"
nu_build d
nu_git_routes "$(nu_main_list)"
expect "merge-if-green: a docs file changed by both the PR and main is not taken as merged" "1 1 0" \
  "$(nu_git_run 12) $(nu_samec) $(nu_count 'ĐÃ MERGE TRƯỚC ĐÓ')"
nu_build e
nu_git_routes "$(nu_main_list)"
expect "merge-if-green: a same-titled commit without a merge base with the PR is refused" "1 1" "$(nu_git_run 12) $(nu_samec)"

# wait-run.sh waits for one workflow run in a single command and says how it ended.
wait_run() { # <fixture JSON> <args...>; prints "<exit code> <first word of the line>"
  echo "$1" > "$work/wait.json"; shift
  rm -f "$work/wait.json.seen"
  local out code=0
  out="$(PATH="$work/bin:$PATH" FIXTURE="$work/wait.json" WAIT_RUN_POLL_SECONDS=0 WAIT_RUN_TIMEOUT_SECONDS=2 \
    scripts/wait-run.sh "$@" 2>&1)" || code=$?
  echo "$code ${out%%[: ]*}"
}
no_jobs='{"jobs": []}'
pr_routes() { # <runs answer> [jobs answer]
  printf '{"routes": {"pulls/7": {"head": {"sha": "abc1234"}}, "actions/workflows/pr-check.yml/runs?head_sha=abc1234&per_page=10": %s, "actions/runs/91/jobs?per_page=50": %s}}' \
    "$1" "${2:-$no_jobs}"
}
pending='{"workflow_runs": [{"id": 91, "status": "in_progress", "conclusion": null, "html_url": "u"}]}'
green='{"workflow_runs": [{"id": 91, "status": "completed", "conclusion": "success", "html_url": "u"}]}'
red='{"workflow_runs": [{"id": 91, "status": "completed", "conclusion": "failure", "html_url": "u"}]}'
sequence='{"__sequence__": [{"workflow_runs": []}, '"$pending, $green"']}'
expect "wait-run: waits until the PR check turns green" "0 XANH" "$(wait_run "$(pr_routes "$sequence")" --pr 7)"
expect "wait-run: a red PR check names the failed job" "1 ĐỎ" \
  "$(wait_run "$(pr_routes "$red" '{"jobs": [{"name": "build-and-test", "conclusion": "failure"}]}')" --pr 7)"
expect "wait-run: the red line names build-and-test" 1 \
  "$(pr_routes "$red" '{"jobs": [{"name": "build-and-test", "conclusion": "failure"}]}' > "$work/wait.json"; \
     PATH="$work/bin:$PATH" FIXTURE="$work/wait.json" WAIT_RUN_POLL_SECONDS=0 scripts/wait-run.sh --pr 7 2>&1 | grep -c 'job: build-and-test')"
expect "wait-run: the red line names ui-test" 1 \
  "$(pr_routes "$red" '{"jobs": [{"name": "ui-test", "conclusion": "failure"}]}' > "$work/wait.json"; \
     PATH="$work/bin:$PATH" FIXTURE="$work/wait.json" WAIT_RUN_POLL_SECONDS=0 scripts/wait-run.sh --pr 7 2>&1 | grep -c 'job: ui-test')"
expect "wait-run: a run still going when time runs out asks to call again" "2 CHƯA" \
  "$(wait_run "$(pr_routes "$pending")" --pr 7 --timeout 1)"
# A run GitHub never started (no runner: spending limit, billing) is not a red check: exit 4 (template L-064).
# Fixture from main-guard 39916343646, attempt 1: the job has no steps, its check run carries the reason.
unstarted='{"jobs": [{"id": 120, "name": "guard", "conclusion": "failure", "steps": []}]}'
billing='[{"annotation_level": "failure", "message": "The job was not started because recent account payments have failed or your spending limit needs to be increased."}, {"annotation_level": "notice", "message": "The ubuntu-latest label will migrate to Ubuntu 26"}]'
infra_routes() { # <jobs answer> <annotations answer>
  printf '{"routes": {"pulls/7": {"head": {"sha": "abc1234"}}, "actions/workflows/pr-check.yml/runs?head_sha=abc1234&per_page=10": %s, "actions/runs/91/jobs?per_page=50": %s, "check-runs/120/annotations": %s}}' \
    "$red" "$1" "$2"
}
infra_line() { echo "$1" > "$work/wait.json"; PATH="$work/bin:$PATH" FIXTURE="$work/wait.json" WAIT_RUN_POLL_SECONDS=0 scripts/wait-run.sh --pr 7 2>&1 || true; }
expect "wait-run: a job GitHub never started is an infrastructure failure" "4 HẠ" \
  "$(wait_run "$(infra_routes "$unstarted" "$billing")" --pr 7)"
expect "wait-run: the infrastructure line quotes GitHub's annotation" "1 0" \
  "$(out="$(infra_line "$(infra_routes "$unstarted" "$billing")")"; echo "$(grep -c 'spending limit' <<< "$out") $(grep -c 'Ubuntu 26' <<< "$out" || true)")"
expect "wait-run: an unstarted job without annotations is still infrastructure" "4 HẠ" \
  "$(wait_run "$(infra_routes "$unstarted" '[]')" --pr 7)"
expect "wait-run: says when GitHub gave no reason" 1 \
  "$(infra_line "$(infra_routes "$unstarted" '[]')" | grep -c 'GitHub không ghi lý do')"
expect "wait-run: a failed job that ran steps stays red" "1 ĐỎ" \
  "$(wait_run "$(infra_routes '{"jobs": [{"id": 120, "name": "guard", "conclusion": "failure", "steps": [{"name": "Set up job"}]}]}' "$billing")" --pr 7)"
expect "wait-run: one unstarted and one failed job that ran stays red" "1 ĐỎ" \
  "$(wait_run "$(infra_routes '{"jobs": [{"id": 120, "name": "guard", "conclusion": "failure", "steps": []}, {"id": 121, "name": "ui-test", "conclusion": "failure", "steps": [{"name": "Set up job"}]}]}' "$billing")" --pr 7)"
expect "wait-run: an unstarted main-guard run is an infrastructure failure" "4 HẠ" \
  "$(wait_run '{"routes": {"actions/workflows/main-guard.yml/runs?branch=main&per_page=20": {"workflow_runs": [{"id": 91, "created_at": "2026-10-23T07:13:10Z", "status": "completed", "conclusion": "failure", "html_url": "u"}]}, "actions/runs/91/jobs?per_page=50": '"$unstarted"', "check-runs/120/annotations": '"$billing"'}}' \
    --workflow main-guard.yml --since 2026-10-23T07:10:00Z)"

# GitHub runs no pull_request workflow for a PR in conflict with main: say so at once, exit 5 (template L-064).
conflict_routes() { # <mergeable_state> <runs answer>
  printf '{"routes": {"pulls/7": {"head": {"sha": "abc1234"}, "mergeable_state": "%s"}, "actions/workflows/pr-check.yml/runs?head_sha=abc1234&per_page=10": %s, "actions/runs/91/jobs?per_page=50": {"jobs": []}}}' "$1" "$2"
}
conflict_run() { # <fixture>; long timeout, so only the conflict check can end the wait early
  echo "$1" > "$work/wait.json"; rm -f "$work/wait.json.seen"
  local out code=0
  out="$(PATH="$work/bin:$PATH" FIXTURE="$work/wait.json" WAIT_RUN_POLL_SECONDS=0 WAIT_RUN_TIMEOUT_SECONDS="${2:-30}" \
    scripts/wait-run.sh --pr 7 2>&1)" || code=$?
  echo "$code ${out%%[: ]*}"
}
expect "wait-run: a conflicting PR without runs stops at once" "5 XUNG" \
  "$(conflict_run "$(conflict_routes dirty '{"workflow_runs": []}')")"
expect "wait-run: an unknown mergeable state keeps waiting" "2 CHƯA" \
  "$(conflict_run "$(conflict_routes unknown '{"workflow_runs": []}')" 1)"
expect "wait-run: a run that exists is waited for even if the PR became dirty" "0 XANH" \
  "$(conflict_run "$(conflict_routes dirty "$green")")"

# wait-run.sh --pr follows the newest commit of the PR and --sha waits for the commit just pushed (template L-087, from camera-pose T-044).
# Fixture from camera-pose PR #100: e35ae86 was pushed first, eba5f98 after it; GitHub cancelled the run of e35ae86 once eba5f98
# had a run. pulls/7 is a sequence of heads; the read at the start of the script takes the first element.
head_at() { printf '{"head": {"sha": "%s"}, "mergeable_state": "%s"}' "$1" "${2:-clean}"; }
cancelled='{"workflow_runs": [{"id": 90, "status": "completed", "conclusion": "cancelled", "html_url": "u"}]}'
no_runs='{"workflow_runs": []}'
ui_red_jobs='{"jobs": [{"id": 130, "name": "ui-test", "conclusion": "failure", "steps": [{"name": "Set up job"}]}]}'
head_routes() { # <pulls/7 answers, comma separated> <runs of e35ae86> <runs of eba5f98> [<runs of c0ffee1> [<jobs answer>]]
  printf '{"routes": {"pulls/7": {"__sequence__": [%s]}, "actions/workflows/pr-check.yml/runs?head_sha=e35ae86&per_page=10": %s, "actions/workflows/pr-check.yml/runs?head_sha=eba5f98&per_page=10": %s, "actions/workflows/pr-check.yml/runs?head_sha=c0ffee1&per_page=10": %s, "actions/runs/91/jobs?per_page=50": %s}}' \
    "$1" "$2" "$3" "${4:-$no_runs}" "${5:-$no_jobs}"
}
wait_line() { # <fixture> <timeout seconds> <args...>; prints "<exit code> <result line>"
  echo "$1" > "$work/wait.json"; local seconds="$2"; shift 2
  rm -f "$work/wait.json.seen"
  local out code=0
  out="$(PATH="$work/bin:$PATH" FIXTURE="$work/wait.json" WAIT_RUN_POLL_SECONDS=0 WAIT_RUN_TIMEOUT_SECONDS="$seconds" \
    scripts/wait-run.sh "$@" 2>&1)" || code=$?
  echo "$code $out"
}
wait_code() { # same arguments as wait_line; prints "<exit code> <first word of the line>"
  local line; line="$(wait_line "$@")"
  local rest="${line#* }"
  echo "${line%% *} ${rest%%[: ]*}"
}
old_new="$(head_at e35ae86), $(head_at e35ae86), $(head_at eba5f98)"
new_run_sequence='{"__sequence__": [{"workflow_runs": [{"id": 91, "status": "in_progress", "conclusion": null, "html_url": "u"}]}, '"$green"']}'
old_cancelled_fixture="$(head_routes "$old_new" "$cancelled" "$new_run_sequence")"
expect "wait-run: follows the PR head when the old commit's run is cancelled" "0 XANH" \
  "$(WAIT_RUN_CANCEL_RECHECKS=3 wait_code "$old_cancelled_fixture" 10 --pr 7)"
line="$(WAIT_RUN_CANCEL_RECHECKS=3 wait_line "$old_cancelled_fixture" 10 --pr 7)"
expect "wait-run: the green line names the new head" "1 0" \
  "$(echo "$(grep -c 'eba5f98' <<< "$line") $(grep -c 'e35ae86' <<< "$line" || true)")"
expect "wait-run: a new head while the old run is going waits for the new commit" "0 XANH" \
  "$(wait_code "$(head_routes "$old_new" "$pending" "$green")" 10 --pr 7)"
expect "wait-run: a cancelled run of a head that stays is red after the re-reads" "1 ĐỎ" \
  "$(WAIT_RUN_CANCEL_RECHECKS=2 wait_code "$(head_routes "$(head_at e35ae86)" "$cancelled" "$no_runs")" 10 --pr 7)"
line="$(WAIT_RUN_CANCEL_RECHECKS=99 wait_line "$(head_routes "$(head_at e35ae86)" "$cancelled" "$no_runs")" 2 --pr 7)"
expect "wait-run: re-reading after a cancel stops at the deadline" "1 1" \
  "$(echo "$(grep -c '^2 CHƯA' <<< "$line") $(grep -c 'bị hủy' <<< "$line" || true)")"
# The second read is empty and the empty answer stays: a script that took it would ask GitHub for head_sha= forever.
expect "wait-run: an unreadable head keeps the last one" "0 XANH" \
  "$(wait_code "$(head_routes "$(head_at e35ae86), $(head_at '')" "$green" "$no_runs")" 10 --pr 7)"
sha_fixture="$(head_routes "$old_new" "$green" '{"workflow_runs": [{"id": 91, "status": "completed", "conclusion": "failure", "html_url": "u"}]}' "$no_runs" "$ui_red_jobs")"
expect "wait-run: --sha ignores a finished run of the previous head" "1 ĐỎ" \
  "$(wait_code "$sha_fixture" 10 --pr 7 --sha eba5f98)"
expect "wait-run: without --sha the same fixture is green" "0 XANH" "$(wait_code "$sha_fixture" 10 --pr 7)"
expect "wait-run: --sha does not report a conflict of the previous head" "0 XANH" \
  "$(wait_code "$(head_routes "$(head_at e35ae86 dirty), $(head_at e35ae86 dirty), $(head_at eba5f98 clean)" "$no_runs" "$green")" 10 --pr 7 --sha eba5f98)"
line="$(WAIT_RUN_CANCEL_RECHECKS=3 wait_line "$(head_routes "$(head_at eba5f98), $(head_at eba5f98), $(head_at c0ffee1)" "$no_runs" "$cancelled" "$green")" 10 --pr 7 --sha eba5f98)"
expect "wait-run: a head that moves past --sha is followed" "1 1" \
  "$(echo "$(grep -c '^0 XANH' <<< "$line") $(grep -c 'c0ffee1' <<< "$line" || true)")"
line="$(wait_line "$(head_routes "$(head_at e35ae86)" "$green" "$green")" 1 --pr 7 --sha eba5f98)"
expect "wait-run: --sha the PR never reaches asks to call again" "1 1" \
  "$(echo "$(grep -c '^2 CHƯA' <<< "$line") $(grep -c 'chưa nhận commit eba5f98' <<< "$line" || true)")"
# A --sha mistyped after its first 7 characters (RUN-20261111-01, PR #462: bcdc328897 for bcdc328e9d) stops at once
# instead of waiting the whole timeout, and the line shows both commits in full.
line="$(wait_line "$(head_routes "$(head_at eba5f98aa1)" "$green" "$green")" 10 --pr 7 --sha eba5f98bb2)"
expect "wait-run: a --sha sharing only the short form of the head is refused at once" "1 1 1" \
  "$(echo "$(grep -c '^3 LỖI' <<< "$line") $(grep -c 'eba5f98bb2' <<< "$line" || true) $(grep -c 'eba5f98aa1' <<< "$line" || true)")"
unknown_sha="$(head_routes "$(head_at e35ae86)" "$green" "$green" | jq -c '. + {errors: {"commits/1234abc": "HTTP 422: No commit found for SHA: 1234abc"}}')"
line="$(WAIT_RUN_UNKNOWN_SHA_SECONDS=0 wait_line "$unknown_sha" 10 --pr 7 --sha 1234abc)"
expect "wait-run: a --sha GitHub still does not know after the grace time is refused" "1 1" \
  "$(echo "$(grep -c '^3 LỖI' <<< "$line") $(grep -c 'không có trên GitHub' <<< "$line" || true)")"
line="$(wait_line "$(head_routes "$(head_at e35ae86)" "$green" "$green" | jq -c '. + {errors: {"commits/eba5f98": "HTTP 500: Server Error"}}')" 1 --pr 7 --sha eba5f98)"
expect "wait-run: a failed commit lookup is not proof: the wait goes on" 1 "$(grep -c '^2 CHƯA' <<< "$line" || true)"
# Right after a push GitHub may answer "No commit found" for a few seconds while the PR still shows the old head:
# not proof before WAIT_RUN_UNKNOWN_SHA_SECONDS, so the wait reaches the new head and its green run.
just_pushed="$(head_routes "$(head_at e35ae86), $(head_at e35ae86), $(head_at c0ffee1)" "$green" "$no_runs" "$green" | jq -c '. + {errors: {"commits/c0ffee1": "HTTP 422: No commit found for SHA: c0ffee1"}}')"
expect "wait-run: a commit GitHub does not know yet right after the push is waited for" "0 XANH" \
  "$(wait_code "$just_pushed" 10 --pr 7 --sha c0ffee1)"
line="$(wait_line "$(head_routes "$(head_at e35ae86)" "$green" "$green")" 10 --pr 7 --sha main)"
expect "wait-run: rejects a --sha that is not a commit" "1 1" \
  "$(echo "$(grep -c '^3 LỖI' <<< "$line") $(grep -c 'mã commit' <<< "$line" || true)")"
line="$(wait_line '{"routes": {}}' 10 --workflow staging.yml --since 2026-10-18T02:20:00Z --sha eba5f98)"
expect "wait-run: refuses --sha with --workflow" "1 1" \
  "$(echo "$(grep -c '^3 LỖI' <<< "$line") $(grep -c 'cùng --pr' <<< "$line" || true)")"

# pr-check does not run for a draft PR (ADR-0041): wait-run.sh says so at once with exit 6, on every round. The run
# GitHub left while the PR was a draft (skipped) is not a result: wait for the run of ready_for_review.
draft_pr() { printf '{"head": {"sha": "abc1234"}, "draft": %s}' "$1"; } # <true or false>
draft_routes() { # <pulls/7 answer> <runs answer>
  printf '{"routes": {"pulls/7": %s, "actions/workflows/pr-check.yml/runs?head_sha=abc1234&per_page=10": %s, "actions/runs/91/jobs?per_page=50": {"jobs": []}}}' "$1" "$2"
}
expect "wait-run: a draft PR stops at once with code 6" "6 NHÁP 6 NHÁP 2 CHƯA" \
  "$(wait_code "$(draft_routes "$(draft_pr true)" "$no_runs")" 0 --pr 7) $(wait_code "$(draft_routes "{\"__sequence__\": [$(draft_pr false), $(draft_pr false), $(draft_pr true)]}" "$pending")" 5 --pr 7) $(wait_code "$(draft_routes "$(draft_pr false)" "$no_runs")" 0 --pr 7)"
skipped_run='{"id": 90, "status": "completed", "conclusion": "skipped", "html_url": "u"}'
ready_runs='{"__sequence__": [{"workflow_runs": ['"$skipped_run"']}, {"workflow_runs": [{"id": 91, "status": "completed", "conclusion": "success", "html_url": "u"}, '"$skipped_run"']}]}'
line="$(wait_line "$(draft_routes "$(draft_pr false)" "$ready_runs")" 10 --pr 7)"
expect "wait-run: a skipped run left from the draft is not a result" "1 1 2 CHƯA" \
  "$(grep -c '^0 XANH' <<< "$line" || true) $(grep -c 'lần chạy 91' <<< "$line" || true) $(wait_code "$(draft_routes "$(draft_pr false)" "{\"workflow_runs\": [$skipped_run]}")" 1 --pr 7)"

dispatch_routes='{"routes": {"actions/workflows/staging.yml/runs?branch=main&per_page=20": {"workflow_runs": [
  {"id": 93, "created_at": "2026-10-18T02:25:00Z", "status": "completed", "conclusion": "failure", "html_url": "u"},
  {"id": 92, "created_at": "2026-10-18T02:21:00Z", "status": "completed", "conclusion": "success", "html_url": "u"},
  {"id": 80, "created_at": "2026-10-17T16:57:00Z", "status": "completed", "conclusion": "failure", "html_url": "u"}]}}}'
expect "wait-run: picks the first run after --since" "0 XANH" \
  "$(wait_run "$dispatch_routes" --workflow staging.yml --since 2026-10-18T02:20Z)"
expect "wait-run: no run after --since is not finished" "2 CHƯA" \
  "$(wait_run "$dispatch_routes" --workflow staging.yml --since 2026-10-18T03:00:00Z)"
expect "wait-run: rejects a time without a zone" "3 LỖI" \
  "$(wait_run "$dispatch_routes" --workflow staging.yml --since '2026-10-18 02:20')"

# A job waiting for a self-hosted runner while nothing of its run is going: exit 7 once it has waited
# WAIT_RUN_RUNNER_QUEUE_MINUTES (default 75) since it could start (ADR-0042, section 5). WAIT_RUN_NOW fixes the clock
# at T = 2026-11-08T12:00:00Z; rw_at gives the time some minutes before T.
rw_now="$(python3 -c 'import datetime as d; print(int(d.datetime(2026, 11, 8, 12, tzinfo=d.timezone.utc).timestamp()))')"
rw_at() { python3 -c "import datetime as d; print((d.datetime(2026,11,8,12,tzinfo=d.timezone.utc)-d.timedelta(minutes=$1)).strftime('%Y-%m-%dT%H:%M:%SZ'))"; }
rw_job() { # <name> <status> <labels JSON> <created minutes before T> [<runner name JSON> [<completed minutes before T>]]
  local completed=""
  [[ -z "${6:-}" ]] || completed=", \"completed_at\": \"$(rw_at "$6")\""
  printf '{"id": 120, "name": "%s", "status": "%s", "runner_name": %s, "labels": %s, "created_at": "%s"%s}' \
    "$1" "$2" "${5:-null}" "$3" "$(rw_at "$4")" "$completed"
}
rw_queued="$(rw_job build-and-test queued '["vgl-android"]' 90)"
rw_jobs() { printf '{"jobs": [%s]}' "$1"; }
rw_code() { WAIT_RUN_NOW="$rw_now" wait_code "$@"; }
rw_main_line="$(WAIT_RUN_NOW="$rw_now" wait_line "$(pr_routes "$pending" "$(rw_jobs "$rw_queued")")" 2 --pr 7)"
expect "wait-run: a job waiting for a self-hosted runner with nothing running is CHỜ RUNNER" "7 CHỜ" \
  "$(rw_code "$(pr_routes "$pending" "$(rw_jobs "$rw_queued")")" 2 --pr 7)"
expect "wait-run: the CHỜ RUNNER line names the wait, the label and the run" "1 1 1" \
  "$(grep -c 'chờ runner tự host' <<<"$rw_main_line" || true) $(grep -c 'vgl-android' <<<"$rw_main_line" || true) $(grep -c 'lần chạy 91' <<<"$rw_main_line" || true)"
rw_needs_late="$(rw_job guard completed '["vgl-android"]' 95 '"vgl-android-1"' 10), $(rw_job ui-test queued '["vgl-android"]' 90)"
rw_needs_early="$(rw_job guard completed '["vgl-android"]' 95 '"vgl-android-1"' 80), $(rw_job ui-test queued '["vgl-android"]' 90)"
expect "wait-run: a job waits from the time the last job of its run finished" "2 CHƯA 7 CHỜ" \
  "$(rw_code "$(pr_routes "$pending" "$(rw_jobs "$rw_needs_late")")" 2 --pr 7) $(rw_code "$(pr_routes "$pending" "$(rw_jobs "$rw_needs_early")")" 2 --pr 7)"
expect "wait-run: a job queued for 30 minutes is still waited for" "2 CHƯA" \
  "$(rw_code "$(pr_routes "$pending" "$(rw_jobs "$(rw_job build-and-test queued '["vgl-android"]' 30)")")" 2 --pr 7)"
expect "wait-run: a job waiting for a GitHub runner is not CHỜ RUNNER" "2 CHƯA" \
  "$(rw_code "$(pr_routes "$pending" "$(rw_jobs "$(rw_job build-and-test queued '["ubuntu-latest"]' 90)")")" 2 --pr 7)"
expect "wait-run: a waiting job while another job runs is not CHỜ RUNNER" "2 CHƯA" \
  "$(rw_code "$(pr_routes "$pending" "$(rw_jobs "$rw_queued, $(rw_job guard in_progress '["vgl-android"]' 95 '"vgl-android-1"')")")" 2 --pr 7)"
expect "wait-run: a queued job that already has a runner is not CHỜ RUNNER" "2 CHƯA" \
  "$(rw_code "$(pr_routes "$pending" "$(rw_jobs "$(rw_job build-and-test queued '["vgl-android"]' 90 '"vgl-android-1"')")")" 2 --pr 7)"
expect "wait-run: WAIT_RUN_RUNNER_QUEUE_MINUTES sets how long a job may wait" "2 CHƯA" \
  "$(WAIT_RUN_RUNNER_QUEUE_MINUTES=120 rw_code "$(pr_routes "$pending" "$(rw_jobs "$rw_queued")")" 2 --pr 7)"
rw_jobs_error="$(pr_routes "$pending" "$(rw_jobs "$rw_queued")" | jq -c '.errors = {"actions/runs/91/jobs?per_page=50": "HTTP 500: Server Error"}')"
expect "wait-run: an unreadable or odd job list keeps waiting" "2 CHƯA 2 CHƯA" \
  "$(rw_code "$rw_jobs_error" 2 --pr 7) $(rw_code "$(pr_routes "$pending" '{}')" 2 --pr 7)"
expect "wait-run: WAIT_RUN_RUNNER_QUEUE_MINUTES must be a number" "3 LỖI" \
  "$(WAIT_RUN_RUNNER_QUEUE_MINUTES=abc rw_code "$(pr_routes "$pending" "$(rw_jobs "$rw_queued")")" 2 --pr 7)"
rw_workflow="{\"routes\": {\"actions/workflows/main-guard.yml/runs?branch=main&per_page=20\": {\"workflow_runs\": [{\"id\": 91, \"created_at\": \"$(rw_at 95)\", \"status\": \"in_progress\", \"conclusion\": null, \"html_url\": \"u\"}]}, \"actions/runs/91/jobs?per_page=50\": $(rw_jobs "$(rw_job ui-test queued '["vgl-android"]' 90)")}}"
expect "wait-run: --workflow also says CHỜ RUNNER" "7 CHỜ" \
  "$(rw_code "$rw_workflow" 2 --workflow main-guard.yml --since "$(rw_at 100)")"
# The draft and conflict checks come first (ADR-0041, L-064).
rw_draft="$(draft_routes "$(draft_pr true)" "$pending" | jq -c --argjson j "$(rw_jobs "$rw_queued")" '.routes["actions/runs/91/jobs?per_page=50"] = $j')"
rw_conflict="$(conflict_routes dirty "$no_runs" | jq -c --argjson j "$(rw_jobs "$rw_queued")" '.routes["actions/runs/91/jobs?per_page=50"] = $j')"
expect "wait-run: a draft PR and a conflicting PR without a run still stop first" "6 NHÁP 5 XUNG" \
  "$(rw_code "$rw_draft" 2 --pr 7) $(rw_code "$rw_conflict" 2 --pr 7)"

# resume-check.py finds a session that stopped in the middle (quota used up) so resume-run.yml can start it again.
resume_check() { # <run status> <minutes since the last commit> [extra routes JSON]; prints the run file or nothing
  # RESUME_QUEUED and RESUME_IN_PROGRESS answer the Actions runs not finished (none by default), RESUME_ROUTES adds
  # routes (starting with a comma), RESUME_ERRORS the failed reads, RESUME_ERR keeps stderr. Returns the exit code.
  local last empty='{"total_count": 0, "workflow_runs": []}' errors="${RESUME_ERRORS:-}"
  [[ -n "$errors" ]] || errors='{}'
  last="$(python3 -c "import datetime as d; print((d.datetime(2026,10,18,3,0,tzinfo=d.timezone.utc)-d.timedelta(minutes=$2)).strftime('%Y-%m-%dT%H:%M:%SZ'))")"
  cat > "$work/resume.json" << JSON
{"routes": {
  "branches?per_page=100&page=1": [{"name": "main"}, {"name": "claude/RUN-20261018-01-report"}, {"name": "claude/T-007-x"}, {"name": "claude/T-099-other"}],
  "contents/docs/runs/RUN-20261018-01.md?ref=claude/RUN-20261018-01-report": "---\nid: RUN-20261018-01\nstatus: $1\nstarted_at: 2026-10-17 22:00 UTC\n---\n\n## 1. Danh sách chạy\n\n| 1 | T-007 | demo |\n",
  "commits/claude/RUN-20261018-01-report": {"commit": {"committer": {"date": "2026-10-17T22:05:00Z"}}},
  "commits/claude/T-007-x": {"commit": {"committer": {"date": "$last"}}},
  "commits/claude/T-099-other": {"commit": {"committer": {"date": "2026-10-18T02:59:00Z"}}},
  "issues?state=open&labels=main-broken&per_page=10": ${3:-[]},
  "issues?state=open&labels=env-broken&per_page=10": [],
  "actions/runs?status=queued&per_page=20": ${RESUME_QUEUED:-$empty},
  "actions/runs?status=in_progress&per_page=20": ${RESUME_IN_PROGRESS:-$empty}${RESUME_ROUTES:-}
}, "errors": $errors}
JSON
  PATH="$work/bin:$PATH" FIXTURE="$work/resume.json" GITHUB_REPOSITORY=o/r RESUME_NOW=2026-10-18T03:00:00Z \
    python3 -I scripts/resume-check.py 2> "${RESUME_ERR:-/dev/null}"
}
expect "resume-check: a running session silent for 2 hours is continued" docs/runs/RUN-20261018-01.md "$(resume_check running 120)"
expect "resume-check: a session with a recent task commit is left alone" "" "$(resume_check running 30)"
expect "resume-check: a finished session is left alone" "" "$(resume_check finished 120)"
expect "resume-check: not while main is red" "" "$(resume_check running 120 '[{"number": 5}]')"
expect "resume-check: not a session started more than a day ago" "" \
  "$(RESUME_MAX_HOURS=3 resume_check running 120)"
python3 -I - "$work/resume.json" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1]))
first = fx["routes"].pop("branches?per_page=100&page=1")
fx["routes"]["branches?per_page=100&page=1"] = [{"name": f"claude/old-{n}"} for n in range(100)]
fx["routes"]["branches?per_page=100&page=2"] = first
json.dump(fx, open(sys.argv[1], "w"))
PY
expect "resume-check: finds a report branch on page 2 of the branches" docs/runs/RUN-20261018-01.md \
  "$(PATH="$work/bin:$PATH" FIXTURE="$work/resume.json" GITHUB_REPOSITORY=o/r RESUME_NOW=2026-10-18T03:00:00Z python3 -I scripts/resume-check.py 2> /dev/null)"
# A job of the repository waits for a self-hosted runner: the session may still be waiting for it, so it is not
# continued, however long the job has waited (ADR-0042). A read that fails is exit 1, nothing continued.
rs_job() { # <status> <labels JSON> <created at>
  printf '{"id": 120, "name": "build-and-test", "status": "%s", "runner_name": null, "labels": %s, "created_at": "%s"}' "$1" "$2" "$3"
}
rs_run91='{"total_count": 1, "workflow_runs": [{"id": 91}]}'
rs_resume() { # <where run 91 is: queued or in_progress> <job JSON>; "<exit code>|<stdout>|<stderr lines about the wait>"
  local code=0 out
  if [[ "$1" == queued ]]; then
    out="$(RESUME_QUEUED="$rs_run91" RESUME_ROUTES=", \"actions/runs/91/jobs?per_page=50\": {\"jobs\": [$2]}" \
      RESUME_ERR="$work/resume.err" resume_check running 120)" || code=$?
  else
    out="$(RESUME_IN_PROGRESS="$rs_run91" RESUME_ROUTES=", \"actions/runs/91/jobs?per_page=50\": {\"jobs\": [$2]}" \
      RESUME_ERR="$work/resume.err" resume_check running 120)" || code=$?
  fi
  echo "$code|$out|$(grep -c 'đang chờ runner tự host' "$work/resume.err" || true)"
}
expect "resume-check: no resume while a job waits for a self-hosted runner" "0||1" \
  "$(rs_resume queued "$(rs_job queued '["vgl-android"]' 2026-10-18T01:30:00Z)")"
expect "resume-check: a job queued a minute ago on a self-hosted runner also stops the resume" "0||1" \
  "$(rs_resume queued "$(rs_job queued '["vgl-android"]' 2026-10-18T02:59:00Z)")"
expect "resume-check: a waiting job of a run in progress also stops the resume" "0||1" \
  "$(rs_resume in_progress "$(rs_job queued '["vgl-android"]' 2026-10-18T01:30:00Z)")"
rs_file="0|docs/runs/RUN-20261018-01.md|0"
expect "resume-check: a job waiting for a GitHub runner, without labels or running does not stop the resume" \
  "$rs_file $rs_file $rs_file" \
  "$(rs_resume queued "$(rs_job queued '["ubuntu-latest"]' 2026-10-18T01:30:00Z)") $(rs_resume queued "$(rs_job queued '[]' 2026-10-18T01:30:00Z)") $(rs_resume queued "$(rs_job in_progress '["vgl-android"]' 2026-10-18T01:30:00Z)")"
rs_code=0
rs_out="$(RESUME_ERR="$work/resume.err" resume_check running 120)" || rs_code=$?
expect "resume-check: no run waiting on GitHub continues the session" "$rs_file" "$rs_code|$rs_out|$(grep -c 'đang chờ runner tự host' "$work/resume.err" || true)"
rs_code=0
rs_out="$(RESUME_ERRORS='{"actions/runs?status=queued&per_page=20": "HTTP 500"}' resume_check running 120)" || rs_code=$?
expect "resume-check: unreadable Actions runs continue nothing, exit 1" "1|" "$rs_code|$rs_out"
rs_code=0
rs_out="$(RESUME_QUEUED=null resume_check running 120)" || rs_code=$?
expect "resume-check: an odd answer for the Actions runs continues nothing, exit 1" "1|" "$rs_code|$rs_out"

# run-watchdog.py opens an issue when a running session has no commit for 60 minutes, so a person can
# continue a `claude --cloud` session that resume-run.yml does not continue (L-044, L-109).
wd_time() { python3 -c "import datetime as d; print((d.datetime(2026,10,27,12,0,tzinfo=d.timezone.utc)-d.timedelta(minutes=$1)).strftime('%Y-%m-%dT%H:%M:%SZ'))"; }
watchdog() { # <run status> <minutes since the last commit> [workflow runs: "min:branch:event:status:name,..." or -] [open issues JSON] [started_at] [errors JSON]
  # Each run of the third argument gets the id 1000 + its position and no job. WD_QUEUED and WD_IN_PROGRESS add runs
  # (JSON objects with an id) to the queued and in-progress lists; WD_ROUTES adds routes (starting with a comma).
  local last in_progress="${WD_IN_PROGRESS:-}" queued="${WD_QUEUED:-}" code=0 spec min branch event status name entry
  local errors="${6:-}" id=1000 job_routes=""
  [ -n "$errors" ] || errors="{}"
  last="$(wd_time "$2")"
  if [ "${3:--}" != "-" ]; then
    IFS=',' read -r -a specs <<< "$3"
    for spec in "${specs[@]}"; do
      IFS=':' read -r min branch event status name <<< "$spec"
      entry="{\"id\": $id, \"name\": \"$name\", \"head_branch\": \"$branch\", \"event\": \"$event\", \"created_at\": \"$(wd_time "$min")\"}"
      job_routes="$job_routes, \"actions/runs/$id/jobs?per_page=50\": {\"jobs\": []}"
      id=$((id + 1))
      if [ "$status" = queued ]; then queued="${queued:+$queued, }$entry"; else in_progress="${in_progress:+$in_progress, }$entry"; fi
    done
  fi
  rm -f "$work/wd.json.writes"
  cat > "$work/wd.json" << JSON
{"routes": {
  "branches?per_page=100&page=1": [{"name": "main"}, {"name": "claude/RUN-20261027-05-report"}, {"name": "claude/T-062-x"}, {"name": "claude/T-099-other"}],
  "contents/docs/runs/RUN-20261027-05.md?ref=claude/RUN-20261027-05-report": "---\nid: RUN-20261027-05\nstatus: $1\nstarted_at: ${5:-2026-10-27T09:00:00Z}\n---\n\n## 1. Danh sách chạy\n\n| 1 | T-062 | demo |\n",
  "commits/claude/RUN-20261027-05-report": {"commit": {"committer": {"date": "2026-10-27T09:05:00Z"}}},
  "commits/claude/T-062-x": {"commit": {"committer": {"date": "$last"}}},
  "commits/claude/T-099-other": {"commit": {"committer": {"date": "2026-10-27T11:59:00Z"}}},
  "issues?state=open&labels=session-stalled&per_page=100": ${4:-[]},
  "actions/runs?status=in_progress&per_page=100": {"workflow_runs": [$in_progress]},
  "actions/runs?status=queued&per_page=100": {"workflow_runs": [$queued]},
  "labels/session-stalled": {"name": "session-stalled"}$job_routes${WD_ROUTES:-}
}, "errors": $errors}
JSON
  PATH="$work/bin:$PATH" FIXTURE="$work/wd.json" GITHUB_REPOSITORY=o/r WATCHDOG_NOW=2026-10-27T12:00:00Z \
    python3 -I scripts/run-watchdog.py > "$work/wd.out" 2>&1 || code=$?
  echo "$code"
}
wd_writes() { if [ -f "$work/wd.json.writes" ]; then paste -sd ';' - < "$work/wd.json.writes"; else echo none; fi; }
wd_open='[{"number": 41, "title": "session-stalled: RUN-20261027-05 không có commit 50 phút"}]'
expect "run-watchdog: a running session quiet for 70 minutes exits 0" 0 "$(watchdog running 70)"
expect "run-watchdog: ... opens one session-stalled issue" "POST issues" "$(wd_writes)"
expect "run-watchdog: the issue body has the resume line" 1 \
  "$(grep -c 'Đây là phiên tiếp tục: nhánh báo cáo đã có, danh sách chạy đang running.' "$work/wd.out")"
expect "run-watchdog: the issue body has the whole prompt of staging-and-runs 2.7" 1 \
  "$(grep -c '^Danh sách chạy: docs/runs/RUN-20261027-05.md. Danh sách không có trạng thái approved' "$work/wd.out")"
expect "run-watchdog: the prompt names this run's report branch" 1 "$(grep -c 'git worktree add ../report claude/RUN-20261027-05-report' "$work/wd.out")"
expect "run-watchdog: the body warns that a routine session is resumed by resume-run" 1 "$(grep -c 'không mở thêm phiên tay' "$work/wd.out")"
expect "run-watchdog: the quiet time counts the latest commit of any branch of the run" 1 \
  "$(grep -c 'không có commit nào trong 70 phút' "$work/wd.out")"
watchdog running 59 > /dev/null
expect "run-watchdog: 59 minutes is not quiet yet" none "$(wd_writes)"
watchdog running 60 > /dev/null
expect "run-watchdog: 60 minutes is quiet" "POST issues" "$(wd_writes)"
watchdog running 70 "20:claude/T-062-x:pull_request:in_progress:pr-check" > /dev/null
expect "run-watchdog: CI on a branch of the session means it may be waiting" none "$(wd_writes)"
expect "run-watchdog: ... and says which workflow" 1 "$(grep -c 'đang có workflow chạy (pr-check)' "$work/wd.out")"
watchdog running 70 "20:main:workflow_dispatch:queued:device-session" > /dev/null
expect "run-watchdog: a queued run on main started by hand counts too" none "$(wd_writes)"
watchdog running 70 "20:claude/T-099-other:pull_request:in_progress:pr-check" > /dev/null
expect "run-watchdog: CI of a branch outside the session does not count" "POST issues" "$(wd_writes)"
watchdog running 70 "20:main:schedule:in_progress:env-canary" > /dev/null
expect "run-watchdog: a scheduled run on main does not count" "POST issues" "$(wd_writes)"
watchdog running 70 "10:main:workflow_dispatch:in_progress:run-watchdog" > /dev/null
expect "run-watchdog: its own run does not count" "POST issues" "$(wd_writes)"
watchdog running 70 "74:claude/T-062-x:pull_request:in_progress:pr-check" > /dev/null
expect "run-watchdog: a run created 4 minutes before the last commit counts (5 minutes of slack)" none "$(wd_writes)"
watchdog running 70 "76:claude/T-062-x:pull_request:in_progress:pr-check" > /dev/null
expect "run-watchdog: a run created 6 minutes before the last commit is not a wait" "POST issues" "$(wd_writes)"
watchdog running 95 "20:claude/T-062-x:pull_request:in_progress:pr-check" > /dev/null
expect "run-watchdog: past 90 minutes a running workflow no longer holds the issue back" "POST issues" "$(wd_writes)"
watchdog running 70 - "$wd_open" > /dev/null
expect "run-watchdog: an open issue is updated, not opened again" "PATCH issues/41" "$(wd_writes)"
watchdog running 10 - "$wd_open" > /dev/null
expect "run-watchdog: a new commit closes the issue with a comment" "PATCH issues/41;POST issues/41/comments" "$(wd_writes)"
watchdog finished 300 - "$wd_open" > /dev/null
expect "run-watchdog: a finished run closes its issue" "PATCH issues/41;POST issues/41/comments" "$(wd_writes)"
watchdog finished 300 > /dev/null
expect "run-watchdog: a finished run without an issue changes nothing" none "$(wd_writes)"
watchdog running 70 - '[{"number": 7, "title": "main-broken: abc1234"}, {"number": 8, "title": "session-stalled: RUN-20261027-05 x", "pull_request": {}}]' > /dev/null
expect "run-watchdog: issues that are not its own are never touched" "POST issues" "$(wd_writes)"
watchdog running 70 - "[]" 2026-10-26T06:00:00Z > /dev/null
expect "run-watchdog: a run older than 24 hours is still reported, with a note" 1 "$(grep -c 'hơn 24 giờ trước' "$work/wd.out")"
expect "run-watchdog: a server error on the run file exits 1" 1 \
  "$(watchdog running 10 - "$wd_open" "" '{"contents/docs/runs/RUN-20261027-05.md?ref=claude/RUN-20261027-05-report": "HTTP 500: Server Error"}')"
expect "run-watchdog: ... and changes no issue" none "$(wd_writes)"
expect "run-watchdog: a server error on the workflow runs exits 1" 1 \
  "$(watchdog running 70 - "[]" "" '{"actions/runs?status=in_progress&per_page=100": "HTTP 502: Bad Gateway"}')"
expect "run-watchdog: ... and opens no issue" none "$(wd_writes)"
watchdog running 70 > /dev/null
python3 -I - "$work/wd.json" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1]))
fx["routes"]["branches?per_page=100&page=1"].append({"name": "claude/RUN-20261027-06-report"})
fx["errors"] = {"contents/docs/runs/RUN-20261027-06.md?ref=claude/RUN-20261027-06-report": "HTTP 500: Server Error"}
json.dump(fx, open(sys.argv[1], "w"))
PY
rm -f "$work/wd.json.writes"
expect "run-watchdog: a later run that cannot be read exits 1" 1 \
  "$(PATH="$work/bin:$PATH" FIXTURE="$work/wd.json" GITHUB_REPOSITORY=o/r WATCHDOG_NOW=2026-10-27T12:00:00Z python3 -I scripts/run-watchdog.py > /dev/null 2>&1; echo $?)"
expect "run-watchdog: ... before anything is written for the first run" none "$(wd_writes)"
watchdog running 70 > /dev/null
python3 -I - "$work/wd.json" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1]))
fx["fail_writes"] = ["POST issues"]
json.dump(fx, open(sys.argv[1], "w"))
PY
expect "run-watchdog: a write that fails exits 1" 1 \
  "$(PATH="$work/bin:$PATH" FIXTURE="$work/wd.json" GITHUB_REPOSITORY=o/r WATCHDOG_NOW=2026-10-27T12:00:00Z python3 -I scripts/run-watchdog.py 2> "$work/wd.err" > /dev/null; echo $?)"
expect "run-watchdog: ... and says the write failed" 1 "$(grep -c 'Ghi issue lỗi' "$work/wd.err")"
watchdog running 10 - "$wd_open" > /dev/null
python3 -I - "$work/wd.json" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1]))
fx["fail_writes"] = ["PATCH issues/41"]
json.dump(fx, open(sys.argv[1], "w"))
PY
rm -f "$work/wd.json.writes"
PATH="$work/bin:$PATH" FIXTURE="$work/wd.json" GITHUB_REPOSITORY=o/r WATCHDOG_NOW=2026-10-27T12:00:00Z python3 -I scripts/run-watchdog.py > /dev/null 2>&1 || true
expect "run-watchdog: a close that fails adds no comment" "PATCH issues/41" "$(wd_writes)"
watchdog running 70 > /dev/null
python3 -I - "$work/wd.json" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1]))
fx["routes"].pop("labels/session-stalled")
fx["errors"] = {}
json.dump(fx, open(sys.argv[1], "w"))
PY
rm -f "$work/wd.json.writes"
PATH="$work/bin:$PATH" FIXTURE="$work/wd.json" GITHUB_REPOSITORY=o/r WATCHDOG_NOW=2026-10-27T12:00:00Z \
  python3 -I scripts/run-watchdog.py > /dev/null 2>&1 || true
expect "run-watchdog: a missing label is created before the issue" "POST labels;POST issues" "$(wd_writes)"
python3 -I - "$work/wd.json" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1]))
fx["routes"].pop("branches?per_page=100&page=1")
json.dump(fx, open(sys.argv[1], "w"))
PY
expect "run-watchdog: GitHub that cannot be read exits 1" 1 \
  "$(PATH="$work/bin:$PATH" FIXTURE="$work/wd.json" GITHUB_REPOSITORY=o/r WATCHDOG_NOW=2026-10-27T12:00:00Z python3 -I scripts/run-watchdog.py > /dev/null 2>&1; echo $?)"

# A job waiting for a self-hosted runner past the wait of wait-run.sh code 7 (75 minutes, nothing of its run going) is
# named in the session-stalled issue: a person fixes the runner first (T-095, ADR-0042). Run 120 of pr-check was
# created 200 minutes ago, before the last commit, so busy_workflows does not hold the issue back.
wd_run120() { # [minutes since the run was created, default 200]; the JSON of run 120 on claude/T-062-x
  echo "{\"id\": 120, \"name\": \"pr-check\", \"head_branch\": \"claude/T-062-x\", \"event\": \"pull_request\", \"created_at\": \"$(wd_time "${1:-200}")\"}"
}
wd_job() { # <name> <status> <labels JSON> <minutes since created> [minutes since completed]; the JSON of one job
  local runner=null done=null
  [ "$2" = queued ] || runner='"r1"'
  [ -z "${5:-}" ] || done="\"$(wd_time "$5")\""
  echo "{\"name\": \"$1\", \"status\": \"$2\", \"runner_name\": $runner, \"labels\": $3, \"created_at\": \"$(wd_time "$4")\", \"completed_at\": $done}"
}
wd_jobs120() { echo ", \"actions/runs/120/jobs?per_page=50\": {\"jobs\": [$(paste -sd ',' -)]}"; } # job JSON lines on stdin
wd_waits() { grep -c 'Job chờ runner tự host:' "$work/wd.out" || true; }
wd_r1="$(wd_job build-and-test queued '["vgl-android"]' 100 | wd_jobs120)"
wd_e120='{"actions/runs/120/jobs?per_page=50": "HTTP 500: Server Error"}'
expect "run-watchdog: a job waiting for a self-hosted runner past 75 minutes is named in the issue" "0|POST issues|1|1|1" \
  "$(WD_QUEUED="$(wd_run120)" WD_ROUTES="$wd_r1" watchdog running 70)|$(wd_writes)|$(wd_waits)|$(grep -cF 'job `build-and-test` (lần chạy 120)' "$work/wd.out")|$(grep -c 'quá 75 phút' "$work/wd.out")"
wd_r2="$(wd_job build-and-test queued '["vgl-android"]' 30 | wd_jobs120)"
WD_QUEUED="$(wd_run120)" WD_ROUTES="$wd_r2" watchdog running 70 > /dev/null
expect "run-watchdog: a job queued 30 minutes ago is not named" "POST issues|0" "$(wd_writes)|$(wd_waits)"
WD_QUEUED="$(wd_run120)" WD_ROUTES="$(wd_job build-and-test queued '["ubuntu-latest"]' 100 | wd_jobs120)" \
  watchdog running 70 > /dev/null
expect "run-watchdog: a job waiting for a GitHub runner is not named" "POST issues|0" "$(wd_writes)|$(wd_waits)"
WD_IN_PROGRESS="$(wd_run120)" WD_ROUTES="$({ wd_job build-and-test queued '["vgl-android"]' 100; wd_job guard in_progress '["vgl-android"]' 110; } | wd_jobs120)" \
  watchdog running 70 > /dev/null
expect "run-watchdog: a run with a job in progress is not named" "POST issues|0" "$(wd_writes)|$(wd_waits)"
WD_IN_PROGRESS="$(wd_run120)" WD_ROUTES="$({ wd_job build-and-test queued '["vgl-android"]' 100; wd_job guard completed '["vgl-android"]' 110 20; } | wd_jobs120)" \
  watchdog running 70 > /dev/null
wd_w5="$(wd_waits)"
WD_IN_PROGRESS="$(wd_run120)" WD_ROUTES="$({ wd_job build-and-test queued '["vgl-android"]' 100; wd_job guard completed '["vgl-android"]' 110 80; } | wd_jobs120)" \
  watchdog running 70 > /dev/null
expect "run-watchdog: the wait counts from the latest finished job of the run" "0 1" "$wd_w5 $(wd_waits)"
WATCHDOG_RUNNER_QUEUE_MINUTES=20 WD_QUEUED="$(wd_run120)" WD_ROUTES="$wd_r2" watchdog running 70 > /dev/null
expect "run-watchdog: WATCHDOG_RUNNER_QUEUE_MINUTES sets the wait" 1 "$(wd_waits)"
expect "run-watchdog: a server error on the jobs of a run exits 1 and opens no issue" "1 none" \
  "$(WD_QUEUED="$(wd_run120)" WD_ROUTES="$wd_r1" watchdog running 70 - "[]" "" "$wd_e120") $(wd_writes)"
WD_QUEUED="$(wd_run120)" WD_ROUTES="$wd_r1" watchdog running 70 - "$wd_open" > /dev/null
expect "run-watchdog: an open issue gets the waiting job on update" "PATCH issues/41|1" "$(wd_writes)|$(wd_waits)"
expect "run-watchdog: a session with a recent commit reads no jobs" "0 none" \
  "$(WD_QUEUED="$(wd_run120)" WD_ROUTES="$wd_r1" watchdog running 30 - "[]" "" "$wd_e120") $(wd_writes)"
expect "run-watchdog: a session held back by a running workflow reads no jobs" "0 none" \
  "$(WD_QUEUED="$(wd_run120)" WD_ROUTES="$wd_r1" watchdog running 70 "20:claude/T-062-x:pull_request:in_progress:pr-check" "[]" "" "$wd_e120") $(wd_writes)"
WD_QUEUED="$(wd_run120 94)" WD_ROUTES="$(wd_job build-and-test queued '["vgl-android"]' 94 | wd_jobs120)" \
  watchdog running 95 > /dev/null
expect "run-watchdog: past 90 minutes a waiting job created after the last commit is named" "POST issues|1" \
  "$(wd_writes)|$(wd_waits)"

# schedule-idle.py turns resume-run.yml and run-watchdog.yml off when no session runs and no approved run list waits,
# and status-page.yml, start-run.yml and the session turn them back on (T-051, L-126). Fixed time 2026-10-29 12:00 UTC.
cat > "$work/si-fixture.py" << 'PY'
# Builds the fixture of one schedule-idle run from key=value arguments; a JSON argument is merged into it.
import datetime as d, json, sys
now = d.datetime(2026, 10, 29, 12, 0, tzinfo=d.timezone.utc)
ago = lambda hours: (now - d.timedelta(hours=float(hours))).strftime("%Y-%m-%dT%H:%M:%SZ")
run = lambda status, rid: f"---\nid: {rid}\nstatus: {status}\n---\n"
o = {"report": "none", "pending": "none", "pending_hours": "10", "pending_report": "none", "updated": "100",
     "start": "100", "state": "active", "nodocs": "", "old": ""}
extra = {}
for arg in sys.argv[2:]:
    if arg.startswith("{"):
        extra = json.loads(arg)
    else:
        key, value = arg.split("=", 1)
        o[key] = value
branches = [{"name": "main"}, {"name": "claude/T-099-other"}]
routes = {}
if o["report"] != "none":
    branches.append({"name": "claude/RUN-20261029-01-report"})
    if o["report"] != "nofile":
        statuses = o["report"].split(",")
        texts = [run(s, "RUN-20261029-01") for s in statuses]
        routes["contents/docs/runs/RUN-20261029-01.md?ref=claude/RUN-20261029-01-report"] = \
            texts[0] if len(texts) == 1 else {"__sequence__": texts}
listing = [{"name": "README.md", "type": "file"}, {"name": "RUN-20261029-01.md", "type": "file"}]
if o["pending"] != "none":
    listing.append({"name": "RUN-20261028-01.md", "type": "file"})
    routes["contents/docs/runs/RUN-20261028-01.md?ref=main"] = run(o["pending"], "RUN-20261028-01")
    routes["commits?path=docs/runs/RUN-20261028-01.md&sha=main&per_page=1"] = \
        [{"commit": {"committer": {"date": ago(o["pending_hours"])}}}]
    if o["pending_report"] != "none":
        branches.append({"name": "claude/RUN-20261028-01-report"})
        routes["contents/docs/runs/RUN-20261028-01.md?ref=claude/RUN-20261028-01-report"] = \
            run(o["pending_report"], "RUN-20261028-01")
errors = {}
if o["old"]:
    listing.append({"name": "RUN-20261001-01.md", "type": "file"})
    errors["contents/docs/runs/RUN-20261001-01.md?ref=main"] = "HTTP 500: Server Error"
if not o["nodocs"]:
    routes["contents/docs/runs?ref=main"] = listing
routes["branches?per_page=100&page=1"] = branches
for name in ("resume-run.yml", "run-watchdog.yml"):
    routes[f"actions/workflows/{name}"] = {"state": o["state"], "updated_at": ago(o["updated"])}
routes["actions/workflows/start-run.yml/runs?per_page=1"] = \
    {"workflow_runs": [] if o["start"] == "none" else [{"created_at": ago(o["start"])}]}
fx = {"routes": routes, "errors": errors}
for key, value in extra.items():
    if isinstance(value, dict):
        fx.setdefault(key, {}).update(value)
    else:
        fx[key] = value
json.dump(fx, open(sys.argv[1], "w"))
PY
schedule_idle() { # <mode args...> -- [key=value ...] [extra JSON]; prints the exit code, output in $work/si.out
  local mode=() code=0
  while [ "$1" != "--" ]; do mode+=("$1"); shift; done
  shift
  rm -f "$work/si.json.writes" "$work/si.json.seen"
  python3 -I "$work/si-fixture.py" "$work/si.json" "$@"
  PATH="$work/bin:$PATH" FIXTURE="$work/si.json" GITHUB_REPOSITORY=o/r SCHEDULE_IDLE_NOW=2026-10-29T12:00:00Z \
    python3 -I scripts/schedule-idle.py "${mode[@]}" > "$work/si.out" 2>&1 || code=$?
  echo "$code"
}
si_writes() { if [ -f "$work/si.json.writes" ]; then paste -sd ';' - < "$work/si.json.writes"; else echo none; fi; }
si_off="PUT actions/workflows/resume-run.yml/disable"
si_on="PUT actions/workflows/resume-run.yml/enable"
si_both="PUT actions/workflows/resume-run.yml/enable;PUT actions/workflows/run-watchdog.yml/enable"
expect "schedule-idle: a running session keeps the schedule" "0 none" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- report=running) $(si_writes)"
expect "schedule-idle: no session, no pending list, enabled long ago, no recent start-run: disables" "0 $si_off" \
  "$(schedule_idle --disable-if-idle resume-run.yml --) $(si_writes)"
expect "schedule-idle: ... and says it turned the schedule off" 1 "$(grep -c 'đã tắt' "$work/si.out")"
expect "schedule-idle: an approved run list without a report branch keeps the schedule" "0 none" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- pending=approved pending_hours=10) $(si_writes)"
expect "schedule-idle: ... but not once it was merged more than 72 hours ago" "0 $si_off" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- pending=approved pending_hours=80) $(si_writes)"
expect "schedule-idle: ... nor once its report branch is finished" "0 $si_off" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- pending=approved pending_report=finished) $(si_writes)"
expect "schedule-idle: a draft run list does not keep the schedule" "0 $si_off" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- pending=draft) $(si_writes)"
expect "schedule-idle: enabled within the grace hours keeps" "0 none" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- updated=1) $(si_writes)"
expect "schedule-idle: ... but not after 4 hours" "0 $si_off" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- updated=4) $(si_writes)"
expect "schedule-idle: start-run ran within the grace hours keeps" "0 none" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- start=1) $(si_writes)"
expect "schedule-idle: ... but not after 4 hours" "0 $si_off" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- start=4) $(si_writes)"
expect "schedule-idle: no start-run run at all still disables, exit 0" "0 $si_off" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- start=none) $(si_writes)"
expect "schedule-idle: a session that appears after the disable turns the schedule back on" "0 $si_off;$si_on" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- report=finished,running) $(si_writes)"
expect "schedule-idle: ... and says why" 1 "$(grep -c 'bật lại' "$work/si.out")"
expect "schedule-idle: a failed second read turns the schedule back on" "0 $si_off;$si_on" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- \
    '{"routes": {"branches?per_page=100&page=1": {"__sequence__": [[{"name": "main"}], "x"]}}}') $(si_writes)"
expect "schedule-idle: no docs/runs folder means no pending list" "0 $si_off" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- nodocs=1) $(si_writes)"
expect "schedule-idle: an approved run list dated more than 14 days ago is not read" "0 $si_off" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- old=1) $(si_writes)"
expect "schedule-idle: a finished run does not keep the schedule" "0 $si_off" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- report=finished) $(si_writes)"
expect "schedule-idle: a report branch without its run file does not keep the schedule" "0 $si_off" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- report=nofile) $(si_writes)"
expect "schedule-idle: an unreadable run file keeps the schedule, exit 0" "0 none" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- report=running \
    '{"errors": {"contents/docs/runs/RUN-20261029-01.md?ref=claude/RUN-20261029-01-report": "HTTP 500: Server Error"}}') $(si_writes)"
expect "schedule-idle: GitHub unreadable keeps, exit 0" "0 none 1" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- '{"errors": {"branches?per_page=100&page=1": "HTTP 502: Bad Gateway"}}') $(si_writes) $(grep -c 'Không đọc được GitHub' "$work/si.out")"
expect "schedule-idle: an unreadable workflow keeps, exit 0" "0 none" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- '{"errors": {"actions/workflows/resume-run.yml": "HTTP 500: Server Error"}}') $(si_writes)"
expect "schedule-idle: a failed disable warns and exits 0" "0 1" \
  "$(schedule_idle --disable-if-idle resume-run.yml -- '{"fail_writes": ["PUT actions/workflows/resume-run.yml/disable"]}') $(grep -c 'CHÚ Ý' "$work/si.out")"
expect "schedule-idle: disables only the workflow it is given" "0 PUT actions/workflows/run-watchdog.yml/disable" \
  "$(schedule_idle --disable-if-idle run-watchdog.yml --) $(si_writes)"
expect "schedule-idle: an unknown workflow is refused" "2 none" \
  "$(schedule_idle --disable-if-idle staging.yml --) $(si_writes)"
expect "schedule-idle --enable: enables both schedules" "0 $si_both" \
  "$(schedule_idle --enable --) $(si_writes)"
expect "schedule-idle --enable: a failed enable exits 1 and names the workflow" "1 1" \
  "$(schedule_idle --enable -- '{"fail_writes": ["PUT actions/workflows/run-watchdog.yml/enable"]}') $(grep -c 'Chưa bật được lịch: run-watchdog.yml\.' "$work/si.out")"
expect "schedule-idle --enable: a schedule still disabled after enable exits 1" 1 \
  "$(schedule_idle --enable -- state=disabled_manually)"
expect "schedule-idle --enable-if-pending: enables when an approved list waits" "0 $si_both" \
  "$(schedule_idle --enable-if-pending -- pending=approved) $(si_writes)"
expect "schedule-idle --enable-if-pending: enables while a session runs" "0 $si_both" \
  "$(schedule_idle --enable-if-pending -- report=running) $(si_writes)"
expect "schedule-idle --enable-if-pending: nothing otherwise" "0 none" \
  "$(schedule_idle --enable-if-pending --) $(si_writes)"
expect "schedule-idle --enable-if-pending: a failed enable warns and exits 0" "0 1" \
  "$(schedule_idle --enable-if-pending -- pending=approved '{"fail_writes": ["PUT actions/workflows/resume-run.yml/enable"]}') $(grep -c '::warning::.*resume-run.yml' "$work/si.out")"
expect "schedule-idle --enable-if-pending: GitHub unreadable enables to be safe" "0 $si_both" \
  "$(schedule_idle --enable-if-pending -- '{"errors": {"branches?per_page=100&page=1": "HTTP 502: Bad Gateway"}}') $(si_writes)"

# The workflows that call it: each schedule turns itself off in a job of its own, after its main job, and only that
# job may write Actions; start-run.yml and status-page.yml turn both on.
wf_job() { # <workflow> <job>; prints the lines of that job
  awk -v job="  $2:" '$0 == job { f = 1; print; next } f && /^  [^ #]/ { f = 0 } f' "$1"
}
wf_level() { awk '/^permissions:$/ { f = 1; next } f && /^[^ ]/ { f = 0 } f' "$1"; }
si_idle_job() { # <workflow file name> <main job>; "yes" when the idle job is complete
  local wf=".github/workflows/$1" job
  job="$(wf_job "$wf" idle)"
  if grep -qE "^    needs: $2$" <<<"$job" && grep -qE "^    if: always\(\) && github.ref == 'refs/heads/main'$" <<<"$job" &&
    grep -qE '^      actions: write$' <<<"$job" && grep -qF 'GH_TOKEN: ${{ github.token }}' <<<"$job" &&
    grep -qF 'persist-credentials: false' <<<"$job" &&
    grep -qF "python3 -I scripts/schedule-idle.py --disable-if-idle $1" <<<"$job" &&
    ! grep -q 'actions: write' <<<"$(wf_level "$wf")"; then echo yes; else echo no; fi
}
expect "workflows: resume-run and run-watchdog disable themselves when idle" "yes yes" \
  "$(si_idle_job resume-run.yml resume) $(si_idle_job run-watchdog.yml watch)"
expect "workflows: the jobs with secrets or issue writes keep their permissions" "0 0" \
  "$(wf_job .github/workflows/resume-run.yml resume | grep -c 'actions: write') $(wf_job .github/workflows/run-watchdog.yml watch | grep -c 'actions: write')"
expect "start-run: enables both schedules before firing the routine" "yes" \
  "$(wf_job .github/workflows/start-run.yml fire | awk '/^      actions: write$/ { p = NR } /GH_TOKEN: \$\{\{ github.token \}\}/ { t = NR }
     /actions\/workflows\/resume-run.yml\/enable/ { r = NR } /actions\/workflows\/run-watchdog.yml\/enable/ { w = NR }
     /curl / && !c { c = NR } END { print ((p && t && r && w && c && r < c && w < c) ? "yes" : "no") }')"
expect "start-run: only the fire job writes Actions" "1" "$(grep -c 'actions: write' .github/workflows/start-run.yml)"
expect "status-page: a separate job enables the schedules for a pending run list" "yes 0 0" \
  "$(wf_job .github/workflows/status-page.yml schedules | grep -q '^      actions: write$' && wf_job .github/workflows/status-page.yml schedules | grep -qF 'python3 -I scripts/schedule-idle.py --enable-if-pending' && echo yes || echo no) $(wf_job .github/workflows/status-page.yml page | grep -c 'actions: write') $(wf_level .github/workflows/status-page.yml | grep -c 'actions: write')"

# Self-hosted runner for the heavy jobs (ADR-0042). A heavy job runs scripts/check.sh, ./gradlew or the emulator and
# reads no secret: it runs on the runner the repository variable HEAVY_RUNNER names, else on ubuntu-latest. Every
# other job stays on ubuntu-latest. Each check takes a directory or a workflow, so it also runs on broken copies.
heavy_runs_on="    runs-on: \${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}"
job_kind() { # <workflow> <job>; secret, heavy or light (comment lines do not count)
  local body
  body="$(wf_job "$1" "$2" | grep -vE '^[[:space:]]*#' || true)"
  if grep -qF 'secrets.' <<<"$body"; then
    echo secret
  elif grep -qE 'scripts/check\.sh|\./gradlew|android-emulator-runner' <<<"$body"; then
    echo heavy
  else
    echo light
  fi
}
wf_jobs() { # <workflow>; the names of its jobs, one per line
  awk '/^jobs:$/ { j = 1; next } j && /^[^ #]/ { j = 0 } j && /^  [A-Za-z0-9_-]+:$/ { sub(/:$/, ""); sub(/^  /, ""); print }' "$1"
}
heavy_jobs() { # [<directory>]; "<file> <job>" of every heavy job, sorted
  local f j
  for f in "${1:-.github/workflows}"/*.yml; do
    for j in $(wf_jobs "$f"); do
      if [[ "$(job_kind "$f" "$j")" == heavy ]]; then echo "$(basename "$f") $j"; fi
    done
  done | sort
}
runner_rule() { # [<directory>]; "yes", or "no" and every <file>:<job> whose runs-on is wrong
  local f j want bad=""
  for f in "${1:-.github/workflows}"/*.yml; do
    for j in $(wf_jobs "$f"); do
      want="    runs-on: ubuntu-latest"
      if [[ "$(job_kind "$f" "$j")" == heavy ]]; then want="$heavy_runs_on"; fi
      [[ "$(wf_job "$f" "$j" | grep -E '^    runs-on:' || true)" == "$want" ]] || bad="$bad $(basename "$f"):$j"
    done
  done
  if [[ -n "$bad" ]]; then echo "no$bad"; else echo yes; fi
}
rwf_copy() { # <name>; a copy of every workflow in $work/rwf/<name>, printed
  rm -rf "$work/rwf/$1"
  mkdir -p "$work/rwf/$1"
  cp .github/workflows/*.yml "$work/rwf/$1/"
  echo "$work/rwf/$1"
}
expect "runners: heavy jobs read HEAVY_RUNNER, every other job stays on ubuntu-latest" yes "$(runner_rule)"
rwd="$(rwf_copy a)"
perl -pi -e "s/^    runs-on: ubuntu-latest\$/    runs-on: \\\${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}/" "$rwd/staging.yml"
expect "runners: the runner check fails on a copy whose staging job reads HEAVY_RUNNER" "no staging.yml:distribute" \
  "$(runner_rule "$rwd")"
rwd="$(rwf_copy b)"
awk '/^  guard:$/ { g = 1 } /^  ui-test:$/ { g = 0 } g && /^    runs-on:/ { print "    runs-on: ubuntu-latest"; next } { print }' \
  .github/workflows/main-guard.yml > "$rwd/main-guard.yml"
expect "runners: the runner check fails on a copy whose guard job stays on ubuntu-latest" "no main-guard.yml:guard" \
  "$(runner_rule "$rwd")"
mkdir -p "$work/rwf/kind"
printf '%s\n' 'jobs:' '  x:' '    runs-on: ubuntu-latest' '    steps:' '      # ./gradlew build' '      - run: echo hi' \
  > "$work/rwf/kind/comment.yml"
printf '%s\n' 'jobs:' '  x:' '    runs-on: ubuntu-latest' '    steps:' '      - run: ./gradlew build' > "$work/rwf/kind/gradle.yml"
expect "runners: ./gradlew in a comment does not make a job heavy, in a step it does" "light heavy" \
  "$(job_kind "$work/rwf/kind/comment.yml" x) $(job_kind "$work/rwf/kind/gradle.yml" x)"
expect "runners: the heavy jobs are build-and-test, guard and ui-test" \
  "main-guard.yml guard
main-guard.yml ui-test
pr-check.yml build-and-test" "$(heavy_jobs)"
mkdir -p "$work/rwf/light"
cp .github/workflows/status-page.yml "$work/rwf/light/"
expect "runners: on a directory without heavy jobs the runner check passes and the heavy job list is empty" "yes|" \
  "$(runner_rule "$work/rwf/light")|$(heavy_jobs "$work/rwf/light")"

# Steps of one job, parsed for the order and sudo checks below.
cat > "$work/wf-steps.py" << 'PY'
import re, sys
mode, wf, job = sys.argv[1:4]
body, inside = [], False
for line in open(wf, encoding="utf-8").read().split("\n"):
    if line == "  " + job + ":":
        inside = True; continue
    if inside and (re.match(r"^  [^ #]", line) or re.match(r"^[^ #]", line)):
        break
    if inside:
        body.append(line)
steps, in_steps = [], False
for line in body:
    if line == "    steps:":
        in_steps = True; continue
    if re.match(r"^    [^ ]", line):
        in_steps = False; continue
    if not in_steps:
        continue
    if line.startswith("      - "):
        steps.append([line[8:]])
    elif steps and line.startswith("        "):
        steps[-1].append(line[8:])
def parse(step):
    keys, cur = {}, None
    for l in step:
        m = re.match(r"^([A-Za-z0-9_-]+):(?: (.*))?$", l)
        if m:
            cur = m.group(1); keys[cur] = [m.group(2) or ""]
        elif cur is not None:
            keys[cur].append(l)
    return keys
def text(keys, k):
    if k not in keys:
        return None
    first, rest = keys[k][0], keys[k][1:]
    lines = rest if first.strip() in ("|", "|-", ">", ">-") else [first] + rest
    return "\n".join(x.strip() for x in lines if x.strip() and not re.match(r"^\s*#", x))
parsed = [parse(s) for s in steps]
def label(p):
    return text(p, "name") or text(p, "uses") or text(p, "run") or "?"
def done(answer):
    print(answer); sys.exit(0)
if mode == "sudo":
    bad = [label(p) for p in parsed if re.search(r"\bsudo ", text(p, "run") or "")
           and "runner.environment == 'github-hosted'" not in (text(p, "if") or "")]
    done("no " + ", ".join(bad) if bad else "yes")
CHECK = "Kiểm runner tự host (scripts/runner-check.sh)"
checkout = [i for i, p in enumerate(parsed) if (text(p, "uses") or "").startswith("actions/checkout@")]
if not checkout:
    done("no checkout")
check = [i for i, p in enumerate(parsed) if text(p, "name") == CHECK]
if not check:
    done("no runner check")
c, k = checkout[0], check[0]
p = parsed[k]
emulator = any("android-emulator-runner" in l for l in body if not re.match(r"^\s*#", l))
if text(p, "if") != "runner.environment == 'self-hosted'":
    done("no runner check if")
if "REPO_VISIBILITY: ${{ github.event.repository.visibility }}" not in (text(p, "env") or "").split("\n"):
    done("no REPO_VISIBILITY")
if text(p, "run") != ("scripts/runner-check.sh --emulator" if emulator else "scripts/runner-check.sh"):
    done("no runner check run: " + (text(p, "run") or ""))
def builds(p):
    uses, run = text(p, "uses") or "", text(p, "run") or ""
    return (uses.startswith(("actions/setup-java@", "gradle/actions/setup-gradle@")) or text(p, "id") == "mode"
            or "gradlew" in run or re.search(r"(?<!runner-)check\.sh", run) or "tests/run.sh" in run)
for i in range(c + 1, k):
    if builds(parsed[i]):
        done("no runner check after " + label(parsed[i]))
if k != c + 1:
    done("no runner check not right after checkout")
ALLOWED = {'"$GITHUB_OUTPUT"', '"$GITHUB_STEP_SUMMARY"', "build.log", "/dev/null"}
for i in range(k):
    p = parsed[i]
    uses = text(p, "uses")
    if uses is not None:
        if uses.startswith(("actions/checkout@", "dorny/paths-filter@")):
            continue
        done("no before the runner check: " + uses)
    extra = sorted(set(p) - {"name", "id", "if", "env", "run"})
    if "run" not in p or extra:
        done("no before the runner check: " + label(p) + " has " + (", ".join(extra) or "no run"))
    run = text(p, "run")
    for word in ("gradle", "$HOME", "${HOME}", "~/", "/home/", "/root/"):
        if word in run:
            done("no before the runner check: " + word)
    m = re.search(r"(^|[^A-Za-z0-9_-])(cp|mv|mkdir|touch|ln|install|wget|dd)([^A-Za-z0-9_-]|$)", run, re.M)
    if m:
        done("no before the runner check: " + m.group(2))
    if re.search(r"\bcurl\b[^\n]*\s(-[A-Za-z]*[oO]|--output|--remote-name)\b", run):
        done("no before the runner check: curl -o")
    if re.search(r"\bsed\b[^\n]*\s(-[A-Za-z]*i|--in-place)", run):
        done("no before the runner check: sed in place")
    targets = [t for _, t in re.findall(r"(>>?)\s*(&\d|[^\s;|&()]+)", run)]
    targets += [t for t in re.findall(r"\btee\b(?:\s+-[A-Za-z]+)*\s+([^\s;|&()]+)", run)]
    for t in targets:
        if t not in ALLOWED and not re.match(r"^&\d$", t):
            done("no before the runner check: writes " + t)
done("yes")
PY
runner_order() { python3 -I "$work/wf-steps.py" order "$1" "$2"; } # <workflow> <job>; "yes" or "no <why>"
runner_order_all() { # [<directory>]; "yes" when runner_order is yes for every heavy job, else the first "<job>: no ..."
  local dir="${1:-.github/workflows}" f j r
  while read -r f j; do
    [[ -n "$f" ]] || continue
    r="$(runner_order "$dir/$f" "$j")"
    [[ "$r" == yes ]] || { echo "$j: $r"; return; }
  done <<<"$(heavy_jobs "$dir")"
  echo yes
}
expect "runners: every heavy job checks the runner right after checkout" "yes 3" \
  "$(runner_order_all) $(heavy_jobs | grep -c .)"
expect "runners: the order check passes on pr-check (filter steps before checkout), guard and ui-test" "yes yes yes" \
  "$(runner_order "$pr_wf" build-and-test) $(runner_order "$guard_wf" guard) $(runner_order "$guard_wf" ui-test)"
rwd="$(rwf_copy order)"
# Broken copies, each with one change: the runner check moved after setup-gradle, without --emulator, without if.
awk '/^  guard:$/ { g = 1 } /^  ui-test:$/ { g = 0 }
  g && $0 == "      - name: Kiểm runner tự host (scripts/runner-check.sh)" { h = 1 }
  h { if ($0 == "") { h = 0 } else { held = held $0 "\n" } ; next }
  { print } g && held != "" && $0 == "        uses: gradle/actions/setup-gradle@v4" { printf "\n%s", held; held = "" }' \
  "$guard_wf" > "$rwd/guard-late.yml"
expect "runners: the order check fails when the runner check comes after setup-gradle" \
  "no runner check after Chọn cách kiểm (scripts/main-guard-verified.sh)" "$(runner_order "$rwd/guard-late.yml" guard)"
sed 's|run: scripts/runner-check.sh --emulator$|run: scripts/runner-check.sh|' "$guard_wf" > "$rwd/no-emulator.yml"
expect "runners: the order check fails when ui-test checks the runner without --emulator" \
  "no runner check run: scripts/runner-check.sh" "$(runner_order "$rwd/no-emulator.yml" ui-test)"
grep -vxF "        if: runner.environment == 'self-hosted'" "$pr_wf" > "$rwd/no-if.yml" || true
expect "runners: the order check fails when the runner check has no if" "no runner check if" \
  "$(runner_order "$rwd/no-if.yml" build-and-test)"
rwf_before_checkout() { # <copy name> <step line>...; pr-check.yml with that step before checkout; prints its order
  STEP="$(printf '%s\n' "${@:2}")" awk '!d && $0 == "      - uses: actions/checkout@v5" { print ENVIRON["STEP"]; print ""; d = 1 } { print }' \
    "$pr_wf" > "$rwd/$1.yml"
  runner_order "$rwd/$1.yml" build-and-test
}
expect "runners: the order check refuses a step before the check that writes ~/.gradle" "no before the runner check: gradle" \
  "$(rwf_before_checkout gradle-home '      - name: x' '        run: echo x > ~/.gradle/init.d/a.gradle')"
expect "runners: the order check refuses a step before the check that writes \$HOME" 'no before the runner check: $HOME' \
  "$(rwf_before_checkout home '      - name: x' '        run: echo x > "$HOME/build.log"')"
expect "runners: the order check refuses a step before the check that writes \${HOME}" 'no before the runner check: ${HOME}' \
  "$(rwf_before_checkout home-braces '      - name: x' '        run: echo x > "${HOME}/a"')"
expect "runners: the order check refuses curl -o and curl -O before the check" \
  "no before the runner check: curl -o|no before the runner check: curl -o" \
  "$(rwf_before_checkout curl-o '      - name: x' '        run: curl -sS -o a.txt https://example.com/a')|$(rwf_before_checkout curl-big-o '      - name: x' '        run: curl -sSL -O https://example.com/a')"
expect "runners: the order check refuses cp and install before the check" \
  "no before the runner check: cp|no before the runner check: install" \
  "$(rwf_before_checkout cp '      - name: x' '        run: cp a b')|$(rwf_before_checkout install '      - name: x' '        run: install -m 644 a b')"
expect "runners: the order check refuses a redirection to another file before the check" \
  "no before the runner check: writes /tmp/a" "$(rwf_before_checkout tmp '      - name: x' '        run: echo x > /tmp/a')"
expect "runners: the order check refuses working-directory before the check" \
  "no before the runner check: x has working-directory" \
  "$(rwf_before_checkout workdir '      - name: x' '        working-directory: /home/runner/.gradle/init.d' '        run: echo x')"
expect "runners: the order check refuses setup-gradle before checkout" \
  "no before the runner check: gradle/actions/setup-gradle@v4" \
  "$(rwf_before_checkout setup-gradle '      - uses: gradle/actions/setup-gradle@v4')"
expect "runners: the order check matches whole words: add-on and copy-ok to build.log pass" "yes|yes" \
  "$(rwf_before_checkout add-on '      - name: x' '        run: echo add-on | tee -a build.log')|$(rwf_before_checkout copy-ok '      - name: x' '        run: echo copy-ok >> build.log')"

# sudo exists on GitHub runners only: the steps of heavy jobs that use it run only there.
sudo_rule() { # [<directory>]; "yes", or "no" and the offending steps
  local dir="${1:-.github/workflows}" f j r bad=""
  while read -r f j; do
    [[ -n "$f" ]] || continue
    r="$(python3 -I "$work/wf-steps.py" sudo "$dir/$f" "$j")"
    [[ "$r" == yes ]] || bad="$bad $j: ${r#no }"
  done <<<"$(heavy_jobs "$dir")"
  if [[ -n "$bad" ]]; then echo "no$bad"; else echo yes; fi
}
expect "runners: sudo steps of heavy jobs run only on GitHub runners" yes "$(sudo_rule)"
rwd="$(rwf_copy sudo)"
awk '{ print } $0 == "      - name: Bật KVM cho emulator" { getline; if ($0 !~ /^        if: /) print }' "$guard_wf" > "$rwd/main-guard.yml"
expect "runners: the sudo check fails on a copy whose KVM step has no if" "no ui-test: Bật KVM cho emulator" \
  "$(sudo_rule "$rwd")"
expect "runners: device-session (a job with secrets) keeps its sudo steps and is not checked" \
  "secret no" "$(job_kind .github/workflows/device-session.yml emulator) $(python3 -I "$work/wf-steps.py" sudo .github/workflows/device-session.yml emulator | cut -d' ' -f1)"

# A self-hosted runner starts every job from a fresh checkout.
heavy_clean() { # [<directory>]; "yes" when no heavy job sets clean: false
  local dir="${1:-.github/workflows}" f j body bad=""
  while read -r f j; do
    [[ -n "$f" ]] || continue
    # Read whole first: under pipefail, grep -q stopping early would make the pipeline fail.
    body="$(wf_job "$dir/$f" "$j" | grep -vE '^[[:space:]]*#' || true)"
    if grep -qE '^[[:space:]]+clean: false' <<<"$body"; then bad="$bad $f:$j"; fi
  done <<<"$(heavy_jobs "$dir")"
  if [[ -n "$bad" ]]; then echo "no$bad"; else echo yes; fi
}
expect "runners: heavy jobs keep the default clean checkout" yes "$(heavy_clean)"
rwd="$(rwf_copy clean)"
awk '{ print } /^  guard:$/ { g = 1 } g && $0 == "          fetch-depth: 50" { print "          clean: false"; g = 0 }' \
  "$guard_wf" > "$rwd/main-guard.yml"
expect "runners: the clean check fails on a copy whose guard checkout keeps old files" "no main-guard.yml:guard" \
  "$(heavy_clean "$rwd")"

# resume-check.py reads the Actions runs that wait (ADR-0042).
expect "resume-run: the workflow may read Actions runs" "1 0" \
  "$(wf_level .github/workflows/resume-run.yml | grep -cx '  actions: read' || true) $(wf_level .github/workflows/resume-run.yml | grep -c 'actions: write' || true)"
grep -vx '  actions: read' .github/workflows/resume-run.yml > "$work/rwf/resume-run.yml" || true
expect "resume-run: the Actions read check fails on a copy without it" "0 0" \
  "$(wf_level "$work/rwf/resume-run.yml" | grep -cx '  actions: read' || true) $(wf_level "$work/rwf/resume-run.yml" | grep -c 'actions: write' || true)"

# scripts/runner-check.sh catches a self-hosted runner set up wrong, before anything is built (ADR-0042). Not a safety
# fence: a PR can change it. Each case runs it in a temporary repo, with a temporary HOME and no variable of this
# machine (GRADLE_USER_HOME, ANDROID_HOME); rc_* variables set the case: rc_vis (REPO_VISIBILITY, default private),
# rc_eph (VGL_RUNNER_EPHEMERAL, default 1, empty: not set), rc_guh (GRADLE_USER_HOME), rc_ah (ANDROID_HOME, set when
# the variable is set), rc_kvm (RUNNER_CHECK_KVM), rc_min (RUNNER_CHECK_MIN_FREE_GB, default 0), rc_dir (the directory),
# rc_log (RUNNER_CHECK_LOG, not set by default).
rc_script="$PWD/scripts/runner-check.sh"
rc_repo="$work/rc"
rc_home="$work/rc-home"
git init -q -b main "$rc_repo"
printf 'build/\n' > "$rc_repo/.gitignore"
echo a > "$rc_repo/a.txt"
git -C "$rc_repo" add -A && git -C "$rc_repo" commit -q -m init
rc_reset() { # a clean repo and an empty HOME
  git -C "$rc_repo" clean -q -fdx
  rm -rf "$rc_home" "$work/gh"
  mkdir -p "$rc_home"
}
rc() { # [args]; "<exit code> <lines RUNNER KHÔNG DÙNG ĐƯỢC>"; the output is in $work/rc.out
  local vars=(HOME="$rc_home" GIT_CEILING_DIRECTORIES="$work" REPO_VISIBILITY="${rc_vis-private}"
    RUNNER_CHECK_MIN_FREE_GB="${rc_min-0}") code=0
  [[ -z "${rc_eph-1}" ]] || vars+=(VGL_RUNNER_EPHEMERAL="${rc_eph-1}")
  [[ -z "${rc_guh:-}" ]] || vars+=(GRADLE_USER_HOME="$rc_guh")
  [[ -z "${rc_ah+set}" ]] || vars+=(ANDROID_HOME="$rc_ah")
  [[ -z "${rc_kvm:-}" ]] || vars+=(RUNNER_CHECK_KVM="$rc_kvm")
  [[ -z "${rc_log:-}" ]] || vars+=(RUNNER_CHECK_LOG="$rc_log")
  (cd "${rc_dir:-$rc_repo}" && env -u GRADLE_USER_HOME -u ANDROID_HOME -u VGL_RUNNER_EPHEMERAL -u REPO_VISIBILITY \
    -u RUNNER_CHECK_MIN_FREE_GB -u RUNNER_CHECK_KVM -u RUNNER_CHECK_LOG "${vars[@]}" "$rc_script" "$@") \
    > "$work/rc.out" 2>&1 || code=$?
  echo "$code $(grep -c '^RUNNER KHÔNG DÙNG ĐƯỢC: .* (docs/ops/self-hosted-runner.md)\.$' "$work/rc.out" || true)"
}
rc_reset
expect "runner-check: a clean one-shot container of a private repo is usable" "0 0 1" \
  "$(rc) $(grep -c '^RUNNER DÙNG ĐƯỢC: repo private, đĩa trống [0-9]* GB\.$' "$work/rc.out" || true)"
expect "runner-check: a public repo, or no visibility, is refused; internal is usable" "1 1|1 1|0 0" \
  "$(rc_vis=public rc)|$(rc_vis='' rc)|$(rc_vis=internal rc)"
expect "runner-check: a container without the one-shot mark is refused" "1 1" "$(rc_eph='' rc)"
rc_unmarked_line="$(rc_eph='' rc > /dev/null; cat "$work/rc.out")"
rc_gradle=""
for f in init.d/x.gradle init.gradle init.gradle.kts gradle.properties; do
  rc_reset
  mkdir -p "$(dirname "$rc_home/.gradle/$f")"
  echo x > "$rc_home/.gradle/$f"
  rc_gradle="$rc_gradle$(rc)|"
done
expect "runner-check: an init script or gradle.properties in ~/.gradle is refused" "1 1|1 1|1 1|1 1|" "$rc_gradle"
rc_reset
mkdir -p "$work/gh/init.d"
expect "runner-check: GRADLE_USER_HOME with init.d is refused" "1 1" "$(rc_guh="$work/gh" rc)"
rc_reset
mkdir -p "$rc_home/.gradle/init.d" "$work/gh"
expect "runner-check: only GRADLE_USER_HOME counts when it is set" "0 0" "$(rc_guh="$work/gh" rc)"
rc_reset
mkdir -p "$rc_home/.gradle/caches"
expect "runner-check: caches in ~/.gradle are allowed" "0 0" "$(rc)"
rc_reset
mkdir -p "$rc_repo/build" "$rc_repo/core/x/src/test/screenshots"
echo x > "$rc_repo/build/x"
rc_ignored="$(rc)"
rm -rf "$rc_repo/build"
echo x > "$rc_repo/core/x/src/test/screenshots/a.png"
expect "runner-check: an ignored or an untracked file in the work directory is refused" "1 1|1 1|1" \
  "$rc_ignored|$(rc)|$(grep -c 'thư mục làm việc có file ngoài git: core/' "$work/rc.out" || true)"
rc_reset
mkdir -p "$work/rc-nogit"
expect "runner-check: a work directory that is not a git repo is refused" "1 1" "$(rc_dir="$work/rc-nogit" rc)"
expect "runner-check: too little free disk is refused" "1 1" "$(rc_min=999999 rc)"
rc_reset
mkdir -p "$work/rc-sdk"
: > "$work/rc-kvm"
expect "runner-check --emulator: no KVM is refused; KVM and a writable ANDROID_HOME are usable" "1 1|0 0|1" \
  "$(rc_kvm="$work/nokvm" rc_ah="$work/rc-sdk" rc --emulator)|$(rc_kvm="$work/rc-kvm" rc_ah="$work/rc-sdk" rc --emulator)|$(grep -c '^RUNNER DÙNG ĐƯỢC: .*, KVM và ANDROID_HOME dùng được\.$' "$work/rc.out" || true)"
expect "runner-check: without --emulator KVM is not checked" "0 0" "$(rc_kvm="$work/nokvm" rc)"
expect "runner-check --emulator: an empty ANDROID_HOME is refused" "1 1" "$(rc_kvm="$work/rc-kvm" rc_ah='' rc --emulator)"
if [[ "$(id -u)" == 0 ]]; then
  echo "skip - runner-check --emulator: a read-only ANDROID_HOME is refused (root writes everywhere)"
else
  mkdir -p "$work/rc-sdk-ro"
  chmod 555 "$work/rc-sdk-ro"
  expect "runner-check --emulator: a read-only ANDROID_HOME is refused" "1 1" \
    "$(rc_kvm="$work/rc-kvm" rc_ah="$work/rc-sdk-ro" rc --emulator)"
  chmod 755 "$work/rc-sdk-ro"
fi
rc_reset
mkdir -p "$rc_home/.gradle/init.d"
expect "runner-check: every problem is reported, not only the first" "1 3" "$(rc_vis=public rc_eph='' rc)"
rc_reset
expect "runner-check: refuses other arguments with exit 2" "2 2" \
  "$(rc --emu | cut -d' ' -f1) $(rc --emulator x | cut -d' ' -f1)"
expect "runner-check: the script is executable in git" 100755 "$(git ls-files -s scripts/runner-check.sh | cut -c1-6)"
# RUNNER_CHECK_LOG: the result lines also go to that file, appended after every check (T-095), so the failure comment
# of pr-check.yml shows them; build.log in the work directory does not make the script refuse its own directory.
rc_same() { if cmp -s "$work/rc.log" "$work/rc.out"; then echo same; else echo differ; fi; }
rc_reset
rm -f "$work/rc.log"
rc_r1="$(rc_log="$work/rc.log" rc)|$(rc_same)|$(grep -c '^RUNNER DÙNG ĐƯỢC' "$work/rc.log" 2> /dev/null || true)"
rm -f "$work/rc.log"
rc_r1="$rc_r1|$(rc_log="$work/rc.log" rc_vis=public rc_eph='' rc)|$(rc_same)|$(grep -c '^RUNNER KHÔNG DÙNG ĐƯỢC:' "$work/rc.log" 2> /dev/null || true)"
rm -f "$work/rc.log"
echo x > "$work/rc.log"
rc_r1="$rc_r1|$(rc_log="$work/rc.log" rc)|$(head -n1 "$work/rc.log")|$(wc -l < "$work/rc.log" | tr -d ' ')"
expect "runner-check: RUNNER_CHECK_LOG gets the same result lines" "0 0|same|1|1 2|same|2|0 0|x|2" "$rc_r1"
rc_reset
rc_r2="$(rc_log=build.log rc)|$(grep -c '^RUNNER DÙNG ĐƯỢC' "$rc_repo/build.log" 2> /dev/null || true)"
rc_r2="$rc_r2|$(rc_log=build.log rc)|$(grep -c 'file ngoài git: build.log' "$work/rc.out" || true)"
expect "runner-check: a RUNNER_CHECK_LOG inside the work directory is written after the checks" "0 0|1|1 1|1" "$rc_r2"
rc_reset
expect "runner-check: without RUNNER_CHECK_LOG nothing is written" "0 0|" \
  "$(rc)|$(git -C "$rc_repo" status --porcelain --ignored)"
rm -f "$work/rc2.log"
expect "runner-check: a usage error writes no log" "2|no" \
  "$(rc_log="$work/rc2.log" rc --x | cut -d' ' -f1)|$(if [ -e "$work/rc2.log" ]; then echo yes; else echo no; fi)"
rc_reset
rc_r5="$(rc_log="$work" rc)|$(grep -c 'CHÚ Ý: không ghi được' "$work/rc.out" || true)"
rc_r5="$rc_r5|$(rc_log="$work" rc_vis=public rc)|$(grep -c 'CHÚ Ý: không ghi được' "$work/rc.out" || true)"
expect "runner-check: a RUNNER_CHECK_LOG that cannot be written keeps the result" "0 0|1|1 1|1" "$rc_r5"

# pr-check.yml passes RUNNER_CHECK_LOG: build.log to the runner check; main-guard.yml has no failure comment that
# reads build.log, so it does not (T-095).
rcl_lines() { # <workflow> <job>; lines "RUNNER_CHECK_LOG: build.log" in the env of the runner check step of that job
  awk -v job="  $2:" '$0 == job { j = 1; next } j && /^  [^ ]/ { j = 0 }
    j && /^      - / { s = ($0 == "      - name: Kiểm runner tự host (scripts/runner-check.sh)"); e = 0; next }
    j && s && $0 == "        env:" { e = 1; next } j && s && e && !/^          / { e = 0 }
    j && s && e && $0 == "          RUNNER_CHECK_LOG: build.log" { n++ } END { print n + 0 }' "$1"
}
grep -vxF '          RUNNER_CHECK_LOG: build.log' "$pr_wf" > "$work/pr-no-log.yml" || true
expect "pr-check: the runner check writes its result lines to build.log" "1 0|0" \
  "$(rcl_lines "$pr_wf" build-and-test) $(grep -c 'RUNNER_CHECK_LOG' "$guard_wf" || true)|$(rcl_lines "$work/pr-no-log.yml" build-and-test)"

# The failure comment step of pr-check.yml, run like GitHub in the work directory after a refused runner, shows the
# lines of runner-check.sh instead of "Không có log." (T-095). A gh of its own: fake-gh only answers `gh api`.
mkdir -p "$work/gh-comment"
printf '#!/bin/sh\nexit 0\n' > "$work/gh-comment/gh"
chmod +x "$work/gh-comment/gh"
rc_reset
rc_p2="$(rc_log=build.log rc_vis=public rc)"
pr_comment_script="$(awk '/^      - / { s = 0 } /^      - name: Bình luận lỗi vào PR$/ { s = 1; next }
    s && /^        run: \|$/ { r = 1; next } r && s && /^ *$/ { print ""; next }
    r && s && /^          / { sub(/^          /, ""); print; next } r { r = 0 }' "$pr_wf" |
  sed -e 's/${{ steps\.release\.outcome }}//g' -e 's/${{ github\.event\.pull_request\.number }}/1/g' \
    -e 's|${{ github\.repository }}|o/r|g')"
(cd "$rc_repo" && PATH="$work/gh-comment:$work/bin:$PATH" bash -eo pipefail -c "$pr_comment_script") > /dev/null 2>&1 || true
expect "pr-check: the failure comment shows the lines of a refused runner" "1 1|1|0" \
  "$rc_p2|$(grep -c '^RUNNER KHÔNG DÙNG ĐƯỢC: repo không phải private' "$rc_repo/comment.md" 2> /dev/null || true)|$(grep -c 'Không có log\.' "$rc_repo/comment.md" 2> /dev/null || true)"
rc_reset

# The log files of the playbook rows are what the scripts print.
expect "playbook: PB-17.log is what wait-run.sh prints" "7 $(head -n1 "$pb_dir/PB-17.log") u" "$rw_main_line"
expect "playbook: PB-19.log is what runner-check.sh prints" "$(head -n1 "$pb_dir/PB-19.log")" "$rc_unmarked_line"

# DesignCompare: an identical image differs by 0%, a design without a screenshot is reported missing.
java_cmd="java"
[ -z "${JAVA_HOME:-}" ] || java_cmd="${JAVA_HOME}/bin/java"
mkdir -p "$work/design" "$work/shots"
cp feature/settings/src/test/screenshots/SettingsScreen_default_light.png "$work/design/Same.png"
cp feature/settings/src/test/screenshots/SettingsScreen_default_light.png "$work/shots/Same.png"
# Two screens that differ in content, so they differ under any app theme, including one without dark mode (L-144).
cp feature/settings/src/test/screenshots/SettingsScreen_language-picker_light.png "$work/design/Other.png"
cp feature/settings/src/test/screenshots/SettingsScreen_default_light.png "$work/shots/Other.png"
cp feature/settings/src/test/screenshots/SettingsScreen_default_light.png "$work/design/Missing.png"
"$java_cmd" -Djava.awt.headless=true scripts/design-compare/DesignCompare.java "$work/design" "$work/shots" "$work/review" > /dev/null
expect "design-compare: identical images differ by 0%" "| Same | 0.0% | \`Same_compare.png\` |" "$(grep '^| Same ' "$work/review/summary.md")"
expect "design-compare: two different screens differ" yes "$(grep -qE '^\| Other \| ([1-9][0-9]?|100)\.[0-9]%' "$work/review/summary.md" && echo yes || echo no)"
expect "design-compare: missing screenshot is listed" 1 "$(grep -c '^| Missing | thiếu ảnh chuẩn' "$work/review/summary.md")"
expect "design-compare: writes side-by-side images" yes "$([ -s "$work/review/Same_compare.png" ] && echo yes || echo no)"

# figma-export.py (offline): context files match frames by name and map values to tokens.json.
# The fixture tokens.json is a fixed copy, so changing the real design system never breaks these tests (L-020).
FIGMA_EXPORT_FEATURES_DIR="$work/features" FIGMA_EXPORT_TOKENS=scripts/tests/fixtures/tokens.json \
  python3 -I scripts/figma-export.py --offline \
  scripts/tests/fixtures/figma-nodes.json loading > /dev/null
context="$work/features/loading/design/context"
expect "figma-export: only frames named like screenshots" "LoadingScreen_failed_light.md" "$(ls "$context")"
expect "figma-export: text style matched to a token" 1 "$(grep -c 'kiểu titleMedium (16/24, đậm 500)' "$context/LoadingScreen_failed_light.md")"
expect "figma-export: color matched to light roles" 1 "$(grep -c '| màu nền | #FEF7FF | background, surface |' "$context/LoadingScreen_failed_light.md")"
expect "figma-export: unknown values flagged" 2 "$(grep -c '| ngoài hệ thống |' "$context/LoadingScreen_failed_light.md")"
expect "figma-export: hidden layers left out" 0 "$(grep -c 'Hidden' "$context/LoadingScreen_failed_light.md")"
# Unfolded foldable frames (673 x 841, suffix _unfolded) and states with hyphens are exported too.
python3 -I - scripts/tests/fixtures/figma-nodes.json "$work/figma-fold.json" <<'PY'
import copy, json, sys
data = json.load(open(sys.argv[1]))
frame = next(c for c in data["children"] if c["name"] == "LoadingScreen_failed_light")
fold = copy.deepcopy(frame); fold["id"] = "9:1"; fold["name"] = "LoadingScreen_failed_light_unfolded"
fold["absoluteBoundingBox"].update(width=673, height=841)
hyphen = copy.deepcopy(frame); hyphen["id"] = "9:2"; hyphen["name"] = "LoadingScreen_no-network_dark"
wrong = copy.deepcopy(frame); wrong["id"] = "9:3"; wrong["name"] = "LoadingScreen_failed_dark_unfolded"
data["children"] += [fold, hyphen, wrong]
json.dump(data, open(sys.argv[2], "w"))
PY
FIGMA_EXPORT_FEATURES_DIR="$work/fold" FIGMA_EXPORT_TOKENS=scripts/tests/fixtures/tokens.json \
  python3 -I scripts/figma-export.py --offline "$work/figma-fold.json" loading > /dev/null
fold_context="$work/fold/loading/design/context"
expect "figma-export: unfolded and hyphenated frames are exported" 4 "$(ls "$fold_context" | wc -l | tr -d ' ')"
expect "figma-export: an unfolded frame of the right size has no size note" 0 \
  "$(grep -c 'CHÚ Ý: khung' "$fold_context/LoadingScreen_failed_light_unfolded.md")"
expect "figma-export: an unfolded frame of the phone size is flagged" 1 \
  "$(grep -c 'CHÚ Ý: khung không phải 673 x 841' "$fold_context/LoadingScreen_failed_dark_unfolded.md")"
expect "figma-export: spacing matched" 1 "$(grep -c 'cách nhau 16 (md)' "$context/LoadingScreen_failed_light.md")"
# Opacity of a paint and of a layer, and instances overriding their component's fills, are written (L-023).
python3 -I - scripts/tests/fixtures/figma-nodes.json "$work/figma-opacity.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
frame = next(c for c in data["children"] if c["name"] == "LoadingScreen_failed_light")
title = frame["children"][0]
title["opacity"] = 0.38
button = {"id": "1:7", "name": "Retry", "type": "INSTANCE", "componentId": "5:1",
          "absoluteBoundingBox": {"x": 160, "y": 400, "width": 120, "height": 40},
          "fills": [{"type": "SOLID", "opacity": 0.12, "color": {"r": 0.114, "g": 0.106, "b": 0.125, "a": 1}}],
          "overrides": [{"id": "1:7", "overriddenFields": ["fills", "name"]}]}
frame["children"].append(button)
near = dict(frame, id="9:9", name="LoadingScreen_failed", children=[])
data["children"].append(near)
json.dump(data, open(sys.argv[2], "w"))
PY
FIGMA_EXPORT_FEATURES_DIR="$work/opacity" FIGMA_EXPORT_TOKENS=scripts/tests/fixtures/tokens.json \
  python3 -I scripts/figma-export.py --offline "$work/figma-opacity.json" loading > /dev/null 2> "$work/figma-opacity.err"
opacity_context="$work/opacity/loading/design/context/LoadingScreen_failed_light.md"
expect "figma-export: layer opacity is written" 2 "$(grep -cE 'độ mờ của lớp (0.38|\| 0.38)' "$opacity_context")"
expect "figma-export: paint opacity is named apart" 1 "$(grep -c 'nền #1D1B20 (độ mờ 0.12, lớp tô 0.12)' "$opacity_context")"
expect "figma-export: an instance overriding its component's fills is flagged" 2 \
  "$(grep -cE 'CHÚ Ý: ghi đè màu nền của component gốc|\| instance ghi đè component \| Retry \| màu nền \|' "$opacity_context")"
expect "figma-export: a frame named almost like a screen is reported" 1 \
  "$(grep -c 'bỏ qua frame "LoadingScreen_failed"' "$work/figma-opacity.err")"

# release.sh must refuse a release folder that is incomplete or holds staging/test values, before touching git.
release_dir="$work/release"
release_fails_with() {
  local name="$1" pattern="$2" output
  if output="$(scripts/release.sh --config "$release_dir" "${@:3}" 2>&1 < /dev/null)"; then
    expect "release.sh refuses: $name" refused accepted
  elif [[ "$output" == *"$pattern"* ]]; then
    expect "release.sh refuses: $name" refused refused
  else
    expect "release.sh refuses: $name" "message with '$pattern'" "$output"
  fi
}
write_release_properties() {
  printf '%s\n' "admobAppId=$1" "adUnits=$2" "assetsBaseUrl=$3" "uploadKeyAlias=upload" > "$release_dir/release.properties"
}
mkdir -p "$release_dir"
release_fails_with "missing files" "/release/google-services.json"
touch "$release_dir/google-services.json" "$release_dir/upload.jks"
write_release_properties "ca-app-pub-3940256099942544~3347511713" "" "https://cdn.example.vn/"
release_fails_with "Google test app id" "mã thử của Google"
write_release_properties "ca-app-pub-1234567890123456/1111111111" "" "https://cdn.example.vn/"
release_fails_with "ad unit given as app id" "admobAppId phải có dạng"
write_release_properties "ca-app-pub-1234567890123456~1111111111" "sample_bottom_banner=ca-app-pub-3940256099942544/9214589741" "https://cdn.example.vn/"
release_fails_with "Google test ad unit" "mã quảng cáo thử"
write_release_properties "ca-app-pub-1234567890123456~1111111111" "sample_bottom_banner" "https://cdn.example.vn/"
release_fails_with "malformed ad unit entry" "mục adUnits"
write_release_properties "ca-app-pub-1234567890123456~1111111111" "" "http://cdn.example.vn/"
release_fails_with "assets address without https" "assetsBaseUrl phải"
release_fails_with "unknown argument" "tham số không hợp lệ" --upload

# new-app.sh renames the template into a new repository and leaves out the template's own files.
# It exists only in the template repository: an app made from it has no new-app.sh, so the app skips these tests.
new_app="$work/new-app"
# CI runners have no git identity; the new repository needs one for its first commit.
if [[ ! -f scripts/new-app.sh ]]; then
  echo "skip - new-app.sh: chỉ có ở repo dự án mẫu"
elif GIT_AUTHOR_NAME=test GIT_AUTHOR_EMAIL=test@example.com GIT_COMMITTER_NAME=test GIT_COMMITTER_EMAIL=test@example.com \
  scripts/new-app.sh --dest "$new_app" --package com.example.notes --name "Ghi chú" \
  --staging-assets-url https://notes.example.vn/staging/ > /dev/null 2>&1; then
  expect "new-app.sh succeeds" ok ok
  expect "new-app.sh leaves out itself" absent "$([ -e "$new_app/scripts/new-app.sh" ] && echo present || echo absent)"
  expect "new-app.sh replaces the package outside ADRs" "" \
    "$(grep -rlIF com.vglstudio.camera.pose.line.guide "$new_app" --exclude-dir=.git --exclude-dir=adr || true)"
  expect "new-app.sh moves sources" yes \
    "$([ -f "$new_app/app/src/main/kotlin/com/example/notes/MainActivity.kt" ] && echo yes || echo no)"
  expect "new-app.sh sets the app name" 1 "$(grep -c '>Ghi chú</string>' "$new_app/app/src/main/res/values/strings.xml")"
  expect "new-app.sh sets the staging id of staging.yml" 1 \
    "$(grep -c 'STAGING_APPLICATION_ID: com.example.notes.staging$' "$new_app/.github/workflows/staging.yml")"
  expect "new-app.sh drops links to template-only files" 0 \
    "$(grep -cE 'ops/new-app\.md' "$new_app/README.md" || true)"
  expect "new-app.sh leaves out the from-app issue form" absent \
    "$([ -e "$new_app/.github/ISSUE_TEMPLATE/from-app.yml" ] && echo present || echo absent)"
  expect "new-app.sh starts an empty lessons log" 0 "$(grep -c '^### L-' "$new_app/docs/lessons.md" || true)"
  expect "new-app.sh leaves out the template's run reports" 0 "$(ls "$new_app/docs/runs" | grep -c '^RUN-' || true)"
  expect "new-app.sh leaves out the template's tickets, plans, feedback and requests" "" \
    "$(find "$new_app/docs/features" -mindepth 2 -maxdepth 2 -type d \( -name tickets -o -name plans -o -name feedback -o -name requests \))"
  expect "new-app.sh leaves out the template's process feature" absent \
    "$([ -e "$new_app/docs/features/quy-trinh" ] && echo present || echo absent)"
  expect "new-app.sh keeps the specs and designs of the template's modules" "yes yes" \
    "$([ -f "$new_app/docs/features/loading/spec.md" ] && echo yes || echo no) $([ -d "$new_app/docs/features/loading/design" ] && echo yes || echo no)"
  expect "new-app.sh regenerates the progress page without the template's lessons" 0 \
    "$(grep -c 'L-0[0-9][0-9]' "$new_app/docs/STATUS.md" || true)"
  expect "new-app.sh leaves pages that match the documents" 0 \
    "$(cd "$new_app" && python3 -I scripts/status.py --check > /dev/null && python3 -I scripts/foundation.py --check > /dev/null; echo $?)"
  expect "new-app.sh commits everything" "" "$(git -C "$new_app" status --porcelain)"
  expect "new-app.sh leaves the progress page out of git" "" "$(git -C "$new_app" ls-files docs/STATUS.md)"
  expect "new-app.sh leaves out superseded ADRs" "" \
    "$(grep -lE '^status:[[:space:]]*superseded' "$new_app"/docs/adr/*.md || true)"
  expect "new-app.sh keeps only the header of the template's token log" 1 \
    "$(wc -l < "$new_app/docs/metrics/token-usage.csv" | tr -d ' ')"
  expect "new-app.sh leaves out the history check and its allow list" "absent absent" \
    "$([ -e "$new_app/scripts/history-refs.py" ] && echo present || echo absent) $(
      [ -e "$new_app/scripts/tests/history-refs-allow.txt" ] && echo present || echo absent)"
  expect "the app made by new-app.sh has no broken relative link" 0 \
    "$(python3 -I scripts/history-refs.py --links "$new_app" > /dev/null; echo $?)"
else
  expect "new-app.sh succeeds" ok failed
fi
for args in "--package com.vglstudio.camera.pose.line.guide.notes" "--package Notes" "--staging-assets-url https://app-project-template.vgl.io.vn/"; do
  # shellcheck disable=SC2086
  if scripts/new-app.sh --dest "$work/rejected" --package com.example.notes --name "Ghi chú" \
    --staging-assets-url https://notes.example.vn/ $args > /dev/null 2>&1; then
    expect "new-app.sh rejects '$args'" rejected accepted
  else
    expect "new-app.sh rejects '$args'" rejected rejected
  fi
done
if [[ -f scripts/new-app.sh ]]; then
  expect "new-app.sh records the template commit and package for template-sync.sh" \
    "base=$(git rev-parse HEAD) package=com.example.notes" \
    "$(tr '\n' ' ' < "$new_app/docs/template-sync.txt" | sed 's/ $//')"
  expect "new-app.sh leaves out template-sync.sh" absent \
    "$([ -e "$new_app/scripts/template-sync.sh" ] && echo present || echo absent)"
fi

# Workflow commands (.claude/skills/) only point at documents and scripts, so they must name real ones. In the
# template /whatnow lists every command; an app adds its own commands without changing /whatnow, which shows them
# from the folder. Files that exist only in the template, or only in an app, are left out.
skill_problems=""
for skill in .claude/skills/*/SKILL.md; do
  dir="$(basename "$(dirname "$skill")")"
  [[ "$(sed -n '2s/^name: //p' "$skill")" == "$dir" ]] || skill_problems+=" $dir:name"
  grep -q '^description: .' "$skill" || skill_problems+=" $dir:description"
  if [[ -f docs/ops/new-app.md ]]; then
    grep -q "^| \`/$dir[ \`]" .claude/skills/whatnow/SKILL.md || skill_problems+=" $dir:whatnow"
  fi
  for path in $(grep -oE '(scripts/[A-Za-z0-9_./-]+\.(sh|py)|docs/[A-Za-z0-9_./-]+\.md)' "$skill" | sort -u); do
    case "$path" in
      scripts/new-app.sh | scripts/template-sync.sh | scripts/history-refs.py | docs/ops/new-app.md | *YYYY* | *NNN*) continue ;;
    esac
    # Generated pages such as docs/STATUS.md are ignored by git, so a fresh checkout (CI) does not have them.
    [[ -e "$path" ]] || git check-ignore -q "$path" || skill_problems+=" $dir:$path"
  done
done
expect "every skill names its folder, real files and, in the template, has a row in /whatnow" "" "$skill_problems"

# The command that lists the reference images a recording changed (CLAUDE.md, section Lệnh; the plan template,
# section 4) must list a changed image and a new one. A wildcard pathspec that ends in "/" matches no file (L-136).
sp_repo="$work/screenshot-pathspec"
git init -q -b main "$sp_repo"
mkdir -p "$sp_repo/feature/demo/src/test/screenshots"
echo old > "$sp_repo/feature/demo/src/test/screenshots/Changed.png"
git -C "$sp_repo" add -A && git -C "$sp_repo" commit -q -m root
echo new > "$sp_repo/feature/demo/src/test/screenshots/Changed.png"
echo new > "$sp_repo/feature/demo/src/test/screenshots/Added.png"
for doc in CLAUDE.md docs/templates/plan.md; do
  pathspec="$(sed -n "s/.*git status --short -- '\([^']*\)'.*/\1/p" "$doc" | sed -n 1p)"
  expect "$doc: the command for changed reference images lists a changed one and a new one" 2 \
    "$(git -C "$sp_repo" status --short -- "$pathspec" | grep -c -e 'screenshots/Changed\.png$' -e 'screenshots/Added\.png$' || true)"
done

# template-sync.sh brings later template commits into an app made by new-app.sh (ADR-0030). It runs on a scratch
# copy of the template, so the test can add template commits; like new-app.sh it exists only in the template.
if [[ ! -f scripts/template-sync.sh ]]; then
  echo "skip - template-sync.sh: chỉ có ở repo dự án mẫu"
else
  (
    failures=0
    export GIT_AUTHOR_NAME=test GIT_AUTHOR_EMAIL=test@example.com GIT_COMMITTER_NAME=test GIT_COMMITTER_EMAIL=test@example.com
    tpl="$work/sync-template" app="$work/sync-app"
    mkdir -p "$tpl"
    git ls-files -z --cached --others --exclude-standard | xargs -0 tar -cf - 2> /dev/null | tar -xf - -C "$tpl"
    git -C "$tpl" init --quiet -b main
    git -C "$tpl" add -A > /dev/null
    git -C "$tpl" commit --quiet -m "template v1"
    "$tpl/scripts/new-app.sh" --dest "$app" --package com.example.notes --name "Ghi chú" \
      --staging-assets-url https://notes.example.vn/staging/ > /dev/null 2>&1
    v1="$(git -C "$tpl" rev-parse HEAD)"
    logger_kt="core/logging/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/logging/AppLogger.kt"
    ads_kt="core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIds.kt"
    echo "# template v2" >> "$tpl/scripts/wait-run.sh"
    echo "// template v2: com.vglstudio.camera.pose.line.guide.core.logging" >> "$tpl/$logger_kt"
    echo "// template v2" >> "$tpl/$ads_kt"
    echo "template v2" >> "$tpl/docs/lessons.md"
    # ADR-0001 is replaced in v2; the app also still has ADR-0022, replaced before its base, like the apps made
    # before ADR-0044.
    adr1="$(cd "$tpl" && ls docs/adr/0001-*.md)" adr22="$(cd "$tpl" && ls docs/adr/0022-*.md)"
    sed -i.bak 's/^status: accepted/status: superseded/' "$tpl/$adr1" && rm "$tpl/$adr1.bak"
    git -C "$tpl" commit --quiet -am "template v2"
    v2="$(git -C "$tpl" rev-parse HEAD)"
    echo "hold=core/ads $v1" >> "$app/docs/template-sync.txt"
    git -C "$tpl" show "$v1:$adr22" > "$app/$adr22"
    git -C "$app" add -- "$adr22"
    git -C "$app" commit --quiet -am "hold ads"
    app_logger="$app/core/logging/src/main/kotlin/com/example/notes/core/logging/AppLogger.kt"
    app_ads="$app/core/ads/src/main/kotlin/com/example/notes/core/ads/AdUnitIds.kt"

    code=0; out="$("$tpl/scripts/template-sync.sh" --app "$app" --to "$v2" 2>&1)" || code=$?
    expect "template-sync.sh merges a template commit without conflicts" "0 1" "$code $(grep -c 'Xong, không có xung đột' <<< "$out")"
    expect "template-sync.sh works on a new branch of the app" "template-sync/${v2:0:7}" "$(git -C "$app" branch --show-current)"
    expect "template-sync.sh brings a changed script" 1 "$(grep -cx '# template v2' "$app/scripts/wait-run.sh")"
    expect "template-sync.sh renames the package in moved sources" 1 \
      "$(grep -cx '// template v2: com.example.notes.core.logging' "$app_logger")"
    expect "template-sync.sh leaves the template's lessons out" 0 "$(grep -c 'template v2' "$app/docs/lessons.md")"
    expect "template-sync.sh keeps a held path at its commit" 0 "$(grep -c 'template v2' "$app_ads")"
    expect "template-sync.sh moves the base in the same commit" "base=$v2" "$(grep '^base=' "$app/docs/template-sync.txt")"
    expect "template-sync.sh removes an ADR superseded in the template, and one superseded before the base" \
      "absent absent" "$([ -e "$app/$adr1" ] && echo present || echo absent) $([ -e "$app/$adr22" ] && echo present || echo absent)"
    expect "template-sync.sh says nothing about removed ADRs the app did not change" 0 \
      "$(grep -c '^CHÚ Ý: ADR' <<< "$out" || true)"
    expect "template-sync.sh lists rule documents of the app that name a removed ADR" 1 \
      "$(grep -cE '^   CLAUDE\.md: .*ADR-0001' <<< "$out")"
    expect "template-sync.sh leaves the app clean" "" "$(git -C "$app" status --porcelain)"

    code=0; out="$("$tpl/scripts/template-sync.sh" --app "$app" --to "$v2" 2>&1)" || code=$?
    expect "template-sync.sh has nothing to do at the base" "0 1" "$code $(grep -c 'Không có gì mới' <<< "$out")"

    code=0; "$tpl/scripts/template-sync.sh" --app "$app" --take core/ads > /dev/null 2>&1 || code=$?
    expect "template-sync.sh --take brings a held path up to the base" "0 1 0" \
      "$code $(grep -cx '// template v2' "$app_ads") $(grep -c '^hold=' "$app/docs/template-sync.txt")"

    sed -i.bak '2s/.*/# app change/' "$app/scripts/wait-run.sh" && rm "$app/scripts/wait-run.sh.bak"
    sed -i.bak '2s/.*/name: app change/' "$app/.claude/skills/whatnow/SKILL.md" && rm "$app/.claude/skills/whatnow/SKILL.md.bak"
    git -C "$app" commit --quiet -am "app change"
    sed -i.bak '2s/.*/# template v3/' "$tpl/scripts/wait-run.sh" && rm "$tpl/scripts/wait-run.sh.bak"
    sed -i.bak '2s/.*/name: template v3/' "$tpl/.claude/skills/whatnow/SKILL.md" && rm "$tpl/.claude/skills/whatnow/SKILL.md.bak"
    # new-app.sh put the app's name on the first line of README.md, so a template change there conflicts too.
    sed -i.bak '1s/.*/# TEMPLATE V3/' "$tpl/README.md" && rm "$tpl/README.md.bak"
    git -C "$tpl" commit --quiet -am "template v3"
    code=0; out="$("$tpl/scripts/template-sync.sh" --app "$app" --to HEAD 2>&1)" || code=$?
    expect "template-sync.sh stops on a conflict and sorts the files" "1 1 1" \
      "$code $(sed -n '/Phần của dự án mẫu/,/Phần app cũng sửa/p' <<< "$out" | grep -cx '   scripts/wait-run.sh') $(
        sed -n '/Phần app cũng sửa/,$p' <<< "$out" | grep -cx '   README.md')"
    # Workflow commands are the template's (docs/ops/to-app.md, section 2).
    expect "template-sync.sh sorts a Claude Code skill with the template's files" 1 \
      "$(sed -n '/Phần của dự án mẫu/,/Phần app cũng sửa/p' <<< "$out" | grep -cx '   .claude/skills/whatnow/SKILL.md')"

    code=0; out="$("$tpl/scripts/template-sync.sh" --app "$app" --to HEAD 2>&1)" || code=$?
    expect "template-sync.sh refuses an app with uncommitted changes" "1 1" "$code $(grep -c 'chưa commit' <<< "$out")"

    # An ADR the app changed and the template replaces is still removed, but the sync says so; an app's own ADR
    # (from 1001) is never removed, even when superseded.
    app2="$work/sync-app2"
    # new-app.sh needs the template's own README title, which the conflict test above changed.
    git -C "$tpl" show "$v1:README.md" > "$tpl/README.md" && git -C "$tpl" commit --quiet -am "template README back"
    "$tpl/scripts/new-app.sh" --dest "$app2" --package com.example.notes --name "Ghi chú" \
      --staging-assets-url https://notes.example.vn/staging/ > /dev/null 2>&1
    adr2="$(cd "$tpl" && ls docs/adr/0002-*.md)" adr3="$(cd "$tpl" && ls docs/adr/0003-*.md)"
    echo "app change" >> "$app2/$adr2"
    # ADR-0022 was replaced before the app's base; this app kept it and changed it.
    { git -C "$tpl" show "$v1:$adr22"; echo "app change"; } > "$app2/$adr22"
    # ADR-0003 is replaced by a merged ADR that copies it, which git may take for a rename of the app's changed file.
    echo "app change" >> "$app2/$adr3"
    printf -- '---\nid: ADR-1001\nstatus: superseded\norigin: app\n---\n' > "$app2/docs/adr/1001-rieng.md"
    git -C "$app2" add -A && git -C "$app2" commit --quiet -m "app changes an ADR"
    sed -i.bak 's/^status: accepted/status: superseded/' "$tpl/$adr2" && rm "$tpl/$adr2.bak"
    # The new line is near the top, so it merges cleanly with the app's line at the end.
    { sed -n 1p "$tpl/$adr3"; echo "merged: ADR-0003"; sed 1d "$tpl/$adr3"; } > "$tpl/docs/adr/0099-hop-nhat.md"
    sed -i.bak 's/^status: accepted/status: superseded/' "$tpl/$adr3" && rm "$tpl/$adr3.bak"
    git -C "$tpl" add -A && git -C "$tpl" commit --quiet -m "template v4"
    code=0; out="$("$tpl/scripts/template-sync.sh" --app "$app2" --to HEAD 2>&1)" || code=$?
    expect "template-sync.sh removes a replaced ADR the app changed, says so, and finishes" "0 1 1 absent" \
      "$code $(grep -c '^CHÚ Ý: ADR .*app đã sửa' <<< "$out") $(grep -cx "   ADR-0002 ($adr2)" <<< "$out") $(
        [ -e "$app2/$adr2" ] && echo present || echo absent)"
    expect "template-sync.sh never removes an ADR of the app" present \
      "$([ -e "$app2/docs/adr/1001-rieng.md" ] && echo present || echo absent)"
    expect "template-sync.sh says so for a changed ADR replaced before the app's base" 1 \
      "$(grep -cx "   ADR-0022 ($adr22)" <<< "$out")"
    expect "template-sync.sh says so when the app's change may have moved into the merged ADR" 1 \
      "$(grep -c "^   ADR-0003 ($adr3; .*gộp vào ADR mới docs/adr/0099-hop-nhat\.md," <<< "$out")"

    # When the list of mentions cannot be made, the sync still finishes and says how to make it.
    app3="$work/sync-app3"
    "$tpl/scripts/new-app.sh" --dest "$app3" --package com.example.notes --name "Ghi chú" \
      --staging-assets-url https://notes.example.vn/staging/ > /dev/null 2>&1
    # Any ADR still in force at the template's head: the sync below supersedes it.
    adr4="$(cd "$tpl" && ls docs/adr/0005-*.md)"
    sed -i.bak 's/^status: accepted/status: superseded/' "$tpl/$adr4" && rm "$tpl/$adr4.bak"
    git -C "$tpl" commit --quiet -am "template v5"
    mkdir -p "$work/broken-python"
    printf '#!/bin/sh\nexit 1\n' > "$work/broken-python/python3" && chmod +x "$work/broken-python/python3"
    code=0; out="$(PATH="$work/broken-python:$PATH" "$tpl/scripts/template-sync.sh" --app "$app3" --to HEAD 2>&1)" || code=$?
    expect "template-sync.sh finishes and says how to list mentions when the listing fails" "0 1 absent" \
      "$code $(grep -c 'không liệt kê được' <<< "$out") $([ -e "$app3/$adr4" ] && echo present || echo absent)"
    exit "$failures"
  ) || failures=$((failures + $?))
fi

# cloud-env-check.sh passes only when the environment was built from the current cloud-setup.sh and can run
# the project's Gradle (L-075, from app lesson L-056 of camera-pose).
fake_java() { # <Java major version> [line printed before it]; writes $work/fake-jdk/bin/java, which answers
  # `java -XshowSettings:properties -version` like a real JDK (java-check.sh runs $JAVA_HOME/bin/java).
  mkdir -p "$work/fake-jdk/bin"
  { printf '#!/bin/sh\n'
    [ -n "${2:-}" ] && printf 'echo '"'"'%s'"'"' >&2\n' "$2"
    printf 'echo "Property settings:" >&2\necho "    java.specification.version = %s" >&2\n' "$1"
    printf 'echo '"'"'openjdk version "%s"'"'"' >&2\n' "$1"; } > "$work/fake-jdk/bin/java"
  chmod +x "$work/fake-jdk/bin/java"
}
setup_version="$(sed -n 's/^SETUP_VERSION=\([0-9][0-9]*\)$/\1/p' scripts/cloud-setup.sh)"
compile_sdk="$(sed -n 's/^compileSdk = "\([0-9][0-9]*\)"$/\1/p' gradle/libs.versions.toml)"
mkdir -p "$work/gradle-home/init.d" "$work/sdk-ok/platforms/android-$compile_sdk" "$work/sdk-empty"
touch "$work/gradle-home/init.d/maven-central-mirror.gradle"
printf '#!/bin/sh\nexit 0\n' > "$work/gradlew-ok"
printf '#!/bin/sh\necho "Could not install Gradle distribution"\nexit 1\n' > "$work/gradlew-fail"
printf '#!/bin/sh\nsleep 5\n' > "$work/gradlew-hang"
printf '#!/bin/sh\nexit 0\n' > "$work/java-check-ok"
chmod +x "$work/gradlew-ok" "$work/gradlew-fail" "$work/gradlew-hang" "$work/java-check-ok"
# env_check <setup version> <gradle home>: prints pass or stop. Java, Gradle and the SDK are fakes that pass,
# unless the caller sets ENV_JAVA_CHECK, ENV_GRADLEW, ENV_SDK, ENV_GRADLEW_TIMEOUT. The output goes to $work/env-check.out.
env_check() {
  echo "$1" > "$work/cloud-setup-version"
  if CLOUD_SETUP_VERSION_FILE="$work/cloud-setup-version" GRADLE_USER_HOME="$2" \
    JAVA_CHECK_CMD="${ENV_JAVA_CHECK:-$work/java-check-ok}" GRADLEW_CMD="${ENV_GRADLEW:-$work/gradlew-ok}" \
    ANDROID_HOME="${ENV_SDK:-$work/sdk-ok}" GRADLEW_TIMEOUT="${ENV_GRADLEW_TIMEOUT:-300}" \
    scripts/cloud-env-check.sh > "$work/env-check.out" 2>&1; then echo pass; else echo stop; fi
}
expect "cloud-setup.sh has a version" yes "$([[ -n "$setup_version" ]] && echo yes || echo no)"
# Plugins that only Google's Maven repository hosts (Android, Google services, Crashlytics) must be found before
# the plugin portal: the portal sends what it does not host to Maven Central, which answers 429 to cloud sessions,
# and Gradle stops at that error instead of trying Google (L-121).
init_plugin_repos="$(sed -n '/settings.pluginManagement.repositories {/,/^    }$/p' scripts/cloud-setup.sh)"
expect "cloud-setup.sh: the init script adds Google to the plugin repositories" 1 \
  "$(grep -c 'google()' <<< "$init_plugin_repos" || true)"
expect "cloud-setup.sh: the init script puts Google before the plugin portal" yes \
  "$(awk '/google\(\)/ && !g {g = NR} /gradlePluginPortal\(\)/ && !p {p = NR}
    END {print (g && p && g < p) ? "yes" : "no"}' <<< "$init_plugin_repos")"
expect "cloud-env-check.sh passes on the current version" pass "$(env_check "$setup_version" "$work/gradle-home")"
expect "cloud-env-check.sh stops on an older version" stop "$(env_check 1 "$work/gradle-home")"
expect "cloud-env-check.sh stops without the mirror init script" stop \
  "$(env_check "$setup_version" "$work/no-gradle-home")"
fake_java 21 'Picked up JAVA_TOOL_OPTIONS: -Dx=1'
expect "cloud-env-check.sh passes with Java 21 after a Picked up line" pass \
  "$(ENV_JAVA_CHECK=scripts/java-check.sh JAVA_HOME="$work/fake-jdk" env_check "$setup_version" "$work/gradle-home")"
fake_java 26
expect "cloud-env-check.sh stops on Java 26 and names Java" stop \
  "$(ENV_JAVA_CHECK=scripts/java-check.sh JAVA_HOME="$work/fake-jdk" env_check "$setup_version" "$work/gradle-home")"
expect "cloud-env-check.sh names Java 26 in the message" 1 "$(grep -c 'Java 26' "$work/env-check.out" || true)"
expect "cloud-env-check.sh does not ask to paste the setup script for a Java problem" 0 \
  "$(grep -c 'dán lại toàn bộ scripts/cloud-setup.sh' "$work/env-check.out" || true)"
expect "cloud-env-check.sh points a Java problem at JAVA_TOOL_OPTIONS" 1 "$(grep -c 'JAVA_TOOL_OPTIONS' "$work/env-check.out" || true)"
expect "cloud-env-check.sh stops when the Gradle wrapper fails" stop \
  "$(ENV_GRADLEW="$work/gradlew-fail" env_check "$setup_version" "$work/gradle-home")"
expect "cloud-env-check.sh names the Gradle wrapper and its output" 2 \
  "$(grep -o 'Gradle wrapper\|Could not install Gradle distribution' "$work/env-check.out" | wc -l | tr -d ' ')"
if command -v timeout > /dev/null; then
  hang_start=$SECONDS
  hang_result="$(ENV_GRADLEW="$work/gradlew-hang" ENV_GRADLEW_TIMEOUT=1 env_check "$setup_version" "$work/gradle-home")"
  expect "cloud-env-check.sh stops when the Gradle wrapper hangs" stop "$hang_result"
  expect "cloud-env-check.sh gives up on a hanging Gradle wrapper within 4 seconds" yes \
    "$([ $((SECONDS - hang_start)) -lt 4 ] && echo yes || echo no)"
else
  echo "skip - cloud-env-check.sh Gradle wrapper timeout: máy test không có timeout"
fi
expect "cloud-env-check.sh stops without the compileSdk platform" stop \
  "$(ENV_SDK="$work/sdk-empty" env_check "$setup_version" "$work/gradle-home")"
expect "cloud-env-check.sh names the missing platform" 1 \
  "$(grep -c "platforms;android-$compile_sdk" "$work/env-check.out" || true)"
expect "cloud-env-check.sh passes with the compileSdk platform" pass \
  "$(ENV_SDK="$work/sdk-ok" env_check "$setup_version" "$work/gradle-home")"
env_check 1 "$work/gradle-home" > /dev/null
expect "cloud-env-check.sh tells to paste the setup script for a setup problem" 1 \
  "$(grep -c 'dán lại toàn bộ scripts/cloud-setup.sh' "$work/env-check.out" || true)"
# A version catalog without compileSdk must stop the check, not skip the SDK platform.
printf '[versions]\nminSdk = "26"\n' > "$work/no-compile-sdk.toml"
expect "cloud-env-check.sh stops when compileSdk cannot be read" stop \
  "$(VERSIONS_TOML="$work/no-compile-sdk.toml" env_check "$setup_version" "$work/gradle-home")"
expect "cloud-env-check.sh names compileSdk when it cannot be read" 1 \
  "$(grep -c '^LỖI: không đọc được compileSdk' "$work/env-check.out" || true)"
# The Android 37 package is platforms;android-37.0 and lives in platforms/android-37.0, not android-37 (T-079, L-127).
# A minor version other than .0 is another platform (android-36.1 is API level 36.1) and does not count.
expect "cloud-setup.sh installs platforms;android-37.0 next to platforms;android-36" "1 1" \
  "$(grep -cxF '  "platforms;android-37.0" \' scripts/cloud-setup.sh || true) $(grep -cxF '  "platforms;android-36" \' scripts/cloud-setup.sh || true)"
printf '[versions]\ncompileSdk = "37"\n' > "$work/v37.toml"
printf '[versions]\ncompileSdk = "36"\n' > "$work/v36.toml"
mkdir -p "$work/sdk-37-0/platforms/android-37.0" "$work/sdk-37/platforms/android-37" "$work/sdk-36-1/platforms/android-36.1"
expect "cloud-env-check.sh passes for compileSdk 37 with platforms/android-37.0" "pass 1" \
  "$(VERSIONS_TOML="$work/v37.toml" ENV_SDK="$work/sdk-37-0" env_check "$setup_version" "$work/gradle-home") $(grep -c 'platforms/android-37\.0' "$work/env-check.out" || true)"
expect "cloud-env-check.sh passes for compileSdk 37 with platforms/android-37" pass \
  "$(VERSIONS_TOML="$work/v37.toml" ENV_SDK="$work/sdk-37" env_check "$setup_version" "$work/gradle-home")"
expect "cloud-env-check.sh names both directories for a missing compileSdk 37" "stop 1" \
  "$(VERSIONS_TOML="$work/v37.toml" ENV_SDK="$work/sdk-empty" env_check "$setup_version" "$work/gradle-home") $(grep -c 'platforms/android-37 hay platforms/android-37\.0' "$work/env-check.out" || true)"
expect "cloud-env-check.sh does not take platforms/android-36.1 for compileSdk 36" stop \
  "$(VERSIONS_TOML="$work/v36.toml" ENV_SDK="$work/sdk-36-1" env_check "$setup_version" "$work/gradle-home")"

# local-setup.sh installs on a developer machine the JDK and the SDK packages of cloud-setup.sh, checking every
# download against its published checksum (T-069 of camera-pose). curl, the Adoptium API, Google's package list and
# sdkmanager are fakes, so no test touches the network; the JDK in the fake archive answers like fake_java above.
ls_dir="$work/local-setup"
mkdir -p "$ls_dir/bin" "$ls_dir/build"
ls_release="$(sed -n 's/^JDK_RELEASE="\(.*\)"$/\1/p' scripts/local-setup.sh)"
ls_tools="$(sed -nE 's/^CMDLINE_TOOLS_ZIP="commandlinetools-linux-([0-9]+)_latest\.zip"$/\1/p' scripts/cloud-setup.sh)"
# Fake curl: `curl -fsSL <url> -o <file>`; answers from $ls_dir by the shape of the URL, logs every URL and every
# argument list.
cat > "$ls_dir/bin/curl" << 'SH'
#!/usr/bin/env bash
echo "$*" >> "$LS_DIR/curl-args.log"
url="" out=""
while [ "$#" -gt 0 ]; do case "$1" in -o) out="$2"; shift 2 ;; --proto | --proto-redir | --connect-timeout | --retry) shift 2 ;; -*) shift ;; *) url="$1"; shift ;; esac; done
echo "$url" >> "$LS_DIR/curl.log"
case "$url" in
  *assets/release_name/*) src="$LS_DIR/jdk.json" ;;
  *repository2-3.xml) src="$LS_DIR/repository.xml" ;;
  *commandlinetools-mac-*_latest.zip) src="$LS_DIR/tools-mac.zip" ;;
  *commandlinetools-linux-*_latest.zip) src="$LS_DIR/tools-linux.zip" ;;
  *.tar.gz) src="$LS_DIR/jdk.tar.gz" ;;
  *) exit 22 ;;
esac
[ -f "$src" ] || exit 22
cp "$src" "$out"
SH
chmod +x "$ls_dir/bin/curl"
# ls_jdk <mac|linux> <Java major>: writes the fake JDK archive and the Adoptium answer (right checksum).
ls_jdk() {
  local home="$ls_dir/build/jdk/$ls_release"
  rm -rf "$ls_dir/build/jdk"
  [ "$1" = mac ] && home="$home/Contents/Home"
  mkdir -p "$home/bin"
  fake_java "$2"
  cp "$work/fake-jdk/bin/java" "$home/bin/java"
  tar -czf "$ls_dir/jdk.tar.gz" -C "$ls_dir/build/jdk" "$ls_release"
  jq -n --arg sum "$(shasum -a 256 "$ls_dir/jdk.tar.gz" | cut -d' ' -f1)" \
    '{binaries: [{package: {link: "https://github.com/adoptium/temurin21-binaries/releases/download/fake/jdk.tar.gz", checksum: $sum}}]}' > "$ls_dir/jdk.json"
}
# The fake command-line tools: sdkmanager logs its arguments and creates the package directories.
mkdir -p "$ls_dir/build/tools/cmdline-tools/bin"
cat > "$ls_dir/build/tools/cmdline-tools/bin/sdkmanager" << 'SH'
#!/usr/bin/env bash
echo "$*" >> "$LS_DIR/sdkmanager.log"
if [ "$1" = --install ]; then shift; for p in "$@"; do mkdir -p "$ANDROID_HOME/${p//;//}"; done; fi
SH
chmod +x "$ls_dir/build/tools/cmdline-tools/bin/sdkmanager"
# Two zips with different content, so each OS has its own SHA-1 in the fake package list.
for ls_os in mac linux; do
  (cd "$ls_dir/build/tools" && LS_OS_NAME="$ls_os" python3 -c 'import os, zipfile
with zipfile.ZipFile("../../tools-" + os.environ["LS_OS_NAME"] + ".zip", "w") as z:
    i = zipfile.ZipInfo("cmdline-tools/bin/sdkmanager"); i.external_attr = 0o755 << 16
    z.writestr(i, open("cmdline-tools/bin/sdkmanager").read())
    z.writestr("cmdline-tools/" + os.environ["LS_OS_NAME"] + ".txt", os.environ["LS_OS_NAME"])')
done
ls_repo() { # <sha1 for the mac zip> <sha1 for the linux zip>
  printf '<sdk>\n  <archive>\n    <complete>\n      <size>1</size>\n      <checksum type="sha1">%s</checksum>\n      <url>commandlinetools-mac-%s_latest.zip</url>\n    </complete>\n  </archive>\n  <archive>\n    <complete>\n      <size>1</size>\n      <checksum type="sha1">%s</checksum>\n      <url>commandlinetools-linux-%s_latest.zip</url>\n    </complete>\n  </archive>\n</sdk>\n' \
    "$1" "$ls_tools" "${2:-$1}" "$ls_tools" > "$ls_dir/repository.xml"
}
ls_calls() { if [ -f "$ls_dir/curl.log" ]; then wc -l < "$ls_dir/curl.log" | tr -d ' '; else echo 0; fi; }
ls_mac_sha1="$(shasum -a 1 "$ls_dir/tools-mac.zip" | cut -d' ' -f1)"
ls_linux_sha1="$(shasum -a 1 "$ls_dir/tools-linux.zip" | cut -d' ' -f1)"
# ls_run <home> [args...]: runs local-setup.sh with the fakes as a Mac on arm64 unless LS_OS, LS_ARCH say otherwise;
# prints the exit code; output in $ls_dir/out.
ls_run() {
  local home="$1"; shift
  set +e
  HOME="$home" XDG_DATA_HOME="" LS_DIR="$ls_dir" CURL="$ls_dir/bin/curl" \
    LOCAL_SETUP_OS="${LS_OS:-Darwin}" LOCAL_SETUP_ARCH="${LS_ARCH:-arm64}" \
    LOCAL_SETUP_ADOPTIUM_API=https://api.example.test/v3 LOCAL_SETUP_GOOGLE_REPO=https://dl.example.test/repository \
    scripts/local-setup.sh "$@" > "$ls_dir/out" 2>&1
  echo $?
  set -e
}
# ls_prefix <home> [script]: the default install dir that local-setup.sh prints for that HOME.
ls_prefix() {
  HOME="$1" XDG_DATA_HOME="" "${2:-scripts/local-setup.sh}" --print-versions 2> /dev/null | sed -n 's/^thư mục cài: //p' || true
}
ls_jdk mac 21
ls_repo "$ls_mac_sha1" "$ls_linux_sha1"

expect "local-setup.sh has a JDK release" yes "$([[ "$ls_release" == jdk-* ]] && echo yes || echo no)"
expect "local-setup.sh reads the command-line tools build from cloud-setup.sh" "command-line tools: $ls_tools" \
  "$(scripts/local-setup.sh --print-versions | sed -n 's/^command-line tools: /command-line tools: /p')"
printf 'CMDLINE_TOOLS_ZIP="commandlinetools-linux-42_latest.zip"\n"${SDKMANAGER}" --install \\\n  "platform-tools" \\\n  "platforms;android-99" > /dev/null\n' \
  > "$ls_dir/cloud-setup-fixture.sh"
expect "local-setup.sh reads the package list after --install" "gói SDK: platform-tools platforms;android-99" \
  "$(LOCAL_SETUP_CLOUD_SETUP="$ls_dir/cloud-setup-fixture.sh" scripts/local-setup.sh --print-versions | grep '^gói SDK')"
printf 'echo nothing here\n' > "$ls_dir/cloud-setup-broken.sh"
expect "local-setup.sh stops when cloud-setup.sh has no versions" 1 \
  "$(LOCAL_SETUP_CLOUD_SETUP="$ls_dir/cloud-setup-broken.sh" ls_run "$ls_dir/home-broken" --print-versions)"
expect "local-setup.sh names cloud-setup.sh when it cannot read the versions" 1 \
  "$(grep -c '^LỖI: không đọc được phiên bản command-line tools' "$ls_dir/out" || true)"
expect "local-setup.sh refuses an unknown argument" 2 "$(ls_run "$ls_dir/home-args" --prefx /tmp)"
expect "local-setup.sh refuses --prefix without a directory" 2 "$(ls_run "$ls_dir/home-args" --prefix)"
expect "local-setup.sh refuses a --prefix with a line break" 2 "$(ls_run "$ls_dir/home-args" --prefix "$ls_dir/a
b")"

# The default install dir is named after the repo, never after one app: the origin remote (the tests give every
# git command the remote https://github.com/o/r.git), else the repo directory, cut down to safe characters.
expect "local-setup.sh names the default install dir after the origin remote" "$ls_dir/home-mac/.local/share/r" \
  "$(ls_prefix "$ls_dir/home-mac")"
expect "local-setup.sh reads an ssh origin remote" "$ls_dir/h/.local/share/my-app" \
  "$(GIT_CONFIG_VALUE_0=git@github.com:org/my-app.git ls_prefix "$ls_dir/h")"
expect "local-setup.sh puts the install dir under XDG_DATA_HOME" "$ls_dir/xdg/r" \
  "$(HOME="$ls_dir/h" XDG_DATA_HOME="$ls_dir/xdg" scripts/local-setup.sh --print-versions | sed -n 's/^thư mục cài: //p')"
ls_named="$ls_dir/named/My App!"
mkdir -p "$ls_named/scripts"
cp scripts/local-setup.sh scripts/cloud-setup.sh "$ls_named/scripts/"
git init -q -b main "$ls_named"
expect "local-setup.sh falls back to the repo directory name without an origin remote" "$ls_dir/h/.local/share/My-App-" \
  "$(GIT_CONFIG_COUNT=0 ls_prefix "$ls_dir/h" "$ls_named/scripts/local-setup.sh")"
expect "local-setup.sh has no app name in its default install dir" 0 \
  "$(grep -c '^prefix=.*camera-pose' scripts/local-setup.sh || true)"

ls_mac_prefix="$(ls_prefix "$ls_dir/home-mac")"
expect "local-setup.sh --check on an empty machine reports missing" 1 "$(ls_run "$ls_dir/home-mac" --check)"
expect "local-setup.sh --check makes no download" 0 "$(ls_calls)"
rm -f "$ls_dir/curl.log" "$ls_dir/curl-args.log" "$ls_dir/sdkmanager.log"
expect "local-setup.sh installs on a Mac" 0 "$(ls_run "$ls_dir/home-mac")"
expect "local-setup.sh downloads the JDK info, the JDK, the package list and the tools" 4 \
  "$(ls_calls)"
expect "local-setup.sh allows only HTTPS, redirects included, on every download" 4 \
  "$(grep -c -- "--proto =https --proto-redir =https " "$ls_dir/curl-args.log" || true)"
expect "local-setup.sh asks Adoptium for the mac aarch64 JDK" 1 \
  "$(grep -c "release_name/eclipse/${ls_release//+/%2B}?architecture=aarch64&image_type=jdk&os=mac" "$ls_dir/curl.log" || true)"
expect "local-setup.sh downloads the mac command-line tools" 1 \
  "$(grep -c "/commandlinetools-mac-${ls_tools}_latest.zip$" "$ls_dir/curl.log" || true)"
expect "local-setup.sh installs the packages of cloud-setup.sh" \
  "--install $(scripts/local-setup.sh --print-versions | sed -n 's/^gói SDK: //p')" \
  "$(grep '^--install' "$ls_dir/sdkmanager.log")"
ls_env="$ls_mac_prefix/env.sh"
expect "local-setup.sh writes JAVA_HOME to Contents/Home on a Mac" 1 \
  "$(grep -c "^export JAVA_HOME=\"$ls_mac_prefix/jdk/$ls_release/Contents/Home\"$" "$ls_env" || true)"
expect "local-setup.sh writes ANDROID_HOME" 1 "$(grep -c "^export ANDROID_HOME=\"$ls_dir/home-mac/android-sdk\"$" "$ls_env" || true)"
expect "local-setup.sh writes ANDROID_SDK_ROOT" 1 "$(grep -c "^export ANDROID_SDK_ROOT=\"$ls_dir/home-mac/android-sdk\"$" "$ls_env" || true)"
expect "local-setup.sh puts the JDK and platform-tools on PATH" 1 \
  "$(grep -cF "/jdk/$ls_release/Contents/Home/bin:$ls_dir/home-mac/android-sdk/platform-tools:\${PATH}\"" "$ls_env" || true)"
expect "local-setup.sh accepts the SDK licences" 1 "$(grep -c '^--licenses$' "$ls_dir/sdkmanager.log" || true)"
expect "local-setup.sh leaves no temp dir behind" 0 \
  "$(find "$ls_dir/home-mac" -name '.tmp.*' 2> /dev/null | wc -l | tr -d ' ')"
# JAVA_HOME points nowhere before env.sh is sourced, so the Java of the machine running the tests cannot pass
# this test in place of the JDK that env.sh names (RUN-20261027-02 of camera-pose).
expect "env.sh gives a Java that java-check.sh accepts" 0 \
  "$(set +e; (export JAVA_HOME="$ls_dir/no-such-jdk"; . "$ls_env"; scripts/java-check.sh > /dev/null 2>&1); echo $?)"
cp "$ls_env" "$ls_dir/env-first.sh" 2> /dev/null || : > "$ls_dir/env-first.sh"
rm -f "$ls_dir/curl.log"
expect "local-setup.sh runs again without error" 0 "$(ls_run "$ls_dir/home-mac")"
expect "local-setup.sh downloads nothing the second time" 0 "$(ls_calls)"
expect "local-setup.sh says both steps were skipped" 2 "$(grep -c '^Bỏ qua:' "$ls_dir/out" || true)"
expect "local-setup.sh leaves env.sh as it was" same "$(cmp -s "$ls_dir/env-first.sh" "$ls_env" && echo same || echo changed)"
expect "local-setup.sh --check after the install passes" 0 "$(ls_run "$ls_dir/home-mac" --check)"
expect "local-setup.sh --check lists every package" 4 "$(grep -c '^QUA: \(platform-tools\|platforms;\|build-tools;\)' "$ls_dir/out" || true)"
expect "local-setup.sh --check finds platforms;android-37.0" 1 "$(grep -cxF 'QUA: platforms;android-37.0' "$ls_dir/out" || true)"

ls_pkg="$(scripts/local-setup.sh --print-versions | sed -n 's/^gói SDK: //p' | awk '{print $NF}' || true)"
mv "$ls_dir/home-mac/android-sdk/${ls_pkg//;//}" "$ls_dir/pkg-aside" 2> /dev/null || true
expect "local-setup.sh --check fails when one SDK package is missing" 1 "$(ls_run "$ls_dir/home-mac" --check)"
expect "local-setup.sh --check names the missing package" 1 "$(grep -cxF "THIẾU: $ls_pkg" "$ls_dir/out" || true)"
mv "$ls_dir/pkg-aside" "$ls_dir/home-mac/android-sdk/${ls_pkg//;//}" 2> /dev/null || true
# A JDK left half-installed (java does not run) is installed again, not taken as present.
printf '#!/bin/sh\nexit 1\n' > "$ls_mac_prefix/jdk/$ls_release/Contents/Home/bin/java" 2> /dev/null || true
rm -f "$ls_dir/curl.log"
expect "local-setup.sh installs a JDK that does not run again" 0 "$(ls_run "$ls_dir/home-mac")"
expect "local-setup.sh says the broken JDK is installed again" 1 "$(grep -c 'không chạy được (cài dở?): cài lại' "$ls_dir/out" || true)"
expect "local-setup.sh downloads only the JDK again" 2 "$(ls_calls)"
expect "local-setup.sh refuses a relative --prefix" 2 "$(ls_run "$ls_dir/home-args" --prefix relative/dir)"
expect "local-setup.sh refuses a relative --sdk" 2 "$(ls_run "$ls_dir/home-args" --sdk sdk)"
mkdir -p "$ls_dir/home-cloud" && echo 3 > "$ls_dir/home-cloud/.cloud-setup-version"
expect "local-setup.sh refuses to run in a cloud environment" 1 "$(ls_run "$ls_dir/home-cloud")"
expect "local-setup.sh points a cloud environment at CLAUDE.md" 1 "$(grep -c 'chỉ dành cho máy dev' "$ls_dir/out" || true)"
expect "local-setup.sh makes no download in a cloud environment" no "$([ -e "$ls_dir/home-cloud/android-sdk" ] && echo yes || echo no)"
expect "local-setup.sh refuses a machine without a needed tool" 1 \
  "$(LOCAL_SETUP_TOOLS="curl no-such-tool-t069" ls_run "$ls_dir/home-tools" --check)"
expect "local-setup.sh names the missing tool" 1 "$(grep -c '^LỖI: máy thiếu công cụ: no-such-tool-t069' "$ls_dir/out" || true)"
printf 'CMDLINE_TOOLS_ZIP="commandlinetools-linux-42_latest.zip"\n"${SDKMANAGER}" --install \\\n  "platform-tools" \\\n  "platforms;android-99"\n  "systemProp.org.gradle.internal.repository.max.retries=8" \\\n' \
  > "$ls_dir/cloud-setup-shape.sh"
expect "local-setup.sh refuses a package name that is not an SDK package" 1 \
  "$(LOCAL_SETUP_CLOUD_SETUP="$ls_dir/cloud-setup-shape.sh" ls_run "$ls_dir/home-shape" --print-versions)"
expect "local-setup.sh names the bad package" 1 "$(grep -c 'gói không hợp lệ: systemProp' "$ls_dir/out" || true)"
# The packages read from cloud-setup.sh are the ones cloud-setup.sh itself passes to sdkmanager: run the real
# cloud-setup.sh with a temp HOME and a fake sdkmanager already in place (so it downloads nothing).
mkdir -p "$ls_dir/home-cloudsetup/android-sdk/cmdline-tools/latest/bin"
printf '#!/bin/sh\n[ "$1" = --install ] && { shift; echo "$*" > "$HOME/install-args"; }\nexit 0\n' \
  > "$ls_dir/home-cloudsetup/android-sdk/cmdline-tools/latest/bin/sdkmanager"
chmod +x "$ls_dir/home-cloudsetup/android-sdk/cmdline-tools/latest/bin/sdkmanager"
HOME="$ls_dir/home-cloudsetup" bash scripts/cloud-setup.sh > /dev/null 2>&1 || true
expect "local-setup.sh installs exactly what cloud-setup.sh installs" \
  "$(cat "$ls_dir/home-cloudsetup/install-args" 2> /dev/null || echo 'cloud-setup.sh did not run')" \
  "$(scripts/local-setup.sh --print-versions 2> /dev/null | sed -n 's/^gói SDK: //p' || true)"
# A wrong checksum stops before anything is unpacked into place.
jq '.binaries[0].package.checksum = "0000"' "$ls_dir/jdk.json" > "$ls_dir/jdk.json.new" && mv "$ls_dir/jdk.json.new" "$ls_dir/jdk.json"
expect "local-setup.sh refuses a JDK with a wrong SHA-256" 1 "$(ls_run "$ls_dir/home-badjdk")"
expect "local-setup.sh names the SHA-256 mismatch" 1 "$(grep -c '^LỖI: mã SHA-256 của JDK không khớp' "$ls_dir/out" || true)"
expect "local-setup.sh leaves no JDK after a wrong SHA-256" no \
  "$([ -e "$(ls_prefix "$ls_dir/home-badjdk")/jdk/$ls_release" ] && echo yes || echo no)"
ls_jdk mac 21
jq '.binaries[0].package.link = "https://evil.example.test/jdk.tar.gz"' "$ls_dir/jdk.json" > "$ls_dir/jdk.json.new" && mv "$ls_dir/jdk.json.new" "$ls_dir/jdk.json"
expect "local-setup.sh refuses a JDK link outside github.com/adoptium" 1 "$(ls_run "$ls_dir/home-badlink")"
expect "local-setup.sh names the JDK link" 1 "$(grep -c '^LỖI: đường tải JDK không nằm ở https://github.com/adoptium/' "$ls_dir/out" || true)"
ls_jdk mac 21
ls_repo 0000000000000000000000000000000000000000 0000000000000000000000000000000000000000
expect "local-setup.sh refuses command-line tools with a wrong SHA-1" 1 "$(ls_run "$ls_dir/home-badtools")"
expect "local-setup.sh names the SHA-1 mismatch" 1 "$(grep -c '^LỖI: mã SHA-1 của commandlinetools-mac-' "$ls_dir/out" || true)"
expect "local-setup.sh leaves no command-line tools after a wrong SHA-1" no \
  "$([ -e "$ls_dir/home-badtools/android-sdk/cmdline-tools/latest" ] && echo yes || echo no)"
ls_repo "$ls_mac_sha1" "$ls_linux_sha1"
printf '<sdk/>\n' > "$ls_dir/repository.xml"
expect "local-setup.sh stops when Google's list has no checksum for the tools" 1 "$(ls_run "$ls_dir/home-nosum")"
ls_repo "$ls_mac_sha1" "$ls_linux_sha1"

# Linux x86_64: other URLs, JAVA_HOME at the top of the JDK.
ls_jdk linux 21
rm -f "$ls_dir/curl.log"
expect "local-setup.sh installs on Linux x86_64" 0 "$(LS_OS=Linux LS_ARCH=x86_64 ls_run "$ls_dir/home-linux")"
expect "local-setup.sh asks Adoptium for the linux x64 JDK" 1 "$(grep -c 'architecture=x64&image_type=jdk&os=linux' "$ls_dir/curl.log" || true)"
expect "local-setup.sh downloads the linux command-line tools" 1 \
  "$(grep -c "/commandlinetools-linux-${ls_tools}_latest.zip$" "$ls_dir/curl.log" || true)"
ls_linux_prefix="$(ls_prefix "$ls_dir/home-linux")"
expect "local-setup.sh writes JAVA_HOME at the top of the JDK on Linux" 1 \
  "$(grep -c "^export JAVA_HOME=\"$ls_linux_prefix/jdk/$ls_release\"$" "$ls_linux_prefix/env.sh" || true)"
expect "local-setup.sh refuses another system" 1 "$(LS_OS=FreeBSD ls_run "$ls_dir/home-bsd")"

# A JDK that java-check.sh does not accept is reported, not left silently in env.sh.
ls_jdk mac 26
expect "local-setup.sh stops when java-check.sh refuses the JDK" 1 "$(ls_run "$ls_dir/home-java26")"
expect "local-setup.sh names java-check.sh and the JDK dir to delete" 1 \
  "$(grep -c "^LỖI: scripts/java-check.sh không nhận JDK vừa cài. Xem lại JDK_RELEASE, xóa $(ls_prefix "$ls_dir/home-java26")/jdk/$ls_release" "$ls_dir/out" || true)"
ls_jdk mac 21
# --prefix and --sdk move the install.
expect "local-setup.sh honours --prefix and --sdk" 0 "$(ls_run "$ls_dir/home-custom" --prefix "$ls_dir/custom" --sdk "$ls_dir/custom-sdk")"
expect "local-setup.sh writes env.sh under --prefix" 1 "$(grep -c "^export ANDROID_HOME=\"$ls_dir/custom-sdk\"$" "$ls_dir/custom/env.sh" || true)"
# A --prefix or --sdk with characters that mean something inside double quotes (" $ ` \) is written to env.sh as
# it is: sourcing env.sh gives back the same paths and runs nothing.
ls_odd="$ls_dir/odd \"q\" \$HOME \`touch $ls_dir/ran\` back\\slash"
expect "local-setup.sh installs under a --prefix with quotes and dollars" 0 \
  "$(ls_run "$ls_dir/home-odd" --prefix "$ls_odd" --sdk "$ls_odd/sdk")"
expect "env.sh gives back a JAVA_HOME with quotes and dollars as it is" "$ls_odd/jdk/$ls_release/Contents/Home" \
  "$(set +e; (. "$ls_odd/env.sh" 2> /dev/null; printf '%s' "${JAVA_HOME:-}"))"
expect "env.sh gives back an ANDROID_HOME with quotes and dollars as it is" "$ls_odd/sdk" \
  "$(set +e; (. "$ls_odd/env.sh" 2> /dev/null; printf '%s' "${ANDROID_HOME:-}"))"
expect "env.sh runs no command hidden in the path" no "$([ -e "$ls_dir/ran" ] && echo yes || echo no)"

# plan-check.py finds, from the plan alone, what would otherwise stop a task during the night (L-020).
repo="$work/plan-repo"
mkdir -p "$repo/scripts" "$repo/docs/data" "$repo/docs/features/demo/plans" "$repo/docs/features/demo/tickets" \
  "$repo/docs/features/demo/design/screens" "$repo/feature/demo/src/test/screenshots" "$repo/core/x"
echo '{}' > "$repo/docs/data/values.json"
echo 'open("docs/data/values.json")' > "$repo/scripts/reader.py"
touch "$repo/feature/demo/A.kt" "$repo/core/x/B.kt" "$repo/docs/features/demo/design/screens/DemoScreen_main_light.png"
write_plan() { # <file> <risk> <status> <rows> [extra text]
  printf -- '---\nticket: %s\nstatus: %s\nrisk_level: %s\nreviewer: A\ncross_reviewer:\napproved_at: 2026-10-17\n---\n\n### 3.2. Danh sách file\n\n| Thao tác | Đường dẫn | Nội dung |\n| --- | --- | --- |\n%b\n\n### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện\n\n- **Quyền truy cập:** %s\n- **Thư viện và SDK:** Không thêm.\n\n%s\n\n%s\n\n## 4. Kiểm thử\n' \
    "$(basename "$1" | cut -d- -f1-2)" "$3" "$2" "$4" "${PERMISSIONS:-Không thêm.}" \
    "${CHANGED_BEHAVIOUR-### 3.5. Hành vi đã có bị đổi

Không có.}" "${5:-}" > "$1"
}
plan_check() { if PLAN_CHECK_ROOT="$repo" python3 -I scripts/plan-check.py "$1" > "$work/plan-check.out" 2>&1; then echo pass; else echo stop; fi; }
# One row of the file list of a plan; the backticks are Markdown, not command substitution.
# shellcheck disable=SC2016
row() { printf '| %s | `%s` | x |' "$1" "$2"; }
plans="$repo/docs/features/demo/plans"
write_plan "$plans/T-901-a.md" medium draft "$(row Sửa feature/demo/A.kt)"
expect "plan-check: a correct plan passes" pass "$(plan_check "$plans/T-901-a.md")"
# --touches: only a row of the file table of section 3.2 counts, never prose (ADR-0032, L8; PB-14).
touches() { python3 -I scripts/plan-check.py --touches "$@" > /dev/null 2>&1; echo $?; }
write_plan "$plans/T-961-a.md" high draft "$(row Sửa .github/workflows/ci.yml)"
expect "plan-check --touches: a Sửa row under the prefix -> 0" 0 "$(touches .github/workflows/ "$plans/T-961-a.md")"
write_plan "$plans/T-962-a.md" high draft "$(row Tạo .github/workflows/new.yml)\n$(row Sửa feature/demo/A.kt)"
expect "plan-check --touches: a Tạo row among others -> 0" 0 "$(touches .github/workflows/ "$plans/T-962-a.md")"
write_plan "$plans/T-963-a.md" medium draft "$(row Sửa feature/demo/A.kt)\n\nGhi chú: \`.github/workflows/ci.yml\` chỉ nhắc tới file này, không đọc."
expect "plan-check --touches: workflows named only in prose of section 3.2 -> 1" 1 "$(touches .github/workflows/ "$plans/T-963-a.md")"
expect "plan-check --touches: no such plan -> 2" 2 "$(touches .github/workflows/ "$plans/T-999-none.md")"
expect "plan-check --touches: an empty prefix -> 2" 2 "$(touches "" "$plans/T-961-a.md")"
expect "plan-check --touches: a missing plan argument -> 2" 2 "$(touches .github/workflows/)"
write_plan "$plans/T-964-a.md" high draft "$(row Xóa .github/workflows/old.yml)"
expect "plan-check --touches: a Xóa row under the prefix -> 0" 0 "$(touches .github/workflows/ "$plans/T-964-a.md")"
write_plan "$plans/T-965-a.md" medium draft "$(row Sửa docs/.github/workflows/x.md)"
expect "plan-check --touches: the prefix in the middle of a path -> 1" 1 "$(touches .github/workflows/ "$plans/T-965-a.md")"
write_plan "$plans/T-966-a.md" medium draft "$(row Sửa feature/demo/A.kt)" \
  "$(printf '### 3.4. Phương án dự phòng\n\n| Thao tác | Đường dẫn | Nội dung |\n| --- | --- | --- |\n%s' "$(row Sửa .github/workflows/ci.yml)")"
expect "plan-check --touches: a table outside section 3.2 -> 1" 1 "$(touches .github/workflows/ "$plans/T-966-a.md")"
write_plan "$plans/T-967-a.md" high draft "$(row Sửa .github/workflows/)"
expect "plan-check --touches: a directory row under the prefix counts -> 0" 0 "$(touches .github/workflows/ "$plans/T-967-a.md")"
write_plan "$plans/T-902-a.md" medium draft "$(row Sửa feature/demo/Missing.kt)"
expect "plan-check: changing a missing file stops" stop "$(plan_check "$plans/T-902-a.md")"
write_plan "$plans/T-903-a.md" medium draft "$(row Sửa docs/data/values.json)"
expect "plan-check: a script reading a changed file stops" stop "$(plan_check "$plans/T-903-a.md")"
expect "plan-check: names the reader" 1 "$(grep -c 'scripts/reader.py nhắc tới docs/data/values.json' "$work/plan-check.out")"
write_plan "$plans/T-904-a.md" medium draft "$(row Sửa docs/data/values.json)" 'scripts/reader.py chỉ đọc khóa a, không đổi.'
expect "plan-check: a reader the plan explains passes" pass "$(plan_check "$plans/T-904-a.md")"
# A reader that appears after the plan was approved (a new script naming the file in a comment) is only a warning:
# nobody may edit an approved plan, and the session runs plan-check again right before the task and skips plans
# with errors, so every approved plan of the list would be skipped (L-078).
write_plan "$plans/T-950-a.md" medium approved "$(row Sửa docs/data/values.json)"
expect "plan-check: a new reader of an approved plan only warns" pass "$(plan_check "$plans/T-950-a.md")"
expect "plan-check: the warning names the new reader" 1 \
  "$(grep -c 'CHÚ Ý: scripts/reader.py nhắc tới docs/data/values.json' "$work/plan-check.out")"
# A file name counts only as a whole word: setup-guide.md is not guide.md, docs/ROOT.md is not ROOT.md.
echo 'Read docs/ROOT.md, then setup-guide.md.' > "$repo/scripts/other.sh"
touch "$repo/ROOT.md" "$repo/docs/data/guide.md"
write_plan "$plans/T-916-a.md" medium draft "$(row Sửa ROOT.md)"
expect "plan-check: a root file is not docs/ROOT.md" pass "$(plan_check "$plans/T-916-a.md")"
write_plan "$plans/T-917-a.md" medium draft "$(row Sửa docs/data/guide.md)"
expect "plan-check: guide.md is not setup-guide.md" pass "$(plan_check "$plans/T-917-a.md")"
echo 'Read ROOT.md and data/guide.md.' > "$repo/scripts/other.sh"
expect "plan-check: a root file named alone is a mention" stop "$(plan_check "$plans/T-916-a.md")"
expect "plan-check: a base name after a folder is a mention" stop "$(plan_check "$plans/T-917-a.md")"
rm "$repo/scripts/other.sh"
write_plan "$plans/T-905-a.md" medium draft "$(row Sửa core/x/B.kt)"
expect "plan-check: core module needs a high-risk plan" stop "$(plan_check "$plans/T-905-a.md")"
write_plan "$plans/T-906-a.md" high approved "$(row Sửa core/x/B.kt)"
expect "plan-check: approved high-risk plan needs a cross reviewer" stop "$(plan_check "$plans/T-906-a.md")"
write_plan "$plans/T-907-a.md" medium draft "$(row Sửa feature/demo/A.kt)" 'Chạy scripts/design-compare.sh demo.'
expect "plan-check: design image without screenshot stops" stop "$(plan_check "$plans/T-907-a.md")"
write_plan "$plans/T-908-a.md" medium draft \
  "$(row Tạo feature/demo/src/test/screenshots/DemoScreen_main_light.png)" 'Chạy scripts/design-compare.sh demo.'
expect "plan-check: design image with its screenshot passes" pass "$(plan_check "$plans/T-908-a.md")"
write_plan "$plans/T-909-a.md" medium draft "$(row Tạo feature/demo/New.kt)"
printf -- '---\nid: T-910\ndepends_on: [T-909]\n---\n' > "$repo/docs/features/demo/tickets/T-910-a.md"
write_plan "$plans/T-910-a.md" medium draft "$(row Sửa feature/demo/New.kt)"
expect "plan-check: a file created by a dependency passes" pass "$(plan_check "$plans/T-910-a.md")"
write_plan "$plans/T-911-a.md" high draft "$(row Sửa scripts/release.sh)"
expect "plan-check: release.sh is never allowed" stop "$(plan_check "$plans/T-911-a.md")"
decision_v1=$'## 9. Quyết định duyệt\n\nPhiên bản 1:\n\n- **Quyết định:** Duyệt\n- **Người duyệt:** A'
blank_decision='- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện'
write_plan "$plans/T-912-a.md" medium approved "$(row Sửa feature/demo/A.kt)" \
  $'## 9. Quyết định duyệt\n\n'"$blank_decision"
expect "plan-check: approved plan with a blank decision stops" stop "$(plan_check "$plans/T-912-a.md")"
write_plan "$plans/T-913-a.md" medium approved "$(row Sửa feature/demo/A.kt)" \
  "$decision_v1"$'\n\nPhiên bản 2:\n\n'"$blank_decision"
expect "plan-check: blank decision of the latest version stops" stop "$(plan_check "$plans/T-913-a.md")"
write_plan "$plans/T-914-a.md" medium approved "$(row Sửa feature/demo/A.kt)" "$decision_v1"
expect "plan-check: approved plan with its decision passes" pass "$(plan_check "$plans/T-914-a.md")"

expect "plan-check: a missing token estimate only warns" pass "$(plan_check "$plans/T-901-a.md")"
expect "plan-check: names the missing token estimate" 1 "$(grep -c 'thiếu token_estimate' "$work/plan-check.out")"
printf -- '---\nticket: T-915\nstatus: draft\nrisk_level: low\ntoken_estimate: 20M\n---\n\n### 3.2. Danh sách file\n\n| Thao tác | Đường dẫn | Nội dung |\n| --- | --- | --- |\n%s\n\n## 4. Kiểm thử\n' \
  "$(row Sửa feature/demo/A.kt)" > "$plans/T-915-a.md"
plan_check "$plans/T-915-a.md" > /dev/null
expect "plan-check: a token estimate removes the warning" 0 "$(grep -c 'token_estimate' "$work/plan-check.out")"
# A new technique (criterion E from 2) needs a trial run with scripts/check.sh before review (L-042).
novel='| E. Độ mới của kỹ thuật | 2 | x |'
write_plan "$plans/T-919-a.md" medium draft "$(row Sửa feature/demo/A.kt)" "$novel"
expect "plan-check: a draft plan with a new technique and no trial run stops" stop "$(plan_check "$plans/T-919-a.md")"
expect "plan-check: names the missing trial run" 1 "$(grep -c 'mục 4.1. Chạy thử' "$work/plan-check.out")"
write_plan "$plans/T-920-a.md" medium draft "$(row Sửa feature/demo/A.kt)" \
  "$novel"$'\n\n### 4.1. Chạy thử\n\n- **Lệnh:** `scripts/check.sh`\n'
expect "plan-check: a draft plan with its trial run passes" pass "$(plan_check "$plans/T-920-a.md")"
write_plan "$plans/T-921-a.md" medium approved "$(row Sửa feature/demo/A.kt)" "$novel"$'\n\n'"$decision_v1"
expect "plan-check: an approved plan without a trial run only warns" pass "$(plan_check "$plans/T-921-a.md")"
# Permissions and libraries are decided while planning, not found during the session (rule 8).
printf '<manifest>\n<uses-permission android:name="android.permission.CAMERA" />\n</manifest>\n' > "$repo/feature/demo/AndroidManifest.xml"
mkdir -p "$repo/app/src/main" "$repo/gradle"
printf '<manifest>\n<uses-permission android:name="android.permission.CAMERA" />\n</manifest>\n' > "$repo/app/src/main/AndroidManifest.xml"
printf '[libraries]\nandroidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.0" }\n' > "$repo/gradle/libs.versions.toml"
printf -- '---\nticket: T-922\nstatus: draft\nrisk_level: medium\n---\n\n### 3.2. Danh sách file\n\n| Thao tác | Đường dẫn | Nội dung |\n| --- | --- | --- |\n| Sửa | `feature/demo/A.kt` | x |\n\n## 4. Kiểm thử\n' > "$plans/T-922-a.md"
expect "plan-check: a draft plan without the permission and library lines stops" stop "$(plan_check "$plans/T-922-a.md")"
write_plan "$plans/T-923-a.md" medium draft "$(row Sửa feature/demo/A.kt)" 'Gọi API cần `WRITE_EXTERNAL_STORAGE`.'
expect "plan-check: a new permission the plan does not decide stops" stop "$(plan_check "$plans/T-923-a.md")"
PERMISSIONS='`WRITE_EXTERNAL_STORAGE` (maxSdkVersion 28, spec KB-6).' write_plan "$plans/T-924-a.md" medium draft \
  "$(row Sửa feature/demo/A.kt)" 'Gọi API cần `WRITE_EXTERNAL_STORAGE`.'
expect "plan-check: an added permission needs a high-risk plan with the permission blocking point" stop "$(plan_check "$plans/T-924-a.md")"
PERMISSIONS='Không thêm: `READ_MEDIA_IMAGES`.' write_plan "$plans/T-925-a.md" medium draft \
  "$(row Sửa feature/demo/A.kt)" 'Không cần `READ_MEDIA_IMAGES`; `CAMERA` đã có.'
expect "plan-check: a permission the plan decides not to add passes" pass "$(plan_check "$plans/T-925-a.md")"
write_plan "$plans/T-926-a.md" medium draft "$(row Sửa feature/demo/A.kt)" 'Dùng `com.squareup.okhttp3:okhttp:4.12.0`.'
expect "plan-check: a library missing from the catalog stops" stop "$(plan_check "$plans/T-926-a.md")"
expect "plan-check: names the missing library" 1 "$(grep -c 'com.squareup.okhttp3:okhttp' "$work/plan-check.out")"
write_plan "$plans/T-927-a.md" medium draft "$(row Sửa feature/demo/A.kt)" 'Dùng `androidx.core:core-ktx` (`libs.androidx.core.ktx`).'
expect "plan-check: a library already in the catalog passes" pass "$(plan_check "$plans/T-927-a.md")"
# A plan smaller than its feature declares the design images another ticket draws; they need no screenshot here.
# shellcheck disable=SC2016
write_plan "$plans/T-928-a.md" medium draft "$(row Sửa feature/demo/A.kt)" \
  'Chạy scripts/design-compare.sh demo. Ảnh thiết kế của ticket khác (T-930): `DemoScreen_main_light`.'
expect "plan-check: a design image of another ticket passes" pass "$(plan_check "$plans/T-928-a.md")"
# shellcheck disable=SC2016
write_plan "$plans/T-929-a.md" medium draft "$(row Sửa feature/demo/A.kt)" \
  'Chạy scripts/design-compare.sh demo. Ảnh thiết kế của ticket khác (T-930): `DemoScreen_typo_light`.'
expect "plan-check: an unknown image of another ticket stops" stop "$(plan_check "$plans/T-929-a.md")"
expect "plan-check: names the unknown image" 1 "$(grep -c 'DemoScreen_typo_light' "$work/plan-check.out")"
# shellcheck disable=SC2016
write_plan "$plans/T-930-a.md" medium draft "$(row Sửa feature/demo/A.kt)" \
  'Chạy scripts/design-compare.sh demo. Ảnh thiết kế của ticket khác: `DemoScreen_main_light`.'
expect "plan-check: images of another ticket without the ticket id stop" stop "$(plan_check "$plans/T-930-a.md")"
rm "$repo/feature/demo/AndroidManifest.xml" "$repo/app/src/main/AndroidManifest.xml" "$repo/gradle/libs.versions.toml"
# What scripts/check.sh refuses shows up in the plan (rule 9, L-050). The Kotlin quotes are plan text.
# shellcheck disable=SC2016
shot='Test gọi `captureRoboImage("src/test/screenshots/DemoNew.png")`.'
# shellcheck disable=SC2016
refused='Test gọi `captureRoboImage("DemoNew.png")`.'
write_plan "$plans/T-928-a.md" medium draft "$(row Sửa feature/demo/A.kt)" "$refused"
expect "plan-check: captureRoboImage outside src/test/screenshots stops" stop "$(plan_check "$plans/T-928-a.md")"
expect "plan-check: names the refused captureRoboImage" 1 "$(grep -c 'captureRoboImage("DemoNew.png")' "$work/plan-check.out")"
write_plan "$plans/T-929-a.md" medium draft "$(row Sửa feature/demo/A.kt)" \
  $'```kotlin\ncaptureRoboImage(\n    "src/test/screenshots/DemoNew.png",\n)\n```'
expect "plan-check: captureRoboImage with the path on the next line stops" stop "$(plan_check "$plans/T-929-a.md")"
write_plan "$plans/T-930-a.md" medium draft "$(row Sửa feature/demo/A.kt)" "$shot"
expect "plan-check: a new reference image missing from the file list stops" stop "$(plan_check "$plans/T-930-a.md")"
expect "plan-check: names the missing reference image" 1 "$(grep -c 'ảnh chuẩn DemoNew.png' "$work/plan-check.out")"
write_plan "$plans/T-931-a.md" medium draft \
  "$(row Sửa feature/demo/A.kt)"$'\n'"$(row Tạo feature/demo/src/test/screenshots/DemoNew.png)" "$shot"
expect "plan-check: a new reference image in the file list passes" pass "$(plan_check "$plans/T-931-a.md")"
touch "$repo/feature/demo/src/test/screenshots/DemoOld.png"
# shellcheck disable=SC2016
write_plan "$plans/T-932-a.md" medium draft "$(row Sửa feature/demo/A.kt)" \
  'Gọi `captureRoboImage("src/test/screenshots/DemoOld.png")` và `captureRoboImage("src/test/screenshots/<Tên>.png")`.'
expect "plan-check: an existing reference image and a placeholder pass" pass "$(plan_check "$plans/T-932-a.md")"
write_plan "$plans/T-933-a.md" medium draft "$(row Tạo feature/demo/src/test/DemoNew.png)"
expect "plan-check: a reference image outside src/test/screenshots stops" stop "$(plan_check "$plans/T-933-a.md")"
write_plan "$plans/T-934-a.md" medium approved "$(row Sửa feature/demo/A.kt)" \
  "$refused"$'\n\n'"$decision_v1"
expect "plan-check: an approved plan refused by check.sh only warns" pass "$(plan_check "$plans/T-934-a.md")"
expect "plan-check: warns about the refused captureRoboImage" 1 "$(grep -c 'CHÚ Ý: plan ghi captureRoboImage' "$work/plan-check.out")"
# The ticket's status follows the plan's (rule 10, L-050).
printf -- '---\nid: T-935\nstatus: planned\n---\n' > "$repo/docs/features/demo/tickets/T-935-a.md"
write_plan "$plans/T-935-a.md" medium approved "$(row Sửa feature/demo/A.kt)" "$decision_v1"
expect "plan-check: a ticket behind its approved plan only warns" pass "$(plan_check "$plans/T-935-a.md")"
expect "plan-check: points to status.py --sync" 1 "$(grep -c "ticket T-935 còn 'planned'. Chạy scripts/status.py --sync" "$work/plan-check.out")"
printf -- '---\nid: T-936\nstatus: approved\n---\n' > "$repo/docs/features/demo/tickets/T-936-a.md"
write_plan "$plans/T-936-a.md" medium draft "$(row Sửa feature/demo/A.kt)"
plan_check "$plans/T-936-a.md" > /dev/null
expect "plan-check: warns about a ticket approved before its plan" 1 "$(grep -c 'ticket T-936 đã approved khi plan còn draft' "$work/plan-check.out")"
printf -- '---\nid: T-937\nstatus: approved\n---\n' > "$repo/docs/features/demo/tickets/T-937-a.md"
write_plan "$plans/T-937-a.md" medium approved "$(row Sửa feature/demo/A.kt)" "$decision_v1"
plan_check "$plans/T-937-a.md" > /dev/null
expect "plan-check: a ticket in step with its plan gets no warning" 0 "$(grep -c 'ticket T-937' "$work/plan-check.out")"

# Rule 11 (L-085): every manual test row of section 4 says where it runs; an emulator row names a scenario
# file that exists and passes `device-scenario.py check`. An error for a draft plan, a warning for an approved one
# (camera-pose T-052).
mkdir -p "$repo/feature/demo/src/deviceScenarios" "$repo/feature/demo/src/main/res/values"
printf 'object DemoTags { const val TAG = "demo_tag" }\n' > "$repo/feature/demo/DemoTags.kt"
printf '<resources>\n    <string name="demo_text">Demo</string>\n</resources>\n' > "$repo/feature/demo/src/main/res/values/strings.xml"
printf 'ticket: T-940\ncriterion: Demo\ntimeout: 30\nwait id=demo_tag 5\nexpect str=demo_text\n' \
  > "$repo/feature/demo/src/deviceScenarios/T-940-good.txt"
printf 'ticket: T-941\ncriterion: Demo\ntimeout: 30\nfrobnicate id=demo_tag\n' > "$repo/feature/demo/src/deviceScenarios/T-941-bad.txt"
manual_rows() { # <plan> <test type cell>...: appends manual test rows to section 4 of a plan
  local plan="$1" cell
  shift
  printf '\n| Tiêu chí | Loại test | Cách kiểm tra |\n| --- | --- | --- |\n' >> "$plan"
  for cell in "$@"; do printf '| Mục | %s | x |\n' "$cell" >> "$plan"; done
}
rule11() { grep -c 'quy tắc 11' "$work/plan-check.out" || true; } # lines of rule 11 in the last plan-check output
write_plan "$plans/T-940-a.md" medium draft "$(row Sửa feature/demo/A.kt)"
manual_rows "$plans/T-940-a.md" 'Kiểm tra thủ công'
expect "plan-check: a manual row without where it runs is an error for a draft plan" "stop 1 1" \
  "$(plan_check "$plans/T-940-a.md") $(rule11) $(grep -c 'nơi chạy' "$work/plan-check.out" || true)"
write_plan "$plans/T-941-a.md" medium approved "$(row Sửa feature/demo/A.kt)" "$decision_v1"
manual_rows "$plans/T-941-a.md" 'Kiểm tra thủ công'
expect "plan-check: a manual row of an approved plan without where it runs is a warning too" "pass 1" \
  "$(plan_check "$plans/T-941-a.md") $(rule11)"
write_plan "$plans/T-942-a.md" medium draft "$(row Sửa feature/demo/A.kt)"
manual_rows "$plans/T-942-a.md" 'Kiểm tra thủ công (emulator: feature/demo/src/deviceScenarios/T-942-none.txt)'
expect "plan-check: an emulator row needs a scenario file in the file list or the repository" "stop 1" \
  "$(plan_check "$plans/T-942-a.md") $(grep -c 'quy tắc 11.*T-942-none.txt' "$work/plan-check.out" || true)"
write_plan "$plans/T-943-a.md" medium draft "$(row Sửa feature/demo/A.kt)"
manual_rows "$plans/T-943-a.md" 'Kiểm tra thủ công (emulator: feature/demo/src/deviceScenarios/T-941-bad.txt)'
expect "plan-check: a scenario file that fails check is an error for a draft plan" "stop 1" \
  "$(plan_check "$plans/T-943-a.md") $(grep -c 'quy tắc 11.*T-941-bad.txt dòng 4' "$work/plan-check.out" || true)"
write_plan "$plans/T-944-a.md" medium draft "$(row Sửa feature/demo/A.kt)"
manual_rows "$plans/T-944-a.md" 'Kiểm tra thủ công (máy thật: )' 'Kiểm tra thủ công (người:)'
expect "plan-check: máy thật and người need a reason" "stop 2" \
  "$(plan_check "$plans/T-944-a.md") $(grep -c 'quy tắc 11.*thiếu lý do' "$work/plan-check.out" || true)"
# Control: a correct plan has no line of rule 11. The file of the first row is only in the file list, not yet in the
# repository (a plan creates it), so only the rows that can be read are checked.
write_plan "$plans/T-945-a.md" medium draft "$(row Sửa feature/demo/A.kt)"$'\n'"$(row Tạo feature/demo/src/deviceScenarios/T-945-new.txt)"
manual_rows "$plans/T-945-a.md" \
  'Kiểm tra thủ công (emulator: feature/demo/src/deviceScenarios/T-945-new.txt, feature/demo/src/deviceScenarios/T-940-good.txt)' \
  'Kiểm tra thủ công (máy thật: camera thật)' 'Kiểm tra thủ công (người: đọc tài liệu)' 'Kiểm tra tự động (unit test)'
expect "plan-check: a correct plan has no line of rule 11 (the rule does run on T-940)" "pass 0 1" \
  "$(plan_check "$plans/T-945-a.md") $(rule11) $(plan_check "$plans/T-940-a.md" > /dev/null; rule11)"
# camera-pose T-052: rule 11 stops a draft plan, as an error, and only warns for an approved one, like rule 7.
write_plan "$plans/T-951-a.md" medium draft "$(row Sửa feature/demo/A.kt)"
manual_rows "$plans/T-951-a.md" 'Kiểm tra thủ công'
expect "plan-check: rule 11 stops a draft plan, as an error" "stop 1 0" \
  "$(plan_check "$plans/T-951-a.md") $(grep -c 'LỖI:.*quy tắc 11' "$work/plan-check.out" || true) $(grep -c 'CHÚ Ý:.*quy tắc 11' "$work/plan-check.out" || true)"
write_plan "$plans/T-952-a.md" medium approved "$(row Sửa feature/demo/A.kt)" "$decision_v1"
manual_rows "$plans/T-952-a.md" 'Kiểm tra thủ công'
expect "plan-check: control: rule 11 only warns for an approved plan" "pass 1 0" \
  "$(plan_check "$plans/T-952-a.md") $(grep -c 'CHÚ Ý:.*quy tắc 11' "$work/plan-check.out" || true) $(grep -c 'LỖI:.*quy tắc 11' "$work/plan-check.out" || true)"

# Rule 12 (L-088): TMPDIR is not set in a cloud session, so "$TMPDIR/x" writes to /x. A plan writes ${TMPDIR:-/tmp}.
# The dollar signs below are plan text, not shell expansion.
# shellcheck disable=SC2016
tmpdir_lines() { grep -c 'quy tắc 12' "$work/plan-check.out" || true; }
# shellcheck disable=SC2016
write_plan "$plans/T-946-a.md" medium draft "$(row Sửa feature/demo/A.kt)" 'Chạy `git show origin/main:x.sh > "$TMPDIR/x.sh"`.'
expect "plan-check: a draft plan with \$TMPDIR and no default stops" "stop 1" \
  "$(plan_check "$plans/T-946-a.md") $(grep -c 'LỖI: quy tắc 12' "$work/plan-check.out" || true)"
# shellcheck disable=SC2016
write_plan "$plans/T-947-a.md" medium draft "$(row Sửa feature/demo/A.kt)" 'Ghi vào `${TMPDIR}/out.txt`.'
expect "plan-check: a draft plan with \${TMPDIR} and no default stops" stop "$(plan_check "$plans/T-947-a.md")"
# shellcheck disable=SC2016
write_plan "$plans/T-948-a.md" medium approved "$(row Sửa feature/demo/A.kt)" 'Ghi vào `$TMPDIR/out.txt`.'$'\n\n'"$decision_v1"
expect "plan-check: an approved plan with \$TMPDIR only warns" "pass 1" \
  "$(plan_check "$plans/T-948-a.md") $(grep -c 'CHÚ Ý: quy tắc 12' "$work/plan-check.out" || true)"
# shellcheck disable=SC2016
write_plan "$plans/T-949-a.md" medium draft "$(row Sửa feature/demo/A.kt)" 'Ghi vào `${TMPDIR:-/tmp}/out.txt` và `$TMPDIR_OLD`.'
expect "plan-check: \${TMPDIR:-/tmp} passes" "pass 0" "$(plan_check "$plans/T-949-a.md") $(tmpdir_lines)"

# Rule 13 and scripts/existing-tests.py: a plan that changes Kotlin code says which existing tests check behaviour
# it changes (ADR-0031, L-106: T-041 stopped on an existing test that measured where the card sits).
CHANGED_BEHAVIOUR="" write_plan "$plans/T-951-a.md" medium draft "$(row Sửa feature/demo/A.kt)"
expect "plan-check: a draft plan changing Kotlin code without section 3.5 stops" \
  "stop 1" "$(plan_check "$plans/T-951-a.md") $(grep -c 'LỖI: quy tắc 13' "$work/plan-check.out" || true)"
CHANGED_BEHAVIOUR="" write_plan "$plans/T-952-a.md" medium approved "$(row Sửa feature/demo/A.kt)"
expect "plan-check: an approved plan without section 3.5 passes" \
  "pass 0" "$(plan_check "$plans/T-952-a.md") $(grep -c 'quy tắc 13' "$work/plan-check.out" || true)"
CHANGED_BEHAVIOUR="" write_plan "$plans/T-953-a.md" medium draft \
  "$(row Sửa docs/data/values.json)\n$(row Sửa feature/demo/src/test/kotlin/ATest.kt)" 'scripts/reader.py chỉ đọc khóa a.'
mkdir -p "$repo/feature/demo/src/test/kotlin" && touch "$repo/feature/demo/src/test/kotlin/ATest.kt"
expect "plan-check: a plan changing only data and tests needs no section 3.5" pass "$(plan_check "$plans/T-953-a.md")"

# Rule 14: a plan an auto-run session works keeps the lessons log, its own plan and its ticket out of section 3.2,
# because the session writes them through the report pull request (docs/auto-run.md, steps 7 and 10).
touch "$repo/docs/lessons.md"
printf -- '---\nid: T-971\nstatus: planned\n---\n' > "$repo/docs/features/demo/tickets/T-971-a.md"
write_plan "$plans/T-971-a.md" medium draft "$(row Sửa feature/demo/A.kt)\n$(row Sửa docs/lessons.md)"
expect "plan-check: an auto-run plan listing docs/lessons.md passes with a rule 14 warning" \
  "pass 1" "$(plan_check "$plans/T-971-a.md") $(grep -c 'CHÚ Ý: quy tắc 14: mục 3.2 có docs/lessons.md' "$work/plan-check.out" || true)"
printf -- '---\nid: T-972\nstatus: planned\n---\n' > "$repo/docs/features/demo/tickets/T-972-a.md"
write_plan "$plans/T-972-a.md" medium draft "$(row Sửa feature/demo/A.kt)\n$(row Sửa docs/features/demo/plans/T-972-a.md)\n$(row Sửa docs/features/demo/tickets/T-972-a.md)"
expect "plan-check: an auto-run plan listing its own plan and ticket gets one warning each" \
  "pass 1 1" "$(plan_check "$plans/T-972-a.md") $(grep -c 'quy tắc 14: mục 3.2 có docs/features/demo/plans/T-972-a.md' "$work/plan-check.out" || true) $(grep -c 'quy tắc 14: mục 3.2 có docs/features/demo/tickets/T-972-a.md' "$work/plan-check.out" || true)"
printf -- '---\nid: T-973\nstatus: planned\nauto_run: False    # people work it; any case, like run-check.py\n---\n' > "$repo/docs/features/demo/tickets/T-973-a.md"
write_plan "$plans/T-973-a.md" medium draft "$(row Sửa feature/demo/A.kt)\n$(row Sửa docs/lessons.md)\n$(row Sửa docs/features/demo/tickets/T-973-a.md)"
expect "plan-check: a plan whose ticket has auto_run: false may list the lessons log and its ticket" \
  "pass 0" "$(plan_check "$plans/T-973-a.md") $(grep -c 'quy tắc 14' "$work/plan-check.out" || true)"
printf -- '---\nid: T-974\nstatus: planned\n---\n' > "$repo/docs/features/demo/tickets/T-974-a.md"
write_plan "$plans/T-974-a.md" medium draft "$(row Sửa feature/demo/A.kt)\n$(row Sửa docs/lessons.md)"
perl -pi -e 's/^status: draft$/status: draft\nlane: quick/' "$plans/T-974-a.md"
expect "plan-check: a quick-lane plan may list the lessons log (one pull request for the task)" \
  "pass 0 1" "$(plan_check "$plans/T-974-a.md") $(grep -c 'quy tắc 14' "$work/plan-check.out" || true) $(grep -c '^lane: quick$' "$plans/T-974-a.md")"
write_plan "$plans/T-975-a.md" medium draft "$(row Sửa feature/demo/A.kt)\n$(row Sửa docs/lessons.md)"
expect "plan-check: a plan whose ticket file is missing still gets the rule 14 warning" \
  "pass 1" "$(plan_check "$plans/T-975-a.md") $(grep -c 'quy tắc 14: mục 3.2 có docs/lessons.md' "$work/plan-check.out" || true)"
write_plan "$plans/T-976-a.md" medium done "$(row Sửa feature/demo/A.kt)\n$(row Sửa docs/lessons.md)"
expect "plan-check: a done plan is left alone by rule 14" \
  "0" "$(plan_check "$plans/T-976-a.md" > /dev/null; grep -c 'quy tắc 14' "$work/plan-check.out" || true)"
expect "plan-check: a plan listing none of the session records has no rule 14 warning" \
  "pass 0" "$(plan_check "$plans/T-901-a.md") $(grep -c 'quy tắc 14' "$work/plan-check.out" || true)"

trepo="$work/tests-repo"
mkdir -p "$trepo/feature/demo/src/main/kotlin" "$trepo/feature/demo/src/test/kotlin" "$trepo/docs/plans" \
  "$trepo/feature/other/src/test/kotlin"
cat > "$trepo/feature/demo/src/main/kotlin/DemoScreen.kt" <<'KOTLIN'
object DemoTags {
    const val PAGER = "demo_pager"
    const val CARD = "demo_card"
}

@Composable
fun DemoScreen(modifier: Modifier) {
    fun localHelper() = Unit
    val every = 3
    Box(modifier.testTag(DemoTags.PAGER)) { Card(Modifier.testTag(DemoTags.CARD)) }
}

private fun hiddenHelper() = Unit
KOTLIN
cat > "$trepo/feature/demo/src/test/kotlin/DemoScreenTest.kt" <<'KOTLIN'
class DemoScreenTest {
    @Test
    fun card_belowPager() {
        assertTrue(bounds(DemoTags.CARD).top >= bounds(DemoTags.PAGER).bottom)
    }

    @Test
    fun card_exists() {
        assertTrue(exists(DemoTags.CARD))
    }

    @Test
    fun alreadyNamed_inPlan() {
        assertTrue(bounds(DemoTags.CARD).height > 0)
    }

    @Test
    fun unrelated() {
        val every = 3
        localHelper()
        hiddenHelper()
    }
}
KOTLIN
cat > "$trepo/feature/other/src/test/kotlin/OtherTest.kt" <<'KOTLIN'
class OtherTest {
    @Test
    fun otherCard_measured() {
        assertTrue(bounds(OtherTags.CARD).height > 0)
    }
}
KOTLIN
# Backticks are Markdown here, not command substitution.
# shellcheck disable=SC2016
printf '### 3.2. Danh sách file\n\n| Thao tác | Đường dẫn | Nội dung |\n| --- | --- | --- |\n| Sửa | `feature/demo/src/main/kotlin/DemoScreen.kt` | thẻ đè lên `PAGER` |\n| Sửa | `feature/demo/src/test/kotlin/DemoScreenTest.kt` | `alreadyNamed_inPlan` |\n\n## 4. Kiểm thử\n' \
  > "$trepo/docs/plans/T-1.md"
EXISTING_TESTS_ROOT="$trepo" python3 -I scripts/existing-tests.py "$trepo/docs/plans/T-1.md" > "$work/existing.out"
expect "existing-tests: the measuring test of a tag the plan names comes first" 1 \
  "$(sed -n '/^Cần xem trước/,/^Nên xem/p' "$work/existing.out" | grep -c 'DemoScreenTest.card_belowPager.*| có |')"
expect "existing-tests: a test the plan already names is only counted" "0 1" \
  "$(grep -c 'alreadyNamed_inPlan' "$work/existing.out" || true) $(grep -c '^Plan đã nhắc tới 1 test' "$work/existing.out")"
expect "existing-tests: a test using a tag the plan does not name is only counted" "0 1" \
  "$(grep -c 'DemoScreenTest.card_exists' "$work/existing.out" || true) $(grep -c '^Còn 1 test' "$work/existing.out")"
expect "existing-tests: local functions, plain vals, private functions and another object's CARD are not names" 0 \
  "$(grep -c 'unrelated\|OtherTest' "$work/existing.out" || true)"
EXISTING_TESTS_ROOT="$trepo" python3 -I scripts/existing-tests.py --all "$trepo/docs/plans/T-1.md" > "$work/existing.out"
expect "existing-tests: --all lists the test using a tag the plan does not name" 1 \
  "$(grep -c 'DemoScreenTest.card_exists' "$work/existing.out")"

# The latest run list must not get an error from rule 11 (the plans of a run list are approved, so rule 11 only warns).
# A new app has no run list until its first run (new-app.sh leaves out the template's), so it skips the test; the new
# app made above checks that finding none does not stop the tests (L-134).
latest_run_list() { find docs/runs -maxdepth 1 -name 'RUN-*.md' | sort | tail -n 1; }
latest_run="$(latest_run_list)"
if [[ -z "$latest_run" ]]; then
  echo "skip - run-check: repo chưa có danh sách chạy nào trong docs/runs/"
else
  python3 -I scripts/run-check.py "$latest_run" > "$work/run-check-latest.out" 2>&1 || true
  expect "run-check: the latest run list has no new error from rule 11" "0 yes" \
    "$(grep -c 'LỖI.*kiểm tra thủ công\|LỖI.*quy tắc 11' "$work/run-check-latest.out" || true) $([[ -s "$work/run-check-latest.out" ]] && echo yes || echo no)"
fi
if [[ -f scripts/new-app.sh && -d "$new_app/docs" ]]; then
  expect "run-check: a new app has no latest run list, and looking for it does not fail" ":0" \
    "$(cd "$new_app" && latest_run_list 2>&1; echo ":$?")"
fi

# The shell of the app exposes testTags to uiautomator in the main window and in the network dialog window.
expect "app: MainActivity and NetworkDialogHost call exposeTestTagsForUiAutomator" "1 1" \
  "$(grep -c 'exposeTestTagsForUiAutomator()' app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt || true) \
$(grep -c 'exposeTestTagsForUiAutomator()' app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/network/NetworkDialogHost.kt || true)"

# run-check.py checks a whole run list: specs confirmed and answered, dependencies in order, scenarios tested.
rr="$work/run-repo"
mkdir -p "$rr/docs/features/demo/tickets" "$rr/docs/features/demo/plans" "$rr/docs/runs" "$rr/feature/demo"
touch "$rr/feature/demo/A.kt" "$rr/feature/demo/B.kt"
write_spec() { # <status> <answer>
  printf -- '---\nstatus: %s\n---\n\n## 3. Kịch bản\n\n### KB-1. Một\n\n- [ ] Khi a thì b.\n\n## 9. Câu hỏi còn mở\n\n1. **Câu?**\n   - Trả lời: %s\n\n## 10. Lịch sử\n\n| 2026-10-01 | AI | Tạo |\n' \
    "$1" "$2" > "$rr/docs/features/demo/spec.md"
}
write_ticket() { # <id> <depends>
  printf -- '---\nid: %s\nstatus: approved\nspec: features/demo/spec.md\nscenarios: [KB-1]\ndepends_on: [%s]\n---\n' \
    "$1" "$2" > "$rr/docs/features/demo/tickets/$1-x.md"
}
write_run_plan() { # <id> <file> <test row> [operation] [text added to section 3] [text added as section 6]
  local section6=""
  [ -z "${6:-}" ] || section6="## 6. Ảnh hưởng chéo"$'\n\n'"$6"$'\n\n'
  printf -- '---\nticket: %s\nstatus: approved\nrisk_level: medium\nreviewer: A\napproved_at: 2026-10-02\n---\n\n## 3. Giải pháp\n\n### 3.2. Danh sách file\n\n| Thao tác | Đường dẫn | Nội dung |\n| --- | --- | --- |\n| %s | `%s` | x |\n\n%s\n\n## 4. Kiểm thử\n\n| Tiêu chí | Loại | Tên |\n| --- | --- | --- |\n%s\n\n%s## 9. Quyết định duyệt\n\n- **Quyết định:** Duyệt\n- **Người duyệt:** A\n' \
    "$1" "${4:-Sửa}" "$2" "${5:-}" "$3" "$section6" > "$rr/docs/features/demo/plans/$1-x.md"
}
write_run() { # <rows>
  printf -- '---\nstatus: draft\n---\n\n## 1. Danh sách chạy\n\n| Thứ tự | Ticket | Feature | Plan | Rủi ro | Phụ thuộc | Ghi chú |\n| --- | --- | --- | --- | --- | --- | --- |\n%b\n\n## 2. Kiểm tra\n' \
    "$1" > "$rr/docs/runs/RUN-1.md"
}
run_row() { printf '| %s | %s | demo | `features/demo/plans/%s-x.md` (1) | | | |\\n' "$1" "$2" "$2"; }
run_check() { if RUN_CHECK_ROOT="$rr" PLAN_CHECK_ROOT="$rr" python3 -I scripts/run-check.py "$rr/docs/runs/RUN-1.md" > "$work/run-check.out" 2>&1; then echo pass; else echo stop; fi; }
write_spec confirmed 'Đồng ý.'
write_ticket T-001 ''
write_ticket T-002 ''
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |'
write_run_plan T-002 feature/demo/B.kt '| KB-1 | Unit | `bTest` |'
write_run "$(run_row 1 T-001)$(run_row 2 T-002)"
expect "run-check: a ready list passes" pass "$(run_check)"
expect "run-check: a ready list has no warning" 0 "$(grep -c 'CHÚ Ý' "$work/run-check.out")"
# The pre-run review table also sits in section 1 and names tickets; only the run table lists tasks.
write_run "$(run_row 1 T-001)$(run_row 2 T-002)\n\n### Kiểm tra trước khi duyệt\n\n| Mức | Ticket | Vấn đề | Xử lý |\n| --- | --- | --- | --- |\n| Chặn | T-002 | x | y |"
expect "run-check: a review table naming a ticket is not a task" pass "$(run_check)"
write_run "$(run_row 1 T-001)$(run_row 2 T-002)"'\n\n### Kiểm tra trước khi duyệt\n\n| Mức | Ticket | Vấn đề | Xử lý |\n| --- | --- | --- | --- |\n| Nên sửa | T-002 | x | y |'
expect "run-check: the review table is not read as the list" pass "$(run_check)"
write_run "$(run_row 1 T-001)$(run_row 2 T-002)"
write_spec changed 'Đồng ý.'
expect "run-check: a spec waiting for confirmation stops" stop "$(run_check)"
write_spec confirmed ''
expect "run-check: an unanswered spec question stops" stop "$(run_check)"
expect "run-check: names the open question" 1 "$(grep -c "câu hỏi còn mở chưa có trả lời: 'Câu?'" "$work/run-check.out")"
write_spec confirmed 'Đồng ý.'
write_ticket T-001 'T-002'
expect "run-check: a dependency later in the list stops" stop "$(run_check)"
write_run "$(run_row 1 T-002)$(run_row 2 T-001)"
expect "run-check: a dependency earlier in the list passes" pass "$(run_check)"
expect "run-check: a single dependency chain warns" 1 "$(grep -c 'một chuỗi phụ thuộc' "$work/run-check.out")"
write_ticket T-001 ''
write_run_plan T-002 feature/demo/A.kt '| Khác | Unit | `bTest` |'
expect "run-check: a scenario without a test row stops" stop "$(run_check)"
write_run_plan T-002 feature/demo/A.kt '| KB-1 | Unit | `bTest` |'
expect "run-check: two independent plans changing one file warn" 1 "$(run_check > /dev/null; grep -c 'L-045' "$work/run-check.out")"
# Two plans that both create one file, even dependent ones: only one may create it, the other reuses it (L-054).
write_ticket T-002 'T-001'
write_run_plan T-001 feature/demo/New.kt '| KB-1 | Unit | `aTest` |' Tạo
write_run_plan T-002 feature/demo/New.kt '| KB-1 | Unit | `bTest` |' Tạo
write_run "$(run_row 1 T-001)$(run_row 2 T-002)"
expect "run-check: two plans creating one file warn" 1 "$(run_check > /dev/null; grep -c 'cùng ghi Tạo feature/demo/New.kt' "$work/run-check.out")"
write_run_plan T-002 feature/demo/New.kt '| KB-1 | Unit | `bTest` |' Sửa
expect "run-check: a later plan that changes the created file does not warn" 0 \
  "$(run_check > /dev/null; grep -c 'cùng ghi Tạo' "$work/run-check.out")"
write_ticket T-002 ''
# A ticket without a business spec (a script or process ticket whose requirement is a lesson) has no spec and no
# scenario; only a ticket that names scenarios needs a spec.
write_run_plan T-002 feature/demo/B.kt '| L-001 | Test script | `bTest` |'
printf -- '---\nid: T-002\nstatus: approved\nspec:                        # no business spec\nscenarios: []\ndepends_on: []\n---\n' \
  > "$rr/docs/features/demo/tickets/T-002-x.md"
expect "run-check: a ticket without spec and scenarios passes" pass "$(run_check)"
printf -- '---\nid: T-002\nstatus: approved\nspec:\nscenarios: [KB-1]\ndepends_on: []\n---\n' \
  > "$rr/docs/features/demo/tickets/T-002-x.md"
expect "run-check: a ticket with scenarios but no spec stops" stop "$(run_check)"
# A ticket that people do by hand (auto_run: false, for example scripts/release.sh) never goes into a run list.
printf -- '---\nid: T-002\nstatus: approved\nspec:\nscenarios: []\ndepends_on: []\nauto_run: false               # people do it\n---\n' \
  > "$rr/docs/features/demo/tickets/T-002-x.md"
expect "run-check: a ticket with auto_run false stops" stop "$(run_check)"
expect "run-check: auto_run false names the ticket" 1 "$(grep -c 'T-002: ticket ghi auto_run: false' "$work/run-check.out")"
write_ticket T-002 ''

# run-check.py reads a plan's order hints and the files of other plans of the list (L-078).
# write_run_plan's fifth argument goes into section 3 of the plan, the sixth is a section 6 line.
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa '' '- Đề xuất thứ tự chạy: T-001 trước T-002.'
write_run_plan T-002 feature/demo/B.kt '| KB-1 | Unit | `bTest` |'
write_run "$(run_row 1 T-002)$(run_row 2 T-001)"
expect "run-check: a plan's order hint broken by the list stops" stop "$(run_check)"
expect "run-check: the broken order hint names the pair and the lesson" 1 \
  "$(grep -c 'T-001 trước T-002.*L-078\|L-078.*T-001 trước T-002' "$work/run-check.out" || true)"
write_run "$(run_row 1 T-001)$(run_row 2 T-002)"
expect "run-check: a list following the order hint passes" pass "$(run_check)"
expect "run-check: a list following the order hint has no L-078 message" 0 "$(grep -c 'L-078' "$work/run-check.out" || true)"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |'
write_run_plan T-002 feature/demo/B.kt '| KB-1 | Unit | `bTest` |' Sửa '' '- Đề xuất thứ tự chạy: sau T-001.'
write_run "$(run_row 1 T-002)$(run_row 2 T-001)"
expect "run-check: a 'sau' order hint is read" stop "$(run_check)"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa '' '- Đề xuất thứ tự chạy: trong nhóm demo: T-001, T-002.'
write_run_plan T-002 feature/demo/B.kt '| KB-1 | Unit | `bTest` |'
expect "run-check: a group order hint is read" stop "$(run_check)"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa '' '- Đề xuất thứ tự chạy: T-002, T-003 không phụ thuộc T-001.'
expect "run-check: a list of tickets that is not an order is no hint" pass "$(run_check)"
write_run "$(run_row 1 T-002)$(run_row 2 T-001)"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa '' '- Đề xuất thứ tự chạy: có thể chạy trước hay sau T-002.'
expect "run-check: 'trước hay sau' is no hint" pass "$(run_check)"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa '' '- Đề xuất thứ tự chạy: không cần T-001 trước T-002.'
expect "run-check: a negated order is no hint" pass "$(run_check)"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa '' '- Đề xuất thứ tự chạy: tự do; T-002 chạy sau T-001 thì gộp `A.kt`.'
expect "run-check: a condition is no hint" pass "$(run_check)"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa '' '- Đề xuất thứ tự chạy: tự do (bản trước ghi "T-001 trước T-002", đã bỏ).'
expect "run-check: a quoted old hint is no hint" pass "$(run_check)"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa '' '- Đề xuất thứ tự chạy: T-001 trước T-002.'
expect "run-check: the control hint in the same list still stops" stop "$(run_check)"
write_run "$(run_row 1 T-001)$(run_row 2 T-002)"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa '' '- Đề xuất thứ tự chạy: T-001 trước T-002.'
write_run "$(run_row 1 T-002)$(run_row 2 T-001)\n\nBỏ đề xuất thứ tự: T-001 trước T-002 (hai plan chỉ chung vài dòng)."
expect "run-check: a dropped order hint with a reason passes" pass "$(run_check)"
expect "run-check: a dropped order hint with a reason warns once" 1 "$(grep -c 'CHÚ Ý.*T-001 trước T-002' "$work/run-check.out" || true)"

# The order hint parser is conservative (review of PR 141): only a hint whose subject is the plan's own task stops
# the list; a hint about two other tasks is only a warning; negation, options and "hoặc" give no hint.
# Each case is a sentence in the plan of T-003, with a list order that the wrong reading would break.
write_ticket T-003 ''
touch "$rr/feature/demo/C.kt"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |'
write_run_plan T-002 feature/demo/B.kt '| KB-1 | Unit | `bTest` |'
plan3() { write_run_plan T-003 feature/demo/C.kt '| KB-1 | Unit | `cTest` |' Sửa '' "$1"; }
plan3 '- Đề xuất thứ tự chạy: T-002 chạy sau T-001.'
write_run "$(run_row 1 T-003)$(run_row 2 T-001)$(run_row 3 T-002)"
expect "run-check: 'B chạy sau A' in another plan is not about this plan" pass "$(run_check)"
expect "run-check: 'B chạy sau A' gives no pair with this plan" 0 "$(grep -c 'T-003' "$work/run-check.out" || true)"
write_run "$(run_row 1 T-002)$(run_row 2 T-001)$(run_row 3 T-003)"
expect "run-check: a hint about two other plans only warns" pass "$(run_check)"
expect "run-check: the warning reads 'B chạy sau A' as A before B" 1 \
  "$(grep -c 'CHÚ Ý.*T-001 trước T-002' "$work/run-check.out" || true)"
plan3 '- Đề xuất thứ tự chạy: T-001 chạy trước T-002.'
write_run "$(run_row 1 T-002)$(run_row 2 T-003)$(run_row 3 T-001)"
expect "run-check: 'A chạy trước B' in another plan does not stop" pass "$(run_check)"
expect "run-check: 'A chạy trước B' is not read as this plan before B" 0 \
  "$(grep -c 'T-003 trước T-002' "$work/run-check.out" || true)"
expect "run-check: 'A chạy trước B' is read as A before B" 1 "$(grep -c 'CHÚ Ý.*T-001 trước T-002' "$work/run-check.out" || true)"
write_run "$(run_row 1 T-003)$(run_row 2 T-001)$(run_row 3 T-002)"
plan3 '- Đề xuất thứ tự chạy: không nên chạy sau T-001.'
expect "run-check: 'không nên chạy sau' is no hint" pass "$(run_check)"
expect "run-check: 'không nên chạy sau' has no L-078 message" 0 "$(grep -c 'L-078' "$work/run-check.out" || true)"
plan3 '- Đề xuất thứ tự chạy: chưa cần chạy sau T-001.'
expect "run-check: 'chưa' is no hint" pass "$(run_check)"
write_run "$(run_row 1 T-002)$(run_row 2 T-003)$(run_row 3 T-001)"
plan3 '- Đề xuất thứ tự chạy: có thể sau T-001 hoặc trước T-002.'
expect "run-check: 'có thể ... hoặc ...' is no hint" pass "$(run_check)"
expect "run-check: 'có thể ... hoặc ...' has no L-078 message" 0 "$(grep -c 'L-078' "$work/run-check.out" || true)"
write_run "$(run_row 1 T-003)$(run_row 2 T-001)$(run_row 3 T-002)"
plan3 '- Thứ Tự Chạy: sau T-001.'
expect "run-check: the order label is read in any case" stop "$(run_check)"
plan3 '- Đề xuất thứ tự chạy: sau T-001, T-002.'
write_run "$(run_row 1 T-001)$(run_row 2 T-003)$(run_row 3 T-002)"
expect "run-check: 'sau A, B' reads every task of the list" stop "$(run_check)"
expect "run-check: 'sau A, B' names the second task" 1 "$(grep -c 'T-002 trước T-003' "$work/run-check.out" || true)"
plan3 '- Đề xuất thứ tự chạy: sau T-001 và T-002.'
expect "run-check: 'sau A và B' reads every task of the list" stop "$(run_check)"
# Positive control: the same list with the plan's own task as explicit subject stops.
plan3 '- Đề xuất thứ tự chạy: T-003 chạy sau T-002.'
expect "run-check: a hint with this plan as subject stops" stop "$(run_check)"
plan3 '- Đề xuất thứ tự chạy: T-003 chạy trước T-002.'
expect "run-check: a hint with this plan as subject that the list follows passes" pass "$(run_check)"
rm -f "$rr/docs/features/demo/tickets/T-003-x.md" "$rr/docs/features/demo/plans/T-003-x.md" "$rr/feature/demo/C.kt"

# Rule 9: a plan that deletes a file another plan after it in the list still names: that later plan fails
# plan-check.py at its start and is skipped. Section 3.2 is an error, a name elsewhere in sections 3, 4 a warning.
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Xóa
write_run_plan T-002 feature/demo/A.kt '| KB-1 | Unit | `bTest` |'
write_run "$(run_row 1 T-001)$(run_row 2 T-002)"
expect "run-check: a later plan listing a file an earlier plan deletes stops" "stop 1" \
  "$(run_check) $(grep -c 'LỖI: T-002 có `feature/demo/A.kt` ở mục 3.2 mà T-001 (đứng trước) xóa' "$work/run-check.out" || true)"
write_run "$(run_row 1 T-002)$(run_row 2 T-001)"
expect "run-check: the plan that deletes the file last passes" "pass 0" \
  "$(run_check) $(grep -c 'xóa' "$work/run-check.out" || true)"
write_run_plan T-002 feature/demo/B.kt '| KB-1 | Unit | `bTest` |' Sửa 'Đọc `feature/demo/A.kt` để so.'
write_run "$(run_row 1 T-001)$(run_row 2 T-002)"
expect "run-check: a later plan naming a deleted file outside section 3.2 warns" "pass 1" \
  "$(run_check) $(grep -c 'CHÚ Ý: T-002 nhắc `feature/demo/A.kt` mà T-001 (đứng trước) xóa' "$work/run-check.out" || true)"
write_run_plan T-002 feature/demo/A.kt '| KB-1 | Unit | `bTest` |' Tạo
expect "run-check: a later plan that creates the deleted file again only warns" "pass 1" \
  "$(run_check) $(grep -c 'CHÚ Ý: T-002 tạo lại `feature/demo/A.kt` mà T-001 (đứng trước) xóa' "$work/run-check.out" || true)"
write_run_plan T-001 feature/demo/ '| KB-1 | Unit | `aTest` |' Xóa
write_run_plan T-002 feature/demo/A.kt '| KB-1 | Unit | `bTest` |'
expect "run-check: a later plan listing a file in a folder an earlier plan deletes stops" "stop 1" \
  "$(run_check) $(grep -c 'LỖI: T-002 có `feature/demo/A.kt` ở mục 3.2 mà T-001 (đứng trước) xóa' "$work/run-check.out" || true)"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |'
write_run_plan T-002 feature/demo/B.kt '| KB-1 | Unit | `bTest` |'

# Rule 8: what the way of opening the session can do (L-112).
mkdir -p "$rr/docs/ops" "$rr/.github/workflows"
touch "$rr/.github/workflows/ci.yml"
cap_table() { # <cloud> <routine> <local>: the value of workflow-push for each way of opening
  printf -- '# x\n\n| `workflow-push` | `^docs/` | không | không | không | outside the markers |\n\n<!-- capabilities:start -->\n| Khả năng | Mẫu đường dẫn ở mục 3.2 | cloud | routine | local | Ghi chú |\n| --- | --- | --- | --- | --- | --- |\n| `workflow-push` | `^\\.github/workflows/` | %s | %s | %s | x |\n| `r2-network` | - | không | không | có | x |\n<!-- capabilities:end -->\n\n| Khả năng | Mẫu | cloud | routine | local | Ghi chú |\n| --- | --- | --- | --- | --- | --- |\n| `workflow-push` | `^docs/` | không | không | không | another table after the markers |\n' \
    "$1" "$2" "$3" > "$rr/docs/ops/session-capabilities.md"
}
open_with() { # <value>: sets open_with in the front matter of the run list written by write_run
  perl -0pi -e "s/^status: draft\n/status: draft\nopen_with: $1\n/m" "$rr/docs/runs/RUN-1.md"
}
high_risk() { # <id>: a plan that touches .github/ is high risk with a cross reviewer (risk-scoring.md, mục 1)
  perl -0pi -e 's/^risk_level: medium\n/risk_level: high\nblocking_points: [ci]\ncross_reviewer: B\n/m' "$rr/docs/features/demo/plans/$1-x.md"
}
write_ticket T-004 ''
write_run_plan T-004 .github/workflows/ci.yml '| KB-1 | Unit | `cTest` |'
high_risk T-004
cap_table thử thử có
write_run "$(run_row 1 T-001)$(run_row 2 T-004)"
open_with cloud
expect "run-check: a workflow plan in a cloud session that must probe passes" pass "$(run_check)"
expect "run-check: ... and warns that the session probes at its start" 1 \
  "$(grep -c "CHÚ Ý: T-004 cần 'workflow-push' (\`.github/workflows/ci.yml\` ở mục 3.2): phiên mở bằng cloud phải thử ở đầu phiên" "$work/run-check.out" || true)"
cap_table không thử có
expect "run-check: a workflow plan in a session that cannot push workflows stops" stop "$(run_check)"
expect "run-check: the error names the task, the capability, the path and the way of opening" 1 \
  "$(grep -c "LỖI: T-004 cần 'workflow-push' (\`.github/workflows/ci.yml\` ở mục 3.2) mà phiên mở bằng cloud không có" "$work/run-check.out" || true)"
expect "run-check: a plan without a workflow path gets no capability message" 0 "$(grep -c 'T-001 cần' "$work/run-check.out" || true)"
write_run "$(run_row 1 T-001)$(run_row 2 T-004)"
open_with local
expect "run-check: the same plan in a local session passes" pass "$(run_check)"
expect "run-check: a local session that can push workflows says nothing about it" 0 "$(grep -c 'workflow-push' "$work/run-check.out" || true)"
write_run "$(run_row 1 T-001)$(run_row 2 T-004)"
open_with cloud
expect "run-check: RUN_CHECK_OPEN_WITH overrides open_with" pass "$(RUN_CHECK_OPEN_WITH=local run_check)"
write_run "$(run_row 1 T-001)$(run_row 2 T-004)"
expect "run-check: a list without open_with whose plan needs a capability passes" pass "$(run_check)"
expect "run-check: ... and warns that open_with is missing" 1 "$(grep -c "CHÚ Ý: danh sách chưa ghi open_with.*nhận ''.*T-004" "$work/run-check.out" || true)"
open_with clould
expect "run-check: an unknown open_with is read as missing" 1 "$(run_check > /dev/null; grep -c "chưa ghi open_with.*nhận 'clould'" "$work/run-check.out" || true)"
write_run "$(run_row 1 T-001)$(run_row 2 T-002)"
expect "run-check: a list without open_with and without such plans has no open_with message" 0 \
  "$(run_check > /dev/null; grep -c 'open_with' "$work/run-check.out" || true)"
write_run_plan T-002 .github/workflows/ci.yml '| KB-1 | Unit | `bTest` |'
high_risk T-002
write_run "$(run_row 1 T-002)$(run_row 2 T-004)"
open_with cloud
expect "run-check: every plan that needs the missing capability is an error" 2 "$(run_check > /dev/null; grep -c "LỖI: T-00[24] cần 'workflow-push'" "$work/run-check.out" || true)"
write_run_plan T-002 feature/demo/B.kt '| KB-1 | Unit | `bTest` |'
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa 'Cách làm: xem `.github/workflows/ci.yml`, không sửa.'
write_run "$(run_row 1 T-001)"
open_with cloud
expect "run-check: a workflow path only in the prose of a plan needs nothing" "pass 0" "$(run_check) $(grep -c 'workflow-push' "$work/run-check.out" || true)"
printf -- '<!-- capabilities:start -->\n| Khả năng | Mẫu đường dẫn ở mục 3.2 | local | routine | cloud |\n| --- | --- | --- | --- | --- |\n| `workflow-push` | `^\\.github/workflows/` | có | có | không |\n| `broken` | `(` | có | có | có |\n<!-- capabilities:end -->\n' > "$rr/docs/ops/session-capabilities.md"
write_run "$(run_row 1 T-004)"
open_with cloud
expect "run-check: columns are read by their header" stop "$(run_check)"
expect "run-check: a pattern that does not compile is a warning" 1 "$(grep -c "CHÚ Ý: docs/ops/session-capabilities.md: mẫu của 'broken' không đọc được" "$work/run-check.out" || true)"
write_run "$(run_row 1 T-004)"
open_with local
expect "run-check: the same reordered table lets a local session pass" pass "$(run_check)"
cap_table không thử có
touch "$rr/docs/x.md"
write_run_plan T-001 docs/x.md '| KB-1 | Unit | `aTest` |'
write_run "$(run_row 1 T-001)"
open_with cloud
expect "run-check: a row outside the markers and a row without a pattern are not checked" pass "$(run_check)"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |'
cp docs/ops/session-capabilities.md "$rr/docs/ops/session-capabilities.md" 2> /dev/null || rm -f "$rr/docs/ops/session-capabilities.md"
write_run "$(run_row 1 T-004)"
open_with cloud
expect "run-check: the table of the repository asks a cloud session to probe workflow pushes" 1 \
  "$(run_check > /dev/null; grep -c "CHÚ Ý: T-004 cần 'workflow-push'.*cloud phải thử" "$work/run-check.out" || true)"
rm -f "$rr/docs/ops/session-capabilities.md"
write_run "$(run_row 1 T-004)"
open_with cloud
expect "run-check: without the table no capability is checked" "pass 0" "$(run_check) $(grep -c 'workflow-push' "$work/run-check.out" || true)"
rm -rf "$rr/docs/x.md" "$rr/docs/features/demo/tickets/T-004-x.md" "$rr/docs/features/demo/plans/T-004-x.md" "$rr/.github" "$rr/docs/ops"

# Files of other plans: a plan using a file that a later plan creates (and not in its own section 3.2).
git init -q "$rr"
mkdir -p "$rr/feature/other"
touch "$rr/feature/other/strings.xml"
git -C "$rr" add -A
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa 'Nối `Unfolded.kt` vào màn hình.'
write_run_plan T-002 feature/demo/Unfolded.kt '| KB-1 | Unit | `bTest` |' Tạo
write_run "$(run_row 1 T-001)$(run_row 2 T-002)"
expect "run-check: a plan using a file a later plan creates stops" stop "$(run_check)"
expect "run-check: names the file and the creating plan" 1 "$(grep -c 'nhắc `Unfolded.kt` mà T-002 (đứng sau) tạo' "$work/run-check.out" || true)"
write_run "$(run_row 1 T-002)$(run_row 2 T-001)"
expect "run-check: a later plan using a created file only warns" pass "$(run_check)"
expect "run-check: the warning says what is missing if the creating plan stops" 1 \
  "$(grep -c 'CHÚ Ý.*nếu T-002 dừng thì T-001 thiếu.*Unfolded.kt' "$work/run-check.out" || true)"
write_ticket T-001 'T-002'
run_check > /dev/null
expect "run-check: a plan depending on the creating plan gets no such warning" 0 "$(grep -c 'nếu T-002 dừng' "$work/run-check.out" || true)"
write_ticket T-001 ''
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa 'Dùng `strings.xml`.'
write_run_plan T-002 feature/demo/strings.xml '| KB-1 | Unit | `bTest` |' Tạo
write_run "$(run_row 1 T-001)$(run_row 2 T-002)"
run_check > /dev/null
expect "run-check: a common file name is no use" 0 "$(grep -c 'L-078' "$work/run-check.out" || true)"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa '' 'Plan kia có thể thêm `Unfolded.kt`, không ảnh hưởng.'
write_run_plan T-002 feature/demo/Unfolded.kt '| KB-1 | Unit | `bTest` |' Tạo
run_check > /dev/null
expect "run-check: a file named only in section 6 is no use" 0 "$(grep -c 'L-078' "$work/run-check.out" || true)"
rm -rf "$rr/.git"
write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |'
write_run_plan T-002 feature/demo/B.kt '| KB-1 | Unit | `bTest` |'
write_run "$(run_row 1 T-001)$(run_row 2 T-002)"

# plan-queue.py picks the tickets whose plan the session drafts at the end of a run (L-084).
pq="$work/pq"
mkdir -p "$pq/docs/features/demo/tickets" "$pq/docs/features/demo/plans"
pq_git() { GIT_AUTHOR_NAME=t GIT_AUTHOR_EMAIL=t@t GIT_COMMITTER_NAME=t GIT_COMMITTER_EMAIL=t@t git -C "$pq" "$@"; }
write_pq_spec() { # <file> <status> <answer>
  printf -- '---\nstatus: %s\n---\n\n## 3. Kịch bản\n\n### KB-1. Một\n\n- [ ] Khi a thì b.\n\n## 9. Câu hỏi còn mở\n\n1. **Câu?**\n   - Trả lời: %s\n\n## 10. Lịch sử\n\n| 2026-10-01 | AI | Tạo |\n' \
    "$2" "$3" > "$pq/docs/features/demo/$1"
}
write_pq_ticket() { # <id> <status> <spec> <scenarios> <depends> <sprint> <plan>
  printf -- '---\nid: %s\nstatus: %s\nspec: %s\nscenarios: [%s]\ndepends_on: [%s]\nsprint: %s\nplan: %s\n---\n' \
    "$1" "$2" "$3" "$4" "$5" "$6" "$7" > "$pq/docs/features/demo/tickets/$1-x.md"
}
write_pq_spec spec.md confirmed 'Đồng ý.'
write_pq_spec spec-draft.md draft 'Đồng ý.'
write_pq_spec spec-open.md confirmed ''
S=features/demo/spec.md
write_pq_ticket T-901 ready "$S" KB-1 '' 1 ''
write_pq_ticket T-902 ready "$S" KB-1 '' 2 ''
write_pq_ticket T-903 ready '' '' '' '' ''
write_pq_ticket T-904 ready '' '' '' '' features/demo/plans/T-904-x.md
write_pq_ticket T-905 ready features/demo/spec-draft.md KB-1 '' '' ''
write_pq_ticket T-906 ready features/demo/spec-open.md KB-1 '' '' ''
write_pq_ticket T-907 ready '' '' T-905 '' ''
write_pq_ticket T-908 ready '' '' T-904 '' ''
write_pq_ticket T-909 approved '' '' '' '' ''
write_pq_ticket T-910 ready "$S" KB-1 '' 1 ''
write_pq_ticket T-911 ready "$S" KB-1 '' 10 ''
write_pq_ticket T-912 ready "$S" KB-1 '' beta ''
write_pq_ticket T-913 ready '' '' T-903 '' ''
write_pq_ticket T-914 stopped '' '' '' '' ''
write_pq_ticket T-915 ready '' '' T-914 '' ''
write_pq_ticket T-916 ready '' '' T-999 '' ''
write_pq_ticket T-917 ready '' KB-1 '' '' ''
write_pq_ticket T-918 ready features/demo/missing.md KB-1 '' '' ''
write_pq_ticket T-919 ready '' '' '' '' ''
write_pq_ticket T-920 ready '' '' '' '' ''
write_pq_ticket T-921 stopped '' '' '' '' features/demo/plans/T-921-x.md
write_pq_ticket T-922 ready '' '' T-921 '' ''
write_pq_ticket T-923 backlog "$S" KB-1 '' '' ''
write_pq_ticket T-924 cancelled '' '' '' '' features/demo/plans/T-924-x.md
write_pq_ticket T-925 backlog '' '' T-924 '' ''
printf -- '---\nid: T-926\nstatus: backlog\nspec:\nscenarios: []\ndepends_on: []\nsprint:\nplan:\nauto_run: false               # people do it\n---\n' \
  > "$pq/docs/features/demo/tickets/T-926-x.md"
write_pq_ticket T-927 backlog '' '' T-926 '' ''
touch "$pq/README"
pq_git init -q -b main
pq_git add -A
pq_git commit -q -m init
pq_git update-ref refs/remotes/origin/claude/plan-T-919-x HEAD
pq_fixture='{"routes": {"pulls?state=open&per_page=100&page=1": [{"number": 77}], "pulls/77/files?per_page=100&page=1": [{"filename": "docs/features/demo/tickets/T-920-x.md"}]}}'
echo "$pq_fixture" > "$work/pq.json"
pq_run() { # <args...>; stdout of the script, its stderr goes to $work/pq.err
  PLAN_QUEUE_ROOT="${PQ_ROOT:-$pq}" PATH="$work/bin:$PATH" FIXTURE="${PQ_FIXTURE:-$work/pq.json}" \
    python3 -I scripts/plan-queue.py "$@" 2> "$work/pq.err"
}
pq_codes() { PLAN_QUEUE_ROOT="${PQ_ROOT:-$pq}" PATH="$work/bin:$PATH" FIXTURE="${PQ_FIXTURE:-$work/pq.json}" \
  python3 -I scripts/plan-queue.py "$@" > /dev/null 2>&1 && echo 0 || echo $?; }
pq_ids() { pq_run "$@" | cut -f1 | tr '\n' ' ' | sed 's/ $//'; }
all_ids="$(pq_ids --max 20)"
expect "plan-queue: order is sprint, then ticket number" "T-901 T-910 T-902 T-911 T-912 T-903 T-908 T-913 T-923" "$all_ids"
expect "plan-queue: an approved ticket is not queued" 0 "$(grep -c 'T-909' <<< "$all_ids" || true)"
expect "plan-queue: a ticket with a plan is not queued" 0 "$(grep -c 'T-904' <<< "$all_ids" || true)"
expect "plan-queue: a stopped ticket is not queued" 0 "$(grep -c 'T-914' <<< "$all_ids" || true)"
expect "plan-queue: a ticket without spec is queued" 1 "$(grep -c 'T-903' <<< "$all_ids" || true)"
expect "plan-queue: a dependency that has a plan is enough" 1 "$(grep -c 'T-908' <<< "$all_ids" || true)"
expect "plan-queue: a dependency queued earlier is enough" 1 "$(grep -c 'T-903 T-908 T-913' <<< "$all_ids" || true)"
pq_run --max 20 > /dev/null
pq_err() { grep -c "$1" "$work/pq.err" || true; }
expect "plan-queue: a draft spec is skipped with the reason" 1 "$(pq_err 'T-905: bỏ qua.*chưa confirmed')"
expect "plan-queue: an open spec question is skipped" 1 "$(pq_err 'T-906: bỏ qua.*câu hỏi còn mở')"
expect "plan-queue: a missing spec file is skipped" 1 "$(pq_err 'T-918: bỏ qua.*không có')"
expect "plan-queue: scenarios without spec are skipped" 1 "$(pq_err 'T-917: bỏ qua.*có kịch bản mà không có spec')"
expect "plan-queue: a dependency without plan skips the ticket" 1 "$(pq_err 'T-907: bỏ qua, chờ T-905')"
expect "plan-queue: a stopped dependency is skipped" 1 "$(pq_err 'T-915: bỏ qua.*T-914 đang stopped')"
expect "plan-queue: a stopped dependency with a plan is skipped" 1 "$(pq_err 'T-922: bỏ qua.*T-921 đang stopped')"
expect "plan-queue: a cancelled dependency with a plan is skipped" 1 "$(pq_err 'T-925: bỏ qua.*T-924 đang cancelled')"
expect "plan-queue: a backlog ticket is queued" 1 "$(grep -c 'T-923' <<< "$all_ids" || true)"
expect "plan-queue: a missing dependency is skipped" 1 "$(pq_err 'T-916: bỏ qua.*T-999 không có ticket')"
expect "plan-queue: a ticket with auto_run false is skipped" 1 "$(pq_err 'T-926: bỏ qua.*auto_run: false')"
expect "plan-queue: a ticket waiting for an auto_run false ticket is skipped" 1 "$(pq_err 'T-927: bỏ qua, chờ T-926')"
expect "plan-queue: a ticket with a plan branch is skipped" 1 "$(pq_err 'T-919: bỏ qua.*đang có plan trên nhánh origin/claude/plan-T-919-x')"
expect "plan-queue: a ticket in an open PR is skipped" 1 "$(pq_err 'T-920: bỏ qua.*đang có PR mở #77')"
expect "plan-queue: --max limits the list" "T-901 T-910" "$(pq_ids --max 2)"
expect "plan-queue: default is 2" "T-901 T-910" "$(pq_ids)"
expect "plan-queue: the line has the ticket path" "T-901 docs/features/demo/tickets/T-901-x.md" \
  "$(pq_run --max 1 | tr '\t' ' ')"
expect "plan-queue: skipped tickets do not change the exit code" 0 "$(pq_codes --max 20)"
touch "$work/pq.mark"; sleep 1
pq_run --max 20 > /dev/null
expect "plan-queue: writes nothing" "" "$(find "$pq" -newer "$work/pq.mark" -not -path '*/.git/*' -type f)"
mkdir -p "$work/pq2/docs/features/demo/tickets"
printf 'no front matter here\n' > "$work/pq2/docs/features/demo/tickets/T-930-x.md"
git -C "$work/pq2" init -q -b main
expect "plan-queue: a ticket without front matter exits 2" 2 "$(PQ_ROOT="$work/pq2" pq_codes)"
echo '{"routes": {}}' > "$work/pq-nogh.json"
printf -- '---\nid: X-1\nstatus: ready\n---\n' > "$work/pq2/docs/features/demo/tickets/T-931-x.md"
printf -- '---\nid: T-930\nstatus: ready\n---\n' > "$work/pq2/docs/features/demo/tickets/T-930-x.md"
expect "plan-queue: a ticket id that is not T-NNN exits 2" 2 "$(PQ_ROOT="$work/pq2" pq_codes)"
expect "plan-queue: a gh error exits 2" 2 "$(PQ_FIXTURE="$work/pq-nogh.json" pq_codes)"
echo '{"routes": {"pulls?state=open&per_page=100&page=1": []}}' > "$work/pq-empty.json"
expect "plan-queue: runs on the repository" 1 \
  "$([ "$(PQ_ROOT="$PWD" PQ_FIXTURE="$work/pq-empty.json" pq_codes --max 1)" != 2 ] && echo 1 || echo 0)"
expect "plan-queue: an ssh remote names the same repo" 0 \
  "$(GIT_CONFIG_VALUE_0=git@github.com:o/r.git pq_codes --max 1)"
expect "plan-queue: a remote that is not GitHub exits 2 without printing it" "2 0" \
  "$(GIT_CONFIG_VALUE_0=https://user:tok@mirror.example/o/r.git pq_codes) $(GIT_CONFIG_VALUE_0=https://user:tok@mirror.example/o/r.git pq_run > /dev/null; grep -c 'tok' "$work/pq.err" || true)"
# Pages are read with "&page=N", not --paginate (L-092): a PR on page 2 of the open PRs still holds its ticket.
python3 -I - "$work/pq-paged.json" << 'PY'
import json, sys
routes = {"pulls?state=open&per_page=100&page=1": [{"number": 1000 + n} for n in range(100)],
          "pulls?state=open&per_page=100&page=2": [{"number": 77}],
          "pulls/77/files?per_page=100&page=1": [{"filename": "docs/features/demo/tickets/T-920-x.md"}]}
routes.update({f"pulls/{1000 + n}/files?per_page=100&page=1": [] for n in range(100)})
json.dump({"routes": routes}, open(sys.argv[1], "w"))
PY
paged_ids="$(PQ_FIXTURE="$work/pq-paged.json" pq_ids --max 20)"
expect "plan-queue: a PR on page 2 of the open PRs still holds its ticket" "0 1" \
  "$(grep -c 'T-920' <<< "$paged_ids" || true) $(grep -c 'T-901' <<< "$paged_ids" || true)"

# token-usage.py counts tokens per task from transcripts, one usage per request, never failing the session.
tokens="$work/token-repo"
mkdir -p "$tokens/docs/features/demo/plans" "$tokens/docs/features/demo/tickets" "$work/projects/p/s/subagents"
printf -- '---\nsize: M\n---\n' > "$tokens/docs/features/demo/tickets/T-031-a.md"
printf -- '---\ntoken_estimate: 2k\n---\n' > "$tokens/docs/features/demo/plans/T-031-a.md"
usage_line() { # <request> <branch> <time> <output> [cwd]
  printf '{"type":"assistant","requestId":"%s","gitBranch":"%s","timestamp":"%s","cwd":"%s","message":{"usage":{"input_tokens":10,"cache_creation_input_tokens":100,"cache_read_input_tokens":1000,"output_tokens":%s}}}\n' \
    "$1" "$2" "$3" "${5:-$tokens}" "$4"
}
{
  usage_line r1 claude/T-031-loading 2026-10-18T01:00:00Z 5
  usage_line r1 claude/T-031-loading 2026-10-18T01:00:01Z 90
  usage_line r2 claude/RUN-20261018-01-report 2026-10-18T01:05:00Z 90
  usage_line r3 claude/T-031-loading 2026-10-17T01:00:00Z 90
  usage_line r4 claude/T-031-loading 2026-10-18T01:06:00Z 90 /elsewhere
  echo 'not json'
} > "$work/projects/p/s.jsonl"
usage_line r5 claude/fix-T-031-x 2026-10-18T01:10:00Z 90 > "$work/projects/p/s/subagents/agent-1.jsonl"
token_usage() { TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/projects" python3 -I scripts/token-usage.py "$@"; }
token_usage report --since 2026-10-18T00:00:00Z --run RUN-20261018-01 --csv docs/metrics/t.csv > "$work/tokens.out"
expect "token-usage: a task counts each request once, subagents and fix branch included" 1 \
  "$(grep -c '^| T-031 | M | 2k | 2k | 1.2x | 2 |' "$work/tokens.out")"
expect "token-usage: other branches are shared" 1 "$(grep -c '^| Chung (ngoài task) | - | - | 1k | - | 1 |' "$work/tokens.out")"
token_usage report --since '2026-10-18 00:00 UTC' --run RUN-20261018-01 --csv docs/metrics/t.csv > /dev/null
expect "token-usage: running again (started_at form) replaces the rows of the run" 3 "$(wc -l < "$tokens/docs/metrics/t.csv" | tr -d ' ')"
expect "token-usage: the csv keeps the estimate and the total" 1 \
  "$(grep -c '^RUN-20261018-01,2026-10-18,T-031,M,2000,2,20,200,2000,180,2400,$' "$tokens/docs/metrics/t.csv")"
# A subagent working in a worktree by path carries the report branch; its brief names the task on its own line (L-071).
mkdir -p "$work/projects-brief/p/s/subagents"
{
  printf '{"type":"user","message":{"role":"user","content":"Plan: x\\nTask: T-031\\nWorktree: ../task-T-031"}}\n'
  usage_line b1 claude/RUN-20261018-01-report 2026-10-18T01:12:00Z 90
} > "$work/projects-brief/p/s/subagents/agent-1.jsonl"
{ # Mentioning a task inside a sentence is not a task line.
  printf '{"type":"user","message":{"role":"user","content":[{"type":"text","text":"Review the run, see Task: T-031 notes"}]}}\n'
  usage_line b2 claude/RUN-20261018-01-report 2026-10-18T01:13:00Z 90
} > "$work/projects-brief/p/s/subagents/agent-2.jsonl"
usage_line b3 claude/RUN-20261018-01-report 2026-10-18T01:14:00Z 90 > "$work/projects-brief/p/s.jsonl"
TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/projects-brief" python3 -I scripts/token-usage.py report \
  --since 2026-10-18T00:00:00Z > "$work/tokens-brief.out"
expect "token-usage: a subagent whose brief has a Task line counts for that task" 1 \
  "$(grep -c '^| T-031 | M | 2k | 1k | 0.6x | 1 |' "$work/tokens-brief.out")"
expect "token-usage: a task named in passing and the main session stay shared" 1 \
  "$(grep -c '^| Chung (ngoài task) | - | - | 2k | - | 2 |' "$work/tokens-brief.out")"
expect "token-usage: no transcripts is not a failure" 0 \
  "$(TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/none" python3 -I scripts/token-usage.py report > /dev/null; echo $?)"
expect "token-usage: estimate falls back to the starting value" 1 \
  "$(token_usage estimate --csv docs/metrics/t.csv | grep -c '^| M | 1 | 2k | 15.0M (tạm')"
expect "token-usage: starting estimates are S 5M, M 15M, L 30M (L-021)" "5.0M 15.0M 30.0M" \
  "$(token_usage estimate --csv docs/metrics/none.csv | sed -n 's/^| [SML] | 0 | - | \([0-9.]*M\) (tạm.*/\1/p' | xargs)"
# A task continued in a later run is one sample, the sum of its rows (L-111).
printf 'run,date,task,size,estimate,requests,input,cache_write,cache_read,output,total,credit_usd\n%s\n%s\n%s\n' \
  'RUN-1,2026-10-01,T-001,M,,1,1,1,1,1,3000,' 'RUN-2,2026-10-02,T-001,M,,1,1,1,1,1,1000,' \
  'RUN-2,2026-10-02,T-002,M,,1,1,1,1,1,2000,' > "$tokens/docs/metrics/continued.csv"
expect "token-usage: a task measured in two runs counts once, with the sum" 1 \
  "$(token_usage estimate --csv docs/metrics/continued.csv | grep -c '^| M | 2 | 3k | 15.0M (tạm')"
# The cloud session credit of a run goes on its shared row, once, and survives measuring the run again (L-020).
expect "token-usage: credit needs a measured run" 1 "$(token_usage credit --run RUN-0 --usd 1 --csv docs/metrics/t.csv > /dev/null; echo $?)"
expect "token-usage: credit must be a number" 2 "$(token_usage credit --run RUN-20261018-01 --usd abc --csv docs/metrics/t.csv > /dev/null; echo $?)"
token_usage credit --run RUN-20261018-01 --usd 9.5 --csv docs/metrics/t.csv > /dev/null
expect "token-usage: credit is written on the shared row only" "1 0" \
  "$(grep -c '^RUN-20261018-01,2026-10-18,chung,.*,9.5$' "$tokens/docs/metrics/t.csv") $(grep -c '^RUN-20261018-01,2026-10-18,T-031,.*,9.5$' "$tokens/docs/metrics/t.csv")"
token_usage report --since 2026-10-18T00:00:00Z --run RUN-20261018-01 --csv docs/metrics/t.csv > /dev/null
expect "token-usage: measuring the run again keeps its credit" 1 "$(grep -c ',chung,.*,9.5$' "$tokens/docs/metrics/t.csv")"
expect "token-usage: estimate shows the credit per million tokens" 1 \
  "$(token_usage estimate --csv docs/metrics/t.csv | grep -c '^Credit đã dùng: 9.5 USD cho 4k token ở 1 phiên, khoảng 2638.89 USD mỗi 1M token.$')"
printf 'run,date,task,size,estimate,requests,input,cache_write,cache_read,output,total\nRUN-1,2026-10-01,T-001,S,,1,1,1,1,1,4\n' \
  > "$tokens/docs/metrics/old.csv"
token_usage credit --run RUN-1 --usd 3 --csv docs/metrics/old.csv > /dev/null
expect "token-usage: an old csv without the credit column is extended, rows kept" \
  "run,date,task,size,estimate,requests,input,cache_write,cache_read,output,total,credit_usd|RUN-1,2026-10-01,T-001,S,,1,1,1,1,1,4,|RUN-1,2026-10-01,chung,,,0,0,0,0,0,0,3" \
  "$(paste -sd '|' "$tokens/docs/metrics/old.csv")"
# Transcripts are found by the repository's name and by its git worktrees, not only by its folder (L-008).
{
  usage_line w1 claude/T-031-loading 2026-10-18T02:00:00Z 90 /home/user/token-repo
  usage_line w2 claude/T-031-loading 2026-10-18T02:00:00Z 90 /home/user/token-repo/app
  usage_line w3 claude/T-031-loading 2026-10-18T02:00:00Z 90 /home/user/token-repo-other
  usage_line w4 claude/T-031-loading 2026-10-18T02:00:00Z 90 /home/user/camera-pose
} > "$work/by-name.jsonl"
expect "token-usage: a checkout elsewhere with the repository's name is counted" 1 \
  "$(token_usage report --transcripts "$work/by-name.jsonl" | grep -c '^| T-031 | M | 2k | 2k | 1.2x | 2 |')"
expect "token-usage: TOKEN_USAGE_REPO sets the repository's name" 1 \
  "$(TOKEN_USAGE_REPO=camera-pose token_usage report --transcripts "$work/by-name.jsonl" | grep -c '^| T-031 | M | 2k | 1k | 0.6x | 1 |')"
wt_repo="$work/wt-main"
mkdir -p "$wt_repo"
git -C "$wt_repo" init -q
git -C "$wt_repo" -c user.name=t -c user.email=t@example.com commit -q --allow-empty -m init
git -C "$wt_repo" worktree add -q "$work/wt-report" 2> /dev/null
usage_line k1 claude/RUN-1-report 2026-10-18T02:00:00Z 90 "$work/wt-report" > "$work/worktree.jsonl"
expect "token-usage: a worktree of the repository is counted" 1 \
  "$(TOKEN_USAGE_ROOT="$wt_repo" python3 -I scripts/token-usage.py report --transcripts "$work/worktree.jsonl" | grep -c '^| Chung (ngoài task) | - | - | 1k | - | 1 |')"
# The main session marks the task it works on with `scripts/token-usage.py mark T-NNN` and ends it with
# `mark chung`: its own requests between the two count for the task, even on a detached HEAD (L-083 of camera-pose).
mark_line() { # <time> <command> [cwd]
  printf '{"type":"assistant","timestamp":"%s","cwd":"%s","message":{"content":[{"type":"tool_use","name":"Bash","input":{"command":"%s"}}]}}\n' \
    "$1" "${3:-$tokens}" "$2"
}
mkdir -p "$work/projects-mark/p/s/subagents"
{
  usage_line m0 HEAD 2026-10-18T02:59:00Z 90
  mark_line 2026-10-18T03:00:00Z 'cd /repo && scripts/token-usage.py mark T-031'
  usage_line m1 HEAD 2026-10-18T03:01:00Z 90
  usage_line m2 claude/T-032-other 2026-10-18T03:02:00Z 90
  usage_line m5 HEAD 2026-10-18T03:07:00Z 90
  mark_line 2026-10-18T03:08:00Z "git commit -m 'chạy scripts/token-usage.py mark T-035'"
  mark_line 2026-10-18T03:09:00Z "grep -En 'scripts/token-usage.py mark T-033|mark chung' docs/auto-run.md"
  usage_line m6 HEAD 2026-10-18T03:09:30Z 90
  mark_line 2026-10-18T03:10:00Z 'git checkout --detach origin/main; scripts/token-usage.py mark chung'
  usage_line m3 HEAD 2026-10-18T03:11:00Z 90
} > "$work/projects-mark/p/s.jsonl"
{
  mark_line 2026-10-18T03:05:00Z 'scripts/token-usage.py mark T-034'
  usage_line m4 HEAD 2026-10-18T03:06:00Z 90
} > "$work/projects-mark/p/s/subagents/agent-1.jsonl"
TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/projects-mark" python3 -I scripts/token-usage.py report \
  --since 2026-10-18T00:00:00Z > "$work/tokens-mark.out"
expect "token-usage: main-session requests after a mark count for the marked task" 1 \
  "$(grep -c '^| T-031 | M | 2k | 4k | 1.8x | 3 |' "$work/tokens-mark.out")"
expect "token-usage: before the first mark and after mark chung stay shared" 1 \
  "$(grep -c '^| Chung (ngoài task) | - | - | 4k | - | 3 |' "$work/tokens-mark.out")"
expect "token-usage: a request on a task branch keeps its branch's task during a mark" 1 \
  "$(grep -c '^| T-032 | - | - | 1k | - | 1 |' "$work/tokens-mark.out")"
expect "token-usage: a mark named in quotes or run by a subagent is not a mark" "0 0 0" \
  "$(grep -c '^| T-033 |' "$work/tokens-mark.out") $(grep -c '^| T-034 |' "$work/tokens-mark.out") $(grep -c '^| T-035 |' "$work/tokens-mark.out")"
mkdir -p "$work/projects-mark2/p"
{
  mark_line 2026-10-18T01:00:00Z 'scripts/token-usage.py mark T-031'
  usage_line n1 HEAD 2026-10-18T01:01:00Z 90
} > "$work/projects-mark2/p/s.jsonl"
usage_line n2 HEAD 2026-10-18T05:00:00Z 90 > "$work/projects-mark2/p/s2.jsonl"
TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/projects-mark2" python3 -I scripts/token-usage.py report \
  --since 2026-10-18T00:30:00Z > "$work/tokens-mark2.out"
expect "token-usage: a mark in an earlier transcript of the run still counts (resumed session)" "1 0" \
  "$(grep -c '^| T-031 | M | 2k | 2k | 1.2x | 2 |' "$work/tokens-mark2.out") $(grep -c '^| Chung' "$work/tokens-mark2.out")"
TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/projects-mark2" python3 -I scripts/token-usage.py report \
  --since 2026-10-18T04:00:00Z > "$work/tokens-mark3.out"
expect "token-usage: a mark before --since does not count (left open by an earlier session)" "0 1" \
  "$(grep -c '^| T-031 |' "$work/tokens-mark3.out") $(grep -c '^| Chung (ngoài task) | - | - | 1k | - | 1 |' "$work/tokens-mark3.out")"
mkdir -p "$work/projects-mark4/p"
{
  mark_line 2026-10-18T00:50:00Z 'scripts/token-usage.py mark T-036' /elsewhere
  usage_line q0 HEAD 2026-10-18T00:55:00Z 90
  mark_line 2026-10-18T01:00:00Z 'python3 /home/user/camera-pose/scripts/token-usage.py mark T-031'
  usage_line q1 HEAD 2026-10-18T01:01:00Z 90
} > "$work/projects-mark4/p/s.jsonl"
TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/projects-mark4" python3 -I scripts/token-usage.py report \
  --since 2026-10-18T00:00:00Z > "$work/tokens-mark4.out"
expect "token-usage: a mark run by an absolute path counts" 1 \
  "$(grep -c '^| T-031 | M | 2k | 1k | 0.6x | 1 |' "$work/tokens-mark4.out")"
expect "token-usage: a mark run in another folder is not a mark" 0 "$(grep -c '^| T-036 |' "$work/tokens-mark4.out")"
# Transcripts are read in file name order, not in time order: the marks of all of them are sorted by time.
mkdir -p "$work/projects-mark5/p"
mark_line 2026-10-18T03:00:00Z 'scripts/token-usage.py mark T-031' > "$work/projects-mark5/p/a.jsonl"
{
  mark_line 2026-10-18T02:00:00Z 'scripts/token-usage.py mark chung'
  usage_line r1 HEAD 2026-10-18T04:00:00Z 90
} > "$work/projects-mark5/p/b.jsonl"
TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/projects-mark5" python3 -I scripts/token-usage.py report \
  --since 2026-10-18T00:00:00Z > "$work/tokens-mark5.out"
expect "token-usage: the latest mark wins whatever the order of the transcript files" "1 0" \
  "$(grep -c '^| T-031 | M | 2k | 1k | 0.6x | 1 |' "$work/tokens-mark5.out") $(grep -c '^| Chung' "$work/tokens-mark5.out")"
expect "token-usage: mark prints the task and exits 0" "0 1" \
  "$(token_usage mark T-031 > "$work/mark.out"; echo $?) $(grep -c 'T-031' "$work/mark.out")"
expect "token-usage: mark with a name that is not T-NNN or chung exits 2 and says why" "2 1" \
  "$(token_usage mark T031 > "$work/mark-bad.out" 2>&1; echo $?) $(grep -c '^LỖI: ' "$work/mark-bad.out")"
# A line naming the command in an odd shape (not an object, a text message, a text input) is no mark and no failure.
mkdir -p "$work/projects-mark6/p"
{
  echo '["scripts/token-usage.py mark T-031"]'
  printf '{"type":"assistant","timestamp":"2026-10-18T03:00:00Z","cwd":"%s","message":"scripts/token-usage.py mark T-031"}\n' "$tokens"
  printf '{"type":"assistant","timestamp":"2026-10-18T03:00:00Z","cwd":"%s","message":{"content":[{"type":"tool_use","name":"Bash","input":"scripts/token-usage.py mark T-031"}]}}\n' "$tokens"
  usage_line s1 HEAD 2026-10-18T03:01:00Z 90
} > "$work/projects-mark6/p/s.jsonl"
expect "token-usage: a mark line of an odd shape is skipped without failing the report" "0 1" \
  "$(TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/projects-mark6" python3 -I scripts/token-usage.py report \
    --since 2026-10-18T00:00:00Z > "$work/tokens-mark6.out" 2>&1; echo $?) $(grep -c '^| Chung (ngoài task) | - | - | 1k | - | 1 |' "$work/tokens-mark6.out")"

# In a git checkout, plan-check.py only searches tracked files for readers: a copy of the repository in an untracked
# folder such as .claude/worktrees/ is not a reader of the changed file.
grepo="$work/plan-git"
mkdir -p "$grepo/docs/data" "$grepo/docs/features/demo/plans" "$grepo/scripts" "$grepo/.claude/worktrees/x/scripts"
echo '{}' > "$grepo/docs/data/values.json"
echo 'open("docs/data/values.json")' > "$grepo/.claude/worktrees/x/scripts/reader.py"
git -C "$grepo" init -q
git -C "$grepo" add docs/data/values.json
write_plan "$grepo/docs/features/demo/plans/T-940-a.md" medium draft "$(row Sửa docs/data/values.json)"
git_plan_check() { if PLAN_CHECK_ROOT="$grepo" python3 -I scripts/plan-check.py "$1" > "$work/plan-check.out" 2>&1; then echo pass; else echo stop; fi; }
expect "plan-check: an untracked copy under .claude/worktrees is not a reader" pass \
  "$(git_plan_check "$grepo/docs/features/demo/plans/T-940-a.md")"
cp "$grepo/.claude/worktrees/x/scripts/reader.py" "$grepo/scripts/reader.py"
git -C "$grepo" add scripts/reader.py
expect "plan-check: a tracked reader still stops" stop "$(git_plan_check "$grepo/docs/features/demo/plans/T-940-a.md")"

# status.py builds the progress page from the statuses in the documents.
st="$work/status-repo/docs"
mkdir -p "$st/features/demo/requests" "$st/features/demo/tickets" "$st/features/demo/plans" "$st/features/demo/feedback" \
  "$st/inbox" "$st/runs" "$st/adr"
printf -- '---\ndate: 2026-10-01\nfeature: demo\nstatus: specified\n---\n\n# YÊU CẦU: MÀN HÌNH DEMO\n' > "$st/features/demo/requests/2026-10-01-demo.md"
printf -- '---\ndate: 2026-10-02\nfeature:\nstatus: new\n---\n\n# YÊU CẦU: LOG CHO AI\n' > "$st/inbox/2026-10-02-log.md"
printf -- '---\nfeature: demo\nstatus: confirmed\n---\n' > "$st/features/demo/spec.md"
printf -- '---\ntitle: Một\nstatus: on-staging\n---\n' > "$st/features/demo/tickets/T-001-mot.md"
printf -- '---\ntitle: Hai\nstatus: approved\n---\n' > "$st/features/demo/tickets/T-002-hai.md"
printf -- '---\nstatus: done\nversion: 2\n---\n' > "$st/features/demo/plans/T-001-mot.md"
printf -- '---\nstatus: draft\nversion: 1\ntoken_estimate: 20M\n---\n' > "$st/features/demo/plans/T-002-hai.md"
printf -- '---\nid: FB-20261003-01\ntitle: Nút sai màu\nseverity: nhe\nstatus: new\n---\n' > "$st/features/demo/feedback/FB-20261003-01-nut.md"
printf -- '---\nid: RUN-20261004-01\nstatus: finished\nstarted_at: 2026-10-04 10:00 UTC\nstaging_build: 0.1.0-staging.1\n---\n\n## 1. Danh sách chạy\n\n| 1 | T-001 | demo |\n\n## 2. Khác\n' > "$st/runs/RUN-20261004-01.md"
printf -- '---\nid: ADR-0001\ntitle: A\nstatus: proposed\n---\n' > "$st/adr/0001-a.md"
printf -- '# SỔ\n\n### L-001. Một bài\n\n- **Trạng thái:** proposed\n' > "$st/lessons.md"
# Backticks of the page are matched with '.' in the patterns below.
status_page() { STATUS_ROOT="$work/status-repo" python3 -I scripts/status.py "$@"; }
expect "status: check fails before the page exists" 1 "$(status_page --check > /dev/null; echo $?)"
expect "status: check without the page asks to run status.py, not to commit" 0 \
  "$(status_page --check 2>&1 | grep -c 'commit' || true)"
status_page > /dev/null
page="$st/STATUS.md"
expect "status: request without spec waits for people" 1 "$(grep -c '^- Yêu cầu chưa có spec: .*(Log cho AI)' "$page")"
expect "status: draft plan, new feedback, proposed lesson and ADR wait for people" 4 \
  "$(grep -cE '^- (Plan .draft.|Phản hồi chờ phân loại|Bài học .proposed.|ADR .proposed.)' "$page")"
expect "status: request shows tickets done" 1 "$(grep -c '| demo | .specified. đã có spec | 1/2 |$' "$page")"
expect "status: ticket row has plan, estimate and runs" 1 \
  "$(grep -c '^| \[T-001\](features/demo/tickets/T-001-mot.md) | Một | .on-staging. | .done. (bản 2) | - | RUN-20261004-01 |$' "$page")"
expect "status: check passes on a fresh page" 0 "$(status_page --check > /dev/null; echo $?)"
# The lessons of an app may use "L-NNN:" and a status line with more fields; both forms are counted (L-057).
# A lesson number used twice (two branches adding lessons at once) stops the script (L-056).
cp "$st/lessons.md" "$work/lessons.bak"
printf -- '# SỔ\n\n### L-001. Một bài\n\n- **Trạng thái:** proposed\n\n### L-002: Bài của app\n\n- **Trạng thái:** accepted. **Loại:** quy trình.\n' > "$st/lessons.md"
status_page > /dev/null
expect "status: lessons written as L-NNN: are counted" 1 "$(grep -c 'lessons.md)): 2 bài; 1 .accepted., 1 .proposed.' "$page")"
printf -- '\n### L-002: Bài trùng số\n\n- **Trạng thái:** proposed\n' >> "$st/lessons.md"
expect "status: a lesson number used twice stops" 1 "$(status_page > "$work/status.out" 2>&1; echo $?)"
expect "status: names the duplicate lesson number" 1 "$(grep -c 'L-002' "$work/status.out")"
cp "$work/lessons.bak" "$st/lessons.md"
status_page > /dev/null
# The progress page is not in git on main: status-page.yml builds it after each push to main and pushes it to the
# status branch only, so PRs never conflict on it (L-066).
expect "status page: docs/STATUS.md is not tracked" "" "$(git ls-files docs/STATUS.md)"
expect "status page: git ignores docs/STATUS.md" 0 "$(git check-ignore -q docs/STATUS.md; echo $?)"
status_wf=.github/workflows/status-page.yml
expect "status-page.yml pushes only the status branch" "1 0" \
  "$(grep -c 'git push' "$status_wf" 2>/dev/null || echo 0) $(grep 'git push' "$status_wf" 2>/dev/null | grep -vc 'origin status$' || true)"
expect "status-page.yml builds the page with status.py before pushing" yes \
  "$(awk '/python3 scripts\/status.py/ { built = 1 } /git push/ { print (built ? "yes" : "no"); exit }' "$status_wf" 2>/dev/null)"
expect "status-page.yml runs on pushes to main that change docs" 2 \
  "$(grep -cE "^    branches: \[main\]$|^    paths: \['docs/\*\*'" "$status_wf" 2>/dev/null || true)"
expect "status page: README links the page on the status branch, not a repository name" 1 \
  "$(grep -c '](\.\./\.\./blob/status/docs/STATUS\.md)' README.md || true)"

# Section 6 of the page, "Sẵn sàng phát hành", is built from sources already in the repo (L-086):
# AdPlacement.kt, the "Cần người kiểm tra" part of finished run reports, tickets, feedback and the origin URL.
ad_dir="$work/status-repo/core/ads/src/main/kotlin/x"
mkdir -p "$ad_dir" "$work/status-repo/app/src/production"
write_ad_placement() {
  printf -- 'enum class AdPlacement(\n    val key: String,\n) {\n    HOME_BANNER("home_banner", AdFormat.BANNER),\n    LIST_NATIVE("list_native", AdFormat.NATIVE_SMALL, nativeEvery = 8),\n    GALLERY_INTERSTITIAL("gallery_interstitial", AdFormat.INTERSTITIAL),\n    DONE_INTERSTITIAL("done_interstitial", AdFormat.INTERSTITIAL),\n    ;\n}\n' > "$ad_dir/AdPlacement.kt"
}
write_ad_placement
printf -- 'val adUnit = "PROD-MARKER-T047"\n' > "$work/status-repo/app/src/production/ProdAds.kt"
printf -- 'admob.app.id=PROD-MARKER-T047\n' > "$work/status-repo/release.properties"
printf -- '\n### Cần người kiểm tra\n\n- [x] Đã xem ảnh\n  - a\n  - b\n- [ ] Kiểm FPS trên máy thật\n- [ ] Ảnh chuẩn mới: `feature/<tên>/src/test/screenshots/A.png`\n- [ ] Ảnh chuẩn screenshot mới: <màn hình, đường dẫn ảnh>\n- [ ] Test chập chờn: Không có (issue flaky không mở).\n- [ ] Ảnh chuẩn screenshot mới: không có (dừng trước bước ghi ảnh).\n- [ ] Tiêu chí kiểm tra thủ công (máy thật, người): không có\n- [ ] Kịch bản emulator lỗi hay bỏ qua: Không có\n- [ ] T-040: ghi chú, không có test cho trường hợp revert\n- [ ] Mũi tên <-> và so sánh a < b, c > d\n- [ ] Dòng rất dài chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ chữ `lệnh rất dài không được cắt giữa chừng` đuôi\n- [ ] Điểm rà soát mức ghi chú:\n  - x\n  - y\n\n### Task bị dừng\n\n- [ ] Không phải việc của mục kiểm\n' >> "$st/runs/RUN-20261004-01.md"
printf -- '---\nid: RUN-20261005-01\nstatus: running\n---\n\n### Cần người kiểm tra\n\n- [ ] Kiểm A\n' > "$st/runs/RUN-20261005-01.md"
printf -- '---\nid: RUN-20261006-01\nstatus: finished\n---\n\n## 3. Báo cáo\n\n### Cần người kiểm tra\n\n- [ ] Kiểm B\n' > "$st/runs/RUN-20261006-01.md"
printf -- '---\nid: RUN-20261006-02\nstatus: finished\n---\n\n### Cần người kiểm tra\n\n### Task bị dừng\n\n- [ ] Không phải việc của mục kiểm\n' > "$st/runs/RUN-20261006-02.md"
status_page > /dev/null
expect "status: page ends with the release readiness section" "1 5" \
  "$(grep -c '^## 6. Sẵn sàng phát hành$' "$page" || true) $(grep -c '^## [1-5]\. ' "$page" || true)"
expect "status: release readiness lists every ad placement key that ships, with attributes and NATIVE_SMALL" "1 1 0" \
  "$(grep -c '^- .*release.md.*.home_banner. (banner), .list_native. (native_small), .done_interstitial. (interstitial)\.$' "$page" || true) $(grep -c '^- Bản production cần một mã quảng cáo thật' "$page" || true) $(grep -c 'gallery_interstitial' "$page" || true)"
cp "$ad_dir/AdPlacement.kt" "$work/AdPlacement.kt.bak"
printf -- '    LATE_BANNER(\n        "late_banner",\n        AdFormat.BANNER,\n    ),\n' >> "$ad_dir/AdPlacement.kt"
status_page > /dev/null
expect "status: a placement wrapped over several lines is named, as release.sh refuses it" 1 \
  "$(grep -c 'khai báo vị trí trên nhiều dòng' "$page" || true)"
cp "$work/AdPlacement.kt.bak" "$ad_dir/AdPlacement.kt"
printf -- '    SPACE_BANNER("space_banner", AdFormat.BANNER), \n    CRLF_BANNER("crlf_banner", AdFormat.BANNER),\r\n' >> "$ad_dir/AdPlacement.kt"
status_page > /dev/null
expect "status: a placement line release.sh skips (trailing space, CRLF) is not listed either" "0 0" \
  "$(grep -c 'space_banner' "$page" || true) $(grep -c 'crlf_banner' "$page" || true)"
cp "$work/AdPlacement.kt.bak" "$ad_dir/AdPlacement.kt"
rm "$ad_dir/AdPlacement.kt"
status_page > /dev/null
expect "status: release readiness says when AdPlacement.kt is missing" 1 "$(grep -c 'Không tìm thấy' "$page" || true)"
write_ad_placement
status_page > /dev/null
expect "status: unchecked manual checks of finished runs are listed" 6 \
  "$(grep -c '^- \[RUN-20261004-01\](runs/RUN-20261004-01.md): ' "$page" || true)"
expect "status: <-> and a < b, c > d are not template placeholders" 1 "$(grep -c 'Mũi tên <-> và so sánh' "$page" || true)"
expect "status: a long item is cut before a code span, not inside it" "1 0" \
  "$(grep -c 'Dòng rất dài .*chữ\.\.\.$' "$page" || true) $(grep -c 'lệnh rất dài' "$page" || true)"
expect "status: manual checks are Kiểm FPS, the screenshot path, T-040 and the note point" "1 1 1 1" \
  "$(grep -c 'RUN-20261004-01.md): Kiểm FPS trên máy thật' "$page" || true) $(grep -c 'RUN-20261004-01.md): Ảnh chuẩn mới' "$page" || true) $(grep -c 'RUN-20261004-01.md): T-040: ' "$page" || true) $(grep -c 'RUN-20261004-01.md): Điểm rà soát' "$page" || true)"
expect "status: a path with <...> inside backticks is kept" 1 \
  "$(grep -c 'feature/<tên>/src/test/screenshots/A.png' "$page" || true)"
expect "status: a template line is skipped" "0 1" \
  "$(grep -c '<màn hình, đường dẫn ảnh>' "$page" || true) $(grep -c 'RUN-20261004-01.md): Kiểm FPS' "$page" || true)"
expect "status: children of a checked item are not counted" "0 1" \
  "$(grep 'Kiểm FPS' "$page" | grep -c 'dòng con' || true) $(grep -c 'Điểm rà soát mức ghi chú: (2 dòng con)$' "$page" || true)"
expect "status: only the text after the first colon decides \"không có\"" 1 \
  "$(grep -c 'T-040: ghi chú, không có test cho trường hợp revert$' "$page" || true)"
expect "status: items after the next heading and the done items are not listed" "0 0 0 6" \
  "$(grep -c 'Không phải việc' "$page" || true) $(grep -c 'Đã xem ảnh' "$page" || true) $(grep -c 'issue flaky' "$page" || true) $(grep -c '^- \[RUN-20261004-01\]' "$page" || true)"
expect "status: the run template still has the manual check section" yes \
  "$(awk '/^### Cần người kiểm tra$/ { s = 1; next } s && /^- \[ \] / { found = 1 } END { print (found ? "yes" : "no") }' docs/templates/run.md)"
expect "status: manual checks skip runs not finished" "0 1" \
  "$(grep -c 'Kiểm A' "$page" || true) $(grep -c 'RUN-20261006-01.md): Kiểm B' "$page" || true)"
expect "status: an empty or unterminated manual check section does not break the page" "0 1 0" \
  "$(status_page > /dev/null 2>&1; echo $?) $(grep -c 'RUN-20261006-01.md): Kiểm B' "$page" || true) $(status_page --check > /dev/null; echo $?)"
expect "status: tickets not on staging are listed by feature" "1 0 0" \
  "$(grep -c '^- demo: T-002 .approved.$' "$page" || true) $(grep '^- demo: ' "$page" | grep -c 'T-001' || true) $(grep -c 'gồm ticket quy trình' "$page" || true)"
mkdir -p "$st/features/proc/tickets"
printf -- '---\ntitle: Quy trình\nstatus: ready\n---\n' > "$st/features/proc/tickets/T-003-quy-trinh.md"
status_page > /dev/null
expect "status: a ticket of a feature without a spec marks process tickets" "1 1" \
  "$(grep -c 'gồm ticket quy trình' "$page" || true) $(grep -c '^- proc: T-003 .ready.$' "$page" || true)"
expect "status: open feedback is listed in release readiness" 1 \
  "$(grep -c '^- FB-20261003-01 .new., mức .nhe.: Nút sai màu$' "$page" || true)"
# Remote URL: the tests export a fake origin (https://github.com/o/r.git, L-068); outside a repository git still reads it.
expect "status: release readiness links open issues of the origin repo" "1 1" \
  "$(grep -cF 'https://github.com/o/r/issues?q=is%3Aissue+is%3Aopen)' "$page" || true) $(grep -cF 'label%3Amain-broken' "$page" || true)"
GIT_CONFIG_COUNT=0 GIT_CONFIG_GLOBAL=/dev/null GIT_CONFIG_NOSYSTEM=1 status_page > /dev/null
expect "status: without a readable remote there is a sentence and no link" "1 0" \
  "$(grep -c 'Không đọc được repo GitHub' "$page" || true) $(grep -c 'github.com/o/r' "$page" || true)"
GIT_CONFIG_VALUE_0=https://user:tok@github.com/o/r.git status_page > /dev/null
expect "status: a remote URL with credentials does not leak them" "1 0" \
  "$(grep -c 'https://github.com/o/r/issues' "$page" || true) $(grep -cw 'tok' "$page" || true)"
GIT_CONFIG_VALUE_0=git@github.com:o/r.git status_page > /dev/null
expect "status: an ssh remote gives the same link" 1 "$(grep -cF 'https://github.com/o/r/issues?q=' "$page" || true)"
GIT_CONFIG_VALUE_0=https://mirror.example/github.com/o/r.git status_page > /dev/null
expect "status: a remote that only has github.com in its path gives no link" "1 0" \
  "$(grep -c 'Không đọc được repo GitHub' "$page" || true) $(grep -c 'github.com/o/r/issues' "$page" || true)"
printf -- '---\nid: RUN-20261007-01\nstatus: finished\n---\n\n### Cần người kiểm tra\n\n- [ ] Kiểm C \xff\n' > "$st/runs/RUN-20261007-01.md"
expect "status: a report that is not UTF-8 stops the script, as any other document does" 1 \
  "$(status_page > /dev/null 2>&1; echo $?)"
rm "$st/runs/RUN-20261007-01.md"
printf -- '---\ntitle: Không spec\nspec:\nstatus: ready\n---\n' > "$st/features/demo/tickets/T-004-khong-spec.md"
status_page > /dev/null
expect "status: a ticket with an empty spec marks process tickets" 1 "$(grep -c 'gồm ticket quy trình' "$page" || true)"
rm "$st/features/demo/tickets/T-004-khong-spec.md"
status_page > /dev/null
status_page > /dev/null
# Nothing under app/src/production or release* is read: record every file the script opens (audit hook).
opened="$work/status-opened.txt"
STATUS_ROOT="$work/status-repo" OPENED="$opened" python3 -I -c '
import atexit, os, runpy, sys
seen = []
sys.addaudithook(lambda event, args: seen.append(args[0]) if event == "open" and isinstance(args[0], str) else None)
atexit.register(lambda: open(os.environ["OPENED"], "w").write("\n".join(seen) + "\n"))
sys.argv = ["scripts/status.py"]
runpy.run_path("scripts/status.py", run_name="__main__")
' > /dev/null
under_root="$(grep "^$work/status-repo/" "$opened" | sed "s|^$work/status-repo/||" || true)"
under_root="$(sort -u <<<"$under_root")"
expect "status: release readiness opens only docs and AdPlacement.kt" "0 0 1 1" \
  "$(grep -v -e '^docs/' -e '/AdPlacement.kt$' <<<"$under_root" | grep -c . || true) $(grep -v '^docs/' <<<"$under_root" | grep -c -e 'production' -e 'release' || true) $(grep -c '^docs/lessons.md$' <<<"$under_root" || true) $(grep -c '/AdPlacement.kt$' <<<"$under_root" || true)"
no_marker="$(grep -c PROD-MARKER-T047 "$page" || true)"
printf -- '- [ ] Dấu PROD-MARKER-T047 trong mục kiểm\n' >> "$st/runs/RUN-20261006-01.md"
status_page > /dev/null
expect "status: release readiness never reads production config" "0 1" \
  "$no_marker $(grep -c PROD-MARKER-T047 "$page" || true)"
perl -pi -e 's/^- \[ \] Kiểm FPS/- [x] Kiểm FPS/' "$st/runs/RUN-20261004-01.md"
status_page > /dev/null
expect "status: a checked item leaves the page" "0 1" \
  "$(grep -c 'Kiểm FPS' "$page" || true) $(grep -c 'RUN-20261004-01.md): T-040' "$page" || true)"
expect "status: check passes after the release readiness section" "0 1" \
  "$(status_page --check > /dev/null; echo $?) $(grep -c '^## 6. Sẵn sàng phát hành$' "$page" || true)"
# The page also builds from this repository's own docs, so a docs-only PR with a lesson number used twice is red in
# build-and-test, not only in status-page.yml after the merge (L-040). The number used twice is the first one of the
# log, so the copy sits far from the original, as when two branches add lessons at once (L-056); a new app starts with
# an empty log, so there the same lesson is added twice, as the new app made above checks (L-134).
lesson_number_used_twice() { # <copy of a repository>: prints the exit code of status.py and whether it names the number
  local log="$1/docs/lessons.md" number
  number="$(sed -n 's/^### \(L-[0-9]*\)[.:].*/\1/p' "$log" | sed -n 1p)"
  [[ -n "$number" ]] || { number=L-001; printf -- '\n### %s. Một\n\n- **Trạng thái:** proposed\n' "$number" >> "$log"; }
  printf -- '\n### %s. Trùng số\n\n- **Trạng thái:** proposed\n' "$number" >> "$log"
  echo "$(STATUS_ROOT="$1" python3 -I scripts/status.py > "$1.out" 2>&1; echo $?) $(grep -c "số bài học dùng hai lần: $number" "$1.out" || true)"
}
mkdir -p "$work/status-real"
cp -R docs "$work/status-real/docs"
expect "status: the progress page builds from this repository's docs" 0 \
  "$(STATUS_ROOT="$work/status-real" python3 -I scripts/status.py > "$work/status-real.out" 2>&1; echo $?)"
expect "status: this repository's docs with a lesson number used twice stop the page" "1 1" \
  "$(lesson_number_used_twice "$work/status-real")"
if [[ -f scripts/new-app.sh && -d "$new_app/docs" ]]; then
  mkdir -p "$work/status-new-app"
  cp -R "$new_app/docs" "$work/status-new-app/docs"
  expect "status: a new app's docs with a lesson number used twice stop the page" "1 1" \
    "$(lesson_number_used_twice "$work/status-new-app")"
fi


# status.py --sync moves each ticket's status after its plan's (L-050).
sy="$work/sync-repo/docs/features/demo"
mkdir -p "$sy/tickets" "$sy/plans"
sync_case() { # <id> <ticket status> <plan status>
  printf -- '---\ntitle: X\nstatus: %s   # backlog | ready | planned\n---\n\n# %s\n' "$2" "$1" > "$sy/tickets/$1-x.md"
  printf -- '---\nstatus: %s\nversion: 1\n---\n' "$3" > "$sy/plans/$1-x.md"
}
ticket_status() { sed -n 's/^status: \([a-z-]*\).*/\1/p' "$sy/tickets/$1-x.md"; }
sync_case T-001 planned approved
sync_case T-002 stopped approved
sync_case T-003 approved stopped
sync_case T-004 on-staging stopped
sync_case T-005 approved draft
sync_case T-006 approved approved
STATUS_ROOT="$work/sync-repo" python3 -I scripts/status.py --sync > "$work/sync.out"
expect "status --sync: approved plan moves a planned ticket to approved" approved "$(ticket_status T-001)"
expect "status --sync: approved new plan version moves a stopped ticket back to approved" approved "$(ticket_status T-002)"
expect "status --sync: stopped plan stops the ticket" stopped "$(ticket_status T-003)"
expect "status --sync: a ticket already on staging stays" on-staging "$(ticket_status T-004)"
expect "status --sync: a draft plan leaves the ticket for people" approved "$(ticket_status T-005)"
expect "status --sync: keeps the comment after the status" 6 "$(grep -c '   # backlog | ready | planned$' "$sy"/tickets/*.md | awk -F: '{s += $2} END {print s}')"
expect "status --sync: names each change" 3 "$(grep -c ' -> ' "$work/sync.out")"
expect "status --sync: writes the page" 0 "$(STATUS_ROOT="$work/sync-repo" python3 -I scripts/status.py --check > /dev/null; echo $?)"
STATUS_ROOT="$work/sync-repo" python3 -I scripts/status.py --sync > "$work/sync.out"
expect "status --sync: a second run changes nothing" 0 "$(grep -c ' -> ' "$work/sync.out")"

# foundation.py builds docs/foundation.md from the rule section of the ADRs.
fd="$work/foundation-repo/docs/adr"
mkdir -p "$fd"
printf -- '---\nid: ADR-0001\ntitle: Mẫu\nstatus: accepted\norigin: template\n---\n\n## Hệ quả\n\n- **Tích cực:** x\n- **Việc AI phải tuân thủ:** làm A (x; y); không làm `b; c`.\n' > "$fd/0001-mau.md"
printf -- '---\nid: ADR-0002\ntitle: Cũ\nstatus: superseded\norigin: template\n---\n\n- **Việc AI phải tuân thủ:** quy tắc cũ.\n' > "$fd/0002-cu.md"
printf -- '---\nid: ADR-1001\ntitle: Riêng\nstatus: accepted\norigin: app\n---\n\n- **Việc AI phải tuân thủ:** quy tắc app.\n' > "$fd/1001-rieng.md"
printf -- '---\nid: ADR-1002\ntitle: Đề xuất\nstatus: proposed\norigin: app\n---\n\n- **Phải chấp nhận:** y\n' > "$fd/1002-de-xuat.md"
foundation() { FOUNDATION_ROOT="$work/foundation-repo" python3 -I scripts/foundation.py "$@"; }
expect "foundation: check fails before the page exists" 1 "$(foundation --check > /dev/null; echo $?)"
foundation > /dev/null
fpage="$work/foundation-repo/docs/foundation.md"
expect "foundation: each clause is one rule, semicolons in code and in parentheses kept" 2 "$(grep -cE '^- (làm A \(x; y\)|không làm .b; c.)$' "$fpage")"
expect "foundation: superseded ADRs are left out" 0 "$(grep -c 'quy tắc cũ' "$fpage")"
expect "foundation: app ADRs are listed apart" 1 "$(grep -c '^## Quyết định riêng của app' "$fpage")"
expect "foundation: an ADR without rules points to the full ADR" 1 "$(grep -c 'ADR chưa có mục' "$fpage")"
expect "foundation: check passes on a fresh page" 0 "$(foundation --check > /dev/null; echo $?)"
expect "foundation: docs/foundation.md of this repo matches its ADRs" 0 \
  "$(python3 -I scripts/foundation.py --check > /dev/null; echo $?)"

# history-refs.py keeps lesson, ticket and run codes out of the rule documents (ADR-0044). Like new-app.sh it exists
# only in the template: an app's own documents name the app's own lessons, which are not broken.
if [[ ! -f scripts/history-refs.py ]]; then
  echo "skip - history-refs.py: chỉ có ở repo dự án mẫu"
else
  hr="$work/history-repo"
  mkdir -p "$hr/docs/adr" "$hr/docs/runs" "$hr/docs/features/demo/plans" "$hr/scripts/tests" "$hr/links/sub"
  printf -- '---\nid: ADR-0001\nstatus: accepted\n---\n\nBối cảnh: L-001.\n\n- **Việc AI phải tuân thủ:** làm A.\n' > "$hr/docs/adr/0001-a.md"
  printf -- '---\nid: ADR-0002\nstatus: superseded\n---\n\n- **Việc AI phải tuân thủ:** làm B (L-002).\n' > "$hr/docs/adr/0002-b.md"
  printf 'Quy tắc, vì một lý do (L-123).\n' > "$hr/docs/guide.md"
  printf 'Chữ giữ chỗ `L-NNN`, `RUN-YYYYMMDD-NN`, mã dòng PB-07, mã luật T-01.\n' > "$hr/docs/clean.md"
  printf 'Theo ADR-0002.\n' > "$hr/docs/mention.md"
  printf 'Báo cáo: L-001, T-001.\n' > "$hr/docs/runs/RUN-20261101-01.md"
  printf 'Các phiên từ RUN-20261015-01.\n' > "$hr/docs/runs/README.md"
  printf 'Plan T-002.\n' > "$hr/docs/features/demo/plans/T-002-x.md"
  history_refs() { HISTORY_REFS_ROOT="$hr" python3 -I scripts/history-refs.py "$@"; }
  code=0; out="$(history_refs)" || code=$?
  expect "history-refs: lists the codes of rule documents and a mention of a superseded ADR" "1 1 1 1" \
    "$code $(grep -cx '   docs/guide.md: L-123' <<< "$out") $(grep -cx '   docs/runs/README.md: RUN-20261015' <<< "$out") $(
      grep -cx '   docs/mention.md: ADR-0002' <<< "$out")"
  expect "history-refs: placeholders, playbook rows, run reports, plans, ADR context and superseded ADRs pass" 0 \
    "$(grep -cE 'clean\.md|RUN-20261101-01|T-002|ADR-0001|L-002' <<< "$out" || true)"
  printf '# not cleaned yet\ndocs/guide.md\ndocs/runs/README.md\ndocs/mention.md\n' > "$hr/scripts/tests/history-refs-allow.txt"
  expect "history-refs: files in the allow list pass" 0 "$(history_refs > /dev/null; echo $?)"
  echo docs/clean.md >> "$hr/scripts/tests/history-refs-allow.txt"
  code=0; out="$(history_refs)" || code=$?
  expect "history-refs: a clean file left in the allow list fails" "1 1" "$code $(grep -c 'docs/clean.md: đã sạch' <<< "$out")"
  printf 'docs/guide.md\ndocs/runs/README.md\ndocs/mention.md\n' > "$hr/scripts/tests/history-refs-allow.txt"
  printf -- '---\nid: ADR-0001\nstatus: accepted\n---\n\n- **Việc AI phải tuân thủ:** làm A (T-080).\n' > "$hr/docs/adr/0001-a.md"
  code=0; out="$(history_refs)" || code=$?
  expect "history-refs: checks the rule line of an ADR in force" "1 1" "$code $(grep -cx '   ADR-0001: T-080' <<< "$out")"
  printf 'x\n' > "$hr/links/sub/b.md"
  printf '[b](sub/b.md) [c](sub/b.md#muc) [m](missing.md) `[code](code.md)` [g](../../../blob/status/x.md) [w](https://e.vn/x.md)\n\n```\n[f](fenced.md)\n```\n' > "$hr/links/a.md"
  code=0; out="$(python3 -I scripts/history-refs.py --links "$hr/links")" || code=$?
  expect "history-refs --links: reports only a missing file inside the tree" "1 1 1" \
    "$code $(grep -c 'a.md: missing.md' <<< "$out") $(grep -c '^   ' <<< "$out")"
  expect "history-refs --mentions: lists rule documents that name a removed ADR" "   docs/mention.md: ADR-0002" \
    "$(python3 -I scripts/history-refs.py --mentions "$hr" ADR-0002)"
  mkdir -p "$hr/docs/features"
  printf 'Hướng dẫn (L-124).\n' > "$hr/docs/features/README.md"
  code=0; out="$(history_refs)" || code=$?
  expect "history-refs: a guide in docs/features that is not a spec, design or ticket is a rule document" "1 1" \
    "$code $(grep -cx '   docs/features/README.md: L-124' <<< "$out")"
  rm "$hr/docs/features/README.md"
  # A file that is not UTF-8 and a file in a merge conflict (one index entry per stage) are each listed once.
  cr="$work/history-conflict"
  git init -q -b main "$cr" && mkdir -p "$cr/docs"
  printf 'a\n' > "$cr/docs/x.md" && git -C "$cr" add -A && git -C "$cr" commit -q -m a
  git -C "$cr" switch -q -c other && printf 'b ADR-0005\n' > "$cr/docs/x.md" && git -C "$cr" commit -q -am b
  git -C "$cr" switch -q main && printf 'c ADR-0005\n' > "$cr/docs/x.md" && git -C "$cr" commit -q -am c
  git -C "$cr" merge -q other > /dev/null 2>&1 || true
  printf 'Theo ADR-0005, caf\xe9.\n' > "$cr/docs/latin.md"
  git -C "$cr" add docs/latin.md
  code=0; out="$(python3 -I scripts/history-refs.py --mentions "$cr" ADR-0005 2>&1)" || code=$?
  expect "history-refs --mentions: a conflicted file once, a file that is not UTF-8 read" "0 1 1" \
    "$code $(grep -cx '   docs/x.md: ADR-0005' <<< "$out") $(grep -cx '   docs/latin.md: ADR-0005' <<< "$out")"
  expect "history-refs: the rule documents of this repo are clean or in the allow list" 0 \
    "$(python3 -I scripts/history-refs.py > /dev/null; echo $?)"
fi

# secret-scan.py finds real-looking ad IDs and keys; Google test IDs and keys marked FAKE are allowed.
scan="$work/scan"
mkdir -p "$scan"
printf 'banner: ca-app-pub-3940256099942544/9214589741\nkey: AIzaSyFAKE_STAGING_KEY_NOT_REAL00000000\napp: ca-app-pub-%s~%s\n' 0000000000000000 0000000000 > "$scan/ok.md"
expect "secret-scan: Google test IDs, all-zero IDs and fake keys pass" 0 \
  "$(SECRET_SCAN_ROOT="$scan" python3 -I scripts/secret-scan.py > /dev/null; echo $?)"
# Built from pieces, so this file itself holds no real-looking ID for the repository scan below.
printf 'log: AdLoader init ca-app-pub-%s/%s ok\n' 5555555555555555 2222222222 > "$scan/report.md"
expect "secret-scan: a real-looking ad unit in a report fails" 1 \
  "$(SECRET_SCAN_ROOT="$scan" python3 -I scripts/secret-scan.py > /dev/null; echo $?)"
expect "secret-scan: the finding is masked" 0 \
  "$(SECRET_SCAN_ROOT="$scan" python3 -I scripts/secret-scan.py | grep -c '2222222222' || true)"
# An AppLovin SDK key is 86 characters; built from pieces so this file holds no key-like string (plan T-034).
applovin_piece="Ab3dEf6hIj"
applovin_key="$(printf "$applovin_piece%.0s" 1 2 3 4 5 6 7 8)Kl3nOp"
mkdir -p "$work/scan-applovin" "$work/scan-applovin-fake"
printf 'applovin.sdk.key=%s\n' "$applovin_key" > "$work/scan-applovin/strings.md"
expect "secret-scan: an AppLovin SDK key fails" "1 0" \
  "$(SECRET_SCAN_ROOT="$work/scan-applovin" python3 -I scripts/secret-scan.py > /dev/null; echo $?) $(
    SECRET_SCAN_ROOT="$work/scan-applovin" python3 -I scripts/secret-scan.py | grep -c "$applovin_key" || true)"
printf 'applovin_sdk_key: FAKE%sKl\n' "$(printf "$applovin_piece%.0s" 1 2 3 4 5 6 7 8)" \
  > "$work/scan-applovin-fake/strings.md"
expect "secret-scan: a fake AppLovin key passes" 0 \
  "$(SECRET_SCAN_ROOT="$work/scan-applovin-fake" python3 -I scripts/secret-scan.py > /dev/null; echo $?)"
# The AppLovin MAX SDK key is the same 86-character key, on lines named after the build parameter (plan T-068).
mkdir -p "$work/scan-max" "$work/scan-max-fake"
printf 'ads.maxSdkKey=%s\n' "$applovin_key" > "$work/scan-max/local.properties"
printf './gradlew bundleProductionRelease -PmaxSdkKey=%s\n' "$applovin_key" > "$work/scan-max/notes.md"
expect "secret-scan: a MAX SDK key fails" "1 2 0" \
  "$(SECRET_SCAN_ROOT="$work/scan-max" python3 -I scripts/secret-scan.py > /dev/null; echo $?) $(
    SECRET_SCAN_ROOT="$work/scan-max" python3 -I scripts/secret-scan.py | grep -c 'khóa SDK AppLovin' || true) $(
    SECRET_SCAN_ROOT="$work/scan-max" python3 -I scripts/secret-scan.py | grep -c "$applovin_key" || true)"
max_fake="FAKE$(printf "$applovin_piece%.0s" 1 2 3 4 5 6 7 8)Kl"
printf 'ads.maxSdkKey=%s\n' "$max_fake" > "$work/scan-max-fake/local.properties"
printf './gradlew bundleProductionRelease -PmaxSdkKey=%s\n' "$max_fake" > "$work/scan-max-fake/notes.md"
expect "secret-scan: a fake MAX SDK key passes" 0 \
  "$(SECRET_SCAN_ROOT="$work/scan-max-fake" python3 -I scripts/secret-scan.py > /dev/null; echo $?)"
# A MAX ad unit (16 hex characters on the Ad Units page of MAX) after a slot name, on a line about MAX ad units (plan
# T-071); built from pieces, so this file holds no unit-like string. Only the slot name is printed, never the unit.
max_unit="$(printf 'a1b2%.0s' 1 2 3 4)"
mkdir -p "$work/scan-max-units" "$work/scan-max-units-ok"
printf 'ads.maxAdUnits=banner=%s;banner_mrec=%s\n' "$max_unit" "$max_unit" > "$work/scan-max-units/local.properties"
printf './gradlew bundleProductionRelease -PmaxAdUnits="banner=%s"\n' "$max_unit" > "$work/scan-max-units/notes.md"
printf 'maxAdUnits ghi: banner=%s\n' "$max_unit" > "$work/scan-max-units/report.md"
printf 'val config = MaxConfig("k", "banner=%s")\n' "$max_unit" > "$work/scan-max-units/Sample.kt"
expect "secret-scan: a MAX ad unit fails and is masked" "1 5 0 0" \
  "$(SECRET_SCAN_ROOT="$work/scan-max-units" python3 -I scripts/secret-scan.py > /dev/null; echo $?) $(
    SECRET_SCAN_ROOT="$work/scan-max-units" python3 -I scripts/secret-scan.py | grep -c 'mã đơn vị MAX' || true) $(
    SECRET_SCAN_ROOT="$work/scan-max-units" python3 -I scripts/secret-scan.py | grep -c "$max_unit" || true) $(
    SECRET_SCAN_ROOT="$work/scan-max-units" python3 -I scripts/secret-scan.py | grep -c '=a1b2' || true)"
printf 'ads.maxAdUnits=banner=FAKE%s\n' "$max_unit" > "$work/scan-max-units-ok/local.properties"
printf '%s\n' '-PmaxAdUnits="banner=<MAX ad unit>;banner_mrec=<MAX ad unit>"' > "$work/scan-max-units-ok/build.md"
printf '%s\n' 'maxAdUnits = "banner=u1;banner_mrec=b2"' > "$work/scan-max-units-ok/Test.kt"
printf 'ghi: banner=%s\n' "$max_unit" > "$work/scan-max-units-ok/report.md"
expect "secret-scan: fake, placeholder and short MAX ad units pass" 0 \
  "$(SECRET_SCAN_ROOT="$work/scan-max-units-ok" python3 -I scripts/secret-scan.py > /dev/null; echo $?)"
expect "secret-scan: this repository has no real ad IDs or keys" 0 \
  "$(python3 -I scripts/secret-scan.py > /dev/null; echo $?)"

# env-canary.sh opens an env-broken issue when the environment is red and closes it when it is green again.
printf '#!/usr/bin/env bash\nexit 0\n' > "$work/pass.sh"
printf '#!/usr/bin/env bash\necho "Could not GET https://repo.maven.apache.org/x"\nexit 1\n' > "$work/fail.sh"
chmod +x "$work/pass.sh" "$work/fail.sh"
canary() { # <env check> <check> <open issues JSON>; prints the writes, one per line
  echo "{\"routes\": {\"issues?state=open&labels=env-broken&per_page=10\": $3}}" > "$work/canary.json"
  rm -f "$work/canary.json.writes"
  PATH="$work/bin:$PATH" FIXTURE="$work/canary.json" CANARY_CHECKOUT=no RETRY_WAIT_SECONDS=0 \
    ENV_CHECK_CMD="$1" CHECK_CMD="$2" scripts/env-canary.sh > /dev/null 2>&1 || true
  cat "$work/canary.json.writes" 2>/dev/null | tr '\n' ';'
}
expect "env-canary: red opens an env-broken issue" "POST issues;" "$(canary "$work/pass.sh" "$work/fail.sh" '[]')"
expect "env-canary: red again comments on the open issue" "POST issues/7/comments;" \
  "$(canary "$work/pass.sh" "$work/fail.sh" '[{"number": 7}]')"
expect "env-canary: old setup script is red" "POST issues;" "$(canary "$work/fail.sh" "$work/pass.sh" '[]')"
expect "env-canary: green closes the open issue" "POST issues/7/comments;PATCH issues/7;" \
  "$(canary "$work/pass.sh" "$work/pass.sh" '[{"number": 7}]')"
expect "env-canary: green with nothing open changes nothing" "" "$(canary "$work/pass.sh" "$work/pass.sh" '[]')"

# java-check.sh stops scripts/check.sh before Gradle when the Java is outside what the build and Gradle accept.
java_check() { # <java.specification.version printed by the fake java>; prints the exit code
  mkdir -p "$work/jdk-$1/bin"
  printf '#!/usr/bin/env bash\necho "Property settings:" >&2\necho "    java.specification.version = %s" >&2\n' "$1" \
    > "$work/jdk-$1/bin/java"
  chmod +x "$work/jdk-$1/bin/java"
  local code=0
  JAVA_HOME="$work/jdk-$1" scripts/java-check.sh > "$work/java.out" 2>&1 || code=$?
  echo "$code"
}
gradle_minor="$(sed -n 's/^distributionUrl=.*gradle-[0-9]*\.\([0-9]*\).*/\1/p' gradle/wrapper/gradle-wrapper.properties)"
expect "java-check: Java 17 passes" 0 "$(java_check 17)"
expect "java-check: Java 11 is too old" 1 "$(java_check 11)"
expect "java-check: names Android Studio's JDK" 1 "$(grep -c 'JDK đi kèm Android Studio' "$work/java.out")"
if [[ "$(sed -n 's/^distributionUrl=.*gradle-\([0-9]*\)\..*/\1/p' gradle/wrapper/gradle-wrapper.properties)" == 8 && "$gradle_minor" -ge 14 ]]; then
  expect "java-check: Java 26 is too new for Gradle 8.14" 1 "$(java_check 26)"
fi
# The Java bounds per Gradle version, on a copy of the script with a given wrapper (not the repo's Gradle).
java_check_gradle() { # <Gradle version> <Java version>; prints the exit code
  local dir="$work/java-gradle-$1"
  mkdir -p "$dir/scripts" "$dir/gradle/wrapper" "$dir/build-logic"
  cp scripts/java-check.sh "$dir/scripts/"
  printf 'distributionUrl=https\\://services.gradle.org/distributions/gradle-%s-bin.zip\n' "$1" \
    > "$dir/gradle/wrapper/gradle-wrapper.properties"
  printf 'val java = JavaVersion.VERSION_17\n' > "$dir/build-logic/KotlinAndroid.kt"
  java_check "$2" > /dev/null
  local code=0
  JAVA_HOME="$work/jdk-$2" "$dir/scripts/java-check.sh" > "$work/java.out" 2>&1 || code=$?
  echo "$code"
}
expect "java-check: Gradle 9.3.1 runs on Java 25" 0 "$(java_check_gradle 9.3.1 25)"
expect "java-check: Java 26 is too new for Gradle 9.3.1" 1 "$(java_check_gradle 9.3.1 26)"
expect "java-check: names Gradle 9.3.1 and Java 17 to 25" 1 \
  "$(grep -c '^LỖI: Java 26 quá mới .*Gradle 9.3.1 chỉ chạy trên Java 17 đến 25.$' "$work/java.out" || true)"
expect "java-check: Java 25 is too new for Gradle 9.0" 1 "$(java_check_gradle 9.0.0 25)"
expect "java-check: a Gradle newer than the table has no upper bound" 0 "$(java_check_gradle 9.4.0 26)"
expect "java-check: a missing Java stops" 1 \
  "$(JAVA_HOME="$work/no-jdk" scripts/java-check.sh > /dev/null 2>&1; echo $?)"

# setup-check.sh: required R2 files (curl -I), DNS through 1.1.1.1 and 8.8.8.8, the Firebase staging secret and the
# tester groups. curl, dig and gcloud are replaced by small fakes driven by files in $work/setup.
mkdir -p "$work/setup-bin" "$work/setup"
cat > "$work/setup-bin/curl" << 'SH'
#!/usr/bin/env bash
# Fake curl: prints the HTTP code listed for the URL (last argument) in $SETUP_FAKE/codes, else 404. A list such as
# "429,200" gives one code per call of that URL (the last one stays). A cache-busting query (?setup-check-nocache=)
# reads $SETUP_FAKE/nocache instead, else 404. Every call without that query is logged in $SETUP_FAKE/calls.
url="${*: -1}"
plain="${url%%\?*}"
if [[ "$url" == *"?setup-check-nocache="* ]]; then
  code="$(awk -v u="$plain" '$1 == u {print $2}' "$SETUP_FAKE/nocache" 2> /dev/null)"
  printf '%s' "${code:-404}"
  exit 0
fi
calls="$(grep -cxF -- "$plain" "$SETUP_FAKE/calls" 2> /dev/null || true)"
echo "$plain" >> "$SETUP_FAKE/calls"
codes="$(awk -v u="$plain" '$1 == u {print $2}' "$SETUP_FAKE/codes")"
IFS=',' read -r -a list <<< "${codes:-404}"
index="${calls:-0}"
[[ "$index" -lt "${#list[@]}" ]] || index=$((${#list[@]} - 1))
printf '%s' "${list[$index]}"
SH
cat > "$work/setup-bin/dig" << 'SH'
#!/usr/bin/env bash
# Fake dig: answers an address unless the resolver (@x) is listed in $SETUP_FAKE/dns-broken.
for arg in "$@"; do [[ "$arg" == @* ]] && resolver="${arg#@}"; done
grep -qx "$resolver" "$SETUP_FAKE/dns-broken" 2> /dev/null || echo 104.21.40.144
SH
cat > "$work/setup-bin/gcloud" << 'SH'
#!/usr/bin/env bash
# Fake gcloud: signs in and prints a token.
[[ "$*" == *print-access-token* ]] && echo fake-token
exit 0
SH
chmod +x "$work/setup-bin/"*
printf 'test/v1/a.txt\n# comment\n\ntest/v1/b.txt  # inline comment\n' > "$work/setup/assets.txt"
base="https://demo.example.com/"
groups_url="https://firebaseappdistribution.googleapis.com/v1/projects/123/groups"
setup_check() { # <codes> [args...]; prints the exit code, keeps the output in $work/setup.out
  printf '%b' "$1" > "$work/setup/codes"; shift
  rm -f "$work/setup/calls"
  local code=0
  # SETUP_CHECK_APP_CHECKS=none: an app's own scripts/setup-check-app.sh stays out of these tests (scripts/tests/app.sh).
  PATH="$work/setup-bin:$PATH" SETUP_FAKE="$work/setup" SETUP_CHECK_ASSETS_LIST="$work/setup/assets.txt" \
    SETUP_CHECK_ASSETS_BASE_URL="$base" SETUP_CHECK_PACKAGE=com.demo GITHUB_ACTIONS='' SETUP_CHECK_RETRY_SECONDS=0 \
    SETUP_CHECK_APP_CHECKS="${SETUP_APP_CHECKS:-none}" \
    scripts/setup-check.sh "$@" > "$work/setup.out" 2>&1 || code=$?
  echo "$code"
}
all_files="${base}test/v1/a.txt 200\n${base}test/v1/b.txt 200\n"
: > "$work/setup/dns-broken"
expect "setup-check: every file there and DNS fine passes" 0 "$(setup_check "$all_files")"
expect "setup-check: checks each listed file" 2 "$(grep -c '^ok   - https://demo.example.com/test/v1/' "$work/setup.out")"
expect "setup-check: a missing R2 file stops" 1 "$(setup_check "${base}test/v1/a.txt 200\n")"
expect "setup-check: names the missing file" 1 "$(grep -c "^LỖI - ${base}test/v1/b.txt trả '404'" "$work/setup.out")"
echo 8.8.8.8 > "$work/setup/dns-broken"
expect "setup-check: a host 8.8.8.8 cannot resolve stops" 1 "$(setup_check "$all_files")"
expect "setup-check: names the resolver" 1 "$(grep -c 'không phân giải được qua 8.8.8.8' "$work/setup.out")"
: > "$work/setup/dns-broken"
real_json='{"project_info": {"project_id": "demo-staging", "project_number": "123"}, "client": [{"client_info": {"android_client_info": {"package_name": "com.demo.staging"}}}]}'
staging_check() { # <google-services JSON> <codes>; prints the exit code
  GOOGLE_SERVICES="$1" DISTRIBUTION_CREDENTIALS='{"type": "service_account"}' STAGING_TESTER_GROUPS="team, qa" \
    setup_check "$2" --staging
}
expect "setup-check --staging: real secret and both groups pass" 0 \
  "$(staging_check "$real_json" "$all_files${groups_url}/team 200\n${groups_url}/qa 200\n")"
expect "setup-check --staging: the fake google-services.json stops" 1 \
  "$(staging_check '{"project_info": {"project_id": "fake-staging-project", "project_number": "1"}, "client": []}' \
    "$all_files${groups_url}/team 200\n${groups_url}/qa 200\n")"
expect "setup-check --staging: says the secret is the fake file" 1 "$(grep -c 'đang là file cấu hình giả' "$work/setup.out")"
expect "setup-check --staging: a secret without the staging app stops" 1 \
  "$(staging_check "${real_json/com.demo.staging/com.other.staging}" "$all_files${groups_url}/team 200\n${groups_url}/qa 200\n")"
expect "setup-check --staging: a missing tester group stops" 1 \
  "$(staging_check "$real_json" "$all_files${groups_url}/team 200\n")"
expect "setup-check --staging: names the missing group" 1 "$(grep -c "chưa có nhóm người test 'qa'" "$work/setup.out")"
expect "setup-check --staging: a group the API does not answer only warns" 0 \
  "$(staging_check "$real_json" "$all_files${groups_url}/team 200\n${groups_url}/qa 403\n")"
expect "setup-check --staging: never prints the secret" 0 \
  "$(grep -c 'demo-staging\|fake-token\|service_account' "$work/setup.out" || true)"
expect "setup-check --staging: a missing secret stops" 1 \
  "$(GOOGLE_SERVICES='' DISTRIBUTION_CREDENTIALS='' setup_check "$all_files" --staging)"
expect "setup-check: rejects an unknown argument" 1 "$(setup_check "$all_files" --stagin)"
expect "setup-check: reads the staging address of this repository" 1 \
  "$(grep -cE '"ASSETS_BASE_URL", *"\\"https://[^"]+/\\""' app/build.gradle.kts)"
# One passing hiccup of R2 (no connection, 429, 5xx) is asked again; a CDN that keeps an old 404 is named (L-113).
setup_calls() { grep -cxF -- "$1" "$work/setup/calls" 2> /dev/null || true; }
expect "setup-check: a 503 that passes on the retry is not a failure" "0 2" \
  "$(setup_check "${base}test/v1/a.txt 503,200\n${base}test/v1/b.txt 200\n") $(setup_calls "${base}test/v1/a.txt")"
expect "setup-check: a 429 that passes on the retry is not a failure" 0 \
  "$(setup_check "${base}test/v1/a.txt 429,200\n${base}test/v1/b.txt 200\n")"
expect "setup-check: a file that fails to connect once is asked again" "0 2" \
  "$(setup_check "${base}test/v1/a.txt 000,200\n${base}test/v1/b.txt 200\n") $(setup_calls "${base}test/v1/a.txt")"
expect "setup-check: a 429 seen twice stops" "1 2" \
  "$(setup_check "${base}test/v1/a.txt 429,429,200\n${base}test/v1/b.txt 200\n") $(setup_calls "${base}test/v1/a.txt")"
expect "setup-check: a 404 is not asked again" "1 1" \
  "$(setup_check "${base}test/v1/a.txt 200\n") $(setup_calls "${base}test/v1/b.txt")"
expect "setup-check: a file really missing gets no CDN hint" 0 "$(grep -c 'CDN' "$work/setup.out" || true)"
printf '%s 200\n' "${base}test/v1/b.txt" > "$work/setup/nocache"
expect "setup-check: a required file the CDN still answers 404 for names the cache" "1 1" \
  "$(setup_check "${base}test/v1/a.txt 200\n") $(grep -c "^LỖI - ${base}test/v1/b.txt trả '404', cần 200.*CDN còn giữ câu trả lời cũ" "$work/setup.out")"
expect "setup-check: a 500 gets no CDN hint, even when the file is there" 0 \
  "$(setup_check "${base}test/v1/a.txt 200\n${base}test/v1/b.txt 500\n" > /dev/null; grep -c 'CDN' "$work/setup.out" || true)"
rm -f "$work/setup/nocache"
# An app's own checks (scripts/setup-check-app.sh in the app) are sourced at the end, with the helpers.
cat > "$work/setup/app-checks.sh" << 'SH'
ok "app check ran (staging=$STAGING, base=$base_url)"
[ "$(http_status "${base_url}app/extra.json" -o /dev/null -I)" = "200" ] || problem "app file missing"
SH
expect "setup-check: an app's own checks run with the helpers" "0 1" \
  "$(SETUP_APP_CHECKS="$work/setup/app-checks.sh" setup_check "$all_files${base}app/extra.json 200\n") $(grep -c "^ok   - app check ran (staging=no, base=$base)" "$work/setup.out")"
expect "setup-check: a problem of an app's own checks stops" "1 1" \
  "$(SETUP_APP_CHECKS="$work/setup/app-checks.sh" setup_check "$all_files") $(grep -c '^LỖI - app file missing' "$work/setup.out")"
expect "setup-check: an app without its own checks file still passes" 0 \
  "$(SETUP_APP_CHECKS="$work/setup/no-such-file.sh" setup_check "$all_files")"

# device-session.sh records a test session and counts crashes and ANRs; adb is replaced by scripts/tests/fake-adb.
cp scripts/tests/fake-adb "$work/bin/adb"
chmod +x "$work/bin/adb"
fixtures=scripts/tests/fixtures/device-session
session() { # <logcat fixture> <command...>; prints the exit code
  local code=0
  PATH="$work/bin:$PATH" DEVICE_SESSION_DIR="$work/sessions" DEVICE_SESSION_REPORTS="$work/reports" DEVICE_SESSION_RUNS="$work/runs" \
    APP_PACKAGE=com.example.app.staging \
    FAKE_ADB_LOGCAT="$1" scripts/device-session.sh "${@:2}" > "$work/session.out" 2>&1 || code=$?
  echo "$code"
}
expect "device-session: start refuses without a device" 1 "$(FAKE_ADB_NO_DEVICE=1 session /dev/null start)"
expect "device-session: start refuses when the app is missing" 1 "$(FAKE_ADB_NO_APP=1 session /dev/null start)"
expect "device-session: start records" 0 "$(session "$fixtures/logcat-crash.txt" start ci)"
first="$(find "$work/sessions" -maxdepth 1 -name "TS-*" | sort | head -n 1)"
expect "device-session: session folder has notes and details" "notes.md session.md" \
  "$( [[ -f "$first/notes.md" ]] && echo -n "notes.md "; [[ -f "$first/session.md" ]] && echo -n "session.md")"
expect "device-session: a second start is refused while recording" 1 "$(session /dev/null start)"
expect "device-session: reset clears the app's data while recording" 0 "$(session /dev/null reset)"
expect "device-session: stop reminds to write notes" 1 "$(session /dev/null stop > /dev/null; grep -c 'notes.md chưa có ghi chú' "$work/session.out")"
mkdir -p "$work/reports"
touch "$work/reports/TS-$(date +%Y%m%d)-02.md"
expect "device-session: an id used by a report is skipped" 0 "$(session "$fixtures/logcat-crash.txt" start ci)"
expect "device-session: new session gets the next free id" yes \
  "$([[ -d "$work/sessions/TS-$(date +%Y%m%d)-03" && ! -e "$work/sessions/TS-$(date +%Y%m%d)-02" ]] && echo yes || echo no)"
first="$work/sessions/TS-$(date +%Y%m%d)-03"
expect "device-session: scenarios need --yes" 1 "$(session /dev/null scenarios)"
expect "device-session: stop reports a crash as blocking" 1 "$(session /dev/null stop)"
expect "device-session: counts the crash" 1 "$(grep -c '^| App dừng đột ngột (crash) | 1 | chặn |' "$first/summary.md")"
expect "device-session: counts the ANR" 1 "$(grep -c '^| App không phản hồi (ANR) | 1 | chặn |' "$first/summary.md")"
expect "device-session: counts app errors" 1 "$(grep -F '| Log lỗi của app (' "$first/summary.md" | grep -cF '| 1 | xem |')"
expect "device-session: keeps the crash trace in app-log.txt" 1 "$(grep -c 'Main.onCreate' "$first/app-log.txt")"
expect "device-session: leaves out other apps' lines" 0 "$(grep -c 'com.android.settings' "$first/app-log.txt")"
mkdir -p "$work/clean"
cp "$fixtures/logcat-clean.txt" "$work/clean/logcat.txt"
expect "device-session: a clean session is not blocking" 0 "$(session /dev/null summary "$work/clean" com.example.app.staging)"
# An ANR inside the ad SDK's AdActivity (a test ad under monkey) is counted apart and does not block; one in an
# activity of the app still blocks (FB-20261025-02).
mkdir -p "$work/ad-anr" "$work/app-anr"
{
  cat "$fixtures/logcat-clean.txt"
  echo "10-27 05:01:37.028   538  7321 E ActivityManager: ANR in com.example.app.staging (com.example.app.staging/com.google.android.gms.ads.AdActivity)"
  echo "10-27 05:01:37.033  6539  6554 W Monkey  : ANR in com.example.app.staging (com.example.app.staging/com.google.android.gms.ads.AdActivity)"
} > "$work/ad-anr/logcat.txt"
printf '%s\n' '// NOT RESPONDING: com.example.app.staging (pid 5828)' \
  'ANR in com.example.app.staging (com.example.app.staging/com.google.android.gms.ads.AdActivity)' 'Events injected: 1059' \
  > "$work/ad-anr/monkey.txt"
expect "device-session: an ANR only inside AdActivity does not block" 0 \
  "$(session /dev/null summary "$work/ad-anr" com.example.app.staging)"
expect "device-session: the AdActivity ANRs have their own row, level xem" "1 1 1" \
  "$(grep -c '^| ANR trong màn quảng cáo của SDK (`AdActivity`, FB-20261025-02) | 2 | xem |' "$work/ad-anr/summary.md") $(grep -c '^| App không phản hồi (ANR) | 0 |  |' "$work/ad-anr/summary.md") $(grep -c '^| Monkey báo crash hay treo | 0 |  |' "$work/ad-anr/summary.md")"
{
  cat "$work/ad-anr/logcat.txt"
  echo "10-27 05:02:01.000   538  7321 E ActivityManager: ANR in com.example.app.staging (com.example.app.staging/com.example.app.MainActivity)"
} > "$work/app-anr/logcat.txt"
printf '%s\n' '// NOT RESPONDING: com.example.app.staging (pid 5828)' \
  'ANR in com.example.app.staging (com.example.app.staging/com.example.app.MainActivity)' > "$work/app-anr/monkey.txt"
expect "device-session: an ANR in an activity of the app still blocks" 1 \
  "$(session /dev/null summary "$work/app-anr" com.example.app.staging)"
expect "device-session: the app ANR and the monkey hang stay blocking" "1 1" \
  "$(grep -c '^| App không phản hồi (ANR) | 1 | chặn |' "$work/app-anr/summary.md") $(grep -c '^| Monkey báo crash hay treo | 1 | chặn |' "$work/app-anr/summary.md")"
# The offline scenario turns every radio off and runs only when the device really has no network (L-031).
# The plan scenarios (scripts/device-scenario.py) get an empty folder here, so these tests keep counting the standard ones.
export DEVICE_SESSION_SLEEP=0 FAKE_ADB_CALLS="$work/adb-calls.txt" DEVICE_SCENARIOS_GLOB="$work/no-scenarios/*.txt"
# An emulator session named only in an auto-run report keeps its id (L-026 of camera-pose).
mkdir -p "$work/runs"
printf 'Kiểm thử trên máy ảo: phiên TS-%s-04, không có báo cáo riêng.\n' "$(date +%Y%m%d)" > "$work/runs/RUN-1.md"
# Runtime permissions are granted after each data clear, so a prompt left open does not cover a cold start (L-009).
export FAKE_ADB_RUNTIME_PERMISSIONS="android.permission.CAMERA android.permission.POST_NOTIFICATIONS"
expect "device-session: scenarios run offline" 0 \
  "$(session "$fixtures/logcat-clean.txt" start ci > /dev/null; session /dev/null scenarios --yes)"
offline="$(find "$work/sessions" -maxdepth 1 -name "TS-*" | sort | tail -n 1)"
expect "device-session: an id named in an auto-run report is skipped" "TS-$(date +%Y%m%d)-05 no" \
  "$(basename "$offline") $([[ -e "$work/sessions/TS-$(date +%Y%m%d)-04" ]] && echo yes || echo no)"
expect "device-session: every runtime permission is granted after each data clear" "2 2 0" \
  "$(grep -c '^shell pm grant com.example.app.staging android.permission.CAMERA$' "$FAKE_ADB_CALLS") $(grep -c 'pm grant .*POST_NOTIFICATIONS$' "$FAKE_ADB_CALLS") $(grep -c 'pm grant .*INTERNET' "$FAKE_ADB_CALLS")"
expect "device-session: granted permissions are recorded" 2 \
  "$(grep -c '^permissions granted=CAMERA,POST_NOTIFICATIONS$' "$offline/scenarios.txt")"
expect "device-session: Wi-Fi and mobile data are turned off, then on" "4" \
  "$(grep -cE '^shell svc (wifi|data) (disable|enable)$' "$FAKE_ADB_CALLS")"
expect "device-session: the offline launch is recorded" 1 "$(grep -c 'offline_first_launch network=off' "$offline/scenarios.txt")"
session /dev/null stop > /dev/null
expect "device-session: a valid offline scenario adds no warning" 0 "$(grep -c 'Kịch bản mất mạng bị bỏ qua' "$offline/summary.md")"
expect "device-session: scenarios skip offline when the network stays on" 0 \
  "$(session "$fixtures/logcat-clean.txt" start ci > /dev/null; FAKE_ADB_NETWORK=100 session /dev/null scenarios --yes)"
offline="$(find "$work/sessions" -maxdepth 1 -name "TS-*" | sort | tail -n 1)"
expect "device-session: the skipped offline launch is recorded" 1 \
  "$(grep -c 'offline_first_launch network=still_on skipped' "$offline/scenarios.txt")"
session /dev/null stop > /dev/null
expect "device-session: summary shows the skipped offline scenario" 1 \
  "$(grep -c '^| Kịch bản mất mạng bị bỏ qua (không tắt được mạng) | 1 | xem |' "$offline/summary.md")"
: > "$FAKE_ADB_CALLS"
expect "device-session: scenarios refuse an unknown option" 1 \
  "$(session "$fixtures/logcat-clean.txt" start ci > /dev/null; session /dev/null scenarios --yes --grant)"
expect "device-session: --ask-permissions keeps the permission prompts" 0 "$(session /dev/null scenarios --yes --ask-permissions)"
offline="$(find "$work/sessions" -maxdepth 1 -name "TS-*" | sort | tail -n 1)"
expect "device-session: no grant when asking, and it is recorded" "0 2" \
  "$(grep -c 'pm grant' "$FAKE_ADB_CALLS") $(grep -c '^permissions ask$' "$offline/scenarios.txt")"
session /dev/null stop > /dev/null
expect "device-session: a refused grant is recorded" 2 \
  "$(session "$fixtures/logcat-clean.txt" start ci > /dev/null; FAKE_ADB_GRANT_FAILS=android.permission.CAMERA session /dev/null scenarios --yes > /dev/null
     grep -c '^permissions granted=POST_NOTIFICATIONS failed=CAMERA$' "$(find "$work/sessions" -maxdepth 1 -name "TS-*" | sort | tail -n 1)/scenarios.txt")"
session /dev/null stop > /dev/null
# A staging build with a second LAUNCHER activity (the ad preview of T-014) still starts MainActivity (FB-20261024-01).
: > "$FAKE_ADB_CALLS"
expect "device-session: two launchers start MainActivity, not the chooser" 0 \
  "$(session "$fixtures/logcat-clean.txt" start ci > /dev/null
     FAKE_ADB_LAUNCHERS="com.example.app/com.example.app.ads.AdPreviewActivity com.example.app/com.example.app.MainActivity" \
       session /dev/null scenarios --yes)"
expect "device-session: every scenario launch opens MainActivity" 4 \
  "$(grep -c '^shell am start -W -n com.example.app/com.example.app.MainActivity$' "$FAKE_ADB_CALLS")"
session /dev/null stop > /dev/null
expect "device-session: two launchers without MainActivity are refused" 1 \
  "$(session "$fixtures/logcat-clean.txt" start ci > /dev/null
     FAKE_ADB_LAUNCHERS="com.example.app/.A com.example.app/.B" session /dev/null scenarios --yes)"
session /dev/null stop > /dev/null
unset DEVICE_SESSION_SLEEP FAKE_ADB_CALLS FAKE_ADB_RUNTIME_PERMISSIONS DEVICE_SCENARIOS_GLOB

# device-scenario.py runs the scenario files of plans on a device through uiautomator dumps (L-085).
# adb is scripts/tests/fake-adb; the UI it shows comes from FAKE_ADB_UI_SEQ, one dump file per `uiautomator dump`.
ds_fixtures=scripts/tests/fixtures/device-scenario
ds_files="$work/ds-files"
mkdir -p "$ds_files"
ds_log="$work/ds-session/scenarios.txt"
ds_calls="$work/ds-calls.txt"
ds_run() { # <dump files of the fixtures, in order> <scenario glob> [VAR=value ...]; prints the exit code
  local seq="" f code=0
  for f in $1; do seq+="$ds_fixtures/$f "; done
  rm -rf "$work/ds-session" "$work/adb-state"
  mkdir -p "$work/ds-session"
  [[ -n "${ds_logcat:-}" ]] && cp "$ds_logcat" "$work/ds-session/logcat.txt"
  : > "$ds_calls"
  env PATH="$work/bin:$PATH" FAKE_ADB_UI_SEQ="${seq% }" FAKE_ADB_STATE_DIR="$work/adb-state" FAKE_ADB_CALLS="$ds_calls" \
    DEVICE_SCENARIOS_GLOB="$2" DEVICE_SCENARIO_POLL=0.05 DEVICE_SESSION_SLEEP=0 APP_PACKAGE=com.example.app.staging \
    DEVICE_SCENARIO_CONSENT_GRACE=0.3 FAKE_ADB_RUNTIME_PERMISSIONS="android.permission.CAMERA" \
    FAKE_ADB_LOGCAT_FILE="$work/ds-session/logcat.txt" "${@:3}" \
    python3 -I scripts/device-scenario.py run --session "$work/ds-session" --package com.example.app.staging \
    > "$work/ds.out" 2>&1 || code=$?
  echo "$code"
}
ds_count() { grep -c -- "$1" "${2:-$ds_log}" 2> /dev/null || true; } # lines of the log (or another file) that match
joined() { echo "$@"; }
ds_script() { # <name> <commands...>: writes a scenario file with a valid header
  local name="$1"
  shift
  printf 'ticket: T-999\ncriterion: Test\ntimeout: 30\n' > "$ds_files/$name"
  printf '%s\n' "$@" >> "$ds_files/$name"
}

expect "device-scenario: a passing file is reported as passed" "0 1 6" \
  "$(ds_run "consent.xml welcome.xml home.xml" "$ds_fixtures/scenarios/ok.txt") $(ds_count '^plan ok.txt result=đạt seconds=[0-9]*$') $(ds_count '^plan ok.txt step=[0-9]* .* ok$')"
expect "device-scenario: a passing file leaves no fail.png" "1 no" \
  "$(ds_count 'result=đạt') $([[ -e "$work/ds-session/scenarios/ok/fail.png" ]] && echo yes || echo no)"
expect "device-scenario: a passing file keeps its screenshot" yes \
  "$([[ -f "$work/ds-session/scenarios/ok/done.png" ]] && echo yes || echo no)"
expect "device-scenario: the header is written to the log" 1 "$(ds_count '^plan ok.txt ticket=T-900 criterion=Mẫu đạt | có dấu gạch$')"
expect "device-scenario: tap uses the centre of the bounds" 1 "$(ds_count '^shell input tap 540 1806$' "$ds_calls")"
expect "device-scenario: marks the start of each file in the log" 1 \
  "$(ds_count "^shell log -t VGL.Session .*scenario=plan file=ok.txt" "$ds_calls")"

ds_script consent.txt 'consent decline 5'
expect "device-scenario: consent decline taps Do not consent when shown" "0 1" \
  "$(ds_run "consent.xml" "$ds_files/consent.txt") $(ds_count '^shell input tap 540 1970$' "$ds_calls")"
# The consent form's WebView on some devices (Xiaomi HyperOS) labels its buttons only in content-desc (L-122).
expect "device-scenario: consent decline taps Do not consent labelled only in content-desc" "0 1" \
  "$(ds_run "consent-desc.xml" "$ds_files/consent.txt") $(ds_count '^shell input tap 540 1970$' "$ds_calls")"
ds_script late.txt 'consent decline 5' 'wait id=welcome_screen 3'
expect "device-scenario: consent decline waits for a late consent form" "0 1 1" \
  "$(ds_run "blank.xml blank.xml consent.xml welcome.xml" "$ds_files/late.txt") $(ds_count '^shell input tap 540 1970$' "$ds_calls") $(ds_count 'result=đạt')"
expect "device-scenario: consent decline taps nothing when there is no form" "0 1 0" \
  "$(ds_run "blank.xml welcome.xml" "$ds_files/consent.txt") $(ds_count 'result=đạt') $(ds_count 'input tap' "$ds_calls")"
# MainActivity asks for consent asynchronously: the form may show a few seconds after the app root.
expect "device-scenario: consent decline still declines a form that shows after the app root" "0 1 1" \
  "$(ds_run "welcome.xml consent.xml welcome.xml" "$ds_files/consent.txt" DEVICE_SCENARIO_CONSENT_GRACE=3) $(ds_count '^shell input tap 540 1970$' "$ds_calls") $(ds_count 'result=đạt')"
ds_script none.txt 'consent decline 1'
expect "device-scenario: consent decline fails when nothing shows" "0 1 1" \
  "$(ds_run "blank.xml" "$ds_files/none.txt") $(ds_count 'step=1 consent decline 1 FAIL') $(ds_count 'result=lỗi')"

# The system shade left open over the app (monkey swipes from the top edge) hides the app from every dump:
# fresh closes it before opening the app, and a dump of the system UI only closes it once more (FB-20261027-02).
ds_first() { local n; n="$(grep -n -m1 -- "$1" "$ds_calls" | cut -d: -f1)"; echo "${n:-999999}"; } # line of the first matching adb call, 999999 when none
expect "device-scenario: fresh closes the system shade before opening the app" "0 1 yes" \
  "$(ds_run "consent.xml welcome.xml home.xml" "$ds_fixtures/scenarios/ok.txt" FAKE_ADB_SHADE="$ds_fixtures/shade.xml") $(ds_count 'result=đạt') $( (( $(ds_first '^shell cmd statusbar collapse$') < $(ds_first '^shell am start -W') )) && echo yes)"
ds_script shade.txt 'fresh' 'expect id=home_screen'
expect "device-scenario: a dump of the system UI only closes the shade and reads again" "0 1 2 1" \
  "$(ds_run "shade.xml home.xml" "$ds_files/shade.txt") $(ds_count 'result=đạt') $(ds_count '^shell cmd statusbar collapse$' "$ds_calls") $(ds_count 'note step=2 .*bảng của hệ thống che app')"
expect "device-scenario: a shade that stays open is closed once per dump, then the step fails" "0 1 2" \
  "$(ds_run "shade.xml" "$ds_files/shade.txt") $(ds_count 'step=2 expect id=home_screen FAIL') $(ds_count '^shell cmd statusbar collapse$' "$ds_calls")"

ds_script cards.txt 'tap idp=gallery_card_ in=gallery_grid index=2'
expect "device-scenario: idp= in= index= pick the second card of the grid" "0 1" \
  "$(ds_run "grid.xml" "$ds_files/cards.txt") $(ds_count '^shell input tap 540 600$' "$ds_calls")"
ds_script strs.txt 'tap str=poses_chip_all' 'tap str=poses_title'
expect "device-scenario: str= matches text and content-desc" "0 1 1" \
  "$(ds_run "grid.xml" "$ds_files/strs.txt") $(ds_count '^shell input tap 140 300$' "$ds_calls") $(ds_count '^shell input tap 325 445$' "$ds_calls")"
ds_script selected.txt 'expect text=All selected=true' 'expect text=Chế độ selected=true'
expect "device-scenario: selected=true matches selected or checked" "0 1" \
  "$(ds_run "grid.xml" "$ds_files/selected.txt") $(ds_count 'result=đạt')"
ds_script unselected.txt 'expect text=Tất cả selected=true'
expect "device-scenario: selected=true does not match an unselected chip" "0 1 1" \
  "$(ds_run "grid.xml" "$ds_files/unselected.txt") $(ds_count 'step=1 expect text=Tất cả selected=true FAIL') $(ds_count 'result=lỗi')"
ds_script clickable.txt 'tap text=Nhãn clickable=true'
expect "device-scenario: clickable=true skips plain nodes" "0 1" \
  "$(ds_run "grid.xml" "$ds_files/clickable.txt") $(ds_count '^shell input tap 250 35$' "$ds_calls")"
# A Compose button that is turned off still has clickable="true" in the dump, only enabled="false": enabled=true
# waits for it to turn on, and tap_if with it does not tap the button while it is off.
ds_script next-held.txt 'tap_if id=intro_next enabled=true' 'expect id=intro_screen'
expect "device-scenario: tap_if with enabled=true does not tap a button that is off" "0 1 0" \
  "$(ds_run "next-held.xml" "$ds_files/next-held.txt") $(ds_count 'result=đạt') $(ds_count '^shell input tap ' "$ds_calls")"
ds_script next-ready.txt 'wait id=intro_next enabled=true 10' 'tap id=intro_next enabled=true'
# The tap comes only after the third dump (the first with the button on): a wait that ignored enabled would tap
# after two dumps, while the button is still off. Only the dumps before the first tap are counted.
expect "device-scenario: wait with enabled=true waits until the button turns on, then taps it" "0 1 1 yes" \
  "$(ds_run "next-held.xml next-held.xml next-ready.xml" "$ds_files/next-ready.txt") $(ds_count 'result=đạt') $(ds_count '^shell input tap 540 2250$' "$ds_calls") $( (( $(awk '/^shell input tap/ {exit} /^shell uiautomator dump/ {n++} END {print n+0}' "$ds_calls") >= 3 )) && echo yes || echo no)"
ds_script next-off.txt 'tap id=intro_next enabled=false'
expect "device-scenario: enabled= takes only true" "1 1" \
  "$(code=0; python3 -I scripts/device-scenario.py check "$ds_files/next-off.txt" > "$work/ds-check.out" 2>&1 || code=$?; echo "$code") $(grep -c 'enabled= chỉ nhận true' "$work/ds-check.out" || true)"
ds_script alternatives.txt 'wait id=missing_tag | id=gallery_grid 2'
expect "device-scenario: alternatives separated by | match any of them" "0 1" \
  "$(ds_run "grid.xml" "$ds_files/alternatives.txt") $(ds_count 'result=đạt')"
ds_script count3.txt 'count idp=gallery_card_ in=gallery_grid 3'
ds_script count4.txt 'count idp=gallery_card_ in=gallery_grid 4'
expect "device-scenario: count checks the number of matches" "0 1 0 1" \
  "$(ds_run "grid.xml" "$ds_files/count3.txt") $(ds_count 'result=đạt') $(ds_run "grid.xml" "$ds_files/count4.txt" > /dev/null; ds_count 'result=đạt') $(ds_count 'step=1 count idp=gallery_card_ in=gallery_grid 4 FAIL')"

expect "device-scenario: the first failing step stops the file" "0 1 0 1 1 1" \
  "$(ds_run "home.xml" "$ds_fixtures/scenarios/fail.txt") $(ds_count '^plan fail.txt step=3 .* FAIL ') $(ds_count '^plan fail.txt step=4') $(ds_count 'result=lỗi seconds=') $( [[ -f "$work/ds-session/scenarios/fail/fail.png" ]] && echo 1) $( [[ -f "$work/ds-session/scenarios/fail/fail.xml" ]] && echo 1)"
expect "device-scenario: need ends the file as skipped (environment)" "0 1 0 0 none" \
  "$(ds_run "home.xml" "$ds_fixtures/scenarios/need.txt") $(ds_count 'result=bỏ qua (môi trường) seconds=') $(ds_count '^plan need.txt step=3') $(ds_count 'FAIL') $([[ -e "$work/ds-session/scenarios/need/fail.png" ]] && echo yes || echo none)"

ds_script busy.txt 'wait id=home_screen 5'
expect "device-scenario: not idle is retried until the timeout" "0 1" \
  "$(ds_run "busy.idle-error busy.idle-error home.xml" "$ds_files/busy.txt") $(ds_count 'result=đạt')"
ds_script busy-only.txt 'wait id=home_screen 1'
expect "device-scenario: not idle until the timeout is a failure that says so" "0 1" \
  "$(ds_run "busy.idle-error" "$ds_files/busy-only.txt") $(ds_count 'FAIL could not get idle state')"

# A hanging adb: every adb call has its own deadline, the file fails, the next file runs, the whole run is short.
printf 'ticket: T-999\ncriterion: Hang\ntimeout: 3\nwait id=home_screen 3\n' > "$ds_files/hang-a.txt"
printf 'ticket: T-999\ncriterion: After the hang\ntimeout: 30\nback\nshot after\n' > "$ds_files/hang-b.txt"
ds_started=$SECONDS
ds_code="$(ds_run "home.xml" "$ds_files/hang-*.txt" FAKE_ADB_HANG=uiautomator DEVICE_SCENARIO_ADB_TIMEOUT=1)"
expect "device-scenario: a hanging adb fails the file within its deadline" "0 1 1 1 yes" \
  "$ds_code $(ds_count 'FAIL adb_timeout') $(ds_count '^plan hang-a.txt result=lỗi') $(ds_count '^plan hang-b.txt result=đạt') $([[ $((SECONDS - ds_started)) -lt 15 ]] && echo yes || echo no)"

expect "device-scenario: files beyond the budget are skipped" "0 2 yes" \
  "$(ds_run "home.xml" "$ds_fixtures/scenarios/[of]*.txt" DEVICE_SCENARIOS_BUDGET=0 | head -n 1) $(ds_count 'result=bỏ qua (hết thời gian)') $([[ "$(ds_count '^shell wm size reset$' "$ds_calls")" -ge 1 ]] && echo yes || echo no)"

ds_reset_calls() { # how many of the reset commands the last run sent
  local n=0 pattern
  for pattern in '^shell wm size reset$' '^shell wm density reset$' '^shell cmd uimode night no$' \
    'settings put system accelerometer_rotation 1$' 'settings put system user_rotation 0$' 'set-app-locales'; do
    n=$((n + $(ds_count "$pattern" "$ds_calls")))
  done
  echo "$n"
}
ds_run "home.xml" "$ds_fixtures/scenarios/fail.txt" > /dev/null
expect "device-scenario: the device is reset after a failing file" yes "$([[ "$(ds_reset_calls)" -ge 6 ]] && echo yes || echo no)"
ds_run "consent.xml welcome.xml home.xml" "$ds_fixtures/scenarios/ok.txt" > /dev/null
expect "device-scenario: the device is reset after a passing file" yes "$([[ "$(ds_reset_calls)" -ge 6 ]] && echo yes || echo no)"
ds_script rotate.txt 'device rotate landscape' 'device night on' 'device unfold' 'device locale en-US'
ds_run "home.xml" "$ds_files/rotate.txt" > /dev/null
expect "device-scenario: device commands change the device and the reset undoes them" "1 1 1 1 1" \
  "$(ds_count 'user_rotation 1$' "$ds_calls") $(ds_count '^shell cmd uimode night yes$' "$ds_calls") $(ds_count '^shell wm size 1766x2208$' "$ds_calls") $(ds_count '^shell wm density 420$' "$ds_calls") $(ds_count 'set-app-locales com.example.app.staging --locales en-US' "$ds_calls")"

ds_script offline.txt 'network off' 'back'
expect "device-scenario: network off turns every radio off" "0 1 1" \
  "$(ds_run "home.xml" "$ds_files/offline.txt") $(ds_count 'result=đạt') $(ds_count '^shell svc wifi disable$' "$ds_calls")"
expect "device-scenario: network off that keeps a network skips the file" "0 1 0" \
  "$(ds_run "home.xml" "$ds_files/offline.txt" FAKE_ADB_NETWORK=100) $(ds_count 'result=bỏ qua (môi trường)') $(ds_count 'result=đạt')"

# The device is given back as it was before the run, not with fixed values.
ds_script night.txt 'device night on'
ds_run "home.xml" "$ds_files/night.txt" FAKE_ADB_ACCELEROMETER_ROTATION=0 FAKE_ADB_NIGHT=yes > /dev/null
expect "device-scenario: the reset restores the rotation and night mode read before the file" "2 1 0 0" \
  "$(ds_count 'settings put system accelerometer_rotation 0$' "$ds_calls") $(ds_count '^shell cmd uimode night yes$' "$ds_calls" | awk '{print ($1 >= 2)}') $(ds_count 'accelerometer_rotation 1$' "$ds_calls") $(ds_count '^shell cmd uimode night no$' "$ds_calls")"

# network on waits until the device has a default network again; without one the file is skipped.
ds_script offon.txt 'network off' 'network on' 'back'
expect "device-scenario: network on that brings no network back skips the file" "0 1 0" \
  "$(ds_run "home.xml" "$ds_files/offon.txt") $(ds_count 'step=2 network on SKIP') $(ds_count 'result=đạt')"
expect "device-scenario: network on waits for a default network" "0 1 yes" \
  "$(ds_run "home.xml" "$ds_files/offon.txt" FAKE_ADB_NETWORK_ON=100) $(ds_count 'result=đạt') $([[ "$(sed -n '/^shell svc wifi enable$/,$p' "$ds_calls" | grep -c 'dumpsys connectivity')" -ge 1 ]] && echo yes || echo no)"

# The reset runs on every way out of a file: an internal error of the runner and SIGTERM from the outer timeout.
ds_script broken.txt 'shot broken' 'back'
rm -rf "$work/ds-session" "$work/adb-state"
mkdir -p "$work/ds-session"
: > "$work/ds-session/scenarios" # a file where the runner needs a folder: shot raises an OSError
: > "$ds_calls"
ds_code=0
env PATH="$work/bin:$PATH" FAKE_ADB_UI_SEQ="$ds_fixtures/home.xml" FAKE_ADB_STATE_DIR="$work/adb-state" FAKE_ADB_CALLS="$ds_calls" \
  DEVICE_SCENARIOS_GLOB="$ds_files/broken.txt" DEVICE_SCENARIO_POLL=0.05 DEVICE_SESSION_SLEEP=0 APP_PACKAGE=com.example.app.staging \
  python3 -I scripts/device-scenario.py run --session "$work/ds-session" --package com.example.app.staging \
  > "$work/ds.out" 2>&1 || ds_code=$?
expect "device-scenario: an internal error still resets the device" "1 yes 1" \
  "$ds_code $([[ "$(ds_reset_calls)" -ge 6 ]] && echo yes || echo no) $(ds_count '^plan broken.txt result=lỗi')"

ds_script slow.txt 'wait id=never_shown 25'
rm -rf "$work/ds-session" "$work/adb-state"
mkdir -p "$work/ds-session"
: > "$ds_calls"
env PATH="$work/bin:$PATH" FAKE_ADB_UI_SEQ="$ds_fixtures/home.xml" FAKE_ADB_STATE_DIR="$work/adb-state" FAKE_ADB_CALLS="$ds_calls" \
  DEVICE_SCENARIOS_GLOB="$ds_files/slow.txt" DEVICE_SCENARIO_POLL=0.05 DEVICE_SESSION_SLEEP=0 APP_PACKAGE=com.example.app.staging \
  python3 -I scripts/device-scenario.py run --session "$work/ds-session" --package com.example.app.staging \
  > "$work/ds.out" 2>&1 &
ds_pid=$!
for _ in $(seq 1 50); do grep -q 'uiautomator dump' "$ds_calls" 2> /dev/null && break; sleep 0.1; done
kill -TERM "$ds_pid"
ds_code=0
wait "$ds_pid" || ds_code=$?
expect "device-scenario: SIGTERM still resets the device and ends the file" "yes yes 1" \
  "$([[ "$ds_code" -ne 0 ]] && echo yes || echo no) $([[ "$(ds_reset_calls)" -ge 6 ]] && echo yes || echo no) $(ds_count '^plan slow.txt result=lỗi')"

# The reset is part of the budget: a file ends early enough for the reset to finish within DEVICE_SCENARIOS_BUDGET.
ds_script slow-budget.txt 'wait id=never_shown 25'
ds_run "home.xml" "$ds_files/slow-budget.txt" DEVICE_SCENARIOS_BUDGET=6 DEVICE_SCENARIO_RESET_LIMIT=4 \
  DEVICE_SCENARIO_EVIDENCE_RESERVE=0 > /dev/null
expect "device-scenario: the file deadline leaves room for the reset" 1 "$(ds_count '^plan slow-budget.txt result=lỗi seconds=2$')"

# A quote in the file name never reaches the device shell: such a name is a file error (SAFE_NAME), no adb call.
printf "ticket: T-999\ncriterion: Quote\ntimeout: 30\nback\n" > "$ds_files/q'x.txt"
ds_run "home.xml" "$ds_files/q'x.txt" > /dev/null
expect "device-scenario: a file name with a quote is a file error that sends nothing to the device shell" "1 0" \
  "$(ds_count '^plan q_x.txt result=lỗi tệp') $(grep -c 'log -t' "$ds_calls" || true)"

expect "device-scenario: check reports an unknown command with its line" "1 1" \
  "$(code=0; python3 -I scripts/device-scenario.py check "$ds_fixtures/bad.txt" > "$work/ds-check.out" 2>&1 || code=$?; echo "$code") $(grep -c 'bad.txt dòng 5' "$work/ds-check.out" || true)"
expect "device-scenario: a file with a syntax error is a file error that sends no input" "0 1 0" \
  "$(ds_run "home.xml" "$ds_fixtures/bad.txt") $(ds_count 'result=lỗi tệp') $(ds_count 'input' "$ds_calls")"
expect "device-scenario: check reports an id or str that the code does not have" "1 1 1" \
  "$(code=0; python3 -I scripts/device-scenario.py check "$ds_fixtures/bad-id.txt" > "$work/ds-check.out" 2>&1 || code=$?; echo "$code") $(grep -c 'no_such_tag_zz' "$work/ds-check.out" || true) $(grep -c 'no_such_string_zz' "$work/ds-check.out" || true)"
ds_repo_files="$(ls feature/*/src/deviceScenarios/*.txt app/src/deviceScenarios/*.txt 2> /dev/null || true)"
# shellcheck disable=SC2086
# The template has no scenario file of its own; an app's files are checked here as soon as it adds them.
expect "device-scenario: the scenarios of the repository pass check" 0 \
  "$(code=0; [[ -z "$ds_repo_files" ]] || python3 -I scripts/device-scenario.py check $ds_repo_files > "$work/ds-check.out" 2>&1 || code=$?; echo "$code")"
# The example of docs/ops/test-sessions.md, section 5, runs on the pose library: it must keep passing check.
sed -n '/^ticket: T-NNN$/,/^shot landscape$/p' docs/ops/test-sessions.md | sed 's/^ticket: T-NNN$/ticket: T-001/' > "$ds_files/doc-example.txt"
expect "device-scenario: the example of test-sessions.md passes check" "0 21" \
  "$(code=0; python3 -I scripts/device-scenario.py check "$ds_files/doc-example.txt" > "$work/ds-check.out" 2>&1 || code=$?; echo "$code") $(grep -c . "$ds_files/doc-example.txt")"
# A testTag that only a test still names is gone from the app: check does not count test sources.
mkdir -p "$work/ds-root/feature/x/src/test/kotlin" "$work/ds-root/feature/x/src/main/kotlin"
printf 'val t = "only_in_test"\n' > "$work/ds-root/feature/x/src/test/kotlin/T.kt"
printf 'val m = "in_main"\n' > "$work/ds-root/feature/x/src/main/kotlin/M.kt"
ds_script tags.txt 'expect id=only_in_test' 'expect id=in_main'
expect "device-scenario: check ignores tags that only test sources name" "1 0" \
  "$(python3 -I -c 'import importlib.util, sys
from pathlib import Path
spec = importlib.util.spec_from_file_location("ds", "scripts/device-scenario.py"); m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
out = m.check_files([sys.argv[1]], Path(sys.argv[2]))
print(sum("only_in_test" in x for x in out), sum("in_main" in x for x in out))' "$ds_files/tags.txt" "$work/ds-root")"
# The first screen of `consent decline` is the root tag of MainActivity, so it exists in the shell of the app.
expect "device-scenario: the default first screen is the root tag of MainActivity" "1 1" \
  "$(grep -c '"id=app_root"' scripts/device-scenario.py || true) $(grep -c 'testTag(APP_ROOT_TAG)' app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt || true)"
ds_script first.txt 'consent decline 2'
expect "device-scenario: DEVICE_SCENARIO_FIRST_SCREENS names another first screen" "0 1 0 1" \
  "$(ds_run "grid.xml" "$ds_files/first.txt" DEVICE_SCENARIO_FIRST_SCREENS='id=gallery_screen') $(ds_count 'result=đạt') $(ds_run "grid.xml" "$ds_files/first.txt") $(ds_count 'result=lỗi')"
ds_script fresh.txt 'fresh'
expect "device-scenario: fresh opens MainActivity when the build has two LAUNCHER activities" "0 1 0" \
  "$(ds_run "home.xml" "$ds_files/fresh.txt" FAKE_ADB_LAUNCHERS="com.example.app.staging/com.example.app.ads.AdPreviewActivity com.example.app.staging/com.example.app.MainActivity") $(ds_count '^shell am start -W -n com.example.app.staging/com.example.app.MainActivity$' "$ds_calls") $(ds_count 'resolve-activity' "$ds_calls")"
empty_code="$(ds_run "home.xml" "$work/no-scenarios/*.txt")"
empty_calls="$(wc -l < "$ds_calls" | tr -d ' ')"
ds_run "consent.xml welcome.xml home.xml" "$ds_fixtures/scenarios/ok.txt" > /dev/null
expect "device-scenario: an empty glob exits before any adb call (a file makes calls)" "0 0 yes" \
  "$(joined "$empty_code" "$empty_calls" "$([[ "$(wc -l < "$ds_calls")" -gt 0 ]] && echo yes || echo no)")"

# From camera-pose T-048, T-049, T-052 (issues #163, #188): fresh, late consent, selected on a parent, settle after
# device, notes, quotes, file names, evidence reserve, SIGTERM, unfold on small screens, stop settle.
ds_n() { cat "$work/adb-state/ui-seq.n" 2> /dev/null || echo 0; } # dumps taken by the last ds_run
ds_check() { # <file> [VAR=value ...]; prints the exit code of check, output in $work/ds-check.out
  local code=0
  env "${@:2}" python3 -I scripts/device-scenario.py check "$1" > "$work/ds-check.out" 2>&1 || code=$?
  echo "$code"
}
ds_in_order() { # <file> <pattern...>: yes when the first line of each pattern comes after the one before
  local file="$1" last=0 n pattern
  shift
  for pattern in "$@"; do
    n="$(grep -n -- "$pattern" "$file" 2> /dev/null | head -n 1 | cut -d: -f1 || true)"
    if [[ -z "$n" || "$n" -le "$last" ]]; then echo no; return; fi
    last="$n"
  done
  echo yes
}

expect "device-scenario: fresh clears the app and starts it with am start -W" "0 yes 1 1" \
  "$(ds_run "home.xml" "$ds_files/fresh.txt") $(ds_in_order "$ds_calls" '^shell am force-stop com.example.app.staging$' \
    '^shell pm clear com.example.app.staging$' '^shell am start -W -n com.example.app/.MainActivity$' \
    '^shell pidof com.example.app.staging$') $(ds_count '^plan fresh.txt step=1 fresh ok$') $(ds_count '^shell am start -W' "$ds_calls")"
expect "device-scenario: fresh starts the app again when the first start left no process" "0 2 1 1 1" \
  "$(ds_run "home.xml" "$ds_files/fresh.txt" FAKE_ADB_DEAD_STARTS=1 DEVICE_SCENARIO_PROCESS_WAIT=0.3) $(ds_count '^shell am start -W' "$ds_calls") $(ds_count '^logcat -b crash -d$' "$ds_calls") $(ds_count '^plan fresh.txt step=1 fresh ok$') $(ds_count '^plan fresh.txt note step=1 t=[0-9.]* .*pidof rỗng.*mở lại')"
printf 'ticket: T-999\ncriterion: Hang\ntimeout: 60\nfresh\n' > "$ds_files/fresh-hang.txt"
ds_started=$SECONDS
ds_code="$(ds_run "home.xml" "$ds_files/fresh-hang.txt" FAKE_ADB_HANG="am start" DEVICE_SCENARIO_START_WAIT=1 DEVICE_SCENARIO_PROCESS_WAIT=0.3)"
expect "device-scenario: a hanging am start counts as no process and is retried" "0 1 2 1 yes" \
  "$ds_code $(ds_count '^plan fresh-hang.txt note step=1 .*am start -W hết 1 giây') $(ds_count '^shell am start -W' "$ds_calls") $(ds_count '^plan fresh-hang.txt step=1 fresh FAIL app không chạy sau 2 lần mở$') $([[ $((SECONDS - ds_started)) -lt 15 ]] && echo yes || echo no)"
expect "device-scenario: a slow am start with a live process is not restarted" "0 1 1 1 1" \
  "$(ds_run "home.xml" "$ds_files/fresh-hang.txt" FAKE_ADB_HANG="am start" DEVICE_SCENARIO_START_WAIT=1 FAKE_ADB_ALIVE_AFTER_HANG=1) $(ds_count '^shell am start -W' "$ds_calls") $(ds_count '^plan fresh-hang.txt step=1 fresh ok$') $(ds_count '^plan fresh-hang.txt note step=1 .*am start -W hết 1 giây') $(ds_count '^shell am force-stop' "$ds_calls")"
expect "device-scenario: fresh fails when the app never runs" "0 1 2" \
  "$(ds_run "home.xml" "$ds_files/fresh.txt" FAKE_ADB_DEAD_STARTS=5 DEVICE_SCENARIO_PROCESS_WAIT=0.3) $(ds_count '^plan fresh.txt step=1 fresh FAIL app không chạy sau 2 lần mở$') $(ds_count '^shell am start -W' "$ds_calls")"
printf '%s\n' '--------- beginning of crash' 'E AndroidRuntime: FATAL EXCEPTION: main' \
  'E AndroidRuntime: Process: com.example.app.staging, PID: 4242' > "$work/ds-crash.txt"
expect "device-scenario: fresh fails on a first-start crash instead of starting again" "0 1 1" \
  "$(ds_run "home.xml" "$ds_files/fresh.txt" FAKE_ADB_DEAD_STARTS=1 DEVICE_SCENARIO_PROCESS_WAIT=0.3 FAKE_ADB_CRASH_LOG="$work/ds-crash.txt") $(ds_count '^plan fresh.txt step=1 fresh FAIL app crash ở lần mở đầu$') $(ds_count '^shell am start -W' "$ds_calls")"
expect "device-scenario: fresh does not take an adb error with digits for a process" "0 2 1" \
  "$(ds_run "home.xml" "$ds_files/fresh.txt" FAKE_ADB_PIDOF_TEXT="error: device 'emulator-5554' not found" DEVICE_SCENARIO_PROCESS_WAIT=0.3) $(ds_count '^shell am start -W' "$ds_calls") $(ds_count '^plan fresh.txt step=1 fresh FAIL app không chạy sau 2 lần mở$')"
expect "device-scenario: control: pidof with two pids counts as a process" "0 1 1" \
  "$(ds_run "home.xml" "$ds_files/fresh.txt" FAKE_ADB_PIDOF_TEXT="4242 4243") $(ds_count '^shell am start -W' "$ds_calls") $(ds_count '^plan fresh.txt step=1 fresh ok$')"
# Stopping the app right after the device was given back makes the system kill the next start (camera-pose
# FB-20261025-02), so fresh of a file after a reset waits after stopping the app; the first file does not.
ds_script settle-1.txt 'fresh'
ds_script settle-2.txt 'fresh'
settle_began="$(date +%s)"
settle_code="$(ds_run "home.xml" "$ds_files/settle-*.txt" DEVICE_SCENARIO_STOP_SETTLE=2)"
settle_seconds=$(( $(date +%s) - settle_began ))
expect "device-scenario: fresh after a reset waits after stopping the app, the first file does not" "0 1 0 yes" \
  "$settle_code $(ds_count '^plan settle-2.txt note step=1 .*fresh: chờ 2 giây sau khi dừng app') $(ds_count '^plan settle-1.txt note step=1 .*fresh: chờ') $([ "$settle_seconds" -ge 2 ] && echo yes || echo no)"
ds_script settle-mid.txt 'fresh' 'fresh'
expect "device-scenario: control: a second fresh inside one file does not wait" "0 0 2" \
  "$(ds_run "home.xml" "$ds_files/settle-mid.txt" DEVICE_SCENARIO_STOP_SETTLE=2) $(ds_count 'fresh: chờ') $(ds_count '^plan settle-mid.txt step=[12] fresh ok$')"

# A form later than the grace of consent decline: the next step that reads the screen declines it once. The grace is
# 0 here, so consent decline ends on the first screen of the app.
ds_late() { ds_run "$1" "$2" DEVICE_SCENARIO_CONSENT_GRACE=0 "${@:3}"; }
ds_script consent-late.txt 'consent decline 5' 'wait id=welcome_screen 10' 'expect id=home_screen'
expect "device-scenario: a consent form that shows after consent decline saw the app is declined once" "0 1 1 1 4" \
  "$(ds_late "welcome.xml consent.xml welcome.xml home.xml" "$ds_files/consent-late.txt") $(ds_count '^plan consent-late.txt result=đạt') $(ds_count '^shell input tap 540 1970$' "$ds_calls") $(ds_count '^plan consent-late.txt note step=2 t=[0-9.]* hộp thoại đồng ý hiện muộn, đã từ chối') $(ds_n)"
ds_script consent-twice.txt 'consent decline 5' 'wait id=welcome_screen 3' 'wait id=home_screen 1'
expect "device-scenario: a second late consent form is not declined again" "0 1 1 4" \
  "$(ds_late "welcome.xml consent.xml welcome.xml consent.xml" "$ds_files/consent-twice.txt") $(ds_count 'input tap' "$ds_calls") $(ds_count '^plan consent-twice.txt step=3 wait id=home_screen 1 FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối$') $(ds_n)"
ds_script consent-again.txt 'consent decline 5' 'wait id=home_screen 3'
expect "device-scenario: a consent form that shows again after decline is a failure" "0 1 1 3" \
  "$(ds_late "consent.xml welcome.xml consent.xml" "$ds_files/consent-again.txt") $(ds_count 'input tap' "$ds_calls") $(ds_count '^plan consent-again.txt step=2 wait id=home_screen 3 FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối$') $(ds_n)"
ds_script consent-none.txt 'expect id=welcome_screen'
expect "device-scenario: without consent decline a consent form is not tapped" "0 0 1" \
  "$(ds_late "consent.xml" "$ds_files/consent-none.txt") $(ds_count 'input tap' "$ds_calls") $(ds_count '^plan consent-none.txt step=1 expect id=welcome_screen FAIL')"
ds_script consent-fresh.txt 'fresh' 'consent decline 5' 'fresh' 'wait id=welcome_screen 3'
expect "device-scenario: fresh resets the consent state" "0 1 1 4" \
  "$(ds_late "consent.xml welcome.xml consent.xml welcome.xml" "$ds_files/consent-fresh.txt") $(ds_count '^plan consent-fresh.txt result=đạt') $(ds_count 'input tap' "$ds_calls") $(ds_n)"
mkdir -p "$work/ds-two"
printf 'ticket: T-999\ncriterion: A\ntimeout: 30\nconsent decline 5\n' > "$work/ds-two/a.txt"
printf 'ticket: T-999\ncriterion: B\ntimeout: 30\nexpect id=home_screen\n' > "$work/ds-two/b.txt"
expect "device-scenario: nothing kept crosses into the next file" "0 1 1 3" \
  "$(ds_late "consent.xml welcome.xml home.xml" "$work/ds-two/*.txt") $(ds_count '^plan a.txt result=đạt') $(ds_count '^plan b.txt result=đạt') $(ds_n)"
ds_script consent-back.txt 'consent decline 5' 'back' 'expect id=home_screen'
expect "device-scenario: a kept screen is not used after back" "0 1 3" \
  "$(ds_late "consent.xml welcome.xml home.xml" "$ds_files/consent-back.txt") $(ds_count '^plan consent-back.txt result=đạt') $(ds_n)"
ds_script consent-close.txt 'consent decline 5' 'expect id=welcome_screen'
expect "device-scenario: consent decline waits for the form to close and taps once" "0 1 1 4" \
  "$(ds_late "consent.xml consent.xml consent.xml welcome.xml" "$ds_files/consent-close.txt") $(ds_count 'input tap' "$ds_calls") $(ds_count '^plan consent-close.txt result=đạt') $(ds_n)"
ds_script consent-need.txt 'consent decline 5' 'need id=x 3'
expect "device-scenario: a consent error inside need is a failure, not a skip" "0 1 1 3" \
  "$(ds_late "consent.xml welcome.xml consent.xml" "$ds_files/consent-need.txt") $(ds_count '^plan consent-need.txt result=lỗi ') $(ds_count '^plan consent-need.txt step=2 need id=x 3 FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối$') $(ds_n)"
expect "device-scenario: a consent form that does not close after the tap is a failure" "0 1 1" \
  "$(ds_late "consent.xml" "$ds_files/consent.txt" DEVICE_SCENARIO_CONSENT_CLOSE=1) $(ds_count '^plan consent.txt step=1 consent decline 5 FAIL hộp thoại đồng ý không đóng sau khi chạm từ chối$') $(ds_count 'input tap' "$ds_calls")"
ds_script consent-second-form.txt 'consent decline 5' 'consent decline 5'
expect "device-scenario: a second consent decline that sees the form again after a decline fails, tapping once" "0 1 1" \
  "$(ds_late "consent.xml welcome.xml consent.xml welcome.xml" "$ds_files/consent-second-form.txt") $(ds_count 'input tap' "$ds_calls") $(ds_count '^plan consent-second-form.txt step=2 consent decline 5 FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối$')"
ds_script consent-second-keep.txt 'consent decline 5' 'consent decline 5' 'wait id=home_screen 3'
expect "device-scenario: a second consent decline without a form keeps the decline, a form after it is a failure" "0 1 1" \
  "$(ds_late "consent.xml welcome.xml welcome.xml consent.xml" "$ds_files/consent-second-keep.txt") $(ds_count 'input tap' "$ds_calls") $(ds_count '^plan consent-second-keep.txt step=3 wait id=home_screen 3 FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối$')"
ds_script consent-second-none.txt 'consent decline 5' 'consent decline 5' 'expect id=welcome_screen'
expect "device-scenario: control: a second consent decline without a form passes" "0 1 1" \
  "$(ds_late "consent.xml welcome.xml welcome.xml" "$ds_files/consent-second-none.txt") $(ds_count 'input tap' "$ds_calls") $(ds_count '^plan consent-second-none.txt result=đạt')"

# selected=true of a Compose chip: the text is a child of the selected node (fixtures from a real dump of the app).
ds_script chip-all.txt 'expect text=All selected=true'
expect "device-scenario: selected=true matches a text whose parent chip is selected" "0 1" \
  "$(ds_run "chips.xml" "$ds_files/chip-all.txt") $(ds_count '^plan chip-all.txt result=đạt')"
ds_script chip-saved.txt 'expect text=Saved selected=true'
expect "device-scenario: selected=true stops at the clickable parent of an unselected chip" "0 1 0 1" \
  "$(ds_run "chips.xml" "$ds_files/chip-saved.txt") $(ds_count '^plan chip-saved.txt result=lỗi ') $(ds_run "chips-saved.xml" "$ds_files/chip-saved.txt") $(ds_count '^plan chip-saved.txt result=đạt')"
ds_script chip-deep.txt 'expect desc=deep_clickable selected=true'
expect "device-scenario: a clickable node is selected only by itself" "0 1" \
  "$(ds_run "chips.xml" "$ds_files/chip-deep.txt") $(ds_count '^plan chip-deep.txt step=1 expect desc=deep_clickable selected=true FAIL')"
ds_script chip-far.txt 'expect text=Sâu selected=true'
expect "device-scenario: selected=true looks at most two parents up" "0 1" \
  "$(ds_run "chips.xml" "$ds_files/chip-far.txt") $(ds_count '^plan chip-far.txt step=1 expect text=Sâu selected=true FAIL')"

ds_script unfold.txt 'device unfold'
ds_script unfold-grid.txt 'device unfold' 'expect id=gallery_grid'
expect "device-scenario: a device command waits until two dumps agree" "0 1 3 1 0 0 1 3" \
  "$(ds_run "home.xml grid.xml grid.xml" "$ds_files/unfold.txt") $(ds_count '^plan unfold.txt result=đạt') $(ds_n) $(ds_count '^plan unfold.txt note step=1 t=[0-9.]* .*ổn định sau') $(ds_count 'không ổn định') $(ds_run "home.xml grid.xml grid.xml home.xml" "$ds_files/unfold-grid.txt") $(ds_count '^plan unfold-grid.txt result=đạt') $(ds_n)"
ds_alternating=""
for _ in $(seq 1 50); do ds_alternating+="home.xml grid.xml "; done
expect "device-scenario: a screen that never settles is noted, not failed" "0 1 1 yes" \
  "$(ds_run "$ds_alternating" "$ds_files/unfold.txt" DEVICE_SCENARIO_SETTLE=1) $(ds_count '^plan unfold.txt step=1 device unfold ok$') $(ds_count '^plan unfold.txt note step=1 t=[0-9.]* .*không ổn định') $([[ "$(ds_n)" -lt 100 ]] && echo yes || echo no)"
expect "device-scenario: a tap notes its coordinates and the element" "0 1 6" \
  "$(ds_run "consent.xml welcome.xml home.xml" "$ds_fixtures/scenarios/ok.txt") $(ds_count '^plan ok.txt note step=3 t=[0-9]*\.[0-9] .*540,1806.*welcome_continue') $(ds_count '^plan ok.txt step=[0-9]* .* ok$')"
expect "device-scenario: device unfold turns the letterbox education off before it changes the size" "0 1 yes" \
  "$(ds_run "home.xml grid.xml grid.xml" "$ds_files/unfold.txt") $(ds_count '^shell wm set-letterbox-style --isEducationEnabled false$' "$ds_calls") $(ds_in_order "$ds_calls" '^shell wm set-letterbox-style --isEducationEnabled false$' '^shell wm size 1766x2208$')"
expect "device-scenario: the reset after the file and at the end turns the letterbox education back to the default" "0 2 yes" \
  "$(ds_run "home.xml grid.xml grid.xml" "$ds_files/unfold.txt") $(ds_count '^shell wm reset-letterbox-style isEducationEnabled$' "$ds_calls") $(ds_in_order "$ds_calls" '^shell wm size 1766x2208$' '^shell wm reset-letterbox-style isEducationEnabled$')"
expect "device-scenario: on a 320x640 screen unfold picks a size within three times the screen, 673 x 841 dp" "0 1 1 0" \
  "$(ds_run "home.xml grid.xml grid.xml" "$ds_files/unfold.txt" FAKE_ADB_PHYSICAL_SIZE=320x640) $(ds_count '^shell wm size 1536x1919$' "$ds_calls") $(ds_count '^shell wm density 365$' "$ds_calls") $(ds_count '^shell wm size 1766x2208$' "$ds_calls")"
expect "device-scenario: control: on a large screen unfold keeps 1766x2208 at 420" "0 1 1" \
  "$(ds_run "home.xml grid.xml grid.xml" "$ds_files/unfold.txt") $(ds_count '^shell wm size 1766x2208$' "$ds_calls") $(ds_count '^shell wm density 420$' "$ds_calls")"

ds_script quoted.txt 'wait text="Hands in Pockets" 3'
expect "device-scenario: a quoted value keeps its spaces and the number after it is the timeout" "0 1 0" \
  "$(ds_run "chips.xml" "$ds_files/quoted.txt") $(ds_count '^plan quoted.txt result=đạt') $(ds_check "$ds_files/quoted.txt")"
ds_script page.txt 'wait text=Page 2'
ds_script grid-wait.txt 'wait id=gallery_grid 2'
mkdir -p "$work/ds-grid-root/feature/x/src/main"
printf 'val g = "gallery_grid"\n' > "$work/ds-grid-root/feature/x/src/main/G.kt"
ds_page_check="$(ds_check "$ds_files/page.txt")"
ds_page_line="$(grep -c 'page.txt dòng 4: .*ngoặc kép' "$work/ds-check.out" || true)"
expect "device-scenario: a text with spaces followed by a number is a file error" "1 1 0 1 0 0" \
  "$ds_page_check $ds_page_line $(ds_run "chips.xml" "$ds_files/page.txt") $(ds_count '^plan page.txt result=lỗi tệp') $(ds_count 'input' "$ds_calls") $(ds_check "$ds_files/grid-wait.txt" DEVICE_SCENARIO_ROOT="$work/ds-grid-root")"
ds_script one-word.txt 'wait desc=Back 10'
expect "device-scenario: an unquoted one-word desc= on a line that ends in a number is ambiguous, the message says so" "1 1" \
  "$(ds_check "$ds_files/one-word.txt") $(ds_count 'one-word.txt dòng 4: .*kể cả một từ.*ngoặc kép' "$work/ds-check.out")"
ds_script one-word-quoted.txt 'wait desc="Back" 10'
expect "device-scenario: control: the quoted one-word value passes check" 0 "$(ds_check "$ds_files/one-word-quoted.txt")"
mkdir -p "$work/ds-names"
printf 'ticket: T-999\ncriterion: Test\ntimeout: 30\nback\n' > "$work/ds-names/a b|c.txt"
expect "device-scenario: a file name with odd characters is a file error and sends no adb command" "0 1 1 0 1" \
  "$(ds_run "home.xml" "$work/ds-names/*.txt") $(ds_count '^plan a_b_c.txt step=0 check FAIL .*tên tệp') $(ds_count '^plan a_b_c.txt result=lỗi tệp') $(ds_count 'log -t' "$ds_calls") $(ds_check "$work/ds-names/a b|c.txt")"
printf 'ticket: T-902\ncriterion: Test\ntimeout: 30\nback\n' > "$ds_files/T-901-x.txt"
printf 'ticket: T-901\ncriterion: Test\ntimeout: 30\nback\n' > "$ds_files/T-901-y.txt"
expect "device-scenario: a T-NNN file whose ticket differs is a file error" "1 1 0 0" \
  "$(ds_check "$ds_files/T-901-x.txt") $(grep -c 'tên tệp bắt đầu bằng T-901' "$work/ds-check.out" || true) $(ds_check "$ds_files/T-901-y.txt") $(grep -c 'tên tệp' "$work/ds-check.out" || true)"

# The reset and the evidence (log marker, fail.png) are taken from the budget before the file starts.
ds_script reserve.txt 'wait id=never_shown 25'
ds_run "home.xml" "$ds_files/reserve.txt" DEVICE_SCENARIOS_BUDGET=9 DEVICE_SCENARIO_RESET_LIMIT=4 DEVICE_SCENARIO_EVIDENCE_RESERVE=3 > /dev/null
expect "device-scenario: the file deadline also leaves room for the evidence" 1 "$(ds_count '^plan reserve.txt result=lỗi seconds=2$')"
expect "device-scenario: a budget that only covers the reset and the evidence skips the file" 1 \
  "$(ds_run "home.xml" "$ds_files/reserve.txt" DEVICE_SCENARIOS_BUDGET=6 DEVICE_SCENARIO_RESET_LIMIT=4 DEVICE_SCENARIO_EVIDENCE_RESERVE=3 > /dev/null; ds_count '^plan reserve.txt result=bỏ qua (hết thời gian)')"

# SIGTERM from outside (timeout of device-session.sh) must not leave the adb child running (no pgrep -f).
ds_script term.txt 'wait id=x 90'
rm -rf "$work/adb-term" "$work/ds-term"
mkdir -p "$work/ds-term"
env PATH="$work/bin:$PATH" FAKE_ADB_STATE_DIR="$work/adb-term" FAKE_ADB_HANG=uiautomator DEVICE_SCENARIO_ADB_TIMEOUT=100 \
  DEVICE_SCENARIOS_GLOB="$ds_files/term.txt" DEVICE_SESSION_SLEEP=0 APP_PACKAGE=com.example.app.staging \
  python3 -I scripts/device-scenario.py run --session "$work/ds-term" --package com.example.app.staging \
  > "$work/ds-term.out" 2>&1 &
ds_term_pid=$!
for _ in $(seq 1 100); do
  [[ -s "$work/adb-term/hang.pid" ]] && break
  sleep 0.1
done
kill -TERM "$ds_term_pid" 2> /dev/null || true
ds_term_code=0
wait "$ds_term_pid" || ds_term_code=$?
ds_term_child="$(cat "$work/adb-term/hang.pid" 2> /dev/null || true)"
expect "device-scenario: a terminated runner kills its adb child and says so" "143 1 dead" \
  "$ds_term_code $(ds_count '^plan runner_terminated' "$work/ds-term/scenarios.txt") $(if [[ -z "$ds_term_child" ]]; then echo nopid; elif kill -0 "$ds_term_child" 2> /dev/null; then echo alive; else echo dead; fi)"
[[ -n "$ds_term_child" ]] && kill -9 "$ds_term_child" 2> /dev/null || true
# SIGTERM while the reset after a file runs: the run still gives the device back (size, density, network) before 143.
ds_script term-reset.txt 'device unfold'
rm -rf "$work/adb-term2" "$work/ds-term2"
mkdir -p "$work/ds-term2"
: > "$work/ds-term2-calls.txt"
env PATH="$work/bin:$PATH" FAKE_ADB_STATE_DIR="$work/adb-term2" FAKE_ADB_HANG="wm size reset" FAKE_ADB_HANG_ONCE=1 \
  FAKE_ADB_UI_SEQ="$ds_fixtures/home.xml" FAKE_ADB_CALLS="$work/ds-term2-calls.txt" FAKE_ADB_NETWORK_ON=100 \
  DEVICE_SCENARIOS_GLOB="$ds_files/term-reset.txt" DEVICE_SCENARIO_POLL=0.05 DEVICE_SESSION_SLEEP=0 \
  APP_PACKAGE=com.example.app.staging \
  python3 -I scripts/device-scenario.py run --session "$work/ds-term2" --package com.example.app.staging \
  > "$work/ds-term2.out" 2>&1 &
ds_term_pid=$!
for _ in $(seq 1 100); do
  [[ -s "$work/adb-term2/hang.pid" ]] && break
  sleep 0.1
done
kill -TERM "$ds_term_pid" 2> /dev/null || true
ds_term_code=0
wait "$ds_term_pid" || ds_term_code=$?
ds_term_child="$(cat "$work/adb-term2/hang.pid" 2> /dev/null || true)"
[[ -n "$ds_term_child" ]] && kill -9 "$ds_term_child" 2> /dev/null || true
expect "device-scenario: SIGTERM during the reset after a file still gives the device back" "143 2 1 yes" \
  "$ds_term_code $(ds_count '^shell wm size reset$' "$work/ds-term2-calls.txt") $(ds_count '^shell wm density reset$' "$work/ds-term2-calls.txt") $([[ "$(ds_count '^shell svc wifi enable$' "$work/ds-term2-calls.txt")" -ge 1 ]] && echo yes || echo no)"

# device-session.sh runs the plan scenarios after the standard ones and shows the result (plan T-046).
export DEVICE_SESSION_SLEEP=0 FAKE_ADB_CALLS="$work/adb-calls.txt" DEVICE_SCENARIO_POLL=0.05 FAKE_ADB_STATE_DIR="$work/adb-state-session" \
  FAKE_ADB_RUNTIME_PERMISSIONS="android.permission.CAMERA"
newest_session() { find "$work/sessions" -maxdepth 1 -name "TS-*" | sort | tail -n 1; }
rm -rf "$work/adb-state-session"
session "$fixtures/logcat-clean.txt" start ci > /dev/null
plan_code="$(DEVICE_SCENARIOS_GLOB="$ds_fixtures/scenarios/ok.txt" \
  FAKE_ADB_UI_SEQ="$ds_fixtures/consent.xml $ds_fixtures/welcome.xml $ds_fixtures/home.xml" session /dev/null scenarios --yes)"
plan_session="$(newest_session)"
monkey_line="$(grep -n '^monkey ' "$plan_session/scenarios.txt" | head -n 1 | cut -d: -f1 || true)"
plan_line="$(grep -n '^plan ok.txt' "$plan_session/scenarios.txt" | head -n 1 | cut -d: -f1 || true)"
expect "device-session: scenarios runs the plan scenarios after monkey" "0 yes" \
  "$(joined "$plan_code" "$([[ -n "$monkey_line" && -n "$plan_line" && "$monkey_line" -lt "$plan_line" ]] && echo yes || echo no)")"
session /dev/null stop > /dev/null
expect "device-session: the summary shows the plan scenario that ran" 1 \
  "$(grep -c '^| ok.txt | T-900 | Mẫu đạt / có dấu gạch | đạt |' "$plan_session/summary.md" || true)"

expect "device-session: helper network-off reuses the scenario function" "0 1" \
  "$(joined "$(session /dev/null helper network-off)" "$(FAKE_ADB_NETWORK=100 session /dev/null helper network-off)")"
helper_on="$(FAKE_ADB_NETWORK=100 session /dev/null helper network-on)"
helper_on_none="$(DEVICE_SESSION_SLEEP=0 session /dev/null helper network-on)"
helper_grant="$(session /dev/null helper grant-permissions)"
expect "device-session: helper network-on (waits for a default network) and grant-permissions" "0 1 0 1" \
  "$(joined "$helper_on" "$helper_on_none" "$helper_grant" "$(grep -c '^granted=CAMERA$' "$work/session.out" || true)")"
usage_code="$(session /dev/null)"
expect "device-session: Usage lists helper" "2 yes" \
  "$(joined "$usage_code" "$(grep -q 'helper' "$work/session.out" && echo yes || echo no)")"

# The summary reads scenarios.txt and the log; sessions are written by hand here so each case is exact.
mkplan() { # <dir> <scenarios.txt content> <logcat content>
  rm -rf "$1"
  mkdir -p "$1"
  printf '%s\n' "$2" > "$1/scenarios.txt"
  printf '%s\n' "$3" > "$1/logcat.txt"
}
plan_lines='plan ok.txt ticket=T-900 criterion=Mẫu đạt
plan ok.txt step=1 fresh ok
plan ok.txt result=đạt seconds=12
plan fail.txt ticket=T-901 criterion=Mẫu lỗi
plan fail.txt step=1 back ok
plan fail.txt step=2 expect id=x FAIL không thấy
plan fail.txt result=lỗi seconds=3
plan need.txt ticket=T-902 criterion=Mẫu thiếu
plan need.txt step=1 need id=y SKIP không thấy
plan need.txt result=bỏ qua (môi trường) seconds=2'
mkplan "$work/plan-summary" "$plan_lines" "$(cat "$fixtures/logcat-clean.txt")"
summary_code="$(session /dev/null summary "$work/plan-summary" com.example.app.staging)"
cell() { grep -cF -- "$1" "$work/plan-summary/summary.md" || true; }
expect "device-session: summary lists the plan scenarios" "0 1 1 1 1" \
  "$(joined "$summary_code" "$(cell '## Kịch bản theo plan')" \
    "$(cell '| ok.txt | T-900 | Mẫu đạt | đạt |  | 12 | 0 / 0 |')" \
    "$(cell '| fail.txt | T-901 | Mẫu lỗi | lỗi | 2: expect id=x | 3 | 0 / 0 |')" \
    "$(cell '| need.txt | T-902 | Mẫu thiếu | bỏ qua (môi trường) |  | 2 | 0 / 0 |')")"
# camera-pose T-054: the system kills the app when Google Play services dies (FB-20261025-03): the runner reads logcat.txt
# of the session after the marker of the run (fake-adb writes the marker, then FAKE_ADB_KILL_LINE) and runs the file
# once more; killed again, or without the time of a second run, the file is skipped.
ds_kill_line() { # <process of the app>: the line of the system killing it with Google Play services
  echo "10-25 19:53:18.629   598   631 I ActivityManager: Killing 9002:$1/u0a192 (adj 0): depends on provider com.google.android.gms/.fonts.provider.FontsProvider in dying proc com.google.android.gms.persistent"
}
ds_script killed.txt 'fresh'
ds_kill_app="$(ds_kill_line com.example.app.staging)"
expect "device-scenario: an app killed with Google Play services runs the file again, and the second run counts" "0 1 1 0 2 1" \
  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$ds_kill_app" FAKE_ADB_DEAD_STARTS=2 DEVICE_SCENARIO_PROCESS_WAIT=0.1) $(ds_count '^plan killed.txt step=1 fresh RETRY app bị hệ thống giết theo Google Play services') $(ds_count '^plan killed.txt result=đạt') $(ds_count ' FAIL') $(ds_count "scenario=plan file=killed.txt" "$ds_calls") $(ds_count '^plan killed.txt note step=1 .*chạy lại tệp từ đầu, mở lại app')"
expect "device-scenario: an app killed in both runs ends as skipped (environment)" "0 1 1 1 0" \
  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$ds_kill_app" FAKE_ADB_DEAD_STARTS=9 DEVICE_SCENARIO_PROCESS_WAIT=0.1) $(ds_count '^plan killed.txt step=1 fresh RETRY ') $(ds_count '^plan killed.txt step=1 fresh SKIP app bị hệ thống giết theo Google Play services') $(ds_count '^plan killed.txt result=bỏ qua (môi trường)') $(ds_count ' FAIL')"
expect "device-scenario: a process of the app (package:name) killed with Google Play services counts too" "0 1 1" \
  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$(ds_kill_line com.example.app.staging:remote)" FAKE_ADB_DEAD_STARTS=2 DEVICE_SCENARIO_PROCESS_WAIT=0.1) $(ds_count '^plan killed.txt step=1 fresh RETRY ') $(ds_count '^plan killed.txt result=đạt')"
expect "device-scenario: the kill of the first run does not count in the second run" "0 1 1 1" \
  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$ds_kill_app" FAKE_ADB_KILL_ONCE=1 FAKE_ADB_DEAD_STARTS=9 DEVICE_SCENARIO_PROCESS_WAIT=0.1) $(ds_count '^plan killed.txt step=1 fresh RETRY ') $(ds_count '^plan killed.txt step=1 fresh FAIL app không chạy sau 2 lần mở') $(ds_count '^plan killed.txt result=lỗi')"
# camera-pose T-073: the marker of the second run has not reached logcat.txt yet (FAKE_ADB_LOG_LINES=1): the kill after the
# marker of the first run is not this run's, so the file fails instead of being skipped.
expect "device-scenario: a second run whose marker is not in logcat.txt yet does not take the kill of the first run" "0 1 1 1 0 2 1" \
  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$ds_kill_app" FAKE_ADB_KILL_ONCE=1 FAKE_ADB_LOG_LINES=1 FAKE_ADB_DEAD_STARTS=9 DEVICE_SCENARIO_PROCESS_WAIT=0.1) $(ds_count '^plan killed.txt step=1 fresh RETRY ') $(ds_count '^plan killed.txt step=1 fresh FAIL app không chạy sau 2 lần mở') $(ds_count '^plan killed.txt result=lỗi') $(ds_count 'SKIP') $(ds_count "scenario=plan file=killed.txt" "$ds_calls") $(ds_count "scenario=plan file=killed.txt" "$work/ds-session/logcat.txt")"
ds_kill_line com.example.app.staging > "$work/ds-logcat-before.txt"
expect "device-scenario: control: a kill before the marker of the file or of another app is a plain failure" "0 1 0 1 1" \
  "$(ds_logcat="$work/ds-logcat-before.txt" ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$(ds_kill_line com.other.app)" FAKE_ADB_DEAD_STARTS=9 DEVICE_SCENARIO_PROCESS_WAIT=0.1) $(ds_count '^plan killed.txt step=1 fresh FAIL app không chạy sau 2 lần mở') $(ds_count 'RETRY') $(ds_count '^plan killed.txt result=lỗi') $(ds_count "scenario=plan file=killed.txt" "$ds_calls")"
expect "device-scenario: a killed app without time for its resets and evidence is skipped at once" "0 1 0 1 1" \
  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$ds_kill_app" FAKE_ADB_DEAD_STARTS=9 DEVICE_SCENARIO_PROCESS_WAIT=0.1 DEVICE_SCENARIOS_BUDGET=60 DEVICE_SCENARIO_RESET_LIMIT=25 DEVICE_SCENARIO_EVIDENCE_RESERVE=20) $(ds_count '^plan killed.txt step=1 fresh SKIP app bị hệ thống giết theo Google Play services.*không còn thời gian chạy lại') $(ds_count 'RETRY') $(ds_count '^plan killed.txt result=bỏ qua (môi trường)') $(ds_count "scenario=plan file=killed.txt" "$ds_calls")"
expect "device-scenario: a killed app is not run again when what is left is less than the first run took" "0 1 0 1" \
  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$ds_kill_app" FAKE_ADB_DEAD_STARTS=9 DEVICE_SCENARIO_PROCESS_WAIT=1 DEVICE_SCENARIOS_BUDGET=12 DEVICE_SCENARIO_RESET_LIMIT=0 DEVICE_SCENARIO_EVIDENCE_RESERVE=0) $(ds_count '^plan killed.txt step=1 fresh SKIP .*không còn thời gian chạy lại') $(ds_count 'RETRY') $(ds_count '^plan killed.txt result=bỏ qua (môi trường)')"
# camera-pose T-054: str= reads res/values (English), so fresh sets the language of the app to English from Android 13; below
# 13 a device in another language is skipped (environment).
ds_script lang.txt 'fresh'
expect "device-scenario: fresh sets the language of the app to English before it starts the app" "0 1 yes" \
  "$(ds_run "camera.xml" "$ds_files/lang.txt") $(ds_count '^shell cmd locale set-app-locales com.example.app.staging --locales en$' "$ds_calls") $(ds_in_order "$ds_calls" '^shell pm clear' '^shell cmd locale set-app-locales com.example.app.staging --locales en$' '^shell am start -W')"
expect "device-scenario: below Android 13 a device in another language is skipped (environment)" "0 1 1 0" \
  "$(ds_run "camera.xml" "$ds_files/lang.txt" FAKE_ADB_SDK=30 FAKE_ADB_LOCALE=vi-VN) $(ds_count '^plan lang.txt step=1 fresh SKIP máy để ngôn ngữ vi-VN') $(ds_count '^plan lang.txt result=bỏ qua (môi trường)') $(ds_count '^shell am start -W' "$ds_calls")"
expect "device-scenario: control: below Android 13 a device in English runs without setting the language" "0 1 0" \
  "$(ds_run "camera.xml" "$ds_files/lang.txt" FAKE_ADB_SDK=30 FAKE_ADB_LOCALE=en-US) $(ds_count '^plan lang.txt result=đạt') $(ds_count 'set-app-locales com.example.app.staging --locales en$' "$ds_calls")"
expect "device-scenario: a device that refuses to set the language of the app is skipped (environment)" "0 1 1" \
  "$(ds_run "camera.xml" "$ds_files/lang.txt" FAKE_ADB_LOCALE_FAILS=1) $(ds_count '^plan lang.txt step=1 fresh SKIP không đặt được ngôn ngữ tiếng Anh cho app') $(ds_count '^plan lang.txt result=bỏ qua (môi trường)')"
# camera-pose T-054 (review notes of T-052): the note gives the real wait, and a device command before fresh also waits.
printf 'ticket: T-999\ncriterion: Test\ntimeout: 3\nfresh\n' > "$ds_files/settle-short-a.txt"
cp "$ds_files/settle-short-a.txt" "$ds_files/settle-short-b.txt"
expect "device-scenario: the note of fresh gives the real wait when the file ends first" "1 0" \
  "$(ds_run "camera.xml" "$ds_files/settle-short-*.txt" DEVICE_SCENARIO_STOP_SETTLE=10 > /dev/null; ds_count '^plan settle-short-b.txt note step=1 .*fresh: chờ [0-3]\(\.[0-9]\)\{0,1\} giây sau khi dừng app') $(ds_count 'fresh: chờ 10 giây')"
ds_script devfresh.txt 'device night on' 'fresh'
expect "device-scenario: fresh after a device command in the same file waits after stopping the app" "0 1" \
  "$(ds_run "camera.xml" "$ds_files/devfresh.txt" DEVICE_SCENARIO_STOP_SETTLE=2) $(ds_count '^plan devfresh.txt note step=2 .*fresh: chờ 2 giây sau khi dừng app')"

# Notes of the runner about a late consent form or a restarted app are listed under the table (camera-pose T-048).
mkplan "$work/plan-notes" "$plan_lines
plan ok.txt note step=1 t=0.4
plan ok.txt note step=2 t=3.1 hộp thoại đồng ý hiện muộn, đã từ chối" "$(cat "$fixtures/logcat-clean.txt")"
notes_code="$(session /dev/null summary "$work/plan-notes" com.example.app.staging)"
notes_cell() { grep -cF -- "$1" "$work/plan-notes/summary.md" || true; }
expect "device-session: summary lists the runner notes about consent and restarts" "0 1 1 0 0" \
  "$(joined "$notes_code" "$(notes_cell 'Ghi chú của trình chạy:')" \
    "$(notes_cell '- ok.txt, bước 2: hộp thoại đồng ý hiện muộn, đã từ chối')" "$(notes_cell '- ok.txt, bước 1')" \
    "$(cell 'Ghi chú của trình chạy')")"
# camera-pose T-070: the system shade the runner closed is listed too, so a reader sees why a step took longer.
mkplan "$work/plan-shade" "$plan_lines
plan ok.txt note step=2 t=0.4 bảng của hệ thống che app, đã đóng (cmd statusbar collapse)" "$(cat "$fixtures/logcat-clean.txt")"
shade_code="$(session /dev/null summary "$work/plan-shade" com.example.app.staging)"
expect "device-session: summary lists the system shade the runner closed" "0 1" \
  "$(joined "$shade_code" "$(grep -cF -- '- ok.txt, bước 2: bảng của hệ thống che app, đã đóng (cmd statusbar collapse)' "$work/plan-shade/summary.md" || true)")"
# camera-pose T-054: a file run again after the system killed the app: the table has the second result and no failing step,
# and the note of the run again is listed (it says "mở lại").
mkplan "$work/plan-retry" 'plan killed.txt ticket=T-022 criterion=Mẫu
plan killed.txt note step=2 t=30.1 app bị hệ thống giết theo Google Play services (com.google.android.gms.persistent) (logcat.txt); chạy lại tệp từ đầu, mở lại app
plan killed.txt step=2 consent decline 30 RETRY app bị hệ thống giết theo Google Play services (com.google.android.gms.persistent)
plan killed.txt note step=1 t=0.1
plan killed.txt step=1 fresh ok
plan killed.txt result=đạt seconds=95' "$(cat "$fixtures/logcat-clean.txt")"
retry_code="$(session /dev/null summary "$work/plan-retry" com.example.app.staging)"
expect "device-session: control: a file run again shows its second result, no failing step, and the note" "0 1 1" \
  "$retry_code $(grep -cF '| killed.txt | T-022 | Mẫu | đạt |  | 95 |' "$work/plan-retry/summary.md" || true) $(grep -cF -- '- killed.txt, bước 2: app bị hệ thống giết theo Google Play services' "$work/plan-retry/summary.md" || true)"
expect "device-session: the runner is called with timeout -k" 1 "$(grep -c 'timeout -k 30' scripts/device-session.sh || true)"
mkplan "$work/plan-pipe" 'plan ok.txt ticket=T-900 criterion=a | b
plan ok.txt result=đạt seconds=1' "$(cat "$fixtures/logcat-clean.txt")"
session /dev/null summary "$work/plan-pipe" com.example.app.staging > /dev/null
expect "device-session: a criterion with a pipe keeps the table" "1 9" \
  "$(joined "$(grep -cF '| ok.txt | T-900 | a / b | đạt |' "$work/plan-pipe/summary.md" || true)" \
    "$(grep -F '| ok.txt |' "$work/plan-pipe/summary.md" | awk -F'|' '{print NF}')")"
mkplan "$work/plan-none" 'monkey events=1500 seed=1' "$(cat "$fixtures/logcat-clean.txt")"
session /dev/null summary "$work/plan-none" com.example.app.staging > /dev/null
expect "device-session: summary says there are no plan scenarios" 1 \
  "$(grep -c '^Kịch bản theo plan: không có$' "$work/plan-none/summary.md" || true)"
mkdir -p "$work/no-scenarios"
session "$fixtures/logcat-clean.txt" start ci > /dev/null
empty_code="$(DEVICE_SCENARIOS_GLOB="$work/no-scenarios/*.txt" session /dev/null scenarios --yes)"
empty_session="$(newest_session)"
session /dev/null stop > /dev/null
expect "device-session: an empty scenario folder gives the no-plan-scenarios line" "0 1" \
  "$(joined "$empty_code" "$(grep -c '^Kịch bản theo plan: không có$' "$empty_session/summary.md" || true)")"

crash_log='10-17 09:00:00.100  1234  1234 I VGL.Session: session=start name=ci
10-17 09:00:01.000  1234  1234 I VGL.Session: scenario=plan file=ok.txt
10-17 09:00:05.000  4321  4321 E AndroidRuntime: FATAL EXCEPTION: main
10-17 09:00:05.000  4321  4321 E AndroidRuntime: Process: com.example.app.staging, PID: 4321
10-17 09:00:06.000  1234  1234 I VGL.Session: scenario=plan file=fail.txt
10-17 09:00:07.000  4321  4321 I VGL.loading: state=in_progress'
printf '%s\n' "$crash_log" > "$work/plan-crash.log"
session "$work/plan-crash.log" start ci > /dev/null
crash_session="$(newest_session)"
printf '%s\n' 'plan ok.txt ticket=T-900 criterion=A' 'plan ok.txt result=đạt seconds=4' \
  'plan fail.txt ticket=T-901 criterion=B' 'plan fail.txt result=đạt seconds=1' > "$crash_session/scenarios.txt"
crash_code="$(session /dev/null stop)"
cell() { grep -cF -- "$1" "$crash_session/summary.md" || true; }
expect "device-session: a crash during a plan scenario is blocking and named" "1 1 1 1" \
  "$(joined "$crash_code" "$(cell '| ok.txt | T-900 | A | đạt |  | 4 | 1 / 0 |')" \
    "$(cell '| fail.txt | T-901 | B | đạt |  | 1 | 0 / 0 |')" "$(cell '| App dừng đột ngột (crash) | 1 | chặn |')")"

session "$fixtures/logcat-clean.txt" start ci > /dev/null
runner_code="$(DEVICE_SCENARIO_CMD=false session /dev/null scenarios --yes)"
runner_session="$(newest_session)"
session /dev/null stop > /dev/null
expect "device-session: a runner error does not stop scenarios" "0 1" \
  "$(joined "$runner_code" "$(grep -c '^plan runner_error' "$runner_session/scenarios.txt" || true)")"
unset DEVICE_SESSION_SLEEP FAKE_ADB_CALLS DEVICE_SCENARIO_POLL FAKE_ADB_STATE_DIR FAKE_ADB_RUNTIME_PERMISSIONS

# app-explore: the guard refuses taps that would open an ad or start a purchase (docs/ops/clone-study.md).
ax_fixtures=scripts/tests/fixtures/app-explore
guard() { # <fixture> <top activity> <x> <y>; prints the exit code
  local code=0
  python3 -I scripts/app-explore/guard.py check "$ax_fixtures/$1" "$2" "$3" "$4" 2> /dev/null || code=$?
  echo "$code"
}
expect "app-explore: a normal button is tappable" 0 "$(guard ads.xml app 540 1800)"
expect "app-explore: the media of a native ad is refused" 4 "$(guard ads.xml app 540 600)"
expect "app-explore: the install button of an ad is refused" 4 "$(guard ads.xml app 540 1350)"
expect "app-explore: the close button of an ad is tappable" 0 "$(guard ads.xml app 944 136)"
expect "app-explore: a button drawn on top of an ad is tappable" 0 "$(guard ads.xml app 900 250)"
expect "app-explore: the collapse button of a banner is tappable" 0 "$(guard ads.xml app 1025 2044)"
expect "app-explore: every tap is refused on a full-screen ad activity" 4 \
  "$(guard ads.xml 'topResumedActivity=com.google.android.gms.ads.AdActivity' 540 1800)"
expect "app-explore: every tap is refused on the Play billing screen" 4 \
  "$(guard paywall.xml 'topResumedActivity=com.android.vending/.billing.Acquire' 95 155)"
expect "app-explore: the buy button of a paywall is refused" 4 "$(guard paywall.xml app 540 2213)"
expect "app-explore: Continue on a paywall is refused" 4 "$(guard paywall.xml app 540 1950)"
expect "app-explore: the close button of a paywall is tappable" 0 "$(guard paywall.xml app 95 155)"
expect "app-explore: Continue on an onboarding page is tappable" 0 "$(guard onboarding.xml app 540 2290)"
expect "app-explore: the close button of an ad is found" "944 136 ad_close" \
  "$(python3 -I scripts/app-explore/guard.py close "$ax_fixtures/ads.xml")"
expect "app-explore: taptext prefers the exact label (Agree, not Disagree)" "788 2066" \
  "$(python3 -I scripts/app-explore/tree.py --find agree "$ax_fixtures/onboarding.xml")"
expect "app-explore: Upgrade outside a paywall only opens it, so it is tappable" 0 "$(guard settings.xml app 540 375)"
expect "app-explore: the unlabeled X of a full-screen native ad is found" "1000 120 unlabeled-corner" \
  "$(python3 -I scripts/app-explore/guard.py close "$ax_fixtures/native-full.xml")"
expect "app-explore: taptext prefers the app's own button over an ad button with the same label" "945 2315" \
  "$(python3 -I scripts/app-explore/guard.py find "Cài đặt" "$ax_fixtures/native-full.xml")"
expect "app-explore: an empty id is refused" 2 \
  "$(python3 -I scripts/app-explore/tree.py --findid "" "$ax_fixtures/ads.xml" > /dev/null; echo $?)"
ax() { # runs ax.sh against fake-adb serving a UI dump; prints the exit code
  local code=0
  PATH="$work/bin:$PATH" FAKE_ADB_UI="$ax_fixtures/$1" FAKE_ADB_CALLS="$work/ax-calls.txt" AX_SETTLE=0.4 \
    AX_INCIDENTS="$work/incidents.log" scripts/app-explore/ax.sh "${@:2}" > "$work/ax.out" 2>&1 || code=$?
  echo "$code"
}
rm -f "$work/ax-calls.txt"
expect "app-explore: seq runs every step in one call" 0 "$(ax settings.xml seq "tapid btn_capture; wait 0; back")"
expect "app-explore: seq sends the tap and the back key" 2 \
  "$(grep -cE '^shell input (tap 540 1810|keyevent 4)$' "$work/ax-calls.txt")"
expect "app-explore: seq stops at a refused step" 4 "$(ax paywall.xml seq "tapid pay_purchase_btn; back")"
expect "app-explore: seq names the step it stopped at" 1 "$(grep -c 'DỪNG ở bước 1' "$work/ax.out")"
expect "app-explore: a drag from the screen edge is refused" 4 "$(ax settings.xml drag 20 1200 600 1200)"
printf '/sdcard/DCIM/Camera/a.jpg\n' > "$work/files.txt"
FAKE_ADB_NETWORK=100 FAKE_ADB_FILES="$work/files.txt" AX_FILES="$work/before.txt" ax settings.xml check > /dev/null
printf '/sdcard/DCIM/Camera/a.jpg\n/sdcard/Pictures/App/new.jpg\n' > "$work/files.txt"
FAKE_ADB_FILES="$work/files.txt" AX_FILES="$work/before.txt" ax settings.xml newfiles > /dev/null
expect "app-explore: newfiles lists only files added since the first check" "/sdcard/Pictures/App/new.jpg" \
  "$(grep '^/sdcard' "$work/ax.out")"
expect "app-explore: the tree shows sizes in dp" 1 \
  "$(AX_DENSITY=440 python3 -I scripts/app-explore/tree.py "$ax_fixtures/ads.xml" | grep -c 'btn_capture' )"

# save-branch.sh pushes a task branch, retries network errors, and bundles the branch when the push fails (L-081).
# A bare repository stands in for GitHub; its pre-receive hook makes the push fail in the way a test needs.
# The header of this file points every origin at a fake GitHub URL; this block needs the real bare repository (L-082).
sb_git_config=("$GIT_CONFIG_COUNT" "$GIT_CONFIG_KEY_0" "$GIT_CONFIG_VALUE_0")
unset GIT_CONFIG_COUNT GIT_CONFIG_KEY_0 GIT_CONFIG_VALUE_0
sb_root="$PWD"
sb_origin="$work/sb-origin.git"
sb_clone="$work/sb-clone"
git init -q --bare "$sb_origin"
git init -q -b main "$sb_clone"
git -C "$sb_clone" config user.email t@example.com
git -C "$sb_clone" config user.name t
git -C "$sb_clone" commit -q --allow-empty -m base
git -C "$sb_clone" remote add origin "$sb_origin"
git -C "$sb_clone" push -q origin main
git -C "$sb_clone" fetch -q origin
git -C "$sb_clone" checkout -q -b claude/T-999-thu
git -C "$sb_clone" commit -q --allow-empty -m one
git -C "$sb_clone" commit -q --allow-empty -m two
sb_head="$(git -C "$sb_clone" rev-parse HEAD)"
sb_hook="$sb_origin/hooks/pre-receive"
sb_calls="$work/sb-hook.calls"
sb_hook_mode() { # reject | network | none | flaky | raced
  rm -f "$sb_calls" "$sb_calls.count"
  case "$1" in
    none) rm -f "$sb_hook" ;;
    # Another push of the same commit (push-on-commit.sh) wins the race: the ref moves, this push is refused.
    raced) printf '#!/bin/sh\nread old new ref\nenv -u GIT_QUARANTINE_PATH git update-ref "$ref" "$new"\necho "remote: cannot lock ref" >&2\nexit 1\n' > "$sb_hook" ;;
    reject) printf '#!/bin/sh\necho x >> "%s"\necho "remote: denied" >&2\nexit 1\n' "$sb_calls" > "$sb_hook" ;;
    network) printf '#!/bin/sh\necho x >> "%s"\necho "fatal: The remote end hung up unexpectedly" >&2\nexit 1\n' "$sb_calls" > "$sb_hook" ;;
    flaky) printf '#!/bin/sh\necho x >> "%s"\nif [ -f "%s.count" ]; then exit 0; fi\ntouch "%s.count"\necho "fatal: The remote end hung up unexpectedly" >&2\nexit 1\n' \
      "$sb_calls" "$sb_calls" "$sb_calls" > "$sb_hook" ;;
  esac
  [ "$1" = none ] || chmod +x "$sb_hook"
}
sb_run() { # <args>; runs save-branch.sh in the clone; output in $work/sb.out, prints the exit code
  local code=0
  (cd "$sb_clone" && SAVE_BRANCH_DIR="$work/rescue" SAVE_BRANCH_RETRY_SECONDS="${SB_RETRY:-0}" \
    "$sb_root/scripts/save-branch.sh" "$@") > "$work/sb.out" 2>&1 || code=$?
  echo "$code"
}
sb_calls_count() { if [ -f "$sb_calls" ]; then wc -l < "$sb_calls" | tr -d ' '; else echo 0; fi; }
sb_remote_head() { git -C "$sb_origin" rev-parse --verify -q refs/heads/claude/T-999-thu || echo none; }
sb_bundle="$work/rescue/claude__T-999-thu.bundle"

sb_hook_mode none
expect "save-branch: pushes the branch and says so" 0 "$(sb_run claude/T-999-thu)"
expect "save-branch: the pushed branch is on the remote" "$sb_head" "$(sb_remote_head)"
expect "save-branch: says ĐÃ ĐẨY" 1 "$(grep -c 'ĐÃ ĐẨY' "$work/sb.out" || true)"
git -C "$sb_origin" update-ref -d refs/heads/claude/T-999-thu
sb_hook_mode reject
expect "save-branch: a refused push is not reported as pushed" 3 "$(sb_run claude/T-999-thu)"
expect "save-branch: a refused push prints no ĐÃ ĐẨY" 0 "$(grep -c 'ĐÃ ĐẨY' "$work/sb.out" || true)"
expect "save-branch: a refused push leaves the remote without the branch" none "$(sb_remote_head)"
expect "save-branch: a refused push leaves a verified bundle" ok \
  "$(git -C "$sb_clone" bundle verify "$sb_bundle" > /dev/null 2>&1 && echo ok || echo bad)"
expect "save-branch: the bundle holds the branch head" "$sb_head refs/heads/claude/T-999-thu" \
  "$(git bundle list-heads "$sb_bundle" | head -n 1)"
expect "save-branch: the bundle needs main as its base" bad \
  "$(git init -q "$work/sb-empty" && cd "$work/sb-empty" && git bundle verify "$sb_bundle" > /dev/null 2>&1 && echo ok || echo bad)"
sb_restore="$work/sb-restore"
git clone -q "$sb_origin" "$sb_restore" 2> /dev/null
restore_cmd="$(grep -m1 '^git fetch ' "$work/sb.out" || true)"
(cd "$sb_restore" && eval "$restore_cmd") > /dev/null 2>&1 || true
expect "save-branch: the printed commands restore the branch" "$sb_head 2" \
  "$(git -C "$sb_restore" rev-parse -q --verify refs/heads/claude/T-999-thu || echo none) $(git -C "$sb_restore" rev-list --count origin/main..claude/T-999-thu 2> /dev/null || echo 0)"
for needle in "Nhánh chưa đẩy" "${sb_head:0:7}" "2 commit" "$sb_bundle"; do
  expect "save-branch: the rescue section has '$needle'" yes \
    "$(grep -qF -- "$needle" "$work/sb.out" && echo yes || echo no)"
done
rm -rf "$sb_bundle" "$sb_restore"
sb_hook_mode reject
expect "save-branch: --bundle-only exits 3" 3 "$(sb_run --bundle-only claude/T-999-thu)"
expect "save-branch: --bundle-only never pushes" 0 "$(sb_calls_count)"
expect "save-branch: --bundle-only leaves a bundle" ok "$(git -C "$sb_clone" bundle verify "$sb_bundle" > /dev/null 2>&1 && echo ok || echo bad)"
rm -f "$sb_bundle"
sb_hook_mode network
expect "save-branch: a network error is retried, then bundled" 3 "$(SB_RETRY="0 0" sb_run claude/T-999-thu)"
expect "save-branch: a network error calls the hook 3 times" 3 "$(sb_calls_count)"
expect "save-branch: a network error leaves a bundle" ok "$(git -C "$sb_clone" bundle verify "$sb_bundle" > /dev/null 2>&1 && echo ok || echo bad)"
rm -f "$sb_bundle"
sb_hook_mode flaky
expect "save-branch: a network error that clears is pushed" 0 "$(SB_RETRY="0 0" sb_run claude/T-999-thu)"
expect "save-branch: a cleared network error says ĐÃ ĐẨY" 1 "$(grep -c 'ĐÃ ĐẨY' "$work/sb.out" || true)"
git -C "$sb_origin" update-ref -d refs/heads/claude/T-999-thu
sb_hook_mode reject
expect "save-branch: a refusal by GitHub exits 3" 3 "$(SB_RETRY="0 0" sb_run claude/T-999-thu)"
expect "save-branch: a refusal by GitHub is not retried" 1 "$(sb_calls_count)"
rm -f "$sb_bundle"
# The push failed, but origin already has the very head commit (a parallel push of the same branch): pushed.
sb_hook_mode none
git -C "$sb_clone" push -q origin claude/T-999-thu:refs/heads/sb-objects
git -C "$sb_origin" update-ref -d refs/heads/claude/T-999-thu
sb_hook_mode raced
rm -rf "$work/rescue"
expect "save-branch: a push refused after a parallel push of the same commit is pushed" "0 1 $sb_head no" \
  "$(sb_run claude/T-999-thu) $(grep -c 'ĐÃ ĐẨY: claude/T-999-thu' "$work/sb.out" || true) $(sb_remote_head) $([ -e "$sb_bundle" ] && echo yes || echo no)"
git -C "$sb_origin" update-ref -d refs/heads/claude/T-999-thu
git -C "$sb_origin" update-ref -d refs/heads/sb-objects
git -C "$sb_clone" branch -q claude/T-998-nothing main
sb_hook_mode reject
expect "save-branch: a refused push with nothing beyond main exits 3" 3 "$(sb_run claude/T-998-nothing)"
expect "save-branch: nothing beyond main says CHƯA ĐẨY and không cần bundle" 1 "$(grep -c 'CHƯA ĐẨY.*không cần bundle' "$work/sb.out" || true)"
expect "save-branch: nothing beyond main prints no ĐÃ ĐẨY" 0 "$(grep -c 'ĐÃ ĐẨY' "$work/sb.out" || true)"
expect "save-branch: nothing beyond main leaves no bundle" none "$(ls "$work/rescue/claude__T-998-nothing.bundle" 2> /dev/null || echo none)"
sb_hook_mode none
expect "save-branch: the same branch pushes when nothing refuses" 0 "$(sb_run claude/T-998-nothing)"
expect "save-branch: that push says ĐÃ ĐẨY" 1 "$(grep -c 'ĐÃ ĐẨY' "$work/sb.out" || true)"
sb_hook_mode reject
expect "save-branch: refuses a branch outside claude/" 2 "$(sb_run main)"
expect "save-branch: a branch outside claude/is never pushed" 0 "$(sb_calls_count)"
expect "save-branch: refuses a branch that does not exist" 2 "$(sb_run claude/T-000-none)"
# A push that GitHub refuses for lack of the workflow scope is also handed off as a patch on the report branch,
# pushed there for a person to review and apply (PB-14, ADR-0032). Never with --bundle-only, never for another refusal.
sb_report="$work/sb-report"
sb_report_branch=claude/RUN-20261027-09-report
sb_hook_mode none
git -C "$sb_clone" worktree add -q -b "$sb_report_branch" "$sb_report" main
git -C "$sb_report" push -q -u origin "$sb_report_branch"
git -C "$sb_clone" checkout -q -b claude/T-997-patch main
printf 'one\n' > "$sb_clone/a.txt"
git -C "$sb_clone" add a.txt && git -C "$sb_clone" commit -q -m "T-997: a"
mkdir -p "$sb_clone/.github/workflows"
printf 'name: x\n' > "$sb_clone/.github/workflows/x.yml"
printf '\x00\x01\x02\xff' > "$sb_clone/b.bin"
git -C "$sb_clone" add .github/workflows/x.yml b.bin && git -C "$sb_clone" commit -q -m "T-997: workflow and binary"
sb_patch_head="$(git -C "$sb_clone" rev-parse HEAD)"
sb_mbox_rel="docs/runs/patches/RUN-20261027-09/claude__T-997-patch.mbox"
sb_hook_scope() { # <task | all | task-and-deny-report>: GitHub refusing a workflow change without the workflow scope
  rm -f "$sb_calls" "$sb_calls.count"
  local task="echo 'remote: refusing to allow an OAuth App to create or update workflow .github/workflows/x.yml without workflow scope' >&2; exit 1"
  local report=':'
  case "$1" in
    all) report="$task" ;;
    task-and-deny-report) report="echo 'remote: denied' >&2; exit 1" ;;
  esac
  printf '#!/bin/sh\nwhile read old new ref; do case "$ref" in refs/heads/claude/T-*) echo x >> "%s"; %s;; refs/heads/claude/RUN-*) %s;; esac; done\nexit 0\n' \
    "$sb_calls" "$task" "$report" > "$sb_hook"
  chmod +x "$sb_hook"
}
sb_report_has() { git -C "$sb_origin" cat-file -e "refs/heads/$sb_report_branch:$1" 2> /dev/null && echo yes || echo no; }
sb_report_commits() { git -C "$sb_origin" rev-list --count "main..$sb_report_branch"; }
sb_report_reset() { git -C "$sb_report" reset -q --hard main && git -C "$sb_report" push -q -f origin "$sb_report_branch"; }
sb_apply_check() { # <file of printed output> <branch>: runs the printed commands (but git push) in a fresh clone; prints the tree
  rm -rf "$work/sb-apply"
  git clone -q "$sb_origin" "$work/sb-apply" 2> /dev/null
  git -C "$work/sb-apply" config user.email p@example.com
  git -C "$work/sb-apply" config user.name p
  local cmds
  cmds="$(awk '/^- Patch trên nhánh báo cáo/ { on = 1; next } on && /^```$/ { n++; if (n == 2) exit; next } on && n == 1 && !/^git push/' "$1")"
  (cd "$work/sb-apply" && eval "$cmds") > /dev/null 2>&1 || true
  git -C "$work/sb-apply" rev-parse -q --verify "$2^{tree}" || echo none
}
sb_hook_scope task
expect "save-branch patch: a workflow-scope refusal still exits 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: the mbox is on the pushed report branch" yes "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: the report branch got one commit" 1 "$(sb_report_commits)"
expect "save-branch patch: the rescue section names the patch for a person to review" yes \
  "$(grep -qF -- "- Patch trên nhánh báo cáo, để người xem rồi tự quyết áp hay không: \`$sb_mbox_rel\`" "$work/sb.out" && echo yes || echo no)"
expect "save-branch patch: the bundle is still made" ok \
  "$(git -C "$sb_clone" bundle verify "$work/rescue/claude__T-997-patch.bundle" > /dev/null 2>&1 && echo ok || echo bad)"
expect "save-branch patch: the printed commands rebuild the same tree" "$(git -C "$sb_clone" rev-parse "$sb_patch_head^{tree}")" \
  "$(sb_apply_check "$work/sb.out" claude/T-997-patch)"
expect "save-branch patch: the rebuilt branch keeps both commit messages" "T-997: workflow and binary|T-997: a" \
  "$(git -C "$work/sb-apply" log --format=%s origin/main..claude/T-997-patch 2> /dev/null | paste -sd '|' -)"
expect "save-branch patch: the check worktree is removed" 2 "$(git -C "$sb_clone" worktree list | grep -c .)"
expect "save-branch patch: the same patch again exits 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: ... adds no second commit" 1 "$(sb_report_commits)"
expect "save-branch patch: ... still prints the commands" 1 "$(grep -c '| git am$' "$work/sb.out")"
sb_report_reset
sb_hook_mode reject
expect "save-branch patch: another refusal exits 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: another refusal writes no patch" no "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: another refusal says why" 1 "$(grep -c 'chỉ ghi khi GitHub từ chối vì quyền workflow' "$work/sb.out")"
sb_hook_scope task
rm -f "$sb_calls"
expect "save-branch patch: --bundle-only exits 3" 3 "$(sb_run --bundle-only claude/T-997-patch)"
expect "save-branch patch: --bundle-only never writes the patch" no "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: --bundle-only leaves the report worktree clean" "" "$(git -C "$sb_report" status --short)"
expect "save-branch patch: --bundle-only says it does not push in another form" 1 "$(grep -c 'không đẩy lại dưới dạng khác' "$work/sb.out")"
expect "save-branch patch: --bundle-only never pushes the task branch" 0 "$(sb_calls_count)"
printf 'session draft\n' > "$sb_report/notes.md"
git -C "$sb_report" add notes.md
expect "save-branch patch: other staged changes of the report worktree" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: ... stay staged, not committed" "A  notes.md" "$(git -C "$sb_report" status --short notes.md)"
git -C "$sb_report" rm -q --cached notes.md && rm -f "$sb_report/notes.md"
sb_report_reset
printf '#!/bin/sh\nexit 1\n' > "$sb_clone/.git/hooks/pre-commit" && chmod +x "$sb_clone/.git/hooks/pre-commit"
expect "save-branch patch: a commit that fails exits 3" 3 "$(sb_run claude/T-997-patch)"
rm -f "$sb_clone/.git/hooks/pre-commit"
expect "save-branch patch: a failed commit leaves no file and nothing staged" "" "$(git -C "$sb_report" status --short --untracked-files=all)"
expect "save-branch patch: a failed commit says so" 1 "$(grep -c 'Không commit được patch' "$work/sb.out")"
expect "save-branch patch: a patch over the limit is left out" 3 "$(SAVE_BRANCH_PATCH_MAX_BYTES=100 sb_run claude/T-997-patch)"
expect "save-branch patch: over the limit, no patch on the report branch" no "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: over the limit says so" 1 "$(grep -c 'lớn hơn giới hạn 100 byte' "$work/sb.out")"
sb_token="$(printf '%s%s' "gh""p_" "abcdefghijklmnopqrstuvwxyz0123456789")"
git -C "$sb_clone" checkout -q -b claude/T-996-secret main
printf 'token=%s\n' "$sb_token" > "$sb_clone/leak.txt"
git -C "$sb_clone" add leak.txt && git -C "$sb_clone" commit -q -m "T-996: leak"
expect "save-branch patch: a patch with a secret exits 3" 3 "$(sb_run claude/T-996-secret)"
expect "save-branch patch: a secret is never on the report branch" no \
  "$(sb_report_has docs/runs/patches/RUN-20261027-09/claude__T-996-secret.mbox)"
expect "save-branch patch: a secret is never left in the report worktree" "" "$(git -C "$sb_report" status --short --untracked-files=all)"
expect "save-branch patch: a secret says secret-scan" 1 "$(grep -c 'secret-scan.py thấy chuỗi giống bí mật' "$work/sb.out")"
expect "save-branch patch: the secret is not printed" 0 "$(grep -c "$sb_token" "$work/sb.out")"
git -C "$sb_clone" checkout -q -b claude/T-995-latin main
printf 'caf\xe9 token=%s\n' "$sb_token" > "$sb_clone/latin.txt"
git -C "$sb_clone" add latin.txt && git -C "$sb_clone" commit -q -m "T-995: latin-1"
expect "save-branch patch: a patch that is not UTF-8 exits 3" 3 "$(sb_run claude/T-995-latin)"
expect "save-branch patch: a patch that is not UTF-8 is never written" "" "$(git -C "$sb_report" status --short --untracked-files=all)"
expect "save-branch patch: a patch that is not UTF-8 says so" 1 "$(grep -c 'không phải UTF-8' "$work/sb.out")"
git -C "$sb_clone" checkout -q -b claude/T-994-merge main
printf 'on branch\n' > "$sb_clone/c.txt"
git -C "$sb_clone" add c.txt && git -C "$sb_clone" commit -q -m "T-994: c"
git -C "$sb_clone" checkout -q main
printf 'main moved\n' > "$sb_clone/m.txt"
git -C "$sb_clone" add m.txt && git -C "$sb_clone" commit -q -m "main: m"
git -C "$sb_clone" checkout -q claude/T-994-merge
# The merge of main also drops main's new file: replaying only the non-merge commits (git am) would keep it.
git -C "$sb_clone" merge -q --no-commit main > /dev/null 2>&1
git -C "$sb_clone" rm -q -f m.txt
git -C "$sb_clone" commit -q -m "merge main, without m.txt"
git -C "$sb_clone" push -q origin main 2> /dev/null; git -C "$sb_clone" fetch -q origin
sb_hook_scope task
expect "save-branch patch: a branch with a merge of main exits 3" 3 "$(sb_run claude/T-994-merge)"
expect "save-branch patch: a branch with a merge gets one combined diff" yes \
  "$(sb_report_has docs/runs/patches/RUN-20261027-09/claude__T-994-merge.diff)"
expect "save-branch patch: the diff commands rebuild the same tree" "$(git -C "$sb_clone" rev-parse 'claude/T-994-merge^{tree}')" \
  "$(sb_apply_check "$work/sb.out" claude/T-994-merge)"
git -C "$sb_clone" checkout -q -b claude/T-993-empty main
git -C "$sb_clone" commit -q --allow-empty -m "T-993: nothing"
expect "save-branch patch: a branch whose patch cannot rebuild it exits 3" 3 "$(sb_run claude/T-993-empty)"
expect "save-branch patch: ... writes no patch and says so" 1 "$(grep -c 'Không dựng được patch' "$work/sb.out")"
git -C "$sb_clone" checkout -q claude/T-997-patch
sb_report_reset
sb_hook_scope task-and-deny-report
expect "save-branch patch: report branch refused too exits 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: report branch refused says the patch is only local" 1 "$(grep -c 'nhánh báo cáo cũng không đẩy được' "$work/sb.out")"
expect "save-branch patch: report branch refused prints no apply commands" 0 "$(grep -c '| git am' "$work/sb.out")"
git -C "$sb_report" reset -q --hard main
git -C "$sb_report" commit -q --allow-empty -m "report: timeline"
sb_hook_scope all
expect "save-branch patch: the report branch itself is never patched onto itself" 3 "$(sb_run "$sb_report_branch")"
expect "save-branch patch: ... and says so" 1 "$(grep -c 'chính nhánh báo cáo' "$work/sb.out")"
git -C "$sb_clone" worktree remove --force "$sb_report"
sb_hook_scope task
sb_report_reset_remote() { git -C "$sb_clone" push -q -f origin "main:$sb_report_branch"; }
git -C "$sb_clone" branch -q -f "$sb_report_branch" main && sb_report_reset_remote
expect "save-branch patch: a local report branch without a worktree still gets the patch" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: ... pushed through a temporary worktree" yes "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: ... which is removed" 1 "$(git -C "$sb_clone" worktree list | grep -c .)"
git -C "$sb_clone" branch -q -D "$sb_report_branch"
expect "save-branch patch: no report branch at all still bundles" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: no report branch at all says so" 1 "$(grep -c 'Không có nhánh báo cáo' "$work/sb.out")"
git -C "$sb_clone" worktree add -q -b "$sb_report_branch" "$sb_report" main && sb_report_reset_remote
expect "save-branch patch: SAVE_BRANCH_REPORT_DIR picks the worktree" 3 \
  "$(SAVE_BRANCH_REPORT_DIR="$sb_report" sb_run claude/T-997-patch)"
expect "save-branch patch: with SAVE_BRANCH_REPORT_DIR the patch is pushed" yes "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: the commands say how to read the patch after the report PR merges" 1 \
  "$(grep -c 'thay `origin/claude/RUN-20261027-09-report` bằng `origin/main`' "$work/sb.out")"
sb_tracked="$(git -C "$sb_report" rev-parse -q --verify "HEAD:$sb_mbox_rel" || echo none)"
printf 'two\n' > "$sb_clone/a.txt"
git -C "$sb_clone" checkout -q claude/T-997-patch && git -C "$sb_clone" commit -q -am "T-997: a again"
printf '#!/bin/sh\nexit 1\n' > "$sb_clone/.git/hooks/pre-commit" && chmod +x "$sb_clone/.git/hooks/pre-commit"
expect "save-branch patch: a newer patch whose commit fails exits 3" 3 "$(SAVE_BRANCH_REPORT_DIR="$sb_report" sb_run claude/T-997-patch)"
rm -f "$sb_clone/.git/hooks/pre-commit"
expect "save-branch patch: ... leaves the report worktree clean" "" "$(git -C "$sb_report" status --short --untracked-files=all)"
expect "save-branch patch: ... keeps the patch already committed" "$sb_tracked" "$(git -C "$sb_report" hash-object "$sb_mbox_rel" 2> /dev/null || echo gone)"
git -C "$sb_clone" push -q -f origin "main:$sb_report_branch"
git -C "$sb_clone" fetch -q origin
git -C "$sb_origin" update-ref "refs/heads/$sb_report_branch" "$(git -C "$sb_origin" -c user.name=t -c user.email=t@example.com commit-tree -p "refs/heads/$sb_report_branch" -m remote "refs/heads/$sb_report_branch^{tree}")"
git -C "$sb_report" reset -q --hard main
git -C "$sb_report" commit -q --allow-empty -m "local timeline"
expect "save-branch patch: a report branch that diverged from GitHub exits 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: ... writes no patch" "" "$(git -C "$sb_report" status --short --untracked-files=all)"
expect "save-branch patch: ... says the branches differ" 1 "$(grep -c 'đã khác nhau' "$work/sb.out")"
git -C "$sb_clone" worktree add -q -b claude/RUN-20261027-10-report "$work/sb-report2" main
expect "save-branch patch: two report worktrees exit 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: ... pick neither" 1 "$(grep -c 'Có nhiều nhánh báo cáo' "$work/sb.out")"
git -C "$sb_clone" worktree remove --force "$work/sb-report2"
git -C "$sb_clone" worktree remove --force "$sb_report"
expect "save-branch patch: two local report branches without worktrees exit 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: ... pick neither" 1 "$(grep -c 'Có nhiều nhánh báo cáo' "$work/sb.out")"
expect "save-branch patch: ... and leave no worktree" 1 "$(git -C "$sb_clone" worktree list | grep -c .)"
git -C "$sb_clone" branch -q -D claude/RUN-20261027-10-report
git -C "$sb_clone" checkout -q claude/T-999-thu

# push-on-commit.sh pushes a task branch whenever it gets a new commit, for a bounded time (L-090, L-096).
# A fake push command records its calls, so no remote is needed.
pc_fake="$work/pc-save.sh"
printf '#!/bin/sh\necho "$1 $(git rev-parse --short=7 "refs/heads/$1")" >> "%s"\nexit "${PC_SAVE_CODE:-0}"\n' \
  "$work/pc.calls" > "$pc_fake"
chmod +x "$pc_fake"
pc_run() { # <seconds> <args>; runs push-on-commit.sh in the clone, prints the exit code
  local seconds="$1" code=0
  shift
  (cd "$sb_clone" && PUSH_ON_COMMIT_SAVE="$pc_fake" PUSH_ON_COMMIT_SECONDS="$seconds" \
    "$sb_root/scripts/push-on-commit.sh" "$@") > "$work/pc.out" 2>&1 || code=$?
  echo "$code"
}
pc_calls() { if [ -f "$work/pc.calls" ]; then wc -l < "$work/pc.calls" | tr -d ' '; else echo 0; fi; }
git -C "$sb_clone" commit -q --allow-empty -m pc-one
rm -f "$work/pc.calls"
expect "push-on-commit: a commit the remote does not have is pushed on the first read" "0 1" \
  "$(pc_run 0 claude/T-999-thu --interval 1) $(pc_calls)"
git -C "$sb_clone" update-ref refs/remotes/origin/claude/T-999-thu "$(git -C "$sb_clone" rev-parse HEAD)"
rm -f "$work/pc.calls"
expect "push-on-commit: nothing new, nothing pushed" "0 0" "$(pc_run 0 claude/T-999-thu --interval 1) $(pc_calls)"
rm -f "$work/pc.calls"
( sleep 1; git -C "$sb_clone" commit -q --allow-empty -m three ) &
expect "push-on-commit: a commit made while it watches is pushed, then it stops when the time is up" "0 1" \
  "$(pc_run 3 claude/T-999-thu --interval 1) $(pc_calls)"
wait
expect "push-on-commit: says when the time is up" 1 "$(grep -c '^HẾT GIỜ' "$work/pc.out")"
git -C "$sb_clone" commit -q --allow-empty -m four
rm -f "$work/pc.calls"
expect "push-on-commit: a failed push (save-branch exit 3) stops it with exit 3" "3 1" \
  "$(PC_SAVE_CODE=3 pc_run 30 claude/T-999-thu --interval 1) $(pc_calls)"
expect "push-on-commit: refuses a branch outside claude/" 2 "$(pc_run 0 main)"
expect "push-on-commit: refuses a bad interval" 2 "$(pc_run 0 claude/T-999-thu --interval x)"
export GIT_CONFIG_COUNT="${sb_git_config[0]}" GIT_CONFIG_KEY_0="${sb_git_config[1]}" GIT_CONFIG_VALUE_0="${sb_git_config[2]}"

# push-probe.sh tells at the start of a session whether it may push a change to .github/workflows/ (L-112).
# A bare repository stands in for GitHub; its pre-receive hook answers as GitHub does.
# The probe branch never runs a workflow: every workflow of the repository that runs on push runs on main only.
push_on_other_branches() { # <dir>: prints the workflows of <dir> with a push trigger not limited to main
  python3 -I - "$1" <<'PY'
import glob, re, sys
for f in sorted(glob.glob(sys.argv[1] + "/*.yml")):
    text = open(f, encoding="utf-8").read()
    on = re.search(r"^on:\n((?:[ #].*\n|\n)*)", text, re.M)
    push = re.search(r"^  push:\n((?:    .*\n)*)", on.group(1) if on else "", re.M)
    if push and "branches: [main]" not in push.group(1):
        print(f.rsplit("/", 1)[-1])
PY
}
mkdir -p "$work/pp-wf"
printf 'on:\n  push:\n    paths: [x]\n' > "$work/pp-wf/bad.yml"
printf 'on:\n  push:\n    branches: [main]\n  workflow_dispatch:\n' > "$work/pp-wf/good.yml"
expect "push-probe: the trigger check finds a push trigger for every branch" bad.yml "$(push_on_other_branches "$work/pp-wf")"
expect "push-probe: no workflow of the repository runs on a push to another branch than main" "" "$(push_on_other_branches .github/workflows)"
pp_git_config=("$GIT_CONFIG_COUNT" "$GIT_CONFIG_KEY_0" "$GIT_CONFIG_VALUE_0")
unset GIT_CONFIG_COUNT GIT_CONFIG_KEY_0 GIT_CONFIG_VALUE_0
pp_root="$PWD"
pp_origin="$work/pp-origin.git"
pp_clone="$work/pp-clone"
git init -q --bare "$pp_origin"
git init -q -b main "$pp_clone"
git -C "$pp_clone" config user.email t@example.com
git -C "$pp_clone" config user.name t
mkdir -p "$pp_clone/.github/workflows"
printf 'on:\n  push:\n    branches: [main]\n' > "$pp_clone/.github/workflows/a.yml"
printf 'on:\n  workflow_dispatch:\n' > "$pp_clone/.github/workflows/b.yml"
printf 'x\n' > "$pp_clone/README.md"
git -C "$pp_clone" add -A
git -C "$pp_clone" commit -q -m base
git -C "$pp_clone" remote add origin "$pp_origin"
git -C "$pp_clone" push -q origin main
git -C "$pp_clone" fetch -q origin
git -C "$pp_clone" checkout -q -b claude/T-999-task
# The playbook row of ADR-0032 (PB-14, kind bo-qua-loai) in a table of its own, so the block does not depend on the
# real table; one test below reads the real table.
pp_table="$work/pp-playbook.md"
printf '%s\n' '<!-- playbook:start -->' '| Mã | Mẫu | Thấy ở | Loại | Việc phải làm | Nguồn |' '| --- | --- | --- | --- | --- | --- |' \
  '| PB-14 | `refusing to allow an? [A-Za-z ]+ to create or update workflow` | lệnh đẩy | `bo-qua-loai` | bỏ qua loại task | L-112 |' \
  '<!-- playbook:end -->' > "$pp_table"
pp_hook="$pp_origin/hooks/pre-receive"
pp_files="$work/pp-pushed.files"
pp_calls="$work/pp-hook.calls"
pp_hook_mode() { # record | nodelete | scope | app | other | network | flaky
  rm -f "$pp_files" "$pp_calls" "$pp_calls.count"
  local refuse='echo x >> "'"$pp_calls"'"'
  case "$1" in
    record) printf '#!/bin/sh\n%s\nwhile read old new ref; do [ "$new" = 0000000000000000000000000000000000000000 ] || git diff-tree --no-commit-id --name-only -r "$new" >> "%s"; done\nexit 0\n' "$refuse" "$pp_files" > "$pp_hook" ;;
    nodelete) printf '#!/bin/sh\n%s\nwhile read old new ref; do if [ "$new" = 0000000000000000000000000000000000000000 ]; then echo "deletion refused" >&2; exit 1; fi; done\nexit 0\n' "$refuse" > "$pp_hook" ;;
    scope) printf '#!/bin/sh\n%s\necho %s >&2\nexit 1\n' "$refuse" "'refusing to allow an OAuth App to create or update workflow \`.github/workflows/b.yml\` without \`workflow\` scope'" > "$pp_hook" ;;
    app) printf '#!/bin/sh\n%s\necho %s >&2\nexit 1\n' "$refuse" "'refusing to allow a GitHub App to create or update workflow \`.github/workflows/b.yml\` without \`workflows\` permission'" > "$pp_hook" ;;
    other) printf '#!/bin/sh\n%s\necho "protected branch hook declined" >&2\necho "line two" >&2\necho "line three" >&2\necho "line four" >&2\nexit 1\n' "$refuse" > "$pp_hook" ;;
    network) printf '#!/bin/sh\n%s\necho "fatal: The remote end hung up unexpectedly" >&2\nexit 1\n' "$refuse" > "$pp_hook" ;;
    flaky) printf '#!/bin/sh\n%s\nif [ -f "%s.count" ]; then exit 0; fi\ntouch "%s.count"\necho "fatal: The remote end hung up unexpectedly" >&2\nexit 1\n' \
      "$refuse" "$pp_calls" "$pp_calls" > "$pp_hook" ;;
  esac
  chmod +x "$pp_hook"
}
pp_run() { # <args>; runs push-probe.sh in the clone with the test playbook; output in $work/pp.out, prints the exit code
  local code=0
  (cd "$pp_clone/${PP_SUBDIR:-}" && PUSH_PROBE_PLAYBOOK="${PP_TABLE-$pp_table}" PUSH_PROBE_RETRY_SECONDS="${PP_RETRY:-0 0}" \
    "$pp_root/scripts/push-probe.sh" "$@") > "$work/pp.out" 2>&1 || code=$?
  echo "$code"
}
pp_probe_branches() { git -C "$pp_origin" for-each-ref --format='%(refname)' refs/heads/claude/ | grep -c 'probe-RUN-20261027-02' || true; }
pp_calls_count() { if [ -f "$pp_calls" ]; then wc -l < "$pp_calls" | tr -d ' '; else echo 0; fi; }
pp_untouched() { echo "$(git -C "$pp_clone" branch --show-current) $(git -C "$pp_clone" status --porcelain | wc -l | tr -d ' ') $(git -C "$pp_clone" worktree list | wc -l | tr -d ' ')"; }

pp_hook_mode record
expect "push-probe: a push that goes through exits 0" 0 "$(pp_run RUN-20261027-02)"
expect "push-probe: says QUA" 1 "$(grep -c '^QUA: phiên đẩy được thay đổi .github/workflows/' "$work/pp.out" || true)"
expect "push-probe: the probe commit changes only the workflow without a push trigger" ".github/workflows/b.yml" "$(cat "$pp_files")"
expect "push-probe: the probe branch is deleted again" 0 "$(pp_probe_branches)"
expect "push-probe: the checkout keeps its branch, files and worktrees" "claude/T-999-task 0 1" "$(pp_untouched)"
expect "push-probe: main on the remote does not change" "$(git -C "$pp_clone" rev-parse origin/main)" "$(git -C "$pp_origin" rev-parse main)"
pp_hook_mode nodelete
expect "push-probe: a refused delete still says the push went through" "0 1" \
  "$(pp_run RUN-20261027-02) $(grep -c '^CHÚ Ý: không xóa được nhánh claude/probe-RUN-20261027-02-' "$work/pp.out" || true)"
expect "push-probe: a probe branch left by a refused delete does not block the next probe" "0 2" \
  "$(pp_run RUN-20261027-02) $(pp_probe_branches)"
for ref in $(git -C "$pp_origin" for-each-ref --format='%(refname)' refs/heads/claude/); do git -C "$pp_origin" update-ref -d "$ref"; done
pp_hook_mode scope
expect "push-probe: a push refused for the workflow scope exits 3" 3 "$(pp_run RUN-20261027-02)"
expect "push-probe: says TỪ CHỐI with the playbook row and what the session does" "1 1" \
  "$(grep -c '^TỪ CHỐI: PB-14 bo-qua-loai' "$work/pp.out" || true) $(grep -c 'plan-check.py --touches .github/workflows/' "$work/pp.out" || true)"
expect "push-probe: quotes GitHub's line" 1 "$(grep -c 'without .workflow. scope' "$work/pp.out" || true)"
expect "push-probe: a refusal is not retried" 1 "$(pp_calls_count)"
expect "push-probe: a refused probe leaves no branch" 0 "$(pp_probe_branches)"
expect "push-probe: a refused probe leaves the checkout as it was" "claude/T-999-task 0 1" "$(pp_untouched)"
pp_hook_mode app
expect "push-probe: the GitHub App wording of the same refusal exits 3" 3 "$(pp_run RUN-20261027-02)"
pp_hook_mode scope
expect "push-probe: the real playbook (PB-14) classifies the refusal" 3 "$(PP_TABLE='' pp_run RUN-20261027-02)"
pp_hook_mode other
expect "push-probe: another refusal exits 4" 4 "$(pp_run RUN-20261027-02)"
expect "push-probe: another refusal is not called a missing workflow scope" "0 1" \
  "$(grep -c 'TỪ CHỐI' "$work/pp.out" || true) $(grep -c '^CHƯA RÕ: không đẩy được nhánh thử' "$work/pp.out" || true)"
expect "push-probe: an unclear refusal shows at most the last 3 git or remote lines" "3 1" \
  "$(grep -c '^remote:\|^ ! \|^error:\|^fatal:' "$work/pp.out" || true) $(grep -c 'remote: line four' "$work/pp.out" || true)"
pp_hook_mode network
expect "push-probe: a network error is retried, then unclear" "4 3" "$(pp_run RUN-20261027-02) $(pp_calls_count)"
pp_hook_mode flaky
expect "push-probe: a network error that clears goes through" "0 3" "$(pp_run RUN-20261027-02) $(pp_calls_count)"
pp_hook_mode record
expect "push-probe: a call from a subdirectory of the repository finds the workflows" "0 .github/workflows/b.yml" \
  "$(PP_SUBDIR=.github/workflows pp_run RUN-20261027-02) $(cat "$pp_files" 2> /dev/null)"
pp_hook_mode record
expect "push-probe: refuses an argument that is not a run id" 2 "$(pp_run RUN-abc)"
expect "push-probe: a bad argument pushes nothing" 0 "$(pp_calls_count)"
git -C "$pp_clone" checkout -q main
git -C "$pp_clone" rm -q -r .github
git -C "$pp_clone" commit -q -m "no workflows"
git -C "$pp_clone" push -q origin main
git -C "$pp_clone" fetch -q origin
git -C "$pp_clone" checkout -q claude/T-999-task
expect "push-probe: a repository without workflows has nothing to probe" 2 "$(pp_run RUN-20261027-02)"
export GIT_CONFIG_COUNT="${pp_git_config[0]}" GIT_CONFIG_KEY_0="${pp_git_config[1]}" GIT_CONFIG_VALUE_0="${pp_git_config[2]}"

# In-place sed without a backup suffix is GNU only: BSD sed on macOS stops with "invalid command code" (L-088).
# Scripts write `sed -i.bak ... && rm -f file.bak`, or use perl or python. The pattern is built from two
# parts so this file does not match itself.
gnu_sed_in_place() { # <dir>: prints the lines that edit in place with sed and no suffix
  grep -rnE "$(printf '%s' 'se')"'d( +-[A-Za-z]+)* +-[A-Za-z]*i([[:space:]]|['"'"'"]|$)' "$1" \
    --include='*.sh' --include='*.py' --include='*.bash' 2> /dev/null || true
}
mkdir -p "$work/sed-guard"
printf '#!/bin/bash\n%s -i "s/a/b/" f\nsed -i.bak "s/a/b/" f && rm -f f.bak\nsed -n 1p f\n' "se""d" > "$work/sed-guard/bad.sh"
expect "sed guard: finds in-place sed without a suffix in a sample" 1 "$(gnu_sed_in_place "$work/sed-guard" | grep -c .)"
expect "sed guard: no script edits in place with sed and no suffix" "" "$(gnu_sed_in_place scripts)"

# ads-plan.py writes the ads, Remote Config and events workbook of an app from its code (docs/ops/ads-plan.md).
# --dump needs no openpyxl, so these run everywhere; the workbook block runs only where openpyxl is installed.
ap="$work/ads-plan-repo"
mkdir -p "$ap/core/ads/src/main/kotlin/com/vglstudio/notes/core/ads" "$ap/docs" \
  "$ap/feature/notes/src/main/kotlin/n" "$ap/feature/notes/build/generated/src/main/kotlin/n" \
  "$ap/.claude/worktrees/w/feature/old/src/main/kotlin/o"
echo 'rootProject.name = "notes"' > "$ap/settings.gradle.kts"
cat > "$ap/core/ads/src/main/kotlin/com/vglstudio/notes/core/ads/AdPlacement.kt" <<'KT'
enum class AdFormat(val code: String) {
    INTERSTITIAL("interstitial"),
    REWARDED("rewarded"),
    NATIVE_SMALL("native_small"),
    NATIVE_IN_COLLECTION("native_in_collection"),
    APP_OPEN("app_open"),
}

private val SOUNDS = AdIntervalGroup.named("sounds", 30)

enum class AdPlacement(
    val key: String,
    val format: AdFormat,
) {
    /** Bottom bar of Home. */
    HOME_BOTTOM("home_bottom", AdFormat.NATIVE_SMALL),
    OPEN_APP("open_app", AdFormat.APP_OPEN, appWide = true),
    INTER_SOUNDS("inter_sounds", AdFormat.INTERSTITIAL, intervalGroup = SOUNDS),
    INTER_SAVE("inter_save", AdFormat.INTERSTITIAL, intervalGroup = AdIntervalGroup.named("save", 60)),
    RW_EXPORT("rw_export", AdFormat.REWARDED, noAdPolicy = RewardedNoAdPolicy.AllowAfter(3)),
    HOME_GRID("home_grid", AdFormat.NATIVE_IN_COLLECTION, nativeEvery = 6, firstOpenOnly = true),
    GALLERY_BANNER("gallery_banner", AdFormat.NATIVE_SMALL),
    ;
}
KT
cat > "$ap/docs/remote-config-keys.md" <<'MD'
| Khóa | Kiểu | Mặc định | Module | Mô tả |
| --- | --- | --- | --- | --- |
| `show_open_ads` | boolean | `false` | `:core:ads` | Shows the ad when the app opens. |
| `is_show_home_bottom` | boolean | `false` | `:core:ads` | Shows ads. Derived key of placement home_bottom (native_small). |
| `native_every_home_grid` | long | `6` | `:core:ads` | Items between ads. Derived key of placement home_grid (native_in_collection). |
| `is_log_af_native_ad_displayed` | boolean | `true` | `:core:analytics` | Sends af_native_ad_displayed. |
| `notes_max_count` | long | `50` | `:feature:notes` | Most notes kept. |
MD
echo 'const val EVENT_SAVED = "note_saved"' > "$ap/feature/notes/src/main/kotlin/n/NotesViewModel.kt"
echo 'const val EVENT_GEN = "generated_event"' > "$ap/feature/notes/build/generated/src/main/kotlin/n/Gen.kt"
echo 'const val EVENT_OLD = "worktree_event"' > "$ap/.claude/worktrees/w/feature/old/src/main/kotlin/o/Old.kt"
ads_plan() { ADS_PLAN_ROOT="$ap" python3 -I scripts/ads-plan.py "$@"; }
ap_get() { # <python expression on d (the dump) and P (placements by code)> [ads-plan.py options]: prints its value
  local expr="$1"
  shift
  ads_plan --dump "$@" | python3 -I -c '
import json, sys
d = json.load(sys.stdin)
P = {p["code"]: p for p in d["placements"]}
K = {k["key"]: (group, k) for group, keys in d["keys"].items() for k in keys}
print(eval(sys.argv[1]))' "$expr"
}
expect "ads-plan: the app name comes from settings.gradle.kts" notes "$(ap_get 'd["app"]')"
expect "ads-plan: placements of the code, in order, staging ones left out" \
  "home_bottom open_app inter_sounds inter_save rw_export home_grid" \
  "$(ap_get '" ".join(p["code"] for p in d["placements"])' --no-examples)"
expect "ads-plan: --with-staging lists the gallery_ placements" True "$(ap_get '"gallery_banner" in P' --with-staging)"
expect "ads-plan: AdPlacement.kt is found under a renamed package" native_small "$(ap_get 'P["home_bottom"]["format"]')"
expect "ads-plan: an interval group named by a constant" "sounds 30" \
  "$(ap_get 'P["inter_sounds"]["group"] + " " + str(P["inter_sounds"]["seconds"])')"
expect "ads-plan: an interval group written on the line" "save 60" \
  "$(ap_get 'P["inter_save"]["group"] + " " + str(P["inter_save"]["seconds"])')"
expect "ads-plan: the no-ad policy of a rewarded placement" allow_after_3 "$(ap_get 'P["rw_export"]["policy"]')"
expect "ads-plan: native every N of a list placement" 6 "$(ap_get 'P["home_grid"]["every"]')"
expect "ads-plan: an app-wide placement has no launch switch" "Có -" "$(ap_get 'P["open_app"]["wide"] + " " + P["open_app"]["launch"]')"
expect "ads-plan: a placement of the code is off at launch until the team says so" "Tắt Đã code" \
  "$(ap_get 'P["home_bottom"]["launch"] + " " + P["home_bottom"]["status"]')"
expect "ads-plan: the KDoc of a placement becomes its note" "Bottom bar of Home." "$(ap_get 'P["home_bottom"]["note"]')"
expect "ads-plan: a first-open-only placement says so in its note" firstOpenOnly "$(ap_get 'P["home_grid"]["note"]')"
expect "ads-plan: derived keys are not listed twice (they come from the placements sheet)" "False False" \
  "$(ap_get 'str("is_show_home_bottom" in K) + " " + str("native_every_home_grid" in K)')"
expect "ads-plan: a shared key goes in its group" "Mở app" "$(ap_get 'K["show_open_ads"][0]')"
expect "ads-plan: an AppsFlyer switch goes in its group" "Số liệu AppsFlyer" "$(ap_get 'K["is_log_af_native_ad_displayed"][0]')"
expect "ads-plan: a key of the app goes in the app group with its default" "Khóa của app 50" \
  "$(ap_get 'K["notes_max_count"][0] + " " + K["notes_max_count"][1]["default"]')"
expect "ads-plan: events of the main sources, not of build outputs or hidden folders" "note_saved :feature:notes" \
  "$(ap_get '" ".join(e["Tên sự kiện"] + " " + e["Module"] for e in d["events"])' --no-examples)"
expect "ads-plan: example rows by default, never twice the code of a real placement" "Ví dụ 1" \
  "$(ap_get 'P["rw_get_item"]["status"] + " " + str([p["code"] for p in d["placements"]].count("inter_sounds"))')"
mv "$ap/docs/remote-config-keys.md" "$ap/docs/keys.md.bak"
expect "ads-plan: a missing key table stops with the command that makes it" "1 1" \
  "$(ads_plan --dump > /dev/null 2> "$work/ap.err"; echo "$? $(grep -c 'remoteConfigKeys.update=true' "$work/ap.err")")"
mv "$ap/docs/keys.md.bak" "$ap/docs/remote-config-keys.md"
cp "$ap/core/ads/src/main/kotlin/com/vglstudio/notes/core/ads/AdPlacement.kt" "$ap/core/ads/src/main/kotlin/Extra.kt"
mv "$ap/core/ads/src/main/kotlin/Extra.kt" "$ap/core/ads/src/main/kotlin/AdPlacement.kt"
expect "ads-plan: two AdPlacement.kt stop the script" 1 "$(ads_plan --dump > /dev/null 2>&1; echo $?)"
rm "$ap/core/ads/src/main/kotlin/AdPlacement.kt"
# Every enum line of this repo's AdPlacement.kt is a placement the workbook lists: a declaration the script cannot
# read would be dropped without a word.
# An app that still holds an older core/ads (hold= in docs/template-sync.txt) has no key table yet, so it skips these.
if [[ ! -f docs/remote-config-keys.md ]]; then
  echo "skip - ads-plan: repo chưa có docs/remote-config-keys.md (app còn giữ core/ads cũ)"
else
  ap_real_file="$(find core/ads/src/main -name AdPlacement.kt)"
  ap_real_lines="$(awk '/^enum class AdPlacement/ {in_enum = 1} in_enum && /^    [A-Z_]+\("/ {n++} in_enum && /^    ;/ {exit} END {print n}' "$ap_real_file")"
  expect "ads-plan: every placement of this repo is read" "$ap_real_lines" \
    "$(python3 -I scripts/ads-plan.py --dump --with-staging --no-examples | python3 -I -c 'import json,sys; print(len(json.load(sys.stdin)["placements"]))')"
  expect "ads-plan: this repo lists its app-wide placements" True \
    "$(python3 -I scripts/ads-plan.py --dump | python3 -I -c 'import json,sys; c={p["code"] for p in json.load(sys.stdin)["placements"]}; print({"open_app","open_app_inter","resume","resume_inter","native_combo"} <= c)')"
  # The format sheet has one row per AdFormat code, read with the regex of read_placements (plan T-062).
  expect "ads-plan: the format table lists every AdFormat code of :core:ads" True \
    "$(python3 -I - "$ap_real_file" << 'PY'
import re, runpy, sys
plan = runpy.run_path("scripts/ads-plan.py")
text = open(sys.argv[1]).read().split("enum class AdPlacement")[0]
codes = {code for _, code in re.findall(r'^\s*([A-Z_]+)\("([a-z_]+)"\),', text, re.M)}
print(bool(codes) and codes == {row[0] for row in plan["FORMATS"]})
PY
)"
  expect "ads-plan: banner_mrec is a placement format with metric code BANNER_MREC" "True BANNER_MREC" \
    "$(python3 -I - << 'PY'
import runpy
plan = runpy.run_path("scripts/ads-plan.py")
rows = {row[0]: row for row in plan["FORMATS"]}
print("banner_mrec" in plan["PLACEMENT_FORMATS"], rows["banner_mrec"][6] if "banner_mrec" in rows else None)
PY
)"
  # The reload keys of KB-34 (plan T-087), read from the key table of this repo.
  expect "ads-plan: reload keys go in their groups" "Thanh đáy Thanh đáy Banner" \
    "$(python3 -I scripts/ads-plan.py --dump | python3 -I -c '
import json, sys
K = {k["key"]: group for group, keys in json.load(sys.stdin)["keys"].items() for k in keys}
print(*(K.get(n) for n in ("time_reload_native_small", "time_reload_collap_ad", "time_reload_banner_collap")))')"
  # The reload keys of the fixed banner and the MREC (plan T-088), read from the key table of this repo.
  expect "ads-plan: banner reload keys go in the Banner group" "Banner Banner" \
    "$(python3 -I scripts/ads-plan.py --dump | python3 -I -c '
import json, sys
K = {k["key"]: group for group, keys in json.load(sys.stdin)["keys"].items() for k in keys}
print(*(K.get(n) for n in ("time_reload_banner", "time_reload_banner_mrec")))')"
  # Control: the collapsible banner row already says reload by hand, so the check reads the refresh column.
  expect "ads-plan: banner and MREC formats say reload by hand" "True True True True True" \
    "$(python3 -I - << 'PY'
import runpy
plan = runpy.run_path("scripts/ads-plan.py")
rows = {row[0]: row for row in plan["FORMATS"]}
banner, mrec = rows["banner"][5], rows["banner_mrec"][5]
print("Tự làm mới theo đơn vị" not in banner, "time_reload_banner" in banner.replace("time_reload_banner_", ""),
      "Tự làm mới theo đơn vị" not in mrec, "time_reload_banner_mrec" in mrec,
      "Nạp lại tay" in rows["banner_collapsible"][5])
PY
)"
  # Control: time_reload_bottom_ad is still listed, so the list is read.
  expect "ads-plan: time_reload_collap_ad is not an unused AdsKit key" "False True" \
    "$(python3 -I - << 'PY'
import runpy
plan = runpy.run_path("scripts/ads-plan.py")
names = {key for key, _ in plan["UNUSED_ADSKIT_KEYS"]}
print("time_reload_collap_ad" in names, "time_reload_bottom_ad" in names)
PY
)"
  expect "ads-plan: the constants sheet has no reload interval" "False False True" \
    "$(python3 -I - << 'PY'
import runpy
plan = runpy.run_path("scripts/ads-plan.py")
names = [name for name, _ in plan["CONSTANTS"]]
print(any(n.startswith("Làm mới native") for n in names), any(n.startswith("Nạp lại banner thu gọn") for n in names),
      ("Mức sàn của nhịp làm mới", "30 giây") in plan["CONSTANTS"])
PY
)"
fi
if python3 -I -c 'import openpyxl' 2> /dev/null; then
  ads_plan --out "$work/ap-1.xlsx" > /dev/null
  expect "ads-plan: the workbook has its sheets in order" "Hướng dẫn,Vị trí QC,Remote Config,Sự kiện,Định dạng,Hằng số,Danh mục" \
    "$(python3 -I -c 'import openpyxl,sys; print(",".join(openpyxl.load_workbook(sys.argv[1]).sheetnames))' "$work/ap-1.xlsx")"
  # The team fills the workbook: a screen and a launch value of a coded placement, two rows of a planned placement
  # (same code twice), a launch value of a key, a planned key and a planned event.
  python3 -I -c '
import openpyxl, sys
wb = openpyxl.load_workbook(sys.argv[1])
ws = wb["Vị trí QC"]
rows = {ws.cell(r, 2).value: r for r in range(5, ws.max_row + 1) if ws.cell(r, 2).value}
ws.cell(rows["home_bottom"], 5).value = "Home"
ws.cell(rows["home_bottom"], 11).value = "Bật"
free = [r for r in range(5, ws.max_row + 1) if not ws.cell(r, 2).value][:2]
for r in free:
    ws.cell(r, 2).value, ws.cell(r, 3).value, ws.cell(r, 18).value = "inter_new", "interstitial", "Đề xuất"
rc = wb["Remote Config"]
for r in range(5, rc.max_row + 1):
    if rc.cell(r, 2).value == "show_open_ads":
        rc.cell(r, 5).value = "true"
app_rows = [r for r in range(5, rc.max_row + 1) if rc.cell(r, 1).value == "Khóa của app" and not rc.cell(r, 2).value]
rc.cell(app_rows[0], 2).value, rc.cell(app_rows[0], 3).value = "notes_sort_order", "string"
ev = wb["Sự kiện"]
free = [r for r in range(5, ev.max_row + 1) if ev.cell(r, 2).value == "Sự kiện của app" and not ev.cell(r, 3).value]
ev.cell(free[0], 3).value, ev.cell(free[0], 4).value = "note_shared", "Chia sẻ xong"
wb.save(sys.argv[1])' "$work/ap-1.xlsx"
  keep=(--keep "$work/ap-1.xlsx")
  expect "ads-plan: --keep keeps what the team filled for a coded placement" "Home Bật Đã code" \
    "$(ap_get 'P["home_bottom"]["screen"] + " " + P["home_bottom"]["launch"] + " " + P["home_bottom"]["status"]' "${keep[@]}")"
  expect "ads-plan: --keep keeps a planned placement, twice when the team wrote it twice" 2 \
    "$(ap_get '[p["code"] for p in d["placements"]].count("inter_new")' "${keep[@]}")"
  expect "ads-plan: --keep drops the example rows" False "$(ap_get '"rw_get_item" in P' "${keep[@]}")"
  expect "ads-plan: --keep keeps the launch value of a key" true "$(ap_get 'K["show_open_ads"][1]["launch"]' "${keep[@]}")"
  expect "ads-plan: --keep keeps a planned key of the app" string "$(ap_get 'K["notes_sort_order"][1]["type"]' "${keep[@]}")"
  expect "ads-plan: --keep keeps a planned event, an event of the code stays coded" "Đề xuất Đã code" \
    "$(ap_get '" ".join({e["Tên sự kiện"]: e["Trạng thái"] for e in d["events"]}[n] for n in ("note_shared", "note_saved"))' "${keep[@]}")"
  ads_plan "${keep[@]}" --out "$work/ap-2.xlsx" > /dev/null
  expect "ads-plan: a workbook written with --keep opens again" True \
    "$(python3 -I -c 'import openpyxl,sys; print("Vị trí QC" in openpyxl.load_workbook(sys.argv[1]).sheetnames)' "$work/ap-2.xlsx")"
else
  echo "skip - ads-plan: workbook tests need openpyxl (python3 -m pip install --user openpyxl)"
fi

# An app's own script tests (scripts/tests/app.sh in the app; the template has none) run in this same shell, so they
# use expect, $work and the fakes above, and count in the same total (docs/ops/to-app.md, section 6).
if [[ -f scripts/tests/app.sh ]]; then
  # shellcheck source=/dev/null
  . scripts/tests/app.sh
fi

if [[ "$failures" -gt 0 ]]; then
  echo "$failures script test(s) failed"
  exit 1
fi
echo "All script tests passed"
