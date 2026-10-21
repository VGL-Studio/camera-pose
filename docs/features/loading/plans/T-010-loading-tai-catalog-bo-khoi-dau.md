---
ticket: T-010
title: Màn loading tải catalog và bộ tư thế khởi đầu
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 20M          # cỡ M, số gợi ý của scripts/token-usage.py estimate
blocking_points: [core, r2]  # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-010: MÀN LOADING TẢI CATALOG VÀ BỘ TƯ THẾ KHỞI ĐẦU

## 1. Tóm tắt

Màn loading bỏ ba file mẫu `test/v1/*` và làm ba phần việc nối tiếp nhau: tải cấu hình (10% thanh tiến trình), tải catalog tư thế (10%), tải bộ khởi đầu (80%, tăng theo từng file). Cả ba dùng mức chờ 15 giây (ADR-1001): `RemoteConfigRefresher` đổi từ 10 sang 15 giây, catalog và từng file đã có mức 15 giây trong `:core:poses` (T-009). Sau loading, `:app` chọn màn chính bằng một hàm thuần (màn chào, camera, hoặc màn mẫu khi hai màn kia chưa có); T-012 và T-013 chỉ cần cắm đích của mình vào. Kiểm chứng bằng unit test cho ViewModel, repository, `RemoteConfigRefresher` và hàm chọn màn chính, test trên emulator trong CI (cần catalog mẫu trên R2 staging), và kiểm tra thủ công trên bản staging.

## 2. Hiểu yêu cầu

Ticket xử lý KB-1 đến KB-4 trong `features/loading/spec.md` (đã xác nhận, bản đổi ngày 2026-10-21). Giao diện thuộc T-008; plan này không đổi màn hình.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Màn loading hiện đầu tiên, đúng thiết kế | Không đổi: `LoadingDestination` vẫn là `startDestination`. Bố cục và chữ thuộc T-008 |
| KB-1 | Thanh tiến trình tăng theo phần của từng việc, bộ khởi đầu tăng theo từng file, không bao giờ lùi | Ba phần việc với phần chia 100, 100, 800 trên 1000 (câu hỏi 8 của spec). Phần bộ khởi đầu bằng `800 * done / total` (chia lấy phần nguyên), `done`, `total` lấy từ `onProgress` của `loadStarterSet`. Trạng thái chỉ được thay bằng giá trị lớn hơn hoặc bằng (`MutableStateFlow.update` với `max`) |
| KB-1 | Cả ba xong và đủ 2 giây thì tự chuyển | Như hiện nay: chờ phần còn thiếu của 2 giây rồi `Finished` |
| KB-1 | Xong trước 2 giây thì đứng ở 100% tới đủ 2 giây | Như hiện nay: `InProgress(1f)` trong lúc chờ |
| KB-1 | Ghi `loading_completed` một lần | Không đổi: tên, `duration_ms`, `retry_count`, chỗ ghi |
| KB-1 | Bấm quay lại ở màn chính thì app đóng | Không đổi: `popUpTo<LoadingDestination> { inclusive = true }`, nay áp dụng cho mọi đích của màn chính |
| KB-1 | Xoay máy không chạy lại, không tính lại 2 giây | Không đổi: ViewModel sống qua đổi cấu hình |
| KB-1, phạm vi | Chuẩn bị xong thì sang màn chào (lần đầu, cờ bật) hoặc camera | Hàm thuần `chooseMainScreen` trong `:app` (mục 3.1). Màn chào và camera chưa có nên luôn ra màn mẫu; T-012, T-013 bật nhánh của mình |
| KB-2 | Một phần việc không xong thì báo lỗi, giữ mức đã đạt, có "Thử lại" | Ba phần việc chạy nối tiếp, vì catalog cần đường dẫn trong cấu hình vừa tải và bộ khởi đầu cần catalog (câu hỏi 1). Phần việc đầu tiên thất bại thì dừng, `Failed(progress)` với mức đã đạt. Giao diện lỗi giữ nguyên |
| KB-2 | "Thử lại" chỉ làm lại phần chưa xong, tiến trình tiếp tục từ chỗ đã đạt | ViewModel giữ tập phần việc đã xong và phần nghìn của bộ khởi đầu đã đạt. `:core:poses` nhớ các file đã xong và chỉ tải file còn thiếu (plan T-009) |
| KB-2 | Có mạng lại, bấm "Thử lại" thì vào màn chính | Như KB-1 sau khi thử lại thành công |
| KB-2 | Cấu hình, catalog, một file của bộ khởi đầu không có phản hồi sau 15 giây thì báo lỗi | Cấu hình: `REFRESH_TIMEOUT_MILLIS = 15_000` (ADR-1001). Catalog và từng file: `LOAD_TIMEOUT_MILLIS = 15_000` của `:core:poses` (T-009), trả `CatalogState.Failed` hay `StarterSetResult.Incomplete`. ViewModel không thêm mức chờ riêng |
| KB-3 | Catalog và bộ khởi đầu đã có trên máy thì xong ngay, không cần mạng | `loadCatalog()` trả ngay khi đã có với cùng đường dẫn; `RemoteAssets` trả file đã giữ (T-009, ADR-0015) |
| KB-3 | Không tải được cấu hình thì báo lỗi, kể cả khi đã từng tải | Không đổi (ADR-0018). Vì chạy nối tiếp, lỗi này hiện ở 0%; catalog và bộ khởi đầu xong ngay sau "Thử lại" có mạng |
| KB-4 | Cờ `feature_loading_enabled` tắt thì vào thẳng màn chính, không ghi sự kiện | Không đổi trong ViewModel; màn chính cũng được chọn bằng `chooseMainScreen` |
| Mục 9, câu 4 | Cấu hình Firebase giả: phần cấu hình xong ngay | Không đổi. Với file giả, `poses_catalog_path` luôn là `DEFAULT_CATALOG_PATH` (catalog mẫu của T-009) |
| Ticket | Bỏ ba file `test/v1/*` | `DefaultPreparationRepository` không còn dùng `RemoteAssets`. File trên R2 không bị xóa (ADR-0006) |

