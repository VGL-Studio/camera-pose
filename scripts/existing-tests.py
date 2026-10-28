#!/usr/bin/env python3
"""Lists the existing tests that may check behaviour a plan changes, for section 3.5 of the plan (ADR-0031).

Usage: scripts/existing-tests.py [--all] <plan.md>
       scripts/existing-tests.py [--all] --files <source.kt> [<source.kt> ...]

For every Kotlin source file the plan changes (rows "Sửa" or "Xóa" of section 3.2, outside test source sets), the
script collects the names other code can reach: non-private classes, objects, functions and constants declared in
the file, and the test tags the file places (testTag(X.Y) or testTag("y")). It then lists every test function in
the repository's test source sets that uses one of those names, and marks the ones that measure position or size
(bounds, width, height), because a layout change breaks those first (L-106: T-041 stopped on such a test).
Tests that use a name the plan writes in backticks and that the plan does not name yet come first; the others are
only counted unless --all is given.

The list is a starting point for the person or AI writing the plan, not a verdict: they read each test and write in
section 3.5 whether its assertions stay true, or which ones change and how. Nothing is run; exit 0 unless the input
cannot be read.
"""
import os
import re
import sys
from pathlib import Path

ROOT = Path(os.environ.get("EXISTING_TESTS_ROOT", Path(__file__).resolve().parent.parent))
TEST_SET = re.compile(r"/src/(test|androidTest)[A-Za-z]*/")
OPERATIONS = {"sửa": "change", "ghi lại": "change", "xóa": "delete", "xoá": "delete"}
# Top-level or member declarations (indent of at most 4 spaces); local functions and plain vals are left out.
DECLARATION = re.compile(
    r"^(?P<prefix>(?: {0,4})(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|internal|private|protected|data|sealed|enum|"
    r"abstract|open|inline|value|override|suspend|const|annotation)\s+)*)"
    r"(?P<kind>class|object|interface|fun|val)\s+(?:<[^>]+>\s*)?(?:[\w.]+\.)?(?P<name>[A-Za-z_]\w*)",
    re.M)
TAG_REFERENCE = re.compile(r"testTag\(\s*(?:\w+\.)*?((?:[A-Z]\w*\.)?\w+)\s*\)")
TOP_LEVEL = re.compile(r"^(?:@\w+(?:\([^)]*\))?\s+|[a-z]+\s+)*(fun|object|class|interface)\s+(?:<[^>]+>\s*)?(\w+)")
TAG_LITERAL = re.compile(r'testTag\(\s*"([^"]+)"\s*\)')
TEST_FUNCTION = re.compile(r"@Test\b[^\n]*\n(?:\s*@[^\n]*\n)*\s*fun\s+`?([^`(\s]+)`?\s*\(")
MEASURES = re.compile(r"[Bb]ounds|getBoundsIn|assert(?:Width|Height|TopPosition|LeftPosition|Position)|"
                      r"\.(?:width|height|top|bottom|left|right)\b|IsEqualTo\(.*dp")
# Names too common to say anything about a test.
GENERIC = {"Content", "Screen", "Route", "invoke", "TAG", "Companion", "value", "modifier", "state", "uiState"}


def plan_files(plan):
    """Kotlin source files (not tests) changed or deleted in section 3.2 of the plan."""
    text = plan.read_text()
    match = re.search(r"^### 3\.2\..*?$(.*?)(?=^##)", text, re.S | re.M)
    files = []
    for line in (match.group(1) if match else "").splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 2 or cells[0].lower() not in OPERATIONS:
            continue
        for path in re.findall(r"`([^`]+\.kt)`", cells[1]):
            if not TEST_SET.search("/" + path):
                files.append(path)
    return files


def enclosing(text):
    """(kind, name) of the last top-level declaration in the text, or None."""
    found = [m.groups() for line in text.splitlines() if (m := TOP_LEVEL.match(line))]
    return found[-1] if found else None


def reachable_names(source):
    """Names declared in the file that other files can use, and test tags the file places."""
    names = set()
    for match in DECLARATION.finditer(source):
        if "private" in match.group("prefix") or len(match.group("name")) < 3:
            continue
        if match.group("kind") == "val" and "const" not in match.group("prefix"):
            continue
        name = match.group("name")
        if match.group("prefix").startswith(" "):
            owner = enclosing(source[:match.start()])
            if owner is None or owner[0] == "fun":
                continue  # local to a function
            if match.group("kind") == "val":
                # A constant of an object (test tags): "CTA" alone would match the CTA of every other tag object.
                name = f"{owner[1]}.{name}"
        names.add(name)
    names.update(TAG_REFERENCE.findall(source))
    names.update(TAG_LITERAL.findall(source))
    return names - GENERIC


