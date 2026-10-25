#!/usr/bin/env bash
# Records a test session of the staging app on a device, so the AI can analyse its log (docs/ops/test-sessions.md).
# The same script runs on a physical device by a person and on the emulator in .github/workflows/device-session.yml,
# so both produce the same folder and are analysed the same way.
#
# Usage:
#   scripts/device-session.sh start [<name>]   start recording on the connected device
#   scripts/device-session.sh reset           clear the app's data, as if just installed, and mark it in the log
#   scripts/device-session.sh scenarios --yes [--ask-permissions]
#                                              run the standard scenarios: cold starts, first launch offline,
#                                              random taps (monkey), then the scenario files of the plans
#                                              (scripts/device-scenario.py). Clears the app's data and toggles airplane mode.
#                                              Grants the app's runtime permissions after each data clear, unless
#                                              --ask-permissions keeps the permission prompts (to test that flow).
#   scripts/device-session.sh stop             stop recording, collect device data, write summary.md
#   scripts/device-session.sh summary <dir>    (re)write summary.md of a session folder; no device needed
#   scripts/device-session.sh helper network-off|network-on|grant-permissions
#                                              functions of the scenarios, called by scripts/device-scenario.py
# Exit codes: 0 done (summary: no blocking signal), 1 error or blocking signal (crash, ANR), 2 usage.
#
# Settings (environment):
#   ANDROID_SERIAL       device to use when several are connected (read by adb itself)
#   APP_PACKAGE          package to test; default: STAGING_APPLICATION_ID of .github/workflows/staging.yml
#   DEVICE_SESSION_DIR   where session folders go; default: test-sessions (ignored by git)
#   DEVICE_SESSION_REPORTS  reports already written; their ids are not reused; default: docs/test-sessions
#   DEVICE_SESSION_RUNS  auto-run reports; session ids they mention (emulator sessions) are not reused;
#                        default: docs/runs
#   MONKEY_EVENTS        random events in `scenarios`; default 1500
#   DEVICE_SESSION_SLEEP seconds for every wait in `scenarios`; tests set 0 (default: the wait of each step)
#   DEVICE_SCENARIOS_GLOB   scenario files of the plans; default: feature/*/src/deviceScenarios/*.txt
#                        app/src/deviceScenarios/*.txt (read by scripts/device-scenario.py)
#   DEVICE_SCENARIOS_BUDGET seconds for all scenario files of the plans, giving the device back included; default 900
#   DEVICE_SCENARIO_CMD  command that runs them; default: python3 scripts/device-scenario.py
# Messages are in Vietnamese for the team; comments are in English.
set -euo pipefail
cd "$(dirname "$0")/.."

SESSIONS_DIR="${DEVICE_SESSION_DIR:-test-sessions}"
REPORTS_DIR="${DEVICE_SESSION_REPORTS:-docs/test-sessions}"
RUNS_DIR="${DEVICE_SESSION_RUNS:-docs/runs}"
STATE_FILE="${SESSIONS_DIR}/.current"
MONKEY_EVENTS="${MONKEY_EVENTS:-1500}"
MONKEY_SEED=20261017
MARKER_TAG="VGL.Session"

fail() { echo "LỖI: $*" >&2; exit 1; }
usage() { sed -n '6,19p' "$0" | sed 's/^# //' >&2; exit 2; }

app_package() {
  if [[ -n "${APP_PACKAGE:-}" ]]; then echo "${APP_PACKAGE}"; return; fi
  sed -n 's/^ *STAGING_APPLICATION_ID: *\([A-Za-z0-9_.]*\).*/\1/p' .github/workflows/staging.yml | head -n 1
}

need_device() {
  command -v adb > /dev/null || fail "không tìm thấy adb. Cài Android SDK platform-tools (docs/ops/test-sessions.md)."
  [[ "$(adb get-state 2>/dev/null || true)" == "device" ]] ||
    fail "không thấy thiết bị. Cắm cáp và bật USB debugging, hoặc ghép Wireless debugging; nhiều máy thì đặt ANDROID_SERIAL."
}

# Waits between scenario steps; DEVICE_SESSION_SLEEP overrides the duration (tests).
pause() { sleep "${DEVICE_SESSION_SLEEP:-$1}"; }

