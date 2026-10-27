---
ticket: T-057
title: Màn loading báo "máy chủ chưa phản hồi" khi máy có mạng mà vẫn không tải được
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 7                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: medium           # low | medium | high
token_estimate: 15M          # cỡ M (7.8M, scripts/token-usage.py estimate) gần gấp đôi: RUN-20261026-01 tính cả phiên con vào task, hai task cỡ M ở đó 15.0M và 19.7M
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-26
---

# PLAN T-057: MÀN LOADING BÁO "MÁY CHỦ CHƯA PHẢN HỒI" KHI MÁY CÓ MẠNG MÀ VẪN KHÔNG TẢI ĐƯỢC

## 1. Tóm tắt

Màn loading luôn bảo "kiểm tra mạng" khi một phần việc không xong, kể cả khi máy có mạng mà máy chủ không trả lời (FB-20261022-03). Plan thêm lý do vào trạng thái lỗi: khi phần việc không xong, ViewModel đọc trạng thái mạng của hệ thống (`ConnectivityManager`, mạng mặc định có `NET_CAPABILITY_INTERNET` và `NET_CAPABILITY_VALIDATED`); đã xác nhận thì lỗi là `SERVER` và màn hình hiện lời mới `loading_error_server` ("Máy chủ chưa phản hồi. Vui lòng thử lại sau ít phút."), còn lại là `OFFLINE` với lời cũ. Mọi thứ nằm trong `:feature:loading`; kiểm chứng bằng unit test, test Robolectric của phần đọc mạng, test giao diện ở 7 ngôn ngữ và 2 ảnh chuẩn mới. Cả thay đổi đã chạy thử trên code hiện tại (mục 4.1).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/loading/spec.md` (spec đang `changed`: KB-2 thêm một tiêu chí, kịch bản mới KB-5, câu hỏi 11 trong PR này, chờ người phụ trách sản phẩm xác nhận lại):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-2 | Khi một phần việc không xong được mà máy không có mạng, hoặc có mạng nhưng hệ thống chưa xác nhận mạng đó vào được Internet, thì lời hướng dẫn là "Kết nối đang gián đoạn. Vui lòng kiểm tra mạng và thử lại để tiếp tục." | `LoadingFailure.OFFLINE`: mạng mặc định không có, hoặc thiếu `NET_CAPABILITY_INTERNET`, hoặc thiếu `NET_CAPABILITY_VALIDATED`. Đọc trạng thái mạng lỗi (ngoại lệ) cũng là `OFFLINE`, vì đó là lời cũ, an toàn |
| KB-5 | Khi một phần việc không xong được mà hệ thống xác nhận máy đang vào được Internet thì màn hình báo lỗi như KB-2, nhưng lời hướng dẫn là "Máy chủ chưa phản hồi. Vui lòng thử lại sau ít phút."; tiêu đề, nhãn, thanh tiến trình, phần trăm và nút "Thử lại" như KB-2 | `LoadingFailure.SERVER`, cho cả ba phần việc (cấu hình, catalog, bộ khởi đầu). Tiêu đề "Không thể tải dữ liệu", nhãn "Tiến độ đã dừng", màu lỗi, nút giữ nguyên |
| KB-5 | Khi bấm "Thử lại" mà phần việc lại không xong thì lời hướng dẫn chọn lại theo trạng thái mạng lúc đó | Đọc trạng thái mạng ở mỗi lần lỗi, không nhớ từ lần trước |
| KB-2, KB-3 | Các tiêu chí cũ (giữ mức đã đạt, chỉ làm lại phần chưa xong, 15 giây) | Không đổi; các test hiện có giữ nguyên và phải qua |
| KB-5 | Khi máy chủ trả lời lại và bấm "Thử lại" thì app làm tiếp các phần việc chưa xong và vào màn hình chính | Đường "Thử lại" hiện có, không đổi; test `retrySucceeds_finishes_andCountsOneRetry` và `retry_afterStarterSetFailed_runsOnlyStarterSet_andProgressContinues` phủ, vì lý do lỗi không đổi đường thử lại |

Thiết kế tham chiếu: `features/loading/design/README.md`, ảnh `LoadingScreen_progress_light`, `LoadingScreen_progress_dark`, `LoadingScreen_failed_light`, `LoadingScreen_failed_dark`. Lời mới không có frame riêng: cùng bố cục với `LoadingScreen_failed_*`, chỉ đổi một dòng chữ; bảng chữ (mục 2 của README) thêm một dòng trong PR này (mục 8, câu 3).

ADR liên quan: ADR-0019 (log: thẻ `Loading`, không ghi đường dẫn), ADR-0020 (ảnh thiết kế và screenshot test cùng tên), ADR-0001 (feature chỉ phụ thuộc core; plan không thêm phụ thuộc module).

## 3. Giải pháp

### 3.1. Cách làm

Không đổi `:core:assets`, `:core:poses` hay `:core:network`. Loại lỗi của lần tải không đủ: ở FB-20261022-02 tên miền R2 không phân giải được trong khi máy có mạng, lỗi là `UnknownHostException`, mà `:core:assets` xếp vào `NETWORK`. Trạng thái mạng của hệ thống lúc lỗi mới phân biệt được.

1. **`LoadingUiState.kt`:** `Failed(val progress: Float, val cause: LoadingFailure = LoadingFailure.OFFLINE)`; enum mới `LoadingFailure { OFFLINE, SERVER }` cùng tệp, có chú thích tiếng Anh như bản chạy thử. Mặc định `OFFLINE` để mọi chỗ gọi `Failed(x)` hiện có (test, preview) giữ nghĩa cũ.
2. **`data/InternetCheck.kt` (mới):**
   ```kotlin
   fun interface InternetCheck { fun isInternetValidated(): Boolean }

   internal class AndroidInternetCheck(context: Context) : InternetCheck {
       private val manager = context.getSystemService(ConnectivityManager::class.java)
       override fun isInternetValidated(): Boolean =
           manager.getNetworkCapabilities(manager.activeNetwork).isValidatedInternet()
   }

   internal fun NetworkCapabilities?.isValidatedInternet(): Boolean = this != null &&
       hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
       hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
   ```
   Định dạng như trên là định dạng `ktlintFormat` cho ra (lần chạy thử đầu viết khác và bị ktlint từ chối, mục 4.1).
   Chú thích của hàm cuối ghi vì sao cần `VALIDATED`: Wi-Fi chưa đăng nhập hay không ra Internet có `INTERNET` mà không có `VALIDATED`, và ở đó "kiểm tra mạng" là lời đúng. Khác `AndroidConnectivitySource` của `:core:network` (chỉ cần `INTERNET`, plan T-011 câu 1) có chủ ý: hộp thoại mất mạng không được hiện nhầm, còn ở đây lời nhầm an toàn là lời cũ.
3. **`di/LoadingModule.kt`:** thêm `@Provides fun internetCheck(@ApplicationContext context: Context): InternetCheck = AndroidInternetCheck(context)` trong `companion object`, cạnh `loadingClock()`.
4. **`LoadingViewModel.kt`:** tham số mới cuối hàm khởi tạo `private val internet: InternetCheck`. Trong `runPending()`, nhánh phần việc không xong:
   ```kotlin
   val cause = failureCause()
   logger.warn(LOG_TAG, "Task $task not done (cause=$cause), waiting for retry")
   state.value = LoadingUiState.Failed(progress(), cause)
   ```
   Hàm riêng `failureCause()`: gọi `internet.isInternetValidated()` trong `try`, bắt `RuntimeException` (ví dụ `SecurityException`), ghi `logger.warn(LOG_TAG, "Could not read the network state", e)` và coi là chưa xác nhận; trả `SERVER` khi đã xác nhận, `OFFLINE` khi chưa. Không đổi số dòng log và mức log của luồng thường (test `logsStart_eachTaskResult_retry_andFinish` giữ nguyên).
5. **`LoadingScreen.kt`:** `LoadingScreen` lấy `cause` từ `Failed` (mặc định `OFFLINE`), truyền qua `LoadingContent(..., cause = cause, ...)` tới `LoadingError(cause, onRetry)`; `Text` của lời hướng dẫn dùng `if (cause == LoadingFailure.SERVER) R.string.loading_error_server else R.string.loading_error`. Bố cục, màu, kiểu chữ, khoảng cách không đổi. Thêm một preview `LoadingScreenFailedServerPreview` cạnh `LoadingScreenFailedPreview`.
6. **Chữ, chuỗi `loading_error_server`, đặt ngay sau `loading_error` (trước `loading_retry`) trong 7 tệp:**

   | Thư mục | Chữ |
   | --- | --- |
   | `values` (tiếng Anh, mặc định) | `Our server isn\'t responding right now. Please try again in a few minutes.` |
   | `values-vi` | `Máy chủ chưa phản hồi. Vui lòng thử lại sau ít phút.` |
   | `values-pt-rBR` | `Nosso servidor não está respondendo agora. Tente novamente em alguns minutos.` |
   | `values-b+es+419` | `Nuestro servidor no responde en este momento. Vuelve a intentarlo en unos minutos.` |
   | `values-hi` | `हमारा सर्वर अभी जवाब नहीं दे रहा है। कृपया कुछ मिनट बाद फिर से कोशिश करें।` |
   | `values-in` | `Server kami sedang tidak merespons. Coba lagi dalam beberapa menit.` |
   | `values-th` | `เซิร์ฟเวอร์ของเรายังไม่ตอบสนอง โปรดลองอีกครั้งในอีกสักครู่` |

   Không có chuỗi nào đi cùng con số (L-077). Rủi ro nghĩa của lời này là bảo kiểm tra mạng trong khi máy có mạng: test `serverGuidance_doesNotAskToCheckTheNetwork` chặn từ "mạng" của từng ngôn ngữ (mục 4). Bản dịch 5 ngôn ngữ do AI làm, người biết tiếng duyệt trước khi phát hành production như T-050, T-051.
