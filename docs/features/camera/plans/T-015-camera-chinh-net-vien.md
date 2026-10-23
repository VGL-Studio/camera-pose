---
ticket: T-015
title: Camera: chỉnh nét viền
status: done             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 35M          # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [analytics, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-015: CAMERA: CHỈNH NÉT VIỀN

## 1. Tóm tắt

Thêm vào camera của T-014 phần chỉnh nét viền: kéo một ngón để dời, chụm hai ngón để đổi cỡ (40% đến 160%), cột công cụ bốn nút (chỉnh, lật, khóa, hiện tư thế) và bảng chỉnh `AdjustPanel` (thanh trượt "Độ mờ", "Cỡ", nút Lật, Khóa, Ẩn, Đặt lại), trạng thái khóa với lời nhắc "Đã khóa lớp phủ", nét viền luôn còn trong khung, lựa chọn giữ khi đổi tư thế và giữa các lần mở app, sự kiện `overlay_adjusted` một lần mỗi thao tác. Trạng thái nằm trong `PoseViewModel` có sẵn; `AdjustPanel` là thành phần mới của `:core:designsystem`. Kiểm chứng bằng unit test ViewModel, repository và hàm tính vị trí, test giao diện Robolectric (kéo, chụm, thanh trượt, khung thấp), screenshot test cùng tên hai ảnh thiết kế mới, ghi lại các ảnh camera có cột công cụ, ảnh so sánh `scripts/design-compare.sh camera`.

`token_estimate` 35M thay cho số gợi ý 15.4M của `scripts/token-usage.py estimate` (trung vị cỡ M): ba task camera gần nhất đều tốn khoảng 40M (T-014 40.7M, T-016 39.7M, T-025 cỡ M 37.7M, `docs/metrics/token-usage.csv`); task này nhỏ hơn T-014 một chút (khoảng 40 file, một thành phần core).

## 2. Hiểu yêu cầu

Ticket xử lý KB-3 trong `features/camera/spec.md`, và việc T-014 giao lại (plan T-014, câu 12: tham số công tắc của `CameraIconButton`):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-3 | Khi kéo một ngón trên khung xem trước thì nét viền di chuyển theo ngón tay | Kéo ở bất kỳ chỗ nào của khung mà không có nút hay thẻ (thẻ bước, cột công cụ, bảng chỉnh, zoom nhận chạm trước). Vị trí lưu theo phần của khung (`offsetX`, `offsetY` từ -0.5 đến 0.5 so với tâm), để giữ đúng chỗ khi khung đổi cỡ (máy gập, T-020) |
| KB-3 | Khi chụm hoặc mở hai ngón thì nét viền nhỏ lại hoặc to lên, trong khoảng 40% đến 160% | Cỡ nhân theo tỷ lệ chụm, chặn trong `[0.4, 1.6]`; phóng quanh tâm nét viền. Cùng một giá trị với thanh trượt "Cỡ" |
| KB-3 | Khi bấm nút chỉnh thì bảng chỉnh hiện, có "Độ mờ" (20% đến 100%), "Cỡ" (40% đến 160%), bốn nút Lật, Khóa, Ẩn, Đặt lại; nút chỉnh đang bật. Bấm dấu đóng hoặc bấm lại nút chỉnh thì bảng đóng | `panelOpen` trong trạng thái, không lưu. Khi bảng mở, zoom ẩn (ảnh `adjust` không có zoom). Giá trị hiện bằng `%1$d%%` (làm tròn tới 1%). Thanh trượt bước liên tục, không chia nấc |
| KB-3 | Khi bấm Lật thì nét viền lật ngang | `mirrored` đảo; vẽ bằng `graphicsLayer(scaleX = -scale)`. Nút lật ở cột công cụ đang bật khi đang lật (mục 8, câu 5) |
| KB-3 | Khi bấm Khóa thì kéo và chụm không đổi nét viền; nút khóa đang bật; nút chỉnh và nút lật mờ đi; lời nhắc "Đã khóa lớp phủ" hiện trên khung. Bấm lại thì mở khóa | `locked`: cử chỉ bị bỏ qua; nút chỉnh, nút lật mờ (độ mờ 38%, không bấm được, `enabled = false`); biểu tượng `lock` thay `lock_open`. Khóa từ bảng chỉnh thì bảng đóng (vì nút chỉnh mờ, mục 8, câu 3). Lời nhắc hiện 2 giây sau khi khóa, và 2 giây mỗi lần kéo hay chụm trong lúc khóa (mục 8, câu 2) |
| KB-3 | Khi bấm Ẩn (hoặc nút hiện tư thế ở cột công cụ) thì nét viền ẩn; bấm lại thì hiện. Nút hiện tư thế đang bật khi nét viền đang hiện | `hidden`; nút ở cột: biểu tượng `visibility` nền vàng khi hiện, `visibility_off` nền trắng khi ẩn. Nút Ẩn trong bảng: biểu tượng `visibility_off`, đang bật khi ẩn. Khi ẩn, kéo và chụm không làm gì (không có gì để kéo, mục 8, câu 4). Thẻ bước không đổi |
| KB-3 | Khi bấm Đặt lại thì nét viền về giữa khung, cỡ 100%, độ mờ mặc định, không lật | `offset = 0`, `scale = 1`, `opacity = DEFAULT_OPACITY`, `mirrored = false`. Không đổi `locked`, `hidden` (spec không nêu). Độ mờ mặc định 100% (mục 8, câu 1) |
| KB-3 | Khi đổi sang tư thế khác thì độ mờ, cỡ, lật, khóa, ẩn giữ như trước; vị trí kéo cũng giữ | Trạng thái chỉnh không gắn với tư thế: `select()` của `PoseViewModel` không chạm nó |
| KB-3 | Mỗi lần chỉnh xong một thao tác thì ghi `overlay_adjusted` một lần, không ghi liên tục lúc kéo | Kéo, chụm: ghi khi nhấc hết ngón, `type` `size` nếu cử chỉ có chụm, ngược lại `move` (nếu có dời). Thanh trượt: khi thả (`onValueChangeFinished`), `opacity` hoặc `size`. Nút: `flip`, `lock` (cả khóa và mở khóa), `hide` (cả ẩn và hiện, từ cột hay bảng), `reset`. Mở, đóng bảng không ghi |
| KB-3, đặc biệt | Khi kéo ra sát mép thì luôn còn ít nhất một phần nét viền trong khung | Tâm hộp nét viền luôn trong khung: `offsetX`, `offsetY` chặn trong `[-0.5, 0.5]`, nên ít nhất một phần tư hộp nét viền nằm trong khung ở mọi cỡ |
| Spec mục 6 | Độ mờ, cỡ, lật, khóa, ẩn giữ giữa các lần dùng | Năm khóa DataStore mới (mục 3.3). Vị trí kéo không có trong bảng dữ liệu của spec: chỉ giữ trong lần mở camera này, mở lại app thì về giữa (mục 8, câu 6) |
| Plan T-014, mục 2 | Không có tư thế thì không có công cụ chỉnh nét viền | Cột công cụ, bảng chỉnh, cử chỉ chỉ có khi `selected != null` và camera `Ready` |
| Plan T-014, câu 12 | `CameraIconButton` có `selected` cho cả nút không phải công tắc | Tham số mới `toggleable: Boolean = true`; `false` thì không đặt `selected`. Camera truyền `false` cho thư viện, cài đặt, flash, đổi camera; hẹn giờ, lưới và bốn nút mới giữ `true` |

Thiết kế tham chiếu: `features/camera/design/README.md` (mục 3: cột công cụ nét viền, bảng chỉnh, lời nhắc trên khung); số liệu trong `design/context/CameraScreen_adjust_dark.md`, `CameraScreen_locked_dark.md`, `CameraScreen_default_dark.md`. Phân chia ảnh:

| Ảnh | Ticket | Ghi chú |
| --- | --- | --- |
| `CameraScreen_adjust_dark`, `CameraScreen_locked_dark` | T-015 (mới) | |
| `CameraScreen_default_dark`, `CameraScreen_steps-hidden_dark`, `CameraScreen_countdown_dark` | T-014, T-015 ghi lại | Thêm cột công cụ (plan T-014, mục 2) |
| `CameraScreen_starting_dark`, `CameraScreen_permission-denied_dark`, `CameraScreen_error_dark`, `CameraScreen_no-pose_dark` | Không đổi | Không có cột công cụ (camera chưa sẵn sàng hoặc không có tư thế) |
| `CameraScreen_matching_dark`, `CameraScreen_matched_dark`, `CameraScreen_no-person_dark` | T-017 | |
| `CameraScreen_default_dark_unfolded`, `ReviewScreen_default_dark_unfolded` | T-020 | |

