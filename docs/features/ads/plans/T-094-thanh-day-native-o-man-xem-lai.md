---
ticket: T-094
title: Thanh đáy native nhỏ ở màn xem lại ảnh (review_bottom, tắt lúc phát hành)
status: draft                # draft | approved | stopped | done
version: 3                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 15.0M        # cỡ M theo scripts/token-usage.py estimate (2026-11-13), không nâng tay
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-094: THANH ĐÁY NATIVE NHỎ Ở MÀN XEM LẠI ẢNH

## 1. Tóm tắt

**Phiên bản 3 (2026-11-14).** Task dừng ở RUN-20261113-02, bước 4 (điều kiện dừng 8; nhật ký ở mục 10). Vì sao phiên bản 2 chưa đủ tốt: số của chế độ gọn ở cột phải (mục 4.1, lần đo 3) đo trên bố cục `SaveErrorMessage(Modifier.fillMaxWidth(), actionOnNewLine = false)`, trong đó Snackbar một hàng của Material cho chữ cả bề rộng và vẽ nút hành động đè lên chữ; test viết trước không bắt được vì chưa kiểm vị trí tương đối của nút và chữ. Vẽ đúng (nút bên phải chữ) thì Snackbar rộng tối đa 600 dp, nên pt-BR xuống hai dòng; con số một dòng của test pt-BR sai. Thay đổi so với phiên bản 2:
- Mục 3.1: chế độ gọn dùng `Modifier` (không `fillMaxWidth()`) cho thông báo lỗi; tiếp tục trên nhánh `claude/T-094-thanh-day-xem-lai`.
- Mục 4: test pt-BR mong thông báo hai dòng 264 tới 332 (Nhật chấp nhận ngày 2026-11-14, phương án (a) của nhật ký); mọi test chế độ gọn kiểm nút hành động bên phải chữ (`action.left >= text.right`); nhận ba thay đổi test của phiên sau commit test (`Snapshot.sendApplyNotifications()`, cách kiểm "không bị cắt" mới, `actionText`). Số đo lại trên commit `7afc6e9` ở mục 4.1.
- Mục 8: bỏ các câu đã trả lời ở phiên bản 1; thêm câu về ranh giới 841 x 593.

Thêm vị trí `review_bottom` (native nhỏ, thanh đáy) vào `AdPlacement`, khai báo kèm thuộc tính mới `collapsibleLayer = false` để thanh đáy của vị trí này không bao giờ có lớp native thu gọn nổi (câu 19, ý e). Màn xem lại đặt `BottomAdBarScaffold(AdPlacement.REVIEW_BOTTOM)` quanh thanh trên và phần nội dung, dưới hộp thoại "Bỏ ảnh này?", nên nội dung nhường 96 dp ở đáy và hộp thoại phủ lên thanh. Ở khung chuẩn 411 x 891, ảnh giữ 379 x 505 như khi không có khe, nút chính dời lên 96; ảnh chỉ co ở khung thấp (Nhật quyết ngày 2026-11-13: giữ bố cục của code, sửa thiết kế `ReviewScreen_ad_dark` theo). Bản staging bật sẵn `is_show_review_bottom`.

Kiểm chứng bằng unit test của khung (khai báo, các lớp của thanh, mã đơn vị, kho native), test Robolectric của màn xem lại với thanh giả (vị trí, tỷ lệ ảnh, hộp thoại phủ lên thanh, màn thấp, máy gập mở ra) và ba ảnh chuẩn cùng tên thiết kế `ReviewScreen_ad_*`.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md` và câu 19 của `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-9, tiêu chí 4 | Màn xem lại chỉ có thanh đáy `review_bottom` | Một vị trí mới; không thêm vị trí nào khác ở camera |
| KB-9, tiêu chí 5 | Khóa bật, native bật, đã đồng ý thì thanh native nhỏ 80 dp ở đáy, dưới hàng nút; ảnh co lại, giữ tỷ lệ, không bị cắt hay che; bốn nút không bị che | Thanh của khung (`BottomAdBarScaffold`); nội dung nhận phần đệm đáy 96 khi khe có quảng cáo. Ở 411 x 891 khoảng trống giữa hàng nút và nút chính (150 dp) đủ nhận 96, nên ảnh giữ 379 x 505 và chỉ nút chính dời lên; "ảnh co lại cho vừa phần còn lại" xảy ra khi không đủ chỗ (khung thấp, lỗi lưu), theo `weight` và `aspectRatio` có sẵn. Số đo ở mục 4.1 |
| KB-9, tiêu chí 6 | `is_show_native_collap` bật thì vẫn chỉ có thanh nhỏ, không có lớp nổi | Thuộc tính khai báo của vị trí, khung đọc nó khi chọn các lớp; vị trí cũng không mở kho native thu gọn |
| KB-9, tiêu chí 7 | Đang lưu, lỗi lưu, mở từ ô ảnh vừa chụp: thanh vẫn ở đáy; hộp thoại "Bỏ ảnh này?" và thông báo lỗi lưu phủ lên trên thanh | Thanh bọc mọi trạng thái của màn. Ô ảnh vừa chụp mở cùng `ReviewRoute`. Hộp thoại vẽ sau thanh trong cùng `Box`, nên lớp mờ và thẻ nằm trên thanh. Thông báo lỗi lưu nằm trong nội dung, phía trên nút chính, nên luôn ở trên thanh, không bị thanh che |
| KB-9, tiêu chí 8 | `is_show_native_bottom` tắt thì khe là banner cùng chỗ; ảnh và bốn nút không bị che | Hành vi có sẵn của khung (D-01): phần đệm theo chiều cao banner |
| KB-9, tiêu chí 9 | Khóa tắt hay không tải được thì không có thanh, ảnh và nút như khi chưa có quảng cáo | Hành vi có sẵn của khung (D-04): không có quảng cáo thì không chừa chỗ |
| KB-11, tiêu chí 3 | Máy gập mở ra, thanh có quảng cáo: ảnh và nút hiện trọn, đúng `ReviewScreen_ad_dark_unfolded` | Thanh trải hết bề rộng đáy (thiết kế đã duyệt), ảnh giữ 450 x 600, nút chính dời lên trên thanh |
| KB-6 | Các hành vi chụp, lưu, chia sẻ, chụp lại của màn xem lại | Không đổi; test có sẵn giữ nguyên |

Thiết kế tham chiếu: `features/camera/design/README.md` (mục 1, 3, 5, 6), ảnh `design/screens/ReviewScreen_ad_dark.png`, `ReviewScreen_ad_dark_unfolded.png`, số liệu ở `design/context/ReviewScreen_ad_dark.md`, `ReviewScreen_ad_dark_unfolded.md`. Frame `ReviewScreen_ad_dark` đã duyệt ngày 2026-11-13 co ảnh còn 307 x 409; Nhật quyết cùng ngày giữ bố cục của code (ảnh 379 x 505, hàng nút ở 617 như `default`, nút chính dời lên 96 tới y 715, thanh ở 795) và sửa frame trên Figma, xuất lại trong một PR thiết kế riêng. Plan dựa trên frame đã sửa; PR thiết kế đó phải merge trước khi task chạy (mục 6).

ADR liên quan: ADR-0045 (khung quảng cáo AdsKit PARTNER), ADR-0020 (thiết kế và ảnh chuẩn), ADR-0029, ADR-0031 (test cũ), ADR-0007, ADR-0009 (mã thật và production là việc của người).

## 3. Giải pháp

### 3.1. Cách làm

**Cách tiếp tục (phiên bản 3):** tiếp tục trên nhánh `claude/T-094-thanh-day-xem-lai` (chưa có PR; commit `d922a38` test viết trước, `9bb26d7` chi tiết test, `7afc6e9` code dở). Phiên cập nhật nhánh theo nhánh chính (`git fetch origin main`, rồi merge `origin/main`; nhánh chính chỉ hơn nhánh này commit báo cáo `6e90d2b` lúc viết plan). Phần đã làm theo nhật ký mục 10 giữ nguyên. Phần còn phải làm:
- sửa test pt-BR và thêm kiểm vị trí nút hành động theo mục 4 (bước 3 chỉ áp cho các test đổi này: chạy chúng trên `7afc6e9` trước khi sửa code, thấy test pt-BR mới qua và câu `action.left >= text.right` qua trên bố cục `Modifier`, đỏ khi đổi tạm lại `fillMaxWidth()`);
- chế độ gọn của cột phải giữ `modifier = if (compact) Modifier else Modifier.fillMaxWidth()` cho `SaveErrorMessage` (đã có ở `7afc6e9`);
- sinh lại `docs/remote-config-keys.md`, `docs/remote-config-template.json` (`RemoteConfigKeysTest` còn đỏ);
- ghi ba ảnh chuẩn mới `ReviewScreen_ad_dark`, `ReviewScreen_ad_dark_unfolded`, `ReviewScreen_ad_small` (chỉ giữ ba ảnh này, trả mọi ảnh khác);
- `docs/ops/manual-test.md`, `docs/template-sync.txt`;
- `scripts/design-compare.sh camera` (ảnh so sánh trong `docs/features/camera/design/review/`), `scripts/check.sh`, rồi bước 5 tới 10.

Plan này viết cho nhánh chính sau khi T-093 (`settings_bottom`) đã merge (T-093 đã `on-staging` ở RUN-20261113-02): T-093 thêm dòng `SETTINGS_BOTTOM` vào `AdPlacement`, khóa `is_show_settings_bottom` vào `StagingRemoteValuesModule`, test của nó và bảng khóa. Mọi file chung ở mục 3.2 ghi Sửa trên trạng thái đó.

**1. Thuộc tính khai báo `collapsibleLayer` (`:core:ads`, theo mô hình chung, MC-10).**

- `AdPlacement` nhận thêm tham số `collapsibleLayer: Boolean = true`, như `firstOpenOnly`: chỉ truyền vào `declaration`, đọc qua `declaration.collapsibleLayer`. KDoc: "False: the bottom bar of this placement never has the floating collapsible layer, even with `is_show_native_collap` on (D-01); native small bottom bars only."
- `AdPlacementDeclaration` thêm trường `collapsibleLayer: Boolean = true` và kiểm `require(collapsibleLayer || (format == AdFormat.NATIVE_SMALL && nativeEvery == null))`, thông báo `"Placement $code: collapsibleLayer is for a native small bottom bar only"`.
- `AdUnitIds.kt`, `layerFormats`: thanh đáy không có lớp thu gọn thì chỉ có `BANNER`:

  ```kotlin
  internal val AdPlacementDeclaration.layerFormats: List<AdFormat>
      get() = when {
          format != AdFormat.NATIVE_SMALL -> emptyList()
          collapsibleLayer -> BOTTOM_BAR_LAYER_FORMATS
          else -> BOTTOM_BAR_LAYER_FORMATS - AdFormat.NATIVE_COLLAPSIBLE
      }
  ```

  Nhờ đó `usesStock(NATIVE_COLLAPSIBLE)` (`NativeAdSources.kt`) trả false cho vị trí này: bật `is_show_review_bottom` không mở hay nạp kho native thu gọn, và `missingUnitKeys` không đòi khóa khe `native_collap` chỉ vì vị trí này. Không sửa `NativeAdSources.kt`.
