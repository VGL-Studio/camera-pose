---
ticket: T-079
title: Thanh đáy native thay hai banner ở thư viện và chi tiết tư thế; thêm ô quảng cáo trong lưới và interstitial khi chụp với tư thế (tắt lúc phát hành)
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 39.4M        # cỡ L (số gợi ý của scripts/token-usage.py estimate), chia hai lượt cỡ M; không nâng tay
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-079: THANH ĐÁY NATIVE THAY HAI BANNER Ở THƯ VIỆN VÀ CHI TIẾT TƯ THẾ; THÊM Ô QUẢNG CÁO TRONG LƯỚI VÀ INTERSTITIAL KHI CHỤP VỚI TƯ THẾ

## 1. Tóm tắt

Đáy thư viện và chi tiết tư thế chuyển từ hai banner (`poses_banner`, `poses_detail_banner`) sang thanh đáy native nhỏ của khung (`BottomAdBarScaffold`, vị trí `poses_bottom`, `pose_detail_bottom`). App thêm hai vị trí đợt 2, đều tắt lúc phát hành: ô quảng cáo trong lưới thư viện (`poses_in_collection`, sau mỗi 8 tư thế) và interstitial khi bấm "Chụp với tư thế này" (`inter_pose_detail`). Làm trong hai lượt của cùng danh sách chạy, mỗi lượt một PR có CI xanh. Lượt 1 làm thanh đáy, bản staging, bảng khóa và ảnh chuẩn `ad`. Lượt 2 làm ô trong lưới, interstitial và frame `ad-collection`. Kiểm chứng gồm:

- test vị trí bằng Robolectric (khe đáy không che nút chính và thẻ cuối, ô quảng cáo đúng sau mỗi N tư thế);
- screenshot test cùng tên với năm ảnh thiết kế mới;
- test giá trị của bản staging;
- bảng khóa sinh lại;
- thử trên máy thật với quảng cáo thử của Google.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/poses/spec.md` (confirmed 2026-10-30), cùng câu 7 tới 11 của `features/ads/spec.md` (confirmed 2026-10-30):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-4 | Khóa `is_show_inter_pose_detail` bật, có quảng cáo toàn màn hình sẵn sàng, qua nhịp chung `ads_interval` thì quảng cáo hiện sau khi bấm "Chụp với tư thế này"; đóng quảng cáo thì camera mở với đúng tư thế. Không có quảng cáo, chưa đủ nhịp hay khóa tắt thì camera mở ngay, không chờ | Lượt 2. Route chi tiết gọi `rememberShowInterstitial(AdPlacement.INTER_POSE_DETAIL)`, mở camera trong `onFinished`. Khung không chờ tải: không có quảng cáo đã nạp thì `onFinished(false)` chạy ngay. Vị trí không có luật đếm ảnh (`InterstitialBreakSettings.capOf` trả `null`, cổng đếm cho qua mọi lần). Cùng nhóm giãn cách mặc định với `review_interstitial` (`AdIntervalGroup.DEFAULT`). `pose_selected` ghi một lần cho mỗi lần bấm được nhận, trước quảng cáo, như cũ. |
| KB-6, tiêu chí 1 | `is_show_poses_bottom` bật, `is_show_native_bottom` bật, đã đồng ý thì thanh đáy native 80 dp hiện ở đáy thư viện; lưới cuộn phía trên thanh, thẻ cuối không bị che | Lượt 1. `BottomAdBarScaffold(AdPlacement.POSES_BOTTOM)` bọc cả màn. Khoảng chừa ở đáy (96 dp khi khe có quảng cáo, 0 khi không) cộng vào lề dưới của lưới, của nội dung trạng thái lỗi, lưu trống, đang tải và của snackbar. Lưới vẫn kéo tới đáy màn, thẻ trôi sau lề 16 dp quanh khe như thiết kế. |
| KB-6, tiêu chí 2 | `is_show_pose_detail_bottom` bật thì thanh đáy hiện ở đáy chi tiết, dưới nút "Chụp với tư thế này"; nút không bị che hay đẩy ra ngoài màn hình | Lượt 1. `BottomAdBarScaffold(AdPlacement.POSE_DETAIL_BOTTOM)` bọc màn chi tiết. Khoảng chừa thành lề dưới của cột nội dung, nên thanh nút chính (`PoseDetailBottomBar`) nằm ngay trên khe. Bố cục mở gập: khe ngang cả màn ở đáy, cột phải có nút chính nằm trên khe. Khung thấp 360 x 640: nút vẫn thấy trọn. |
| KB-6, tiêu chí 3 (khóa tắt, đang tải, không có thì không chừa chỗ) | Như spec | Khung chỉ chừa chỗ khi khe đã có quảng cáo (D-04). Không có `LocalAdsController` (test) thì không vẽ gì, chừa 0. |
| KB-6, tiêu chí 4 | `is_show_native_collap` tắt (giá trị lúc phát hành) thì không có lớp nổi trên lưới hay nút chính | Không đổi code của lớp. Bản staging đặt `is_show_native_collap` là `false` (câu 11). Production đặt trên Firebase trước khi phát hành (mục 8, câu 2). |
| KB-6, tiêu chí 5 | `is_show_poses_in_collection` bật thì lưới có một ô quảng cáo sau mỗi `native_every_poses_in_collection` tư thế (mặc định 8), không ở đầu, không ở cuối. Thẻ quảng cáo có nhãn, khác rõ thẻ tư thế. Hết quảng cáo thì ô ẩn. Áp dụng cho "Tất cả", từng nhóm, cả khi mở gập (3 cột) | Lượt 2. `rememberInListAds(AdPlacement.POSES_IN_COLLECTION)` ngoài lưới, rồi `itemsWithInListAds(cards, ads, columns)` của `LazyGridScope`: định dạng `native_in_collection` chiếm một ô, N đếm theo tư thế. `keepSpaceWhenPoolEmpty` để mặc định `false` nên pool cạn thì ô ẩn (P-06). Một hàm lưới dùng cho mọi bộ lọc, kể cả "Đã lưu" (mục 8, câu 1). |
| Spec ads, câu 8 | Mã mới `poses_bottom`, `pose_detail_bottom`; bỏ `poses_banner`, `poses_detail_banner` khỏi code; khóa cũ giữ trên Firebase | Lượt 1. Bỏ hai dòng enum, `PosesBottomBanner`, `PosesAdSeparator`, `PosesTestTags.BANNER_SLOT`. Không xóa gì trên Firebase. |
| Spec ads, câu 11 | Staging bật `is_show_poses_bottom`, `is_show_pose_detail_bottom`, `is_show_native_bottom`, `is_show_inter_pose_detail`, `is_show_poses_in_collection`; `is_show_native_collap` là `false`; không bật `is_show_native_after_inter`; có test | Lượt 1 đặt bốn khóa của thanh đáy và lớp thu gọn. Lượt 2 thêm hai khóa đợt 2. `StagingRemoteValuesModuleTest` kiểm cả tập. |

Thiết kế tham chiếu: `features/poses/design/README.md` (mục 4, dòng "Thanh đáy native", "Ô quảng cáo trong lưới"; mục 5). Ảnh trong `design/screens/`: `PosesScreen_ad_light`, `PosesScreen_ad_dark`, `PoseDetailScreen_ad_light`, `PoseDetailScreen_ad_dark`, `PosesScreen_ad-collection_light`, xuất ngày 2026-10-30 (PR #253). Số liệu trong `design/context/`:

- Khe thanh đáy 379 x 80, cách mép trái, phải, đáy 16; nội dung chừa 96.
- Ở chi tiết, thanh nút chính cao 96 nằm ở `y = 699`, khe ở `y = 795`.
- Frame `ad-collection` là lưới cuộn tới tư thế 7: hàng thứ 5 có ô quảng cáo và tư thế 9.

ADR liên quan: ADR-0004, ADR-0013, ADR-0014 (vị trí theo spec, mặc định tắt), ADR-0020 (ảnh thiết kế, screenshot test cùng tên), ADR-0023 (tên khóa), ADR-0026 (khung AdsKit), ADR-0029, ADR-0031, ADR-1007.

## 3. Giải pháp

### 3.1. Cách làm

**Hai lượt.** Danh sách chạy có hai dòng cùng ticket và plan này, ghi chú `lượt 1` và `lượt 2`; lượt 2 phụ thuộc lượt 1. Mỗi lượt đi đủ 10 bước của `docs/auto-run.md`, mục 3, với nhánh và PR riêng. Nhánh chính chạy đúng sau từng lượt.

- **Lượt 1, nhánh `claude/T-079-1-thanh-day`**, từ nhánh chính. Mục tiêu: thanh đáy thay banner, bản staging của thanh đáy, bảng khóa, ảnh chuẩn `ad`.
- **Lượt 2, nhánh `claude/T-079-2-vi-tri-dot-2`**, từ nhánh chính sau lượt 1. Mục tiêu: `poses_in_collection`, `inter_pose_detail`, ảnh chuẩn `ad-collection`, bảng khóa, `manual-test.md`.

#### Lượt 1: thanh đáy native thay banner

**`AdPlacement.kt`.** Ở khối "The placements of this app", thay hai dòng banner bằng:

```kotlin
/** Bottom bar of the pose library (spec poses, section 5; spec ads, question 8). */
POSES_BOTTOM("poses_bottom", AdFormat.NATIVE_SMALL),