Thiết kế tham chiếu: không có thiết kế riêng cho phần này. Màn hình không đổi; giao diện Sticker của loading thuộc T-008.

ADR liên quan: ADR-0001 (feature không phụ thuộc feature: `:feature:loading` không biết màn chào hay camera, `:app` chọn đích), ADR-0003 (cờ), ADR-0006 và ADR-0015 (đường dẫn R2 lấy từ catalog qua `:core:poses`), ADR-0012 (cấu hình Firebase giả), ADR-0018 và ADR-1001 (15 giây), ADR-0019 (log).

## 3. Giải pháp

### 3.1. Cách làm

**Điều kiện trước khi bắt đầu.** T-009 và T-007 đã merge vào nhánh chính. Catalog mẫu và ảnh mượn của T-009 đã có trên R2 staging (mục 3.3). Không thỏa thì dừng (mục 7).

**`:core:config`**

- `RemoteConfigRefreshPolicy.REFRESH_TIMEOUT_MILLIS` đổi từ `10_000L` thành `15_000L`. KDoc của `RemoteConfigRefresher` ghi thêm "ADR-1001: 15 giây ở app này". Không đổi gì khác.
- `:core:config` có `origin: template`. ADR-1001 ghi rõ không đưa thay đổi này về dự án mẫu; PR ghi đây là thay đổi riêng của app ở module dự án mẫu.

**`:feature:loading`**

- `feature/loading/build.gradle.kts`: thêm `dependencies { implementation(project(":core:poses")) }` (plan T-009, câu hỏi 7).
- `PreparationTask`: `enum class PreparationTask(val share: Int) { REMOTE_CONFIG(100), CATALOG(100), STARTER_SET(800) }`. `share` là phần trên 1000 của thanh tiến trình. Thứ tự của enum là thứ tự chạy. Hằng `TOTAL_SHARE = 1000` trong companion.
- `PreparationRepository`: `suspend fun run(task: PreparationTask, onProgress: (done: Int, total: Int) -> Unit): Boolean`. Chỉ `STARTER_SET` gọi `onProgress`. Không có giá trị mặc định cho tham số, để lời gọi luôn rõ ràng.
- `DefaultPreparationRepository(refresher: RemoteConfigRefresher, poses: PoseCatalogRepository)`:
  - `REMOTE_CONFIG`: `refresher.refresh()`.
  - `CATALOG`: `poses.loadCatalog() is CatalogState.Ready`.
  - `STARTER_SET`: `poses.loadStarterSet(onProgress) is StarterSetResult.Complete`.
  - Bỏ `RemoteAssets` và ba đường dẫn `test/v1/*`. Không log ở đây: `:core:poses` đã log lý do lỗi (thẻ `Poses`), ViewModel log kết quả từng phần việc.
- `LoadingViewModel` (giữ constructor, `LoadingUiState`, `LoadingClock`, `onRetry()`, sự kiện):
  - `runPending()` chạy lần lượt các phần việc chưa xong theo thứ tự enum, không còn `async`. Một phần việc trả `false` hay ném lỗi (`runSafely` như hiện nay) thì ghi log `warn`, đặt `Failed(progress())` và dừng; các phần sau không chạy ở lần này.
  - Tiến trình tính theo phần nghìn: `doneShare` là tổng `share` của các phần việc đã xong, trừ `STARTER_SET`; `starterShare` là phần nghìn đã đạt của bộ khởi đầu (`AtomicInteger`, chỉ tăng). `progress() = (doneShare + (if STARTER_SET đã xong thì 800 else starterShare)) / 1000f`. Chia một số nguyên cho `1000f` cho kết quả gần đúng nhất, nên test so được bằng `InProgress(0.3f)`.
  - `onProgress(done, total)`: `total <= 0` thì bỏ qua; ngược lại `starterShare` lấy `max(cũ, 800 * done.coerceIn(0, total) / total)`, rồi `state.update { if (it is InProgress) InProgress(max(it.progress, progress())) else it }`. Hàm này có thể được gọi từ luồng khác (tối đa 4 file tải cùng lúc trong `:core:poses`); `update` và `max` bảo đảm không lùi, và không ghi đè `Failed` hay `Finished`.
  - Một phần việc xong: thêm vào tập đã xong, log `info`, `state.update` như trên.
  - Đủ ba phần việc: như hiện nay (chờ đủ 2 giây, ghi `loading_completed`, `Finished`).
  - Catalog đổi giữa hai lần thử (cấu hình mới đổi đường dẫn) làm `total` khác đi: tiến trình giữ mức đã đạt nhờ `max`, chỉ tăng tiếp khi phần mới vượt qua (câu hỏi 6).
  - KDoc lớp: "ba phần việc nối tiếp, 10%, 10%, 80%", bỏ chữ "four tasks", "a quarter".
  - Log, thẻ `Loading`: như hiện nay (bắt đầu, từng phần việc xong hay thất bại, chờ thử lại, lần thử lại, xong). Không log từng file của bộ khởi đầu (`:core:poses` log khi bộ khởi đầu xong). Không ghi đường dẫn hay mã tư thế.
- `LoadingScreen.kt`, `LoadingUiState.kt`, `LoadingNavigation.kt`, `LoadingModule.kt`, `strings.xml`: không đổi. `LoadingModule` vẫn gắn `PreparationRepository` vào `DefaultPreparationRepository`; Hilt tự nhận constructor mới.

**`:app`: chọn màn chính sau loading**

Quyết định nằm trong `:app`, vì chỉ `:app` biết mọi feature (ADR-0001). `:feature:loading` chỉ báo `onFinished` như hiện nay.

- `navigation/MainScreen.kt` (mới):
  - `internal enum class MainScreen { ONBOARDING, CAMERA, SAMPLE }`.
  - `internal data class MainScreenInputs(val onboardingAvailable: Boolean, val onboardingDone: Boolean, val cameraAvailable: Boolean)`. Một màn "có sẵn" khi đích của nó đã đăng ký trong `AppNavHost` và cờ của nó bật.
  - `internal fun chooseMainScreen(inputs: MainScreenInputs): MainScreen`: `onboardingAvailable && !onboardingDone` thì `ONBOARDING`; ngược lại `cameraAvailable` thì `CAMERA`; ngược lại `SAMPLE`.
