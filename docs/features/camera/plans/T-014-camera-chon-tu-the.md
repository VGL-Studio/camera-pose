---
ticket: T-014
title: Camera: chọn tư thế, nét viền, thẻ bước
status: approved                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 30M          # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [analytics, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                   # người duyệt
cross_reviewer: Tuấn             # bắt buộc khi risk_level là high
approved_at: 2026-10-22
---

# PLAN T-014: CAMERA: CHỌN TƯ THẾ, NÉT VIỀN, THẺ BƯỚC

## 1. Tóm tắt

Thêm vào màn camera của T-013 phần tư thế: hàng chip nhóm, dải ô tư thế cuộn ngang theo thứ tự của `:core:poses` (đã lưu, gần đây, còn lại), nét viền trắng có quầng tối trên khung xem trước, thẻ bước chuyển bước được, vuốt lên để ẩn và nút "Bước" để hiện lại (giữ trạng thái ẩn qua các lần mở app), trạng thái không có tư thế, ô tải ảnh lỗi chạm để thử lại, mở camera với tư thế chỉ định (cho T-022), sự kiện `pose_selected` và `content_load_failed`. Phần tư thế nằm trong một ViewModel riêng (`PoseViewModel`) cạnh `CameraViewModel`, nên camera của T-013 gần như không đổi và ít đụng T-016; bốn thành phần mới (`StickerChip`, `SelectedBadge`, `PoseThumb`, `StepCard`) vào `:core:designsystem`. Kiểm chứng bằng unit test ViewModel và repository, test giao diện Robolectric (kể cả vuốt và khung thấp), screenshot test cùng tên ba ảnh thiết kế mới và ghi lại bảy ảnh chuẩn của T-013, ảnh so sánh `scripts/design-compare.sh camera`.

`token_estimate` 30M thay cho số gợi ý 9.5M của `scripts/token-usage.py estimate` (trung vị cỡ L): cỡ việc tương đương T-013 (khoảng 55 file, bốn thành phần core, hai lớp ViewModel, 14 ảnh chuẩn), và T-013 dùng 60.4M, trong đó khoảng 24M tới lúc CI vòng 2 (báo cáo RUN-20261022-01, mục Token). Các task L khác trong số trung vị nhỏ hơn nhiều (T-007 lần 2 và 3, T-011 phiên bản 2).

## 2. Hiểu yêu cầu

Ticket xử lý KB-1 và KB-2 trong `features/camera/spec.md`, cộng các phần của KB-4, KB-6, KB-7, KB-8 mà T-013 để lại cho T-014 (plan T-013, mục 2):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi vào camera thì khung xem trước chạy và nét viền của tư thế dùng lần trước hiện sẵn; lần đầu là tư thế mặc định | Tư thế dùng lần trước là `PoseLibrary.lastUsedPose` (plan T-009, câu 5: không lưu khóa riêng); không có thì `defaultPose` (tư thế đầu của catalog, spec câu 1). Chọn tự động lúc mở không tính là "đã dùng" và không ghi `pose_selected` (mục 8, câu 13) |
| KB-1 | Khi vào camera thì hàng chip hiện "Tất cả" và các nhóm có tư thế; nhóm chưa có tư thế không hiện | "Tất cả" đứng đầu, rồi `PoseLibrary.categories` (đã bỏ nhóm rỗng, giữ thứ tự catalog). Tên nhóm lấy từ catalog theo ngôn ngữ máy (`LocalizedText.resolve()`) |
| KB-1 | Khi chạm một chip thì dải đổi theo nhóm; chip đang chọn có nền màu nhấn và dấu ✓ | `StickerChip` đang chọn: nền `secondaryContainer` và `SelectedBadge`. Đổi chip không đổi tư thế đang chọn; dải cuộn về đầu. Chip đang chọn chỉ giữ trong lần mở camera này (mặc định "Tất cả") |
| KB-1 | Khi dải hiện thì tư thế xếp: đã lưu, gần đây (mới nhất trước), còn lại theo catalog; cho "Tất cả" và từng nhóm | `PoseLibrary.posesFor(PoseFilter.All)` và `posesFor(PoseFilter.Category(id))` đã xếp đúng (T-009). Thứ tự chụp lại một lần khi thư viện sẵn sàng và giữ nguyên trong suốt đời của màn camera, không nhảy khi người dùng vừa chọn một ô (mục 8, câu 2) |
| KB-1 | Khi chạm một ô thì nét viền và thẻ bước đổi sang tư thế đó; ô đang chọn có viền màu nhấn và dấu ✓; app ghi `pose_selected` | `PoseThumb` đang chọn: viền `secondaryContainer` dày `Stroke.selected` (3) và `SelectedBadge`. Ghi `pose_selected` với `pose_id`, `category` (mã nhóm), `source` là `strip`; ghi "đã dùng" (`PoseHistoryRepository.markUsed`). Chạm lại ô đang chọn không làm gì |
| KB-1 | Khi mở camera từ "Chụp với tư thế này" thì camera dùng đúng tư thế đó, dải cuộn tới ô của nó | `CameraDestination` thêm `poseId: String? = null`. `PoseViewModel` theo dõi khóa `poseId` của `SavedStateHandle` (đúng cả khi T-022 tạo đích mới hay đặt khóa vào đích camera đang có). Tư thế không có trong nhóm đang chọn thì chip về "Tất cả". Ghi "đã dùng". `pose_selected` với `source` `detail` do T-022 ghi (mục 8, câu 3). Nút ở chi tiết tư thế và điều hướng là việc của T-022 |
| KB-1 | Khi ảnh nhỏ của ô chưa tải xong thì ô hiện khung trống, không chặn việc chọn; tải lỗi thì ô hiện dấu lỗi, chạm lại để thử | Ảnh nhỏ tải khi ô hiện trên dải (`PoseCatalogRepository.loadFile(pose, THUMBNAIL)`, spec mục 7). Đang tải: chỉ có nền ô. Lỗi: biểu tượng `refresh` giữa ô; chạm thì chọn tư thế và tải lại ảnh (mục 8, câu 5). Mỗi lần tải lỗi ghi `content_load_failed` (`content` `thumbnail`) |
| KB-1 | Khi tư thế dùng lần trước không còn trong catalog thì dùng tư thế mặc định | `lastUsedPose` đã bỏ mã không còn trong catalog (T-009), nên rơi về `defaultPose` |
| KB-1 | Khi chưa chọn tư thế nào thì không có nét viền, thẻ bước, công cụ chỉnh; khung hiện "Chọn một tư thế ở dải bên dưới" | `PoseUiState.Ready.selected == null`: xảy ra khi catalog không có tư thế nào (mục 8, câu 1). Cột công cụ chỉnh thuộc T-015, T-015 cũng phải ẩn nó khi không có tư thế |
| KB-2 | Khi có tư thế thì thẻ bước hiện ở đầu khung, ghi "2/4" và nội dung bước | `StepCard` ở đầu khung, cách mép `Spacing.md`. Bước lấy từ `Pose.steps.resolve()`. Tư thế không có bước nào thì không có thẻ |
| KB-2 | Khi chạm mũi tên sau, trước thì chuyển bước; ở bước đầu, cuối mũi tên tương ứng mờ và không làm gì | Mũi tên mờ: độ mờ 38%, không bấm được |
| KB-2 | Khi đổi tư thế thì thẻ quay về bước 1 | `stepIndex = 0` mỗi lần đổi tư thế (cả từ dải, từ chi tiết, tư thế tiếp) |
| KB-2 | Khi vuốt thẻ lên thì thẻ ẩn, nút nhỏ "Bước" hiện ở góc trên khung; bấm thì thẻ hiện lại | Vuốt lên quá 24 dp thì ẩn. Thêm hành động trợ năng "Ẩn thẻ bước" trên thẻ, vì người dùng TalkBack không vuốt được. Nút "Bước" ở đúng chỗ của thẻ (góc trên trái) |
| KB-2 | Khi thẻ đang ẩn thì ẩn tới khi người dùng hiện lại, kể cả khi đổi tư thế hay mở lại app | Khóa DataStore mới `camera_steps_hidden` (boolean, mặc định `false`), spec mục 6 dòng "thẻ bước ẩn hay hiện" |
| KB-4 (phần T-013 để lại) | Khi bấm đổi camera thì nét viền và tư thế giữ nguyên | Tư thế nằm trong `PoseViewModel`, đổi camera chỉ chạm `CameraViewModel`; kiểm bằng test giao diện |
| KB-6 (phần liên quan) | `photo_captured` có `pose_id` | `CameraViewModel` đọc tư thế đang dùng từ `ActivePoseHolder` lúc chụp; `CapturedPhoto` thêm `poseId` để T-016 ghi `photo_saved` (`pose_id`) |
| KB-6 (bàn giao cho T-016) | "Tư thế tiếp" về camera với tư thế kế tiếp trong dải đang chọn, hết dải thì về tư thế đầu | T-014 làm sẵn `PoseViewModel.onNextPose()` (ghi `pose_selected` với `source` `next`); T-016 nối nút và đường quay về (mục 8, câu 14) |
| KB-7 (phần T-013 để lại) | Khi bị từ chối quyền thì hàng chip và dải tư thế vẫn dùng được | Hàng chip và dải hiện ở mọi trạng thái của camera. Nét viền, thẻ bước, lời nhắc chỉ hiện khi camera `Ready` (đúng ảnh `starting`, `permission-denied`, `error`) |
| KB-8 (phần liên quan) | Khi app ra nền rồi quay lại thì giữ tư thế | `PoseViewModel` không làm gì khi màn hình dừng; tư thế và thứ tự dải giữ nguyên |
| Spec mục 7 | Nét viền tải lỗi thì không hiện nét viền, thẻ bước báo "Chưa tải được nét viền" kèm "Thử lại" | Thẻ bước thay hai mũi tên bằng nút chữ "Thử lại"; ghi `content_load_failed` (`content` `outline`). Chữ này có trong spec nhưng không có trong bảng chữ của thiết kế (mục 8, câu 7) |
| Spec mục 7 | Mất mạng thì hộp thoại của spec `network` | Lỗi mạng của `RemoteAssets` đã tự bật hộp thoại (T-011). `PoseViewModel` thu `NetworkStatus.recovered` để tải lại các ảnh lỗi và catalog lỗi (plan T-011, mục 6) |

Thiết kế tham chiếu: `features/camera/design/README.md`; số liệu trong `design/context/`; ảnh trong `design/screens/`. Phân chia ảnh:

| Ảnh | Ticket | Ghi chú |
| --- | --- | --- |
| `CameraScreen_default_dark`, `CameraScreen_no-pose_dark`, `CameraScreen_steps-hidden_dark` | T-014 (mới) | `default` và `steps-hidden` có cột công cụ nét viền (chỉnh, lật, khóa, hiện tư thế) của T-015: T-014 chưa vẽ cột này, T-015 ghi lại hai ảnh |
| `CameraScreen_starting_dark`, `CameraScreen_permission-denied_dark`, `CameraScreen_error_dark`, `CameraScreen_countdown_dark` | T-013, T-014 ghi lại | Thêm hàng chip, dải tư thế; ảnh `countdown` thêm nét viền và thẻ bước. Cột công cụ nét viền của ảnh `countdown` vẫn chờ T-015 |
| `CameraScreen_adjust_dark`, `CameraScreen_locked_dark` | T-015 | |
| `CameraScreen_matching_dark`, `CameraScreen_matched_dark`, `CameraScreen_no-person_dark` | T-017 | |
| `CameraScreen_default_dark_unfolded` | T-020 | |
| `ReviewScreen_*` (5 ảnh) | T-016 | |

