---
ticket: T-078
title: Hộp thoại mất mạng hiện ngay khi mở lại app sau khi hệ thống đóng app ở nền
status: draft
version: 1
risk_score:
risk_level: high
token_estimate: 7.0M         # cỡ S, trung vị của scripts/token-usage.py estimate
blocking_points: [core]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-078: HỘP THOẠI MẤT MẠNG HIỆN NGAY KHI MỞ LẠI APP SAU KHI HỆ THỐNG ĐÓNG APP Ở NỀN

## 1. Tóm tắt

Thêm vào `NetworkDialogController` của `:core:network` hàm `onAppRestored()`, mà `MainActivity.onCreate` gọi khi activity được dựng lại từ trạng thái đã lưu (`savedInstanceState != null`). Nếu trong tiến trình này chưa có activity nào bắt đầu, đây là lần mở lại sau khi hệ thống đóng app ở nền. Nếu activity trước đã rời app, đây là lần mở lại sau khi hệ thống hủy activity ở nền mà tiến trình còn sống. Controller khi đó ghi nhớ và chạy lần kiểm 2 giây của T-077 ngay khi app được bật (`setActive(true)`). Xoay màn hình (cùng tiến trình) và lần mở mới (không có trạng thái đã lưu) không tính. Chạy thử đã xác nhận điều ticket nêu: điều hướng khôi phục thẳng màn sau loading, không dựng lại loading. Chạy thử cũng thấy `setActive(true)` tới sau `onStart`, nên `onAppStarted()` của T-077 không làm được việc này.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/network/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-3, tiêu chí thứ hai (T-077) | Khi người dùng đưa app ra nền rồi quay lại một màn sau màn loading mà máy không có mạng thì hộp thoại hiện ngay khi app trở lại, không chờ thời gian chờ của KB-1: máy được kiểm lại tối đa 2 giây, có mạng trong khoảng đó thì không hiện. Lần mở app đầu tiên và xoay màn hình không tính là quay lại | Áp cả khi tiến trình đã bị hệ thống đóng lúc app ở nền: người dùng mở lại từ màn đa nhiệm, màn sau loading được khôi phục. "Lần mở app đầu tiên" là lần mở không có trạng thái đã lưu (mở mới, sau khi vuốt bỏ app hay sau khi cài) |
| KB-1, "chỉ có một hộp thoại" | Kể cả khi nhiều lần tải cùng thất bại | Lần kiểm khi khôi phục và đường "mất mạng quá 3 giây" cùng đi qua `show()`; chỉ một hộp thoại, một dòng log `Dialog shown` |
| KB-4 | Cờ `feature_network_dialog_enabled` tắt thì không hiện | Lần kiểm khi khôi phục chỉ chạy khi controller `armed`; cờ tắt lúc khôi phục thì bỏ lần kiểm, không để dành tới lúc cờ bật |

Thiết kế tham chiếu: không có ảnh mới; hộp thoại dùng nguyên `NetworkDialog` của `features/network/design/README.md`.

ADR liên quan: ADR-0024 (app luôn cần mạng, popup chung do `:app` chặn), ADR-0019 (log), ADR-0001, ADR-0029, ADR-0031.

## 3. Giải pháp

### 3.1. Cách làm

**Điều đã kiểm (mục 4.1):** `AppNavHost` dùng `rememberNavController()`, nên back stack được lưu cùng trạng thái của activity. Khi hệ thống đóng tiến trình rồi người dùng mở lại, activity mới nhận `savedInstanceState` và màn trên cùng được khôi phục; màn loading đã bị bỏ khỏi back stack (`popUpTo<LoadingDestination> { inclusive = true }`) nên không chạy lại. Khung hình đầu báo `pastLoading = false` (chưa có `currentBackStackEntry`), khung hình kế báo `true`; `setActive(true)` tới sau `onStart`. Controller là đối tượng mới của tiến trình mới (`@Singleton` của Hilt), nên `leftApp = false` và lần bắt đầu này bị coi là lần mở đầu.

