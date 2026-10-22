---
ticket: T-025
title: Màn cài đặt
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 20.0M        # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [consent, external-link, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-025: MÀN CÀI ĐẶT

## 1. Tóm tắt

Tạo module `:feature:settings` với màn cài đặt theo thiết kế Sticker: giao diện sáng tối, âm khi chụp, ngôn ngữ, tùy chọn quyền riêng tư, chính sách quyền riêng tư, phiên bản; mở từ nút bánh răng của camera (và của thư viện nếu T-021 đã có). Ba lựa chọn của người dùng nằm trong `:core:data` (`UserPreferences`), vì `:app` (giao diện sáng tối, ngôn ngữ dưới Android 13) và `:feature:camera` (âm chụp) cũng đọc. Thành phần mới `SettingsRow`, `SectionHeader`, `ChoiceDialog` vào `:core:designsystem`; `PrivacyOptionsButton` của `:core:ads` thêm một bản nhận nội dung để vẽ thành dòng cài đặt; đường dẫn chính sách đọc từ Remote Config qua `AppLinks` mới của `:core:config`. Kiểm chứng bằng unit test ViewModel, repository và các hàm của `:app`, test giao diện Robolectric, screenshot test cùng tên bốn ảnh thiết kế cộng ảnh hộp chọn ngôn ngữ và hai ảnh `_small`, test emulator mở cài đặt từ camera rồi quay lại, và kiểm tra thủ công cho phần phụ thuộc hệ thống (trang ngôn ngữ của máy, hộp thoại đồng ý, trình duyệt).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/settings/spec.md` (spec `confirmed`):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi bấm nút bánh răng ở camera hoặc ở thư viện thì màn cài đặt mở, đúng thiết kế, không có quảng cáo | Camera đã có nút cài đặt ẩn khi `onOpenSettings == null` (T-013). `:app` truyền hàm mở cài đặt khi cờ `feature_settings_enabled` bật. Thư viện là màn của T-021, chạy song song: nếu `posesScreen` đã có tham số `onOpenSettings` trên nhánh chính lúc bắt đầu task thì T-025 nối luôn trong `AppNavHost.kt`; nếu chưa, T-021 nối (mục 6). Màn cài đặt không gọi `AdSlot` hay `rememberShowInterstitial` |
| KB-1 | Khi bấm quay lại thì về màn trước đó | Nút quay lại trên thanh trên và nút Back của hệ thống gọi `popBackStack()`; cài đặt mở với `launchSingleTop` nên bấm bánh răng hai lần không chồng hai màn |
| KB-2 | Khi bấm "Giao diện" thì một hộp chọn hiện ba lựa chọn: Theo hệ thống, Sáng, Tối; lựa chọn hiện tại được đánh dấu | `ChoiceDialog` với ba `RadioRow`; lựa chọn đang lưu có nút chọn vàng và ngữ nghĩa `selected` |
| KB-2 | Khi chọn một lựa chọn thì app đổi giao diện ngay, dòng "Giao diện" ghi lựa chọn mới | Chọn thì đóng hộp và lưu. `MainActivity` theo dõi `UserPreferences.themeMode` và truyền `darkTheme` cho `AppTheme`, nên mọi màn đổi ngay, không dựng lại activity |
| KB-2 | Khi chọn "Theo hệ thống" thì app đổi theo chế độ sáng tối của máy, kể cả khi máy đổi lúc app đang mở | `SYSTEM` dùng `isSystemInDarkTheme()`. Máy đổi chế độ thì hệ thống dựng lại activity (manifest không khai `configChanges` cho `uiMode`), nên giá trị mới được đọc |
| KB-2 | Khi đổi giao diện thì camera và màn xem lại vẫn nền tối | `CameraRoute` đã bọc `AppTheme(darkTheme = true)` (T-013), T-025 không đổi phần này. Màn xem lại chưa có (T-016): T-016 làm theo cùng cách (mục 6). Dải thanh trạng thái phía trên camera vẫn theo giao diện của app (câu hỏi 12 của T-013, mục 8 câu 7) |
| KB-2 | Khi mở lại app thì lựa chọn vẫn giữ. Mặc định là "Theo hệ thống" | Khóa DataStore `settings_theme_mode`, mặc định `SYSTEM` |
| KB-3 | Khi tắt "Âm khi chụp" thì chụp ảnh không phát âm thanh của app; bật thì có | `CameraViewModel` theo dõi `UserPreferences.shutterSoundEnabled` và truyền vào `CameraController.takePicture(playShutterSound)`; `CameraXController` chỉ phát `MediaActionSound` khi được yêu cầu (trả lời câu hỏi 6 của plan T-013) |
| KB-3 | Khi mở lại app thì lựa chọn vẫn giữ. Mặc định là bật | Khóa `settings_shutter_sound`, mặc định `true` |
| KB-4 | Khi bấm "Ngôn ngữ" trên Android 13 trở lên thì trang ngôn ngữ của app trong cài đặt máy mở ra; chọn xong quay lại thì app hiện ngôn ngữ mới | `Settings.ACTION_APP_LOCALE_SETTINGS` với `package:<tên gói>`. Trang này chỉ liệt kê ngôn ngữ khi app khai `android:localeConfig`, nên thêm `res/xml/locales_config.xml` với 7 ngôn ngữ. Chọn xong, hệ thống tự dựng lại activity với ngôn ngữ mới |
| KB-4 | Khi bấm "Ngôn ngữ" trên máy dưới Android 13 thì một hộp chọn trong app hiện "Theo hệ thống" và 7 ngôn ngữ phát hành; chọn xong app đổi ngôn ngữ ngay | `ChoiceDialog` 8 lựa chọn. Chọn thì lưu `settings_app_language` rồi dựng lại activity; `MainActivity.attachBaseContext` gắn ngôn ngữ đã lưu vào context. Dự án không có AppCompat (`AppCompatDelegate.setApplicationLocales`) và plan không thêm thư viện, nên tự làm (mục 3.1) |
| KB-4 | Khi dòng "Ngôn ngữ" hiện thì nó ghi ngôn ngữ đang dùng, viết bằng chính ngôn ngữ đó | Chuỗi `settings_language_current` có trong cả 7 thư mục chữ, mỗi thư mục ghi tên của chính ngôn ngữ đó ("English", "Tiếng Việt", "हिन्दी"...). Chuỗi được lấy theo ngôn ngữ mà tài nguyên đang dùng, nên luôn đúng ngôn ngữ thật đang hiện, kể cả khi máy đặt ngôn ngữ ngoài 7 ngôn ngữ (app hiện tiếng Anh, dòng ghi "English") |
| KB-5 | Khi bấm "Tùy chọn quyền riêng tư" thì hộp thoại đồng ý quảng cáo của `:core:ads` hiện lại | `PrivacyOptionsButton` thêm bản `PrivacyOptionsButton(modifier, content: @Composable (onClick: () -> Unit) -> Unit)`: vẫn tự quyết định hiện hay ẩn và tự mở hộp thoại, chỉ để feature vẽ dòng. Feature vẫn chỉ dùng `PrivacyOptionsButton` như ADR-0013 |
| KB-5 | Khi người dùng ở nơi không cần hỏi đồng ý thì dòng "Tùy chọn quyền riêng tư" không hiện | `PrivacyOptionsButton` không vẽ gì khi `privacyOptionsRequired` là `false` hoặc không có `AdsController` (test, preview) |
| KB-5 | Khi bấm "Chính sách quyền riêng tư" thì trang chính sách mở trong trình duyệt. Đường dẫn đặt trong `RemoteSettings`, mặc định https://studio.vgl.io.vn/privacy-policy | Khóa Remote Config `privacy_policy_url`, khai trong `AppLinks` của `:core:config` (chú thích của `RemoteSettings`: feature không dùng trực tiếp, khóa do module core khai). Giá trị không bắt đầu bằng `https://` thì dùng mặc định. Mở bằng `Intent.ACTION_VIEW` |
| KB-6 | Khi vào cài đặt thì dòng "Phiên bản" ghi số phiên bản của app; dòng này không bấm được và không có mũi tên | `versionName` của gói (`PackageManager`), ví dụ bản staging "0.1.0-staging.42". `SettingsRow` kiểu `Info`: không có hành động bấm, không có biểu tượng cuối dòng |
| Phạm vi ticket | Cờ `feature_settings_enabled` | `FeatureFlag.SETTINGS`, mặc định tắt, bật ở staging. Cờ tắt thì `:app` truyền `onOpenSettings = null`, nút bánh răng ẩn |
| Phạm vi ticket | Thành phần mới `SettingsRow` (bốn kiểu), `SectionHeader`; hộp chọn giao diện và ngôn ngữ | `SettingsRow`, `SectionHeader`, `ChoiceDialog`, `RadioRow` trong `:core:designsystem`, trang catalog mới (mục 3.1, mục 8 câu 2) |

Thiết kế tham chiếu: `features/settings/design/README.md` (`approved`), số liệu trong `design/context/`, ảnh trong `design/screens/`: `SettingsScreen_default_light`, `SettingsScreen_default_dark`, `SettingsScreen_theme-picker_light`, `SettingsScreen_theme-picker_dark`. Cả bốn ảnh thuộc ticket này. Hộp chọn ngôn ngữ không có ảnh riêng (README mục 1). Trên ảnh, dòng "Phiên bản" đã bỏ mũi tên theo KB-6.

ADR liên quan: ADR-0001 (module feature, dùng chung qua core), ADR-0003 (cờ tính năng, `RemoteSettings`), ADR-0007 (không viết cứng cấu hình môi trường: đường dẫn chính sách là tham số kinh doanh, không theo môi trường), ADR-0008 (thành phần mới trong `:core:designsystem`, không viết cứng màu và cỡ chữ), ADR-0013 (feature chỉ dùng `PrivacyOptionsButton`), ADR-0019 (log qua `AppLogger`), ADR-0020 (tên screenshot test theo ảnh thiết kế, ảnh so sánh).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu `:feature:sample` và `:feature:camera` (repository trong `data/`, Hilt module trong `di/`, `UiState` sealed, ViewModel với `StateFlow`, `Route` nối ViewModel và `Screen` không trạng thái, sự kiện một lần qua `Channel`, đích `@Serializable`), mẫu thành phần của `:core:designsystem` (T-007, T-013: `component/`, trang catalog, test đo kích thước và màu điểm ảnh) và mẫu hộp thoại của `NetworkDialogHost` (lớp mờ do màn vẽ, cửa sổ `Dialog` riêng, tắt lớp mờ của hệ thống). Gói gốc: `com.vglstudio.camera.pose.line.guide.feature.settings`. Thẻ log: `Settings`.

