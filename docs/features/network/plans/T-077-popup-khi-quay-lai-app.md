---
ticket: T-077
title: Hộp thoại mất mạng hiện ngay khi quay lại app lúc không có mạng
status: draft
version: 1
risk_score:
risk_level: high
token_estimate: 6.4M
blocking_points: [core]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-077: HỘP THOẠI MẤT MẠNG HIỆN NGAY KHI QUAY LẠI APP LÚC KHÔNG CÓ MẠNG

## 1. Tóm tắt

Thêm vào `NetworkDialogController` của `:core:network` hai hàm `onAppStarted()` và `onAppStopped(changingConfigurations)`, theo `onAppForeground()` của dự án mẫu (T-017 của mẫu) nhưng giữ controller riêng của app (T-011): khi app quay lại từ nền ở một màn sau màn loading mà máy không có mạng, hộp thoại hiện sau một khoảng kiểm lại ngắn (tối đa 1 giây, kiểm mỗi 250 ms), không chờ 3 giây của KB-1; lần mở đầu, xoay màn hình và màn loading không tính là quay lại. `:app` gọi hai hàm này qua một `DefaultLifecycleObserver` gắn vào `MainActivity`. Quảng cáo quay lại app (`resume`) hiện trước, hộp thoại chờ quảng cáo đóng như mục 5 của spec. Kiểm chứng bằng unit test trên thời gian ảo cho controller và test Robolectric cho observer.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/network/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-3 (tiêu chí mới, câu hỏi 1 của mục 8) | Khi người dùng đưa app ra nền rồi quay lại một màn sau màn loading mà máy không có mạng thì hộp thoại hiện ngay khi app trở lại, không chờ thời gian chờ của KB-1 | "Ngay" là sau tối đa 1 giây kiểm lại (câu hỏi 2): máy có mạng lại trong khoảng đó thì không hiện. "Quay lại" là `MainActivity` bắt đầu lại sau một lần dừng không do đổi cấu hình (câu hỏi 3). Lần mở đầu và màn loading không tính (loading đã tự kiểm mạng, spec `loading`) |
| KB-3, tiêu chí có sẵn | Mở lại app thì nếu vẫn chưa có mạng, hộp thoại hiện lại | Hộp thoại đã hiện trước khi ra nền thì vẫn hiện (controller không đổi trạng thái khi ra nền); lần kiểm khi quay lại không mở thêm hộp thoại thứ hai |
| KB-1, "chỉ có một hộp thoại" | Kể cả khi nhiều lần tải cùng thất bại | Lần kiểm khi quay lại và đường "mất mạng quá 3 giây" cùng đi qua `show()`; `showNow()` chỉ đổi khi đang `Hidden`, nên chỉ một hộp thoại và một dòng log `Dialog shown` |
| KB-4 | Cờ `feature_network_dialog_enabled` tắt thì không hiện | Lần kiểm khi quay lại chỉ chạy khi controller đang `armed` (qua loading, cờ bật); cờ tắt giữa lúc kiểm lại thì bỏ lần hiện |
| Mục 5 của spec | Quảng cáo toàn màn hình đang hiện thì hộp thoại chờ quảng cáo đóng | Quảng cáo `resume` được quyết ở `onActivityStarted` (trước observer, mục 4.1) và hiện ở lần `onResume` kế tiếp; trong 1 giây kiểm lại, cờ `fullScreenAdShowing` đã tới controller, nên hộp thoại không lóe lên trước quảng cáo mà hiện sau khi quảng cáo đóng (câu hỏi 4) |

Thiết kế tham chiếu: không có ảnh mới; hộp thoại dùng nguyên `NetworkDialog` của `features/network/design/README.md` (docs/ops/design-workflow.md).

ADR liên quan: ADR-0024 (app luôn cần mạng, popup chung do `:app` chặn), ADR-0019 (log), ADR-0001 (phụ thuộc module), ADR-0029, ADR-0031.

## 3. Giải pháp

### 3.1. Cách làm

**Bản mẫu tham chiếu** (dự án mẫu, commit `95f8673` "T-017: Popup mất mạng chung (#113)", `core/network/.../NetworkDialogController.kt` và `app/.../MainActivity.kt`), dán nguyên để phiên không cần đọc repo mẫu (L-078):

