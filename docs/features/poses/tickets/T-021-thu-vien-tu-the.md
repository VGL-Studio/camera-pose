---
id: T-021
title: Thư viện tư thế: lưới, nhóm, đã lưu, tải và lỗi
feature: poses
spec: features/poses/spec.md
scenarios: [KB-1, KB-2, KB-3, KB-5]
owner: Nhật
status: on-staging
risk: high
size: L
depends_on: [T-009, T-007, T-014]
sprint:
plan: features/poses/plans/T-021-thu-vien-tu-the.md
created: 2026-10-21
---

# T-021: THƯ VIỆN TƯ THẾ: LƯỚI, NHÓM, ĐÃ LƯU, TẢI VÀ LỖI

## Mô tả

Tạo `:feature:poses` với màn thư viện: dòng tổng số, chip nhóm và "Đã lưu", lưới thẻ tư thế 2 cột, lưu và bỏ lưu, khung xương khi tải, lỗi có "Thử lại". Nút thư viện ở camera mở màn này.

## Phạm vi

- Thành phần mới: `PoseCard`, `PoseCardSkeleton` (dùng `StickerChip`, `SelectedBadge`, `StateMessage` từ T-013, T-014 nếu đã có, nếu chưa thì thêm ở đây).
- Thứ tự: đã lưu, gần đây, còn lại; giữ chip và vị trí cuộn khi quay lại.
- Trạng thái đã lưu rỗng với "Xem tất cả tư thế".
- Sự kiện `pose_saved`, `content_load_failed`; cờ `feature_poses_enabled`; nút cài đặt ở thanh trên (khi có màn cài đặt).

**Không bao gồm:**

- Chi tiết tư thế (T-022), banner (T-023), máy gập (T-024).

## Rủi ro sơ bộ

Điểm chặn 4 (sự kiện phân tích) và 7 (thành phần mới trong core): rủi ro cao.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
