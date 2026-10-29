---
ticket: T-076
title: Chốt định nghĩa "có mạng" theo ADR-0024 điều 1 (INTERNET và VALIDATED)
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn 7 (module core), không tính tổng
risk_level: high             # low | medium | high
token_estimate: 6.4M         # cỡ S, trung vị của scripts/token-usage.py estimate
blocking_points: [core]
run_order:
reviewer: Nhật
cross_reviewer:
approved_at:
lane:
---

# PLAN T-076: CHỐT ĐỊNH NGHĨA "CÓ MẠNG" THEO ADR-0024 ĐIỀU 1 (INTERNET VÀ VALIDATED)

## 1. Tóm tắt

Theo câu trả lời của Nhật (ticket, câu 1: hướng (a)), `AndroidConnectivitySource` của `:core:network` coi "có mạng" là mạng mặc định có cả `NET_CAPABILITY_INTERNET` lẫn `NET_CAPABILITY_VALIDATED`, như `hasUsableInternet` của dự án mẫu (T-016 của mẫu, commit `c77076e`), và bỏ `onAvailable` (lúc đó hệ thống chưa xác nhận mạng). Hộp thoại mất mạng và khung quảng cáo (`InternetConnectivity`, cùng nguồn) cùng đổi theo. Kiểm bằng test Robolectric mới cho `AndroidConnectivitySource` (đã chạy thử: thất bại đúng lý do trên code hiện tại, qua sau khi đổi), và một tệp kịch bản emulator chứng minh hộp thoại không hiện oan khi có mạng; log của emulator CI hiện có cho thấy mạng được xác nhận trong khoảng 0,14 giây sau khi bật lại, nên không cần quay về hướng (b).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/network/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi máy báo không còn kết nối mạng và tình trạng này kéo dài quá thời gian chờ thì hộp thoại "Mất kết nối mạng" hiện | "Máy báo không còn kết nối" theo định nghĩa đã chốt (ADR-0024 điều 1): mạng mặc định không có, hoặc thiếu `INTERNET`, hoặc chưa (hay không còn) `VALIDATED`. Wi-Fi có cổng đăng nhập hay Wi-Fi không ra được Internet là không có mạng, nên sau thời gian chờ (3 giây, `network_lost_delay_seconds`) hộp thoại hiện mà không cần đợi một lần tải thất bại. Bộ đếm thời gian chờ và luật hiện hộp thoại trong `NetworkDialogController` giữ nguyên |
| KB-1 | Mạng chập chờn mất rồi có lại trong thời gian chờ thì hộp thoại không hiện | Giữ nguyên (`collectLatest` của controller hủy lần chờ khi giá trị đổi). Mạng mới chưa xác nhận mà được xác nhận trong 3 giây thì hộp thoại không hiện; trên emulator CI việc xác nhận mất khoảng 0,14 giây (mục 4.1) |
| KB-2 | Bấm "Thử lại" mà đã có mạng thì hộp thoại đóng; vẫn chưa có mạng thì hộp thoại vẫn hiện | `retry()` đọc `isConnected()`, giờ đòi cả `VALIDATED`: mạng có lại nhưng chưa xác nhận trong 2 giây kiểm tra thì hiện "Vẫn chưa có mạng", người dùng bấm lại. Luật thử lại giữ nguyên |

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện; hộp thoại `NetworkDialog` giữ nguyên, docs/ops/design-workflow.md)

ADR liên quan: ADR-0024 (điều 1, điều 2), ADR-0001 (không đổi phụ thuộc module), ADR-0031 (mục 3.5).

## 3. Giải pháp

### 3.1. Cách làm

Chỉ đổi lớp mỏng đọc trạng thái mạng của hệ thống; không đổi `NetworkDialogController`, `InternetConnectivity` hay `:app`.

`core/network/.../ConnectivitySource.kt`, lấy từ bản mẫu (`vglstudio/template`, commit `c77076e`, `core/network/src/main/kotlin/com/vglstudio/template/core/network/ConnectivitySource.kt`), đổi gói về `com.vglstudio.camera.pose.line.guide.core.network` và dẫn KB-1 của spec app (spec mẫu ghi KB-3). Diff đã chạy thử (mục 4.1):