- `navigation/MainScreenChooser.kt` (mới): `class MainScreenChooser @Inject constructor()` với `suspend fun choose(): MainScreen = chooseMainScreen(inputs())`. Ở T-010, `inputs()` trả `MainScreenInputs(onboardingAvailable = false, onboardingDone = false, cameraAvailable = false)`, kèm chú thích: T-012 đọc `FeatureFlag.ONBOARDING` (giá trị đầu, `first()`) và trạng thái "đã qua màn chào"; T-013 đọc `FeatureFlag.CAMERA`. Đọc giá trị đầu là đúng ở đây: lựa chọn chỉ có nghĩa đúng lúc rời loading, như cách loading đọc cờ của mình (plan T-001, câu hỏi 3).
- `AppNavHost` thêm tham số `chooseMainScreen: suspend () -> MainScreen`:
  - `loadingScreen(onFinished = { scope.launch { ... } })` với `scope = rememberCoroutineScope()`: gọi `chooseMainScreen()`, rồi chỉ khi đích hiện tại vẫn là `LoadingDestination` (`navController.currentDestination?.hasRoute<LoadingDestination>() == true`) mới `navigate(screen.destination()) { popUpTo<LoadingDestination> { inclusive = true }; launchSingleTop = true }`. Kiểm tra này chặn điều hướng hai lần khi `onFinished` được gọi lại sau khi activity được tạo lại.
  - `private fun MainScreen.destination(): Any = when (this) { ONBOARDING -> SampleDestination; CAMERA -> SampleDestination; SAMPLE -> SampleDestination }`, mỗi nhánh có chú thích "T-012 thay bằng `OnboardingDestination`", "T-013 thay bằng `CameraDestination`". Với đầu vào của T-010, hai nhánh đầu không xảy ra.
  - Giữ mọi thứ T-011 đã thêm (tham số `onPastLoadingChanged`, nếu có trên nhánh chính).
- `MainActivity`: `@Inject lateinit var mainScreenChooser: MainScreenChooser`; gọi `AppNavHost(chooseMainScreen = mainScreenChooser::choose, ...)`, giữ nguyên các tham số khác đang có.
- `app/build.gradle.kts`: thêm `implementation(project(":core:poses"))` sau `:core:logging`, như mọi core khác mà app dùng. Đây cũng là lần đầu đồ thị Hilt có `PosesModule` (plan T-009, mục 3.1); build bản staging kiểm tra đồ thị.

**T-012 và T-013 cắm vào thế nào** (ghi lại để hai plan sau làm theo, không làm trong T-010):

- T-012: thêm `FeatureFlag.ONBOARDING`; lưu "đã qua màn chào" (nơi lưu: câu hỏi 2); `MainScreenChooser` đọc cờ và trạng thái đó; `ONBOARDING -> OnboardingDestination`; đăng ký `onboardingScreen(...)` trong `AppNavHost`, khi xong thì điều hướng tới `MainScreen.CAMERA.destination()` và bỏ màn chào khỏi back stack.
- T-013: thêm `FeatureFlag.CAMERA`; `cameraAvailable` đọc cờ; `CAMERA -> CameraDestination`.
- Không ai phải sửa `:feature:loading`.

**Thứ tự commit (L-025).**

- Bước 3 (commit test): `LoadingViewModelTest.kt` viết lại (gồm `FakePreparationRepository` mới có `onProgress`), `DefaultPreparationRepositoryTest.kt` viết lại (gồm `FakeRefresher`, `FakePoseCatalogRepository`), `RemoteConfigRefresherTest.kt`, `MainScreenTest.kt`, `AppLaunchTest.kt`. Cùng commit là khung để test biên dịch được: `PreparationTask` mới có `share`; chữ ký mới của `PreparationRepository`; `DefaultPreparationRepository` nhận `PoseCatalogRepository`, `CATALOG` và `STARTER_SET` trả `false`; `LoadingViewModel` chỉ đổi lời gọi thành `repository.run(task) { _, _ -> }`, logic cũ giữ nguyên (chạy song song, tiến trình chia đều cho số phần việc); `feature/loading/build.gradle.kts`; `MainScreen.kt` với `chooseMainScreen` luôn trả `SAMPLE`; `MainScreenChooser.kt`. `REFRESH_TIMEOUT_MILLIS` vẫn là 10 giây.
- Bước 4 (commit code): logic thật của `LoadingViewModel`, `DefaultPreparationRepository`, `chooseMainScreen`; `REFRESH_TIMEOUT_MILLIS = 15_000L` và KDoc; `AppNavHost.kt`, `MainActivity.kt`, `app/build.gradle.kts`.

