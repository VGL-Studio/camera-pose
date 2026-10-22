---
ticket: T-016
title: Màn xem lại, lưu, chia sẻ, tư thế tiếp
status: approved               # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 25M          # số gợi ý cỡ L là 9.5M; chỉnh lên, lý do ở mục 5
blocking_points: [analytics] # thêm permission nếu người duyệt chọn phương án A ở mục 8 câu 1
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                   # người duyệt
cross_reviewer: Tuấn              # bắt buộc khi risk_level là high
approved_at: 2026-10-22
---

# PLAN T-016: MÀN XEM LẠI, LƯU, CHIA SẺ, TƯ THẾ TIẾP

## 1. Tóm tắt

Thêm màn xem lại vào `:feature:camera`: chụp xong mở màn xem lại với ảnh vừa chụp; "Lưu vào thư viện" chép ảnh vào thư mục ảnh riêng của app qua MediaStore, ghi `photo_saved`, đếm số ảnh đã lưu; "Chia sẻ" mở hộp chia sẻ của hệ thống; "Chụp lại", "Tư thế tiếp", nút đóng và nút quay lại về camera, hỏi "Bỏ ảnh này?" khi chưa lưu; lưu lỗi vì hết bộ nhớ hiện thông báo kèm "Thử lại"; ô ảnh vừa chụp ở camera hiện và mở ảnh đã lưu gần nhất. MediaStore, giải mã ảnh và chia sẻ nằm sau interface, nên ViewModel và màn hình được test trên JVM bằng bản giả; phần MediaStore và FileProvider thật được test trên emulator của CI. Kiểm chứng bằng unit test ViewModel và repository, test giao diện Robolectric, bốn screenshot test cùng tên ảnh thiết kế và một ảnh khung thấp, test emulator lưu ảnh thật, và kiểm tra thủ công trên máy thật.

**Plan chưa duyệt được trước khi người duyệt trả lời mục 8 câu 1** (lưu ảnh trên Android 8 và 9 cần quyền ghi bộ nhớ mà spec không nêu).

## 2. Hiểu yêu cầu

Ticket xử lý KB-6 của `features/camera/spec.md`, phần sau khi chụp (phần chụp và đếm ngược đã xong ở T-013):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-6 | Khi bấm nút chụp mà hẹn giờ tắt thì app chụp ngay, ghi `photo_captured`, mở màn xem lại với ảnh vừa chụp, không có nét viền | Phần chụp và sự kiện đã có (T-013). T-016 nối `onPhotoCaptured` sang đích `ReviewDestination()`. Ảnh là file JPEG từ camera nên không bao giờ có nét viền |
| KB-6 | Khi bấm "Lưu vào thư viện" thì nút hiện "Đang lưu…", các nút khác mờ đi; lưu xong ảnh có trong thư viện máy, ở thư mục ảnh riêng của app; app ghi `photo_saved` | Thư mục `Pictures/Camera Pose/` (tên ở mục 8 câu 4). Trong lúc lưu: nút chính hiện vòng chờ và "Đang lưu…", ba nút phụ và nút đóng mờ, không bấm được; nút quay lại của hệ thống bị bỏ qua. Lưu xong: `photo_saved` với `pose_id`, số ảnh đã lưu tăng 1, nhớ địa chỉ ảnh đã lưu gần nhất |
| KB-6 | Khi bấm "Chụp lại" sau khi đã lưu ảnh thì app quay về camera | Về camera ngay, không hỏi |
| KB-6 | Khi rời màn xem lại mà chưa lưu (Chụp lại, Tư thế tiếp, nút quay lại) thì hộp thoại "Bỏ ảnh này?" hiện; "Bỏ ảnh" làm tiếp việc vừa chọn, ảnh không được lưu; "Lưu ảnh" lưu rồi làm tiếp; chạm ra ngoài hay bấm quay lại thì đóng hộp thoại, vẫn ở màn xem lại | Nút đóng (`close`) ở thanh trên cũng là một cách rời, nên cũng hỏi. ViewModel nhớ việc đang chờ (`ExitAction`). "Lưu ảnh" mà lưu lỗi: hộp thoại đóng, thông báo lỗi hiện, người dùng ở lại màn xem lại (việc đang chờ bị bỏ). "Bỏ ảnh" xóa file tạm ngay |
| KB-6 | Khi bấm "Chia sẻ" thì hộp chia sẻ của hệ thống hiện với ảnh vừa chụp | Chưa lưu: chia sẻ file tạm qua `FileProvider` của module. Đã lưu: chia sẻ địa chỉ MediaStore. `Intent.ACTION_SEND`, `image/jpeg`, `FLAG_GRANT_READ_URI_PERMISSION`, bọc trong `Intent.createChooser`. Chia sẻ không tính là đã lưu |
| KB-6 | Khi bấm "Tư thế tiếp" thì app quay về camera với tư thế kế tiếp trong dải đang chọn (hết dải thì về tư thế đầu) | Màn xem lại đặt một yêu cầu "tư thế tiếp" (`NextPoseRequests`) rồi rời; khi màn camera bắt đầu lại, `CameraRoute` lấy yêu cầu và gọi `PoseViewModel.onNextPose()` mà T-014 làm sẵn (kế tiếp trong dải, hết dải về đầu, ghi `pose_selected` với `source` `next`). Cách nối phụ thuộc T-014 đã merge hay chưa (mục 3.1, phương án A và B) |
| KB-6 | Khi quay về camera từ màn xem lại thì tư thế, chỉnh nét viền, công cụ chụp giữ như trước khi chụp | Màn xem lại là đích riêng đặt trên camera, camera vẫn nằm trong back stack nên `CameraViewModel` giữ nguyên. Khi màn camera dừng thì đóng camera, bắt đầu lại thì mở lại với cùng công cụ (T-013, test `stoppedThenStarted_closesThenReopensWithSameTools`) |
| KB-6 | Khi chạm ô ảnh vừa chụp ở camera thì màn xem lại mở với ảnh đã lưu gần nhất; chưa có ảnh nào thì ô trống và không làm gì | Ô hiện ảnh thu nhỏ của ảnh đã lưu gần nhất. Ảnh đó đã bị xóa khỏi thư viện máy thì coi như chưa có ảnh. Màn xem lại mở từ ô này ở trạng thái đã lưu: không hỏi khi rời, không ghi `photo_saved` lần nữa |
| KB-6, đặc biệt | Khi lưu lỗi vì hết bộ nhớ thì hiện "Không lưu được ảnh: bộ nhớ máy đã đầy." kèm "Thử lại"; ảnh vẫn còn trên màn xem lại | Thông báo nằm cố định trên nút chính cho tới khi bấm "Thử lại" hoặc rời màn, không tự ẩn. Lỗi khác với hết bộ nhớ: mục 8 câu 3 |
| KB-6, đặc biệt | Khi lưu ảnh thì app không xin quyền đọc ảnh; manifest không có quyền đọc ảnh | Android 10 trở lên: MediaStore không cần quyền. Android 8 và 9: mục 8 câu 1 |
| Phạm vi ticket | Đếm số ảnh đã lưu (cho T-018, T-019) | Khóa DataStore mới `camera_saved_photo_count`, tính từ khi cài, lộ ra `SavedPhotosRepository.savedCount`. Mốc quảng cáo của phiên là việc của T-018 |
| Phạm vi ticket | Ảnh không kèm nét viền | Ảnh lưu đúng byte của file JPEG camera ghi ra, kể cả thẻ EXIF chiều ảnh |

Ghi chú từ nhật ký T-013 và báo cáo RUN-20261022-01, mục Cần người xem:

- **`events` thu không theo vòng đời.** `ReviewRoute` thu sự kiện trong `repeatOnLifecycle(Lifecycle.State.STARTED)`. `CameraRoute` đổi sang cách này luôn, vì T-016 nối sự kiện `PhotoCaptured` sang điều hướng: điều hướng khi màn đã dừng (ví dụ ảnh chụp xong lúc người dùng vừa bấm nút Home) sẽ chờ tới khi màn quay lại. `Channel(BUFFERED)` giữ sự kiện trong lúc chờ.
- **Chụp hỏng vẫn xóa ảnh cũ trong cache.** `CaptureFileStore.newFile` xóa mọi file cũ trước khi chụp, kể cả khi lần chụp đó hỏng. Với T-016 điều này không làm mất ảnh người dùng còn cần: màn xem lại chỉ mở sau một lần chụp thành công, với chính file mới; khi rời màn xem lại (đã lưu hay bỏ), file tạm bị xóa ngay; ảnh đã lưu nằm trong thư viện máy, không ở cache. Trường hợp duy nhất còn lại là file biến mất trong lúc màn xem lại đang mở (hệ thống dọn cache): ViewModel kiểm tra file khi mở và khi lưu, file không còn thì về camera (test `photoFileMissing_leavesAtOnce`, `save_fileVanished_showsError`). T-016 không đổi `newFile`.

Thiết kế tham chiếu: `features/camera/design/README.md`; số liệu trong `design/context/ReviewScreen_*.md`; ảnh trong `design/screens/`. Phân chia ảnh thiết kế:

| Ảnh | Ticket | Lý do |
| --- | --- | --- |
| `ReviewScreen_default_dark`, `ReviewScreen_saving_dark`, `ReviewScreen_save-error_dark`, `ReviewScreen_discard_dark` | T-016 | Màn xem lại |
| `ReviewScreen_default_dark_unfolded` | T-020 | Máy gập (ticket T-020) |
| `CameraScreen_*` | T-013, T-014, T-015, T-017, T-020 | Màn camera. T-016 chỉ thêm ảnh thu nhỏ vào ô ảnh vừa chụp; ảnh chuẩn của camera không đổi vì test truyền ô trống |