```diff
-/** Whether the device has a network that can reach the Internet. */
+/** Whether the device has usable Internet, read from the system's network state only (spec network, KB-1). */
 internal interface ConnectivitySource {
+    /** Usable Internet right now: the default network has INTERNET and VALIDATED (ADR-0024). */
     fun isConnected(): Boolean
@@
 /**
- * Connected means the default network has [NetworkCapabilities.NET_CAPABILITY_INTERNET]; validation is not required,
- * because some devices validate slowly or never (plan T-011, question 1). No decision is made here.
+ * A network that has not been validated, such as Wi-Fi behind a sign-in page or Wi-Fi without a way out, is no
+ * Internet (spec network KB-1, ADR-0024 point 1; plan T-076). The kind of connection does not matter.
+ */
+internal fun isUsableInternet(hasInternet: Boolean, isValidated: Boolean): Boolean = hasInternet && isValidated
+
+internal fun NetworkCapabilities?.hasUsableInternet(): Boolean = this != null &&
+    isUsableInternet(
+        hasInternet = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
+        isValidated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
+    )
+
+/**
+ * Watches the default network through [ConnectivityManager]. Reads only its capabilities: no Wi-Fi name, IP address
+ * or network identifier. onAvailable is not used: the system has not validated the network yet at that point, and
+ * onCapabilitiesChanged reports it once it has.
  */
 internal class AndroidConnectivitySource(context: Context) : ConnectivitySource {
     private val manager = context.getSystemService(ConnectivityManager::class.java)
 
-    override fun isConnected(): Boolean = manager.getNetworkCapabilities(manager.activeNetwork).hasInternet()
+    override fun isConnected(): Boolean = manager.getNetworkCapabilities(manager.activeNetwork).hasUsableInternet()
 
     override fun connectivity(): Flow<Boolean> = callbackFlow {
         val callback = object : ConnectivityManager.NetworkCallback() {
-            override fun onAvailable(network: Network) {
-                trySend(true)
-            }
-
             override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
-                trySend(capabilities.hasInternet())
+                trySend(capabilities.hasUsableInternet())
             }
@@
     }.distinctUntilChanged()
-
-    private fun NetworkCapabilities?.hasInternet(): Boolean =
-        this?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
 }
```

Vì sao bỏ `onAvailable`: với `registerDefaultNetworkCallback` (minSdk 26), hệ thống gọi `onAvailable` rồi ngay sau đó `onCapabilitiesChanged` cho mạng mặc định mới; giữ `trySend(true)` ở `onAvailable` thì mạng chưa xác nhận được báo là có mạng trong chốc lát. `onLost` giữ `trySend(false)`.

Các phần khác:

- `InternetConnectivity` (khung quảng cáo, T-074) đọc `ConnectivitySource.connectivity()` nên tự đổi theo. `AdsController` không chặn lần nạp theo giá trị này; nó dùng `online` ở bốn chỗ, đều đổi theo định nghĩa mới: (1) nhãn `offline` của số liệu nạp quảng cáo (`AdsController.kt`, dòng 273 và 1947, `offline = !online.value`); (2) lý do không có quảng cáo thưởng `NETWORK` thay vì `NOT_READY` (`AdsController.kt` dòng 411, `RewardedRules.kt` dòng 72 tới 76, `noAdReason`, `isNetworkLoadError`); (3) nạp sẵn lại khi mạng trở lại, S-24 (`networkReturns(online)`, `AdsController.kt` dòng 589); (4) hỏi lại đồng ý khi Internet trở lại (`ConsentCoordinator.onInternetChanged`, dòng 136, S-09). Hệ quả: mạng chưa xác nhận được tính là mất mạng ở cả bốn chỗ; một lần mạng mất xác nhận rồi được xác nhận lại (trước đây không ai thấy) giờ là một lần mất và một lần trở lại, nên có thêm một lượt nạp sẵn và một lần hỏi lại đồng ý (chỉ khi luật của `ConsentCoordinator` cho phép). Không sửa `DefaultInternetConnectivity` hay `:core:ads`; test mới kiểm `InternetConnectivity` qua nguồn thật (mục 4).
- `feature/loading/.../InternetCheck.kt`: chỉ sửa KDoc của `isValidatedInternet`, câu "Unlike AndroidConnectivitySource of :core:network (INTERNET only, plan T-011, question 1) on purpose ..." thành "Same rule as AndroidConnectivitySource of :core:network (ADR-0024 point 1, plan T-076)." Không đổi code của màn loading, không gộp hai hàm (feature không phụ thuộc `:core:network` theo kiểu này; gộp là việc khác).
- `core/network/build.gradle.kts`: thêm `testImplementation(libs.robolectric)`, `testImplementation(libs.androidx.test.core)`, `testImplementation(libs.androidx.test.ext.junit)`, như bản mẫu. Thư viện đã có trong `gradle/libs.versions.toml`; `:core:network` không dùng plugin compose nên không tự có Robolectric. Thêm `core/network/src/test/resources/robolectric.properties` (`sdk=35`, như `core/ads`, `core/review`): Robolectric 4.15.1 không chạy được `targetSdk 36` (`DefaultSdkPicker`, `IllegalArgumentException`, thấy ở mục 4.1).
- Tệp kịch bản `app/src/deviceScenarios/T-076-co-mang-khong-hien-hop-thoai.txt` (mục 4): `device-session` cuối phiên chạy nó trên emulator CI.
- Spec network: không sửa trong task (xem câu 1 ở mục 8).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/ConnectivitySource.kt` | Đòi `INTERNET` và `VALIDATED` (`isUsableInternet`, `hasUsableInternet`), bỏ `onAvailable`, KDoc theo ADR-0024 (diff ở mục 3.1) |
| Sửa | `core/network/build.gradle.kts` | Thêm ba dòng `testImplementation` (Robolectric, `androidx.test:core`, `androidx.test.ext:junit`), thư viện đã có |
| Tạo | `core/network/src/test/resources/robolectric.properties` | `sdk=35`, như `core/ads/src/test/resources/robolectric.properties` |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/network/AndroidConnectivitySourceTest.kt` | Test Robolectric của mục 4 |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/data/InternetCheck.kt` | Chỉ KDoc của `isValidatedInternet` (mục 3.1), không đổi code |
| Tạo | `app/src/deviceScenarios/T-076-co-mang-khong-hien-hop-thoai.txt` | Kịch bản emulator của mục 4 |
| Sửa | `scripts/device-scenario.py` | Mặc định `DEVICE_SCENARIOS_BUDGET` 1500 thành 1800: dòng 512 (`env_seconds("DEVICE_SCENARIOS_BUDGET", 1800)`) và docstring dòng 67 (mục 4, Ngân sách) |
| Sửa | `scripts/device-session.sh` | Cùng mặc định 1800: chú thích dòng 32 và `${DEVICE_SCENARIOS_BUDGET:-1800}` dòng 276 |
| Sửa | `docs/ops/test-sessions.md` | Mục 5, Hạn chót (dòng 177): "mặc định 1500" thành "mặc định 1800" |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có cờ mới. `feature_network_dialog_enabled` (`FeatureFlag.NETWORK_DIALOG`) giữ nguyên vai trò: tắt thì không hiện hộp thoại (KB-4). Định nghĩa mới không đứng sau cờ: ticket chốt một định nghĩa cho cả app và khung quảng cáo, và cờ tắt thì hộp thoại đã không hiện.
- Quảng cáo: Không chạm vị trí, định dạng, tần suất hay SDK, không sửa file của `:core:ads`. Khung đọc `InternetConnectivity`: nhãn `offline` của số liệu nạp, lý do `NETWORK` của quảng cáo thưởng, nạp sẵn khi mạng trở lại (S-24) và hỏi lại đồng ý khi Internet trở lại (S-09) giờ theo mạng đã xác nhận (mục 3.1).
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `ConnectivityManager.getNetworkCapabilities`, `activeNetwork`, `registerDefaultNetworkCallback` và `NetworkCapabilities.hasCapability` cần `ACCESS_NETWORK_STATE` trên mọi bản từ API 26 tới 36; quyền này đã có từ T-011 (manifest của `:core:network`). Không thêm: `ACCESS_WIFI_STATE`, `INTERNET` (đã có), không đọc tên Wi-Fi hay địa chỉ IP.
- **Thư viện và SDK:** Không thêm thư viện mới. `robolectric`, `androidx-test-core`, `androidx-test-ext-junit` đã có trong `gradle/libs.versions.toml`; `:core:network` thêm chúng ở cấu hình `testImplementation` (chỉ test, không vào manifest gộp hay bản release). `turbine` đã có cho mọi module qua `KotlinAndroid.kt`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** trên emulator CI, hộp thoại có hiện oan không (mạng được xác nhận chậm hơn thời gian chờ 3 giây, hay không bao giờ được xác nhận). Log của `device-session` gần nhất (TS-20261029-01) cho thấy không (mục 4.1), nhưng chỉ tệp kịch bản của task chạy sau merge mới chứng minh trên code mới. Cloud không có emulator, nên phiên không thử được trước merge.
- **Điều kiện chuyển:** `summary.md` của `device-session` cuối phiên ghi tệp `T-076-co-mang-khong-hien-hop-thoai.txt` là `lỗi` ở một bước `expect_no id=network_dialog` của phần (1) (trước `network off`) hay phần (3) (sau khi Thử lại đóng hộp thoại), và `app-log.txt` có `VGL.Network: Dialog shown: device_offline` mà không có lệnh `network off` trước đó; hoặc `logcat.txt` không có `validation passed` cho mạng mặc định.
- **Mạng chặn bước xác nhận:** có mạng mà máy chủ kiểm tra của Android (`connectivitycheck.gstatic.com`, `www.google.com/generate_204`) bị chặn, ví dụ tường lửa công ty hay một số vùng: hệ thống không bao giờ đặt `VALIDATED`, dù app vẫn tải được nội dung từ R2 và Remote Config. Với định nghĩa mới, hộp thoại hiện trên mọi màn sau 3 giây và "Thử lại" không bao giờ đóng nó; đây chính là lý do của plan T-011 câu 1. Đánh đổi: hướng (a) bắt được Wi-Fi có cổng đăng nhập và Wi-Fi không ra Internet ngay, nhưng chặn hẳn người dùng ở mạng như vậy; hướng (b) không chặn họ, nhưng Wi-Fi hỏng chỉ được bắt khi một lần tải thất bại. Cần gạt khi gặp: tắt `feature_network_dialog_enabled` trên Remote Config (KB-4), hộp thoại không hiện, các màn báo lỗi tải theo cách riêng; khung quảng cáo vẫn thấy `false` (nhãn `offline`, lý do `NETWORK`). Câu 4 ở mục 8.
- **Cách làm thay:** phiên không tự đổi định nghĩa: ghi phát hiện ở mức chặn vào báo cáo phiên, ghi vào mục Cần người kiểm tra để người chọn giữa (1) revert PR của task bằng PR thường rồi soạn plan phiên bản 2 theo hướng (b) (ADR app mới, mã từ 1001; code về `INTERNET` như trước; các file đó vào mục 3.2 của plan phiên bản 2, không làm trong phiên này), hay (2) giữ (a) nếu lỗi là của môi trường (ví dụ emulator mất Internet thật). Hướng (b) theo ticket: ghi lý do của plan T-011 câu 1 và số đo của lần lỗi; mở issue `from-app` nếu lý do có ích cho mẫu. Phương án này không hạ tiêu chí hay đổi test viết trước của mục 4.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py docs/features/network/plans/T-076-co-mang-da-xac-nhan.md` (chạy 2026-10-29) cho `ConnectivitySource.kt`, `InternetCheck.kt`, `NetworkModule.kt`: 26 test dùng tên của các file đó; "Cần xem trước" 0; "Nên xem" 2: `PosesViewModelTest.catalogFails_showsError_andLogsContentLoadFailedCatalog` và `PoseDetailViewModelTest.imageFails_isFailed_andLogsContentLoadFailedImage`, chỉ dùng tên `networkStatus` của `NetworkModule.kt` (bản giả báo lỗi tải vì mạng), không đọc trạng thái mạng; plan đã nêu 6 test (5 của `InternetCheckTest`, 1 của `DefaultInternetConnectivityTest`); 18 test còn lại (`--all`): 17 của `NetworkDialogControllerTest` dùng bản giả `FakeConnectivitySource` (`Fakes.kt`) và `LoadingViewModelTest.startupAd_notAskedBeforeTwoSeconds` dùng `InternetCheck` giả; không test nào đọc `NetworkCapabilities` qua nguồn thật. Không test nào khẳng định định nghĩa cũ:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Mạng mặc định có `INTERNET` là có mạng | Có mạng khi có cả `INTERNET` và `VALIDATED` | Không có: `NetworkDialogControllerTest`, `DefaultInternetConnectivityTest` dùng `FakeConnectivitySource` (giá trị `Boolean` đặt tay), không đọc `NetworkCapabilities` | Giữ: các câu khẳng định của hai lớp test đó nói về giá trị `true`/`false` của nguồn, không về cách tính; qua không đổi (đã chạy, mục 4.1) |
| `onAvailable` báo có mạng ngay | `onAvailable` không báo gì; `onCapabilitiesChanged` báo khi đã xác nhận | Không có | Không có câu cũ |
| Khung quảng cáo coi mạng chưa xác nhận là có mạng (nhãn `offline`, lý do `NETWORK`, nạp sẵn S-24, hỏi lại đồng ý S-09) | Mạng chưa xác nhận là mất mạng ở cả bốn chỗ; mất rồi lại được xác nhận là một lần mạng trở lại | Test của `core/ads` (`AdsControllerFixtures.kt`) dùng `InternetConnectivity` giả, đặt `Boolean` tay, không đọc nguồn thật | Giữ |
| Màn loading: `isValidatedInternet` đòi `VALIDATED` | Không đổi code | `InternetCheckTest` (5 test) | Giữ, chỉ KDoc đổi |