ADR liên quan: ADR-0001 (feature chỉ phụ thuộc core; `:core:poses` dùng chung với thư viện tư thế), ADR-0003 (không thêm cờ: phần tư thế nằm sau `feature_camera_enabled` sẵn có), ADR-0008 (thành phần mới trong `:core:designsystem`, không viết cứng màu và cỡ chữ), ADR-0012 (sự kiện qua `:core:analytics`), ADR-0015 (ảnh tư thế qua `:core:poses` và `RemoteAssets`, không tự ghép đường dẫn), ADR-0019 (log qua `AppLogger`), ADR-0020 (tên screenshot theo ảnh thiết kế, ảnh so sánh), ADR-1001 (mỗi lần tải chờ tối đa 15 giây, đã có trong `:core:poses`).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu `:feature:sample` và cách T-013 dựng `:feature:camera` (repository trong `data/`, Hilt module trong `di/`, `UiState` sealed, `StateFlow`, `Route` nối ViewModel, `Screen` không trạng thái), mẫu thành phần camera của T-013 trong `:core:designsystem` (`component/`, trang catalog, test đo kích thước và màu điểm ảnh bằng `pixelAt` như `CameraComponentsTest`, L-006). Gói gốc của feature: `com.vglstudio.camera.pose.line.guide.feature.camera`, phần mới trong gói con `pose`. Thẻ log: `Camera` (cùng feature).

**Điều kiện đầu vào.** T-013 đã merge (đã có: `CameraScreen` với chỗ trống `POSE_AREA_HEIGHT` 132 dp, `PreviewContent` và `ReadyPreview` trong `CameraControls.kt`, `CameraDestination(askPermission)`, `CapturedPhotoHolder`). T-009 đã merge (`PoseLibraryRepository`, `PoseCatalogRepository.loadFile`, `PoseHistoryRepository.markUsed`, `PoseLibrary`). T-011 đã merge (`NetworkStatus.recovered`). Thiếu hay khác thì dừng (mục 7).

**Vì sao một ViewModel riêng.** `CameraUiState` của T-013 là sealed theo trạng thái phần cứng (`Starting`, `PermissionDenied`, `Error`, `Ready`), còn hàng chip và dải phải dùng được ở mọi trạng thái đó (KB-7). Gộp tư thế vào `CameraUiState` buộc mọi nhánh mang thêm dữ liệu tư thế và sửa phần lớn `CameraViewModel`, đúng file mà T-016 cũng sửa. `PoseViewModel` (`@HiltViewModel`, cùng đích điều hướng nên cùng `SavedStateHandle`) giữ toàn bộ phần tư thế; `CameraViewModel` chỉ thêm một phụ thuộc để biết tư thế lúc chụp.

**Dùng chung giữa hai ViewModel (`pose/ActivePoseHolder.kt`).**

```kotlin
/** The pose the camera shows now; null when there is none. Written by PoseViewModel, read at capture (and by T-017). */
@Singleton
class ActivePoseHolder @Inject constructor() {
    val current: StateFlow<Pose?>
    fun set(pose: Pose?)
}
```

**Thêm vào `:core:poses` (`model/PoseLibrary.kt`).** `PoseLibrary` có constructor `internal`, nên feature không dựng được thư viện giả để test. Thêm hàm tạo công khai, dùng đúng thuật toán xếp của T-009, không đổi gì khác:

```kotlin
companion object {
    /** The library of [catalog] with [saved] and [recent] applied: the same ordering the repository uses. */
    fun from(catalog: PoseCatalog, saved: List<SavedPose> = emptyList(), recent: List<UsedPose> = emptyList()): PoseLibrary =
        buildPoseLibrary(catalog, saved, recent)
}
```

T-021 (thư viện tư thế) cũng cần hàm này cho test của mình (mục 6).

**Trạng thái phần tư thế (`pose/PoseUiState.kt`).**

```kotlin
sealed interface PoseUiState {
    /** The catalog is loading (for example after the system restored the camera without the loading screen). */
    data object Loading : PoseUiState

    /** The catalog could not load: the pose area offers "Thử lại" (question 8). */
    data object Unavailable : PoseUiState

    data class Ready(
        val categories: List<CategoryChip>,      // without "Tất cả", which the screen adds first
        val selectedCategoryId: String?,         // null: "Tất cả"
        val strip: List<PoseThumbItem>,          // order of the selected chip
        val selected: SelectedPose?,             // null: no pose
        val stepsHidden: Boolean,
        val scrollRequest: StripScroll? = null,  // the strip scrolls to index once per new key
    ) : PoseUiState
}
data class CategoryChip(val id: String, val label: String)
data class PoseThumbItem(val id: String, val label: String, val image: PoseImage)
data class SelectedPose(val id: String, val steps: List<String>, val stepIndex: Int, val outline: PoseImage)
data class StripScroll(val index: Int, val key: Int)
sealed interface PoseImage {
    data object Loading : PoseImage
    data class Ready(val bitmap: ImageBitmap) : PoseImage
    data object Failed : PoseImage
}
```

`label` của ô là tên tư thế theo ngôn ngữ máy, dùng cho trình đọc màn hình.

**Giải mã ảnh (`pose/PoseImageDecoder.kt`).** Dự án chưa có thư viện tải ảnh (không thêm, điểm chặn 2). `fun interface PoseImageDecoder { suspend fun decode(file: File, targetPx: Int): ImageBitmap? }`; bản thật `BitmapPoseImageDecoder` chạy trên `Dispatchers.IO`: đọc kích thước (`inJustDecodeBounds`), chọn `inSampleSize` lũy thừa 2 lớn nhất mà cạnh ngắn vẫn không nhỏ hơn `targetPx`, rồi `BitmapFactory.decodeFile(...)?.asImageBitmap()`; trả `null` khi không đọc được. `targetPx`: ô 256, nét viền 1080. WebP đọc được bằng `BitmapFactory` từ API 26 (minSdk). Ảnh nhỏ đã giải mã giữ trong bộ nhớ của ViewModel (khoảng 260 KB mỗi ô, catalog mẫu có 18 tư thế; mục 8, câu 16).

**Lưu ẩn hiện thẻ bước (`data/`).** `PoseOverlaySettingsRepository`: `val stepsHidden: Flow<Boolean>`, `suspend fun setStepsHidden(hidden: Boolean)` (ném `IOException`). `DataStorePoseOverlaySettingsRepository` dùng `DataStore<Preferences>` chung của `:core:data`, khóa `camera_steps_hidden`, đọc lỗi `IOException` thì thử lại như `DataStoreCameraSettingsRepository`. T-015 thêm độ mờ, cỡ, lật, khóa, ẩn nét viền vào cùng repository.

**`PoseViewModel`** (`@HiltViewModel`) nhận `PoseLibraryRepository`, `PoseCatalogRepository`, `PoseHistoryRepository`, `PoseOverlaySettingsRepository`, `PoseImageDecoder`, `ActivePoseHolder`, `NetworkStatus`, `Analytics`, `AppLogger`, `SavedStateHandle`. Không nhận `FeatureFlags`: không có cờ mới, màn camera chỉ mở khi `feature_camera_enabled` bật (như T-013). `uiState: StateFlow<PoseUiState>`. Hoạt động:

- `init`: thu `library`. Lần `LibraryState.Ready` đầu tiên: giữ bản chụp `PoseLibrary` này cho tới hết đời ViewModel (thứ tự dải không nhảy khi `markUsed` làm thư viện phát lại, mục 8, câu 2). `Loading` thì `PoseUiState.Loading`, `Failed` thì `Unavailable` (chỉ khi chưa có bản chụp). Đọc `stepsHidden.first()` một lần, sau đó giữ trong bộ nhớ và lưu mỗi lần đổi. Chọn tư thế đầu: `poseId` của `SavedStateHandle` nếu có trong catalog, rồi `lastUsedPose`, rồi `defaultPose`; không ghi sự kiện, không ghi "đã dùng". Ghi log "Pose library ready" kèm số tư thế và nhóm.
- Thu `savedStateHandle.getStateFlow<String?>("poseId", null)`: giá trị khác `null` sau khi đã có bản chụp thì `select(id, source = DETAIL)`, rồi đặt khóa về `null` để không áp lại. Mã không có trong catalog: log `warn` "Requested pose is not in the catalog", giữ tư thế hiện tại.
- Thu `networkStatus.recovered`: tải lại mọi ảnh nhỏ đang `Failed` và nét viền đang `Failed`; nếu `Unavailable` thì `library.retry()`. Log "Reloading pose content after the network came back".
- `select(pose, source)`: đặt `selected = SelectedPose(id, steps, stepIndex = 0, outline = Loading)`, `activePose.set(pose)`, tải nét viền (`loadFile(pose, OUTLINE)` rồi `decode(file, 1080)`). Nguồn `STRIP` và `NEXT`: ghi `pose_selected`. Mọi nguồn trừ chọn tự động lúc mở: `markUsed` (lỗi `IOException` chỉ log `warn`, như T-013 câu 4). Nguồn `DETAIL` hoặc tư thế không có trong dải đang chọn: chip về "Tất cả". Sau đó đặt `scrollRequest` tới vị trí của tư thế trong dải (khóa tăng dần). Chỉ lần tải nét viền mới nhất được ghi vào trạng thái (tải cũ bị hủy khi đổi tư thế).
- `onCategorySelect(id: String?)`: đổi `selectedCategoryId`, dựng lại `strip` từ bản chụp, `scrollRequest` về 0. Không đổi tư thế.
- `onPoseSelect(id)`: tư thế đang chọn thì không làm gì về chọn; ô đang `Failed` thì tải lại ảnh nhỏ (kể cả khi là ô đang chọn). Ngược lại `select(pose, STRIP)`.
- `onThumbShown(id)`: ô chưa có ảnh và chưa đang tải thì tải (`loadFile(pose, THUMBNAIL)`, `decode(file, 256)`). Không tải lại ô `Failed` (chỉ khi chạm hoặc khi mạng trở lại).
- Tải lỗi (`PoseFileResult.Failed`) hoặc giải mã ra `null`: ảnh thành `Failed`, ghi `content_load_failed` với `content` `thumbnail` hoặc `outline` và `reason` (mục 3.3), log `warn` "Pose thumbnail failed: <reason>" hoặc "Pose outline failed: <reason>".
- `onPreviousStep()`, `onNextStep()`: đổi `stepIndex` trong `[0, steps.size - 1]`; ngoài khoảng thì bỏ qua.
- `onHideSteps()`, `onShowSteps()`: đổi `stepsHidden`, lưu (`IOException` thì log `warn`, lựa chọn vẫn có hiệu lực trong lần dùng này), log "Steps hidden" hoặc "Steps shown".
- `onRetryOutline()`: tải lại nét viền của tư thế đang chọn. `onRetryLibrary()`: `library.retry()`.
- `onNextPose()` (cho T-016): tư thế kế tiếp của tư thế đang chọn trong `strip` hiện tại; hết dải hoặc tư thế đang chọn không có trong dải thì tư thế đầu; `select(next, NEXT)`.