```kotlin
// NetworkDialogController (template)
    fun onAppForeground() {
        synchronized(lock) {
            foregroundCount++
            if (foregroundCount == 1 || screen == null) return
            if (!connectivity.isConnected()) markLost("offline_on_foreground")
        }
    }

// MainActivity (template)
    override fun onStart() {
        super.onStart()
        // Back from the background without network: the dialog shows at once (spec network, KB-2).
        networkDialog.onAppForeground()
    }
```

Plan không chép nguyên bản mẫu, vì hai điểm khác: (1) đếm `onStart` thì xoay màn hình trên màn lớn (app mở khóa hướng từ T-056) cũng tính là quay lại; (2) không có khoảng kiểm lại nên dễ hiện oan khi máy đang nối lại mạng (rủi ro của ticket). Controller của app cũng không có `screen`, `lock`, `markLost`; nó có `armed`, `show()` và `pendingShow` (T-011, T-074 lượt 2).

**`:core:network`, `NetworkDialogController`** (chỉ thêm, không đổi đường cũ):

- Trường mới: `@Volatile private var leftApp = false` (đã có một lần dừng không do đổi cấu hình từ lần bắt đầu trước) và `@Volatile private var foregroundJob: Job? = null`.
- `fun onAppStopped(changingConfigurations: Boolean)`: `if (!changingConfigurations) leftApp = true`. Luồng chính. Không ghi log (mỗi lần ra nền một dòng là thừa; `AdsController` đã ghi `App in background`).
- `fun onAppStarted()`: luồng chính.
  1. `if (!leftApp) return`, rồi `leftApp = false`. Lần mở đầu chưa có lần dừng nào nên tự bị bỏ qua; xoay màn hình gọi `onAppStopped(true)` nên cũng bị bỏ qua.
  2. `if (!armed.value) return`: màn loading hay cờ tắt.
  3. `foregroundJob?.cancel()`, rồi `foregroundJob = scope.launch { checkOnForeground() }`.
- `private suspend fun checkOnForeground()`: kiểm `connectivity.isConnected()` ngay, rồi mỗi `NetworkSettings.RETRY_POLL_MILLIS` (250 ms) cho tới `NetworkSettings.FOREGROUND_GRACE_MILLIS` (1 000 ms). Có mạng ở một lần kiểm: dừng, không ghi log. Hết khoảng mà vẫn không có mạng: `if (armed.value) show(FOREGROUND_REASON)` với `FOREGROUND_REASON = "offline_on_foreground"`. Vòng lặp viết giống `retry()` (cùng kiểu `waited`), để đọc một kiểu.
- `disarm()`: thêm `foregroundJob?.cancel()` cạnh `retryJob?.cancel()`, để màn loading hay cờ tắt giữa lúc kiểm lại không hiện hộp thoại.
- Ra nền giữa lúc kiểm lại: không hủy job. Job hiện hộp thoại (nếu vẫn không có mạng) khi app đang ở nền, giống đường "mất mạng quá 3 giây" hiện nay vẫn chạy khi app ở nền; quay lại thì hộp thoại đã có (KB-3, tiêu chí có sẵn), và lần `onAppStarted()` mới thay job cũ. Test `backgroundDuringGrace_dialogStaysOnReturn` (mục 4) giữ điều này.
- KDoc của lớp thêm một câu: "It also shows when the app comes back from the background without network, after a check of at most [NetworkSettings.FOREGROUND_GRACE_MILLIS] ([onAppStarted]); the first start and a configuration change are not a return." KDoc của hai hàm mới nói luồng chính và ý nghĩa tham số.
- Log (ADR-0019, thẻ `Network`): dòng có sẵn `Dialog shown: offline_on_foreground` qua `showNow()`, hoặc `Dialog waits: full_screen_ad` qua `show()` khi quảng cáo đang hiện. Không thêm sự kiện phân tích.

**`:core:network`, `NetworkSettings`:** thêm `internal const val FOREGROUND_GRACE_MILLIS = 1_000L` với KDoc "How long a return to the app without network is checked again before the dialog shows (plan T-077, question 2)". Không là khóa Remote Config (không thêm vào `all()`), nên bảng khóa `docs/remote-config-keys.md` và `RemoteConfigKeysTest` không đổi.

**`:app`, tệp mới `network/AppForegroundObserver.kt`:**

