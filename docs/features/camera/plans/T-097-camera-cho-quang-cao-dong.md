---
ticket: T-097
title: Camera chờ quảng cáo toàn màn hình đóng rồi mới mở khi quay lại app
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 6                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: medium           # low | medium | high
token_estimate: 15.0M        # cỡ M theo scripts/token-usage.py estimate (2026-11-13), không nâng tay
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-097: CAMERA CHỜ QUẢNG CÁO TOÀN MÀN HÌNH ĐÓNG RỒI MỚI MỞ KHI QUAY LẠI APP

## 1. Tóm tắt

Màn camera theo dõi cờ "đang hiện quảng cáo toàn màn hình" có sẵn của `:core:ads` (`AdsController.fullScreenAdShowing`, đọc qua `LocalAdsController`). `CameraViewModel` chỉ mở camera khi màn đang chạy và không có quảng cáo toàn màn hình phủ lên; cờ bật khi camera đang mở hay đang mở dở thì đóng camera như khi rời màn; cờ tắt thì mở lại. Không sửa `:core:ads`.

Kiểm chứng bằng unit test của `CameraViewModel` (mọi đường tới lời mở camera, hai thứ tự của cờ và lúc màn chạy, ra nền giữa lúc chờ), test Robolectric của phần nối giữa cờ và vòng đời màn (ra nền rồi trở lại bằng `moveToState`), và thử trên máy thật theo FB-20261113-02.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Mục 5 | "Đóng quảng cáo thì camera mở lại như khi quay lại app thường" | Trong lúc quảng cáo toàn màn hình phủ lên camera (quảng cáo khi quay lại app `resume`, `resume_inter`), camera không chạy; quảng cáo đóng thì camera mở như khi quay lại app. Áp cho mọi quảng cáo toàn màn hình phủ lên màn camera đang chạy, kể cả quảng cáo có thưởng mở khóa tư thế premium (câu hỏi 2) |
| KB-1, tiêu chí 1 | Khi vào camera thì khung xem trước chạy | Không có quảng cáo toàn màn hình thì camera mở như hôm nay, không chờ thêm |
| KB-8, tiêu chí 1 | Khi camera đang khởi động thì khung xem trước hiện vòng chờ | Lúc chờ quảng cáo đóng, màn camera bị quảng cáo che; khi quảng cáo đóng, camera mở lại giữ trạng thái `Ready` (các nút, tư thế) như lúc đổi camera (`openCamera(keepReady = true)`), không về vòng chờ |
| KB-8, tiêu chí 3 | Khi app ra nền rồi quay lại thì camera tự chạy lại, giữ tư thế và các lựa chọn | Không đổi khi không có quảng cáo. Có quảng cáo: camera chạy lại sau khi quảng cáo đóng, vẫn giữ tư thế và các lựa chọn |
| KB-9, tiêu chí 4 | Quảng cáo toàn app khi quay lại app có thể phủ lên camera | Không đổi luật hiện quảng cáo; chỉ đổi việc camera chạy dưới quảng cáo |

Phản hồi: [FB-20261113-02](../feedback/FB-20261113-02-camera-mo-duoi-quang-cao-resume.md) (phiên thử [TS-20261113-01](../../../test-sessions/TS-20261113-01.md), mục P-2). Log gốc: `MainActivity` resume 08:50:23.830, `Resume ad shown on resume: resume_inter` 08:50:23.973, `Open camera top activityName is ...AdActivity` 08:50:24.327, `Camera ready` 08:50:24.328, `Full-screen turn finished: closed` 08:50:30.399.

Thiết kế tham chiếu: không đổi giao diện; không có ảnh thiết kế mới.

ADR liên quan: ADR-0045 (quảng cáo theo AdsKit PARTNER; feature chỉ dùng `:core:ads`), ADR-0019 (log).

## 3. Giải pháp

### 3.1. Cách làm

