#!/usr/bin/env python3
"""Writes docs/STATUS.md, the progress page of the project, from the statuses already kept in the documents.

Usage: scripts/status.py [--check | --sync]
  (no option)  rewrite docs/STATUS.md
  --check      exit 1 when docs/STATUS.md is not what the documents say now (nothing is written)
  --sync       first move each ticket's status after its plan's (L-050), then rewrite docs/STATUS.md:
               plan approved -> ticket approved when the ticket is backlog, ready, planned or stopped
               (a stopped task whose new plan version is approved goes back to approved, auto-run.md, section 3);
               plan stopped -> ticket stopped unless the ticket is already stopped, cancelled or past the session.
               The person who approves or stops the plan runs it; the script only follows that decision.

Every status comes from the front matter of requests, specs, tickets, plans, feedback, ADRs and run lists, and from
the "Trạng thái" lines of docs/lessons.md. Section 6 (release readiness, L-086) also reads the placement keys of
core/ads AdPlacement.kt, the unchecked "Cần người kiểm tra" items of finished run reports and the owner/repo of the
origin remote. It never opens production config, scripts/release.sh or the release folder, and never uses the network.
Nobody edits STATUS.md by hand: change the document, then run the script.
The page has no date or time, so running it twice on the same documents gives the same file.
The page is not in git on main (.gitignore): .github/workflows/status-page.yml runs this script after each push to
main and pushes the page to the `status` branch (L-066). Run it locally to preview the page.
STATUS_ROOT overrides the repository root (tests). The page is in Vietnamese for the team.
"""

import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(os.environ.get("STATUS_ROOT", Path(__file__).resolve().parent.parent))
DOCS = ROOT / "docs"
OUTPUT = DOCS / "STATUS.md"
# Ticket statuses that mean the work is in the main branch.
TICKET_DONE = {"on-staging", "accepted", "released"}
FEEDBACK_OPEN = {"new", "triaged", "planned"}
REQUEST_LABELS = {"new": "chưa có spec", "specified": "đã có spec", "done": "xong", "dropped": "bỏ"}


def front_matter(path):
    """Front matter fields; a key followed by '  - item' lines becomes a list."""
    match = re.match(r"^---\n(.*?)\n---\n", path.read_text(), re.S)
    fields, last = {}, None
    for line in (match.group(1).splitlines() if match else []):
        item = re.match(r"^\s+-\s+(.*)$", line)
        if item and last:
            if not isinstance(fields[last], list):
                fields[last] = []
            fields[last].append(item.group(1).split("#", 1)[0].strip())
        elif ":" in line and not line.startswith(" "):
            key, value = line.split(":", 1)
            last = key.strip()
            fields[last] = value.split("#", 1)[0].strip()
    return fields


def heading(path):
    """First '# ' heading without its 'KIND:' prefix, for documents without a title field."""
    for line in path.read_text().splitlines():
        if line.startswith("# "):
            words = line[2:].split(":", 1)[-1].strip().split()
            # Headings are upper case; keep short acronyms (AI, R2, UI) and lower the rest.
            words = [w if len(w) <= 2 and w.isascii() and w.isupper() else w.lower() for w in words]
            return " ".join(words)[:1].upper() + " ".join(words)[1:]
    return path.stem


def link(path):
    return f"[{path.name}]({path.relative_to(DOCS).as_posix()})"


def ticket_number(ticket_id):
    return int(re.sub(r"\D", "", ticket_id) or 0)


def requests():
    found = []
    for path in sorted(DOCS.glob("features/*/requests/*.md")) + sorted(DOCS.glob("inbox/*.md")):
        if path.name == "README.md":
            continue
        fields = front_matter(path)
        feature = fields.get("feature") or (path.parent.parent.name if "features" in path.parts else "")
        found.append({"path": path, "title": heading(path), "date": fields.get("date", ""),
                      "feature": feature, "status": fields.get("status", "")})
    return found


