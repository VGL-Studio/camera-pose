---
ticket: T-102
title: Lấy core/ads và hai tài liệu ads của dự án mẫu lên mốc 7bf8c3b (thẻ native hai tầng, luật thanh đáy mới)
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn: để trống
risk_level: high             # low | medium | high
token_estimate: 34.0M        # cỡ L theo scripts/token-usage.py estimate (2026-11-22), không nâng tay
blocking_points: [ads, core]
run_order:                   # AI điền khi lập danh sách chạy
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-11-22
lane:                        # để trống: phiên tự chạy
---

# PLAN T-102: LẤY CORE/ADS VÀ HAI TÀI LIỆU ADS CỦA DỰ ÁN MẪU LÊN MỐC 7BF8C3B

## 1. Tóm tắt

Đưa `core/ads`, `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md` từ mốc `bc3e80e` lên `7bf8c3b` của dự án mẫu. Phần gộp đã có sẵn trên nhánh `template-sync/take-ads-7bf8c3b` (dựng ngày 2026-11-21, có người theo dõi). Trên nhánh đó `:core:ads` qua 2084 test.

Phiên làm cho nhánh qua `scripts/check.sh`:
- viết lại 13 test đo kích thước của feature theo số mới (đo thật ở mục 3.5);
- ghi lại 12 ảnh chuẩn `_ad` theo thiết kế đã duyệt;
- bọc thư viện bằng `InContentAds(listOf(POSES_IN_COLLECTION))` (câu 22, ý e);
- khai báo `pose_detail_bottom` không có lớp nổi (câu 22, ý g);
- sửa các bước kiểm tay của `docs/ops/manual-test.md`.

Kiểm bằng: test của khung (đi kèm nhánh), test đo của ba feature với số mới, test mới cho ý e và ý g, ảnh chuẩn so với thiết kế (`design-compare.sh`), và thử tay trên bản staging.

## 2. Hiểu yêu cầu

