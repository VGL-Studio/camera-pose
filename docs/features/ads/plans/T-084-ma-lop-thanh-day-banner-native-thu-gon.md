---
ticket: T-084
title: Mã quảng cáo theo lớp cho thanh đáy (banner, native thu gọn), chọn lớp bằng Remote Config
status: stopped              # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 9.0M         # cỡ M theo scripts/token-usage.py estimate (2026-10-31), không nâng tay
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Tuấn               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-10-31
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-084: MÃ QUẢNG CÁO THEO LỚP CHO THANH ĐÁY, CHỌN LỚP BẰNG REMOTE CONFIG

## 1. Tóm tắt

`ConfiguredAdUnitIds` (bản production) đọc thêm mục `<khóa vị trí>/<định dạng lớp>=<mã>` cho hai lớp của thanh đáy: `banner` và `native_collap`. `of(placement, format)` trả mã của lớp khi có, và một danh sách mới `missingLayers` liệt kê lớp của thanh đáy còn thiếu mã. Bản staging giữ mã thử theo định dạng như hiện nay.

Kiểm chứng bằng unit test của `AdUnitIdsTest` và một test Robolectric của `BottomAdBarTest` đọc mã lớp từ chuỗi cấu hình. Bốn tổ hợp khóa của câu 13 thử tay trên Firebase staging (`docs/ops/manual-test.md`).

## 2. Hiểu yêu cầu

Ticket xử lý câu 13 tới 15 của `features/ads/spec.md` (confirmed 2026-10-31). Spec ads không có kịch bản riêng; thanh đáy theo KB-6 của `features/poses/spec.md`.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Câu 14 | Cấu hình phát hành nhận thêm mục `<khóa vị trí>/<định dạng lớp>=<mã>`, ví dụ `poses_bottom/banner`, `poses_bottom/native_collap` | Khóa giống hệt khóa log mà khung đã dùng (`nativeSlotKey(placement, format)` ở `NativeAdSources.kt`): định dạng chính thì là khóa vị trí, lớp khác thì là `<khóa vị trí>/<mã định dạng>`. Mã định dạng là `AdFormat.code`: `banner`, `native_collap` |
| Câu 14 | Khi có mã lớp thì lớp gửi yêu cầu bằng mã đó | `ConfiguredAdUnitIds.of(placement, AdFormat.BANNER)` và `of(placement, AdFormat.NATIVE_COLLAPSIBLE)` trả mã của mục lớp. Khung đã gọi `unitIds.of(placement, format)` cho hai lớp (`AdSlot.kt:91`, `CollapsibleNativeAd.kt:336`, `AdsController.kt:1465`, `1512`), nên không sửa chỗ gọi |
| Câu 14, B-04 | Lớp không có mã thì không gửi, không chiếm chỗ | Giữ như hôm nay: `of` trả `null`. `BottomAdBarTest.layerUnitIdNull_noLoad_slotStays`, `bannerUnitIdNull_noLoad_noReservation` đã kiểm phần vẽ |
| Câu 14 | Hai thanh đáy dùng chung một mã banner và một mã native thu gọn | Không cần code: cấu hình phát hành ghi cùng một mã cho `poses_bottom/banner` và `pose_detail_bottom/banner` (và tương tự cho `native_collap`). Khóa vẫn theo vị trí, để tên khóa trùng khóa log và bộ đếm |
| Câu 14 | Bản phát hành báo lỗi khi thanh đáy thiếu mã lớp | Phần chặn nằm ở `scripts/release.sh` của dự án mẫu: tiêu chí này hoàn thành khi dự án mẫu nhận đề xuất và T-075 lấy bản đó (người làm). Ở code: `ConfiguredAdUnitIds.missingLayers` liệt kê lớp thiếu, cùng khuôn chuỗi, để T-075 và test đối chiếu (mục 3.1) |
| Câu 13 | Bốn tổ hợp `is_show_native_bottom`, `is_show_native_collap` | Không sửa cách chọn lớp. Khung hôm nay (luật D-01 của `docs/ops/ads-business-rules.md`, `bottomAdBarLayers`) chỉ vẽ lớp thu gọn khi khe là native: tổ hợp 4 "native thu gọn trên banner" ra banner, không có lớp. Mâu thuẫn này ở câu 1 mục 8 |
| Câu 15 | Lúc phát hành: native nhỏ, lớp thu gọn tắt | Không đổi giá trị mặc định nào. Bản staging giữ như câu 11 |

Thiết kế tham chiếu: Không có thiết kế riêng (`docs/ops/design-workflow.md`); ticket không đổi giao diện.

ADR liên quan: ADR-0009 (không đọc cấu hình production), ADR-0013 (mã production đưa vào lúc build, `ConfiguredAdUnitIds.missing`), ADR-0007 (biến thể staging, production), ADR-0026 (khung AdsKit), ADR-0029 (phần ngoài mục 3.2), ADR-0031 (câu khẳng định cũ).

## 3. Giải pháp

### 3.1. Cách làm

Chỉ sửa `AdUnitIds.kt` trong code chính. `ProductionAdsModule` vẫn dựng `ConfiguredAdUnitIds(BuildConfig.AD_UNITS)`; tham số `-PadUnits` đi nguyên vào `BuildConfig` (`app/build.gradle.kts:61`), dấu `/` không cần thoát trong chuỗi Java. `GoogleTestAdUnitIds` không đổi.

