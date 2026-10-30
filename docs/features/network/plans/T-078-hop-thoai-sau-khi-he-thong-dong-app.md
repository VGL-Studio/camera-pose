---
ticket: T-078
title: Hộp thoại mất mạng hiện ngay khi mở lại app sau khi hệ thống đóng app ở nền
status: done
version: 1
risk_score:
risk_level: high
token_estimate: 7.0M         # cỡ S, trung vị của scripts/token-usage.py estimate
blocking_points: [core]
run_order:
reviewer: Nhật
cross_reviewer: Tuấn
approved_at: 2026-10-30
lane:
---

# PLAN T-078: HỘP THOẠI MẤT MẠNG HIỆN NGAY KHI MỞ LẠI APP SAU KHI HỆ THỐNG ĐÓNG APP Ở NỀN

## 1. Tóm tắt

Plan sửa hai lỗi, cả hai xảy ra khi activity được dựng lại.

- **Lỗi của ticket:** hệ thống đóng app ở nền, người dùng mở lại từ màn đa nhiệm khi không có mạng. Hộp thoại hiện sau 3 giây, mà KB-3 đòi hiện sau tối đa 2 giây kiểm lại.
- **Lỗi gộp vào (Nhật quyết 2026-10-30, câu 2 của bản nháp đầu):** activity dựng lại trong cùng tiến trình, ví dụ khi xoay trên máy màn lớn, gập máy, đổi sáng tối hay đổi ngôn ngữ. Hộp thoại đang hiện thì bị ẩn, rồi 3 giây sau mới hiện lại.

Cách sửa:

- **`:app`, `AppNavHost`:** chỉ báo "đã qua loading hay chưa" khi back stack đã có màn. Lỗi thứ hai đến từ khung hình đầu của activity mới, lúc back stack chưa có màn mà vẫn báo "chưa qua loading". Phần báo được tách thành composable `ReportPastLoading`, để test dựng được đúng code thật.
- **`:core:network`, `NetworkDialogController`:** thêm hàm `onAppRestored()`, mà `MainActivity.onCreate` gọi khi activity được dựng lại từ trạng thái đã lưu.
  - Trong tiến trình này chưa có activity nào bắt đầu: đây là lần mở lại sau khi hệ thống đóng app. Controller ghi nhớ điều đó và chạy lần kiểm 2 giây của T-077 khi màn khôi phục báo đã qua loading.
  - Màn khôi phục là loading: không kiểm. Hộp thoại đi đường 3 giây của KB-1.
- Chạy thử đã xác nhận điều ticket nêu: điều hướng khôi phục thẳng màn sau loading, không dựng lại loading.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/network/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-3, tiêu chí thứ hai (T-077) | Khi người dùng đưa app ra nền rồi quay lại một màn sau màn loading mà máy không có mạng thì hộp thoại hiện ngay khi app trở lại, không chờ thời gian chờ của KB-1: máy được kiểm lại tối đa 2 giây, có mạng trong khoảng đó thì không hiện. Lần mở app đầu tiên và xoay màn hình không tính là quay lại | Áp cả khi tiến trình đã bị hệ thống đóng lúc app ở nền, và người dùng mở lại từ màn đa nhiệm vào màn sau loading. "Lần mở app đầu tiên" là lần mở không có trạng thái đã lưu: mở mới, sau khi vuốt bỏ app hay sau khi cài |
| KB-1, hộp thoại "chỉ đóng bằng Thử lại" | Hộp thoại không tự đóng | Dựng lại activity trong cùng tiến trình không ẩn hộp thoại đang hiện. Đây là phần Nhật quyết gộp vào |
| KB-1, "chỉ có một hộp thoại" | Kể cả khi nhiều lần tải cùng thất bại | Lần kiểm khi khôi phục và đường "mất mạng quá 3 giây" cùng đi qua `show()`, nên chỉ có một hộp thoại và một dòng log `Dialog shown` |
| KB-4 | Cờ `feature_network_dialog_enabled` tắt thì không hiện | Lần kiểm khi khôi phục chỉ chạy khi controller `armed`. Cờ tắt lúc khôi phục thì bỏ lần kiểm, không để dành tới lúc cờ bật |

Thiết kế tham chiếu: không có ảnh mới; hộp thoại dùng nguyên `NetworkDialog` của `features/network/design/README.md`.

ADR liên quan: ADR-0024 (app luôn cần mạng, popup chung do `:app` chặn), ADR-0019 (log), ADR-0001, ADR-0029, ADR-0031.

## 3. Giải pháp

### 3.1. Cách làm

**Điều đã kiểm (mục 4.1):**

- `AppNavHost` dùng `rememberNavController()`, nên back stack được lưu cùng trạng thái của activity.
- Khi hệ thống đóng tiến trình rồi người dùng mở lại, activity mới nhận `savedInstanceState` và màn trên cùng được khôi phục. Màn loading đã bị bỏ khỏi back stack (`popUpTo<LoadingDestination> { inclusive = true }`), nên không chạy lại.
- Với code hiện tại, khung hình đầu của activity mới báo `pastLoading = false`, vì chưa có `currentBackStackEntry`; khung hình kế báo `true`. Chuyện này xảy ra ở mọi lần dựng lại activity, kể cả trong cùng tiến trình. `setActive(false)` khi đó gọi `disarm()`, nên hộp thoại đang hiện bị ẩn (log `Dialog hidden: inactive`) và lần kiểm của T-077 đang chạy bị hủy.
- `setActive(true)` tới sau `onStart`, nên `onAppStarted()` của T-077 không nhận ra lần mở lại sau khi tiến trình bị đóng. Controller là đối tượng mới của tiến trình mới (`@Singleton` của Hilt), `leftApp = false`.

