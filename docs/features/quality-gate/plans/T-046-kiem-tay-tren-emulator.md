---
ticket: T-046
title: Tiêu chí kiểm thủ công chạy được trên emulator thành kịch bản device-session
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 10.4M        # cỡ M, số gợi ý của scripts/token-usage.py estimate
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-046: TIÊU CHÍ KIỂM THỦ CÔNG CHẠY ĐƯỢC TRÊN EMULATOR THÀNH KỊCH BẢN DEVICE-SESSION

## 1. Tóm tắt

Hiện mỗi phiên tự chạy để lại nhiều mục kiểm thủ công. Phần lớn chạy được trên emulator, nhưng vẫn phải chờ người (L-062). Plan này làm bốn việc:

1. Mỗi dòng kiểm thủ công ở mục 4 của plan ghi rõ chạy ở "máy thật" hay "emulator".
2. Phần chạy được trên emulator được viết thành tệp kịch bản trong `docs/features/<feature>/device-scenarios/`.
3. Lệnh `scripts/device-session.sh scenarios` chạy mọi tệp đó sau kịch bản chuẩn, qua một trình chạy mới `scripts/device-scenario.py`. Trình chạy đọc màn hình bằng `uiautomator dump`, chọn phần tử theo `testTag`, ghi kết quả và ảnh chụp vào thư mục phiên, và in một bảng vào `summary.md`.
4. Phiên đọc bảng đó ở cuối phiên. Chỉ phần máy thật còn lại cho người.

Không sửa `.github/`. `device-session.yml` vẫn gọi `scenarios --yes` như cũ. Muốn `uiautomator` thấy `testTag`, vỏ app bật `testTagsAsResourceId` ở gốc giao diện. Kỹ thuật này đã chạy thử trên JVM (mục 4.1).

Kiểm chứng gồm:
- test của trình chạy với `adb` giả và các bản dump giao diện mẫu;
- test cho quy tắc mới của `plan-check.py`;
- test Robolectric cho thuộc tính gốc;
- ba kịch bản đầu tiên cho các mục còn chờ của RUN-20261023-04, chạy thật ở `device-session` đầu tiên sau khi merge.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là bài học L-062 (`docs/lessons.md`) và phạm vi của ticket.

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Mẫu plan ghi nơi chạy | Mỗi dòng "Kiểm tra thủ công" ở mục 4 ghi `(máy thật: <lý do>)` hoặc `(emulator)`; dòng emulator nêu tệp kịch bản | Ghi ngay ở cột "Loại test" của bảng có sẵn, không thêm cột, để các plan cũ vẫn đọc được. Lý do của "máy thật" là một trong các nhóm cố định (mục 3.1) để người duyệt thấy vì sao không tự động được |
| `plan-check.py` kiểm điều đó | Plan `draft` thiếu ghi chú nơi chạy, hay ghi `(emulator)` mà không nêu tệp kịch bản có trong mục 3.2 hoặc đã có trong repo: lỗi. Plan `approved`: chỉ chú ý | Như quy tắc 2, 7 (L-065): không làm hỏng plan đã duyệt |
| Kịch bản chạy trong `device-session` | Mọi tệp `docs/features/*/device-scenarios/*.txt` được chạy sau kịch bản chuẩn, mỗi tệp từ trạng thái mới cài, có giới hạn thời gian; `summary.md` có bảng "Kịch bản theo plan" | Không đổi workflow: `scenarios --yes` gọi trình chạy. Kịch bản lỗi không làm job đỏ (chỉ crash, ANR làm đỏ như hiện nay); kết quả là mức "xem" (câu hỏi 1, mục 8) |
| Phiên đọc kết quả | `auto-run.md` mục 4: đọc bảng, kịch bản lỗi thành phát hiện có ảnh và bước lỗi, soạn phản hồi `FB-...`; "Cần người kiểm tra" chỉ còn phần máy thật và kịch bản lỗi | Mẫu báo cáo `docs/templates/run.md` có dòng kết quả kịch bản; dòng kiểm thủ công ghi "máy thật" |
| Kịch bản đầu tiên | Các mục còn chờ của RUN-20261023-04 chạy được trên emulator thành kịch bản | Làm được: T-022 bước 1 đến 6, T-024 bước 1 đến 4 và phần bố cục của T-020, bằng cách đổi kích thước màn hình (mục 3.1). Không làm được trên emulator của CI: T-018, T-023 (cần Remote Config bật vị trí quảng cáo; bản staging dùng file Firebase giả, L-002), phần camera thật của T-020 (emulator không có camera sau), gập thật có bản lề, T-015 chụm hai ngón |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0019 (log), ADR-0012 (cấu hình staging giả), ADR-0013 (hộp thoại đồng ý quảng cáo phủ lên app khi mới cài), ADR-1003 (nối chỗ gọi).

## 3. Giải pháp

### 3.1. Cách làm

**Cách viết tệp kịch bản.** Mỗi dòng là một lệnh; dòng trống và dòng bắt đầu bằng `#` bị bỏ qua. Ba dòng đầu là phần đầu bắt buộc:

```text
ticket: T-022
criterion: Luồng thật trên máy, bước 1 đến 6 (plan T-022, mục 4)
timeout: 240
```

