---
ticket: T-021
title: Thư viện tư thế: lưới, nhóm, đã lưu, tải và lỗi
status: approved                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 45M          # cỡ L, số gợi ý 9.5M; nâng lên vì là module feature mới như T-013 (thật 60.4M), xem mục 8 câu 12
blocking_points: [analytics, core]  # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                   # người duyệt
cross_reviewer: Tuấn              # bắt buộc khi risk_level là high
approved_at: 2026-10-22
---

# PLAN T-021: THƯ VIỆN TƯ THẾ: LƯỚI, NHÓM, ĐÃ LƯU, TẢI VÀ LỖI

## 1. Tóm tắt

Tạo module `:feature:poses` với màn thư viện tư thế: thanh trên (quay lại, tiêu đề, cài đặt), dòng tổng số, hàng chip "Tất cả", "Đã lưu" và các nhóm, lưới thẻ 2 cột; lưu và bỏ lưu trên thẻ; khung xương khi ảnh chưa có; lỗi catalog có "Thử lại"; "Đã lưu" rỗng có "Xem tất cả tư thế". Dữ liệu lấy từ `:core:poses` (T-009), ảnh nhỏ tải khi thẻ hiện và bỏ khi thẻ ra khỏi màn hình. Hai thành phần mới `PoseCard`, `PoseCardSkeleton` vào `:core:designsystem`; `StickerChip`, `SelectedBadge` dùng lại của T-014. Nút thư viện ở camera mở màn này khi cờ `feature_poses_enabled` bật. Kiểm chứng bằng unit test ViewModel với repository giả và thời gian ảo, test giao diện Robolectric, 8 screenshot test cùng tên ảnh thiết kế, 2 ảnh khung thấp và kiểm tra thủ công trên staging.

## 2. Hiểu yêu cầu

Ticket xử lý KB-1, KB-2, KB-3, KB-5 trong `features/poses/spec.md`. Phần của chi tiết tư thế (T-022), banner (T-023), máy gập (T-024) không làm ở đây.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Bấm nút thư viện ở camera thì thư viện mở, chip "Tất cả" đang chọn, lưới 2 cột | `:app` nối `onOpenPoses` của `cameraScreen` sang `PosesDestination` khi cờ bật; cờ tắt thì truyền `null`, nút ẩn (camera đã có sẵn cách ẩn này từ T-013). Bộ lọc mặc định `All` |
| KB-1 | Thứ tự: đã lưu, gần đây (mới nhất trước), còn lại theo catalog; cho "Tất cả" và từng nhóm | Thứ tự tính trong `:core:poses` (`PoseLibrary.posesFor`, đã có test ở T-009). ViewModel giữ đúng thứ tự đó. Thứ tự chụp lại một lần khi lưới hiện cho một chip, không nhảy khi người dùng bấm lưu ngay trên màn (mục 8 câu 3) |
| KB-1 | Dòng dưới tiêu đề ghi tổng số tư thế và số nhóm | `"%1$d tư thế trong %2$d nhóm"` với `totalPoses` và số nhóm có chip (spec câu hỏi 3) |
| KB-1 | Chỉ nhóm có tư thế mới có chip | `PoseLibrary.categories` đã bỏ nhóm rỗng (T-009) |
| KB-1 | Thẻ có ảnh, tên, loại và số bước, nút lưu; tên đọc được | Tên `LocalizedText.resolve` theo ngôn ngữ của màn hình (đọc trong composable, không trong ViewModel, để đổi ngôn ngữ không giữ chữ cũ). Dòng phụ "<loại> · %d bước" với loại Một người, Cặp đôi, Nhóm theo `SubjectType` và số bước là số mục của `steps` |
| KB-1 | Bấm quay lại thì về camera, giữ nguyên tư thế đang dùng | `popBackStack()`; mục camera trên back stack giữ ViewModel nên giữ tư thế (tư thế của camera thuộc T-014) |
| KB-1 | Bấm nút cài đặt thì màn cài đặt mở | `PosesScreen` nhận `onOpenSettings: (() -> Unit)?`; `null` thì ẩn nút. Nối vào `SettingsDestination` khi `:feature:settings` (T-025) đã có trên nhánh chính lúc chạy task; chưa có thì truyền `null` và T-025 nối (mục 6) |
| KB-1, đặc biệt | Ảnh chưa tải xong thì thẻ hiện khung xương; xong thì hiện ảnh, không xô lệch lưới | Thẻ có ảnh `Ready` vẽ `PoseCard`, còn lại vẽ `PoseCardSkeleton` cùng cỡ thẻ (README mục 3). Vùng ảnh giữ tỷ lệ 162 x 210 nên không đổi cỡ khi ảnh về (mục 8 câu 6) |
| KB-2 | Chạm chip nhóm thì lưới chỉ còn tư thế của nhóm; chip chọn có nền nhấn và dấu ✓ | `PoseFilter.Category(id)`; `StickerChip(selected = true)` vẽ nền `secondaryContainer` và `SelectedBadge` (T-014). Đổi chip thì cuộn về đầu lưới |
| KB-2 | Quay lại từ chi tiết thì chip và vị trí cuộn giữ như trước | Chip lưu trong `SavedStateHandle` của ViewModel; vị trí cuộn là `rememberLazyGridState` (lưu theo trạng thái của mục back stack). Chi tiết chưa có (T-022), nên kiểm bằng `StateRestorationTester` và test khôi phục ViewModel; T-022 kiểm lại trên luồng thật |
| KB-3 | Bấm lưu trên thẻ thì lưu, nút đổi trạng thái; bấm lại thì bỏ; mỗi lần đổi ghi `pose_saved` | Phần "ở chi tiết" thuộc T-022. Nút đổi ngay (lạc quan), gọi `PoseHistoryRepository.setSaved`; ghi xong thì ghi `pose_saved` (`pose_id`, `saved` là `true` hoặc `false`) |
| KB-3 | Chip "Đã lưu" hiện tư thế đã lưu, mới lưu nhất ở đầu | `posesFor(PoseFilter.Saved)` (T-009) |
| KB-3 | Chưa lưu gì và chạm "Đã lưu" thì hiện tiêu đề, lời hướng dẫn, nút "Xem tất cả tư thế" về "Tất cả" | `Ready` với bộ lọc `Saved` và danh sách rỗng; `StateMessage` có nút gọi `onShowAll()` |
| KB-3 | Mở lại app thì danh sách đã lưu còn | DataStore của `:core:poses` (T-009, `DataStorePoseHistoryRepositoryTest.setSavedTrueThenFalse_persistsAcrossRepositoryInstances`); plan chỉ dùng lại |
| KB-3, đặc biệt | Tư thế đã lưu không còn trong catalog thì không hiện | Đã làm ở T-009 (`PoseOrderingTest.savedPoseMissingFromCatalog_isNotShown`) |
| KB-3, đặc biệt | Ghi đã lưu lỗi thì nút về trạng thái cũ và có thông báo ngắn | Bắt `IOException`, bỏ trạng thái lạc quan, hiện `Snackbar` "Chưa lưu được tư thế. Thử lại sau." (chữ chưa có trong bảng chữ, mục 8 câu 5); không ghi `pose_saved` |
| KB-5 | Thẻ bộ khởi đầu hiện ảnh ngay | Ảnh đã có trên máy: `loadFile` trả ngay, chỉ còn giải mã trên luồng IO; không chờ mạng |
| KB-5 | Đang tải thêm ảnh thì thẻ chưa có ảnh hiện khung xương | Như KB-1, đặc biệt |
| KB-5 | Đọc catalog lỗi (không do mất mạng) thì hiện tiêu đề, lời hướng dẫn, "Thử lại"; ghi `content_load_failed` | `LibraryState.Failed`: `Error` với `StateMessage`; ghi `content_load_failed` (`content` là `catalog`, `reason` theo `LoadFailureReason.analyticsValue`) một lần mỗi lần chuyển sang lỗi. Ảnh nhỏ lỗi ghi `content` là `thumbnail` |
| KB-5 | Ảnh lớn ở chi tiết | T-022, ngoài phạm vi |
| KB-5 | Không phản hồi sau 15 giây thì là lỗi | Mức chờ 15 giây nằm trong `:core:poses` (ADR-1001, T-009). Lỗi `TIMEOUT` của catalog hiện `Error`; của ảnh nhỏ thì thẻ giữ khung xương, chạm để thử lại (spec mục 7) |
| KB-5 | Lỗi do mất mạng thì hiện hộp thoại mạng thay cho thông báo riêng | `:core:assets` đã báo hộp thoại (T-011). Lỗi catalog lý do `NETWORK` khi `NetworkStatus.dialogShown` đang đúng: trạng thái `Loading` (khung xương) dưới hộp thoại, không hiện thông báo riêng. Hộp thoại đóng bằng "Thử lại" (`recovered`) thì tải lại catalog và các ảnh nhỏ đang lỗi. Hộp thoại không hiện (cờ của nó tắt) thì hiện `Error` như lỗi khác, để không chờ mãi |
| Ticket | Cờ `feature_poses_enabled` | `FeatureFlag.POSES`, mặc định tắt, bật ở staging. Cờ tắt khi đang ở thư viện thì màn gọi `onBack` một lần |

