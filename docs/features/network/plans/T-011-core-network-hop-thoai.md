---
ticket: T-011
title: Nhận ra mất mạng và hộp thoại mất mạng
status: done
version: 2
risk_score:
risk_level: high
token_estimate: 40M
blocking_points: [permission, core]
run_order:
reviewer: Nhật
cross_reviewer: Tuấn
approved_at: 2026-10-22
---

# PLAN T-011: NHẬN RA MẤT MẠNG VÀ HỘP THOẠI MẤT MẠNG

## 1. Tóm tắt

Tạo module core `:core:network` theo dõi trạng thái mạng của máy (`ConnectivityManager`) và nhận báo "tải thất bại vì mạng" từ `:core:assets`. Một bộ điều khiển dùng chung (`NetworkDialogController`) quyết định khi nào hiện hộp thoại: mất mạng quá 3 giây (giá trị trong `RemoteSettings`) hoặc ngay khi một lần tải thất bại vì mạng, chỉ sau màn loading, chỉ khi cờ `feature_network_dialog_enabled` bật. Hộp thoại `NetworkDialogScreen` là thành phần của `:core:designsystem`; vỏ app (`:app`) vẽ nó phủ lên mọi màn, chặn thao tác phía sau, nút quay lại đưa app ra nền. Kiểm chứng bằng unit test với thời gian ảo, test giao diện trên JVM, screenshot test cùng tên hai ảnh thiết kế và ảnh so sánh thiết kế.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/network/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Máy báo mất kết nối quá thời gian chờ thì hộp thoại hiện, đúng chữ và thiết kế | Thời gian chờ đọc từ `RemoteSettings`, khóa `network_lost_delay_seconds`, mặc định 3 (spec, câu hỏi 2). "Có kết nối" nghĩa là mạng mặc định của máy có `NET_CAPABILITY_INTERNET` (câu hỏi 1 ở mục 8) |
| KB-1 | Lần tải thất bại vì mạng thì hộp thoại hiện, dù máy chưa báo mất kết nối | Hiện ngay, không chờ. Nguồn báo trong ticket này là `:core:assets` (catalog, ảnh tư thế đều tải qua `RemoteAssets`). "Vì mạng" là lỗi `UnknownHostException`, `SocketException` (gồm `ConnectException`, `NoRouteToHostException`), `SocketTimeoutException`, kể cả khi nằm trong chuỗi `cause`. Lỗi HTTP (ví dụ 404) không tính |
| KB-1 | Đang có hộp thoại thì không bấm được gì phía sau; chạm ra ngoài không đóng | Lớp mờ phủ cả màn hình, nhận hết thao tác chạm; nội dung phía sau bị ẩn khỏi trình đọc màn hình (`hideFromAccessibility`) để TalkBack không bấm vòng qua được |
| KB-1 | Chỉ một hộp thoại, kể cả khi nhiều lần tải cùng thất bại | Trạng thái là một `StateFlow` duy nhất; báo lỗi khi đang hiện không đổi gì |
| KB-1, đặc biệt | Đang đếm ngược ở camera thì dừng | Ngoài phạm vi (ticket, mục Không bao gồm). Ticket này cung cấp `NetworkStatus.dialogShown` để T-013 dùng |
| KB-1, đặc biệt | Đang lưu ảnh thì lưu tới xong, hộp thoại hiện sau | Hộp thoại chỉ là lớp vẽ phủ, không hủy coroutine của màn hình, nên việc lưu vẫn chạy. Việc giữ hộp thoại tới khi lưu xong là của T-016 (câu hỏi 4 ở mục 8) |
| KB-1, đặc biệt | Mạng mất rồi có lại trong thời gian chờ thì không hiện | Bộ đếm thời gian chờ bị hủy khi có mạng lại |
| KB-2 | Bấm "Thử lại" khi đã có mạng thì hộp thoại đóng, màn hình tải lại phần đã thất bại | Kiểm tra lại `ConnectivityManager`. Có mạng thì đóng và phát một sự kiện `NetworkStatus.recovered`; màn hình nào có phần tải dở thì nghe sự kiện này và tải lại (làm ở ticket của màn hình đó) |
| KB-2 | Bấm "Thử lại" khi chưa có mạng thì nút cho thấy đang kiểm tra rồi trở lại | Nút hiện vòng chờ, kiểm tra lại mỗi 250 ms, tối đa 2 giây (spec, câu hỏi 3). Hết 2 giây vẫn chưa có mạng thì nút trở lại và dòng "Vẫn chưa có mạng" hiện dưới nút. Có mạng trong lúc chờ thì đóng ngay |
| KB-2 | Mạng có lại mà chưa bấm "Thử lại" thì hộp thoại vẫn hiện | Hộp thoại chỉ đóng qua "Thử lại", khi cờ tắt, hoặc khi vỏ app quay về màn loading |
| KB-3 | Nút quay lại đưa app ra nền; mở lại mà chưa có mạng thì hộp thoại hiện lại | `BackHandler` của vỏ app gọi `moveTaskToBack(true)`. Trạng thái nằm trong bộ điều khiển `@Singleton`, nên mở lại app thì hộp thoại vẫn còn |
| KB-4 | Cờ tắt thì không hiện hộp thoại | `FeatureFlags.enabled(NETWORK_DIALOG)` được theo dõi liên tục (ADR-0003); cờ tắt khi đang hiện thì hộp thoại đóng. Màn hình tự báo lỗi theo cách riêng khi cờ tắt (ticket của từng màn) |
| Spec, mục 2 | Chỉ ở các màn sau màn loading | Vỏ app báo cho bộ điều khiển biết đích hiện tại có phải `LoadingDestination` không. Khi đang ở loading, bộ điều khiển không theo dõi mạng và bỏ qua mọi báo lỗi (không giữ lại để hiện sau) |
| Spec, mục 5 | Khi hộp thoại đang hiện thì không hiện quảng cáo toàn màn hình | App chưa có vị trí toàn màn hình nào. Ticket này cung cấp `NetworkStatus.dialogShown`; T-018 đọc nó khi thêm `review_interstitial` (câu hỏi 3 ở mục 8) |
| Spec, mục 6 | Không có sự kiện phân tích | Không chạm `:core:analytics` |

Thiết kế tham chiếu: `features/network/design/README.md`; ảnh trong `design/screens/`: `NetworkDialogScreen_default_light.png`, `NetworkDialogScreen_default_dark.png`; số liệu trong `design/context/` cùng tên. Hệ thống thiết kế: `docs/design-system/sticker.md`.

ADR liên quan: ADR-0001 (core chỉ phụ thuộc core), ADR-0003 (cờ và `RemoteSettings`), ADR-0008 (màu, chữ từ `:core:designsystem`), ADR-0012, ADR-0015 (`:core:assets`), ADR-0018 (màn loading có cách báo lỗi riêng), ADR-0019 (ghi log qua `AppLogger`), ADR-0020 (tên screenshot test trùng tên ảnh thiết kế).

**Giả định về T-007.** Ticket phụ thuộc T-007 (hệ thống thiết kế Sticker, plan đang viết song song). Plan này chỉ dùng tên vai trò trong `sticker.md` và giả định T-007 đã merge với: vai trò màu Material 3 theo mục 1 (`surface`, `onSurface`, `onSurfaceVariant`, `outline`, `secondary`, `onSecondary`, `secondaryContainer`, `onSecondaryContainer`); màu riêng `camera/scrim`; kiểu chữ theo mục 2 (`titleLarge` đậm 800, `bodyMedium`, `bodySmall`, `titleMedium`); giá trị bo góc `sheet` (24), viền `sticker` (2 dp); `PrimaryButton` đã đổi kiểu (cao 56, bo `button`, viền 2, bóng `shadow/sticker`). Cách T-007 đặt tên các giá trị này trong code (ví dụ một `CompositionLocal` cho màu riêng) lấy đúng như T-007 đã merge, không đoán trước. Thiếu một trong các giá trị trên thì dừng (mục 7).

## 3. Giải pháp

### 3.1. Cách làm

Cách tiếp tục: tiếp tục trên nhánh `claude/T-011-core-network-hop-thoai` (RUN-20261021-03: code và 42 trên 43 test đã qua, chưa có PR). Phiên bản 2 chỉ đổi `NetworkDialogHost` sang cửa sổ `Dialog` riêng (hướng (b) người duyệt chọn) cùng các test của nó; mọi phần khác giữ nguyên. Bước 3 làm lại cho `NetworkDialogHostTest`.

**Hướng phụ thuộc giữa module (ADR-0001).**

```
:app ──> :core:network ──> :core:config, :core:logging
:app ──> :core:designsystem            (NetworkDialogScreen)
:core:assets ──> :core:network          (báo lỗi tải vì mạng)
```

