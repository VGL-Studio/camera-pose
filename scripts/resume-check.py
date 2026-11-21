#!/usr/bin/env python3
"""Finds an auto-run session that stopped in the middle, usually because the usage quota ran out, so that
.github/workflows/resume-run.yml can start it again once the quota is renewed (docs/auto-run.md, section 4).

Usage: scripts/resume-check.py
Prints the run file to continue (docs/runs/RUN-YYYYMMDD-NN.md) on stdout, or nothing. Explains on stderr.
Exit 0 when the check ran (with or without a session to continue), 1 when GitHub could not be read or
scripts/runners-app.conf is wrong.

A session has stopped when its run file on its report branch is still "running" and none of its branches
(the report branch, claude/T-NNN-... and claude/fix-T-NNN-... of the tickets in its run list) got a commit
for STALE_MINUTES. A session that is alive pushes at every step (docs/auto-run.md, section 3), and its longest
wait (CI, emulator) is shorter than that, except a job waiting for a self-hosted runner, which this script checks on
its own. It is not continued when it started more than MAX_HOURS ago: people decide then. Nor while main is red
(main-broken) or the cloud environment is broken (env-broken), or while a job of the repository waits for a
self-hosted runner (ADR-0042; scripts/wait-run.sh exit 7), however long it has waited: below the wait of exit 7 the
session may still be waiting for it, so a second session would overlap.

scripts/runners-app.conf, only in an app whose runner model is not ADR-0042 (an ADR of the app), may set the runner
page the message names (RUNNER_DOC); the file is read and checked as scripts/wait-run.sh reads it, and its
RUNNER_QUEUE_MINUTES and RUNNER_LABELS do not apply here. RUNNERS_APP_CONF (tests) names another file.

The workflow runs it every hour while its schedule is on; it turns itself off when idle (scripts/schedule-idle.py).

Uses only `gh api` (REST); GITHUB_REPOSITORY or the origin remote gives the repository.
RESUME_STALE_MINUTES, RESUME_MAX_HOURS and RESUME_NOW (ISO time, tests) override the defaults.
Messages are in Vietnamese for the team.
"""

import json
import os
import re
import subprocess
import sys
from datetime import datetime, timedelta, timezone

STALE_MINUTES = int(os.environ.get("RESUME_STALE_MINUTES", "90"))
MAX_HOURS = int(os.environ.get("RESUME_MAX_HOURS", "24"))
REPORT_BRANCH = re.compile(r"^claude/(RUN-\d{8}-\d{2})-report$")


def say(message):
    print(message, file=sys.stderr)


def api(path, raw=False):
    command = ["gh", "api", f"repos/{REPO}/{path}"]
    if raw:
        command += ["-H", "Accept: application/vnd.github.raw"]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode != 0:
        raise LookupError(path)
    return result.stdout if raw else json.loads(result.stdout or "null")


def api_all(path):
    """Every item of a list endpoint, page by page with "&page=N" (the first page alone misses branches past 100,
    L-092). The path must already end in "per_page=100"."""
    items = []
    for page in range(1, 51):
        batch = api(f"{path}&page={page}") or []
        items.extend(batch)
        if len(batch) < 100:
            break
    return items


def repository():
    if os.environ.get("GITHUB_REPOSITORY"):
        return os.environ["GITHUB_REPOSITORY"]
    url = subprocess.run(["git", "config", "--get", "remote.origin.url"], capture_output=True, text=True).stdout
    return re.sub(r"^(git@github\.com:|https://github\.com/)|\.git\s*$", "", url.strip())


def moment(text):
    """ISO time, or the started_at form of a run file: '2026-10-17 16:19 UTC'."""
    try:
        value = datetime.fromisoformat(re.sub(r"\s*(UTC|Z)$", "+00:00", (text or "").strip()))
    except ValueError:
        return None
    return value if value.tzinfo else value.replace(tzinfo=timezone.utc)


def front_matter(text):
    match = re.match(r"^---\n(.*?)\n---\n", text or "", re.S)
    fields = {}
    for line in (match.group(1).splitlines() if match else []):
        if ":" in line:
            key, value = line.split(":", 1)
            fields[key.strip()] = value.split("#", 1)[0].strip()
    return fields


def last_commit(branch):
    try:
        return moment(api(f"commits/{branch}")["commit"]["committer"]["date"])
    except (LookupError, KeyError, TypeError):
        return None


GITHUB_LABEL = re.compile(r"^(ubuntu|windows|macos)-")
RUNNERS_CONF_KEYS = {
    "RUNNER_QUEUE_MINUTES": r"[1-9][0-9]*",
    "RUNNER_LABELS": r"[A-Za-z0-9._-]+(,[A-Za-z0-9._-]+)*",
    "RUNNER_DOC": r"[A-Za-z0-9_][A-Za-z0-9._/-]*",
    "RUNNER_TESTS": r"skip",
    "RUNNER_ADR": r"ADR-[0-9]{4}",
}