```kotlin
/**
 * Reports to [onStarted] each start of [this] activity and to [onStopped] each stop, with whether the stop is a
 * configuration change (plan T-077). Registered in onCreate, so a recreated activity reports too.
 */
internal fun ComponentActivity.observeAppForeground(onStarted: () -> Unit, onStopped: (changingConfigurations: Boolean) -> Unit) {
    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = onStarted()

            override fun onStop(owner: LifecycleOwner) = onStopped(isChangingConfigurations)
        },
    )
}
```

**`:app`, `MainActivity.onCreate`:** sau `ads.gatherConsent(this)`, thêm `observeAppForeground(onStarted = networkDialog::onAppStarted, onStopped = networkDialog::onAppStopped)` kèm chú thích "Back from the background without network: the dialog shows within a second (spec network, KB-3; plan T-077)". Dùng observer thay vì ghi đè `onStart`/`onStop` để phần đọc `isChangingConfigurations` có test Robolectric (MainActivity cần Hilt, `:app` chưa có khung test dựng MainActivity).

**Thứ tự với quảng cáo quay lại app** (khung AdsKit, T-074): khi quay lại, `AdsActivityTracker.onActivityStarted` (gọi trong `super.onStart()`, trước observer ON_START, xem 4.1) quyết quảng cáo `resume` (`ResumeAdTurns.onProcessForegroundChanged`), rồi hiện ở `onActivityResumed`. Khi không có mạng, khung không tải quảng cáo mới (`NOT_READY` thì chỉ gọi `loadIfNeeded`, tải không đi được), nhưng quảng cáo đã nạp sẵn vẫn hiện: `presence.showing` lên `true`, `AppAdsRoot` đưa cờ này tới `setFullScreenAdShowing(true)` ở khung hình kế. Khi 1 giây kiểm lại hết, `show()` thấy `adShowing` và giữ lý do vào `pendingShow`; quảng cáo đóng thì hộp thoại hiện. Không có quảng cáo thì hộp thoại hiện sau 1 giây. Quảng cáo mở dạng activity riêng (`AdActivity`) làm `MainActivity` dừng rồi bắt đầu lại khi quảng cáo đóng: lần bắt đầu đó cũng là một lần quay lại, kiểm lại lần nữa, và vẫn chỉ một hộp thoại (`showNow` chỉ đổi khi `Hidden`). Plan không chạm `:core:ads`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogController.kt` | `onAppStarted()`, `onAppStopped()`, `checkOnForeground()`, `foregroundJob` hủy trong `disarm()`, KDoc |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkSettings.kt` | Hằng `FOREGROUND_GRACE_MILLIS = 1_000L` (không phải khóa Remote Config) |
| Sửa | `core/network/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogControllerTest.kt` | Các test mới của mục 4, thêm vào cuối lớp, không sửa test cũ |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/network/AppForegroundObserver.kt` | `ComponentActivity.observeAppForeground(onStarted, onStopped)` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/network/AppForegroundObserverTest.kt` | Test Robolectric của mục 4 |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | Gọi `observeAppForeground(...)` trong `onCreate` |

Không đổi file mà nơi khác đọc: không thêm khóa Remote Config (bảng `docs/remote-config-keys.md`, `RemoteConfigKeysTest` giữ nguyên), không ảnh chuẩn, không tài nguyên chuỗi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: dùng `feature_network_dialog_enabled` có sẵn (`FeatureFlag.NETWORK_DIALOG`) qua `armed`; không thêm cờ.
- Quảng cáo: Không chạm. Chỉ đọc thứ tự với quảng cáo `resume` có sẵn qua `setFullScreenAdShowing` (T-074 lượt 2); không đổi vị trí, định dạng hay tần suất.
- Sự kiện phân tích: Không chạm (spec, câu hỏi 5).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `ConnectivityManager.getActiveNetwork`/`getNetworkCapabilities` đã dùng `ACCESS_NETWORK_STATE` có sẵn của `:core:network` (T-011); `Activity.isChangingConfigurations` và `Lifecycle.addObserver` không cần quyền.
- **Thư viện và SDK:** Không thêm. `DefaultLifecycleObserver`, `LifecycleOwner` thuộc `androidx.lifecycle:lifecycle-common`, có trên classpath biên dịch của `:app` qua `androidx.activity` (phụ thuộc `api`); chạy thử ở 4.1 biên dịch đúng hai lớp này trong source set test của `:app`. `:core:network` chỉ dùng `kotlinx.coroutines` có sẵn.

