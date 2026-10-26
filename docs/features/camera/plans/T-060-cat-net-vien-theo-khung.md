---
ticket: T-060
title: Nét viền không vẽ ra ngoài khung xem trước
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 2                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: low              # low | medium | high
token_estimate: 5.8M         # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-060: NÉT VIỀN KHÔNG VẼ RA NGOÀI KHUNG XEM TRƯỚC

## 1. Tóm tắt

Trên bố cục điện thoại, lớp nét viền (đã dời, phóng, lật) không bị cắt theo khung xem trước. Vì vậy ở cỡ 160% hay khi kéo ra mép, nét viền đè lên thanh trên, hàng nhóm, dải tư thế và hàng nút chụp (FB-20261026-03). Task này cắt riêng lớp nét viền theo khung (`clipToBounds()` đặt trước `graphicsLayer` trong `PoseOverlay`), không cắt cả khung, vì cột công cụ ở trường hợp 4 của T-015 được cố ý tràn 16 dp khỏi đáy khung. Hai test điểm ảnh mới đã thất bại trên code hiện tại (độ sáng 255 ở thanh trên, giữa hàng nhóm và dải tư thế, ở hàng nút chụp). Với cách sửa tạm, cả hai qua và 27 ảnh chuẩn của `:feature:camera` không đổi.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-3 | Khi nét viền to hơn khung hay bị kéo ra một phần ngoài khung thì phần ngoài khung không được vẽ: nét viền không đè lên thanh trên, hàng nhóm, dải tư thế hay hàng nút chụp (FB-20261026-03) | Cắt cả nét trắng lẫn quầng tối của nét viền theo khung 3:4, ở mọi bố cục. Bố cục mở gập đã cắt cả khung (`CameraUnfolded.kt:117`), thêm lớp cắt ở `PoseOverlay` không đổi gì ở đó. |
| KB-3 | Khi chụm hoặc mở hai ngón thì nét viền nhỏ lại hoặc to lên, trong khoảng 40% đến 160% | Không đổi: `MIN_SCALE`, `MAX_SCALE` giữ nguyên. |
| KB-3, trường hợp đặc biệt | Khi kéo nét viền ra sát mép thì luôn còn ít nhất một phần nét viền trong khung | Không đổi: `MAX_OFFSET` 0.5 giữ nguyên, tâm hộp nét viền luôn trong khung. Test mới kiểm thêm phần trong khung vẫn được vẽ (đối chứng, L-035). |

Thiết kế tham chiếu: `features/camera/design/README.md`, mục 3: nét viền nằm trong "Khung xem trước". Không có ảnh thiết kế mới; không ảnh nào đổi.

ADR liên quan: ADR-0020 (ảnh thiết kế và screenshot test).

## 3. Giải pháp

### 3.1. Cách làm

Các con của khung xem trước trên điện thoại (`CameraScreen.kt:313-345`, `PreviewContent` > `ReadyPreview` trong `CameraControls.kt:220-240`, `PoseOverlay` trong `PoseOverlay.kt`), và ảnh hưởng nếu cắt:

| Con của khung | Vẽ ra ngoài khung hiện nay? | Sau task |
| --- | --- | --- |
| Luồng camera (`preview`) | Không, `fillMaxSize` | Không đổi |
| `GridOverlay` | Không | Không đổi |
| Vùng kéo và chụm (`OVERLAY_GESTURES`, `matchParentSize`) | Không | Không đổi |
| **Lớp nét viền** (`Box` `matchParentSize().graphicsLayer { translation, scale, alpha }`, `PoseOverlay.kt:108-114`) | **Có**: `graphicsLayer` mặc định `clip = false`, nên phần đã dời hay đã phóng vẽ ra ngoài; quầng tối `blur(..., BlurredEdgeTreatment.Unbounded)` cũng vậy | **Cắt theo khung** |
| Thẻ bước, nút "Bước" (cách mép `Spacing.md`) | Không; bóng đổ `shadow/sticker` lệch 4 dp vẫn nằm trong lề 16 dp | Không đổi (không cắt) |
| Cột công cụ trong khung (trường hợp 2 và 4 của `toolColumnPlacement`) | Trường hợp 4: **cố ý** tràn tối đa 16 dp khỏi đáy khung (plan T-015, mục 3.1, cách 4) | Không đổi (không cắt), vì vậy không cắt cả khung |
| Cột công cụ ở lề ngoài khung | Là con của `BoxWithConstraints`, không phải của khung | Không đổi |
| Bảng chỉnh, lời nhắc khóa, lời nhắc khớp dáng, zoom, lớp mờ đếm ngược | Không, đều trong khung có lề | Không đổi |

Cách làm: trong `PoseOverlay.kt`, đặt `clipToBounds()` vào trước `graphicsLayer` của lớp nét viền:

```kotlin
Box(
    modifier = Modifier.matchParentSize().clipToBounds().graphicsLayer {
        translationX = overlay.offsetX * size.width
        ...
    },
)
```

`clipToBounds()` cắt theo hộp chưa biến đổi (bằng khung, vì `matchParentSize`). `graphicsLayer` phía sau dời và phóng nội dung bên trong hộp cắt đó. Không dùng `graphicsLayer { clip = true }`, vì vùng cắt sẽ dời và phóng theo nét viền. Thêm một dòng chú thích tiếng Anh vào chú thích của lớp.

Không đổi `CameraScreen.kt`, `CameraUnfolded.kt`, `OverlayGeometry.kt`. Không đổi giới hạn cỡ hay giới hạn kéo.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseOverlay.kt` | `clipToBounds()` trước `graphicsLayer` của lớp nét viền, import, chú thích |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenOverlayTest.kt` | Hai test mới ở mục 4, ảnh mẫu `FULL_IMAGE`, các hàm phụ `boundsOf`, `windowBitmap`, `lumaAt(bitmap, x, y)`, import `MAX_OFFSET`, `MAX_SCALE` |
| Sửa | `docs/features/camera/plans/T-060-cat-net-vien-theo-khung.md` | Nhật ký thực hiện |
| Sửa | `docs/features/camera/tickets/T-060-cat-net-vien-theo-khung.md` | Trạng thái |
| Sửa | `docs/features/camera/feedback/FB-20261026-03-net-vien-tran-khung.md` | Trạng thái `fixed` sau khi merge (nhánh báo cáo) |

Không ảnh chuẩn nào được ghi lại. Đã chạy `:feature:camera:verifyRoborazziDebug` với cách sửa tạm: 27 ảnh `unchanged`, 0 `changed` (mục 4.1). Bất kỳ ảnh chuẩn nào đổi là điều kiện dừng (mục 7).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm. `androidx.compose.ui.draw.clipToBounds` thuộc `compose-ui`, đã có trên classpath biên dịch của `:feature:camera` (cùng gói với `clip` đang import ở `PoseOverlay.kt`).

### 3.4. Phương án dự phòng

Không có. Cách sửa đã chạy thử, qua cả hai test mới và toàn bộ test của module (mục 4.1).

## 4. Kiểm thử

Cả hai test dùng `CameraScreenOverlayTest` (khung `vi-w411dp-h891dp-xxhdpi`, `GraphicsMode.NATIVE`, giống khung nơi phát hiện lỗi). Nét viền là ảnh trắng đặc 100 x 200 (`FULL_IMAGE`), nên mọi điểm trong hộp nét viền có độ sáng 255. Điểm đo lấy từ `boundsInWindow` của node trên ảnh vẽ `decorView` (L-006). Nền camera `#0A0A0B` có độ sáng khoảng 10.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-3, nét viền không đè thanh trên; phần trong khung vẫn vẽ | Compose UI test (điểm ảnh) | `CameraScreenOverlayTest.outline_largestAndMovedUp_notDrawnOverTopBar`: `OverlayState(settings = OverlaySettings(scale = MAX_SCALE), offsetY = -MAX_OFFSET)`. Điểm ở thanh trên, giữa cạnh phải của `POSES` và cạnh trái của `FLASH`, ở `poses.center.y`: độ sáng `< 60`. Đối chứng: điểm cùng x, ở `preview.top + 8 dp`: độ sáng `> 200` |
| KB-3, nét viền không đè hàng nhóm, dải tư thế, hàng nút chụp | Compose UI test (điểm ảnh) | `CameraScreenOverlayTest.outline_largestAndMovedDown_notDrawnOverPickerOrCaptureRow`: `scale = MAX_SCALE`, `offsetY = MAX_OFFSET`. Điểm ở `preview.center.x - 60 dp`, giữa đáy `CHIPS` và đỉnh `STRIP`: `< 60`. Điểm giữa cạnh phải `LAST_PHOTO` và cạnh trái `SHUTTER`, ở `shutter.center.y`: `< 60`. Đối chứng: điểm ở `preview.center.x - 60 dp`, `preview.bottom - 8 dp`: `> 200` |
| KB-3, cỡ 40% đến 160%, dời theo phần của khung | Compose UI test (có sẵn) | `outline_followsOffsetScaleMirrorOpacity`, `outline_mirrored_flipsTheImage`, `outline_opacity_fadesTheOutline` và `OverlayGeometryTest` vẫn qua |
| Bố cục mở gập không đổi | Screenshot test (có sẵn) | `CameraScreen_default_dark_unfolded.png` không đổi |
| Giao diện các màn camera không đổi | Screenshot test (có sẵn) | 19 ảnh `CameraScreen_*` và 8 ảnh `ReviewScreen_*` không đổi (`verifyRoborazziDebug`) |
| Đúng thiết kế (khi có `design/screens/`) | Ảnh chuẩn có sẵn, cùng tên ảnh thiết kế | Không thêm hay ghi lại ảnh nào. Ảnh thiết kế của ticket khác (T-013, T-014, T-015, T-016, T-017, T-020): `CameraScreen_default_dark`, `CameraScreen_no-pose_dark`, `CameraScreen_adjust_dark`, `CameraScreen_locked_dark`, `CameraScreen_steps-hidden_dark`, `CameraScreen_matching_dark`, `CameraScreen_matched_dark`, `CameraScreen_no-person_dark`, `CameraScreen_countdown_dark`, `CameraScreen_starting_dark`, `CameraScreen_permission-denied_dark`, `CameraScreen_error_dark`, `CameraScreen_default_dark_unfolded`, `ReviewScreen_default_dark`, `ReviewScreen_saving_dark`, `ReviewScreen_save-error_dark`, `ReviewScreen_discard_dark`, `ReviewScreen_default_dark_unfolded` |
| Nét viền khi phóng và kéo trên máy | Kiểm tra thủ công (máy thật: cử chỉ nhiều ngón) | Trên bản staging: mở hai ngón tới 160%, kéo nét viền sát mép trên rồi mép dưới; nét viền dừng ở mép khung, không hiện trên thanh trên hay hàng dưới khung |

