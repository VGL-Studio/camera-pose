#!/usr/bin/env python3
"""Checks a whole run list before a dev approves it, and again at the start of the session.

Usage: scripts/run-check.py <docs/runs/RUN-YYYYMMDD-NN.md>
Exit 0: no error (warnings may be printed). Exit 1: at least one error.

plan-check.py looks at one plan at a time. This script looks at what only shows across the list and the
documents behind it, the mistakes that stopped or slowed earlier sessions:
  1. Every plan of the list exists, passes plan-check.py, and it and its ticket are approved (L-019).
  2. The spec of every ticket is confirmed, has no unanswered question, and has no history row still
     waiting for confirmation; a plan approved before the spec's last change is a warning (stale plan).
  3. Every dependency of a ticket is merged already, or comes earlier in the list.
  4. Every scenario of the ticket appears in section 4 of the plan; a scenario with fewer test rows
     than acceptance criteria in the spec is a warning.
  5. Two plans of the list that change the same file without depending on each other (L-014): a warning.
     Two plans that both create one file, dependent or not (L-033): a warning; one creates it, the other reuses it.
  6. A list that is a single dependency chain (L-015), and exceptions to test-first: warnings for people.
  7. Order hints and files of other plans (L-060): a plan's "thứ tự" line in section 6 ("A trước B", "sau A",
     "nhóm ...: A, B, C", "A, rồi B") that the list contradicts is an error, unless the list says
     "Bỏ đề xuất thứ tự: A trước B" (then a warning). A plan that names a file another plan of the list creates,
     without having it in its own section 3.2 and without depending on that plan: an error when it comes first,
     a warning (what it misses if the other plan stops) when it comes after.
RUN_CHECK_ROOT overrides the repository root (tests). Messages are in Vietnamese for the team.
"""

import importlib.util
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(os.environ.get("RUN_CHECK_ROOT", Path(__file__).resolve().parent.parent))
DOCS = ROOT / "docs"
MERGED = {"on-staging", "accepted", "released"}
# App shell code that test-first exceptions often leave without a JVM test; bugs there show only on ui-test (L-030).
SHELL_CODE = re.compile(r"AppNavHost|MainActivity|navigation/")