- `BottomAdBar.kt`, ba hàm:
  - `bottomAdBarLayers(allowed, flags)` (hai đối số): không đổi; nó chỉ chọn khe theo `is_show_native_bottom` và lớp theo `is_show_native_collap`.
  - `bottomAdBarLayers(canRequestAds, enabled, flags, bannerHadAd, collapsibleLayer: Boolean)` (năm đối số): tham số mới **không có giá trị mặc định**; kết quả của nhánh có đồng ý là `bottomAdBarLayers(allowed = true, flags)` rồi `copy(collapsible = false)` khi `collapsibleLayer` sai. Nhánh không đồng ý giữ như cũ (chỉ banner, không lớp).
  - `rememberBottomAdBarLayers(canRequestAds, enabled, flags, collapsibleLayer: Boolean)`: tham số mới **không có giá trị mặc định**, truyền xuống hàm năm đối số.
  - `rememberBottomAdBarState(ads, placement)` truyền `placement.declaration.collapsibleLayer`. Quên truyền ở bất kỳ lời gọi nào thì không biên dịch được.

  Điều kiện nằm trong hàm thuần mà mọi lần chọn lớp đều gọi, nên khóa `is_show_native_collap` bật lúc màn đang mở cũng không thêm lớp.
- `AdPlacement`: thêm một dòng, sau `REVIEW_INTERSTITIAL`:

  ```kotlin
  /** Bottom bar of the review screen, below the buttons; never the floating layer (spec ads, question 19). */
  REVIEW_BOTTOM("review_bottom", AdFormat.NATIVE_SMALL, collapsibleLayer = false),
  ```

  Một dòng, thuộc tính sau định dạng: `scripts/release.sh`, `scripts/status.py`, `scripts/ads-plan.py` đọc được (mẫu `(, .*)?`).

**2. Màn xem lại (`:feature:camera`).**

- `ReviewScreen` nhận thêm tham số cuối:

  ```kotlin
  adBar: @Composable (Modifier, @Composable (PaddingValues) -> Unit) -> Unit = { barModifier, content ->
      BottomAdBarScaffold(AdPlacement.REVIEW_BOTTOM, barModifier, content)
  },
  ```

  Trong `Box` ngoài cùng (nền `cameraBackground`, tag `SCREEN`), `adBar` thay chỗ `Column` hiện nay: `adBar(barModifier) { padding -> Column(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) { ReviewTopBar(...); ... } }`. `barModifier` là `Modifier.fillMaxSize()` cộng `clearAndSetSemantics {}` khi hộp thoại mở (chuyển từ `Column` lên, để trình đọc màn hình không đọc cả khe quảng cáo sau hộp thoại). `DiscardDialog` vẫn vẽ sau, trong cùng `Box`, nên phủ lên thanh.
- Không đổi `ReviewPhoneContent`. `ReviewUnfoldedContent` sửa cột phải (dưới đây). Cả hai đã co ảnh theo chỗ còn lại. Điện thoại: ảnh `weight(1f, fill = false)` và `aspectRatio(3/4)`; ở 411 x 891 ảnh đã bị giới hạn bởi bề rộng (379), cột chứa ảnh vẫn dư chỗ khi có khe (biên 37,7 dp ở khung test, khoảng 13,7 trên máy có thanh trạng thái 24), nên ảnh và hàng nút đứng yên, chỉ khoảng trống trên nút chính nhường 96 (đo ở mục 4.1). Gập mở: `photoHeight = min(maxHeight - 20, maxPhotoWidth / 0.75)`; ở 673 x 841 chỗ còn lại sau thanh trên và 96 vẫn đủ cho ảnh 600, nên ảnh giữ 450 x 600 như thiết kế. Nút chính cách khe 20 (lề đáy có sẵn); gói thiết kế ở PR #337 ghi 20 theo code, đã thống nhất.
- `ReviewUnfoldedContent`, cột phải gọn lại khi thiếu chỗ. Hiện nay cột là `Column(Modifier.weight(1f).fillMaxHeight())` với `Spacer(Modifier.weight(1f))` giữa hàng nút và phần đáy; khi cột thấp hơn nội dung (nằm ngang 960 x 520 có khe và có thông báo lỗi: cần 192 + 16 + 92 + 16 + 56 = 372, cột chỉ 340), `Column` ép nút chính còn 40 dp (dưới 56 của thiết kế), vùng chạm dưới 48. Không dùng cuộn: `verticalScroll` cắt bóng sticker 4 dp dưới nút chính (người rà đã thấy `verifyRoborazziDebug` đỏ ở `ReviewScreen_default_dark_unfolded.png`, `ReviewScreen_save-error_unfolded.png`) và để nút chính nằm dưới mép cột tới khi cuộn. Đổi thành:

  ```kotlin
  BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
      val hasError = ready.save in ERROR_STATUSES
      // Room the stacked actions need: 3 x 48 + 2 x 24, the main button 56, and, with an error, ERROR_ROOM: 140 covers
      // the error in Vietnamese and English in the 175 dp column (112 to 132 high). Longer languages are taller there
      // (pt-BR 152 to 160), which only matters in a column of about 503 dp or more, where the stacked layout still
      // fits (192 + 16 + 160 + 16 + 56 = 440).
      val stackedNeed = STACKED_ACTIONS_HEIGHT + Spacing.md + MAIN_BUTTON_HEIGHT +
          if (hasError) Spacing.md + ERROR_ROOM else 0.dp
      val compact = maxHeight < stackedNeed
      Column(modifier = Modifier.fillMaxSize()) {
          ReviewActions(..., vertical = !compact)
          Spacer(modifier = Modifier.weight(1f))
          if (hasError) {
              // Compact: on one row as on a phone, not forced to the full width, or Material's one-row snackbar
              // gives the message the whole width and draws its action over it.
              SaveErrorMessage(..., modifier = if (compact) Modifier else Modifier.fillMaxWidth(), actionOnNewLine = !compact)
              Spacer(modifier = Modifier.height(Spacing.md))
          }
          PrimaryButton(...) // as today
      }
  }
  ```

  với `STACKED_ACTIONS_HEIGHT = 192.dp`, `MAIN_BUTTON_HEIGHT = 56.dp`, `ERROR_ROOM = 140.dp` (hằng `private` trong `ReviewUnfolded.kt`, chú thích ghi nguồn). Số đo thông báo lỗi ở cột 175 (673 x 841, test tạm 2026-11-13): tiếng Việt `FAILED_STORAGE_FULL` 112, `PERMISSION_BLOCKED` 132; tiếng Anh `PERMISSION_BLOCKED` 132; pt-BR `FAILED_STORAGE_FULL` 152, `PERMISSION_BLOCKED` 160. 140 phủ tiếng Việt và tiếng Anh; ngôn ngữ dài vượt 140 chỉ ở cột hẹp 175, mà cột đó chỉ có khi cột cao từ khoảng 503 trở lên (máy gập mở ra), nơi xếp dọc vẫn vừa (cần 440), nên ngưỡng không làm nút chính bị ép. Thiếu chỗ thì hàng nút phụ nằm ngang như điện thoại (cao 72), thông báo lỗi đặt nút hành động cùng dòng (cột rộng): ở 960 x 520 cần 72 + 16 + 48 + 16 + 56 = 208, cột 340, không cắt, không cuộn, không đụng bóng. Đủ chỗ thì y như hôm nay. Đã đo thật bằng thay đổi tạm này (mục 4.1). `verifyRoborazziDebug` của `ReviewScreenScreenshotTest`, `ReviewScreenSavingScreenshotTest`, `ReviewScreenUnfoldedScreenshotTest` qua trên code tạm: các ảnh chuẩn có sẵn (gồm `ReviewScreen_default_dark_unfolded.png`, `ReviewScreen_save-error_unfolded.png`) không đổi; `ReviewScreenTest`, `ReviewScreenUnfoldedTest` qua. Ngưỡng dùng hằng thay vì đo thông báo lỗi thật: ở 841 x 593 có khe và có lỗi, cột 413 nhỏ hơn 420 nên hàng nút nằm ngang dù xếp dọc vẫn vừa (372); chấp nhận được vì cả hai cách đều trọn và đúng thiết kế điện thoại.
- `ReviewRoute` không đổi: dùng `adBar` mặc định. Không có `AdsController` (test, preview) thì thanh chỉ vẽ nội dung, phần đệm 0, nên mọi test và ảnh chuẩn có sẵn không đổi.

**3. Bản staging.** `StagingRemoteValuesModule`: thêm `AdPlacement.REVIEW_BOTTOM` vào danh sách vị trí bật sẵn (câu 19, ý a); `StagingRemoteValuesModuleTest`: thêm `"is_show_review_bottom" to true` (dữ liệu của phần plan thêm, ADR-0029). `is_show_native_collap` của staging vẫn `false`.

**4. Bảng khóa.** Sinh lại `docs/remote-config-keys.md` và `docs/remote-config-template.json` bằng `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`; thêm `is_show_review_bottom` (khóa suy ra từ khai báo).

**5. Tài liệu.** `docs/ops/manual-test.md`, mục Quảng cáo: thêm `is_show_review_bottom` vào dòng khóa không đặt `false` trên Firebase staging, và bước thanh đáy ở màn xem lại (mục 4, dòng máy thật). Cách bật dần 10% (câu 19, ý d) đã ghi ở spec ads, nơi người phát hành đọc; plan không chép lại (câu hỏi 3).