Số liệu chính (khung 411 x 548, đỉnh khung ở 92):

| Phần | Số liệu |
| --- | --- |
| Cột công cụ | Bốn `CameraIconButton` 48 xếp dọc, cách nhau `Spacing.sm` (cao 216), cách mép phải khung `Spacing.md`, đỉnh ở 150 trong khung. Thứ tự: `tune`, `flip`, `lock_open`/`lock`, `visibility`/`visibility_off` |
| Bảng chỉnh | Rộng khung trừ 2 x `Spacing.md`, cách đáy khung `Spacing.md` (thiết kế: 16, 344, 379 x 280), nền `camera/control`, viền `Stroke.sticker` `camera/onControl`, bo `Radius.sheet` (24), lề `Spacing.md`, các phần cách `Spacing.md` |
| Đầu bảng | "Chỉnh lớp phủ" `titleMedium` màu `camera/onControl`, nút đóng `close` 24 bên phải (vùng chạm 48) |
| Thanh trượt | Cao 52: hàng nhãn 20 ("Độ mờ" bên trái, "70%" bên phải, `labelLarge`), cách `Spacing.xs`, rãnh 28: rãnh chưa chạy cao 8 màu `camera/onSurfaceVariant` bo `full`; phần đã chạy cao 8 màu `secondaryContainer` viền 1 `camera/onControl`; núm tròn 24 nền `camera/control` viền `Stroke.sticker` `camera/onControl` |
| Hàng nút | Bốn cột 48 x 68 chia đều (`SpaceBetween`): nút tròn 48 (như `CameraIconButton`), cách `Spacing.xs`, nhãn `labelMedium` màu `camera/onControl` |
| Lời nhắc khóa | `PreviewHint` của T-014, icon `lock` 20; giữa ngang, đáy lời nhắc cách đỉnh zoom `Spacing.md` (thiết kế: 120, 520, 172 x 40; zoom ở 576) |

ADR liên quan: ADR-0001 (thành phần mới vào `:core:designsystem`), ADR-0008 (không viết cứng màu và cỡ chữ), ADR-0012 (sự kiện qua `:core:analytics`), ADR-0019 (log qua `AppLogger`), ADR-0020 (tên screenshot theo ảnh thiết kế, ảnh so sánh).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo cách T-014 dựng phần tư thế: trạng thái trong `PoseViewModel`, callback trong `PoseActions`, lớp trên khung trong `PoseOverlay.kt`, thành phần chung trong `:core:designsystem` có trang catalog và test đo kích thước, màu điểm ảnh (`pixelAt` như `PoseComponentsTest`, L-006). Thẻ log `Camera`.

**Điều kiện đầu vào.** T-014 đã merge (đã có: `PoseViewModel`, `PoseUiState.Ready`, `PoseActions` trong `PosePicker.kt`, `PoseOverlay`, `PreviewHint`, `PoseOverlaySettingsRepository` với `stepsHidden`). Thiếu thì dừng (mục 7).

**Trạng thái (`pose/PoseUiState.kt`).** `PoseUiState.Ready` thêm trường cuối có giá trị mặc định, nên mọi chỗ dựng `Ready` hiện có vẫn biên dịch:

```kotlin
val overlay: OverlayState = OverlayState(),

/** How the outline is adjusted (spec camera, KB-3). Offsets are parts of the frame from its centre: -0.5 to 0.5. */
data class OverlayState(
    val settings: OverlaySettings = OverlaySettings(),
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val panelOpen: Boolean = false,
    val lockHintVisible: Boolean = false,
)
```

**Phần được lưu (`data/PoseOverlaySettingsRepository.kt`).**

```kotlin
/** The kept part of the outline adjustment (spec camera, section 6). Values out of range are clamped on read. */
data class OverlaySettings(
    val opacity: Float = DEFAULT_OPACITY, // 0.2..1.0
    val scale: Float = 1f,                // 0.4..1.6
    val mirrored: Boolean = false,
    val locked: Boolean = false,
    val hidden: Boolean = false,
)
interface PoseOverlaySettingsRepository {
    val stepsHidden: Flow<Boolean>
    suspend fun setStepsHidden(hidden: Boolean)
    /** OverlaySettings() when nothing is stored. */
    val overlay: Flow<OverlaySettings>
    /** Writes the five values at once. Throws IOException. */
    suspend fun setOverlay(settings: OverlaySettings)
}
```

`DataStorePoseOverlaySettingsRepository`: khóa `camera_overlay_opacity`, `camera_overlay_scale` (float), `camera_overlay_mirrored`, `camera_overlay_locked`, `camera_overlay_hidden` (boolean); đọc lỗi `IOException` thì thử lại như `stepsHidden`; giá trị ngoài khoảng (sửa tay, bản cũ) được chặn lại khi đọc.

**Hàm tính thuần (`pose/OverlayGeometry.kt`).** `MIN_SCALE = 0.4f`, `MAX_SCALE = 1.6f`, `MIN_OPACITY = 0.2f`, `MAX_OPACITY = 1f`, `DEFAULT_OPACITY = 1f`; `clampScale`, `clampOpacity`, `clampOffset` (chặn `[-0.5, 0.5]`); `percent(value): Int` (làm tròn); `toolColumnPlacement(...)` (dưới). Có unit test riêng.

**`PoseViewModel` (sửa).** Thêm các trường `overlay` (phần lưu), `offsetX`, `offsetY`, `panelOpen`, `lockHintVisible`, `gestureMoved`, `gestureScaled`, `lockHintJob`. Lần thư viện sẵn sàng đầu tiên đọc `overlay.first()` cùng chỗ đọc `stepsHidden.first()`. `publish()` thêm `overlay = OverlayState(...)`. Hàm mới:

- `onOverlayTransform(panX: Float, panY: Float, zoom: Float)`: pan là phần của khung. Không có tư thế, hoặc `hidden`: bỏ qua. `locked`: bỏ qua và hiện lời nhắc khóa 2 giây (một lần mỗi cử chỉ). Ngược lại cộng offset (chặn), nhân cỡ (chặn); `zoom != 1` thì `gestureScaled = true`, pan khác 0 thì `gestureMoved = true`. Không lưu, không ghi sự kiện trong lúc kéo.
- `onOverlayGestureEnd()`: `gestureScaled` thì ghi `overlay_adjusted` `size` và lưu; không thì `gestureMoved` ghi `move` (không lưu: vị trí không lưu); đặt lại hai cờ.
- `onOpacityChange(value)`, `onScaleChange(value)`: đổi tại chỗ (chặn), không lưu. `onOpacityChangeFinished()`, `onScaleChangeFinished()`: ghi `opacity` hoặc `size`, lưu.
- `onTogglePanel()`, `onClosePanel()`: đổi `panelOpen`; khi khóa thì không mở.
- `onToggleMirror()`: khi khóa thì bỏ qua; đảo, ghi `flip`, lưu.
- `onToggleLock()`: đảo; khóa thì đóng bảng và hiện lời nhắc 2 giây (`lockHintJob` hủy lần trước); mở khóa thì ẩn lời nhắc; ghi `lock`, lưu.
- `onToggleHidden()`: đảo, ghi `hide`, lưu.
- `onResetOverlay()`: khi khóa thì bỏ qua; về giữa, cỡ 1, độ mờ mặc định, không lật; ghi `reset`, lưu.
- Lưu: `setOverlay(overlay)` trong `viewModelScope`; `IOException` thì log `warn` "Overlay settings could not be saved", lựa chọn vẫn dùng trong lần này (như `setStepsHidden`, plan T-014 câu 15).
- Mỗi sự kiện ghi kèm log `info` "Overlay adjusted: <type>".
- Lời nhắc khóa: `lockHintVisible = true`, `delay(LOCK_HINT_MILLIS = 2_000)`, `false`. Test dùng `advanceTimeBy`.

