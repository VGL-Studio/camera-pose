---
ticket: T-024
title: Thư viện và chi tiết tư thế trên máy gập
status: done             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 7                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: medium           # low | medium | high
token_estimate: 20M          # cỡ S, số gợi ý 5.0M (tạm, 2 task); nâng lên vì sửa hai màn và 4 ảnh chuẩn, mục 8 câu 5
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-024: THƯ VIỆN VÀ CHI TIẾT TƯ THẾ TRÊN MÁY GẬP

## 1. Tóm tắt

Khi màn hình rộng từ 600 dp (máy gập mở ra), thư viện hiện lưới 3 cột và chi tiết chia hai cột: ảnh lớn bên trái, hướng dẫn bên phải, thanh nút "Chụp với tư thế này" ở đáy cột phải. Bố cục chọn theo chiều rộng thật của màn (`BoxWithConstraints`, đã dùng ở `:feature:loading`, `:feature:onboarding`), không thêm thư viện, chỉ sửa `:feature:poses`. Chip, vị trí cuộn và tư thế đang xem giữ khi gập mở vì đã nằm trong ViewModel, `SavedStateHandle` và trạng thái lưu của màn (T-021, T-022). Kiểm chứng bằng test giao diện Robolectric ở khung `w673dp-h841dp-xxhdpi` và khi đổi chiều rộng, 4 screenshot test cùng tên ảnh `_unfolded`, kiểm tra thủ công trên máy gập hoặc emulator gập.

## 2. Hiểu yêu cầu

Ticket xử lý KB-7 trong `features/poses/spec.md`.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-7 | Máy gập mở ra thì thư viện hiện 3 cột | `PosesScreen` đo chiều rộng của vùng nội dung: từ 600 dp là bố cục rộng, `GridCells.Fixed(3)`; dưới 600 dp giữ 2 cột. Ngưỡng 600 dp là ranh giới cỡ cửa sổ "compact" của Material; điện thoại (360 đến 430 dp) luôn dưới, máy gập mở ra (khoảng 673 dp) luôn trên; app khóa hướng dọc nên điện thoại xoay ngang không xảy ra (`android:screenOrientation="portrait"`). Thẻ rộng theo ô lưới, cao theo tỷ lệ ảnh của `PoseCard` (README mục 6: "chiều cao thẻ theo tỷ lệ ảnh thật") |
| KB-7 | Chi tiết chia hai cột: ảnh trái, hướng dẫn phải, nút ở đáy cột phải | Bố cục rộng của `PoseDetailScreen`: thanh trên rộng hết; dưới là `Row`: cột trái là vùng ảnh cao 440, cột phải gồm vùng cuộn (dòng phụ, các bước, lỗi hay gặp) và thanh nút đáy (đường kẻ và nút) chỉ trong cột phải. Chiều rộng hai cột theo tỷ lệ thiết kế 315 : 326 (cột phải gồm lề phải 16), nên ở 673 dp đúng 315 và 310 như thiết kế |
| KB-7 | Đúng thiết kế `_unfolded` | Screenshot test cùng tên ở khung `w673dp-h841dp-xxhdpi` (ADR-0020) |
| KB-7 | Gập lại thì dùng bố cục điện thoại, giữ chip, vị trí cuộn và tư thế đang xem | Gập mở đổi cỡ màn hình nên activity được dựng lại (manifest không khai `configChanges`). Chip nằm trong `SavedStateHandle` của `PosesViewModel`; vị trí cuộn là `rememberLazyGridState` dựng ở `PosesRoute`, ngoài nhánh bố cục, nên cùng một trạng thái cho 2 và 3 cột; tư thế đang xem là tham số của đích chi tiết. Chi tiết dùng một `ScrollState` chung cho hai bố cục (mục 8 câu 2) |

