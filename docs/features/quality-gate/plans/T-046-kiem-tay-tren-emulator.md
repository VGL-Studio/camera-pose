---
ticket: T-046
title: Tiêu chí kiểm thủ công chạy được trên emulator thành kịch bản device-session
status: done             # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 13M          # cỡ M (10.4M, scripts/token-usage.py estimate) cộng hạn chót, bộ chọn và test thêm sau rà soát độc lập
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-24
---

# PLAN T-046: TIÊU CHÍ KIỂM THỦ CÔNG CHẠY ĐƯỢC TRÊN EMULATOR THÀNH KỊCH BẢN DEVICE-SESSION

## 1. Tóm tắt

Hiện mỗi phiên tự chạy để lại nhiều mục kiểm thủ công. Phần lớn chạy được trên emulator, nhưng vẫn phải chờ người (L-062). Plan này làm bốn việc:

1. Mỗi dòng kiểm thủ công ở mục 4 của plan ghi nơi chạy: `(emulator: <tệp>)`, `(máy thật: <lý do>)` hay `(người: <lý do>)`.
2. Phần chạy được trên emulator thành tệp kịch bản `feature/<feature>/src/deviceScenarios/*.txt`.
3. `scripts/device-session.sh scenarios` chạy các tệp đó sau kịch bản chuẩn, qua trình chạy mới `scripts/device-scenario.py`:
   - đọc màn hình bằng `uiautomator dump`, chọn phần tử theo `testTag`;
   - có hạn chót tuyệt đối cho mọi lệnh `adb`;
   - ghi kết quả, thời gian và ảnh vào thư mục phiên, và in một bảng vào `summary.md`.
4. Phiên đọc bảng đó ở cuối phiên. Chỉ phần máy thật và phần cần người còn lại cho người.

Một số điểm chính:
- Không sửa `.github/`.
- Vỏ app bật `testTagsAsResourceId` ở gốc giao diện và ở hộp thoại mạng.
- Quy tắc 11 mới của `plan-check.py` chỉ ở mức chú ý, cho tới khi một `device-session` chạy cả 3 kịch bản đầu tiên đạt.

Kiểm chứng:
- test trình chạy với `adb` giả (kể cả `adb` treo);
- test quy tắc 11;
- test Robolectric cho thuộc tính;
- `check` trên các tệp kịch bản của repo chạy ở mọi PR đổi `feature/**`;
- 3 kịch bản đầu tiên chạy thật ở `device-session` đầu tiên sau khi merge.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là bài học L-062 (`docs/lessons.md`) và phạm vi của ticket.

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Mẫu plan ghi nơi chạy | Mỗi dòng "Kiểm tra thủ công" ở mục 4 ghi một trong ba dạng: `(emulator: <đường dẫn tệp kịch bản>)`, `(máy thật: <lý do>)`, `(người: <lý do>)` | Ghi ngay ở cột "Loại test" của bảng có sẵn, không thêm cột "Chạy ở" như ticket viết, để plan cũ vẫn đọc được (lệch ticket, đã sửa phạm vi ticket, người xác nhận ở mục 8). "Người" dành cho việc không phải chạy app, như đọc tài liệu hay xem ảnh chuẩn |
| `plan-check.py` nhắc điều đó | Thiếu ghi chú nơi chạy, dòng emulator mà tệp không có trong mục 3.2 hay repo, tệp sai cú pháp: in **chú ý** cho mọi plan, `draft` lẫn `approved` | Quyết định của Tuấn (2026-10-24): chú ý cho tới khi có một `device-session` chạy cả 3 kịch bản đạt. Đổi thành lỗi với plan `draft` là một dòng sửa riêng sau đó (mục 3.1, 6), không làm trong task này |
| Kịch bản chạy trong `device-session` | Mọi tệp `feature/*/src/deviceScenarios/*.txt` và `app/src/deviceScenarios/*.txt` được chạy sau kịch bản chuẩn, mỗi tệp từ trạng thái mới cài, trong hạn chót; `summary.md` có bảng "Kịch bản theo plan" | Không đổi workflow: `scenarios --yes` gọi trình chạy |
| Mức của kết quả | Kịch bản lỗi không chặn staging. Thiếu điều kiện môi trường (lệnh `need`) thì là `bỏ qua (môi trường)`, không soạn phản hồi. Crash hay ANR trong lúc chạy kịch bản vẫn chặn như hiện nay | Câu hỏi 1 ở mục 8, đã trả lời |
| Phiên đọc kết quả | `auto-run.md` mục 4: mỗi dòng `lỗi` là một phát hiện có ảnh, dump và bước lỗi, kèm phản hồi `FB-...` `nen-sua`. Tiêu chí bố cục chỉ đánh dấu đạt sau khi phiên xem ảnh `shot`. "Cần người kiểm tra" chỉ còn phần máy thật, phần người, và kịch bản lỗi | Mẫu báo cáo `docs/templates/run.md` có dòng kết quả kịch bản |
| Kịch bản đầu tiên | Các mục còn chờ của RUN-20261023-04 chạy được trên emulator thành kịch bản | T-022 bước 1 đến 7, T-024 bước 1 đến 4, phần bố cục của T-020. Gập dùng `wm size` / `wm density`. T-018, T-023 (quảng cáo theo Remote Config, bản staging dùng file Firebase giả, L-002) giữ "máy thật: Remote Config" dù ticket ban đầu có liệt kê: lệch ticket, phạm vi ticket đã sửa, báo cáo phiên nhắc lại |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0019 (log), ADR-0012 (cấu hình staging giả), ADR-0013 (hộp thoại đồng ý quảng cáo phủ lên app khi mới cài).

## 3. Giải pháp

### 3.1. Cách làm

**Tệp kịch bản.** Đặt ở `feature/<feature>/src/deviceScenarios/<T-NNN>-<tên>.txt`, hoặc `app/src/deviceScenarios/` cho luồng của vỏ app.
- Không đặt trong `docs/`: `pr-check` (bộ lọc `code`) và `main-guard` (`paths-ignore: docs/**`) không chạy với PR chỉ đổi `docs/`, nên lỗi cú pháp hay `testTag` bị đổi tên sẽ không được kiểm. `feature/**` và `app/**` thì có.
- Gradle không đọc thư mục `src/deviceScenarios/`.

Cú pháp của tệp:
- Mỗi dòng là một lệnh; dòng trống và dòng bắt đầu bằng `#` bị bỏ qua.
- Phần đầu bắt buộc, ba dòng:

  ```text
  ticket: T-022
  criterion: Luồng thật trên máy, bước 1 đến 7 (plan T-022, mục 4)
  timeout: 240
  ```

  `criterion` có thể chứa `|`; khi in vào bảng của `summary.md`, ký tự này được đổi thành `/`. `timeout` tính bằng giây, mặc định 180, tối đa 300.

**Bộ chọn phần tử.** Một từ khóa chính:

| Từ khóa | Khớp với |
| --- | --- |
| `id=<testTag>` | `resource-id` đúng bằng tag |
| `idp=<tiền tố>` | `resource-id` bắt đầu bằng tiền tố, ví dụ `idp=poses_card_` cho `PosesTestTags.card(id)` |
| `desc=<chuỗi>` | `content-desc` |
| `text=<chuỗi>` | chữ hiển thị |
| `str=<tên chuỗi>` | giá trị trong `res/values/strings.xml` của mọi module (chữ emulator en-US hiện), khớp cả `text` lẫn `content-desc` |