`timeout` là giới hạn của cả tệp, tính bằng giây, mặc định 180, tối đa 300.

Bộ chọn phần tử:
- `id=<testTag>`: `resource-id` của nút, khi gốc có `testTagsAsResourceId`;
- `desc=<chuỗi>`: `content-desc`;
- `text=<chuỗi>`: chữ hiển thị;
- `str=<tên chuỗi>`: lấy giá trị trong `res/values/strings.xml` của mọi module, tức chữ mà emulator en-US hiện.
- Thêm `index=<n>` để chọn phần tử thứ n khi nhiều phần tử khớp, đếm từ 1 theo thứ tự trong dump.
- Thêm `in=<testTag>` để chỉ tìm trong cây con của một phần tử.

Mười lệnh, đủ cho các mục đã gặp và không hơn:

| Lệnh | Việc |
| --- | --- |
| `fresh` | Xóa dữ liệu, cấp quyền runtime như kịch bản chuẩn (`grant_permissions`), mở app |
| `consent decline` | Có hộp thoại đồng ý quảng cáo (nút có chữ `Do not consent` hay `Không đồng ý`) thì chạm, không có thì bỏ qua |
| `wait <bộ chọn> [giây]` | Chờ tới khi phần tử có trên màn, mặc định 20 giây; nhiều bộ chọn ngăn bằng `\|` thì chờ một trong số đó |
| `tap <bộ chọn>` / `tap_if <bộ chọn>` | Chạm vào tâm của `bounds`; `tap_if` bỏ qua khi không thấy |
| `scroll <bộ chọn> [lần]` | Vuốt lên trong vùng của phần tử, mặc định 3 lần |
| `expect <bộ chọn>` / `expect_no <bộ chọn>` | Kiểm có hay không có ngay lúc đó (sau một lần dump) |
| `network off\|on` | Dùng lại `network_off`, `network_on` của `device-session.sh` (tắt cả Wi-Fi và dữ liệu, chờ không còn mạng mặc định, L-031) |
| `device <thiết lập>` | `rotate landscape\|portrait` (`settings put system user_rotation`), `night on\|off` (`cmd uimode night`), `unfold` / `fold` (`wm size 1766x2208` và `wm density 420`, tức 673 × 841 dp như khung `_unfolded`; `fold` trả về kích thước gốc), `locale <thẻ>` (`cmd locale set-app-locales`, Android 13 trở lên) |
| `back` | Phím Back |
| `shot <tên>` | Chụp màn hình vào `<thư mục phiên>/scenarios/<tệp>/<tên>.png` |

Lệnh lạ, hay bộ chọn sai cú pháp, là lỗi của tệp: tệp đó báo `LỖI TỆP` ở dòng đó và không chạy. `plan-check.py` cũng kiểm cú pháp này (quy tắc 11), nên lỗi lộ ra từ lúc viết plan.

**Trình chạy `scripts/device-scenario.py`.** Python thuần, không thư viện ngoài, giống `scripts/app-explore/tree.py`.
- `check <tệp>...`: chỉ kiểm cú pháp, không cần máy. `plan-check.py` dùng chung hàm đọc này.
- `run --session <thư mục> --package <gói>`: chạy mọi tệp kịch bản theo thứ tự tên.
- Đọc màn hình:
  - lệnh `adb shell uiautomator dump /sdcard/ds.xml`, rồi `adb exec-out cat /sdcard/ds.xml`, như `scripts/app-explore/ax.sh`;
  - dump báo `could not get idle state` (khi có hiệu ứng chạy mãi) thì thử lại sau 1 giây, tới hết giờ của lệnh.
- Mỗi bước ghi một dòng vào `<thư mục phiên>/scenarios.txt`: `plan <tệp> step=<n> <lệnh> ok|FAIL <lý do>`.
- Bước đầu tiên lỗi thì:
  - chụp màn hình `fail.png` và giữ bản dump `fail.xml` để AI đọc;
  - ghi `VGL.Session` vào log (`mark`), để AI cắt đúng đoạn log;
  - bỏ các bước còn lại của tệp, chuyển sang tệp sau.
- Sau mỗi tệp, dù đạt hay lỗi, đưa máy về trạng thái gốc:
  - xoay dọc, tắt chế độ tối;
  - `wm size reset`, `wm density reset`;
  - bật lại mạng, đặt lại ngôn ngữ của app.
- Tổng thời gian mọi tệp giới hạn bởi `DEVICE_SCENARIOS_BUDGET`, mặc định 900 giây, để job (giới hạn 50 phút, hiện chạy khoảng 15 đến 25 phút) không hết giờ. Tệp chưa chạy khi hết ngân sách ghi `BỎ QUA (hết thời gian)`.
- Thư mục chứa tệp: `DEVICE_SCENARIOS_GLOB`, mặc định `docs/features/*/device-scenarios/*.txt`, để test dùng thư mục riêng.

