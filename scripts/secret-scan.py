#!/usr/bin/env python3
"""Fails when a tracked file holds something that looks like a real secret or a real ad ID (ADR-0013, ADR-0021).

Usage: scripts/secret-scan.py
  Scans the files tracked by git (SECRET_SCAN_ROOT: scan every file under that folder instead; tests).
  Exit 0: nothing found. Exit 1: one line per finding, with the value masked.

What counts: AdMob app and ad unit IDs (ca-app-pub-<16 digits>), Google API keys (AIza...), AppLovin SDK keys
(exactly 86 characters of [A-Za-z0-9_-] on a line that mentions applovin, plan T-034: the key is entered only in the
AdMob ad source mapping, never in the repo), private keys, GitHub tokens. Allowed: Google's own test ad IDs
(publisher 3940256099942544), the obviously fake publishers 1234567890123456 and 0000000000000000 used by script
tests and plans, and keys that say FAKE (the staging google-services.json, script tests). The typical leak
this catches: a survey report of a reference app quoting a log line with that app's ad ID (docs/ops/clone-study.md).
Messages are in Vietnamese for the team; comments are in English.
"""

import os
import re
import subprocess
import sys
from pathlib import Path

PATTERNS = [
    ("mã AdMob", re.compile(r"ca-app-pub-(\d{16})[~/]\d{6,}")),
    ("khóa API của Google", re.compile(r"AIza[0-9A-Za-z_\-]{35}")),
    ("khóa SDK AppLovin", re.compile(r"(?<![A-Za-z0-9_\-])[A-Za-z0-9_\-]{86}(?![A-Za-z0-9_\-])")),
    ("khóa riêng", re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----")),
    ("token GitHub", re.compile(r"\bgh[pousr]_[A-Za-z0-9]{36}\b")),
]
ALLOWED_PUBLISHERS = {"3940256099942544", "1234567890123456", "0000000000000000"}
# A pattern listed here only applies to lines that match the given expression.
LINE_FILTERS = {"khóa SDK AppLovin": re.compile(r"applovin", re.IGNORECASE)}


def files():
    root = os.environ.get("SECRET_SCAN_ROOT")
    if root:
        return [p for p in Path(root).rglob("*") if p.is_file() and ".git" not in p.parts]
    top = Path(__file__).resolve().parent.parent
    names = subprocess.run(["git", "ls-files", "-z"], cwd=top, capture_output=True, text=True).stdout.split("\0")
    return [top / n for n in names if n]


def allowed(kind, match):
    if kind == "mã AdMob":
        return match.group(1) in ALLOWED_PUBLISHERS
    if kind in ("khóa API của Google", "khóa SDK AppLovin"):
        return "FAKE" in match.group(0)
    return False


def main():
    findings = []
    for path in files():
        try:
            text = path.read_text(errors="strict")
        except (UnicodeDecodeError, OSError):
            continue  # binary files
        for number, line in enumerate(text.splitlines(), 1):
            for kind, pattern in PATTERNS:
                line_filter = LINE_FILTERS.get(kind)
                if line_filter and not line_filter.search(line):
                    continue
                for match in pattern.finditer(line):
                    if not allowed(kind, match):
                        value = match.group(0)
                        findings.append(f"{path}:{number}: {kind} {value[:12]}<đã che>")
    if findings:
        print("LỖI: có giá trị giống bí mật hay mã quảng cáo thật trong repo. Che hoặc xóa trước khi commit:")
        print("\n".join(findings))
        return 1
    print("Không thấy bí mật hay mã quảng cáo thật.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