def features():
    found = []
    for folder in sorted(p for p in (DOCS / "features").glob("*") if p.is_dir()):
        spec = folder / "spec.md"
        tickets = []
        for path in folder.glob("tickets/T-*.md"):
            fields = front_matter(path)
            ticket_id = path.name.split("-")[0] + "-" + path.name.split("-")[1]
            plans = list(folder.glob(f"plans/{ticket_id}-*.md"))
            plan = front_matter(plans[0]) if plans else {}
            tickets.append({"id": ticket_id, "path": path, "title": fields.get("title", ""),
                            "status": fields.get("status", ""), "spec": fields.get("spec"),
                            "plan": plans[0] if plans else None,
                            "plan_status": plan.get("status", ""), "plan_version": plan.get("version", ""),
                            "estimate": plan.get("token_estimate", "")})
        feedback = []
        for path in sorted(folder.glob("feedback/FB-*.md")):
            fields = front_matter(path)
            feedback.append({"id": fields.get("id", path.stem), "path": path, "title": fields.get("title", ""),
                             "status": fields.get("status", ""), "severity": fields.get("severity", "")})
        found.append({"name": folder.name, "spec": spec if spec.is_file() else None,
                      "spec_status": front_matter(spec).get("status", "") if spec.is_file() else "",
                      "tickets": sorted(tickets, key=lambda t: ticket_number(t["id"])), "feedback": feedback})
    return found


def runs():
    found = []
    for path in sorted(DOCS.glob("runs/RUN-*.md")):
        fields = front_matter(path)
        listed = re.search(r"^## 1\..*?$(.*?)(?=^## )", path.read_text(), re.S | re.M)
        tickets = re.findall(r"^\|\s*\d+\s*\|\s*(T-\d+)\s*\|", listed.group(1) if listed else "", re.M)
        found.append({"id": fields.get("id", path.stem), "path": path, "status": fields.get("status", ""),
                      "started": fields.get("started_at", ""), "staging": fields.get("staging_build", ""),
                      "tickets": tickets})
    return found


def lessons():
    path = DOCS / "lessons.md"
    if not path.is_file():
        return []
    found = []
    for block in re.split(r"^### ", path.read_text(), flags=re.M)[1:]:
        title = block.splitlines()[0]
        # "L-001. Title" (template) or "L-001: Title" (some apps); the status may be followed by ". **Loại:** ..." (L-057).
        heading = re.match(r"(L-\d+)[.:]\s*(.*)", title)
        status = re.search(r"^- \*\*Trạng thái:\*\*\s*([a-z]+)", block, re.M)
        if heading:
            found.append({"id": heading.group(1), "title": heading.group(2).strip(),
                          "status": status.group(1) if status else ""})
    return found


def duplicate_lessons(all_lessons):
    """Lesson numbers used more than once, as when two branches add lessons at the same time (L-056)."""
    seen, twice = set(), []
    for lesson in all_lessons:
        if lesson["id"] in seen and lesson["id"] not in twice:
            twice.append(lesson["id"])
        seen.add(lesson["id"])
    return twice


def adrs():
    found = []
    for path in sorted(DOCS.glob("adr/[0-9]*.md")):
        fields = front_matter(path)
        found.append({"id": fields.get("id", path.stem), "path": path, "title": fields.get("title", ""),
                      "status": fields.get("status", "")})
    return found


def ad_placements():
    """(placements, problem): the key and format of each AdPlacement entry; problem is a sentence when none is read."""
    paths = sorted(ROOT.glob("core/ads/src/main/**/AdPlacement.kt"))
    if not paths:
        return [], "Không tìm thấy `AdPlacement.kt`."
    try:
        # Bytes, not read_text(): universal newlines would turn a CRLF line into one that release.sh skips.
        text = paths[0].read_bytes().decode()
    except (OSError, UnicodeDecodeError):
        return [], "Không đọc được `AdPlacement.kt`."
    # Same one-line rule as scripts/release.sh (which this script never opens): NAME("key", AdFormat.X[, attributes]),
    # formats with underscores; gallery_ keys belong to the staging-only ad gallery and need no production ad unit.
    found = [{"key": m.group(1), "format": m.group(2).lower()} for m in
             re.finditer(r'^\s*[A-Z0-9_]+\("([a-z0-9_]+)", AdFormat\.([A-Z_]+)(?:, .*)?\),?$', text, re.M)
             if not m.group(1).startswith("gallery_")]
    if re.search(r"^\s*[A-Z][A-Z0-9_]*\($", text, re.M):
        return found, "`AdPlacement.kt` khai báo vị trí trên nhiều dòng; `scripts/release.sh` sẽ từ chối, viết mỗi vị trí trên một dòng."
    return found, "" if found else "Không đọc được khóa nào trong `AdPlacement.kt`."