Cùng lỗi có trong một trường hợp thứ hai (rà soát độc lập): tiến trình còn sống nhưng activity bị hủy khi ở nền (hệ thống thu hồi activity, hay tùy chọn nhà phát triển "Don't keep activities"). Lần kiểm của T-077 chạy ở `onStart`, vì controller vẫn `armed`, nhưng khung hình đầu của activity mới gọi `setActive(false)`, `disarm()` hủy lần kiểm, và `setActive(true)` sau đó không chạy lại nó. Hộp thoại khi đó cũng chỉ hiện sau 3 giây.

**`:core:network`, `NetworkDialogController`** (chỉ thêm, không đổi đường cũ):

- Hai trường mới, `@Volatile`:
  - `startedOnce = false`: đã có một lần `onAppStarted()` trong tiến trình này.
  - `checkWhenActive = false`: một lần mở lại (sau khi tiến trình bị đóng, hay activity bị hủy ở nền) đang chờ app được bật.
- `fun onAppRestored()`: luồng chính, `MainActivity.onCreate` gọi khi `savedInstanceState != null`. Thân hàm: `if (!startedOnce || leftApp) checkWhenActive = true`.
  - `!startedOnce`: tiến trình mới, chưa có lần bắt đầu nào. Đây là trường hợp tiến trình bị đóng.
  - `leftApp`: activity cũ dừng không do đổi cấu hình, rồi bị hủy khi ở nền. Đây là trường hợp tiến trình còn sống.
  - Xoay màn hình cũng dựng lại activity với `savedInstanceState`, nhưng `startedOnce` là `true` và `leftApp` là `false` (dừng với `changingConfigurations = true`), nên lệnh gọi bị bỏ qua.
- `onAppStarted()`: thêm `startedOnce = true` ở dòng đầu, trước `if (!leftApp) return`. Phần còn lại giữ nguyên.
- Khối `collectLatest { (active, enabled) -> ... }` trong `init`, ngay đầu khối:
  ```kotlin
  val restoredReturn = active && checkWhenActive
  if (active) checkWhenActive = false
  ```
  Trong nhánh `armed`, trước `connectivity.connectivity().collectLatest`:
  ```kotlin
  if (restoredReturn) {
      foregroundJob?.cancel()
      foregroundJob = scope.launch { checkOnForeground() }
  }
  ```
  Cờ được bỏ ở lần đầu app được bật, kể cả khi cờ tính năng đang tắt (KB-4). Như vậy không có lần kiểm nào để dành tới lúc cờ bật giữa chừng.
  - `checkOnForeground()` và `disarm()` dùng lại của T-077: `disarm()` hủy `foregroundJob`; `checkOnForeground()` kiểm lại `armed` trước khi hiện.
  - Cờ tính năng ở lần khôi phục là giá trị Remote Config đã kích hoạt từ trước. Cờ vừa bật trên máy chủ mà chưa tới máy thì lần kiểm bị bỏ, như mọi lần cờ tắt. Bản staging bật cờ trên máy (`StagingFeatureFlagsModule`), nên không gặp.
  - Không thêm lý do log mới: dòng `Dialog shown: offline_on_foreground` giống lần quay lại thường, vì với người dùng đây cũng là một lần quay lại.
- KDoc của lớp, câu về quay lại app, thêm: "A return after the system closed the app in the background ([onAppRestored]) is checked the same way once the app is active."
- KDoc của `onAppRestored`: "Called by :app from onCreate when the activity is restored from saved state. Main thread. Before any start in this process, or after the app left: this is a return after the system closed the app or its activity in the background, and the network is checked as in [onAppStarted] once the app is past loading (plan T-078). A rotation is ignored."
- Log (ADR-0019, thẻ `Network`): chỉ dòng có sẵn qua `show()`. Không thêm sự kiện phân tích.

**`:app`, `MainActivity.onCreate`:** ngay sau dòng `observeAppForeground(...)`, thêm:

```kotlin
// Reopened after the system closed the app in the background: checked like a return (spec network, KB-3; plan T-078).
if (savedInstanceState != null) networkDialog.onAppRestored()
```

