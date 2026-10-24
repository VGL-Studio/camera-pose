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
# merge-if-green.sh, wait-run.sh and env-canary.sh read owner/repo from the origin remote. A new app has no remote
# until someone pushes it, so give every git command of the tests a fake one (git reads GIT_CONFIG_* as -c options).
export GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=remote.origin.url GIT_CONFIG_VALUE_0=https://github.com/o/r.git
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
# with code (L-055, plan T-041). Same fixture as find-breaking-commit.sh; D=ddddd04 is the head of main.
main_guard_status() { # <fixture> [env...]: prints "<exit code> <output on one line>"
  rm -f "$work/mgs.json.calls"
  echo "$1" > "$work/mgs.json"
  shift
  local out code=0
  out="$(env PATH="$work/bin:$PATH" FIXTURE="$work/mgs.json" GITHUB_REPOSITORY=o/r "$@" \
    scripts/main-guard-status.sh ddddd04 2>&1)" || code=$?
  echo "$code $(tr '\n' ' ' <<<"$out")"
}
mgs="$(main_guard_status '{
  "ccccc03":{"parent":"","states":["success"]},
  "ddddd04":{"parent":"ccccc03","states":["success"]}}')"
expect "main-guard-status: green run on the head passes" 1 "$(grep -c '^0 XANH' <<<"$mgs")"
# RUN-20261023-03: c83777a (docs only, no run) on top of c49a682 (scripts/run-check.py, no run: GitHub sent no push).
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
  "ddddd04":{"parent":"","states":["pending"]}}')"
expect "main-guard-status: pending run asks to wait" 1 "$(grep -c '^2 CHỜ' <<<"$mgs")"
mgs="$(main_guard_status '{
  "ddddd04":{"parent":"","states":["failure"]}}')"
expect "main-guard-status: red run is refused" 1 "$(grep -c '^1 ĐỎ' <<<"$mgs")"
mgs="$(main_guard_status '{
  "bbbbbb2":{"parent":"","states":["success"]},
  "ccccc03":{"parent":"bbbbbb2","files":["docs/b.md"]},
  "ddddd04":{"parent":"ccccc03","files":["docs/a.md"]}}' MAX_DEPTH=2)"
expect "main-guard-status: stops after MAX_DEPTH" 3 "${mgs%% *}"
# start-run.yml checks main-guard after the run-file checks and before firing the routine; main-guard.yml can be
# run by hand for a commit that got no push event (L-054), keeping its push trigger as it was.
start_wf=.github/workflows/start-run.yml
expect "start-run: main-guard step runs after the run-file checks and before the routine" "yes yes" \
  "$(awk '/main-broken/ && !b { b = NR } /scripts\/main-guard-status.sh/ && !s { s = NR } /name: Kích hoạt routine/ { r = NR }
     END { print ((b && s > b && s < r) ? "yes" : "no") }' "$start_wf") $(grep -qE '^  checks: read$' "$start_wf" && echo yes || echo no)"
guard_wf=.github/workflows/main-guard.yml
expect "main-guard: has workflow_dispatch and keeps the push trigger with the same paths-ignore" "1 1 1 1" \
  "$(grep -cE '^  workflow_dispatch:' "$guard_wf") $(grep -cE '^  push:$' "$guard_wf") $(grep -cE "^      - 'docs/\*\*'$" "$guard_wf") $(grep -cE "^      - '\*\*/\*\.md'$" "$guard_wf")"

# merge-if-green.sh must reject any argument other than exactly "<PR> [--dry-run]" before doing anything.
for args in "12 --dryrun" "12 -n" "12 --dry_run" "12 --dry-run extra" "abc" "" \
  "12 --update extra" "12 --updat" "12 --update --dry-run"; do
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
  "branches?per_page=100": [{"name": "main"}, {"name": "claude/RUN-20261015-01-report"}, {"name": "claude/RUN-20261016-01-report"}],
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
expect "merge-if-green without --update does not update the branch" 0 \
  "$(cat "$work/merge.json.writes" 2>/dev/null | grep -c update-branch || true)"

# merge-if-green.sh --update (plan T-029, L-017): a branch behind main is updated by GitHub, then exit 2 ("wait for CI").
# Every check that comes before still refuses first (L-044).
merge_update() { # <head ref> <run status> <main-broken issues JSON> [fail_writes JSON]; prints the exit code
  rm -f "$work/update.json.writes"
  cat > "$work/update.json" << JSON
{"fail_writes": ${4:-[]},
 "routes": {
  "pulls/12": {"state": "open", "draft": false, "base": {"ref": "main"}, "head": {"sha": "abc1234", "ref": "$1"}, "title": "t", "labels": []},
  "issues?state=open&labels=main-broken&per_page=10": $3,
  "branches?per_page=100": [{"name": "main"}, {"name": "claude/RUN-20261016-01-report"}],
  "contents/docs/runs/RUN-20261016-01.md?ref=claude/RUN-20261016-01-report": "---\nid: RUN-20261016-01\nstatus: $2\n---\n",
  "compare/main...abc1234": {"behind_by": 2}
}}
JSON
  local code=0
  PATH="$work/bin:$PATH" FIXTURE="$work/update.json" scripts/merge-if-green.sh 12 --update > "$work/update.out" 2>&1 || code=$?
  echo "$code"
}
update_writes() { cat "$work/update.json.writes" 2>/dev/null | grep -c '^PUT pulls/12/update-branch$' || true; }
expect "merge-if-green --update updates a branch behind main" 2 "$(merge_update claude/template-x finished '[]')"
expect "merge-if-green --update asks GitHub to update the branch" 1 "$(update_writes)"
expect "merge-if-green --update says to wait for CI" 1 "$(grep -c '^CHƯA XONG: đã cập nhật nhánh' "$work/update.out")"
expect "merge-if-green --update refuses when GitHub cannot update" 1 \
  "$(merge_update claude/template-x finished '[]' '["PUT pulls/12/update-branch"]')"