# Prints the device's default network: an id, "none", or "unknown" when dumpsys has no such line.
default_network() {
  local network
  network="$(adb shell dumpsys connectivity 2>/dev/null | tr -d '\r' | sed -n 's/^ *Active default network: *//p' | head -n 1)"
  echo "${network:-unknown}"
}

# Airplane mode alone does not always cut Wi-Fi: Android may keep Wi-Fi on in airplane mode, and the emulator
# did in TS-20261017-03 (L-031). Turn every radio off, then wait until the device has no default network.
network_off() {
  local i
  adb shell cmd connectivity airplane-mode enable > /dev/null 2>&1 || true
  adb shell svc wifi disable > /dev/null 2>&1 || true
  adb shell svc data disable > /dev/null 2>&1 || true
  for i in 1 2 3 4 5 6 7 8 9 10; do
    [[ "$(default_network)" == "none" ]] && return 0
    pause 1
  done
  return 1
}

network_on() {
  adb shell cmd connectivity airplane-mode disable > /dev/null 2>&1 || true
  adb shell svc wifi enable > /dev/null 2>&1 || true
  adb shell svc data enable > /dev/null 2>&1 || true
}

# Writes a line into the device log, so the analysis can split the log by step.
mark() { adb shell log -t "${MARKER_TAG}" "'$*'" > /dev/null 2>&1 || true; }

# Next free id of the day. An id is taken by a session folder on this machine, by a report in the repo
# (sessions recorded elsewhere, such as the emulator in CI, have reports but no folder here), or by an auto-run
# report that mentions it (an emulator session of a run may have no report of its own; L-026 of camera-pose).
next_session_dir() {
  local day number=1 id taken
  day="$(date +%Y%m%d)"
  taken="$(cat "${RUNS_DIR}"/*.md 2>/dev/null | grep -oE "TS-${day}-[0-9]{2}" || true)"
  while true; do
    id="TS-${day}-$(printf '%02d' "${number}")"
    [[ -e "${SESSIONS_DIR}/${id}" || -e "${REPORTS_DIR}/${id}.md" ]] || grep -qx "${id}" <<<"${taken}" || break
    number=$((number + 1))
  done
  echo "${SESSIONS_DIR}/${id}"
}

# Runtime permissions the app requests, as `dumpsys package` lists them under "runtime permissions:".
runtime_permissions() {
  adb shell dumpsys package "$1" 2>/dev/null | tr -d '\r' | awk '
    /^ *runtime permissions:/ { inside = 1; next }
    inside && /: granted=/ { sub(/^ */, ""); sub(/:.*/, ""); print; next }
    inside { inside = 0 }' | sort -u
}

# Grants every runtime permission of the app, so a permission prompt left open does not cover the next cold
# start (L-009 of camera-pose). Prints "granted=<names>" for scenarios.txt; a refused grant is listed as failed.
grant_permissions() {
  local pkg="$1" permission granted="" failed=""
  for permission in $(runtime_permissions "${pkg}"); do
    if adb shell pm grant "${pkg}" "${permission}" > /dev/null 2>&1; then
      granted="${granted:+${granted},}${permission##*.}"
    else
      failed="${failed:+${failed},}${permission##*.}"
    fi
  done
  echo "granted=${granted:-none}${failed:+ failed=${failed}}"
}