**Quyết định 1: vị trí nào có lớp.** Vị trí có lớp là vị trí mà `BottomAdBarScaffold` nhận: định dạng `NATIVE_SMALL` (`BottomAdBar.kt:167`). Plan loại thêm vị trí native nhỏ trong danh sách (`nativeEvery != null`), vì đó không phải thanh đáy. Khung không có thuộc tính "là thanh đáy" riêng; plan suy từ khai báo, không thêm thuộc tính vào `AdPlacement`, vì:

- thêm thuộc tính là sửa dòng khai báo mà `scripts/release.sh` đọc (`docs/ops/ads-guide.md`, mục 5, bước 2);
- `AdPlacement.kt` cũng do T-080 sửa.

Hôm nay (trước T-081) các vị trí có lớp ở production là `sample_native_small`, `poses_bottom`, `pose_detail_bottom`; T-081 bỏ `sample_native_small`. Giới hạn: một vị trí native nhỏ vẽ bằng `NativeAdSlot` trong màn (không phải thanh đáy) cũng bị coi là có lớp. App chưa có vị trí như vậy. Câu 2 mục 8 hỏi có nên khai báo rõ.

**Quyết định 2: báo thiếu mã lớp.** Dùng danh sách riêng `missingLayers: List<String>`, không thêm vào `missing`:

- `missing` là `List<AdPlacement>`, không ghi được lớp;
- ba test có sẵn so `missing` bằng đúng danh sách vị trí; ADR-0013 mô tả nó là "vị trí thiếu mã";
- mỗi phần tử của `missingLayers` là đúng khóa phải thêm vào cấu hình (`poses_bottom/banner`), người đọc chép thẳng vào `release.properties`; T-075 dùng cùng khuôn.

Thứ tự: theo thứ tự `AdPlacement.entries`, rồi `banner` trước `native_collap`. Vị trí `gallery_*` không bao giờ thiếu.

**Quyết định 3: mục lạ.** Như hôm nay, mục có khóa lạ được bỏ qua, không ném lỗi (test có sẵn dùng `other_key=unit/2`). Gõ sai `poses_bottom/baner` thì lớp `poses_bottom/banner` vẫn nằm trong `missingLayers`. Mục lớp của vị trí không phải thanh đáy (ví dụ `review_interstitial/banner`) cũng bị bỏ qua: `of` chỉ trả mã lớp cho vị trí có lớp và hai định dạng lớp. Mục thiếu `=` hay thiếu mã vẫn ném `IllegalArgumentException` như cũ (`require` có sẵn).

Code (chưa chạy thử: E dưới 2, mục 4.1). Phần còn lại của `AdUnitIds.kt` giữ nguyên:

```kotlin
fun interface AdUnitIds {
    fun of(placement: AdPlacement): String?

    /**
     * The unit of [placement] drawn as [format]: a layer of a bottom bar (banner, collapsible native) is another
     * format than the bar's own (plan T-030, question 2). By default only the placement's own format has a unit, so a
     * layer of another format sends nothing (B-04); [ConfiguredAdUnitIds] also reads a unit per layer (spec ads,
     * question 14).
     */
    fun of(placement: AdPlacement, format: AdFormat): String? = if (format == placement.format) of(placement) else null
}

/** The layers of a bottom bar loaded with another format than the bar's own (D-01): banner slot, collapsible layer. */
internal val BOTTOM_BAR_LAYER_FORMATS: List<AdFormat> = listOf(AdFormat.BANNER, AdFormat.NATIVE_COLLAPSIBLE)

/**
 * Whether [this] is a bottom bar placement, with a unit per layer on production: native small, the format that
 * [BottomAdBarScaffold] takes, and not an in-list placement ([AdPlacement.nativeEvery]).
 */
internal val AdPlacement.hasBottomBarLayers: Boolean
    get() = format == AdFormat.NATIVE_SMALL && nativeEvery == null

/**
 * Real ad units, given when the release is built and never stored in the repo (ADR-0009).
 * [config] has the form `<key>=<ad unit>;<key>=<ad unit>`. The key is a placement key for the placement's own
 * format, or `<placement key>/<format code>` for a layer of a bottom bar (`poses_bottom/banner`,
 * `poses_bottom/native_collap`; spec ads, question 14), the same key as the logs ([nativeSlotKey]).
 */
class ConfiguredAdUnitIds(config: String) : AdUnitIds {
    private val units: Map<String, String> = config.split(';')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .associate { entry ->
            val key = entry.substringBefore('=').trim()
            val unit = entry.substringAfter('=', missingDelimiterValue = "").trim()
            require(key.isNotEmpty() && unit.isNotEmpty()) { "Ad unit entry must be <key>=<ad unit>" }
            key to unit
        }

    /**
     * Placements that have no ad unit in this build. A release must have none. Staging-only placements
     * ([AdPlacement.stagingOnly]) never ship, so they are never missing.
     */
    val missing: List<AdPlacement> = AdPlacement.entries.filter { !it.stagingOnly && it.key !in units }

    /**
     * Layers of bottom bars that have no ad unit in this build, as the key to add (`poses_bottom/banner`). Such a layer
     * sends nothing (B-04); a release must have none (spec ads, question 14). Staging-only placements are never
     * missing.
     */
    val missingLayers: List<String> = AdPlacement.entries
        .filter { !it.stagingOnly && it.hasBottomBarLayers }
        .flatMap { placement -> BOTTOM_BAR_LAYER_FORMATS.map { nativeSlotKey(placement, it) } }
        .filter { it !in units }

    /** Staging-only placements get no unit even when the configuration names them: production never shows them. */
    override fun of(placement: AdPlacement): String? = of(placement, placement.format)

    /** The placement's own format, or a layer of a bottom bar placement; any other format has no unit. */
    override fun of(placement: AdPlacement, format: AdFormat): String? = when {
        placement.stagingOnly -> null
        format == placement.format -> units[placement.key]
        placement.hasBottomBarLayers && format in BOTTOM_BAR_LAYER_FORMATS -> units[nativeSlotKey(placement, format)]
        else -> null
    }
}
```

