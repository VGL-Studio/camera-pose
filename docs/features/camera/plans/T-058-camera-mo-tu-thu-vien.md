---
ticket: T-058
title: Camera mở từ "Chụp với tư thế này" chạy và chụp được
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 10               # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 9M           # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-058: CAMERA MỞ TỪ "CHỤP VỚI TƯ THẾ NÀY" CHẠY VÀ CHỤP ĐƯỢC

## 1. Tóm tắt

Mở camera từ nút "Chụp với tư thế này" ở chi tiết tư thế thì khung xem trước đứng hình và không chụp được (FB-20261026-01, mức chặn). Màn camera cũ bị hủy sau khi màn camera mới đã gắn camera; lúc hủy, `CameraXController.close()` gọi `provider.unbindAll()` và gỡ luôn camera của màn mới, còn `MlKitPoseAnalyzer.release()` đóng bộ nhận dạng dáng mà màn mới đang dùng. Sau task này, mỗi bộ điều khiển chỉ gỡ các use case của chính nó, và bộ nhận dạng dáng chỉ đóng khi không còn màn nào bật nó (tập người bật, không phụ thuộc thứ tự). Kiểm chứng bằng test Robolectric tái hiện lỗi (đã chạy thử trên code hiện tại bằng `scripts/check.sh`: các test tái hiện lỗi thất bại đúng lý do, mục 4.1; các test đối chứng qua ngay, mục 4), test hai `PoseMatchViewModel` thật trên một bộ nhận dạng, và một kịch bản máy có camera sau.

`token_estimate` 9M: số gợi ý 7.5M (trung vị cỡ M của `scripts/token-usage.py estimate`) cộng phần test hai view model trên một bộ nhận dạng. Không nâng gấp đôi: ở RUN-20261026-02 task thật chỉ dùng 0,4 đến 0,7 lần số đã nâng tay (T-056 9.0M trên 18M, T-057 6.0M trên 15M; L-081).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi mở camera từ nút "Chụp với tư thế này" ở chi tiết tư thế thì camera dùng đúng tư thế đó, dải tư thế cuộn tới ô của nó | Camera đó phải chạy thật: khung xem trước có hình, như khi mở camera từ đầu. Phần chọn tư thế và cuộn dải đã đúng (T-022), không đổi |
| KB-5 | Khi có người trong khung thì app hiện điểm khớp ... | Nhận dạng dáng của màn camera mở từ chi tiết phải chạy: màn camera cũ bị hủy không được tắt nó |
| KB-6 | Khi bấm nút chụp mà hẹn giờ tắt thì app chụp ngay ..., mở màn xem lại | Chụp được ở màn camera mở từ chi tiết. Không đổi gì khác của KB-6 |

Thiết kế tham chiếu: không đổi giao diện; không có ảnh thiết kế mới.

ADR liên quan: ADR-0001 (feature chỉ phụ thuộc core), ADR-0019 (log).

**Nguyên nhân, AI đọc trên code (nhánh `docs/fb-camera-may-that`, 68f5959; chưa chạy trên máy):**

