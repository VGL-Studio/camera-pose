#!/usr/bin/env bash
# Drives a reference app on a device for the AI study of apps to clone (docs/ops/clone-study.md, ADR-0021).
# Every tap goes through guard.py: taps that would open an ad or start a purchase are refused, and a jump to the
# Play Store, a browser or a mail app (an ad click, a "rate us" link) is undone and recorded in incidents.log.
#
# Usage:
#   ax.sh check                         device present, accepts input (Xiaomi needs "USB debugging (Security
#                                       settings)"), and online
#   ax.sh shot <dir> <name>             screenshot (full + preview, longest side 1000 px) and compact UI tree
#   ax.sh tree                          compact UI tree of the current screen, then ad status
#   ax.sh tap <x> <y>                   tap at pixel coordinates (guarded)
#   ax.sh taptext <text>                tap the best match by text or description, ad views last (guarded)
#   ax.sh tapid <resource-id>           tap the first node with this resource-id, short name (guarded)
#   ax.sh drag <x1> <y1> <x2> <y2> [ms] slow drag; the start point is guarded and kept off the screen edges
#   ax.sh swipe up|down|left|right
#   ax.sh adclose                       find and tap the close or collapse button of an ad
#   ax.sh adcorner tl|tr                tap the top-left or top-right corner, for ad close buttons not in the tree
#   ax.sh adwait [max s]                wait until an ad close button shows, print the seconds, tap it
#   ax.sh type <text>                   type ASCII text into the focused field
#   ax.sh wait <s>                      sleep (useful inside seq)
#   ax.sh back | home | wake | launch <pkg> | stop <pkg> | top
#   ax.sh log <pkg> [lines]             recent log lines of the app's process
#   ax.sh newfiles [--reset]            photos and files added under Pictures/ and DCIM/ since the first check, for
#                                       the person to clean up (nothing is deleted); --reset takes a new snapshot
#   ax.sh color <png> <x> <y> ...       hex colour of pixels of a full screenshot
# Several steps in one call (one AI turn instead of one per step); stops at the first failing step:
#   ax.sh seq "tapid tab_tips; wait 2; shot captures/pkg 12-tips; back"
#   ax.sh scrollshots <dir> <prefix> [max]   shoot, scroll down, repeat until the screen stops changing
#   ax.sh scrollcollect [max]                print the texts of a scrolling list, no screenshots
#
# Exit codes: 0 done, 1 not found, 2 usage, 3 input refused by the device, 4 tap refused by the guard,
# 5 no device, 6 left the app (undone), 7 screen locked, 8 offline.
# Settings: ANDROID_SERIAL (several devices), AX_INCIDENTS (incident log, default ./incidents.log in the study
# folder), AX_SETTLE (longest wait after a tap, default 1.2 s), AX_FILES (snapshot of the device's photo folders,
# default ./device-files-before.txt). Messages are in Vietnamese; comments in English.
set -euo pipefail
TOOLS="$(cd "$(dirname "$0")" && pwd)"
LOG="${AX_INCIDENTS:-$PWD/incidents.log}"
CUR="${TMPDIR:-/tmp}/ax_cur_$$.xml"
FILES="${AX_FILES:-$PWD/device-files-before.txt}"
trap 'rm -f "$CUR" "$CUR.prev"' EXIT