def shorten(text, limit=160):
    """Cuts a long line at the last space before `limit` that is outside backticks, so a code span is never split."""
    if len(text) <= limit:
        return text
    in_code, space, code_start = False, 0, None
    for i, char in enumerate(text[:limit]):
        if char == "`":
            in_code = not in_code
            code_start = i if in_code else None
        elif char == " " and not in_code:
            space = i
    cut = space or (code_start if in_code and code_start else limit)
    return text[:cut].rstrip() + "..."


def is_not_a_task(text):
    """True for a line that is no task: the text after the first colon starts with 'không có', or it is a template.

    A template placeholder is <word...> outside backticks; `<->` or `a < b, c > d` are not placeholders."""
    after = text.split(":", 1)[-1].strip().lower()
    return after.startswith("không có") or bool(re.search(r"<\w[^<>]*>", re.sub(r"`[^`]*`", "", text)))


def manual_checks():
    """The '- [ ]' items of the "Cần người kiểm tra" part of each finished run report.

    A report that is not UTF-8 already stops runs() before this section, like every other document."""
    found = []
    for path in sorted(DOCS.glob("runs/RUN-*.md"), reverse=True):
        if front_matter(path).get("status") != "finished":
            continue
        lines = path.read_text().splitlines()
        start = next((i for i, line in enumerate(lines) if line.strip() == "### Cần người kiểm tra"), None)
        if start is None:
            continue
        item = None
        for line in lines[start + 1:]:
            if re.match(r"^#{2,3} ", line):
                break
            task = re.match(r"^- \[ \] (.*\S)\s*$", line)
            if task:
                item = {"run": path.stem, "path": path, "text": task.group(1).strip(), "children": 0}
                if not is_not_a_task(item["text"]):
                    found.append(item)
            elif re.match(r"^\s+- ", line) and item:
                item["children"] += 1
            else:
                item = None
    return found


def issue_links():
    """Links to the open issues of the origin repo, or None. Only owner/repo is kept, so credentials never show."""
    try:
        result = subprocess.run(["git", "-C", str(ROOT), "config", "--get", "remote.origin.url"],
                                capture_output=True, text=True, check=False, timeout=5)
    except (OSError, subprocess.SubprocessError):
        return None
    match = re.search(r"^(?:https?://(?:[^@/\s]+@)?|ssh://git@|git@)github\.com[:/]([\w.-]+)/([\w.-]+?)(?:\.git)?/?$",
                      result.stdout.strip())
    if result.returncode != 0 or not match:
        return None
    base = f"https://github.com/{match.group(1)}/{match.group(2)}/issues?q=is%3Aissue+is%3Aopen"
    return {"open": base, "broken": base + "+label%3Amain-broken"}


def counts(items, key="status"):
    tally = {}
    for item in items:
        tally[item[key] or "?"] = tally.get(item[key] or "?", 0) + 1
    return ", ".join(f"{n} `{status}`" for status, n in sorted(tally.items(), key=lambda kv: (-kv[1], kv[0])))


def cell(text):
    return str(text).replace("|", "\\|")


