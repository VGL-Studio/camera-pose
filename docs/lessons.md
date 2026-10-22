# SỔ BÀI HỌC

Sổ này ghi những điều nhóm và AI học được khi làm việc, và đã làm gì với chúng. Đây là đầu vào của buổi retro sau mỗi phiên tự chạy (auto-run.md, mục 6).

## Cách ghi

- Mỗi bài học một mục, mã `L-NNN` tăng dần. Chỉ thêm mục mới hoặc cập nhật dòng **Trạng thái**, **Áp dụng**, **Kiểm chứng** của mục cũ; không sửa phần mô tả.
- Trạng thái:
  - `proposed`: AI hoặc người vừa ghi, chưa ai quyết định.
  - `accepted`: retro đồng ý làm, chưa làm.
  - `applied`: đã làm, ghi PR ở dòng **Áp dụng**.
  - `verified`: phiên sau cho thấy cải tiến có hiệu quả, ghi bằng chứng ở dòng **Kiểm chứng**.
  - `rejected`: retro quyết định không làm, ghi lý do.
- Loại: `quy trình` (auto-run, docs/CLAUDE.md, mẫu tài liệu), `môi trường` (cloud, CI, script), `quy tắc code` (CLAUDE.md), `plan` (cách viết plan, test), `rủi ro` (thang điểm).
- Phạm vi: `chung` (đúng với mọi app, làm trong repo dự án mẫu trước), `app` (chỉ app này).
- Chỉ người chuyển trạng thái sang `accepted`, `rejected` hay `verified`. AI chỉ ghi `proposed`, và ghi `applied` khi PR cải tiến đã merge.

Bài học `chung` ghi ở app thì được đưa về dự án mẫu qua issue `from-app` (`docs/ops/from-app.md`). Ở dự án mẫu, bài học từ app ghi nguồn là app và số issue. Các app đã tạo trước đó đọc sổ của dự án mẫu để chép những cải tiến cần thiết về (chưa có cách đồng bộ tự động).

## Bài học

<!-- Các bài học ghi dưới dòng này. -->

Bài học trước ngày 2026-10-21 nằm trong sổ bài học của repo dự án mẫu (commit 9475da7).

### L-001: Kỹ thuật đo lần đầu dùng phải chạy thử trước khi duyệt plan

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-01, T-007.
- **Chuyện gì:** plan T-007 dựa vào `captureToImage` trong Robolectric `NATIVE` để đo màu điểm ảnh; kỹ thuật này hết thời gian chờ, task dừng ở bước 3.
- **Bằng chứng:** `ComposeTimeoutException` tại `WindowCapture.forceRedraw`; nhật ký plan T-007.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: kỹ thuật hay thư viện lần đầu dùng trong dự án phải được chạy thử bằng một test tạm không commit trước khi duyệt, kết quả ghi vào mục 4 (mở rộng L-029).
- **Áp dụng:**
- **Kiểm chứng:**

### L-002: Bí mật Firebase staging là file giả, cuối phiên không phát được

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261021-01.
- **Chuyện gì:** `STAGING_GOOGLE_SERVICES_JSON` vẫn là file giả; `device-session.yml` và `staging.yml` lỗi ở bước cấu hình, sau khi cả phiên đã chạy.
- **Bằng chứng:** `device-session` 39589532354 và 39589890609, `staging` 39589924059: `processStagingReleaseGoogleServices: No matching client found for package name '...guide.staging'`.
- **Đề xuất sửa:** `docs/auto-run.md`, mục 2: kiểm tra đầu phiên có một điều kiện "bí mật Firebase staging có app staging", để lỗi lộ ra trước khi phiên bắt đầu.
- **Áp dụng:**
- **Kiểm chứng:**