7. **Ảnh chuẩn mới** (tiếng Việt như các ảnh khác của lớp): `LoadingScreen_failed-server_light.png` ở khung thiết kế `vi-w411dp-h891dp-xxhdpi`, `LoadingScreen_failed-server_small.png` ở khung thấp `vi-w360dp-h640dp-xxhdpi`. Không có ảnh thiết kế cùng tên, nên `scripts/design-compare.sh loading` không so hai ảnh này (như `_small`). Ảnh chuẩn cũ không đổi.

Thứ tự commit của bước 3 (test trước, L-025): khung rỗng gồm mục 1 đầy đủ, `InternetCheck.kt` với `AndroidInternetCheck.isInternetValidated()` và `isValidatedInternet()` trả `false`, tham số `internet` của ViewModel chưa dùng, `@Provides` của mục 3, và `loading_error_server` chỉ trong `values/` với chữ giữ chỗ `Server guidance pending` (khác hẳn lời cũ, không có từ chỉ mạng, để các test của lời máy chủ thất bại đúng lý do và test đối chứng của lời mạng qua); cùng mọi test của mục 4 (kể cả `FakeInternetCheck` và tham số `internet = internet` trong hàm `viewModel()` của `LoadingViewModelTest`). Bước 3 chỉ chạy `./gradlew :feature:loading:testDebugUnitTest`, không chạy `scripts/check.sh` (nó tự ghi ảnh chuẩn còn thiếu). Bước 4: mục 2, 4, 5, 6 thật; xóa hai ảnh `LoadingScreen_failed-server_*.png` nếu có, rồi chạy `scripts/check.sh`: nó chỉ ghi ảnh chuẩn còn thiếu, nên ghi đúng hai ảnh mới và so mọi ảnh cũ. Không dùng `recordRoborazziDebug` (ghi lại cả ảnh cũ, có thể trúng điều kiện dừng "ảnh chuẩn cũ đổi" ở mục 7). Mở hai ảnh xem chữ mới hiện đủ, nút "Thử lại" không bị cắt.

