#!/usr/bin/env python3
"""Chooses and checks the run list that .github/workflows/start-run.yml may start (ADR-0049, docs/auto-run.md).

Usage:
  scripts/auto-start-check.py push --before <sha> --after <sha>
      The run list that this push to main moved to "approved", with "start: auto" and "open_with: routine".
  scripts/auto-start-check.py dispatch <run_file> [--force] [--recheck]
      A start by hand; --recheck is the second check in the fire job, just before the routine is fired.

Prints the run file that may start (docs/runs/RUN-YYYYMMDD-NN.md) on stdout, or nothing; explains on stderr.
Exit 0 when checked (with a run file, or a push that approved no list), 3 when a run list is refused by the common
checks, 1 when git or GitHub could not be read, 2 on wrong arguments.

The routine API has no idempotency key: every successful call is a new session. So every start, by push or by hand,
is refused when:
  4. the run file on the tip of main is no longer "approved" (a re-run of an old push run reuses its before and after);
  1. the list already has its report branch claude/<RUN>-report (--force passes when that branch is "running": a
     session reopened by hand);
  2. another list is "running" on its report branch (never skipped);
  3. another fire job of start-run.yml holds a session (--force skips it): one in progress; one waiting that already
     names its list (not counted with --recheck: it waits behind this one in the start-run concurrency group and
     checks again when its turn comes); or one whose step "Kích hoạt routine" ran (even if the job then failed or was
     cancelled) within AUTO_START_FIRE_MINUTES, unless the list it names already has a report branch or is no longer
     approved on main. The job is named "fire <run_file>" and the step keeps that name: do not rename either.

Uses only git and `gh api` (REST); GITHUB_REPOSITORY or the origin remote gives the repository. GITHUB_RUN_ID and
GITHUB_RUN_ATTEMPT name this run; AUTO_START_NOW (ISO time) and AUTO_START_FIRE_MINUTES are for tests.
Messages are in Vietnamese for the team.
"""

import argparse
import json
import os
import re
import subprocess
import sys
from datetime import datetime, timedelta, timezone

RUN_FILE = re.compile(r"docs/runs/(RUN-[0-9]{8}-[0-9]{2})\.md")
REPORT_BRANCH = re.compile(r"^claude/(RUN-[0-9]{8}-[0-9]{2})-report$")
FIRE_MINUTES = int(os.environ.get("AUTO_START_FIRE_MINUTES", "90"))
FIRE_STEP = "Kích hoạt routine"
WAITING = {"queued", "waiting", "pending", "requested"}
STEP_RAN = {"success", "failure", "cancelled"}


class Refused(Exception):
    pass


def say(message):
    print(message, file=sys.stderr)


def gh(path, raw=False):
    """(ok, text): ok is False on a 404, LookupError on any other failure."""
    command = ["gh", "api", f"repos/{REPO}/{path}"]
    if raw:
        command += ["-H", "Accept: application/vnd.github.raw"]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode == 0:
        return True, result.stdout
    if "404" in result.stderr:
        return False, ""
    raise LookupError(f"{path}: {result.stderr.strip()}")


def api(path):
    ok, text = gh(path)
    if not ok:
        raise LookupError(f"{path}: HTTP 404")
    return json.loads(text or "null")


def repository():
    if os.environ.get("GITHUB_REPOSITORY"):
        return os.environ["GITHUB_REPOSITORY"]
    url = subprocess.run(["git", "config", "--get", "remote.origin.url"], capture_output=True, text=True).stdout
    return re.sub(r"^(git@github\.com:|https://github\.com/)|\.git\s*$", "", url.strip())


def front_matter(text):
    match = re.match(r"^---\n(.*?)\n---\n", text or "", re.S)
    fields = {}
    for line in (match.group(1).splitlines() if match else []):
        if ":" in line:
            key, value = line.split(":", 1)
            fields[key.strip()] = value.split("#", 1)[0].strip()
    return fields


def moment(text):
    try:
        value = datetime.fromisoformat(re.sub(r"Z$", "+00:00", (text or "").strip()))
    except ValueError:
        return None
    return value if value.tzinfo else value.replace(tzinfo=timezone.utc)


def git(*args):
    result = subprocess.run(["git", *args], capture_output=True, text=True)
    return result.returncode, result.stdout


def file_at(commit, path):
    code, text = git("show", f"{commit}:{path}")
    return text if code == 0 else None


# --- choosing the list of a push -------------------------------------------------------------------------------

