---
ticket: T-097
title: Camera chờ quảng cáo toàn màn hình đóng rồi mới mở khi quay lại app
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 11               # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 15.0M        # cỡ M theo scripts/token-usage.py estimate (2026-11-13), không nâng tay
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high: dev còn lại (risk-scoring.md, mục 3)
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-097: CAMERA CHỜ QUẢNG CÁO TOÀN MÀN HÌNH ĐÓNG RỒI MỚI MỞ KHI QUAY LẠI APP

## 1. Tóm tắt

`CameraViewModel` nhận cờ "đang hiện quảng cáo toàn màn hình" qua một interface của `feature/camera` (`FullScreenAdCover`, interface theo mẫu `CountdownInterruptions`), phần cài đặt cấp ở `:app` bằng Hilt từ `AdsController.fullScreenAdShowing`, như `AdFreeModule`. Camera coi là bị che khi cờ bật, trừ khi cờ vẫn bật sau khi màn đã qua một lần `PAUSED` rồi `RESUMED` (cờ kẹt). Bị che thì camera đóng và khung xem trước về vòng chờ; hết che thì camera tự mở lại. Không sửa `:core:ads`.

Kiểm chứng bằng unit test của `CameraViewModel` (cả hai thứ tự giữa lúc cờ bật và `ON_RESUME`, cờ kẹt, mọi đường tới lời mở camera, ra nền giữa lúc chờ), test của module Hilt ở `:app`, test Robolectric của phần báo `RESUMED`, và thử trên máy thật theo FB-20261113-02 và với quảng cáo có thưởng.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md` (bản `changed` trong PR của plan này, chờ Nhật xác nhận, mục 8):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Mục 5 | Trong lúc bất kỳ quảng cáo toàn màn hình nào phủ lên camera (quảng cáo khi quay lại app, quảng cáo có thưởng mở khóa tư thế premium), camera không chạy; đóng quảng cáo thì camera mở lại như khi quay lại app thường | Áp cho `resume`, `resume_inter` và quảng cáo có thưởng của hộp thoại mở khóa (Nhật chọn ngày 2026-11-13) |
| KB-8, tiêu chí 4 (mới) | Khi một quảng cáo toàn màn hình phủ lên camera thì camera không chạy trong lúc quảng cáo hiện; quảng cáo đóng thì khung xem trước hiện vòng chờ rồi camera tự chạy lại, giữ tư thế và các lựa chọn | Bị che thì trạng thái về `Starting` (vòng chờ, nút chụp mờ theo KB-8, tiêu chí 1); mở lại giữ đèn flash, hẹn giờ, lưới, ống kính, tư thế; zoom về 1x như khi quay lại app. Camera đang báo lỗi mà bị che thì sau quảng cáo tự thử mở lại, như khi quay lại app |
| KB-1, tiêu chí 1 | Khi vào camera thì khung xem trước chạy | Không có quảng cáo che thì camera mở như hôm nay |
| KB-1, tiêu chí 8 | Mở khóa tư thế premium xong thì camera dùng tư thế đó | Không đổi; camera chạy lại sau quảng cáo có thưởng với tư thế vừa mở khóa |
| KB-8, tiêu chí 3 | Khi app ra nền rồi quay lại thì camera tự chạy lại, giữ tư thế và các lựa chọn | Không đổi khi không có quảng cáo; có quảng cáo thì chạy lại sau khi quảng cáo đóng |
| KB-9, tiêu chí 4 | Quảng cáo toàn app khi quay lại app có thể phủ lên camera | Không đổi luật hiện quảng cáo |

Phản hồi: [FB-20261113-02](../feedback/FB-20261113-02-camera-mo-duoi-quang-cao-resume.md) (phiên thử [TS-20261113-01](../../../test-sessions/TS-20261113-01.md), mục P-2).

**Thứ tự sự kiện khi quay lại app (đọc code, 2026-11-13).**

- `AdsActivityTracker.onActivityStarted` báo app vào foreground; `ResumeAdTurns` quyết định quảng cáo, chưa hiện. `LifecycleStartEffect` của `CameraRoute` gọi `onScreenStarted` ở `ON_START`; cờ còn `false`, lời mở camera bắt đầu.
- `AdsActivityTracker.onActivityResumed` (callback của `Application`) gọi `ResumeAdTurns.onAppActivityResumed`, rồi `scope.launch { showInterstitialTurn(...) }` hay `showAppOpen`, trên `scope` dùng `Dispatchers.Main.immediate` (`AdsController.kt`, khoảng dòng 151, 457 tới 462). Coroutine chạy ngay trong callback tới lời treo đầu tiên: `decideFullScreenTurn` đọc `isEnabled(placement).first()` và `flags.intervalSeconds(...).first()`, là `Flow` dựng từ `RemoteSettings`; treo hay không tùy giá trị đã có sẵn. Không treo thì `presence.tryBegin` (cờ bật) chạy ngay trong `onActivityResumed`, trước khi `Lifecycle` của activity tới `RESUMED` (trên API 29 trở lên `ON_RESUME` phát sau callback của `Application`, chú thích của `isHostUsable`, `AdsController.kt` khoảng dòng 1596 tới 1600). Có treo thì cờ bật sau `ON_RESUME`.
- Log gốc: `MainActivity` resume 08:50:23.830, `Resume ad shown on resume: resume_inter` 23.973 (cờ bật ngay sau dòng này), CameraX gắn camera 24.327 (`Open camera top activityName is ...AdActivity`), `Camera ready` 24.328, quảng cáo đóng 30.399 (`Full-screen turn finished: closed`, ghi ở `FullScreenCompletion.complete`, `FullScreenCompletion.kt:25`). Lần đó cờ bật sau resume 143 ms, tức có treo; lần khác có thể không.
- `AdActivity` của AdMob (cả quảng cáo có thưởng) trong suốt, nên `MainActivity` chỉ về `PAUSED`, vẫn `STARTED`.

Kết luận: plan phải đúng với **cả hai thứ tự** (cờ bật trước `ON_RESUME`, và sau `ON_RESUME`), và không được coi "màn `RESUMED`" là dấu hiệu hết che, vì ở thứ tự thứ nhất màn tới `RESUMED` khi quảng cáo đã bắt đầu, rồi mới `PAUSED` khi `AdActivity` lên. Đường chính là **hủy lời mở đang dở** ngay khi cờ bật. Nếu camera đã gắn xong trước đó (máy nhanh), camera chạy rất ngắn rồi đóng; dòng log của hệ thống lúc gắn có thể vẫn ghi `...AdActivity`, nên tiêu chí thử trên máy (mục 4) không dựa vào dòng đó.

Thiết kế tham chiếu: không đổi giao diện; trạng thái `Starting` dùng thiết kế `CameraScreen_starting_dark` có sẵn.

ADR liên quan: ADR-0045 (feature chỉ dùng quảng cáo qua composable và hàm của `:core:ads`, mục 4; feature không đọc `AdsController` hay `LocalAdsController`), ADR-0001 (feature không phụ thuộc `:app`; `:app` cấp phần cài đặt), ADR-0019 (log).

## 3. Giải pháp

### 3.1. Cách làm

**Interface và hàm thuần trong `feature/camera` (file mới `FullScreenAdCover.kt`):**

- `fun interface FullScreenAdCover { fun showing(): StateFlow<Boolean> }`: true khi một quảng cáo toàn màn hình của app đang hiện. `StateFlow` để `CameraViewModel` có giá trị ngay khi tạo.
- `fun adCoversCamera(adShowing: Boolean, resumed: Boolean, pausedSinceAdShown: Boolean): Boolean = adShowing && !(resumed && pausedSinceAdShown)`.
- `fun cameraMayOpen(started: Boolean, covered: Boolean): Boolean = started && !covered`.
- Composable `CameraResumeEffect(onResumed: () -> Unit, onPaused: () -> Unit)`: `LifecycleResumeEffect(Unit) { onResumed(); onPauseOrDispose { onPaused() } }`, giữ callback mới nhất bằng `rememberUpdatedState`.

**Chốt chặn cờ kẹt: "cờ vẫn bật sau một lần `PAUSED` rồi `RESUMED`".** Cờ công khai `showing` của `FullScreenAdPresence` chỉ hạ khi lượt kết thúc (callback đóng, lỗi hiện, `endCurrentTurn`). `SHOWING_CAP_MILLIS` chỉ dùng trong `isBusy()`, không hạ `showing`. `StuckFullScreenRecovery` chỉ gỡ lượt có activity chủ, không gỡ lượt `NO_HOST_ACTIVITY` (native toàn màn hình, combo sau inter, quảng cáo có thưởng khi không có activity). Tin cờ một mình thì một lượt kẹt làm camera tối mãi. Quảng cáo SDK thật làm màn `PAUSED` khi `AdActivity` lên, và màn chỉ `RESUMED` lại khi `AdActivity` đã đi; nên cờ vẫn bật sau một lần `PAUSED` rồi `RESUMED` là cờ kẹt, camera bỏ qua nó. Còn `ON_START` → cờ bật → `ON_RESUME` mà chưa qua `PAUSED` (thứ tự thứ nhất ở mục 2) thì vẫn là bị che. Biến `pausedSinceAdShown` về `false` mỗi khi cờ đổi từ `false` sang `true` (và khi tạo view model), thành `true` ở `onScreenPaused` hay `onScreenStopped` lúc cờ đang bật.

Hệ quả: lớp native toàn màn hình vẽ trong cùng `MainActivity` (`FullScreenAdHost`) bật cờ mà màn không `PAUSED`, nên camera cũng không chạy dưới nó (đúng mục 5). Nếu một lượt như vậy kẹt (cờ không hạ), camera tối tới khi màn qua một lần `PAUSED` rồi `RESUMED` (người dùng ra nền rồi quay lại, hay mở một hộp thoại của hệ thống); mục 8, câu 3.

**`CameraViewModel`:**

- Nhận thêm `fullScreenAdCover: FullScreenAdCover` (constructor). Trong `init`, `viewModelScope.launch { fullScreenAdCover.showing().collect { onFullScreenAdChanged(it) } }`, nên giá trị hiện tại có trước `ON_START` đầu tiên.
- Biến trạng thái riêng, không suy từ `uiState`:
  - `screenStarted`: giữa `onScreenStarted` và `onScreenStopped`.
  - `screenResumed`: giữa `onScreenResumed` và `onScreenPaused`; `onScreenStopped` cũng đặt `false`.
  - `adShowing`, `pausedSinceAdShown`: như trên.
  - `cameraWanted`: đã có quyền camera và màn muốn camera chạy (đặt ở `onScreenStarted(true)`, `onPermissionResult(granted = true)`, `onRetry`, `onFlipClick`; bỏ ở `onScreenStopped`).
  - `cameraActive`: từ lúc `openCamera` bắt đầu tới `controller.close()` hay tới khi lời mở báo lỗi.
  - `waitingForUncover`: true khi camera đóng vì bị che, khi một lời mở bị bỏ vì bị che, hay khi màn bị che lúc camera đang báo lỗi; false khi mở lại hay `onScreenStopped`. Quyết lần mở lại sau quảng cáo và dòng log mở lại.
  - `covered` tính bằng `adCoversCamera(adShowing, screenResumed, pausedSinceAdShown)`.
- Mọi lời mở camera (`onScreenStarted`, `onPermissionResult`, `onRetry`, `onFlipClick`) đi qua `openCameraIfAllowed(keepReady)`: kiểm `cameraMayOpen(screenStarted, covered)` ngay trước khi gọi `openCamera`; không được thì `waitingForUncover = true`, trạng thái `Starting`, log `Camera waits for the full-screen ad`, không gọi `controller.open`.
- `onFullScreenAdChanged(showing)`: cùng giá trị thì bỏ qua (dòng phòng thủ: `StateFlow` không phát lại giá trị trùng, nhưng giữ để một nguồn khác không đặt lại `pausedSinceAdShown`); đổi sang `true` thì `pausedSinceAdShown = false`; rồi `reconcile()`. `onScreenResumed`, `onScreenPaused` cũng gọi `reconcile()`.
- `reconcile()`:
  - Màn đang chạy, `cameraWanted` và vừa thành bị che: nếu `cameraActive` thì `cancelCountdown(reason = "full_screen_ad")`, hủy `openJob`, `controller.close()`, `cameraActive = false`, trạng thái `Starting`, log `Camera closed: full-screen ad`; dù camera đang mở hay đang báo lỗi, `waitingForUncover = true`.
  - Không bị che, `waitingForUncover`, `cameraWanted` và `cameraMayOpen(screenStarted, false)`: log `Camera reopens after the full-screen ad`, `waitingForUncover = false`, `openCamera()` (về `Starting`, `Ready` khi gắn xong). Không `waitingForUncover` thì không mở thêm: camera đang báo lỗi mà không bị che vẫn chờ nút "Thử lại" như hôm nay.
- `onScreenStopped` giữ việc đóng camera như hôm nay, thêm đặt lại `screenStarted`, `screenResumed`, `cameraActive`, `waitingForUncover`; `pausedSinceAdShown = true` nếu cờ đang bật.
- Nút chụp trong khoảng từ lúc bị che tới lúc camera gắn lại: trạng thái `Starting`, `onShutterClick` chỉ chạy với `Ready` nên bị bỏ qua; nút chụp mờ theo KB-8, tiêu chí 1.
- Nút đổi camera lúc bị che: `onFlipClick` dừng ở `state.value as? Ready ?: return` như hôm nay (trạng thái là `Starting`), nên lần chạm bị bỏ qua, ống kính giữ nguyên; không đổi điều kiện đầu của `onFlipClick` (chỉ lời mở cuối hàm đi qua `openCameraIfAllowed`).
- Lần chụp đang chạy khi bị che: không xảy ra ở luồng quay lại app (ra nền đã đóng camera) hay quảng cáo có thưởng (mở từ hộp thoại, không lúc chụp); nếu có thì đóng như `onScreenStopped` hôm nay (ảnh báo lỗi chụp).

**`CameraScreen.kt`:** `CameraRoute` gọi `CameraResumeEffect(viewModel::onScreenResumed, viewModel::onScreenPaused)` cạnh `LifecycleStartEffect` có sẵn. Không đọc `LocalAdsController`.

**`:app`, module Hilt mới `CameraAdCoverModule`** (cách cấp như `AdFreeModule`): `@Provides fun fullScreenAdCover(ads: AdsController): FullScreenAdCover = FullScreenAdCover { ads.fullScreenAdShowing }`, `@InstallIn(SingletonComponent::class)`. `AdsController` là singleton `@Inject` có sẵn. `CameraModule` của `feature/camera` không đổi.

**Không sửa `:core:ads`**: chỉ đọc `AdsController.fullScreenAdShowing` công khai, ở `:app` như `MainActivity` đang làm cho hộp thoại mất mạng.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/FullScreenAdCover.kt` | Interface `FullScreenAdCover`, hàm thuần `adCoversCamera`, `cameraMayOpen`, composable `CameraResumeEffect` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModel.kt` | Tham số `fullScreenAdCover`, thu cờ trong `init`, `onScreenResumed`, `onScreenPaused`, `openCameraIfAllowed`, `reconcile`, các biến của mục 3.1, ba dòng log mới |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | Gọi `CameraResumeEffect` trong `CameraRoute` |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/di/CameraAdCoverModule.kt` | Cấp `FullScreenAdCover` từ `AdsController.fullScreenAdShowing` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/di/CameraAdCoverModuleTest.kt` | Test của module Hilt (mục 4) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraFakes.kt` | `FakeFullScreenAdCover` (một `MutableStateFlow(false)`) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModelTest.kt` | Truyền `FakeFullScreenAdCover` khi dựng `CameraViewModel`; test của mục 4 |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/FullScreenAdCoverTest.kt` | Test hai hàm thuần và test Robolectric của `CameraResumeEffect` |
| Sửa | `docs/ops/manual-test.md` | Mục 11.3 (quảng cáo khi quay lại app) và mục 11.6 (quảng cáo có thưởng): bước thử camera dưới quảng cáo (mục 4) |