Thiết kế tham chiếu: `features/poses/design/README.md` (mục 3, đoạn "Máy gập mở ra"), các ảnh trong `design/screens/`: `PosesScreen_default_light_unfolded`, `PosesScreen_default_dark_unfolded`, `PoseDetailScreen_default_light_unfolded`, `PoseDetailScreen_default_dark_unfolded`; số liệu trong `design/context/` cùng tên (thư viện: thẻ 203 rộng, 3 cột cách nhau 16; chi tiết: ảnh 315 x 440 tại 16, 80; cột phải 310 rộng tại 347; thanh nút 326 x 96 tại 347, 745).

ADR liên quan: ADR-0020 (khung gập 673 x 841, tên `_unfolded`, ảnh so sánh), ADR-0008 (khoảng cách từ `Spacing`).

## 3. Giải pháp

### 3.1. Cách làm

Plan viết trên code sau T-022 (tạo màn chi tiết) và T-023 (thêm `bottomBanner`).

**Ngưỡng.** `PosesComponents.kt`: `internal val WideLayoutMinWidth = 600.dp` và `@Composable internal fun BoxWithConstraintsScope.isWide(): Boolean = maxWidth >= WideLayoutMinWidth`. Giữ trong `:feature:poses`, không tạo file dùng chung trong module core (mục 8 câu 1).

**Thư viện (`PosesScreen.kt`).**

- `ReadyContent` và `LoadingGrid` đặt lưới trong `BoxWithConstraints(Modifier.weight(1f))`; `columns = GridCells.Fixed(if (isWide()) 3 else 2)`. Khung xương lúc tải: 6 ô ở cả hai bố cục (2 hàng khi rộng).
- `gridState` vẫn dựng ở `PosesRoute` và truyền xuống; lưới có `key = { it.id }` nên khi đổi số cột, mục đầu đang thấy giữ nguyên.
- Dòng tổng số, hàng chip, thông báo lỗi và "Đã lưu" rỗng, `bottomBanner` (T-023) giữ như điện thoại; banner rộng hết đáy màn.

**Chi tiết (`detail/PoseDetailScreen.kt`, `detail/PoseDetailComponents.kt`).**

- `PoseDetailScreen` dựng `val scrollState = rememberScrollState()` một lần, rồi `BoxWithConstraints` chọn `PhoneDetailLayout` (bố cục của T-022, không đổi) hay `WideDetailLayout`.
- Tách từ `PoseDetailComponents.kt` của T-022 các phần dùng chung cho hai bố cục: `DetailImage(state, categoryId, onRetryImage, modifier)`, `DetailInfo(pose, locale)` (dòng phụ, "Các bước", các bước, khối lỗi hay gặp), `DetailBottomBar(enabled, onShoot)`. Bố cục điện thoại gọi lại đúng các phần đó theo thứ tự cũ, nên ảnh chuẩn điện thoại không đổi.
- `WideDetailLayout`: dưới thanh trên là `Row(Modifier.weight(1f).padding(start = Spacing.md, top = Spacing.md))`:
  - cột trái `Modifier.weight(315f)`: `DetailImage` cao 440 (vùng ảnh lỗi hiện `StateMessage` như điện thoại);
  - khoảng cách `Spacing.md`;
  - cột phải `Modifier.weight(326f)`: `Column` gồm vùng cuộn `Modifier.weight(1f).verticalScroll(scrollState).padding(end = Spacing.md)` chứa `DetailInfo`, rồi `DetailBottomBar` (đường kẻ rộng hết cột phải, nút có lề `Spacing.md`), rồi `bottomBanner()` (T-023) rộng bằng cột phải.
- `Loading`, `Disabled`, `Missing`, `saveFailed` giữ như T-022 ở cả hai bố cục.

