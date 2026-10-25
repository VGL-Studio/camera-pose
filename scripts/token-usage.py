#!/usr/bin/env python3
"""Counts the AI tokens an auto-run session used, per task, from the Claude Code transcripts (docs/ops/token-usage.md).

Usage:
  scripts/token-usage.py report [--since ISO] [--until ISO] [--run RUN-ID] [--csv FILE] [--transcripts FILE ...]
  scripts/token-usage.py estimate [--csv FILE]
  scripts/token-usage.py credit --run RUN-ID --usd AMOUNT [--csv FILE]

report: reads every transcript (subagents included) of this repository, keeps one usage per API request,
  attributes it to a task by the git branch the request was made on (claude/T-NNN-..., claude/fix-T-NNN-...)
  or, for a subagent transcript, by a `Task: T-NNN` line of its first user message, and prints a Markdown table with the plan's estimate next to the actual count. With --run and --csv it also
  writes the rows of that run into the CSV, replacing rows of the same run so it can run again.
estimate: median total per ticket size from the CSV, for writing token_estimate in a new plan, and the cloud
  session credit spent per million tokens over the runs that recorded it.
credit: records the cloud session credit (US dollars) a run opened with `claude --cloud` spent, read by a person
  from the balance before and after the run. It goes on the shared row of the run, once per run.

A transcript belongs to this repository when its working directory is the repository, any of its git worktrees
(the report branch is often checked out in one, L-008 of camera-pose), or a folder named like the repository
(the origin remote's name, else the folder name; TOKEN_USAGE_REPO overrides it).

Tokens are counted as they are, never converted to a price. The script never fails a session: it exits 0 and
says what it could not measure. TOKEN_USAGE_PROJECTS_DIR overrides ~/.claude/projects and TOKEN_USAGE_ROOT
the repository root (tests). Messages are in Vietnamese for the team.
"""

import argparse
import csv
import json
import os
import re
import statistics
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(os.environ.get("TOKEN_USAGE_ROOT", Path(__file__).resolve().parent.parent))
PROJECTS_DIR = Path(os.environ.get("TOKEN_USAGE_PROJECTS_DIR", Path.home() / ".claude" / "projects"))
DEFAULT_CSV = "docs/metrics/token-usage.csv"
# credit_usd came later; older CSV files without it are read and rewritten with an empty value.
FIELDS = ["run", "date", "task", "size", "estimate", "requests", "input", "cache_write", "cache_read", "output",
          "total", "credit_usd"]
COUNTS = ["input", "cache_write", "cache_read", "output"]
SHARED = "chung"
# Starting estimates per ticket size, used until the CSV has enough tasks of that size (docs/ops/token-usage.md).
# Lowered from 10M/20M/40M after the first runs of camera-pose: medians S 5M, M 15M, L 11M (L-021).
DEFAULT_ESTIMATES = {"S": 5_000_000, "M": 15_000_000, "L": 30_000_000}
MIN_SAMPLES = 5
SIZE_ORDER = {"S": 0, "M": 1, "L": 2}
TASK_BRANCH = re.compile(r"^claude/(?:fix-)?(T-\d+)(?:-|$)")
# A subagent works in a worktree by path, so its transcript keeps the main checkout's branch. The main session
# writes a line "Task: T-NNN" in its brief, and every request of that subagent transcript counts for the task (L-073).
BRIEF_TASK = re.compile(r"^Task: (T-\d+)\s*$", re.M)


def parse_amount(text):
    """'20M', '1.5M', '800k', '1200000' -> int; None when blank or unreadable."""
    match = re.fullmatch(r"\s*([\d.]+)\s*([kKmM]?)\s*", text or "")
    if not match:
        return None
    try:
        value = float(match.group(1))
    except ValueError:
        return None
    return int(value * {"": 1, "k": 1_000, "m": 1_000_000}[match.group(2).lower()])


def short(number):
    if number is None:
        return "-"
    if number >= 1_000_000:
        return f"{number / 1_000_000:.1f}M"
    if number >= 1_000:
        return f"{number / 1_000:.0f}k"
    return str(number)


