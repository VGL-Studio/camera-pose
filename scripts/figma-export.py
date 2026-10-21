#!/usr/bin/env python3
"""Builds the design handoff package of a feature from Figma, through the Figma REST API (ADR-0020).

Usage:
  FIGMA_TOKEN=... scripts/figma-export.py <figma link of a page, section or frame> <feature>
  scripts/figma-export.py --offline <nodes.json> <feature>      (tests: no network, context files only)

For every frame named like a screenshot image (<Name>Screen_<state>_<light|dark>, for example
LoadingScreen_failed_light; a state may have hyphens, like permission-denied) under the given node, it writes:
  docs/features/<feature>/design/screens/<frame>.png   PNG at 3x, checked to be 1233 x 2673 (frame 411 x 891)
Frames for an unfolded foldable phone end with _unfolded (CameraScreen_default_dark_unfolded): frame 673 x 841,
PNG 2019 x 2523.
  docs/features/<feature>/design/context/<frame>.md    sizes, spacing, colors, text, matched to tokens.json

Works on every Figma plan (the REST API is not limited like the MCP server). The token needs only the
file_content:read scope; it is read from the environment and never printed. Run it in a supervised session
only, never in an auto-run session. Messages are in Vietnamese for the team; comments are in English.
"""

import json
import os
import re
import struct
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
# Tests point this at a fixed copy, so a change of the design system never breaks them (L-020).
TOKENS = Path(os.environ.get("FIGMA_EXPORT_TOKENS", ROOT / "docs" / "design-system" / "tokens.json"))
FRAME_NAME = re.compile(r"^[A-Z][A-Za-z0-9]*Screen_[a-z0-9]+(?:-[a-z0-9]+)*_(light|dark)(_unfolded)?$")
# Standard phone frame, and the unfolded foldable frame (Compose Devices.FOLDABLE), in dp; images are 3x.
FRAME_SIZES = {"": (411, 891), "_unfolded": (673, 841)}


def frame_size(name):
    return FRAME_SIZES[FRAME_NAME.match(name).group(2) or ""]


def image_size(name):
    width, height = frame_size(name)
    return width * 3, height * 3
API = "https://api.figma.com/v1"


def fail(message):
    print(f"LỖI: {message}", file=sys.stderr)
    sys.exit(1)


def parse_link(link):
    match = re.search(r"figma\.com/(?:design|file)/([A-Za-z0-9]+)(?:/branch/([A-Za-z0-9]+))?", link)
    if not match:
        fail(f"không đọc được link Figma: {link}")
    file_key = match.group(2) or match.group(1)
    node = urllib.parse.parse_qs(urllib.parse.urlparse(link).query).get("node-id", [None])[0]
    if not node:
        fail("link cần có node-id: chọn trang, section hoặc frame trong Figma rồi bấm Copy link.")
    return file_key, node.replace("-", ":")


def get_json(url, token):
    request = urllib.request.Request(url, headers={"X-Figma-Token": token})
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        hint = {403: "token sai hoặc thiếu quyền file_content:read", 404: "không có file hay node này",
                429: "Figma giới hạn số lần gọi, thử lại sau"}.get(error.code, "")
        fail(f"Figma trả HTTP {error.code}. {hint}")


def download(url, path):
    with urllib.request.urlopen(url, timeout=120) as response:
        path.write_bytes(response.read())


def png_size(path):
    data = path.read_bytes()[:24]
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", data[16:24])


def find_frames(node, found):
    if node.get("type") in ("FRAME", "COMPONENT", "INSTANCE") and FRAME_NAME.match(node.get("name", "")):
        found.append(node)
        return found
    for child in node.get("children", []):
        find_frames(child, found)
    return found