/** Bottom bar of the pose detail, below the main button (spec poses, section 5; spec ads, question 8). */
POSE_DETAIL_BOTTOM("pose_detail_bottom", AdFormat.NATIVE_SMALL),
```

Mỗi vị trí một dòng (`scripts/release.sh` đọc dòng một hàng, `AdPlacementSourceTest`). Sửa chú thích của khối thành "spec ads, questions 1, 2 and 8". `AdsSettings` tự sinh `is_show_poses_bottom`, `is_show_pose_detail_bottom` từ khai báo, không sửa.

**Thư viện (`PosesScreen.kt`).**

- `PosesRoute` bọc màn: `BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) { padding -> PosesScreen(..., contentPadding = padding) }`.
- `PosesScreen` bỏ tham số `bottomBanner`, thêm `contentPadding: PaddingValues = PaddingValues()` (chỉ dùng phần đáy). Giá trị mặc định 0 giữ nguyên mọi ảnh chuẩn không có quảng cáo.
- Phần đáy của `contentPadding` (gọi là `bottomReserve`) đi vào:
  - lưới của `ReadyContent`: `gridPadding(bottomReserve)` trả `bottom = Spacing.md + bottomReserve`. Lưới vẫn chiếm hết chiều cao, nên thẻ cuộn sau khe như thiết kế; cuộn hết thì thẻ cuối nằm trên khe, cách 16 dp;
  - `LoadingGrid`: cùng `gridPadding(bottomReserve)`;
  - `CenteredScrollable` của lỗi và lưu trống: `Modifier.padding(bottom = bottomReserve)`;
  - `SnackbarHost`: `Modifier.padding(bottom = bottomReserve)`, để snackbar "lưu không được" không nằm dưới khe.
- Bỏ `PosesTestTags.BANNER_SLOT`.

**Chi tiết (`PoseDetailScreen.kt`, `PoseDetailComponents.kt`).**

- `PoseDetailRoute` bọc màn bằng `BottomAdBarScaffold(AdPlacement.POSE_DETAIL_BOTTOM) { padding -> PoseDetailScreen(..., contentPadding = padding) }`.
- `PoseDetailScreen` bỏ `bottomBanner`, thêm `contentPadding: PaddingValues = PaddingValues()`. Cột ngoài cùng (`Column` dưới thanh tiêu đề) nhận `Modifier.padding(bottom = bottomReserve)`, `SnackbarHost` cũng vậy.
- `PhoneDetailLayout`, `WideDetailLayout` bỏ tham số `bottomBanner` và lời gọi `bottomBanner()` sau `PoseDetailBottomBar`.
- Thanh nút chính giữ đường kẻ trên của nó. Thiết kế tách thanh đáy khỏi nút bằng đường kẻ này, viền và nền của thẻ quảng cáo (README, mục 5).

**`PosesComponents.kt`.** Xóa `PosesBottomBanner` và `PosesAdSeparator`: sau lượt này không còn chỗ dùng ngoài test, và test của lượt 1 bỏ chúng (mục 3.5). Bỏ import `AdSlot`, `AdPlacement` nếu không còn dùng.

**Bản staging (`StagingRemoteValuesModule.kt`).**

- `appPlacementsShown()`: `POSES_BOTTOM`, `POSE_DETAIL_BOTTOM`, `REVIEW_INTERSTITIAL`.
- `globalAdSwitchesOn()`: thêm `AdsSettings.IS_SHOW_NATIVE_BOTTOM`.
- Hàm mới `globalAdSwitchesOff()`, cùng chú thích `@Provides @ElementsIntoSet @LocalRemoteValues`, trả `LocalRemoteValue.of(AdsSettings.IS_SHOW_NATIVE_COLLAP, false)`. Mặc định trong code của khóa này là bật; staging cần giống bản phát hành (câu 10, 11).
- Sửa KDoc: bỏ `is_show_native_bottom` khỏi câu "Not set, so off".

**Bảng khóa.** Chạy `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`. `docs/remote-config-keys.md` và `docs/remote-config-template.json` thêm `is_show_poses_bottom`, `is_show_pose_detail_bottom` và bỏ `is_show_poses_banner`, `is_show_poses_detail_banner`. Không sửa tay.

**Ảnh chuẩn và so thiết kế.** Ghi lại 6 ảnh `ad` của mục 3.2. Lệnh:

```
./gradlew :feature:poses:recordRoborazziDebug :feature:poses:testDebugUnitTest --rerun
git status --short -- '*/src/test/screenshots/'
```

`git status` chỉ được đổi 6 ảnh đó. Rồi chạy `scripts/design-compare.sh poses` và commit `docs/features/poses/design/review/`. Screenshot test vẽ thanh đáy bằng phần không trạng thái của khung:

- `BottomAdBarLayout(reservation = BottomAdBarDefaults.nativeReservation, slot = { NativeAdCard(SCREENSHOT_AD, NativeAdSize.SMALL, it) }, layer = null) { padding -> PosesScreen(..., contentPadding = padding) }`;
- tài sản giả như `BottomAdBarScreenshotTest` của khung: "Headline of the ad", "Advertiser or short body", "Install", `icon = null`, `hasMedia = false`.

**Firebase staging.** Bản 23 đã đặt `is_show_poses_bottom`, `is_show_pose_detail_bottom` là `true` và `is_show_native_collap` là `false` (FB-20261030-01). Việc của người trước khi phát bản staging của lượt 1, ghi vào mục "Kiểm tra trước khi duyệt" của danh sách chạy: xem `is_show_native_bottom` trên Firebase staging không đặt `false`. Nếu khóa này là `false`, thanh đáy ở staging sẽ là banner của khung, không phải native.

#### Lượt 2: ô quảng cáo trong lưới và interstitial khi chụp với tư thế

**`AdPlacement.kt`.** Thêm sau `POSE_DETAIL_BOTTOM`:

```kotlin
/** Ad card in the library grid, one cell after every 8 poses (spec poses, section 5; spec ads, question 9). */
POSES_IN_COLLECTION("poses_in_collection", AdFormat.NATIVE_IN_COLLECTION, nativeEvery = 8),

