#!/usr/bin/env python3
"""Finds an auto-run session that stopped in the middle, usually because the usage quota ran out, so that
.github/workflows/resume-run.yml can start it again once the quota is renewed (docs/auto-run.md, section 4).

Usage: scripts/resume-check.py
Prints the run file to continue (docs/runs/RUN-YYYYMMDD-NN.md) on stdout, or nothing. Explains on stderr.
Exit 0 when the check ran (with or without a session to continue), 1 when GitHub could not be read.

A session has stopped when its run file on its report branch is still "running" and none of its branches
(the report branch, claude/T-NNN-... and claude/fix-T-NNN-... of the tickets in its run list) got a commit
for STALE_MINUTES. A session that is alive pushes at every step (docs/auto-run.md, section 3), and its longest
wait (CI, emulator) is shorter than that. It is not continued when it started more than MAX_HOURS ago: people
decide then. Nor while main is red (main-broken) or the cloud environment is broken (env-broken).

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


def main():
    now = moment(os.environ["RESUME_NOW"]) if os.environ.get("RESUME_NOW") else datetime.now(timezone.utc)
    branches = [b["name"] for b in api("branches?per_page=100")]
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
        since = f"{int((now - latest).total_seconds() // 60)} phút" if latest else "không rõ bao lâu"
        say(f"{run_id} đã dừng giữa chừng: không có commit nào trong {since}. Tiếp tục phiên.")
        print(run_file)
        return 0
    return 0


if __name__ == "__main__":
    REPO = repository()
    try:
        sys.exit(main())
    except (LookupError, json.JSONDecodeError) as error:
        say(f"Không đọc được GitHub ({error}). Không tiếp tục phiên nào.")
        sys.exit(1)
