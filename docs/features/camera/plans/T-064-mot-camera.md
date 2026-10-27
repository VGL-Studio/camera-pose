---
ticket: T-064
title: Máy chỉ có một camera thì nút đổi camera mờ đi, camera mở bằng camera máy có
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 5                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: medium           # low | medium | high
token_estimate: 5.5M         # cỡ S, số gợi ý của scripts/token-usage.py estimate (2026-10-27), không nâng tay (L-081)
blocking_points: []
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-064: MÁY CHỈ CÓ MỘT CAMERA THÌ NÚT ĐỔI CAMERA MỜ ĐI

## 1. Tóm tắt

Trên máy chỉ có một camera, nút đổi camera vẫn bấm được; bấm vào thì app lưu lựa chọn camera kia, mở không được và hiện màn lỗi, mở lại app vẫn lỗi (FB-20261027-01, thấy ở lần chạy thử của T-062). Task này hỏi máy có những camera nào trước khi mở (`ProcessCameraProvider.hasCamera`, qua hàm mới `CameraController.availableLenses()`): máy một camera thì mở camera máy có, kể cả khi lựa chọn đã lưu là camera kia, và nút đổi camera mờ đi (trạng thái tắt đã có trong thiết kế). Tám test JVM mới đã chạy trên code hiện tại cộng khung rỗng: 6 thất bại đúng lý do, 2 đối chứng qua; với bản cài thử, cả 452 test của `:feature:camera` qua và `scripts/check.sh` qua (mục 4.1). Không đổi ảnh chuẩn.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md` (phần đổi của KB-4 và câu hỏi 17 do plan này đề xuất, spec `changed`, chờ Tuấn xác nhận lại; plan chỉ duyệt được sau đó):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-4 (đề xuất) | Khi máy chỉ có một camera thì nút đổi camera mờ đi, không bấm được; camera mở bằng camera máy có, kể cả khi lần trước đã chọn camera kia (FB-20261027-01, câu hỏi 17) | Máy có hai camera: không đổi gì. Máy một camera: `Ready.canFlip = false`, nút tắt như khi camera chưa sẵn sàng (`CameraIconButton` `enabled = false`, mờ 38%, TalkBack đọc "đã tắt"); `onFlipClick` không làm gì. Lựa chọn đã lưu không bị ghi đè (máy khác, hay camera kia cắm lại, vẫn theo lựa chọn cũ). Máy không trả lời được (lỗi khi hỏi) thì làm như hôm nay: mở lựa chọn đã lưu, nút bấm được |
| KB-4 | Khi bấm nút đổi camera thì camera đổi giữa trước và sau; nét viền và tư thế giữ nguyên | Không đổi với máy hai camera: các test `flip_*` có sẵn vẫn qua |
| KB-4 | Khi mở lại app thì ... camera trước sau giữ như lần trước | Không đổi với máy hai camera. Máy một camera: lựa chọn vẫn lưu, nhưng app mở camera máy có |
| KB-6 | Khi đang đếm ngược mà ... đổi camera ... thì đếm dừng | Máy một camera: nút tắt nên không dừng đếm; nút hủy và chạm khung vẫn dừng đếm (T-061) |

Thiết kế tham chiếu: `features/camera/design/README.md`, dòng "Nút đổi camera": "Mờ (độ mờ 38%, không bấm được) khi camera chưa sẵn sàng". Trạng thái tắt đã có, task chỉ thêm một điều kiện dùng nó; không có ảnh thiết kế mới (câu hỏi 2). Không ảnh nào đổi.

ADR liên quan: ADR-0019 (log), ADR-0020 (ảnh thiết kế và screenshot test).

## 3. Giải pháp

### 3.1. Cách làm

Hiện nay (nhánh chính f464d7f): `FlipCameraButton` có `enabled = ready != null` (`CameraControls.kt:368`); `CameraViewModel.openCamera` mở `tools.lens` đã lưu (`CameraViewModel.kt:215`); `onFlipClick` lưu `lens.next()` rồi mở lại. App không hỏi máy có camera nào.

Cách làm, đúng như bản cài thử ở mục 4.1:

1. `camera/CameraController.kt`: thêm vào interface

   ```kotlin
   /** The lenses this device has; empty when the camera system cannot say (spec camera, KB-4, T-064). */
   suspend fun availableLenses(): Set<Lens> = Lens.entries.toSet()
   ```

   Mặc định trả cả hai, để các bản giả khác của interface không phải đổi.
2. `camera/CameraXController.kt`: cài đặt bằng `ProcessCameraProvider.awaitInstance(context)` (giữ vào `provider` như `open`) và `provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA / DEFAULT_FRONT_CAMERA)`, trên `Dispatchers.Main`. `hasCamera` ném `CameraInfoUnavailableException` hay lỗi khác thì trả tập rỗng (không biết); `CancellationException` của chính coroutine gọi thì ném lại, như `open`. Mã đã chạy thử (đặt trước `setFlash`):

   ```kotlin
   override suspend fun availableLenses(): Set<Lens> = withContext(Dispatchers.Main) {
       try {
           val provider = ProcessCameraProvider.awaitInstance(context).also { provider = it }
           Lens.entries.filterTo(mutableSetOf()) { lens ->
               val selector =
                   if (lens == Lens.BACK) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
               provider.hasCamera(selector)
           }
       } catch (e: CancellationException) {
           if (!currentCoroutineContext().isActive) throw e
           emptySet()
       } catch (e: Exception) {
           // Unknown: the open says whether the lens is there.
           emptySet()
       }
   }
   ```
3. `CameraUiState.kt`: `Ready` thêm `val canFlip: Boolean = true` (cuối danh sách, có mặc định, để test và ảnh chuẩn có sẵn không đổi).
4. `CameraViewModel.kt`:
   - `openCamera`: sau `loadTools()`, `val lenses = controller.availableLenses()`; `val lens = if (lenses.isEmpty() || tools.lens in lenses) tools.lens else lenses.first()`; `lenses.size == 1` thì ghi log `info` "Only one camera: <back|front>"; mở `lens`; `Ready(lens = lens, ..., canFlip = lenses.isEmpty() || lenses.size > 1)`; dòng log "Camera ready: ..." dùng `lens` (nội dung không đổi với máy hai camera; kịch bản và log của T-058 đọc dòng này).
   - `onFlipClick`: `if (ready.capturing || !ready.canFlip) return`, trước `cancelCountdown`.
   - Không lưu `lens` vừa chọn thay: `repository.setLens` chỉ gọi khi người dùng bấm.
5. `CameraControls.kt`, `FlipCameraButton`: `enabled = ready?.canFlip == true`. Bố cục mở gập dùng cùng hàm (`CameraUnfolded.kt:193`), nên không cần sửa file đó.

Thứ tự commit (L-025): bước 3 gồm bốn file test và khung rỗng ở code chính: hàm `availableLenses()` của interface với mặc định trên, `canFlip` của `Ready` (mặc định `true`, chưa đặt ở đâu), bản giả `FakeCameraController` thêm `var lenses` và `override suspend fun availableLenses() = lenses`, tệp kịch bản. Test biên dịch được và thất bại ở phần khẳng định (mục 4.1). Bước 4: phần còn lại của mục 1 đến 5.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraController.kt` | `availableLenses()` có mặc định |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraXController.kt` | Cài đặt `availableLenses()` bằng `hasCamera` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraUiState.kt` | `Ready.canFlip` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModel.kt` | Chọn camera mở, `canFlip`, log, chặn `onFlipClick` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraControls.kt` | `FlipCameraButton` tắt khi `!canFlip` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraFakes.kt` | `FakeCameraController.lenses`, `availableLenses()` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModelTest.kt` | 6 test mới (mục 4) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenTest.kt` | 1 test mới, import `assertIsNotEnabled` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenUnfoldedTest.kt` | 1 test mới, import `assertIsNotEnabled` |
| Tạo | `feature/camera/src/deviceScenarios/T-064-mot-camera.txt` | Kịch bản trên emulator một camera (mục 4) |