Thứ tự ưu tiên tải: nét viền của tư thế đang chọn tải ngay khi chọn; ảnh nhỏ tải khi ô hiện trên dải. Nét viền của các tư thế khác không tải trước (mục 8, câu 6). Bộ khởi đầu đã có trên máy từ màn loading (T-010), nên phần lớn lần tải trả ngay từ bộ nhớ đệm.

**`CameraViewModel` (sửa nhỏ).** Constructor thêm `activePose: ActivePoseHolder`. Trong `capture()`: `val pose = activePose.current.value`; `photo_captured` có `"pose_id" to (pose?.id ?: "")`; `CapturedPhoto(..., poseId = pose?.id ?: "")`. Đọc lúc chụp xong, nên đổi tư thế trong lúc đếm ngược thì ảnh mang tư thế mới (đúng nét viền đang thấy khi chụp). Không đổi gì khác.

**`CapturedPhoto` (sửa nhỏ).** Thêm trường cuối `val poseId: String = ""`. Giá trị mặc định giữ nguyên mọi chỗ gọi và test hiện có (`CapturedPhoto(File("capture_1.jpg"), BACK, OFF, 1_000L)` vẫn bằng nhau).

**Điều hướng (`navigation/CameraNavigation.kt`).** `@Serializable data class CameraDestination(val askPermission: Boolean = true, val poseId: String? = null)`. `AppNavHost` gọi `CameraDestination()` nên không đổi. T-022 điều hướng `CameraDestination(poseId = id)`.

**Màn hình.**

- `CameraRoute`: thêm `poseViewModel: PoseViewModel = hiltViewModel()`, thu `poseViewModel.uiState`, truyền `PoseActions(...)` nối các hàm của `PoseViewModel` vào `CameraScreen`.
- `CameraScreen`: thêm hai tham số có giá trị mặc định ở cuối, nên mọi test của T-013 vẫn biên dịch: `poseState: PoseUiState = PoseUiState.Loading`, `poseActions: PoseActions = PoseActions()`. `PoseActions` là lớp giữ các callback (`onCategorySelect`, `onPoseSelect`, `onThumbShown`, `onPreviousStep`, `onNextStep`, `onHideSteps`, `onShowSteps`, `onRetryOutline`, `onRetryLibrary`), mặc định không làm gì. Chỗ `Spacer(POSE_AREA_HEIGHT)` thay bằng `PosePicker(poseState, poseActions)` cao đúng 132 dp.
- `CameraControls.kt`: `PreviewContent` và `ReadyPreview` nhận thêm slot `poseOverlay: @Composable BoxScope.() -> Unit = {}`, vẽ sau lưới và trước `ZoomControl` và lớp đếm ngược. `CameraScreen` truyền `PoseOverlay(poseState, poseActions)` khi camera `Ready`. Trạng thái khác không vẽ lớp này (ảnh `starting`, `permission-denied`, `error` không có nét viền, thẻ bước).

**Bố cục phần tư thế** (số liệu `context/CameraScreen_default_dark.md`, khung 411 x 891):

| Phần | Thành phần, màu | Kích thước, khoảng cách |
| --- | --- | --- |
| Hàng chip | `LazyRow` của `StickerChip`: "Tất cả" rồi các nhóm. `testTag(CHIPS)` | Cao 52 = 16 trống trên (chỗ cho `SelectedBadge` nhô lên 9) + 36; `contentPadding` ngang `Spacing.md`, cách nhau `Spacing.sm`. Đỉnh chip ở 656 như thiết kế |
| Dải tư thế | `LazyRow` của `PoseThumb`, `key = id`. Mỗi ô gọi `onThumbShown(id)` trong `LaunchedEffect(id)` khi được dựng. `scrollRequest` mới thì `animateScrollToItem(index)`. `testTag(STRIP)`, mỗi ô `testTag("camera_pose_<id>")` | Cao 80 = 16 trống trên + 64; ngang `Spacing.md`, cách nhau `Spacing.sm`. Đỉnh ô ở 708 |
| Catalog đang tải | Vùng 132 trống | |
| Catalog lỗi (`Unavailable`) | `Row` giữa vùng: chữ "Chưa tải được danh sách tư thế." `bodyMedium` màu `onCamera` và `TextButton` "Thử lại" gọi `onRetryLibrary` (mục 8, câu 8) | Giữa vùng 132 |

**Bố cục trên khung xem trước** (chỉ khi camera `Ready` và `poseState is Ready`):

| Phần | Thành phần, màu | Kích thước, vị trí |
| --- | --- | --- |
| Nét viền (`pose/PoseOutline.kt`) | `Image(bitmap)` tô `cameraOverlayStroke` (`ColorFilter.tint`, `BlendMode.SrcIn`), dưới nó một bản tô `cameraScrim` làm quầng: API 31 trở lên `Modifier.blur(6.dp, BlurredEdgeTreatment.Unbounded)`; dưới API 31 (`blur` không có tác dụng) là bốn bản tô `cameraScrim` lệch 1.5 dp lên, xuống, trái, phải. Tham số `color` để T-017 đổi sang `success`. `testTag(OUTLINE)`. Nét viền `Loading` hoặc `Failed`: không vẽ | Hộp rộng 200/411, cao 400/548 của khung (thiết kế 200 x 400 trong 411 x 548), căn giữa, `ContentScale.Fit` |
| Thẻ bước | `StepCard` với nhãn "2/4" (`%1$d/%2$d`), nội dung bước, hai mũi tên. Vuốt lên: `pointerInput` với `detectVerticalDragGestures`, cộng dồn quãng kéo, quá -24 dp thì gọi `onHideSteps` một lần mỗi lần kéo. `semantics { customActions = "Ẩn thẻ bước" }`. `testTag(STEP_CARD)` | Đầu khung cách `Spacing.md`, ngang `Spacing.md` mỗi bên, rộng khung trừ 32 |
| Thẻ bước khi nét viền lỗi | `StepCard` không nhãn số, chữ "Chưa tải được nét viền", thay mũi tên bằng `TextButton` "Thử lại" gọi `onRetryOutline` | Như trên |
| Nút "Bước" | `Row` icon `format_list_numbered` 20 và chữ "Bước" `labelLarge`; nền `cameraControl`, viền `Stroke.sticker` màu `cameraOnControl`, bo `Radius.full`, chữ màu `cameraOnControl`; bấm gọi `onShowSteps`. `testTag(SHOW_STEPS)` | Cao 48, lề ngang `Spacing.md`, icon cách chữ `Spacing.xs`; ở góc trên trái, đúng chỗ của thẻ (thiết kế 95 x 48 tại 16, 108) |
| Lời nhắc chọn tư thế | Khi `selected == null`: `Row` icon `swipe_left` 20 và chữ "Chọn một tư thế ở dải bên dưới" `labelLarge`; nền `cameraControl`, viền `Stroke.sticker` `cameraOnControl`, bo `Radius.full`. `testTag(NO_POSE_HINT)` | Giữa khung; lề dọc `Spacing.sm`, ngang `Spacing.md`, icon cách chữ `Spacing.sm`; rộng tối đa khung trừ 32, chữ xuống dòng khi khung hẹp |

Lời nhắc này là `PreviewHint` riêng của feature (README ghi "`Row` icon và chữ", không phải thành phần chung); T-015 ("Đã khóa lớp phủ") và T-017 ("Lùi lại để cả người vào khung") dùng lại.

**Màn hình thấp (360 x 640).** Khung xem trước co còn khoảng 223 x 297 (plan T-013). Thẻ bước theo thiết kế (nhãn 41, hai mũi tên 80, lề) chỉ còn khoảng 25 dp cho chữ. `StepCard` dùng `BoxWithConstraints`: rộng từ 300 dp trở lên thì một hàng như thiết kế; hẹp hơn thì hàng trên là nhãn và chữ, hàng dưới là hai mũi tên căn phải (mục 8, câu 10). Lời nhắc chọn tư thế xuống dòng trong khung.

**Thành phần mới trong `:core:designsystem`** (`component/`, `sticker.md` mục 4, bảng ánh xạ của README; dùng vai trò màu Material nên đúng cả nền sáng của thư viện tư thế, T-021):

- `SelectedBadge(modifier)`: tròn 22, nền `secondaryContainer`, viền `Stroke.sticker` màu `outline`, icon `check` 14 màu `onSecondaryContainer`. Chỉ để trang trí (không có mô tả cho trình đọc màn hình); trạng thái chọn nằm ở ngữ nghĩa của thành phần cha.
- `StickerChip(label: String, selected: Boolean, onClick: () -> Unit, modifier)`: cao 36, bo `Radius.chip`, lề ngang `Spacing.md`, viền `Stroke.sticker` màu `outline`, chữ `labelLarge`. Thường: nền `surface`, chữ `onSurface`. Chọn: nền `secondaryContainer`, chữ `onSecondaryContainer`, `SelectedBadge` ở góc trên phải (lệch ra ngoài 8 dp ngang, 9 dp lên; thiết kế: badge tại 78, 647 với chip 16, 656 rộng 76). `Modifier.selectable(selected, role = Role.Tab)`, `minimumInteractiveComponentSize()` để vùng chạm cao 48. Không cắt badge (`clip` chỉ cho nền).
- `PoseThumb(image: ImageBitmap?, selected: Boolean, onClick: () -> Unit, contentDescription: String, modifier, failed: Boolean = false)`: ô 64, bo `Radius.thumb`, nền `surfaceVariant`. Thường: viền `Stroke.sticker` màu `outline`. Chọn: viền `Stroke.selected` màu `secondaryContainer` và `SelectedBadge` (lệch ra ngoài 7 dp ngang, 7 dp lên; thiết kế: badge 137, 701 với ô 88, 708). Có `image`: ảnh `ContentScale.Crop` cắt theo bo góc. `failed`: icon `refresh` 24 màu `onSurfaceVariant` giữa ô. `Modifier.selectable(selected, role = Role.Button)`.
- `StepCard(text: String, modifier, badge: String? = null, trailing: @Composable RowScope.() -> Unit = {})` và `StepCardArrows(onPrevious, onNext, previousEnabled, nextEnabled, previousDescription, nextDescription)`: nền `cameraControl`, viền `Stroke.sticker` `cameraOnControl`, bo `Radius.card`, lề trên/phải/dưới/trái `Spacing.sm`/`Spacing.xs`/`Spacing.sm`/`Spacing.md`, các phần cách `Spacing.sm`. Nhãn: viên thuốc nền `secondaryContainer`, viền `Stroke.sticker` `cameraOnControl`, lề `Spacing.xs` dọc và `Spacing.sm` ngang, chữ `labelLarge` `onSecondaryContainer`. Chữ `bodyMedium` màu `cameraOnControl`, `weight(1f)`. Mỗi mũi tên vùng chạm 40 x 48, icon 24; tắt thì độ mờ 38% và không bấm được. Bố cục hẹp như đoạn trên.
- Biểu tượng mới của core: `ic_check.xml`, `ic_chevron_left.xml`, `ic_chevron_right.xml` (Material Symbols Outlined như T-013; `third_party/material-symbols/LICENSE.txt` của core đã có). `ic_refresh.xml` đã có.
- Catalog: trang mới `PoseComponentCatalog`: chip thường và đang chọn, `SelectedBadge`, ô thường, đang chọn, đang tải, lỗi (ảnh tạm vẽ bằng `Canvas` trong catalog), `StepCard` có mũi tên (bước đầu, giữa) và `StepCard` có nút chữ. Hai screenshot test mới, sáng và tối.