## 4. Kiểm thử

Test viết trước, trong `AndroidConnectivitySourceTest` (Robolectric, `@RunWith(AndroidJUnit4::class)`, mạng mặc định của Robolectric không có `VALIDATED`). Hàm phụ trong lớp test: `capabilities(vararg capability: Int)` tạo `ShadowNetworkCapabilities.newInstance()`, thêm `TRANSPORT_WIFI` và các capability. Test chỉ gọi API đã có (`AndroidConnectivitySource`, `isConnected()`, `connectivity()`, `DefaultInternetConnectivity`), nên commit test của bước 3 biên dịch được và thất bại vì câu khẳng định, không vì thiếu hàm. `isUsableInternet` và `hasUsableInternet` được kiểm qua `isConnected()` và `connectivity()`. Riêng câu của mẫu `assertFalse((null as NetworkCapabilities?).hasUsableInternet())` (không có mạng mặc định thì không có mạng) gọi thẳng hàm mới, nên nằm trong test `hasUsableInternet_noCapabilities_isFalse` thêm ở commit code của bước 4 (L-025 của mẫu), không ở commit test của bước 3.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, "máy báo không còn kết nối" theo ADR-0024 điều 1: chỉ `INTERNET` hay chỉ `VALIDATED` là không có mạng | Unit test (Robolectric) | `AndroidConnectivitySourceTest.isConnected_needsInternetAndValidated`: đặt `shadowOf(manager).setNetworkCapabilities(activeNetwork, capabilities(INTERNET))` thì `assertFalse(source.isConnected())`; `capabilities(VALIDATED)` thì `assertFalse`; đối chứng: `capabilities(INTERNET, VALIDATED)` thì `assertTrue`. Trên code hiện tại: thất bại ở câu đầu (đã thấy, mục 4.1) |
| KB-1: mạng mới chưa xác nhận không được báo là có mạng; mất xác nhận hay mất mạng thì báo mất | Unit test (Robolectric, Turbine) | `AndroidConnectivitySourceTest.connectivity_waitsForValidation_thenReportsLoss_andUnregisters`: mạng mặc định chỉ có `INTERNET`, `connectivity().test { }`: giá trị đầu `false`; một callback được đăng ký (`shadowOf(manager).networkCallbacks.single()`); `onAvailable(network)` rồi `onCapabilitiesChanged(network, INTERNET)` thì `expectNoEvents()`; `onCapabilitiesChanged(INTERNET, VALIDATED)` thì `true`; `onCapabilitiesChanged(INTERNET)` thì `false`; lại `INTERNET, VALIDATED` thì `true`; `onLost` thì `false`; sau khi hủy, `networkCallbacks` rỗng. Trên code hiện tại: thất bại ở giá trị đầu (`true`) (đã thấy, mục 4.1) |
| Khung quảng cáo dùng cùng định nghĩa (S-09, plan T-074 mục 6) | Unit test (Robolectric) | `AndroidConnectivitySourceTest.internetConnectivity_followsValidation`: `DefaultInternetConnectivity(AndroidConnectivitySource(context)).connected.first()` là `false` khi mạng mặc định chỉ có `INTERNET`; đối chứng: `true` sau khi đặt `INTERNET, VALIDATED`. Trên code hiện tại: dự kiến thất bại ở câu `false` (cùng nguyên nhân với test đầu; chưa chạy trên code cũ, chỉ chạy trên code mới, qua) |
| KB-1 tiêu chí 1, KB-1 mạng chập chờn, KB-2: luật hiện, chờ, thử lại | Unit test có sẵn | `NetworkDialogControllerTest` (24 test) giữ nguyên, qua (mục 4.1) |
| KB-1: có mạng đã xác nhận thì hộp thoại không hiện oan (sau màn loading, quá 3 giây; và ngay sau khi mạng có lại); mất mạng thật thì hộp thoại vẫn hiện; Thử lại sau khi mạng có lại thì hộp thoại đóng (KB-2) | Kiểm tra thủ công (emulator: `app/src/deviceScenarios/T-076-co-mang-khong-hien-hop-thoai.txt`) | Tệp kịch bản dưới; `device-session` cuối phiên chạy nó trên emulator API 34. Đạt: mọi bước `ok`, ảnh `online.png` và `back-online.png` không có hộp thoại, `offline.png` có hộp thoại |
| Wi-Fi có cổng đăng nhập hay Wi-Fi không ra Internet thì hộp thoại hiện sau 3 giây | Kiểm tra thủ công (máy thật: khác, emulator CI không giả được mạng có cổng đăng nhập hay mạng mất xác nhận) | Dev kiểm bản staging: điện thoại nối Wi-Fi của router đã rút dây WAN (hoặc điểm phát của máy khác đã tắt dữ liệu di động), tắt dữ liệu di động của điện thoại, mở app qua màn loading khi còn mạng rồi chuyển sang Wi-Fi đó; hộp thoại hiện sau khoảng 3 giây (khoảng thời gian hệ thống cần để thấy mạng không xác nhận được có thể thêm vài giây). Bật lại WAN, bấm "Thử lại": hộp thoại đóng |
| Máy thật xác nhận mạng bình thường không hiện oan | Kiểm tra thủ công (máy thật: khác, ticket đòi máy thật ngoài emulator CI) | Dev dùng bản staging trên máy thật vài phút ở Wi-Fi nhà và 4G, chuyển qua lại Wi-Fi và 4G; hộp thoại không hiện |