def render():
    all_requests, all_features, all_runs = requests(), features(), runs()
    all_lessons, all_adrs = lessons(), adrs()
    tickets = [t for f in all_features for t in f["tickets"]]
    runs_of = {}
    for run in all_runs:
        for ticket_id in run["tickets"]:
            runs_of.setdefault(ticket_id, []).append(run["id"])

    out = ["# TIẾN ĐỘ DỰ ÁN", "",
           "<!-- Sinh bởi scripts/status.py từ trạng thái trong các tài liệu. Không sửa tay: sửa tài liệu gốc rồi chạy lại "
           "script (docs/CLAUDE.md, mục Trang tiến độ). -->", ""]

    waiting = []
    for r in all_requests:
        if r["status"] in ("", "new"):
            waiting.append(f"Yêu cầu chưa có spec: {link(r['path'])} ({cell(r['title'])})")
    for f in all_features:
        if f["spec_status"] in ("draft", "changed"):
            waiting.append(f"Spec `{f['spec_status']}` chờ xác nhận: {link(f['spec'])} ({f['name']})")
        for t in f["tickets"]:
            if t["plan_status"] == "draft":
                waiting.append(f"Plan `draft` chờ duyệt: {link(t['plan'])}")
        for fb in f["feedback"]:
            if fb["status"] == "new":
                waiting.append(f"Phản hồi chờ phân loại: {link(fb['path'])} ({cell(fb['title'])})")
    for run in all_runs:
        if run["status"] == "draft":
            waiting.append(f"Danh sách chạy `draft` chờ duyệt: {link(run['path'])}")
        elif run["status"] == "approved":
            waiting.append(f"Danh sách chạy đã duyệt, chờ kích hoạt: {link(run['path'])}")
    ready = [t for t in tickets if t["status"] == "approved" and t["plan_status"] == "approved"
             and not any(t["id"] in run["tickets"] and run["status"] in ("draft", "approved", "running")
                         for run in all_runs)]
    for t in ready:
        waiting.append(f"Ticket đã duyệt, chưa vào danh sách chạy nào: {link(t['path'])}")
    for lesson in all_lessons:
        if lesson["status"] == "proposed":
            waiting.append(f"Bài học `proposed` chờ retro: {lesson['id']} ({cell(lesson['title'])})")
    for adr in all_adrs:
        if adr["status"] == "proposed":
            waiting.append(f"ADR `proposed` chờ quyết định: {link(adr['path'])}")
    out += ["## 1. Đang chờ người", ""]
    out += [f"- {line}" for line in waiting] if waiting else ["Không có."]
    out.append("")

    out += ["## 2. Yêu cầu", "",
            "| Yêu cầu | Ngày | Feature | Trạng thái | Ticket xong |", "| --- | --- | --- | --- | --- |"]
    for r in all_requests:
        feature = next((f for f in all_features if f["name"] == r["feature"]), None)
        done = "-"
        if feature and feature["tickets"]:
            active = [t for t in feature["tickets"] if t["status"] != "cancelled"]
            done = f"{sum(t['status'] in TICKET_DONE for t in active)}/{len(active)}"
        status = r["status"] or "chưa ghi"
        out.append(f"| {cell(r['title'])} ({link(r['path'])}) | {r['date']} | {r['feature'] or '-'} | "
                   f"`{status}` {REQUEST_LABELS.get(status, '')} | {done} |")
    out.append("")

    out += ["## 3. Feature", ""]
    for f in all_features:
        spec = f"{link(f['spec'])} `{f['spec_status']}`" if f["spec"] else "chưa có spec"
        out += [f"### {f['name']}", "", f"Spec: {spec}. Ticket: {counts(f['tickets']) or 'chưa có'}.", ""]
        if f["tickets"]:
            out += ["| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |",
                    "| --- | --- | --- | --- | --- | --- |"]
            for t in f["tickets"]:
                plan = f"`{t['plan_status']}` (bản {t['plan_version']})" if t["plan"] else "chưa có"
                out.append(f"| [{t['id']}]({t['path'].relative_to(DOCS).as_posix()}) | {cell(t['title'])} | "
                           f"`{t['status']}` | {plan} | {t['estimate'] or '-'} | "
                           f"{', '.join(runs_of.get(t['id'], [])) or '-'} |")
            out.append("")
        open_feedback = [fb for fb in f["feedback"] if fb["status"] in FEEDBACK_OPEN]
        out.append(f"Phản hồi: {counts(f['feedback']) or 'chưa có'}.")
        for fb in open_feedback:
            out.append(f"- Đang mở: {link(fb['path'])} `{fb['status']}`, mức `{fb['severity']}`: {cell(fb['title'])}")
        out.append("")

    out += ["## 4. Phiên tự chạy", ""]
    if all_runs:
        out += ["| Phiên | Trạng thái | Bắt đầu | Ticket | Bản staging |", "| --- | --- | --- | --- | --- |"]
    else:
        out.append("Chưa có phiên nào.")
    for run in reversed(all_runs):
        out.append(f"| [{run['id']}]({run['path'].relative_to(DOCS).as_posix()}) | `{run['status']}` | "
                   f"{cell(run['started'])} | {', '.join(run['tickets']) or '-'} | {cell(run['staging']) or '-'} |")
    out.append("")

    out += ["## 5. Bài học và ADR", "",
            f"- Bài học ([lessons.md](lessons.md)): {len(all_lessons)} bài; {counts(all_lessons) or 'chưa có'}.",
            f"- ADR: {len(all_adrs)}; {counts(all_adrs) or 'chưa có'}.", ""]
    out += release_readiness(all_features)
    return "\n".join(out)