**`PoseActions` (`pose/PosePicker.kt`, sửa).** Thêm callback có mặc định không làm gì: `onOverlayTransform`, `onOverlayGestureEnd`, `onOpacityChange`, `onOpacityChangeFinished`, `onScaleChange`, `onScaleChangeFinished`, `onTogglePanel`, `onClosePanel`, `onToggleMirror`, `onToggleLock`, `onToggleHidden`, `onResetOverlay`. `CameraRoute` nối vào `PoseViewModel`.

**Cử chỉ (`pose/OverlayGestures.kt`).** `Modifier.overlayGestures(enabled, onTransform, onEnd)`: `pointerInput` với `awaitEachGesture`: `awaitFirstDown(requireUnconsumed = false)`, lặp `awaitPointerEvent()`, bỏ qua sự kiện đã bị con dùng (`isConsumed`), gọi `onTransform(calculatePan() / size, calculateZoom())`, dùng các thay đổi đã xử lý (`consume()`); hết ngón (`changes.none { it.pressed }`) thì gọi `onEnd()` một lần nếu đã có biến đổi. Gắn trên một `Box(fillMaxSize)` vẽ trước thẻ bước, cột công cụ, bảng, zoom, nên chạm vào các thành phần đó không thành cử chỉ kéo.

**Vẽ nét viền (`pose/PoseOverlay.kt`, sửa).** Hộp nét viền giữ cỡ của T-014 (200/411 x 400/548 của khung), thêm `graphicsLayer { translationX = offsetX * frameWidth; translationY = offsetY * frameHeight; scaleX = scale * (if (mirrored) -1 else 1); scaleY = scale; alpha = opacity }` (cả quầng mờ theo). `hidden` thì không vẽ nét viền. Thứ tự vẽ: lớp cử chỉ, nét viền, thẻ bước hoặc nút "Bước", cột công cụ (khi đặt trong khung), bảng chỉnh, lời nhắc khóa. Lời nhắc khóa nằm trên zoom như thiết kế: `PoseOverlay` nhận tham số mới `hintBottomPadding: Dp`, `CameraScreen` truyền `Spacing.md + 48 + Spacing.md` khi zoom nằm trong khung (điện thoại), và `Spacing.md` khi zoom không nằm trong khung (máy gập, T-020 truyền; T-017 đặt `MatchHint` theo cùng tham số). Khi bảng mở thì không có lời nhắc khóa (khóa làm bảng đóng).

**Cột công cụ và bảng (`pose/OverlayTools.kt`, tạo).** `OverlayToolColumn(overlay, actions, modifier)`: bốn `CameraIconButton` (`toggleable = true`): chỉnh (`active = panelOpen`, `enabled = !locked`), lật (`active = mirrored`, `enabled = !locked`), khóa (`active = locked`), hiện tư thế (`active = !hidden`). `OverlayAdjustPanel(overlay, actions, modifier)`: `AdjustPanel` của core với hai `AdjustSlider` và bốn `AdjustAction` (Lật `enabled = !locked`, Khóa `active = locked`, Ẩn `active = hidden`, Đặt lại). Mô tả cho trình đọc màn hình ở mục 3.1, Chữ mới.

**Chỗ đặt cột công cụ ở khung thấp.** Ở khung 360 x 640 (ảnh `CameraScreen_default_small.png` hiện có, đo được): khung còn 222 x 296, thẻ bước hai hàng chiếm 16 đến 140 trong khung, zoom chiếm 232 đến 280; giữa thẻ bước và zoom chỉ còn 92 dp, không đủ cho cột 216. Nhưng khung hẹp hơn màn hình: mỗi bên còn 69 dp trống. Quy tắc (`toolColumnPlacement(containerWidth, frameWidth, frameHeight, stepBottom)`, thuần, có unit test):

1. Lề hai bên khung từ 64 dp (`Spacing.md` + 48) trở lên: cột nằm ở lề phải ngoài khung, giữa lề theo chiều ngang, đỉnh ở `min(150, frameHeight - 216)` so với đỉnh khung.
2. Ngược lại, nếu vừa trong khung: cột cách mép phải `Spacing.md`, đỉnh ở 150, hoặc cao hơn nếu `150 + 216` vượt đáy cho phép (đáy khung trừ `Spacing.md`; trừ thêm zoom 48 và `Spacing.sm` khi khung hẹp dưới 264 dp, vì khi đó zoom chạm ngang cột), nhưng không cao hơn đáy thẻ bước (hoặc nút "Bước") cộng `Spacing.sm`.
3. Không vừa trong khung mà lề từ 48 dp: cột ở lề ngoài khung như cách 1, sát mép khung.
4. Còn lại (dải hẹp, ví dụ màn 360 x 720): cột trong khung, đỉnh ở đáy thẻ bước cộng `Spacing.sm`, được tràn khỏi đáy khung vào 16 dp trống phía trên hàng chip; ghi log `debug` "Overlay tools overflow the frame".

Để đặt được ở lề ngoài khung mà vẫn nhận chạm, cột là con của `Box` chứa khung trong `CameraScreen` (không phải con của khung): `CameraScreen` dùng `BoxWithConstraints` cho hộp đó, tính cỡ khung (3:4, theo chiều cao), truyền vị trí xuống. Chiều cao đáy thẻ bước lấy bằng `onSizeChanged` của thẻ trong `PoseOverlay`, giữ trong `remember` của `CameraScreen` (lần vẽ đầu dùng 84, đáy thẻ một hàng của thiết kế). Ở khung chuẩn 411 x 891 cột rơi vào cách 2, đúng vị trí thiết kế.

**Bảng chỉnh ở khung thấp.** Khung 296 dp cao, bảng 280: bảng cao tối đa khung trừ 2 x `Spacing.md`, phần trong cuộn dọc (`verticalScroll`); bảng vẽ đè lên thẻ bước (mục 8, câu 7).

**`CameraControls.kt` (sửa).** `PreviewContent` và `ReadyPreview` nhận thêm `zoomVisible: Boolean = true` (bảng mở thì `false`). `CameraIconButton` của thư viện, cài đặt, flash, đổi camera thêm `toggleable = false`.

**`CameraScreen.kt` (sửa).** `CameraRoute` nối callback mới. `CameraScreen`: hộp chứa khung thành `BoxWithConstraints`; truyền `zoomVisible = !overlay.panelOpen`; đặt `OverlayToolColumn` theo quy tắc trên khi camera `Ready` và có tư thế. Thẻ test mới trong `CameraTestTags`: `OVERLAY_GESTURES`, `OVERLAY_TOOLS`, `TOOL_ADJUST`, `TOOL_MIRROR`, `TOOL_LOCK`, `TOOL_VISIBILITY`, `ADJUST_PANEL`, `OPACITY_SLIDER`, `SIZE_SLIDER`, `LOCK_HINT`.

**Thành phần mới trong `:core:designsystem` (`component/AdjustPanel.kt`).**

- `AdjustPanel(title: String, onClose: () -> Unit, closeDescription: String, modifier, content: @Composable ColumnScope.() -> Unit)`: như số liệu ở mục 2; nút đóng là `IconButton` vùng chạm 48, icon `close` 24 (`ic_close.xml` mới của core).
- `AdjustSlider(label: String, valueText: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, onValueChange, onValueChangeFinished, modifier)`: `Slider` của Material 3 với `thumb` và `track` tự vẽ theo số liệu; nhãn đọc cho trình đọc màn hình là `label`, giá trị là `valueText` (`stateDescription`).
- `AdjustAction(icon: Painter, label: String, onClick, modifier, active = false, enabled = true)`: cột 48 x 68, nút tròn như `CameraIconButton` (dùng lại `CameraIconButton` với `contentDescription = null`, nhãn bên dưới là chữ đọc được), `Modifier.semantics(mergeDescendants = true)`, `selected = active`.
- `CameraIconButton` (sửa): tham số `toggleable: Boolean = true` ở cả ba hàm; `false` thì không đặt `selected`.
- Catalog: thêm `AdjustPanel` (độ mờ 70%, cỡ 100%, Khóa đang bật, Lật tắt) vào cuối `CameraComponentCatalog`; ghi lại `DesignSystem_camera_components_dark.png`.