cmd_start() {
  local name="${1:-phien-thu}" pkg dir
  need_device
  pkg="$(app_package)"
  [[ -n "${pkg}" ]] || fail "không xác định được tên gói; đặt APP_PACKAGE."
  adb shell pm path "${pkg}" > /dev/null 2>&1 || fail "máy chưa cài ${pkg}. Cài bản staging từ Firebase App Distribution trước."
  [[ ! -f "${STATE_FILE}" ]] || fail "đang có phiên chưa dừng ($(head -n 1 "${STATE_FILE}")). Chạy: scripts/device-session.sh stop"

  dir="$(next_session_dir)"
  mkdir -p "${dir}"
  adb logcat -b all -c || true
  adb shell dumpsys gfxinfo "${pkg}" reset > /dev/null 2>&1 || true
  nohup adb logcat -b all -v threadtime > "${dir}/logcat.txt" 2>&1 &
  printf '%s\n%s\n%s\n' "${dir}" "$!" "${pkg}" > "${STATE_FILE}"

  {
    echo "# PHIÊN THỬ $(basename "${dir}")"
    echo
    echo "- Tên: ${name}"
    echo "- Bắt đầu: $(date '+%Y-%m-%d %H:%M:%S %z')"
    echo "- Ứng dụng: ${pkg}, $(adb shell dumpsys package "${pkg}" | sed -n 's/^ *versionName=//p' | head -n 1 | tr -d '\r')"
    echo "- Máy: $(adb shell getprop ro.product.manufacturer | tr -d '\r') $(adb shell getprop ro.product.model | tr -d '\r'), Android $(adb shell getprop ro.build.version.release | tr -d '\r') (API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))"
    echo "- Commit của repo trên máy chạy script: $(git rev-parse --short HEAD 2>/dev/null || echo 'không rõ')"
  } > "${dir}/session.md"
  [[ -f "${dir}/notes.md" ]] || cat > "${dir}/notes.md" << 'NOTES'
# GHI CHÚ CỦA NGƯỜI THỬ

<!-- Viết ngắn trong lúc thử. AI đọc file này cùng log. Không ghi dữ liệu cá nhân. -->

## Định thử gì

## Đã thử (giờ, việc)

- HH:MM

## Thấy lạ (giờ, chuyện gì)

- HH:MM
NOTES
  mark "session=start name=${name}"
  echo "Đang ghi phiên ${dir}. Thử app, ghi chú vào ${dir}/notes.md, xong thì chạy: scripts/device-session.sh stop"
}

cmd_reset() {
  need_device
  [[ -f "${STATE_FILE}" ]] || fail "chưa có phiên đang ghi. Chạy scripts/device-session.sh start trước."
  local pkg
  pkg="$(sed -n 3p "${STATE_FILE}")"
  mark "action=clear_data"
  adb shell pm clear "${pkg}" > /dev/null
  echo "Đã xóa dữ liệu của ${pkg}, như vừa cài. Mở app để thử lần mở đầu."
}

