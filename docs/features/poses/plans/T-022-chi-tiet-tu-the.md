---
ticket: T-022
title: Chi tiết tư thế và "Chụp với tư thế này"
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 35M          # cỡ M, số gợi ý 15.4M; nâng lên vì là màn hình mới với 8 ảnh chuẩn như T-025 (thật 37.7M), mục 8 câu 8
blocking_points: [analytics] # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-022: CHI TIẾT TƯ THẾ VÀ "CHỤP VỚI TƯ THẾ NÀY"

## 1. Tóm tắt

Thêm màn chi tiết tư thế vào `:feature:poses`: thanh trên có tên tư thế và nút lưu, ảnh lớn (đang tải có vòng chờ, lỗi có "Thử lại"), dòng phụ, các bước đánh số, khối "Lỗi hay gặp", nút "Chụp với tư thế này" ghim ở đáy. Chạm thẻ ở thư viện mở màn này; bấm nút thì `:app` thay mục camera trên back stack bằng camera mới có `poseId` (T-014 đã đọc khóa này), nên quay lại từ camera vẫn thoát app như trước. Màn ghi `pose_detail_opened`, `pose_selected` (`source` `detail`), `pose_saved`, `content_load_failed` (`image`). Kiểm chứng bằng unit test ViewModel với bản giả, test giao diện Robolectric, test điều hướng Robolectric trong `:app`, 8 screenshot test (6 cùng tên ảnh thiết kế, 2 khung thấp) và kiểm tra thủ công trên staging.

## 2. Hiểu yêu cầu

Ticket xử lý KB-3 (phần lưu ở chi tiết), KB-4, KB-5 (phần ảnh lớn) trong `features/poses/spec.md`. Banner chi tiết (T-023) và bố cục máy gập (T-024) không làm ở đây.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-3 | Bấm nút lưu ở chi tiết thì lưu, nút đổi trạng thái; bấm lại thì bỏ lưu; mỗi lần đổi ghi `pose_saved` | Nút lưu ở thanh trên (icon `bookmark` hoặc `bookmark_added`). Cùng cách của thư viện (T-021): đổi ngay (lạc quan), gọi `PosesLibrarySource.setSaved`, ghi xong thì ghi `pose_saved` (`pose_id`, `saved` là `true` hoặc `false`); bấm khi đang ghi thì bỏ qua |
| KB-3 | Mở lại app vẫn còn | DataStore của `:core:poses` (T-009), như T-021 |
| KB-3, đặc biệt | Ghi lỗi thì nút về trạng thái cũ và có thông báo ngắn | Bắt `IOException`, bỏ trạng thái lạc quan, `Snackbar` "Chưa lưu được tư thế. Thử lại sau." (chữ của T-021), không ghi `pose_saved` |
| KB-4 | Chạm thẻ thì chi tiết mở: tên ở thanh trên, ảnh lớn, dòng loại, nhóm, khung hình, số bước, các bước đánh số, "Lỗi hay gặp", nút ghim ở đáy; ghi `pose_detail_opened` | `:app` nối `onOpenPose` của `posesScreen` sang `PoseDetailDestination(poseId)`. Dòng phụ `"%1$s · %2$s · %3$s · %4$d bước"`: loại (Một người, Cặp đôi, Nhóm), tên nhóm của catalog, khung hình (Toàn thân, Nửa người, Cận mặt), số bước theo ngôn ngữ của màn. `pose_detail_opened` (`pose_id`, `category` là mã nhóm) một lần cho mỗi lần mở (cờ trong `SavedStateHandle`, không ghi lại khi xoay hay khôi phục) |
| KB-4 | Nội dung dài hơn màn hình thì cuộn được, nút luôn thấy được, không che chữ cuối | Nội dung trong `Column(Modifier.weight(1f).verticalScroll(...))`, thanh nút đáy nằm ngoài vùng cuộn, nên chữ cuối dừng trên thanh nút |
| KB-4 | Bấm "Chụp với tư thế này" thì camera mở với đúng tư thế; ghi `pose_selected` `source` `detail` | ViewModel ghi `pose_selected` (`pose_id`, `category`, `source` là `detail`), rồi `:app` gọi `openCameraWithPose(id)`: `navigate(CameraDestination(askPermission = <của mục camera đang có>, poseId = id)) { popUpTo<CameraDestination> { inclusive = true }; launchSingleTop = true }`. Camera mới đọc `poseId` lúc mở, chọn tư thế, ghi "đã dùng", cuộn dải tới ô của nó (T-014, `PoseViewModelTest.poseIdArgument_selectsThatPose_andScrollsToIt`); camera không ghi `pose_selected` cho `detail` (plan T-014, câu 3) nên không đếm đôi. Công cụ camera (ống kính, đèn, hẹn giờ) đọc lại từ DataStore (T-013) |
| KB-4 | Bấm quay lại ở chi tiết thì về thư viện | `popBackStack()` chỉ khi chi tiết đang ở trên cùng (như thư viện). Thư viện giữ chip (`SavedStateHandle`) và vị trí cuộn (`rememberLazyGridState` lưu theo mục back stack); T-021 để T-022 kiểm lại trên luồng thật (nhật ký T-021) |
| KB-5 | Ảnh lớn chưa xong thì vùng ảnh có vòng chờ; lỗi thì "Không tải được ảnh", lời hướng dẫn, "Thử lại", các bước vẫn đọc được | `PoseFile.IMAGE` qua `PoseCatalogRepository.loadFile` (đường dẫn trong catalog, ADR-0015), giải mã bằng `BitmapPoseImageDecoder` có sẵn. Lỗi: ghi `content_load_failed` (`content` `image`, `reason`), vùng ảnh hiện `StateMessage`; dòng phụ, các bước, lỗi hay gặp vẫn hiện bên dưới |
| KB-5 | 15 giây không phản hồi thì là lỗi | Mức chờ nằm trong `:core:poses` (ADR-1001, T-009); `TIMEOUT` hiện như lỗi khác |
| KB-5 | Lỗi do mất mạng thì hộp thoại mạng thay thông báo riêng | Như T-021: lý do `NETWORK` khi `NetworkStatus.dialogShown` đúng thì vùng ảnh giữ vòng chờ dưới hộp thoại; hộp thoại đóng bằng "Thử lại" (`recovered`) thì tải lại ảnh. Hộp thoại không hiện thì hiện lỗi thường |
| Ticket | Cờ `feature_poses_enabled` | Cờ tắt khi đang ở chi tiết thì màn gọi `onBack` một lần (như thư viện; thư viện cũng tự đóng) |

