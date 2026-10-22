#!/usr/bin/env python3
"""Checks a plan before people review it and again right before an auto-run session starts its task.

Usage: scripts/plan-check.py <plan.md> [<plan.md> ...]
Exit 0: no error (warnings may be printed). Exit 1: at least one error.

Finds, from the plan alone, the mistakes that otherwise stop a task in the middle of the night
(RUN-20261016-02 stopped on a test that read a file the plan changed, L-020):
  1. The file list (section 3.2) matches the repository: files to change exist, files to create do not,
     except files that a plan this ticket depends on creates.
  2. Every script, workflow, build file or test that names a changed file is in the file list too,
     or the plan mentions it (to say why it is not affected).
  3. Protected paths need a high-risk plan (risk-scoring.md, section 1; CLAUDE.md); scripts/release.sh is never allowed.
  4. A plan that runs scripts/design-compare.sh writes a screenshot for every design image of its feature,
     except the images it declares as drawn by another ticket, on a line
     'Ảnh thiết kế của ticket khác (T-NNN): `Name`, ...'. Each declared image must exist and the line names a ticket.
  5. An approved plan has its reviewers and date.
  6. The plan has a token estimate (token_estimate, docs/ops/token-usage.md); a missing one is only a warning.
  7. A plan whose criterion E (novelty of the technique, risk-scoring.md) scores 2 or more has a trial-run
     section 4.1 that names scripts/check.sh (L-001, L-010). An error for a draft plan; only a warning for a plan
     reviewed before this rule, so a session does not skip it.
PLAN_CHECK_ROOT overrides the repository root (tests). Messages are in Vietnamese for the team.
"""

import os
import re
import sys
from pathlib import Path

ROOT = Path(os.environ.get("PLAN_CHECK_ROOT", Path(__file__).resolve().parent.parent))
OPERATIONS = {"tạo": "create", "sửa": "change", "ghi lại": "change", "xóa": "delete", "xoá": "delete"}
# Places that read files by name. Docs are left out: a doc naming a file does not break when the file changes.
READER_DIRS = ["scripts", ".github", "build-logic"]
READER_GLOBS = ["**/build.gradle.kts", "**/src/test/**/*", "**/src/androidTest/**/*"]
SKIP_DIRS = {"build", "bin", ".gradle", ".git", "node_modules"}
# Files searched by name as well as by path: data and config files that scripts open by name.
NAME_SEARCH_SUFFIXES = {".json", ".yml", ".yaml", ".toml", ".properties", ".py", ".sh", ".txt", ".md"}
GENERIC_NAMES = {"README.md", "CLAUDE.md", "build.gradle.kts", "settings.gradle.kts", "gradle.properties"}
HIGH_RISK_PREFIXES = [".github/", "scripts/check.sh", "scripts/merge-if-green.sh", "build-logic/",
                      "gradle/libs.versions.toml", "core/"]
NEVER = ["scripts/release.sh"]
SELF = "scripts/plan-check.py"
# Words of the plan template's section 9 that a reviewer replaces (docs/templates/plan.md).
DECISION_PLACEHOLDERS = ["Duyệt / Dừng", "<tên>", "<tên, nếu rủi ro cao>"]
# A plan smaller than its feature names the design images another ticket draws (rule 4).
OTHER_TICKET_IMAGES = re.compile(r"Ảnh thiết kế của ticket khác([^:\n]*):([^\n]*)")
# Score of criterion E in the risk table of section 5 (rule 7).
NOVELTY_SCORE = re.compile(r"^\|\s*E\.[^|]*\|\s*(\d)", re.M)
TRIAL_RUN_HEADING = "4.1. Chạy thử"


def front_matter(text):
    match = re.match(r"^---\n(.*?)\n---\n", text, re.S)
    fields = {}
    for line in (match.group(1).splitlines() if match else []):
        if ":" in line:
            key, value = line.split(":", 1)
            fields[key.strip()] = value.split("#", 1)[0].strip()
    return fields


def current_decision(text, version):
    """Section 9 of the plan, or only the block of the current version when it has "Phiên bản <n>:" blocks."""
    match = re.search(r"^## 9\..*?$(.*?)(?=^## |\Z)", text, re.S | re.M)
    body = match.group(1) if match else ""
    blocks = re.split(r"^Phiên bản (\d+):\s*$", body, flags=re.M)
    if len(blocks) > 1:
        by_version = dict(zip(blocks[1::2], blocks[2::2]))
        return by_version.get(version, blocks[-1])
    return body