Commit test của bước 3 gồm `FakeFullScreenAdCover` trong `CameraFakes.kt`, tham số constructor trong phần dựng của `CameraViewModelTest`, và khung rỗng ở code chính để biên dịch được: interface `FullScreenAdCover`, hai hàm thuần trả `false`/`true` cố định, `CameraResumeEffect` rỗng, tham số constructor chưa dùng, `onScreenResumed`, `onScreenPaused` rỗng, module Hilt ở `:app`. Code thật vào commit của bước 4.

Spec camera (`docs/features/camera/spec.md`), ticket T-097 và phản hồi FB-20261113-02 đổi trong PR của plan này, không trong phiên.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Đọc cờ trạng thái `AdsController.fullScreenAdShowing` ở `:app` (không phải cờ tính năng hay khóa Remote Config).
- Quảng cáo: Không chạm vị trí, định dạng hay tần suất. Camera chỉ không chạy trong lúc quảng cáo toàn màn hình phủ lên.
- Sự kiện phân tích: Không chạm. Thêm ba dòng log `VGL.Camera` (ADR-0019), không gửi đi.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. Không thêm: `CAMERA` đã có (spec camera, mục 6).
- **Thư viện và SDK:** Không thêm. `LifecycleResumeEffect` thuộc `androidx.lifecycle:lifecycle-runtime-compose`, đã dùng cho `LifecycleStartEffect` trong `CameraScreen.kt`; `mockk` có sẵn trong `testImplementation` của mọi module (convention plugin), dùng cho test của `:app`.