**Thứ tự làm (L-025).** Bước 3 (commit test): `WideLayoutMinWidth`, `isWide()` trả luôn `false` (khung rỗng); tách `DetailImage`, `DetailInfo`, `DetailBottomBar` (chỉ di chuyển code, ảnh chuẩn cũ phải qua y nguyên); `WideDetailLayout` gọi `PhoneDetailLayout`. Mọi test mới. Bước 4 (commit code): `isWide()` thật, `WideDetailLayout`, ghi ảnh chuẩn mới (`./gradlew recordRoborazziDebug`), kiểm ảnh chuẩn điện thoại không đổi, `scripts/design-compare.sh poses`, build `stagingDebug`.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

Đường dẫn gói viết tắt: `.../poses` là `com/vglstudio/camera/pose/line/guide/feature/poses`.

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesComponents.kt` | `WideLayoutMinWidth`, `isWide()` |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreen.kt` | Lưới 3 cột khi rộng, cả khung xương lúc tải |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreen.kt` | Chọn bố cục theo chiều rộng, `ScrollState` chung (file do T-022 tạo) |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailComponents.kt` | Tách `DetailImage`, `DetailInfo`, `DetailBottomBar`; `PhoneDetailLayout`, `WideDetailLayout` (file do T-022 tạo) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenTest.kt` | Test 3 cột, ngưỡng, đổi chiều rộng (mục 4) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenScreenshotTest.kt` | 2 screenshot test `_unfolded` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenTest.kt` | Test hai cột (mục 4; file do T-022 tạo) |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenScreenshotTest.kt` | 2 screenshot test `_unfolded` (file do T-022 tạo) |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_default_light_unfolded.png` | Ảnh chuẩn mới, khung `w673dp-h841dp-xxhdpi` |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_default_dark_unfolded.png` | Ảnh chuẩn mới, khung `w673dp-h841dp-xxhdpi` |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_default_light_unfolded.png` | Ảnh chuẩn mới, khung `w673dp-h841dp-xxhdpi` |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_default_dark_unfolded.png` | Ảnh chuẩn mới, khung `w673dp-h841dp-xxhdpi` |
| Ghi lại | `docs/features/poses/design/review/` | Sinh bởi `scripts/design-compare.sh poses`, không sửa tay |

Không file nào khác. Không sửa `PosesViewModel.kt`, `PoseDetailViewModel.kt` (trạng thái không đổi theo bố cục), `:core:designsystem` (`PoseCard` đã rộng theo ô lưới, T-021), `:feature:camera`, `:app`, `gradle/libs.versions.toml`, `AndroidManifest.xml` (không thêm `configChanges`). Ảnh chuẩn điện thoại của T-021, T-022, T-023 không ghi lại; phải qua y nguyên.

**Nơi khác đọc các file trên** (L-020): các file chỉ được test của module đọc; `PosesNavigation.kt`, `PoseDetailNavigation.kt` gọi `PosesRoute`, `PoseDetailRoute`, chữ ký không đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: không có.
- Quảng cáo: không thêm vị trí. Banner của T-023 giữ vị trí: thư viện rộng hết đáy màn; chi tiết dưới thanh nút trong cột phải (mục 8 câu 3).
- Sự kiện phân tích: không chạm.
- Dữ liệu cục bộ: không chạm.
- File R2: không chạm. Ảnh nhỏ giải mã rộng 486 px (T-021) mà thẻ 3 cột rộng 203 dp (609 px ở xxhdpi): ảnh `preview_540` không bị thu nhỏ khi giải mã (540 / 2 < 486), nên vẫn đủ nét; không đổi bộ giải mã.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Không dùng `androidx.window` hay `material3-window-size-class` (chưa có trong `gradle/libs.versions.toml`); `BoxWithConstraints` thuộc `compose-foundation` đã có.

### 3.4. Phương án dự phòng

Kỹ thuật không có điểm chưa chắc: `BoxWithConstraints` theo chiều rộng đã dùng ở `LoadingScreen.kt`, `OnboardingScreen.kt`; khung `w673dp-h841dp-xxhdpi` đã dùng ở screenshot test của camera (T-013). Phương án dưới đây chỉ cho thứ tự task:

- **Điểm chưa chắc:** T-023 có thể dừng trong cùng phiên, nên lúc T-024 chạy `PoseDetailScreen` chưa có tham số `bottomBanner`.
- **Điều kiện chuyển:** lúc bắt đầu task, `detail/PoseDetailScreen.kt` trên nhánh chính không có `bottomBanner`.
- **Cách làm thay:** làm mọi phần khác như mục 3.1; `WideDetailLayout` không gọi banner; không viết test `wide_banner_belowBottomBar_inRightColumn` và ghi vào nhật ký để T-023 (khi chạy lại) thêm banner vào bố cục rộng. Không tự thêm `bottomBanner` (phần của T-023). Các tiêu chí của KB-7 và các test khác giữ nguyên.

## 4. Kiểm thử

Mọi test dưới đây vào commit test của bước 3, cùng khung ở mục 3.1. Lý do thất bại ở cột cuối là điều AI dự kiến; bước 3 chạy và ghi lý do thật vào nhật ký. Test đo vị trí dùng `@GraphicsMode(GraphicsMode.Mode.NATIVE)`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-7, thư viện 3 cột khi mở gập | Compose UI test | `PosesScreenTest.wide_hasThreeColumns` (`@Config(qualifiers = "w673dp-h841dp-xxhdpi")`): thẻ 1, 2, 3 cùng `top`, `left` tăng dần; thẻ 4 dưới thẻ 1, cùng `left`; mỗi thẻ rộng 203 dp (sai số 1 dp). Thất bại: khung giữ 2 cột (thẻ 3 nằm dưới thẻ 1) |
| KB-7, khung xương 3 cột | Compose UI test | `PosesScreenTest.wideLoading_hasThreeColumns` (như trên với `Loading`) |
| Ngưỡng 600 dp | Compose UI test | `PosesScreenTest.width599_isTwoColumns_width600_isThreeColumns` (khung `w673dp-h841dp-xxhdpi`, nội dung trong `Box(Modifier.requiredWidth(...))`). `width599` qua ngay ở khung rỗng; `width600` thất bại |
| Điện thoại giữ 2 cột | Test sẵn có | `PosesScreenTest.grid_hasTwoColumns` (T-021) và các ảnh chuẩn điện thoại không đổi |
| KB-7, gập mở giữ vị trí cuộn và chip | Compose UI test | `PosesScreenTest.widthChange_keepsScrollAndSelectedChip`: khung `w673dp-h841dp-xxhdpi`, chiều rộng của nội dung là trạng thái (411 rồi 673 rồi 411), cùng `gridState`, chip "Nam" đang chọn; cuộn tới thẻ 6 ở 411; đổi 673: `firstVisibleItemIndex == 6`, lưới 3 cột, chip "Nam" vẫn `assertIsSelected()`; đổi lại 411: vẫn 6, 2 cột. Thất bại: khung không đổi số cột (phần 3 cột). Phần dựng lại activity đã có test: `PosesScreenTest.scrollPosition_survivesStateRestore`, `PosesViewModelTest.selectedFilter_restoredFromSavedStateHandle` (T-021) |
| KB-7, chi tiết hai cột, nút ở đáy cột phải | Compose UI test | `PoseDetailScreenTest.wide_imageLeft_infoRight_bottomBarInRightColumn` (`@Config(qualifiers = "w673dp-h841dp-xxhdpi")`): vùng ảnh rộng 315 dp, cao 440 dp (sai số 1 dp), cạnh trái 16 dp; dòng phụ bắt đầu ở 347 dp; thanh nút có cạnh trái 347 dp (cạnh phải của ảnh 331 cộng khoảng cách 16), cạnh phải bằng cạnh phải màn, cạnh dưới bằng cạnh dưới màn; nút không nằm dưới vùng ảnh. Thất bại: khung dùng bố cục điện thoại (dòng phụ nằm dưới ảnh) |
| KB-7, chi tiết rộng: nội dung cuộn, nút luôn thấy | Compose UI test | `PoseDetailScreenTest.wide_infoScrolls_bottomBarStaysVisible` (`w05`, thêm lỗi dài để vượt chiều cao): `performScrollTo()` chữ lỗi cuối, cạnh dưới của nó không vượt cạnh trên thanh nút; thanh nút vẫn trong màn |
| KB-7, chi tiết rộng có banner (T-023) | Compose UI test | `PoseDetailScreenTest.wide_banner_belowBottomBar_inRightColumn` (`bottomBanner` là `Box` cao 58 dp): cạnh trên banner bằng cạnh dưới thanh nút, cạnh trái bằng cạnh trái cột phải |
| KB-7, gập mở giữ tư thế và vị trí cuộn ở chi tiết | Compose UI test | `PoseDetailScreenTest.widthChange_keepsPoseAndScroll`: cùng `uiState`, đổi chiều rộng 411, 673, 411: tên ở thanh trên không đổi; `ScrollState` dùng chung (cuộn ở 411, sang 673 rồi về 411: `scrollState.value > 0`). Thất bại: khung không có bố cục rộng (phần vị trí của dòng phụ khi 673) |
| KB-7, ảnh lỗi ở cột trái | Compose UI test | `PoseDetailScreenTest.wide_imageFailed_retryInLeftColumn` (`StateMessage` và nút "Thử lại" nằm trong vùng ảnh bên trái) |
| Đúng thiết kế `_unfolded` | Screenshot test cùng tên ảnh thiết kế, `@Config(qualifiers = "w673dp-h841dp-xxhdpi")` trên từng test; ảnh so sánh `scripts/design-compare.sh poses`; rà soát độc lập theo mục 7 của README thiết kế | `PosesScreen_default_light_unfolded.png`, `PosesScreen_default_dark_unfolded.png` bằng `captureRoboImage("src/test/screenshots/PosesScreen_default_light_unfolded.png") { ... }` (cùng dữ liệu với `default`: 8 nhóm, 6 thẻ thành 2 hàng); `PoseDetailScreen_default_light_unfolded.png`, `PoseDetailScreen_default_dark_unfolded.png` bằng `captureRoboImage("src/test/screenshots/PoseDetailScreen_default_light_unfolded.png") { ... }` (ảnh lớn `Ready`) |
| Ảnh thiết kế của ticket khác | Không làm ở đây | Ảnh thiết kế của ticket khác (T-021): `PosesScreen_default_light`, `PosesScreen_default_dark`, `PosesScreen_loading_light`, `PosesScreen_loading_dark`, `PosesScreen_error_light`, `PosesScreen_error_dark`, `PosesScreen_saved-empty_light`, `PosesScreen_saved-empty_dark` |
| Ảnh thiết kế của ticket khác | Không làm ở đây | Ảnh thiết kế của ticket khác (T-022): `PoseDetailScreen_default_light`, `PoseDetailScreen_default_dark`, `PoseDetailScreen_loading_light`, `PoseDetailScreen_loading_dark`, `PoseDetailScreen_error_light`, `PoseDetailScreen_error_dark` |
| Ảnh thiết kế của ticket khác | Không làm ở đây | Ảnh thiết kế của ticket khác (T-023): `PosesScreen_ad_light`, `PosesScreen_ad_dark`, `PoseDetailScreen_ad_light`, `PoseDetailScreen_ad_dark` |
| Bố cục điện thoại không đổi | Screenshot test sẵn có | Mọi ảnh chuẩn điện thoại của `:feature:poses` (T-021, T-022, T-023) qua y nguyên; `scripts/check.sh` so ảnh |
| Màn hình thấp (L-028) | Không có ảnh mới | Bố cục điện thoại không đổi; ảnh `_small` của T-021, T-022, T-023 giữ nguyên |
| Gập mở trên máy thật | Kiểm tra thủ công | Staging, máy gập thật hoặc emulator gập (Pixel Fold trong Android Studio): (1) mở gập ở thư viện: 3 cột; chọn chip "Nam", cuộn xuống; gập lại: 2 cột, chip "Nam", vẫn ở chỗ đang xem; mở lại: như trước; (2) mở chi tiết khi mở gập: ảnh trái, hướng dẫn phải, nút ở đáy cột phải; gập lại: bố cục điện thoại, cùng tư thế; (3) bấm "Chụp với tư thế này" khi mở gập: camera mở với tư thế đó (bố cục camera là T-020); (4) chế độ tối |

