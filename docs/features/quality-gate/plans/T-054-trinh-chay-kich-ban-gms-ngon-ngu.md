---
ticket: T-054
title: Trình chạy kịch bản chạy lại tệp khi app bị giết theo Google Play services, đặt app về tiếng Anh, ghi đúng thời gian chờ của `fresh`
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 10M          # cỡ S (5.8M, scripts/token-usage.py estimate); nâng theo RUN-20261026-01, nơi task S có cả phiên con đo được 8.1M và 11.4M (L-073)
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-26
---

# PLAN T-054: TRÌNH CHẠY KỊCH BẢN CHẠY LẠI TỆP KHI APP BỊ GIẾT THEO GOOGLE PLAY SERVICES, ĐẶT APP VỀ TIẾNG ANH, GHI ĐÚNG THỜI GIAN CHỜ CỦA `fresh`

## 1. Tóm tắt

Bốn việc nhỏ của trình chạy kịch bản emulator (`scripts/device-scenario.py`), theo FB-20261025-03 và mục Đề xuất của báo cáo RUN-20261026-01:

- **App bị giết theo Google Play services (FB-20261025-03).** Bước lỗi mà `logcat.txt` của phiên có dòng `Killing <pid>:<gói của app>/... depends on provider ... in dying proc <tiến trình>` sau dấu của tệp: bước đó ghi `RETRY`, trình chạy trả máy và chạy lại tệp một lần từ đầu; kết quả của tệp là của lần chạy lại. Lần chạy lại cũng bị giết, hay không đủ thời gian cho một lần chạy lại trọn vẹn, thì `SKIP` và `bỏ qua (môi trường)` (Tuấn chọn ngày 2026-10-26).
- **`kill_group` trên macOS.** `os.killpg` trả `EPERM` (`PermissionError`) với nhóm tiến trình có tiến trình đầu là zombie; hàm chỉ bắt `ProcessLookupError`, nên trình chạy thoát mã 1 (lỗi có sẵn, rà soát độc lập thấy 1 trong 12 lần chạy song song). Bắt cả `PermissionError`: `scripts/check.sh` chạy test này trên Mac và trên cloud.
- **App hiện tiếng Anh.** `str=` đọc `res/values/`, từ T-050, T-051 là tiếng Anh ở mọi module. `fresh` đặt ngôn ngữ riêng của app là `en` (`cmd locale set-app-locales`) trước khi mở app, từ Android 13. Dưới 13 không đặt được ngôn ngữ riêng của app bằng `adb` không root: máy không để tiếng Anh thì tệp `bỏ qua (môi trường)`. Máy từ chối lệnh đặt ngôn ngữ cũng vậy.
- **Ghi chú rà soát T-052, dòng `note`.** `fresh: chờ <n> giây sau khi dừng app` ghi thời gian thật từ lúc dừng app tới lúc mở, không phải số đặt trong `DEVICE_SCENARIO_STOP_SETTLE` (khác nhau khi hạn của tệp tới trước).
- **Ghi chú rà soát T-052, lệnh `device` rồi `fresh`.** Lệnh `device ...` cũng làm `fresh` sau đó trong cùng tệp chờ sau khi dừng app, như sau một lần trả máy.

Không sửa `.github/`, `scripts/device-session.sh` hay app. Bảng "Kịch bản theo plan" của `summary.md` đọc đúng tệp chạy lại mà không phải đổi `device-session.sh` (mục 2).

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ. Yêu cầu là FB-20261025-03 (mục Phân loại, Tuấn chọn ngày 2026-10-26), mục Đề xuất 3 của báo cáo RUN-20261026-01 và hai ghi chú rà soát T-052 (nhật ký plan T-052).

| Lỗi hay đề xuất | Tiêu chí chấp nhận | Cách hiểu của AI (bằng chứng) |
| --- | --- | --- |
| FB-20261025-03: T-022 lỗi ở bước 2 vì Google Play services chết và hệ thống giết app | Bước lỗi của lần chạy đầu mà `logcat.txt` có dòng giết app theo tiến trình đang chết, sau dấu `scenario=plan file=<tệp>` cuối cùng: dòng bước ghi `RETRY app bị hệ thống giết theo Google Play services (<tiến trình>)`, `note` ghi `...; chạy lại tệp từ đầu, mở lại app`, bằng chứng của lần đầu là `fail-1.png`, `fail-1.xml`; trình chạy trả máy, ghi lại dấu của tệp và chạy lại tệp; dòng `result=` duy nhất của tệp là kết quả của lần chạy lại, `seconds=` tính cả hai lần. Lần chạy lại cũng bị giết: `SKIP app bị hệ thống giết theo ...`, `result=bỏ qua (môi trường)`. Chỉ chạy lại khi phần ngân sách còn lại ít nhất bằng `min(timeout của tệp, thời gian lần đầu đã chạy) + 10 (fail.png) + DEVICE_SCENARIO_EVIDENCE_RESERVE + 2 x (DEVICE_SCENARIO_RESET_LIMIT + 45 nếu tệp có network)`; không thì `SKIP ...; không còn thời gian chạy lại`, `result=bỏ qua (môi trường)`. Đầu lần chạy lại, phần còn cho tệp ít hơn thời gian lần đầu đã chạy (tối đa timeout): dòng `step=0 retry SKIP ...`, `result=bỏ qua (môi trường)`. Tiến trình phụ của app (`<gói>:<tên>`) tính như app. Dòng giết chỉ tính khi `logcat.txt` đã có dấu của lần chạy này (lần chạy thứ hai cần dấu thứ hai), nên dòng giết của lần đầu không tính cho lần sau. Dòng giết trước dấu của tệp, hay của gói khác: lỗi như cũ | Dòng thật trong `logcat.txt` của lần chạy 40226750845: `ActivityManager: Killing 9002:com.vglstudio.camera.pose.line.guide.staging/u0a192 (adj 0): depends on provider com.google.android.gms/.fonts.provider.FontsProvider in dying proc com.google.android.gms.persistent`. `device-session.sh` ghi logcat của cả phiên vào `<thư mục phiên>/logcat.txt` (dòng 142, `adb logcat -b all -v threadtime`) và gọi trình chạy với cùng thư mục (`--session "${dir}"`, dòng 252), nên trình chạy đọc được tệp đó lúc chạy, không cần lệnh `adb logcat` mới. Dấu `scenario=plan file=<tệp>` do trình chạy ghi bằng `log -t VGL.Session` ở đầu mỗi lần chạy, nên dòng sau dấu cuối cùng là của lần chạy đang làm |
| Báo cáo RUN-20261026-01, đề xuất 3: trình chạy đặt rõ ngôn ngữ tiếng Anh | Mỗi `fresh`, sau `pm clear` và cấp quyền, trước `am start -W`: `adb shell cmd locale set-app-locales <gói> --locales en`. Lệnh thoát khác 0 hay in `Exception`, `Error`, `Unknown`: `SKIP không đặt được ngôn ngữ tiếng Anh cho app (<dòng đầu>)`. API dưới 33: không gửi lệnh đó; `getprop persist.sys.locale` (rỗng thì `getprop ro.product.locale`, ngôn ngữ xuất xưởng của máy chưa từng đổi) không bắt đầu bằng `en` thì `SKIP máy để ngôn ngữ <giá trị>; dưới Android 13 trình chạy không đặt được ngôn ngữ của app, đặt máy về tiếng Anh` | `string_values` chỉ đọc `res/values/strings.xml`. `cmd locale set-app-locales` có từ Android 13 (API 33) và đã chạy được trên emulator CI: lệnh `device locale en-US` của T-022 dùng đúng lệnh này và đạt ở TS-20261025-02. Thẻ `en` trùng `app/src/main/res/xml/locales_config.xml` (`<locale android:name="en" />`). Trả máy sau mỗi tệp đã gửi `set-app-locales <gói>` không có `--locales` (về ngôn ngữ của máy), nên máy thật không giữ tiếng Anh sau lượt. Dưới Android 13, app chọn ngôn ngữ bằng hộp chọn trong app lưu ở DataStore (spec settings, KB-4); `pm clear` xóa nó nên app theo ngôn ngữ của máy; đổi ngôn ngữ của máy cần quyền mà `adb shell` không có nếu không root |
| Ghi chú rà soát T-052: dòng `note` luôn ghi "chờ 3 giây" | `note` ghi `min(lúc dừng app + DEVICE_SCENARIO_STOP_SETTLE, hạn của tệp) - lúc dừng app`, định dạng `seconds_text` (số nguyên không có phần lẻ, còn lại một chữ số lẻ) | Code hiện tại: `self.note(f"fresh: chờ {seconds_text(self.stop_settle)} ...")` rồi `time.sleep(max(0.0, min(stopped + self.stop_settle, self.file_deadline) - time.monotonic()))` |
| Ghi chú rà soát T-052: tệp có `device ...` rồi `fresh` không chờ | `step_device` đặt `self.device_reset = True` | Plan T-052 mục 2: `Destroy timeout of remove-task` chỉ thấy khi dừng app lúc Activity còn đang dựng lại sau lần trả máy; lệnh `device` (kích thước, xoay, chế độ tối, ngôn ngữ) dựng lại Activity như vậy. Ba kịch bản hiện có không có `fresh` sau lệnh `device` (T-022 có `fresh` ở dòng 34, lệnh `device` ở dòng 52, 56), nên không đổi thời gian của chúng |