**Biểu tượng mới.** Feature: `ic_camera_tune.xml` (`tune`), `ic_camera_mirror.xml` (`flip`), `ic_camera_lock.xml` (`lock`), `ic_camera_lock_open.xml` (`lock_open`), `ic_camera_visibility.xml` (`visibility`), `ic_camera_visibility_off.xml` (`visibility_off`), `ic_camera_reset.xml` (`restart_alt`). Core: `ic_close.xml` (`close`). Material Symbols Outlined, cùng nguồn và ghi chú như biểu tượng của T-013; tải không được là lỗi hạ tầng (auto-run.md, bước 5).

**Chữ mới (`strings.xml` của feature).** Theo bảng chữ của thiết kế: `camera_adjust_title` "Chỉnh lớp phủ", `camera_adjust_opacity` "Độ mờ", `camera_adjust_size` "Cỡ", `camera_adjust_percent` "%1$d%%", `camera_adjust_flip` "Lật", `camera_adjust_lock` "Khóa", `camera_adjust_hide` "Ẩn", `camera_adjust_reset` "Đặt lại", `camera_locked_hint` "Đã khóa lớp phủ". AI đề xuất (mục 8, câu 8): `camera_cd_adjust` "Chỉnh lớp phủ", `camera_cd_mirror` "Lật nét viền", `camera_cd_lock` "Khóa lớp phủ", `camera_cd_unlock` "Mở khóa lớp phủ", `camera_cd_show_outline` "Hiện tư thế", `camera_cd_hide_outline` "Ẩn tư thế", `camera_cd_close_adjust` "Đóng bảng chỉnh".