Thiết kế tham chiếu: `features/poses/design/README.md`, các ảnh trong `design/screens/`: `PosesScreen_default_light`, `PosesScreen_default_dark`, `PosesScreen_loading_light`, `PosesScreen_loading_dark`, `PosesScreen_error_light`, `PosesScreen_error_dark`, `PosesScreen_saved-empty_light`, `PosesScreen_saved-empty_dark`; số liệu trong `design/context/` cùng tên. Thành phần theo `docs/design-system/sticker.md`, mục 4.

ADR liên quan: ADR-0001 (module feature chỉ phụ thuộc core), ADR-0003 (cờ tính năng theo dõi bằng `Flow`), ADR-0008 (thành phần mới trong `:core:designsystem`, không viết cứng màu, cỡ chữ), ADR-0012 (sự kiện qua `:core:analytics`), ADR-0015 (ảnh qua `:core:poses` và `RemoteAssets`, không viết đường dẫn trong feature), ADR-0019 (log qua `AppLogger`), ADR-0020 (tên screenshot test theo ảnh thiết kế, ảnh so sánh), ADR-1001 (15 giây).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu `:feature:sample` và `:feature:camera` (T-013): repository trong `data/`, Hilt module trong `di/`, `UiState` sealed, ViewModel trả `StateFlow`, `PosesRoute` nối ViewModel với `PosesScreen` không trạng thái, đích `@Serializable` trong `navigation/`. Gói gốc `com.vglstudio.camera.pose.line.guide.feature.poses`. Thẻ log `Poses` (trùng thẻ của `:core:poses`, mục 8 câu 10).

**Module.** `feature/poses/build.gradle.kts`: plugin `template.android.feature`, `namespace`, `implementation(project(":core:poses"))` (CLAUDE.md, dòng `:core:poses`) và `implementation(project(":core:network"))` (đọc `NetworkStatus`, như camera). Không thêm thư viện: ảnh giải mã bằng `BitmapFactory`, không dùng thư viện tải ảnh.

**Dữ liệu (`data/`).**

- `PoseLibrary` của `:core:poses` có constructor `internal`, nên test của feature không dựng được nó. Feature có kiểu riêng, dựng được trong test:
  - `data class LibraryContent(val categories: List<PoseCategory>, val totalPoses: Int, val savedIds: Set<String>, private val lists: Map<PoseFilter, List<Pose>>)` với `fun posesFor(filter: PoseFilter): List<Pose>` (bộ lọc không có trong `lists` thì rỗng).
  - `sealed interface LibraryResult { Loading; Ready(content: LibraryContent); Failed(failure: PoseLoadFailure) }`.
  - `interface PosesLibrarySource { val library: Flow<LibraryResult>; suspend fun retry(); suspend fun setSaved(poseId: String, saved: Boolean) }` (`setSaved` ném `IOException`).
  - `internal class DefaultPosesLibrarySource @Inject constructor(PoseLibraryRepository, PoseHistoryRepository)`: đổi `LibraryState` sang `LibraryResult`; `Ready` dựng `LibraryContent` với `lists` cho `All`, `Saved` và từng nhóm của `categories`. Chỉ chép dữ liệu, không tính lại thứ tự.
- Ảnh nhỏ:
  - `sealed interface ThumbnailResult { Ready(image: ImageBitmap); Failed(failure: PoseLoadFailure) }`.
  - `interface PoseThumbnailRepository { suspend fun load(pose: Pose): ThumbnailResult }`.
  - `internal class DefaultPoseThumbnailRepository @Inject constructor(PoseCatalogRepository, PoseImageDecoder, @PosesIoDispatcher dispatcher)`: `loadFile(pose, PoseFile.THUMBNAIL)`; `Ready(file)` thì giải mã trên dispatcher IO; giải mã ra `null` thì `Failed(PoseLoadFailure(THUMBNAIL, OTHER))`; `Failed` chuyển nguyên.
  - `fun interface PoseImageDecoder { fun decode(file: File, targetWidthPx: Int): ImageBitmap? }` và `internal class BitmapPoseImageDecoder`: `BitmapFactory` đọc kích thước trước, `inSampleSize` là lũy thừa 2 lớn nhất mà ảnh vẫn rộng ít nhất `targetWidthPx`, `Bitmap.Config.RGB_565` (ảnh không cần trong suốt), lỗi đọc thì `null`. `targetWidthPx` cố định 486 (thẻ rộng 162 dp ở xxhdpi), ảnh `preview_540` giữ nguyên cỡ.
- `di/PosesFeatureModule.kt` (`internal`): `@Binds` hai repository, cung cấp `BitmapPoseImageDecoder` và dispatcher IO bằng qualifier `internal` `PosesIoDispatcher` của feature. Tên module khác `PosesModule` của `:core:poses`.

**Trạng thái (`PosesUiState.kt`).**

```kotlin
sealed interface PosesUiState {
    data object Loading : PosesUiState              // cờ chưa biết, catalog đang tải, hay lỗi mạng dưới hộp thoại
    data object Disabled : PosesUiState
    data class Error(val reason: LoadFailureReason) : PosesUiState
    data class Ready(
        val totalPoses: Int,
        val chips: List<PoseChip>,                  // All, Saved, rồi Category theo thứ tự catalog
        val selected: PoseFilter,
        val cards: List<PoseCardItem>,              // rỗng với Saved là trạng thái "đã lưu rỗng"
        val saveFailed: Boolean = false,
    ) : PosesUiState
}
sealed interface PoseChip { data object All; data object Saved; data class Category(val id: String, val name: LocalizedText) }
data class PoseCardItem(val id: String, val categoryId: String, val name: LocalizedText, val subjectType: SubjectType,
    val stepCount: Int, val saved: Boolean, val thumbnail: ThumbnailState)
sealed interface ThumbnailState { data object Loading; data class Ready(val image: ImageBitmap); data object Failed }
```

Số nhóm của dòng tổng số là số `PoseChip.Category`.

**ViewModel (`PosesViewModel`).** Nhận `PosesLibrarySource`, `PoseThumbnailRepository`, `NetworkStatus`, `Analytics`, `AppLogger`, `FeatureFlags`, `SavedStateHandle`.

- Cờ: `featureFlags.enabled(FeatureFlag.POSES)` thu theo luồng (ADR-0003); `null` là `Loading`, `false` là `Disabled`.
- Bộ lọc: `MutableStateFlow<PoseFilter>` khởi đầu từ `SavedStateHandle["poses_filter"]` (`all`, `saved`, `category:<id>`), ghi lại mỗi lần đổi. Bộ lọc nhóm không còn trong `chips` thì dùng `All`.
- Thứ tự chụp lại: khi một bộ lọc hiện lần đầu (hoặc chọn lại), ViewModel giữ danh sách mã theo thứ tự của `posesFor(filter)` lúc đó. Lần phát mới của thư viện (sau khi lưu, bỏ lưu) chỉ cập nhật cờ `saved` và nội dung thẻ theo mã, không đổi thứ tự; mã không còn trong catalog thì bỏ; mã mới xuất hiện trong bộ lọc (catalog đổi khi đang mở màn) thêm vào cuối. Đổi chip, rời màn rồi vào lại thì tính lại theo thứ tự mới. Ở "Đã lưu", bỏ lưu một thẻ thì thẻ vẫn nằm đó ở trạng thái chưa lưu tới khi đổi chip (mục 8 câu 3).
- Lưu: `onToggleSave(id)`: ghi đè lạc quan `saved` của thẻ; `setSaved(id, !saved)`; xong thì bỏ ghi đè (thư viện đã phát giá trị mới), ghi `pose_saved`, log `info` "Pose saved: true/false" (không ghi mã, tên). `IOException`: bỏ ghi đè, `saveFailed = true`, log `warn` kèm lỗi. Mỗi mã chỉ một lần ghi đang chạy; bấm khi đang ghi thì bỏ qua. `onSaveFailedShown()` đặt `saveFailed = false`.
- Ảnh nhỏ: màn gọi `onThumbnailNeeded(id)` khi thẻ vào lưới (`LaunchedEffect`) và `onThumbnailReleased(id)` khi thẻ rời lưới (`DisposableEffect`). Cần mà chưa có thì `Loading` và tải (tối đa 4 lần tải cùng lúc, `Semaphore`); nhả thì hủy lần tải đang chờ và bỏ ảnh khỏi bộ nhớ, lần cần sau tải lại (đã có trên máy nên nhanh). `Failed`: thẻ giữ khung xương, ghi `content_load_failed` (`thumbnail`, lý do), log `warn`; `onRetryThumbnail(id)` tải lại. Thẻ không cần nữa thì không giữ `ImageBitmap`, nên bộ nhớ chỉ chứa ảnh của các thẻ đang có trong lưới.
- Catalog: `LibraryResult.Failed`: ghi `content_load_failed` (`catalog`, lý do) và log `warn` khi chuyển từ trạng thái khác sang lỗi (không ghi lại khi cùng lỗi phát lại). Lý do `NETWORK` và `networkStatus.dialogShown.value` đúng thì `Loading`, ngược lại `Error(reason)`. `onRetry()`: log `info` "Retrying catalog", gọi `retry()`.
- `networkStatus.recovered`: tải lại catalog nếu đang lỗi, và các ảnh nhỏ đang `Failed` mà thẻ còn trong lưới.
- `onSelectFilter(filter)`, `onShowAll()` (bằng `onSelectFilter(All)`): log `info` "Filter: all/saved/category" (không ghi mã nhóm).