**Thứ tự sự kiện khi quay lại app (đọc từ code, không đo).** `AdsActivityTracker.onActivityStarted` báo app vào foreground và `ResumeAdTurns` quyết định quảng cáo (chưa hiện). `LifecycleStartEffect` của `CameraRoute` chạy ở `ON_START` và gọi `CameraViewModel.onScreenStarted`, lúc này `fullScreenAdShowing` còn `false`. Ở `onActivityResumed`, `ResumeAdTurns.onAppActivityResumed` gọi `showAppOpen` hay `showInterstitial`, và cờ chỉ bật khi lượt quảng cáo bắt đầu (`presence.tryBegin`), sau `ON_START`. Vì vậy camera không thể chờ cờ ở `ON_START`: cờ bật sau khi lời mở camera đã bắt đầu. Cách làm xử lý cả hai thứ tự (cờ bật trước khi màn chạy, cờ bật khi camera đang mở hay đang mở dở) và không dựa vào việc đo khoảng cách giữa chúng.

**`CameraViewModel` (`feature/camera`):**

- Thêm hai trạng thái riêng: `screenStarted` (giữa `onScreenStarted(true)` và `onScreenStopped`) và `coveredByFullScreenAd` (giá trị cờ mới nhất). Thêm `cameraWanted`: màn đã có quyền và muốn camera chạy (đặt ở `onScreenStarted(true)`, `onPermissionResult(granted = true)`, `onRetry`, `onFlipClick`; bỏ ở `onScreenStopped`).
- Mọi lời mở camera (`onScreenStarted`, `onPermissionResult`, `onRetry`, `onFlipClick`) đi qua một hàm riêng `openCameraIfUncovered(keepReady)`: hàm thuần `cameraMayOpen(started, covered)` (cùng file mới `FullScreenAdCover.kt`) trả `started && !covered`; được thì `openCamera(keepReady)` như hôm nay; không được thì ghi log `Camera waits for the full-screen ad` và không gọi `controller.open`. Kiểm cổng ngay trước lời gọi, ở mọi đường.
- Hàm mới `onFullScreenAdChanged(showing: Boolean)`:
  - `showing = true`: nếu `openJob` còn chạy hay camera đã mở (trạng thái `Ready`, hay `Starting` có `openJob`): `cancelCountdown(reason = "full_screen_ad")`, hủy `openJob`, `controller.close()`, ghi log `Camera closed: full-screen ad`. Trạng thái giao diện giữ nguyên (đang bị quảng cáo che).
  - `showing = false`: nếu `cameraWanted` và `cameraMayOpen(screenStarted, false)` và camera không đang mở: ghi log `Camera reopens after the full-screen ad`, `openCamera(keepReady = true)`.
  - Cùng giá trị gọi lại thì không làm gì (cờ có thể phát lại khi màn dựng lại).
- `onScreenStopped` giữ nguyên việc đóng camera, thêm `screenStarted = false`. Cờ tắt trong lúc màn dừng (app ở nền) thì không mở camera; lần `onScreenStarted` sau mở như hôm nay.
- Lần chụp đang chạy (`capturing`) khi cờ bật: không xảy ra ở luồng quay lại app (app ra nền đã đóng camera). Quảng cáo có thưởng chỉ mở từ hộp thoại mở khóa, khi không chụp. Plan không thêm luồng riêng; cờ bật khi đang chụp thì đóng camera như `onScreenStopped` hôm nay (ảnh đang chụp báo lỗi chụp như khi rời màn).

**`CameraScreen.kt`:**

- `CameraRoute` thêm tham số `fullScreenAdShowing: StateFlow<Boolean>? = LocalAdsController.current?.fullScreenAdShowing` (test truyền `MutableStateFlow`; không có `AdsController` thì `null`, camera như hôm nay).
- Composable mới `FullScreenAdCoverEffect(showing: StateFlow<Boolean>?, onChange: (Boolean) -> Unit)` trong `FullScreenAdCover.kt`: `LaunchedEffect(showing) { showing?.collect { onChange(it) } }`. Thu cờ suốt lúc màn còn trong cây giao diện, không theo `repeatOnLifecycle`, vì cờ bật khi màn ở `STARTED` (activity bị `AdActivity` trong suốt che, chỉ `PAUSED`) và tắt có thể khi màn đã dừng; `CameraViewModel` tự bỏ qua giá trị không cần. `CameraRoute` gọi nó với `viewModel::onFullScreenAdChanged`, đặt ngay trước `LifecycleStartEffect`.
- Mẫu có sẵn: `AppAdsRoot` của `:app` thu `fullScreenAdShowing` cùng cách cho hộp thoại mất mạng; `CountdownInterruptions` của camera dừng đếm ngược theo một cờ phủ màn.

