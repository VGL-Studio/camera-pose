---
ticket: T-020
title: Camera và màn xem lại trên máy gập
status: done             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 8                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: medium           # low | medium | high
token_estimate: 30M          # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-020: CAMERA VÀ MÀN XEM LẠI TRÊN MÁY GẬP

## 1. Tóm tắt

Khi màn hình rộng từ 600 dp (máy gập mở ra), camera đặt khung 3:4 bên trái, dời nút đổi camera, nút chụp, ô ảnh vừa chụp và zoom sang cột phải, hàng chip và dải tư thế dưới khung; màn xem lại đặt ảnh bên trái, ba nút phụ xếp dọc và nút "Lưu ảnh" ở cột phải. Chọn bố cục theo chiều rộng đo được (`BoxWithConstraints`), mọi trạng thái nằm trong ViewModel có sẵn nên gập mở không mất tư thế và lựa chọn; trạng thái cuộn của hàng chip và dải tư thế được đưa lên trên chỗ chọn bố cục. Kiểm chứng bằng test giao diện Robolectric ở khung `w673dp-h841dp-xxhdpi` (vị trí, chuyển bố cục giữ trạng thái), screenshot test cùng tên hai ảnh `_unfolded`, ảnh so sánh `scripts/design-compare.sh camera`, và kiểm tra thủ công trên emulator gập.

`token_estimate` 30M thay cho số gợi ý 15.4M của `scripts/token-usage.py estimate` (trung vị cỡ M): các task camera gần nhất tốn khoảng 40M (`docs/metrics/token-usage.csv`); task này ít logic hơn (không ViewModel mới, không thành phần core) nhưng phải ghi ảnh và chạy so sánh thiết kế như các task kia.

## 2. Hiểu yêu cầu