**Điều kiện đầu vào.** T-007 và T-013 đã merge (đúng với nhánh chính hiện tại): có `AppColors.cameraScrim`, `Radius.sheet`, `Stroke.sticker`, `Spacing`; `cameraScreen(onPhotoCaptured, onOpenPoses, onOpenSettings)`; `CameraController.takePicture()`; `CameraViewModelTest` dựng ViewModel ở một hàm phụ trợ. Khác thì dừng (mục 7).

**Lựa chọn của người dùng (`:core:data`).** File mới `UserPreferences.kt`:

```kotlin
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The release languages (spec settings, KB-4); "" follows the device. */
object AppLanguages {
    const val SYSTEM = ""
    val RELEASE: List<String> = listOf("en", "hi", "pt-BR", "es-419", "id", "th", "vi")
}

/** Choices made on the settings screen, read by :app and the camera. Setters throw IOException. */
interface UserPreferences {
    val themeMode: Flow<ThemeMode>
    val shutterSoundEnabled: Flow<Boolean>
    /** Used below Android 13 only; from Android 13 the system keeps the app language. */
    val appLanguage: Flow<String>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setShutterSoundEnabled(enabled: Boolean)
    suspend fun setAppLanguage(tag: String)
}
```

`DataStoreUserPreferences` (`internal`) dùng `DataStore<Preferences>` chung, ba khóa mới `settings_theme_mode` (tên enum), `settings_shutter_sound` (boolean), `settings_app_language` (thẻ ngôn ngữ). Giá trị lạ về mặc định (`SYSTEM`, `true`, `""`); thẻ ngoài `AppLanguages.RELEASE` coi như `""`. Đọc lỗi `IOException` thì phát dữ liệu rỗng và thử lại như `DataStoreCounterRepository`. `di/UserPreferencesModule.kt` gắn interface bằng `@Binds`. Không đổi `core/data/build.gradle.kts`: DataStore đã kéo theo coroutines, Hilt đã có.

**Đường dẫn chính sách (`:core:config`).** File mới `AppLinks.kt`: `interface AppLinks { val privacyPolicyUrl: Flow<String> }`; `DefaultAppLinks` (`internal`) đọc `remoteSettings.string("privacy_policy_url", DEFAULT_PRIVACY_POLICY_URL)` với `DEFAULT_PRIVACY_POLICY_URL = "https://studio.vgl.io.vn/privacy-policy"`, giá trị không bắt đầu bằng `https://` (rỗng, gõ sai trên Remote Config) thì thay bằng mặc định. Gắn trong `ConfigModule`. `FeatureFlag.SETTINGS("feature_settings_enabled")`.

**Mở hộp thoại đồng ý (`:core:ads`, `AdSlot.kt`).** Thêm bản thứ hai, bản cũ gọi bản mới để không lặp logic:

```kotlin
@Composable
fun PrivacyOptionsButton(modifier: Modifier = Modifier, content: @Composable (onClick: () -> Unit) -> Unit) {
    val ads = LocalAdsController.current ?: return
    val activity = LocalActivity.current ?: return
    val required by ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    if (required) Box(modifier) { content { ads.showPrivacyOptionsForm(activity) } }
}
```

Bản cũ `PrivacyOptionsButton(modifier)` giữ nguyên cách hiện (`TextButton` "Privacy settings"). Không đổi `AdsController`, thứ tự đồng ý hay khởi tạo SDK.

**Thành phần mới trong `:core:designsystem`** (`component/`, theo `sticker.md` mục 4 và bảng ánh xạ của README thiết kế; số liệu `context/`):

- `SectionHeader(title: String, modifier)`: `Text` `labelLarge` màu `onSurfaceVariant`, lề trên `Spacing.lg`, ngang `Spacing.md`, dưới `Spacing.sm` (cao 52 như `context/`), ngữ nghĩa `heading()`.
- `SettingsRow(icon: Painter, title: String, trailing: SettingsRowTrailing, modifier, value: String? = null)` với `sealed interface SettingsRowTrailing { data class Chevron(val onClick: () -> Unit); data class Switch(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit); data class Link(val onClick: () -> Unit); data object None }`, ứng với bốn kiểu `Value`, `Switch`, `Link`, `Info` của thiết kế. `Row` cao ít nhất 64, lề ngang `Spacing.md`, dọc `Spacing.sm`, các phần cách `Spacing.md`, căn giữa dọc. Icon đầu 24 màu `onSurfaceVariant`; tiêu đề `bodyLarge` `onSurface`; giá trị `bodyMedium` `onSurfaceVariant` ngay dưới tiêu đề. `context/` có khoảng cách 2 giữa tiêu đề và giá trị, ngoài hệ thống: bỏ, khối chữ cao 44 thay vì 46, vẫn căn giữa trong dòng 64 (lệch 1 dp, trong lưới 4 dp, README mục 6). Cuối dòng: `Chevron` vẽ `ic_chevron_right` 24 màu `onSurfaceVariant`, cả dòng `clickable` (`Role.Button`); `Link` vẽ `ic_open_in_new`, cả dòng `clickable`; `Switch` vẽ `Switch` của Material 3 với `onCheckedChange = null`, cả dòng `toggleable(role = Role.Switch)`, màu: bật thì rãnh `primary`, núm `onPrimary`, viền `outline`; tắt thì rãnh `surface`, núm `outline`, viền `outline`; `None` không vẽ gì, dòng không bấm được, ngữ nghĩa gộp tiêu đề và giá trị (`mergeDescendants`). Mỗi phần cuối dòng mang `testTag` (`SettingsRowTestTags.CHEVRON`, `LINK`, `SWITCH`).
- `ChoiceDialog(title: String, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, dismissLabel: String, onDismiss: () -> Unit, modifier)`: như `NetworkDialogScreen`, phủ cả màn bằng `cameraScrim` độ mờ 50%; chạm lớp mờ gọi `onDismiss`. Thẻ ở giữa rộng tối đa 331, nền `surface`, viền `Stroke.sticker` màu `outline`, bo `Radius.sheet`, lề trong `Spacing.lg`, các phần cách `Spacing.sm`, ngữ nghĩa `paneTitle = title`. Tiêu đề `titleLarge` `onSurface`. Danh sách lựa chọn `selectableGroup()`, cuộn được khi màn thấp (8 lựa chọn của hộp ngôn ngữ). Nút đóng: `TextButton` cao 48, chữ `labelLarge` `onSurface`, căn phải. `dismissLabel` do feature truyền, để core không phải có chữ dịch 7 ngôn ngữ.
- `RadioRow(label: String, selected: Boolean, onClick: () -> Unit, modifier)`: cao 48, nút chọn và chữ cách `Spacing.md`, cả dòng `selectable(role = Role.RadioButton)`. Nút chọn tự vẽ tròn 24, viền `Stroke.sticker` màu `outline`; đang chọn thì nền `secondaryContainer` và chấm giữa 10 màu `onSecondaryContainer`, không chọn thì nền `surface`. `RadioButton` của Material 3 không tô được nền vàng có viền đen như thiết kế (mục 8 câu 2). Chữ `bodyLarge` `onSurface`.
- Hai biểu tượng mới `res/drawable/ic_chevron_right.xml`, `ic_open_in_new.xml` (Material Symbols Rounded như `sticker.md`; giấy phép đã có ở `core/designsystem/third_party/material-symbols/LICENSE.txt`). Chú thích đầu file ghi tên, nguồn, commit SHA đã tải, như các biểu tượng hiện có.
- Catalog: trang `SettingsComponentCatalog` (nền `background`): `SectionHeader`, bốn kiểu `SettingsRow` (công tắc bật và tắt), `ChoiceDialog` ba lựa chọn vẽ trong khung cao 400. Hai screenshot test mới `settingsComponents_light`, `settingsComponents_dark`.

**Màn cài đặt (`:feature:settings`).**

```kotlin
sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Ready(
        val themeMode: ThemeMode,
        val shutterSoundEnabled: Boolean,
        /** Saved language below Android 13 ("" = system); unused from Android 13. */
        val languageTag: String,
        val version: String,
        val dialog: SettingsDialog? = null,
        val saveFailed: Boolean = false,
    ) : SettingsUiState
}
enum class SettingsDialog { THEME, LANGUAGE }
sealed interface SettingsEvent {
    data object OpenSystemLanguageSettings : SettingsEvent
    data class OpenUrl(val url: String) : SettingsEvent
    data object ApplyLanguage : SettingsEvent
}
```

`data/`: `fun interface AppVersionProvider { fun versionName(): String }` với `PackageManagerAppVersionProvider` (`internal`, `getPackageInfo(packageName, PackageInfoFlags.of(0))` từ Android 13, bản cũ dưới đó; `null` thì chuỗi rỗng); `fun interface LanguageSettingsMode { fun usesSystemPage(): Boolean }` với bản mặc định `Build.VERSION.SDK_INT >= TIRAMISU`. Cả hai gắn trong `di/SettingsModule.kt`, test truyền bản giả.

`SettingsViewModel` (`@HiltViewModel`) nhận `UserPreferences`, `AppLinks`, `AppVersionProvider`, `LanguageSettingsMode`, `AppLogger`. Không nhận `Analytics` (spec không có sự kiện) và `FeatureFlags` (cờ chỉ quyết định nút bánh răng trong `:app`, màn này chỉ mở được khi cờ bật). `uiState` = `combine(themeMode, shutterSoundEnabled, appLanguage, uiExtras)` thành `Ready`, `stateIn(WhileSubscribed(5_000), Loading)`; `uiExtras` là `MutableStateFlow` giữ `dialog` và `saveFailed`. `events: Flow<SettingsEvent>` (`Channel(BUFFERED).receiveAsFlow()`). Hàm:

- `onThemeClick()`: `dialog = THEME`. `onThemeSelect(mode)`: đóng hộp; lưu; log "Theme set to <mode>". `onDialogDismiss()`: đóng hộp, không lưu.
- `onShutterSoundChange(enabled)`: lưu; log.
- `onLanguageClick()`: `usesSystemPage()` thì phát `OpenSystemLanguageSettings`, log; không thì `dialog = LANGUAGE`.
- `onLanguageSelect(tag)`: đóng hộp; `tag` trùng giá trị đang lưu thì thôi; lưu; thành công thì phát `ApplyLanguage`, log "Language set to <tag>" (thẻ ngôn ngữ không phải dữ liệu cá nhân).
- `onPrivacyOptionsClick()`: chỉ log; hộp thoại do `PrivacyOptionsButton` mở.
- `onPrivacyPolicyClick()`: đọc `appLinks.privacyPolicyUrl.first()` (giá trị hiện tại lúc bấm), phát `OpenUrl`, log (không ghi đường dẫn).
- `onExternalPageUnavailable(page: String)`: log `warn` (máy không có trình duyệt, hay không có trang ngôn ngữ).
- `onSaveErrorShown()`: `saveFailed = false`.
- Mọi lần lưu: `try { ... } catch (e: IOException) { logger.warn(...); saveFailed = true }`. Màn hình báo bằng `Snackbar` chữ `settings_save_error` (mục 8 câu 4).

`SettingsRoute(onBack, viewModel = hiltViewModel())`:

- Thu `events` trong `repeatOnLifecycle(STARTED)` (ghi chú rà soát của T-013): `OpenSystemLanguageSettings` mở `Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))`; `OpenUrl` mở `Intent(Intent.ACTION_VIEW, url.toUri())`; `ActivityNotFoundException` thì gọi `onExternalPageUnavailable`. `ApplyLanguage` gọi `LocalActivity.current?.recreate()`: activity dựng lại, `attachBaseContext` đọc ngôn ngữ mới, back stack được khôi phục nên vẫn ở màn cài đặt.
- Truyền `versionName`, các hàm, và `privacyOptionsEntry = { row -> PrivacyOptionsButton(content = row) }`.

`SettingsScreen(uiState, onBack, onThemeClick, onThemeSelect, onShutterSoundChange, onLanguageClick, onLanguageSelect, onPrivacyOptionsClick, onPrivacyPolicyClick, onDialogDismiss, onSaveErrorShown, modifier, privacyOptionsEntry: @Composable (row: @Composable (onClick: () -> Unit) -> Unit) -> Unit = {}, dialogInWindow: Boolean = true)` không trạng thái. Test truyền `privacyOptionsEntry = { row -> row(onClick) }` để thấy dòng, để trống để thấy dòng ẩn. Bố cục (khung 411 x 891):

| Phần | Thành phần, màu | Kích thước, khoảng cách |
| --- | --- | --- |
| Nền | `background`, `testTag(SCREEN)` | Cả màn |
| Thanh trên | `TopAppBar` của Material 3: nút `IconButton` biểu tượng `arrow_back` (mô tả "Quay lại") gọi `onBack`; tiêu đề `titleLarge` `onSurface`; nền `background` | Cao 64. Ảnh thiết kế có lề trên 24 là chỗ của thanh trạng thái; trên máy `safeDrawingPadding` của `AppNavHost` lo phần này, nên màn không thêm lề (mục 8 câu 8) |
| Nội dung | `Column` `verticalScroll`, `testTag(LIST)` | Nhóm "Chung": `SectionHeader`, dòng Giao diện (`Chevron`, giá trị theo `themeMode`), Âm khi chụp (`Switch`), Ngôn ngữ (`Chevron`, giá trị `settings_language_current`). Nhóm "Quyền riêng tư": dòng Tùy chọn quyền riêng tư (`Link`, trong `privacyOptionsEntry`; bấm thì gọi `onPrivacyOptionsClick` rồi `onClick` của `:core:ads`), Chính sách quyền riêng tư (`Link`). Nhóm "Thông tin": Phiên bản (`None`, giá trị `version`) |
| Biểu tượng đầu dòng | `palette`, `volume_up`, `language`, `shield_person`, `policy`, `info` | 24 |
| Hộp chọn | `dialog == THEME`: `ChoiceDialog` tiêu đề "Giao diện", ba lựa chọn theo thứ tự `SYSTEM`, `LIGHT`, `DARK`. `dialog == LANGUAGE`: tiêu đề "Ngôn ngữ", "Theo hệ thống" rồi 7 tên ngôn ngữ theo thứ tự `AppLanguages.RELEASE` | `dialogInWindow = true`: trong `Dialog` (`usePlatformDefaultWidth = false`, `decorFitsSystemWindows = false`, `setDimAmount(0f)`), như `NetworkDialogHost`, để lớp mờ phủ cả thanh trạng thái và Back đóng hộp. Screenshot test truyền `false` để vẽ ngay trong màn |
| Thông báo lỗi lưu | `SnackbarHost` ở đáy | Hiện khi `saveFailed`, xong gọi `onSaveErrorShown` |
| `Loading` | Chỉ thanh trên | |

Tên 7 ngôn ngữ trong hộp chọn là chuỗi `translatable="false"` trong `values/strings.xml`: "English", "हिन्दी", "Português (Brasil)", "Español (Latinoamérica)", "Bahasa Indonesia", "ไทย", "Tiếng Việt".

**Chữ.** Theo cách của T-008: `values/` là tiếng Anh, thêm `values-vi`, `values-hi`, `values-pt-rBR`, `values-b+es+419`, `values-in`, `values-th`. Chữ tiếng Việt đúng bảng chữ của README mục 2. AI dịch, người biết tiếng duyệt trước khi phát hành (mục 8 câu 3).

| Khóa | en | vi | hi | pt-BR | es-419 | in | th |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `settings_title` | Settings | Cài đặt | सेटिंग | Configurações | Configuración | Setelan | การตั้งค่า |
| `settings_back` | Back | Quay lại | वापस | Voltar | Volver | Kembali | กลับ |
| `settings_section_general` | General | Chung | सामान्य | Geral | General | Umum | ทั่วไป |
| `settings_theme` | Theme | Giao diện | थीम | Tema | Tema | Tema | ธีม |
| `settings_theme_system` | System default | Theo hệ thống | सिस्टम के अनुसार | Padrão do sistema | Según el sistema | Ikuti sistem | ตามระบบ |
| `settings_theme_light` | Light | Sáng | लाइट | Claro | Claro | Terang | สว่าง |
| `settings_theme_dark` | Dark | Tối | डार्क | Escuro | Oscuro | Gelap | มืด |
| `settings_shutter_sound` | Shutter sound | Âm khi chụp | शटर की आवाज़ | Som do obturador | Sonido del obturador | Suara rana | เสียงชัตเตอร์ |
| `settings_language` | Language | Ngôn ngữ | भाषा | Idioma | Idioma | Bahasa | ภาษา |
| `settings_language_system` | System default | Theo hệ thống | सिस्टम के अनुसार | Padrão do sistema | Según el sistema | Ikuti sistem | ตามระบบ |
| `settings_language_current` | English | Tiếng Việt | हिन्दी | Português (Brasil) | Español (Latinoamérica) | Bahasa Indonesia | ไทย |
| `settings_section_privacy` | Privacy | Quyền riêng tư | निजता | Privacidade | Privacidad | Privasi | ความเป็นส่วนตัว |
| `settings_privacy_options` | Privacy options | Tùy chọn quyền riêng tư | निजता विकल्प | Opções de privacidade | Opciones de privacidad | Opsi privasi | ตัวเลือกความเป็นส่วนตัว |
| `settings_privacy_policy` | Privacy policy | Chính sách quyền riêng tư | निजता नीति | Política de privacidade | Política de privacidad | Kebijakan privasi | นโยบายความเป็นส่วนตัว |
| `settings_section_about` | About | Thông tin | जानकारी | Sobre | Información | Tentang | เกี่ยวกับ |
| `settings_version` | Version | Phiên bản | वर्शन | Versão | Versión | Versi | เวอร์ชัน |
| `settings_cancel` | Cancel | Hủy | रद्द करें | Cancelar | Cancelar | Batal | ยกเลิก |
| `settings_save_error` | Couldn't save your choice. Try again. | Không lưu được lựa chọn. Hãy thử lại. | आपकी पसंद सेव नहीं हो सकी। फिर से कोशिश करें। | Não foi possível salvar sua escolha. Tente de novo. | No se pudo guardar tu elección. Vuelve a intentarlo. | Pilihan tidak dapat disimpan. Coba lagi. | บันทึกตัวเลือกไม่สำเร็จ ลองอีกครั้ง |

**`:app`.**

- `settings/AppAppearance.kt`: `internal fun ThemeMode.isDark(systemDark: Boolean): Boolean` (`SYSTEM` theo máy, `LIGHT` là `false`, `DARK` là `true`).
- `settings/AppLanguageContext.kt`: `internal fun languageToApply(sdkInt: Int, storedTag: String): String?` (từ Android 13 hoặc thẻ không thuộc `AppLanguages.RELEASE` hoặc rỗng thì `null`); `internal fun Context.withAppLanguage(tag: String?): Context` (`null` thì trả chính nó, không thì `createConfigurationContext` với `LocaleList.forLanguageTags(tag)`); `@EntryPoint @InstallIn(SingletonComponent::class) internal interface UserPreferencesEntryPoint { fun userPreferences(): UserPreferences }`; `internal fun readStoredLanguage(context: Context): String` chỉ chạy dưới Android 13: `runBlocking { withTimeoutOrNull(LANGUAGE_READ_TIMEOUT_MILLIS = 500) { appLanguage.first() } } ?: ""`. Đọc chặn vì `attachBaseContext` chạy trước khi Hilt tiêm và cần ngôn ngữ ngay; DataStore chỉ đọc một file nhỏ (mục 8 câu 5).
- `MainActivity.kt`: tiêm `UserPreferences` và `FeatureFlags`. `attachBaseContext(newBase)` gọi `super.attachBaseContext(newBase.withAppLanguage(languageToApply(SDK_INT, readStoredLanguage(newBase))))` (dưới Android 13 mới đọc). Trong `setContent`: `themeMode` từ `userPreferences.themeMode.collectAsState(initial = <giá trị đọc trước>)`, `darkTheme = themeMode.isDark(isSystemInDarkTheme())`; `DisposableEffect(darkTheme)` gọi lại `enableEdgeToEdge(SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { darkTheme }, SystemBarStyle.auto(DefaultLightScrim, DefaultDarkScrim) { darkTheme })` để biểu tượng thanh trạng thái đổi màu theo lựa chọn của app, không theo máy (cách của Now in Android); `AppTheme(darkTheme = darkTheme)`; `NetworkDialogHost(darkTheme = darkTheme, ...)` (hiện mặc định theo máy, sẽ lệch khi người dùng chọn khác máy); `settingsEnabled` từ `featureFlags.enabled(FeatureFlag.SETTINGS).collectAsState(initial = false)` truyền cho `AppNavHost`. Giá trị đầu của `themeMode` đọc một lần trong `onCreate` bằng `runBlocking { withTimeoutOrNull(200) { themeMode.first() } } ?: SYSTEM`, để lần vẽ đầu không chớp sáng rồi tối (mục 8 câu 6). Giữ nguyên phần đồng ý quảng cáo, `NetworkDialogHost`, `onUserInteraction`, mọi thứ T-011, T-013 đã có.
- `navigation/AppNavHost.kt`: tham số mới `settingsEnabled: Boolean = false`. `val openSettings: (() -> Unit)? = if (settingsEnabled) { { navController.navigate(SettingsDestination) { launchSingleTop = true } } } else null`; `cameraScreen(onPhotoCaptured = {}, onOpenSettings = openSettings)`; `settingsScreen(onBack = { navController.popBackStack() })`; nếu `posesScreen` có `onOpenSettings` thì truyền `openSettings` (mục 6). Sửa chú thích "T-021 and T-025 wire ..." cho đúng phần còn lại. Không đổi phần chọn màn chính, `popUpTo`, `onPastLoadingChanged`.
- `res/xml/locales_config.xml` (mới) và `android:localeConfig="@xml/locales_config"` trên `<application>` của manifest. Không thêm quyền.