Thiết kế tham chiếu: `features/poses/design/README.md`, các ảnh trong `design/screens/`: `PoseDetailScreen_default_light`, `PoseDetailScreen_default_dark`, `PoseDetailScreen_loading_light`, `PoseDetailScreen_loading_dark`, `PoseDetailScreen_error_light`, `PoseDetailScreen_error_dark`; số liệu trong `design/context/` cùng tên.

ADR liên quan: ADR-0001 (feature không phụ thuộc feature khác: điều hướng sang camera nằm ở `:app`), ADR-0003 (cờ theo dõi bằng `Flow`), ADR-0008 (màu, chữ, khoảng cách từ `:core:designsystem`), ADR-0012 (sự kiện qua `:core:analytics`), ADR-0015 (ảnh qua `:core:poses`), ADR-0019 (log qua `AppLogger`), ADR-0020 (tên screenshot test, ảnh so sánh), ADR-1001 (15 giây).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu của thư viện (T-021) trong cùng module: repository trong `data/`, `UiState` sealed, ViewModel trả `StateFlow`, `PoseDetailRoute` nối ViewModel với `PoseDetailScreen` không trạng thái, đích `@Serializable` trong `navigation/`. Màn chi tiết nằm trong gói con `detail/`. Thẻ log `Poses`.

**Dữ liệu.**

- Tư thế lấy từ `PosesLibrarySource.library` có sẵn: `LibraryResult.Ready` thì tìm mã trong `content.posesFor(PoseFilter.All)`, đã lưu khi mã nằm trong `content.savedIds`; tên nhóm từ `content.categories`.
- Ảnh lớn: `data/PoseImageRepository.kt` (mới): `sealed interface PoseImageResult { Ready(image: ImageBitmap); Failed(failure: PoseLoadFailure) }`, `interface PoseImageRepository { suspend fun load(pose: Pose): PoseImageResult }`. `data/DefaultPoseImageRepository.kt` (mới, `internal`): như `DefaultPoseThumbnailRepository` nhưng `PoseFile.IMAGE` và chiều rộng đích 1137 px (vùng ảnh 379 dp ở xxhdpi; `preview_1200` giữ nguyên cỡ); giải mã ra `null` thì `Failed(PoseLoadFailure(IMAGE, OTHER))`. Gắn bằng một `@Binds` thêm vào `PosesFeatureModule`.

**Trạng thái (`detail/PoseDetailUiState.kt`).**

```kotlin
sealed interface PoseDetailUiState {
    data object Loading : PoseDetailUiState      // cờ chưa biết hoặc catalog đang tải
    data object Disabled : PoseDetailUiState     // cờ tắt: màn gọi onBack
    data object Missing : PoseDetailUiState      // mã không còn trong catalog, hoặc catalog lỗi: màn gọi onBack
    data class Ready(
        val pose: PoseDetail,
        val saved: Boolean,
        val image: DetailImageState,
        val saveFailed: Boolean = false,
    ) : PoseDetailUiState
}
data class PoseDetail(val id: String, val categoryId: String, val name: LocalizedText, val categoryName: LocalizedText?,
    val subjectType: SubjectType, val framing: Framing, val steps: LocalizedList, val mistakes: LocalizedList)
sealed interface DetailImageState { data object Loading; data class Ready(val image: ImageBitmap); data object Failed }
```

**ViewModel (`detail/PoseDetailViewModel.kt`).** Nhận `PosesLibrarySource`, `PoseImageRepository`, `NetworkStatus`, `Analytics`, `AppLogger`, `FeatureFlags`, `SavedStateHandle`. Mã tư thế đọc từ khóa `poseId` của `SavedStateHandle` (tham số của đích).

- Cờ `FeatureFlag.POSES` thu theo luồng; `false` là `Disabled`; bật thì thu `library`.
- `LibraryResult.Ready`: mã có trong catalog thì `Ready`, bắt đầu tải ảnh lớn lần đầu, ghi `pose_detail_opened` nếu khóa `detail_opened_logged` của `SavedStateHandle` chưa có (rồi đặt khóa); log `info` "Pose detail opened". Mã không có: `Missing`, log `warn` "Pose detail: pose not in catalog". `LibraryResult.Failed`: `Missing`. `Loading`: `Loading`.
- Ảnh lớn: `Loading` rồi `load(pose)`; `Ready` thì giữ ảnh; `Failed`: ghi `content_load_failed` (`content` `image`, `reason` theo `analyticsValue`), log `warn`; lý do `NETWORK` và `dialogShown` đúng thì giữ `Loading`, ngược lại `Failed`. `onRetryImage()`: log `info` "Retrying image", tải lại. `networkStatus.recovered`: tải lại khi ảnh đang lỗi hoặc đang chờ dưới hộp thoại. Một lần tải tại một thời điểm.
- `onToggleSave()`: như `PosesViewModel.onToggleSave` (ghi đè lạc quan tới khi thư viện phát giá trị đã ghi; `IOException` thì `saveFailed = true`, log `warn` kèm lỗi). `onSaveFailedShown()`.
- `onShoot(): String?`: khi `Ready`, ghi `pose_selected` (`pose_id`, `category`, `source` `detail`), log `info` "Shoot with pose", trả mã; trạng thái khác trả `null` và không ghi gì.
- Log không chứa mã hay tên tư thế (như T-021).

