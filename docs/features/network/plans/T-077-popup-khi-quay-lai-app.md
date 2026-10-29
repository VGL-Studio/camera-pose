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

Thêm vào `NetworkDialogController` của `:core:network` hai hàm `onAppStarted()` và `onAppStopped(changingConfigurations)`, theo `onAppForeground()` của dự án mẫu (T-017 của mẫu) nhưng giữ controller riêng của app (T-011): khi app quay lại từ nền ở một màn sau màn loading mà máy không có mạng, hộp thoại hiện sau một khoảng kiểm lại ngắn (tối đa 2 giây, kiểm mỗi 250 ms), không chờ 3 giây của KB-1; lần mở đầu, xoay màn hình và màn loading không tính là quay lại. `:app` gọi hai hàm này qua một `DefaultLifecycleObserver` gắn vào `MainActivity`. Quảng cáo quay lại app (`resume`) hiện trước, hộp thoại chờ quảng cáo đóng như mục 5 của spec. Kiểm chứng bằng unit test trên thời gian ảo cho controller và test Robolectric cho observer.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/network/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-3 (tiêu chí mới, câu hỏi 1 của mục 8) | Khi người dùng đưa app ra nền rồi quay lại một màn sau màn loading mà máy không có mạng thì hộp thoại hiện ngay khi app trở lại, không chờ thời gian chờ của KB-1 | "Ngay" là sau tối đa 2 giây kiểm lại (câu hỏi 2): máy có mạng lại trong khoảng đó thì không hiện. "Quay lại" là `MainActivity` bắt đầu lại sau một lần dừng không do đổi cấu hình (câu hỏi 3). Lần mở đầu và màn loading không tính (loading đã tự kiểm mạng, spec `loading`) |
| KB-3, tiêu chí có sẵn | Mở lại app thì nếu vẫn chưa có mạng, hộp thoại hiện lại | Hộp thoại đã hiện trước khi ra nền thì vẫn hiện (controller không đổi trạng thái khi ra nền); lần kiểm khi quay lại không mở thêm hộp thoại thứ hai |
| KB-1, "chỉ có một hộp thoại" | Kể cả khi nhiều lần tải cùng thất bại | Lần kiểm khi quay lại và đường "mất mạng quá 3 giây" cùng đi qua `show()`; `showNow()` chỉ đổi khi đang `Hidden`, nên chỉ một hộp thoại và một dòng log `Dialog shown` |
| KB-4 | Cờ `feature_network_dialog_enabled` tắt thì không hiện | Lần kiểm khi quay lại chỉ chạy khi controller đang `armed` (qua loading, cờ bật); cờ tắt giữa lúc kiểm lại thì bỏ lần hiện |
| Mục 5 của spec | Quảng cáo toàn màn hình đang hiện thì hộp thoại chờ quảng cáo đóng | Quảng cáo `resume` được quyết ở `onActivityStarted` (trước observer, mục 4.1) và hiện ở lần `onResume` kế tiếp; trong 2 giây kiểm lại, cờ `fullScreenAdShowing` đã tới controller, nên hộp thoại không lóe lên trước quảng cáo mà hiện sau khi quảng cáo đóng (câu hỏi 4) |

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
- `private suspend fun checkOnForeground()`: kiểm `connectivity.isConnected()` ngay, rồi mỗi `NetworkSettings.RETRY_POLL_MILLIS` (250 ms) cho tới `NetworkSettings.FOREGROUND_GRACE_MILLIS` (2 000 ms). Có mạng ở một lần kiểm: dừng, không ghi log. Hết khoảng mà vẫn không có mạng: `if (armed.value) show(FOREGROUND_REASON)` với `FOREGROUND_REASON = "offline_on_foreground"`. Vòng lặp viết giống `retry()` (cùng kiểu `waited`), để đọc một kiểu.
- `disarm()`: thêm `foregroundJob?.cancel()` cạnh `retryJob?.cancel()`, để màn loading hay cờ tắt giữa lúc kiểm lại không hiện hộp thoại.
- Ra nền giữa lúc kiểm lại: không hủy job. Job hiện hộp thoại (nếu vẫn không có mạng) khi app đang ở nền, giống đường "mất mạng quá 3 giây" hiện nay vẫn chạy khi app ở nền; quay lại thì hộp thoại đã có (KB-3, tiêu chí có sẵn), và lần `onAppStarted()` mới thay job cũ. Test `backgroundDuringGrace_dialogStaysOnReturn` (mục 4) giữ điều này.
- KDoc của lớp thêm một câu: "It also shows when the app comes back from the background without network, after a check of at most [NetworkSettings.FOREGROUND_GRACE_MILLIS] ([onAppStarted]); the first start and a configuration change are not a return." KDoc của hai hàm mới nói luồng chính và ý nghĩa tham số.
- Log (ADR-0019, thẻ `Network`): dòng có sẵn `Dialog shown: offline_on_foreground` qua `showNow()`, hoặc `Dialog waits: full_screen_ad` qua `show()` khi quảng cáo đang hiện. Không thêm sự kiện phân tích.