expect "merge-if-green --update says GitHub could not update" 1 "$(grep -c 'không cập nhật được nhánh' "$work/update.out")"
expect "merge-if-green --update still refuses a person's PR while a run is running" 1 \
  "$(merge_update claude/template-x running '[]')"
expect "merge-if-green --update does not update a refused PR" 0 "$(update_writes)"
expect "merge-if-green --update still refuses while main is broken" 1 \
  "$(merge_update claude/template-x finished '[{"number": 7, "body": "Nhánh chính đỏ."}]')"
expect "merge-if-green --update does not update while main is broken" 0 "$(update_writes)"

# A check run still queued or in progress whose check suite completed with success counts as green (plan T-029, L-017).
# A commit pushed less than 3 minutes ago gets a warning, not a refusal (L-018).
merge_checks() { # <changes status> <suite answer> <commit date>; prints the exit code
  local changes_conclusion=null
  [[ "$1" == "completed" ]] && changes_conclusion='"success"'
  cat > "$work/checks.json" << JSON
{"routes": {
  "pulls/12": {"state": "open", "draft": false, "base": {"ref": "main"}, "head": {"sha": "abc1234", "ref": "claude/template-x"}, "title": "t", "labels": []},
  "issues?state=open&labels=main-broken&per_page=10": [],
  "branches?per_page=100": [{"name": "main"}],
  "compare/main...abc1234": {"behind_by": 0},
  "commits/abc1234/check-runs?per_page=100": {"check_runs": [
    {"name": "changes", "status": "$1", "conclusion": $changes_conclusion, "started_at": "2026-10-23T08:00:00Z", "check_suite": {"id": 77}},
    {"name": "build-and-test", "status": "completed", "conclusion": "success", "started_at": "2026-10-23T08:00:10Z", "check_suite": {"id": 77}},
    {"name": "ui-test", "status": "completed", "conclusion": "skipped", "started_at": "2026-10-23T08:00:10Z", "check_suite": {"id": 77}}]},
  "check-suites/77": $2,
  "commits/abc1234": {"sha": "abc1234", "commit": {"committer": {"date": "$3"}}}
}}
JSON
  local code=0
  PATH="$work/bin:$PATH" FIXTURE="$work/checks.json" MERGE_IF_GREEN_NOW=1792742400 \
    scripts/merge-if-green.sh 12 --dry-run > "$work/checks.out" 2>&1 || code=$?
  echo "$code"
}
# MERGE_IF_GREEN_NOW=1792742400 is 2026-10-23T08:00:00Z.
suite_ok='{"id": 77, "status": "completed", "conclusion": "success"}'
expect "merge-if-green treats a stuck check as green when its suite succeeded" 0 \
  "$(merge_checks in_progress "$suite_ok" 2026-10-23T07:50:00Z)"
expect "merge-if-green says it trusted the suite" 1 "$(grep -c "^CHÚ Ý: kiểm tra 'changes'" "$work/checks.out")"
expect "merge-if-green waits for a check whose suite is still running" 2 \
  "$(merge_checks queued '{"id": 77, "status": "in_progress", "conclusion": null}' 2026-10-23T07:50:00Z)"
expect "merge-if-green does not trust a failed suite" 2 \
  "$(merge_checks in_progress '{"id": 77, "status": "completed", "conclusion": "failure"}' 2026-10-23T07:50:00Z)"
expect "merge-if-green warns about a commit pushed a minute ago" 0 "$(merge_checks completed "$suite_ok" 2026-10-23T07:59:00Z)"
expect "merge-if-green names the fresh commit" 1 "$(grep -c "^CHÚ Ý: commit cuối abc1234" "$work/checks.out")"
expect "merge-if-green does not warn about an older commit" 0 "$(merge_checks completed "$suite_ok" 2026-10-23T07:50:00Z)"
expect "merge-if-green has no warning for an older commit" 0 "$(grep -c "^CHÚ Ý" "$work/checks.out" || true)"

