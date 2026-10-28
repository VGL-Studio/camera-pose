---
ticket: T-072
title: Ghi log khi không hỏi được danh sách camera, làm chặt test chữ của camera và loading
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 3                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: low              # low | medium | high
token_estimate: 5.8M         # cỡ S (scripts/token-usage.py estimate, 2026-10-28)
blocking_points: []
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at: 2026-10-28
---

# PLAN T-072: GHI LOG KHI KHÔNG HỎI ĐƯỢC DANH SÁCH CAMERA, LÀM CHẶT TEST CHỮ CỦA CAMERA VÀ LOADING

## 1. Tóm tắt

Ba ghi chú rà soát còn treo ở phần app (BR-20261028-01, dòng 6). Một: `CameraXController.availableLenses` nuốt lỗi khi hỏi danh sách camera mà không ghi log (ghi chú rà soát T-064, RUN-20261027-01; ADR-0019); plan thêm `logger.warn` ở cả hai chỗ bắt lỗi, kết quả trả về giữ nguyên. Hai: `CameraStringsTest.opacityLabel_isNotTransparency` dùng gốc tiếng Hindi `पारदर्शिता` nên không bắt tính từ `पारदर्शी`, và không chuẩn hóa NFC (ghi chú rà soát T-055). Ba: `LoadingStringsTest.serverGuidance_doesNotAskToCheckTheNetwork` chỉ chặn một từ chỉ mạng mỗi ngôn ngữ (ghi chú rà soát T-057). Đã chạy thử trên code hiện tại: hai test log mới thất bại đúng lý do; hai test chữ làm chặt qua trên chữ hiện tại và thất bại khi đột biến chữ (mục 4.1).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-4 | Khi máy chỉ có một camera thì nút đổi camera mờ đi, không bấm được; camera mở bằng camera máy có, kể cả khi lần trước đã chọn camera kia | Hành vi không đổi (T-064). Plan chỉ thêm log khi máy không trả lời được nó có camera nào, trường hợp "không biết" mà T-064 để phần mở camera quyết |

Hai test chữ không thêm tiêu chí mới: chúng kiểm chặt hơn tiêu chí đã có của `features/settings/spec.md` KB-4 (nhãn "Độ đậm" không đọc ngược nghĩa, T-055) và `features/loading/spec.md` KB-2, KB-5 (lời báo máy chủ không bảo kiểm mạng, T-057).

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện).

ADR liên quan: ADR-0019 (ghi log lỗi đã bắt), ADR-0031 (test có sẵn), ADR-1003 (test cho hành vi đã đúng).

## 3. Giải pháp

### 3.1. Cách làm

**Log camera.** `lensesOf` nhận thêm tham số `onError: (Exception) -> Unit = {}`, đặt trước `hasCamera` để các lời gọi `lensesOf { ... }` có sẵn (`CameraLensesTest`) không đổi:

```kotlin
internal fun lensesOf(onError: (Exception) -> Unit = {}, hasCamera: (Lens) -> Boolean): Set<Lens> = try {
    Lens.entries.filterTo(mutableSetOf(), hasCamera)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    onError(e)
    emptySet()
}
```

`CameraXController.availableLenses`:

- gọi `lensesOf(onError = { logger.warn(LOG_TAG, "Camera list unavailable, the open decides", it) }) { lens -> ... }`;
- nhánh `catch (e: Exception)` ngoài cùng (lấy `ProcessCameraProvider` lỗi) cũng ghi `logger.warn(LOG_TAG, "Camera provider unavailable, the open decides", e)` trước `emptySet()`.
- Nhánh `CancellationException` không ghi log (bị hủy không phải lỗi). Lời log không chứa dữ liệu cá nhân, chỉ có loại lỗi qua `error`.

**Test chữ camera.** Trong `CameraStringsTest`:

- `OPPOSITE_WORDS`: `"hi" to "पारदर्शिता"` đổi thành `"hi" to "पारदर्श"`. Điều kiện "gốc bắt đầu một từ" (`(?<![\p{L}\p{M}])`) giữ nguyên, nên `अपारदर्शिता` (độ đậm) vẫn không khớp.
- `opacityLabel_isNotTransparency` chuẩn hóa cả nhãn và gốc từ bằng `Normalizer.normalize(..., Normalizer.Form.NFC)` trước khi dựng `Regex`. Sửa chú thích KDoc của `OPPOSITE_WORDS` và chú thích trong test cho khớp (gốc của danh từ và tính từ, NFC).

**Test chữ loading.** Trong `LoadingStringsTest`, `NETWORK_WORDS` đổi từ `mapOf(tag to word)` thành `mapOf(tag to listOf(...))`:

| Ngôn ngữ | Từ (hai từ đầu là đối chứng, phải có trong `loading_error`) |
| --- | --- |
| `en` | `network`, `connection`, `internet`, `wi-fi` |
| `vi` | `mạng`, `kết nối`, `internet`, `wi-fi` |
| `hi` | `नेटवर्क`, `कनेक्शन`, `इंटरनेट`, `वाई-फ़ाई` |
| `pt-BR` | `rede`, `conexão`, `internet`, `wi-fi` |
| `es-419` | `red`, `conexión`, `internet`, `wi-fi` |
| `id` | `jaringan`, `koneksi`, `internet`, `wi-fi` |
| `th` | `เครือข่าย`, `เชื่อมต่อ`, `อินเทอร์เน็ต`, `ไวไฟ` |

`serverGuidance_doesNotAskToCheckTheNetwork`: đối chứng `words.take(2)` đều có trong `loading_error` (thay cho một từ); `loading_error_server` không chứa từ nào của danh sách (thay cho một từ). Cách so giữ nguyên: chữ thường `Locale.ROOT`, chứa chuỗi con.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraLenses.kt` | Tham số `onError` có giá trị mặc định |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraXController.kt` | Hai `logger.warn` trong `availableLenses` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraXControllerTest.kt` | `controller()` nhận `logger` (mặc định `RecordingAppLogger()`); ba test mới (mục 4) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraLensesTest.kt` | Một test mới: `onError` nhận đúng lỗi |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraStringsTest.kt` | Gốc `पारदर्श`, chuẩn hóa NFC |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingStringsTest.kt` | Danh sách từ chỉ mạng |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm (`AppLogger`, `RecordingAppLogger` và `mockk` đã có trên classpath của `:feature:camera`, `CameraXControllerTest` đang dùng)

### 3.4. Phương án dự phòng

