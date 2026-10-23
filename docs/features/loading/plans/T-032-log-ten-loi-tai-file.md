---
ticket: T-032
title: Log tải file thất bại ghi loại lỗi và lý do
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.0M         # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [core]      # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-032: LOG TẢI FILE THẤT BẠI GHI LOẠI LỖI VÀ LÝ DO

## 1. Tóm tắt

Thêm vào `:core:assets` hai hàm có unit test:
- `downloadFailureMessage` dựng câu log gồm đường dẫn, lý do, chuỗi loại lỗi và mã HTTP;
- `downloadFailureLogger` ghi câu đó bằng `AppLogger` ở mức cảnh báo, tag `Assets`.

`AssetsModule` dùng hàm thứ hai thay cho `android.util.Log.w`. Kiểm chứng bằng unit test, `scripts/check.sh` và một bước xem logcat trên bản staging.

## 2. Hiểu yêu cầu

Ticket không xử lý kịch bản nào của spec: hành vi của app giữ nguyên. Yêu cầu đến từ phản hồi `features/loading/feedback/FB-20261022-04-log-ly-do-tai-file.md` và bài học L-027, phần (a).

| Yêu cầu | Tiêu chí | Cách hiểu của AI |
| --- | --- | --- |
| FB-20261022-04, Mong đợi | Một dòng log cho biết loại lỗi (ví dụ `cause=UnknownHostException`) và mã HTTP nếu có, không chứa dữ liệu cá nhân | Câu log có `reason=<AssetFailureReason>`, `error=<lớp lỗi> <- <lớp nguyên nhân> ...` (tối đa 5 lớp), và `status=<mã>` khi chuỗi lỗi có `HttpStatusException`. Không ghi message của lỗi (có thể chứa địa chỉ đầy đủ), không ghi gì về người dùng |
| L-027 (a) | `:core:assets` ghi tên lớp lỗi trong câu log | Như trên. Android bỏ stack trace của `UnknownHostException` khi in ra logcat, nên tên lớp phải nằm trong chính câu log |
| ADR-0019 | Log hành vi của app đi qua `AppLogger` | `AssetsModule` hiện gọi thẳng `android.util.Log.w("Assets", ...)`, nên dòng này không có tiền tố `VGL.`, cũng không vào breadcrumb của Crashlytics. Plan chuyển sang `AppLogger.warn("Assets", ...)`: logcat ghi `VGL.Assets`, Crashlytics giữ breadcrumb kèm tên lớp lỗi |

Vì sao đặt ticket ở feature `loading`: không có thư mục tài liệu riêng cho `:core:assets`. Phản hồi FB-20261022-04 thuộc `loading` (ticket T-010, tải bộ khởi đầu ở màn loading), và màn loading là nơi người dùng gặp lỗi tải file đầu tiên.

Thiết kế tham chiếu: không có thiết kế riêng (không đổi giao diện).

ADR liên quan: ADR-0019 (log), ADR-0015 (file tĩnh trên R2), ADR-0001 (core chỉ phụ thuộc core).

## 3. Giải pháp

### 3.1. Cách làm

File mới `core/assets/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/DownloadFailureLog.kt`:

```kotlin
/** "Could not download <path> (reason=NETWORK, error=IOException <- UnknownHostException)", plus ", status=404". */
internal fun downloadFailureMessage(path: AssetPath, error: IOException): String

/** Logs a failed download as a warning under the tag "Assets" (ADR-0019). */
internal fun downloadFailureLogger(logger: AppLogger): (AssetPath, IOException) -> Unit =
    { path, error -> logger.warn(LOG_TAG, downloadFailureMessage(path, error), error) }
```

- Chuỗi lớp lỗi: `generateSequence<Throwable>(error) { it.cause }.take(MAX_CLASSES)` với `MAX_CLASSES = 5`, nối `simpleName` bằng `" <- "`. Lớp không tên (lớp ẩn danh) thì dùng `javaClass.name`.
- `reason` lấy từ `error.toFailureReason()` có sẵn (`AssetFailureReason.kt`), nên log và giao diện luôn cùng một cách phân loại.
- `status`: lấy từ `HttpStatusException` đầu tiên trong chuỗi lỗi (`HttpStatusException.status`, có sẵn trong `AssetDownloader.kt`).
- Không ghi `error.message`: message của `HttpStatusException` chứa địa chỉ đầy đủ, message của lỗi khác không kiểm được nội dung.

