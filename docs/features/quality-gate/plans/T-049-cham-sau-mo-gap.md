---
ticket: T-049
title: Lệnh chạm sau `device unfold` tới được app; ba điểm còn lại của T-048
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 7M           # cỡ S (4.4M, scripts/token-usage.py estimate) cộng khoảng 60% vì 11 test với adb giả và lần chạy thử trên emulator của T-048 (12.4M cho cỡ M)
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-049: LỆNH CHẠM SAU `device unfold` TỚI ĐƯỢC APP; BA ĐIỂM CÒN LẠI CỦA T-048

## 1. Tóm tắt

FB-20261025-01 nghi tọa độ của `input tap` lệch với `bounds` của `uiautomator` khi có `wm size`. Đã kiểm trên một emulator giống CI (API 34, màn 320x640, 160 dpi, cài mới): giả thuyết đó sai. Nguyên nhân thật là hộp thoại "See and do more" mà Android hiện lần đầu một app khóa hướng dọc bị đóng khung trên màn lớn. Lệnh chạm đầu tiên chỉ đóng hộp thoại; `uiautomator dump` chỉ đọc cửa sổ của app nên không thấy nó. Ảnh `unfolded.png` của lần chạy CI 40179423644 có đúng hộp thoại đó.

Plan sửa `scripts/device-scenario.py`:
- `device unfold` tắt hộp thoại giới thiệu (`wm set-letterbox-style --isEducationEnabled false`) trước khi đổi kích thước; trả máy sau mỗi tệp đặt lại (`wm reset-letterbox-style isEducationEnabled`);
- `device unfold` chọn kích thước nằm trong giới hạn của Android (3 lần cạnh dài của màn thật), để khung vẫn là 673 x 841 dp;
- ba điểm còn lại của rà soát T-048: câu chữ quy tắc số mơ hồ, `consent decline` lần hai sau khi đã từ chối, `pidof` chỉ nhận số.

Kiểm chứng:
- 11 test mới với `adb` giả, đã chạy trên code hiện tại (7 thất bại đúng lý do, 4 đối chứng qua);
- bản cài thử trên code hiện tại qua cả 11 test và toàn bộ `scripts/tests/run.sh` (mục 4.1);
- `device-session` đầu tiên sau khi merge.

Không sửa `.github/`, `scripts/device-session.sh` hay app.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ. Yêu cầu là FB-20261025-01 và mục Cần người kiểm tra của báo cáo RUN-20261025-01 (điểm nên sửa và ghi chú của rà soát T-048).

