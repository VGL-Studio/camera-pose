---
id: FB-20261024-01
title: Màn mở gập bị đóng khung, thư viện chỉ 2 cột vì app khóa hướng dọc (Android 14, 15)
reporter: AI (phân tích device-session RUN-20261024-02), Tuấn
source: staging
build: 5443ecd (bản staging của RUN-20261024-02, chạy trên emulator CI)
feature: poses
ticket: T-024
severity: nang
status: new
created: 2026-10-24
---

# FB-20261024-01: MÀN MỞ GẬP BỊ ĐÓNG KHUNG, THƯ VIỆN CHỈ 2 CỘT VÌ APP KHÓA HƯỚNG DỌC

## Điều đã xảy ra

Phát hiện khi `device-session.yml` chạy kịch bản T-024 trên emulator CI (API 34, lần chạy 40116129967).
- Kịch bản đổi màn hình sang kích thước mở gập bằng `wm size 1766x2208` và `wm density 420`, tức 673 × 841 dp, cùng khung `_unfolded` của thiết kế.
- App không dùng cả màn hình. Android đặt app vào một khung ở giữa: `[246..1521]` px, rộng khoảng 486 dp, hai bên là nền đen, có thanh tác vụ ở đáy.
- Thư viện hiện 2 cột như trên điện thoại, không phải 3 cột như spec yêu cầu.

Nguyên nhân dự đoán: `app/src/main/AndroidManifest.xml` khai báo `android:screenOrientation="portrait"` cho `MainActivity`.
- Từ Android 12L đến Android 15, khi màn lớn có tỉ lệ không hợp với hướng bị khóa, hệ thống đóng khung app khóa hướng (letterbox).
- Từ Android 16 (API 36, `targetSdk` hiện tại), hệ thống bỏ qua khóa hướng trên màn từ 600 dp trở lên.

Vì vậy máy gập chạy Android 14, 15 có thể gặp đúng cảnh trên emulator, còn máy chạy Android 16 thì không. Một số hãng tự bỏ khóa hướng trên màn trong của máy gập; chưa kiểm trên máy thật.

Test giao diện trên JVM (Robolectric, khung `w673dp-h841dp-xxhdpi`) không thấy lỗi này, vì chúng dựng màn trực tiếp ở kích thước đó, không qua cơ chế đóng khung của hệ thống.

## Các bước làm lại

1. Emulator API 34 (hoặc máy gập Android 14, 15), cài bản staging.
2. `adb shell wm size 1766x2208` và `adb shell wm density 420` (trên máy gập thật: mở màn trong).
3. Mở app, vào thư viện tư thế.

## Mong đợi

KB-7 của `features/poses/spec.md`: khi máy gập mở ra, thư viện hiện 3 cột; chi tiết chia hai cột, đúng thiết kế `_unfolded`.

## Ảnh chụp, video

- `FB-20261024-01-1.png`: `fail.png` của kịch bản T-024, lần chạy 40116129967, đã thu nhỏ. App nằm trong khung giữa, lưới 2 cột, nền đen hai bên.

## Thiết bị

- Máy: emulator CI `google_apis` x86_64, không GPU
- Phiên bản Android: 14 (API 34)
- Ngôn ngữ, chế độ tối: tiếng Việt, sáng

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

- **Loại:** chưa phân loại. Đề xuất của AI: lỗi so với spec (KB-7) trên Android 14, 15. Cần kiểm trên một máy gập thật chạy Android 14 hay 15 trước khi kết luận.
- **Xử lý:** chủ feature quyết. Có hai hướng, đều cần người phụ trách sản phẩm xác nhận:
  - bỏ khóa hướng dọc trên màn lớn;
  - chấp nhận đóng khung trên Android 14, 15 và ghi rõ trong spec.

  Kịch bản emulator T-024 hiện không kiểm số cột (plan T-048).
- **Lý do:**
- **Liên kết:** ticket T-024 (bố cục máy gập), plan T-048 (mục 8, câu 2)