Tài liệu:

- `docs/ops/ads-guide.md`, mục 5:
  - Bước 2: bỏ câu "Mã đơn vị theo vị trí hay theo khe như AdsKit là câu hỏi 14 của spec ads; đổi cách này cần sửa `release.sh` (việc của người)". Thay bằng: mã theo vị trí; thanh đáy có thêm mã theo lớp (bước 6).
  - Bước 6: thay đoạn "Thanh đáy: production hôm nay chỉ có mã..." bằng cách khai báo mã lớp: mỗi thanh đáy cần ba mục (`<vị trí>`, `<vị trí>/banner`, `<vị trí>/native_collap`). Hai thanh đáy dùng chung một đơn vị banner và một đơn vị native thu gọn (câu 14), nên cùng một mã ghi hai lần. Đơn vị banner của lớp bật tự làm mới trên AdMob như vị trí `banner`. `ConfiguredAdUnitIds.missingLayers` liệt kê lớp còn thiếu. Thiếu mã lớp thì lớp không gửi, không chiếm chỗ; tắt `is_show_native_bottom` khi đó thì đáy trống. Chỉ thêm mục lớp vào `release.properties` khi `release.sh` đã nhận khuôn `<vị trí>/<định dạng>`: bản `release.sh` app đang giữ kiểm từng mục theo khuôn `<khóa vị trí>=ca-app-pub-...` và có thể dừng ở mục có `/`. Trước đó bản phát hành giữ khe native nhỏ như câu 15. Phần chặn khi thiếu mã lớp chỉ có khi dự án mẫu nhận đề xuất (T-075 lấy nguyên `release.sh` của mẫu, không sửa ở app). Staging dùng mã thử theo định dạng của lớp.
- `docs/ops/manual-test.md`: mục mới 11.5 "Bốn tổ hợp của thanh đáy trên Firebase staging (T-084)". Bốn bước, mỗi bước đổi hai khóa trên Remote Config của project staging, đóng hẳn app rồi mở lại, mở thư viện và chi tiết. Kết quả mong đợi theo khung hôm nay:
  - bật, tắt: khe native 80 dp;
  - tắt, tắt: banner neo đáy, không lề;
  - bật, bật: lớp 250 dp nổi trên khe native, thu gọn thì lộ khe, mở lại sau `time_expand_collap_ad` giây;
  - tắt, bật: banner, không có lớp (luật D-01; câu 1 mục 8).

  Cuối mục: đặt lại `is_show_native_bottom = true`, `is_show_native_collap = false` như câu 11.
- `docs/ops/release.md` **không sửa ở ticket này.** App giữ file ở commit cũ của dự án mẫu (`hold=docs/ops/release.md` trong `docs/template-sync.txt`), và T-075 lấy nguyên bản mới của dự án mẫu (`--take`). Sửa ở đây thì T-075 ghi đè. Cách khai báo mã lớp nằm ở `ads-guide.md`; T-075 có ghi chú khuôn (dưới).
- `docs/features/ads/tickets/T-075-phat-hanh-voi-quang-cao-moi.md`: thêm một ý ở mục Phạm vi:
  - danh sách mã cần chuẩn bị có thêm `<vị trí>/banner`, `<vị trí>/native_collap` cho mỗi thanh đáy (khuôn câu 14, T-084);
  - khi lấy `release.sh` của dự án mẫu, kiểm nó có nhận mục có `/` và có chặn khi thiếu mã lớp (như `ConfiguredAdUnitIds.missingLayers`) không;
  - chưa có thì đó là đề xuất về dự án mẫu, không phải việc sửa trong T-075 (T-075 không sửa `release.sh` ở app).

  Ticket đang `backlog`, chưa có plan, nên sửa được. Không sửa gì khác của T-075.

**Đề xuất về dự án mẫu** (`docs/ops/from-app.md`, mục 1 và 2): `core/ads` không bị giữ (`docs/template-sync.txt` không có `hold=core/ads`), nên đợt đồng bộ sau sẽ đụng `AdUnitIds.kt`. Phiên không mở issue ở repo khác. Mô tả PR và báo cáo phiên ghi một dòng "Đề xuất về dự án mẫu: mã theo lớp của thanh đáy, `ConfiguredAdUnitIds` đọc `<vị trí>/<định dạng lớp>`, `missingLayers`; cùng ý câu 14 của spec AdsKit của mẫu (mã theo khe)". Người mở issue trong một tuần. Đây là thay đổi có ích chung cho mọi app ở module core; ghi vào mô tả PR theo `CLAUDE.md`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIds.kt` | `BOTTOM_BAR_LAYER_FORMATS`, `AdPlacement.hasBottomBarLayers`, `ConfiguredAdUnitIds.of(placement, format)`, `missingLayers`, KDoc của `AdUnitIds.of` và `ConfiguredAdUnitIds` (mục 3.1) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | Bảy test mới của mục 4; chú thích của `production_otherFormat_null_ownFormat_configured` bỏ "until units per slot exist" (câu khẳng định giữ, mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/BottomAdBarTest.kt` | Test `configuredLayerUnits_buildBannerAndLayer` (mục 4) |
| Sửa | `docs/ops/ads-guide.md` | Mục 5, bước 2 và bước 6 (mục 3.1) |
| Sửa | `docs/ops/manual-test.md` | Mục 11.5 (mục 3.1) |
| Sửa | `docs/features/ads/tickets/T-075-phat-hanh-voi-quang-cao-moi.md` | Một ý ở mục Phạm vi: khuôn mã lớp (mục 3.1) |
| Sửa | `docs/features/ads/tickets/T-084-ma-lop-thanh-day-banner-native-thu-gon.md` | Trạng thái theo phiên |