`:core:network` không phụ thuộc `:core:assets` hay `:core:designsystem`, nên không có vòng. `:core:assets` gọi xuống `:core:network` qua interface `NetworkFailureReporter`, đúng chiều "nơi phát hiện lỗi báo cho nơi quyết định". `:core:poses` (T-009) tải mọi file qua `RemoteAssets`, nên lỗi mạng của catalog và ảnh tư thế tự đi qua `:core:assets`, không cần nối riêng. `:core:network` không có Compose, không có giao diện.

**`:core:network`, phần công khai.**

- `NetworkFailureReporter` (`fun interface`): `reportNetworkFailure(source: String)`. `source` là nhãn ngắn của module báo (ví dụ `"assets"`), chỉ để ghi log, không chứa đường dẫn hay dữ liệu người dùng.
- `Throwable.isNetworkFailure(): Boolean` trong `NetworkErrors.kt`: đúng khi lỗi, hoặc một `cause` trong chuỗi, là `UnknownHostException`, `SocketException` hay `SocketTimeoutException`. Để `:core:assets` và module khác dùng chung một cách phân loại.
- `NetworkStatus` (interface cho feature và module core khác): `val dialogShown: StateFlow<Boolean>` (T-013 dừng đếm ngược, T-018 bỏ quảng cáo toàn màn hình), `val recovered: Flow<Unit>` (phát một lần mỗi khi hộp thoại đóng nhờ "Thử lại"; màn hình tải lại phần dở).
- `NetworkDialogState`: `sealed interface` gồm `Hidden` và `Shown(checking: Boolean, stillOffline: Boolean)`.
- `NetworkDialogController` (`@Singleton`, `@Inject internal constructor`, theo mẫu `AdsController`): cài `NetworkStatus` và `NetworkFailureReporter`; thêm cho `:app`: `val state: StateFlow<NetworkDialogState>`, `fun setActive(active: Boolean)`, `fun retry()`.

**`:core:network`, phần nội bộ.**

- `ConnectivitySource` (internal interface): `fun isConnected(): Boolean`, `fun connectivity(): Flow<Boolean>`. Cài đặt `AndroidConnectivitySource(context)`: `callbackFlow` với `ConnectivityManager.registerDefaultNetworkCallback` (minSdk 26 có sẵn), phát `true` khi mạng mặc định có `NET_CAPABILITY_INTERNET` (`onAvailable`, `onCapabilitiesChanged`), `false` khi `onLost`; giá trị đầu là `isConnected()`; gỡ callback trong `awaitClose`; `distinctUntilChanged()`. Lớp này mỏng, không có logic quyết định.
- `NetworkSettings` (internal object, theo mẫu `AdsSettings`): `LOST_DELAY_SECONDS = "network_lost_delay_seconds"`, `LOST_DELAY_SECONDS_DEFAULT = 3L`; hằng số trong code (không đưa lên Remote Config vì spec không yêu cầu): `RETRY_MAX_MILLIS = 2_000L`, `RETRY_POLL_MILLIS = 250L`.
- `di/NetworkModule.kt`: `@Binds` `NetworkStatus` và `NetworkFailureReporter` vào `NetworkDialogController`; `@Provides` `ConnectivitySource` (`AndroidConnectivitySource` từ `@ApplicationContext`); `@Provides @NetworkScope CoroutineScope` = `CoroutineScope(SupervisorJob() + Dispatchers.Default)`. `NetworkScope` là qualifier internal.

**Luồng quyết định trong `NetworkDialogController`.** Nhận `ConnectivitySource`, `FeatureFlags`, `RemoteSettings`, `AppLogger`, scope. Test truyền `backgroundScope` của `runTest` để dùng thời gian ảo.

1. `active` (`MutableStateFlow<Boolean>`, mặc định `false`) do `:app` đặt; `enabled` = `featureFlags.enabled(FeatureFlag.NETWORK_DIALOG)`, theo dõi liên tục. `armed = combine(active, enabled) { a, e -> a && e }`.
2. Khi `armed` chuyển `false`: trạng thái về `Hidden`, ngừng theo dõi mạng (gỡ callback). Khi `armed` là `true`: `connectivity()` được thu bằng `collectLatest`; gặp `false` thì `delay(giây chờ đọc từ RemoteSettings * 1000)` rồi đặt `Shown(checking = false, stillOffline = false)` nếu đang `Hidden`; gặp `true` trước khi hết thời gian thì lần chờ bị hủy (mạng chập chờn không hiện gì). Chỉ đăng ký callback của `ConnectivityManager` khi đã qua loading.
3. `reportNetworkFailure(source)`: nếu `armed` hiện là `true` và trạng thái đang `Hidden` thì đặt `Shown` ngay. Ngược lại không làm gì; báo lỗi không được giữ lại để hiện sau (lỗi lúc ở màn loading bị bỏ qua). Hàm này có thể được gọi từ luồng IO; cập nhật bằng `MutableStateFlow.update`.
4. Đang `Shown` thì có mạng lại cũng không đóng (KB-2, tiêu chí 3).
5. `retry()`: bỏ qua nếu đang `checking`. Đặt `Shown(checking = true, stillOffline = false)`, rồi trong scope: kiểm tra `isConnected()` ngay và sau mỗi 250 ms, tối đa 2 giây. Có mạng thì đặt `Hidden` và phát `recovered` (`MutableSharedFlow<Unit>(extraBufferCapacity = 1)`). Hết 2 giây thì đặt `Shown(checking = false, stillOffline = true)`.
6. Hộp thoại hiện vì lỗi tải trong khi máy vẫn báo có mạng: "Thử lại" đóng ngay, màn hình tải lại; nếu lại lỗi thì hộp thoại hiện lại ngay. Đây là hành vi chấp nhận được, người dùng luôn biết chuyện gì xảy ra.
7. `dialogShown` = `state.map { it is Shown }.stateIn(scope, Eagerly, false)`.
8. Log qua `AppLogger`, thẻ `Network` (ADR-0019): đổi `active`; hộp thoại hiện (lý do `device_offline` kèm số giây chờ, hoặc `load_failed` kèm `source`); kết quả "Thử lại" (`online`, `still_offline`); cờ tắt làm đóng hộp thoại. Không ghi tên mạng, địa chỉ hay đường dẫn file.

**`:core:assets`.** `DefaultRemoteAssets` thêm tham số cuối `networkFailures: NetworkFailureReporter? = null`. Trong `catch (e: IOException)`, sau `onDownloadFailed`, nếu `e.isNetworkFailure()` thì gọi `networkFailures?.reportNetworkFailure("assets")`. File đã có trên máy thì không tải, không báo. `AssetsModule` nhận `NetworkFailureReporter` qua Hilt và truyền vào. Giao diện công khai `RemoteAssets`, `AssetResult` không đổi, nên `:feature:loading` và test của nó không bị ảnh hưởng. Phân biệt lý do lỗi trong `AssetResult` (cho `content_load_failed`, `reason`) để T-009 quyết định (câu hỏi 5 ở mục 8).

**`:core:designsystem`.**

- `component/NetworkDialog.kt`:
  - `NetworkDialog(checking, stillOffline, onRetry, modifier)`: thẻ rộng tối đa 331 dp (`widthIn(max = 331.dp)`, số liệu `context/`), nền `surface`, viền `outline` dày theo giá trị viền `sticker`, bo `sheet`, lề trong `Spacing.lg`, các phần cách nhau `Spacing.md`, căn giữa. Trong thẻ, theo thứ tự thiết kế: vòng 64 dp nền `secondaryContainer` viền `outline` chứa icon `wifi_off` 32 dp màu `onSecondaryContainer`; tiêu đề `titleLarge` màu `onSurface`; lời hướng dẫn `bodyMedium` màu `onSurfaceVariant`; `PrimaryButton` rộng hết thẻ, icon `refresh`, chữ "Thử lại", `loading = checking`; khi `stillOffline` thì dòng "Vẫn chưa có mạng" `bodySmall` màu `onSurfaceVariant`, căn giữa, dưới nút. Icon là trang trí, không có `contentDescription`. Thẻ mang `paneTitle` là tiêu đề, để TalkBack đọc khi hộp thoại mở.
  - `NetworkDialogScreen(checking, stillOffline, onRetry, modifier)`: `Box` phủ cả màn hình, nền `camera/scrim` độ mờ 50%, nhận mọi thao tác chạm (`pointerInput` tiêu thụ sự kiện, không đóng gì), `NetworkDialog` ở giữa, cách mép ngang ít nhất `Spacing.md` để màn 360 dp không bị cắt, có `verticalScroll` khi màn quá thấp. Đây là composable mà screenshot test chụp (tên ảnh thiết kế).
  - `NetworkDialogTestTags`: `SCREEN = "network_dialog"`, `CARD`, `RETRY`, `SPINNER`, `STILL_OFFLINE`.