Tệp kịch bản (đã chạy `scripts/device-scenario.py check`, mục 4.1). `back` từ chi tiết về thư viện và `idp=poses_card_ in=poses_grid` theo `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`:

```text
ticket: T-076
criterion: Mạng đã xác nhận thì hộp thoại mất mạng không hiện oan, kể cả ngay sau khi mạng có lại; mất mạng thật thì vẫn hiện và Thử lại đóng được (plan T-076, mục 4)
timeout: 180
fresh
consent decline 30
wait id=onboarding_screen | id=camera_screen 60
tap_if id=onboarding_enable_camera
need id=camera_poses 15
# (1) online: past the loading screen for longer than the 3 s delay, the dialog never shows
expect_no id=network_dialog
tap id=camera_poses
wait id=poses_grid 20
expect_no id=network_dialog
tap idp=poses_card_ in=poses_grid index=1
wait id=pose_detail_screen 15
expect_no id=network_dialog
back
wait id=poses_grid 10
expect_no id=network_dialog
shot online
# (2) real loss still shows the dialog
network off
wait id=network_dialog 30
shot offline
# (3) network back: Retry closes the dialog once the network is validated, and it does not come back
network on
tap id=network_dialog_retry
wait id=poses_grid 30
expect_no id=network_dialog
tap idp=poses_card_ in=poses_grid index=1
wait id=pose_detail_screen 15
expect_no id=network_dialog
back
wait id=poses_grid 10
expect_no id=network_dialog
shot back-online
```