| Lỗi hay điểm rà soát | Tiêu chí chấp nhận | Cách hiểu của AI (bằng chứng) |
| --- | --- | --- |
| FB-20261025-01: T-024 bước 14, lệnh chạm đầu tiên sau `device unfold` không tới chip "Đã lưu" | `device unfold` chạy `wm set-letterbox-style --isEducationEnabled false` trước `wm size`. Trả máy sau mỗi tệp (và cuối lượt) chạy `wm reset-letterbox-style isEducationEnabled` | Mục 4.1, phần Chẩn đoán. Tọa độ đúng: `dumpsys input` cho thấy lệnh chạm `593,383` được đổi thành `(107.5, 215.2)` trên màn thật, nằm trong cửa sổ app `[45,146][276,464]`. Lần chạm thứ hai thì có tác dụng. Trên emulator cài mới: có giới thiệu thì lần chạm đầu mất, tắt giới thiệu thì lần chạm đầu có tác dụng |
| `device unfold` trên emulator 320x640 không ra khung 673 x 841 dp | Đọc `Physical size` ở `read_display`. Khi 2208 lớn hơn 3 lần cạnh dài của màn thật, chọn mật độ `d = 160 * giới_hạn // 841` và kích thước `ceil(673 * d / 160) x ceil(841 * d / 160)`. Trên 320x640: `1536x1919` ở 365 dpi. Không đọc được kích thước thật hay màn đủ lớn thì giữ `1766x2208` ở 420 | Logcat CI: `WindowManager: Using new display size: 1766x1920`, cấu hình `w673dp h722dp`. Android giới hạn kích thước ép ở 3 lần cạnh dài (`DisplayContent.setForcedSize`, 3 x 640 = 1920). Trên emulator thử: `wm size 1536x1919`, `wm density 365` cho `sw673dp w673dp h757dp` (phần còn lại của 841 là thanh trạng thái và thanh tác vụ) |
| Nên sửa của rà soát T-048: câu chữ quy tắc số mơ hồ | Câu báo lỗi, docstring đầu `scripts/device-scenario.py` và `docs/ops/test-sessions.md` mục 5 nói: mọi `text=` hay `desc=` không ngoặc trên dòng kết thúc bằng một số là mơ hồ, kể cả một từ; ghi giá trị trong ngoặc kép, ví dụ `desc="Back" 10`. Hành vi không đổi | Code đã báo lỗi cho `wait desc=Back 10` (mục 4.1); chỉ câu chữ thiếu. Câu báo lỗi vẫn có "ngoặc kép", vì test có sẵn `a text with spaces followed by a number is a file error` tìm chữ đó |
| Ghi chú của rà soát T-048: `consent decline` lần hai sau khi đã từ chối chạm lại mà không lỗi | Trong cùng `fresh`, bước `consent decline` thấy hộp thoại khi đã `declined` thì `FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối` (`ConsentFail`), không chạm. Thấy màn đầu của app khi đã `declined` thì giữ `declined`, không đổi thành `late` | Code hiện tại chạm lần hai (mục 4.1, 2 lần chạm). Nếu không giữ `declined`, một hộp thoại hiện sau bước `consent decline` thứ hai sẽ bị tự từ chối thêm lần nữa thay vì lỗi (cùng lý do của ADR-0013 ở plan T-048 mục 8 câu 4) |
| Ghi chú của rà soát T-048: `fresh` coi mọi chữ số trong đầu ra `pidof` là có tiến trình | Chỉ đầu ra gồm các số cách nhau bằng khoảng trắng (`4242`, `4242 4243`) là có tiến trình. Đầu ra khác (`error: device 'emulator-5554' not found`) tính như `pidof` rỗng | Code hiện tại: `re.search(r"\d", output)`, nên lỗi `adb` có số cổng thành có tiến trình (mục 4.1) |

Các ghi chú còn lại của rà soát T-048 không làm ở đây: chúng đúng plan T-048 hoặc chỉ theo dõi ở `device-session` (khung 5 giây của tự từ chối, ngoặc một phần, dump treo trong lúc chờ ổn định, thứ tự trả máy khi SIGTERM, chữ của dòng `note`).

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0013 (hộp thoại đồng ý), ADR-0020 (khung `_unfolded` 673 x 841).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu có sẵn của `scripts/device-scenario.py` (plan T-046, T-048).

**Hộp thoại giới thiệu đóng khung.**
- `step_device`, nhánh `unfold`: thêm `self.adb("shell", "wm", "set-letterbox-style", "--isEducationEnabled", "false")` trước hai lệnh `wm size`, `wm density`. Nhánh `fold` không bật lại; trả máy bật lại.
- `reset_device`: thêm `["shell", "wm", "reset-letterbox-style", "isEducationEnabled"]` ngay sau lệnh `wm density`. Lệnh này đặt về mặc định của máy. Danh sách có 7 lệnh trong hạn tổng `DEVICE_SCENARIO_RESET_LIMIT` (30 giây, 5 giây mỗi lệnh), không đổi hạn.
- Trên máy thật của dev mà dev đã tự đổi cài đặt này, trả máy đặt về mặc định. Ghi điều này vào `docs/ops/test-sessions.md` mục 5.

**Kích thước mở gập.**
- Hàm mới `unfolded_display(physical)` ở mức module, trước `class AdbTimeout`:

  ```python
  def unfolded_display(physical):
      """(size, density) for 673 x 841 dp: 1766x2208 at 420, or smaller when Android would cut 2208 down to three
      times the longer side of the screen (a 320x640 emulator allows 1920)."""
      limit = 3 * max(physical) if physical else None
      if limit is None or 2208 <= limit:
          return UNFOLDED_SIZE, UNFOLDED_DENSITY
      density = 160 * limit // 841
      return f"{-(-673 * density // 160)}x{-(-841 * density // 160)}", str(density)
  ```