Không có.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` đọc mục 3.2: kết quả ở dưới bảng.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `lensesOf` lỗi thì trả tập rỗng, không báo ai | Vẫn trả tập rỗng, gọi `onError` (mặc định không làm gì) | `CameraLensesTest.lensesOf_error_isUnknown`, `lensesOf_cancellation_isRethrown`, `lensesOf_bothCameras`, `lensesOf_backOnly` | Giữ nguyên: lời gọi không truyền `onError` nên hành vi như cũ |
| `availableLenses` lỗi thì trả tập rỗng, không ghi log | Vẫn trả tập rỗng, ghi một dòng `warn` thẻ `Camera` | Không có test nào kiểm `availableLenses` (các test của `CameraXControllerTest` kiểm bind và unbind) | Không có câu cũ phải đổi |
| Test chữ chặn một gốc tiếng Hindi, một từ chỉ mạng mỗi ngôn ngữ | Chặn gốc ngắn hơn, nhiều từ hơn | `CameraStringsTest.opacityLabel_isNotTransparency`, `LoadingStringsTest.serverGuidance_doesNotAskToCheckTheNetwork` | Làm chặt, không nới: mọi từ cũ vẫn nằm trong danh sách mới (gốc `पारदर्श` là tiền tố của gốc cũ); đối chứng cũ (từ đầu có trong `loading_error`) vẫn còn. Đây là dữ liệu của test do plan đổi (ADR-0031), ghi ở mục "Test cũ viết lại" của PR |

Kết quả `scripts/existing-tests.py` (2026-10-28): 35 test dùng tên hay tag của hai file code; "Cần xem trước" 0; "Nên xem" 2 (`CameraLensesTest.lensesOf_bothCameras`, `lensesOf_backOnly`, gọi `lensesOf { ... }` không truyền `onError`, giữ nguyên); 30 test còn lại chỉ dùng tên plan không đổi (`CameraXController`, `LOG_TAG`).

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Hỏi danh sách camera lỗi thì ghi log `warn` (ADR-0019), kết quả vẫn "không biết" | Unit test (Robolectric) | `CameraXControllerTest.lensQueryFails_logsWarn_andLensesAreUnknown`: `every { provider.hasCamera(any()) } throws IllegalStateException(...)`; `availableLenses()` trả `emptySet()`; `logger.entries` có đúng một mục `LogLevel.WARN`, thẻ `Camera`, `error is IllegalStateException` |
| Lấy `ProcessCameraProvider` lỗi thì ghi log `warn` | Unit test (Robolectric) | `CameraXControllerTest.providerFails_logsWarn_andLensesAreUnknown`: `coEvery { ProcessCameraProvider.awaitInstance(any()) } throws IllegalStateException(...)`; trả `emptySet()`; đúng một mục `WARN` thẻ `Camera` |
| Đối chứng: hỏi được thì không ghi `warn` (L-035) | Unit test (Robolectric) | `CameraXControllerTest.lensQueryOk_noWarn`: `hasCamera` trả `true`; trả `{BACK, FRONT}`; không có mục `WARN` |
| `lensesOf` báo lỗi cho bên gọi | Unit test | `CameraLensesTest.lensesOf_error_isReportedToCaller`: `lensesOf(onError = { errors += it }) { throw IllegalStateException() }` trả `emptySet()`, `errors` có một phần tử `is IllegalStateException` |
| Nhãn "Độ đậm" không đọc ngược nghĩa, cả tính từ tiếng Hindi và chữ tổ hợp | Unit test (Robolectric) | `CameraStringsTest.opacityLabel_isNotTransparency` với gốc `पारदर्श` và NFC (mục 3.1) |
| Lời báo máy chủ không bảo kiểm mạng, với mọi từ chỉ mạng | Unit test (Robolectric) | `LoadingStringsTest.serverGuidance_doesNotAskToCheckTheNetwork` với danh sách từ (mục 3.1) |

**Ngoại lệ viết test trước:** hai test chữ kiểm hành vi đã đúng (chữ hiện tại không chứa từ nào bị cấm), nên không thất bại trước được (ADR-1003). Thay vào đó: đột biến chữ trong một khối tạm, không commit, ghi kết quả vào nhật ký plan (L-108 của mẫu), như đã chạy ở mục 4.1: `values-hi` `camera_adjust_opacity` = `पारदर्शी स्तर`; `values-vi` `camera_adjust_opacity` = `Độ mờ` ở dạng NFD; `values-vi` `loading_error_server` thêm "kết nối". Mỗi đột biến làm test mới đỏ và test cũ xanh. `CameraLensesTest.lensesOf_error_isReportedToCaller` không biên dịch được trên code cũ (thiếu tham số): đó là lý do thất bại đúng của test viết trước.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-28, trên `origin/main` 2b8d67e, các test tạm không commit (bản sao gần đúng của các test mục 4, tên `tmp...`).
- **Lệnh:** `./gradlew :feature:camera:testDebugUnitTest --tests '*CameraXControllerTest.tmp*'`; `./gradlew :feature:camera:testDebugUnitTest --tests '*CameraStringsTest*' :feature:loading:testDebugUnitTest --tests '*LoadingStringsTest*'` (E dưới 2, không bắt buộc `check.sh`).
- **Kết quả:**
  - Log camera, code hiện tại: `tmpLensQueryFails_logsWarn` đỏ `[] expected:<1> but was:<0>`; `tmpProviderFails_logsWarn` đỏ `[] expected:<1> but was:<0>`; `tmpLensQueryOk_noWarn` qua (đối chứng).
  - Test chữ làm chặt, chữ hiện tại: qua cả hai.
  - Đột biến `values-hi` `पारदर्शी स्तर`: test mới đỏ "hi reads the other way round: पारदर्शी स्तर", `opacityLabel_isNotTransparency` cũ qua.
  - Đột biến `values-vi` `Độ mờ` dạng NFD: test mới đỏ "vi reads the other way round: Độ mờ", test cũ qua.
  - Đột biến `values-vi` `loading_error_server` thêm "kết nối": test mới đỏ "vi server has 'kết nối'", `serverGuidance_doesNotAskToCheckTheNetwork` cũ qua (`vietnamese_matchesDesignTextTable` đỏ vì đổi chữ, không liên quan).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Hai module feature, nhưng ở `:feature:loading` chỉ một file test; code chỉ hai file của `:feature:camera`, `lensesOf` là `internal`, tham số mới có mặc định |
| B. Độ rõ của yêu cầu | 0 | Ghi chú rà soát nêu rõ cách sửa; spec đã xác nhận, hành vi không đổi |
| C. Khả năng kiểm chứng tự động | 0 | Mọi thay đổi có test JVM; đã chạy thử |
| D. Ảnh hưởng chéo | 1 | Dùng `AppLogger` của `:core:logging`, không sửa |
| E. Độ mới của kỹ thuật | 0 | Đúng mẫu `logger.warn` có sẵn trong `CameraXController` (dòng 167) và mẫu `mockk` của `CameraXControllerTest` |
| **Tổng** | 3 | **Mức: Thấp** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: không. `AppLogger` chỉ được dùng.
- Task khác trong sprint có thể xung đột: T-073 (chỉ `scripts/tests/`), không chung file.
- Đề xuất thứ tự chạy: độc lập, chạy trước hay sau T-073 đều được.

## 7. Điều kiện dừng riêng

- Một bản dịch hiện tại làm test chữ mới đỏ (chữ thật chứa từ bị cấm): dừng, không sửa chữ trong task này; ghi chữ và từ vào báo cáo để người duyệt bản dịch quyết.

## 8. Câu hỏi cho người duyệt

1. Danh sách từ chỉ mạng ở mục 3.1 do AI chọn (mạng, kết nối, internet, wi-fi). Người biết tiếng có thể thêm từ; đề xuất: duyệt danh sách này, thêm sau khi duyệt bản dịch (BR-20261028-01, dòng 5).
2. `availableLenses` ghi `warn` mỗi lần mở màn camera khi máy lỗi. Đề xuất: giữ `warn` mỗi lần (lỗi hiếm, ADR-0019 nói ghi lỗi đã bắt), không đếm hay gộp.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật
- **Ghi chú:** Ngày 2026-10-28, trả lời mục 8 theo đề xuất của AI: (1) duyệt danh sách từ chỉ mạng; (2) ghi `warn` mỗi lần, không gộp.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