- `PrimaryButton` thêm tham số `loading: Boolean = false`: khi `true`, vòng chờ 20 dp (`CircularProgressIndicator`, màu nội dung của nút, tag `PrimaryButtonTestTags.SPINNER`) thay chỗ icon, chữ giữ nguyên, nút giữ màu như đang bật nhưng bấm không gọi `onClick`. Mọi nơi gọi cũ không đổi. Nếu `PrimaryButton` của T-007 đã có trạng thái đang chờ thì dùng nó, không thêm.
- Icon vector `ic_wifi_off.xml`, `ic_refresh.xml` lấy từ Material Symbols Rounded (Apache 2.0, `sticker.md` mục 2), kèm file giấy phép theo mẫu `third_party/inter/LICENSE.txt`. Nếu T-007 đã thêm bộ icon này với tên khác thì dùng của T-007, không tạo trùng.
- Chữ trong `res/values/strings.xml` của `:core:designsystem`: `network_dialog_title`, `network_dialog_message`, `network_dialog_retry`, `network_dialog_still_offline`, đúng bảng chữ của thiết kế.

**`:app`.**

- `network/NetworkDialogHost.kt` (phiên bản 2): `NetworkDialogHost(state: NetworkDialogState, onRetry, onBack, modifier, darkTheme: Boolean = isSystemInDarkTheme(), content)`. Vẽ `content` như thường, không thêm thuộc tính ngữ nghĩa nào. Khi `state is Shown`: vẽ `Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false))`, trong đó là `AppTheme(darkTheme) { NetworkDialogScreen(...) }` phủ kín cửa sổ hộp thoại và `BackHandler { onBack() }`. Cửa sổ hộp thoại riêng là cửa sổ trên cùng và phủ kín màn hình, nên hệ thống không đưa chạm và trình đọc màn hình xuống cửa sổ app phía sau. Phím quay lại đi vào bộ điều phối phím quay lại của chính cửa sổ hộp thoại, nên `BackHandler` trong hộp thoại nhận phím, `NavHost` phía sau không điều hướng. Phiên bản 1 dùng một cửa sổ và `Modifier.semantics { hideFromAccessibility() }` trên khung bọc; RUN-20261021-03 cho thấy thuộc tính đó không lan xuống nút con, nên TalkBack vẫn đọc được nút phía sau. Phiên chính đã chạy thử cách này trên nhánh của task (test tạm, không commit): có 2 node gốc, bộ điều phối phím quay lại của hộp thoại khác của activity, gọi nó thì `BackHandler` trong hộp thoại chạy một lần, `NavHost` vẫn ở đích thứ hai, hộp thoại vẫn còn. `darkTheme` để T-013, T-016 bắt hộp thoại tối trên camera và màn xem lại (README thiết kế, mục 1).
- `AppNavHost` thêm tham số `onPastLoadingChanged: (Boolean) -> Unit = {}`: theo dõi `navController.currentBackStackEntryAsState()`, gọi với `true` khi đích hiện tại tồn tại và không phải `LoadingDestination` (`destination.hasRoute<LoadingDestination>()`), trong `LaunchedEffect`. Cách này đúng cả khi điều hướng được khôi phục sau khi hệ thống tạo lại activity.
- `MainActivity`: `@Inject lateinit var networkDialog: NetworkDialogController`; trong `setContent`, `val state by networkDialog.state.collectAsState()`, bọc `AppNavHost(onPastLoadingChanged = networkDialog::setActive)` trong `NetworkDialogHost(state, onRetry = networkDialog::retry, onBack = { moveTaskToBack(true) })`.
- `app/build.gradle.kts`: `implementation(project(":core:network"))`.
- Quyền `ACCESS_NETWORK_STATE`: bản build hiện tại đã có quyền này trong manifest gộp, do `play-services-ads-api` 25.5.0 và `play-services-measurement` 23.2.0 khai báo (đã xem manifest của hai thư viện trong bộ nhớ đệm Gradle). Plan vẫn khai báo rõ ở hai nơi: `app/src/main/AndroidManifest.xml` (nhóm thấy mọi quyền ở một chỗ, khớp khai báo Data safety) và `core/network/src/main/AndroidManifest.xml` (module gọi `ConnectivityManager` tự mang quyền, Android Lint không báo `MissingPermission`, và quyền không mất khi bỏ SDK quảng cáo). Trình gộp manifest gộp hai dòng trùng thành một. Tập quyền của app không đổi, nên khai báo Data safety không đổi.
- Cờ: `FeatureFlag.NETWORK_DIALOG("feature_network_dialog_enabled")`, mặc định tắt; bật trên staging trong `StagingFeatureFlagsModule`.

**Cách màn hình tải lại sau "Thử lại".** Màn hình có phần tải dở thu `NetworkStatus.recovered` trong ViewModel và gọi lại hàm tải của mình. Không màn hình nào hiện có cần việc này (`:feature:sample` không tải gì qua mạng, `:feature:loading` nằm ngoài phạm vi). T-021, T-022, T-014 dùng khi làm màn của mình (mục 6).

**Mẫu có sẵn được làm theo:** `AdsController` (lớp `@Singleton` có scope riêng, `:app` tiêm vào `MainActivity`), `AdsSettings` và `AdsSettingsTest` (khóa và giá trị mặc định của `RemoteSettings`), `RemoteConfigRefreshPolicy` (logic tách khỏi Android để test trên JVM), `ComponentsTest` và `DesignSystemCatalogScreenshotTest` (test và ảnh chụp trong `:core:designsystem`), `LoadingScreenTest` (so chữ theo bảng chữ).

**Đưa về dự án mẫu:** không làm (spec, câu hỏi 1). Một điểm nhỏ về quy trình nên đề xuất về dự án mẫu (`docs/ops/from-app.md`), ghi vào mô tả PR: script so sánh thiết kế của ADR-0020 chỉ tìm ảnh trong `feature/<feature>/src/test/screenshots`, chưa hỗ trợ thành phần nằm trong module core (mục 4).

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