/** After "Shoot with this pose" in the detail, before the camera opens (spec poses, KB-4; spec ads, question 9). */
INTER_POSE_DETAIL("inter_pose_detail", AdFormat.INTERSTITIAL),
```

`AdsSettings` tự sinh `is_show_poses_in_collection`, `native_every_poses_in_collection` (8), `is_show_inter_pose_detail`. `InterstitialBreakSettings.capOf` không có nhánh cho vị trí mới nên trả `null`, tức không có luật đếm ảnh. Không sửa `InterstitialBreaks.kt`.

**`InListAds.kt` (`:core:ads`): ô quảng cáo cố định cho screenshot.** Feature không tạo được `InListAds` giả (hàm dựng `internal`), và không có controller thì không có ô nào. Vì vậy thêm một hàm công khai, không trạng thái, cùng kiểu với `BottomAdBarLayout` và `NativeAdCard`:

```kotlin
/**
 * Like the grid [itemsWithInListAds], with a fixed ad for screenshots and previews ([NativeAdCard]): an ad after every
 * [every] items of [items] (never first, never last), one cell for [AdFormat.NATIVE_IN_COLLECTION], else a full-width
 * row. The app shows real ads with [itemsWithInListAds].
 */
fun <T> LazyGridScope.itemsWithFixedAds(
    items: List<T>,
    format: AdFormat,
    every: Int,
    columns: Int,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    adContent: @Composable LazyGridItemScope.() -> Unit,
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit,
)
```

Hàm gọi đúng hàm `internal` mà `itemsWithInListAds` của lưới đang gọi. Vị trí lấy từ `gridInListPositions`, kích thước ô từ `gridAdSpansOneCell`, nên luật vị trí chỉ có một chỗ (L-072 của mẫu). Hàm chung cho mọi app, không mang tên tính năng (MC-10); ghi đề xuất về dự án mẫu trong mô tả PR (`docs/ops/from-app.md`).

**Thư viện.**

- Trong `PosesScreen.kt` thêm kiểu `sealed interface PosesGridAds`:
  - `None`;
  - `Live(val ads: InListAds)`;
  - `Fixed(val every: Int, val cell: @Composable (Modifier) -> Unit)`, chỉ cho screenshot và test.
- `PosesScreen` nhận `gridAds: PosesGridAds = PosesGridAds.None`.
- `PosesRoute` gọi `rememberInListAds(AdPlacement.POSES_IN_COLLECTION)` ngoài lưới và truyền `PosesGridAds.Live(ads)`.
- Trong `ReadyContent`, khối `items(state.cards, key = { it.id })` đổi thành một hàm `posesGridItems(cards, gridAds, columns, ...)`:
  - `None`: `items(...)` như cũ;
  - `Live`: `itemsWithInListAds(cards, ads, columns, key = { it.id })`;
  - `Fixed`: `itemsWithFixedAds(cards, AdFormat.NATIVE_IN_COLLECTION, every, columns, key = { it.id }, adContent = { cell(Modifier.testTag(PosesTestTags.AD_CELL)) })`.
- `columns` là `gridColumns()` đang có, tính trong `BoxWithConstraints` (2 hay 3).
- `LoadingGrid` không có ô quảng cáo.
- Thêm `PosesTestTags.AD_CELL = "poses_ad_cell"`. Thẻ quảng cáo thật có `testTag` của khung.

**Giữ chỗ cuộn khi quay lại từ chi tiết (KB-2).** `gridState` lưu chỉ số trong danh sách gộp (tư thế cộng ô quảng cáo). N không đổi thì vị trí giữ đúng. N đổi từ xa, hay quảng cáo vừa được phép giữa hai lần mở, thì lưới có thể lệch vài ô; mục 8, câu 3.

**Chi tiết: interstitial trước camera.**

- Thêm file `detail/ShootWithAd.kt`, trong đó có lớp thuần `internal class ShootGate`:
  - `fun begin(poseId: String): Boolean`: `false` khi đang có một lượt chờ;
  - `fun finish(): String?`: trả tư thế của lượt rồi xóa lượt.
- Cùng file có hàm composable:

```kotlin
/**
 * Returns the action of "Shoot with this pose" (KB-4). The action calls [pick] (the ViewModel logs the choice and
 * returns the pose), asks the interstitial of [placement] at this natural break, then opens the camera with the pose
 * in onFinished, shown or not. A tap while an earlier one waits for its ad is ignored, [pick] not called, so the
 * camera opens once and the choice is logged once.
 */