### 3.4. Phương án dự phòng

Không có. Điểm còn chưa chắc (camera có thể gắn xong rồi mới đóng khi máy nhanh) không có cách làm thay chạy được trong phiên mà không sửa `:core:ads`; ghi ở mục 8, câu 2, cùng việc tiếp theo.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (2026-11-13): 12 test ở nhóm "Cần xem trước" đều là `LoadingScreenTest`, chỉ trùng tên tham số `onRetry` của màn loading, không dùng `CameraViewModel`; plan không đổi chúng. Nhóm "Nên xem" có 80 test: 33 của `CameraViewModelTest` (bảng dưới); 4 test của `CameraScreenTest`, `CameraLastPhotoTest`, `CameraUnfoldedScreenshotTest` chỉ truyền callback vào `CameraScreen`, không đổi; `LeaveAppCallSitesTest` quét chỗ rời app trong `CameraScreen.kt`, plan không thêm chỗ rời app; 42 test còn lại của màn khác (loading, onboarding, network, poses, review, last photo) chỉ trùng tên hàm như `onRetry`, `onScreenStarted`, không dùng `CameraViewModel`.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `onScreenStarted(true)` luôn mở camera | Mở khi `cameraMayOpen(started, covered)`; cờ `false` thì như cũ | `CameraViewModelTest.stoppedThenStarted_closesThenReopensWithSameTools` và các test dùng `readyViewModel()` | Giữ: `FakeFullScreenAdCover` mặc định `false`; chỉ đổi phần dựng (thêm tham số constructor), không đổi câu khẳng định |
| `onRetry`, `onFlipClick`, `onPermissionResult(true)` luôn mở camera | Đi qua cùng cổng | Các test của ba hàm trong `CameraViewModelTest` | Giữ, cùng lý do |
| Camera chạy dưới quảng cáo có thưởng khi mở khóa tư thế | Camera đóng, về `Starting`, mở lại khi quảng cáo đóng | `LockedPoseUnlockTest` (không dựng `CameraViewModel`) | Giữ: không có câu khẳng định về camera |