1. `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/PoseDetailEntry.kt:23-26`: `navigate(CameraDestination(askPermission, poseId)) { popUpTo<CameraDestination> { inclusive = true } }`. Mục camera cũ (dưới thư viện và chi tiết) bị bỏ, một mục camera mới được đẩy lên, với `CameraViewModel`, `PoseViewModel`, `PoseMatchViewModel` mới.
2. `CameraXController` là `@ViewModelScoped` (`di/CameraControllerModule.kt:15-17`): màn mới có bộ điều khiển riêng. Màn mới bắt đầu (`CameraScreen.kt:126-134`, `LifecycleStartEffect`), `CameraViewModel.onScreenStarted` mở camera: `CameraXController.open` gọi `provider.unbindAll()` (dòng 109) rồi `bindToLifecycle(this, ...)` (dòng 111), lifecycle của bộ điều khiển mới `RESUMED`.
3. Mục camera cũ bị hủy khi hiệu ứng chuyển màn của `NavHost` xong (`ComposeNavigator` giữ các mục bị bỏ trong "đang chuyển" tới lúc đó), thường là sau khi màn mới đã mở camera. `CameraViewModel.onCleared()` (dòng 199-200) gọi `controller.release()`; `release()` gọi `close()` (`CameraXController.kt:221-222`), và `close()` gọi `provider?.unbindAll()` (dòng 212). `ProcessCameraProvider` dùng chung cả tiến trình, nên lệnh này gỡ khung xem trước, chụp ảnh và phân tích của bộ điều khiển mới. Không ai gắn lại: lifecycle của bộ điều khiển mới vẫn `RESUMED`, `imageCapture` vẫn khác `null`. Khung đứng hình; bấm chụp thì `takePicture` lỗi, `CameraViewModel` chỉ ghi "Capture failed".
4. `release()` cũng gọi `poseAnalyzer.release()` (dòng 228): `MlKitPoseAnalyzer` là `@Singleton` (`match/MlKitPoseAnalyzer.kt:25`), `release()` (dòng 113-118) đặt `active = false` và đóng bộ nhận dạng mà `PoseMatchViewModel` mới vừa bật. `PoseMatchViewModel.onCleared()` của màn cũ (`match/PoseMatchViewModel.kt:159-161`) gọi `frameSource.setActive(false)`, cũng tắt nhận dạng của màn mới. Điểm khớp của màn mới không bao giờ chạy. `PoseMatchViewModel` cũ vẫn sống tới `onCleared` và vẫn theo `ActivePoseHolder` (`@Singleton`): khi `PoseViewModel` mới đặt tư thế của chi tiết, cả hai view model chạy lại và cùng gọi `setActive(true)` theo thứ tự thu luồng (người rà soát đo được: A:true, B:true, đặt tư thế, A:true, B:true). Vì thế cách "người bật gần nhất thắng" không đủ: A có thể là người bật sau cùng (mục 3.1).
5. Màn cũ đã đóng camera khi thư viện mở (`onStopOrDispose { viewModel.onScreenStopped() }`, `CameraScreen.kt:133`; `CameraViewModel.kt:120-123`), nên lần `close()` trong `release()` là lần thứ hai: nó còn phát một khung `null` (`poseAnalyzer.reset()`, dòng 217) vào luồng khung hình mà màn mới đang đọc.
6. Chọn tư thế ở dải tư thế (`PoseViewModel.onPoseSelect`) không điều hướng, không tạo màn mới, nên không gặp lỗi.

Log xác nhận được trên máy (`adb logcat -v threadtime | grep ' VGL\.'`, thẻ `VGL.Camera`): "Camera ready" của màn mới, rồi "Camera unbound" (debug), rồi khi bấm chụp "Capture failed". Ghi vào mục "Cần người xem" của nhật ký nếu dev có máy để so trước và sau.

## 3. Giải pháp

### 3.1. Cách làm

**Chọn phương án (a): bộ điều khiển chỉ gỡ phần của mình, nhận dạng dáng chỉ đóng khi không còn màn nào bật nó.** Không đổi điều hướng (câu hỏi 1, mục 8). Lý do: lỗi nằm ở chỗ một màn đã hủy gỡ tài nguyên dùng chung của cả tiến trình; sửa ở đó thì mọi đường có hai màn camera cùng lúc (điều hướng hôm nay, khôi phục sau khi hệ thống giết app, đường mới sau này) đều an toàn. Phương án (b), quay về màn camera đang có và giao tư thế cho nó, chỉ tránh được đường này, và cần thêm một bộ giữ yêu cầu dùng chung giữa `:app` và `:feature:camera` (`NavBackStackEntry.savedStateHandle` không phải `SavedStateHandle` của `PoseViewModel`), đổi `PoseDetailEntryTest` và kịch bản T-022.

**1. `CameraXController` (`feature/camera/.../camera/CameraXController.kt`):**

- Thêm trường `private var boundUseCases: List<UseCase> = emptyList()`.
- `open()`: thay `provider.unbindAll()` (dòng 109) bằng `unbindOwn(provider)`; sau khi gắn xong, `boundUseCases = listOfNotNull(preview, capture, analysis-thực-sự-được-gắn)`.
- `bindUseCases()`: khi gắn ba use case lỗi `IllegalArgumentException` (dòng 161-164), thay `provider.unbindAll()` bằng `provider.unbind(preview, capture, analysis)`, rồi gắn hai use case như cũ; hàm trả cả `Camera` và danh sách đã gắn (ví dụ một `Pair`, hoặc gán `boundUseCases` trong hàm).
- `close()`: nếu `boundUseCases` rỗng và `camera == null` thì không làm gì (đã đóng; không phát khung `null`, không ghi log lần hai). Ngược lại: `orientationListener.disable()`, `unbindOwn(provider)`, đặt lifecycle `CREATED` như cũ, xóa `camera`, `imageCapture`, `requests`, `poseAnalyzer.reset()`, ghi "Camera unbound" như cũ.
- `private fun unbindOwn(provider: ProcessCameraProvider?)`: `if (boundUseCases.isNotEmpty()) provider?.unbind(*boundUseCases.toTypedArray())`, rồi `boundUseCases = emptyList()`.
- `release()`: bỏ dòng `poseAnalyzer.release()` (bộ điều khiển không bật bộ nhận dạng, nên không đóng nó; xem mục 2). Giữ phần còn lại: `close()`, lifecycle `DESTROYED` (CameraX tự bỏ mọi thứ gắn với lifecycle này), giải phóng âm chụp, tắt luồng phân tích (`analysisExecutor`).
- Sửa chú thích của lớp: một câu về việc chỉ gỡ use case của mình, vì có thể có hai màn camera cùng lúc lúc chuyển màn (FB-20261026-01).
- CameraX cho nhiều lifecycle gắn cùng một camera; chỉ lifecycle đang chạy được dùng camera. Bộ điều khiển cũ đã ở `CREATED` (đã đóng), nên không giành camera của bộ mới.

