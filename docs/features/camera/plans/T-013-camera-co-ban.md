---
ticket: T-013
title: Camera: khung xem trước, quyền, công cụ chụp, chụp ảnh
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 40.0M        # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [analytics, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-013: CAMERA: KHUNG XEM TRƯỚC, QUYỀN, CÔNG CỤ CHỤP, CHỤP ẢNH

## 1. Tóm tắt

Tạo module `:feature:camera` với màn camera chạy thật: khung xem trước 3:4 bằng CameraX, xin quyền và trạng thái chưa có quyền, trạng thái đang mở và lỗi, flash, hẹn giờ, lưới, đổi camera, zoom, nút chụp và đếm ngược, giữ lựa chọn công cụ trong DataStore, ghi `camera_permission_result` và `photo_captured`. CameraX nằm sau interface `CameraController`, nên ViewModel và màn hình được test trên JVM bằng bản giả; bốn thành phần mới (`CameraIconButton`, `GridOverlay`, `ZoomControl`, `StateMessage`) vào `:core:designsystem`. Kiểm chứng bằng unit test ViewModel với thời gian ảo, test giao diện Robolectric, screenshot test cùng tên bốn ảnh thiết kế không cần tư thế, test emulator đến được camera, và danh sách kiểm tra thủ công trên máy thật cho phần phần cứng.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md` (phần không có tư thế; phần tư thế thuộc T-014 đến T-017):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-4 | Khi bấm nút flash thì flash đổi lần lượt: tắt, bật, tự động (mặc định tắt); biểu tượng cho biết chế độ; camera trước không có flash thì nút mờ đi | Chu kỳ `OFF`, `ON`, `AUTO`. "Không có flash" lấy từ `CameraInfo.hasFlashUnit()` của camera đang mở, không chỉ dựa vào camera trước. Nút mờ thì không bấm được, chế độ đã lưu vẫn giữ để dùng lại khi đổi về camera sau |
| KB-4 | Khi bấm nút hẹn giờ thì đổi lần lượt: tắt, 3 giây, 10 giây; khi bật, nút ở trạng thái đang bật và hiện "3s", "10s" | Khi tắt, nút hiện biểu tượng `timer`, nền thường. Khi bật, nút vàng hiện chữ |
| KB-4 | Khi bấm nút lưới thì lưới chia ba hiện hoặc ẩn; nút đang bật khi lưới hiện | `GridOverlay` vẽ trong khung xem trước |
| KB-4 | Khi bấm nút đổi camera thì đổi giữa trước và sau; nét viền và tư thế giữ nguyên | Phần đổi camera làm ở đây. Phần "nét viền và tư thế giữ nguyên" kiểm chứng ở T-014 khi có tư thế. Zoom về 1x khi đổi camera vì mỗi camera có dải zoom khác nhau |
| KB-4 | Khi máy có ống kính góc rộng thì zoom có 0.5x, 1x, 2x; không có thì chỉ 1x và 2x; mức đang chọn có nền màu nhấn | Mức hiện khi nằm trong `[minZoomRatio, maxZoomRatio]` của camera đang mở. Có 0.5 khi `minZoomRatio <= 0.5`. Máy có `maxZoomRatio < 2` thì chỉ còn 1x (và 0.5 nếu có); chỉ còn một mức thì ẩn `ZoomControl` |
| KB-4 | Khi mở lại app thì hẹn giờ, lưới, flash, camera trước sau giữ như lần trước; zoom về 1x | Bốn khóa DataStore mới, không có dữ liệu cũ cần chuyển. Zoom không lưu |
| KB-6 | Khi bấm nút chụp mà hẹn giờ tắt thì app chụp ngay (có âm chụp nếu bật trong Cài đặt), ghi `photo_captured`, mở màn xem lại với ảnh vừa chụp, không có nét viền | Chụp, phát âm chụp (luôn bật, vì màn Cài đặt T-025 chưa có, mục 8 câu 6), ghi sự kiện, đưa ảnh vào `CapturedPhotoHolder` và gọi `onPhotoCaptured`. Mở màn xem lại là việc của T-016: ở T-013 `:app` truyền `onPhotoCaptured` rỗng. Ảnh chụp từ camera nên không bao giờ có nét viền |
| KB-6 | Khi hẹn giờ bật thì số đếm ngược hiện to giữa khung, nút chụp đổi thành nút hủy; hết giờ thì chụp; bấm hủy thì dừng, không chụp | Đếm theo giây (3, 2, 1) bằng `delay` trong `viewModelScope`, test bằng thời gian ảo |
| KB-6 | Khi quay về camera từ màn xem lại thì công cụ chụp giữ như trước | Công cụ nằm trong ViewModel và DataStore, nên giữ. Kiểm chứng đầy đủ ở T-016 |
| KB-6 | Khi chạm ô ảnh vừa chụp mà chưa có ảnh nào thì ô trống và không làm gì | Ô ảnh vừa chụp ở T-013 luôn trống, không bấm được. T-016 nối ảnh đã lưu gần nhất |
| KB-7 | Khi chưa có quyền camera và máy còn hỏi được thì camera hỏi quyền khi mở; app ghi `camera_permission_result` | Hỏi một lần mỗi lần vào camera (lưu cờ trong `SavedStateHandle`). Đích có tham số `askPermission` (mặc định `true`) để màn chào (T-012) vào camera ngay sau khi người dùng vừa từ chối mà không hỏi lại lần hai. Sự kiện ghi mỗi khi hộp hỏi quyền trả kết quả, `source` là `camera` |
| KB-7 | Khi bị từ chối thì khung xem trước hiện tiêu đề "Chưa có quyền dùng camera", lời giải thích, nút "Mở cài đặt"; nút chụp mờ đi; chip và dải tư thế vẫn dùng được | Phần chip và dải tư thế thuộc T-014 |
| KB-7 | Khi bấm "Mở cài đặt" thì trang cài đặt của app mở ra; cho phép rồi quay lại thì camera tự chạy | `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` với `package:<tên gói>`. Mỗi lần màn hình về `ON_START`, `CameraRoute` kiểm tra quyền và báo ViewModel |
| KB-8 | Khi camera đang khởi động thì khung hiện vòng chờ và "Đang mở camera…"; nút chụp mờ | Trạng thái `Starting` |
| KB-8 | Khi không mở được camera thì hiện "Không mở được camera", lời giải thích, nút "Thử lại"; bấm thì thử mở lại | Trạng thái `Error`; lỗi khi gắn CameraX vào vòng đời được bọc thành `CameraOpenException` |
| KB-8 | Khi app ra nền rồi quay lại thì camera tự chạy lại, giữ tư thế và các lựa chọn | `ON_STOP` đóng camera và hủy đếm ngược; `ON_START` mở lại với công cụ trong bộ nhớ |
| Phạm vi ticket | Cờ `feature_camera_enabled`; camera là đích sau loading khi cờ bật | Cờ mới trong `FeatureFlag`, bật sẵn ở staging. `MainScreenChooser` của T-010 đọc cờ khi loading xong và chọn `CAMERA` (mục 3.1, phần Điều hướng) |
| Phạm vi ticket | Dừng đếm ngược khi có hộp thoại mất mạng (nếu T-011 đã xong) | Interface `CountdownInterruptions` trong feature; nối vào `NetworkStatus.dialogShown` khi `:core:network` có trên nhánh chính, ngược lại là luồng luôn `false` |
| Phạm vi ticket | Khóa chiều dọc | `android:screenOrientation="portrait"` cho `MainActivity` (spec, câu 7: mọi màn khóa dọc). Với targetSdk 36, Android 16 bỏ qua khóa này trên màn rộng (sw600dp trở lên), đúng ý "máy gập mở ra dùng bố cục mở gập" của T-020 |
| Phạm vi ticket | Chụp xong giữ ảnh cho màn xem lại (T-016) | `CapturedPhoto` và `CapturedPhotoHolder` trong cùng module (mục 3.1, phần Bàn giao ảnh) |

Thiết kế tham chiếu: `features/camera/design/README.md`; số liệu trong `design/context/`; ảnh trong `design/screens/`. Phân chia ảnh thiết kế:

| Ảnh | Ticket | Lý do |
| --- | --- | --- |
| `CameraScreen_starting_dark`, `CameraScreen_permission-denied_dark`, `CameraScreen_error_dark` | T-013 | Trạng thái của KB-7, KB-8; không cần tư thế |
| `CameraScreen_countdown_dark` | T-013 | Đếm ngược thuộc KB-6. Ảnh thiết kế có thẻ bước, nét viền và cột công cụ nét viền: T-013 vẽ phần còn lại, T-014 và T-015 ghi lại ảnh khi thêm phần của mình |
| `CameraScreen_default_dark`, `CameraScreen_no-pose_dark`, `CameraScreen_steps-hidden_dark` | T-014 | Cần nét viền, thẻ bước hoặc lời nhắc chọn tư thế |
| `CameraScreen_adjust_dark`, `CameraScreen_locked_dark` | T-015 | Chỉnh nét viền |
| `CameraScreen_matching_dark`, `CameraScreen_matched_dark`, `CameraScreen_no-person_dark` | T-017 | Điểm khớp |
| `CameraScreen_default_dark_unfolded` | T-020 | Máy gập |
| `ReviewScreen_*` (5 ảnh) | T-016 | Màn xem lại |

Mọi ảnh thiết kế của camera đều có hàng chip và dải tư thế (T-014). T-013 chừa đúng chỗ của hai hàng này (cao 132 dp, mục 3.1) nhưng để trống, nên bốn ảnh chuẩn của T-013 khác thiết kế ở vùng đó. T-014 ghi lại các ảnh này.

ADR liên quan: ADR-0001 (module feature, phụ thuộc core), ADR-0003 (cờ tính năng), ADR-0008 (thành phần mới trong `:core:designsystem`, không viết cứng màu và cỡ chữ), ADR-0012 (sự kiện qua `:core:analytics`), ADR-0019 (log qua `AppLogger`), ADR-0020 (tên screenshot test theo ảnh thiết kế, ảnh so sánh).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu `:feature:sample` (repository trong `data/`, Hilt module trong `di/`, `UiState` sealed, ViewModel với `StateFlow`, `Route` nối ViewModel và `Screen` không trạng thái, đích `@Serializable`) và mẫu thành phần của `:core:designsystem` (T-007: `component/`, catalog, test đo kích thước và màu điểm ảnh). Gói gốc: `com.vglstudio.camera.pose.line.guide.feature.camera`. Thẻ log: `Camera`.

**Điều kiện đầu vào.** T-010 đã merge (`MainScreen`, `MainScreenChooser`, tham số `chooseMainScreen` của `AppNavHost`, plan T-010 mục 3.1). T-007 đã merge: `MaterialTheme.appColors` có `cameraBackground`, `cameraSurface`, `onCamera`, `cameraOnSurfaceVariant`, `cameraControl`, `cameraOnControl`, `cameraScrim`; có `Radius.thumb`, `Radius.full`, `Stroke.sticker`; `PrimaryButton` kiểu Sticker. Thiếu hay khác tên thì dừng (mục 7).

**Lớp camera (`camera/`).** CameraX chỉ xuất hiện trong `CameraXController.kt` và trong `CameraRoute` (khung `CameraXViewfinder`). ViewModel chỉ biết interface:

```kotlin
interface CameraController {
    /** Surface for the viewfinder; null while no camera is bound. */
    val surfaceRequest: StateFlow<SurfaceRequest?>
    /** Binds preview and capture for [lens]. Throws [CameraOpenException]. */
    suspend fun open(lens: Lens, flash: FlashMode): CameraCapabilities
    fun setFlash(mode: FlashMode)
    fun setZoom(ratio: Float)
    /** Throws [CaptureFailedException]. */
    suspend fun takePicture(): File
    fun close()
}
data class CameraCapabilities(val hasFlash: Boolean, val minZoomRatio: Float, val maxZoomRatio: Float)
class CameraOpenException(cause: Throwable) : Exception(cause)
class CaptureFailedException(cause: Throwable) : Exception(cause)
```

- `CameraXController` (`@ViewModelScoped`, nhận `@ApplicationContext`, `CaptureFileStore`, `AppLogger`): tự giữ một `LifecycleOwner` riêng (`LifecycleRegistry`), nên ViewModel không phải cầm `LifecycleOwner` của màn hình. `open`: lấy `ProcessCameraProvider.awaitInstance(context)`, tạo `Preview` và `ImageCapture` với `ResolutionSelector` tỷ lệ 4:3 (`AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY`), `CAPTURE_MODE_MINIMIZE_LATENCY`, đặt `flashMode`; `unbindAll()` rồi `bindToLifecycle` với `CameraSelector` theo `lens`, đưa registry sang `RESUMED`. Mọi lỗi (trừ `CancellationException`) bọc thành `CameraOpenException`. Trả `CameraCapabilities` từ `cameraInfo.hasFlashUnit()` và `cameraInfo.zoomState.value`. `Preview.setSurfaceProvider` đẩy `SurfaceRequest` vào `surfaceRequest`.
- Ảnh đúng chiều khi cầm ngang: app khóa dọc, nên `OrientationEventListener` (bật trong `open`, tắt trong `close`) cập nhật `imageCapture.targetRotation` theo góc cầm máy.
- `takePicture`: lấy file từ `CaptureFileStore.newFile()`, gọi `ImageCapture.takePicture(OutputFileOptions, mainExecutor, callback)` qua `suspendCancellableCoroutine`; `onCaptureStarted` phát `MediaActionSound.SHUTTER_CLICK` (âm hệ thống, không cần quyền); lỗi bọc thành `CaptureFailedException`. Ảnh camera trước giữ mặc định của CameraX, không lật gương (mục 8 câu 7).
- `close`: `unbindAll()`, registry về `CREATED`, `surfaceRequest` về `null`. `ViewModel.onCleared` gọi `close()`.
- Không cần thư viện mới: `awaitInstance` là hàm suspend của `camera-lifecycle` 1.6.2; `CameraXViewfinder` thuộc `camera-compose` 1.6.2.
- `CaptureFileStore(dir: File)`: thư mục `cacheDir/captures`. `newFile(nowMillis)` xóa mọi file cũ trong thư mục (ảnh chưa lưu của lần trước, kể cả file sót lại sau khi app bị đóng) rồi trả `capture_<nowMillis>.jpg`. Lớp thuần JVM, test bằng `TemporaryFolder`.

**Bàn giao ảnh cho T-016 (`capture/CapturedPhotoHolder.kt`).**

```kotlin
data class CapturedPhoto(val file: File, val lens: Lens, val timer: TimerSetting, val capturedAtMillis: Long)