## 4. Kiểm thử

Test viết trước dùng `FakeCameraController` (`openCalls`, `closeCount`, `pendingOpen`, `openError`, `captureCount`), `FakeFullScreenAdCover` và `RecordingAppLogger`. Mỗi test "không mở" có đối chứng: cùng bước với cờ `false` thì camera mở.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Hai hàm thuần | Unit test | `FullScreenAdCoverTest.adCoversCamera_truthTable`: đủ 8 tổ hợp; chỉ `adShowing` mà chưa (`resumed` và `pausedSinceAdShown`) là `true`. `FullScreenAdCoverTest.cameraMayOpen_truthTable`: 4 tổ hợp |
| Giá trị cờ có trước `ON_START` đầu tiên | Unit test | `CameraViewModelTest.adAlreadyShowingAtCreation_firstStartWaits`: cờ `true` trước khi dựng view model; `onScreenStarted(true)`, `onScreenResumed()`: `openCalls` rỗng, `Starting`, log `Camera waits for the full-screen ad`. Đối chứng: cờ `false` thì mở |
| Thứ tự thứ nhất: cờ bật trước `ON_RESUME` | Unit test | `CameraViewModelTest.adShownBeforeResume_staysClosedUntilAdCloses`: `pendingOpen` chưa xong; `onScreenStarted(true)`; cờ `true`: `closeCount == 1`, `Starting`, log `Camera closed: full-screen ad`; `onScreenResumed()`: `openCalls.size == 1` (không mở lại); `onScreenPaused()`; cờ `false`: `openCalls.size == 2`, log `Camera reopens after the full-screen ad`; hoàn tất lời mở: `Ready`. Không có log `Camera ready` giữa `Camera closed` và lúc cờ `false` |
| Thứ tự thứ hai (log gốc): cờ bật sau `ON_RESUME` | Unit test | `CameraViewModelTest.adShownAfterResume_closesAtOnce`: `pendingOpen` chưa xong; `onScreenStarted(true)`, `onScreenResumed()`, cờ `true`: `closeCount == 1` ngay (không chờ `PAUSED`), hoàn tất `pendingOpen` không đưa về `Ready`; `onScreenPaused()`, cờ `false`: mở lại một lần |
| Quảng cáo đóng: màn resume trước khi cờ hạ | Unit test | `CameraViewModelTest.resumedBeforeFlagDrops_reopensOnce`: tiếp test trên, sau `onScreenPaused()` thì `onScreenResumed()` khi cờ vẫn `true`: mở lại một lần (cờ coi là đã xong); cờ `false` sau đó không mở thêm (`openCalls.size == 2`) |
| KB-8 tiêu chí 4: mở lại giữ lựa chọn | Unit test | `CameraViewModelTest.adClosed_reopensWithSameTools`: `readyViewModel()` có lưới, hẹn giờ; `onScreenResumed()`, cờ `true`: `closeCount == 1`, `Starting`; `onScreenPaused()`, cờ `false`: `openCalls.size == 2`, `Ready`, lưới và hẹn giờ giữ |
| Cờ kẹt thật | Unit test, chặn hồi quy | `CameraViewModelTest.flagStuckAfterPauseAndResume_cameraRuns`: `readyViewModel()`, `onScreenResumed()`, cờ `true` (đóng), `onScreenPaused()`, `onScreenResumed()` với cờ vẫn `true`: mở lại, `Ready`. Đột biến: đổi `adCoversCamera` thành `adShowing` thì test đỏ (camera không mở) |
| Nút chụp trong lúc mở lại | Unit test | `CameraViewModelTest.shutterWhileReopening_isIgnored`: sau khi đóng vì bị che và trước khi lời mở lại xong: `onShutterClick()`: `captureCount == 0` |
| Đếm ngược dừng khi bị che | Unit test | `CameraViewModelTest.coveredDuringCountdown_cancelsCountdown`: hẹn giờ 3 giây, bấm chụp, cờ `true`: `captureCount == 0`, log `Countdown cancelled: full_screen_ad` |
| Đổi camera lúc bị che | Unit test | `CameraViewModelTest.flipWhileCovered_isIgnored`: `readyViewModel()`, cờ `true`, `onFlipClick()`: ống kính lưu không đổi (`lensSaves` rỗng); cờ `false`: mở đúng một lần với ống kính cũ |
| Thử lại, quyền lúc bị che | Unit test | `CameraViewModelTest.retryWhileCovered_doesNotOpen`, `permissionGrantedWhileCovered_doesNotOpen`: không tăng `openCalls`, log `Camera waits for the full-screen ad`; cờ `false`: mở đúng một lần |
| Camera đang lỗi mà bị che | Unit test | `CameraViewModelTest.errorThenCovered_retriesAfterAd`: `openError` đặt sẵn, `onScreenStarted(true)`: `Error`; cờ `true`; bỏ `openError`; cờ `false`: `openCalls.size == 2`, `Ready`, log `Camera reopens after the full-screen ad`. Đối chứng: `Error` không bị che thì cờ không đổi, không mở thêm |
| Ra nền rồi trở lại khi quảng cáo vẫn còn | Unit test | `CameraViewModelTest.stoppedWhileCovered_restartWithAdStillShowing_waits`: `onScreenResumed()`, cờ `true` (đóng), `onScreenStopped()`, `onScreenStarted(true)` khi cờ vẫn `true` và chưa resume: `openCalls` không tăng (kiểm `onScreenStopped` đặt lại `screenResumed`); cờ `false`: mở một lần |
| Ra nền giữa lúc chờ, quảng cáo đóng khi ở nền | Unit test | `CameraViewModelTest.adClosedWhileStopped_opensOnlyOnNextStart`: `readyViewModel()`, `onScreenResumed()`, cờ `true`: `closeCount == 1`, `Starting`, log `Camera closed: full-screen ad`; `onScreenStopped()`, cờ `false`: `openCalls.size == 1` (không mở khi ở nền); `onScreenStarted(true)`: `openCalls.size == 2` |
| Cờ bật rồi hạ khi màn đang `RESUMED` (lớp phủ trong cùng activity) | Unit test, chặn hồi quy | `CameraViewModelTest.coverWhileResumed_closesThenReopens`: `readyViewModel()`, `onScreenResumed()`, cờ `true`: `closeCount == 1`; cờ `false`: `openCalls.size == 2`, `Ready`. Đột biến: bỏ phần "chưa `PAUSED` từ lúc cờ bật" thì camera không đóng và test đỏ |
| Phần nối DI ở `:app` | Unit test (`:app`) | `CameraAdCoverModuleTest.fullScreenAdCover_followsAdsControllerFlag`: `mockk<AdsController>` chỉ khai báo `fullScreenAdShowing` (một `MutableStateFlow(false)`); thuộc tính khác chưa khai báo nên mockk ném lỗi nếu module đọc nhầm; `CameraAdCoverModule.fullScreenAdCover(ads).showing()` là cùng đối tượng, đổi giá trị thì thấy giá trị mới. Thiếu binding thì Hilt báo lỗi lúc build `:app` |
| Phần báo `RESUMED`, ra nền rồi trở lại | Compose UI test (Robolectric, `createAndroidComposeRule<ComponentActivity>()`) | `FullScreenAdCoverTest.resumeEffect_reportsPauseAndResume`: nội dung `CameraResumeEffect(record("resumed"), record("paused"))`; `moveToState(STARTED)`, `moveToState(CREATED)`, `moveToState(RESUMED)`: bản ghi `[resumed, paused, resumed]` |
| KB-1, KB-8 không đổi khi không có quảng cáo | Unit test | Test có sẵn của `CameraViewModelTest` qua với câu khẳng định nguyên (mục 3.5) |
| Mục 5 trên máy: quảng cáo khi quay lại app | Kiểm tra thủ công (máy thật: khác, cần đưa app ra nền rồi mở lại từ biểu tượng; trình chạy kịch bản không có lệnh về màn hình chính) | `docs/ops/manual-test.md`, mục 11.3. `resume_ad_type` là `inter` trên Firebase staging (bản staging bật `show_resum_ads`). Mở app tới camera, về màn hình chính 2 giây, mở lại. Trong lúc quảng cáo còn trên màn: chấm báo camera của Android tắt; log có `Camera closed: full-screen ad` hay `Camera waits for the full-screen ad`. `Camera reopens after the full-screen ad` và `Camera ready` chỉ có sau khi quảng cáo đã đóng trên màn hình (người thử nhìn thấy; trong log, gần dòng `Full-screen turn finished: closed` do `FullScreenCompletion.kt:25` ghi khi lượt kết thúc). Tư thế và lựa chọn giữ nguyên. Lặp lại với `resume_ad_type` mặc định (app open) |
| Mục 5 trên máy: quảng cáo có thưởng | Kiểm tra thủ công (máy thật: khác, quảng cáo có thưởng thử của AdMob trên bản staging) | `docs/ops/manual-test.md`, mục 11.6. Chạm ô tư thế premium, xem quảng cáo: chấm báo camera tắt, log `Camera closed: full-screen ad`; xem xong: `Camera reopens after the full-screen ad`, `Camera ready` với tư thế vừa mở khóa |

