---
id: FB-20261026-04
title: Đang đếm ngược hẹn giờ mà các nút trên camera vẫn bấm được
reporter: Tuấn
source: staging
build: bản staging trên máy thật của Tuấn (chưa ghi số phiên bản)
feature: camera
ticket: T-061
severity: nang
status: triaged
created: 2026-10-26
---

# FB-20261026-04: ĐANG ĐẾM NGƯỢC HẸN GIỜ MÀ CÁC NÚT TRÊN CAMERA VẪN BẤM ĐƯỢC

## Điều đã xảy ra

Khi bật hẹn giờ và bấm chụp, trong lúc đếm ngược các tính năng trên màn hình vẫn thao tác được. Tuấn chưa rõ đây là chủ ý thiết kế hay lỗi, và đề nghị khóa các tính năng khi đồng hồ đang chạy.

## Các bước làm lại

1. Mở camera, bật hẹn giờ 3 giây hoặc 10 giây.
2. Bấm nút chụp.
3. Trong lúc đếm, bấm flash, lưới, hẹn giờ, đổi tư thế, kéo nét viền, mở bảng chỉnh.

## Mong đợi

Trong lúc đếm, chỉ còn các thao tác hợp lý: hủy đếm, đổi camera, zoom; các nút khác khóa.

## Ảnh chụp, video

Không có.

## Thiết bị

- Máy: điện thoại của Tuấn (chưa ghi dòng máy)
- Phiên bản Android: chưa ghi
- Ngôn ngữ, chế độ tối: chưa ghi

## Phân loại

- **Loại:** thay đổi yêu cầu. Spec KB-6 chỉ ghi "số đếm ngược hiện to giữa khung xem trước, nút chụp đổi thành nút hủy"; không nói gì về các nút khác.
  - Code: lớp mờ khi đếm là một `Box.background` không nhận chạm (`CameraControls.kt:234-241`), nên mọi chạm đi xuống các nút bên dưới. Chỉ đổi camera (`CameraViewModel.kt:159-170`), rời màn (ON_STOP) và hộp thoại mất mạng làm dừng đếm.
- **Xử lý:** sửa ở phiên tự chạy kế tiếp (T-061). Tuấn chọn ngày 2026-10-26, như các app camera phổ biến:
  - Khóa (mờ đi): flash, lưới, hẹn giờ ở thanh trên, hàng nhóm, dải tư thế không phản hồi; cột công cụ nét viền và mũi tên thẻ bước mờ đi; bảng chỉnh đang mở thì đóng; kéo và chụm nét viền không đổi nét viền.
  - Hủy đếm: chạm bất kỳ chỗ nào trong khung xem trước (trừ nút zoom), nút hủy, đổi camera, rời màn (thư viện, cài đặt, ô ảnh vừa chụp).
  - Vẫn dùng được: zoom.
  - Spec camera KB-6 và thiết kế `CameraScreen_countdown_dark` đổi theo.
- **Liên kết:** [T-061](../tickets/T-061-khoa-nut-khi-dem-nguoc.md)