def front_matter(path):
    match = re.match(r"^---\n(.*?)\n---\n", path.read_text(), re.S)
    fields = {}
    for line in (match.group(1).splitlines() if match else []):
        if ":" in line:
            key, value = line.split(":", 1)
            fields[key.strip()] = value.split("#", 1)[0].strip()
    return fields


def plan_info(task):
    """(size, estimate) of a task from its ticket and its plan."""
    size = estimate = None
    for ticket in ROOT.glob(f"docs/features/*/tickets/{task}-*.md"):
        size = front_matter(ticket).get("size") or None
    for plan in ROOT.glob(f"docs/features/*/plans/{task}-*.md"):
        estimate = parse_amount(front_matter(plan).get("token_estimate", ""))
    return size, estimate


def timestamp(text):
    """ISO time, or the started_at form of a run file: '2026-10-17 16:19 UTC'."""
    try:
        moment = datetime.fromisoformat(re.sub(r"\s*(UTC|Z)$", "+00:00", text.strip()))
    except (AttributeError, ValueError):
        return None
    return moment if moment.tzinfo else moment.replace(tzinfo=timezone.utc)


def transcripts():
    if not PROJECTS_DIR.is_dir():
        return []
    return sorted(PROJECTS_DIR.rglob("*.jsonl"))


def git(*args):
    try:
        result = subprocess.run(["git", "-C", str(ROOT), *args], capture_output=True, text=True, timeout=10)
    except (OSError, subprocess.SubprocessError):
        return ""
    return result.stdout.strip() if result.returncode == 0 else ""


def repo_roots():
    """The repository folder and every worktree of it, as git lists them."""
    roots = {ROOT.as_posix(), ROOT.resolve().as_posix()}
    for line in git("worktree", "list", "--porcelain").splitlines():
        if line.startswith("worktree "):
            path = line[len("worktree "):]
            roots |= {path, os.path.realpath(path)}
    return roots


def repo_name():
    if os.environ.get("TOKEN_USAGE_REPO"):
        return os.environ["TOKEN_USAGE_REPO"]
    url = git("remote", "get-url", "origin")
    name = re.sub(r"\.git$", "", url.rstrip("/").rsplit("/", 1)[-1].rsplit(":", 1)[-1]) if url else ""
    return name or ROOT.resolve().name


def repo_matcher():
    roots, name = repo_roots(), repo_name()

    def in_repo(cwd):
        if not cwd:
            return False
        paths = {cwd, os.path.realpath(cwd)}
        if any(path == root or path.startswith(root + "/") for root in roots for path in paths):
            return True
        # Another checkout of the same repository, such as the main checkout seen from a worktree.
        return any(name in Path(path).parts for path in paths)

    return in_repo


def message_text(message):
    """The text of a transcript message: content is a string or a list of parts."""
    content = (message or {}).get("content") if isinstance(message, dict) else None
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        return "\n".join(part.get("text", "") for part in content if isinstance(part, dict))
    return ""


def requests(files, since, until):
    """One usage per API request: the last line of a request carries its final counts."""
    found = {}
    in_repo = repo_matcher()
    for path in files:
        try:
            lines = path.open(encoding="utf-8", errors="replace")
        except OSError:
            continue
        brief_task, first_user = None, "subagents" in path.parts
        with lines:
            for line in lines:
                if first_user and '"user"' in line:
                    try:
                        entry = json.loads(line)
                    except json.JSONDecodeError:
                        entry = {}
                    if entry.get("type") == "user":
                        first_user = False
                        match = BRIEF_TASK.search(message_text(entry.get("message")))
                        brief_task = f"claude/{match.group(1)}" if match else None
                if '"usage"' not in line:
                    continue
                try:
                    entry = json.loads(line)
                except json.JSONDecodeError:
                    continue
                message = entry.get("message") or {}
                usage = message.get("usage")
                if entry.get("type") != "assistant" or not usage or not in_repo(entry.get("cwd")):
                    continue
                moment = timestamp(entry.get("timestamp"))
                if moment is None or (since and moment < since) or (until and moment > until):
                    continue
                key = entry.get("requestId") or entry.get("uuid")
                found[key] = {
                    "branch": brief_task or entry.get("gitBranch") or "",
                    "input": usage.get("input_tokens") or 0,
                    "cache_write": usage.get("cache_creation_input_tokens") or 0,
                    "cache_read": usage.get("cache_read_input_tokens") or 0,
                    "output": usage.get("output_tokens") or 0,
                }
    return found.values()