**Màn hình (`PosesScreen.kt`, `PosesComponents.kt`).**

- `PosesRoute(onBack, onOpenSettings, onOpenPose, viewModel = hiltViewModel())`: thu `uiState` bằng `collectAsStateWithLifecycle`, giữ `rememberLazyGridState()`, chuyển các hàm của ViewModel.
- `PosesScreen(uiState, gridState, onBack, onOpenSettings: (() -> Unit)?, onOpenPose: ((String) -> Unit)?, onSelectFilter, onShowAll, onToggleSave, onRetry, onThumbnailNeeded, onThumbnailReleased, onRetryThumbnail, onSaveFailedShown, modifier)`. Nền `background`.
- Thanh trên: `TopAppBar` của Material 3, cao 64; trái nút quay lại (vùng chạm 48, icon `arrow_back` 24); tiêu đề `titleLarge` màu `onSurface`; phải nút cài đặt (icon `settings` 24) khi `onOpenSettings` khác `null`.
- `Ready`: dòng tổng số `bodySmall` màu `onSurfaceVariant`, lề trái `Spacing.md`; hàng chip `LazyRow` cuộn ngang, lề `Spacing.md` hai bên, trên dưới `Spacing.sm`, cách nhau `Spacing.sm`, `StickerChip` của T-014; lưới `LazyVerticalGrid(GridCells.Fixed(2))`, lề ngang `Spacing.md`, lề trên `Spacing.xs`, cách nhau `Spacing.md` hai chiều, có `contentPadding` dưới `Spacing.md` để thẻ cuối không sát đáy. Bấm chip: `onSelectFilter`, rồi `gridState.scrollToItem(0)` trong coroutine của màn (chỉ khi người dùng bấm, nên quay lại từ chi tiết không cuộn về đầu).
- Thẻ: `ThumbnailState.Ready` thì `PoseCard(name = item.name.resolve(locale), meta, imageBackground = categoryColor(item.categoryId), image = BitmapPainter(image), saved, saveLabel, onSaveClick, onClick = onOpenPose?.let { { it(item.id) } })`. `Loading` thì `PoseCardSkeleton()`; `Failed` thì `PoseCardSkeleton(onClick = { onRetryThumbnail(id) }, onClickLabel = "Tải lại ảnh")`. `locale` lấy từ `LocalConfiguration.current.locales[0]`. Mỗi thẻ có `testTag("poses_card_<id>")`.
- Màu nền ảnh theo nhóm (`categoryColor`, trong `PosesComponents.kt`): mã nhóm của catalog sang `MaterialTheme.appColors`: `women` sang `categoryWomen`, `men` sang `categoryMen`, `couples` sang `categoryCouple`, `friends`, `family`, `kids`, `travel`, `cafe` sang màu cùng tên; mã khác dùng `surfaceVariant` (mục 8 câu 4).
- `Ready` với `Saved` và `cards` rỗng: dòng tổng số và chip vẫn hiện; vùng dưới là `StateMessage(icon bookmark, "Chưa lưu tư thế nào", "Chạm biểu tượng lưu trên thẻ tư thế để giữ lại ở đây.", actionLabel = "Xem tất cả tư thế", onAction = onShowAll)` giữa vùng còn lại, cuộn được khi màn thấp.
- `Error`: thanh trên và `StateMessage(icon error, "Không tải được thư viện", "Đã có lỗi khi tải thư viện. Thử lại sau ít phút.", actionLabel = "Thử lại", actionIcon = refresh, onAction = onRetry)` giữa vùng còn lại. Không có dòng tổng số và chip vì catalog chưa đọc được (khác ảnh thiết kế, mục 8 câu 2).
- `Loading`: thanh trên và lưới 6 `PoseCardSkeleton`, không có dòng tổng số và chip (thiết kế không có trạng thái này, mục 8 câu 13).
- `Disabled`: chỉ thanh trên; `LaunchedEffect(Unit) { onBack() }`.
- `saveFailed`: `SnackbarHost` ở đáy hiện chữ "Chưa lưu được tư thế. Thử lại sau.", hiện xong gọi `onSaveFailedShown`.
- Chữ trong `res/values/strings.xml` theo bảng chữ, mô tả cho trình đọc màn hình: "Quay lại", "Cài đặt", "Lưu tư thế", "Bỏ lưu tư thế", "Tải lại ảnh".

**Thành phần mới trong `:core:designsystem`** (`component/PoseCard.kt`, theo `sticker.md` mục 4 và README mục 3):

- `PoseCard(name: String, meta: String, image: Painter, imageBackground: Color, saved: Boolean, saveLabel: String, onSaveClick: () -> Unit, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null)`: `Column` nền `surface`, viền `Stroke.sticker` màu `outline`, bo `Radius.card`, lề trong `Spacing.sm`, cách nhau `Spacing.sm`. Vùng ảnh rộng hết, `aspectRatio(162f / 210f)`, nền `imageBackground`, viền `Stroke.sticker` `outline`, bo `Radius.image`, ảnh `ContentScale.Fit`. Nút lưu ở góc trên phải vùng ảnh: vùng chạm 48, vòng 36 viền `Stroke.sticker` `outline`, nền `surface` khi chưa lưu, `secondaryContainer` khi đã lưu, icon 20 `bookmark` hoặc `bookmark_added` màu `onSecondaryContainer` khi đã lưu, `onSurface` khi chưa; ngữ nghĩa `Role.Button`, `contentDescription = saveLabel`, `toggleableState`. Chữ: lề phải, trái, dưới `Spacing.xs`, cách nhau 2 dp (giá trị ngoài hệ thống của thiết kế, làm tròn về `Spacing.xs / 2`, mục 8 câu 6); tên `titleMedium` `onSurface`, tối đa 2 dòng, cắt bằng dấu ba chấm; dòng phụ `bodySmall` `onSurfaceVariant`, một dòng. `onClick` khác `null` thì cả thẻ bấm được (cho T-022).
- `PoseCardSkeleton(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, onClickLabel: String? = null)`: cùng khung ngoài với `PoseCard` có tên một dòng; nền `surface`, viền `Stroke.sticker` màu `outlineVariant`, bo `Radius.card`; vùng ảnh cùng tỷ lệ, nền `surfaceContainerHighest`, bo `Radius.image`; vùng chữ cao bằng vùng chữ một dòng của `PoseCard`, chứa thanh tên 120 x 16 và thanh dòng phụ 80 x 12 nền `surfaceContainerHighest`, bo `Radius.full`. Không có chữ cho trình đọc màn hình ngoài `onClickLabel`.
- Biểu tượng `ic_bookmark.xml`, `ic_bookmark_added.xml` trong `core/designsystem/src/main/res/drawable/`, Material Symbols Outlined, cùng cách và giấy phép đã có trong `core/designsystem/third_party/material-symbols/` (T-007, T-013). Feature dùng lại `ic_bookmark` cho vòng icon của "đã lưu rỗng" và `ic_refresh` có sẵn cho nút "Thử lại".
- Catalog: trang `PoseCardCatalog` (nền `background`; tên khác trang `PoseComponentCatalog` mà plan T-014 thêm): `PoseCard` chưa lưu, đã lưu, tên dài hai dòng; `PoseCardSkeleton`. Screenshot test mới `poseCards_light`. Các trang cũ không đổi.

Biểu tượng của feature: `ic_poses_arrow_back.xml`, `ic_poses_settings.xml`, `ic_poses_error.xml` trong `feature/poses/src/main/res/drawable/`, Material Symbols Outlined (Apache 2.0, repo `google/material-design-icons`), chú thích đầu file ghi tên, nguồn và commit SHA như T-013; giấy phép chép vào `feature/poses/third_party/material-symbols/LICENSE.txt`. Tải không được là lỗi hạ tầng (auto-run.md, bước 5).

