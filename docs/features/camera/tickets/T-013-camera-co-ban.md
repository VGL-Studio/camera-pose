---
id: T-013
title: Camera: khung xem trước, quyền, công cụ chụp, chụp ảnh
feature: camera
spec: features/camera/spec.md
scenarios: [KB-4, KB-6, KB-7, KB-8]
owner: Nhật
status: approved
risk: high
size: L
depends_on: [T-007, T-010]
sprint:
plan: features/camera/plans/T-013-camera-co-ban.md
created: 2026-10-21
---

# T-013: CAMERA: KHUNG XEM TRƯỚC, QUYỀN, CÔNG CỤ CHỤP, CHỤP ẢNH

## Mô tả

Tạo `:feature:camera` với camera chạy được: khung xem trước 3:4 bằng CameraX, xin quyền và trạng thái chưa có quyền, trạng thái đang mở và lỗi, công cụ flash, hẹn giờ, lưới, đổi camera, zoom, nút chụp và đếm ngược. Chụp xong giữ ảnh để màn xem lại dùng (T-016).

## Phạm vi

- Bố cục camera theo `features/camera/design/README.md` (phần không có tư thế), khóa chiều dọc.
- Thành phần mới: `CameraIconButton`, `GridOverlay`, `ZoomControl`, `StateMessage`.
- Giữ hẹn giờ, lưới, flash, camera trước sau giữa các lần dùng; âm chụp theo Cài đặt (mặc định bật khi chưa có màn cài đặt).
- Sự kiện `camera_permission_result` (`source`: `camera`), `photo_captured` (`pose_id` để trống khi chưa có tư thế).
- Cờ `feature_camera_enabled`; camera là đích sau loading khi cờ bật.
- Dừng đếm ngược khi có hộp thoại mất mạng (nếu T-011 đã xong).

**Không bao gồm:**

- Tư thế, nét viền, thẻ bước (T-014); chỉnh nét viền (T-015); màn xem lại (T-016); điểm khớp (T-017); máy gập (T-020).

## Rủi ro sơ bộ

Điểm chặn 4 (sự kiện phân tích) và 7 (thành phần mới trong `:core:designsystem`): rủi ro cao. CameraX và quyền `CAMERA` do dev thêm trước.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