Thư mục gói Kotlin viết tắt: `<pkg>` là `com/vglstudio/camera/pose/line/guide`. Bảng dưới ghi đường dẫn đầy đủ.

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `settings.gradle.kts` | Thêm `include(":core:network")` sau `:core:logging` |
| Tạo | `core/network/build.gradle.kts` | Plugin `template.android.library`, `template.hilt`; `namespace` `com.vglstudio.camera.pose.line.guide.core.network`; `implementation` `:core:config`, `:core:logging` |
| Tạo | `core/network/consumer-rules.pro` | File rỗng, như các module core khác (convention plugin đọc file này) |
| Tạo | `core/network/src/main/AndroidManifest.xml` | Khai báo `ACCESS_NETWORK_STATE`, chú thích vì sao |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkFailureReporter.kt` | `fun interface NetworkFailureReporter` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkErrors.kt` | `Throwable.isNetworkFailure()` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkStatus.kt` | Interface `NetworkStatus`: `dialogShown`, `recovered` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogState.kt` | `sealed interface NetworkDialogState`: `Hidden`, `Shown(checking, stillOffline)` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogController.kt` | Bộ điều khiển theo mục 3.1 |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkSettings.kt` | Khóa `network_lost_delay_seconds`, mặc định 3; hằng số 2 000 ms và 250 ms |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/ConnectivitySource.kt` | Interface internal và `AndroidConnectivitySource` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/di/NetworkModule.kt` | Hilt: bind, provide `ConnectivitySource`, scope, qualifier `NetworkScope` |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogControllerTest.kt` | Unit test của bộ điều khiển (mục 4) |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkErrorsTest.kt` | Unit test phân loại lỗi |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkSettingsTest.kt` | Giá trị mặc định là quyết định kinh doanh |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/network/Fakes.kt` | `FakeConnectivitySource`, `FakeFeatureFlags`, `FakeRemoteSettings` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FeatureFlag.kt` | Thêm `NETWORK_DIALOG("feature_network_dialog_enabled")`. `DefaultFeatureFlagsTest` duyệt `FeatureFlag.entries` nên tự kiểm tra mặc định tắt, không phải sửa |
| Sửa | `core/assets/build.gradle.kts` | `implementation(project(":core:network"))` |
| Sửa | `core/assets/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/DefaultRemoteAssets.kt` | Tham số `networkFailures`, báo khi `isNetworkFailure()` |
| Sửa | `core/assets/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/di/AssetsModule.kt` | Nhận `NetworkFailureReporter`, truyền vào `DefaultRemoteAssets` |
| Tạo | `core/assets/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/DefaultRemoteAssetsNetworkFailureTest.kt` | Test báo lỗi mạng. `DefaultRemoteAssetsTest` không sửa: các lời gọi hiện có dùng tham số theo vị trí, tham số mới ở cuối có mặc định |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/NetworkDialog.kt` | `NetworkDialog`, `NetworkDialogScreen`, `NetworkDialogTestTags` |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/PrimaryButton.kt` | Tham số `loading`, `PrimaryButtonTestTags.SPINNER` (sửa trên bản của T-007) |
| Sửa | `core/designsystem/src/main/res/values/strings.xml` | Bốn chuỗi của hộp thoại |
| Tạo | `core/designsystem/src/main/res/drawable/ic_wifi_off.xml` | Icon Material Symbols Rounded `wifi_off` |
| Tạo | `core/designsystem/src/main/res/drawable/ic_refresh.xml` | Icon Material Symbols Rounded `refresh` |
| Tạo | `core/designsystem/third_party/material-symbols/LICENSE.txt` | Giấy phép Apache 2.0 của Material Symbols (bỏ qua nếu T-007 đã thêm) |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/NetworkDialogTest.kt` | Test giao diện trên JVM của hộp thoại |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/PrimaryButtonLoadingTest.kt` | Test trạng thái đang chờ của nút. `ComponentsTest` không sửa |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/NetworkDialogScreenshotTest.kt` | Bốn screenshot test (mục 4) |
| Tạo | `core/designsystem/src/test/screenshots/NetworkDialogScreen_default_light.png`, `NetworkDialogScreen_default_dark.png`, `NetworkDialogScreen_stillOffline_light.png`, `NetworkDialogScreen_stillOffline_small.png` | Ảnh chuẩn mới, do `scripts/check.sh` ghi. Ảnh chuẩn cũ của `:core:designsystem` không đổi: catalog không thêm hộp thoại, `PrimaryButton` mặc định không đổi hình |
| Sửa | `app/build.gradle.kts` | `implementation(project(":core:network"))` |
| Sửa | `app/src/main/AndroidManifest.xml` | `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` dưới chú thích về quyền |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | Tiêm `NetworkDialogController`, bọc `AppNavHost` trong `NetworkDialogHost` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/AppNavHost.kt` | Tham số `onPastLoadingChanged` |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/network/NetworkDialogHost.kt` | Vỏ hiện hộp thoại trong cửa sổ `Dialog` riêng phủ kín màn hình, `BackHandler` trong hộp thoại |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingFeatureFlagsModule.kt` | Bật `FeatureFlag.NETWORK_DIALOG` trên staging |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/network/NetworkDialogHostTest.kt` | Test giao diện trên JVM của vỏ (Robolectric) |
| Tạo | `app/src/test/resources/robolectric.properties` | `sdk=35`, như các module khác |
| Tạo | `docs/features/network/design/review/NetworkDialogScreen_default_light_compare.png`, `NetworkDialogScreen_default_dark_compare.png`, `summary.md` | Ảnh so sánh thiết kế (mục 4), commit cùng PR |

Các file đọc file bị sửa, đã xét (L-020):

- `scripts/check.sh` duyệt `core/*/` để tìm screenshot test: module mới `core/network` không có `captureRoboImage`, bị bỏ qua; ảnh mới của `core/designsystem` được ghi và kiểm bình thường. Không cần sửa.
- `build.gradle.kts` gốc (`checkModuleDependencies`) tự áp dụng cho module mới; mọi phụ thuộc mới là core sang core hoặc `:app` sang core.
- `app/src/androidTest/.../AppLaunchTest.kt` tìm `sample_screen` bằng `fetchSemanticsNodes()`. Nếu emulator CI không có mạng và hộp thoại hiện, màn mẫu nằm ở node gốc thứ nhất, hộp thoại ở node gốc thứ hai; `onAllNodesWithTag` tìm trên mọi node gốc (có test JVM khẳng định nút phía sau vẫn tìm được), nên test không đổi.
- `feature/loading` dùng `RemoteAssets` và `AssetResult` không đổi.
- `scripts/release.sh` nhắc tới `app/build.gradle.kts` vì truyền tham số build production (`-PadmobAppId`, `-PadUnits`, `-PassetsBaseUrl`, khóa ký) mà file đó đọc. Plan chỉ thêm một dòng `implementation(project(":core:network"))`, không đổi tham số, biến thể hay cấu hình ký nào, nên script không bị ảnh hưởng. Plan không đọc, không chạy, không sửa script này (CLAUDE.md, mục Không được làm).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: `feature_network_dialog_enabled` (`FeatureFlag.NETWORK_DIALOG`), mặc định tắt, bật sẵn trên staging. Bộ điều khiển theo dõi cờ như một `Flow`. Tham số kinh doanh mới trong `RemoteSettings`: `network_lost_delay_seconds`, mặc định 3.
- Quảng cáo: Không chạm. Cung cấp `NetworkStatus.dialogShown` để T-018 chặn quảng cáo toàn màn hình.
- Sự kiện phân tích: Không chạm (spec, câu hỏi 5).
- File R2: Không chạm. `:core:assets` chỉ thêm việc báo lỗi mạng, không đổi đường dẫn hay cách lưu.

## 4. Kiểm thử

<!--
Test được viết TRƯỚC code và phải thất bại đúng lý do trước khi viết code (xem auto-run.md).
-->

Unit test của `:core:network` dùng `runTest` với `StandardTestDispatcher`, bộ điều khiển chạy trong `backgroundScope`, thời gian đi bằng `advanceTimeBy`, `runCurrent`. Giả lập: `FakeConnectivitySource` (một `MutableStateFlow<Boolean>` cho cả `connectivity()` và `isConnected()`, đếm số lần đang được thu để kiểm tra việc gỡ callback), `FakeFeatureFlags` (`MutableStateFlow` cho từng cờ), `FakeRemoteSettings` (`MutableStateFlow<Long?>` cho từng khóa), `RecordingAppLogger`. Mặc định trong test: `active = true`, cờ bật, có mạng.

