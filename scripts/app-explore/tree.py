"""Prints a compact view of a uiautomator dump: one line per meaningful node, bounds in px and dp.

Usage:
  tree.py <tree.xml>                 one line per node: class, bounds in px, position and size in dp, flags, id, text
  tree.py --find <text> <tree.xml>   centre "x y" of the node whose text or description matches best
  tree.py --findid <id> <tree.xml>   centre "x y" of the first node with this resource-id (short name)
Density comes from AX_DENSITY (dpi, set by ax.sh from `adb shell wm density`); default 440.
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

DENSITY = int(os.environ.get("AX_DENSITY", "440")) / 160


def nodes(path):
    root = ET.parse(path).getroot()
    for n in root.iter("node"):
        yield n


def bounds(n):
    m = re.findall(r"\d+", n.get("bounds", "[0,0][0,0]"))
    return [int(v) for v in m]


def main():
    args = sys.argv[1:]
    if args and args[0] == "--find":
        needle, path = args[1].lower().strip(), args[2]
        # Exact text or description first, then whole word, then substring ("Agree" must not hit "Disagree").
        all_nodes = list(nodes(path))
        tests = (lambda t: t == needle,
                 lambda t: re.search(r"(^|\W)" + re.escape(needle) + r"(\W|$)", t) is not None,
                 lambda t: needle in t)
        for test in tests:
            for n in all_nodes:
                for t in (n.get("text", "").lower().strip(), n.get("content-desc", "").lower().strip()):
                    if t and test(t):
                        x1, y1, x2, y2 = bounds(n)
                        print((x1 + x2) // 2, (y1 + y2) // 2)
                        return
        sys.exit(1)
    if args and args[0] == "--findid":
        rid, path = args[1], args[2]
        if not rid:
            sys.exit(2)
        for n in nodes(path):
            if n.get("resource-id", "").split("/")[-1] == rid:
                x1, y1, x2, y2 = bounds(n)
                print((x1 + x2) // 2, (y1 + y2) // 2)
                return
        sys.exit(1)
    path = args[0]
    pkg = None
    for n in nodes(path):
        pkg = pkg or n.get("package")
        text, desc = n.get("text", ""), n.get("content-desc", "")
        rid = n.get("resource-id", "").split("/")[-1]
        click = n.get("clickable") == "true" or n.get("scrollable") == "true"
        if not (text or desc or rid or click):
            continue
        x1, y1, x2, y2 = bounds(n)
        if x2 - x1 <= 0 or y2 - y1 <= 0:
            continue
        cls = n.get("class", "").split(".")[-1]
        flags = "".join(f for f, on in (("C", n.get("clickable") == "true"), ("S", n.get("scrollable") == "true"),
                                          ("K", n.get("checked") == "true"), ("L", n.get("selected") == "true")) if on)
        dp = f"{x1/DENSITY:.0f},{y1/DENSITY:.0f} {(x2-x1)/DENSITY:.0f}x{(y2-y1)/DENSITY:.0f}dp"
        parts = [cls, f"[{x1},{y1}][{x2},{y2}]", dp]
        if flags: parts.append(flags)
        if rid: parts.append("#" + rid)
        if text: parts.append(repr(text[:80]))
        if desc: parts.append("desc=" + repr(desc[:80]))
        print(" ".join(parts))
    print(f"(package {pkg})")


main()
