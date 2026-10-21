---
ticket: T-006
title: Test cuộn tới nút "Thử lại" trên màn hình rất thấp
status: approved
version: 1
risk_score: 1
risk_level: low
blocking_points: []
run_order:
reviewer: Nhật
cross_reviewer: Nhật (vì là app mẫu)
approved_at: 2026-10-18
---

# PLAN T-006: TEST CUỘN TỚI NÚT "THỬ LẠI" TRÊN MÀN HÌNH RẤT THẤP

## 1. Tóm tắt

Thêm hai test giao diện ở khung `w320dp-h480dp-xxhdpi`, nơi màn hình lỗi dài hơn màn hình: trước khi cuộn, nút "Thử lại" ở ngoài màn hình; cuộn tới nút thì thấy trọn và bấm được; cuộn tới cuối thì chân trang nằm dưới nút. Sửa chú thích lớp `LoadingScreenScreenshotTest` cho đúng với hai ảnh `_small`. Không đổi code chạy: số đo trên code hiện tại (mục 4) cho thấy bố cục của T-005 đã đúng, nên đây là test bổ sung, không viết trước được theo nghĩa "thất bại trước khi có code".

## 2. Hiểu yêu cầu

Ticket xử lý điểm "nên sửa" của rà soát độc lập trong RUN-20261017-03 (báo cáo `docs/runs/RUN-20261017-03.md`, mục Cần người kiểm tra), cho các kịch bản sau trong `features/loading/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-2 | Khi lỗi thì có nút "Thử lại" | Trên màn hình thấp hơn nội dung, người dùng cuộn được tới nút, thấy trọn nút và bấm được |
| KB-3 | Báo lỗi như KB-2 | Như trên |

Thiết kế tham chiếu: `features/loading/design/README.md`, mục 6 (màn hình thấp hơn 800 dp: nội dung và chân trang cuộn chung khi dài hơn màn hình, T-005) và mục 7 (dùng được ở màn hình thấp).

ADR liên quan: ADR-0020. Bài học: L-028, L-029.

## 3. Giải pháp

### 3.1. Cách làm

**Test (`LoadingScreenTest.kt`).** Thêm hằng số `VERY_SHORT_SCREEN = "w320dp-h480dp-xxhdpi"` cạnh `SHORT_SCREEN`, chú thích: khung có nội dung dài hơn màn hình, để chạy qua đường cuộn (T-006). Hai test mới, mỗi test có `@Config(qualifiers = VERY_SHORT_SCREEN)`, trạng thái `LoadingUiState.Failed(0.68f)`, `appName = APP_NAME`:

1. `failed_onVeryShortScreen_retryButtonReachableByScrolling`:
   - `onNodeWithTag(RETRY).assertIsNotDisplayed()` trước khi cuộn: chứng minh khung này thật sự cần cuộn. Không có điều kiện này thì test có thể qua mà không chạy qua đường cuộn.
   - `performScrollTo()` trên `RETRY`, rồi `assertIsDisplayed()`.
   - Đáy của `RETRY` theo `getUnclippedBoundsInRoot()` không lớn hơn đáy của `SCREEN`: nút thấy trọn, không bị cắt.
   - `performClick()` trên `RETRY`, `onRetry` được gọi đúng một lần.
2. `failed_onVeryShortScreen_footerBelowRetryAfterScrolling`:
   - `performScrollTo()` trên dòng chân trang (tìm theo chữ `R.string.loading_footer`), rồi `assertIsDisplayed()`.
   - Đỉnh của chân trang không nhỏ hơn đáy của `RETRY`, cả hai theo `getUnclippedBoundsInRoot()`.

Dùng `getUnclippedBoundsInRoot()`, không dùng `getBoundsInRoot()`: phần tử ở ngoài màn hình có `getBoundsInRoot()` bị cắt về 0 (đã đo, mục 4), nên so sánh sẽ sai.

**Chú thích (`LoadingScreenScreenshotTest.kt`).** Câu "Each test has the name of a design image ..." thành: các test ở khung chuẩn có tên trùng ảnh thiết kế; các test `_small` ở khung thấp (L-028) không có ảnh thiết kế nên không được so với thiết kế (ADR-0020). Chỉ sửa chú thích, không đổi test.

**Nơi khác đọc các file trên:** không có. Hai file test không được script hay workflow nào đọc theo tên.

**Commit:** một commit test gồm cả hai file (không có commit code, vì không đổi code chạy).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenTest.kt` | Hằng số `VERY_SHORT_SCREEN`, hai test ở mục 3.1 |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenScreenshotTest.kt` | Chỉ chú thích lớp |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không chạm.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.

## 4. Kiểm thử

**Số đo trên code hiện tại** (L-029; đo ngày 2026-10-18 trên commit a43822c bằng một test tạm, không commit; `Failed(0.68f)`, "Todo App", dp theo `getUnclippedBoundsInRoot()`):

| Khung | Lúc | Đáy màn hình | Nút "Thử lại" | Chân trang |
| --- | --- | --- | --- | --- |
| `w320dp-h480dp` | Trước khi cuộn | 480 | 482 tới 534 (ngoài màn hình; `getBoundsInRoot()` trả 0 tới 0) | 558 tới 574 |
| `w320dp-h480dp` | Sau khi cuộn tới nút | 480 | 428 tới 480 | 504 tới 520 |
| `w320dp-h480dp` | Sau khi cuộn tới chân trang | 480 | 388 tới 440 | 464 tới 480 |
| `w320dp-h640dp` | Không cần cuộn | 640 | 495 tới 547 | 592 tới 608 |
| `w360dp-h640dp` | Không cần cuộn | 640 | 475 tới 527 | 592 tới 608 |

Hàng `w320dp-h640dp` cho thấy lỗi gốc của FB-20261017-01 (chiều rộng 320, emulator 320 x 640) đã hết trên JVM sau T-005.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-2: cuộn được tới nút "Thử lại", thấy trọn và bấm được | Compose UI test (Robolectric), khung `w320dp-h480dp-xxhdpi` | `LoadingScreenTest.failed_onVeryShortScreen_retryButtonReachableByScrolling`. Theo số đo: trước khi cuộn nút không hiện; sau khi cuộn đáy nút 480 ≤ 480; bấm gọi `onRetry` một lần. Qua ngay trên code hiện tại |
| Chân trang không đè nút khi cuộn tới cuối | Compose UI test, khung `w320dp-h480dp-xxhdpi` | `LoadingScreenTest.failed_onVeryShortScreen_footerBelowRetryAfterScrolling`. Theo số đo: đỉnh chân trang 464 ≥ đáy nút 440. Qua ngay trên code hiện tại |
| Không đổi gì khác | Screenshot test, test hiện có | Sáu ảnh chuẩn hiện có so khớp, không ghi lại; test hiện có của `LoadingScreenTest` qua, không sửa |

**Ngoại lệ viết test trước:** cả hai test qua ngay khi viết, vì hành vi đã có từ T-005 (số đo ở trên). Đây là test bổ sung cho đường cuộn chưa được test chạy qua, không phải test cho code mới. Để chắc test không qua vô điều kiện, phiên tự chạy thử một lần (không commit) với `LoadingScreen.kt` tạm bỏ `verticalScroll`, ghi kết quả vào nhật ký: test 1 phải thất bại ở `performScrollTo` hoặc ở điều kiện đáy nút, rồi hoàn tác. Người duyệt đồng ý ngoại lệ này ở câu hỏi 1.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 0 | Hai file test trong một module, không đổi code chạy |
| B. Độ rõ của yêu cầu | 0 | Hành vi và số đo đã rõ |
| C. Khả năng kiểm chứng tự động | 0 | Chính là test tự động |
| D. Ảnh hưởng chéo | 0 | Chỉ trong `:feature:loading` |
| E. Độ mới của kỹ thuật | 1 | `performScrollTo` và `getUnclippedBoundsInRoot` chưa dùng trong dự án, nhưng là API chuẩn của Compose test |
| **Tổng** | 1 | **Mức: Thấp** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: Không có.
- Task khác trong sprint có thể xung đột: Không có.
- Đề xuất thứ tự chạy: bất kỳ.

## 7. Điều kiện dừng riêng

1. Một trong hai test thất bại trên code hiện tại: số đo ở mục 4 không còn đúng. Dừng, ghi số đo mới, không sửa `LoadingScreen.kt` (ngoài danh sách file).
2. Lần thử bỏ `verticalScroll` mà test 1 vẫn qua: test không kiểm tra được đường cuộn. Dừng, ghi lại.

## 8. Câu hỏi cho người duyệt

1. **Ngoại lệ viết test trước:** test qua ngay vì hành vi đã có; thay bằng một lần thử tạm bỏ cuộn để chứng minh test bắt được lỗi. Đề xuất: đồng ý.
- Trả lời: Đồng ý.
2. **Có cần ảnh chuẩn ở khung `w320dp-h480dp` không:** AI đề xuất không. Ảnh chỉ chụp phần đầu màn hình (trước khi cuộn), không cho thấy thêm điều gì so với hai test; quy tắc L-028 chỉ yêu cầu một ảnh khung thấp mỗi màn hình, đã có.
- Trả lời: Không.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (vì là app mẫu)
- **Ghi chú:** 

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