- `read_display`: đọc thêm `Physical size: <w>x<h>` vào `self.physical_size` (mặc định `None`, đặt trong `__init__`). Không đọc được thì giữ `None`.
- `step_device` nhánh `unfold` dùng `unfolded_display(self.physical_size)`.
- `fake-adb`: `wm size` in `Physical size: ${FAKE_ADB_PHYSICAL_SIZE:-1080x2400}`, `wm density` in `Physical density: ${FAKE_ADB_PHYSICAL_DENSITY:-420}`. Mặc định không đổi, nên test có sẵn (`device commands change the device and the reset undoes them` kiểm `wm size 1766x2208`, `wm density 420`) không đổi.

**Ba điểm của T-048.**
- `selector_and_number`: câu báo lỗi thành `mọi text= hay desc= không ngoặc trên dòng kết thúc bằng một số là mơ hồ, kể cả một từ: ghi giá trị trong ngoặc kép, ví dụ desc="Back" 10`. Dòng docstring đầu tệp ("An unquoted text= or desc= with a space ...") đổi theo: "An unquoted text= or desc= on a line that ends in a number is ambiguous, even one word: quote it (desc="Back" 10)". `ambiguous()` giữ nguyên (docstring của nó sửa theo).
- `step_consent`: khi thấy nút từ chối mà `self.consent_state == "declined"` thì `raise ConsentFail(CONSENT_AGAIN)` trước khi chạm. Khi thấy màn đầu của app mà `self.consent_state == "declined"` thì trả về, không đặt `"late"`. Docstring đầu tệp (lệnh `consent decline`) thêm: "a second consent decline that sees the form after a decline fails".
- `start_app`: `re.search(r"\d", output)` thành `re.fullmatch(r"\s*\d+(\s+\d+)*\s*", output)`.
- `fake-adb`: `pidof` in `$FAKE_ADB_PIDOF_TEXT` rồi thoát 0 khi biến được đặt (trước nhánh trạng thái có sẵn). Chú thích đầu tệp ghi ba biến mới.

**Tài liệu.** `docs/ops/test-sessions.md` mục 5:
- đoạn **Ngoặc kép**: câu quy tắc số mơ hồ như trên, thêm ví dụ `wait desc=Back 10`;
- lệnh `device unfold|fold`: tắt hộp thoại giới thiệu đóng khung của hệ thống (lần chạm đầu sau khi mở gập chỉ đóng nó, và `uiautomator` không thấy nó, FB-20261025-01); trên màn nhỏ dùng kích thước nhỏ hơn với cùng khung dp; trả máy đặt lại cài đặt này về mặc định;
- đoạn Hộp thoại đồng ý: `consent decline` lần hai thấy hộp thoại sau khi đã từ chối là lỗi.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/device-scenario.py` | `unfolded_display`; `physical_size` trong `__init__` và `read_display`; nhánh `unfold` của `step_device`; lệnh `reset-letterbox-style` trong `reset_device`; câu báo lỗi của `selector_and_number`, docstring của `ambiguous`; `step_consent` với `declined`; `start_app` `fullmatch`; docstring đầu tệp |
| Sửa | `scripts/tests/fake-adb` | `FAKE_ADB_PHYSICAL_SIZE`, `FAKE_ADB_PHYSICAL_DENSITY`, `FAKE_ADB_PIDOF_TEXT`; chú thích đầu tệp |
| Sửa | `scripts/tests/run.sh` | Khối `device-scenario`: 11 test của mục 4, thêm ngay trước dòng `# Plan T-048: notes of the runner about a late consent form or a restarted app are listed under the table.`, với chú thích `# Plan T-049: ...`; tệp kịch bản tạm ghi bằng `ds_script`, không thêm fixture |
| Sửa | `docs/ops/test-sessions.md` | Mục 5: ba điểm ở cuối mục 3.1 |