**`:core:network`, `NetworkSettings`:** thêm `internal const val FOREGROUND_GRACE_MILLIS = 2_000L` với KDoc "How long a return to the app without network is checked again before the dialog shows (plan T-077, question 2)". Không là khóa Remote Config (không thêm vào `all()`), nên bảng khóa `docs/remote-config-keys.md` và `RemoteConfigKeysTest` không đổi.

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

**`:app`, `MainActivity.onCreate`:** sau `ads.gatherConsent(this)`, thêm `observeAppForeground(onStarted = networkDialog::onAppStarted, onStopped = networkDialog::onAppStopped)` kèm chú thích "Back from the background without network: the dialog shows within a second (spec network, KB-3; plan T-077)". Dùng observer thay vì ghi đè `onStart`/`onStop` để phần đọc `isChangingConfigurations` có test Robolectric (MainActivity cần Hilt, `:app` chưa có khung test dựng MainActivity); lời gọi trong `onCreate` có test quét mã nguồn `MainActivityForegroundWiringTest`.

**Thứ tự với quảng cáo quay lại app** (khung AdsKit, T-074): khi quay lại, `AdsActivityTracker.onActivityStarted` (gọi trong `super.onStart()`, trước observer ON_START, xem 4.1) quyết quảng cáo `resume` (`ResumeAdTurns.onProcessForegroundChanged`), rồi hiện ở `onActivityResumed`. Khi không có mạng, khung không tải quảng cáo mới (`NOT_READY` thì chỉ gọi `loadIfNeeded`, tải không đi được), nhưng quảng cáo đã nạp sẵn vẫn hiện: `presence.showing` lên `true`, `AppAdsRoot` đưa cờ này tới `setFullScreenAdShowing(true)` ở khung hình kế. Khi 2 giây kiểm lại hết, `show()` thấy `adShowing` và giữ lý do vào `pendingShow`; quảng cáo đóng thì hộp thoại hiện. Không có quảng cáo thì hộp thoại hiện sau 2 giây. `AdActivity` của SDK dùng theme trong suốt (`Theme.Translucent`), nên `MainActivity` chỉ tạm dừng (`onPause`), không dừng: quảng cáo toàn màn hình không tạo thêm một lần quay lại. Plan không chạm `:core:ads`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogController.kt` | `onAppStarted()`, `onAppStopped()`, `checkOnForeground()`, `foregroundJob` hủy trong `disarm()`, KDoc |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkSettings.kt` | Hằng `FOREGROUND_GRACE_MILLIS = 2_000L` (không phải khóa Remote Config) |
| Sửa | `core/network/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogControllerTest.kt` | Các test mới của mục 4, thêm vào cuối lớp, không sửa test cũ |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/network/AppForegroundObserver.kt` | `ComponentActivity.observeAppForeground(onStarted, onStopped)` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/network/AppForegroundObserverTest.kt` | Test Robolectric của mục 4 |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | Gọi `observeAppForeground(...)` trong `onCreate` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/network/MainActivityForegroundWiringTest.kt` | Test quét mã nguồn: `MainActivity.onCreate` gọi `observeAppForeground(onStarted = networkDialog::onAppStarted, onStopped = networkDialog::onAppStopped)` |

`scripts/tests/run.sh` có nhắc `MainActivity.kt` (dòng đếm `exposeTestTagsForUiAutomator()` bằng 1): không bị ảnh hưởng, vì plan chỉ thêm một lời gọi `observeAppForeground(...)` và không thêm hay bớt `exposeTestTagsForUiAutomator()` (L-020). Không đổi file khác mà nơi khác đọc: không thêm khóa Remote Config (bảng `docs/remote-config-keys.md`, `RemoteConfigKeysTest` giữ nguyên), không ảnh chuẩn, không tài nguyên chuỗi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: dùng `feature_network_dialog_enabled` có sẵn (`FeatureFlag.NETWORK_DIALOG`) qua `armed`; không thêm cờ.
- Quảng cáo: Không chạm. Chỉ đọc thứ tự với quảng cáo `resume` có sẵn qua `setFullScreenAdShowing` (T-074 lượt 2); không đổi vị trí, định dạng hay tần suất.
- Sự kiện phân tích: Không chạm (spec, câu hỏi 5).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `ConnectivityManager.getActiveNetwork`/`getNetworkCapabilities` đã dùng `ACCESS_NETWORK_STATE` có sẵn của `:core:network` (T-011); `Activity.isChangingConfigurations` và `Lifecycle.addObserver` không cần quyền. Không thêm: `INTERNET` (mục 6 chỉ nhắc `NET_CAPABILITY_INTERNET` của T-076, một khả năng của mạng, không phải quyền của app).
- **Thư viện và SDK:** Không thêm. `DefaultLifecycleObserver`, `LifecycleOwner` thuộc thư viện lifecycle của AndroidX (cùng phiên bản `androidxLifecycle` của `gradle/libs.versions.toml`), đã có trên classpath biên dịch của `:app` qua `androidx.activity` (phụ thuộc `api`, `ComponentActivity.lifecycle` trả kiểu của thư viện này); chạy thử ở 4.1 biên dịch đúng hai lớp này trong source set test của `:app`. `:core:network` chỉ dùng `kotlinx.coroutines` có sẵn.

### 3.4. Phương án dự phòng

Không có. Phần chưa chắc duy nhất (Robolectric có báo `isChangingConfigurations` đúng khi dựng lại activity không) đã chạy thử ở 4.1.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (2026-10-29): 140 test dùng tên của các file plan sửa; "Cần xem trước" 0; "Nên xem" 11: `MergedManifestTest.mergedManifest_hasNoAdGalleryActivity`, `ScreenOrientationTest.manifest_keepsPortrait_soPhonesStartWithoutARelaunch` (manifest của `MainActivity`, plan không đổi manifest), `RemoteConfigKeysTest.networkKeys_areListed` (so `NetworkSettings.all()`, plan không đổi `all()`), `NetworkSettingsTest.lostDelayIs3SecondsByDefault`, `NetworkSettingsTest.retryWaitsAtMost2Seconds` (hai hằng cũ giữ giá trị), `NetworkDialogControllerTest.adCloses_withoutLoss_staysHidden`, `adStartsWhileDialogShown_hidesThenShowsAgainOnClose`, `lostWhileAdShowing_dialogWaits_thenShowsWhenAdCloses` (đường chờ quảng cáo không đổi, test mới dùng lại nó), `NativeFullScreenOverlayTest.recreate_keepsDeadline_notRestarted`, `PhotoStoreTest.save_insertThrowsIllegalArgument_isIOException`, `LoadingRouteTest.backgroundWhileWaiting_thenResumed_showsOnce` (chỉ trùng chữ `onCreate`, không dùng `MainActivity`). Đã đọc từng test: không câu khẳng định nào bị đổi, vì plan chỉ thêm đường mới. Đường cũ (mất mạng quá 3 giây, lỗi tải, thử lại, chờ quảng cáo, cờ tắt) không đổi, và không test cũ nào gọi `onAppStarted`/`onAppStopped`.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `disarm()` hủy lần thử lại đang chạy và lần hiện đang chờ quảng cáo | Cũng hủy lần kiểm khi quay lại (`foregroundJob`) | `NetworkDialogControllerTest.deactivated_hidesDialog_andStopsWatching`, `inactiveWhileWaiting_dropsPending`, `flagTurnedOff_whileShown_hides` | Giữ nguyên |
| Quay lại app không có mạng: hộp thoại chỉ hiện sau 3 giây của KB-1 | Hiện sau tối đa 2 giây kiểm lại | Không có test khẳng định hành vi cũ (`deviceOffline_shownOnlyAfterDelay` không có lần quay lại) | Giữ nguyên; test mới ở mục 4 |
| `MainActivity` không theo dõi vòng đời cho mạng | Gắn `observeAppForeground` trong `onCreate` | `AppAdsRootTest` (chỉ `AppAdsRoot`, không phải `onCreate`) | Giữ nguyên |

## 4. Kiểm thử

Unit test thêm vào `NetworkDialogControllerTest` (thời gian ảo, helper `controller()` có sẵn, `RecordingAppLogger`). Quy ước: "ra nền" là `controller.onAppStopped(changingConfigurations = false)`, "quay lại" là `controller.onAppStarted()` rồi `runCurrent()`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-3 mới: quay lại không có mạng thì hiện ngay, trước 3 giây của KB-1 | Unit test | `returnOffline_showsAfterGrace_beforeLostDelay`: controller có mạng; ra nền; `connected = false`; quay lại; `advanceTimeBy(1_999)` → `HIDDEN`; `advanceTimeBy(2)` → `SHOWN` (tức 2 001 ms, trước 3 000 ms của đường cũ); log info có đúng một dòng `Dialog shown: offline_on_foreground` |
| KB-3 mới: máy nối lại mạng trong khoảng kiểm lại thì không hiện (rủi ro của ticket) | Unit test | `returnOffline_networkBackWithinGrace_showsNothing`: ra nền; offline; quay lại; `advanceTimeBy(500)`; `connected = true`; `advanceTimeBy(5_000)` → `HIDDEN`, không dòng `Dialog shown`. Đối chứng là test trên (cùng dựng, không bật lại mạng → `SHOWN`) |
| KB-3 mới: quay lại có mạng thì không hiện | Unit test | `returnOnline_showsNothing`: ra nền; quay lại có mạng; `advanceTimeBy(5_000)` → `HIDDEN` |
| Lần mở đầu không tính là quay lại | Unit test | `firstStart_isNotAReturn`: controller có mạng; `connected = false`; `onAppStarted()` không có `onAppStopped` trước; `advanceTimeBy(2_500)` → `HIDDEN`; `advanceTimeBy(501)` → `SHOWN` ở 3 001 ms (đường 3 giây vẫn chạy, đối chứng) |
| Xoay màn hình không tính là quay lại | Unit test | `configurationChange_isNotAReturn`: `onAppStopped(changingConfigurations = true)`; offline; quay lại; `advanceTimeBy(2_500)` → `HIDDEN`. Đối chứng: `returnOffline_showsAfterGrace_beforeLostDelay` (cùng bước với `false` → `SHOWN` ở 2 001 ms) |
| Một lần ra nền chỉ cho một lần kiểm | Unit test | `secondStartWithoutStop_isNotAReturn`: ra nền; quay lại có mạng; `advanceTimeBy(2_500)`; `connected = false`; `onAppStarted()` lần nữa (không ra nền); `advanceTimeBy(2_500)` → `HIDDEN` (2 500 ms từ lúc mất mạng: lần kiểm đã hết hiện ở 2 001 ms, đường 3 giây chưa tới) |
| Màn loading không tính (KB-3 mới, "sau màn loading") | Unit test | `returnOnLoadingScreen_ignored`: `controller(active = false)`; ra nền; offline; quay lại; `advanceTimeBy(5_000)` → `HIDDEN` |
| KB-4: cờ tắt thì không hiện | Unit test | `returnWithFlagOff_ignored`: `flags.enabled.value = emptySet()` trước khi dựng; ra nền; offline; quay lại; `advanceTimeBy(5_000)` → `HIDDEN` |
| Cờ tắt hay về loading giữa lúc kiểm lại (L-061) | Unit test | `flagTurnedOffDuringGrace_dropsShow`: ra nền; offline; quay lại; `advanceTimeBy(500)` (giữa lúc kiểm lại); `flags.enabled.value = emptySet()`; `advanceTimeBy(100)`; bật lại cờ (`setOf(FeatureFlag.NETWORK_DIALOG)`); `advanceTimeBy(1_500)`: khẳng định `HIDDEN` ở 2 100 ms (lần kiểm khi quay lại đã bị hủy, không thì hộp thoại hiện ở 2 000 ms) và log không có `offline_on_foreground`; đối chứng: `advanceTimeBy(1_501)` → `SHOWN` ở 3 601 ms với lý do `device_offline` (đường 3 giây bắt đầu lại từ lúc bật cờ ở 600 ms). `deactivatedDuringGrace_dropsShow`: ra nền; offline; quay lại; `advanceTimeBy(500)`; `setActive(false)`; `advanceTimeBy(5_000)` → `HIDDEN`, log không có `offline_on_foreground` |
| KB-1: chỉ một hộp thoại khi hai đường cùng hiện | Unit test | `returnOffline_andLostDelay_singleDialog`: ra nền; offline; quay lại; `advanceTimeBy(2_001)` → `SHOWN`, log có `Dialog shown: offline_on_foreground` (khung rỗng: vẫn `HIDDEN` ở đây); `advanceTimeBy(3_000)` (qua 3 giây của đường cũ); `seenStates` có đúng một `Shown(checking = false, stillOffline = false)`; log có đúng một dòng bắt đầu bằng `Dialog shown` |
| KB-3 có sẵn: hộp thoại đã hiện trước khi ra nền vẫn hiện, không hiện lại lần hai | Unit test | `shownBeforeBackground_staysOnReturn`: offline, `advanceTimeBy(3_001)` → `SHOWN`; ra nền; quay lại; `advanceTimeBy(2_500)` → vẫn `SHOWN`, log có đúng một `Dialog shown` |
| Ra nền giữa lúc kiểm lại (L-091) | Unit test | `backgroundDuringGrace_dialogStaysOnReturn`: ra nền; offline; quay lại; `advanceTimeBy(500)`; ra nền; `advanceTimeBy(1_600)` → `SHOWN` ở 2 100 ms (job vẫn chạy khi ở nền, mục 3.1; khung rỗng: `HIDDEN`); quay lại; `advanceTimeBy(2_500)` → `SHOWN`, đúng một dòng `Dialog shown` |
| Mục 5 của spec: quảng cáo quay lại app hiện trước, hộp thoại chờ quảng cáo đóng | Unit test | `returnOffline_whileAdShowing_waitsForAd`: ra nền; offline; quay lại; `advanceTimeBy(100)`; `setFullScreenAdShowing(true)` (quảng cáo `resume` vừa hiện); `advanceTimeBy(2_000)` → ở 2 100 ms, trước 3 giây của đường cũ: `HIDDEN` và log đã có `Dialog waits: full_screen_ad` (khung rỗng: chưa có dòng này, vì đường 3 giây chỉ ghi nó ở 3 000 ms); `advanceTimeBy(3_000)` → vẫn `HIDDEN`; `setFullScreenAdShowing(false)` → `SHOWN`, đúng một dòng `Dialog shown` |
| Sau "Thử lại" có mạng, lần quay lại sau vẫn kiểm | Unit test | `afterRecovery_nextReturnChecksAgain`: ra nền; offline; quay lại; `advanceTimeBy(2_001)` → `SHOWN`; `connected = true`; `retry()`; `runCurrent()` → `HIDDEN`; ra nền; offline; quay lại; `advanceTimeBy(2_001)` → `SHOWN` |
| `:app` báo bắt đầu và dừng, phân biệt đổi cấu hình | Robolectric (`:app`) | `AppForegroundObserverTest` (`@Config(application = Application::class)` như `ScreenOrientationTest`, `Robolectric.buildActivity(ComponentActivity::class.java)`, gọi `observeAppForeground` sau `create()`): `create().start().resume()` → `started = 1`, `stops = []`; `pause().stop()` → `stops = [false]`; `start().resume()` → `started = 2`; `recreate()` → `stops = [false, true]` |
| Dựng lại activity (L-061) | Robolectric (`:app`) | Cùng test, bước `recreate()`: activity cũ báo `onStopped(true)`. Controller nhận `true` thì bỏ qua lần bắt đầu kế (`configurationChange_isNotAReturn`). Activity mới gắn observer của nó trong `onCreate` (MainActivity gọi trong `onCreate` ở mỗi lần dựng) |
| Đưa activity ra nền rồi trở lại (L-091) | Robolectric (`:app`) | Cùng test, bước `pause().stop()` rồi `start().resume()` (tương đương `moveToState(CREATED)` rồi `RESUMED`; `ActivityScenario` cần activity khai báo trong manifest, `ComponentActivity` trơn thì không) |
| `MainActivity` nối observer vào controller | Unit test quét mã nguồn (`:app`) | `MainActivityForegroundWiringTest.mainActivity_observesAppForeground`: đọc `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` (thư mục làm việc của unit test là thư mục module), bỏ chú thích và chuỗi như `codeOnly` của `LeaveAppCallSitesTest`, khẳng định có đúng một lời gọi khớp `observeAppForeground\(\s*onStarted\s*=\s*networkDialog::onAppStarted\s*,\s*onStopped\s*=\s*networkDialog::onAppStopped\s*,?\s*\)` và nó nằm trong thân `override fun onCreate`; đối chứng: tệp đọc được có `class MainActivity` (đúng tệp thật). Quét theo ký hiệu, không theo chuỗi con (L-062 của mẫu) |
| Quay lại app thật, tắt Wi-Fi và dữ liệu, có và không có quảng cáo `resume` | Kiểm tra thủ công (máy thật: khác — trình chạy kịch bản chưa có lệnh đưa app ra nền rồi mở lại mà không xóa dữ liệu (`fresh` xóa dữ liệu), và Wi-Fi nối lại khi mở màn hình chỉ thấy trên máy thật) | Bản staging, cờ `feature_network_dialog_enabled` bật: (1) mở camera, Home, tắt Wi-Fi và dữ liệu rồi mở lại app từ danh sách gần đây trong vòng 3 giây kể từ lúc tắt (đường 3 giây vẫn chạy khi app ở nền, nên chờ lâu hơn thì hộp thoại đã hiện sẵn; lỗi của ticket chỉ thấy rõ khi callback mạng bị hoãn, ví dụ tiến trình bị đóng băng ở nền, cached-app freezer): hộp thoại hiện trong khoảng 2 giây sau khi mở lại (`adb logcat -v threadtime \| grep ' VGL\.'` có `Dialog shown: offline_on_foreground`); (2) bật lại mạng, "Thử lại", Home, khóa màn hình 1 phút, mở khóa rồi mở app khi Wi-Fi tự nối lại: không có hộp thoại oan; (3) xoay màn hình trên máy gập mở hay máy tính bảng khi không có mạng dưới 3 giây: không hiện ngay; (4) bật `ads_resume_enabled` trên staging, mở app có mạng để quảng cáo `resume` nạp sẵn, Home, tắt mạng, mở lại: quảng cáo hiện trước, hộp thoại hiện sau khi đóng quảng cáo (log `Dialog waits: full_screen_ad`, rồi một dòng `Dialog shown: …` bất kỳ lý do ngay sau khi đóng quảng cáo; lý do là lý do của lần hiện đã chờ, không phải `after_full_screen_ad`) |

Test viết trước: mọi unit test và `AppForegroundObserverTest` ở trên thất bại trên code hiện tại vì chưa có `onAppStarted`, `onAppStopped`, `observeAppForeground` (lỗi biên dịch). Để thất bại đúng lý do, bước 3 thêm vào `NetworkDialogController` hai hàm rỗng và vào `AppForegroundObserver.kt` một hàm rỗng (khung, cùng commit test); khi đó `returnOffline_showsAfterGrace_beforeLostDelay`, `returnOffline_andLostDelay_singleDialog` (`HIDDEN` ở 2 001 ms), `afterRecovery_nextReturnChecksAgain`, `returnOffline_whileAdShowing_waitsForAd` (chưa có `Dialog waits` ở 2 100 ms), `backgroundDuringGrace_dialogStaysOnReturn` (`HIDDEN` ở 2 100 ms), `AppForegroundObserverTest` và `MainActivityForegroundWiringTest` (chưa có lời gọi) đỏ; các test "không hiện" (`returnOnline_showsNothing`, `firstStart_isNotAReturn`, `configurationChange_isNotAReturn`, `returnOnLoadingScreen_ignored`, `returnWithFlagOff_ignored`, `secondStartWithoutStop_isNotAReturn`, `flagTurnedOffDuringGrace_dropsShow`, `deactivatedDuringGrace_dropsShow`, `returnOffline_networkBackWithinGrace_showsNothing`, `shownBeforeBackground_staysOnReturn`) qua ngay trên khung rỗng: đối chứng của từng test là một test "hiện" cùng bước dựng, ghi ở cột trên (L-035).

**Ngoại lệ viết test trước:** Không có. Dòng nối trong `MainActivity.onCreate` có `MainActivityForegroundWiringTest` (quét mã nguồn), vì `MainActivity` cần Hilt và `:app` chưa có khung test dựng nó; kiểm tra thủ công (1) xác nhận nó chạy trên máy.

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
| D. Ảnh hưởng chéo | 1 | Dùng `setFullScreenAdShowing` và quảng cáo `resume` có sẵn của `:core:ads` mà không sửa; T-076 cùng module `:core:network` nhưng mục 3.2 của plan T-076 không sửa file nào T-077 sửa (mục 6). Hành vi mới chỉ thêm, `NetworkStatus`/`NetworkFailureReporter` mà feature dựa vào không đổi |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu có sẵn: vòng kiểm như `retry()`, observer vòng đời như `AdsActivityTracker`, bản mẫu T-017 |
| **Tổng** | 9 | **Mức: Cao** (điểm chặn 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NetworkDialogController` (dùng bởi `MainActivity`, `NetworkDialogHost`; `NetworkStatus`, `NetworkFailureReporter` mà feature dùng không đổi), `NetworkSettings` (thêm hằng `internal`, không đổi `all()`), `MainActivity.onCreate`.
- Task khác có thể xung đột: **T-076** (plan `features/network/plans/T-076-co-mang-da-xac-nhan.md`, đã chọn hướng (a): "có mạng" là mạng mặc định có `INTERNET` và `VALIDATED`). Mục 3.2 của plan T-076 sửa `ConnectivitySource.kt` và test của nó; không sửa `NetworkDialogController.kt`, `NetworkDialogControllerTest.kt`, `Fakes.kt`, `NetworkSettings.kt` hay `MainActivity.kt`, nên hai plan không cùng sửa file nào. Ảnh hưởng về hành vi: sau T-076, `isConnected()` chỉ trả `true` khi mạng đã được xác nhận, nên lần kiểm khi quay lại cần đủ thời gian cho xác nhận; vì vậy plan chọn 2 giây (câu hỏi 2).
- T-074 (đã merge): plan dựa vào `setFullScreenAdShowing` của lượt 2 và quảng cáo `resume` của lượt 2; không sửa file của T-074 ngoài `MainActivity.kt` (một dòng trong `onCreate`, không chạm `AppAdsRoot`).
- Đề xuất thứ tự chạy: T-076 trước T-077 trong cùng danh sách. Không phụ thuộc nhau về code, nhưng 2 giây kiểm lại của T-077 được chọn theo định nghĩa "có mạng" của T-076 (đòi `VALIDATED`), và kiểm tra thủ công của T-077 phải chạy trên định nghĩa đó.

