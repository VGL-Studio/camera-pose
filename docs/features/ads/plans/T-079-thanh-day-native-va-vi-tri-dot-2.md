---
ticket: T-079
title: Thanh đáy native thay hai banner ở thư viện và chi tiết tư thế; interstitial khi chụp với tư thế (tắt lúc phát hành)
status: approved             # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 15.7M        # hai lượt theo số gợi ý của scripts/token-usage.py estimate: lượt 1 cỡ M (8.6M), lượt 2 cỡ S (7.1M); không nâng tay
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Tuấn               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-10-31
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-079: THANH ĐÁY NATIVE THAY HAI BANNER Ở THƯ VIỆN VÀ CHI TIẾT TƯ THẾ; INTERSTITIAL KHI CHỤP VỚI TƯ THẾ

## 1. Tóm tắt

Đáy thư viện và chi tiết tư thế chuyển từ hai banner (`poses_banner`, `poses_detail_banner`) sang thanh đáy native nhỏ của khung (`BottomAdBarScaffold`, vị trí `poses_bottom`, `pose_detail_bottom`). App thêm interstitial khi bấm "Chụp với tư thế này" (`inter_pose_detail`), tắt lúc phát hành.

Việc chia hai lượt trong cùng danh sách chạy, mỗi lượt một PR có CI xanh:

- **Lượt 1:** thanh đáy, bản staging, bảng khóa, ảnh chuẩn `ad`.
- **Lượt 2:** interstitial.

Kiểm chứng gồm:

- test vị trí Robolectric: khe đáy không che nút chính và thẻ cuối;
- screenshot test cùng tên với bốn ảnh thiết kế `ad`;
- test giá trị của bản staging;
- bảng khóa sinh lại;
- thử trên máy thật với quảng cáo thử của Google.

**Phiên bản 2 (2026-10-31): vì sao phiên bản 1 chưa đủ tốt và những gì đổi.** Rà soát độc lập trước khi duyệt danh sách RUN-20261031-01 tìm ra hai điểm chặn.

1. **Test đối chứng của KB-6, tiêu chí 3 không thể đúng ở thư viện.** Lưới luôn kéo tới đáy màn; phần chừa đi vào lề trong của lưới, nên đo đáy lưới thì không thấy khác biệt. Test viết trước sẽ đỏ dù code đúng (điều kiện dừng 8). Phiên bản 2 đo thẻ cuối sau khi cuộn hết (mục 4).
2. **Ô quảng cáo trong lưới để lại ô trống khi pool cạn.** Hàm lưới của khung luôn đặt ô quảng cáo: nội dung cao 0 nhưng vẫn chiếm một cột. Spec poses KB-6 lại ghi "hết quảng cáo thì ô ẩn, lưới không có ô trống". Nhật chọn sửa khung để bỏ ô khi pool cạn (2026-10-31). Cách sửa là kỹ thuật mới của `:core:ads` (E = 2), phải chạy thử trước khi duyệt, nên `poses_in_collection` tách sang ticket T-080 (Nhật chọn, 2026-10-31).

Phiên bản 2 bỏ `poses_in_collection`, `itemsWithFixedAds`, `PosesGridAds` và ảnh `PosesScreen_ad-collection_light` khỏi plan. Plan cũng sửa các điểm nên sửa của rà soát:

- test lỗi và lưu trống chạy ở khung thấp, có đối chứng;
- luật "đang chờ thì bỏ qua" nằm trong `ShootGate.tryBegin`, để unit test chạy đúng code thật;
- phần nối ở hai Route ghi vào ngoại lệ viết test trước;
- khe giả của test dùng modifier được truyền vào;
- snackbar chụp ở đồng hồ dừng;
- phần thanh đáy của `manual-test.md` chuyển sang lượt 1.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/poses/spec.md` (confirmed 2026-10-30), cùng câu 7 tới 11 của `features/ads/spec.md` (confirmed 2026-10-30). KB-6 tiêu chí 5 (`poses_in_collection`) thuộc T-080 từ phiên bản 2.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-4 | Khi khóa `is_show_inter_pose_detail` bật, có quảng cáo toàn màn hình sẵn sàng và đã qua nhịp chung `ads_interval`, quảng cáo hiện sau khi bấm "Chụp với tư thế này". Đóng quảng cáo thì camera mở với đúng tư thế đó. Không có quảng cáo, chưa đủ nhịp hay khóa tắt thì camera mở ngay, không chờ | Làm ở lượt 2. Route chi tiết gọi `rememberShowInterstitial(AdPlacement.INTER_POSE_DETAIL)` và mở camera trong `onFinished`. Khung không chờ tải: không có quảng cáo đã nạp thì `onFinished(false)` chạy ngay. Vị trí này không có luật đếm ảnh (`InterstitialBreakSettings.capOf` trả `null`, cổng đếm cho qua mọi lần). Cùng nhóm giãn cách mặc định với `review_interstitial` (`AdIntervalGroup.DEFAULT`). `pose_selected` ghi một lần cho mỗi lần bấm được nhận, trước quảng cáo, như cũ. |
| KB-6, tiêu chí 1 | Khi `is_show_poses_bottom` bật, `is_show_native_bottom` bật và người dùng đã đồng ý, thanh đáy native 80 dp hiện ở đáy thư viện. Lưới cuộn phía trên thanh, thẻ cuối không bị che | Làm ở lượt 1. `BottomAdBarScaffold(AdPlacement.POSES_BOTTOM)` bọc cả màn. Khoảng chừa ở đáy (96 dp khi khe có quảng cáo, 0 khi không) cộng vào lề dưới của lưới, của nội dung các trạng thái lỗi, lưu trống, đang tải, và của snackbar. Lưới vẫn kéo tới đáy màn, nên thẻ trôi sau phần lề 16 dp quanh khe như thiết kế. |
| KB-6, tiêu chí 2 | Khi `is_show_pose_detail_bottom` bật, thanh đáy hiện ở đáy màn chi tiết, dưới nút "Chụp với tư thế này". Nút không bị che hay đẩy ra ngoài màn hình | Làm ở lượt 1. `BottomAdBarScaffold(AdPlacement.POSE_DETAIL_BOTTOM)` bọc màn chi tiết. Khoảng chừa thành lề dưới của cột nội dung, nên thanh nút chính (`PoseDetailBottomBar`) nằm ngay trên khe. Bố cục mở gập: khe rộng cả màn ở đáy, nút chính ở cột phải nằm trên khe. Ở khung thấp 360 x 640, nút vẫn thấy trọn. |
| KB-6, tiêu chí 3 (khóa tắt, đang tải hay không có quảng cáo thì không chừa chỗ) | Như spec | Khung chỉ chừa chỗ khi khe đã có quảng cáo (D-04). Không có `LocalAdsController` (trong test) thì không vẽ gì và chừa 0. |
| KB-6, tiêu chí 4 | `is_show_native_collap` tắt (giá trị lúc phát hành) thì không có lớp nổi trên lưới hay nút chính | Không đổi code của lớp. Bản staging đặt `is_show_native_collap` là `false` (câu 11). Production đặt khóa này trên Firebase trước khi phát hành (mục 8, câu 2). |
| Spec ads, câu 8 | Mã mới `poses_bottom`, `pose_detail_bottom`; bỏ `poses_banner`, `poses_detail_banner` khỏi code; khóa cũ giữ trên Firebase | Làm ở lượt 1. Bỏ hai dòng enum, `PosesBottomBanner`, `PosesAdSeparator`, `PosesTestTags.BANNER_SLOT`. Không xóa gì trên Firebase. |
| Spec ads, câu 11 | Bản staging bật `is_show_poses_bottom`, `is_show_pose_detail_bottom`, `is_show_native_bottom`, `is_show_inter_pose_detail`; đặt `is_show_native_collap` là `false`; không bật `is_show_native_after_inter`; có test | Lượt 1 đặt bốn khóa của thanh đáy và lớp thu gọn. Lượt 2 thêm `is_show_inter_pose_detail`. `is_show_poses_in_collection` do T-080 đặt. `StagingRemoteValuesModuleTest` kiểm cả tập. |

Thiết kế tham chiếu: `features/poses/design/README.md` (mục 4, dòng "Thanh đáy native"; mục 5). Ảnh trong `design/screens/`: `PosesScreen_ad_light`, `PosesScreen_ad_dark`, `PoseDetailScreen_ad_light`, `PoseDetailScreen_ad_dark`, xuất ngày 2026-10-30 (PR #253). `PosesScreen_ad-collection_light` thuộc T-080.

Số liệu trong `design/context/`:

- khe 379 x 80, cách mép trái, phải và đáy 16, nội dung chừa 96;
- ở chi tiết, thanh nút chính cao 96 nằm ở `y = 699`, khe ở `y = 795`.

ADR liên quan: ADR-0004, ADR-0013, ADR-0014 (vị trí theo spec, mặc định tắt), ADR-0020 (ảnh thiết kế, screenshot test cùng tên), ADR-0023 (tên khóa), ADR-0026 (khung AdsKit), ADR-0029, ADR-0031, ADR-1007.

## 3. Giải pháp

### 3.1. Cách làm

**Hai lượt.** Danh sách chạy có hai dòng cùng ticket và plan này, ghi chú `lượt 1` và `lượt 2`; lượt 2 phụ thuộc lượt 1. Mỗi lượt đi đủ 10 bước của `docs/auto-run.md`, mục 3, với nhánh và PR riêng. Nhánh chính phải chạy đúng sau từng lượt.

- **Lượt 1, nhánh `claude/T-079-1-thanh-day`**, từ nhánh chính. Mục tiêu: thanh đáy thay banner, giá trị staging của thanh đáy, bảng khóa, ảnh chuẩn `ad`, phần thanh đáy của `manual-test.md`.
- **Lượt 2, nhánh `claude/T-079-2-inter-chi-tiet`**, từ nhánh chính sau lượt 1. Mục tiêu: `inter_pose_detail`, giá trị staging, bảng khóa, phần interstitial của `manual-test.md`.

#### Lượt 1: thanh đáy native thay banner

**`AdPlacement.kt`.** Ở khối "The placements of this app", thay hai dòng banner bằng:

```kotlin
/** Bottom bar of the pose library (spec poses, section 5; spec ads, question 8). */
POSES_BOTTOM("poses_bottom", AdFormat.NATIVE_SMALL),