# failure-kind.sh (ADR-1002, point 1): "tests" only when every failed Gradle task is a test task with failing tests.
failure_kind() { printf '%b' "$1" > "$work/fk.log"; scripts/failure-kind.sh "$work/fk.log"; }
gradle_failure() { printf '* What went wrong:\\nExecution failed for task '"'"'%s'"'"'.\\n> %s\\n' "$1" "$2"; }
expect "failure-kind: only failing unit tests -> tests" tests \
  "$(failure_kind "$(gradle_failure :feature:camera:testDebugUnitTest 'There were failing tests. See the report at: x')")"
expect "failure-kind: failing staging unit tests and verifyRoborazzi -> tests" tests \
  "$(failure_kind "FAILURE: Build completed with 2 failures.\\n$(gradle_failure :app:testStagingDebugUnitTest 'There were failing tests.')$(gradle_failure :feature:camera:verifyRoborazziDebug 'There were failing tests.')")"
expect "failure-kind: compile error -> other" other \
  "$(failure_kind "$(gradle_failure :app:compileStagingDebugKotlin 'Compilation error. See log for more details')")"
expect "failure-kind: lint -> other" other \
  "$(failure_kind "$(gradle_failure :app:lintStagingDebug 'Lint found errors in the project; aborting build.')")"
expect "failure-kind: test and lint both failed -> other" other \
  "$(failure_kind "$(gradle_failure :feature:camera:testDebugUnitTest 'There were failing tests.')$(gradle_failure :app:lintStagingDebug 'Lint found errors.')")"
expect "failure-kind: a test task that failed for another reason -> other" other \
  "$(failure_kind "$(gradle_failure :feature:camera:testDebugUnitTest 'Could not start Gradle Test Executor 1.')")"
expect "failure-kind: no Gradle failure line -> other" other \
  "$(failure_kind "FAIL - merge-if-green rejects '12 -n'\\n1 script test(s) failed\\n")"
expect "failure-kind: missing log -> other" other "$(scripts/failure-kind.sh "$work/no-such.log")"

# is-exact-revert.sh (ADR-1002, point 3), on a throwaway repo: A, B (the culprit), C (main).
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

# merge-if-green.sh lets an exact revert through while main is broken (ADR-1002, point 3), and nothing else.
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
  "branches?per_page=100": [{"name": "main"}, {"name": "claude/RUN-20261016-01-report"}],
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
# A run GitHub never started (no runner: spending limit, billing) is not a red check: exit 4 (plan T-033, L-046).
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

# GitHub runs no pull_request workflow for a PR in conflict with main: say so at once, exit 5 (plan T-033, L-048).
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
  "branches?per_page=100": [{"name": "main"}, {"name": "claude/RUN-20261018-01-report"}, {"name": "claude/T-007-x"}, {"name": "claude/T-099-other"}],
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
  expect "new-app.sh regenerates the progress page without the template's lessons" 0 \
    "$(grep -c 'L-0[0-9][0-9]' "$new_app/docs/STATUS.md" || true)"
  expect "new-app.sh leaves pages that match the documents" 0 \
    "$(cd "$new_app" && python3 -I scripts/status.py --check > /dev/null && python3 -I scripts/foundation.py --check > /dev/null; echo $?)"
  expect "new-app.sh commits everything" "" "$(git -C "$new_app" status --porcelain)"
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

# cloud-env-check.sh passes only when the environment was built from the current cloud-setup.sh.
setup_version="$(sed -n 's/^SETUP_VERSION=\([0-9][0-9]*\)$/\1/p' scripts/cloud-setup.sh)"
mkdir -p "$work/gradle-home/init.d"
touch "$work/gradle-home/init.d/maven-central-mirror.gradle"
env_check() {
  echo "$1" > "$work/cloud-setup-version"
  if CLOUD_SETUP_VERSION_FILE="$work/cloud-setup-version" GRADLE_USER_HOME="$2" \
    scripts/cloud-env-check.sh > /dev/null 2>&1; then echo pass; else echo stop; fi
}
expect "cloud-setup.sh has a version" yes "$([[ -n "$setup_version" ]] && echo yes || echo no)"
expect "cloud-env-check.sh passes on the current version" pass "$(env_check "$setup_version" "$work/gradle-home")"
expect "cloud-env-check.sh stops on an older version" stop "$(env_check 1 "$work/gradle-home")"
expect "cloud-env-check.sh stops without the mirror init script" stop \
  "$(env_check "$setup_version" "$work/no-gradle-home")"

# plan-check.py finds, from the plan alone, what would otherwise stop a task during the night (L-020).
repo="$work/plan-repo"
mkdir -p "$repo/scripts" "$repo/docs/data" "$repo/docs/features/demo/plans" "$repo/docs/features/demo/tickets" \
  "$repo/docs/features/demo/design/screens" "$repo/feature/demo/src/test/screenshots" "$repo/core/x"
