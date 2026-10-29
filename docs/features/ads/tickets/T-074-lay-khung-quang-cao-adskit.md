---
id: T-074
title: Lấy khung quảng cáo AdsKit của dự án mẫu về app (chưa bật AppsFlyer), chuyển ba vị trí quảng cáo hiện có sang khung mới, bật quảng cáo mở app và quay lại app
feature: ads
spec: features/ads/spec.md   # confirmed 2026-10-28; vị trí của từng màn vẫn ở mục 5 của features/camera/spec.md và features/poses/spec.md
scenarios: []
owner: Tuấn
status: on-staging
risk: high
size: L
depends_on: []               # đồng bộ đợt 4 (VGL-Studio/camera-pose#224) đã merge, base của docs/template-sync.txt là 47782ec
sprint:
plan: features/ads/plans/T-074-lay-khung-quang-cao-adskit.md   # draft, 2026-10-28
created: 2026-10-28
---

# T-074: LẤY KHUNG QUẢNG CÁO ADSKIT CỦA DỰ ÁN MẪU VỀ APP (CHƯA BẬT APPSFLYER), CHUYỂN BA VỊ TRÍ QUẢNG CÁO HIỆN CÓ SANG KHUNG MỚI, BẬT QUẢNG CÁO MỞ APP VÀ QUAY LẠI APP

## Mô tả

App đang dùng bộ quảng cáo cũ của dự án mẫu (commit `0eaeadb`): banner, interstitial, hộp thoại đồng ý, khóa `ads_<vị trí>_enabled`. ADR-1004 giữ app ở bộ cũ cho tới khi lấy `core/ads` và `core/analytics` qua một plan được duyệt. Dự án mẫu đã xong khung quảng cáo theo AdsKit PARTNER:
- nhịp hiển thị, mediation tám mạng;
- native, rewarded, app open, quảng cáo trong danh sách và trong luồng;
- AppsFlyer, ghi số liệu doanh thu.

Bộ này đã lên staging và được thử trên máy thật tới T-049 của dự án mẫu (`0.1.0-staging.18`).

Theo câu trả lời ở mục 9 của `features/ads/spec.md` (Nhật, 2026-10-28): AppsFlyer chưa bật, không mang màn xem thử quảng cáo, bật quảng cáo mở app (sau loading, trước camera, không ở lần mở đầu) và khi quay lại app (ở mọi màn, kể cả camera).

Sau ticket này, app dùng khung mới. Ba vị trí hiện có (`review_interstitial`, `poses_banner`, `poses_detail_banner`) chạy qua `AdPlacement` và khóa `is_show_<vị trí>`, theo luật của `docs/ops/ads-business-rules.md`. Bản staging dùng mã thử của Google. ADR-1004 chuyển `superseded`.

## Phạm vi

