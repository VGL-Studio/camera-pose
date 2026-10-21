"""Guard for ax.sh: decides whether a tap at a point is safe, and finds ad close buttons (docs/ops/clone-study.md).

A tap is refused when it would open an ad or start a purchase: a full-screen ad or a Play billing screen is on top,
the view under the point belongs to an ad, or it is a buy, subscribe or trial button (or "Continue" on a screen
that shows a price).

Usage:
  guard.py check <tree.xml> <top-activity-line> <x> <y>   exit 0 safe, 4 refused (reason on stderr)
  guard.py close <tree.xml>                               print "x y label" of a close button, exit 1 if none
  guard.py find <text> <tree.xml>                         centre "x y" of the best match by text or description,
                                                          preferring views that are not part of an ad; exit 1 if none
  guard.py status <tree.xml> <top-activity-line>          print ad markers on screen
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

SCREEN_AREA = int(os.environ.get("AX_WIDTH", "1080")) * int(os.environ.get("AX_HEIGHT", "2400"))
AD_ACTIVITY = re.compile(r"AdActivity|applovin|mbridge|bytedance|pangle|unity3d\.ads|ironsource|facebook\.ads|"
                         r"vungle|inmobi|chartboost|Interstitial|FullScreenAd|AppOpen", re.I)
AD_ID = re.compile(r"(^|[_.])(ad|ads|adview|ad_view|native_ad|nativead|banner_ad|ad_container|ad_frame|"
                   r"admob|adchoices|ad_choices|ad_media|ad_headline|ad_call_to_action|ad_body|ad_icon|ad_badge)"
                   r"([_.]|$)|nativeAd|adView|AdView|adContainer|bannerAd", re.I)
AD_TEXT = {"ad", "ads", "quảng cáo", "sponsored", "adchoices", "được tài trợ"}
# Purchase: Play billing screens, and buttons that start a purchase or a trial.
BILLING_ACTIVITY = re.compile(r"com\.android\.vending|ProxyBillingActivity|billing", re.I)
BUY_ID = re.compile(r"purchase|buy|subscri|pay_|_pay|trial|checkout", re.I)
BUY_TEXT = re.compile(r"buy|purchase|subscribe|start (free )?trial|free trial|dùng thử|mua|đăng ký|"
                      r"\d[\d.,]*\s*(₫|đ|\$|€|usd|vnd)|(₫|\$|€)\s*\d", re.I)
# These only open a paywall; on a paywall (a screen showing a price) they may start the purchase.
SOFT_ID = re.compile(r"unlock|upgrade|premium_btn|btn_premium|btn_pro|buttonPrimary", re.I)
SOFT_TEXT = re.compile(r"^(continue|tiếp tục|next|tiếp)$|unlock|upgrade|nâng cấp|get pro|go pro|go premium|get premium|"
                       r"mở khóa", re.I)
CLOSE_ID = re.compile(r"close|dismiss|skip|collap", re.I)
# Ad containers: tapping their empty area does not open the ad; only the asset views inside do.
CONTAINER_ID = re.compile(r"^(native_ad_view|fl_?native|ad_container|ad_frame|fr_?ads?|layout_ads?|ads?_layout|flNative)$", re.I)
CLOSE_TEXT = re.compile(r"^(x|×|✕|close|close ad|đóng|skip|skip ad|bỏ qua|interstitial close button)$", re.I)


def nodes(path):
    for n in ET.parse(path).getroot().iter("node"):
        b = [int(v) for v in re.findall(r"\d+", n.get("bounds", "[0,0][0,0]"))]
        yield n, b


def is_ad(n):
    rid = n.get("resource-id", "").split("/")[-1]
    cls = n.get("class", "")
    label = (n.get("text", "") or n.get("content-desc", "")).strip().lower()
    return bool(AD_ID.search(rid)) or "gms.ads" in cls or label in AD_TEXT


def ad_regions(path):
    """Bounds of views that open an ad when tapped: ad assets, and every visible view inside an ad container."""
    out = []
    order = [0]

    def walk(n, in_ad):
        order[0] += 1
        rid = n.get("resource-id", "").split("/")[-1]
        here = in_ad or is_ad(n)
        b = [int(v) for v in re.findall(r"\d+", n.get("bounds", "[0,0][0,0]"))]
        visible = len(b) == 4 and b[2] > b[0] and b[3] > b[1]
        if here and visible and not CONTAINER_ID.match(rid) and not CLOSE_ID.search(rid):
            label = (n.get("text", "") or n.get("content-desc", "")).strip()
            cls = n.get("class", "")
            if is_ad(n) or n.get("clickable") == "true" or label or "WebView" in cls or "Image" in cls:
                out.append(b + [order[0]])
        for c in n.findall("node"):
            walk(c, here)

    for top in ET.parse(path).getroot().findall("node"):
        walk(top, False)
    return out


def target_at(path, x, y):
    """The clickable view that receives a tap at (x, y): the last one in drawing order containing the point.
    Returns (rid, in_ad) or None."""
    found = None
    order = [0]

    def walk(n, in_ad):
        nonlocal found
        order[0] += 1
        here = in_ad or is_ad(n)
        b = [int(v) for v in re.findall(r"\d+", n.get("bounds", "[0,0][0,0]"))]
        if n.get("clickable") == "true" and len(b) == 4 and b[0] <= x < b[2] and b[1] <= y < b[3]:
            found = (n.get("resource-id", "").split("/")[-1], here, order[0])
        for c in n.findall("node"):
            walk(c, here)

    for top in ET.parse(path).getroot().findall("node"):
        walk(top, False)
    return found


PRICE = re.compile(r"\d[\d.,]*\s*(₫|đ|\$|€|usd|vnd)|(₫|\$|€)\s*\d|/\s*(week|month|year|tuần|tháng|năm)", re.I)


def buy_at(path, x, y):
    """A purchase or trial button (or a view showing a price) under the point, if any.
    "Continue", "Upgrade", "Unlock" count as purchase buttons only on a screen that shows a price (a paywall)."""
    all_nodes = list(nodes(path))
    paywall = any(PRICE.search((n.get("text", "") or n.get("content-desc", ""))) for n, _ in all_nodes)
    hit = None
    for n, b in all_nodes:
        if len(b) == 4 and b[0] <= x < b[2] and b[1] <= y < b[3]:
            rid = n.get("resource-id", "").split("/")[-1]
            label = (n.get("text", "") or n.get("content-desc", "")).strip()
            small = (b[2] - b[0]) * (b[3] - b[1]) < SCREEN_AREA * 0.25
            soft = SOFT_ID.search(rid) or (label and SOFT_TEXT.search(label))
            if small and (BUY_ID.search(rid) or (label and BUY_TEXT.search(label)) or (paywall and soft)):
                hit = rid or label
    return hit


def find(path, needle):
    """Exact label first, then whole word, then substring ("Agree" must not hit "Disagree"); at each level a view
    outside any ad wins over an ad view with the same label (an ad button may read "Settings" too)."""
    candidates = []

    def walk(n, in_ad):
        here = in_ad or is_ad(n)
        b = [int(v) for v in re.findall(r"\d+", n.get("bounds", "[0,0][0,0]"))]
        if len(b) == 4 and b[2] > b[0] and b[3] > b[1]:
            labels = [n.get("text", "").lower().strip(), n.get("content-desc", "").lower().strip()]
            candidates.append((labels, here, b))
        for c in n.findall("node"):
            walk(c, here)

    for top in ET.parse(path).getroot().findall("node"):
        walk(top, False)
    tests = (lambda t: t == needle,
             lambda t: re.search(r"(^|\W)" + re.escape(needle) + r"(\W|$)", t) is not None,
             lambda t: needle in t)
    for test in tests:
        for want_ad in (False, True):
            for labels, in_ad, b in candidates:
                if in_ad == want_ad and any(t and test(t) for t in labels):
                    return (b[0] + b[2]) // 2, (b[1] + b[3]) // 2
    return None


def fullscreen_ad(path, top):
    if AD_ACTIVITY.search(top):
        return "activity " + top.strip()
    for x1, y1, x2, y2, _ in ad_regions(path):
        if (x2 - x1) * (y2 - y1) > SCREEN_AREA * 0.4:
            return f"ad view covering [{x1},{y1}][{x2},{y2}]"
    return None


def main():
    cmd = sys.argv[1]
    if cmd == "check":
        path, top, x, y = sys.argv[2], sys.argv[3], int(sys.argv[4]), int(sys.argv[5])
        if AD_ACTIVITY.search(top):
            print(f"TỪ CHỐI: đang ở màn hình quảng cáo ({top.strip()}). Dùng 'ax.sh adclose' hoặc 'adcorner'.", file=sys.stderr)
            sys.exit(4)
        if BILLING_ACTIVITY.search(top):
            print(f"TỪ CHỐI: đang ở màn hình thanh toán ({top.strip()}). Chỉ được back.", file=sys.stderr)
            sys.exit(4)
        buy = buy_at(path, x, y)
        if buy:
            print(f"TỪ CHỐI: điểm ({x},{y}) là nút mua hay dùng thử ({buy}). Chỉ chụp và đóng màn hình trả phí.", file=sys.stderr)
            sys.exit(4)
        target = target_at(path, x, y)
        if target and target[1] and not CLOSE_ID.search(target[0]):
            print(f"TỪ CHỐI: điểm ({x},{y}) rơi vào phần tử quảng cáo '{target[0]}'.", file=sys.stderr)
            sys.exit(4)
        if target and CLOSE_ID.search(target[0]):
            return
        # A non-ad view drawn after (on top of) every ad view under the point receives the tap safely.
        for x1, y1, x2, y2, idx in ad_regions(path):
            if x1 - 20 <= x <= x2 + 20 and y1 - 20 <= y <= y2 + 20 and not (target and target[2] > idx):
                print(f"TỪ CHỐI: điểm ({x},{y}) nằm trong vùng quảng cáo [{x1},{y1}][{x2},{y2}].", file=sys.stderr)
                sys.exit(4)
        return
    if cmd == "close":
        best = None
        for n, (x1, y1, x2, y2) in nodes(sys.argv[2]):
            rid = n.get("resource-id", "").split("/")[-1]
            label = (n.get("text", "") or n.get("content-desc", "")).strip()
            if (CLOSE_ID.search(rid) or CLOSE_TEXT.match(label)) and x2 > x1 and y2 > y1:
                # Close buttons are small; skip big containers named like "close_layout".
                if (x2 - x1) * (y2 - y1) < 400 * 400:
                    best = ((x1 + x2) // 2, (y1 + y2) // 2, rid or label)
                    break
        if not best and fullscreen_ad(sys.argv[2], ""):
            width = int(os.environ.get("AX_WIDTH", "1080")); height = int(os.environ.get("AX_HEIGHT", "2400"))
            for n, (x1, y1, x2, y2) in nodes(sys.argv[2]):
                label = (n.get("text", "") or n.get("content-desc", "")).strip()
                corner = y2 < height * 0.15 and (x2 < width * 0.2 or x1 > width * 0.8)
                if n.get("clickable") == "true" and not label and corner and 0 < x2 - x1 < width * 0.12:
                    best = ((x1 + x2) // 2, (y1 + y2) // 2, "unlabeled-corner")
        if not best:
            sys.exit(1)
        print(*best)
        return
    if cmd == "find":
        needle, path = sys.argv[2].lower().strip(), sys.argv[3]
        if not needle:
            sys.exit(2)
        found = find(path, needle)
        if not found:
            sys.exit(1)
        print(*found)
        return
    if cmd == "status":
        path, top = sys.argv[2], sys.argv[3]
        print("fullscreen:", fullscreen_ad(path, top) or "no")
        for b in ad_regions(path):
            print("ad region:", b[:4])


main()
