---
ticket: T-001
title: Màn hình loading có thanh tiến trình khi mở app
status: done
version: 2
risk_score:
risk_level: high
blocking_points: [analytics, core]
run_order: 1
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-10-15
---

# PLAN T-001: MÀN HÌNH LOADING CÓ THANH TIẾN TRÌNH KHI MỞ APP

## 1. Tóm tắt

**Phiên bản 2 (2026-10-15)**, sau khi RUN-20261015-01 dừng ở bước 8 (mục 10). Thay đổi so với phiên bản 1:

- `AppLaunchTest` chỉ chờ màn hình mẫu có trong cây giao diện, không kiểm tra màn hình loading thoáng qua và không dùng `assertIsDisplayed`, vì hộp thoại đồng ý quảng cáo phủ lên app mới cài (CLAUDE.md, bước 10).
- ViewModel và `RemoteConfigRefresher` ghi log qua `AppLogger` (ADR-0019), có test.
- Ghi rõ cách hiểu về lúc báo lỗi ở KB-2 (câu hỏi 7).
- Cách tiếp tục: làm lại từ đầu. PR #13 đã đóng, nhánh đã xóa.

Tạo module `:feature:loading` theo đúng mẫu `:feature:sample`. ViewModel chạy song song bốn phần việc (tải Remote Config qua `RemoteConfigRefresher` mới của `:core:config`, tải ba file qua `RemoteAssets`), mỗi phần xong cộng 25% tiến trình, giữ màn hình ít nhất 2 giây rồi báo cho `:app` chuyển sang màn hình mẫu. Kiểm chứng bằng unit test cho ViewModel, repository và `RemoteConfigRefresher`, test giao diện và screenshot bằng Robolectric, test khởi động app trên emulator trong CI, và kiểm tra thủ công trên bản staging.

## 2. Hiểu yêu cầu

Ticket xử lý mọi kịch bản trong `features/loading/spec.md` (đã xác nhận ngày 2026-10-15):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Màn hình loading hiện đầu tiên, có thanh tiến trình, phần trăm, dòng chữ | Màn hình loading là `startDestination` của `AppNavHost`. Thanh ngang `LinearProgressIndicator` có tiến trình xác định |
| KB-1 | Mỗi phần việc xong tăng 25%, không lùi | Tiến trình bằng số phần việc đã xong chia 4. Phần việc đã xong không bao giờ bị tính lại |
| KB-1 | Xong cả bốn và đủ 2 giây thì tự chuyển | Mốc 2 giây tính từ lúc ViewModel bắt đầu chạy |
| KB-1 | Xong trước 2 giây thì đứng ở 100% cho tới đủ 2 giây | Trạng thái `InProgress(1f)` trong lúc chờ |
| KB-1 | Ghi `loading_completed` một lần khi chuyển | Ghi ngay trước khi chuyển sang `Finished`, chỉ trong ViewModel |
| KB-1 | Bấm quay lại ở màn hình chính thì app đóng | Điều hướng sang màn hình mẫu với `popUpTo<LoadingDestination> { inclusive = true }` |
| KB-1 | Xoay máy không chạy lại, không tính lại 2 giây | ViewModel sống qua đổi cấu hình, việc chạy bắt đầu trong `init` |
| KB-2 | Một phần việc không xong thì báo lỗi, có "Thử lại", không vào màn hình chính | Báo lỗi khi mọi phần việc đang chạy đã kết thúc (thành công, thất bại hay hết giờ) mà còn phần thất bại: trạng thái `Failed(progress)`. Lâu nhất 15 giây sau khi bắt đầu. Không báo lỗi giữa chừng khi các phần khác còn chạy, để "Thử lại" không chạy chồng (câu hỏi 7) |
| KB-2 | "Thử lại" chỉ làm lại phần chưa xong, tiến trình tiếp tục | Giữ tập phần việc đã xong trong ViewModel |
| KB-2 | Có mạng lại, bấm "Thử lại" thì vào màn hình chính | Như KB-1 sau khi thử lại thành công |
| KB-2 | Cấu hình quá 10 giây, file quá 15 giây thì báo lỗi | 10 giây trong `RemoteConfigRefresher`; 15 giây là thời gian chờ sẵn có của `HttpAssetDownloader` (ADR-0015), không đổi |
| KB-3 | File đã có trên máy thì xong ngay | Hành vi sẵn có của `RemoteAssets` (ADR-0015) |
| KB-3 | Không tải được cấu hình thì báo lỗi, kể cả khi đã có cấu hình cũ | `refresh()` luôn tải từ máy chủ, không dùng bộ nhớ đệm (ADR-0018) |
| KB-4 | Cờ tắt thì mở thẳng màn hình chính, không ghi sự kiện | ViewModel đọc giá trị cờ đầu tiên; tắt thì chuyển ngay sang `Finished`, không chạy phần việc nào, không ghi sự kiện |
| Mục 9, câu 4 | Cấu hình Firebase giả: tải cấu hình được tính là xong ngay | `RemoteConfigRefresher` nhận ra project giả `fake-staging-project` và trả về thành công ngay |