**`:app`, `AppNavHost`:** tách phần báo thành composable mới trong cùng file, đặt trước `private fun MainScreen.destination()`:

```kotlin
/**
 * Reports whether the current destination of [navController] is past the loading screen, each time it changes. Nothing
 * is reported before the back stack has an entry: a recreated activity has none on its first frame, and reporting
 * "not past loading" then would hide the network dialog for one frame (plan T-078).
 */
@Composable
internal fun ReportPastLoading(navController: NavHostController, onPastLoadingChanged: (Boolean) -> Unit) {
    val entry by navController.currentBackStackEntryAsState()
    val pastLoading = entry?.destination?.let { !it.hasRoute<LoadingDestination>() }
    val currentOnPastLoadingChanged by rememberUpdatedState(onPastLoadingChanged)
    LaunchedEffect(pastLoading) { pastLoading?.let(currentOnPastLoadingChanged) }
}
```

- Trong `AppNavHost`, bỏ ba dòng `val entry`, `val pastLoading`, `val currentOnPastLoadingChanged` và dòng `LaunchedEffect(pastLoading) { ... }`. Ở chỗ của `LaunchedEffect`, gọi `ReportPastLoading(navController, onPastLoadingChanged)`.
- Thêm `import androidx.navigation.NavHostController`. Các import còn lại vẫn được dùng.
- Câu KDoc về `[onPastLoadingChanged]` của `AppNavHost` thêm: "Nothing is reported before the back stack has an entry ([ReportPastLoading])."
- Hành vi mới, đã chạy thử:
  - Lần mở mới: lần báo đầu là `false` (màn loading), như cũ. Controller bắt đầu với `active = false`, nên lần báo này không đổi gì.
  - Dựng lại trong cùng tiến trình, màn sau loading: chỉ báo `true`. Giá trị này đã là `true`, nên controller không thấy thay đổi: hộp thoại đang hiện vẫn hiện, lần kiểm đang chạy vẫn chạy.
  - Khôi phục sau khi tiến trình bị đóng: chỉ báo `true` (màn sau loading), hay chỉ báo `false` (màn loading).

**`:core:network`, `NetworkDialogController`** (chỉ thêm, không đổi đường cũ):

- Hai trường mới, `@Volatile`:
  - `startedOnce = false`: đã có một lần `onAppStarted()` trong tiến trình này.
  - `checkWhenActive = false`: một lần mở lại sau khi tiến trình bị đóng đang chờ màn khôi phục báo đã qua loading.
- `fun onAppRestored()`: đặt ngay sau `onAppStarted()`, với KDoc riêng, để KDoc có sẵn của `onAppStarted()` vẫn đi với nó (rà soát độc lập). Chạy trên luồng chính, `MainActivity.onCreate` gọi khi `savedInstanceState != null`. Thân hàm: `if (!startedOnce) checkWhenActive = true`.
  - Xoay màn hình, hay activity bị hủy ở nền khi tiến trình còn sống, cũng dựng lại activity với `savedInstanceState`. Lúc đó `startedOnce` đã là `true`, nên lệnh gọi bị bỏ qua.
  - Trường hợp activity bị hủy ở nền đã được `onAppStarted()` của T-077 lo, vì `leftApp` là `true`. Bản nháp đầu cần thêm vế `leftApp` cho trường hợp này, vì khung hình đầu làm `disarm()` hủy lần kiểm. Sửa ở `AppNavHost` bỏ được nguyên nhân đó, nên không cần vế này nữa.
- `onAppStarted()`: thêm `startedOnce = true` ở dòng đầu, trước `if (!leftApp) return`.
- `setActive(active)`: thêm `if (!active) checkWhenActive = false` ở dòng đầu. Màn khôi phục là loading thì bỏ lần kiểm; hộp thoại đi đường 3 giây của KB-1. Bản nháp đầu không làm được điều này, vì khung hình đầu của mọi lần khôi phục đều báo `false` (câu 1 cũ của mục 8).
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
  - Cờ được bỏ ở lần đầu app được bật, kể cả khi cờ tính năng đang tắt (KB-4). Như vậy không có lần kiểm nào để dành tới lúc cờ bật giữa chừng.
  - `checkOnForeground()` và `disarm()` dùng lại của T-077: `disarm()` hủy `foregroundJob`; `checkOnForeground()` kiểm lại `armed` trước khi hiện.
  - Cờ tính năng ở lần khôi phục là giá trị Remote Config đã kích hoạt từ trước. Cờ vừa bật trên máy chủ mà chưa tới máy thì lần kiểm bị bỏ, như mọi lần cờ tắt. Bản staging bật cờ trên máy (`StagingFeatureFlagsModule`), nên không gặp.
  - Không thêm lý do log mới: dòng `Dialog shown: offline_on_foreground` giống lần quay lại thường, vì với người dùng đây cũng là một lần quay lại.
- KDoc của lớp, câu về quay lại app, thêm: "A return after the system closed the app in the background ([onAppRestored]) is checked the same way once the restored screen is past loading."
- KDoc của `onAppRestored`: "Called by :app from onCreate when the activity is restored from saved state. Main thread. Before any start in this process, this is a return after the system closed the app in the background: the network is checked as in [onAppStarted] once the restored screen is past loading, and not when it is the loading screen (plan T-078). A recreation in the same process is ignored."
- Log (ADR-0019, thẻ `Network`): chỉ dòng có sẵn qua `show()`. Không thêm sự kiện phân tích.