Không thuộc T-016: quảng cáo khi rời màn xem lại (T-018), hỏi đánh giá (T-019), bố cục máy gập (T-020). T-016 chừa sẵn chỗ cho T-018 và T-019: mọi cách rời màn đi qua một hàm `leave()` duy nhất, phát `ReviewEvent.Leave(savedThisVisit)`.

ADR liên quan: ADR-0001 (màn xem lại trong `:feature:camera`, không phụ thuộc feature khác), ADR-0008 (màu, chữ, thành phần từ `:core:designsystem`), ADR-0012 (sự kiện qua `:core:analytics`), ADR-0013 (không có quảng cáo ở màn này), ADR-0019 (log qua `AppLogger`, không ghi địa chỉ ảnh hay tên file), ADR-0020 (tên screenshot test, ảnh so sánh).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu `:feature:sample` và chính `:feature:camera` của T-013: interface trong feature, bản cài đặt Android mỏng, `UiState` sealed, ViewModel với `StateFlow` và `Channel` cho sự kiện, `Route` nối ViewModel và `Screen` không trạng thái, đích `@Serializable`. Gói gốc `com.vglstudio.camera.pose.line.guide.feature.camera` (viết tắt `…camera` bên dưới). Thẻ log: `Review` cho màn xem lại, `Camera` cho phần trên camera.

**Điều kiện đầu vào.** T-013 đã merge (đúng, commit 0c2243e). T-014 có thể đã merge hoặc chưa (phương án A, B ở dưới).

**Lưu ảnh (`photo/`).**

```kotlin
/** The device gallery. Saved photos are addressed by their content uri, kept as a string. */
interface PhotoStore {
    /** Copies [file] into the app's own folder of the gallery. Throws [StorageFullException] or [IOException]. */
    suspend fun save(file: File, takenAtMillis: Long): String
    /** False when the photo was deleted from the gallery, or cannot be read. */
    suspend fun exists(uri: String): Boolean
}
class StorageFullException(cause: Throwable? = null) : IOException(cause)
/** True when [error] or one of its causes says the device storage is full (ENOSPC). */
fun isStorageFull(error: Throwable): Boolean
```

- `MediaStorePhotoStore` (`@Singleton`, nhận `@ApplicationContext`): chạy trên `Dispatchers.IO`. Trước khi ghi: `StatFs(Environment.getExternalStorageDirectory().path).availableBytes < file.length() + FREE_SPACE_MARGIN_BYTES` (5 MB) thì ném `StorageFullException`. Android 10 trở lên: `insert` vào `MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)` với `DISPLAY_NAME = "CameraPose_<yyyyMMdd_HHmmss>.jpg"` (giờ máy lúc chụp, `Locale.US`), `MIME_TYPE = "image/jpeg"`, `RELATIVE_PATH = "Pictures/<camera_album_name>"`, `DATE_TAKEN`, `IS_PENDING = 1`; chép byte bằng `openOutputStream`; `IS_PENDING = 0`. Lỗi giữa chừng thì `delete` dòng vừa thêm rồi ném lại; `IOException` mà `isStorageFull` thì bọc thành `StorageFullException`. Android 8 và 9: theo câu trả lời mục 8 câu 1.
- `exists(uri)`: `contentResolver.query(uri, arrayOf(_ID), …)` có một dòng; mọi `SecurityException`, `IllegalArgumentException` coi là không còn.
- Lớp này `public` (kèm chú thích "public for the device test in :app") để `app/src/androidTest` gọi được; mọi lớp khác của T-016 giữ `internal`.

**Giải mã ảnh (`photo/PhotoDecoder.kt`).** `interface PhotoDecoder { suspend fun decode(location: String, maxSidePx: Int): ImageBitmap? }`. `location` là đường dẫn file tạm hoặc địa chỉ `content://`. `AndroidPhotoDecoder`: `Dispatchers.IO`, `BitmapFactory` đọc cỡ trước (`inJustDecodeBounds`), chọn `inSampleSize` là lũy thừa của 2 sao cho cạnh dài không quá `maxSidePx`, giải mã, xoay và lật theo `android.media.ExifInterface` (thư viện hệ thống, có từ API 24, không thêm thư viện), trả `asImageBitmap()`. Lỗi thì trả `null` và màn hình hiện khung trống. Giải mã chạy trong `Route` bằng `produceState`, nên ViewModel không giữ `Bitmap` và test được trên JVM thuần. Ảnh xem lại cỡ tối đa 1600 px, ảnh thu nhỏ của ô 192 px.

**Dữ liệu (`data/`).** `SavedPhotosRepository`: `val lastSavedPhoto: Flow<String?>`, `val savedCount: Flow<Int>`, `suspend fun recordSaved(uri: String)` (ném `IOException`). `DataStoreSavedPhotosRepository` dùng `DataStore<Preferences>` chung của `:core:data`, hai khóa mới `camera_last_saved_photo` (chuỗi địa chỉ) và `camera_saved_photo_count` (số nguyên, thiếu là 0), ghi trong một lần `edit`; đọc lỗi `IOException` thì thử lại như `DataStoreCameraSettingsRepository`. Khóa mới, không chuyển dữ liệu cũ.

**Bàn giao từ camera.** `CapturedPhoto` cần trường `poseId: String = ""` (trường cuối, giá trị mặc định giữ nguyên lời gọi của T-013). Plan T-014 thêm đúng trường này và điền từ `ActivePoseHolder` lúc chụp. Quy tắc chung của hai plan: task nào chạy trước thì thêm trường, task sau thấy đã có thì dùng lại, không thêm lần hai. T-016 chạy trước T-014 thì `pose_id` của `photo_saved` là chuỗi rỗng cho tới khi T-014 điền. `CaptureFileStore` thêm `fun delete(file: File)`: xóa file nếu nằm trong thư mục tạm, bỏ qua file ngoài thư mục.

**Tư thế tiếp (`review/NextPoseRequests.kt`).** `@Singleton class NextPoseRequests @Inject constructor() { fun request(); fun consume(): Boolean }` (cờ `AtomicBoolean`; `consume` trả cờ rồi đặt lại `false`).

`LastPhotoViewModel` (bên dưới) nhận `NextPoseRequests` và có `fun consumeNextPoseRequest(): Boolean = nextPoseRequests.consume()`, để `CameraRoute` không phải tự tiêm singleton và phần quyết định có unit test. T-016 không sửa `CameraViewModel` và `PoseViewModel` ở cả hai phương án.

- **Phương án A**, khi lúc bắt đầu task `pose/PoseViewModel.kt` của T-014 có trên nhánh chính với `fun onNextPose()` (plan T-014, mục 3.1): trong `LifecycleStartEffect` đã có của `CameraRoute`, sau `lastPhotoViewModel.onScreenStarted()`, gọi `if (lastPhotoViewModel.consumeNextPoseRequest()) poseViewModel.onNextPose()`, dùng chính `poseViewModel` mà T-014 đã thêm vào `CameraRoute`. `onNextPose()` lo phần còn lại: kế tiếp trong dải đang hiện, hết dải về đầu, ghi `pose_selected` (`source` `next`) và "đã dùng". Dải của T-014 giữ thứ tự cố định suốt đời `PoseViewModel` (plan T-014, mục 8 câu 2), và `PoseViewModel` vẫn sống trong back stack lúc ở màn xem lại, nên "kế tiếp" đúng là ô bên cạnh ô người dùng thấy lúc chụp.
- **Phương án B**, ngược lại (T-014 chưa merge): `CameraRoute` vẫn gọi `consumeNextPoseRequest()` để yêu cầu không tồn đọng, nhưng không có gì để gọi tiếp; "Tư thế tiếp" chỉ về camera. Ghi "nối `onNextPose()` vào `CameraRoute` khi T-014 merge" vào mục Việc còn lại của nhật ký. Tiêu chí "Tư thế tiếp" của KB-6 khi đó chỉ đạt phần "quay về camera".

Hai phương án chỉ khác một dòng trong `CameraScreen.kt`, file đã có trong mục 3.2.

**Trạng thái màn xem lại (`review/ReviewUiState.kt`).**

```kotlin
sealed interface ReviewUiState {
    /** Reading the photo to show. */
    data object Loading : ReviewUiState
    data class Ready(
        /** File path of the photo just taken, or content uri of a saved photo. */
        val location: String,
        val save: SaveStatus,
        /** Non-null while "Bỏ ảnh này?" is open: what to do after the answer. */
        val pendingExit: ExitAction? = null,
    ) : ReviewUiState
}
enum class SaveStatus { NOT_SAVED, SAVING, SAVED, FAILED_STORAGE_FULL, FAILED }
enum class ExitAction { RETAKE, NEXT_POSE, CLOSE }
sealed interface ReviewEvent {
    data class Leave(val savedThisVisit: Boolean) : ReviewEvent
    data class Share(val location: String) : ReviewEvent
}
```

**`ReviewViewModel`** (`@HiltViewModel`) nhận `CapturedPhotoHolder`, `CaptureFileStore`, `PhotoStore`, `SavedPhotosRepository`, `NextPoseRequests`, `Analytics`, `AppLogger`, `SavedStateHandle`. Đích có tham số `savedPhotoUri: String?` (đọc từ `SavedStateHandle`).