Thiết kế tham chiếu: không có thiết kế riêng, dùng thành phần Material 3 có sẵn trong `AppTheme`.

ADR liên quan: ADR-0001, ADR-0003, ADR-0012, ADR-0015, ADR-0018 (`proposed`, đi cùng plan này).

## 3. Giải pháp

### 3.1. Cách làm

Cách tiếp tục: làm lại từ đầu (auto-run.md, mục Tiếp tục task đã dừng). PR #13 của phiên bản 1 đã đóng, nhánh đã xóa; phiên làm đủ 10 bước trên nhánh mới `claude/T-001-loading`.

**`:core:config`**

- `RemoteConfigRefresher` (công khai): `suspend fun refresh(): Boolean`.
- `FirebaseRemoteConfigRefresher` (internal, gắn bằng `ConfigModule`):
  - Project giả (`FirebaseApp.getInstance().options.projectId == "fake-staging-project"`) thì trả `true` ngay.
  - Ngược lại gọi `remoteConfig.fetch(0)` rồi `activate()`, chờ bằng `suspendCancellableCoroutine` trên `Task` (không thêm thư viện), bọc trong `withTimeoutOrNull(10_000)`.
  - Kết quả: thành công thì `true`; `FirebaseRemoteConfigFetchThrottledException` thì `true` (ADR-0018); lỗi khác hoặc quá giờ thì `false`.
  - Phần quyết định tách thành một lớp internal nhận `isFakeConfig: () -> Boolean` và `fetchAndActivate: suspend () -> Unit` để unit test không cần Firebase.
  - Ghi log qua `AppLogger` (thẻ `RemoteConfig`): `warn` khi tải lỗi hoặc hết giờ, `info` khi bị Firebase giới hạn hoặc cấu hình giả. `:core:config` phụ thuộc thêm `:core:logging`.
- `FeatureFlag.LOADING("feature_loading_enabled")`, mặc định tắt.

**`:feature:loading`** (theo đúng 10 bước trong `CLAUDE.md`)