**`:app`, `MainActivity.onCreate`:** ngay sau dòng `observeAppForeground(...)`, thêm:

```kotlin
// Reopened after the system closed the app in the background: checked like a return (spec network, KB-3; plan T-078).
if (savedInstanceState != null) networkDialog.onAppRestored()
```

**Cách làm khác đã cân nhắc:**

- Đặt `leftApp = true` trong `onAppRestored()`, rồi để `onAppStarted()` chạy lần kiểm: không được. Lúc `onStart`, controller chưa `armed` (mục 4.1), nên lần bắt đầu bị bỏ qua.
- Giữ hộp thoại trong controller, bỏ qua `setActive(false)` ngay sau một lần dựng lại: không được. Controller phải đoán khung hình nào là khung hình đầu, trong khi nguyên nhân nằm ở chỗ `AppNavHost` báo một giá trị chưa có.

**Test `NavRestoreTest`** (tệp mới): test dựng đúng `ReportPastLoading` của `AppNavHost` trong một `NavHost` nhỏ, có loading bị bỏ `inclusive` như `AppNavHost`. Dựng thẳng `AppNavHost` thì cần Hilt cho màn loading, mà `:app` chưa có khung test cho Hilt. Đã chạy thử: test đỏ với logic cũ, qua với code mới (4.1). Nội dung để phiên chép nguyên:

```kotlin
package com.vglstudio.camera.pose.line.guide.navigation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.feature.loading.navigation.LoadingDestination
import com.vglstudio.camera.pose.line.guide.feature.settings.navigation.SettingsDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ReportPastLoading of AppNavHost when the activity is restored from saved state (plan T-078): the back stack of
 * rememberNavController comes back without composing loading again, and only the restored screen is reported, never a
 * "not past loading" first frame that would hide the network dialog. The host follows AppNavHost (loading popped
 * inclusive); AppNavHost itself needs Hilt for the loading screen, which :app has no test setup for.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class NavRestoreTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()

    private fun StateRestorationTester.setHost(loadingFinishes: Boolean) = setContent {
        val navController = rememberNavController()
        ReportPastLoading(navController) { events += "pastLoading=$it" }
        NavHost(navController, startDestination = LoadingDestination) {
            composable<LoadingDestination> {
                Text("loading")
                LaunchedEffect(Unit) {
                    events += "loading"
                    if (loadingFinishes) {
                        navController.navigate(SettingsDestination) {
                            popUpTo<LoadingDestination> { inclusive = true }
                        }
                    }
                }
            }
            composable<SettingsDestination> { Text("main") }
        }
    }

    @Test
    fun restoredPastLoading_skipsLoading_reportsOnlyPastLoading() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setHost(loadingFinishes = true)
        composeRule.waitForIdle()
        // Control, the same with or without the fix: the first launch goes through loading, then past it.
        assertTrue("loading" in events)
        assertEquals("pastLoading=true", events.last())

        events.clear()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertEquals(listOf("pastLoading=true"), events)
    }

    @Test
    fun restoredOnLoading_reportsNotPastLoading() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setHost(loadingFinishes = false)
        composeRule.waitForIdle()
        events.clear()

        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertEquals(listOf("loading", "pastLoading=false"), events)
    }
}
```

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/AppNavHost.kt` | Tách `ReportPastLoading`, không báo khi chưa có màn, KDoc |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogController.kt` | `onAppRestored()`, `startedOnce`, `checkWhenActive`, lần kiểm khi được bật trong `init`, dòng mới trong `onAppStarted()` và `setActive()`, KDoc |
| Sửa | `core/network/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogControllerTest.kt` | Các test mới của mục 4 và hai hàm phụ `restoredProcess()`, `recreate()`, thêm vào cuối lớp, trước `private companion object`; không sửa test cũ |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | Gọi `networkDialog.onAppRestored()` trong `onCreate` khi `savedInstanceState != null` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/navigation/NavRestoreTest.kt` | Test Robolectric khôi phục điều hướng, nội dung ở mục 3.1 |
| Sửa | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/network/MainActivityForegroundWiringTest.kt` | Thêm test `mainActivity_reportsRestore`, dùng lại `codeOnly`, `bodyOf`, `ON_CREATE` của lớp; không sửa test cũ |

`scripts/tests/run.sh` có nhắc `MainActivity.kt`, ở dòng đếm `exposeTestTagsForUiAutomator()` bằng 1. Dòng này không bị ảnh hưởng, vì plan chỉ thêm một dòng `if` (L-020). Không đổi file khác mà nơi khác đọc: không thêm khóa Remote Config, ảnh chuẩn hay tài nguyên chuỗi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: dùng `feature_network_dialog_enabled` có sẵn qua `armed`; không thêm cờ.
- Quảng cáo: không chạm. Lần kiểm đi qua `show()`, nên vẫn chờ quảng cáo toàn màn hình đóng như mục 5 của spec (`setFullScreenAdShowing`, T-074 lượt 2).
- Sự kiện phân tích: không chạm.
- File R2: không chạm.
- **Quyền truy cập:** Không thêm. `savedInstanceState` là tham số có sẵn của `onCreate`; kiểm mạng dùng `ACCESS_NETWORK_STATE` có sẵn của `:core:network` (T-011).
- **Thư viện và SDK:** Không thêm. `NavHostController` có sẵn trong `navigation-compose` mà `:app` đang dùng.

### 3.4. Phương án dự phòng

Không có. Thứ tự khôi phục, lúc `setActive(true)` tới và các giá trị `ReportPastLoading` báo đều đã chạy thử ở 4.1.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (2026-10-30): 84 test dùng tên của các file plan sửa; "Cần xem trước" 0; "Nên xem" 23. Đã đọc từng test:

- `MergedManifestTest.mergedManifest_hasNoAdGalleryActivity`, `ScreenOrientationTest.manifest_keepsPortrait_soPhonesStartWithoutARelaunch`: đọc manifest của `MainActivity`. Plan không đổi manifest.
- `MainActivityForegroundWiringTest.mainActivity_observesAppForeground`: đếm đúng một lời gọi `observeAppForeground(...)` trong `onCreate`. Dòng `if` mới không khớp mẫu đó, nên vẫn đúng một.
- `NativeFullScreenOverlayTest.recreate_keepsDeadline_notRestarted`, `PhotoStoreTest.save_insertThrowsIllegalArgument_isIOException`, `LoadingRouteTest.backgroundWhileWaiting_thenResumed_showsOnce`: chỉ trùng chữ `onCreate` hay `MainActivity`, không dùng controller hay `AppNavHost`.
- `AndroidConnectivitySourceTest` (3 test), `MlKitPoseAnalyzerTest` (6 test), `PoseMatchViewModelTest.cleared_releasesWithItself`: gọi `setActive` của lớp khác, không phải của controller.
- `NetworkDialogControllerTest.activated_whileOffline_showsAfterDelay`, `deactivatedDuringGrace_dropsShow`, `deactivated_hidesDialog_andStopsWatching`, `inactive_ignoresOfflineAndFailures`: gọi `setActive` của controller. Dòng mới của `setActive(false)` chỉ bỏ `checkWhenActive`, mà các test này không bật (không gọi `onAppRestored`), nên kết quả không đổi.
- `NetworkDialogControllerTest.adCloses_withoutLoss_staysHidden`, `adStartsWhileDialogShown_hidesThenShowsAgainOnClose`, `lostWhileAdShowing_dialogWaits_thenShowsWhenAdCloses`, `returnOffline_whileAdShowing_waitsForAd`: đường chờ quảng cáo không đổi.
- `NetworkDialogControllerTest.inactiveWhileWaiting_dropsPending`: gọi `onAppStarted`. Dòng `startedOnce = true` mới không đổi kết quả, vì test không gọi `onAppRestored`.
- Không test nào của `:app` khẳng định `onPastLoadingChanged` hay lần báo của khung hình đầu.

Không câu khẳng định nào bị đổi. Chạy thử ở 4.1 với code của mục 3.1: cả 39 test cũ của `NetworkDialogControllerTest` qua, và toàn bộ test của `:app` qua.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Mở lại sau khi tiến trình bị đóng, không có mạng: hộp thoại hiện sau 3 giây của KB-1 | Hiện sau tối đa 2 giây kiểm lại; màn khôi phục là loading thì vẫn 3 giây | Không có. `firstStart_isNotAReturn` của T-077 không gọi `onAppRestored` | Giữ nguyên; test mới ở mục 4 |
| Dựng lại activity: khung hình đầu báo `setActive(false)`, hộp thoại đang hiện bị ẩn | Không báo khi back stack chưa có màn; hộp thoại vẫn hiện | Không có test khẳng định lần báo `false` của khung hình đầu | Giữ nguyên; `NavRestoreTest` và `recreatedInSameProcess_dialogStays` |
| `onAppStarted()` chỉ đọc `leftApp`; `setActive()` chỉ ghi `active` | Ghi thêm `startedOnce = true`; `setActive(false)` bỏ `checkWhenActive` | `NetworkDialogControllerTest` của T-077 (15 test quay lại), `inactiveWhileWaiting_dropsPending` | Giữ nguyên |
| `MainActivity.onCreate` không đọc `savedInstanceState` cho mạng | Gọi `onAppRestored()` khi có trạng thái đã lưu | `MainActivityForegroundWiringTest.mainActivity_observesAppForeground` | Giữ nguyên |

## 4. Kiểm thử

Unit test thêm vào cuối `NetworkDialogControllerTest`, chạy trên thời gian ảo. Test dùng các hàm phụ có sẵn `controller()`, `backToApp()`, `toBackground()`, `infoMessages()`, `shownLines()` và `RecordingAppLogger`. Hai hàm phụ mới dựng lại thứ tự thật đã đo ở 4.1, sau khi đã sửa `AppNavHost`:

```kotlin
/** A new process restored past loading: onCreate with saved state, onStart, then the restored screen. */
private fun TestScope.restoredProcess(): NetworkDialogController {
    val controller = controller(active = false)
    controller.onAppRestored()
    backToApp(controller)
    controller.setActive(true)
    runCurrent()
    return controller
}

/** The activity recreated in the same process for a configuration change; the screen stays past loading. */
private fun TestScope.recreate(controller: NetworkDialogController) {
    controller.onAppStopped(changingConfigurations = true)
    controller.onAppRestored()
    backToApp(controller)
    controller.setActive(true)
    runCurrent()
}
```

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-3: mở lại sau khi tiến trình bị đóng, không có mạng, thì hiện sau tối đa 2 giây, trước 3 giây của KB-1 | Unit test | `restoredOffline_showsAfterGrace_beforeLostDelay`: offline; `restoredProcess()`; `advanceTimeBy(1_999)` → `HIDDEN`; `advanceTimeBy(2)` → `SHOWN`; `shownLines()` bằng `["Dialog shown: offline_on_foreground"]` |
| KB-3: có mạng thì không hiện | Unit test | `restoredOnline_showsNothing`: `restoredProcess()`; `advanceTimeBy(5_000)` → `HIDDEN`, `shownLines()` rỗng |
| KB-3: mạng có lại trong lúc kiểm thì không hiện | Unit test | `restoredNetworkBackWithinGrace_showsNothing`: offline; `restoredProcess()`; `advanceTimeBy(500)`; có mạng; `advanceTimeBy(5_000)` → `HIDDEN`. Đối chứng: `restoredOffline_showsAfterGrace_beforeLostDelay` |
| Lần mở mới (không có trạng thái đã lưu) không tính | Unit test | `freshStart_isNotARestore`: offline; `controller(active = false)`; `backToApp`; `setActive(true)`; `advanceTimeBy(2_500)` → `HIDDEN`; `advanceTimeBy(501)` → `shownLines()` bằng `["Dialog shown: device_offline delay=3s"]` (đường 3 giây, đối chứng) |
| Khôi phục vào màn loading không tính | Unit test | `restoredOnLoading_isNotAReturn`: `controller(active = false)`; `onAppRestored()`; `backToApp`; `setActive(false)` (màn khôi phục là loading); offline; `setActive(true)` (loading xong); `advanceTimeBy(2_500)` → `HIDDEN`; `advanceTimeBy(501)` → `["Dialog shown: device_offline delay=3s"]`. Bỏ dòng mới của `setActive` thì đỏ (4.1) |
| Xoay màn hình (dựng lại trong cùng tiến trình) không tính, kể cả khi cờ bật sau đó | Unit test | `recreatedInSameProcess_isNotARestore_evenWhenFlagTurnsOnLater`: `controller()`; `backToApp`; tắt cờ; `recreate()`; offline; bật cờ; `advanceTimeBy(2_500)` → `HIDDEN`; `advanceTimeBy(501)` → `["Dialog shown: device_offline delay=3s"]`. Bỏ điều kiện của `onAppRestored()`, hay bỏ `startedOnce = true`, thì đỏ (4.1) |
| KB-1: dựng lại trong cùng tiến trình không ẩn hộp thoại đang hiện | Unit test | `recreatedInSameProcess_dialogStays`: `controller()`; `backToApp`; offline; `advanceTimeBy(3_001)` → `SHOWN`; `recreate()` → vẫn `SHOWN`, không có dòng log bắt đầu bằng `Dialog hidden`. Test qua cả trên code hiện tại. Phần sửa thật nằm ở `AppNavHost`, và chỉ `NavRestoreTest` kiểm; test này giữ cho các lời gọi của `MainActivity` khi dựng lại không ẩn hộp thoại |
| Activity bị hủy ở nền, tiến trình còn sống | Unit test | `activityDestroyedInBackground_isAReturn`: `controller()`; `backToApp`; `toBackground()`; `onAppRestored()`; offline; `backToApp`; `setActive(true)`; `advanceTimeBy(1_999)` → `HIDDEN`; `advanceTimeBy(2)` → `["Dialog shown: offline_on_foreground"]` Đây là đường T-077; test giữ cho `onAppRestored()` không làm hỏng nó. Test không dựng lại lần báo `false` của khung hình đầu: chỗ đó chỉ `NavRestoreTest` kiểm |
| Xoay màn hình giữa lúc kiểm khi khôi phục | Unit test | `restoredRecreatedDuringGrace_showsOnce`: offline; `restoredProcess()`; `advanceTimeBy(500)`; `recreate()`; `advanceTimeBy(1_501)` → `SHOWN`; `advanceTimeBy(5_000)` → `shownLines()` bằng đúng `["Dialog shown: offline_on_foreground"]`. Giữ cho lần dựng lại không hủy lần kiểm đang chạy (rà soát độc lập) |
| KB-4: cờ tắt lúc khôi phục thì không kiểm, kể cả khi cờ bật sau | Unit test | `restoredWithFlagOff_ignored_evenWhenFlagTurnsOnLater`: tắt cờ; offline; `restoredProcess()`; `advanceTimeBy(5_000)` → `HIDDEN`; bật cờ; `advanceTimeBy(2_500)` → `HIDDEN`; `advanceTimeBy(501)` → `["Dialog shown: device_offline delay=3s"]`. Bỏ `if (active) checkWhenActive = false` thì đỏ (4.1) |
| Về loading giữa lúc kiểm (L-061) | Unit test | `restoredDeactivatedDuringGrace_dropsShow`: offline; `restoredProcess()`; `advanceTimeBy(500)`; `setActive(false)`; `advanceTimeBy(5_000)` → `HIDDEN`, `infoMessages()` không có dòng chứa `offline_on_foreground` |
| KB-1: chỉ một hộp thoại khi hai đường cùng hiện | Unit test | `restoredOffline_andLostDelay_singleDialog`: offline; `restoredProcess()`; `advanceTimeBy(5_001)` → `SHOWN`, `shownLines()` bằng đúng `["Dialog shown: offline_on_foreground"]` |
| Ra nền giữa lúc kiểm (L-091) | Unit test | `restoredBackgroundDuringGrace_dialogStays`: offline; `restoredProcess()`; `advanceTimeBy(500)`; `toBackground()`; `advanceTimeBy(1_600)` → `SHOWN` ở 2 100 ms |
| Khôi phục điều hướng: không dựng lại loading, chỉ báo màn khôi phục | Robolectric (`:app`) | `NavRestoreTest.restoredPastLoading_skipsLoading_reportsOnlyPastLoading`: lần mở đầu có `loading` và lần báo cuối là `pastLoading=true` (đối chứng, giống nhau dù có sửa hay không); sau `emulateSavedInstanceStateRestore()` cho đúng `[pastLoading=true]` |
| Khôi phục vào màn loading báo "chưa qua loading" | Robolectric (`:app`) | `NavRestoreTest.restoredOnLoading_reportsNotPastLoading`: loading không chuyển màn; sau khi khôi phục cho `[loading, pastLoading=false]`. Với logic cũ, test chỉ đỏ vì thứ tự (`[pastLoading=false, loading]`); phần kiểm chính là test trên |
| `MainActivity` báo lần khôi phục | Unit test quét mã nguồn (`:app`) | `MainActivityForegroundWiringTest.mainActivity_reportsRestore`: trên `codeOnly(...)` của `MainActivity.kt`, có đúng một lời gọi khớp `\bif\s*\(\s*savedInstanceState\s*!=\s*null\s*\)\s*networkDialog\.onAppRestored\(\s*\)`, nằm trong thân `override fun onCreate` (`bodyOf(code, ON_CREATE)`). Đối chứng: tệp có `class MainActivity`. Quét theo ký hiệu, không theo chuỗi con |
| Mở lại thật sau khi tiến trình bị đóng, và dựng lại khi hộp thoại đang hiện | Kiểm tra thủ công (máy thật: trình chạy kịch bản không có lệnh đóng tiến trình rồi mở lại từ màn đa nhiệm mà giữ trạng thái; `fresh` thì xóa dữ liệu) | Bản staging, cờ `feature_network_dialog_enabled` bật, máy nối USB, theo dõi `adb logcat -v threadtime \| grep ' VGL\.'`:<br>(1) Mở camera hay thư viện, bấm Home, chạy `adb shell am kill com.vglstudio.camera.pose.line.guide.staging` (đóng như khi thiếu bộ nhớ; chỉ được khi app ở nền), tắt Wi-Fi và dữ liệu, mở lại app từ màn đa nhiệm. Màn đang xem hiện lại, không qua loading, và hộp thoại hiện trong khoảng 2 giây, có dòng `Dialog shown: offline_on_foreground`.<br>(2) Như (1) nhưng giữ mạng: không có hộp thoại.<br>(3) Vuốt bỏ app khỏi màn đa nhiệm, rồi mở lại khi không có mạng: app mở từ loading như lần mở mới, không có dòng `offline_on_foreground`.<br>(4) Không có mạng, chờ hộp thoại hiện, rồi đổi sáng tối trong cài đặt nhanh của máy (hay xoay, trên máy màn lớn). Hộp thoại vẫn hiện, không có dòng `Dialog hidden: inactive` |

