---
id: FB-20261027-01
title: Máy chỉ có một camera thì nút đổi camera vẫn bấm được, bấm vào thì camera báo lỗi và lỗi còn sau khi mở lại app
reporter: AI
source: staging
build: nhánh thử của T-062 từ f464d7f (device-session.yml, lần chạy 40355405834, emulator `-camera-back emulated`, không có camera trước)
feature: camera
ticket: T-064
severity: nang
status: planned
created: 2026-10-27
---

# FB-20261027-01: MÁY CHỈ CÓ MỘT CAMERA THÌ NÚT ĐỔI CAMERA VẪN BẤM ĐƯỢC, BẤM VÀO THÌ CAMERA BÁO LỖI

## Điều đã xảy ra

Trong lần chạy thử của plan T-062 (emulator CI có camera sau giả, không có camera trước), monkey bấm nút đổi camera. App chuyển sang camera trước, camera không mở được và màn camera hiện "Couldn't open the camera". Log có 10 lần "Camera could not open", mỗi lần ngay sau khi camera sau đã mở, kèm "No available camera can be found".

Đọc code trên nhánh chính f464d7f (chưa chạy trên máy thật):

- Nút đổi camera chỉ mờ khi camera chưa sẵn sàng (`FlipCameraButton`, `enabled = ready != null`); app không hỏi máy có mấy camera.
- Bấm nút thì `CameraViewModel.onFlipClick` lưu lựa chọn camera trước vào DataStore rồi mở camera đó. Mở không được thì màn lỗi hiện.
- Lựa chọn đã lưu, nên mở lại app hay bấm "Try again" vẫn mở camera trước và vẫn lỗi. Người dùng không còn cách nào quay về camera sau, trừ xóa dữ liệu app.

## Các bước làm lại

1. Dùng máy hay emulator chỉ có camera sau (emulator: `-camera-back emulated -camera-front none`).
2. Mở camera, chờ camera sẵn sàng.
3. Bấm nút đổi camera.
4. Đóng app, mở lại.

## Mong đợi

Nút đổi camera mờ đi và không bấm được khi máy chỉ có một camera; camera luôn mở bằng camera máy có (KB-4, phần đổi đề xuất theo phản hồi này).

## Ảnh chụp, video

Không đính kèm. Log nằm trong artifact `device-session` của lần chạy 40355405834 (không lưu vào repo).

## Thiết bị

- Máy: emulator CI của `device-session.yml` (ảnh hệ thống như `pr-check.yml`), camera sau giả, không có camera trước
- Phiên bản Android: như emulator CI
- Ngôn ngữ, chế độ tối: tiếng Anh (trình chạy kịch bản đặt), mặc định

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

- **Loại:** đề xuất của AI: lỗi so với spec (KB-4 "Khi bấm nút đổi camera thì camera đổi giữa trước và sau" ngầm giả định máy có hai camera), cần thêm tiêu chí cho máy một camera.
- **Xử lý:** ticket T-064. Tuấn chọn ngày 2026-10-26 tạo ticket. Spec camera đổi KB-4 và thêm câu hỏi 17, chờ xác nhận.
- **Lý do:** máy tính bảng giá rẻ, Chromebook, một số máy chỉ có một camera; lỗi kẹt lại sau khi mở lại app.
- **Liên kết:** [T-064](../tickets/T-064-mot-camera.md), [plan T-064](../plans/T-064-mot-camera.md), plan T-062 mục 4.1 và câu hỏi 3