Khung rỗng để test biên dịch được (bước 3): `NetworkDialogController` có `state` luôn `Hidden`, `retry()` và `reportNetworkFailure()` không làm gì; `NetworkDialogScreen` vẽ `Box` rỗng có tag `SCREEN`; `PrimaryButton(loading)` bỏ qua tham số; `NetworkDialogHost` chỉ vẽ `content`; `isNetworkFailure()` trả `false`; `LOST_DELAY_SECONDS_DEFAULT = 0`. Mỗi test dưới đây phải thất bại ở phần khẳng định với khung này.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, mất kết nối quá thời gian chờ | Unit test | `NetworkDialogControllerTest.deviceOffline_shownOnlyAfterDelay`: đặt mất mạng; sau `advanceTimeBy(2_999)` trạng thái `Hidden`; sau thêm 2 ms là `Shown(checking = false, stillOffline = false)` |
| KB-1, thời gian chờ lấy từ `RemoteSettings` | Unit test | `NetworkDialogControllerTest.delay_followsRemoteSetting`: khóa `network_lost_delay_seconds` = 5; lúc 4 999 ms vẫn `Hidden`, lúc 5 001 ms là `Shown` |
| Thời gian chờ mặc định 3 giây | Unit test | `NetworkSettingsTest.lostDelayIs3SecondsByDefault`, `retryWaitsAtMost2Seconds` |
| KB-1, mạng chập chờn dưới thời gian chờ | Unit test | `NetworkDialogControllerTest.flapping_underDelay_showsNothing`: mất mạng 2 s, có lại, mất 2 s, có lại; chạy thêm 10 s; mọi trạng thái đã phát (thu bằng Turbine) chỉ là `Hidden` |
| KB-1, lỗi tải vì mạng hiện ngay | Unit test | `NetworkDialogControllerTest.networkFailure_showsAtOnce`: máy vẫn báo có mạng; `reportNetworkFailure("assets")`, `runCurrent()`, không đẩy thời gian; trạng thái `Shown` |
| KB-1, chỉ một hộp thoại | Unit test | `NetworkDialogControllerTest.manyFailures_singleDialog`: năm lần `reportNetworkFailure` liên tiếp và mất mạng 10 s; Turbine thấy đúng hai giá trị `Hidden`, `Shown`, không có giá trị nào sau đó |
| Spec mục 2, không hiện ở màn loading | Unit test | `NetworkDialogControllerTest.inactive_ignoresOfflineAndFailures`: `active = false`; mất mạng 10 s và báo lỗi; vẫn `Hidden`; `FakeConnectivitySource` không bị thu. Sau đó `setActive(true)` khi có mạng: vẫn `Hidden` (lỗi cũ không được giữ lại) |
| Spec mục 2, rời loading khi đang mất mạng | Unit test | `NetworkDialogControllerTest.activated_whileOffline_showsAfterDelay`: mất mạng khi `active = false`; `setActive(true)`; `Shown` sau 3 s |
| Quay về loading thì đóng | Unit test | `NetworkDialogControllerTest.deactivated_hidesDialog_andStopsWatching`: đang `Shown`; `setActive(false)`; `Hidden`, nguồn mạng không còn bị thu |
| KB-2, mạng có lại mà chưa bấm "Thử lại" | Unit test | `NetworkDialogControllerTest.networkBack_withoutRetry_keepsDialog`: đang `Shown`; có mạng; chạy thêm 10 s; vẫn `Shown` |
| KB-2, "Thử lại" khi đã có mạng | Unit test | `NetworkDialogControllerTest.retry_online_closes_andEmitsRecovered`: đang `Shown`, có mạng; `retry()`, `runCurrent()`; `Hidden`; `recovered` phát đúng một lần |
| KB-2, "Thử lại" khi chưa có mạng | Unit test | `NetworkDialogControllerTest.retry_offline_spinsAtMost2s_thenStillOffline`: `retry()`; ngay sau là `Shown(checking = true, stillOffline = false)`; lúc 1 999 ms vẫn `checking`; lúc 2 001 ms là `Shown(checking = false, stillOffline = true)`; `recovered` không phát |
| KB-2, mạng có lại trong lúc kiểm tra | Unit test | `NetworkDialogControllerTest.retry_networkReturnsWhileChecking_closes`: `retry()` khi mất mạng; lúc 1 000 ms có mạng; trước 1 300 ms đã `Hidden`, `recovered` phát một lần |
| KB-2, bấm "Thử lại" nhiều lần | Unit test | `NetworkDialogControllerTest.retry_whileChecking_isIgnored`: `retry()` hai lần liền khi mất mạng; sau 2 001 ms là `stillOffline = true`, không có lần kiểm tra thứ hai kéo dài trạng thái `checking` |
| KB-2, dòng "Vẫn chưa có mạng" mất khi thử lại | Unit test | `NetworkDialogControllerTest.retryAgain_clearsStillOffline`: từ `stillOffline = true`, `retry()`; trạng thái `checking = true, stillOffline = false` |
| Hiện lại sau khi đã đóng | Unit test | `NetworkDialogControllerTest.afterRecovery_newFailureShowsAgain`, `afterRecovery_offlineAgainShowsAfterDelay` |
| KB-4, cờ tắt | Unit test | `NetworkDialogControllerTest.flagOff_neverShows`: cờ tắt; mất mạng 10 s và báo lỗi; vẫn `Hidden`. `NetworkDialogControllerTest.flagTurnedOff_whileShown_hides`: đang `Shown`; tắt cờ; `Hidden` |
| Tín hiệu cho T-013, T-018 | Unit test | `NetworkDialogControllerTest.dialogShown_followsState`: `dialogShown` là `false`, `true` khi hiện, `false` sau khi "Thử lại" thành công |
| Log theo ADR-0019 | Unit test | `NetworkDialogControllerTest.logsShowAndRetryOutcome`: các mục thẻ `Network` mức `INFO` cho lần hiện (`device_offline`, `load_failed`) và kết quả thử lại (`online`, `still_offline`); không mục nào chứa đường dẫn file |
| KB-1, nhận ra lỗi vì mạng | Unit test | `NetworkErrorsTest`: `UnknownHostException`, `ConnectException`, `NoRouteToHostException`, `SocketTimeoutException`, `SocketException` là `true`; `IOException("HTTP 404 for ...")`, `FileNotFoundException`, `IOException("Could not move ...")` là `false`; `IOException(cause = UnknownHostException())` là `true` |
| KB-1, `:core:assets` báo lỗi mạng | Unit test | `DefaultRemoteAssetsNetworkFailureTest`: bộ tải giả ném `UnknownHostException` thì `load()` trả `Unavailable` và bộ báo giả nhận đúng một lần `"assets"`; ném `IOException("HTTP 404 ...")` thì không báo; file đã có trên máy thì không báo; tải thành công thì không báo |
| KB-1, chữ đúng bảng chữ | Compose UI test (Robolectric, `NATIVE`, khung `w411dp-h891dp-xxhdpi`) | `NetworkDialogTest.default_showsTextOfDesignTable`: có chữ "Mất kết nối mạng", "App cần mạng để hoạt động. Kiểm tra kết nối Internet rồi thử lại.", "Thử lại"; không có "Vẫn chưa có mạng" |
| KB-1, kích thước theo `context/` | Compose UI test | `NetworkDialogTest.card_is331dpWide_andRetryAtLeast48dp`: thẻ (tag `CARD`) rộng 331 dp ở khung 411; nút (tag `RETRY`) cao ít nhất 48 dp và rộng bằng thẻ trừ hai lề `Spacing.lg` (sai số 0,5 dp) |
| KB-1, chạm ra ngoài và phía sau | Compose UI test | `NetworkDialogTest.screen_consumesTouches`: `Box` có nút đếm số lần bấm, phủ `NetworkDialogScreen`; `performClick` vào nút phía sau và `performTouchInput { click(topLeft + Offset(10f, 10f)) }` lên lớp mờ; số lần bấm là 0, `onRetry` không được gọi, hộp thoại vẫn còn |
| KB-2, bấm "Thử lại" | Compose UI test | `NetworkDialogTest.retry_click_callsOnRetry`: bấm nút; `onRetry` gọi đúng một lần |
| KB-2, đang kiểm tra | Compose UI test | `NetworkDialogTest.checking_showsSpinner_andIgnoresClicks`: `checking = true`; có tag `SPINNER`; bấm nút không gọi `onRetry` |
| KB-2, "Vẫn chưa có mạng" | Compose UI test | `NetworkDialogTest.stillOffline_showsMessageBelowButton`: `stillOffline = true`; có chữ "Vẫn chưa có mạng"; cạnh trên của nó thấp hơn cạnh dưới của nút |
| Nút đang chờ | Compose UI test | `PrimaryButtonLoadingTest.loading_showsSpinner_keepsText_ignoresClick`, `notLoading_hasNoSpinner` |
| Màn hình thấp | Compose UI test, khung `w360dp-h640dp-xxhdpi` | `NetworkDialogTest.smallScreen_cardFitsAndRetryVisible`: `stillOffline = true`; thẻ nằm trọn trong màn hình (cạnh trái, phải, trên, dưới trong khung), nút `assertIsDisplayed()` |
| KB-1, chặn thao tác và trình đọc màn hình | Compose UI test (`:app`, Robolectric, `@Config(application = Application::class)` để không khởi tạo Hilt và Firebase) | `NetworkDialogHostTest.shown_drawsDialogInSeparateWindow`: nội dung là nút có tag; `state = Shown`; `onAllNodes(isRoot())` có 2 node gốc; tag `network_dialog` nằm ở node gốc thứ hai; nút phía sau vẫn tìm được bằng `onAllNodesWithTag(...).fetchSemanticsNodes()` (giữ cho `AppLaunchTest`). Thất bại trên code của phiên bản 1: chỉ có 1 node gốc. `hidden_contentWorksNormally`: `state = Hidden`; 1 node gốc, bấm được, không có tag `network_dialog`. Robolectric không mô phỏng việc cửa sổ trên cùng chặn chạm và trình đọc màn hình của cửa sổ phía sau, nên phần đó kiểm tra thủ công (dòng "trên máy thật") |
| KB-3, nút quay lại | Compose UI test (`:app`) | `NetworkDialogHostTest.back_whileShown_callsOnBack_andKeepsDestination`: `createAndroidComposeRule<ComponentActivity>()`; nội dung là `NavHost` hai đích, đã sang đích thứ hai; `state = Shown`; lấy hộp thoại đang hiện bằng `ShadowDialog.getLatestDialog() as ComponentDialog` (Robolectric; `Dialog` của Compose dựng trên `ComponentDialog`) và gọi `onBackPressedDispatcher.onBackPressed()` trên luồng giao diện, đúng chỗ phím quay lại của hệ thống đi vào khi hộp thoại ở trên cùng; không thêm tham số chỉ dùng cho test vào `NetworkDialogHost`; `onBack` gọi một lần, vẫn ở đích thứ hai, hộp thoại vẫn còn. `back_whileHidden_navigatesBack`: `state = Hidden`; `activity.onBackPressedDispatcher.onBackPressed()` về đích đầu, `onBack` không được gọi |
| Giao diện hộp thoại, sáng | Screenshot test, khung `w411dp-h891dp-xxhdpi` | `NetworkDialogScreenshotTest.default_light`: `captureRoboImage("src/test/screenshots/NetworkDialogScreen_default_light.png")`, `AppTheme(darkTheme = false)`, `Surface` màu `background` phía sau, `NetworkDialogScreen(checking = false, stillOffline = false)` |
| Giao diện hộp thoại, tối | Screenshot test, khung chuẩn | `NetworkDialogScreenshotTest.default_dark`: `NetworkDialogScreen_default_dark.png`, `AppTheme(darkTheme = true)` |
| Dòng "Vẫn chưa có mạng" (không có trên ảnh thiết kế) | Screenshot test, khung chuẩn | `NetworkDialogScreenshotTest.stillOffline_light`: `NetworkDialogScreen_stillOffline_light.png`, để người duyệt xem vị trí dòng chữ |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất (`stillOffline = true`) | `NetworkDialogScreenshotTest.stillOffline_small`: `NetworkDialogScreen_stillOffline_small.png` |
| Đúng thiết kế | Screenshot test cùng tên với hai ảnh thiết kế (hai dòng trên), ảnh so sánh, rà soát độc lập theo mục 7 của mẫu thiết kế | Script so sánh thiết kế chỉ tìm ảnh trong `feature/<feature>/src/test/screenshots`, mà hộp thoại nằm ở `:core:designsystem`. Sau `scripts/check.sh`, chạy thẳng chương trình mà script đó gọi: `java -Djava.awt.headless=true scripts/design-compare/DesignCompare.java docs/features/network/design/screens core/designsystem/src/test/screenshots docs/features/network/design/review`. Chương trình chỉ ghép những ảnh có tên trong `screens/`. Commit `design/review/`, dán `summary.md` vào mô tả PR. Không có ảnh `_unfolded` nên không có test khung gập |
| Không có màn hình nào khác bị đổi | Screenshot test hiện có | Ảnh chuẩn của `:core:designsystem`, `:feature:sample`, `:feature:loading` qua `verifyRoborazziDebug` mà không ghi lại |
| Cờ mặc định tắt | Unit test hiện có | `DefaultFeatureFlagsTest` duyệt `FeatureFlag.entries`, tự bao cờ mới |
| Quy tắc phụ thuộc module | `checkModuleDependencies` | Chạy trong `scripts/check.sh` |
| App chạy được với callback mạng | Test trên emulator hiện có | `AppLaunchTest.appStarts_andReachesStartScreen` không đổi: sau loading, bộ điều khiển đăng ký callback của `ConnectivityManager`; thiếu quyền thì app dừng bằng `SecurityException` và test đỏ. Không thêm test emulator mới vì trạng thái mạng của emulator CI không điều khiển được |
| KB-1, KB-2, KB-3 trên máy thật | Kiểm tra thủ công | Trên bản staging: (1) bật chế độ máy bay khi đang ở màn loading: màn loading báo lỗi riêng, không có hộp thoại. (2) Qua màn mẫu, bật chế độ máy bay: hộp thoại hiện sau khoảng 3 giây, đúng chế độ sáng, tối theo máy. (3) Tắt rồi bật Wi-Fi trong dưới 3 giây: không có hộp thoại. (4) Đang có hộp thoại, chạm ra ngoài, chạm vào chỗ có nút phía sau: không có gì xảy ra. (5) Bấm "Thử lại" khi vẫn mất mạng: vòng chờ khoảng 2 giây, rồi dòng "Vẫn chưa có mạng". (6) Bật lại mạng mà không bấm: hộp thoại vẫn còn; bấm "Thử lại": đóng. (7) Đang có hộp thoại, bấm quay lại: app ra nền; mở lại khi vẫn mất mạng: hộp thoại còn. (8) Bật TalkBack khi có hộp thoại: chỉ đọc được hộp thoại. Log xem bằng `adb logcat -v threadtime \| grep ' VGL\.Network'` (5) Bật TalkBack, mất mạng ở màn mẫu: TalkBack chỉ đọc và lướt được trong hộp thoại; chạm vào vùng phía sau không bấm được nút của màn mẫu. |
| KB-4 trên bản staging | Kiểm tra thủ công | Không làm được trên staging vì cờ bật sẵn ở máy; unit test `flagOff_neverShows` thay thế. Người phụ trách sản phẩm kiểm tra trên production khi bật cờ từ xa |

