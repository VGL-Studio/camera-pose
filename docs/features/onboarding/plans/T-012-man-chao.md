---
ticket: T-012
title: Màn chào và xin quyền camera
status: done                       # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 20.0M        # cỡ M, số gợi ý của scripts/token-usage.py estimate (tạm, chưa đủ 5 task)
blocking_points: [analytics, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                    # người duyệt
cross_reviewer: Tuấn              # bắt buộc khi risk_level là high
approved_at: 2026-10-22
---

# PLAN T-012: MÀN CHÀO VÀ XIN QUYỀN CAMERA

## 1. Tóm tắt

Tạo module `:feature:onboarding` với một màn chào theo thiết kế Sticker (hình minh họa ba thẻ, tiêu đề, giới thiệu, ba điểm chính, nút "Bật camera", ghi chú), chữ đủ 7 ngôn ngữ. Bấm "Bật camera" thì app ghi `onboarding_complete`, đánh dấu đã qua màn chào, hỏi quyền camera, ghi `camera_permission_result` với `source` là `onboarding` khi hộp hỏi quyền thật sự hiện ra, rồi luôn sang camera (`CameraDestination(askPermission = false)`). `:app` lưu khóa `onboarding_done` trong DataStore chung và cắm màn chào vào `MainScreenChooser` của T-010 (cờ `feature_onboarding_enabled`), có bắt lỗi khi đọc DataStore. Kiểm chứng bằng unit test ViewModel, lớp lưu trạng thái và hàm chọn màn chính; test giao diện Robolectric (bố cục, màu, 7 ngôn ngữ ở màn hình thấp); screenshot test cùng tên hai ảnh thiết kế và ảnh khung thấp; test emulator đi từ màn chào tới camera; kiểm tra thủ công hộp hỏi quyền thật.

Phiên bản 2 (2026-10-22): theo quyết định của Nhật, màn chào dùng cùng quy tắc với camera (spec `camera` KB-7, plan T-026): không ghi `camera_permission_result` khi quyền đã bị từ chối vĩnh viễn và hộp hỏi không hiện. Spec `onboarding` đã cập nhật (KB-1, KB-2, mục 6). Thay đổi so với phiên bản 1: hàm thuần `permissionDialogShown` chép từ T-026 vào module, `OnboardingRoute` đọc "nên giải thích" trước và sau khi hỏi, `onPermissionResult` nhận thêm `dialogShown`, ba test đơn vị mới thay một test cũ, ticket phụ thuộc thêm T-026. Phần còn lại giữ nguyên phiên bản 1.

## 2. Hiểu yêu cầu

Ticket xử lý KB-1 đến KB-4 trong `features/onboarding/spec.md` (đã xác nhận ngày 2026-10-21, ba câu hỏi đã trả lời; cập nhật ngày 2026-10-22 về điều kiện ghi `camera_permission_result`, chờ xác nhận lại).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi màn loading xong ở lần mở đầu tiên thì màn chào hiện, đúng thiết kế, không có quảng cáo | `MainScreenChooser` trả `ONBOARDING` khi cờ màn chào bật, cờ camera bật (mục 8, câu 2) và chưa có khóa `onboarding_done`; `AppNavHost` đưa `ONBOARDING` tới `OnboardingDestination`. Module không dùng `:core:ads`: không `AdSlot`, không `rememberShowInterstitial` |
| KB-1 | Khi bấm "Bật camera" thì máy hiện hộp hỏi quyền camera của hệ thống | `OnboardingRoute` chạy `RequestPermission()` khi ViewModel phát `RequestPermission`. Quyền đã có sẵn thì không hỏi (mục 8, câu 4) |
| KB-1 | Khi người dùng cho phép thì app mở camera, khung xem trước chạy, nét viền của tư thế mặc định đã hiện sẵn | Màn chào điều hướng tới `CameraDestination(askPermission = false)`, bỏ màn chào khỏi back stack. Camera có quyền thì tự mở (T-013). Nét viền tư thế mặc định thuộc T-014, plan này chỉ kiểm chứng tới được camera |
| KB-1 | Đúng 2 chạm từ lúc loading xong tới camera: "Bật camera" và "Cho phép" | Không màn hình nào chen giữa: một lần bấm phát `Started` rồi `RequestPermission`; kết quả quyền phát `Finished`; `:app` điều hướng ngay. Hộp đồng ý quảng cáo (ADR-0013) không tính vào đây (mục 8, câu 6) |
| KB-1 | Bấm "Bật camera" thì ghi `onboarding_complete` một lần; hộp hỏi quyền hiện ra và người dùng chọn thì ghi `camera_permission_result` | `onboarding_complete` không tham số, ghi một lần cho mỗi lần qua màn chào (cờ trong `SavedStateHandle`, bấm lại không ghi thêm). `camera_permission_result`: `result` là `granted` hoặc `denied`, `source` là `onboarding`; chỉ ghi khi `permissionDialogShown(...)` đúng (mục 3.1) |
| KB-1, đặc biệt | Bấm quay lại ở màn chào thì app đóng | Màn chào là đích đầu tiên sau loading (`popUpTo<LoadingDestination> { inclusive = true }` của T-010) và không chặn nút quay lại, nên quay lại đóng activity |
| KB-1, đặc biệt | Xoay máy không cắt chữ hay nút; màn thấp thì cuộn được, nút "Bật camera" luôn thấy | App khóa dọc (T-013); màn rộng trên Android 16 bỏ qua khóa này. Bố cục: phần trên (minh họa, chữ, ba điểm) cuộn được, nút và ghi chú ghim ở đáy, hình minh họa co lại trước (README mục 6). ViewModel sống qua đổi cấu hình |
| KB-1, đặc biệt | Máy đặt ngôn ngữ chưa có thì màn chào hiện tiếng Anh | `values/` là tiếng Anh (như T-008); 6 thư mục `values-vi`, `values-hi`, `values-pt-rBR`, `values-b+es+419`, `values-in`, `values-th` |
| KB-2 | Từ chối quyền thì vẫn sang camera ở trạng thái chưa có quyền, không quay lại màn chào | `Finished` cho cả hai kết quả. `askPermission = false` làm camera hiện `PermissionDenied` ngay, không hỏi lần hai (T-013, KB-7). Màn chào bị bỏ khỏi back stack |
| KB-2 | Đã chọn "không hỏi lại" từ trước: bấm "Bật camera" thì sang thẳng camera ở trạng thái chưa có quyền và không ghi `camera_permission_result` | Hệ thống trả `false` ngay, không hiện hộp; luồng giống từ chối nhưng không ghi sự kiện, chỉ log "Camera permission denied without dialog" (phiên bản 2, mục 8, câu 5 và 12) |
| KB-3 | Đã bấm "Bật camera" ở lần trước thì lần sau vào thẳng camera, kể cả khi đã từ chối | Khóa `onboarding_done = true` ghi ngay khi bấm (trước hộp hỏi quyền, plan T-010 câu 2). `MainScreenChooser` đọc khóa: có thì `CAMERA` |
| KB-3 | Đóng app ở màn chào mà chưa bấm thì lần sau màn chào hiện lại | Chỉ lần bấm mới ghi khóa; mở màn chào không ghi gì |
| KB-3 | Xóa dữ liệu của app thì lần sau màn chào hiện lại | Khóa nằm trong DataStore `app_preferences`; xóa dữ liệu xóa file, không có khóa thì coi là chưa qua. `android:allowBackup="false"` nên cài lại cũng không khôi phục khóa |
| KB-4 | Cờ `feature_onboarding_enabled` tắt thì vào thẳng camera; camera tự hỏi quyền; không ghi `onboarding_complete` | `onboardingAvailable = false`, `chooseMainScreen` trả `CAMERA`; điều hướng tới `CameraDestination()` (mặc định `askPermission = true`) như hiện nay. Màn chào không được tạo nên không có sự kiện. Không đọc DataStore khi cờ tắt |
| Ticket | Cờ mới, đăng ký trong `AppNavHost` | `FeatureFlag.ONBOARDING("feature_onboarding_enabled")`, mặc định tắt, bật ở staging |
| Ghi chú rà soát T-010 | Bắt lỗi trong `MainScreenChooser.choose()` khi đọc DataStore | `IOException` khi đọc khóa thì log `warn` và coi là chưa qua màn chào (mục 8, câu 3); `choose()` không ném lỗi này ra `AppNavHost` |

Thiết kế tham chiếu: `features/onboarding/design/README.md` (đã duyệt 2026-10-21); số liệu trong `design/context/OnboardingScreen_default_light.md`, `OnboardingScreen_default_dark.md`; ảnh `design/screens/OnboardingScreen_default_light.png`, `OnboardingScreen_default_dark.png`. Cả hai ảnh thuộc ticket này.

ADR liên quan: ADR-0001 (feature chỉ phụ thuộc core; trạng thái "đã qua" lưu ở `:app`), ADR-0003 (cờ), ADR-0008 (màu, chữ, khoảng cách từ `:core:designsystem`), ADR-0012 (sự kiện qua `:core:analytics`), ADR-0013 (không quảng cáo, hộp đồng ý vẫn ở `MainActivity`), ADR-0019 (log), ADR-0020 (tên screenshot test, ảnh so sánh).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu `:feature:sample` và `:feature:camera` (T-013): `UiState` sealed, ViewModel với `StateFlow` và kênh sự kiện `Channel(BUFFERED).receiveAsFlow()`, `Route` nối ViewModel, `Screen` không trạng thái, đích `@Serializable`. Gói gốc: `com.vglstudio.camera.pose.line.guide.feature.onboarding`. Thẻ log: `Onboarding`.

**Điều kiện đầu vào.** T-010 và T-013 đã merge (có trên nhánh chính lúc viết plan): `MainScreen`, `MainScreenInputs(onboardingAvailable, onboardingDone, cameraAvailable)`, `MainScreenChooser(featureFlags)`, `AppNavHost(chooseMainScreen, modifier, onPastLoadingChanged)`, `MainScreen.destination()` với nhánh `ONBOARDING -> SampleDestination`, `CameraDestination(askPermission: Boolean = true)`. T-026 (phiên bản 2): `feature/camera/.../PermissionPrompt.kt` có `permissionDialogShown(granted, rationaleBefore, rationaleAfter)` theo quy tắc `granted || rationaleBefore || rationaleAfter`. T-007: `MaterialTheme.appColors.categoryWomen`, `categoryCouple`, `categoryFriends`, `onCategory`; `Radius.card`, `Radius.full`, `Stroke.sticker`; `PrimaryButton(text, onClick, modifier, enabled, icon, loading)`. Khác đi thì dừng (mục 7).

**Không có `data/` và `di/` trong feature.** Trạng thái "đã qua màn chào" do `:app` lưu (plan T-010, câu 2, đã trả lời OK): feature báo `onStarted` khi người dùng bấm, `:app` ghi khóa. Feature không phụ thuộc `:core:data`.

**Trạng thái và sự kiện (`OnboardingUiState.kt`).**

```kotlin
sealed interface OnboardingUiState {
    data object Ready : OnboardingUiState
    data object RequestingPermission : OnboardingUiState
    data object Finished : OnboardingUiState
}
sealed interface OnboardingEvent {
    data object Started : OnboardingEvent            // the user tapped "Bật camera": :app marks onboarding done
    data object RequestPermission : OnboardingEvent  // the route launches the system permission prompt
    data object Finished : OnboardingEvent           // go to the camera, granted or not
}
```

**`OnboardingViewModel`** (`@HiltViewModel`) nhận `Analytics`, `AppLogger`, `SavedStateHandle`. Không nhận `FeatureFlags`: cờ chỉ quyết định đích sau loading trong `:app` (như T-013). Không có repository.

- `init`: log `info` "Onboarding shown". Trạng thái đầu: `RequestingPermission` nếu `savedStateHandle["requesting_permission"] == true` (activity bị tạo lại khi hộp hỏi quyền đang mở), ngược lại `Ready`.
- `onEnableCameraClick(permissionGranted: Boolean)`: chỉ chạy khi `Ready`. Nếu `savedStateHandle["completion_logged"] != true` thì ghi `AnalyticsEvent("onboarding_complete")` và đặt cờ đó. Log `info` "Enable camera tapped". Phát `Started`. Có quyền sẵn: log "Camera permission already granted", trạng thái `Finished`, phát `Finished`. Chưa có: đặt `requesting_permission = true`, trạng thái `RequestingPermission`, log "Asking for the camera permission", phát `RequestPermission`.
- `onPermissionResult(granted: Boolean, dialogShown: Boolean = true)`: chỉ chạy khi `RequestingPermission`. Khi `dialogShown` đúng: ghi `AnalyticsEvent("camera_permission_result", mapOf("result" to "granted"|"denied", "source" to "onboarding"))`, log `info` "Camera permission granted|denied". Khi `dialogShown` sai (phiên bản 2): không ghi sự kiện, log `info` "Camera permission denied without dialog" (cùng chữ với camera). Cả hai nhánh: xóa `requesting_permission`, trạng thái `Finished`, phát `Finished`.
- Hằng số tên sự kiện và khóa `SavedStateHandle` trong `companion object`, như `CameraViewModel`.

**Màn hình (`OnboardingScreen.kt`, `OnboardingIllustration.kt`).**

`OnboardingRoute(onStarted: () -> Unit, onFinished: () -> Unit, viewModel = hiltViewModel())`: `val activity = LocalActivity.current` (như T-026); `var rationaleBefore by rememberSaveable { mutableStateOf(false) }` (mục 8, câu 12); `rememberLauncherForActivityResult(RequestPermission()) { granted -> val rationaleAfter = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, CAMERA) } ?: false; viewModel.onPermissionResult(granted, activity == null || permissionDialogShown(granted, rationaleBefore, rationaleAfter)) }` (activity null chỉ có trong preview, coi như hộp đã hiện, như T-026); `LaunchedEffect(viewModel)` thu `events` (callback qua `rememberUpdatedState`): `Started -> onStarted()`, `RequestPermission -> { rationaleBefore = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, CAMERA) } ?: false; launcher.launch(CAMERA) }`, `Finished -> onFinished()`. Nút gọi `viewModel.onEnableCameraClick(ContextCompat.checkSelfPermission(context, CAMERA) == PERMISSION_GRANTED)`. Không có `BackHandler`. Theme theo máy (sáng hoặc tối), không bọc `AppTheme` riêng.

`OnboardingScreen(uiState, onEnableCamera, modifier)`, `testTag(SCREEN)`, nền `colorScheme.background`. Bố cục theo `context/` (khung 411 x 891, gốc là mép trên khung):

| Phần | Thành phần, màu, kiểu chữ | Kích thước, khoảng cách |
| --- | --- | --- |
| Khung ngoài | `Column(fillMaxSize)`, lề trái phải `Spacing.lg`, lề trên `Spacing.xl + Spacing.lg` (56), lề dưới `Spacing.lg` | |
| Phần trên | `BoxWithConstraints(Modifier.weight(1f))` lấy chiều cao khung nhìn; bên trong `Column(verticalScroll)` chứa `ShrinkFirstLayout` (dưới đây) | Phần trên chiếm chỗ còn lại, nên nút luôn ở đáy |
| Hình minh họa (`ILLUSTRATION`) | `OnboardingIllustration`, trang trí, không có mô tả cho trình đọc màn hình | Cao 280 (tối đa), rộng hết; co lại trước tiên, thấp nhất `MinIllustrationHeight = 160` (mục 8, câu 7) |
| Tiêu đề (`TITLE`) | `Text`, `headlineMedium`, `colorScheme.onBackground` | Cách hình minh họa `Spacing.lg` (đỉnh 360) |
| Giới thiệu (`BODY`) | `Text`, `bodyLarge`, `colorScheme.onSurfaceVariant` | Cách tiêu đề `Spacing.lg` (đỉnh 456) |
| Ba điểm (`FEATURES`, từng hàng `FEATURE_POSES`, `FEATURE_ADJUST`, `FEATURE_SAVED`) | `Row(verticalAlignment = CenterVertically)`: vòng 40 bo `Radius.full`, nền `colorScheme.secondaryContainer`, viền `Stroke.sticker` màu `colorScheme.outline`, icon 22 màu `colorScheme.onSecondaryContainer`; `Text` `bodyMedium` màu `colorScheme.onSurface` | Cách giới thiệu `Spacing.lg` (đỉnh 552); hàng cách nhau `Spacing.md`; icon cách chữ `Spacing.md`. Icon `accessibility_new`, `tune`, `bookmark` |
| Nút (`ENABLE_CAMERA`) | `PrimaryButton(text, onClick, icon = photo_camera, loading = uiState !is Ready)`, rộng hết | Cách phần trên `Spacing.lg`. Thiết kế cao 60, `PrimaryButton` cao tối thiểu 56 (lệch một bước lưới 4 dp, README mục 6 cho phép) |
| Ghi chú (`NOTE`) | `Text`, `bodySmall`, `colorScheme.onSurfaceVariant`, căn giữa | Dưới nút `Spacing.lg`; đáy cách mép dưới `Spacing.lg` |

`loading = uiState !is Ready`: khi hộp hỏi quyền đang mở hay đang chuyển sang camera, nút giữ màu như cũ, icon máy ảnh thành vòng chờ nhỏ và bấm không có tác dụng (`PrimaryButton` sẵn có). Ảnh thiết kế chỉ có trạng thái `Ready`.

`ShrinkFirstLayout(viewportHeight: Dp, illustration, text)`: một `Layout` riêng trong `OnboardingScreen.kt`. Đo khối chữ (tiêu đề, giới thiệu, ba điểm) trước với chiều rộng cố định, chiều cao không giới hạn; chiều cao hình minh họa là `(viewportHeight - chiều cao khối chữ - Spacing.lg).coerceIn(MinIllustrationHeight, 280.dp)`; đặt hình rồi khối chữ. Tổng cao hơn khung nhìn thì `verticalScroll` cho cuộn. Ở khung 411 x 891 hình cao đúng 280; ở 360 x 640 hình co về 160 rồi phần còn lại cuộn.

`OnboardingIllustration(modifier)`: một khung thiết kế 363 x 280 (số liệu `context/`, gốc là góc trên trái vùng minh họa), co theo `min(rộng / 363, cao / 280)` bằng `graphicsLayer(scaleX, scaleY)`, căn giữa. Thứ tự vẽ (sau đè trước), mọi thẻ 150 x 200, bo `Radius.card`, viền `Stroke.sticker` màu `colorScheme.outline`:

| Thành phần | Tâm (x, y) trong khung 363 x 280 | Góc nghiêng (`rotationZ`) | Màu nền |
| --- | --- | --- | --- |
| Thẻ "women" | (116, 158.5) | -8 | `appColors.categoryWomen` |
| Thẻ "couple" | (181, 130) | 0 | `appColors.categoryCouple` |
| Thẻ "friends" | (246, 179.5) | 8 | `appColors.categoryFriends` |
| Huy hiệu | (264, 42), tròn 56, bo `Radius.full`, viền `Stroke.sticker` màu `colorScheme.outline`, icon `photo_camera` 28 màu `colorScheme.onSecondaryContainer` | 0 | `colorScheme.secondaryContainer` |

Tâm lấy từ khung bao trong `context/` (thẻ nghiêng 8 độ có khung bao 176 x 219). Chiều nghiêng theo ảnh thiết kế: thẻ hồng nghiêng ngược chiều kim đồng hồ, thẻ xanh dương nghiêng theo chiều kim đồng hồ (Compose `rotationZ` dương là theo chiều kim đồng hồ). Trong mỗi thẻ: nét viền tư thế `onboarding_pose_outline` 70 x 140, cách mép trên thẻ 30, căn giữa ngang, màu `appColors.onCategory` (thiết kế #111111 ở cả hai chế độ). Một hình nét viền dùng cho cả ba thẻ: khung bao 89 x 148 của hai thẻ nghiêng chính là hình 70 x 140 xoay 8 độ.

**Suy ra hộp hỏi có hiện không (phiên bản 2).** `PermissionPrompt.kt` trong gói gốc của module: `internal fun permissionDialogShown(granted: Boolean, rationaleBefore: Boolean, rationaleAfter: Boolean): Boolean = granted || rationaleBefore || rationaleAfter`, chép nguyên từ `:feature:camera` của T-026 kèm KDoc và bảng năm tình huống (plan T-026, mục 3.1). Không phụ thuộc `:feature:camera` (ADR-0001, plan T-026 câu 2). Ở màn chào, lần hỏi đầu chạm ra ngoài hộp hỏi (Android 11 trở lên) bị coi là không hiện hộp nên không ghi (plan T-026, câu 1; spec `onboarding` mục 6).

**Biểu tượng và nét viền.** `feature/onboarding/src/main/res/drawable/`:

- `ic_onboarding_photo_camera.xml`, `ic_onboarding_accessibility_new.xml`, `ic_onboarding_tune.xml`, `ic_onboarding_bookmark.xml`: Material Symbols Outlined (Apache 2.0, repo `google/material-design-icons`, `symbols/web/<tên>/materialsymbolsoutlined/<tên>_24px.svg`), đổi sang `<vector>` 24 dp, màu theo `tint`. Chú thích đầu file ghi tên, nguồn, commit SHA đã tải, như 12 biểu tượng của T-013. Giấy phép chép vào `feature/onboarding/third_party/material-symbols/LICENSE.txt`. Tải không được là lỗi hạ tầng (auto-run.md, bước 5).
- `onboarding_pose_outline.xml`: vector 70 x 140 (khung nhìn 70 x 140), hai nét (đầu tròn 19 tại (26, 5); thân 50 x 108 tại (7, 26)), nét `2` dp, không tô. Nguồn: file SVG mà dev xuất từ Figma (frame "pose-outline" của thẻ "couple", node trong frame `18:129`) vào `docs/features/onboarding/design/handoff/pose_outline.svg` trước phiên. Không có file đó thì AI vẽ tay một nét viền người đứng gần giống ảnh thiết kế, đúng khung và vị trí hai nét ở trên, ghi vào nhật ký mục Lệch so với plan và Cần người xem (mục 8, câu 1). Không cần Figma trong phiên.

**Chữ và ngôn ngữ.** Khóa trong `feature/onboarding/src/main/res/values*/strings.xml`, chữ chép đúng từng ký tự. Không chữ nào có dấu nháy đơn, `&`, `<`, `>`. Bản dịch do AI viết, người biết tiếng duyệt trước khi phát hành (spec, câu hỏi 1).

| Khóa | `values/` (Anh) | `values-vi/` | `values-hi/` | `values-pt-rBR/` | `values-b+es+419/` | `values-in/` | `values-th/` |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `onboarding_title` | Strike a great pose, no guesswork | Chụp dáng đẹp, không cần đoán | बढ़िया पोज़, बिना अंदाज़े के | Poses perfeitas, sem adivinhar | Poses perfectas, sin adivinar | Pose keren, tanpa menebak | โพสสวยได้ ไม่ต้องเดา |
| `onboarding_body` | Pick a pose, step into the outline, then tap to shoot. Photos save to your gallery without the outline. | Chọn một tư thế, đứng vào nét viền rồi bấm chụp. Ảnh lưu vào thư viện máy, không kèm nét viền. | एक पोज़ चुनें, आउटलाइन में खड़े हों और फ़ोटो लें। फ़ोटो आउटलाइन के बिना आपकी गैलरी में सेव होती है। | Escolha uma pose, encaixe-se no contorno e toque para fotografar. As fotos vão para a galeria sem o contorno. | Elige una pose, colócate dentro del contorno y toca para tomar la foto. Las fotos se guardan en tu galería sin el contorno. | Pilih pose, berdiri di dalam garis pose, lalu ketuk untuk memotret. Foto tersimpan di galeri tanpa garis pose. | เลือกท่าโพส ยืนให้ตรงเส้นโครง แล้วแตะเพื่อถ่าย รูปจะบันทึกลงคลังภาพโดยไม่มีเส้นโครง |
| `onboarding_point_poses` | 200+ poses: solo, couples, friends | Hơn 200 tư thế: một người, cặp đôi, nhóm bạn | 200 से ज़्यादा पोज़: अकेले, कपल, दोस्तों के साथ | Mais de 200 poses: sozinho, casal, amigos | Más de 200 poses: solo, en pareja, con amigos | Lebih dari 200 pose: sendiri, pasangan, bersama teman | ท่าโพสกว่า 200 ท่า: คนเดียว คู่รัก กลุ่มเพื่อน |
| `onboarding_point_adjust` | Adjust outline opacity, size and position | Chỉnh độ mờ, cỡ và vị trí nét viền | आउटलाइन की पारदर्शिता, आकार और जगह बदलें | Ajuste a transparência, o tamanho e a posição do contorno | Ajusta la transparencia, el tamaño y la posición del contorno | Atur transparansi, ukuran, dan posisi garis pose | ปรับความโปร่งใส ขนาด และตำแหน่งของเส้นโครง |
| `onboarding_point_saved` | Save favorite poses to shoot again later | Lưu tư thế yêu thích để chụp lại sau | पसंदीदा पोज़ सेव करें और बाद में फिर से फ़ोटो लें | Salve suas poses favoritas para fotografar depois | Guarda tus poses favoritas para volver a usarlas | Simpan pose favorit untuk dipakai lagi nanti | บันทึกท่าโพสที่ชอบไว้ถ่ายอีกครั้งภายหลัง |
| `onboarding_enable_camera` | Turn on camera | Bật camera | कैमरा चालू करें | Ativar câmera | Activar cámara | Aktifkan kamera | เปิดกล้อง |
| `onboarding_note` | The app needs camera access to show the preview. Photos are stored only on your device. | App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn. | प्रीव्यू दिखाने के लिए ऐप को कैमरे की अनुमति चाहिए। फ़ोटो सिर्फ़ आपके डिवाइस पर सेव होती हैं। | O app precisa acessar a câmera para mostrar a visualização. As fotos ficam só no seu aparelho. | La app necesita acceso a la cámara para mostrar la vista previa. Las fotos solo se guardan en tu dispositivo. | Aplikasi perlu akses kamera untuk menampilkan pratinjau. Foto hanya tersimpan di perangkat Anda. | แอปต้องใช้สิทธิ์กล้องเพื่อแสดงภาพตัวอย่าง รูปภาพจะเก็บไว้ในเครื่องของคุณเท่านั้น |

Thuật ngữ "nét viền" theo bản dịch của T-008 (`आउटलाइन`, `contorno`, `garis pose`, `เส้นโครง`). Thiếu một khóa ở một ngôn ngữ thì Lint báo `MissingTranslation` và `scripts/check.sh` đỏ (đã đo ở T-008).

**Điều hướng và cờ.**

- `FeatureFlag.ONBOARDING("feature_onboarding_enabled")` trong `:core:config`, mặc định tắt; thêm `fun onboarding(): FeatureFlag = FeatureFlag.ONBOARDING` vào `StagingFeatureFlagsModule`.
- `navigation/OnboardingNavigation.kt`: `@Serializable data object OnboardingDestination`; `fun NavGraphBuilder.onboardingScreen(onStarted: () -> Unit, onFinished: () -> Unit)`.
- `:app`, gói `onboarding/` (mới), file `OnboardingStatus.kt`:
  - `internal interface OnboardingStatus { suspend fun isDone(): Boolean; suspend fun markDone() }`.
  - `internal class DataStoreOnboardingStatus @Inject constructor(dataStore: DataStore<Preferences>, logger: AppLogger)`: khóa `booleanPreferencesKey("onboarding_done")`. `isDone()` đọc `dataStore.data.first()[KEY] ?: false`, ném `IOException` cho bên gọi. `markDone()` chạy trong `withContext(NonCancellable)` (lần ghi không bị hủy khi activity bị tạo lại đúng lúc), `IOException` thì log `warn` thẻ `Onboarding` "Could not save onboarding done" và không ném ra; thành công thì log `info` "Onboarding marked done".
  - `di/OnboardingStatusModule.kt`: `@Binds` `OnboardingStatus` vào `DataStoreOnboardingStatus`, `SingletonComponent`.
- `navigation/MainScreenChooser.kt`: constructor `(featureFlags: FeatureFlags, onboardingStatus: OnboardingStatus, logger: AppLogger)`. `inputs()`:
  - `camera = featureFlags.enabled(FeatureFlag.CAMERA).first()`; `onboarding = featureFlags.enabled(FeatureFlag.ONBOARDING).first() && camera` (mục 8, câu 2).
  - `onboardingDone`: chỉ đọc khi `onboarding` đúng; `try { onboardingStatus.isDone() } catch (e: IOException) { logger.warn("Navigation", "Could not read onboarding status", e); false }`. Cờ tắt thì không đọc DataStore.
  - `choose()` log `info` thẻ `Navigation` "Main screen: <tên enum>". Giữ cách đọc giá trị đầu bằng `first()` (plan T-010, mục 3.1) và KDoc, bỏ câu "T-012 reads ...".
- `navigation/AppNavHost.kt`:
  - Tham số mới `markOnboardingDone: suspend () -> Unit` (ngay sau `chooseMainScreen`), qua `rememberUpdatedState` như `chooseMainScreen`.
  - `onboardingScreen(onStarted = { scope.launch { currentMarkOnboardingDone() } }, onFinished = { ... })`. `onFinished`: chỉ khi đích hiện tại là `OnboardingDestination` (`hasRoute<OnboardingDestination>()`, chặn điều hướng hai lần như loading) thì `navigate(CameraDestination(askPermission = false)) { popUpTo<OnboardingDestination> { inclusive = true }; launchSingleTop = true }`.
  - `MainScreen.destination()`: `ONBOARDING -> OnboardingDestination`, bỏ chú thích "T-012 replaces ...". Giữ nguyên nhánh `CAMERA`, `SAMPLE`, `cameraScreen(...)`, `sampleScreen()`, `onPastLoadingChanged` và mọi phần task khác đã thêm.
- `MainActivity.kt`: `@Inject internal lateinit var onboardingStatus: OnboardingStatus`; `AppNavHost(chooseMainScreen = mainScreenChooser::choose, markOnboardingDone = onboardingStatus::markDone, onPastLoadingChanged = ...)`. Không đổi gì khác (hộp đồng ý quảng cáo, `NetworkDialogHost`).
- `settings.gradle.kts`: `include(":feature:onboarding")`. `app/build.gradle.kts`: `implementation(project(":feature:onboarding"))`.

**Thứ tự commit (L-025).**

- Bước 3 (commit test): mọi file test ở mục 4 (kể cả `MainScreenTest.kt` sửa và `FakeOnboardingStatus` trong đó, `AppLaunchTest.kt`). Cùng commit là khung để biên dịch được: `settings.gradle.kts`, `feature/onboarding/build.gradle.kts`, `consumer-rules.pro`, `robolectric.properties`; `OnboardingUiState.kt` đủ kiểu; `PermissionPrompt.kt` với `permissionDialogShown` luôn trả `true` (phiên bản 2); `OnboardingViewModel` đủ hàm (kể cả tham số `dialogShown`) nhưng không làm gì, `uiState` luôn `Ready`, `events` rỗng; `OnboardingScreen` chỉ vẽ `Box` mang `testTag(SCREEN)`, `OnboardingTestTags` đủ hằng; `OnboardingNavigation.kt`; `values/strings.xml` đủ 7 khóa tiếng Anh (6 thư mục ngôn ngữ để bước 4); `FeatureFlag.ONBOARDING`; `OnboardingStatus.kt` với `isDone()` trả `false` và `markDone()` rỗng; `OnboardingStatusModule.kt`; `MainScreenChooser` nhận ba tham số mới nhưng `inputs()` vẫn đặt `onboardingAvailable = false`; `app/build.gradle.kts`. Chạy, ghi lý do thất bại thật vào nhật ký.
- Bước 4 (commit code): quy tắc thật của `permissionDialogShown`, ViewModel, màn hình, hình minh họa, 5 drawable, giấy phép, 6 thư mục ngôn ngữ, logic thật của `DataStoreOnboardingStatus` và `MainScreenChooser`, `AppNavHost.kt`, `MainActivity.kt`, `StagingFeatureFlagsModule.kt`.
- Commit ảnh: `./gradlew :feature:onboarding:recordRoborazziDebug`, xem từng ảnh; `scripts/check.sh`; `scripts/design-compare.sh onboarding`; commit `design/review/` và README mục 8.

**Mẫu có sẵn được làm theo:** `CameraViewModel` (sự kiện quyền, `SavedStateHandle`, `Analytics`), `CameraRoute` (launcher hỏi quyền), `DataStoreCounterRepository` và test của nó (DataStore giả trong bộ nhớ), `LoadingStringsTest` (7 ngôn ngữ), `LoadingScreenTest` (màn hình thấp, 7 ngôn ngữ), `CameraComponentsTest.pixelAt` (đo màu điểm ảnh bằng cách vẽ view gốc ra `Bitmap`, L-006), `MainScreenTest`.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `settings.gradle.kts` | `include(":feature:onboarding")` |
| Tạo | `feature/onboarding/build.gradle.kts` | Plugin `template.android.feature`, namespace; không thêm phụ thuộc |
| Tạo | `feature/onboarding/consumer-rules.pro` | Rỗng, như các feature khác |
| Tạo | `feature/onboarding/third_party/material-symbols/LICENSE.txt` | Giấy phép Apache 2.0 của Material Symbols |
| Tạo | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingUiState.kt` | `OnboardingUiState`, `OnboardingEvent` |
| Tạo | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/PermissionPrompt.kt` | Hàm thuần `permissionDialogShown`, chép từ T-026 (phiên bản 2) |
| Tạo | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingViewModel.kt` | ViewModel theo mục 3.1 |
| Tạo | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreen.kt` | `OnboardingRoute`, `OnboardingScreen`, `ShrinkFirstLayout`, `OnboardingTestTags` |
| Tạo | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingIllustration.kt` | Ba thẻ nghiêng, nét viền, huy hiệu |
| Tạo | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/navigation/OnboardingNavigation.kt` | `OnboardingDestination`, `onboardingScreen()` |
| Tạo | `feature/onboarding/src/main/res/values/strings.xml` | 7 khóa tiếng Anh (gốc) |
| Tạo | `feature/onboarding/src/main/res/values-vi/strings.xml` | Tiếng Việt (bảng chữ của thiết kế) |
| Tạo | `feature/onboarding/src/main/res/values-hi/strings.xml` | Tiếng Hindi |
| Tạo | `feature/onboarding/src/main/res/values-pt-rBR/strings.xml` | Tiếng Bồ Đào Nha Brazil |
| Tạo | `feature/onboarding/src/main/res/values-b+es+419/strings.xml` | Tiếng Tây Ban Nha Mỹ Latinh |
| Tạo | `feature/onboarding/src/main/res/values-in/strings.xml` | Tiếng Indonesia |
| Tạo | `feature/onboarding/src/main/res/values-th/strings.xml` | Tiếng Thái |
| Tạo | `feature/onboarding/src/main/res/drawable/ic_onboarding_photo_camera.xml` | Material Symbols `photo_camera` |
| Tạo | `feature/onboarding/src/main/res/drawable/ic_onboarding_accessibility_new.xml` | Material Symbols `accessibility_new` |
| Tạo | `feature/onboarding/src/main/res/drawable/ic_onboarding_tune.xml` | Material Symbols `tune` |
| Tạo | `feature/onboarding/src/main/res/drawable/ic_onboarding_bookmark.xml` | Material Symbols `bookmark` |
| Tạo | `feature/onboarding/src/main/res/drawable/onboarding_pose_outline.xml` | Nét viền tư thế 70 x 140 (mục 3.1) |
| Tạo | `feature/onboarding/src/test/resources/robolectric.properties` | `sdk=35`, như các feature khác |
| Tạo | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/MainDispatcherRule.kt` | Chép từ `:feature:sample` |
| Tạo | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingViewModelTest.kt` | Unit test ViewModel, `FakeAnalytics` riêng trong file |
| Tạo | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/PermissionPromptTest.kt` | Năm tình huống của bảng, chép từ T-026 (phiên bản 2) |
| Tạo | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreenTest.kt` | Test giao diện Robolectric |
| Tạo | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingStringsTest.kt` | Chữ 7 ngôn ngữ |
| Tạo | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreenScreenshotTest.kt` | Ba screenshot test |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_default_light.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_default_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_default_small.png` | Khung 360 x 640 (L-028) |
| Tạo | `docs/features/onboarding/design/review/` | Sinh bởi `scripts/design-compare.sh onboarding` (hai ảnh ghép, `summary.md`), không sửa tay |
| Sửa | `docs/features/onboarding/design/README.md` | Mục 8: thay "Chưa có." bằng `review/summary.md` và nhận xét của rà soát độc lập |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FeatureFlag.kt` | `ONBOARDING("feature_onboarding_enabled")` |
| Sửa | `app/build.gradle.kts` | `implementation(project(":feature:onboarding"))` |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/onboarding/OnboardingStatus.kt` | `OnboardingStatus`, `DataStoreOnboardingStatus` |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/di/OnboardingStatusModule.kt` | Gắn `OnboardingStatus` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/MainScreenChooser.kt` | Đọc cờ màn chào, cờ camera, trạng thái đã qua; bắt `IOException`; log |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/AppNavHost.kt` | Tham số `markOnboardingDone`, đăng ký `onboardingScreen`, `ONBOARDING -> OnboardingDestination` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | Tiêm `OnboardingStatus`, truyền `markOnboardingDone` |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingFeatureFlagsModule.kt` | Bật `FeatureFlag.ONBOARDING` ở staging |
| Sửa | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/navigation/MainScreenTest.kt` | Ba test chooser cũ đổi cách dựng; sáu test mới (mục 4); `FakeOnboardingStatus` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/onboarding/DataStoreOnboardingStatusTest.kt` | Unit test lưu trạng thái, DataStore giả trong file |
| Sửa | `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt` | Từ màn chào bấm "Bật camera" rồi tới camera (mục 4) |

Không file nào khác. Đặc biệt không sửa `:feature:camera` (đã có `CameraDestination(askPermission)`; `PermissionPrompt.kt` của camera chỉ được đọc để chép), `:feature:loading`, `MainScreen.kt`, `AndroidManifest.xml` (quyền `CAMERA` đã có), `gradle/libs.versions.toml`, `build-logic/`, `:core:designsystem`, `:core:data`, `docs/design-system/tokens.json`.

**Nơi khác đọc các file trên** (L-020, đã tìm bằng `grep -rn` trong `scripts/`, `.github/`, `build-logic/`, `*/src/test`, `*/src/androidTest`):

- `scripts/`, `.github/`, `build-logic/` không nhắc tới `AppNavHost`, `MainScreenChooser`, `StagingFeatureFlagsModule`, `FeatureFlag.kt`, `AppLaunchTest` hay tên module `onboarding` (chỉ `scripts/plan-check.py` có các từ chung). `pr-check.yml` chạy `:app:connectedStagingDebugAndroidTest`, không đọc tên file test.
- `FeatureFlag.kt`: `DefaultFeatureFlagsTest.kt` duyệt `FeatureFlag.entries`; cờ mới mặc định tắt nên `everyFlag_isOffByDefault` vẫn qua, không cần sửa.
- `MainScreenChooser.kt`: chỉ `MainActivity.kt` (Hilt) và `MainScreenTest.kt` dùng, đều trong bảng. `AppNavHost.kt`: chỉ `MainActivity.kt` gọi.
- `settings.gradle.kts`, `app/build.gradle.kts`: `checkModuleDependencies` đọc phụ thuộc lúc build; `:feature:onboarding` chỉ phụ thuộc core qua plugin. `scripts/release.sh` chỉ đọc `applicationId`, `versionCode`, `versionName` của `app/build.gradle.kts`; plan chỉ thêm một dòng `implementation`, không chạm script. `scripts/new-app.sh` không có trong repo của app (ADR-0017 chỉ áp dụng khi đổi tên file).
- `AppLaunchTest.kt`: chỉ CI chạy; L-009 (`device-session`) không đọc file này.
- `docs/features/onboarding/design/README.md`: `scripts/status.py` đọc `status` của README thiết kế; plan không đổi `status`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: `feature_onboarding_enabled` (spec mục 8), mặc định tắt, bật sẵn ở staging. Chỉ `:app` đọc, một lần khi loading xong (`first()`, như cờ camera của T-013). Màn chào chỉ có khi cờ camera cũng bật (mục 8, câu 2).
- Quảng cáo: Không chạm. Màn chào không có vị trí quảng cáo (spec mục 5). Hộp đồng ý quảng cáo của `MainActivity` không đổi.
- Sự kiện phân tích (điểm chặn 4, đã có trong spec mục 6):
  - `onboarding_complete`: mới, không tham số, khi bấm "Bật camera".
  - `camera_permission_result`: sự kiện đã có (T-013), thêm giá trị `source = onboarding`; `result` là `granted` hoặc `denied`; chỉ ghi khi hộp hỏi quyền hiện ra (spec `onboarding` mục 6, phiên bản 2).
- Dữ liệu cục bộ: khóa DataStore mới `onboarding_done` (boolean) trong `app_preferences` chung (spec mục 6). Khóa mới, không chuyển đổi dữ liệu cũ, nên không chạm điểm chặn 9. Không gửi gì ra khỏi máy ngoài hai sự kiện trên.
- File R2: Không chạm. Hình minh họa vẽ bằng Compose và vector trong module (spec mục 7).

## 4. Kiểm thử

Mọi test dưới đây vào commit test của bước 3 cùng khung ở mục 3.1. Lý do thất bại ở cột cuối là điều AI dự kiến; bước 3 chạy và ghi lý do thật vào nhật ký.

Bản giả: `FakeAnalytics` (ghi `events`), `RecordingAppLogger` của `:core:logging`. ViewModel dựng với `SavedStateHandle()` hoặc `SavedStateHandle(mapOf(...))`. Sự kiện thu bằng Turbine. `FakeOnboardingStatus(done: Boolean, readError: IOException? = null)` ghi `reads`, `marks`. `FakeFeatureFlags` của `MainScreenTest.kt` giữ nguyên. DataStore giả trong bộ nhớ (`InMemoryDataStore`, `UnreadableDataStore`, `UnwritableDataStore`) chép theo `DataStoreCounterRepositoryTest`. Kiểm tra lỗi so loại (`assertTrue(entry.error is IOException)`). Đo màu điểm ảnh bằng `pixelAt` (vẽ view gốc ra `Bitmap`, như `CameraComponentsTest`), không dùng `captureToImage()` (L-006). Test bố cục dùng `@GraphicsMode(GraphicsMode.Mode.NATIVE)`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, mở màn chào không ghi gì | Unit test | `OnboardingViewModelTest.shown_logsShown_withoutEventOrAnalytics`: `uiState` là `Ready`; không có sự kiện; `analytics.events` rỗng; log `INFO` thẻ `Onboarding` "Onboarding shown". Thất bại: khung không log |
| KB-1, bấm thì hỏi quyền | Unit test | `enableCamera_withoutPermission_logsComplete_thenStarted_thenRequestPermission`: `onEnableCameraClick(false)`; sự kiện lần lượt `Started`, `RequestPermission`; `analytics.events == [AnalyticsEvent("onboarding_complete")]`; `uiState` là `RequestingPermission`. Thất bại: không có sự kiện |
| KB-1, `onboarding_complete` một lần | Unit test | `enableCamera_tappedTwice_logsOnce_andRequestsOnce`: bấm hai lần; một `onboarding_complete`, một `Started`, một `RequestPermission` |
| KB-1, cho phép | Unit test | `permissionGranted_logsResultWithOnboardingSource_andFinishes`: sau `onPermissionResult(true)`: `analytics.events` thứ hai là `AnalyticsEvent("camera_permission_result", mapOf("result" to "granted", "source" to "onboarding"))`; sự kiện `Finished`; `uiState` là `Finished`; log "Camera permission granted" |
| KB-1, 2 chạm | Unit test | `twoTaps_reachFinished_withNothingInBetween`: chỉ `onEnableCameraClick(false)` và `onPermissionResult(true)`; toàn bộ sự kiện đúng `[Started, RequestPermission, Finished]` |
| KB-1, quyền đã có sẵn | Unit test | `enableCamera_permissionAlreadyGranted_finishesWithoutPromptOrResultEvent`: `onEnableCameraClick(true)`; sự kiện `[Started, Finished]`; `analytics.events == [AnalyticsEvent("onboarding_complete")]` |
| KB-2, từ chối khi hộp hỏi hiện | Unit test | `permissionDeniedWithDialog_logsDenied_andStillFinishes`: `onPermissionResult(granted = false, dialogShown = true)`; sự kiện `camera_permission_result` với `"result" to "denied"`, `"source" to "onboarding"`; sự kiện `Finished` (không quay về `Ready`) |
| KB-2, "không hỏi lại" từ trước (phiên bản 2) | Unit test | `permissionDeniedWithoutDialog_logsNoResult_andStillFinishes`: `onPermissionResult(granted = false, dialogShown = false)`; `analytics.events == [AnalyticsEvent("onboarding_complete")]` (không có `camera_permission_result`); sự kiện `Finished`; log `INFO` thẻ `Onboarding` "Camera permission denied without dialog". Thất bại ở bước 3: khung không có sự kiện `Finished` |
| Quy tắc suy luận (phiên bản 2) | Unit test | `PermissionPromptTest`, chép năm test của T-026: `granted_isShown` (`true, false, false` trả `true`); `firstDenial_isShown` (`false, false, true` trả `true`); `secondDenial_isShown` (`false, true, false` trả `true`); `permanentDenial_isNotShown` (`false, false, false` trả `false`); `deniedWithBothRationales_isShown` (`false, true, true` trả `true`). Thất bại ở bước 3: `permanentDenial_isNotShown` (khung trả `true`); bốn test kia qua ngay, giữ làm test chặn |
| Kết quả quyền lạc | Unit test | `permissionResult_whenNotRequesting_isIgnored`: ở `Ready`, `onPermissionResult(true)`; không sự kiện, không phân tích |
| Activity tạo lại khi hộp quyền đang mở | Unit test | `restoredWhileRequesting_acceptsResult_withoutSecondComplete`: `SavedStateHandle(mapOf("requesting_permission" to true, "completion_logged" to true))`; `uiState` là `RequestingPermission`; `onPermissionResult(false)` cho `Finished`; không có `onboarding_complete` |
| Log không chứa dữ liệu cá nhân | Unit test | `logs_useOnboardingTag`: sau luồng cho phép, mọi dòng thẻ `Onboarding`, thứ tự "Onboarding shown", "Enable camera tapped", "Asking for the camera permission", "Camera permission granted" |
| KB-1, đúng thiết kế: bố cục | Compose UI test, `@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")` | `OnboardingScreenTest.default_layoutMatchesDesign`: `ILLUSTRATION` đỉnh 56, cao 280, trái 24, rộng 363; `TITLE` đỉnh 360; `BODY` đỉnh 456; `FEATURES` đỉnh 552, cao 152; `ENABLE_CAMERA` rộng 363, cao ít nhất 56, đỉnh 753; `NOTE` đáy 869. Sai số mỗi số 4 dp (README mục 6, 7). Thất bại: không có node |
| KB-1, đúng thiết kế: màu | Compose UI test, `@GraphicsMode(NATIVE)`, cùng khung, `AppTheme(darkTheme = false)` | `default_illustrationColors_light`: trong `ILLUSTRATION` (toạ độ dp từ góc trên trái), điểm (125, 60) là #3DDC97 (`categoryCouple`), (60, 120) là #FF5C8A, (310, 150) là #4D8DFF, (264, 20) là #FFC83D (huy hiệu `secondaryContainer`); vòng icon đầu tiên, điểm (4, 20) của `FEATURE_POSES` là #FFC83D; nền màn hình (2, 2) là #FFFDF8. `default_colors_dark`: nền #121212, nút `ENABLE_CAMERA` điểm (8, 30) là #FFFFFF (`secondary` tối) |
| Chữ đúng bảng chữ của thiết kế | Compose UI test, `vi-w411dp-h891dp-xxhdpi` | `default_showsDesignTexts`: có đúng 7 chữ của README mục 2 (tiêu đề, giới thiệu, ba điểm, "Bật camera", ghi chú) |
| KB-1, bấm nút | Compose UI test | `enableCamera_click_callsCallbackOnce`: `Ready`, bấm `ENABLE_CAMERA`; `onEnableCamera` được gọi một lần |
| Đang hỏi quyền không bấm lại được | Compose UI test | `requesting_showsSpinner_andIgnoresClicks`: `RequestingPermission`; có `PrimaryButtonTestTags.SPINNER`; bấm `ENABLE_CAMERA` không gọi callback |
| KB-1, quay lại đóng app | Compose UI test | `screen_doesNotInterceptBack`: `createAndroidComposeRule<ComponentActivity>()`, đặt `OnboardingScreen(Ready, {})`; `activity.onBackPressedDispatcher.hasEnabledCallbacks()` là `false`. Qua ngay ở bước 3 (khung không có `BackHandler`); giữ làm test chặn, ghi vào nhật ký |
| KB-1, màn hình thấp 7 ngôn ngữ (L-028) | Compose UI test, `@GraphicsMode(NATIVE)`, 7 test với `@Config(qualifiers = "<ngôn ngữ>-w360dp-h640dp-xxhdpi")` | `small_buttonAndNoteVisible_contentScrolls_en` (không tiền tố), `_vi`, `_hi`, `_ptBR` (`pt-rBR`), `_es419` (`b+es+419`), `_in`, `_th`. Chung một hàm phụ trợ: không cuộn, đáy `ENABLE_CAMERA` và đáy `NOTE` không lớn hơn 640, đỉnh `ENABLE_CAMERA` không nhỏ hơn đáy vùng cuộn; `ENABLE_CAMERA` cao ít nhất 48; `ILLUSTRATION` cao từ 160 tới dưới 280; `performScrollTo()` trên `FEATURE_SAVED` rồi `assertIsDisplayed()`; bấm `ENABLE_CAMERA` gọi callback |
| Hình minh họa không co khi đủ chỗ | Compose UI test | `default_illustrationFullHeight_whenRoomEnough`: khung 411 x 891, `ILLUSTRATION` cao 280 (gộp trong `default_layoutMatchesDesign` nếu trùng) |
| KB-1, ngôn ngữ chưa có thì tiếng Anh | Unit test (Robolectric) | `OnboardingStringsTest.english_isDefault`: ngữ cảnh `en` và `fr` ra đúng cột `values/`. Qua ngay ở bước 3 (chữ tiếng Anh có trong khung); test chặn |
| Chữ tiếng Việt khớp thiết kế | Unit test (Robolectric) | `vietnamese_matchesDesignTextTable`: ngữ cảnh `vi`, 7 khóa đúng từng ký tự cột `values-vi/`. Thất bại ở bước 3: ra chữ tiếng Anh |
| Đủ 7 ngôn ngữ | Unit test (Robolectric) | `everyReleaseLanguage_isTranslated`: `hi`, `pt-BR`, `es-419`, `es-MX`, `id`, `th`, `vi`: mỗi khóa khác chữ tiếng Anh; `id` đọc `values-in`. Thất bại ở bước 3 |
| KB-3, lưu đã qua | Unit test | `DataStoreOnboardingStatusTest.nothingStored_isNotDone` (qua ngay ở bước 3, test chặn); `markDone_thenIsDone` (thất bại: khung không ghi); `markDone_storesOnboardingDoneKey`: preferences có `booleanPreferencesKey("onboarding_done") == true` |
| Ghi lỗi không làm app dừng | Unit test | `markDone_writeFails_logsWarning_doesNotThrow`: `UnwritableDataStore`; không ném; log `WARN` thẻ `Onboarding`, `error is IOException` |
| Đọc lỗi chuyển cho bên gọi | Unit test | `isDone_readFails_throwsIOException`: `UnreadableDataStore`; `assertTrue(exception is IOException)` |
| KB-1, lần đầu ra màn chào | Unit test | `MainScreenTest.chooser_onboardingAndCameraOn_notDone_choosesOnboarding`: cờ `ONBOARDING`, `CAMERA`, `FakeOnboardingStatus(done = false)`: `ONBOARDING`. Thất bại ở bước 3: `CAMERA` |
| KB-3, đã qua thì vào camera | Unit test | `chooser_onboardingDone_choosesCamera`: `done = true`: `CAMERA`, `reads == 1` |
| KB-4, cờ màn chào tắt | Unit test | `chooser_onboardingFlagOff_choosesCamera_withoutReadingStatus`: chỉ cờ `CAMERA`: `CAMERA`, `reads == 0`. Qua ngay ở bước 3; test chặn |
| Màn chào cần camera (mục 8, câu 2) | Unit test | `chooser_onboardingOnCameraOff_choosesSample`: chỉ cờ `ONBOARDING`: `SAMPLE`, `reads == 0`. Qua ngay ở bước 3; test chặn |
| Bắt lỗi đọc DataStore (rà soát T-010) | Unit test | `chooser_statusReadFails_showsOnboarding_andLogsWarning`: `readError = IOException()`; không ném; `ONBOARDING`; log `WARN` thẻ `Navigation`, `error is IOException`. Thất bại ở bước 3: `CAMERA` |
| Log màn chính | Unit test | `chooser_logsChosenScreen`: log `INFO` thẻ `Navigation` "Main screen: ONBOARDING" |
| Test cũ giữ ý | Unit test | `chooserBeforeOnboardingAndCamera_choosesSample`, `chooser_cameraFlagOn_choosesCamera`, `chooser_cameraFlagOff_choosesSample` đổi `MainScreenChooser(FakeFeatureFlags(...))` thành `MainScreenChooser(FakeFeatureFlags(...), FakeOnboardingStatus(done = false), RecordingAppLogger())`, điều kiểm tra giữ nguyên. Thay đổi này và `FakeOnboardingStatus` vào commit test của bước 3 (L-025) |
| Quy tắc phụ thuộc module (ADR-0001) | Kiểm tra tự động | `checkModuleDependencies` trong `scripts/check.sh`: `:feature:onboarding` chỉ phụ thuộc core |
| Cờ mới mặc định tắt | Unit test có sẵn | `DefaultFeatureFlagsTest.everyFlag_isOffByDefault` |
| Màn hình thấp (L-028) | Screenshot test khung `vi-w360dp-h640dp-xxhdpi`, `@GraphicsMode(NATIVE)` | `OnboardingScreenScreenshotTest.default_small` ghi `src/test/screenshots/OnboardingScreen_default_small.png` (giao diện sáng; hình co, nút và ghi chú ở đáy) |
| Đúng thiết kế | Screenshot test, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")` | `default_light` ghi `src/test/screenshots/OnboardingScreen_default_light.png`, `default_dark` ghi `src/test/screenshots/OnboardingScreen_default_dark.png`; `uiState = Ready`. Chạy `scripts/design-compare.sh onboarding`, commit `docs/features/onboarding/design/review/`, dán `review/summary.md` vào mô tả PR và README mục 8 (ADR-0020). Rà soát độc lập theo README mục 7, nhất là màu viền thẻ theo chế độ, nét viền tư thế, chữ hai dòng của tiêu đề |
| KB-1, KB-3 trên app thật | Test trên emulator (CI) | `AppLaunchTest.appStarts_passesOnboarding_andReachesCamera` (thay `appStarts_andReachesStartScreen`): giữ luật cấp quyền camera trước khi mở app và 60 giây chờ. `waitUntil(60_000)` tới khi có node `onboarding_screen` hoặc `camera_screen` (`fetchSemanticsNodes()`). Có `onboarding_screen` thì `onNodeWithTag("onboarding_enable_camera").performClick()` rồi `waitUntil(10_000)` tới khi có `camera_screen`. Quyền đã cấp nên không có hộp hỏi quyền; không dùng `assertIsDisplayed` (hộp đồng ý quảng cáo phủ lên app). Bản cài mới trên CI luôn có màn chào (cờ bật ở staging); nhánh "đã ở camera" chỉ để test không phụ thuộc dữ liệu cũ trên emulator |
| KB-1, hộp hỏi quyền thật, 2 chạm | Kiểm tra thủ công | Cài mới bản staging, mở app có mạng: loading xong hiện màn chào, không quảng cáo. Bấm "Bật camera": hộp hỏi quyền hiện. "Cho phép": camera chạy ngay, không màn nào chen giữa (nét viền tư thế mặc định khi T-014 có). Log `VGL.Onboarding` có "Enable camera tapped", "Camera permission granted"; `VGL.Navigation` "Main screen: ONBOARDING" |
| KB-1, quay lại đóng app | Kiểm tra thủ công | Ở màn chào bấm quay lại: app đóng. Mở lại: màn chào hiện lại (chưa bấm) |
| KB-2 trên máy | Kiểm tra thủ công | Cài mới, "Bật camera", "Không cho phép": camera ở trạng thái chưa có quyền, không hỏi lại, quay lại thì app đóng. "Không hỏi lại": cài mới, trước khi mở app chạy `adb shell pm set-permission-flags <gói staging> android.permission.CAMERA user-fixed` (Android 11 trở lên), mở app, bấm "Bật camera": không có hộp, sang thẳng camera chưa có quyền; log `VGL.Onboarding` có "Camera permission denied without dialog", DebugView không có `camera_permission_result` (phiên bản 2). Lần "Không cho phép" ở trên: log "Camera permission denied" và DebugView có `denied`, `source` `onboarding` |
| KB-3 trên máy | Kiểm tra thủ công | Sau KB-1 hay KB-2, đóng hẳn app, mở lại: loading xong vào thẳng camera. Cài đặt máy, xóa dữ liệu app, mở lại: màn chào hiện |
| KB-4 trên máy | Kiểm tra thủ công | Không làm được trên bản staging (cờ bật cục bộ, Remote Config không tắt được). Kiểm bằng unit test `chooser_onboardingFlagOff_choosesCamera_withoutReadingStatus` và nhánh `CAMERA` hiện có |
| Màn rộng và màn thấp thật | Kiểm tra thủ công | Máy màn nhỏ hoặc cỡ chữ hệ thống lớn nhất: phần trên cuộn được, nút và ghi chú luôn thấy. Máy gập mở ra hoặc máy tính bảng (khóa dọc bị bỏ qua): không cắt chữ hay nút |

**Ngoại lệ viết test trước:**

- `AppNavHost.kt` (đăng ký `onboardingScreen`, điều hướng sang camera, chặn điều hướng hai lần), `MainActivity.kt`, `OnboardingStatusModule.kt`: điều hướng cần `LoadingRoute` với `hiltViewModel()`, không có test JVM riêng (như T-010, T-013). Kiểm bằng `AppLaunchTest` trong CI và kiểm tra thủ công.
- `OnboardingRoute` (launcher hỏi quyền, thu sự kiện, đọc "nên giải thích" trước và sau khi hỏi): cần hộp quyền của hệ thống; logic nằm trong ViewModel và hàm thuần `permissionDialogShown`, đã có test (như ngoại lệ của T-026). Kiểm bằng `AppLaunchTest` (nhánh quyền có sẵn) và kiểm tra thủ công (nhánh hộp hỏi quyền).
- 5 drawable, `LICENSE.txt`, 6 thư mục ngôn ngữ: tài nguyên, có test chữ và screenshot test.
- `AppLaunchTest` đổi điều nó kiểm tra (từ "tới camera" thành "qua màn chào rồi tới camera") vì màn đích ở staging đổi theo spec; người duyệt cần đồng ý.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 4 (sự kiện mới `onboarding_complete`; giá trị mới `source = onboarding` của `camera_permission_result`; cả hai có trong spec mục 6) và điểm 7 (cờ mới trong `:core:config`; thêm module vào `settings.gradle.kts`). Rủi ro cao, bắt buộc duyệt chéo; người phụ trách sản phẩm biết trước vì chạm dữ liệu.

Không chạm: điểm 3 (quyền `CAMERA` đã có từ T-013, không thêm quyền); điểm 5 (hộp hỏi quyền là của hệ thống, không phải hộp đồng ý; thứ tự khởi tạo SDK không đổi); điểm 9 (khóa DataStore mới, không chuyển dữ liệu cũ). Người duyệt thấy khác thì sửa `blocking_points`.

Bảng điểm dưới đây chỉ để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Module mới và ba module khác (`:core:config`, `:app`, cấu hình Gradle), khoảng 45 file; đổi giao diện công khai của `AppNavHost` và `MainScreenChooser` |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, thiết kế đã duyệt, cách lưu đã chốt ở plan T-010; AI tự quyết vài điểm nhỏ: nguồn nét viền tư thế, màn chào cần cờ camera, đọc lỗi thì hiện màn chào, quyền có sẵn thì không hỏi (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Logic, bố cục, màu, 7 ngôn ngữ có test JVM; luồng tới camera có test emulator. Hộp hỏi quyền thật, "không hỏi lại", xóa dữ liệu, máy rộng chỉ kiểm tra thủ công |
| D. Ảnh hưởng chéo | 3 | Đổi màn đầu tiên sau loading ở staging (mọi người thử và `AppLaunchTest` của mọi PR từ nay đi qua màn chào); sửa `AppNavHost.kt`, `MainActivity.kt`, `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`, `settings.gradle.kts`, `app/build.gradle.kts` mà T-014, T-016, T-021, T-025 có thể cũng sửa |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu camera và loading; mới là `Layout` riêng cho hình co trước khi cuộn và một test emulator có bấm nút. Cách suy ra hộp hỏi đã có ở T-026 (phiên bản 2) |
| **Tổng** | 10 | **Mức: Cao** (chạm điểm chặn, và tổng điểm cũng từ 9 trở lên) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `FeatureFlag.kt`; `StagingFeatureFlagsModule.kt`; `settings.gradle.kts`; `app/build.gradle.kts`; `AppNavHost.kt` (tham số mới `markOnboardingDone`, đích mới); `MainScreenChooser.kt` và `MainScreenTest.kt` của T-010 (constructor mới); `MainActivity.kt` (tiêm thêm một lớp); `AppLaunchTest.kt` (màn đầu ở staging đổi thành màn chào); DataStore chung (khóa `onboarding_done`, không trùng khóa nào hiện có: `sample_count`, `camera_*`, khóa của `:core:poses`).
- Hành vi mà người khác dựa vào: bản staging cài mới từ nay mở màn chào trước camera. Phiên thử (`device-session`, L-009) và test emulator của các plan sau phải bấm qua màn chào hoặc chờ một trong hai màn. Lần mở sau vẫn vào thẳng camera.
- Task khác có thể xung đột (các plan này đang được viết song song, AI chưa đọc được nội dung):
  - T-014 (chọn tư thế, nét viền, thẻ bước trong camera): chủ yếu trong `:feature:camera`; có thể sửa `AppNavHost.kt` (tham số của `cameraScreen`) và chữ của camera. Không trùng logic; T-012 không đổi dòng `cameraScreen(...)`.
  - T-016 (màn xem lại): sửa `AppNavHost.kt` (`onPhotoCaptured`, đích xem lại), có thể `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`. Chỉ xung đột dòng.
  - T-021 (thư viện tư thế, `:feature:poses` mới) và T-025 (cài đặt, `:feature:settings` mới): cùng thêm dòng vào `settings.gradle.kts`, `app/build.gradle.kts`, `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`, `AppNavHost.kt` (đích mới, nối `onOpenPoses`, `onOpenSettings`). T-025 có thể đụng DataStore chung (khóa cài đặt). Chỉ xung đột dòng, không xung đột logic.
  - Nếu một task trong số đó cũng đổi `AppLaunchTest.kt` hay màn đầu sau loading, task chạy sau phải dựng trên bản đã có của task chạy trước.
- T-026 (phiên bản 2): T-012 chép hàm `permissionDialogShown` và năm test của T-026, nên T-026 phải merge trước; ticket T-012 phụ thuộc thêm T-026 để phiên bỏ qua T-012 khi T-026 dừng. Sau này đổi quy tắc thì phải sửa cả hai bản (mục 8, câu 13).
- Cách xử lý: phiên tự chạy làm từng task trên nhánh chính mới nhất, nên task chạy sau thấy phần của task trước. T-012 giữ nguyên mọi dòng của task khác trong các file chung và chỉ thêm phần của mình (mục 3.1). Thứ tự trong file (cờ, `include`, `implementation`, hàm staging) theo thứ tự hiện có, thêm vào cuối nhóm.
- Đề xuất thứ tự chạy: T-026 trước T-012 (phiên bản 2). T-012 không phụ thuộc T-014, T-016, T-021, T-025 và ngược lại. Đề xuất chạy T-012 trước bốn task đó: nó đổi `AppLaunchTest.kt` và màn đầu ở staging, nên các task sau dựng test emulator trên luồng đã có màn chào ngay từ đầu. Chạy sau cũng được nếu không task nào trong bốn task đổi `AppLaunchTest.kt`; nếu có, xem điều kiện dừng 3.

## 7. Điều kiện dừng riêng

1. Lúc bắt đầu task, API của T-010, T-013 khác mục 3.1: `MainScreenInputs` không có ba trường, `MainScreenChooser` không có `suspend fun choose()` và `inputs()`, `AppNavHost` không có `chooseMainScreen` và `MainScreen.destination()` với nhánh `ONBOARDING`, hoặc `CameraDestination` không có `askPermission`: dừng.
2. Thiếu `appColors.categoryWomen`, `categoryCouple`, `categoryFriends`, `onCategory`, `Radius.card`, `Radius.full`, `Stroke.sticker`, hoặc `PrimaryButton` không có `icon`, `loading`: dừng, không tự thêm màu hay viết cứng mã màu.
3. `AppLaunchTest.kt` trên nhánh chính đã bị task khác đổi thành chờ một màn không phải `camera_screen` (ví dụ màn khác thành màn đầu): dừng, không tự gộp hai cách chờ.
4. `AppLaunchTest` đỏ ở bước bấm "Bật camera" (không tìm thấy hay bấm không được `onboarding_enable_camera`, ví dụ vì hộp đồng ý quảng cáo, hoặc không tới `camera_screen` sau 10 giây) sau một lần sửa: dừng và ghi chi tiết từ bước "Chi tiết test trên emulator" (ADR-0019). Không bỏ bước bấm, không tăng 10 giây nếu người duyệt chưa đồng ý phương án dự phòng (mục 8, câu 8).
5. `AppLaunchTest` đỏ vì loading dừng ở `Failed` (thiếu file trên R2 staging, điều kiện của T-010): dừng, không tăng thời gian chờ.
6. Một test sẵn có ngoài ba test chooser của `MainScreenTest.kt` và `AppLaunchTest.kt` thất bại sau thay đổi (ví dụ test của `:feature:camera` vì đổi điều hướng).
7. Cần sửa file của `:feature:camera`, `:core:designsystem`, `:core:data` hoặc `AndroidManifest.xml` để code qua.
8. `ShrinkFirstLayout` không cho kết quả như mục 4 (hình không co, hay nút bị đẩy khỏi khung thấp) sau 3 lần sửa: dừng (điều kiện chung 4); không đổi số trong test.
9. (Phiên bản 2) T-026 chưa merge, hoặc `permissionDialogShown` của `:feature:camera` trên nhánh chính khác quy tắc `granted || rationaleBefore || rationaleAfter` hay khác tên, tham số: dừng, không tự chọn quy tắc khác.

## 8. Câu hỏi cho người duyệt

1. **Nét viền tư thế trong hình minh họa lấy ở đâu?** `context/` chỉ ghi khung của hai nét (đầu và thân), không có đường vẽ; repo chưa có file SVG; phiên tự chạy không dùng Figma (figma-export.py chỉ chạy trong phiên có người).
   - Đề xuất của AI: dev xuất frame "pose-outline" của thẻ "couple" ra SVG, lưu `docs/features/onboarding/design/handoff/pose_outline.svg` trước phiên (ghi vào "Việc dev làm trước khi kích hoạt" của danh sách chạy). Không có file thì AI vẽ tay một nét viền người đứng gần giống, đúng khung 70 x 140 và vị trí hai nét, ghi vào Cần người xem; dev thay bằng nét thật ở plan sau nếu cần.
   - Trả lời: Đồng ý với đề xuất của AI.
2. **Màn chào khi cờ camera tắt.** Màn chào chỉ dẫn tới camera; nếu cờ camera tắt mà cờ màn chào bật thì "Bật camera" không có nơi để đến.
   - Đề xuất của AI: màn chào chỉ có khi cả hai cờ bật (`onboardingAvailable = ONBOARDING && CAMERA` trong `MainScreenChooser`, không đổi `chooseMainScreen`). Cờ camera tắt thì vào màn mẫu như plan T-010, câu 3. Sau màn chào luôn điều hướng tới camera.
   - Trả lời: Đồng ý với đề xuất của AI.
3. **Đọc trạng thái "đã qua" lỗi thì làm gì?** (ghi chú rà soát T-010)
   - Đề xuất của AI: log `warn` và coi là chưa qua, tức hiện màn chào. Người đã qua phải bấm thêm một lần, còn người mới không bị bỏ lỡ màn giải thích trước khi hỏi quyền. Chỉ bắt `IOException` (cách của dự án); lỗi khác vẫn ném như mọi chỗ đọc DataStore khác.
   - Trả lời:  Đồng ý với đề xuất của AI.
4. **Quyền camera đã có sẵn khi màn chào hiện** (ví dụ cài đè, hay quyền được cấp bằng `adb`).
   - Đề xuất của AI: không hỏi, không ghi `camera_permission_result` (hệ thống không trả kết quả nào), vẫn ghi `onboarding_complete` và sang camera. Spec ghi "khi hệ thống trả kết quả quyền thì ghi", nên không ghi là đúng chữ của spec; tỷ lệ cho phép (`granted` trên `onboarding_complete`) sẽ thấp hơn thật một chút trong trường hợp hiếm này.
   - Trả lời: Đồng ý với đề xuất của AI.
5. **Quyết định còn mở của RUN-20261022-01: ghi `camera_permission_result: denied` khi quyền đã bị từ chối vĩnh viễn (không có hộp).** Ở màn chào điều này hiếm (chỉ lần qua màn chào đầu tiên, và chỉ khi quyền đã bị khóa từ trước), nhưng cùng sự kiện với camera nên nên làm giống nhau.
   - Đề xuất của AI: T-012 làm giống camera hiện nay (ghi `denied`). Nếu người duyệt quyết không ghi khi không có hộp, AI đề xuất cách đo: hệ thống trả kết quả trong dưới 300 ms sau khi gọi, hoặc `shouldShowRequestPermissionRationale` là `false` cả trước và sau khi hỏi; sửa cả camera và màn chào trong một plan riêng, cần spec ghi rõ.
   - Trả lời: Đồng ý với đề xuất của AI.
   - Phiên bản 2: câu trả lời trên đã bị thay. Nhật quyết không ghi khi hộp hỏi không hiện (2026-10-22); camera làm ở T-026, màn chào làm trong phiên bản này với cùng quy tắc của T-026 thay vì quy tắc 300 ms. Spec `onboarding` đã cập nhật.
6. **Hộp đồng ý quảng cáo và "đúng 2 chạm".** Ở vùng cần đồng ý (ví dụ EEA), `MainActivity` hiện hộp đồng ý UMP khi mở app lần đầu (ADR-0013). Nếu người dùng chưa trả lời lúc loading xong, hộp còn phủ lên màn chào và thêm chạm.
   - Đề xuất của AI: không đổi trong ticket này (điểm chặn 5); coi hộp đồng ý là yêu cầu pháp lý, không tính vào 2 chạm của spec. Nếu muốn đo, thêm câu hỏi vào spec `onboarding` mục đo kết quả.
   - Trả lời: Đồng ý với đề xuất của AI.
7. **Hình minh họa co tới đâu ở màn thấp?** README cho phép co trước tiên nhưng không ghi mức thấp nhất.
   - Đề xuất của AI: thấp nhất 160 dp (thẻ khoảng 114 dp, vẫn nhận ra ba thẻ và nét viền). Ở 360 x 640 phần chữ vẫn cần cuộn với mọi ngôn ngữ.
   - Trả lời: Đồng ý với đề xuất của AI.
8. **`AppLaunchTest` bấm qua màn chào.** Test emulator sẽ bấm "Bật camera" khi hộp đồng ý quảng cáo có thể đang phủ lên app. Bấm của Compose test gửi thẳng vào view của app nên AI cho là vẫn được, nhưng chưa thử (L-001).
   - Đề xuất của AI: giữ bước bấm (kiểm KB-1 trên app thật). Nếu CI cho thấy không bấm được, phương án dự phòng: test chỉ chờ `onboarding_screen` hoặc `camera_screen`; người duyệt đồng ý trước thì phiên dùng phương án này mà không dừng, ghi vào nhật ký. Không đồng ý thì phiên dừng (mục 7, điều 4).
   - Trả lời: Đồng ý với đề xuất của AI.
9. **Vùng sau thanh trạng thái.** Như câu 12 của plan T-013: `AppNavHost` đặt `safeDrawingPadding` cho mọi màn, nên trên máy thật lề trên 56 tính từ dưới thanh trạng thái, nội dung thấp hơn ảnh thiết kế một khoảng bằng thanh trạng thái.
   - Đề xuất của AI: giữ nguyên; màn chào theo theme máy nên màu sau thanh trạng thái trùng nền, không lộ dải khác màu. Phần chữ cuộn được nên không mất nội dung.
   - Trả lời: Đồng ý với đề xuất của AI.
10. **Bản dịch.** Mục 3.1 có bảng 7 ngôn ngữ do AI dịch. Riêng tiếng Anh, tiêu đề "Strike a great pose, no guesswork" và nút "Turn on camera".
    - Đề xuất của AI: dùng bảng này; người biết tiếng duyệt trước khi phát hành (spec, câu hỏi 1). Người duyệt muốn đổi chữ nào thì sửa thẳng vào bảng trước khi duyệt.
    - Trả lời: Đồng ý với đề xuất của AI.
11. **Thứ tự chạy với T-014, T-016, T-021, T-025** (mục 6).
    - Đề xuất của AI: T-012 chạy trước bốn task này trong cùng phiên.
    - Trả lời: Đồng ý với đề xuất của AI.

12. **(Phiên bản 2) Giữ "nên giải thích trước" qua lần activity bị tạo lại.** T-026 lưu giá trị này bằng `remember`; nếu activity bị tạo lại lúc hộp hỏi đang mở, giá trị về `false`, và lần từ chối thứ hai (trước `true`, sau `false`) bị coi là không hiện hộp. Màn chào đã xử lý trường hợp tạo lại (`requesting_permission` trong `SavedStateHandle`).
    - Đề xuất của AI: màn chào dùng `rememberSaveable` (khác T-026 một chữ, ghi trong KDoc). Ở màn chào gần như luôn là lần hỏi đầu (trước `false`), nên khác biệt chỉ có ý nghĩa khi cài đè. Nếu muốn camera cũng vậy thì sửa ở ticket sau của camera, không sửa trong T-012.
    - Trả lời: Đồng ý với đề xuất của AI.
13. **(Phiên bản 2) Hai bản của cùng một hàm.** `permissionDialogShown` có ở `:feature:camera` và `:feature:onboarding` (ADR-0001 không cho feature dùng feature khác).
    - Đề xuất của AI: chấp nhận hai bản như câu 2 của plan T-026 đã duyệt; hai file test giống nhau giữ quy tắc không lệch. Khi có nơi thứ ba thì đề xuất đưa vào một module core (đề xuất về dự án mẫu nếu có ích chung).
    - Trả lời: Đồng ý với đề xuất của AI.

Ghi nhận khi đọc tài liệu, không phải mâu thuẫn chặn plan: chữ của `:feature:camera` đang nằm trong `values/` bằng tiếng Việt, trong khi T-008 và spec `onboarding` câu hỏi 1 đặt tiếng Anh làm ngôn ngữ gốc. Plan này không chạm camera; đề xuất tạo ticket dịch chữ camera. Trong `context/`, tên lớp chữ của điểm 3 là "60 tư thế dùng được khi không ..." (tên cũ), nội dung là "Lưu tư thế yêu thích để chụp lại sau"; plan theo nội dung và bảng chữ.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1: Duyệt (Nhật, duyệt chéo Tuấn, 2026-10-22); thay bằng phiên bản 2 trước khi chạy vì quyết định ở mục 8, câu 5 đổi.

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** 

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-YYYYMMDD-NN, phiên bản plan 2

- **Kết quả:** đã merge / dừng / CI thất bại
- **Nhánh, PR:** <...>
- **Đã làm:** <...>
- **Test viết trước:** <số test, xác nhận đã thất bại đúng lý do trước khi viết code>
- **Rà soát độc lập:** <số điểm phát hiện, đã xử lý những gì, điểm nào còn để người xem>
- **Kết quả CI:** <build, lint, unit test, UI test, screenshot test>
- **Lệch so với plan:** <chỗ nào và vì sao, hoặc "Không có">
- **Việc còn lại:** <...>
- **Cần người xem:** <điểm dev nên kiểm tra sáng nay>

### RUN-20261023-01, phiên bản plan 2

- **Kết quả:** chưa xong, dừng vì nhánh chính đỏ (issue #35 `main-broken` mở lúc 16:58 UTC, trong khi task đang làm; auto-run.md mục 4). Không do plan: plan và ticket giữ `approved`, đưa cùng phiên bản vào danh sách chạy sau, tiếp tục trên nhánh cũ.
- **Nhánh, PR:** `claude/T-012-man-chao`, PR #36 (mở, không merge). Commit: dabc2a3 (test và khung), bdf0bcb (code), da6c8e9 (ảnh chuẩn), 15f10b2 (đối chiếu thiết kế), eb97846 (sửa theo rà soát).
- **Đã làm:** bước 3 đến 7 đủ theo mục 3.1 và 3.2 (47 file, đều trong danh sách). Bước 3 và 4 do một phiên AI viết code riêng làm (phiên chính giao việc để giữ ngữ cảnh gọn), phiên chính kiểm lại danh sách file, chạy rà soát độc lập, sửa theo rà soát, mở PR.
- **Test viết trước:** 33 test JVM mới trong `:feature:onboarding`, 3 screenshot test; trong `:app` 11 test mới và 3 test chooser đổi cách dựng; `AppLaunchTest` đổi điều kiểm tra như mục 4. Trên khung rỗng 35 test thất bại đúng lý do (không có sự kiện Turbine, không có node, chữ tiếng Anh, `CAMERA` thay vì `ONBOARDING`, DataStore không ghi). Qua ngay: các test chặn mục 4 đã nêu và 3 screenshot test (chưa có ảnh chuẩn nên Roborazzi không so). `permissionResult_whenNotRequesting_isIgnored` qua ngay trên khung nên được viết chặt thêm (bấm lại sau kết quả lạc vẫn hỏi quyền), không đổi điều nó kiểm tra. Sau rà soát thêm `restoredReady_withCompletionLogged_tapDoesNotLogAgain` (test chặn, qua ngay).
- **Rà soát độc lập:** 0 chặn, 1 nên sửa (đã sửa: `onStarted` chạy `scope.launch(start = CoroutineStart.UNDISPATCHED)` để `markDone` vào `NonCancellable` ngay trong lần bấm), 8 ghi chú (đã làm: test khóa `completion_logged`, sửa câu lệch nút và ghi chú trong README mục 8, thêm nhận xét rà soát vào README). Bản dịch 49/49 chuỗi khớp bảng plan.
- **Kết quả CI:** lần chạy 39821691045: `changes`, `build-and-test` xanh; `ui-test` đỏ. `AppLaunchTest.appStarts_passesOnboarding_andReachesCamera`: `IllegalStateException: Method setCurrentState must be called on the main thread` tại `AppNavHost.kt:59` (`navController.navigate` trong `onFinished` của loading), stack cho thấy coroutine được tiếp tục trên `LimitedDispatcher$Worker`. `chooseMainScreen()` nay đọc DataStore, và dưới bộ điều phối của Compose test, coroutine của `rememberCoroutineScope` tiếp tục trên luồng nền. Lỗi thật của T-012, đây là lần đỏ thứ nhất; chưa sửa vì phiên phải dừng.
- **Lệch so với plan:** thêm hằng `OnboardingTestTags.CONTENT` (vùng cuộn, test màn thấp cần); `default_illustrationFullHeight_whenRoomEnough` gộp vào `default_layoutMatchesDesign` (plan cho phép); nét viền dùng nguyên hai path của `handoff/pose_outline.svg`, màu qua tint `onCategory`; `UNDISPATCHED` ở `onStarted` (rà soát).
- **Việc còn lại:** sửa lỗi luồng ở `AppNavHost.kt` (trong danh sách file): phần sau `chooseMainScreen()` chạy trên `Dispatchers.Main.immediate`, ví dụ `scope.launch { val screen = currentChooseMainScreen(); withContext(Dispatchers.Main.immediate) { ...navigate... } }`; chạy lại `check.sh`, đẩy, chờ `ui-test`; rồi bước 9, 10. Nếu `ui-test` đỏ ở bước bấm "Bật camera" thì theo mục 8, câu 8 và mục 7, điều 4.
- **Cần người xem:** 3 ảnh chuẩn mới và ảnh so sánh thiết kế (4,3% khác, lệch ở chỗ ngắt dòng và nút 56 dp); bản dịch 6 ngôn ngữ; dòng lịch sử ngày 2026-10-22 của spec `onboarding` còn ghi "Chờ xác nhận lại" trong khi spec `confirmed`.

### RUN-20261023-02, phiên bản plan 2

- **Kết quả:** đã merge (PR #36, commit 9c141d7, 23:35 UTC).
- **Nhánh, PR:** `claude/T-012-man-chao`, PR #36. Tiếp tục trên nhánh cũ: merge nhánh chính 3890834 (ffc6bf2), sửa lỗi luồng (9a9cad6).
- **Đã làm:** `AppNavHost.kt`: `onFinished` của loading chạy `scope.launch(Dispatchers.Main.immediate)`, nên `navigate` sau `chooseMainScreen()` (đọc DataStore) luôn tiếp tục trên luồng chính. Đúng cách sửa ghi ở "Việc còn lại" của lần trước, chỉ một file trong danh sách.
- **Test viết trước:** không thêm test (bước 3 đã xong ở RUN-20261023-01). `AppNavHost.kt` thuộc ngoại lệ đã duyệt (mục 4); lỗi được kiểm bằng `AppLaunchTest`, đỏ trước khi sửa (lần chạy 39821691045), xanh sau khi sửa.
- **Rà soát độc lập:** phiên con chỉ đọc, xem commit 9a9cad6: 0 chặn, 0 nên sửa, 4 ghi chú: (1) chú thích nói luồng nền do Compose test chọn, thật ra là luồng của DataStore, giữ nguyên; (2) với `.immediate`, khi không có lần treo nào `navigate` chạy ngay trong effect của `LoadingRoute`, không hại; (3) phần sau `markDone` ở `onStarted` có thể chạy trên luồng nền, hiện chỉ ghi log; task sau thêm thao tác giao diện ở đó thì phải đưa về luồng chính; (4) `AppLaunchTest` chưa từng đỏ ở bước bấm "Bật camera".
- **Kết quả CI:** lần chạy 39870610590 (9a9cad6): `changes`, `build-and-test`, `ui-test` đều xanh vòng đầu. `scripts/check.sh` cục bộ qua: 410 test JVM, 0 lỗi, 34 ảnh screenshot.
- **Lệch so với plan:** Không có (thêm so với lần trước).
- **Việc còn lại:** kiểm tra thủ công ở mục 4.
- **Cần người xem:** như lần trước (3 ảnh chuẩn mới, ảnh so sánh thiết kế, bản dịch 6 ngôn ngữ); bấm "Bật camera" khi có hộp đồng ý quảng cáo phủ lên đã qua trên CI (mục 8, câu 8).