**Thứ tự làm.** Bước 3: khung rỗng (mục 4), mọi test, `strings.xml` đủ chữ; chạy để thấy thất bại đúng lý do; commit. Bước 4: `AdjustPanel`, `CameraIconButton`, biểu tượng core, catalog; `OverlaySettings`, repository; `OverlayGeometry`; `PoseViewModel`; `OverlayGestures`, `OverlayTools`, `PoseOverlay`; `PoseActions`; `CameraControls`, `CameraScreen`, `CameraRoute`; biểu tượng feature; `scripts/check.sh` ghi ảnh chuẩn; `scripts/design-compare.sh camera`; build `stagingDebug`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseUiState.kt` | `OverlayState`, trường `overlay` của `Ready` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseViewModel.kt` | Đọc, lưu, đổi trạng thái chỉnh; `overlay_adjusted`; lời nhắc khóa |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/OverlayGeometry.kt` | Hằng số, hàm chặn, `percent`, `toolColumnPlacement` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/OverlayGestures.kt` | `Modifier.overlayGestures` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/OverlayTools.kt` | `OverlayToolColumn`, `OverlayAdjustPanel` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseOverlay.kt` | Lớp cử chỉ, biến đổi nét viền, cột công cụ trong khung, bảng chỉnh, lời nhắc khóa, đo đáy thẻ bước |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PosePicker.kt` | `PoseActions` thêm 12 callback |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/data/PoseOverlaySettingsRepository.kt` | `OverlaySettings`, `overlay`, `setOverlay` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/data/DataStorePoseOverlaySettingsRepository.kt` | Năm khóa mới |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | Nối callback; `BoxWithConstraints`; chỗ đặt cột công cụ; `zoomVisible`; thẻ test mới |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraControls.kt` | `zoomVisible` của `PreviewContent`, `ReadyPreview`; `toggleable = false` cho bốn nút không phải công tắc |
| Sửa | `feature/camera/src/main/res/values/strings.xml` | Chữ mới ở mục 3.1 |
| Tạo | `feature/camera/src/main/res/drawable/ic_camera_tune.xml`, `ic_camera_mirror.xml`, `ic_camera_lock.xml`, `ic_camera_lock_open.xml`, `ic_camera_visibility.xml`, `ic_camera_visibility_off.xml`, `ic_camera_reset.xml` | Bảy biểu tượng Material Symbols |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseFakes.kt` | `FakePoseOverlaySettingsRepository` thêm `storedOverlay`, `overlaySaves`, `failWrites` cho `setOverlay` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseViewModelTest.kt` | Test mới (mục 4) |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/OverlayGeometryTest.kt` | Unit test hàm thuần |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/data/DataStorePoseOverlaySettingsRepositoryTest.kt` | Test năm khóa mới |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenOverlayTest.kt` | Test giao diện phần chỉnh (file riêng, không sửa `CameraScreenTest.kt`, `CameraScreenPoseTest.kt`) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenScreenshotTest.kt` | `samplePoses` nhận `overlay`; ba test mới |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_adjust_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_locked_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_adjust_small.png` | Khung 360 x 640, bảng chỉnh mở (L-028) |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_default_dark.png` | Thêm cột công cụ |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_steps-hidden_dark.png` | Như trên |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_countdown_dark.png` | Như trên |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_ready_dark.png` | Như trên |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_default_small.png` | Cột công cụ ở lề phải ngoài khung |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_countdown_small.png` | Như trên |
| Ghi lại | `docs/features/camera/design/review/` | Sinh bởi `scripts/design-compare.sh camera`, không sửa tay |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/AdjustPanel.kt` | `AdjustPanel`, `AdjustSlider`, `AdjustAction` |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/CameraIconButton.kt` | Tham số `toggleable` |
| Tạo | `core/designsystem/src/main/res/drawable/ic_close.xml` | Biểu tượng `close` |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/catalog/DesignSystemCatalog.kt` | `AdjustPanel` cuối `CameraComponentCatalog` |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/AdjustPanelTest.kt` | Test giao diện ba thành phần mới và `toggleable` |
| Ghi lại | `core/designsystem/src/test/screenshots/DesignSystem_camera_components_dark.png` | Thêm `AdjustPanel` |

Không file nào khác. Đặc biệt không sửa: `CameraScreenTest.kt`, `CameraScreenPoseTest.kt`, `CameraFakes.kt`, `CameraViewModel.kt`, `CameraUiState.kt`, `PoseOutline.kt`; mọi file của màn xem lại (`review/`); `:app`; `gradle/libs.versions.toml`, `build-logic/`; `docs/design-system/tokens.json` (mọi giá trị đã có token: `Radius.sheet` 24, `Stroke.sticker`, các màu ở mục 2).

**Nơi khác đọc các file trên** (L-020), không cần sửa:

- `PoseUiState.kt`: `CameraScreenPoseTest.kt`, `CameraScreenScreenshotTest.kt`, `PoseViewModelTest.kt` dựng `PoseUiState.Ready` không có `overlay`; trường mới có giá trị mặc định.
- `PoseOverlaySettingsRepository.kt`: chỉ `PoseFakes.kt` (trong danh sách) cài đặt interface; `PoseModule.kt` gắn bản DataStore, không đổi.
- `PosePicker.kt` (`PoseActions`): `CameraScreenPoseTest.kt` dựng `PoseActions(...)` bằng tham số có tên; callback mới có mặc định.
- `CameraControls.kt`, `CameraScreen.kt`: `CameraScreenTest.kt` gọi `CameraScreen` như cũ; nút hẹn giờ, lưới vẫn là công tắc nên `assertIsSelected`, `assertIsNotSelected` của nó vẫn đúng; không test nào khẳng định thư viện, cài đặt, flash, đổi camera có `selected`. `AppLaunchTest.kt` chỉ chờ `camera_screen`.
- `CameraIconButton.kt`: `feature/poses`, `feature/settings` và `CameraComponentsTest.kt` gọi không có `toggleable`; mặc định giữ hành vi cũ.
- `DesignSystemCatalog.kt`: chỉ `DesignSystemCatalogScreenshotTest.kt` gọi; chỉ trang camera đổi.
- Sáu ảnh chuẩn ghi lại: chỉ `CameraScreenScreenshotTest.kt` và `scripts/design-compare.sh` đọc.
- DataStore chung: năm khóa mới, không trùng khóa nào (`camera_flash`, `camera_timer`, `camera_grid`, `camera_lens`, `camera_steps_hidden`, `poses_saved`, `poses_recent`, khóa của `:core:data`), không chuyển dữ liệu cũ.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không thêm. Phần chỉnh thuộc màn camera, sau cờ `feature_camera_enabled` của T-013.
- Quảng cáo: Không chạm. Camera không có vị trí quảng cáo (spec mục 5).
- Sự kiện phân tích (điểm chặn 4, đã có trong spec mục 6): `overlay_adjusted` với `type` là `opacity`, `size`, `move`, `flip`, `lock`, `hide`, `reset`. Không có tham số khác.
- Dữ liệu cục bộ: năm khóa DataStore mới `camera_overlay_opacity`, `camera_overlay_scale`, `camera_overlay_mirrored`, `camera_overlay_locked`, `camera_overlay_hidden` (spec mục 6, dòng "Độ mờ, cỡ, lật, khóa, ẩn của nét viền"). Khóa mới, không chuyển dữ liệu cũ, nên không chạm điểm chặn 9.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. Cử chỉ, thanh trượt và DataStore không cần quyền.
- **Thư viện và SDK:** Không thêm. `Slider` có trong `androidx-compose-material3` (BOM `composeBom` của `gradle/libs.versions.toml`), đã có trên classpath của `:core:designsystem` và `:feature:camera`; cử chỉ dùng `androidx.compose.foundation` đã có.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `Slider` với `thumb`, `track` tự vẽ đo không đúng số liệu (rãnh 8, núm 24, phần đã chạy có viền) trong Robolectric `NATIVE`, hoặc vùng chạm của nó không còn 48.
- **Điều kiện chuyển:** test `AdjustPanelTest.adjustSlider_trackAndThumbSizes` hoặc `adjustSlider_touchHeight48` vẫn thất bại vì cùng một lỗi sau 2 lần sửa.
- **Cách làm thay:** trong cùng `AdjustPanel.kt`, giữ `Slider` của Material 3 nhưng `track = {}` và `thumb = {}` rỗng, vẽ rãnh và núm bằng `Modifier.drawBehind` của `Slider` theo `value` và `valueRange`. Ngữ nghĩa (`SetProgress`, `onValueChangeFinished`) vẫn của `Slider`, nên test giữ nguyên.

## 4. Kiểm thử

Mọi test dưới đây vào commit test của bước 3, cùng khung rỗng để biên dịch được: `OverlayState`, `OverlaySettings` đủ trường; `OverlayGeometry` có hằng số, các hàm trả giá trị nhận vào không chặn, `toolColumnPlacement` luôn trả "trong khung, đỉnh 150"; `PoseViewModel` có đủ hàm mới nhưng không làm gì; repository trả `flowOf(OverlaySettings())` và không ghi; `PoseActions` có đủ callback; `OverlayToolColumn`, `OverlayAdjustPanel` là `Box` rỗng; `Modifier.overlayGestures` trả `this`; `AdjustPanel`, `AdjustSlider`, `AdjustAction` vẽ `Box(modifier)` rỗng; `CameraIconButton` có `toggleable` nhưng chưa dùng; `strings.xml` đủ chữ. Phần sửa của `PoseFakes.kt` và `CameraScreenScreenshotTest.kt` cùng vào commit test (L-025). Lý do thất bại ở cột cuối là điều AI dự kiến; bước 3 ghi lý do thật vào nhật ký.

`PoseViewModelTest` dùng lại cách dựng của T-014 (Robolectric, `@GraphicsMode(NATIVE)`, `MainDispatcherRule`), thư viện có tư thế `w01` được chọn sẵn.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-3, kéo | Unit test | `PoseViewModelTest.drag_movesOutline_andLogsMoveOnceAtEnd`: `onOverlayTransform(0.1f, -0.05f, 1f)` ba lần; `offsetX == 0.3f`, `offsetY == -0.15f` (sai số 1e-4); chưa có sự kiện; `onOverlayGestureEnd()`: đúng một `AnalyticsEvent("overlay_adjusted", mapOf("type" to "move"))`; `overlaySaves` rỗng (vị trí không lưu). Thất bại: khung rỗng không đổi offset |
| KB-3, chụm | Unit test | `pinch_scalesWithinBounds_andLogsSizeOnce`: `onOverlayTransform(0f, 0f, 1.5f)` hai lần: `scale == 1.6f` (chặn); `onOverlayTransform(0f, 0f, 0.1f)`: `scale == 0.4f`; kéo kèm chụm trong cùng cử chỉ: kết thúc ghi một sự kiện `size` (không thêm `move`); `overlaySaves.last().scale == 0.4f` |
| KB-3, đặc biệt: còn trong khung | Unit test | `drag_isClampedSoTheCentreStaysInFrame`: `onOverlayTransform(2f, -2f, 1f)`: `offsetX == 0.5f`, `offsetY == -0.5f`. `OverlayGeometryTest.clampOffset_keepsCentreInFrame` với -0.7, -0.5, 0, 0.5, 0.7 |
| KB-3, bảng chỉnh | Unit test | `togglePanel_opensAndCloses`: `onTogglePanel()`: `panelOpen`; lần nữa: đóng; mở rồi `onClosePanel()`: đóng; không sự kiện nào |
| KB-3, thanh trượt | Unit test | `opacitySlider_logsOnceWhenReleased_andSaves`: `onOpacityChange(0.5f)`, `onOpacityChange(0.3f)`: `opacity == 0.3f`, không sự kiện, không lưu; `onOpacityChangeFinished()`: một sự kiện `opacity`, `overlaySaves.last().opacity == 0.3f`. `onOpacityChange(0.05f)`: `0.2f`. Cùng ý cho cỡ: `sizeSlider_logsSize_andSaves` (giá trị 1.2, chặn 0.4 và 1.6) |
| KB-3, lật | Unit test | `mirror_toggles_logsFlip_andSaves`: hai lần `onToggleMirror()`: `mirrored` true rồi false, hai sự kiện `flip`, `overlaySaves.map { it.mirrored } == [true, false]` |
| KB-3, khóa | Unit test | `lock_ignoresGestures_closesPanel_andShowsHintForTwoSeconds`: mở bảng; `onToggleLock()`: `locked`, `panelOpen == false`, `lockHintVisible`; sự kiện `lock`; `advanceTimeBy(2_001)`: lời nhắc tắt; `onOverlayTransform(0.1f, 0f, 1.3f)`: offset, cỡ không đổi, lời nhắc hiện lại; `onOverlayGestureEnd()` không ghi sự kiện; `onTogglePanel()`, `onToggleMirror()`, `onResetOverlay()` không làm gì. `unlock_allowsGestures_andHidesHint`: `onToggleLock()` lần hai: `locked == false`, lời nhắc tắt ngay, sự kiện `lock` thứ hai |
| KB-3, ẩn | Unit test | `hide_toggles_logsHide_andIgnoresGestures`: `onToggleHidden()`: `hidden`, sự kiện `hide`; `onOverlayTransform(0.1f, 0f, 1f)`: không dời; lần nữa: hiện |
| KB-3, đặt lại | Unit test | `reset_centresOutline_withDefaultSizeOpacityAndNoMirror_keepsLockAndHide`: sau kéo, chụm, độ mờ 0.4, lật, ẩn; `onResetOverlay()`: `offsetX == 0f`, `offsetY == 0f`, `scale == 1f`, `opacity == DEFAULT_OPACITY`, `mirrored == false`, `hidden == true`; sự kiện `reset`; được lưu |
| KB-3, giữ khi đổi tư thế | Unit test | `adjustment_isKeptWhenPoseChanges`: kéo, chụm, độ mờ, lật, khóa; `onPoseSelect("m01")` (khóa không chặn chọn tư thế): `overlay` giữ nguyên; `onNextPose()`: vẫn giữ |
| Spec mục 6, giữ khi mở lại app | Unit test | `storedOverlay_isUsedOnOpen`: repository có `OverlaySettings(0.5f, 1.3f, mirrored = true, locked = true, hidden = false)`; trạng thái đầu đúng như vậy, `offsetX == 0f`. `saveOverlayFails_keepsChoice_andLogsWarning`: `failWrites`; `onToggleMirror()`: `mirrored`; có log `WARN` với `error is IOException` |
| Spec mục 6, repository | Unit test | `DataStorePoseOverlaySettingsRepositoryTest.nothingStored_isDefaultOverlay`; `setOverlay_isReadBack` (đối chứng: ghi giá trị khác mặc định ở cả năm trường rồi đọc lại, L-035); `outOfRangeStored_isClamped` (ghi thẳng `camera_overlay_scale` 5.0 và `camera_overlay_opacity` 0.0 vào DataStore: đọc ra 1.6 và 0.2); `setOverlay_keepsStepsHidden` (ghi `stepsHidden = true` rồi `setOverlay`: vẫn `true`). Thất bại: khung rỗng không ghi |
| Không có tư thế thì không chỉnh | Unit test | `noPose_ignoresOverlayActions`: catalog rỗng; `onOverlayTransform`, `onToggleMirror` không đổi gì, không sự kiện |
| Chỗ đặt cột công cụ | Unit test | `OverlayGeometryTest.toolColumnPlacement_byFrame`: khung chuẩn (hộp 411 x 548, khung 411 x 548, đáy thẻ 84): trong khung, đỉnh 150; 360 x 640 (hộp 360 x 296, khung 222 x 296, đáy thẻ 140): lề ngoài, đỉnh 80; 411 x 731 (hộp 411 x 387, khung 290 x 387, đáy thẻ 140): trong khung, đỉnh 150; 360 x 690 (hộp 360 x 346, khung 259 x 346, lề 50, đáy thẻ 140): lề ngoài, đỉnh 130; 360 x 720 (hộp 360 x 376, khung 282 x 376, lề 39, đáy thẻ 140): trong khung, đỉnh 148, tràn 4 dp. `percent_roundsToWholePercent` (0.704 thành 70, 1.0 thành 100) |
| KB-3, kéo trên màn hình | Compose UI test | `CameraScreenOverlayTest.dragOnPreview_reportsPartsOfFrame_andEndsOnce`: ghi lại callback; `onNodeWithTag(OVERLAY_GESTURES).performTouchInput { swipe(center, center + Offset(width * 0.2f, 0f)) }`; tổng `panX` gần 0.2 (sai số 0.02), `panY` gần 0; `onOverlayGestureEnd` đúng một lần |
| KB-3, chụm trên màn hình | Compose UI test | `pinchOnPreview_reportsZoom`: `pinch(...)` từ hai điểm cách 100 px ra 300 px; tích `zoom` lớn hơn 2.5; một lần kết thúc |
| Chạm vào nút không thành kéo | Compose UI test | `swipeOnStepCard_doesNotMoveOutline`: vuốt ngang trên `STEP_CARD`: không gọi `onOverlayTransform`. `tapOnTool_doesNotMoveOutline` |
| KB-3, biến đổi nét viền | Compose UI test | `outline_followsOffsetScaleMirrorOpacity`: `overlay` có `offsetX = 0.25f`, `scale = 0.5f`: tâm `OUTLINE` lệch phải đúng một phần tư chiều rộng `PREVIEW` (sai số 2 dp), cỡ nhìn thấy bằng nửa (đo `boundsInRoot` sau `graphicsLayer`). `hidden = true`: không có `OUTLINE` |
| KB-3, cột công cụ | Compose UI test | `toolColumn_showsFourTools_withStates`: `panelOpen = true`, `mirrored = true`: `TOOL_ADJUST` và `TOOL_MIRROR` `assertIsSelected`, `TOOL_LOCK` `assertIsNotSelected`, `TOOL_VISIBILITY` `assertIsSelected`; bấm từng nút gọi đúng callback. `toolColumn_absentWithoutPoseOrCamera`: `selected = null` hoặc `CameraUiState.Starting`: không có `OVERLAY_TOOLS` |
| KB-3, khóa trên màn hình | Compose UI test | `locked_dimsAdjustAndMirror_andShowsHint`: `locked = true`, `lockHintVisible = true`: `TOOL_ADJUST`, `TOOL_MIRROR` `assertIsNotEnabled`; `TOOL_LOCK` `assertIsSelected` với mô tả "Mở khóa lớp phủ"; có chữ "Đã khóa lớp phủ". `lockHintVisible = false`: không có `LOCK_HINT` |
| KB-3, ẩn trên màn hình | Compose UI test | `hidden_visibilityToolOff`: `hidden = true`: `TOOL_VISIBILITY` `assertIsNotSelected`, mô tả "Hiện tư thế" |
| KB-3, bảng chỉnh trên màn hình | Compose UI test | `panel_showsSlidersAndActions_andHidesZoom`: `panelOpen = true`, `opacity = 0.7f`: có "Chỉnh lớp phủ", "Độ mờ", "70%", "Cỡ", "100%", "Lật", "Khóa", "Ẩn", "Đặt lại"; không có `ZOOM`. Bấm "Đặt lại", "Lật", "Khóa", "Ẩn", nút đóng gọi đúng callback. `panelClosed_showsZoom` |
| KB-3, thanh trượt trên màn hình | Compose UI test | `opacitySlider_setProgress_callsChangeThenFinished`: `performSemanticsAction(SetProgress) { it(0.4f) }` trên `OPACITY_SLIDER`: `onOpacityChange(0.4f)` rồi `onOpacityChangeFinished` một lần. Cùng ý cho `SIZE_SLIDER` với 1.2 |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | Các test trên so đúng chuỗi của README mục 2 ("Chỉnh lớp phủ", "Độ mờ", "Cỡ", "70%", "100%", "Lật", "Khóa", "Ẩn", "Đặt lại", "Đã khóa lớp phủ") |
| Màn hình thấp (L-028) | Compose UI test, `@Config(qualifiers = "w360dp-h640dp-xxhdpi")` | `small_toolColumnInRightMargin_clearOfFrameAndStrip`: mép trái `OVERLAY_TOOLS` không nhỏ hơn mép phải `PREVIEW`; mép phải trong màn hình; mép dưới không quá mép trên `CHIPS`; bốn nút đều 48. `small_panelInsideFrame_andScrolls`: `panelOpen = true`: `ADJUST_PANEL` nằm trọn trong `PREVIEW`; "Đặt lại" tới được bằng `performScrollTo()` và bấm gọi callback; `SHUTTER` vẫn trọn trong màn hình |
| Thành phần `AdjustPanel` | Compose UI test, `@GraphicsMode(NATIVE)` | `AdjustPanelTest.adjustPanel_whiteSheet_radius24_closeButton48`: điểm (30 dp, 30 dp) là #FFFFFF trên nền tối; nút đóng 48 x 48, bấm gọi `onClose`. `adjustSlider_trackAndThumbSizes`: rãnh cao 8, núm 24 (đo qua phần tử bọc có `testTag`); điểm giữa phần đã chạy là #FFC83D, giữa phần chưa chạy là #B5B0A8. `adjustSlider_touchHeight48`. `adjustSlider_setProgress_finishedOnce`. `adjustSlider_readsLabelAndValue` (nhãn "Độ mờ", `stateDescription` "70%"). `adjustAction_48button_label_selectedWhenActive`; `adjustAction_disabled_notClickable` |
| `CameraIconButton` công tắc | Compose UI test | `AdjustPanelTest.cameraIconButton_notToggleable_hasNoSelected`: `toggleable = false`: node không có `SemanticsProperties.Selected`; mặc định vẫn có (đối chứng cạnh nhau, L-035) |
| Giao diện catalog | Screenshot test, ghi lại | `DesignSystemCatalogScreenshotTest.cameraComponents_dark` (`src/test/screenshots/DesignSystem_camera_components_dark.png`), thêm `AdjustPanel` |
| Đúng thiết kế, hai ảnh mới | Screenshot test, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "w411dp-h891dp-xxhdpi")` | `CameraScreenScreenshotTest.adjust_dark` (`src/test/screenshots/CameraScreen_adjust_dark.png`): như `default_dark`, bảng mở, độ mờ 70%, cỡ 100%, nút hiện tư thế bật. `locked_dark` (`src/test/screenshots/CameraScreen_locked_dark.png`): như `default_dark`, `locked = true`, `lockHintVisible = true` |
| Bốn ảnh có cột công cụ | Screenshot test, ghi lại | `default_dark`, `stepsHidden_dark`, `countdown_dark`, `ready_dark` giữ tên file cũ; `samplePoses` mặc định `OverlayState()` (độ mờ 100%, nút hiện tư thế bật) |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` | `adjust_small` (`src/test/screenshots/CameraScreen_adjust_small.png`, nhiều nội dung nhất: thẻ bước hai hàng, bảng chỉnh cuộn, cột công cụ ở lề). Ghi lại `default_small`, `countdown_small` |
| So với thiết kế | Ảnh so sánh và rà soát độc lập (bước 6) | Chạy `scripts/design-compare.sh camera` sau khi ghi ảnh chuẩn; commit `docs/features/camera/design/review/`, dán `review/summary.md` vào mô tả PR (ADR-0020). Rà soát xem ảnh ghép của `adjust`, `locked`, `default`, `steps-hidden`, `countdown` theo mục 7 của README thiết kế. Khác biệt đã biết: ảnh `locked` của thiết kế để nút chỉnh, nút lật trắng như thường (spec yêu cầu mờ, mục 8, câu 9); ảnh tạm của ô và nét viền |
| Ảnh thiết kế của ticket khác | | Ảnh thiết kế của ticket khác (T-013, T-014, T-016, T-017, T-020): `CameraScreen_starting_dark`, `CameraScreen_permission-denied_dark`, `CameraScreen_error_dark`, `CameraScreen_no-pose_dark`, `ReviewScreen_default_dark`, `ReviewScreen_saving_dark`, `ReviewScreen_save-error_dark`, `ReviewScreen_discard_dark`, `CameraScreen_matching_dark`, `CameraScreen_matched_dark`, `CameraScreen_no-person_dark`, `CameraScreen_default_dark_unfolded`, `ReviewScreen_default_dark_unfolded` |
| Kéo, chụm bằng tay | Kiểm tra thủ công | Bản staging trên máy thật: kéo nét viền một ngón, chụm hai ngón: mượt, không giật; kéo sát mép: luôn còn phần nét viền để kéo lại; chạm thẻ bước, nút công cụ, zoom không làm nét viền nhảy |
| Giữ khi mở lại app | Kiểm tra thủ công | Đổi độ mờ 40%, cỡ 130%, lật, khóa; đóng app hẳn, mở lại: bốn lựa chọn giữ, nét viền ở giữa khung |
| TalkBack | Kiểm tra thủ công | Bật TalkBack: bốn nút công cụ đọc đúng tên và trạng thái; thanh trượt đọc "Độ mờ, 70%" và chỉnh được bằng cử chỉ tăng giảm; nút thư viện, cài đặt, flash, đổi camera không còn đọc "chưa chọn" |

**Ngoại lệ viết test trước:**

- Bảy biểu tượng của feature và một biểu tượng của core: tài nguyên, không có logic.
- Phần `CameraRoute` nối callback mới vào `PoseViewModel`: không có test JVM riêng, như T-014; kiểm bằng `AppLaunchTest` có sẵn và kiểm tra thủ công.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-23, trên nhánh chính (commit fa65d75) cộng một test Robolectric tạm không commit (`@GraphicsMode(NATIVE)`, `createComposeRule`) trong `:feature:camera`: `Slider` của Material 3 với `thumb` và `track` tự vẽ, `valueRange = 0.2f..1f`; một `Box` có `pointerInput` dùng `awaitEachGesture`, `calculateZoom`, `calculatePan`, gọi kết thúc khi nhấc hết ngón. Cùng lần chạy với thay đổi tạm của T-017.
- **Lệnh:** `./gradlew :feature:camera:testDebugUnitTest --tests '*SpikeTest*'` để đọc số in ra; `scripts/check.sh`.
- **Kết quả:**
  - `Slider` tự vẽ: `performSemanticsAction(SetProgress) { it(0.5f) }` đổi giá trị thành 0.5 và gọi `onValueChangeFinished` đúng một lần; `performTouchInput { swipeRight() }` kéo tới 1.0 và gọi đúng một lần. Bản `composeBom` hiện có có `thumb`, `track` (mục 7, điều 2 không xảy ra).
  - Cử chỉ: `pinch` từ hai điểm cách 100 px ra 300 px cho tích `calculateZoom` 3.0 và một lần kết thúc; `swipe` 120 x 60 px cho tổng `calculatePan` đúng (120, 60), một lần kết thúc mỗi cử chỉ.
  - Đo trên ảnh chuẩn hiện có `CameraScreen_default_small.png` (khung 360 x 640): khung 222 x 296, thẻ bước hai hàng ở 16 đến 140 trong khung, zoom ở 232 đến 280; lề mỗi bên khung 69 dp (cơ sở của chỗ đặt cột công cụ, mục 3.1).
  - `scripts/check.sh` (cùng lần với thay đổi tạm của T-015, T-017, có `libs.mlkit.pose.detection` trong `:feature:camera`): "Cổng chất lượng: QUA", 752 test JVM, 0 lỗi, 65 ảnh screenshot, `lintStagingDebug` qua. Hai lần chạy trước thất bại vì lý do không thuộc kỹ thuật: máy chạy thiếu `gh` trong `PATH` (test của `release.sh`), và code tạm chưa đúng ktlint (sửa bằng `./gradlew :feature:camera:ktlintFormat`).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 4 (ghi `overlay_adjusted`, có trong spec mục 6), điểm 7 (thành phần mới `AdjustPanel`, biểu tượng mới, sửa `CameraIconButton` và trang catalog trong `:core:designsystem`). Không chạm điểm 2 (không thêm thư viện), điểm 9 (khóa DataStore mới, không chuyển dữ liệu cũ).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:feature:camera`, `:core:designsystem`), khoảng 40 file kể cả 10 ảnh chuẩn |
