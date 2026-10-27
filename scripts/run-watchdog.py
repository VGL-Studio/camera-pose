#!/usr/bin/env python3
"""Tells people when an auto-run session has gone quiet, so someone can continue it (camera-pose L-084, T-068).

A session opened with `claude --cloud` is not continued by resume-run.yml (L-008): when it stops in the middle
(an API error, the quota, the process restarting), the run file stays "running" and nobody knows until they look.
RUN-20261026-03 waited 2 hours 30 minutes that way. .github/workflows/run-watchdog.yml runs this script every
30 minutes; it only opens, updates and closes issues labelled session-stalled, and never starts a session.

Usage: scripts/run-watchdog.py
A run is quiet when its run file on its report branch is "running" and none of its branches (the report branch,
claude/T-NNN-... and claude/fix-T-NNN-... of its tickets, as in scripts/resume-check.py) got a commit for
WATCHDOG_STALL_MINUTES (60). While a workflow run of the repository is queued or in progress and started after the
last commit on one of the session's branches (or on main, by push or by hand: device-session, staging), the
session may be waiting for it, so the issue waits until WATCHDOG_HARD_MINUTES (90). A quiet run gets one open
issue "session-stalled: RUN-..." whose body has the prompt to paste (docs/ops/staging-and-runs.md, 2.7); the body
is refreshed on later runs. The issue is closed when the run has a new commit or is no longer "running". A run file
left "running" on an old report branch keeps its issue open until a person sets it to "finished".

Exit 0 when the check ran, 1 when GitHub could not be read (any error but a 404 on a file, a branch or the
label): then no issue is changed. Prints what it did, and the issue body, on stdout.
Uses only `gh api` (REST); GITHUB_REPOSITORY or the origin remote gives the repository.
WATCHDOG_STALL_MINUTES, WATCHDOG_HARD_MINUTES and WATCHDOG_NOW (ISO time, tests) override the defaults.
Messages are in Vietnamese for the team; comments are in English.
"""

import json
import os
import re
import subprocess
import sys
import tempfile
from datetime import datetime, timedelta, timezone

STALL_MINUTES = int(os.environ.get("WATCHDOG_STALL_MINUTES", "60"))
HARD_MINUTES = int(os.environ.get("WATCHDOG_HARD_MINUTES", "90"))
LABEL = "session-stalled"
REPORT_BRANCH = re.compile(r"^claude/(RUN-\d{8}-\d{2})-report$")
TITLE = re.compile(r"^session-stalled: (RUN-\d{8}-\d{2})\b")


class NotFound(LookupError):
    """GitHub answered 404."""


def api(path, raw=False, method="GET", body=None):
    command = ["gh", "api", f"repos/{REPO}/{path}"]
    if method != "GET":
        command += ["-X", method]
    if raw:
        command += ["-H", "Accept: application/vnd.github.raw"]
    handle = None
    if body is not None:
        handle = tempfile.NamedTemporaryFile("w", suffix=".json", delete=False)
        json.dump(body, handle)
        handle.close()
        command += ["--input", handle.name]
    try:
        result = subprocess.run(command, capture_output=True, text=True)
    finally:
        if handle:
            os.unlink(handle.name)
    if result.returncode != 0:
        if "HTTP 404" in result.stderr:
            raise NotFound(f"{method} {path}")
        raise LookupError(f"{method} {path}: {result.stderr.strip()[:200]}")
    return result.stdout if raw else json.loads(result.stdout or "null")


def api_all(path):
    """Every item of a list endpoint, page by page (the path ends in "per_page=100")."""
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
    except NotFound:
        return None
    except (KeyError, TypeError):
        return None


def busy_workflows(since, own):
    """Names of workflow runs queued or in progress that the session may be waiting for: created after `since`
    (5 minutes of slack), on one of its branches `own` or on main by push or by hand; never this watchdog."""
    names = []
    for status in ("in_progress", "queued"):
        runs = (api(f"actions/runs?status={status}&per_page=100") or {}).get("workflow_runs", [])
        for run in runs:
            created = moment(run.get("created_at"))
            branch = run.get("head_branch")
            ours = branch in own or (branch == "main" and run.get("event") in ("push", "workflow_dispatch"))
            if created and created >= since - timedelta(minutes=5) and ours and run.get("name") != "run-watchdog":
                names.append(run.get("name") or "?")
    return sorted(set(names))


def body_for(run_id, run_file, latest, quiet, note):
    # The prompt of docs/ops/staging-and-runs.md, section 2.7, plus its "phiên tiếp tục" line ("Khi phiên bị ngắt").
    resume = (
        "Bạn đang chạy một phiên tự chạy của repo này, mở bằng claude --cloud, không qua routine hay start-run.yml. "
        "Đọc CLAUDE.md, docs/CLAUDE.md và docs/auto-run.md, rồi làm đúng quy trình trong docs/auto-run.md.\n\n"
        f"Danh sách chạy: {run_file}. Danh sách không có trạng thái approved thì không làm task nào, "
        "kết thúc phiên và báo lý do.\n\n"
        "Nhánh: không làm việc trên nhánh mặc định của phiên này. Tạo nhánh báo cáo và nhánh riêng cho từng task "
        "đúng như docs/auto-run.md. Làm nhánh báo cáo trong một worktree riêng "
        f"(git worktree add ../report claude/{run_id}-report), để không đổi nhánh dưới bản build của task.\n\n"
        "Ghi vào mục Kiểm tra đầu phiên: phiên mở bằng claude --cloud để dùng cloud session credits.\n\n"
        "Đây là phiên tiếp tục: nhánh báo cáo đã có, danh sách chạy đang running. "
        "Làm theo docs/auto-run.md, mục 4, Tiếp tục phiên sau khi hết hạn mức."
    )
    last = latest.strftime("%Y-%m-%d %H:%M UTC") if latest else "không rõ"
    lines = [
        f"Phiên `{run_id}` vẫn `running` trên nhánh `claude/{run_id}-report`, nhưng không có commit nào "
        f"trong {quiet} phút (commit gần nhất: {last}).",
        "",
        "Người làm: xem phiên còn chạy không (claude.ai/code hoặc terminal đã mở `claude --cloud`). "
        "Phiên mở bằng routine (`start-run.yml`) thì `resume-run.yml` tự tiếp tục sau 90 phút không có commit: "
        "không mở thêm phiên tay, kẻo hai phiên cùng chạy. Phiên `claude --cloud` đã dừng thì mở lại bằng "
        "`claude --cloud` và dán đoạn dưới (docs/ops/staging-and-runs.md, mục 2.7, Khi phiên bị ngắt); "
        "phiên mới đẩy nhánh báo cáo ngay ở bước đầu (giờ tiếp tục), nên `resume-run.yml` không kích hoạt thêm.",
        "",
        "```text",
        resume,
        "```",
        "",
        "Issue này tự đóng khi phiên có commit mới hoặc danh sách chạy không còn `running` "
        "(scripts/run-watchdog.py, T-068).",
    ]
    if note:
        lines += ["", note]
    return "\n".join(lines)