Nơi khác đọc các file bị đổi (L-020 của mẫu):

- `AdUnitIds.kt`:
  - `scripts/tests/run.sh` (khối `template-sync.sh`) chỉ chép file vào repo tạm, và chỉ chạy ở repo dự án mẫu (`skip` ở app): không ảnh hưởng.
  - `ProductionAdsModule`, `StagingAdsModule` dùng hai lớp, không đổi chữ ký: không sửa.
  - Mọi chỗ gọi `of(placement, format)` qua `AdsController.unitId` (`AdSlot.kt`, `CollapsibleNativeAd.kt`, `NativeAdSlot.kt`, `AdsController.kt`) không đổi.
  - Test dùng `AdUnitIds { null }` hay `object : AdUnitIds` (`AdsControllerFunnelTest`, `AdsControllerNativeComboTest`, `AdsControllerResumeTest`, `RewardedActionTest`, `BottomAdBarTest`) không đổi vì giao diện `fun interface` giữ nguyên.
- `scripts/release.sh`:
  - đọc `AdPlacement.kt`, không đọc `AdUnitIds.kt`; chú thích dòng 145 nhắc `ConfiguredAdUnitIds.missing`, vẫn đúng;
  - không sửa (T-075).
- `scripts/status.py` (mục 6 "Sẵn sàng phát hành") đọc `AdPlacement.kt`, không đổi. Trang chưa liệt kê mã lớp; ghi ở mục 6 của plan.
- `docs/ops/ads-guide.md`, `docs/ops/manual-test.md`: không script nào đọc nội dung hai file này. `grep -rln "ads-guide\|manual-test" scripts/ .github/` chỉ thấy `scripts/ads-plan.py`, nơi tên file nằm trong chú thích và chữ in ra (mục 5 vẫn là mục 5).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí; không thêm khóa. Bản production gửi được yêu cầu cho lớp banner và lớp native thu gọn của `poses_bottom`, `pose_detail_bottom` khi có mã lớp và hai khóa `is_show_native_bottom`, `is_show_native_collap` chọn lớp đó (spec ads câu 13 tới 15). Giá trị lúc phát hành không đổi (câu 15).
- Sự kiện phân tích: Không thêm. Lớp banner và lớp thu gọn đã ghi số liệu với mã vị trí của thanh và định dạng của lớp (T-030).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Code chỉ dùng thư viện chuẩn Kotlin và lớp có sẵn của `:core:ads` (`nativeSlotKey`, `AdPlacement`, `AdFormat`).

### 3.4. Phương án dự phòng

Không có. Thay đổi là phân tích chuỗi và tra bảng trong một lớp thuần Kotlin, không có điểm kỹ thuật chưa chắc.

### 3.5. Hành vi đã có bị đổi

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `ConfiguredAdUnitIds.of(placement, format)` trả `null` với mọi định dạng khác định dạng chính (mặc định của giao diện) | Trả mã của mục `<khóa vị trí>/<mã định dạng>` cho hai định dạng lớp của vị trí có lớp; không có mục thì vẫn `null` | `AdUnitIdsTest.production_otherFormat_null_ownFormat_configured` (cấu hình chỉ có `sample_native_small=unit/1`) | Giữ: cấu hình của test không có mục lớp nên hai câu `assertNull` vẫn đúng. Chỉ sửa chú thích "until units per slot exist (spec, Q14)" thành "no layer entry in the configuration (B-04)". T-081 đổi vị trí của test sang `POSES_BOTTOM` (mục 6) |
| `of(placement)` đọc `units[placement.key]` | `of(placement)` gọi `of(placement, placement.format)`, cùng giá trị | `configuredUnits_areParsedPerPlacementKey`, `stagingOnlyPlacement_hasNoUnit_evenWhenConfigured`, `placementWithoutUnit_isReportedMissingAndShowsNoAd` | Giữ nguyên |
| `missing` | Không đổi; lớp thiếu nằm ở `missingLayers` | `configuredUnits_areParsedPerPlacementKey`, `placementWithoutUnit_isReportedMissingAndShowsNoAd` | Giữ nguyên. Chuỗi cấu hình không có mục lớp nên `missing` vẫn rỗng hay đúng danh sách cũ |
| Thông báo của `require` "Ad unit entry must be <placement key>=<ad unit>" | "Ad unit entry must be <key>=<ad unit>" | `malformedEntry_isRejected` chỉ kiểm loại lỗi | Giữ nguyên |
| Lớp của thanh đáy không có mã thì không gửi (B-04) | Không đổi | `BottomAdBarTest.layerUnitIdNull_noLoad_slotStays`, `bannerUnitIdNull_noLoad_noReservation` (bản giả `NoLayerUnits`) | Giữ nguyên |
| Staging: mã thử theo định dạng của lớp | Không đổi | `AdUnitIdsTest.staging_unitOfEachLayerFormat` | Giữ nguyên |