**Vì sao app bị giết theo Google Play services** (FB-20261025-03, Phân loại): app không dùng phông tải về; thư viện `androidx.emoji2` mà Compose kéo theo hỏi provider phông chữ của Google Play services lúc khởi động. Đây là hành vi chung của app Compose, không phải lỗi của app; plan không sửa app.

**Bảng `summary.md` với tệp chạy lại** (đọc `scripts/device-session.sh`, hàm `print_plan_scenarios`):

- cột "Bước lỗi" lấy dòng đầu tiên có ` FAIL` của tệp; dòng `RETRY` không khớp, nên lần đầu không hiện thành bước lỗi;
- cột "Kết quả" và "Giây" lấy dòng `result=`; trình chạy ghi đúng một dòng cho mỗi tệp;
- cột crash và ANR đếm từ dấu `scenario=plan file=<tệp>` tới dòng `VGL.Session` sau, nên cộng cả hai lần chạy;
- mục "Ghi chú của trình chạy" lọc `note` có `hộp thoại đồng ý hiện muộn|mở lại`: chữ `note` của lần chạy lại có "mở lại app", nên hiện ở đó mà không phải đổi `device-session.sh`.

Một test đối chứng (mục 4) kiểm ba điểm này trên `device-session.sh` hiện tại.

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0013 (hộp thoại đồng ý của bản staging, không đổi).

## 3. Giải pháp

### 3.1. Cách làm

Bản cài thử dưới đây đã chạy với khối test của mục 4 (mục 4.1). Commit code của bước 4 là đúng `diff` này, cộng docstring đầu tệp và tài liệu (cuối mục):

```diff
diff --git a/scripts/device-scenario.py b/scripts/device-scenario.py
index ea04bdf..4422b8f 100755
--- a/scripts/device-scenario.py
+++ b/scripts/device-scenario.py
@@ -90,6 +90,8 @@ UNFOLDED_SIZE, UNFOLDED_DENSITY = "1766x2208", "420"  # 673 x 841 dp, the frame
 IDLE_ERROR = "could not get idle state"
 RESULT_PASSED, RESULT_FAILED, RESULT_ENV = "đạt", "lỗi", "bỏ qua (môi trường)"
 RESULT_TIME, RESULT_SYNTAX = "bỏ qua (hết thời gian)", "lỗi tệp"
+RETRY = "chạy lại"  # not a result in the log: the first run of a file whose app the system killed
+EVIDENCE_SHOT = 10  # seconds keep_evidence may take for fail.png
 SELECTOR = re.compile(r'^(id|idp|desc|text|str)=("[^"]*"|.+?)((?:\s+(?:in|index|selected|clickable)=\S+)*)\s*$')
 QUOTED = re.compile(r'"[^"]*"')
 SAFE_NAME = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*\.txt")
@@ -101,6 +103,8 @@ FILTER = re.compile(r"(in|index|selected|clickable)=(\S+)")
 TRAILING_NUMBER = re.compile(r"^(.*\S)\s+(\d+)$")
 PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdf]")
 LOCALE_TAG = re.compile(r"[A-Za-z]{2,3}(-[A-Za-z0-9]+)*")
+SCENARIO_LANGUAGE = "en"  # str= reads res/values, which is English in every module (T-050, T-051)
+KILLED_WITH = re.compile(r"Killing \d+:(?P<package>[^/\s]+)/\S+ .*depends on provider \S+ in dying proc (?P<proc>\S+)")
 
 
 def unfolded_display(physical):
@@ -440,8 +444,8 @@ class Screen:
 def kill_group(process):
     try:
         os.killpg(process.pid, signal.SIGKILL)
-    except ProcessLookupError:
-        pass
+    except (ProcessLookupError, PermissionError):
+        pass  # macOS answers EPERM for a group whose leader is a zombie: nothing is left to kill
 
 
 def run_process(command, timeout, binary=False, env=None):
@@ -485,8 +489,10 @@ class Runner:
         self.process_wait = env_seconds("DEVICE_SCENARIO_PROCESS_WAIT", 5)
         self.evidence_reserve = env_seconds("DEVICE_SCENARIO_EVIDENCE_RESERVE", 20)
         self.stop_settle = env_seconds("DEVICE_SCENARIO_STOP_SETTLE", 3)
-        # The device was given back since the last fresh, so fresh waits after stopping the app.
+        # The device was given back (or changed by a device command) since the last fresh, so fresh waits after
+        # stopping the app.
         self.device_reset = False
+        self.sdk = None  # API level of the device, read once at the start of the run
         self.log_path = self.session / "scenarios.txt"
         self.file_deadline = 0.0
         self.file_dir = self.session
@@ -634,12 +640,15 @@ class Runner:
             self.activity = lines[-1].strip() if lines else ""
         if "/" not in self.activity:
             raise Fail(f"không tìm thấy màn hình khởi động của {self.package}")
+        self.set_language()
         if self.device_reset:
             # Stopping an app whose Activity the reset is still rebuilding makes the system kill the next start
             # about a second later ("Destroy timeout of remove-task", FB-20261025-02): start after that.
             self.device_reset = False
-            self.note(f"fresh: chờ {seconds_text(self.stop_settle)} giây sau khi dừng app (máy vừa trả về gốc)")
-            time.sleep(max(0.0, min(stopped + self.stop_settle, self.file_deadline) - time.monotonic()))
+            # The shorter of the two, not (stopped + settle) - stopped, which can be 1.9999 and print as "2.0".
+            wait = max(0.0, min(self.stop_settle, self.file_deadline - stopped))
+            self.note(f"fresh: chờ {seconds_text(wait)} giây sau khi dừng app (máy vừa trả về gốc)")
+            time.sleep(max(0.0, stopped + wait - time.monotonic()))
         problem = self.start_app()
         if problem is None:
             return
@@ -653,6 +662,23 @@ class Runner:
             self.note(f"fresh: lần mở 2 không có tiến trình ({problem})")
             raise Fail("app không chạy sau 2 lần mở")
 
+    def set_language(self):
+        """The app shows English, the language str= reads: from Android 13 its own language is set; below 13 the
+        language of the device must be English, otherwise the file is skipped (plan T-054)."""
+        if self.sdk is not None and self.sdk < 33:
+            _, output = self.adb("shell", "getprop", "persist.sys.locale")
+            if not output.strip():  # a device never set to another language keeps its factory one
+                _, output = self.adb("shell", "getprop", "ro.product.locale")
+            language = output.strip() or "?"
+            if not language.lower().startswith(SCENARIO_LANGUAGE):
+                raise Skip(f"máy để ngôn ngữ {language}; dưới Android 13 trình chạy không đặt được ngôn ngữ của app, "
+                           "đặt máy về tiếng Anh")
+            return
+        code, output = self.adb("shell", "cmd", "locale", "set-app-locales", self.package, "--locales",
+                                SCENARIO_LANGUAGE)
+        if code != 0 or re.search(r"Exception|Error|Unknown", output):
+            raise Skip(f"không đặt được ngôn ngữ tiếng Anh cho app ({(output.strip().splitlines() or [str(code)])[0]})")
+
     def start_app(self):
         """None when the app has a process after am start -W, otherwise why not. A start that outlives its limit
         still counts when the process is there (an app that starts slowly but runs)."""
@@ -754,6 +780,7 @@ class Runner:
             raise Skip("không tắt được mạng (máy vẫn có mạng mặc định)")
 
     def step_device(self, arguments):
+        self.device_reset = True  # a fresh later in the file stops an app whose Activity was rebuilt, like after a reset
         words = arguments["words"]
         if words[0] == "rotate":
             self.adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
@@ -871,12 +898,29 @@ class Runner:
             self.log(f"plan {name} result={RESULT_SYNTAX} seconds=0")
             return
         uses_network = any(command == "network" for _, command, _, _ in steps)
-        allowance = self.budget_left() - self.reset_limit - (45 if uses_network else 0) - self.evidence_reserve
-        if allowance <= 0:
-            self.log(f"plan {name} result={RESULT_TIME} seconds=0")
-            return
+        first_began, attempt = time.monotonic(), 1
+        while True:
+            allowance = self.budget_left() - self.reset_limit - (45 if uses_network else 0) - self.evidence_reserve
+            if attempt > 1 and allowance < min(self.timeout_of(header), first_run):
+                self.log(f"plan {name} step=0 retry SKIP app bị hệ thống giết; không còn thời gian chạy lại")
+                allowance = 0.0
+            if allowance <= 0:
+                result = RESULT_TIME if attempt == 1 else RESULT_ENV
+                self.log(f"plan {name} result={result} seconds={int(round(time.monotonic() - first_began))}")
+                return
+            result = self.attempt_file(name, header, steps, allowance, attempt, uses_network)
+            first_run = time.monotonic() - first_began
+            self.reset_device(network=self.network_touched)
+            if result != RETRY:
+                self.log(f"plan {name} result={result} seconds={int(round(time.monotonic() - first_began))}")
+                return
+            attempt += 1
+
+    def attempt_file(self, name, header, steps, allowance, attempt, uses_network):
+        """The result of one run of the steps; RETRY when the system killed the app on the first run and there is
+        time to run the file once more (FB-20261025-03)."""
         began = time.monotonic()
-        self.file_deadline = began + min(int(header.get("timeout", DEFAULT_TIMEOUT)), allowance)
+        self.file_deadline = began + min(self.timeout_of(header), allowance)
         self.last_xml = None
         self.cached_screen = self.pending_screen = None
         self.consent_state = None
@@ -901,29 +945,69 @@ class Runner:
             except (AdbTimeout, Fail, ConsentFail) as problem:
                 outcome, result, failed = f"{text} FAIL {'adb_timeout' if isinstance(problem, AdbTimeout) else problem}", \
                     RESULT_FAILED, True
+                killer = self.killed_by(name, attempt)
+                if killer is not None:
+                    reason = f"app bị hệ thống giết theo {killer}"
+                    # A second run that has only a few seconds would end as a failure of its own: run again only
+                    # when there is the time this run took (at most the timeout of the file), its evidence, and two
+                    # returns of the device (network included).
+                    needed = min(self.timeout_of(header), time.monotonic() - began) + EVIDENCE_SHOT \
+                        + self.evidence_reserve + 2 * (self.reset_limit + (45 if uses_network else 0))
+                    if attempt == 1 and self.budget_left() >= needed:
+                        outcome, result = f"{text} RETRY {reason}", RETRY
+                        self.note(f"{reason} (logcat.txt); chạy lại tệp từ đầu, mở lại app")
+                    else:
+                        why = "" if attempt > 1 else "; không còn thời gian chạy lại"
+                        outcome, result = f"{text} SKIP {reason}{why}", RESULT_ENV
             self.pending_screen = None
             self.log(" ".join([f"plan {name} note step={index} t={at:.1f}", "; ".join(self.notes)]).rstrip())
             self.log(outcome)
             if failed:
-                self.keep_evidence()
+                self.keep_evidence("fail-1" if result == RETRY else "fail")
             if result != RESULT_PASSED:
                 break
-        self.log(f"plan {name} result={result} seconds={int(round(time.monotonic() - began))}")
-        self.reset_device(network=self.network_touched)
+        return result
 
-    def keep_evidence(self):
-        """fail.xml (the last dump) and fail.png of the first failing step."""
+    def timeout_of(self, header):
+        return int(header.get("timeout", DEFAULT_TIMEOUT))
+
+    def killed_by(self, name, attempt=1):
+        """Who the system killed the app (or one of its processes, <package>:<name>) with, from logcat.txt of the
+        session after the marker of this run of the file: "Google Play services (<process>)" or the dying process;
+        None without such a line, or while logcat.txt does not have the marker of this run yet."""
+        try:
+            lines = (self.session / "logcat.txt").read_text(encoding="utf-8", errors="replace").splitlines()
+        except OSError:
+            return None
+        marker = f"scenario=plan file={name}"
+        starts = [number for number, line in enumerate(lines) if marker in line]
+        if len(starts) < attempt:
+            return None  # the marker of this run is not written yet: a kill after an older marker is not this run
+        for line in lines[starts[-1] + 1:]:
+            found = KILLED_WITH.search(line)
+            if found and found.group("package").split(":")[0] == self.package:
+                proc = found.group("proc")
+                return f"Google Play services ({proc})" if proc.startswith("com.google.android.gms") else proc
+        return None
+
+    def keep_evidence(self, stem="fail"):
+        """<stem>.xml (the last dump) and <stem>.png of the first failing step (fail-1 for a run that is run again)."""
         if self.last_xml:
             self.file_dir.mkdir(parents=True, exist_ok=True)
-            (self.file_dir / "fail.xml").write_text(self.last_xml, encoding="utf-8")
+            (self.file_dir / f"{stem}.xml").write_text(self.last_xml, encoding="utf-8")
         try:
-            self.save_screenshot("fail.png", timeout=10)
+            self.save_screenshot(f"{stem}.png", timeout=10)
         except (AdbTimeout, Fail, OSError):
             pass
 
     def run(self, files):
         self.session.mkdir(parents=True, exist_ok=True)
         self.read_display()
+        try:
+            _, output = run_process(["adb", "shell", "getprop", "ro.build.version.sdk"], 5)
+            self.sdk = int(output.strip())
+        except (AdbTimeout, ValueError):
+            self.sdk = None  # unknown: fresh tries to set the language of the app
         for path in files:
             self.run_file(path)
         self.reset_device(network=self.network_used)
```