need_device() {
  [[ "$(adb get-state 2>/dev/null || true)" == "device" ]] || { echo "LỖI: không thấy thiết bị (adb). Chờ rồi thử lại." >&2; exit 5; }
}
# Screen size and density, for the dp values in the tree, the size rules of the guard and the swipe points.
screen_env() {
  local size density
  size="$({ adb shell wm size 2>/dev/null || true; } | tr -d '\r' | sed -n 's/.*: *\([0-9]*x[0-9]*\).*/\1/p' | tail -n 1)"
  density="$({ adb shell wm density 2>/dev/null || true; } | tr -d '\r' | sed -n 's/.*: *\([0-9]*\).*/\1/p' | tail -n 1)"
  export AX_WIDTH="${size%x*}" AX_HEIGHT="${size#*x}" AX_DENSITY="${density:-440}"
  [[ -n "${AX_WIDTH}" && "${AX_WIDTH}" != "${size}" ]] || export AX_WIDTH=1080 AX_HEIGHT=2400
}
# adb shell exits 0 even when the device refuses injected input, so read the output.
run_input() {
  local out; out="$(adb shell input "$@" 2>&1 || true)"
  if grep -q "Exception" <<<"$out"; then
    echo "LỖI: máy từ chối thao tác giả lập (input $*). Xiaomi: bật Tùy chọn nhà phát triển > Gỡ lỗi USB (Cài đặt bảo mật)." >&2
    exit 3
  fi
}
dump() { adb shell uiautomator dump /sdcard/ax.xml > /dev/null 2>&1 || true; adb exec-out cat /sdcard/ax.xml 2>/dev/null || true; }
device_files() { { adb shell find /sdcard/Pictures /sdcard/DCIM -type f 2>/dev/null || true; } | tr -d '\r' | sort; }
top_line() { { adb shell dumpsys activity activities 2>/dev/null || true; } | grep -m1 -E "topResumedActivity|mResumedActivity" | sed 's/^ *//' || true; }
# After a tap, undo a jump to the store, a browser or a mail app, and record it. Checks a few times within
# AX_SETTLE instead of one fixed sleep, so a normal tap costs less than a second.
after_tap() {
  local t i steps
  steps="$(python3 -c "print(max(1, round(${AX_SETTLE:-1.2} / 0.4)))")"
  for ((i = 0; i < steps; i++)); do
    sleep 0.4
    t="$(top_line)"
    if grep -qE "com.android.vending|chrome|browser|com.mi.globalbrowser|webview|com.google.android.gm|ProxyBilling" <<<"$t"; then
      run_input keyevent 4
      echo "$(date '+%F %T') rời app sau khi chạm $1: $t (đã back)" >> "$LOG"
      echo "CẢNH BÁO: chạm $1 đã mở app khác ($t); đã back. Ghi vào báo cáo." >&2
      exit 6
    fi
  done
}
# One UI dump per tap: the caller may pass the dump it already read to find the target.
guarded_tap() {
  local x="$1" y="$2" have_dump="${3:-}"
  [[ -n "$have_dump" ]] || dump > "$CUR"
  python3 -I "$TOOLS/guard.py" check "$CUR" "$(top_line)" "$x" "$y" || exit 4
  run_input tap "$x" "$y"; echo "tapped $x $y"
  after_tap "($x,$y)"
}
# Fingerprint of a screen's content, to tell when scrolling stops changing it.
tree_signature() {
  python3 -I "$TOOLS/tree.py" "$1" | grep -v '^(package' |
    python3 -I -c 'import hashlib, sys; print(hashlib.md5(sys.stdin.buffer.read()).hexdigest())'
}

run_cmd() {
  local cmd="$1"; shift
  case "$cmd" in
    check)
      run_input keyevent 0; echo "input: ok"
      net="$({ adb shell dumpsys connectivity 2>/dev/null || true; } | tr -d '\r' | sed -n 's/^ *Active default network: *//p' | head -n 1)"
      if [[ "${net:-none}" == "none" ]]; then
        echo "CHÚ Ý: máy không có mạng. Quảng cáo và nội dung tải về sẽ không hiện; nhờ người bật mạng trước khi khảo sát." >&2
        exit 8
      fi
      echo "network: ok"
      # The first check of a study remembers which photos were already there (ax.sh newfiles).
      [[ -f "$FILES" ]] || { device_files > "$FILES"; echo "đã ghi danh sách ảnh có sẵn trên máy: $FILES"; } ;;
    newfiles)
      if [[ "${1:-}" == "--reset" ]]; then device_files > "$FILES"; echo "đã ghi lại danh sách: $FILES"; exit 0; fi
      [[ -f "$FILES" ]] || { echo "chưa có danh sách ban đầu: chạy 'ax.sh check' lúc bắt đầu khảo sát" >&2; exit 1; }
      local added; added="$(comm -13 "$FILES" <(device_files))"
      if [[ -z "$added" ]]; then echo "không có file mới trên máy"; else echo "file mới trên máy, cần người dọn:"; echo "$added"; fi ;;
    shot)
      local out="$1" name="$2" w
      mkdir -p "$out/full" "$out/preview" "$out/tree"
      for _ in 1 2 3; do
        adb exec-out screencap -p > "$out/full/$name.png"
        w="$(sips -g pixelWidth "$out/full/$name.png" 2>/dev/null | awk '/pixelWidth/{print $2}' || true)"
        # A capture taken during a rotation comes out landscape; take it again.
        [[ -z "$w" || "$w" == "$AX_WIDTH" ]] && break; sleep 1
      done
      python3 -P -c 'import sys
