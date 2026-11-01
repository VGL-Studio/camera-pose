#!/usr/bin/env python3
"""Turns the schedules of resume-run.yml and run-watchdog.yml off while no auto-run session needs them, and back on
when one may (T-051, L-126). A disabled workflow gets no runner: neither its schedule nor workflow_dispatch runs.

Usage:
  scripts/schedule-idle.py --disable-if-idle <resume-run.yml|run-watchdog.yml>
      Run by the idle job of that workflow. Disables it when no session is running, no approved run list waits to
      start, it was not enabled in the last GRACE_HOURS and start-run.yml did not run in the last GRACE_HOURS. Reads
      again after disabling and enables it again when a session or a run list showed up meanwhile, or when that read
      fails. Always exits 0 (2 for a wrong argument): the workflow never turns red because of this script.
  scripts/schedule-idle.py --enable
      Enables both and reads their state back. Run by the session at its start (docs/auto-run.md, section 2) and by
      a developer after merging a run list. Exit 0 when both are active, 1 otherwise.
  scripts/schedule-idle.py --enable-if-pending
      Run by status-page.yml after each push of documents to main. Enables both when a session is running, an
      approved run list waits, or GitHub cannot be read. Always exits 0, with a ::warning:: when an enable fails.

Busy means: a report branch claude/RUN-YYYYMMDD-NN-report whose run file is "running"; or a run list
docs/runs/RUN-YYYYMMDD-NN.md on main, dated in the last LIST_DAYS days, "approved", without a report branch, whose
last commit on main is younger than PENDING_HOURS (Run now, a timer or `claude --cloud` may start it hours later).

Uses only `gh api` (REST); GITHUB_REPOSITORY or the origin remote gives the repository.
SCHEDULE_IDLE_NOW (ISO time), SCHEDULE_IDLE_GRACE_HOURS and SCHEDULE_IDLE_PENDING_HOURS override the defaults (tests).
Messages are in Vietnamese for the team.
"""

import json
import os
import re
import subprocess
import sys
from datetime import datetime, timedelta, timezone

SCHEDULES = ("resume-run.yml", "run-watchdog.yml")
GRACE_HOURS = float(os.environ.get("SCHEDULE_IDLE_GRACE_HOURS", "3"))
PENDING_HOURS = float(os.environ.get("SCHEDULE_IDLE_PENDING_HOURS", "72"))
LIST_DAYS = 14
REPORT_BRANCH = re.compile(r"^claude/(RUN-\d{8}-\d{2})-report$")
RUN_LIST = re.compile(r"^(RUN-(\d{8})-\d{2})\.md$")


class NotFound(LookupError):
    pass


def api(path, raw=False):
    command = ["gh", "api", f"repos/{REPO}/{path}"]
    if raw:
        command += ["-H", "Accept: application/vnd.github.raw"]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode != 0:
        raise (NotFound if "404" in result.stderr else LookupError)(path)
    return result.stdout if raw else json.loads(result.stdout or "null")


def api_all(path):
    """Every item of a list endpoint, page by page (L-092). The path must already end in "per_page=100"."""
    items = []
    for page in range(1, 51):
        batch = api(f"{path}&page={page}") or []
        items.extend(batch)
        if len(batch) < 100:
            break
    return items


def put(path):
    return subprocess.run(["gh", "api", "-X", "PUT", f"repos/{REPO}/{path}"], capture_output=True, text=True)


def repository():
    if os.environ.get("GITHUB_REPOSITORY"):
        return os.environ["GITHUB_REPOSITORY"]
    url = subprocess.run(["git", "config", "--get", "remote.origin.url"], capture_output=True, text=True).stdout
    return re.sub(r"^(git@github\.com:|https://github\.com/)|\.git\s*$", "", url.strip())


def moment(text):
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


def status_of(path, ref):
    """The status of a run file, or None when the file is not there. Any other read error raises: busy."""
    try:
        return front_matter(api(f"contents/{path}?ref={ref}", raw=True)).get("status")
    except NotFound:
        return None