- `data/PreparationTask.kt`: `enum class PreparationTask { REMOTE_CONFIG, HELLO_FILE, CONTENT_FILE, GOODBYE_FILE }`.
- `data/PreparationRepository.kt`: `interface PreparationRepository { suspend fun run(task: PreparationTask): Boolean }`.
- `data/DefaultPreparationRepository.kt` (internal): `REMOTE_CONFIG` gọi `RemoteConfigRefresher.refresh()`; ba task file gọi `RemoteAssets.load(AssetPath("test/v1/hello.txt"))`, `test/v1/content.txt`, `test/v1/goodbye.txt`; `Ready` là `true`, `Unavailable` là `false`.
- `di/LoadingModule.kt`: gắn `PreparationRepository`; cung cấp `LoadingClock` (`fun interface`, trả `SystemClock.elapsedRealtime()`), để test dùng thời gian ảo.
- `LoadingUiState`: `InProgress(progress: Float)`, `Failed(progress: Float)`, `Finished`.
- `LoadingViewModel(repository, analytics, logger, featureFlags, clock)`:
  - `init`: đọc giá trị đầu tiên của `featureFlags.enabled(FeatureFlag.LOADING)`. Tắt thì `Finished` (KB-4). Bật thì ghi mốc bắt đầu và chạy.
  - Chạy: mọi phần việc chưa xong chạy song song (`async`). Mỗi phần thành công thêm vào tập đã xong và cập nhật `InProgress`.
  - Khi tất cả kết thúc: còn phần thất bại thì `Failed(progress)`. Đủ bốn phần thì `delay` phần còn thiếu của 2 giây, ghi sự kiện, chuyển `Finished`.
  - `onRetry()`: chỉ khi đang `Failed`; tăng `retryCount`, chạy lại phần chưa xong.
  - Log qua `AppLogger`, thẻ `Loading`: `info` khi bắt đầu (cờ bật) hoặc bỏ qua (cờ tắt); `info` khi một phần việc xong, `warn` khi một phần việc thất bại (tên phần việc, tên loại lỗi nếu có); `info` khi thử lại (lần thứ mấy) và khi xong (thời gian). Không ghi đường dẫn file hay nội dung.
  - Sự kiện: `AnalyticsEvent("loading_completed", mapOf("duration_ms" to ..., "retry_count" to ...))`, `duration_ms` tính từ mốc bắt đầu tới lúc chuyển.
  - Cờ được đọc một lần lúc mở màn hình, không theo dõi tiếp. Đây là chỗ khác với ghi chú "theo dõi, không đọc một lần" của `CLAUDE.md`, vì cờ chỉ quyết định có hiện màn hình loading lúc mở app hay không (xem câu hỏi 3).
- `LoadingScreen.kt`:
  - `LoadingRoute(onFinished)` nối ViewModel, gọi `onFinished()` một lần khi trạng thái là `Finished`.
  - `LoadingScreen(uiState, onRetry)` không trạng thái, có `testTag("loading_screen")`. Gồm thanh tiến trình, phần trăm, dòng chữ "Đang chuẩn bị". Khi `Failed`: thêm thông báo lỗi và nút "Thử lại".
  - Màu, chữ, khoảng cách lấy từ `MaterialTheme` và `Spacing`.
- `navigation/LoadingNavigation.kt`: `@Serializable data object LoadingDestination`, `fun NavGraphBuilder.loadingScreen(onFinished: () -> Unit)`.

**`:app`**

- `AppNavHost`: `startDestination = LoadingDestination`; `loadingScreen(onFinished = { navController.navigate(SampleDestination) { popUpTo<LoadingDestination> { inclusive = true } } })`.
- `StagingFeatureFlagsModule`: bật `FeatureFlag.LOADING` cho staging.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