**Nơi khác đọc các file plan này sửa (L-020), và vì sao không bị ảnh hưởng:**
- **`scripts/plan-check.py`** nạp `scripts/device-scenario.py` và gọi `check_files`. Chỉ câu chữ của lỗi số mơ hồ đổi; dạng `<tệp> dòng <n>: <lý do>` giữ nguyên. Test quy tắc 11 có sẵn không có dòng mơ hồ.
- **`scripts/device-session.sh`** gọi trình chạy và đọc `scenarios.txt`. Dạng dòng log không đổi. Khối test `device-session` của `run.sh` dùng `fake-adb` với giá trị mặc định, nên `wm size`/`wm density` in như cũ.
- **`scripts/app-explore/`** và test của nó dùng `fake-adb`: không đặt ba biến mới, hành vi như cũ.
- **`.github/workflows/device-session.yml`** chạy `device-session.sh`; lệnh không đổi. Hai lệnh `wm` thêm vào mỗi tệp có `device unfold` (chỉ T-024), vài giây.
- **`app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ui/UiAutomatorTagsTest.kt`** nhắc `scripts/device-scenario.py` trong chú thích (lý do bật `testTagsAsResourceId`), không chạy nó. Plan không đổi cách đọc `resource-id`.
- **`scripts/tests/run.sh`** được `scripts/check.sh` chạy (cả tệp), và được `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/wait-run.sh` nhắc trong chú thích. Plan chỉ thêm test vào khối `device-scenario`; cách gọi, hàm `ds_run` và dòng tổng kết không đổi.
- **Kịch bản của repo**: không sửa. Test có sẵn `the scenarios of the repository pass check` chạy với câu báo lỗi mới; ba tệp không có dòng mơ hồ.
- Đã chạy toàn bộ `scripts/tests/run.sh` với bản cài thử của plan (mục 4.1): `All script tests passed`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm. Trình chạy vẫn chỉ chạm nút từ chối của hộp thoại đồng ý có sẵn (ADR-0013), trên emulator.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Lệnh `wm set-letterbox-style` có sẵn trong Android 13 trở lên; trên máy cũ hơn lệnh in lỗi và trình chạy đi tiếp (kết quả của `self.adb` không được kiểm, như hai lệnh `wm` có sẵn).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** emulator CI (x86_64, `google_apis`) có cùng hộp thoại giới thiệu và cùng lệnh `wm set-letterbox-style` như emulator thử (arm64, `google_apis`, cùng API 34). `unfolded.png` của CI cho thấy cùng hộp thoại, nên khả năng cao là có.
- **Điều kiện chuyển:** ở `device-session` đầu sau khi merge, T-024 vẫn lỗi ở bước 14, và `unfold`-`fold` ảnh vẫn có hộp thoại "See and do more", hoặc `scenarios.txt` có lỗi của lệnh `wm set-letterbox-style`.
- **Cách làm thay** (task sau, không cài sẵn): sau `device unfold`, chạm một lần vào vùng đóng khung ngoài app (vùng nền đen bên trái, không phải nút của app) để đóng hộp thoại, rồi dump lại. Phiên không dừng vì điều này (mục 7, điều 3).

## 4. Kiểm thử

