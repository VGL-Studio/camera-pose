---
ticket: T-027
title: Screenshot test của trạng thái có vòng chờ chụp ở đồng hồ dừng, không còn chập chờn
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 4                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: low              # low | medium | high
token_estimate: 10.0M        # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-027: SCREENSHOT TEST CỦA TRẠNG THÁI CÓ VÒNG CHỜ CHỤP Ở ĐỒNG HỒ DỪNG, KHÔNG CÒN CHẬP CHỜN

## 1. Tóm tắt

Hai screenshot test chụp trạng thái có vòng chờ chạy mãi (`CameraScreen_starting_dark`, `ReviewScreen_saving_dark`) chuyển từ `captureRoboImage(path) { ... }` sang `runComposeUiTest` với đồng hồ dừng, chụp ở mốc 400 ms của hiệu ứng. Ảnh chuẩn của hai test được ghi lại một lần. Kiểm chứng: chạy lớp test nhiều lần với cỡ bộ nhớ khác nhau, ảnh không lệch điểm nào, không còn lỗi hết bộ nhớ, mỗi test dưới một giây.

## 2. Hiểu yêu cầu

Ticket không xử lý kịch bản nào của `features/camera/spec.md`: giao diện và hành vi của app giữ nguyên. Yêu cầu đến từ L-029 và L-033: một test chập chờn trên `main-guard` mở `main-broken` và kết thúc cả phiên.

| Yêu cầu | Tiêu chí | Cách hiểu của AI |
| --- | --- | --- |
| L-029 | Khi chạy `starting_dark` nhiều lần, với cỡ bộ nhớ khác nhau, thì ảnh luôn khớp ảnh chuẩn | Đồng hồ test dừng, chụp ở một mốc cố định; ảnh chỉ còn phụ thuộc code, không phụ thuộc máy |
| L-029 | Khi chạy test thì không còn `OutOfMemoryError` trong log của test | Màn hình không phải chờ rảnh nữa, nên Robolectric không vẽ khung hình mãi |
| L-029 | Cùng cách cho mọi screenshot test của trạng thái có hiệu ứng chạy mãi trong `:feature:camera` | Hiện chỉ có hai: `starting_dark` (`CameraControls.kt`, vòng chờ của nút chụp) và `saving_dark` của T-016 (`PrimaryButton` với `loading = true`) |

Thiết kế tham chiếu: `features/camera/design/README.md`, ảnh `design/screens/CameraScreen_starting_dark.png`, `design/screens/ReviewScreen_saving_dark.png`. Vòng chờ trên thiết kế là một cung tĩnh; ảnh mới có cung thấy rõ, gần thiết kế hơn ảnh hiện tại.

ADR liên quan: ADR-0010 (cổng chất lượng, `main-guard`), ADR-0020 (screenshot test cùng tên ảnh thiết kế).

## 3. Giải pháp

### 3.1. Cách làm

Nguyên nhân, đo ngày 2026-10-22 (mục 4.1): `captureRoboImage(path) { content }` của Roborazzi 1.43.0 mở `RoborazziActivity`, đặt nội dung, rồi chụp qua một thao tác Espresso. Espresso chờ luồng chính rảnh, không có giới hạn thời gian. Vòng chờ chạy mãi nên luồng chính không bao giờ rảnh: Robolectric vẽ khung hình tới khi máy test hết bộ nhớ (`OutOfMemoryError` trong `ShadowTrace`), luồng vẽ của Compose chết, luồng chính rảnh và ảnh được chụp. Vị trí vòng chờ lúc đó phụ thuộc cỡ bộ nhớ và lúc dọn bộ nhớ, nên ảnh lệch giữa các máy. Ngưỡng so ảnh mặc định là 0, nên lệch một điểm là thất bại.

Cách sửa, chỉ trong code test:

```kotlin
// The spinner animates forever, so the screen never becomes idle (L-029): drive the test clock by hand.
@OptIn(ExperimentalTestApi::class)
@Test
fun starting_dark() = runComposeUiTest {
    mainClock.autoAdvance = false
    setContent { Screen(CameraUiState.Starting) }
    // A fixed point of the animation, so the arc is visible and the image is the same on every run.
    mainClock.advanceTimeBy(SPINNER_FRAME_MILLIS)
    onRoot().captureRoboImage("src/test/screenshots/CameraScreen_starting_dark.png")
}
```

