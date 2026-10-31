---
ticket: T-080
title: Ô quảng cáo trong lưới thư viện (`poses_in_collection`, tắt lúc phát hành); khung bỏ ô khi pool cạn
status: draft                # draft | approved | stopped | done
version: 3                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 39.4M        # cỡ L theo số gợi ý của scripts/token-usage.py estimate (2026-10-31); không nâng tay
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-080: Ô QUẢNG CÁO TRONG LƯỚI THƯ VIỆN; KHUNG BỎ Ô KHI POOL CẠN

## 1. Tóm tắt

Lưới thư viện có một ô quảng cáo native (`poses_in_collection`, định dạng `native_in_collection`) sau mỗi `native_every_poses_in_collection` tư thế (mặc định 8), tắt lúc phát hành, bật ở bản staging. Hàm lưới của `:core:ads` đổi để không đặt ô một-cột khi pool cạn và ô không giữ quảng cáo nào: lưới không còn ô trống (spec poses KB-6, tiêu chí 5). Ô đang giữ quảng cáo vẫn ở lại; pool có quảng cáo lại thì ô quay lại sau tối đa 2 giây. Danh sách và hàng toàn chiều rộng không đổi.

Kiểm chứng gồm:

- test Robolectric của khung (pool cạn, ô đang giữ, ô quay lại, khóa tắt, danh sách và hàng toàn chiều rộng không đổi, 17 thẻ có hai ô);
- test giao diện của thư viện ở 2 và 3 cột, mọi bộ lọc;
- ảnh chuẩn `PosesScreen_ad-collection_light` so với ảnh thiết kế;
- test giá trị staging và bảng khóa sinh lại.

**Phiên bản 3 (2026-10-31).** Rà soát độc lập trước RUN-20261031-02 ra 1 điểm chặn, 4 điểm nên sửa (mục 8, sau phần của phiên bản 2), nên phiên bản 2 chưa đủ tốt.

**Điểm chặn:** test chiều cao ở 3 cột so ô quảng cáo với thẻ cùng hàng. Ở `w673dp` xxhdpi, lưới chia cột [533, 533, 532] px vì `GridCells.Fixed` dồn phần dư vào các cột đầu. Ô ở cột 3 vì vậy thấp hơn thẻ cùng hàng 2 px (0,76 dp), vượt sai số 0,5 dp, nên test đỏ dù code đúng. Phương án dự phòng (đo khung xương vô hình) cũng lệch đúng như thế.

Thay đổi so với phiên bản 2:

- **Mục 4, sửa điểm chặn:** test chiều cao so ô với `oneLineHeight` và khung xương đo ở chính bề ngang của ô, và với thẻ tên một dòng cùng cột. Hai khung là hai hàm, mỗi hàm một `@Config`. Lệch tới 1 cột (1 px) giữa ô và thẻ cạnh nó là cách lưới tự chia, không phải lỗi, nên không khẳng định ô bằng thẻ khác cột.
- **Khung rỗng của bước 3:** thêm hai test giữ hành vi kèm phép đột biến. Ghi khung rỗng của `poolExhausted` trả `false`.
- **Test sự kiện:** nêu rõ cách dựng và số lần mong đợi.
- **Đồng hồ:** dừng đồng hồ (`mainClock.autoAdvance = false`) ở các test dựa vào thời gian.
- **Mục 3.1:** chỗ khai báo các thuộc tính mới của `InListAds`, trước `init`.
- **Ghi chú của rà soát:** truyền `cardHeight` vào khung chờ tĩnh ở `NativeAdCell.kt`; sửa câu chữ "cách 1 dp" thành 2 dp; dùng một tên test ở bảng.

**Phiên bản 2 (2026-10-31, duyệt cùng Nhật).** Phiên bản 1 chưa đủ tốt vì hai lý do:

- **Một điểm chặn của rà soát độc lập (mục 8, điểm 4):** phần đối chứng `false` của `poolExhausted_noPool_isTrue` không dựng được.
- **Người duyệt chọn cách khác cho câu 1:** ô quảng cáo cao bằng thẻ tư thế có tên một dòng, thay vì giữ 300 dp.

Thay đổi so với phiên bản 1:

- **Khung:**
  - Hàm lưới công khai nhận `adCellHeight`.
  - `NativeAdCard`, `NativeAdPlaceholder` và đường vẽ ô của pool nhận `cardHeight` cho cỡ `CARD`.
- **`:core:designsystem`:** thêm `PoseCardDefaults.oneLineHeight(width)`.
- **Chạy thử mới (mục 4.1, lần 2).**
- **Sửa theo bảy điểm "nên sửa" của rà soát (mục 8, điểm 5 tới 11):**
  - thêm `AdUnitIdsTest`, `ads-guide.md`, tên ảnh `_compare`, `summary.md` vào 3.2;
  - test khung rỗng và phép đột biến ở mục 4;
  - phần nối ở `PosesRoute` vào ngoại lệ viết test trước;
  - sự kiện lặp vào 3.5;
  - số đo theo ba bề ngang.
- **Câu 2 và 3 giữ theo đề xuất.**

Plan này nhận các điểm của rà soát độc lập trước RUN-20261031-01 mà danh sách chạy chuyển sang T-080 (`docs/runs/RUN-20261031-01.md`, bảng rà soát): chữ ký `itemsWithFixedAds` khớp hàm `internal`; 17 thẻ cho hai ô ở vị trí [8, 17]; `AdPlacementTest.inListGalleryPlacements_declareNativeEvery8` đỏ khi thêm vị trí; chiều cao ô `CARD`; test "khóa tắt thì không có ô".

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/poses/spec.md` (confirmed 2026-10-30), cùng câu 9 và 11 của `features/ads/spec.md` (confirmed 2026-10-30):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-6, tiêu chí 5 | Khi khóa `is_show_poses_in_collection` bật thì lưới có một ô quảng cáo sau mỗi `native_every_poses_in_collection` tư thế (mặc định 8), không ở đầu, không ở cuối lưới; thẻ quảng cáo có nhãn quảng cáo và khác rõ thẻ tư thế; hết quảng cáo thì ô ẩn, lưới không có ô trống. Áp dụng cho "Tất cả" và từng nhóm, cả khi mở gập (3 cột) | `rememberInListAds(AdPlacement.POSES_IN_COLLECTION)` ngoài lưới, `itemsWithInListAds(cards, ads, columns)` của `LazyGridScope`: ô chiếm một cột, N đếm theo tư thế (`gridInListPositions`). "Hết quảng cáo" là pool của vị trí cạn (`NativeAdPool.exhausted`, P-05). Lúc đó ô không giữ quảng cáo bị bỏ khỏi lưới, các thẻ sau dồn lên. Một hàm lưới cho mọi bộ lọc, kể cả "Đã lưu" (câu 1 của plan T-079 phiên bản 1, Nhật trả lời 2026-10-31). Nhãn "Ad" và viền mảnh có sẵn trong `NativeAdCard` cỡ `CARD`. |
| KB-6, tiêu chí 6 | Khi khóa tắt thì lưới không có ô quảng cáo, thứ tự tư thế giữ như KB-1 | Khóa tắt thì `InListAds.every` là `null`, không có vị trí nào; đã đúng trên khung hiện nay, plan thêm test cho lưới một-cột (rà soát của RUN-20261031-01). |
| Spec ads, câu 9 | `poses_in_collection`: ô native trong lưới thư viện, sau mỗi 8 tư thế, không ở đầu, không ở cuối; pool cạn thì ẩn ô; thẻ quảng cáo khác rõ thẻ tư thế; tắt lúc phát hành | Một dòng `AdPlacement`, `keepSpaceWhenPoolEmpty` mặc định (`false`). `AdsSettings` tự sinh hai khóa. Mặc định tắt (ADR-0014). |
| Spec ads, câu 11 | Bản staging bật `is_show_poses_in_collection` | `appPlacementsShown()` thêm `POSES_IN_COLLECTION`; `StagingRemoteValuesModuleTest` kiểm cả tập. |

"Không ở cuối" theo `inListAdPositions`: ô không đứng sau tư thế cuối cùng. Với 17 tư thế và N = 8, có hai ô ở chỉ số [8, 17] trong danh sách gộp (sau tư thế 8 và sau tư thế 16; tư thế 17 vẫn đứng sau ô thứ hai). Với 16 tư thế chỉ có một ô ở chỉ số 8.

Thiết kế tham chiếu: `features/poses/design/README.md` (mục 3, câu về frame `ad-collection`; mục 4, dòng "Ô quảng cáo trong lưới"; mục 5). Ảnh trong `design/screens/`: `PosesScreen_ad-collection_light` (xuất 2026-10-30, PR #253). Số liệu trong `design/context/PosesScreen_ad-collection_light.md`:

- `NativeAdCard` ở vị trí 16,484, cỡ 180 x 284, nền `surfaceContainerLow`, viền `outlineVariant` dày 1, bo góc 12, lề trong 8 (`Spacing.sm`); nhãn "Ad" `labelSmall`.
- Thẻ tư thế cùng hàng ở 212,484, cỡ 182 x 284, viền 2. Hàng cách hàng trên 16 (`Spacing.md`).
- Frame không vẽ thanh đáy (README, mục 3).

ADR liên quan: ADR-0001 (phụ thuộc module), ADR-0013 (vị trí theo spec), ADR-0014 (vị trí mặc định tắt), ADR-0020 (ảnh thiết kế có screenshot cùng tên), ADR-0023 (tên khóa), ADR-0026 (khung AdsKit PARTNER), ADR-0029 (cập nhật dữ liệu của test cũ), ADR-0031 (hành vi đã có bị đổi), ADR-0032 (đẩy nhánh).

## 3. Giải pháp

### 3.1. Cách làm

Một PR, nhánh `claude/T-080-o-quang-cao-trong-luoi` từ nhánh chính (đã có T-079, PR #256, #257).

**Khung: lưới bỏ ô một-cột khi pool cạn (`InListAds.kt`, `AdsController.kt`).** Đây là phần chạy thử ở mục 4.1, code dưới đây là code đã chạy thử.

Cách biết "ô không có quảng cáo để hiện": trạng thái chung của pool, cộng trạng thái của từng ô.

- Một ô của pool chỉ hiện `Unavailable` khi pool cạn (`pooledCellStates`, P-05). Khi một ô báo `Unavailable` và `poolExhausted()` trả `true`, `InListAds` đặt `poolEmpty = true`.
- Khi `poolEmpty` là `true`, một vòng chờ chạy trong scope của màn hình. Cứ `PoolCellPolicy.EXHAUSTED_RETRY_MILLIS` (2 giây, như ô của pool, P-05) nó hỏi lại `poolExhausted()`. Pool không còn cạn thì đặt `poolEmpty = false` và dừng.
- Ô đang hiện quảng cáo (`Loaded`) ghi vào `holding[slot]`. Ô rời composition hay đổi sang trạng thái khác thì xóa.
- Hàm lưới chỉ đặt ô của `slot` khi `cellShown(slot)`: `!poolEmpty || holding[slot] == true`. Ô đang giữ quảng cáo vì vậy không bị gỡ.
- `poolEmpty` và `holding` là state của snapshot (`mutableStateOf`, `mutableStateMapOf`). Nội dung của `LazyVerticalGrid` đọc chúng khi dựng danh sách mục, nên lưới dựng lại khi chúng đổi, như `ads.every` hiện nay.

Thêm vào `InListAds`. Các thuộc tính mới (`poolEmpty`, `holding`, `poolWatch`) khai báo **trước khối `init`**, cạnh `slots` và `loads`: `init` gọi `release()` qua `snapshotFlow`, và với scope chạy ngay (`Dispatchers.Unconfined`) cùng `every = null` từ đầu, `release()` sẽ gặp thuộc tính chưa khởi tạo nếu chúng khai báo sau.

```kotlin
    /** Whether the pool of [placement] is exhausted now (P-05); null without a pool. */
    internal val poolExhausted: (() -> Boolean)? = null,   // new constructor parameter, last

    // Grid cells of a card (KB-6, criterion 5): no empty cell while the pool is exhausted.
    private val poolEmpty = mutableStateOf(false)
    private val holding = mutableStateMapOf<Int, Boolean>()
    private var poolWatch: Job? = null

    /** A one-cell grid ad of [slot] is laid out: the pool may still give an ad, or the cell holds one. */
    internal fun cellShown(slot: Int): Boolean = !poolEmpty.value || holding[slot] == true

    /** A pooled cell of [slot] has a new state. */
    internal fun onCellState(slot: Int, state: NativeAdState) {
        if (state is NativeAdState.Loaded) holding[slot] = true else holding.remove(slot)
        if (state == NativeAdState.Unavailable && poolExhausted?.invoke() == true) {
            poolEmpty.value = true
            if (poolWatch?.isActive != true) {
                poolWatch = scope.launch {
                    while (poolEmpty.value) {
                        delay(PoolCellPolicy.EXHAUSTED_RETRY_MILLIS)
                        if (poolExhausted.invoke() != true) poolEmpty.value = false
                    }
                }
            }
        }
    }

    /** The pooled cell of [slot] left the composition: its ad went back to the pool. */
    internal fun onCellLeft(slot: Int) {
        holding.remove(slot)
    }