**`scripts/device-session.sh`.**
- Thêm một bước 4 vào cuối `cmd_scenarios`, sau monkey: `python3 scripts/device-scenario.py run --session "${dir}" --package "${pkg}"`. Lỗi của trình chạy không dừng `scenarios`; nó được ghi một dòng `plan runner_error` vào `scenarios.txt`.
- Hàm `network_off`, `network_on`, `grant_permissions` giữ nguyên. Trình chạy gọi lại chúng qua lệnh ẩn `scripts/device-session.sh _helper <tên hàm>`, để chỉ có một cách tắt mạng và cấp quyền.
- Đưa lệnh `_helper` vào phần Usage.
- `cmd_summary` thêm mục "## Kịch bản theo plan". Bảng gồm các cột Tệp, Ticket, Tiêu chí, Kết quả (`đạt`, `lỗi ở bước n: <lệnh>`, `bỏ qua`, `lỗi tệp`), Ảnh, đọc từ các dòng `plan ...` của `scenarios.txt`.
- Mức của mọi dòng là "xem"; biến `blocking` không đổi.

**Vỏ app.** `MainActivity` đặt `Modifier.semantics { testTagsAsResourceId = true }` trên `Surface` gốc, qua một hàm `Modifier.exposeTestTagsForUiAutomator()` trong tệp mới `app/src/main/.../ui/UiAutomatorTags.kt`, để có test riêng.
- `testTag` đã có ở mọi màn chính: `camera_screen`, `camera_poses`, `poses_screen`, `poses_grid`, `pose_detail_screen`, `pose_detail_bottom_bar`, `onboarding_screen`, `onboarding_enable_camera`. Kịch bản dùng `id=` thay vì chữ, nên không phụ thuộc ngôn ngữ của emulator.
- Hộp thoại mạng là một cửa sổ khác, không nhận thuộc tính này. Kịch bản tìm nó bằng `str=` (câu hỏi 2, mục 8).

**Mẫu và tài liệu.**
- **`docs/templates/plan.md` mục 4:** dòng mẫu "Kiểm tra thủ công" thành hai dòng: `Kiểm tra thủ công (emulator)` nêu tệp kịch bản, và `Kiểm tra thủ công (máy thật: <lý do>)`. Chú thích liệt kê các nhóm lý do:
  - camera thật;
  - FPS, khung hình;
  - Play (cài từ Play, In-App Review);
  - Remote Config;
  - quảng cáo thật;
  - gập có bản lề;
  - cử chỉ nhiều ngón;
  - TalkBack;
  - khác, kèm lời giải thích.
- **`docs/CLAUDE.md`, mục Khi viết plan:** một gạch đầu dòng. Tiêu chí kiểm thủ công chạy được trên emulator thì viết kịch bản (cách viết ở `docs/ops/test-sessions.md`, mục 5) và đưa tệp vào mục 3.2. Tệp kịch bản là test của plan: viết ở bước 3 cùng test khác, nhưng chỉ chạy được ở `device-session` cuối phiên, nên không thể "thất bại trước"; ghi vào ngoại lệ viết test trước.
- **`docs/ops/test-sessions.md`:**
  - mục 4 thêm một đoạn về bước "kịch bản theo plan";
  - thêm mục mới 5 "Kịch bản theo plan", với bảng lệnh ở trên, ví dụ, cách chạy trên máy dev (`scripts/device-session.sh start`, `scenarios --yes`, `stop`), giới hạn thời gian;
  - mục "Quy tắc về dữ liệu" dời thành 6.
- **`docs/auto-run.md` mục 4, Kết thúc phiên:** hai câu thêm vào ý "Phân tích thư mục phiên".
  - Đọc bảng "Kịch bản theo plan" của `summary.md`.
  - Mỗi dòng `lỗi` là một phát hiện: bước lỗi, `fail.png`, đoạn log quanh dấu `VGL.Session`. Soạn phản hồi `FB-...` mức `nen-sua`, ghi vào "Cần người kiểm tra".
  - Dòng `lỗi tệp` hay `bỏ qua` ghi vào báo cáo như lỗi của tệp kịch bản.
  - Kịch bản lỗi không chặn phát staging (câu hỏi 1, mục 8).
  - Ý về "Những gì người cần tự kiểm tra" (dòng 308) đổi thành "tiêu chí kiểm tra thủ công máy thật".
- **`docs/templates/run.md`:**
  - mục "Kiểm thử trên máy ảo" thêm dòng `- Kịch bản theo plan: <n đạt, n lỗi, n bỏ qua; tệp lỗi và bước>`;
  - dòng "Tiêu chí kiểm tra thủ công" đổi thành "Tiêu chí kiểm tra thủ công (máy thật)", và thêm dòng "Kịch bản emulator lỗi".

**Ba kịch bản đầu tiên** (mục còn chờ của RUN-20261023-04). Mỗi tệp bắt đầu bằng `fresh`, `consent decline`, `wait id=onboarding_screen|id=camera_screen 60` (tải catalog có thể tới 47 giây, như `AppLaunchTest`), `tap_if id=onboarding_enable_camera`, `wait id=camera_screen 15`.