Bộ lọc thêm, tùy chọn:
- `in=<testTag>`: chỉ tìm trong cây con của phần tử có tag đó;
- `index=<n>`: phần tử thứ n theo thứ tự trong dump, đếm từ 1;
- `selected=true`: thuộc tính `selected` hoặc `checked` của nút là `true`;
- `clickable=true`.

Nhiều bộ chọn ngăn bằng ` | ` nghĩa là khớp một trong số đó.

**Các lệnh:**

| Lệnh | Việc |
| --- | --- |
| `fresh` | Xóa dữ liệu, cấp quyền runtime như kịch bản chuẩn (`grant_permissions`), mở app |
| `consent decline [giây]` | Chờ tới N giây (mặc định 20) cho tới khi thấy hoặc nút từ chối của hộp thoại đồng ý (`text=Do not consent \| text=Không đồng ý`), rồi chạm nút đó; hoặc `id=onboarding_screen \| id=camera_screen`, tức không có hộp thoại. Hết giờ mà không thấy gì: `FAIL` |
| `wait <bộ chọn> [giây]` | Chờ tới khi khớp, mặc định 20 giây |
| `need <bộ chọn> [giây]` | Như `wait`, nhưng hết giờ thì tệp kết thúc với kết quả `bỏ qua (môi trường)`, kèm bộ chọn. Dùng cho điều kiện mà emulator CI có thể không có (camera) |
| `tap <bộ chọn>` / `tap_if <bộ chọn>` | Chạm vào tâm của `bounds`; `tap_if` bỏ qua khi không thấy |
| `scroll <bộ chọn> [lần]` | Vuốt lên trong vùng của phần tử, mặc định 3 lần |
| `expect <bộ chọn>` / `expect_no <bộ chọn>` | Kiểm có hay không có ngay lúc đó, sau một lần dump |
| `count <bộ chọn> <n>` | Đúng n phần tử khớp, ví dụ số cột của lưới |
| `network off\|on` | `network_off`, `network_on` của `device-session.sh` (tắt cả Wi-Fi và dữ liệu, chờ không còn mạng mặc định, L-031). `network off` không tắt được mạng thì kết quả `bỏ qua (môi trường)`, như kịch bản chuẩn |
| `device rotate landscape\|portrait` | `settings put system accelerometer_rotation 0`, rồi `user_rotation 1\|0` |
| `device night on\|off` | `cmd uimode night yes\|no` |
| `device unfold` / `device fold` | `unfold` đặt `wm size 1766x2208` và `wm density 420` (673 × 841 dp, như khung `_unfolded`); `fold` trả về kích thước và mật độ gốc |
| `device locale <thẻ>` | `cmd locale set-app-locales <gói> --locales <thẻ>`, ví dụ `en-US` (Android 13 trở lên) |
| `back` | Phím Back |
| `shot <tên>` | Chụp màn hình vào `<thư mục phiên>/scenarios/<tệp>/<tên>.png` |

**`check <tệp>...`** (không cần máy) kiểm:
- phần đầu, lệnh, tham số và bộ chọn đúng cú pháp;
- mỗi `id=` hay `idp=` có trong một tệp `*.kt` dưới `app/`, `feature/`, `core/` (chuỗi trong ngoặc kép, hoặc tiền tố của nó);
- mỗi `str=` có trong một `res/values/strings.xml`.

Lỗi in dạng `<tệp> dòng <n>: <lý do>`, mã 1. `plan-check.py` dùng chung hàm đọc này.

**Hạn chót của trình chạy** (`run --session <thư mục> --package <gói>`):
- **Ngân sách cả lượt:** `DEVICE_SCENARIOS_BUDGET`, mặc định 600 giây, đo bằng đồng hồ đơn điệu từ lúc trình chạy bắt đầu.
- **Hạn của mỗi tệp:** `min(timeout của tệp, ngân sách còn lại)`. Các lệnh chờ (`wait`, `need`, `consent`) không chờ quá hạn đó.
- **Mỗi lệnh `adb`:** chạy bằng `subprocess.run(..., timeout=min(30, thời gian còn lại của tệp))`. Hết giờ thì bước đó là `FAIL adb_timeout`, phần còn lại của tệp bị bỏ, trình chạy sang tệp sau.
- **Trả máy về gốc:** cũng là lệnh `adb` có giới hạn (mỗi lệnh 10 giây), chạy kể cả khi hết ngân sách.
- **Hết ngân sách:** tệp chưa chạy ghi `bỏ qua (hết thời gian)`.
- **`device-session.sh` gọi trình chạy** bằng `timeout $((DEVICE_SCENARIOS_BUDGET + 120))` khi máy có lệnh `timeout`.
- **Trình chạy trả mã khác 0 hay bị giết:** ghi dòng `plan runner_error`, và `scenarios` vẫn kết thúc bình thường.

Vì vậy phần kịch bản theo plan không làm job dài thêm quá khoảng 12 phút: hiện job chạy khoảng 15 đến 25 phút, giới hạn 50 phút.

**Ghi kết quả.**
- **Mở đầu mỗi tệp:** ghi dấu `VGL.Session scenario=plan file=<tệp>` vào log (`mark`), để AI cắt đúng đoạn log, và đếm crash, ANR theo từng tệp.
- **Mỗi bước:** một dòng `plan <tệp> step=<n> <lệnh> ok|FAIL <lý do>` trong `<thư mục phiên>/scenarios.txt`.
- **Cuối tệp:** một dòng `plan <tệp> result=<đạt|lỗi|bỏ qua (môi trường)|bỏ qua (hết thời gian)|lỗi tệp> seconds=<n>`. Thời gian từng tệp giúp chỉnh ngân sách sau.
- **Bước đầu tiên lỗi:** chụp `fail.png`, giữ `fail.xml` (bản dump cuối), bỏ phần còn lại của tệp.
- **Đầu lượt:** đọc kích thước và mật độ gốc (`wm size`, `wm density`).
- **Sau mỗi tệp,** dù đạt hay không, đưa máy về gốc:
  - `wm size reset`, `wm density reset`, hoặc đặt lại giá trị gốc nếu máy có `Override size`;
  - `accelerometer_rotation 1` và `user_rotation 0`;
  - `cmd uimode night no`;
  - bật lại mạng;
  - xóa ngôn ngữ riêng của app.
- **Đọc dump:** `adb shell uiautomator dump /sdcard/ds.xml`, rồi `adb exec-out cat /sdcard/ds.xml`, như `scripts/app-explore/ax.sh`.
  - Dump báo `ERROR: could not get idle state.` (màn có hiệu ứng chạy mãi) thì thử lại sau 1 giây, trong hạn của lệnh.
  - Hết hạn thì lý do là `could not get idle state`.
- **Thư mục kịch bản:** `DEVICE_SCENARIOS_GLOB`, mặc định `feature/*/src/deviceScenarios/*.txt app/src/deviceScenarios/*.txt`, để test dùng thư mục riêng. Glob do trình chạy giải bằng `glob` của Python (không để shell bung). Glob không khớp tệp nào (thư mục rỗng) thì trình chạy thoát trước mọi lệnh `adb`, kể cả bước đọc `wm size` đầu lượt: không cấp quyền, không đổi mạng, không gọi `adb`, `summary.md` ghi "Kịch bản theo plan: không có".
- **Test có sẵn không đổi:** khối test `device-session` của `scripts/tests/run.sh` đặt `export DEVICE_SCENARIOS_GLOB="$work/no-scenarios/*.txt"` (thư mục rỗng) cùng dòng `export DEVICE_SESSION_SLEEP=0` có sẵn, và bỏ nó ở dòng `unset` cuối khối. Chỉ các test mới của bước 4, bảng `summary.md` và `_helper` đặt glob riêng. Đây là thiết lập môi trường, không đổi điều test có sẵn kiểm (các test đếm quyền, đếm lần bật mạng giữ nguyên số) (phiên bản 2).