File không đổi, đã kiểm (L-020): `CameraUnfolded.kt` (gọi `FlipCameraButton`); các bản giả khác của `CameraController`: `git grep -n ': CameraController'` chỉ có `CameraXController` và `FakeCameraController`, nên mặc định của interface đủ. Không ảnh chuẩn nào đổi: mọi `Ready` của screenshot test dùng mặc định `canFlip = true` (mục 4.1, `verifyRoborazziDebug` qua).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `ProcessCameraProvider.hasCamera` không cần quyền ngoài `CAMERA` đã có; gọi trước khi có quyền cũng không mở camera (`openCamera` chỉ chạy sau khi có quyền, `onScreenStarted`).
- **Thư viện và SDK:** Không thêm. `ProcessCameraProvider`, `CameraSelector` thuộc `androidx.camera:camera-lifecycle` và `camera-core`, đã có trong `:feature:camera` (`libs.androidx.camera.lifecycle`, `libs.androidx.camera.core`).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `hasCamera` trên vài máy trả `true` cho camera mà `bindToLifecycle` vẫn không mở được (máy báo sai số camera; log của emulator CI cũ: "Device reporting less cameras than anticipated").
- **Điều kiện chuyển:** không chuyển trong phiên; JVM test không thấy được điều này. Nếu bản staging trên máy thật vẫn hiện màn lỗi sau khi bấm nút đổi camera, ghi phản hồi mới.
- **Cách làm thay:** (cho plan sau, không làm trong phiên) khi mở camera đã chọn bị lỗi mà camera kia có, mở camera kia và đặt `canFlip = false`. Không có trong mục 3.2 vì đổi tiêu chí chấp nhận.