Mã của hai test đúng như đã chạy thử (mục 4.1); thêm import `MAX_OFFSET`, `MAX_SCALE` từ gói `pose`:

```kotlin
    // The outline stays in the frame (spec camera, KB-3; plan T-060)

    @Test
    fun outline_largestAndMovedUp_notDrawnOverTopBar() {
        setScreen(
            overlay = OverlayState(settings = OverlaySettings(scale = MAX_SCALE), offsetY = -MAX_OFFSET),
            selected = sampleSelected(outline = FULL_IMAGE),
        )
        val bitmap = windowBitmap()
        val preview = boundsOf(CameraTestTags.PREVIEW)
        val poses = boundsOf(CameraTestTags.POSES)
        val flash = boundsOf(CameraTestTags.FLASH)
        val inTopBar = lumaAt(bitmap, (poses.right + flash.left) / 2, poses.center.y)
        val inFrame = lumaAt(bitmap, (poses.right + flash.left) / 2, preview.top + 8f * composeRule.density.density)
        assertTrue("outline is drawn in the frame: $inFrame", inFrame > 200)
        assertTrue("outline is drawn over the top bar: $inTopBar", inTopBar < 60)
    }

    @Test
    fun outline_largestAndMovedDown_notDrawnOverPickerOrCaptureRow() {
        setScreen(
            overlay = OverlayState(settings = OverlaySettings(scale = MAX_SCALE), offsetY = MAX_OFFSET),
            selected = sampleSelected(outline = FULL_IMAGE),
        )
        val bitmap = windowBitmap()
        val density = composeRule.density.density
        val preview = boundsOf(CameraTestTags.PREVIEW)
        val chips = boundsOf(CameraTestTags.CHIPS)
        val strip = boundsOf(CameraTestTags.STRIP)
        val lastPhoto = boundsOf(CameraTestTags.LAST_PHOTO)
        val shutter = boundsOf(CameraTestTags.SHUTTER)
        val x = preview.center.x - 60f * density
        val betweenRows = lumaAt(bitmap, x, (chips.bottom + strip.top) / 2)
        val captureRow = lumaAt(bitmap, (lastPhoto.right + shutter.left) / 2, shutter.center.y)
        val inFrame = lumaAt(bitmap, x, preview.bottom - 8f * density)
        assertTrue("outline is drawn in the frame: $inFrame", inFrame > 200)
        assertTrue("outline is drawn between chips and strip: $betweenRows", betweenRows < 60)
        assertTrue("outline is drawn over the capture row: $captureRow", captureRow < 60)
    }

    private fun boundsOf(tag: String) = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow

    private fun windowBitmap(): Bitmap {
        composeRule.waitForIdle()
        val root = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        return Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888).also { root.draw(Canvas(it)) }
    }

    private fun lumaAt(bitmap: Bitmap, x: Float, y: Float): Int =
        (Color(bitmap.getPixel(x.toInt(), y.toInt())).let { (it.red + it.green + it.blue) / 3f } * 255).toInt()

    // In the companion object:
        /** All white, 1:2 like an outline. */
        val FULL_IMAGE = PoseImage.Ready(
            ImageBitmap(100, 200).also { image ->
                ComposeCanvas(image).drawRect(0f, 0f, 100f, 200f, Paint().apply { color = Color.White })
            },
        )
```

