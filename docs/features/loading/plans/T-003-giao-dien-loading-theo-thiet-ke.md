---
ticket: T-003
title: Màn hình loading theo thiết kế Figma
status: done
version: 1
risk_score: 5
risk_level: medium
blocking_points: []
run_order: 2
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-10-16
---

# PLAN T-003: MÀN HÌNH LOADING THEO THIẾT KẾ FIGMA

## 1. Tóm tắt

Vẽ lại `LoadingScreen` theo gói thiết kế `features/loading/design/`, dùng thành phần và token của T-002. Luồng xử lý của T-001 giữ nguyên, chỉ đổi giao diện và chữ. Kiểm chứng bằng test giao diện theo bảng chữ của thiết kế, screenshot test cùng tên ba ảnh thiết kế, ảnh so sánh của `scripts/design-compare.sh loading`, và rà soát độc lập theo mục 7 của mẫu thiết kế.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/loading/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Màn hình loading có logo, tên app, khẩu hiệu, nhãn "Đang tải dữ liệu", thanh tiến trình, phần trăm, dòng hướng dẫn, chân trang, đúng thiết kế | Trạng thái `InProgress` vẽ đủ các phần theo `design/README.md`, mục 2 và 3 |
| KB-2 | Khi lỗi: tiêu đề "Không thể tải dữ liệu", lời hướng dẫn, nhãn "Tiến độ đã dừng", thanh tiến trình và phần trăm màu báo lỗi giữ ở mức đã đạt, nút "Thử lại" | Trạng thái `Failed` thay dòng hướng dẫn bằng khối lỗi, đổi màu nhãn, phần trăm, thanh sang vai trò `error`; logo không đổi vị trí |
| KB-3 | Báo lỗi như KB-2 | Cùng giao diện `Failed` |

Các tiêu chí còn lại của KB-1, KB-2, KB-3, KB-4 (thời gian, thử lại, sự kiện, cờ) đã làm ở T-001 và không đổi; test hiện có của T-001 phải vẫn qua.

Thiết kế tham chiếu: `features/loading/design/README.md`; ảnh `design/screens/LoadingScreen_progress_light.png`, `LoadingScreen_progress_dark.png`, `LoadingScreen_failed_light.png`, `LoadingScreen_failed_dark.png`; số liệu trong `design/context/` cùng tên; biểu tượng trong `design/handoff/`.

ADR liên quan: ADR-0008, ADR-0020.

## 3. Giải pháp

### 3.1. Cách làm

**Tên app.** `LoadingScreen` nhận thêm tham số `appName: String` (đặt sau `uiState`). `LoadingRoute` lấy tên từ app đang chạy: `LocalContext.current.applicationInfo.loadLabel(context.packageManager).toString()`, tức `app_name` của `:app`. Feature không phụ thuộc `:app` và không viết cứng tên. Test truyền chữ mẫu "Todo App" như thiết kế.

**Bố cục** (`design/README.md`, mục 3; số liệu trong `context/`):

- `Surface` toàn màn hình (`testTag` `SCREEN` như cũ), trong đó `BoxWithConstraints`.
- Cột nội dung căn giữa ngang, lề trái phải `Spacing.xl + Spacing.md` (thiết kế 46 và 47), bắt đầu từ `maxHeight * 0.25f` tính từ đỉnh (thiết kế: 220 trên 891). Vị trí này không phụ thuộc trạng thái.
- Trong cột, theo thứ tự:
  1. `BrandLogo()`;
  2. `Spacing.lg`, tên app (`headlineLarge`, `onSurface`, căn giữa, `testTag` `APP_NAME`);
  3. `Spacing.sm`, khẩu hiệu (`stringResource(designsystem R.string.brand_tagline)`, `bodyMedium`, `onSurfaceVariant`);
  4. `Spacing.xl + Spacing.sm`, hàng nhãn và phần trăm (`Arrangement.SpaceBetween`, căn giữa dọc);
  5. `Spacing.md`, `ProgressBar` (`testTag` `PROGRESS`);
  6. phần dưới thanh tùy trạng thái.