```

`release()` (khóa tắt giữa chừng, rời màn hình) thêm: hủy `poolWatch`, `poolEmpty.value = false`, `holding.clear()`. Bật lại khóa thì ô quay lại như lần đầu.

`rememberInListAds` truyền `poolExhausted`, xuống dòng trước `?.let` để qua ktlint (dòng quá 120 ký tự, lần chạy `check.sh` đầu của mục 4.1):

```kotlin
            poolExhausted = controller?.takeIf { placement.format.usesPool }
                ?.let { ads -> { ads.poolExhausted(placement) } },
``` `AdsController` thêm:

```kotlin
    /** P-05 for the cells of [placement]: true when it has no pool (no ad unit), like [pooledCell]. */
    internal fun poolExhausted(placement: AdPlacement): Boolean = nativePools.pool(placement)?.exhausted ?: true
```

Hàm này đặt sau `pooledCell` (bản chạy thử đặt trước, làm KDoc của `pooledCell` lệch chỗ; bản thật sửa).

`InListAdRow`, nhánh có pool, thêm `onStateChange = { ads.onCellState(slot, it) }` cho `NativeAdCellHost` và `DisposableEffect(ads, slot) { onDispose { ads.onCellLeft(slot) } }`.

Hàm `internal` của lưới nhận thêm `adShown: (slot: Int) -> Boolean = { true }`. Nhánh `InListRun.Ad` chỉ gọi `item(...)` khi `adShown(run.slot)`. `forEachInListRun` tính số mục trước mỗi ô bằng `position - slot`, không phụ thuộc ô trước có được đặt hay không, nên bỏ một ô không làm lệch các mục sau (chạy thử: tư thế 4 dồn lên cạnh tư thế 3). Khóa của ô còn lại giữ `InListAdKey(placementKey, slot)` như cũ.

Hàm lưới công khai truyền `adShown = if (gridAdSpansOneCell(format)) ads::cellShown else { _ -> true }`. Như vậy:

- hàng toàn chiều rộng của lưới (`native_medium`, `native_small`) không đổi: vẫn đặt hàng, cao 0 khi không có quảng cáo;
- hai hàm `LazyListScope.itemsWithInListAds` không đổi: không gọi `cellShown`.

**Khung: ô một-cột không có lề dọc riêng, cao theo thẻ của app (câu 1, phiên bản 2).**

`InListAdRow` hiện bọc quảng cáo bằng `padding(vertical = Spacing.md)`, để cách các hàng của danh sách 16 dp. Trong lưới, `verticalArrangement = spacedBy(Spacing.md)` đã tạo khoảng cách đó.

`LazyVerticalGrid` không kéo các mục của một hàng cho cao bằng nhau. Thẻ tư thế có tên 1 hoặc 2 dòng, nên các thẻ cùng hàng đã có thể lệch nhau. Vì vậy, theo Nhật: ô quảng cáo cao bằng thẻ có tên một dòng (chiều cao của `PoseCardSkeleton`), tính theo bề ngang ô và cỡ chữ.

- `InListAdRow` nhận `contentPadding: PaddingValues = PaddingValues(vertical = Spacing.md)` và `cardHeight: Dp? = null`. Hai nhánh của nó truyền `cardHeight`:
  - nhánh pool: `NativeAdCellHost(cardHeight)`, `BoundNativeAd(cardHeight)`;
  - nhánh `InListAdLoader`: `NativeAdPlaceholder(cardHeight)`, `BoundNativeAd(cardHeight)`.
- Hàm lưới công khai nhận `adCellHeight: (@Composable (cellWidth: Dp) -> Dp)? = null`. Với định dạng một-cột, ô được bọc bằng `BoxWithConstraints`, rồi gọi `InListAdRow(ads, slot, contentPadding = PaddingValues(), cardHeight = adCellHeight?.invoke(maxWidth))`. Hàng toàn chiều rộng giữ `InListAdRow(ads, slot)` như cũ.
- `cardHeight` cũng vào khung chờ tĩnh của `NativeAdCellOnScreen` (nhánh `Unavailable` với `keepSpaceWhenEmpty`, `NativeAdCell.kt`). Lưới không đi qua nhánh này (`InListAdRow` truyền `keepSpaceWhenEmpty = false`); truyền để mọi chỗ vẽ cỡ `CARD` nhận cùng chiều cao (ghi chú của rà soát, phiên bản 3).
- `cardHeight` đi qua `NativeAdCellHost`, `NativeAdCellOnScreen`, `NativeAdSlotContent`, tới `NativeAdPlaceholder` (công khai, tham số mới cuối, mặc định `null`). Nó cũng đi qua `BoundNativeAd`, `NativeAdLayout`, tới `NativeAdCard` (công khai, tham số mới cuối). Chỉ cỡ `CARD` đọc nó: `Modifier.height(cardHeight ?: CardHeight)`. Bố cục `CardLayout` đã cho media `weight(1f)`, nên media co giãn theo chiều cao.
- Truyền bằng tham số, không bằng `CompositionLocal`: quảng cáo thật vẽ trong một `ComposeView` riêng (`NativeAdViewHost`), test trên JVM không đi qua đường đó.
- Không truyền chiều cao thì vẫn 300 dp (`NativeAdDimens.CardHeight`), như trang của pager (`GALLERY_IN_COLLECTION`) và mọi app khác.

**`:core:designsystem`: chiều cao thẻ tên một dòng.** Thêm vào `PoseCard.kt`:

```kotlin
/** Sizes of [PoseCard] that other components match. */
object PoseCardDefaults {
    /**
     * Height of a [PoseCard] [width] wide whose name takes one line, at the current font scale: the height of a
     * [PoseCardSkeleton]. A grid cell next to the cards (an ad card, plan T-080) takes it so its row stays even.
     */
    @Composable
    fun oneLineHeight(width: Dp): Dp {
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val typography = MaterialTheme.typography
        fun lineHeight(style: TextStyle): Int = measurer.measure(" ", style, maxLines = 1).size.height
        return with(density) {
            val frame = (Stroke.sticker + Spacing.sm).roundToPx()
            val image = ((width.roundToPx() - 2 * frame) / IMAGE_ASPECT_RATIO).roundToInt()
            val text = lineHeight(typography.titleMedium) + (Spacing.xs / 2).roundToPx() +
                lineHeight(typography.bodySmall) + Spacing.xs.roundToPx()
            (2 * frame + image + Spacing.sm.roundToPx() + text).toDp()
        }
    }
}
```

Tính theo điểm ảnh như bố cục thật: khung 2 + 8 mỗi phía, ảnh 162 : 210, khoảng cách 8, hai dòng chữ cách 2 dp (`Spacing.xs / 2`), lề dưới 4. Hàm nằm cạnh `PoseCard`, nên đổi bố cục thẻ thì đổi cùng file; test của mục 4 so hàm với thẻ thật.

Ô ở ba bề ngang (chạy thử, cỡ chữ 1.0):

| Khung | Bề ngang ô | Ô quảng cáo |
| --- | --- | --- |
| 360 dp, 2 cột | khoảng 156 dp | khoảng 250 dp |
| 411 dp, 2 cột | 182 dp | 284 dp, như thiết kế |
| 673 dp, 3 cột | khoảng 203 dp | khoảng 312 dp |

Ở cỡ chữ 2.0, ô cao theo thẻ (318 dp ở 182 dp).

**Khung: ô cố định cho screenshot (`itemsWithFixedAds`).** Feature không tạo được `InListAds` giả (hàm dựng `internal`), và không có controller thì không có ô. Thêm hàm công khai, không trạng thái, cùng kiểu với `BottomAdBarLayout` và `NativeAdCard`. Chữ ký khớp hàm `internal` (`adContent` nhận `slot`, không có receiver), sửa điểm rà soát của RUN-20261031-01:

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
    adContent: @Composable (slot: Int) -> Unit,
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit,
) = itemsWithInListAds(
    items = items,
    positions = gridInListPositions(format, items.size, every, columns),
    placementKey = FIXED_ADS_KEY,
    key = key,
    contentType = contentType,
    adSpansOneCell = gridAdSpansOneCell(format),
    adContent = adContent,
    itemContent = itemContent,
)
```