- **Lấy code của dự án mẫu** theo `docs/ops/to-app.md` của dự án mẫu, mục 5. Người hay AI có người theo dõi chạy `scripts/template-sync.sh --app <camera-pose> --take <đường dẫn>` cho từng phần, gộp xung đột, đẩy nhánh `template-sync/take-...`. Plan lấy nhánh đó làm điểm bắt đầu.
  - `core/ads`: 254 file đổi từ `0eaeadb`.
  - `core/analytics`: lấy khung nhưng không đưa SDK AppsFlyer vào app (câu 3 của spec). Plan kiểm làm được không; không được thì giữ `core/analytics` cũ (`hold=`) và báo trong plan trước khi duyệt.
  - `core/analytics` làm được không cần SDK (plan, mục 3.1): bỏ `implementation(libs.appsflyer)`, thay `AppsFlyerLibSink` (chỗ duy nhất import SDK, `AppsFlyerSink.kt:4`) bằng một sink rỗng gắn trong `AnalyticsModule`, bỏ `AppsFlyerSdkUseTest`, `:app` cấp `AppsFlyerConfig("")`. Đây là sửa code core dùng chung: ghi trong PR và đề xuất về dự án mẫu (`docs/ops/from-app.md`).
  - `core/config`: gộp tay T-007 (`RemoteKey`, `RemoteSettings.value(key)`, `FeatureFlag.description`) và T-008 (`LocalRemoteValues`, `@Multibinds` ở `ConfigModule`), không `--take`: app có `AppLinks`, `PoseMatchingSettings`, 7 cờ thêm, `REFRESH_TIMEOUT_MILLIS = 15_000` (ADR-1001). Đổi API làm sáu chỗ gọi phải sửa: `DefaultPoseCatalogRepository.kt:127` (`core/poses`), `InAppReview.kt:47,51` (`core/review`), `NetworkDialogController.kt:123` (`core/network`), `AppLinks`, `PoseMatchingSettings`, `core/ads` cũ.
  - Các core mà `core/ads` cần ở bản mới: `core/network` chỉ lấy interface `InternetConnectivity` và binding của nó (T-020 của mẫu) cùng `setFullScreenAdShowing` (một phần T-017), không lấy phần còn lại của `core/network` mẫu; `core/designsystem`, `core/assets` không lấy (app đã có bản riêng); `core/data`, `core/logging`, `build-logic` mẫu không đổi từ `0eaeadb`. Ghi chú: T-076 (`VALIDATED`) đổi ý nghĩa của `InternetConnectivity`; T-074 không phụ thuộc T-076, T-077.
  - `feature/loading`: chép tay T-024 của mẫu (`StartupAds` trong `LoadingViewModel`, `LoadingRoute`), vì loading của app khác (catalog R2, bộ khởi đầu, chờ 15 giây, chữ và ảnh chuẩn riêng); test theo `LoadingRouteTest` và `FakeStartupAds` của mẫu; kiểm tổng thời gian loading với giới hạn của `StartupAds` (đồng ý 10 giây, SDK 35 giây, quảng cáo 40 giây, tính từ lúc mở app).
  - `docs/template-sync.txt`: tách `hold=core` theo module trước khi `--take` (script chỉ nhận đúng một dòng `hold=<đường dẫn>`).
  - `gradle/libs.versions.toml`: chín adapter mediation, Unity Ads. Không thêm AppsFlyer.
  - `settings.gradle.kts`: kho Maven của Pangle và Mintegral.
  - Phần `app/` mà khung cần:
    - `AdsHost`;
    - khởi tạo sau đồng ý;
    - `app/lint.xml`;
    - không mang màn xem thử quảng cáo `AdGalleryActivity` (câu 4 của spec);
    - `StagingRemoteValuesModule` của app **không** lấy nguyên bản mẫu: bản mẫu bật sẵn quảng cáo mở app, quay lại app, combo native sau interstitial và native đáy ở staging. Theo câu 6 của spec, app bật `show_open_ads` và `show_resum_ads`, không bật `is_show_native_after_inter`, `is_show_native_bottom`; không bật `show_open_ads_first_open` (câu 6). Có test kiểm các giá trị này.
    - Quảng cáo mở app do màn loading gọi `StartupAds` của khung, hiện khi loading xong, trước khi vào camera; không hiện ở lần mở đầu sau khi cài. Cần `feature_loading_enabled` bật. Quảng cáo khi quay lại app theo cổng của khung (luật A-11), không có cổng theo màn: không sửa `core/ads` cho việc này. Có test cho phần của app.
    - Đánh dấu các lần rời app có chủ đích bằng `rememberAdSuppression` và `rememberMarkLeavingApp` của khung (`AdSuppression.kt`, luật G-03, G-05): hộp thoại quyền camera (camera, màn chào), quyền lưu, chia sẻ ảnh, mở cài đặt hệ thống, mở link và trang ngôn ngữ từ cài đặt, hộp đánh giá In-App Review. Thiếu thì quảng cáo quay lại app hiện ngay sau hộp thoại quyền.
    - Đổi hành vi: bộ cũ không mở quảng cáo toàn màn hình khi popup mạng đang hiện (`AdsController.kt:125`, `networkStatus.dialogShown`); khung mới chỉ theo `InternetConnectivity`, popup chờ quảng cáo đóng (`setFullScreenAdShowing`).
    - Firebase staging thật (bí mật `STAGING_GOOGLE_SERVICES_JSON`) thắng giá trị cục bộ của `StagingRemoteValuesModule`; người kiểm các khóa trên Firebase staging trước khi phát (plan, mục 3.1).
- **Chuyển ba vị trí của app sang `AdPlacement` mới.**
  - `review_interstitial`: interstitial.
  - `poses_banner`, `poses_detail_banner`: banner.
  - Bỏ `SAMPLE_BOTTOM_BANNER` (câu 4 của spec).
  - Các chỗ gọi: `ReviewScreen.kt`; `PosesScreen.kt:85` và `PoseDetailScreen.kt:51` (qua `PosesBottomBanner` ở `PosesComponents.kt`); `SettingsScreen.kt`, `SettingsViewModel.kt` (nút quyền riêng tư).
  - Luật tần suất của `review_interstitial` theo câu 1 của spec: giữ "không trước ảnh lưu thứ 2" và "tối đa 1 lần mỗi 3 ảnh" (khóa `ads_review_interstitial_first_break`, `ads_review_interstitial_breaks_between`), bỏ 120 giây riêng; nhịp chung `ads_interval`.
  - Khóa Remote Config đổi từ `ads_<vị trí>_enabled` sang `is_show_<vị trí>`.
  - Bảng khóa sinh lại theo `docs/ops/remote-config.md`.
