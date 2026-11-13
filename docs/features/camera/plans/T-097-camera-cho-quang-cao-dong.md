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

`CameraViewModel` nhận cờ "đang hiện quảng cáo toàn màn hình" qua một interface của `feature/camera` (`FullScreenAdCover`), do `:app` cấp bằng Hilt từ `AdsController.fullScreenAdShowing`, theo mẫu `CountdownInterruptions`. Camera coi là bị che khi cờ bật và màn camera không ở trạng thái `RESUMED` (quảng cáo là một activity khác phủ lên); bị che thì camera đóng và khung xem trước về vòng chờ, hết che thì camera tự mở lại. Không sửa `:core:ads`.

Kiểm chứng bằng unit test của `CameraViewModel` (mọi đường tới lời mở camera, thứ tự sự kiện của log gốc, cờ kẹt, ra nền giữa lúc chờ), test của module Hilt ở `:app`, test Robolectric của phần báo `RESUMED`, và thử trên máy thật theo FB-20261113-02.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md` (bản `changed` trong PR của plan này, chờ Nhật xác nhận, mục 8):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Mục 5 | Trong lúc bất kỳ quảng cáo toàn màn hình nào phủ lên camera (quảng cáo khi quay lại app, quảng cáo có thưởng mở khóa tư thế premium), camera không chạy; đóng quảng cáo thì camera mở lại như khi quay lại app thường | Áp cho mọi quảng cáo toàn màn hình phủ lên camera: `resume`, `resume_inter`, quảng cáo có thưởng của hộp thoại mở khóa (Nhật chọn ngày 2026-11-13) |
| KB-8, tiêu chí 4 (mới) | Khi một quảng cáo toàn màn hình phủ lên camera thì camera không chạy trong lúc quảng cáo hiện; quảng cáo đóng thì khung xem trước hiện vòng chờ rồi camera tự chạy lại, giữ tư thế và các lựa chọn | Đóng vì quảng cáo thì trạng thái về `Starting` (vòng chờ, nút chụp mờ theo KB-8, tiêu chí 1); mở lại giữ đèn flash, hẹn giờ, lưới, ống kính, tư thế; zoom về 1x như khi quay lại app |
| KB-1, tiêu chí 1 | Khi vào camera thì khung xem trước chạy | Không có quảng cáo che thì camera mở như hôm nay, không chờ thêm |
| KB-1, tiêu chí 8 | Mở khóa tư thế premium xong thì camera dùng tư thế đó | Không đổi; camera chạy lại sau quảng cáo có thưởng với tư thế vừa mở khóa |
| KB-8, tiêu chí 3 | Khi app ra nền rồi quay lại thì camera tự chạy lại, giữ tư thế và các lựa chọn | Không đổi khi không có quảng cáo; có quảng cáo thì chạy lại sau khi quảng cáo đóng |
| KB-9, tiêu chí 4 | Quảng cáo toàn app khi quay lại app có thể phủ lên camera | Không đổi luật hiện quảng cáo |

Phản hồi: [FB-20261113-02](../feedback/FB-20261113-02-camera-mo-duoi-quang-cao-resume.md) (phiên thử [TS-20261113-01](../../../test-sessions/TS-20261113-01.md), mục P-2).

Cách đọc log gốc: `MainActivity` resume lúc 08:50:23.830; quảng cáo bắt đầu (cờ bật) lúc 23.973 (`Resume ad shown on resume: resume_inter`); CameraX gắn camera lúc 24.327 (`Open camera top activityName is ...AdActivity`) và `Camera ready` lúc 24.328; quảng cáo đóng lúc 30.399. Lời mở camera bắt đầu ở `ON_START` (trước 23.830) và chưa xong khi quảng cáo đã phủ lên, nên đường chính của plan là **hủy lời mở đang dở** khi màn bị che. Nếu camera đã gắn xong trước khi bị che (máy nhanh hơn), camera chạy một lúc rất ngắn rồi đóng; khi đó dòng log của hệ thống ở lần gắn có thể vẫn ghi `...AdActivity` nếu activity quảng cáo đã lên trên, nên tiêu chí thử trên máy (mục 4) không dựa vào dòng đó mà dựa vào việc không còn camera chạy trong lúc quảng cáo hiện sau dòng `Camera closed: full-screen ad`.

Thiết kế tham chiếu: không đổi giao diện; trạng thái `Starting` dùng thiết kế `CameraScreen_starting_dark` có sẵn.

ADR liên quan: ADR-0045 (feature chỉ dùng quảng cáo qua composable và hàm của `:core:ads`, mục 4; feature không đọc `AdsController` hay `LocalAdsController`), ADR-0001 (feature không phụ thuộc `:app`; `:app` cấp phần cài đặt), ADR-0019 (log).

## 3. Giải pháp

### 3.1. Cách làm

**Thứ tự sự kiện khi quay lại app (đọc từ code).** `AdsActivityTracker.onActivityStarted` báo app vào foreground và `ResumeAdTurns` quyết định quảng cáo. `LifecycleStartEffect` của `CameraRoute` gọi `onScreenStarted` ở `ON_START`, lúc cờ còn `false`. Ở `onActivityResumed`, `ResumeAdTurns.onAppActivityResumed` gọi `showAppOpen` hay `showInterstitial`; cờ bật khi lượt bắt đầu (`presence.tryBegin`), sau `ON_START` và gần `ON_RESUME`. `AdActivity` của AdMob trong suốt nên `MainActivity` chỉ về `PAUSED` (vẫn `STARTED`). Quảng cáo có thưởng cũng là `AdActivity` (`raisePresence` của `RewardedTurns`).

**Interface trong `feature/camera` (file mới `FullScreenAdCover.kt`):**

- `fun interface FullScreenAdCover { fun showing(): StateFlow<Boolean> }`: true khi một quảng cáo toàn màn hình của app đang hiện. `StateFlow` để `CameraViewModel` có giá trị ngay khi tạo.
- Hàm thuần `cameraMayOpen(started: Boolean, resumed: Boolean, adShowing: Boolean): Boolean = started && !(adShowing && !resumed)`.
- Composable `CameraResumeEffect(onResumed: () -> Unit, onPaused: () -> Unit)`: `LifecycleResumeEffect(Unit) { onResumed(); onPauseOrDispose { onPaused() } }`, giữ callback mới nhất bằng `rememberUpdatedState`.

**Vì sao "cờ bật và màn không `RESUMED`" (chốt chặn cờ kẹt).** Cờ công khai `showing` của `FullScreenAdPresence` chỉ hạ khi lượt kết thúc (callback đóng, lỗi hiện, `endCurrentTurn`). `SHOWING_CAP_MILLIS` chỉ dùng trong `isBusy()`, không hạ `showing`. `StuckFullScreenRecovery` chỉ gỡ lượt có activity chủ (so mã activity), không gỡ lượt `NO_HOST_ACTIVITY` (native toàn màn hình, combo sau inter, quảng cáo có thưởng khi không có activity). Nếu camera tin cờ một mình, một lượt kẹt làm camera tối mãi. Quảng cáo toàn màn hình có thật thì là activity khác phủ lên, nên màn camera không `RESUMED`; còn màn đang `RESUMED` thì người dùng đang thấy camera, cờ có bật cũng là cờ kẹt hay lớp phủ trong cùng activity. Vì vậy camera chỉ coi là bị che khi cả hai điều kiện cùng đúng. Giới hạn đã biết: lớp native toàn màn hình vẽ trong cùng activity (`FullScreenAdHost`) không làm màn `PAUSED`, nên camera vẫn chạy dưới nó; hôm nay lớp này chỉ hiện ở màn xem lại (combo sau `review_interstitial`) và luồng giới thiệu, không hiện trên camera (mục 8, câu 3).

**`CameraViewModel`:**

- Nhận thêm `fullScreenAdCover: FullScreenAdCover` (constructor). Trong `init`, `viewModelScope.launch { fullScreenAdCover.showing().collect { onFullScreenAdChanged(it) } }`, nên giá trị hiện tại có trước `ON_START` đầu tiên.
- Biến trạng thái riêng, không suy từ `uiState`:
  - `screenStarted`: giữa `onScreenStarted` và `onScreenStopped`.
  - `screenResumed`: giữa `onScreenResumed` và `onScreenPaused`.
  - `adShowing`: giá trị cờ mới nhất.
  - `cameraWanted`: đã có quyền camera và màn muốn camera chạy (đặt ở `onScreenStarted(true)`, `onPermissionResult(granted = true)`, `onRetry`, `onFlipClick`; bỏ ở `onScreenStopped`).
  - `cameraActive`: từ lúc `openCamera` bắt đầu tới `controller.close()`.
  - `closedForFullScreenAd`: true từ lúc camera đóng vì bị che tới lần mở lại hay `onScreenStopped`; chỉ để ghi log mở lại và để biết lần mở sau là sau quảng cáo.
- Mọi lời mở camera (`onScreenStarted`, `onPermissionResult`, `onRetry`, `onFlipClick`) đi qua `openCameraIfAllowed(keepReady)`: kiểm `cameraMayOpen(screenStarted, screenResumed, adShowing)` ngay trước khi gọi `openCamera`; không được thì trạng thái `Starting`, log `Camera waits for the full-screen ad`, không gọi `controller.open`.
- Hàm `reconcile()` gọi từ `onFullScreenAdChanged`, `onScreenResumed`, `onScreenPaused` (chỉ khi giá trị đổi):
  - `cameraActive` và màn đang chạy mà không còn được mở: `cancelCountdown(reason = "full_screen_ad")`, hủy `openJob`, `controller.close()`, `cameraActive = false`, `closedForFullScreenAd = true`, trạng thái `Starting`, log `Camera closed: full-screen ad`.
  - Không `cameraActive`, `cameraWanted` và được mở: log `Camera reopens after the full-screen ad` khi `closedForFullScreenAd`, rồi `openCamera()` (về `Starting`, `Ready` khi gắn xong).
- `onScreenStopped` giữ việc đóng camera như hôm nay, thêm `screenStarted = false`, `screenResumed = false`, `cameraActive = false`, `closedForFullScreenAd = false`.
- Nút chụp trong khoảng từ lúc quảng cáo đóng tới lúc camera gắn lại: trạng thái là `Starting`, `onShutterClick` chỉ chạy với `Ready` nên bị bỏ qua; nút chụp mờ theo KB-8, tiêu chí 1.
- Lần chụp đang chạy khi bị che: không xảy ra ở luồng quay lại app (ra nền đã đóng camera) hay quảng cáo có thưởng (mở từ hộp thoại, không lúc chụp); nếu có thì đóng như `onScreenStopped` hôm nay (ảnh báo lỗi chụp).

**`CameraScreen.kt`:** `CameraRoute` gọi `CameraResumeEffect(viewModel::onScreenResumed, viewModel::onScreenPaused)` cạnh `LifecycleStartEffect` có sẵn. Không đọc `LocalAdsController`.

**`:app`, module Hilt mới `CameraAdCoverModule`:** `@Provides fun fullScreenAdCover(ads: AdsController): FullScreenAdCover = FullScreenAdCover { ads.fullScreenAdShowing }`, `@InstallIn(SingletonComponent::class)`. `AdsController` là singleton `@Inject` có sẵn. `CameraModule` của `feature/camera` không đổi.

**Không sửa `:core:ads`**: chỉ đọc `AdsController.fullScreenAdShowing` công khai, ở `:app` như `MainActivity` đang làm cho hộp thoại mất mạng.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/FullScreenAdCover.kt` | Interface `FullScreenAdCover`, hàm thuần `cameraMayOpen`, composable `CameraResumeEffect` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModel.kt` | Tham số `fullScreenAdCover`, thu cờ trong `init`, `onScreenResumed`, `onScreenPaused`, `openCameraIfAllowed`, `reconcile`, các biến của mục 3.1, ba dòng log mới |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | Gọi `CameraResumeEffect` trong `CameraRoute` |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/di/CameraAdCoverModule.kt` | Cấp `FullScreenAdCover` từ `AdsController.fullScreenAdShowing` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/di/CameraAdCoverModuleTest.kt` | Test của module Hilt (mục 4) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraFakes.kt` | `FakeFullScreenAdCover` (một `MutableStateFlow(false)`) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModelTest.kt` | Truyền `FakeFullScreenAdCover` khi dựng `CameraViewModel`; test của mục 4 |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/FullScreenAdCoverTest.kt` | Test `cameraMayOpen` và test Robolectric của `CameraResumeEffect` |
| Sửa | `docs/ops/manual-test.md` | Mục 11.3: bước thử camera dưới quảng cáo khi quay lại app và dưới quảng cáo có thưởng (mục 4) |

