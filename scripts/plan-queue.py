#!/usr/bin/env python3
"""Picks the tickets whose plan the session drafts at the end of a run (L-084).

Usage: scripts/plan-queue.py [--max N]      (N defaults to 2)

A ticket is picked when it is `backlog` or `ready`, has no `plan:`, is not marked `auto_run: false` (people do it), and
  - its spec (when it has one) exists, is confirmed and has no unanswered question; a ticket with scenarios needs a spec;
  - every `depends_on` ticket exists, is not stopped or cancelled, and is merged, has a plan,
    or was picked earlier in the same pass;
  - no remote branch `origin/claude/plan-T-NNN[-...]` and no open pull request already holds a plan for it.
Order: `sprint` (a number before a word before empty), then the ticket number.

stdout: at most N lines `T-NNN<TAB><ticket path>`. stderr: one line per ticket skipped, with the reason, then one line
`Đang chặn hàng chờ: T-NNN chặn K ticket (...): <reason>` per ticket that holds back others through `depends_on`
(directly or along a chain, also behind another root) while it waits for people: marked `auto_run: false`, spec not
confirmed, plan in review, not open and without a plan, stopped or cancelled. A dependency cycle (overlapping
cycles together) gets one line `Đang chặn hàng chờ: chu trình T-A, T-B chặn K ticket (...): <reason>`. Most blocked
tickets first. A dependency that is only beyond --max or does not exist is not named.
Exit 0 when the documents were read (also for an empty list); 2 when a document, a `git` or a `gh` call failed.
Reads every open pull request and its files (gh api, page by page). Writes no file.
The session runs `git fetch origin` before calling it.
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
# `backlog` is the status a new ticket gets; `ready` is optional, set by a dev who wants to mark it.
OPEN = {"backlog", "ready"}


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
        if not re.fullmatch(r"T-\d+", fields.get("id", "")) or not fields.get("status"):
            fail(f"{path.relative_to(ROOT)}: không đọc được khối frontmatter có id dạng T-NNN và status.")
        fields["path"] = path
        tickets[fields["id"]] = fields
    return tickets


def api_all(path):
    """Every item of a list endpoint. Pages are read with "&page=N" on the repos/<owner>/<repo> path: `gh api
    --paginate` follows GitHub's Link header, which names the repository by numeric id, and the proxy of a cloud
    session refuses such paths (L-092). The path must already end in "per_page=100"."""
    items = []
    for page in range(1, 51):
        batch = json.loads(run(["gh", "api", f"{path}&page={page}"]) or "[]")
        items.extend(batch)
        if len(batch) < 100:
            break
    return items


def plan_branches():
    out = run(["git", "-C", str(ROOT), "for-each-ref", "--format=%(refname:short)", "refs/remotes/origin/claude/"])
    return out.split()


def open_pr_files():
    url = run(["git", "-C", str(ROOT), "config", "--get", "remote.origin.url"]).strip()
    # https (with or without credentials) or ssh; only owner/repo is kept, so credentials never reach a message.
    match = re.match(r"^(?:https?://(?:[^@/\s]+@)?|ssh://git@|git@)github\.com[:/]([\w.-]+)/([\w.-]+?)(?:\.git)?/?$", url)
    if not match:
        fail("không xác định được repo GitHub từ remote.origin.url.")
    repo = f"{match.group(1)}/{match.group(2)}"
    files = {}
    for pull in api_all(f"repos/{repo}/pulls?state=open&per_page=100"):
        number = pull["number"]
        files[int(number)] = [f["filename"] for f in api_all(f"repos/{repo}/pulls/{number}/files?per_page=100")]
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
        # A dead ticket blocks even when it has a plan: that plan will not be carried out.
        if other["status"] in DEAD:
            return f"phụ thuộc {dep} đang {other['status']}"
        if other["status"] in MERGED or other.get("plan"):
            continue
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


def manual(ticket):
    """A ticket that people do by hand (for example scripts/release.sh) gets no plan from a session."""
    if ticket.get("auto_run", "").lower() == "false":
        return "ticket ghi auto_run: false (người làm, không soạn plan cho phiên tự chạy)"
    return ""


def blocking_dependencies(ticket, tickets, chosen):
    """The dependencies that keep a ticket out of the queue (the same test as dependency_problem, every one of them)."""
    found = []
    for dep in run_check.ids(ticket.get("depends_on", "")):
        other = tickets.get(dep)
        if dep in chosen or other is None:
            continue
        if other["status"] in DEAD or not (other["status"] in MERGED or other.get("plan")):
            found.append(dep)
    return found


def cycles(skipped, tickets, chosen):
    """Maps each skipped ticket that lies on a dependency cycle to the sorted ids of its cycle: a strongly connected
    component (Tarjan) of the blocking dependencies between skipped tickets with more than one ticket, or a ticket
    that depends on itself. Overlapping cycles form one component."""
    number = lambda t: int(t.split("-")[1])
    edges = {t: [d for d in blocking_dependencies(tickets[t], tickets, chosen) if d in skipped] for t in skipped}
    index, low, stack, on_stack, found = {}, {}, [], set(), {}

    def visit(node):
        index[node] = low[node] = len(index)
        stack.append(node)
        on_stack.add(node)
        for dep in edges[node]:
            if dep not in index:
                visit(dep)
                low[node] = min(low[node], low[dep])
            elif dep in on_stack:
                low[node] = min(low[node], index[dep])
        if low[node] == index[node]:
            component = []
            while True:
                member = stack.pop()
                on_stack.discard(member)
                component.append(member)
                if member == node:
                    break
            if len(component) > 1 or node in edges[node]:
                ordered = tuple(sorted(component, key=number))
                for member in component:
                    found[member] = ordered

    for node in edges:
        if node not in index:
            visit(node)
    return found


def blockers(skipped, tickets, chosen):
    """Lists each root that waits for people with the sorted tickets it holds back and its reason.

    A root is a dependency that is itself skipped for a reason of its own, or that is not a candidate at all (not
    open, or stopped or cancelled), or a dependency cycle (see cycles). The walk goes on below a root, so a deeper
    root is named too, with every ticket behind it, including the roots in between. A dependency that only waits
    behind another one passes the tickets on. An eligible ticket left out by --max is not a root: nobody needs to act
    on it."""
    number = lambda t: int(t.split("-")[1])
    on_cycle = cycles(skipped, tickets, chosen)
    roots = {}

    def root_reason(dep):
        other = tickets[dep]
        if other["status"] in DEAD:
            return f"đang {other['status']}"
        if dep in skipped and not skipped[dep][1]:
            return skipped[dep][0]
        if other["status"] not in OPEN:
            return f"đang {other['status']}, chưa có plan"
        return ""

    def walk(ticket_id, seen):
        for dep in blocking_dependencies(tickets[ticket_id], tickets, chosen):
            if dep in seen:
                continue
            seen.add(dep)
            reason = root_reason(dep)
            if reason:
                yield dep, number(dep), reason
            if dep in on_cycle:
                members = on_cycle[dep]
                label = "chu trình " + ", ".join(members)
                yield label, number(members[0]), "các ticket phụ thuộc vòng tròn, sửa depends_on"
            if dep in skipped:
                yield from walk(dep, seen)

    for ticket_id in skipped:
        for label, order, reason in walk(ticket_id, set()):
            roots.setdefault(label, (order, reason, set()))[2].add(ticket_id)
    return sorted(
        ((label, reason, sorted(held, key=number)) for label, (_, reason, held) in roots.items()),
        key=lambda item: (-len(item[2]), roots[item[0]][0], item[0]),
    )


def main(argv):
    limit = 2
    if argv:
        if len(argv) != 2 or argv[0] != "--max" or not argv[1].isdigit():
            print("Cách dùng: scripts/plan-queue.py [--max N]", file=sys.stderr)
            return 2
        limit = int(argv[1])
    tickets = read_tickets(run_check)
    branches = plan_branches()
    pr_files = open_pr_files()
    candidates = [t for t in tickets.values() if t["status"] in OPEN and not t.get("plan")]
    candidates.sort(key=lambda t: (sprint_key(t.get("sprint", "")), int(t["id"].split("-")[1])))
    chosen = []
    # ticket id -> (reason, True when the reason is only a dependency)
    skipped = {}
    for ticket in candidates:
        own = manual(ticket) or holder(ticket["id"], branches, pr_files) or spec_problem(ticket)
        reason = own or dependency_problem(ticket, tickets, chosen)
        if reason:
            skipped[ticket["id"]] = (reason, not own)
            print(f"{ticket['id']}: bỏ qua, {reason}", file=sys.stderr)
        elif len(chosen) < limit:
            chosen.append(ticket["id"])
            print(f"{ticket['id']}\t{ticket['path'].relative_to(ROOT)}")
    for dep, reason, held in blockers(skipped, tickets, chosen):
        print(f"Đang chặn hàng chờ: {dep} chặn {len(held)} ticket ({', '.join(held)}): {reason}", file=sys.stderr)
    return 0


run_check = load_run_check()

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