def group(found):
    rows = {}
    for request in found:
        match = TASK_BRANCH.match(request["branch"])
        task = match.group(1) if match else SHARED
        row = rows.setdefault(task, {"task": task, "requests": 0, **{name: 0 for name in COUNTS}})
        row["requests"] += 1
        for name in COUNTS:
            row[name] += request[name]
    for row in rows.values():
        row["total"] = sum(row[name] for name in COUNTS)
        row["size"], row["estimate"] = plan_info(row["task"]) if row["task"] != SHARED else (None, None)
    tasks = sorted((r for r in rows.values() if r["task"] != SHARED), key=lambda r: int(r["task"][2:]))
    return tasks + [rows[SHARED]] if SHARED in rows else tasks


def ratio(row):
    if not row["estimate"]:
        return "-"
    return f"{row['total'] / row['estimate']:.1f}x"


def table(rows):
    lines = ["| Task | Cỡ | Ước tính | Thực tế (tổng) | So với ước tính | Request | Input | Ghi cache | Đọc cache | Output |",
             "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |"]
    for row in rows:
        name = "Chung (ngoài task)" if row["task"] == SHARED else row["task"]
        lines.append(f"| {name} | {row['size'] or '-'} | {short(row['estimate'])} | {short(row['total'])} | "
                     f"{ratio(row)} | {row['requests']} | {short(row['input'])} | {short(row['cache_write'])} | "
                     f"{short(row['cache_read'])} | {short(row['output'])} |")
    total = {name: sum(r[name] for r in rows) for name in COUNTS + ["requests", "total"]}
    lines.append(f"| **Cả phiên** | | | **{short(total['total'])}** | | {total['requests']} | "
                 f"{short(total['input'])} | {short(total['cache_write'])} | {short(total['cache_read'])} | "
                 f"{short(total['output'])} |")
    return "\n".join(lines)


def read_csv(path):
    if not path.is_file():
        return []
    with path.open(newline="") as handle:
        return list(csv.DictReader(handle))


def save_csv(path, rows):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDS, lineterminator="\n", restval="", extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def write_csv(path, run, rows, date):
    old = read_csv(path)
    # A credit recorded before the run was measured again is kept.
    credit = next((r.get("credit_usd") for r in old if r.get("run") == run and r.get("credit_usd")), "")
    kept = [r for r in old if r.get("run") != run]
    for row in rows:
        kept.append({"run": run, "date": date, "task": row["task"], "size": row["size"] or "",
                     "estimate": row["estimate"] or "", "requests": row["requests"],
                     **{name: row[name] for name in COUNTS}, "total": row["total"],
                     "credit_usd": credit if row["task"] == SHARED else ""})
    save_csv(path, kept)


def report(args):
    since, until = timestamp(args.since) if args.since else None, timestamp(args.until) if args.until else None
    if (args.since and not since) or (args.until and not until):
        print("Không đo được: --since hoặc --until sai dạng; dùng 2026-10-18T02:00:00Z hoặc 2026-10-18 02:00 UTC.")
        return 0
    files = [Path(f) for f in args.transcripts] if args.transcripts else transcripts()
    rows = group(requests(files, since, until))
    if not rows:
        print(f"Không đo được: không có transcript nào của repo này trong {PROJECTS_DIR}"
              f"{' từ ' + args.since if args.since else ''}. Ghi \"không đo được\" vào báo cáo.")
        return 0
    print(table(rows))
    if args.run and args.csv:
        write_csv(ROOT / args.csv, args.run, rows, (since or datetime.now(timezone.utc)).date().isoformat())
        print(f"\nĐã ghi {len(rows)} dòng của {args.run} vào {args.csv}.")
    return 0


