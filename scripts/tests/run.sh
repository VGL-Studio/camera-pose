#!/usr/bin/env bash
# Tests for the shell scripts of the quality gate. Needs bash, python3 and jq.
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
# and the job keeps its name and is never skipped, so merge-if-green.sh still requires it green (L-040).
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
  pr_steps "$1" | grep -F "if: needs.changes.outputs.code != 'true'" | grep -cF 'scripts/tests/run.sh 2>&1 | tee build.log' || true
}
pr_unconditional() { # <workflow> <uses>; "yes" when the step with that action has no if:, "missing" with no such step
  local step
  step="$(pr_steps "$1" | grep -F "uses: $2" || true)"
  if [[ -z "$step" ]]; then echo missing; elif grep -q 'if:' <<<"$step"; then echo no; else echo yes; fi
}
pr_gradle_gated() { # <workflow>; "yes" when every step using Gradle or the Android SDK runs only for code
  local bad tab=$'\t'
  bad="$(pr_steps "$1" | grep -E 'setup-gradle|gradlew|scripts/check\.sh|ANDROID_HOME|android-emulator-runner' \
    | grep -vcE "if: [^$tab]*needs\.changes\.outputs\.code == 'true'" || true)"
  [[ "$bad" == 0 ]] && echo yes || echo no
}
pr_docs_step_tools() { # <workflow>; the docs-only step checks its tools and has its own timeout
  local step
  step="$(pr_steps "$1" | grep -F "if: needs.changes.outputs.code != 'true'" | grep -F 'scripts/tests/run.sh' || true)"
  local t n=0
  for t in 'command -v' python3 jq java 'timeout-minutes: 10'; do
    grep -qF -- "$t" <<<"$step" && n=$((n + 1))
  done
  echo "$n"
}
pr_job_if() { # <workflow>; counts job-level if: lines of build-and-test
  awk '/^  build-and-test:$/ { j = 1; next } j && /^    steps:$/ { j = 0 } j && /^    if:/ { n++ } END { print n + 0 }' "$1"
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
perl -0pe "s/- if: needs.changes.outputs.code == 'true'\n        uses: gradle\/actions\/setup-gradle/- uses: gradle\/actions\/setup-gradle/" \
  "$pr_wf" > "$work/prwf/gradle.yml"
expect "pr-check: the Gradle check fails on a copy whose setup-gradle runs for every PR" no "$(pr_gradle_gated "$work/prwf/gradle.yml")"
expect "pr-check: the docs-only step checks python3, jq and java first and has its own timeout" 5 "$(pr_docs_step_tools "$pr_wf")"
expect "pr-check: build-and-test has no job-level if" 0 "$(pr_job_if "$pr_wf")"
awk '/^  build-and-test:$/ { j = 1 } { print } j && /^    needs: changes$/ { print "    if: needs.changes.outputs.code == '\''true'\''"; j = 0 }' \
  "$pr_wf" > "$work/prwf/job-if.yml"
expect "pr-check: the job-level if check fails on a copy that skips build-and-test" 1 "$(pr_job_if "$work/prwf/job-if.yml")"
expect "pr-check: the failure comment of build-and-test also runs for docs-only PRs" yes "$(pr_failure_comment "$pr_wf")"
expect "pr-check: every check merge-if-green.sh requires or may skip is a job of pr-check.yml" "names=3 missing= none" \
  "$(pr_missing_jobs "$pr_wf")"
sed 's/^  build-and-test:$/  build:/' "$pr_wf" > "$work/prwf/renamed.yml"
expect "pr-check: the job name check names a renamed job" "names=3 missing= build-and-test" \
  "$(pr_missing_jobs "$work/prwf/renamed.yml")"

# merge-if-green.sh must reject any argument other than exactly "<PR> [--dry-run]" before doing anything.
for args in "12 --dryrun" "12 -n" "12 --dry_run" "12 --dry-run extra" "abc" ""; do
  # shellcheck disable=SC2086
  if PATH="$work/bin:$PATH" scripts/merge-if-green.sh $args > /dev/null 2>&1; then
    expect "merge-if-green rejects '$args'" rejected accepted
  else
    expect "merge-if-green rejects '$args'" rejected rejected
  fi
done

# merge-if-green.sh refuses other PRs while an auto-run session is running, but lets that session's PRs through.
merge_during_run() {
  local head_ref="$1" run_status="$2" labels="${3:-[]}"
  cat > "$work/merge.json" << JSON
{"routes": {
  "pulls/12": {"state": "open", "draft": false, "base": {"ref": "main"}, "head": {"sha": "abc1234", "ref": "$head_ref"}, "title": "t", "labels": $labels},
  "issues?state=open&labels=main-broken&per_page=10": [],
  "branches?per_page=100&page=1": [{"name": "main"}, {"name": "claude/RUN-20261015-01-report"}, {"name": "claude/RUN-20261016-01-report"}],
  "contents/docs/runs/RUN-20261015-01.md?ref=claude/RUN-20261015-01-report": "---\nid: RUN-20261015-01\nstatus: finished\n---\n",
  "contents/docs/runs/RUN-20261016-01.md?ref=claude/RUN-20261016-01-report": "---\nid: RUN-20261016-01\nstatus: $run_status   # draft | approved | running | finished\n---\n",
  "compare/main...abc1234": {"behind_by": 2}
}}
JSON
  local code=0
  PATH="$work/bin:$PATH" FIXTURE="$work/merge.json" scripts/merge-if-green.sh 12 --dry-run > /dev/null 2>&1 || code=$?
  echo "$code"
}
# Exit 3 ("branch is behind main") means the run check let the PR through; 1 means it was refused.
expect "merge-if-green refuses a person's PR while a run is running" 1 "$(merge_during_run claude/template-x running)"
expect "merge-if-green lets the session's task PR through" 3 "$(merge_during_run claude/T-001-loading running)"
expect "merge-if-green lets the session's report PR through" 3 "$(merge_during_run claude/RUN-20261016-01-report running)"
expect "merge-if-green ignores finished runs" 3 "$(merge_during_run claude/template-x finished)"
expect "merge-if-green refuses a PR labelled needs-human" 1 \
  "$(merge_during_run claude/fix-T-002-x finished '[{"name": "needs-human"}]')"
expect "merge-if-green accepts other labels" 3 "$(merge_during_run claude/template-x finished '[{"name": "docs"}]')"

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
  rm -f "$work/mg.json.writes" "$work/mg.json.seen"
  local code=0
  PATH="$work/bin:$PATH" FIXTURE="$work/mg.json" MERGE_POLL_SECONDS=0 MERGE_UPDATE_TIMEOUT_SECONDS=5 \
    MERGE_NOW_EPOCH="${NOW_EPOCH:-1793000000}" scripts/merge-if-green.sh "$@" > "$work/mg.out" 2>&1 || code=$?
  echo "$code"
}
green_runs='{"check_runs": [
  {"name": "changes", "status": "completed", "conclusion": "success", "started_at": "2026-10-18T02:01:00Z", "check_suite": {"id": 5}},
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
# A docs-only PR has this shape: changes green, build-and-test runs scripts/tests/run.sh, ui-test skipped. A red or
# skipped build-and-test is refused; the same fixture with build-and-test green is ready (L-040).
red_docs_runs="${green_runs/\"build-and-test\", \"status\": \"completed\", \"conclusion\": \"success\"/\"build-and-test\", \"status\": \"completed\", \"conclusion\": \"failure\"}"
skipped_docs_runs="${green_runs/\"build-and-test\", \"status\": \"completed\", \"conclusion\": \"success\"/\"build-and-test\", \"status\": \"completed\", \"conclusion\": \"skipped\"}"
merge_routes claude/template-x '{"behind_by": 0}' "$green_runs"
expect "merge-if-green: a docs-only PR whose build-and-test passed is ready" 0 "$(merge_run 12 --dry-run)"
merge_routes claude/template-x '{"behind_by": 0}' "$red_docs_runs"
expect "merge-if-green: a docs-only PR whose build-and-test failed is refused" "1 1" \
  "$(merge_run 12 --dry-run) $(grep -c 'có kiểm tra không qua: build-and-test\.' "$work/mg.out" || true)"