Test viết trước: mọi test ở trên thất bại trên code hiện tại, vì chưa có `onAppRestored` và `ReportPastLoading` (lỗi biên dịch). Để thất bại đúng lý do, bước 3 thêm khung trong cùng commit test:

- `fun onAppRestored() {}` rỗng trong `NetworkDialogController`;
- `ReportPastLoading` với logic cũ (`?: false`, báo cả khi chưa có màn), và `AppNavHost` gọi nó.

Trên khung đó (đã chạy thử, 4.1):

- 4 test controller đỏ:
  - `restoredOffline_showsAfterGrace_beforeLostDelay`: `Hidden` ở 2 001 ms;
  - `restoredRecreatedDuringGrace_showsOnce`: `Hidden` ở 2 001 ms;
  - `restoredOffline_andLostDelay_singleDialog`: lý do là `device_offline delay=3s`;
  - `restoredBackgroundDuringGrace_dialogStays`: `Hidden` ở 2 100 ms.
- Cả 2 test của `NavRestoreTest` đỏ. Test thứ nhất đỏ ở bước khôi phục: `[pastLoading=false, pastLoading=true]` thay vì `[pastLoading=true]`. Test thứ hai đỏ vì thứ tự.
- `mainActivity_reportsRestore` đỏ, vì chưa có lời gọi.

Các test "không hiện" và "vẫn hiện" qua ngay trên khung. Mỗi test có đối chứng: một test "hiện" cùng cách dựng, hay bước đối chứng ghi trong chính test (L-035). Đột biến ở 4.1 cho thấy mỗi dòng mới của controller có một test đỏ khi bỏ dòng đó.

Dòng `if` trong `MainActivity.onCreate` có `mainActivity_reportsRestore` (quét mã nguồn), vì `MainActivity` cần Hilt và `:app` chưa có khung test dựng nó (như T-077). Kiểm tra thủ công (1) và (4) xác nhận phần này chạy trên máy.

### 4.1. Chạy thử

- **Lần chạy:** 2026-10-30, trên `main` `95b7697`, nhánh `claude/plan-T-078-mo-lai-sau-khi-dong`. Code và test tạm đã hoàn tác, không commit. Lệnh chạy sau `source ~/.local/share/camera-pose/env.sh`.
- **Thứ tự khôi phục (bản nháp đầu):**
  - Test tạm `DryRunRestoreTest`, Robolectric, `@Config(application = Application::class)`, dựng một `NavHost` nhỏ theo đúng cách của `AppNavHost`, rồi `StateRestorationTester.emulateSavedInstanceStateRestore()`.
  - Đích phải là lớp `@Serializable` của module feature: lớp khai báo trong source set test của `:app` báo `Serializer ... is not found`.
  - Kết quả: `past=false | loadingComposed | past=true | cameraComposed | --restore | past=false | cameraComposed | past=true`. Vậy khi khôi phục, màn loading không dựng lại, nhưng khung hình đầu báo `false` trước `true`.