cmd_scenarios() {
  [[ "${1:-}" == "--yes" ]] || fail "kịch bản sẽ xóa dữ liệu của app và bật, tắt chế độ máy bay. Chạy lại với --yes nếu đồng ý."
  local ask_permissions=0
  case "${2:-}" in
    "") ;;
    --ask-permissions) ask_permissions=1 ;;
    *) fail "không hiểu tùy chọn ${2}. Chỉ có --ask-permissions." ;;
  esac
  need_device
  [[ -f "${STATE_FILE}" ]] || fail "chưa có phiên đang ghi. Chạy scripts/device-session.sh start trước."
  local dir pkg activity i out
  dir="$(sed -n 1p "${STATE_FILE}")"
  pkg="$(sed -n 3p "${STATE_FILE}")"
  activity="$(adb shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER "${pkg}" | tail -n 1 | tr -d '\r')"
  [[ "${activity}" == */* ]] || fail "không tìm thấy màn hình khởi động của ${pkg}."
  : > "${dir}/scenarios.txt"
  # Clearing the data also revokes runtime permissions, so they are granted again after every clear.
  clear_data() {
    adb shell pm clear "${pkg}" > /dev/null
    if [[ "${ask_permissions}" -eq 1 ]]; then
      mark "permissions=ask"
      echo "permissions ask" >> "${dir}/scenarios.txt"
    else
      out="$(grant_permissions "${pkg}")"
      mark "permissions=${out}"
      echo "permissions ${out}" >> "${dir}/scenarios.txt"
    fi
  }

  # 1. Cold starts on a clean install state (the first one also shows the consent dialog).
  clear_data
  for i in 1 2 3; do
    mark "scenario=cold_start run=${i}"
    adb shell am force-stop "${pkg}"
    out="$(adb shell am start -W -n "${activity}" | tr -d '\r')"
    echo "cold_start run=${i} total_ms=$(sed -n 's/^TotalTime: //p' <<<"${out}")" >> "${dir}/scenarios.txt"
    pause 15
  done
  adb exec-out screencap -p > "${dir}/after-start.png" 2>/dev/null || true

  # 2. First launch without network: the spec expects an error and a retry button (loading, KB-2).
  mark "scenario=offline_first_launch"
  clear_data
  if network_off; then
    mark "network=off"
    adb shell am start -W -n "${activity}" > /dev/null
    pause 20
    adb exec-out screencap -p > "${dir}/offline.png" 2>/dev/null || true
    echo "offline_first_launch network=off screenshot=offline.png" >> "${dir}/scenarios.txt"
  else
    # Without a verified offline state the screenshot would show a normal launch, not the error screen.
    mark "network=still_on"
    echo "offline_first_launch network=still_on skipped" >> "${dir}/scenarios.txt"
    echo "CHÚ Ý: tắt mạng không được (máy vẫn có mạng mặc định $(default_network)); bỏ qua kịch bản mất mạng." >&2
  fi
  network_on
  pause 10

  # 3. Random taps and swipes inside the app, with a fixed seed so a crash can be reproduced.
  mark "scenario=monkey events=${MONKEY_EVENTS} seed=${MONKEY_SEED}"
  adb shell monkey -p "${pkg}" -s "${MONKEY_SEED}" --throttle 200 --pct-syskeys 0 -v "${MONKEY_EVENTS}" \
    > "${dir}/monkey.txt" 2>&1 || true
  echo "monkey events=${MONKEY_EVENTS} seed=${MONKEY_SEED}" >> "${dir}/scenarios.txt"

  # 4. The scenario files of the plans (docs/ops/test-sessions.md, section 5), each from a fresh install. A failing
  # scenario is a finding for the analysis, not a block, and a runner that breaks must not stop the session.
  local runner_status=0 runner="${DEVICE_SCENARIO_CMD:-python3 scripts/device-scenario.py}"
  # shellcheck disable=SC2086
  if command -v timeout > /dev/null; then
    # -k: a runner that ignores SIGTERM is killed 30 seconds later (the runner kills its adb child on SIGTERM).
    timeout -k 30 "$((${DEVICE_SCENARIOS_BUDGET:-900} + 120))" ${runner} run --session "${dir}" --package "${pkg}" || runner_status=$?
  else
    ${runner} run --session "${dir}" --package "${pkg}" || runner_status=$?
  fi
  if [[ "${runner_status}" -ne 0 ]]; then
    echo "plan runner_error exit=${runner_status}" >> "${dir}/scenarios.txt"
    echo "CHÚ Ý: trình chạy kịch bản theo plan lỗi (mã ${runner_status}); phần còn lại của phiên vẫn dùng được." >&2
  fi
  mark "scenario=done"
  echo "Đã chạy xong kịch bản chuẩn. Chạy: scripts/device-session.sh stop"
}

# Functions of the scenarios that scripts/device-scenario.py calls, so there is one way to cut the network and to grant.
cmd_helper() {
  need_device
  case "${1:-}" in
    network-off) network_off ;;
    network-on) network_on ;;
    grant-permissions) grant_permissions "$(app_package)" ;;
    *) usage ;;
  esac
}

# The table "Kịch bản theo plan" of summary.md, from the plan lines of scenarios.txt and the log. The crash and ANR
# columns count the app's crashes between the marker of a file and the next VGL.Session line.
print_plan_scenarios() {
  local dir="$1" pkg="$2"
  [[ -f "${dir}/scenarios.txt" ]] || return 0
  echo "## Kịch bản theo plan"
  echo
  if grep -qE '^plan [^ ]+ result=' "${dir}/scenarios.txt"; then
    echo "| Tệp | Ticket | Tiêu chí | Kết quả | Bước lỗi | Giây | Crash/ANR trong tệp |"
    echo "| --- | --- | --- | --- | --- | --- | --- |"
    awk -v pkg="${pkg}" '
      function cell(text) { gsub(/\|/, "/", text); return text }
      FNR == NR {
        if ($0 ~ /^plan [^ ]+ ticket=/) {
          line = $0; sub(/^plan [^ ]+ ticket=/, "", line)
          ticket[$2] = line; sub(/ criterion=.*/, "", ticket[$2])
          crit[$2] = line; sub(/^[^ ]* criterion=/, "", crit[$2])
        } else if ($0 ~ /^plan [^ ]+ step=[0-9]+ .* FAIL( |$)/ && !($2 in bad)) {
          rest = $0; sub(/^plan [^ ]+ step=/, "", rest)
          number = rest; sub(/ .*/, "", number)
          command = rest; sub(/^[0-9]+ /, "", command); sub(/ FAIL( .*|$)/, "", command)
          reason = rest; sub(/^.* FAIL /, "", reason)
          bad[$2] = (command == "check") ? reason : number ": " command
        } else if ($0 ~ /^plan [^ ]+ result=/) {
          result = $0; sub(/^plan [^ ]+ result=/, "", result)
          seconds[$2] = result; sub(/^.* seconds=/, "", seconds[$2])
          sub(/ seconds=.*/, "", result)
          outcome[$2] = result; order[++count] = $2
        }
        next
      }
      /VGL\.Session/ {
        current = ""
        if (match($0, /scenario=plan file=[^ ]+/)) current = substr($0, RSTART + 19, RLENGTH - 19)
        next
      }
      current != "" && index($0, "AndroidRuntime: Process: " pkg ",") { crashes[current]++ }
      current != "" && index($0, "ANR in " pkg) { anrs[current]++ }
      END {
        for (i = 1; i <= count; i++) {
          f = order[i]
          printf "| %s | %s | %s | %s | %s | %s | %d / %d |\n", f, ticket[f], cell(crit[f]), outcome[f], cell(bad[f]), seconds[f], crashes[f], anrs[f]
        }
      }' "${dir}/scenarios.txt" "${dir}/logcat.txt"
    echo
  else
    echo "Kịch bản theo plan: không có"
    echo
  fi
  # Notes of the runner that are out of the ordinary: a late consent form it declined, an app it had to start again.
  local notes
  notes="$(sed -n 's/^plan \([^ ]*\) note step=\([0-9]*\) t=[^ ]* \(.*\)$/- \1, bước \2: \3/p' "${dir}/scenarios.txt" |
    grep -E 'hộp thoại đồng ý hiện muộn|mở lại' || true)"
  if [[ -n "${notes}" ]]; then
    echo "Ghi chú của trình chạy:"
    echo
    echo "${notes}"
    echo
  fi
  if grep -q '^plan runner_error' "${dir}/scenarios.txt"; then
    echo "CHÚ Ý: trình chạy kịch bản theo plan lỗi (dòng \`plan runner_error\` trong scenarios.txt); các tệp chưa chạy không có kết quả."
    echo
  fi
}