merge_routes claude/template-x '{"behind_by": 0}' "$skipped_docs_runs"
expect "merge-if-green: a skipped build-and-test is refused" "1 1" \
  "$(merge_run 12 --dry-run) $(grep -c "kiểm tra 'build-and-test' kết thúc với kết quả 'skipped'" "$work/mg.out" || true)"
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

# merge-if-green.sh notices a PR that is already on main, and a merge that GitHub answered with an error although the
# squash commit reached main (L-080). Every earlier check still comes first (L-058).
# The fixture is built from MIG_* variables; MIG_MAIN is the answer of "commits?sha=main", a JSON value or a
# {"__sequence__": [...]} (one answer per read).
mig_title_line='t (#12)'
mig_on_main='[{"sha": "c49a682c49a682", "commit": {"message": "t (#12)\n\nbody", "tree": {"sha": "tree1"}}}]'
mig_other_tree='[{"sha": "d111111d111111", "commit": {"message": "t (#12)", "tree": {"sha": "tree2"}}}]'
mig_other_title='[{"sha": "e222222e222222", "commit": {"message": "other (#99)", "tree": {"sha": "tree1"}}}]'
mig_run() { # <extra arg or empty>; reads MIG_*; prints the exit code, output in $work/mig.out
  local checks='{"check_runs": [
    {"name": "changes", "status": "completed", "conclusion": "'"${MIG_CHANGES:-success}"'", "started_at": "2026-10-23T08:00:00Z", "check_suite": {"id": 77}},
    {"name": "build-and-test", "status": "completed", "conclusion": "success", "started_at": "2026-10-23T08:00:10Z", "check_suite": {"id": 77}},
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
  PATH="$work/bin:$PATH" FIXTURE="$work/mig.json" MERGE_NOW_EPOCH=1792742400 MERGE_RECHECK_SECONDS=0 MERGE_POLL_SECONDS=0 MERGE_UPDATE_TIMEOUT_SECONDS=5 \
    scripts/merge-if-green.sh 12 ${1:+"$1"} > "$work/mig.out" 2>&1 || code=$?
  echo "$code"
}
mig_writes() { cat "$work/mig.json.writes" 2> /dev/null | grep -c -- "$1" || true; }
mig_reads() { jq -r '."commits?sha=main&per_page=30" // 0' "$work/mig.json.seen" 2>/dev/null || echo 0; }
mig_reset() { unset MIG_MAIN MIG_STATE MIG_DRAFT MIG_BASE MIG_LABELS MIG_ISSUES MIG_RUN MIG_BEHIND MIG_FAIL MIG_CHANGES MIG_DATE; }
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
  "$(MIG_MAIN='[]' MIG_CHANGES=failure MIG_FAIL='["PUT pulls/12/merge"]' mig_run)"
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
for lacking in '[]' "$mig_other_tree" "$mig_other_title"; do
  MIG_MAIN="$lacking"
  expect "merge-if-green refuses when the API fails and main lacks the commit" 1 "$(mig_run)"
  expect "merge-if-green says GitHub refused the merge" 1 "$(grep -c '^TỪ CHỐI: GitHub từ chối merge PR #12' "$work/mig.out" || true)"
  expect "merge-if-green does not ask for main-guard when the merge failed" 0 "$(mig_writes dispatches)"
