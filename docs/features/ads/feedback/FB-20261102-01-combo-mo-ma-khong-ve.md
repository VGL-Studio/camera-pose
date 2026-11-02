---
id: FB-20261102-01
title: Firebase staging bật is_show_native_after_inter; lượt combo native mở mà không vẽ, không đóng được
reporter: AI (phân tích TS-20261102-01)
source: staging
build: 0.1.0-staging.22
feature: ads
ticket: T-090
severity: nang
status: new
created: 2026-11-02
---

# FB-20261102-01: LƯỢT COMBO NATIVE MỞ MÀ KHÔNG VẼ, KHÔNG ĐÓNG ĐƯỢC

## Điều đã xảy ra

Phiên thử máy thật [TS-20261102-01](../../../test-sessions/TS-20261102-01.md), P-1. Lúc 08:40:38, sau quảng cáo khi quay lại app dạng interstitial (`resume_inter`), log ghi `Native combo opened (inter gone: true)` nhưng màn hình vẫn là camera; người thử chạm nút thư viện 0,6 giây sau. Không có dòng `Native combo closed` tới cuối phiên, và chặn quảng cáo quay lại app (`Resume ads held: full_screen_page`) không được gỡ.

Nguyên nhân: Remote Config của project Firebase staging bật `is_show_native_after_inter`, trong khi `:app` chưa đặt `FullScreenAdHost` (quyết định của plan T-074 câu 7, ADR-1007, khi combo còn tắt). App đúng spec khi khóa tắt.

## Các bước làm lại

1. Firebase staging: `is_show_native_after_inter = true` (hiện trạng).
2. Mở bản staging 22, ở camera, ra màn hình chính rồi quay lại app tới khi có interstitial `resume_inter`.
3. Đóng interstitial.

## Mong đợi

Theo spec ads câu 18 (2026-11-02): combo hiện sau interstitial của màn, không hiện sau `resume_inter`; có lớp phủ đếm ngược và đóng được (K-01 tới K-08). Trước khi có T-090 thì khóa trên Firebase staging phải tắt (`manual-test.md`, mục 11).

## Ảnh chụp, video

Không có. Bằng chứng là log, trích trong báo cáo TS-20261102-01.

## Thiết bị

- Máy: Xiaomi, Android 15 (API 35)
- Phiên bản Android: 15
- Ngôn ngữ, chế độ tối: không ghi

## Phân loại

- **Loại:** đề xuất của AI: lỗi cấu hình staging, kèm thay đổi yêu cầu đã chốt (spec ads câu 18).
- **Xử lý:** đề xuất: tắt khóa trên Firebase staging ngay (việc của người); T-090 làm combo chạy đúng; đề xuất `from-app` về dự án mẫu: khung không mở lượt combo khi không có host nào vẽ.
- **Lý do:** chủ feature quyết.
- **Liên kết:** T-090
