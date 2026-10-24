---
ticket: T-023
title: Banner ở thư viện và chi tiết tư thế
status: done             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 25M          # cỡ S, số gợi ý 5.0M (tạm, 2 task); nâng lên vì sửa :core:ads và 6 ảnh chuẩn, mục 8 câu 6
blocking_points: [ads, core] # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-023: BANNER Ở THƯ VIỆN VÀ CHI TIẾT TƯ THẾ

## 1. Tóm tắt

Thêm hai vị trí banner `poses_banner` (neo đáy thư viện) và `poses_detail_banner` (đáy chi tiết, dưới thanh nút "Chụp với tư thế này") vào `AdPlacement`, đặt qua `AdSlot`, mặc định tắt (cờ `ads_<vị trí>_enabled`, ADR-0014). `AdSlot` của `:core:ads` sửa thêm hai điểm để đúng KB-6: banner tải lỗi thì thu về 0 dp thay vì giữ chỗ trống, và nhận một đường kẻ phân cách chỉ vẽ khi banner đang chiếm chỗ. Kiểm chứng bằng test Robolectric của `AdSlot` với `AdView` thật (đã chạy thử), test giao diện đo vị trí banner so với lưới và nút chính, 6 screenshot test (4 cùng tên ảnh thiết kế `_ad_`, 2 khung thấp) và kiểm tra thủ công trên staging với mã quảng cáo thử của Google.

## 2. Hiểu yêu cầu

Ticket xử lý KB-6 trong `features/poses/spec.md`.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-6 | Cờ `ads_poses_banner_enabled` bật và người dùng đã đồng ý thì banner neo ở đáy thư viện; lưới cuộn phía trên banner, không bị che | `PosesScreen` nhận `bottomBanner: @Composable () -> Unit = {}` đặt cuối `Column`, sau vùng lưới có `weight(1f)`; nên lưới luôn kết thúc ở cạnh trên banner. `PosesRoute` truyền `PosesBottomBanner(AdPlacement.POSES_BANNER)`. Điều kiện cờ và đồng ý đã nằm trong `AdSlot` (`canRequestAds`, `isEnabled`, ADR-0013, ADR-0014) |
| KB-6 | Cờ `ads_poses_detail_banner_enabled` bật thì banner ở đáy chi tiết, dưới nút chính; nút không bị che hay đẩy ra ngoài màn hình | `PoseDetailScreen` (T-022) nhận `bottomBanner` như trên, đặt sau thanh nút đáy. Vùng cuộn có `weight(1f)` co lại, thanh nút và banner luôn trong màn hình, kể cả khung 360 x 640 (README mục 6) |
| KB-6 | Cờ tắt hoặc banner không tải được thì không chừa khoảng trống | Cờ tắt, chưa đồng ý, không có mã: `AdSlot` đã vẽ `Box` rỗng (0 dp). Không tải được: hiện `AdSlot` giữ chỗ bằng chiều cao banner từ lúc tạo `AdView` và giữ mãi khi tải lỗi. Plan sửa `AdSlot`: `AdListener.onAdFailedToLoad` thu `AndroidView` về 0 dp và bỏ đường kẻ; `onAdLoaded` (lần làm mới sau) mở lại. `AdView` vẫn gắn vào cây giao diện để lần làm mới tự động của AdMob vẫn chạy |
| Thiết kế | Vùng quảng cáo tách khỏi nút bằng đường kẻ và nền khác màu (README mục 5) | `AdSlot` thêm tham số `separator: (@Composable () -> Unit)? = null`, vẽ phía trên banner chỉ khi banner đang chiếm chỗ. Màn truyền đường kẻ 2 dp `outlineVariant` và nền `surfaceContainerHigh` cho vùng banner qua `modifier` (vùng 0 dp thì nền không thấy) |
| Ticket | Vị trí trong `AdPlacement`, cờ mặc định tắt | `POSES_BANNER("poses_banner", AdFormat.BANNER)`, `POSES_DETAIL_BANNER("poses_detail_banner", AdFormat.BANNER)`; khóa `ads_poses_banner_enabled`, `ads_poses_detail_banner_enabled` sinh từ `AdsSettings.placementEnabledKey`, mặc định `false` (`PLACEMENT_ENABLED_DEFAULT`) |