cmd_stop() {
  [[ -f "${STATE_FILE}" ]] || fail "không có phiên đang ghi."
  local dir pid pkg
  dir="$(sed -n 1p "${STATE_FILE}")"
  pid="$(sed -n 2p "${STATE_FILE}")"
  pkg="$(sed -n 3p "${STATE_FILE}")"
  if [[ "$(adb get-state 2>/dev/null || true)" == "device" ]]; then
    mark "session=stop"
    sleep 1
    adb shell dumpsys gfxinfo "${pkg}" > "${dir}/gfxinfo.txt" 2>&1 || true
    adb shell dumpsys meminfo "${pkg}" > "${dir}/meminfo.txt" 2>&1 || true
    adb shell dumpsys dropbox --print data_app_crash data_app_anr > "${dir}/crash-anr.txt" 2>&1 || true
  else
    echo "CHÚ Ý: mất kết nối với máy; chỉ giữ log đã ghi." >&2
  fi
  kill "${pid}" 2>/dev/null || true
  rm -f "${STATE_FILE}"
  echo "- Kết thúc: $(date '+%Y-%m-%d %H:%M:%S %z')" >> "${dir}/session.md"
  if ! grep -qE '^- [0-9]{1,2}:[0-9]{2} +[^ ]' "${dir}/notes.md" 2>/dev/null; then
    echo "CHÚ Ý: ${dir}/notes.md chưa có ghi chú. Ghi vài dòng 'HH:MM việc đã làm, điều thấy lạ' trước khi nhờ AI phân tích." >&2
  fi
  local status=0
  cmd_summary "${dir}" "${pkg}" || status=$?
  echo "Đã lưu phiên vào ${dir}. Nhờ AI: \"Phân tích phiên thử ${dir}\" (docs/CLAUDE.md, mục Khi phân tích một phiên thử)."
  return "${status}"
}