**Ngoại lệ viết test trước:** tách `DetailImage`, `DetailInfo`, `DetailBottomBar` ở bước 3 chỉ di chuyển code; được kiểm bằng ảnh chuẩn điện thoại có sẵn (phải qua y nguyên ở bước 3).

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có. Không thêm vị trí quảng cáo (banner của T-023 chỉ đổi chỗ đặt trong bố cục rộng), không sửa module core, không thêm thư viện, quyền hay sự kiện.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Bốn file code và bốn file test trong `:feature:poses` |
| B. Độ rõ của yêu cầu | 1 | Spec, thiết kế `_unfolded` đã duyệt; AI tự quyết ngưỡng (câu 1), cuộn ở chi tiết (câu 2), chỗ banner khi rộng (câu 3) |
| C. Khả năng kiểm chứng tự động | 2 | Bố cục và đổi chiều rộng có test JVM; gập mở thật (dựng lại activity) cần máy gập hay emulator gập |
| D. Ảnh hưởng chéo | 2 | Sửa các file mà T-022, T-023 tạo hoặc sửa trước đó trong cùng phiên |
| E. Độ mới của kỹ thuật | 1 | `BoxWithConstraints` theo chiều rộng và khung `w673dp` đã có trong dự án |
| **Tổng** | 7 | **Mức: Trung bình** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: chỉ file của `:feature:poses`. Dùng lại `PoseCard`, `PoseCardSkeleton` (không sửa).
- Task khác có thể xung đột:
  - **T-022, T-023**: tạo và sửa các file chi tiết, `PosesScreen.kt` và hai file screenshot test mà plan này sửa. T-024 chạy sau cả hai.
  - **T-020** (camera và màn xem lại trên máy gập): cùng loại việc trong `:feature:camera`, không chung file. Nên dùng cùng ngưỡng 600 dp để camera và thư viện đổi bố cục cùng lúc (mục 8 câu 1).
