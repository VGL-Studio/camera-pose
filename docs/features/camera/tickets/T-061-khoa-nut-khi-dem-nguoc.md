---
id: T-061
title: Khóa nút và dừng đếm khi đang đếm ngược
feature: camera
spec: features/camera/spec.md
scenarios: [KB-6]
owner: Nhật
status: planned
risk: high
size: M
depends_on: [T-013]
sprint:
plan: features/camera/plans/T-061-khoa-nut-khi-dem-nguoc.md
created: 2026-10-26
---

# T-061: KHÓA NÚT VÀ DỪNG ĐẾM KHI ĐANG ĐẾM NGƯỢC

## Mô tả

Khi đang đếm ngược hẹn giờ, mọi nút trên camera vẫn bấm được và đổi được ảnh sắp chụp (FB-20261026-04). Sau task này, đúng spec KB-6 mới: flash, lưới, hẹn giờ, hàng nhóm, dải tư thế mờ đi và không phản hồi; cột công cụ nét viền và mũi tên thẻ bước mờ đi; bảng chỉnh đang mở thì đóng; chạm khung xem trước (ngoài nút zoom), đổi camera hay rời màn thì dừng đếm; zoom vẫn dùng được.

## Phạm vi

- `CameraScreen` và `CameraUnfolded` nhận trạng thái đang đếm, làm mờ và khóa các nút, lớp mờ của khung xem trước nhận chạm để dừng đếm.
- `CameraViewModel`: dừng đếm khi rời màn bằng nút (ghi log), đóng bảng chỉnh khi bắt đầu đếm.
- Test giao diện và unit test cho từng nút trong bảng của spec; ghi lại ảnh chuẩn `CameraScreen_countdown_*` theo thiết kế mới.

**Không bao gồm:** đổi số giây hay âm đếm.

## Rủi ro sơ bộ

Trung bình: đổi hành vi của nhiều nút trên màn chính; cần thiết kế `CameraScreen_countdown_dark` mới trước phiên (Figma). Không thêm quyền, thư viện hay sự kiện phân tích.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