**Cách làm khác đã cân nhắc:**
- Đặt `leftApp = true` trong `onAppRestored()` rồi để `onAppStarted()` chạy lần kiểm: không được, vì lúc `onStart` controller chưa `armed` (mục 4.1), lần bắt đầu bị bỏ qua.
- Bỏ cờ khi `setActive(false)`, để khôi phục vào màn loading không chạy lần kiểm: không được, vì khôi phục vào màn sau loading cũng báo `setActive(false)` ở khung hình đầu (mục 4.1).

**Test khôi phục điều hướng** (`NavRestoreTest`, tệp mới): giữ thành test thật điều mà cách làm dựa vào, để một thay đổi sau này ở `AppNavHost` không làm hỏng T-078 mà không test nào đỏ (rà soát độc lập). Test dựng một host theo đúng cách của `AppNavHost`; dựng thẳng `AppNavHost` thì cần Hilt cho màn loading, mà `:app` chưa có khung test cho Hilt. Đã chạy thử, qua (4.1). Nội dung để phiên chép nguyên:

```kotlin
package com.vglstudio.camera.pose.line.guide.navigation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.feature.loading.navigation.LoadingDestination
import com.vglstudio.camera.pose.line.guide.feature.settings.navigation.SettingsDestination
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * What NetworkDialogController.onAppRestored relies on (plan T-078): when the activity is restored from saved state,
 * the back stack of rememberNavController comes back past loading without composing loading again, and the first
 * frame reports "not past loading" before the restored screen reports "past loading". The host below follows
 * AppNavHost (rememberNavController, currentBackStackEntryAsState, LaunchedEffect(pastLoading), loading popped
 * inclusive); AppNavHost itself needs Hilt for the loading screen, which :app has no test setup for.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class NavRestoreTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun restoredPastLoading_skipsLoading_firstFrameNotPastLoading() {
        val events = mutableListOf<String>()
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            val navController = rememberNavController()
            val entry by navController.currentBackStackEntryAsState()
            val pastLoading = entry?.destination?.let { !it.hasRoute<LoadingDestination>() } ?: false
            val onPastLoading by rememberUpdatedState<(Boolean) -> Unit>({ events += "pastLoading=$it" })
            LaunchedEffect(pastLoading) { onPastLoading(pastLoading) }
            NavHost(navController, startDestination = LoadingDestination) {
                composable<LoadingDestination> {
                    Text("loading")
                    LaunchedEffect(Unit) {
                        events += "loading"
                        navController.navigate(SettingsDestination) {
                            popUpTo<LoadingDestination> { inclusive = true }
                        }
                    }
                }
                composable<SettingsDestination> { Text("main") }
            }
        }
        composeRule.waitForIdle()
        // Control: the first launch goes through loading.
        assertEquals(listOf("pastLoading=false", "loading", "pastLoading=true"), events)

        events.clear()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertEquals(listOf("pastLoading=false", "pastLoading=true"), events)
    }
}
```

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogController.kt` | `onAppRestored()`, `startedOnce`, `checkWhenActive`, lần kiểm khi được bật trong `init`, `startedOnce = true` trong `onAppStarted()`, KDoc |
| Sửa | `core/network/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogControllerTest.kt` | Các test mới của mục 4 và hai hàm phụ `restoredProcess()`, `firstFrames()`, thêm vào cuối lớp, không sửa test cũ |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | Gọi `networkDialog.onAppRestored()` trong `onCreate` khi `savedInstanceState != null` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/navigation/NavRestoreTest.kt` | Test Robolectric khôi phục điều hướng, nội dung ở mục 3.1 |
| Sửa | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/network/MainActivityForegroundWiringTest.kt` | Thêm test `mainActivity_reportsRestore`, dùng lại `codeOnly`, `bodyOf`, `ON_CREATE` của lớp; không sửa test cũ |

`scripts/tests/run.sh` có nhắc `MainActivity.kt`: dòng đếm `exposeTestTagsForUiAutomator()` bằng 1. Không bị ảnh hưởng, vì plan chỉ thêm một dòng `if` (L-020). Không đổi file khác mà nơi khác đọc: không thêm khóa Remote Config, không ảnh chuẩn, không tài nguyên chuỗi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: dùng `feature_network_dialog_enabled` có sẵn qua `armed`; không thêm cờ.
- Quảng cáo: Không chạm. Lần kiểm đi qua `show()`, nên vẫn chờ quảng cáo toàn màn hình đóng như mục 5 của spec (`setFullScreenAdShowing`, T-074 lượt 2).
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `savedInstanceState` là tham số có sẵn của `onCreate`; kiểm mạng dùng `ACCESS_NETWORK_STATE` có sẵn của `:core:network` (T-011).
- **Thư viện và SDK:** Không thêm. `:core:network` chỉ dùng `kotlinx.coroutines` có sẵn; `:app` không dùng lớp mới.

### 3.4. Phương án dự phòng

Không có. Điểm chưa chắc là thứ tự khôi phục và lúc `setActive(true)` tới. Cả hai đã chạy thử ở 4.1, và cách làm không phụ thuộc thứ tự khung hình (cờ chờ tới lần `active` đầu).

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (2026-10-30): 84 test dùng tên của các file plan sửa; "Cần xem trước" 0; "Nên xem" 11. Đã đọc từng test.
- `MergedManifestTest.mergedManifest_hasNoAdGalleryActivity`, `ScreenOrientationTest.manifest_keepsPortrait_soPhonesStartWithoutARelaunch`: đọc manifest của `MainActivity`; plan không đổi manifest.
- `MainActivityForegroundWiringTest.mainActivity_observesAppForeground`: đếm đúng một lời gọi `observeAppForeground(...)` trong `onCreate`; dòng `if` mới không khớp mẫu đó, nên vẫn đúng một.
- `NativeFullScreenOverlayTest.recreate_keepsDeadline_notRestarted`, `PhotoStoreTest.save_insertThrowsIllegalArgument_isIOException`, `LoadingRouteTest.backgroundWhileWaiting_thenResumed_showsOnce`: chỉ trùng chữ `onCreate` hay `MainActivity`, không dùng controller.
- `NetworkDialogControllerTest.adCloses_withoutLoss_staysHidden`, `adStartsWhileDialogShown_hidesThenShowsAgainOnClose`, `lostWhileAdShowing_dialogWaits_thenShowsWhenAdCloses`, `returnOffline_whileAdShowing_waitsForAd`: đường chờ quảng cáo không đổi.
- `NetworkDialogControllerTest.inactiveWhileWaiting_dropsPending`: gọi `onAppStarted`; dòng `startedOnce = true` mới không đổi kết quả của nó, vì test không gọi `onAppRestored`.

Không câu khẳng định nào bị đổi, vì plan chỉ thêm đường mới. Chạy thử ở 4.1 với code của mục 3.1: cả 39 test cũ của `NetworkDialogControllerTest` qua.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Mở lại sau khi tiến trình bị đóng, không có mạng: hộp thoại hiện sau 3 giây của KB-1 | Hiện sau tối đa 2 giây kiểm lại | Không có test khẳng định hành vi cũ (`firstStart_isNotAReturn` của T-077 không có `onAppRestored`) | Giữ nguyên; test mới ở mục 4 |
| `onAppStarted()` chỉ đọc `leftApp` | Ghi thêm `startedOnce = true` | `NetworkDialogControllerTest` của T-077 (15 test quay lại), `inactiveWhileWaiting_dropsPending` | Giữ nguyên |
| `MainActivity.onCreate` không đọc `savedInstanceState` cho mạng | Gọi `onAppRestored()` khi có trạng thái đã lưu | `MainActivityForegroundWiringTest.mainActivity_observesAppForeground` | Giữ nguyên |

## 4. Kiểm thử

Unit test thêm vào cuối `NetworkDialogControllerTest`, trên thời gian ảo, dùng các hàm phụ có sẵn `controller()`, `backToApp()`, `toBackground()`, `infoMessages()`, `shownLines()` và `RecordingAppLogger`. Hai hàm phụ mới dựng lại thứ tự thật đã đo ở 4.1. `firstFrames()` có `runCurrent()` giữa hai lần `setActive`: trên máy, hai lần gọi nằm ở hai khung hình; gọi liền nhau thì `StateFlow` gộp lại và controller chỉ thấy `true`, test qua dù code sai (đã gặp khi chạy thử, 4.1).

```kotlin
/** A new process restored past loading (plan T-078): onCreate with saved state, onStart, then the first frames. */
private fun TestScope.restoredProcess(): NetworkDialogController {
    val controller = controller(active = false)
    controller.onAppRestored()
    backToApp(controller)
    firstFrames(controller)
    return controller
}