**Mẫu có sẵn được làm theo:** `LoadingViewModel` và `LoadingViewModelTest` hiện có (fake chờ `CompletableDeferred`, thời gian ảo), `RemoteConfigRefreshPolicy` (logic tách khỏi Android), cách `:app` nối điều hướng trong `AppNavHost`.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/RemoteConfigRefresher.kt` | `REFRESH_TIMEOUT_MILLIS = 15_000L`, KDoc nhắc ADR-1001 |
| Sửa | `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/RemoteConfigRefresherTest.kt` | Đổi hai test 10 giây thành 15 giây, thêm một test (mục 4) |
| Sửa | `feature/loading/build.gradle.kts` | `implementation(project(":core:poses"))` |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/data/PreparationTask.kt` | Ba phần việc, `share`, `TOTAL_SHARE`, KDoc |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/data/PreparationRepository.kt` | Tham số `onProgress`, KDoc |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/data/DefaultPreparationRepository.kt` | Dùng `RemoteConfigRefresher` và `PoseCatalogRepository`, bỏ `RemoteAssets` và `test/v1/*` |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingViewModel.kt` | Chạy nối tiếp, tiến trình theo phần nghìn, `onProgress`, KDoc |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingViewModelTest.kt` | Viết lại theo mục 4; fake mới có `onProgress` |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/data/DefaultPreparationRepositoryTest.kt` | Viết lại theo mục 4; `FakePoseCatalogRepository` riêng trong file |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/MainScreen.kt` | `MainScreen`, `MainScreenInputs`, `chooseMainScreen` |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/MainScreenChooser.kt` | `MainScreenChooser` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/AppNavHost.kt` | Tham số `chooseMainScreen`, điều hướng theo `MainScreen`, chặn điều hướng hai lần |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | Tiêm `MainScreenChooser`, truyền vào `AppNavHost` |
| Sửa | `app/build.gradle.kts` | `implementation(project(":core:poses"))` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/navigation/MainScreenTest.kt` | Unit test của `chooseMainScreen` và `MainScreenChooser` (mục 4) |
| Sửa | `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt` | Chờ tối đa 60 giây thay vì 30 giây; KDoc ghi test cần catalog mẫu và bộ khởi đầu trên R2 staging |

Những nơi đọc các file trên (L-020, đã tìm bằng `grep -rn` trong `scripts/`, `.github/`, `build-logic/`, `*/src/test`, `*/src/androidTest`):

- `scripts/`, `.github/`, `build-logic/` không nhắc tới file nào trong bảng, cũng không nhắc `test/v1`, `sample_screen` hay `REFRESH_TIMEOUT_MILLIS`. `pr-check.yml` chạy `:app:connectedStagingDebugAndroidTest`, không đọc tên file test.
- `scripts/release.sh` đọc `app/build.gradle.kts` để lấy `applicationId`, `versionCode`, `versionName` (dòng `applicationId = ...`, `versionCode = ...`, `versionName = ...` trong `defaultConfig`). Plan chỉ thêm một dòng `implementation(project(":core:poses"))` trong `dependencies`, không chạm ba dòng đó, nên script không bị ảnh hưởng. Không sửa, không chạy script này.
- `RemoteConfigRefresher` chỉ được `ConfigModule.kt` (gắn Hilt, không đổi) và `DefaultPreparationRepository.kt` (trong bảng) dùng.
- `AppNavHost` chỉ được `MainActivity.kt` (trong bảng) gọi.
- `PreparationTask`, `PreparationRepository` chỉ nằm trong `:feature:loading`, các file đều trong bảng hoặc không dùng hai kiểu này (`LoadingScreen.kt`, `LoadingNavigation.kt`). `LoadingModule.kt` chỉ gắn kiểu, không đổi.
- `LoadingScreenTest.kt`, `LoadingScreenScreenshotTest.kt` chỉ dùng `LoadingUiState` (không đổi), nên ảnh chuẩn không đổi.
- `docs/ops/setup-new-app.md` lấy `test/v1/hello.txt` làm ví dụ cho dự án mẫu; `docs/ops/test-sessions.md` mục 4 ghi "10 giây tải cấu hình" khi kể lại các phiên thử cũ. Hai tài liệu này kể điều đã xảy ra, không đọc code; không sửa trong task này.
- Nhật ký plan, trạng thái plan và ticket, báo cáo phiên đi qua nhánh báo cáo của phiên (auto-run.md, mục 4).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: không thêm cờ. `feature_loading_enabled` giữ như hiện nay (bật sẵn ở staging qua `StagingFeatureFlagsModule`, không sửa file đó). `feature_onboarding_enabled`, `feature_camera_enabled` thuộc T-012, T-013.
- Quảng cáo: không chạm.
- Sự kiện phân tích: không chạm. `loading_completed` giữ nguyên tên, chỗ ghi và hai tham số `duration_ms`, `retry_count`.
- Dữ liệu cục bộ: không thêm khóa DataStore. Catalog và ảnh do `:core:assets` giữ trên máy như hiện nay.
- File R2 (điểm chặn 8):
  - Màn loading thôi đọc `test/v1/hello.txt`, `content.txt`, `goodbye.txt`. Ba file vẫn ở R2 staging (ADR-0006: không xóa).
  - Loading đọc catalog theo `poses_catalog_path` (mặc định `DEFAULT_CATALOG_PATH`, `poses/catalog/v1/catalog-<sha8>.json`) và ảnh nhỏ, nét viền của các tư thế `starter` theo đường dẫn ghi trong catalog. Cách đặt tên do T-009 quyết; plan này không tạo đường dẫn mới.
  - Bản build trên máy dev và trong CI dùng file Firebase giả, nên luôn đọc `DEFAULT_CATALOG_PATH`. Catalog mẫu có 4 tư thế `starter` (`w01`, `w02`, `m01`, `m02`), tức 8 file: `borrowed/poses/img/v1/<id>/preview_540.webp` và `outline.webp`.
  - **Điều kiện cho `AppLaunchTest`:** trước khi xóa ba file mẫu khỏi loading, 9 file này (catalog mẫu và 8 file bộ khởi đầu) phải trả `200` từ `https://camera-pose.vgl.io.vn/`. Thiếu một file thì loading trên emulator dừng ở `Failed`, `AppLaunchTest` hết giờ, CI đỏ. Dev đưa file lên sau khi T-009 merge (plan T-009, mục 3.3); AI không ghi lên R2.

## 4. Kiểm thử