Ngân sách `device-session` (`docs/ops/test-sessions.md` mục 5, Hạn chót): sáu tệp có sẵn có `timeout` 150, 180, 120, 120, 180, 240, cộng 990 giây, thêm 80 giây mỗi tệp là 1470; tệp mới (`timeout: 180`, T-022 chạy phần tương tự trong khoảng 100 giây ở TS-20261029-01) đưa tổng lên 1470 + 180 + 80 = 1730, quá mặc định 1500. Rút `timeout` không đủ (kể cả 30 giây vẫn là 1580), nên plan nâng mặc định lên 1800 ở `scripts/device-scenario.py` và `scripts/device-session.sh`, và sửa số trong `docs/ops/test-sessions.md` (mục 3.2). `device-session.yml` có `timeout-minutes: 50` và mỗi lần chạy khoảng 25 phút, nên thêm 300 giây vẫn vừa; Nơi khác đọc ba file này (L-020, đã `grep -n "BUDGET\|1500"`) không bị ảnh hưởng: `.github/workflows/device-session.yml` chỉ gọi `scripts/device-session.sh` và dẫn `docs/ops/test-sessions.md`, không đặt `DEVICE_SCENARIOS_BUDGET` hay số 1500; `scripts/tests/run.sh` gọi hai script với `DEVICE_SCENARIOS_BUDGET` đặt rõ (0, 6, 9) và dẫn tài liệu, không dựa vào mặc định; `scripts/tests/fake-adb` và `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ui/UiAutomatorTagsTest.kt` chỉ nhắc tên `scripts/device-scenario.py` (cách chạy, lệnh `check`), không đọc ngân sách.

**Ngoại lệ viết test trước:** KDoc của `InternetCheck.kt` (chỉ chú thích); `build.gradle.kts` và `robolectric.properties` (cần để test viết trước chạy được, đi cùng commit test của bước 3); tệp kịch bản emulator (chạy ở `device-session` cuối phiên, không "thất bại trước" được). Không có phần code nào khác nằm ngoài test viết trước.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-29, trên worktree tạm từ nhánh chính `57998e3`, không commit, đã xóa sau khi chạy: thêm `testImplementation` và `robolectric.properties` của mục 3.2, test tạm gồm hai test đầu của mục 4 (`isConnected_needsInternetAndValidated`, `connectivity_waitsForValidation_thenReportsLoss_andUnregisters`), rồi diff của mục 3.1. Lần chạy thứ hai (dưới) có đủ bốn test của mục 4; hai test thêm chỉ chạy trên code mới.
- **Lệnh:** `./gradlew :core:network:testDebugUnitTest --tests '*AndroidConnectivitySourceTest*'` trên code hiện tại; `./gradlew :core:network:testDebugUnitTest` sau khi đổi code; `scripts/check.sh` sau khi đổi code; `scripts/device-scenario.py check app/src/deviceScenarios/T-076-co-mang-khong-hien-hop-thoai.txt`.
- **Kết quả:**
  - Lần đầu thiếu `robolectric.properties`: `AndroidConnectivitySourceTest > initializationError FAILED`, `IllegalArgumentException at DefaultSdkPicker.java:119` (Robolectric 4.15.1 không có SDK 36). Thêm `sdk=35` thì chạy được; vì vậy tệp này có ở mục 3.2.
  - Code hiện tại: 2 test, 2 thất bại đúng lý do: `isConnected_needsInternetAndValidated` ở dòng `assertFalse(source.isConnected())` với chỉ `INTERNET` (code cũ trả `true`); `connectivity_...` ở câu giá trị đầu `assertFalse(awaitItem())` (code cũ phát `true`).
  - Sau diff của mục 3.1: `BUILD SUCCESSFUL`; `:core:network` 32 test, 0 thất bại (`AndroidConnectivitySourceTest` 2, `NetworkDialogControllerTest` 24, `NetworkErrorsTest` 3, `NetworkSettingsTest` 2, `DefaultInternetConnectivityTest` 1).
  - `scripts/check.sh` (lần 1, cùng ngày): chưa chạy xong, đã dừng vì hết thời gian. Lần 2, trên worktree tạm khác từ `57998e3` có diff của mục 3.1, `build.gradle.kts`, `robolectric.properties` và đủ bốn test của mục 4 (cả `internetConnectivity_followsValidation` và `hasUsableInternet_noCapabilities_isFalse`): mã 0, `BUILD SUCCESSFUL in 5m 3s`, "Test JVM: 2377 test, 0 lỗi. Ảnh screenshot: 138", `:core:network` 34 test, 0 lỗi, "Cổng chất lượng: QUA". Trước đó `./gradlew :core:network:testDebugUnitTest` trên worktree này: `AndroidConnectivitySourceTest` 4 test, 0 lỗi.
  - `scripts/device-scenario.py check` tệp kịch bản: mã 0.
  - Emulator CI (bằng chứng có sẵn, không chạy mới): artifact `device-session` của lần chạy `40905234848` (TS-20261029-01, 2026-10-29). `logcat.txt`: mạng Wi-Fi của emulator có `VALIDATED` từ đầu (`[100 WIFI] validation passed` lúc 15:59:58, trước khi app chạy; dòng `GnssNetworkConnectivityHandler` liệt kê `INTERNET&...&VALIDATED`). Trong T-022, `network on` lúc 16:08:03.925 đổi mạng mặc định sang di động, `[104 CELLULAR] validation passed` lúc 16:08:04.067 (khoảng 0,14 giây); Wi-Fi được xác nhận (16:08:07.166) trước khi thành mạng mặc định (16:08:07.167). Sau kịch bản mất mạng lúc mở app: di động được xác nhận lúc 16:01:20.481, 0,13 giây sau khi thành mặc định (16:01:20.351). Hộp thoại hiện 3 giây sau khi mất mạng (`Lost default Internet network: 103` 16:07:56.288, `VGL.Network: Dialog shown: device_offline delay=3s` 16:07:59.286). Nên trên emulator CI, đòi `VALIDATED` không làm hộp thoại hiện oan; không cần quay về hướng (b). Tệp kịch bản của task xác nhận lại trên code mới sau merge (mục 3.4).
  - Máy thật: chưa kiểm (không có máy nối với máy dev lúc viết plan); dòng máy thật ở mục 4.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (module core dùng chung `:core:network`, cấu hình build của module đó). Xếp Cao, không tính tổng. Điểm từng tiêu chí để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một file code trong `:core:network`, không đổi giao diện công khai (`ConnectivitySource` là `internal`), cộng một chú thích ở `:feature:loading` và tệp build, test |
