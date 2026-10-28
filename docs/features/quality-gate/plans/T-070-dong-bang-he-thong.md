---
ticket: T-070
title: Trình chạy kịch bản đóng bảng của hệ thống che app (bảng thông báo, cài đặt nhanh)
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 8M           # cỡ S (6.3M, scripts/token-usage.py estimate); code đã có sẵn ở mục 3.1, chỉ cần chép, chạy test và rà soát
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-070: TRÌNH CHẠY KỊCH BẢN ĐÓNG BẢNG CỦA HỆ THỐNG CHE APP (BẢNG THÔNG BÁO, CÀI ĐẶT NHANH)

## 1. Tóm tắt

Ở `device-session` cuối RUN-20261027-02 (lần chạy 40576589779), cả sáu tệp kịch bản lỗi ở bước 2 `consent decline 30` vì bảng cài đặt nhanh của hệ thống mở đè lên app (FB-20261027-02). Plan sửa trình chạy kịch bản (`scripts/device-scenario.py`) theo hai lớp:

- **`fresh` đóng bảng trước khi mở app:** `adb shell cmd statusbar collapse`, sau khi đặt ngôn ngữ và trước `am start -W`. Sáu tệp kịch bản hiện có đều bắt đầu bằng `fresh`, nên tệp đầu tiên sau monkey cũng bắt đầu với bảng đã đóng; trình chạy không bắt buộc điều đó, tệp không mở đầu bằng `fresh` được lớp đóng bảng trong `dump()` (ý dưới) che. Không cần thêm một bước riêng giữa monkey và trình chạy, nên không sửa `.github/` hay phần monkey của `device-session.sh`.
- **Dump chỉ có giao diện hệ thống:** mọi node của lần dump có `package="com.android.systemui"` thì trình chạy đóng bảng một lần, đọc lại màn hình và ghi `note ... bảng của hệ thống che app, đã đóng (cmd statusbar collapse)`. Lớp này bắt trường hợp bảng mở giữa tệp, ví dụ do một thông báo của hệ thống.

`summary.md` liệt kê dòng `note` đó ở mục "Ghi chú của trình chạy". Bốn test mới tái hiện lỗi của phiên trên `scripts/tests/fake-adb`; cả bốn đã thất bại trên code hiện tại và qua với code ở mục 3.1 (mục 4). Lệnh `cmd statusbar collapse` chỉ kiểm thật được ở `device-session` sau khi merge, vì `device-session.yml` chỉ chạy trên nhánh chính.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ. Yêu cầu là FB-20261027-02 (mục Phân loại; nhóm chọn ngày 2026-10-28 làm ticket và plan ngay).

| Lỗi | Tiêu chí chấp nhận | Cách hiểu của AI (bằng chứng) |
| --- | --- | --- |
| FB-20261027-02: bảng cài đặt nhanh che app, mọi tệp lỗi ở `consent decline 30` | `fresh` gửi `adb shell cmd statusbar collapse` trước `am start -W`; với bảng mở từ trước (như sau monkey), `ok.txt` đạt | Bằng chứng của phiên: cả sáu `fail.xml` chỉ có node của `com.android.systemui` (68 node mỗi tệp); `fail.png` là bảng cài đặt nhanh; app vẫn chạy phía dưới (`20:35:00.654 I VGL.Onboarding: Onboarding shown` trong 30 giây chờ của bước 2). `step_fresh` hiện tại dừng app, xóa dữ liệu, cấp quyền, đặt ngôn ngữ rồi mở app, không đóng bảng nào. `am start` không đóng bảng thông báo: bảng là cửa sổ của SystemUI, nằm trên Activity của app |
| Bảng mở giữa tệp | Một lần dump mà mọi node có `package="com.android.systemui"`: gửi `cmd statusbar collapse` một lần rồi dump lại; dòng `note` của bước có `bảng của hệ thống che app, đã đóng (cmd statusbar collapse)` một lần dù bước đọc nhiều lần; bảng vẫn còn sau khi đóng thì bước xử lý màn hình đọc lại như mọi màn hình không có phần tử nó tìm (chờ tiếp tới hết hạn của bước, rồi `FAIL` với lý do cũ) | Dump rỗng (không node) không tính: đã có lý do `dump rỗng` riêng. Một node của app trong dump là đủ để không đóng gì, nên hộp thoại đồng ý (vẽ trong cửa sổ của app) và hộp thoại xin quyền (`com.google.android.permissioncontroller`) không bị đụng tới. Không tệp kịch bản nào của app tìm phần tử của SystemUI |
| Người đọc `summary.md` thấy trình chạy đã đóng bảng | Mục "Ghi chú của trình chạy" có dòng `- <tệp>, bước <n>: bảng của hệ thống che app, đã đóng (cmd statusbar collapse)` | Mục này lọc `note` bằng `grep -E 'hộp thoại đồng ý hiện muộn|mở lại'` (`scripts/device-session.sh`, hàm `print_plan_scenarios`); thêm `bảng của hệ thống` vào biểu thức. Dòng `note` của `fresh` (đóng bảng lần đầu) không ghi gì, vì lần nào cũng đóng: chỉ lần đóng do dump chỉ có SystemUI mới là chuyện bất thường |
| Tiêu chí của T-062, T-064 chưa kiểm vì lỗi này | `device-session` của phiên có T-070 merge: các tệp kịch bản không lỗi ở bước `consent decline` vì bảng của hệ thống; tiêu chí kịch bản camera của T-062 (`T-058-chup-tu-chi-tiet.txt`, `T-020-camera-mo-gap.txt` khác `bỏ qua (môi trường)`) và tệp `T-064-mot-camera.txt` được kiểm lại | Kiểm ở `device-session` cuối phiên (mục 4) |