Thiết kế tham chiếu: `features/poses/design/README.md` (mục 3, dòng Banner; mục 5; mục 6, màn hình thấp), các ảnh trong `design/screens/`: `PosesScreen_ad_light`, `PosesScreen_ad_dark`, `PoseDetailScreen_ad_light`, `PoseDetailScreen_ad_dark`; số liệu trong `design/context/` cùng tên (vùng `AdSlot` 411 x 58: đường kẻ 2 dp và vùng banner; nhãn "Quảng cáo" là của AdMob, README mục 2).

ADR liên quan: ADR-0004 (mọi vị trí khai báo một chỗ trong `AdPlacement`), ADR-0013 (feature chỉ dùng `AdSlot`, đồng ý trước khi tải), ADR-0014 (vị trí mặc định tắt), ADR-0007 và ADR-0009 (mã thật chỉ có lúc build bản phát hành), ADR-0020 (screenshot test cùng tên ảnh thiết kế).

## 3. Giải pháp

### 3.1. Cách làm

**`:core:ads`.**

- `AdPlacement.kt`: thêm hai dòng, mỗi vị trí đúng một dòng dạng `TÊN("khóa", AdFormat.BANNER),` vì `scripts/release.sh` đọc danh sách vị trí bằng biểu thức chính quy theo từng dòng (mục 3.2, nơi khác đọc).
- `AdSlot.kt`:
  - `fun AdSlot(placement: AdPlacement, modifier: Modifier = Modifier, separator: (@Composable () -> Unit)? = null)`. Lời gọi cũ (`:feature:sample`) không đổi.
  - Phần vẽ banner tách thành `@Composable internal fun BannerView(adView: AdView, heightDp: Int, separator: (@Composable () -> Unit)?, modifier: Modifier)`: `var failed by remember(adView) { mutableStateOf(false) }`; `DisposableEffect(adView)` gắn `adView.adListener = bannerListener { failed = it }` và gỡ khi rời; `Column(modifier) { if (!failed) separator?.invoke(); AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(if (failed) 0.dp else heightDp.dp)) }`.
  - `internal fun bannerListener(onFailedChange: (Boolean) -> Unit): AdListener`: `onAdFailedToLoad` gọi `onFailedChange(true)`, `onAdLoaded` gọi `onFailedChange(false)`.
  - `BannerAd` giữ cách tạo `AdView`, `loadAd`, theo dõi vòng đời như hiện tại, rồi gọi `BannerView`. Ghi chú KDoc: chiều cao vẫn giữ trước khi tải để màn hình không giật; chỉ thu về khi tải lỗi.
  - Không thêm log: `:core:ads` hiện không ghi log khi banner lỗi, giữ như vậy (mục 8 câu 4).

**`:feature:poses`.**

- `PosesComponents.kt`: `@Composable internal fun PosesBottomBanner(placement: AdPlacement)`: `AdSlot(placement, Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).testTag("poses_banner_slot"), separator = { PosesAdSeparator() })`; `@Composable internal fun PosesAdSeparator()`: `HorizontalDivider(thickness = Stroke.sticker, color = outlineVariant)`.
- `PosesScreen.kt`: `PosesScreen(..., bottomBanner: @Composable () -> Unit = {})`; gọi `bottomBanner()` cuối `Column` của màn (sau `ReadyContent`, `Error`, `Loading`), nên banner neo đáy ở mọi trạng thái trừ `Disabled`. `PosesRoute` truyền `{ PosesBottomBanner(AdPlacement.POSES_BANNER) }`. `SnackbarHost` giữ ở đáy `Box` (nằm trên banner khi hiện).
- `detail/PoseDetailScreen.kt` (T-022 tạo): `PoseDetailScreen(..., bottomBanner: @Composable () -> Unit = {})`, gọi sau thanh nút đáy khi `Ready`; `PoseDetailRoute` truyền `{ PosesBottomBanner(AdPlacement.POSES_DETAIL_BANNER) }`.
- Screenshot test `_ad_` dùng một banner giả trong file test (`TestBanner`: `PosesAdSeparator()` rồi `Box` cao 56 dp, nền `surfaceContainerHigh`; tổng 58 dp như vùng `AdSlot` của thiết kế), vì `AdSlot` không vẽ gì khi không có `LocalAdsController` (test và preview không bao giờ có quảng cáo thật).