- Đề xuất thứ tự chạy: T-022, T-023, rồi T-024. Ticket T-024 chỉ ghi `depends_on: [T-021, T-022]`, nên khi T-023 dừng, phiên vẫn chạy T-024; khi đó `bottomBanner` chưa có và plan dùng phương án dự phòng ở mục 3.4 (mục 8 câu 6).

## 7. Điều kiện dừng riêng

1. Lúc bắt đầu task, màn chi tiết của T-022 chưa có trên nhánh chính.
2. Tách `DetailImage`, `DetailInfo`, `DetailBottomBar` làm đổi một ảnh chuẩn điện thoại.
3. Cần sửa `:core:designsystem` (ví dụ `PoseCard` không rộng theo ô lưới) hay thêm thư viện cửa sổ.

## 8. Câu hỏi cho người duyệt

1. **Ngưỡng bố cục rộng.** Spec nói "khi máy gập mở ra"; plan dùng chiều rộng màn từ 600 dp, không nhận dạng tư thế gập (không thêm `androidx.window`). Plan T-020 của camera (viết song song) nên dùng cùng ngưỡng; chưa đưa ngưỡng vào một module core để hai plan không cùng tạo một file.
   - Đề xuất của AI: 600 dp trong `:feature:poses`; nếu sau này cần dùng chung, một ticket riêng đưa ngưỡng vào `:core:designsystem`.