## 7. Điều kiện dừng riêng

- Cần sửa `:core:ads` (thứ tự quyết hay hiện quảng cáo `resume`) để hộp thoại và quảng cáo đúng thứ tự của mục 3.1: dừng (điểm chặn 1, quảng cáo).
- Cần thêm khóa Remote Config cho khoảng kiểm lại (đổi `NetworkSettings.all()`, bảng khóa): dừng, vì câu hỏi 2 chốt hằng trong code; trừ khi câu trả lời câu hỏi 2 chọn khóa Remote Config.
- Test cũ của `NetworkDialogControllerTest` đỏ sau khi thêm `foregroundJob`: dừng theo điều kiện 3 (plan nói đường cũ không đổi).

## 8. Câu hỏi cho người duyệt

1. **Tiêu chí mới trong spec network.** Spec đang `changed` (chờ xác nhận lại sau T-074) và chưa có tiêu chí này; plan không tự sửa spec. Đề xuất Nhật thêm vào KB-3, sau tiêu chí có sẵn, đúng câu sau, rồi xác nhận lại spec (`status: confirmed`) trước khi duyệt plan:
   - [ ] Khi người dùng đưa app ra nền rồi quay lại một màn sau màn loading mà máy không có mạng thì hộp thoại hiện ngay khi app trở lại, không chờ thời gian chờ của KB-1: máy được kiểm lại tối đa 2 giây để kịp nối lại mạng, có mạng trong khoảng đó thì hộp thoại không hiện. Lần mở app đầu tiên và xoay màn hình không tính là quay lại. Khi một quảng cáo toàn màn hình hiện lúc quay lại thì hộp thoại hiện sau khi quảng cáo đóng (mục 5).
   - Đề xuất của AI: thêm đúng câu trên.