Thay đổi có ích cho mọi app (màn loading là feature mẫu của dự án mẫu): ghi vào mô tả PR để đề xuất về dự án mẫu (`docs/ops/from-app.md`).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingUiState.kt` | `Failed.cause`, enum `LoadingFailure` |
| Tạo | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/data/InternetCheck.kt` | `InternetCheck`, `AndroidInternetCheck`, `isValidatedInternet()` |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/di/LoadingModule.kt` | `@Provides` cho `InternetCheck` |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingViewModel.kt` | Tham số `internet`, `failureCause()`, log có `cause=` |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreen.kt` | Lời hướng dẫn theo `cause`, preview mới |
| Sửa | `feature/loading/src/main/res/values/strings.xml` | `loading_error_server` tiếng Anh |
| Sửa | `feature/loading/src/main/res/values-vi/strings.xml` | `loading_error_server` |
| Sửa | `feature/loading/src/main/res/values-pt-rBR/strings.xml` | `loading_error_server` |
| Sửa | `feature/loading/src/main/res/values-b+es+419/strings.xml` | `loading_error_server` |
| Sửa | `feature/loading/src/main/res/values-hi/strings.xml` | `loading_error_server` |
| Sửa | `feature/loading/src/main/res/values-in/strings.xml` | `loading_error_server` |
| Sửa | `feature/loading/src/main/res/values-th/strings.xml` | `loading_error_server` |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingViewModelTest.kt` | `FakeInternetCheck`, tham số `internet` của `viewModel()`, 5 test mới |
| Tạo | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/data/InternetCheckTest.kt` | 5 test Robolectric |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenTest.kt` | 3 test mới, 7 test màn thấp với lời máy chủ, `assertRetryReachableOnShortScreen(cause)` |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingStringsTest.kt` | `loading_error_server` trong `ENGLISH`, `VIETNAMESE`; `NETWORK_WORDS`; test `serverGuidance_doesNotAskToCheckTheNetwork` |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenScreenshotTest.kt` | `failedServer_light`, `failedServer_small` |
| Tạo | `feature/loading/src/test/screenshots/LoadingScreen_failed-server_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/loading/src/test/screenshots/LoadingScreen_failed-server_small.png` | Ảnh chuẩn mới, khung thấp |
| Sửa | `docs/features/loading/design/review/` | `summary.md` và 4 ảnh `LoadingScreen_progress_light_compare.png`, `LoadingScreen_progress_dark_compare.png`, `LoadingScreen_failed_light_compare.png`, `LoadingScreen_failed_dark_compare.png`, chỉ khi `scripts/design-compare.sh loading` sinh lại khác; commit cùng PR, không phải điều kiện dừng (`docs/ops/design-workflow.md`). Ảnh chuẩn cũ không đổi, nên dự kiến không đổi |