**Ngoại lệ viết test trước:** dòng gọi `CameraResumeEffect(viewModel::onScreenResumed, viewModel::onScreenPaused)` trong `CameraRoute`, vì `CameraRoute` cần Hilt và không test nào dựng nó (như `LifecycleStartEffect` có sẵn ngay cạnh); hai phía của dòng nối có test (`FullScreenAdCoverTest`, `CameraViewModelTest`), và hai bước thử trên máy thật kiểm dòng nối. Bước của `docs/ops/manual-test.md` là tài liệu.

### 4.1. Chạy thử

- **Đã chạy thử (1):** test tạm không commit `TmpCoverProbeTest` (2026-11-13, trên `origin/main` `8af6a3e`): `createAndroidComposeRule<ComponentActivity>()`, `LaunchedEffect` thu một `MutableStateFlow`, `moveToState(CREATED)` rồi `RESUMED`. Lệnh `./gradlew :feature:camera:testDebugUnitTest --tests '*TmpCoverProbeTest*'`: qua; bản ghi `[false, true, false]`. Cho thấy `moveToState` của `createAndroidComposeRule` chạy được trong `feature/camera` trên máy dev.
- **Đã chạy thử (2):** test tạm không commit `TmpAdsMockProbeTest` trong `:app` (2026-11-13): `mockk<AdsController> { every { fullScreenAdShowing } returns flow }`, `assertSame(flow, ads.fullScreenAdShowing)`. Lệnh `./gradlew :app:testStagingDebugUnitTest --tests '*TmpAdsMockProbeTest*'`: qua; mockk tạo được bản giả của lớp `final` `AdsController` (constructor `internal`) mà không chạy constructor.
- **Thứ tự cờ và `ON_RESUME`:** chốt bằng đọc code (mục 2), không chạy thử: thứ tự tùy `Flow` của `RemoteSettings` có treo hay không, nên plan xử lý và test cả hai thứ tự.
- **Kết quả:** hai test tạm đã xóa. Chưa chạy `scripts/check.sh` (E dưới 2, mục 5).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có. Không thêm, bớt hay đổi vị trí, định dạng, tần suất quảng cáo (điểm 1); không sửa `:core:*` (điểm 7).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module: `feature/camera` (interface mới, đổi constructor và hàm công khai của `CameraViewModel`) và `:app` (module Hilt mới) |
| B. Độ rõ của yêu cầu | 2 | Nhật đã chọn cách làm và phạm vi, nhưng spec camera đang `changed` (mục 5, KB-8 tiêu chí 4) chờ xác nhận; plan chờ spec chốt (mục 8, câu 1) |
| C. Khả năng kiểm chứng tự động | 3 | Vòng đời ứng dụng và thứ tự với quảng cáo thật; khoảng thời gian giữa lời mở camera và lúc cờ bật chỉ kiểm được trên máy thật |
| D. Ảnh hưởng chéo | 2 | `docs/ops/manual-test.md` cùng được T-093, T-094 sửa (mục Quảng cáo) |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu `CountdownInterruptions` và `LifecycleStartEffect`; lần đầu dùng `LifecycleResumeEffect` và mockk trong test của `:app` (đã chạy thử, mục 4.1) |
| **Tổng** | **11** | **Mức: Cao**, cần duyệt chéo |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: đọc `AdsController.fullScreenAdShowing` của `:core:ads` ở `:app` (không sửa); thêm một module Hilt ở `:app`.
- Spec camera `changed`: `scripts/run-check.py` từ chối mọi danh sách chạy có plan của feature camera (T-096, T-097) tới khi Nhật xác nhận lại spec. Thứ tự: Nhật xác nhận spec camera trước khi lập danh sách chạy có T-096 hay T-097.
- Task khác trong sprint có thể xung đột: T-093, T-094 cùng thêm bước vào `docs/ops/manual-test.md`, mục Quảng cáo (khác đoạn); T-094, T-096 sửa `review/ReviewScreen.kt`, T-093, T-094 sửa `core/ads/.../AdPlacement.kt`, plan này không chạm hai file đó.
- Đề xuất thứ tự chạy: độc lập về code; xếp trước hay sau T-093, T-094 trong cùng danh sách (không song song) vì cùng `docs/ops/manual-test.md`.