/** Bottom bar of the pose detail, below the main button (spec poses, section 5; spec ads, question 8). */
POSE_DETAIL_BOTTOM("pose_detail_bottom", AdFormat.NATIVE_SMALL),
```

Mỗi vị trí một dòng, vì `scripts/release.sh` chỉ đọc khai báo một hàng (`AdPlacementSourceTest`). Sửa chú thích của khối thành "spec ads, questions 1, 2 and 8". `AdsSettings` tự sinh `is_show_poses_bottom` và `is_show_pose_detail_bottom` từ khai báo, không cần sửa.

**Thư viện (`PosesScreen.kt`).**

- `PosesRoute` bọc màn: `BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) { padding -> PosesScreen(..., contentPadding = padding) }`.
- `PosesScreen` bỏ tham số `bottomBanner`, thêm `contentPadding: PaddingValues = PaddingValues()`. Chỉ dùng phần đáy, lấy bằng `calculateBottomPadding()`, gọi là `bottomReserve`. Giá trị mặc định 0 giữ nguyên mọi ảnh chuẩn không có quảng cáo.
- `bottomReserve` đi vào:
  - lưới của `ReadyContent`: `gridPadding(bottomReserve)` trả `bottom = Spacing.md + bottomReserve`. Lưới vẫn chiếm hết chiều cao, nên thẻ cuộn sau khe như thiết kế; khi cuộn hết, thẻ cuối nằm trên khe, cách khe 16 dp;
  - `LoadingGrid`: cùng `gridPadding(bottomReserve)`;
  - `CenteredScrollable` của trạng thái lỗi và lưu trống: `Modifier.padding(bottom = bottomReserve)`;
  - `SnackbarHost`: `Modifier.padding(bottom = bottomReserve)`.
- Bỏ `PosesTestTags.BANNER_SLOT`.

**Chi tiết (`PoseDetailScreen.kt`, `PoseDetailComponents.kt`).**

- `PoseDetailRoute` bọc màn: `BottomAdBarScaffold(AdPlacement.POSE_DETAIL_BOTTOM) { padding -> PoseDetailScreen(..., contentPadding = padding) }`.
- `PoseDetailScreen` bỏ `bottomBanner`, thêm `contentPadding: PaddingValues = PaddingValues()`. Cột ngoài cùng (`Column` dưới thanh tiêu đề) và `SnackbarHost` nhận `Modifier.padding(bottom = bottomReserve)`.
- `PhoneDetailLayout`, `WideDetailLayout` bỏ tham số `bottomBanner` và lời gọi `bottomBanner()` sau `PoseDetailBottomBar`.
- Khung bọc cả màn, nên khe có thể hiện cả lúc màn chi tiết đang tải. Trước đây banner chỉ hiện khi tư thế đã hiện. Spec không cấm điều này (mục 8, câu 6).

**`PosesComponents.kt`.** Xóa `PosesBottomBanner` và `PosesAdSeparator`, vì không còn chỗ dùng; test của lượt 1 cũng bỏ chúng (mục 3.5). Bỏ import `AdSlot`, `AdPlacement` nếu không còn dùng.

**Bản staging (`StagingRemoteValuesModule.kt`).**

- `appPlacementsShown()`: `POSES_BOTTOM`, `POSE_DETAIL_BOTTOM`, `REVIEW_INTERSTITIAL`.
- `globalAdSwitchesOn()`: thêm `AdsSettings.IS_SHOW_NATIVE_BOTTOM`.
- Hàm mới `globalAdSwitchesOff()` (`@Provides @ElementsIntoSet @LocalRemoteValues`) trả `LocalRemoteValue.of(AdsSettings.IS_SHOW_NATIVE_COLLAP, false)`. Mặc định trong code của khóa này là bật; staging cần giống bản phát hành (câu 10, 11).
- Sửa KDoc: bỏ `is_show_native_bottom` khỏi câu "Not set, so off".

**Bảng khóa.** Chạy `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`. `docs/remote-config-keys.md` và `docs/remote-config-template.json` thêm `is_show_poses_bottom`, `is_show_pose_detail_bottom`, bỏ `is_show_poses_banner`, `is_show_poses_detail_banner`. Không sửa tay.

**Ảnh chuẩn và so thiết kế.**

1. Ghi lại 6 ảnh `ad` của mục 3.2:

   ```
   ./gradlew :feature:poses:recordRoborazziDebug :feature:poses:testDebugUnitTest --rerun
   git status --short -- '*/src/test/screenshots/'
   ```

   `git status` chỉ được đổi 6 ảnh đó.
2. Chạy `scripts/design-compare.sh poses`, commit `docs/features/poses/design/review/`.

Screenshot test vẽ thanh đáy bằng phần không trạng thái của khung: `BottomAdBarLayout(reservation = BottomAdBarDefaults.nativeReservation, slot = { NativeAdCard(SCREENSHOT_AD, NativeAdSize.SMALL, it) }, layer = null) { padding -> PosesScreen(..., contentPadding = padding) }`. Tài sản giả như ảnh thiết kế: "Headline of the ad", "Advertiser or short body", "Install", `icon = null`, `hasMedia = false`.

**Tài liệu.** Trong `docs/ops/manual-test.md` mục 11 (Quảng cáo), thay bước banner ở thư viện và chi tiết bằng bước thanh đáy native: khe 80 dp, nút chính không bị che, không có lớp nổi.

**Firebase staging.** Bản 23 đã đặt `is_show_poses_bottom` và `is_show_pose_detail_bottom` là `true`, `is_show_native_collap` là `false` (FB-20261030-01). Nhật kiểm ngày 2026-10-31: `is_show_native_bottom` đang `true` (RUN-20261031-01).

#### Lượt 2: interstitial khi chụp với tư thế

**`AdPlacement.kt`.** Thêm sau `POSE_DETAIL_BOTTOM`:

```kotlin
/** After "Shoot with this pose" in the detail, before the camera opens (spec poses, KB-4; spec ads, question 9). */
INTER_POSE_DETAIL("inter_pose_detail", AdFormat.INTERSTITIAL),
```

`AdsSettings` tự sinh `is_show_inter_pose_detail`. `InterstitialBreakSettings.capOf` không có nhánh cho vị trí này nên trả `null`, tức không có luật đếm ảnh. Không sửa `InterstitialBreaks.kt`.

**Chi tiết: interstitial trước camera.** Tạo file `detail/ShootWithAd.kt`, gồm:

- Lớp thuần `internal class ShootGate`, giữ luật "đang chờ thì bỏ qua":
  - `fun tryBegin(pick: () -> String?): String?` trả `null` mà không gọi `pick` khi đang có một lượt chờ. Nếu không có lượt chờ, hàm gọi `pick()` (ViewModel ghi `pose_selected` và trả tư thế); có tư thế thì giữ nó làm lượt chờ và trả lại.
  - `fun finish(): String?` trả tư thế của lượt chờ rồi xóa lượt.
- Hàm composable:

```kotlin
/**
 * Returns the action of "Shoot with this pose" (KB-4). The action asks [ShootGate.tryBegin] for the pose (the
 * ViewModel logs the choice), asks the interstitial of [placement] at this natural break, then opens the camera with
 * the pose in onFinished, shown or not. A tap while an earlier one waits for its ad is ignored, so the camera opens
 * once and the choice is logged once.
 */