2. **Khoảng kiểm lại khi quay lại.** Hiện ngay (như bản mẫu) dễ hiện oan khi Wi-Fi đang nối lại sau khi mở khóa màn hình, hay khi hệ thống vừa gỡ chặn mạng của app lúc trở về từ nền (Doze, tiết kiệm dữ liệu: `getActiveNetwork` trả `null` khi mạng của app đang bị chặn); chờ 3 giây như KB-1 thì không giải quyết ticket. Plan T-076 đã chọn đòi `VALIDATED`: emulator xác nhận mạng trong khoảng 0,14 giây, nhưng trên máy thật, nối lại Wi-Fi sau khi mở khóa rồi xác nhận (gửi thử một yêu cầu) thường chậm hơn nhiều, tới một hai giây. Hộp thoại hiện oan chặn cả app, còn hiện chậm một giây chỉ để người dùng thao tác thêm một giây. Đề xuất của AI: chọn 2 giây, kiểm mỗi 250 ms (bằng `RETRY_MAX_MILLIS`/`RETRY_POLL_MILLIS` của "Thử lại", cùng một chuẩn "chờ mạng được xác nhận"), hằng riêng `FOREGROUND_GRACE_MILLIS` trong code, không là khóa Remote Config; vẫn nhanh hơn 3 giây của KB-1, và quan trọng hơn là lần kiểm chạy ngay khi quay lại dù callback mạng bị hoãn.
3. **"Quay lại app" tính thế nào.** Ticket ghi `MainActivity.onStart` gọi hàm và bỏ qua lần bắt đầu đầu tiên (đếm như bản mẫu). Plan tính "quay lại" là `MainActivity` bắt đầu sau một lần dừng không do đổi cấu hình (`onAppStopped(false)`), gắn bằng observer vòng đời: lần đầu tự bị bỏ qua, xoay màn hình trên màn lớn (T-056) không bị tính. Hệ quả: trở về từ app khác mở từ app (chia sẻ ảnh, cài đặt hệ thống) cũng là một lần quay lại. Quảng cáo toàn màn hình của SDK (`AdActivity`, theme trong suốt) chỉ làm `MainActivity` tạm dừng nên không tính. Khác khung quảng cáo (đếm theo tiến trình, không coi bảng chia sẻ hay trang cài đặt mở từ app là ra nền theo luật của nó): trở về từ bảng chia sẻ hay trang cài đặt cũng chạy lần kiểm 2 giây; vô hại vì có mạng thì không hiện gì. Đề xuất của AI: đồng ý cách này, coi đó là thay cho câu "MainActivity.onStart gọi hàm này" của ticket.
4. **Thứ tự với quảng cáo quay lại app khi không có mạng.** Khung không tải quảng cáo mới khi mất mạng, nhưng quảng cáo `resume` nạp sẵn vẫn hiện. Đề xuất của AI: quảng cáo hiện trước, hộp thoại hiện sau khi quảng cáo đóng (đúng mục 5 của spec, không đổi `:core:ads`); nhờ 2 giây kiểm lại, hộp thoại không lóe lên trước quảng cáo. Mạng có lại trong lúc quảng cáo hiện thì hộp thoại vẫn hiện khi quảng cáo đóng, như đường "mất mạng quá 3 giây" hiện nay; người dùng bấm "Thử lại" là đóng (KB-2). Nếu muốn kiểm lại mạng lúc quảng cáo đóng thì đó là đổi chung cho mọi lý do hiện, nên để ticket riêng.
5. **Ra nền giữa lúc kiểm lại.** Đề xuất của AI: không hủy lần kiểm; nếu vẫn không có mạng thì hộp thoại hiện khi app ở nền (giống đường 3 giây hiện nay) và còn đó khi quay lại (KB-3). Đồng ý không?
6. **Thứ tự duyệt.** `scripts/plan-check.py` qua (2026-10-29), không có lỗi chỉ người sửa được. Plan chờ spec network `confirmed` có tiêu chí của câu hỏi 1 rồi mới duyệt; người duyệt chéo bắt buộc (điểm chặn 7). Đề xuất của AI: Nhật sửa spec và xác nhận lại cùng lúc với lần xác nhận đang chờ sau T-074.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