## 4. Kiểm thử

Test JVM dùng `FakeCameraController.lenses` (tập camera máy có), `FakeCameraSettingsRepository.stored` (lựa chọn đã lưu) và `RecordingAppLogger` như các test `flip_*` có sẵn.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-4 (đề xuất), máy một camera: nút không bấm được | Unit test | `CameraViewModelTest.oneCamera_flipUnavailable`: `lenses = setOf(BACK)`, `ready().canFlip` là `false` |
| KB-4 (đề xuất), bấm khi máy một camera không làm gì | Unit test | `CameraViewModelTest.oneCamera_flipClick_isIgnored`: `onFlipClick()`; `openCalls == [BACK]`, `ready().lens == BACK`, `lensSaves` rỗng |
| KB-4 (đề xuất), lựa chọn đã lưu là camera máy không có | Unit test | `CameraViewModelTest.savedFront_withoutFrontCamera_opensBack_keepsSetting`: `stored = CameraSettings(lens = FRONT)`, `lenses = setOf(BACK)`; `openCalls == [BACK]`, `lens == BACK`, `canFlip == false`, `lensSaves` rỗng, log `Camera` "Only one camera: back" |
| KB-4 (đề xuất), máy chỉ có camera trước | Unit test | `CameraViewModelTest.frontOnlyDevice_opensFront`: `lenses = setOf(FRONT)`; `openCalls == [FRONT]`, `lens == FRONT`, `canFlip == false` |
| KB-4, máy hai camera không đổi (đối chứng) | Unit test | `CameraViewModelTest.twoCameras_flipAvailable`: mặc định; `canFlip == true`. Đối chứng, qua cả trên khung rỗng; cùng với `flip_switchesLens_resetsZoom_keepsTools`, `flip_twiceQuickly_returnsToBack` có sẵn |
| KB-4, máy không trả lời được (đối chứng) | Unit test | `CameraViewModelTest.noLensReported_opensSavedLens_flipStaysAvailable`: `lenses = emptySet()`; `openCalls == [BACK]`, `canFlip == true`. Đối chứng, qua trên khung rỗng; thất bại nếu cài đặt coi tập rỗng là "không có camera" (đột biến ở mục 4.1) |
| KB-4 (đề xuất), nút mờ, bấm không gọi gì | Compose UI test | `CameraScreenTest.flip_cannotFlip_disabled`: `READY.copy(canFlip = false)`; `FLIP` `assertIsNotEnabled()`, `performClick()`, `calls` rỗng |
| KB-4 (đề xuất), bố cục mở gập | Compose UI test | `CameraScreenUnfoldedTest.unfolded_cannotFlip_disabled`: `Screen(uiState = DEFAULT.copy(canFlip = false))`; như trên |
| Giao diện các màn camera không đổi | Screenshot test (có sẵn) | Mọi ảnh `CameraScreen_*`, `ReviewScreen_*` không đổi (`verifyRoborazziDebug`) |
| Đúng thiết kế (khi có `design/screens/`) | Ảnh chuẩn có sẵn, cùng tên ảnh thiết kế | Không thêm hay ghi lại ảnh nào. Ảnh thiết kế của ticket khác (T-013, T-014, T-015, T-016, T-017, T-020): `CameraScreen_default_dark`, `CameraScreen_no-pose_dark`, `CameraScreen_adjust_dark`, `CameraScreen_locked_dark`, `CameraScreen_steps-hidden_dark`, `CameraScreen_matching_dark`, `CameraScreen_matched_dark`, `CameraScreen_no-person_dark`, `CameraScreen_countdown_dark`, `CameraScreen_starting_dark`, `CameraScreen_permission-denied_dark`, `CameraScreen_error_dark`, `CameraScreen_default_dark_unfolded`, `ReviewScreen_default_dark`, `ReviewScreen_saving_dark`, `ReviewScreen_save-error_dark`, `ReviewScreen_discard_dark`, `ReviewScreen_default_dark_unfolded` |
| KB-4 (đề xuất), máy một camera thật: bấm nút đổi camera, rời màn, quay lại, camera vẫn sẵn sàng | Kiểm tra thủ công (emulator: `feature/camera/src/deviceScenarios/T-064-mot-camera.txt`) | Emulator CI chỉ có camera sau sau T-062. Tệp bấm `camera_flip`, mở thư viện, `back`, chờ `camera_grid_button` (chỉ có ở `Ready`), `expect_no str=camera_error_title`. Trên code hiện tại camera kẹt ở camera trước và màn lỗi hiện, nên bước `wait id=camera_grid_button` thất bại; trên máy hai camera tệp cũng đạt (đổi sang camera trước được). Emulator không có camera (trước T-062) thì `need` cho `bỏ qua (môi trường)` |
| TalkBack đọc nút đã tắt | Kiểm tra thủ công (máy thật: TalkBack) | Máy một camera (hay emulator `-camera-front none`): TalkBack đọc nút đổi camera kèm "đã tắt" |