Nơi đọc các file bị đổi (L-020): `grep -rln 'loading_error\|LoadingUiState\|LoadingViewModel(' scripts/ .github/ app/ core/ feature/ docs/ --include=*.kt --include=*.sh --include=*.py --include=*.txt --include=*.log --include=*.md --include=*.yml` (bỏ `build/`) chỉ thấy các tệp của `:feature:loading` ở trên, plan và ticket này, plan cũ của loading (T-001, T-003, T-006, T-008, T-010, chỉ là lịch sử) và báo cáo phiên thử `docs/test-sessions/TS-20261022-02.md` (bằng chứng, không đọc lại). Không tệp kịch bản emulator, không test `androidTest`, không script nào đọc chữ hay trạng thái của màn loading.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. `feature_loading_enabled` giữ nguyên cách dùng.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm. `loading_completed` giữ nguyên tham số; lý do lỗi chỉ ghi vào log (ADR-0019), không gửi đi.
- File R2: Không chạm.
- **Quyền truy cập:** không thêm: `ACCESS_NETWORK_STATE` (`ConnectivityManager.getNetworkCapabilities` và `activeNetwork` cần nó trên mọi bản từ API 26 tới 36; đã có trong `app/src/main/AndroidManifest.xml` và `core/network/src/main/AndroidManifest.xml`), `INTERNET` (plan chỉ nhắc khả năng mạng `NET_CAPABILITY_INTERNET`, không phải quyền; app đã có quyền mạng qua các SDK hiện có).
- **Thư viện và SDK:** Không thêm. `android.net.ConnectivityManager`, `NetworkCapabilities` thuộc Android SDK; test dùng `org.robolectric.shadows.ShadowNetworkCapabilities`, `ShadowConnectivityManager` của Robolectric đã có trên classpath test (`template.android.feature`).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `InternetCheckTest.androidCheck_readsTheActiveNetwork` dựa vào `ShadowConnectivityManager.setNetworkCapabilities` và mạng mặc định của Robolectric (lần đầu dùng trong dự án). Đã chạy thử qua (mục 4.1).
- **Điều kiện chuyển:** test này vẫn thất bại vì Robolectric (không có `activeNetwork`, shadow không đổi được capabilities) sau 2 lần sửa, trong khi 4 test của `isValidatedInternet()` qua.
- **Cách làm thay:** `AndroidInternetCheck` nhận thêm một tham số mặc định `capabilities: () -> NetworkCapabilities? = { manager.getNetworkCapabilities(manager.activeNetwork) }`; test truyền hàm trả `ShadowNetworkCapabilities` đã dựng. Tiêu chí và các test khác của mục 4 giữ nguyên. Cùng tệp `InternetCheck.kt` và `InternetCheckTest.kt` ở mục 3.2.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-5, có Internet đã xác nhận thì lỗi `SERVER` | Unit test | `LoadingViewModelTest.failureWithValidatedInternet_showsServerCause`: `internet.validated = true`, cấu hình thất bại, trạng thái `Failed(0f, LoadingFailure.SERVER)` |
| KB-2, chưa xác nhận thì lỗi `OFFLINE` | Unit test | `LoadingViewModelTest.failureWithoutValidatedInternet_showsOfflineCause`: catalog thất bại, `Failed(0.1f, LoadingFailure.OFFLINE)` (đối chứng, mục 4.1) |
| KB-5, chọn lại ở mỗi lần lỗi | Unit test | `LoadingViewModelTest.causeIsCheckedAgain_atEachFailure`: `OFFLINE`, rồi `validated = true`, "Thử lại", thất bại: `SERVER`; rồi `validated = false`, "Thử lại", thất bại: `OFFLINE` |
| KB-2, đọc mạng lỗi thì lời cũ, không dừng app | Unit test | `LoadingViewModelTest.internetCheckThrows_showsOfflineCause_andLogsIt`: `FakeInternetCheck.error = SecurityException(...)`, trạng thái `Failed(0f, OFFLINE)`, có log `WARN` chứa `cause=OFFLINE` |
| Log ghi lý do (ADR-0019) | Unit test | `LoadingViewModelTest.failureLog_namesTheCause`: có log `WARN` chứa `CATALOG` và `cause=SERVER`; `logsStart_eachTaskResult_retry_andFinish` giữ nguyên (8 dòng, cùng mức, không có đường dẫn) |
| Cách nhận "đã xác nhận" | Unit test (Robolectric) | `InternetCheckTest`: `noNetwork_isNotValidated` (`null`), `internetWithoutValidation_isNotValidated`, `validatedWithoutInternet_isNotValidated`, `validatedInternet_isValidated`, `androidCheck_readsTheActiveNetwork` (`ShadowConnectivityManager.setNetworkCapabilities` cho `activeNetwork`: có cả hai thì `true`, chỉ `INTERNET` thì `false`) |
| KB-5, lời máy chủ trên màn hình, không có lời mạng | Compose UI test | `LoadingScreenTest.failedServer_showsServerGuidance_notNetworkGuidance`: tiêu đề lỗi hiện, `loading_error_server` hiện, `loading_error` không có, nút "Thử lại" hiện |
| KB-2, lời mạng khi `OFFLINE` | Compose UI test | `LoadingScreenTest.failedOffline_showsNetworkGuidance_notServerGuidance`; `failed_showsErrorTitleGuidanceAndRetry_andRetryCallsOnRetry` giữ nguyên |
| KB-5, logo không nhảy khi hiện lời máy chủ | Compose UI test | `LoadingScreenTest.appName_staysInPlace_whenFailedServer` |
| KB-5, màn hình thấp (L-028), lời máy chủ ở 7 ngôn ngữ | Compose UI test khung `w360dp-h640dp-xxhdpi` | `LoadingScreenTest.failedServer_onShortScreen_retryReachable_{en,vi,hi,ptBR,es419,in,th}`: `assertRetryReachableOnShortScreen(LoadingFailure.SERVER)` có thêm `onNodeWithText(<lời theo cause>).assertExists()`, rồi như cũ: nút cuộn tới được, không bị đáy màn hình cắt, chân trang không chồng lên nút, bấm được |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `LoadingScreen_failed-server_small.png` (và `LoadingScreen_failed_small.png` cũ) |
| Giao diện lời máy chủ | Screenshot test | `LoadingScreen_failed-server_light.png`, khung thiết kế, tiếng Việt |
| Đúng thiết kế (khi có ảnh thiết kế) | Ảnh thiết kế của ticket khác (T-008): `LoadingScreen_progress_light`, `LoadingScreen_progress_dark`, `LoadingScreen_failed_light`, `LoadingScreen_failed_dark` | Test và ảnh chuẩn của T-008 giữ nguyên và phải qua; lệnh so ảnh thiết kế của feature sinh lại ảnh so sánh, dự kiến không đổi (mục 3.2, dòng cuối) |
| Chữ đúng bảng chữ của thiết kế, 7 ngôn ngữ | Unit test (Robolectric) | `LoadingStringsTest`: `vietnamese_matchesDesignTextTable` và `english_isDefault` có `loading_error_server`; `everyReleaseLanguage_isTranslated` tự phủ chuỗi mới (đọc khóa của `ENGLISH`) |
| Lời máy chủ không bảo kiểm tra mạng (L-077) | Unit test (Robolectric) | `LoadingStringsTest.serverGuidance_doesNotAskToCheckTheNetwork` (test chặn, qua ngay trên khung rỗng): với mỗi ngôn ngữ, `loading_error_server` không chứa từ "mạng" của ngôn ngữ đó (`network`, `mạng`, `नेटवर्क`, `rede`, `red`, `jaringan`, `เครือข่าย`), và đối chứng: `loading_error` có chứa từ đó |
| KB-5 trên máy thật: máy có mạng, máy chủ file không tới được | Kiểm tra thủ công (máy thật: khác, cần máy có Internet mà máy chủ file không tới được, ví dụ chặn tên miền file tĩnh bằng DNS riêng; trình chạy kịch bản trên emulator chỉ tắt bật mạng, không chặn riêng một máy chủ) | Bản staging, xóa dữ liệu app, bật DNS riêng chặn tên miền R2 staging, mở app: màn loading hiện "Máy chủ chưa phản hồi..."; tắt Wi-Fi và dữ liệu di động, bấm "Thử lại": hiện "Kết nối đang gián đoạn..."; bỏ chặn, bật mạng, bấm "Thử lại": vào màn hình chính |
| Bản dịch 5 ngôn ngữ đúng nghĩa | Kiểm tra thủ công (người: người biết tiếng đọc `loading_error_server` của `hi`, `pt-BR`, `es-419`, `in`, `th`) | Đọc 5 chuỗi ở mục 3.1, sửa bằng PR riêng nếu cần, trước khi phát hành production |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-062); `scripts/plan-check.py` báo lỗi dòng thiếu khi plan còn `draft` (quy tắc 11).