2. **Vị trí cuộn ở chi tiết khi gập mở.** Spec giữ "tư thế đang xem"; bố cục điện thoại cuộn cả ảnh, bố cục rộng chỉ cuộn cột phải.
   - Đề xuất của AI: một `ScrollState` chung; khi đổi bố cục, vị trí gần đúng chỗ đang đọc (có thể lệch bằng chiều cao ảnh), không về đầu.
3. **Banner chi tiết khi mở gập.** Thiết kế `_unfolded` không có banner.
   - Đề xuất của AI: banner dưới thanh nút, rộng bằng cột phải (vẫn "dưới nút chính" như KB-6); thư viện giữ banner rộng hết đáy màn.
4. **Thẻ 3 cột cao hơn thiết kế.** Thẻ rộng 203 dp, ảnh giữ tỷ lệ 162 : 210 nên ảnh cao khoảng 263 dp, thẻ cao khoảng 337 dp thay vì 284 trên thiết kế. README mục 6 cho phép "chiều cao thẻ theo tỷ lệ ảnh thật".
   - Đề xuất của AI: giữ tỷ lệ ảnh; ảnh so sánh sẽ lệch ở chiều cao thẻ.
5. **`token_estimate`.** Số gợi ý cỡ S là 5.0M (mới đo 2 task); task sửa hai màn, bốn file test, bốn ảnh chuẩn.
   - Đề xuất của AI: ghi 20M.
6. **T-024 có nên phụ thuộc T-023?** Ticket T-024 chỉ ghi `depends_on: [T-021, T-022]`. Hai task cùng sửa `detail/PoseDetailScreen.kt`; plan này viết trên code sau T-023 và có phương án dự phòng (mục 3.4) khi T-023 dừng.
   - Đề xuất của AI: giữ `depends_on` như cũ, để T-024 vẫn chạy khi T-023 dừng (task độc lập hơn, theo docs/CLAUDE.md mục Khi lập danh sách chạy); xếp T-023 trước T-024 trong danh sách chạy.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-23, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### 2026-10-23, RUN-20261023-04: đã merge

- **Kết quả:** đã merge, PR #75, commit `bd94e59`. CI xanh ngay vòng đầu. Làm theo nhánh chính của plan (T-023 đã merge), không dùng phương án dự phòng.
- **Test viết trước:** 9 test giao diện mới (4 `PosesScreenTest`, 5 `PoseDetailScreenTest`) thất bại đúng lý do trên khung rỗng (còn 2 cột; ảnh rộng 641 thay vì 315); 4 screenshot test `_unfolded` thất bại vì chưa có ảnh chuẩn. Viết chặt lại có đối chứng: 0 (hai test có sẵn bước đối chứng ở 411 dp).
- **Lệch so với plan:** (1) tên thành phần giữ `PoseDetailImage`, `PoseDetailMeta`, `PoseDetailSteps`, `PoseDetailMistakes`, `PoseDetailBottomBar` của T-022, thêm `PoseDetailInfo` và tham số `imageHeight` thay cho `DetailImage`, `DetailInfo`, `DetailBottomBar`; (2) `widthChange_keepsScrollAndSelectedChip` sửa ở bước 4: 30 thẻ thay vì 12 vì `LazyGrid` kẹp vị trí cuộn (điều kiểm không đổi); (3) `wideLoading_hasThreeColumns` đếm khung xương đang hiện thay vì đúng 6.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa trong phạm vi plan, 10 ghi chú: thẻ 3 cột cao hơn thiết kế (337 so với 284 dp, plan mục 8 câu 4 đã duyệt); thanh nút chi tiết lệch khoảng 6 px; cột ảnh 440 dp cố định.
- **Cần người kiểm tra:** gập mở thật trên máy gập hoặc emulator gập (activity dựng lại thật chưa được test trên máy); 4 ảnh chuẩn `_unfolded`; ảnh so sánh thiết kế (thư viện 23,5%, chi tiết 3,7%).
