---
ticket: T-098
title: Lấy core/ads và hai tài liệu ads của dự án mẫu lên mốc bc3e80e (kích thước native mới, MAX toàn màn hình chưa bật)
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn: để trống
risk_level: high             # low | medium | high
token_estimate: 39.0M        # cỡ L theo scripts/token-usage.py estimate (2026-11-16), không nâng tay
blocking_points: [ads, core]
run_order:                   # AI điền khi lập danh sách chạy
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-11-16
lane:                        # để trống: phiên tự chạy
---

# PLAN T-098: LẤY CORE/ADS VÀ HAI TÀI LIỆU ADS CỦA DỰ ÁN MẪU LÊN MỐC BC3E80E

## 1. Tóm tắt

Đưa `core/ads`, `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md` từ mốc `698c5b3` lên `bc3e80e` của dự án mẫu. Phần gộp đã có sẵn trên nhánh `template-sync/take-ads-bc3e80e` (người làm trước phiên, ngày 2026-11-16). Phiên làm cho nhánh đó qua `scripts/check.sh`: đổi test cũ theo kích thước khe mới (126 dp ở khung chuẩn thay 80 dp), đổi tên vị trí mẫu trong ba test của `core/ads`, ghi lại 12 ảnh chuẩn `_ad` của poses, camera, settings theo thiết kế đã duyệt, sửa các bước tay ở `manual-test.md` và ghi chú ở `docs/template-sync.txt`. Kiểm bằng test của khung (đã đi kèm nhánh), test đo kích thước của ba feature với số mới, ảnh chuẩn so với thiết kế (`design-compare.sh`), và thử tay trên bản staging.

## 2. Hiểu yêu cầu

Ticket theo câu 20 của `features/ads/spec.md` (Nhật trả lời và xác nhận ngày 2026-11-16). Spec ads không có kịch bản riêng; tiêu chí thanh đáy ở các spec màn:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| poses KB-6, tiêu chí 1 | Khi khóa bật và người dùng đã đồng ý thì thanh đáy native nhỏ (cao theo khung, D-02) hiện ở đáy thư viện; lưới cuộn phía trên thanh, thẻ cuối không bị che | Khe 126 dp ở 411 dp, 106 dp ở 360 dp, 200 dp ở 673 dp (cạnh ngắn của cửa sổ); nội dung chừa khe + 16 dp |
| poses KB-6, tiêu chí 2 | Thanh đáy ở chi tiết nằm dưới nút "Chụp với tư thế này"; nút không bị che hay đẩy ra ngoài | Thanh nút chính kết thúc ngay ở mép trên khe |
| poses KB-6, tiêu chí 3 | Khóa tắt, quảng cáo đang tải hay không tải được thì không chừa khoảng trống | Giữ nguyên (test `realBottomBar_withoutAds_takesNoSpace`) |
| settings KB-1 | Thanh đáy ở đáy cài đặt; danh sách cuộn phía trên, dòng cuối không bị che | Như poses |
| settings KB-1, lớp nổi | Khi `is_show_native_collap` bật thì lớp quảng cáo nổi có thể phủ lên đáy danh sách và thu gọn được; thu gọn thì dòng cuối tới được | Lớp cao 299 dp ở khung chuẩn, tắt khi cửa sổ thấp; code và test của khung |
| camera KB-9 | Thanh đáy ở đáy màn xem lại, dưới hàng nút; ảnh co lại cho vừa, giữ tỷ lệ, không bị cắt hay che; bốn nút không bị che | Khe cao thêm nên ảnh co thêm ở mọi trạng thái, kể cả trạng thái không lỗi ở khung chuẩn (trước đây ảnh giữ 379 x 505 ở khung chuẩn) |
| camera KB-11 (máy gập, màn ngang) | Nút chính trong cửa sổ, trên thanh | Màn ngang 960 x 520 dp: khe 169 dp nên cột phải còn 251 dp, dưới ngưỡng xếp dọc (264): hàng nút phụ nằm ngang ở cả trạng thái không lỗi |
| spec ads câu 20, ý b | Lớp thu gọn co vùng ảnh hay không mở khi cửa sổ thấp | Code và test của khung (`BottomAdBarTest`, `AdsControllerNativeStockTest`) |
| spec ads câu 20, ý c | Thử lại native giãn dần 5, 10, 20, 40, 60 giây, đặt lại khi về foreground hay có mạng | Code và test của khung (`AdsControllerNativeRetryTest`, `NativeRetryCountTest`) |
| spec ads câu 20, ý d | MAX toàn màn hình có trong app, chưa bật khi không có khóa MAX | Code và test của khung (`AdsControllerMaxInterstitialTest`, `AdsControllerMaxRewardedTest`, `AdsControllerMaxAppOpenTest`) |

