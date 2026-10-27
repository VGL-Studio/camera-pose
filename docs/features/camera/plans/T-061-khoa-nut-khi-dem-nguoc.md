---
ticket: T-061
title: Khóa nút và dừng đếm khi đang đếm ngược
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 7.5M         # cỡ M, scripts/token-usage.py estimate (7.5M); không nâng tay (L-081)
blocking_points: [core]      # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-26
---

# PLAN T-061: KHÓA NÚT VÀ DỪNG ĐẾM KHI ĐANG ĐẾM NGƯỢC

## 1. Tóm tắt

Khi `CameraUiState.Ready.countdown` khác `null`, màn camera làm mờ và khóa các nút đổi ảnh sắp chụp (flash, hẹn giờ, lưới, hàng nhóm, dải tư thế, cột công cụ nét viền, mũi tên thẻ bước), đóng bảng chỉnh đang mở, và đặt một lớp nhận chạm trong suốt phủ khung xem trước, nằm trên nét viền, thẻ bước và cột công cụ nhưng dưới nút zoom: chạm (không kéo) vào khung thì dừng đếm, kéo hay chụm thì không làm gì. Nút thư viện, cài đặt và ô ảnh vừa chụp dừng đếm rồi mới mở màn khác; đổi camera đã dừng đếm sẵn (`CameraViewModel.onFlipClick`). `StickerChip` và `PoseThumb` của `:core:designsystem` có thêm tham số `enabled` (chỉ truyền vào `selectable`, không đổi độ mờ) để trình đọc màn hình biết chip và ô tư thế đang khóa (điểm chặn 7); hàng nhóm và dải tư thế mờ ở mức hàng trong `PosePicker`, như thiết kế. Kiểm chứng bằng unit test của `CameraViewModel`, test giao diện Robolectric cho từng nút ở bố cục điện thoại, màn thấp và bố cục mở gập, và ghi lại hai ảnh chuẩn `CameraScreen_countdown_dark.png`, `CameraScreen_countdown_small.png` theo thiết kế mới (PR #167). Đã chạy thử cách làm trên nhánh chính 16e9019: 9 trên 10 test tạm thất bại đúng lý do, cả 10 qua với code thử, và chỉ hai ảnh chuẩn đếm ngược đổi (mục 4.1).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md` (spec `confirmed` ngày 2026-10-26, FB-20261026-04, câu hỏi 16):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-6 | Khi bấm nút chụp mà hẹn giờ bật thì số đếm ngược hiện to giữa khung xem trước, nút chụp đổi thành nút hủy; hết giờ thì chụp như trên; bấm nút hủy thì dừng đếm, không chụp. | Đã có (T-013). Giữ nguyên; test cũ `cancelCountdown_stopsWithoutCapture` phải còn qua không sửa; `countdown_showsNumber_andShutterCancels` giữ tên, chỉ đổi lambda ghi lời gọi và kỳ vọng thành `"cancel BUTTON"` (mục 4). |
| KB-6 | Khi đang đếm ngược thì flash, lưới, hẹn giờ, hàng nhóm và dải tư thế mờ đi và không phản hồi; cột công cụ nét viền và mũi tên thẻ bước mờ đi; bảng chỉnh đang mở thì đóng; kéo và chụm không đổi nét viền. Zoom vẫn dùng được (câu hỏi 16). | "Mờ đi" là độ mờ 0.38 (`DISABLED_ALPHA` của mọi thành phần, như thiết kế `CameraScreen_countdown_dark`, PR #167). "Không phản hồi" là chạm không gọi callback, và trình đọc màn hình đọc nút là không dùng được (ngữ nghĩa `Disabled`). Cột công cụ và mũi tên thẻ bước cũng khóa như vậy. Hàng nhóm và dải tư thế không cuộn được trong lúc đếm. Bảng chỉnh đóng một lần khi bắt đầu đếm. Kéo và chụm trên khung không tới `overlayGestures`. Zoom là nút zoom trong khung (điện thoại) hay ở cột bên phải (mở gập). |
| KB-6 | Khi đang đếm ngược mà chạm vào khung xem trước (ở bất kỳ chỗ nào ngoài nút zoom), đổi camera, hay rời màn camera (thư viện, cài đặt, ô ảnh vừa chụp) thì đếm dừng, không chụp; đổi camera và rời màn vẫn làm tiếp việc của nút. | "Chạm" là một ngón, nhấc lên trong ngưỡng chạm (touch slop); kéo hay chụm không phải chạm và không dừng đếm (câu hỏi 1). Chạm trên thẻ bước, cột công cụ, nét viền, nút "Bước", lời nhắc đều là chạm vào khung: dừng đếm, không làm việc của thành phần đó (ghi chú của plan T-059, mục 6). Rời màn: dừng đếm ngay khi bấm, trước khi mở màn khác, vì `ON_STOP` chỉ tới sau hiệu ứng chuyển màn và đếm có thể hết giờ trong lúc đó. Đổi camera: đã dừng đếm (`CameraViewModel.kt:163`), chỉ thêm test giao diện. Cột công cụ nằm ngoài khung trên màn thấp (T-015, trường hợp 4) không thuộc khung: chạm vào nó không dừng đếm, nút vẫn khóa. |

Ngoài spec, AI tự quyết (người duyệt thấy khác thì sửa):

- Nút "Bước" (khi thẻ bước đang ẩn) không có trong danh sách khóa của spec: chạm bằng tay vào nó là chạm vào khung (dừng đếm); trình đọc màn hình vẫn bấm được (hiện lại thẻ không đổi ảnh sắp chụp) (câu hỏi 2).
- Lớp nhận chạm không có ngữ nghĩa cho trình đọc màn hình (chỉ có `testTag`): người dùng TalkBack dừng đếm bằng nút hủy, đã có nhãn "Hủy đếm ngược".
- `CameraViewModel` cũng bỏ qua `onFlashClick`, `onTimerClick`, `onGridClick` khi đang đếm, phòng khi một lời gọi lọt qua giao diện (ví dụ bàn phím, trình đọc màn hình).
- Log `Camera`: "Countdown cancelled: <lý do>" với lý do `user` (nút hủy, giữ chữ cũ), `preview` (chạm khung), `leave` (rời màn), `flip`, `stopped`, `interruption` (đã có). Không thêm sự kiện phân tích.

Thiết kế tham chiếu: `features/camera/design/README.md` (dòng "Đếm ngược", cập nhật ở PR #162), ảnh `design/screens/CameraScreen_countdown_dark.png` và `design/context/CameraScreen_countdown_dark.md` (xuất lại ở PR #167, 0c4929f): độ mờ 0.38 trên flash (16:840), hẹn giờ (16:842), lưới (16:844), "Overlay tools" (16:866), hai mũi tên thẻ bước, "Group chips" (16:885), "Pose strip" (16:902); thư viện, cài đặt, zoom, vòng đếm, lớp mờ, ô ảnh vừa chụp, nút hủy, đổi camera giữ nguyên. Thiết kế chỉ có bản điện thoại; bố cục mở gập và màn thấp làm theo cùng quy tắc.

ADR liên quan: ADR-0019 (log), ADR-0008 (hệ thống thiết kế là `:core:designsystem`), ADR-0020 (screenshot test theo ảnh thiết kế).

## 3. Giải pháp

### 3.1. Cách làm

Code nền khi task chạy: nhánh chính sau T-058, T-060, T-059 của cùng danh sách chạy. T-058 không chạm file của plan này. T-060 thêm `clipToBounds()` vào lớp nét viền trong `PoseOverlay.kt`. T-059 đổi `HideableStepCard` trong `PoseOverlay.kt`: thêm `clickable(interactionSource = null, indication = null, onClickLabel = hideLabel, onClick = { currentOnHide() })`, bỏ hành động riêng "Ẩn thẻ bước", đổi `detectVerticalDragGestures` thành `detectDragGestures`. Plan này chỉ thêm tham số vào các hàm đó; nếu T-059 chưa merge (dừng hay bị bỏ qua) thì làm theo nhánh "chưa có T-059" ở bước 4.

1. **Lý do dừng đếm.** Thêm vào `CameraUiState.kt`:
   ```kotlin
   /** Why a running countdown stops, for the log (ADR-0019). */
   enum class CountdownCancelReason(val logName: String) { BUTTON("user"), PREVIEW("preview"), LEAVE("leave") }
   ```
   Enum công khai, như `CameraUiState`: `CameraViewModel` là lớp công khai, nên tham số kiểu `internal` của hàm công khai `onCancelCountdown` báo lỗi biên dịch `EXPOSED_PARAMETER_TYPE`.
   `CameraViewModel.onCancelCountdown(reason: CountdownCancelReason = CountdownCancelReason.BUTTON) = cancelCountdown(reason.logName)`. Tham số `onCancelCountdown` của `CameraScreen`, `CameraPhoneLayout`, `CameraUnfoldedLayout` đổi kiểu thành `(CountdownCancelReason) -> Unit`; `CaptureRow`, `UnfoldedCaptureColumn`, `ShutterButton` giữ `() -> Unit` và được truyền `{ onCancelCountdown(CountdownCancelReason.BUTTON) }`. Các test hiện có truyền `onCancelCountdown = {}` hay `{ calls += "cancel" }` vẫn biên dịch (lambda một tham số ngầm `it`). `CameraRoute` giữ `onCancelCountdown = viewModel::onCancelCountdown`.
2. **`CameraViewModel`:** `onFlashClick`, `onTimerClick`, `onGridClick` thêm `if (ready.countdown != null) return` ngay sau dòng lấy `ready`. `onZoomSelect`, `onFlipClick` không đổi.
3. **`CameraScreen` (chung cho hai bố cục).** Tính `counting = (uiState as? CameraUiState.Ready)?.countdown != null` và:
   - `LaunchedEffect(counting) { if (counting && (poseState as? PoseUiState.Ready)?.overlay?.panelOpen == true) poseActions.onClosePanel() }`: bảng chỉnh đóng một lần khi bắt đầu đếm. Lệch so với chữ của ticket ("`CameraViewModel`: ... đóng bảng chỉnh khi bắt đầu đếm"): trạng thái bảng chỉnh nằm ở `PoseViewModel`, `CameraViewModel` không thấy nó, nên việc đóng đặt ở `CameraScreen`, nơi thấy cả hai trạng thái.
   - Bọc ba lối rời màn trước khi truyền xuống bố cục: `onOpenPoses`, `onOpenSettings`, `onLastPhotoClick` thành `{ if (counting) onCancelCountdown(CountdownCancelReason.LEAVE); open() }` (vẫn `null` khi gốc `null`, để `topButtons_hiddenWithoutCallbacks` và `lastPhoto_isEmptyAndNotClickable` giữ nguyên).
   - Truyền `locked = counting` cho `PoseOverlay`, `enabled = !counting` cho `PosePicker` và `OverlayToolColumn` (nhánh ngoài khung của bố cục điện thoại), `onCountdownTap = { onCancelCountdown(CountdownCancelReason.PREVIEW) }` cho `PreviewContent`, ở cả `CameraPhoneLayout` và `CameraUnfoldedLayout`.
4. **`PoseOverlay(locked: Boolean = false)`:** `previousEnabled = index > 0 && !locked`, `nextEnabled = index < selected.steps.lastIndex && !locked`; `OverlayToolColumn(enabled = !locked)`; `HideableStepCard(enabled = !locked)`:
   Quy tắc chung: cách ẩn thẻ nào còn lại trong `HideableStepCard` sau T-059 cũng chỉ có hiệu lực khi `enabled`, để trình đọc màn hình không ẩn được thẻ khi đang đếm. Ba trường hợp có thể gặp:
   - T-059 theo phương án chính: `clickable(enabled = enabled, onClickLabel = hideLabel, ...)`;
   - T-059 theo phương án dự phòng (mục 3.4 của plan đó: một `pointerInput` và `semantics { onClick(label = hideLabel) { ... } }`): chỉ gắn `onClick` của `semantics` khi `enabled`, và `pointerInput` không gọi `onHide` khi `!enabled`;
   - T-059 chưa merge: `semantics { if (enabled) customActions = ... }`.
   Thẻ không tự đổi độ mờ: thiết kế chỉ làm mờ hai mũi tên. Không đổi `overlayGestures`: lớp nhận chạm ở bước 6 nằm trên nó.
5. **`OverlayToolColumn(enabled: Boolean = true)`:** bốn nút `enabled = enabled && <điều kiện cũ>` (chỉnh và lật vẫn mờ khi khóa nét viền). Mỗi nút tự mờ 0.38 khi tắt (`CameraIconButton`), nên không đặt độ mờ cho cả cột (tránh nhân hai lần, 0.38 x 0.38). Các nút không chồng nhau nên nhìn giống độ mờ của khung "Overlay tools" trên thiết kế.
6. **`CameraControls.kt`:**
   - `CameraTopBar`: `val locked = ready?.countdown != null`; flash `enabled = ready.flashAvailable && !locked`, hẹn giờ `enabled = !locked` (thêm tham số `enabled` cho `TimerButton` riêng của file, truyền cho cả hai nhánh), lưới `enabled = !locked`. Thư viện và cài đặt không đổi.
   - `PreviewContent(onCountdownTap: () -> Unit = {})` truyền xuống `ReadyPreview`. Trong `ReadyPreview`, ngay sau `poseOverlay()` và trước `CameraZoom`, khi `ready.countdown != null` thêm:
     ```kotlin
     Box(modifier = Modifier.fillMaxSize().testTag(CameraTestTags.COUNTDOWN_TAP).countdownTapArea { currentOnCountdownTap() })
     ```
     với `val currentOnCountdownTap by rememberUpdatedState(onCountdownTap)` và hàm riêng của file:
     ```kotlin
     /**
      * Takes every touch on the frame while counting down, so nothing under it (outline, step card, tools) reacts. A tap,
      * one finger that stays within the touch slop, calls [onTap]; a drag or a pinch does nothing (spec camera, KB-6).
      */
     private fun Modifier.countdownTapArea(onTap: () -> Unit): Modifier = pointerInput(Unit) {
         awaitEachGesture {
             val down = awaitFirstDown(requireUnconsumed = false)
             down.consume()
             var tap = true
             do {
                 val event = awaitPointerEvent()
                 event.changes.forEach { it.consume() }
                 val moved = event.changes.any {
                     it.id == down.id && (it.position - down.position).getDistance() > viewConfiguration.touchSlop
                 }
                 if (event.changes.size > 1 || moved) tap = false
             } while (event.changes.any { it.pressed })
             if (tap) onTap()
         }
     }
     ```
     Thứ tự vẽ không đổi: lớp mờ 35% và vòng số vẫn vẽ sau cùng, trên cả zoom, như thiết kế. Lớp mờ và vòng số không có bộ nhận chạm, nên chạm đi xuống nút zoom (ở trên lớp nhận chạm) hoặc lớp nhận chạm. Compose chỉ giao chạm cho anh em trên cùng có bộ nhận chạm, nên nét viền, thẻ bước, cột công cụ ở dưới không nhận gì (đo ở mục 4.1). Không dùng `detectTapGestures`: nó không hủy khi kéo trong vùng, nên kéo cũng gọi `onTap` (đo ở mục 4.1). Mẫu `awaitEachGesture` có sẵn ở `pose/OverlayGestures.kt`.
   - `CameraTestTags.COUNTDOWN_TAP = "camera_countdown_tap"` (thêm vào `CameraScreen.kt`).
7. **`PosePicker(enabled: Boolean = true)`:** truyền cho `CategoryChips` và `PoseStrip`; hai `LazyRow` có `userScrollEnabled = enabled` và `.alpha(if (enabled) 1f else DISABLED_ALPHA)` (hằng riêng của file, 0.38f) trên chính `LazyRow`, như độ mờ của khung "Group chips", "Pose strip" trên thiết kế; mỗi `StickerChip` và `PoseThumb` nhận `enabled = enabled`. Không đặt độ mờ trên từng chip hay ô: `Modifier.alpha` là `graphicsLayer(alpha, clip = true)`, cắt dấu chọn `SelectedBadge` vẽ ra ngoài chip (lệch 8, -9 dp) và ngoài ô (7 dp); trên `LazyRow`, dấu chọn nằm trong lề trên 16 dp của hàng nên không bị cắt (đo ở mục 4.1). Thông báo lỗi tải thư viện ("Thử lại") không đổi.
8. **`:core:designsystem`:** `StickerChip(..., enabled: Boolean = true)` và `PoseThumb(..., enabled: Boolean = true)`: chỉ `selectable(enabled = enabled, ...)`, không đổi độ mờ (nơi gọi tự làm mờ cả hàng, bước 7). Cập nhật KDoc ("[enabled] false: not selectable; the caller fades it"). Mặc định `true`, nên `feature/poses` (`PosesComponents.kt`) và trang mẫu `DesignSystemCatalog.kt` không đổi.

Không cần đề xuất về dự án mẫu: `StickerChip`, `PoseThumb` là thành phần riêng của hệ thống thiết kế sticker của app; quy tắc khóa nút là của màn camera.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraUiState.kt` | `CountdownCancelReason` (bước 1) |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModel.kt` | `onCancelCountdown(reason)`; bỏ qua flash, hẹn giờ, lưới khi đang đếm (bước 1, 2) |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | Kiểu `onCancelCountdown`; `counting`, đóng bảng chỉnh, bọc lối rời màn, truyền `locked`, `enabled`, `onCountdownTap` cho bố cục điện thoại; `CameraTestTags.COUNTDOWN_TAP` (bước 3, 6) |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraUnfolded.kt` | Kiểu `onCancelCountdown`; truyền `locked`, `enabled`, `onCountdownTap`; nút hủy gọi `BUTTON` (bước 1, 3) |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraControls.kt` | Khóa flash, hẹn giờ, lưới; `TimerButton(enabled)`; `PreviewContent`, `ReadyPreview` nhận `onCountdownTap`; lớp nhận chạm `countdownTapArea` (bước 6) |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseOverlay.kt` | `locked`: mũi tên, cột công cụ, `HideableStepCard(enabled)` (bước 4) |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/OverlayTools.kt` | `OverlayToolColumn(enabled)` (bước 5) |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PosePicker.kt` | `PosePicker(enabled)`, hai hàng mờ 0.38 và không cuộn, chip và ô tắt (bước 7) |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/StickerChip.kt` | Tham số `enabled` cho `selectable`, không đổi độ mờ (bước 8) |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PoseThumb.kt` | Tham số `enabled` cho `selectable`, không đổi độ mờ (bước 8) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModelTest.kt` | Test ViewModel ở mục 4 |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenCountdownTest.kt` | Test giao diện khi đang đếm, bố cục điện thoại và màn thấp (mục 4). Lớp riêng để không chạm `CameraScreenPoseTest.kt`, `CameraScreenOverlayTest.kt` mà T-059, T-060 sửa |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenTest.kt` | `setScreen`: `onCancelCountdown = { calls += "cancel $it" }`; `countdown_showsNumber_andShutterCancels` giữ tên, chỉ đổi kỳ vọng thành `"cancel BUTTON"`; test mới `countdown_flipCancelsThenFlips` (mục 4) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenUnfoldedTest.kt` | Test khi đang đếm ở bố cục mở gập; `Screen()` thêm tham số `uiState`, `onCancelCountdown` có mặc định (mục 4). T-059 cũng sửa file này (thêm `onHideSteps` vào `actions`); chạy sau T-059 nên không xung đột; test mới tự ghi lời gọi của nó, không dựa vào `actions` của T-059 |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/PoseComponentsTest.kt` | Test `enabled = false` của `StickerChip`, `PoseThumb` (mục 4) |
| Sửa | `feature/camera/src/test/screenshots/CameraScreen_countdown_dark.png` | Ghi lại theo thiết kế mới (plan cho phép) |
| Sửa | `feature/camera/src/test/screenshots/CameraScreen_countdown_small.png` | Ghi lại theo thiết kế mới (plan cho phép) |
| Sửa | `docs/features/camera/design/review/` | Ảnh ghép và `summary.md` do `scripts/design-compare.sh camera` sinh lại; không sửa tay |

Nơi đọc các file bị đổi (L-020): `grep -rn "CameraScreen_countdown" scripts/ .github/ */src/test` chỉ thấy `CameraScreenScreenshotTest.kt` (đường dẫn của `captureRoboImage`, không đổi). `grep -rl "StickerChip" feature core app` và `grep -rl "PoseThumb(" feature core app` thấy thêm `feature/poses/.../PosesComponents.kt` và `core/designsystem/.../catalog/DesignSystemCatalog.kt`: dùng mặc định `enabled = true`, không đổi; các ảnh chuẩn của `:feature:poses` và `:core:designsystem` không đổi (`:core:designsystem:verifyRoborazziDebug` qua với code thử, mục 4.1). `grep -rn "onCancelCountdown" feature app` và `grep -rn "PreviewContent(" feature app` chỉ thấy các file của mục này và test của `:feature:camera`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm (`photo_captured` không đổi; không thêm sự kiện cho lần dừng đếm).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `awaitEachGesture`, `awaitFirstDown`, `pointerInput` thuộc `androidx.compose.foundation` và `androidx.compose.ui`, đã có trên classpath của `:feature:camera` (dùng ở `pose/OverlayGestures.kt`); `alpha` (`androidx.compose.ui.draw.alpha`) dùng ở `PosePicker.kt` của `:feature:camera`, đã có ở `CameraControls.kt:370`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** lớp nhận chạm có thể vẫn để chạm lọt xuống thẻ bước hay vùng kéo nét viền trên CI (đã đo trên Robolectric `NATIVE`, mục 4.1).
- **Điều kiện chuyển:** test `counting_tapStepCard_cancelsWithoutHiding` hoặc `counting_dragOnFrame_doesNothing` vẫn thất bại vì có `hide` hay `transform` trong `calls` sau 2 lần sửa.
- **Cách làm thay:** giữ lớp nhận chạm, thêm `PoseOverlay(locked)` tắt hẳn bộ nhận chạm bên dưới khi đang đếm: `overlayGestures(enabled = !overlay.settings.hidden && !locked)`, `HideableStepCard` không gắn `pointerInput` vuốt khi `!enabled`. Chỉ sửa `PoseOverlay.kt`, đã có ở mục 3.2. Test giữ nguyên.

## 4. Kiểm thử

Mọi test giao diện mới: `@GraphicsMode(GraphicsMode.Mode.NATIVE)`, `@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")` (khung của thiết kế, nơi phát hiện lỗi) trừ khi ghi khác; trạng thái `COUNTING = Ready(flash = OFF, flashAvailable = true, timer = THREE, gridVisible = true, lens = BACK, zoomLevels = [0.5, 1, 2], zoom = 1f, countdown = 3)`, tư thế `CameraScreenScreenshotTest.samplePoses(...)`; `PoseActions` ghi lời gọi vào `calls` (`category`, `pose`, `previous`, `next`, `hide`, `show`, `transform`, `togglePanel`, `closePanel`, `adjust`, `mirror`, `lock`, `visibility`), `CameraScreen` ghi `flash`, `timer`, `grid`, `flip`, `zoom <tỷ lệ>`, `shutter`, `cancel <lý do>`, `poses`, `settings`, `lastPhoto`. Sau `setContent` gọi `waitForIdle()` rồi `calls.clear()`. Kiểm "không dùng được" bằng `assertIsNotEnabled()` (ngữ nghĩa `Disabled`), kiểm "không phản hồi" bằng `performTouchInput { click(...) }` rồi `calls` rỗng: `performClick()` trên node tắt không gọi gì nên không đủ.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-6, tiêu chí 2 (đã có) | Unit test, Compose UI test | `CameraViewModelTest.cancelCountdown_stopsWithoutCapture` (giữ nguyên, gọi mặc định `BUTTON`); `CameraScreenTest.countdown_showsNumber_andShutterCancels` giữ tên và `assertTextEquals("3")`; chỉ đổi lambda của `setScreen` thành `onCancelCountdown = { calls += "cancel $it" }` và kỳ vọng thành `calls == ["cancel BUTTON"]` (sửa vì kiểu callback đổi theo plan, ý kiểm giữ nguyên) |
| KB-6, mới 1: flash, lưới, hẹn giờ không phản hồi (ViewModel) | Unit test | `CameraViewModelTest.countdown_ignoresFlashTimerGrid`: hẹn giờ 10 giây, bấm chụp, `runCurrent()`; gọi `onFlashClick()`, `onTimerClick()`, `onGridClick()`: `flash`, `timer`, `gridVisible` của `ready()` không đổi, `repository` không ghi gì, `countdown` vẫn 10; đối chứng sau `onCancelCountdown()`: `onTimerClick()` đổi `timer` (L-035) |
| KB-6, mới 1: zoom vẫn dùng được (ViewModel) | Unit test | `CameraViewModelTest.countdown_zoomStillChanges`: đang đếm, `onZoomSelect(2f)`: `ready().zoom == 2f`, `countdown` vẫn khác `null`, `controller.zoomCalls` kết thúc bằng `2f`. Qua ngay ở commit test (đối chứng, chặn hồi quy, L-035) |
| KB-6, mới 1: flash, hẹn giờ, lưới mờ và tắt | Compose UI test | `CameraScreenCountdownTest.counting_topToolsDisabled`: `FLASH`, `TIMER`, `GRID_BUTTON` `assertIsNotEnabled()`; chạm từng nút: `calls` rỗng; `POSES`, `SETTINGS` `assertIsEnabled()`. Đối chứng: cùng màn với `countdown = null`, ba nút `assertIsEnabled()` |
| KB-6, mới 1: độ mờ 0.38 | Compose UI test, đo điểm ảnh (L-006) | `CameraScreenCountdownTest.counting_lockedControlsFaded`: vẽ `decorView` ra `Bitmap`; đọc điểm ảnh ở giữa nền trắng của nút cài đặt (không khóa) và ở giữa nền của nút flash (`flash_off`, nền `camera/control` trắng, mờ 0.38 thành xám khi khóa): flash có độ sáng (luma) dưới 60% của nút cài đặt; điểm giữa chip "Nữ" và ô tư thế đầu tối hơn ảnh cùng điểm khi `countdown = null`; dấu chọn của chip "Tất cả" ngoài mép chip, ở (mép phải + 4 dp, mép trên - 5 dp) của `boundsInWindow` (điểm nằm trên viền trắng của dấu): đang đếm có kênh đỏ trong 60..150 (đo 103: trắng mờ 0.38 trên nền tối, không bị cắt; nền là khoảng 10), không đếm thì trên 200 (đo 255). Không đo điểm trong khung (lớp mờ 35% đã làm tối) |
| KB-6, mới 1: hàng nhóm, dải tư thế không phản hồi | Compose UI test | `CameraScreenCountdownTest.counting_chipsAndStripDisabled`: chip "Nữ" và `pose("p3")` `assertIsNotEnabled()`; chạm chúng: không có `category`, `pose` trong `calls`; vuốt ngang `STRIP` 200 dp: `firstVisibleItemIndex` của dải không đổi (đọc qua `onNodeWithTag(pose("p1"))` vẫn hiện ở vị trí cũ, `getUnclippedBoundsInRoot().left` không đổi) |
| KB-6, mới 1: cột công cụ, mũi tên thẻ bước tắt | Compose UI test | `CameraScreenCountdownTest.counting_toolsAndArrowsDisabled`: `TOOL_ADJUST`, `TOOL_MIRROR`, `TOOL_LOCK`, `TOOL_VISIBILITY`, "Bước trước", "Bước sau" `assertIsNotEnabled()`. Đối chứng: `countdown = null`, "Bước sau" và `TOOL_ADJUST` `assertIsEnabled()` (bước 2 trên 4 nên cả hai mũi tên dùng được) |
| KB-6, mới 1: cột công cụ ngoài khung trên màn thấp | Compose UI test, `@Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")` | `CameraScreenCountdownTest.small_counting_toolsDisabled`: `TOOL_ADJUST` `assertIsNotEnabled()` (cột trong hay ngoài khung, cả hai nhánh của `CameraPhoneLayout`) |
| KB-6, mới 1: bảng chỉnh đóng | Compose UI test | `CameraScreenCountdownTest.counting_closesOpenPanel`: `samplePoses(overlay = CameraScreenScreenshotTest.ADJUSTING)` và `COUNTING`: `calls == ["closePanel"]` (không gọi `calls.clear()` trước khi đọc); đối chứng: `ADJUSTING` với `countdown = null`: không có `closePanel` |
| KB-6, mới 1: kéo, chụm không đổi nét viền | Compose UI test | `CameraScreenCountdownTest.counting_dragOnFrame_doesNothing`: `swipe` trên `OVERLAY_GESTURES` từ (0.3, 0.6) tới (0.3, 0.8) chiều cao: `calls` rỗng (không `transform`, không `cancel`); `counting_pinchOnFrame_doesNothing`: hai ngón `pinch` trên `PREVIEW`: `calls` rỗng |
| KB-6, mới 1: thẻ bước không ẩn bằng trình đọc màn hình khi đang đếm | Compose UI test | `CameraScreenCountdownTest.counting_stepCardHasNoHideAction`: ngữ nghĩa của `STEP_CARD` không có `CustomActions`, và không có `OnClick` hoặc có `Disabled`; đúng với cả ba trường hợp của mục 3.1 bước 4 (`clickable` của T-059, `semantics { onClick }` của phương án dự phòng T-059, `customActions` khi T-059 chưa merge); đối chứng `countdown = null`: có một cách ẩn (`OnClick` không `Disabled`, hoặc `CustomActions` có nhãn "Ẩn thẻ bước") |
| KB-6, mới 2: chạm khung dừng đếm | Compose UI test | `CameraScreenCountdownTest.counting_tapFrame_cancels`: chạm `PREVIEW` ở (0.2, 0.7): `calls == ["cancel PREVIEW"]` |
| KB-6, mới 2: chạm thẻ bước dừng đếm, không ẩn thẻ (plan T-059, mục 6) | Compose UI test | `CameraScreenCountdownTest.counting_tapStepCard_cancelsWithoutHiding`: chạm `STEP_CARD` ở (0.3 chiều rộng, giữa): `calls == ["cancel PREVIEW"]` |
| KB-6, mới 2: chạm nét viền, cột công cụ dừng đếm | Compose UI test | `CameraScreenCountdownTest.counting_tapOutlineOrTool_cancels`: chạm giữa `OUTLINE` (ngoài vòng số, ở 0.5 chiều rộng, 0.15 chiều cao của nét viền) rồi chạm `TOOL_ADJUST`: `calls == ["cancel PREVIEW", "cancel PREVIEW"]` |
| KB-6, mới 1, 2: zoom dùng được, không dừng đếm | Compose UI test | `CameraScreenCountdownTest.counting_zoomStillWorks`: bấm chữ "2": `calls == ["zoom 2.0"]`. Qua ngay ở commit test (đối chứng: chạm hiện nay đi thẳng tới zoom, L-035); chặn hồi quy cho lớp nhận chạm |
| KB-6, mới 2: rời màn dừng đếm rồi mở màn | Compose UI test | `CameraScreenCountdownTest.counting_leaveCancelsThenOpens`: bấm `POSES`, `SETTINGS`, `LAST_PHOTO` (với `onLastPhotoClick`): `calls == ["cancel LEAVE", "poses", "cancel LEAVE", "settings", "cancel LEAVE", "lastPhoto"]`; đối chứng `countdown = null`: `["poses"]` |
| KB-6, mới 2: đổi camera dừng đếm và đổi | Unit test, Compose UI test | `CameraViewModelTest.countdown_flipCancelsAndSwitches` (mới; chưa có test đổi camera khi đang đếm): hẹn giờ 10 giây, bấm chụp, `runCurrent()`, `onFlipClick()`, `runCurrent()`: `countdown == null`, `controller.openCalls == [BACK, FRONT]`, log có "Countdown cancelled: flip"; `advanceTimeBy(20_000)`: `controller.captureCount == 0`. Qua ngay ở commit test (hành vi đã có ở `CameraViewModel.kt:163`, chặn hồi quy). `CameraScreenTest.countdown_flipCancelsThenFlips`: đang đếm, `FLIP` `assertIsEnabled()`, bấm: `calls == ["flip"]` (giao diện chỉ gọi `onFlipClick`; dừng đếm do `CameraViewModel.onFlipClick`, kiểm ở test ViewModel cùng dòng) (qua ngay) |
| KB-6, mới 2: log lý do | Unit test | `CameraViewModelTest.cancelReasons_logged`: đếm, `onCancelCountdown(PREVIEW)`: log `Camera` "Countdown cancelled: preview"; lần nữa với `LEAVE`: "Countdown cancelled: leave"; với mặc định: "Countdown cancelled: user" |
| KB-6, mới 1, 2: bố cục mở gập | Compose UI test, `CameraScreenUnfoldedTest` (khung của lớp đó) | `CameraScreenUnfoldedTest.counting_locksAndTapCancels` (hàm `Screen()` của lớp đang viết cứng `uiState = DEFAULT`, `onCancelCountdown = {}`: thêm hai tham số `uiState: CameraUiState = DEFAULT`, `onCancelCountdown: (CountdownCancelReason) -> Unit = {}` có mặc định, chỉ vậy, để các test cũ và phần `onHideSteps` của T-059 không đổi; test mới truyền `COUNTING` và `{ calls += "cancel $it" }`): `FLASH` và chip "Nữ" `assertIsNotEnabled()`, chạm `PREVIEW`: có `cancel PREVIEW`; bấm zoom "2" ở cột phải: có `zoom 2.0` |
| `StickerChip`, `PoseThumb` tắt | Compose UI test | `PoseComponentsTest.stickerChip_disabled_notSelectable`, `poseThumb_disabled_notSelectable`: `enabled = false`: `assertIsNotEnabled()`, chạm không gọi `onClick`; đối chứng `enabled = true` gọi một lần |
| Giao diện màn đếm ngược | Screenshot test | `CameraScreenScreenshotTest.countdown_dark`, `countdown_small` (đã có): ghi lại `CameraScreen_countdown_dark.png`, `CameraScreen_countdown_small.png` bằng `./gradlew :feature:camera:recordRoborazziDebug --tests '*CameraScreenScreenshotTest.countdown*'` sau khi code xong |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` | `CameraScreen_countdown_small.png` (ghi lại); `CameraScreenTest.small_countdown_shutterVisibleAndLargeEnough` giữ nguyên |
| Đúng thiết kế | Screenshot test cùng tên với ảnh thiết kế, `scripts/design-compare.sh camera` sau `check.sh`, commit `docs/features/camera/design/review/` cùng PR, dán `review/summary.md` vào mô tả PR (không chặn merge) | 18 ảnh `CameraScreen_*`, `ReviewScreen_*` của `docs/features/camera/design/screens/`; chỉ `CameraScreen_countdown_dark` đổi, tỷ lệ khác biệt khoảng 5,7% (đo ở mục 4.1, lần 2; trước task 6,0% so với thiết kế cũ) |
| Ảnh thiết kế của feature camera mà task không đổi | Screenshot test đã có, `verifyRoborazziDebug` không lệch (mục 7, điều kiện 1) | Ảnh thiết kế của ticket khác (T-013): `CameraScreen_starting_dark`, `CameraScreen_permission-denied_dark`, `CameraScreen_error_dark`. Ảnh thiết kế của ticket khác (T-014): `CameraScreen_default_dark`, `CameraScreen_no-pose_dark`, `CameraScreen_steps-hidden_dark`. Ảnh thiết kế của ticket khác (T-015): `CameraScreen_adjust_dark`, `CameraScreen_locked_dark`. Ảnh thiết kế của ticket khác (T-017): `CameraScreen_matching_dark`, `CameraScreen_matched_dark`, `CameraScreen_no-person_dark`. Ảnh thiết kế của ticket khác (T-020): `CameraScreen_default_dark_unfolded`, `ReviewScreen_default_dark_unfolded`. Ảnh thiết kế của ticket khác (T-016): `ReviewScreen_default_dark`, `ReviewScreen_saving_dark`, `ReviewScreen_save-error_dark`, `ReviewScreen_discard_dark` |
| Chạm thật khi đang đếm, camera thật | Kiểm tra thủ công (máy thật: camera thật) | Trên bản staging, bật hẹn giờ 10 giây, bấm chụp: (1) flash, hẹn giờ, lưới, hàng nhóm, dải tư thế, cột công cụ, mũi tên mờ, bấm không có gì; (2) kéo nét viền: không di chuyển, vẫn đếm; (3) chạm khung: dừng, không chụp; (4) bấm thư viện khi còn 1 giây: dừng, mở thư viện, quay lại không mở màn xem lại; (5) zoom 2: đổi zoom, vẫn đếm; log `VGL.Camera` "Countdown cancelled: preview", "leave". Emulator CI không có camera (FB-20261026-01, plan T-058, câu 2) nên không viết kịch bản emulator |
| Trình đọc màn hình khi đang đếm | Kiểm tra thủ công (máy thật: TalkBack) | Bật TalkBack, đếm 10 giây: nút flash, chip, ô tư thế, mũi tên đọc "đã tắt" (disabled); nút "Hủy đếm ngược" nhấn đúp thì dừng |

**Ngoại lệ viết test trước:** (1) Hai ảnh chuẩn `CameraScreen_countdown_dark.png`, `CameraScreen_countdown_small.png` ghi lại ở commit code của bước 4, sau khi code xong; `verifyRoborazziDebug` trên commit test vẫn qua với ảnh cũ. (2) `docs/features/camera/design/review/` sinh lại sau `check.sh`. (3) Phần khung để test biên dịch, vào commit test của bước 3 (`docs/auto-run.md`, bước 3; L-025): enum `CountdownCancelReason`, `CameraViewModel.onCancelCountdown(reason = BUTTON)` bỏ qua `reason` (log "user" như cũ), kiểu mới của `onCancelCountdown` ở `CameraScreen`, `CameraPhoneLayout`, `CameraUnfoldedLayout` (truyền `BUTTON` cho nút hủy), `CameraTestTags.COUNTDOWN_TAP`, tham số `enabled` của `StickerChip`, `PoseThumb` chưa dùng. Với khung này, các test ở bảng trên thất bại ở phần kiểm tra (không ở biên dịch), trừ các test ghi "qua ngay"; `countdown_showsNumber_andShutterCancels` (đã sửa kỳ vọng) qua ngay (khung đã truyền `BUTTON`).

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-26, trên nhánh `plan/T-061` (gốc 16e9019, nhánh chính sau PR #167), một tệp test tạm không commit `feature/camera/src/test/.../camera/T061ProbeTest.kt` với 10 test ở `vi-w411dp-h891dp-xxhdpi`, `NATIVE`, tư thế `samplePoses()`, `countdown = 3`:
  - Code hiện tại (`./gradlew :feature:camera:testDebugUnitTest --tests '*T061ProbeTest*'`): 9 thất bại, 1 qua, đúng lý do:
    - flash `assertIsNotEnabled` thất bại (nút đang bật); mũi tên "Bước sau", chip "Nữ" (`Role = 'Tab'`), `TOOL_ADJUST` cũng vậy;
    - chạm thẻ bước ở 0.3 chiều rộng: `expected [cancel] but was []` (thẻ chỉ ẩn bằng vuốt, chạm không làm gì);
    - chạm khung ở (0.2, 0.7): `expected [cancel] but was []`;
    - kéo trên `OVERLAY_GESTURES`: `calls=[transform x 12]` (nét viền di chuyển khi đang đếm);
    - bấm `POSES`: `expected [cancel, poses] but was [poses]`;
    - bảng chỉnh mở khi đếm: `calls=[]` (không đóng);
    - bấm zoom "2": qua (`[zoom 2.0]`), đối chứng.
  - Code thử theo mục 3.1 (bước 3 đến 8, chưa có `CameraUnfolded.kt`, lý do dừng đếm và phần ViewModel): lần đầu lớp nhận chạm dùng `detectTapGestures(onTap = ...)`: 9 qua, kéo thất bại `calls=[cancel]`, vì `detectTapGestures` không hủy khi ngón tay kéo trong vùng. Đổi sang `countdownTapArea` (mục 3.1, bước 6): cả 10 qua.
- **Lệnh:** `./gradlew :feature:camera:testDebugUnitTest --tests '*T061ProbeTest*'`; với code thử: `./gradlew :feature:camera:verifyRoborazziDebug :core:designsystem:verifyRoborazziDebug --continue` (408 test của `:feature:camera` chạy, chỉ 2 ảnh lệch: `CameraScreen_countdown_dark.png`, `CameraScreen_countdown_small.png`; Roborazzi báo lỗi dưới tên test khác trong cùng lớp, `matching_dark` và `permissionDenied_small`, nhưng thông báo ghi đúng hai tệp trên; `:core:designsystem` qua hết, ảnh chuẩn không đổi); rồi `recordRoborazziDebug --tests '*CameraScreenScreenshotTest.countdown*'` và `scripts/design-compare.sh camera`: `CameraScreen_countdown_dark` 5,8% (trước 6,0%), các ảnh khác không đổi. Ảnh ghi lại đã xem: flash, hẹn giờ, lưới, cột công cụ, mũi tên, chip, ô tư thế mờ; thư viện, cài đặt, zoom, nút hủy, đổi camera giữ nguyên, như `design/screens/CameraScreen_countdown_dark.png`. Không chạy `scripts/check.sh`: tiêu chí E là 1 (mục 5), không bắt buộc.
- **Chạy thử lần 2 (sau rà soát độc lập):** bản đầu đặt `.alpha(0.38)` trên `StickerChip`, `PoseThumb` cắt dấu chọn của "Tất cả" và ô `p2` (ảnh ghi thử thấy dấu bị cắt mép). Đổi theo mục 3.1 bước 7, 8 (độ mờ trên hai `LazyRow`, lõi chỉ có `selectable(enabled)`), thêm 2 test tạm đo điểm ảnh dấu chọn: đang đếm kênh đỏ 103 (qua), không đếm 255 (đối chứng, qua); 12 test tạm qua; ghi lại hai ảnh chuẩn đếm ngược, xem ảnh: dấu chọn nguyên vẹn, mờ cùng hàng; `design-compare.sh camera`: `CameraScreen_countdown_dark` 5,7%; `./gradlew :feature:camera:verifyRoborazziDebug :core:designsystem:verifyRoborazziDebug :feature:poses:verifyRoborazziDebug --continue` qua hết với hai ảnh mới (ảnh của `:core:designsystem`, `:feature:poses` không đổi).
- **Kết quả:** cách làm chạy được trên Robolectric; đã bỏ code thử, test tạm, ảnh ghi thử và `design/review/` sinh thử (`git status` sạch). Phương án dự phòng (mục 3.4) chưa cần.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7, module core dùng chung: plan thêm tham số `enabled` (chỉ cho `selectable`) cho `StickerChip` và `PoseThumb` của `:core:designsystem`. Vì chạm điểm chặn, `risk_score` để trống (`risk-scoring.md`, như T-013 đến T-019); bảng dưới chỉ để tham khảo. Mặc định `true`, nên `:feature:poses` và trang mẫu không đổi. Không chạm điểm 2 (không thêm thư viện), 3 (không thêm quyền), 4 (không có sự kiện). Người duyệt thấy khác thì sửa `blocking_points`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:feature:camera`, `:core:designsystem`), đổi chữ ký công khai của `StickerChip`, `PoseThumb` (thêm tham số có mặc định) và của `CameraScreen` |
| B. Độ rõ của yêu cầu | 0 | Spec KB-6 đã xác nhận (2026-10-26), thiết kế đã xuất lại (PR #167); hai điểm AI tự quyết ở mục 2 là chi tiết nhỏ (câu hỏi 1, 2) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi tiêu chí có test JVM; chạm thật trên camera thật và TalkBack kiểm thủ công |
| D. Ảnh hưởng chéo | 2 | `PoseOverlay.kt` cũng do T-059, T-060 sửa trong cùng danh sách; `CameraScreen.kt` do T-060 đọc |
| E. Độ mới của kỹ thuật | 1 | Bộ nhận chạm `awaitEachGesture` là biến thể của `pose/OverlayGestures.kt`; `enabled` và độ mờ 0.38 như `CameraIconButton`; đã chạy thử |
| **Tổng** | (7, chỉ để tham khảo) | **Mức: Cao vì điểm chặn 7** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `StickerChip`, `PoseThumb` của `:core:designsystem` (dùng ở `:feature:poses` và trang mẫu, mặc định không đổi). `PreviewContent` dùng ở cả hai bố cục của camera.
- Task khác trong sprint có thể xung đột:
  - T-060 (`PoseOverlay.kt`, lớp nét viền) và T-059 (`PoseOverlay.kt`, `HideableStepCard`): plan này sửa chỗ khác trong cùng file (tham số `locked`, mũi tên, cột công cụ, `enabled` của thẻ). Chạy sau hai task đó. Nếu một trong hai dừng, bước 4 có nhánh "chưa có T-059"; T-060 không ảnh hưởng tới phần của plan này.
  - T-058 (`CameraXController.kt`, bộ nhận dạng dáng): không chung file.
  - Test: plan tạo lớp mới `CameraScreenCountdownTest.kt` để không chạm `CameraScreenPoseTest.kt`, `CameraScreenOverlayTest.kt` của T-059, T-060. `CameraScreenUnfoldedTest.kt` cũng do T-059 sửa (thêm `onHideSteps` vào `actions`): chạy sau nên không xung đột; test mới của plan này tự ghi lời gọi, không dựa vào phần T-059 thêm.
- Đề xuất thứ tự chạy: T-058, T-060, T-059, T-061 (cuối cùng).

## 7. Điều kiện dừng riêng

1. `verifyRoborazziDebug` báo lệch ảnh chuẩn nào khác `CameraScreen_countdown_dark.png`, `CameraScreen_countdown_small.png` (của `:feature:camera`, `:feature:poses` hay `:core:designsystem`): dừng, không ghi lại ảnh đó. Nhận ảnh lệch theo tên tệp trong thông báo ("<tệp>.png is changed") hay theo `build/outputs/roborazzi/*_compare.png`, không theo tên test: Roborazzi có thể báo lỗi dưới tên test khác của cùng lớp (đo ở mục 4.1). Hai ảnh đếm ngược chụp tĩnh `Screen(COUNTDOWN)`, không có hiệu ứng chạy mãi (không thuộc L-029).
2. Test hiện có nào thất bại sau thay đổi, trừ `CameraScreenTest.countdown_showsNumber_andShutterCancels` với đúng phần sửa của mục 4 (lambda `"cancel $it"`, kỳ vọng `"cancel BUTTON"`): dừng.
3. `design-compare.sh` cho `CameraScreen_countdown_dark` tỷ lệ khác biệt trên 10%: không dừng, ghi vào "Cần người kiểm tra" của báo cáo.

## 8. Câu hỏi cho người duyệt

1. **Kéo hay chụm trên khung khi đang đếm có dừng đếm không?** Spec ghi "chạm" thì dừng và "kéo và chụm không đổi nét viền". Đề xuất của AI: kéo, chụm không dừng đếm, không làm gì (vẫn đếm); chỉ chạm nhẹ (một ngón, không kéo) mới dừng, để người đang tạo dáng lỡ quệt tay không mất lượt đếm. Phương án khác: mọi lần chạm xuống đều dừng đếm (đơn giản hơn, dễ dừng nhầm hơn).
2. **Nút "Bước" (thẻ bước đang ẩn) khi đang đếm:** spec không nhắc. Đề xuất của AI: không làm mờ (thiết kế không có trạng thái này); chạm bằng tay là chạm vào khung (dừng đếm); trình đọc màn hình vẫn bấm được để hiện lại thẻ, vì việc này không đổi ảnh sắp chụp. Lưu ý: như vậy trình đọc màn hình hiện lại được thẻ trong lúc đếm nhưng không ẩn được thẻ (thẻ tắt, mục 3.1 bước 4); nếu muốn đối xứng thì tắt cả nút "Bước" khi đang đếm.
3. **Sửa `:core:designsystem` (điểm chặn 7, rủi ro Cao) hay chỉ sửa trong `:feature:camera`?** Không sửa module core thì chip và ô tư thế chỉ mờ và bị chặn chạm, còn trình đọc màn hình vẫn đọc là dùng được và nhấn đúp không có tác dụng. Đề xuất của AI: thêm tham số `enabled` (mặc định `true`, chỉ truyền vào `selectable`, không đổi độ mờ) cho `StickerChip`, `PoseThumb`; độ mờ đặt ở hàng trong `PosePicker`; duyệt chéo theo mức Cao.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-26, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Câu 1: kéo, chụm không dừng đếm. Câu 2: không làm mờ nút "Bước". Câu 3: thêm `enabled` cho `StickerChip`, `PoseThumb`.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261026-03, phiên bản plan 1

- **Kết quả:** đã merge (PR #173, commit merge ed7f18d, 2026-10-26 14:12 UTC).
- **Nhánh, PR:** `claude/T-061-khoa-nut-dem-nguoc`, https://github.com/VGL-Studio/camera-pose/pull/173. Commit: `c6b5da1` (test và khung), `c5a786c` (code, hai ảnh chuẩn), `88e78de` (`design/review/`).
- **Đã làm:** theo mục 3.1, bước 1 đến 8, nhánh "T-059 theo phương án chính" ở bước 4 (`clickable(enabled = enabled, ...)`); 19 file của mục 3.2 cộng `design/review/` (`CameraScreen_countdown_dark_compare.png`, `summary.md`). Ghi lại `CameraScreen_countdown_dark.png`, `CameraScreen_countdown_small.png` (plan cho phép); trước khi ghi, `verifyRoborazziDebug` của `:feature:camera`, `:core:designsystem`, `:feature:poses` chỉ lệch đúng hai tệp này. Bước 3 đến 5 giao phiên con trong worktree `../task-T-061` (L-031).
- **Test viết trước:** 22 test mới, 1 test sửa (`countdown_showsNumber_andShutterCancels`, đúng phần mục 4 cho phép). 18 thất bại ở phần kiểm tra, đúng lý do (nút khóa còn bật; kéo, chụm ra `transform`, `gestureEnd`; chạm khung `[]`; chạm thẻ `[hide]`; chạm nét viền rồi cột công cụ `[togglePanel]`; bảng chỉnh không đóng; rời màn `[poses, settings, lastPhoto]`; flash độ sáng 255; ViewModel đổi flash khi đang đếm; log "user" thay vì "preview"). 4 qua ngay đúng như plan: `countdown_zoomStillChanges`, `countdown_flipCancelsAndSwitches`, `countdown_flipCancelsThenFlips`, `counting_zoomStillWorks`; test sửa qua ngay như plan. Viết chặt lại (L-035): 0.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 8 ghi chú: (1) `counting_closesOpenPanel` dựng không đếm rồi chuyển sang đếm (điều kiểm không đổi, giống dòng chạy thật hơn); (2) test ghi thêm `onOverlayGestureEnd` (chặt hơn); (3) điểm đo chip, ô cách mép trái 6 dp và thân `onCancelCountdown` dạng khối không đổi điều kiểm; (4) `CameraScreenUnfoldedTest.counting_locksAndTapCancels` dùng `in calls`, chưa chứng minh zoom ở cột phải không gửi thêm `cancel` (zoom ngoài khung, rủi ro thấp; sửa thì qua ngay nên không đủ điều kiện nên sửa); (5) `CameraTestTags.COUNTDOWN_TAP` chưa test nào dùng; (6) chặn chạm dựa vào cách Compose giao chạm cho anh em trên cùng, bộ nhận kéo bên dưới vẫn còn (dự phòng 3.4 chưa cần), kiểm thủ công trên máy thật; (7) `leave()` tạo lambda mỗi lần vẽ lại; (8) ảnh `_small`: thẻ bước 3 dòng, mũi tên "<" dưới vòng số (bố cục màn thấp có sẵn).
- **Kết quả CI:** `pr-check` lần chạy 40335161637 xanh (`ui-test` 13:57 đến 14:08, `build-and-test` 13:57 đến 14:12), một vòng. Cục bộ `scripts/check.sh`: 1168 test JVM, 0 lỗi (`:feature:camera` 444, `:core:designsystem` 116), 97 ảnh. `scripts/design-compare.sh camera`: `CameraScreen_countdown_dark` 5,7% (trước 6,0% với thiết kế cũ, 9,6% với thiết kế mới trước task); ảnh khác không đổi.
- **Lệch so với plan:** bốn lệch nhỏ không đổi điều test kiểm, ghi ở rà soát (1) đến (3): thân `onCancelCountdown` dạng khối vì ktlint; ghi thêm `onOverlayGestureEnd`; điểm đo chip, ô cách mép trái 6 dp (tránh chữ, hình); `counting_closesOpenPanel` dựng không đếm trước. Phương án dự phòng 3.4 không dùng. Không có file nối chỗ gọi ngoài mục 3.2.
- **Việc còn lại:** Không có.
- **Cần người xem:** hai ảnh chuẩn mới `feature/camera/src/test/screenshots/CameraScreen_countdown_dark.png`, `CameraScreen_countdown_small.png` và ảnh so sánh `docs/features/camera/design/review/CameraScreen_countdown_dark_compare.png`; kiểm tra thủ công trên máy thật (plan mục 4): hẹn giờ 10 giây, các nút mờ và bấm không có gì, kéo nét viền không di chuyển, chạm khung dừng, bấm thư viện khi còn 1 giây dừng rồi mở thư viện, zoom vẫn đổi; TalkBack đọc "đã tắt", "Hủy đếm ngược" dừng.