echo '{}' > "$repo/docs/data/values.json"
echo 'open("docs/data/values.json")' > "$repo/scripts/reader.py"
touch "$repo/feature/demo/A.kt" "$repo/core/x/B.kt" "$repo/docs/features/demo/design/screens/DemoScreen_main_light.png"
write_plan() { # <file> <risk> <status> <rows> [extra text]
  printf -- '---\nticket: %s\nstatus: %s\nrisk_level: %s\nreviewer: A\ncross_reviewer:\napproved_at: 2026-10-17\n---\n\n### 3.2. Danh sách file\n\n| Thao tác | Đường dẫn | Nội dung |\n| --- | --- | --- |\n%b\n\n### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện\n\n- **Quyền truy cập:** %s\n- **Thư viện và SDK:** Không thêm.\n\n%s\n\n## 4. Kiểm thử\n' \
    "$(basename "$1" | cut -d- -f1-2)" "$3" "$2" "$4" "${PERMISSIONS:-Không thêm.}" "${5:-}" > "$1"
}
plan_check() { if PLAN_CHECK_ROOT="$repo" python3 -I scripts/plan-check.py "$1" > "$work/plan-check.out" 2>&1; then echo pass; else echo stop; fi; }
# One row of the file list of a plan; the backticks are Markdown, not command substitution.
# shellcheck disable=SC2016
row() { printf '| %s | `%s` | x |' "$1" "$2"; }
plans="$repo/docs/features/demo/plans"
write_plan "$plans/T-901-a.md" medium draft "$(row Sửa feature/demo/A.kt)"
expect "plan-check: a correct plan passes" pass "$(plan_check "$plans/T-901-a.md")"
write_plan "$plans/T-902-a.md" medium draft "$(row Sửa feature/demo/Missing.kt)"
expect "plan-check: changing a missing file stops" stop "$(plan_check "$plans/T-902-a.md")"
write_plan "$plans/T-903-a.md" medium draft "$(row Sửa docs/data/values.json)"
expect "plan-check: a script reading a changed file stops" stop "$(plan_check "$plans/T-903-a.md")"
expect "plan-check: names the reader" 1 "$(grep -c 'scripts/reader.py nhắc tới docs/data/values.json' "$work/plan-check.out")"
write_plan "$plans/T-904-a.md" medium draft "$(row Sửa docs/data/values.json)" 'scripts/reader.py chỉ đọc khóa a, không đổi.'
expect "plan-check: a reader the plan explains passes" pass "$(plan_check "$plans/T-904-a.md")"
write_plan "$plans/T-905-a.md" medium draft "$(row Sửa core/x/B.kt)"
expect "plan-check: core module needs a high-risk plan" stop "$(plan_check "$plans/T-905-a.md")"
write_plan "$plans/T-906-a.md" high approved "$(row Sửa core/x/B.kt)"
expect "plan-check: approved high-risk plan needs a cross reviewer" stop "$(plan_check "$plans/T-906-a.md")"
write_plan "$plans/T-907-a.md" medium draft "$(row Sửa feature/demo/A.kt)" 'Chạy scripts/design-compare.sh demo.'
expect "plan-check: design image without screenshot stops" stop "$(plan_check "$plans/T-907-a.md")"
write_plan "$plans/T-908-a.md" medium draft \
  "$(row Tạo feature/demo/src/test/screenshots/DemoScreen_main_light.png)" 'Chạy scripts/design-compare.sh demo.'
expect "plan-check: design image with its screenshot passes" pass "$(plan_check "$plans/T-908-a.md")"
# A plan smaller than its feature declares the design images another ticket draws; they need no screenshot here.
# shellcheck disable=SC2016
other='Chạy scripts/design-compare.sh demo. Ảnh thiết kế của ticket khác (T-920): `DemoScreen_main_light`.'
write_plan "$plans/T-916-a.md" medium draft "$(row Sửa feature/demo/A.kt)" "$other"
expect "plan-check: a design image of another ticket passes" pass "$(plan_check "$plans/T-916-a.md")"
# shellcheck disable=SC2016
write_plan "$plans/T-917-a.md" medium draft "$(row Sửa feature/demo/A.kt)" \
  'Chạy scripts/design-compare.sh demo. Ảnh thiết kế của ticket khác (T-920): `DemoScreen_typo_light`.'
expect "plan-check: an unknown image of another ticket stops" stop "$(plan_check "$plans/T-917-a.md")"
expect "plan-check: names the unknown image" 1 "$(grep -c 'DemoScreen_typo_light' "$work/plan-check.out")"
# shellcheck disable=SC2016
write_plan "$plans/T-918-a.md" medium draft "$(row Sửa feature/demo/A.kt)" \
  'Chạy scripts/design-compare.sh demo. Ảnh thiết kế của ticket khác: `DemoScreen_main_light`.'
