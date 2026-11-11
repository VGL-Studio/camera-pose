#!/usr/bin/env bash
# Waits for a GitHub Actions run inside one command, so an AI session does not ask for the status again and again:
# every question is a model call that reads the whole conversation again (docs/auto-run.md, steps 8 and 4).
#
# Usage:  scripts/wait-run.sh --pr <number> [--sha <commit>] [--timeout <minutes>]
#           waits for pr-check.yml on the newest commit of the PR branch; the head is read again on every round
#           (a run cancelled because a newer commit arrived is not red). --sha makes it wait for that commit, so
#           the run of the previous head is not taken for the one just pushed (7 to 40 hex characters). A --sha that
#           shares its first 7 characters with the head without being its prefix is a mistyped commit: code 3 at
#           once, with both commits in full. So is a --sha GitHub still does not know ("No commit found") after
#           WAIT_RUN_UNKNOWN_SHA_SECONDS (default 90): right after a push GitHub may not know the commit for a moment
#         scripts/wait-run.sh --workflow <file.yml> --since <ISO time> [--timeout <minutes>]
#           waits for the first run of that workflow on main created at or after --since
#           (the time just before the session dispatched it, e.g. 2026-10-18T02:20:00Z)
# --timeout defaults to 40 minutes. Where one command may only run 10 minutes, call again with --timeout 9.
# Exit codes: 0 the run finished green
#             1 the run of the PR's current head finished red, or was cancelled while that commit stayed the head
#               (the failed jobs are named)
#             2 still not finished when the time ran out: call again
#             3 wrong arguments, the PR cannot be read, or --sha is not a commit GitHub knows
#             4 GitHub never started the failed jobs (no runner: spending limit, billing): not a code failure (L-064)
#             5 (--pr) no run, and the PR conflicts with main: GitHub runs no pull_request workflow for it (L-064)
#             6 (--pr) the PR is a draft: pr-check does not run for drafts (ADR-0041)
#             7 a job waits for a self-hosted runner for WAIT_RUN_RUNNER_QUEUE_MINUTES (default 75) and nothing of the
#               run is going: a person must act, not a code failure (ADR-0042)
# A run of the PR that ended skipped (left from when the PR was a draft) is not a result: the wait goes on.
# Prints one line at the end. Uses only the REST API through `gh api` (gh pr is blocked in the cloud environment).
# WAIT_RUN_POLL_SECONDS (default 30) and WAIT_RUN_TIMEOUT_SECONDS override the waits (tests); so does
# WAIT_RUN_CANCEL_RECHECKS (default 3), how many rounds the head is read again after its run shows "cancelled".
# WAIT_RUN_UNKNOWN_SHA_SECONDS sets how long an unknown --sha is waited for before code 3.
# WAIT_RUN_RUNNER_QUEUE_MINUTES sets the wait of code 7; WAIT_RUN_NOW (Unix seconds) fixes the clock for the tests.
# Messages are in Vietnamese for the team; comments are in English.

set -euo pipefail

die() { echo "LỖI: $*" >&2; exit 3; }

pr="" workflow="" since="" want="" timeout_minutes=40
while [ "$#" -gt 0 ]; do
  [ "$#" -ge 2 ] || die "thiếu giá trị cho $1."
  case "$1" in
    --pr) pr="$2" ;;
    --workflow) workflow="$2" ;;
    --since) since="$2" ;;
    --sha) want="$2" ;;
    --timeout) timeout_minutes="$2" ;;
    *) die "tham số không hợp lệ: '$1'. Xem cách dùng ở đầu scripts/wait-run.sh." ;;
  esac
  shift 2
done
[[ "$timeout_minutes" =~ ^[0-9]+$ ]] || die "--timeout phải là số phút."
queue_minutes="${WAIT_RUN_RUNNER_QUEUE_MINUTES:-75}"
[[ "$queue_minutes" =~ ^[0-9]+$ ]] || die "WAIT_RUN_RUNNER_QUEUE_MINUTES phải là số phút."
unknown_sha_seconds="${WAIT_RUN_UNKNOWN_SHA_SECONDS:-90}"
[[ "$unknown_sha_seconds" =~ ^[0-9]+$ ]] || die "WAIT_RUN_UNKNOWN_SHA_SECONDS phải là số giây."
if [ -n "$want" ]; then
  [[ "$want" =~ ^[0-9a-f]{7,40}$ ]] || die "--sha phải là mã commit (7 đến 40 ký tự hex)."
  [ -n "$pr" ] && [ -z "$workflow" ] || die "--sha chỉ dùng cùng --pr."