- `SPINNER_FRAME_MILLIS = 400L` trong `companion object` của từng lớp test. Ở mốc 0 vòng chờ chỉ là một chấm; ở 400 ms là một cung thấy rõ (mục 4.1).
- `ReviewScreenScreenshotTest.saving_dark` làm đúng như vậy, với nội dung mà T-016 đã viết cho test này. Các test khác của hai lớp giữ `captureRoboImage(path) { ... }`.
- Không dùng `createComposeRule()` cho cả lớp: rule mở thêm một cửa sổ, Roborazzi thấy nhiều cửa sổ thì chụp cả màn hình, và 6 ảnh còn lại của `CameraScreenScreenshotTest` lệch khoảng 99,99% (mục 4.1, phương án a).
- Đường dẫn ảnh vẫn ghi rõ trong mỗi lệnh `captureRoboImage`, đúng quy tắc của `scripts/check.sh`.
- `runComposeUiTest`, `onRoot`, `ExperimentalTestApi` nằm trong thư viện ui-test của Compose, mà `androidx-compose-ui-test-junit4` kéo theo; thư viện này đã có trên classpath test của module (`CameraScreenTest` dùng `createComposeRule`); hàm mở rộng `SemanticsNodeInteraction.captureRoboImage` nằm trong `roborazzi-compose`, đã dùng sẵn. Chạy thử đã biên dịch và chạy được mà không thêm phụ thuộc.

Thứ tự làm: bước 3 commit phần test (hai lớp test sửa, ảnh chuẩn cũ giữ nguyên). Với ảnh cũ, test mới thất bại vì ảnh khác (đo được 2950 điểm, 0,09%, ở `starting_dark`); đây là lý do thất bại đúng. Bước 4 ghi lại đúng hai ảnh chuẩn bằng `./gradlew :feature:camera:recordRoborazziDebug`, chỉ commit hai ảnh trong mục 3.2 (các ảnh khác của module không được đổi; đổi thì dừng, mục 7), rồi chạy `scripts/design-compare.sh camera`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenScreenshotTest.kt` | `starting_dark` chụp bằng `runComposeUiTest`, đồng hồ dừng, mốc `SPINNER_FRAME_MILLIS`; chú thích dẫn L-029 |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenScreenshotTest.kt` | `saving_dark` cùng cách |
| Ghi lại | `feature/camera/src/test/screenshots/CameraScreen_starting_dark.png` | Vòng chờ ở mốc 400 ms |
| Ghi lại | `feature/camera/src/test/screenshots/ReviewScreen_saving_dark.png` | Vòng chờ trên nút ở mốc 400 ms |
| Sửa | `docs/features/camera/design/review/summary.md` | Tỷ lệ khác biệt mới của hai ảnh, do `scripts/design-compare.sh camera` sinh |
| Ghi lại | `docs/features/camera/design/review/CameraScreen_starting_dark_compare.png` | Do `scripts/design-compare.sh camera` sinh |
| Tạo | `docs/features/camera/design/review/ReviewScreen_saving_dark_compare.png` | Do `scripts/design-compare.sh camera` sinh; T-016 đã sinh thì ghi đè |
| Sửa | `docs/features/camera/plans/T-027-test-vong-cho-on-dinh.md` | Nhật ký thực hiện |
| Sửa | `docs/features/camera/tickets/T-027-test-vong-cho-on-dinh.md` | Trạng thái |
| Sửa | `docs/STATUS.md` | Sinh lại bằng `scripts/status.py` |