def release_readiness(all_features):
    """Section 6: what is still open before the production build, from documents in the repo only (L-086)."""
    out = ["## 6. Sẵn sàng phát hành", "",
           "<!-- Chỉ từ tài liệu trong repo. Mã quảng cáo thật, cấu hình production và tình trạng trên Google Play",
           "không nằm trong repo: người phát hành kiểm theo docs/ops/release.md. -->", "", "### Quảng cáo", ""]
    placements, problem = ad_placements()
    if placements:
        keys = ", ".join(f"`{p['key']}` ({p['format']})" for p in placements)
        out.append("- Bản production cần một mã quảng cáo thật cho mỗi khóa dưới đây (docs/ops/release.md, mục 1.2): "
                   f"{keys}.")
    if problem:
        out.append(problem)
    out += ["", "### Kiểm thủ công chưa đánh dấu", ""]
    checks = manual_checks()
    if checks:
        phases = len({c["run"] for c in checks})
        out += [f"{len(checks)} mục trong {phases} phiên. Người kiểm xong thì đánh dấu [x] trong báo cáo phiên."]
        for c in checks:
            extra = f" ({c['children']} dòng con)" if c["children"] else ""
            out.append(f"- [{c['run']}](runs/{c['path'].name}): {shorten(c['text'])}{extra}")
    else:
        out.append("Không có.")
    out += ["", "### Ticket chưa lên staging", ""]
    lines, total, process = [], 0, False
    for f in all_features:
        open_tickets = [t for t in f["tickets"] if t["status"] not in TICKET_DONE | {"cancelled"}]
        if open_tickets:
            total += len(open_tickets)
            process = process or f["spec"] is None or any(t["spec"] == "" for t in open_tickets)
            lines.append(f"- {f['name']}: " + ", ".join(f"{t['id']} `{t['status']}`" for t in open_tickets))
    if lines:
        out += [f"{total} ticket{' (gồm ticket quy trình)' if process else ''}."] + lines
    else:
        out.append("Không có.")
    out += ["", "### Phản hồi đang mở", ""]
    feedback = [fb for f in all_features for fb in f["feedback"] if fb["status"] in FEEDBACK_OPEN]
    out += [f"- {fb['id']} `{fb['status']}`, mức `{fb['severity']}`: {cell(fb['title'])}" for fb in feedback] or ["Không có."]
    out += ["", "### Issue đang mở", ""]
    links = issue_links()
    if links:
        out.append(f"- [Mọi issue đang mở]({links['open']}), [issue main-broken]({links['broken']})")
    else:
        out.append("Không đọc được repo GitHub từ `remote.origin.url`; xem issue trên trang của repo.")
    out.append("")
    return out


def sync_target(ticket_status, plan_status):
    """The status a ticket takes after its plan's, or None to leave it. scripts/plan-check.py (rule 10) mirrors it."""
    if plan_status == "approved" and ticket_status in ("backlog", "ready", "planned", "stopped"):
        return "approved"
    if plan_status == "stopped" and ticket_status not in ("stopped", "cancelled", "on-staging", "accepted", "released"):
        return "stopped"
    return None


def sync():
    """Writes the new status into the front matter of every ticket that lags behind its plan."""
    changed = 0
    for feature in features():
        for ticket in feature["tickets"]:
            target = sync_target(ticket["status"], ticket["plan_status"])
            if not target:
                continue
            text = ticket["path"].read_text()
            head, sep, rest = text.partition("\n---\n")
            # Only the front matter changes; a comment after the value stays.
            head = re.sub(r"^(status:[ \t]*)[^\s#]*", lambda m: m.group(1) + target, head, count=1, flags=re.M)
            ticket["path"].write_text(head + sep + rest)
            print(f"{ticket['id']}: ticket '{ticket['status']}' -> '{target}' theo plan '{ticket['plan_status']}'.")
            changed += 1
    if not changed:
        print("Trạng thái ticket đã khớp với plan.")


def main(argv):
    twice = duplicate_lessons(lessons())
    if twice:
        print(f"docs/lessons.md: LỖI: số bài học dùng hai lần: {', '.join(twice)}. Đổi số bài học thêm sau "
              "(và mọi chỗ nhắc tới nó) thành số tiếp theo của sổ.", file=sys.stderr)
        return 1
    if "--sync" in argv[1:]:
        sync()
    page = render()
    if "--check" in argv[1:]:
        if OUTPUT.is_file() and OUTPUT.read_text() == page:
            print("docs/STATUS.md khớp với tài liệu.")
            return 0
        print("docs/STATUS.md chưa khớp với tài liệu. Chạy scripts/status.py để sinh lại trang; trang không có trong git, "
              "status-page.yml sinh nó trên nhánh status (docs/CLAUDE.md, mục Trang tiến độ).")
        return 1
    OUTPUT.write_text(page)
    print("Đã ghi docs/STATUS.md.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