fi
if [ -n "$pr" ]; then
  [[ "$pr" =~ ^[0-9]+$ ]] && [ -z "$workflow" ] || die "dùng --pr <số> hoặc --workflow <file> --since <giờ>, không dùng cả hai."
else
  [[ "$workflow" =~ ^[A-Za-z0-9._-]+\.ya?ml$ ]] || die "cần --pr <số>, hoặc --workflow <file.yml> --since <giờ>."
  [[ "$since" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}(:[0-9]{2})?Z$ ]] ||
    die "--since phải có dạng 2026-10-18T02:20:00Z (giờ UTC)."
  [[ "$since" == *:*:*Z ]] || since="${since%Z}:00Z"
fi

remote_url="$(git config --get remote.origin.url)"
repo="$(echo "$remote_url" | sed -E 's#(git@github.com:|https://github.com/)##; s#\.git$##')"
[[ "$repo" =~ ^[^/]+/[^/]+$ ]] || die "không xác định được repo từ remote: $remote_url"

state="" draft="false"
set_pr_query() {
  query="actions/workflows/$workflow/runs?head_sha=$sha&per_page=10"
  what="$workflow của PR #$pr (commit ${sha:0:7})"
}
# Reads the head of the PR, whether it is a draft and its mergeable state in one call. A read that fails or gives no
# commit keeps the old values, so a network error does not turn into a query for head_sha= (empty). The draft flag
# comes before the state, which may be empty: read splits on tabs and would merge two empty fields.
read_head() {
  local answer new_sha new_draft new_state
  answer="$(gh api "repos/$repo/pulls/$pr" --jq '[.head.sha, (.draft // false | tostring), (.mergeable_state // "")] | @tsv' \
    2> /dev/null)" || return 0
  IFS=$'\t' read -r new_sha new_draft new_state <<< "$answer"
  [[ "${new_sha:-}" =~ ^[0-9a-f]{7,40}$ ]] || return 0
  sha="$new_sha" draft="${new_draft:-false}" state="${new_state:-}"
}
if [ -n "$pr" ]; then
  answer="$(gh api "repos/$repo/pulls/$pr" --jq '[.head.sha, (.draft // false | tostring)] | @tsv' 2> /dev/null)" ||
    die "không đọc được PR #$pr."
  IFS=$'\t' read -r sha draft <<< "$answer"
  [[ "$sha" =~ ^[0-9a-f]{7,40}$ ]] || die "không đọc được commit của PR #$pr."
  workflow="pr-check.yml"
  # Runs come newest first; a re-run of the same commit replaces the older one.
  pick='.workflow_runs[0] // empty'
  set_pr_query
else
  query="actions/workflows/$workflow/runs?branch=main&per_page=20"
  # ISO times in the same format compare correctly as text; take the oldest run at or after --since.
  pick="[.workflow_runs[] | select(.created_at >= \"$since\")] | last // empty"
  what="$workflow trên main từ $since"
fi