- **`ReportPastLoading`:** `./gradlew :app:testStagingDebugUnitTest --tests '*NavRestoreTest*' --rerun`.
  - Code mới: 2 test qua. Khi khôi phục chỉ có `pastLoading=true`; khi khôi phục vào loading chỉ có `pastLoading=false`.
  - Lần mở mới trong host của test không còn báo `false` trước `loading`, vì loading của test chuyển màn ngay trong khung hình nó được dựng. Bản đầu của test lấy thứ tự này làm đối chứng, nên với logic cũ nó đỏ ở bước đối chứng, không ở bước khôi phục (rà soát độc lập). Đối chứng giờ giống nhau dù có sửa hay không.
  - Logic cũ (`?: false`, báo cả khi chưa có màn): cả 2 test đỏ. Test thứ nhất đỏ ở bước khôi phục (`[pastLoading=false, pastLoading=true]`).
- **Controller:** thêm tạm 13 test controller của mục 4, chạy `./gradlew :core:network:testDebugUnitTest --rerun`.
  - Code đủ của mục 3.1: 52 test, 0 đỏ, gồm 39 test cũ.
  - Khung (`onAppRestored()` rỗng, không có dòng mới trong `setActive`): 4 đỏ đúng lý do, như mục 4.
  - Đột biến, mỗi lần một dòng, đúng một test đỏ:
    - `checkWhenActive = true` không điều kiện → `recreatedInSameProcess_isNotARestore_evenWhenFlagTurnsOnLater`;
    - bỏ `startedOnce = true` → cùng test đó;
    - bỏ `if (!active) checkWhenActive = false` → `restoredOnLoading_isNotAReturn`;
    - bỏ `if (active) checkWhenActive = false` → `restoredWithFlagOff_ignored_evenWhenFlagTurnsOnLater`.
  - Bản đầu của test xoay màn hình bật tắt cờ sau khi dựng lại, và không bắt được đột biến thứ nhất: lần tắt cờ đã bỏ `checkWhenActive`. Test giờ dựng lại khi cờ đang tắt rồi mới bật cờ.