### 3.4. Phương án dự phòng

Không có. Phần chưa chắc duy nhất (Robolectric có báo `isChangingConfigurations` đúng khi dựng lại activity không) đã chạy thử ở 4.1.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (kết quả ở dưới) chỉ ra `NetworkDialogControllerTest`, `AppAdsRootTest` và các test khác của `:core:network`, `:app`. Đã đọc từng test ở nhóm "Cần xem trước" và "Nên xem": không câu khẳng định nào bị đổi, vì plan chỉ thêm đường mới. Đường cũ (mất mạng quá 3 giây, lỗi tải, thử lại, chờ quảng cáo, cờ tắt) không đổi, và không test cũ nào gọi `onAppStarted`/`onAppStopped`.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `disarm()` hủy lần thử lại đang chạy và lần hiện đang chờ quảng cáo | Cũng hủy lần kiểm khi quay lại (`foregroundJob`) | `NetworkDialogControllerTest.deactivated_hidesDialog_andStopsWatching`, `inactiveWhileWaiting_dropsPending`, `flagTurnedOff_whileShown_hides` | Giữ nguyên |
| Quay lại app không có mạng: hộp thoại chỉ hiện sau 3 giây của KB-1 | Hiện sau tối đa 1 giây kiểm lại | Không có test khẳng định hành vi cũ (`deviceOffline_shownOnlyAfterDelay` không có lần quay lại) | Giữ nguyên; test mới ở mục 4 |
| `MainActivity` không theo dõi vòng đời cho mạng | Gắn `observeAppForeground` trong `onCreate` | `AppAdsRootTest` (chỉ `AppAdsRoot`, không phải `onCreate`) | Giữ nguyên |

## 4. Kiểm thử