**`StickerChip`, `SelectedBadge`.** Dùng của T-014 (ticket T-021, phạm vi: "dùng ... từ T-013, T-014 nếu đã có"). Plan giả định T-014 đã merge trước T-021 và `StickerChip` nhận ít nhất chữ, `selected`, `onClick`, `modifier`, tự vẽ `SelectedBadge` khi chọn. Không có thì dừng (mục 7, điều 1; mục 8 câu 1). `StateMessage` có sẵn từ T-013.

**Điều hướng và cờ.**

- `FeatureFlag.POSES("feature_poses_enabled")`; thêm vào `StagingFeatureFlagsModule`.
- `navigation/PosesNavigation.kt`: `@Serializable data object PosesDestination`; `fun NavGraphBuilder.posesScreen(onBack: () -> Unit, onOpenSettings: (() -> Unit)? = null, onOpenPose: ((String) -> Unit)? = null)`.
- `:app`:
  - `navigation/PosesEntry.kt` (mới): `internal fun posesOpener(enabled: Boolean, open: () -> Unit): (() -> Unit)? = if (enabled) open else null`.
  - `MainActivity.kt`: tiêm `FeatureFlags`; `val posesEnabled by featureFlags.enabled(FeatureFlag.POSES).collectAsState(initial = false)`; truyền `posesEnabled` vào `AppNavHost`. Theo dõi luồng, không đọc một lần, vì nút ở camera phải ẩn hiện theo giá trị từ xa (ADR-0003).
  - `navigation/AppNavHost.kt`: thêm tham số `posesEnabled: Boolean = false`; `cameraScreen(onPhotoCaptured = ..., onOpenPoses = posesOpener(posesEnabled) { navController.navigate(PosesDestination) { launchSingleTop = true } }, onOpenSettings = <giữ như lúc chạy task>)`; `posesScreen(onBack = { navController.popBackStack() }, onOpenSettings = <openSettings của T-025 nếu T-025 đã merge, không thì null>)`. Plan T-025 thêm tham số `settingsEnabled` và giá trị `openSettings` trong `AppNavHost.kt`; task nào chạy sau thì nối nút cài đặt của thư viện (đã thống nhất với plan T-025, mục 6). `onOpenPose` để `null` (T-022 nối). Giữ nguyên phần của T-010, T-011, T-013 và của các plan khác đã merge.
  - `app/build.gradle.kts`: `implementation(project(":feature:poses"))`.

**Thứ tự làm (L-025).** Bước 3 (commit test): `settings.gradle.kts`, `feature/poses/build.gradle.kts`, `consumer-rules.pro`, `robolectric.properties`, `strings.xml` đủ chữ, `FeatureFlag.POSES`, các kiểu của `PosesUiState.kt` và `data/` đầy đủ chữ ký; khung rỗng: `PosesViewModel` có đủ hàm nhưng không làm gì, `uiState` luôn `Loading`; `DefaultPosesLibrarySource.library` là `flowOf(LibraryResult.Loading)`; `DefaultPoseThumbnailRepository.load` luôn trả `Failed(THUMBNAIL, OTHER)`; `BitmapPoseImageDecoder.decode` luôn `null`; `PoseCard`, `PoseCardSkeleton`, `PoseCardCatalog` vẽ `Box(modifier)` rỗng; `PosesScreen` chỉ vẽ `Box` mang `testTag(SCREEN)`; `posesOpener` luôn trả `null`; `posesScreen()` đăng ký `PosesRoute`. Mọi file test và bản giả. Bước 4 (commit code): cài đặt thật, biểu tượng và giấy phép, `MainActivity.kt`, `AppNavHost.kt`, `app/build.gradle.kts`, `StagingFeatureFlagsModule.kt`, ghi ảnh chuẩn (`./gradlew recordRoborazziDebug`), `scripts/design-compare.sh poses`, build `stagingDebug`.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