expect "plan-check: images of another ticket without the ticket id stop" stop "$(plan_check "$plans/T-918-a.md")"
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
# A new technique (criterion E from 2) needs a trial run with scripts/check.sh before review (L-001, L-010).
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
rm "$repo/feature/demo/AndroidManifest.xml" "$repo/app/src/main/AndroidManifest.xml" "$repo/gradle/libs.versions.toml"
# What scripts/check.sh refuses shows up in the plan (rule 9, L-013). The Kotlin quotes are plan text.
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
# The ticket's status follows the plan's (rule 10, L-019).
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
write_run_plan() { # <id> <file> <test row> [operation]
  printf -- '---\nticket: %s\nstatus: approved\nrisk_level: medium\nreviewer: A\napproved_at: 2026-10-02\n---\n\n### 3.2. Danh sách file\n\n| Thao tác | Đường dẫn | Nội dung |\n| --- | --- | --- |\n| %s | `%s` | x |\n\n## 4. Kiểm thử\n\n| Tiêu chí | Loại | Tên |\n| --- | --- | --- |\n%s\n\n## 9. Quyết định duyệt\n\n- **Quyết định:** Duyệt\n- **Người duyệt:** A\n' \
    "$1" "${4:-Sửa}" "$2" "$3" > "$rr/docs/features/demo/plans/$1-x.md"
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
expect "run-check: two independent plans changing one file warn" 1 "$(run_check > /dev/null; grep -c 'L-014' "$work/run-check.out")"
# Two plans that both create one file, even dependent ones: only one may create it, the other reuses it (L-033).
write_ticket T-002 'T-001'
write_run_plan T-001 feature/demo/New.kt '| KB-1 | Unit | `aTest` |' Tạo
write_run_plan T-002 feature/demo/New.kt '| KB-1 | Unit | `bTest` |' Tạo
write_run "$(run_row 1 T-001)$(run_row 2 T-002)"
expect "run-check: two plans creating one file warn" 1 "$(run_check > /dev/null; grep -c 'cùng ghi Tạo feature/demo/New.kt' "$work/run-check.out")"
write_run_plan T-002 feature/demo/New.kt '| KB-1 | Unit | `bTest` |' Sửa
expect "run-check: a later plan that changes the created file does not warn" 0 \
  "$(run_check > /dev/null; grep -c 'cùng ghi Tạo' "$work/run-check.out")"
write_ticket T-002 ''

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
expect "token-usage: no transcripts is not a failure" 0 \
  "$(TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/none" python3 -I scripts/token-usage.py report > /dev/null; echo $?)"
expect "token-usage: estimate falls back to the starting value" 1 \
  "$(token_usage estimate --csv docs/metrics/t.csv | grep -c '^| M | 1 | 2k | 15.0M (tạm')"
expect "token-usage: starting estimates are S 5M, M 15M, L 30M (L-021)" "5.0M 15.0M 30.0M" \
  "$(token_usage estimate --csv docs/metrics/none.csv | sed -n 's/^| [SML] | 0 | - | \([0-9.]*M\) (tạm.*/\1/p' | xargs)"
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
# The progress page is not in git on main: status-page.yml builds it after each push to main and pushes it to the
# status branch only, so PRs never conflict on it (plan T-034, L-047).
expect "status page: docs/STATUS.md is not tracked" "" "$(git ls-files docs/STATUS.md)"
expect "status page: git ignores docs/STATUS.md" 0 "$(git check-ignore -q docs/STATUS.md; echo $?)"
status_wf=.github/workflows/status-page.yml
expect "status-page.yml pushes only the status branch" "1 0" \
  "$(grep -c 'git push' "$status_wf" 2>/dev/null || echo 0) $(grep 'git push' "$status_wf" 2>/dev/null | grep -vc 'origin status$' || true)"
expect "status-page.yml builds the page with status.py before pushing" yes \
  "$(awk '/python3 scripts\/status.py/ { built = 1 } /git push/ { print (built ? "yes" : "no"); exit }' "$status_wf" 2>/dev/null)"