Spec camera (`docs/features/camera/spec.md`) đổi trong PR của plan này, không trong phiên.

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
| `onScreenStarted(true)` luôn mở camera | Mở khi `cameraMayOpen(started, resumed, adShowing)`; cờ `false` thì như cũ | `CameraViewModelTest.stoppedThenStarted_closesThenReopensWithSameTools` và các test dùng `readyViewModel()` | Giữ: `FakeFullScreenAdCover` mặc định `false`; chỉ đổi phần dựng (thêm tham số constructor), không đổi câu khẳng định |
| `onRetry`, `onFlipClick`, `onPermissionResult(true)` luôn mở camera | Đi qua cùng cổng | Các test của ba hàm trong `CameraViewModelTest` | Giữ, cùng lý do |
| Camera chạy dưới quảng cáo có thưởng khi mở khóa tư thế | Camera đóng, về `Starting`, mở lại khi quảng cáo đóng | `LockedPoseUnlockTest` (không dựng `CameraViewModel`) | Giữ: không có câu khẳng định về camera |

## 4. Kiểm thử

Test viết trước dùng `FakeCameraController` (`openCalls`, `closeCount`, `pendingOpen`, `captureCount`), `FakeFullScreenAdCover` và `RecordingAppLogger`. Mỗi test "không mở" có đối chứng: cùng bước với cờ `false` (hay màn `RESUMED`) thì camera mở.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Cổng thuần | Unit test | `FullScreenAdCoverTest.cameraMayOpen_truthTable`: đủ 8 tổ hợp; chỉ `started` mà `adShowing && !resumed` là `false`; `started = false` luôn `false` |
| Giá trị cờ có trước `ON_START` đầu tiên | Unit test | `CameraViewModelTest.adAlreadyShowingAtCreation_firstStartWaits`: cờ `true` trước khi dựng view model; `onScreenStarted(true)` (chưa resume): `openCalls` rỗng, `Starting`, log `Camera waits for the full-screen ad`. Đối chứng: cờ `false` thì mở |
| Thứ tự của log gốc: hủy lời mở đang dở | Unit test | `CameraViewModelTest.adCoversWhileOpening_cancelsOpenAndCloses`: `pendingOpen` chưa xong; `onScreenStarted(true)`, `onScreenResumed()`, cờ `true` (chưa đóng: còn `RESUMED`), `onScreenPaused()`: `closeCount == 1`, `Starting`, log `Camera closed: full-screen ad`; hoàn tất `pendingOpen` không đưa về `Ready` |
| Mục 5, KB-8 tiêu chí 4: quảng cáo đóng thì camera chạy lại, giữ lựa chọn | Unit test | `CameraViewModelTest.adClosed_reopensWithSameTools`: `readyViewModel()` có lưới và hẹn giờ; cờ `true`, `onScreenPaused()`: đóng, `Starting`; cờ `false` rồi `onScreenResumed()`: `openCalls.size == 2`, `Ready`, lưới và hẹn giờ giữ, log `Camera reopens after the full-screen ad` |
| Cờ tắt trước, màn resume sau (thứ tự ngược) | Unit test | `CameraViewModelTest.resumedBeforeFlagDrops_reopensOnce`: như trên nhưng `onScreenResumed()` trước khi cờ `false`: mở đúng một lần ở `onScreenResumed()` (cổng mở vì `resumed`), cờ `false` sau không mở thêm |
| Chốt chặn cờ kẹt | Unit test | `CameraViewModelTest.flagStuckWhileResumed_cameraRuns`: cờ `true` mãi; `onScreenStarted(true)`, `onScreenResumed()`: mở, `Ready`. `CameraViewModelTest.flagStuckThenPausedAndResumed_cameraRunsAgain`: thêm `onScreenPaused()` (đóng) rồi `onScreenResumed()`: mở lại |
| Nút chụp trong lúc mở lại | Unit test | `CameraViewModelTest.shutterWhileReopening_isIgnored`: sau khi đóng vì quảng cáo và trước khi `pendingOpen` xong: `onShutterClick()`: `captureCount == 0` |
| Đếm ngược dừng khi bị che | Unit test | `CameraViewModelTest.coveredDuringCountdown_cancelsCountdown`: hẹn giờ 3 giây, bấm chụp, cờ `true`, `onScreenPaused()`: `captureCount == 0`, log `Countdown cancelled: full_screen_ad` |
| Mọi đường mở camera đi qua cổng | Unit test | `CameraViewModelTest.retryWhileCovered_doesNotOpen`, `flipWhileCovered_doesNotOpen`, `permissionGrantedWhileCovered_doesNotOpen`: gọi khi bị che không tăng `openCalls`; hết che thì mở đúng một lần (đổi camera: ống kính mới) |
| Ra nền giữa lúc chờ | Unit test | `CameraViewModelTest.adClosedWhileStopped_opensOnlyOnNextStart`: bị che, `onScreenStopped()`, cờ `false`: `openCalls` không tăng; `onScreenStarted(true)`: mở một lần |
| Cờ phát lại cùng giá trị, cờ đổi giữa chừng | Unit test | `CameraViewModelTest.sameValueTwice_changesNothing`; `CameraViewModelTest.flagFlipsWhileResumed_neverCloses`: `readyViewModel()`, `onScreenResumed()`, cờ `true` rồi `false`: `closeCount == 0`, `openCalls.size == 1` |
| Phần nối DI ở `:app` | Unit test (`:app`) | `CameraAdCoverModuleTest.fullScreenAdCover_followsAdsControllerFlag`: `mockk<AdsController>` chỉ trả `fullScreenAdShowing` (một `MutableStateFlow(false)`; thuộc tính khác không khai báo nên mockk ném lỗi nếu module đọc nhầm); `CameraAdCoverModule.fullScreenAdCover(ads).showing()` là cùng đối tượng, đổi giá trị thì thấy giá trị mới. Thiếu binding thì Hilt báo lỗi lúc build `:app` |
| Phần báo `RESUMED`, ra nền rồi trở lại | Compose UI test (Robolectric, `createAndroidComposeRule<ComponentActivity>()`) | `FullScreenAdCoverTest.resumeEffect_reportsPauseAndResume`: nội dung `CameraResumeEffect(record("resumed"), record("paused"))`; `moveToState(STARTED)`, `moveToState(CREATED)`, `moveToState(RESUMED)`: bản ghi `[resumed, paused, resumed]` (ra nền không báo `resumed` thêm, trở lại báo đúng một lần) |
| KB-1, KB-8 không đổi khi không có quảng cáo | Unit test | Test có sẵn của `CameraViewModelTest` qua với câu khẳng định nguyên (mục 3.5) |
| Mục 5 trên máy: quảng cáo khi quay lại app | Kiểm tra thủ công (máy thật: khác, cần đưa app ra nền rồi mở lại từ biểu tượng; trình chạy kịch bản không có lệnh về màn hình chính) | `resume_ad_type` là `inter` trên Firebase staging (bản staging bật `show_resum_ads`). Mở app tới camera, về màn hình chính 2 giây, mở lại. Trong lúc quảng cáo hiện: chấm báo camera của Android tắt; log có `Camera closed: full-screen ad` (hay `Camera waits for the full-screen ad`) và không có `Camera ready` nào giữa dòng đó và `Full-screen turn finished`. Sau khi đóng: `Camera reopens after the full-screen ad` rồi `Camera ready`, tư thế và lựa chọn giữ nguyên. Lặp lại với `resume_ad_type` mặc định (app open) |
| Mục 5 trên máy: quảng cáo có thưởng | Kiểm tra thủ công (máy thật: khác, quảng cáo có thưởng thử của AdMob trên bản staging) | Chạm ô tư thế premium, xem quảng cáo: chấm báo camera tắt, log `Camera closed: full-screen ad`; xem xong: `Camera ready` với tư thế vừa mở khóa |

