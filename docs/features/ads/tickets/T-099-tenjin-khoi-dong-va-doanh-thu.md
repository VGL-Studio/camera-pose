---
id: T-099
title: Thêm SDK Tenjin vào :core:analytics, khởi động sau đồng ý khi build có khóa, gửi doanh thu từng lượt quảng cáo
feature: ads
spec: features/ads/spec.md   # câu 21, ý a, c (khóa doanh thu), d, e, f, g; mục 6
scenarios: [KB-T1]           # tiêu chí cài từ chiến dịch, doanh thu một lần, từ chối đồng ý, không khóa, Tenjin lỗi
owner: Nhật
status: planned
risk: high
size: M
depends_on: []
sprint:
plan: features/ads/plans/T-099-tenjin-khoi-dong-va-doanh-thu.md
auto_run:
created: 2026-11-17
---

# T-099: THÊM SDK TENJIN, KHỞI ĐỘNG SAU ĐỒNG Ý, GỬI DOANH THU QUẢNG CÁO

## Mô tả

Đưa SDK Tenjin vào `:core:analytics` theo ADR-1010. Bản build có khóa Tenjin khởi động Tenjin cùng lúc khung khởi động SDK số liệu sau đồng ý, để Tenjin ghi lượt cài và phiên, và gửi doanh thu của mỗi lượt quảng cáo AdMob có doanh thu qua hàm doanh thu quảng cáo của Tenjin. Người dùng không thấy gì mới.

## Phạm vi

- Thư viện Tenjin (và Install Referrer, App Set nếu chưa có) trong `gradle/libs.versions.toml`, `core/analytics/build.gradle.kts`.
- Interface nội bộ gọi SDK, bản cài đặt gọi SDK, bản giả cho test; lớp theo dõi Tenjin nhận `AdMetrics`, thêm vào `FanOutAdMetrics`.
- Khóa lúc build: `-PtenjinSdkKey` (production, tùy chọn), `tenjin.sdkKey` của `local.properties` (máy dev); không khóa thì không khởi động.
- Khởi động cùng chỗ khởi động AppsFlyer của khung; đọc đồng ý của UMP qua hàm của Tenjin trước khi kết nối.
- Khóa từ xa `is_log_tenjin_ad_revenue` (mặc định bật), sinh lại `docs/remote-config-keys.md`.
- `MergedManifestTest` giữ nguyên danh sách quyền; luật R8 nếu cần.

**Không bao gồm:**

- Sự kiện phễu và công tắc của chúng (T-100).
- Bước khóa Tenjin trong `scripts/release.sh`, Data safety, chính sách quyền riêng tư, báo UA (T-101, người làm).

## Rủi ro sơ bộ

Cao: thêm SDK mới (điểm chặn `sdk`), sửa `gradle/libs.versions.toml` (file được bảo vệ), sửa module core dùng chung, gửi dữ liệu ra dịch vụ ngoài. Plan cần chạy thử `scripts/check.sh` và `assembleStagingRelease`, so manifest gộp trước và sau, và duyệt chéo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của KB-T1 về cài đặt, doanh thu, đồng ý, không khóa và lỗi Tenjin đạt.
- [ ] Có test tự động cho cổng khóa, đồng ý, công tắc, doanh thu một lần và bỏ lượt 0.
- [ ] Manifest gộp không có quyền mới.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging (không gửi gì vì không có khóa; log ghi Tenjin tắt).
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