Nơi khác đọc hai ảnh chuẩn: `scripts/design-compare.sh` (đã có trong bảng) và `verifyRoborazziDebug` trong `scripts/check.sh` (đọc, không sửa). `scripts/release.sh` và các workflow không đọc ảnh chuẩn. Nơi đọc `summary.md` và `STATUS.md` không bị ảnh hưởng: `scripts/design-compare/DesignCompare.java` sinh `summary.md`; `scripts/device-session.sh` và `.github/workflows/device-session.yml` chỉ dẫn đường tới nó; `scripts/tests/run.sh` dựng bản riêng của hai file trong thư mục tạm của test. Plan chỉ đổi số liệu trong hai file, không đổi cấu trúc.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Dùng `androidx-compose-ui-test-junit4` và `roborazzi-compose` trong catalog, đã có trên classpath test của `:feature:camera`; không sửa `build.gradle.kts` hay `gradle/libs.versions.toml`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `ReviewScreenScreenshotTest` chưa có trên nhánh chính lúc viết plan (T-016 tạo); nút "Đang lưu…" có thể vẽ khác ở mốc 400 ms (ví dụ cung quá ngắn).
- **Điều kiện chuyển:** ảnh `ReviewScreen_saving_dark` ghi ở mốc 400 ms không thấy cung của vòng chờ khi xem ảnh.
- **Cách làm thay:** dùng mốc 600 ms riêng cho lớp này (`SPINNER_FRAME_MILLIS = 600L`), ghi vào nhật ký. Tiêu chí ở mục 4 giữ nguyên.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| L-029, ảnh ổn định | Screenshot test | `CameraScreenScreenshotTest.starting_dark` và `ReviewScreenScreenshotTest.saving_dark` qua `verifyRoborazziDebug` trong `scripts/check.sh`. Trước khi ghi lại ảnh (bước 3) cả hai thất bại vì ảnh khác ảnh chuẩn cũ |
| L-029, ổn định với cỡ bộ nhớ khác nhau | Lệnh kiểm tra, ghi kết quả vào nhật ký | Sau bước 4, chạy `./gradlew :feature:camera:testDebugUnitTest --rerun -Proborazzi.test.verify=true` ba lần: bộ nhớ test mặc định, 1g và 384m. Cỡ bộ nhớ đặt bằng một init script tạm ngoài repo (`--init-script`, `tasks.withType(Test).configureEach { maxHeapSize = ... }`), không sửa file build. Mỗi lần: mọi test qua, `pixelDifferences=0` |
| L-029, không còn hết bộ nhớ, không chờ mãi | Lệnh kiểm tra, ghi kết quả vào nhật ký | Trong ba lần trên: `grep -c OutOfMemoryError feature/camera/build/test-results/testDebugUnitTest/TEST-*ScreenshotTest.xml` bằng 0; thời gian của `starting_dark` và `saving_dark` trong file XML dưới 5 giây (đo được 0,23 đến 0,25 giây) |
| Các ảnh khác của module không đổi | Screenshot test | Mọi ảnh chuẩn khác trong `feature/camera/src/test/screenshots/` qua `verifyRoborazziDebug` mà không ghi lại; `git status` sau bước ghi ảnh chỉ có hai ảnh trong mục 3.2 |
| Gần thiết kế | Screenshot test, so ảnh thiết kế | `scripts/design-compare.sh camera`: tỷ lệ khác biệt của hai ảnh ghi vào `summary.md`; người xem ảnh `_compare.png` |
| Toàn bộ | Lệnh của phiên | `scripts/check.sh` qua |

Ảnh thiết kế của ticket khác (T-013, T-014, T-015, T-016, T-017, T-020): `CameraScreen_adjust_dark`, `CameraScreen_countdown_dark`, `CameraScreen_default_dark`, `CameraScreen_default_dark_unfolded`, `CameraScreen_error_dark`, `CameraScreen_locked_dark`, `CameraScreen_matched_dark`, `CameraScreen_matching_dark`, `CameraScreen_no-person_dark`, `CameraScreen_no-pose_dark`, `CameraScreen_permission-denied_dark`, `CameraScreen_steps-hidden_dark`, `ReviewScreen_default_dark`, `ReviewScreen_default_dark_unfolded`, `ReviewScreen_discard_dark`, `ReviewScreen_save-error_dark`

**Ngoại lệ viết test trước:** Ticket sửa chính test, không có code app để viết test trước. Bước 3 commit hai test đã sửa với ảnh chuẩn cũ (thất bại vì ảnh khác, là lý do đúng); bước 4 chỉ ghi lại hai ảnh chuẩn.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-22, trên nhánh chính tại 3890834 (đã duyệt RUN-20261023-02, trước khi T-012 merge), bằng một agent con trong worktree riêng; mọi thay đổi đã hoàn tác. Máy có Temurin JDK 21.0.13 và Android SDK 36 cài tạm. Chỉ thử `CameraScreenScreenshotTest` (`ReviewScreenScreenshotTest` chưa có).
- **Lệnh:** `./gradlew :feature:camera:testDebugUnitTest --rerun -Proborazzi.test.verify=true` (cùng chế độ với `verifyRoborazziDebug` của `scripts/check.sh`), `./gradlew :feature:camera:recordRoborazziDebug`, `./gradlew :feature:camera:ktlintCheck`. Chưa chạy cả `scripts/check.sh` trên máy này; phiên chạy ở bước 5.
- **Kết quả trên code hiện tại** (`captureRoboImage(path) { ... }`), cả lớp mỗi lần:

| Số lần | Bộ nhớ test | Thời gian `starting_dark` | Kết quả |
| --- | --- | --- | --- |
| 5 | mặc định (512m) | 253,4 s; 257,9 s; 272,2 s; 257,7 s; 261,4 s | 5 qua, `pixelDifferences=0` |
| 1 | 1g | 577,4 s | Thất bại, 3956 / 3295809 điểm |
| 1 | 384m | 165,9 s | Thất bại, 2573 / 3295809 điểm |

  Mọi lần, qua hay thất bại, đều có `java.lang.OutOfMemoryError: Java heap space` trên luồng chính của Robolectric. Lỗi tái hiện đúng lỗi trên `main-guard` (lần chạy 39817360705): `AssertionFailedWithCauseError: Roborazzi: .../CameraScreen_starting_dark.png is changed.` tại dòng `captureRoboImage`. Các test khác của lớp chạy 0,2 đến 7 giây.