class Tokens:
    """Reverse lookup from raw values to the names in tokens.json."""

    def __init__(self, tokens):
        self.colors = {}
        for theme, roles in tokens["color"].items():
            table = self.colors.setdefault(theme, {})
            for role, value in roles.items():
                table.setdefault(value.upper(), []).append(role)
        self.type = {}
        for name, style in tokens["typography"].items():
            key = (round(style["fontSizeSp"]), round(style["lineHeightSp"]), style["fontWeight"])
            self.type.setdefault(key, []).append(name)
        self.spacing = {value: name for name, value in tokens["spacingDp"].items()}

    def color(self, hex_value, theme):
        names = self.colors.get(theme, {}).get(hex_value.upper())
        return ", ".join(names) if names else "ngoài hệ thống"

    def text(self, size, line_height, weight):
        names = self.type.get((round(size), round(line_height), weight))
        return ", ".join(names) if names else "ngoài hệ thống"

    def space(self, value):
        value = round(value)
        if value == 0:
            return "0"
        name = self.spacing.get(value)
        return f"{value} ({name})" if name else f"{value} (ngoài hệ thống)"


def hex_color(paint):
    c = paint["color"]
    alpha = c.get("a", 1) * paint.get("opacity", 1)
    value = "#%02X%02X%02X" % tuple(round(c[k] * 255) for k in ("r", "g", "b"))
    return value if alpha >= 0.999 else f"{value} (độ mờ {alpha:.2f})"


def solid_paints(paints):
    return [p for p in paints or [] if p.get("type") == "SOLID" and p.get("visible", True)]


def describe(node, frame_box, tokens, theme, depth, lines, used):
    box = node.get("absoluteBoundingBox") or {}
    x = round(box.get("x", 0) - frame_box["x"])
    y = round(box.get("y", 0) - frame_box["y"])
    parts = [f'{node["type"].title()} "{node.get("name", "")}"', f"vị trí {x},{y}",
             f"cỡ {round(box.get('width', 0))} x {round(box.get('height', 0))}"]
    if node.get("type") == "TEXT":
        style = node.get("style", {})
        size, line = style.get("fontSize", 0), style.get("lineHeightPx", 0)
        weight = style.get("fontWeight", 400)
        type_name = tokens.text(size, line, weight)
        used.add(("kiểu chữ", f"{round(size)}/{round(line)}, đậm {weight}", type_name))
        parts.append(f'chữ "{node.get("characters", "")}"')
        parts.append(f"kiểu {type_name} ({round(size)}/{round(line)}, đậm {weight})")
        for paint in solid_paints(node.get("fills")):
            value = hex_color(paint)
            name = tokens.color(value[:7], theme)
            used.add(("màu chữ", value, name))
            parts.append(f"màu {value} ({name})")
    else:
        for paint in solid_paints(node.get("fills")):
            value = hex_color(paint)
            name = tokens.color(value[:7], theme)
            used.add(("màu nền", value, name))
            parts.append(f"nền {value} ({name})")
        for paint in solid_paints(node.get("strokes")):
            value = hex_color(paint)
            parts.append(f"viền {value} ({tokens.color(value[:7], theme)}), dày {node.get('strokeWeight', 1)}")
        if node.get("cornerRadius"):
            parts.append(f"bo góc {round(node['cornerRadius'])}")
        if node.get("layoutMode") in ("VERTICAL", "HORIZONTAL"):
            direction = "dọc" if node["layoutMode"] == "VERTICAL" else "ngang"
            padding = [node.get(k, 0) for k in ("paddingTop", "paddingRight", "paddingBottom", "paddingLeft")]
            gap = node.get("itemSpacing", 0)
            for value in padding + [gap]:
                if round(value):
                    used.add(("khoảng cách", str(round(value)), tokens.space(value).split(" ", 1)[1].strip("()")))
            parts.append(
                f"xếp {direction}, cách nhau {tokens.space(gap)}, lề trên/phải/dưới/trái "
                + "/".join(tokens.space(v) for v in padding)
                + f", căn {node.get('primaryAxisAlignItems', 'MIN')}/{node.get('counterAxisAlignItems', 'MIN')}"
            )
    lines.append("  " * depth + "- " + ", ".join(parts))
    for child in node.get("children", []):
        if child.get("visible", True):
            describe(child, frame_box, tokens, theme, depth + 1, lines, used)