**Màn hình (`detail/PoseDetailScreen.kt`, `detail/PoseDetailComponents.kt`).**

- `PoseDetailRoute(onBack, onShootWithPose: (String) -> Unit, viewModel = hiltViewModel())`; nút chính gọi `viewModel.onShoot()?.let(onShootWithPose)`.
- `PoseDetailScreen(uiState, onBack, onToggleSave, onRetryImage, onShoot, onSaveFailedShown, modifier)`: `Box` nền `background`, `testTag("pose_detail_screen")`, trong đó `Column`:
  - Thanh trên: `TopAppBar` như `PosesTopBar` (cao 64, `windowInsets = WindowInsets(0)`); nút quay lại dùng `ic_poses_arrow_back`; tiêu đề là tên tư thế theo ngôn ngữ của màn, `titleLarge`, một dòng, cắt bằng dấu ba chấm; bên phải nút lưu (vùng chạm 48, icon 24 `DesignR.drawable.ic_bookmark` hoặc `ic_bookmark_added`, mô tả "Lưu tư thế" hoặc "Bỏ lưu tư thế", `Modifier.toggleable` không dùng, chỉ `IconButton`, như thiết kế). Thanh trên không có nút lưu khi chưa `Ready`.
  - Vùng cuộn `Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.md), verticalArrangement = spacedBy(Spacing.md))`:
    - Vùng ảnh: rộng hết, cao 380, bo `Radius.card`, viền `Stroke.sticker` `outline`, `testTag("pose_detail_image")`. `Ready`: nền `categoryColor(categoryId)` (hàm có sẵn của T-021), ảnh `ContentScale.Fit`, không có mô tả (tên đã ở thanh trên). `Loading`: nền `surfaceContainerHighest`, `CircularProgressIndicator` 40 dp màu `primary` ở giữa. `Failed`: nền `surfaceVariant`, `StateMessage(icon broken_image, "Không tải được ảnh", "Đã có lỗi khi tải ảnh. Thử lại sau ít phút.", actionLabel "Thử lại", actionIcon `ic_refresh`, onAction = onRetryImage)` ở giữa.
    - Dòng phụ `bodyMedium` `onSurfaceVariant`; nhóm không có tên thì bỏ phần nhóm.
    - "Các bước" `titleMedium` `onSurface`; các bước cách nhau `Spacing.sm`, mỗi bước một `Row` cách nhau `Spacing.sm`: vòng 28 nền `secondaryContainer`, viền `Stroke.sticker` `outline`, số `labelLarge` `onSecondaryContainer`; chữ `bodyMedium` `onSurface`, xuống dòng khi dài.
    - Khối "Lỗi hay gặp" (khi `mistakes` theo ngôn ngữ của màn khác rỗng): nền `tertiaryContainer`, viền `Stroke.sticker` `outline`, bo `Radius.card`, lề trong `Spacing.md`; `Row` cách nhau `Spacing.sm`: icon `warning` 24 màu `onTertiaryContainer`; cột cách nhau 2 dp (`Spacing.xs / 2`, như T-021 câu 6): "Lỗi hay gặp" `titleSmall`, mỗi lỗi một `Text` `bodyMedium`, màu `onTertiaryContainer`.
    - Khoảng trống cuối `Spacing.md` để chữ cuối không sát thanh nút.
  - Thanh nút đáy (`testTag("pose_detail_bottom_bar")`): đường kẻ 2 dp `outlineVariant`, rồi `PrimaryButton("Chụp với tư thế này", icon photo_camera, Modifier.fillMaxWidth())` với lề `Spacing.md`. Nút chỉ bật khi `Ready`.
  - `Loading`: thanh trên (không tiêu đề), vòng chờ giữa vùng còn lại, không có thanh nút. `Disabled`, `Missing`: `LaunchedEffect(Unit) { onBack() }` một lần (như `PosesScreen`).
  - `saveFailed`: `SnackbarHost` ở đáy hiện "Chưa lưu được tư thế. Thử lại sau.", rồi gọi `onSaveFailedShown`.
- Chữ trong `res/values/strings.xml` theo bảng chữ (README mục 2): "Các bước", "Lỗi hay gặp", "Chụp với tư thế này", "Không tải được ảnh", "Đã có lỗi khi tải ảnh. Thử lại sau ít phút.", "Thử lại" (dùng lại `poses_error_action`), dòng phụ `"%1$s · %2$s · %3$s · %4$d bước"`, khung hình "Toàn thân", "Nửa người", "Cận mặt". Loại dùng lại `poses_subject_*`; mô tả nút lưu dùng lại `poses_cd_save`, `poses_cd_unsave`.
- Biểu tượng mới trong `feature/poses/src/main/res/drawable/`: `ic_poses_photo_camera.xml` (chép nội dung `feature/onboarding/src/main/res/drawable/ic_onboarding_photo_camera.xml`, cùng nguồn và giấy phép), `ic_poses_warning.xml`, `ic_poses_broken_image.xml` (Material Symbols Outlined, `google/material-design-icons@49d4db3`, chú thích đầu file như `ic_poses_error.xml`; giấy phép đã có ở `feature/poses/third_party/material-symbols/LICENSE.txt`). Tải không được là lỗi hạ tầng (auto-run.md, bước 5).

