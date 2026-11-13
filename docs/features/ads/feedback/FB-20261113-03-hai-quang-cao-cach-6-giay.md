---
id: FB-20261113-03
title: Ra rồi vào lại app ngay sau quảng cáo mở app thì gặp quảng cáo thứ hai sau 6 giây
reporter: AI
source: staging
build: 0.1.0-staging.24
feature: ads
ticket: T-074
severity: gop-y
status: wontfix
created: 2026-11-13
---

# FB-20261113-03: RA RỒI VÀO LẠI APP NGAY SAU QUẢNG CÁO MỞ APP THÌ GẶP QUẢNG CÁO THỨ HAI SAU 6 GIÂY

Đề xuất ở mục 4 của phiên thử [TS-20261113-01](../../../test-sessions/TS-20261113-01.md).

## Điều đã xảy ra

Người thử mở app, xem quảng cáo mở app, vào camera, về màn hình chính 2 giây rồi mở lại app. Quảng cáo resume (dạng inter) hiện ngay, 6 giây sau khi quảng cáo mở app đóng, và phủ lên camera:

```text
08:50:16.291 I VGL.Ads  : App open shown: open_app
08:50:18.152 I VGL.Ads  : Full-screen turn finished: closed
08:50:21.941 I VGL.Ads  : App in background
08:50:23.856 I VGL.Ads  : App in foreground
08:50:24.132 I VGL.Ads  : Interstitial shown: resume_inter
```

Việc này đúng luật hiện tại: A-11 của `docs/ops/ads-business-rules.md` không có thời gian tối thiểu ở nền và không áp `ads_interval` cho resume, còn A-12 cho resume dạng inter bỏ qua cổng giãn cách. Spec ads (câu 6) chốt resume hiện ở mọi màn, kể cả camera.

## Các bước làm lại

1. Bật quảng cáo mở app và `show_resum_ads`, đặt `resume_ad_type` là `inter`.
2. Mở app (không phải lần mở đầu), chờ quảng cáo mở app, đóng nó.
3. Về màn hình chính, mở lại app trong vòng vài giây.

## Mong đợi

Góp ý, không phải lỗi so với spec. Đề xuất để chủ feature ads cân nhắc một khoảng cách tối thiểu giữa quảng cáo mở app (hay quảng cáo toàn màn hình bất kỳ) và resume, hoặc một thời gian tối thiểu ở nền trước khi hiện resume. Đây là luật của khung (bộ PARTNER, ADR-0045), nên nếu làm thì đề xuất về dự án mẫu (`docs/ops/from-app.md`).

## Ảnh chụp, video

Không có.

## Thiết bị

- Máy: Xiaomi 23021RAAEG
- Phiên bản Android: 15 (API 35)
- Ngôn ngữ, chế độ tối: không ghi trong phiên

## Phân loại

- **Loại:** góp ý mới. Hành vi đúng luật hiện tại (A-11, A-12 của `docs/ops/ads-business-rules.md`) và câu 6 của spec ads.
- **Xử lý:** không xử lý. Nhật chọn ngày 2026-11-13.
- **Lý do:** giữ luật resume hiện tại của khung; không đề xuất về dự án mẫu.
- **Liên kết:** không có.
