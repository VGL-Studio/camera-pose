---
ticket: T-093
title: Thanh đáy native nhỏ ở màn Cài đặt (settings_bottom, tắt lúc phát hành)
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 7.2M         # cỡ S theo scripts/token-usage.py estimate (2026-11-13), không nâng tay
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật
cross_reviewer: Nhật           # nhóm một dev (risk-scoring.md, mục 3)
approved_at: 2026-11-13
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-093: THANH ĐÁY NATIVE NHỎ Ở MÀN CÀI ĐẶT

## 1. Tóm tắt

Màn Cài đặt có thanh đáy native nhỏ của khung: `BottomAdBarScaffold` với vị trí mới `settings_bottom`, theo câu 19 của spec ads. Khóa `is_show_settings_bottom` mặc định tắt. Bản staging bật sẵn khóa này. Danh sách cài đặt nhận khoảng chừa ở đáy, nên cuộn hết thì dòng cuối nằm trên thanh.

Cách làm theo đúng mẫu đã chạy ở thư viện và chi tiết tư thế: khung bọc màn ở Route, màn nhận `contentPadding`, screenshot test vẽ thanh bằng `BottomAdBarLayout` và `NativeAdCard` giả.

Kiểm chứng gồm:

- test vị trí Robolectric: cuộn hết thì dòng "Phiên bản" nằm trên khe, cả với khe native lẫn khe banner; không có quảng cáo thì không chừa chỗ;
- screenshot test cùng tên với hai ảnh thiết kế `SettingsScreen_ad_light`, `SettingsScreen_ad_dark`, cộng ảnh khung thấp `SettingsScreen_ad_small`;
- test vị trí của app và giá trị của bản staging;
- bảng khóa sinh lại;
- thử trên máy thật với quảng cáo thử của Google.

## 2. Hiểu yêu cầu

Ticket xử lý KB-1 của `features/settings/spec.md` (confirmed 2026-11-13) và câu 19 của `features/ads/spec.md` (confirmed 2026-11-13).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1, tiêu chí 1 | Khi bấm nút bánh răng ở camera hoặc ở thư viện thì màn cài đặt mở, đúng thiết kế | Không đổi điều hướng. "Đúng thiết kế" nay gồm hai ảnh `ad` (khi khe có quảng cáo) và bốn ảnh cũ (khi không có) |
| KB-1, tiêu chí 2 | Khi `is_show_settings_bottom` bật, `is_show_native_bottom` bật và người dùng đã đồng ý thì thanh đáy native nhỏ (80 dp) hiện ở đáy màn cài đặt; danh sách cuộn phía trên thanh, dòng cuối không bị che | `SettingsRoute` bọc màn trong `BottomAdBarScaffold(AdPlacement.SETTINGS_BOTTOM)`. Khung tự xét đồng ý, khóa vị trí và khóa chọn lớp; plan không đổi phần đó. Phần đáy của `contentPadding` (96 dp khi khe native có quảng cáo) thành lề dưới bên trong vùng cuộn của danh sách, nên cuộn hết thì dòng "Phiên bản" nằm trên khe |
| KB-1, tiêu chí 3 | Khi `is_show_native_bottom` tắt thì khe đáy là banner ở cùng chỗ; danh sách vẫn cuộn phía trên, dòng cuối không bị che | Khung chọn banner và truyền chiều cao banner qua cùng `contentPadding` (D-04). Màn không phân biệt hai lớp; một test vị trí chạy với khe kiểu banner (không lề, cao 50) để chắc lề dưới theo đúng giá trị khung truyền |
| KB-1, tiêu chí 4 | Khi `is_show_native_collap` bật thì lớp quảng cáo nổi có thể phủ lên đáy danh sách và thu gọn được; thu gọn thì dòng cuối tới được | Hành vi của khung: lớp nổi không chừa chỗ (D-04), thu gọn thì còn khe native có chừa chỗ. Màn không có code riêng. Kiểm bằng test sẵn có của khung và bằng test của tiêu chí 2 (khe native sau khi lớp thu gọn). Bản staging để lớp này tắt như hôm nay |
| KB-1, tiêu chí 5 | Khi `is_show_settings_bottom` tắt hay không tải được quảng cáo thì không có thanh, danh sách dùng cả màn | Khung chỉ chừa chỗ khi khe đã có quảng cáo. Không có `LocalAdsController` (test) thì khung không vẽ gì, chừa 0 |
| KB-1, tiêu chí 6 | Khi bấm quay lại thì về màn trước đó | Không đổi; test sẵn có `back_callsOnBack` |
| Spec ads, câu 19, ý a | Production tắt lúc phát hành, bản staging bật sẵn | Khóa mặc định tắt (khung sinh từ khai báo). `StagingRemoteValuesModule.appPlacementsShown()` thêm `SETTINGS_BOTTOM` |
| Spec settings, mục 5 | Mở trang chính sách quyền riêng tư là rời app có chủ đích | Đã có (`rememberStartOutsideApp` trong `SettingsRoute`); giữ nguyên |