Ghi chú về cách làm:

- **Thời gian chạy lại** (rà soát độc lập, điểm nên sửa 1). Lần chạy lại chỉ còn vài giây sẽ ra `lỗi`, và `docs/auto-run.md` mục 4 soạn phản hồi giả cho dòng `lỗi`. Vì vậy lúc bước lỗi, trình chạy chỉ chạy lại khi `budget_left() >= min(timeout, thời gian lần đầu đã chạy) + EVIDENCE_SHOT (10 giây, hạn của `fail.png`) + DEVICE_SCENARIO_EVIDENCE_RESERVE + 2 x (DEVICE_SCENARIO_RESET_LIMIT + 45 nếu tệp có network)`, tức đủ cho bằng chứng, hai lần trả máy và một lần chạy dài như lần đầu. Đầu lần chạy lại, `allowance < min(timeout, thời gian lần đầu)` (lần trả máy lâu hơn dự kiến) thì ghi `plan <tệp> step=0 retry SKIP app bị hệ thống giết; không còn thời gian chạy lại` và `result=bỏ qua (môi trường)`; nhánh này không có test riêng vì lần trả máy của `adb` giả không tốn thời gian, điều kiện lúc bước lỗi đã chặn trước nó.
- **`kill_group`** bắt thêm `PermissionError` (rà soát độc lập, điểm nên sửa 2): không có test riêng, vì không tạo được nhóm có tiến trình đầu zombie một cách chắc chắn; test thời gian của mục 4 nới khoảng dư để không chạm trường hợp này.
- **Dấu của lần chạy.** `killed_by(name, attempt)` chỉ đọc khi `logcat.txt` có ít nhất `attempt` dấu của tệp (ghi chú rà soát): `adb logcat` ghi qua bộ đệm, nên ở lần thứ hai, dấu cuối cùng có thể vẫn là dấu của lần đầu. Ba tệp hiện có chạy hết 348 giây ở RUN-20261025-02 (T-024 149 giây ở RUN-20261026-01), xa ngân sách 900, nên một lần chạy lại vừa.
- **Dòng log.** Dạng dòng bước thêm `RETRY <lý do>` cạnh `ok`, `FAIL`, `SKIP`. Dòng tiêu đề `plan <tệp> ticket=...` ghi một lần.
- **API đọc một lần.** `getprop ro.build.version.sdk` ở đầu lượt (`run`). Không đọc được thì coi như từ 13 trở lên, gửi lệnh đặt ngôn ngữ; máy cũ từ chối thì tệp `bỏ qua (môi trường)` theo nhánh lệnh lỗi.
- **Vị trí của lệnh ngôn ngữ.** Sau `pm clear` và cấp quyền, trước lúc chờ sau khi dừng app (T-052): lệnh rơi vào khoảng chờ, không thêm thời gian cho tệp đứng sau lần trả máy.