- **Kết quả với cách sửa ở mục 3.1:**
  - Mốc 0 ms: khác ảnh chuẩn cũ 1548 điểm, vòng chờ chỉ là một chấm. Mốc 400 ms: khác 2950 điểm (0,09%), cung thấy rõ.
  - Ghi ảnh ở mốc 400 ms, rồi so 7 lần (5 lần bộ nhớ mặc định, 1 lần 1g, 1 lần 384m): 7 trên 7 qua, `pixelDifferences=0`, không có `OutOfMemoryError`. `starting_dark` chạy 0,23 đến 0,25 giây; cả lệnh Gradle 9 đến 11 giây.
  - 6 test còn lại giữ `pixelDifferences=0` với ảnh chuẩn hiện có. Khi ghi lại cả lớp, chỉ `CameraScreen_starting_dark.png` đổi.
  - `ktlintCheck` qua; module có 7 lệnh `captureRoboImage(` và 7 đường dẫn rõ, đúng quy tắc của `scripts/check.sh`.
- **Phương án đã thử và bỏ:** (a) `createComposeRule()` cho cả lớp với `autoAdvance = false`: 7 trên 7 thất bại vì thêm cửa sổ (mục 3.1). (b) `RoborazziComposeOptions` không có cách điều khiển đồng hồ. (c) `inspectionMode` không dừng vòng chờ của Material 3. Các cách đổi code app (vòng chờ tĩnh khi test) không thử, vì ticket không đổi code app.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có. Không sửa `scripts/check.sh`, CI hay cấu hình build.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Hai file test và hai ảnh chuẩn trong một module feature |
| B. Độ rõ của yêu cầu | 0 | Nguyên nhân đã đo, tiêu chí là số đo |
| C. Khả năng kiểm chứng tự động | 0 | Mọi tiêu chí kiểm bằng lệnh và test |
| D. Ảnh hưởng chéo | 1 | Sửa file của T-014, T-016 nhưng chạy sau khi hai task đó merge; không task nào khác trong danh sách sửa các file này |
| E. Độ mới của kỹ thuật | 2 | `runComposeUiTest` và đồng hồ dừng lần đầu dùng trong dự án; phổ biến, đã chạy thử |
| **Tổng** | 4 | **Mức: Thấp** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: Không có.
- Task khác có thể xung đột: T-014 ghi lại `CameraScreen_starting_dark.png` (thêm hàng chip, dải tư thế) và sửa `CameraScreenScreenshotTest.kt`; T-016 tạo `ReviewScreenScreenshotTest.kt` và `ReviewScreen_saving_dark.png`. Cả hai đang trong RUN-20261023-02. Plan này mô tả hai file theo trạng thái sau khi hai task đó merge.
- Cho tới khi T-027 merge, hai ảnh vẫn có thể làm `main-guard` đỏ ở bất kỳ lần merge nào (khoảng 4 phút mỗi test).
- Đề xuất thứ tự chạy: đầu phiên sau, trước mọi task khác, vì mỗi lần merge của phiên đều chạy hai test này trên `main-guard`.

## 7. Điều kiện dừng riêng

- T-014 hoặc T-016 chưa merge khi bắt đầu task: dừng; plan cần phiên bản mới bỏ phần của task đó.
- Test của T-016 cho trạng thái đang lưu đã dùng đồng hồ dừng: bỏ phần đó, ghi vào nhật ký, không phải dừng.
- Ghi lại ảnh làm đổi ảnh chuẩn nào ngoài hai ảnh trong mục 3.2: dừng, không commit ảnh đó.
- Sau bước 4, một trong ba lần chạy với cỡ bộ nhớ khác nhau còn lệch ảnh hay còn `OutOfMemoryError`: dừng.

## 8. Câu hỏi cho người duyệt

1. **Mốc chụp 400 ms.** Ở mốc 0 vòng chờ chỉ là một chấm, khác thiết kế (cung tĩnh). Đề xuất của AI: 400 ms, cung thấy rõ; mốc nào cũng ổn định như nhau.
- Trả lời:
2. **Có cần quét cả app tìm trạng thái có hiệu ứng chạy mãi không?** Lúc viết plan, ngoài camera chỉ `:feature:sample` có vòng chờ ở trạng thái đang tải, và không có screenshot test cho trạng thái đó; `PrimaryButton` với `loading = true` chỉ được chụp ở T-016. Plan T-021 có ảnh `PosesScreen_loading_*` với khung xương; plan không nói khung xương có hiệu ứng. Đề xuất của AI: không mở rộng ticket; quy tắc mới ở `CLAUDE.md` bước 10 (PR #39) và rà soát trước phiên (`docs/ops/pre-run-review.md`, mục 2.2) bắt các trường hợp sau này.
- Trả lời:

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
