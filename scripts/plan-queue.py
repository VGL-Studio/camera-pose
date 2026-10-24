#!/usr/bin/env python3
"""Picks the tickets whose plan the session drafts at the end of a run (plan T-045, L-061).

Usage: scripts/plan-queue.py [--max N]      (N defaults to 3)

A ticket is picked when it is `ready`, has no `plan:`, and
  - its spec (when it has one) exists, is confirmed and has no unanswered question; a ticket with scenarios needs a spec;
  - every `depends_on` ticket is merged, has a plan, or was picked earlier in the same pass;
  - no remote branch `origin/claude/plan-T-NNN[-...]` and no open pull request already holds a plan for it.
Order: `sprint` (a number before a word before empty), then the ticket number.

stdout: at most N lines `T-NNN<TAB><ticket path>`. stderr: one line per ticket skipped, with the reason.
Exit 0 when the documents were read (also for an empty list); 2 when a document, a `git` or a `gh` call failed.
Writes no file. The session runs `git fetch origin` before calling it.
PLAN_QUEUE_ROOT overrides the repository root (tests). Messages are in Vietnamese for the team.
"""

import importlib.util
import json
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(os.environ.get("PLAN_QUEUE_ROOT", Path(__file__).resolve().parent.parent))
DOCS = ROOT / "docs"
MERGED = {"on-staging", "accepted", "released"}
DEAD = {"stopped", "cancelled"}


def load_run_check():
    # run-check.py reads the same document tree as this script: it takes its root from RUN_CHECK_ROOT.
    os.environ["RUN_CHECK_ROOT"] = str(ROOT)
    spec = importlib.util.spec_from_file_location("run_check", Path(__file__).resolve().parent / "run-check.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def fail(message):
    print(f"LỖI: {message}", file=sys.stderr)
    sys.exit(2)


def run(command):
    try:
        done = subprocess.run(command, capture_output=True, text=True, check=True)
    except (OSError, subprocess.CalledProcessError) as error:
        detail = getattr(error, "stderr", "") or str(error)
        fail(f"lệnh '{' '.join(command)}' không chạy được: {detail.strip()}")
    return done.stdout


def sprint_key(value):
    if value.isdigit():
        return (0, int(value), "")
    return (1, 0, value) if value else (2, 0, "")


def read_tickets(run_check):
    tickets = {}
    for path in sorted(DOCS.glob("features/*/tickets/T-*.md")):
        fields = run_check.front_matter(path.read_text())
        if not fields.get("id") or not fields.get("status"):
            fail(f"{path.relative_to(ROOT)}: không đọc được khối frontmatter có id và status.")
        fields["path"] = path
        tickets[fields["id"]] = fields
    return tickets


def plan_branches():
    out = run(["git", "-C", str(ROOT), "for-each-ref", "--format=%(refname:short)", "refs/remotes/origin/claude/"])
    return out.split()


def open_pr_files():
    url = run(["git", "-C", str(ROOT), "config", "--get", "remote.origin.url"]).strip()
    repo = re.sub(r"\.git$", "", re.sub(r"^(git@github\.com:|https://github\.com/)", "", url))
    if not re.fullmatch(r"[^/]+/[^/]+", repo):
        fail(f"không xác định được repo từ remote: {url}")
    pulls = json.loads(run(["gh", "api", f"repos/{repo}/pulls?state=open&per_page=100"]) or "[]")
    files = {}
    for pull in pulls:
        number = pull["number"]
        names = run(["gh", "api", f"repos/{repo}/pulls/{number}/files?per_page=100", "--jq", ".[].filename"])
        files[number] = names.split()
    return files


def holder(ticket_id, branches, pr_files):
    for branch in branches:
        if branch == f"origin/claude/plan-{ticket_id}" or branch.startswith(f"origin/claude/plan-{ticket_id}-"):
            return f"đang có plan trên nhánh {branch}"
    for number, names in pr_files.items():
        for name in names:
            if name.startswith("docs/features/") and Path(name).name.startswith(f"{ticket_id}-"):
                return f"đang có PR mở #{number} sửa ticket"
    return ""


def dependency_problem(ticket, tickets, chosen):
    for dep in run_check.ids(ticket.get("depends_on", "")):
        if dep in chosen:
            continue
        other = tickets.get(dep)
        if other is None:
            return f"phụ thuộc {dep} không có ticket"
        if other["status"] in MERGED or other.get("plan"):
            continue
        if other["status"] in DEAD:
            return f"phụ thuộc {dep} đang {other['status']}"
        return f"chờ {dep} ({other['status']}, chưa có plan)"
    return ""


def spec_problem(ticket):
    spec = ticket.get("spec", "")
    if not spec:
        return "có kịch bản mà không có spec" if run_check.ids(ticket.get("scenarios", "")) else ""
    spec_path = DOCS / spec
    if not spec_path.is_file():
        return f"spec {spec} không có"
    errors, _ = run_check.spec_problems(spec_path, {})
    return "; ".join(errors)


def main(argv):
    limit = 3
    if argv:
        if len(argv) != 2 or argv[0] != "--max" or not argv[1].isdigit():
            print("Cách dùng: scripts/plan-queue.py [--max N]", file=sys.stderr)
            return 2
        limit = int(argv[1])
    tickets = read_tickets(run_check)
    branches = plan_branches()
    pr_files = open_pr_files()
    candidates = [t for t in tickets.values() if t["status"] == "ready" and not t.get("plan")]
    candidates.sort(key=lambda t: (sprint_key(t.get("sprint", "")), int(t["id"].split("-")[1])))
    chosen = []
    for ticket in candidates:
        reason = (
            holder(ticket["id"], branches, pr_files)
            or spec_problem(ticket)
            or dependency_problem(ticket, tickets, chosen)
        )
        if reason:
            print(f"{ticket['id']}: bỏ qua, {reason}", file=sys.stderr)
        elif len(chosen) < limit:
            chosen.append(ticket["id"])
            print(f"{ticket['id']}\t{ticket['path'].relative_to(ROOT)}")
    return 0


run_check = load_run_check()

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