| B. Độ rõ của yêu cầu | 2 | Spec xác nhận, thiết kế duyệt, nhưng còn câu hỏi ảnh hưởng cách làm: độ mờ mặc định, lời nhắc khóa hiện bao lâu, khóa từ bảng chỉnh, chỗ đặt cột công cụ ở khung thấp, vị trí kéo có lưu không (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Logic, cử chỉ, thanh trượt, khung thấp có test JVM; độ mượt khi kéo bằng tay và TalkBack kiểm tra thủ công |
| D. Ảnh hưởng chéo | 2 | Sửa `PoseViewModel.kt`, `PoseOverlay.kt`, `PosePicker.kt`, `CameraScreen.kt`, `CameraControls.kt`, `strings.xml`, ảnh chuẩn camera mà T-017, T-020 cũng sửa; sửa `CameraIconButton.kt` mà `:feature:poses`, `:feature:settings` dùng (mặc định giữ hành vi cũ) |
| E. Độ mới của kỹ thuật | 2 | Cử chỉ chụm tự viết bằng `awaitEachGesture`, `Slider` vẽ lại rãnh và núm, đặt thành phần theo cỡ khung đo được: chưa có trong dự án nhưng phổ biến |
| **Tổng** | 10 | **Mức: Cao** (chạm điểm chặn, và tổng điểm cũng từ 9 trở lên) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `:core:designsystem` (`AdjustPanel.kt` mới, `CameraIconButton.kt`, `DesignSystemCatalog.kt`, `ic_close.xml`, một ảnh catalog); DataStore chung (năm khóa mới); trong `:feature:camera`: `PoseUiState.kt`, `PoseViewModel.kt`, `PoseOverlay.kt`, `PosePicker.kt`, `PoseOverlaySettingsRepository.kt`, `CameraScreen.kt`, `CameraControls.kt`, `strings.xml`, `PoseFakes.kt`, `PoseViewModelTest.kt`, `CameraScreenScreenshotTest.kt`, sáu ảnh chuẩn camera và `design/review/`.
- Task khác có thể xung đột:
  - **T-017** (điểm khớp, plan `features/camera/plans/T-017-camera-diem-khop.md` viết cùng lúc): dựa vào trạng thái `hidden` của plan này (không chạy nhận dạng khi nét viền ẩn) và vị trí lời nhắc khóa (cùng chỗ với `MatchHint`); cùng sửa `PoseViewModel.kt`, `PoseOverlay.kt`, `CameraScreen.kt`, `CameraControls.kt`, `strings.xml`, ảnh chuẩn camera, `DesignSystemCatalog.kt`. Ticket T-017 phụ thuộc T-015.
  - **T-020** (máy gập, plan `features/camera/plans/T-020-camera-may-gap.md` viết cùng lúc): dời bố cục `CameraScreen.kt`, `CameraControls.kt`; cột công cụ của plan này có trong ảnh `_unfolded`, nên T-020 chạy sau; vị trí kéo tính theo phần của khung nên giữ đúng khi gập mở. Ticket T-020 phụ thuộc T-015.
  - **T-018, T-019** (màn xem lại): không chung file.
  - **T-022, T-023, T-024** (thư viện, chi tiết tư thế): không chung file; `:feature:poses` dùng `CameraIconButton` (giữ hành vi mặc định).
- Đề xuất thứ tự chạy: T-015 trước T-017 và T-020 (thứ tự trong nhóm camera: T-015, T-017, T-020). Không phụ thuộc T-018, T-019, T-022 đến T-024.

## 7. Điều kiện dừng riêng

1. Lúc bắt đầu task, thiếu phần T-014 mà plan dựa vào: `PoseUiState.Ready`, `PoseActions`, `PoseOverlay`, `PreviewHint`, `PoseOverlaySettingsRepository.stepsHidden`, `ReadyPreview` với slot `poseOverlay`. Dừng.
2. `Slider` của Material 3 không có `thumb`, `track` trong bản `composeBom` hiện có, và phương án dự phòng (mục 3.4) cũng không vẽ được: dừng.
3. Cử chỉ của lớp kéo làm `LazyRow` của dải tư thế, thẻ bước hay zoom nhận nhầm chạm trong test giao diện của T-013, T-014, và sửa cần đổi `CameraScreenTest.kt` hay `CameraScreenPoseTest.kt`: dừng.
4. Đặt cột công cụ ở lề ngoài khung cần chuyển phần dựng khung sang file không có ở mục 3.2: dừng.
5. Tải Material Symbols không được: lỗi hạ tầng (auto-run.md, bước 5).

## 8. Câu hỏi cho người duyệt

1. **Độ mờ mặc định.** Spec ghi "độ mờ mặc định" nhưng không nêu số; ảnh `adjust` có 70% như một giá trị ví dụ. Đề xuất của AI: 100%, vì nét viền rõ là lý do chính của tính năng (bài học 5), và các ảnh chuẩn của T-014 đang vẽ nét viền đầy. Nếu chọn 70% thì nét viền mờ đi ngay lần đầu dùng, và sáu ảnh ghi lại đổi thêm phần nét viền.
2. **Lời nhắc "Đã khóa lớp phủ" hiện bao lâu.** Spec gọi là "lời nhắc ngắn", ảnh `locked` có lời nhắc. Lời nhắc này cùng chỗ với `MatchHint` của T-017 (trên zoom), nên hiện mãi khi khóa sẽ che điểm khớp. Đề xuất của AI: hiện 2 giây sau khi bấm Khóa, và hiện lại 2 giây mỗi lần kéo hay chụm trong lúc khóa (để biết vì sao nét viền không đổi). Cách khác: hiện suốt lúc khóa, T-017 đặt `MatchHint` lên trên lời nhắc.
3. **Bấm Khóa trong bảng chỉnh.** Khóa làm nút chỉnh mờ đi, nên bảng không mở lại được khi đang khóa. Đề xuất của AI: bấm Khóa trong bảng thì bảng đóng; mở khóa bằng nút khóa ở cột công cụ. Trong bảng, nút Lật và Đặt lại không có tác dụng khi khóa (không xảy ra vì bảng đóng).
4. **Kéo khi nét viền đang ẩn.** Spec không nêu. Đề xuất của AI: kéo, chụm không làm gì khi ẩn (không thấy gì để kéo); bấm hiện lại thì nét viền ở chỗ cũ.
5. **Trạng thái đang bật của nút lật ở cột công cụ.** Spec chỉ nêu trạng thái đang bật cho nút chỉnh, khóa, hiện tư thế. Đề xuất của AI: nút lật cũng vàng khi đang lật, để người dùng biết nét viền đang bị lật.
6. **Vị trí kéo có giữ khi mở lại app không.** Bảng dữ liệu của spec (mục 6) chỉ có độ mờ, cỡ, lật, khóa, ẩn. Đề xuất của AI: vị trí giữ khi đổi tư thế, khi ra nền, khi về từ màn xem lại (cùng ViewModel), nhưng mở lại app thì về giữa. Muốn giữ cả vị trí thì thêm dòng vào spec mục 6 và hai khóa DataStore.
7. **Cột công cụ và bảng chỉnh ở màn hình thấp.** Ở khung 360 x 640, khung còn 222 x 296 và không đủ chỗ cho cột cao 216 giữa thẻ bước và zoom (đo trên ảnh chuẩn hiện có). Đề xuất của AI: khi lề hai bên khung từ 64 dp, cột ra lề phải ngoài khung; bảng chỉnh cao tối đa khung trừ 32 dp, cuộn được, đè lên thẻ bước khi mở. Cách khác: ở khung thấp chỉ giữ nút chỉnh ở cột (lật, khóa, ẩn đã có trong bảng), nhưng khi khóa thì nút chỉnh mờ và không mở được bảng để mở khóa, nên phải thêm nút khóa: hai nút.
8. **Mô tả cho trình đọc màn hình.** Bảng chữ không có. Đề xuất của AI: "Chỉnh lớp phủ", "Lật nét viền", "Khóa lớp phủ" (khi mở) và "Mở khóa lớp phủ" (khi khóa), "Ẩn tư thế" (khi hiện) và "Hiện tư thế" (khi ẩn), "Đóng bảng chỉnh"; thanh trượt đọc nhãn và phần trăm.
9. **Ảnh `locked` của thiết kế khác spec.** Spec: nút chỉnh và nút lật mờ đi khi khóa; ảnh thiết kế để hai nút trắng như thường. Đề xuất của AI: làm theo spec (mờ, không bấm được); ảnh so sánh lệch hai nút; cập nhật thiết kế sau.
10. **Khóa và mở khóa, ẩn và hiện ghi cùng một `type`.** Đề xuất của AI: `lock` cho cả khóa và mở khóa, `hide` cho cả ẩn và hiện, đúng danh sách giá trị của spec. Muốn phân biệt thì thêm tham số mới vào spec mục 6 (điểm chặn 4).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-23, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### 2026-10-23, RUN-20261023-04: đã merge

- **Kết quả:** đã merge, PR #73, commit `1240201`. CI xanh ngay vòng đầu.
- **Test viết trước:** 55 test JVM mới (+3 screenshot), cả 55 thất bại đúng lý do trên khung rỗng. Viết chặt lại có đối chứng: 2 (`toolColumn_absentWithoutPose`, `toolColumn_absentWhenCameraNotReady`) và 5 test có đối chứng từ đầu; bỏ 2 test qua ngay vô nghĩa (hằng số, ca khung chuẩn gộp vào `toolColumnPlacement_byFrame`).
- **Lệch so với plan:** (1) `AdjustAction`: nút trong là nút bấm riêng nên nút có `contentDescription = label`, chữ nhãn `clearAndSetSemantics`; test tìm nút theo mô tả thay vì chữ. (2) `AdjustSlider`: node ngoài tự đặt semantics của thanh trượt; chữ nhãn và giá trị chỉ tìm được ở cây chưa gộp. (3) `adjustSlider_touchHeight48` đổi từ đo chiều cao node sang chạm thật. (4) Hàng bốn nút dùng `FlowRow`; ở khung 360 x 640 bảng chỉ rộng 190 dp nên nút xuống hai hàng và bảng cuộn. (5) Không ghi log "Overlay tools overflow the frame" vì `CameraScreen` không có logger. (6) Nút "sát mép khung" đặt giữa lề.
- **Rà soát độc lập:** 1 chặn: vùng chạm `AdjustSlider` dưới 48 dp (`Slider` `requiredHeight(48.dp)` trong `Box(28.dp)`). Không tái hiện: test chạm thật cách tâm rãnh 22 dp ở cả hai phía (16 dp và 60 dp từ đỉnh, ngoài biên 52 dp của thanh trượt) đều đổi giá trị trên code cũ, nên không đổi code, thay phép đo cũ bằng test chạm thật. 3 nên sửa: bảng không chặn cử chỉ (có thật, đã sửa bằng `pointerInput`, test `panel_blocksGesturesUnderIt`); chụm rung ghi `size` (có thật, ngưỡng cộng dồn 3%, test `twoFingerJitter_isNotAPinch`); đọc lại cài đặt đè chỉnh dở (không phải lỗi, thêm test hồi quy). Ghi chú: nút chỉnh và lật mờ khi khóa theo spec (thiết kế để trắng); núm "Cỡ" ở 50% thay vì 70%.
- **Cần người kiểm tra:** vùng chạm thanh trượt trên máy thật; kéo, chụm, TalkBack; 3 ảnh chuẩn mới và 7 ảnh ghi lại; ảnh so sánh `docs/features/camera/design/review/` (locked 6,8%, steps-hidden 6,0%).