Unit test chạy trên JVM với thời gian ảo, không mạng. `FakePreparationRepository` (trong `LoadingViewModelTest.kt`): mỗi lời gọi `run(task, onProgress)` ghi vào `calls`, giữ `onProgress` và chờ `CompletableDeferred`; test gọi `complete(task, success)`, `fail(task, error)`, `progress(done, total)` (gọi `onProgress` đang giữ của `STARTER_SET`) và `completeInOrder()` (xong lần lượt từng phần việc đang chạy cho tới hết). Với khung của bước 3 (chạy song song, chia đều cho ba), các test ghi "thất bại ở bước 3" dưới đây thất bại ở phần khẳng định.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, chạy nối tiếp | Unit test | `LoadingViewModelTest.flagOn_startsAtZero_andRunsRemoteConfigOnly`: `InProgress(0f)`, `calls == [REMOTE_CONFIG]` (thất bại ở bước 3: cả ba chạy cùng lúc) |
| KB-1, thứ tự | Unit test | `LoadingViewModelTest.tasksRunInOrder_configThenCatalogThenStarterSet`: sau `complete(REMOTE_CONFIG, true)` thì `calls == [REMOTE_CONFIG, CATALOG]`; sau `complete(CATALOG, true)` thì thêm `STARTER_SET` |
| KB-1, 10%, 10%, 80% theo từng file, không lùi | Unit test | `LoadingViewModelTest.progress_isTenTenEighty_starterSetPerFile_neverDecreasing`: cấu hình xong `InProgress(0.1f)`; catalog xong `0.2f`; `progress(0, 8)` `0.2f`; `progress(1, 8)` `0.3f`; `progress(4, 8)` `0.6f`; `progress(8, 8)` `1f`; `complete(STARTER_SET, true)` vẫn `1f`. Mọi giá trị thu được (Turbine hay `collect` trên `UnconfinedTestDispatcher`) không giảm |
| KB-1, tiến trình báo lệch thứ tự | Unit test | `LoadingViewModelTest.starterSetProgressOutOfOrder_neverGoesBack`: `progress(3, 8)` rồi `progress(2, 8)`: vẫn `InProgress(0.5f)` |
| KB-1, bộ khởi đầu 60 tư thế | Unit test | `LoadingViewModelTest.starterSetOfSixtyFiles_roundsDownPerFile`: `progress(1, 60)` là `InProgress(0.213f)` (`800 * 1 / 60 = 13`); `progress(59, 60)` là `InProgress(0.986f)` |
| KB-1, bộ khởi đầu rỗng | Unit test | `LoadingViewModelTest.starterSetWithoutFiles_reachesFullWhenDone`: `progress(0, 0)` giữ `0.2f`; `complete(STARTER_SET, true)` là `1f` |
| KB-1, phần chia đủ thanh | Unit test | `LoadingViewModelTest.taskShares_addUpToWholeBar`: tổng `share` là `1000`, thứ tự enum là `REMOTE_CONFIG, CATALOG, STARTER_SET` |
| KB-1, đủ 2 giây thì chuyển | Unit test | `LoadingViewModelTest.allTasksDoneAfterTwoSeconds_finishesAtOnce` (viết lại với `completeInOrder()`) |
| KB-1, xong trước 2 giây thì đứng ở 100% | Unit test | `LoadingViewModelTest.allTasksDoneBeforeTwoSeconds_staysAtFullUntilTwoSeconds` (viết lại với `completeInOrder()`) |
| KB-1, `loading_completed` một lần, tham số không đổi | Unit test | `LoadingViewModelTest.finished_logsLoadingCompletedOnce_withDurationAndRetryCount`: đúng một sự kiện `loading_completed` với `duration_ms = "2000"`, `retry_count = "0"` |
| KB-1, sang màn chính đúng lựa chọn | Unit test | `MainScreenTest.notOnboardedAndOnboardingAvailable_choosesOnboarding`, `onboardedAndCameraAvailable_choosesCamera`, `onboardingUnavailable_cameraAvailable_choosesCamera` (gồm cả trường hợp chưa qua màn chào), `onboardingAvailableButDone_cameraUnavailable_choosesSample`, `nothingAvailable_choosesSample` (thất bại ở bước 3 trừ hai test ra `SAMPLE`) |
| KB-1, khi chưa có màn chào và camera | Unit test | `MainScreenTest.chooserBeforeOnboardingAndCamera_choosesSample`: `runTest { assertEquals(MainScreen.SAMPLE, MainScreenChooser().choose()) }` |
| KB-1, chuyển sang màn chính trên app thật | Test trên emulator (CI) | `AppLaunchTest.appStarts_andReachesStartScreen`: `waitUntil` tối đa 60 giây tới khi `onAllNodesWithTag("sample_screen").fetchSemanticsNodes()` không rỗng. Không kiểm tra màn loading, không dùng `assertIsDisplayed`. Cần 9 file ở mục 3.3 trên R2 staging. 60 giây vì đường chậm nhất mà vẫn thành công là catalog 15 giây, rồi 8 file trong hai lượt 4 file, mỗi lượt tới 15 giây, cộng 2 giây tối thiểu: 47 giây (câu hỏi 4) |
| KB-1, quay lại thì đóng app; xoay máy | Kiểm tra thủ công | Bản staging: qua loading tới màn mẫu, bấm quay lại: app đóng. Xoay máy khi thanh đang tăng: thanh không về 0 |
| KB-1, log | Unit test | `LoadingViewModelTest.logsStart_eachTaskResult_retry_andFinish`: cấu hình xong, catalog `false`; thứ tự mức `INFO` (bắt đầu), `INFO` (cấu hình xong), `WARN` (catalog thất bại), `WARN` (chờ thử lại), `INFO` (thử lại 1), `INFO` (catalog xong), `INFO` (bộ khởi đầu xong), `INFO` (xong); mọi thẻ là `Loading`; không dòng nào chứa `poses/`, `.json`, `.webp` hay `test/v1` |
| KB-2, cấu hình lỗi | Unit test | `LoadingViewModelTest.remoteConfigFails_showsFailedAtZero_andSkipsCatalogAndStarterSet`: `Failed(0f)`, `calls == [REMOTE_CONFIG]`; 20 giây sau vẫn `Failed(0f)`, không có sự kiện |
| KB-2, catalog lỗi | Unit test | `LoadingViewModelTest.catalogFails_showsFailedAtTenPercent_andSkipsStarterSet`: `Failed(0.1f)`, `STARTER_SET` không chạy |
| KB-2, bộ khởi đầu lỗi giữa chừng | Unit test | `LoadingViewModelTest.starterSetFails_showsFailedAtReachedProgress`: `progress(5, 8)` rồi `complete(STARTER_SET, false)`: `Failed(0.7f)` |
| KB-2, lỗi bất ngờ | Unit test | `LoadingViewModelTest.taskThrows_isTreatedAsFailure_andShowsFailed`: `fail(CATALOG, IllegalStateException())`: `Failed(0.1f)` |
| KB-2, thử lại chỉ làm phần chưa xong, tiếp tục từ chỗ đã đạt | Unit test | `LoadingViewModelTest.retry_afterStarterSetFailed_runsOnlyStarterSet_andProgressContinues`: dừng ở `Failed(0.7f)`; `onRetry()`: `InProgress(0.7f)`, `calls` mới chỉ có `STARTER_SET`; `progress(5, 8)` vẫn `0.7f`; `progress(6, 8)` là `0.8f` |
| KB-2, thử lại sau lỗi cấu hình | Unit test | `LoadingViewModelTest.retry_afterRemoteConfigFailed_runsAllThreeInOrder` |
| KB-2, "Thử lại" khi đang chạy | Unit test | `LoadingViewModelTest.retry_whileInProgress_doesNothing` |
| KB-2, có mạng lại thì vào màn chính | Unit test | `LoadingViewModelTest.retrySucceeds_finishes_andCountsOneRetry`: `loading_completed` với `retry_count = "1"`, `duration_ms = "3000"` |
| KB-2, tiến trình báo muộn sau khi lỗi | Unit test | `LoadingViewModelTest.progressAfterFailed_keepsFailed`: đang `Failed(0.7f)`, `progress(8, 8)` từ lần chạy trước: vẫn `Failed(0.7f)` |
| KB-2, cấu hình chờ 15 giây | Unit test | `RemoteConfigRefresherTest.fetchLongerThanFifteenSeconds_returnsFalse` (`delay(15_001)`, đổi từ `fetchLongerThanTenSeconds_returnsFalse`), `fetchJustUnderFifteenSeconds_returnsTrue` (`delay(14_999)`, đổi từ `fetchJustUnderTenSeconds_returnsTrue`), `fetchBetweenTenAndFifteenSeconds_returnsTrue` (`delay(12_000)`). Hai test sau thất bại ở bước 3 |
| KB-2, catalog và từng file chờ 15 giây | Test của T-009 | `TimedAssetLoaderTest.noAnswerWithin15s_returnsTimeoutAtExactly15s`, `DefaultPoseCatalogRepositoryTest.loadCatalog_timeout_failsWithTimeout`, `starterSet_fileWithoutAnswer_failsAfter15s_othersComplete` |
| KB-2, hết giờ thành lỗi của phần việc | Unit test | `DefaultPreparationRepositoryTest.catalogFailed_returnsFalse_forEveryReason`: `CatalogState.Failed` với `NETWORK`, `NOT_FOUND`, `TIMEOUT`, `OTHER` đều trả `false`; `starterSetIncomplete_returnsFalse` (`Incomplete(1, TIMEOUT)`) |
| KB-2, mất mạng thật | Kiểm tra thủ công | Cài mới bản staging, bật chế độ máy bay, mở app: báo lỗi ở 0%. Tắt chế độ máy bay, bấm "Thử lại": thanh lên 10%, 20%, rồi tăng theo từng file tới 100%, vào màn mẫu |
| Phần việc catalog | Unit test | `DefaultPreparationRepositoryTest.catalogReady_returnsTrue` (một lần `loadCatalog()`, không gọi `refresh()`) |
| Phần việc bộ khởi đầu, chuyển tiến trình | Unit test | `DefaultPreparationRepositoryTest.starterSetComplete_returnsTrue_andForwardsProgress`: fake gọi `onProgress(0, 2)`, `(1, 2)`, `(2, 2)`; callback của test nhận đúng ba cặp đó theo thứ tự |
| Phần việc cấu hình | Unit test | `DefaultPreparationRepositoryTest.remoteConfigTask_usesRefresher` (giữ ý của test cũ): `true` rồi `false` theo `refresher`, không gọi `loadCatalog` hay `loadStarterSet` |
| Bỏ ba file mẫu | Unit test | `DefaultPreparationRepositoryTest` không còn `RemoteAssets`; constructor chỉ nhận `refresher` và `poses`, nên không thể đọc `test/v1/*`. Thay cho `fileTasks_loadTheThreeVersionedPaths` và `unavailableFile_returnsFalse` (hai test này kiểm tra yêu cầu đã bỏ) |
| KB-3, đã có trên máy thì xong ngay | Test của T-009 và unit test | `DefaultPoseCatalogRepositoryTest.loadCatalog_whenReady_doesNotLoadAgain`, test bộ nhớ đệm của `:core:assets`; `LoadingViewModelTest.tasksRunInOrder_configThenCatalogThenStarterSet` (catalog, bộ khởi đầu xong ngay khi fake trả ngay) |
| KB-3, không tải được cấu hình | Unit test | `RemoteConfigRefresherTest.fetchFails_returnsFalse`, `LoadingViewModelTest.remoteConfigFails_showsFailedAtZero_andSkipsCatalogAndStarterSet` |
| KB-3 trên máy thật | Kiểm tra thủ công | Mở bản staging một lần có mạng, tắt mạng, mở lại: báo lỗi ở 0%. Bật mạng, "Thử lại": tới 100% gần như ngay, vì catalog và bộ khởi đầu đã có |
| KB-4 | Unit test | `LoadingViewModelTest.flagOff_finishesAtOnce_withoutRunningTasksOrEvent` (giữ nguyên ý); màn chính chọn bằng `chooseMainScreen` như KB-1 |
| Đồ thị Hilt có `:core:poses` | Build | `assembleStagingDebug` trong `scripts/check.sh`; `AppLaunchTest` mở app thật |
| Quy tắc phụ thuộc module (ADR-0001) | Kiểm tra tự động | `checkModuleDependencies` trong `scripts/check.sh`: `:feature:loading` chỉ thêm `:core:poses` |
| Giao diện không đổi | Screenshot test hiện có | `LoadingScreenScreenshotTest`, `LoadingScreenTest`, `SampleScreenScreenshotTest` qua mà không ghi lại ảnh chuẩn nào |
| 9 file trên R2 staging | Kiểm tra trước bước 3 | `curl -sI -o /dev/null -w '%{http_code}'` cho `https://camera-pose.vgl.io.vn/` cộng `DEFAULT_CATALOG_PATH`, và 8 đường dẫn `borrowed/poses/img/v1/{w01,w02,m01,m02}/{preview_540,outline}.webp`: đều `200` |