- **Câu trả lời của `features/ads/spec.md` mục 9 đã chép vào mục 5 của spec camera, loading, poses** trong PR xác nhận spec ads (2026-10-28). T-074 chỉ còn:
  - bỏ các ghi chú "Áp dụng từ T-074" và "trước đó ..." ở mục 5 của ba spec khi code đã đổi;
  - mục 6 của spec màn có vị trí: sự kiện phân tích mà khung ghi (không có AppsFlyer). Phần này đổi spec đã xác nhận, nên plan nêu rõ để người xác nhận lại.
- **Ảnh chuẩn screenshot.** Ảnh của màn có quảng cáo ghi lại theo plan, lệnh có `--rerun` (L-114 của dự án mẫu). Ảnh mới của khung (catalog native, màn xem thử) theo plan.
- **ADR và tài liệu.**
  - ADR-1004 chuyển `superseded`.
  - Bỏ các dòng `hold=` của các đường dẫn đã lấy trong `docs/template-sync.txt`.
  - Sinh lại `docs/foundation.md`.
  - Ghi chú trong `docs/template-sync.txt` về `release.sh` để lại cho T-075.

**Không bao gồm:**

- `scripts/release.sh`, `docs/ops/release.md`, mã quảng cáo thật: ticket T-075.
- Thêm vị trí quảng cáo mới khác `open_app` và `resume` (native, rewarded, trong danh sách). Mỗi vị trí mới cần spec được duyệt và ticket riêng.
- AppsFlyer, màn xem thử quảng cáo (câu 3, 4 của spec).
- Các phần khác của dự án mẫu đã đổi từ `0eaeadb` mà khung quảng cáo không cần, ví dụ `feature/loading`, `core/assets`. Plan liệt kê phần nào để lại, cho một đợt `--take` khác.

## Rủi ro sơ bộ

Chạm gần hết điểm chặn của `risk-scoring.md`:
- quảng cáo;
- SDK mới (chín adapter mediation, Unity Ads);
- quyền mà các SDK kéo vào manifest gộp;
- vị trí quảng cáo mới (`open_app`, `resume`);
- module core;
- `gradle/libs.versions.toml`, `settings.gradle.kts`.

Rủi ro cao, cần duyệt chéo (Tuấn và Nhật). Phần chép từ dự án mẫu rất lớn (khoảng 37 nghìn dòng ở `core/ads`, gồm test và ảnh chuẩn). Plan nên tách thành nhiều lượt trong cùng danh sách chạy:
1. lấy code và build qua;
2. nối vỏ app và chuyển vị trí;
3. spec, ảnh chuẩn, tài liệu.

Mỗi lượt có PR riêng.

## Câu hỏi cho người

Sáu câu ở mục 9 của `features/ads/spec.md` đã có câu trả lời (Nhật, 2026-10-28). Nhật xác nhận spec ngày 2026-10-28.

## Định nghĩa hoàn thành

- [ ] Ba vị trí hiện có chạy qua khung mới, đúng spec đã cập nhật; khóa `is_show_<vị trí>` mặc định tắt.
- [ ] Hộp thoại đồng ý và khởi tạo SDK đúng ADR-0004, ADR-0013 của dự án mẫu (đã đồng bộ); không có vị trí quảng cáo của màn camera, màn chào hay loading, kể cả ở bản staging; quảng cáo mở app sau loading (không ở lần mở đầu) và khi quay lại app (mọi màn) theo câu 6 của spec, có test.
- [ ] Không có SDK, quyền hay dữ liệu gửi đi của AppsFlyer; không có `AdGalleryActivity`, `SAMPLE_BOTTOM_BANNER`.
- [ ] Manifest gộp chỉ có quyền mà spec đã duyệt; bản release staging build qua với R8.
- [ ] Test của khung (lấy từ dự án mẫu) và test của app qua; ảnh chuẩn đã ghi lại theo plan.
- [ ] ADR-1004 `superseded`; `docs/template-sync.txt` không còn `hold=` cho các đường dẫn đã lấy.
- [ ] Có `docs/remote-config-keys.md`; `scripts/ads-plan.py` chạy được trên app (`CLAUDE.md`, mục Lệnh), hai test đọc code thật của khối `ads-plan` trong `scripts/tests/run.sh` không còn bỏ qua.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật: banner ở thư viện và chi tiết tư thế, interstitial khi rời màn xem lại theo luật đã chốt, quảng cáo mở app sau loading từ lần mở thứ hai, quảng cáo khi quay lại app ở thư viện và ở camera (camera mở lại được sau khi đóng quảng cáo), không bấm quảng cáo; đã nghiệm thu chéo.