def ensure_label():
    try:
        api(f"labels/{LABEL}")
    except NotFound:
        try:
            api("labels", method="POST", body={"name": LABEL, "color": "d93f0b",
                                               "description": "Phiên tự chạy không có commit mới (T-068)"})
        except LookupError:
            pass  # created meanwhile, or no right to create: the issue is still opened with the name


def main():
    now = moment(os.environ["WATCHDOG_NOW"]) if os.environ.get("WATCHDOG_NOW") else datetime.now(timezone.utc)
    branches = [b["name"] for b in api_all("branches?per_page=100")]
    open_issues = {}
    for issue in api(f"issues?state=open&labels={LABEL}&per_page=100") or []:
        match = TITLE.match(issue.get("title", ""))
        if match and "pull_request" not in issue:
            open_issues[match.group(1)] = issue["number"]
    running = set()
    # Everything is read first and written after, so a read that fails changes no issue.
    writes = []
    for branch in sorted(branches):
        match = REPORT_BRANCH.match(branch)
        if not match:
            continue
        run_id = match.group(1)
        run_file = f"docs/runs/{run_id}.md"
        try:
            text = api(f"contents/{run_file}?ref={branch}", raw=True)
        except NotFound:
            continue
        fields = front_matter(text)
        if fields.get("status") != "running":
            continue
        running.add(run_id)
        tickets = set(re.findall(r"^\|\s*\d+\s*\|\s*(T-\d+)\s*\|", text, re.M))
        own = [branch] + [b for b in branches
                          if (m := re.match(r"^claude/(?:fix-)?(T-\d+)-", b)) and m.group(1) in tickets]
        dates = [d for d in (last_commit(b) for b in own) if d]
        latest = max(dates) if dates else moment(fields.get("started_at"))
        quiet = int((now - latest).total_seconds() // 60) if latest else HARD_MINUTES
        number = open_issues.get(run_id)
        if quiet < STALL_MINUTES:
            print(f"{run_id}: đang chạy, commit gần nhất {quiet} phút trước.")
            if number:
                writes.append(lambda n=number, q=quiet, r=run_id: close(n, f"Phiên `{r}` có commit lại ({q} phút trước). Đóng."))
            continue
        busy = busy_workflows(latest, set(own)) if latest else []
        if busy and quiet < HARD_MINUTES:
            print(f"{run_id}: {quiet} phút không có commit, nhưng đang có workflow chạy ({', '.join(busy)}): "
                  f"có thể phiên đang chờ; báo khi tới {HARD_MINUTES} phút.")
            continue
        note = ""
        started = moment(fields.get("started_at"))
        if started and now - started > timedelta(hours=24):
            note = "Phiên bắt đầu hơn 24 giờ trước: `resume-run.yml` không tự tiếp tục; người quyết định."
        body = body_for(run_id, run_file, latest, quiet, note)
        writes.append(lambda n=number, b=body, q=quiet, r=run_id: report(n, b, q, r))
    for run_id, number in sorted(open_issues.items()):
        if run_id not in running:
            writes.append(lambda n=number, r=run_id: close(n, f"Danh sách chạy `{r}` không còn `running`. Đóng."))
    try:
        for write in writes:
            write()
    except LookupError as error:
        print(f"Ghi issue lỗi ({error}). Lần chạy sau thử lại.", file=sys.stderr)
        return 1
    return 0


def report(number, body, quiet, run_id):
    if number:
        api(f"issues/{number}", method="PATCH", body={"body": body})
        print(f"{run_id}: {quiet} phút không có commit. Cập nhật issue #{number}.")
    else:
        ensure_label()
        api("issues", method="POST", body={"title": f"session-stalled: {run_id} không có commit {quiet} phút",
                                           "body": body, "labels": [LABEL]})
        print(f"{run_id}: {quiet} phút không có commit. Mở issue {LABEL}.")
    print(body)


def close(number, message):
    # Closed first: a close that fails leaves no comment behind, so a retry every 30 minutes adds none.
    api(f"issues/{number}", method="PATCH", body={"state": "closed"})
    api(f"issues/{number}/comments", method="POST", body={"body": message})
    print(f"Đóng issue #{number}: {message}")


if __name__ == "__main__":
    REPO = repository()
    try:
        sys.exit(main())
    except (LookupError, json.JSONDecodeError) as error:
        print(f"Không đọc được GitHub ({error}). Không đổi issue nào.", file=sys.stderr)
        sys.exit(1)