Mã các test JVM đúng như đã chạy thử (mục 4.1):

```kotlin
    // KB-4 (plan T-064): a device with one camera cannot switch; the saved lens it lacks falls back to the one it has.
    @Test
    fun oneCamera_flipUnavailable() = runTest {
        controller.lenses = setOf(Lens.BACK)
        val viewModel = readyViewModel()
        assertFalse(viewModel.ready().canFlip)
    }

    @Test
    fun twoCameras_flipAvailable() = runTest {
        val viewModel = readyViewModel()
        assertTrue(viewModel.ready().canFlip)
    }

    @Test
    fun oneCamera_flipClick_isIgnored() = runTest {
        controller.lenses = setOf(Lens.BACK)
        val viewModel = readyViewModel()
        viewModel.onFlipClick()
        runCurrent()
        assertEquals(listOf(Lens.BACK), controller.openCalls)
        assertEquals(Lens.BACK, viewModel.ready().lens)
        assertEquals(emptyList<Lens>(), repository.lensSaves)
    }

    @Test
    fun savedFront_withoutFrontCamera_opensBack_keepsSetting() = runTest {
        repository.stored.value = CameraSettings(lens = Lens.FRONT)
        controller.lenses = setOf(Lens.BACK)
        val viewModel = readyViewModel()
        assertEquals(listOf(Lens.BACK), controller.openCalls)
        assertEquals(Lens.BACK, viewModel.ready().lens)
        assertFalse(viewModel.ready().canFlip)
        assertEquals(emptyList<Lens>(), repository.lensSaves)
        assertTrue(logger.entries.any { it.tag == "Camera" && it.message == "Only one camera: back" })
    }

    @Test
    fun frontOnlyDevice_opensFront() = runTest {
        controller.lenses = setOf(Lens.FRONT)
        val viewModel = readyViewModel()
        assertEquals(listOf(Lens.FRONT), controller.openCalls)
        assertEquals(Lens.FRONT, viewModel.ready().lens)
        assertFalse(viewModel.ready().canFlip)
    }

    @Test
    fun noLensReported_opensSavedLens_flipStaysAvailable() = runTest {
        controller.lenses = emptySet()
        val viewModel = readyViewModel()
        assertEquals(listOf(Lens.BACK), controller.openCalls)
        assertTrue(viewModel.ready().canFlip)
    }
```