def section(text, heading):
    match = re.search(rf"^### {re.escape(heading)}.*?$(.*?)(?=^##)", text, re.S | re.M)
    return match.group(1) if match else ""


def file_list(text):
    """(operation, path) for every path in the table of section 3.2."""
    rows = []
    for line in section(text, "3.2").splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 2 or cells[0].lower() not in OPERATIONS:
            continue
        operation = OPERATIONS[cells[0].lower()]
        previous = None
        for path in re.findall(r"`([^`]+)`", cells[1]):
            # "`dir/A.png`, `B.png`": B.png lives in the folder of A.png.
            if "/" not in path and previous:
                path = previous.rsplit("/", 1)[0] + "/" + path
            rows.append((operation, path))
            previous = path
    return rows


def unknown_operations(text):
    found = []
    for line in section(text, "3.2").splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 3 and "`" in cells[1] and cells[0].lower() not in OPERATIONS:
            found.append(cells[0])
    return found


def plans_of_dependencies(fields):
    """Plans of the tickets this plan's ticket depends on."""
    ticket = fields.get("ticket", "")
    plans = []
    for ticket_file in ROOT.glob(f"docs/features/*/tickets/{ticket}-*.md"):
        depends = re.findall(r"T-\d+", front_matter(ticket_file.read_text()).get("depends_on", ""))
        for dependency in depends:
            plans += list(ROOT.glob(f"docs/features/*/plans/{dependency}-*.md"))
    return plans


def reader_files():
    files = set()
    for folder in READER_DIRS:
        files.update(p for p in (ROOT / folder).rglob("*") if p.is_file())
    for pattern in READER_GLOBS:
        files.update(p for p in ROOT.glob(pattern) if p.is_file())
    for path in sorted(files):
        relative = path.relative_to(ROOT)
        if SKIP_DIRS.intersection(relative.parts) or path.stat().st_size > 1_000_000:
            continue
        try:
            yield relative.as_posix(), path.read_text()
        except (UnicodeDecodeError, OSError):
            continue


def other_ticket_images(text):
    """Design image names a plan leaves to other tickets, and the declaration lines that name no ticket."""
    names, without_ticket = set(), []
    for match in OTHER_TICKET_IMAGES.finditer(text):
        if not re.search(r"T-\d+", match.group(0)):
            without_ticket.append(match.group(0).strip())
        names.update(name.removesuffix(".png") for name in re.findall(r"`([^`]+)`", match.group(2)))
    return names, without_ticket