**Ngoại lệ viết test trước:** dòng gọi `CameraResumeEffect(viewModel::onScreenResumed, viewModel::onScreenPaused)` trong `CameraRoute`, vì `CameraRoute` cần Hilt và không test nào dựng nó (như `LifecycleStartEffect` có sẵn ngay cạnh); hai phía của dòng nối có test (`FullScreenAdCoverTest`, `CameraViewModelTest`), và hai bước thử trên máy thật kiểm dòng nối. Bước của `docs/ops/manual-test.md` là tài liệu.

### 4.1. Chạy thử

- **Đã chạy thử (1):** test tạm không commit `TmpCoverProbeTest` (2026-11-13, trên `origin/main` `8af6a3e`): `createAndroidComposeRule<ComponentActivity>()`, `LaunchedEffect` thu một `MutableStateFlow`, `moveToState(CREATED)` rồi `RESUMED`. Lệnh `./gradlew :feature:camera:testDebugUnitTest --tests '*TmpCoverProbeTest*'`: qua; bản ghi `[false, true, false]`. Cho thấy `moveToState` của `createAndroidComposeRule` chạy được trong `feature/camera` trên máy dev.
- **Đã chạy thử (2):** test tạm không commit `TmpAdsMockProbeTest` trong `:app` (2026-11-13): `mockk<AdsController> { every { fullScreenAdShowing } returns flow }`, `assertSame(flow, ads.fullScreenAdShowing)`. Lệnh `./gradlew :app:testStagingDebugUnitTest --tests '*TmpAdsMockProbeTest*'`: qua; mockk tạo được bản giả của lớp `final` `AdsController` (constructor `internal`) mà không chạy constructor.
- **Kết quả:** hai test tạm đã xóa. Chưa chạy `scripts/check.sh` (E dưới 2, mục 5).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có. Không thêm, bớt hay đổi vị trí, định dạng, tần suất quảng cáo (điểm 1); không sửa `:core:*` (điểm 7).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module: `feature/camera` (interface mới, đổi constructor và hàm công khai của `CameraViewModel`) và `:app` (module Hilt mới) |
| B. Độ rõ của yêu cầu | 2 | Nhật đã chọn cách làm và phạm vi, nhưng spec camera đang `changed` (mục 5, KB-8 tiêu chí 4) chờ xác nhận; plan chờ spec chốt (mục 8, câu 1) |
| C. Khả năng kiểm chứng tự động | 3 | Vòng đời ứng dụng và thứ tự với quảng cáo thật; khoảng thời gian giữa lời mở camera và lúc màn bị che chỉ kiểm được trên máy thật |
| D. Ảnh hưởng chéo | 2 | `docs/ops/manual-test.md` cùng được T-093, T-094 sửa (mục Quảng cáo) |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu `CountdownInterruptions` và `LifecycleStartEffect`; lần đầu dùng `LifecycleResumeEffect` và mockk trong test của `:app` (đã chạy thử, mục 4.1) |
| **Tổng** | **11** | **Mức: Cao**, cần duyệt chéo |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: đọc `AdsController.fullScreenAdShowing` của `:core:ads` ở `:app` (không sửa); thêm một module Hilt ở `:app`.
- Task khác trong sprint có thể xung đột: T-093, T-094 cùng thêm bước vào `docs/ops/manual-test.md`, mục Quảng cáo (khác đoạn); T-094, T-096 sửa `review/ReviewScreen.kt`, T-093, T-094 sửa `core/ads/.../AdPlacement.kt`, plan này không chạm hai file đó.
- Đề xuất thứ tự chạy: độc lập về code; xếp trước hay sau T-093, T-094 trong cùng danh sách (không song song) vì cùng `docs/ops/manual-test.md`.