def load_plan_check():
    os.environ.setdefault("PLAN_CHECK_ROOT", str(ROOT))
    spec = importlib.util.spec_from_file_location("plan_check", Path(__file__).resolve().parent / "plan-check.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


PLAN_CHECK = load_plan_check()
front_matter = PLAN_CHECK.front_matter


def chapter(text, number):
    """Body of the '## <number>.' section."""
    match = re.search(rf"^## {number}\..*?$(.*?)(?=^## |\Z)", text, re.S | re.M)
    return match.group(1) if match else ""


def run_rows(text):
    """(ticket, plan path relative to docs/) of the table in section 1 of the run list, in order."""
    rows = []
    in_table = False
    for line in chapter(text, 1).splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if not line.lstrip().startswith("|"):
            if rows:
                break  # only the run table lists tasks; the pre-run review table below also names tickets
            continue
        in_table = in_table or cells[0] == "Thứ tự"
        if not in_table or len(cells) < 4 or not re.fullmatch(r"T-\d+", cells[1]):
            continue
        path = re.search(r"`([^`]+\.md)`", cells[3])
        rows.append((cells[1], path.group(1) if path else ""))
    return rows


def ticket_file(ticket):
    found = sorted(DOCS.glob(f"features/*/tickets/{ticket}-*.md"))
    return found[0] if found else None


def ids(value):
    return re.findall(r"(?:T|KB)-\d+", value or "")


def spec_problems(spec_path, plan_fields):
    errors, warnings = [], []
    text = spec_path.read_text()
    fields = front_matter(text)
    name = spec_path.relative_to(ROOT)
    if fields.get("status") != "confirmed":
        errors.append(f"{name}: spec đang '{fields.get('status')}', chưa confirmed; plan dựa trên spec chưa chốt.")
    for question in re.split(r"^\d+\. ", chapter(text, 9), flags=re.M)[1:]:
        answer = re.search(r"Trả lời:\s*(.*)", question)
        if answer is None or not answer.group(1).strip():
            title = question.splitlines()[0].strip("* ")[:70]
            errors.append(f"{name}: câu hỏi còn mở chưa có trả lời: '{title}'.")
    history = [line for line in chapter(text, 10).splitlines() if re.match(r"^\|\s*\d{4}-\d{2}-\d{2}", line)]
    for line in history:
        if "Chờ xác nhận lại" in line and fields.get("status") == "confirmed":
            date = line.split("|")[1].strip()
            warnings.append(f"{name}: dòng lịch sử {date} còn ghi 'Chờ xác nhận lại' trong khi spec confirmed; "
                            "người xác nhận ghi lại dòng lịch sử và ngày confirmed.")
    last_change = max((line.split("|")[1].strip() for line in history), default="")
    approved = plan_fields.get("approved_at", "")
    if approved and last_change > approved:
        warnings.append(f"{name}: spec đổi ngày {last_change}, sau khi plan được duyệt ({approved}); "
                        "xem lại plan có còn khớp spec không.")
    return errors, warnings


def criteria_per_scenario(spec_text):
    counts = {}
    for match in re.finditer(r"^### (KB-\d+)\..*?$(.*?)(?=^##|\Z)", spec_text, re.S | re.M):
        counts[match.group(1)] = len(re.findall(r"^- \[[ x]\]", match.group(2), re.M))
    return counts


def test_rows(plan_text):
    return [line for line in chapter(plan_text, 4).splitlines() if line.startswith("|")]


def depends_closure(ticket, depends):
    seen, stack = set(), list(depends.get(ticket, []))
    while stack:
        current = stack.pop()
        if current not in seen:
            seen.add(current)
            stack.extend(depends.get(current, []))
    return seen


def order_hints(ticket, plan_text):
    """(first, second) pairs from the lines of section 6 that talk about the run order (L-060)."""
    pairs = []
    ids_pattern = r"T-\d+"
    for line in chapter(plan_text, 6).splitlines():
        if "thứ tự" not in line:
            continue
        for clause in re.split(r";|\.\s|\.$", line):
            for match in re.finditer(rf"({ids_pattern})\s+trước\s+({ids_pattern}(?:(?:,\s*|\s+và\s+){ids_pattern})*)", clause):
                pairs += [(match.group(1), later) for later in re.findall(ids_pattern, match.group(2))]
            for match in re.finditer(rf"(?<![\w-])trước\s+({ids_pattern}(?:(?:,\s*|\s+và\s+){ids_pattern})*)", clause):
                if not re.search(rf"{ids_pattern}\s+trước\s+{re.escape(match.group(1))}", clause):
                    pairs += [(ticket, later) for later in re.findall(ids_pattern, match.group(1))]
            for match in re.finditer(rf"(?:({ids_pattern})\s+(?:bắt buộc\s+)?)?(?:ngay\s+)?sau\s+({ids_pattern})", clause):
                pairs.append((match.group(2), match.group(1) or ticket))
            listed = []
            if "nhóm" in clause:
                paren = re.search(rf"\(({ids_pattern}(?:,\s*{ids_pattern})+)\)", clause)
                listed = re.findall(ids_pattern, paren.group(1)) if paren else []
            tail = clause.rsplit(":", 1)[-1].strip() if ":" in clause else ""
            if re.fullmatch(rf"{ids_pattern}(?:,\s*{ids_pattern})+", tail):
                listed = re.findall(ids_pattern, tail)
            pairs += list(zip(listed, listed[1:]))
            for match in re.finditer(rf"{ids_pattern}(?:,?\s*rồi\s*{ids_pattern})+", clause):
                chain = re.findall(ids_pattern, match.group(0))
                pairs += list(zip(chain, chain[1:]))
    return [pair for pair in pairs if pair[0] != pair[1]]


def repo_basenames():
    """Base names of the files tracked by git; empty without a git repository."""
    try:
        out = subprocess.run(["git", "-C", str(ROOT), "ls-files"], capture_output=True, text=True, check=True).stdout
    except (OSError, subprocess.CalledProcessError):
        return []
    return [Path(line).name for line in out.splitlines()]


def check(run_path):
    errors, warnings = [], []
    rows = run_rows(run_path.read_text())
    if not rows:
        return ["mục 1 không có dòng nào đúng mẫu (Thứ tự | Ticket | Feature | Plan | ...)."], []
    run_text = run_path.read_text()
    in_list = [ticket for ticket, _ in rows]
    depends, files_of, created_by, plan_texts, hints = {}, {}, {}, {}, {}

    for position, (ticket, plan_ref) in enumerate(rows):
        plan = DOCS / plan_ref
        if not plan_ref or not plan.is_file():
            errors.append(f"{ticket}: không tìm thấy plan '{plan_ref}'.")
            continue
        plan_text = plan.read_text()
        plan_fields = front_matter(plan_text)
        if plan_fields.get("status") != "approved":
            errors.append(f"{ticket}: plan đang '{plan_fields.get('status')}', chưa approved.")
        plan_errors, _ = PLAN_CHECK.check(plan)
        for message in plan_errors:
            errors.append(f"{ticket}: plan-check: {message}")
        plan_texts[ticket] = plan_text
        for pair in order_hints(ticket, plan_text):
            hints.setdefault(pair, []).append(ticket)
        files_of[ticket] = {path for op, path in PLAN_CHECK.file_list(plan_text) if not path.endswith("/")}
        for op, path in PLAN_CHECK.file_list(plan_text):
            if op == "create" and not path.endswith("/"):
                created_by.setdefault(path, []).append(ticket)

        ticket_path = ticket_file(ticket)
        if ticket_path is None:
            errors.append(f"{ticket}: không tìm thấy ticket.")
            continue
        fields = front_matter(ticket_path.read_text())
        if fields.get("status") != "approved":
            errors.append(f"{ticket}: ticket đang '{fields.get('status')}', chưa approved (L-019).")
        depends[ticket] = [t for t in ids(fields.get("depends_on")) if t.startswith("T-")]
        for dependency in depends[ticket]:
            if dependency in in_list[:position]:
                continue
            dependency_path = ticket_file(dependency)
            status = front_matter(dependency_path.read_text()).get("status") if dependency_path else None
            if status in MERGED:
                continue
            where = "đứng sau trong danh sách" if dependency in in_list else f"chưa merge ('{status}')"
            errors.append(f"{ticket}: phụ thuộc {dependency} {where}.")

        scenarios = [s for s in ids(fields.get("scenarios")) if s.startswith("KB-")]
        if not fields.get("spec") and not scenarios:
            # A script or process ticket: its requirement is a lesson or an ADR, not a business spec.
            continue
        spec_path = DOCS / fields.get("spec", "")
        if not fields.get("spec") or not spec_path.is_file():
            errors.append(f"{ticket}: không tìm thấy spec '{fields.get('spec')}'.")
            continue
        spec_errors, spec_warnings = spec_problems(spec_path, plan_fields)
        # A spec shared by several tickets reports each problem once.
        errors += [f"{ticket}: {m}" for m in dict.fromkeys(spec_errors) if not any(e.endswith(m) for e in errors)]
        warnings += [f"{ticket}: {m}" for m in dict.fromkeys(spec_warnings) if not any(w.endswith(m) for w in warnings)]

        counts = criteria_per_scenario(spec_path.read_text())
        tests = test_rows(plan_text)
        for scenario in scenarios:
            covering = [row for row in tests if re.search(rf"\b{scenario}\b", row)]
            if not covering:
                errors.append(f"{ticket}: kịch bản {scenario} của ticket không có dòng nào ở mục 4 của plan.")
            elif len(covering) < counts.get(scenario, 0):
                warnings.append(f"{ticket}: {scenario} có {counts[scenario]} tiêu chí trong spec nhưng chỉ "
                                f"{len(covering)} dòng test ở mục 4; xem tiêu chí nào chưa có test.")
        exception = re.search(r"\*\*Ngoại lệ viết test trước:\*\*(.*?)(?=^#|\Z)", plan_text, re.S | re.M)
        shell = sorted(set(SHELL_CODE.findall(exception.group(1)))) if exception else []
        if shell:
            warnings.append(f"{ticket}: ngoại lệ viết test trước gồm vỏ app ({', '.join(shell)}): lỗi ở đó chỉ lộ ra "
                            "ở ui-test, vòng CI dài nhất (L-030). Cân nhắc một test Robolectric thay cho ngoại lệ.")

    listed = [t for t in in_list if t in files_of]
    for i, first in enumerate(listed):
        for second in listed[i + 1:]:
            related = first in depends_closure(second, depends) or second in depends_closure(first, depends)
            shared = sorted(files_of[first] & files_of[second])
            if shared and not related:
                warnings.append(f"{first} và {second} cùng sửa {', '.join(shared[:3])}"
                                f"{' ...' if len(shared) > 3 else ''} mà không phụ thuộc nhau (L-014).")
    for path, tickets in sorted(created_by.items()):
        if len(tickets) > 1:
            warnings.append(f"{' và '.join(tickets)} cùng ghi Tạo {path}: chỉ một plan tạo file này, plan sau ghi Sửa "
                            "hoặc dùng lại, và hai plan phải thống nhất nội dung trước khi duyệt (L-033).")
    position_of = {t: i for i, t in enumerate(in_list)}
    for (first, second), proposers in sorted(hints.items()):
        if first not in position_of or second not in position_of or position_of[first] < position_of[second]:
            continue
        dropped = re.search(rf"Bỏ đề xuất thứ tự:\s*{first} trước {second}", run_text)
        if dropped:
            warnings.append(f"danh sách bỏ đề xuất thứ tự {first} trước {second} của plan {', '.join(proposers)} "
                            "(L-060); xem lý do ghi kèm trong danh sách.")
        else:
            errors.append(f"{second} đứng trước {first}, nhưng plan {', '.join(proposers)} (mục 6) đề xuất {first} trước "
                          f"{second} (L-060). Đổi thứ tự, hoặc ghi \"Bỏ đề xuất thứ tự: {first} trước {second}\" kèm lý do "
                          "vào mục Lý do của thứ tự chạy.")
    # A plan that names a file another plan creates, without owning or depending on it (L-060).
    names = repo_basenames()
    created_names = [Path(path).name for path in created_by]
    for path, creators in sorted(created_by.items()):
        base = Path(path).name
        unique = base not in names and created_names.count(base) == 1
        for creator in creators:
            for user in listed:
                if user == creator or path in files_of[user] or creator in depends_closure(user, depends):
                    continue
                named = set(re.findall(r"`([^`]+)`", chapter(plan_texts[user], 3) + chapter(plan_texts[user], 4)))
                mention = path if path in named else (base if unique and base in named else None)
                if mention is None:
                    continue
                if position_of[user] < position_of[creator]:
                    errors.append(f"{user} nhắc `{mention}` mà {creator} (đứng sau) tạo (L-060).")
                else:
                    warnings.append(f"{user} nhắc `{mention}` mà {creator} tạo; nếu {creator} dừng thì {user} thiếu "
                                    f"`{mention}`. Cân nhắc thêm {creator} vào depends_on của {user}, hoặc ghi phương án "
                                    "ở mục 3.4 của plan (L-060).")
    independent = [t for t in in_list if not set(depends.get(t, [])) & set(in_list)]
    if len(in_list) > 1 and len(independent) == 1:
        warnings.append("danh sách chỉ có một chuỗi phụ thuộc: một task dừng thì các task sau bị bỏ qua (L-015).")
    return errors, warnings


def main(argv):
    if len(argv) != 2:
        print("Cách dùng: scripts/run-check.py <docs/runs/RUN-YYYYMMDD-NN.md>", file=sys.stderr)
        return 2
    run = Path(argv[1])
    if not run.is_file():
        print(f"{argv[1]}: LỖI: không có file này.")
        return 1
    errors, warnings = check(run)
    for message in errors:
        print(f"{argv[1]}: LỖI: {message}")
    for message in warnings:
        print(f"{argv[1]}: CHÚ Ý: {message}")
    if not errors:
        print(f"{argv[1]}: qua.")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