**2. `PoseFrameSource` (`feature/camera/.../match/BodyPose.kt:15-20`) và `MlKitPoseAnalyzer`:**

- Giao diện đổi thành:
  - `fun setActive(owner: Any, active: Boolean)`: `owner` muốn hay không muốn nhận dạng chạy.
  - `fun release(owner: Any)`: `owner` không dùng nữa; bộ nhận dạng đóng khi không còn ai bật.
- `MlKitPoseAnalyzer`, trong khối `@Synchronized` như hiện nay, giữ một **tập người bật** so theo đối tượng, không theo `equals`: `private val owners: MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())`. Không phụ thuộc thứ tự gọi của các màn (mục 2, nguyên nhân 4).
  - `setActive(owner, true)`: thêm `owner` vào tập, tạo bộ nhận dạng nếu chưa có (như cũ).
  - `setActive(owner, false)`: bỏ `owner` khỏi tập.
  - Sau mỗi lần đổi tập: `active = owners.isNotEmpty()`.
  - `release(owner)`: bỏ `owner` khỏi tập; tập rỗng thì `active = false`, `detector?.close()`, `detector = null`; còn người khác thì giữ bộ nhận dạng.
  - `internal val running: Boolean get() = synchronized(this) { active && detector != null }` cho test.
- `PoseMatchViewModel` (`match/PoseMatchViewModel.kt`): dòng 70 thành `frameSource.setActive(this, reason == null)`; `onCleared()` thành `frameSource.release(this)`.

**3. Kịch bản máy:** `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt` (nội dung ở mục 3.2, đã chạy `scripts/device-scenario.py check`: qua). Emulator của `device-session.yml` không có camera sau (`-camera-back none`), nên trong CI tệp dừng ở bước `need id=camera_grid_button` và ghi `bỏ qua (môi trường)`, như `T-020-camera-mo-gap.txt`. Trên máy của dev (`scripts/device-session.sh scenarios --yes`), tệp chạy hết và chứng minh lỗi đã hết.