def context_markdown(frame, link, tokens):
    theme = FRAME_NAME.match(frame["name"]).group(1)
    expected = frame_size(frame["name"])
    box = frame["absoluteBoundingBox"]
    lines, used = [], set()
    describe(frame, box, tokens, theme, 0, lines, used)
    size_note = "" if (round(box["width"]), round(box["height"])) == expected else \
        f" CHÚ Ý: khung không phải {expected[0]} x {expected[1]}."
    out = [f"# SỐ LIỆU THIẾT KẾ: {frame['name']}", "",
           "Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).", "",
           "## 1. Nguồn", "",
           f"- Link: {link}", f"- Frame: `{frame['id']}`, giao diện {'tối' if theme == 'dark' else 'sáng'}",
           f"- Ngày lấy: {date.today().isoformat()}{size_note}", "",
           "## 2. Giá trị dùng", "",
           "Đối chiếu với `docs/design-system/tokens.json` theo giá trị. \"ngoài hệ thống\" nghĩa là không có token",
           "nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.", "",
           "| Loại | Giá trị | Token |", "| --- | --- | --- |"]
    out += [f"| {kind} | {value} | {name} |" for kind, value, name in sorted(used)]
    out += ["", "## 3. Cấu trúc và số liệu", "",
            "Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.", ""]
    out += lines
    out += ["", "## 4. Code tham khảo", "", "Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.", ""]
    return "\n".join(out)


def main(argv):
    offline = len(argv) == 4 and argv[1] == "--offline"
    if not offline and len(argv) != 3:
        fail("cách dùng: scripts/figma-export.py <link Figma> <feature>")
    feature = argv[-1]
    if not re.match(r"^[a-z][a-z0-9-]*$", feature):
        fail(f"tên feature không hợp lệ: {feature}")
    tokens = Tokens(json.loads(TOKENS.read_text()))
    # Tests write into a temporary folder instead of docs/features.
    features_dir = Path(os.environ.get("FIGMA_EXPORT_FEATURES_DIR", ROOT / "docs" / "features"))
    design_dir = features_dir / feature / "design"

    if offline:
        link = "offline"
        document = json.loads(Path(argv[2]).read_text())
        file_key = token = None
    else:
        token = os.environ.get("FIGMA_TOKEN")
        if not token:
            fail("chưa có biến môi trường FIGMA_TOKEN (docs/ops/design-workflow.md, mục 4).")
        link = argv[1]
        file_key, node_id = parse_link(link)
        nodes = get_json(f"{API}/files/{file_key}/nodes?ids={urllib.parse.quote(node_id)}", token)
        entry = (nodes.get("nodes") or {}).get(node_id)
        if not entry:
            fail(f"không có node {node_id} trong file.")
        document = entry["document"]

    frames = find_frames(document, [])
    if not frames:
        fail("không có frame nào đặt tên dạng <Ten>Screen_<trạng thái>_<light|dark> dưới node này.")
    (design_dir / "context").mkdir(parents=True, exist_ok=True)
    for frame in frames:
        frame_link = link if offline else \
            f"https://www.figma.com/design/{file_key}/?node-id={frame['id'].replace(':', '-')}"
        (design_dir / "context" / f"{frame['name']}.md").write_text(context_markdown(frame, frame_link, tokens))
        print(f"context/{frame['name']}.md")

    if offline:
        return
    (design_dir / "screens").mkdir(parents=True, exist_ok=True)
    ids = ",".join(frame["id"] for frame in frames)
    images = get_json(f"{API}/images/{file_key}?ids={urllib.parse.quote(ids)}&format=png&scale=3", token)
    problems = []
    for frame in frames:
        url = (images.get("images") or {}).get(frame["id"])
        if not url:
            problems.append(f"{frame['name']}: Figma không xuất được ảnh")
            continue
        path = design_dir / "screens" / f"{frame['name']}.png"
        download(url, path)
        size = png_size(path)
        print(f"screens/{frame['name']}.png {size[0]} x {size[1]}" if size else f"screens/{frame['name']}.png")
        want = image_size(frame["name"])
        if size != want:
            box = frame_size(frame["name"])
            problems.append(f"{frame['name']}: ảnh {size}, cần {want} (khung phải là {box[0]} x {box[1]})")
    if problems:
        fail("; ".join(problems))


if __name__ == "__main__":
    main(sys.argv)