**Thứ tự làm (L-025).** Bước 3 (commit test): sửa `AdUnitIdsTest.kt` (mục 4); hai dòng mới của `AdPlacement` (để test biên dịch được; chưa nơi nào dùng); khung: `BannerView` có đủ chữ ký nhưng giữ cách vẽ cũ (luôn giữ chiều cao, không vẽ `separator`), `bannerListener` trả `AdListener()` rỗng; `AdSlot` nhận `separator` nhưng chưa dùng; `PosesBottomBanner`, `PosesAdSeparator` vẽ `Box` rỗng; `PosesScreen`, `PoseDetailScreen` nhận `bottomBanner` nhưng chưa gọi. Mọi file test. Bước 4 (commit code): cài đặt thật, ghi ảnh chuẩn (`./gradlew recordRoborazziDebug`), `scripts/design-compare.sh poses`, build `stagingDebug`.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

Đường dẫn gói viết tắt: `.../ads` là `com/vglstudio/camera/pose/line/guide/core/ads`; `.../poses` là `com/vglstudio/camera/pose/line/guide/feature/poses`.

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | Thêm `POSES_BANNER`, `POSES_DETAIL_BANNER`, mỗi vị trí một dòng |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdSlot.kt` | Tham số `separator`; `BannerView`, `bannerListener`; thu về 0 dp khi tải lỗi |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdSlotTest.kt` | Test Robolectric của `AdSlot`, `BannerView`, `bannerListener` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | `configuredUnits_areParsedPerPlacementKey`: chuỗi cấu hình có mã cho mọi vị trí (mục 4). Nếu plan kia (T-018 hay T-023) đã merge trước thì test đã sinh chuỗi từ `entries`: giữ nguyên phần đó, chỉ thêm khẳng định của plan này. |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreen.kt` | Tham số `bottomBanner`; `PosesRoute` truyền banner `poses_banner` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesComponents.kt` | `PosesBottomBanner`, `PosesAdSeparator` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreen.kt` | Tham số `bottomBanner`; `PoseDetailRoute` truyền banner `poses_detail_banner` (file do T-022 tạo) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenTest.kt` | Test vị trí banner ở thư viện (mục 4) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenScreenshotTest.kt` | 3 screenshot test có banner giả |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenTest.kt` | Test vị trí banner ở chi tiết (mục 4; file do T-022 tạo) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenScreenshotTest.kt` | 3 screenshot test có banner giả (file do T-022 tạo) |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_ad_light.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_ad_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_light.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_ad_small.png` | Khung 360 x 640, banner và lưới (L-028) |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_small.png` | Khung 360 x 640, banner không đẩy nút chính ra ngoài |
| Ghi lại | `docs/features/poses/design/review/` | Sinh bởi `scripts/design-compare.sh poses`, không sửa tay |

Không file nào khác. Đặc biệt không sửa `AdsController.kt`, `AdsSettings.kt`, `AdUnitIds.kt`, `FullScreenAdGuard.kt`, `:feature:sample`, `:app` (`LocalAdsController` đã có), `scripts/release.sh`, `AndroidManifest.xml`, `gradle/libs.versions.toml`. Ảnh chuẩn cũ của thư viện và chi tiết không đổi, vì `bottomBanner` mặc định rỗng.

**Nơi khác đọc các file trên** (L-020):

- `AdPlacement.kt`:
  - `scripts/release.sh` (dòng 138 đến 145) tìm `AdPlacement.kt`, đọc khóa mỗi dòng dạng `TÊN("khóa", AdFormat.X),` và dừng nếu `release.properties` thiếu mã cho một vị trí. Không sửa script (CLAUDE.md, mục Không được làm); hai dòng mới viết đúng dạng đó. Hệ quả: lần phát hành sau, dev phải thêm `poses_banner=` và `poses_detail_banner=` vào `adUnits` (mục 8 câu 1).
  - `AdUnitIdsTest.kt`: `configuredUnits_areParsedPerPlacementKey` khẳng định `missing` rỗng với chuỗi chỉ có `sample_bottom_banner`; thêm vị trí làm test này thất bại, nên plan sửa chuỗi (mục 4). `placementWithoutUnit_isReportedMissingAndShowsNoAd`, `googleTestUnits_existForEveryPlacement`, `everyPlacementKey_isUniqueAndUsableInRemoteConfig` duyệt `AdPlacement.entries`, vẫn qua với khóa mới.
  - `AdsController.kt` (dòng 192) chỉ tải trước vị trí `INTERSTITIAL`; hai vị trí mới là `BANNER`, không ảnh hưởng.
- `AdSlot.kt`: `feature/sample/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/sample/SampleScreen.kt` gọi `AdSlot(placement, modifier)`; tham số mới có mặc định, nên không đổi. Ảnh chuẩn của `:feature:sample` không đổi vì test không có `LocalAdsController`.
- `PosesScreen.kt`, `detail/PoseDetailScreen.kt`: chỉ test của module và `PosesNavigation.kt`, `PoseDetailNavigation.kt` (gọi `PosesRoute`, `PoseDetailRoute` không đổi chữ ký) đọc.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: không thêm `FeatureFlag`. Hai khóa Remote Config của vị trí sinh tự động: `ads_poses_banner_enabled`, `ads_poses_detail_banner_enabled`, mặc định tắt (ADR-0014).
- Quảng cáo (điểm chặn 1, đã có trong spec mục 5):
  - `poses_banner`: banner, neo đáy thư viện, làm mới theo mặc định của AdMob.
  - `poses_detail_banner`: banner, đáy chi tiết dưới thanh nút chính, làm mới theo mặc định của AdMob.
  - Staging dùng mã thử của Google (`GoogleTestAdUnitIds`); bản phát hành cần mã thật trong `adUnits` (ADR-0009), dev thêm trước lần phát hành sau.
  - Đổi hành vi chung của `AdSlot` (thu về 0 dp khi tải lỗi, đường kẻ phân cách): có ích cho mọi app, đề xuất về dự án mẫu theo `docs/ops/from-app.md`, ghi trong mô tả PR.
- Sự kiện phân tích: không chạm.
- Dữ liệu cục bộ: không chạm.
- File R2: không chạm.
- **Quyền truy cập:** Không thêm. Banner dùng quyền mà `play-services-ads` đã gộp vào manifest (`INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID`).
- **Thư viện và SDK:** Không thêm. `AdListener`, `AdView`, `LoadAdError` thuộc `play-services-ads` (`libs.play.services.ads`), đã có trên classpath biên dịch của `:core:ads`; `:feature:poses` chỉ gọi `AdSlot`, `AdPlacement` của `:core:ads` (plugin `template.android.feature` đã thêm `:core:ads`).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `BannerView` với `AdView` thật trong test Robolectric (đã chạy thử, mục 4.1); trên CI có thể khác nếu `AdView` cần dịch vụ Google Play lúc tạo.
- **Điều kiện chuyển:** `AdSlotTest` thất bại vì `AdView(context)` ném lỗi khi tạo trên CI (không phải vì khẳng định), sau 2 lần sửa.
- **Cách làm thay:** `BannerView` nhận `content: @Composable (Modifier) -> Unit` thay vì `AdView`; `BannerAd` truyền `AndroidView(factory = { adView })`, test truyền `Box` có `testTag`; `bannerListener` vẫn test riêng bằng cách gọi `onAdFailedToLoad` trên `AdListener` (không cần `AdView`). Test viết trước giữ nguyên điều kiểm tra (chiều cao 0 dp khi lỗi, đường kẻ biến mất, mở lại khi tải được). File không đổi: chỉ `AdSlot.kt` và `AdSlotTest.kt` (dự phòng).

## 4. Kiểm thử

Mọi test dưới đây vào commit test của bước 3, cùng khung ở mục 3.1. Lý do thất bại ở cột cuối là điều AI dự kiến; bước 3 chạy và ghi lý do thật vào nhật ký.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Vị trí mới, khóa đúng dạng | Unit test | `AdUnitIdsTest.everyPlacementKey_isUniqueAndUsableInRemoteConfig`, `googleTestUnits_existForEveryPlacement` (có sẵn, duyệt `AdPlacement.entries`); thêm `posesPlacements_areBannersWithSpecKeys`: `POSES_BANNER.key == "poses_banner"`, `POSES_DETAIL_BANNER.key == "poses_detail_banner"`, cả hai `AdFormat.BANNER`, `AdsSettings.placementEnabledKey(POSES_BANNER) == "ads_poses_banner_enabled"`. Thất bại: chưa có hai vị trí (lỗi biên dịch, nên hai dòng của `AdPlacement` vào commit test, mục 3.1; test so khóa vẫn chạy được) |
| Mặc định tắt (ADR-0014) | Test sẵn có | `AdsSettingsTest` (khóa và giá trị mặc định chung cho mọi vị trí) |
| `missing` rỗng khi có mã cho mọi vị trí | Unit test (sửa) | `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey`: chuỗi giữ phần cũ `" sample_bottom_banner = unit/1 ; other_key=unit/2;"` (kiểm khoảng trắng, khóa lạ) và nối thêm `"<khóa>=unit/<khóa>;"` cho mọi vị trí khác `SAMPLE_BOTTOM_BANNER` của `AdPlacement.entries`; khẳng định cũ giữ nguyên (`of(SAMPLE_BOTTOM_BANNER) == "unit/1"`, `missing` rỗng), thêm `of(POSES_BANNER) == "unit/poses_banner"`. Sinh từ `entries` để T-018 (thêm `review_interstitial`) không phải sửa lại |
| KB-6, banner tải lỗi thì không chừa chỗ | Test Robolectric | `AdSlotTest.failedLoad_collapsesBannerToZero_andHidesSeparator` (`GraphicsMode.NATIVE`): `BannerView` với `AdView` thật (mã thử, không gọi `loadAd`), `heightDp = 50`, `separator` là `Box` cao 2 dp có `testTag`; trước: vùng banner 50 dp, có `separator`; gọi `adView.adListener.onAdFailedToLoad(LoadAdError(3, "No fill", "com.google.android.gms.ads", null, null))` trên luồng giao diện: vùng banner 0 dp, không còn `separator`, phần tử đặt ngay dưới dịch lên 52 dp. Thất bại: khung giữ 50 dp |
| Banner tải được sau lần lỗi thì hiện lại | Test Robolectric | `AdSlotTest.loadedAfterFailure_showsBannerAndSeparatorAgain` (tiếp test trên, gọi `onAdLoaded()`: 50 dp, có `separator`) |
| Giữ chỗ trước khi tải (hành vi cũ) | Test Robolectric | `AdSlotTest.beforeLoad_keepsBannerHeight_withSeparator` (qua ngay ở khung, chặn hồi quy) |
| Không có bộ điều khiển quảng cáo: không chiếm chỗ | Test Robolectric | `AdSlotTest.withoutAdsController_takesNoSpace_andDrawsNoSeparator` (`AdSlot(SAMPLE_BOTTOM_BANNER, Modifier.testTag(...), separator = {...})`: cao 0 dp, `separator` không có trong cây; qua ngay ở khung) |
| `bannerListener` | Unit test (Robolectric) | `AdSlotTest.bannerListener_reportsFailedThenLoaded` (gọi hai hàm của `AdListener`, ghi lại `[true, false]`). Thất bại: khung trả `AdListener()` rỗng |
| KB-6, banner thư viện neo đáy, lưới không bị che | Compose UI test | `PosesScreenTest.banner_sitsBelowGrid_lastCardScrollsAboveIt` (`GraphicsMode.NATIVE`, `bottomBanner` là `Box` cao 58 dp mang `testTag`): cạnh dưới lưới bằng cạnh trên banner, cạnh dưới banner bằng cạnh dưới màn; `performScrollToIndex(6)`, cạnh dưới thẻ cuối không vượt cạnh trên banner. Thất bại: khung không gọi `bottomBanner` |
| KB-6, banner thư viện ở trạng thái lỗi và đã lưu rỗng | Compose UI test | `PosesScreenTest.banner_shownAlsoOnErrorAndSavedEmpty` (banner có trong cây ở `Error` và `Ready` rỗng) |
| KB-6, cờ tắt (không có quảng cáo) thì không chừa chỗ ở thư viện | Compose UI test | `PosesScreenTest.realBottomBanner_withoutAds_takesNoSpace`: `bottomBanner = { PosesBottomBanner(AdPlacement.POSES_BANNER) }` không có `LocalAdsController` (như cờ tắt): cạnh dưới lưới bằng cạnh dưới màn, `poses_banner_slot` cao 0 dp |
| KB-6, banner chi tiết dưới nút chính, nút không bị che | Compose UI test | `PoseDetailScreenTest.banner_belowBottomBar_buttonNotCovered` (`GraphicsMode.NATIVE`): cạnh trên banner bằng cạnh dưới thanh nút, banner chạm đáy màn, nút "Chụp với tư thế này" nằm trọn trong màn và trên banner |
| KB-6, màn thấp: banner không đẩy nút ra ngoài | Compose UI test | `PoseDetailScreenTest.smallScreen_withBanner_buttonFullyVisible` (`@Config(qualifiers = "w360dp-h640dp-xxhdpi")`): nút nằm trọn trong khung màn hình; chữ lỗi cuối cuộn được tới trên thanh nút |
| KB-6, cờ tắt thì không chừa chỗ ở chi tiết | Compose UI test | `PoseDetailScreenTest.realBottomBanner_withoutAds_takesNoSpace` (như ở thư viện: cạnh dưới thanh nút bằng cạnh dưới màn) |
| Giao diện có banner, đúng thiết kế | Screenshot test cùng tên ảnh thiết kế, khung `w411dp-h891dp-xxhdpi`, banner giả `TestBanner`; ảnh so sánh `scripts/design-compare.sh poses`; rà soát độc lập theo mục 7 của README thiết kế | `PosesScreen_ad_light.png`, `PosesScreen_ad_dark.png` bằng `captureRoboImage("src/test/screenshots/PosesScreen_ad_light.png") { ... }` (trạng thái như `default`); `PoseDetailScreen_ad_light.png`, `PoseDetailScreen_ad_dark.png` bằng `captureRoboImage("src/test/screenshots/PoseDetailScreen_ad_light.png") { ... }` (ảnh lớn `Ready`, không có vòng chờ) |
| Ảnh thiết kế của ticket khác | Không làm ở đây | Ảnh thiết kế của ticket khác (T-021): `PosesScreen_default_light`, `PosesScreen_default_dark`, `PosesScreen_loading_light`, `PosesScreen_loading_dark`, `PosesScreen_error_light`, `PosesScreen_error_dark`, `PosesScreen_saved-empty_light`, `PosesScreen_saved-empty_dark` |
| Ảnh thiết kế của ticket khác | Không làm ở đây | Ảnh thiết kế của ticket khác (T-022): `PoseDetailScreen_default_light`, `PoseDetailScreen_default_dark`, `PoseDetailScreen_loading_light`, `PoseDetailScreen_loading_dark`, `PoseDetailScreen_error_light`, `PoseDetailScreen_error_dark` |
| Ảnh thiết kế của ticket khác | Không làm ở đây | Ảnh thiết kế của ticket khác (T-024): `PosesScreen_default_light_unfolded`, `PosesScreen_default_dark_unfolded`, `PoseDetailScreen_default_light_unfolded`, `PoseDetailScreen_default_dark_unfolded` |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` | `PosesScreen_ad_small.png` (lưới cuộn phía trên banner), `PoseDetailScreen_ad_small.png` (`w05`: nhiều bước nhất; nút chính và banner thấy đủ) |
| Luồng thật trên máy | Kiểm tra thủ công | Staging, máy thật, đã đồng ý ở hộp thoại: (1) Remote Config staging bật `ads_poses_banner_enabled`: thư viện có banner thử của Google ở đáy, cuộn hết lưới, thẻ cuối không bị che; (2) bật `ads_poses_detail_banner_enabled`: chi tiết có banner dưới nút chính, nút bấm được, không bị che; (3) tắt hai khóa: không còn chỗ trống ở đáy; (4) chế độ máy bay rồi mở lại thư viện: banner không tải được, không còn chỗ trống (sau khi lần tải lỗi); (5) chế độ tối |

