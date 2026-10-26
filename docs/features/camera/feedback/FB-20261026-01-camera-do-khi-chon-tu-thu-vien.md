---
id: FB-20261026-01
title: Mở camera từ "Chụp với tư thế này" ở thư viện thì khung xem trước đứng hình và không chụp được
reporter: Tuấn
source: staging
build: bản staging trên máy thật của Tuấn (chưa ghi số phiên bản)
feature: camera
ticket: T-058
severity: chan
status: fixed
created: 2026-10-26
---

# FB-20261026-01: MỞ CAMERA TỪ "CHỤP VỚI TƯ THẾ NÀY" Ở THƯ VIỆN THÌ KHUNG XEM TRƯỚC ĐỨNG HÌNH VÀ KHÔNG CHỤP ĐƯỢC

## Điều đã xảy ra

Chọn một tư thế trong thư viện, bấm "Chụp với tư thế này": camera mở nhưng đứng hình, bấm nút chụp không có gì xảy ra. Chọn tư thế ngay trên màn camera (dải tư thế) thì chụp bình thường.

## Các bước làm lại

1. Mở app, vào thư viện tư thế, chạm một tư thế.
2. Ở màn chi tiết, bấm "Chụp với tư thế này".
3. Chờ camera hiện, bấm nút chụp.

## Mong đợi

Camera chạy và chụp được như khi chọn tư thế ở dải (spec camera KB-1: "Khi mở camera từ nút "Chụp với tư thế này" ở chi tiết tư thế thì camera dùng đúng tư thế đó"; KB-6).

## Ảnh chụp, video

Không có.

## Thiết bị

- Máy: điện thoại của Tuấn (chưa ghi dòng máy)
- Phiên bản Android: chưa ghi
- Ngôn ngữ, chế độ tối: chưa ghi

## Phân loại

- **Loại:** lỗi so với spec.
- **Xử lý:** sửa ở phiên tự chạy kế tiếp, chạy đầu tiên (T-058).
- **Lý do:** AI đọc code trên nhánh chính 0939acf, chưa chạy trên máy:
  - "Chụp với tư thế này" bỏ màn camera cũ khỏi ngăn xếp và mở một màn camera mới (`app/.../navigation/PoseDetailEntry.kt:14`). Màn mới có `CameraViewModel` và `CameraXController` riêng (`@ViewModelScoped`) và gắn camera xong.
  - Màn cũ chỉ bị hủy khi hiệu ứng chuyển màn kết thúc, thường là sau đó. `CameraXController.release()` gọi `close()`, và `close()` gọi `provider.unbindAll()` (`CameraXController.kt:212`). `ProcessCameraProvider` dùng chung cả tiến trình, nên lệnh này gỡ luôn khung xem trước, chụp ảnh và phân tích của màn mới. Khung đứng hình; `takePicture` lỗi và chỉ ghi log "Capture failed".
  - Cùng lúc, `poseAnalyzer.release()` của màn cũ đóng bộ nhận dạng dáng (`@Singleton`) mà màn mới đang dùng, và `PoseMatchViewModel.onCleared()` của màn cũ tắt luồng khung hình của màn mới.
  - Chọn ở dải chỉ đổi tư thế đang dùng, không tạo màn mới, nên không gặp.
  - Log xác nhận được trên máy (`adb logcat -v threadtime | grep ' VGL\.'`): "Camera ready" của màn mới, rồi "Camera unbound" mà không có "Camera closed" trước đó; bấm chụp thì "Capture failed".
  - Chưa có test nào bấm chụp sau khi đi từ thư viện: `PoseDetailEntryTest` chỉ kiểm ngăn xếp, kịch bản `T-022-chi-tiet-sang-camera.txt` dừng khi `camera_screen` hiện.
- **Kết quả sau khi sửa:** đã sửa ở T-058 (PR #170), có trong bản 0.1.0-staging.12 (RUN-20261026-03). Tuấn thử trên máy thật theo mục Cần người kiểm tra của báo cáo phiên: đạt. Tuấn chuyển `fixed` ngày 2026-10-26.
- **Liên kết:** [T-058](../tickets/T-058-camera-mo-tu-thu-vien.md), [plan T-058](../plans/T-058-camera-mo-tu-thu-vien.md)