`FIXED_ADS_KEY = "fixed"` là hằng `private`. Vị trí lấy từ `gridInListPositions`, cỡ ô từ `gridAdSpansOneCell`, nên luật vị trí chỉ có một chỗ (L-072 của mẫu). Hàm chung cho mọi app, không mang tên tính năng (MC-10).

Ba phần sửa khung ở trên là của mọi app. Ghi đề xuất về dự án mẫu trong mô tả PR (`docs/ops/from-app.md`): `core/ads` đồng bộ từ mẫu, nên phần này có thể bị ghi đè ở lần đồng bộ sau nếu mẫu chưa nhận.

**`AdPlacement`.** Thêm một dòng cạnh các vị trí của app:

```kotlin
POSES_IN_COLLECTION("poses_in_collection", AdFormat.NATIVE_IN_COLLECTION, nativeEvery = 8),
```

`AdsSettings` tự sinh `is_show_poses_in_collection` (mặc định tắt) và `native_every_poses_in_collection` (8). `keepSpaceWhenPoolEmpty` để mặc định. Pool của định dạng: 5 quảng cáo, hết hạn 55 phút (spec poses mục 5), là `poolCapacity()` có sẵn của `NATIVE_IN_COLLECTION`.

**Thư viện (`PosesScreen.kt`).**

- Thêm `sealed interface PosesGridAds`:
  - `None`;
  - `Live(val ads: InListAds)`;
  - `Fixed(val every: Int, val cell: @Composable (Modifier, Dp) -> Unit)`, chỉ cho screenshot và test.
- `PosesScreen` nhận `gridAds: PosesGridAds = PosesGridAds.None`, truyền xuống `ReadyContent`.
- `PosesRoute` gọi `val gridAds = rememberInListAds(AdPlacement.POSES_IN_COLLECTION)` ngoài `BottomAdBarScaffold` và truyền `PosesGridAds.Live(gridAds)`.
- Trong `ReadyContent`, `items(state.cards, key = { it.id }) { ... }` đổi thành một hàm mở rộng `private fun LazyGridScope.posesGridItems(cards, gridAds, columns, cardContent)`:
  - `None`: `items(cards, key = { it.id })` như cũ;
  - `Live`: `itemsWithInListAds(cards, ads, columns, key = { it.id }, adCellHeight = { PoseCardDefaults.oneLineHeight(it) })`;
  - `Fixed`: `itemsWithFixedAds(cards, AdFormat.NATIVE_IN_COLLECTION, every, columns, key = { it.id }, adContent = { BoxWithConstraints { cell(Modifier.testTag(PosesTestTags.AD_CELL), PoseCardDefaults.oneLineHeight(maxWidth)) } })`.
- `Fixed.cell` có kiểu `@Composable (Modifier, cardHeight: Dp) -> Unit`.
- `columns` là `gridColumns()` đang có, tính trong `BoxWithConstraints` (2 hay 3, KB-7).
- `LoadingGrid` không có ô quảng cáo.
- Thêm `PosesTestTags.AD_CELL = "poses_ad_cell"`. Ô thật mang `testTag` của khung (`inlist_ad_<slot>`).

**Giữ chỗ cuộn khi quay lại từ chi tiết (KB-2).** `gridState` lưu chỉ số và khóa của mục đầu trong danh sách gộp. N không đổi và pool không đổi trạng thái thì vị trí giữ đúng. Ô quảng cáo vừa xuất hiện hay vừa bị bỏ (pool cạn) có thể làm lưới lệch một ô. Ticket chấp nhận lệch một ô (câu 3 của plan T-079 phiên bản 1).

**Bản staging (`StagingRemoteValuesModule.kt`).** `appPlacementsShown()` thêm `AdPlacement.POSES_IN_COLLECTION`. Không đặt `native_every_poses_in_collection`: mặc định 8.

**Bảng khóa.** Chạy `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`. `docs/remote-config-keys.md` và `docs/remote-config-template.json` thêm `is_show_poses_in_collection` và `native_every_poses_in_collection` (8). Không sửa tay.

**Ảnh chuẩn và so thiết kế.**

- Ảnh mới `PosesScreen_ad-collection_light.png`, khung chuẩn `w411dp-h891dp-xxhdpi`, sáng.
- Lưới vẽ với `rememberLazyGridState(initialFirstVisibleItemIndex = 6)`, không có thanh đáy (README, mục 3).
- 12 tư thế theo `design/context/PosesScreen_ad-collection_light.md`: thẻ 7 cặp đôi `#3DDC97`, thẻ 8 gia đình `#5ED3F3`, thẻ 9 nữ `#FF5C8A`. `defaultState()` chỉ có 6 thẻ, nên test dựng danh sách riêng.
- `PosesGridAds.Fixed(every = 8) { modifier, height -> NativeAdCard(SCREENSHOT_CARD_AD, NativeAdSize.CARD, modifier, cardHeight = height) }`. Ô cao 284 như thiết kế.
- Tài sản giả như ảnh thiết kế: "Headline of the ad", "Advertiser or short body", "Install", `hasMedia = true`.
- Chạy `scripts/design-compare.sh poses`. Ảnh so sánh là `docs/features/poses/design/review/PosesScreen_ad-collection_light_compare.png`, cùng `summary.md` sinh lại. Ảnh `_compare` cũ mà chỉ đổi byte thì khôi phục (L-110).

**Tài liệu.**

- `docs/ops/ads-guide.md`:
  - dòng 48 ("chưa có vị trí của app") ghi `poses_in_collection`;
  - dòng 117 ("pool cạn thì hàng cao 0") thêm ngoại lệ ô một-cột của lưới;
  - dòng 124, 128 thêm `itemsWithFixedAds` và `adCellHeight`;
  - dòng 125 ghi `keepSpaceWhenPoolEmpty` không áp cho ô một-cột của lưới khi pool cạn.
- `docs/ops/manual-test.md` mục 11 (Quảng cáo) thêm một bước: trên bản staging, ô quảng cáo sau tư thế 8 ở "Tất cả" và ở một nhóm có từ 9 tư thế; thẻ có nhãn "Ad"; không bấm quảng cáo; tắt mạng sau khi pool đã hết quảng cáo thì không còn ô trống (popup mất mạng của `:app` hiện, ADR-0024).