**Ngoại lệ viết test trước:** bản dịch 5 ngôn ngữ (nghĩa của chữ chỉ người kiểm được; test chỉ kiểm có dịch, khác tiếng Anh và không bảo kiểm tra mạng), và hai ảnh chuẩn mới (chỉ ghi được sau khi có code, bước 4).

### 4.1. Chạy thử

- **Đã chạy thử** (2026-10-26, worktree tạm từ `origin/main` `3bf61da`, không commit; L-029, L-075): (a) khung rỗng đúng như đoạn cuối mục 3.1 cùng mọi test của mục 4; (b) bản đầy đủ của mục 3.1 thay khung rỗng, cùng các test đó. Bản vá tạm giữ ngoài repo, không commit.
- **Lệnh:** (a), (b): `./gradlew :feature:loading:testDebugUnitTest --continue`; (b) thêm `scripts/check.sh` (E là 2: `ShadowNetworkCapabilities`, `ShadowConnectivityManager` lần đầu dùng trong dự án).
- **Kết quả thật của (a), khung rỗng** (chạy lại sau rà soát độc lập với chữ giữ chỗ `Server guidance pending`): 78 test của module, 18 thất bại, các test cũ đều qua:
  - `LoadingViewModelTest.failureWithValidatedInternet_showsServerCause`, `causeIsCheckedAgain_atEachFailure`: **thất bại đúng lý do**, `expected:<Failed(progress=0.0, cause=SERVER)> but was:<Failed(progress=0.0, cause=OFFLINE)>`.
  - `LoadingViewModelTest.internetCheckThrows_showsOfflineCause_andLogsIt`, `failureLog_namesTheCause`: **thất bại đúng lý do**, `AssertionError` ở dòng tìm log có `cause=` (khung rỗng chưa ghi lý do).
  - `LoadingViewModelTest.failureWithoutValidatedInternet_showsOfflineCause`: **đối chứng, qua**.
  - `InternetCheckTest.validatedInternet_isValidated`, `androidCheck_readsTheActiveNetwork`: **thất bại đúng lý do**, `AssertionError` ở `assertTrue` (khung rỗng trả `false`). `noNetwork_isNotValidated`, `internetWithoutValidation_isNotValidated`, `validatedWithoutInternet_isNotValidated`: **đối chứng, qua**.
  - `LoadingScreenTest.failedServer_showsServerGuidance_notNetworkGuidance`, `appName_staysInPlace_whenFailedServer`: **thất bại đúng lý do**, `Assert failed: The component with Text ... contains 'Server guidance pending'` không hiện (màn hình chưa đổi lời theo lý do).
  - `LoadingScreenTest.failedServer_onShortScreen_retryReachable_{en,vi,hi,ptBR,es419,in,th}`: **thất bại đúng lý do** cả 7, `assertExists` không thấy lời máy chủ.
  - `LoadingScreenTest.failedOffline_showsNetworkGuidance_notServerGuidance`: **đối chứng, qua**.
  - `LoadingStringsTest.vietnamese_matchesDesignTextTable`: **thất bại đúng lý do**, `vi loading_error_server expected:<[Máy chủ chưa phản hồi. Vui lòng thử lại sau ít phút.]> but was:<[Server guid...`. `english_isDefault`: **thất bại đúng lý do**, `en loading_error_server expected:<[Our server isn't responding right now...`. `everyReleaseLanguage_isTranslated`: **thất bại đúng lý do**, `hi loading_error_server is not translated. Actual: Server guidance pending`.
  - `LoadingStringsTest.serverGuidance_doesNotAskToCheckTheNetwork`: **test chặn, qua ngay** (chữ giữ chỗ không có từ chỉ mạng; phần đối chứng trên `loading_error` qua ở mọi ngôn ngữ). Nó chặn bản dịch của bước 4 bảo kiểm tra mạng.
  - `LoadingScreenScreenshotTest.failedServer_light`, `failedServer_small`: **qua**, không ghi ảnh (lệnh test không ghi, không so ảnh chưa có); ảnh chuẩn ghi ở bước 4 bằng `scripts/check.sh`.
  - Lần chạy đầu (trước rà soát) dùng chữ giữ chỗ chép y nguyên lời cũ: `failedOffline_...` thất bại vì hai lời trùng chữ, `_en` và `appName_staysInPlace_whenFailedServer` qua sai; đã đổi chữ giữ chỗ như trên.