**Docstring đầu `scripts/device-scenario.py`:**
- lệnh `fresh`: thêm "sets the language of the app to en first (Android 13 and later; below 13 a device in another language skips the file)";
- đoạn Log: dạng dòng bước thành `ok|FAIL <reason>|SKIP <reason>|RETRY <reason>`; thêm câu "A step that fails while logcat.txt of the session shows the system killing the app with a dying process (`Killing <pid>:<package>/... depends on provider ... in dying proc ...`, after the marker of the file) is RETRY: the device is given back and the file runs once more, its result is the one of the second run and the first evidence is fail-1.png and fail-1.xml; killed again, or without time for a second run, the step is SKIP and the file skipped (FB-20261025-03).";
- dòng Settings của `DEVICE_SCENARIO_STOP_SETTLE`: "when the device was given back or changed by a device command".

**Tài liệu.** `docs/ops/test-sessions.md` mục 5:
- câu "Kết quả nằm trong thư mục phiên": dạng dòng `ok|FAIL|SKIP|RETRY`; thêm `fail-1.png`, `fail-1.xml` của lần chạy đầu khi tệp được chạy lại;
- hàng `fresh` của bảng lệnh: thêm "Trước khi mở, đặt ngôn ngữ riêng của app là tiếng Anh (`en`), vì `str=` đọc `res/values/` (tiếng Anh); dưới Android 13 máy phải để tiếng Anh, không thì tệp `bỏ qua (môi trường)`. Cũng chờ sau khi dừng app khi tệp đã có lệnh `device` trước đó";
- hàng `device locale <thẻ>`: thêm "Mỗi `fresh` đặt lại `en`";
- dưới đoạn **Hạn chót**, thêm một gạch đầu dòng "**App bị hệ thống giết theo tiến trình khác** (FB-20261025-03): bước lỗi mà `logcat.txt` có `Killing ...: depends on provider ... in dying proc ...` của app sau dấu của tệp ghi `RETRY`; trình chạy chạy lại tệp một lần. Lần chạy lại cũng bị giết, hay không còn thời gian, thì `SKIP` và `bỏ qua (môi trường)`. Thường gặp trên emulator: Google Play services tự cập nhật hay khởi động lại, và Android giết mọi app đang giữ provider phông chữ của nó. Máy chạy kịch bản dưới Android 13 phải để tiếng Anh."

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/device-scenario.py` | Như `diff` ở mục 3.1 (gồm `kill_group` bắt `PermissionError`); docstring đầu tệp |
| Sửa | `scripts/tests/run.sh` | Khối test của mục 4, chép nguyên: (1) `ds_run` chép `$ds_logcat` vào thư mục phiên; (2) 13 test `device-scenario` ngay trước dòng `# Plan T-048: notes of the runner about a late consent form or a restarted app are listed under the table.`; (3) 1 test đối chứng `device-session` ngay trước `expect "device-session: the runner is called with timeout -k"`. Không đổi test có sẵn |
| Sửa | `scripts/tests/fake-adb` | `FAKE_ADB_SDK`, `FAKE_ADB_LOCALE`, `FAKE_ADB_LOCALE_FAILS`; `log -t` ghi dấu vào `FAKE_ADB_LOGCAT_FILE` rồi `FAKE_ADB_KILL_LINE` (`FAKE_ADB_KILL_ONCE`); chú thích của chúng, như khối của mục 4 |
| Sửa | `docs/auto-run.md` | Mục 4, bước 1, gạch đầu dòng "Đọc bảng Kịch bản theo plan": thêm câu "Tệp có dòng `RETRY` (app bị hệ thống giết theo tiến trình khác, T-054): kết quả là của lần chạy lại; `fail-1.png`, `fail-1.xml` là của lần đầu, không phải phát hiện. Tệp đạt ở lần chạy lại thì ghi lần chạy lại vào báo cáo, không soạn phản hồi." |
| Sửa | `docs/ops/test-sessions.md` | Mục 5: dạng dòng `RETRY`, `fail-1`, hàng `fresh`, hàng `device locale`, đoạn app bị giết theo tiến trình khác, máy dưới Android 13 để tiếng Anh |

PR của plan này (không phải phiên) tạo ticket T-054 và chuyển FB-20261025-03 sang `planned`, dòng Liên kết thêm ticket và plan T-054.

**Nơi khác đọc các file plan này sửa (L-020), và vì sao không bị ảnh hưởng:**
- **`scripts/device-session.sh`** gọi trình chạy, đọc `scenarios.txt` cho bảng và ghi chú của `summary.md` (mục 2). Dòng `RETRY` không khớp mẫu ` FAIL`; mỗi tệp vẫn một dòng `result=`; ghi chú của lần chạy lại hiện nhờ chữ "mở lại". Test đối chứng mới kiểm điều này; không đổi `device-session.sh`.
- **`.github/workflows/device-session.yml`** chạy `device-session.sh`; không đổi. Mỗi `fresh` thêm một lệnh `adb` (dưới 1 giây trên emulator CI). Lần chạy lại chỉ xảy ra khi app bị giết.
- **`docs/auto-run.md` mục 4, bước 1** đọc bảng của `summary.md`: dòng `bỏ qua (môi trường)` ghi vào báo cáo, không soạn phản hồi. Tệp bị giết hai lần đi đúng đường đó. Plan thêm một câu cho tệp chạy lại (mục 3.2).
- **Nơi nhắc `docs/auto-run.md`**: `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/resume-check.py`, `scripts/save-branch.sh`, `scripts/status.py` dẫn tới tệp này hay một mục của nó trong chú thích, thông báo hoặc đường dẫn. Plan chỉ thêm một câu vào một gạch đầu dòng có sẵn của mục 4, bước 1; không đổi tên hay số của mục nào, nên các chỗ dẫn đó không bị ảnh hưởng.
- **Kịch bản của repo** (`T-020`, `T-022`, `T-024`): bộ chọn `str=` đọc chữ tiếng Anh; emulator CI đã để `en-US`, nên đặt `en` không đổi màn nào. Lệnh `device locale en-US` của T-022 vẫn chạy được (mục 8, câu 3). Test có sẵn `the scenarios of the repository pass check` không đổi.
- **`scripts/tests/fake-adb`** dùng ở khối `device-session`, `app-explore`: giá trị mặc định của `getprop ro.build.version.sdk` vẫn là 34; `getprop persist.sys.locale` trước đây in `Fake` (nhánh `getprop*`), nay in `en-US`; không chỗ nào khác đọc giá trị này (`grep -rn 'persist.sys.locale' scripts/` chỉ thấy dòng mới). Lệnh `cmd locale set-app-locales <gói>` không có `--locales` (trả máy) vẫn vào nhánh `*) exit 0`.
- **Test có sẵn đếm `set-app-locales`**: `ds_reset_calls` đếm `set-app-locales` với ngưỡng `-ge 6`, thêm lệnh của `fresh` không làm nó sai; test `device commands change the device and the reset undoes them` đếm `--locales en-US`, không khớp `--locales en`. Toàn bộ `scripts/tests/run.sh` qua với bản cài thử (mục 4.1).
- **`scripts/tests/run.sh`** được `scripts/check.sh` chạy (cả tệp), và được `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/wait-run.sh` nhắc trong chú thích. Plan thêm 14 test và đổi `ds_run` (chép `logcat.txt` giả khi biến `ds_logcat` có giá trị; đặt `FAKE_ADB_LOGCAT_FILE` cho thư mục phiên, nên `log -t` của `adb` giả ghi dấu vào `logcat.txt`; trình chạy chỉ đọc tệp này khi một bước lỗi, và chỉ tìm dòng giết app, nên test có sẵn không đổi kết quả); cách gọi, `plan_check` và dòng tổng kết không đổi, nên bốn script đó không bị ảnh hưởng.
- **`app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ui/UiAutomatorTagsTest.kt`** nhắc `scripts/device-scenario.py` trong chú thích, không chạy nó.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `cmd locale set-app-locales` chạy bằng quyền của `adb shell`, không phải quyền của app.
- **Thư viện và SDK:** Không thêm.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** dòng giết app có thể chưa có trong `logcat.txt` lúc bước lỗi, vì `adb logcat` ghi vào tệp qua bộ đệm. Ở FB-20261025-03, app bị giết lúc 19:53:18.629, còn bước 2 lỗi khi hết 30 giây chờ, nên dòng đã có từ lâu; với bước lỗi ngay sau lúc giết (ví dụ `tap`), dòng có thể tới sau. Thứ hai, `cmd locale set-app-locales` có thể bị một số máy thật từ chối; trình chạy đã xử lý bằng `bỏ qua (môi trường)` kèm lý do.
- **Điều kiện chuyển:** `device-session` có tệp `lỗi` mà `logcat.txt` có dòng giết app sau dấu của tệp, không có `RETRY`.
- **Cách làm thay** (task sau, không cài sẵn): `killed_by` đọc lại tệp sau `DEVICE_SCENARIO_POLL` giây khi chưa thấy. Phiên không dừng vì điều này (mục 7, điều 3).