**Ngân sách kịch bản** (`docs/ops/test-sessions.md`, mục 5, Hạn chót): bốn tệp hiện có cần tối đa khoảng 1080 giây (plan T-056); tệp mới thêm tối đa 120 (timeout) + 30 (trả máy) + 20 (phần chừa) = 170 giây, tổng khoảng 1250, vượt 1100; chừa thêm 50 giây vì T-054 có thể chạy lại một tệp khi app bị giết. Trong CI tệp bỏ qua sau khoảng 40 giây, nhưng quy tắc của mục 5 tính theo timeout. `sorted()` chạy `app/` rồi `feature/camera/` trước `feature/poses/`, nên không nâng thì T-022 hay T-024 có thể thành `bỏ qua (hết thời gian)` trên máy có camera. Nâng mặc định `DEVICE_SCENARIOS_BUDGET` từ 1100 lên 1300, tìm theo nội dung: `scripts/device-scenario.py`, dòng `self.budget = env_seconds("DEVICE_SCENARIOS_BUDGET", 1100)` và câu `(default 1100)` của chuỗi trợ giúp; `scripts/device-session.sh`, `${DEVICE_SCENARIOS_BUDGET:-1100}` trong lệnh `timeout -k 30` và chú thích `default 1100` ở đầu tệp; `docs/ops/test-sessions.md`, mục 5, đoạn **Hạn chót**: "(mặc định 1100)" thành 1300, câu "Bốn tệp hiện có cần tối đa khoảng 1080 giây (plan T-056, mục 3.1). **Tệp mới làm tổng vượt 1100 giây**" thành "Năm tệp hiện có cần tối đa khoảng 1250 giây (plan T-058, mục 3.1). **Tệp mới làm tổng vượt 1300 giây**". `.github/workflows/device-session.yml` có `timeout-minutes: 50`: lượt `device-session` 22 phút với ngân sách 900 (RUN-20261026-01); thêm 400 giây so với 900 còn khoảng 29 phút, đủ, không sửa `.github/`. `scripts/tests/run.sh` không kiểm số 1100 (`grep -n 1100 scripts/tests/run.sh`: không có; các test ngân sách đặt `DEVICE_SCENARIOS_BUDGET` riêng), nên không sửa. `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ui/UiAutomatorTagsTest.kt` (chú thích của lớp) và `scripts/tests/fake-adb` (chú thích đầu tệp) chỉ nhắc `scripts/device-scenario.py` trong chú thích (cách trình chạy chọn phần tử, dump giả), không phụ thuộc ngân sách: không ảnh hưởng (L-020).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraXController.kt` | Gỡ đúng use case của mình (`boundUseCases`, `unbindOwn`), không còn `unbindAll()`; `close()` không làm gì khi đã đóng; `release()` không đóng bộ nhận dạng; chú thích lớp |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/BodyPose.kt` | `PoseFrameSource.setActive(owner, active)`, `release(owner)` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/MlKitPoseAnalyzer.kt` | Tập người bật (`owners`); `setActive`, `release` theo tập; `running` cho test; chú thích lớp |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/PoseMatchViewModel.kt` | `setActive(this, ...)`, `onCleared()` gọi `release(this)` |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraXControllerTest.kt` | Robolectric, `ProcessCameraProvider` giả bằng `mockk` (mục 4) |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/MlKitPoseAnalyzerTest.kt` | Robolectric, `PoseDetection.getClient` giả bằng `mockkStatic` (mục 4), gồm test hai `PoseMatchViewModel` thật trên một `MlKitPoseAnalyzer` thật |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/MatchFakes.kt` | `FakePoseFrameSource` theo giao diện mới: ghi người bật mỗi lần gọi (`activeOwners`), `releasedBy`; giữ `activeCalls` và `active` như cũ để các test hiện có không đổi |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/PoseMatchViewModelTest.kt` | Thêm hai test (mục 4) |
| Tạo | `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt` | Kịch bản, nội dung dưới bảng |
| Sửa | `scripts/device-scenario.py` | Mặc định `DEVICE_SCENARIOS_BUDGET` 1100 thành 1300 (`env_seconds(...)` và chuỗi trợ giúp, tìm theo nội dung) |
| Sửa | `scripts/device-session.sh` | `${DEVICE_SCENARIOS_BUDGET:-1100}` thành `:-1300` và chú thích `default 1100` ở đầu tệp |
| Sửa | `docs/ops/test-sessions.md` | Mục 5, Hạn chót: mặc định 1300; năm tệp cần khoảng 1250 giây (plan T-058) |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraProviderSource.kt` | (dự phòng) giao diện mỏng trên `ProcessCameraProvider` (`get()`, `bind`, `unbind`) và cài đặt thật, mục 3.4 |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/di/CameraControllerModule.kt` | (dự phòng) gắn `CameraProviderSource` cho `ViewModelComponent`, mục 3.4 |

Nội dung `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt`:

```text
ticket: T-058
criterion: Mở camera từ "Chụp với tư thế này" rồi bấm chụp thì màn xem lại mở (FB-20261026-01); cần camera sau, emulator CI bỏ qua
timeout: 120
fresh
consent decline 30
wait id=onboarding_screen | id=camera_screen 60
tap_if id=onboarding_enable_camera
wait id=camera_screen 15
need id=camera_grid_button 15
need id=camera_poses 15
# (1) thư viện, chi tiết, "Chụp với tư thế này": camera mới sẵn sàng
tap id=camera_poses
wait id=poses_grid 20
tap idp=poses_card_ in=poses_grid index=1
wait id=pose_detail_screen 15
tap str=poses_detail_shoot
wait id=camera_screen 15
wait id=camera_grid_button 15
shot camera-from-detail
# (2) màn camera cũ đã hủy (hiệu ứng chuyển màn xong); bấm chụp thì màn xem lại mở
expect id=camera_grid_button
tap id=camera_shutter
wait id=review_screen 20
shot review
```