- **Kết quả thật của (b), bản đầy đủ:** `./gradlew :feature:loading:testDebugUnitTest`: 78 test, **qua hết**, kể cả 7 test màn thấp với lời máy chủ (tiếng Hindi dài nhất vẫn để nút "Thử lại" trong màn hình và chân trang dưới nút) và `InternetCheckTest.androidCheck_readsTheActiveNetwork` (Robolectric có `activeNetwork`, `setNetworkCapabilities` đổi được kết quả; phương án dự phòng 3.4 chưa cần). `scripts/check.sh`: lần đầu **thất bại** chỉ ở `:feature:loading:ktlintMainSourceSetCheck` và `ktlintTestSourceSetCheck` (định dạng của `isValidatedInternet()`, `serverGuidance_doesNotAskToCheckTheNetwork` và một dòng quá 120 ký tự của `ENGLISH`; mọi task khác qua); sửa bằng `./gradlew :feature:loading:ktlintFormat` và xuống dòng sau `R.string.loading_error_server to` trong `ENGLISH`; lần hai **qua**: `Cổng chất lượng: QUA`, 1087 test JVM, 0 lỗi, `./feature/loading: 78 test, 0 lỗi`, 97 ảnh screenshot. `check.sh` tự ghi hai ảnh `LoadingScreen_failed-server_*.png`: đã mở `_small`, lời máy chủ hiện đủ hai dòng, nút "Thử lại" và chân trang không bị che (mục 8, câu 8). Hai ảnh tạm đã xóa; phiên ghi lại ở bước 4.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có. Không chạm module core, quyền, SDK, sự kiện phân tích hay R2.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Nhiều phần trong một module (trạng thái, ViewModel, màn hình, DI, 7 tệp chữ), đổi giao diện công khai của `LoadingUiState.Failed` và hàm khởi tạo `LoadingViewModel` |
| B. Độ rõ của yêu cầu | 2 | Spec đang `changed`, chờ xác nhận câu hỏi 11; cách nhận "có Internet" (mục 8, câu 2) ảnh hưởng tới cách làm. Còn 1 sau khi người trả lời |
| C. Khả năng kiểm chứng tự động | 1 | Mọi tiêu chí có test JVM; tình huống thật (máy chủ không tới được khi có mạng) kiểm trên máy thật |
| D. Ảnh hưởng chéo | 0 | Chỉ `:feature:loading`; không file nào khác đọc trạng thái hay chữ của màn loading |
| E. Độ mới của kỹ thuật | 2 | Đọc `NetworkCapabilities` trong feature là biến thể của `:core:network`, nhưng shadow mạng của Robolectric lần đầu dùng trong dự án; đã chạy thử |
| **Tổng** | **7** | **Mức: Trung bình** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: không. Đọc `ConnectivityManager` trong `:feature:loading` thay vì mở `ConnectivitySource` (đang `internal`) của `:core:network`, để không chạm module core (mục 8, câu 5).
- Task khác trong sprint có thể xung đột: không thấy. Các ticket khác của phiên sau (trình chạy kịch bản, test bố cục bản dịch, khóa hướng dọc) không chạm `feature/loading/`. Nếu ticket khóa hướng dọc đổi `MainActivity` hay manifest thì vẫn không giao tệp với plan này.
- Đề xuất thứ tự chạy: độc lập, chạy ở vị trí nào cũng được; nên trước các task chạm `scripts/` để `device-session` cuối phiên chạy trên bản đã có lời mới (không kịch bản nào đọc lời này).