Hàm phụ `boundsOf`, `windowBitmap`, `lumaAt(bitmap, x, y)` và `FULL_IMAGE` thuộc commit test của bước 3, vì chúng chỉ đọc màn hình, không đổi hành vi (L-025). Hàm `lumaAt(tag, fraction)` hiện có giữ nguyên.

**Ngoại lệ viết test trước:** Không có. Dòng kiểm tra trên máy thật chỉ để xác nhận thêm; tiêu chí đã có test tự động.

### 4.1. Chạy thử

Plan sửa lỗi đo được (L-029), nên đã chạy hai test trên code hiện tại, rồi với cách sửa tạm. Tiêu chí E là 0.

- **Đã chạy thử:** hai test ở mục 4, viết tạm vào `CameraScreenOverlayTest.kt`, ngày 2026-10-26, trên nhánh `docs/fb-camera-may-that` 68f5959 (gốc là nhánh chính 0939acf). Không commit.
- **Lệnh:** `./gradlew :feature:camera:testDebugUnitTest --tests '*CameraScreenOverlayTest.outline_largest*' -i`, rồi `./gradlew :feature:camera:verifyRoborazziDebug -i` với cách sửa tạm.
- **Kết quả:**
  - **Code hiện tại:** 2 test thất bại đúng lý do.
    - `outline_largestAndMovedUp_notDrawnOverTopBar`: `inTopBar=255 inFrame=255`, "outline is drawn over the top bar: 255".
    - `outline_largestAndMovedDown_notDrawnOverPickerOrCaptureRow`: `betweenRows=255 captureRow=255 inFrame=255`, "outline is drawn between chips and strip: 255".
  - **Cách sửa tạm** (`clipToBounds()` trước `graphicsLayer`): `inTopBar=10 inFrame=255`; `betweenRows=10 captureRow=10 inFrame=255`. Cả hai qua. Toàn bộ test của `:feature:camera` qua. Roborazzi `total=27, changed=0, unchanged=27`.
  - Chưa chạy `scripts/check.sh` đầy đủ. Kỹ thuật không mới (E = 0), nên không bắt buộc.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một file code và một file test trong `:feature:camera`, không đổi giao diện công khai |
| B. Độ rõ của yêu cầu | 0 | Spec đã xác nhận lại ngày 2026-10-26 với tiêu chí mới ở KB-3; thiết kế đặt nét viền trong khung |
| C. Khả năng kiểm chứng tự động | 0 | Tiêu chí có test điểm ảnh; ảnh chuẩn có sẵn giữ cho phần còn lại không đổi |
| D. Ảnh hưởng chéo | 1 | `PoseOverlay` dùng ở cả bố cục điện thoại và bố cục mở gập; T-059 và T-061 cũng sửa `PoseOverlay.kt` (mục 6) |
| E. Độ mới của kỹ thuật | 0 | `clipToBounds`/`clip` đã dùng trong dự án (`CameraUnfolded.kt:117`) |
| **Tổng** | 2 | **Mức: Thấp** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: không. `PoseOverlay.kt` thuộc `:feature:camera`.
- Task khác trong sprint có thể xung đột:
  - T-059 (chạm thẻ bước) sửa phần thẻ bước của `PoseOverlay.kt`.
  - T-061 (khóa nút khi đếm ngược) có thể sửa `PoseOverlay.kt` (mũi tên thẻ bước, cột công cụ) và `CameraScreenOverlayTest.kt`.
  - T-060 chỉ thêm một modifier ở lớp nét viền và thêm test ở một mục riêng, nên xung đột nhỏ.
- Đề xuất thứ tự chạy: trước T-059 và T-061, vì là thay đổi nhỏ nhất trên cùng file. Không phụ thuộc T-058.

## 7. Điều kiện dừng riêng

1. `verifyRoborazziDebug` báo bất kỳ ảnh chuẩn nào đổi: dừng. Plan không cho ghi lại ảnh nào.
2. Cách sửa làm thất bại một test có sẵn của cột công cụ hay bảng chỉnh (`small_*`, `toolColumn_*`): dừng, vì lớp cắt đã lan ra ngoài lớp nét viền.

## 8. Câu hỏi cho người duyệt

1. **Có thêm ảnh chuẩn cho trạng thái "nét viền 160%, kéo lên sát mép" không?** Hai test điểm ảnh đã chặn lỗi.
   - Đề xuất của AI: không thêm. Không có ảnh thiết kế tương ứng, và mỗi ảnh chuẩn thêm khoảng 3 MB vào repo.
2. **Có giới hạn kéo chặt hơn không** (ví dụ luôn còn ít nhất nửa nét viền trong khung)?
   - Tuấn đã chọn ngày 2026-10-26: giữ giới hạn như spec (FB-20261026-03, mục Phân loại). Plan không đổi `OverlayGeometry.kt`; câu này chỉ để ghi lại.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