**Điều hướng.**

- `navigation/PoseDetailNavigation.kt` (mới): `@Serializable data class PoseDetailDestination(val poseId: String)`; `fun NavGraphBuilder.poseDetailScreen(onBack: () -> Unit, onShootWithPose: (String) -> Unit)`.
- `:app`, `navigation/PoseDetailEntry.kt` (mới): `internal fun NavController.openCameraWithPose(poseId: String)`: chỉ chạy khi `currentDestination` là `PoseDetailDestination` (bấm hai lần không mở hai camera); `askPermission` đọc từ mục `CameraDestination` đang có trên back stack (`getBackStackEntry<CameraDestination>().toRoute<CameraDestination>()`), không có thì `true`; rồi `navigate(CameraDestination(askPermission, poseId)) { popUpTo<CameraDestination> { inclusive = true }; launchSingleTop = true }`. Back stack còn đúng một camera, không còn thư viện và chi tiết; nút Back ở camera thoát app như trước.
- `:app`, `AppNavHost.kt`: `posesScreen(..., onOpenPose = { id -> navController.navigate(PoseDetailDestination(id)) { launchSingleTop = true } })`; `poseDetailScreen(onBack = { if (navController.currentDestination?.hasRoute<PoseDetailDestination>() == true) navController.popBackStack() }, onShootWithPose = { id -> navController.openCameraWithPose(id) })`. Giữ nguyên phần còn lại.
- Không sửa `:feature:camera`: `CameraDestination(poseId)` và phần đọc khóa đã có từ T-014.

**Vì sao thay mục camera thay vì đặt khóa vào mục camera đang có.** `NavBackStackEntry.savedStateHandle` và `SavedStateHandle` của ViewModel tạo bằng `hiltViewModel()` là hai đối tượng khác nhau, nên đặt `poseId` vào mục camera đang có không tới `PoseViewModel`; muốn vậy phải sửa `:feature:camera`, file mà T-015, T-017, T-020 cũng sửa. Camera mới đọc `poseId` từ tham số của đích, đã có test ở T-014. Đổi lại camera mở lại từ đầu (mục 8 câu 1).

**Thứ tự làm (L-025).** Bước 3 (commit test): `strings.xml` đủ chữ; các kiểu của `PoseDetailUiState.kt`, `PoseImageRepository.kt`; khung rỗng: `PoseDetailViewModel` có đủ hàm nhưng `uiState` luôn `Loading`, `onShoot()` trả `null`; `DefaultPoseImageRepository.load` luôn `Failed(IMAGE, OTHER)`; `PoseDetailScreen` chỉ vẽ `Box` mang `testTag("pose_detail_screen")`; `PoseDetailComponents.kt` rỗng; `PoseDetailDestination` và `poseDetailScreen()` đăng ký `PoseDetailRoute`; `openCameraWithPose` không làm gì; `@Binds` mới trong `PosesFeatureModule`. Mọi file test, phần thêm vào `PosesFakes.kt`. Bước 4 (commit code): cài đặt thật, biểu tượng, `AppNavHost.kt`, ghi ảnh chuẩn (`./gradlew recordRoborazziDebug`), `scripts/design-compare.sh poses`, build `stagingDebug`.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

Đường dẫn gói viết tắt: `.../poses` là `com/vglstudio/camera/pose/line/guide/feature/poses`; `.../guide` là `com/vglstudio/camera/pose/line/guide`.

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreen.kt` | `PoseDetailRoute`, `PoseDetailScreen`, `PoseDetailTestTags` |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailComponents.kt` | Thanh trên, vùng ảnh, dòng phụ, bước, khối lỗi hay gặp, thanh nút đáy |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailUiState.kt` | `PoseDetailUiState`, `PoseDetail`, `DetailImageState` |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailViewModel.kt` | ViewModel theo mục 3.1 |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/PoseImageRepository.kt` | Interface, `PoseImageResult` |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/DefaultPoseImageRepository.kt` | Tải `PoseFile.IMAGE`, giải mã rộng 1137 px |
| Sửa | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/di/PosesFeatureModule.kt` | Thêm `@Binds` cho `PoseImageRepository` |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/navigation/PoseDetailNavigation.kt` | `PoseDetailDestination`, `poseDetailScreen()` |
| Sửa | `feature/poses/src/main/res/values/strings.xml` | Chữ của chi tiết (mục 3.1) |
| Tạo | `feature/poses/src/main/res/drawable/ic_poses_photo_camera.xml`, `ic_poses_warning.xml`, `ic_poses_broken_image.xml` | Ba biểu tượng Material Symbols |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesFakes.kt` | Thêm `FakePoseImageRepository` (kết quả, độ trễ ảo, số lần gọi theo mã), `testPose` nhận thêm `framing`, `mistakes` (giá trị mặc định giữ nguyên test cũ) |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailViewModelTest.kt` | Unit test ViewModel (mục 4) |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenTest.kt` | Test giao diện Robolectric (mục 4) |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenScreenshotTest.kt` | 8 screenshot test |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/DefaultPoseImageRepositoryTest.kt` | Unit test (mục 4) |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/navigation/PosesBackStackTest.kt` | Test Robolectric: thư viện, sang chi tiết, quay lại giữ chip và vị trí cuộn (mục 4) |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_default_light.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_default_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_loading_light.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_loading_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_error_light.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_error_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_default_small.png` | Khung 360 x 640, 5 bước, 2 lỗi hay gặp (L-028) |
| Tạo | `feature/poses/src/test/screenshots/PoseDetailScreen_error_small.png` | Khung 360 x 640, nút "Thử lại" và nút chính thấy đủ |
| Ghi lại | `docs/features/poses/design/review/` | Sinh bởi `scripts/design-compare.sh poses`, không sửa tay |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/AppNavHost.kt` | Nối `onOpenPose`, đăng ký `poseDetailScreen` |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/PoseDetailEntry.kt` | `openCameraWithPose` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/navigation/PoseDetailEntryTest.kt` | Test Robolectric với `NavHost` thật và đích giả (mục 4) |