@Singleton
class CapturedPhotoHolder @Inject constructor() {
    val latest: StateFlow<CapturedPhoto?>
    fun put(photo: CapturedPhoto)
    fun clear()
}
```

Màn xem lại nằm trong cùng module (spec camera, ticket T-016), nên không cần module core. Ảnh là file JPEG trong cache của app, không phải `Bitmap` trong bộ nhớ. T-016 đọc `latest`, lưu vào MediaStore, gọi `clear()` khi bỏ ảnh. `clear()` chỉ xóa tham chiếu; file cũ bị `CaptureFileStore` dọn ở lần chụp sau. Holder sống theo tiến trình: app bị hệ thống đóng khi đang ở màn xem lại thì ảnh chưa lưu mất (T-016 quyết định có cần giữ lâu hơn không, mục 8 câu 8).

**Lưu công cụ (`data/`).** `model/CameraTools.kt`: `enum class FlashMode { OFF, ON, AUTO }` có `next()`; `enum class TimerSetting(val seconds: Int) { OFF(0), THREE(3), TEN(10) }` có `next()`; `enum class Lens { BACK, FRONT }`; `data class CameraSettings(flash = OFF, timer = OFF, gridVisible = false, lens = BACK)`. `CameraSettingsRepository`: `val settings: Flow<CameraSettings>`, `suspend fun setFlash`, `setTimer`, `setGridVisible`, `setLens` (ném `IOException`). `DataStoreCameraSettingsRepository` dùng `DataStore<Preferences>` chung của `:core:data`, khóa `camera_flash` (chuỗi tên enum), `camera_timer` (số giây), `camera_grid` (boolean), `camera_lens` (chuỗi tên enum); giá trị lạ về mặc định; đọc lỗi `IOException` thì thử lại như `DataStoreCounterRepository`. Lưới mặc định tắt (spec không nói, mục 8 câu 5).

**Trạng thái màn hình (`CameraUiState.kt`).**

```kotlin
sealed interface CameraUiState {
    data object Starting : CameraUiState
    data object PermissionDenied : CameraUiState
    data object Error : CameraUiState
    data class Ready(
        val flash: FlashMode,
        val flashAvailable: Boolean,
        val timer: TimerSetting,
        val gridVisible: Boolean,
        val lens: Lens,
        val zoomLevels: List<Float>,
        val zoom: Float,
        val countdown: Int? = null,
        val capturing: Boolean = false,
    ) : CameraUiState
}
sealed interface CameraEvent { data object RequestPermission : CameraEvent; data object PhotoCaptured : CameraEvent }
```

Trạng thái chờ hộp hỏi quyền hiện như `Starting`. Thiết kế không có thông báo cho lỗi lưu công cụ hay lỗi chụp, nên hai lỗi này chỉ ghi log và không đổi trạng thái (mục 8 câu 4).

**`CameraViewModel`** (`@HiltViewModel`) nhận `CameraController`, `CameraSettingsRepository`, `CapturedPhotoHolder`, `CountdownInterruptions`, `Analytics`, `AppLogger`, `SavedStateHandle`, và `clock: () -> Long` qua một `@Provides` (test truyền giá trị cố định). Không nhận `FeatureFlags`: cờ chỉ quyết định đích điều hướng trong `:app`, màn camera chỉ mở được khi cờ bật. `uiState: StateFlow<CameraUiState>`, `events: Flow<CameraEvent>` (`Channel(BUFFERED).receiveAsFlow()`), `surfaceRequest` lấy từ controller. Hàm cho người dùng và vòng đời:

- `onScreenStarted(permissionGranted: Boolean)`: đọc `settings.first()` một lần mỗi ViewModel. Có quyền thì `openCamera()`. Không có quyền: nếu `askPermission` (đọc `savedStateHandle["askPermission"] ?: true`) và chưa hỏi ở lần vào này (`savedStateHandle["permission_requested"]`) thì đặt cờ đã hỏi, ở `Starting`, phát `RequestPermission`; ngược lại `PermissionDenied`.
- `onPermissionResult(granted)`: ghi `camera_permission_result`, log, rồi `openCamera()` hoặc `PermissionDenied`.
- `openCamera()`: `Starting`, `controller.open(lens, flash)`. Thành công: `Ready` với `zoomLevels` lọc từ `[0.5f, 1f, 2f]` theo `CameraCapabilities`, `zoom = 1f`, `flashAvailable = hasFlash`. Lỗi `CameraOpenException`: log `error`, `Error`.
- `onRetry()`: log "Retrying camera open", `openCamera()`.
- `onScreenStopped()`: hủy đếm ngược (log lý do `stopped`), `controller.close()`. Trạng thái giữ nguyên để lần `onScreenStarted` sau mở lại với cùng công cụ.
- `onFlashClick()`: bỏ qua khi không `Ready` hoặc `!flashAvailable`; `flash.next()`, `controller.setFlash`, lưu.
- `onTimerClick()`, `onGridClick()`: đổi và lưu.
- `onFlipClick()`: đổi `lens`, lưu, hủy đếm ngược nếu có, mở lại camera (giữ `Ready` cũ đến khi có `CameraCapabilities` mới, zoom về 1x; lỗi thì `Error`).
- `onZoomSelect(ratio)`: bỏ qua mức không có trong `zoomLevels`; `controller.setZoom`, cập nhật `zoom`.
- `onShutterClick()`: bỏ qua khi không `Ready`, đang chụp hoặc đang đếm. Hẹn giờ tắt thì `capture()`. Hẹn giờ bật thì chạy `countdownJob`: `countdown = n`, `delay(1_000)` cho `n` từ `timer.seconds` xuống 1, rồi `countdown = null` và `capture()`.
- `onCancelCountdown()`: hủy `countdownJob`, `countdown = null`, log lý do `user`. Không chụp, không ghi sự kiện.
- `init`: thu `countdownInterruptions.active`; khi `true` mà đang đếm thì hủy như trên, log lý do `interruption`.
- `capture()`: `capturing = true`; `controller.takePicture()`; thành công thì `holder.put(CapturedPhoto(file, lens, timer, clock()))`, ghi `photo_captured`, log, phát `PhotoCaptured`; `CaptureFailedException` thì log `warn`. Cuối cùng `capturing = false`.
- Lưu công cụ: `viewModelScope.launch { try { repository.setX(...) } catch (e: IOException) { logger.warn(...) } }`. Lựa chọn vẫn có hiệu lực trong phiên dùng.

**Ngắt đếm ngược (`CountdownInterruptions.kt`).** `fun interface CountdownInterruptions { fun active(): Flow<Boolean> }`. `di/CameraModule.kt` cung cấp:

- Phương án A, khi `core/network/.../NetworkStatus.kt` có trên nhánh chính lúc bắt đầu task và có `val dialogShown: StateFlow<Boolean>` (plan T-011, mục 3.1): `CountdownInterruptions { networkStatus.dialogShown }`, thêm `implementation(project(":core:network"))` vào `feature/camera/build.gradle.kts`.
- Phương án B, ngược lại: `CountdownInterruptions { flowOf(false) }`, ghi "nối hộp thoại mất mạng" vào mục Việc còn lại của nhật ký.

Cả hai chỉ khác hai file đã có trong danh sách. ViewModel test dùng `MutableStateFlow` giả, nên test như nhau.

**Màn hình (`CameraScreen.kt`, `CameraControls.kt`).** `CameraRoute(onPhotoCaptured, onOpenPoses: (() -> Unit)?, onOpenSettings: (() -> Unit)?, viewModel = hiltViewModel())`:

- Bọc nội dung trong `AppTheme(darkTheme = true)`: camera luôn tối (spec).
- `LifecycleStartEffect`: khi start gọi `onScreenStarted(ContextCompat.checkSelfPermission(context, CAMERA) == GRANTED)`, khi stop gọi `onScreenStopped()`.
- `rememberLauncherForActivityResult(RequestPermission())` chạy khi nhận `RequestPermission`; kết quả gọi `onPermissionResult`. `PhotoCaptured` gọi `onPhotoCaptured`.
- "Mở cài đặt" mở `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))`.
- Khung xem trước: slot `preview` truyền `CameraXViewfinder(surfaceRequest)` khi `surfaceRequest != null`.

`CameraScreen(uiState, onFlashClick, onTimerClick, onGridClick, onFlipClick, onZoomSelect, onShutterClick, onCancelCountdown, onRetry, onOpenAppSettings, onOpenPoses: (() -> Unit)?, onOpenSettings: (() -> Unit)?, modifier, preview: @Composable (Modifier) -> Unit = {})` không trạng thái. Test và screenshot không truyền `preview`, nên khung chỉ có nền `camera/surface`. Bố cục cột (số liệu `context/`, khung 411 x 891; tính từ mép trên khung vì test không có thanh trạng thái):

| Phần | Thành phần, màu | Kích thước, khoảng cách |
| --- | --- | --- |
| Nền | `cameraBackground` | Cả màn, `testTag(SCREEN)` |
| Thanh trên | Trái: `CameraIconButton` `grid_view` (ẩn khi `onOpenPoses == null`). Phải, cách nhau `Spacing.sm`: khi `Ready` là flash, hẹn giờ, lưới, rồi cài đặt; trạng thái khác chỉ có cài đặt (ẩn khi `onOpenSettings == null`) | Lề ngang `Spacing.md`, lề trên `Spacing.xl` (32), cao 48 |
| Khoảng trống | | `Spacing.sm + Spacing.xs` (12) |
| Khung xem trước | `Box` trong vùng `weight(1f)`, căn giữa trên, `aspectRatio(3f / 4f, matchHeightConstraintsFirst = true)`. Nền `cameraSurface` khi `Ready`, `cameraBackground` ở trạng thái khác | Khung 891: 411 x 547 (thiết kế 548, lệch 1 dp do làm tròn đáy) |
| Trong khung | `Ready`: `preview`, `GridOverlay` khi `gridVisible`, `ZoomControl` giữa đáy cách đáy `Spacing.md`, khi đếm ngược thêm lớp `cameraScrim` độ mờ 35% và vòng đếm. `Starting`: `CircularProgressIndicator` 40 dp màu `primary` và chữ `bodyMedium` màu `onCamera` cách `Spacing.md`, căn giữa. `PermissionDenied`, `Error`: `StateMessage` căn giữa | Vòng đếm 128, nền `secondaryContainer`, viền `Stroke.sticker` màu `onSecondaryContainer`, số `displayLarge` màu `onSecondaryContainer` |
| Chỗ của chip và dải tư thế | Slot rỗng cho T-014 | Cao cố định 132 (16 + 36 + 16 + 64, số liệu `context/`) |
| Khoảng trống | | `Spacing.sm + Spacing.xs` (12) |
| Hàng nút chụp | `Row` ba ô `weight(1f)`, nội dung căn giữa (tâm 68.5, 205.5, 342.5 khớp thiết kế 68, 205.5, 343). Trái: ô ảnh vừa chụp 48, nền `cameraSurface`, viền `Stroke.sticker` màu `onCamera`, bo `Radius.thumb`, không bấm được. Giữa: nút chụp. Phải: `CameraIconButton` `flip_camera_android` (mờ khi không `Ready`) | Cao 76; lề dưới `Spacing.xl` (32, thiết kế 31) |

Nút chụp (`ShutterButton`, riêng của feature vì README không coi là thành phần chung): vòng 76 viền 4 màu `onCamera`, tròn trong 62 màu `onCamera`; khi đếm ngược hiện icon `close` 28 màu `cameraOnControl` và bấm thì gọi `onCancelCountdown`; khi không `Ready` hoặc đang chụp thì `enabled = false`, độ mờ 38% (ảnh thiết kế cho thấy nút xám; `context/` không xuất độ mờ của lớp). Chữ trong khung 360 x 640: khung xem trước co còn 223 x 297, các hàng khác giữ cỡ (README, mục 6).

Mức zoom hiển thị: mức đang chọn có hậu tố "x" ("1x"), mức khác là số trần ("0.5", "2"), đúng bảng chữ. Số định dạng không theo locale (`0.5`, không phải `0,5`).

**Thành phần mới trong `:core:designsystem`** (`component/`, theo `sticker.md` mục 4 và bảng ánh xạ của README):

- `CameraIconButton(onClick, contentDescription: String?, modifier, active = false, enabled = true, content: @Composable () -> Unit)` và hai hàm tiện dùng `CameraIconButton(icon: Painter, ...)`, `CameraIconButton(text: String, ...)`. Tròn 48, viền `Stroke.sticker` màu `cameraOnControl`. Thường: nền `cameraControl`, nội dung `cameraOnControl`. `active`: nền `secondaryContainer`, nội dung `onSecondaryContainer`. `enabled = false`: cả nút độ mờ 38%, không bấm được. Icon 24, chữ `titleMedium`. Ngữ nghĩa: `Role.Button`, `selected = active`.
- `GridOverlay(modifier, color = appColors.onCamera, alpha = GridOverlayDefaults.Alpha)`: `Canvas` vẽ hai đường dọc ở 1/3 và 2/3 chiều rộng, hai đường ngang ở 1/3 và 2/3 chiều cao, nét `Stroke.thin` (1 dp). `Alpha` theo mục 8 câu 1 (đề xuất 0.18 theo README).
- `ZoomControl(levels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier)`: nhận nhãn đã định dạng, để thành phần không phụ thuộc cách đặt tên mức zoom. Viên thuốc bo `Radius.full`, nền `cameraScrim` độ mờ 45%, lề trong `Spacing.xs`, các nút cách `Spacing.xs`. Mỗi nút tròn 40, chữ `labelLarge`; nút chọn nền `secondaryContainer` chữ `onSecondaryContainer`, nút khác chữ `onCamera`. Vùng chạm cao 48 (viên thuốc), rộng 44 mỗi nút; giữ đúng thiết kế (mục 8 câu 9).
- `StateMessage(icon: Painter, title: String, message: String, modifier, actionLabel: String? = null, actionIcon: Painter? = null, onAction: (() -> Unit)? = null)`: `Column` rộng tối đa 331, lề dọc `Spacing.lg`, các phần cách `Spacing.md`, căn giữa. Vòng 72 nền `secondaryContainer` viền `Stroke.sticker` màu `outline`, icon 36 màu `onSecondaryContainer`; tiêu đề `titleLarge` màu `onSurface`; lời `bodyMedium` màu `onSurfaceVariant`; `PrimaryButton(actionLabel, onAction, icon = actionIcon)` khi có `actionLabel`. Chữ căn giữa. Thành phần dùng màu Material nên dùng được cả ngoài camera (thư viện tư thế, T-021).
- Catalog: trang `CameraComponentCatalog` (nền `cameraBackground`): `CameraIconButton` thường, đang bật, chữ "3s", mờ; `ZoomControl` ba mức và hai mức; `GridOverlay` trong khung 200 x 267; `StateMessage` có nút. Screenshot test mới `cameraComponents_dark`.

**Biểu tượng.** 12 vector drawable trong `feature/camera/src/main/res/drawable/`, lấy từ Material Symbols Outlined (Apache 2.0, repo `google/material-design-icons`, thư mục `symbols/web/<tên>/materialsymbolsoutlined/<tên>_24px.svg`), đổi sang `<vector>` 24 dp, màu lấy từ `tint`. Chú thích đầu mỗi file ghi tên biểu tượng, nguồn và commit SHA đã tải. Giấy phép chép vào `feature/camera/third_party/material-symbols/LICENSE.txt`. Tải không được là lỗi hạ tầng (auto-run.md, bước 5).

**Điều hướng và cờ.**

- `FeatureFlag.CAMERA("feature_camera_enabled")`, thêm vào `StagingFeatureFlagsModule`.
- `navigation/CameraNavigation.kt`: `@Serializable data class CameraDestination(val askPermission: Boolean = true)`; `fun NavGraphBuilder.cameraScreen(onPhotoCaptured: () -> Unit, onOpenPoses: (() -> Unit)? = null, onOpenSettings: (() -> Unit)? = null)`.
- Cắm vào cơ chế chọn màn chính của T-010 (plan T-010, mục 3.1, phần "`:app`: chọn màn chính sau loading" và "T-012 và T-013 cắm vào thế nào"); T-013 không tạo cách chọn đích riêng:
  - `navigation/MainScreenChooser.kt`: constructor nhận `FeatureFlags` (`class MainScreenChooser @Inject constructor(private val featureFlags: FeatureFlags)`); `inputs()` đặt `cameraAvailable = featureFlags.enabled(FeatureFlag.CAMERA).first()`, giữ nguyên hai trường của màn chào (`false`, hoặc giá trị T-012 đã đặt nếu T-012 merge trước). Đọc giá trị đầu bằng `first()`, không theo dõi luồng: lựa chọn chỉ có nghĩa đúng lúc rời loading, `choose()` là hàm `suspend` gọi một lần khi loading xong, và loading đã chờ Remote Config tối đa 15 giây trước đó (ADR-1001), nên giá trị đầu đã là giá trị mới nhất. Đây cũng là cách T-010 chỉ định cho T-012 và T-013. Ở staging, cờ bật cục bộ (`flowOf(true)`) nên `first()` trả ngay.
  - `navigation/MainScreen.kt`: không đổi `chooseMainScreen` (nhánh `cameraAvailable` thì `CAMERA` đã có từ T-010).
  - `navigation/AppNavHost.kt`: trong `MainScreen.destination()`, nhánh `CAMERA -> CameraDestination()` thay cho `SampleDestination`, bỏ chú thích "T-013 thay bằng ..."; đăng ký `cameraScreen(onPhotoCaptured = {})` (T-016 nối màn xem lại; T-021, T-025 nối hai nút trên). Giữ nguyên phần chặn điều hướng hai lần, `popUpTo`, và mọi thứ của T-011.
  - `MainActivity.kt`: không đổi. T-010 đã tiêm `MainScreenChooser` và truyền `mainScreenChooser::choose`; Hilt tự cung cấp `FeatureFlags` cho constructor mới.
- `AndroidManifest.xml`: `android:screenOrientation="portrait"` trên `<activity android:name=".MainActivity">`. Không thêm quyền.

**Thứ tự làm.** Bước 3: tạo module, khung rỗng (mục 4), mọi test, `strings.xml` đủ chữ; chạy để thấy thất bại đúng lý do; commit. Bước 4: thành phần core và catalog; repository; `CaptureFileStore`, holder; ViewModel; màn hình; `CameraXController`; biểu tượng; điều hướng, cờ, manifest; `scripts/check.sh` ghi ảnh chuẩn mới; build `stagingDebug`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `settings.gradle.kts` | `include(":feature:camera")` |
| Tạo | `feature/camera/build.gradle.kts` | Plugin `template.android.feature`, namespace, `:core:data`, bốn thư viện CameraX; `:core:network` theo phương án A |
| Tạo | `feature/camera/consumer-rules.pro` | Rỗng, như các feature khác |
| Tạo | `feature/camera/third_party/material-symbols/LICENSE.txt` | Giấy phép Apache 2.0 của Material Symbols |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | `CameraRoute`, `CameraScreen`, `CameraTestTags` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraControls.kt` | Thanh trên, `ShutterButton`, ô ảnh vừa chụp, vòng đếm ngược, định dạng nhãn zoom |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraUiState.kt` | `CameraUiState`, `CameraEvent` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModel.kt` | ViewModel theo mục 3.1 |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CountdownInterruptions.kt` | `fun interface CountdownInterruptions` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/model/CameraTools.kt` | `FlashMode`, `TimerSetting`, `Lens`, `CameraSettings` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/data/CameraSettingsRepository.kt` | Interface repository |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/data/DataStoreCameraSettingsRepository.kt` | Cài đặt bằng DataStore, bốn khóa `camera_*` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraController.kt` | Interface, `CameraCapabilities`, hai lớp lỗi |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraXController.kt` | Cài đặt bằng CameraX |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CaptureFileStore.kt` | Thư mục ảnh tạm, dọn ảnh cũ |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/capture/CapturedPhotoHolder.kt` | `CapturedPhoto`, `CapturedPhotoHolder` (bàn giao cho T-016) |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/di/CameraModule.kt` | Gắn repository, `CaptureFileStore`, đồng hồ, `CountdownInterruptions` (phương án A hoặc B) |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/di/CameraControllerModule.kt` | `@InstallIn(ViewModelComponent::class)`, gắn `CameraXController` `@ViewModelScoped` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/navigation/CameraNavigation.kt` | `CameraDestination`, `cameraScreen()` |
| Tạo | `feature/camera/src/main/res/values/strings.xml` | Chữ theo bảng chữ của thiết kế và mô tả cho trình đọc màn hình (mục 8 câu 10) |
| Tạo | `feature/camera/src/main/res/drawable/ic_camera_grid_view.xml`, `ic_camera_settings.xml`, `ic_camera_flash_off.xml`, `ic_camera_flash_on.xml`, `ic_camera_flash_auto.xml`, `ic_camera_timer.xml`, `ic_camera_grid_on.xml`, `ic_camera_flip.xml`, `ic_camera_close.xml`, `ic_camera_no_photography.xml`, `ic_camera_error.xml`, `ic_camera_refresh.xml` | 12 biểu tượng Material Symbols |
| Tạo | `feature/camera/src/test/resources/robolectric.properties` | `sdk=35`, như các feature khác |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/MainDispatcherRule.kt` | Chép từ `:feature:sample` |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraFakes.kt` | `FakeCameraController`, `FakeCameraSettingsRepository`, `FakeAnalytics`, `FakeCountdownInterruptions` |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModelTest.kt` | Unit test ViewModel (mục 4) |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/data/DataStoreCameraSettingsRepositoryTest.kt` | Unit test repository |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CaptureFileStoreTest.kt` | Unit test thư mục ảnh tạm |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenTest.kt` | Test giao diện Robolectric |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenScreenshotTest.kt` | Bảy screenshot test |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_starting_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_permission-denied_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_error_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_countdown_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế (chưa có phần của T-014, T-015) |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_ready_dark.png` | Đủ công cụ: flash tự động, hẹn giờ 10s, lưới tắt, zoom 1x và 2 |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_permission-denied_small.png` | Khung 360 x 640 (L-028) |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_countdown_small.png` | Khung 360 x 640 (L-028) |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/CameraIconButton.kt` | Thành phần mới |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/GridOverlay.kt` | Thành phần mới, `GridOverlayDefaults` |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/ZoomControl.kt` | Thành phần mới |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/StateMessage.kt` | Thành phần mới |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/catalog/DesignSystemCatalog.kt` | Thêm `CameraComponentCatalog`; trang cũ không đổi |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/CameraComponentsTest.kt` | Test giao diện bốn thành phần (file riêng, không chạm `ComponentsTest.kt` mà T-007, T-011 sửa) |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignSystemCatalogScreenshotTest.kt` | Thêm test `cameraComponents_dark` |
| Tạo | `core/designsystem/src/test/screenshots/DesignSystem_camera_components_dark.png` | Ảnh chuẩn mới |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FeatureFlag.kt` | `CAMERA("feature_camera_enabled")` |
| Sửa | `app/build.gradle.kts` | `implementation(project(":feature:camera"))` |
| Sửa | `app/src/main/AndroidManifest.xml` | `android:screenOrientation="portrait"` cho `MainActivity` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/AppNavHost.kt` | `CAMERA -> CameraDestination()` trong `MainScreen.destination()`, đăng ký `cameraScreen` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/MainScreenChooser.kt` | Nhận `FeatureFlags`; `cameraAvailable` đọc `FeatureFlag.CAMERA` bằng `first()` |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingFeatureFlagsModule.kt` | Bật `FeatureFlag.CAMERA` ở staging |
| Sửa | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/navigation/MainScreenTest.kt` | Hai test mới của `MainScreenChooser` theo cờ camera; test `chooserBeforeOnboardingAndCamera_choosesSample` của T-010 đổi cách dựng chooser (mục 4) |
| Sửa | `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt` | Chờ `camera_screen` thay cho `sample_screen`, giữ 60 giây của T-010 |

Không file nào khác. Đặc biệt không sửa `gradle/libs.versions.toml` (bốn thư viện CameraX đã có), `build-logic/`, `ComponentsTest.kt`, `docs/design-system/tokens.json` (không thêm giá trị thiết kế mới), code của `:feature:loading` và `:feature:sample`.

**Nơi khác đọc các file trên** (L-020), không cần sửa:

- `FeatureFlag.kt`: `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/DefaultFeatureFlagsTest.kt` duyệt `FeatureFlag.entries`; cờ mới mặc định tắt nên test `everyFlag_isOffByDefault` tự kiểm tra và vẫn qua.
- `DesignSystemCatalog.kt`: chỉ `DesignSystemCatalogScreenshotTest.kt` (trong danh sách) gọi; các trang cũ không đổi nên sáu ảnh catalog cũ không đổi.
- `AppNavHost.kt`, `MainScreenChooser.kt`: `AppLaunchTest.kt` (trong danh sách) mở `MainActivity`; `MainScreenTest.kt` (trong danh sách) gọi `chooseMainScreen` và `MainScreenChooser`. `MainScreen.kt` không đổi. `MainActivity.kt` dựng `MainScreenChooser` qua Hilt và không đổi: constructor mới chỉ thêm một phụ thuộc Hilt đã có (`FeatureFlags` của `:core:config`). Phần T-011 thêm vào `AppNavHost.kt` (`onPastLoadingChanged`) giữ nguyên.
- `settings.gradle.kts`, `app/build.gradle.kts`: kiểm tra quy tắc phụ thuộc module (`checkModuleDependencies`) đọc phụ thuộc lúc build; `:feature:camera` chỉ phụ thuộc core nên qua. `scripts/release.sh` chỉ đọc `applicationId`, `versionCode`, `versionName` trong `app/build.gradle.kts`; T-013 chỉ thêm một dòng `implementation` trong `dependencies`, không đổi ba giá trị đó và không chạm script.
- `AndroidManifest.xml`: chỉ thêm thuộc tính, không thêm quyền. `scripts/app-explore/apk-screens.py` đọc manifest của APK app mẫu, không phải của repo.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: `feature_camera_enabled` (spec mục 8), mặc định tắt, bật sẵn ở staging. Cờ quyết định đích sau loading trong `:app`, theo dõi bằng `collectAsState` trên `FeatureFlags.enabled()`.
- Quảng cáo: Không chạm. Camera không có vị trí quảng cáo (spec mục 5).
- Sự kiện phân tích (điểm chặn 4, đã có trong spec mục 6):
  - `camera_permission_result`: `result` là `granted` hoặc `denied`, `source` là `camera`.
  - `photo_captured`: `pose_id` là chuỗi rỗng (chưa có tư thế, theo ticket), `timer` là `off`, `3` hoặc `10`, `matched` là `false` (chưa có điểm khớp; định dạng `true`/`false`, mục 8 câu 3).
- Dữ liệu cục bộ: bốn khóa DataStore mới `camera_flash`, `camera_timer`, `camera_grid`, `camera_lens` (spec mục 6, dòng "Hẹn giờ, lưới, flash, camera trước sau"). Khóa mới, không chuyển đổi dữ liệu cũ, nên không chạm điểm chặn 9. Ảnh chụp nằm tạm trong cache của app, không rời máy.
- File R2: Không chạm.

## 4. Kiểm thử

Mọi test dưới đây vào commit test của bước 3, cùng khung rỗng để biên dịch được: bốn thành phần core vẽ `Box(modifier)` rỗng; `CameraComponentCatalog` là `Box` rỗng; `CameraViewModel` có đủ hàm nhưng không làm gì, `uiState` luôn `Starting`, `events` rỗng; `DataStoreCameraSettingsRepository` trả `CameraSettings()` và không ghi; `CaptureFileStore.newFile` trả file mới mà không xóa gì; `CapturedPhotoHolder.put` không làm gì; `CameraScreen` chỉ vẽ `Box` mang `testTag(SCREEN)`; `MainScreenChooser` đã nhận `FeatureFlags` nhưng `inputs()` vẫn đặt `cameraAvailable = false`; `MainScreen.destination()` vẫn trả `SampleDestination` cho `CAMERA`. `strings.xml` đủ chữ ngay ở bước 3. Lý do thất bại ở cột cuối là điều AI dự kiến; bước 3 chạy và ghi lý do thật vào nhật ký.

Bản giả: `FakeCameraController` ghi lại `openCalls: List<Lens>`, `flashCalls`, `zoomCalls`, `closeCount`, `captureCount`; `capabilities` đặt được; `openError`, `captureError` đặt được; `takePicture` trả `File("capture_1.jpg")`. `FakeCameraSettingsRepository` có `MutableStateFlow<CameraSettings>`, ghi lại lần lưu, `failWrites` ném `IOException`. `FakeCountdownInterruptions` là `MutableStateFlow(false)`. ViewModel dựng với `SavedStateHandle()` (hoặc có `"askPermission" to false`), `clock = { 1_000L }`. Kiểm tra lỗi so loại (`assertTrue(entry.error is IOException)`).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-8, mở camera khi có quyền | Unit test | `CameraViewModelTest.started_withPermission_opensBackCamera_thenReady`: `onScreenStarted(true)`; `openCalls == [BACK]`; trạng thái `Ready(flash = OFF, flashAvailable = true, timer = OFF, gridVisible = false, lens = BACK, zoomLevels = [1f, 2f], zoom = 1f)` với `capabilities(min = 1f, max = 8f)`. Thất bại: khung rỗng ở `Starting` |
| KB-8, không mở được và thử lại | Unit test | `openFails_showsError_thenRetryOpensAgain`: `openError` đặt; `Error`, có log `ERROR` thẻ `Camera`; bỏ `openError`, `onRetry()`; `Ready`, `openCalls.size == 2`, có log "Retrying camera open". Thất bại: `Starting` |
| KB-8, ra nền rồi quay lại | Unit test | `stoppedThenStarted_closesThenReopensWithSameTools`: `Ready`, bật lưới, hẹn giờ 3; `onScreenStopped()`: `closeCount == 1`; `onScreenStarted(true)`: `openCalls.size == 2`, `Ready` giữ lưới và hẹn giờ 3. Thất bại: không gọi `close` |
| KB-7, hỏi quyền khi mở | Unit test | `started_withoutPermission_requestsOnce`: `onScreenStarted(false)`; `events` phát đúng một `RequestPermission`, trạng thái `Starting`; gọi `onScreenStarted(false)` lần nữa (quay lại từ nền): không phát thêm, trạng thái `PermissionDenied`. Thất bại: không có sự kiện |
| KB-7, không hỏi lại ngay sau màn chào | Unit test | `askPermissionFalse_showsDeniedWithoutRequest`: `SavedStateHandle(mapOf("askPermission" to false))`, `onScreenStarted(false)`; `PermissionDenied`, không có sự kiện |
| KB-7, cho phép | Unit test | `permissionGranted_logsEvent_andOpens`: `onPermissionResult(true)`; `analytics.events == [AnalyticsEvent("camera_permission_result", mapOf("result" to "granted", "source" to "camera"))]`; `Ready`. Thất bại: danh sách rỗng |
| KB-7, từ chối | Unit test | `permissionDenied_logsEvent_andShowsDenied`: `onPermissionResult(false)`; sự kiện với `"result" to "denied"`; `PermissionDenied`; `openCalls` rỗng |
| KB-7, quay lại từ cài đặt | Unit test | `deniedThenStartedWithPermission_opensWithoutNewEvent`: từ `PermissionDenied`, `onScreenStarted(true)`; `Ready`; `analytics.events` chỉ có một sự kiện của lần từ chối trước |
| KB-4, flash | Unit test | `flash_cyclesOffOnAuto_andIsSaved`: ba lần `onFlashClick()`; `flash` lần lượt `ON`, `AUTO`, `OFF`; `flashCalls == [ON, AUTO, OFF]`; repository lưu `[ON, AUTO, OFF]` |
| KB-4, flash mờ khi không có đèn | Unit test | `flash_unavailable_ignoresClicks`: `capabilities(hasFlash = false)`; `flashAvailable == false`; `onFlashClick()` không đổi `flash`, `flashCalls` rỗng |
| KB-4, hẹn giờ | Unit test | `timer_cyclesOffThreeTen_andIsSaved`: `THREE`, `TEN`, `OFF`; lưu đúng thứ tự |
| KB-4, lưới | Unit test | `grid_toggles_andIsSaved`: `true`, rồi `false` |
| KB-4, đổi camera | Unit test | `flip_switchesLens_resetsZoom_keepsTools`: `onZoomSelect(2f)`, `onFlipClick()`; `openCalls == [BACK, FRONT]`; `lens = FRONT`, `zoom = 1f`; hẹn giờ và lưới giữ; repository lưu `FRONT` |
| KB-4, zoom có góc rộng | Unit test | `zoomLevels_withUltraWide_include05`: `capabilities(min = 0.5f, max = 10f)`; `zoomLevels == [0.5f, 1f, 2f]`; `onZoomSelect(0.5f)`: `zoomCalls == [0.5f]`, `zoom = 0.5f` |
| KB-4, zoom không có góc rộng | Unit test | `zoomLevels_withoutUltraWide_are1And2`: `min = 1f`, `max = 4f`: `[1f, 2f]`; `onZoomSelect(0.5f)` bị bỏ qua. `zoomLevels_maxBelow2_onlyOne`: `max = 1.5f`: `[1f]` |
| KB-4, giữ lựa chọn khi mở lại, zoom về 1x | Unit test | `savedTools_restored_zoomBackTo1x`: repository có `CameraSettings(AUTO, TEN, true, FRONT)`; `openCalls == [FRONT]`, `Ready` có đủ bốn giá trị, `zoom = 1f` |
| Lưu lỗi không làm app dừng | Unit test | `saveFails_keepsChoice_andLogsWarning`: `failWrites = true`, `onGridClick()`; `gridVisible == true`; có log `WARN` với `error is IOException` |
| KB-6, chụp ngay khi hẹn giờ tắt | Unit test | `shutter_timerOff_capturesAtOnce`: `onShutterClick()`; `captureCount == 1`; `analytics.events` có `AnalyticsEvent("photo_captured", mapOf("pose_id" to "", "timer" to "off", "matched" to "false"))`; `holder.latest.value == CapturedPhoto(File("capture_1.jpg"), BACK, OFF, 1_000L)`; `events` phát `PhotoCaptured` |
| KB-6, đếm ngược rồi chụp | Unit test | `shutter_timer3_countsDownThenCaptures`: hẹn giờ 3, `onShutterClick()`; `countdown == 3`; `advanceTimeBy(1_000)`, `runCurrent()`: 2; thêm 1 000: 1; thêm 999: `captureCount == 0`; thêm 1: `countdown == null`, `captureCount == 1`, `timer` của sự kiện là `"3"` |
| KB-6, hủy đếm ngược | Unit test | `cancelCountdown_stopsWithoutCapture`: đếm 10; sau 2 000 ms `onCancelCountdown()`; chạy thêm 20 s; `captureCount == 0`, `countdown == null`, không có `photo_captured` |
| KB-6, ra nền khi đang đếm | Unit test | `stopped_duringCountdown_cancels`: đếm 3, `onScreenStopped()`, chạy thêm 5 s; không chụp |
| Dừng đếm khi có hộp thoại mất mạng | Unit test | `interruption_duringCountdown_cancels`: đếm 10; `interruptions.value = true`; chạy thêm 20 s; không chụp; log có lý do `interruption`. `interruption_whenIdle_changesNothing`: `true` khi không đếm, rồi bấm chụp với hẹn giờ tắt vẫn chụp |
| Chụp một lần dù bấm hai lần | Unit test | `shutter_doubleTap_capturesOnce`: controller giả treo `takePicture` bằng `CompletableDeferred`; bấm hai lần; `captureCount == 1`; `capturing == true` cho tới khi hoàn tất |
| Chụp lỗi | Unit test | `captureFails_staysReady_withoutEvent`: `captureError` đặt; `Ready`, `capturing == false`, không có `photo_captured`, có log `WARN` |
| Nút chụp chỉ chạy khi `Ready` | Unit test | `shutter_ignoredWhenNotReady`: trạng thái `PermissionDenied`, `onShutterClick()`; `captureCount == 0` |
| KB-4, giá trị lưu và mặc định | Unit test | `DataStoreCameraSettingsRepositoryTest` (DataStore trong bộ nhớ, như `DataStoreCounterRepositoryTest`): `nothingStored_returnsDefaults` (`OFF`, `OFF`, `false`, `BACK`); `eachSetter_isReadBack`; `unknownStoredValue_fallsBackToDefault` (`camera_flash = "BLINK"`, `camera_timer = 7` thành `OFF`); `readFailsOnce_recovers`. Thất bại: khung rỗng không ghi |
| Ảnh tạm không chất đống | Unit test | `CaptureFileStoreTest.newFile_deletesPreviousCaptures`: tạo hai file trong thư mục tạm, `newFile(5)`; thư mục chỉ còn đường dẫn mới `capture_5.jpg` (chưa tồn tại) và hai file cũ đã xóa. `newFile_createsMissingDirectory` |
| Camera là màn chính sau loading khi cờ bật | Unit test | `MainScreenTest.chooser_cameraFlagOn_choosesCamera`: `runTest { assertEquals(MainScreen.CAMERA, MainScreenChooser(FakeFeatureFlags(setOf(FeatureFlag.CAMERA))).choose()) }`. `chooser_cameraFlagOff_choosesSample`: không cờ nào bật thì `SAMPLE`. `FakeFeatureFlags` (trả `flowOf(flag in enabled)`) đặt trong `MainScreenTest.kt`. Thất bại đúng lý do: khung rỗng ở bước 3 có constructor mới nhưng `inputs()` vẫn đặt `cameraAvailable = false`, nên test đầu nhận `SAMPLE`; test thứ hai qua ngay, giữ làm test chặn, ghi vào nhật ký. Test `chooserBeforeOnboardingAndCamera_choosesSample` của T-010 đổi `MainScreenChooser()` thành `MainScreenChooser(FakeFeatureFlags(emptySet()))`, ý kiểm tra giữ nguyên; thay đổi này và `FakeFeatureFlags` vào commit test của bước 3 (L-025) |
| KB-8, trạng thái đang mở | Compose UI test | `CameraScreenTest.starting_showsSpinnerText_andDisablesShutter`: có chữ "Đang mở camera…"; nút chụp (`SHUTTER`) `assertIsNotEnabled`; không có `FLASH`, `TIMER`, `GRID_BUTTON`, `ZOOM` |
| KB-7, trạng thái chưa có quyền | Compose UI test | `permissionDenied_showsMessage_andOpensSettings`: có "Chưa có quyền dùng camera", "Cho phép camera trong Cài đặt máy để chụp theo tư thế."; bấm "Mở cài đặt" gọi `onOpenAppSettings` một lần; nút chụp không bật |
| KB-8, trạng thái lỗi | Compose UI test | `error_showsMessage_andRetries`: có "Không mở được camera", "Camera có thể đang được ứng dụng khác dùng. Đóng ứng dụng đó rồi thử lại."; bấm "Thử lại" gọi `onRetry` |
| KB-4, flash | Compose UI test | `flash_showsModeAndClicks`: `FLASH` có `contentDescription` "Flash: tự động" khi `AUTO`; bấm gọi `onFlashClick`. `flash_unavailable_isDisabled`: `flashAvailable = false` thì `assertIsNotEnabled` |
| KB-4, hẹn giờ | Compose UI test | `timer_active_showsSecondsLabel`: `THREE` hiện "3s" và `assertIsSelected`; `TEN` hiện "10s"; `OFF` không có chữ "s", `assertIsNotSelected` |
| KB-4, lưới | Compose UI test | `grid_visibleOnlyWhenOn`: `gridVisible = true` có `GRID`, nút lưới `assertIsSelected`; `false` không có `GRID` |
| KB-4, zoom | Compose UI test | `zoom_showsLevels_andSelects`: ba mức hiện "0.5", "1x", "2"; bấm "2" gọi `onZoomSelect(2f)`. `zoom_withoutUltraWide_hides05`: không có "0.5". `zoom_singleLevel_hidden`: không có `ZOOM` |
| KB-4, đổi camera | Compose UI test | `flip_clicks`: bấm `FLIP` gọi `onFlipClick` |
| KB-6, đếm ngược | Compose UI test | `countdown_showsNumber_andShutterCancels`: `countdown = 3` có `COUNTDOWN` với chữ "3"; `SHUTTER` có mô tả "Hủy đếm ngược"; bấm gọi `onCancelCountdown`, không gọi `onShutterClick` |
| KB-6, chụp | Compose UI test | `ready_shutterClicks`: `SHUTTER` mô tả "Chụp ảnh", bấm gọi `onShutterClick` |
| KB-6, ô ảnh vừa chụp trống | Compose UI test | `lastPhoto_isEmptyAndNotClickable`: `LAST_PHOTO` có mặt, `assertHasNoClickAction` |
| Nút thư viện và cài đặt chưa có đích | Compose UI test | `topButtons_hiddenWithoutCallbacks`: `onOpenPoses = null`, `onOpenSettings = null` thì không có `POSES`, `SETTINGS`; có callback thì có và bấm được |
| Màn hình thấp (L-028) | Compose UI test, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "w360dp-h640dp-xxhdpi")` | `small_countdown_shutterVisibleAndLargeEnough`: mép dưới của `SHUTTER` không quá chiều cao gốc, cao 76; `FLIP` cao 48; `PREVIEW` cao hơn 0 và tỷ lệ rộng/cao 0.75 (sai số 1 px). `small_permissionDenied_buttonVisible`: nút "Mở cài đặt" nằm trọn trong `PREVIEW` |
| Thành phần `CameraIconButton` | Compose UI test, `@GraphicsMode(NATIVE)` | `CameraComponentsTest.cameraIconButton_is48dpCircle`: `assertWidthIsEqualTo(48.dp)`, `assertHeightIsEqualTo(48.dp)`. `cameraIconButton_colors`: trong `AppTheme(darkTheme = true)`, điểm (10 dp, 24 dp) là #FFFFFF khi thường, #FFC83D khi `active`. `cameraIconButton_text_showsLabel` ("3s"). `cameraIconButton_disabled_notClickable`: bấm không gọi `onClick`, `assertIsNotEnabled`. `cameraIconButton_active_isSelected`. Thất bại: khung rỗng cao 0, không có ngữ nghĩa |
| Thành phần `GridOverlay` | Compose UI test, `@GraphicsMode(NATIVE)` | `gridOverlay_drawsThirds`: `Box(Modifier.size(300.dp, 450.dp).background(Color.Black)) { GridOverlay(color = Color.White, alpha = 1f) }`; điểm (100 dp, 50 dp), (200 dp, 50 dp), (50 dp, 150 dp), (50 dp, 300 dp) là trắng; (50 dp, 50 dp) và (150 dp, 225 dp) là đen. Thất bại: không vẽ gì |
| Thành phần `ZoomControl` | Compose UI test, `@GraphicsMode(NATIVE)` | `zoomControl_showsLabels_selects`: nhãn "0.5", "1x", "2"; nút chỉ số 1 `assertIsSelected`; bấm "2" gọi `onSelect(2)`. `zoomControl_height_is48dp`. `zoomControl_selectedFill_isSecondaryContainer`: điểm giữa nút chọn là #FFC83D |
| Thành phần `StateMessage` | Compose UI test, `@GraphicsMode(NATIVE)` | `stateMessage_showsTexts_andAction`: tiêu đề, lời, nút; bấm gọi `onAction`. `stateMessage_withoutAction_hasNoButton`. `stateMessage_width_isAtMost331dp` trong khung rộng 411 |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | Các test trạng thái ở trên so đúng chuỗi của README mục 2 (dấu ba chấm một ký tự `…`). `timer_active_showsSecondsLabel` và `zoom_showsLevels_andSelects` kiểm tra "3s", "10s", "0.5", "1x", "2" |
| Giao diện catalog | Screenshot test | `DesignSystemCatalogScreenshotTest.cameraComponents_dark` ghi `src/test/screenshots/DesignSystem_camera_components_dark.png`, khung `w411dp-h891dp-xxhdpi` của lớp. Sáu ảnh catalog cũ không đổi |
| Đúng thiết kế, bốn trạng thái của T-013 | Screenshot test, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "w411dp-h891dp-xxhdpi")`, có `onOpenPoses` và `onOpenSettings` để thanh trên giống thiết kế | `CameraScreenScreenshotTest`: `starting_dark` (`CameraScreen_starting_dark.png`), `permissionDenied_dark` (`CameraScreen_permission-denied_dark.png`), `error_dark` (`CameraScreen_error_dark.png`), `countdown_dark` (`CameraScreen_countdown_dark.png`: flash tắt, hẹn giờ 3, lưới bật, zoom 0.5/1x/2 chọn 1x, `countdown = 3`) |
| Đủ công cụ | Screenshot test | `ready_dark` (`CameraScreen_ready_dark.png`): flash tự động, hẹn giờ 10, lưới tắt, hai mức zoom |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` | `permissionDenied_small` (`CameraScreen_permission-denied_small.png`, nhiều chữ nhất), `countdown_small` (`CameraScreen_countdown_small.png`, nhiều nút nhất) |
| So với thiết kế | Rà soát độc lập (bước 6) | Phiên rà soát đặt từng ảnh chuẩn cạnh ảnh thiết kế cùng tên và xem theo mục 7 của README thiết kế. Không chạy script so sánh thiết kế của ADR-0020 ở T-013: `plan-check.py` (quy tắc 4) buộc plan nhắc tới script đó phải có ảnh chuẩn cho cả 18 ảnh thiết kế của feature, trong khi T-013 chỉ làm 4 (mục 8 câu 2). Khác biệt đã biết: vùng chip và dải tư thế trống; ảnh countdown thiếu thẻ bước, nét viền, cột công cụ nét viền |
| Camera là màn đích sau loading ở staging | Test trên emulator | `AppLaunchTest.appStarts_andReachesStartScreen` đổi tag chờ từ `sample_screen` sang `camera_screen` (`CameraTestTags.SCREEN`), giữ `waitUntil` 60 giây mà T-010 đặt và `fetchSemanticsNodes()`. Không khẳng định trạng thái nào của camera: emulator của CI có thể không có camera, và hộp hỏi quyền hệ thống phủ lên app |
| Khung xem trước chạy, đúng 3:4, không méo | Kiểm tra thủ công | Cài bản staging lên máy thật, cho phép camera: hình chạy trong khung 3:4, vật tròn không bị kéo dài; xoay người cầm máy không làm giao diện xoay (khóa dọc) |
| Flash | Kiểm tra thủ công | Phòng tối, camera sau: chế độ bật thì đèn nháy khi chụp; tự động thì nháy khi tối; tắt thì không. Đổi sang camera trước: nút flash mờ |
| Zoom 0.5x | Kiểm tra thủ công | Máy có ống kính góc rộng: có 0.5 và hình rộng ra. Máy không có (hoặc CameraX không lộ ống kính góc rộng): chỉ 1x và 2. Ghi tên máy vào báo cáo |
| Âm chụp, ảnh đúng chiều | Kiểm tra thủ công | Bấm chụp nghe âm chụp; log có `VGL.Camera` "Photo captured". Chụp khi cầm ngang: kiểm tra chiều ảnh bằng `adb shell run-as <gói> ls cache/captures` và kéo file về xem |
| KB-7 trên máy | Kiểm tra thủ công | Cài mới, vào camera: hộp hỏi quyền hiện; từ chối: màn chưa có quyền; "Mở cài đặt", cho phép, quay lại: camera tự chạy, không mở lại app |
| KB-8 trên máy | Kiểm tra thủ công | Mở một app camera khác giữ camera (hoặc chia màn hình): camera báo "Không mở được camera"; đóng app kia, "Thử lại": chạy. Về màn chính rồi quay lại: camera chạy lại, hẹn giờ và lưới giữ nguyên |
| Đếm ngược trên máy | Kiểm tra thủ công | Hẹn giờ 3: số 3, 2, 1 rồi chụp; hẹn giờ 10, bấm hủy ở giây thứ 5: không chụp. Khi T-011 đã có: tắt mạng lúc đang đếm, hộp thoại mất mạng hiện thì đếm dừng |