## 7. Điều kiện dừng riêng

- Cần sửa file trong `core/ads/` để camera đọc được cờ hay để chốt chặn cờ kẹt chạy: dừng, vì plan chọn không sửa `:core:*`; ghi lý do để plan phiên bản mới xét.
- Feature `camera` cần đọc `AdsController` hay `LocalAdsController`: dừng (trái ADR-0045, mục 4).
- Một test có sẵn của `:core:ads` hay `AppAdsRootTest` đổi kết quả: dừng, vì plan không đổi hành vi của quảng cáo.
- Test của mục 4 cho thấy camera có thể không chạy khi màn `RESUMED` (camera tối khi người dùng đang thấy màn): dừng.

## 8. Câu hỏi cho người duyệt

1. **Chờ xác nhận spec.** PR của plan này đổi `features/camera/spec.md` (mục 5, KB-8 tiêu chí 4 mới) sang `changed` theo lựa chọn của Nhật ngày 2026-11-13: camera không chạy khi bất kỳ quảng cáo toàn màn hình nào phủ lên, kể cả quảng cáo có thưởng mở khóa tư thế. Plan chỉ duyệt được sau khi Nhật xác nhận spec.
2. **Khoảng mở rồi đóng ngắn (rủi ro còn lại, việc tiếp theo).** Lời mở camera bắt đầu ở `ON_START`, trước khi biết có quảng cáo. Máy nhanh có thể gắn camera xong trước khi màn bị che, nên camera chạy rất ngắn rồi đóng. Không có cách làm thay trong phiên mà không sửa `:core:ads`. Nếu thử trên máy vẫn thấy chấm báo camera trong lúc quảng cáo hiện, việc tiếp theo là đề xuất về dự án mẫu một cờ công khai "quảng cáo khi quay lại app đang chờ hiện" (từ lúc vào foreground tới khi lượt bắt đầu hay bị bỏ), để camera chờ cả lúc đó (`docs/ops/from-app.md`). Đề xuất của AI: chấp nhận ở plan này.
3. **Lớp native toàn màn hình trong cùng activity.** Combo native sau inter và native toàn màn hình độc lập vẽ trong `MainActivity`, không làm màn `PAUSED`, nên camera vẫn chạy dưới chúng. Hôm nay chúng không hiện trên camera. Đề xuất của AI: chấp nhận; ghi lại để spec hay plan sau xét khi có quảng cáo loại này trên camera.
4. **Người duyệt.** Rủi ro Cao: cần người duyệt và người duyệt chéo (dev còn lại); nhóm một dev thì ghi lý do ở mục 9 (`risk-scoring.md`, mục 3).

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