## 7. Điều kiện dừng riêng

- Cần sửa file trong `core/ads/` để camera đọc được cờ hay để chốt chặn cờ kẹt chạy: dừng, vì plan chọn không sửa `:core:*`; ghi lý do để plan phiên bản mới xét.
- Feature `camera` cần đọc `AdsController` hay `LocalAdsController`: dừng (trái ADR-0045, mục 4).
- Một test có sẵn của `:core:ads` hay `AppAdsRootTest` đổi kết quả: dừng, vì plan không đổi hành vi của quảng cáo.
- Test của mục 4 cho thấy cờ kẹt làm camera không chạy cả sau một lần `PAUSED` rồi `RESUMED`, hay camera mở lại khi quảng cáo còn trên màn ở thứ tự thứ nhất: dừng.

## 8. Câu hỏi cho người duyệt

1. **Chờ xác nhận spec.** PR của plan này đổi `features/camera/spec.md` (mục 5, KB-8 tiêu chí 4 mới) sang `changed` theo lựa chọn của Nhật ngày 2026-11-13: camera không chạy khi bất kỳ quảng cáo toàn màn hình nào phủ lên, kể cả quảng cáo có thưởng mở khóa tư thế. Plan chỉ duyệt được sau khi Nhật xác nhận spec; spec còn `changed` thì `run-check.py` từ chối danh sách có T-096 hay T-097 (mục 6).
2. **Khoảng mở rồi đóng ngắn (rủi ro còn lại, việc tiếp theo).** Lời mở camera bắt đầu ở `ON_START`, trước khi biết có quảng cáo. Máy nhanh có thể gắn camera xong trước khi cờ bật, nên camera chạy rất ngắn rồi đóng. Không có cách làm thay trong phiên mà không sửa `:core:ads`. Nếu thử trên máy vẫn thấy chấm báo camera trong lúc quảng cáo hiện, việc tiếp theo là đề xuất về dự án mẫu một cờ công khai "quảng cáo khi quay lại app đang chờ hiện" (từ lúc vào foreground tới khi lượt bắt đầu hay bị bỏ), để camera chờ cả lúc đó (`docs/ops/from-app.md`). Đề xuất của AI: chấp nhận ở plan này.
3. **Giới hạn của chốt chặn cờ kẹt.** Một lượt kẹt mà màn không qua `PAUSED` rồi `RESUMED` (lớp native toàn màn hình trong cùng activity, hay lượt `NO_HOST_ACTIVITY` không hạ cờ) làm camera tối tới khi người dùng ra nền rồi quay lại hay một hộp thoại của hệ thống hiện. Trên Android 8 tới 9 (API 26 tới 28) ở chế độ chia đôi màn hình, màn camera không có tiêu điểm thì `PAUSED`, nên khi cờ kẹt camera tối trong lúc người dùng ở cửa sổ kia, và chạy lại khi chạm vào cửa sổ camera. Combo native sau inter giữ cờ liền mạch từ inter sang lớp native trong `MainActivity`; nếu sau này có combo phủ camera thì `MainActivity` resume khi inter đóng (qua `PAUSED` rồi `RESUMED`, cờ vẫn bật) và camera sẽ mở lại dưới lớp native. Hôm nay `resume_inter` không mở combo và `review_interstitial` ở màn khác; xét lại khi thêm combo trên camera. Đề xuất của AI: chấp nhận; cờ kẹt là lỗi của lượt quảng cáo, sửa ở `:core:ads` nếu gặp.
4. **Người duyệt.** Rủi ro Cao: cần người duyệt và người duyệt chéo (dev còn lại); nhóm một dev thì ghi lý do ở mục 9 (`risk-scoring.md`, mục 3).

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