**Camera (`:feature:camera`).** `CameraController.takePicture(playShutterSound: Boolean): File`. `CameraXController` chỉ nạp và phát `MediaActionSound` khi `playShutterSound`. `CameraViewModel` nhận thêm `UserPreferences`; `init` thu `shutterSoundEnabled` vào một biến (mặc định `true` trước khi đọc xong), `capture()` truyền biến này. Không đổi gì khác của camera.

**Thứ tự làm.** Bước 3: tạo module, khung rỗng (mục 4), mọi test, `strings.xml` đủ 7 ngôn ngữ; chạy để thấy thất bại đúng lý do; commit. Bước 4: `:core:data`, `:core:config`, `:core:ads`; thành phần core và catalog; ViewModel, màn hình, biểu tượng; camera; `:app`; `scripts/check.sh` ghi ảnh chuẩn mới; `scripts/design-compare.sh settings`; build `stagingDebug`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `settings.gradle.kts` | `include(":feature:settings")` |
| Tạo | `feature/settings/build.gradle.kts` | Plugin `template.android.feature`, namespace, `implementation(project(":core:data"))` |
| Tạo | `feature/settings/consumer-rules.pro` | Rỗng, như các feature khác |
| Tạo | `feature/settings/third_party/material-symbols/LICENSE.txt` | Giấy phép Apache 2.0 của Material Symbols |
| Tạo | `feature/settings/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsScreen.kt` | `SettingsRoute`, `SettingsScreen`, `SettingsTestTags` |
| Tạo | `feature/settings/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsUiState.kt` | `SettingsUiState`, `SettingsDialog`, `SettingsEvent` |
| Tạo | `feature/settings/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsViewModel.kt` | ViewModel theo mục 3.1 |
| Tạo | `feature/settings/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/data/AppVersionProvider.kt` | Interface và bản đọc `PackageManager` |
| Tạo | `feature/settings/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/data/LanguageSettingsMode.kt` | Interface và bản theo `SDK_INT` |
| Tạo | `feature/settings/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/di/SettingsModule.kt` | Gắn hai interface trên |
| Tạo | `feature/settings/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/navigation/SettingsNavigation.kt` | `SettingsDestination`, `settingsScreen(onBack)` |
| Tạo | `feature/settings/src/main/res/values/strings.xml` | Chữ tiếng Anh, 7 tên ngôn ngữ `translatable="false"` |
| Tạo | `feature/settings/src/main/res/values-vi/strings.xml` | Chữ tiếng Việt (bảng chữ của thiết kế) |
| Tạo | `feature/settings/src/main/res/values-hi/strings.xml` | Chữ Hindi |
| Tạo | `feature/settings/src/main/res/values-pt-rBR/strings.xml` | Chữ Bồ Đào Nha (Brazil) |
| Tạo | `feature/settings/src/main/res/values-b+es+419/strings.xml` | Chữ Tây Ban Nha (Mỹ Latinh) |
| Tạo | `feature/settings/src/main/res/values-in/strings.xml` | Chữ Indonesia |
| Tạo | `feature/settings/src/main/res/values-th/strings.xml` | Chữ Thái |
| Tạo | `feature/settings/src/main/res/drawable/ic_settings_arrow_back.xml`, `ic_settings_palette.xml`, `ic_settings_volume_up.xml`, `ic_settings_language.xml`, `ic_settings_shield_person.xml`, `ic_settings_policy.xml`, `ic_settings_info.xml` | 7 biểu tượng Material Symbols Rounded |
| Tạo | `feature/settings/src/test/resources/robolectric.properties` | `sdk=35`, như các feature khác |
| Tạo | `feature/settings/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/MainDispatcherRule.kt` | Chép từ `:feature:sample` |
| Tạo | `feature/settings/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsFakes.kt` | `FakeUserPreferences`, `FakeAppLinks` |
| Tạo | `feature/settings/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsViewModelTest.kt` | Unit test ViewModel (mục 4) |
| Tạo | `feature/settings/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsScreenTest.kt` | Test giao diện Robolectric |
| Tạo | `feature/settings/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsStringsTest.kt` | Đủ 7 ngôn ngữ, tên ngôn ngữ khớp |
| Tạo | `feature/settings/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsScreenScreenshotTest.kt` | Bảy screenshot test |
| Tạo | `feature/settings/src/test/screenshots/SettingsScreen_default_light.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/settings/src/test/screenshots/SettingsScreen_default_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/settings/src/test/screenshots/SettingsScreen_theme-picker_light.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/settings/src/test/screenshots/SettingsScreen_theme-picker_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/settings/src/test/screenshots/SettingsScreen_language-picker_light.png` | Hộp chọn ngôn ngữ (không có ảnh thiết kế) |
| Tạo | `feature/settings/src/test/screenshots/SettingsScreen_default_small.png` | Khung 360 x 640 (L-028) |
| Tạo | `feature/settings/src/test/screenshots/SettingsScreen_language-picker_small.png` | Khung 360 x 640, hộp chọn nhiều lựa chọn nhất (L-028) |
| Tạo | `docs/features/settings/design/review/` | Sinh bởi `scripts/design-compare.sh settings` (ảnh ghép của bốn ảnh và `summary.md`), không sửa tay |
| Tạo | `core/data/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/data/UserPreferences.kt` | `ThemeMode`, `AppLanguages`, `UserPreferences` |
| Tạo | `core/data/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/data/DataStoreUserPreferences.kt` | Cài đặt bằng DataStore, ba khóa `settings_*` |
| Tạo | `core/data/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/data/di/UserPreferencesModule.kt` | `@Binds` |
| Tạo | `core/data/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/data/DataStoreUserPreferencesTest.kt` | Unit test repository |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FeatureFlag.kt` | `SETTINGS("feature_settings_enabled")` |
| Tạo | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/AppLinks.kt` | `AppLinks`, `DefaultAppLinks`, khóa `privacy_policy_url` và giá trị mặc định |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/di/ConfigModule.kt` | Gắn `AppLinks` |
| Tạo | `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/DefaultAppLinksTest.kt` | Unit test, dùng `FakeRemoteFlagSource` có sẵn |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdSlot.kt` | Bản `PrivacyOptionsButton(modifier, content)`; bản cũ gọi bản mới |
| Tạo | `core/ads/src/test/resources/robolectric.properties` | `sdk=35` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/PrivacyOptionsButtonTest.kt` | Không có `AdsController` thì không vẽ gì |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/SettingsRow.kt` | `SettingsRow`, `SettingsRowTrailing`, `SectionHeader`, `SettingsRowTestTags` |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/ChoiceDialog.kt` | `ChoiceDialog`, `RadioRow`, `ChoiceDialogTestTags` |
| Tạo | `core/designsystem/src/main/res/drawable/ic_chevron_right.xml` | Biểu tượng Material Symbols Rounded |
| Tạo | `core/designsystem/src/main/res/drawable/ic_open_in_new.xml` | Biểu tượng Material Symbols Rounded |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/catalog/DesignSystemCatalog.kt` | Thêm `SettingsComponentCatalog`; trang cũ không đổi |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/SettingsComponentsTest.kt` | Test giao diện bốn thành phần (file riêng, không chạm `ComponentsTest.kt`, `CameraComponentsTest.kt`) |
| Sửa | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/DesignSystemCatalogScreenshotTest.kt` | Thêm `settingsComponents_light`, `settingsComponents_dark` |
| Tạo | `core/designsystem/src/test/screenshots/DesignSystem_settings_components_light.png` | Ảnh chuẩn mới |
| Tạo | `core/designsystem/src/test/screenshots/DesignSystem_settings_components_dark.png` | Ảnh chuẩn mới |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraController.kt` | `takePicture(playShutterSound: Boolean)` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraXController.kt` | Chỉ phát âm chụp khi được yêu cầu |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModel.kt` | Nhận `UserPreferences`, truyền lựa chọn âm chụp |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraFakes.kt` | `FakeCameraController` ghi `soundCalls`; `FakeUserPreferences` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModelTest.kt` | Hàm dựng ViewModel truyền `FakeUserPreferences`; hai test mới (mục 4) |
| Sửa | `app/build.gradle.kts` | `implementation(project(":feature:settings"))` |
| Sửa | `app/src/main/AndroidManifest.xml` | `android:localeConfig="@xml/locales_config"` trên `<application>` |
| Tạo | `app/src/main/res/xml/locales_config.xml` | 7 ngôn ngữ phát hành |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | Giao diện theo lựa chọn, thanh hệ thống theo giao diện, ngôn ngữ dưới Android 13, cờ cài đặt |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/settings/AppAppearance.kt` | `ThemeMode.isDark` |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/settings/AppLanguageContext.kt` | `languageToApply`, `withAppLanguage`, `UserPreferencesEntryPoint`, `readStoredLanguage` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/AppNavHost.kt` | `settingsEnabled`, đăng ký `settingsScreen`, nối `onOpenSettings` của camera (và thư viện nếu có) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingFeatureFlagsModule.kt` | Bật `FeatureFlag.SETTINGS` ở staging |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/settings/AppAppearanceTest.kt` | Unit test |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/settings/AppLanguageContextTest.kt` | Unit test và Robolectric |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/settings/LocalesConfigTest.kt` | `locales_config.xml` khớp `AppLanguages.RELEASE` |
| Tạo | `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt` | Mở cài đặt từ camera, quay lại |