**Về nguyên nhân bảng mở (giả thuyết, chưa chắc):** monkey (1500 sự kiện, `--pct-syskeys 0`, chạy ngay trước trình chạy, `scripts/device-session.sh` dòng 264) có thể vuốt từ mép trên. `--pct-syskeys 0` chỉ tắt phím hệ thống, không tắt cử chỉ vuốt. Plan không đổi tham số của monkey: đóng bảng ở trình chạy đúng cho mọi nguyên nhân, còn monkey vẫn thử được cả cử chỉ ở mép.

**Lệnh `cmd statusbar collapse`:** là lệnh shell của dịch vụ `statusbar` (`StatusBarShellCommand` của Android, cùng nhóm với `expand-notifications`, `expand-settings`), không cần root. Emulator của CI là API 34 (`device-session.yml`). Máy cũ không có lệnh này thì `adb` trả mã khác 0 và trình chạy bỏ qua mã đó (mục 3.1), nên không có lệnh này cũng không làm hỏng `fresh`.

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0013 (hộp thoại đồng ý của bản staging, không đổi), ADR-0030 (đồng bộ dự án mẫu: trình chạy là file của dự án mẫu, nên đề xuất về mẫu).

## 3. Giải pháp

### 3.1. Cách làm

Bản cài thử dưới đây đã chạy với khối test của mục 4 (mục 4.1). Commit code của bước 4 là đúng `diff` này. Thứ tự: commit test (fake-adb, ảnh dump mẫu, `run.sh`) trước, chạy thấy bốn test thất bại, rồi commit code và tài liệu.

**`scripts/device-scenario.py`.** `dump()` cũ đổi tên thành `dump_once()`; `dump()` mới gọi nó, và khi mọi node là của SystemUI thì đóng bảng, ghi `note` (một lần mỗi bước, vì `self.notes` được đặt lại ở đầu mỗi bước) rồi đọc lại đúng một lần. Mọi chỗ đang gọi `dump()` (`next_screen`, `wait_consent_closed`, `step_consent`, lệnh `device`) đều được hưởng mà không phải sửa. `collapse_shade()` không đọc mã thoát: máy không có lệnh thì giữ nguyên màn hình. `adb` hết hạn thì ném `AdbTimeout` như mọi lệnh `adb` khác của bước.

