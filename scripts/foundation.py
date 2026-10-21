#!/usr/bin/env python3
"""Writes docs/foundation.md, the short list of rules every task follows, from the ADRs in docs/adr/.

Usage: scripts/foundation.py [--check]
  (no option)  rewrite docs/foundation.md
  --check      exit 1 when docs/foundation.md is not what the ADRs say now (nothing is written)

An ADR holds two things: the rule ("Việc AI phải tuân thủ" in its Hệ quả section) and the reason (context, options,
consequences). The AI reads this page before every task and opens a full ADR only when a task touches its area or
would change its rule. ADRs from the template project carry `origin: template`; an app's own ADRs are numbered from
1001 and listed apart. Superseded ADRs are left out. FOUNDATION_ROOT overrides the repository root (tests).
The page is in Vietnamese for the team.
"""

import os
import re
import sys
from pathlib import Path

ROOT = Path(os.environ.get("FOUNDATION_ROOT", Path(__file__).resolve().parent.parent))
DOCS = ROOT / "docs"
OUTPUT = DOCS / "foundation.md"
RULE = re.compile(r"^- \*\*Việc AI phải tuân thủ:\*\*\s*(.*?)(?=^- \*\*|^#|\Z)", re.S | re.M)


def front_matter(text):
    match = re.match(r"^---\n(.*?)\n---\n", text, re.S)
    fields = {}
    for line in (match.group(1) if match else "").splitlines():
        key, _, value = line.partition(":")
        if key and not key.startswith((" ", "#")):
            fields[key.strip()] = value.split("#", 1)[0].strip()
    return fields


def split_rules(text):
    """One bullet per clause: the rule sections are written as clauses joined by semicolons."""
    text = " ".join(text.split())
    parts, depth, current = [], 0, ""
    for ch in text:
        if ch == "`":
            depth ^= 1
        if ch == ";" and not depth:
            parts.append(current.strip())
            current = ""
        else:
            current += ch
    parts.append(current.strip())
    return [p.rstrip(".") for p in parts if p]


def adrs():
    found = []
    for path in sorted((DOCS / "adr").glob("[0-9]*.md")):
        text = path.read_text()
        meta = front_matter(text)
        status = meta.get("status", "")
        if status == "superseded":
            continue
        rule = RULE.search(text)
        found.append({
            "id": meta.get("id", path.stem[:8]),
            "title": meta.get("title", ""),
            "status": status,
            "origin": meta.get("origin", "app"),
            "file": f"adr/{path.name}",
            "rules": split_rules(rule.group(1)) if rule else [],
        })
    return found


def section(items):
    lines = []
    for adr in items:
        lines.append(f"### [{adr['id']}]({adr['file']}): {adr['title']}")
        lines.append("")
        if adr["rules"]:
            lines.extend(f"- {r}" for r in adr["rules"])
        else:
            lines.append("- ADR chưa có mục \"Việc AI phải tuân thủ\": đọc ADR đầy đủ.")
        lines.append("")
    return lines


def render():
    items = adrs()
    lines = [
        "# QUY TẮC NỀN TẢNG",
        "",
        "Trang này do `scripts/foundation.py` sinh từ mục \"Việc AI phải tuân thủ\" của các ADR, không sửa tay. "
        "Muốn đổi một quy tắc thì sửa hoặc thay ADR, rồi chạy lại script.",
        "",
        "AI đọc trang này trước mọi task. Chỉ mở ADR đầy đủ (bối cảnh, phương án, hệ quả) khi task đụng vào vùng của "
        "ADR đó hoặc định đổi quy tắc của nó: lý do nằm ở đó.",
        "",
        "ADR có `origin: template` đến từ dự án mẫu: trong app, không sửa; muốn khác thì viết ADR riêng của app "
        "(đánh số từ 1001) ghi rõ thay quy tắc nào và vì sao, và đề xuất về dự án mẫu nếu có ích chung "
        "(`docs/ops/from-app.md`).",
        "",
    ]
    groups = [
        ("Quy tắc từ dự án mẫu", lambda a: a["origin"] == "template" and a["status"] == "accepted"),
        ("Quyết định riêng của app", lambda a: a["origin"] != "template" and a["status"] == "accepted"),
        ("Đang đề xuất, chưa duyệt chính thức (vẫn làm theo khi tài liệu quy trình dẫn tới)",
         lambda a: a["status"] == "proposed"),
    ]
    for title, keep in groups:
        chosen = [a for a in items if keep(a)]
        if not chosen:
            continue
        lines += [f"## {title}", ""] + section(chosen)
    return "\n".join(lines).rstrip("\n") + "\n"


def main(argv):
    page = render()
    if argv[1:] == ["--check"]:
        if not OUTPUT.is_file() or OUTPUT.read_text() != page:
            print("docs/foundation.md chưa khớp với các ADR. Chạy scripts/foundation.py rồi commit.")
            return 1
        print("docs/foundation.md khớp với các ADR.")
        return 0
    if argv[1:]:
        print(__doc__)
        return 2
    OUTPUT.write_text(page)
    print("Đã ghi docs/foundation.md.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