Đường dẫn gói viết tắt: `.../poses` là `com/vglstudio/camera/pose/line/guide/feature/poses`; `.../designsystem` là `com/vglstudio/camera/pose/line/guide/core/designsystem`; `.../guide` là `com/vglstudio/camera/pose/line/guide`.

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `settings.gradle.kts` | `include(":feature:poses")` sau các feature đã có |
| Tạo | `feature/poses/build.gradle.kts` | Plugin `template.android.feature`, `namespace`, `:core:poses`, `:core:network` |
| Tạo | `feature/poses/consumer-rules.pro` | Rỗng, như các feature khác |
| Tạo | `feature/poses/third_party/material-symbols/LICENSE.txt` | Giấy phép Apache 2.0 của Material Symbols |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreen.kt` | `PosesRoute`, `PosesScreen`, `PosesTestTags` |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesComponents.kt` | Thanh trên, hàng chip, ô lưới, `categoryColor`, dòng phụ của thẻ |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesUiState.kt` | `PosesUiState`, `PoseChip`, `PoseCardItem`, `ThumbnailState` |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesViewModel.kt` | ViewModel theo mục 3.1 |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/PosesLibrarySource.kt` | Interface, `LibraryContent`, `LibraryResult` |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/DefaultPosesLibrarySource.kt` | Cài đặt trên `PoseLibraryRepository`, `PoseHistoryRepository` |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/PoseThumbnailRepository.kt` | Interface, `ThumbnailResult` |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/DefaultPoseThumbnailRepository.kt` | Tải qua `PoseCatalogRepository.loadFile`, giải mã |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/PoseImageDecoder.kt` | `PoseImageDecoder`, `BitmapPoseImageDecoder` |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/di/PosesFeatureModule.kt` | Hilt module, qualifier `PosesIoDispatcher` |
| Tạo | `feature/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/navigation/PosesNavigation.kt` | `PosesDestination`, `posesScreen()` |
| Tạo | `feature/poses/src/main/res/values/strings.xml` | Chữ theo bảng chữ và mô tả cho trình đọc màn hình |
| Tạo | `feature/poses/src/main/res/drawable/ic_poses_arrow_back.xml`, `ic_poses_settings.xml`, `ic_poses_error.xml` | Ba biểu tượng Material Symbols |
| Tạo | `feature/poses/src/test/resources/robolectric.properties` | `sdk=35`, như các feature khác |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/MainDispatcherRule.kt` | Chép từ `:feature:sample` |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesFakes.kt` | `FakePosesLibrarySource`, `FakePoseThumbnailRepository`, `FakeNetworkStatus`, `FakeAnalytics`, `FakeFeatureFlags`, `testPose()`, `testContent()` |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesViewModelTest.kt` | Unit test ViewModel (mục 4) |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenTest.kt` | Test giao diện Robolectric (mục 4) |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenScreenshotTest.kt` | 10 screenshot test |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/DefaultPoseThumbnailRepositoryTest.kt` | Unit test (mục 4) |
| Tạo | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/data/BitmapPoseImageDecoderTest.kt` | Test Robolectric (mục 4) |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_default_light.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_default_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_loading_light.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_loading_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_error_light.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_error_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_saved-empty_light.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_saved-empty_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_default_small.png` | Khung 360 x 640, nhiều nội dung nhất (L-028) |
| Tạo | `feature/poses/src/test/screenshots/PosesScreen_saved-empty_small.png` | Khung 360 x 640, nút "Xem tất cả tư thế" không bị che |
| Tạo | `docs/features/poses/design/review/` | Sinh bởi `scripts/design-compare.sh poses` (ảnh ghép và `summary.md`), không sửa tay |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PoseCard.kt` | `PoseCard`, `PoseCardSkeleton` |
| Tạo | `core/designsystem/src/main/res/drawable/ic_bookmark.xml`, `ic_bookmark_added.xml` | Hai biểu tượng Material Symbols |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/catalog/DesignSystemCatalog.kt` | Thêm `PoseCardCatalog`; trang cũ không đổi |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/PoseCardTest.kt` | Test giao diện hai thành phần (file riêng, không chạm `ComponentsTest.kt`, `CameraComponentsTest.kt`) |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignSystemCatalogScreenshotTest.kt` | Thêm test `poseCards_light` |
| Tạo | `core/designsystem/src/test/screenshots/DesignSystem_pose_cards_light.png` | Ảnh chuẩn mới |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FeatureFlag.kt` | `POSES("feature_poses_enabled")` |
| Sửa | `app/build.gradle.kts` | `implementation(project(":feature:poses"))` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | Tiêm `FeatureFlags`, theo dõi `FeatureFlag.POSES`, truyền `posesEnabled` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/AppNavHost.kt` | Tham số `posesEnabled`; nối nút thư viện của camera; đăng ký `posesScreen` |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/PosesEntry.kt` | `posesOpener` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/navigation/PosesEntryTest.kt` | Unit test (mục 4) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingFeatureFlagsModule.kt` | Bật `FeatureFlag.POSES` ở staging |

Không file nào khác. Đặc biệt không sửa `:core:poses` (dùng API công khai của T-009), `:feature:camera` (đã có `onOpenPoses`, `onOpenSettings`), `ComponentsTest.kt`, `CameraComponentsTest.kt`, `docs/design-system/tokens.json` (không thêm token), `gradle/libs.versions.toml`, `build-logic/`, `AndroidManifest.xml` (không thêm quyền). Nhật ký plan, trạng thái plan và ticket đi qua nhánh báo cáo của phiên (auto-run.md, mục 4).

**Nơi khác đọc các file trên** (L-020), không cần sửa:

- `FeatureFlag.kt`: `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/DefaultFeatureFlagsTest.kt` duyệt `FeatureFlag.entries`; cờ mới mặc định tắt nên test vẫn qua.
- `DesignSystemCatalog.kt`: chỉ `DesignSystemCatalogScreenshotTest.kt` (trong danh sách) gọi; trang cũ không đổi nên các ảnh catalog cũ không đổi.
- `MainActivity.kt`, `AppNavHost.kt`: `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt` mở `MainActivity` và chờ `camera_screen`; màn chính vẫn là camera, nên không bị ảnh hưởng. `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/network/NetworkDialogHostTest.kt` và `MainScreenTest.kt` không gọi `AppNavHost` hay `MainActivity`.
- `StagingFeatureFlagsModule.kt`: chỉ Hilt đọc; thêm một hàm `@IntoSet`.
- `settings.gradle.kts`, `app/build.gradle.kts`: `checkModuleDependencies` đọc phụ thuộc lúc build; `:feature:poses` chỉ phụ thuộc core. Repo app này không có `scripts/new-app.sh`. `scripts/release.sh` chỉ đọc `applicationId`, `versionCode`, `versionName`; plan không đổi ba giá trị đó và không chạm script.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: `feature_poses_enabled` (spec mục 8), mặc định tắt, bật sẵn ở staging. Quyết định nút thư viện ở camera (theo dõi luồng trong `MainActivity`) và màn thư viện (ViewModel theo dõi, tắt thì về camera).
- Quảng cáo: không chạm. `poses_banner` là T-023; màn này chưa đặt `AdSlot`.
- Sự kiện phân tích (điểm chặn 4, đã có trong spec mục 6):
  - `pose_saved`: `pose_id` (mã tư thế của catalog, không phải dữ liệu cá nhân), `saved` là `true` hoặc `false` (cùng định dạng `matched` của `photo_captured`, T-013).
  - `content_load_failed`: `content` là `catalog` hoặc `thumbnail`; `reason` là `network`, `not_found`, `timeout`, `other` (`LoadFailureReason.analyticsValue`). `image` thuộc T-022.
  - `pose_detail_opened`, `pose_selected` thuộc T-022.
- Dữ liệu cục bộ: không thêm khóa. Đã lưu và đã dùng gần đây là hai khóa của T-009 (`poses_saved`, `poses_recent`). Bộ lọc đang chọn nằm trong `SavedStateHandle` (trạng thái của màn, không phải DataStore).
- File R2: không chạm. Ảnh nhỏ đọc qua `PoseCatalogRepository.loadFile`, đường dẫn nằm trong catalog (ADR-0015).

## 4. Kiểm thử

Mọi test dưới đây vào commit test của bước 3, cùng khung rỗng ở mục 3.1. Lý do thất bại ở cột cuối là điều AI dự kiến; bước 3 chạy và ghi lý do thật vào nhật ký.

Bản giả (`PosesFakes.kt`): `FakePosesLibrarySource` có `MutableStateFlow<LibraryResult>`, ghi lại `retryCount` và các lần `setSaved`, `failWrites` ném `IOException`, `writeGate: CompletableDeferred<Unit>?` để giữ lần ghi chưa xong; `FakePoseThumbnailRepository` trả kết quả theo mã, có độ trễ ảo theo mã, đếm số lần gọi theo mã; `FakeNetworkStatus` có `dialogShown: MutableStateFlow<Boolean>` và `recovered: MutableSharedFlow<Unit>`; `FakeFeatureFlags` theo `MutableStateFlow<Boolean>`; `FakeAnalytics` ghi lại sự kiện; `testPose(id, categoryId, subjectType, stepCount)` dựng `Pose` bằng constructor công khai với `AssetPath` hợp lệ (`poses/img/<id>/preview_540-0123abcd.webp`); `testContent()` dựng `LibraryContent` 7 tư thế trong 3 nhóm (`women`, `men`, `couples`), `w02` đã lưu. Ảnh trong unit test ViewModel là `mockk<ImageBitmap>()`; test giao diện dùng `ImageBitmap(162, 210)` thật. Logger là `RecordingAppLogger`. Kiểm tra lỗi so loại (CLAUDE.md, bước 10).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Cờ tắt | Unit test | `PosesViewModelTest.flagOff_isDisabled`; `flagTurnsOffWhileReady_becomesDisabled`. Thất bại: luôn `Loading` |
| Cờ tắt thì về camera | Compose UI test | `PosesScreenTest.disabled_callsOnBackOnce` |
| Catalog đang tải | Unit test | `PosesViewModelTest.libraryLoading_isLoading` (qua ngay ở khung rỗng, test chặn hồi quy) |
| KB-1, mở thư viện từ camera; cờ tắt thì không có nút | Unit test | `PosesEntryTest.flagOff_noOpener`, `flagOn_openerOpensLibrary` (`posesOpener(true) { opened = true }` rồi gọi, `opened == true`). Thất bại: khung trả `null` (riêng `flagOff_noOpener` qua ngay) |
| KB-1, "Tất cả" chọn sẵn và thẻ theo thứ tự của thư viện | Unit test | `PosesViewModelTest.ready_selectsAll_andCardsFollowLibraryOrder`: `cards.map { it.id } == content.posesFor(All).map { it.id }`, `selected == All` |
| KB-1, lưới 2 cột | Compose UI test | `PosesScreenTest.grid_hasTwoColumns` (`GraphicsMode.NATIVE`): thẻ 1 và 2 cùng `top`, thẻ 2 nằm bên phải; thẻ 3 nằm dưới thẻ 1, cùng `left` |
| KB-1, dòng tổng số | Unit test, Compose UI test | `PosesViewModelTest.ready_totalAndCategoryCount`: `totalPoses == 7`, 3 chip nhóm; `PosesScreenTest.ready_showsTitleSummaryChipsAndCards`: có chữ "Thư viện tư thế", "7 tư thế trong 3 nhóm" |
| KB-1, chỉ nhóm có tư thế | Unit test | `PosesViewModelTest.ready_chipsAreAllSavedThenCategoriesInCatalogOrder`: `[All, Saved, Category(women), Category(men), Category(couples)]` theo `content.categories` |
| KB-1, thẻ có tên, loại, số bước, nút lưu | Unit test, Compose UI test | `PosesViewModelTest.card_hasNameSubjectStepsAndSaved`; `PosesScreenTest.card_showsNameMetaAndSaveButton`: "Tựa lan can", "Một người · 4 bước", nút mô tả "Lưu tư thế"; `card_couple_showsCoupleLabel` ("Cặp đôi · 5 bước"), `card_group_showsGroupLabel` ("Nhóm · 3 bước") |
| KB-1, tên đọc được theo ngôn ngữ | Compose UI test | `PosesScreenTest.card_nameUsesScreenLocale` (`@Config(qualifiers = "en")`: tên tiếng Anh của `LocalizedText`). Phần chọn ngôn ngữ đã có test ở T-009 (`LocalizedTextTest`) |
| KB-1, quay lại | Compose UI test | `PosesScreenTest.back_callsOnBack` |
| KB-1, quay lại về camera giữ tư thế | Kiểm tra thủ công | Staging: chọn tư thế ở camera (sau T-014), mở thư viện, bấm quay lại và nút Back của hệ thống: camera còn tư thế đó |
| KB-1, nút cài đặt | Compose UI test | `PosesScreenTest.settings_callsOnOpenSettings`, `settings_hiddenWhenNull` (qua ngay ở khung rỗng). Nối sang màn cài đặt kiểm thủ công khi T-025 đã có |
| KB-1 đặc biệt, KB-5, khung xương khi ảnh chưa có | Unit test, Compose UI test | `PosesViewModelTest.thumbnailNeeded_pending_isLoading` (fake trễ 5 giây; trước 5 giây `Loading`, sau đó `Ready`); `PosesScreenTest.thumbnailLoading_showsSkeleton_thumbnailReady_showsCard` |
| KB-1 đặc biệt, không xô lệch lưới | Compose UI test | `PosesScreenTest.skeletonThenImage_keepsCardBounds` (`GraphicsMode.NATIVE`): đo khung thẻ 3 khi `Loading` và khi `Ready` (tên một dòng): bằng nhau; `PoseCardTest.skeleton_sameSizeAsOneLineCard` |
| KB-2, lọc theo nhóm | Unit test | `PosesViewModelTest.selectCategory_showsOnlyThatCategory`: `onSelectFilter(Category("men"))`; mọi thẻ có `categoryId == "men"`, theo thứ tự `posesFor(Category("men"))` |
| KB-2, chip chọn có nền nhấn và dấu ✓ | Compose UI test | `PosesScreenTest.selectedChip_isSelected`: chip "Nam" `assertIsSelected()`, "Tất cả" `assertIsNotSelected()`. Màu và dấu ✓ thuộc test của `StickerChip` (T-014) và screenshot |
| KB-2, bấm chip | Compose UI test | `PosesScreenTest.chipClick_callsOnSelectFilter_andScrollsToTop`: cuộn tới thẻ 6, bấm "Nam"; nhận `Category("men")`, `gridState.firstVisibleItemIndex == 0` |
| KB-2, giữ chip khi quay lại | Unit test | `PosesViewModelTest.selectedFilter_restoredFromSavedStateHandle` (ViewModel thứ hai dựng từ cùng `SavedStateHandle` có bộ lọc `men`); `restoredCategoryMissing_fallsBackToAll` |
| KB-2, giữ vị trí cuộn khi quay lại | Compose UI test | `PosesScreenTest.scrollPosition_survivesStateRestore` (`StateRestorationTester`, `rememberLazyGridState` trong nội dung test như `PosesRoute`): cuộn tới thẻ 6, `emulateSavedInstanceStateRestore()`, `firstVisibleItemIndex == 6`. T-022 kiểm lại trên luồng chi tiết thật |
| KB-3, lưu, bỏ lưu, `pose_saved` | Unit test | `PosesViewModelTest.toggleSave_savesAndLogsPoseSavedTrue`: `setSaved("w01", true)`; thẻ `saved` ngay khi bấm (trước khi ghi xong, nhờ `writeGate`); sự kiện `AnalyticsEvent("pose_saved", mapOf("pose_id" to "w01", "saved" to "true"))`. `toggleSaveOfSavedPose_unsavesAndLogsSavedFalse`. Thất bại: không gọi `setSaved` |
| KB-3, bấm khi đang ghi | Unit test | `PosesViewModelTest.toggleSaveWhileWriting_isIgnored`: hai lần bấm, một lần `setSaved` |
| KB-3, nút lưu trên thẻ | Compose UI test | `PosesScreenTest.saveClick_callsOnToggleSave_notOnOpenPose`; `savedCard_saveButtonDescribedAsUnsave` ("Bỏ lưu tư thế") |
| KB-3, thứ tự không nhảy khi lưu trên màn | Unit test | `PosesViewModelTest.saveOnScreen_keepsOrder_untilFilterChanges`: lưu `w05` ở "Tất cả", thư viện phát thứ tự mới (`w05` lên đầu); `cards` giữ thứ tự cũ với `w05.saved == true`; chọn "Nam" rồi "Tất cả": `w05` ở đầu |
| KB-3, "Đã lưu" mới lưu nhất ở đầu | Unit test | `PosesViewModelTest.selectSaved_showsSavedInLibraryOrder` (thứ tự của `posesFor(Saved)`, đã có test thứ tự ở T-009) |
| KB-3, "Đã lưu" rỗng và "Xem tất cả tư thế" | Unit test, Compose UI test | `PosesViewModelTest.selectSaved_nothingSaved_isEmpty_thenShowAllSelectsAll`; `PosesScreenTest.savedEmpty_showsTexts_andShowAllCalls`: "Chưa lưu tư thế nào", "Chạm biểu tượng lưu trên thẻ tư thế để giữ lại ở đây.", bấm "Xem tất cả tư thế" gọi `onShowAll` |
| KB-3, mở lại app vẫn còn | Test sẵn có, kiểm tra thủ công | `DataStorePoseHistoryRepositoryTest.setSavedTrueThenFalse_persistsAcrossRepositoryInstances` (T-009). Staging: lưu hai tư thế, đóng hẳn app, mở lại: "Đã lưu" có hai tư thế |
| KB-3 đặc biệt, tư thế không còn trong catalog | Test sẵn có | `PoseOrderingTest.savedPoseMissingFromCatalog_isNotShown` (T-009); ViewModel chỉ hiện `posesFor(Saved)` |
| KB-3 đặc biệt, ghi lỗi | Unit test, Compose UI test | `PosesViewModelTest.saveFails_revertsButton_showsMessage_noEvent`: `failWrites`; thẻ về `saved == false`, `saveFailed == true`, không có `pose_saved`, có log `WARN` thẻ `Poses` kèm lỗi là `IOException` (`assertTrue(entry.error is IOException)`); `onSaveFailedShown()` đặt lại `false`. `PosesScreenTest.saveFailed_showsSnackbar_thenCallsShown`: chữ "Chưa lưu được tư thế. Thử lại sau." |
| KB-5, bộ khởi đầu hiện ảnh ngay | Unit test, kiểm tra thủ công | `PosesViewModelTest.thumbnailAlreadyOnDevice_isReadyWithoutWaiting`: fake không trễ; sau `runCurrent()` (thời gian ảo vẫn 0) thẻ `Ready`. Staging: lần mở thư viện đầu, các thẻ bộ khởi đầu hiện ảnh không thấy khung xương |
| KB-5, chỉ tải ảnh của thẻ đang có trong lưới | Unit test, Compose UI test | `PosesViewModelTest.thumbnailNeededTwice_loadsOnce`, `thumbnailReleased_dropsImage_andNextNeedLoadsAgain`, `atMostFourThumbnailLoadsAtOnce`; `PosesScreenTest.visibleCards_requestThumbnails_andScrolledAwayCardsRelease` |
| KB-5 / spec mục 7, ảnh nhỏ lỗi, chạm để thử lại | Unit test, Compose UI test | `PosesViewModelTest.thumbnailFails_staysSkeleton_logsContentLoadFailedThumbnail` (`content` `thumbnail`, `reason` `timeout`), `retryThumbnail_loadsAgain`; `PosesScreenTest.failedThumbnail_tapSkeleton_callsRetryThumbnail` |
| KB-5, catalog lỗi: tiêu đề, lời, "Thử lại", `content_load_failed` | Unit test, Compose UI test | `PosesViewModelTest.catalogFails_showsError_andLogsContentLoadFailedCatalog` cho `OTHER`, `NOT_FOUND`, `TIMEOUT`: `Error(reason)`, sự kiện `mapOf("content" to "catalog", "reason" to "<giá trị>")`; `catalogFailureRepeated_logsOnce`; `retry_callsLibraryRetry_andLogs`. `PosesScreenTest.error_showsTitleMessageRetry_andRetryCalls`: "Không tải được thư viện", "Đã có lỗi khi tải thư viện. Thử lại sau ít phút.", bấm "Thử lại" gọi `onRetry` |
| KB-5, 15 giây | Unit test, test sẵn có | `PosesViewModelTest.catalogTimeout_showsError_notLoadingForever` (fake phát `Failed(TIMEOUT)`); mức 15 giây có test ở T-009 (`TimedAssetLoaderTest.noAnswerWithin15s_returnsTimeoutAtExactly15s`) |
| KB-5, mất mạng thì hộp thoại thay thông báo | Unit test | `PosesViewModelTest.catalogFailsNetwork_dialogShown_isLoadingNotError` (vẫn ghi `content_load_failed` với `network`), `catalogFailsNetwork_dialogNotShown_showsError`, `networkRecovered_retriesCatalog_andFailedThumbnails` |
| Bấm thẻ (cho T-022) | Compose UI test | `PosesScreenTest.cardClick_callsOnOpenPose`, `cardClick_withoutOnOpenPose_doesNothing` |
| Log | Unit test | `PosesViewModelTest.logs_useTagPoses_andNoPoseIdsOrNames`: mọi dòng thẻ `Poses`, không chứa `w01` hay tên tư thế |
| Ảnh nhỏ: tải đúng loại file, chuyển lỗi | Unit test | `DefaultPoseThumbnailRepositoryTest.load_requestsThumbnailFile_andDecodes`, `loadFileFailed_returnsSameFailure` (lý do `TIMEOUT`), `decoderReturnsNull_returnsFailedOther` (qua ngay ở khung rỗng) |
| Giải mã ảnh nhỏ | Test Robolectric | `BitmapPoseImageDecoderTest.largeImage_isSampledDownButAtLeastTargetWidth` (`GraphicsMode.NATIVE`, ảnh PNG 1944 x 2520 ghi vào `TemporaryFolder`, `targetWidthPx = 486`: rộng 486), `corruptFile_returnsNull` (qua ngay ở khung rỗng) |
| `PoseCard`: chữ, nút lưu, màu | Compose UI test | `PoseCardTest.showsNameAndMeta`, `saveButton_touchTarget48_circle36` (`GraphicsMode.NATIVE`, đo qua phần tử bọc ngoài mang `testTag`), `saved_buttonFillIsSecondaryContainer_unsavedIsSurface` (đọc màu điểm ảnh bằng cách vẽ view gốc ra `Bitmap`, L-006), `saveClick_callsOnSaveClick_only`, `saveButton_hasButtonRoleAndLabel`, `imageArea_hasDesignAspectRatio` (162 : 210), `longName_wrapsToAtMostTwoLines` |
| `PoseCardSkeleton` | Compose UI test | `PoseCardTest.skeleton_sameSizeAsOneLineCard`, `skeleton_withoutOnClick_notClickable` (qua ngay ở khung rỗng), `skeleton_withOnClick_callsIt` |
| Giao diện thư viện, đúng thiết kế | Screenshot test cùng tên ảnh thiết kế, khung `w411dp-h891dp-xxhdpi`; ảnh so sánh `scripts/design-compare.sh poses`; rà soát độc lập theo mục 7 của README thiết kế | `PosesScreen_default_light.png`, `PosesScreen_default_dark.png` (8 nhóm như thiết kế, 6 thẻ, thẻ 2 đã lưu, ảnh trong suốt để thấy nền nhóm), `PosesScreen_loading_light.png`, `PosesScreen_loading_dark.png` (2 thẻ đầu có ảnh, 4 thẻ khung xương), `PosesScreen_error_light.png`, `PosesScreen_error_dark.png`, `PosesScreen_saved-empty_light.png`, `PosesScreen_saved-empty_dark.png`. Cả 8 ảnh truyền `onOpenSettings` khác `null` để thanh trên giống thiết kế |
| Ảnh thiết kế của ticket khác | Không làm ở đây | Ảnh thiết kế của ticket khác (T-022): `PoseDetailScreen_default_light`, `PoseDetailScreen_default_dark`, `PoseDetailScreen_loading_light`, `PoseDetailScreen_loading_dark`, `PoseDetailScreen_error_light`, `PoseDetailScreen_error_dark` |
| Ảnh thiết kế của ticket khác | Không làm ở đây | Ảnh thiết kế của ticket khác (T-023): `PosesScreen_ad_light`, `PosesScreen_ad_dark`, `PoseDetailScreen_ad_light`, `PoseDetailScreen_ad_dark` |
| Ảnh thiết kế của ticket khác | Không làm ở đây | Ảnh thiết kế của ticket khác (T-024): `PosesScreen_default_light_unfolded`, `PosesScreen_default_dark_unfolded`, `PoseDetailScreen_default_light_unfolded`, `PoseDetailScreen_default_dark_unfolded` |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` | `PosesScreen_default_small.png` (8 nhóm, 6 thẻ, một tên dài hai dòng; lưới cuộn được, chip cuộn ngang), `PosesScreen_saved-empty_small.png` (nút "Xem tất cả tư thế" thấy đủ, không bị cắt) |
| Màn hình thấp, cuộn được | Compose UI test | `PosesScreenTest.smallScreen_gridScrollsToLastCard` (`@Config(qualifiers = "w360dp-h640dp-xxhdpi")`, `performScrollToIndex(5)`, thẻ cuối có trong cây giao diện); `smallScreen_savedEmptyButtonFullyVisible` (khung nút nằm trong khung màn hình) |
| Catalog thành phần | Screenshot test | `DesignSystemCatalogScreenshotTest.poseCards_light` ghi `DesignSystem_pose_cards_light.png` |
| Chữ đúng bảng chữ | Compose UI test | Các test có chữ ở trên (`ready_showsTitleSummaryChipsAndCards`, `card_showsNameMetaAndSaveButton`, `error_showsTitleMessageRetry_andRetryCalls`, `savedEmpty_showsTexts_andShowAllCalls`) so đúng từng chữ của README mục 2 |
| Quy tắc phụ thuộc module (ADR-0001) | Kiểm tra tự động | `checkModuleDependencies` trong `scripts/check.sh` |
| Luồng thật trên máy | Kiểm tra thủ công | Staging, máy thật: (1) camera có nút thư viện, bấm mở thư viện, "Tất cả" chọn sẵn; (2) cuộn hết lưới, ảnh ngoài bộ khởi đầu hiện khung xương rồi ảnh, lưới không giật; (3) lưu, bỏ lưu, "Đã lưu"; (4) bật chế độ máy bay, cuộn tới thẻ có ảnh chưa tải: hộp thoại mạng hiện; tắt chế độ máy bay, bấm "Thử lại": ảnh hiện; (5) chế độ tối; (6) đổi ngôn ngữ máy sang tiếng Anh: tên tư thế và chip đổi theo |