Không file nào khác. Đặc biệt không sửa `gradle/libs.versions.toml`, `build-logic/`, `core/data/build.gradle.kts`, `core/ads/build.gradle.kts`, `AdsController.kt`, `RemoteSettings.kt`, `Theme.kt`, `ComponentsTest.kt`, `CameraComponentsTest.kt`, `CameraScreen.kt`, `AppLaunchTest.kt`, `docs/design-system/tokens.json` (không thêm giá trị thiết kế mới).

**Nơi khác đọc các file trên** (L-020), không cần sửa:

- `FeatureFlag.kt`: `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/DefaultFeatureFlagsTest.kt` duyệt `FeatureFlag.entries`; cờ mới mặc định tắt nên `everyFlag_isOffByDefault` tự kiểm tra và vẫn qua.
- `ConfigModule.kt`: chỉ Hilt đọc lúc build; thêm một `@Binds`.
- `AdSlot.kt`: chưa có test nào gọi `PrivacyOptionsButton`; bản cũ giữ chữ ký.
- `DesignSystemCatalog.kt`: chỉ `DesignSystemCatalogScreenshotTest.kt` (trong danh sách) gọi; trang cũ không đổi nên các ảnh catalog cũ không đổi.
- `CameraController.kt`, `CameraViewModel.kt`: `CameraFakes.kt`, `CameraViewModelTest.kt` (trong danh sách) cài đặt và dựng; `CameraScreenTest.kt`, `CameraScreenScreenshotTest.kt` chỉ dùng `CameraScreen` không trạng thái, không đổi.
- `AppNavHost.kt`, `MainActivity.kt`: `AppLaunchTest.kt` mở `MainActivity` và chờ `camera_screen`; màn đích không đổi nên không sửa. `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/navigation/MainScreenTest.kt` không gọi `AppNavHost`. `NetworkDialogHostTest.kt` gọi `NetworkDialogHost` với tham số riêng, không qua `MainActivity`.
- `settings.gradle.kts`, `app/build.gradle.kts`: `checkModuleDependencies` đọc phụ thuộc lúc build; `:feature:settings` chỉ phụ thuộc core nên qua. `scripts/release.sh` chỉ đọc `applicationId`, `versionCode`, `versionName` trong `app/build.gradle.kts`; T-025 chỉ thêm một dòng `implementation`. `scripts/new-app.sh` (ADR-0017) sửa hai file này theo tên app, không theo danh sách module.
- `AndroidManifest.xml`: chỉ thêm thuộc tính, không thêm quyền. `scripts/app-explore/apk-screens.py` đọc manifest của APK app mẫu, không phải của repo.
- `StagingFeatureFlagsModule.kt`: chỉ Hilt đọc.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: `feature_settings_enabled` (spec mục 8), mặc định tắt, bật sẵn ở staging. `MainActivity` theo dõi `FeatureFlags.enabled(FeatureFlag.SETTINGS)` bằng `collectAsState` (ADR-0003); cờ tắt thì nút bánh răng ẩn.
- Quảng cáo: không thêm vị trí. Màn cài đặt không có quảng cáo (spec mục 5). Chạm hộp thoại đồng ý (điểm chặn 5) theo nghĩa mở lại hộp thoại có sẵn của `:core:ads` từ một chỗ mới, qua bản mới của `PrivacyOptionsButton`; không đổi thứ tự đồng ý, khởi tạo SDK, tham số đồng ý.
- Sự kiện phân tích: Không chạm (spec mục 6).
- Liên kết ra ngoài (điểm chặn 6): trang chính sách quyền riêng tư của studio, đường dẫn từ Remote Config, mặc định https://studio.vgl.io.vn/privacy-policy. Không có quảng bá chéo.
- Dữ liệu cục bộ: ba khóa DataStore mới `settings_theme_mode`, `settings_shutter_sound`, `settings_app_language` (spec mục 6). Khóa mới, không trùng khóa nào hiện có (`sample_count`, `camera_*`, khóa của `:core:poses` và `:feature:loading` không có tiền tố `settings_`), không chuyển dữ liệu cũ, nên không chạm điểm chặn 9. Không gì rời khỏi máy.
- File R2: Không chạm.

## 4. Kiểm thử

Mọi test dưới đây vào commit test của bước 3, cùng khung rỗng để biên dịch được: `DataStoreUserPreferences` trả mặc định và không ghi; `DefaultAppLinks` trả chuỗi rỗng; bản `PrivacyOptionsButton(modifier, content)` đã có chữ ký nhưng luôn vẽ `content` (để test không có controller thất bại đúng lý do); `SettingsRow`, `SectionHeader`, `ChoiceDialog`, `RadioRow` vẽ `Box(modifier)` rỗng; `SettingsComponentCatalog` là `Box` rỗng; `SettingsViewModel` có đủ hàm nhưng không làm gì, `uiState` luôn `Loading`, `events` rỗng; `SettingsScreen` chỉ vẽ `Box` mang `testTag(SCREEN)`; `CameraController.takePicture(playShutterSound)` đã có tham số, `CameraViewModel` đã nhận `UserPreferences` nhưng luôn truyền `true`; `isDark` luôn `false`; `languageToApply` luôn `null`; `withAppLanguage` trả chính nó; `locales_config.xml` chỉ có `en`. `strings.xml` đủ 7 ngôn ngữ ngay ở bước 3. Hàm dựng ViewModel của `CameraViewModelTest`, `FakeUserPreferences` và tham số mới của `FakeCameraController.takePicture` vào commit test của bước 3 (L-025). Lý do thất bại ở cột cuối là điều AI dự kiến; bước 3 chạy và ghi lý do thật vào nhật ký.

