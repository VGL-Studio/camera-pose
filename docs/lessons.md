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