Test viết trước code: 11 dòng "Test script" dưới đây, thêm vào khối `device-scenario` của `scripts/tests/run.sh`, chạy với `adb` giả. Bước 3 (khung rỗng) chỉ thêm ba biến của `fake-adb`, không đổi `device-scenario.py`. Kết quả thật của từng test trên code hiện tại nằm ở mục 4.1. Dòng ghi **(đối chứng)** qua được cả trên code cũ (L-035) và không tính vào số test thất bại trước.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Câu báo lỗi số mơ hồ | Test script | `device-scenario: an unquoted one-word desc= on a line that ends in a number is ambiguous, the message says so`: `ds_script one-word.txt 'wait desc=Back 10'`; `ds_check` mã 1 và `grep -c 'one-word.txt dòng 4: .*kể cả một từ.*ngoặc kép' "$work/ds-check.out"` là 1 |
| Ngoặc kép một từ | Test script (đối chứng) | `device-scenario: control: the quoted one-word value passes check`: `wait desc="Back" 10`, `ds_check` mã 0 |
| `consent decline` lần hai thấy hộp thoại | Test script | `device-scenario: a second consent decline that sees the form again after a decline fails, tapping once`: tệp `consent decline 5`, `consent decline 5`; chuỗi `consent.xml onboarding.xml consent.xml onboarding.xml`: mã 0, đúng 1 lệnh `input tap`, dòng `^plan consent-twice-step.txt step=2 consent decline 5 FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối$` |
| Giữ `declined` | Test script | `device-scenario: a second consent decline without a form keeps the decline, a form after it is a failure`: tệp `consent decline 5`, `consent decline 5`, `wait id=camera_screen 3`; chuỗi `consent.xml onboarding.xml onboarding.xml consent.xml`: 1 lệnh chạm, dòng `step=3 wait id=camera_screen 3 FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối` |
| Lần hai không có hộp thoại | Test script (đối chứng) | `device-scenario: control: a second consent decline without a form passes`: tệp `consent decline 5`, `consent decline 5`, `expect id=onboarding_screen`; chuỗi `consent.xml onboarding.xml onboarding.xml`: 1 lệnh chạm, `result=đạt` |
| `pidof` lỗi có số | Test script | `device-scenario: fresh does not take an adb error with digits for a process`: tệp `fresh` (`fresh.txt` của khối T-048), `FAKE_ADB_PIDOF_TEXT="error: device 'emulator-5554' not found" DEVICE_SCENARIO_PROCESS_WAIT=0.3`: 2 lệnh `am start -W`, dòng `step=1 fresh FAIL app không chạy sau 2 lần mở` |
| `pidof` nhiều số | Test script (đối chứng) | `device-scenario: control: pidof with two pids counts as a process`: `FAKE_ADB_PIDOF_TEXT="4242 4243"`: 1 lệnh `am start -W`, `step=1 fresh ok` |
| Tắt giới thiệu trước khi đổi kích thước | Test script | `device-scenario: device unfold turns the letterbox education off before it changes the size`: tệp `device unfold` (`unfold-edu.txt`), chuỗi `camera.xml grid.xml grid.xml`: 1 dòng `^shell wm set-letterbox-style --isEducationEnabled false$`, đứng trước `^shell wm size 1766x2208$` (`ds_in_order`) |
| Trả máy đặt lại | Test script | `device-scenario: the reset after a file turns the letterbox education back to the default`: cùng tệp: ít nhất 1 dòng `^shell wm reset-letterbox-style isEducationEnabled$` (trả máy chạy sau tệp và cuối lượt, nên có 2), đứng sau `wm size 1766x2208` |
| Màn nhỏ | Test script | `device-scenario: on a 320x640 screen unfold picks a size within three times the screen, 673 x 841 dp`: cùng tệp, `FAKE_ADB_PHYSICAL_SIZE=320x640 FAKE_ADB_PHYSICAL_DENSITY=160`: 1 dòng `^shell wm size 1536x1919$`, 1 dòng `^shell wm density 365$`, 0 dòng `wm size 1766x2208` |
| Màn lớn giữ như cũ | Test script (đối chứng) | `device-scenario: control: on a large screen unfold keeps 1766x2208 at 420`: cùng tệp, `fake-adb` mặc định (1080x2400): 1 dòng `wm size 1766x2208`, 1 dòng `wm density 420` |
| Test có sẵn | Test script | Toàn bộ `scripts/tests/run.sh` có sẵn phải còn qua, gồm `a text with spaces followed by a number is a file error` (tìm "ngoặc kép"), `device commands change the device and the reset undoes them`, `the device is reset after a failing file` (ít nhất 6 lệnh trả máy), các test `consent` và `fresh` của T-048 |
| T-024 qua bước 14 | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`) | `device-session` đầu tiên sau khi merge: dòng T-024 của bảng "Kịch bản theo plan" không lỗi ở bước 14; `unfolded.png` không có hộp thoại "See and do more". Lỗi ở bước sau là phát hiện mới, ghi vào báo cáo (mục 7, điều 3) |
| Phiên làm theo tài liệu | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt chéo đọc diff `docs/ops/test-sessions.md` mục 5 |

**Ngoại lệ viết test trước:** `docs/ops/test-sessions.md` là tài liệu; docstring của `scripts/device-scenario.py` là chú thích.

### 4.1. Chạy thử

Tiêu chí E là 1: lệnh `wm set-letterbox-style` mới với repo, đã chạy thử trên emulator.

**Chẩn đoán FB-20261025-01 trên emulator** (2026-10-25, máy dev macOS arm64, emulator 37.2 với `system-images;android-34;google_apis;arm64-v8a`, AVD `hw.lcd.width=320`, `hw.lcd.height=640`, `hw.lcd.density=160`, `hw.camera.back=none`; bản `:app:assembleStagingDebug` của nhánh chính `05a6a5f`; SDK và AVD ở scratchpad, không vào repo):
- `wm size 1766x2208` cho `Override size: 1766x1920`. `dumpsys display`: `real 1766 x 1920`. Khớp logcat CI `Using new display size: 1766x1920`.
- `uiautomator dump` sau khi mở gập: chữ "Đã lưu" `[538,357][648,410]`, gốc `[246,0][1521,1920]`, đúng như `fail.xml` của CI.
- Lần chạm `input tap 593 383` đầu tiên: không có `VGL.Poses: Filter:`. `dumpsys input`: `logicalSize=1766x1920`, biến đổi của màn `5.5187 ... -807.8386`, cửa sổ app `frame=[45,146][276,464]`; sự kiện được tiêm có tọa độ `(107.5, 215.2)`, nằm trong cửa sổ app. Tọa độ đúng.
- Lần chạm thứ hai cùng điểm: `VGL.Poses: Filter: saved`. Mở gập lại (máy đã thấy hộp thoại) rồi chạm sau 10 giây và 30 giây: lần chạm đầu có tác dụng cả hai lần.
- `unfolded.png` của CI (lần chạy 40179423644): hộp thoại hệ thống "See and do more", "Double-tap outside an app to reposition it", nút "Got it", phủ lên thư viện. `fail.xml` không có nút nào của hộp thoại.
- Đối chứng trên emulator cài mới (`-wipe-data`), cùng các bước tới thư viện rồi mở gập: giới thiệu bật (mặc định): lần chạm đầu **mất**; `wm set-letterbox-style --isEducationEnabled false` trước khi mở gập: lần chạm đầu **có tác dụng** (`Filter: saved`), ảnh chụp không có hộp thoại.
- `wm size 1536x1919` và `wm density 365`: cấu hình `sw673dp w673dp h757dp 365dpi`, không bị cắt.

**Chạy các test mới trên code hiện tại** (L-075; 2026-10-25, nhánh chính `05a6a5f`, macOS, bash 3.2). Khối tạm ở scratchpad nạp phần đầu của `run.sh` (hàm `expect`), các hàm `ds_*`, rồi 11 test của mục 4; `fake-adb` là bản có ba biến mới. Không commit.

| Test | Code hiện tại | Lý do |
| --- | --- | --- |
| một từ `desc=Back 10` | thất bại `'1 0'` | `check` đã báo lỗi (mã 1), câu cũ không có "kể cả một từ" |
| đối chứng `desc="Back" 10` | qua `0` | |
| `consent decline` lần hai thấy hộp thoại | thất bại `'0 2 0'` | chạm lần hai, bước 2 `ok` |
| giữ `declined` | thất bại `'0 2 0'` | bước 2 đặt `late`, bước 3 tự từ chối lần nữa (2 lần chạm), rồi lỗi `không đóng` thay vì `hiện lại` |
| đối chứng lần hai không có hộp thoại | qua `0 1 1` | |
| `pidof` lỗi có số | thất bại `'0 1 0'` | một lần mở, `fresh ok` |
| đối chứng `pidof` hai số | qua `0 1 1` | |
| tắt giới thiệu trước `wm size` | thất bại `'0 0 no'` | không có lệnh |
| trả máy đặt lại | thất bại `'0 no no'` | không có lệnh |
| màn 320x640 | thất bại `'0 0 0 1'` | vẫn gửi `1766x2208` |
| đối chứng màn lớn | qua `0 1 1` | |

Tổng: 7 thất bại đúng lý do, 4 đối chứng qua.

**Bản cài thử** (không commit, đã trả về bằng `git checkout`): sửa `scripts/device-scenario.py` đúng như mục 3.1 và `fake-adb` như mục 3.2. Kết quả: 11/11 test mới qua; toàn bộ `scripts/tests/run.sh` (với `scratchpad/env.sh`): `All script tests passed`. Lần đầu bản cài thử làm test "trả máy đặt lại" ra `0 2 yes` vì trả máy chạy hai lần (sau tệp và cuối lượt); test trong bảng mục 4 đã sửa thành "ít nhất 1".

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI và quy trình phiên tự chạy). Trình chạy kịch bản chạy ở `device-session` cuối mỗi phiên.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script (vài hàm), `fake-adb` ba biến, test, một mục tài liệu |
| B. Độ rõ của yêu cầu | 0 | Nguyên nhân đã tái hiện và đối chứng trên emulator; ba điểm còn lại có câu chữ đề xuất sẵn |
| C. Khả năng kiểm chứng tự động | 2 | Mọi thay đổi có test với `adb` giả; hộp thoại hệ thống chỉ kiểm được trên emulator sau khi merge |
| D. Ảnh hưởng chéo | 1 | `fake-adb` dùng chung, biến mới mặc định như cũ; `plan-check.py` dùng `check_files`, chỉ câu chữ đổi |
| E. Độ mới của kỹ thuật | 1 | `wm set-letterbox-style` mới với repo, đã chạy thử (mục 4.1) |
| **Tổng** | 5 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:** `scripts/device-scenario.py` (`device-session` cuối mỗi phiên, `plan-check.py` quy tắc 11), `scripts/tests/fake-adb`, `scripts/tests/run.sh`.
- **Task khác có thể xung đột:** chưa có ticket nào khác `ready` (2026-10-25). Nếu danh sách chạy có thêm plan sửa `scripts/tests/run.sh`, mỗi plan thêm khối riêng; task chạy lần lượt, mỗi task tạo nhánh từ nhánh chính mới nhất.
- **Việc sau task này, không làm ở đây:**
  - **Quy tắc 11 thành lỗi**, khi một `device-session` có cả 3 kịch bản không `lỗi` (plan T-048, mục 6).
  - **FB-20261025-01:** sau khi merge, chủ feature điền mục Phân loại (đề xuất của AI: lỗi của cách giả lập gập, không phải của app; liên kết T-049).
  - **FB-20261024-01** (khóa hướng dọc trên màn lớn): nếu bỏ khóa, app không còn bị đóng khung và hộp thoại giới thiệu không hiện nữa; phần tắt giới thiệu vô hại.
- **Dự án mẫu.** PR VGL-Studio/app-project-template#152 (trình chạy kịch bản của T-046) chưa có `device unfold` kiểu này. Đề xuất về mẫu (`docs/ops/from-app.md`) cùng các điểm của T-048; ghi vào mô tả PR.
- **Đề xuất thứ tự chạy:** độc lập. `device-session` cuối phiên là lần kiểm thật.

## 7. Điều kiện dừng riêng

1. Cần sửa `.github/workflows/device-session.yml` (ví dụ đổi cỡ màn của emulator): dừng, viết plan riêng.
2. Một test có sẵn của khối `device-scenario`, `device-session` hay `plan-check` (quy tắc 11) phải sửa thì mới qua: dừng.
3. Không phải điều kiện dừng: ở `device-session` cuối chính phiên này hay lần đầu sau khi merge, T-024 vẫn lỗi (mục 3.4) hoặc lỗi ở bước khác. Phiên chỉ ghi vào báo cáo kèm các dòng `note` và ảnh; crash và ANR vẫn chặn staging như cũ.

## 8. Câu hỏi cho người duyệt

1. **Kích thước mở gập trên màn nhỏ.** Trên emulator CI, Android cắt `1766x2208` thành `1766x1920`, nên khung là 673 x 731 dp thay vì 673 x 841 dp. Plan đổi sang `1536x1919` ở 365 dpi (cùng khung dp) khi màn thật nhỏ; trên máy thật lớn vẫn là `1766x2208` ở 420. **Đề xuất của AI:** làm trong ticket này (đã kiểm trên emulator, có test, không đổi gì trên máy lớn). Phương án khác: để nguyên, chỉ tắt hộp thoại giới thiệu.
2. **Trả máy đặt lại cài đặt giới thiệu về mặc định**, kể cả trên máy thật của dev đã tự đổi nó. **Đề xuất của AI:** chấp nhận (cài đặt chỉ dành cho kiểm thử, giống `wm size reset` có sẵn), ghi vào tài liệu. Phương án khác: đọc giá trị cũ bằng `wm get-letterbox-style` và trả lại đúng giá trị đó (thêm một lệnh và một đoạn đọc chữ).
3. **FB-20261025-01.** Kết luận của plan khác giả thuyết trong phản hồi. **Đề xuất của AI:** không sửa phản hồi trong PR plan này; phiên làm T-049 ghi kết luận và liên kết vào mục Phân loại khi chủ feature đồng ý, hoặc chủ feature tự ghi.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