def word(name):
    return re.compile(rf"(?<![\w$]){re.escape(name)}(?![\w$])")


def test_files():
    for path in sorted(ROOT.rglob("*.kt")):
        relative = "/" + path.relative_to(ROOT).as_posix()
        if TEST_SET.search(relative) and "/build/" not in relative:
            yield path


def test_functions(text):
    """(name, body) of every @Test function; a body runs to the next @Test."""
    starts = list(TEST_FUNCTION.finditer(text))
    for index, match in enumerate(starts):
        end = starts[index + 1].start() if index + 1 < len(starts) else len(text)
        yield match.group(1), text[match.end():end]


def report(files, plan_text=None, show_all=False):
    names = {}
    for path in files:
        target = ROOT / path
        if not target.is_file():
            print(f"{path}: không có file này trên nhánh hiện tại, bỏ qua.")
            continue
        for name in reachable_names(target.read_text()):
            names.setdefault(name, set()).add(path)
    if not names:
        print("Không có tên nào để tìm (plan không sửa file Kotlin ngoài test).")
        return
    # Names the plan writes in backticks are the parts it changes; without a plan every name counts.
    written = set(re.findall(r"`([A-Za-z_][\w.]*)`", plan_text)) if plan_text is not None else None
    mentioned = {n for n in names if written is None or n in written or n.rsplit(".", 1)[-1] in written}
    hits = []
    for test_file in test_files():
        text = test_file.read_text()
        patterns = {name: word(name) for name in names}
        for name in names:
            # "Owner.NAME" is used bare only where the test imports it.
            if "." in name and re.search(rf"^import\s+[\w.]*\b{re.escape(name)}\s*$", text, re.M):
                patterns[name] = word(name.rsplit(".", 1)[-1])
        if not any(p.search(text) for p in patterns.values()):
            continue
        relative = test_file.relative_to(ROOT).as_posix()
        for function, body in test_functions(text):
            used = sorted(name for name, pattern in patterns.items() if pattern.search(body))
            if not used:
                continue
            named = plan_text is not None and function in plan_text
            hits.append((relative, function, bool(MEASURES.search(body)), used, named,
                         sorted(set(used) & mentioned)))
    first = [h for h in hits if h[2] and h[5] and not h[4]]
    second = [h for h in hits if not h[2] and h[5] and not h[4]]
    named = [h for h in hits if h[4]]
    rest = [h for h in hits if not h[5] and not h[4]]
    print(f"File của plan: {', '.join(files)}")
    print(f"Test có sẵn dùng tên hay tag của các file đó: {len(hits)}.")
    table("Cần xem trước: đo vị trí hay kích thước, dùng tên mà plan nêu, plan chưa nhắc tới test", first)
    table("Nên xem: dùng tên mà plan nêu, plan chưa nhắc tới test", second)
    if named:
        print(f"\nPlan đã nhắc tới {len(named)} test trong số này.")
    if show_all:
        table("Còn lại: chỉ dùng tên mà plan không nêu", rest)
    elif rest:
        print(f"Còn {len(rest)} test chỉ dùng tên mà plan không nêu; xem bằng --all.")


def table(title, hits):
    print(f"\n{title}: {len(hits)}")
    if not hits:
        return
    print()
    print("| Test | Đo vị trí, kích thước | Tên plan nêu | Dùng |")
    print("| --- | --- | --- | --- |")
    for relative, function, measures, used, _, plan_names in sorted(hits, key=lambda h: (h[0], h[1])):
        shown = ", ".join(used[:6]) + (", ..." if len(used) > 6 else "")
        print(f"| `{Path(relative).stem}.{function}` | {'có' if measures else ''} | {', '.join(plan_names)} | {shown} |")


def main(argv):
    show_all = "--all" in argv
    argv = [a for a in argv if a != "--all"]
    if len(argv) >= 3 and argv[1] == "--files":
        report(argv[2:], show_all=show_all)
        return 0
    if len(argv) != 2:
        print(__doc__.split("\n\n")[1], file=sys.stderr)
        return 2
    plan = Path(argv[1])
    if not plan.is_file():
        print(f"{argv[1]}: không có file này.", file=sys.stderr)
        return 2
    report(plan_files(plan), plan.read_text(), show_all)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