# Counts the signals the analysis starts from. Blocking: the app crashed or did not respond.
cmd_summary() {
  local dir="${1:-}" pkg="${2:-}"
  [[ -d "${dir}" && -f "${dir}/logcat.txt" ]] || fail "không có ${dir}/logcat.txt."
  [[ -n "${pkg}" ]] || pkg="$(sed -n 's/^- Ứng dụng: \([^,]*\),.*/\1/p' "${dir}/session.md" 2>/dev/null | head -n 1)"
  [[ -n "${pkg}" ]] || pkg="$(app_package)"
  local log="${dir}/logcat.txt" crashes anrs app_errors app_warnings monkey_events monkey_problem blocking offline_skipped
  crashes="$(grep -c "AndroidRuntime: Process: ${pkg}," "${log}" || true)"
  anrs="$(grep -c "ANR in ${pkg}" "${log}" || true)"
  app_errors="$(grep -cE ' E VGL\.' "${log}" || true)"
  app_warnings="$(grep -cE ' W VGL\.' "${log}" || true)"
  monkey_events=""
  monkey_problem=0
  if [[ -f "${dir}/monkey.txt" ]]; then
    monkey_events="$(sed -n 's/^Events injected: //p' "${dir}/monkey.txt" | tail -n 1)"
    monkey_problem="$(grep -cE '^// (CRASH|NOT RESPONDING)' "${dir}/monkey.txt" || true)"
  fi
  blocking=$(( crashes + anrs + monkey_problem ))
  offline_skipped=0
  [[ -f "${dir}/scenarios.txt" ]] && offline_skipped="$(grep -c 'offline_first_launch network=still_on' "${dir}/scenarios.txt" || true)"
  # The app's own log, crash traces and session markers, for a quick read before the full log.
  grep -E " (VGL[A-Za-z0-9_.]*|AndroidRuntime|ActivityManager) *:" "${log}" |
    grep -E "VGL|FATAL|Process: ${pkg}|ANR in ${pkg}|^\s|Caused by|at " > "${dir}/app-log.txt" || true

  {
    echo "# TÓM TẮT PHIÊN $(basename "${dir}")"
    echo
    echo "Sinh bởi \`scripts/device-session.sh\`. Tín hiệu đếm tự động; AI phân tích tiếp theo \`docs/templates/test-session.md\`."
    echo
    echo "| Tín hiệu | Số lần | Mức |"
    echo "| --- | --- | --- |"
    echo "| App dừng đột ngột (crash) | ${crashes:-0} | $([ "${crashes:-0}" -gt 0 ] && echo chặn || echo '') |"
    echo "| App không phản hồi (ANR) | ${anrs:-0} | $([ "${anrs:-0}" -gt 0 ] && echo chặn || echo '') |"
    echo "| Monkey báo crash hay treo | ${monkey_problem:-0} | $([ "${monkey_problem:-0}" -gt 0 ] && echo chặn || echo '') |"
    echo "| Log lỗi của app (\`E VGL.*\`) | ${app_errors:-0} | xem |"
    echo "| Log cảnh báo của app (\`W VGL.*\`) | ${app_warnings:-0} | xem |"
    if [[ "${offline_skipped:-0}" -gt 0 ]]; then
      echo "| Kịch bản mất mạng bị bỏ qua (không tắt được mạng) | ${offline_skipped} | xem |"
    fi
    echo
    if [[ -f "${dir}/scenarios.txt" ]]; then
      echo "## Kịch bản chuẩn"
      echo
      echo '```text'
      cat "${dir}/scenarios.txt"
      [[ -n "${monkey_events}" ]] && echo "monkey events_injected=${monkey_events}"
      echo '```'
      echo
    fi
    print_plan_scenarios "${dir}" "${pkg}"
    if [[ -f "${dir}/gfxinfo.txt" ]]; then
      echo "## Khung hình và bộ nhớ"
      echo
      echo '```text'
      grep -E 'Total frames rendered|Janky frames|90th percentile|99th percentile' "${dir}/gfxinfo.txt" | head -n 4 || true
      grep -E '^ *TOTAL PSS|^ *TOTAL:' "${dir}/meminfo.txt" 2>/dev/null | head -n 1 || true
      echo '```'
      echo
      echo "Số liệu khung hình trên emulator không có GPU chỉ để so giữa các lần chạy, không phản ánh máy thật."
    fi
  } > "${dir}/summary.md"
  cat "${dir}/summary.md"
  [[ "${blocking}" -eq 0 ]]
}

case "${1:-}" in
  start) shift; cmd_start "$@" ;;
  reset) cmd_reset ;;
  scenarios) shift; cmd_scenarios "$@" ;;
  stop) cmd_stop ;;
  summary) shift; [[ $# -ge 1 ]] || usage; cmd_summary "$1" "${2:-}" ;;
  helper) shift; cmd_helper "$@" ;;
  *) usage ;;
esac