def chosen_by_push(before, after):
    if git("cat-file", "-e", f"{after}^{{commit}}")[0] != 0:
        raise LookupError(f"không có commit {after} trong kho")
    if re.fullmatch(r"0+", before) or git("cat-file", "-e", f"{before}^{{commit}}")[0] != 0:
        code, parent = git("rev-parse", "--verify", "--quiet", f"{after}^1")
        if code != 0:
            say("Lần push không có commit trước để so (commit đầu tiên): không chọn danh sách nào.")
            return None
        before = parent.strip()
    code, out = git("diff", "--name-status", "-M", "--diff-filter=AMR", before, after, "--", "docs/runs/")
    if code != 0:
        raise LookupError("git diff không chạy được")
    picked = []
    for line in out.splitlines():
        parts = line.split("\t")
        old, new = (parts[1], parts[2]) if parts[0].startswith("R") else (parts[1], parts[1])
        match = RUN_FILE.fullmatch(new)
        if not match:
            continue
        run_id = match.group(1)
        fields = front_matter(file_at(after, new))
        if fields.get("status") != "approved":
            say(f"{run_id}: chưa approved ở lần push này ('{fields.get('status', '')}').")
            continue
        start = fields.get("start", "").lower()  # read as run-check.py reads it
        if not start:
            say(f"{run_id}: không ghi start, không tự bắt đầu (bấm start-run như cũ).")
            continue
        if start != "auto":
            say(f"{run_id}: start là {start}, không tự bắt đầu.")
            continue
        if fields.get("open_with", "").lower() != "routine":
            say(f"{run_id}: open_with là '{fields.get('open_with', '')}', không phải routine: workflow chỉ kích hoạt được routine.")
            continue
        earlier = file_at(before, old)
        if earlier is not None and front_matter(earlier).get("status") == "approved":
            say(f"{run_id}: đã approved trước lần push này, không bắt đầu lại.")
            continue
        picked.append(new)
    if not picked:
        return None
    picked.sort()
    if len(picked) > 1:
        rest = ", ".join(RUN_FILE.fullmatch(p).group(1) for p in picked[1:])
        say(f"Lần push duyệt nhiều danh sách: bắt đầu {RUN_FILE.fullmatch(picked[0]).group(1)}; {rest} chờ, dev bấm start-run sau.")
    return picked[0]


# --- common checks ----------------------------------------------------------------------------------------------

def status_on_main(run_file):
    ok, text = gh(f"contents/{run_file}?ref=main", raw=True)
    return front_matter(text).get("status") if ok else None


def has_report_branch(run_id):
    return gh(f"branches/claude%2F{run_id}-report")[0]


def report_branches():
    names = []
    for page in range(1, 51):
        batch = api(f"branches?per_page=100&page={page}") or []
        names += [b.get("name", "") for b in batch]
        if len(batch) < 100:
            break
    return [n for n in names if REPORT_BRANCH.match(n)]


def fire_jobs():
    """(run id, job) of every fire job of start-run.yml worth looking at."""
    own = os.environ.get("GITHUB_RUN_ID", "")
    attempt = int(os.environ.get("GITHUB_RUN_ATTEMPT") or "1")
    runs = api("actions/workflows/start-run.yml/runs?per_page=20")
    if not isinstance(runs, dict) or not isinstance(runs.get("workflow_runs"), list):
        raise LookupError("actions/workflows/start-run.yml/runs: câu trả lời lạ")
    found = []
    ids = [str(r.get("id")) for r in runs["workflow_runs"] if str(r.get("id")) != own]
    for run_id in ids:
        found += [(run_id, job) for job in jobs_of(run_id)]
    # Earlier attempts of this very run (a re-run): read directly, the run may be older than the 20 listed.
    if own and attempt > 1:
        found += [(own, job) for job in jobs_of(own) if int(job.get("run_attempt") or 0) < attempt]
    return found


def jobs_of(run_id):
    path = f"actions/runs/{run_id}/jobs?filter=all&per_page=50"
    data = api(path)
    if not isinstance(data, dict) or not isinstance(data.get("jobs"), list):
        raise LookupError(f"{path}: câu trả lời lạ")
    return [j for j in data["jobs"] if j.get("name") == "fire" or str(j.get("name", "")).startswith("fire ")]