**Ngoại lệ viết test trước:**

- Dòng `ACCESS_NETWORK_STATE` trong hai manifest: quyền đã có trong manifest gộp nhờ `play-services-ads-api` và `play-services-measurement`, nên không có test nào thất bại trước được. Kiểm bằng cách đọc manifest gộp của `stagingDebug` sau `scripts/check.sh` (tập quyền phải giống hệt trước khi sửa) và bằng Android Lint không báo `MissingPermission`.
- `AndroidConnectivitySource`: lớp chuyển callback của `ConnectivityManager` thành `Flow`, không có logic quyết định; Robolectric không giả lập đủ callback mạng mặc định. Kiểm bằng `AppLaunchTest` (không lỗi quyền) và bước kiểm tra thủ công (2), (3).
- Nối Hilt (`NetworkModule`, `AssetsModule`), `MainActivity`, tham số `onPastLoadingChanged` của `AppNavHost`: kiểm bằng build bản staging, `AppLaunchTest` và kiểm tra thủ công (1), (2).
- Icon vector, file giấy phép, chuỗi trong `strings.xml`: tài nguyên, kiểm qua test giao diện và screenshot test của hộp thoại.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 3 (quyền `ACCESS_NETWORK_STATE` khai báo mới trong manifest, dù manifest gộp đã có) và 7 (module core mới `:core:network`; sửa `:core:assets`, `:core:config`, `:core:designsystem`). Rủi ro cao, cần duyệt chéo. Không chạm quảng cáo, dữ liệu hay hộp thoại đồng ý, nên không cần người phụ trách sản phẩm duyệt riêng. Điểm dưới đây để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Năm module: module mới, ba module core, `:app` |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, mọi câu hỏi đã trả lời, thiết kế đã duyệt. AI tự quyết vài chi tiết: tiêu chí "có mạng", lớp vẽ phủ thay cửa sổ `Dialog`, khai báo quyền ở hai nơi (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Logic thời gian kiểm hết bằng thời gian ảo, giao diện có test JVM và screenshot. Trạng thái mạng thật, việc app ra nền và TalkBack chỉ kiểm thủ công trên máy |
| D. Ảnh hưởng chéo | 3 | Lớp phủ nằm trên mọi màn hình; `:core:assets` báo thêm tín hiệu mà mọi màn tải file đi qua; sửa `AppNavHost`, `FeatureFlag.kt`, `StagingFeatureFlagsModule`, `settings.gradle.kts`, `PrimaryButton` mà T-007, T-009, T-010 cũng sửa |
| E. Độ mới của kỹ thuật | 2 | `ConnectivityManager.NetworkCallback` và hộp thoại trong cửa sổ `Dialog` của Compose chưa có trong dự án nhưng phổ biến |
| **Tổng** | 11 | **Mức: Cao** (điểm chặn 3 và 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `settings.gradle.kts`, `app/build.gradle.kts`, `AppNavHost.kt`, `MainActivity.kt`, `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`, `PrimaryButton.kt`, `strings.xml` của `:core:designsystem`, `DefaultRemoteAssets.kt`, `AssetsModule.kt`, hai manifest.
- Task khác trong sprint có thể xung đột:
  - **T-007** (bắt buộc chạy trước): đổi `PrimaryButton`, màu, chữ, ghi lại ảnh catalog. Plan này sửa `PrimaryButton.kt` trên bản của T-007 và chụp ảnh với màu Sticker. Chạy trước T-007 thì ảnh chụp sai màu và phải ghi lại.
  - **T-009** (`:core:poses`): sửa `settings.gradle.kts` và có thể `app/build.gradle.kts`. Catalog và ảnh tư thế tải qua `RemoteAssets`, nên lỗi mạng tự được báo. T-009 dùng `isNetworkFailure()` nếu cần phân biệt lý do cho `content_load_failed`. Nếu T-009 có đường tải không đi qua `RemoteAssets` thì T-009 gọi `NetworkFailureReporter`.
  - **T-010**: sửa `AppNavHost` (đích sau loading) và có thể `FeatureFlag.kt`, `StagingFeatureFlagsModule`. Xung đột chỉ ở mức dòng; chạy tuần tự thì task sau dựng trên nhánh chính đã có task trước. Tham số `onPastLoadingChanged` không phụ thuộc đích nào sau loading, nên đúng với mọi đích mới của T-010.
  - **T-013** (camera): đọc `NetworkStatus.dialogShown` để dừng đếm ngược; đặt `darkTheme = true` cho `NetworkDialogHost` khi đích là camera (sửa `AppNavHost` hoặc `MainActivity`). T-016 cũng vậy cho màn xem lại, và quyết định việc giữ hộp thoại tới khi lưu xong (câu hỏi 4).
  - **T-018**: `:core:ads` thêm phụ thuộc `:core:network` (core sang core) và không hiện quảng cáo toàn màn hình khi `dialogShown` là `true`.
  - **T-021, T-022, T-014**: ViewModel thu `NetworkStatus.recovered` để tải lại phần dở; khi lỗi vì mạng và cờ hộp thoại bật thì không hiện thông báo lỗi riêng (spec `poses`). Feature cần thêm `implementation(project(":core:network"))` vì convention plugin của feature chưa có module này (câu hỏi 2).
- Đề xuất thứ tự chạy: sau T-007 và T-009 (dùng lý do lỗi T-009 thêm vào `AssetResult.Unavailable`). Trước T-013, T-018, T-021, T-022 để các task đó dùng được tín hiệu. Độc lập với T-010 về logic; nếu chạy cùng phiên thì đặt T-011 trước T-010 để T-010 dựng trên `AppNavHost` đã có tham số mới.

## 7. Điều kiện dừng riêng

1. T-007 chưa merge vào nhánh chính lúc bắt đầu task, hoặc sau T-007 thiếu một giá trị mà mục 2 giả định (`camera/scrim`, bo `sheet`, viền `sticker`, vai trò màu, kiểu chữ Sticker). Không viết cứng giá trị thay thế.
2. Manifest gộp của `stagingDebug` sau thay đổi có quyền nào khác so với trước, hoặc thiếu `ACCESS_NETWORK_STATE`.
3. Trên JVM, `Dialog` không tạo node gốc thứ hai, hoặc bộ điều phối phím quay lại của hộp thoại không gọi được `BackHandler` trong hộp thoại: dừng, không quay lại cách một cửa sổ. (Phiên bản 1 dừng vì `hideFromAccessibility` trên khung bọc không lan xuống nút con.)
4. Test JVM của `:app` không chạy được trên Robolectric dù đã dùng `@Config(application = Application::class)` (ví dụ vì Hilt hay Firebase), và cách sửa cần file ngoài danh sách.
5. `BackHandler` của vỏ không được ưu tiên hơn nút quay lại của `NavHost` sau 3 lần sửa.
6. `PrimaryButton` của T-007 có API khác tới mức thêm `loading` phải đổi chữ ký mà nơi khác đang gọi.
7. Lúc làm task, `:core:poses` đã có trên nhánh chính và có đường tải mạng không qua `RemoteAssets`: không tự nối vào, ghi vào nhật ký để T-009 hay plan sau xử lý.

## 8. Câu hỏi cho người duyệt

1. **Thế nào là "máy có mạng"?** Đề xuất của AI: mạng mặc định có `NET_CAPABILITY_INTERNET`, không đòi `NET_CAPABILITY_VALIDATED`. Đòi thêm `VALIDATED` thì bắt được Wi-Fi có cổng đăng nhập, nhưng có máy và emulator xác thực chậm hoặc không bao giờ, dễ hiện hộp thoại sai. Trường hợp Wi-Fi không ra được Internet đã được bắt qua lỗi tải vì mạng.
- Trả lời: OK.
2. **Feature dùng `NetworkStatus` thế nào?** `AndroidFeatureConventionPlugin` tự thêm sáu module core cho mọi feature, chưa có `:core:network`, và plan không được sửa `build-logic/`. Đề xuất của AI: feature nào cần (T-013, T-021, T-022) ghi thêm `implementation(project(":core:network"))` trong `build.gradle.kts` của mình, lệch nhẹ khỏi bước 1 của CLAUDE.md, ghi rõ trong plan của task đó. Phương án khác: một plan rủi ro cao riêng thêm module vào convention plugin.
- Trả lời: OK.
3. **Chặn quảng cáo toàn màn hình khi hộp thoại hiện: làm ở đâu?** App chưa có vị trí toàn màn hình. Đề xuất của AI: T-018 thêm kiểm tra `dialogShown` vào `AdsController.showInterstitial` khi thêm `review_interstitial`; ticket này không chạm `:core:ads` để không đụng thêm điểm chặn 1.
- Trả lời: OK.
4. **Đang lưu ảnh thì hộp thoại "hiện sau đó" (KB-1, trường hợp đặc biệt).** Lưu ảnh không bị dừng vì hộp thoại chỉ là lớp vẽ phủ. Nếu muốn hộp thoại chờ tới khi lưu xong, đề xuất của AI: T-016 thêm một hàm hoãn hiển thị vào `NetworkDialogController` (ví dụ giữ lại khi màn xem lại báo đang lưu). Ticket này không làm, vì chưa có màn xem lại để test.
- Trả lời: OK.
5. **Lý do lỗi cho `content_load_failed`: đã quyết, không cần trả lời.** Phiên chính đối chiếu với plan T-009 và giao việc này cho T-009: T-009 thêm lý do vào `AssetResult.Unavailable` (`NETWORK`, `NOT_FOUND`, `OTHER`). Khi làm ticket này, `isNetworkFailure()` và `NetworkFailureReporter` dùng lại cách phân loại đó của `:core:assets` nếu đã có trên nhánh chính, không phân loại lại; ticket này vì vậy chạy sau T-009 (ticket T-011 phụ thuộc thêm T-009).
- Trả lời: OK.
6. **Khai báo quyền ở cả hai manifest.** Đề xuất của AI: khai báo ở `app/src/main/AndroidManifest.xml` để nhóm thấy và ở `core/network/src/main/AndroidManifest.xml` để module tự mang quyền và Lint không báo. Người duyệt muốn chỉ một nơi thì chọn `:app`, bỏ file manifest của `:core:network` khỏi danh sách.
- Trả lời: OK.
7. **Ảnh so sánh thiết kế cho thành phần trong module core.** Script so sánh thiết kế chưa hỗ trợ module core; plan chạy thẳng `DesignCompare.java` với thư mục ảnh của `:core:designsystem`. Đề xuất của AI: ghi đề xuất về dự án mẫu (`docs/ops/from-app.md`) cho script nhận thêm thư mục ảnh, không sửa script trong ticket này.
- Trả lời: OK.
8. **Màu vòng chờ trên nút.** `sticker.md` ghi vòng chờ dùng `primary`, nhưng trên nút nền `secondary` (đen ở chế độ sáng), màu xanh `primary` khó thấy. Đề xuất của AI: vòng chờ trong nút dùng màu nội dung của nút (`onSecondary`), vòng chờ ở chỗ khác vẫn dùng `primary`.
- Trả lời: OK.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1: Duyệt (Nhật, duyệt chéo Tuấn, 2026-10-21); dừng ở RUN-20261021-03, xem mục 10.

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** 

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261021-03, phiên bản plan 1

- **Kết quả:** dừng ở bước 4 (risk-scoring.md, mục 4, điều kiện 8: cần đổi điều mà test viết trước đang kiểm tra để code qua được)
- **Nhánh, PR:** `claude/T-011-core-network-hop-thoai` (đã đẩy: commit test acac38b, commit code 555eb12; chưa mở PR vì test chưa qua)
- **Đã làm:** bước 3 đủ theo mục 4 (khung rỗng và test). Bước 4 đủ theo mục 3.2: `:core:network` (bộ điều khiển, nguồn mạng, phân loại lỗi, Hilt), `:core:assets` báo lỗi mạng, `NetworkDialog`, `NetworkDialogScreen`, `PrimaryButton(loading)`, icon `wifi_off`, `refresh` và giấy phép Material Symbols, bốn chuỗi, `NetworkDialogHost`, `AppNavHost(onPastLoadingChanged)`, `MainActivity`, hai manifest, cờ staging. Mọi file đều trong mục 3.2. Chưa ghi ảnh chuẩn, chưa chạy `scripts/check.sh` và ảnh so sánh thiết kế.
- **Test viết trước:** 43 test JVM mới và 4 screenshot test (chưa chạy, chưa có ảnh chuẩn). Trên khung rỗng 35 test thất bại ở phần khẳng định (vì chức năng chưa có), 8 qua ngay vì là test chặn hồi quy: `NetworkErrorsTest.otherErrors_areNotNetworkFailures`, bốn test "không báo" của `DefaultRemoteAssetsNetworkFailureTest`, `PrimaryButtonLoadingTest.notLoading_hasNoSpinner`, `NetworkDialogHostTest.hidden_contentWorksNormally`, `back_whileHidden_navigatesBack`. Một số test của mục 4 sẽ qua ngay trên khung rỗng (ví dụ `flapping_underDelay_showsNothing`, `inactive_ignoresOfflineAndFailures`, `flagOff_neverShows`); phiên thêm vào cuối mỗi test một bước kiểm tra chiều ngược lại (hộp thoại có hiện khi đủ điều kiện) để test thất bại trước khi có code. Sau bước 4: 42 qua, 1 thất bại (dưới đây). Một lỗi code đã sửa trong bước 4: `testTag` của thẻ đặt sau `padding` nên thẻ đo ra 283 dp thay vì 331.
- **Lý do dừng:** `NetworkDialogHostTest.shown_blocksContentClicks_andHidesItFromAccessibility` thất bại ở khẳng định "nút phía sau mang `SemanticsProperties.HideFromAccessibility`" (mục 4): `Failed to assert the following: (HideFromAccessibility is defined) ... Tag: 'content' Role = 'Button'`. Code làm đúng mục 3.1: `Modifier.semantics { hideFromAccessibility() }` trên khung bọc nội dung. Thuộc tính này chỉ nằm trên node của khung, không lan xuống node con. Trong Compose UI 1.9.1 (BOM 2025.09.00), `SemanticsNode.isHidden` chỉ đọc cấu hình của chính node (`isTransparent`, `HideFromAccessibility`, `InvisibleToUser`), và bộ trợ năng gọi `setVisibleToUser(!isHidden)` cho từng node (đọc bytecode `SemanticsOwnerKt.isHidden` và `AndroidComposeViewAccessibilityDelegateCompat`). Nghĩa là với cách của plan, nút phía sau vẫn hiện với TalkBack: tiêu chí KB-1 "không bấm được gì phía sau" không đạt với người dùng trình đọc màn hình, và test viết trước đã bắt đúng điều đó.
- **Vì sao không tự xử lý:** muốn test qua phải đổi điều test kiểm tra (ví dụ chỉ xét node cha, khi đó test qua mà TalkBack vẫn đọc được nút) hoặc đổi cách ẩn sang cách plan chưa nêu. `clearAndSetSemantics` bị mục 7, điều kiện 3 cấm vì làm hỏng `AppLaunchTest`.
- **Rà soát độc lập:** chưa chạy (dừng ở bước 4).
- **Kết quả CI:** chưa chạy.
- **Lệch so với plan:** `DefaultRemoteAssets` báo lỗi mạng khi `toFailureReason()` là `AssetFailureReason.NETWORK` (cách phân loại T-009 đã thêm), thay vì gọi `e.isNetworkFailure()`, theo câu trả lời câu hỏi 5 ở mục 8 ("dùng lại cách phân loại đó của `:core:assets` nếu đã có trên nhánh chính"). `isNetworkFailure()` vẫn có trong `:core:network` và có test. Icon vector bỏ `android:tint="?attr/colorControlNormal"` của file gốc, vì `Icon` của Compose tự tô màu.
- **Việc còn lại:** cách ẩn nội dung khỏi trình đọc màn hình; ghi 4 ảnh chuẩn; `scripts/check.sh`; so sánh manifest gộp (đã xem báo cáo gộp manifest: `ACCESS_NETWORK_STATE` có từ `play-services-ads-api` 25.5.0 và `play-services-measurement` 23.2.0, tập quyền không đổi); ảnh so sánh thiết kế; bước 6 đến 10.
- **Cần người xem:** chọn hướng ẩn nội dung khỏi trình đọc màn hình (bản sửa đề xuất bên dưới).

#### Bản sửa đề xuất, phiên bản 2 (đã viết thành plan phiên bản 2: hướng (b), người duyệt chọn ngày 2026-10-22)

- **Vì sao plan chưa đủ tốt:** mục 3.1 giả định `hideFromAccessibility` trên một khung bọc ẩn được cả cây con; plan chưa chạy thử điều này trước khi duyệt (L-029, L-001). Đây là kỹ thuật lần đầu dùng trong dự án (plan tự chấm E = 2).
- **Thay đổi so với phiên bản 1:** chỉ cách ẩn nội dung phía sau khỏi trình đọc màn hình (mục 3.1 phần `NetworkDialogHost`, dòng KB-1 của mục 4 và mục 7 điều kiện 3). Người chọn một trong các hướng, mỗi hướng phải chạy thử bằng test tạm trước khi duyệt, đo bằng cây `AccessibilityNodeInfo` thật (ví dụ `isVisibleToUser` của nút khi bật trợ năng trong Robolectric), không chỉ bằng cây ngữ nghĩa:
  - (a) Cho lớp mờ của `NetworkDialogScreen` một thuộc tính ngữ nghĩa làm nó "quan trọng với trợ năng" (giả thuyết chưa kiểm chứng: Compose bỏ các node bị một node quan trọng che kín, `getAllUncoveredSemanticsNodesToIntObjectMap`). Nút vẫn trong cây ngữ nghĩa nên `AppLaunchTest` không đổi.
  - (b) Vẽ hộp thoại trong một cửa sổ `Dialog` riêng (`DialogProperties(dismissOnClickOutside = false)`), để hệ thống tự chặn trợ năng phía sau. Bỏ lựa chọn "một cửa sổ" của mục 3.1; screenshot test chụp `NetworkDialogScreen` trực tiếp như hiện tại.
  - (c) `View.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS` trên view chứa nội dung (cần tách nội dung và hộp thoại ra hai view; lớn hơn hai hướng trên).
- **Cách tiếp tục (mục 3.1, khi duyệt):** tiếp tục trên nhánh `claude/T-011-core-network-hop-thoai`; mọi phần khác đã có code và test qua. Bước 3 chỉ áp dụng cho test đổi điều kiểm tra (phần trợ năng của `shown_blocksContentClicks_andHidesItFromAccessibility`).
- **Chưa có PR sửa nguyên nhân:** nguyên nhân nằm trong cách làm của plan, không phải trong code ngoài plan.

### RUN-20261022-01, phiên bản plan 2

- **Kết quả:** đã merge (PR #23, commit 2797b86)
- **Nhánh, PR:** `claude/T-011-core-network-hop-thoai`, PR #23. Tiếp tục trên nhánh cũ, merge nhánh chính vào trước khi làm.
- **Đã làm:** bước 3 cho `NetworkDialogHostTest` theo mục 4 phiên bản 2 (`shown_drawsDialogInSeparateWindow`, `back_whileShown_callsOnBack_andKeepsDestination` qua `ShadowDialog.getLatestDialog()`); `NetworkDialogHost` vẽ hộp thoại trong cửa sổ `Dialog` riêng; 4 ảnh chuẩn; ảnh so sánh thiết kế bằng `DesignCompare.java`.
- **Test viết trước:** 2 test đổi điều kiểm tra thất bại đúng lý do trên code phiên bản 1 (1 node gốc thay vì 2; không có hộp thoại nên `ShadowDialog.getLatestDialog()` trả `null`). 2 test chặn hồi quy qua ngay như trước. Sau rà soát độc lập thêm `shown_dialogWindowAddsNoSystemDim`, thất bại trước khi sửa.
- **Rà soát độc lập:** 0 chặn đã xác nhận, 2 nên sửa (đã sửa trong phạm vi file của plan), 6 ghi chú. Đã sửa: cửa sổ hộp thoại để hệ thống làm tối thêm phía sau (mờ hai lần) và không phủ thanh hệ thống, nay `decorFitsSystemWindows = false`, `setDimAmount(0f)`, thẻ có `safeDrawingPadding()`; `retry()` đổi trạng thái bằng `compareAndSet` để không mở lại hộp thoại vừa bị tắt (cờ tắt hay quay về loading cùng lúc bấm "Thử lại").
- **Kết quả CI:** xanh ở vòng đầu (`changes`, `build-and-test`, `ui-test`), lần chạy 39708763748.
- **Lệch so với plan:** `DialogProperties` thêm `decorFitsSystemWindows = false` và tắt lớp tối của hệ thống (mục 3.1 chỉ nêu ba thuộc tính); `NetworkDialogScreen` thêm `safeDrawingPadding()`; thêm một test vào `NetworkDialogHostTest`. Đều trong file của mục 3.2, theo rà soát độc lập. Lệch của phiên bản 1 (báo lỗi theo `AssetFailureReason.NETWORK`) giữ nguyên.
- **Cần người xem:** 4 ảnh chuẩn mới; ảnh so sánh thiết kế (phần khác 76,5% ở ảnh sáng là nền đen phía sau lớp mờ của ảnh thiết kế); kiểm tra thủ công mục 4 trên bản staging, thêm: lớp mờ phủ cả thanh trạng thái, độ tối giống thiết kế; TalkBack đọc tiêu đề hộp thoại một lần (chưa đặt `windowTitle`).