`camera_grid_button` chỉ hiện khi camera sẵn sàng (`CameraControls.kt:99-115`, `ready != null`). `expect` sau `shot` là một lần đọc màn hình nữa, sau ít nhất hai lần đọc, nên hiệu ứng chuyển màn (mặc định khoảng 0,7 giây) đã xong và màn cũ đã hủy trước khi bấm chụp. Hẹn giờ mặc định bật hay tắt đều nằm trong 20 giây chờ màn xem lại.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. `pose_matching` giữ như cũ.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm (`pose_selected` với `source=detail` đã có, không đổi).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `ProcessCameraProvider.unbind(vararg UseCase)` có trong `androidx-camera-lifecycle` 1.6.2 đã có (`javap` trên `camera-lifecycle-1.6.2.aar`: `public final void unbind(androidx.camera.core.UseCase...)`). Test dùng `libs.mockk` đã có trong cấu hình test chung (đã dùng ở `CameraZoomTest`).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `mockkStatic("androidx.camera.lifecycle.ProcessCameraProviderExtKt")` (giả `ProcessCameraProvider.awaitInstance`) lần đầu dùng trong dự án. Đã chạy qua `scripts/check.sh` (mục 4.1); còn chưa chắc khi test thật chạy cùng các test khác của module theo thứ tự khác (giả tĩnh còn sót giữa các lớp test).
- **Điều kiện chuyển:** `CameraXControllerTest` hay test khác của `:feature:camera` lỗi ở `scripts/check.sh` vì giả tĩnh (lỗi khởi tạo `MockK`, `awaitInstance` trả sai kiểu, test khác thấy `ProcessCameraProvider` giả), mà vẫn qua khi chạy riêng; hoặc lỗi cùng loại sau 2 lần sửa.
- **Cách làm thay:** thêm `internal interface CameraProviderSource { suspend fun get(): ProcessCameraProvider }` ở `camera/CameraProviderSource.kt` với cài đặt thật gọi `ProcessCameraProvider.awaitInstance(context)`, gắn trong `CameraControllerModule.kt`; `CameraXController` nhận nó qua constructor thay cho gọi thẳng `awaitInstance`. Test truyền nguồn giả trả `mockk<ProcessCameraProvider>` (giả lớp `final` không cần `mockkStatic`). Tiêu chí chấp nhận và test của mục 4 giữ nguyên.

## 4. Kiểm thử