Unit test thêm vào `NetworkDialogControllerTest` (thời gian ảo, helper `controller()` có sẵn, `RecordingAppLogger`). Quy ước: "ra nền" là `controller.onAppStopped(changingConfigurations = false)`, "quay lại" là `controller.onAppStarted()` rồi `runCurrent()`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-3 mới: quay lại không có mạng thì hiện ngay, trước 3 giây của KB-1 | Unit test | `returnOffline_showsAfterGrace_beforeLostDelay`: controller có mạng; ra nền; `connected = false`; quay lại; `advanceTimeBy(999)` → `HIDDEN`; `advanceTimeBy(2)` → `SHOWN` (tức 1 001 ms, trước 3 000 ms của đường cũ); log info có đúng một dòng `Dialog shown: offline_on_foreground` |
| KB-3 mới: máy nối lại mạng trong khoảng kiểm lại thì không hiện (rủi ro của ticket) | Unit test | `returnOffline_networkBackWithinGrace_showsNothing`: ra nền; offline; quay lại; `advanceTimeBy(500)`; `connected = true`; `advanceTimeBy(5_000)` → `HIDDEN`, không dòng `Dialog shown`. Đối chứng là test trên (cùng dựng, không bật lại mạng → `SHOWN`) |
| KB-3 mới: quay lại có mạng thì không hiện | Unit test | `returnOnline_showsNothing`: ra nền; quay lại có mạng; `advanceTimeBy(5_000)` → `HIDDEN` |
| Lần mở đầu không tính là quay lại | Unit test | `firstStart_isNotAReturn`: controller có mạng; `connected = false`; `onAppStarted()` không có `onAppStopped` trước; `advanceTimeBy(1_500)` → `HIDDEN`; `advanceTimeBy(1_501)` → `SHOWN` (đường 3 giây vẫn chạy, đối chứng) |
| Xoay màn hình không tính là quay lại | Unit test | `configurationChange_isNotAReturn`: `onAppStopped(changingConfigurations = true)`; offline; quay lại; `advanceTimeBy(1_500)` → `HIDDEN`. Đối chứng: `returnOffline_showsAfterGrace_beforeLostDelay` (cùng bước với `false` → `SHOWN` ở 1 001 ms) |
| Một lần ra nền chỉ cho một lần kiểm | Unit test | `secondStartWithoutStop_isNotAReturn`: ra nền; quay lại có mạng; `advanceTimeBy(1_500)`; `connected = false`; `onAppStarted()` lần nữa (không ra nền); `advanceTimeBy(1_500)` → `HIDDEN` (tổng 1 500 ms từ lúc mất mạng, chưa tới 3 giây) |
| Màn loading không tính (KB-3 mới, "sau màn loading") | Unit test | `returnOnLoadingScreen_ignored`: `controller(active = false)`; ra nền; offline; quay lại; `advanceTimeBy(5_000)` → `HIDDEN` |
| KB-4: cờ tắt thì không hiện | Unit test | `returnWithFlagOff_ignored`: `flags.enabled.value = emptySet()` trước khi dựng; ra nền; offline; quay lại; `advanceTimeBy(5_000)` → `HIDDEN` |
| Cờ tắt hay về loading giữa lúc kiểm lại (L-061) | Unit test | `flagTurnedOffDuringGrace_dropsShow`: ra nền; offline; quay lại; `advanceTimeBy(500)`; `flags.enabled.value = emptySet()`; `runCurrent()`; bật lại cờ ở `advanceTimeBy(100)` sau đó thì đường 3 giây bắt đầu lại: khẳng định `HIDDEN` ở 1 100 ms (lần kiểm khi quay lại đã bị hủy) và không dòng `offline_on_foreground` nào trong log. `deactivatedDuringGrace_dropsShow`: như trên với `setActive(false)`, `advanceTimeBy(5_000)` → `HIDDEN` |
| KB-1: chỉ một hộp thoại khi hai đường cùng hiện | Unit test | `returnOffline_andLostDelay_singleDialog`: ra nền; offline; quay lại; `advanceTimeBy(5_000)`; `seenStates` có đúng một `Shown(checking = false, stillOffline = false)`; log có đúng một dòng bắt đầu bằng `Dialog shown` |
| KB-3 có sẵn: hộp thoại đã hiện trước khi ra nền vẫn hiện, không hiện lại lần hai | Unit test | `shownBeforeBackground_staysOnReturn`: offline, `advanceTimeBy(3_001)` → `SHOWN`; ra nền; quay lại; `advanceTimeBy(1_500)` → vẫn `SHOWN`, log có đúng một `Dialog shown` |
| Ra nền giữa lúc kiểm lại (L-091) | Unit test | `backgroundDuringGrace_dialogStaysOnReturn`: ra nền; offline; quay lại; `advanceTimeBy(500)`; ra nền; `advanceTimeBy(600)` → `SHOWN` (job vẫn chạy khi ở nền, mục 3.1); quay lại; `advanceTimeBy(1_500)` → `SHOWN`, đúng một dòng `Dialog shown` |
| Mục 5 của spec: quảng cáo quay lại app hiện trước, hộp thoại chờ quảng cáo đóng | Unit test | `returnOffline_whileAdShowing_waitsForAd`: ra nền; offline; quay lại; `advanceTimeBy(100)`; `setFullScreenAdShowing(true)` (quảng cáo `resume` vừa hiện); `advanceTimeBy(5_000)` → `HIDDEN`, log có `Dialog waits: full_screen_ad`; `setFullScreenAdShowing(false)` → `SHOWN` |
| Sau "Thử lại" có mạng, lần quay lại sau vẫn kiểm | Unit test | `afterRecovery_nextReturnChecksAgain`: ra nền; offline; quay lại; `advanceTimeBy(1_001)` → `SHOWN`; `connected = true`; `retry()`; `runCurrent()` → `HIDDEN`; ra nền; offline; quay lại; `advanceTimeBy(1_001)` → `SHOWN` |
| `:app` báo bắt đầu và dừng, phân biệt đổi cấu hình | Robolectric (`:app`) | `AppForegroundObserverTest` (`@Config(application = Application::class)` như `ScreenOrientationTest`, `Robolectric.buildActivity(ComponentActivity::class.java)`, gọi `observeAppForeground` sau `create()`): `create().start().resume()` → `started = 1`, `stops = []`; `pause().stop()` → `stops = [false]`; `start().resume()` → `started = 2`; `recreate()` → `stops = [false, true]` |
| Dựng lại activity (L-061) | Robolectric (`:app`) | Cùng test, bước `recreate()`: activity cũ báo `onStopped(true)`. Controller nhận `true` thì bỏ qua lần bắt đầu kế (`configurationChange_isNotAReturn`). Activity mới gắn observer của nó trong `onCreate` (MainActivity gọi trong `onCreate` ở mỗi lần dựng) |
| Đưa activity ra nền rồi trở lại (L-091) | Robolectric (`:app`) | Cùng test, bước `pause().stop()` rồi `start().resume()` (tương đương `moveToState(CREATED)` rồi `RESUMED`; `ActivityScenario` cần activity khai báo trong manifest, `ComponentActivity` trơn thì không) |
| Quay lại app thật, tắt Wi-Fi và dữ liệu, có và không có quảng cáo `resume` | Kiểm tra thủ công (máy thật: khác — trình chạy kịch bản chưa có lệnh đưa app ra nền rồi mở lại mà không xóa dữ liệu (`fresh` xóa dữ liệu), và Wi-Fi nối lại khi mở màn hình chỉ thấy trên máy thật) | Bản staging, cờ `feature_network_dialog_enabled` bật: (1) mở camera, Home, tắt Wi-Fi và dữ liệu, mở lại app từ danh sách gần đây: hộp thoại hiện trong khoảng 1 giây (`adb logcat -v threadtime \| grep ' VGL\.'` có `Dialog shown: offline_on_foreground`); (2) bật lại mạng, "Thử lại", Home, khóa màn hình 1 phút, mở khóa rồi mở app khi Wi-Fi tự nối lại: không có hộp thoại oan; (3) xoay màn hình trên máy gập mở hay máy tính bảng khi không có mạng dưới 3 giây: không hiện ngay; (4) bật `ads_resume_enabled` trên staging, mở app có mạng để quảng cáo `resume` nạp sẵn, Home, tắt mạng, mở lại: quảng cáo hiện trước, hộp thoại hiện sau khi đóng quảng cáo (log `Dialog waits: full_screen_ad` rồi `Dialog shown: after_full_screen_ad`) |