**`scripts/device-session.sh`.**
- **`cmd_scenarios`:** thêm bước 4 cuối hàm, sau monkey: gọi trình chạy (mục trên). Lệnh gọi lấy từ `DEVICE_SCENARIO_CMD`, mặc định `python3 scripts/device-scenario.py`, để test thay bằng lệnh hỏng.
- **Lệnh mới `helper network-off|network-on|grant-permissions`:** trình chạy gọi lại các hàm có sẵn, để chỉ có một cách tắt mạng và cấp quyền.
  - Lệnh được ghi trong phần Usage, khối chú thích đầu tệp (dòng 6 đến 15).
  - `usage()` in đúng đoạn đó (`sed -n '6,15p'`); khối Usage dài thêm thì sửa số dòng trong `usage()` cho khớp.
- **`cmd_summary`:** thêm mục "## Kịch bản theo plan", đọc các dòng `plan ... result=` của `scenarios.txt`.
  - Bảng gồm Tệp, Ticket, Tiêu chí, Kết quả, Bước lỗi, Giây, Crash/ANR trong tệp.
  - Cột Crash/ANR: số `FATAL EXCEPTION` / `ANR in` của gói, giữa hai dấu `scenario=plan file=` liên tiếp.
  - Không có tệp kịch bản nào: dòng "Kịch bản theo plan: không có".
  - Mức của dòng `lỗi` là "xem". Biến `blocking` không đổi: crash và ANR trong cả log (gồm cả lúc chạy kịch bản) vẫn chặn.

**Vỏ app.**
- **Tệp mới `app/src/main/.../ui/UiAutomatorTags.kt`:** hàm `Modifier.exposeTestTagsForUiAutomator()` đặt `semantics { testTagsAsResourceId = true }`.
- **Gắn hàm ở hai chỗ:**
  - `MainActivity`: `Surface` gốc;
  - `NetworkDialogHost` của `:app` (`app/.../network/NetworkDialogHost.kt`): bọc `NetworkDialogScreen` trong `Dialog` bằng `Box(Modifier.exposeTestTagsForUiAutomator())`. `Dialog` là một cửa sổ khác, không nhận thuộc tính từ gốc. Không sửa `:core:designsystem`.
- **Hộp thoại và popup khác** (mỗi cửa sổ riêng) mà kịch bản cần tìm theo `id=` phải gắn hàm này ở chỗ mở chúng. Ghi điều này vào chú thích của hàm và vào `docs/ops/test-sessions.md`.
- **Tag kịch bản dùng, đã kiểm có trong code:**
  - `onboarding_screen`, `onboarding_enable_camera`;
  - `camera_screen`, `camera_poses` (`CameraControls.kt`, nút mở thư viện), `camera_grid_button`, `camera_grid` (chỉ có khi camera `Ready`), `camera_strip`;
  - `poses_screen`, `poses_grid`, `poses_card_` (tiền tố của `PosesTestTags.card(id)`);
  - `pose_detail_screen`, `pose_detail_image`, `pose_detail_bottom_bar`;
  - `network_dialog`, `network_dialog_retry` (`NetworkDialogTestTags` của `:core:designsystem`).
- **Chuỗi kịch bản dùng:** `poses_cd_save`, `poses_cd_unsave` (nút lưu trên thẻ và ở chi tiết), `poses_detail_shoot`, `poses_chip_saved`.

**Mẫu và tài liệu.**
- **`docs/templates/plan.md` mục 4:** dòng mẫu "Kiểm tra thủ công" thành ba dòng `Kiểm tra thủ công (emulator: <tệp>)`, `(máy thật: <lý do>)`, `(người: <lý do>)`. Chú thích liệt kê nhóm lý do "máy thật":
  - camera thật;
  - FPS, khung hình;
  - Play (cài từ Play, In-App Review);
  - Remote Config;
  - quảng cáo thật;
  - gập có bản lề;
  - cử chỉ nhiều ngón;
  - TalkBack;
  - khác, kèm lời giải thích.
- **`docs/CLAUDE.md`, mục Khi viết plan:** một gạch đầu dòng.
  - Tiêu chí kiểm thủ công chạy được trên emulator thì viết kịch bản (`docs/ops/test-sessions.md`, mục 5), đưa tệp vào mục 3.2, chạy `scripts/device-scenario.py check <tệp>`.
  - Tệp kịch bản là test chạy ở `device-session` cuối phiên, không "thất bại trước" được, nên ghi vào ngoại lệ viết test trước.
- **`docs/ops/test-sessions.md`:**
  - mục 4 thêm một đoạn về bước kịch bản theo plan và mức của kết quả;
  - mục mới 5 "Kịch bản theo plan": bảng bộ chọn, bảng lệnh, ví dụ, hạn chót, cách chạy trên máy dev (`start`, `scenarios --yes`, `stop`), điều cần biết về cửa sổ riêng;
  - mục "Quy tắc về dữ liệu" dời thành 6.
- **`docs/auto-run.md` mục 4, Kết thúc phiên:** ý "Phân tích thư mục phiên" thêm:
  - Đọc bảng "Kịch bản theo plan".
  - Dòng `lỗi`: một phát hiện (bước lỗi, `fail.png`, `fail.xml`, đoạn log sau dấu `scenario=plan file=`), soạn phản hồi `FB-...` mức `nen-sua`, ghi vào "Cần người kiểm tra".
  - Dòng `bỏ qua (môi trường)`: ghi vào báo cáo, không soạn phản hồi; dòng kiểm thủ công đó vẫn chờ người.
  - Dòng `lỗi tệp`, `bỏ qua (hết thời gian)`, `runner_error`: lỗi của kịch bản hay của trình chạy, ghi vào báo cáo.
  - Tiêu chí bố cục (vị trí, số cột, giao diện tối) chỉ ghi là đạt sau khi phiên mở ảnh `shot` của bước đó và đối chiếu với tiêu chí, hoặc khi kịch bản có lệnh `count` hay `expect` kiểm đúng điều đó.
  - Kịch bản lỗi không chặn phát staging.
  - Ý "Những gì người cần tự kiểm tra" (mục 5 của `docs/auto-run.md`; tìm theo chữ, số dòng lệch sau T-044, T-045) đổi thành "tiêu chí kiểm tra thủ công máy thật và người".
- **`docs/templates/run.md`:**
  - mục "Kiểm thử trên máy ảo" thêm dòng `- Kịch bản theo plan: <n đạt, n lỗi, n bỏ qua; tệp lỗi và bước>`;
  - "Cần người kiểm tra": dòng "Tiêu chí kiểm tra thủ công" thành "Tiêu chí kiểm tra thủ công (máy thật, người)", thêm dòng "Kịch bản emulator lỗi hay bỏ qua".
- **`docs/lessons.md`:** dòng Áp dụng của L-062 ghi T-046 và số PR, ở bước 10 của phiên.

**Ba kịch bản đầu tiên** (mục còn chờ của RUN-20261023-04). Viết đầy đủ ở đây; phiên chép đúng nội dung này. `need id=camera_poses` dùng thay `wait`: emulator CI tắt camera sau (`-camera-back none`), và chưa chắc màn camera hiện nút thư viện khi không có camera.

`feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt` (plan T-022, mục 4, bước 1 đến 7):

