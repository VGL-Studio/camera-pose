---
id: FB-20261024-01
title: Màn mở gập bị đóng khung, thư viện chỉ 2 cột vì app khóa hướng dọc (Android 14, 15)
reporter: AI (phân tích device-session RUN-20261024-02), Tuấn
source: staging
build: 5443ecd (bản staging của RUN-20261024-02, chạy trên emulator CI)
feature: poses
ticket: T-024
severity: nang
status: planned
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

- **Loại:** lỗi so với spec (poses KB-7) trên Android 12L đến 15 khi màn lớn nằm ngang. Bằng chứng trên emulator CI của phản hồi này là do kích thước bị cắt (FB-20261025-01, T-049); trên máy thật, màn trong nằm ngang của máy gập và máy tính bảng vẫn bị đóng khung khi app khóa dọc (tài liệu Android về chế độ tương thích).
- **Xử lý:** Tuấn chọn sửa ở T-056 (2026-10-26): điện thoại vẫn khóa dọc, màn có cạnh ngắn từ 600 dp theo hướng của máy. Spec camera đổi theo (mục 2, KB-11, câu 7, câu 15), chờ người phụ trách sản phẩm xác nhận.
- **Lý do:** Android 16 đã bỏ qua khóa dọc trên màn từ 600 dp, nên app vốn phải chạy được ở màn lớn nằm ngang; T-056 cho Android 12L đến 15 cùng hành vi đó.
- **Liên kết:** ticket T-024 (bố cục máy gập), plan T-048 (mục 8, câu 2), ticket và plan T-056