Test viết trước: mọi unit test và `AppForegroundObserverTest` ở trên thất bại trên code hiện tại vì chưa có `onAppStarted`, `onAppStopped`, `observeAppForeground` (lỗi biên dịch). Để thất bại đúng lý do, bước 3 thêm vào `NetworkDialogController` hai hàm rỗng và vào `AppForegroundObserver.kt` một hàm rỗng (khung, cùng commit test); khi đó `returnOffline_showsAfterGrace_beforeLostDelay`, `returnOffline_andLostDelay_singleDialog` (thấy `Shown` ở 3 001 ms chứ không phải 1 001), `afterRecovery_nextReturnChecksAgain`, `returnOffline_whileAdShowing_waitsForAd` (không có `Dialog waits` trước 3 giây) và `AppForegroundObserverTest` đỏ; các test "không hiện" (`returnOnline_showsNothing`, `firstStart_isNotAReturn`, `configurationChange_isNotAReturn`, `returnOnLoadingScreen_ignored`, `returnWithFlagOff_ignored`, `secondStartWithoutStop_isNotAReturn`, `flagTurnedOffDuringGrace_dropsShow`, `deactivatedDuringGrace_dropsShow`, `returnOffline_networkBackWithinGrace_showsNothing`) qua ngay trên khung rỗng: đối chứng của từng test là một test "hiện" cùng bước dựng, ghi ở cột trên (L-035).