```diff
diff --git a/scripts/device-scenario.py b/scripts/device-scenario.py
--- a/scripts/device-scenario.py
+++ b/scripts/device-scenario.py
@@ -33,7 +33,9 @@ Commands:
                                 with am start -W and wait for its process (pidof); without one, fail when the crash
                                 buffer names the app, otherwise start it once more ("mở lại" in the note); after the
                                 device was given back or changed by a device command it waits
-                                DEVICE_SCENARIO_STOP_SETTLE seconds (default 3) after stopping the app
+                                DEVICE_SCENARIO_STOP_SETTLE seconds (default 3) after stopping the app; before
+                                opening the app it closes the system shade (cmd statusbar collapse), which monkey
+                                may leave open
   consent decline [seconds]     tap "Do not consent" when the ad consent form shows (ADR-0013) and wait until it
                                 closes, or see the app root (DEVICE_SCENARIO_FIRST_SCREENS) without a form; consent
                                 is gathered asynchronously, so after the root it keeps looking for a late form for
@@ -111,6 +113,8 @@ TERMINATED_RESET_LIMIT = 25.0
 NIGHT_MODES = ("yes", "no", "auto")
 UNFOLDED_SIZE, UNFOLDED_DENSITY = "1766x2208", "420"  # 673 x 841 dp, the frame of the _unfolded screenshots
 IDLE_ERROR = "could not get idle state"
+SYSTEM_UI = "com.android.systemui"
+SHADE_NOTE = "bảng của hệ thống che app, đã đóng (cmd statusbar collapse)"
 RESULT_PASSED, RESULT_FAILED, RESULT_ENV = "đạt", "lỗi", "bỏ qua (môi trường)"
 RESULT_TIME, RESULT_SYNTAX = "bỏ qua (hết thời gian)", "lỗi tệp"
 RETRY = "chạy lại"  # not a result in the log: the first run of a file whose app the system killed
@@ -578,7 +582,21 @@ class Runner:
         return lines[-1].strip() if code == 0 and lines else ""
 
     def dump(self):
-        """(screen, None) or (None, reason)."""
+        """(screen, None) or (None, reason). A dump of the system UI only (the notification shade or quick settings
+        over the app, camera-pose FB-20261027-02) closes the shade once and reads the screen again."""
+        screen, reason = self.dump_once()
+        if screen is None or not screen.nodes or any(node.get("package") != SYSTEM_UI for node in screen.nodes):
+            return screen, reason
+        self.collapse_shade()
+        if SHADE_NOTE not in self.notes:
+            self.note(SHADE_NOTE)
+        return self.dump_once()
+
+    def collapse_shade(self):
+        """Closes the notification shade and quick settings; a device without the command keeps what it shows."""
+        self.adb("shell", "cmd", "statusbar", "collapse")
+
+    def dump_once(self):
         code, output = self.adb("shell", "uiautomator", "dump", "/sdcard/ds.xml")
         if IDLE_ERROR in output:
             return None, IDLE_ERROR
@@ -680,6 +698,7 @@ class Runner:
         if "/" not in self.activity:
             raise Fail(f"không tìm thấy màn hình khởi động của {self.package}")
         self.set_language()
+        self.collapse_shade()  # monkey runs before the plan scenarios and may leave the shade open over the app
         if self.device_reset:
             # Stopping an app whose Activity the reset is still rebuilding makes the system kill the next start
             # about a second later ("Destroy timeout of remove-task", camera-pose FB-20261025-02): start after that.
```

**`scripts/device-session.sh`.** Chỉ biểu thức lọc của mục "Ghi chú của trình chạy" và chú thích của nó.

```diff
diff --git a/scripts/device-session.sh b/scripts/device-session.sh
--- a/scripts/device-session.sh
+++ b/scripts/device-session.sh
@@ -346,10 +346,11 @@ print_plan_scenarios() {
     echo "Kịch bản theo plan: không có"
     echo
   fi
-  # Notes of the runner that are out of the ordinary: a late consent form it declined, an app it had to start again.
+  # Notes of the runner that are out of the ordinary: a late consent form it declined, an app it had to start again,
+  # the system shade it closed.
   local notes
   notes="$(sed -n 's/^plan \([^ ]*\) note step=\([0-9]*\) t=[^ ]* \(.*\)$/- \1, bước \2: \3/p' "${dir}/scenarios.txt" |
-    grep -E 'hộp thoại đồng ý hiện muộn|mở lại' || true)"
+    grep -E 'hộp thoại đồng ý hiện muộn|mở lại|bảng của hệ thống' || true)"
   if [[ -n "${notes}" ]]; then
     echo "Ghi chú của trình chạy:"
     echo
```

**Tài liệu.** `docs/ops/test-sessions.md` mục 5: dòng `fresh` của bảng lệnh, câu về dòng `note` chép vào `summary.md`, và một đoạn mới "Bảng của hệ thống" ngay sau đoạn "Hộp thoại đồng ý".

```diff
diff --git a/docs/ops/test-sessions.md b/docs/ops/test-sessions.md
--- a/docs/ops/test-sessions.md
+++ b/docs/ops/test-sessions.md
@@ -103,7 +103,7 @@ Kết quả nằm trong thư mục phiên: `scenarios.txt` (mỗi bước một
 - `fresh`, `consent decline` và lệnh `device` ghi điều đã xảy ra (mở lại, đã chạm từ chối, ổn định sau bao nhiêu giây);
 - các bước khác chỉ có `t=`.
 
-Dòng `note` có `hộp thoại đồng ý hiện muộn` hay `mở lại` (chuyện bất thường) được chép vào `summary.md`, mục "Ghi chú của trình chạy:", ngay sau bảng, mỗi dòng `- <tệp>, bước <n>: <chi tiết>`. Không có thì mục này không hiện. Khi đọc một tệp lỗi, xem các dòng `note` của nó cùng `fail.png`.
+Dòng `note` có `hộp thoại đồng ý hiện muộn`, `mở lại` hay `bảng của hệ thống` (chuyện bất thường) được chép vào `summary.md`, mục "Ghi chú của trình chạy:", ngay sau bảng, mỗi dòng `- <tệp>, bước <n>: <chi tiết>`. Không có thì mục này không hiện. Khi đọc một tệp lỗi, xem các dòng `note` của nó cùng `fail.png`.
 
 **Phần đầu** (ba dòng, trước mọi lệnh): `ticket: T-NNN`, `criterion: <điều tệp chứng minh>`, `timeout: <giây>` (mặc định 180, tối đa 300). Dòng trống và dòng bắt đầu bằng `#` bị bỏ qua.
 