Ticket theo câu 22 của `features/ads/spec.md` (Nhật trả lời và xác nhận ngày 2026-11-21, PR #374). Spec ads không có kịch bản riêng; tiêu chí thanh đáy nằm ở các spec màn:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| poses KB-6, tiêu chí 1 | Khi khóa bật và người dùng đã đồng ý thì thanh đáy native nhỏ (cao theo khung, D-02) hiện ở đáy thư viện; lưới cuộn phía trên thanh, thẻ cuối không bị che | Khe 106 dp ở cỡ chữ mặc định ở mọi khung; nội dung chừa 122 dp |
| poses KB-6, tiêu chí 2 | Thanh đáy ở chi tiết nằm dưới nút "Chụp với tư thế này"; nút không bị che hay đẩy ra ngoài màn hình, kể cả khi `is_show_native_collap` bật: chi tiết không có lớp nổi | Thanh nút chính kết thúc ở mép trên khe; `POSE_DETAIL_BOTTOM` khai báo `collapsibleLayer = false` |
| poses KB-6, tiêu chí 3 | Khóa tắt hay chưa tải được quảng cáo nào thì không chừa khoảng trống; khe đang tải lượt đầu thì nội dung chừa đúng chỗ khe chiếm | D-04 của khung mới; test của khung (`BottomAdBarTest`, `BannerFrameTest`); test feature giữ phần "khóa tắt" |
| poses KB-6, lớp nổi | Khi `is_show_native_collap` và `is_show_poses_in_collection` cùng bật thì thư viện không có lớp nổi; ô lưới tắt thì lớp nổi như thường | MC-13: `PosesRoute` bọc gốc màn bằng `InContentAds(listOf(AdPlacement.POSES_IN_COLLECTION))`, bên ngoài `BottomAdBarScaffold` |
| settings KB-1 | Thanh đáy ở đáy cài đặt; danh sách cuộn phía trên; lớp nổi khi bật thì thu gọn được | Như poses; cài đặt giữ lớp nổi |
| camera KB-9 | Thanh đáy ở đáy màn xem lại, dưới hàng nút; ảnh co lại cho vừa, giữ tỷ lệ; bốn nút không bị che | Khe thấp hơn nên ảnh lớn hơn: ở trạng thái thường ảnh giữ cỡ khi chưa có thanh (giới hạn theo bề rộng), chỉ co ở trạng thái có báo lỗi |
| camera KB-11 (máy gập, màn ngang) | Nút chính trong cửa sổ, trên thanh | Khung gập mở: khe 641 x 106; ảnh 450 x 600 (giữ cỡ); màn ngang 960 x 520: khe 106 thay 169 |
| spec ads câu 22, ý c, d, f | Khe nhường chỗ lớp thu gọn; một quảng cáo neo đáy mỗi màn; hàm biết trước hành động có thưởng | Code và test của khung; app không gọi hàm của ý f |

Thiết kế tham chiếu: `features/poses/design/README.md`, `features/settings/design/README.md`, `features/camera/design/README.md` (đoạn "Vẽ lại ngày 2026-11-21", Nhật duyệt). Ảnh trong `design/screens/`: `PosesScreen_ad_light`, `PosesScreen_ad_dark`, `PoseDetailScreen_ad_light`, `PoseDetailScreen_ad_dark`, `SettingsScreen_ad_light`, `SettingsScreen_ad_dark`, `ReviewScreen_ad_dark`, `ReviewScreen_ad_dark_unfolded`.

ADR liên quan: ADR-0045 (khung quảng cáo), ADR-0030 (đồng bộ dự án mẫu), ADR-0029 (phần ngoài mục 3.2), ADR-0031 (viết lại câu khẳng định), ADR-0020 (thiết kế, screenshot), ADR-0001 (phụ thuộc module).

## 3. Giải pháp

### 3.1. Cách làm

**Điểm bắt đầu: nhánh `template-sync/take-ads-7bf8c3b`, commit `46e13a14`** (đã đẩy, `docs/ops/to-app.md`, mục 5). Nhánh có năm commit:

- `dd959e12` `core/ads` từ `bc3e80e` lên `7bf8c3b`: 74 file, có 7 xung đột đã gộp tay:
  - `AdSlot.kt`: giữ tham số `separator` của app (dải phân cách của banner, T-023) bên cạnh `inBottomBar` của mẫu. Chiều cao khe dùng `slotHeightDp` của mẫu; khi có `separator` thì nội dung đặt chiều cao, như trước.
  - `NativeAdSlot.kt`, nhánh `Unavailable` khi giữ chỗ: `if (size == NativeAdSize.CARD && cardHeight != null) cardHeight else nativeAdHeight(size, ctaLarge)`. Giữ chiều cao ô lưới của app (T-080), phần còn lại theo hàm mới của mẫu.
  - `BottomAdBar.kt`, `AdUnitIds.kt` và ba test (`AdPlacementTest`, `AdUnitIdsTest`, `BottomAdBarTest`): chỉ khác chú thích dẫn plan T-094, lấy bản mẫu (`collapsibleLayer` đã về mẫu). `BottomAdBar.kt` sau gộp giống hệt bản mẫu.
- `748d1bb1` `docs/ops/ads-guide.md`: ba xung đột, lấy bản mẫu, giữ các câu của app (`poses_in_collection`, `native_onboarding`, `PosesScreenScreenshotTest`, pool bước của `native_onboarding`).
- `59858efe` `docs/ops/ads-business-rules.md`: không xung đột.
- `d679393d` `docs/template-sync.txt`: bỏ `hold=` của ba đường dẫn, ghi hai phần riêng của app trong `core/ads`.
- `46e13a14`: 10 test mới của mẫu đổi `SAMPLE_NATIVE_SMALL` thành `POSES_BOTTOM`, kể cả mã trong câu so dòng log (`BottomAdBarTest`, `AdsControllerNativeStockTest`), theo quy ước ở `docs/template-sync.txt`. Bảng khóa Remote Config sinh lại: chỉ đổi mã ADR trong mô tả `is_enable_consent_shortcut`, không có khóa mới.

Code của mẫu nằm trên nhánh của app nên phiên đọc được. Plan không dán diff khoảng 5 nghìn dòng (câu hỏi 2).

Nhánh task: `claude/T-102-lay-core-ads` tạo từ commit `46e13a14` (`git switch -c claude/T-102-lay-core-ads 46e13a14`, không từ tên nhánh, để commit đẩy thêm lên nhánh take sau khi duyệt không vào task), rồi `git merge origin/main`. Có xung đột ngoài `docs/template-sync.txt`, `docs/foundation.md` và hai bảng khóa Remote Config thì dừng (mục 7).

Các việc của phiên trên nhánh đó:

1. **Test đo của feature theo số mới** (mục 3.5; số đo thật ở mục 4.1). Mỗi câu khẳng định đổi theo cột cuối của mục 3.5.
   - Đổi tên hai test vì ảnh không còn co ở trạng thái thường:
     - `ReviewScreenTest.adBar_slotAtBottom_photoShrinks_saveMovesUp` thành `adBar_slotAtBottom_photoKept_saveMovesUp`;
     - `ReviewScreenUnfoldedTest.adBar_slotFullWidth_photoShrinks_saveAbove` thành `adBar_slotFullWidth_photoKept_saveAbove`.
   - Hằng `SLOT_HEIGHT = 126f` của `PosesScreenTest` thành `106f`.
   - Chú thích ghi số cũ trong các file test ở mục 3.2 đổi theo số mới, để chú thích không trái câu khẳng định:
     - mọi chú thích ghi 126, 142, 158, 169, 200, 216 của khe hay khoảng chừa;
     - `PoseDetailScreenTest.kt`, chú thích trước "slot starts where the bar ends": thanh nút kết thúc ở y 769 (673 + 96), thay 749 (653 + 96);
     - `ReviewScreenUnfoldedTest.kt`, chú thích của `landscape_adBar_saveInsideWindow`: cột phải cao 314 (khe ở y 398), từ ngưỡng 264 trở lên nên hàng nút phụ xếp dọc, thay "251 high … side by side";
     - `ReviewScreenUnfoldedTest.kt`, KDoc của `assertCompactColumn`: thông báo lỗi từ `errorTop` tới 306, thay 243;
     - `ReviewScreenUnfoldedTest.kt`, chú thích của `landscape_adBar_thresholdBoundary`: cột cao 387 khi có khe (từ 64 tới đáy nút Lưu 451), thay 296;
     - "Numbers from section 3.5 of plan T-098" ở `ReviewScreenTest.kt` và `ReviewScreenUnfoldedTest.kt` thành "plan T-102".
2. **Ý g:** `AdPlacement.POSE_DETAIL_BOTTOM` khai báo `collapsibleLayer = false`, như `REVIEW_BOTTOM`. KDoc của dòng dẫn spec ads, câu 22, ý g. Test `AdPlacementAppTest` viết lại (mục 3.5).
3. **Ý e:** `PosesRoute` bọc phần thân bằng `InContentAds(listOf(AdPlacement.POSES_IN_COLLECTION)) { BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) { ... } }`. `InContentAds` đặt ở gốc của đích, bên ngoài thanh đáy, theo KDoc của nó. `rememberInListAds` giữ nguyên chỗ cũ. Test quét mã mới `PosesInContentAdsTest` (mục 4, câu hỏi 1). Danh sách của `InContentAds` không có `POSES_BOTTOM` (KDoc của nó cấm liệt kê native nhỏ của chính thanh đáy).
4. **Ảnh chuẩn:** ghi lại đúng 12 ảnh ở mục 3.2 (lệnh ở mục 4). Ảnh khác không đổi: lần chạy `check.sh` trên nhánh gộp thấy chỉ 12 ảnh này đỏ (mục 4.1).
5. **`design-compare.sh`** cho poses, settings, camera; commit `design/review/` của ba feature.
6. **`docs/ops/manual-test.md`, mục 11.1:**
   - Bước 1, 9, 10: "khoảng 126 dp (khoảng 106 dp trên máy rộng 360 dp)" thành "khoảng 106 dp (cao hơn khi đặt chữ lớn)".
   - Bước 10, dòng "thư viện có lớp nổi, màn xem lại không có": đổi thành "Cài đặt có lớp nổi; thư viện không có (ô quảng cáo trong lưới đang bật, log `Floating ads off on this screen: in-content ad on (poses_in_collection)`); chi tiết và màn xem lại không có (log `Bottom bar pose_detail_bottom: slot=NATIVE collapsible=false`, `Bottom bar review_bottom: slot=NATIVE collapsible=false`)".
   - Hai dòng máy gập (bước 10) làm ở màn Cài đặt thay thư viện. Chuỗi log `Collapsible poses_bottom not shown` thành `Collapsible settings_bottom not shown`. Lớp vẫn cao khoảng 400 dp ở khung gập đứng dọc (tổng hai phần 400 dp từ cạnh ngắn 600 dp trở lên, `ads-business-rules.md` mục 5.7).
   - Thêm một dòng mới cuối bước 10: "Đặt `is_show_poses_in_collection = false` cùng `is_show_native_collap = true` trên Firebase staging, đóng hẳn app rồi mở lại, vào thư viện: không có ô quảng cáo trong lưới, có lớp nổi; log không có dòng `Floating ads off on this screen`. Trả hai khóa về như cũ."
   - Nguồn của các dòng log mà bước tay chờ (đối chiếu khi viết bước): `Floating ads off on this screen: in-content ad on (poses_in_collection)` do `inContentBlockLog` của `InContentAds.kt` ghi, một lần mỗi lần vào màn, khi `is_show_poses_in_collection` bật (không cần có quảng cáo hay đồng ý); `Bottom bar <mã>: slot=NATIVE collapsible=false` do `BottomAdBar.kt` ghi khi thanh đáy dựng lớp, với vị trí khai báo `collapsibleLayer = false`; `Collapsible settings_bottom not shown: window ... dp has no room` do `CollapsibleNativeAd.kt` ghi khi cửa sổ không đủ chỗ (CL-05). Vì khóa lấy lúc mở app, mỗi bước đổi khóa có "đóng hẳn app rồi mở lại".
7. **`docs/template-sync.txt`:** nhánh đã sửa; phiên chỉ kiểm lại sau khi gộp `origin/main`.
8. `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`: không sửa thêm (đã gộp trên nhánh).

Mẫu làm theo: T-098 (lấy `core/ads` qua nhánh take, test đo thanh đáy, ảnh chuẩn `_ad`), T-094 (`collapsibleLayer = false` của `review_bottom`), `LeaveAppCallSitesTest` của `:app` (test quét mã theo ký hiệu).

### 3.2. Danh sách file

Dòng thư mục (kết thúc bằng `/`) là phần lấy nguyên từ nhánh take; file app sửa thêm trong đó có dòng riêng.

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/` | Lấy nguyên từ nhánh take (74 file đổi từ `bc3e80e`, gồm ảnh chuẩn của `core/ads`, 10 test đổi tên vị trí) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | `POSE_DETAIL_BOTTOM(..., collapsibleLayer = false)`, KDoc dẫn câu 22, ý g |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementAppTest.kt` | Câu khẳng định của `reviewBottom_hasNoCollapsibleLayer` viết lại, đổi tên (mục 3.5); `reviewBottom_usesNoCollapsibleStock` thêm `assertFalse(AdPlacement.POSE_DETAIL_BOTTOM.usesStock(AdFormat.NATIVE_COLLAPSIBLE))` |
| Sửa | `docs/ops/ads-guide.md` | Lấy nguyên từ nhánh take |
| Sửa | `docs/ops/ads-business-rules.md` | Lấy nguyên từ nhánh take |
| Sửa | `docs/template-sync.txt` | Nhánh take đã sửa; phiên kiểm lại sau khi gộp |
| Sửa | `docs/remote-config-keys.md` | Nhánh take đã sinh lại (chỉ mã ADR) |
| Sửa | `docs/remote-config-template.json` | Nhánh take đã sinh lại (chỉ mã ADR) |
| Sửa | `docs/ops/manual-test.md` | Mục 11.1 (mục 3.1, việc 6) |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreen.kt` | `PosesRoute` bọc bằng `InContentAds(listOf(AdPlacement.POSES_IN_COLLECTION))` (mục 3.1, việc 3) |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesInContentAdsTest.kt` | Test quét mã: `PosesRoute` gọi `InContentAds` với `AdPlacement.POSES_IN_COLLECTION`, và `BottomAdBarScaffold(AdPlacement.POSES_BOTTOM)` nằm trong khối đó |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenTest.kt` | `SLOT_HEIGHT = 106f`, câu khẳng định mới (mục 3.5) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenTest.kt` | Câu khẳng định mới |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenTest.kt` | Câu khẳng định mới; đổi tên `adBar_slotAtBottom_photoShrinks_saveMovesUp` thành `adBar_slotAtBottom_photoKept_saveMovesUp` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenUnfoldedTest.kt` | Câu khẳng định mới; đổi tên `adBar_slotFullWidth_photoShrinks_saveAbove` thành `adBar_slotFullWidth_photoKept_saveAbove` |
| Sửa | `feature/poses/src/test/screenshots/PosesScreen_ad_light.png` | Ghi lại: khe 106 dp, thẻ hai tầng |
| Sửa | `feature/poses/src/test/screenshots/PosesScreen_ad_dark.png` | Ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PosesScreen_ad_small.png` | Ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_light.png` | Ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_dark.png` | Ghi lại |
| Sửa | `feature/poses/src/test/screenshots/PoseDetailScreen_ad_small.png` | Ghi lại |
| Sửa | `feature/camera/src/test/screenshots/ReviewScreen_ad_dark.png` | Ghi lại: ảnh lớn hơn |
| Sửa | `feature/camera/src/test/screenshots/ReviewScreen_ad_dark_unfolded.png` | Ghi lại: khe 641 x 106 dp |
| Sửa | `feature/camera/src/test/screenshots/ReviewScreen_ad_small.png` | Ghi lại |
| Sửa | `feature/settings/src/test/screenshots/SettingsScreen_ad_light.png` | Ghi lại |
| Sửa | `feature/settings/src/test/screenshots/SettingsScreen_ad_dark.png` | Ghi lại |
| Sửa | `feature/settings/src/test/screenshots/SettingsScreen_ad_small.png` | Ghi lại |
| Sửa | `docs/features/poses/design/review/` | Sinh lại bằng `scripts/design-compare.sh poses` |
| Sửa | `docs/features/camera/design/review/` | Sinh lại bằng `scripts/design-compare.sh camera` |
| Sửa | `docs/features/settings/design/review/` | Sinh lại bằng `scripts/design-compare.sh settings` |
| Sửa | `docs/foundation.md` | Chỉ khi `scripts/foundation.py` đổi sau khi gộp `origin/main` (L6) |

Test của `feature/settings` qua trên nhánh take, trừ ba ảnh chuẩn: khe giả của nó đã dùng `BottomAdBarDefaults.nativeSlotHeight()`. Test của `:app`, `feature/onboarding`, `feature/loading`, `core/network` qua trên nhánh take (mục 4.1).

Nơi đọc các file mà plan sửa (`grep -rn` trong `scripts/`, `.github/`, `*/src/test`), không bị ảnh hưởng:

- `AdPlacement.kt`:
  - `scripts/release.sh` đọc khóa vị trí theo luật một dòng (khóa `adUnits` của production); `collapsibleLayer` không đổi khóa.
  - `scripts/status.py` và `scripts/release.sh` đọc khai báo theo luật một dòng (gồm `collapsibleLayer = false`): khai báo của `POSE_DETAIL_BOTTOM` giữ trên một dòng, không có chú thích cuối dòng (như KDoc của `AdPlacement.kt` dặn).
  - `scripts/ads-plan.py` đọc khai báo vị trí để sinh Excel: dòng `pose_detail_bottom` có thêm ghi chú `collapsibleLayer = false`. Đó là đúng hành vi mới, file Excel không ở trong repo.
  - Test của `core/ads` đọc `collapsibleLayer`: `AdPlacementTest` (luật chung), `AdUnitIdsTest`, `BottomAdBarTest` (dùng `REVIEW_BOTTOM` hay vị trí riêng của test). Chỉ `AdPlacementAppTest` khẳng định giá trị của `POSE_DETAIL_BOTTOM` (mục 3.5).
  - `RemoteConfigKeysTest` của `:app`: khóa `is_show_native_collap` vẫn được đọc vì ba thanh đáy khác còn lớp.
- `PosesScreen.kt`: chỉ các test của `feature/poses` đọc; `PosesScreen` (composable không trạng thái) không đổi chữ ký, `InContentAds` chỉ ở `PosesRoute`.
- `docs/ops/manual-test.md`: `scripts/status.py` không đọc; `scripts/device-scenario.py` không đọc.
- `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`: các test sau chỉ nhắc tài liệu trong KDoc dẫn mục, không đọc file; phần gộp không đổi số mục mà chúng dẫn (8.1, 8.2, 8.3, 10.1, S, 5.7):
  - `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdFormatTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdNetworkResponsesTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsSettingsTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdTelemetryRulesTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/ConsentGateTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/FullScreenAdGuardTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/LoadAttemptCounterTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdCardScreenshotTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdCardTest.kt`
  - `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AppsFlyerEventsTest.kt`
  - `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AppsFlyerSettingsTest.kt`
  - `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AppsFlyerSwitchesTest.kt`
  - `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AppsFlyerTrackerTest.kt`
  - `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/FirebaseAdMetricsTest.kt`
- `docs/remote-config-keys.md`, `docs/remote-config-template.json`: `app/build.gradle.kts` đưa hai file vào task test, `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` so chúng với code (qua trên nhánh take); `scripts/main-guard-verified.sh` coi chúng là tài liệu Gradle đọc (`GRADLE_READ_DOCS`), nên `main-guard` kiểm commit merge như code, đúng ý; `.github/workflows/pr-check.yml` chạy cùng các test đó.
- `docs/template-sync.txt`: `scripts/setup-check-app.sh` chỉ nhắc tên file trong một thông báo; `scripts/tests/run.sh` dựng file riêng trong repo giả.
- `AdPlacement.kt`, `docs/foundation.md`: `scripts/tests/run.sh` đọc bản mẫu `scripts/tests/fixtures/release/AdPlacement.kt` và dựng repo giả riêng, không đọc file của app; `docs/foundation.md` chỉ sinh lại khi gộp `origin/main` làm nó đổi.
- `docs/template-sync.txt`, hai bảng khóa, `docs/foundation.md`: như plan T-098 (test của script dựng repo giả; `RemoteConfigKeysTest` so bảng với code).

Nơi đọc ảnh chuẩn đổi: `scripts/design-compare.sh` (đọc `src/test/screenshots/` của ba feature, sinh `design/review/`, có trong bảng); `scripts/check.sh` (chỉ kiểm đường dẫn `captureRoboImage`).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo:
  - Không thêm vị trí.
  - Đổi khai báo vị trí có sẵn `pose_detail_bottom` (`collapsibleLayer = false`, spec ads câu 22, ý g).
  - Thư viện khai báo vị trí trong nội dung `poses_in_collection` cho MC-13 (câu 22, ý e).
  - Kích thước và luật thanh đáy theo khung (câu 22, ý a tới d).
- Sự kiện phân tích: Không chạm. Ở chi tiết, lớp thu gọn không còn ghi số liệu vì không bao giờ có lớp. Đó là hệ quả của ý g, không phải sự kiện mới.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `MergedManifestTest` qua trên nhánh take.
- **Thư viện và SDK:** Không thêm. Code mới của mẫu chỉ dùng thư viện đã có trong `gradle/libs.versions.toml` (Google Mobile Ads, AppLovin MAX, Compose); `core/ads/build.gradle.kts` không đổi giữa hai mốc.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** test quét mã `PosesInContentAdsTest` phải tìm được lời gọi theo ký hiệu (import của `InContentAds` và `AdPlacement.POSES_IN_COLLECTION` trong đối số), mà không khớp nhầm chú thích hay KDoc.
- **Điều kiện chuyển:** test vẫn qua khi tạm bỏ lời gọi `InContentAds` (bước đối chứng của mục 4), sau 2 lần sửa.
- **Cách làm thay:** tách phần bọc thành một composable `internal` trong chính `PosesScreen.kt` (không thêm file), ví dụ `PosesAdsFrame(content)` gọi `InContentAds(listOf(AdPlacement.POSES_IN_COLLECTION)) { BottomAdBarScaffold(AdPlacement.POSES_BOTTOM, content) }`. Test quét mã chỉ kiểm thân hàm đó, và `PosesRoute` gọi nó. Tiêu chí và bước đối chứng giữ nguyên.

### 3.5. Hành vi đã có bị đổi

Số mới là số đo thật trên nhánh take (mục 4.1). Câu khẳng định nào không có trong bảng thì giữ nguyên: lần đo (gồm lượt 3b cho test khung gập và lượt 3c cho test điện thoại, chạy hết mọi câu của hai test bị dừng giữa chừng) thấy giá trị không đổi.

Ý định của hai test đổi tên: có thanh đáy thì nút Lưu dời lên đúng bằng chỗ khe chiếm, ảnh không bị cắt hay đè, và với khe 106 dp ảnh không phải co (đã bị giới hạn theo bề rộng). Đường "ảnh co lại cho vừa" của KB-9 vẫn có test: `ReviewScreenTest.small_adBar_photoShrinks_allVisible` (khung 360 x 640, khe ở đó vốn đã 106 dp, test không đổi) và `adBar_permissionBlocked_photoShrinks_allAbove`. Khung gập đứng dọc không còn test "ảnh co": ở đó ảnh luôn giữ cỡ khi có thanh.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Khe đáy 126 dp ở khung chuẩn | 106 dp ở cỡ chữ mặc định ở mọi khung (D-02) | `PosesScreenTest.bottomBar_lastCardScrollsAboveSlot` (qua `SLOT_HEIGHT`), `PoseDetailScreenTest.bottomBar_slotBelowButtonBar_buttonNotCovered` ("slot height" 126) | `SLOT_HEIGHT = 106f`; "slot height" 106, sai số 0.5; các câu "slot 16 above the bottom", "last card above slot", "slot starts where the bar ends" giữ |
| Có khe thì thẻ cuối của thư viện cách đáy 158 dp | 138 dp (khe 106 + lề 16 + lề lưới 16) | `PosesScreenTest.realBottomBar_withoutAds_takesNoSpace` ("control: with a slot" 158) | "control: with a slot" 138, sai số 0.5; phần không có quảng cáo (16) giữ |
| Có khe thì thanh nút của chi tiết cách đáy 142 dp | 122 dp | `PoseDetailScreenTest.realBottomBar_withoutAds_takesNoSpace` ("control: with a slot" 142) | "control: with a slot" 122, sai số 0.5 |
| Màn xem lại 411 x 891: khe ở y 749, nút Lưu 673 tới 729, ảnh co về 372.75 x 497, nút dời lên 142 | Khe ở y 769, nút Lưu 693 tới 749, ảnh giữ cỡ khi không có thanh (379 x 505.33, giới hạn theo bề rộng), nút dời lên 122 | `ReviewScreenTest.adBar_slotAtBottom_photoShrinks_saveMovesUp` | Đổi tên `adBar_slotAtBottom_photoKept_saveMovesUp`. "slot height" 106; "slot top" 769; "save top" 693; "save bottom" 749; "save moved up" 122; "photo width" 379; "photo height" 505.33 (sai số 1); "actions top" 593.33 (sai số 1; ảnh cao hơn nên hàng nút phụ ở 64 + 505.33 + 24). Câu `assertTrue` "photo height < before" thành "photo height giữ như trước": `assertEquals(photoBefore.height.value, photo.height.value, 0.5f)`. "photo top" 64, "photo top kept", "slot to the bottom" 16, "save to the slot" 20 giữ |
| Trạng thái lưu, lỗi: khe ở y 749, ảnh 497 / 324.75 x 433 | Khe ở y 769; SAVED: nút Lưu 693, ảnh cao 505.33; FAILED_STORAGE_FULL: ảnh 339.67 x 453 | `ReviewScreenTest.adBar_savingAndSaveError_slotStays` (và `assertSlotStays`) | "slot top" của mọi trạng thái 769; "SAVED save top" 693; "SAVED photo height" 505.33; "FAILED_STORAGE_FULL photo width" 339.67, "photo height" 453 (sai số 1); "error height" 48 giữ |
| PERMISSION_BLOCKED: ảnh co về 309.67 x 412.9 | Ảnh 324.67 x 433 | `ReviewScreenTest.adBar_permissionBlocked_photoShrinks_allAbove` | "photo width" 324.67, "photo height" 433 (sai số 1); tên giữ (ảnh vẫn co so với khi không lỗi); "error height" 68 giữ |
| Không có thanh: nút Lưu cách đáy 20; có thanh: 162 | Có thanh: 142 | `ReviewScreenTest.realAdBar_withoutAds_takesNoSpace` ("control: save to the bottom" 162) | "control: save to the bottom" 142, sai số 0.5; phần không có thanh (20) giữ |
| Khung gập mở 673 x 841: khe ở y 625, ảnh co về 405.75 x 541, nút Lưu đáy ở 605, dời lên 216 | Khe ở y 719, ảnh giữ cỡ 450 x 600, nút Lưu đáy ở 699, dời lên 122 | `ReviewScreenUnfoldedTest.adBar_slotFullWidth_photoShrinks_saveAbove` | Đổi tên `adBar_slotFullWidth_photoKept_saveAbove`. "slot top" 719; "photo width" 450; "photo height" 600 (sai số 1). `assertTrue` "photo height < before" thành `assertEquals(photoBefore.height.value, photo.height.value, 0.5f)`. "save bottom" 699; "save moved up" 122 (sai số 0.5). "slot width" 641, "slot left" 16, tỷ lệ 0.75, "save right of the photo" giữ |
| Màn ngang 960 x 520: khe ở y 335, nút Lưu 259 tới 315, ảnh 188.25 x 251, hàng nút phụ ngang (đáy 136) | Khe ở y 398, nút Lưu 322 tới 378, ảnh 235.67 x 314, hàng nút phụ xếp dọc (đáy 256) | `ReviewScreenUnfoldedTest.landscape_adBar_saveInsideWindow` | "slot top" 398; "save top" 322; "save bottom" 378; "photo width" 235.67, "photo height" 314 (sai số 1); "actions bottom" 256; "actions top" 64 giữ |
| Màn ngang, lỗi lưu: khe ở y 335, nút Lưu 259 tới 315, thông báo lỗi 195 tới 243 | Khe 398, nút Lưu 322 tới 378, thông báo lỗi 258 tới 306 | `ReviewScreenUnfoldedTest.landscape_adBar_saveError_compactColumn` (`assertCompactColumn` cho PERMISSION_BLOCKED, FAILED_STORAGE_FULL) | Đối số "slot top" 398, "save top" 322, "save bottom" 378, `errorTop = 258f`, "error bottom" 306; bề rộng nút Lưu 137 giữ |
| Như trên, tiếng Bồ Đào Nha: lỗi 175 tới 243 | Lỗi 238 tới 306, khe 398, nút Lưu 322 tới 378 | `ReviewScreenUnfoldedTest.landscape_adBar_saveError_compactColumn_portuguese` | `errorTop = 238f`, "error bottom" 306, "slot top" 398, "save top" 322, "save bottom" 378; bề rộng 160.7 giữ |
| Ngưỡng xếp dọc: nút Lưu 304 tới 360 | 395 tới 451 | `ReviewScreenUnfoldedTest.landscape_adBar_thresholdBoundary` (`assertSaveAt`) | `assertSaveAt("PERMISSION_BLOCKED", 395f, 451f)`, `assertSaveAt("NOT_SAVED", 395f, 451f)`; các câu về hàng nút phụ giữ |
| `pose_detail_bottom` có lớp thu gọn (D-01) | Không bao giờ có lớp (câu 22, ý g) | `AdPlacementAppTest.reviewBottom_hasNoCollapsibleLayer` (`assertTrue(POSE_DETAIL_BOTTOM.declaration.collapsibleLayer)`) | Đổi tên `bottomBarsWithoutLayer_reviewAndPoseDetail`. Câu mới: `assertFalse(AdPlacement.REVIEW_BOTTOM.declaration.collapsibleLayer)`, `assertFalse(AdPlacement.POSE_DETAIL_BOTTOM.declaration.collapsibleLayer)`; đối chứng `assertTrue` cho `POSES_BOTTOM`, `SETTINGS_BOTTOM` giữ |
| Thư viện có lớp thu gọn khi `is_show_native_collap` bật | Không có khi `is_show_poses_in_collection` bật (MC-13) | Không có test feature nào khẳng định (lớp do khung vẽ) | Test mới `PosesInContentAdsTest` (mục 4); hành vi của khung: `InContentAdsTest`, `InContentAdsWiringTest` của `core/ads` |

`scripts/existing-tests.py` (2026-11-22): 0 test ở nhóm "Cần xem trước", 2 test ở nhóm "Nên xem", cả hai giữ nguyên:
- `PosesScreenTest.scrollPosition_survivesStateRestore` dựng `PosesRoute`: không có `AdsController` thì `InContentAds` chỉ vẽ phần nội dung (KDoc của nó), không thêm node bố cục.
- `PosesScreenScreenshotTest.default_dark_unfolded`: dựng `PosesScreen`, không qua `PosesRoute`, không có quảng cáo.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| poses KB-6 tiêu chí 1 | Compose UI test | `PosesScreenTest.bottomBar_lastCardScrollsAboveSlot`, `realBottomBar_withoutAds_takesNoSpace` (câu mới ở mục 3.5) |
| poses KB-6 tiêu chí 2, chi tiết không có lớp nổi | Compose UI test, unit test | `PoseDetailScreenTest.bottomBar_slotBelowButtonBar_buttonNotCovered`, `realBottomBar_withoutAds_takesNoSpace`; `AdPlacementAppTest.bottomBarsWithoutLayer_reviewAndPoseDetail` (thất bại trước code: câu `assertFalse` cho `POSE_DETAIL_BOTTOM`) |
| poses KB-6 tiêu chí 3: chừa chỗ khi đang tải, chừa 0 khi tắt hay lỗi trước quảng cáo đầu | Unit test của khung, Compose UI test | `BottomAdBarTest`, `BannerFrameTest`, `BottomBarSlotWiringTest` của `core/ads`; `PosesScreenTest.realBottomBar_withoutAds_takesNoSpace` (khóa tắt) |
| poses KB-6, thư viện không có lớp nổi khi ô lưới bật | Unit test quét mã, unit test của khung | `PosesInContentAdsTest` đọc `PosesScreen.kt`, bỏ chú thích, KDoc và chuỗi ký tự trước khi tìm (như `LeaveAppCallSitesTest` của `:app`). (1) `posesRoute_wrapsBottomBarInInContentAds`: file import `com.vglstudio.camera.pose.line.guide.core.ads.InContentAds`; trong thân `fun PosesRoute` có lời gọi `InContentAds(` mà đối số chứa `AdPlacement.POSES_IN_COLLECTION` và không chứa `POSES_BOTTOM`; `BottomAdBarScaffold(AdPlacement.POSES_BOTTOM` nằm trong khối lambda của lời gọi đó. (2) `scanner_findsTheRealCall_butNotInCommentsOrStrings`: văn bản mẫu có lời gọi trong chú thích dòng, KDoc và chuỗi ký tự thì không khớp, lời gọi thật trong thân hàm thì khớp, lời gọi trong một hàm khác `PosesRoute` thì không khớp. Đối chứng: tạm bỏ lời gọi thì (1) thất bại. Hành vi: `InContentAdsTest`, `InContentAdsWiringTest` |
| poses KB-6 tiêu chí 4: `is_show_native_collap` tắt thì không có lớp nổi | Unit test của khung (giữ) | `BottomAdBarTest.layers_collapOff_keepsNativeSlot_noLayer` |
| poses KB-6 tiêu chí 6, 7: ô quảng cáo trong lưới, khóa tắt thì không có ô | Compose UI test, unit test (giữ, test có sẵn, qua trên nhánh take) | `PosesScreenTest.fixedGridAds_adCellAfterEighthPose`, `fixedGridAds_savedAndGroupFilters_haveCell`, `fixedGridAds_unfolded_threeColumns`, `noGridAds_noAdCell`, `posesScreen_liveGridAdsWithoutController_noAdCell`; `InListAdsTest` của `core/ads` |
| settings KB-1 | Compose UI test | `SettingsScreenTest` (qua trên nhánh take, giữ) |
| camera KB-9 | Compose UI test | `ReviewScreenTest.adBar_slotAtBottom_photoKept_saveMovesUp`, `adBar_savingAndSaveError_slotStays`, `realAdBar_withoutAds_takesNoSpace`, `adBar_permissionBlocked_photoShrinks_allAbove`; `small_adBar_photoShrinks_allVisible`, `smallPortuguese_adBar_permissionBlocked_allVisible` (qua, giữ) |
| camera KB-11 (máy gập, màn ngang) | Compose UI test | `ReviewScreenUnfoldedTest.adBar_slotFullWidth_photoKept_saveAbove`, `landscape_adBar_saveInsideWindow`, `landscape_adBar_saveError_compactColumn`, `landscape_adBar_saveError_compactColumn_portuguese`, `landscape_adBar_thresholdBoundary` |
| Câu 22 ý a: thẻ hai tầng 106 dp, medium 250 dp, lớp 299 dp | Unit test, screenshot test của khung | `NativeAdDimensTest`, `NativeAdFontScaleTest`, `NativeAdCardTest`, `BottomAdBarTest`, ảnh `NativeAdCard_small_light`, `BottomAdBar_expanded_light` của `core/ads` |
| Câu 22 ý c: khe nhường chỗ lớp thu gọn | Unit test của khung | `BottomSlotYieldWiringTest`, `AdsControllerNativeStockTest` |
| Câu 22 ý d: một quảng cáo neo đáy mỗi màn | Unit test của khung | `AnchoredAdTurnTest`, `AnchoredAdWiringTest` |
| Câu 22 ý f: hàm biết trước hành động có thưởng | Unit test của khung | `RewardedAdRequiredTest` |
| Phần riêng của app trong `core/ads` còn đủ | Unit test | `AdSlotTest` (dải phân cách), `InListAdsTest` (năm test T-080, chiều cao ô), `NativeAdSlotTest`, `AdPlacementAppTest`, `AdUnitIdsTest` |
| Giao diện thư viện | Screenshot test | `PosesScreen_ad_light.png`, `PosesScreen_ad_dark.png` |
| Giao diện chi tiết | Screenshot test | `PoseDetailScreen_ad_light.png`, `PoseDetailScreen_ad_dark.png` |
| Giao diện cài đặt | Screenshot test | `SettingsScreen_ad_light.png`, `SettingsScreen_ad_dark.png` |
| Giao diện màn xem lại | Screenshot test | `ReviewScreen_ad_dark.png`, `ReviewScreen_ad_dark_unfolded.png` |
| Màn hình thấp | Screenshot test khung `w360dp-h640dp-xxhdpi` | `PosesScreen_ad_small.png`, `PoseDetailScreen_ad_small.png`, `SettingsScreen_ad_small.png`, `ReviewScreen_ad_small.png` |
| Đúng thiết kế | Screenshot test cùng tên với ảnh thiết kế, `scripts/design-compare.sh poses`, `camera`, `settings`, rà soát độc lập theo mục 7 của mẫu thiết kế | `PosesScreen_ad_light`, `PosesScreen_ad_dark`, `PoseDetailScreen_ad_light`, `PoseDetailScreen_ad_dark`, `SettingsScreen_ad_light`, `SettingsScreen_ad_dark`, `ReviewScreen_ad_dark`, `ReviewScreen_ad_dark_unfolded` |
| Các ảnh khác không đổi | Screenshot test | `verifyRoborazziDebug` của `core/ads`, `feature/poses`, `feature/camera`, `feature/settings`, `feature/onboarding` qua; `git status --short -- '*/src/test/screenshots/*'` chỉ có 12 ảnh của mục 3.2 |
| Khe và thẻ trên bản staging (thư viện, chi tiết, cài đặt, màn xem lại) | Kiểm tra thủ công (máy thật: quảng cáo thật) | `docs/ops/manual-test.md`, mục 11.1, bước 1, 2, 9, 10: khe khoảng 106 dp, thẻ hai tầng, nội dung không bị che |
| Lớp nổi chỉ ở Cài đặt; thư viện và chi tiết không có (staging bật ô lưới) | Kiểm tra thủ công (máy thật: Remote Config) | `manual-test.md`, mục 11.1, bước 10 mới: `is_show_native_collap = true`, lớp nổi ở Cài đặt; log `Floating ads off on this screen: in-content ad on (poses_in_collection)` ở thư viện, `Bottom bar pose_detail_bottom: slot=NATIVE collapsible=false` ở chi tiết |
| Lớp thu gọn trên máy gập | Kiểm tra thủ công (máy thật: gập có bản lề) | `manual-test.md`, mục 11.1, hai dòng máy gập của bước 10, làm ở Cài đặt |
| Thư viện có lớp nổi khi tắt ô lưới | Kiểm tra thủ công (máy thật: Remote Config) | `manual-test.md`, mục 11.1, dòng mới của bước 10: thêm `is_show_poses_in_collection = false` cùng `is_show_native_collap = true`, đóng hẳn app rồi mở lại: thư viện có lớp nổi, không có dòng `Floating ads off`; trả hai khóa về như cũ |

Ghi lại ảnh chuẩn (chỉ 12 ảnh của mục 3.2): `./gradlew :feature:poses:recordRoborazziDebug :feature:poses:testDebugUnitTest :feature:camera:recordRoborazziDebug :feature:camera:testDebugUnitTest :feature:settings:recordRoborazziDebug :feature:settings:testDebugUnitTest --rerun`, rồi `git status --short -- '*/src/test/screenshots/*'`; ảnh khác đổi vì nhiễu vẽ thì `git checkout -- <ảnh>`.

So với thiết kế: frame thiết kế vẽ thanh trạng thái 24 dp (8 dp ở khung gập), screenshot test không có. Vì vậy ở `ReviewScreen_ad_dark`, ảnh trong screenshot (379 x 505.33) lớn hơn frame (370 x 493); khe, thẻ và nút chính khớp theo mép dưới. Ở `ReviewScreen_ad_dark_unfolded`, frame và test cùng có ảnh 450 x 600. README thiết kế camera đã ghi hai điều này.

**Ngoại lệ viết test trước:**
- Phần lấy nguyên từ nhánh take (`core/ads/`, hai tài liệu ads, bảng khóa): test của mẫu đi cùng code trong cùng commit của nhánh. Commit test của bước 3 gồm thay đổi test của feature và `AdPlacementAppTest`, chạy trên nhánh take (code mới đã có), nên test đo của feature không "thất bại trước code". Phiên xác nhận bằng đối chứng: tạm đổi `SLOT_HEIGHT` của `PosesScreenTest` về `126f` thì `bottomBar_lastCardScrollsAboveSlot` thất bại, rồi trả lại (`git checkout -- <tệp>`). Hai test thất bại trước code thật: `AdPlacementAppTest.bottomBarsWithoutLayer_reviewAndPoseDetail` (trước việc 2) và `PosesInContentAdsTest` (trước việc 3).
- `docs/ops/manual-test.md`, `docs/template-sync.txt`: chỉ là tài liệu.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-11-21 tới 2026-11-22, trên nhánh `template-sync/take-ads-7bf8c3b`, không commit phần đo.
  - Lượt 1, `scripts/check.sh` trên nhánh (trước commit `46e13a14`): `:core:ads` không biên dịch được test vì `SAMPLE_NATIVE_SMALL` (đã sửa ở `46e13a14`). `:app` đỏ hai test bảng khóa (đã sinh lại). `feature/camera` 12 đỏ, `feature/poses` 10 đỏ, `feature/settings` 3 đỏ (chỉ ảnh `_ad`).
  - Lượt 2, sau `46e13a14`: `./gradlew :core:ads:testDebugUnitTest` qua 2084 test.
  - Lượt 3, đo số mới: trong worktree tạm của nhánh, đổi tạm `assertNear` của `ReviewScreenTest`, `ReviewScreenUnfoldedTest` và thêm một `assertEquals` thành viên ở `PosesScreenTest`, `PoseDetailScreenTest` để in giá trị thay vì thất bại; lượt 3b tạm bỏ câu `assertTrue` "photo height < before" của test khung gập, lượt 3c (2026-11-22, trên commit `46e13a14`) của test điện thoại, để đo mọi câu sau nó (lượt 3c cho "actions top" 593.33). Chạy `./gradlew :feature:camera:testDebugUnitTest --tests '*ReviewScreenTest' --tests '*ReviewScreenUnfoldedTest' :feature:poses:testDebugUnitTest --tests '*PosesScreenTest' --tests '*PoseDetailScreenTest'`.
- **Lệnh:** `scripts/check.sh`; `./gradlew :core:ads:testDebugUnitTest`; lệnh đo ở trên.
- **Kết quả:**
  - Số mới của mục 3.5 lấy từ lượt 3. Hai câu `assertTrue` "photo height < before" của `adBar_slotAtBottom_photoShrinks_saveMovesUp` và `adBar_slotFullWidth_photoShrinks_saveAbove` thất bại: ảnh bằng đúng cỡ khi không có thanh (505.33 và 600). Hai test này đổi tên và câu theo mục 3.5.
  - Ảnh chuẩn đỏ: đúng 12 ảnh của mục 3.2. Qua: `PosesScreen_ad-collection_light`, mọi ảnh của `feature/onboarding` (thẻ bước giữ 250 dp), mọi ảnh không có quảng cáo, ảnh của `core/ads` (bản mẫu).
  - `:app` (sau khi sinh lại bảng khóa), `feature/settings` (trừ ảnh), `feature/onboarding`, `feature/loading`, `core/network`: qua.
  - Chưa chạy thử `PosesInContentAdsTest` và phần nối `InContentAds` (việc của phiên, có bước đối chứng và phương án dự phòng ở mục 3.4). Không chạy `:app:assembleStagingRelease`: không thêm thư viện hay module, `core/ads/build.gradle.kts` và luật R8 không đổi.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**
- 1, quảng cáo: kích thước native, luật thanh đáy, khai báo `pose_detail_bottom`, MC-13 ở thư viện.
- 7, module core: `:core:ads`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | `core/ads` (74 file từ mẫu), một file code của `feature/poses`, test và ảnh chuẩn của ba feature |
| B. Độ rõ của yêu cầu | 0 | Spec ads câu 22 đã trả lời và xác nhận, thiết kế đã duyệt, số mới đã đo |
| C. Khả năng kiểm chứng tự động | 1 | Kích thước, bố cục, luật thanh đáy đều có test; phần nối `InContentAds` chỉ có test quét mã và thử tay trên staging |
| D. Ảnh hưởng chéo | 3 | Đổi thanh đáy mà poses, camera, settings dựa vào; đổi khai báo một vị trí |
| E. Độ mới của kỹ thuật | 1 | Lặp lại cách lấy `core/ads` qua nhánh take (T-074, T-098), test đo thanh đáy (T-093, T-094, T-098); test quét mã theo mẫu `LeaveAppCallSitesTest` |
| **Tổng** | 8 | **Mức: Cao (chạm điểm chặn)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `:core:ads` (mọi feature có quảng cáo), `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`, `docs/template-sync.txt`, `docs/ops/manual-test.md`, hai bảng khóa Remote Config.
- Task khác có thể xung đột: không có plan `approved` hay `draft` nào khác đang chờ chạy (2026-11-22). T-101 (`auto_run: false`) chạm `scripts/release.sh`, không chạm file của plan này. Plan sau này chạm thanh đáy hay test đo của ba feature phải chạy sau T-102.
- Lần đồng bộ dự án mẫu kế tiếp (`/to-app`): chạy sau khi T-102 merge, để `core/ads` không còn ở mốc cũ.
- Đề xuất thứ tự chạy: một mình, hay đầu danh sách. Môi trường cloud đang lỗi (issue #330): mở bằng `local` hay `routine` sau khi #330 đóng.

## 7. Điều kiện dừng riêng

- `git merge origin/main` vào nhánh task có xung đột ngoài `docs/template-sync.txt`, `docs/foundation.md`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`.
- Phần riêng của app trong `core/ads` (dải phân cách của `AdSlot`, chiều cao ô của `NativeAdSlot`) bị mất hay đổi hành vi: test của nó đỏ.
- Ảnh chuẩn ngoài 12 ảnh của mục 3.2 lệch mà không phải nhiễu vẽ (dừng như quy tắc chung).
- `RemoteConfigKeysTest` hay bảng khóa đổi thêm, ngoài dòng mã ADR (khung thêm khóa mà spec chưa duyệt).
- Manifest gộp có quyền mới (`MergedManifestTest` đỏ).
- Đầu nhánh `origin/template-sync/take-ads-7bf8c3b` không phải commit `46e13a14` (có commit mới, bị đẩy đè hay bị xóa): phần mới chưa được duyệt.
- Số đo của một câu khẳng định mới ở mục 3.5 lệch quá sai số của dòng đó (điều kiện dừng 8 của `docs/risk-scoring.md`).

## 8. Câu hỏi cho người duyệt

1. Test cho ý e (thư viện không có lớp nổi khi ô lưới bật): feature không có `AdsController` thật trong test, nên không dựng được hành vi ở mức feature. Đề xuất của AI: test quét mã theo ký hiệu (`PosesInContentAdsTest`, có bước đối chứng, có phương án dự phòng ở mục 3.4), cộng test hành vi của khung (`InContentAdsWiringTest`) và hai dòng thử tay trên staging (ô lưới bật, ô lưới tắt). Phương án khác: thêm một test dựng `PosesRoute` với bộ điều khiển giả của `core/ads`. Cách này cần đưa hàm dựng test của `core/ads` ra ngoài module (testFixtures), tức sửa `build-logic`, chỉ làm theo plan rủi ro cao riêng.
2. Ticket và `docs/CLAUDE.md` (mục Khi viết plan) đòi dán `diff` của phần lấy từ dự án mẫu vào mục 3.1, vì phiên của app không đọc được repo mẫu. Ở đây phần gộp đã nằm sẵn trên nhánh `template-sync/take-ads-7bf8c3b` của repo app (74 file, khoảng 5 nghìn dòng), phiên đọc được. Đề xuất của AI: như T-098, plan chỉ ghi nhánh, commit và các chỗ gộp tay; mục 7 dừng task khi nhánh không còn commit `46e13a14`.
3. Duyệt chéo: plan rủi ro Cao. Đề xuất: Nhật duyệt (người phụ trách sản phẩm, đã quyết câu 22), Tuấn duyệt chéo (chủ feature ads), như T-098.

## 9. Quyết định duyệt

- **Quyết định:** duyệt ngày 2026-11-22 (Nhật duyệt, Tuấn duyệt chéo), các câu hỏi mục 8 theo đề xuất của AI.
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:**
  - AI ghi theo lời Nhật (2026-11-22): chọn "Tôi là Nhật, duyệt" khi được hỏi duyệt plan T-102 theo nội dung PR #375 ở commit 050395e.
  - Câu hỏi mục 8, Nhật trả lời: (1) chọn "Test quét mã + thử tay": ý e kiểm bằng `PosesInContentAdsTest`, test của khung và thử tay trên staging, không sửa `build-logic`; (2) chọn "Không dán diff": phiên đọc phần gộp trên nhánh take, dừng khi đầu nhánh không phải `46e13a14`; (3) chọn "Nhật duyệt, Tuấn duyệt chéo".
  - AI ghi theo lời Tuấn (2026-11-22), trong cùng phiên, ngay sau lời duyệt của Nhật và khi plan ở commit 320160c: "Tôi là Tuấn, tôi duyệt."

## 10. Nhật ký thực hiện