`scripts/existing-tests.py` chạy ngày 2026-10-31 (bỏ các dòng từ `.claude/worktrees/`): 11 test dùng tên của `AdUnitIds.kt`, 0 "Cần xem trước", 4 "Nên xem", plan đã nhắc 7. Đã đọc mọi test của `AdUnitIdsTest` và các test về mã lớp của `BottomAdBarTest`. Bốn test "Nên xem" chỉ kiểm `GoogleTestAdUnitIds` (staging), plan không đổi: giữ.

- `googleTestIds_coverGalleryPlacements`;
- `googleTestUnit_existsForEveryFormat`;
- `googleTestUnits_nativeFormats`;
- `googleTestUnits_newFormats`.

## 4. Kiểm thử

Test của khung dùng vị trí của app `POSES_BOTTOM`, `POSE_DETAIL_BOTTOM` (vị trí có lớp ở production sau T-081), `REVIEW_INTERSTITIAL` (vị trí không có lớp) và `GALLERY_NATIVE_SMALL` (chỉ staging). Mã là chuỗi giả `unit/N`, không có mã thật (`scripts/secret-scan.py`). `BottomAdBarTest` dựng chuỗi theo `placement.key` để test giữ được khi T-081 đổi vị trí của lớp test.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Câu 14: đọc mục lớp | Unit test | `AdUnitIdsTest.layerEntries_giveTheUnitOfEachLayer`: `ConfiguredAdUnitIds("poses_bottom=unit/1;poses_bottom/banner=unit/2;poses_bottom/native_collap=unit/3")`. `of(POSES_BOTTOM, BANNER) == "unit/2"`, `of(POSES_BOTTOM, NATIVE_COLLAPSIBLE) == "unit/3"`, `of(POSES_BOTTOM, NATIVE_SMALL) == "unit/1"`, `of(POSES_BOTTOM) == "unit/1"`. Đối chứng cùng test: `of(POSE_DETAIL_BOTTOM, BANNER) == null` (mục lớp của vị trí khác không lan). Đỏ trên code cũ ở câu `"unit/2"` (trả `null`) |
| Câu 14: hai thanh dùng chung một mã | Unit test | `AdUnitIdsTest.twoBars_mayShareOneLayerUnit`: `poses_bottom/banner=unit/2;pose_detail_bottom/banner=unit/2`; cả hai `of(..., BANNER) == "unit/2"`. Đỏ trên code cũ |
| B-04: lớp không có mã thì không có mã | Unit test | `AdUnitIdsTest.layerWithoutEntry_hasNoUnit`: `poses_bottom=unit/1;poses_bottom/banner=unit/2`: `of(POSES_BOTTOM, NATIVE_COLLAPSIBLE) == null`. Đối chứng cùng test (để test đỏ trên code cũ, L-035): `of(POSES_BOTTOM, BANNER) == "unit/2"`. Thêm: chỉ có `poses_bottom/banner=unit/2` thì `of(POSES_BOTTOM) == null` và `POSES_BOTTOM in missing` (mục lớp không thay mã chính) |
| B-04 trên thanh đáy, qua chuỗi cấu hình | Compose UI test (Robolectric) | `BottomAdBarTest.configuredLayerUnits_buildBannerAndLayer`: `layerUnitIds = ConfiguredAdUnitIds("${placement.key}=unit/1;${placement.key}/banner=unit/2")`, `flags = barFlags(nativeBottom = false, collap = false)`: `banners.createdCount == 1`. Rồi `flags = barFlags(nativeBottom = true, collap = true)`, slot có quảng cáo: không có `CollapsibleNativeAdTestTags.AD`, `layerFeed.collections == 0` (không có mục `native_collap`). Đối chứng: `layerUnitIds` thêm `;${placement.key}/native_collap=unit/3` thì `layerFeed.collections == 1`. Đỏ trên code cũ ở `createdCount == 1` |
| Câu 14: báo thiếu mã lớp | Unit test | `AdUnitIdsTest.missingLayers_listEveryBottomBarLayerWithoutUnit`: `ConfiguredAdUnitIds("").missingLayers` bằng đúng `["sample_native_small/banner", "sample_native_small/native_collap", "poses_bottom/banner", "poses_bottom/native_collap", "pose_detail_bottom/banner", "pose_detail_bottom/native_collap"]` (T-081 bỏ hai mục đầu, mục 6). Cấu hình có đủ sáu mục: rỗng. Đối chứng: đủ trừ `pose_detail_bottom/native_collap` thì đúng `["pose_detail_bottom/native_collap"]`. Đỏ trên code cũ vì `missingLayers` chưa có (biên dịch) |
| Mục gõ sai không được tính | Unit test | `AdUnitIdsTest.misspelledLayerEntry_isIgnoredAndStillMissing`: mọi mục chính của vị trí không phải staging, `pose_detail_bottom/banner`, `pose_detail_bottom/native_collap`, `poses_bottom/native_collap`, nhưng `poses_bottom/baner=unit/2` thay `poses_bottom/banner`: không ném lỗi, `of(POSES_BOTTOM, BANNER) == null`, `"poses_bottom/banner" in missingLayers`; đối chứng trong cùng cấu hình: `of(POSE_DETAIL_BOTTOM, BANNER) != null` |
| Chỉ native nhỏ không có `nativeEvery` là thanh đáy | Unit test | `AdUnitIdsTest.bottomBarLayers_onlyNativeSmallWithoutEvery`: `assertFalse(AdPlacement.GALLERY_INLIST_SMALL.hasBottomBarLayers)` (native nhỏ có `nativeEvery`), đối chứng `assertTrue(AdPlacement.POSES_BOTTOM.hasBottomBarLayers)`. Bỏ điều kiện `nativeEvery == null` thì đỏ (rà soát độc lập) |
| `gallery_*` không bao giờ thiếu, không có mã lớp | Unit test | `AdUnitIdsTest.galleryLayers_neverMissing_andGetNoUnit`: `ConfiguredAdUnitIds("gallery_native_small/banner=x;poses_bottom/banner=y")`: `of(GALLERY_NATIVE_SMALL, BANNER) == null`; không phần tử nào của `missingLayers` bắt đầu bằng `gallery_`. Đối chứng cùng cấu hình: `of(POSES_BOTTOM, BANNER) == "y"`, để test đỏ trên code cũ |
| Vị trí không phải thanh đáy không có lớp | Unit test | `AdUnitIdsTest.nonBarPlacement_layerEntryIgnored`: `review_interstitial=unit/1;review_interstitial/banner=unit/2`: `of(REVIEW_INTERSTITIAL, BANNER) == null`, `of(REVIEW_INTERSTITIAL) == "unit/1"`; `missingLayers` của cấu hình rỗng không có khóa nào bắt đầu bằng `review_interstitial`. Qua trên code cũ (hành vi giữ nguyên), là test chặn hồi quy của nhánh `else` |
| Mục lớp thiếu mã bị từ chối | Unit test | `AdUnitIdsTest.layerEntryWithoutUnit_isRejected`: `ConfiguredAdUnitIds("poses_bottom/banner=")` ném `IllegalArgumentException` (`@Test(expected = ...)`). Qua trên code cũ (giữ `require`) |
| Bản staging giữ mã thử theo định dạng | Unit test có sẵn | `AdUnitIdsTest.staging_unitOfEachLayerFormat`, `googleTestUnits_existForEveryPlacement`: giữ nguyên |
| Câu 13: bốn tổ hợp khóa trên bản staging | Kiểm tra thủ công (máy thật: Remote Config) | `docs/ops/manual-test.md` mục 11.5: người đổi hai khóa trên Firebase staging, xem thư viện và chi tiết ở bốn tổ hợp. Emulator của phiên không đổi được Remote Config trên Firebase |
| Câu 14: bản production có mã lớp | Kiểm tra thủ công (người: cấu hình production) | Người phát hành thêm mục lớp vào `release.properties` theo `ads-guide.md` mục 5 bước 6 (T-075, câu 15). AI không đọc hay tạo cấu hình production (ADR-0009) |
| Tài liệu khai báo mã lớp, ghi chú cho T-075 | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc `ads-guide.md` mục 5 bước 2 và 6, ý mới của ticket T-075 |