`AssetsModule.remoteAssets(...)` nhận thêm `logger: AppLogger` (đã gắn trong `SingletonComponent` bởi `LoggingModule` của `:core:logging`) và truyền `onDownloadFailed = downloadFailureLogger(logger)`. Bỏ `import android.util.Log`. `core/assets/build.gradle.kts` thêm `implementation(project(":core:logging"))`. Core phụ thuộc core là hợp lệ (ADR-0001), giống `:core:network` và `:core:poses`.

**Thứ tự làm (L-025).**

- Bước 3 (commit test):
  - `build.gradle.kts` thêm `:core:logging`, vì test cần `RecordingAppLogger`;
  - `DownloadFailureLog.kt` dạng khung rỗng: `downloadFailureMessage` trả `""`, `downloadFailureLogger` trả `{ _, _ -> }`;
  - `DownloadFailureLogTest.kt`.
- Bước 4 (commit code): thân hai hàm, `AssetsModule.kt`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/assets/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/DownloadFailureLog.kt` | `downloadFailureMessage`, `downloadFailureLogger` |
| Sửa | `core/assets/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/di/AssetsModule.kt` | Nhận `AppLogger`, dùng `downloadFailureLogger`; bỏ `android.util.Log` |
| Sửa | `core/assets/build.gradle.kts` | `implementation(project(":core:logging"))` |
| Tạo | `core/assets/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/DownloadFailureLogTest.kt` | Unit test ở mục 4 |
| Sửa | `docs/features/loading/plans/T-032-log-ten-loi-tai-file.md` | Nhật ký thực hiện |
| Sửa | `docs/features/loading/tickets/T-032-log-ten-loi-tai-file.md` | Trạng thái |

`DefaultRemoteAssets.kt` không đổi: nó đã nhận `onDownloadFailed: (AssetPath, IOException) -> Unit`. Các test hiện có của `:core:assets` dựng `DefaultRemoteAssets` với lambda riêng, không qua `AssetsModule`, nên không bị ảnh hưởng.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm. Log không phải sự kiện phân tích; breadcrumb của Crashlytics chỉ gửi kèm báo cáo lỗi (ADR-0012, ADR-0019).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `:core:assets` thêm phụ thuộc vào module `:core:logging` của dự án. `:core:logging` đã kéo Firebase Crashlytics, đã có trong app qua `:core:network` và các feature.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** Hilt không cung cấp được `AppLogger` cho `AssetsModule`, ví dụ khi một test Robolectric có `@HiltAndroidTest` của `:app` thay `LoggingModule` mà không gắn `AppLogger`.
- **Điều kiện chuyển:** bước 5 báo lỗi biên dịch Hilt `[Dagger/MissingBinding] AppLogger` ở `:app`.
- **Cách làm thay:** giữ `android.util.Log.w("Assets", downloadFailureMessage(path, error), error)` trong `AssetsModule`, bỏ phụ thuộc `:core:logging`, và chỉ commit `downloadFailureMessage` cùng test của nó. `downloadFailureLogger` và test của nó bỏ ra; ghi lý do vào nhật ký. Tiêu chí về nội dung câu log giữ nguyên.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| FB-20261022-04, lỗi không phân giải tên miền | Unit test | `DownloadFailureLogTest.unknownHost_namesTheErrorAndNetworkReason`: `downloadFailureMessage(AssetPath("poses/catalog/v1/catalog-ab12cd34.json"), UnknownHostException("cdn.example"))` bằng đúng `"Could not download poses/catalog/v1/catalog-ab12cd34.json (reason=NETWORK, error=UnknownHostException)"`. Khung rỗng trả `""` nên thất bại |
| FB-20261022-04, nguyên nhân bọc trong lỗi khác | Unit test | `DownloadFailureLogTest.wrappedCause_listsTheChain`: `IOException("x", SocketTimeoutException())` cho `"... (reason=NETWORK, error=IOException <- SocketTimeoutException)"` |
| FB-20261022-04, mã HTTP | Unit test | `DownloadFailureLogTest.httpStatus_isWritten`: `HttpStatusException(404, "https://cdn.example/a/v1/x.json")` cho `"... (reason=NOT_FOUND, error=HttpStatusException, status=404)"`. Bước đối chứng (L-035): cùng test khẳng định câu log không chứa `"https://"`, để một bản cài đặt ghi `error.message` thất bại |
| Chuỗi lỗi dài không làm log phình | Unit test | `DownloadFailureLogTest.longChain_stopsAtFiveClasses`: chuỗi 7 lớp `IOException` lồng nhau cho đúng 4 dấu `" <- "` |
| ADR-0019, ghi qua `AppLogger` | Unit test | `DownloadFailureLogTest.logger_writesOneWarningUnderAssets`: `val logger = RecordingAppLogger()`; `downloadFailureLogger(logger)(path, error)`; `logger.entries` có đúng một `LogEntry(LogLevel.WARN, "Assets", downloadFailureMessage(path, error), error)`. Khung rỗng không ghi gì nên thất bại |
| Không đổi hành vi tải file | Unit test | Các test hiện có của `:core:assets` (`DefaultRemoteAssetsTest`, `DefaultRemoteAssetsFailureReasonTest`, `DefaultRemoteAssetsNetworkFailureTest`, `AssetFailureReasonTest`, `AssetPathTest`) qua mà không sửa |
| Log trên máy thật | Kiểm tra thủ công | Bản staging, máy ở chế độ máy bay, mở app lần đầu: `adb logcat -v threadtime \| grep ' VGL\.'` có dòng `W VGL.Assets: Could not download ... (reason=NETWORK, error=UnknownHostException)` |

**Ngoại lệ viết test trước:** `AssetsModule.kt` (nối Hilt) và `build.gradle.kts` không có test riêng. Phần nối chỉ truyền hàm đã có test; biên dịch Hilt ở `scripts/check.sh` và bước thủ công kiểm phần này.

### 4.1. Chạy thử

Không cần (E dưới 2): dùng lại `RecordingAppLogger` và mẫu phụ thuộc `:core:logging` như `:core:network`. Chưa chạy Gradle cho plan này.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (module core dùng chung `:core:assets`). Plan xếp **Cao**, cần duyệt chéo. Nếu không tính điểm chặn, năm tiêu chí dưới đây cho 3 điểm.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Ba file trong một module, một file mới |
| B. Độ rõ của yêu cầu | 0 | Phản hồi nêu rõ điều mong đợi |
| C. Khả năng kiểm chứng tự động | 1 | Câu log và ghi log có unit test; dòng trên logcat kiểm thủ công |
| D. Ảnh hưởng chéo | 1 | Mọi feature tải file qua `:core:assets`, nhưng chỉ dòng log đổi |
| E. Độ mới của kỹ thuật | 0 | Đúng mẫu `AppLogger` và `RecordingAppLogger` đã có |
| **Tổng** | 3 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `:core:assets` (`AssetsModule`, `build.gradle.kts`).
- Task khác có thể xung đột: không task nào đang có plan sửa `:core:assets`.
- Đề xuất thứ tự chạy: task độc lập, chạy sớm; log rõ giúp phân tích các phiên thử sau (`docs/ops/test-sessions.md`).
- Có ích chung: `:core:assets` giống hệt dự án mẫu ở chỗ này; sau khi merge, đề xuất về dự án mẫu (`docs/ops/from-app.md`).

## 7. Điều kiện dừng riêng

- Một test hiện có của `:core:assets` thất bại sau thay đổi: dừng, không sửa test cũ.
- `checkModuleDependencies` báo lỗi khi `:core:assets` phụ thuộc `:core:logging`: dừng.

## 8. Câu hỏi cho người duyệt

1. **Chuyển sang `AppLogger`.** Plan đổi tag logcat từ `Assets` sang `VGL.Assets`, và thêm breadcrumb vào Crashlytics (chỉ gửi kèm báo cáo lỗi). Đề xuất của AI: đồng ý, đúng ADR-0019 và cách lọc log `grep ' VGL\.'` trong `CLAUDE.md`. Không muốn thì dùng phương án ở mục 3.4.
- Trả lời:
2. **Trạng thái của FB-20261022-04.** Phản hồi đang `new`. Đề xuất của AI: chủ feature chuyển sang `planned`, liên kết ticket T-032; plan không tự đổi trạng thái phản hồi.
- Trả lời:
3. **Mức rủi ro.** Plan xếp Cao chỉ vì điểm chặn 7. Cần một dev khác duyệt chéo.
- Trả lời:

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