**Ngoại lệ viết test trước:** dòng gọi `observeAppForeground(onStarted = networkDialog::onAppStarted, onStopped = networkDialog::onAppStopped)` trong `MainActivity.onCreate`: `MainActivity` cần Hilt và `:app` chưa có khung test dựng nó (các test của `:app` dùng `Application` trơn). Phần quyết định nằm trong controller và observer, cả hai có test; dòng nối kiểm bằng kiểm tra thủ công (1) ở trên.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-29, trên `main` `57998e3`, một worktree tạm (đã xóa): test tạm `app/src/test/.../network/DryRunForegroundTest.kt` (không commit) đăng ký `Application.ActivityLifecycleCallbacks` (như `AdsActivityTracker`) và một `DefaultLifecycleObserver` (như `observeAppForeground`) trên `Robolectric.buildActivity(ComponentActivity::class.java)`, `@Config(application = Application::class)`; chạy `create`, `start().resume()`, `pause().stop()`, `start().resume()`, `recreate()` và in thứ tự gọi.
- **Lệnh:** `./gradlew :app:testStagingDebugUnitTest --tests '*DryRunForegroundTest*'` (sau `source ~/.local/share/camera-pose/env.sh`). Không chạy `scripts/check.sh`: tiêu chí E dưới 2 (mục 5), chạy thử chỉ để chắc hai điểm của observer.
- **Kết quả:** biên dịch được `DefaultLifecycleObserver`, `LifecycleOwner` trong `:app` (không thiếu thư viện). Thứ tự in ra: `cb_started | obs_start | cb_resumed | --home | obs_stop changing=false | cb_stopped | --back | cb_started | obs_start | cb_resumed | --recreate | obs_stop changing=true | cb_started | cb_resumed`. Vậy: (1) callback của `Application` (nơi khung quảng cáo quyết quảng cáo `resume`) chạy trước ON_START của observer, nên quảng cáo được quyết trước lần kiểm của controller; (2) Robolectric báo `isChangingConfigurations = false` khi dừng thường và `true` khi `recreate()`, nên `AppForegroundObserverTest` viết được như mục 4. Test tạm cố ý thất bại ở câu `assertEquals("x", ...)` để in thứ tự; không còn lỗi khác.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7, module core dùng chung (`:core:network`). Mức Cao, cần duyệt chéo. Bảng dưới chỉ để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:core:network`, `:app`), thêm hàm công khai cho controller |
| B. Độ rõ của yêu cầu | 2 | Spec network đang `changed` chờ Nhật xác nhận lại sau T-074, tiêu chí mới chưa có trong spec (câu hỏi 1), khoảng kiểm lại và định nghĩa "quay lại" do plan đề xuất (câu hỏi 2, 3) |
| C. Khả năng kiểm chứng tự động | 2 | Logic có unit test trên thời gian ảo, observer có test Robolectric; nối lại Wi-Fi thật, máy ngủ và thứ tự với quảng cáo thật chỉ kiểm được trên máy thật (vòng đời ứng dụng) |
| D. Ảnh hưởng chéo | 2 | `NetworkDialogController.kt` và `NetworkDialogControllerTest.kt` có thể cùng bị T-076 sửa (mục 6); hành vi hộp thoại đi cùng quảng cáo `resume` của `:core:ads` (không sửa) |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu có sẵn: vòng kiểm như `retry()`, observer vòng đời như `AdsActivityTracker`, bản mẫu T-017 |
| **Tổng** | 10 | **Mức: Cao** (điểm chặn 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NetworkDialogController` (dùng bởi `MainActivity`, `NetworkDialogHost`; `NetworkStatus`, `NetworkFailureReporter` mà feature dùng không đổi), `NetworkSettings` (thêm hằng `internal`, không đổi `all()`), `MainActivity.onCreate`.
- Task khác có thể xung đột: **T-076** (đang soạn plan song song, "có mạng" theo `NET_CAPABILITY_VALIDATED`). T-076 chắc sửa `ConnectivitySource.kt` (T-077 không sửa) và có thể sửa `Fakes.kt`, `NetworkDialogControllerTest.kt` hay KDoc của `NetworkDialogController.kt`, những file T-077 cũng sửa: chỉ là thêm ở chỗ khác nhau, git thường tự gộp; task chạy sau cập nhật nhánh theo `merge-if-green.sh --update`. Ảnh hưởng về hành vi: nếu T-076 đòi `VALIDATED`, máy vừa nối lại Wi-Fi cần thêm thời gian xác thực (thường dưới 1 tới vài giây) trước khi `isConnected()` trả `true`, nên 1 giây kiểm lại có thể ngắn; câu hỏi 2 đề xuất cách chọn.
- T-074 (đã merge): plan dựa vào `setFullScreenAdShowing` của lượt 2 và quảng cáo `resume` của lượt 2; không sửa file của T-074 ngoài `MainActivity.kt` (một dòng trong `onCreate`, không chạm `AppAdsRoot`).
- Đề xuất thứ tự chạy: T-076 trước T-077 trong cùng danh sách, để giá trị khoảng kiểm lại được chốt theo định nghĩa "có mạng" cuối cùng; hai task không phụ thuộc nhau về code, chạy ngược thứ tự cũng được.

## 7. Điều kiện dừng riêng

- Cần sửa `:core:ads` (thứ tự quyết hay hiện quảng cáo `resume`) để hộp thoại và quảng cáo đúng thứ tự của mục 3.1: dừng (điểm chặn 1, quảng cáo).
- Cần thêm khóa Remote Config cho khoảng kiểm lại (đổi `NetworkSettings.all()`, bảng khóa): dừng, vì câu hỏi 2 chốt hằng trong code; trừ khi câu trả lời câu hỏi 2 chọn khóa Remote Config.
- Test cũ của `NetworkDialogControllerTest` đỏ sau khi thêm `foregroundJob`: dừng theo điều kiện 3 (plan nói đường cũ không đổi).

