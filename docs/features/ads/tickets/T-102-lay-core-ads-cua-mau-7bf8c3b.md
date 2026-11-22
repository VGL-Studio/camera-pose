---
id: T-102
title: Lấy core/ads và hai tài liệu ads của dự án mẫu lên mốc 7bf8c3b (thẻ native hai tầng, luật thanh đáy mới)
feature: ads
spec: features/ads/spec.md   # câu 22, ý a tới g (Nhật trả lời và xác nhận 2026-11-21)
scenarios: []                # spec ads không có kịch bản riêng (mục 3); ticket đổi KB-6 của spec poses (tiêu chí 2, 3, lớp nổi) và thanh đáy của KB-1 spec settings, KB-9 và KB-11 spec camera
owner: Nhật
status: approved
risk: high
size: L
depends_on: []
sprint:
plan: features/ads/plans/T-102-lay-core-ads-cua-mau-7bf8c3b.md
auto_run:
created: 2026-11-21
---

# T-102: LẤY CORE/ADS VÀ HAI TÀI LIỆU ADS CỦA DỰ ÁN MẪU LÊN MỐC 7BF8C3B

## Mô tả

Đợt đồng bộ dự án mẫu `bc3e80e..7bf8c3b` (PR #372) giữ `core/ads`, `docs/ops/ads-guide.md` và `docs/ops/ads-business-rules.md` ở mốc `bc3e80e`. Ticket này đưa ba đường dẫn lên mốc `7bf8c3b` (`docs/ops/to-app.md`, mục 5).

Sau khi xong:
- thẻ native ở thanh đáy của bốn màn vẽ theo thẻ hai tầng của khung, cao 106 dp ở cỡ chữ mặc định ở mọi khung;
- nội dung chừa đúng chỗ khe đang chiếm, kể cả lúc tải;
- khe nhường chỗ khi lớp thu gọn mở, mỗi màn tối đa một quảng cáo neo đáy;
- thư viện không có lớp nổi khi ô lưới bật, chi tiết tư thế không bao giờ có lớp nổi.

## Điều kiện trước plan

- Câu 22 của spec ads đã trả lời, spec ads và spec poses đã chốt: xong 2026-11-21 (PR #374).
- Thiết kế trạng thái `ad` đã vẽ lại theo thẻ native hai tầng, gói bàn giao đã lập lại: xong 2026-11-21 (PR #374, Nhật duyệt).
- Nhánh điểm bắt đầu đã đẩy: `template-sync/take-ads-7bf8c3b` (2026-11-21).

## Phạm vi

- **Điểm bắt đầu:** nhánh `template-sync/take-ads-7bf8c3b`. Nhánh dựng bằng `scripts/template-sync.sh --take` cho ba đường dẫn. Đã giải xung đột, giữ hai phần riêng của app: dải phân cách banner trong `AdSlot` và chiều cao ô lưới khi khe trống trong `NativeAdSlot`. Test mới của mẫu đổi `SAMPLE_NATIVE_SMALL` sang `POSES_BOTTOM`, bảng khóa Remote Config sinh lại. Trên nhánh: `:core:ads` qua 2084 test.
- **Phần còn đỏ trên nhánh**, phiên làm cho qua `scripts/check.sh`:
  - 13 test đo kích thước ghim số của plan T-098: camera 9 (`ReviewScreenTest`, `ReviewScreenUnfoldedTest`), poses 4 (`PosesScreenTest`, `PoseDetailScreenTest`). Viết lại theo ADR-0031 với số của câu 22.
  - 12 ảnh chuẩn `_ad` của poses, chi tiết, cài đặt, xem lại: ghi lại theo thiết kế đã duyệt.
- **Thư viện bọc `InContentAds`** với `poses_in_collection` (MC-13, câu 22, ý e). Có test: ô lưới bật thì `poses_bottom` không có lớp nổi; ô lưới tắt thì có lớp nổi như thường.
- **`pose_detail_bottom` khai báo `collapsibleLayer = false`** (câu 22, ý g). Có test: khóa `is_show_native_collap` bật thì chi tiết vẫn không có lớp nổi.
- **Tài liệu:**
  - `docs/ops/manual-test.md`, mục 11.1: bước 126 dp đổi theo khe 106 dp; các bước lớp nổi chuyển sang màn Cài đặt, hay tắt `is_show_poses_in_collection` khi kiểm ở thư viện.
  - `docs/template-sync.txt`: đã sửa trên nhánh, kiểm lại.

**Không bao gồm:**

- Dùng `rememberRewardedAdRequired` cho dấu khóa của tư thế premium (câu 22, ý f): ticket riêng nếu cần.
- Bật MAX.

## Rủi ro sơ bộ

Chạm điểm chặn quảng cáo và module core (`risk-scoring.md`, mục 1): thay gần hết `:core:ads`, đổi khai báo vị trí và ảnh chuẩn. Rủi ro cao, cần duyệt chéo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí của câu 22 và KB-6 của spec poses đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