def busy(now):
    """Why the schedules are still needed, or None."""
    reports = {}
    for branch in sorted(b["name"] for b in api_all("branches?per_page=100")):
        match = REPORT_BRANCH.match(branch)
        if match:
            reports[match.group(1)] = branch
    for run_id, branch in reports.items():
        if status_of(f"docs/runs/{run_id}.md", branch) == "running":
            return f"{run_id} đang running"
    try:
        listing = api("contents/docs/runs?ref=main") or []
    except NotFound:
        listing = []
    for item in sorted(listing, key=lambda entry: entry["name"]):
        match = RUN_LIST.match(item["name"])
        if not match or match.group(1) in reports:
            continue
        dated = datetime.strptime(match.group(2), "%Y%m%d").replace(tzinfo=timezone.utc)
        if now - dated > timedelta(days=LIST_DAYS):
            continue
        path = f"docs/runs/{item['name']}"
        if status_of(path, "main") != "approved":
            continue
        commits = api(f"commits?path={path}&sha=main&per_page=1")
        merged = moment(commits[0]["commit"]["committer"]["date"]) if commits else None
        if merged and now - merged < timedelta(hours=PENDING_HOURS):
            return f"{match.group(1)} đã duyệt, chưa bắt đầu (merge {hours(now - merged)} giờ trước)"
    return None


def hours(delta):
    return f"{delta.total_seconds() / 3600:.1f}"


def recent(now, text):
    when = moment(text)
    return when is not None and now - when < timedelta(hours=GRACE_HOURS)


def disable_if_idle(name, now):
    try:
        reason = busy(now)
        if reason:
            print(f"{name}: giữ lịch: {reason}.")
            return 0
        updated = api(f"actions/workflows/{name}").get("updated_at")
        if recent(now, updated):
            print(f"{name}: giữ lịch: vừa được bật lúc {updated}.")
            return 0
        try:
            runs = api("actions/workflows/start-run.yml/runs?per_page=1").get("workflow_runs") or []
        except NotFound:
            runs = []
        if runs and recent(now, runs[0].get("created_at")):
            print(f"{name}: giữ lịch: start-run vừa chạy lúc {runs[0]['created_at']}.")
            return 0
    except Exception as error:  # noqa: BLE001 - any failure to read keeps the schedule on
        print(f"{name}: Không đọc được GitHub ({error!r}). Không tắt lịch.")
        return 0
    result = put(f"actions/workflows/{name}/disable")
    if result.returncode != 0:
        print(f"{name}: CHÚ Ý: không tắt được lịch ({result.stderr.strip()}). Lịch vẫn chạy như cũ.")
        return 0
    try:
        reason = busy(now)
    except Exception as error:  # noqa: BLE001
        reason = f"không đọc lại được GitHub ({error!r})"
    if not reason:
        print(f"{name}: đã tắt lịch: không có phiên running hay danh sách chờ. Phiên hay start-run sẽ bật lại.")
        return 0
    result = put(f"actions/workflows/{name}/enable")
    if result.returncode != 0:
        print(f"{name}: CHÚ Ý: đã tắt nhưng không bật lại được ({result.stderr.strip()}), trong khi {reason}. "
              f"Người bật: scripts/schedule-idle.py --enable.")
        return 0
    print(f"{name}: bật lại lịch: {reason}.")
    return 0


def enable():
    """Enables both schedules; returns the names that are not active afterwards."""
    failed = []
    for name in SCHEDULES:
        result = put(f"actions/workflows/{name}/enable")
        if result.returncode != 0:
            print(f"{name}: không bật được ({result.stderr.strip()}).")
            failed.append(name)
            continue
        try:
            state = api(f"actions/workflows/{name}").get("state")
        except Exception as error:  # noqa: BLE001
            state = f"không đọc được ({error!r})"
        if state == "active":
            print(f"{name}: lịch đang bật (active).")
        else:
            print(f"{name}: đã gọi bật nhưng trạng thái là {state}.")
            failed.append(name)
    return failed


def main(argv):
    now = moment(os.environ["SCHEDULE_IDLE_NOW"]) if os.environ.get("SCHEDULE_IDLE_NOW") else datetime.now(timezone.utc)
    if len(argv) == 2 and argv[0] == "--disable-if-idle" and argv[1] in SCHEDULES:
        return disable_if_idle(argv[1], now)
    if argv == ["--enable"]:
        failed = enable()
        if failed:
            print(f"Chưa bật được lịch: {', '.join(failed)}. Bật tay: Actions > <workflow> > Enable workflow.")
        return 1 if failed else 0
    if argv == ["--enable-if-pending"]:
        try:
            reason = busy(now)
        except Exception as error:  # noqa: BLE001 - when in doubt, turn the schedules on
            reason = f"không đọc được GitHub ({error!r}), bật cho chắc"
        if not reason:
            print("Không có phiên running hay danh sách chờ: không đổi lịch.")
            return 0
        print(f"Bật lịch: {reason}.")
        for name in enable():
            print(f"::warning::Không bật được lịch {name}. Bật tay trước khi bắt đầu phiên.")
        return 0
    print(__doc__, file=sys.stderr)
    return 2


if __name__ == "__main__":
    REPO = repository()
    sys.exit(main(sys.argv[1:]))