poll="${WAIT_RUN_POLL_SECONDS:-30}"
started=$(date +%s)
deadline=$((started + ${WAIT_RUN_TIMEOUT_SECONDS:-$((timeout_minutes * 60))}))
run=""
seen_want=0 checked_want=0 cancelled_sha="" rechecks=0 max_rechecks="${WAIT_RUN_CANCEL_RECHECKS:-3}"
while :; do
  rechecking=0 waiting_for_push=0
  if [ -n "$pr" ]; then
    # The head can move while we wait (a push, merge-if-green.sh --update): follow it (L-087).
    read_head
    # Checked on every round, before the run and before the deadline (ADR-0041).
    if [ "$draft" = "true" ]; then
      echo "NHÁP: PR #$pr là bản nháp, pr-check không chạy cho PR nháp (ADR-0041). Chuyển PR sang sẵn sàng rồi gọi lại."
      exit 6
    fi
    if [ -n "$want" ]; then
      if [[ "$sha" == "$want"* ]]; then
        seen_want=1
      elif [ "$seen_want" = 1 ]; then
        want="" # the head went on past the commit asked for: follow it
      fi
    fi
    set_pr_query
    if [ -n "$want" ] && [[ "$sha" != "$want"* ]]; then
      # A mistyped --sha would otherwise wait the whole timeout for a commit that never comes. Asked on each round
      # until GitHub knows the commit; "No commit found" is proof only once it lasts unknown_sha_seconds, since
      # GitHub may not know a commit for a moment after the push. Other failed reads (network, 404) are not proof.
      if [ "$checked_want" = 0 ]; then
        [ "${sha:0:7}" != "${want:0:7}" ] ||
          die "--sha $want không phải commit đầu $sha của PR #$pr dù 7 ký tự đầu trùng: mã commit gõ sai. Dùng --sha \"\$(git rev-parse HEAD)\", không gõ tay."
        if lookup="$(gh api "repos/$repo/commits/$want" --jq .sha 2>&1)"; then
          checked_want=1 # GitHub knows it: wait for the PR to reach it
        elif grep -q 'No commit found' <<< "$lookup" && [ $(($(date +%s) - started)) -ge "$unknown_sha_seconds" ]; then
          die "--sha $want vẫn không có trên GitHub sau $unknown_sha_seconds giây (commit đầu của PR #$pr là $sha): mã commit gõ sai hay commit chưa được đẩy. Kiểm lệnh đẩy, rồi gọi lại với --sha \"\$(git rev-parse HEAD)\"."
        fi
      fi
      run="" waiting_for_push=1
    else
      run="$(gh api "repos/$repo/$query" --jq "$pick | {id, status, conclusion, html_url} | @json" 2> /dev/null || true)"
      # The run of the PR while it was a draft: wait for the run of ready_for_review.
      if [ -n "$run" ] && [ "$(jq -r '"\(.status) \(.conclusion)"' <<< "$run")" = "completed skipped" ]; then
        run=""
      fi
    fi
  else
    run="$(gh api "repos/$repo/$query" --jq "$pick | {id, status, conclusion, html_url} | @json" 2> /dev/null || true)"
  fi
  if [ -n "$run" ] && [ "$(jq -r .status <<< "$run")" = "completed" ]; then
    if [ -z "$pr" ] || [ "$(jq -r .conclusion <<< "$run")" != "cancelled" ]; then
      break
    fi
    # GitHub cancels the run of a commit once a newer one exists, but the PR may still show the old head for a few
    # seconds: read the head again before calling the cancel red. Counted per commit.
    if [ "$sha" != "$cancelled_sha" ]; then cancelled_sha="$sha" rechecks=0; fi
    [ "$rechecks" -lt "$max_rechecks" ] || break # the head stayed: a real cancel
    rechecks=$((rechecks + 1)) rechecking=1
  fi
  # No run yet for a PR that conflicts with main: none will come until the conflict is solved (L-064).
  # "unknown" means GitHub has not computed it yet: keep waiting. Not while waiting for --sha: the state read
  # belongs to the previous commit.
  if [ -z "$run" ] && [ -n "$pr" ] && [ "$waiting_for_push" = 0 ] && [ "$state" = "dirty" ]; then
    echo "XUNG ĐỘT: PR #$pr xung đột với nhánh chính, GitHub không chạy pr-check cho PR này. Gộp nhánh chính vào nhánh (git merge origin/main), sửa xung đột, đẩy, rồi gọi lại (L-064)."
    exit 5
  fi
  # A job queued for a self-hosted runner, while no job of the run is going, for queue_minutes since it could start:
  # the latest end of a finished job of the run, else the job's creation (ADR-0042, section 5). A failed read or an
  # odd answer skips this round.
  if [ -n "$run" ] && [ "$(jq -r .status <<< "$run")" != "completed" ]; then
    waiting="$(gh api "repos/$repo/actions/runs/$(jq -r .id <<< "$run")/jobs?per_page=50" 2> /dev/null |
      jq -r --argjson now "${WAIT_RUN_NOW:-$(date -u +%s)}" --argjson min "$queue_minutes" '
        (.jobs // []) as $j
        | if any($j[]; .status == "in_progress") then empty else
          ([$j[] | select(.status == "completed") | .completed_at // empty] | max) as $done
          | [$j[] | select(.status == "queued" and ((.runner_name // "") == "")
              and any(.labels[]?; test("^(ubuntu|windows|macos)-") | not))
            | . + {since: ($done // .created_at)}
            | select((.since | fromdateiso8601) <= ($now - $min * 60))]
          | first // empty | "\(.name)\t\(.labels | join(","))\t\(.since)"
          end' 2> /dev/null || true)"
    if [ -n "$waiting" ]; then
      IFS=$'\t' read -r job_name job_labels job_since <<< "$waiting"
      echo "CHỜ RUNNER: job $job_name của $what, lần chạy $(jq -r .id <<< "$run"), chờ runner tự host (nhãn $job_labels) từ $job_since, không còn job nào của lần chạy đang chạy. Không phải lỗi của code: không sửa code, không chạy lại, không revert; ghi vào \"Cần người kiểm tra\": máy runner hay biến HEAVY_RUNNER của repo (docs/ops/self-hosted-runner.md, mục 8); phiên làm theo docs/auto-run.md, mục 4 (ADR-0042). $(jq -r .html_url <<< "$run")"
      exit 7
    fi
  fi
  if [ "$(date +%s)" -ge "$deadline" ]; then
    if [ "$waiting_for_push" = 1 ]; then
      echo "CHƯA XONG sau $timeout_minutes phút: PR #$pr chưa nhận commit $want (commit đầu đang là $sha). Kiểm tra lệnh đẩy, rồi gọi lại script."
    elif [ "$rechecking" = 1 ]; then
      echo "CHƯA XONG sau $timeout_minutes phút: $what, lần chạy $(jq -r .id <<< "$run") bị hủy, đang kiểm commit đầu của PR. Gọi lại script."
    elif [ -n "$run" ]; then
      echo "CHƯA XONG sau $timeout_minutes phút: $what, lần chạy $(jq -r .id <<< "$run") ($(jq -r .status <<< "$run")). Gọi lại script để chờ tiếp."
    else
      echo "CHƯA XONG sau $timeout_minutes phút: chưa thấy lần chạy nào của $what. Gọi lại script, hoặc kiểm tra workflow đã được khởi động chưa."
    fi
    exit 2
  fi
  sleep "$poll"
done

id="$(jq -r .id <<< "$run")"
conclusion="$(jq -r .conclusion <<< "$run")"
url="$(jq -r .html_url <<< "$run")"
if [ "$conclusion" = "success" ]; then
  echo "XANH: $what, lần chạy $id. $url"
  exit 0
fi
jobs="$(gh api "repos/$repo/actions/runs/$id/jobs?per_page=50" 2> /dev/null || true)"
[ -n "$jobs" ] || jobs='{}'
failed="$(jq -r '[.jobs[]? | select(.conclusion == "failure" or .conclusion == "cancelled" or .conclusion == "timed_out") | .name] | join(", ")' \
  <<< "$jobs" 2> /dev/null || true)"
# A job GitHub never started (no runner: spending limit, billing) has no steps. Only when every failed job is such a
# job is the run "infrastructure": one failed job that ran may be the code (L-064). A job without the "steps" key
# (unknown) does not count as unstarted.
unstarted="$(jq -r '[.jobs[]? | select(.conclusion == "failure")] as $f
  | if ($f | length) > 0 and ($f | all(.steps? == [])) then ($f | map(.id | tostring) | join(" ")) else "" end' \
  <<< "$jobs" 2> /dev/null || true)"
if [ -n "$unstarted" ]; then
  reason="$(gh api "repos/$repo/check-runs/${unstarted%% *}/annotations" \
    --jq '[.[] | select(.annotation_level == "failure") | .message] | first // empty' 2> /dev/null || true)"
  echo "HẠ TẦNG: GitHub không chạy job ${failed:-không rõ} của $what, lần chạy $id: ${reason:-GitHub không ghi lý do}. Không phải lỗi của code: không sửa code, không revert; người kiểm hạn mức GitHub Actions (Billing & plans), phiên làm theo docs/auto-run.md, mục 4 (L-064). $url"
  exit 4
fi
echo "ĐỎ ($conclusion): $what, lần chạy $id; job: ${failed:-không rõ}. $url"
exit 1