1. `docs/features/poses/device-scenarios/T-022-chi-tiet-sang-camera.txt` (plan T-022, mục 4, bước 1 đến 6; bước 7 tiếng Anh dùng `device locale en`):
   - chạm `id=camera_poses`, chờ `id=poses_grid`;
   - chạm thẻ đầu (`in=poses_grid index=1`), chờ `id=pose_detail_screen`, `shot detail`;
   - `scroll id=pose_detail_screen 5`, `expect id=pose_detail_bottom_bar`;
   - chạm `desc=` theo `str=poses_cd_save`, rồi `back`, chờ `id=poses_grid`, `expect` biểu tượng đã lưu ở thẻ đầu;
   - mở lại chi tiết, chạm `str=poses_detail_shoot`, chờ `id=camera_screen`, `back` rồi `expect_no id=camera_screen` (thoát app);
   - `network off`, mở thẻ thứ 3 (ảnh chưa tải), chờ hộp thoại mạng, `shot offline`, `network on`, chạm "Thử lại", chờ `id=pose_detail_image`;
   - `device night on`, `shot dark`.
2. `docs/features/poses/device-scenarios/T-024-thu-vien-mo-gap.txt` (plan T-024, mục 4, bước 1 đến 4, bằng đổi kích thước thay cho gập thật):
   - vào thư viện, `device unfold`, chờ `id=poses_grid`, `shot unfolded`;
   - chạm chip thứ 2, `scroll id=poses_grid 2`;
   - `device fold`, `expect id=poses_grid`, `expect` chip thứ 2 đang chọn, `shot folded`;
   - mở chi tiết khi mở gập, `expect id=pose_detail_bottom_bar`, `device fold`, `expect id=pose_detail_screen`;
   - `device unfold`, chạm "Chụp với tư thế này", chờ `id=camera_screen`.
3. `docs/features/camera/device-scenarios/T-020-camera-mo-gap.txt` (plan T-020, mục 4, phần bố cục; không có camera thật):
   - ở camera, chạm ô tư thế thứ 2 của `id=camera_strip`, bật `id=camera_grid_button`;
   - `device unfold`, `expect id=camera_screen`, `expect id=camera_grid`, `shot unfolded`;
   - `device fold`, `expect id=camera_grid`, `expect id=camera_strip`.

   Phần "chụp, màn xem lại" không chạy: emulator CI tắt camera sau (`-camera-back none`), và camera trước chưa chắc có (mục 3.4).