**Ngoại lệ viết test trước:** `AppNavHost.kt`, `MainActivity.kt`, `app/build.gradle.kts`, `feature/loading/build.gradle.kts` và KDoc không có test JVM riêng. Điều hướng cần `LoadingRoute` với `hiltViewModel()`, nên kiểm bằng build bản staging, `AppLaunchTest` và kiểm tra thủ công. Chặn điều hướng hai lần chỉ kiểm được bằng cách tạo lại activity đúng lúc, nên không có test tự động. Các test cũ của `LoadingViewModelTest` và `DefaultPreparationRepositoryTest` được viết lại vì yêu cầu đổi (bốn phần việc 25% thành ba phần việc 10%, 10%, 80%; bỏ ba file mẫu); hai test 10 giây của `RemoteConfigRefresherTest` đổi sang 15 giây theo ADR-1001. Đây là thay đổi điều test kiểm tra do spec đổi, người duyệt cần đồng ý.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (sửa `RemoteConfigRefresher` của `:core:config`, module dùng chung có `origin: template`) và điểm 8 (màn loading bắt đầu đọc đường dẫn catalog và ảnh trên R2 theo cách đặt tên của T-009, thôi đọc `test/v1/*`). Rủi ro cao, bắt buộc duyệt chéo.

Không chạm: điểm 4 (`loading_completed` không đổi, không thêm sự kiện); điểm 5 (thứ tự khởi tạo SDK không đổi: hộp thoại đồng ý vẫn ở `MainActivity.onCreate`, Firebase tự khởi tạo, Remote Config vẫn tải trong loading; chỉ đổi đích sau loading); điểm 9 (không thêm, không đổi khóa DataStore).