Không file nào khác. Đặc biệt không sửa `:feature:camera`, `:core:poses`, `:core:designsystem` (không thêm thành phần: bước và khối lỗi hay gặp chỉ dùng ở màn này, README mục 3 không ghi là thành phần mới), `FeatureFlag.kt`, `MainActivity.kt`, `docs/design-system/tokens.json`, `gradle/libs.versions.toml`, `AndroidManifest.xml`. Nhật ký plan, trạng thái plan và ticket đi qua nhánh báo cáo của phiên (auto-run.md, mục 4).

**Nơi khác đọc các file trên** (L-020), không cần sửa:

- `PosesFeatureModule.kt`: chỉ Hilt đọc.
- `strings.xml`: chỉ code của module đọc; thêm chuỗi, không đổi chuỗi cũ, nên ảnh chuẩn của thư viện không đổi.
- `PosesFakes.kt`: `PosesViewModelTest.kt`, `PosesScreenTest.kt`, `PosesScreenScreenshotTest.kt` dùng `testPose`, `testContent`; tham số mới có giá trị mặc định nên các test này không đổi.
- `AppNavHost.kt`: `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt` chờ màn camera; màn chính không đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: `feature_poses_enabled` có sẵn (T-021); chi tiết theo dõi cờ như thư viện. Không thêm cờ.
- Quảng cáo: không chạm. `poses_detail_banner` là T-023; màn này chưa đặt `AdSlot`.
- Sự kiện phân tích (điểm chặn 4, đã có trong spec mục 6):
  - `pose_detail_opened`: `pose_id`, `category` (mã nhóm của catalog).
  - `pose_selected`: `pose_id`, `category`, `source` là `detail`.
  - `pose_saved`: `pose_id`, `saved` là `true` hoặc `false` (như T-021).
  - `content_load_failed`: `content` là `image`; `reason` là `network`, `not_found`, `timeout`, `other`.
- Dữ liệu cục bộ: không thêm khóa DataStore. `detail_opened_logged` nằm trong `SavedStateHandle` (trạng thái của màn).
- File R2: không chạm. Ảnh lớn đọc qua `PoseCatalogRepository.loadFile`, đường dẫn trong catalog (ADR-0015). Ảnh lớn không thuộc bộ khởi đầu nên không vào `scripts/required-assets.txt`.
- **Quyền truy cập:** Không thêm. Tải ảnh qua `:core:assets` dùng quyền `INTERNET` đã có.
- **Thư viện và SDK:** Không thêm. `navigation-compose` (`libs.androidx.navigation.compose`) đã có trên classpath của `:app` và `:feature:poses` (plugin `template.android.feature`); `toRoute` nằm trong thư viện đó. `BitmapFactory` của Android.

### 3.4. Phương án dự phòng

Không có. Mọi kỹ thuật đã có trong dự án: tải và giải mã ảnh (T-021), `NavHost` thật trong test Robolectric của `:app` (`NetworkDialogHostTest`), chụp trạng thái có vòng chờ bằng đồng hồ dừng (L-029, `CameraScreenStartingScreenshotTest`).

## 4. Kiểm thử

Mọi test dưới đây vào commit test của bước 3, cùng khung rỗng ở mục 3.1. Lý do thất bại ở cột cuối là điều AI dự kiến; bước 3 chạy và ghi lý do thật vào nhật ký.