**Ngoại lệ viết test trước:** `PosesRoute`, `PoseDetailRoute` truyền `PosesBottomBanner` với đúng vị trí: dùng `hiltViewModel()` nên không dựng trong test JVM; kiểm bằng rà soát diff và kiểm tra thủ công (1), (2). `BannerAd` (tạo `AdView`, `loadAd`, theo dõi vòng đời) không đổi ngoài lời gọi `BannerView`.

### 4.1. Chạy thử

- **Đã chạy thử:** `AdView` thật trong test Robolectric thu về 0 dp khi `AdListener.onAdFailedToLoad` được gọi. Test tạm `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/SpikeBannerCollapseTest.kt` (không commit, đã xóa), 2026-10-23, trên nhánh chính `fa65d75`: dựng `AdView(context)` với mã thử, `AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, 411)`, gắn `AdListener`, vẽ bằng `AndroidView` cao 50 dp (0 dp khi lỗi), gọi `adView.adListener.onAdFailedToLoad(LoadAdError(3, "No fill", "com.google.android.gms.ads", null, null))` trên luồng giao diện.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*SpikeBannerCollapseTest*'`, rồi `scripts/check.sh` trên cùng cây làm việc (có test tạm).
- **Kết quả:** `./gradlew :core:ads:testDebugUnitTest`: 1 test qua (8,8 giây): tạo `AdView` không lỗi trên Robolectric SDK 35, không cần dịch vụ Google Play; vùng banner 50 dp, sau khi gọi `onAdFailedToLoad` còn 0 dp. `scripts/check.sh`: "Cổng chất lượng: QUA" sau 15 phút 30 giây; 749 test JVM, 0 lỗi (`:core:ads` 17 test, có test tạm), 65 ảnh screenshot. Phương án dự phòng (mục 3.4) chưa cần thử.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 1 (thêm hai vị trí quảng cáo, đã có trong spec mục 5) và điểm 7 (sửa `:core:ads`: `AdPlacement`, hành vi `AdSlot` khi banner tải lỗi). Không chạm điểm 2 (không thêm thư viện), 3 (không thêm quyền), 4 (không thêm sự kiện), 5 (không đổi hộp thoại đồng ý hay thứ tự khởi tạo SDK). Rủi ro cao, bắt buộc duyệt chéo; người phụ trách sản phẩm biết trước khi duyệt vì có vị trí quảng cáo mới.

