---
id: FB-20261113-02
title: Quay lại app khi đang ở camera thì camera mở ngay dưới quảng cáo resume, không chờ quảng cáo đóng
reporter: AI
source: staging
build: 0.1.0-staging.24
feature: camera
ticket: T-097
severity: nhe
status: triaged
created: 2026-11-13
---

# FB-20261113-02: CAMERA MỞ NGAY DƯỚI QUẢNG CÁO KHI QUAY LẠI APP, KHÔNG CHỜ QUẢNG CÁO ĐÓNG

Phát hiện P-2 của phiên thử [TS-20261113-01](../../../test-sessions/TS-20261113-01.md).

## Điều đã xảy ra

Người thử rời app 2 giây khi đang ở camera rồi quay lại. Quảng cáo `resume_inter` hiện, và camera được mở ngay dưới quảng cáo, chạy suốt 6 giây quảng cáo hiện:

```text
08:50:23.973 I VGL.Ads  : Resume ad shown on resume: resume_inter
08:50:24.327 I CameraManager: Open camera top activityName is com.google.android.gms.ads.AdActivity
08:50:24.328 I VGL.Camera: Camera ready: back, flash true
08:50:30.399 I VGL.Ads  : Full-screen turn finished: closed
```

`AdActivity` của AdMob trong suốt, nên `MainActivity` chỉ dừng ở trạng thái PAUSED. Màn camera mở camera theo `LifecycleStartEffect` (`CameraScreen.kt:135-143`), nên camera không chờ quảng cáo đóng. Có thể chấm báo camera đang dùng của Android hiện trên quảng cáo; điều này chưa kiểm bằng mắt.

## Các bước làm lại

1. Bật `show_resum_ads`, đặt `resume_ad_type` là `inter`.
2. Mở app tới camera, về màn hình chính, mở lại app từ biểu tượng.
3. Khi quảng cáo hiện, xem có chấm báo camera ở góc màn hình không, và xem log `Open camera top activityName is ...AdActivity`.

## Mong đợi

Theo mục 5 của `features/camera/spec.md`: "Đóng quảng cáo thì camera mở lại như khi quay lại app thường", tức camera mở sau khi quảng cáo đóng, giống hai interstitial trong app (`inter_pose_detail`, `review_interstitial`). Nếu chủ feature thấy mở camera sớm là chấp nhận được (camera sẵn sàng ngay khi quảng cáo đóng), thì sửa câu chữ của spec thay vì sửa code.

## Ảnh chụp, video

Không có.

## Thiết bị

- Máy: Xiaomi 23021RAAEG
- Phiên bản Android: 15 (API 35)
- Ngôn ngữ, chế độ tối: không ghi trong phiên

## Phân loại

- **Loại:** lỗi so với spec. Mục 5 của `features/camera/spec.md`: "Đóng quảng cáo thì camera mở lại như khi quay lại app thường"; camera hiện mở ngay dưới quảng cáo.
- **Xử lý:** sửa code, giữ câu chữ của spec: camera chờ quảng cáo toàn màn hình đóng rồi mới mở. Ticket mới T-097. Nhật chọn ngày 2026-11-13.
- **Lý do:** mở camera dưới quảng cáo có thể làm chấm báo camera của Android hiện trên quảng cáo, và dùng camera khi người dùng không thấy khung xem trước.
- **Liên kết:** [T-097](../tickets/T-097-camera-cho-quang-cao-dong.md)