Đặt trước `flip_twiceQuickly_returnsToBack` trong `CameraViewModelTest`. `CameraScreenTest` (trước `countdown_showsNumber_andShutterCancels`):

```kotlin
    // KB-4 (plan T-064): one camera, the flip button is faded and does nothing.
    @Test
    fun flip_cannotFlip_disabled() {
        setScreen(READY.copy(canFlip = false))
        composeRule.onNodeWithTag(CameraTestTags.FLIP).assertIsNotEnabled().performClick()
        assertEquals(emptyList<String>(), calls)
    }
```

`CameraScreenUnfoldedTest` (trước `unfoldedControls_callTheSameCallbacks`):

```kotlin
    // KB-4 (plan T-064): the right column uses the same flip button.
    @Test
    fun unfolded_cannotFlip_disabled() {
        composeRule.setContent { Screen(uiState = DEFAULT.copy(canFlip = false)) }
        composeRule.onNodeWithTag(CameraTestTags.FLIP).assertIsNotEnabled().performClick()
        assertEquals(emptyList<String>(), calls)
    }
```

`CameraFakes.kt`, sau `var openError`:

```kotlin
    var lenses: Set<Lens> = Lens.entries.toSet()

    override suspend fun availableLenses(): Set<Lens> = lenses
```

Tệp kịch bản `feature/camera/src/deviceScenarios/T-064-mot-camera.txt`:

```text
ticket: T-064
criterion: Máy chỉ có camera sau (emulator CI sau T-062): bấm nút đổi camera, rời màn rồi quay lại thì camera vẫn sẵn sàng, không có màn lỗi (FB-20261027-01)
timeout: 120
fresh
consent decline 30
wait id=onboarding_screen | id=camera_screen 60
tap_if id=onboarding_enable_camera
wait id=camera_screen 15
need id=camera_grid_button 15
# (1) bấm nút đổi camera: máy hai camera đổi sang camera trước, máy một camera không đổi gì
tap id=camera_flip
# (2) rời màn camera rồi quay lại: camera mở lại theo lựa chọn đã lưu
tap id=camera_poses
wait id=poses_grid 20
back
wait id=camera_screen 15
wait id=camera_grid_button 15
expect_no str=camera_error_title
shot camera-after-flip
```

**Ngoại lệ viết test trước:** `CameraXController.availableLenses()` gọi CameraX thật, không chạy được trên JVM (như `open`); kiểm bằng tệp kịch bản trên emulator CI một camera ở `device-session` cuối phiên. Tệp kịch bản là test chạy cuối phiên, không "thất bại trước" được (`docs/CLAUDE.md`, mục Khi viết plan).

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-27, worktree `../wt-t064` trên nhánh chính f464d7f, không commit. JDK 21 (Temurin 21.0.13) và Android SDK (`platforms;android-36`, `build-tools;36.0.0`) tải vào thư mục tạm, vì máy dev không có.
- **Lệnh và kết quả:**
  1. Khung rỗng của bước 3 cộng 8 test mới: `./gradlew :feature:camera:testDebugUnitTest --tests '*CameraViewModelTest*' --tests '*CameraScreenTest*' --tests '*CameraScreenUnfoldedTest*'`: 81 test, 6 thất bại đúng lý do, 2 đối chứng qua, các test có sẵn qua:
     - `oneCamera_flipUnavailable`: `AssertionError` (`canFlip` là `true`);
     - `oneCamera_flipClick_isIgnored`: `expected:<[BACK]> but was:<[BACK, FRONT]>`;
     - `savedFront_withoutFrontCamera_opensBack_keepsSetting`: `expected:<[BACK]> but was:<[FRONT]>`;
     - `frontOnlyDevice_opensFront`: `expected:<[FRONT]> but was:<[BACK]>`;
     - `flip_cannotFlip_disabled`, `unfolded_cannotFlip_disabled`: `Failed to assert the following: (is not enabled)`;
     - đối chứng qua: `twoCameras_flipAvailable`, `noLensReported_opensSavedLens_flipStaysAvailable`.
  2. Bản cài thử (mục 3.1, bước 1 đến 5): `./gradlew :feature:camera:testDebugUnitTest`: 452 test, 0 thất bại.
  3. Bản cài thử: `scripts/check.sh`: lần đầu đỏ ở `:feature:camera:ktlintMainSourceSetCheck` (dòng `provider.hasCamera(if (...) ... else ...)` dài 134 ký tự; các task khác qua nhờ `--continue`), vì vậy mục 3.1 bước 2 tách biến `selector`; lần hai qua: `BUILD SUCCESSFUL`, "Test JVM: 1176 test, 0 lỗi. Ảnh screenshot: 97", `:feature:camera` 452 test, "Cổng chất lượng: QUA". Không ảnh chuẩn nào đổi.
  4. Đột biến cho đối chứng tập rỗng: đổi `canFlip = lenses.isEmpty() || lenses.size > 1` thành `canFlip = lenses.size > 1`: `noLensReported_opensSavedLens_flipStaysAvailable` thất bại, các test khác qua. Đã hoàn tác.
  5. `scripts/device-scenario.py check feature/camera/src/deviceScenarios/T-064-mot-camera.txt`: mã 0 (tệp tạm, không commit; tạo ở bước 3 của task).