**Không sửa `:core:ads`.** `LocalAdsController` và `AdsController.fullScreenAdShowing` là API công khai. Cờ gồm quảng cáo mở app, inter, native toàn màn hình và quảng cáo có thưởng (`presence.tryBegin` ở cả bốn đường), nên cả `resume` và `resume_inter` đều được. Cờ kẹt: `StuckFullScreenRecovery` hạ cờ khi activity chủ resume lại trong lúc cờ vẫn bật, và trần 10 phút (F-02); camera mở lại theo cờ hạ.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/FullScreenAdCover.kt` | Hàm thuần `cameraMayOpen(started, covered)` và composable `FullScreenAdCoverEffect(showing, onChange)` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModel.kt` | `onFullScreenAdChanged`, `openCameraIfUncovered`, `screenStarted`, `coveredByFullScreenAd`, `cameraWanted`, ba dòng log mới |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | Tham số `fullScreenAdShowing` của `CameraRoute`, gọi `FullScreenAdCoverEffect` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModelTest.kt` | Test của mục 4 cho `CameraViewModel` |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/FullScreenAdCoverTest.kt` | Test `cameraMayOpen` và test Robolectric của `FullScreenAdCoverEffect` với vòng đời activity |
| Sửa | `docs/ops/manual-test.md` | Mục Quảng cáo: bước thử camera dưới quảng cáo khi quay lại app (mục 4, dòng máy thật) |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Đọc cờ trạng thái `AdsController.fullScreenAdShowing` (không phải cờ tính năng hay khóa Remote Config).
- Quảng cáo: Không chạm vị trí, định dạng hay tần suất. Chỉ đọc trạng thái "đang hiện quảng cáo toàn màn hình" để camera không chạy dưới quảng cáo.
- Sự kiện phân tích: Không chạm. Thêm ba dòng log `VGL.Camera` (ADR-0019), không gửi đi.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. Không thêm: `CAMERA` đã có (spec camera, mục 6).
- **Thư viện và SDK:** Không thêm. `:core:ads` đã có trên classpath của mọi feature (convention plugin `template.android.feature`); `kotlinx.coroutines` và Compose runtime đã có.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** trên máy thật, lời mở camera bắt đầu ở `ON_START` có thể xong trước khi cờ bật (khoảng 140 ms sau resume trong log gốc), nên camera mở rồi đóng ngay khi quảng cáo hiện. Unit test không đo được khoảng này.
- **Điều kiện chuyển:** lần thử trên máy thật (mục 4) vẫn thấy `Open camera top activityName is ...AdActivity`, hay chấm báo camera hiện trên quảng cáo, sau khi code của mục 3.1 đã merge.
- **Cách làm thay:** không làm trong phiên này. Ghi vào báo cáo để người quyết: đề xuất về dự án mẫu một cờ công khai "quảng cáo khi quay lại app đang chờ hiện" của `ResumeAdTurns` (bật từ lúc vào foreground tới khi lượt quảng cáo bắt đầu hay bị bỏ), để camera chờ cả lúc đó (`docs/ops/from-app.md`). Tiêu chí và test của mục 4 giữ nguyên.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (2026-11-13): 12 test ở nhóm "Cần xem trước" đều là `LoadingScreenTest`, chỉ trùng tên tham số `onRetry` của màn loading, không dùng `CameraViewModel`; plan không đổi chúng. Nhóm "Nên xem" (65 test) gồm test của `CameraScreen` (chỉ truyền callback, không đổi) và `CameraViewModelTest`; test có sẵn của `CameraViewModel` chạy không có cờ (giá trị mặc định `false`), nên giữ nguyên.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `onScreenStarted(true)` luôn mở camera | Mở khi `cameraMayOpen(started, covered)`; không có quảng cáo thì như cũ | `CameraViewModelTest.stoppedThenStarted_closesThenReopensWithSameTools` và các test dùng `readyViewModel()` | Giữ: không gọi `onFullScreenAdChanged`, cờ mặc định `false` |
| `onRetry`, `onFlipClick`, `onPermissionResult(true)` luôn mở camera | Đi qua cùng cổng | Các test của `onRetry`, `onFlipClick`, `onPermissionResult` trong `CameraViewModelTest` | Giữ, cùng lý do |
| Camera chạy dưới quảng cáo có thưởng khi mở khóa tư thế premium | Camera đóng trong lúc quảng cáo hiện, mở lại khi nó đóng | `LockedPoseUnlockTest` (không có `AdsController`, không có cờ) | Giữ: test không có cờ |