@@ -133,7 +133,7 @@ Lý do: chữ của chip Compose nằm trong một nút con, còn `selected` n
 
 | Lệnh | Việc |
 | --- | --- |
-| `fresh` | Dừng app, xóa dữ liệu, cấp quyền runtime, mở app bằng `am start -W` rồi chờ tiến trình của app (`pidof`, chỉ nhận đầu ra toàn số). Không có tiến trình thì ghi `note`; bộ đệm crash có tên app thì `FAIL app crash ở lần mở đầu`, không thì dừng app và mở lại một lần. Vẫn không có thì `FAIL app không chạy sau 2 lần mở`. Tệp đứng sau một lần trả máy thì chờ `DEVICE_SCENARIO_STOP_SETTLE` giây (mặc định 3) sau khi dừng app rồi mới mở: dừng app khi Activity còn đang dựng lại sau lần trả máy làm hệ thống giết lần mở ngay sau đó (`Destroy timeout of remove-task`). Cũng chờ sau khi dừng app khi tệp đã có lệnh `device` trước đó. Trước khi mở, đặt ngôn ngữ riêng của app là tiếng Anh (`en`), vì `str=` đọc `res/values/` (tiếng Anh); dưới Android 13 máy phải để tiếng Anh, không thì tệp `bỏ qua (môi trường)` (camera-pose T-054) |
+| `fresh` | Dừng app, xóa dữ liệu, cấp quyền runtime, mở app bằng `am start -W` rồi chờ tiến trình của app (`pidof`, chỉ nhận đầu ra toàn số). Không có tiến trình thì ghi `note`; bộ đệm crash có tên app thì `FAIL app crash ở lần mở đầu`, không thì dừng app và mở lại một lần. Vẫn không có thì `FAIL app không chạy sau 2 lần mở`. Tệp đứng sau một lần trả máy thì chờ `DEVICE_SCENARIO_STOP_SETTLE` giây (mặc định 3) sau khi dừng app rồi mới mở: dừng app khi Activity còn đang dựng lại sau lần trả máy làm hệ thống giết lần mở ngay sau đó (`Destroy timeout of remove-task`). Cũng chờ sau khi dừng app khi tệp đã có lệnh `device` trước đó. Trước khi mở, đặt ngôn ngữ riêng của app là tiếng Anh (`en`), vì `str=` đọc `res/values/` (tiếng Anh); dưới Android 13 máy phải để tiếng Anh, không thì tệp `bỏ qua (môi trường)` (camera-pose T-054). Ngay trước khi mở, đóng bảng của hệ thống (`cmd statusbar collapse`, camera-pose T-070) |
 | `consent decline [giây]` | Chạm nút từ chối của hộp thoại đồng ý quảng cáo (ADR-0013) khi nó hiện, rồi chờ hộp thoại đóng; hoặc thấy luôn app mà không có hộp thoại. "Thấy app" là bộ chọn `DEVICE_SCENARIO_FIRST_SCREENS`, mặc định `id=app_root` (tag của `Surface` gốc trong `MainActivity`); app có màn đầu riêng thì đặt bộ chọn theo màn đó. App hỏi đồng ý không đồng bộ nên hộp thoại có thể hiện sau màn đầu vài giây: thấy app rồi, lệnh vẫn tìm hộp thoại thêm `DEVICE_SCENARIO_CONSENT_GRACE` giây (mặc định 8) và từ chối nếu nó hiện. Hộp thoại hiện muộn hơn nữa: xem Hộp thoại đồng ý, dưới |
 | `wait <bộ chọn> [giây]` | Chờ tới khi khớp (mặc định 20 giây) |
 | `need <bộ chọn> [giây]` | Như `wait`, nhưng hết giờ thì tệp kết thúc `bỏ qua (môi trường)`. Dùng cho điều kiện mà emulator CI có thể không có (camera trên máy không có camera, mạng) |
@@ -154,6 +154,10 @@ Sau mỗi lệnh `device`, trình chạy đọc màn hình tới khi hai lần d
 - Tệp chưa có bước `consent decline` (hoặc vừa `fresh`) thì trình chạy không chạm hộp thoại, nên vẫn viết được kịch bản kiểm chính hộp thoại.
 - Trạng thái này không đi qua ranh giới tệp, và `fresh` đặt lại nó.
 