**Sau khi merge (việc của người, không nằm trong PR).** Chạy `python3 scripts/ads-plan.py --keep <file trên Drive>`; dòng `poses_in_collection` vào danh sách mã cần tạo của T-075.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InListAds.kt` | `poolExhausted`, `poolEmpty`, `holding`, `cellShown`, `onCellState`, `onCellLeft`, `release` dọn thêm; `adShown` của hàm lưới `internal`; `contentPadding`, `cardHeight` của `InListAdRow`; `adCellHeight` của hàm lưới công khai; `itemsWithFixedAds` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdCard.kt` | `cardHeight` của `NativeAdCard`, `NativeAdPlaceholder`, `NativeAdLayout` (chỉ cỡ `CARD`) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdCell.kt` | `cardHeight` của `NativeAdCellHost`, `NativeAdCellOnScreen` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdSlot.kt` | `cardHeight` của `NativeAdSlotContent` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdViewBinding.kt` | `cardHeight` của `BoundNativeAd(ad, size, ...)` |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PoseCard.kt` | `PoseCardDefaults.oneLineHeight` |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/PoseCardTest.kt` | Test của `oneLineHeight` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | Thêm `poses_in_collection=unit/12` vào hai chuỗi cấu hình của `configuredUnits_areParsedPerPlacementKey` (dữ liệu, ADR-0029; mục 3.5) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsController.kt` | `poolExhausted(placement)` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | `POSES_IN_COLLECTION` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InListAdsTest.kt` | Test mới của mục 4 (khung) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsControllerNativeComboTest.kt` | Test của `poolExhausted` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementAppTest.kt` | Dòng `poses_in_collection` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementTest.kt` | Thêm `POSES_IN_COLLECTION` vào tập `inList` của `inListGalleryPlacements_declareNativeEvery8` (dữ liệu, ADR-0029; mục 3.5) |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreen.kt` | `PosesGridAds`, `posesGridItems`, `rememberInListAds`, `AD_CELL` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenTest.kt` | Test ô quảng cáo của mục 4 |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenScreenshotTest.kt` | Ảnh `ad-collection` |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_ad-collection_light.png` | Ảnh chuẩn mới |
| Tạo | `docs/features/poses/design/review/PosesScreen_ad-collection_light_compare.png` | Ảnh so sánh của `scripts/design-compare.sh poses` |
| Sửa | `docs/features/poses/design/review/summary.md` | Sinh lại cùng lệnh |
| Sửa | `docs/ops/ads-guide.md` | Vị trí của app, ngoại lệ ô một-cột khi pool cạn, `itemsWithFixedAds`, `adCellHeight` |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModule.kt` | `appPlacementsShown()` thêm `POSES_IN_COLLECTION` |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModuleTest.kt` | Tập giá trị mong đợi thêm `is_show_poses_in_collection=true` (mục 3.5) |
| Sửa | `docs/remote-config-keys.md` | Sinh lại bằng `-PremoteConfigKeys.update=true` |
| Sửa | `docs/remote-config-template.json` | Sinh lại cùng lệnh |
| Sửa | `docs/ops/manual-test.md` | Mục 11: bước ô trong lưới |

Nơi khác đọc các file bị đổi (L-020 của mẫu):

- `scripts/ads-plan.py` đọc `AdPlacement` và `docs/remote-config-keys.md`. Test của script đọc code thật (`scripts/tests/run.sh`, khối `ads-plan`), không đếm số vị trí cố định, nên không sửa.
- `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` so `docs/remote-config-keys.md` và `docs/remote-config-template.json` với khóa của code. Tập khóa lấy từ `AdsSettings.all()` và `AdPlacement.entries`, không ghi tên vị trí, nên không sửa test. Hai tệp được sinh lại trong cùng commit code, nên test qua (như T-079).
- `app/build.gradle.kts` chỉ nêu đường dẫn của hai file bảng khóa cho tham số `-PremoteConfigKeys.update`. Đường dẫn không đổi, nên không sửa.
- `AdPlacementSourceTest` và `StagingOnlyEntriesTest` quét `AdPlacement`: dòng mới không phải dòng chỉ-staging, không có mã quảng cáo, nên không đổi.
- `scripts/check.sh` và `design-compare.sh` đọc `src/test/screenshots/` và `design/screens/`. Sau task, mọi ảnh thiết kế của poses có screenshot test cùng tên.
- `docs/features/poses/design/review/summary.md`: `scripts/design-compare/DesignCompare.java` ghi file này, plan không sửa script, chỉ chạy nó. `scripts/device-session.sh` và `.github/workflows/device-session.yml` đọc một `summary.md` khác (thư mục `test-sessions/` của phiên thử trên máy), không đọc file của gói thiết kế, nên không sửa.
- `NativeAdCard`, `NativeAdPlaceholder` có tham số mới cuối, mặc định `null`: mọi chỗ gọi cũ (`:core:ads`, ảnh chuẩn của thanh đáy ở `:feature:poses`) không đổi, nên ảnh chuẩn cũ giữ nguyên.
- Không có feature nào khác gọi hàm lưới của khung (`grep -rn itemsWithInListAds feature app`: không có kết quả ngày 2026-10-31), nên phần bỏ lề dọc của ô một-cột chỉ ảnh hưởng test của `:core:ads`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Vị trí bật tắt bằng khóa Remote Config của khung.
- Quảng cáo: `poses_in_collection` (spec ads câu 9; spec poses mục 5, KB-6), mặc định tắt (ADR-0014), bật ở staging (spec ads câu 11). Khóa `is_show_poses_in_collection`, `native_every_poses_in_collection` theo ADR-0023, sinh từ `AdPlacement`.
- Sự kiện phân tích: Không thêm. Sự kiện quảng cáo của khung (`ad_opportunity`, `af_native_ad_eligible`) tự ghi cho vị trí mới.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `mutableStateMapOf` và `delay` thuộc `androidx.compose.runtime` và `kotlinx-coroutines-core`, đã có trên classpath biên dịch của `:core:ads` (chạy thử biên dịch qua).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** lưới có thể dựng lại liên tục khi pool dao động giữa "đang tải" và "cạn" (ô hiện khung chờ rồi biến mất mỗi 2 giây), hoặc test Robolectric chờ mãi vì vòng chờ của `poolWatch`.
- **Điều kiện chuyển:** test `grid_inCollection_poolHasAdsAgain_cellReturns` hay `grid_inCollection_poolExhausted_noEmptyCell` vẫn thất bại vì cùng một lỗi (hết giờ chờ của `waitForIdle`, hay ô xuất hiện rồi biến mất trong cùng lần kiểm) sau 2 lần sửa.
- **Điểm chưa chắc thứ hai (phiên bản 2):** trên máy thật, `oneLineHeight` có thể lệch thẻ thật, ví dụ vì phông của hệ thống khác Robolectric.
  - **Điều kiện chuyển:** test `oneLineHeight_matchesSkeletonAndOneLineCard` hay hai test `fixedGridAds_*_cellHeightIsOneLineCardOfItsWidth` vẫn lệch quá 0,5 dp sau 2 lần sửa, khi so ở cùng bề ngang (không so khác cột).
  - **Cách làm thay:** ô đo thẻ thật. Ô bọc một `PoseCardSkeleton` vô hình (alpha 0, không ngữ nghĩa) trong `Box`, quảng cáo nằm trên với `matchParentSize`. `oneLineHeight` không còn dùng. Tham số `cardHeight` của khung giữ, ô truyền chiều cao đo được. Vẫn trong `PoseCard.kt`, `PosesScreen.kt`.
- **Cách làm thay:** bỏ vòng chờ riêng. `cellShown` đọc thêm một `State<Long>` nhịp 2 giây do `rememberInListAds` tạo bằng `produceState` (chạy theo vòng đời composition của màn hình). Mỗi nhịp đọc lại `poolExhausted()`. Ô bị bỏ chỉ quay lại khi `poolExhausted()` trả `false` hai nhịp liền. Test ở mục 4 giữ nguyên; file vẫn là `InListAds.kt` (dự phòng nằm trong cùng file của mục 3.2).