## 4. Kiểm thử

Test viết trước trong `CameraViewModelTest` dùng `FakeCameraController` (`openCalls`, `closeCount`, `pendingOpen`) và `RecordingAppLogger`; đối chứng: mỗi test "không mở" có cùng bước với cờ `false` ở test kế bên cho thấy camera mở.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Cổng thuần | Unit test | `FullScreenAdCoverTest.cameraMayOpen_onlyWhenStartedAndNotCovered`: bốn tổ hợp, chỉ `(true, false)` là `true` |
| Mục 5: quảng cáo hiện trước khi màn chạy thì camera chờ | Unit test | `CameraViewModelTest.adShowingBeforeStart_doesNotOpen`: `onFullScreenAdChanged(true)`, `onScreenStarted(true)`, `runCurrent()`: `openCalls` rỗng, log có `Camera waits for the full-screen ad` |
| Mục 5: quảng cáo đóng thì camera mở | Unit test | `CameraViewModelTest.adClosedAfterWaiting_opensCamera`: tiếp test trên, `onFullScreenAdChanged(false)`: `openCalls.size == 1`, trạng thái `Ready`, log `Camera reopens after the full-screen ad` |
| Mục 5: quảng cáo hiện khi camera đang mở dở (thứ tự của log gốc) | Unit test | `CameraViewModelTest.adShownWhileOpening_cancelsOpenAndCloses`: `pendingOpen` chưa xong, `onScreenStarted(true)`, rồi `onFullScreenAdChanged(true)`: `closeCount == 1`, hoàn tất `pendingOpen` không đưa về `Ready` mới (open bị hủy); `onFullScreenAdChanged(false)`: `openCalls.size == 2`, `Ready` |
| Mục 5: quảng cáo hiện khi camera đã chạy | Unit test | `CameraViewModelTest.adShownWhileReady_closesThenReopensWithSameTools`: `readyViewModel()`, bật lưới và hẹn giờ, cờ `true`: `closeCount == 1`, log `Camera closed: full-screen ad`; cờ `false`: `openCalls.size == 2`, lưới và hẹn giờ giữ nguyên (KB-8, tiêu chí 3) |
| Đếm ngược dừng khi quảng cáo che | Unit test | `CameraViewModelTest.adShownDuringCountdown_cancelsCountdown`: hẹn giờ 3 giây, bấm chụp, cờ `true`: `countdown == null`, `captureCount == 0`, log `Countdown cancelled: full_screen_ad` |
| Mọi đường mở camera đi qua cổng | Unit test | `CameraViewModelTest.retryWhileCovered_doesNotOpen`, `flipWhileCovered_doesNotOpen`, `permissionGrantedWhileCovered_doesNotOpen`: mỗi đường gọi khi cờ `true` không tăng `openCalls`; cờ `false` sau đó mở đúng một lần (ống kính mới với đổi camera) |
| Ra nền giữa lúc chờ | Unit test | `CameraViewModelTest.adClosedWhileStopped_opensOnlyOnNextStart`: cờ `true`, `onScreenStarted(true)`, `onScreenStopped()`, cờ `false`: `openCalls` rỗng; `onScreenStarted(true)`: mở một lần |
| Cờ phát lại cùng giá trị | Unit test | `CameraViewModelTest.sameAdValueTwice_changesNothing`: `readyViewModel()`, cờ `false` hai lần: `openCalls.size == 1`; cờ `true` hai lần: `closeCount == 1` |
| Không có `AdsController` | Unit test | `FullScreenAdCoverTest.nullFlow_neverReports`: `FullScreenAdCoverEffect(null, onChange)` không gọi `onChange` |
| Phần nối với vòng đời: ra nền rồi trở lại | Compose UI test (Robolectric, `createAndroidComposeRule<ComponentActivity>()`) | `FullScreenAdCoverTest.flowReportedAcrossBackground`: nội dung gồm `FullScreenAdCoverEffect(flow, record)`; đặt `flow = true` khi `RESUMED`, `scenario.moveToState(CREATED)`, đặt `false`, `moveToState(RESUMED)`: bản ghi là `[false, true, false]` (giá trị đầu, lúc che, lúc tắt khi ở nền), tức cờ tắt khi màn đã dừng vẫn tới `CameraViewModel` |
| Cờ đổi giữa chừng (quảng cáo đóng sớm hay bị bỏ khi chưa hiện xong) | Unit test | `CameraViewModelTest.adFlagFlipsDuringOpen_endsReady`: `pendingOpen`, cờ `true` rồi `false` trước khi open xong: cuối cùng `Ready`, `openCalls.size == 2`, không còn `openJob` cũ đặt trạng thái |
| KB-1, KB-8 không đổi khi không có quảng cáo | Unit test | Các test có sẵn của `CameraViewModelTest` qua nguyên (mục 3.5) |
| Mục 5 trên máy: camera không chạy dưới quảng cáo khi quay lại app | Kiểm tra thủ công (máy thật: khác, cần đưa app ra nền rồi mở lại từ biểu tượng để quảng cáo khi quay lại app hiện; trình chạy kịch bản không có lệnh về màn hình chính) | Bản staging có `show_resum_ads`; đặt `resume_ad_type` là `inter` trên Firebase staging. Mở app tới camera, về màn hình chính 2 giây, mở lại từ biểu tượng. Khi quảng cáo hiện: không có chấm báo camera; log không có `Open camera top activityName is ...AdActivity`; có `Camera closed: full-screen ad` hay `Camera waits for the full-screen ad`; sau `Full-screen turn finished` có `Camera reopens after the full-screen ad` rồi `Camera ready`. Lặp lại với `resume_ad_type` mặc định (app open) |
| Quảng cáo có thưởng mở khóa tư thế | Kiểm tra thủ công (máy thật: khác, quảng cáo có thưởng thử của AdMob trên bản staging) | Chạm ô tư thế premium, xem quảng cáo: log `Camera closed: full-screen ad`; xem xong: camera chạy lại với tư thế vừa mở khóa |