+**Bảng của hệ thống.** Monkey chạy ngay trước các tệp kịch bản và có thể vuốt từ mép trên, để bảng thông báo hay cài đặt nhanh mở đè lên app; khi đó mọi lần dump chỉ có node của `com.android.systemui` (camera-pose FB-20261027-02). Màn hình khóa cũng chỉ có node của `com.android.systemui`: khi đó lệnh đóng bảng không có tác dụng, dòng `note` vẫn ghi, và tệp lỗi như trước.
+- `fresh` đóng bảng (`adb shell cmd statusbar collapse`) trước khi mở app.
+- Một lần dump chỉ có node của `com.android.systemui`: trình chạy đóng bảng một lần, đọc lại màn hình và ghi `note ... bảng của hệ thống che app, đã đóng (cmd statusbar collapse)` (một lần mỗi bước). Bảng vẫn còn thì bước xử lý màn hình đọc lại như mọi màn hình không có phần tử nó tìm.
+
 Ví dụ ngắn:
 
 ```text
```

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/device-scenario.py` | `SYSTEM_UI`, `SHADE_NOTE`; `dump()` đóng bảng một lần khi dump chỉ có SystemUI; `dump_once()`; `collapse_shade()`; `step_fresh` đóng bảng trước khi mở app; docstring của lệnh `fresh` |
| Sửa | `scripts/device-session.sh` | Mục "Ghi chú của trình chạy" của `summary.md` lọc cả `bảng của hệ thống` |
| Sửa | `scripts/tests/fake-adb` | `FAKE_ADB_SHADE`: bảng mở tới khi có `cmd statusbar collapse` |
| Tạo | `scripts/tests/fixtures/device-scenario/shade.xml` | Dump chỉ có node của `com.android.systemui` |
| Sửa | `scripts/tests/run.sh` | Bốn test mới (mục 4) |
| Sửa | `docs/ops/test-sessions.md` | Mục 5: lệnh `fresh`, dòng `note`, đoạn "Bảng của hệ thống" |
| Sửa | `docs/features/quality-gate/tickets/T-070-dong-bang-he-thong.md` | Trạng thái theo quy trình |
| Sửa | `docs/features/quality-gate/feedback/FB-20261027-02-bang-he-thong-che-kich-ban.md` | `fixed` sau khi merge (mục 4, dòng cuối) |

**Nơi khác đọc các file plan này sửa (L-020), và vì sao không bị ảnh hưởng:**

- **`.github/workflows/device-session.yml`** gọi `scripts/device-session.sh start ci`, `scenarios --yes`, `stop` và nhắc `docs/ops/test-sessions.md` trong chú thích. Plan không đổi lệnh con, tham số hay tệp ra nào của `device-session.sh` (chỉ biểu thức lọc ghi chú trong `summary.md`), nên workflow không đổi.
- **`.github/workflows/pr-check.yml`** chạy cả tệp `scripts/tests/run.sh`; plan chỉ thêm test, không đổi cách gọi hay công cụ cần có (`bash`, `python3`, `jq`).
- **`scripts/tests/app.sh`** được `run.sh` nạp trong cùng shell trước dòng tổng kết; nó không dùng `ds_run`, và không có tên `ds_first` hay `shade` (`grep` trả 0 dòng), nên hàm mới `ds_first` không đè tên nào của nó.
- **`scripts/local-setup.sh`** nhắc `scripts/tests/run.sh` trong chú thích (các biến test của nó), không chạy nó.
- **`app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ui/UiAutomatorTagsTest.kt`** nhắc `scripts/device-scenario.py` trong chú thích, không chạy nó; cách chọn phần tử theo `resource-id` không đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có. Code đã chạy với test ở mục 4.1. Nếu ở `device-session` sau khi merge mà `cmd statusbar collapse` không đóng được bảng trên emulator, việc sửa tiếp là một ticket mới (ví dụ `input keyevent KEYCODE_BACK` hay `service call statusbar 2`), vì phiên của task này không có emulator để kiểm (`device-session.yml` chỉ chạy trên nhánh chính).

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` không có gì để liệt kê: plan không sửa code Kotlin. Hành vi của script mà test có sẵn kiểm:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `fresh` gửi một dãy lệnh `adb` cố định | Thêm `shell cmd statusbar collapse` trước `am start -W` | Không có test nào so cả dãy lệnh của `fresh`; các test đếm từng lệnh (`am start -W`, `set-app-locales`) bằng `ds_count` | Giữ: đã chạy cả bộ `scripts/tests/run.sh` với code mới, mọi test cũ qua (mục 4.1) |
| Mục "Ghi chú của trình chạy" chỉ lọc `hộp thoại đồng ý hiện muộn`, `mở lại` | Lọc thêm `bảng của hệ thống` | `device-session: summary lists the runner notes about consent and restarts` (dòng `note` của bước 1 chỉ có `t=` vẫn không hiện) | Giữ |

## 4. Kiểm thử