## 7. Điều kiện dừng riêng

- Một test cũ của `:feature:loading` (ngoài các test mục 3.2 ghi sửa) thất bại sau bước 4: dừng, không sửa test cũ, vì plan không định đổi hành vi cũ.
- Ảnh chuẩn cũ của `LoadingScreenScreenshotTest` đổi: dừng (plan không đổi bố cục).

## 8. Câu hỏi cho người duyệt

1. **Chữ của lời mới, 7 ngôn ngữ (mục 3.1, bảng ở bước 6)?** Tiếng Việt "Máy chủ chưa phản hồi. Vui lòng thử lại sau ít phút." theo FB-20261022-03. Đề xuất của AI: dùng bảng ở mục 3.1; người biết tiếng duyệt 5 bản dịch trước khi phát hành production (như T-050, T-051). Câu hỏi này đi cùng câu hỏi 11 của spec loading.
2. **Thế nào là "máy có Internet"?** Đề xuất của AI: mạng mặc định có `INTERNET` và `VALIDATED` (hệ thống đã kiểm mạng ra được Internet). Phương án khác: chỉ cần `INTERNET`, như hộp thoại mất mạng (plan T-011, câu 1); khi đó Wi-Fi chưa đăng nhập hay không ra Internet sẽ hiện lời "máy chủ chưa phản hồi", sai. Máy chưa kịp kiểm (vừa nối mạng) thì với đề xuất sẽ hiện lời cũ "kiểm tra mạng", là lời an toàn.
3. **Thiết kế:** đề xuất của AI: không dựng frame Figma riêng (cùng bố cục với `LoadingScreen_failed_*`, chỉ đổi một dòng chữ); PR này thêm một dòng vào bảng chữ của `design/README.md` (mục 2) và đổi tên dòng cũ thành "Lỗi, máy không có mạng hay mạng chưa được hệ thống xác nhận". Người duyệt thiết kế (Nhật) đồng ý thì giữ; muốn có frame thì dựng `LoadingScreen_failed-server_light` trên Figma, xuất lại, và plan đổi screenshot test `failedServer_light` thành ảnh có thiết kế (cùng tên).
4. **Bấm "Thử lại" liên tục (4 lần trong 5 giây ở FB-20261022-03):** đề xuất của AI: không đổi hành vi trong task này. Spec KB-2 bảo bấm là làm lại ngay; lời mới đã bảo "thử lại sau ít phút", và mỗi lần thử chỉ tải phần còn thiếu, chờ tối đa 15 giây. Phương án khác (cần đổi spec, ticket riêng): sau một lần lỗi `SERVER`, nút "Thử lại" tắt 5 giây kèm đếm ngược.
5. **Có nên đưa phần đọc "có Internet" vào `:core:network`?** Đề xuất của AI: chưa, vì chỉ màn loading cần và chạm module core là điểm chặn (rủi ro cao). Nếu sau này feature khác cần (camera, thư viện báo lỗi tải), đưa `InternetCheck` vào `:core:network` bằng ticket riêng và đề xuất về dự án mẫu.
6. **Đề xuất về dự án mẫu:** màn loading là feature mẫu, nên lời báo thứ hai có ích cho mọi app. Đề xuất của AI: sau khi task merge, mở issue `from-app` về dự án mẫu (`docs/ops/from-app.md`), kèm PR của task.
7. **Spec loading đang `changed` (KB-2, KB-5, câu hỏi 11):** người phụ trách sản phẩm (Nhật) xác nhận lại spec trước khi duyệt plan; đổi ý ở câu 1, 2 hay 4 thì plan sửa theo.

