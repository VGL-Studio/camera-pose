---
id: T-023
title: Banner ở thư viện và chi tiết tư thế
feature: poses
spec: features/poses/spec.md
scenarios: [KB-6]
owner: Nhật
status: ready
risk: high
size: S
depends_on: [T-021, T-022]
sprint:
plan:
created: 2026-10-21
---

# T-023: BANNER Ở THƯ VIỆN VÀ CHI TIẾT TƯ THẾ

## Mô tả

Thêm hai vị trí banner `poses_banner` (neo đáy thư viện) và `poses_detail_banner` (đáy chi tiết, dưới nút chính) qua `AdSlot`, mặc định tắt.

## Phạm vi

- Vị trí trong `AdPlacement`, cờ `ads_poses_banner_enabled`, `ads_poses_detail_banner_enabled`.
- Nội dung không bị che; cờ tắt thì không chừa khoảng trống.
- Screenshot test các ảnh `_ad_`.

**Không bao gồm:**

- Không có.

## Rủi ro sơ bộ

Điểm chặn 1 (vị trí quảng cáo mới): rủi ro cao; người phụ trách sản phẩm biết trước khi duyệt.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