Chạy thử trên emulator: chưa chạy (emulator của máy dev đã xóa theo yêu cầu, 2026-10-25). Lần kiểm thật là `device-session` cuối phiên (mục 4, dòng kiểm thủ công).

## 4. Kiểm thử

Test viết trước code: 14 test mới trong `scripts/tests/run.sh`, chạy với `adb` giả (`scripts/tests/fake-adb`) và `logcat.txt` trong thư mục phiên (dấu của tệp do `log -t` của `adb` giả ghi, dòng giết từ `FAKE_ADB_KILL_LINE`). Phần `fake-adb` và `ds_run` vào commit test của bước 3, vì test cần chúng để chạy. Kết quả thật của từng test trên code hiện tại nằm ở mục 4.1. Dòng ghi **(đối chứng)** qua được cả trên code cũ (L-035) và không tính vào số test thất bại trước.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| App bị giết ở lần đầu: chạy lại, kết quả là của lần chạy lại | Test script | `device-scenario: an app killed with Google Play services runs the file again, and the second run counts`: `FAKE_ADB_DEAD_STARTS=2` (lần đầu không có tiến trình sau 2 lần mở), dòng giết sau mỗi dấu; mong đợi `"0 1 1 0 2 1"`: mã 0, 1 dòng `RETRY app bị hệ thống giết theo Google Play services`, 1 dòng `result=đạt`, 0 dòng ` FAIL`, 2 dấu của tệp, 1 `note` có `chạy lại tệp từ đầu, mở lại app` |
| Bị giết cả hai lần: bỏ qua (môi trường) | Test script | `device-scenario: an app killed in both runs ends as skipped (environment)`: `FAKE_ADB_DEAD_STARTS=9`; mong đợi `"0 1 1 1 0"` (1 `RETRY`, 1 `SKIP app bị hệ thống giết theo Google Play services`, 1 `result=bỏ qua (môi trường)`, 0 ` FAIL`) |
| Tiến trình phụ của app (`<gói>:<tên>`) tính như app | Test script | `device-scenario: a process of the app (package:name) killed with Google Play services counts too`; mong đợi `"0 1 1"` (1 `RETRY`, 1 `result=đạt`) |
| Dòng giết của lần đầu không tính cho lần chạy lại | Test script | `device-scenario: the kill of the first run does not count in the second run`: `FAKE_ADB_KILL_ONCE=1`, `FAKE_ADB_DEAD_STARTS=9`; mong đợi `"0 1 1 1"` (1 `RETRY`, 1 `FAIL app không chạy sau 2 lần mở` ở lần chạy lại, 1 `result=lỗi`) |
| Dòng giết trước dấu của tệp, hay của app khác: lỗi như cũ | Test script (đối chứng) | `device-scenario: control: a kill before the marker of the file or of another app is a plain failure`: `logcat.txt` có sẵn dòng giết trước dấu, dòng giết sau dấu là của `com.other.app`; mong đợi `"0 1 0 1 1"` (1 `FAIL app không chạy sau 2 lần mở`, 0 `RETRY`, 1 `result=lỗi`, 1 dấu của tệp) |
| Không đủ thời gian cho hai lần trả máy và bằng chứng: bỏ qua ngay | Test script | `device-scenario: a killed app without time for its resets and evidence is skipped at once`: `DEVICE_SCENARIOS_BUDGET=60 DEVICE_SCENARIO_RESET_LIMIT=25 DEVICE_SCENARIO_EVIDENCE_RESERVE=20 DEVICE_SCENARIO_PROCESS_WAIT=0.1` (lần đầu dưới 1 giây, cần khoảng 80 giây, còn khoảng 59); mong đợi `"0 1 0 1 1"` (1 `SKIP ... không còn thời gian chạy lại`, 0 `RETRY`, 1 `result=bỏ qua (môi trường)`, 1 dấu) |
| Không đủ thời gian cho một lần chạy dài như lần đầu: bỏ qua | Test script | `device-scenario: a killed app is not run again when what is left is less than the first run took`: `DEVICE_SCENARIOS_BUDGET=12 DEVICE_SCENARIO_RESET_LIMIT=0 DEVICE_SCENARIO_EVIDENCE_RESERVE=0 DEVICE_SCENARIO_PROCESS_WAIT=1` (lần đầu khoảng 2,2 giây; cần khoảng 12,2, còn khoảng 9,8; khoảng dư không đổi khi máy chậm, vì hai vế cùng dịch theo thời gian lần đầu); mong đợi `"0 1 0 1"` (1 `SKIP ... không còn thời gian chạy lại`, 0 `RETRY`, 1 `result=bỏ qua (môi trường)`). Code cũ của bản cài thử đầu (`time_left > 0`) cho `RETRY` ở đây |
| `fresh` đặt app về tiếng Anh trước khi mở | Test script | `device-scenario: fresh sets the language of the app to English before it starts the app`; mong đợi `"0 1 yes"` (1 lệnh `shell cmd locale set-app-locales com.example.app.staging --locales en`, sau `pm clear`, trước `am start -W`) |
| Dưới Android 13, máy tiếng Việt: bỏ qua | Test script | `device-scenario: below Android 13 a device in another language is skipped (environment)`: `FAKE_ADB_SDK=30 FAKE_ADB_LOCALE=vi-VN`; mong đợi `"0 1 1 0"` (1 `SKIP máy để ngôn ngữ vi-VN`, 1 `result=bỏ qua (môi trường)`, 0 `am start -W`) |
| Dưới Android 13, máy tiếng Anh: chạy, không gửi lệnh đặt ngôn ngữ | Test script (đối chứng) | `device-scenario: control: below Android 13 a device in English runs without setting the language`; mong đợi `"0 1 0"` |
| Máy từ chối lệnh đặt ngôn ngữ: bỏ qua | Test script | `device-scenario: a device that refuses to set the language of the app is skipped (environment)`: `FAKE_ADB_LOCALE_FAILS=1`; mong đợi `"0 1 1"` |
| `note` ghi thời gian chờ thật | Test script | `device-scenario: the note of fresh gives the real wait when the file ends first`: hai tệp `timeout: 3`, `DEVICE_SCENARIO_STOP_SETTLE=10`; mong đợi `"1 0"` (1 `note` của tệp thứ hai `fresh: chờ <0 đến 3,0> giây sau khi dừng app`, 0 dòng `fresh: chờ 10 giây`) |
| Lệnh `device` rồi `fresh` cũng chờ | Test script | `device-scenario: fresh after a device command in the same file waits after stopping the app`: `device night on`, `fresh`, `DEVICE_SCENARIO_STOP_SETTLE=2`; mong đợi `"0 1"` |
| Bảng `summary.md` của tệp chạy lại | Test script (đối chứng) | `device-session: control: a file run again shows its second result, no failing step, and the note`: `scenarios.txt` có dòng `RETRY` rồi lần chạy lại `đạt`; mong đợi `"0 1 1"` (hàng `\| killed.txt \| T-022 \| Mẫu \| đạt \|  \| 95 \|`, ghi chú `- killed.txt, bước 2: app bị hệ thống giết theo Google Play services`) |
| Test có sẵn | Test script | Toàn bộ `scripts/tests/run.sh` có sẵn phải còn qua, không đổi test nào |
| Tệp bị giết được chạy lại ở `device-session` | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`) | Chỉ khi Google Play services chết lại trong một lượt: `scenarios.txt` có `RETRY app bị hệ thống giết theo Google Play services` và một dòng `result=` của tệp; không chết thì ghi "không gặp", không phải lỗi (mục 7, điều 3) |
| `fresh` đặt tiếng Anh ở `device-session` | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`) | `device-session` cuối phiên: `scenarios.txt` có `set-app-locales` trong lệnh, không tệp nào `lỗi` vì `str=`; T-024 đạt như TS-20261025-03 |
| Kịch bản trên máy thật để tiếng Việt | Kiểm tra thủ công (máy thật: emulator CI để tiếng Anh, cần một máy Android 13 trở lên để tiếng Việt) | Người chạy `scripts/device-session.sh` có kịch bản trên máy thật để tiếng Việt: các tệp không lỗi vì chữ; sau lượt app về ngôn ngữ của máy |
| Phiên làm theo tài liệu | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt chéo đọc diff `docs/ops/test-sessions.md` mục 5 |