## 8. Câu hỏi cho người duyệt

1. **Tiêu chí mới trong spec network.** Spec đang `changed` (chờ xác nhận lại sau T-074) và chưa có tiêu chí này; plan không tự sửa spec. Đề xuất Nhật thêm vào KB-3, sau tiêu chí có sẵn, đúng câu sau, rồi xác nhận lại spec (`status: confirmed`) trước khi duyệt plan:
   - [ ] Khi người dùng đưa app ra nền rồi quay lại một màn sau màn loading mà máy không có mạng thì hộp thoại hiện ngay khi app trở lại, không chờ thời gian chờ của KB-1: máy được kiểm lại tối đa 1 giây để kịp nối lại mạng, có mạng trong khoảng đó thì hộp thoại không hiện. Lần mở app đầu tiên và xoay màn hình không tính là quay lại. Khi một quảng cáo toàn màn hình hiện lúc quay lại thì hộp thoại hiện sau khi quảng cáo đóng (mục 5).
   - Đề xuất của AI: thêm đúng câu trên.
2. **Khoảng kiểm lại khi quay lại.** Hiện ngay (như bản mẫu) dễ hiện oan khi Wi-Fi đang nối lại sau khi mở màn hình, hay khi hệ thống vừa gỡ chặn mạng của app lúc trở về từ nền (Doze, tiết kiệm dữ liệu: `getActiveNetwork` trả `null` khi mạng của app đang bị chặn). Chờ 3 giây như KB-1 thì không giải quyết ticket. Đề xuất của AI: 1 giây, kiểm mỗi 250 ms như "Thử lại", hằng trong code (`NetworkSettings.FOREGROUND_GRACE_MILLIS`), không là khóa Remote Config. Nếu T-076 chọn đòi `NET_CAPABILITY_VALIDATED` thì nâng lên 2 giây (bằng `RETRY_MAX_MILLIS`), vì xác thực mạng cần thêm thời gian; người duyệt chốt một số trước khi duyệt cả hai plan.
3. **"Quay lại app" tính thế nào.** Ticket ghi `MainActivity.onStart` gọi hàm và bỏ qua lần bắt đầu đầu tiên (đếm như bản mẫu). Plan tính "quay lại" là `MainActivity` bắt đầu sau một lần dừng không do đổi cấu hình (`onAppStopped(false)`), gắn bằng observer vòng đời: lần đầu tự bị bỏ qua, xoay màn hình trên màn lớn (T-056) không bị tính. Hệ quả: trở về từ app khác mở từ app (chia sẻ ảnh, cài đặt hệ thống) và từ quảng cáo toàn màn hình mở dạng activity riêng của SDK cũng là một lần quay lại. Đề xuất của AI: đồng ý cách này, coi đó là thay cho câu "MainActivity.onStart gọi hàm này" của ticket.
4. **Thứ tự với quảng cáo quay lại app khi không có mạng.** Khung không tải quảng cáo mới khi mất mạng, nhưng quảng cáo `resume` nạp sẵn vẫn hiện. Đề xuất của AI: quảng cáo hiện trước, hộp thoại hiện sau khi quảng cáo đóng (đúng mục 5 của spec, không đổi `:core:ads`); nhờ 1 giây kiểm lại, hộp thoại không lóe lên trước quảng cáo. Mạng có lại trong lúc quảng cáo hiện thì hộp thoại vẫn hiện khi quảng cáo đóng, như đường "mất mạng quá 3 giây" hiện nay; người dùng bấm "Thử lại" là đóng (KB-2). Nếu muốn kiểm lại mạng lúc quảng cáo đóng thì đó là đổi chung cho mọi lý do hiện, nên để ticket riêng.
5. **Ra nền giữa lúc kiểm lại.** Đề xuất của AI: không hủy lần kiểm; nếu vẫn không có mạng thì hộp thoại hiện khi app ở nền (giống đường 3 giây hiện nay) và còn đó khi quay lại (KB-3). Đồng ý không?
6. **Lỗi của `plan-check.py` chỉ người sửa được:** xem kết quả ở mô tả PR; plan chờ spec `confirmed` (câu hỏi 1) rồi mới duyệt.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