/** The first frames of a new activity: not past loading, then the restored screen (NavRestoreTest). */
private fun TestScope.firstFrames(controller: NetworkDialogController) {
    controller.setActive(false)
    runCurrent()
    controller.setActive(true)
    runCurrent()
}
```

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-3: mở lại sau khi tiến trình bị đóng, không có mạng, thì hiện sau tối đa 2 giây, trước 3 giây của KB-1 | Unit test | `restoredOffline_showsAfterGrace_beforeLostDelay`: `connected = false`; `restoredProcess()`; `advanceTimeBy(1_999)` → `HIDDEN`; `advanceTimeBy(2)` → `SHOWN`; `shownLines()` bằng `["Dialog shown: offline_on_foreground"]` |
| KB-3: có mạng thì không hiện | Unit test | `restoredOnline_showsNothing`: `restoredProcess()`; `advanceTimeBy(5_000)` → `HIDDEN`, `shownLines()` rỗng |
| KB-3: mạng có lại trong lúc kiểm thì không hiện | Unit test | `restoredNetworkBackWithinGrace_showsNothing`: offline; `restoredProcess()`; `advanceTimeBy(500)`; `connected = true`; `advanceTimeBy(5_000)` → `HIDDEN`. Đối chứng: `restoredOffline_showsAfterGrace_beforeLostDelay` (cùng dựng, không bật lại mạng → `SHOWN`) |
| Lần mở mới (không có trạng thái đã lưu) không tính | Unit test | `freshStart_isNotARestore`: offline; `controller(active = false)`; `backToApp`; `firstFrames`; `advanceTimeBy(2_500)` → `HIDDEN`; `advanceTimeBy(501)` → `SHOWN`, `shownLines()` bằng `["Dialog shown: device_offline delay=3s"]` (đường 3 giây, đối chứng) |
| Xoay màn hình (dựng lại trong cùng tiến trình, có `savedInstanceState`) không tính | Unit test | `recreatedInSameProcess_isNotARestore`: `controller()`; `backToApp` (lần bắt đầu đầu); `onAppStopped(changingConfigurations = true)`; `onAppRestored()`; offline; `backToApp`; `firstFrames`; `advanceTimeBy(2_500)` → `HIDDEN` (lần kiểm 2 giây thì đã `SHOWN` ở 2 000 ms); `advanceTimeBy(501)` → `shownLines()` bằng `["Dialog shown: device_offline delay=3s"]` (đối chứng). Bỏ điều kiện của `onAppRestored()` thì test này đỏ (đã chạy thử, 4.1) |
| Activity bị hủy ở nền, tiến trình còn sống (rà soát độc lập) | Unit test | `activityDestroyedInBackground_isAReturn`: `controller()`; `backToApp`; `toBackground()`; `onAppRestored()`; offline; `backToApp`; `firstFrames`; `advanceTimeBy(1_999)` → `HIDDEN`; `advanceTimeBy(2)` → `shownLines()` bằng `["Dialog shown: offline_on_foreground"]`. Bỏ vế `leftApp` thì đỏ (4.1) |
| KB-4: cờ tắt lúc khôi phục thì không kiểm, kể cả khi cờ bật sau | Unit test | `restoredWithFlagOff_ignored_evenWhenFlagTurnsOnLater`: `flags.enabled.value = emptySet()`; offline; `restoredProcess()`; `advanceTimeBy(5_000)` → `HIDDEN`; bật cờ; `advanceTimeBy(2_500)` → `HIDDEN` (không có lần kiểm 2 giây để dành); `advanceTimeBy(501)` → `shownLines()` bằng `["Dialog shown: device_offline delay=3s"]` (đường 3 giây sau khi bật cờ, đối chứng) |
| Về loading giữa lúc kiểm (L-061) | Unit test | `restoredDeactivatedDuringGrace_dropsShow`: offline; `restoredProcess()`; `advanceTimeBy(500)`; `setActive(false)`; `advanceTimeBy(5_000)` → `HIDDEN`, `infoMessages()` không có dòng chứa `offline_on_foreground` |
| KB-1: chỉ một hộp thoại khi hai đường cùng hiện | Unit test | `restoredOffline_andLostDelay_singleDialog`: offline; `restoredProcess()`; `advanceTimeBy(5_001)` (qua cả 2 giây và 3 giây) → `SHOWN`, `shownLines()` bằng đúng `["Dialog shown: offline_on_foreground"]` (khung rỗng: `device_offline delay=3s`) |
| Ra nền giữa lúc kiểm (L-091) | Unit test | `restoredBackgroundDuringGrace_dialogStays`: offline; `restoredProcess()`; `advanceTimeBy(500)`; `toBackground()`; `advanceTimeBy(1_600)` → `SHOWN` ở 2 100 ms (lần kiểm không bị hủy khi ra nền, như T-077; khung rỗng: `HIDDEN`) |
| Khôi phục điều hướng: không dựng lại loading, khung hình đầu báo chưa qua loading | Robolectric (`:app`) | `NavRestoreTest.restoredPastLoading_skipsLoading_firstFrameNotPastLoading` (mục 3.1): lần mở đầu cho `[pastLoading=false, loading, pastLoading=true]` (đối chứng); sau `emulateSavedInstanceStateRestore()` cho `[pastLoading=false, pastLoading=true]` |
| `MainActivity` báo lần khôi phục | Unit test quét mã nguồn (`:app`) | `MainActivityForegroundWiringTest.mainActivity_reportsRestore`: trên `codeOnly(...)` của `MainActivity.kt`, đúng một lời gọi khớp `\bif\s*\(\s*savedInstanceState\s*!=\s*null\s*\)\s*networkDialog\.onAppRestored\(\s*\)`, nằm trong thân `override fun onCreate` (`bodyOf(code, ON_CREATE)`). Đối chứng: tệp có `class MainActivity`. Quét theo ký hiệu, không theo chuỗi con |
| Mở lại thật sau khi tiến trình bị đóng | Kiểm tra thủ công (máy thật: trình chạy kịch bản không có lệnh đóng tiến trình rồi mở lại từ màn đa nhiệm mà giữ trạng thái; `fresh` xóa dữ liệu) | Bản staging, cờ `feature_network_dialog_enabled` bật, máy nối USB: (1) mở camera (hay thư viện), Home, `adb shell am kill com.vglstudio.camera.pose.line.guide.staging` (đóng như khi thiếu bộ nhớ, chỉ được khi app ở nền), tắt Wi-Fi và dữ liệu, mở lại app từ màn đa nhiệm: màn đang xem hiện lại, không qua loading, hộp thoại hiện trong khoảng 2 giây (`adb logcat -v threadtime \| grep ' VGL\.'` có `Dialog shown: offline_on_foreground`); (2) như (1) nhưng giữ mạng: không có hộp thoại; (3) vuốt bỏ app khỏi màn đa nhiệm rồi mở lại khi không có mạng: app mở từ loading như lần mở mới, không có dòng `offline_on_foreground` |

Test viết trước: mọi test ở trên thất bại trên code hiện tại vì chưa có `onAppRestored` (lỗi biên dịch). Để thất bại đúng lý do, bước 3 thêm vào `NetworkDialogController` một hàm rỗng `fun onAppRestored() {}` (khung, cùng commit test). Trên khung đó (đã chạy thử, 4.1), có 4 test controller đỏ:
- `restoredOffline_showsAfterGrace_beforeLostDelay` (`Hidden` ở 2 001 ms);
- `restoredOffline_andLostDelay_singleDialog` (lý do `device_offline delay=3s`);
- `restoredBackgroundDuringGrace_dialogStays` (`Hidden` ở 2 100 ms);
- `activityDestroyedInBackground_isAReturn` (chưa có dòng `Dialog shown` ở 2 001 ms).

Thêm vào đó, `mainActivity_reportsRestore` đỏ vì chưa có lời gọi. Sáu test "không hiện" qua ngay trên khung rỗng; đối chứng của từng test là một test "hiện" cùng cách dựng, hay bước đối chứng ghi trong chính test (L-035).

**Ngoại lệ viết test trước:** `NavRestoreTest` qua ngay trên code hiện tại. Test này giữ hành vi có sẵn của Navigation mà cách làm dựa vào, không kiểm code mới, nên không thể đỏ trước; câu đối chứng (lần mở đầu đi qua loading) cho thấy test đọc đúng chuỗi sự kiện. Dòng `if` trong `MainActivity.onCreate` có `mainActivity_reportsRestore` (quét mã nguồn), vì `MainActivity` cần Hilt và `:app` chưa có khung test dựng nó (như T-077). Kiểm tra thủ công (1) xác nhận nó chạy trên máy.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-30, trên `main` `379065e`, nhánh `claude/plan-T-078-mo-lai-sau-khi-dong` (code và test tạm, đã hoàn tác, không commit).
- **Thứ tự khôi phục:**
  - Test tạm `app/src/test/.../network/DryRunRestoreTest.kt`, Robolectric, `@Config(application = Application::class)`. Test dựng một `NavHost` nhỏ theo đúng cách của `AppNavHost`: `rememberNavController()`, `currentBackStackEntryAsState()`, `pastLoading` theo `hasRoute<LoadingDestination>()`, `LaunchedEffect(pastLoading)`. Loading chuyển sang `SettingsDestination` với `popUpTo<LoadingDestination> { inclusive = true }`. Sau đó `StateRestorationTester.emulateSavedInstanceStateRestore()` mô phỏng lưu rồi khôi phục trạng thái, như khi tiến trình bị đóng.
  - Đích phải là lớp `@Serializable` của module feature: lớp khai báo trong source set test của `:app` báo `Serializer ... is not found`.
  - Lệnh: `./gradlew :app:testStagingDebugUnitTest --tests '*DryRunRestoreTest*'`, sau `source ~/.local/share/camera-pose/env.sh`.
  - Kết quả: `past=false | loadingComposed | past=true | cameraComposed | --restore | past=false | cameraComposed | past=true`. Vậy khi khôi phục, màn loading không dựng lại và màn sau loading hiện ngay. Khung hình đầu vẫn báo `past=false` trước `true`. Đây là lý do mục 3.1 bỏ cờ ở lần `active` đầu, không bỏ ở `setActive(false)`.
- **Controller:**
  - Thêm tạm 10 test controller của mục 4 vào `NetworkDialogControllerTest`. Lệnh: `./gradlew :core:network:testDebugUnitTest --rerun`.
  - Lần chạy đầu gọi `setActive(false)` rồi `setActive(true)` liền nhau. Bỏ điều kiện của `onAppRestored()` thì `recreatedInSameProcess_isNotARestore` vẫn qua, vì `StateFlow` gộp hai giá trị (rà soát độc lập báo điểm chặn này). Sửa bằng `firstFrames()` có `runCurrent()` ở giữa.
  - Với `firstFrames()`, khung rỗng (`onAppRestored()` không làm gì): 49 test, 4 đỏ đúng lý do (bốn test kể ở mục 4), 45 qua, gồm cả 39 test cũ.
  - Code đủ của mục 3.1: 49 test, 0 đỏ.
  - Đột biến: bỏ cả điều kiện (`checkWhenActive = true` luôn) thì chỉ `recreatedInSameProcess_isNotARestore` đỏ (`Shown` thay vì `Hidden`). Bỏ vế `leftApp` thì chỉ `activityDestroyedInBackground_isAReturn` đỏ.
- **`NavRestoreTest`:** viết ở dạng thật (mục 3.1), lệnh `./gradlew :app:testStagingDebugUnitTest --tests '*NavRestoreTest*'`: 1 test, qua.
- **Không chạy `scripts/check.sh`:** tiêu chí E dưới 2 (mục 5); không có kỹ thuật hay thư viện mới, chạy thử chỉ để chắc thứ tự khôi phục và thời gian của test.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7, module core dùng chung (`:core:network`). Mức Cao, cần duyệt chéo. Bảng dưới chỉ để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Hai module (`:core:network`, `:app`), thêm một hàm công khai, một dòng ở `MainActivity` và một test Robolectric |
| B. Độ rõ của yêu cầu | 1 | KB-3 đã `confirmed`, ticket nêu rõ trường hợp; chỉ còn câu hỏi 1 về khôi phục vào màn loading |
| C. Khả năng kiểm chứng tự động | 2 | Logic có unit test trên thời gian ảo; tiến trình bị đóng thật và màn đa nhiệm chỉ kiểm được trên máy (mục 4) |
| D. Ảnh hưởng chéo | 1 | Dùng lại `checkOnForeground`, `foregroundJob`, `disarm` của T-077; `NetworkStatus`, `NetworkFailureReporter` không đổi |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của T-077; `StateRestorationTester` đã dùng ở `:core:ads` |
| **Tổng** | 7 | **Mức: Cao** (điểm chặn 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `NetworkDialogController`: dùng bởi `MainActivity` và `NetworkDialogHost`; `NetworkStatus`, `NetworkFailureReporter` mà feature dùng không đổi.
  - `MainActivity.onCreate`: thêm một dòng.
  - `MainActivityForegroundWiringTest`: thêm một test.
- Task khác có thể xung đột: không có plan nào khác đang chờ. T-076 và T-077 đã merge (`e9f5a5b`, `5d85b6b`); plan này dựa vào `onAppStarted`, `checkOnForeground`, `foregroundJob` và `FOREGROUND_GRACE_MILLIS` của T-077.
- Đề xuất thứ tự chạy: không có ràng buộc; T-077, ticket phụ thuộc, đã merge.

## 7. Điều kiện dừng riêng

- Test cũ của `NetworkDialogControllerTest` đỏ sau khi thêm `startedOnce` hay lần kiểm trong `init`: dừng theo điều kiện 3, vì plan nói đường cũ không đổi.
- Cần sửa `AppNavHost` hay `:feature:loading` để lần khôi phục đi qua loading: dừng, vì đó là đổi hành vi của spec loading, ngoài ticket.

## 8. Câu hỏi cho người duyệt

1. **Khôi phục vào màn loading.** Tiến trình bị đóng khi người dùng còn ở màn loading, rồi người dùng mở lại: loading chạy lại như bình thường. Khi loading xong, app được bật lần đầu, nên lần kiểm 2 giây vẫn chạy một lần. Có mạng thì không hiện gì. Loading xong mà không có mạng (ví dụ cấu hình từ xa không tải được và loading cho đi tiếp) thì hộp thoại hiện ở 2 giây thay vì 3 giây của KB-1. Tách được trường hợp này thì phải biết màn được khôi phục trước khung hình đầu: phải sửa `AppNavHost`, mà chạy thử cho thấy khung hình đầu chưa có màn. Đề xuất của AI: chấp nhận, vì hộp thoại đằng nào cũng hiện, chỉ sớm hơn 1 giây; không sửa `AppNavHost`.
2. **Hộp thoại đang hiện bị ẩn khi activity dựng lại trong cùng tiến trình** (ghi chú của rà soát độc lập, có từ trước T-078). Mọi lần dựng lại activity đều gọi `setActive(false)` rồi `setActive(true)`: xoay trên máy màn lớn, gập máy, đổi sáng tối, đổi ngôn ngữ. `disarm()` khi đó ẩn hộp thoại đang hiện (log `Dialog hidden: inactive`), và hộp thoại chỉ hiện lại sau 3 giây. Plan không đổi điều này, vì đó là `AppNavHost` báo khung hình đầu. Đề xuất của AI: ticket riêng.
3. **Duyệt chéo.** `scripts/plan-check.py` qua; điểm chặn 7 nên cần dev còn lại duyệt chéo. Đề xuất của AI: Nhật duyệt, Tuấn duyệt chéo như T-077.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ.
Đường làm nhanh (ADR-0034): tiêu đề "Làm nhanh YYYY-MM-DD, phiên bản plan N", ghi trong lần đẩy cuối của PR, sau rà soát độc lập và trước CI; Kết quả ghi "merge bằng PR này", Kết quả CI ghi "CI của PR này", bỏ dòng Cần người xem sáng nay, ghi việc cần người kiểm. -->