### L-003: Quy tắc đếm `captureRoboImage(` của `check.sh` chặn ảnh tạm để đo điểm ảnh

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-02, T-007.
- **Chuyện gì:** plan T-007 phiên bản 2 đo màu điểm ảnh từ ảnh `captureRoboImage` ghi vào `build/`; code và test qua, nhưng `scripts/check.sh` đòi mọi lần gọi `captureRoboImage(` ghi `"src/test/screenshots/<Tên>.png"`, nên cổng chất lượng không qua và task dừng ở bước 5.
- **Bằng chứng:** `scripts/check.sh` dòng 45 đến 55; log `::error:::core:designsystem: mọi captureRoboImage phải ghi rõ đường dẫn`; nhật ký plan T-007.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: kỹ thuật chụp hay đo mới phải được chạy thử cùng `scripts/check.sh`, không chỉ chạy test; tìm script đọc file test (`grep -n captureRoboImage scripts/`) như L-020. Hoặc đổi `check.sh` cho phép ảnh tạm ghi vào `build/` (qua dự án mẫu).
- **Áp dụng:**
- **Kiểm chứng:**

### L-004: Thuộc tính trợ năng của Compose không lan từ node cha xuống node con

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261021-03, T-011.
- **Chuyện gì:** plan T-011 ẩn màn hình phía sau hộp thoại khỏi TalkBack bằng `Modifier.semantics { hideFromAccessibility() }` trên khung bọc nội dung. Thuộc tính chỉ nằm trên node của khung; nút bên trong vẫn hiện với trình đọc màn hình. Test viết trước (nút phải mang `HideFromAccessibility`) bắt được, task dừng ở bước 4.
- **Bằng chứng:** `NetworkDialogHostTest.shown_blocksContentClicks_andHidesItFromAccessibility` trên nhánh `claude/T-011-core-network-hop-thoai` (commit 555eb12); bytecode Compose UI 1.9.1: `SemanticsOwnerKt.isHidden` chỉ đọc cấu hình của chính node, `AndroidComposeViewAccessibilityDelegateCompat` gọi `setVisibleToUser(!isHidden)` cho từng node; nhật ký plan T-011.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: thay đổi về trợ năng (ẩn, gộp, thứ tự đọc) phải được chạy thử bằng test tạm trước khi duyệt, đo trên cây `AccessibilityNodeInfo` hay node đích, không suy ra từ node cha (mở rộng L-001).
- **Áp dụng:**
- **Kiểm chứng:**

### L-005: Môi trường `claude --cloud` chặn máy chủ R2 staging, plan không kiểm được file bằng `curl`

- **Trạng thái:** proposed. **Loại:** môi trường. **Phạm vi:** chung. **Nguồn:** RUN-20261022-01, T-010.
- **Chuyện gì:** plan T-010 (mục 4, dòng cuối; mục 7, điều 3) yêu cầu `curl` 9 địa chỉ trên `https://camera-pose.vgl.io.vn/` trước bước 3. Proxy của môi trường cloud trả 403 cho máy chủ này (chính sách mạng), nên `curl` ra mã `000`. Phiên dựa vào ghi chú của danh sách chạy (dev đã kiểm, `200`) và `AppLaunchTest` trên CI (đã qua).
- **Bằng chứng:** `curl: (56) CONNECT tunnel failed, response 403` lúc 02:08 UTC; rà soát độc lập T-010 cũng không ra mạng được; CI lần chạy 39710999529 `ui-test` xanh.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: điều kiện về file trên R2 kiểm bằng một workflow trên GitHub (ví dụ thêm bước vào `device-session.yml` hay một workflow chỉ đọc) hoặc ghi rõ "dev kiểm trước phiên", không bằng `curl` từ phiên; hoặc thêm máy chủ R2 staging vào danh sách mạng cho phép của môi trường cloud (`docs/ops/` hướng dẫn dựng môi trường).
- **Áp dụng:**
- **Kiểm chứng:**