Thiết kế tham chiếu: `features/settings/design/README.md` (mục 1, dòng "Có thanh đáy native"; mục 3, dòng "Thanh đáy native"; mục 5; mục 6). Ảnh trong `design/screens/`: `SettingsScreen_ad_light`, `SettingsScreen_ad_dark` (Nhật duyệt 2026-11-13, PR #331). Số liệu ở `design/context/SettingsScreen_ad_*.md`:

- khe `NativeAdSmall` ở `y = 795`, cỡ 379 x 80, cách mép trái, phải và đáy 16;
- danh sách giữ nguyên vị trí của ảnh `default` (dòng cuối kết thúc ở `y = 628`), nên ở khung chuẩn không phải cuộn;
- số của thẻ (bo 12, khoảng cách 12, chữ 13 và 10) là số của khung (`NativeAdDimens`), không làm tròn hay thêm token.

ADR liên quan: ADR-0013, ADR-0014 (vị trí theo spec, mặc định tắt), ADR-0020 (ảnh thiết kế, screenshot test cùng tên), ADR-0023 (giá trị của bản staging), ADR-0026 (khung AdsKit), ADR-0029 (dữ liệu của test cũ), ADR-0031.

## 3. Giải pháp

### 3.1. Cách làm

**`AdPlacement.kt`.** Ở khối "The placements of this app", thêm sau `POSE_DETAIL_BOTTOM`:

```kotlin
/** Bottom bar of the settings screen (spec settings, section 5; spec ads, question 19). */
SETTINGS_BOTTOM("settings_bottom", AdFormat.NATIVE_SMALL),
```

Một dòng, vì `scripts/release.sh` chỉ đọc khai báo một hàng. Sửa chú thích của khối thành "spec ads, questions 1, 2, 8, 9 and 19". `AdsSettings` tự sinh `is_show_settings_bottom` (mặc định `false`) từ khai báo. Không thêm code nào mang tên tính năng vào `core/ads`.

**Màn Cài đặt (`SettingsScreen.kt`).**

- `SettingsRoute` bọc màn:

  ```kotlin
  BottomAdBarScaffold(AdPlacement.SETTINGS_BOTTOM) { padding ->
      SettingsScreen(..., contentPadding = padding)
  }
  ```

  Phần còn lại của Route (sự kiện, `rememberStartOutsideApp`, `PrivacyOptionsButton`) giữ nguyên.
- `SettingsScreen` thêm hai tham số có giá trị mặc định, đặt sau `modifier`:
  - `contentPadding: PaddingValues = PaddingValues()`: chỉ dùng phần đáy, `val bottomReserve = contentPadding.calculateBottomPadding()`;
  - `listScrollState: ScrollState = rememberScrollState()`: trạng thái cuộn của danh sách, để test và screenshot cuộn tới cuối một cách xác định.
- `SettingsList` nhận `bottomReserve` và `scrollState`. Thứ tự modifier của cột: `.fillMaxSize().verticalScroll(scrollState).padding(bottom = bottomReserve).testTag(LIST)`. Lề nằm **bên trong** vùng cuộn, nên nền của màn vẫn kéo tới đáy màn (thẻ quảng cáo nằm trên nền như thiết kế), còn cuộn hết thì dòng cuối dừng trên khe.
- `SnackbarHost`: `Modifier.align(Alignment.BottomCenter).padding(bottom = bottomReserve)`, để thông báo lưu lỗi không nằm dưới khe.
- Hộp chọn giao diện và ngôn ngữ: không đổi. Ở app chúng nằm trong cửa sổ riêng (`dialogInWindow = true`), nên phủ lên cả thanh như thiết kế mục 5. Kiểu vẽ tại chỗ (`dialogInWindow = false`) chỉ dùng cho ảnh chuẩn không có quảng cáo.
- Giá trị mặc định 0 của `contentPadding` và `rememberScrollState()` giữ nguyên mọi ảnh chuẩn và test cũ.

**Bản staging (`StagingRemoteValuesModule.kt`).** `appPlacementsShown()` thêm `AdPlacement.SETTINGS_BOTTOM` với chú thích "The bottom bar of the settings screen (spec ads, question 19)". `is_show_native_bottom` đã bật, `is_show_native_collap` đã tắt từ trước, không đổi.

**Bảng khóa.** Chạy `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`. `docs/remote-config-keys.md` và `docs/remote-config-template.json` thêm `is_show_settings_bottom` (tắt). Không sửa tay.

**Ảnh chuẩn và so thiết kế.**

1. Screenshot test mới vẽ thanh đáy bằng phần không trạng thái của khung, như `PoseDetailScreenScreenshotTest`:

   ```kotlin
   BottomAdBarLayout(
       reservation = BottomAdBarDefaults.nativeReservation,
       slot = { NativeAdCard(SCREENSHOT_AD, NativeAdSize.SMALL, it) },
       layer = null,
   ) { padding -> Column(background) { Spacer(Spacing.lg); SettingsScreen(..., contentPadding = padding) } }
   ```

   `BottomAdBarLayout` bọc ngoài cả cột (thanh trạng thái giả 24 dp và màn), nên khe cách đáy khung 16 như thiết kế (`y = 795`). Tài sản giả như ảnh thiết kế: "Headline of the ad", "Advertiser or short body", "Install", `icon = null`, `hasMedia = false`.
2. Ảnh `SettingsScreen_ad_small` (khung thấp, trạng thái nhiều nội dung nhất): danh sách cuộn tới cuối bằng `listScrollState = remember { ScrollState(Int.MAX_VALUE) }` (ScrollState tự kéo giá trị về `maxValue` khi đo), để thấy dòng "Phiên bản" nằm trên khe. Ở 360 x 640 danh sách chỉ dài 540 (3 tên nhóm 52, 6 dòng 64) trong vùng 576 dưới thanh trên 64, nên không có quảng cáo thì không cuộn; với lề 96 của khe native nó cuộn khoảng 60 dp, với khe banner 50 khoảng 14 dp. Ảnh `_ad_small` có thêm thanh trạng thái giả 24 dp (`Spacer(Spacing.lg)`) ở trên, nên ảnh cuộn 84 dp (252 px, đo bằng test tạm 2026-11-13: `ScrollState(Int.MAX_VALUE)` tự về `maxValue` = 252); dòng "Phiên bản" vẫn kết thúc ở `y = 544`, ngay trên khe. Hàm `Screen` của screenshot test truyền `privacyOptionsEntry = { row -> row {} }` như hôm nay, nên danh sách có đủ 9 phần tử.
3. Ghi ảnh mới:

   ```
   ./gradlew :feature:settings:recordRoborazziDebug :feature:settings:testDebugUnitTest --rerun
   git status --short -- '*/src/test/screenshots/*'
   ```

   `git status` chỉ được có ba ảnh mới của mục 3.2. Ảnh cũ nào đổi vì nhiễu vẽ (lệch 1 mỗi kênh) thì trả bằng `git checkout -- <ảnh>` (`CLAUDE.md`, mục Lệnh).
4. Chạy `scripts/design-compare.sh settings`, commit `docs/features/settings/design/review/`.

**Tài liệu (`docs/ops/manual-test.md`).**

- Mục 9, bước 1: thay "Màn cài đặt mở, không có quảng cáo" bằng "Màn cài đặt mở; có khe quảng cáo native thử ở đáy (mục 11.1)".
- Mục 11, dòng kiểm Firebase staging "Không có ... đặt `false`": thêm `is_show_settings_bottom`.
- Mục 11.1: thêm bước "Mở Cài đặt: khe native thử cao khoảng 80 dp ở đáy, cách mép 16 dp; cuộn hết thì dòng Phiên bản nằm trên khe; mở hộp chọn Giao diện thì hộp phủ lên khe; log `VGL.Ads` có `settings_bottom`".

**Sau khi merge (việc của người, không nằm trong PR).** Chạy `python3 scripts/ads-plan.py --keep <file kế hoạch quảng cáo trên Drive>` để thêm `settings_bottom`. Mã quảng cáo thật của vị trí này thêm vào `release.properties` lúc phát hành (T-075, `docs/ops/release.md`).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | Dòng `SETTINGS_BOTTOM`, chú thích của khối |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementAppTest.kt` | Bảng vị trí của app thêm `settings_bottom` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | Chuỗi cấu hình đủ mã của app thêm `settings_bottom=unit/16` (dữ liệu, ADR-0029; mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsControllerUnitIdsTest.kt` | Hằng `PLACEMENT_UNITS` thêm `settings_bottom=unit/16` (dữ liệu, ADR-0029; mục 3.5) |
| Sửa | `feature/settings/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsScreen.kt` | `BottomAdBarScaffold` ở Route; `contentPadding`, `listScrollState`; lề dưới của danh sách và snackbar |
| Sửa | `feature/settings/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsScreenTest.kt` | Test vị trí của thanh đáy (mục 4) |
| Sửa | `feature/settings/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsScreenScreenshotTest.kt` | Ba ảnh `ad`, hàm `Screen` nhận cờ `ad` và trạng thái cuộn |
| Tạo | `feature/settings/src/test/screenshots/SettingsScreen_ad_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/settings/src/test/screenshots/SettingsScreen_ad_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/settings/src/test/screenshots/SettingsScreen_ad_small.png` | Ảnh chuẩn mới, khung thấp |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModule.kt` | `appPlacementsShown()` thêm `SETTINGS_BOTTOM` |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModuleTest.kt` | Tập mong đợi thêm `is_show_settings_bottom=true` (mục 3.5) |
| Sửa | `docs/remote-config-keys.md` | Sinh lại bằng `-PremoteConfigKeys.update=true` |
| Sửa | `docs/remote-config-template.json` | Sinh lại cùng lệnh |
| Sửa | `docs/features/settings/design/review/` | Ảnh so sánh và `summary.md` do `scripts/design-compare.sh settings` sinh |
| Sửa | `docs/ops/manual-test.md` | Mục 9 bước 1, mục 11 dòng kiểm Firebase, mục 11.1 bước Cài đặt |

Nơi khác đọc file mà plan này đổi:

- `scripts/release.sh` đọc dòng một hàng của `AdPlacement.kt`: dòng mới một hàng, không đổi script. Mã thật của vị trí là việc của người lúc phát hành.
- `scripts/status.py` (mục "Sẵn sàng phát hành") đọc `AdPlacement.kt` theo cùng luật: `settings_bottom` sẽ hiện trong danh sách khóa cần mã thật; đúng ý, không sửa.
- `scripts/ads-plan.py` đọc `AdPlacement` và `docs/remote-config-keys.md`; test của script đọc code thật và không đếm số vị trí cố định (`scripts/tests/run.sh`, khối `ads-plan`), không sửa.
- `app/build.gradle.kts` chỉ nêu đường dẫn của `docs/remote-config-keys.md` và `docs/remote-config-template.json` cho tham số `-PremoteConfigKeys.update`. Đường dẫn không đổi, nên không sửa.
- `scripts/main-guard-verified.sh` coi `docs/remote-config-keys.md` và `docs/remote-config-template.json` là tài liệu Gradle đọc (code). PR này có đổi code Kotlin, nên `main-guard` chạy đủ như mọi PR code; danh sách của script không đổi, không sửa.
- `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` so hai tệp bảng khóa với `AdsSettings.all()` và `AdPlacement.entries`, không ghi tên vị trí; hai tệp được sinh lại trong cùng commit code, nên test qua, không sửa test.
- Các test của `core/ads` khác nhắc `poses_bottom` (`BottomAdBarTest`, `NativeAdRefreshTest`, ...) dùng vị trí cụ thể làm mẫu, không liệt kê đủ vị trí của app; chỉ `AdUnitIdsTest` và `AdsControllerUnitIdsTest` dựng chuỗi "đủ mã của app" nên phải thêm dữ liệu.
- `scripts/check.sh` và `scripts/design-compare.sh` đọc `src/test/screenshots/` và `design/screens/`: hai ảnh thiết kế `ad` có ảnh chuẩn cùng tên ở mục 3.2.
- `app/src/test/.../LeaveAppCallSitesTest.kt` quét chỗ rời app: Route vẫn dùng `rememberStartOutsideApp`, không thêm chỗ rời app mới.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: không có. Vị trí dùng khóa `is_show_settings_bottom` của khung, mặc định tắt (ADR-0014).
- Quảng cáo: thêm `settings_bottom` (native small, thanh đáy) theo câu 19 của `features/ads/spec.md` và mục 5 của `features/settings/spec.md`. Hai khóa chung `is_show_native_bottom`, `is_show_native_collap` không đổi.
- Sự kiện phân tích: không thêm. Khung ghi sự kiện quảng cáo với `ad_placement = settings_bottom`.
- File R2: không chạm.
- **Quyền truy cập:** không thêm.
- **Thư viện và SDK:** không thêm. `BottomAdBarScaffold`, `BottomAdBarLayout`, `BottomAdBarDefaults`, `NativeAdCard`, `NativeAdAssets`, `NativeAdSize` đã có trong `:core:ads`; `:feature:settings` đã dùng `:core:ads` (`PrivacyOptionsButton`, `rememberStartOutsideApp`). `ScrollState` thuộc `androidx.compose.foundation`, đã có trên classpath của module.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `ScrollState(Int.MAX_VALUE)` của ảnh `SettingsScreen_ad_small` có thể không kéo về cuối ở lần vẽ đầu của Roborazzi (ảnh chụp trước khi `maxValue` được đặt).
- **Điều kiện chuyển:** ảnh `SettingsScreen_ad_small` ghi ra vẫn ở đầu danh sách (dòng "Giao diện" ở đỉnh) sau một lần chạy `recordRoborazziDebug`.
- **Cách làm thay:** trong hàm `Screen` của screenshot test, `LaunchedEffect(Unit) { state.scrollTo(state.maxValue) }` với `state = rememberScrollState()`. Test vị trí ở mục 4 vẫn cuộn bằng `scrollTo(maxValue)` như cũ; tiêu chí không đổi.

### 3.5. Hành vi đã có bị đổi

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Màn cài đặt không có quảng cáo | `SettingsRoute` bọc màn trong `BottomAdBarScaffold(SETTINGS_BOTTOM)`; không có controller hay khóa tắt thì khung không vẽ gì, chừa 0 | Không test nào dựng `SettingsRoute`. `SettingsScreenTest` và `SettingsScreenScreenshotTest` dựng `SettingsScreen` | Giữ mọi câu khẳng định: `contentPadding` mặc định 0, `listScrollState` mặc định `rememberScrollState()`, nên vị trí và ảnh cũ không đổi (gồm `small_versionRowReachable`, `landscape_versionRowReachable_*`, `saveFailed_showsSnackbar_thenReports`, bốn ảnh `default`, `theme-picker` và hai ảnh `language-picker`) |
| `SettingsScreen` không có `contentPadding`, `listScrollState` | Hai tham số mới có mặc định, đặt sau `modifier` | Hàm `setScreen` của `SettingsScreenTest`, hàm `Screen` của `SettingsScreenScreenshotTest` gọi theo tên tham số | Giữ: lời gọi theo tên vẫn biên dịch. Test mới truyền hai tham số |
| Tập vị trí của app có bảy vị trí | Tám vị trí, thêm `settings_bottom` (native small) | `AdPlacementAppTest.appPlacements_declaredWithSpecFormats` | Dữ liệu (ADR-0029): bảng mong đợi thêm `"settings_bottom" to AdFormat.NATIVE_SMALL` và `assertEquals(AdPlacement.SETTINGS_BOTTOM, byKey["settings_bottom"])`; các câu kiểm của vòng lặp giữ nguyên |
| Chuỗi "đủ mã của app" cho bản production không còn thiếu vị trí nào | Thiếu `settings_bottom` thì `missing` có nó | `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` (ba chuỗi), `AdUnitIdsTest.missingUnitsWarning_listsMissingKeys`, `AdsControllerUnitIdsTest` (hằng `PLACEMENT_UNITS`) | Dữ liệu (ADR-0029): thêm `settings_bottom=unit/16` vào các chuỗi đủ mã. Câu khẳng định giữ nguyên (`missing` rỗng; thiếu đúng `banner`, `native_collap`; thiếu thêm các vị trí toàn app) |
| Staging bật bảy vị trí của app | Thêm `is_show_settings_bottom=true` | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet` | Dữ liệu (ADR-0029): tập mong đợi thêm `"is_show_settings_bottom" to true`; `noGalleryPlacement_isShown`, `firstOpen_isNotSet`, `firstOpen_choosesNoStartupAd` và các test khác giữ nguyên |
| Các test của khung đi qua mọi vị trí tự gồm vị trí mới | Vị trí mới là native small thường: không toàn app, không chỉ staging, nhóm giãn cách mặc định | `AdFreeGateTest.adFreeMidSession_everyPlacementSwitchOff` (đối chứng `AdPlacement.entries.size`), `AdsSettingsTest` (đếm khóa `>= 14 + số vị trí không toàn app`), `AdPlacementTest.everyPlacement_isInDefaultGroup`, `otherPlacements_areNotStagingOnly`, `AdPlacementSourceTest.releaseScript_readsEveryShippedPlacement` (dòng một hàng), `AdPreloadPlanTest.fromPlacements_*`, `RemoteConfigKeysTest.everyFeatureFlagAndPlacement_isListed` (sau khi sinh lại bảng khóa) | Giữ mọi câu khẳng định; không test nào ghi số vị trí cố định. `existing-tests.py` xếp các test đo vị trí của `core/ads` (`AdSlotTest`, `BannerAdSizeTest`, `InListAdsTest`, `NativeAdCellTest`, `NativeAdSlotTest`, `NativeFullScreenOverlayTest`, `RewardedActionTest`, `PosesScreenTest.posesScreen_liveGridAdsWithoutController_noAdCell`) vào "Cần xem trước" chỉ vì chúng dùng `AdPlacement`; chúng dùng vị trí khác, giữ nguyên. `SettingsScreenScreenshotTest.languagePicker_small` dùng mặc định của hai tham số mới, ảnh không đổi |

## 4. Kiểm thử

Mọi khung dựng của test mới truyền `privacyOptionsEntry = { row -> row {} }` (như `withPrivacyOptions` của `setScreen`), để danh sách có dòng "Tùy chọn quyền riêng tư". Thiếu dòng này (mặc định `privacyOptionsEntry = {}`) danh sách chỉ dài 476, khe native và banner không cuộn, đối chứng của tiêu chí 2 và 3 hỏng, còn câu chính vẫn qua dù lề không chạy.

Số đo ở khung `vi-w360dp-h640dp-xxhdpi` (màn của test không có thanh trạng thái giả): thanh trên 64, danh sách 540 (3 tên nhóm 52, 6 dòng 64, gồm dòng "Tùy chọn quyền riêng tư"), nên dòng "Phiên bản" kết thúc ở `y = 604` và danh sách không cuộn khi không có lề. Khe native giả: `slot.top = 640 - 16 - 80 = 544`; lề 96 làm nội dung dài 636 trong vùng 576, cuộn hết 60, dòng cuối về `y = 544`. Khe banner giả: `slot.top = 590`; lề 50 làm nội dung dài 590, cuộn hết 14, dòng cuối về `y = 590`. So sánh `<=` dùng sai số 0,5. Các số này đã đo bằng test tạm không commit trên nhánh plan (rà soát lại, 2026-11-13): native giả cuộn 60, dòng cuối 544; banner giả cuộn 14, dòng cuối 590; `BARE` và `REAL` dòng cuối 604.

`setContent` chỉ gọi được một lần trong một test, nên mỗi đối chứng đổi cách dựng bằng một `mutableStateOf` đọc trong `setContent` (ví dụ `var padded by mutableStateOf(true)`, rồi `contentPadding = if (padded) padding else PaddingValues()`; hay `var mode by mutableStateOf(Mode.REAL)` chọn giữa khung thật, khe giả và màn không bọc). Sau mỗi lần đổi: `waitForIdle()`, rồi cuộn hết lại. `listScrollState` tạo một lần ngoài nhánh (`val state = ScrollState(0)` ở đầu test), nên đổi cách dựng không tạo trạng thái cuộn mới.

Mọi test vị trí mới vẽ khe giả bằng `BottomAdBarLayout(reservation, slot = { Box(it.height(h).testTag(FAKE_SLOT)) }, layer = null, slotMargin = m) { padding -> SettingsScreen(..., contentPadding = padding, listScrollState = state) }`. Khe giả dùng modifier khung truyền vào. Khung thật mang tag `"bottom_ad_bar_slot"` (đối tượng tag là `internal`, test của feature ghi thẳng chuỗi). Test chạy với `@GraphicsMode(NATIVE)` như lớp hiện có. "Cuộn hết" là `composeRule.runOnIdle { runBlocking { state.scrollTo(state.maxValue) } }` rồi `waitForIdle()`. Hàm phụ trợ dựng khung của test nằm trong commit test của bước 3; test đỏ vì `SettingsScreen` chưa có `contentPadding` và `listScrollState` (lỗi biên dịch của module test, đúng lý do).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Spec ads câu 19: vị trí `settings_bottom` là native small, khóa mặc định tắt | Unit test | `AdPlacementAppTest.appPlacements_declaredWithSpecFormats`: bảng mong đợi có `"settings_bottom" to AdFormat.NATIVE_SMALL`; vòng lặp kiểm không toàn app, không chỉ staging, khóa `is_show_settings_bottom`, mặc định `false`. Đối chứng có sẵn: các vị trí khác của bảng |
| KB-1, tiêu chí 2: cuộn hết thì dòng cuối nằm trên khe native | Robolectric, khung `vi-w360dp-h640dp-xxhdpi` | `SettingsScreenTest.small_nativeBottomBar_lastRowScrollsAboveSlot`: `reservation = BottomAdBarDefaults.nativeReservation`, khe giả cao 80, `slotMargin` mặc định (16). `padded = true`, cuộn hết. Khẳng định: khe cao 80 ± 0,5; `screen.bottom - slot.bottom == 16` ± 0,5; dòng "Phiên bản" có `bottom <= slot.top + 0,5` (đo được khoảng 544 so với 544). Đối chứng trong cùng test: đặt `padded = false`, `waitForIdle()`, cuộn hết lại (lúc này `maxValue` là 0): dòng "Phiên bản" có `bottom > slot.top` (khoảng 604 so với 544) |
| KB-1, tiêu chí 3: khe banner cũng không che dòng cuối | Robolectric, khung `vi-w360dp-h640dp-xxhdpi` | `SettingsScreenTest.small_bannerBottomBar_lastRowScrollsAboveSlot`: `reservation = 50.dp`, khe giả cao 50, `slotMargin = 0.dp` (khe banner không có lề). `padded = true`, cuộn hết. Khẳng định: `slot.bottom == screen.bottom` ± 0,5; dòng "Phiên bản" có `bottom <= slot.top + 0,5` và `slot.top - row.bottom < 1` (khoảng 590 so với 590: lề đúng bằng giá trị khung truyền, không cộng thêm). Đối chứng: `padded = false`, `waitForIdle()`, cuộn hết lại: dòng "Phiên bản" có `bottom > slot.top` (khoảng 604 so với 590) |
| KB-1, tiêu chí 4: lớp nổi thu gọn được, thu gọn thì dòng cuối tới được | Unit test sẵn có của khung | `core/ads` `BottomAdBarTest` (các test lớp thu gọn: lớp không chừa chỗ, thu gọn còn khe native); không sửa. Sau khi thu gọn màn ở trạng thái của tiêu chí 2, đã kiểm ở dòng trên. Thử trên máy thật ở dòng cuối (bật `is_show_native_collap` trên Firebase staging) |
| KB-1, tiêu chí 5: không có quảng cáo thì không có thanh, danh sách dùng cả màn | Robolectric, khung `vi-w360dp-h640dp-xxhdpi` | `SettingsScreenTest.small_realBottomBar_withoutAds_takesNoSpace`, một `mode` ba giá trị. (1) `BARE`: `SettingsScreen(..., listScrollState = state)` không bọc khung, cuộn hết, ghi `bareRow = ` vị trí dòng "Phiên bản" (khoảng `bottom = 604`). (2) `REAL`: `BottomAdBarScaffold(AdPlacement.SETTINGS_BOTTOM) { SettingsScreen(..., contentPadding = it, listScrollState = state) }` thật, không có `LocalAdsController`; cuộn hết. Khẳng định: không có node `"bottom_ad_bar_slot"`; dòng "Phiên bản" có `top`, `bottom` bằng `bareRow` ± 0,5 (khung không chừa gì). Đây là bằng chứng chính: node `LIST` luôn chiếm 64 tới 640 dù có lề hay không, nên câu `LIST.bottom == screen.bottom` (giữ làm câu phụ) không phân biệt được hai trường hợp. (3) Đối chứng `FAKE`: cùng màn trong `BottomAdBarLayout` có khe giả native, cuộn hết: `screen.bottom - row.bottom == 96` ± 0,5 (khoảng 640 - 544) |
| KB-1, tiêu chí 2: thông báo lưu lỗi không nằm dưới khe | Robolectric | `SettingsScreenTest.saveFailedSnackbar_aboveSlot`: `mainClock.autoAdvance = false`, `ready(saveFailed = true)` trong `BottomAdBarLayout` có khe giả native, `advanceTimeByFrame()` như `saveFailed_showsSnackbar_thenReports`. Khẳng định: node chữ "Không lưu được lựa chọn. Hãy thử lại." có `bottom <= slot.top` |
| KB-1, tiêu chí 6: quay lại | Robolectric sẵn có | `SettingsScreenTest.back_callsOnBack`, không sửa |
| Spec ads câu 19, ý a: bản staging bật `is_show_settings_bottom` | Unit test (testStaging) | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet`: tập mong đợi có `"is_show_settings_bottom" to true`, các giá trị khác như cũ (`is_show_native_bottom=true`, `is_show_native_collap=false`). Đối chứng có sẵn: `assertEquals(values.size, byName.size)` |
| Bản production thiếu mã `settings_bottom` thì được báo | Unit test sẵn có của khung | `AdUnitIdsTest.placementWithoutUnit_isReportedMissingAndShowsNoAd` (không sửa); `configuredUnits_areParsedPerPlacementKey` sau khi thêm dữ liệu: chuỗi đủ mã có `missing` rỗng. Đối chứng chạy trên code: ở commit test, chuỗi đã có `settings_bottom=unit/16` nhưng `AdPlacement` chưa có dòng mới, test vẫn qua (khóa thừa không làm hỏng); ở commit code, bỏ khóa đó thì `missing` có `settings_bottom` (kiểm bằng lần chạy tạm, không commit) |
| Đúng thiết kế | Screenshot test cùng tên, ảnh so sánh `scripts/design-compare.sh settings`, rà soát độc lập theo mục 7 của mẫu thiết kế | Khung `vi-w411dp-h891dp-xxhdpi`: `SettingsScreen_ad_light.png`, `SettingsScreen_ad_dark.png` (trạng thái `DEFAULT`, khe native giả, lớp nổi `null`) |
| Màn hình thấp (L-028) | Screenshot test khung `vi-w360dp-h640dp-xxhdpi` | `SettingsScreen_ad_small.png`: có thanh đáy, danh sách cuộn tới cuối, dòng "Phiên bản" nằm trên khe |
| Ảnh không có quảng cáo không đổi | Screenshot test sẵn có | `scripts/check.sh`; `git status --short -- '*/src/test/screenshots/*'` chỉ có ba ảnh mới của mục 3.2 |
| Bảng khóa khớp code | Unit test sẵn có | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode`, `everyPlacementShowKey_isInTableOff` sau khi sinh lại |
| `scripts/ads-plan.py` chạy được | Test của script sẵn có | `scripts/tests/run.sh`, khối `ads-plan` (chạy trong `scripts/check.sh`) |
| Thanh đáy với quảng cáo thử của Google | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging sau khi merge, đã đồng ý quảng cáo. Mở Cài đặt từ camera: khe native thử cao khoảng 80 dp ở đáy, cách mép 16 dp; log `VGL.Ads` có `settings_bottom`. Cuộn hết (máy màn thấp, hay cỡ chữ lớn): dòng "Phiên bản" nằm trên khe. Mở hộp "Giao diện": hộp và lớp mờ phủ lên khe. Đặt `is_show_native_bottom = false` trên Firebase staging, mở lại app: khe là banner ở cùng chỗ, dòng cuối không bị che. Đặt `is_show_native_collap = true`: lớp nổi phủ lên đáy, chạm thu gọn thì còn khe native và dòng cuối tới được. Trả hai khóa về như cũ sau khi thử. Không bấm quảng cáo |
| Mục 9 và 11 của `manual-test.md` đúng vị trí mới | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc mục 9 bước 1, mục 11 dòng kiểm Firebase và mục 11.1 |

**Ngoại lệ viết test trước:**

- Sinh lại `docs/remote-config-keys.md`, `docs/remote-config-template.json` và ảnh so sánh của `design-compare.sh`: tệp sinh tự động, sinh trong commit code.
- Ghi ba ảnh chuẩn `ad`: ảnh chỉ ghi được khi đã có code. Screenshot test vẫn viết trước; ở commit test, module test không biên dịch vì `SettingsScreen` chưa có `contentPadding`, `listScrollState`.
- Phần nối trong `SettingsRoute` (bọc `BottomAdBarScaffold(SETTINGS_BOTTOM)`, truyền `padding`): Route cần `hiltViewModel()`, mà test của feature không dựng Route. Bỏ phần nối thì test JVM vẫn xanh. Phần này được kiểm bằng rà soát độc lập (bước 6, đọc diff của Route) và bằng bước thử trên máy thật. Test `small_realBottomBar_withoutAds_takesNoSpace` kiểm khung thật với đúng vị trí ở cấp màn.
- Ở commit test, chỉ hai test đỏ thật vì phần mới: `AdPlacementAppTest` (lỗi biên dịch, `AdPlacement.SETTINGS_BOTTOM` chưa có) và `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet` (tập mong đợi có `is_show_settings_bottom`, code chưa đặt). Dữ liệu thêm vào `AdUnitIdsTest` và `AdsControllerUnitIdsTest` (`settings_bottom=unit/16`) vẫn xanh ở commit test, vì khóa thừa trong chuỗi không làm hỏng gì; nó chỉ cần để hai test giữ xanh sau commit code (dòng "Bản production thiếu mã" ở bảng trên).
- `docs/ops/manual-test.md`: tài liệu.

### 4.1. Chạy thử

Không cần (E dưới 2). Thanh đáy của khung, `BottomAdBarLayout` và `NativeAdCard` cho ảnh chuẩn đã dùng ở thư viện và chi tiết tư thế, kèm test của khung. Lề dưới bên trong `verticalScroll` là cách dùng thường của Compose.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**

- điểm 1: thêm vị trí quảng cáo `settings_bottom`;
- điểm 7: module core `:core:ads` (một dòng của `AdPlacement`, dữ liệu của hai test).

Mức Cao, cần duyệt chéo. Người phụ trách sản phẩm (Nhật) đã chốt vị trí ở câu 19 của spec ads và duyệt thiết kế ngày 2026-11-13.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module (`:core:ads` một dòng, `:feature:settings`, `:app` staging), thêm hai tham số có mặc định cho một màn |
| B. Độ rõ của yêu cầu | 0 | Spec settings KB-1 và spec ads câu 19 đã xác nhận, có thiết kế đã duyệt, không còn câu hỏi mở |
| C. Khả năng kiểm chứng tự động | 1 | Vị trí, lề, giá trị staging, bảng khóa có test. Quảng cáo thật hiện, lớp nổi và phần nối của Route chỉ kiểm trên máy và bằng rà soát |
| D. Ảnh hưởng chéo | 2 | `AdPlacement.kt`, `StagingRemoteValuesModule.kt`, bảng khóa, `manual-test.md` là file dùng chung với T-094 |
| E. Độ mới của kỹ thuật | 0 | Lặp lại đúng mẫu thanh đáy đã chạy ở thư viện và chi tiết tư thế |
| **Tổng** | 6 | **Mức: Cao (chạm điểm chặn)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdPlacement.kt` của `:core:ads`, `AdPlacementAppTest.kt`, `AdUnitIdsTest.kt`, `AdsControllerUnitIdsTest.kt`, `StagingRemoteValuesModule.kt` và test của nó, bảng khóa `docs/remote-config-keys.md` và `docs/remote-config-template.json`, `docs/ops/manual-test.md` (mục 11.1).
- Task khác có thể xung đột:
  - T-094 (`review_bottom`, `depends_on: [T-093]`) sửa cùng các file trên, thêm dòng `REVIEW_BOTTOM` và khóa của nó. T-093 chạy trước; plan T-094 mô tả các file đó ở trạng thái sau khi T-093 merge.
  - T-096 (`ReviewScreen.kt`) và T-097 (`CameraScreen.kt`): không chung file với plan này.
- Đề xuất thứ tự chạy: T-093 trước T-094, cùng phiên hay phiên trước. T-093 độc lập với T-096, T-097.

## 7. Điều kiện dừng riêng

- Ảnh chuẩn ngoài ba ảnh mới của mục 3.2 đổi hơn mức nhiễu vẽ (lệch quá 1 mỗi kênh, hay đổi bố cục), nhất là bốn ảnh `default`, `theme-picker` và hai ảnh `language-picker`: dừng. `contentPadding` mặc định 0 không được làm đổi ảnh nào.
- Phải sửa `BottomAdBarScaffold`, `BottomAdBarLayout` hay cách khung chọn lớp mới có được hành vi của plan: dừng, vì hành vi chung của khung sẽ đổi theo một màn.
- Bảng khóa sinh lại có thay đổi ngoài dòng `is_show_settings_bottom`: dừng.

## 8. Câu hỏi cho người duyệt

1. **Tham số `listScrollState` mới của `SettingsScreen`.** Plan thêm tham số này chỉ để test và ảnh khung thấp cuộn tới cuối một cách xác định (không vuốt). Đề xuất của AI: chấp nhận; giá trị mặc định là `rememberScrollState()` như hôm nay, Route không truyền.
2. **Ảnh `SettingsScreen_ad_small` cuộn tới cuối.** Thiết kế không có frame khung thấp; ảnh cuộn tới cuối cho thấy điều KB-1 cần (dòng cuối nằm trên khe), còn ảnh ở đầu danh sách giống ảnh `default_small` thêm thanh. Đề xuất của AI: cuộn tới cuối.
3. **Bản phát hành.** `is_show_settings_bottom` mặc định tắt, nên production không hiện thanh tới khi nhóm bật trên Firebase (câu 19, ý a). Khi bật, mã thật của `settings_bottom` phải có trong `release.properties`; thiếu mã thì bản production ghi `Production ad units missing ... settings_bottom` lúc khởi động và khe không có quảng cáo. Đây là việc của người lúc phát hành; plan chỉ ghi nhắc ở mục 3.1. Đồng ý?

## 9. Quyết định duyệt

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm hiện chỉ có một dev, ngoại lệ của `risk-scoring.md` mục 3)
- **Ghi chú:** Nhật duyệt trong phiên có người theo dõi ngày 2026-11-13, lời của Nhật: "duyệt", sau khi AI tóm tắt plan và nói câu hỏi mục 8 theo đề xuất của AI. AI ghi thay. Trả lời câu hỏi ở mục 8:
  1. Chấp nhận tham số `listScrollState` của `SettingsScreen` (mặc định `rememberScrollState()`, Route không truyền), chỉ để test và ảnh khung thấp cuộn tới cuối.
  2. Ảnh `SettingsScreen_ad_small` cuộn tới cuối danh sách.
  3. Đồng ý: mã thật của `settings_bottom` trong `release.properties` là việc của người lúc phát hành; plan chỉ ghi nhắc ở mục 3.1.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). -->