| B. Độ rõ của yêu cầu | 1 | Ticket đã chốt (a); ADR-0024 điều 1 nói rõ. Spec network đang `changed` chờ Nhật xác nhận lại sau T-074 (câu 1 ở mục 8), không ảnh hưởng cách làm |
| C. Khả năng kiểm chứng tự động | 1 | Định nghĩa có test Robolectric; hộp thoại không hiện oan có kịch bản emulator chạy sau merge; Wi-Fi có cổng đăng nhập và máy thật xác nhận chậm cần máy thật |
| D. Ảnh hưởng chéo | 3 | Đổi hành vi mà vỏ app (hộp thoại trên mọi màn) và khung quảng cáo (`InternetConnectivity`, nạp quảng cáo) đang dựa vào |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu đã có: `InternetCheck` của `:feature:loading` đã đọc `VALIDATED` bằng `ShadowNetworkCapabilities`; `ShadowConnectivityManager.networkCallbacks` mới ở app nhưng có trong test của mẫu, đã chạy thử |
| **Tổng** | 7 (tham khảo) | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `ConnectivitySource` của `:core:network`, nguồn duy nhất của `NetworkDialogController` (hộp thoại trên mọi màn sau loading) và `InternetConnectivity` (khung quảng cáo `:core:ads`: nhãn `offline` của số liệu, lý do không có quảng cáo thưởng, nạp sẵn khi mạng trở lại S-24, hỏi lại đồng ý S-09; mục 3.1). `core/network/build.gradle.kts`. Ngân sách kịch bản emulator (`scripts/device-scenario.py`, `scripts/device-session.sh`, `docs/ops/test-sessions.md`), dùng chung cho mọi tệp kịch bản.
- Task khác có thể xung đột: T-077 (`docs/features/network/plans/T-077-popup-khi-quay-lai-app.md`, đang soạn) sửa `NetworkDialogController`; không trùng file với T-076 và không thêm test Robolectric ở `:core:network`. Ràng buộc thật: vì T-076 đòi `VALIDATED`, T-077 cho một khoảng chờ 2 giây khi app trở lại trước (bằng `RETRY_MAX_MILLIS`) để mạng kịp được xác nhận lại. T-074 đã merge (`cee00a4`, `aa09dce`, `5c0321a`); plan T-074 mục 6 ghi T-076 chạy sau T-074 thì không xung đột file và test của T-076 kiểm cả `InternetConnectivity` (dòng thứ ba của mục 4).
- Đề xuất thứ tự chạy: T-076 trước T-077, vì khoảng chờ của T-077 dựa vào định nghĩa `VALIDATED`; hai task không trùng file nên T-076 dừng thì T-077 vẫn chạy được về mặt code.

## 7. Điều kiện dừng riêng