**Biểu tượng của feature.** `ic_camera_steps.xml` (`format_list_numbered`), `ic_camera_swipe.xml` (`swipe_left`), cùng nguồn và cách ghi chú như 12 biểu tượng của T-013. Tải không được là lỗi hạ tầng (auto-run.md, bước 5).

**Chữ mới (`strings.xml` của feature).** Theo bảng chữ của thiết kế: `camera_chip_all` "Tất cả", `camera_step_count` "%1$d/%2$d", `camera_show_steps` "Bước", `camera_no_pose_hint` "Chọn một tư thế ở dải bên dưới". Theo spec mục 7: `camera_outline_failed` "Chưa tải được nét viền", `camera_outline_retry` "Thử lại". AI đề xuất (mục 8, câu 8 và 11): `camera_poses_failed` "Chưa tải được danh sách tư thế.", `camera_poses_retry` "Thử lại", `camera_cd_step_previous` "Bước trước", `camera_cd_step_next` "Bước sau", `camera_cd_hide_steps` "Ẩn thẻ bước", `camera_cd_show_steps` "Hiện thẻ bước", `camera_cd_thumb_failed` "%1$s, chưa tải được ảnh, chạm để thử lại".

**Thứ tự làm.** Bước 3: khung rỗng (mục 4), mọi test, `strings.xml` đủ chữ; chạy để thấy thất bại đúng lý do; commit. Bước 4: `PoseLibrary.from`; bốn thành phần core, biểu tượng core, trang catalog; repository ẩn thẻ bước; `ActivePoseHolder`, `PoseImageDecoder`; `PoseViewModel`; `CameraViewModel`, `CapturedPhoto`; `PosePicker`, `PoseOverlay`, `PoseOutline`; `CameraScreen`, `CameraControls`, `CameraRoute`; điều hướng; biểu tượng feature; `scripts/check.sh` ghi ảnh chuẩn; `scripts/design-compare.sh camera`; build `stagingDebug`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/build.gradle.kts` | Thêm `implementation(project(":core:poses"))` (plan T-009, câu 7) |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseUiState.kt` | `PoseUiState`, `CategoryChip`, `PoseThumbItem`, `SelectedPose`, `StripScroll`, `PoseImage` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseViewModel.kt` | ViewModel theo mục 3.1 |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/ActivePoseHolder.kt` | Tư thế đang dùng, dùng chung với `CameraViewModel` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseImageDecoder.kt` | Interface và `BitmapPoseImageDecoder` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PosePicker.kt` | `PoseActions`, hàng chip, dải tư thế, hàng lỗi catalog |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseOverlay.kt` | Lớp trên khung: nét viền, thẻ bước và cử chỉ vuốt, nút "Bước", `PreviewHint` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseOutline.kt` | Nét viền tô màu có quầng |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/data/PoseOverlaySettingsRepository.kt` | Interface |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/data/DataStorePoseOverlaySettingsRepository.kt` | Khóa `camera_steps_hidden` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/di/PoseModule.kt` | Gắn repository và `PoseImageDecoder` (file riêng, không chạm `CameraModule.kt` mà T-016 có thể sửa) |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | `CameraRoute` thêm `PoseViewModel`; `CameraScreen` thêm `poseState`, `poseActions`, `PosePicker` thay chỗ trống; thẻ test mới trong `CameraTestTags` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraControls.kt` | Slot `poseOverlay` trong `PreviewContent`, `ReadyPreview`. Nút đổi camera chỉ đổi nếu mục 8, câu 9 chọn phương án trắng |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModel.kt` | Nhận `ActivePoseHolder`; `pose_id` của `photo_captured` và `CapturedPhoto.poseId` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/capture/CapturedPhotoHolder.kt` | `CapturedPhoto` thêm `poseId: String = ""` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/navigation/CameraNavigation.kt` | `CameraDestination` thêm `poseId: String? = null` |
| Sửa | `feature/camera/src/main/res/values/strings.xml` | Chữ mới ở mục 3.1 |
| Tạo | `feature/camera/src/main/res/drawable/ic_camera_steps.xml`, `ic_camera_swipe.xml` | Hai biểu tượng Material Symbols |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseFakes.kt` | `FakePoseLibraryRepository`, `FakePoseCatalogRepository`, `FakePoseHistoryRepository`, `FakePoseOverlaySettingsRepository`, `FakePoseImageDecoder`, `FakeNetworkStatus`, `PoseFixtures` |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseViewModelTest.kt` | Unit test ViewModel (Robolectric, vì cần `ImageBitmap`) |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/BitmapPoseImageDecoderTest.kt` | Giải mã và thu nhỏ ảnh PNG do test tạo |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/TestPoseImages.kt` | Ảnh tạm (hình người nét trắng) vẽ bằng `Canvas` cho test giao diện và screenshot |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/data/DataStorePoseOverlaySettingsRepositoryTest.kt` | Unit test repository |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenPoseTest.kt` | Test giao diện phần tư thế (file riêng, không sửa `CameraScreenTest.kt`) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModelTest.kt` | Hàm dựng ViewModel truyền `ActivePoseHolder()`; một test mới (mục 4) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenScreenshotTest.kt` | Hàm `Screen` nhận `poseState` (mặc định trạng thái mẫu giống thiết kế); bốn test mới |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_default_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_no-pose_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_steps-hidden_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_default_small.png` | Khung 360 x 640, nhiều nội dung nhất (L-028) |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_starting_dark.png` | Thêm hàng chip và dải tư thế |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_permission-denied_dark.png` | Như trên |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_error_dark.png` | Như trên |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_countdown_dark.png` | Như trên, thêm nét viền và thẻ bước |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_ready_dark.png` | Như trên, thêm nét viền và thẻ bước |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_permission-denied_small.png` | Thêm hàng chip và dải tư thế |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_countdown_small.png` | Thêm hàng chip, dải tư thế, nét viền, thẻ bước hai hàng |
| Ghi lại | `docs/features/camera/design/review/` | Sinh bởi `scripts/design-compare.sh camera` (ảnh ghép và `summary.md`), không sửa tay |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/StickerChip.kt` | Thành phần mới |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/SelectedBadge.kt` | Thành phần mới |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PoseThumb.kt` | Thành phần mới |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/StepCard.kt` | Thành phần mới, `StepCardArrows` |
| Tạo | `core/designsystem/src/main/res/drawable/ic_check.xml`, `ic_chevron_left.xml`, `ic_chevron_right.xml` | Ba biểu tượng Material Symbols |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/catalog/DesignSystemCatalog.kt` | Thêm `PoseComponentCatalog`; các trang cũ không đổi |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/PoseComponentsTest.kt` | Test giao diện bốn thành phần (file riêng, không chạm `ComponentsTest.kt`, `CameraComponentsTest.kt`) |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignSystemCatalogScreenshotTest.kt` | Thêm `poseComponents_light`, `poseComponents_dark` |
| Tạo | `core/designsystem/src/test/screenshots/DesignSystem_pose_components_light.png` | Ảnh chuẩn mới |
| Tạo | `core/designsystem/src/test/screenshots/DesignSystem_pose_components_dark.png` | Ảnh chuẩn mới |
| Sửa | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/model/PoseLibrary.kt` | `companion object` với `from(catalog, saved, recent)` |
| Tạo | `core/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/PoseLibraryFromTest.kt` | `from` cho cùng kết quả với kho thư viện |

Không file nào khác. Đặc biệt không sửa: `CameraScreenTest.kt`, `CameraFakes.kt`, `CameraModule.kt`, `CameraUiState.kt` của T-013; `CameraIconButton.kt` (mục 8, câu 12); `AppNavHost.kt` và mọi file của `:app`; `gradle/libs.versions.toml`, `build-logic/`, `settings.gradle.kts`; `docs/design-system/tokens.json` (không thêm giá trị thiết kế mới: mọi màu, cỡ, bo góc đã có token).

**Nơi khác đọc các file trên** (L-020), không cần sửa:

- `CameraScreen.kt`, `CameraControls.kt`: `CameraScreenTest.kt` gọi `CameraScreen` không có hai tham số mới (giá trị mặc định `Loading` vẽ vùng tư thế trống, như T-013), nên các test cũ giữ ý nghĩa; `AppLaunchTest.kt` chỉ chờ `camera_screen`, thẻ này không đổi.
- `CameraViewModel.kt`: chỉ `CameraViewModelTest.kt` (trong danh sách) dựng ViewModel; Hilt tự cung cấp `ActivePoseHolder` (`@Singleton` có `@Inject constructor`).
- `CapturedPhotoHolder.kt`: `CameraViewModelTest.kt` so `CapturedPhoto(...)` bốn tham số; trường mới có giá trị mặc định nên vẫn bằng.
- `CameraNavigation.kt`: `AppNavHost.kt` gọi `CameraDestination()`; tham số mới có giá trị mặc định nên không đổi.
- Bảy ảnh chuẩn ghi lại: chỉ `CameraScreenScreenshotTest.kt` (trong danh sách) và `scripts/design-compare.sh` (đọc theo tên ảnh thiết kế, ghi `review/` đã có trong danh sách) đọc.
- `DesignSystemCatalog.kt`: chỉ `DesignSystemCatalogScreenshotTest.kt` gọi; trang cũ không đổi nên bảy ảnh catalog cũ không đổi.
- `PoseLibrary.kt`: test của `:core:poses` dựng thư viện qua `buildPoseLibrary` và kho thư viện; chỉ thêm `companion object`, không đổi constructor hay hàm có sẵn.
- `feature/camera/build.gradle.kts`: `checkModuleDependencies` đọc phụ thuộc lúc build; `:core:poses` là core nên qua.
- DataStore chung: khóa mới `camera_steps_hidden`, không trùng khóa nào (`camera_flash`, `camera_timer`, `camera_grid`, `camera_lens`, `poses_saved`, `poses_recent`), không chuyển dữ liệu cũ.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không thêm. Phần tư thế thuộc màn camera, sau cờ `feature_camera_enabled` của T-013. Plan không đọc cờ nào, nên chỗ plan T-013 mục 3.3 và 3.1 nói khác nhau về cách đọc cờ camera không ảnh hưởng plan này.
- Quảng cáo: Không chạm. Camera không có vị trí quảng cáo (spec mục 5).
- Sự kiện phân tích (điểm chặn 4, đã có trong spec mục 6):
  - `pose_selected`: `pose_id`, `category` (mã nhóm trong catalog, ví dụ `women`), `source` là `strip` (chạm ô) hoặc `next` (`onNextPose`, T-016 nối). Không ghi `detail` (mục 8, câu 3) và không ghi khi camera tự chọn tư thế lúc mở.
  - `content_load_failed`: `content` là `thumbnail` hoặc `outline`; `reason` từ `LoadFailureReason.analyticsValue` của `:core:poses`: `network`, `not_found`, `other`, và `timeout` khi chờ quá 15 giây (mục 8, câu 4). Giải mã ảnh lỗi ghi `other`.
  - `photo_captured`: `pose_id` nay là mã tư thế đang dùng (rỗng khi không có tư thế), như plan T-013 câu 3 đã định.
- Dữ liệu cục bộ: khóa mới `camera_steps_hidden` (spec mục 6, dòng "thẻ bước ẩn hay hiện"); "đã dùng gần đây" ghi qua `PoseHistoryRepository.markUsed` sẵn có (`poses_recent`). Khóa mới, không chuyển dữ liệu cũ, nên không chạm điểm chặn 9.
- File R2: Không chạm cách đặt tên hay đường dẫn. Ảnh nhỏ và nét viền tải qua `PoseCatalogRepository.loadFile` (đường dẫn lấy từ catalog, ADR-0015).

## 4. Kiểm thử

Mọi test dưới đây vào commit test của bước 3, cùng khung rỗng để biên dịch được: bốn thành phần core vẽ `Box(modifier)` rỗng; `PoseComponentCatalog` là `Box` rỗng; `PoseLibrary.from` trả `buildPoseLibrary(PoseCatalog("", emptyList(), emptyList()), emptyList(), emptyList())` (thư viện rỗng); `PoseViewModel` có đủ hàm nhưng không làm gì, `uiState` luôn `Loading`; `DataStorePoseOverlaySettingsRepository` trả `flowOf(false)` và không ghi; `BitmapPoseImageDecoder.decode` trả `null`; `ActivePoseHolder` có `current` luôn `null`; `CameraViewModel` đã nhận `ActivePoseHolder` nhưng chưa đọc; `CapturedPhoto` đã có `poseId`; `CameraDestination` đã có `poseId`; `PosePicker` và `PoseOverlay` chỉ là `Box` trống đúng chiều cao; `strings.xml` đủ chữ. Phần sửa trong `CameraViewModelTest.kt` (hàm dựng truyền `ActivePoseHolder()` và test mới) và trong `CameraScreenScreenshotTest.kt` cùng vào commit test của bước 3 (L-025). Lý do thất bại ở cột cuối là điều AI dự kiến; bước 3 chạy và ghi lý do thật vào nhật ký.

Bản giả (`PoseFakes.kt`): `PoseFixtures.catalog()` có nhóm `women` ("Nữ"), `men` ("Nam"), `kids` (không có tư thế); tư thế `w01`, `w02`, `w03` (nhóm `women`, bốn bước tiếng Anh "Step 1" đến "Step 4"), `m01`, `m02` (nhóm `men`, ba bước), theo thứ tự catalog `w01`, `m01`, `w02`, `m02`, `w03`; tên tiếng Anh "Pose <id>". `FakePoseLibraryRepository`: `MutableStateFlow<LibraryState>`, đặt bằng `PoseLibrary.from(...)`, đếm `retryCount`. `FakePoseCatalogRepository`: ghi lại `loads: List<Pair<String, PoseFile>>`, kết quả theo `(id, file)` đặt được (mặc định `Ready(File("<id>-<file>"))`), có thể treo bằng `CompletableDeferred`. `FakePoseHistoryRepository`: ghi `used: List<String>`, `failWrites`. `FakePoseOverlaySettingsRepository`: `MutableStateFlow(false)`, ghi `saves`, `failWrites`. `FakePoseImageDecoder`: trả `ImageBitmap(1, 1)`, trừ file trong `undecodable`. `FakeNetworkStatus`: `dialogShown = MutableStateFlow(false)`, `recovered = MutableSharedFlow<Unit>()`. `PoseViewModelTest` chạy bằng Robolectric (`@RunWith(AndroidJUnit4::class)`) vì `ImageBitmap` cần `Bitmap` của Android, cùng `MainDispatcherRule`. Kiểm tra lỗi so loại (`error is IOException`).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, lần đầu là tư thế mặc định | Unit test | `PoseViewModelTest.firstOpen_selectsDefaultPose_withoutEventOrUse`: thư viện không có đã dùng; `selected.id == "w01"`, `stepIndex == 0`; `loads` có `("w01", OUTLINE)`; `analytics.events` rỗng; `history.used` rỗng; `activePose.current.value?.id == "w01"`. Thất bại: khung rỗng ở `Loading` |
| KB-1, tư thế dùng lần trước hiện sẵn | Unit test | `lastUsedPose_isSelectedOnOpen_withOutline`: `recent = [UsedPose("m02", 5)]`; `selected.id == "m02"`; nét viền `PoseImage.Ready` sau khi chạy hết |
| KB-1, tư thế dùng lần trước không còn | Unit test | `lastUsedMissingFromCatalog_fallsBackToDefault`: `recent = [UsedPose("gone", 5)]`; `selected.id == "w01"` |
| KB-1, hàng chip | Unit test | `chips_areCategoriesWithPoses_inCatalogOrder`: `categories == [CategoryChip("women", "Nữ"), CategoryChip("men", "Nam")]` (không có `kids`), `selectedCategoryId == null` |
| KB-1, thứ tự dải "Tất cả" | Unit test | `strip_all_savedThenRecentThenCatalog`: `saved = [SavedPose("w03", 9)]`, `recent = [UsedPose("m02", 8), UsedPose("w02", 7)]`; `strip.map { it.id } == ["w03", "m02", "w02", "w01", "m01"]` |
| KB-1, thứ tự trong nhóm | Unit test | `strip_category_sameOrderWithinGroup`: cùng dữ liệu, `onCategorySelect("women")`; `["w03", "w02", "w01"]`; `selected.id` vẫn `"m02"`; `scrollRequest.index == 0` |
| KB-1, thứ tự không nhảy khi vừa chọn | Unit test | `selectingPose_doesNotReorderStrip`: `onPoseSelect("m01")`; thư viện giả phát lại với `recent` mới có `m01` đầu; `strip` giữ nguyên thứ tự cũ, `selected.id == "m01"` |
| KB-1, chạm ô | Unit test | `selectFromStrip_logsEvent_marksUsed_resetsStep`: `onNextStep()`, rồi `onPoseSelect("m01")`; `analytics.events == [AnalyticsEvent("pose_selected", mapOf("pose_id" to "m01", "category" to "men", "source" to "strip"))]`; `history.used == ["m01"]`; `stepIndex == 0`; `steps.size == 3`; `loads` có `("m01", OUTLINE)`; `activePose.current.value?.id == "m01"` |
| KB-1, chạm lại ô đang chọn | Unit test | `selectSamePose_doesNothing`: `onPoseSelect("w01")` khi đang chọn `w01`; không có sự kiện, `used` rỗng |
| KB-1, ghi "đã dùng" lỗi | Unit test | `markUsedFails_keepsSelection_andLogsWarning`: `history.failWrites = true`; `onPoseSelect("m01")`; `selected.id == "m01"`; có log `WARN` với `error is IOException` |
| KB-1, mở từ chi tiết tư thế | Unit test | `poseIdArgument_selectsThatPose_andScrollsToIt`: `SavedStateHandle(mapOf("poseId" to "w02"))`; `selected.id == "w02"`, `selectedCategoryId == null`, `scrollRequest.index == strip.indexOfFirst { it.id == "w02" }`; `history.used == ["w02"]`; không có `pose_selected`. `poseIdSetLater_switchesPose_andSwitchesChipToAll`: đang ở chip `men`, đặt `savedStateHandle["poseId"] = "w03"`; `selected.id == "w03"`, chip về `null`, khóa `poseId` về `null`. `poseIdNotInCatalog_keepsCurrent_andLogsWarning` |
| KB-1, ảnh nhỏ chưa tải không chặn chọn | Unit test | `thumbnailLoading_doesNotBlockSelection`: tải ảnh nhỏ `m01` treo; `onThumbShown("m01")`; ô `m01` là `PoseImage.Loading`; `onPoseSelect("m01")` vẫn chọn |
| KB-1, ảnh nhỏ tải khi hiện | Unit test | `thumbShown_loadsOnce`: `onThumbShown("m01")` hai lần; `loads` có `("m01", THUMBNAIL)` đúng một lần; ô `Ready` |
| KB-1, ảnh nhỏ lỗi, chạm để thử | Unit test | `thumbnailFails_marksFailed_logsEvent_andTapRetries`: kết quả `Failed(PoseLoadFailure(THUMBNAIL, NETWORK))`; ô `Failed`; sự kiện `AnalyticsEvent("content_load_failed", mapOf("content" to "thumbnail", "reason" to "network"))`; `onThumbShown` lần nữa không tải lại; đổi kết quả sang `Ready`, `onPoseSelect("m01")`: tải lại, ô `Ready`, `selected.id == "m01"` |
| Ảnh không giải mã được | Unit test | `undecodableThumbnail_isFailed_withReasonOther` |
| KB-1, không có tư thế | Unit test | `emptyCatalog_readyWithoutPose`: catalog không có tư thế; `Ready(selected = null, strip = [])`; `activePose.current.value == null` |
| Catalog lỗi và thử lại | Unit test | `libraryFailed_isUnavailable_andRetryCallsLibrary`: `LibraryState.Failed(...)`; `Unavailable`; `onRetryLibrary()`: `retryCount == 1` |
| Mạng trở lại thì tải lại | Unit test | `networkRecovered_reloadsFailedImagesAndLibrary`: ảnh nhỏ `m01` và nét viền đang lỗi; đổi kết quả sang `Ready`; phát `recovered`; cả hai `Ready`. Với `Unavailable`: `retryCount == 1` |
| Spec mục 7, nét viền lỗi | Unit test | `outlineFails_marksFailed_logsEvent_andRetryReloads`: `Failed(PoseLoadFailure(OUTLINE, TIMEOUT))`; `selected.outline == Failed`; sự kiện `mapOf("content" to "outline", "reason" to "timeout")`; `onRetryOutline()` với kết quả `Ready`: `Ready` |
| Đổi tư thế khi nét viền cũ còn đang tải | Unit test | `changingPose_ignoresOldOutlineResult`: nét viền `w01` treo, chọn `m01`; hoàn tất `w01`; `selected.id == "m01"` và nét viền là của `m01` |
| KB-2, chuyển bước, đầu và cuối | Unit test | `steps_moveWithinBounds`: `onPreviousStep()` ở bước 0 giữ 0; ba lần `onNextStep()` tới 3; thêm một lần giữ 3; `onPreviousStep()` về 2 |
| KB-2, đổi tư thế về bước 1 | Unit test | Trong `selectFromStrip_logsEvent_marksUsed_resetsStep` (ở trên) |
| KB-2, ẩn hiện và giữ | Unit test | `hideSteps_isSaved_andKeptWhenPoseChanges`: `onHideSteps()`; `stepsHidden == true`; `overlay.saves == [true]`; `onPoseSelect("m01")`: vẫn `true`; `onShowSteps()`: `false`, `saves == [true, false]`. `stepsHiddenStored_isHiddenOnOpen`: repository có `true`; trạng thái đầu `stepsHidden == true`. `saveStepsFails_keepsChoice_andLogsWarning` |
| KB-2, giữ khi mở lại app | Unit test | `DataStorePoseOverlaySettingsRepositoryTest` (DataStore trong bộ nhớ như `DataStoreCameraSettingsRepositoryTest`): `nothingStored_isShown` (`false`), `setStepsHidden_isReadBack`, `readFailsOnce_recovers`. Thất bại: khung rỗng không ghi |
| KB-6, tư thế tiếp (cho T-016) | Unit test | `nextPose_followsStrip_andWraps`: chip `women`, dải `[w01, w02, w03]`, đang chọn `w02`; `onNextPose()`: `w03`, sự kiện `source` `next`; lần nữa: `w01` |
| KB-6, `photo_captured` có `pose_id` | Unit test | `CameraViewModelTest.shutter_withActivePose_logsItsId_andKeepsItOnPhoto`: `activePose.set(PoseFixtures.pose("w01"))`, hẹn giờ tắt, `onShutterClick()`; sự kiện có `"pose_id" to "w01"`; `holder.latest.value?.poseId == "w01"`. Test cũ `shutter_timerOff_capturesAtOnce` vẫn đúng với `pose_id` rỗng. Thất bại: khung rỗng chưa đọc holder, `pose_id` rỗng |
| `:core:poses`, hàm tạo thư viện | Unit test | `PoseLibraryFromTest.from_matchesLibraryOrder`: với `Fixtures.sixPoseCatalog()`, `saved`, `recent`: `posesFor(All)`, `posesFor(Category("x"))`, `categories`, `defaultPose`, `lastUsedPose` bằng kết quả của `buildPoseLibrary`. `from_defaultsToEmptyHistory`. Thất bại: khung rỗng trả thư viện rỗng |
| Giải mã ảnh | Unit test (Robolectric, `@GraphicsMode(NATIVE)`) | `BitmapPoseImageDecoderTest.decode_scalesDownToTarget`: PNG 1024 x 2048 ghi bằng `Bitmap.compress` vào `TemporaryFolder`; `decode(file, 256)` cho ảnh rộng 256 hoặc 512 (cạnh ngắn không nhỏ hơn 256, giảm theo lũy thừa 2); `decode_smallImage_keepsSize`; `decode_notAnImage_returnsNull` |
| KB-1, chip trên màn hình | Compose UI test | `CameraScreenPoseTest.chips_showAllFirst_thenCategories_andSelect`: `Ready` camera và tư thế; thứ tự chữ "Tất cả", "Nữ", "Nam"; "Tất cả" `assertIsSelected`; bấm "Nam" gọi `onCategorySelect("men")`; "Tất cả" gọi `onCategorySelect(null)` |
| KB-1, ô trên màn hình | Compose UI test | `strip_showsThumbs_selectedMarked_andSelects`: `camera_pose_w02` `assertIsSelected`, `camera_pose_w01` `assertIsNotSelected`; bấm `w01` gọi `onPoseSelect("w01")`; mỗi ô được dựng gọi `onThumbShown` với mã của nó |
| KB-1, ô lỗi | Compose UI test | `failedThumb_hasRetryDescription_andTapSelects`: ô `Failed` có mô tả "Pose w01, chưa tải được ảnh, chạm để thử lại"; bấm gọi `onPoseSelect("w01")` |
| KB-1, cuộn tới ô | Compose UI test | `scrollRequest_scrollsStripToPose`: 20 ô, `scrollRequest = StripScroll(15, 1)`; `camera_pose_p15` `assertIsDisplayed` |
| KB-1, nét viền | Compose UI test | `outline_shownWhenCameraReadyAndLoaded`: nét viền `Ready` có `OUTLINE`; nét viền `Loading` không có `OUTLINE` |
| KB-1, không có tư thế | Compose UI test | `noPose_showsHint_withoutOutlineOrSteps`: `selected = null`; có chữ "Chọn một tư thế ở dải bên dưới"; không có `OUTLINE`, `STEP_CARD`, `SHOW_STEPS` |
| KB-7, chip và dải khi chưa có quyền | Compose UI test | `permissionDenied_chipsAndStripStillWork`: `CameraUiState.PermissionDenied`; bấm "Nam" và `camera_pose_w01` gọi đúng callback; không có `OUTLINE`, `STEP_CARD`. Cùng ý cho `Starting` và `Error`: `notReady_hidesOverlay_keepsPicker` |
| KB-4, đổi camera giữ tư thế | Compose UI test | `flipLens_keepsPoseOverlay`: cùng `poseState`, `Ready(lens = BACK)` rồi `Ready(lens = FRONT)` (dựng lại bằng `mutableStateOf`); `OUTLINE` và chữ bước vẫn có |
| KB-2, thẻ bước | Compose UI test | `stepCard_showsCountAndText`: `stepIndex = 1` của bốn bước; có "2/4" và "Step 2". Chữ bước đầu và cuối theo `stepIndex` |
| KB-2, mũi tên | Compose UI test | `stepArrows_clickAndDisableAtEnds`: bước 0: "Bước trước" `assertIsNotEnabled`, "Bước sau" bấm gọi `onNextStep`; bước 3: "Bước sau" `assertIsNotEnabled`, "Bước trước" gọi `onPreviousStep` |
| KB-2, vuốt lên để ẩn | Compose UI test | `swipeUpOnStepCard_hides`: `onNodeWithTag(STEP_CARD).performTouchInput { swipeUp() }` gọi `onHideSteps` đúng một lần. `smallDragUp_doesNotHide`: kéo lên 10 dp không gọi. `stepCard_hasHideAccessibilityAction`: có hành động "Ẩn thẻ bước", gọi `onHideSteps` |
| KB-2, nút "Bước" | Compose UI test | `stepsHidden_showsStepsButton_andShowsCard`: `stepsHidden = true`; không có `STEP_CARD`; có "Bước" (`SHOW_STEPS`) cao 48; bấm gọi `onShowSteps` |
| Spec mục 7, nét viền lỗi | Compose UI test | `outlineFailed_stepCardShowsRetry`: có "Chưa tải được nét viền"; không có "Bước trước"; bấm "Thử lại" gọi `onRetryOutline`; không có `OUTLINE` |
| Catalog lỗi trên màn hình | Compose UI test | `posesUnavailable_showsRetry`: `PoseUiState.Unavailable`; có "Chưa tải được danh sách tư thế."; bấm "Thử lại" gọi `onRetryLibrary` |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | Các test trên so đúng chuỗi của README mục 2 ("Tất cả", "Bước", "Chọn một tư thế ở dải bên dưới", "2/4"). Chữ spec mục 7 và chữ AI đề xuất so theo mục 3.1 |
| Màn hình thấp (L-028) | Compose UI test, `@Config(qualifiers = "w360dp-h640dp-xxhdpi")` | `small_default_stepCardInsidePreview_andPickerFullSize`: mép dưới của `STEP_CARD` không quá mép dưới của `PREVIEW`; chữ bước có chiều rộng ít nhất 100 dp (bố cục hai hàng); hai mũi tên mỗi cái 40 x 48; `CHIPS` và `STRIP` giữ cao 52 và 80; `SHUTTER` vẫn nằm trọn trong màn hình. `small_noPose_hintInsidePreview`: lời nhắc nằm trọn trong `PREVIEW` |
| Thành phần `StickerChip` | Compose UI test, `@GraphicsMode(NATIVE)` | `PoseComponentsTest.stickerChip_height36_touch48`: phần nhìn thấy cao 36 (đo qua phần tử bọc có `testTag`), vùng chạm cao 48. `stickerChip_selected_isYellowWithBadge`: trong `AppTheme(darkTheme = true)` điểm (8 dp, 18 dp) là #FFC83D khi chọn, #1E1E1E khi thường; trong sáng là #FFFFFF khi thường; có `SelectedBadge` khi chọn, không có khi thường. `stickerChip_click_andSelectedSemantics` |
| Thành phần `SelectedBadge` | Compose UI test, `@GraphicsMode(NATIVE)` | `selectedBadge_is22dpYellow`: 22 x 22; điểm giữa (11 dp, 4 dp) là #FFC83D |
| Thành phần `PoseThumb` | Compose UI test, `@GraphicsMode(NATIVE)` | `poseThumb_is64dp`. `poseThumb_selectedBorder_isYellow3dp`: điểm (1.5 dp, 32 dp) là #FFC83D khi chọn, #FFFFFF khi thường (nền tối). `poseThumb_failed_showsRetryIcon` (có node icon), `poseThumb_loading_isEmptyFill`: điểm giữa là #2A2A2A. `poseThumb_click_andSelectedSemantics` |
| Thành phần `StepCard` | Compose UI test, `@GraphicsMode(NATIVE)` | `stepCard_wide_oneRow`: rộng 379: chữ và mũi tên cùng hàng (đỉnh mũi tên cao hơn đáy chữ). `stepCard_narrow_twoRows`: rộng 190: mũi tên nằm dưới chữ, chữ rộng ít nhất 100 dp. `stepCardArrows_disabled_notClickable`. `stepCard_badge_isYellowPill`. `stepCard_trailingSlot_replacesArrows` |
| Giao diện catalog | Screenshot test | `DesignSystemCatalogScreenshotTest.poseComponents_light` (`src/test/screenshots/DesignSystem_pose_components_light.png`), `poseComponents_dark` (`src/test/screenshots/DesignSystem_pose_components_dark.png`), khung `w411dp-h891dp-xxhdpi` của lớp. Tám ảnh catalog cũ không đổi |
| Đúng thiết kế, ba ảnh mới | Screenshot test, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "w411dp-h891dp-xxhdpi")`, có `onOpenPoses` và `onOpenSettings` | `CameraScreenScreenshotTest.default_dark` (`src/test/screenshots/CameraScreen_default_dark.png`): flash tắt, hẹn giờ 3, lưới bật, zoom 0.5/1x/2 chọn 1x; chip "Tất cả", "Nữ", "Nam", "Cặp đôi", "Bạn bè", "Du lịch", "Cà phê", chọn "Tất cả"; 8 ô có ảnh tạm, chọn ô thứ hai; thẻ bước "2/4" "Xoay vai 45° về phía máy, cằm hơi hạ."; nét viền ảnh tạm. `noPose_dark` (`src/test/screenshots/CameraScreen_no-pose_dark.png`): như trên, `selected = null`. `stepsHidden_dark` (`src/test/screenshots/CameraScreen_steps-hidden_dark.png`): như `default`, `stepsHidden = true` |
| Bốn ảnh của T-013 và ảnh đủ công cụ | Screenshot test, ghi lại | `starting_dark`, `permissionDenied_dark`, `error_dark`, `countdown_dark`, `ready_dark` giữ tên file cũ; hàm `Screen` truyền trạng thái tư thế mẫu như `default_dark` |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` | `default_small` (`src/test/screenshots/CameraScreen_default_small.png`, nhiều nội dung nhất: thẻ bước hai hàng, nét viền, chip, dải). Ghi lại `permissionDenied_small`, `countdown_small` |
| So với thiết kế | Ảnh so sánh và rà soát độc lập (bước 6) | Chạy `scripts/design-compare.sh camera` sau khi ghi ảnh chuẩn; commit `docs/features/camera/design/review/` và dán `review/summary.md` vào mô tả PR (ADR-0020). Phiên rà soát xem ảnh ghép của bảy ảnh T-013, T-014 theo mục 7 của README thiết kế. Khác biệt đã biết: cột công cụ nét viền (T-015) ở `default`, `steps-hidden`, `countdown`; ảnh tạm của ô và nét viền; ô đang chọn ở `starting`, `permission-denied`, `error` (mục 8, câu 9) |
| Ảnh thiết kế của ticket khác | | Ảnh thiết kế của ticket khác (T-015, T-016, T-017, T-020): `CameraScreen_adjust_dark`, `CameraScreen_locked_dark`, `CameraScreen_matching_dark`, `CameraScreen_matched_dark`, `CameraScreen_no-person_dark`, `CameraScreen_default_dark_unfolded`, `ReviewScreen_default_dark`, `ReviewScreen_default_dark_unfolded`, `ReviewScreen_discard_dark`, `ReviewScreen_save-error_dark`, `ReviewScreen_saving_dark` |
| Nét viền rõ trên nền sáng và tối | Kiểm tra thủ công | Bản staging trên máy thật: hướng camera vào tường trắng và vào chỗ tối; nét viền đọc rõ ở cả hai. Lặp lại trên một máy Android 11 trở xuống (quầng bằng bốn bản lệch, không có `blur`) |
| Ảnh tư thế thật | Kiểm tra thủ công | Cài mới, qua loading: dải có ảnh của bộ khởi đầu ngay; cuộn tới các tư thế khác: ô trống rồi có ảnh. Tắt mạng, cuộn tới ô chưa tải: ô hiện dấu lỗi, hộp thoại mất mạng hiện; bật mạng, "Thử lại" ở hộp thoại: ô tự tải lại |
| Giữ tư thế và thẻ ẩn khi mở lại | Kiểm tra thủ công | Chọn một tư thế khác mặc định, vuốt thẻ bước lên; đóng app hẳn, mở lại: camera dùng tư thế đó, thẻ vẫn ẩn, nút "Bước" hiện |
| Vuốt bằng ngón tay | Kiểm tra thủ công | Vuốt thẻ bước lên bằng ngón tay trên máy thật: thẻ ẩn, không làm dải hay khung xem trước nhảy; bật TalkBack: thẻ có hành động "Ẩn thẻ bước", chip và ô đọc đúng trạng thái chọn |

**Ngoại lệ viết test trước:**

- Hai biểu tượng của feature và ba biểu tượng của core: tài nguyên, không có logic.
- Phần `CameraRoute` nối `PoseViewModel` vào màn hình (`hiltViewModel()`, `collectAsStateWithLifecycle`): không có test JVM riêng, như T-013; kiểm bằng `AppLaunchTest` có sẵn trên emulator (vẫn tới `camera_screen`) và kiểm tra thủ công.
- Nhánh quầng dưới API 31 của `PoseOutline`: Robolectric chạy `sdk=35`; kiểm tra thủ công trên máy Android 11 trở xuống.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 4 (thêm ghi `pose_selected`, `content_load_failed`, điền `pose_id` của `photo_captured`; đều có trong spec mục 6), điểm 7 (bốn thành phần mới, ba biểu tượng và một trang catalog trong `:core:designsystem`; hàm tạo công khai mới trong `:core:poses`). Không chạm điểm 2 (không thêm thư viện: giải mã ảnh bằng `BitmapFactory`), điểm 8 (đường dẫn R2 lấy từ catalog), điểm 9 (khóa DataStore mới, không chuyển dữ liệu cũ).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module (`:feature:camera`, `:core:designsystem`, `:core:poses`), khoảng 55 file kể cả 14 ảnh chuẩn |
| B. Độ rõ của yêu cầu | 2 | Spec xác nhận, thiết kế duyệt, nhưng còn câu hỏi ảnh hưởng cách làm: khi nào không có tư thế, thứ tự dải có giữ cố định không, ai ghi `pose_selected` `detail`, giá trị `timeout`, giao diện lỗi nét viền và lỗi catalog chưa có trong thiết kế (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Logic, cử chỉ vuốt, khung thấp có test JVM; độ rõ của nét viền trên nền thật, quầng dưới API 31, ảnh tải thật từ R2 và mất mạng chỉ kiểm tra thủ công |
| D. Ảnh hưởng chéo | 2 | Sửa `CameraScreen.kt`, `CameraControls.kt`, `CameraViewModel.kt`, `CapturedPhotoHolder.kt`, `CameraNavigation.kt`, `strings.xml`, test và ảnh chuẩn của camera mà T-016 cũng sửa; tạo `StickerChip`, `SelectedBadge`, sửa `DesignSystemCatalog.kt`, thêm `PoseLibrary.from` mà T-021 cũng cần. Không đổi hành vi mà feature khác dựa vào (mọi thay đổi chung đều là thêm, có giá trị mặc định) |
| E. Độ mới của kỹ thuật | 2 | Giải mã ảnh bằng `BitmapFactory`, cử chỉ vuốt tự viết, `Modifier.blur` có nhánh thay thế theo API, `LazyRow` cuộn theo yêu cầu: chưa có trong dự án nhưng phổ biến |
| **Tổng** | 11 | **Mức: Cao** (chạm điểm chặn, và tổng điểm cũng từ 9 trở lên) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `:core:designsystem` (bốn thành phần mới, ba biểu tượng, một trang catalog, hai ảnh chuẩn; không đổi thành phần có sẵn); `:core:poses` (`PoseLibrary.from`, chỉ thêm); DataStore chung (khóa `camera_steps_hidden`); `CameraScreen.kt`, `CameraControls.kt`, `CameraViewModel.kt`, `CapturedPhotoHolder.kt`, `CameraNavigation.kt`, `strings.xml`, `CameraViewModelTest.kt`, `CameraScreenScreenshotTest.kt`, bảy ảnh chuẩn của T-013 và `design/review/` của camera.
- Task khác có thể xung đột (các plan T-012, T-016, T-021, T-025 đang viết song song):
  - **T-016** (màn xem lại, cùng `:feature:camera`; plan `features/camera/plans/T-016-man-xem-lai.md` bản nháp viết song song): cùng sửa `CameraScreen.kt` (T-016 thêm `LastPhotoViewModel` vào `CameraRoute`, hai tham số có mặc định vào `CameraScreen`), `CameraControls.kt` (T-016 sửa `CaptureRow`, `LastPhotoSlot`; T-014 sửa `PreviewContent`, `ReadyPreview`), `CameraNavigation.kt` (T-016 thêm `onOpenLastPhoto` vào `cameraScreen`; T-014 thêm `poseId` vào `CameraDestination`), `CapturedPhotoHolder.kt` (cả hai cùng định thêm `CapturedPhoto.poseId: String = ""`; plan T-016 ghi bỏ qua nếu T-014 đã thêm, plan này cũng vậy), `CameraViewModel.kt` (chỉ phương án A của T-016), `design/review/`. T-016 để chữ ở `strings_review.xml` riêng và không đổi ảnh chuẩn camera. Chung file nhưng khác phần, chỉ xung đột dòng. **Điểm cần chỉnh:** phương án A của plan T-016 giả định dải tư thế nằm trong `CameraViewModel`; plan này đặt nó trong `PoseViewModel` và làm sẵn `onNextPose()`. Nếu giữ thiết kế của plan này, phương án A của T-016 nên là: `CameraRoute` gọi `poseViewModel.onNextPose()` khi `NextPoseRequests.consume()` trả `true` lúc màn camera bắt đầu lại, không sửa `CameraViewModel` (mục 8, câu 14).
  - **T-021** (thư viện tư thế, `:feature:poses` mới; plan `features/poses/plans/T-021-thu-vien-tu-the.md` bản nháp viết song song): dùng `StickerChip`, `SelectedBadge` của T-014, giả định `StickerChip` nhận chữ, `selected`, `onClick`, `modifier` và tự vẽ badge khi chọn (đúng API ở mục 3.1), nên phải chạy sau T-014. Cùng sửa `DesignSystemCatalog.kt`, `DesignSystemCatalogScreenshotTest.kt` (T-021 thêm `PoseCard`), chỉ xung đột dòng. Plan T-021 không sửa `:core:poses` và tự chép `PoseLibrary` sang kiểu riêng để test; sau T-014 nó có thể dùng `PoseLibrary.from` thay. Thành phần ở đây dùng vai trò màu Material nên đúng cả nền sáng của thư viện (kiểm trong `PoseComponentsTest` và ảnh catalog sáng).
  - **T-015, T-017, T-020** (phụ thuộc T-014): T-015 thêm cột công cụ nét viền vào slot `poseOverlay`, thêm khóa vào `PoseOverlaySettingsRepository`, ghi lại `default`, `steps-hidden`, `countdown`; T-017 đọc `ActivePoseHolder` (điểm khớp) và đổi màu `PoseOutline`; T-020 dời bố cục.
  - **T-022** (chi tiết tư thế): điều hướng `CameraDestination(poseId = id)`; ghi `pose_selected` `detail` (mục 8, câu 3).
  - **T-012** (màn chào, sửa `MainScreenChooser`) và **T-025** (cài đặt): không chung file với T-014.
- Đề xuất thứ tự chạy:
  - **T-014 trước T-016.** "Tư thế tiếp" của T-016 cần dải và `onNextPose()` của T-014; `photo_saved` của T-016 cần `CapturedPhoto.poseId`; T-016 chạy sau thì ghi lại ảnh chuẩn camera một lần với đủ chip, dải, thẻ bước. Nếu T-016 chạy trước: T-014 vẫn làm được trên các file đã liệt kê (chỉ dừng khi T-016 đã chuyển phần màn hình sang file không có ở mục 3.2, mục 7), và T-016 phải tự định `pose_id` của `photo_saved` (rỗng) rồi T-014 điền.
  - **T-014 trước T-021**, để T-021 dùng lại `StickerChip`, `SelectedBadge`, `PoseLibrary.from` thay vì tạo lại. Nếu T-021 chạy trước và đã tạo các file này: xem mục 7, điều 3.
  - T-015, T-017, T-020 sau T-014 (ticket ghi `depends_on`).

## 7. Điều kiện dừng riêng

1. Lúc bắt đầu task, thiếu phần T-013, T-009 hay T-011 mà plan dựa vào: `POSE_AREA_HEIGHT` và `ReadyPreview` trong `CameraControls.kt`, `CameraDestination`, `CapturedPhoto`; `PoseLibraryRepository.library`, `PoseCatalogRepository.loadFile`, `PoseHistoryRepository.markUsed`, `LoadFailureReason.analyticsValue`; `NetworkStatus.recovered`. Dừng.
2. Thêm `PoseLibrary.from` cần sửa file khác của `:core:poses` ngoài `PoseLibrary.kt` (ví dụ đổi `buildPoseLibrary` sang công khai): dừng.
3. Lúc bắt đầu task, nhánh chính đã có `StickerChip.kt`, `SelectedBadge.kt`, `PoseThumb.kt` hoặc `StepCard.kt` (T-021 chạy trước), hoặc đã có hàm công khai dựng `PoseLibrary`: dùng lại bản có sẵn, không tạo file đó, ghi vào nhật ký. Bản có sẵn không vẽ được đúng thiết kế camera (màu, cỡ, badge) mà không sửa nó: dừng.
4. T-016 đã merge và đã chuyển phần dựng màn camera (`CameraScreen`, `PreviewContent`, `ReadyPreview`) sang file không có ở mục 3.2, hoặc đã đổi `CapturedPhoto` khiến cần sửa file của màn xem lại: dừng. Chỉ khác dòng trong các file đã liệt kê thì làm tiếp.
5. `Modifier.blur` không vẽ quầng trong screenshot Robolectric: dùng nhánh bốn bản lệch cho mọi API, ghi vào nhật ký; không dừng.
6. Tải Material Symbols không được: lỗi hạ tầng (auto-run.md, bước 5).
7. Cử chỉ vuốt của thẻ bước làm `LazyRow` hay khung xem trước nhận nhầm chạm trong test giao diện, và sửa cần đổi `CameraScreenTest.kt`: dừng.

## 8. Câu hỏi cho người duyệt

1. **Khi nào camera ở trạng thái không có tư thế?** Spec KB-1 luôn chọn sẵn tư thế dùng lần trước hoặc tư thế mặc định, nên với catalog có tư thế thì không bao giờ "chưa chọn tư thế nào". Ảnh `no-pose` lại có dải đầy ô và không ô nào được chọn. Đề xuất của AI: chỉ xảy ra khi catalog không có tư thế nào; chạm lại ô đang chọn không bỏ chọn. Nếu muốn người dùng tự bỏ tư thế (chụp không nét viền) thì cần thêm tiêu chí vào spec, ví dụ chạm lại ô đang chọn để bỏ chọn.
- Trả lời: Đồng ý với đề xuất của AI.
2. **Thứ tự dải có giữ cố định trong lúc camera mở không?** Chọn một ô thì ô đó thành "đã dùng gần đây" và theo luật xếp sẽ nhảy lên đầu, làm dải xáo trộn dưới ngón tay và làm "Tư thế tiếp" (T-016) không còn là ô bên cạnh. Đề xuất của AI: thứ tự chụp lại một lần khi camera mở, giữ tới khi màn camera bị hủy (thường là khi mở lại app); tư thế vừa lưu ở thư viện (T-021) cũng lên đầu ở lần mở sau.
- Trả lời: Đồng ý với đề xuất của AI.
3. **Ai ghi `pose_selected` với `source` `detail`?** Spec camera (mục 6) ghi sự kiện "khi mở camera từ chi tiết tư thế", spec poses (KB-3, mục 6) và ticket T-022 ghi khi bấm "Chụp với tư thế này". Hai bên cùng ghi thì đếm đôi. Đề xuất của AI: T-022 ghi lúc bấm nút; camera chỉ chọn tư thế và ghi "đã dùng".
- Trả lời: Đồng ý với đề xuất của AI.
4. **`reason` của `content_load_failed`.** Spec camera liệt kê `network`, `not_found`, `other`; spec poses có thêm `timeout`, và `:core:poses` có `LoadFailureReason.TIMEOUT` (chờ quá 15 giây). Đề xuất của AI: dùng đúng `analyticsValue` của `:core:poses` (có `timeout`) để hai màn cùng cách đếm, và bổ sung `timeout` vào spec camera. Nếu không, gộp `timeout` vào `network`.
- Trả lời: Đồng ý với đề xuất của AI.
5. **Chạm ô đang lỗi.** Spec ghi "chạm lại để thử tải". Đề xuất của AI: chạm ô lỗi vừa chọn tư thế vừa tải lại ảnh nhỏ (chọn không cần ảnh nhỏ). Nếu chỉ muốn tải lại mà không chọn thì người dùng phải chạm hai lần.
- Trả lời: Đồng ý với đề xuất của AI.
6. **Khi nào tải nét viền.** Spec mục 7 ghi nét viền tải "như trên" (khi ô hiện ở dải), nét viền tư thế đang chọn tải trước. Đề xuất của AI: chỉ tải nét viền khi chọn tư thế, vì bộ khởi đầu đã có trên máy và tải trước nét viền của mọi ô hiện ra tốn dữ liệu mà ít dùng. Đổi ý thì chỉ thêm một lần tải trong `onThumbShown`.
- Trả lời: Đồng ý với đề xuất của AI.
7. **Giao diện khi nét viền tải lỗi.** Spec mục 7 có "Chưa tải được nét viền" kèm "Thử lại" trong thẻ bước, nhưng thiết kế không có trạng thái này và bảng chữ không có hai chữ đó. Đề xuất của AI: thẻ bước giữ nền, viền, vị trí như thường, chữ "Chưa tải được nét viền" ở chỗ nội dung bước, nút chữ "Thử lại" thay hai mũi tên, không có nhãn số bước; người dùng không đọc được bước cho tới khi tải lại xong. Cần bổ sung vào thiết kế nếu muốn khác.
- Trả lời: Đồng ý với đề xuất của AI.
8. **Catalog tải lỗi trên camera.** Bình thường camera chỉ mở sau khi loading đã có catalog; nhưng khi hệ thống khôi phục app vào thẳng camera (loading đã rời ngăn điều hướng) thì catalog phải tải lại và có thể lỗi. Thiết kế không có trạng thái này. Đề xuất của AI: vùng chip và dải hiện "Chưa tải được danh sách tư thế." và "Thử lại"; camera vẫn chụp được, không có nét viền. Khi đang tải thì vùng này trống.
- Trả lời: Đồng ý với đề xuất của AI.
9. **Ô đang chọn khi camera chưa sẵn sàng, và nút đổi camera (việc còn mở từ T-013).** Ảnh `starting`, `permission-denied`, `error` không có ô nào được chọn, trong khi theo KB-1 luôn có tư thế được chọn. Đề xuất của AI: vẫn đánh dấu ô đang chọn (người dùng chọn được ở các trạng thái này, KB-7, nên cần thấy mình đã chọn gì); ảnh so sánh lệch một ô. Cùng ba ảnh này, nút đổi camera đang mờ theo plan T-013 còn thiết kế để trắng (báo cáo RUN-20261022-01). Việc này ngoài phạm vi ticket; nếu người duyệt chốt "trắng" trước khi duyệt plan này, T-014 đổi `enabled` của nút đổi camera trong `CaptureRow` (`CameraControls.kt`, đã có trong mục 3.2) thành luôn bật khi không đang chụp, và ghi lại ảnh chuẩn trong cùng lượt; `CameraScreenTest` không có test nào khẳng định nút này mờ. Không chốt thì giữ nguyên.
- Trả lời: Đồng ý với đề xuất của AI.
10. **Thẻ bước ở màn hình thấp.** Ở khung 360 x 640, khung xem trước chỉ rộng khoảng 223 dp; thẻ bước theo thiết kế còn khoảng 25 dp cho chữ. Đề xuất của AI: khung hẹp hơn 300 dp thì hai mũi tên xuống hàng dưới (chữ rộng khoảng 120 dp, thẻ cao hơn). Cách khác: thẻ bước rộng hết màn hình, đè ra ngoài khung xem trước (trái README mục 6).
- Trả lời: Đồng ý với đề xuất của AI.
11. **Mô tả cho trình đọc màn hình.** Bảng chữ không có mô tả cho mũi tên, thẻ bước, ô lỗi. Đề xuất của AI: "Bước trước", "Bước sau", hành động "Ẩn thẻ bước", "Hiện thẻ bước" cho nút "Bước", ô dùng tên tư thế, ô lỗi "<tên>, chưa tải được ảnh, chạm để thử lại".
- Trả lời: Đồng ý với đề xuất của AI.
12. **`selected` trên mọi `CameraIconButton`** (ghi chú rà soát T-013: TalkBack có thể đọc "chưa chọn" cho cả nút không phải công tắc). T-014 không thêm nút `CameraIconButton` nào. Đề xuất của AI: sửa ở T-015, vì T-015 thêm bốn nút vào cột công cụ (chỉnh, lật, khóa, hiện tư thế; ba nút có trạng thái đang bật), bằng một tham số cho biết nút có phải công tắc không, mặc định giữ hành vi cũ để không phải sửa test của T-013.
- Trả lời: Đồng ý với đề xuất của AI.
13. **Chọn tự động có tính là "đã dùng" không?** Đề xuất của AI: không. Tư thế tự chọn lúc mở không ghi `markUsed` và không ghi `pose_selected`; chỉ chọn của người dùng (ở dải, từ chi tiết, tư thế tiếp) mới tính.
- Trả lời: Đồng ý với đề xuất của AI.
14. **`onNextPose()` làm sẵn ở T-014.** Đề xuất của AI: T-014 làm hàm và test (cùng sự kiện `pose_selected` với `source` `next`), T-016 chỉ nối nút "Tư thế tiếp" và đường quay về camera (`NextPoseRequests` của plan T-016, gọi `onNextPose()` từ `CameraRoute`); phương án A của plan T-016 cần sửa theo, vì nó đang giả định dải nằm trong `CameraViewModel`. Nếu muốn ranh giới ticket chặt hơn thì để toàn bộ cho T-016 (T-016 khi đó phải sửa `PoseViewModel`).
- Trả lời: Đồng ý với đề xuất của AI.
15. **Lỗi ghi "đã dùng" và lỗi lưu ẩn thẻ bước không báo người dùng.** `CLAUDE.md` (bước 4) muốn lỗi ghi đưa vào `UiState` để báo; thiết kế không có chữ cho hai lỗi này. Đề xuất của AI: như T-013 câu 4, chỉ ghi log; lựa chọn vẫn có hiệu lực trong lần dùng này.
- Trả lời: Đồng ý với đề xuất của AI.
16. **Bộ nhớ ảnh nhỏ.** Ảnh nhỏ đã giải mã giữ trong ViewModel (khoảng 260 KB mỗi ô sau khi thu nhỏ). Với catalog mẫu 18 tư thế là khoảng 5 MB. Đề xuất của AI: chưa giới hạn ở T-014; khi catalog thật vượt khoảng 100 tư thế thì thêm bộ đệm có giới hạn (ticket riêng).
- Trả lời: Đồng ý với đề xuất của AI.
17. **Cỡ task.** Khoảng 55 file, như T-013 (T-013 dùng 60.4M token, 3 vòng CI). Người duyệt T-013 chọn giữ một plan. Nếu muốn tách: T-014a gồm bốn thành phần core, `PoseLibrary.from`, `PoseViewModel` phần chip, dải, chọn tư thế, ảnh nhỏ, `pose_selected`, `content_load_failed` (thumbnail), mở từ chi tiết; T-014b gồm nét viền, thẻ bước, ẩn hiện và khóa `camera_steps_hidden`, trạng thái không có tư thế, nét viền lỗi, ba ảnh thiết kế mới. Đề xuất của AI: giữ một plan, vì hai phần dùng chung `PoseViewModel` và cùng ghi lại bảy ảnh chuẩn.
- Trả lời: Đồng ý với đề xuất của AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** 

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-YYYYMMDD-NN, phiên bản plan 1

- **Kết quả:** đã merge / dừng / CI thất bại
- **Nhánh, PR:** <...>
- **Đã làm:** <...>
- **Test viết trước:** <số test, xác nhận đã thất bại đúng lý do trước khi viết code>
- **Rà soát độc lập:** <số điểm phát hiện, đã xử lý những gì, điểm nào còn để người xem>
- **Kết quả CI:** <build, lint, unit test, UI test, screenshot test>
- **Lệch so với plan:** <chỗ nào và vì sao, hoặc "Không có">
- **Việc còn lại:** <...>
- **Cần người xem:** <điểm dev nên kiểm tra sáng nay>