**Ngoại lệ viết test trước:** tài liệu (`docs/ops/test-sessions.md`), docstring và chú thích (`scripts/device-scenario.py`, `scripts/tests/fake-adb`, `scripts/tests/run.sh`).

**Khối test (chép nguyên vào commit test của bước 3):**

```diff
diff --git a/scripts/tests/fake-adb b/scripts/tests/fake-adb
index be19b05..eac9969 100755
--- a/scripts/tests/fake-adb
+++ b/scripts/tests/fake-adb
@@ -12,6 +12,10 @@
 # $FAKE_ADB_HANG names a shell subcommand (for example uiautomator) that never returns. `wm size` and `wm density`
 # report a $FAKE_ADB_PHYSICAL_SIZE (default 1080x2400) screen at 420 dpi.
 # `logcat -b crash -d` prints $FAKE_ADB_CRASH_LOG (default nothing) and returns.
+# `getprop ro.build.version.sdk` prints $FAKE_ADB_SDK (default 34), `getprop persist.sys.locale` $FAKE_ADB_LOCALE
+# (default en-US); `cmd locale set-app-locales ... --locales` fails when $FAKE_ADB_LOCALE_FAILS is set (plan T-054).
+# `log -t <tag> <text>` appends a logcat line to $FAKE_ADB_LOGCAT_FILE when set, then $FAKE_ADB_KILL_LINE when set
+# (with FAKE_ADB_KILL_ONCE=1 only after the first line, counted in $FAKE_ADB_STATE_DIR/kills.n).
 # The app process (plan T-048) is only simulated when $FAKE_ADB_STATE_DIR is set; without it `pidof` prints nothing.
 # With it: `am start -W` counts the starts in starts.n and leaves the process alive (file `alive`) once the count is
 # above $FAKE_ADB_DEAD_STARTS (default 0); `pm clear` and `am force-stop` kill it; `pidof` prints 4242 while it is
@@ -52,7 +56,27 @@ case "$1" in
       "wm size") echo "Physical size: ${FAKE_ADB_PHYSICAL_SIZE:-1080x2400}" ;;
       "wm density") echo "Physical density: 420" ;;
       "pm path"*) [[ -n "${FAKE_ADB_NO_APP:-}" ]] && exit 1; echo "package:/data/app/base.apk" ;;
-      "getprop ro.build.version.sdk") echo 34 ;;
+      "getprop ro.build.version.sdk") echo "${FAKE_ADB_SDK:-34}" ;;
+      "getprop persist.sys.locale") echo "${FAKE_ADB_LOCALE:-en-US}" ;;
+      "log -t "*)
+        [[ -z "${FAKE_ADB_LOGCAT_FILE:-}" ]] && exit 0
+        echo "10-25 19:53:04.500  1000  1000 I $3: ${*:4}" >> "${FAKE_ADB_LOGCAT_FILE}"
+        if [[ -n "${FAKE_ADB_KILL_LINE:-}" ]]; then
+          state="${FAKE_ADB_STATE_DIR:-${TMPDIR:-/tmp}}"
+          mkdir -p "$state"
+          kills="$(cat "$state/kills.n" 2>/dev/null || echo 0)"
+          if [[ "${FAKE_ADB_KILL_ONCE:-}" != 1 || "$kills" == 0 ]]; then
+            echo "${FAKE_ADB_KILL_LINE}" >> "${FAKE_ADB_LOGCAT_FILE}"
+          fi
+          echo $(( kills + 1 )) > "$state/kills.n"
+        fi
+        exit 0 ;;
+      "cmd locale set-app-locales"*"--locales"*)
+        if [[ -n "${FAKE_ADB_LOCALE_FAILS:-}" ]]; then
+          echo "Exception occurred while executing 'set-app-locales':"
+          exit 255
+        fi
+        exit 0 ;;
       getprop*) echo Fake ;;
       "dumpsys package"*)
         echo "    versionName=0.1.0-staging.4"
diff --git a/scripts/tests/run.sh b/scripts/tests/run.sh
index 9bf90e4..15a9516 100755
--- a/scripts/tests/run.sh
+++ b/scripts/tests/run.sh
@@ -1892,10 +1892,11 @@ ds_run() { # <dump files of the fixtures, in order> <scenario glob> [VAR=value .
   for f in $1; do seq+="$ds_fixtures/$f "; done
   rm -rf "$work/ds-session" "$work/adb-state"
   mkdir -p "$work/ds-session"
+  [[ -n "${ds_logcat:-}" ]] && cp "$ds_logcat" "$work/ds-session/logcat.txt"
   : > "$ds_calls"
   env PATH="$work/bin:$PATH" FAKE_ADB_UI_SEQ="${seq% }" FAKE_ADB_STATE_DIR="$work/adb-state" FAKE_ADB_CALLS="$ds_calls" \
     DEVICE_SCENARIOS_GLOB="$2" DEVICE_SCENARIO_POLL=0.05 DEVICE_SESSION_SLEEP=0 APP_PACKAGE=com.example.app.staging \
-    FAKE_ADB_RUNTIME_PERMISSIONS="android.permission.CAMERA" "${@:3}" \
+    FAKE_ADB_RUNTIME_PERMISSIONS="android.permission.CAMERA" FAKE_ADB_LOGCAT_FILE="$work/ds-session/logcat.txt" "${@:3}" \
     python3 -I scripts/device-scenario.py run --session "$work/ds-session" --package com.example.app.staging \
     > "$work/ds.out" 2>&1 || code=$?
   echo "$code"
@@ -2260,6 +2261,49 @@ ds_script settle-mid.txt 'fresh' 'fresh'
 expect "device-scenario: control: a second fresh inside one file does not wait" "0 0 2" \
   "$(ds_run "camera.xml" "$ds_files/settle-mid.txt" DEVICE_SCENARIO_STOP_SETTLE=2) $(ds_count 'fresh: chờ') $(ds_count '^plan settle-mid.txt step=[12] fresh ok$')"
 
+# Plan T-054: the system kills the app when Google Play services dies (FB-20261025-03): the runner reads logcat.txt
+# of the session after the marker of the run (fake-adb writes the marker, then FAKE_ADB_KILL_LINE) and runs the file
+# once more; killed again, or without the time of a second run, the file is skipped.
+ds_kill_line() { # <process of the app>: the line of the system killing it with Google Play services
+  echo "10-25 19:53:18.629   598   631 I ActivityManager: Killing 9002:$1/u0a192 (adj 0): depends on provider com.google.android.gms/.fonts.provider.FontsProvider in dying proc com.google.android.gms.persistent"
+}
+ds_script killed.txt 'fresh'
+ds_kill_app="$(ds_kill_line com.example.app.staging)"
+expect "device-scenario: an app killed with Google Play services runs the file again, and the second run counts" "0 1 1 0 2 1" \
+  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$ds_kill_app" FAKE_ADB_DEAD_STARTS=2 DEVICE_SCENARIO_PROCESS_WAIT=0.1) $(ds_count '^plan killed.txt step=1 fresh RETRY app bị hệ thống giết theo Google Play services') $(ds_count '^plan killed.txt result=đạt') $(ds_count ' FAIL') $(ds_count "scenario=plan file=killed.txt" "$ds_calls") $(ds_count '^plan killed.txt note step=1 .*chạy lại tệp từ đầu, mở lại app')"
+expect "device-scenario: an app killed in both runs ends as skipped (environment)" "0 1 1 1 0" \
+  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$ds_kill_app" FAKE_ADB_DEAD_STARTS=9 DEVICE_SCENARIO_PROCESS_WAIT=0.1) $(ds_count '^plan killed.txt step=1 fresh RETRY ') $(ds_count '^plan killed.txt step=1 fresh SKIP app bị hệ thống giết theo Google Play services') $(ds_count '^plan killed.txt result=bỏ qua (môi trường)') $(ds_count ' FAIL')"
+expect "device-scenario: a process of the app (package:name) killed with Google Play services counts too" "0 1 1" \
+  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$(ds_kill_line com.example.app.staging:remote)" FAKE_ADB_DEAD_STARTS=2 DEVICE_SCENARIO_PROCESS_WAIT=0.1) $(ds_count '^plan killed.txt step=1 fresh RETRY ') $(ds_count '^plan killed.txt result=đạt')"
+expect "device-scenario: the kill of the first run does not count in the second run" "0 1 1 1" \
+  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$ds_kill_app" FAKE_ADB_KILL_ONCE=1 FAKE_ADB_DEAD_STARTS=9 DEVICE_SCENARIO_PROCESS_WAIT=0.1) $(ds_count '^plan killed.txt step=1 fresh RETRY ') $(ds_count '^plan killed.txt step=1 fresh FAIL app không chạy sau 2 lần mở') $(ds_count '^plan killed.txt result=lỗi')"
+ds_kill_line com.example.app.staging > "$work/ds-logcat-before.txt"
+expect "device-scenario: control: a kill before the marker of the file or of another app is a plain failure" "0 1 0 1 1" \
+  "$(ds_logcat="$work/ds-logcat-before.txt" ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$(ds_kill_line com.other.app)" FAKE_ADB_DEAD_STARTS=9 DEVICE_SCENARIO_PROCESS_WAIT=0.1) $(ds_count '^plan killed.txt step=1 fresh FAIL app không chạy sau 2 lần mở') $(ds_count 'RETRY') $(ds_count '^plan killed.txt result=lỗi') $(ds_count "scenario=plan file=killed.txt" "$ds_calls")"
+expect "device-scenario: a killed app without time for its resets and evidence is skipped at once" "0 1 0 1 1" \
+  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$ds_kill_app" FAKE_ADB_DEAD_STARTS=9 DEVICE_SCENARIO_PROCESS_WAIT=0.1 DEVICE_SCENARIOS_BUDGET=60 DEVICE_SCENARIO_RESET_LIMIT=25 DEVICE_SCENARIO_EVIDENCE_RESERVE=20) $(ds_count '^plan killed.txt step=1 fresh SKIP app bị hệ thống giết theo Google Play services.*không còn thời gian chạy lại') $(ds_count 'RETRY') $(ds_count '^plan killed.txt result=bỏ qua (môi trường)') $(ds_count "scenario=plan file=killed.txt" "$ds_calls")"
+expect "device-scenario: a killed app is not run again when what is left is less than the first run took" "0 1 0 1" \
+  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$ds_kill_app" FAKE_ADB_DEAD_STARTS=9 DEVICE_SCENARIO_PROCESS_WAIT=1 DEVICE_SCENARIOS_BUDGET=12 DEVICE_SCENARIO_RESET_LIMIT=0 DEVICE_SCENARIO_EVIDENCE_RESERVE=0) $(ds_count '^plan killed.txt step=1 fresh SKIP .*không còn thời gian chạy lại') $(ds_count 'RETRY') $(ds_count '^plan killed.txt result=bỏ qua (môi trường)')"
+# Plan T-054: str= reads res/values (English), so fresh sets the language of the app to English from Android 13; below
+# 13 a device in another language is skipped (environment).
+ds_script lang.txt 'fresh'
+expect "device-scenario: fresh sets the language of the app to English before it starts the app" "0 1 yes" \
+  "$(ds_run "camera.xml" "$ds_files/lang.txt") $(ds_count '^shell cmd locale set-app-locales com.example.app.staging --locales en$' "$ds_calls") $(ds_in_order "$ds_calls" '^shell pm clear' '^shell cmd locale set-app-locales com.example.app.staging --locales en$' '^shell am start -W')"
+expect "device-scenario: below Android 13 a device in another language is skipped (environment)" "0 1 1 0" \
+  "$(ds_run "camera.xml" "$ds_files/lang.txt" FAKE_ADB_SDK=30 FAKE_ADB_LOCALE=vi-VN) $(ds_count '^plan lang.txt step=1 fresh SKIP máy để ngôn ngữ vi-VN') $(ds_count '^plan lang.txt result=bỏ qua (môi trường)') $(ds_count '^shell am start -W' "$ds_calls")"
+expect "device-scenario: control: below Android 13 a device in English runs without setting the language" "0 1 0" \
+  "$(ds_run "camera.xml" "$ds_files/lang.txt" FAKE_ADB_SDK=30 FAKE_ADB_LOCALE=en-US) $(ds_count '^plan lang.txt result=đạt') $(ds_count 'set-app-locales com.example.app.staging --locales en$' "$ds_calls")"
+expect "device-scenario: a device that refuses to set the language of the app is skipped (environment)" "0 1 1" \
+  "$(ds_run "camera.xml" "$ds_files/lang.txt" FAKE_ADB_LOCALE_FAILS=1) $(ds_count '^plan lang.txt step=1 fresh SKIP không đặt được ngôn ngữ tiếng Anh cho app') $(ds_count '^plan lang.txt result=bỏ qua (môi trường)')"
+# Plan T-054 (review notes of T-052): the note gives the real wait, and a device command before fresh also waits.
+printf 'ticket: T-999\ncriterion: Test\ntimeout: 3\nfresh\n' > "$ds_files/settle-short-a.txt"
+cp "$ds_files/settle-short-a.txt" "$ds_files/settle-short-b.txt"
+expect "device-scenario: the note of fresh gives the real wait when the file ends first" "1 0" \
+  "$(ds_run "camera.xml" "$ds_files/settle-short-*.txt" DEVICE_SCENARIO_STOP_SETTLE=10 > /dev/null; ds_count '^plan settle-short-b.txt note step=1 .*fresh: chờ [0-3]\(\.[0-9]\)\{0,1\} giây sau khi dừng app') $(ds_count 'fresh: chờ 10 giây')"
+ds_script devfresh.txt 'device night on' 'fresh'
+expect "device-scenario: fresh after a device command in the same file waits after stopping the app" "0 1" \
+  "$(ds_run "camera.xml" "$ds_files/devfresh.txt" DEVICE_SCENARIO_STOP_SETTLE=2) $(ds_count '^plan devfresh.txt note step=2 .*fresh: chờ 2 giây sau khi dừng app')"
+
 # Plan T-048: notes of the runner about a late consent form or a restarted app are listed under the table.
 mkplan "$work/plan-notes" "$plan_lines
 plan ok.txt note step=1 t=0.4
@@ -2270,6 +2314,17 @@ expect "device-session: summary lists the runner notes about consent and restart
   "$(joined "$notes_code" "$(notes_cell 'Ghi chú của trình chạy:')" \
     "$(notes_cell '- ok.txt, bước 2: hộp thoại đồng ý hiện muộn, đã từ chối')" "$(notes_cell '- ok.txt, bước 1')" \
     "$(cell 'Ghi chú của trình chạy')")"
+# Plan T-054: a file run again after the system killed the app: the table has the second result and no failing step,
+# and the note of the run again is listed (it says "mở lại").
+mkplan "$work/plan-retry" 'plan killed.txt ticket=T-022 criterion=Mẫu
+plan killed.txt note step=2 t=30.1 app bị hệ thống giết theo Google Play services (com.google.android.gms.persistent) (logcat.txt); chạy lại tệp từ đầu, mở lại app
+plan killed.txt step=2 consent decline 30 RETRY app bị hệ thống giết theo Google Play services (com.google.android.gms.persistent)
+plan killed.txt note step=1 t=0.1
+plan killed.txt step=1 fresh ok
+plan killed.txt result=đạt seconds=95' "$(cat "$fixtures/logcat-clean.txt")"
+retry_code="$(session /dev/null summary "$work/plan-retry" com.example.app.staging)"
+expect "device-session: control: a file run again shows its second result, no failing step, and the note" "0 1 1" \
+  "$retry_code $(grep -cF '| killed.txt | T-022 | Mẫu | đạt |  | 95 |' "$work/plan-retry/summary.md" || true) $(grep -cF -- '- killed.txt, bước 2: app bị hệ thống giết theo Google Play services' "$work/plan-retry/summary.md" || true)"
 expect "device-session: the runner is called with timeout -k" 1 "$(grep -c 'timeout -k 30' scripts/device-session.sh || true)"
 mkplan "$work/plan-pipe" 'plan ok.txt ticket=T-900 criterion=a | b
 plan ok.txt result=đạt seconds=1' "$(cat "$fixtures/logcat-clean.txt")"
```