Bộ giả trong `CameraXControllerTest`: `mockk<ProcessCameraProvider>(relaxed = true)` giữ một tập "đang gắn": `bindToLifecycle(owner, selector, *useCases)` thêm các use case vào tập và trả một `mockk<Camera>(relaxed = true)` có `cameraInfo.zoomState` là `MutableLiveData<ZoomState>()`; `unbind(*useCases)` bỏ chúng khỏi tập; `unbindAll()` xóa cả tập. Hai bộ điều khiển dùng chung bộ giả này và chung một `MlKitPoseAnalyzer`, với `FakeFeatureFlags(false)` (hai use case) và `FakeFeatureFlags(true)` (ba use case; `ImageAnalysis.Builder().build()` chạy được dưới Robolectric, mục 4.1; bộ điều khiển không gọi `setActive` hay `getClient`, nên không cần giả `PoseDetection`). `MainDispatcherRule` thay `Dispatchers.Main` vì `open()` chạy `withContext(Dispatchers.Main)`. Test so tập "đang gắn" (hành vi), không so cách gỡ, nên đúng cho cả phương án dự phòng. Kiểm ở mức cách làm (ví dụ `verify(exactly = 0) { provider.unbindAll() }`) chỉ là phụ, ghi rõ trong tên hay chú thích của test, không thay kiểm theo tập.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, KB-6: màn cũ hủy không gỡ camera của màn mới | Unit test (Robolectric) | `CameraXControllerTest.releaseOfStaleController_keepsUseCasesOfTheNewOneBound`: A mở, A đóng (thư viện mở), B mở, A `release()`: các use case B đã gắn vẫn còn trong tập. Chạy với cờ tắt và cờ bật (hai biến thể hay hai test) |
| Như trên, đối chứng (L-035) | Unit test (Robolectric) | `CameraXControllerTest.close_unbindsItsOwnUseCases`: B mở rồi B `close()`: tập không còn use case nào của B; B mở lại được và tập có đúng các use case mới Qua ngay ở commit test (đối chứng, chặn hồi quy, L-035). |
| Đổi camera trước, sau trên cùng bộ điều khiển | Unit test (Robolectric) | `CameraXControllerTest.reopen_unbindsThePreviousUseCasesOfTheSameController`: B mở `BACK`, B mở `FRONT`: tập chỉ có các use case của lần mở sau Qua ngay ở commit test (đối chứng, chặn hồi quy, L-035). |
| Máy không chạy được ba use case | Unit test (Robolectric) | `CameraXControllerTest.analysisNotSupported_bindsPreviewAndCapture_keepsOthers`: cờ bật, `bindToLifecycle` với ba use case ném `IllegalArgumentException`; trước đó A đã mở và chưa đóng: sau khi B mở, tập có use case của A và hai use case của B, không có use case phân tích |
| Đóng lần hai không phát khung `null` | Unit test (Robolectric) | `CameraXControllerTest.releaseAfterClose_emitsNoFrame`: thu `analyzer.frames` trong `backgroundScope`; A mở, A đóng (một `null`), A `release()`: vẫn chỉ một `null` |
| KB-5: màn cũ hủy không tắt nhận dạng của màn mới, mọi thứ tự | Unit test (Robolectric) | `MlKitPoseAnalyzerTest.releaseByStaleOwner_keepsDetectorOfActiveOwner`: `setActive(a, true)`, `setActive(b, true)`, `release(a)`: `detector.close()` không được gọi, `running` vẫn đúng; `staleActivatesLast_releaseByStale_keepsRunning`: `setActive(b, true)`, `setActive(a, true)`, `release(a)`: như trên |
| Như trên | Unit test (Robolectric) | `MlKitPoseAnalyzerTest.setInactiveByOneOwner_keepsRunningForTheOther`: `setActive(a, true)`, `setActive(b, true)`, `setActive(a, false)`: `running` vẫn đúng; thêm `setActive(b, false)`: `running` sai, `close()` chưa gọi |
| Đối chứng: người cuối cùng đóng được (L-035) | Unit test (Robolectric) | `MlKitPoseAnalyzerTest.releaseByLastOwner_closesDetector`: `setActive(b, true)`, `release(b)`: `close()` một lần, `running` sai; `releaseAfterAllInactive_closesDetector`: `setActive(a, true)`, `setActive(a, false)`, `release(a)`: `close()` một lần Cả hai test qua ngay ở commit test (đối chứng, chặn hồi quy, L-035). |
| Bật lại sau khi đóng | Unit test (Robolectric) | `MlKitPoseAnalyzerTest.activeAfterRelease_makesANewDetector`: `setActive(b, true)`, `release(b)`, `setActive(c, true)`: `getClient` gọi hai lần, `running` đúng Qua ngay ở commit test (đối chứng, chặn hồi quy, L-035). |
| KB-5: đường thật của hai màn camera | Unit test (Robolectric) | `MlKitPoseAnalyzerTest.twoMatchViewModels_clearOfStale_keepsMatching_staleCreatedFirst` và `..._newCreatedFirst`: một `MlKitPoseAnalyzer` thật (`mockkStatic(PoseDetection::class)`), một `ActivePoseHolder` có tư thế có điểm khớp, hai `PoseMatchViewModel` thật trong hai `ViewModelStore` (A rồi B, và B rồi A); đặt tư thế khác (`poseWithKeypoints(id = "w02")`, như khi chi tiết mở camera); xóa `ViewModelStore` của A: `running` vẫn đúng, `close()` không được gọi; rồi xóa `ViewModelStore` của B: `close()` được gọi đúng một lần, `running` sai |
| `PoseMatchViewModel` bật, tắt, đóng theo chính nó | Unit test | `PoseMatchViewModelTest.setActive_passesItself`: mọi lần gọi `setActive` mang cùng một người, là view model; `cleared_releasesWithItself`: xóa `ViewModelStore` (như `ReviewViewModelTest.save_leaveDuringRecord_recordStillCompletes`): `releasedBy` là view model, không còn lần `setActive(false)` thêm Cả hai test qua ngay ở commit test, vì hai dòng gọi của `PoseMatchViewModel` vào commit đó (ngoại lệ 4; đối chứng, chặn hồi quy, L-035). |
| KB-1, KB-6 trên máy có camera | Kiểm tra thủ công (emulator: `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt`) | Emulator CI không có camera sau: `bỏ qua (môi trường)` ở bước `need id=camera_grid_button`. Dev chạy tệp trên máy thật bằng `scripts/device-session.sh scenarios --yes`: tệp `đạt`, ảnh `review.png` là màn xem lại |
| KB-5 trên máy: điểm khớp hiện ở camera mở từ chi tiết | Kiểm tra thủ công (máy thật: camera thật) | Bản staging, cờ `pose_matching` bật: thư viện, chi tiết một tư thế có điểm khớp, "Chụp với tư thế này", đứng trước camera: điểm và lời nhắc hiện; log có "Pose matching on" và không có "Camera unbound" ngay sau "Camera ready". Nếu khung vẫn đứng hình và log có "Camera unbound" ngay sau "Camera ready" thì CameraX tự gỡ camera dùng chung khi lifecycle của màn cũ thành `DESTROYED`: ghi phản hồi mới, hướng sửa là bộ điều khiển chỉ gỡ khi chính nó gắn gần nhất (một bộ giữ người gắn gần nhất dùng chung) |
| Không ảnh hưởng đường khác | Unit test, Compose UI test hiện có | `CameraViewModelTest`, `PoseMatchViewModelTest`, `CameraScreen*Test`, `PoseDetailEntryTest` qua không sửa (trừ hai test thêm ở trên) |