### 3.5. Hành vi đã có bị đổi

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Lưới luôn đặt ô một-cột ở mọi vị trí, kể cả khi ô không có quảng cáo | Pool cạn và ô không giữ quảng cáo thì ô bị bỏ | `InListAdsTest.grid_inCollection_adTakesOneCell_everyNItems` gọi thẳng hàm `internal` với `adContent` giả, không qua `cellShown` | Giữ: tham số mới `adShown` mặc định `{ true }`, nên test không đổi |
| Ô quảng cáo trong lưới có lề dọc 16 dp như hàng của danh sách, thẻ cao 300 | Ô một-cột không có lề dọc (lưới đã có `spacedBy`); cao theo `adCellHeight` khi app truyền, 300 khi không | Không test nào đo lề của ô một-cột (`adRow_isSixteenDpFromItemsAboveAndBelow` đo hàng của `LazyColumn`); `NativeAdCardTest` đo 300 khi không có `cardHeight` | Giữ; test mới `grid_inCollection_cellTakesAppHeight`, `grid_inCollection_noHeight_keeps300` |
| Mỗi lần ô vào lại lưới thì ghi `af_native_ad_eligible` (`InListAds.kt:397`); ô đã thấy mà chưa có quảng cáo rời composition thì ghi `ad_opportunity` với `not_ready` (`NativePoolCell.kt:76-81`) | Không đổi cách ghi. Khi pool dao động (câu 2), ô bị bỏ rồi quay lại, nên hai sự kiện có thể lặp mỗi chu kỳ 2 giây. Nhật chấp nhận (câu 2) | Không test nào đếm sự kiện qua một chu kỳ bỏ rồi quay lại | Giữ; test mới `grid_inCollection_poolCycle_reportsOncePerEntry` ghi rõ số lần |
| `ConfiguredAdUnitIds.missing` rỗng với chuỗi cấu hình của test | Thêm vị trí không phải staging thì `missing` có `poses_in_collection` | `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` | Cập nhật dữ liệu theo ADR-0029: thêm `poses_in_collection=unit/12` vào hai chuỗi, như T-079 làm với `inter_pose_detail` |
| Hàng toàn chiều rộng của lưới và hàng của danh sách khi pool cạn: vẫn đặt, cao 0 | Không đổi | `InListAdsTest.unavailableSlot_takesNoHeight`, `lazyGrid_twoColumnsEveryThree_adSpansFullWidthAfterItem4`, `pooledRows_everyBecomesNull_releaseToPool` | Giữ; thêm hai test đối chứng ở mục 4 |
| Chỉ ba vị trí `GALLERY_*` khai báo `nativeEvery` | Thêm `POSES_IN_COLLECTION` với `nativeEvery = 8` | `AdPlacementTest.inListGalleryPlacements_declareNativeEvery8` (khẳng định mọi vị trí khác có `nativeEvery == null`) | Cập nhật dữ liệu theo ADR-0029: tập `inList` thêm `POSES_IN_COLLECTION`; câu "mọi vị trí còn lại `null`" giữ nguyên; giá trị 8 kiểm ở `AdPlacementAppTest` |
| Staging bật bốn vị trí của app | Bật thêm `poses_in_collection` | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet` | Tập mong đợi thêm `is_show_poses_in_collection=true` (spec ads câu 11). `noGalleryNorSamplePlacement_isShown` giữ nguyên |
| Thư viện vẽ thẻ tư thế bằng `items(cards)` | `posesGridItems` với `PosesGridAds`, mặc định `None` vẽ như cũ | Các test của `PosesScreenTest`, ảnh chuẩn của `PosesScreenScreenshotTest` | Giữ: `PosesScreen` mặc định `None`, ảnh chuẩn cũ không đổi |

`scripts/existing-tests.py` chạy ngày 2026-10-31: 26 test "Cần xem trước", 338 test "Nên xem". Đã đọc nhóm "Cần xem trước" và các test của `InListAdsTest`, `AdPlacementTest`, `AdPlacementAppTest`, `RemoteConfigKeysTest`, `StagingRemoteValuesModuleTest` trong nhóm "Nên xem":

- Phần lớn khớp vì chữ `slot` hay `AdPlacement` (thanh đáy, `NativeAdSlot`, `CollapsibleNativeAd`, màn chi tiết). Các hành vi đó plan không đổi: giữ.
- `InListAdsTest.lazyColumn_sevenItemsEveryThree_adRowsAfterItems3And6`, `alternatingPlacements_takeTurns`: danh sách, không gọi `adShown`: giữ.
- `PosesScreenTest.realBottomBar_withoutAds_takesNoSpace`, `bottomBar_lastCardScrollsAboveSlot`, `saveFailedSnackbar_aboveSlot`, các test `smallScreen_*`: `PosesRoute` thêm `rememberInListAds`, nhưng test không có `LocalAdsController` nên không có ô (`every` là `null`); `PosesScreen` mặc định `None`: giữ.
- `AdPlacementAppTest.appPlacements_declaredWithSpecFormats`: thêm dòng `"poses_in_collection" to AdFormat.NATIVE_IN_COLLECTION` (dữ liệu, ADR-0029); các câu khẳng định của vòng lặp (không `appWide`, không `stagingOnly`, tên khóa `is_show_<key>`) giữ và áp cho dòng mới.
- `AdPlacementTest.galleryPlacements_coverEveryFormatOnce`, `stagingOnly_isExactlyGalleryPrefix`, `otherPlacements_areNotStagingOnly`: lọc theo tiền tố `gallery_` hay kiểm mọi vị trí khác không chỉ-staging; dòng mới thỏa: giữ.
- `RemoteConfigKeysTest.everyFeatureFlagAndPlacement_isListed`, `everyPlacementShowKey_isInTableOff`: đỏ cho tới khi sinh lại bảng khóa, rồi qua; `is_show_poses_in_collection` ghi mặc định tắt: giữ.

## 4. Kiểm thử

Test của khung dùng vị trí của khung `GALLERY_IN_COLLECTION` (N trong test đặt bằng `every`), không dùng vị trí của app, để test giữ được khi đề xuất về dự án mẫu. Pool giả: hàm `cell()` trả `Flow<NativeAdState>` như `FakePooledCells`, cộng `poolExhausted = { exhausted }` đổi được trong test.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-6 tiêu chí 5: hết quảng cáo thì không còn ô trống | Compose UI test (Robolectric) | `InListAdsTest.grid_inCollection_poolExhausted_noEmptyCell`: lưới 2 cột, 7 mục, `every = 3`, hai ô đầu của pool trả `Unavailable`, `exhausted = true`. Không có node `inlist_ad_0`; `top(item_4) == top(item_3)` (tư thế 4 dồn lên cạnh tư thế 3). Đối chứng đã chạy (mục 4.1): bỏ quy tắc thì có node `inlist_ad_0` |
| Ô đang giữ quảng cáo vẫn giữ | Compose UI test | `InListAdsTest.grid_inCollection_cellHoldingAd_staysWhenPoolExhausted`: ô 0 nhận `Loaded`, ô 1 nhận `Unavailable`, `exhausted = true`. Node `inlist_ad_0` có, `inlist_ad_1` không; quảng cáo của ô 0 chưa bị trả (`released` rỗng) |
| Pool có lại thì ô quay lại | Compose UI test | `InListAdsTest.grid_inCollection_poolHasAdsAgain_cellReturns`: như test đầu, rồi `exhausted = false`, `mainClock.autoAdvance = false` ngay sau `setContent`, cuộn tức thì (L-048); `mainClock.advanceTimeBy(EXHAUSTED_RETRY_MILLIS + 100)`. Node `inlist_ad_0` có lại, và khẳng định quảng cáo của ô là của lần gọi `cell()` thứ ba, không phải lần 1 hay 2. Đối chứng: tiến 1000 ms (chưa tới nhịp) thì chưa có |
| Khóa tắt giữa chừng rồi bật lại: ô quay lại | Compose UI test | `InListAdsTest.grid_inCollection_everyBackAfterNull_cellsReturn`: pool cạn, ô bị bỏ; `mainClock.autoAdvance = false`; `every = null` rồi `every = 3`, `exhausted = false`, chỉ `mainClock.advanceTimeByFrame()`: có `inlist_ad_0` ngay (không chờ nhịp 2 giây), vì `release()` đã đặt `poolEmpty = false` |
| KB-6 tiêu chí 6: khóa tắt thì không có ô | Compose UI test | `InListAdsTest.grid_inCollection_everyNull_noCell`: `every = null`, 20 mục, 2 cột; không node nào có tag bắt đầu bằng `inlist_ad_`; thứ tự `item_1` tới `item_20` liên tiếp (`item_3` cạnh `item_4`). Đối chứng cùng test: `every = 8` thì có `inlist_ad_0` |
| Danh sách không đổi | Compose UI test | `InListAdsTest.lazyColumn_pooledRow_poolExhausted_keepsZeroHeightRow`: `LazyColumn`, `GALLERY_INLIST_MEDIUM`, `exhausted = true`, ô `Unavailable`: node `inlist_ad_0` vẫn có, cao 0 (sai số 0,5 dp) |
| Hàng toàn chiều rộng của lưới không đổi | Compose UI test | `InListAdsTest.grid_fullWidthRow_poolExhausted_keepsZeroHeightRow`: lưới 2 cột, `GALLERY_INLIST_MEDIUM`, `exhausted = true`: node `inlist_ad_0` vẫn có, cao 0 |
| 17 thẻ, N = 8: hai ô ở [8, 17] | Unit test | `InListAdsTest.grid_inCollection_seventeenItems_twoCellsAt8And17`: `gridInListPositions(NATIVE_IN_COLLECTION, 17, 8, 2) == [8, 17]`; 16 mục thì `[8]`; 8 mục thì rỗng (không ở cuối) |
| `itemsWithFixedAds` đặt ô như hàm thật | Compose UI test | `InListAdsTest.fixedAds_sameCellsAsLive`: 12 mục, 2 cột, `every = 8`, `NATIVE_IN_COLLECTION`: một ô, `top` bằng `item_9`, ở cột trái; 3 cột: ô ở cột thứ ba, cùng hàng `item_7` và `item_8`; `item_9` ở đầu hàng sau |
| `poolExhausted` của controller | Unit test | `AdsControllerNativeComboTest.poolExhausted_noPool_isTrue`: vị trí không có mã quảng cáo (không pool) trả `true`. Không có đối chứng `false` (điểm chặn của rà soát, Nhật chọn bỏ): `nativePools` là `private`, trên JVM pool luôn đóng; nhánh có pool chỉ đọc `NativeAdPool.exhausted`, đã có test ở `NativeAdPoolTest` |
| Ô một-cột cao theo app | Compose UI test | `InListAdsTest.grid_inCollection_cellTakesAppHeight`: lưới 2 cột, `adCellHeight = { it * 1.5f }`; thẻ `Loaded` và khung chờ `Loading` cao bằng 1,5 lần bề ngang ô (sai số 0,5 dp), ô không có lề dọc. Đối chứng đã chạy (mục 4.1, lần 2): bỏ `adCellHeight` thì đỏ |
| Không truyền chiều cao thì giữ 300 | Compose UI test | `InListAdsTest.grid_inCollection_noHeight_keeps300`: `adCellHeight` mặc định, thẻ cao 300 |
| `oneLineHeight` bằng thẻ thật | Compose UI test (`NATIVE`) | `PoseCardTest.oneLineHeight_matchesSkeletonAndOneLineCard`: bề ngang 156, 182, 203 dp, cỡ chữ 1.0 và 2.0; chiều cao của `PoseCardSkeleton` và của `PoseCard` tên một dòng bằng `oneLineHeight` (sai số 0,5 dp). 182 dp ở cỡ 1.0 cho 284 |
| Sự kiện khi pool dao động | Compose UI test | `InListAdsTest.grid_inCollection_poolCycle_reportsOncePerEntry`: bọc lưới bằng `CompositionLocalProvider(LocalAdsController provides testAdsController(funnel = RecordingAdFunnel()))` như `NativeEligibleReportTest`, vì `af_native_ad_eligible` chỉ ghi qua controller. Đồng hồ dừng. Ô 0 thấy, `Loading`, rồi `Unavailable` với `exhausted = true`: ô bị bỏ; rồi `exhausted = false`, tiến 2100 ms: ô quay lại. Mong đợi đúng 2 `AdFunnelEvent.Eligible` của vị trí (một lần mỗi lần ô vào lưới) và 1 `onPoolOpportunity("not_ready")` (ô đã thấy, rời lưới khi chưa có quảng cáo). Đối chứng: không cho ô quay lại (giữ `exhausted = true`) thì chỉ 1 `Eligible` (câu 2) |
| Spec ads câu 9: vị trí mới | Unit test | `AdPlacementAppTest` thêm vào bảng: `poses_in_collection` là `NATIVE_IN_COLLECTION`, `nativeEvery == 8`, `keepSpaceWhenPoolEmpty == false`; `AdsSettings.placementEnabled(POSES_IN_COLLECTION).default == false` |
| KB-6 tiêu chí 5, thư viện 2 cột | Compose UI test | `PosesScreenTest.fixedGridAds_adCellAfterEighthPose`: 12 thẻ, `PosesGridAds.Fixed(every = 8)`, khung chuẩn; cuộn tới `AD_CELL`; `AD_CELL` ở cột trái, `top` bằng thẻ 9; chỉ một `AD_CELL`; ô cao bằng thẻ tên một dòng cùng hàng (câu 1) |
| Ô cao bằng thẻ ở khung thấp | Compose UI test | `PosesScreenTest.fixedGridAds_small_cellHeightIsOneLineCardOfItsWidth`, `@Config(qualifiers = "w360dp-h640dp-xxhdpi")`: chiều cao `AD_CELL` bằng `oneLineHeight(bề ngang AD_CELL)` (đọc trong cùng composition qua state, sai số 0,5 dp), và bằng thẻ tên một dòng ở cùng cột (cột trái, thẻ 7) |
| Ô cao bằng thẻ khi mở gập | Compose UI test | `PosesScreenTest.fixedGridAds_unfolded_cellHeightIsOneLineCardOfItsWidth`, `@Config(qualifiers = "w673dp-h841dp-xxhdpi")`: ô ở cột 3; chiều cao bằng `oneLineHeight(bề ngang AD_CELL)` và bằng thẻ tên một dòng cùng cột 3 (thẻ 6, hàng trên), sai số 0,5 dp. Không so với thẻ 7, 8 cùng hàng: cột của lưới lệch nhau tới 1 px ([533, 533, 532] px), nên ô có thể thấp hơn thẻ khác cột 2 px; đó là cách lưới chia, không phải lỗi |
| KB-6 tiêu chí 5, mở gập 3 cột | Compose UI test | `PosesScreenTest.fixedGridAds_unfolded_threeColumns`: `@Config(qualifiers = "w673dp-h841dp-xxhdpi")`, 12 thẻ; `AD_CELL` ở cột thứ ba, cùng hàng thẻ 7 |
| KB-6 tiêu chí 5, mọi bộ lọc | Compose UI test | `PosesScreenTest.fixedGridAds_savedAndGroupFilters_haveCell`: `Ready` với bộ lọc một nhóm và với "Đã lưu", mỗi lần 9 thẻ: có `AD_CELL` sau thẻ 8 |
| KB-6 tiêu chí 6, thư viện | Compose UI test | `PosesScreenTest.noGridAds_noAdCell`: `gridAds` mặc định, 12 thẻ: không có `AD_CELL`, thẻ 9 cạnh thẻ 10. `PosesScreen` với `PosesGridAds.Live(rememberInListAds(...))` mà không có `LocalAdsController` thì không có ô: `PosesScreenTest.posesScreen_liveGridAdsWithoutController_noAdCell` |
| Spec ads câu 11: staging bật vị trí | Unit test (testStaging) | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet`: tập mong đợi thêm `is_show_poses_in_collection=true` |
| Bảng khóa khớp code | Unit test có sẵn | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode`, `everyPlacementShowKey_isInTableOff` qua sau khi sinh lại; ở commit test (bước 3) hai test đầu chưa đỏ, chỉ đỏ khi dòng `AdPlacement` vào ở commit code, rồi qua khi sinh lại trong cùng commit |
| Đúng thiết kế | Screenshot test cùng tên, ảnh so sánh `scripts/design-compare.sh poses`, rà soát độc lập theo mục 7 của mẫu thiết kế | `PosesScreen_ad-collection_light.png` |
| Màn hình thấp (L-028) | Screenshot test có sẵn | Trạng thái nhiều nội dung nhất của thư viện là `Ready`, đã có ảnh `_small` từ T-009; ô quảng cáo chỉ thay một thẻ, không đổi chiều cao vùng cuộn đáng kể, nên không thêm ảnh `_small` mới |
| Ô sau tư thế 8 trên bản staging, "Tất cả" và một nhóm | Kiểm tra thủ công (máy thật: quảng cáo thật) | `docs/ops/manual-test.md` mục 11, bước mới. Quảng cáo thử của AdMob không chắc có trên emulator; dev xem trên máy thật, không bấm quảng cáo |
| Không còn ô trống khi pool cạn trên máy | Kiểm tra thủ công (máy thật: quảng cáo thật) | Cùng bước: cuộn qua nhiều ô cho tới khi hết quảng cáo, không thấy ô trống |

Commit test (bước 3) có khung rỗng để test biên dịch được, theo `docs/auto-run.md` bước 3:

- `poolExhausted`, `adShown`, `adCellHeight`, `cardHeight`, `oneLineHeight` có mặt nhưng không làm gì (`oneLineHeight` trả `0.dp`, `cardHeight` bị bỏ qua, `AdsController.poolExhausted` trả `false`);
- `itemsWithFixedAds` gọi hàm `internal` với `adShown = { true }`;
- `PosesGridAds` có ba nhánh, `posesGridItems` vẽ như `None`.

Test mới đỏ ở câu khẳng định trước khi viết code. Dòng `POSES_IN_COLLECTION` và dữ liệu của `AdPlacementTest`, `AdPlacementAppTest`, `AdUnitIdsTest` nhắc hằng enum, nên đi cùng commit có dòng enum.

Chín test giữ hành vi sẽ xanh ngay với khung rỗng:

- `grid_inCollection_everyNull_noCell`, `seventeenItems_twoCellsAt8And17`;
- `lazyColumn_pooledRow_poolExhausted_keepsZeroHeightRow`, `grid_fullWidthRow_poolExhausted_keepsZeroHeightRow`;
- `grid_inCollection_everyBackAfterNull_cellsReturn`, `grid_inCollection_noHeight_keeps300`;
- `noGridAds_noAdCell`;
- `fixedAds_sameCellsAsLive`: khung rỗng của `itemsWithFixedAds` đã là bản cài đặt đầy đủ;
- `posesScreen_liveGridAdsWithoutController_noAdCell`: không có controller thì `every` là `null`.

`poolExhausted_noPool_isTrue` đỏ với khung rỗng (`false`).

Mỗi test giữ hành vi ghi phép đột biến làm nó đỏ (L-108), chạy một lần khi viết, ghi kết quả vào nhật ký:

| Test | Phép đột biến |
| --- | --- |
| `everyNull_noCell` | `adShown` luôn `false`: đỏ ở đối chứng `every = 8` |
| `noHeight_keeps300` | `cardHeight` mặc định 0 |
| `fixedAds_sameCellsAsLive` | `itemsWithFixedAds` truyền `adSpansOneCell = false`, hay `positions` rỗng |
| `posesScreen_liveGridAdsWithoutController_noAdCell` | `posesGridItems` nhánh `Live` vẽ một ô cố định |

Test đặt `exhausted` rồi tiến đồng hồ (L-091 của mẫu): vòng chờ chạy trong scope của màn hình, nên ra nền không dừng nó, nhưng nó chỉ đọc `exhausted` và đổi state; không hiện gì, không có bước chờ có giới hạn trước khi hiện. Không cần test ra nền rồi trở lại.

Ghi ảnh chuẩn mới: `./gradlew :feature:poses:recordRoborazziDebug :feature:poses:testDebugUnitTest --rerun`, rồi `git status --short -- '*/src/test/screenshots/'` chỉ được có `PosesScreen_ad-collection_light.png` (L-114).

**Ngoại lệ viết test trước:**

- **Phần nối ở `PosesRoute`:** `rememberInListAds(AdPlacement.POSES_IN_COLLECTION)` và `PosesGridAds.Live(...)` với `adCellHeight`. Test JVM không có `LocalAdsController`, nên không dựng được ô thật. Test `liveGridAds_withoutController_noAdCell` đổi tên thành `posesScreen_liveGridAdsWithoutController_noAdCell` và chỉ kiểm `PosesScreen`. Phần nối kiểm bằng tay trên bản staging (mục 4, dòng thủ công), như plan T-079 phiên bản 2.
- **Bảng khóa sinh lại**  (`docs/remote-config-keys.md`, `docs/remote-config-template.json`) và bước mới của `docs/ops/manual-test.md`: tài liệu sinh hay viết tay, không có test riêng ngoài test bảng khóa có sẵn. Ảnh chuẩn `PosesScreen_ad-collection_light.png` ghi sau khi code xong.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-31, phần sửa khung của mục 3.1 (`poolExhausted`, `cellShown`, `onCellState`, `onCellLeft`, `adShown` của hàm lưới `internal`, `AdsController.poolExhausted`), trên nhánh tạm `tmp/T-080-try` từ nhánh plan (nhánh chính sau T-079, `86eca05`), worktree riêng, không commit vào nhánh plan, đã xóa. Test tạm `GridPoolEmptyTryTest` (2 test): `poolExhausted_noEmptyCell_andCellBackWhenPoolHasAds` (lưới 2 cột, 7 mục, `every = 3`, `GALLERY_IN_COLLECTION`, hai ô đầu `Unavailable`; không có `inlist_ad_0`, `top(item_4) == top(item_3)`; rồi `exhausted = false`, tiến 2100 ms: `inlist_ad_0` có lại) và `poolExhausted_cellHoldingAd_staysInGrid` (ô 0 `Loaded`, ô 1 `Unavailable`: `inlist_ad_0` có, `inlist_ad_1` không). Chưa chạy thử: `contentPadding` của ô một-cột và `itemsWithFixedAds` (dùng lại hàm `internal` đã có, không phải kỹ thuật mới).
- **Lệnh:** `./gradlew -q :core:ads:testDebugUnitTest --tests '*GridPoolEmptyTryTest*' --tests '*InListAdsTest*'`, rồi `scripts/check.sh` trên nhánh tạm.
- **Kết quả:**
  - Lệnh Gradle: `GridPoolEmptyTryTest` 2/2 qua; `InListAdsTest` 18/18 qua (không test cũ nào đổi).
  - Đối chứng: đổi `adShown` của hàm lưới công khai thành `{ _ -> true }` (như code hiện nay) thì cả 2 test tạm thất bại đúng lý do: `assertDoesNotExist` thấy node `inlist_ad_0` (test 1) và `inlist_ad_1` (test 2).
  - Vòng chờ của `poolWatch` không làm `waitForIdle` hết giờ: test 1 chạy xong cùng lớp trong vài giây.
  - `scripts/check.sh` lần 1 (15:20 tới 15:32 UTC): thất bại ở `:core:ads:ktlintMainSourceSetCheck` (dòng `poolExhausted = ...` của `rememberInListAds` dài 121 ký tự, quy tắc `function-literal`, `argument-list-wrapping`, `max-line-length`) và `ktlintTestSourceSetCheck` (thứ tự import của test tạm). Không test nào đỏ trước đó. Sửa: xuống dòng trước `?.let` (đoạn code ở mục 3.1 đã theo cách này) và `./gradlew :core:ads:ktlintFormat` cho test tạm.
  - `scripts/check.sh` lần 2 (15:33 tới 15:43 UTC): `Cổng chất lượng: QUA`, exit 0. Test JVM 2420, 0 lỗi (`./core/ads` 1102, `./feature/poses` 170, `./app` 101); 138 ảnh screenshot; lint và build staging qua.
  - Nhánh tạm `tmp/T-080-try` và worktree `../plan-T-080-try` đã xóa sau khi chạy.
- **Đã chạy thử lần 2 (phiên bản 2, 2026-10-31):** phần chiều cao ô của câu 1, trên nhánh tạm `tmp/T-080-height-try` từ nhánh chính sau T-079 và T-081 (`7f29ab0`), worktree riêng `../plan-T-080-height-try`, không commit vào nhánh plan.
  - Code đã chạy: `cardHeight` của `NativeAdCard`, `NativeAdPlaceholder`, `NativeAdLayout`, `BoundNativeAd`, `NativeAdCellHost`, `NativeAdSlotContent`; `contentPadding`, `cardHeight` của `InListAdRow` (cả hai nhánh); `adCellHeight` của hàm lưới công khai; `PoseCardDefaults.oneLineHeight`. Đoạn code ở mục 3.1 là code đã chạy.
  - Test tạm:
    - `GridCardHeightTryTest` (3 test): thẻ `Loaded` cao 1,5 lần bề ngang ô, ô không có lề dọc; khung chờ `Loading` cùng chiều cao; không truyền thì 300.
    - `OneLineHeightTryTest` (2 test): bề ngang 156, 182, 206 dp; cỡ chữ 1.0 và 2.0; khung xương và thẻ tên một dòng bằng `oneLineHeight`.
  - Kết quả:
    - **Test tạm:** 5/5 qua. Lần chạy đầu đỏ vì cách dựng test, không phải vì code: cột 6 thẻ cao hơn màn hình làm thẻ sau bị ép thấp; lưới có 2 ô nên lấy node phải theo chỉ số. Sửa test rồi qua. 182 dp cho 284 ở cỡ 1.0 và 318 ở cỡ 2.0.
    - **Test cũ:** `InListAdsTest` 18/18, `NativeAdCardTest` 35/35, `NativeAdCellTest` 8/8 qua.
    - **Đối chứng:** bỏ `adCellHeight` (truyền `cardHeight = null`) thì 2 test chiều cao đỏ, test 300 vẫn xanh.
    - **`scripts/check.sh` trên nhánh tạm:** 16:53 tới 17:03 UTC, `Cổng chất lượng: QUA`, exit 0. Test JVM 2423, 0 lỗi (`./core/ads` 1103, `./core/designsystem` 118, `./feature/poses` 170, `./app` 101). 138 ảnh screenshot, không ảnh chuẩn cũ nào đổi (`cardHeight` mặc định `null`). Lint và build staging qua.
  - Nhánh tạm `tmp/T-080-height-try` và worktree `../plan-T-080-height-try` đã xóa sau khi chạy.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (thêm vị trí `poses_in_collection`, điểm 1); module core dùng chung `:core:ads`, đổi hành vi của hàm lưới của khung (điểm 7). Rủi ro Cao, cần duyệt chéo. Điểm dưới đây chỉ để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | `:core:ads`, `:core:designsystem`, `:feature:poses`, `:app` (staging), tài liệu; thêm hàm công khai `itemsWithFixedAds`, tham số công khai `adCellHeight`, `cardHeight`, `PoseCardDefaults` |
| B. Độ rõ của yêu cầu | 1 | Spec đã chốt, có thiết kế; câu 1 đã trả lời (ô bằng thẻ tên một dòng) |
| C. Khả năng kiểm chứng tự động | 1 | Khung và thư viện có test Robolectric với pool giả; quảng cáo thật trên lưới kiểm bằng tay |
| D. Ảnh hưởng chéo | 2 | Sửa hàm lưới dùng chung của khung (chưa feature nào khác gọi), `AdPlacement.kt`, `StagingRemoteValuesModule.kt` và bảng khóa mà T-079 vừa sửa |
| E. Độ mới của kỹ thuật | 2 | Bỏ mục khỏi lưới theo state của pool, đọc trong lúc dựng danh sách lazy; chiều cao ô tính từ bố cục thẻ của app: chưa có trong dự án; đã chạy thử hai lần (mục 4.1) |
| **Tổng** | 9 (tham khảo) | **Mức: Cao** (điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `InListAds.kt`, `AdsController.kt`, `AdPlacement.kt`, `NativeAdCard.kt`, `NativeAdCell.kt`, `NativeAdSlot.kt`, `NativeAdViewBinding.kt` của `:core:ads` (đồng bộ từ dự án mẫu); `PoseCard.kt` của `:core:designsystem`; `StagingRemoteValuesModule.kt`; `docs/remote-config-keys.md`, `docs/remote-config-template.json`; `docs/ops/manual-test.md`.
- Task khác trong sprint có thể xung đột: T-079 đã merge (PR #256, #257), không còn xung đột. T-075 (mã quảng cáo thật) sửa `AdUnitIds` và đọc danh sách vị trí: chạy T-080 trước để `poses_in_collection` có trong danh sách mã cần tạo. T-081 (bỏ `:feature:sample`, `sample_native_small`) sửa nhiều test của `:core:ads` mà T-080 cũng sửa (`InListAdsTest`, `AdUnitIdsTest`, `AdPlacementTest`): chạy T-080 trước T-081, không cùng lúc. T-086 phụ thuộc T-080 (`PosesScreen.kt`, `AdPlacement.kt`).
- Đề xuất thứ tự chạy: một task, một PR. Chạy trước T-075.
- Phần sửa khung đề xuất về dự án mẫu (`docs/ops/from-app.md`), ghi trong mô tả PR.

## 7. Điều kiện dừng riêng

- Bảng khóa sinh lại có thay đổi ngoài hai khóa `is_show_poses_in_collection`, `native_every_poses_in_collection`: dừng, vì nghĩa là khung hay vị trí khác đã đổi ngoài plan.
- `git status --short -- '*/src/test/screenshots/'` có ảnh chuẩn nào đổi ngoài `PosesScreen_ad-collection_light.png`: dừng, không ghi lại ảnh cũ (mặc định `PosesGridAds.None` và `cardHeight = null` phải giữ mọi ảnh cũ, kể cả ảnh của `:core:ads` và `:core:designsystem`).
- Test cũ nào của `InListAdsTest`, `NativeAdCellTest`, `NativePoolCellTest` đỏ sau khi sửa khung (ngoài `AdPlacementTest` đã nêu ở mục 3.5): dừng; hành vi của danh sách và hàng toàn chiều rộng phải giữ nguyên.

## 8. Câu hỏi cho người duyệt

1. **Chiều cao ô quảng cáo trong lưới.** Thiết kế vẽ ô 180 x 284, bằng thẻ tư thế. `NativeAdCard` cỡ `CARD` của khung cao cố định 300 dp; plan bỏ lề dọc 16 x 2 của ô trong lưới (ô thật còn 300 thay vì 332), nên hàng có ô quảng cáo cao hơn hàng khác 16 dp, thẻ tư thế cùng hàng nằm trên, chừa 16 dp phía dưới. Đề xuất của AI: chấp nhận 16 dp này; ảnh so sánh sẽ ghi lệch. Cách khác: thêm tham số chiều cao cho `NativeAdCard` cỡ `CARD` trong khung (đổi `NativeAdCard.kt` và ảnh chuẩn của khung), hay cho thẻ tư thế cao theo hàng. Hai cách sau thêm phần sửa khung hay đổi ảnh chuẩn cũ, nên không đưa vào plan nếu không được yêu cầu.
2. **Ô quay lại khi pool chỉ đang tải.** Pool cạn rồi gửi được một yêu cầu mới (hết thời gian chờ thử lại) thì `exhausted` là `false`, ô quay lại với khung chờ; nếu yêu cầu thất bại, ô lại biến mất. Lưới có thể đổi bố cục vài lần khi mạng yếu. Đề xuất: chấp nhận (cùng nhịp 2 giây như ô của pool, P-05). Nếu không chấp nhận, phương án ở mục 3.4 (hai nhịp liền) làm chậm việc quay lại.
3. **Đề xuất về dự án mẫu.** Ba phần sửa khung (bỏ ô khi pool cạn, ô một-cột không lề dọc, `itemsWithFixedAds`) là của mọi app. Đề xuất: ghi trong mô tả PR theo `docs/ops/from-app.md`, và người mở đề xuất ở repo mẫu sau khi merge. Đúng không?

Trả lời của Nhật ngày 2026-10-31 (duyệt cùng AI):

- câu 1: ô cao theo hàng, rồi chọn "bằng thẻ tên một dòng" khi AI báo lưới không kéo mục;
- câu 2: chấp nhận, ghi rõ sự kiện lặp;
- câu 3: ghi trong mô tả PR, người mở đề xuất sau;
- điểm chặn 4: bỏ đối chứng `false`.

Phiên bản 2 đã sửa điểm 4 tới 11.

**Rà soát độc lập (RUN-20261031-01, bước 5, 2026-10-31 15:52 UTC, agent ngữ cảnh mới, chỉ đọc): 1 chặn, 7 nên sửa, 7 ghi chú.** Các con số của plan (vị trí ô [8, 17], lưới 2 và 3 cột) và mọi API mà plan dựa vào đều có thật trên `origin/main`. Plan dùng hết 45 phút của bước 5 trước khi sửa theo rà soát; mọi điểm dưới đây chưa sửa trong phiên bản 1 (người duyệt quyết, hay sửa ở phiên bản 2):

4. **Chặn:** mục 4, `AdsControllerNativeComboTest.poolExhausted_noPool_isTrue`: phần đối chứng "pool có một quảng cáo rảnh trả `false`" không dựng được. `nativePools` là `private` (`AdsController.kt:1373`), `AdsControllerFixtures` không nhận nhà máy pool, trên JVM pool đóng vì chưa đồng ý và `_sdkReady` là `false`, nên mọi nhánh dựng được đều trả `true`; test không thể đỏ với cài đặt sai `= true`. Đề xuất: giữ câu "không có mã đơn vị thì `true`", bỏ đối chứng `false` (nhánh đó ủy quyền cho `NativeAdPool.exhausted`, đã có test ở `NativeAdPoolTest`), hay thêm một móc `@VisibleForTesting` vào `AdsController.kt` và nêu ở 3.1, 3.2.
5. **Nên sửa:** 3.2 và 3.5 thiếu `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` (`missing` sẽ khác rỗng khi thêm `POSES_IN_COLLECTION`): thêm `poses_in_collection=unit/12` vào hai chuỗi cấu hình (dữ liệu, ADR-0029), như T-079 làm với `inter_pose_detail`.
6. **Nên sửa:** mục 4 nói test của khung "thất bại vì chưa có tham số `poolExhausted` (biên dịch)", trái `docs/auto-run.md` bước 3 (khung rỗng để test biên dịch được rồi đỏ ở câu khẳng định). Với khung rỗng, 7 test xanh ngay (`grid_inCollection_everyNull_noCell`, `seventeenItems_twoCellsAt8And17`, `lazyColumn_pooledRow_poolExhausted_keepsZeroHeightRow`, `grid_fullWidthRow_poolExhausted_keepsZeroHeightRow`, `grid_inCollection_everyBackAfterNull_cellsReturn`, `noGridAds_noAdCell`, `liveGridAds_withoutController_noAdCell`): ghi khung rỗng, tách nhóm test giữ hành vi kèm phép đột biến (L-108). Thứ tự commit: dữ liệu `AdPlacementTest` và `AdPlacementAppTest` nhắc hằng enum nên phải đi cùng commit có dòng enum, trái câu về `RemoteConfigKeysTest`.
7. **Nên sửa:** phần nối ở `PosesRoute` không có test (`liveGridAds_withoutController_noAdCell` chỉ dựng `PosesScreen`); đổi tên hay mô tả test cho đúng và thêm phần nối vào "Ngoại lệ viết test trước", như plan T-079 phiên bản 2.
8. **Nên sửa:** 3.2: ảnh so sánh tên đúng là `docs/features/poses/design/review/PosesScreen_ad-collection_light_compare.png`; thêm `docs/features/poses/design/review/summary.md`; ảnh `_compare` cũ đổi byte thì khôi phục (L-110).
9. **Nên sửa:** 3.2 thiếu `docs/ops/ads-guide.md` (dòng 117 "pool cạn thì hàng cao 0", dòng 48 "chưa có vị trí của app", dòng 124, 128 chưa nói `itemsWithFixedAds`; dòng 125 về `keepSpaceWhenPoolEmpty`).
10. **Nên sửa:** số sự kiện phân tích đổi mà plan không nêu: mỗi lần ô vào lại lưới, `ReportNativeEligible(true)` ghi lại `af_native_ad_eligible` (`InListAds.kt:397`); mỗi lần ô đã thấy mà chưa có quảng cáo rời composition, `ad_opportunity` với `not_ready` (`NativePoolCell.kt:76-81`). Khi pool dao động (câu 2), hai sự kiện lặp mỗi chu kỳ. Thêm dòng ở 3.5, ý ở câu 2; giữ hành vi thì thêm test đếm `onPoolOpportunity` qua một chu kỳ.
11. **Nên sửa:** câu 1 và dòng "Màn hình thấp" dùng số của khung 411 dp. Thẻ tư thế cao theo bề ngang (`PoseCard.kt:93`): ở w360 thẻ khoảng 250 dp (ô quảng cáo cao hơn khoảng 50), ở 3 cột 673 dp thẻ khoảng 316 dp (ô thấp hơn khoảng 16). Lập luận không cần ảnh `_small` mới vẫn đứng.

Ghi chú của rà soát: kịch bản emulator không lệch chỉ số (`idp=` chỉ đếm `poses_card_*`; T-022, T-024 chạm thẻ 1, 3, trước ô đầu); hai dòng kiểm thủ công "máy thật" chấp nhận được, riêng "cuộn tới khi hết quảng cáo" dựa vào test Robolectric; ảnh `ad-collection` nên chỉ rõ 12 thẻ theo context thiết kế (thẻ 7 cặp đôi #3DDC97, thẻ 8 gia đình #5ED3F3, thẻ 9 nữ #FF5C8A, icon quảng cáo 32 dp) vì `defaultState()` chỉ có 6 thẻ; `grid_inCollection_poolHasAdsAgain_cellReturns` nên khẳng định "không phải quảng cáo của lần gọi 1 hay 2"; vòng chờ `poolWatch` chạy cả ở `LazyColumn` (vô hại); `keepSpaceWhenPoolEmpty` bị bỏ qua với ô một cột; mức Cao hợp lý.

**Rà soát độc lập trước RUN-20261031-02 (2026-10-31, agent ngữ cảnh mới, chỉ đọc, trên phiên bản 2): 1 chặn, 4 nên sửa, 6 ghi chú.** Phiên bản 3 sửa điểm chặn và 4 điểm nên sửa (mục 1). Ghi chú đã xử lý: khung chờ tĩnh nhận `cardHeight`; câu chữ 2 dp; tên test thống nhất. Ghi chú còn lại, không cần sửa:

- Kẽ hở: ô nhận `Unavailable` đúng lúc pool vừa gửi yêu cầu mới (`exhausted` là `false`) thì `poolEmpty` không bật. Ô cao 0 đứng giữa lưới tới lần đổi trạng thái sau. Cùng loại với câu 2 đã chấp nhận.
- `PoseCardTest.oneLineHeight_matchesSkeletonAndOneLineCard` đổi bề ngang và cỡ chữ bằng state (một `setContent`), tên tư thế ngắn để còn một dòng ở 156 dp, cỡ chữ 2.0.
- Tag `AD_CELL` truyền vào `NativeAdCard` thắng tag `native_ad_card` bên trong, nên `onNodeWithTag(AD_CELL)` đo được thẻ.
- Số dòng của `ads-guide.md` ở mục 3.1 đã lệch vài dòng; nội dung cần sửa vẫn còn.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Tuấn, **duyệt chéo:** Nhật
- **Ghi chú:**
  - Nhật chọn "Tuấn và Nhật đều duyệt" khi AI hỏi, sau buổi duyệt cùng AI ngày 2026-10-31.
  - Trả lời câu hỏi ở mục 8:
    - câu 1: ô bằng thẻ tên một dòng;
    - câu 2: chấp nhận, ghi rõ sự kiện lặp;
    - câu 3: ghi đề xuất trong mô tả PR;
    - điểm chặn 4: bỏ đối chứng `false`.

Phiên bản 3:

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