- `init`, mở từ ảnh vừa chụp (`savedPhotoUri == null`): lấy `holder.latest.value`; nếu `null` (tiến trình bị đóng rồi mở lại) thì lấy đường dẫn, `poseId`, giờ chụp đã chép vào `SavedStateHandle` ở lần trước. File không tồn tại hoặc không có gì: log `warn` "Review photo missing", phát `Leave(false)`. Có file: chép vào `SavedStateHandle`, `Ready(path, NOT_SAVED)`, log "Review opened: captured".
- `init`, mở từ ô ảnh vừa chụp: `photoStore.exists(uri)` thì `Ready(uri, SAVED)`, log "Review opened: last saved"; không thì `Leave(false)`.
- `onSaveClick()`, `onRetrySave()`: chỉ khi `Ready` và `save` là `NOT_SAVED`, `FAILED_STORAGE_FULL` hoặc `FAILED`. `SAVING`; file không còn thì `FAILED`; `photoStore.save(file, capturedAtMillis)`. Thành công: `SAVED`, chép địa chỉ mới vào `SavedStateHandle`, ghi `photo_saved` (`pose_id`), log "Photo saved", rồi `recordSaved(uri)` trong `try`; `IOException` ở bước này chỉ log `warn` (ảnh đã nằm trong thư viện). `StorageFullException` (hoặc `isStorageFull`): `FAILED_STORAGE_FULL`, log `warn` với lỗi. `IOException` khác: `FAILED`, log `warn`. Có `pendingExit` đang chờ (đi từ "Lưu ảnh" của hộp thoại): lưu xong thì `leave(pendingExit)`; lỗi thì bỏ `pendingExit`.
- `onRetakeClick()`, `onNextPoseClick()`, `onCloseClick()` (nút đóng và nút quay lại của hệ thống): bỏ qua khi `SAVING`. `SAVED` thì `leave(action)`. Còn lại thì mở hộp thoại (`pendingExit = action`), log "Discard asked".
- `onDiscardConfirm()` ("Bỏ ảnh"): log "Photo discarded", `leave(pendingExit)`. `onDiscardSave()` ("Lưu ảnh"): đóng hộp thoại nhưng nhớ việc chờ, gọi lưu như trên. `onDismissDialog()` (chạm ra ngoài, nút quay lại khi hộp thoại mở): `pendingExit = null`.
- `onShareClick()`: bỏ qua khi `SAVING`; phát `Share(location)` (đường dẫn file tạm, hoặc địa chỉ đã lưu nếu `SAVED`); log "Share opened".
- `leave(action)`, nơi duy nhất rời màn: nếu ảnh là file tạm thì `fileStore.delete(file)` và `holder.clear()`; `NEXT_POSE` thì `nextPoseRequests.request()`; log "Review left: <retake|next_pose|close>"; phát `Leave(savedThisVisit)` (`true` khi lần mở này đã lưu thành công). T-018 và T-019 cắm vào đây.

**Màn hình (`review/ReviewScreen.kt`, `review/ReviewComponents.kt`).** `ReviewRoute(onLeave: () -> Unit, viewModel = hiltViewModel())`:

- Bọc trong `AppTheme(darkTheme = true)` như camera.
- Ảnh: `produceState<ImageBitmap?>(null, location) { value = decoder.decode(location, 1600) }`, `decoder` tiêm qua `ReviewViewModel` (thuộc tính `internal val decoder: PhotoDecoder`).
- Sự kiện: `LaunchedEffect(lifecycleOwner) { lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.events.collect { … } } }`. `Leave` gọi `onLeave`. `Share` dựng địa chỉ: file tạm qua `FileProvider.getUriForFile(context, "${context.packageName}.captures", File(location))`, địa chỉ đã lưu dùng thẳng; rồi `context.startActivity(shareChooser(uri, title))`. `ActivityNotFoundException` thì log `warn`.
- `BackHandler(enabled = true)`: hộp thoại mở thì `onDismissDialog`, không thì `onCloseClick` (ViewModel tự bỏ qua khi đang lưu).

`review/ShareIntents.kt`: `internal fun shareChooser(uri: Uri, title: String): Intent`: `ACTION_SEND`, `type = "image/jpeg"`, `EXTRA_STREAM`, `clipData = ClipData.newRawUri("", uri)`, `FLAG_GRANT_READ_URI_PERMISSION`, bọc `Intent.createChooser(send, title)`. Tiêu đề hộp chọn "Chia sẻ ảnh" (mục 8 câu 7).

`ReviewScreen(uiState, image: ImageBitmap?, onCloseClick, onSaveClick, onRetrySave, onRetakeClick, onShareClick, onNextPoseClick, onDiscardConfirm, onDiscardSave, onDismissDialog, modifier)` không trạng thái. `Loading` chỉ vẽ nền và thanh trên. Bố cục (số liệu `context/ReviewScreen_*.md`, khung 411 x 891; tính từ mép trên khung vì test không có thanh trạng thái, trên máy thật `safeDrawingPadding` của `AppNavHost` đẩy xuống dưới thanh trạng thái; thiết kế có 24 dp thanh trạng thái nên mọi thứ trong ảnh chuẩn cao hơn thiết kế 24 dp):

