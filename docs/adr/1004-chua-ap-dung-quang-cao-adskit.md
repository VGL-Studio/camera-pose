---
id: ADR-1004
title: App chưa áp dụng quảng cáo theo AdsKit và AppsFlyer cho tới đợt đồng bộ core, trong lúc đó giữ quy tắc quảng cáo cũ
status: accepted             # proposed | accepted | superseded
origin: app                  # template: ADR của dự án mẫu (mã dưới 1000) | app: ADR riêng của app (mã từ 1001)
date: 2026-10-27
deciders: [Tuấn]
supersedes:                  # tạm không áp dụng ADR-0026, ADR-0027 và các vế đổi của ADR-0004, ADR-0013, ADR-0014, ADR-0023; xem mục Quyết định
superseded_by:
---

# ADR-1004: APP CHƯA ÁP DỤNG QUẢNG CÁO THEO ADSKIT VÀ APPSFLYER CHO TỚI ĐỢT ĐỒNG BỘ CORE

## Bối cảnh

Đồng bộ dự án mẫu đợt 1 (PR #179, ADR-0030 của dự án mẫu) chỉ lấy phần quy trình: `scripts/`, `.github/`, `docs/`. Các thư mục code (`app`, `core`, `feature`, cấu hình build) giữ ở mốc cũ (`hold=` trong `docs/template-sync.txt`), `scripts/release.sh` và `docs/ops/release.md` cũng vậy.

Đợt này mang theo ADR-0026 (quảng cáo theo bộ PARTNER của AdsKit), ADR-0027 (AppsFlyer) và ghi chú sửa ADR-0004, ADR-0013, ADR-0014, ADR-0023. Quy tắc mới đòi khóa quảng cáo theo tên AdsKit, giá trị mặc định theo `docs/ops/ads-business-rules.md`, AppsFlyer trong `:core:analytics`, bảng khóa Remote Config sinh từ `AppRemoteKeys`. Code của app chưa có phần nào trong số đó: `:core:ads` vẫn dùng khóa `ads_<vị trí>_enabled`, app chưa có AppsFlyer hay `AppRemoteKeys`.

Nếu để nguyên, `docs/foundation.md` có hai bộ quy tắc trái nhau về quảng cáo. Plan nào chạm quảng cáo hay khóa Remote Config sẽ gặp mâu thuẫn tài liệu và phải dừng, hoặc phiên làm theo quy tắc mà code chưa đỡ được.

## Quyết định

- App chưa áp dụng ADR-0026, ADR-0027, và các vế mà ghi chú ngày 2026-10-23 đổi trong ADR-0004, ADR-0013, ADR-0014, cùng vế "sinh lại bảng khóa" của ADR-0023, cho tới khi đồng bộ lấy `core/ads` (và `core/analytics`) qua một plan được duyệt.
- Trong lúc đó giữ quy tắc cũ của app:
  - ADR-0004: không khởi tạo SDK quảng cáo trước khi xử lý xong sự đồng ý của người dùng.
  - ADR-0013: feature chỉ dùng `AdSlot`, `rememberShowInterstitial` và `PrivacyOptionsButton`; quảng cáo toàn màn hình chỉ ở điểm dừng tự nhiên, không bao giờ lúc mở hay đóng app.
  - ADR-0014: vị trí mới mặc định tắt; khóa `ads_<vị trí>_enabled`; không đổi các giá trị mặc định khi không có quyết định mới của người phụ trách sản phẩm.
  - ADR-0023: đặt tên khóa theo quy ước của ADR-0023; khai báo khóa mới như các khóa hiện có trong `:core:config`, chưa sinh bảng khóa.
  - Không gọi AppsFlyer; không thêm thư viện AppsFlyer.
- Đợt đồng bộ lấy `core/ads` chuyển ADR này sang `superseded` trong cùng PR và sinh lại `docs/foundation.md`.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Hoãn bằng một ADR của app (đã chọn) | Quy tắc trong `foundation.md` khớp với code; phiên không gặp mâu thuẫn | Thêm một ADR phải nhớ gỡ khi đồng bộ `core` |
| Giữ ADR-0004, 0013, 0014 ở mốc cũ (`hold=`) | Không thêm ADR | ADR-0026, 0027 vẫn còn hiệu lực và trái với code; `foundation.md` vẫn mâu thuẫn |
| Lấy luôn `core/ads` trong đợt 1 | Hết mâu thuẫn | Đổi code quảng cáo, AppsFlyer, quyền mới trong một PR đồng bộ không có plan; trái quy trình |

## Hệ quả

- **Tích cực:** đợt 1 merge được mà không đổi hành vi quảng cáo của app; plan quảng cáo trước đợt đồng bộ `core` biết rõ quy tắc nào áp dụng.
- **Phải chấp nhận:** `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md`, `docs/ops/remote-config.md` có trong repo nhưng chưa áp dụng cho app; mẫu spec (`docs/templates/spec.md`, mục Quảng cáo, dẫn `ads-guide.md` và `ads-business-rules.md`) và mẫu thiết kế (`docs/templates/design.md`, mục Quảng cáo, dẫn `ads-guide.md`) cũng dẫn tới các tài liệu này, phần quảng cáo của hai mẫu chưa áp dụng.
- **Việc AI phải tuân thủ:** chưa áp dụng ADR-0026, ADR-0027, các vế mà ghi chú ngày 2026-10-23 đổi trong ADR-0004, ADR-0013, ADR-0014, và vế "sinh lại bảng khóa" của ADR-0023, cho tới khi `core/ads` được đồng bộ qua plan đã duyệt; không khởi tạo SDK quảng cáo trước khi xử lý xong sự đồng ý của người dùng; feature chỉ dùng `AdSlot`, `rememberShowInterstitial` và `PrivacyOptionsButton`; gọi quảng cáo toàn màn hình chỉ ở điểm dừng tự nhiên, không bao giờ lúc mở hay đóng app; khóa quảng cáo dạng `ads_<vị trí>_enabled`, vị trí mới mặc định tắt; khai báo khóa Remote Config mới như các khóa hiện có trong `:core:config` cho tới khi app có `AppRemoteKeys`; không thêm AppsFlyer khi chưa có plan đồng bộ `core` được duyệt.