Khung test: `ds_run` của `scripts/tests/run.sh` với `scripts/tests/fake-adb`. `fake-adb` thêm biến `FAKE_ADB_SHADE`: tên một dump chỉ có node của SystemUI; khi biến được đặt, mọi `uiautomator dump` trả dump đó (không làm tiến dãy `FAKE_ADB_UI_SEQ`) cho tới khi có `shell cmd statusbar collapse` (ghi tệp `shade-closed` trong `FAKE_ADB_STATE_DIR`). Đó là đúng điều đã xảy ra ở phiên: bảng mở từ trước khi tệp chạy và chỉ đóng khi có người đóng.

```diff
diff --git a/scripts/tests/fake-adb b/scripts/tests/fake-adb
--- a/scripts/tests/fake-adb
+++ b/scripts/tests/fake-adb
@@ -26,6 +26,9 @@
 # alive; a hanging subcommand writes its pid to hang.pid and, with FAKE_ADB_ALIVE_AFTER_HANG=1, leaves the process
 # alive first (an app that starts slowly but runs). When $FAKE_ADB_PIDOF_TEXT is set, `pidof` prints it and returns 0
 # (camera-pose T-049: an adb error with digits, or several pids).
+# $FAKE_ADB_SHADE names a dump of the system shade (only com.android.systemui nodes) that covers the app: until
+# `cmd statusbar collapse` runs (file shade-closed in $FAKE_ADB_STATE_DIR), `uiautomator dump` shows it and keeps the
+# place in $FAKE_ADB_UI_SEQ (camera-pose T-070).
 [[ -n "${FAKE_ADB_CALLS:-}" ]] && echo "$*" >> "${FAKE_ADB_CALLS}"
 case "$1" in
   get-state) [[ -n "${FAKE_ADB_NO_DEVICE:-}" ]] && exit 1; echo device ;;
@@ -47,8 +50,15 @@ case "$1" in
       exec sleep 300
     fi
     case "$*" in
+      "cmd statusbar collapse")
+        mkdir -p "${FAKE_ADB_STATE_DIR:-${TMPDIR:-/tmp}}"
+        : > "${FAKE_ADB_STATE_DIR:-${TMPDIR:-/tmp}}/shade-closed" ;;
       "uiautomator dump"*)
-        if [[ -n "${FAKE_ADB_UI_SEQ:-}" ]]; then
+        if [[ -n "${FAKE_ADB_SHADE:-}" && ! -f "${FAKE_ADB_STATE_DIR:-${TMPDIR:-/tmp}}/shade-closed" ]]; then
+          mkdir -p "${FAKE_ADB_STATE_DIR:-${TMPDIR:-/tmp}}"
+          echo "${FAKE_ADB_SHADE}" > "${FAKE_ADB_STATE_DIR:-${TMPDIR:-/tmp}}/ui-seq.cur"
+          echo "UI hierarchy dumped to: /sdcard/ds.xml"
+        elif [[ -n "${FAKE_ADB_UI_SEQ:-}" ]]; then
           state="${FAKE_ADB_STATE_DIR:-${TMPDIR:-/tmp}}"
           mkdir -p "$state"
           read -r -a files <<<"${FAKE_ADB_UI_SEQ}"
```

Ảnh dump mẫu `scripts/tests/fixtures/device-scenario/shade.xml` (hai node, cả hai `package="com.android.systemui"`):

```xml
<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
<hierarchy rotation="0">
<node index="0" text="" resource-id="com.android.systemui:id/notification_panel" class="android.widget.FrameLayout" package="com.android.systemui" content-desc="" checkable="false" checked="false" clickable="false" enabled="true" focusable="false" focused="false" scrollable="false" long-clickable="false" password="false" selected="false" bounds="[0,0][1080,2400]"><node index="0" text="Internet" resource-id="com.android.systemui:id/tile_label" class="android.widget.TextView" package="com.android.systemui" content-desc="" checkable="false" checked="false" clickable="true" enabled="true" focusable="true" focused="false" scrollable="false" long-clickable="false" password="false" selected="false" bounds="[60,300][500,420]"></node></node>
</hierarchy>
```

Bốn test mới, đặt ngay trước khối `ds_script cards.txt ...` (ba test của trình chạy) và ngay trước khối "camera-pose T-054: a file run again ..." của phần `summary` (test thứ tư):