- Lần chạy thử T-062 (lần chạy 40355405834, emulator chỉ có camera sau) là bằng chứng của lỗi gốc; tệp kịch bản chưa chạy trên emulator vì T-062 chưa merge: kết quả đầu tiên là `device-session` cuối phiên có T-062 và T-064.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | 5 file chính trong `:feature:camera`, thêm hàm vào interface công khai `CameraController` |
| B. Độ rõ của yêu cầu | 1 | Spec đổi KB-4 chờ xác nhận lại; chọn mờ hay ẩn nút là câu hỏi 1 |
| C. Khả năng kiểm chứng tự động | 1 | Logic và giao diện có test JVM; phần CameraX kiểm bằng tệp kịch bản trên emulator; TalkBack trên máy thật |
| D. Ảnh hưởng chéo | 0 | Chỉ file của `:feature:camera` |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu đã có (`hasFlash` làm nút flash mờ; `ProcessCameraProvider` đã dùng ở `open`) |
| **Tổng** | 5 | **Mức: Trung bình** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: Không (chỉ `:feature:camera`).
- Task khác trong sprint có thể xung đột: T-062 (đổi emulator CI sang camera sau giả): không chung file, nhưng tệp kịch bản của T-064 chỉ kiểm được sau T-062. T-063 không liên quan.
- Đề xuất thứ tự chạy: sau T-062.

## 7. Điều kiện dừng riêng

Không có.

## 8. Câu hỏi cho người duyệt

1. **Nút đổi camera mờ đi hay ẩn đi khi máy chỉ có một camera?** (spec camera, câu hỏi 17)
   - Đề xuất của AI: mờ đi, không bấm được. Lý do: trạng thái tắt đã có trong thiết kế và đã dùng khi camera chưa sẵn sàng; bố cục không đổi (bố cục mở gập đặt nút theo vị trí cố định, `CameraUnfolded.kt:44`); giống nút flash mờ khi camera không có flash (KB-4); không cần sửa Figma. Ẩn nút để lại chỗ trống ở hàng nút chụp và cột phải, cần thiết kế lại.
2. **Thiết kế:** không cần ảnh thiết kế mới (trạng thái tắt có sẵn). Đề xuất của AI: chỉ thêm "máy chỉ có một camera" vào dòng "Nút đổi camera" của `design/README.md` khi Tuấn xác nhận spec (việc của người, như các lần trước), không sửa Figma.
3. **Lựa chọn đã lưu khi máy không có camera đó:** đề xuất của AI: giữ, không ghi đè (chỉ lưu khi người dùng bấm), để dữ liệu khôi phục sang máy hai camera vẫn theo lựa chọn cũ.
4. **Máy báo có camera nhưng mở không được** (mục 3.4): đề xuất của AI: không làm ở task này; nếu bản staging trên máy thật vẫn gặp thì ghi phản hồi mới.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