```text
ticket: T-022
criterion: Luồng thật trên máy, bước 1 đến 7 (plan T-022, mục 4)
timeout: 240
fresh
consent decline 30
wait id=onboarding_screen | id=camera_screen 60
tap_if id=onboarding_enable_camera
wait id=camera_screen 15
need id=camera_poses 15
# (1) thư viện, chạm thẻ: chi tiết mở, ảnh lớn hiện
tap id=camera_poses
wait id=poses_grid 20
tap idp=poses_card_ in=poses_grid index=1
wait id=pose_detail_screen 15
wait id=pose_detail_image 30
shot detail
# (2) cuộn hết nội dung, nút luôn thấy
scroll id=pose_detail_screen 5
expect id=pose_detail_bottom_bar
# (3) lưu ở chi tiết, quay lại: thẻ đã lưu
tap str=poses_cd_save in=pose_detail_screen
wait str=poses_cd_unsave in=pose_detail_screen 5
back
wait id=poses_grid 10
expect str=poses_cd_unsave in=poses_grid index=1
# (4) "Chụp với tư thế này": camera mở; Back ở camera thoát app
tap idp=poses_card_ in=poses_grid index=1
wait id=pose_detail_screen 15
tap str=poses_detail_shoot
wait id=camera_screen 15
shot camera-with-pose
back
expect_no id=camera_screen
# (5) mất mạng, mở chi tiết chưa tải ảnh lớn: hộp thoại mạng; bật mạng, Thử lại: ảnh hiện
fresh
consent decline 30
wait id=camera_screen | id=onboarding_screen 60
tap_if id=onboarding_enable_camera
need id=camera_poses 15
tap id=camera_poses
wait id=poses_grid 20
network off
tap idp=poses_card_ in=poses_grid index=3
wait id=network_dialog 30
shot offline
network on
tap id=network_dialog_retry
wait id=pose_detail_image 30
# (6) chế độ tối
device night on
wait id=pose_detail_screen 10
shot dark
# (7) tiếng Anh
device locale en-US
wait id=pose_detail_screen 15
shot english
```

`feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt` (plan T-024, mục 4, bước 1 đến 4; mở gập giả lập bằng đổi kích thước):

```text
ticket: T-024
criterion: Gập mở, bước 1 đến 4 (plan T-024, mục 4), bằng wm size thay cho gập thật
timeout: 180
fresh
consent decline 30
wait id=onboarding_screen | id=camera_screen 60
tap_if id=onboarding_enable_camera
need id=camera_poses 15
tap id=camera_poses
wait id=poses_grid 20
# lưu một thẻ để chip "Đã lưu" có nội dung
tap str=poses_cd_save in=poses_grid index=1
wait str=poses_cd_unsave in=poses_grid 5
# (1) mở gập ở thư viện: 3 cột; chọn chip, gập lại: chip giữ
device unfold
wait id=poses_grid 10
shot unfolded
tap str=poses_chip_saved
wait str=poses_chip_saved selected=true 5
device fold
wait id=poses_grid 10
expect str=poses_chip_saved selected=true
shot folded
device unfold
wait id=poses_grid 10
expect str=poses_chip_saved selected=true
# (2) chi tiết khi mở gập; gập lại: cùng tư thế
tap idp=poses_card_ in=poses_grid index=1
wait id=pose_detail_screen 15
expect id=pose_detail_bottom_bar
shot detail-unfolded
device fold
wait id=pose_detail_screen 10
shot detail-folded
# (3) "Chụp với tư thế này" khi mở gập
device unfold
wait id=pose_detail_screen 10
tap str=poses_detail_shoot
wait id=camera_screen 15
# (4) chế độ tối
device night on
shot camera-unfolded-dark
```

`feature/camera/src/deviceScenarios/T-020-camera-mo-gap.txt` (plan T-020, mục 4, phần bố cục; không có camera thật):

```text
ticket: T-020
criterion: Gập mở thật, phần bố cục camera (plan T-020, mục 4); không chụp
timeout: 150
fresh
consent decline 30
wait id=onboarding_screen | id=camera_screen 60
tap_if id=onboarding_enable_camera
wait id=camera_screen 15
need id=camera_grid_button 15
tap idp=camera_pose_ in=camera_strip index=2
tap id=camera_grid_button
wait id=camera_grid 5
device unfold
wait id=camera_screen 10
expect id=camera_grid
expect id=camera_strip
shot unfolded
device fold
wait id=camera_screen 10
expect id=camera_grid
expect id=camera_strip
shot folded
```

Phần "chụp, màn xem lại" của T-020 không chạy: không có camera thật. T-020 dùng `need id=camera_grid_button`, vì nút lưới chỉ có khi camera `Ready`.

**Không làm trong plan này.**
- Các mục giữ là "máy thật":
  - T-018, T-023: "máy thật: Remote Config", mặc dù ticket ban đầu liệt kê; báo cáo phiên nhắc lại điều này;
  - FPS, camera thật, chụp và màn xem lại;
  - Play In-App Review, TalkBack;
  - gập thật có bản lề, tư thế bàn, màn ngoài;
  - chụm hai ngón (T-015).