**Ngoại lệ viết test trước:**

- `CameraXController`: lớp mỏng gọi phần cứng qua CameraX, không chạy được trên JVM; kiểm chứng bằng kiểm tra thủ công và test emulator. Logic quyết định nằm trong ViewModel và `CaptureFileStore`, đều có test trước.
- 12 biểu tượng, `LICENSE.txt`, thuộc tính `screenOrientation` trong manifest: tài nguyên và cấu hình, không có logic.
- Nhánh `CAMERA -> CameraDestination()` và đăng ký `cameraScreen` trong `AppNavHost.kt`: điều hướng cần `LoadingRoute` với `hiltViewModel()`, không có test JVM riêng (như T-010, mục 4); kiểm chứng bằng `AppLaunchTest` trên emulator, chạy trong CI.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 4 (thêm ghi hai sự kiện `camera_permission_result`, `photo_captured`, đã có trong spec), điểm 7 (bốn thành phần mới và trang catalog trong `:core:designsystem`, cờ mới trong `:core:config`, thêm module vào `settings.gradle.kts`). Không chạm điểm 2 (CameraX đã được dev thêm vào catalog ở PR #6, plan không thêm thư viện), điểm 3 (quyền `CAMERA` đã có, chỉ thêm thuộc tính khóa chiều), điểm 6 ("Mở cài đặt" mở trang cài đặt của chính app trong máy, không phải liên kết ra ngoài hay quảng bá), điểm 9 (khóa DataStore mới, không chuyển dữ liệu cũ). Người duyệt thấy khác thì sửa `blocking_points`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Module mới và bốn module khác (`:core:designsystem`, `:core:config`, `:app`, cấu hình Gradle), khoảng 60 file |
| B. Độ rõ của yêu cầu | 2 | Spec đã xác nhận, thiết kế đã duyệt, còn vài điểm AI tự quyết: lỗi chụp không có thông báo, hỏi quyền một lần mỗi lần vào, ảnh camera trước không lật |
| C. Khả năng kiểm chứng tự động | 3 | Phụ thuộc phần cứng (khung xem trước, flash, ống kính góc rộng, âm chụp), vòng đời (ra nền, quay lại từ cài đặt), thời gian (đếm ngược). Logic có test trên JVM, phần phần cứng chỉ kiểm tra thủ công |
| D. Ảnh hưởng chéo | 2 | Sửa `AppNavHost.kt`, `MainScreenChooser.kt`, `MainScreenTest.kt`, `FeatureFlag.kt`, `settings.gradle.kts`, `app/build.gradle.kts`, `AppLaunchTest.kt` mà T-010, T-011, T-009, T-012 cũng sửa; `DesignSystemCatalog.kt` mà T-007 sửa |
| E. Độ mới của kỹ thuật | 3 | CameraX, `CameraXViewfinder` của `camera-compose` (API mới), quyền lúc chạy, `LifecycleRegistry` riêng: chưa có trong dự án |
| **Tổng** | 13 | **Mức: Cao** (chạm điểm chặn, và tổng điểm cũng từ 9 trở lên) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `:core:designsystem` (thêm bốn thành phần, một trang catalog, một ảnh chuẩn; không đổi thành phần có sẵn); `FeatureFlag.kt`; `settings.gradle.kts`; `app/build.gradle.kts`; `AppNavHost.kt`; `MainScreenChooser.kt`, `MainScreenTest.kt` của T-010; `AndroidManifest.xml` (khóa dọc áp dụng cho mọi màn của app, đúng spec câu 7); `StagingFeatureFlagsModule.kt`; `AppLaunchTest.kt` (màn đích ở staging đổi từ màn mẫu sang camera); DataStore chung (bốn khóa mới, tiền tố `camera_` không trùng khóa nào hiện có).
- Task khác trong sprint có thể xung đột:
  - T-007 (chưa merge): T-013 dựng trên `AppColors`, `Radius`, `Stroke`, `PrimaryButton` mới và sửa `DesignSystemCatalog.kt` sau T-007. Phải chạy sau T-007.
  - T-011: sửa `MainActivity.kt`, `AppNavHost.kt`, `FeatureFlag.kt`, `settings.gradle.kts`, `app/build.gradle.kts`; cung cấp `NetworkStatus` cho phương án A. Nên chạy sau T-011. T-013 không sửa `MainActivity.kt` nữa.
  - T-009: thêm `:core:poses` vào `settings.gradle.kts`, có thể thêm cờ; chỉ xung đột dòng, không xung đột logic.
  - T-010: T-013 phụ thuộc T-010 (ticket `depends_on: [T-007, T-010]`): dùng `MainScreen`, `MainScreenChooser`, tham số `chooseMainScreen` của `AppNavHost` và `MainScreenTest.kt` mà T-010 tạo. Điều kiện R2 của T-010 vẫn áp dụng: `AppLaunchTest` chỉ tới được camera khi catalog mẫu và bộ khởi đầu có trên R2 staging (plan T-010, mục 3.3).
  - T-012 (màn chào): điều hướng tới `CameraDestination(askPermission = false)` sau khi đã hỏi quyền; ghi `camera_permission_result` với `source` `onboarding`.
  - T-014, T-015, T-017: thêm phần tư thế vào `CameraScreen` (slot chip và dải tư thế, lớp trong khung), ghi lại bốn ảnh chuẩn của T-013 và tạo các ảnh thiết kế còn lại. T-016: đọc `CapturedPhotoHolder`, nối `onPhotoCaptured` và ô ảnh vừa chụp. T-020: bố cục mở gập. T-021, T-025: nối hai nút ở thanh trên; T-025 quyết định nơi lưu "âm khi chụp".
- Đề xuất thứ tự chạy: sau T-007, T-010 và T-011 (cùng phiên hay phiên trước), trước T-012 nếu được (T-012 khi đó dùng `CameraDestination(askPermission = false)` sẵn có), trước T-014 và T-016. T-009 chạy trước vì T-010 phụ thuộc T-009.

## 7. Điều kiện dừng riêng

1. Lúc bắt đầu task, T-007 chưa có trên nhánh chính, hoặc `AppColors` thiếu một trong các trường `cameraBackground`, `cameraSurface`, `onCamera`, `cameraOnSurfaceVariant`, `cameraControl`, `cameraOnControl`, `cameraScrim`, hoặc thiếu `Radius.thumb`, `Radius.full`, `Stroke.sticker`, `Stroke.thin`: dừng, không tự thêm màu hay viết cứng mã màu.
2. Lúc bắt đầu task, T-010 chưa có trên nhánh chính (thiếu `navigation/MainScreen.kt`, `navigation/MainScreenChooser.kt` hoặc `MainScreenTest.kt`), hoặc API khác plan T-010: `MainScreenInputs` không có `cameraAvailable`, `MainScreenChooser` không có `suspend fun choose(): MainScreen` và `inputs()`, `AppNavHost` không có `MainScreen.destination()` với nhánh `CAMERA`, hoặc thêm camera cần sửa `MainActivity.kt` hay file khác không có ở mục 3.2: dừng.
3. `AppLaunchTest.kt` trên nhánh chính chờ một màn khác `sample_screen` (ví dụ màn chào của T-012 đã bật ở staging): không đổi sang `camera_screen`; dừng nếu màn đích ở staging sau T-013 không còn là màn mà test đang chờ. `AppLaunchTest` đỏ vì loading dừng ở `Failed` (thiếu file trên R2 staging, điều kiện của T-010): dừng, không tăng thời gian chờ.
4. `camera-lifecycle` hoặc `camera-compose` 1.6.2 không có `ProcessCameraProvider.awaitInstance` hoặc `CameraXViewfinder`, và cách thay thế cần thêm thư viện (ví dụ `concurrent-futures-ktx`): dừng (điểm chặn 2). Tự chờ `ListenableFuture` bằng `addListener` và `suspendCancellableCoroutine` thì được, không cần dừng.
5. Lint báo lỗi (không phải cảnh báo) cho `android:screenOrientation`: dừng, không tắt quy tắc lint.
6. Test emulator `AppLaunchTest` đỏ vì app dừng đột ngột khi mở camera trên emulator không có camera: sửa trong `CameraXController` (bắt lỗi thành `Error`) được tính vào giới hạn 3 lần sửa chung.
7. Phương án A: `NetworkStatus` có nhưng không có `dialogShown: StateFlow<Boolean>` như plan T-011: dùng phương án B và ghi vào nhật ký, không sửa `:core:network`.
8. (Đã bỏ: độ mờ lưới đã giải quyết ở mục 8 câu 1.)

## 8. Câu hỏi cho người duyệt

1. **Độ mờ của lưới: đã giải quyết, không cần trả lời.** Phiên chính kiểm tra Figma: các instance `GridOverlay` (và `ZoomControl`, thanh trượt trong `AdjustPanel`) còn giữ độ mờ 1 từ trước khi component được sửa, nên ảnh xuất ra có nét trắng đặc. Đã sửa trên Figma và xuất lại gói `camera`: lưới `camera/onCamera` độ mờ 18% như README; nền `ZoomControl` `camera/scrim` 45%. `context/` không ghi độ mờ của lớp tô nên vẫn chỉ ghi #FFFFFF; lấy độ mờ theo README. `GridOverlayDefaults.Alpha = 0.18f`.
2. **Script so sánh thiết kế.** ADR-0020 yêu cầu chạy script so sánh và đưa `design/review/` vào PR. `plan-check.py` báo lỗi nếu plan nhắc tới script mà thiếu ảnh chuẩn cho bất kỳ ảnh nào trong 18 ảnh thiết kế của camera, nên plan theo ticket nhỏ hơn feature không qua được. Đề xuất của AI: T-013 không chạy script, rà soát độc lập so bằng mắt; ảnh so sánh đầy đủ chạy ở ticket camera cuối cùng (T-020) khi đủ 18 ảnh. Đồng thời đề xuất về dự án mẫu (`docs/ops/from-app.md`): cho plan khai báo các ảnh thiết kế thuộc ticket khác để `plan-check.py` bỏ qua. Người duyệt muốn chạy script ngay ở T-013 thì phải sửa quy tắc của `plan-check.py` trước.
3. **Giá trị tham số của `photo_captured`.** Ticket ghi `pose_id` để trống; spec ghi `matched` là "có hay không". Đề xuất của AI: `pose_id` là chuỗi rỗng, `matched` là `"false"` (dạng `true`/`false`), T-014 và T-017 điền giá trị thật. Firebase có thể hiện chuỗi rỗng là "(not set)"; nếu muốn phân biệt rõ thì dùng `"none"`.
4. **Lỗi chụp và lỗi lưu công cụ không có thông báo.** Thiết kế và spec không có chữ cho hai lỗi này. Đề xuất của AI: chỉ ghi log; khi chụp lỗi, nút chụp bật lại để người dùng bấm lần nữa; khi lưu công cụ lỗi, lựa chọn vẫn dùng được trong lần này, chỉ không được nhớ. Nếu cần thông báo, bổ sung chữ vào thiết kế và làm ở plan sau.
5. **Lưới mặc định.** Spec không ghi mặc định của lưới. Đề xuất của AI: tắt ở lần dùng đầu (như flash và hẹn giờ). Ảnh thiết kế `default` bật lưới vì đó là trạng thái minh họa.
6. **Âm chụp.** Ticket ghi "âm chụp theo Cài đặt (mặc định bật khi chưa có màn cài đặt)". Cài đặt thuộc `:feature:settings` (T-025), camera không đọc được trực tiếp (ADR-0001). Đề xuất của AI: T-013 luôn phát âm; T-025 đưa khóa "âm khi chụp" vào một module core (ví dụ `:core:data`) và nối vào `CameraXController`. Lưu ý: vài nước bắt buộc âm chụp, Android vẫn phát dù app tắt.
7. **Ảnh camera trước có lật gương không?** Khung xem trước camera trước luôn như gương; ảnh lưu mặc định của CameraX thì không lật (như người khác nhìn thấy). Đề xuất của AI: giữ mặc định, không lật. Nếu muốn ảnh giống khung xem trước thì đặt `isReversedHorizontal = true` cho camera trước.
8. **Bàn giao ảnh cho T-016.** Đề xuất của AI: `CapturedPhotoHolder` trong bộ nhớ, file JPEG trong `cacheDir/captures`, mỗi lần chụp mới dọn ảnh cũ chưa lưu. Ảnh chưa lưu mất khi hệ thống đóng app lúc đang ở màn xem lại; T-016 có thể lưu đường dẫn vào `SavedStateHandle` nếu cần giữ lâu hơn.
9. **Vùng chạm của zoom.** Mỗi nút zoom tròn 40 theo thiết kế; vùng chạm thực tế cao 48 (cả viên thuốc) nhưng rộng 44. Tiêu chí duyệt thiết kế đòi ít nhất 48. Đề xuất của AI: giữ đúng thiết kế ở T-013; nếu cần 48 x 48 thì sửa thiết kế (nút 44 hoặc khoảng cách 8) trước.
10. **Mô tả cho trình đọc màn hình.** Bảng chữ của thiết kế không có mô tả cho các nút biểu tượng. Đề xuất của AI: "Thư viện tư thế", "Cài đặt", "Flash: tắt", "Flash: bật", "Flash: tự động", "Hẹn giờ: tắt", "Hẹn giờ: 3 giây", "Hẹn giờ: 10 giây", "Lưới chia ba", "Đổi camera", "Chụp ảnh", "Hủy đếm ngược", "Ảnh vừa chụp".
11. **Đổi camera khi đang đếm ngược.** Thiết kế đếm ngược không làm mờ thanh trên và nút đổi camera. Đề xuất của AI: các nút vẫn bấm được; flash và lưới đổi ngay; đổi hẹn giờ áp dụng cho lần chụp sau; đổi camera thì hủy đếm ngược (camera phải mở lại).
12. **Thanh trạng thái phía trên camera.** `AppNavHost` đặt `safeDrawingPadding` cho mọi màn, nên vùng sau thanh trạng thái lấy màu nền của theme máy (máy để giao diện sáng thì có dải sáng trên camera tối). Sửa đúng cần đổi cách xử lý vùng an toàn cho mọi màn, ngoài phạm vi ticket. Đề xuất của AI: T-013 giữ nguyên; tạo ticket riêng cho camera tràn viền trước hoặc cùng T-020 (máy gập cũng cần). Lề trên 32 của thanh công cụ tính từ mép màn như thiết kế, nên trên máy thật khung xem trước thấp đi bằng chiều cao thanh trạng thái (README cho phép khung co theo chiều cao còn lại).
13. **Cỡ task.** Plan có khoảng 60 file (kể cả 12 biểu tượng, 8 ảnh chuẩn), cả code phần cứng lẫn bốn thành phần core. AI đánh giá khó xong trong một lượt của phiên tự chạy và `token_estimate` 40M (số tạm cho cỡ L) có thể thiếu. Đề xuất của AI: tách thành hai plan cùng ticket, chạy nối tiếp:
    - T-013a: module, bốn thành phần core và catalog, `CameraController` và `CameraXController`, quyền và ba trạng thái (KB-7, KB-8), cờ, điều hướng, khóa dọc, `AppLaunchTest`, ba ảnh chuẩn `starting`, `permission-denied`, `error` và `permission-denied_small`. Khoảng 40 file.
    - T-013b: công cụ chụp và lưu công cụ (KB-4), chụp, đếm ngược, `CapturedPhotoHolder`, `photo_captured`, ngắt đếm ngược (KB-6), ảnh `countdown`, `ready`, `countdown_small`. Khoảng 20 file.
    Người duyệt đồng ý thì AI viết lại thành hai plan phiên bản 2 từ chính các mục của plan này; không thì chạy nguyên một plan.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

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
