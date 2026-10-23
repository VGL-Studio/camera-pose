---
id: T-015
title: Camera: chỉnh nét viền
feature: camera
spec: features/camera/spec.md
scenarios: [KB-3]
owner: Nhật
status: ready
risk: high
size: M
depends_on: [T-014]
sprint:
plan: features/camera/plans/T-015-camera-chinh-net-vien.md
created: 2026-10-21
---

# T-015: CAMERA: CHỈNH NÉT VIỀN

## Mô tả

Người dùng kéo, chụm, chỉnh độ mờ và cỡ, lật, khóa, ẩn, đặt lại nét viền; lựa chọn giữ khi đổi tư thế và giữa các lần dùng.

## Phạm vi

- Thành phần mới: `AdjustPanel` (dùng `Slider` của Material 3).
- Trạng thái khóa: kéo và chụm không đổi, nút mờ đi, lời nhắc "Đã khóa lớp phủ".
- Nét viền luôn còn một phần trong khung.
- Sự kiện `overlay_adjusted`, ghi một lần mỗi thao tác.

**Không bao gồm:**

- Điểm khớp (T-017).

## Rủi ro sơ bộ

Điểm chặn 4 (sự kiện phân tích) và 7 (thành phần mới trong core): rủi ro cao.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