@Composable
internal fun rememberShootWithAd(
    placement: AdPlacement,
    onOpen: (poseId: String) -> Unit,
): (pick: () -> String?) -> Unit
```

  Bên trong, hàm dùng `remember { ShootGate() }` và `rememberShowInterstitial(placement) { _ -> gate.finish()?.let(currentOnOpen) }`. Hàm trả về `{ pick -> if (!gate.waiting) pick()?.let { id -> if (gate.begin(id)) showAd() } }`. `ShootGate` có thêm `val waiting: Boolean`. `rememberShowInterstitial` chạy `onFinished` trên luồng chính (`Dispatchers.Main.immediate`, L-030), một lần mỗi lần gọi.
- `PoseDetailRoute`: `val shoot = rememberShootWithAd(AdPlacement.INTER_POSE_DETAIL, onShootWithPose)` và `onShoot = { shoot(viewModel::onShoot) }`. `PoseDetailViewModel` không đổi.
- Ra nền giữa chừng (L-091 của mẫu): không có bước chờ có giới hạn. Khung chỉ hiện quảng cáo đã nạp và chỉ khi activity đang resumed. Nếu không hiện được, `onFinished(false)` chạy ngay và camera mở. Nếu quảng cáo đang hiện, `onFinished(true)` chạy sau khi đóng.
- Cổng đếm (`LocalInterstitialBreakGate`) đếm lần dừng của `inter_pose_detail` riêng và cho qua mọi lần (`BreakCap.NONE`). Nhịp `ads_interval` chung với `review_interstitial` vì cùng `AdIntervalGroup.DEFAULT`.

**Bản staging.** `appPlacementsShown()` thêm `POSES_IN_COLLECTION`, `INTER_POSE_DETAIL`.

**Bảng khóa.** Sinh lại như lượt 1. Bảng thêm `is_show_poses_in_collection`, `native_every_poses_in_collection` (8) và `is_show_inter_pose_detail`.

**Ảnh chuẩn và so thiết kế.**

- Ảnh mới `PosesScreen_ad-collection_light.png`, khung chuẩn, sáng.
- Lưới vẽ với `rememberLazyGridState(initialFirstVisibleItemIndex = 6)`, 12 tư thế, `PosesGridAds.Fixed(every = 8) { NativeAdCard(SCREENSHOT_CARD_AD, NativeAdSize.CARD, it) }`, không có thanh đáy (README, mục 3, câu về frame này).
- Tài sản giả: như ảnh thiết kế ("Headline of the ad", "Advertiser or short body", "Install", `hasMedia = true`).
- Chạy lại `scripts/design-compare.sh poses`.

**Tài liệu.** `docs/ops/manual-test.md` mục 11 (Quảng cáo) gồm ba việc:

- thay bước banner ở thư viện và chi tiết bằng bước thanh đáy native (khe 80 dp, nút chính không bị che, không có lớp nổi);
- thêm bước ô trong lưới (sau tư thế 8, ở "Tất cả" và một nhóm);
- thêm bước interstitial ở chi tiết (bấm "Chụp với tư thế này", đóng quảng cáo thì camera mở đúng tư thế).

**Sau khi merge lượt 2 (việc của người, không trong PR).** Chạy `python3 scripts/ads-plan.py --keep <file v2 trên Drive>` và đổi trạng thái các dòng thành `Đã code`. Danh sách mã cần tạo của T-075 có bốn vị trí mới và không còn hai banner.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | Lượt 1: `POSES_BOTTOM`, `POSE_DETAIL_BOTTOM` thay hai banner. Lượt 2: `POSES_IN_COLLECTION`, `INTER_POSE_DETAIL` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InListAds.kt` | Lượt 2: `LazyGridScope.itemsWithFixedAds` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementAppTest.kt` | Lượt 1 và 2: tập vị trí của app (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | Lượt 1 và 2: chuỗi cấu hình mã đơn vị có mã của vị trí mới, bỏ hai banner (dữ liệu, ADR-0029) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdSlotTest.kt` | Lượt 1: `POSES_BANNER` thành `GALLERY_BANNER`, vị trí banner còn lại (dữ liệu) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InterstitialBreaksTest.kt` | Lượt 1: `POSES_BANNER` thành `POSES_BOTTOM` (dữ liệu). Lượt 2: `interPoseDetail_hasNoBreakCap` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/ShowInterstitialTest.kt` | Lượt 1: `POSES_BANNER` thành `POSES_BOTTOM` (vị trí không phải interstitial, dữ liệu) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InListAdsTest.kt` | Lượt 2: test của `itemsWithFixedAds` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreen.kt` | Lượt 1: `BottomAdBarScaffold`, `contentPadding`, bỏ `bottomBanner`, `BANNER_SLOT`. Lượt 2: `PosesGridAds`, `rememberInListAds`, `AD_CELL` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesComponents.kt` | Lượt 1: xóa `PosesBottomBanner`, `PosesAdSeparator` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreen.kt` | Lượt 1: `BottomAdBarScaffold`, `contentPadding`, bỏ `bottomBanner`. Lượt 2: `rememberShootWithAd` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailComponents.kt` | Lượt 1: `PhoneDetailLayout`, `WideDetailLayout` bỏ `bottomBanner` |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/ShootWithAd.kt` | Lượt 2: `ShootGate`, `rememberShootWithAd` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenTest.kt` | Lượt 1: test thanh đáy thay test banner (mục 3.5). Lượt 2: test ô trong lưới |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenScreenshotTest.kt` | Lượt 1: ảnh `ad` vẽ `BottomAdBarLayout` và `NativeAdCard`, bỏ `TestBanner`. Lượt 2: ảnh `ad-collection` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenTest.kt` | Lượt 1: test thanh đáy thay test banner (mục 3.5) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenScreenshotTest.kt` | Lượt 1: ảnh `ad` vẽ `BottomAdBarLayout` và `NativeAdCard`, bỏ `TestBanner` |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/ShootWithAdTest.kt` | Lượt 2: mục 4 |
| Sửa | `feature/poses/src/test/screenshots/PosesScreen_ad_light.png` | Lượt 1: ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PosesScreen_ad_dark.png` | Lượt 1: ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PosesScreen_ad_small.png` | Lượt 1: ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_light.png` | Lượt 1: ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_dark.png` | Lượt 1: ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_small.png` | Lượt 1: ghi lại |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_ad-collection_light.png` | Lượt 2: ảnh mới |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModule.kt` | Lượt 1: thanh đáy, `is_show_native_bottom`, `globalAdSwitchesOff`. Lượt 2: hai vị trí đợt 2 |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModuleTest.kt` | Lượt 1 và 2: tập giá trị mong đợi (mục 3.5, mục 4) |
| Sửa | `docs/remote-config-keys.md` | Lượt 1 và 2: sinh lại bằng `-PremoteConfigKeys.update=true` |
| Sửa | `docs/remote-config-template.json` | Lượt 1 và 2: sinh lại cùng lệnh |
| Sửa | `docs/features/poses/design/review/` | Lượt 1 và 2: ảnh so sánh và `summary.md` do `scripts/design-compare.sh poses` sinh |
| Sửa | `docs/ops/manual-test.md` | Lượt 2: mục 11 |

Nơi khác đọc file plan này đổi:

- `scripts/release.sh` đọc dòng một hàng của `AdPlacement.kt`. Bốn dòng mới đều một hàng, không đổi script; mã thật của bốn vị trí là việc của T-075.
- `scripts/ads-plan.py` đọc `AdPlacement` và `docs/remote-config-keys.md`. Test của script đọc code thật (`scripts/tests/run.sh`, khối `ads-plan`), không đếm số vị trí cố định, nên không sửa.
- `app/build.gradle.kts` chỉ nêu đường dẫn của `docs/remote-config-keys.md` và `docs/remote-config-template.json` cho tham số `-PremoteConfigKeys.update`. Đường dẫn không đổi, nên không sửa.
- `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` so hai tệp đó với khóa của code. Tập khóa lấy từ `AdsSettings.all()` và `AdPlacement.entries`, không ghi tên vị trí, nên không sửa test. Hai tệp sinh lại trong cùng commit code thì test qua.
- Test sẵn có không dùng banner mà `scripts/existing-tests.py` xếp "Cần xem trước" giữ nguyên câu khẳng định. Các test này dựng màn không có quảng cáo, `contentPadding` mặc định 0:
  - `PoseDetailScreenTest.smallScreen_bottomBarAlwaysVisible_lastMistakeScrollsAboveIt`;
  - `wide_imageLeft_infoRight_bottomBarInRightColumn`;
  - `wide_infoScrolls_bottomBarStaysVisible`;
  - `landscape_bottomBarVisible`;
  - `PosesScreenTest.scrollPosition_survivesStateRestore`;
  - ảnh `default_*_unfolded`;
  - các test của `InListAdsTest`, `AdSlotTest`, `NativeAdCellTest` mà plan không nêu.
- `scripts/check.sh` và `design-compare.sh` đọc `src/test/screenshots/` và `design/screens/`. Mỗi ảnh thiết kế đều có screenshot test cùng tên sau lượt 2. Sau lượt 1, `PosesScreen_ad-collection_light` chưa có ảnh chuẩn: `design-compare.sh` chỉ ghi "thiếu ảnh chuẩn screenshot" như hiện nay. `scripts/plan-check.py` được báo bằng dòng "Ảnh thiết kế của lượt 2" ở mục 4.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: không có. Bốn vị trí theo khóa `is_show_<vị trí>` của khung, mặc định tắt (ADR-0014).
- Quảng cáo:
  - thêm `poses_bottom`, `pose_detail_bottom` (native small, thanh đáy), `poses_in_collection` (native in collection, N = 8), `inter_pose_detail` (interstitial, nhóm mặc định);
  - bỏ `poses_banner`, `poses_detail_banner`;
  - tất cả theo `features/ads/spec.md` câu 8, 9 và `features/poses/spec.md` mục 5.
- Sự kiện phân tích: không thêm. Khung ghi sự kiện quảng cáo với `ad_placement` là mã vị trí mới. `pose_selected` giữ nguyên.
- File R2: không chạm.
- **Quyền truy cập:** không thêm.
- **Thư viện và SDK:** không thêm. `BottomAdBarScaffold`, `rememberInListAds`, `itemsWithInListAds`, `rememberShowInterstitial`, `NativeAdCard`, `BottomAdBarLayout` có trong `:core:ads` từ T-074; `:feature:poses` đã phụ thuộc `:core:ads`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** lấy phần đáy của `PaddingValues` trong `PosesScreen` cần `LayoutDirection` (`calculateBottomPadding()` không cần, nên dự kiến chạy). Ngoài ra, `BottomAdBarScaffold` bọc ngoài `PoseDetailScreen` có thể làm lệch đo của các test vị trí sẵn có, nếu khung thêm một tầng `Box` có `testTag` khác.
- **Điều kiện chuyển:** test vị trí của mục 4 đỏ vì cùng một lỗi đo sau 2 lần sửa.
- **Cách làm thay:** test vị trí đo bằng `BottomAdBarLayout` với khe giả 80 dp (khung dùng cùng phần bố cục cho cả hai), thay cho khung thật. Tiêu chí và câu khẳng định giữ nguyên.

### 3.5. Hành vi đã có bị đổi

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Banner 58 dp nằm dưới lưới, lưới ngắn lại (`PosesScreen` đặt `bottomBanner()` sau lưới trong `Column`) | Khe 80 dp đè ở đáy, cách mép 16; lưới kéo tới đáy màn, lề dưới cộng 96 | `PosesScreenTest.banner_sitsBelowGrid_lastCardScrollsAboveIt` | Thay bằng `bottomBar_lastCardScrollsAboveSlot`: cuộn tới thẻ cuối, `lastCard.bottom <= slot.top`; khe cao 80, cách đáy 16. Ý định giữ: thẻ cuối không bị quảng cáo che |
| Banner hiện cả ở lỗi và lưu trống | Khe hiện ở mọi trạng thái (khung bọc cả màn); nội dung trạng thái chừa chỗ | `PosesScreenTest.banner_shownAlsoOnErrorAndSavedEmpty` | Thay bằng `bottomBar_onErrorAndSavedEmpty_actionAboveSlot`: khe có mặt, nút hành động của `StateMessage` có `bottom <= slot.top` |
| Banner thật không có quảng cáo thì chiếm 0 | Khung thật không có controller thì không vẽ khe, chừa 0 | `PosesScreenTest.realBottomBanner_withoutAds_takesNoSpace`, `PoseDetailScreenTest.realBottomBanner_withoutAds_takesNoSpace` | Thay `PosesBottomBanner(...)` bằng `BottomAdBarScaffold(POSES_BOTTOM)` / `(POSE_DETAIL_BOTTOM)`. Câu giữ: không có node của khe; thẻ (chi tiết: `BOTTOM_BAR`) chạm đáy màn như khi không có quảng cáo |
| Banner nằm dưới thanh nút ở chi tiết | Khe đè ở đáy, cột nội dung có lề dưới 96, nên thanh nút nằm trên khe | `PoseDetailScreenTest.banner_belowBottomBar_buttonNotCovered`, `smallScreen_withBanner_buttonFullyVisible`, `wide_banner_belowBottomBar_inRightColumn` | Đổi tên thành `bottomBar_slotBelowButtonBar_buttonNotCovered`, `smallScreen_withBottomBar_buttonFullyVisible`, `wide_bottomBar_slotFullWidth_buttonAbove`. Câu: `buttonBar.bottom <= slot.top`; ở khung thấp, nút nằm trọn trong màn và trên khe; khi mở gập, khe rộng bằng màn trừ 2 x 16, nút cột phải nằm trên khe |
| Staging bật `is_show_poses_banner`, `is_show_poses_detail_banner`; không đặt `is_show_native_bottom` | Bật `is_show_poses_bottom`, `is_show_pose_detail_bottom`, `is_show_native_bottom`; đặt `is_show_native_collap = false` | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet`, `firstOpenAndNativeCombos_areNotSet` | Tập mong đợi đổi theo mục 4. Câu cũ "`is_show_native_bottom` không đặt" trái câu 11 của spec ads: viết lại theo ADR-0031 thành `firstOpenAndCombo_areNotSet` (hai khóa `show_open_ads_first_open`, `is_show_native_after_inter`, giữ đối chứng) cộng `nativeBottomOn_collapOff` (mục 4). Ý định giữ: staging không bật gì ngoài điều spec chốt |
| Tập vị trí của app là ba vị trí, có hai banner | Năm vị trí sau lượt 2, không còn banner | `AdPlacementAppTest` (dòng 16, 17, 30, 31) | Dữ liệu (ADR-0029): bảng mong đợi đổi theo mục 4, thêm câu "không còn `poses_banner`, `poses_detail_banner`" |
| `POSES_BANNER` dùng làm mẫu "vị trí không phải interstitial" và "vị trí không có luật đếm" | `POSES_BOTTOM` thay vai trò đó | `InterstitialBreaksTest.placements_countSeparately`, `InterstitialBreakSettings` (dòng 101), `ShowInterstitialTest` (dòng 38), `AdSlotTest` (dòng 53, 124: `GALLERY_BANNER`), `AdUnitIdsTest` (dòng 15, 22) | Dữ liệu (ADR-0029), câu khẳng định giữ nguyên |
| `PosesScreen`, `PoseDetailScreen` nhận `bottomBanner` | Nhận `contentPadding`; thư viện nhận thêm `gridAds` ở lượt 2 | Hàm `setScreen` của `PosesScreenTest`, `PoseDetailScreenTest`; `Screen(...)` của hai lớp screenshot test | Phần dựng của test đổi theo chữ ký mới. Ảnh không quảng cáo không đổi (`contentPadding` mặc định 0) |
| Bấm "Chụp với tư thế này" mở camera ngay | Qua `rememberShootWithAd`: không có controller hay không có quảng cáo thì mở ngay; có quảng cáo thì mở sau khi đóng | `PoseDetailScreenTest` (các test bấm nút gọi `onShoot`), `PoseDetailViewModelTest` (`onShoot` ghi `pose_selected`) | Giữ: `PoseDetailScreen` vẫn gọi `onShoot` một lần mỗi lần bấm; ViewModel không đổi. Phần mới ở `ShootWithAdTest` |