### 4.1. Chạy thử

Tiêu chí E là 1: đọc `logcat.txt` của phiên trong lúc chạy và lệnh `cmd locale` lúc `fresh` là mới với trình chạy, nhưng lệnh `cmd locale set-app-locales` đã dùng ở `device locale` (T-046) và đạt trên emulator CI.

**Chạy các test mới trên code hiện tại** (L-075; 2026-10-26, nhánh chính `3bf61da`, macOS, `/bin/bash` 3.2, `scratchpad/env.sh`). Khối test của mục 4 chèn vào `scripts/tests/run.sh`, `fake-adb` của worktree, chạy cả tệp; không commit, đã trả về.

| Test | Code hiện tại | Lý do |
| --- | --- | --- |
| chạy lại khi app bị giết | thất bại `'0 0 0 1 1 0'` | không có `RETRY`, tệp `lỗi`, 1 dòng ` FAIL`, 1 dấu |
| bị giết hai lần | thất bại `'0 0 0 0 1'` | tệp `lỗi`, không `SKIP` |
| tiến trình phụ của app | thất bại `'0 0 0'` | không có `RETRY`, tệp `lỗi` |
| dòng giết của lần đầu không tính cho lần sau | thất bại `'0 0 1 1'` | không có `RETRY` (chỉ một lần chạy) |
| đối chứng dòng giết trước dấu, app khác | qua `0 1 0 1 1` | |
| không đủ thời gian cho trả máy và bằng chứng | thất bại `'0 0 0 0 1'` | tệp `lỗi` |
| không đủ thời gian cho lần chạy dài như lần đầu | thất bại `'0 0 0 0'` | tệp `lỗi` |
| `fresh` đặt tiếng Anh | thất bại `'0 0 no'` | không có lệnh `set-app-locales ... --locales en` |
| dưới 13, máy tiếng Việt | thất bại `'0 0 0 1'` | tệp chạy và mở app |
| đối chứng dưới 13, máy tiếng Anh | qua `0 1 0` | |
| máy từ chối lệnh ngôn ngữ | thất bại `'0 0 0'` | không gửi lệnh, tệp đạt |
| `note` ghi thời gian thật | thất bại `'0 1'` | `note` ghi `chờ 10 giây` |
| `device` rồi `fresh` chờ | thất bại `'0 0'` | `fresh` không chờ |
| đối chứng bảng `summary.md` | qua | `device-session.sh` không đổi |