def check(plan_path):
    errors, warnings = [], []
    text = plan_path.read_text()
    fields = front_matter(text)
    rows = file_list(text)
    listed = {path.rstrip("/") for _, path in rows}
    risk = fields.get("risk_level", "")

    if not rows:
        errors.append("mục 3.2 không có file nào, hoặc bảng không đúng mẫu (Thao tác | Đường dẫn | Nội dung).")
    for operation in unknown_operations(text):
        errors.append(f"thao tác '{operation}' trong mục 3.2 không hợp lệ; dùng Tạo, Sửa, Ghi lại hoặc Xóa.")

    created_by_dependencies = set()
    for dependency in plans_of_dependencies(fields):
        created_by_dependencies.update(p for op, p in file_list(dependency.read_text()) if op == "create")

    for operation, path in rows:
        target = ROOT / path
        if path.endswith("/"):
            continue
        if operation in ("change", "delete") and not target.exists():
            if path in created_by_dependencies:
                continue
            errors.append(f"{path}: plan sửa hoặc xóa, nhưng file chưa có trên nhánh chính.")
        if operation == "create" and target.exists():
            # Usually a generated file (design/review/) that the task overwrites; worth a look, not a stop.
            warnings.append(f"{path}: plan ghi Tạo, nhưng file đã có; task sẽ ghi đè. Nên ghi là Ghi lại.")

    for _, path in rows:
        if any(path.startswith(p) for p in NEVER):
            errors.append(f"{path}: không plan nào được chạm vào file này (CLAUDE.md, mục Không được làm).")
        elif any(path.startswith(p) for p in HIGH_RISK_PREFIXES) and risk != "high":
            errors.append(f"{path}: chạm điểm chặn của risk-scoring.md, mục 1, nhưng risk_level là '{risk}'.")

    changed = [path for operation, path in rows if operation != "create" and not path.endswith("/")]
    needles = {}
    for path in changed:
        names = [path]
        name = path.rsplit("/", 1)[-1]
        if Path(name).suffix in NAME_SEARCH_SUFFIXES and name not in GENERIC_NAMES:
            names.append(name)
        needles[path] = names
    for reader, content in reader_files():
        if reader in listed or reader in text or reader == SELF:
            continue
        for path, names in needles.items():
            if reader != path and any(name in content for name in names):
                errors.append(
                    f"{reader} nhắc tới {path} mà plan sửa, nhưng không có trong mục 3.2. "
                    f"Thêm vào danh sách file, hoặc ghi trong plan vì sao {reader} không bị ảnh hưởng (L-020).")

    if "design-compare.sh" in text:
        feature = plan_path.resolve().parent.parent.name
        images = sorted((ROOT / "docs" / "features" / feature / "design" / "screens").glob("*.png"))
        others, without_ticket = other_ticket_images(text)
        for line in without_ticket:
            errors.append(f"dòng '{line}' không ghi mã ticket vẽ các ảnh đó, ví dụ (T-014).")
        for name in sorted(others - {image.stem for image in images}):
            errors.append(f"ảnh thiết kế của ticket khác {name} không có trong docs/features/{feature}/design/screens/.")
        for image in images:
            if image.stem in others:
                continue
            expected = f"feature/{feature}/src/test/screenshots/{image.name}"
            if expected not in listed:
                errors.append(f"ảnh thiết kế {image.name} không có ảnh chuẩn screenshot {expected} trong mục 3.2. "
                              "Ảnh do ticket khác vẽ thì khai báo: 'Ảnh thiết kế của ticket khác (T-NNN): `Tên`'.")

    if fields.get("status") == "approved":
        for key in ("reviewer", "approved_at"):
            if not fields.get(key):
                errors.append(f"plan approved nhưng thiếu {key}.")
        if risk == "high" and not fields.get("cross_reviewer"):
            errors.append("plan rủi ro cao approved nhưng thiếu cross_reviewer.")
        decision = current_decision(text, fields.get("version", ""))
        if any(placeholder in decision for placeholder in DECISION_PLACEHOLDERS):
            errors.append("plan approved nhưng mục 9 (Quyết định duyệt) của phiên bản hiện tại còn chữ mẫu (L-023).")
    if not re.fullmatch(r"[\d.]+\s*[kKmM]?", fields.get("token_estimate", "")):
        warnings.append("thiếu token_estimate hoặc sai dạng (ví dụ 20M); xem scripts/token-usage.py estimate. "
                        "Chỉ để lập kế hoạch, không chặn task.")
    novelty = NOVELTY_SCORE.search(text)
    status = fields.get("status")
    if (novelty and int(novelty.group(1)) >= 2 and status in ("draft", "approved")
            and "scripts/check.sh" not in section(text, TRIAL_RUN_HEADING)):
        message = (f"tiêu chí E là {novelty.group(1)} nhưng mục {TRIAL_RUN_HEADING} không có hoặc không ghi lần chạy thử "
                   "bằng scripts/check.sh (L-001, L-010).")
        (errors if status == "draft" else warnings).append(message)
    if "## 4. Kiểm thử" not in text:
        warnings.append("không thấy mục 4. Kiểm thử.")
    return errors, warnings


def main(argv):
    if len(argv) < 2:
        print("Cách dùng: scripts/plan-check.py <plan.md> [<plan.md> ...]", file=sys.stderr)
        return 2
    failed = False
    for argument in argv[1:]:
        plan = Path(argument)
        if not plan.is_file():
            print(f"{argument}: LỖI: không có file này.")
            failed = True
            continue
        errors, warnings = check(plan)
        for message in errors:
            print(f"{argument}: LỖI: {message}")
        for message in warnings:
            print(f"{argument}: CHÚ Ý: {message}")
        if not errors:
            print(f"{argument}: qua.")
        failed = failed or bool(errors)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