Bản giả: `FakeUserPreferences` (một bản trong mỗi module test cần: `:feature:settings`, `:feature:camera`) có ba `MutableStateFlow`, ghi lại lần lưu, `failWrites` ném `IOException`. `FakeAppLinks` là `MutableStateFlow("https://studio.vgl.io.vn/privacy-policy")`. ViewModel dựng với `AppVersionProvider { "1.0.0" }` và `LanguageSettingsMode { systemPage }`. Kiểm tra lỗi so loại (`assertTrue(entry.error is IOException)`).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-2, mặc định và giữ lựa chọn | Unit test | `DataStoreUserPreferencesTest` (DataStore trong bộ nhớ, như `DataStoreCounterRepositoryTest`): `nothingStored_returnsDefaults` (`SYSTEM`, `true`, `""`); `eachSetter_isReadBack` (`DARK`, `false`, `"vi"`); `unknownStoredValue_fallsBackToDefault` (`settings_theme_mode = "SEPIA"` thành `SYSTEM`, `settings_app_language = "fr"` thành `""`); `readFailsOnce_recovers`. Thất bại: khung rỗng không ghi, `eachSetter_isReadBack` nhận mặc định |
| KB-3, mặc định bật và giữ lựa chọn | Unit test | Cùng các test trên (`shutterSoundEnabled`) |
| KB-5, đường dẫn mặc định | Unit test | `DefaultAppLinksTest.withoutRemoteValue_usesDefault`: `"https://studio.vgl.io.vn/privacy-policy"`. `remoteValue_replacesDefault`: `privacy_policy_url = "https://example.com/p"` thì ra giá trị đó. `invalidRemoteValue_usesDefault`: `""` và `"http://x"` ra mặc định. Thất bại: khung rỗng trả `""` |
| KB-1, mở cài đặt đúng nội dung | Unit test | `SettingsViewModelTest.ready_reflectsStoredChoices_andVersion`: preferences `DARK`, `false`, `""`; `uiState == Ready(DARK, false, "", "1.0.0")`. Thất bại: luôn `Loading` |
| KB-2, hộp chọn và lựa chọn mới | Unit test | `themeClick_opensDialog_selectSavesAndCloses`: `onThemeClick()`: `dialog == THEME`; `onThemeSelect(LIGHT)`: `dialog == null`, preferences lưu `[LIGHT]`, `themeMode == LIGHT`; log `INFO` thẻ `Settings`. `themeDialog_dismiss_savesNothing` |
| KB-3, đổi âm khi chụp | Unit test | `shutterSoundChange_isSaved`: `onShutterSoundChange(false)`; lưu `[false]`; `shutterSoundEnabled == false` |
| KB-4, Android 13 trở lên | Unit test | `languageClick_withSystemPage_opensSystemSettings`: `systemPage = true`; `events == [OpenSystemLanguageSettings]`; `dialog == null` |
| KB-4, dưới Android 13 | Unit test | `languageClick_withoutSystemPage_opensDialog`: `dialog == LANGUAGE`. `languageSelect_savesAndApplies`: `onLanguageSelect("hi")`: lưu `["hi"]`, `events == [ApplyLanguage]`, `dialog == null`. `languageSelect_sameAsSaved_doesNothing`: đang `"hi"`, chọn `"hi"`: không lưu, không sự kiện |
| KB-5, chính sách | Unit test | `privacyPolicyClick_opensCurrentUrl`: `FakeAppLinks` đổi sang `"https://example.com/p"` trước khi bấm; `events == [OpenUrl("https://example.com/p")]` |
| KB-5, mở trang ngoài lỗi | Unit test | `externalPageUnavailable_logsWarning`: log `WARN` thẻ `Settings` |
| Lưu lỗi không làm app dừng, người dùng được báo | Unit test | `saveFails_setsSaveFailed_andLogsWarning`: `failWrites = true`, `onThemeSelect(DARK)`; `saveFailed == true`, `themeMode == SYSTEM`; log `WARN` với `error is IOException`; `onSaveErrorShown()`: `saveFailed == false`. `languageSaveFails_doesNotApply`: không có `ApplyLanguage` |
| KB-1, đúng thiết kế, không quảng cáo | Compose UI test | `SettingsScreenTest.ready_showsAllRowsInOrder`: trong `vi`, các chữ "Cài đặt", "Chung", "Giao diện", "Theo hệ thống", "Âm khi chụp", "Ngôn ngữ", "Tiếng Việt", "Quyền riêng tư", "Tùy chọn quyền riêng tư", "Chính sách quyền riêng tư", "Thông tin", "Phiên bản", "1.0.0" có mặt, đỉnh của từng dòng tăng dần đúng thứ tự trên. Không có quảng cáo: rà soát độc lập kiểm tra `feature/settings` không gọi `AdSlot`, `rememberShowInterstitial` (bước 6) |
| KB-1, quay lại | Compose UI test | `back_callsOnBack`: bấm nút mô tả "Quay lại" gọi `onBack` một lần |
| KB-2, hộp chọn đánh dấu lựa chọn hiện tại | Compose UI test | `themeDialog_marksCurrent_andSelects`: `Ready(themeMode = DARK, dialog = THEME)`; ba `RadioRow` "Theo hệ thống", "Sáng", "Tối"; "Tối" `assertIsSelected`, hai cái kia `assertIsNotSelected`; bấm "Sáng" gọi `onThemeSelect(LIGHT)`; bấm "Hủy" gọi `onDialogDismiss`. `themeRow_showsCurrentChoice`: `LIGHT` thì dòng ghi "Sáng"; bấm dòng gọi `onThemeClick` |
| KB-3, công tắc | Compose UI test | `shutterSwitch_reflectsAndToggles`: `shutterSoundEnabled = true` thì dòng "Âm khi chụp" `assertIsOn`; bấm gọi `onShutterSoundChange(false)` |
| KB-4, dòng ngôn ngữ ghi đúng ngôn ngữ | Compose UI test | `languageRow_showsLanguageInUse_vi` (`@Config(qualifiers = "vi")`): "Tiếng Việt"; `_hi` (`hi`): "हिन्दी"; `_unsupportedLocale_showsEnglish` (`fr`): "English"; bấm dòng gọi `onLanguageClick` |
| KB-4, hộp chọn ngôn ngữ | Compose UI test | `languageDialog_listsSystemAndSevenLanguages`: `dialog = LANGUAGE`, `languageTag = "th"`; có "Theo hệ thống" và đủ 7 tên; "ไทย" `assertIsSelected`; bấm "Español (Latinoamérica)" gọi `onLanguageSelect("es-419")`; bấm "Theo hệ thống" gọi `onLanguageSelect("")` |
| KB-4, đủ 7 ngôn ngữ | Unit test (Robolectric) | `SettingsStringsTest.everyReleaseLanguage_isTranslated`: với `hi`, `pt-BR`, `es-419`, `id`, `th`, `vi`, mọi khóa trừ tên ngôn ngữ không rỗng và khác chữ của `en`. `currentLanguageName_matchesDialogName`: ở mỗi ngôn ngữ trong `AppLanguages.RELEASE`, `settings_language_current` bằng đúng tên của ngôn ngữ đó trong hộp chọn. Thất bại ở bước 3: không, vì `strings.xml` đủ ngay ở bước 3; giữ làm test chặn, ghi vào nhật ký |
| KB-5, dòng tùy chọn quyền riêng tư hiện và ẩn | Compose UI test | `privacyOptions_shownWhenEntryProvided_andClicks`: `privacyOptionsEntry = { row -> row(adsClick) }`; dòng "Tùy chọn quyền riêng tư" có mặt, có `LINK`; bấm gọi `onPrivacyOptionsClick` rồi `adsClick`, mỗi hàm một lần. `privacyOptions_hiddenWithoutEntry`: không có chữ đó; dòng "Chính sách quyền riêng tư" vẫn có |
| KB-5, `:core:ads` ẩn dòng khi không cần | Compose UI test | `PrivacyOptionsButtonTest.withoutAdsController_rendersNothing`: không có `LocalAdsController`, `content` không được gọi, không có node chữ của `content`. Thất bại: khung rỗng luôn vẽ `content`. Trường hợp có `AdsController` (vùng không cần đồng ý, vùng cần) chỉ kiểm tra thủ công: `AdsController` là lớp thật gọi SDK UMP, không tạo được trên JVM |
| KB-5, chính sách | Compose UI test | `privacyPolicy_clicks`: bấm dòng gọi `onPrivacyPolicyClick`; dòng có `LINK` |
| KB-6, phiên bản | Compose UI test | `versionRow_showsVersion_notClickable_noChevron`: "1.0.0" có mặt; node gộp của dòng "Phiên bản" `assertHasNoClickAction`; trong dòng không có `CHEVRON`, `LINK`, `SWITCH` |
| Lưu lỗi | Compose UI test | `saveFailed_showsSnackbar_thenReports`: `saveFailed = true`; chữ "Không lưu được lựa chọn. Hãy thử lại." hiện; `mainClock.advanceTimeBy(5_000)`: `onSaveErrorShown` được gọi một lần |
| Màn hình thấp (L-028) | Compose UI test, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")` | `small_versionRowReachable`: `performScrollTo()` trên dòng "Phiên bản", `assertIsDisplayed()`, đáy dòng không quá đáy `SCREEN`. `small_languageDialog_allOptionsReachable`: cuộn tới "Tiếng Việt" và nút "Hủy" trong hộp, cả hai `assertIsDisplayed()`; thẻ hộp nằm trọn trong màn |
| Thành phần `SettingsRow` | Compose UI test, `@GraphicsMode(NATIVE)` | `SettingsComponentsTest.settingsRow_isAtLeast64dpTall` (bốn kiểu, khung rộng 411); `settingsRow_chevron_clickable`, `settingsRow_link_clickable`; `settingsRow_switch_togglesWholeRow` (bấm vào chữ cũng gọi `onCheckedChange(true)`, `assertIsOff` trước); `settingsRow_info_notClickable`; `settingsRow_value_showsBelowTitle` (đỉnh giá trị bằng đáy tiêu đề, sai số 1 px); `settingsRow_iconColor_isOnSurfaceVariant` (điểm giữa icon trong `AppTheme(darkTheme = false)` là #5A5650). Thất bại: khung rỗng cao 0, không có node |
| Thành phần `SectionHeader` | Compose UI test, `@GraphicsMode(NATIVE)` | `sectionHeader_is52dpTall_andIsHeading`: cao 52, có `SemanticsProperties.Heading` |
| Thành phần `ChoiceDialog`, `RadioRow` | Compose UI test, `@GraphicsMode(NATIVE)` | `choiceDialog_showsTitleOptionsAndDismiss`; `choiceDialog_selectsAndDismisses` (bấm lựa chọn gọi `onSelect(i)`, bấm "Hủy" và bấm lớp mờ ngoài thẻ đều gọi `onDismiss`); `choiceDialog_cardWidth_isAtMost331dp` trong khung 411; `radioRow_is48dpTall_andSelectable`; `radioRow_selectedFill_isSecondaryContainer` (điểm cách tâm nút chọn 8 dp là #FFC83D khi chọn, #FFFFFF khi không, sáng); `choiceDialog_scrim_isHalfBlack` (điểm (5 dp, 5 dp) trên nền trắng là #7F7F7F, sai số 2) |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | `ready_showsAllRowsInOrder` và `themeDialog_marksCurrent_andSelects` so đúng chuỗi của README mục 2 (trong `vi`) |
| Giao diện sáng tối theo lựa chọn | Unit test | `AppAppearanceTest.system_followsDevice` (`SYSTEM.isDark(true) == true`, `SYSTEM.isDark(false) == false`); `light_isNeverDark`; `dark_isAlwaysDark`. Thất bại: khung rỗng luôn `false` ở hai trường hợp |
| KB-2, máy đổi chế độ lúc app mở | Kiểm tra thủ công | Chọn "Theo hệ thống", mở cài đặt; kéo thanh thông báo đổi chế độ tối của máy: màn cài đặt đổi màu, vẫn ở màn cài đặt |
| KB-2, đổi ngay, thanh hệ thống | Kiểm tra thủ công | Máy để sáng, chọn "Tối": màn cài đặt đổi tối ngay, biểu tượng thanh trạng thái màu sáng; về camera: nền camera tối; mở lại app: vẫn tối, không chớp sáng ở màn loading |
| KB-4, dưới Android 13, áp ngôn ngữ | Unit test và Robolectric | `AppLanguageContextTest.languageToApply_belowAndroid13_usesStoredTag` (`32`, `"vi"` ra `"vi"`); `_fromAndroid13_isNull` (`33`, `"vi"` ra `null`); `_systemOrUnknown_isNull` (`""`, `"fr"`); `withAppLanguage_setsLocale` (Robolectric sdk 35: `context.withAppLanguage("th").resources.configuration.locales[0].language == "th"`); `withAppLanguage_null_returnsSameContext`. Thất bại: khung rỗng trả `null` và chính context |
| KB-4, trang ngôn ngữ của máy có đủ ngôn ngữ | Unit test (Robolectric) | `LocalesConfigTest.listsExactlyReleaseLanguages`: đọc `R.xml.locales_config` bằng `resources.getXml`, tập thuộc tính `android:name` của các thẻ `locale` bằng tập `AppLanguages.RELEASE`. Thất bại: khung chỉ có `en` |
| KB-4 trên máy | Kiểm tra thủ công | Android 13 trở lên: bấm "Ngôn ngữ", trang ngôn ngữ của app mở với 7 ngôn ngữ; chọn "Tiếng Việt", quay lại: màn cài đặt tiếng Việt, dòng ghi "Tiếng Việt". Android 12 trở xuống (emulator API 30 hoặc máy thật): hộp chọn 8 lựa chọn; chọn "ไทย": app dựng lại, vẫn ở màn cài đặt, chữ tiếng Thái; tắt hẳn app mở lại: vẫn tiếng Thái; chọn "Theo hệ thống": về ngôn ngữ máy. Ghi tên máy và bản Android vào báo cáo |
| KB-3, âm chụp | Unit test | `CameraViewModelTest.shutter_soundOn_byDefault_playsSound`: preferences mặc định, chụp; `controller.soundCalls == [true]`. `shutter_soundOff_capturesWithoutSound`: `shutterSoundEnabled = false` rồi chụp; `soundCalls == [false]`, ảnh vẫn chụp, vẫn ghi `photo_captured`. Thất bại: khung rỗng luôn truyền `true` |
| KB-3 trên máy | Kiểm tra thủ công | Tắt "Âm khi chụp", chụp: không có tiếng của app; bật lại: có. Ghi chú: máy bán ở nước bắt buộc âm chụp có thể vẫn phát tiếng của hệ thống (mục 8 câu 9) |
| KB-5, hộp thoại đồng ý trên máy | Kiểm tra thủ công | Bản staging (ép vùng EEA, ADR-0013): dòng "Tùy chọn quyền riêng tư" hiện; bấm thì hộp thoại đồng ý hiện lại; đổi lựa chọn, đóng: về màn cài đặt. Bản production ở Việt Nam: dòng không hiện (người kiểm tra ở bản phát hành đầu) |
| KB-5, trình duyệt | Kiểm tra thủ công | Bấm "Chính sách quyền riêng tư": trình duyệt mở https://studio.vgl.io.vn/privacy-policy |
| KB-6 trên máy | Kiểm tra thủ công | Dòng "Phiên bản" ghi đúng `versionName` của bản staging (ví dụ "0.1.0-staging.42") |
| Giao diện catalog | Screenshot test | `DesignSystemCatalogScreenshotTest.settingsComponents_light` ghi `src/test/screenshots/DesignSystem_settings_components_light.png`, `settingsComponents_dark` ghi `src/test/screenshots/DesignSystem_settings_components_dark.png`, khung `w411dp-h891dp-xxhdpi` của lớp. Ảnh catalog cũ không đổi |
| Đúng thiết kế | Screenshot test, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")`, `dialogInWindow = false`, có `privacyOptionsEntry`, phiên bản "1.0.0", nội dung đặt dưới một `Spacer` cao `Spacing.lg` thay cho thanh trạng thái của khung thiết kế (mục 8 câu 8) | `SettingsScreenScreenshotTest`: `default_light` (`SettingsScreen_default_light.png`: `SYSTEM`, âm bật), `default_dark` (`SettingsScreen_default_dark.png`), `themePicker_light` (`SettingsScreen_theme-picker_light.png`: `dialog = THEME`, `SYSTEM` đang chọn), `themePicker_dark` (`SettingsScreen_theme-picker_dark.png`) |
| Hộp chọn ngôn ngữ | Screenshot test | `languagePicker_light` (`SettingsScreen_language-picker_light.png`: `dialog = LANGUAGE`, `languageTag = ""`) |
| Màn hình thấp (L-028) | Screenshot test khung `vi-w360dp-h640dp-xxhdpi` | `default_small` (`SettingsScreen_default_small.png`), `languagePicker_small` (`SettingsScreen_language-picker_small.png`, nhiều nội dung nhất) |
| So với thiết kế | Ảnh so sánh và rà soát độc lập (bước 6) | Chạy `scripts/design-compare.sh settings` sau khi ghi ảnh chuẩn; commit `docs/features/settings/design/review/` và dán `review/summary.md` vào mô tả PR (ADR-0020). Phiên rà soát xem ảnh ghép của bốn ảnh theo mục 7 của README thiết kế. Khác biệt đã biết: khoảng 2 giữa tiêu đề và giá trị bỏ (lệch 1 dp); núm công tắc 24 của Material 3 thay vì 22 |
| KB-1 trên emulator: mở cài đặt từ camera, quay lại | Test trên emulator | `SettingsNavigationTest.cameraGear_opensSettings_backReturns`: cấp quyền camera bằng `UiAutomation` như `AppLaunchTest`; chờ `camera_screen` tối đa 60 giây; `performClick` trên `CameraTestTags.SETTINGS`; `waitUntil(10_000)` có `settings_screen` (`fetchSemanticsNodes()`); `Espresso.pressBack()`; `waitUntil(10_000)` có `camera_screen`. Chỉ kiểm tra trạng thái ổn định. Nếu hộp thoại đồng ý của bản staging phủ lên app làm cú bấm không tới được camera thì xử lý theo mục 7 điều 5 |
| KB-1, từ thư viện | Kiểm tra thủ công (khi T-021 đã có) | Bấm bánh răng ở thư viện: màn cài đặt mở; quay lại: về thư viện |

**Ngoại lệ viết test trước:**

- `SettingsRoute` (mở `Intent`, `recreate()`, thu sự kiện), `PackageManagerAppVersionProvider`, bản `LanguageSettingsMode` theo `SDK_INT`, `readStoredLanguage` và `UserPreferencesEntryPoint`: lớp mỏng gọi hệ thống, cần `hiltViewModel()` hoặc Hilt thật; kiểm chứng bằng test emulator và kiểm tra thủ công. Logic quyết định nằm trong ViewModel và các hàm thuần của `:app`, đều có test trước.
- Phần `MainActivity.kt` (theo dõi lựa chọn, `enableEdgeToEdge` theo giao diện, `attachBaseContext`) và nối điều hướng trong `AppNavHost.kt`: như T-010 và T-013, không có test JVM; kiểm chứng bằng `SettingsNavigationTest`, `AppLaunchTest` trên emulator và kiểm tra thủ công.
- `CameraXController`: không chạy được trên JVM (như T-013); chỉ thêm điều kiện quanh lệnh phát âm.
- 9 biểu tượng, `LICENSE.txt`, thuộc tính `android:localeConfig`: tài nguyên và cấu hình, không có logic.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 5 (hộp thoại đồng ý: thêm bản `PrivacyOptionsButton` mới và mở lại hộp thoại từ màn cài đặt; không đổi thứ tự đồng ý hay khởi tạo SDK), điểm 6 (liên kết ra ngoài: trang chính sách quyền riêng tư), điểm 7 (module core: `:core:data`, `:core:config`, `:core:ads`, `:core:designsystem`; thêm module vào `settings.gradle.kts`). Không chạm điểm 2 (không thêm thư viện, cố ý không dùng AppCompat), điểm 3 (`android:localeConfig` không phải quyền), điểm 4 (không có sự kiện), điểm 9 (khóa DataStore mới, không chuyển dữ liệu cũ). Người duyệt thấy khác thì sửa `blocking_points`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Module mới và sáu module khác (`:core:data`, `:core:config`, `:core:ads`, `:core:designsystem`, `:feature:camera`, `:app`), khoảng 75 file kể cả 7 thư mục chữ, 9 biểu tượng, 9 ảnh chuẩn |
| B. Độ rõ của yêu cầu | 2 | Spec đã xác nhận, thiết kế đã duyệt, nhưng còn câu hỏi ảnh hưởng cách làm: hộp chọn tự vẽ hay của Material 3, thông báo lỗi lưu không có trong thiết kế, ngôn ngữ gốc của chữ, đọc chặn lúc mở app (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Logic và giao diện có test; phần phụ thuộc hệ thống chỉ kiểm tra thủ công: trang ngôn ngữ của máy, dựng lại activity theo ngôn ngữ dưới Android 13, hộp thoại đồng ý UMP, trình duyệt, máy đổi chế độ tối, âm chụp |
| D. Ảnh hưởng chéo | 3 | Đổi hành vi mà mọi màn dựa vào: giao diện sáng tối của cả app, ngôn ngữ của activity, thanh hệ thống, giao diện hộp thoại mất mạng; đổi `CameraController.takePicture`. Sửa `MainActivity.kt`, `AppNavHost.kt`, `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`, `settings.gradle.kts`, `app/build.gradle.kts`, `DesignSystemCatalog.kt`, file camera mà T-012, T-014, T-016, T-021 cũng sửa |
| E. Độ mới của kỹ thuật | 2 | Ngôn ngữ theo app (`localeConfig`, `ACTION_APP_LOCALE_SETTINGS`), gắn ngôn ngữ trong `attachBaseContext` không có AppCompat, Hilt `EntryPoint`, đổi kiểu thanh hệ thống theo giao diện: chưa có trong dự án nhưng phổ biến |
| **Tổng** | 12 | **Mức: Cao** (chạm điểm chặn, và tổng điểm cũng từ 9 trở lên) |

Plan chạm hộp thoại đồng ý: người phụ trách sản phẩm cần biết trước khi duyệt (risk-scoring.md, mục 3).

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `:core:data` (interface `UserPreferences`, ba khóa DataStore mới); `:core:config` (`FeatureFlag.kt`, `AppLinks`, `ConfigModule.kt`); `:core:ads` (`AdSlot.kt`, bản mới của `PrivacyOptionsButton`); `:core:designsystem` (bốn thành phần, hai biểu tượng, một trang catalog, hai ảnh chuẩn; không đổi thành phần có sẵn); `settings.gradle.kts`; `app/build.gradle.kts`; `AndroidManifest.xml`; `MainActivity.kt`; `AppNavHost.kt`; `StagingFeatureFlagsModule.kt`; `CameraController.kt`, `CameraXController.kt`, `CameraViewModel.kt`, `CameraFakes.kt`, `CameraViewModelTest.kt`. Thay đổi ở `:core:*` có ích chung (bản `PrivacyOptionsButton` nhận nội dung, `UserPreferences` giao diện sáng tối, `SettingsRow`, `ChoiceDialog`) được đề xuất về dự án mẫu theo `docs/ops/from-app.md`; ghi điều này vào mô tả PR.
- Task khác trong sprint có thể xung đột (plan đang viết song song):
  - T-012 (màn chào): sửa `MainActivity.kt`, `MainScreenChooser.kt`, `AppNavHost.kt` (đích màn chào), có thể `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`, `settings.gradle.kts`, `app/build.gradle.kts`. T-025 không chạm `MainScreenChooser.kt`. Trong `MainActivity.kt`, T-025 chỉ thêm `attachBaseContext`, hai lần tiêm và phần giao diện quanh `AppTheme`; không đổi chỗ truyền `chooseMainScreen`. Xung đột dòng, không xung đột logic.
  - T-014 (chọn tư thế trên camera): sửa nhiều `CameraViewModel.kt`, `CameraFakes.kt`, `CameraViewModelTest.kt`. T-025 chỉ thêm một tham số constructor, một biến và một đối số `takePicture`. Chạy nối tiếp nào cũng được; task chạy sau dựng trên nhánh chính mới.
  - T-016 (màn xem lại): màn xem lại phải luôn tối (spec settings, mục 2; KB-2): bọc `AppTheme(darkTheme = true)` như `CameraRoute`, và `NetworkDialogHost` nhận `darkTheme` theo màn nếu cần. T-016 sửa `AppNavHost.kt` (đích xem lại) và có thể `CameraViewModel.kt` (`onPhotoCaptured`). Nếu T-016 thêm `ConfirmDialog` vào `:core:designsystem` thì cùng sửa `DesignSystemCatalog.kt`, `DesignSystemCatalogScreenshotTest.kt`.
  - T-021 (thư viện tư thế): tạo module mới, sửa `settings.gradle.kts`, `app/build.gradle.kts`, `AppNavHost.kt`, `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`, `DesignSystemCatalog.kt`, `DesignSystemCatalogScreenshotTest.kt`. Thỏa thuận: `posesScreen` của T-021 có tham số `onOpenSettings: (() -> Unit)? = null` và ẩn nút bánh răng khi `null`, như camera. Chạy sau T-025 thì T-021 truyền `openSettings` của T-025 trong `AppNavHost.kt`; chạy trước thì T-025 truyền (đã nằm trong file `AppNavHost.kt` của danh sách). Tiêu chí "mở từ thư viện" của KB-1 chỉ kiểm tra được khi cả hai đã merge.
  - Các task sau này thêm chữ vào màn khác: màn camera hiện chỉ có chữ tiếng Việt trong `values/`, nên khi người dùng chọn ngôn ngữ khác, camera vẫn tiếng Việt (mục 8 câu 3).
- Đề xuất thứ tự chạy: T-025 độc lập với T-012, T-014, T-016, T-021 về logic. Đề xuất chạy sau T-012 và T-014 (hai task này sửa nhiều `MainActivity.kt` và `CameraViewModel.kt` hơn; T-025 sửa ít dòng hơn nên dễ dựng lại trên nhánh chính mới), trước hoặc sau T-021 đều được nhờ thỏa thuận `onOpenSettings` ở trên.

## 7. Điều kiện dừng riêng

1. Lúc bắt đầu task, `CameraController` không còn `takePicture()` một mình (task khác đã đổi chữ ký), `CameraViewModelTest` không còn dựng ViewModel ở một hàm phụ trợ, hoặc `cameraScreen` không còn tham số `onOpenSettings`: dừng nếu cần sửa file ngoài mục 3.2.
2. Bản `PrivacyOptionsButton(modifier, content)` không làm được mà không sửa `AdsController.kt` (ví dụ `privacyOptionsRequired` hay `showPrivacyOptionsForm` đã đổi): dừng, không sửa `AdsController`.
3. Gắn ngôn ngữ trong `attachBaseContext` không đổi được chữ của Compose dưới Android 13 trên emulator API 30 (kiểm tra thủ công ở bước 7 của phiên, hoặc `stringResource` vẫn ra ngôn ngữ máy), và cách sửa cần AppCompat hay thư viện khác: dừng (điểm chặn 2), ghi kết quả vào nhật ký.
4. `ACTION_APP_LOCALE_SETTINGS` hay `android:localeConfig` làm lint báo lỗi (không phải cảnh báo) với minSdk 26: dừng, không tắt quy tắc lint.
5. `SettingsNavigationTest` đỏ vì hộp thoại đồng ý UMP của bản staging phủ lên app nên cú bấm bánh răng không tới được camera: đây là giới hạn của môi trường test, không phải lỗi màn cài đặt. Sửa một lần bằng cách bấm qua `CameraTestTags.SETTINGS` sau khi chờ có giới hạn cây Compose của app hiện ra; vẫn đỏ thì bỏ test này khỏi PR, ghi vào nhật ký và mục Cần người xem, kiểm tra KB-1 bằng tay. Không tăng thời gian chờ quá 60 giây.
6. Đọc chặn trong `onCreate` hay `attachBaseContext` làm `AppLaunchTest` hoặc test emulator báo ANR, hay thời gian khởi động tăng rõ trên emulator (log `VGL.` cho thấy hơn 500 ms): dừng, không tự đổi cách đọc ngoài mục 8 câu 5 và câu 6.
7. Người duyệt chọn phương án khác của mục 8 câu 2 (hộp chọn trong cửa sổ của Material 3) mà screenshot không chụp được hộp: dừng, ghi lại để sửa plan.

## 8. Câu hỏi cho người duyệt

1. **Dùng chung lựa chọn qua `:core:data`.** Giao diện sáng tối do `:app` áp, âm chụp do camera đọc, nên ba lựa chọn không thể nằm trong `:feature:settings` (ADR-0001). Đề xuất của AI: `UserPreferences` trong `:core:data` (đúng gợi ý ở câu hỏi 6 của plan T-013), không tạo module core mới.
2. **Hộp chọn: tự vẽ hay của Material 3?** `sticker.md` ghi `RadioRow`, `ChoiceDialog` dùng `RadioButton` và `AlertDialog` của Material 3; spec mục 4 ghi "thành phần có sẵn"; README thiết kế chỉ liệt kê `SettingsRow`, `SectionHeader` là thành phần mới. Nhưng ảnh thiết kế có nút chọn nền vàng viền đen và lớp mờ 50%, `RadioButton` của Material 3 không vẽ được nền vàng, `AlertDialog` dùng lớp mờ của hệ thống và screenshot của Robolectric không chụp cửa sổ hộp thoại. Ba tài liệu không khớp nhau. Đề xuất của AI: `ChoiceDialog` và `RadioRow` tự vẽ trong `:core:designsystem`, theo đúng cách `NetworkDialog` đã làm (thẻ và lớp mờ do màn vẽ, cửa sổ `Dialog` riêng, tắt lớp mờ hệ thống); sau khi duyệt, cập nhật `sticker.md` và README thiết kế ở một PR tài liệu.
3. **Ngôn ngữ gốc của chữ.** T-008 đặt tiếng Anh làm gốc trong `values/` và dịch đủ 7 ngôn ngữ; T-013 (camera) để tiếng Việt trong `values/`, chưa dịch. Đề xuất của AI: màn cài đặt làm theo T-008 (đây là màn đổi ngôn ngữ, phải hiện đúng ngôn ngữ đã chọn), bảng dịch ở mục 3.1 do AI dịch, người biết tiếng duyệt trước khi phát hành. Camera và các màn khác dịch ở ticket riêng; cho đến khi đó, đổi ngôn ngữ chỉ thấy ở loading và cài đặt.
4. **Thông báo khi lưu lỗi.** CLAUDE.md yêu cầu báo người dùng khi ghi dữ liệu lỗi; thiết kế không có chữ cho trường hợp này. Đề xuất của AI: `Snackbar` của Material 3 ("Không lưu được lựa chọn. Hãy thử lại.", `sticker.md` đã có `Snackbar` đổi màu); lựa chọn không đổi. Nếu không muốn thêm chữ ngoài thiết kế thì chỉ ghi log như camera (câu hỏi 4 của T-013).
5. **Ngôn ngữ dưới Android 13 không có AppCompat.** Đề xuất của AI: lưu thẻ ngôn ngữ trong DataStore, đọc chặn tối đa 500 ms trong `attachBaseContext` (chỉ dưới Android 13, trước khi Hilt tiêm nên dùng `EntryPoint`), chọn xong thì `recreate()`. Phương án khác: thêm `androidx.appcompat` để dùng `AppCompatDelegate.setApplicationLocales` (sửa `gradle/libs.versions.toml`, đổi `MainActivity` sang `AppCompatActivity`, cần plan riêng). Lưu ý: chữ lấy qua context của `Application` (ví dụ thông báo) vẫn theo ngôn ngữ máy; hiện app chưa có chữ nào như vậy.
6. **Chớp màu lúc mở app.** Người dùng chọn "Tối" trên máy để sáng thì cửa sổ và lần vẽ đầu theo máy. Đề xuất của AI: đọc giá trị đầu trong `onCreate` bằng `runBlocking` tối đa 200 ms, nên lần vẽ đầu đã đúng màu; nền cửa sổ trước khi Compose vẽ (`Theme.App` theo `values-night`) vẫn theo máy trong vài khung hình. Muốn bỏ hẳn cần thư viện splash screen, ngoài phạm vi.
7. **Camera luôn tối nhưng dải thanh trạng thái theo app.** Câu hỏi 12 của T-013: phía trên camera có dải màu nền của app. Khi người dùng chọn "Sáng", dải này sáng và rõ hơn trước. Đề xuất của AI: giữ nguyên ở T-025, gộp vào ticket camera tràn viền (đề xuất của T-013) hoặc T-020.
8. **Lề trên 24 trong ảnh thiết kế.** Khung thiết kế có lề trên 24 cho thanh trạng thái; trên máy, `safeDrawingPadding` của `AppNavHost` lo phần này; Robolectric không có thanh trạng thái. Đề xuất của AI: màn không thêm lề; screenshot test đặt màn dưới một `Spacer` cao `Spacing.lg` để ảnh trùng khung thiết kế. Không làm vậy thì ảnh so sánh lệch 24 dp toàn bộ.
9. **Âm chụp ở nước bắt buộc.** Spec câu hỏi 1: cho tắt ở mọi nơi. Từ Android 13 có `MediaActionSound.mustPlayShutterSound()`. Đề xuất của AI: T-025 làm đúng spec (tắt là không phát); khi phát hành ở nước bắt buộc thì ẩn lựa chọn và luôn phát ở ticket riêng.
10. **Mô tả cho trình đọc màn hình.** Bảng chữ không có mô tả cho nút quay lại. Đề xuất của AI: "Quay lại" (`settings_back`); các icon đầu dòng không có mô tả vì chữ của dòng đã đủ nghĩa.
11. **Khi máy không mở được trang ngoài.** Máy không có trình duyệt hay không có trang ngôn ngữ của app (một số bản Android sửa đổi): đề xuất của AI là chỉ ghi log `warn`, không báo người dùng (không có chữ trong thiết kế). Nếu muốn, dùng lại `Snackbar` của câu 4 với chữ mới.
12. **Phiên bản hiện gì.** Đề xuất của AI: `versionName` nguyên văn, nên bản staging hiện "0.1.0-staging.42" (biết được bản nào đang thử), bản production hiện "0.1.0". Thiết kế minh họa "1.0.0".

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