Commit test (bước 3) có khung rỗng để biên dịch được: `missingLayers` trả `emptyList()`, `BOTTOM_BAR_LAYER_FORMATS` và `hasBottomBarLayers` có mặt, `of(placement, format)` chưa đổi. Test mới đỏ ở câu khẳng định, trừ hai test chặn hồi quy đã ghi.

**Ngoại lệ viết test trước:** sửa tài liệu (`ads-guide.md`, `manual-test.md`, ticket T-075) không có test; người duyệt đọc. Mục 11.5 của `manual-test.md` là kịch bản cho người, không phải tệp `deviceScenarios`, vì cần đổi Remote Config trên Firebase.

### 4.1. Chạy thử

Không cần (E dưới 2). Thay đổi là biến thể nhỏ của cách đọc chuỗi có sẵn trong cùng lớp; khóa lớp dùng lại `nativeSlotKey` có sẵn.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**

- 1, quảng cáo: bản production có thêm định dạng gửi yêu cầu cho thanh đáy (banner, native thu gọn) khi khóa chọn.
- 7, module core dùng chung `:core:ads`.

Rủi ro Cao, cần duyệt chéo (Tuấn và Nhật). Nhật là người phụ trách sản phẩm, cần biết trước khi duyệt (`risk-scoring.md`, mục 3).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một file code chính trong một module core, có thêm giao diện công khai (`missingLayers`) và đổi hành vi của `of(placement, format)`; thêm tài liệu |
| B. Độ rõ của yêu cầu | 2 | Câu 13, 14 rõ về khuôn. Còn câu hỏi mở: tổ hợp 4 của câu 13 trái luật D-01 của khung (câu 1 mục 8), cách nhận ra thanh đáy (câu 2) |
| C. Khả năng kiểm chứng tự động | 1 | Phân tích chuỗi và tra bảng có unit test đủ trường hợp; bốn tổ hợp trên Firebase staging và cấu hình production kiểm tay |
| D. Ảnh hưởng chéo | 2 | `AdUnitIdsTest.kt`, `docs/ops/ads-guide.md`, `docs/ops/manual-test.md` cũng do T-080 sửa; `AdUnitIdsTest.kt`, `BottomAdBarTest.kt` do T-081 sửa; T-075 dùng khuôn mã lớp |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của cách đọc chuỗi có sẵn |
| **Tổng** | 8 | **Mức: Cao (chạm điểm chặn)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `ConfiguredAdUnitIds` của `:core:ads`, chỉ bản production dùng.
  - `core/ads` không bị giữ khi đồng bộ (`docs/template-sync.txt`): đợt đồng bộ sau có thể xung đột ở `AdUnitIds.kt` tới khi dự án mẫu nhận đề xuất (mục 3.1).
