---
ticket: T-059
title: Chạm thẻ bước để thu gọn
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 5                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: medium           # low | medium | high
token_estimate: 6M           # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-26
---

# PLAN T-059: CHẠM THẺ BƯỚC ĐỂ THU GỌN

## 1. Tóm tắt

Thẻ bước trên camera chỉ ẩn được bằng cách vuốt lên, không có dấu hiệu nào trên màn (FB-20261026-02). Plan thêm thao tác chạm vào thẻ (ở bất kỳ chỗ nào ngoài hai mũi tên) để ẩn thẻ, giữ vuốt lên, bằng một `clickable` không hiệu ứng gợn sóng trên `HideableStepCard` của `PoseOverlay.kt`, và đổi bộ dò vuốt dọc thành bộ dò kéo cả hai chiều để kéo ngang trên thẻ không bị tính là chạm. Đã chạy thử trên nhánh có code: 14 trường hợp tạm đúng như mong đợi, 27 screenshot test của `:feature:camera` không đổi.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md` (bản `confirmed` ngày 2026-10-26, PR #162):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-2 | Khi chạm vào thẻ bước, ở bất kỳ chỗ nào ngoài hai mũi tên, hoặc vuốt thẻ lên, thì thẻ ẩn và một nút nhỏ "Bước" hiện ở góc trên khung xem trước; bấm nút này thì thẻ hiện lại (câu hỏi 2) | Chạm số bước "2/4", chữ bước, hay phần trống của thẻ đều ẩn thẻ (gọi `onHideSteps` một lần). Chạm mũi tên chỉ chuyển bước; mũi tên đang mờ (bước đầu, bước cuối) không làm gì, kể cả không ẩn thẻ. Vuốt lên vẫn ẩn như cũ: bộ dò kéo chỉ báo phần dời sau ngưỡng chạm, nên ngưỡng ẩn thật là ngưỡng chạm cộng 24 dp (khoảng 32 dp trên máy, 40 dp trên Robolectric), không đổi so với hiện nay. Kéo (ngang hay dọc) quá ngưỡng chạm mà chưa tới ngưỡng ẩn thì không ẩn. Nút "Bước" giữ nguyên |
| KB-2 | Khi chuyển bước bằng mũi tên … mũi tên tương ứng mờ đi và không làm gì | Giữ nguyên; test chặn việc chạm mũi tên mờ rơi xuống thẻ và ẩn thẻ |
| KB-2 | Khi thẻ bước đang ẩn thì nó ẩn cho tới khi người dùng hiện lại | Không đổi: chạm gọi cùng `onHideSteps` như vuốt, `PoseViewModel` lưu như cũ (`PoseViewModelTest.hideSteps_isSaved_andKeptWhenPoseChanges`) |

Thẻ báo lỗi "Chưa tải được nét viền" kèm nút "Thử lại" dùng cùng `HideableStepCard`: chạm thẻ (ngoài nút "Thử lại") cũng ẩn thẻ (câu hỏi 2 ở mục 8).

Thiết kế tham chiếu: `features/camera/design/README.md` (dòng `StepCard`: "chạm thẻ ngoài mũi tên thì thẻ ẩn, như vuốt lên"), ảnh `CameraScreen_default_dark`, `CameraScreen_steps-hidden_dark`, `CameraScreen_default_dark_unfolded`. Thao tác không đổi hình: không có ảnh thiết kế mới, không ghi lại ảnh chuẩn.

ADR liên quan: ADR-0008 (hệ thống thiết kế; plan không sửa `StepCard` của `:core:designsystem`), ADR-0020 (screenshot theo thiết kế).

## 3. Giải pháp

### 3.1. Cách làm

Mọi thay đổi code nằm trong `HideableStepCard` (`feature/camera/.../pose/PoseOverlay.kt`, khoảng dòng 212-257). `StepCard` và `StepCardArrows` của `:core:designsystem` giữ nguyên.

1. Đổi `detectVerticalDragGestures` thành `detectDragGestures`: vẫn `change.consume()` mỗi lần kéo, cộng `amount.y` vào `dragged`, ẩn một lần khi `dragged < -hideDistance` (24 dp tính sau ngưỡng chạm), như cũ. Lý do: `clickable` của Compose không tự hủy khi ngón tay di chuyển trong vùng của thẻ; chỉ hủy khi lần di chuyển bị bộ khác "tiêu thụ" (consume). Bộ dò vuốt dọc không tiêu thụ kéo ngang, nên kéo ngang 40 dp trên thẻ bị tính là chạm và ẩn thẻ (đo ở mục 4.1). Bộ dò kéo hai chiều tiêu thụ mọi lần kéo quá ngưỡng chạm, nên chạm bị hủy.
2. Bỏ `semantics { customActions = ... "Ẩn thẻ bước" ... }`, thêm vào cuối chuỗi modifier:
   ```kotlin
   .clickable(
       interactionSource = null,
       indication = null,
       onClickLabel = hideLabel,
       onClick = { currentOnHide() },
   )
   ```
   - `onClickLabel` dùng lại chuỗi `camera_cd_hide_steps` (đã có ở 7 ngôn ngữ), nên TalkBack đọc "nhấn đúp để Ẩn thẻ bước"; hành động riêng cũ thành thừa (câu hỏi 3).
   - `indication = null`: không có gợn sóng. Gợn sóng cần `clip` theo góc bo của thẻ; thử với `.clip(RoundedCornerShape(Radius.card))` thì 13 trong 27 screenshot test của camera lệch ảnh chuẩn (lớp vẽ mới làm đổi điểm ảnh ở góc thẻ và ở nét viền), còn gợn sóng không cắt thì tràn ra ngoài góc bo. Thẻ biến mất ngay khi nhấc tay nên gợn sóng gần như không thấy (câu hỏi 1).
   - Không thêm `role`: thẻ không phải nút; TalkBack vẫn đọc nhãn hành động.
3. Mũi tên: `StepArrow` đang bật có `clickable` riêng nên tiêu thụ chạm trước thẻ; mũi tên đang mờ dùng `clickable(enabled = false)`, đo ở mục 4.1 thấy chạm vào đó cũng không rơi xuống thẻ. Không cần sửa `:core:designsystem`.
4. Chạm thẻ không đi xuống vùng kéo nét viền (`OVERLAY_GESTURES`) vì thẻ nằm trên và đã có bộ nhận chạm, như hiện nay; test chặn ở mục 4.
5. Cập nhật chú thích KDoc của `HideableStepCard`: chạm hoặc vuốt lên đều ẩn; TalkBack dùng hành động nhấp có nhãn "Ẩn thẻ bước" (spec camera KB-2, plan T-059).

Bố cục mở gập (`CameraUnfolded.kt:129`) gọi cùng `PoseOverlay`, nên có cùng hành vi, không sửa file này.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseOverlay.kt` | `HideableStepCard`: `detectDragGestures` thay `detectVerticalDragGestures`; `clickable(interactionSource = null, indication = null, onClickLabel = hideLabel)` thay hành động riêng; sửa import (bỏ `detectVerticalDragGestures`, `CustomAccessibilityAction`, `customActions` nếu không còn dùng); sửa KDoc. Phương án dự phòng ở mục 3.4 cũng chỉ sửa file này |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenPoseTest.kt` | Test mới và hai test sửa ở mục 4 |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenUnfoldedTest.kt` | Thêm `onHideSteps = { calls += "hide" }` vào `actions`; test chạm thẻ ở bố cục mở gập |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenOverlayTest.kt` | Test chạm thẻ không đổi nét viền |

Không file nào khác đọc `PoseOverlay.kt` hay các test này (`grep -rn "HideableStepCard\|camera_cd_hide_steps" scripts/ .github/ */src` chỉ thấy `PoseOverlay.kt` và tệp chuỗi). Ảnh chuẩn không đổi (đã kiểm ở mục 4.1).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm (spec không có sự kiện cho ẩn hiện thẻ bước)
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm. `clickable(interactionSource, indication, ...)` và `detectDragGestures` thuộc `androidx.compose.foundation`, đã có trên classpath của `:feature:camera` (BOM `composeBom` 2025.09.00 trong `gradle/libs.versions.toml`)

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** trên Robolectric ngưỡng chạm là 16 dp (đo ở mục 4.1), trên máy thật thường là 8 dp; cách `clickable` và bộ dò kéo cùng chạy trên một chuỗi modifier có thể khác giữa phiên bản Compose.
- **Điều kiện chuyển:** sau 2 lần sửa, một test ở mục 4 về kéo (ngang 40 dp, lên ngưỡng chạm cộng 4 dp) vẫn thấy `hide`, hoặc chạm thẻ không gọi `hide`.
- **Cách làm thay:** bỏ `clickable`, viết một `pointerInput` duy nhất bằng `awaitEachGesture`: `awaitFirstDown`, theo dõi độ dời; quá `viewConfiguration.touchSlop` thì chuyển sang chế độ kéo (tiêu thụ, ẩn khi phần dời lên sau ngưỡng chạm quá 24 dp, như hiện nay); nhấc tay khi chưa quá ngưỡng và không có thay đổi nào bị tiêu thụ thì gọi `onHide`. Phần trợ năng dùng `semantics { onClick(label = hideLabel) { currentOnHide(); true } }`. Cùng file `PoseOverlay.kt`, test giữ nguyên.

## 4. Kiểm thử

Test viết trước trong các file ở mục 3.2. `calls` của `CameraScreenPoseTest` nhận cả `shown <id>` từ dải tư thế ngay sau khi dựng màn (`onThumbShown`, `CameraScreenPoseTest.kt:62-72`): mỗi test mới gọi `composeRule.waitForIdle()` rồi `calls.clear()` sau `setScreen(...)`, trước thao tác cần đo; các so sánh `calls == [...]` dưới đây tính sau bước đó. Khung chuẩn `vi-w411dp-h891dp-xxhdpi` và `vi-w673dp-h841dp-xxhdpi` (mở gập), như các lớp test đang có. Kết quả trên code hiện tại lấy từ lần chạy test tạm ở mục 4.1.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-2, chạm chữ bước thì ẩn | Compose UI test | `CameraScreenPoseTest.tapStepText_hides`: `onNodeWithText("Step 2", useUnmergedTree = true).performClick()`, `calls == ["hide"]`. Code hiện tại: thất bại, `calls` rỗng |
| KB-2, chạm số bước thì ẩn | Compose UI test | `CameraScreenPoseTest.tapStepBadge_hides`: chạm `"2/4"` (cây chưa gộp), `calls == ["hide"]`. Code hiện tại: thất bại, `calls` rỗng |
| KB-2, chạm mũi tên chỉ chuyển bước | Compose UI test | `CameraScreenPoseTest.tapEnabledArrow_movesStep_doesNotHide`: chạm "Bước sau", `calls == ["next"]`. Code hiện tại: qua (đối chứng) |
| KB-2, mũi tên mờ không làm gì | Compose UI test | `CameraScreenPoseTest.tapDisabledArrow_doesNothing`: `selected(stepIndex = 3)`, chạm "Bước sau", `calls` rỗng. Code hiện tại: qua (đối chứng; chặn việc chạm rơi xuống thẻ) |
| KB-2, kéo ngang trên thẻ không ẩn | Compose UI test | `CameraScreenPoseTest.sidewaysDragOnStepCard_doesNotHide`: `down(center)` trên chữ bước, 8 lần `moveBy(5 dp, 0)`, `up()`, `calls` rỗng. Chặn phương án `clickable` cùng bộ dò vuốt dọc: đo được `["hide"]` |
| KB-2, kéo lên chưa tới ngưỡng ẩn không ẩn | Compose UI test, sửa test cũ | `CameraScreenPoseTest.smallDragUp_doesNotHide`: đổi từ một lần `moveBy(0, -10 dp)` thành tổng dời `viewConfiguration.touchSlop + 4.dp` lên, chia đều 4 lần `moveBy` trong `performTouchInput`; test khẳng định trước `touchSlop + 4.dp < 24.dp` (Robolectric: 20 dp; máy 8 dp: 12 dp), rồi `calls.count { it == "hide" } == 0`. Lý do sửa: theo spec mới, dời 10 dp (dưới ngưỡng chạm) là một lần chạm và phải ẩn thẻ; ý của test (kéo ngắn không ẩn) giữ nguyên. Đo trên phương án ở mục 3.1: dời 10 dp thành `["hide"]`, kéo 16 dp lên thành rỗng; người rà soát đo kéo 20 dp không ẩn ở cả ngưỡng chạm 16 dp và 8 dp |
| KB-2, vuốt lên vẫn ẩn, một lần | Compose UI test, có sẵn | `CameraScreenPoseTest.swipeUpOnStepCard_hides`: `swipeUp()`, `calls.count { it == "hide" } == 1` (chạm không cộng thêm lần thứ hai). Code hiện tại và phương án: qua |
| KB-2, kéo trên thẻ không đổi nét viền | Compose UI test, có sẵn | `CameraScreenOverlayTest.swipeOnStepCard_doesNotMoveOutline` (giữ nguyên, `swipeLeft()` trên thẻ, `transforms` và `calls` rỗng). Code hiện tại và phương án: qua |
| KB-2, chạm thẻ không đổi nét viền | Compose UI test | `CameraScreenOverlayTest.tapOnStepCard_doesNotMoveOutline`: chạm chữ bước, `transforms` rỗng, `calls` không có `"end"` |
| KB-2, trình đọc màn hình | Compose UI test, sửa test cũ | `CameraScreenPoseTest.stepCard_hasHideAccessibilityAction` đổi thành `stepCard_hasHideClickLabel`: node `STEP_CARD` có `SemanticsActions.OnClick` với nhãn "Ẩn thẻ bước"; node không còn `SemanticsActions.CustomActions` (`config.getOrNull(...) == null`); gọi hành động thì `calls == ["hide"]`; `onNodeWithContentDescription("Bước sau")` vẫn tìm được (mũi tên là node riêng). Code hiện tại: nhãn `null`, chỉ có hành động riêng "Ẩn thẻ bước". Lý do sửa: hành động riêng bỏ theo mục 3.1, bước 2 |
| KB-2, nút "Bước" hiện lại thẻ | Compose UI test, có sẵn | `CameraScreenPoseTest.stepsHidden_showsStepsButton_andShowsCard` (giữ nguyên) |
| Thẻ lỗi nét viền | Compose UI test | `CameraScreenPoseTest.outlineFailed_tapCard_hides_retryOnlyRetries`: chạm "Thử lại" rồi chạm "Chưa tải được nét viền" (cây chưa gộp), `calls == ["retryOutline", "hide"]`. Code hiện tại: thất bại, `["retryOutline"]` |
| KB-2, bố cục mở gập | Compose UI test | `CameraScreenUnfoldedTest.tapStepText_hides`: chạm chữ bước, `calls == ["hide"]`. Code hiện tại: thất bại, `calls` rỗng |
| Giao diện không đổi | Screenshot test, có sẵn | 27 screenshot test của `:feature:camera` (sáu lớp `*ScreenshotTest`) qua với ảnh chuẩn hiện có (`verifyRoborazziDebug`); không ghi lại ảnh chuẩn nào |
| Đúng thiết kế (khi có `design/screens/`) | Screenshot test có sẵn | Ảnh thiết kế của ticket khác (T-013, T-014, T-015, T-016, T-017, T-020): `CameraScreen_adjust_dark`, `CameraScreen_countdown_dark`, `CameraScreen_default_dark`, `CameraScreen_default_dark_unfolded`, `CameraScreen_error_dark`, `CameraScreen_locked_dark`, `CameraScreen_matched_dark`, `CameraScreen_matching_dark`, `CameraScreen_no-person_dark`, `CameraScreen_no-pose_dark`, `CameraScreen_permission-denied_dark`, `CameraScreen_starting_dark`, `CameraScreen_steps-hidden_dark`, `ReviewScreen_default_dark`, `ReviewScreen_default_dark_unfolded`, `ReviewScreen_discard_dark`, `ReviewScreen_save-error_dark`, `ReviewScreen_saving_dark`. Plan không đổi hình nên không thêm test, không chạy lại so sánh thiết kế |
| Chạm và TalkBack trên máy thật | Kiểm tra thủ công (máy thật: TalkBack) | Trên bản staging: chạm chữ bước thì thẻ ẩn, nút "Bước" hiện; chạm mũi tên chỉ chuyển bước; kéo ngang trên thẻ không ẩn; chạm hai lần nhanh vào thẻ: xem lần chạm thứ hai có rơi vào nút "Bước" và hiện lại thẻ không, ghi kết quả; chạm hụt vào khoảng giữa hai mũi tên: thẻ ẩn (đúng spec), ghi lại nếu thấy dễ chạm nhầm; bật TalkBack, chạm một lần vào thẻ thì đọc số bước, chữ bước và "nhấn đúp để Ẩn thẻ bước", nhấn đúp thì thẻ ẩn; hai mũi tên vẫn là hai mục riêng |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-062). Không có tiêu chí chạy trên emulator mà JVM chưa kiểm, nên không có tệp kịch bản.

**Ngoại lệ viết test trước:** Không có. Hai test sửa (`smallDragUp_doesNotHide`, `stepCard_hasHideAccessibilityAction`) sửa trong commit test của bước 3, cùng các test mới; `stepCard_hasHideClickLabel` thất bại trên code hiện tại (nhãn `null`), `smallDragUp_doesNotHide` bản mới qua trên code hiện tại (đối chứng, chặn phương án làm hỏng nó).

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-26, nhánh `plan/T-059` (từ `docs/fb-camera-may-that` 68f5959, code bằng `main` 0939acf), test tạm `TrialStepCardTapTest` (không commit) với 14 trường hợp, rồi các lớp test camera có sẵn, rồi screenshot test.
- **Lệnh:** `./gradlew :feature:camera:testDebugUnitTest --tests '*TrialStepCardTapTest*' --tests '*CameraScreenPoseTest*' --tests '*CameraScreenOverlayTest*' --tests '*CameraScreenUnfoldedTest*' --tests '*CameraScreenMatchTest*'` và `./gradlew :feature:camera:verifyRoborazziDebug --tests '*ScreenshotTest*'`. Chưa chạy `scripts/check.sh` (E bằng 1: `clickable` và `pointerInput` đã dùng nhiều trong dự án; trợ năng đo trên chính node đích theo L-004).
- **Kết quả:**
  - `calls` trong lớp tạm chỉ ghi các hành động được đo (không có `onThumbShown`), nên các so sánh ở đây không bị `shown` chen vào.
  - Code hiện tại: chạm chữ, chạm số bước, chạm thẻ lỗi, chạm ở bố cục mở gập đều thất bại (`calls` rỗng); nhãn `OnClick` là `null`, node không gộp, chỉ có hành động riêng "Ẩn thẻ bước". Chạm mũi tên bật, chạm mũi tên mờ, `swipeUp`, `swipeLeft` qua.
  - Ngưỡng chạm trên Robolectric: `LocalViewConfiguration.current.touchSlop` = 48 px, 16 dp ở `xxhdpi`.
  - Phương án `clickable` (có `clip`) cùng bộ dò vuốt dọc: các chạm đều đúng, nhưng kéo ngang 40 dp ra `["hide"]`, và `smallDragUp_doesNotHide` cũ ra 1 lần `hide` (10 dp dưới ngưỡng 16 dp).
  - Phương án ở mục 3.1 (`detectDragGestures`, `clickable` không `clip`, `indication = null`): 14 trường hợp tạm đúng như bảng ở mục 4 (kéo ngang 40 dp rỗng, kéo lên 16 dp rỗng, kéo xuống 20 dp rỗng, vuốt lên một lần `hide`); nhãn `OnClick` là "Ẩn thẻ bước", node gộp chữ và số bước, mũi tên vẫn tìm được theo mô tả; 69 test có sẵn của bốn lớp qua trừ đúng hai test sửa ở mục 4.
  - Screenshot: có `.clip(RoundedCornerShape(Radius.card))` thì 13 trong 27 test lệch (ví dụ `CameraScreen_default_dark`: góc thẻ và nét viền); bỏ `clip` thì 27 qua. Code hiện tại: 27 qua.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một file code và ba file test trong `:feature:camera`, không đổi giao diện công khai |
| B. Độ rõ của yêu cầu | 0 | Spec KB-2 đã xác nhận ngày 2026-10-26, thiết kế ghi thao tác; ba câu hỏi ở mục 8 chỉ là chi tiết có đề xuất |
| C. Khả năng kiểm chứng tự động | 1 | Mọi tiêu chí có test JVM; TalkBack và ngưỡng chạm thật kiểm trên máy thật |
| D. Ảnh hưởng chéo | 2 | T-061 (cùng danh sách chạy dự kiến) cũng sửa `PoseOverlay.kt` (làm mờ mũi tên khi đếm ngược) |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ: `clickable` cùng bộ dò kéo trên một modifier, đã đo |
| **Tổng** | **5** | **Mức: Trung bình** |

Ticket ghi rủi ro sơ bộ thấp; chấm theo plan ra trung bình vì tiêu chí D.

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: không. `StepCard`, `StepCardArrows` của `:core:designsystem` dùng nguyên.
- Task khác trong sprint có thể xung đột:
  - T-061 (khóa nút khi đếm ngược) sửa `PoseOverlay.kt` và có thể `CameraScreenPoseTest.kt`. Theo spec KB-6 mới, khi đang đếm, chạm bất kỳ chỗ nào trong khung xem trước (trừ zoom) thì dừng đếm; thẻ bước nằm trong khung, nên plan T-061 phải quyết chạm thẻ lúc đang đếm là dừng đếm chứ không ẩn thẻ, và có test "chạm thẻ bước khi đang đếm thì dừng đếm, không ẩn thẻ": `clickable` của thẻ nằm trên lớp mờ nên sẽ nhận chạm trước lớp mờ nằm dưới nó.
  - T-060 (cắt nét viền theo khung) sửa khung xem trước ở `CameraScreen.kt`, không chạm `HideableStepCard`.
  - T-058 (camera mở từ thư viện) không chạm file của plan này.
- Đề xuất thứ tự chạy: trước T-061, để T-061 xây trên hành vi chạm thẻ đã có. Không phụ thuộc T-058, T-060.

## 7. Điều kiện dừng riêng

- Phải sửa `StepCard` hay `StepCardArrows` của `:core:designsystem` để mũi tên mờ không làm ẩn thẻ: dừng, vì file đó ngoài mục 3.2 và là thành phần dùng chung.
- Một screenshot test có sẵn lệch ảnh chuẩn: dừng, không ghi lại ảnh chuẩn (plan không cho ghi lại).

## 8. Câu hỏi cho người duyệt

1. **Có hiệu ứng gợn sóng khi chạm thẻ không?** Gợn sóng đúng góc bo cần `clip`, làm 13 ảnh chuẩn camera lệch và phải ghi lại; gợn sóng không cắt thì tràn ra ngoài góc bo. Thẻ biến mất ngay khi nhấc tay nên gợn sóng gần như không thấy.
   - Đề xuất của AI: không có gợn sóng (`indication = null`).
2. **Thẻ báo lỗi "Chưa tải được nét viền" có ẩn khi chạm không?** Thẻ này dùng cùng thành phần, đang ẩn được bằng vuốt lên.
   - Đề xuất của AI: có, giống thẻ bước; chạm nút "Thử lại" chỉ thử lại.
3. **TalkBack:** bỏ hành động riêng "Ẩn thẻ bước", thay bằng hành động nhấp có nhãn "Ẩn thẻ bước". Thẻ thành một mục đọc gộp số bước và chữ bước, nhấn đúp thì ẩn; hai mũi tên vẫn là hai mục riêng.
   - Đề xuất của AI: đồng ý thay; giữ cả hai thì menu hành động của TalkBack có hai mục làm cùng một việc.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-26, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Không có gợn sóng; thẻ lỗi cũng ẩn khi chạm; TalkBack dùng hành động nhấp có nhãn "Ẩn thẻ bước".

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261026-03, phiên bản plan 1

- **Kết quả:** đã merge (PR #172, commit merge b9708ed, 2026-10-26 13:36 UTC).
- **Nhánh, PR:** `claude/T-059-cham-the-buoc`, https://github.com/VGL-Studio/camera-pose/pull/172. Commit: `94a69fe` (test), `b31b3e7` (code), `018bf68` (bỏ import `semantics` thừa, ktlint).
- **Đã làm:** `HideableStepCard`: `detectDragGestures` (cộng `amount.y`) thay `detectVerticalDragGestures`; `clickable(interactionSource = null, indication = null, onClickLabel = hideLabel, onClick = { currentOnHide() })` thay hành động riêng; bỏ import thừa; KDoc. Test: 6 test mới và 2 test sửa ở `CameraScreenPoseTest`, 1 test ở `CameraScreenOverlayTest` (thêm `onHideSteps` vào `actions`), 1 test ở `CameraScreenUnfoldedTest` (thêm `onHideSteps`). Phiên con viết test thì dừng vì lỗi API 403 (mục Tiếp tục của báo cáo phiên); phiên chính chạy, commit phần test đó và làm tiếp bước 3 đến 5 trong worktree `../task-T-059`.
- **Test viết trước:** 10 test mới hoặc sửa. 7 thất bại đúng lý do: `tapStepText_hides` (điện thoại và mở gập), `tapStepBadge_hides`, `tapOnStepCard_doesNotMoveOutline`, `sidewaysDragOnStepCard_doesNotHide` (ở bước đối chứng chạm) đều `expected:<[hide]> but was:<[]>`; `outlineFailed_tapCard_hides_retryOnlyRetries` `[retryOutline]`; `stepCard_hasHideClickLabel` nhãn `null`. 3 qua ngay đúng như plan (đối chứng): `tapEnabledArrow_movesStep_doesNotHide`, `tapDisabledArrow_doesNothing`, `smallDragUp_doesNotHide`. Viết chặt lại (L-035): 2, thêm bước đối chứng "chạm cùng chỗ thì ẩn" vào `sidewaysDragOnStepCard_doesNotHide` và "chạm tới thẻ, chỉ có `hide`" vào `tapOnStepCard_doesNotMoveOutline`.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 5 ghi chú: (1) `onNodeWithContentDescription("Bước sau").assertExists()` chưa chứng minh mũi tên là node riêng (code đúng); (2) chưa có test chạm phần trống của thẻ (có ở kiểm tra thủ công); (3) TalkBack và chạm hai lần nhanh kiểm trên máy thật; (4) chạm thẻ khi đang đếm ngược giờ ẩn thẻ, T-061 xử lý; (5) mũi tên mờ không chuyển chạm xuống thẻ dựa vào `clickable(enabled = false)` của Compose, có test chặn.
- **Kết quả CI:** `pr-check` lần chạy 40330604227 xanh (`build-and-test`, `ui-test` 13:26 đến 13:35), một vòng. Cục bộ `scripts/check.sh` lần 1 đỏ ở `:feature:camera:ktlintMainSourceSetCheck` (import `semantics` thừa sau khi bỏ hành động riêng), sửa một lần; lần 2 qua: 1146 test JVM, 0 lỗi, 97 ảnh không đổi.
- **Lệch so với plan:** Không có. Phương án dự phòng 3.4 không dùng.
- **Việc còn lại:** Không có.
- **Cần người xem:** kiểm tra thủ công trên máy thật (plan mục 4, dòng cuối): chạm chữ bước ẩn thẻ; chạm mũi tên chỉ chuyển bước; kéo ngang không ẩn; chạm hai lần nhanh (lần hai có rơi vào nút "Bước" không); chạm hụt giữa hai mũi tên; TalkBack đọc "nhấn đúp để Ẩn thẻ bước".