Bảng điểm dưới đây chỉ để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module: `:core:config`, `:feature:loading`, `:app`; đổi giao diện công khai `PreparationRepository` và `AppNavHost` |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, đủ câu trả lời (10/10/80, 15 giây); AI tự quyết chạy nối tiếp và cách cắm đích sau loading (câu hỏi 1, 2) |
| C. Khả năng kiểm chứng tự động | 1 | Logic có unit test với thời gian ảo; mất mạng thật, xoay máy, nút quay lại kiểm tra thủ công; test emulator dựa vào file trên R2 staging |
| D. Ảnh hưởng chéo | 3 | Đổi đích sau loading mà T-012, T-013 dựa vào; sửa `AppNavHost`, `MainActivity` mà T-011 cũng sửa; `AppLaunchTest` của mọi PR từ nay cần catalog mẫu trên R2 staging |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu loading có sẵn; mới là tiến trình gọi về từ luồng khác, xử lý bằng `MutableStateFlow.update` |
| **Tổng** | 9 | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `:core:config`: một hằng số thời gian chờ. Chỉ màn loading dùng `RemoteConfigRefresher` (ADR-0018), nên không màn nào khác đổi hành vi.
  - `AppNavHost`, `MainActivity`: thêm tham số và tiêm một lớp; đích hiện tại vẫn là màn mẫu.
  - `app/build.gradle.kts`: thêm `:core:poses`. Lần đầu `PosesModule` vào đồ thị Hilt của app.
  - `AppLaunchTest`: từ T-010, test emulator của mọi PR cần 9 file của catalog mẫu trên R2 staging. File R2 không bao giờ bị xóa (ADR-0006), nên điều kiện này chỉ phải lo một lần. Đổi `DEFAULT_CATALOG_PATH` về sau (catalog mới) cũng phải đưa file lên R2 staging trước khi merge.
- Task khác trong sprint có thể xung đột:
  - T-009: plan này dùng API của T-009 (`PoseCatalogRepository.loadCatalog`, `loadStarterSet(onProgress)`, `CatalogState`, `StarterSetResult`). T-009 sửa một dòng của `DefaultPreparationRepositoryTest.kt` (`AssetResult.Unavailable()`); plan này viết lại file đó trên bản đã có dòng sửa.
  - T-011: sửa `AppNavHost.kt` (`onPastLoadingChanged`) và `MainActivity.kt` (`NetworkDialogHost`). Chỉ xung đột ở mức dòng; T-010 dựng trên nhánh chính đã có T-011 và giữ phần của T-011. Hộp thoại mạng không hiện ở màn loading, nên lỗi tải catalog trong loading vẫn chỉ hiện trạng thái lỗi của loading.
  - T-008: sửa `LoadingScreen.kt`, `strings.xml` và ảnh chuẩn của loading. Plan này không chạm các file đó; `LoadingUiState` không đổi. Chạy trước hay sau đều được.
  - T-012, T-013: cắm đích theo mục 3.1; sửa `MainScreenChooser.kt`, `AppNavHost.kt`, `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`.
- Đề xuất thứ tự chạy: sau T-009 (bắt buộc), T-007 và T-011 (để dựng trên `AppNavHost`, `MainActivity` đã có hộp thoại mạng), và sau khi dev đưa catalog mẫu lên R2 staging. Trước T-012, T-013.

## 7. Điều kiện dừng riêng