def credit(args):
    try:
        usd = float(args.usd)
    except ValueError:
        usd = -1
    if usd < 0:
        print(f"LỖI: --usd phải là số đô la không âm, ví dụ 10 hoặc 9.5; nhận được: {args.usd}")
        return 2
    path = ROOT / args.csv
    rows = read_csv(path)
    if not any(r.get("run") == args.run for r in rows):
        print(f"LỖI: {args.csv} chưa có dòng nào của {args.run}. Chạy report --run {args.run} --csv trước.")
        return 1
    shared = [r for r in rows if r.get("run") == args.run and r.get("task") == SHARED]
    if not shared:
        rows.append({"run": args.run, "date": next(r["date"] for r in rows if r.get("run") == args.run),
                     "task": SHARED, "requests": 0, **{name: 0 for name in COUNTS}, "total": 0})
        shared = rows[-1:]
    shared[0]["credit_usd"] = f"{usd:g}"
    save_csv(path, rows)
    print(f"Đã ghi {usd:g} USD credit cho {args.run} vào {args.csv}.")
    return 0


def estimate(args):
    by_size = {}
    run_totals, run_credits = {}, {}
    for row in read_csv(ROOT / args.csv):
        total = parse_amount(row.get("total", ""))
        run_totals[row.get("run")] = run_totals.get(row.get("run"), 0) + (total or 0)
        if row.get("credit_usd"):
            try:
                run_credits[row.get("run")] = float(row["credit_usd"])
            except ValueError:
                pass
        if row.get("task") != SHARED and row.get("size") and total:
            by_size.setdefault(row["size"], []).append(total)
    print("| Cỡ | Số task đã đo | Trung vị thực tế | Ghi vào token_estimate |")
    print("| --- | --- | --- | --- |")
    for size in sorted(set(DEFAULT_ESTIMATES) | set(by_size), key=lambda s: (SIZE_ORDER.get(s, 9), s)):
        totals = by_size.get(size, [])
        median = int(statistics.median(totals)) if totals else None
        if len(totals) >= MIN_SAMPLES:
            suggestion = short(median)
        else:
            suggestion = f"{short(DEFAULT_ESTIMATES.get(size))} (tạm, chưa đủ {MIN_SAMPLES} task)"
        print(f"| {size} | {len(totals)} | {short(median)} | {suggestion} |")
    tokens = sum(run_totals[run] for run in run_credits)
    if tokens:
        usd = sum(run_credits.values())
        print(f"\nCredit đã dùng: {usd:g} USD cho {short(tokens)} token ở {len(run_credits)} phiên, "
              f"khoảng {usd / tokens * 1_000_000:.2f} USD mỗi 1M token.")
    return 0


def main(argv):
    parser = argparse.ArgumentParser(description="Đếm token AI theo task của phiên tự chạy.")
    commands = parser.add_subparsers(dest="command", required=True)
    report_parser = commands.add_parser("report")
    report_parser.add_argument("--since")
    report_parser.add_argument("--until")
    report_parser.add_argument("--run")
    report_parser.add_argument("--csv")
    report_parser.add_argument("--transcripts", nargs="+")
    estimate_parser = commands.add_parser("estimate")
    estimate_parser.add_argument("--csv", default=DEFAULT_CSV)
    credit_parser = commands.add_parser("credit")
    credit_parser.add_argument("--run", required=True)
    credit_parser.add_argument("--usd", required=True)
    credit_parser.add_argument("--csv", default=DEFAULT_CSV)
    args = parser.parse_args(argv[1:])
    return {"report": report, "estimate": estimate, "credit": credit}[args.command](args)


if __name__ == "__main__":
    sys.exit(main(sys.argv))