**Ngoại lệ viết test trước:** (1) Tệp kịch bản: chạy ở `device-session` cuối phiên, không "thất bại trước" được. (2) Ngân sách 1300 của hai script và tài liệu: giá trị cấu hình, kiểm ở `device-session` cuối phiên (mọi tệp có kết quả, không tệp nào `bỏ qua (hết thời gian)`). (3) Chú thích của `CameraXController`, `MlKitPoseAnalyzer`. (4) Phần đổi chữ ký của `PoseFrameSource` (`setActive(owner, active)`, `release(owner)`) và thuộc tính `running` của `MlKitPoseAnalyzer` vào commit test của bước 3 dưới dạng khung: cài đặt giữ hành vi cũ (bỏ qua `owner`), để test biên dịch được và thất bại ở phần kiểm tra (`docs/auto-run.md`, bước 3; L-025). Khung giữ thêm hàm `release()` không tham số của `MlKitPoseAnalyzer` (hành vi cũ), để dòng `poseAnalyzer.release()` của `CameraXController.kt:228` vẫn biên dịch; bước 4 xóa dòng 228 và hàm không tham số đó. `MatchFakes.kt` và hai dòng gọi trong `PoseMatchViewModel.kt` cũng vào commit đó. Phần hành vi theo người bật vào commit code của bước 4.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-26, trên nhánh `plan/T-058` (gốc 5f5cc5c, nhánh chính sau PR #162), hai tệp test tạm không commit:
  - `feature/camera/src/test/.../camera/TmpTwoCamerasTest.kt`: `ProcessCameraProvider` giả bằng `mockk` thả lỏng với tập "đang gắn" như mục 4 (`bindToLifecycle` thêm, `unbind` bỏ, `unbindAll` xóa), `mockkStatic("androidx.camera.lifecycle.ProcessCameraProviderExtKt")` cho `awaitInstance`, `cameraInfo.zoomState` là `MutableLiveData<ZoomState>()`. Hai `CameraXController` thật chung một `MlKitPoseAnalyzer`; A mở, A đóng, B mở, A `release()`, rồi kiểm theo hành vi: các use case của B vẫn trong tập. Hai test: `FakeFeatureFlags(false)` (B gắn 2 use case) và `FakeFeatureFlags(true)` (B gắn 3, có `ImageAnalysis`).
  - `feature/camera/src/test/.../match/TmpTwoMatchersTest.kt`: `mockkStatic(PoseDetection::class)`. (1) `setActive(true)` của B, `setActive(true)` của A (A sau cùng), `release()` của bộ điều khiển A: `verify(exactly = 0) { detector.close() }`. (2) và (3): hai `PoseMatchViewModel` thật trong hai `ViewModelStore` trên một `MlKitPoseAnalyzer` thật và một `ActivePoseHolder`, tạo A rồi B và B rồi A; đặt tư thế `w02`; xóa `ViewModelStore` của A; kiểm trường `active` (đọc bằng reflection, vì code hiện tại chưa có `running`) vẫn đúng và `close()` không được gọi.
- **Lệnh:** `scripts/check.sh` trong worktree của nhánh (cùng lệnh phiên dùng); trước đó `./gradlew :feature:camera:testDebugUnitTest --tests '*TmpTwo*'` cho cùng kết quả.
- **Kết quả:** `BUILD FAILED in 4m 29s`, chỉ ở `:feature:camera:testDebugUnitTest`: `403 tests completed, 5 failed`, đúng 5 test tạm, mọi test khác qua; không có bước nào khác của `check.sh` lỗi.
  - `staleRelease_keepsNewBound_flagOff`, `..._flagOn`: `AssertionError` ở phần kiểm "B's use cases still bound": `unbindAll()` trong `release()` của A xóa use case của B (dòng 212). Biến thể cờ bật cho thấy `ImageAnalysis.Builder().build()` và `setAnalyzer` chạy được dưới Robolectric.
  - `staleActivatesLast_releaseOfStale_keepsNew`: `PoseDetector.close()` được gọi.
  - `twoViewModels_staleCreatedFirst`, `twoViewModels_newCreatedFirst`: `active` sai sau khi xóa `ViewModelStore` của A, ở cả hai thứ tự tạo (`setActive(false)` trong `onCleared` của A tắt nhận dạng của B).
  - Lần chạy đầu (trước khi có tập "đang gắn") lỗi `ClassCastException: java.lang.Object cannot be cast to androidx.camera.core.ZoomState` vì `mockk` thả lỏng không trả được `LiveData<ZoomState>`; sửa bằng `every { camera.cameraInfo.zoomState } returns MutableLiveData<ZoomState>()`, ghi vào bộ giả ở mục 4.
  - `mockkStatic` trên `ProcessCameraProviderExtKt` và `PoseDetection`, giả lớp `final` `ProcessCameraProvider`, chạy được dưới `scripts/check.sh` (Robolectric 4.15.1, `mockk` 1.14.5). Test tạm có `@After unmockkAll()`; test thật làm như vậy.
  - Đã xóa hai test tạm.
- `scripts/device-scenario.py check feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt`: mã 0 (tag và chuỗi có thật). Tệp tạm, đã xóa; phiên tạo lại theo nội dung ở mục 3.2.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có. `scripts/device-scenario.py`, `scripts/device-session.sh` chỉ đổi số mặc định của ngân sách kịch bản, như plan T-056 (không đổi `.github/`, `scripts/check.sh`, `scripts/merge-if-green.sh`). Có thể coi hai script `scripts/device-*` thuộc điểm chặn 7 (quy trình phiên tự chạy); mức đã là Cao theo điểm, nên đường duyệt (duyệt chéo) không đổi.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một module feature, có đổi giao diện công khai (`PoseFrameSource`); thêm số mặc định trong hai script và tài liệu |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận; nguyên nhân đọc từ code và tái hiện bằng test, chưa xem log trên máy |
| C. Khả năng kiểm chứng tự động | 3 | Vòng đời màn và CameraX thật: JVM chỉ kiểm bằng bộ giả; emulator CI không có camera, kịch bản chỉ chạy trên máy dev |
| D. Ảnh hưởng chéo | 2 | `CameraXController` là chỗ gắn camera của cả feature; `scripts/device-scenario.py`, `docs/ops/test-sessions.md` cũng là file mà task khác chạm (T-054, T-056 đã merge) |
| E. Độ mới của kỹ thuật | 2 | `mockkStatic` và gỡ từng use case của CameraX lần đầu dùng trong dự án (đã chạy thử, mục 4.1) |
| **Tổng** | **10** | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `CameraXController` (mọi đường mở camera), `MlKitPoseAnalyzer` và `PoseFrameSource` (điểm khớp), ngân sách kịch bản của `device-session`.
- Task khác trong sprint có thể xung đột:
  - T-059, T-060, T-061 sửa `CameraScreen.kt`, `PoseOverlay.kt`, `CameraControls.kt`, `CameraUnfolded.kt`, `CameraViewModel.kt`. Plan này không sửa file nào trong số đó.
  - T-061 có thể thêm test vào `CameraViewModelTest.kt`; plan này không sửa file đó.
  - Plan khác thêm tệp kịch bản thì tính lại ngân sách từ 1300.
- Đề xuất thứ tự chạy: chạy đầu tiên, vì lỗi mức chặn, và các task sau của camera kiểm trên bản staging có camera chạy từ mọi đường.

## 7. Điều kiện dừng riêng

- Không có điều kiện riêng cho CameraX thật: JVM chỉ có CameraX giả, nên trường hợp CameraX tự gỡ use case của B khi A `DESTROYED` không lộ ra trong phiên; nó nằm ở dòng kiểm thủ công trên máy thật (mục 4).
- Cần sửa `PoseDetailEntry.kt`, `AppNavHost.kt` hay `CameraViewModel.kt` để test qua: dừng (ngoài phương án đã chọn, câu hỏi 1).

## 8. Câu hỏi cho người duyệt

1. **Có đổi luôn điều hướng "Chụp với tư thế này" để quay về màn camera đang có (phương án b) không?** Đề xuất của AI: không làm ở task này. Phương án (a) sửa chỗ gốc: mọi đường có hai màn camera đều an toàn. Phương án (b) cần một bộ giữ yêu cầu tư thế dùng chung giữa `:app` và `:feature:camera`, đổi `PoseDetailEntryTest` và kịch bản T-022; lợi ích thêm là không mở lại camera khi từ chi tiết sang, người dùng khó nhận ra. Nếu muốn, tách ticket riêng.
2. **Kịch bản cần camera sau mà emulator CI không có (`-camera-back none` trong `.github/workflows/device-session.yml`).** Đề xuất của AI: chấp nhận `bỏ qua (môi trường)` trong CI như `T-020-camera-mo-gap.txt`; dev chạy tệp trên máy thật khi kiểm bản staging. Riêng một ticket rủi ro cao sau (đổi `.github/`) có thể bật camera giả của emulator (`-camera-back emulated` hay `virtualscene`) để T-020 và T-058 chạy được trong CI; đề xuất cả về dự án mẫu.
3. **Ngân sách kịch bản 1300 giây** chỉ vì một tệp bỏ qua sớm trong CI. Đề xuất của AI: vẫn nâng theo quy tắc của mục 5 (tính theo timeout), vì trên máy có camera tệp chạy hết; 1250 cộng 50 giây dư cho lần chạy lại của T-054. Phương án khác: giữ 1100 và ghi ngoại lệ; AI không đề xuất. Con số này riêng của app (số tệp kịch bản của app), không đề xuất về dự án mẫu.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