**Ngoại lệ viết test trước:** một dòng nối trong `CameraRoute` (tham số mặc định `LocalAdsController.current?.fullScreenAdShowing` và lời gọi `FullScreenAdCoverEffect(fullScreenAdShowing, viewModel::onFullScreenAdChanged)`), vì `CameraRoute` cần Hilt và không có test nào dựng nó; cả hai phía của dòng nối có test (`FullScreenAdCoverTest`, `CameraViewModelTest`), và lần thử trên máy thật kiểm dòng nối. Bước của `docs/ops/manual-test.md` là tài liệu.

### 4.1. Chạy thử

Không bắt buộc (E dưới 2): dùng lại `StateFlow` công khai của `:core:ads` và mẫu thu cờ của `AppAdsRoot`; `moveToState` của `ActivityScenario` đã dùng trong test của `:core:ads`. Đã chạy thử câu khẳng định của `FullScreenAdCoverTest.flowReportedAcrossBackground`, vì cờ tắt khi màn đã dừng phải tới được `CameraViewModel`:

- **Đã chạy thử:** test tạm không commit `TmpCoverProbeTest` (2026-11-13, trên `origin/main` `8af6a3e`): `createAndroidComposeRule<ComponentActivity>()`, nội dung là `LaunchedEffect(flow) { flow.collect { record += it } }`; cờ `true` khi `RESUMED`, `moveToState(CREATED)`, cờ `false`, `mainClock.advanceTimeBy(100)`, `moveToState(RESUMED)`.
- **Lệnh:** `./gradlew :feature:camera:testDebugUnitTest --tests '*TmpCoverProbeTest*'`
- **Kết quả:** qua; bản ghi lúc còn dừng là `[false, true, false]`, cuối cùng `[false, true, false]`: giá trị tắt tới ngay khi màn ở `CREATED`, không chờ màn chạy lại. Test tạm đã xóa.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có. Không thêm, bớt hay đổi vị trí, định dạng, tần suất quảng cáo (điểm 1); không sửa `:core:*` (điểm 7). Ticket chấm sơ bộ Cao vì có thể phải thêm API vào `:core:ads`; plan dùng API có sẵn nên không chạm.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Nhiều phần trong `feature/camera`, đổi giao diện công khai của `CameraViewModel` và tham số của `CameraRoute` |
| B. Độ rõ của yêu cầu | 0 | Spec camera mục 5 đã chốt, phản hồi có log và bước làm lại, chủ feature đã chọn sửa code |
| C. Khả năng kiểm chứng tự động | 3 | Vòng đời ứng dụng và thứ tự với quảng cáo thật; khoảng thời gian giữa lời mở camera và lúc cờ bật chỉ kiểm được trên máy thật |
| D. Ảnh hưởng chéo | 1 | Đọc thành phần dùng chung của `:core:ads` nhưng không sửa; không task nào khác sửa `CameraViewModel.kt`, `CameraScreen.kt` |
| E. Độ mới của kỹ thuật | 0 | Lặp lại mẫu thu cờ của `AppAdsRoot` và mẫu đóng mở camera theo vòng đời đã có |
| **Tổng** | **6** | **Mức: Trung bình** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: chỉ đọc `LocalAdsController`, `AdsController.fullScreenAdShowing` của `:core:ads`.
- Task khác trong sprint có thể xung đột: T-094 và T-096 sửa `review/ReviewScreen.kt`, T-093 và T-094 sửa `core/ads/.../AdPlacement.kt`; plan này không chạm hai file đó. Không plan nào khác sửa `CameraViewModel.kt`, `CameraScreen.kt` hay `docs/ops/manual-test.md`, trừ T-093 và T-094 cũng thêm bước vào `docs/ops/manual-test.md`, mục Quảng cáo: cùng file, khác đoạn; `run-check.py` sẽ báo chú ý, xếp nối nhau là đủ.
- Đề xuất thứ tự chạy: độc lập, chạy được trước hay sau T-093, T-094, T-096.