**Ngoại lệ viết test trước:** `DefaultPosesLibrarySource` chỉ chép `PoseLibrary` sang `LibraryContent`; `PoseLibrary` có constructor `internal` nên test của feature không dựng được, và plan không sửa `:core:poses` để mở nó. Phần này được kiểm qua build, kiểm tra thủ công trên staging và test thứ tự ở T-009. `MainActivity.kt` (theo dõi cờ), `PosesFeatureModule.kt`, `build.gradle.kts`, `settings.gradle.kts`, `StagingFeatureFlagsModule.kt`, biểu tượng và giấy phép không có test riêng; chúng được kiểm qua build, `AppLaunchTest` trên CI (đồ thị Hilt của `MainActivity`) và kiểm tra thủ công. Không thêm test emulator đi từ camera vào thư viện: bản staging mới cài hiện hộp thoại đồng ý quảng cáo phủ lên app (CLAUDE.md, bước 10), bấm nút thư viện không chắc chắn.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 4 (ghi hai sự kiện `pose_saved`, `content_load_failed`, đã có trong spec mục 6) và điểm 7 (hai thành phần mới, hai biểu tượng và một trang catalog trong `:core:designsystem`; cờ mới trong `:core:config`; thêm module vào `settings.gradle.kts`). Không chạm điểm 1 (chưa đặt banner), 2 (không thêm thư viện), 3 (không thêm quyền), 8 (không đổi đường dẫn R2), 9 (không thêm khóa DataStore). Rủi ro cao, bắt buộc duyệt chéo; người phụ trách sản phẩm biết trước vì có sự kiện phân tích.