expect "status-page.yml runs on pushes to main that change docs" 2 \
  "$(grep -cE "^    branches: \[main\]$|^    paths: \['docs/\*\*'" "$status_wf" 2>/dev/null || true)"

# status.py --sync moves each ticket's status after its plan's (L-019).
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
# The lessons of an app may use "L-NNN:" and a status line with more fields; both forms are counted.
# A lesson number used twice (two branches adding lessons at once) stops the script (L-042).
cp "$st/lessons.md" "$work/lessons.bak"
printf -- '# SỔ\n\n### L-001. Một bài\n\n- **Trạng thái:** proposed\n\n### L-002: Bài của app\n\n- **Trạng thái:** accepted. **Loại:** quy trình.\n' > "$st/lessons.md"
status_page > /dev/null
expect "status: lessons written as L-NNN: are counted" 1 "$(grep -c 'lessons.md)): 2 bài; 1 .accepted., 1 .proposed.' "$page")"
printf -- '\n### L-002: Bài trùng số\n\n- **Trạng thái:** proposed\n' >> "$st/lessons.md"
expect "status: a lesson number used twice stops" 1 "$(status_page > "$work/status.out" 2>&1; echo $?)"
expect "status: names the duplicate lesson number" 1 "$(grep -c 'L-002' "$work/status.out")"
cp "$work/lessons.bak" "$st/lessons.md"
status_page > /dev/null

# foundation.py builds docs/foundation.md from the rule section of the ADRs.
fd="$work/foundation-repo/docs/adr"
mkdir -p "$fd"
printf -- '---\nid: ADR-0001\ntitle: Mẫu\nstatus: accepted\norigin: template\n---\n\n## Hệ quả\n\n- **Tích cực:** x\n- **Việc AI phải tuân thủ:** làm A; không làm `b; c`.\n' > "$fd/0001-mau.md"
printf -- '---\nid: ADR-0002\ntitle: Cũ\nstatus: superseded\norigin: template\n---\n\n- **Việc AI phải tuân thủ:** quy tắc cũ.\n' > "$fd/0002-cu.md"
printf -- '---\nid: ADR-1001\ntitle: Riêng\nstatus: accepted\norigin: app\n---\n\n- **Việc AI phải tuân thủ:** quy tắc app.\n' > "$fd/1001-rieng.md"
printf -- '---\nid: ADR-1002\ntitle: Đề xuất\nstatus: proposed\norigin: app\n---\n\n- **Phải chấp nhận:** y\n' > "$fd/1002-de-xuat.md"
foundation() { FOUNDATION_ROOT="$work/foundation-repo" python3 -I scripts/foundation.py "$@"; }
expect "foundation: check fails before the page exists" 1 "$(foundation --check > /dev/null; echo $?)"
foundation > /dev/null
fpage="$work/foundation-repo/docs/foundation.md"
expect "foundation: each clause is one rule, semicolons in code kept" 2 "$(grep -cE '^- (làm A|không làm .b; c.)$' "$fpage")"
expect "foundation: superseded ADRs are left out" 0 "$(grep -c 'quy tắc cũ' "$fpage")"
expect "foundation: app ADRs are listed apart" 1 "$(grep -c '^## Quyết định riêng của app' "$fpage")"
expect "foundation: an ADR without rules points to the full ADR" 1 "$(grep -c 'ADR chưa có mục' "$fpage")"
expect "foundation: check passes on a fresh page" 0 "$(foundation --check > /dev/null; echo $?)"
expect "foundation: docs/foundation.md of this repo matches its ADRs" 0 \
  "$(python3 -I scripts/foundation.py --check > /dev/null; echo $?)"

# secret-scan.py finds real-looking ad IDs and keys; Google test IDs and keys marked FAKE are allowed.
scan="$work/scan"
mkdir -p "$scan"
printf 'banner: ca-app-pub-3940256099942544/9214589741\nkey: AIzaSyFAKE_STAGING_KEY_NOT_REAL00000000\n' > "$scan/ok.md"
expect "secret-scan: Google test IDs and fake keys pass" 0 \
  "$(SECRET_SCAN_ROOT="$scan" python3 -I scripts/secret-scan.py > /dev/null; echo $?)"
# Built from pieces, so this file itself holds no real-looking ID for the repository scan below.
printf 'log: AdLoader init ca-app-pub-%s/%s ok\n' 5555555555555555 2222222222 > "$scan/report.md"
expect "secret-scan: a real-looking ad unit in a report fails" 1 \
  "$(SECRET_SCAN_ROOT="$scan" python3 -I scripts/secret-scan.py > /dev/null; echo $?)"
expect "secret-scan: the finding is masked" 0 \
  "$(SECRET_SCAN_ROOT="$scan" python3 -I scripts/secret-scan.py | grep -c '2222222222' || true)"
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

# preflight.sh checks the staging environment on GitHub before a session (plan T-030, L-002, L-011, L-012, L-027).
# dig, curl and firebase are replaced by fakes reading answers from $pf; secret values carry markers that must never
# appear in the output.
pf="$work/preflight"
mkdir -p "$pf/curl"
cat > "$work/bin/fake-dig" << 'SH'
#!/usr/bin/env bash
# Answers from $PF/dig-<server>; records its arguments.
echo "$*" >> "$PF/dig.args"
for a in "$@"; do case "$a" in @*) server="${a#@}" ;; esac; done
cat "$PF/dig-$server" 2>/dev/null || true
SH
cat > "$work/bin/fake-curl" << 'SH'
#!/usr/bin/env bash
# The status code for a URL is the first line of $PF/curl/<path with / as _>, consumed one per call; default 200.
url="${*: -1}"; key="$(sed -E 's#^https?://[^/]+/##; s#/#_#g' <<< "$url")"; f="$PF/curl/$key"
if [[ -s "$f" ]]; then code="$(head -n 1 "$f")"; [[ "$(wc -l < "$f")" -gt 1 ]] && sed -i.bak 1d "$f"; else code=200; fi
printf '%s' "$code"
SH
cat > "$work/bin/fake-firebase" << 'SH'
#!/usr/bin/env bash
# Prints $PF/groups.json and exits with the code in $PF/firebase-exit (default 0).
code="$(cat "$PF/firebase-exit" 2>/dev/null || echo 0)"
[[ "$code" == 0 ]] && cat "$PF/groups.json"
exit "$code"
SH
chmod +x "$work/bin/fake-dig" "$work/bin/fake-curl" "$work/bin/fake-firebase"
staging_client='{"client_info": {"android_client_info": {"package_name": "com.vglstudio.camera.pose.line.guide.staging"}}, "api_key": [{"current_key": "MARKER-SECRET-1"}]}'
other_client='{"client_info": {"android_client_info": {"package_name": "com.example.other"}}, "api_key": [{"current_key": "MARKER-SECRET-1"}]}'
google_services() { # <project_id> <client JSON>
  printf '{"project_info": {"project_id": "%s", "project_number": "987654321"}, "client": [%s]}' "$1" "$2"
}
preflight_reset() { # sets up a ready environment; a test then breaks one thing
  rm -rf "$pf"; mkdir -p "$pf/curl"
  google_services proj-test-1 "$staging_client" > "$pf/google-services.json"
  echo '{"type": "service_account", "private_key": "MARKER-SECRET-1"}' > "$pf/credentials.json"
  echo '{"status": "success", "result": [{"name": "projects/987654321/groups/team", "displayName": "Team"}, {"alias": "qa"}]}' > "$pf/groups.json"
  printf '104.21.40.144\n172.67.153.9\n' > "$pf/dig-1.1.1.1"
  printf '104.21.40.144\n' > "$pf/dig-8.8.8.8"
  printf '# Required files\n\nposes/catalog/v1/catalog-587b5394.json\n' > "$pf/assets.txt"
}
preflight() { # prints the exit code; output in $pf/out, step summary in $pf/summary
  local code=0
  PF="$pf" GOOGLE_SERVICES_FILE="${PF_GOOGLE:-$pf/google-services.json}" \
    DISTRIBUTION_CREDENTIALS_FILE="$pf/credentials.json" TESTER_GROUPS="${PF_GROUPS:-team}" \
    REQUIRED_ASSETS="$pf/assets.txt" DIG_CMD="$work/bin/fake-dig" CURL_CMD="$work/bin/fake-curl" \
    FIREBASE_CMD="$work/bin/fake-firebase" PREFLIGHT_RETRY_SECONDS=0 GITHUB_STEP_SUMMARY="$pf/summary" \
    scripts/preflight.sh > "$pf/out" 2>&1 || code=$?
  cat "$pf/out" >> "$work/preflight-all.out"; cat "$pf/summary" >> "$work/preflight-all.out" 2>/dev/null || true
  echo "$code"
}
pf_lines() { grep -c "$1" "$pf/out" || true; }
: > "$work/preflight-all.out"

