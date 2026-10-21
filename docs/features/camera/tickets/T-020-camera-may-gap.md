---
id: T-020
title: Camera và màn xem lại trên máy gập
feature: camera
spec: features/camera/spec.md
scenarios: [KB-11]
owner: Nhật
status: ready
risk: medium
size: M
depends_on: [T-014, T-016]
sprint:
plan:
created: 2026-10-21
---

# T-020: CAMERA VÀ MÀN XEM LẠI TRÊN MÁY GẬP

## Mô tả

Khi máy gập mở ra, camera giữ khung 3:4 bên trái và dời nút chụp, đổi camera, zoom sang cột phải; màn xem lại đặt ảnh bên trái, nút bên phải.

## Phạm vi

- Bố cục theo `CameraScreen_default_dark_unfolded`, `ReviewScreen_default_dark_unfolded`; giữ trạng thái khi gập mở.
- Screenshot test khung `w673dp-h841dp-xxhdpi`.

**Không bao gồm:**

- Không có.

## Rủi ro sơ bộ

Chỉ trong `:feature:camera`; kiểm tra trên máy gập thật hoặc emulator gập.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
