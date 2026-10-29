---
id: ADR-1007
title: App dùng khung quảng cáo AdsKit của dự án mẫu, không có SDK AppsFlyer, với các chỗ lệch có chủ đích ghi trong ADR này
status: accepted             # proposed | accepted | superseded
origin: app                  # template: ADR của dự án mẫu (mã dưới 1000) | app: ADR riêng của app (mã từ 1001)
date: 2026-10-29
deciders: [Tuấn, Nhật]
supersedes: ADR-1004         # app áp dụng ADR-0026 và các vế đổi của ADR-0004, ADR-0013, ADR-0014, ADR-0023; ADR-0027 chỉ áp dụng phần không cần SDK AppsFlyer
superseded_by:
---

# ADR-1007: APP DÙNG KHUNG QUẢNG CÁO ADSKIT CỦA DỰ ÁN MẪU, KHÔNG CÓ SDK APPSFLYER

## Bối cảnh

ADR-1004 hoãn ADR-0026 (quảng cáo theo AdsKit PARTNER), ADR-0027 (AppsFlyer) và các vế đổi của ADR-0004, ADR-0013, ADR-0014, ADR-0023 tới khi app lấy `core/ads` của dự án mẫu qua một plan được duyệt. Plan T-074 (ba lượt, PR #228, #229 và PR của lượt 3) lấy `core/ads`, `core/analytics` của mẫu ở mốc `47782ec`, gộp tay phần `RemoteKey` của `core/config`, `InternetConnectivity` của `core/network`, `StartupAds` ở màn loading, và sinh bảng khóa `docs/remote-config-keys.md` từ `AppRemoteKeys`.

Spec ads của app (`features/ads/spec.md`, mục 9) chốt vài điều khác bản mẫu: không có AppsFlyer, vị trí của app có luật đếm ảnh riêng, banner lỗi tải không chừa khoảng trống. Bản staging của app còn dùng một project Firebase thật, khác dự án mẫu. Nếu không ghi lại, đợt đồng bộ sau có thể kéo lại SDK AppsFlyer hay đè phần riêng của app, và phiên đọc `foundation.md` sẽ tưởng app chạy đúng như mẫu.

## Quyết định

- App dùng khung quảng cáo AdsKit của dự án mẫu (`core/ads`), theo ADR-0026 và các vế đổi của ADR-0004, ADR-0013, ADR-0014: khóa `is_show_<vị trí>`, nhịp chung `ads_interval`, quảng cáo mở app (`open_app`) từ màn loading qua `StartupAds` và quảng cáo khi quay lại app (`resume`) ở mọi màn. Bảng khóa Remote Config sinh từ `AppRemoteKeys` theo ADR-0023.
- **Không có AppsFlyer.** `core/analytics` lấy từ mẫu nhưng không có `libs.appsflyer`; `NoOpAppsFlyerSink` thay `AppsFlyerLibSink` (file duy nhất gọi SDK); `:app` cấp `AppsFlyerConfig("")`, nên `AppsFlyerTracker` không khởi động. Các khóa `is_log_af_*` vẫn khai báo và có trong bảng khóa vì khung đọc chúng; không có SDK nên không gửi gì. Từ ADR-0027 app chỉ giữ phần không cần SDK.
- **Vị trí `gallery_*` và `sample_native_small`** của khung giữ trong `AdPlacement` chỉ vì test của khung dùng chúng. Không màn nào gọi, staging không bật, app không có `AdGalleryActivity`.
- **Luật đếm của `review_interstitial`** là của app: không hiện trước ảnh lưu thứ `ads_review_interstitial_first_break` (mặc định 2) của phiên, sau mỗi lần hiện chờ `ads_review_interstitial_breaks_between` ảnh (mặc định 3), đếm trong bộ nhớ (`InterstitialBreakSettings`, `InterstitialBreakGate` của `core/ads`). Hai luật này chạy trước cổng của khung; không còn luật 120 giây riêng, khoảng cách thời gian chỉ còn `ads_interval`.
- **Banner thu về 0 dp khi lỗi tải** nếu màn truyền `separator` cho `AdSlot` (spec poses, KB-6): vạch ngăn và khung banner cùng ẩn, view vẫn ở trong cây để khung thử tải lại (B-02). Không có `separator` thì giữ cách của khung.
- **Firebase staging thật thắng giá trị cục bộ.** `DefaultRemoteSettings` đọc giá trị Firebase trước, giá trị cục bộ của `StagingRemoteValuesModule` sau; một khóa đặt trên project Firebase staging đè giá trị staging trong code. Kiểm khóa trên Firebase staging là việc của người trước khi phát bản staging (`docs/ops/manual-test.md`, mục Quảng cáo).
- Khóa số đặt tên trước ADR-0023 (`in_app_review_min_saved_photos`, `in_app_review_min_days_between`, `pose_match_threshold`) giữ tên vì đã có trên Firebase; `RemoteConfigKeysTest` ghi rõ ba tên ngoại lệ. Khóa mới theo quy ước.
- `app/lint.xml` bỏ qua `NotificationPermission` chỉ cho gói `com.squareup.picasso.` (InMobi kéo vào, không ai gọi), như dự án mẫu ở T-034.
- `MainActivity` không đặt `FullScreenAdHost`: app không có vị trí native toàn màn hình. Bật `is_show_native_after_inter` thì combo không hiện được (khung ghi `NOT_READY`).
- `scripts/release.sh` và `docs/ops/release.md` giữ ở mốc `0eaeadb` tới T-075; không phát bản production giữa T-074 và T-075.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Lấy khung AdsKit, bỏ SDK AppsFlyer bằng sink rỗng (đã chọn) | Đúng spec ads câu 3; code gần bản mẫu, đợt đồng bộ sau dễ gộp | Phải nhớ bỏ dòng `appsflyer` khi đồng bộ; khóa `is_log_af_*` có trong bảng dù không dùng |
| Lấy khung, giữ `core/analytics` cũ | Không có mã AppsFlyer nào trong app | Lệch mẫu nhiều hơn; đợt đồng bộ sau phải gộp tay cả `core/analytics` |
| Giữ bộ quảng cáo cũ (ADR-1004) | Không đổi hành vi | Không có quảng cáo mở app và quay lại app; `foundation.md` tiếp tục hoãn ADR-0026 |
| Xóa vị trí `gallery_*`, `sample_native_small` và test của khung dùng chúng | `AdPlacement` chỉ có vị trí của app | Mất độ phủ test của khung, hoặc phải sửa nhiều test của mẫu |

## Hệ quả

- **Tích cực:** app có quảng cáo mở app và quay lại app theo khung đã chạy ở mẫu; bảng khóa và `scripts/ads-plan.py` chạy được; đợt đồng bộ sau lấy `core/ads`, `core/analytics` như phần chung.
- **Phải chấp nhận:** `core/ads`, `core/analytics` có phần riêng của app (luật đếm, `separator`, sink rỗng) mà đợt đồng bộ phải giữ; vị trí `gallery_*` có trong code dù không dùng; giá trị staging trong code có thể bị Firebase staging đè.
- **Việc AI phải tuân thủ:** không thêm SDK AppsFlyer hay `libs.appsflyer`, giữ `NoOpAppsFlyerSink` và `AppsFlyerConfig("")`; không gọi vị trí `gallery_*` hay `sample_native_small` từ màn nào, không bật chúng ở staging; giữ luật đếm của `review_interstitial` trong `InterstitialBreakGate` trước cổng của khung; màn cần banner thu về 0 dp khi lỗi tải thì truyền `separator` cho `AdSlot`; test chỉ kiểm giá trị cục bộ, điều Firebase staging trả là kiểm thủ công; không đổi tên khóa Remote Config đã phát hành, khóa mới theo ADR-0023; không phát bản production trước khi T-075 lấy `scripts/release.sh` mới.