done
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
FIXTURE="$work/mig.json" PATH="$work/bin:$PATH" gh api repos/o/r/git/commits/zzz > "$work/fake404.out" 2> "$work/fake404.err" || fake404=$?
expect "fake-gh answers an unknown path with 404" "1 1 0" \
  "${fake404:-0} $(grep -c 'HTTP 404' "$work/fake404.err" || true) $(grep -c Traceback "$work/fake404.err" || true)"

# failure-kind.sh (ADR-0025, point 1): "tests" only when every failed Gradle task is a test task with failing tests.
failure_kind() { printf '%b' "$1" > "$work/fk.log"; scripts/failure-kind.sh "$work/fk.log"; }
gradle_failure() { printf '* What went wrong:\\nExecution failed for task '"'"'%s'"'"'.\\n> %s\\n' "$1" "$2"; }
expect "failure-kind: only failing unit tests -> tests" tests \
  "$(failure_kind "$(gradle_failure :feature:sample:testDebugUnitTest 'There were failing tests. See the report at: x')")"
expect "failure-kind: failing staging unit tests and verifyRoborazzi -> tests" tests \
  "$(failure_kind "FAILURE: Build completed with 2 failures.\\n$(gradle_failure :app:testStagingDebugUnitTest 'There were failing tests.')$(gradle_failure :feature:sample:verifyRoborazziDebug 'There were failing tests.')")"
expect "failure-kind: compile error -> other" other \
  "$(failure_kind "$(gradle_failure :app:compileStagingDebugKotlin 'Compilation error. See log for more details')")"
expect "failure-kind: lint -> other" other \
  "$(failure_kind "$(gradle_failure :app:lintStagingDebug 'Lint found errors in the project; aborting build.')")"