- Task khác có thể xung đột:
  - **T-080** (plan approved, chạy ở RUN-20261031-02 trước ticket này). File trùng:
    - `core/ads/src/test/.../AdUnitIdsTest.kt`: T-080 thêm `poses_in_collection=unit/12` vào hai chuỗi của `configuredUnits_areParsedPerPlacementKey`; T-084 thêm test mới, không sửa test đó;
    - `docs/ops/ads-guide.md`: T-080 sửa phần vị trí của app; T-084 sửa bước 2 và 6;
    - `docs/ops/manual-test.md`: T-080 thêm bước ô trong lưới ở mục 11; T-084 thêm mục 11.5.

    `POSES_IN_COLLECTION` là `NATIVE_IN_COLLECTION`, không có lớp, nên không đổi danh sách `missingLayers` mong đợi.
  - **T-081** (chưa có plan): bỏ `SAMPLE_NATIVE_SMALL`, chuyển test của `core/ads` sang `POSES_BOTTOM`. File trùng: `AdUnitIdsTest.kt`, `BottomAdBarTest.kt`. Sau T-081:
    - danh sách mong đợi của `missingLayers_listEveryBottomBarLayerWithoutUnit` bỏ hai mục `sample_native_small/...` (dữ liệu, ADR-0029);
    - `production_otherFormat_null_ownFormat_configured`, `staging_unitOfEachLayerFormat` đổi sang `POSES_BOTTOM` như các test khác.

    Test mới của T-084 không dùng `SAMPLE_NATIVE_SMALL` và `BottomAdBarTest` dựng chuỗi theo `placement.key`. Plan T-081 nên ghi hai điều này ở mục 3.5 của nó.
  - **T-075** (người làm, `auto_run: false`):
    - dùng khuôn `<vị trí>/<định dạng lớp>` và danh sách lớp thiếu để chặn ở `release.sh`;
    - lấy nguyên `release.md` của dự án mẫu, nên T-084 không sửa file đó.

    T-084 ghi ghi chú vào ticket T-075. Bản production chỉ có mã lớp khi T-075 xong và người thêm mục vào `release.properties`.
  - `scripts/status.py` (mục 6 "Sẵn sàng phát hành") chưa liệt kê mã lớp còn thiếu; không trong phạm vi. Nếu nhóm muốn thì đề xuất ticket sau (cùng câu 3 mục 8).
- Đề xuất thứ tự chạy: sau T-080 (cùng file, T-080 đã xếp trước). Nếu T-081 có plan cùng danh sách thì T-084 chạy trước T-081, để T-081 đổi dữ liệu của test mới theo ADR-0029.

## 7. Điều kiện dừng riêng

- Cần đổi kiểu hay nội dung của `ConfiguredAdUnitIds.missing` (danh sách vị trí) để test qua: dừng, vì ADR-0013 và `release.sh` dựa vào nghĩa hiện tại.
- Cần đổi `GoogleTestAdUnitIds` hay cách chọn lớp (`bottomAdBarLayers`, `AdsGlobalFlags.showNativeCollap`) để đạt tiêu chí: dừng, vì ticket không sửa cách chọn lớp (câu 13).
- Cần sửa `docs/ops/release.md` hay `scripts/release.sh`: dừng (T-075).

## 8. Câu hỏi cho người duyệt

1. **Tổ hợp 4 của câu 13 không có trong khung.** Câu 13 ghi bốn cách hiện, trong đó "native thu gọn trên banner" (`is_show_native_bottom` tắt, `is_show_native_collap` bật). Khung hôm nay theo luật D-01 (`docs/ops/ads-business-rules.md`, `bottomAdBarLayers` ở `BottomAdBar.kt:42-46`, `AdsFlags.kt:105`): lớp thu gọn chỉ hiện khi khe là native. Tổ hợp 4 ra banner, không có lớp. Ticket không sửa cách chọn lớp, nên plan ghi kết quả thật vào `manual-test.md`.
   - Đề xuất của AI: nhận ba cách hiện thật cho ticket này. Dòng "bốn tổ hợp" của định nghĩa hoàn thành hiểu là thử đủ bốn tổ hợp khóa, tổ hợp 4 ra banner.
   - Nếu nhóm cần lớp thu gọn trên banner thì mở ticket riêng: đổi luật D-01 của khung, đề xuất về dự án mẫu. Khi đó spec ads cần ghi lại câu 13 (chuyển `changed`).
2. **Khung nhận ra thanh đáy thế nào.** Plan suy từ khai báo: định dạng `NATIVE_SMALL` và không có `nativeEvery`.
   - Phương án khác: thêm thuộc tính `bottomBar = true` vào `AdPlacement`. Cách này phải sửa dòng khai báo mà `release.sh` đọc, và `AdPlacement.kt` (T-080 cũng sửa).
   - Đề xuất của AI: suy từ khai báo. Khi app có một native nhỏ trong màn (không phải thanh đáy) thì chuyển sang thuộc tính.
3. **Báo thiếu mã lớp ở đâu.** Plan dùng danh sách riêng `missingLayers` (khóa dạng `poses_bottom/banner`), không gộp vào `missing`.
   - Bản phát hành chỉ báo lỗi khi T-075 thêm kiểm vào `release.sh`. Code của app không chặn build hay báo lúc chạy, ngoài dòng log lỗi có sẵn ("No ad unit for placement poses_bottom/banner") khi lớp được chọn mà thiếu mã.
   - Đề xuất của AI: giữ như vậy; không thêm test hay bước build đọc cấu hình production (ADR-0009).
4. **`docs/ops/release.md` để T-075.** Ticket ghi "`release.md` thuộc T-075 nếu T-075 thay cả file". T-075 lấy nguyên file của dự án mẫu, nên plan không sửa `release.md`. Cách khai báo nằm ở `ads-guide.md`, cộng ghi chú vào ticket T-075.
   - Đề xuất của AI: đồng ý. Sau T-075, nếu `release.md` của mẫu chưa có mã lớp thì đưa vào đề xuất về dự án mẫu cùng `AdUnitIds.kt`.