1. T-009 chưa merge vào nhánh chính, hoặc API thật khác mục 3.1 của plan T-009: tên hay chữ ký của `loadCatalog()`, `loadStarterSet(onProgress: (done: Int, total: Int) -> Unit)`, `CatalogState.Ready`, `StarterSetResult.Complete`; hoặc `onProgress` không được gọi lúc bắt đầu và sau mỗi file với `done` chỉ tăng; hoặc thử lại không bắt đầu `done` từ số file đã xong.
2. `loadCatalog()` không đọc `poses_catalog_path` tại lúc gọi (ví dụ đọc một lần lúc tạo repository), nên không thấy giá trị vừa tải ở phần việc cấu hình.
3. Trước bước 3, một trong 9 địa chỉ ở mục 3.3 không trả `200`. Không xóa ba file mẫu khi chưa có catalog mẫu: báo dev đưa file lên.
4. Đồ thị Hilt lỗi khi thêm `:core:poses` vào app và cần sửa `:core:poses`, `:core:assets` hay `:core:config` ngoài hằng số thời gian chờ.
5. Cần sửa `LoadingScreen.kt`, `LoadingUiState.kt`, `strings.xml` hay ảnh chuẩn screenshot (phần của T-008).
6. Cần thêm cờ `FeatureFlag.ONBOARDING`, `FeatureFlag.CAMERA` hay lưu "đã qua màn chào" để code qua (phần của T-012, T-013).
7. `AppLaunchTest` đỏ vì loading dừng ở `Failed` trên emulator (log thẻ `VGL.Loading`, `VGL.Poses`) sau một lần chạy lại: không tăng thời gian chờ quá 60 giây, dừng và ghi lý do lỗi từ log.
8. Một test sẵn có ngoài `LoadingViewModelTest`, `DefaultPreparationRepositoryTest` và hai test 10 giây của `RemoteConfigRefresherTest` thất bại sau thay đổi.

## 8. Câu hỏi cho người duyệt

1. **Ba phần việc chạy nối tiếp hay song song?** Hiện nay bốn phần việc chạy song song. Catalog cần đường dẫn trong cấu hình vừa tải (spec, mục 2), bộ khởi đầu cần catalog.
   - Đề xuất của AI: chạy nối tiếp: cấu hình, rồi catalog, rồi bộ khởi đầu. Phần đầu tiên lỗi thì dừng và báo lỗi ngay. Hệ quả: mở lại khi không có mạng thì lỗi hiện ở 0% thay vì ở mức cao hơn như trước; sau "Thử lại" có mạng, catalog và bộ khởi đầu xong gần như ngay (KB-3). Chạy song song thì catalog có thể tải theo đường dẫn cũ rồi phải tải lại.
2. **"Đã qua màn chào" lưu ở đâu?** `:app` cần đọc để chọn màn chính; `:feature:onboarding` biết lúc người dùng bấm "Bật camera". Feature không được phụ thuộc feature (ADR-0001).
   - Đề xuất của AI: T-012 lưu ở `:app`: một lớp `OnboardingStatusStore` trong `:app` đọc, ghi khóa `onboarding_done` trong DataStore chung của `:core:data`. `onboardingScreen(onStarted, onFinished)` của feature gọi `onStarted` khi bấm "Bật camera" (trước hộp hỏi quyền, để tắt app lúc đó vẫn tính là đã qua, onboarding KB-3); `:app` ghi khóa trong `onStarted`. `MainScreenChooser` đọc khóa đó. Feature không cần lưu gì, không chạm core. Phương án khác: một interface trong `:core:data` cho cả hai bên dùng, nhưng đó là module của dự án mẫu (điểm chặn 7). T-010 không làm phần này, chỉ để chỗ (`onboardingDone`).
3. **Khi camera đã có mà cờ `feature_camera_enabled` tắt thì vào đâu?**
   - Đề xuất của AI: vào màn mẫu, như khi camera chưa có (`chooseMainScreen` trả `SAMPLE`). Khi gỡ `:feature:sample` khỏi app thì cần quyết định lại, ghi vào ticket đó. Bản production bật cờ camera qua Remote Config khi phát hành.
4. **`AppLaunchTest` chờ 60 giây thay vì 30 giây.**
   - Đề xuất của AI: đồng ý 60 giây. Loading nay tải catalog rồi 8 file nối nhau; đường chậm nhất vẫn thành công là khoảng 47 giây (mục 4), và emulator CI vừa khởi động đã từng chậm khi mở lần đầu (FB-20261017-02). Test vẫn chỉ kiểm trạng thái ổn định. Lần chạy bình thường không lâu hơn, vì test dừng chờ ngay khi thấy màn mẫu.
5. **Dev đưa catalog mẫu và ảnh mượn lên R2 staging trước phiên có T-010.** Không có 9 file ở mục 3.3 thì CI của T-010 đỏ.
   - Đề xuất của AI: ghi việc này vào mục "Việc dev làm trước khi kích hoạt" của danh sách chạy có T-010. Phiên kiểm tra bằng `curl` trước bước 3 và dừng task nếu thiếu (mục 7, điều 3).
6. **Catalog đổi giữa hai lần thử làm số file của bộ khởi đầu đổi.**
   - Đề xuất của AI: thanh tiến trình giữ mức đã đạt và chỉ tăng khi phần mới vượt qua, đúng quy tắc không lùi. Trường hợp này hiếm (cấu hình đổi đường dẫn catalog đúng giữa hai lần thử), không cần xử lý riêng.
7. **Phần trăm hiện trên màn hình với 60 file.** Mỗi file thêm khoảng 1,33%; phần nghìn chia lấy phần nguyên, phần trăm hiện được làm tròn (`roundToInt`) như hiện nay.
   - Đề xuất của AI: giữ cách này. Phần trăm có lúc đứng yên khi một file xong, nhưng không bao giờ lùi và đạt 100% đúng lúc file cuối xong.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-YYYYMMDD-NN, phiên bản plan 1

- **Kết quả:** đã merge / dừng / CI thất bại
- **Nhánh, PR:** <...>
- **Đã làm:** <...>
- **Test viết trước:** <số test, xác nhận đã thất bại đúng lý do trước khi viết code>
- **Rà soát độc lập:** <số điểm phát hiện, đã xử lý những gì, điểm nào còn để người xem>
- **Kết quả CI:** <build, lint, unit test, UI test, screenshot test>
- **Lệch so với plan:** <chỗ nào và vì sao, hoặc "Không có">
- **Việc còn lại:** <...>
- **Cần người xem:** <điểm dev nên kiểm tra sáng nay>