Bảng điểm dưới đây chỉ để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Module mới và bốn module khác (`:core:designsystem`, `:core:config`, `:app`, cấu hình Gradle), khoảng 50 file |
| B. Độ rõ của yêu cầu | 2 | Spec đã xác nhận, thiết kế đã duyệt, nhưng còn câu hỏi ảnh hưởng cách làm: ảnh lỗi có dòng tổng số và chip mà app không có dữ liệu (câu 2), thứ tự khi lưu ngay trên màn (câu 3), mã nhóm sang màu (câu 4), chữ thông báo lưu lỗi chưa có trong bảng chữ (câu 5) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi tiêu chí trong phạm vi có test JVM; quay lại camera giữ tư thế, mở lại app và nút cài đặt thật kiểm thủ công |
| D. Ảnh hưởng chéo | 2 | Sửa `AppNavHost.kt`, `MainActivity.kt`, `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`, `settings.gradle.kts`, `app/build.gradle.kts`, `DesignSystemCatalog.kt` mà T-012, T-014, T-016, T-025 cũng sửa; nút thư viện của camera bắt đầu hiện ở staging |
| E. Độ mới của kỹ thuật | 2 | Tải ảnh theo thẻ đang hiện và nhả bộ nhớ khi thẻ rời lưới, giải mã bằng `BitmapFactory`, `StateRestorationTester`: chưa có trong dự án nhưng phổ biến |
| **Tổng** | 10 | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `:core:designsystem`: thêm `PoseCard.kt`, hai biểu tượng, một trang catalog, một test, một ảnh chuẩn; không đổi thành phần có sẵn. T-024 dùng `PoseCard` trong lưới 3 cột (cỡ theo ô lưới, không viết cứng chiều rộng).
  - `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`: thêm `POSES`.
  - `settings.gradle.kts`, `app/build.gradle.kts`: thêm một dòng mỗi file.
  - `AppNavHost.kt`: thêm tham số `posesEnabled`, nối `onOpenPoses` của camera, đăng ký `posesScreen`.
  - `MainActivity.kt`: tiêm `FeatureFlags`, truyền `posesEnabled`.
  - Sự kiện phân tích: mỗi feature tự khai báo tên sự kiện trong ViewModel của mình, không có file sự kiện dùng chung; T-014 cũng ghi `content_load_failed` (lý do và giá trị tham số dùng chung `analyticsValue` của `:core:poses`).