## 7. Điều kiện dừng riêng

- Cần sửa file trong `core/ads/` để camera đọc được cờ: dừng, vì plan chọn không sửa `:core:*`; ghi lý do để plan phiên bản mới xét (điểm chặn 7, đề xuất về dự án mẫu).
- Thay đổi làm một test có sẵn của `:core:ads` hay `:app` (`AppAdsRootTest`) đổi kết quả: dừng, vì plan không đổi hành vi của quảng cáo.

## 8. Câu hỏi cho người duyệt

1. **Đọc thẳng `LocalAdsController` từ feature, hay thêm một hàm của `:core:ads`?** Plan đọc `LocalAdsController.current?.fullScreenAdShowing` ở `CameraRoute` (API công khai, không sửa core). Cách khác: thêm `rememberFullScreenAdShowing()` vào `:core:ads` như `rememberNativeStepAdAllowed`, rồi đề xuất về dự án mẫu; cách này chạm điểm chặn 7 (rủi ro Cao). Đề xuất của AI: đọc thẳng, không sửa core; nếu nhóm muốn hàm chung thì mở issue `from-app` sau.
2. **Áp cho mọi quảng cáo toàn màn hình, hay chỉ quảng cáo khi quay lại app?** Cờ gồm cả quảng cáo có thưởng mở khóa tư thế premium hiện ngay trên camera: camera sẽ đóng trong lúc xem và mở lại sau (chậm khoảng nửa giây khi quay về). Đề xuất của AI: mọi quảng cáo toàn màn hình, vì cùng lý do (không dùng camera khi người dùng không thấy khung xem trước, chấm báo camera trên quảng cáo).
3. **Khoảng mở rồi đóng ngắn.** Lời mở camera bắt đầu ở `ON_START`, trước khi cờ bật; nếu camera mở xong trước đó, camera chạy rất ngắn rồi đóng khi quảng cáo hiện (mục 3.4). Đề xuất của AI: chấp nhận ở plan này, kiểm trên máy thật; nếu vẫn thấy chấm báo camera thì đề xuất cờ "quảng cáo khi quay lại app đang chờ" về dự án mẫu.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