Bản giả: `FakePosesLibrarySource`, `FakeNetworkStatus`, `FakeAnalytics`, `FakeFeatureFlags` có sẵn (T-021); `FakePoseImageRepository` mới. Tư thế mẫu `w01` "Tựa lan can", nhóm `women` "Nữ", một người, toàn thân, 4 bước, 1 lỗi hay gặp; `w05` cặp đôi, nửa người, 5 bước, 2 lỗi. Logger là `RecordingAppLogger`. Ảnh trong unit test là `mockk<ImageBitmap>()`; test giao diện dùng `ImageBitmap(379, 380)` thật. Kiểm tra lỗi so loại (CLAUDE.md, bước 10).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Cờ tắt | Unit test, Compose UI test | `PoseDetailViewModelTest.flagOff_isDisabled`; `PoseDetailScreenTest.disabled_callsOnBackOnce`. Thất bại: luôn `Loading`, không có node |
| Catalog đang tải | Unit test | `PoseDetailViewModelTest.libraryLoading_isLoading` (qua ngay ở khung rỗng, test chặn hồi quy) |
| Mã không có trong catalog | Unit test, Compose UI test | `PoseDetailViewModelTest.poseNotInCatalog_isMissing_andLogsWarn`, `libraryFailed_isMissing`; `PoseDetailScreenTest.missing_callsOnBackOnce` |
| KB-4, nội dung chi tiết | Unit test | `PoseDetailViewModelTest.ready_hasPoseSavedStateAndCategoryName`: `pose.id == "w01"`, `categoryName` là "Nữ", `saved` theo `savedIds` (đặt `w01` đã lưu cạnh `w02` chưa lưu để test thất bại đúng lý do, L-035) |
| KB-4, tên ở thanh trên, dòng phụ, các bước đánh số, lỗi hay gặp, nút | Compose UI test | `PoseDetailScreenTest.ready_showsTitleMetaStepsMistakesAndButton`: "Tựa lan can", "Một người · Nữ · Toàn thân · 4 bước", "Các bước", số "1" đến "4" với chữ từng bước, "Lỗi hay gặp" và chữ lỗi, nút "Chụp với tư thế này". `couple_halfBody_meta` ("Cặp đôi · Nữ · Nửa người · 5 bước"); `noMistakes_hidesMistakesBlock`; `meta_countsStepsInScreenLanguage` (`@Config(qualifiers = "en")`, catalog có số bước khác nhau theo ngôn ngữ) |
| KB-4, ghi `pose_detail_opened` một lần | Unit test | `PoseDetailViewModelTest.ready_logsPoseDetailOpenedOnce`: `AnalyticsEvent("pose_detail_opened", mapOf("pose_id" to "w01", "category" to "women"))` đúng một lần dù thư viện phát lại; `restoredViewModel_doesNotLogOpenedAgain` (ViewModel thứ hai từ cùng `SavedStateHandle`) |
| KB-4, nội dung dài thì cuộn, nút luôn thấy, không che chữ cuối | Compose UI test | `PoseDetailScreenTest.smallScreen_bottomBarAlwaysVisible_lastMistakeScrollsAboveIt` (`@Config(qualifiers = "w360dp-h640dp-xxhdpi")`, `GraphicsMode.NATIVE`, `w05`): thanh nút nằm trong khung màn hình trước và sau khi cuộn; `performScrollTo()` chữ lỗi cuối, cạnh dưới của nó không vượt cạnh trên của thanh nút |
| KB-4, bấm nút: `pose_selected` và mở camera | Unit test, Compose UI test | `PoseDetailViewModelTest.shoot_logsPoseSelectedDetail_andReturnsId`: trả `"w01"`, sự kiện `mapOf("pose_id" to "w01", "category" to "women", "source" to "detail")`; `shootBeforeReady_returnsNull_noEvent` (qua ngay ở khung rỗng); `PoseDetailScreenTest.shootClick_callsOnShoot` |
| KB-4, camera mở đúng tư thế, back stack còn một camera | Test Robolectric | `PoseDetailEntryTest.openCameraWithPose_replacesCameraWithPoseId_andDropsLibraryAndDetail`: `NavHost` thật với ba đích giả (`CameraDestination`, `PosesDestination`, `PoseDetailDestination`), đi camera (`askPermission = false`), thư viện, chi tiết `w01`; gọi hàm: đích hiện tại là `CameraDestination(askPermission = false, poseId = "w01")` (`toRoute`), back stack có đúng một mục camera, không còn thư viện và chi tiết. `notOnDetail_doesNothing` (gọi khi đang ở camera: back stack không đổi). Thất bại: khung không điều hướng. Phần camera chọn tư thế đã có test ở T-014 (`PoseViewModelTest.poseIdArgument_selectsThatPose_andScrollsToIt`) |
| KB-4, quay lại về thư viện | Compose UI test | `PoseDetailScreenTest.back_callsOnBack` |
| KB-2 (T-021 để T-022 kiểm lại), quay lại từ chi tiết giữ chip và vị trí cuộn | Test Robolectric | `PosesBackStackTest.backFromDetail_keepsGridScroll`: `NavHost` thật, đích thư viện vẽ `PosesScreen` không trạng thái với `rememberLazyGridState()` trong mục back stack (như `PosesRoute`), cuộn tới thẻ 6, `navigate` sang đích giả, `popBackStack()`: `firstVisibleItemIndex == 6`. Chip đã có test khôi phục ở T-021 (`PosesViewModelTest.selectedFilter_restoredFromSavedStateHandle`) |
| KB-3, lưu và bỏ lưu ở chi tiết, `pose_saved` | Unit test, Compose UI test | `PoseDetailViewModelTest.toggleSave_savesOptimistically_andLogsPoseSaved` (`writeGate`: `saved == true` trước khi ghi xong; `setSaved("w01", true)`; sự kiện `saved` `true`), `toggleSaveOfSaved_unsaves_logsSavedFalse`, `toggleSaveWhileWriting_isIgnored`; `PoseDetailScreenTest.saveButton_describedBySavedState_andCallsOnToggleSave` ("Lưu tư thế", rồi "Bỏ lưu tư thế" khi `saved`) |
| KB-3 đặc biệt, ghi lỗi | Unit test, Compose UI test | `PoseDetailViewModelTest.saveFails_reverts_showsMessage_noEvent` (log `WARN` thẻ `Poses`, `assertTrue(entry.error is IOException)`); `PoseDetailScreenTest.saveFailed_showsSnackbar_thenCallsShown` |
| KB-5, ảnh lớn đang tải có vòng chờ | Unit test, Compose UI test | `PoseDetailViewModelTest.imagePending_isLoading_thenReady` (fake trễ 5 giây); `PoseDetailScreenTest.imageLoading_showsProgress` (node `ProgressBarRangeInfo.Indeterminate` trong `pose_detail_image`) |
| KB-5, ảnh lỗi: chữ, "Thử lại", các bước vẫn đọc được, `content_load_failed` | Unit test, Compose UI test | `PoseDetailViewModelTest.imageFails_isFailed_andLogsContentLoadFailedImage` cho `OTHER`, `NOT_FOUND`, `TIMEOUT` (`mapOf("content" to "image", "reason" to ...)`); `retryImage_loadsAgain_andLogs`; `PoseDetailScreenTest.imageFailed_showsMessageAndRetry_stepsStillShown`: "Không tải được ảnh", "Đã có lỗi khi tải ảnh. Thử lại sau ít phút.", bấm "Thử lại" gọi `onRetryImage`, chữ bước 1 vẫn có |
| KB-5, 15 giây | Unit test, test sẵn có | `PoseDetailViewModelTest.imageTimeout_isFailed_notLoadingForever`; mức 15 giây có test ở T-009 (`TimedAssetLoaderTest.noAnswerWithin15s_returnsTimeoutAtExactly15s`) |
| KB-5, mất mạng thì hộp thoại | Unit test | `PoseDetailViewModelTest.imageFailsNetwork_dialogShown_staysLoading` (vẫn ghi `content_load_failed` `network`), `imageFailsNetwork_dialogNotShown_isFailed`, `networkRecovered_reloadsFailedImage` |
| Ảnh lớn: đúng loại file, chuyển lỗi | Unit test | `DefaultPoseImageRepositoryTest.load_requestsImageFile_andDecodesAt1137`, `loadFileFailed_returnsSameFailure`, `decoderReturnsNull_returnsFailedOther` (qua ngay ở khung rỗng) |
| Log | Unit test | `PoseDetailViewModelTest.logs_useTagPoses_andNoPoseIdsOrNames` |
| Giao diện chi tiết, đúng thiết kế | Screenshot test cùng tên ảnh thiết kế, khung `w411dp-h891dp-xxhdpi`; ảnh so sánh `scripts/design-compare.sh poses`; rà soát độc lập theo mục 7 của README thiết kế | `PoseDetailScreen_default_light.png`, `PoseDetailScreen_default_dark.png` (`w01`, chưa lưu, ảnh trong suốt để thấy nền nhóm), `PoseDetailScreen_error_light.png`, `PoseDetailScreen_error_dark.png` bằng `captureRoboImage("src/test/screenshots/<Tên>.png") { ... }`. `PoseDetailScreen_loading_light.png`, `PoseDetailScreen_loading_dark.png` có vòng chờ chạy mãi (L-029): `runComposeUiTest { mainClock.autoAdvance = false; setContent { ... }; mainClock.advanceTimeBy(500); onRoot().captureRoboImage("src/test/screenshots/PoseDetailScreen_loading_light.png") }`, trong một lớp riêng `PoseDetailScreenScreenshotTest` không có `createComposeRule()` |
| Ảnh thiết kế của ticket khác | Không làm ở đây | Ảnh thiết kế của ticket khác (T-021): `PosesScreen_default_light`, `PosesScreen_default_dark`, `PosesScreen_loading_light`, `PosesScreen_loading_dark`, `PosesScreen_error_light`, `PosesScreen_error_dark`, `PosesScreen_saved-empty_light`, `PosesScreen_saved-empty_dark` |
| Ảnh thiết kế của ticket khác | Không làm ở đây | Ảnh thiết kế của ticket khác (T-023): `PosesScreen_ad_light`, `PosesScreen_ad_dark`, `PoseDetailScreen_ad_light`, `PoseDetailScreen_ad_dark` |
| Ảnh thiết kế của ticket khác | Không làm ở đây | Ảnh thiết kế của ticket khác (T-024): `PosesScreen_default_light_unfolded`, `PosesScreen_default_dark_unfolded`, `PoseDetailScreen_default_light_unfolded`, `PoseDetailScreen_default_dark_unfolded` |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` | `PoseDetailScreen_default_small.png` (`w05`: 5 bước, 2 lỗi, tên dài bị cắt một dòng), `PoseDetailScreen_error_small.png` (nút "Thử lại" và nút chính thấy đủ) |
| Chữ đúng bảng chữ | Compose UI test | Các test có chữ ở trên so đúng từng chữ của README mục 2 |
| Luồng thật trên máy | Kiểm tra thủ công | Staging, máy thật: (1) thư viện, chạm thẻ: chi tiết mở, ảnh lớn hiện sau vòng chờ; (2) cuộn hết nội dung, nút luôn thấy; (3) lưu ở chi tiết, quay lại: thẻ ở thư viện đã lưu, chip và vị trí cuộn giữ như trước; (4) bấm "Chụp với tư thế này": camera mở với tư thế đó, dải cuộn tới ô của nó; nút Back ở camera thoát app; (5) chế độ máy bay, mở chi tiết tư thế chưa tải ảnh lớn: hộp thoại mạng; tắt chế độ máy bay, "Thử lại": ảnh hiện; (6) chế độ tối; (7) tiếng Anh |

**Ngoại lệ viết test trước:** `PosesFeatureModule.kt` (một dòng `@Binds`), `AppNavHost.kt` (nối hai lambda vào hàm đã có test), biểu tượng không có test riêng; chúng được kiểm qua build, `AppLaunchTest` trên CI (đồ thị Hilt) và kiểm tra thủ công. Không thêm test emulator đi từ thư viện vào chi tiết: bản staging mới cài hiện hộp thoại đồng ý quảng cáo phủ lên app (CLAUDE.md, bước 10).

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 4 (ghi `pose_detail_opened`, `pose_selected`, `pose_saved`, `content_load_failed`, đều đã có trong spec mục 6). Không chạm điểm 1 (chưa đặt banner), 2, 3, 7 (không sửa module core), 8, 9. Rủi ro cao, bắt buộc duyệt chéo; người phụ trách sản phẩm biết trước vì có sự kiện phân tích.

Bảng điểm dưới đây chỉ để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Màn mới trong `:feature:poses` và hai file của `:app`, khoảng 30 file |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, thiết kế đã duyệt; AI tự quyết cách mở camera (câu 1), khi tư thế không còn (câu 3), nhiều lỗi hay gặp (câu 4) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi tiêu chí có test JVM; camera mở thật với tư thế và luồng mạng kiểm thủ công |
| D. Ảnh hưởng chéo | 2 | Sửa `AppNavHost.kt` mà T-016, T-020 có thể sửa; `strings.xml`, `PosesFakes.kt` mà T-023, T-024 sửa sau |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu đã có: tải ảnh như T-021, điều hướng `popUpTo` như màn chào, `NavHost` trong test như `NetworkDialogHostTest` |
| **Tổng** | 7 | **Mức: Cao** (điểm chặn 4) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `AppNavHost.kt`: nối `onOpenPose`, thêm `poseDetailScreen`.
  - `PosesFeatureModule.kt`, `strings.xml`, `PosesFakes.kt` của `:feature:poses`.
  - Dùng lại (không sửa) `PrimaryButton`, `StateMessage`, `ic_bookmark`, `ic_bookmark_added`, `ic_refresh` của `:core:designsystem`; `categoryColor`, `subjectLabel`, `screenLocale`, `PosesTopBar` (chỉ tham khảo, chi tiết có thanh trên riêng) của `:feature:poses`.
- Task khác có thể xung đột:
  - **T-023** (banner): sửa `PosesScreen.kt`, `detail/PoseDetailScreen.kt` mà plan này tạo, để đặt `AdSlot`. Chạy sau T-022.
  - **T-024** (máy gập): sửa `PosesScreen.kt`, `detail/PoseDetailScreen.kt`, `detail/PoseDetailComponents.kt` mà plan này tạo. Chạy sau T-022.
  - **T-015, T-017, T-020** (camera): sửa `:feature:camera`, không chạm file của plan này. Plan này không sửa camera, chỉ dùng `CameraDestination(poseId)` của T-014. Plan T-020 sửa `AppNavHost.kt` thì khác dòng (bố cục máy gập của camera nằm trong `:feature:camera`).
  - **T-018, T-019** (màn xem lại): có thể sửa `AppNavHost.kt` (`reviewScreen`); khác dòng.
- Đề xuất thứ tự chạy: T-022 trước T-023 và T-024 (hai plan đó viết trên code của plan này). Độc lập với các task camera.

## 7. Điều kiện dừng riêng

1. Cần sửa `:feature:camera` để camera mở đúng tư thế (ví dụ camera không đọc `poseId` từ tham số của đích như T-014 ghi).
2. Cần sửa `:core:poses` hay `:core:designsystem`.
3. `toRoute<CameraDestination>()` trên mục camera đang có ném lỗi vì kiểu tham số, và không đọc được `askPermission` cách khác trong `:app`.
4. Cần thêm thư viện để hiện ảnh lớn.

## 8. Câu hỏi cho người duyệt

1. **Mở camera bằng cách thay mục camera trên back stack.** Camera mới mở lại từ đầu: khung xem trước khởi động lại (khoảng nửa giây), mức zoom về 1x; ống kính, đèn, hẹn giờ, tư thế, chip giữ như cũ vì đọc từ DataStore hoặc từ `poseId`. Cách khác là đặt khóa vào mục camera đang có, nhưng phải sửa `:feature:camera` (đọc khóa từ `NavBackStackEntry.savedStateHandle` trong `CameraRoute`), file mà T-015, T-017, T-020 cũng sửa.
   - Đề xuất của AI: thay mục camera như mục 3.1.
2. **Đếm `pose_selected` `detail` ở đâu.** Plan T-014 (câu 3) đã chốt: chi tiết ghi lúc bấm nút, camera không ghi. Plan này làm đúng như vậy.
   - Đề xuất của AI: giữ như vậy.
3. **Tư thế không còn trong catalog khi đang ở chi tiết** (catalog đổi lúc mở màn, hoặc khôi phục sau khi app bị dừng). Spec không nêu.
   - Đề xuất của AI: về thư viện ngay, log `warn`, không hiện thông báo.
4. **Nhiều lỗi hay gặp.** Thiết kế có một khối với một đoạn chữ; catalog có danh sách lỗi.
   - Đề xuất của AI: một khối, tiêu đề "Lỗi hay gặp", mỗi lỗi một đoạn. Danh sách rỗng thì không có khối.
5. **Tên dài trên thanh trên.** Thiết kế có tên một dòng.
   - Đề xuất của AI: một dòng, cắt bằng dấu ba chấm; tên đầy đủ đã có ở thẻ của thư viện.
6. **Lưu ở chi tiết, quay lại chip "Đã lưu".** Theo T-021 (câu 3), thứ tự lưới cố định khi lưới đang hiện, nên tư thế vừa lưu ở chi tiết nằm cuối lưới "Đã lưu" tới khi đổi chip (nhật ký T-021).
   - Đề xuất của AI: giữ như vậy, cùng quy tắc với thư viện.
7. **Mô tả cho trình đọc màn hình của ảnh lớn.** Ảnh minh họa tư thế có tên ở ngay thanh trên.
   - Đề xuất của AI: không đặt mô tả cho ảnh (trình đọc màn hình bỏ qua), để không đọc tên hai lần.
8. **`token_estimate`.** Số gợi ý cỡ M là 15.4M, nhưng các task tạo màn hình mới gần đây dùng 37.7M đến 40.7M (T-014, T-016, T-021, T-025).
   - Đề xuất của AI: ghi 35M.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-23, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