**6. Đề xuất về dự án mẫu.** Thuộc tính `collapsibleLayer` có ích cho mọi app (màn có nội dung không được che). Trong vòng một tuần sau khi merge (`docs/ops/from-app.md`, mục 1), người mở issue `from-app`: thuộc tính, chỗ khung đọc nó, đề xuất thêm dòng vào `ads-business-rules.md` (D-01, MC-08) và `ads-guide.md` (mục 5, các thuộc tính của dòng `AdPlacement`) của mẫu; và `scripts/ads-plan.py` của mẫu chưa tách thuộc tính này ra cột riêng. Task ghi thuộc tính vào đoạn "core/ads keeps parts of this app..." của `docs/template-sync.txt`, để đợt đồng bộ lấy bản mẫu của `:core:ads` không làm mất nó khi mẫu chưa nhận. Ghi vào mô tả PR và mục "Cần người kiểm tra" của báo cáo phiên.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | Tham số và trường `collapsibleLayer`, kiểm khai báo; dòng `REVIEW_BOTTOM` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIds.kt` | `layerFormats` bỏ `NATIVE_COLLAPSIBLE` khi `collapsibleLayer` sai |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/BottomAdBar.kt` | `bottomAdBarLayers`, `rememberBottomAdBarLayers`, `rememberBottomAdBarState` nhận `collapsibleLayer`; KDoc của `BottomAdBarScaffold` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementTest.kt` | Test kiểm khai báo `collapsibleLayer` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementAppTest.kt` | Tập vị trí của app có `review_bottom` (dữ liệu, ADR-0029); test thuộc tính và kho của các thanh đáy (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/BottomAdBarTest.kt` | Test lớp của thanh khi `collapsibleLayer` sai (mục 4); lời gọi `rememberBottomAdBarLayers` của hàm `Bar` và bốn lời gọi năm đối số thêm `collapsibleLayer = true` (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsControllerNativeStockTest.kt` | Test đường nối với controller thật (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementSourceTest.kt` | `releaseScript_readsLinesWithAttributes` thêm dòng mẫu có `collapsibleLayer = false` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | Chuỗi mã đơn vị có `review_bottom` (dữ liệu, ADR-0029); test `layerFormats`, `missingUnitKeys` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsControllerUnitIdsTest.kt` | Chuỗi mã đơn vị có `review_bottom` khi test đòi đủ mã của vị trí (dữ liệu, ADR-0029) |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreen.kt` | Tham số `adBar`, thanh bọc thanh trên và nội dung, hộp thoại vẽ sau thanh |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewUnfolded.kt` | Cột phải: thiếu chỗ (`BoxWithConstraints`, ngưỡng hằng) thì hàng nút phụ nằm ngang và thông báo lỗi để nút hành động cùng dòng; đủ chỗ như cũ |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenTest.kt` | Test thanh đáy ở điện thoại và màn thấp (mục 4); hàm `fakeAdBar` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenUnfoldedTest.kt` | Test thanh đáy khi mở gập và nằm ngang (mục 4); lớp `ReviewScreenUnfoldedScreenshotTest` trong cùng file: ảnh `ad_dark_unfolded` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenScreenshotTest.kt` | Ảnh `ad_dark`, `ad_small` với `BottomAdBarLayout` và `NativeAdCard` giả |
| Tạo | `feature/camera/src/test/screenshots/ReviewScreen_ad_dark.png` | Ảnh chuẩn mới, cùng tên thiết kế |
| Tạo | `feature/camera/src/test/screenshots/ReviewScreen_ad_dark_unfolded.png` | Ảnh chuẩn mới, cùng tên thiết kế |
| Tạo | `feature/camera/src/test/screenshots/ReviewScreen_ad_small.png` | Ảnh chuẩn mới: khung thấp, `PERMISSION_BLOCKED` (thông báo và nút "Mở cài đặt") và thanh: nhiều nội dung nhất |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModule.kt` | Bật sẵn `REVIEW_BOTTOM` |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModuleTest.kt` | `"is_show_review_bottom" to true` (dữ liệu, ADR-0029) |
| Sửa | `docs/remote-config-keys.md` | Sinh lại, thêm `is_show_review_bottom` |
| Sửa | `docs/remote-config-template.json` | Sinh lại cùng lệnh |
| Sửa | `docs/features/camera/design/review/` | Ảnh so sánh và `summary.md` do `scripts/design-compare.sh camera` sinh |
| Sửa | `docs/ops/manual-test.md` | Mục Quảng cáo: khóa `is_show_review_bottom`, bước thanh đáy ở màn xem lại |
| Sửa | `docs/template-sync.txt` | Đoạn "core/ads keeps parts of this app...": thuộc tính `collapsibleLayer` (khai báo, `layerFormats`, `BottomAdBar.kt`) và test của nó, giữ khi đồng bộ tới khi mẫu nhận |

Ảnh chuẩn có sẵn của `feature/camera` không đổi: test và ảnh có sẵn dựng màn không có `AdsController`, thanh chỉ vẽ nội dung. Lệnh ghi ảnh ghi lại mọi ảnh của module; chỉ giữ ba ảnh mới, trả mọi ảnh khác bằng `git checkout -- <ảnh>` (`CLAUDE.md`, mục Lệnh).

Nơi khác đọc file mà plan này đổi:

- `scripts/release.sh`, `scripts/status.py` (mục 6, "Sẵn sàng phát hành"), `scripts/ads-plan.py` đọc dòng một hàng của `AdPlacement.kt`, mẫu nhận thuộc tính sau định dạng; dòng mới một hàng, không sửa script. Test của các script đọc file mẫu hay code thật, không đếm số vị trí.
- `app/build.gradle.kts` chỉ nêu đường dẫn của `docs/remote-config-keys.md` và `docs/remote-config-template.json` cho tham số `-PremoteConfigKeys.update`; đường dẫn không đổi, không sửa.
- `scripts/main-guard-verified.sh` coi hai bảng khóa là tài liệu Gradle đọc (`GRADLE_READ_DOCS`), nên commit merge của task được kiểm như code; đó là điều plan cần, không sửa script.
- `scripts/tests/run.sh` nêu hai bảng khóa trong các test của `main-guard-verified.sh` và `merge-if-green.sh` bằng repo giả, không đọc nội dung; chạy `release.sh --fill-test-units` trên `AdPlacement.kt` thật (dòng mới là native nhỏ, đã có mã thử theo định dạng, nên vẫn mã 0); và chỉ ghi thêm một dòng chú thích vào `AdUnitIds.kt` của repo giả ở test `template-sync`. Không sửa.
- `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` so hai bảng khóa với `AdsSettings.all()` và `AdPlacement.entries`; hai bảng sinh lại trong cùng commit code, không sửa test.
- `scripts/check.sh`, `scripts/design-compare.sh` đọc `src/test/screenshots/` và `design/screens/`: ba ảnh mới cùng tên thiết kế (`_small` không có thiết kế).
- `docs/template-sync.txt`: `scripts/setup-check-app.sh` chỉ nhắc tên file trong một thông báo, không đọc nội dung; plan chỉ thêm chữ vào một đoạn chú thích (dòng `#`), không đổi dòng `base=` hay `hold=` mà `scripts/template-sync.sh` của mẫu đọc. Không sửa script.
- `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md` là tài liệu của dự án mẫu: không sửa ở app, đưa vào đề xuất về mẫu (mục 3.1, bước 6).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: vị trí mới `review_bottom` (native nhỏ, thanh đáy), khóa `is_show_review_bottom` mặc định tắt, bản staging bật sẵn (spec ads câu 19; spec camera mục 5, KB-9). Không có lớp native thu gọn (câu 19, ý e). Mã thật trong cấu hình phát hành và khóa trên Firebase production là việc của người (ADR-0007, ADR-0009).
- Sự kiện phân tích: không thêm. Số liệu quảng cáo của khung ghi theo `ad_placement = review_bottom` như mọi thanh đáy (spec ads câu 19, ý d: không thêm sự kiện mới).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Dùng `:core:ads` mà `:feature:camera` đã phụ thuộc.

### 3.4. Phương án dự phòng

Không có. Thanh đáy, `BottomAdBarLayout` và cách đặt hộp thoại phủ lên đã có mẫu ở thư viện và chi tiết tư thế.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (kết quả ở mục 4.1) liệt kê test của các file plan sửa. Plan đổi các hành vi sau:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Thanh đáy native nào cũng có lớp nổi khi `is_show_native_collap` bật | Vị trí khai báo `collapsibleLayer = false` không có lớp nổi | `BottomAdBarTest` (các test của `bottomAdBarLayers` với `collap = true`) | Giữ: các test đó dùng vị trí mặc định (`collapsibleLayer = true`), kết quả không đổi |
| Vị trí native nhỏ nào cũng có hai lớp `BANNER`, `NATIVE_COLLAPSIBLE` | Vị trí `collapsibleLayer = false` chỉ có `BANNER` | `AdUnitIdsTest` (thiếu khóa khe, `layerFormats`) | Giữ: dữ liệu có sẵn chỉ có vị trí mặc định; chuỗi mã thêm `review_bottom=unit/..` (dữ liệu, ADR-0029) |
| Tập vị trí của app | Thêm `review_bottom` | `AdPlacementAppTest.appPlacements_declaredWithSpecFormats` | Dữ liệu của phần plan thêm (ADR-0029), câu khẳng định giữ |
| Chữ ký của `rememberBottomAdBarLayers` và `bottomAdBarLayers` năm đối số (tham số mới không có mặc định) | Mọi lời gọi truyền `collapsibleLayer` | `BottomAdBarTest`: hàm `Bar` (dòng 120) và `allowed_consentAndPlacement_bannerWithAdKeptWhenConsentWithdrawn`, `layers_bannerKeptWithoutConsent_neverNativeOrLayer` (dòng 269 tới 277) | Thêm đối số `collapsibleLayer = true` (vị trí `POSES_BOTTOM` của test, có lớp); không đổi câu khẳng định nào, kết quả như cũ. Đổi chữ ký có chủ ý để quên truyền thì không biên dịch được |
| `release.sh` đọc dòng có thuộc tính | Thêm dòng có `collapsibleLayer = false` | `AdPlacementSourceTest.releaseScript_readsEveryShippedPlacement`, `releaseScript_readsLinesWithAttributes` | Giữ: `review_bottom` là một dòng, mẫu của `release.sh` đọc được (kiểm bằng `AdPlacementSourceTest`, mục 4); `readsLinesWithAttributes` thêm dòng mẫu `R_B("r_b", AdFormat.NATIVE_SMALL, collapsibleLayer = false)` và `"r_b"` vào danh sách mong đợi (thêm trường hợp, không nới) |
| Giá trị cục bộ của bản staging | Thêm `is_show_review_bottom = true` | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet` | Dữ liệu (ADR-0029), câu khẳng định giữ |
| Hộp thoại mở thì `Column` của màn bị ẩn với trình đọc màn hình | Phần bị ẩn là cả thanh (nội dung và khe) | `ReviewScreenTest.discard_hidesContentFromAccessibility`, `ReviewScreenUnfoldedTest.discardDialog_isCentered_andHidesContent` | Giữ: chữ của nội dung vẫn không đọc được; test mới thêm khe |
| Cột phải khi mở gập hay nằm ngang | Thiếu chỗ thì hàng nút phụ nằm ngang, thông báo lỗi để nút cùng dòng; đủ chỗ như cũ | `ReviewScreenUnfoldedTest.photoLeft_actionsAndSaveRight`, `saveError_isInRightColumn_aboveSave_andRetries`, `discardDialog_isCentered_andHidesContent`, bốn test `landscape_saveInsideWindow_*`; `ReviewScreenUnfoldedScreenshotTest.default_dark_unfolded`, `saveError_unfolded` | Giữ: không khung nào của các test đó thiếu chỗ (không có khe); đã chạy trên thay đổi tạm và `verifyRoborazziDebug` qua, hai ảnh `ReviewScreen_default_dark_unfolded.png`, `ReviewScreen_save-error_unfolded.png` không đổi (mục 4.1) |
| Bố cục điện thoại và gập mở | Không đổi khi không có quảng cáo (phần đệm 0); có khe thì ở 411 x 891 ảnh và hàng nút đứng yên, nút chính dời lên 96 | `ReviewScreenTest`, `ReviewScreenUnfoldedTest`, `ReviewScreenScreenshotTest` | Giữ, ảnh chuẩn có sẵn không đổi |

`scripts/existing-tests.py` xếp 16 test vào nhóm "Cần xem trước" ở lần chạy sau rà soát (19 ở lần đầu; các test bàn dưới đây gồm cả hai lần). Các test của `:core:ads` (`AdSlotTest`, `BannerAdSizeTest`, `InListAdsTest`, `NativeAdCellTest`, `NativeAdSlotTest`, `NativeFullScreenOverlayTest`, `RewardedActionTest`) và của `:feature:poses` (`PosesScreenTest`, `PoseDetailScreenTest`) dùng vị trí khác hay thanh mặc định, không đọc `collapsibleLayer` hay `REVIEW_BOTTOM`: giữ nguyên. Ba test đo vị trí của màn xem lại (`ReviewScreenTest.smallPortuguese_saveError_primaryButtonVisible`, `ReviewScreenUnfoldedTest.photoLeft_actionsAndSaveRight`, `ReviewScreenUnfoldedTest.saveError_isInRightColumn_aboveSave_andRetries`) dựng màn không có `AdsController`, phần đệm 0: giữ nguyên câu khẳng định. Trong nhóm "Nên xem", `AdPreloadPlanTest.fromPlacements_withBottomBar_hasCollapsibleInSecondGroup` đọc `usesStock`: tập vị trí của app vẫn có `POSES_BOTTOM` dùng kho thu gọn nên giữ; test mới ở mục 4 kiểm riêng `REVIEW_BOTTOM`. Các test của `AdPlacementTest`, `RemoteConfigKeysTest`, `StagingRemoteValuesModuleTest` dùng `AdPlacement.entries` không đếm số vị trí cố định ngoài các test đã nêu ở bảng trên.

## 4. Kiểm thử

Test viết trước, ở commit test của bước 3. Chuỗi mã đơn vị của test (`AdUnitIdsTest`, `AdsControllerUnitIdsTest`) thêm `review_bottom=unit/17` (T-093 dùng `unit/16`). Thanh giả trong test màn hình: `BottomAdBarLayout(reservation = BottomAdBarDefaults.nativeReservation hoặc 0.dp, slot = { Box(it.height(80.dp).background(Color.Red).testTag("fake_ad_slot")) { Text("Ad") } }, layer = null, modifier = barModifier, content = content)`, truyền qua `adBar`; `withSlot` đổi được trong test (như `fakeBar` của `PoseDetailScreenTest`). Test màn hình chạy `@GraphicsMode(NATIVE)`, khung `vi-w411dp-h891dp-xxhdpi` trừ khi ghi khác. Hàm `fakeAdBar` vào commit test; tham số `adBar` chưa có nên test không biên dịch được ở bước 3, đó là lý do thất bại đúng (thiếu API); ở bước 4 thêm tham số rồi test chạy.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| `release.sh` đọc dòng có thuộc tính mới | Unit test | `AdPlacementSourceTest.releaseScript_readsLinesWithAttributes` thêm dòng `R_B("r_b", AdFormat.NATIVE_SMALL, collapsibleLayer = false)`, mong đợi có `"r_b"` |
| Câu 19 ý e: khai báo chỉ cho thanh đáy native nhỏ | Unit test | `AdPlacementTest.collapsibleLayerOff_onlyForNativeSmallBottomBar`: `AdPlacementDeclaration("x", INTERSTITIAL, collapsibleLayer = false)` và `AdPlacementDeclaration("x", NATIVE_SMALL, nativeEvery = 8, collapsibleLayer = false)` ném `IllegalArgumentException`; đối chứng `AdPlacementDeclaration("x", NATIVE_SMALL, collapsibleLayer = false)` không ném |
| KB-9, tiêu chí 4, 6: vị trí của app | Unit test | `AdPlacementAppTest`: tập vị trí có `"review_bottom" to NATIVE_SMALL`; `reviewBottom_hasNoCollapsibleLayer`: `REVIEW_BOTTOM.declaration.collapsibleLayer` sai, `POSES_BOTTOM`, `POSE_DETAIL_BOTTOM`, `SETTINGS_BOTTOM` đúng |
| KB-9, tiêu chí 6: không nạp lớp nổi | Unit test | `AdPlacementAppTest.reviewBottom_usesNoCollapsibleStock`: `REVIEW_BOTTOM.usesStock(NATIVE_COLLAPSIBLE)` sai, `usesStock(NATIVE_SMALL)` đúng; đối chứng `POSES_BOTTOM.usesStock(NATIVE_COLLAPSIBLE)` đúng; `AdPreloadPlan.fromPlacements(listOf(REVIEW_BOTTOM))` không có `NATIVE_COLLAPSIBLE` trong nhóm nào, đối chứng `listOf(POSES_BOTTOM)` có |
| KB-9, tiêu chí 6: mã đơn vị | Unit test | `AdUnitIdsTest.noCollapsibleLayer_layerFormatsBannerOnly`: `layerFormats` của khai báo `collapsibleLayer = false` là `[BANNER]`; `missingUnitKeys(listOf(khai báo đó), setOf(code))` là `["banner"]`, không có `native_collap`; đối chứng vị trí mặc định có cả hai |
| KB-9, tiêu chí 6: không có lớp nổi dù khóa bật | Unit test | `BottomAdBarTest.layers_collapsibleLayerOff_noLayerWithCollapOn`: `bottomAdBarLayers(true, true, barFlags(nativeBottom = true, collap = true), false, collapsibleLayer = false)` bằng `BottomAdBarLayers(NATIVE, collapsible = false)`; đối chứng cùng đối số với `collapsibleLayer = true` có `collapsible = true`; `nativeBottom = false` vẫn ra `BANNER` |
| KB-9, tiêu chí 6: đường nối với controller thật | Compose UI test | `AdsControllerNativeStockTest.reviewBottom_collapOn_noLayer_noCollapsibleRequest`: `settings` chỉ bật `is_show_review_bottom` (tắt `POSES_BOTTOM`, `GALLERY_NATIVE_SMALL` trong test), `is_show_native_collap` bật; `startAndPreload()`: `requestsOf(NATIVE_COLLAPSIBLE)` bằng 0, `requestsOf(NATIVE_SMALL)` bằng 1; giao quảng cáo khe, `show { BottomAdBarScaffold(AdPlacement.REVIEW_BOTTOM) {} }`: khe hiện quảng cáo, không có tag `bottom_ad_bar_layer` hay `CollapsibleNativeAdTestTags.AD`, `requestsOf(NATIVE_COLLAPSIBLE)` vẫn 0. Đối chứng: cùng cấu hình với `POSES_BOTTOM` bật thay `REVIEW_BOTTOM` thì có một lượt gửi `NATIVE_COLLAPSIBLE` và có lớp (như `layer_entersAfterPreload_showsStockAd`) |
| KB-9, tiêu chí 6: khóa bật lúc màn đang mở, controller thật | Compose UI test | `AdsControllerNativeStockTest.reviewBottom_collapSwitchedOnWhileShown_noLayer`: `is_show_native_collap` tắt lúc đầu, màn `REVIEW_BOTTOM` đang hiện khe; bật khóa, `idle()` rồi `runPreload()`: không có lớp, không có lượt gửi `NATIVE_COLLAPSIBLE`. Đối chứng với `POSES_BOTTOM` cùng thao tác: có lượt gửi `NATIVE_COLLAPSIBLE` sau khi bật |
| KB-9, tiêu chí 6: khóa bật lúc màn đang mở | Compose UI test | `BottomAdBarTest.host_collapsibleLayerOff_collapSwitchedOn_noLayer`: `BottomAdBarHost` với `layers` từ `rememberBottomAdBarLayers(true, true, flags, collapsibleLayer = false)`, `flags` là `mutableStateOf` đổi `collap` từ false sang true; sau mỗi lần đổi, tag `bottom_ad_bar_layer` không có, tag `bottom_ad_bar_slot` có; đối chứng `collapsibleLayer = true` thì sau khi đổi có lớp |
| KB-9, tiêu chí 5: thanh dưới hàng nút, ảnh giữ khi đủ chỗ | Compose UI test | `ReviewScreenTest.adBar_slotAtBottom_photoKept_saveMovesUp`: khe cao 80 (±0,5), cách đáy 16 (đỉnh 795); ba nút phụ và nút chính có `bottom <= slot.top`; nút chính đỉnh 719, đáy 775, cách khe 20 (±0,5); ảnh 379 x 505,3 (±1) ở y 64, hàng nút ở y 593,3, cả hai bằng lúc không có khe (bật `withSlot` từ false sang true: ảnh và hàng nút đứng yên, nút chính dời lên đúng 96 ±0,5). Câu khẳng định: "ảnh giữ 379 x 505 khi đủ chỗ (bằng lúc không có khe), nút chính dời lên 96, chỉ co ở khung thấp". Số tính từ đỉnh màn của khung test (không có thanh trạng thái): thiết kế ghi ảnh y 88, hàng nút 617, nút chính 715, khe 795 |
| KB-9, tiêu chí 5: bốn nút bấm được | Compose UI test | `ReviewScreenTest.adBar_buttonsStillCallBack`: có khe, bấm bốn nút gọi đúng callback một lần mỗi nút |
| KB-9, tiêu chí 7: mọi trạng thái | Compose UI test | `ReviewScreenTest.adBar_savingAndSaveError_slotStays` (sau khi đặt mỗi trạng thái gọi `Snapshot.sendApplyNotifications()` rồi chờ, vì đồng hồ dừng nên `SAVING` không tự vẽ lại): `SAVING`, `SAVED` (như khi mở từ ô ảnh vừa chụp: ảnh đã lưu, nút chính tắt), `FAILED_STORAGE_FULL`, `PERMISSION_BLOCKED`: khe có, `SAVE_ERROR` (nếu có) và nút chính có `bottom <= slot.top`, nút chính cao 56 (±0,5). Số đo ở 411 x 891 có khe: `SAVED` như `NOT_SAVED`; `FAILED_STORAGE_FULL` ảnh 359,3 x 479, thông báo cao 48 |
| KB-9, tiêu chí 7: hộp thoại phủ lên thanh | Compose UI test | `ReviewScreenTest.adBar_discardDialog_coversSlot`: `pendingExit` khác null; vẽ `decorView` ra `Bitmap`, điểm giữa khe không còn là `Color.Red` thuần (kênh đỏ dưới 200, lớp mờ 50% phủ lên); đối chứng không có hộp thoại thì điểm đó đỏ (đỏ ≥ 240, xanh lá và xanh dương ≤ 15); bấm vào giữa khe gọi `onDismissDialog` một lần |
| KB-9, tiêu chí 7: trình đọc màn hình | Compose UI test | `ReviewScreenTest.adBar_discardDialog_hidesSlotFromAccessibility`: hộp thoại mở thì `onNodeWithText("Ad")` không có; đối chứng hộp thoại đóng thì có |
| KB-9, tiêu chí 7: khe là view Android | Compose UI test | `ReviewScreenTest.adBar_discardDialog_hidesAndroidViewSlot`: khe giả là `AndroidView` (một `TextView` chữ "AdViewText"); bật trình đọc màn hình của Robolectric (`shadowOf(AccessibilityManager).setEnabled(true)`, `setTouchExplorationEnabled(true)`), duyệt cây `AccessibilityNodeInfo` từ `AndroidComposeView.accessibilityNodeProvider` (nút gốc `-1`, con ảo theo `getChildNodeIds`): hộp thoại mở thì không có con là view thật (`ViewFactoryHolder` của khe); đối chứng hộp thoại đóng thì có đúng một. Cách đo đã chạy thử ở mục 4.1 |
| KB-9, tiêu chí 9: không quảng cáo thì không chừa chỗ | Compose UI test | `ReviewScreenTest.realAdBar_withoutAds_takesNoSpace`: `adBar` mặc định (không có `AdsController`): tag `bottom_ad_bar_slot` không có; `root.bottom - save.bottom` bằng 20 (±0,5); đối chứng thanh giả có khe thì bằng 116 |
| KB-9, tiêu chí 5: lỗi lưu ở khung chuẩn | Compose UI test | `ReviewScreenTest.adBar_permissionBlocked_photoShrinks_allAbove`: `PERMISSION_BLOCKED` và khe ở 411 x 891: ảnh co còn 344,3 x 459 (±1), giữ 0,75; thông báo lỗi (68) và nút chính trên khe |
| KB-9, tiêu chí 8: banner | Compose UI test | `ReviewScreenTest.adBar_banner_photoAndButtonsAbove`: thanh giả kiểu banner (`BottomAdBarLayout(reservation = 50.dp, slotMargin = 0.dp, slot = { Box(it.height(50.dp).testTag("fake_banner")) }, layer = null)`): banner sát đáy, rộng 411; ảnh, ba nút phụ và nút chính có `bottom <= banner.top`. Cách khung chọn banner khi `is_show_native_bottom` tắt và chừa đúng chiều cao banner đã có test: `BottomAdBarTest.layers_nativeBottomOff_bannerSlot_noLayer`, `reservation_bannerWithAd_isBannerHeight`, `nativeBottomSwitchedOff_nativeAdDestroyed_bannerSlotShown` |
| Màn hình thấp | Compose UI test, khung `vi-w360dp-h640dp-xxhdpi` | `ReviewScreenTest.small_adBar_photoShrinks_allVisible`: `NOT_SAVED` và khe: ảnh co từ 291 x 388 còn 219 x 292 (±1), giữ 0,75 (±0,01), nút chính đỉnh 468, đáy 524, khe đỉnh 544; `PERMISSION_BLOCKED` và khe: ảnh 156 x 208 (±1), thông báo, nút "Mở cài đặt", ba nút phụ và nút chính nằm trong màn và `bottom <= slot.top`; cùng test với `pt-rBR-w360dp-h640dp-xxhdpi` (`smallPortuguese_adBar_permissionBlocked_allVisible`: ảnh 131 x 174,7, thông báo cao 101,3, mọi nút trên khe) |
| KB-11, tiêu chí 3: gập mở | Compose UI test, khung `vi-w673dp-h841dp-xxhdpi` | `ReviewScreenUnfoldedTest.adBar_slotFullWidth_photoKept_saveAbove`: khe rộng 641 (±0,5), cách mép trái 16, đỉnh 745; ảnh 450 x 600 (±1), như khi không có khe; ba nút phụ và nút chính có `bottom <= slot.top` (nút chính đáy 725, dời lên 96 từ 821), nút chính ở cột phải (`left >= photo.right`) |
| KB-11: nằm ngang, đủ chỗ | Compose UI test | `ReviewScreenUnfoldedTest.landscape_adBar_saveInsideWindow`, khung `vi-w960dp-h520dp-land-xxhdpi`, `NOT_SAVED` có khe: nút chính 348 tới 404, cao 56 (±0,5), khe đỉnh 424; ảnh 255 x 340 (±1), giữ 0,75 |
| KB-9 tiêu chí 5, KB-11: nằm ngang, thiếu chỗ | Compose UI test | Hàm chung `assertCompactColumn(state, message, actionText, saveText, saveWidth, errorTop, messageLines)`: khe đỉnh 424; nút chính 348 tới 404, cao 56 (±0,5), rộng `saveWidth` (±1), đỉnh ≥ đáy thông báo + 15,5, đáy ≤ khe; hàng nút phụ nằm ngang 64 tới 136, ba nút cùng đỉnh 64; thông báo lỗi `errorTop` tới 332 (±1), chữ của thông báo `messageLines` dòng; nút hành động của thông báo tìm theo chữ `actionText` trong `SAVE_ERROR` (không dùng `hasClickAction().onFirst()`), cùng hàng chữ (`action.top < text.bottom`) và **bên phải chữ, không đè lên** (`action.left >= text.right`); nhãn nút chính không bị cắt: `GetTextLayoutResult` của nhãn có `didOverflowHeight` sai, một dòng, bề rộng dòng (`getLineRight(0) - getLineLeft(0)`) không quá bề rộng chữ (`size.width` + 0,5) và `isLineEllipsized(0)` sai (không dùng `didOverflowWidth`: nó đúng cả ở bố cục mặc định chưa đổi); bấm nút chính gọi `onSaveClick` một lần. `ReviewScreenUnfoldedTest.landscape_adBar_saveError_compactColumn`, khung `vi-w960dp-h520dp-land-xxhdpi`: `PERMISSION_BLOCKED` ("Mở cài đặt", rộng 137, thông báo 284, một dòng) rồi `FAILED_STORAGE_FULL` ("Thử lại", 284, một dòng). `landscape_adBar_saveError_compactColumn_portuguese`, khung `pt-rBR-w960dp-h520dp-land-xxhdpi`, `PERMISSION_BLOCKED` ("Abrir configurações", nút chính "Salvar foto" rộng 160,7): thông báo **264 tới 332, hai dòng** (Nhật chấp nhận ngày 2026-11-14), nút chính vẫn 348 tới 404. Đối chứng: trên code của nhánh chính nút chính bị ép còn 40; trên bố cục `fillMaxWidth()` của phiên bản 2 câu `action.left >= text.right` đỏ (nút đè chữ ở x khoảng 298) |
| Ranh giới ngưỡng của cột phải | Compose UI test | `ReviewScreenUnfoldedTest.landscape_adBar_thresholdBoundary`, khung `vi-w841dp-h593dp-land-xxhdpi` có khe (cột 413, ngưỡng có lỗi 420, không lỗi 264): `PERMISSION_BLOCKED` thì hàng nút phụ nằm ngang (ba nút cùng đỉnh 64, cao 72), nút chính 421 tới 477; `NOT_SAVED` thì hàng nút xếp dọc (64 tới 256, ba nút khác đỉnh), nút chính 421 tới 477. Không khẳng định số dòng của thông báo ở khung này (tiếng Việt hai dòng 337 tới 405 trên bố cục `Modifier`, mục 4.1) |
| Nút chính đủ cao ở mọi khung có khe | Compose UI test | Mỗi test bố cục có khe ở trên (điện thoại, khung thấp, pt-BR, gập mở, nằm ngang) thêm câu khẳng định nút chính cao 56 (±0,5) |
| Câu 19 ý a: bản staging | Unit test | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet` có `"is_show_review_bottom" to true` |
| Bảng khóa khớp code | Unit test có sẵn | `RemoteConfigKeysTest` qua sau khi sinh lại hai bảng |
| Giao diện màn hình có thanh | Screenshot test | `ReviewScreenScreenshotTest.ad_dark` ghi `src/test/screenshots/ReviewScreen_ad_dark.png`: `Ready(PATH, NOT_SAVED)`, `adBar` giả với `NativeAdCard(SCREENSHOT_AD, NativeAdSize.SMALL, it)`; `SCREENSHOT_AD` là `private` trong test của `feature/poses`, nên `ReviewScreenScreenshotTest` tự khai báo một `NativeAdAssets` giả cùng chữ (Headline of the ad, Advertiser or short body, Install, không icon, không media); so với frame `ReviewScreen_ad_dark` đã sửa ở PR #337 (ảnh 379 x 505) |
| Màn hình thấp | Screenshot test khung `vi-w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `ReviewScreenScreenshotTest.ad_small`: `src/test/screenshots/ReviewScreen_ad_small.png`, `PERMISSION_BLOCKED` (thông báo và nút "Mở cài đặt") và thanh |
| Đúng thiết kế | Screenshot test cùng tên ảnh thiết kế, `scripts/design-compare.sh camera`, rà soát độc lập theo mục 7 của gói thiết kế | `ReviewScreen_ad_dark.png`; `ReviewScreenUnfoldedScreenshotTest.ad_dark_unfolded`: `src/test/screenshots/ReviewScreen_ad_dark_unfolded.png`, khung `vi-w673dp-h841dp-xxhdpi` |
| Chữ đúng bảng chữ của thiết kế | Compose UI test có sẵn | `ReviewScreenTest.default_showsTexts` giữ nguyên; chữ của thẻ quảng cáo là chữ giả của khung, không thuộc bảng chữ |
| KB-9, tiêu chí 5, 7 trên bản staging | Kiểm tra thủ công (máy thật: quảng cáo thật) | Đồng ý quảng cáo, chụp một ảnh: khe native thử 80 dp ở đáy màn xem lại, dưới nút "Lưu vào thư viện", ảnh không bị che; bấm Lưu, Chia sẻ, Chụp lại, Tư thế tiếp: đúng hành vi, không bấm vào quảng cáo; hộp "Bỏ ảnh này?" phủ lên khe; log `VGL.Ads` có `Bottom bar review_bottom: slot=NATIVE collapsible=false` (`bottomBarLayersLog`, ghi khi lớp đổi) |
| KB-9, tiêu chí 6 trên bản staging | Kiểm tra thủ công (máy thật: Remote Config) | Đặt `is_show_native_collap = true` trên Firebase staging, mở lại app: thư viện tư thế có lớp nổi, màn xem lại không có; log `Bottom bar review_bottom: slot=NATIVE collapsible=false`; trả khóa về như cũ |
| KB-9, tiêu chí 7: TalkBack | Kiểm tra thủ công (máy thật: TalkBack) | Bật TalkBack, mở hộp "Bỏ ảnh này?": vuốt qua các phần tử chỉ đọc hộp thoại, không đọc thẻ quảng cáo (khe thật là view của SDK, test Robolectric chỉ kiểm khe giả) |
| KB-11 trên máy gập | Kiểm tra thủ công (máy thật: gập có bản lề) | Mở gập, chụp: khe trải hết bề rộng đáy, ảnh và nút "Lưu ảnh" ở trên khe |
| Đề xuất về dự án mẫu | Kiểm tra thủ công (người: mở issue ở repo khác) | Người mở issue `from-app` cho thuộc tính `collapsibleLayer` (mục 3.1, bước 6) |
| Kế hoạch quảng cáo trên Drive | Kiểm tra thủ công (người: file trên Drive, ngoài repo) | Sau khi merge, người chạy `scripts/ads-plan.py --keep <file trên Drive>`, xem dòng `review_bottom` có trong file Excel mới, rồi thay file trên Drive (`docs/ops/ads-plan.md`) |
| KB-6 không đổi: chụp, lưu, chia sẻ, chụp lại ở màn xem lại | Unit test, test giao diện (có sẵn) | Test có sẵn của `ReviewViewModelTest` (ví dụ `save_success_logsEventAndRecords`, `save_showsSavingWhileWriting`), `ReviewRouteTest`, `ReviewScreenTest`, `ReviewScreenUnfoldedTest` qua với câu khẳng định nguyên (mục 3.5); màn có thanh giả vẫn bấm được bốn nút (test `adBar_*` ở trên) |

**Ngoại lệ viết test trước:**

- Dòng `REVIEW_BOTTOM` của `adBar` mặc định trong `ReviewScreen`: không có `AdsController` thì test không thấy vị trí nào được dùng; rà soát độc lập kiểm, và dòng log ở bước máy thật.
- Sinh lại `docs/remote-config-keys.md`, `docs/remote-config-template.json`: sinh từ code; `RemoteConfigKeysTest` kiểm khớp.
- `docs/ops/manual-test.md`, `docs/template-sync.txt` và ảnh so sánh của `design/review/`: tài liệu.
- Ba ảnh chuẩn mới: ghi ở bước 4 sau khi code xong (ảnh chuẩn không thất bại trước được).

### 4.1. Chạy thử

Tiêu chí E dưới 2, không bắt buộc `check.sh`. Hai điểm đã chạy thử bằng một test tạm không commit (`feature/camera/src/test/.../review/TmpT094MeasureTest.kt`, xóa sau khi đo), lệnh `./gradlew :feature:camera:testDebugUnitTest --tests '*TmpT094MeasureTest*'`, 2026-11-13, nhánh chính `8af6a3e`. Lần đo thứ ba (sau rà soát lại lần 2) áp thay đổi tạm của cột phải ở `ReviewUnfolded.kt` đúng như mục 3.1 (hàng nút ngang khi thiếu chỗ), không commit, trả về bằng `git checkout -- <tệp>`. Lệnh đo như trên; lệnh kiểm ảnh chuẩn: `./gradlew :feature:camera:verifyRoborazziDebug :feature:camera:testDebugUnitTest --tests '*ReviewScreenUnfoldedScreenshotTest*' --tests '*ReviewScreenScreenshotTest*' --tests '*ReviewScreenSavingScreenshotTest*' --tests '*ReviewScreenUnfoldedTest*' --tests '*ReviewScreenTest*' --rerun` (`roborazzi.test.verify=true`): BUILD SUCCESSFUL; `ReviewScreenScreenshotTest` 5 test, `ReviewScreenSavingScreenshotTest` 1, `ReviewScreenUnfoldedScreenshotTest` 2, `ReviewScreenTest` 16, `ReviewScreenUnfoldedTest` 9, không lỗi. Lần đo thứ hai (cột cuộn) bị bỏ: người rà thấy nó cắt bóng của nút chính. Test tạm bọc `ReviewScreen` hiện tại trong `BottomAdBarLayout` (khe giả 80 dp, phần đệm 96 áp cho cả màn), tức đúng bố cục của mục 3.1, bước 2. Tọa độ tính từ đỉnh màn của khung test; khung không có thanh trạng thái nên mọi thứ cao hơn thiết kế 24 (ảnh y 64 ứng với 88 của thiết kế). Nút chính neo đáy nên không lệch 24; đỉnh 719 so với 715 của thiết kế chênh 4 vì `PrimaryButton` cao 56, thiết kế vẽ 60 (lệch có từ frame `default`).

**Bố cục (số đo thật):**

| Khung, trạng thái | Ảnh (x, y, rộng x cao) | Hàng nút (đỉnh, đáy) | Thông báo lỗi | Nút chính (đỉnh, đáy) | Khe (đỉnh) |
| --- | --- | --- | --- | --- | --- |
| 411 x 891, `NOT_SAVED`, không khe | 16, 64, 379 x 505,3 | 593,3, 665,3 | | 815, 871 | |
| 411 x 891, `NOT_SAVED`, có khe | 16, 64, 379 x 505,3 | 593,3, 665,3 | | 719, 775 | 795 |
| 411 x 891, `PERMISSION_BLOCKED`, có khe | 33,3, 64, 344,3 x 459 | 547, 619 | 635 tới 703 | 719, 775 | 795 |
| 360 x 640, `NOT_SAVED`, không khe | 34,7, 64, 291 x 388 | 476, 548 | | 564, 620 | |
| 360 x 640, `NOT_SAVED`, có khe | 70,7, 64, 219 x 292 | 380, 452 | | 468, 524 | 544 |
| 360 x 640, `FAILED_STORAGE_FULL`, có khe | 102, 64, 156 x 208 | 296, 368 | 384 tới 452 | 468, 524 | 544 |
| 360 x 640, `PERMISSION_BLOCKED`, có khe | 102, 64, 156 x 208 | 296, 368 | 384 tới 452 | 468, 524 | 544 |
| 360 x 640 pt-BR, `PERMISSION_BLOCKED`, có khe | 114,7, 64, 131 x 174,7 | 262,7, 334,7 | 350,7 tới 452 | 468, 524 | 544 |
| 673 x 841, `NOT_SAVED`, không khe | 16, 64, 450 x 600 | cột phải, đáy 256 | | 765, 821 | |
| 673 x 841, `NOT_SAVED`, có khe | 16, 64, 450 x 600 | cột phải, đáy 256 | | 669, 725 | 745 (rộng 641) |
| 960 x 520 ngang, `NOT_SAVED`, có khe | 16, 64, 255 x 340 | cột phải, đáy 256 | | 348, 404 | 424 (rộng 928) |
| 960 x 520 ngang, `PERMISSION_BLOCKED`, có khe (code hiện tại) | 16, 64, 255 x 340 | cột phải, đáy 256 | 256 tới 348 | 364, 404: **bị ép còn 40** | 424 |
| 960 x 520 ngang, `PERMISSION_BLOCKED`, có khe, cột gọn (lần đo 3) | 16, 64, 255 x 340 | ngang: 64, 136 (rộng 331) | 284 tới 332 (một dòng) | 348, 404 (cao 56) | 424 |
| 960 x 520 ngang, `FAILED_STORAGE_FULL`, có khe, cột gọn | 16, 64, 255 x 340 | ngang: 64, 136 | 284 tới 332 | 348, 404 | 424 |
| 960 x 520 ngang pt-BR, `PERMISSION_BLOCKED`, có khe, cột gọn | 16, 64, 255 x 340 | ngang: 64, 136 | 284 tới 332 | 348, 404 (rộng 160,7, cao 56) | 424 |
| 960 x 520 ngang, `NOT_SAVED`, có khe, lần đo 3 | 16, 64, 255 x 340 | dọc: 64, 256 (như cũ) | | 348, 404 | 424 |
| 960 x 520 ngang, `PERMISSION_BLOCKED`, không khe, lần đo 3 | 16, 64, 327 x 436 | dọc: 64, 256 (như cũ) | 336 tới 428 | 444, 500 | |
| 841 x 593 ngang, `PERMISSION_BLOCKED`, có khe, lần đo 3 | 16, 64, 309,7 x 413 | ngang: 64, 136 (cột 413 < ngưỡng 420) | 357 tới 405 | 421, 477 | 497 |
| 841 x 593 ngang, `NOT_SAVED`, có khe, lần đo 3 | 16, 64, 309,7 x 413 | dọc: 64, 256 | | 421, 477 | 497 |
| 673 x 841, `PERMISSION_BLOCKED`, có khe và không khe, lần đo 3 | 16, 64, 450 x 600 | dọc: 64, 256 (như cũ) | 521 tới 653 và 617 tới 749 | 669, 725 và 765, 821 | 745 |
| 673 x 841, `NOT_SAVED`, có khe, lần đo 3 | 16, 64, 450 x 600 | dọc: 64, 256 | | 669, 725 | 745 |
| 411 x 891, `SAVED`, có khe | 16, 64, 379 x 505,3 | 593,3, 665,3 | | 719, 775 | 795 |
| 411 x 891, `FAILED_STORAGE_FULL`, có khe | 26, 64, 359,3 x 479 | 567, 639 | 655 tới 703 | 719, 775 | 795 |

Kết luận:
- Ở 411 x 891 ảnh không co (giới hạn bởi bề rộng 379), hàng nút đứng yên, chỉ nút chính dời lên 96. Biên trước khi ảnh bắt đầu co: cột chứa ảnh cao 655 (64 tới 719), ảnh, hai khoảng và hàng nút cần 617,3, còn 37,7 trong khung test. Trên máy có thanh trạng thái 24 dp, biên còn khoảng 13,7; người rà tính từ thiết kế khoảng 9,7. Máy có thanh trạng thái hay thanh điều hướng cao hơn thì ảnh co sớm, vẫn giữ 3:4.
- Thông báo lỗi ở khung test cao 68 (người rà ước 72 từ thiết kế): ảnh co còn 344,3 x 459 ngay ở khung chuẩn (người rà ước khoảng 332 x 443 khi có thanh trạng thái).
- Khung 360 x 640 có khe: nút chính đỉnh 468, ảnh 219 x 292 trong khung test; có thanh trạng thái thì nút chính khoảng 464 và ảnh nhỏ hơn (người rà ước khoảng 198 x 264). Câu khẳng định của test dùng số đo của khung test, không dùng số ước.
- Mọi nút ở trên khe ở mọi khung đã đo. Ở code hiện tại, nằm ngang 960 x 520 có khe và có thông báo lỗi ép nút chính còn 40 dp; với cột phải gọn lại (hàng nút ngang), nút chính giữ 56 và nằm trọn ở mọi khung, không cuộn, không cắt bóng; các khung đủ chỗ không đổi một điểm nào.

**Lần đo thứ tư (phiên bản 3, 2026-11-14):** trên commit `7afc6e9` của nhánh `claude/T-094-thanh-day-xem-lai` (chế độ gọn dùng `Modifier` cho thông báo), test tạm không commit dựng `ReviewScreen(adBar = fakeAdBar())`, lệnh `./gradlew :feature:camera:testDebugUnitTest --tests '*TmpT094MeasureTest*'`, rồi xóa test tạm. Đo bằng `getUnclippedBoundsInRoot` và `GetTextLayoutResult`:

| Khung, trạng thái (có khe) | Thông báo lỗi (trái, đỉnh tới phải, đáy) | Chữ thông báo (trái tới phải; dòng) | Nút hành động (trái tới phải; đỉnh tới đáy) | Nút chính (đỉnh tới đáy; rộng) | Hàng nút phụ |
| --- | --- | --- | --- | --- | --- |
| 960 x 520 vi, `PERMISSION_BLOCKED` | 287, 284 tới 911, 332 | 303 tới 669,7; 1 dòng | 821,3 tới 891; 298 tới 318 | 348 tới 404; 137 | 64 tới 136 |
| 960 x 520 vi, `FAILED_STORAGE_FULL` | 287, 284 tới 911, 332 | 303 tới 569; 1 dòng | 847 tới 891; 298 tới 318 | 348 tới 404; 137 | 64 tới 136 |
| 960 x 520 pt-BR, `PERMISSION_BLOCKED` | 287, 264 tới 911, 332 | 303 tới 739,3; 2 dòng | 759,3 tới 891; 288 tới 308 | 348 tới 404; 160,7 | 64 tới 136 |
| 841 x 593 vi, `PERMISSION_BLOCKED` | 341,7, 337 tới 825, 405 | 357,7 tới 715,3; 2 dòng | 735,3 tới 805; 361 tới 381 | 421 tới 477; 137 | 64 tới 136 |

Mọi trường hợp: nút hành động bên phải chữ (`action.left >= text.right`), nút chính cao 56 và trên khe. Thông báo rộng 624 ở 960 x 520 (giới hạn 600 dp của Snackbar cộng lề), nên chữ pt-BR dài hơn xuống hai dòng. Số của lần đo 3 ở trên (thông báo 284 tới 332 cho cả pt-BR, 357 tới 405 ở 841 x 593) đo trên bố cục `fillMaxWidth()` có nút đè chữ, không dùng nữa.

**Trợ năng của khe là view Android (dời `clearAndSetSemantics` lên `barModifier`):** test tạm dựng `BottomAdBarLayout` có khe là `AndroidView` (`TextView` "AdViewText"), nội dung `Text("ContentText")`, và `Text("DialogText")` vẽ sau; bật `AccessibilityManager` của Robolectric, duyệt cây từ `AndroidComposeView.accessibilityNodeProvider` (nút `-1`, con theo `getChildNodeIds` qua phản chiếu).
- Thanh có `clearAndSetSemantics {}`: chữ đọc được chỉ có `DialogText`, không có con là view thật.
- Đối chứng, thanh không xóa: chữ `ContentText`, `DialogText` và một con view thật (`ViewFactoryHolder` bọc `TextView` của khe).
- `TextView` vẫn `isImportantForAccessibility = true` trong cây view, nhưng `AndroidComposeView` có `AccessibilityNodeProvider`, nên dịch vụ trợ năng chỉ thấy con mà provider khai; xóa ngữ nghĩa của tổ tiên làm provider không khai holder của khe nữa.

Vì vậy không cần phương án dự phòng; test `adBar_discardDialog_hidesAndroidViewSlot` dùng cùng cách đo. Bước TalkBack trên máy thật vẫn giữ (view của SDK quảng cáo thật).

**Cách đọc dòng `AdPlacement`:** phiên không chạy `scripts/release.sh` (`CLAUDE.md`, mục Không được làm). `AdPlacementSourceTest.releaseScript_readsLinesWithAttributes` (dòng mẫu `collapsibleLayer = false`, mục 4) và `releaseScript_readsEveryShippedPlacement` kiểm cách đọc qua đường test có sẵn.

`scripts/existing-tests.py docs/features/ads/plans/T-094-thanh-day-native-o-man-xem-lai.md` (2026-11-13, nhánh chính `8af6a3e`): đối chiếu ở mục 3.5.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**

- điểm 1: thêm vị trí quảng cáo `review_bottom`;
- điểm 7: module core `:core:ads` (thuộc tính khai báo mới, cách chọn lớp của thanh đáy).

Mức Cao, cần duyệt chéo. Người phụ trách sản phẩm (Nhật) đã chốt vị trí và ý e ở câu 19 của spec ads ngày 2026-11-13, thiết kế đã duyệt cùng ngày.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module (`:core:ads`, `:feature:camera`, `:app`), sửa khung thanh đáy dùng chung và màn chính sau mỗi lần chụp |
| B. Độ rõ của yêu cầu | 1 | Spec và thiết kế đã chốt; còn câu hỏi ở mục 8 về tên thuộc tính và việc tách ticket |
| C. Khả năng kiểm chứng tự động | 1 | Khai báo, lớp, kho (cả đường nối với controller thật), bố cục, hộp thoại phủ lên, trợ năng của khe là view Android có test; quảng cáo thật, TalkBack trên view của SDK chỉ kiểm trên máy |
| D. Ảnh hưởng chéo | 2 | `AdPlacement.kt`, `StagingRemoteValuesModule`, bảng khóa dùng chung với T-093; `ReviewScreen.kt` dùng chung với T-096; `manual-test.md` dùng chung với T-093, T-097; sửa `:core:ads` của mẫu, đợt đồng bộ có thể lấy mất (`docs/template-sync.txt`) |
| E. Độ mới của kỹ thuật | 1 | Lần đầu một vị trí tắt lớp nổi, nhưng theo đúng đường của thuộc tính khai báo có sẵn (`firstOpenOnly`) |
| **Tổng** | 8 | **Mức: Cao (chạm điểm chặn)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdPlacement.kt`, `AdUnitIds.kt`, `BottomAdBar.kt` của `:core:ads` (mọi thanh đáy); `StagingRemoteValuesModule` và test; hai bảng khóa; `docs/ops/manual-test.md`; `docs/template-sync.txt`. Thanh đáy của thư viện, chi tiết tư thế và Cài đặt giữ hành vi cũ vì thuộc tính của khai báo mặc định đúng.
- Rủi ro đồng bộ: ba file `:core:ads` là của dự án mẫu. Một đợt đồng bộ lấy bản mẫu của `:core:ads` trước khi mẫu nhận thuộc tính sẽ làm mất `collapsibleLayer`, và màn xem lại có lại lớp nổi khi `is_show_native_collap` bật. Giảm rủi ro: ghi vào đoạn "core/ads keeps parts of this app..." của `docs/template-sync.txt` (người chạy đồng bộ đọc đoạn này), test `reviewBottom_hasNoCollapsibleLayer` và hai test controller thật đỏ khi mất, và mở issue `from-app` trong một tuần sau khi merge.
- Thiết kế: frame `ReviewScreen_ad_dark` sửa theo quyết định của Nhật (2026-11-13, giữ bố cục của code) ở PR thiết kế #337 (nhánh `design/review-ad-giu-anh`). PR đó phải merge trước khi task chạy, để `design-compare.sh` so với frame đã sửa; chưa merge thì ảnh so sánh lệch ở vùng ảnh và hàng nút.
- Task khác có thể xung đột:
  - T-093 (`settings_bottom`): sửa cùng `AdPlacement.kt`, `AdPlacementAppTest`, `AdUnitIdsTest`, `AdsControllerUnitIdsTest`, `StagingRemoteValuesModule` và test, hai bảng khóa, `manual-test.md`. Ticket T-094 ghi `depends_on: [T-093]`.
  - T-096 (hỏi đánh giá app tính cả ảnh vừa lưu): sửa `ReviewScreen.kt` (phần `ReviewRoute`, số ảnh đưa cho lời hỏi đánh giá) và có thể `ReviewViewModel.kt`. Plan này chỉ sửa phần `ReviewScreen` không trạng thái; hai phần không chồng dòng, nhưng cùng file.
  - T-097 (camera chờ quảng cáo toàn màn hình đóng): sửa `CameraScreen.kt` và cũng sửa `docs/ops/manual-test.md`, mục Quảng cáo.
  - `docs/ops/manual-test.md`, mục Quảng cáo: ba plan T-093, T-094, T-097 cùng thêm dòng. Mỗi plan thêm bước của mình, không sửa dòng của plan khác; dòng khóa "không đặt `false` trên Firebase staging" do T-093 thêm `is_show_settings_bottom` trước, T-094 thêm `is_show_review_bottom` sau.
- Đề xuất thứ tự chạy: task độc lập trước, chuỗi phụ thuộc sau: T-096, T-097, rồi T-093, T-094 (T-094 cuối, sau T-093 theo `depends_on`, và sau T-096 vì cùng `ReviewScreen.kt`). Một phiên làm lần lượt nên các file chung không xung đột; mỗi task làm trên nhánh chính đã có task trước. Nếu T-093 dừng thì T-094 bị bỏ qua theo `depends_on`.

## 7. Điều kiện dừng riêng

- Test có sẵn của `review_interstitial`, luật đếm ảnh (`InterstitialBreaks`) hay hỏi đánh giá đỏ: dừng, vì plan không được đổi các phần đó (ticket, mục Phạm vi).
- Ảnh chuẩn có sẵn của `feature/camera` hay `feature/poses` đổi quá nhiễu vẽ (lệch hơn 1 mỗi kênh màu) sau khi sửa khung: dừng, vì plan giữ bố cục cũ khi không có quảng cáo.

## 8. Câu hỏi cho người duyệt

Các câu 1 tới 8 của phiên bản 1 đã được trả lời (mục 9), giữ nguyên. Hai việc của phiên bản 3 Nhật đã chọn ngày 2026-11-14 (chấp nhận pt-BR hai dòng ở chế độ gọn; chấp nhận ba thay đổi test sau commit test). Câu mới:

1. **Thông báo lỗi hai dòng ở 841 x 593 có khe.** Trên bố cục `Modifier`, tiếng Việt `PERMISSION_BLOCKED` ở 841 x 593 xuống hai dòng (337 tới 405), nút chính vẫn 421 tới 477, nút hành động bên phải chữ. Plan không khẳng định số dòng ở khung này, chỉ khẳng định hàng nút và nút chính (`landscape_adBar_thresholdBoundary`). AI đề xuất giữ như vậy. Đồng ý?

## 9. Quyết định duyệt

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm hiện chỉ có một dev, ngoại lệ của `risk-scoring.md` mục 3)
- **Ghi chú:** Nhật duyệt trong phiên có người theo dõi ngày 2026-11-13, lời của Nhật: "duyệt", sau khi AI tóm tắt plan và nói câu hỏi mục 8 theo đề xuất của AI. AI ghi thay. Trả lời câu hỏi ở mục 8:
  1. Thuộc tính `collapsibleLayer = false` đặt trên dòng `AdPlacement` (khai báo của vị trí); plan không làm MC-13.
  2. Giữ một ticket, không tách phần khung ra ticket riêng.
  3. Không chép cách bật dần 10% vào `docs/ops/remote-config.md` hay ghi chú phát hành; người phát hành bật dần trên Firebase production theo spec ads, câu 19, ý d.
  4. Giữ hành vi chung của khung (D-04): nút chính nhảy 96 khi khe có hay mất quảng cáo, như thư viện và chi tiết tư thế; giữ chỗ sau lần có quảng cáo đầu là thay đổi khung riêng, cần ticket và đề xuất về mẫu riêng.
  5. Khoảng cách nút chính và khe khi mở gập: 20 dp theo code, đã thống nhất với gói thiết kế (PR #337).
  6. Không viết ADR mới cho chỗ lệch D-01; quyết định ở spec ads câu 19 ý e, chỗ lệch với mẫu ở `docs/template-sync.txt`, issue `from-app`. Viết ADR của app nếu mẫu từ chối issue.
  7. Câu "ảnh co lại cho vừa phần còn lại" của spec camera (KB-9, tiêu chí 5) hiểu là ảnh co khi thiếu chỗ; giữ câu chữ của spec, không sửa spec trong plan này.
  8. Cách (a): cột phải gọn lại khi thiếu chỗ theo ngưỡng của mục 3.1 (hàng nút phụ nằm ngang, thông báo lỗi để nút cùng dòng khi có lỗi); chấp nhận hàng nút đổi bố cục theo trạng thái lưu ở cửa sổ nằm ngang thấp có khe.
- **Phiên bản 2 (2026-11-13):** Chỉ thêm một dòng ở mục 4 cho KB-6 (dẫn test có sẵn), vì `scripts/run-check.py` đòi mọi kịch bản của ticket có dòng ở mục 4; không đổi cách làm hay test viết trước. Nhật duyệt phiên bản 2 trong phiên có người theo dõi ngày 2026-11-13 (chọn "Plan bản 2, duyệt lại" khi được hỏi), giữ nguyên câu trả lời mục 8 của phiên bản 1. AI ghi thay.
- **Phiên bản 3:**
  - **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
  - **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
  - **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). -->

### RUN-20261113-02, phiên bản plan 2

- **Kết quả:** dừng ở bước 4, điều kiện dừng 8 của `risk-scoring.md` mục 4 (muốn qua phải đổi điều một test viết trước kiểm).
- **Nhánh, PR:** `claude/T-094-thanh-day-xem-lai` (đã đẩy, chưa có PR). Commit `d922a38` (test viết trước), `9bb26d7` (chi tiết test), `7afc6e9` (code dở, chưa chạy `check.sh`).
- **Đã làm:** `:core:ads` theo mục 3.1 (thuộc tính `collapsibleLayer` có `require`, dòng `REVIEW_BOTTOM`, `layerFormats`, ba hàm của `BottomAdBar.kt`; mọi test `:core:ads` qua); `ReviewScreen` nhận `adBar`, hộp thoại vẽ sau thanh; `ReviewUnfolded` dùng `BoxWithConstraints` với ngưỡng của plan; staging bật `REVIEW_BOTTOM`. Chưa làm: sinh lại bảng khóa (`RemoteConfigKeysTest` còn đỏ), ba ảnh chuẩn mới, `manual-test.md`, `template-sync.txt`, `design/review/`, `check.sh`, `design-compare.sh`.
- **Test viết trước:** 26 test (commit `d922a38`): 8 mới ở `:core:ads` (cùng dòng `R_B` của `AdPlacementSourceTest`, dữ liệu `review_bottom=unit/17`), 11 `ReviewScreenTest`, 5 `ReviewScreenUnfoldedTest`, 3 screenshot. Trên khung chỉ có API: 26 đỏ đúng lý do (bỏ qua `collapsibleLayer`, thiếu node `fake_ad_slot`, dữ liệu staging, bảng khóa). Thêm 2 test đối chứng `POSES_BOTTOM` mà plan nêu trong cùng dòng (`posesBottom_collapOn_layerAndCollapsibleRequest`, `posesBottom_collapSwitchedOnWhileShown_layerLoads`) vì một test không gọi `setContent` hai lần.
- **Lý do dừng:** `ReviewScreenUnfoldedTest.landscape_adBar_saveError_compactColumn_portuguese` (khung `pt-rBR-w960dp-h520dp-land-xxhdpi`, `PERMISSION_BLOCKED`, có khe) mong thông báo lỗi một dòng ở 284 tới 332 (mục 4). Thật: `error top: expected 284.0 dp, got 264.0 dp`, thông báo xuống hai dòng (264 tới 332). Số của mục 4.1 (lần đo 3) đo trên bố cục `SaveErrorMessage(Modifier.fillMaxWidth(), actionOnNewLine = false)`, trong đó Snackbar một hàng của Material cho chữ cả bề rộng và vẽ nút hành động đè lên đầu chữ ("Mở cài đặt", "Thử lại" đè chữ ở x khoảng 298 dp; test bắt được bằng `action.left >= text.right`). Bỏ `fillMaxWidth` ở chế độ gọn thì vẽ đúng (nút ở bên phải như điện thoại), nhưng Snackbar giới hạn rộng 600 dp: tiếng Việt vẫn một dòng (test qua), pt-BR cần hai dòng. Nút chính vẫn 348 tới 404, cao 56, trên khe.
- **Thay đổi ở file test sau commit test (commit `9bb26d7`):** (1) `ReviewScreenTest.adBar_savingAndSaveError_slotStays` thêm `Snapshot.sendApplyNotifications()` sau khi đặt trạng thái (đồng hồ dừng nên `SAVING` không vẽ lại); trước: không thấy "Đang lưu…", sau: qua. (2) `assertCompactColumn`: `didOverflowWidth` của `GetTextLayoutResult` đúng cả ở bố cục mở gập mặc định chưa đổi (673 x 841, không khe, `NOT_SAVED`), nên không bắt được chữ bị cắt; thay bằng "bề rộng dòng không quá bề rộng chữ, không bị cắt ba chấm", giữ kiểm một dòng và chiều cao. Thay đổi này đổi cách kiểm ý "không bị cắt", người cần xác nhận. (3) nút hành động của thông báo tìm theo chữ (`actionText`) thay `hasClickAction().onFirst()`.
- **Lệch so với plan:** ở chế độ gọn, thông báo lỗi dùng `Modifier` thay `fillMaxWidth()` (sửa chuyện nút đè chữ). Không có file ngoài mục 3.2. Test cũ đổi theo mục 3.5: hàm `Bar` và bốn lời gọi năm đối số của `bottomAdBarLayers` trong `BottomAdBarTest` thêm `collapsibleLayer = true`.
- **Cần người quyết (plan phiên bản 3):** (a) chấp nhận pt-BR hai dòng ở chế độ gọn, viết lại số của test pt-BR (thông báo 264 tới 332, hai dòng; nút chính giữ 348 tới 404) — đề xuất của AI, vì nút chính và bốn nút vẫn trọn, chỉ thông báo cao hơn; (b) dùng `actionOnNewLine` ở chế độ gọn cho ngôn ngữ dài (phải đo lại, có thể lại thiếu chỗ); (c) viết hàng báo lỗi riêng thay Snackbar (sửa `ReviewComponents.kt`, ngoài mục 3.2). Cùng lúc xác nhận thay đổi (2) ở trên về cách kiểm "không bị cắt". Plan phiên bản 3 tiếp tục trên nhánh `claude/T-094-thanh-day-xem-lai`, chỉ đổi mục 4 (số của test pt-BR, cách kiểm không bị cắt) và mục 3.1 (bỏ `fillMaxWidth` ở chế độ gọn); bước 3 chỉ áp cho test đổi.