- Task khác viết plan cùng lúc, có thể xung đột:
  - **T-014** (camera chọn tư thế): tạo `StickerChip(label, selected, onClick, modifier)` và `SelectedBadge` mà plan này dùng (khớp chữ ký ở plan T-014, mục 3.1); thêm trang catalog `PoseComponentCatalog` và test `PoseComponentsTest`, nên plan này đặt tên khác (`PoseCardCatalog`, `PoseCardTest`); sửa `DesignSystemCatalog.kt`, `DesignSystemCatalogScreenshotTest.kt`, có thể `AppNavHost.kt` (mở camera với tư thế chỉ định cho T-022) và `CameraNavigation.kt`. T-021 phải chạy sau T-014 (mục 8 câu 1).
  - **T-025** (cài đặt): thêm `:feature:settings` vào `settings.gradle.kts`, `app/build.gradle.kts`; `FeatureFlag.SETTINGS`; `StagingFeatureFlagsModule.kt`; `AppNavHost.kt` (nút cài đặt ở camera và thư viện); có thể `MainActivity.kt` (giao diện sáng tối). Task nào chạy sau thì nối nút cài đặt của thư viện: T-021 sau T-025 thì T-021 truyền `onOpenSettings` của `posesScreen`; T-021 trước thì để `null` và T-025 nối (ticket T-025: "mở từ nút bánh răng ở camera và thư viện").
  - **T-012** (màn chào): `settings.gradle.kts`, `app/build.gradle.kts`, `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`, `AppNavHost.kt`, `MainScreenChooser.kt`. Khác dòng với plan này, chạy lần lượt là đủ.
  - **T-016** (màn xem lại): `AppNavHost.kt` (`onPhotoCaptured`), có thể `FeatureFlag.kt`. Khác dòng.
  - **T-022, T-023, T-024** phụ thuộc T-021: dùng `posesScreen(onOpenPose)`, màn thư viện và `PosesViewModel`.
- Đề xuất thứ tự chạy: T-014 trước T-021; T-021 trước T-022, T-023, T-024. T-012, T-016, T-025 không phụ thuộc T-021, chạy trước hay sau đều được vì chỉ cùng sửa vài dòng trong các file dùng chung; phiên chạy lần lượt nên mỗi task đọc lại file trên nhánh chính mới nhất.

## 7. Điều kiện dừng riêng

1. Lúc bắt đầu task, `StickerChip` hoặc `SelectedBadge` chưa có trên nhánh chính, hoặc `StickerChip` không nhận chữ, `selected`, `onClick`.
2. Cần sửa `:core:poses` (ví dụ mở constructor của `PoseLibrary`) hoặc constructor của `Pose` không gọi được từ test của feature vì tham số `internal`.
3. Cần sửa file của `:feature:camera` để nút thư viện mở được màn này.
4. Cần thêm thư viện (ví dụ thư viện tải ảnh) để hiện ảnh nhỏ.
5. `NetworkStatus` không có trong đồ thị Hilt của app khi `:feature:poses` dùng nó.
6. Khi chạy task, `AppNavHost.kt` đã có cách khác để ẩn hiện nút của camera theo cờ (do task khác thêm) mà không hợp với `posesOpener`.

## 8. Câu hỏi cho người duyệt

1. **`StickerChip`, `SelectedBadge` lấy từ T-014.** Ticket T-021 chỉ phụ thuộc T-009, T-007; T-014 cũng ghi tạo hai thành phần này.
   - Đề xuất của AI: T-021 dùng của T-014, chạy sau T-014; dev thêm T-014 vào `depends_on` của ticket T-021 để phiên tự chạy bỏ qua T-021 khi T-014 dừng. Nếu muốn T-021 chạy trước, plan T-014 và plan này phải đổi: T-021 tạo hai thành phần.
   - Trả lời: Đồng ý với đề xuất của AI.
2. **Ảnh lỗi có dòng tổng số và chip** (`PosesScreen_error_*`), nhưng khi đọc catalog lỗi app chưa có số tư thế hay nhóm nào. Đây là mâu thuẫn giữa thiết kế và dữ liệu.
   - Đề xuất của AI: trạng thái lỗi chỉ có thanh trên và `StateMessage`; vùng dòng tổng số và chip trên ảnh so sánh sẽ khác thiết kế. Người làm thiết kế cập nhật frame Figma sau.
   - Trả lời: Đồng ý với đề xuất của AI.
3. **Thứ tự khi lưu ngay trên màn.** Spec xếp tư thế đã lưu lên đầu. Nếu xếp lại ngay, thẻ vừa bấm lưu nhảy lên đầu lưới.
   - Đề xuất của AI: thứ tự cố định khi lưới đang hiện; đổi chip hay mở lại thư viện thì xếp lại. Ở "Đã lưu", bỏ lưu một thẻ thì thẻ còn đó (trạng thái chưa lưu) tới khi đổi chip, để người dùng lưu lại được nếu bấm nhầm.
   - Trả lời: Đồng ý với đề xuất của AI.
4. **Mã nhóm trong catalog sang màu nền ảnh.** `AppColors` có 8 màu nhóm; catalog mẫu dùng `women`, `men`, `couples`.
   - Đề xuất của AI: `women`, `men`, `couples`, `friends`, `family`, `kids`, `travel`, `cafe`; mã khác dùng `surfaceVariant`. Người làm nội dung giữ đúng các mã này.
   - Trả lời: Đồng ý với đề xuất của AI.
5. **Chữ chưa có trong bảng chữ:** thông báo lưu lỗi "Chưa lưu được tư thế. Thử lại sau."; mô tả cho trình đọc màn hình "Quay lại", "Cài đặt", "Lưu tư thế", "Bỏ lưu tư thế", "Tải lại ảnh".
   - Đề xuất của AI: dùng các chữ này; dev sửa nếu muốn chữ khác.
   - Trả lời: Đồng ý với đề xuất của AI.
6. **Cỡ khung xương và chữ của thẻ.** `context/` cho khung xương cao 274, thẻ cao 284; README ghi "cùng cỡ thẻ" và spec yêu cầu không xô lệch lưới. Khoảng cách 2 giữa tên và dòng phụ nằm ngoài hệ thống.
   - Đề xuất của AI: khung xương cùng cỡ thẻ có tên một dòng (theo README và spec); tên tối đa 2 dòng, thẻ tên hai dòng cao hơn khung xương 24 dp (chỉ hàng đó đổi khi ảnh về). Khoảng 2 dp viết bằng `Spacing.xs / 2`, không thêm token.
   - Trả lời: Đồng ý với đề xuất của AI.
7. **Định dạng và tần suất sự kiện.** `saved` là `true`/`false`. `content_load_failed` của ảnh nhỏ ghi mỗi lần tải lỗi, nên khi mất mạng có thể ghi nhiều lần trong một lần mở thư viện.
   - Đề xuất của AI: giữ như vậy để số liệu đúng với số lần lỗi; nếu chỉ muốn đếm "lần mở thư viện có lỗi" thì ghi tối đa một lần cho mỗi tư thế mỗi lần mở màn.
   - Trả lời: Đồng ý với đề xuất của AI.
8. **Lỗi catalog do mất mạng.** Spec: hộp thoại mạng thay cho thông báo lỗi riêng.
   - Đề xuất của AI: dưới hộp thoại màn hiện khung xương; hộp thoại đóng bằng "Thử lại" thì tải lại. Cờ hộp thoại tắt thì hiện lỗi thường, để không chờ mãi (bài học 7).
   - Trả lời: Đồng ý với đề xuất của AI.
9. **Nút cài đặt.** Màn cài đặt (T-025) chưa có.
   - Đề xuất của AI: như mục 6; ảnh screenshot luôn có nút để giống thiết kế.
   - Trả lời: Đồng ý với đề xuất của AI.
10. **Thẻ log `Poses` trùng với `:core:poses`.** CLAUDE.md: thẻ log là tên feature.
    - Đề xuất của AI: giữ `Poses` theo quy tắc; nội dung dòng log của feature (lọc, lưu, thử lại) khác dòng của core (catalog, tải file).
    - Trả lời: Đồng ý với đề xuất của AI.
11. **Chữ mẫu trên ảnh thiết kế lệch bảng chữ.** Ảnh ghi dòng phụ "Du lịch · 3 bước", "Cà phê · 3 bước" (tên nhóm), bảng chữ và spec ghi "loại · số bước" (Một người, Cặp đôi, Nhóm).
    - Đề xuất của AI: theo bảng chữ và spec; chữ mẫu trên ảnh chỉ để minh họa (README mục 2).
    - Trả lời: Đồng ý với đề xuất của AI.
12. **Cỡ task và `token_estimate`.** Số gợi ý cho cỡ L là 9.5M (trung vị 7 task), nhưng T-013, cũng tạo module feature mới với thành phần core, dùng 60.4M. Plan có khoảng 50 file.
    - Đề xuất của AI: ghi 45M, không tách ticket. Nếu muốn nhẹ hơn, tách thành hai plan chạy nối tiếp: (a) `PoseCard`, `PoseCardSkeleton`, biểu tượng, catalog; (b) module `:feature:poses` và điều hướng.
    - Trả lời: Đồng ý với đề xuất của AI.
13. **Lúc catalog đang tải** (thiết kế không có trạng thái này; thường không thấy vì màn loading đã tải catalog).
    - Đề xuất của AI: thanh trên và 6 khung xương, không có dòng tổng số và chip.
    - Trả lời: Đồng ý với đề xuất của AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:**

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