preflight_reset
expect "preflight: a ready environment passes" 0 "$(preflight)"
expect "preflight: a ready environment has one QUA line per check" 4 "$(pf_lines '^QUA: ')"
expect "preflight: the domain is read from app/build.gradle.kts" 2 "$(grep -c 'camera-pose.vgl.io.vn' "$pf/dig.args" 2>/dev/null || true)"
expect "preflight: existing tester groups pass" 0 "$(PF_GROUPS=team,qa preflight)"

preflight_reset; google_services fake-staging-project "$staging_client" > "$pf/google-services.json"
expect "preflight: a fake google-services is refused" 1 "$(preflight)"
expect "preflight: says the google-services is the fake one" 1 "$(pf_lines '^LỖI: .*file cấu hình giả')"
preflight_reset; google_services proj-test-1 "$other_client" > "$pf/google-services.json"
expect "preflight: google-services without the staging app is refused" 1 "$(preflight)"
expect "preflight: names the missing staging app" 1 "$(pf_lines '^LỖI: .*com.vglstudio.camera.pose.line.guide.staging')"
preflight_reset
expect "preflight: missing secret is refused" 1 "$(PF_GOOGLE="$pf/none.json" preflight)"
expect "preflight: names the missing secret" 1 "$(pf_lines '^LỖI: thiếu bí mật STAGING_GOOGLE_SERVICES_JSON')"
preflight_reset; echo 'not json MARKER-SECRET-1' > "$pf/google-services.json"
expect "preflight: a secret that is not JSON is refused" 1 "$(preflight)"
expect "preflight: says the secret is not JSON" 1 "$(pf_lines '^LỖI: .*không phải JSON')"

preflight_reset; echo '{"result": [{"alias": "other"}]}' > "$pf/groups.json"
expect "preflight: a missing tester group is refused" 1 "$(preflight)"
expect "preflight: names the missing tester group" 1 "$(pf_lines '^LỖI: App Distribution chưa có nhóm team')"
preflight_reset; echo 127 > "$pf/firebase-exit"
expect "preflight: a groups command that cannot run only warns" 0 "$(preflight)"
expect "preflight: warns it could not check the groups" 1 "$(pf_lines '^CHÚ Ý: không kiểm được nhóm người test')"

preflight_reset; : > "$pf/dig-8.8.8.8"
expect "preflight: a domain one public DNS cannot resolve is refused" 1 "$(preflight)"
expect "preflight: names the DNS server and the domain" 1 "$(pf_lines '^LỖI: camera-pose.vgl.io.vn không phân giải được qua 8.8.8.8')"

