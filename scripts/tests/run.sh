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
write_run_plan() { # <id> <file> <test row>
  printf -- '---\nticket: %s\nstatus: approved\nrisk_level: medium\nreviewer: A\napproved_at: 2026-10-02\n---\n\n### 3.2. Danh sách file\n\n| Thao tác | Đường dẫn | Nội dung |\n| --- | --- | --- |\n| Sửa | `%s` | x |\n\n## 4. Kiểm thử\n\n| Tiêu chí | Loại | Tên |\n| --- | --- | --- |\n%s\n\n## 9. Quyết định duyệt\n\n- **Quyết định:** Duyệt\n- **Người duyệt:** A\n' \
    "$1" "$2" "$3" > "$rr/docs/features/demo/plans/$1-x.md"
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
  "$(grep -c '^RUN-20261018-01,2026-10-18,T-031,M,2000,2,20,200,2000,180,2400$' "$tokens/docs/metrics/t.csv")"
expect "token-usage: no transcripts is not a failure" 0 \
  "$(TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/none" python3 -I scripts/token-usage.py report > /dev/null; echo $?)"
expect "token-usage: estimate falls back to the starting value" 1 \
  "$(token_usage estimate --csv docs/metrics/t.csv | grep -c '^| M | 1 | 2k | 20.0M (tạm')"

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
status_page > /dev/null
page="$st/STATUS.md"
expect "status: request without spec waits for people" 1 "$(grep -c '^- Yêu cầu chưa có spec: .*(Log cho AI)' "$page")"
expect "status: draft plan, new feedback, proposed lesson and ADR wait for people" 4 \
  "$(grep -cE '^- (Plan .draft.|Phản hồi chờ phân loại|Bài học .proposed.|ADR .proposed.)' "$page")"
expect "status: request shows tickets done" 1 "$(grep -c '| demo | .specified. đã có spec | 1/2 |$' "$page")"
expect "status: ticket row has plan, estimate and runs" 1 \
  "$(grep -c '^| \[T-001\](features/demo/tickets/T-001-mot.md) | Một | .on-staging. | .done. (bản 2) | - | RUN-20261004-01 |$' "$page")"
expect "status: check passes on a fresh page" 0 "$(status_page --check > /dev/null; echo $?)"

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

# device-session.sh records a test session and counts crashes and ANRs; adb is replaced by scripts/tests/fake-adb.
cp scripts/tests/fake-adb "$work/bin/adb"
chmod +x "$work/bin/adb"
fixtures=scripts/tests/fixtures/device-session
session() { # <logcat fixture> <command...>; prints the exit code
  local code=0
  PATH="$work/bin:$PATH" DEVICE_SESSION_DIR="$work/sessions" DEVICE_SESSION_REPORTS="$work/reports" \
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
expect "device-session: scenarios run offline" 0 \
  "$(session "$fixtures/logcat-clean.txt" start ci > /dev/null; session /dev/null scenarios --yes)"
offline="$(find "$work/sessions" -maxdepth 1 -name "TS-*" | sort | tail -n 1)"
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
unset DEVICE_SESSION_SLEEP FAKE_ADB_CALLS

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