| Phần | Thành phần, màu | Kích thước, khoảng cách |
| --- | --- | --- |
| Nền | `appColors.cameraBackground` (#0A0A0B) | Cả màn, `testTag(SCREEN)` |
| Thanh trên | `Row`: `IconButton` 48 với `ic_camera_close` màu `onSurface`, mô tả "Đóng"; chữ "Xem lại" `titleLarge` màu `onSurface` | Cao 64, lề ngang `Spacing.xs`, cách nhau `Spacing.xs`. Không dùng `TopAppBar` của Material vì nó tự thêm khoảng của thanh trạng thái |
| Vùng giữa | `Column` trong `weight(1f)`, căn trên: ảnh, khoảng `Spacing.lg`, hàng nút phụ. Phần dư nằm dưới hàng nút phụ | |
| Ảnh | `Box` `aspectRatio(3f / 4f, matchHeightConstraintsFirst = true)` trong `weight(1f, fill = false)`, nền `appColors.cameraSurface` (#1E1E1E), viền `Stroke.sticker` màu `outline`, bo `Radius.card`; `Image(image, ContentScale.Crop)` khi có ảnh, `testTag(PHOTO)` | Rộng tối đa khung trừ 2 x `Spacing.md` (379 x 505 ở khung 411 x 891, đúng thiết kế) |
| Hàng nút phụ | `Row` rộng 331 (`widthIn(max = 331.dp)`, căn giữa), `Arrangement.SpaceBetween`; mỗi nút là `Column` cách nhau `Spacing.xs`: `CameraIconButton` (icon `ic_review_replay`, `ic_review_share`, `ic_review_skip_next`) và chữ `labelLarge` màu `onSurface` | Cao 72. Đang lưu: cả ba `enabled = false` (độ mờ 38% của `CameraIconButton`), chữ cũng độ mờ 38% |
| Thông báo lưu lỗi | `Snackbar` của Material 3 khi `FAILED_STORAGE_FULL` hoặc `FAILED`: nền `inverseSurface`, chữ `inverseOnSurface` `bodyMedium`, nút "Thử lại" `labelLarge` màu `inversePrimary`, `testTag(SAVE_ERROR)` | Lề ngang `Spacing.md`, cách nút chính `Spacing.md` |
| Nút chính | `PrimaryButton(text, onSaveClick, icon = ic_review_download, loading = SAVING, enabled = save != SAVED)`, `testTag(SAVE)`. Chữ: "Lưu vào thư viện"; khi `SAVING` "Đang lưu…" | Rộng hết trừ 2 x `Spacing.md`; cách đáy `Spacing.md + Spacing.xs` (20, đúng thiết kế) |
| Hộp thoại "Bỏ ảnh này?" | Vẽ trong cây giao diện, trên cùng: lớp `appColors.cameraScrim` độ mờ 50%, bấm vào thì `onDismissDialog`; thẻ rộng tối đa 331 nền `surface`, viền `Stroke.sticker` màu `outline`, bo `Radius.sheet`, lề `Spacing.lg`, cách nhau `Spacing.md`: tiêu đề `titleLarge`, lời `bodyMedium` màu `onSurfaceVariant`, `Row` hai nút cách `Spacing.sm` mỗi nút `weight(1f)`: `SecondaryButton("Bỏ ảnh")`, `PrimaryButton("Lưu ảnh")`. Thẻ mang `semantics { paneTitle = "Bỏ ảnh này?" }`, `testTag(DISCARD_DIALOG)` | Căn giữa màn. Khi hộp thoại mở, phần nội dung phía sau mang `clearAndSetSemantics {}` để trình đọc màn hình chỉ thấy hộp thoại (L-004: `hideFromAccessibility` không lan xuống node con) |

Lý do vẽ hộp thoại trong cây thay vì `Dialog`: `captureRoboImage` chỉ chụp cửa sổ của màn, không chụp cửa sổ `Dialog`; thiết kế `discard` cần lớp mờ và hộp trong cùng một ảnh. Cách này giống `NetworkDialog` của T-011 ở phần thẻ.

Chữ ở màn hình thấp 360 x 640: ảnh co theo chiều cao còn lại (giữ 3:4), hàng nút phụ, thông báo và nút chính giữ cỡ (README thiết kế, mục 6). Ở trạng thái lưu lỗi, ảnh còn khoảng 246 x 328.

**Ô ảnh vừa chụp trên camera (`lastphoto/LastPhotoViewModel.kt`).** ViewModel riêng để không đụng vào `CameraViewModel` mà T-014 cũng sửa. `@HiltViewModel class LastPhotoViewModel(repository: SavedPhotosRepository, photoStore: PhotoStore, nextPoseRequests: NextPoseRequests, logger)`: `val lastPhoto: StateFlow<String?>`; `fun consumeNextPoseRequest(): Boolean` (log "Next pose requested" khi `true`); `fun onScreenStarted()` đọc `lastSavedPhoto.first()`, kiểm `photoStore.exists`, đặt `lastPhoto` (không còn thì `null`, log "Last photo missing"). Sau khi lưu ở màn xem lại, camera bắt đầu lại nên ô được cập nhật. `internal val decoder: PhotoDecoder` cho ảnh thu nhỏ.

- `CameraRoute` nhận thêm `onOpenLastPhoto: (String) -> Unit` và `lastPhotoViewModel: LastPhotoViewModel = hiltViewModel()`; trong `LifecycleStartEffect` đã có, gọi thêm `lastPhotoViewModel.onScreenStarted()` rồi phần "tư thế tiếp" (phương án A hoặc B ở trên); ảnh thu nhỏ bằng `produceState` với `decoder.decode(uri, 192)`; sự kiện của `CameraViewModel` thu trong `repeatOnLifecycle(STARTED)` (mục 2, ghi chú 1).
- `CameraScreen` thêm hai tham số có giá trị mặc định ở cuối: `lastPhoto: ImageBitmap? = null`, `onLastPhotoClick: (() -> Unit)? = null`. Mặc định giữ đúng hành vi T-013, nên `CameraScreenTest`, `CameraScreenScreenshotTest` và 7 ảnh chuẩn của camera không đổi.
- `CaptureRow` và `LastPhotoSlot` (`CameraControls.kt`) nhận hai giá trị này: có ảnh thì vẽ `Image(ContentScale.Crop)` cắt theo bo `Radius.thumb` trong ô 48; `onLastPhotoClick != null` thì `clickable(role = Role.Button)` với mô tả "Ảnh vừa chụp" (đã có); `null` thì như cũ, không bấm được. Ảnh lỗi giải mã mà có địa chỉ thì ô trống nhưng vẫn bấm được.

**Chia sẻ file tạm.** `feature/camera/src/main/AndroidManifest.xml` mới, chỉ có `<provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.captures" android:exported="false" android:grantUriPermissions="true">` với `meta-data` trỏ `@xml/camera_capture_paths` (`<cache-path name="captures" path="captures/" />`). Không có `<uses-permission>`. `FileProvider` thuộc `androidx.core`, đã có trên classpath của module (`CameraScreen.kt` dùng `ContextCompat`), không thêm thư viện.

**Điều hướng.**

- `navigation/ReviewNavigation.kt`: `@Serializable data class ReviewDestination(val savedPhotoUri: String? = null)`; `fun NavGraphBuilder.reviewScreen(onLeave: () -> Unit)`.
- `navigation/CameraNavigation.kt`: `cameraScreen` thêm `onOpenLastPhoto: (String) -> Unit = {}`, truyền xuống `CameraRoute`.
- `AppNavHost.kt`: `cameraScreen(onPhotoCaptured = { navController.navigate(ReviewDestination()) { launchSingleTop = true } }, onOpenLastPhoto = { navController.navigate(ReviewDestination(savedPhotoUri = it)) { launchSingleTop = true } })` (giữ nguyên các tham số khác mà T-021, T-025 có thể đã nối); `reviewScreen(onLeave = { navController.popBackStack() })`. Bỏ chú thích "T-016 opens the review screen".

**Biểu tượng.** 4 vector drawable mới trong `feature/camera/src/main/res/drawable/`, lấy từ Material Symbols Outlined như T-013 (cùng nguồn, cùng cách chú thích tên, nguồn và commit SHA): `ic_review_replay.xml` (`replay`), `ic_review_share.xml` (`share`), `ic_review_skip_next.xml` (`skip_next`), `ic_review_download.xml` (`download`). Nút đóng dùng `ic_camera_close.xml` đã có. Giấy phép đã có ở `feature/camera/third_party/material-symbols/LICENSE.txt`, không đổi.

**Chữ.** File riêng `feature/camera/src/main/res/values/strings_review.xml` (không sửa `strings.xml` mà T-014 cũng thêm chữ): theo bảng chữ của README mục 2: "Xem lại", "Chụp lại", "Chia sẻ", "Tư thế tiếp", "Lưu vào thư viện", "Đang lưu…", "Không lưu được ảnh: bộ nhớ máy đã đầy.", "Thử lại", "Bỏ ảnh này?", "Ảnh chưa được lưu vào thư viện.", "Bỏ ảnh", "Lưu ảnh"; thêm theo mục 8: "Không lưu được ảnh." (câu 3), "Camera Pose" làm tên thư mục ảnh, `translatable="false"` (câu 4), "Đóng" (mô tả nút đóng), "Chia sẻ ảnh" (tiêu đề hộp chọn, câu 7).

**Thứ tự làm.** Bước 3: khung rỗng (mục 4), mọi test, `strings_review.xml` đủ chữ; chạy để thấy thất bại đúng lý do; commit. Bước 4: `photo/`, repository, `CapturedPhoto.poseId` (nếu chưa có) và `CaptureFileStore.delete`, `NextPoseRequests`, `ReviewViewModel`, `LastPhotoViewModel`, màn hình và hộp thoại, biểu tượng, manifest và FileProvider, điều hướng, dòng gọi `onNextPose()` nếu phương án A; `scripts/check.sh` ghi 5 ảnh chuẩn mới; `scripts/design-compare.sh camera`; build `stagingDebug`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `feature/camera/src/main/AndroidManifest.xml` | `<provider>` FileProvider cho file tạm, không có quyền nào |
| Tạo | `feature/camera/src/main/res/xml/camera_capture_paths.xml` | `cache-path` `captures/` |
| Tạo | `feature/camera/src/main/res/values/strings_review.xml` | Chữ của màn xem lại |
| Tạo | `feature/camera/src/main/res/drawable/ic_review_replay.xml`, `ic_review_share.xml`, `ic_review_skip_next.xml`, `ic_review_download.xml` | 4 biểu tượng Material Symbols |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewUiState.kt` | `ReviewUiState`, `SaveStatus`, `ExitAction`, `ReviewEvent` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewViewModel.kt` | ViewModel theo mục 3.1 |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreen.kt` | `ReviewRoute`, `ReviewScreen`, `ReviewTestTags` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewComponents.kt` | Thanh trên, hàng nút phụ, thông báo lỗi, hộp thoại "Bỏ ảnh này?" |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ShareIntents.kt` | `shareChooser` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/NextPoseRequests.kt` | Yêu cầu "tư thế tiếp" từ màn xem lại tới camera |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/lastphoto/LastPhotoViewModel.kt` | Ảnh đã lưu gần nhất cho ô trên camera |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/photo/PhotoStore.kt` | Interface, `StorageFullException`, `isStorageFull` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/photo/MediaStorePhotoStore.kt` | Lưu vào thư viện máy qua MediaStore |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/photo/PhotoDecoder.kt` | Interface và `AndroidPhotoDecoder` (thu nhỏ, xoay theo EXIF) |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/data/SavedPhotosRepository.kt` | Interface repository |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/data/DataStoreSavedPhotosRepository.kt` | Hai khóa `camera_last_saved_photo`, `camera_saved_photo_count` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/di/ReviewModule.kt` | Gắn `PhotoStore`, `PhotoDecoder`, `SavedPhotosRepository` (file riêng, không sửa `CameraModule.kt`) |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/navigation/ReviewNavigation.kt` | `ReviewDestination`, `reviewScreen()` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/navigation/CameraNavigation.kt` | Tham số `onOpenLastPhoto` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | `CameraRoute`: `LastPhotoViewModel`, ảnh thu nhỏ, `onOpenLastPhoto`, lấy yêu cầu tư thế tiếp (phương án A: gọi `poseViewModel.onNextPose()`), thu sự kiện bằng `repeatOnLifecycle`. `CameraScreen`: hai tham số mới có mặc định |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraControls.kt` | `CaptureRow`, `LastPhotoSlot`: ảnh thu nhỏ và bấm được |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/capture/CapturedPhotoHolder.kt` | `CapturedPhoto.poseId: String = ""`, chỉ khi T-014 chưa thêm (T-014 thêm đúng trường này) |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CaptureFileStore.kt` | `fun delete(file: File)` |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewFakes.kt` | `FakePhotoStore`, `FakeSavedPhotosRepository`, `FakeReviewAnalytics` |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewViewModelTest.kt` | Unit test ViewModel (mục 4) |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenTest.kt` | Test giao diện Robolectric |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenScreenshotTest.kt` | 5 screenshot test |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ShareIntentsTest.kt` | Test intent chia sẻ (Robolectric) |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/NextPoseRequestsTest.kt` | Unit test |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/lastphoto/LastPhotoViewModelTest.kt` | Unit test |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/photo/PhotoStoreTest.kt` | Unit test `isStorageFull` |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/photo/AndroidPhotoDecoderTest.kt` | Robolectric, `GraphicsMode.NATIVE`: thu nhỏ và xoay theo EXIF |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/data/DataStoreSavedPhotosRepositoryTest.kt` | Unit test repository |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraLastPhotoTest.kt` | Test giao diện ô ảnh vừa chụp (file riêng, không sửa `CameraScreenTest.kt`) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CaptureFileStoreTest.kt` | Thêm hai test của `delete`; test cũ giữ nguyên |
| Tạo | `feature/camera/src/test/screenshots/ReviewScreen_default_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/ReviewScreen_saving_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/ReviewScreen_save-error_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/ReviewScreen_discard_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/ReviewScreen_save-error_small.png` | Khung 360 x 640, trạng thái nhiều nội dung nhất (L-028) |
| Sửa | `docs/features/camera/design/review/` | Sinh lại bởi `scripts/design-compare.sh camera` (thêm ảnh ghép của 4 ảnh T-016, `summary.md`), không sửa tay |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/AppNavHost.kt` | Nối camera sang màn xem lại, đăng ký `reviewScreen` |
| Tạo | `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/PhotoSaveDeviceTest.kt` | Test trên emulator: lưu thật qua MediaStore, FileProvider đọc được file tạm |
| Sửa | `app/src/main/AndroidManifest.xml` | Chỉ khi người duyệt chọn phương án A ở mục 8 câu 1: `WRITE_EXTERNAL_STORAGE` với `android:maxSdkVersion="28"` và chú thích |

Không file nào khác. Đặc biệt không sửa `gradle/libs.versions.toml` (không thêm thư viện: MediaStore, `ExifInterface`, `StatFs` thuộc hệ thống; `FileProvider` thuộc `androidx.core` đã có), `build-logic/`, mọi module `:core:*`, `feature/camera/build.gradle.kts`, `strings.xml`, `CameraModule.kt`, `CameraUiState.kt`, `CameraViewModel.kt`, `pose/PoseViewModel.kt` và mọi file `pose/` của T-014, `CameraScreenTest.kt`, `CameraScreenScreenshotTest.kt`, `CameraViewModelTest.kt`, `CameraFakes.kt`, ảnh chuẩn của camera.

**Nơi khác đọc các file trên** (L-020), không cần sửa:

- `CameraScreen.kt`, `CameraControls.kt`: `CameraScreenTest.kt` và `CameraScreenScreenshotTest.kt` gọi `CameraScreen` không truyền hai tham số mới; mặc định giữ hành vi cũ (ô trống, không bấm được), nên test `lastPhoto_isEmptyAndNotClickable` và 7 ảnh chuẩn camera không đổi.
- `CapturedPhotoHolder.kt` (chỉ khi T-016 chạy trước T-014): `CameraViewModelTest.kt` dựng `CapturedPhoto(File("capture_1.jpg"), BACK, OFF, 1_000L)` và so bằng `==`; trường mới có mặc định `""`, `CameraViewModel` của T-013 không truyền `poseId`, nên phép so vẫn đúng. T-014 chạy trước thì T-016 không sửa file này.
- `CaptureFileStore.kt`: chỉ thêm hàm; `CaptureFileStoreTest.kt` (trong danh sách) và `CameraModule.kt` (dựng lớp) không đổi.
- `CameraNavigation.kt`: chỉ `AppNavHost.kt` (trong danh sách) gọi; tham số mới có mặc định.
- `AppNavHost.kt`: `AppLaunchTest.kt` chỉ chờ `camera_screen`, không đổi; `MainScreenTest.kt` gọi `chooseMainScreen`, không đọc phần đăng ký đích.
- `app/src/main/AndroidManifest.xml` (chỉ phương án A): `scripts/app-explore/apk-screens.py` đọc manifest của APK app mẫu, không phải của repo. `feature/camera/src/main/AndroidManifest.xml` là file mới, được AGP gộp vào manifest của app.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không có cờ mới. Màn xem lại chỉ mở từ camera, camera đã sau cờ `feature_camera_enabled` (T-013).
- Quảng cáo: Không chạm. Không có quảng cáo ở màn xem lại (spec mục 5); quảng cáo khi rời màn là T-018.
- Sự kiện phân tích (điểm chặn 4, đã có trong spec mục 6): `photo_saved` khi lưu thành công, tham số `pose_id` (lấy từ `CapturedPhoto.poseId`; chuỗi rỗng khi chưa có tư thế, như `photo_captured` ở T-013). Ở phương án A, `pose_selected` với `source = "next"` do `PoseViewModel.onNextPose()` của T-014 ghi (sự kiện, giá trị và test đều thuộc plan T-014); T-016 chỉ gọi hàm, không thêm chỗ ghi sự kiện nào khác. Không gửi địa chỉ ảnh hay tên file.
- Dữ liệu cục bộ: hai khóa DataStore mới `camera_last_saved_photo`, `camera_saved_photo_count` (spec mục 6, dòng "Số ảnh đã lưu…" và "Ảnh đã lưu"). Khóa mới, không chuyển dữ liệu cũ, nên không chạm điểm chặn 9. Ảnh lưu vào thư viện máy, không tải lên đâu; gỡ app không xóa ảnh.
- Quyền: Không thêm quyền ở phương án B của mục 8 câu 1. Phương án A thêm `WRITE_EXTERNAL_STORAGE` chỉ cho Android 9 trở xuống (điểm chặn 3).
- File R2: Không chạm.

## 4. Kiểm thử

Mọi test dưới đây vào commit test của bước 3, cùng khung rỗng để biên dịch được: `ReviewViewModel` có đủ hàm nhưng không làm gì, `uiState` luôn `Loading`, `events` rỗng; `LastPhotoViewModel.lastPhoto` luôn `null`; `DataStoreSavedPhotosRepository` trả `null` và `0`, không ghi; `isStorageFull` luôn `false`; `AndroidPhotoDecoder.decode` trả `null`; `shareChooser` trả `Intent()`; `NextPoseRequests.consume` luôn `false`; `CaptureFileStore.delete` không làm gì; `ReviewScreen` chỉ vẽ `Box` mang `testTag(SCREEN)`; `CameraScreen` đã có hai tham số mới nhưng chưa dùng; `MediaStorePhotoStore.save` ném `IOException`. `strings_review.xml` đủ chữ ngay ở bước 3. Lý do thất bại ở cột cuối là điều AI dự kiến; bước 3 chạy và ghi lý do thật vào nhật ký.

Bản giả: `FakePhotoStore` có `savedUri = "content://media/external/images/media/7"`, `saveError: IOException?`, `existing: MutableSet<String>`, ghi lại `saveCalls: List<File>`, `save` treo được bằng `CompletableDeferred` để thấy `SAVING`. `FakeSavedPhotosRepository` có `MutableStateFlow` cho hai giá trị, ghi lại `recorded`, `failWrites`. Analytics giả ghi danh sách `AnalyticsEvent`. `RecordingAppLogger` của `:core:logging`. `CapturedPhotoHolder` và `CaptureFileStore` dùng lớp thật với `TemporaryFolder` (file `capture_1.jpg` có thật trong thư mục tạm). `MainDispatcherRule` đã có. Kiểm tra lỗi so loại (`assertTrue(entry.error is IOException)`).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-6, mở màn xem lại với ảnh vừa chụp | Unit test | `ReviewViewModelTest.captured_opensReady_notSaved`: holder có `CapturedPhoto(file, BACK, OFF, 1_000L, poseId = "p1")`; `Ready(file.path, NOT_SAVED, null)`; log "Review opened: captured". Thất bại: khung rỗng ở `Loading` |
| Ảnh không còn thì về camera | Unit test | `photoFileMissing_leavesAtOnce`: holder trỏ file không tồn tại; `events` phát `Leave(false)`; log `WARN` "Review photo missing". `holderEmpty_noSavedState_leaves`: holder rỗng, `SavedStateHandle()` rỗng; `Leave(false)` |
| Giữ ảnh khi tiến trình bị đóng | Unit test | `processRecreated_restoresFromSavedState`: ViewModel đầu chép đường dẫn vào `SavedStateHandle`; dựng ViewModel thứ hai với cùng `SavedStateHandle` và holder rỗng; `Ready(file.path, NOT_SAVED)` |
| KB-6, lưu: đang lưu | Unit test | `save_showsSavingWhileWriting`: `save` treo; `onSaveClick()`; `save == SAVING`; `onRetakeClick()`, `onShareClick()`, `onCloseClick()` không đổi trạng thái, không phát sự kiện; `saveCalls.size == 1` sau khi bấm lưu lần hai |
| KB-6, lưu xong, `photo_saved`, đếm | Unit test | `save_success_logsEventAndRecords`: `onSaveClick()`; `SAVED`; `analytics.events == [AnalyticsEvent("photo_saved", mapOf("pose_id" to "p1"))]`; `repository.recorded == ["content://media/external/images/media/7"]`; log "Photo saved" không chứa địa chỉ hay tên file |
| Đếm lỗi không làm hỏng việc lưu | Unit test | `save_recordFails_stillSaved`: `failWrites = true`; `SAVED`, có `photo_saved`, log `WARN` với `error is IOException` |
| KB-6, đặc biệt: hết bộ nhớ, thử lại | Unit test | `save_storageFull_showsError_thenRetrySaves`: `saveError = StorageFullException()`; `FAILED_STORAGE_FULL`, không có `photo_saved`, ảnh vẫn `Ready` với cùng `location`; bỏ lỗi, `onRetrySave()`; `SAVED`, `saveCalls.size == 2` |
| Lỗi khác hết bộ nhớ | Unit test | `save_otherError_showsGenericError`: `saveError = IOException("boom")`; `FAILED`. `save_ioErrorWithNoSpace_isStorageFull`: `IOException(ErrnoException("write", OsConstants.ENOSPC))`; `FAILED_STORAGE_FULL`. `save_fileVanished_showsError`: xóa file trước khi bấm; `FAILED`, `saveCalls` rỗng |
| `isStorageFull` | Unit test | `PhotoStoreTest`: `StorageFullException()` là `true`; `IOException("write failed: ENOSPC (No space left on device)")` là `true`; `IOException(ErrnoException("write", OsConstants.ENOSPC))` là `true` (bọc hai lớp cũng `true`); `IOException("EACCES")` là `false`. Thất bại: khung rỗng luôn `false` |
| KB-6, "Chụp lại" sau khi lưu | Unit test | `retake_afterSave_leavesWithoutDialog`: lưu xong, `onRetakeClick()`; `pendingExit == null`; `Leave(true)`; file tạm đã bị xóa; `holder.latest.value == null` |
| KB-6, rời khi chưa lưu thì hỏi | Unit test | `retake_notSaved_asks`, `nextPose_notSaved_asks`, `close_notSaved_asks`: `pendingExit` lần lượt `RETAKE`, `NEXT_POSE`, `CLOSE`; không có `Leave` |
| KB-6, "Bỏ ảnh" | Unit test | `discardConfirm_leavesWithoutSaving`: hỏi từ `RETAKE`, `onDiscardConfirm()`; `Leave(false)`; `saveCalls` rỗng; file tạm đã bị xóa; không có `photo_saved`. `discardConfirm_fromNextPose_requestsNextPose`: `nextPoseRequests.consume() == true` sau khi rời |
| KB-6, "Lưu ảnh" trong hộp thoại | Unit test | `discardSave_savesThenLeaves`: hỏi từ `NEXT_POSE`, `onDiscardSave()`; `photo_saved` ghi một lần; `Leave(true)`; có yêu cầu tư thế tiếp. `discardSave_fails_staysWithError`: `saveError = StorageFullException()`; `FAILED_STORAGE_FULL`, `pendingExit == null`, không có `Leave`, không có yêu cầu tư thế tiếp |
| KB-6, chạm ra ngoài hay quay lại khi hộp thoại mở | Unit test | `dismissDialog_staysOnReview`: hỏi từ `CLOSE`, `onDismissDialog()`; `pendingExit == null`, vẫn `NOT_SAVED`, không có `Leave`, file tạm còn |
| KB-6, "Tư thế tiếp" sau khi lưu | Unit test | `nextPose_afterSave_requestsAndLeaves`: `Leave(true)`, `consume()` là `true` rồi `false` |
| KB-6, chia sẻ | Unit test | `share_notSaved_sharesCacheFile`: `Share(file.path)`. `share_afterSave_sharesSavedUri`: `Share("content://media/external/images/media/7")`. Chia sẻ không đổi `save` |
| KB-6, mở ảnh đã lưu gần nhất | Unit test | `lastSaved_opensSaved_closeLeavesAtOnce`: `ReviewDestination(savedPhotoUri = U)`, `existing = {U}`; `Ready(U, SAVED)`; `onCloseClick()` phát `Leave(false)` ngay, không xóa gì, không có `photo_saved`. `lastSaved_deleted_leaves`: `existing` rỗng; `Leave(false)` |
| Yêu cầu tư thế tiếp | Unit test | `NextPoseRequestsTest.consume_returnsOnceThenFalse`; `consume_withoutRequest_isFalse` |
| Xóa file tạm | Unit test | `CaptureFileStoreTest.delete_removesFileInDir`; `delete_ignoresFileOutsideDir` (file trong thư mục khác vẫn còn). Test cũ `newFile_deletesPreviousCaptures`, `newFile_createsMissingDirectory` giữ nguyên |
| Số ảnh đã lưu, ảnh gần nhất | Unit test | `DataStoreSavedPhotosRepositoryTest` (DataStore trong bộ nhớ, như `DataStoreCameraSettingsRepositoryTest`): `nothingStored_nullAndZero`; `recordSaved_incrementsAndKeepsLatest` (hai lần: đếm 2, ảnh gần nhất là địa chỉ thứ hai); `readFailsOnce_recovers` |
| KB-6, ô ảnh vừa chụp | Unit test | `LastPhotoViewModelTest.saved_andExists_exposesUri`; `saved_butDeleted_isNull`; `nothingSaved_isNull`; `startedAgain_afterSave_updates` (đổi `lastSavedPhoto`, gọi lại `onScreenStarted()`) |
| KB-6, tư thế tiếp: camera lấy yêu cầu | Unit test | `LastPhotoViewModelTest.nextPoseRequested_consumedOnce`: `requests.request()`; `consumeNextPoseRequest()` là `true`, lần sau `false`; log "Next pose requested". `noNextPoseRequest_isFalse`. Phần chọn tư thế kế tiếp (`onNextPose()`, `pose_selected` `next`, quay về đầu dải) đã có test của T-014 (`PoseViewModelTest.nextPose_followsStrip_andWraps`); dòng nối trong `CameraRoute` kiểm tra thủ công (dòng KB-6 trên máy thật) |
| KB-6, ô ảnh vừa chụp trên camera | Compose UI test | `CameraLastPhotoTest.withPhoto_isClickable_andOpens`: `CameraScreen(… lastPhoto = ImageBitmap(48, 48), onLastPhotoClick = …)`; `LAST_PHOTO` `assertHasClickAction`, bấm gọi callback một lần. `withoutPhoto_isNotClickable`: mặc định, `assertHasNoClickAction`. `uriWithoutImage_stillClickable`: `lastPhoto = null`, có callback; bấm được |
| Chữ đúng bảng chữ | Compose UI test | `ReviewScreenTest.default_showsTexts`: có "Xem lại", "Chụp lại", "Chia sẻ", "Tư thế tiếp", "Lưu vào thư viện"; nút đóng có mô tả "Đóng" |
| KB-6, nút gọi đúng hành động | Compose UI test | `default_buttonsCallBack`: bấm lần lượt "Lưu vào thư viện", "Chụp lại", "Chia sẻ", "Tư thế tiếp", nút đóng; mỗi callback được gọi đúng một lần |
| KB-6, đang lưu | Compose UI test | `saving_showsSavingAndDisablesOthers`: có "Đang lưu…" và `PrimaryButtonTestTags.SPINNER`; ba nút phụ và nút đóng `assertIsNotEnabled`; bấm "Chụp lại" không gọi callback |
| Sau khi lưu | Compose UI test | `saved_saveButtonDisabled`: `SAVED`; `SAVE` `assertIsNotEnabled`; ba nút phụ bấm được |
| KB-6, đặc biệt: hết bộ nhớ | Compose UI test | `saveErrorFull_showsMessageAndRetry`: có "Không lưu được ảnh: bộ nhớ máy đã đầy." trong `SAVE_ERROR`; bấm "Thử lại" gọi `onRetrySave`; `PHOTO` vẫn có. `saveErrorOther_showsGenericMessage`: `FAILED` có "Không lưu được ảnh." |
| KB-6, hộp thoại | Compose UI test | `discard_showsDialog_andCallsBack`: `pendingExit = RETAKE`; `DISCARD_DIALOG` có "Bỏ ảnh này?", "Ảnh chưa được lưu vào thư viện."; bấm "Bỏ ảnh" gọi `onDiscardConfirm`, "Lưu ảnh" gọi `onDiscardSave`. `discard_scrimTapDismisses`: bấm vào lớp mờ ở góc (10 dp, 10 dp) gọi `onDismissDialog`. `discard_hidesContentFromAccessibility`: khi hộp thoại mở, không tìm được node "Chụp lại" bằng `onAllNodesWithText(...)` (`fetchSemanticsNodes().isEmpty()`) |
| Màn hình thấp (L-028) | Compose UI test, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "w360dp-h640dp-xxhdpi")` | `small_saveError_primaryButtonVisible`: ở `FAILED_STORAGE_FULL`, mép dưới của `SAVE` không quá chiều cao gốc và cao ít nhất 56 dp; `SAVE_ERROR` nằm trên `SAVE`, không chồng lên hàng nút phụ; `PHOTO` có tỷ lệ rộng/cao 0.75 (sai số 1 px) |
| Intent chia sẻ | Unit test (Robolectric) | `ShareIntentsTest.chooser_wrapsSendWithStreamAndGrant`: `Intent.EXTRA_INTENT` của chooser có `ACTION_SEND`, `type == "image/jpeg"`, `EXTRA_STREAM == uri`, cờ `FLAG_GRANT_READ_URI_PERMISSION`, `clipData` chứa `uri`; tiêu đề "Chia sẻ ảnh" |
| Ảnh đúng chiều, thu nhỏ | Unit test (Robolectric, `@GraphicsMode(NATIVE)`) | `AndroidPhotoDecoderTest.decode_scalesDown`: JPEG 1000 x 800 ghi bằng `Bitmap.compress`, `decode(path, 300)`; cạnh dài không quá 300. `decode_appliesExifRotation`: cùng ảnh, ghi `TAG_ORIENTATION = ORIENTATION_ROTATE_90` bằng `ExifInterface.saveAttributes()`; ảnh trả về cao hơn rộng. `decode_missingFile_returnsNull`. Nếu Robolectric không ghi được EXIF thì dừng ở test này (mục 7, điều 5) |
| Đúng thiết kế, 4 trạng thái | Screenshot test, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "w411dp-h891dp-xxhdpi")`, `image = null` (khung ảnh trống thay cho ảnh tạm của thiết kế) | `ReviewScreenScreenshotTest`: `default_dark` (`ReviewScreen_default_dark.png`), `saving_dark` (`ReviewScreen_saving_dark.png`), `saveError_dark` (`ReviewScreen_save-error_dark.png`, `FAILED_STORAGE_FULL`), `discard_dark` (`ReviewScreen_discard_dark.png`, `pendingExit = RETAKE`) |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` | `saveError_small` (`ReviewScreen_save-error_small.png`: thông báo lỗi, ba nút phụ và nút chính, nhiều nội dung nhất) |
| So với thiết kế | Ảnh so sánh và rà soát độc lập (bước 6) | Chạy `scripts/design-compare.sh camera` sau khi ghi ảnh chuẩn; commit `docs/features/camera/design/review/` và dán `review/summary.md` vào mô tả PR (ADR-0020). Phiên rà soát xem ảnh ghép của 4 ảnh T-016 theo mục 7 của README thiết kế. Khác biệt đã biết: khung ảnh trống thay cho ảnh tạm "person"; mọi thứ cao hơn thiết kế 24 dp vì khung test không có thanh trạng thái (mục 3.1) |
| Ảnh thiết kế của ticket khác | Khai báo cho `scripts/plan-check.py` | Ảnh thiết kế của ticket khác (T-013, T-014, T-015, T-017, T-020): `CameraScreen_starting_dark`, `CameraScreen_permission-denied_dark`, `CameraScreen_error_dark`, `CameraScreen_countdown_dark`, `CameraScreen_default_dark`, `CameraScreen_no-pose_dark`, `CameraScreen_steps-hidden_dark`, `CameraScreen_adjust_dark`, `CameraScreen_locked_dark`, `CameraScreen_matching_dark`, `CameraScreen_matched_dark`, `CameraScreen_no-person_dark`, `CameraScreen_default_dark_unfolded`, `ReviewScreen_default_dark_unfolded` |
| Lưu thật vào thư viện máy | Test trên emulator (API 34 của CI) | `PhotoSaveDeviceTest.save_insertsIntoAppAlbum`: ghi một JPEG nhỏ vào `cacheDir/captures/`, gọi `MediaStorePhotoStore(context).save(file, 0L)`; truy vấn địa chỉ trả về: `RELATIVE_PATH == "Pictures/Camera Pose/"`, `MIME_TYPE == "image/jpeg"`, `IS_PENDING == 0`, đọc lại đủ số byte; `exists(uri) == true`; cuối test `contentResolver.delete(uri)` rồi `exists(uri) == false`. `cacheCapture_readableThroughFileProvider`: `FileProvider.getUriForFile(context, "<gói>.captures", file)` mở được `InputStream`. Không mở camera, không qua giao diện |
| Manifest không có quyền đọc ảnh | Kiểm tra trong bước 5 | `aapt2 dump permissions` (hoặc `apkanalyzer manifest permissions`) trên APK `stagingDebug`: không có `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE`, `READ_MEDIA_VISUAL_USER_SELECTED`; phương án B: cũng không có `WRITE_EXTERNAL_STORAGE`. Ghi kết quả vào mô tả PR |
| KB-6 trên máy thật | Kiểm tra thủ công | Android 10 trở lên: chụp, lưu, mở app Ảnh thấy ảnh trong album "Camera Pose", đúng chiều, không có nét viền; không có hộp hỏi quyền. Chia sẻ trước và sau khi lưu sang một app (ví dụ Gmail nháp): ảnh đi kèm. Chụp lại, Tư thế tiếp, nút quay lại khi chưa lưu: hỏi "Bỏ ảnh này?"; chạm ngoài hộp: ở lại. Phương án A: "Tư thế tiếp" về camera với ô bên cạnh ô lúc chụp (ô cuối thì về ô đầu). Quay về camera: tư thế, nét viền, công cụ giữ nguyên. Ô ảnh vừa chụp hiện ảnh vừa lưu; xóa ảnh đó trong app Ảnh rồi quay lại camera: ô trống |
| Android 8 hoặc 9 | Kiểm tra thủ công | Theo câu trả lời mục 8 câu 1, trên emulator API 28 tại máy dev (CI chỉ có API 34) |
| Hết bộ nhớ | Kiểm tra thủ công | Emulator có bộ nhớ nhỏ, chép file lớn tới khi còn dưới 5 MB trống: bấm lưu hiện thông báo hết bộ nhớ; xóa file, "Thử lại": lưu được |
| Tiến trình bị đóng | Kiểm tra thủ công | Ở màn xem lại, bật "Don't keep activities" hoặc `adb shell am kill` khi app ở nền, mở lại: vẫn là ảnh vừa chụp, lưu được |

**Ngoại lệ viết test trước:**

- `MediaStorePhotoStore`: gọi MediaStore của hệ thống, Robolectric không có MediaStore thật. Test emulator `PhotoSaveDeviceTest` viết ở bước 3 cùng các test khác (thất bại vì khung rỗng ném `IOException`), nhưng chỉ chạy trên CI sau khi đẩy nhánh, không chạy được trong phiên.
- `ReviewRoute`, phần dựng `FileProvider` và `startActivity`; trong `CameraRoute`: `repeatOnLifecycle` và dòng gọi `poseViewModel.onNextPose()`: nối Android mỏng, logic nằm ở ViewModel và `shareChooser` đều có test trước; kiểm chứng bằng test emulator (FileProvider) và kiểm tra thủ công.
- `AppNavHost.kt` (đăng ký đích): như T-013, không có test JVM riêng cho điều hướng cần `hiltViewModel()`; `AppLaunchTest` vẫn tới `camera_screen`.
- 4 biểu tượng, `camera_capture_paths.xml`, `AndroidManifest.xml` của module: tài nguyên và cấu hình.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 4 (ghi `photo_saved`, đã có trong spec mục 6; ở phương án A, `pose_selected` `next` do code của T-014 ghi, T-016 chỉ gọi). Điểm 3 nếu người duyệt chọn phương án A ở mục 8 câu 1 (`WRITE_EXTERNAL_STORAGE` cho Android 9 trở xuống); khi đó thêm `permission` vào `blocking_points` và người phụ trách sản phẩm cần biết (khai báo Data safety). Không chạm điểm 2 (không thêm thư viện), điểm 7 (không sửa module core, không đổi cấu hình build; `<provider>` nằm trong manifest của feature), điểm 9 (khóa DataStore mới). Điểm 6: chia sẻ mở hộp chia sẻ của hệ thống với ảnh của chính người dùng, không mở liên kết ra ngoài hay quảng bá app khác; AI cho là không chạm, người duyệt thấy khác thì sửa `blocking_points`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Khoảng 45 file trong `:feature:camera` và `:app` (điều hướng, test emulator, có thể cả manifest) |
| B. Độ rõ của yêu cầu | 2 | Spec đã xác nhận, thiết kế đã duyệt, nhưng còn câu hỏi ảnh hưởng cách làm: lưu trên Android 8 và 9 (câu 1), nút chính sau khi lưu (câu 2), lỗi khác hết bộ nhớ (câu 3), tên thư mục ảnh (câu 4) |
| C. Khả năng kiểm chứng tự động | 3 | Phụ thuộc thiết bị: MediaStore, bộ nhớ đầy, hộp chia sẻ, tiến trình bị đóng, phiên bản Android. Logic có test trên JVM; MediaStore chỉ test trên emulator API 34; Android 8, 9 và hết bộ nhớ chỉ kiểm tra thủ công |
| D. Ảnh hưởng chéo | 2 | Sửa `CameraScreen.kt`, `CameraControls.kt`, `CameraNavigation.kt`, có thể `CapturedPhotoHolder.kt` mà T-014 cũng sửa; gọi `PoseViewModel.onNextPose()` của T-014 mà không sửa nó; `AppNavHost.kt` mà T-012, T-021, T-025 cũng sửa; `design/review/` mà mọi ticket camera sinh lại |
| E. Độ mới của kỹ thuật | 2 | MediaStore với `IS_PENDING`, `FileProvider`, giải mã ảnh theo EXIF: chưa có trong dự án nhưng phổ biến |
| **Tổng** | 12 | **Mức: Cao** (chạm điểm chặn, và tổng điểm cũng từ 9 trở lên) |

**`token_estimate`.** Số gợi ý cỡ L là 9.5M (trung vị 7 task L). AI ghi 25M: task gần nhất cùng module và cùng cỡ, T-013, đo được 24.1M lúc merge (60.4M tính cả phần cuối phiên, RUN-20261022-01); T-016 có số file tương đương, một test emulator mới và có thể một vòng CI thêm vì test đó chỉ chạy trên CI.

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AppNavHost.kt` (thêm đích, nối hai callback của camera); `CameraScreen.kt`, `CameraControls.kt`, `CameraNavigation.kt`, `CaptureFileStore.kt` của T-013; `CapturedPhotoHolder.kt` chỉ khi T-014 chưa thêm `poseId`; `PoseViewModel.onNextPose()` của T-014 (chỉ gọi, ở phương án A); manifest gộp của app (thêm một `<provider>`, phương án A thêm một quyền); DataStore chung (hai khóa mới, tiền tố `camera_` không trùng khóa nào hiện có); `docs/features/camera/design/review/`. Không sửa module core nào.
- Task khác có thể xung đột (các plan đang viết song song):
  - T-014 (plan `features/camera/plans/T-014-camera-chon-tu-the.md`, bản nháp viết song song; phần tư thế nằm trong `PoseViewModel` mới, `ActivePoseHolder`, `onNextPose()`): cùng sửa `CameraScreen.kt` (T-014 thêm `poseViewModel` vào `CameraRoute` và `PosePicker` vào `CameraScreen`; T-016 thêm `LastPhotoViewModel`, dòng gọi `onNextPose()`, `repeatOnLifecycle` trong `CameraRoute` và hai tham số có mặc định vào `CameraScreen`), `CameraControls.kt` (T-014 sửa `PreviewContent`, `ReadyPreview`; T-016 chỉ `CaptureRow`, `LastPhotoSlot`), `CameraNavigation.kt` (T-014 thêm `poseId` vào `CameraDestination`; T-016 thêm `onOpenLastPhoto` vào `cameraScreen`), `CapturedPhotoHolder.kt` (cả hai định thêm cùng trường `poseId: String = ""`; task chạy sau dùng lại), `design/review/`. T-016 không sửa `CameraViewModel.kt`, `PoseViewModel.kt`, `strings.xml` (dùng `strings_review.xml`), `CameraScreenTest.kt`, `CameraViewModelTest.kt` hay ảnh chuẩn camera (T-014 ghi lại bảy ảnh đó; T-016 không đổi chúng vì tham số mới có mặc định). Xung đột chỉ là dòng; T-016 dựng phần mình trong file riêng (`review/`, `lastphoto/`, `photo/`, `ReviewModule.kt`, `CameraLastPhotoTest.kt`). `NextPoseRequests` thuộc T-016; T-014 không tạo.
  - T-012 (màn chào), T-021 (thư viện tư thế), T-025 (cài đặt): cùng sửa `AppNavHost.kt` để đăng ký đích và nối `onOpenPoses`, `onOpenSettings` của `cameraScreen`. T-016 chỉ thêm `onPhotoCaptured`, `onOpenLastPhoto` và dòng `reviewScreen`; task chạy sau dựng trên nhánh chính đã có thay đổi của task trước, giữ nguyên các tham số đã nối.
  - T-018, T-019: dựng trên `ReviewEvent.Leave(savedThisVisit)` và `SavedPhotosRepository.savedCount` của T-016; phải chạy sau T-016.
  - T-020: bố cục mở gập của `ReviewScreen`; chạy sau T-016.
  - T-025: nếu đưa "âm khi chụp" vào camera thì không đụng màn xem lại.
- Đề xuất thứ tự chạy: sau T-014 trong cùng phiên hoặc phiên trước (plan T-014 cũng đề xuất thứ tự này), để dùng phương án A, đủ tiêu chí "Tư thế tiếp" và có `pose_id` thật trong `photo_saved` (T-014 chỉ phụ thuộc T-009, T-013 đã merge). Nếu T-014 dừng, T-016 vẫn chạy được theo phương án B. Thứ tự giữa T-016 và T-012, T-021, T-025 không quan trọng, chỉ cần lần lượt. Trước T-018, T-019, T-020.

## 7. Điều kiện dừng riêng

1. Lúc bắt đầu task, mục 8 câu 1 chưa có câu trả lời: không bắt đầu (plan không được duyệt khi chưa có câu trả lời). Người duyệt chọn phương án A mà `blocking_points` chưa có `permission`, hoặc người phụ trách sản phẩm chưa biết: dừng.
2. Lúc bắt đầu task, `CapturedPhotoHolder`, `CapturedPhoto`, `CaptureFileStore`, `CameraScreen` hay `cameraScreen()` trên nhánh chính khác những gì mục 3.1 dựa vào (ví dụ T-014 đã đổi `CapturedPhoto` thành kiểu khác, hay bỏ `LastPhotoSlot`), và thích ứng cần sửa file không có ở mục 3.2: dừng.
3. Phương án A: `PoseViewModel` trên nhánh chính không có `fun onNextPose()`, hoặc `CameraRoute` không có `poseViewModel` để gọi, hoặc `onNextPose()` không ghi `pose_selected` `next` như plan T-014: dùng phương án B, ghi vào nhật ký; không sửa `PoseViewModel.kt` hay test của T-014.
4. `CapturedPhoto` trên nhánh chính đã có trường tư thế nhưng tên hay kiểu khác `poseId: String`: dùng trường đó nếu chỉ đổi tên trong code của T-016; cần sửa `CapturedPhotoHolder.kt` theo cách khác thêm trường thì dừng.
5. `AndroidPhotoDecoderTest.decode_appliesExifRotation` không chạy được trên Robolectric vì `ExifInterface.saveAttributes()` không ghi được: dừng, không bỏ test; đề xuất chuyển test này sang `PhotoSaveDeviceTest` trong plan sửa lại.
6. `PhotoSaveDeviceTest` đỏ trên CI vì `RELATIVE_PATH` trả về khác mong đợi (ví dụ thiếu dấu `/` cuối) hay vì quyền: sửa trong `MediaStorePhotoStore` được tính vào giới hạn 3 lần sửa chung; không nới điều test kiểm tra.
7. Dựng FileProvider cần thêm thư viện hay sửa `feature/camera/build.gradle.kts`: dừng (điểm chặn 2).
8. Kiểm tra quyền của APK ở bước 5 thấy quyền đọc ảnh (do thư viện nào đó gộp vào): dừng, không thêm `tools:node="remove"` khi plan chưa nêu.

## 8. Câu hỏi cho người duyệt

1. **Lưu ảnh trên Android 8 và 9 (chặn, phải trả lời trước khi duyệt).** App có `minSdk 26`. Từ Android 10, app thêm ảnh vào thư viện qua MediaStore không cần quyền nào. Trên Android 8 và 9, MediaStore và thư mục `Pictures/` chỉ ghi được khi có quyền `WRITE_EXTERNAL_STORAGE`; manifest hiện chỉ có `CAMERA` và `ACCESS_NETWORK_STATE`, spec không nêu quyền này, CLAUDE.md cấm thêm quyền ngoài spec. Máy Android Go và máy giá rẻ ở thị trường đích (spec câu 5) vẫn còn nhiều máy Android 8.1 và 9. Các phương án:
   - A. Thêm `WRITE_EXTERNAL_STORAGE` với `android:maxSdkVersion="28"`: chỉ Android 9 trở xuống thấy quyền; hỏi quyền lúc bấm lưu lần đầu. Lưu ý: trên các bản này, quyền ghi đi cùng nhóm với quyền đọc, nên hộp hỏi ghi "ảnh và nội dung đa phương tiện", trái tinh thần bài học 13 dù manifest không có quyền đọc. Cần cập nhật spec (mục 2 và KB-6), khai báo Data safety, thêm chữ cho trường hợp bị từ chối, và thêm `permission` vào `blocking_points`.
   - B. Không thêm quyền. Android 8 và 9 lưu vào thư mục ảnh riêng của app trên bộ nhớ ngoài (`getExternalFilesDir(DIRECTORY_PICTURES)`, không cần quyền) rồi báo cho hệ thống quét file. Ảnh có thể hiện trong app Ảnh tùy máy, và **bị xóa khi gỡ app**, trái spec mục 6 ("gỡ app không xóa"). Cần ghi điều này vào spec.
   - C. Nâng `minSdk` lên 29. Ngoài phạm vi plan (cấu hình build, điểm chặn 7, ảnh hưởng mọi module) và bỏ máy Android 8, 9.
   - Đề xuất của AI: A, vì giữ được lời hứa "ảnh nằm trong thư viện máy, gỡ app không mất" cho mọi máy. Plan đã liệt kê `app/src/main/AndroidManifest.xml` cho phương án A. Nếu chọn A, AI sẽ tăng plan lên phiên bản 2 để thêm luồng hỏi quyền khi lưu, chữ khi bị từ chối và test tương ứng, sau khi spec được cập nhật.
- Trả lời: Đồng ý với đề xuất của AI.
2. **Nút chính sau khi đã lưu.** Thiết kế không có trạng thái "đã lưu". Đề xuất của AI: nút giữ chữ "Lưu vào thư viện" nhưng mờ, không bấm được, để người dùng không lưu hai bản. Nếu muốn rõ hơn (ví dụ "Đã lưu" kèm dấu ✓), cần thêm vào thiết kế và bảng chữ trước.
- Trả lời: Đồng ý với đề xuất của AI.
3. **Lưu lỗi không phải vì hết bộ nhớ.** Thiết kế chỉ có chữ cho hết bộ nhớ. Đề xuất của AI: lỗi khác hiện cùng kiểu thông báo với chữ "Không lưu được ảnh." và nút "Thử lại" (chữ mới, cần đưa vào bảng chữ của thiết kế). Không đồng ý thì mọi lỗi dùng chữ hết bộ nhớ, dù có thể sai. App coi là hết bộ nhớ khi còn dưới 5 MB trống trước khi ghi, hoặc khi hệ thống báo `ENOSPC`.
- Trả lời: Đồng ý với đề xuất của AI.
4. **Tên thư mục ảnh.** Bản đề xuất ghi `Pictures/<TênApp>/`. Tên app đầy đủ "Camera Pose: Lines and Guides" có dấu hai chấm, không dùng được trong tên thư mục trên mọi máy. Đề xuất của AI: `Pictures/Camera Pose/`, tên file `CameraPose_<yyyyMMdd_HHmmss>.jpg`, không dịch theo ngôn ngữ máy.
- Trả lời: Đồng ý với đề xuất của AI.
5. **Rời màn xem lại bằng nút đóng.** Spec liệt kê "Chụp lại", "Tư thế tiếp" và nút quay lại; thiết kế có thêm nút `close` ở thanh trên. Đề xuất của AI: nút đóng làm giống nút quay lại (hỏi khi chưa lưu, về camera).
- Trả lời: Đồng ý với đề xuất của AI.
6. **Nút quay lại khi đang lưu.** Đề xuất của AI: bỏ qua cho tới khi lưu xong (thường dưới một giây), để không bỏ dở một lần ghi vào thư viện.
- Trả lời: Đồng ý với đề xuất của AI.
7. **Tiêu đề hộp chọn khi chia sẻ và mô tả cho trình đọc màn hình.** Không có trong bảng chữ. Đề xuất của AI: hộp chọn "Chia sẻ ảnh"; nút đóng "Đóng"; ba nút phụ đọc theo chữ của chúng.
- Trả lời: Đồng ý với đề xuất của AI.
8. **Ảnh camera trước.** Ảnh lưu như CameraX ghi (không lật gương), đúng câu trả lời câu 7 của plan T-013. Màn xem lại cũng hiện như vậy, nên ảnh trên màn xem lại có thể ngược với khung xem trước. Đề xuất của AI: giữ như vậy; muốn lật thì làm ở plan riêng.
- Trả lời: Đồng ý với đề xuất của AI.
9. **Cỡ task và có nên tách không.** Plan khoảng 45 file, một test emulator chỉ chạy trên CI, và phần "Tư thế tiếp" phụ thuộc T-014. Đề xuất của AI: giữ một plan với phương án A và B như mục 3.1; vì T-014 làm sẵn `onNextPose()`, phần "Tư thế tiếp" của T-016 chỉ còn một dòng nối, nên tách không còn nhiều lợi. Nếu người duyệt vẫn muốn chắc hơn: tách T-016a (màn xem lại, lưu, chia sẻ, hỏi bỏ ảnh, đếm, `photo_saved`; khoảng 35 file) và T-016b (ô ảnh vừa chụp và "Tư thế tiếp", chạy sau T-014; khoảng 8 file).
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