- Emulator gập thật cần đổi `device-session.yml` (`profile` của AVD), là ticket riêng.
- Đổi quy tắc 11 thành lỗi với plan `draft`: một dòng sửa riêng, sau khi một `device-session` chạy cả 3 kịch bản đạt.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/device-scenario.py` | `check` (cú pháp, `id=` có trong `*.kt`, `str=` có trong `strings.xml`) và `run`: bộ chọn, lệnh, hạn chót, timeout mỗi lệnh `adb`, ghi `scenarios.txt` có thời gian, ảnh, `fail.xml`, dấu log mỗi tệp, trả máy về gốc |
| Sửa | `scripts/device-session.sh` | `cmd_scenarios` bước 4 (gọi trình chạy qua `DEVICE_SCENARIO_CMD`, có `timeout`); lệnh `helper`; mục "Kịch bản theo plan" của `cmd_summary` (crash/ANR theo tệp, "không có"); khối Usage (dòng 6 đến 15) thêm `helper` và số dòng trong `usage()` cho khớp |
| Sửa | `scripts/tests/fake-adb` | Giữ nguyên hành vi hiện có, kể cả `exec-out cat` in `$FAKE_ADB_UI` khi không đặt chuỗi (test của `scripts/app-explore/`). Thêm: `$FAKE_ADB_UI_SEQ` (các tệp dump cách nhau bằng dấu cách, mỗi lần `uiautomator dump` lấy tệp kế tiếp, đếm qua tệp `$FAKE_ADB_UI_SEQ.n`, hết thì giữ tệp cuối); tệp trong chuỗi có tên `*.idle-error` thì `uiautomator dump` in `ERROR: could not get idle state.` và `exec-out cat` không in gì; `$FAKE_ADB_HANG` (tên lệnh con, ví dụ `uiautomator`) thì lệnh đó `exec sleep 300` (có `exec`, như nhánh `logcat` có sẵn, để timeout của trình chạy giết đúng tiến trình, không để tiến trình con giữ stderr; trình chạy bắt output của mọi lệnh `adb`); `wm size` in `Physical size: 1080x2400`, `wm density` in `Physical density: 420`; `input`, `wm`, `cmd uimode`, `settings`, `cmd locale` ghi vào `$FAKE_ADB_CALLS` |
| Tạo | `scripts/tests/fixtures/device-scenario/onboarding.xml` | Dump mẫu: `onboarding_screen`, nút `onboarding_enable_camera` `[96,1740][984,1872]` |
| Tạo | `scripts/tests/fixtures/device-scenario/camera.xml` | Dump mẫu: `camera_screen`, `camera_poses` |
| Tạo | `scripts/tests/fixtures/device-scenario/grid.xml` | Dump mẫu: `poses_grid` có 3 thẻ `poses_card_a`, `_b`, `_c`, một thẻ ngoài lưới, chip `Đã lưu` `selected="true"` |
| Tạo | `scripts/tests/fixtures/device-scenario/consent.xml` | Dump mẫu: hộp thoại đồng ý có nút `Do not consent` |
| Tạo | `scripts/tests/fixtures/device-scenario/blank.xml` | Dump mẫu: chưa có gì của app (màn tải) |
| Tạo | `scripts/tests/fixtures/device-scenario/busy.idle-error` | Đánh dấu cho `adb` giả in `could not get idle state` |
| Tạo | `scripts/tests/fixtures/device-scenario/scenarios/ok.txt` | Tệp đạt trên chuỗi mẫu |
| Tạo | `scripts/tests/fixtures/device-scenario/scenarios/fail.txt` | Tệp lỗi ở bước 3 |
| Tạo | `scripts/tests/fixtures/device-scenario/scenarios/need.txt` | Tệp có `need` không khớp ở bước 2 |
| Tạo | `scripts/tests/fixtures/device-scenario/bad.txt` | Lệnh lạ ở dòng 5 |
| Tạo | `scripts/tests/fixtures/device-scenario/bad-id.txt` | `id=` không có trong code, `str=` không có trong `strings.xml` |
| Sửa | `scripts/tests/run.sh` | Khối `device-scenario` (sau khối `device-session`, trước dòng tổng kết); khối `device-session` thêm test bước 4, `helper`, bảng, và đặt `export DEVICE_SCENARIOS_GLOB` trỏ thư mục rỗng cạnh `DEVICE_SESSION_SLEEP`, bỏ ở dòng `unset` cuối khối (mục 3.1, phiên bản 2); khối `plan-check` thêm test quy tắc 11 |
| Sửa | `scripts/plan-check.py` | Quy tắc 11: dòng "Kiểm tra thủ công" ghi nơi chạy; tệp emulator có trong mục 3.2 hay repo, `check` qua. Chỉ chú ý, cho mọi plan; docstring ghi điều kiện để đổi thành lỗi |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/ui/UiAutomatorTags.kt` | `Modifier.exposeTestTagsForUiAutomator()`; chú thích về cửa sổ riêng |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | `Surface(modifier = Modifier.fillMaxSize().exposeTestTagsForUiAutomator())` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/network/NetworkDialogHost.kt` | Trong `Dialog`: `Box(Modifier.exposeTestTagsForUiAutomator()) { NetworkDialogScreen(...) }` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ui/UiAutomatorTagsTest.kt` | Robolectric: modifier đặt thuộc tính; đối chứng không có modifier |
| Sửa | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/network/NetworkDialogHostTest.kt` | Thêm test: cửa sổ hộp thoại có `TestTagsAsResourceId` |
| Tạo | `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt` | Kịch bản 1 (mục 3.1) |
| Tạo | `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt` | Kịch bản 2 |
| Tạo | `feature/camera/src/deviceScenarios/T-020-camera-mo-gap.txt` | Kịch bản 3 |
| Sửa | `docs/templates/plan.md` | Mục 4: ba dòng mẫu kiểm thủ công, chú thích nhóm lý do |
| Sửa | `docs/CLAUDE.md` | Mục Khi viết plan: một gạch đầu dòng |
| Sửa | `docs/ops/test-sessions.md` | Mục 4 một đoạn; mục mới 5; mục 6 (dời) |
| Sửa | `docs/auto-run.md` | Mục 4, Kết thúc phiên: ý "Phân tích thư mục phiên", ý "Những gì người cần tự kiểm tra" |
| Sửa | `docs/templates/run.md` | Mục Kiểm thử trên máy ảo một dòng; mục Cần người kiểm tra hai dòng |
| Sửa | `docs/lessons.md` | L-062: dòng Áp dụng (bước 10) |

**Nơi khác đọc các file plan này sửa (L-020), và vì sao không bị ảnh hưởng:**
- **`.github/workflows/device-session.yml`:** gọi `scripts/device-session.sh` (`start ci`, `scenarios --yes`, `stop`) và đọc `test-sessions/*/summary.md`. Workflow không đổi: cùng lệnh, cùng tên tệp. Thư mục `test-sessions/` mà artifact lưu đã gồm `scenarios/`.
- **`scripts/app-explore/ax.sh`:** dùng cùng cách dump nhưng không bị sửa. Hành vi `FAKE_ADB_UI` của `adb` giả giữ nguyên cho test của nó.
- **`docs/auto-run.md`, `docs/templates/run.md`:** các tệp sau chỉ nhắc hai tài liệu này trong chú thích hay thông báo, chỉ tới mục mà plan không đổi tên:
  - `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`;
  - `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/resume-check.py`, `scripts/save-branch.sh`.

  `scripts/resume-check.py` đọc `status:` của danh sách chạy, không đọc các mục plan này sửa.
- **`docs/CLAUDE.md`:** `scripts/required-assets.txt` nhắc nó trong chú thích về mục Khi viết plan. Mục đó vẫn còn.
- **`scripts/tests/run.sh`:** `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh` và `scripts/wait-run.sh` nhắc nó trong chú thích (`scripts/wait-run.sh` cũng nhắc `docs/auto-run.md`, `docs/templates/run.md` như các script ở trên). Khối test của chúng không đổi.
- **`scripts/plan-check.py`:** `scripts/run-check.py` nạp nó, nên chú ý của quy tắc 11 cũng hiện khi kiểm danh sách chạy. Chú ý không làm danh sách nào hỏng. Test "run-check on the latest run list" kiểm rằng không có lỗi mới.
- **`docs/lessons.md`:** `scripts/status.py` đọc trạng thái bài học. Dòng Áp dụng không đổi trạng thái.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Kịch bản dựa vào các cờ đã bật trong `StagingFeatureFlagsModule` (`POSES`, `SETTINGS`).
- Quảng cáo: Không chạm. `consent decline` chỉ chạm nút từ chối của hộp thoại đồng ý có sẵn (ADR-0013), trên emulator.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm (kịch bản tải catalog staging như `AppLaunchTest`).
- **Quyền truy cập:** Không thêm. Kịch bản cấp quyền runtime đã khai báo bằng `pm grant` như kịch bản chuẩn.
- **Thư viện và SDK:** Không thêm. `testTagsAsResourceId` thuộc `androidx.compose.ui` (`compose-ui` đã có trong `gradle/libs.versions.toml`, `:app` đã phụ thuộc). Trình chạy là Python chuẩn.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc,** chỉ biết ở `device-session` đầu tiên sau khi merge (mục 4.1):
  - `uiautomator dump` của emulator CI có cho thấy `resource-id` của Compose;
  - phần tử chỉ có `testTag` (ví dụ `camera_grid`, lớp vẽ lưới) có xuất hiện trong dump;
  - dump của màn camera có luôn báo `could not get idle state`;
  - hộp thoại đồng ý có chữ `Do not consent`.
- **Điều kiện chuyển:** báo cáo của lần chạy đầu cho thấy một trong các dấu hiệu sau:
  - `wait id=...` lỗi trong khi `fail.png` cho thấy đúng màn;
  - `fail.xml` không có `resource-id` nào của app;
  - mọi dump của một màn đều là `could not get idle state`.

  Phiên không dừng vì điều này (mục 7, điều 4).
- **Cách làm thay:** plan phiên bản 2 cho một task sửa sau.
  - Kịch bản dùng `str=` hay `desc=` thay cho `id=`.
  - Thêm dạng chờ `log=<regex>`: chờ một dòng `VGL.*` trong logcat của phiên, nhờ log đổi trạng thái chính đã có theo ADR-0019.

  Tiêu chí và test của mục 4 giữ nguyên. Plan này không cài sẵn phần dự phòng.

## 4. Kiểm thử

Test viết trước code: các khối mới trong `scripts/tests/run.sh`, `UiAutomatorTagsTest`, test mới của `NetworkDialogHostTest`. Khung rỗng của bước 3:
- `scripts/device-scenario.py` chỉ đọc tham số, trả 0, không in gì;
- `device-session.sh` chưa gọi nó và chưa có `helper`;
- `exposeTestTagsForUiAutomator()` trả `this`.

Chuỗi dump của `adb` giả dựng bằng `FAKE_ADB_UI_SEQ` (mục 3.2).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Kịch bản đạt | Test script | `device-scenario: a passing file is reported as passed`: `ok.txt` trên chuỗi `consent.xml onboarding.xml camera.xml`; `scenarios.txt` có `plan ok.txt step=` cho mọi bước, dòng `result=đạt seconds=`; mã 0 |
| Chạm đúng tâm | Test script | `device-scenario: tap uses the centre of the bounds`: `$FAKE_ADB_CALLS` có `shell input tap 540 1806` cho nút `[96,1740][984,1872]` |
| Đồng ý hiện ngay | Test script | `device-scenario: consent decline taps Do not consent when shown` |
| Đồng ý hiện muộn | Test script | `device-scenario: consent decline waits for a late consent form`: chuỗi `blank.xml blank.xml consent.xml onboarding.xml`; có lệnh chạm nút từ chối. Đối chứng: chuỗi `blank.xml onboarding.xml` không có lệnh chạm nào; chuỗi chỉ `blank.xml` với `consent decline 2` thì `FAIL` |
| Bộ chọn | Test script | `device-scenario: idp= in= index= pick the second card of the grid` (chạm tâm `poses_card_b`, không phải thẻ ngoài lưới); `... str= matches text and content-desc`; `... selected=true matches selected or checked` (chip `Đã lưu`; đối chứng chip chưa chọn không khớp); `... clickable=true skips plain nodes` |
| `count` | Test script | `device-scenario: count checks the number of matches`: `count idp=poses_card_ in=poses_grid 3` đạt, `4` thì `FAIL` |
| Kịch bản lỗi | Test script | `device-scenario: the first failing step stops the file`: `fail.txt`: dòng `step=3 ... FAIL`, không có `step=4`, có `fail.png` và `fail.xml`, `result=lỗi`; mã 0 |
| Đối chứng (L-035) | Test script | `device-scenario: a passing file leaves no fail.png`. Trên khung rỗng, test "kịch bản lỗi" thất bại vì không có dòng nào |
| `need` | Test script | `device-scenario: need ends the file as skipped (environment)`: `need.txt`: `result=bỏ qua (môi trường)`, không có `fail.png`, không có `step=3` |
| Chờ khi chưa rảnh | Test script | `device-scenario: not idle is retried until the timeout`: chuỗi `busy.idle-error busy.idle-error camera.xml`, `wait id=camera_screen 5` đạt; chỉ `busy.idle-error` thì `FAIL could not get idle state` |
| Hạn chót, `adb` treo | Test script | `device-scenario: a hanging adb fails the file within its deadline`: `FAKE_ADB_HANG=uiautomator`, timeout của lệnh hạ bằng `DEVICE_SCENARIO_ADB_TIMEOUT=1`, tệp `timeout: 3`: `FAIL adb_timeout`, tệp sau vẫn chạy, cả lượt xong dưới 15 giây, mã 0 |
| Ngân sách | Test script | `device-scenario: files beyond the budget are skipped`: `DEVICE_SCENARIOS_BUDGET=0`, hai tệp: cả hai `bỏ qua (hết thời gian)`; vẫn có lệnh trả máy về gốc |
| Trả máy về gốc | Test script | `device-scenario: the device is reset after a failing file` và `... after a passing file`: `$FAKE_ADB_CALLS` có `wm size reset`, `wm density reset`, `cmd uimode night no`, `accelerometer_rotation 1`, `user_rotation 0`, `set-app-locales` để xóa |
| `network off` không được | Test script | `device-scenario: network off that keeps a network skips the file`: `FAKE_ADB_NETWORK=100`: `result=bỏ qua (môi trường)` |
| Dấu log mỗi tệp | Test script | `device-scenario: marks the start of each file in the log`: `$FAKE_ADB_CALLS` có `log -t VGL.Session` với `scenario=plan file=ok.txt` |
| Cú pháp | Test script | `device-scenario: check reports an unknown command with its line`: `check bad.txt` mã 1, in `bad.txt dòng 5`; `run` với `bad.txt` ghi `result=lỗi tệp`, không gửi lệnh `input` nào |
| Tag và chuỗi có thật | Test script | `device-scenario: check reports an id or str that the code does not have`: `check bad-id.txt` mã 1, nêu cả hai |
| Kịch bản của repo | Test script | `device-scenario: the scenarios of the repository pass check`: `check` trên mọi `feature/*/src/deviceScenarios/*.txt` và `app/src/deviceScenarios/*.txt`, mã 0. Chạy trong `check.sh` ở mọi PR đổi `feature/**`, `app/**` (bộ lọc `code` của `pr-check`) |
| `device-session` gọi trình chạy | Test script | `device-session: scenarios runs the plan scenarios after monkey`: `DEVICE_SCENARIOS_GLOB` trỏ `ok.txt`; dòng `monkey` trước dòng `plan ok.txt` |
| Lệnh `helper` | Test script | `device-session: helper network-off and grant-permissions reuse the scenario functions`: `helper network-off` mã 0 khi mạng tắt được, mã 1 khi `FAKE_ADB_NETWORK=100`; `helper grant-permissions` in `granted=CAMERA`; Usage có `helper` |
| Bảng trong `summary.md` | Test script | `device-session: summary lists the plan scenarios`: có `## Kịch bản theo plan`, dòng `ok.txt` `đạt`, `fail.txt` `lỗi`, `need.txt` `bỏ qua (môi trường)`, cột giây; `stop` mã 0 khi không crash |
| Tiêu chí có `\|` | Test script | `device-session: a criterion with a pipe keeps the table`: dòng của tệp có `criterion: a \| b` ra `a / b`, số cột không đổi |
| Không có kịch bản | Test script | `device-session: summary says there are no plan scenarios`: `DEVICE_SCENARIOS_GLOB` trỏ thư mục rỗng: dòng "Kịch bản theo plan: không có" |
| Crash trong lúc chạy kịch bản | Test script | `device-session: a crash during a plan scenario is blocking and named`: logcat mẫu có dấu `scenario=plan file=ok.txt` rồi `FATAL EXCEPTION` của gói: `stop` mã 1, bảng ghi crash ở dòng `ok.txt` |
| Trình chạy hỏng | Test script | `device-session: a runner error does not stop scenarios`: `DEVICE_SCENARIO_CMD` trả 1: `scenarios` mã 0, có `plan runner_error` |
| `plan-check` quy tắc 11 | Test script | `plan-check: a manual row without where it runs is a warning`, cho cả plan `draft` lẫn `approved` (dòng `CHÚ Ý`, mã 0); `... an emulator row needs a scenario file in the file list or the repository`; `... a scenario file that fails check is a warning`; `... máy thật and người need a reason`. Đối chứng: plan mẫu đúng không có dòng nào của quy tắc 11 |
| Không làm hỏng danh sách chạy | Test script | `run-check: the latest run list has no new error from rule 11`: `scripts/run-check.py` trên tệp `docs/runs/RUN-*.md` mới nhất theo tên; không có dòng `LỖI` nào nhắc "kiểm tra thủ công" |
| Thuộc tính gốc | Compose UI test (Robolectric) | `UiAutomatorTagsTest.exposeTestTags_setsTestTagsAsResourceId`, đối chứng `withoutModifier_hasNoTestTagsAsResourceId` |
| Hộp thoại mạng | Compose UI test (Robolectric) | `NetworkDialogHostTest.shownDialog_exposesTestTagsAsResourceId`: trong cửa sổ hộp thoại có node mang `TestTagsAsResourceId == true` và tag `network_dialog_retry` |
| Chỗ gắn hàm | Test script | `app: MainActivity and NetworkDialogHost call exposeTestTagsForUiAutomator`: `grep` thấy lời gọi trong hai tệp. Dòng của `MainActivity` không có test JVM (Hilt, Firebase), nên kiểm bằng cách này |
| Ba kịch bản chạy thật | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`, `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`, `feature/camera/src/deviceScenarios/T-020-camera-mo-gap.txt`) | Lần `device-session` đầu tiên sau khi merge: bảng có 3 dòng. Phiên ghi kết quả từng dòng vào báo cáo, mở các ảnh `shot` và `fail.png` để biết lỗi của app hay của kịch bản (mục 3.4) |
| Phiên làm theo tài liệu | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt chéo đọc diff `docs/auto-run.md`, `docs/templates/run.md`, `docs/templates/plan.md`, `docs/ops/test-sessions.md` |

**Ngoại lệ viết test trước:**
- `docs/templates/plan.md`, `docs/CLAUDE.md`, `docs/ops/test-sessions.md`, `docs/auto-run.md`, `docs/templates/run.md`, `docs/lessons.md` là tài liệu.
- Ba tệp kịch bản là test chạy trên emulator. Phiên chỉ kiểm được bằng `check`; chạy thật ở `device-session` cuối phiên.
- Dòng sửa `MainActivity.kt` chỉ có test `grep` (dòng "Chỗ gắn hàm").

### 4.1. Chạy thử

Phạm vi chạy thử: chỉ API `testTagsAsResourceId` trên JVM. Phần emulator chưa thử được.

- **Đã chạy thử:** ngày 2026-10-24, trên nhánh chính `795028c`, máy dev (JDK 21). Test tạm `app/src/test/.../trial/ExposeTestTagsTrialTest.kt`, không commit, đã xóa, gồm:
  - `Box(Modifier.semantics { testTagsAsResourceId = true })` có `SemanticsPropertiesAndroid.TestTagsAsResourceId == true`;
  - đối chứng: không có modifier thì không có node nào mang giá trị đó.
- **Lệnh:** `./gradlew :app:testStagingDebugUnitTest --tests '*ExposeTestTagsTrialTest*'`, qua (`tests="2" failures="0"`). Toàn bộ chạy bằng `scripts/check.sh` ở bước 5. API biên dịch được với phiên bản Compose hiện tại, cần `@OptIn(ExperimentalComposeUiApi::class)`.
- **Chưa thử được:** môi trường này không có emulator, và `device-session.yml` chỉ chạy trên nhánh chính (`if: github.ref == 'refs/heads/main'`), nên không PR thử nào chạy được nó trước khi merge. Các điểm ở mục 3.4 được kiểm ở `device-session` đầu tiên sau khi merge. Điều đó không làm hỏng phiên:
  - kịch bản lỗi không chặn staging;
  - mọi lệnh `adb` có hạn chót;
  - quy tắc 11 chỉ là chú ý.
- **Phần đọc dump và chạm tâm `bounds`:** là phân tích XML, đã có mẫu trong `scripts/app-explore/guard.py`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI và quy trình phiên tự chạy). Plan đổi `scripts/device-session.sh` mà `device-session.yml` chạy, `scripts/plan-check.py` mà phiên chạy trước mỗi task, và bước cuối phiên của `docs/auto-run.md`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script mới, hai script sửa, hai chỗ của vỏ app, ba kịch bản, sáu tài liệu |
| B. Độ rõ của yêu cầu | 1 | L-062 rõ; câu hỏi ở mục 8 đã được trả lời; còn hai điểm lệch ticket chờ người xác nhận |
| C. Khả năng kiểm chứng tự động | 2 | Trình chạy (kể cả hạn chót), `plan-check` và thuộc tính có test; phần trên emulator thật chỉ kiểm được sau khi merge |
| D. Ảnh hưởng chéo | 2 | `auto-run.md`, `docs/templates/run.md`, `docs/templates/plan.md`, `plan-check.py` cùng được T-045, T-047 sửa |
| E. Độ mới của kỹ thuật | 2 | `uiautomator dump` có trong `scripts/app-explore/`; chạy kịch bản trên emulator CI và `testTagsAsResourceId` là lần đầu; đã chạy thử phần JVM |
| **Tổng** | 9 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:**
  - `scripts/device-session.sh` (máy dev và CI), `scripts/plan-check.py` (mọi plan);
  - `docs/auto-run.md` mục 4, `docs/templates/run.md`, `docs/templates/plan.md`;
  - `scripts/tests/run.sh`, `scripts/tests/fake-adb`;
  - vỏ app `MainActivity.kt`, `NetworkDialogHost.kt`.
- **Task khác có thể xung đột (viết song song):**
  - **T-044** (`wait-run.sh --pr`): chỉ chung `scripts/tests/run.sh`. T-044 thêm vào khối `wait-run`, plan này thêm khối `device-scenario` sau khối `device-session`.
  - **T-045** (hàng chờ plan cuối phiên): sửa `docs/auto-run.md` mục 4, Kết thúc phiên, và `docs/templates/run.md`.
    - Plan này chỉ sửa ý "Phân tích thư mục phiên", ý "Những gì người cần tự kiểm tra", mục "Kiểm thử trên máy ảo" và hai dòng của "Cần người kiểm tra". Khác đoạn.
    - Nếu T-045 sửa mẫu plan hay `plan-check.py`: plan này chỉ sửa mục 4 của mẫu và thêm quy tắc 11.
  - **T-047** (sẵn sàng phát hành, `scripts/status.py`): đọc mục kiểm thủ công chưa đánh dấu của báo cáo các phiên. Plan này đổi tên dòng mẫu thành "Tiêu chí kiểm tra thủ công (máy thật, người)" và thêm "Kịch bản emulator lỗi hay bỏ qua". T-047 nên đọc cả dòng cũ và dòng mới.
- **Việc sau task này, không làm ở đây:** khi một `device-session` có cả 3 kịch bản `đạt`, một dòng sửa ở `scripts/plan-check.py` đổi quy tắc 11 thành lỗi với plan `draft` (vẫn chú ý với plan `approved`, như L-065), kèm test. Báo cáo phiên ghi điều này vào Đề xuất cho buổi họp tiếp theo.
- **Đề xuất thứ tự chạy:** T-046 trước T-047. T-044 độc lập. Với T-045: trước hay sau đều được; ai chạy sau thì gộp `docs/auto-run.md` mục 4.

## 7. Điều kiện dừng riêng

1. Cần sửa `.github/workflows/device-session.yml` hay `.github/workflows/pr-check.yml`: dừng, viết plan riêng.
2. `testTagsAsResourceId` làm đổi ảnh chuẩn screenshot hay làm hỏng một test có sẵn của `:app`: dừng.
3. Quy tắc 11 làm một plan `approved` hay một danh sách chạy có lỗi (không phải chú ý): dừng.
4. Không phải điều kiện dừng: ở `device-session` cuối chính phiên này, hay lần đầu sau khi merge, trình chạy hỏng (`runner_error`, mọi tệp `lỗi` vì dấu hiệu ở mục 3.4). Phiên chỉ ghi vào báo cáo, phát staging như bình thường (crash và ANR vẫn chặn), và đề xuất làm mục 3.4.

## 8. Câu hỏi cho người duyệt

Đã trả lời (Tuấn, 2026-10-24, theo đề xuất của rà soát độc lập):

1. **Kịch bản emulator lỗi có chặn phát staging không?** Không.
   - Kịch bản lỗi thành phát hiện mức "xem", có phản hồi `FB-...` `nen-sua`.
   - Thêm kết quả `bỏ qua (môi trường)` qua lệnh `need`; kết quả này không soạn phản hồi.
   - Crash hay ANR trong lúc chạy kịch bản vẫn chặn.
2. **Bật `testTagsAsResourceId` cho mọi biến thể?** Có.
   - Gắn ở `Surface` gốc của `MainActivity` và trong `Dialog` của `NetworkDialogHost` ở `:app`, không sửa `:core:designsystem`.
   - Hộp thoại và popup khác mà kịch bản cần tìm theo `id=` phải gắn modifier ở chỗ mở chúng.
3. **Gập thật trên emulator?** Dùng `wm size` và `wm density`. Emulator gập thật để ticket riêng sau, vì phải sửa `device-session.yml`.
4. **Quảng cáo trên emulator (T-018, T-023)?** Giữ "máy thật: Remote Config". Báo cáo phiên nhắc rằng ticket ban đầu có hai mục này.
5. **Ngân sách thời gian?** Mặc định 600 giây, hạn chót cứng (mục 3.1). Thời gian từng tệp được ghi vào `scenarios.txt` (dòng `result=... seconds=`), để chỉnh ngân sách sau.
6. **Một task hay tách?** Giữ một task. Vì vậy quy tắc 11 chỉ là chú ý cho tới khi một `device-session` chạy cả 3 kịch bản đạt (mục 2, 6).

Còn chờ người xác nhận:

7. **Lệch với ticket.** Hai điểm, phạm vi ticket đã sửa cho khớp:
   - Không thêm cột "Chạy ở", mà ghi nơi chạy trong cột "Loại test".
   - T-018, T-023 không có kịch bản.

   Đề xuất của AI: chấp nhận cả hai.

   **Trả lời:** chấp nhận cả hai (Tuấn, 2026-10-24).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1: Duyệt (Nhật, duyệt chéo Tuấn, 2026-10-24); ghi chú: Ngày 2026-10-24, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Rà soát độc lập (phiên mới, chỉ đọc) trước khi duyệt; mọi điểm chặn và nên sửa đã sửa trong bản nháp. Giữ một task, quy tắc 11 chỉ chú ý (Tuấn chọn). Thay bằng phiên bản 2 vì rà soát độc lập trước RUN-20261024-02 (docs/ops/pre-run-review.md, mục 2) tìm 4 test `device-session` có sẵn sẽ chạy cả 3 kịch bản thật của repo (`DEVICE_SCENARIOS_GLOB` mặc định) và đếm sai số lần cấp quyền, bật mạng; dòng kịch bản T-020 chỉ có bộ lọc trái cú pháp "một từ khóa chính" (nay dùng `idp=camera_pose_`, tag có sẵn `CameraTestTags.pose(id)`); `sleep 300` của `fake-adb` không có `exec` để lại tiến trình mồ côi; câu 7 của mục 8 chưa có trả lời.

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-24, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Phiên bản 2 sửa theo hai lượt rà soát độc lập trước RUN-20261024-02 (lượt 2: không còn điểm chặn); Tuấn đồng ý duyệt.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261024-02, phiên bản plan 2

- **Kết quả:** đã merge
- **Nhánh, PR:** `claude/T-046-kiem-tay-tren-emulator`, PR #121 (commit merge `d1bc390`)
- **Đã làm:** `scripts/device-scenario.py` (trình chạy kịch bản, hạn chót tuyệt đối cho mọi lệnh `adb`, chọn phần tử theo resource-id từ `uiautomator dump`), `scripts/device-session.sh scenarios` và bảng trong `summary.md`, `scripts/tests/fake-adb` và fixture, ba tệp `deviceScenarios` (T-020, T-022, T-024), quy tắc 11 của `plan-check.py` (mức chú ý), `exposeTestTagsForUiAutomator` ở `MainActivity` và `NetworkDialogHost`, `docs/CLAUDE.md`, `docs/auto-run.md`, `docs/templates/plan.md`, `docs/templates/run.md`, `docs/ops/test-sessions.md`.
- **Test viết trước:** 52 lệnh `expect` mới trong `scripts/tests/run.sh` và 3 test Kotlin; 48 lệnh `expect` và 2 test Kotlin thất bại đúng lý do. Test qua ngay: đối chứng `withoutModifier_hasNoTestTagsAsResourceId` (cặp với test kia), `run-check: the latest run list has no new error from rule 11` (test chặn qua ngay, có điều kiện đầu ra không rỗng), `an empty glob exits before any adb call` (làm chặt bằng lần chạy đối chứng có tệp).
- **Rà soát độc lập:** 0 chặn; nên sửa nhưng chưa sửa (ghi ở Cần người kiểm tra của báo cáo): (1) ba kịch bản chưa kiểm hết tiêu chí của ticket (đúng như mục 3.1 của plan); (2) `check` quét cả `src/test` nên tag bị đổi tên trong code chính mà test còn giữ chuỗi cũ vẫn qua; (3) ngân sách 600 giây không tính thời gian đặt lại máy giữa các tệp; (4) `timeout` ngoài chỉ gửi SIGTERM, tiến trình `adb` con có thể còn lại. Các điểm này không đổi điều test của plan kiểm; để người quyết định.
- **Kết quả CI:** `scripts/check.sh` qua (1050 test JVM, 0 lỗi); `pr-check` xanh (lần chạy 40112382455).
- **Lệch so với plan:** `fake-adb` đếm chuỗi dump ở `$FAKE_ADB_STATE_DIR/ui-seq.n`; test "ngân sách" dùng glob 2 tệp (`scenarios/[of]*.txt`) cho khớp chữ "hai tệp" của plan; thêm biến `DEVICE_SCENARIO_POLL`; ảnh lưu ở `scenarios/<tên tệp bỏ .txt>/<tên>.png`. Làm bước 3 đến 5 bằng phiên con trong worktree riêng (auto-run.md, "Giao bước 3 đến 5 cho phiên con").
- **Việc còn lại:** dòng Áp dụng của L-062 (số PR #121) ghi ở PR báo cáo. Lần `device-session` đầu sau khi merge là lần kiểm thật đầu tiên của trình chạy (plan mục 3.4, 7): điểm chưa chắc là resource-id của Compose, hộp thoại đồng ý (UMP trong WebView) và `could not get idle state` ở màn camera.
- **Cần người xem:** bảng "Kịch bản emulator" trong `summary.md` của `device-session` cuối phiên; ảnh chuẩn screenshot của hộp thoại mạng (thêm một `Box` bọc ngoài) — `check.sh` và CI không báo lệch.