- Test có sẵn của `:core:network`, `:core:ads` hay `:app` đỏ vì định nghĩa mới (ví dụ một test dựng nguồn thật và mong đợi có mạng khi chỉ có `INTERNET`): dừng, vì plan đã khẳng định ở mục 3.5 là không có test như vậy; không viết lại câu khẳng định đó theo ADR-0031 mà báo người.
- Cần đổi `NetworkDialogController` (thời gian chờ, luật thử lại) để tránh hộp thoại hiện oan: dừng, đó là đổi spec (KB-1, câu hỏi 2 và 3).

## 8. Câu hỏi cho người duyệt

1. **Spec network đang `changed`** (dòng lịch sử 2026-10-29 sau T-074, mục 5 quảng cáo), chờ Nhật xác nhận lại. Ticket còn đòi spec ghi rõ định nghĩa đã chốt ở KB-1 ("Hướng đã chốt ghi trong spec network"). Plan không sửa spec, và phiên tự chạy không làm task khi spec chưa chốt.
   - Đề xuất của AI: trong lần xác nhận lại spec (PR tài liệu, trước khi duyệt plan này), thêm vào KB-1 tiêu chí 1 câu: "Máy báo không còn kết nối nghĩa là mạng mặc định của máy không có Internet đã được hệ thống xác nhận (`NET_CAPABILITY_INTERNET` và `NET_CAPABILITY_VALIDATED`, ADR-0024 điều 1); Wi-Fi có cổng đăng nhập hay không ra được Internet là không có mạng.", một dòng lịch sử, rồi Nhật xác nhận (`status: confirmed`). Plan duyệt sau đó. Mục 1 của Định nghĩa hoàn thành trong ticket ("Hướng đã chốt ghi trong spec network") phụ thuộc PR tài liệu đó merge trước; phiên tự chạy không làm mục này.
2. **Kiểm trên emulator chỉ có sau merge.** Cloud không có emulator; `device-session.yml` chỉ chạy trên nhánh chính. Plan dựa vào log có sẵn của TS-20261029-01 (xác nhận trong khoảng 0,14 giây) để chọn (a), và tệp kịch bản cuối phiên để xác nhận lại. Hộp thoại hiện oan trên staging thì người quyết revert hay giữ (mục 3.4).
   - Đề xuất của AI: chấp nhận; nếu muốn chắc hơn, sau khi PR của task merge, dev chạy tay **Actions > device-session** trước khi phát staging (khoảng 25 phút).
3. **Máy thật xác nhận chậm hay không bao giờ** (lý do của plan T-011 câu 1) chưa được đo trên máy nào của nhóm.
   - Đề xuất của AI: dev kiểm bản staging trên một hai máy thật theo hai dòng máy thật của mục 4; thấy hộp thoại hiện oan thì ghi phản hồi và soạn plan phiên bản 2 theo hướng (b).
4. **Mạng chặn bước xác nhận của Android** (tường lửa công ty, một số vùng chặn `connectivitycheck.gstatic.com`): app tải được nội dung nhưng hộp thoại hiện trên mọi màn sau 3 giây và "Thử lại" không đóng được (mục 3.4). Có chấp nhận đánh đổi này của hướng (a) không?
   - Đề xuất của AI: chấp nhận, như Nhật đã chọn (a) theo ADR-0024. Cần gạt khi có phản hồi: tắt `feature_network_dialog_enabled` trên Remote Config (KB-4); khung quảng cáo vẫn coi là mất mạng. Nhiều phản hồi như vậy thì soạn plan theo hướng (b).
## 9. Quyết định duyệt

- **Quyết định:** Duyệt có điều kiện: chờ Tuấn duyệt chéo (rủi ro cao, điểm chặn 7). Plan giữ `draft` tới khi có duyệt chéo.
- **Người duyệt:** Nhật, **duyệt chéo:** chưa có (Tuấn)
- **Ghi chú:** Nhật trả lời ngày 2026-10-29, khi trả lời câu hỏi cuối phiên RUN-20261029-01 (lời của Nhật: chọn "Đồng ý cả ba đề xuất" cho câu 2 tới 4, "Nhật duyệt, chờ Tuấn duyệt chéo" cho cách duyệt).
  - Câu 1: Nhật thêm đúng câu đề xuất vào KB-1 tiêu chí 1 và xác nhận lại spec network (PR tài liệu nhánh `claude/RUN-20261029-01-xac-nhan`; PR đó merge trước khi duyệt plan).
  - Câu 2: chấp nhận kiểm trên emulator sau merge, ở `device-session` cuối phiên.
  - Câu 3: dev thử bản staging trên một hai máy thật theo hai dòng máy thật của mục 4; hộp thoại hiện oan thì ghi phản hồi và soạn plan phiên bản 2 theo hướng (b).
  - Câu 4: chấp nhận đánh đổi của hướng (a); cần gạt khi có phản hồi là tắt `feature_network_dialog_enabled` trên Remote Config.

## 10. Nhật ký thực hiện