### L-006: `captureToImage()` hết giờ trên Robolectric; đo điểm ảnh bằng cách vẽ view gốc ra Bitmap

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** chung. **Nguồn:** RUN-20261022-01, T-008 (lần thứ hai, sau T-007).
- **Chuyện gì:** plan T-008 ghi test màu nền dùng `onNodeWithTag(...).captureToImage()`. Trên Robolectric (`GraphicsMode.NATIVE`) lệnh này ném `ComposeTimeoutException` sau 2 giây, như T-007 đã gặp. Phiên đổi sang cách của `ComponentsTest` (vẽ `decorView` ra `Bitmap`), điều test kiểm tra không đổi.
- **Bằng chứng:** `background_isBackgroundRole_light`: `ComposeTimeoutException: Condition still not satisfied after 2000 ms` ở bước 3; nhật ký plan T-008, mục Lệch so với plan.
- **Đề xuất sửa:** `CLAUDE.md`, mục Viết một feature mới, bước 10: test đo màu điểm ảnh trên JVM vẽ view gốc ra `Bitmap` (hàm `pixelAt` của `ComponentsTest`), không dùng `captureToImage()`. Có thể đưa `pixelAt` vào một tiện ích test dùng chung.
- **Áp dụng:**
- **Kiểm chứng:**

### L-007: Hàm CameraX trả `ListenableFuture` không gọi được vì Guava không có trên classpath biên dịch

- **Trạng thái:** proposed. **Loại:** plan. **Phạm vi:** app. **Nguồn:** RUN-20261022-01, T-013.
- **Chuyện gì:** `CameraControl.setZoomRatio` trả `com.google.common.util.concurrent.ListenableFuture`; Kotlin báo "Cannot access class 'ListenableFuture'". Plan cấm thêm thư viện (mục 7, điều 4), nên phiên gọi hàm qua reflection. Rà soát độc lập chỉ ra R8 có thể xóa hàm ở bản minify và làm app dừng khi vào camera; phiên tra hàm lười và thêm luật keep trong `consumer-rules.pro`, kiểm bằng `mapping.txt` của `stagingRelease`.
- **Bằng chứng:** lỗi biên dịch `CameraXController.kt:114`; rà soát độc lập T-013 (điểm chặn 1); PR #26.
- **Đề xuất sửa:** một plan rủi ro cao riêng thêm `androidx.concurrent:concurrent-futures-ktx` (hoặc `com.google.guava:listenablefuture`) vào `gradle/libs.versions.toml` cho `:feature:camera`, rồi bỏ reflection. Plan dùng CameraX về sau (T-015 chỉnh zoom, lấy nét) kiểm trước những hàm trả `ListenableFuture`.
- **Áp dụng:**
- **Kiểm chứng:**

### L-008: Phiên mở bằng `claude --cloud` không có `resume-run.yml` tiếp tục, và `token-usage.py` cần thư mục gốc của checkout chính

- **Trạng thái:** proposed. **Loại:** quy trình. **Phạm vi:** chung. **Nguồn:** RUN-20261022-01.
- **Chuyện gì:** (1) Phiên này mở bằng `claude --cloud`, không qua routine. `resume-run.yml` kích hoạt lại routine khi phiên dừng vì hết hạn mức, nên không nối lại được phiên này; tiến trình của phiên còn bị khởi động lại một lần lúc 04:10 UTC giữa lúc chạy `check.sh` (lệnh bị dừng, mã 137), phiên tự chạy lại. (2) Nhánh báo cáo dùng một worktree riêng (`../report`) để không đổi nhánh dưới bản build của task; `scripts/token-usage.py` chạy trong worktree đó không thấy transcript (lọc theo thư mục gốc), phải đặt `TOKEN_USAGE_ROOT` về checkout chính.
- **Bằng chứng:** "Không đo được: không có transcript nào của repo này" khi chạy trong `../report`; chạy lại với `TOKEN_USAGE_ROOT=/home/user/camera-pose` ra bảng đầy đủ.
- **Đề xuất sửa:** `docs/auto-run.md`, mục 1 và mục 4: ghi cách tiếp tục khi phiên mở bằng `claude --cloud` (người mở lại phiên với cùng danh sách chạy, phiên đọc nhánh báo cáo như phiên tiếp tục); khuyến nghị worktree riêng cho nhánh báo cáo và ghi cách chạy `token-usage.py` từ đó. `scripts/token-usage.py` có thể tìm transcript theo tên repo thay vì đường dẫn tuyệt đối.
- **Áp dụng:**
- **Kiểm chứng:**