def holding_fire(recheck, now):
    """A description of the fire job that holds a session, or None."""
    for run_id, job in fire_jobs():
        name = str(job.get("name", ""))
        named = RUN_FILE.fullmatch(name[len("fire "):]) if name.startswith("fire ") else None
        status = job.get("status")
        if status == "in_progress":
            return f"job {name} của lần chạy {run_id} đang chạy"
        if status in WAITING:
            if named and not recheck:
                return f"job {name} của lần chạy {run_id} đang chờ"
            continue
        if status != "completed":
            continue
        step = next((s for s in job.get("steps") or [] if s.get("name") == FIRE_STEP), None)
        if not step or step.get("conclusion") not in STEP_RAN:
            continue
        done = moment(job.get("completed_at"))
        if not done or now - done > timedelta(minutes=FIRE_MINUTES):
            continue
        if named:
            fired = named.group(0)
            if has_report_branch(named.group(1)) or status_on_main(fired) != "approved":
                continue
        minutes = int((now - done).total_seconds() // 60)
        return f"job {name} của lần chạy {run_id} đã gọi API kích hoạt {minutes} phút trước, phiên đó chưa có nhánh báo cáo"
    return None


def common_checks(run_file, force, recheck):
    run_id = RUN_FILE.fullmatch(run_file).group(1)
    # 4
    status = status_on_main(run_file)
    if status != "approved":
        where = "không còn trên nhánh chính" if status is None else f"đang '{status}'"
        raise Refused(f"{run_id} không còn approved trên nhánh chính ({where}). Không bắt đầu phiên.")
    # 1
    if has_report_branch(run_id):
        ok, text = gh(f"contents/{run_file}?ref=claude/{run_id}-report", raw=True)
        own_status = front_matter(text).get("status") if ok else None
        if not (force and own_status == "running"):
            hint = ("Phiên của danh sách này đang dừng giữa chừng: muốn mở lại thì bấm start-run với force."
                    if own_status == "running" else "Danh sách đã chạy; lập danh sách mới.")
            raise Refused(f"{run_id} đã có nhánh báo cáo claude/{run_id}-report ({own_status or 'không đọc được file'}). {hint}")
    # 2
    for branch in report_branches():
        other = REPORT_BRANCH.match(branch).group(1)
        if other == run_id:
            continue
        ok, text = gh(f"contents/docs/runs/{other}.md?ref={branch}", raw=True)
        if ok and front_matter(text).get("status") == "running":
            raise Refused(f"Đang có phiên {other} chạy (nhánh {branch}). Chờ phiên đó xong rồi bấm start-run.")
    # 3
    if not force:
        now = moment(os.environ["AUTO_START_NOW"]) if os.environ.get("AUTO_START_NOW") else datetime.now(timezone.utc)
        holder = holding_fire(recheck, now)
        if holder:
            raise Refused(f"Không bắt đầu {run_id}: start-run vừa kích hoạt một phiên ({holder}). Chờ phiên đó đẩy "
                          "nhánh báo cáo; biết chắc nó không bắt đầu thì bấm start-run với force.")


def parse(argv):
    parser = argparse.ArgumentParser(prog="auto-start-check.py")
    sub = parser.add_subparsers(dest="mode", required=True)
    push = sub.add_parser("push")
    push.add_argument("--before", required=True)
    push.add_argument("--after", required=True)
    dispatch = sub.add_parser("dispatch")
    dispatch.add_argument("run_file")
    dispatch.add_argument("--force", action="store_true")
    dispatch.add_argument("--recheck", action="store_true")
    args = parser.parse_args(argv)
    if args.mode == "dispatch" and not RUN_FILE.fullmatch(args.run_file):
        parser.error("đường dẫn danh sách chạy phải có dạng docs/runs/RUN-YYYYMMDD-NN.md")
    if args.mode == "push":
        for sha in (args.before, args.after):
            if not re.fullmatch(r"[0-9a-f]{7,64}", sha):
                parser.error(f"mã commit không hợp lệ: {sha!r}")
    return args


def main(argv):
    args = parse(argv)
    try:
        if args.mode == "push":
            run_file = chosen_by_push(args.before, args.after)
            if not run_file:
                return 0
            common_checks(run_file, force=False, recheck=False)
        else:
            run_file = args.run_file
            common_checks(run_file, force=args.force, recheck=args.recheck)
    except Refused as refusal:
        say(str(refusal))
        return 3
    except (LookupError, json.JSONDecodeError) as error:
        say(f"Không đọc được git hay GitHub ({error}). Không bắt đầu phiên.")
        return 1
    say(f"{run_file}: được bắt đầu.")
    print(run_file)
    return 0


if __name__ == "__main__":
    REPO = repository()
    sys.exit(main(sys.argv[1:]))