Bảng điểm dưới đây chỉ để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:core:ads`, `:feature:poses`), đổi giao diện công khai của `AdSlot` (thêm tham số có mặc định) |
| B. Độ rõ của yêu cầu | 1 | Spec, thiết kế đã duyệt; AI tự quyết đường kẻ và nền (câu 2), banner ở trạng thái lỗi (câu 3) |
| C. Khả năng kiểm chứng tự động | 1 | Vị trí, chiều cao, thu về khi lỗi có test; banner thật của AdMob chỉ kiểm thủ công |
| D. Ảnh hưởng chéo | 3 | `AdSlot` đổi hành vi cho mọi màn có banner (hiện có `:feature:sample`); `AdPlacement.kt`, `AdUnitIdsTest.kt` mà T-018 cũng sửa; `scripts/release.sh` đòi mã cho hai vị trí mới |
| E. Độ mới của kỹ thuật | 2 | `AdListener` để thu banner về khi lỗi, `AdView` trong test Robolectric: lần đầu trong dự án (đã chạy thử) |
| **Tổng** | 10 | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `AdPlacement.kt`: thêm hai dòng. `AdSlot.kt`: thêm tham số, đổi hành vi khi tải lỗi (mọi app tạo từ dự án mẫu đều có lợi, đề xuất về dự án mẫu).
  - `AdUnitIdsTest.kt`: sửa chuỗi cấu hình của một test.
  - `PosesScreen.kt`, `PosesComponents.kt`, `PosesScreenTest.kt`, `PosesScreenScreenshotTest.kt` (T-021), `detail/PoseDetailScreen.kt`, `detail/PoseDetailScreenTest.kt`, `detail/PoseDetailScreenScreenshotTest.kt` (T-022).
- Task khác có thể xung đột:
  - **T-018** (quảng cáo toàn màn hình ở màn xem lại): thêm `REVIEW_INTERSTITIAL` vào `AdPlacement.kt`, có thể sửa `AdUnitIdsTest.kt`. Khác dòng; task chạy sau đọc lại file trên nhánh chính và giữ dòng của task kia. Chuỗi cấu hình sinh từ `entries` (mục 4) nên vị trí của T-018 tự có mã trong test.
  - **T-022**: tạo các file chi tiết mà plan này sửa. T-023 chạy sau T-022.
  - **T-024** (máy gập): sửa `PosesScreen.kt`, `detail/PoseDetailScreen.kt` và hai file screenshot test. Plan T-024 viết trên code sau T-023: banner nằm dưới cùng bố cục rộng (plan T-024).
- Đề xuất thứ tự chạy: T-022, rồi T-023, rồi T-024.

## 7. Điều kiện dừng riêng

1. Cần sửa `AdsController.kt`, `AdsSettings.kt` hay `:app` để banner hiện.
2. Cần sửa `scripts/release.sh` (không plan nào được chạm).
3. `AdListener` đã được gán ở nơi khác của `AdView` (ví dụ `AdsController`) khiến `BannerView` ghi đè mất hành vi có sẵn.
4. Phương án dự phòng ở mục 3.4 cũng không qua.

## 8. Câu hỏi cho người duyệt

1. **Mã quảng cáo thật cho hai vị trí mới.** `scripts/release.sh` dừng khi `adUnits` trong `release.properties` thiếu mã của một vị trí. Sau khi T-023 merge, lần phát hành sau cần tạo hai đơn vị banner trên AdMob và thêm `poses_banner=...;poses_detail_banner=...`. Việc này không chặn phiên (staging dùng mã thử).
   - Đề xuất của AI: người phụ trách sản phẩm tạo hai đơn vị banner trước lần phát hành có T-023; ghi vào danh sách việc của bản phát hành.
2. **Đường kẻ và nền của vùng banner.** Thiết kế có đường kẻ 2 dp và nền khác màu; `AdSlot` hiện không vẽ gì quanh banner.
   - Đề xuất của AI: thêm tham số `separator` vào `AdSlot` (chỉ vẽ khi banner chiếm chỗ) và nền `surfaceContainerHigh` qua `modifier`. Không vẽ chữ "Quảng cáo": AdMob tự có nhãn (README mục 2).
3. **Banner ở thư viện khi lỗi catalog hay "Đã lưu" rỗng.** Spec chỉ nói "neo ở đáy thư viện".
   - Đề xuất của AI: hiện ở mọi trạng thái của thư viện (trừ cờ thư viện tắt); ở chi tiết chỉ khi tư thế đã hiện (`Ready`).
4. **Banner tải lỗi có ghi log không.** `:core:ads` hiện không ghi log lỗi banner.
   - Đề xuất của AI: không ghi ở task này; nếu muốn, một ticket riêng cho `:core:ads` ghi log qua `AppLogger` (dùng chung cho mọi app).
5. **Thu banner về khi tải lỗi đổi hành vi của mọi màn có banner** (hiện chỉ `:feature:sample`). Màn hình có thể nhảy một lần khi tải lỗi (chỗ trống mất đi), và nhảy lại nếu lần làm mới sau tải được.
   - Đề xuất của AI: chấp nhận, vì spec KB-6 yêu cầu không chừa khoảng trống; đề xuất cùng thay đổi về dự án mẫu.
6. **`token_estimate`.** Số gợi ý cỡ S là 5.0M (mới đo 2 task); task này sửa module core, 7 file test và 6 ảnh chuẩn.
   - Đề xuất của AI: ghi 25M.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-23, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### 2026-10-23, RUN-20261023-04: đã merge

- **Kết quả:** đã merge, PR #74, commit `6760799`. CI xanh ngay vòng đầu.
- **Test viết trước:** 13 test mới (`AdSlotTest` 5, `AdUnitIdsTest` 1, `PosesScreenTest` 3, `PoseDetailScreenTest` 3, cộng 1 khẳng định thêm) và 6 screenshot test; 12 thất bại đúng lý do trên khung rỗng. Viết chặt lại có đối chứng: 1 (`AdUnitIdsTest.posesPlacements_areBannersWithSpecKeys`, qua ngay vì hai dòng `AdPlacement` nằm trong commit test; đổi tạm khóa thì thất bại, rồi khôi phục).
- **Lệch so với plan:** `beforeLoad_keepsBannerHeight_withSeparator` thất bại trên khung (plan ghi qua ngay) vì khẳng định cả đường kẻ; `bannerListener` dùng `object : AdListener() {}` (lớp abstract); tag `poses_banner_slot` là hằng `PosesTestTags.BANNER_SLOT`. Không dùng phương án dự phòng 3.4.
- **Rà soát độc lập:** 0 chặn; 2 nên sửa nhỏ, chưa sửa: nhãn `// Small screens` rỗng trong `PosesScreenTest.kt`; `bannerListener` ghi đè `adView.adListener` cũ (hiện chưa ai gán). Đột biến tạm (bỏ thu về 0 dp, bỏ gỡ đường kẻ) làm 2 test thất bại mỗi lần: test thu về 0 dp bắt được cả hai nhánh.
- **Cần người kiểm tra:** kiểm thủ công mục 4 bước (4): banner thu về 0 dp sau lần tải lỗi có tự làm mới trong lần mở màn đó không (ghi chú rà soát trước phiên); 6 ảnh chuẩn `_ad_*`; ảnh so sánh thiết kế. Đề xuất về dự án mẫu: thay đổi `AdSlot` (`separator`, thu về 0 dp khi lỗi) theo `docs/ops/from-app.md`; `release.sh` sẽ đòi mã thật cho `poses_banner`, `poses_detail_banner`.