8. **Xuống dòng của lời tiếng Việt:** ở ảnh chạy thử `LoadingScreen_failed-server_small.png` (khung 360), lời xuống dòng thành "...thử lại sau ít / phút.", chữ "phút." đứng một mình ở dòng hai. Đề xuất của AI: chấp nhận (chữ căn giữa, đủ nghĩa, nút không bị che). Phương án khác: viết `ít&#160;phút` (khoảng trắng không ngắt) trong `values-vi`; khi đó `VIETNAMESE` của `LoadingStringsTest` và bảng chữ ghi cùng ký tự đó.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-26, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261026-02, phiên bản plan 1

- **Kết quả:** đã merge (PR #160, commit merge `7468578`, 2026-10-26 05:09 UTC).
- **Nhánh, PR:** `claude/T-057-loi-bao-may-chu`, https://github.com/VGL-Studio/camera-pose/pull/160.
- **Đã làm:** bước 3 đến 5 do một phiên con làm trong worktree riêng (auto-run.md, mục Giao bước 3 đến 5). Mục 3.1 đủ 7 điểm: `LoadingFailure`, `InternetCheck.kt`, `@Provides`, `failureCause()` và log `cause=`, lời hướng dẫn theo `cause` và preview, `loading_error_server` ở 7 ngôn ngữ đúng bảng, hai ảnh chuẩn mới do `scripts/check.sh` ghi (không dùng `recordRoborazziDebug`).
- **Test viết trước:** commit `6f33fc3`, khung rỗng đúng đoạn cuối mục 3.1 (chữ giữ chỗ `Server guidance pending`). 78 test của module, đúng 18 thất bại đúng lý do như mục 4.1 (a) (ví dụ `expected:<Failed(progress=0.0, cause=SERVER)> but was:<Failed(progress=0.0, cause=OFFLINE)>`; 7 test màn thấp không thấy lời máy chủ); đối chứng (`failureWithoutValidatedInternet_showsOfflineCause`, ba test "không xác nhận" của `InternetCheckTest`, `failedOffline_showsNetworkGuidance_notServerGuidance`) và test chặn `serverGuidance_doesNotAskToCheckTheNetwork` qua. Không test nào phải sửa. Sau code (`6e80d99`): 78 qua.
- **Ảnh chuẩn mới:** `LoadingScreen_failed-server_light.png`, `LoadingScreen_failed-server_small.png` (`630a7a0`); đã mở: lời đủ hai dòng ("...sau ít / phút.", mục 8 câu 8 đã chấp nhận), nút "Thử lại" và chân trang không bị cắt hay chồng. Không ảnh chuẩn cũ nào đổi.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 4 ghi chú: tiếng Thái "ในอีกสักครู่" yếu hơn "sau ít phút" (đề xuất "ในอีกไม่กี่นาที" khi người biết tiếng duyệt); `NETWORK_WORDS` chỉ chặn một từ mỗi ngôn ngữ; dòng bảng chữ ở `design/README.md` còn ghi "chờ duyệt cùng spec, câu hỏi 11" dù spec đã `confirmed`; đọc mạng đồng bộ trên luồng chính, chỉ khi lỗi.
- **Kết quả CI:** `scripts/check.sh` QUA (1092 test JVM, 0 lỗi, 97 ảnh). `scripts/design-compare.sh loading` không đổi (2,7%, 1,6%). `pr-check` xanh vòng đầu (lần chạy 40271831802).
- **Lệch so với plan:** Không có. Commit test và commit code được đẩy cùng một lần, sau khi `main-guard` của T-055 xanh (L-076).
- **Việc còn lại:** kiểm thủ công trên máy thật (DNS chặn tên miền R2 staging); người biết tiếng duyệt 5 bản dịch.
- **Cần người xem:** hai ảnh chuẩn mới; bản dịch tiếng Thái.
