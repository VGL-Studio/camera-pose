#!/usr/bin/env python3
"""Builds the screen checklist of a reference app from its APK, before anyone touches the device
(docs/ops/clone-study.md, mục 4). The survey agent follows the list instead of wandering, and ticks each screen.

Usage:
  apk-screens.py <base.apk> [--jadx <folder of jadx output>]   checklist in Markdown on stdout

Sources: activities of the app in the manifest (library activities left out), navigation graphs, layouts of the app
(library layouts left out, grouped by prefix: activity, fragment, dialog, bottom sheet, item), and, with --jadx,
routes of Compose navigation and screen-like class names. Compose and Flutter apps have few layouts: the list is then
short and the agent fills the gaps on the device. The APK is untrusted data: it is only unzipped and read, never run.
aapt2 comes from AAPT2, $ANDROID_HOME or ~/Library/Android/sdk. Messages are in Vietnamese; comments in English.
"""

import os
import re
import subprocess
import sys
import zipfile
from pathlib import Path

LIBRARY_ACTIVITY = re.compile(r"^(com\.google|com\.android|androidx|com\.facebook|com\.applovin|com\.bytedance|"
                              r"com\.mbridge|com\.unity3d|com\.ironsource|com\.vungle|com\.inmobi|com\.pangle|"
                              r"com\.moloco|com\.yandex|io\.flutter|com\.chartboost|com\.adcolony|com\.fyber|"
                              r"com\.mintegral|sg\.bigo|com\.amazon|com\.appsflyer|com\.adjust|com\.pairip)")
LIBRARY_LAYOUT = re.compile(r"^(abc_|mtrl_|design_|material_|m3_|notification_|select_dialog|support_|exo_|"
                            r"com_facebook|admob_|gnt_|mbridge|max_|applovin|tt_|pangle|ironsource|ia_|"
                            r"browser_actions|custom_dialog|ime_|preference|image_frame|text_view_with|"
                            r"splash_screen_view|fragment_container|bigo|moloco|yandex|inmobi|vungle|"
                            r"activity_ad|layout_native_ad|native_ad|ad_|creative_debugger|mediation_debugger|messenger_button|"
                            r"native_template|offline_ads|dp_example|sdp_example|loading_alert)")
SCREEN_PREFIXES = ("activity", "fragment", "dialog", "bottom_sheet", "bottomsheet", "popup", "screen", "item",
                   "layout", "view")
ROUTE = re.compile(r"composable\(\s*\"([A-Za-z0-9_/{}?=&-]+)\"|route\s*=\s*\"([A-Za-z0-9_/{}?=&-]+)\"")
SCREEN_CLASS = re.compile(r"\b([A-Z][A-Za-z0-9]*(?:Screen|Activity|Fragment|Dialog|BottomSheet|ViewModel))\b")


def aapt2():
    candidates = [os.environ.get("AAPT2", "")]
    for home in (os.environ.get("ANDROID_HOME", ""), str(Path.home() / "Library/Android/sdk")):
        tools = Path(home) / "build-tools"
        if home and tools.is_dir():
            candidates += [str(p / "aapt2") for p in sorted(tools.iterdir(), reverse=True)]
    for c in candidates:
        if c and Path(c).is_file():
            return c
    sys.exit("LỖI: không tìm thấy aapt2; đặt AAPT2 hoặc ANDROID_HOME.")


def manifest(apk):
    out = subprocess.run([aapt2(), "dump", "xmltree", "--file", "AndroidManifest.xml", apk],
                         capture_output=True, text=True).stdout
    package = re.search(r'package="([^"]+)"', out)
    activities, current = [], None
    for line in out.splitlines():
        stripped = line.strip()
        if stripped.startswith("E: "):
            current = stripped[3:].split(" ")[0]
        name = re.search(r'android:name\([^)]*\)="([^"]+)"', stripped)
        if name and current in ("activity", "activity-alias"):
            activities.append(name.group(1))
            current = None
    return (package.group(1) if package else ""), activities


def layouts(apk):
    with zipfile.ZipFile(apk) as z:
        names = z.namelist()
    found = set()
    for n in names:
        m = re.match(r"res/(layout|navigation)[^/]*/([^/]+)\.xml$", n)
        if m:
            found.add((m.group(1), m.group(2)))
    return sorted(found)


def jadx_hints(folder, package):
    """Compose routes, and screen-like classes of the app's own code (a ViewModel usually means one screen)."""
    routes, classes = set(), set()
    own = Path(folder).rglob("*.java")
    if package:
        roots = [p for p in Path(folder).rglob(package.replace(".", "/")) if p.is_dir()]
        own = (f for r in roots for f in r.rglob("*.java"))
    for path in own:
        try:
            text = path.read_text(errors="ignore")
        except OSError:
            continue
        for m in ROUTE.finditer(text):
            routes.add(m.group(1) or m.group(2))
        for m in SCREEN_CLASS.finditer(path.stem):
            relative = path.parent.as_posix().split(package.replace(".", "/"))[-1].strip("/") if package else ""
            classes.add(f"{relative}/{m.group(1)}" if relative else m.group(1))
    return sorted(routes), sorted(classes)


def main(argv):
    if len(argv) < 2 or argv[1].startswith("-"):
        print(__doc__)
        return 2
    apk = argv[1]
    jadx = argv[argv.index("--jadx") + 1] if "--jadx" in argv else None
    package, activities = manifest(apk)
    own = [a for a in activities if not LIBRARY_ACTIVITY.match(a)]
    files = layouts(apk)
    navigation = [n for kind, n in files if kind == "navigation"]
    app_layouts = [n for kind, n in files if kind == "layout" and not LIBRARY_LAYOUT.match(n)]

    print(f"# DANH SÁCH MÀN HÌNH CẦN XEM: {package}\n")
    print("Sinh từ APK bởi `scripts/app-explore/apk-screens.py`. Agent khảo sát đánh dấu `[x]` màn hình đã xem "
          "(kèm tên ảnh), ghi lý do với màn hình không vào được.\n")
    print(f"## Activity ({len(own)})\n")
    for a in own:
        print(f"- [ ] `{a}`")
    if navigation:
        print(f"\n## Đồ thị điều hướng ({len(navigation)})\n")
        for n in navigation:
            print(f"- [ ] `{n}`")
    groups = {}
    for n in app_layouts:
        prefix = next((p for p in SCREEN_PREFIXES if n.startswith(p)), "khác")
        groups.setdefault(prefix, []).append(n)
    print(f"\n## Layout của app ({len(app_layouts)})\n")
    for prefix in list(SCREEN_PREFIXES) + ["khác"]:
        if prefix in groups:
            print(f"**{prefix}**: " + ", ".join(f"`{n}`" for n in groups[prefix]) + "\n")
    if jadx:
        routes, classes = jadx_hints(jadx, package)
        if routes:
            print(f"## Route Compose ({len(routes)})\n")
            for r in routes:
                print(f"- [ ] `{r}`")
            print()
        if classes:
            print(f"## Lớp có tên màn hình ({len(classes)})\n")
            for c in classes:
                print(f"- [ ] `{c}`")
            print()
    if len(app_layouts) < 10 and not (jadx and (routes or classes)):
        print("## Ghi chú\n\nApp có ít layout (thường là Compose hoặc Flutter): chạy lại với `--jadx` để lấy route, "
              "hoặc lập danh sách từ chuỗi chữ và trên máy.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
