---
id: T-014
title: Camera: chọn tư thế, nét viền, thẻ bước
feature: camera
spec: features/camera/spec.md
scenarios: [KB-1, KB-2]
owner: Nhật
status: planned
risk: high
size: L
depends_on: [T-009, T-013]
sprint:
plan: features/camera/plans/T-014-camera-chon-tu-the.md
created: 2026-10-21
---

# T-014: CAMERA: CHỌN TƯ THẾ, NÉT VIỀN, THẺ BƯỚC

## Mô tả

Trên camera có hàng chip nhóm, dải tư thế, nét viền trắng có quầng tối và thẻ bước (ẩn hiện được). Mở lại app vẫn là tư thế dùng lần trước.

## Phạm vi

- Thành phần mới: `StickerChip`, `SelectedBadge`, `PoseThumb`, `StepCard`.
- Thứ tự dải: đã lưu, gần đây, còn lại; ghi "đã dùng" khi chọn tư thế.
- Thẻ bước: chuyển bước, vuốt lên để ẩn, nút "Bước" để hiện lại, giữ trạng thái ẩn.
- Trạng thái không có tư thế; ô tải ảnh lỗi chạm để thử lại.
- Sự kiện `pose_selected`, `content_load_failed`; mở camera với tư thế chỉ định (cho T-022).

**Không bao gồm:**

- Chỉnh nét viền (T-015).

## Rủi ro sơ bộ

Điểm chặn 4 (sự kiện phân tích) và 7 (thành phần mới trong core): rủi ro cao.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