- **Đang tải:** nhãn `loading_status` (`labelLarge`, `onSurfaceVariant`); phần trăm (`bodyMedium`, `primary`, `testTag` `PERCENT`); `ProgressBar` màu mặc định; `Spacing.lg`, dòng `loading_guidance` (`bodySmall`, `onSurfaceVariant`, căn giữa).
- **Lỗi:** nhãn `loading_status_failed` và phần trăm đều màu `error`; `ProgressBar(color = error, trackColor = errorContainer)`; `Spacing.lg + Spacing.xs`, rồi khối lỗi (`Column`, căn giữa, `testTag` `ERROR`, các phần cách nhau `Spacing.sm + Spacing.xs`):
  - hàng tiêu đề: `Icon(ic_loading_alert)` 18 dp màu `error`, `Spacing.sm`, `loading_error_title` (`titleMedium`, `onSurface`);
  - `loading_error` (`bodyMedium`, `onSurfaceVariant`, căn giữa);
  - `PrimaryButton(text = loading_retry, icon = painterResource(ic_loading_retry), modifier = fillMaxWidth().testTag(RETRY))`.
- **Chân trang:** `Row` căn giữa đáy (`Alignment.BottomCenter`), `Icon(ic_loading_lock)` 12 dp màu `onSurfaceVariant`, `Spacing.sm`, `loading_footer` (`labelSmall`, `onSurfaceVariant`). Cách đáy `Spacing.xl` (thiết kế 32).
- Phần trăm giữ cách tính hiện có (`(progress * PERCENT).roundToInt()`, chuỗi `loading_percent`).
- Không viết cứng mã màu, cỡ chữ, khoảng cách. Kích thước biểu tượng (18, 12) là hằng số `private` có tên.

**Biểu tượng.** Ba vector drawable chuyển từ `design/handoff/`: `ic_alert.svg` thành `ic_loading_alert.xml`, `ic_retry.svg` thành `ic_loading_retry.xml`, `ic_lock.svg` thành `ic_loading_lock.xml`. Mỗi file: `width`, `height`, `viewport` theo thuộc tính `width`, `height` của SVG; mỗi `path` chép nguyên `d`; `strokeColor="#FFFFFFFF"`, `strokeWidth` theo SVG, `strokeLineCap="round"`; bỏ `clipPath` của `ic_lock.svg` vì nó trùng khung. Màu thật đặt bằng `tint`.

**Chữ** (`strings.xml` của feature; bảng chữ ở `design/README.md`, mục 2):

| Khóa | Chữ |
| --- | --- |
| `loading_status` (thay `loading_message`) | Đang tải dữ liệu |
| `loading_guidance` (mới) | Đang chuẩn bị không gian của bạn… |
| `loading_status_failed` (mới) | Tiến độ đã dừng |
| `loading_error_title` (mới) | Không thể tải dữ liệu |
| `loading_error` (sửa) | Kết nối đang gián đoạn. Vui lòng kiểm tra mạng và thử lại để tiếp tục. |
| `loading_retry` (giữ) | Thử lại |
| `loading_footer` (mới) | Không gian riêng cho việc của bạn |
| `loading_percent` (giữ) | %1$d%% |

`loading_guidance` dùng ký tự `…` (U+2026). Xóa `loading_message` vì không còn dùng.