`scripts/existing-tests.py` đã chạy trên plan này; kết quả ở mô tả PR của plan.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Spec ads câu 8, lượt 1: hai vị trí thanh đáy là native small, không còn banner | Unit test | `AdPlacementAppTest`. Bảng mong đợi: `poses_bottom` và `pose_detail_bottom` là `NATIVE_SMALL`, `review_interstitial` là `INTERSTITIAL`. `bannersRemoved`: `AdPlacement.entries` không có mã `poses_banner`, `poses_detail_banner`. Đối chứng: `review_interstitial` vẫn có |
| Spec ads câu 9, lượt 2: hai vị trí đợt 2 | Unit test | `AdPlacementAppTest` thêm vào bảng: `poses_in_collection` là `NATIVE_IN_COLLECTION`, `nativeEvery == 8`, `keepSpaceWhenPoolEmpty == false`; `inter_pose_detail` là `INTERSTITIAL`, `intervalGroup == AdIntervalGroup.DEFAULT == REVIEW_INTERSTITIAL.intervalGroup`, không `appWide`, không `ignoreInterval`, `recordInterval` |
| KB-4: `inter_pose_detail` không có luật đếm ảnh; `review_interstitial` giữ luật | Unit test (lượt 2) | `InterstitialBreaksTest.interPoseDetail_hasNoBreakCap`: `capOf(INTER_POSE_DETAIL)` là `null`; cổng với `FakeRemoteSettings` cho qua lần dừng 1, 2, 3 của `INTER_POSE_DETAIL`. Đối chứng: cùng cổng, lần 1 của `REVIEW_INTERSTITIAL` là `null`. Test sẵn có của luật đếm ảnh giữ nguyên câu khẳng định |
| KB-6, tiêu chí 1: lưới cuộn phía trên thanh, thẻ cuối không bị che | Robolectric, `@GraphicsMode(NATIVE)` (lượt 1) | `PosesScreenTest.bottomBar_lastCardScrollsAboveSlot`: `BottomAdBarLayout(reservation = BottomAdBarDefaults.nativeReservation, slot = { Box(Modifier.height(80.dp).testTag(SLOT)) }, layer = null) { PosesScreen(ready(manyCards()), contentPadding = it) }`; `performScrollToIndex(last)`; thẻ cuối `bottom <= slot.top`; khe cao 80 ± 0,5, `screen.bottom - slot.bottom == 16` ± 0,5. Đối chứng: `contentPadding` 0 thì thẻ cuối có `bottom > slot.top` (che) |
| KB-6, tiêu chí 1: lỗi và lưu trống không bị che | Robolectric (lượt 1) | `PosesScreenTest.bottomBar_onErrorAndSavedEmpty_actionAboveSlot`: trạng thái `Error` rồi lưu trống; khe có mặt, nút hành động có `bottom <= slot.top` |
| KB-6, tiêu chí 1: snackbar không nằm dưới khe | Robolectric (lượt 1) | `PosesScreenTest.saveFailedSnackbar_aboveSlot`: `ready(saveFailed = true)` với khe giả; node snackbar có `bottom <= slot.top` |
| KB-6, tiêu chí 2: nút chính ở chi tiết không bị che | Robolectric (lượt 1) | `PoseDetailScreenTest.bottomBar_slotBelowButtonBar_buttonNotCovered`: `BOTTOM_BAR.bottom <= slot.top`, khoảng cách bằng 0 ± 0,5 (khe ngay dưới thanh nút như thiết kế: 699 + 96 = 795) |
| KB-6, tiêu chí 2: khung thấp (L-028) | Robolectric, khung `w360dp-h640dp-xxhdpi` (lượt 1) | `PoseDetailScreenTest.smallScreen_withBottomBar_buttonFullyVisible`: nội dung dài; nút "Chụp với tư thế này" có `top >= 0`, `bottom <= slot.top`, chiều cao như khi không có khe |
| KB-6, tiêu chí 2: mở gập | Robolectric, rộng 673 (lượt 1) | `PoseDetailScreenTest.wide_bottomBar_slotFullWidth_buttonAbove`: khe rộng `673 - 32`; `BOTTOM_BAR` ở cột phải có `bottom <= slot.top` |
| KB-6, tiêu chí 3: không có quảng cáo thì không chừa chỗ | Robolectric (lượt 1) | `PosesScreenTest.realBottomBar_withoutAds_takesNoSpace`, `PoseDetailScreenTest.realBottomBar_withoutAds_takesNoSpace`: `BottomAdBarScaffold` thật, không có `LocalAdsController`; không có node `bottom_ad_bar_slot`; lưới (chi tiết: `BOTTOM_BAR`) chạm đáy màn ± 0,5. Đối chứng: cùng màn trong `BottomAdBarLayout` với khe giả thì phần tử đó cách đáy 96 |
| KB-6, tiêu chí 4: lớp thu gọn tắt thì không có lớp nổi | Unit test sẵn có của khung | `BottomAdBarTest.layers_collapOff_keepsNativeSlot_noLayer` (không sửa); staging đặt khóa này `false` ở test dưới |
| Spec ads câu 11, lượt 1: giá trị staging của thanh đáy | Unit test (testStaging) | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet`: tập của ba hàm đúng bằng `{is_show_poses_bottom=true, is_show_pose_detail_bottom=true, is_show_review_interstitial=true, show_open_ads=true, show_resum_ads=true, is_show_native_bottom=true, is_show_native_collap=false, is_log_ad_fullscreen_events=true, is_log_ad_slot_events=true}`. `nativeBottomOn_collapOff`: qua `localOnly`, `is_show_native_bottom` đọc `true`, `is_show_native_collap` đọc `false`; đối chứng: `IS_SHOW_NATIVE_COLLAP.default` là `true` (giá trị cục bộ mới làm nó tắt). `firstOpenAndCombo_areNotSet`: `show_open_ads_first_open`, `is_show_native_after_inter` không có trong tập. `noGalleryNorSamplePlacement_isShown` giữ nguyên |
| Spec ads câu 11, lượt 2: hai vị trí đợt 2 bật ở staging | Unit test (testStaging) | `localValues_areExactlyTheAppSet` đổi tập mong đợi ở commit test của lượt 2, cộng `is_show_poses_in_collection=true`, `is_show_inter_pose_detail=true` |
| KB-6, tiêu chí 5: ô sau mỗi 8 tư thế, một ô, không đầu, không cuối | Robolectric (lượt 2) | `PosesScreenTest.gridAds_afterEveryEight_oneCell`: 17 thẻ, `PosesGridAds.Fixed(8) { Box(it.size(10.dp)) }`, 2 cột. `AD_CELL` có `top == card(9).top` (cùng hàng, cột trái) và `top >= card(8).bottom`; rộng bằng một thẻ ± 0,5. Chỉ một ô, vì vị trí 16 là cuối danh sách gộp nên không có ô thứ hai |
| KB-6, tiêu chí 5: không ở cuối | Robolectric (lượt 2) | `PosesScreenTest.gridAds_exactlyEightCards_noAdCell`: 8 thẻ, `Fixed(8)`: không có `AD_CELL`. Đối chứng: 9 thẻ thì có |
| KB-6, tiêu chí 5: mở gập 3 cột | Robolectric, rộng 673 (lượt 2) | `PosesScreenTest.wide_gridAds_afterEight_oneCellOfThree`: 17 thẻ, ô quảng cáo rộng bằng một thẻ, nằm sau thẻ 8 (cùng hàng với thẻ 7 và 8: hàng thứ 3 gồm thẻ 7, 8 rồi ô) |
| KB-6, tiêu chí 5: từng nhóm | Robolectric (lượt 2) | `PosesScreenTest.gridAds_categorySelected_sameRule`: `ready(selected = Category(...), cards = 9 thẻ)`: có một `AD_CELL` sau thẻ 8 |
| KB-6, tiêu chí 5: hết quảng cáo thì ô ẩn; không có controller thì không có ô | Unit và Robolectric sẵn có của khung | `NativeAdCellTest.exhausted_takesNoSpace`, `InListAdsTest.publicApi_withoutAdsController_addsNoAdRow` (không sửa); `AdPlacementAppTest` kiểm `keepSpaceWhenPoolEmpty == false` |
| `itemsWithFixedAds` dùng đúng luật vị trí của khung | Robolectric (lượt 2) | `InListAdsTest.fixedGridAds_card_oneCellAfterEveryN`: `LazyVerticalGrid` 2 cột, 9 mục, `NATIVE_IN_COLLECTION`, `every = 4`. Ô sau mục 4 và mục 8, mỗi ô rộng một cột; không có ô thứ ba vì sau mục 9 là cuối. `fixedGridAds_small_fullWidthRow`: `NATIVE_SMALL` thì ô rộng bằng lưới. Đối chứng: vị trí trùng `gridInListPositions` cho cùng tham số |
| KB-4: không có quảng cáo hay khóa tắt thì camera mở ngay, một lần, đúng tư thế | Robolectric (lượt 2) | `ShootWithAdTest.noAdsController_opensAtOnce`: `rememberShootWithAd(INTER_POSE_DETAIL) { opened += it }`, không có `LocalAdsController`; gọi `shoot { "pose_1" }` rồi `waitForIdle`: `opened == ["pose_1"]` |
| KB-4: bấm trong lúc chờ quảng cáo thì bỏ qua; `pose_selected` không ghi lần thừa | Unit test (lượt 2) | `ShootWithAdTest.gate_secondBeginWhileWaiting_isIgnored`: `begin("a")` là `true`, `begin("b")` là `false`; `finish()` trả `"a"`, lần sau trả `null`; `begin("c")` lại `true`. `pickNotCalled_whileWaiting`: lambda `pick` đếm số lần gọi, lần bấm thứ hai trong lúc chờ không gọi |
| KB-4: có quảng cáo thì camera mở sau khi đóng | Robolectric sẵn có của khung, cộng kiểm tra thủ công | `ShowInterstitialTest` của khung (thứ tự `onFinished(true)` sau `onClosed`, không sửa); máy thật ở dưới |
| Đúng thiết kế | Screenshot test cùng tên, ảnh so sánh `scripts/design-compare.sh poses`, rà soát độc lập theo mục 7 của mẫu thiết kế | Lượt 1: `PosesScreen_ad_light.png`, `PosesScreen_ad_dark.png`, `PoseDetailScreen_ad_light.png`, `PoseDetailScreen_ad_dark.png` (khung `w411dp-h891dp-xxhdpi`). Lượt 2: `PosesScreen_ad-collection_light.png`. Ảnh thiết kế của lượt 2 (T-079, lượt 2): `PosesScreen_ad-collection_light` |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` (lượt 1) | `PosesScreen_ad_small.png`, `PoseDetailScreen_ad_small.png`: có thanh đáy, nội dung nhiều nhất |
| Ảnh không quảng cáo không đổi | Screenshot test sẵn có | `scripts/check.sh` sau mỗi lượt: `git status --short -- '*/src/test/screenshots/'` chỉ đổi ảnh của mục 3.2 |
| Bảng khóa khớp code | Unit test sẵn có | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode`, `everyPlacementShowKey_isInTableOff` sau khi sinh lại ở mỗi lượt; bảng có `native_every_poses_in_collection` mặc định 8 (lượt 2) |
| `scripts/ads-plan.py` chạy được | Test của script sẵn có | `scripts/tests/run.sh`, khối `ads-plan` qua sau lượt 2 |
| Thanh đáy, ô trong lưới, interstitial với quảng cáo thử của Google | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging sau lượt 2, đã đồng ý quảng cáo. Thư viện: khe native ở đáy, cuộn hết thì thẻ cuối nằm trên khe, không có lớp nổi. "Tất cả" và một nhóm có ô quảng cáo sau tư thế 8. Chi tiết: khe dưới nút chính, nút không bị che. Bấm "Chụp với tư thế này": interstitial thử, đóng thì camera mở đúng tư thế; bấm lại trong 45 giây thì camera mở ngay. Log `VGL.Ads` có `ad_placement` là mã mới. Không bấm quảng cáo |
| Firebase staging không ghi đè trái ý | Kiểm tra thủ công (máy thật: Remote Config) | Trước khi phát staging của lượt 1, người xem `is_show_native_bottom` trên Firebase staging không đặt `false` (mục 3.1, "Firebase staging"); ghi vào danh sách chạy |
| Mục 11 của `manual-test.md` đúng các vị trí mới | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR lượt 2 đọc mục 11 |

**Ngoại lệ viết test trước:**

- Sinh lại `docs/remote-config-keys.md`, `docs/remote-config-template.json` và ảnh so sánh của `design-compare.sh`: tệp sinh tự động, sinh trong commit code.
- Ghi lại 6 ảnh chuẩn `ad` (lượt 1) và tạo ảnh `ad-collection` (lượt 2): ảnh chuẩn chỉ ghi được khi có code. Screenshot test vẫn viết trước; ở commit test, test đỏ vì `contentPadding`, `PosesGridAds` chưa có.
- `docs/ops/manual-test.md`: tài liệu.

### 4.1. Chạy thử

Không cần (E dưới 2). Thanh đáy, in-list của lưới, interstitial theo lần dừng và các hàm không trạng thái cho screenshot đều có trong `:core:ads` từ T-074, kèm test của khung. `itemsWithFixedAds` chỉ gói lại hàm `internal` đang có.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**

- 1: thêm, bớt và di chuyển vị trí quảng cáo, đổi định dạng ở hai màn;
- 7: module core `:core:ads` (bốn dòng của `AdPlacement`, một hàm công khai mới).

Mức Cao, cần duyệt chéo. Người phụ trách sản phẩm (Nhật) đã chốt các vị trí ở spec ads câu 7 tới 11 ngày 2026-10-30.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module (`:core:ads`, `:feature:poses`, `:app`), đổi chữ ký công khai của hai màn |
| B. Độ rõ của yêu cầu | 1 | Hai spec đã xác nhận, có thiết kế; còn câu 1 (ô ở "Đã lưu") và câu 3 (lệch chỗ cuộn) ở mục 8 |
| C. Khả năng kiểm chứng tự động | 1 | Vị trí, giá trị staging, luật đếm có test; quảng cáo thật hiện và đóng chỉ kiểm trên máy |
| D. Ảnh hưởng chéo | 2 | `AdPlacement.kt` và bảng khóa là file dùng chung với T-075 (danh sách mã thật) |
| E. Độ mới của kỹ thuật | 1 | Lần đầu một feature của app dùng thanh đáy và in-list trong lưới của khung, nhưng theo đúng API và test của khung |
| **Tổng** | 8 | **Mức: Cao (chạm điểm chặn)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `AdPlacement.kt`, `InListAds.kt` của `:core:ads`;
  - `StagingRemoteValuesModule.kt`;
  - bảng khóa `docs/remote-config-keys.md`, `docs/remote-config-template.json`.
- Task khác trong sprint có thể xung đột:
  - T-075 (mã quảng cáo thật, `scripts/release.sh`) đọc danh sách vị trí: chạy sau T-079, hoặc lấy danh sách mã sau khi lượt 2 merge;
  - plan nào cùng danh sách sửa `StagingRemoteValuesModule.kt` hay `AdPlacement.kt` thì xếp sau.
- Đề xuất thứ tự chạy: lượt 1 rồi lượt 2, liền nhau; các task độc lập xếp trước (`docs/CLAUDE.md`, mục Khi lập danh sách chạy).

## 7. Điều kiện dừng riêng

- Ảnh chuẩn ngoài mục 3.2 đổi, nhất là ảnh `default`, `loading`, `error`, `saved-empty` của hai màn, hay ảnh của `core/ads`: dừng. `contentPadding` mặc định 0 không được làm đổi ảnh nào.
- Phải sửa `BottomAdBarScaffold`, `itemsWithInListAds` của lưới hay `rememberShowInterstitial` để có hành vi của plan: dừng, vì hành vi chung của khung đổi theo một app.
- Test sẵn có của luật đếm ảnh `review_interstitial` (`InterstitialBreaksTest` ngoài dòng dữ liệu của mục 3.5) đỏ: dừng.

## 8. Câu hỏi cho người duyệt

1. **Ô quảng cáo ở bộ lọc "Đã lưu"?** Spec ghi "Tất cả" và từng nhóm, không nói về "Đã lưu". Đề xuất của AI: cùng một lưới, nên "Đã lưu" có ô sau tư thế thứ 8 khi người dùng lưu từ 9 tư thế. Muốn bỏ ở "Đã lưu" thì `PosesScreen` dùng `PosesGridAds.None` khi `selected == PoseFilter.Saved`, thêm một test.
2. **`is_show_native_collap` mặc định trong code là bật.** Bản phát hành cần đặt `false` trên Firebase production, nếu không lớp 250 dp sẽ nổi trên lưới và nút chính. Đề xuất của AI: không đổi mặc định của khung (dùng chung với dự án mẫu); thêm khóa này vào danh sách việc của người ở T-075 và `docs/ops/remote-config.md`, mục 5, lúc phát hành.
3. **Chỗ cuộn khi quay lại từ chi tiết (KB-2) khi ô quảng cáo vừa xuất hiện.** `LazyGridState` giữ chỉ số trong danh sách gộp. Nếu ô quảng cáo được phép giữa lúc mở chi tiết và lúc quay lại, hay N đổi từ xa, lưới lệch một ô. Đề xuất của AI: chấp nhận (hiếm, chỉ lệch một ô), không làm giữ chỗ theo khóa tư thế.
4. **Bấm "Chụp với tư thế này" hai lần nhanh.** Khi lượt đầu đang chờ quảng cáo, lần bấm sau bị bỏ qua và không ghi `pose_selected`. Không có quảng cáo thì hành vi như cũ: mỗi lần bấm mở camera một lần. Đề xuất của AI: như vậy.
5. **Hàm công khai mới trong `:core:ads` (`itemsWithFixedAds`), chỉ để vẽ ảnh chuẩn.** Đề xuất của AI: thêm, ghi đề xuất về dự án mẫu trong PR lượt 2. Cách khác là tạo một bản giả `InListAds` công khai cho test, nhưng cách đó lộ phần trạng thái của khung.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