@Composable
internal fun rememberShootWithAd(
    placement: AdPlacement,
    onOpen: (poseId: String) -> Unit,
): (pick: () -> String?) -> Unit
```

Thân hàm:

- `val gate = remember { ShootGate() }`;
- `val showAd = rememberShowInterstitial(placement) { _ -> gate.finish()?.let(currentOnOpen) }`;
- trả `{ pick -> if (gate.tryBegin(pick) != null) showAd() }`.

`rememberShowInterstitial` chạy `onFinished` trên luồng chính (`Dispatchers.Main.immediate`, L-030), đúng một lần mỗi lần gọi.

`PoseDetailRoute`: `val shoot = rememberShootWithAd(AdPlacement.INTER_POSE_DETAIL, onShootWithPose)` và `onShoot = { shoot(viewModel::onShoot) }`. `PoseDetailViewModel` không đổi.

- Ra nền giữa chừng (L-091 của mẫu): plan không có bước chờ có giới hạn. Khung chỉ hiện quảng cáo đã nạp và chỉ khi activity đang resumed. Không hiện được thì `onFinished(false)` chạy ngay và camera mở; quảng cáo đang hiện thì `onFinished(true)` chạy sau khi đóng.
- Cổng đếm (`LocalInterstitialBreakGate`) đếm riêng lần dừng của `inter_pose_detail` và cho qua mọi lần (`BreakCap.NONE`). Nhịp `ads_interval` dùng chung với `review_interstitial`.

**Bản staging.** `appPlacementsShown()` thêm `INTER_POSE_DETAIL`.

**Bảng khóa.** Sinh lại như lượt 1. Bảng thêm `is_show_inter_pose_detail`.

**Tài liệu.** `docs/ops/manual-test.md` mục 11: thêm bước interstitial ở màn chi tiết (bấm "Chụp với tư thế này", đóng quảng cáo thì camera mở đúng tư thế).

**Sau khi merge lượt 2 (việc của người, không nằm trong PR).** Chạy `python3 scripts/ads-plan.py --keep <file v2 trên Drive>` và đổi trạng thái các dòng của T-079 thành `Đã code`. Danh sách mã cần tạo của T-075 có thêm ba vị trí mới (`poses_bottom`, `pose_detail_bottom`, `inter_pose_detail`) và không còn hai banner. `poses_in_collection` vào danh sách đó sau T-080.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | Lượt 1: `POSES_BOTTOM`, `POSE_DETAIL_BOTTOM` thay hai banner. Lượt 2: `INTER_POSE_DETAIL` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementAppTest.kt` | Lượt 1 và 2: tập vị trí của app (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | Lượt 1 và 2: chuỗi cấu hình mã đơn vị có mã của vị trí mới, bỏ hai banner (dữ liệu, ADR-0029) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdSlotTest.kt` | Lượt 1: `POSES_BANNER` thành `GALLERY_BANNER`, vị trí banner còn lại (dữ liệu) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InterstitialBreaksTest.kt` | Lượt 1: `POSES_BANNER` thành `POSES_BOTTOM` (dữ liệu). Lượt 2: `interPoseDetail_hasNoBreakCap` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/ShowInterstitialTest.kt` | Lượt 1: `POSES_BANNER` thành `POSES_BOTTOM` (vị trí không phải interstitial, dữ liệu) |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreen.kt` | Lượt 1: `BottomAdBarScaffold`, `contentPadding`, bỏ `bottomBanner`, `BANNER_SLOT` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesComponents.kt` | Lượt 1: xóa `PosesBottomBanner`, `PosesAdSeparator` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreen.kt` | Lượt 1: `BottomAdBarScaffold`, `contentPadding`, bỏ `bottomBanner`. Lượt 2: `rememberShootWithAd` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailComponents.kt` | Lượt 1: `PhoneDetailLayout`, `WideDetailLayout` bỏ `bottomBanner` |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/ShootWithAd.kt` | Lượt 2: `ShootGate`, `rememberShootWithAd` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenTest.kt` | Lượt 1: test thanh đáy thay test banner (mục 3.5) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenScreenshotTest.kt` | Lượt 1: ảnh `ad` vẽ `BottomAdBarLayout` và `NativeAdCard`, bỏ `TestBanner` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenTest.kt` | Lượt 1: test thanh đáy thay test banner (mục 3.5) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenScreenshotTest.kt` | Lượt 1: ảnh `ad` vẽ `BottomAdBarLayout` và `NativeAdCard`, bỏ `TestBanner` |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/ShootWithAdTest.kt` | Lượt 2: mục 4 |
| Sửa | `feature/poses/src/test/screenshots/PosesScreen_ad_light.png` | Lượt 1: ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PosesScreen_ad_dark.png` | Lượt 1: ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PosesScreen_ad_small.png` | Lượt 1: ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_light.png` | Lượt 1: ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_dark.png` | Lượt 1: ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_small.png` | Lượt 1: ghi lại |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModule.kt` | Lượt 1: thanh đáy, `is_show_native_bottom`, `globalAdSwitchesOff`. Lượt 2: `INTER_POSE_DETAIL` |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModuleTest.kt` | Lượt 1 và 2: tập giá trị mong đợi (mục 3.5, mục 4) |
| Sửa | `docs/remote-config-keys.md` | Lượt 1 và 2: sinh lại bằng `-PremoteConfigKeys.update=true` |
| Sửa | `docs/remote-config-template.json` | Lượt 1 và 2: sinh lại cùng lệnh |
| Sửa | `docs/features/poses/design/review/` | Lượt 1: ảnh so sánh và `summary.md` do `scripts/design-compare.sh poses` sinh |
| Sửa | `docs/ops/manual-test.md` | Lượt 1: bước thanh đáy. Lượt 2: bước interstitial ở chi tiết |

Lượt 2 sửa tiếp các file mà lượt 1 đã sửa (`AdPlacement.kt`, `PoseDetailScreen.kt`, `StagingRemoteValuesModule.kt`, test của nó, bảng khóa, `manual-test.md`). Lượt 2 mô tả các file đó ở trạng thái sau khi lượt 1 merge.

Nơi khác đọc file mà plan này đổi:

- `scripts/release.sh` đọc dòng một hàng của `AdPlacement.kt`. Ba dòng mới đều một hàng, nên không đổi script. Mã thật của ba vị trí là việc của T-075.
- `scripts/ads-plan.py` đọc `AdPlacement` và `docs/remote-config-keys.md`. Test của script đọc code thật (`scripts/tests/run.sh`, khối `ads-plan`) và không đếm số vị trí cố định, nên không sửa.
- `app/build.gradle.kts` chỉ nêu đường dẫn của `docs/remote-config-keys.md` và `docs/remote-config-template.json` cho tham số `-PremoteConfigKeys.update`. Đường dẫn không đổi, nên không sửa.
- `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` so hai tệp đó với khóa của code. Tập khóa lấy từ `AdsSettings.all()` và `AdPlacement.entries`, không ghi tên vị trí, nên không sửa test. Hai tệp được sinh lại trong cùng commit code, nên test qua.
- `scripts/check.sh` và `design-compare.sh` đọc `src/test/screenshots/` và `design/screens/`. `PosesScreen_ad-collection_light` vẫn chưa có ảnh chuẩn tới T-080: `design-compare.sh` ghi "thiếu ảnh chuẩn screenshot" như hiện nay; mục 4 ghi ảnh đó thuộc T-080.
- `scripts/existing-tests.py` xếp một số test vào nhóm "Cần xem trước" dù chúng không dùng banner. Các test này giữ nguyên câu khẳng định, vì chúng dựng màn không có quảng cáo và `contentPadding` mặc định là 0:
  - `PoseDetailScreenTest.smallScreen_bottomBarAlwaysVisible_lastMistakeScrollsAboveIt`;
  - `PoseDetailScreenTest.wide_imageLeft_infoRight_bottomBarInRightColumn`;
  - `PoseDetailScreenTest.wide_infoScrolls_bottomBarStaysVisible`;
  - `PoseDetailScreenTest.landscape_bottomBarVisible`;
  - `PosesScreenTest.scrollPosition_survivesStateRestore`;
  - các ảnh `default_*_unfolded`, vốn không vẽ `PosesAdSeparator`.

  Không test nào dựng `PosesRoute` hay `PoseDetailRoute`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: không có. Ba vị trí dùng khóa `is_show_<vị trí>` của khung, mặc định tắt (ADR-0014).
- Quảng cáo:
  - thêm `poses_bottom`, `pose_detail_bottom` (native small, thanh đáy) và `inter_pose_detail` (interstitial, nhóm giãn cách mặc định);
  - bỏ `poses_banner`, `poses_detail_banner`;
  - căn cứ: `features/ads/spec.md` câu 8, 9 và `features/poses/spec.md` mục 5;
  - `poses_in_collection` thuộc T-080.
- Sự kiện phân tích: không thêm. Khung ghi sự kiện quảng cáo với `ad_placement` là mã vị trí mới. `pose_selected` giữ nguyên.
- File R2: không chạm.
- **Quyền truy cập:** không thêm.
- **Thư viện và SDK:** không thêm. `BottomAdBarScaffold`, `BottomAdBarLayout`, `NativeAdCard`, `rememberShowInterstitial` đã có trong `:core:ads` từ T-074, và `:feature:poses` đã phụ thuộc `:core:ads`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `BottomAdBarScaffold` bọc ngoài màn có thể làm lệch phép đo của các test vị trí sẵn có, nếu khung thêm một tầng bố cục mà test không ngờ tới.
- **Điều kiện chuyển:** một test vị trí của mục 4 đỏ vì cùng một lỗi đo sau 2 lần sửa.
- **Cách làm thay:** các test vị trí đo bằng `BottomAdBarLayout` với khe giả 80 dp thay cho khung thật. Khung thật dùng chính phần bố cục này. Tiêu chí và câu khẳng định giữ nguyên.

### 3.5. Hành vi đã có bị đổi

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Banner 58 dp nằm dưới lưới, lưới ngắn lại (`PosesScreen` đặt `bottomBanner()` sau lưới trong `Column`) | Khe 80 dp đè ở đáy, cách mép 16; lưới kéo tới đáy màn, lề dưới cộng 96 | `PosesScreenTest.banner_sitsBelowGrid_lastCardScrollsAboveIt` | Thay bằng `bottomBar_lastCardScrollsAboveSlot`: sau khi cuộn tới thẻ cuối, `lastCard.bottom <= slot.top`; khe cao 80, cách đáy 16. Ý định giữ: thẻ cuối không bị quảng cáo che |
| Banner hiện cả ở trạng thái lỗi và lưu trống | Khe hiện ở mọi trạng thái vì khung bọc cả màn; nội dung trạng thái chừa chỗ | `PosesScreenTest.banner_shownAlsoOnErrorAndSavedEmpty` | Thay bằng `smallScreen_bottomBar_onErrorAndSavedEmpty_actionAboveSlot` (mục 4) |
| Banner thật không có quảng cáo thì chiếm 0 | Khung thật không có controller thì không vẽ khe, chừa 0 | `PosesScreenTest.realBottomBanner_withoutAds_takesNoSpace`, `PoseDetailScreenTest.realBottomBanner_withoutAds_takesNoSpace` | Thay `PosesBottomBanner(...)` bằng `BottomAdBarScaffold(POSES_BOTTOM)` / `(POSE_DETAIL_BOTTOM)`. Câu giữ: không có node của khe. Câu đo đổi: ở thư viện đo thẻ cuối sau khi cuộn hết (cách đáy màn 16), ở chi tiết đo `BOTTOM_BAR` (chạm đáy màn) (mục 4) |
| Banner nằm dưới thanh nút ở chi tiết | Khe đè ở đáy; cột nội dung có lề dưới 96, nên thanh nút nằm trên khe | `PoseDetailScreenTest.banner_belowBottomBar_buttonNotCovered`, `smallScreen_withBanner_buttonFullyVisible`, `wide_banner_belowBottomBar_inRightColumn` | Đổi thành `bottomBar_slotBelowButtonBar_buttonNotCovered`, `smallScreen_withBottomBar_buttonFullyVisible`, `wide_bottomBar_slotFullWidth_buttonAbove` (mục 4) |
| Staging bật `is_show_poses_banner`, `is_show_poses_detail_banner`; không đặt `is_show_native_bottom` | Bật `is_show_poses_bottom`, `is_show_pose_detail_bottom`, `is_show_native_bottom`; đặt `is_show_native_collap = false` | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet`, `firstOpenAndNativeCombos_areNotSet` | Tập mong đợi đổi theo mục 4. Câu cũ "`is_show_native_bottom` không đặt" trái câu 11 của spec ads, nên viết lại theo ADR-0031 thành hai test: `firstOpenAndCombo_areNotSet` (hai khóa `show_open_ads_first_open`, `is_show_native_after_inter`, giữ đối chứng) và `nativeBottomOn_collapOff` (mục 4). Ý định giữ: staging không bật gì ngoài điều spec đã chốt |
| Tập vị trí của app gồm ba vị trí, có hai banner | Bốn vị trí sau lượt 2, không còn banner | `AdPlacementAppTest` (dòng 16, 17, 30, 31) | Dữ liệu (ADR-0029): bảng mong đợi đổi theo mục 4, thêm câu "không còn `poses_banner`, `poses_detail_banner`" |
| `POSES_BANNER` dùng làm mẫu "vị trí không phải interstitial" và "vị trí không có luật đếm" | `POSES_BOTTOM` thay vai trò đó | `InterstitialBreaksTest.placements_countSeparately`, `InterstitialBreakSettings` (dòng 101), `ShowInterstitialTest` (dòng 38), `AdSlotTest` (dòng 53, 124: `GALLERY_BANNER`), `AdUnitIdsTest` (dòng 15, 22) | Dữ liệu (ADR-0029), câu khẳng định giữ nguyên |
| `PosesScreen`, `PoseDetailScreen` nhận `bottomBanner` | Nhận `contentPadding` | Hàm `setScreen` của `PosesScreenTest`, `PoseDetailScreenTest`; hàm `Screen(...)` của hai lớp screenshot test | Phần dựng của test đổi theo chữ ký mới. Ảnh không có quảng cáo không đổi, vì `contentPadding` mặc định 0 |
| Bấm "Chụp với tư thế này" mở camera ngay | Qua `rememberShootWithAd`: không có controller hay không có quảng cáo thì mở ngay; có quảng cáo thì mở sau khi đóng | `PoseDetailScreenTest` (các test bấm nút gọi `onShoot`), `PoseDetailViewModelTest` (`onShoot` ghi `pose_selected`) | Giữ: `PoseDetailScreen` vẫn gọi `onShoot` một lần mỗi lần bấm; ViewModel không đổi. Phần mới kiểm ở `ShootWithAdTest` |

## 4. Kiểm thử

Mọi test vị trí của lượt 1 vẽ khe giả bằng `BottomAdBarLayout(reservation = BottomAdBarDefaults.nativeReservation, slot = { Box(it.height(80.dp).testTag(SLOT)) }, layer = null) { PosesScreen(..., contentPadding = it) }`. Khe giả dùng modifier mà khung truyền vào, nên rộng bằng khung trừ 2 x 16. Khung thật mang tag `"bottom_ad_bar_slot"`; đối tượng tag là `internal` nên test của feature ghi thẳng chuỗi. Các test này chạy với `@GraphicsMode(NATIVE)`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Spec ads câu 8 (lượt 1): hai vị trí thanh đáy là native small, không còn banner | Unit test | `AdPlacementAppTest`. Bảng mong đợi: `poses_bottom` và `pose_detail_bottom` là `NATIVE_SMALL`, `review_interstitial` là `INTERSTITIAL`. `bannersRemoved`: `AdPlacement.entries` không có mã `poses_banner`, `poses_detail_banner`. Đối chứng: vẫn có `review_interstitial` |
| Spec ads câu 9 (lượt 2): `inter_pose_detail` | Unit test | `AdPlacementAppTest` thêm vào bảng: `inter_pose_detail` là `INTERSTITIAL`, `intervalGroup == AdIntervalGroup.DEFAULT == REVIEW_INTERSTITIAL.intervalGroup`, không `appWide`, không `ignoreInterval`, có `recordInterval` |
| KB-4: `inter_pose_detail` không có luật đếm ảnh; `review_interstitial` giữ luật | Unit test (lượt 2) | `InterstitialBreaksTest.interPoseDetail_hasNoBreakCap`: `capOf(INTER_POSE_DETAIL)` là `null`; cổng dùng `FakeRemoteSettings` cho qua lần dừng 1, 2, 3 của `INTER_POSE_DETAIL`. Đối chứng: cùng cổng, lần 1 của `REVIEW_INTERSTITIAL` là `null`. Test sẵn có của luật đếm ảnh giữ nguyên câu khẳng định |
| KB-6, tiêu chí 1: lưới cuộn phía trên thanh, thẻ cuối không bị che | Robolectric (lượt 1) | `PosesScreenTest.bottomBar_lastCardScrollsAboveSlot`: `ready(manyCards())` trong `BottomAdBarLayout` có khe giả; `performScrollToIndex(last)`. Khẳng định: thẻ cuối có `bottom <= slot.top`; khe cao 80 ± 0,5; `root.bottom - slot.bottom == 16` ± 0,5. Đối chứng: cùng khung với `contentPadding` 0 truyền thẳng (bỏ `it`) thì thẻ cuối có `bottom > slot.top` |
| KB-6, tiêu chí 1: lỗi và lưu trống không bị che | Robolectric, khung `w360dp-h640dp-xxhdpi` (lượt 1) | `PosesScreenTest.smallScreen_bottomBar_onErrorAndSavedEmpty_actionAboveSlot`: trạng thái `Error` rồi lưu trống, trong `BottomAdBarLayout` có khe giả. Khẳng định: khe có mặt; nút hành động của `StateMessage` có `bottom <= slot.top`; tâm dọc của nút cao hơn tâm của cùng nút khi `contentPadding` 0 đúng 48 ± 1 (nửa của 96). Đối chứng: `contentPadding` 0 thì độ lệch là 0 |
| KB-6, tiêu chí 1: snackbar không nằm dưới khe | Robolectric (lượt 1) | `PosesScreenTest.saveFailedSnackbar_aboveSlot`: `mainClock.autoAdvance = false`, `ready(saveFailed = true)` có khe giả, `advanceTimeBy(500)` như `saveFailed_showsSnackbar_thenCallsShown`. Khẳng định: node snackbar có `bottom <= slot.top` |
| KB-6, tiêu chí 2: nút chính ở chi tiết không bị che | Robolectric (lượt 1) | `PoseDetailScreenTest.bottomBar_slotBelowButtonBar_buttonNotCovered`: `BOTTOM_BAR.bottom` cách `slot.top` 0 ± 0,5, tức khe nằm ngay dưới thanh nút như thiết kế (699 + 96 = 795) |
| KB-6, tiêu chí 2: khung thấp (L-028) | Robolectric, khung `w360dp-h640dp-xxhdpi` (lượt 1) | `PoseDetailScreenTest.smallScreen_withBottomBar_buttonFullyVisible`: nội dung dài. Nút "Chụp với tư thế này" có `top >= 0`, `bottom <= slot.top`, chiều cao bằng khi không có khe |
| KB-6, tiêu chí 2: mở gập | Robolectric, rộng 673 (lượt 1) | `PoseDetailScreenTest.wide_bottomBar_slotFullWidth_buttonAbove`: khe rộng `673 - 32` ± 0,5; `BOTTOM_BAR` ở cột phải có `bottom <= slot.top` |
| KB-6, tiêu chí 3: không có quảng cáo thì không chừa chỗ | Robolectric (lượt 1) | `PosesScreenTest.realBottomBar_withoutAds_takesNoSpace`: `BottomAdBarScaffold(POSES_BOTTOM)` thật, không có `LocalAdsController`, `ready(manyCards())`, cuộn tới thẻ cuối. Không có node `bottom_ad_bar_slot`; thẻ cuối có `root.bottom - lastCard.bottom == 16` ± 0,5. Đối chứng: cùng màn trong `BottomAdBarLayout` có khe giả thì khoảng đó là `16 + 96 = 112` ± 0,5. `PoseDetailScreenTest.realBottomBar_withoutAds_takesNoSpace`: không có node của khe; `BOTTOM_BAR.bottom == root.bottom` ± 0,5. Đối chứng: có khe giả thì cách 96 |
| KB-6, tiêu chí 4: lớp thu gọn tắt thì không có lớp nổi | Unit test sẵn có của khung | `BottomAdBarTest.layers_collapOff_keepsNativeSlot_noLayer` (không sửa). Bản staging đặt khóa này `false`, kiểm ở dòng dưới |
| Spec ads câu 11 (lượt 1): giá trị staging của thanh đáy | Unit test (testStaging) | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet`: tập của ba hàm đúng bằng `{is_show_poses_bottom=true, is_show_pose_detail_bottom=true, is_show_review_interstitial=true, show_open_ads=true, show_resum_ads=true, is_show_native_bottom=true, is_show_native_collap=false, is_log_ad_fullscreen_events=true, is_log_ad_slot_events=true}`. `nativeBottomOn_collapOff`: qua `localOnly`, `is_show_native_bottom` đọc `true`, `is_show_native_collap` đọc `false`. Đối chứng: `IS_SHOW_NATIVE_COLLAP.default` là `true`. `firstOpenAndCombo_areNotSet`: `show_open_ads_first_open`, `is_show_native_after_inter` không có trong tập. `noGalleryNorSamplePlacement_isShown` giữ nguyên |
| Spec ads câu 11 (lượt 2): `inter_pose_detail` bật ở staging | Unit test (testStaging) | `localValues_areExactlyTheAppSet` đổi tập mong đợi ở commit test của lượt 2, thêm `is_show_inter_pose_detail=true` |
| KB-4: không có quảng cáo hay khóa tắt thì camera mở ngay, đúng một lần, đúng tư thế | Robolectric (lượt 2) | `ShootWithAdTest.noAdsController_opensAtOnce`: `rememberShootWithAd(INTER_POSE_DETAIL) { opened += it }`, không có `LocalAdsController`. Gọi `shoot { "pose_1" }` rồi `waitForIdle`: `opened == ["pose_1"]`. Gọi lại: `["pose_1", "pose_1"]`, vì không có lượt chờ |
| KB-4: bấm trong lúc chờ quảng cáo thì bỏ qua, `pose_selected` không ghi thêm | Unit test (lượt 2) | `ShootWithAdTest.gate_secondTapWhileWaiting_pickNotCalled`: `pick` đếm số lần được gọi. `tryBegin { "a" }` trả `"a"` (đếm 1); `tryBegin { "b" }` trả `null`, đếm vẫn 1; `finish()` trả `"a"`, lần sau trả `null`; `tryBegin { "c" }` trả `"c"`. `pickReturnsNull_noWait`: `tryBegin { null }` trả `null`, không giữ lượt chờ, lần sau gọi `pick` bình thường |
| KB-4: có quảng cáo thì camera mở sau khi đóng | Robolectric sẵn có của khung, cộng kiểm tra thủ công | `ShowInterstitialTest` của khung (thứ tự `onFinished(true)` sau `onClosed`, không sửa); thử trên máy thật ở dòng dưới |
| Đúng thiết kế | Screenshot test cùng tên, ảnh so sánh `scripts/design-compare.sh poses`, rà soát độc lập theo mục 7 của mẫu thiết kế | Lượt 1, khung `w411dp-h891dp-xxhdpi`: `PosesScreen_ad_light.png`, `PosesScreen_ad_dark.png`, `PoseDetailScreen_ad_light.png`, `PoseDetailScreen_ad_dark.png`. Ảnh `PosesScreen_ad-collection_light` của thiết kế poses thuộc T-080 |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` (lượt 1) | `PosesScreen_ad_small.png`, `PoseDetailScreen_ad_small.png`: có thanh đáy, nhiều nội dung nhất |
| Ảnh không có quảng cáo không đổi | Screenshot test sẵn có | `scripts/check.sh` sau mỗi lượt; `git status --short -- '*/src/test/screenshots/'` chỉ đổi ảnh của mục 3.2 |
| Bảng khóa khớp code | Unit test sẵn có | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode`, `everyPlacementShowKey_isInTableOff` sau khi sinh lại ở mỗi lượt |
| `scripts/ads-plan.py` chạy được | Test của script sẵn có | `scripts/tests/run.sh`, khối `ads-plan`, qua sau mỗi lượt |
| Thanh đáy và interstitial với quảng cáo thử của Google | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging sau lượt 2, đã đồng ý quảng cáo. Thư viện: khe native ở đáy; cuộn hết thì thẻ cuối nằm trên khe; không có lớp nổi. Chi tiết: khe dưới nút chính, nút không bị che. Bấm "Chụp với tư thế này": hiện interstitial thử, đóng thì camera mở đúng tư thế; bấm lại trong 45 giây thì camera mở ngay. Log `VGL.Ads` có `ad_placement` là mã mới. Không bấm quảng cáo |
| Mục 11 của `manual-test.md` đúng các vị trí mới | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR của từng lượt đọc mục 11 |

**Ngoại lệ viết test trước:**

- Sinh lại `docs/remote-config-keys.md`, `docs/remote-config-template.json` và ảnh so sánh của `design-compare.sh`: đây là tệp sinh tự động, sinh trong commit code.
- Ghi lại 6 ảnh chuẩn `ad`: ảnh chỉ ghi được khi đã có code. Screenshot test vẫn viết trước; ở commit test, test đỏ vì chưa có `contentPadding`.
- Phần nối trong `PosesRoute` và `PoseDetailRoute` (bọc `BottomAdBarScaffold`, gọi `rememberShootWithAd`): Route cần `hiltViewModel()`, mà test của feature không dựng Route nào. Bỏ phần nối thì test JVM vẫn xanh. Phần này được kiểm bằng rà soát độc lập (bước 6), đọc diff của hai Route, và bằng bước thử trên máy thật.
- `docs/ops/manual-test.md`: tài liệu.

### 4.1. Chạy thử

Không cần (E dưới 2). Thanh đáy, interstitial theo lần dừng và các hàm không trạng thái dùng cho screenshot đều có trong `:core:ads` từ T-074, kèm test của khung.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**

- điểm 1: thêm, bớt vị trí quảng cáo, đổi định dạng ở hai màn;
- điểm 7: module core `:core:ads` (ba dòng của `AdPlacement`).

Mức Cao, cần duyệt chéo. Người phụ trách sản phẩm (Nhật) đã chốt các vị trí ở spec ads câu 7 tới 11 ngày 2026-10-30.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module (`:core:ads`, `:feature:poses`, `:app`), đổi chữ ký công khai của hai màn |
| B. Độ rõ của yêu cầu | 1 | Hai spec đã xác nhận, có thiết kế; mục 8 đã có câu trả lời |
| C. Khả năng kiểm chứng tự động | 1 | Vị trí, giá trị staging, luật đếm có test. Quảng cáo thật hiện và đóng, cùng phần nối của Route, chỉ kiểm trên máy và bằng rà soát |
| D. Ảnh hưởng chéo | 2 | `AdPlacement.kt` và bảng khóa là file dùng chung với T-080 và T-075 |
| E. Độ mới của kỹ thuật | 1 | Lần đầu một feature của app dùng thanh đáy của khung, nhưng theo đúng API và test của khung |
| **Tổng** | 8 | **Mức: Cao (chạm điểm chặn)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdPlacement.kt` của `:core:ads`, `StagingRemoteValuesModule.kt`, bảng khóa `docs/remote-config-keys.md` và `docs/remote-config-template.json`.
- Task khác có thể xung đột:
  - T-080 (ô quảng cáo trong lưới) sửa `AdPlacement.kt`, `PosesScreen.kt`, `StagingRemoteValuesModule.kt` và bảng khóa, nên chạy sau T-079;
  - T-075 (mã quảng cáo thật) lấy danh sách vị trí sau T-079 và T-080.
- Đề xuất thứ tự chạy: lượt 1 rồi lượt 2, chạy liền nhau; T-080 sau T-079.

## 7. Điều kiện dừng riêng

- Ảnh chuẩn ngoài mục 3.2 đổi (nhất là ảnh `default`, `loading`, `error`, `saved-empty` của hai màn, hay ảnh của `core/ads`): dừng. `contentPadding` mặc định 0 không được làm đổi ảnh nào.
- Phải sửa `BottomAdBarScaffold`, `BottomAdBarLayout` hay `rememberShowInterstitial` mới có được hành vi của plan: dừng, vì hành vi chung của khung sẽ đổi theo một app.
- Test sẵn có của luật đếm ảnh `review_interstitial` (`InterstitialBreaksTest`, ngoài các dòng dữ liệu ở mục 3.5) đỏ: dừng.

## 8. Câu hỏi cho người duyệt

Câu trả lời của phiên bản 1 (Nhật, 2026-10-31) giữ nguyên. Câu 1, 3, 5 nói về ô quảng cáo trong lưới, nay chuyển sang T-080.

1. Ô quảng cáo hiện cả ở bộ lọc "Đã lưu". Chuyển sang T-080.
2. `is_show_native_collap` giữ mặc định của khung. Việc đặt `false` trên Firebase production là việc của người ở T-075. Phiên bản 2 thêm theo rà soát: lúc phát hành, cũng đặt `is_show_native_bottom = true` trên Firebase production (spec ads câu 8), vì mặc định trong code của khóa này là `false`; nếu để mặc định, đáy màn trống.
3. Chấp nhận chỗ cuộn lệch một ô. Chuyển sang T-080.
4. Bấm lần hai trong lúc chờ interstitial thì bỏ qua (`ShootGate.tryBegin`).
5. Hàm `itemsWithFixedAds`. Chuyển sang T-080, cùng phần sửa khung để bỏ ô khi pool cạn.
6. **Mới ở phiên bản 2:** khung bọc cả màn chi tiết, nên khe quảng cáo có thể hiện cả lúc màn đang tải ảnh tư thế. Trước đây banner chỉ hiện khi tư thế đã hiện. Spec không cấm. Đề xuất của AI: chấp nhận, để đáy màn không nhảy khi tư thế tải xong.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

### Phiên bản 1

- **Quyết định:** Duyệt
- **Người duyệt:** Tuấn, **duyệt chéo:** Nhật (người phụ trách sản phẩm, đã chốt các vị trí ở spec ads câu 7 tới 11)
- **Ghi chú:** Ngày 2026-10-31 Nhật trả lời mục 8 trong phiên, AI ghi lại. Cả năm câu theo đề xuất của AI. Rà soát độc lập trước khi chạy RUN-20261031-01 tìm ra hai điểm chặn, nên plan có phiên bản 2.

### Phiên bản 2

- **Quyết định:** Duyệt
- **Người duyệt:** Tuấn, **duyệt chéo:** Nhật
- **Ghi chú:** 2026-10-31, Nhật trả lời trong phiên, AI ghi. Câu 6: chấp nhận khe hiện cả lúc màn chi tiết đang tải. `poses_in_collection` tách sang T-080 theo lựa chọn của Nhật.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