**Không làm trong plan này.** T-018 và T-023 (quảng cáo theo Remote Config), FPS, camera thật, Play In-App Review, TalkBack, gập thật có bản lề và tư thế bàn, chụm hai ngón (T-015). Chúng là "máy thật". Emulator gập thật cần đổi `device-session.yml` (thêm `profile`), là plan riêng (câu hỏi 3, mục 8).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/device-scenario.py` | Đọc và kiểm cú pháp tệp kịch bản (`check`), chạy kịch bản qua `adb` (`run`), ghi `scenarios.txt`, ảnh, dump khi lỗi; ngân sách thời gian; trả máy về trạng thái gốc |
| Sửa | `scripts/device-session.sh` | Bước 4 của `cmd_scenarios` gọi trình chạy; lệnh ẩn `_helper` cho `network_off`, `network_on`, `grant_permissions`; mục "Kịch bản theo plan" của `cmd_summary`; Usage |
| Sửa | `scripts/tests/fake-adb` | `uiautomator dump` đọc lần lượt các tệp của `$FAKE_ADB_UI_SEQ` (đếm qua tệp), `exec-out cat` in tệp hiện tại; `input`, `wm`, `cmd uimode`, `settings`, `cmd locale` ghi vào `$FAKE_ADB_CALLS` |
| Tạo | `scripts/tests/fixtures/device-scenario/onboarding.xml` | Dump mẫu: màn chào có `resource-id="onboarding_enable_camera"`, bounds thật |
| Tạo | `scripts/tests/fixtures/device-scenario/camera.xml` | Dump mẫu: `camera_screen`, `camera_poses` |
| Tạo | `scripts/tests/fixtures/device-scenario/grid.xml` | Dump mẫu: `poses_grid` có 3 thẻ con (cho `in=`, `index=`) |
| Tạo | `scripts/tests/fixtures/device-scenario/consent.xml` | Dump mẫu: hộp thoại đồng ý có nút `Do not consent` |
| Tạo | `scripts/tests/fixtures/device-scenario/not-idle.txt` | Đầu ra `ERROR: could not get idle state.` |
| Tạo | `scripts/tests/fixtures/device-scenario/ok.txt` | Tệp kịch bản đạt trên chuỗi dump mẫu |
| Tạo | `scripts/tests/fixtures/device-scenario/fail.txt` | Tệp kịch bản lỗi ở bước 3 |
| Tạo | `scripts/tests/fixtures/device-scenario/bad.txt` | Tệp sai cú pháp (lệnh lạ ở dòng 5) |
| Sửa | `scripts/tests/run.sh` | Khối `device-scenario` (sau khối `device-session`, trước dòng tổng kết); khối `plan-check` thêm test quy tắc 11 |
| Sửa | `scripts/plan-check.py` | Quy tắc 11: dòng "Kiểm tra thủ công" ghi nơi chạy; tệp kịch bản có trong mục 3.2 hay repo và đúng cú pháp (gọi hàm đọc của `device-scenario.py`); lỗi với plan `draft`, chú ý với plan `approved`; docstring |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/ui/UiAutomatorTags.kt` | `Modifier.exposeTestTagsForUiAutomator()` đặt `testTagsAsResourceId = true` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | `Surface(modifier = Modifier.fillMaxSize().exposeTestTagsForUiAutomator())` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ui/UiAutomatorTagsTest.kt` | Robolectric: gốc có thuộc tính, cây không có modifier thì không có (đối chứng) |
| Tạo | `docs/features/poses/device-scenarios/T-022-chi-tiet-sang-camera.txt` | Kịch bản 1 (mục 3.1) |
| Tạo | `docs/features/poses/device-scenarios/T-024-thu-vien-mo-gap.txt` | Kịch bản 2 |
| Tạo | `docs/features/camera/device-scenarios/T-020-camera-mo-gap.txt` | Kịch bản 3 |
| Sửa | `docs/templates/plan.md` | Mục 4: hai dòng mẫu kiểm thủ công, chú thích nhóm lý do "máy thật" |
| Sửa | `docs/CLAUDE.md` | Mục Khi viết plan: một gạch đầu dòng về kịch bản emulator |
| Sửa | `docs/ops/test-sessions.md` | Mục 4 một đoạn; mục mới 5 "Kịch bản theo plan"; mục 6 (dời) |
| Sửa | `docs/auto-run.md` | Mục 4, Kết thúc phiên, ý "Phân tích thư mục phiên" và ý "Những gì người cần tự kiểm tra" |
| Sửa | `docs/templates/run.md` | Mục Kiểm thử trên máy ảo một dòng; mục Cần người kiểm tra hai dòng |

`.github/workflows/device-session.yml` gọi `scripts/device-session.sh` (`start ci`, `scenarios --yes`, `stop`) và đọc `test-sessions/*/summary.md`. Nó không đổi: cùng lệnh, cùng tên tệp, và thư mục `test-sessions/` mà artifact lưu đã gồm `scenarios/`. `scripts/app-explore/ax.sh` dùng cùng cách dump nhưng không bị sửa.

Nơi khác đọc các file plan này sửa (L-020), và vì sao không bị ảnh hưởng:
- **`docs/auto-run.md`, `docs/templates/run.md`:** các script và workflow sau chỉ nhắc hai tài liệu này trong chú thích hay thông báo, chỉ tới một mục mà plan không đổi tên:
  - `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`;
  - `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/resume-check.py`, `scripts/save-branch.sh`.

  `scripts/resume-check.py` đọc `status:` của danh sách chạy, không đọc các mục plan này sửa. Plan không đổi tên mục nào, chỉ thêm câu và dòng.
- **`docs/CLAUDE.md`:** `scripts/required-assets.txt` nhắc nó trong chú thích về mục Khi viết plan. Mục đó vẫn còn, plan chỉ thêm một gạch đầu dòng.
- **`scripts/tests/run.sh`:** `scripts/main-guard-status.sh` và `scripts/merge-if-green.sh` nhắc nó trong chú thích. Khối test của chúng không đổi; plan chỉ thêm khối mới.
- **`scripts/plan-check.py`:** `scripts/run-check.py` nạp nó (quy tắc 1 của `run-check.py`), nên lỗi của quy tắc 11 cũng hiện khi kiểm danh sách chạy. Đó là ý muốn. Quy tắc 11 chỉ là chú ý với plan `approved`, nên không danh sách nào đã duyệt bị hỏng; test "approved plans of the repository have no rule 11 error" kiểm điều này.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Kịch bản dựa vào các cờ đã bật trong `StagingFeatureFlagsModule` (`POSES`, `SETTINGS`).
- Quảng cáo: Không chạm. `consent decline` chỉ chạm nút của hộp thoại đồng ý có sẵn (ADR-0013) trên emulator.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm (kịch bản tải catalog staging như `AppLaunchTest`).
- **Quyền truy cập:** Không thêm. Kịch bản cấp quyền runtime đã khai báo bằng `pm grant` như kịch bản chuẩn.
- **Thư viện và SDK:** Không thêm. `testTagsAsResourceId` thuộc `androidx.compose.ui` (`compose-ui` đã có trong `gradle/libs.versions.toml`, module `:app` đã phụ thuộc); trình chạy là Python chuẩn.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** emulator của CI có thể không cho thấy `resource-id` của Compose trong `uiautomator dump`, hay dump luôn báo `could not get idle state` ở một màn (camera có preview chạy liên tục). Cũng chưa chắc emulator có camera trước, để camera hiện mà không báo lỗi.
- **Điều kiện chuyển:** chỉ biết được ở lần `device-session` đầu tiên sau khi merge, không biết được trong phiên trước khi merge (mục 4.1). Vì vậy phương án dự phòng không do phiên chạy task này quyết. Phiên ghi kết quả lần chạy đầu vào báo cáo:
  - kịch bản lỗi ở `wait id=...` dù `fail.png` cho thấy đúng màn;
  - hoặc mọi dump của một màn đều là `could not get idle state`.
- **Cách làm thay:** viết thành plan phiên bản 2 cho một task sửa sau.
  - Bộ chọn dùng `str=` hay `desc=` thay cho `id=`, không cần `resource-id`.
  - `wait` thêm dạng `log=<regex>`: chờ một dòng `VGL.*` trong logcat của phiên, nhờ log đổi trạng thái chính đã có theo ADR-0019.

  Tiêu chí và test của mục 4 giữ nguyên. Plan này không cài sẵn phần dự phòng.

## 4. Kiểm thử

Test viết trước code: các khối mới trong `scripts/tests/run.sh` và `UiAutomatorTagsTest`. Khung rỗng của bước 3:
- `scripts/device-scenario.py` chỉ có phần đọc tham số và luôn trả 0, không in gì;
- `device-session.sh` chưa gọi nó;
- `exposeTestTagsForUiAutomator()` trả `this`.

Chuỗi dump của `adb` giả được dựng bằng `FAKE_ADB_UI_SEQ` (danh sách tệp, mỗi lần `uiautomator dump` lấy tệp kế tiếp, hết thì giữ tệp cuối).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Kịch bản đạt | Test script | `device-scenario: a passing file is reported as passed`: `ok.txt` trên chuỗi `consent.xml`, `onboarding.xml`, `camera.xml`; `scenarios.txt` có `plan ok.txt step=` cho mọi bước và `ok`; mã 0 |
| Chạm đúng tâm | Test script | `device-scenario: tap uses the centre of the bounds`: `$FAKE_ADB_CALLS` có `shell input tap 540 1806` cho nút `[96,1740][984,1872]` của `onboarding.xml` |
| Hộp thoại đồng ý | Test script | `device-scenario: consent decline taps Do not consent when shown`: có lệnh chạm vào nút; đối chứng: dump không có hộp thoại thì không có lệnh chạm nào trước `wait` |
| `in=` và `index=` | Test script | `device-scenario: in= and index= pick the second card of the grid`: chạm tâm thẻ thứ 2 của `grid.xml`, không phải thẻ ngoài lưới |
| `str=` | Test script | `device-scenario: str= reads the default strings`: `str=poses_detail_shoot` khớp nút có chữ "Chụp với tư thế này" trong một dump mẫu; tên chuỗi không có thì `LỖI TỆP` |
| Kịch bản lỗi | Test script | `device-scenario: the first failing step stops the file`: `fail.txt`: dòng `step=3 ... FAIL`, không có dòng `step=4`, có `fail.png` và `fail.xml` trong `scenarios/fail.txt/`; mã 0 (lỗi kịch bản không phải lỗi của trình chạy) |
| Đối chứng (L-035) | Test script | `device-scenario: a passing file leaves no fail.png`: sau `ok.txt`, thư mục của tệp không có `fail.png`. Trên khung rỗng test "kịch bản lỗi" thất bại vì không có dòng nào |
| Chờ khi chưa rảnh | Test script | `device-scenario: not idle is retried until the timeout`: chuỗi `not-idle.txt`, `not-idle.txt`, `camera.xml`; `wait id=camera_screen 5` đạt; chuỗi chỉ `not-idle.txt` thì `FAIL` với lý do `could not get idle state` |
| Trả máy về gốc | Test script | `device-scenario: the device is reset after a failing file`: sau `fail.txt`, `$FAKE_ADB_CALLS` có `wm size reset`, `wm density reset`, `cmd uimode night no`, `user_rotation 0` |
| Ngân sách | Test script | `device-scenario: files beyond the budget are skipped`: `DEVICE_SCENARIOS_BUDGET=0`, hai tệp: cả hai `BỎ QUA (hết thời gian)` |
| Cú pháp | Test script | `device-scenario: check reports an unknown command with its line`: `check bad.txt` mã 1, in tên tệp `bad.txt` kèm `dòng 5`; `run` với `bad.txt` ghi `LỖI TỆP` và không gửi lệnh `input` nào |
| `device-session` gọi trình chạy | Test script | `device-session: scenarios runs the plan scenarios after monkey`: `DEVICE_SCENARIOS_GLOB` trỏ `ok.txt`; `scenarios.txt` có dòng `monkey` trước dòng `plan ok.txt` |
| Bảng trong `summary.md` | Test script | `device-session: summary lists the plan scenarios`: có `## Kịch bản theo plan`, dòng `ok.txt` `đạt`, dòng `fail.txt` `lỗi ở bước 3`; `stop` vẫn mã 0 khi không crash (lỗi kịch bản không chặn) |
| Trình chạy hỏng không dừng `scenarios` | Test script | `device-session: a runner error does not stop scenarios`: `DEVICE_SCENARIO_CMD` trỏ một lệnh trả 1; `scenarios` mã 0, `scenarios.txt` có `plan runner_error` |
| `plan-check` quy tắc 11 | Test script | `plan-check: a manual row without where it runs is an error for a draft plan`; `... only a warning for an approved plan`; `... an emulator row needs a scenario file in the file list`; `... a scenario file with an unknown command is an error`; `... máy thật needs a reason`. Đối chứng: plan mẫu đúng không có lỗi quy tắc 11 |
| Plan đã duyệt không hỏng | Test script | `plan-check: approved plans of the repository have no rule 11 error`: chạy trên mọi plan `approved`, `done` hiện có, không có dòng `LỖI` của quy tắc 11 |
| Gốc giao diện có thuộc tính | Compose UI test (Robolectric) | `UiAutomatorTagsTest.exposeTestTags_setsTestTagsAsResourceId`: `onNode(expectValue(TestTagsAsResourceId, true)).assertExists()`; đối chứng `withoutModifier_hasNoTestTagsAsResourceId` (như mục 4.1) |
| Ba kịch bản cú pháp đúng | Test script | `device-scenario: the scenarios of the repository pass check`: `check` trên mọi `docs/features/*/device-scenarios/*.txt`, mã 0 |
| Ba kịch bản chạy thật | Kiểm tra thủ công (emulator) | Lần `device-session` đầu tiên sau khi merge: bảng "Kịch bản theo plan" có 3 dòng; phiên ghi kết quả từng dòng vào báo cáo; dòng lỗi thì đọc `fail.png` để biết lỗi của app hay của kịch bản (mục 3.4) |
| Phiên làm theo tài liệu | Kiểm tra thủ công (máy thật: khác, đọc tài liệu) | Người duyệt chéo đọc diff `docs/auto-run.md`, `docs/templates/run.md`, `docs/ops/test-sessions.md` |

**Ngoại lệ viết test trước:**
- `docs/templates/plan.md`, `docs/CLAUDE.md`, `docs/ops/test-sessions.md`, `docs/auto-run.md`, `docs/templates/run.md` là tài liệu.
- Ba tệp kịch bản là test chạy trên emulator. Phiên chỉ kiểm được cú pháp của chúng (test "the scenarios of the repository pass check"); chạy thật ở `device-session` cuối phiên.
- Dòng sửa `MainActivity.kt` dùng hàm đã có test. `MainActivity` dựng Hilt và Firebase nên không có test JVM; nó được kiểm qua build và `AppLaunchTest` trên CI.

### 4.1. Chạy thử

- **Đã chạy thử:**
  - Ngày 2026-10-24, trên nhánh chính `795028c`, máy dev (JDK 21).
  - Test tạm `app/src/test/.../trial/ExposeTestTagsTrialTest.kt`, không commit, đã xóa. Hai test:
    - `Box(Modifier.semantics { testTagsAsResourceId = true })` có `SemanticsPropertiesAndroid.TestTagsAsResourceId == true`;
    - đối chứng: không có modifier thì không có node nào mang giá trị đó.
- **Lệnh:** `./gradlew :app:testStagingDebugUnitTest --tests '*ExposeTestTagsTrialTest*'`, qua: `tests="2" failures="0"`. Đây là phần JVM; toàn bộ sẽ chạy bằng `scripts/check.sh` ở bước 5. API biên dịch được với phiên bản Compose hiện tại, cần `@OptIn(ExperimentalComposeUiApi::class)`.
- **Kết quả, và điều chưa thử được:**
  - Môi trường này không có emulator. `device-session.yml` chỉ chạy trên nhánh chính (`if: github.ref == 'refs/heads/main'`), nên không có PR thử nào chạy được nó trước khi merge.
  - Vì vậy chưa thử được ba điều:
    - `uiautomator dump` thấy `resource-id` của Compose;
    - camera hiện trên emulator không có camera sau;
    - hộp thoại đồng ý hiện chữ `Do not consent`.
  - Ba điều này được kiểm ở `device-session` đầu tiên sau khi merge (dòng "Ba kịch bản chạy thật", mục 4) và có phương án sửa ở mục 3.4. Kịch bản lỗi không chặn staging, nên điều chưa chắc này không làm hỏng phiên.
  - Phần đọc dump và chạm tâm `bounds` là phân tích XML, đã có mẫu trong `scripts/app-explore/guard.py`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI và quy trình phiên tự chạy): đổi `scripts/device-session.sh` mà `device-session.yml` chạy, `scripts/plan-check.py` mà phiên chạy trước mỗi task, và bước cuối phiên của `docs/auto-run.md`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script mới, hai script sửa, một dòng vỏ app, ba kịch bản, năm tài liệu |
| B. Độ rõ của yêu cầu | 1 | L-062 rõ; còn câu hỏi về mức của kịch bản lỗi và nơi chạy kịch bản gập (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Trình chạy, `plan-check` và thuộc tính gốc có test; phần trên emulator thật chỉ kiểm được sau khi merge |
| D. Ảnh hưởng chéo | 2 | `auto-run.md`, `docs/templates/run.md`, `docs/templates/plan.md`, `plan-check.py` cùng được T-045, T-047 sửa; mọi plan sau dùng quy tắc 11 |
| E. Độ mới của kỹ thuật | 2 | `uiautomator dump` có trong `scripts/app-explore/`, nhưng chạy kịch bản trên emulator CI và `testTagsAsResourceId` thì lần đầu; đã chạy thử phần JVM (mục 4.1) |
| **Tổng** | 9 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:**
  - `scripts/device-session.sh` (máy dev và CI), `scripts/plan-check.py` (mọi plan);
  - `docs/auto-run.md` mục 4, `docs/templates/run.md`, `docs/templates/plan.md`;
  - `scripts/tests/run.sh`, `scripts/tests/fake-adb`;
  - vỏ app `MainActivity.kt`.
- **Task khác có thể xung đột (viết song song):**
  - **T-044** (`scripts/wait-run.sh --pr` chỉ nhận lần chạy của commit hiện tại): không sửa cùng file, trừ `scripts/tests/run.sh`. T-044 thêm vào khối `wait-run`, plan này thêm khối `device-scenario` sau khối `device-session`. Có thể có `docs/auto-run.md` bước 8, nhưng plan này chỉ sửa mục 4.
  - **T-045** (hàng chờ plan cuối phiên): sửa `docs/auto-run.md` mục 4, Kết thúc phiên (thêm bước soạn plan sau báo cáo) và `docs/templates/run.md` (mục câu hỏi cần người).
    - Plan này chỉ sửa ý "Phân tích thư mục phiên" của bước kiểm thử trên máy ảo, ý "Những gì người cần tự kiểm tra", mục "Kiểm thử trên máy ảo" và hai dòng của "Cần người kiểm tra" trong mẫu báo cáo. Khác đoạn, chỉ có thể xung đột dòng khi gộp.
    - Nếu T-045 sửa mẫu plan hay `plan-check.py`, hai plan cùng sửa các file đó ở khác chỗ: plan này chỉ sửa mục 4 của mẫu và thêm quy tắc 11.
  - **T-047** (sẵn sàng phát hành trong `scripts/status.py`): đọc mục kiểm thủ công chưa đánh dấu trong báo cáo các phiên. Plan này đổi tên dòng mẫu thành "Tiêu chí kiểm tra thủ công (máy thật)" và thêm dòng "Kịch bản emulator lỗi". T-047 nên đọc cả hai dòng mới, nhưng vẫn đọc được dòng cũ của các báo cáo trước.
- **Đề xuất thứ tự chạy:** T-046 trước T-047. T-044 độc lập. Với T-045: trước hay sau đều được; ai chạy sau thì gộp `docs/auto-run.md` mục 4.

## 7. Điều kiện dừng riêng

1. Cần sửa `.github/workflows/device-session.yml` (ví dụ: trình chạy cần thêm thời gian hay công cụ mà runner không có): dừng, viết plan riêng.
2. `testTagsAsResourceId` làm đổi ảnh chuẩn screenshot hay làm hỏng một test có sẵn của `:app`: dừng. Theo mục 4.1 thì không nên xảy ra, vì đây chỉ là thuộc tính ngữ nghĩa.
3. Viết quy tắc 11 xong mà có plan `approved` của danh sách chạy đang chờ bị báo lỗi, không phải chú ý: dừng. Quy tắc phải theo L-065.

## 8. Câu hỏi cho người duyệt

1. **Kịch bản emulator lỗi thì có chặn phát staging không?**
   Đề xuất của AI: không. Kịch bản lỗi chỉ thành phát hiện mức "xem", có phản hồi `FB-...` `nen-sua` và nằm trong "Cần người kiểm tra". Lý do:
   - Lần chạy đầu còn điều chưa chắc (mục 3.4).
   - Một kịch bản sai có thể chặn mọi bản staging.
   - Hiện chỉ crash và ANR chặn.

   Sau vài phiên ổn định, có thể đổi thành chặn bằng một plan khác.
2. **Bật `testTagsAsResourceId` cho cả bản production?**
   Đề xuất của AI: có, đặt ở `MainActivity` cho mọi biến thể. Thuộc tính chỉ đưa tên `testTag` (tên kỹ thuật bằng tiếng Anh, không có dữ liệu người dùng) vào cây trợ năng, không đổi giao diện hay hành vi. Tách riêng cho staging thì cần một cơ chế theo biến thể cho một dòng.

   Hộp thoại mạng là cửa sổ riêng, không nhận thuộc tính. Kịch bản tìm nó bằng `str=` thay vì sửa `:core:designsystem` (điểm chặn 7).
3. **Gập thật trên emulator?**
   Đề xuất của AI: plan này dùng `wm size`, `wm density` để kiểm bố cục rộng và việc giữ trạng thái khi đổi kích thước (T-024 bước 1 đến 4, phần bố cục của T-020). Gập thật, có bản lề, tư thế bàn và màn ngoài thì vẫn là "máy thật: gập có bản lề".

   Nếu muốn emulator gập, cần một ticket riêng sửa `device-session.yml` (thêm `profile` cho AVD gập, khoảng 10 phút CI nữa). Ticket đó rủi ro cao, cần duyệt chéo.
4. **Quảng cáo trên emulator (T-018, T-023)?**
   Đề xuất của AI: giữ là "máy thật: Remote Config" cho tới khi có cách bật vị trí quảng cáo riêng ở staging. T-018 đã đề xuất cách đó thành ticket riêng (plan T-018, câu hỏi 2). Plan này không thêm kịch bản quảng cáo.
5. **Ngân sách thời gian 900 giây (15 phút) cho mọi kịch bản của một phiên?**
   Đề xuất của AI: được. Job hiện chạy khoảng 15 đến 25 phút với giới hạn 50 phút, còn khoảng 25 phút. Mỗi kịch bản giới hạn 180 giây (tối đa 300), nên đủ cho 5 đến 8 kịch bản. Khi số kịch bản vượt ngân sách, tệp bị bỏ qua được ghi rõ, và cần xem lại thứ tự hay tách workflow.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