Đường dẫn gói viết tắt: `.../config` là `com/vglstudio/camera/pose/line/guide/core/config`, `.../loading` là `com/vglstudio/camera/pose/line/guide/feature/loading`.

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/config/src/main/kotlin/.../config/RemoteConfigRefresher.kt` | Interface, lớp quyết định có thể test, bản cài đặt Firebase |
| Sửa | `core/config/build.gradle.kts` | Thêm `implementation(project(":core:logging"))` |
| Sửa | `core/config/src/main/kotlin/.../config/FeatureFlag.kt` | Thêm `LOADING("feature_loading_enabled")` |
| Sửa | `core/config/src/main/kotlin/.../config/di/ConfigModule.kt` | Gắn `RemoteConfigRefresher` |
| Tạo | `core/config/src/test/kotlin/.../config/RemoteConfigRefresherTest.kt` | Test mục 4 |
| Sửa | `settings.gradle.kts` | `include(":feature:loading")` |
| Tạo | `feature/loading/build.gradle.kts` | Plugin `template.android.feature`, `namespace` |
| Tạo | `feature/loading/consumer-rules.pro` | Rỗng, như `:feature:sample` |
| Tạo | `feature/loading/src/main/kotlin/.../loading/LoadingUiState.kt` | Trạng thái màn hình |
| Tạo | `feature/loading/src/main/kotlin/.../loading/LoadingViewModel.kt` | Như mục 3.1 |
| Tạo | `feature/loading/src/main/kotlin/.../loading/LoadingScreen.kt` | `LoadingRoute`, `LoadingScreen` |
| Tạo | `feature/loading/src/main/kotlin/.../loading/data/PreparationTask.kt` | Bốn phần việc |
| Tạo | `feature/loading/src/main/kotlin/.../loading/data/PreparationRepository.kt` | Interface |
| Tạo | `feature/loading/src/main/kotlin/.../loading/data/DefaultPreparationRepository.kt` | Cài đặt |
| Tạo | `feature/loading/src/main/kotlin/.../loading/di/LoadingModule.kt` | Hilt module, `LoadingClock` |
| Tạo | `feature/loading/src/main/kotlin/.../loading/navigation/LoadingNavigation.kt` | Đích điều hướng |
| Tạo | `feature/loading/src/main/res/values/strings.xml` | Chữ hiển thị |
| Tạo | `feature/loading/src/test/kotlin/.../loading/MainDispatcherRule.kt` | Chép từ `:feature:sample` |
| Tạo | `feature/loading/src/test/kotlin/.../loading/LoadingViewModelTest.kt` | Test mục 4 |
| Tạo | `feature/loading/src/test/kotlin/.../loading/data/DefaultPreparationRepositoryTest.kt` | Test mục 4 |
| Tạo | `feature/loading/src/test/kotlin/.../loading/LoadingScreenTest.kt` | Test giao diện bằng Robolectric |
| Tạo | `feature/loading/src/test/kotlin/.../loading/LoadingScreenScreenshotTest.kt` | Screenshot test |
| Tạo | `feature/loading/src/test/resources/robolectric.properties` | `sdk=35`, như `:feature:sample` |
| Tạo | `feature/loading/src/test/screenshots/LoadingScreen_progress_light.png` | Ảnh chuẩn mới, ghi bằng `scripts/check.sh` |
| Tạo | `feature/loading/src/test/screenshots/LoadingScreen_progress_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/loading/src/test/screenshots/LoadingScreen_failed_light.png` | Ảnh chuẩn mới |
| Sửa | `app/build.gradle.kts` | `implementation(project(":feature:loading"))` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/AppNavHost.kt` | Màn hình bắt đầu là loading |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingFeatureFlagsModule.kt` | Bật `LOADING` cho staging |
| Sửa | `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt` | Xem mục 4 |

Nhật ký plan, trạng thái plan và ticket, báo cáo phiên không nằm trong PR của task; chúng đi qua nhánh báo cáo của phiên (auto-run.md, mục 4).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: `feature_loading_enabled`, mặc định tắt, bật cho staging qua `StagingFeatureFlagsModule`.
- Quảng cáo: không chạm.
- Sự kiện phân tích: `loading_completed` với `duration_ms`, `retry_count`, đúng mục 6 của spec.
- File R2: đọc `test/v1/hello.txt`, `test/v1/content.txt`, `test/v1/goodbye.txt` qua `RemoteAssets`. Không đổi cách đặt tên hay đường dẫn.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, 1 | Unit test | `LoadingViewModelTest.flagOn_startsInProgressAtZero` |
| KB-1, 2 | Unit test | `LoadingViewModelTest.eachTaskDone_addsAQuarter_andNeverDecreases` |
| KB-1, 3 | Unit test | `LoadingViewModelTest.allTasksDoneAfterTwoSeconds_finishesAtOnce` |
| KB-1, 4 | Unit test | `LoadingViewModelTest.allTasksDoneBeforeTwoSeconds_staysAtFullUntilTwoSeconds` |
| KB-1, 5 | Unit test | `LoadingViewModelTest.finished_logsLoadingCompletedOnce_withDurationAndRetryCount` |
| KB-1, 1 (giao diện) | Compose UI test | `LoadingScreenTest.inProgress_showsProgressBarPercentAndMessage` |
| KB-1, 6 | Kiểm tra thủ công | Trên bản staging: vào màn hình mẫu, bấm quay lại, app đóng |
| KB-1, xoay máy | Kiểm tra thủ công | Trên bản staging: xoay máy khi đang loading, tiến trình không về 0 |
| KB-1, chuyển màn hình | Test trên emulator (CI) | `AppLaunchTest.appStarts_andReachesStartScreen`: `waitUntil` tối đa 30 giây tới khi `onAllNodesWithTag("sample_screen").fetchSemanticsNodes()` không rỗng. Không kiểm tra `loading_screen`, không dùng `assertIsDisplayed` |
| Log của loading | Unit test | `LoadingViewModelTest.logsStart_eachTaskResult_retry_andFinish` (dùng `RecordingAppLogger`: thứ tự mức và thẻ `Loading`, không có đường dẫn file) |
| Log của cấu hình | Unit test | `RemoteConfigRefresherTest.fetchFails_logsWarning` |
| KB-2, 1 | Unit test | `LoadingViewModelTest.taskFails_showsFailedWithProgress_andDoesNotFinish` |
| KB-2, 1 (giao diện) | Compose UI test | `LoadingScreenTest.failed_showsErrorAndRetry_andRetryCallsOnRetry` |
| KB-2, 2 | Unit test | `LoadingViewModelTest.retry_runsOnlyUnfinishedTasks_andProgressContinues` |
| KB-2, 3 | Unit test | `LoadingViewModelTest.retrySucceeds_finishes_andCountsOneRetry` |
| KB-2, 4 (cấu hình) | Unit test | `RemoteConfigRefresherTest.fetchLongerThanTenSeconds_returnsFalse` |
| KB-2, 4 (file) | Test sẵn có | Thời gian chờ 15 giây của `HttpAssetDownloader` không đổi |
| KB-2 | Kiểm tra thủ công | Cài mới bản staging, bật chế độ máy bay, mở app: báo lỗi; tắt chế độ máy bay, bấm "Thử lại": vào màn hình mẫu |
| KB-3, 1 | Test sẵn có | Test bộ nhớ đệm của `:core:assets` |
| KB-3, 2 | Unit test | `RemoteConfigRefresherTest.fetchFails_returnsFalse` và `LoadingViewModelTest.taskFails_showsFailedWithProgress_andDoesNotFinish` |
| KB-3 | Kiểm tra thủ công | Mở bản staging một lần có mạng, tắt mạng, mở lại: báo lỗi ở 75% |
| KB-4 | Unit test | `LoadingViewModelTest.flagOff_finishesAtOnce_withoutRunningTasksOrEvent` |
| Cấu hình giả | Unit test | `RemoteConfigRefresherTest.fakeConfig_returnsTrueWithoutFetching` |
| Bị Firebase giới hạn | Unit test | `RemoteConfigRefresherTest.fetchThrottled_returnsTrue` |
| Tải thành công | Unit test | `RemoteConfigRefresherTest.fetchSucceeds_returnsTrue` |
| Đúng ba file | Unit test | `DefaultPreparationRepositoryTest.fileTasks_loadTheThreeVersionedPaths`, `unavailableFile_returnsFalse`, `remoteConfigTask_usesRefresher` |
| Cờ mặc định tắt | Test sẵn có | Test mọi cờ mặc định tắt trong `:core:config` |
| Giao diện | Screenshot test | `LoadingScreenScreenshotTest`: `progress_light` (50%), `progress_dark` (50%), `failed_light` (75%) |

**Ngoại lệ viết test trước:** `strings.xml` và các file khai báo (`build.gradle.kts`, `consumer-rules.pro`, `robolectric.properties`, `settings.gradle.kts`, Hilt module) không có test riêng; chúng được kiểm qua build và các test trên. Ba ảnh chuẩn screenshot được ghi sau khi có code, người xem ảnh khi đọc báo cáo phiên.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 4 (sự kiện phân tích mới `loading_completed`), điểm 7 (module core dùng chung `:core:config`). Rủi ro cao, bắt buộc duyệt chéo.

Bảng điểm dưới đây chỉ để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module: `:core:config`, `:feature:loading`, `:app` |
| B. Độ rõ của yêu cầu | 0 | Spec đã xác nhận, mọi câu hỏi đã có câu trả lời |
| C. Khả năng kiểm chứng tự động | 1 | Logic có unit test; xoay máy, nút quay lại, mất mạng thật kiểm tra thủ công |
| D. Ảnh hưởng chéo | 3 | Đổi màn hình bắt đầu của app; khi cờ bật, app cần mạng mỗi lần mở |
| E. Độ mới của kỹ thuật | 2 | Chờ `Task` của Firebase bằng coroutine và giới hạn thời gian chưa có trong dự án |
| **Tổng** | 9 | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `:core:config`: thêm interface và cờ, không đổi hành vi cũ.
  - `AppNavHost`: đổi màn hình bắt đầu.
  - `AppLaunchTest`: đổi cách chờ màn hình mẫu.
- Task khác trong sprint có thể xung đột: không có, đây là task duy nhất.
- Đề xuất thứ tự chạy: task đầu tiên và duy nhất của RUN-20261015-01.

## 7. Điều kiện dừng riêng

1. Cần thêm thư viện (ví dụ `kotlinx-coroutines-play-services`) hoặc sửa `gradle/libs.versions.toml`, `build-logic/`.
2. `AppLaunchTest` trên CI thất bại sau một lần chạy lại. Trước khi kết luận, đọc bước "Chi tiết test trên emulator" của job (auto-run.md, bước 8) và ghi lý do vào nhật ký.
3. Firebase không cho tải với khoảng tối thiểu bằng 0 trên bản staging, hoặc báo bị giới hạn ngay từ lần mở đầu.
4. Test sẵn có của `:feature:sample` hoặc `:core:config` thất bại sau khi đổi màn hình bắt đầu.

## 8. Câu hỏi cho người duyệt

1. **Duyệt chéo:** plan rủi ro cao cần dev còn lại duyệt chéo (risk-scoring.md, mục 3). Ai duyệt chéo plan này?
   - Đề xuất của AI: dev còn lại của nhóm. Nếu trong giai đoạn dự án mẫu anh Nhật duyệt một mình thì ghi rõ ở mục 9.
   - Trả lời: Nhật duyệt (vì đây chỉ là dự án mẫu)
2. **Firebase giới hạn số lần tải:** khi Firebase báo bị giới hạn, `refresh()` coi là thành công (ADR-0018).
   - Đề xuất của AI: đồng ý. Nếu coi là lỗi, người mở app nhiều lần trong thời gian ngắn có thể bị chặn dù có mạng.
   - Trả lời: Đồng ý theo đề xuất của AI.
3. **Cờ đọc một lần:** ViewModel đọc cờ `feature_loading_enabled` một lần khi mở màn hình, không theo dõi tiếp như `CLAUDE.md` hướng dẫn.
   - Đề xuất của AI: đồng ý, vì cờ chỉ quyết định có hiện loading lúc mở app hay không; đổi cờ giữa chừng không có ý nghĩa.
   - Trả lời: Đồng ý.
4. **Bốn phần việc chạy song song:** spec không nói thứ tự.
   - Đề xuất của AI: chạy song song cho nhanh. Thanh tiến trình vẫn tăng từng nấc 25%.
   - Trả ời: Chạy song song.
5. **Test trên emulator cần mạng tới R2 staging:** `AppLaunchTest` sẽ đi qua màn hình loading thật.
   - Đề xuất của AI: chấp nhận. CI đã có mạng; nếu lỗi do mạng thì chạy lại một lần, lỗi tiếp thì phiên dừng task (điều kiện dừng 2).
   - Trả lời: Chấp nhận.
6. **Cỡ task L trong một lượt:** risk-scoring khuyên cân nhắc tách task cỡ L.
   - Đề xuất của AI: giữ một task. Các phần phụ thuộc chặt vào nhau, và đây là phiên tự chạy đầu tiên, một task dễ theo dõi hơn.
   - Trả lời: Giữ một task.

Câu hỏi của phiên bản 2:

7. **Lúc báo lỗi ở KB-2:** spec viết "khi một phần việc không xong thì báo lỗi". Phiên bản 1 báo lỗi khi mọi phần việc đang chạy đã kết thúc (rà soát độc lập của RUN-20261015-01 ghi nhận điểm khác này).
   - Đề xuất của AI: giữ cách của phiên bản 1. Chậm nhất 15 giây sau khi bắt đầu là có thông báo, và "Thử lại" không chạy chồng lên phần việc còn đang chạy.
   - Trả lời: giữ như cách của phiên 1.
8. **Duyệt chéo phiên bản 2:** như phiên bản 1?
   - Đề xuất của AI: Nhật duyệt và duyệt chéo, vì đây là dự án mẫu.
   - Trả lời: Nhật duyệt và duyệt chéo.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (vì đây là tính năng mẫu)
- **Ghi chú:** 

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (vì là đang ở trên app template)
- **Ghi chú:** 

## 10. Nhật ký thực hiện

<!-- AI ghi sau mỗi lần chạy. Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261015-01, phiên bản plan 1

- **Kết quả:** gửi để merge bằng `scripts/merge-if-green.sh` sau khi CI xanh (nhật ký ghi trước khi merge vì phiên không được đẩy thẳng lên nhánh chính)
- **Nhánh, PR:** `claude/trusting-ritchie-kr8pph` (nhánh do môi trường chỉ định, thay cho `claude/T-001-<tu-khoa>`), PR ghi trong báo cáo phiên
- **Đã làm:** `RemoteConfigRefresher` trong `:core:config`, cờ `LOADING`, module `:feature:loading` (repository, ViewModel, màn hình, điều hướng), `AppNavHost` bắt đầu từ loading, bật cờ cho staging, cập nhật `AppLaunchTest`
- **Test viết trước:** 20 test (6 `RemoteConfigRefresherTest`, 9 `LoadingViewModelTest`, 3 `DefaultPreparationRepositoryTest`, 2 `LoadingScreenTest`), đã thất bại đúng lý do (`NotImplementedError` hoặc khung màn hình chưa vẽ). Ba screenshot test chưa có ảnh chuẩn nên chưa thất bại, đúng ngoại lệ ở mục 4. `AppLaunchTest` chỉ chạy trong CI. Sau rà soát thêm 1 test: `taskThrows_isTreatedAsFailure_andShowsFailed`, đã thất bại trước khi sửa code
- **Rà soát độc lập:** 0 chặn. Đã xử lý 2 điểm nên sửa: Task Firebase bị hủy làm màn hình kẹt, và lỗi bất ngờ từ `repository.run` làm dừng app. Còn để người xem: (1) lúc cờ tắt màn hình vẽ một khung 0% rồi mới chuyển; (2) khi Firebase báo bị giới hạn, `refresh()` trả thành công mà không gọi `activate()`, và `FirebaseRemoteFlagSource` cũng tải lúc khởi động nên quota có thể cạn nhanh hơn (điều kiện dừng 3, cần theo dõi trên staging); (3) bản cài mới ở production, cờ mặc định tắt nên lần mở đầu bỏ qua loading (hệ quả của ADR-0003, ADR-0012); (4) spec KB-2 nói báo lỗi ngay khi một phần việc không xong, plan và code chỉ báo khi mọi phần việc đã kết thúc (plan đã duyệt như vậy); (5) `FirebaseRemoteConfigRefresher` (gọi `fetch(0)`, nhận ra project giả) và nút quay lại không có test tự động
- **Kết quả CI:** `scripts/check.sh` cục bộ qua (module, ktlint, unit test, screenshot, lint, build staging). CI trên GitHub xem PR
- **Lệch so với plan:** Hai chi tiết của test được sửa sau khi viết code, không đổi điều test kiểm tra: bộ thu trạng thái của `eachTaskDone_...` dùng `UnconfinedTestDispatcher` (bản đầu không chạy bộ thu); `fetchThrottled_returnsTrue` dùng `mockk(relaxed = true)` vì lớp ngoại lệ thật của Firebase gọi `TextUtils` của Android, không có trong test JVM. `RemoteConfigRefreshPolicy` có thêm tham số `onFailure` để ghi log mà không gọi `android.util.Log` trong test JVM. Nhánh làm việc do môi trường chỉ định
- **Việc còn lại:** kiểm tra thủ công trên bản staging theo mục 4 (nút quay lại, xoay máy, máy bay, mất mạng lúc mở lại)
- **Cần người xem:** ba ảnh chuẩn mới trong `feature/loading/src/test/screenshots/`; các điểm ghi chú ở trên

### RUN-20261015-01, phiên bản plan 1 (cập nhật, thay cho mục ngay trên)

- **Kết quả:** dừng ở bước 8 (CI). Không merge. Mục ngay trên ghi trước khi có kết quả CI, nay không còn đúng.
- **Nhánh, PR:** `claude/trusting-ritchie-kr8pph`, PR #13 (giữ mở, không merge)
- **Lý do dừng:** job `ui-test` thất bại ba lần liên tiếp (một lần chạy đầu, một lần chạy lại, một lần sau khi thêm chẩn đoán) ở cùng khẳng định: `AppLaunchTest.appStarts_showsLoadingThenStartScreen` báo `loading_screen` không hiển thị. Lần cuối ghi `App age: 13289 ms` lúc khẳng định chạy, nghĩa là màn hình loading (tối thiểu 2 giây) đã qua trước khi test kịp kiểm tra. Emulator của CI chậm, test khẳng định một màn hình thoáng qua nên không ổn định. `build-and-test` xanh, gồm mọi unit test và screenshot test của loading.
- **Đã thử:** chạy lại job một lần (vẫn đỏ); thêm chẩn đoán vào thông báo lỗi (không đổi điều test kiểm tra). Chưa sửa điều test kiểm tra vì đó là điều kiện dừng 8 và CLAUDE.md cấm làm test qua bằng cách sửa test.
- **Đề xuất hướng xử lý:** người chọn một trong hai. (a) Đổi tiêu chí của `AppLaunchTest` thành: chờ tối đa 30 giây tới khi thấy `sample_screen` (việc loading hiện ra đúng đã có unit test và test giao diện kiểm tra). (b) Giữ tiêu chí cũ nhưng cho test cách kéo dài hoặc giữ màn hình loading (cần thêm file ngoài danh sách plan, ví dụ một hook chỉ có trong bản debug). Phương án (a) nhỏ hơn; cần sửa plan mục 4 và duyệt lại vì thay đổi điều kiểm chứng.
- **Việc còn lại:** quyết định phương án trên, sửa plan (tăng phiên bản), chạy lại CI, merge bằng `scripts/merge-if-green.sh`, kiểm tra thủ công mục 4.
- **Cần người xem:** ba ảnh chuẩn screenshot mới, và các điểm rà soát mức ghi chú ở mục trên (mục "Rà soát độc lập" vẫn đúng).

### RUN-20261015-02, phiên bản plan 2

- **Kết quả:** kết thúc phiên ở bước 3, không merge, không mở PR. Plan và ticket giữ trạng thái `approved` vì lý do là hạ tầng, không phải plan chưa đủ tốt.
- **Nhánh, PR:** `claude/T-001-loading` chỉ tạo cục bộ, không đẩy lên. Không có PR.
- **Lý do:** lần chạy test đầu tiên (`:core:config:testDebugUnitTest`, `:feature:loading:testDebugUnitTest`) thất bại khi Gradle tải plugin: 429 Too Many Requests từ `repo.maven.apache.org`. Đã chạy lại 3 lần (chờ 1 phút, rồi 90 giây), vẫn 429.
- **Đã làm:** viết khung module và toàn bộ test theo mục 4 (chưa chạy được nên chưa xác nhận thất bại đúng lý do, chưa commit lên nhánh nào được đẩy).
- **Việc còn lại:** toàn bộ task. Cần danh sách chạy mới.