Thiết kế tham chiếu (Nhật duyệt 2026-11-16, PR #354): `features/poses/design/screens/PosesScreen_ad_light.png`, `PosesScreen_ad_dark.png`, `PoseDetailScreen_ad_light.png`, `PoseDetailScreen_ad_dark.png`; `features/settings/design/screens/SettingsScreen_ad_light.png`, `SettingsScreen_ad_dark.png`; `features/camera/design/screens/ReviewScreen_ad_dark.png`, `ReviewScreen_ad_dark_unfolded.png`; số liệu trong `design/context/` cùng tên. Thẻ native nhỏ theo `ads-business-rules.md` (bản `bc3e80e`, D-02) và thiết kế `Native/Small` của dự án mẫu.

ADR liên quan: ADR-0001, ADR-0020, ADR-0029, ADR-0030, ADR-0031, ADR-0045, ADR-1007.

## 3. Giải pháp

### 3.1. Cách làm

**Điểm bắt đầu: nhánh `template-sync/take-ads-bc3e80e`, commit `4917154b`** (đã đẩy, `docs/ops/to-app.md`, mục 5). Nhánh có ba commit do `scripts/template-sync.sh --take` của dự án mẫu tạo, mỗi commit cho một đường dẫn:

- `163c26ab` `core/ads` từ `698c5b3` lên `bc3e80e`: 90 file (23 thêm, 67 sửa), 18 ảnh chuẩn của `core/ads`. Một xung đột đã gộp tay ở `NativeAdSlot.kt`, nhánh `Unavailable` khi giữ chỗ: `NativeAdSize.CARD -> cardHeight ?: NativeAdDimens.heightOf(size, ctaLarge, nativeScreenSize())`, `else -> NativeAdDimens.heightOf(size, ctaLarge, nativeScreenSize())` (giữ chiều cao `CARD` của app, thêm tham số mới của mẫu).
- `8aa9fe7d` `docs/ops/ads-guide.md`: hai xung đột, lấy bản mẫu, giữ tên của app `PosesScreenScreenshotTest` và `poses_bottom` thay `SampleScreenScreenshotTest`, `sample_native_small`.
- `4917154b` `docs/ops/ads-business-rules.md`: không xung đột.

`docs/template-sync.txt` trên nhánh đã bỏ ba dòng `hold=` của các đường dẫn này. Phần riêng của app trong `core/ads` (liệt kê ở `docs/template-sync.txt`) còn đủ sau khi gộp: `collapsibleLayer` và `layerFormats`, `startupAdPlacementFor`, đường kẻ của `AdSlot`, `PrivacyOptionsButton(content)`, `cardHeight`, năm test T-080 của `InListAdsTest`, `attachFullScreenHost`, `reloadMillis = MutableStateFlow(0L)` của `AdSlotTest`. Code của mẫu nằm trên nhánh của app, phiên đọc được; plan không dán lại diff 10 nghìn dòng.

Nhánh task: `claude/T-098-lay-core-ads` tạo từ `origin/template-sync/take-ads-bc3e80e`, rồi `git merge origin/main`. Xung đột ngoài `docs/template-sync.txt` và `docs/foundation.md` thì dừng (mục 7).

Các việc của phiên trên nhánh đó:

1. **Biên dịch test** (đã đo ở mục 4.1):
   - 10 chỗ trong 8 file test của feature gọi `BottomAdBarDefaults.nativeReservation` như thuộc tính: đổi thành `BottomAdBarDefaults.nativeReservation()` (hàm composable mới của khung).
   - 4 chỗ trong 3 test của `core/ads` dùng `AdPlacement.SAMPLE_NATIVE_SMALL` (`AdTelemetryTest`, `AdsControllerNativeStockTest`, `CollapsibleNativeAdTest`): đổi thành `AdPlacement.POSES_BOTTOM`, bỏ dòng trùng nếu có (quy ước ở `docs/template-sync.txt`).
2. **Dữ liệu test của `core/ads` theo tên vị trí của app** (ADR-0029): chuỗi log mong đợi `"Collapsible sample_native_small not shown: window ... dp has no room"` trong `AdsControllerNativeStockTest.loggedNoRoom` và `CollapsibleNativeAdTest.noRoomLog_text` đổi thành `poses_bottom`. `AdPlacementAppTest` dòng kiểm `sample_native_small` không có trong app giữ nguyên.
3. **Khe giả của test feature cao theo khung:** khe giả đang viết cứng `80.dp` đổi thành `BottomAdBarDefaults.nativeSlotHeight()`: `fakeBar` của `PosesScreenTest` và `PoseDetailScreenTest`, `FakeAdSlot` và khe `AndroidView` của `ReviewScreenTest`, ba `FakeBar(..., slotHeight = 80.dp)` của `SettingsScreenTest`. Hằng `SLOT_HEIGHT = 80f` của `PosesScreenTest` thành `126f` (khung chuẩn). Câu khẳng định về kích thước viết lại theo mục 3.5. Chú thích nói "fake 80 dp slot" hay "keeps 96" đổi theo: chú thích trước nhóm test thanh đáy và KDoc của `fakeAdBar` (`ReviewScreenTest`), của `fakeBar` (`PosesScreenTest`, `PoseDetailScreenTest`), chú thích "a fake 80 dp slot" của `ReviewScreenUnfoldedTest`, và chú thích "keeps 96" (hay "16 + 96") của các test đối chứng `realBottomBar_withoutAds_takesNoSpace`, `realAdBar_withoutAds_takesNoSpace`, `small_realBottomBar_withoutAds_takesNoSpace` thành "keeps the slot and its margin".
4. **Ảnh chuẩn:** ghi lại đúng 12 ảnh ở mục 3.2 (lệnh ở mục 4). Ảnh khác không đổi: lần chạy thử thấy `PosesScreen_ad-collection_light`, ảnh của `feature/onboarding` và 18 ảnh của `core/ads` đều qua `verifyRoborazziDebug` trên nhánh take.
5. **`design-compare.sh`** cho poses, settings, camera; commit `design/review/` của ba feature.
6. **`docs/ops/manual-test.md`, mục 11.1:** bước 1, 9, 10 đổi "khoảng 80 dp" thành "khoảng 126 dp (khoảng 106 dp trên máy rộng 360 dp)"; bước 1 đổi "lớp quảng cáo nổi lớn (250 dp)" thành "lớp quảng cáo nổi lớn (khoảng 300 dp)". Thêm vào cuối bước 10 hai dòng: (a) đặt `is_show_native_collap = true` trên Firebase staging (bản staging để tắt qua `StagingRemoteValuesModule`, Firebase ghi đè được), đóng hẳn app rồi mở lại, trên máy gập mở ra đứng dọc vào thư viện: lớp nổi mở, cao khoảng 400 dp; (b) cùng khóa, máy gập nằm ngang hay chia đôi màn hình: lớp co vùng ảnh hay không mở, log `VGL.Ads` có `Collapsible poses_bottom not shown: window ... dp has no room`; trả khóa về như cũ. Không có bước xoay điện thoại (app khóa dọc). Thêm bước 11 cuối mục 11.1: ở thư viện, bật chế độ máy bay và chờ khoảng 40 giây (hộp thoại mạng của app phủ lên, không cần đóng): log `VGL.Ads` có các dòng thử lại của `poses_bottom` với khoảng chờ tăng dần, `Native poses_bottom retry in 5000 ms (failures 1)` rồi `10000 ms (failures 2)` khi khe đang trống, hay `refresh: failed, retry in ... ms` khi khe đã có quảng cáo; tắt chế độ máy bay: có `Native retry counters reset: network back`. Thêm vào bước 9, sau khi đặt `is_show_native_collap = true` (dòng của bước 10): mở Cài đặt, lớp nổi phủ đáy danh sách; bấm thu gọn, cuộn hết: dòng "Phiên bản" thấy trọn trên khe. Thêm vào bước 4: log `VGL.Ads` có `MAX skipped: no SDK key` lúc mở app, không có dòng `shown (MAX)`.
7. **`docs/template-sync.txt`:** bỏ đoạn ghi chú "core/ads is held at 698c5b3 ..." (từ "core/ads is held" tới "release.sh and docs/ops/release.md (taken at bc3e80e) do."); giữ đoạn liệt kê phần riêng của app trong `core/ads`, thêm vào đó một câu: "The CARD height when space is kept in NativeAdSlot.kt passes nativeScreenSize() like the template."
8. `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`: không sửa thêm (đã gộp trên nhánh).

Mẫu làm theo: T-074 (lấy `core/ads` qua nhánh take), T-093, T-094 (test đo thanh đáy, ảnh chuẩn `_ad`).

### 3.2. Danh sách file

Dòng thư mục (kết thúc bằng `/`) là phần lấy nguyên từ nhánh take; file app sửa thêm trong đó có dòng riêng.

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/` | Lấy nguyên từ nhánh take (90 file đổi từ `698c5b3`, gồm 18 ảnh chuẩn của `core/ads`, bản mẫu `AdsTestTheme`) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdTelemetryTest.kt` | `SAMPLE_NATIVE_SMALL` thành `POSES_BOTTOM` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsControllerNativeStockTest.kt` | `SAMPLE_NATIVE_SMALL` thành `POSES_BOTTOM`; chuỗi log của `loggedNoRoom` theo `poses_bottom` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/CollapsibleNativeAdTest.kt` | `SAMPLE_NATIVE_SMALL` thành `POSES_BOTTOM`; chuỗi mong đợi của `noRoomLog_text` theo `poses_bottom` |
| Sửa | `docs/ops/ads-guide.md` | Lấy nguyên từ nhánh take |
| Sửa | `docs/ops/ads-business-rules.md` | Lấy nguyên từ nhánh take |
| Sửa | `docs/template-sync.txt` | Nhánh take đã bỏ ba dòng `hold=`; phiên sửa ghi chú (mục 3.1, việc 7) |
| Sửa | `docs/ops/manual-test.md` | Mục 11.1 (mục 3.1, việc 6) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenTest.kt` | `nativeReservation()`, khe giả `nativeSlotHeight()`, `SLOT_HEIGHT = 126f`, câu khẳng định mới (mục 3.5) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenScreenshotTest.kt` | `nativeReservation()` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenTest.kt` | `nativeReservation()`, khe giả `nativeSlotHeight()`, câu khẳng định mới |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenScreenshotTest.kt` | `nativeReservation()` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenTest.kt` | `nativeReservation()`, khe giả `nativeSlotHeight()`, câu khẳng định mới; đổi tên `adBar_slotAtBottom_photoKept_saveMovesUp` thành `adBar_slotAtBottom_photoShrinks_saveMovesUp` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenScreenshotTest.kt` | `nativeReservation()` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenUnfoldedTest.kt` | Câu khẳng định mới; đổi tên `adBar_slotFullWidth_photoKept_saveAbove` thành `adBar_slotFullWidth_photoShrinks_saveAbove`; chú thích "a fake 80 dp slot" thành "a fake slot as high as the framework's" |
| Sửa | `feature/settings/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsScreenTest.kt` | `nativeReservation()`, `slotHeight = BottomAdBarDefaults.nativeSlotHeight()`, câu khẳng định mới |
| Sửa | `feature/settings/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsScreenScreenshotTest.kt` | `nativeReservation()` |
| Sửa | `feature/poses/src/test/screenshots/PosesScreen_ad_light.png` | Ghi lại: khe 126 dp, thẻ mới |
| Sửa | `feature/poses/src/test/screenshots/PosesScreen_ad_dark.png` | Ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PosesScreen_ad_small.png` | Ghi lại: khe 106 dp |
| Sửa | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_light.png` | Ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_dark.png` | Ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_small.png` | Ghi lại |
| Sửa | `feature/camera/src/test/screenshots/ReviewScreen_ad_dark.png` | Ghi lại: ảnh co thêm |
| Sửa | `feature/camera/src/test/screenshots/ReviewScreen_ad_dark_unfolded.png` | Ghi lại: khe 641 x 200 dp |
| Sửa | `feature/camera/src/test/screenshots/ReviewScreen_ad_small.png` | Ghi lại |
| Sửa | `feature/settings/src/test/screenshots/SettingsScreen_ad_light.png` | Ghi lại |
| Sửa | `feature/settings/src/test/screenshots/SettingsScreen_ad_dark.png` | Ghi lại |
| Sửa | `feature/settings/src/test/screenshots/SettingsScreen_ad_small.png` | Ghi lại |
| Sửa | `docs/features/poses/design/review/` | Sinh lại bằng `scripts/design-compare.sh poses` |
| Sửa | `docs/features/camera/design/review/` | Sinh lại bằng `scripts/design-compare.sh camera` |
| Sửa | `docs/features/settings/design/review/` | Sinh lại bằng `scripts/design-compare.sh settings` |
| Sửa | `docs/foundation.md` | Chỉ khi `scripts/foundation.py` đổi sau khi gộp `origin/main` (L6) |

Chỉ sửa test của ba feature và ba test của `core/ads`; không sửa code Kotlin ngoài test: code chính biên dịch được trên nhánh take; test của `:app`, `feature/loading`, `core/network` qua trên nhánh take (mục 4.1). Test của `feature/onboarding` chỉ dùng `NativeAdCard` và `NativeAdPlaceholder` của thẻ bước, chữ ký giữ nguyên; lần chạy thử thấy mọi ảnh của onboarding qua.

Nơi đọc các tài liệu mà plan sửa (`grep -rn` trong `scripts/`, `.github/`, `*/src/test`), không bị ảnh hưởng:

- `scripts/ads-plan.py` đọc bảng tham chiếu của `docs/ops/ads-business-rules.md` và dẫn `docs/ops/ads-guide.md`: chạy thử trên bản gộp (mục 4.1) ghi được file Excel, 14 vị trí, 76 khóa Remote Config, 12 sự kiện; test của nó trong `scripts/tests/run.sh` dựng repo giả riêng, không đọc tài liệu của repo.
- Test của `core/ads` (`core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdNetworkResponsesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdCardTest.kt`) và của `core/analytics` (`core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/FirebaseAdMetricsTest.kt`) chỉ dẫn mã luật của tài liệu trong chú thích, không đọc file; test `core/ads` đi cùng bản mẫu qua ở mục 4.1, `core/analytics` không đổi.
- `docs/template-sync.txt`: `scripts/tests/run.sh` dựng file riêng trong repo giả; `scripts/setup-check-app.sh` chỉ nhắc tên file trong một thông báo. Không ai đọc phần ghi chú phiên sửa.
- `docs/foundation.md`: `.github/workflows/pr-check.yml` và `scripts/tests/run.sh` kiểm trang khớp với ADR; plan chỉ sinh lại bằng `scripts/foundation.py` khi gộp `origin/main` làm nó đổi.

Nơi đọc ảnh chuẩn đổi: `scripts/design-compare.sh` (đọc `src/test/screenshots/` của ba feature, sinh `design/review/`, có trong bảng); `scripts/check.sh` (chỉ kiểm đường dẫn `captureRoboImage`, không đổi). `grep -rn '_ad_' scripts/ .github/` không có chỗ nào đọc tên các ảnh này.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay dời vị trí. Đổi kích thước khe đáy native nhỏ và lớp thu gọn, cách lớp thu gọn tắt khi cửa sổ thấp, nhịp thử lại native (spec ads câu 20, ý a, b, c); thêm interstitial, rewarded, app open của MAX, chưa bật vì bản build không có khóa MAX (câu 16, câu 20 ý d).
- Sự kiện phân tích: Không chạm. `ad_platform` đã có từ PR #351 (`:core:analytics`); `core/ads` mới ghi thêm doanh thu và phễu của MAX qua cùng `FirebaseAdMetrics`, chỉ khi MAX chạy (không xảy ra khi chưa có khóa MAX).
- File R2: Không chạm.
- Khóa Remote Config: không thêm (lần chạy thử: `RemoteConfigKeysTest` của `:app` qua trên nhánh take, bảng khóa không đổi).
- **Quyền truy cập:** Không thêm. `core/ads/src/main/AndroidManifest.xml` không đổi trong khoảng `698c5b3..bc3e80e` (`git diff --stat` rỗng); `MergedManifestTest` của `:app` qua trên nhánh take.
- **Thư viện và SDK:** Không thêm. `core/ads/build.gradle.kts`, `core/ads/consumer-rules.pro` và `gradle/libs.versions.toml` không đổi trong khoảng này; code MAX dùng `com.applovin:applovin-sdk` và `com.applovin.mediation:google-adapter` đã có (`applovin-sdk` và `max-mediation-google` trong `gradle/libs.versions.toml`, `core/ads/build.gradle.kts` đã khai báo; câu 16).

### 3.4. Phương án dự phòng

Không có. Lần chạy thử (mục 4.1) đã đo mọi chỗ đỏ trên nhánh take.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` không có tên để tìm: code Kotlin ngoài test chỉ đổi qua dòng thư mục `core/ads/` (bản mẫu). Thay vào đó, lần chạy thử ở mục 4.1 chạy mọi test của `core/ads`, `feature/poses`, `feature/camera`, `feature/settings`, `feature/onboarding`, `feature/loading`, `core/network` và `:app` trên code mới; mọi test đỏ có một dòng dưới đây. Test của `core/ads` lấy cùng bản mẫu, đã chỉnh theo code mới ở dự án mẫu. Mọi số đo từ đầu khung test (không có thanh trạng thái), khung chuẩn `w411dp-h891dp` trừ khi ghi khác. Khe mới: 126 dp ở cạnh ngắn 411, 106 dp ở 360, 200 dp ở 673, 169 dp ở 520, 197 dp ở 593 (D-02: 90 + (cạnh ngắn − 320) x 110 / 280, trong 90 tới 200, làm tròn tới dp nguyên như `NativeAdDimens`); khoảng chừa = khe + 16.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Khe đáy cao 80 dp | Khe cao theo cạnh ngắn của cửa sổ (D-02) | `PosesScreenTest.bottomBar_lastCardScrollsAboveSlot` (`SLOT_HEIGHT`) | slot height 126; giữ "slot 16 above the bottom", "last card above slot" |
| Nội dung chừa 16 + 96 dưới thẻ cuối | Chừa 16 + 142 | `PosesScreenTest.realBottomBar_withoutAds_takesNoSpace` | control: with a slot 158 (đã đo); giữ câu không chừa khi không có khe (16) |
| Nút của trạng thái lỗi hay rỗng dời lên nửa của 96 (khung 360 x 640) | Dời lên nửa của 122 | `PosesScreenTest.smallScreen_bottomBar_onErrorAndSavedEmpty_actionAboveSlot` | "$action moves up half of 122": 61 (đã đo); giữ đối chứng 0 |
| Khe ở chi tiết 80 dp, thanh nút kết thúc ở 795 | Khe 126, thanh nút kết thúc ở 749 | `PoseDetailScreenTest.bottomBar_slotBelowButtonBar_buttonNotCovered` | slot height 126; chú thích "the button bar ends at y = 749, where the slot starts (653 + 96)"; giữ các câu còn lại |
| Chi tiết chừa 96 dưới thanh nút | Chừa 142 | `PoseDetailScreenTest.realBottomBar_withoutAds_takesNoSpace` | control: with a slot 142 (đã đo) |
| Cài đặt, khung 360 x 640: khe 80 | Khe 106 | `SettingsScreenTest.small_nativeBottomBar_lastRowScrollsAboveSlot` | slot height 106 (đã đo); giữ các câu còn lại |
| Cài đặt chừa 96 dưới dòng cuối | Chừa 122 (khung 360 x 640) | `SettingsScreenTest.small_realBottomBar_withoutAds_takesNoSpace` | control: with a slot 122 (đã đo) |
| Màn xem lại: khe 80 ở y 795; ảnh giữ 379 x 505; chỉ nút chính dời lên 96 | Khe 126 ở y 749; ảnh co lại cho vừa, giữ 3:4: 372.75 x 497; nút chính 673 tới 729, dời lên 142; hàng nút phụ ở 585 | `ReviewScreenTest.adBar_slotAtBottom_photoKept_saveMovesUp` (đổi tên `..._photoShrinks_saveMovesUp`) | slot height 126, slot top 749, save top 673, save bottom 729, save to the slot 20, save moved up 142, photo width 372.75, photo height 497 (sai số 1), photo top 64, actions top 585 (sai số 1); bỏ hai câu "kept" của ảnh và hàng nút, thay bằng `assertPhoto3to4(photo)` và "photo height ${photo.height} < before ${photoBefore.height}" |
| Màn xem lại SAVED: nút chính ở 719, ảnh cao 505.3; lỗi bộ nhớ: ảnh 359.3 x 479 | Nút chính 673, ảnh cao 497; lỗi bộ nhớ: ảnh 324.75 x 433 | `ReviewScreenTest.adBar_savingAndSaveError_slotStays` (và `assertSlotStays`: slot top 795) | SAVED save top 673, SAVED photo height 497, FAILED_STORAGE_FULL photo 324.75 x 433 (sai số 1), slot top 749; giữ chiều cao lỗi 48 |
| Màn xem lại chừa 116 dưới nút chính | 162 | `ReviewScreenTest.realAdBar_withoutAds_takesNoSpace` | control: save to the bottom 162 (đã đo); giữ 20 khi không có khe |
| Thiếu quyền: ảnh 344.3 x 459 | 309.67 x 412.9 | `ReviewScreenTest.adBar_permissionBlocked_photoShrinks_allAbove` | photo width 309.67 (đã đo), photo height 412.9 (sai số 1); giữ lỗi 68, các câu "above" |
| Khung 360 x 640: khe ở 544; ảnh 219 x 292; nút chính 468 tới 524; thiếu quyền ảnh 156 x 208 | Khe ở 518; ảnh 199.67 x 266.2; nút chính 442 tới 498; thiếu quyền ảnh 136.5 x 182 | `ReviewScreenTest.small_adBar_photoShrinks_allVisible` | slot top 518 (đã đo), photo 199.67 x 266.2 (đã đo bề rộng), save 442 tới 498, blocked photo 136.5 x 182 (sai số 1); giữ ảnh khi chưa có khe 291 x 388 |
| pt-BR, 360 x 640, thiếu quyền: ảnh 131 x 174.7 | 111.67 x 148.9 | `ReviewScreenTest.smallPortuguese_adBar_permissionBlocked_allVisible` | photo width 111.67 (đã đo), height 148.9 (sai số 1); giữ chiều cao lỗi 101.3 và "all visible above" |
| Máy gập mở 673 x 841: khe ở 745; ảnh giữ 450 x 600; nút chính dời lên 96, đáy ở 725 | Khe 200 ở 625; ảnh 405.75 x 541; nút chính đáy ở 605, dời lên 216 | `ReviewScreenUnfoldedTest.adBar_slotFullWidth_photoKept_saveAbove` (đổi tên `..._photoShrinks_saveAbove`) | slot width 641, slot left 16, slot top 625 (đã đo), photo 405.75 x 541 (sai số 1), save bottom 605, save moved up 216; bỏ "photo height kept", thay bằng tỷ lệ 0.75 và "photo height ${photo.height} < before ${photoBefore.height}"; giữ "save right of the photo" |
| Màn ngang 960 x 520: khe ở 424; nút chính 348 tới 404; ảnh 255 x 340; hàng nút phụ xếp dọc khi không lỗi | Khe 169 ở 335; nút chính 259 tới 315; ảnh 188.25 x 251; cột phải 251 dưới ngưỡng 264 nên hàng nút phụ nằm ngang ở 64 tới 136 | `ReviewScreenUnfoldedTest.landscape_adBar_saveInsideWindow` | slot top 335 (đã đo), save 259 tới 315 (đã đo 259), photo 188.25 x 251 (sai số 1), tỷ lệ 0.75; thêm: actions top 64, actions bottom 136 |
| Màn ngang 960 x 520 có lỗi: khe ở 424, nút chính 348 tới 404, lỗi kết thúc ở 332, bắt đầu ở 284 (pt-BR 264) | Khe ở 335, nút chính 259 tới 315, lỗi kết thúc ở 243 (nút chính − 16), bắt đầu ở 195 (pt-BR 175: lỗi giữ chiều cao 48, pt-BR 68) | `ReviewScreenUnfoldedTest.landscape_adBar_saveError_compactColumn`, `..._portuguese` (`assertCompactColumn`) | slot top 335 (đã đo), save 259 tới 315, error bottom 243, errorTop 195 và 175 (sai số 1); giữ hàng nút phụ 64 tới 136, số dòng, nút lỗi bên cạnh chữ, nhãn không bị cắt, bề rộng nút chính |
| Màn ngang 841 x 593: cột phải 413 (dưới ngưỡng 420 khi có lỗi, trên 264 khi không); nút chính 421 tới 477 | Khe 197: cột phải 296, vẫn giữa 264 và 420; nút chính 304 tới 360 | `ReviewScreenUnfoldedTest.landscape_adBar_thresholdBoundary` | chú thích "Column 296 high with the slot: under the threshold with an error (420), over it without (264)"; `assertSaveAt` 304 tới 360 cho cả hai trạng thái (đã đo 304); giữ hàng nút phụ nằm ngang 72 khi lỗi, xếp dọc 64 tới 256 khi không lỗi |
| Thẻ native nhỏ: icon 36, nhãn "Ad" 10 sp dưới tiêu đề, CTA 34 dp 13 sp | Icon 44, "Ad" 13 sp trước tiêu đề 16 sp, CTA 44 dp 18 sp | Ảnh chuẩn 12 ảnh `_ad` của mục 3.2 | Ghi lại theo plan (mục 4) |
| Code của khung: thử lại đều 5 giây, khe cố định, không có MAX toàn màn hình | Theo câu 20 | Test của `core/ads` từ mẫu | Lấy nguyên bản mẫu; ba test đổi tên vị trí (mục 3.1, việc 1 và 2) |

Đây là câu khẳng định plan viết sẵn (mục 3.5), không phải lệch theo ADR-0031 của phiên: phiên viết đúng các câu trên. Số ghi "(đã đo)" lấy từ lần chạy thử ở mục 4.1; số khác tính từ cùng công thức bố cục của màn (ảnh = chỗ còn lại − khoảng cố định, giữ 3:4), sai số 1 dp. Rà soát độc lập của plan đã tính lại mọi số từ code bố cục và cách khung tính khe. Lần chạy của phiên ra số lệch quá sai số của dòng thì không đổi số: dừng theo điều kiện dừng 8 của `docs/risk-scoring.md` (mục 7). Sai số là số ghi ở dòng; số "(đã đo)" không ghi sai số thì dùng sai số sẵn có của câu khẳng định trong test.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| poses KB-6 tiêu chí 1, 3 | Compose UI test | `PosesScreenTest.bottomBar_lastCardScrollsAboveSlot`, `realBottomBar_withoutAds_takesNoSpace`, `smallScreen_bottomBar_onErrorAndSavedEmpty_actionAboveSlot` (câu mới ở mục 3.5) |
| poses KB-6 tiêu chí 2 | Compose UI test | `PoseDetailScreenTest.bottomBar_slotBelowButtonBar_buttonNotCovered`, `realBottomBar_withoutAds_takesNoSpace`, `smallScreen_withBottomBar_buttonFullyVisible` (khung 360 x 640), `wide_bottomBar_slotFullWidth_buttonAbove` (hai test sau qua với khe mới, giữ nguyên) |
| settings KB-1 | Compose UI test | `SettingsScreenTest.small_nativeBottomBar_lastRowScrollsAboveSlot`, `small_realBottomBar_withoutAds_takesNoSpace`, `saveFailedSnackbar_aboveSlot` |
| settings KB-1, lớp nổi thu gọn được | Unit test của khung, kiểm tra thủ công (máy thật: quảng cáo thật) | `BottomAdBarTest`, `CollapsibleNativeAdTest`; `manual-test.md`, mục 11.1, dòng mới của bước 9 (lớp nổi ở Cài đặt, thu gọn rồi cuộn hết, dòng "Phiên bản" thấy trọn) |
| camera KB-9 | Compose UI test | `ReviewScreenTest.adBar_slotAtBottom_photoShrinks_saveMovesUp`, `adBar_savingAndSaveError_slotStays`, `realAdBar_withoutAds_takesNoSpace`, `adBar_permissionBlocked_photoShrinks_allAbove`, `small_adBar_photoShrinks_allVisible`, `smallPortuguese_adBar_permissionBlocked_allVisible` |
| camera KB-11 (máy gập, màn ngang) | Compose UI test | `ReviewScreenUnfoldedTest.adBar_slotFullWidth_photoShrinks_saveAbove`, `landscape_adBar_saveInsideWindow`, `landscape_adBar_saveError_compactColumn`, `landscape_adBar_saveError_compactColumn_portuguese`, `landscape_adBar_thresholdBoundary` |
| Câu 20 ý a: khe theo cạnh ngắn, thẻ mới, lớp 299 dp | Unit test, screenshot test của khung | `NativeAdDimensTest`, `BottomAdBarTest`, `NativeAdCardTest`, ảnh `NativeAdCard_small_light`, `BottomAdBar_expanded_light` của `core/ads` (bản mẫu) |
| Câu 20 ý b: lớp thu gọn khi cửa sổ thấp | Unit test của khung | `BottomAdBarTest` (ảnh `BottomAdBar_landscape_light`, `_split_light`, `_short-window_light`), `AdsControllerNativeStockTest.landscape_layerNoRoom_stockKept_noEligible_logged`, `configurationChange_layerGoes_adDestroyed_logged`, `CollapsibleNativeAdTest.noRoomLog_text` |
| Câu 20 ý b: `review_bottom` không có lớp nổi | Unit test | `BottomAdBarTest`, `AdPlacementAppTest` (phần riêng của app, giữ nguyên) |
| Câu 20 ý c: thử lại giãn dần | Unit test của khung | `AdsControllerNativeRetryTest`, `NativeRetryCountTest` |
| Câu 20 ý d: không có khóa MAX thì tải từ AdMob | Unit test của khung | `AdsControllerMaxInterstitialTest`, `AdsControllerMaxRewardedTest`, `AdsControllerMaxAppOpenTest`, `FullScreenPlatformsTest` |
| Phần riêng của app trong `core/ads` còn đủ | Unit test | `AdSlotTest`, `PrivacyOptionsButtonTest`, `InListAdsTest` (42 test, gồm năm test T-080), `AdPlacementTest`, `AdPlacementAppTest`, `AdUnitIdsTest`, `AdPlacementSourceTest`, `NativeAdCellTest`; `StagingRemoteValuesModuleTest` của `app/src/testStaging` |
| Giao diện thư viện | Screenshot test | `PosesScreen_ad_light.png`, `PosesScreen_ad_dark.png` |
| Giao diện chi tiết | Screenshot test | `PoseDetailScreen_ad_light.png`, `PoseDetailScreen_ad_dark.png` |
| Giao diện cài đặt | Screenshot test | `SettingsScreen_ad_light.png`, `SettingsScreen_ad_dark.png` |
| Giao diện màn xem lại | Screenshot test | `ReviewScreen_ad_dark.png`, `ReviewScreen_ad_dark_unfolded.png` |
| Màn hình thấp | Screenshot test khung `w360dp-h640dp-xxhdpi` | `PosesScreen_ad_small.png`, `PoseDetailScreen_ad_small.png`, `SettingsScreen_ad_small.png`, `ReviewScreen_ad_small.png` |
| Đúng thiết kế | Screenshot test cùng tên với ảnh thiết kế, `scripts/design-compare.sh poses`, `camera`, `settings`, rà soát độc lập theo mục 7 của mẫu thiết kế | `PosesScreen_ad_light`, `PosesScreen_ad_dark`, `PoseDetailScreen_ad_light`, `PoseDetailScreen_ad_dark`, `SettingsScreen_ad_light`, `SettingsScreen_ad_dark`, `ReviewScreen_ad_dark`, `ReviewScreen_ad_dark_unfolded` |
| Các ảnh khác không đổi | Screenshot test | `verifyRoborazziDebug` của `core/ads`, `feature/poses`, `feature/camera`, `feature/settings`, `feature/onboarding` qua; `git status --short -- '*/src/test/screenshots/*'` chỉ có 12 ảnh của mục 3.2 |
| Khe và thẻ trên bản staging (thư viện, chi tiết, cài đặt, màn xem lại) | Kiểm tra thủ công (máy thật: quảng cáo thật) | `docs/ops/manual-test.md`, mục 11.1, bước 1, 2, 9, 10: khe khoảng 126 dp, thẻ mới, nội dung không bị che |
| Lớp thu gọn trên máy gập | Kiểm tra thủ công (máy thật: gập có bản lề) | `manual-test.md`, mục 11.1, hai dòng mới của bước 10: đứng dọc lớp khoảng 400 dp; nằm ngang hay chia đôi lớp co hay không mở, có log `not shown` |
| Thử lại giãn dần khi không có quảng cáo | Kiểm tra thủ công (máy thật: quảng cáo thật) | `manual-test.md`, mục 11.1, bước 11 mới: dòng thử lại của `poses_bottom` với khoảng chờ tăng dần; có mạng lại thì `counters reset: network back` |
| Quảng cáo toàn màn hình vẫn tải từ AdMob khi chưa có khóa MAX (hồi quy của luồng interstitial, combo) | Kiểm tra thủ công (máy thật: quảng cáo thật) | `manual-test.md`, mục 11.1, bước 3 tới 8 và mục 11.2, 11.3, 11.6: hiện và đóng như trước; log `VGL.Ads` có `MAX skipped: no SDK key`, không có dòng `shown (MAX)` (dòng mới của bước 4) |

Ghi lại ảnh chuẩn (chỉ 12 ảnh của mục 3.2): `./gradlew :feature:poses:recordRoborazziDebug :feature:poses:testDebugUnitTest :feature:camera:recordRoborazziDebug :feature:camera:testDebugUnitTest :feature:settings:recordRoborazziDebug :feature:settings:testDebugUnitTest --rerun`, rồi `git status --short -- '*/src/test/screenshots/*'`; ảnh khác đổi vì nhiễu vẽ thì `git checkout -- <ảnh>`. Ảnh chuẩn của `core/ads` không ghi lại (bản mẫu).

So với thiết kế: frame thiết kế vẽ thanh trạng thái 24 dp, screenshot test không có, nên ở `ReviewScreen_ad_dark` ảnh vừa chụp trong screenshot (372.75 x 497) lớn hơn frame (355 x 473); vị trí khe, thẻ và nút chính khớp theo mép dưới. Tương tự ở `ReviewScreen_ad_dark_unfolded`: frame vẽ thanh trên ở y 8 và ảnh 400 x 533 ở y 72, test có ảnh 405.75 x 541 ở y 64; nút chính cách thanh 20 trong code, 16 trên frame (README thiết kế camera đã ghi). Đây là lệch đã biết, rà soát xếp mức ghi chú.

**Ngoại lệ viết test trước:**
- Phần lấy nguyên từ nhánh take (`core/ads/`, hai tài liệu ads): test của mẫu đi cùng code trong cùng commit của nhánh; commit test của bước 3 gồm các thay đổi test của feature và ba test của `core/ads` ở mục 3.2, chạy trên nhánh take (code mới đã có), nên chúng không "thất bại trước code". Phiên xác nhận bằng đối chứng: tạm đổi `BottomAdBarDefaults.nativeSlotHeight()` của khe giả về `80.dp` trong một test (ví dụ `PoseDetailScreenTest.bottomBar_slotBelowButtonBar_buttonNotCovered`) thì test đỏ, rồi trả lại.
- `docs/ops/manual-test.md`, `docs/template-sync.txt`: chỉ là tài liệu.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-11-16, trong một worktree tạm của nhánh `template-sync/take-ads-bc3e80e` (`4917154b`) đã gộp `origin/main` (`345a4741`), không commit. Sửa tạm: `nativeReservation` thành `nativeReservation()` ở 8 file test của feature, `SAMPLE_NATIVE_SMALL` thành `POSES_BOTTOM` ở 3 test của `core/ads`; lượt 2 thêm khe giả `nativeSlotHeight()`.
- **Lệnh:** `./gradlew --continue :core:ads:verifyRoborazziDebug :feature:poses:verifyRoborazziDebug :feature:camera:verifyRoborazziDebug :feature:settings:verifyRoborazziDebug :feature:onboarding:verifyRoborazziDebug`; `./gradlew :app:testStagingDebugUnitTest :feature:loading:testDebugUnitTest :core:network:testDebugUnitTest`; lượt 2: `testDebugUnitTest --tests` cho `PosesScreenTest`, `PoseDetailScreenTest`, `ReviewScreenTest`, `ReviewScreenUnfoldedTest`, `SettingsScreenTest`. Chưa chạy `scripts/check.sh` (không cần: E dưới 2; phiên chạy ở bước 5).
- **Kết quả:**
  - Code chính của `core/ads` và `:app` biên dịch được không cần sửa. `:app`, `feature/loading`, `core/network`, `core/review`: qua.
  - `./gradlew :app:assembleStagingRelease` qua (R8 với các lớp MAX toàn màn hình mới dùng: `MaxInterstitialAd`, `MaxRewardedAd`, `MaxAppOpenAd`); PR của task không đổi file build hay luật R8 nên `pr-check` sẽ không tự build bản release.
  - `core/ads`: 1978 test, 3 đỏ, đều vì chuỗi log `sample_native_small` (mục 3.1, việc 2). 18 ảnh chuẩn của `core/ads` qua.
  - Ảnh chuẩn đỏ: đúng 12 ảnh của mục 3.2. Qua: `PosesScreen_ad-collection_light`, mọi ảnh của `feature/onboarding` (thẻ bước giữ 250 dp), mọi ảnh không có quảng cáo.
  - Test đo đỏ: 18 test của mục 3.5 (`ReviewScreenTest` 6, `ReviewScreenUnfoldedTest` 5, `PosesScreenTest` 3, `PoseDetailScreenTest` 2, `SettingsScreenTest` 2); số "(đã đo)" ở mục 3.5 là số thật của lượt 2 (câu khẳng định đầu tiên đỏ của mỗi test).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi kích thước định dạng native nhỏ và lớp thu gọn, nhịp thử lại native; thêm đường MAX toàn màn hình, chưa bật), 7 (module core `:core:ads`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | `core/ads` (90 file từ mẫu) và test, ảnh chuẩn của ba feature |
| B. Độ rõ của yêu cầu | 0 | Spec ads câu 20 đã trả lời và xác nhận, thiết kế đã duyệt, ticket đủ |
| C. Khả năng kiểm chứng tự động | 1 | Kích thước, bố cục, thử lại, MAX đều có test; khe và lớp trên máy gập thật kiểm tay |
| D. Ảnh hưởng chéo | 3 | Đổi kích thước thanh đáy mà poses, camera, settings dựa vào |
| E. Độ mới của kỹ thuật | 1 | Lặp lại cách lấy `core/ads` qua nhánh take (T-074) và test đo thanh đáy (T-093, T-094) |
| **Tổng** | 8 | **Mức: Cao (chạm điểm chặn)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `:core:ads` (mọi feature có quảng cáo), `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`, `docs/template-sync.txt`, `docs/ops/manual-test.md`.
- Task khác có thể xung đột: không có plan `approved` hay `draft` nào khác đang chờ chạy (2026-11-16). Plan sau này chạm thanh đáy hay test đo của ba feature phải chạy sau T-098.
- Lần đồng bộ dự án mẫu kế tiếp (`/to-app`): chạy sau khi T-098 merge, để `core/ads` không còn `hold=`.
- Đề xuất thứ tự chạy: một mình, hay đầu danh sách.

## 7. Điều kiện dừng riêng

- `git merge origin/main` vào nhánh task có xung đột ngoài `docs/template-sync.txt` và `docs/foundation.md`.
- Phần riêng của app trong `core/ads` (mục 3.1) bị mất hay đổi hành vi: test của nó đỏ mà không phải vì dữ liệu tên vị trí.
- Ảnh chuẩn ngoài 12 ảnh của mục 3.2 lệch mà không phải nhiễu vẽ (dừng như quy tắc chung).
- `RemoteConfigKeysTest` hay bảng khóa `docs/remote-config-keys.md` đổi (khung thêm khóa mà spec chưa duyệt).
- Manifest gộp có quyền mới (`MergedManifestTest` đỏ).
- Nhánh `origin/template-sync/take-ads-bc3e80e` không còn chứa commit `4917154b` (bị đẩy đè hay bị xóa).
- Số đo của một câu khẳng định mới ở mục 3.5 lệch quá sai số của dòng đó (điều kiện dừng 8 của `docs/risk-scoring.md`).

## 8. Câu hỏi cho người duyệt

1. Màn ngang 960 x 520 dp (máy gập nằm ngang, máy tính bảng nhỏ) có thanh đáy: khe cao hơn (169 dp) làm cột phải dưới ngưỡng xếp dọc, nên hàng nút phụ nằm ngang cả khi không có lỗi (trước đây xếp dọc). Đề xuất của AI: nhận, vì đó là bố cục gọn đã có của màn (KB-11) và mọi nút vẫn thấy trọn; không đổi code của màn.
2. Trên điện thoại khung chuẩn, ảnh ở màn xem lại co từ 379 x 505 xuống khoảng 373 x 497 khi có thanh (thiết kế vẽ 355 x 473 vì có thanh trạng thái). Đề xuất của AI: nhận như thiết kế đã duyệt; README thiết kế camera đã ghi bố cục này.
3. Ticket và `docs/CLAUDE.md` (mục Khi viết plan) đòi dán `diff` của phần lấy từ dự án mẫu vào mục 3.1, vì phiên của app không đọc được repo mẫu. Ở đây phần gộp nằm sẵn trên nhánh `template-sync/take-ads-bc3e80e` của repo app (90 file, khoảng 10 nghìn dòng), phiên đọc được; plan chỉ ghi nhánh, commit và chỗ gộp tay. Đề xuất của AI: nhận cách này; mục 7 dừng task khi nhánh không còn commit `4917154b`.
4. Duyệt chéo: plan rủi ro Cao. Người duyệt và người duyệt chéo là Tuấn và Nhật; Nhật là người phụ trách sản phẩm đã quyết câu 20.

## 9. Quyết định duyệt

- **Quyết định:** duyệt ngày 2026-11-16, các câu hỏi mục 8 nhận theo đề xuất của AI.
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** AI ghi theo lời Nhật (2026-11-16): "Cả toi va Tuấn đều đa duyệt. Triển khai theo đề xuất của AI." AI ghi theo lời Tuấn (2026-11-16): "Tôi là Tuấn, duyệt chéo plan T-098 theo đề xuất của AI". Câu hỏi mục 8: (1) nhận hàng nút phụ nằm ngang ở màn 960 x 520 dp, không đổi code của màn; (2) nhận ảnh xem lại khoảng 373 x 497 dp như thiết kế đã duyệt; (3) không dán diff vào mục 3.1, phiên đọc phần gộp trên nhánh `template-sync/take-ads-bc3e80e`, dừng khi nhánh mất commit `4917154b`; (4) Nhật duyệt, Tuấn duyệt chéo.

## 10. Nhật ký thực hiện