```diff
diff --git a/scripts/tests/run.sh b/scripts/tests/run.sh
--- a/scripts/tests/run.sh
+++ b/scripts/tests/run.sh
@@ -3025,6 +3025,17 @@ ds_script none.txt 'consent decline 1'
 expect "device-scenario: consent decline fails when nothing shows" "0 1 1" \
   "$(ds_run "blank.xml" "$ds_files/none.txt") $(ds_count 'step=1 consent decline 1 FAIL') $(ds_count 'result=lỗi')"
 
+# The system shade left open over the app (monkey swipes from the top edge) hides the app from every dump:
+# fresh closes it before opening the app, and a dump of the system UI only closes it once more (FB-20261027-02).
+ds_first() { local n; n="$(grep -n -m1 -- "$1" "$ds_calls" | cut -d: -f1)"; echo "${n:-999999}"; } # line of the first matching adb call, 999999 when none
+expect "device-scenario: fresh closes the system shade before opening the app" "0 1 yes" \
+  "$(ds_run "consent.xml welcome.xml home.xml" "$ds_fixtures/scenarios/ok.txt" FAKE_ADB_SHADE="$ds_fixtures/shade.xml") $(ds_count 'result=đạt') $( (( $(ds_first '^shell cmd statusbar collapse$') < $(ds_first '^shell am start -W') )) && echo yes)"
+ds_script shade.txt 'fresh' 'expect id=home_screen'
+expect "device-scenario: a dump of the system UI only closes the shade and reads again" "0 1 2 1" \
+  "$(ds_run "shade.xml home.xml" "$ds_files/shade.txt") $(ds_count 'result=đạt') $(ds_count '^shell cmd statusbar collapse$' "$ds_calls") $(ds_count 'note step=2 .*bảng của hệ thống che app')"
+expect "device-scenario: a shade that stays open is closed once per dump, then the step fails" "0 1 2" \
+  "$(ds_run "shade.xml" "$ds_files/shade.txt") $(ds_count 'step=2 expect id=home_screen FAIL') $(ds_count '^shell cmd statusbar collapse$' "$ds_calls")"
+
 ds_script cards.txt 'tap idp=gallery_card_ in=gallery_grid index=2'
 expect "device-scenario: idp= in= index= pick the second card of the grid" "0 1" \
   "$(ds_run "grid.xml" "$ds_files/cards.txt") $(ds_count '^shell input tap 540 600$' "$ds_calls")"
@@ -3505,6 +3516,12 @@ expect "device-session: summary lists the runner notes about consent and restart
   "$(joined "$notes_code" "$(notes_cell 'Ghi chú của trình chạy:')" \
     "$(notes_cell '- ok.txt, bước 2: hộp thoại đồng ý hiện muộn, đã từ chối')" "$(notes_cell '- ok.txt, bước 1')" \
     "$(cell 'Ghi chú của trình chạy')")"
+# camera-pose T-070: the system shade the runner closed is listed too, so a reader sees why a step took longer.
+mkplan "$work/plan-shade" "$plan_lines
+plan ok.txt note step=2 t=0.4 bảng của hệ thống che app, đã đóng (cmd statusbar collapse)" "$(cat "$fixtures/logcat-clean.txt")"
+shade_code="$(session /dev/null summary "$work/plan-shade" com.example.app.staging)"
+expect "device-session: summary lists the system shade the runner closed" "0 1" \
+  "$(joined "$shade_code" "$(grep -cF -- '- ok.txt, bước 2: bảng của hệ thống che app, đã đóng (cmd statusbar collapse)' "$work/plan-shade/summary.md" || true)")"
 # camera-pose T-054: a file run again after the system killed the app: the table has the second result and no failing step,
 # and the note of the run again is listed (it says "mở lại").
 mkplan "$work/plan-retry" 'plan killed.txt ticket=T-022 criterion=Mẫu
```

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| `fresh` đóng bảng trước khi mở app; `ok.txt` đạt khi bảng mở từ trước | Test của script (`scripts/tests/run.sh`) | `device-scenario: fresh closes the system shade before opening the app`: mã 0, một dòng `result=đạt`, lệnh `shell cmd statusbar collapse` đầu tiên đứng trước `shell am start -W` đầu tiên trong `ds_calls`. Code cũ: `0 0 ` (bước 2 `FAIL không thấy hộp thoại đồng ý hay màn hình đầu của app`, đúng lỗi của phiên) |
| Dump chỉ có SystemUI: đóng một lần, đọc lại, ghi `note` | Test của script | `device-scenario: a dump of the system UI only closes the shade and reads again`: tệp `fresh`, `expect id=home_screen` trên dãy `shade.xml home.xml`; mã 0, `result=đạt`, đúng 2 lệnh `collapse` (của `fresh` và của lần dump), một dòng `note step=2 ...bảng của hệ thống che app`. Code cũ: `0 0 0 0` |
| Bảng không đóng được: đóng một lần mỗi lần dump, bước lỗi như cũ | Test của script | `device-scenario: a shade that stays open is closed once per dump, then the step fails`: dãy chỉ có `shade.xml`; `step=2 expect id=home_screen FAIL`, đúng 2 lệnh `collapse` (`expect` đọc màn hình một lần). Code cũ: `0 1 0` (bước lỗi, không có lệnh đóng) |
| `summary.md` liệt kê dòng `note` về bảng | Test của script | `device-session: summary lists the system shade the runner closed`. Code cũ: `0 0` |
| Không làm hỏng phần khác | Test của script | Cả bộ `scripts/tests/run.sh` qua với `GIT_CONFIG_GLOBAL=/dev/null` (L-087); `scripts/check.sh` |
| Bảng thật trên emulator được đóng, kịch bản bắt đầu từ app; tiêu chí kịch bản của T-062 và T-064 được kiểm lại | Kiểm tra thủ công (emulator: `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt`, `feature/camera/src/deviceScenarios/T-020-camera-mo-gap.txt`, `feature/camera/src/deviceScenarios/T-064-mot-camera.txt`) | `device-session` cuối phiên có T-070 merge: bảng "Kịch bản theo plan" không có tệp nào lỗi ở bước `consent decline` với `fail.xml` chỉ có node của `com.android.systemui`; hai tệp camera của T-062 khác `bỏ qua (môi trường)`; kết quả của `T-064-mot-camera.txt` ghi vào báo cáo. Có dòng "Ghi chú của trình chạy" về bảng thì ghi vào báo cáo (xác nhận giả thuyết monkey). Đạt thì FB-20261027-02 sang `fixed` trên nhánh báo cáo |
| Không làm hỏng các tệp khác | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`, `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`, `app/src/deviceScenarios/T-056-man-lon-xoay-ngang.txt`) | `device-session` cuối phiên: crash 0, ANR 0; ba tệp không lỗi vì bảng của hệ thống |

**Ngoại lệ viết test trước:** đoạn tài liệu `docs/ops/test-sessions.md` (không có hành vi để test).

### 4.1. Chạy thử

- **Đã chạy thử:** bản cài thử ở mục 3.1 cùng test ở mục 4, ngày 2026-10-28, trên nhánh chính `df6a4a2`, không commit.
- **Lệnh:** `GIT_CONFIG_GLOBAL=/dev/null bash scripts/tests/run.sh` (Temurin 21, macOS), hai lần: với code cũ (chỉ có test, fake-adb và ảnh dump mẫu) và với code mới. `scripts/check.sh` chưa chạy (máy viết plan không có Android SDK đầy đủ); phiên chạy ở bước 5.
- **Kết quả:** code cũ: đúng bốn test mới thất bại, với giá trị ghi ở bảng mục 4; mọi test khác qua. Code mới: `All script tests passed`. Lệnh `cmd statusbar collapse` thật chưa chạy trên emulator (mục 3.4).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7, CI và quy trình phiên tự chạy (`ci`): trình chạy kịch bản chạy ở `device-session` cuối mỗi phiên. Theo `docs/risk-scoring.md`, plan chạm điểm chặn là rủi ro cao, cần duyệt chéo; bảng dưới chỉ để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script, khoảng 20 dòng code, cộng test và tài liệu |
| B. Độ rõ của yêu cầu | 0 | Bằng chứng của phiên rõ (`fail.xml`, `fail.png`, logcat); code và test có sẵn trong plan |
| C. Khả năng kiểm chứng tự động | 1 | Logic kiểm bằng fake-adb; lệnh thật chỉ kiểm được ở `device-session` sau merge |
| D. Ảnh hưởng chéo | 1 | Mọi tệp kịch bản đi qua `fresh` và `dump()`; một lệnh `adb` thêm mỗi `fresh` |
| E. Độ mới của kỹ thuật | 1 | `cmd statusbar collapse` lần đầu dùng trong dự án, nhưng là lệnh shell chuẩn của Android, không cần root |
| **Tổng** | 4 | **Mức: Cao** (điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/device-scenario.py`, `scripts/device-session.sh`, `scripts/tests/fake-adb`, `scripts/tests/run.sh` (file của dự án mẫu). Đề xuất về dự án mẫu qua issue `from-app` (`docs/ops/from-app.md`); ghi vào mô tả PR.
- Task khác có thể xung đột: task khác cùng sửa `scripts/tests/run.sh` hay `fake-adb` trong cùng phiên (chèn ở hai mốc khác nhau, nên mốc dời chỗ thì làm theo ADR-0032/L3).
- Đề xuất thứ tự chạy: không phụ thuộc task nào; đặt sớm trong phiên để `device-session` cuối phiên kiểm được.

## 7. Điều kiện dừng riêng

Không có.

## 8. Câu hỏi cho người duyệt

1. Có muốn chặn luôn nguồn gây bảng mở ở monkey không (ví dụ thêm `--pct-touch`/`--pct-motion` hay chạy `cmd statusbar collapse` ngay sau monkey trong `device-session.sh`)? AI đề xuất không: đóng ở `fresh` đã đủ cho mọi nguyên nhân, và giữ monkey như cũ thì các lần chạy so sánh được với nhau.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