from PIL import Image
im = Image.open(sys.argv[1]); im.thumbnail((1000, 1000)); im.save(sys.argv[2])
if im.convert("L").getextrema()[1] < 8:
    print("CHÚ Ý: ảnh đen toàn bộ, có thể màn hình chặn chụp (FLAG_SECURE); dựa vào cây giao diện.")' \
        "$out/full/$name.png" "$out/preview/$name.png"
      dump > "$out/tree/$name.xml"
      python3 -I "$TOOLS/tree.py" "$out/tree/$name.xml" | tee "$out/tree/$name.txt"
      python3 -I "$TOOLS/guard.py" status "$out/tree/$name.xml" "$(top_line)"
      echo "preview: $out/preview/$name.png" ;;
    tree) dump > "$CUR"; python3 -I "$TOOLS/tree.py" "$CUR"; python3 -I "$TOOLS/guard.py" status "$CUR" "$(top_line)" ;;
    tap) guarded_tap "$1" "$2" ;;
    taptext|tapid)
      [[ -n "${1:-}" ]] || { echo "thiếu chữ hay id để chạm" >&2; exit 2; }
      dump > "$CUR"
      local xy x y
      if [[ "$cmd" == "tapid" ]]; then
        xy="$(python3 -I "$TOOLS/tree.py" --findid "$1" "$CUR")" || { echo "không thấy: $1"; exit 1; }
      else
        xy="$(python3 -I "$TOOLS/guard.py" find "$1" "$CUR")" || { echo "không thấy: $1"; exit 1; }
      fi
      read -r x y <<<"$xy"; guarded_tap "$x" "$y" reuse ;;
    drag)
      local edge=$((AX_WIDTH / 9))
      if (( $1 < edge || $1 > AX_WIDTH - edge || $2 > AX_HEIGHT - edge )); then
        echo "TỪ CHỐI: điểm bắt đầu kéo quá sát mép màn hình; máy sẽ hiểu là cử chỉ quay lại hay về màn hình chính." >&2
        exit 4
      fi
      dump > "$CUR"
      python3 -I "$TOOLS/guard.py" check "$CUR" "$(top_line)" "$1" "$2" || exit 4
      run_input swipe "$1" "$2" "$3" "$4" "${5:-800}"; after_tap "drag" ;;
    swipe)
      local cx=$((AX_WIDTH / 2)) cy=$((AX_HEIGHT / 2)) top=$((AX_HEIGHT * 3 / 10)) bottom=$((AX_HEIGHT * 3 / 4))
      local left=$((AX_WIDTH / 6)) right=$((AX_WIDTH * 5 / 6))
      case "${1:-}" in
        up) run_input swipe "$cx" "$bottom" "$cx" "$top" 300 ;;
        down) run_input swipe "$cx" "$top" "$cx" "$bottom" 300 ;;
        left) run_input swipe "$right" "$cy" "$left" "$cy" 300 ;;
        right) run_input swipe "$left" "$cy" "$right" "$cy" 300 ;;
        *) echo "up|down|left|right" >&2; exit 2 ;;
      esac ;;
    adclose)
      dump > "$CUR"
      local found x y label
      found="$(python3 -I "$TOOLS/guard.py" close "$CUR")" ||
        { echo "không thấy nút đóng trong cây giao diện; xem ảnh rồi dùng 'ax.sh adcorner tl|tr'"; exit 1; }
      read -r x y label <<<"$found"
      run_input tap "$x" "$y"; echo "đã đóng bằng $label tại $x $y"; after_tap "adclose" ;;
    adwait)
      local max="${1:-30}" start found x y label
      start="$(date +%s)"
      while true; do
        dump > "$CUR"
        if found="$(python3 -I "$TOOLS/guard.py" close "$CUR")"; then
          read -r x y label <<<"$found"
          echo "nút đóng hiện sau khoảng $(( $(date +%s) - start )) giây ($label)"
          run_input tap "$x" "$y"; after_tap "adwait"; break
        fi
        (( $(date +%s) - start < max )) || { echo "không thấy nút đóng sau $max giây"; exit 1; }
        sleep 1
      done ;;
    adcorner)
      local x y
      case "${1:-}" in
        tl) x=$((AX_WIDTH * 7 / 100)); y=$((AX_HEIGHT * 8 / 100)) ;;
        tr) x=$((AX_WIDTH * 93 / 100)); y=$((AX_HEIGHT * 8 / 100)) ;;
        *) echo "tl|tr" >&2; exit 2 ;;
      esac
      run_input tap "$x" "$y"; echo "đã chạm góc $1"; after_tap "adcorner $1" ;;
    type)
      # input text drops a repeated letter on some keyboards ("coffee" -> "cofee"): send one key event per letter.
      local text="$1" i
      local ch
      for ((i = 0; i < ${#text}; i++)); do
        ch="${text:i:1}"
        if [[ "$ch" == " " ]]; then run_input keyevent 62
        elif [[ "$ch" =~ [A-Za-z0-9] ]]; then run_input text "$ch"
        else run_input text "\\$ch"; fi  # escaped for the device shell
      done ;;
    wait) sleep "${1:-1}" ;;
    back) run_input keyevent 4 ;;
    home) run_input keyevent 3 ;;
    wake)
      run_input keyevent 224
      if { adb shell dumpsys window 2>/dev/null || true; } | grep -qE "isKeyguardShowing=true|mShowingLockscreen=true|mDreamingLockscreen=true"; then
        echo "Màn hình đang khóa: cần người mở khóa máy." >&2; exit 7
      fi ;;
    launch) adb shell monkey -p "$1" -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1 || true ;;
    stop) adb shell am force-stop "$1" ;;
    top) top_line ;;
    log)
      local pid; pid="$(adb shell pidof "$1" 2>/dev/null | tr -d '\r' || true)"
      [[ -n "$pid" ]] || { echo "app $1 không chạy" >&2; exit 1; }
      adb logcat -d -v time --pid="$pid" 2>/dev/null | tail -n "${2:-80}" ;;
    scrollshots)
      local out="$1" prefix="$2" max="${3:-8}" i prev="" sig
      for ((i = 1; i <= max; i++)); do
        run_cmd shot "$out" "${prefix}-$(printf '%02d' "$i")" > /dev/null
        sig="$(tree_signature "$out/tree/${prefix}-$(printf '%02d' "$i").xml")"
        echo "${prefix}-$(printf '%02d' "$i"): $(python3 -I "$TOOLS/tree.py" "$out/tree/${prefix}-$(printf '%02d' "$i").xml" | wc -l | tr -d ' ') dòng"
        [[ "$sig" != "$prev" ]] || { echo "hết danh sách sau $i ảnh (ảnh cuối trùng ảnh trước)"; break; }
        prev="$sig"; run_cmd swipe up; sleep 0.8
      done ;;
    scrollcollect)
      local max="${1:-15}" i prev="" sig
      : > "$CUR.prev"
      for ((i = 1; i <= max; i++)); do
        dump > "$CUR"
        python3 -I "$TOOLS/tree.py" "$CUR" | grep -oE "'[^']+'|desc='[^']+'" >> "$CUR.prev" || true
        sig="$(tree_signature "$CUR")"
        [[ "$sig" != "$prev" ]] || break
        prev="$sig"; run_cmd swipe up; sleep 0.8
      done
      awk '!seen[$0]++' "$CUR.prev"
      echo "($i lần cuộn)" ;;
    seq)
      local step n=0 code
      IFS=';' read -r -a steps <<<"$1"
      for step in "${steps[@]}"; do
        step="$(sed 's/^ *//; s/ *$//' <<<"$step")"; [[ -n "$step" ]] || continue
        n=$((n + 1)); echo "== bước $n: $step"
        code=0; eval "set -- $step"; ( run_cmd "$@" ) || code=$?
        if [[ "$code" -ne 0 ]]; then echo "DỪNG ở bước $n ($step), mã $code" >&2; exit "$code"; fi
      done ;;
    *) sed -n '6,30p' "$0"; exit 2 ;;
  esac
}

[[ $# -ge 1 ]] || { sed -n '6,30p' "$0"; exit 2; }
if [[ "$1" == "color" ]]; then
  png="$2"; shift 2
  exec python3 -P -c 'import sys
from PIL import Image
im = Image.open(sys.argv[1]).convert("RGB"); a = sys.argv[2:]
for i in range(0, len(a), 2):
    x, y = int(a[i]), int(a[i + 1])
    if not (0 <= x < im.width and 0 <= y < im.height):
        print(x, y, "ngoài ảnh %dx%d" % im.size); continue
    print(x, y, "#%02x%02x%02x" % im.getpixel((x, y)))' "$png" "$@"
fi
if [[ "$1" == "wait" ]]; then sleep "${2:-1}"; exit 0; fi

need_device
screen_env
run_cmd "$@"