expect "failure-kind: test and lint both failed -> other" other \
  "$(failure_kind "$(gradle_failure :feature:sample:testDebugUnitTest 'There were failing tests.')$(gradle_failure :app:lintStagingDebug 'Lint found errors.')")"
expect "failure-kind: a test task that failed for another reason -> other" other \
  "$(failure_kind "$(gradle_failure :feature:sample:testDebugUnitTest 'Could not start Gradle Test Executor 1.')")"
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
expect "playbook: the line carries the action and the source" "PB-02 cho: GitHub không cấp máy. Làm theo \`auto-run.md\` mục 4, dòng \"GitHub không chạy job\" (L-064)" \
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
merge_while_broken() { # <issues JSON> [IS_EXACT_REVERT] [run status]; prints the exit code
  cat > "$work/broken.json" << JSON
{"routes": {
  "pulls/12": {"state": "open", "draft": false, "base": {"ref": "main"}, "head": {"sha": "$(git_t rev-parse good)", "ref": "revert/${culprit::7}"}, "title": "Revert", "labels": []},
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
expect "merge-if-green lets the exact revert through while a run is running" 3 \
  "$(merge_while_broken "$one_issue" "$work/bin/revert-yes" running)"
expect "merge-if-green still refuses a revert branch that is not checked while a run is running" 1 \
  "$(merge_while_broken '[]' "$work/bin/revert-yes" running)"

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
  "$(wait_run "$(pr_routes "$red" '{"jobs": [{"name": "ui-test", "conclusion": "failure"}, {"name": "changes", "conclusion": "success"}]}')" --pr 7)"
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
line="$(wait_line "$(head_routes "$(head_at e35ae86)" "$green" "$green")" 10 --pr 7 --sha main)"
expect "wait-run: rejects a --sha that is not a commit" "1 1" \
  "$(echo "$(grep -c '^3 LỖI' <<< "$line") $(grep -c 'mã commit' <<< "$line" || true)")"
line="$(wait_line '{"routes": {}}' 10 --workflow staging.yml --since 2026-10-18T02:20:00Z --sha eba5f98)"
expect "wait-run: refuses --sha with --workflow" "1 1" \
  "$(echo "$(grep -c '^3 LỖI' <<< "$line") $(grep -c 'cùng --pr' <<< "$line" || true)")"

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

# resume-check.py finds a session that stopped in the middle (quota used up) so resume-run.yml can start it again.
resume_check() { # <run status> <minutes since the last commit> [extra routes JSON]; prints the run file or nothing
  local last
  last="$(python3 -c "import datetime as d; print((d.datetime(2026,10,18,3,0,tzinfo=d.timezone.utc)-d.timedelta(minutes=$2)).strftime('%Y-%m-%dT%H:%M:%SZ'))")"
  cat > "$work/resume.json" << JSON
{"routes": {
  "branches?per_page=100&page=1": [{"name": "main"}, {"name": "claude/RUN-20261018-01-report"}, {"name": "claude/T-007-x"}, {"name": "claude/T-099-other"}],
  "contents/docs/runs/RUN-20261018-01.md?ref=claude/RUN-20261018-01-report": "---\nid: RUN-20261018-01\nstatus: $1\nstarted_at: 2026-10-17 22:00 UTC\n---\n\n## 1. Danh sách chạy\n\n| 1 | T-007 | demo |\n",
  "commits/claude/RUN-20261018-01-report": {"commit": {"committer": {"date": "2026-10-17T22:05:00Z"}}},
  "commits/claude/T-007-x": {"commit": {"committer": {"date": "$last"}}},
  "commits/claude/T-099-other": {"commit": {"committer": {"date": "2026-10-18T02:59:00Z"}}},
  "issues?state=open&labels=main-broken&per_page=10": ${3:-[]},
  "issues?state=open&labels=env-broken&per_page=10": []
}}
JSON
  PATH="$work/bin:$PATH" FIXTURE="$work/resume.json" GITHUB_REPOSITORY=o/r RESUME_NOW=2026-10-18T03:00:00Z \
    python3 -I scripts/resume-check.py 2> /dev/null
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

# run-watchdog.py opens an issue when a running session has no commit for 60 minutes, so a person can
# continue a `claude --cloud` session that resume-run.yml does not continue (L-044, L-109).
wd_time() { python3 -c "import datetime as d; print((d.datetime(2026,10,27,12,0,tzinfo=d.timezone.utc)-d.timedelta(minutes=$1)).strftime('%Y-%m-%dT%H:%M:%SZ'))"; }
watchdog() { # <run status> <minutes since the last commit> [workflow runs: "min:branch:event:status:name,..." or -] [open issues JSON] [started_at] [errors JSON]
  local last in_progress="" queued="" code=0 spec min branch event status name entry errors="${6:-}"
  [ -n "$errors" ] || errors="{}"
  last="$(wd_time "$2")"
  if [ "${3:--}" != "-" ]; then
    IFS=',' read -r -a specs <<< "$3"
    for spec in "${specs[@]}"; do
      IFS=':' read -r min branch event status name <<< "$spec"
      entry="{\"name\": \"$name\", \"head_branch\": \"$branch\", \"event\": \"$event\", \"created_at\": \"$(wd_time "$min")\"}"
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
  "labels/session-stalled": {"name": "session-stalled"}
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

# DesignCompare: an identical image differs by 0%, a design without a screenshot is reported missing.
java_cmd="java"
[ -z "${JAVA_HOME:-}" ] || java_cmd="${JAVA_HOME}/bin/java"
mkdir -p "$work/design" "$work/shots"
cp feature/sample/src/test/screenshots/SampleScreen_ready_light.png "$work/design/Same.png"
cp feature/sample/src/test/screenshots/SampleScreen_ready_light.png "$work/shots/Same.png"
cp feature/sample/src/test/screenshots/SampleScreen_ready_dark.png "$work/design/Dark.png"
cp feature/sample/src/test/screenshots/SampleScreen_ready_light.png "$work/shots/Dark.png"
cp feature/sample/src/test/screenshots/SampleScreen_ready_light.png "$work/design/Missing.png"
"$java_cmd" -Djava.awt.headless=true scripts/design-compare/DesignCompare.java "$work/design" "$work/shots" "$work/review" > /dev/null
expect "design-compare: identical images differ by 0%" "| Same | 0.0% | \`Same_compare.png\` |" "$(grep '^| Same ' "$work/review/summary.md")"
expect "design-compare: light against dark differs" yes "$(grep -qE '^\| Dark \| ([1-9][0-9]?|100)\.[0-9]%' "$work/review/summary.md" && echo yes || echo no)"
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
  expect "new-app.sh regenerates the progress page without the template's lessons" 0 \
    "$(grep -c 'L-0[0-9][0-9]' "$new_app/docs/STATUS.md" || true)"
  expect "new-app.sh leaves pages that match the documents" 0 \
    "$(cd "$new_app" && python3 -I scripts/status.py --check > /dev/null && python3 -I scripts/foundation.py --check > /dev/null; echo $?)"
  expect "new-app.sh commits everything" "" "$(git -C "$new_app" status --porcelain)"
  expect "new-app.sh leaves the progress page out of git" "" "$(git -C "$new_app" ls-files docs/STATUS.md)"
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
    git -C "$tpl" commit --quiet -am "template v2"
    v2="$(git -C "$tpl" rev-parse HEAD)"
    echo "hold=core/ads $v1" >> "$app/docs/template-sync.txt"
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
    expect "template-sync.sh leaves the app clean" "" "$(git -C "$app" status --porcelain)"

    code=0; out="$("$tpl/scripts/template-sync.sh" --app "$app" --to "$v2" 2>&1)" || code=$?
    expect "template-sync.sh has nothing to do at the base" "0 1" "$code $(grep -c 'Không có gì mới' <<< "$out")"

    code=0; "$tpl/scripts/template-sync.sh" --app "$app" --take core/ads > /dev/null 2>&1 || code=$?
    expect "template-sync.sh --take brings a held path up to the base" "0 1 0" \
      "$code $(grep -cx '// template v2' "$app_ads") $(grep -c '^hold=' "$app/docs/template-sync.txt")"

    sed -i.bak '2s/.*/# app change/' "$app/scripts/wait-run.sh" && rm "$app/scripts/wait-run.sh.bak"
    git -C "$app" commit --quiet -am "app change"
    sed -i.bak '2s/.*/# template v3/' "$tpl/scripts/wait-run.sh" && rm "$tpl/scripts/wait-run.sh.bak"
    # new-app.sh put the app's name on the first line of README.md, so a template change there conflicts too.
    sed -i.bak '1s/.*/# TEMPLATE V3/' "$tpl/README.md" && rm "$tpl/README.md.bak"
    git -C "$tpl" commit --quiet -am "template v3"
    code=0; out="$("$tpl/scripts/template-sync.sh" --app "$app" --to HEAD 2>&1)" || code=$?
    expect "template-sync.sh stops on a conflict and sorts the files" "1 1 1" \
      "$code $(sed -n '/Phần của dự án mẫu/,/Phần app cũng sửa/p' <<< "$out" | grep -cx '   scripts/wait-run.sh') $(
        sed -n '/Phần app cũng sửa/,$p' <<< "$out" | grep -cx '   README.md')"

    code=0; out="$("$tpl/scripts/template-sync.sh" --app "$app" --to HEAD 2>&1)" || code=$?
    expect "template-sync.sh refuses an app with uncommitted changes" "1 1" "$code $(grep -c 'chưa commit' <<< "$out")"
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
expect "local-setup.sh --check lists every package" 3 "$(grep -c '^QUA: \(platform-tools\|platforms;\|build-tools;\)' "$ls_dir/out" || true)"

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
latest_run="$(ls docs/runs/RUN-*.md | sort | tail -n 1)"
python3 -I scripts/run-check.py "$latest_run" > "$work/run-check-latest.out" 2>&1 || true
expect "run-check: the latest run list has no new error from rule 11" "0 yes" \
  "$(grep -c 'LỖI.*kiểm tra thủ công\|LỖI.*quy tắc 11' "$work/run-check-latest.out" || true) $([[ -s "$work/run-check-latest.out" ]] && echo yes || echo no)"

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
# build-and-test, not only in status-page.yml after the merge (L-040).
mkdir -p "$work/status-real"
cp -R docs "$work/status-real/docs"
expect "status: the progress page builds from this repository's docs" 0 \
  "$(STATUS_ROOT="$work/status-real" python3 -I scripts/status.py > "$work/status-real.out" 2>&1; echo $?)"
printf -- '\n### L-040. Trùng số\n\n- **Trạng thái:** proposed\n' >> "$work/status-real/docs/lessons.md"
expect "status: this repository's docs with a lesson number used twice stop the page" "1 1" \
  "$(STATUS_ROOT="$work/status-real" python3 -I scripts/status.py > "$work/status-real.out" 2>&1; echo $?) $(grep -c 'số bài học dùng hai lần: L-040' "$work/status-real.out" || true)"


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
ds_script strs.txt 'tap str=sample_increment' 'tap str=sample_title'
expect "device-scenario: str= matches text and content-desc" "0 1 1" \
  "$(ds_run "grid.xml" "$ds_files/strs.txt") $(ds_count '^shell input tap 140 300$' "$ds_calls") $(ds_count '^shell input tap 325 445$' "$ds_calls")"
ds_script selected.txt 'expect text=Tap selected=true' 'expect text=Chế độ selected=true'
expect "device-scenario: selected=true matches selected or checked" "0 1" \
  "$(ds_run "grid.xml" "$ds_files/selected.txt") $(ds_count 'result=đạt')"
ds_script unselected.txt 'expect text=Tất cả selected=true'
expect "device-scenario: selected=true does not match an unselected chip" "0 1 1" \
  "$(ds_run "grid.xml" "$ds_files/unselected.txt") $(ds_count 'step=1 expect text=Tất cả selected=true FAIL') $(ds_count 'result=lỗi')"
ds_script clickable.txt 'tap text=Nhãn clickable=true'
expect "device-scenario: clickable=true skips plain nodes" "0 1" \
  "$(ds_run "grid.xml" "$ds_files/clickable.txt") $(ds_count '^shell input tap 250 35$' "$ds_calls")"
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
# The example of docs/ops/test-sessions.md, section 5, runs on the sample feature: it must keep passing check.
sed -n '/^ticket: T-NNN$/,/^shot landscape$/p' docs/ops/test-sessions.md | sed 's/^ticket: T-NNN$/ticket: T-001/' > "$ds_files/doc-example.txt"
expect "device-scenario: the example of test-sessions.md passes check" "0 11" \
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
sb_hook_mode() { # reject | network | none | flaky
  rm -f "$sb_calls" "$sb_calls.count"
  case "$1" in
    none) rm -f "$sb_hook" ;;
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
    HOME_GRID("home_grid", AdFormat.NATIVE_IN_COLLECTION, nativeEvery = 6),
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