**Đối chiếu.** Sau khi screenshot test qua: chạy `scripts/design-compare.sh loading`, commit `docs/features/loading/design/review/`, dán `review/summary.md` vào mô tả PR và vào `design/README.md`, mục 8. Rà soát độc lập xem bốn ảnh `*_compare.png` theo mục 7 của `design/README.md` và ghi nhận xét vào mục 8. Tỷ lệ điểm ảnh khác chỉ để tham khảo: ảnh "trước" lúc lập plan là 3,9% dù hai màn hình khác hẳn nhau, vì phần lớn ảnh là nền.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreen.kt` | Bố cục theo thiết kế, tham số `appName`, `LoadingRoute` lấy tên app, test tag `APP_NAME`, `RETRY`, preview |
| Sửa | `feature/loading/src/main/res/values/strings.xml` | Chữ theo bảng ở mục 3.1 |
| Tạo | `feature/loading/src/main/res/drawable/ic_loading_alert.xml` | Biểu tượng cảnh báo |
| Tạo | `feature/loading/src/main/res/drawable/ic_loading_retry.xml` | Biểu tượng trên nút |
| Tạo | `feature/loading/src/main/res/drawable/ic_loading_lock.xml` | Biểu tượng khóa |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenTest.kt` | Test theo bảng chữ (mục 4) |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenScreenshotTest.kt` | Tiến độ 0,68, tên app "Todo App", thêm `failed_dark` cùng tên ảnh thiết kế |
| Ghi lại | `feature/loading/src/test/screenshots/LoadingScreen_progress_light.png`, `LoadingScreen_progress_dark.png`, `LoadingScreen_failed_light.png` | Giao diện mới |
| Tạo | `feature/loading/src/test/screenshots/LoadingScreen_failed_dark.png` | Trạng thái lỗi, chế độ tối |
| Tạo | `docs/features/loading/design/review/` (ảnh `*_compare.png`, `summary.md`) | Sinh bởi `scripts/design-compare.sh loading` |
| Sửa | `docs/features/loading/design/README.md` | Chỉ mục 8: kết quả đối chiếu và nhận xét rà soát |

Không sửa `:core:designsystem`, `LoadingViewModel`, `LoadingUiState`, dữ liệu, điều hướng hay `:app`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không chạm (`feature_loading_enabled` như T-001).
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, tiêu chí 1: đủ các phần khi đang tải | Compose UI test (Robolectric) | `LoadingScreenTest.inProgress_showsIdentityProgressAndGuidance`: `InProgress(0.68f)`, `appName = "Todo App"`; có chữ "Todo App" (tag `APP_NAME`), `brand_tagline`, `loading_status`, `loading_guidance`, `loading_footer`; `PERCENT` là "68%"; có `PROGRESS`; không có `ERROR`, không có `loading_status_failed` |
| KB-2, tiêu chí 1: giao diện lỗi | Compose UI test | `LoadingScreenTest.failed_showsErrorTitleGuidanceAndRetry_andRetryCallsOnRetry`: `Failed(0.68f)`; có `loading_status_failed`, `loading_error_title`, `loading_error`; `PERCENT` là "68%"; không có `loading_guidance`; bấm `RETRY` gọi `onRetry` một lần |
| Logo không đổi vị trí giữa hai trạng thái | Compose UI test | `LoadingScreenTest.appName_staysInPlace_whenFailed`: vị trí đỉnh của `APP_NAME` (`getBoundsInRoot().top`) ở `InProgress(0.68f)` bằng ở `Failed(0.68f)` |
| Nút đủ lớn | Compose UI test | `LoadingScreenTest.retryButton_isAtLeast48dpHigh`: `RETRY` `assertHeightIsAtLeast(48.dp)` |
| Đúng thiết kế | Screenshot test cùng tên ảnh thiết kế, ảnh so sánh, rà soát độc lập | `LoadingScreenScreenshotTest.progress_light`, `progress_dark`, `failed_light`, `failed_dark` ghi `LoadingScreen_progress_light.png`, `LoadingScreen_progress_dark.png`, `LoadingScreen_failed_light.png`, `LoadingScreen_failed_dark.png`, đều `appName = "Todo App"`, tiến độ 0,68; `scripts/design-compare.sh loading`; rà soát theo `design/README.md`, mục 7 |
| Luồng T-001 không đổi | Unit test, test trên emulator | `LoadingViewModelTest`, `DefaultPreparationRepositoryTest` và test trên emulator của `:app` qua, không sửa |
| Giao diện trên máy thật | Kiểm tra thủ công | Mở bản staging, xem màn hình loading ở chế độ sáng và tối; tắt mạng, xóa dữ liệu app, mở lại để xem trạng thái lỗi |

**Ngoại lệ viết test trước:** ảnh chuẩn screenshot, ảnh so sánh và vector drawable không viết trước được; ba test giao diện ở trên được viết trước và phải thất bại (thiếu tham số `appName`, thiếu chữ mới) trước khi sửa `LoadingScreen`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Vài file trong một module feature |
| B. Độ rõ của yêu cầu | 1 | Có thiết kế và số liệu; spec đang chờ xác nhận lại (câu 6, 7) |
| C. Khả năng kiểm chứng tự động | 1 | Chữ và bố cục chính có test; độ giống thiết kế phải xem ảnh so sánh |
| D. Ảnh hưởng chéo | 1 | Dùng thành phần dùng chung của T-002, không sửa |
| E. Độ mới của kỹ thuật | 1 | Lần đầu làm theo gói thiết kế và chạy ảnh so sánh trong phiên tự chạy |
| **Tổng** | 5 | **Mức: Trung bình** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: không sửa; dùng `BrandLogo`, `ProgressBar`, `PrimaryButton(icon)`, `brand_tagline` của T-002.
- Task khác trong sprint có thể xung đột: T-002 ghi lại cùng ba ảnh loading; T-003 ghi lại lần nữa sau đó.
- Đề xuất thứ tự chạy: sau T-002. T-002 dừng thì T-003 bỏ qua (phụ thuộc).

## 7. Điều kiện dừng riêng

1. `BrandLogo`, `ProgressBar`, `PrimaryButton(icon)` hoặc `brand_tagline` chưa có trên nhánh chính (T-002 chưa merge).
2. Muốn khớp ảnh thiết kế hơn bằng cách viết cứng màu, cỡ chữ hay khoảng cách, hoặc sửa `:core:designsystem`.
3. Rà soát độc lập xếp một sai lệch vào mức chặn mà không sửa được trong ranh giới file của plan.
4. Chữ bị cắt hoặc tràn ở khung 411 x 891.

## 8. Câu hỏi cho người duyệt

1. **Tiến độ trong ảnh là 68%** theo thiết kế, không phải 50% và 75% như T-001. Đề xuất: dùng 68% cho cả bốn ảnh, để so được với thiết kế.
- Trả lời: Dùng 68%

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** 

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261017-01, phiên bản plan 1

- **Kết quả:** đã merge.
- **Nhánh, PR:** `claude/T-003-giao-dien-loading`, PR #40, merge thành commit c7449da trên nhánh chính.
- **Đã làm:** đủ mục 3.1. `LoadingScreen` theo bố cục của thiết kế, tham số `appName`, `LoadingRoute` lấy tên app qua `applicationInfo.loadLabel`, test tag `APP_NAME` và `RETRY`. Có hai preview (đang tải, lỗi). Ba vector drawable chép nguyên `d` và `strokeWidth` từ `handoff/`, bỏ `clipPath` của biểu tượng khóa. Chữ theo bảng chữ, xóa `loading_message`. Ghi lại ba ảnh chuẩn, thêm `LoadingScreen_failed_dark.png`. Chạy `scripts/design-compare.sh loading`, commit `design/review/`, ghi kết quả và nhận xét vào mục 8 của `design/README.md`. 17 file thay đổi, đều nằm trong mục 3.2.
- **Test viết trước:** 4 test trong `LoadingScreenTest`, commit a428a4d, đều thất bại đúng lý do (không tìm thấy tag `APP_NAME`, `RETRY`; chữ "Tiến độ đã dừng" không hiện). Commit này có thêm khung rỗng để test biên dịch được: tham số `appName` chưa dùng, hai test tag, chữ mới trong `strings.xml`. Một lần sửa chi tiết test, không đổi điều test kiểm tra: ktlint sắp lại thứ tự import của `LoadingScreenTest`. Test có kiểm tra kích thước dùng `@GraphicsMode(NATIVE)` và khung 411 x 891.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 5 ghi chú. Phần dưới lệch dần xuống so với thiết kế, nhiều nhất khoảng 10 dp ở nút "Thử lại", do chiều cao dòng và khoảng cách làm tròn về token; mỗi phần lệch nằm trong mục 6 của thiết kế. Chữ ở chân trang và lời hướng dẫn rộng hơn một chút. Chiều cao nút do `PrimaryButton` quyết định. Màu báo lỗi chỉ kiểm qua screenshot. Cách lấy tên app chỉ kiểm được bằng đọc code và trên staging.
- **Kết quả CI:** một vòng, xanh: `changes`, `build-and-test`, `ui-test` trên emulator. `LoadingViewModelTest`, `DefaultPreparationRepositoryTest` và test trên emulator của `:app` qua, không sửa.
- **Đối chiếu thiết kế:** điểm ảnh khác 2,5% (đang tải, sáng và tối), 5,2% (lỗi, sáng), 5,6% (lỗi, tối). Trước khi làm: 3,5%, 4,0%, 9,7%, chưa có.
- **Lệch so với plan:** Không có.
- **Việc còn lại:** kiểm tra thủ công trên bản staging (mục 4).
- **Cần người xem:** bốn ảnh ghép trong `docs/features/loading/design/review/`; bản staging ở chế độ sáng và tối, và trạng thái lỗi (tắt mạng, xóa dữ liệu app, mở lại).