Ticket xử lý KB-11 trong `features/camera/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-11 | Khi máy gập mở ra thì camera giữ khung 3:4 bên trái; nút chụp, đổi camera, ô ảnh vừa chụp và zoom dời sang cột phải; màn xem lại đặt ảnh bên trái, các nút bên phải, đúng thiết kế `_unfolded` | "Máy gập mở ra" là cửa sổ rộng từ 600 dp (ngưỡng cỡ cửa sổ trung bình của Android); điện thoại dọc đều hẹp hơn. Máy tính bảng cũng dùng bố cục này (mục 8, câu 1). Bố cục chọn theo chiều rộng của chính màn hình (`BoxWithConstraints` ở gốc `CameraScreen`, `ReviewScreen`), không đọc trạng thái bản lề |
| KB-11 | Khi gập lại thì camera và màn xem lại dùng bố cục điện thoại, giữ tư thế và các lựa chọn | Gập mở làm activity dựng lại (manifest không khai báo `configChanges`): `CameraViewModel`, `PoseViewModel`, `LastPhotoViewModel`, `ReviewViewModel` còn nguyên, camera đóng rồi mở lại như khi ra nền (KB-8). Trạng thái cuộn của hàng chip và dải tư thế dùng `rememberLazyListState` (lưu được) đặt ở trên chỗ chọn bố cục, nên giữ cả khi chuyển bố cục trong cùng lần dựng lẫn khi activity dựng lại. Vị trí kéo nét viền của T-015 tính theo phần của khung nên giữ đúng chỗ |
| Ticket, phạm vi | Screenshot test khung `w673dp-h841dp-xxhdpi` | `CameraScreen_default_dark_unfolded`, `ReviewScreen_default_dark_unfolded`, thêm `ReviewScreen_save-error_unfolded` (nhiều nội dung nhất ở cột phải) |

Thiết kế tham chiếu: `features/camera/design/README.md` (mục 3, đoạn Máy gập mở ra); số liệu trong `design/context/CameraScreen_default_dark_unfolded.md`, `ReviewScreen_default_dark_unfolded.md`. Phân chia ảnh: T-020 vẽ `CameraScreen_default_dark_unfolded`, `ReviewScreen_default_dark_unfolded`; các ảnh khác thuộc T-013 đến T-017 (mục 4).

Số liệu chính (khung 673 x 841):

| Phần | Camera | Xem lại |
| --- | --- | --- |
| Thanh trên | Rộng bằng khung xem trước (16 đến 466), đỉnh ở 16 (điện thoại 32) | Như điện thoại, rộng hết màn hình |
| Khung bên trái | Khung 3:4 cao 600 (450 x 600) ở 16, 72; bo `Radius.card` (16). Cột công cụ nét viền (T-015), thẻ bước, nét viền trong khung như điện thoại | Ảnh 450 x 600 ở 16, 72, như `ReviewPhoto` |
| Cột phải | Từ mép phải khung cộng `Spacing.md` tới mép màn hình, các phần căn giữa ngang: đổi camera (đỉnh 154 trong chiều cao khung), nút chụp (giữa khung theo chiều dọc: tâm 300), ô ảnh vừa chụp (đỉnh 428), zoom (đáy cách đáy khung `Spacing.xs`) | Bắt đầu ở mép phải ảnh cộng `Spacing.md`: ba nút phụ xếp dọc từ đỉnh ảnh, cách nhau `Spacing.lg`, mỗi nút là nút tròn 48 và nhãn bên phải cách `Spacing.sm`; nút chính "Lưu ảnh" rộng theo chữ ở đáy cột, cách đáy màn hình `Spacing.md + Spacing.xs` |
| Dưới khung | Hàng chip (đỉnh 688) và dải tư thế (đỉnh 740), rộng hết màn hình, như điện thoại | Không có |

Ở điện thoại không đổi gì (mọi ảnh chuẩn hiện có giữ nguyên).

ADR liên quan: ADR-0020 (ảnh `_unfolded` có screenshot test cùng tên ở khung `w673dp-h841dp-xxhdpi`, ảnh so sánh), ADR-0008 (không viết cứng màu, cỡ chữ).

## 3. Giải pháp

### 3.1. Cách làm

**Điều kiện đầu vào.** T-014, T-016, T-015 đã merge: `CameraScreen` có `PosePicker`, `PoseOverlay`, cột công cụ nét viền và tham số `hintBottomPadding` của T-015, `CaptureRow`, `ReadyPreview` với `zoomVisible`; `ReviewScreen` có `ReviewTopBar`, `ReviewPhoto`, `ReviewActions`, `SaveErrorMessage`, `PrimaryButton`. T-017 không bắt buộc: nếu đã merge thì `MatchHint` và màu viền nút chụp có sẵn và bố cục mới giữ chúng (dưới). Thiếu phần của T-014, T-015, T-016 thì dừng (mục 7).

**Chọn bố cục (`layout/WindowLayout.kt`, tạo).** `internal val UNFOLDED_MIN_WIDTH = 600.dp`; `internal fun isUnfolded(width: Dp): Boolean = width >= UNFOLDED_MIN_WIDTH`. Giữ trong `:feature:camera` (không tạo file dùng chung trong core; T-024 của thư viện tư thế có cách riêng, mục 8, câu 2).

**Camera (`CameraScreen.kt`, sửa; `CameraUnfolded.kt`, tạo).**

- `CameraScreen` giữ nguyên tham số. Thân hàm bọc trong `BoxWithConstraints`; `isUnfolded(maxWidth)` thì gọi `CameraUnfoldedLayout(...)`, không thì phần thân hiện có (chuyển nguyên vào hàm riêng `CameraPhoneLayout` trong cùng file, không đổi gì bên trong ngoài trạng thái cuộn).
- Trạng thái cuộn: `val chipsState = rememberLazyListState()`, `val stripState = rememberLazyListState()` tạo ở `CameraScreen`, trên chỗ chọn bố cục, truyền cho `PosePicker` ở cả hai bố cục.
- `CameraUnfoldedLayout` (file mới `CameraUnfolded.kt`): `Column` gồm `Row` (cột trái: `CameraTopBar` với `topPadding = Spacing.md`, khoảng `Spacing.sm`, khung xem trước; cột phải: `UnfoldedCaptureColumn`), rồi `PosePicker` rộng hết màn hình. Chiều cao khung là phần còn lại (`weight(1f)`) sau thanh trên, `PosePicker` (132) và `Spacing.md` ở đáy; chiều rộng 3/4 chiều cao nhưng không quá chiều rộng màn hình trừ cột phải tối thiểu (`ZoomControl` 136 cộng 2 x `Spacing.md`). Khung bo `Radius.card` (`clip`). Nội dung khung gọi đúng `PreviewContent` của điện thoại với `zoomVisible = false` và `hintBottomPadding = Spacing.md` (zoom không còn trong khung).
- `UnfoldedCaptureColumn`: `Box` cao bằng khung: `ShutterButton` ở giữa; nút đổi camera căn giữa, lệch lên 120 dp so với tâm; ô ảnh vừa chụp căn giữa, lệch xuống 152 dp; `ZoomControl` căn đáy, lề dưới `Spacing.xs` (số liệu context: đổi camera 226, nút chụp tâm 372, ô ảnh 500, zoom 620 với khung 72 đến 672). Khung thấp hơn 600 (máy tính bảng nằm ngang): ba khoảng lệch co theo tỷ lệ chiều cao khung so với 600.
- Nếu T-017 đã merge: viền nút chụp đổi màu khi đạt điểm theo cùng tham số của `ShutterButton` (không làm gì thêm); `MatchHint` nằm trong `PreviewContent` theo `hintBottomPadding`, nên ở máy gập cách đáy khung `Spacing.md`.

**`CameraControls.kt` (sửa).** `CameraTopBar` thêm `topPadding: Dp = Spacing.xl` (máy gập truyền `Spacing.md`) và `modifier`. Tách phần zoom của `ReadyPreview` thành `internal fun CameraZoom(ready, onZoomSelect, modifier)` (giữ nguyên nhãn, `testTag(ZOOM)`), `ReadyPreview` gọi lại nó khi `zoomVisible`. `ShutterButton`, `LastPhotoSlot` chuyển từ `private` sang `internal`; nút đổi camera tách thành `internal fun FlipCameraButton(ready, onFlipClick)` dùng ở `CaptureRow` và cột phải. Không đổi hình dạng hay hành vi.

**`pose/PosePicker.kt` (sửa).** `PosePicker` nhận thêm `chipsState: LazyListState = rememberLazyListState()`, `stripState: LazyListState = rememberLazyListState()`; `CategoryChips`, `PoseStrip` dùng hai trạng thái này thay cho trạng thái tự tạo. Mặc định giữ cách cũ cho test hiện có.

**Màn xem lại (`review/ReviewScreen.kt`, sửa; `review/ReviewUnfolded.kt`, tạo).**

- `ReviewScreen` bọc phần nội dung (cột hiện có) trong `BoxWithConstraints`; `isUnfolded(maxWidth)` thì `ReviewUnfoldedContent(...)`, không thì cột hiện có (không đổi). `DiscardDialog` và lớp ẩn ngữ nghĩa khi hộp thoại mở (L-004) giữ chung cho cả hai bố cục.
- `ReviewUnfoldedContent`: `ReviewTopBar` như điện thoại; dưới nó `Row`: `ReviewPhoto` (cao hết phần còn lại trừ `Spacing.md + Spacing.xs` ở đáy, lề trái `Spacing.md`), `Spacer(Spacing.md)`, cột phải `Column(weight(1f))`: `ReviewActions(vertical = true)`, `Spacer(weight(1f))`, `SaveErrorMessage` (nếu có, rộng hết cột), `Spacer(Spacing.md)`, `PrimaryButton` "Lưu ảnh" (đang lưu: "Đang lưu…") rộng theo chữ, lề phải `Spacing.md`, đáy cách `Spacing.md + Spacing.xs`.
- `ReviewRoute` không đổi.

**`review/ReviewComponents.kt` (sửa).** `ReviewActions` thêm `vertical: Boolean = false`: `true` thì `Column(verticalArrangement = spacedBy(Spacing.lg))` và mỗi `ReviewAction` là `Row` (nút 48, `Spacing.sm`, nhãn `labelLarge`), cùng ngữ nghĩa (một nút có tên là nhãn), cùng thẻ test. Không đổi bố cục ngang.

**Chữ mới (`strings_review.xml`).** Theo bảng chữ của thiết kế ("Lưu vào thư viện (máy gập: Lưu ảnh)"): `review_save_unfolded` "Lưu ảnh". Không dùng lại `review_discard_save` (cùng chữ, khác chỗ dùng).

**Thứ tự làm.** Bước 3: khung rỗng (mục 4), mọi test, chữ mới; chạy để thấy thất bại đúng lý do; commit. Bước 4: `WindowLayout.kt`; `CameraControls.kt`; `PosePicker.kt`; `CameraUnfolded.kt`, `CameraScreen.kt`; `ReviewComponents.kt`, `ReviewUnfolded.kt`, `ReviewScreen.kt`; `scripts/check.sh` ghi ảnh chuẩn; `scripts/design-compare.sh camera`; build `stagingDebug`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/layout/WindowLayout.kt` | `UNFOLDED_MIN_WIDTH`, `isUnfolded` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | `BoxWithConstraints`, `CameraPhoneLayout`, trạng thái cuộn đưa lên |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraUnfolded.kt` | `CameraUnfoldedLayout`, `UnfoldedCaptureColumn` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraControls.kt` | `topPadding` của `CameraTopBar`; `CameraZoom`, `FlipCameraButton`; `ShutterButton`, `LastPhotoSlot` thành `internal` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PosePicker.kt` | `chipsState`, `stripState` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreen.kt` | Chọn bố cục |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewUnfolded.kt` | `ReviewUnfoldedContent` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewComponents.kt` | `ReviewActions(vertical)` |
| Sửa | `feature/camera/src/main/res/values/strings_review.xml` | `review_save_unfolded` |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenUnfoldedTest.kt` | Test giao diện bố cục gập, chuyển bố cục |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraUnfoldedScreenshotTest.kt` | Screenshot test camera khung gập |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenUnfoldedTest.kt` | Test giao diện và screenshot test màn xem lại khung gập |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/layout/WindowLayoutTest.kt` | Unit test ngưỡng |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_default_dark_unfolded.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/ReviewScreen_default_dark_unfolded.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/ReviewScreen_save-error_unfolded.png` | Ảnh chuẩn mới: lưu lỗi ở cột phải |
| Ghi lại | `docs/features/camera/design/review/` | Sinh bởi `scripts/design-compare.sh camera`, không sửa tay |

Không file nào khác. Đặc biệt không sửa: `CameraViewModel.kt`, `PoseViewModel.kt`, `ReviewViewModel.kt`, `PoseOverlay.kt`, mọi test và ảnh chuẩn hiện có (bố cục điện thoại không đổi); `app/src/main/AndroidManifest.xml` (không thêm `configChanges`, không đổi `screenOrientation`); `:core:*`; `gradle/libs.versions.toml`.

**Nơi khác đọc các file trên** (L-020), không cần sửa:

- `CameraScreen.kt`, `CameraControls.kt`: `CameraScreenTest.kt`, `CameraScreenPoseTest.kt`, `CameraLastPhotoTest.kt`, `CameraScreenOverlayTest.kt` (T-015), `CameraScreenScreenshotTest.kt` gọi `CameraScreen` với tham số cũ ở khung điện thoại (rộng 411 hoặc 360), nên vẫn vào `CameraPhoneLayout` không đổi; `AppLaunchTest.kt` chỉ chờ `camera_screen`, thẻ này ở cả hai bố cục.
- `PosePicker.kt`: chỉ `CameraScreen.kt` gọi; tham số mới có mặc định.
- `ReviewScreen.kt`, `ReviewComponents.kt`: `ReviewScreenTest.kt`, `ReviewScreenScreenshotTest.kt` ở khung 411 hoặc 360, vào bố cục cũ; `ReviewActions` mặc định ngang.
- `strings_review.xml`: chỉ thêm một chuỗi.
- Ảnh chuẩn mới: chỉ ba file test mới và `scripts/design-compare.sh` đọc.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Bố cục thuộc màn camera và màn xem lại, sau cờ `feature_camera_enabled`.
- Quảng cáo: Không chạm. Không có vị trí quảng cáo ở camera và màn xem lại (spec mục 5).
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `BoxWithConstraints` có trong `androidx.compose.foundation` (đã dùng ở `StepCard` của `:core:designsystem`); không dùng `material3-adaptive` hay `androidx.window` (chưa có trong `gradle/libs.versions.toml`).

### 3.4. Phương án dự phòng

Không có. Kỹ thuật đã có trong dự án (`BoxWithConstraints`, khung test theo `qualifiers`).

## 4. Kiểm thử

Mọi test dưới đây vào commit test của bước 3, cùng khung rỗng để biên dịch được: `isUnfolded` luôn trả `false`; `CameraUnfoldedLayout`, `ReviewUnfoldedContent` có đủ tham số nhưng gọi lại bố cục điện thoại; `PosePicker` đã nhận `chipsState`, `stripState` nhưng chưa dùng; `ReviewActions` đã nhận `vertical` nhưng chưa dùng; chữ mới có. Lý do thất bại ở cột cuối là điều AI dự kiến; bước 3 ghi lý do thật vào nhật ký.

Test giao diện ở khung gập dùng `@Config(qualifiers = "w673dp-h841dp-xxhdpi")`, `@GraphicsMode(NATIVE)`, cùng trạng thái mẫu của `CameraScreenScreenshotTest.samplePoses()` và `DEFAULT`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Ngưỡng máy gập | Unit test | `WindowLayoutTest.isUnfolded_from600dp`: 411, 599.9 là `false`; 600, 673 là `true`. Thất bại: khung rỗng trả `false` cho 673 |
| KB-11, camera: khung 3:4 bên trái | Compose UI test | `CameraScreenUnfoldedTest.preview_isLeft_3by4_600tall`: `PREVIEW` ở x 16, y 72 (sai số 4 dp), cỡ 450 x 600 (sai số 4 dp). Thất bại: khung rỗng dùng bố cục điện thoại, `PREVIEW` rộng 673 |
| KB-11, camera: các nút ở cột phải | Compose UI test | `captureControls_areInRightColumn`: mép trái của `SHUTTER`, `FLIP`, `LAST_PHOTO`, `ZOOM` đều lớn hơn mép phải `PREVIEW`; tâm ngang của bốn phần bằng nhau (sai số 2 dp); thứ tự trên xuống: `FLIP`, `SHUTTER`, `LAST_PHOTO`, `ZOOM`; tâm `SHUTTER` ngang tâm dọc `PREVIEW` (sai số 4 dp); `ZOOM` không nằm trong `PREVIEW` |
| KB-11, camera: thanh trên, chip và dải | Compose UI test | `topBarOverPreview_pickerUnderIt`: nút thư viện ở x 16, nút cài đặt kết thúc ở mép phải `PREVIEW` (sai số 4 dp); `CHIPS` bắt đầu dưới đáy `PREVIEW`, `STRIP` dưới `CHIPS`, cả hai rộng hết màn hình |
| KB-11, camera: nút vẫn dùng được | Compose UI test | `unfoldedControls_callTheSameCallbacks`: bấm `SHUTTER`, `FLIP`, `LAST_PHOTO`, một mức zoom, một chip, một ô: đúng callback như ở điện thoại |
| KB-11, camera: cột công cụ và lời nhắc trong khung | Compose UI test | `overlayToolsAndHints_stayInsideFrame`: `OVERLAY_TOOLS` (T-015) nằm trọn trong `PREVIEW`, cách mép phải 16 dp; với `lockHintVisible = true`, đáy `LOCK_HINT` cách đáy `PREVIEW` 16 dp (sai số 2 dp) |
| KB-11, gập lại: giữ trạng thái khi chuyển bố cục | Compose UI test, khung `w673dp-h891dp-xxhdpi` | `switchingLayout_keepsStripScrollAndPose`: dựng `CameraScreen` trong hộp có chiều rộng đổi được (`mutableStateOf`, 411 rồi 673 rồi 411); dải 20 ô, cuộn tới ô 15; đổi sang 673: `camera_pose_p15` vẫn hiện, ô đang chọn vẫn có `assertIsSelected`; đổi về 411: vẫn như vậy, bố cục điện thoại (`SHUTTER` nằm dưới `STRIP`). Thất bại: khung rỗng chưa dùng `stripState` đưa lên nên ô 15 không còn hiện sau khi đổi |
| KB-11, gập lại: activity dựng lại | Compose UI test | `stateRestoration_keepsStripScroll`: `StateRestorationTester`, cuộn dải tới ô 15, `emulateSavedInstanceStateRestore()`: ô 15 vẫn hiện |
| KB-11, xem lại: ảnh trái, nút phải | Compose UI test | `ReviewScreenUnfoldedTest.photoLeft_actionsAndSaveRight`: `PHOTO` ở x 16, cỡ 450 x 600 (sai số 4 dp); `RETAKE`, `SHARE`, `NEXT_POSE` ở bên phải `PHOTO`, xếp dọc theo thứ tự đó, cách nhau 24 (sai số 2 dp); `SAVE` ở bên phải `PHOTO`, đáy cách đáy màn hình 20 dp (sai số 2 dp), có chữ "Lưu ảnh"; đang lưu: "Đang lưu…" |
| KB-11, xem lại: hành vi như điện thoại | Compose UI test | `unfoldedReview_callsTheSameCallbacks`: bấm ba nút phụ, nút lưu, nút đóng gọi đúng callback; lưu lỗi: `SAVE_ERROR` ở cột phải, trên `SAVE`, "Thử lại" gọi `onRetrySave`; hộp thoại "Bỏ ảnh này?" hiện giữa màn hình, nội dung phía sau bị ẩn khỏi trình đọc màn hình (như điện thoại, L-004) |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | "Lưu ảnh" ở máy gập, "Lưu vào thư viện" ở điện thoại (test hiện có không đổi) |
| Đúng thiết kế | Screenshot test cùng tên, khung `w673dp-h841dp-xxhdpi` | `CameraUnfoldedScreenshotTest.default_dark_unfolded` (`src/test/screenshots/CameraScreen_default_dark_unfolded.png`): trạng thái như `default_dark`. `ReviewScreenUnfoldedTest.default_dark_unfolded` (`src/test/screenshots/ReviewScreen_default_dark_unfolded.png`): như `ReviewScreen_default_dark` |
| Cột phải nhiều nội dung nhất | Screenshot test, khung `w673dp-h841dp-xxhdpi` | `ReviewScreenUnfoldedTest.saveError_unfolded` (`src/test/screenshots/ReviewScreen_save-error_unfolded.png`): thông báo "Không lưu được ảnh: bộ nhớ máy đã đầy." xuống dòng trong cột phải, nút chính còn trọn |
| So với thiết kế | Ảnh so sánh và rà soát độc lập (bước 6) | Chạy `scripts/design-compare.sh camera` sau khi ghi ảnh chuẩn; commit `docs/features/camera/design/review/`, dán `review/summary.md` vào mô tả PR. Rà soát xem ảnh ghép của hai ảnh `_unfolded` theo mục 7 của README thiết kế. Khác biệt đã biết: thanh trên của màn xem lại ở đỉnh màn hình (thiết kế 8, phần trên dành cho thanh trạng thái như ảnh điện thoại); ảnh tạm |
| Ảnh thiết kế của ticket khác | | Ảnh thiết kế của ticket khác (T-013, T-014, T-015, T-016, T-017): `CameraScreen_starting_dark`, `CameraScreen_permission-denied_dark`, `CameraScreen_error_dark`, `CameraScreen_countdown_dark`, `CameraScreen_default_dark`, `CameraScreen_no-pose_dark`, `CameraScreen_steps-hidden_dark`, `CameraScreen_adjust_dark`, `CameraScreen_locked_dark`, `CameraScreen_matching_dark`, `CameraScreen_matched_dark`, `CameraScreen_no-person_dark`, `ReviewScreen_default_dark`, `ReviewScreen_saving_dark`, `ReviewScreen_save-error_dark`, `ReviewScreen_discard_dark` |
| Gập mở thật | Kiểm tra thủ công | Emulator "7.6 Fold-in with outer display" (Android Studio) hoặc máy gập thật, bản staging: mở camera ở màn ngoài, chọn tư thế, kéo nét viền, bật lưới và hẹn giờ; mở máy: bố cục `_unfolded`, cùng tư thế, cùng vị trí nét viền, camera chạy lại; chụp, màn xem lại ở bố cục gập; gập lại: bố cục điện thoại, mọi lựa chọn giữ. Lặp lại khi đang ở màn xem lại chưa lưu ảnh: ảnh còn, hộp "Bỏ ảnh này?" vẫn hiện khi rời |

**Ngoại lệ viết test trước:** Không có.

### 4.1. Chạy thử

Không cần (E dưới 2). Ghi thêm để người duyệt biết: lần chạy thử chung của T-015, T-017 (mục 4.1 của hai plan đó) có một test tạm đo `BoxWithConstraints` ở khung `w673dp-h841dp-xxhdpi`: `maxWidth` là 673 dp, nên ngưỡng 600 dp chọn đúng bố cục gập trong test; `scripts/check.sh` qua cùng lần đó.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có. Không chạm quảng cáo, sự kiện, quyền, thư viện, module core hay manifest.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một module, khoảng 17 file; đổi chữ ký `internal` của `CameraTopBar`, `PosePicker`, `ReviewActions` (thêm tham số có mặc định) |
| B. Độ rõ của yêu cầu | 1 | Spec và thiết kế rõ; AI tự quyết ngưỡng 600 dp, chỗ đặt thông báo lưu lỗi ở máy gập, cách co cột phải khi khung thấp hơn 600 (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Vị trí, chuyển bố cục, khôi phục trạng thái có test JVM; gập mở thật (activity dựng lại, camera mở lại) chỉ kiểm tra thủ công trên emulator gập hay máy thật |
| D. Ảnh hưởng chéo | 2 | Sửa `CameraScreen.kt`, `CameraControls.kt`, `PosePicker.kt` mà T-015, T-017 cũng sửa; `ReviewScreen.kt` mà T-018, T-019 có thể sửa (phần `ReviewRoute`) |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu đã có: `BoxWithConstraints` chọn bố cục như `StepCard` |
| **Tổng** | 8 | **Mức: Trung bình** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: chỉ trong `:feature:camera`: `CameraScreen.kt`, `CameraControls.kt`, `PosePicker.kt`, `ReviewScreen.kt`, `ReviewComponents.kt`, `strings_review.xml`, `docs/features/camera/design/review/`.
- Task khác có thể xung đột:
  - **T-015** (chỉnh nét viền): ticket T-020 phụ thuộc T-015 (cột công cụ có trong ảnh `_unfolded`; tham số `hintBottomPadding`, `zoomVisible`, cách đặt cột công cụ theo cỡ khung của T-015). Cùng sửa `CameraScreen.kt`, `CameraControls.kt`, `PosePicker.kt`.
  - **T-017** (điểm khớp): không bắt buộc chạy trước. Cùng sửa `CameraScreen.kt`, `CameraControls.kt` (`ShutterButton` đổi màu viền), chỉ xung đột dòng. T-017 đặt `MatchHint` theo `hintBottomPadding`, nên chạy trước hay sau T-020 đều đúng chỗ ở máy gập.
  - **T-018, T-019** (quảng cáo, hỏi đánh giá khi rời màn xem lại): có thể sửa `ReviewRoute` trong `ReviewScreen.kt` và `ReviewViewModel.kt`; T-020 chỉ sửa hàm `ReviewScreen` (bố cục), không đụng `ReviewRoute`, `ReviewViewModel`. Chỉ xung đột dòng nếu cùng phiên.
  - **T-024** (thư viện tư thế trên máy gập): cùng ý chọn bố cục theo chiều rộng 600 dp nhưng ở `:feature:poses`, không chung file (mục 8, câu 2).
- Đề xuất thứ tự chạy: sau T-015; trong nhóm camera: T-015, T-017, T-020 (T-020 cuối để ghi ảnh `_unfolded` một lần với đủ phần của T-015, T-017). Nếu T-017 dừng, T-020 vẫn chạy được (ticket không phụ thuộc T-017).

## 7. Điều kiện dừng riêng

1. Lúc bắt đầu task, thiếu phần của T-014, T-015, T-016 mà plan dựa vào: `PosePicker`, `PreviewContent` với `zoomVisible`, tham số `hintBottomPadding`, cột công cụ nét viền, `CaptureRow`, `ReviewActions`, `ReviewPhoto`, `SaveErrorMessage`. Dừng.
2. Để camera không mất hình khi chuyển bố cục cần sửa `CameraXController.kt` hay `CameraViewModel.kt` (không có ở mục 3.2): dừng.
3. T-018 hoặc T-019 đã merge và đã chuyển phần bố cục của `ReviewScreen` sang file không có ở mục 3.2: dừng. Chỉ khác dòng trong `ReviewScreen.kt` thì làm tiếp.

## 8. Câu hỏi cho người duyệt

1. **Nhận biết "máy gập mở ra".** Đề xuất của AI: theo chiều rộng cửa sổ từ 600 dp, không đọc trạng thái bản lề (cần thư viện `androidx.window`, chưa có). Máy tính bảng và cửa sổ rộng (chia đôi màn hình trên máy gập) cũng dùng bố cục này; màn ngoài của máy gập và điện thoại dọc dùng bố cục điện thoại.
2. **Hàm ngưỡng dùng chung với thư viện tư thế (T-024).** Plan này giữ `isUnfolded` trong `:feature:camera`, vì feature không được phụ thuộc nhau và thêm vào core là điểm chặn 7. Đề xuất của AI: T-024 dùng cùng ngưỡng 600 dp trong `:feature:poses`; khi có feature thứ ba cần thì đưa vào `:core:designsystem` bằng một ticket riêng.
3. **Thông báo lưu lỗi ở máy gập.** Thiết kế không có trạng thái `save-error` khi mở gập. Đề xuất của AI: đặt ở cột phải, ngay trên nút "Lưu ảnh", rộng hết cột.
4. **Khung thấp hơn 600 ở cửa sổ rộng** (máy tính bảng nằm ngang, máy gập mở và xoay ngang): đề xuất của AI: khung lấy hết chiều cao còn lại, giữ 3:4, khoảng cách các nút ở cột phải co theo tỷ lệ. Không có ảnh thiết kế cho trường hợp này.
5. **Thanh trên ở máy gập.** Thiết kế đặt thanh trên camera ở đỉnh 16 (điện thoại 32) và thanh tiêu đề màn xem lại ở 8. Đề xuất của AI: camera dùng lề trên `Spacing.md` ở máy gập; màn xem lại giữ như điện thoại (thanh tiêu đề sát dưới thanh trạng thái, ảnh điện thoại cũng làm vậy).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-23, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### 2026-10-23, RUN-20261023-04: đã merge

- **Kết quả:** đã merge, PR #76, commit `b811368`. CI xanh ngay vòng đầu. T-017 chưa merge (xem báo cáo phiên) nên không có phần `MatchHint`; plan cho phép.
- **Test viết trước:** 24 test mới (2 unit, 19 giao diện, 3 screenshot); trên khung rỗng 10 test giao diện và 1 test ngưỡng thất bại đúng lý do (khung ở x=150 thay vì 16, nút chụp dưới dải tư thế, `isUnfolded(673)` false). Viết chặt lại có đối chứng: 5 (2 thêm khẳng định bố cục gập đầu test; 3 làm hỏng code tạm rồi hoàn lại). `switchingLayout_keepsStripScrollAndPose` thất bại sớm ở bước khẳng định bố cục (lệch dự đoán plan: phần giữ cuộn chưa được thử ở bước 3).
- **Lệch so với plan:** (1) lề dưới camera gập `Spacing.xl + Spacing.xs` (36 dp) thay `Spacing.md`, vì `Spacing.md` cho khung cao 621 dp thay vì 600; (2) nút đổi camera lệch 122 dp (số liệu `design/context`); (3) ảnh xem lại giới hạn bề rộng, cột phải tối thiểu 175 dp; (4) `SaveErrorMessage` thêm `actionOnNewLine` vì ở cột hẹp "Thử lại" đè lên chữ (thất bại trước khi sửa); (5) `ReviewPhoneContent` tách ra, `ERROR_STATUSES`, `PREVIEW_ASPECT_RATIO`, `DEFAULT_STEP_BOTTOM`, `TOP_BAR_HEIGHT` đổi sang `internal`; (6) `ReviewScreenUnfoldedTest.kt` tách hai lớp: giao diện và screenshot (CLAUDE.md bước 10).
- **Rà soát độc lập:** 0 chặn; 2 nên sửa, chưa sửa: cột phải màn xem lại không cuộn khi cửa sổ rộng từ 600 dp nhưng thấp hoặc cỡ chữ lớn (`ReviewUnfolded.kt`), nên bọc `verticalScroll`; chưa có test đo vị trí dọc nút đổi camera (122 dp) và ô ảnh vừa chụp (152 dp). Ghi chú: chưa có ca `PERMISSION_BLOCKED` ở bố cục gập.
- **Cần người kiểm tra:** gập mở thật trên máy gập hoặc emulator gập (mục 4); 3 ảnh chuẩn `_unfolded`; ảnh so sánh thiết kế (camera gập 5,4%, xem lại gập 3,6%).