Tổng (lần chạy sau rà soát độc lập, 2026-10-26): 663 test, 652 `ok` (gồm 3 đối chứng mới), 11 `FAIL` (11 test mới), đúng lý do, không `FAIL` nào khác. Lần chạy trước rà soát (11 test, trước khi thêm 3 test thời gian và tiến trình): 660 test, 652 `ok`, 8 `FAIL`, cùng các giá trị trên; rà soát độc lập chạy lại và thấy đúng 8 `FAIL` đó.

**Bản cài thử** (không commit, đã trả về): `scripts/device-scenario.py` như `diff` ở mục 3.1 (không gồm docstring). Kết quả thật, theo thứ tự:

- Bản đầu (trước rà soát): 660 test. Lần 1 có 1 `FAIL` ở test `note` ghi thời gian thật: thời gian chờ thật là 2,96 giây, in thành `3.0`, ngoài mẫu `[0-2]`; mẫu nới thành `[0-3]`. Hai lần sau: 660 `ok`.
- Rà soát độc lập chạy bản đó 12 lần song song: 1 lần thoát mã 1 ở test "không còn thời gian chạy lại" (khoảng dư khoảng 1 giây), vì `kill_group` ném `PermissionError` (`os.killpg` trên macOS trả `EPERM` với nhóm có tiến trình đầu là zombie, lỗi có sẵn). Đã sửa cả hai: `kill_group` bắt `PermissionError`, test nới khoảng dư (mục 4).
- Bản sau rà soát, lần 1: 663 `ok`. Lần 2: 1 `FAIL` ở test có sẵn `fresh after a reset waits after stopping the app, the first file does not` (mong đợi `'0 1 0 yes'`, nhận `'0 0 0 yes'`): `note` tính `min(stopped + 3, hạn) - stopped`, phép trừ số thực ra 1,9999 và in thành `2.0`. Đã đổi thành `min(DEVICE_SCENARIO_STOP_SETTLE, hạn - lúc dừng app)` (mục 3.1), không đổi test.
- Bản cuối: 4 lần liền 663 `ok`, `All script tests passed`, mỗi lần khoảng 3 phút.

Không chạy thử trên emulator (mục 3.4). Lần kiểm thật là `device-session` cuối phiên (mục 4, dòng kiểm thủ công).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI và quy trình phiên tự chạy). Trình chạy kịch bản chạy ở `device-session` cuối mỗi phiên.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script, test, `fake-adb`, một tài liệu |
| B. Độ rõ của yêu cầu | 0 | Cách xử lý do Tuấn chọn; dòng log đọc thẳng từ logcat của lần chạy 40226750845 |
| C. Khả năng kiểm chứng tự động | 2 | Mọi nhánh có test với `adb` giả; việc Google Play services chết chỉ gặp ngẫu nhiên trên emulator |
| D. Ảnh hưởng chéo | 2 | Mọi `fresh` của mọi kịch bản gửi thêm lệnh đặt ngôn ngữ; kết quả của tệp có thể đổi từ `lỗi` sang `bỏ qua (môi trường)` |
| E. Độ mới của kỹ thuật | 1 | Đọc `logcat.txt` của phiên lúc chạy; lệnh `cmd locale` đã có |
| **Tổng** | 6 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:** `scripts/device-scenario.py` (`device-session` cuối mỗi phiên), `scripts/tests/run.sh`, `scripts/tests/fake-adb`, `docs/ops/test-sessions.md`.
- **Task khác có thể xung đột:** các plan viết song song cho phiên sau (test bố cục bản dịch và chữ "Độ mờ", bỏ khóa hướng dọc, lời báo máy chủ không tới được) sửa app và có thể thêm hay sửa tệp kịch bản; không plan nào khác dự kiến sửa `scripts/device-scenario.py`. `scripts/tests/run.sh` có thể chung nếu plan khác đổi fixture hay test của khối `device-scenario`; task chạy lần lượt, mỗi task tạo nhánh từ nhánh chính mới nhất. `run-check.py` sẽ báo chú ý L-014 khi hai plan cùng ghi `scripts/tests/run.sh`.
- **Kịch bản mới của plan khác:** từ sau T-054, kịch bản chạy với app tiếng Anh; bộ chọn `text=` tiếng Việt chỉ khớp sau `device locale vi`.
- **Đề xuất thứ tự chạy:** đầu danh sách, để `device-session` cuối phiên có cả cách nhận ra app bị giết lẫn ngôn ngữ đặt rõ khi chạy kịch bản mới của các task sau.
- **Dự án mẫu.** Chạy lại tệp khi app bị giết theo tiến trình khác và đặt ngôn ngữ của app khi chạy kịch bản có ích cho mọi app; đề xuất về mẫu (`docs/ops/from-app.md`) cùng các điểm T-048, T-049, T-052 (issue VGL-Studio/app-project-template#163); ghi vào mô tả PR.

## 7. Điều kiện dừng riêng

1. Cần sửa `.github/workflows/device-session.yml` hay `scripts/device-session.sh` (kể cả bộ lọc ghi chú hay bảng của `summary.md`): dừng, viết plan riêng.
2. Một test có sẵn của khối `device-scenario`, `device-session` hay `app-explore` phải sửa thì mới qua: dừng.
3. Không phải điều kiện dừng: ở `device-session` cuối chính phiên này, Google Play services không chết (không có `RETRY`), hay một kịch bản lỗi vì lý do khác. Phiên ghi vào báo cáo; crash và ANR vẫn chặn staging như cũ.

## 8. Câu hỏi cho người duyệt

1. **Chạy lại khi app bị giết theo bất kỳ tiến trình nào đang chết**, không riêng Google Play services. Lý do ghi "Google Play services (<tiến trình>)" khi tiến trình bắt đầu bằng `com.google.android.gms`, còn lại ghi tên tiến trình. **Đề xuất của AI:** chấp nhận; dòng `depends on provider ... in dying proc` luôn là việc của hệ thống, không phải lỗi của app.
2. **Dưới Android 13, máy không để tiếng Anh thì bỏ qua cả tệp** (không đổi ngôn ngữ của máy). **Đề xuất của AI:** chấp nhận; đổi ngôn ngữ của máy cần root hay ứng dụng ngoài, và emulator CI là API 34.
3. **Lệnh `device locale en-US` của T-022** (kiểm "tiếng Anh") nay không đổi gì vì app đã tiếng Anh từ `fresh`. **Đề xuất của AI:** không sửa trong T-054; chủ feature poses xem có đổi thành `device locale vi` và kiểm chữ tiếng Việt bằng `text=` qua ticket riêng. Ảnh `english` vẫn chụp đúng.
4. **Thời gian chạy lại** tính đủ cho chính lần chạy lại (thời gian lần đầu, bằng chứng, hai lần trả máy), nhưng không giữ chỗ cho các tệp sau: một lần chạy lại có thể làm tệp cuối `bỏ qua (hết thời gian)` khi tổng gần 900 giây. **Đề xuất của AI:** chấp nhận; ba tệp hiện có dưới 350 giây.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-26, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