### RUN-20261113-02, phiên bản plan 1

- **Kết quả:** đã merge.
- **Nhánh, PR:** `claude/T-093-thanh-day-cai-dat`, PR #341, commit merge `96a3c22`.
- **Đã làm:** `AdPlacement.SETTINGS_BOTTOM("settings_bottom", AdFormat.NATIVE_SMALL)` (một dòng); `SettingsRoute` bọc màn trong `BottomAdBarScaffold(AdPlacement.SETTINGS_BOTTOM)`; `SettingsScreen` nhận `contentPadding` (lề dưới trong vùng cuộn và cho snackbar) và `listScrollState`; staging bật `settings_bottom`; bảng khóa sinh lại (chỉ thêm `is_show_settings_bottom`); `manual-test.md` mục 9 bước 1, dòng Firebase của mục 11, mục 11.1 bước 9 mới. Bước 3 đến 5 do phiên con làm trong worktree riêng.
- **Test viết trước:** 7 test (commit `02522be`): `small_nativeBottomBar_lastRowScrollsAboveSlot`, `small_bannerBottomBar_lastRowScrollsAboveSlot`, `small_realBottomBar_withoutAds_takesNoSpace`, `saveFailedSnackbar_aboveSlot`, ba screenshot `ad_light`, `ad_dark`, `ad_small`. Thất bại đúng lý do: test của `:feature:settings` và `:core:ads` không biên dịch (thiếu `contentPadding`, `listScrollState`, `SETTINGS_BOTTOM`), `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet` đỏ (thiếu `is_show_settings_bottom`). Dữ liệu `AdUnitIdsTest`, `AdsControllerUnitIdsTest` vẫn xanh ở commit test, như plan ghi. Đột biến sau code: bỏ `settings_bottom=unit/16` thì `AdUnitIdsTest` đỏ; bỏ lề dưới của danh sách thì 3 test `SettingsScreenTest` đỏ.
- **Ảnh chuẩn:** mới `SettingsScreen_ad_light`, `_ad_dark`, `_ad_small` (cuộn tới cuối, dòng "Phiên bản" ngay trên khe). Không ảnh cũ nào đổi. `design-compare.sh settings`: `ad_light`, `ad_dark` 1.4% (khử răng cưa chữ); ảnh cũ không đổi.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 3 ghi chú (`FakeBar` ghi thẳng chuỗi tag; phần nối ở `SettingsRoute` không có test JVM, đã đọc code, khớp mẫu `PosesScreen`, `PoseDetailScreen`).
- **Kết quả CI:** `pr-check` xanh (lần chạy 43454084947), `main-guard` xanh (43456658881). `scripts/check.sh` trên máy qua (3155 test JVM, 0 lỗi; 172 ảnh).
- **Lệch so với plan:** Không có về hành vi; phương án dự phòng 3.4 không dùng (`ScrollState(Int.MAX_VALUE)` chạy được). Khi hoàn tác đột biến, `git checkout` trả `SettingsScreen.kt` chưa commit về bản cũ; phiên con áp lại đúng phần sửa, chạy lại test, `verifyRoborazzi` và `check.sh`, đều xanh. Không có file ngoài mục 3.2.
- **Cần người xem:** thử trên máy thật mục 11.1 bước 9 (thanh đáy ở Cài đặt, khóa bật trên staging; không bấm quảng cáo); ảnh chuẩn `feature/settings/src/test/screenshots/SettingsScreen_ad_*.png`. Mã thật của `settings_bottom` trong `release.properties` lúc phát hành. Sau merge chạy `scripts/ads-plan.py --keep <file trên Drive>`.