5. **Ghi chú khi soạn plan.** Lúc tìm chỗ nhắc `ConfiguredAdUnitIds.missing`, AI đã đọc `scripts/release.sh` dòng 130 tới 175 (phần đối chiếu vị trí). Lời giao chỉ cho đọc phần ticket trích. Không đọc phần nào khác, không sửa. Đoạn đó không cho biết `release.sh` cũ có nhận mục có `/` hay không; T-075 xác nhận việc này.

**Rà soát độc lập (2026-10-31, agent ngữ cảnh mới, chỉ đọc, trên `437e232`): 1 chặn, 4 nên sửa, 6 ghi chú.** Code ở mục 3.1 khớp code hiện tại (đối chiếu từng tên; chưa biên dịch thử).

- **Chặn, đã xử lý (2026-10-31):** Nhật chọn cách (a). Spec ads câu 13 đã sửa (3 cách hiện, tổ hợp tắt, bật ra banner) và xác nhận lại; Định nghĩa hoàn thành của ticket sửa theo. Plan giữ như hiện nay. Ghi chú gốc: tổ hợp 4 của câu 13 (câu 1 ở trên). Spec `confirmed` chốt tổ hợp mà khung không làm được, và ticket ghi "bốn tổ hợp" ở Định nghĩa hoàn thành. Theo `docs/ops/pre-run-review.md` mục 3, spec sai thì chuyển `changed`, không giải quyết bằng câu trả lời trong plan. Hai cách:
  - (a) sửa câu 13 của spec (tổ hợp 4 ra banner, không có lớp), chuyển `changed` rồi xác nhận lại; sửa dòng 2 của Định nghĩa hoàn thành của ticket; plan giữ như hiện nay;
  - (b) mở ticket riêng đổi luật D-01, đề xuất về dự án mẫu.
- **Nên sửa, đã sửa trong bản này:**
  - ý cho T-075: T-075 không sửa `release.sh` ở app, nên phần chặn khi thiếu mã lớp là đề xuất về dự án mẫu (mục 2 dòng câu 14, mục 3.1);
  - `ads-guide.md` bước 6: chỉ thêm mục lớp khi `release.sh` nhận khuôn có `/`, vì bản đang giữ kiểm khuôn `<khóa vị trí>=ca-app-pub-...` và có thể dừng;
  - KDoc của `missingLayers` dài 121 ký tự: đã ngắt dòng;
  - nhánh `nativeEvery == null` không test nào làm đỏ: thêm `bottomBarLayers_onlyNativeSmallWithoutEvery`.
- **Ghi chú:**
  - Câu 5 ở trên (AI đã đọc `release.sh` dòng 130 tới 175 khi soạn plan): người duyệt ghi kết luận.
  - Mục 11.5 của `manual-test.md` kiểm luật chọn lớp, không kiểm mã lớp: staging dùng mã thử của Google.
  - `BottomAdBarTest.configuredLayerUnits_buildBannerAndLayer` không đi qua `AdsController.unitId`; giữ, vì đỏ đúng chỗ trên code cũ.
  - Chú thích ở `app/build.gradle.kts:46` còn khuôn cũ; phiên có thể sửa theo ADR-0029 (chỉ chú thích).
  - `misspelledLayerEntry_isIgnoredAndStillMissing` có thêm đối chứng (mục 4).
  - Số dòng `BottomAdBar.kt` lệch một hai dòng; không ảnh hưởng.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt
- **Người duyệt:** Tuấn, **duyệt chéo:** Nhật
- **Ghi chú:** Nhật chọn "Tuấn và Nhật duyệt" khi AI hỏi, ngày 2026-10-31. Trả lời mục 8:
  - câu 1: sửa spec ads câu 13 (3 cách hiện; tổ hợp tắt, bật ra banner), Nhật xác nhận spec lại;
  - câu 2: suy từ khai báo (`NATIVE_SMALL` không có `nativeEvery`);
  - câu 3, 4: `missingLayers` riêng; không sửa `release.md`; ghi chú khuôn mã lớp vào ticket T-075; phần chặn khi phát hành là đề xuất về dự án mẫu;
  - câu 5: chấp nhận việc đã đọc `scripts/release.sh` lúc soạn plan (không sửa). Nhắc lại quy tắc: phiên chạy T-084 không đọc hay sửa `scripts/release.sh`.

## 10. Nhật ký thực hiện

### Dừng 2026-11-01, phiên bản plan 1 (chưa chạy)

- **Lý do:** đợt đồng bộ dự án mẫu 47782ec..151b4c2 (PR #279) đã mang vào `:core:ads` T-056 của mẫu: cấu hình phát hành nhận khóa chung theo khe `banner=<mã>` và `native_collap=<mã>`, một mã cho lớp đó của mọi thanh đáy, và `ConfiguredAdUnitIds.missing` liệt kê khe còn thiếu. Plan này viết cho code trước đợt đó (khóa `<khóa vị trí>/<định dạng lớp>`).
- **Quyết định:** Nhật chọn "Dừng T-084, dùng khe chung của mẫu" (2026-11-01). Spec ads câu 14 trả lời lại theo khóa khe chung. Phần bản phát hành báo thiếu mã lớp đi cùng `release.sh` mới của mẫu ở T-075.
- **Code:** không có nhánh, không có commit.