preflight_reset; printf 'poses/a.json\n#poses/commented.json\n\nposes/b.json\n' > "$pf/assets.txt"; echo 404 > "$pf/curl/poses_b.json"
expect "preflight: a missing R2 file is refused" 1 "$(preflight)"
expect "preflight: names the missing R2 file" 1 "$(pf_lines '^LỖI: poses/b.json trả 404')"
expect "preflight: comments and blank lines of the list are skipped" 0 "$(pf_lines 'commented')"
preflight_reset; printf '000\n200\n' > "$pf/curl/poses_catalog_v1_catalog-587b5394.json"
expect "preflight: a connection that fails once is retried" 0 "$(preflight)"
preflight_reset; printf '000\n000\n' > "$pf/curl/poses_catalog_v1_catalog-587b5394.json"
expect "preflight: a connection that keeps failing is refused" 1 "$(preflight)"

preflight_reset; google_services fake-staging-project "$staging_client" > "$pf/google-services.json"; echo 404 > "$pf/curl/poses_catalog_v1_catalog-587b5394.json"
expect "preflight: reports every failing check" 2 "$(preflight > /dev/null; pf_lines '^LỖI: ')"
expect "preflight: never prints secret values" 0 "$(grep -c 'MARKER-SECRET-1\|proj-test-1\|987654321' "$work/preflight-all.out" || true)"

# java-check.sh stops check.sh before Gradle when Java cannot run the project's Gradle (L-022).
fake_java() { # <version line> [lines printed before it]; writes a fake java printing them like `java -version`
  { printf '#!/bin/sh\n'
    [ -n "${2:-}" ] && printf '%s\n' "$2" | while IFS= read -r line; do printf 'echo '"'"'%s'"'"' >&2\n' "$line"; done
    printf 'echo '"'"'%s'"'"' >&2\necho "OpenJDK Runtime Environment" >&2\n' "$1"; } > "$work/bin/fake-java"
  chmod +x "$work/bin/fake-java"
}
java_check() { local code=0; JAVA_CMD="${1:-$work/bin/fake-java}" scripts/java-check.sh > "$work/java.out" 2>&1 || code=$?; echo "$code"; }
fake_java 'openjdk version "17.0.12" 2024-07-16'
expect "java-check: Java 17 passes" 0 "$(java_check)"
fake_java 'openjdk version "21.0.13" 2025-10-21 LTS'
expect "java-check: Java 21 passes" 0 "$(java_check)"
fake_java 'openjdk version "26" 2026-03-17'
expect "java-check: Java 26 is refused with a JAVA_HOME hint" "1 1" "$(java_check) $(grep -c 'JAVA_HOME' "$work/java.out" | tr -d ' ' | sed 's/^[1-9][0-9]*$/1/')"
fake_java 'java version "1.8.0_392"'
expect "java-check: Java 1.8 is refused" 1 "$(java_check)"
expect "java-check: names Java 8" 1 "$(grep -c '^LỖI: Java 8 ' "$work/java.out" || true)"
expect "java-check: no java is refused" 1 "$(java_check "$work/bin/no-such-java")"
# The JVM prints "Picked up JAVA_TOOL_OPTIONS: ..." before the version when the variable is set, as in the cloud (L-049).
fake_java 'openjdk version "21.0.12.1" 2026-07-21' 'Picked up JAVA_TOOL_OPTIONS: -Dhttps.proxyHost=proxy'
expect "java-check: Java 21 after a Picked up JAVA_TOOL_OPTIONS line passes" "0 1" "$(java_check) $(grep -c '^Java 21: dùng được' "$work/java.out" || true)"
fake_java 'openjdk version "17.0.12" 2024-07-16' "$(printf 'Picked up JAVA_TOOL_OPTIONS: -Da=1\nPicked up _JAVA_OPTIONS: -Db=2')"
expect "java-check: Java 17 after two Picked up lines passes" 0 "$(java_check)"
fake_java 'openjdk version "26" 2026-03-17' 'Picked up JAVA_TOOL_OPTIONS: -Da=1'
expect "java-check: Java 26 after a Picked up line is refused" "1 1" "$(java_check) $(grep -c '^LỖI: Java 26 ' "$work/java.out" || true)"
java_line="$(grep -n 'scripts/java-check.sh' scripts/check.sh | head -n 1 | cut -d: -f1 || true)"
gradle_line="$(grep -n '"${GRADLE\[@\]}"' scripts/check.sh | head -n 1 | cut -d: -f1 || true)"
expect "check.sh calls java-check.sh before the first Gradle call" yes \
  "$([[ -n "$java_line" && "$java_line" -lt "$gradle_line" ]] && echo yes || echo no)"

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
# The offline scenario turns every radio off and runs only when the device really has no network (L-031).
export DEVICE_SESSION_SLEEP=0 FAKE_ADB_CALLS="$work/adb-calls.txt"
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
unset DEVICE_SESSION_SLEEP FAKE_ADB_CALLS FAKE_ADB_RUNTIME_PERMISSIONS

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

if [[ "$failures" -gt 0 ]]; then
  echo "$failures script test(s) failed"
  exit 1
fi
echo "All script tests passed"