def runners_conf():
    """The values of scripts/runners-app.conf, {} when there is no file: lines KEY=value, '#' comments and blank
    lines, only the keys of RUNNERS_CONF_KEYS, each once, with plain values. Read, never run. ValueError says what is
    wrong."""
    path = os.environ.get("RUNNERS_APP_CONF") or os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                                              "runners-app.conf")
    values = {}
    if not os.path.isfile(path):
        return values
    with open(path, encoding="utf-8") as handle:
        lines = handle.read().split("\n")
    for number, line in enumerate(lines, 1):
        line = line.removesuffix("\r")
        if re.fullmatch(r"[ \t]*(#.*)?", line):
            continue
        match = re.fullmatch(r"([A-Z_]+)=(.*)", line)
        if not match:
            raise ValueError(f"{path}: dòng {number} không có dạng KHÓA=giá trị")
        key, value = match.groups()
        if key not in RUNNERS_CONF_KEYS:
            raise ValueError(f"{path}: dòng {number}: khóa lạ {key} (chỉ nhận {', '.join(RUNNERS_CONF_KEYS)})")
        if not re.fullmatch(RUNNERS_CONF_KEYS[key], value, re.ASCII):
            raise ValueError(f"{path}: dòng {number}: giá trị của {key} sai dạng "
                             "(xem docs/ops/self-hosted-runner.md, mục 10)")
        if key in values:
            raise ValueError(f"{path}: dòng {number}: {key} ghi hai lần")
        values[key] = value
    return values


def waiting_job():
    """(job name, Actions run id) of the first queued job that carries a label of no GitHub runner, in the runs
    queued or in progress (ADR-0042); None when there is none. RUNNER_LABELS of scripts/runners-app.conf does not
    narrow this: a self-hosted job with a label left out of the file would then let a second session start. An
    answer of another shape is a failed read."""
    for status in ("queued", "in_progress"):
        path = f"actions/runs?status={status}&per_page=20"
        runs = api(path)
        if not isinstance(runs, dict) or not isinstance(runs.get("workflow_runs"), list):
            raise LookupError(path)
        for run in runs["workflow_runs"]:
            jobs_path = f"actions/runs/{run.get('id')}/jobs?per_page=50"
            jobs = api(jobs_path)
            if not isinstance(jobs, dict) or not isinstance(jobs.get("jobs"), list):
                raise LookupError(jobs_path)
            for job in jobs["jobs"]:
                labels = job.get("labels") or []
                if job.get("status") == "queued" and any(not GITHUB_LABEL.match(str(label)) for label in labels):
                    return job.get("name"), run.get("id")
    return None


def main():
    now = moment(os.environ["RESUME_NOW"]) if os.environ.get("RESUME_NOW") else datetime.now(timezone.utc)
    branches = [b["name"] for b in api_all("branches?per_page=100")]
    for branch in sorted(branches):
        match = REPORT_BRANCH.match(branch)
        if not match:
            continue
        run_id = match.group(1)
        run_file = f"docs/runs/{run_id}.md"
        try:
            text = api(f"contents/{run_file}?ref={branch}", raw=True)
        except LookupError:
            continue
        fields = front_matter(text)
        if fields.get("status") != "running":
            continue
        started = moment(fields.get("started_at"))
        if started and now - started > timedelta(hours=MAX_HOURS):
            say(f"{run_id} vẫn running nhưng đã bắt đầu hơn {MAX_HOURS} giờ trước: không tự tiếp tục, người quyết định.")
            continue
        # scripts/merge-if-green.sh reads the run table and the branch name with the same two patterns:
        # change all three together.
        tickets = set(re.findall(r"^\|\s*\d+\s*\|\s*(T-\d+)\s*\|", text, re.M))
        own = [branch] + [b for b in branches
                          if (m := re.match(r"^claude/(?:fix-)?(T-\d+)-", b)) and m.group(1) in tickets]
        dates = [d for d in (last_commit(b) for b in own) if d]
        latest = max(dates) if dates else started
        if latest and now - latest < timedelta(minutes=STALE_MINUTES):
            say(f"{run_id} đang chạy, commit gần nhất {int((now - latest).total_seconds() // 60)} phút trước.")
            continue
        if api("issues?state=open&labels=main-broken&per_page=10"):
            say(f"{run_id} đã dừng giữa chừng, nhưng nhánh chính đang đỏ (main-broken): không tiếp tục.")
            continue
        if api("issues?state=open&labels=env-broken&per_page=10"):
            say(f"{run_id} đã dừng giữa chừng, nhưng môi trường cloud đang lỗi (env-broken): không tiếp tục.")
            continue
        waiting = waiting_job()
        if waiting:
            job_name, actions_run = waiting
            where = f"; xem {RUNNER_DOC}" if RUNNER_DOC else ""
            say(f"Không tiếp tục: job {job_name} (lần chạy {actions_run}) đang chờ runner tự host (ADR-0042{where}).")
            continue
        since = f"{int((now - latest).total_seconds() // 60)} phút" if latest else "không rõ bao lâu"
        say(f"{run_id} đã dừng giữa chừng: không có commit nào trong {since}. Tiếp tục phiên.")
        print(run_file)
        return 0
    return 0


if __name__ == "__main__":
    REPO = repository()
    try:
        RUNNERS = runners_conf()
    except (OSError, UnicodeDecodeError, ValueError) as error:
        say(f"LỖI: {error}. Không tiếp tục phiên nào.")
        sys.exit(1)
    RUNNER_DOC = RUNNERS.get("RUNNER_DOC", "")
    try:
        sys.exit(main())
    except (LookupError, json.JSONDecodeError) as error:
        say(f"Không đọc được GitHub ({error}). Không tiếp tục phiên nào.")
        sys.exit(1)