- **`:app`:** `./gradlew :app:testStagingDebugUnitTest --rerun` với code đủ: 99 test, 0 đỏ.
- **Rà soát độc lập (bản sửa):** không có điểm chặn. Đã sửa hai điểm nên sửa: đối chứng của `NavRestoreTest`, và chỗ đặt `onAppRestored()` cùng KDoc. Đã thêm test `restoredRecreatedDuringGrace_showsOnce`, và ghi rõ test nào không bắt được lỗi khung hình đầu. Rà soát cũng xác nhận lại bốn đột biến, và xác nhận cờ tính năng trên bản production đọc giá trị đã kích hoạt ngay lần đầu (`FirebaseRemoteFlagSource`).
- **Không chạy `scripts/check.sh`:** tiêu chí E dưới 2 (mục 5), không có kỹ thuật hay thư viện mới. Chạy thử chỉ để chắc thứ tự khôi phục, giá trị được báo và thời gian của test.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7, module core dùng chung (`:core:network`). Mức Cao, cần duyệt chéo. Bảng dưới chỉ để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Hai module (`:core:network`, `:app`). Thêm một hàm công khai ở controller, một composable `internal` ở `AppNavHost`, một dòng ở `MainActivity` và một test Robolectric |
| B. Độ rõ của yêu cầu | 1 | KB-3 đã `confirmed`, ticket nêu rõ trường hợp; Nhật đã trả lời các câu hỏi của bản nháp đầu (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Logic có unit test trên thời gian ảo, phần báo của `AppNavHost` có test Robolectric. Tiến trình bị đóng thật và màn đa nhiệm chỉ kiểm được trên máy (mục 4) |
| D. Ảnh hưởng chéo | 1 | `AppNavHost` báo cho controller ít hơn một lần (khung hình đầu); dùng lại `checkOnForeground`, `foregroundJob`, `disarm` của T-077; `NetworkStatus`, `NetworkFailureReporter` không đổi |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của T-077; `StateRestorationTester` đã dùng ở `:core:ads` |
| **Tổng** | 7 | **Mức: Cao** (điểm chặn 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `NetworkDialogController`: `MainActivity` và `NetworkDialogHost` dùng nó. `NetworkStatus` và `NetworkFailureReporter` mà feature dùng không đổi.
  - `AppNavHost`: chỉ phần báo "đã qua loading" đổi; điều hướng không đổi. `onPastLoadingChanged` chỉ có một nơi nhận, là `networkDialog::setActive`.
  - `MainActivity.onCreate`: thêm một dòng.
  - `MainActivityForegroundWiringTest`: thêm một test.
- Task khác có thể xung đột: không có plan nào khác đang chờ. T-076 và T-077 đã merge (`e9f5a5b`, `5d85b6b`). Plan này dựa vào `onAppStarted`, `checkOnForeground`, `foregroundJob` và `FOREGROUND_GRACE_MILLIS` của T-077.
- Đề xuất thứ tự chạy: không có ràng buộc, vì T-077, ticket mà T-078 phụ thuộc, đã merge.

## 7. Điều kiện dừng riêng

- Test cũ của `NetworkDialogControllerTest` hay của `:app` đỏ sau khi thêm code của mục 3.1: dừng theo điều kiện 3, vì plan nói đường cũ không đổi.
- Cần sửa điều hướng của `AppNavHost` (không chỉ phần báo) hay `:feature:loading`: dừng, vì đó là đổi hành vi của spec loading, ngoài ticket.

## 8. Câu hỏi cho người duyệt

Nhật đã trả lời ba câu của bản nháp đầu (2026-10-30):

1. **Khôi phục vào màn loading:** Nhật chấp nhận lần kiểm chạy một lần sau loading. Sửa ở `AppNavHost` cho câu 2 làm màn khôi phục báo đúng ngay lần đầu, nên plan thêm một dòng ở `setActive` để trường hợp này đi đúng đường 3 giây của KB-1, có test `restoredOnLoading_isNotAReturn`. Người duyệt muốn giữ cách đã chấp nhận thì bỏ dòng này và test của nó.
2. **Hộp thoại bị ẩn khi activity dựng lại trong cùng tiến trình:** Nhật quyết gộp vào T-078. Plan sửa ở `AppNavHost` (`ReportPastLoading`), test ở `NavRestoreTest` và `recreatedInSameProcess_dialogStays`, kiểm thủ công (4).
3. **Duyệt chéo:** Nhật duyệt, Tuấn duyệt chéo.

Không còn câu hỏi mở.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt, 2026-10-30, bản đã sửa theo các câu trả lời của bản nháp đầu.
- **Người duyệt:** Nhật, 2026-10-30, **duyệt chéo:** Tuấn, 2026-10-30 (AI ghi theo lựa chọn của Nhật trong phiên: "Cả hai duyệt", với mô tả AI đưa ra là "Nhật duyệt, Tuấn duyệt chéo bản đã sửa"; trước đó Nhật chọn "Nhật + Tuấn duyệt luôn" cho bản nháp đầu).
- **Ghi chú:** Nhật trả lời ngày 2026-10-30, trong phiên soạn plan.
  - Bản nháp đầu, câu 1 (khôi phục vào màn loading): chấp nhận. Sau khi sửa câu 2, Nhật chọn "Giữ" dòng `if (!active) checkWhenActive = false`, để trường hợp này đi đúng đường 3 giây của KB-1.
  - Bản nháp đầu, câu 2 (hộp thoại bị ẩn khi dựng lại activity): "Gộp vào T-078". Plan sửa ở `AppNavHost` (`ReportPastLoading`); ticket ghi thêm phạm vi.
  - Bản nháp đầu, câu 3 (duyệt chéo): Nhật duyệt, Tuấn duyệt chéo.
  - Bước tiếp: "Đẩy, mở PR, merge".

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ.
Đường làm nhanh (ADR-0034): tiêu đề "Làm nhanh YYYY-MM-DD, phiên bản plan N", ghi trong lần đẩy cuối của PR, sau rà soát độc lập và trước CI; Kết quả ghi "merge bằng PR này", Kết quả CI ghi "CI của PR này", bỏ dòng Cần người xem sáng nay, ghi việc cần người kiểm. -->

### RUN-20261030-02, phiên bản plan 1

- **Kết quả:** đã merge (PR #246, commit `990dd59`, 2026-10-30 16:05 UTC)
- **Nhánh, PR:** `claude/T-078-mo-lai-sau-khi-dong`, https://github.com/VGL-Studio/camera-pose/pull/246
- **Đã làm:** đúng mục 3.1. `ReportPastLoading` tách khỏi `AppNavHost`, không báo khi back stack chưa có màn; `NetworkDialogController` thêm `startedOnce`, `checkWhenActive`, `onAppRestored()`, dòng mới trong `onAppStarted()`, `setActive()` và collector của `init`; `MainActivity.onCreate` gọi `onAppRestored()` khi có `savedInstanceState`. 6 file, đúng mục 3.2.
- **Test viết trước:** 16 test (13 test controller, 2 test `NavRestoreTest`, `mainActivity_reportsRestore`), commit `94b4555` cùng khung rỗng (`onAppRestored()` rỗng, `ReportPastLoading` với logic cũ). Trên khung: 7 test đỏ đúng lý do của mục 4 (`restoredOffline_showsAfterGrace_beforeLostDelay`, `restoredRecreatedDuringGrace_showsOnce`, `restoredBackgroundDuringGrace_dialogStays` còn `Hidden`; `restoredOffline_andLostDelay_singleDialog` ra `device_offline delay=3s`; `NavRestoreTest` thứ nhất ra `[pastLoading=false, pastLoading=true]`, thứ hai ra `[pastLoading=false, loading]`; `mainActivity_reportsRestore` 0 lời gọi). Test JVM của mọi module: không test nào ngoài mục 3.2 đỏ. Với code đủ, `:core:network` và `:app` 162 test, 0 lỗi. Đột biến của mục 4.1 chạy lại: mỗi đột biến làm đúng một test đỏ, như plan.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 3 ghi chú: (1) `foregroundJob` có hai nơi ghi (luồng chính và scope của controller), cặp `cancel`, `launch` không nguyên tử; vô hại vì `show()` chống trùng, có từ T-077; (2) tiến trình bị đóng khi một activity khác ở trên (`AdActivity`, intent hệ thống) thì lần khôi phục `MainActivity` cũng tính là quay lại, chấp nhận được; (3) lần kiểm khi khôi phục dựa vào giá trị đầu của luồng cờ, plan đã nêu.
- **Kết quả CI:** `pr-check` xanh (lần chạy 41084897305), một vòng. `main-guard` của `990dd59` xanh (lần chạy 41086632304).
- **Lệch so với plan:** Không có. Test `recreatedInSameProcess_isNotARestore_evenWhenFlagTurnsOnLater` gọi `runCurrent()` sau khi tắt cờ, trước `recreate()`, theo ghi chú của rà soát trước khi duyệt (RUN-20261030-02); không đổi câu khẳng định. Chú thích ở `MainActivity` tách hai dòng cho vừa 120 ký tự.
- **Việc còn lại:** Không có.
- **Cần người xem:** kiểm tra thủ công (1) tới (4) của mục 4 trên bản staging, máy thật nối USB.
