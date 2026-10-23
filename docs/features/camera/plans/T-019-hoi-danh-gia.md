---
ticket: T-019
title: Hỏi đánh giá app trong app
status: approved             # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 15M          # số gợi ý cỡ S là 5.0M (tạm, mới đo 2 task); chỉnh lên, lý do ở mục 5
blocking_points: [sdk, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-019: HỎI ĐÁNH GIÁ APP TRONG APP

## 1. Tóm tắt

Tạo module core mới `:core:review` bọc Play In-App Review: luật "đã lưu ít nhất 3 ảnh, lần hỏi trước cách ít nhất 30 ngày, cờ `feature_in_app_review_enabled` bật" nằm trong một hàm thuần Kotlin, hai con số đọc từ `RemoteSettings`, thời điểm hỏi gần nhất lưu trong DataStore dùng chung. Màn xem lại gọi việc hỏi đánh giá khi rời màn sau khi đã lưu ảnh ở lần xem lại đó, sau khi đã về camera, và bỏ qua khi quảng cáo `review_interstitial` của T-018 vừa hiện ở cùng lần rời. Kiểm chứng bằng unit test của luật và của thứ tự rời màn, test Robolectric dùng `FakeReviewManager` của Google, test DataStore, và kiểm tra thủ công trên bản cài từ Google Play.

Phiên bản 2 (2026-10-23): rà soát độc lập trước RUN-20261023-03 thấy hai điểm. Một: mọi module có `consumer-rules.pro` (`AndroidLibraryConventionPlugin.kt` khai báo `consumerProguardFiles("consumer-rules.pro")`), plan chưa tạo cho `:core:review`. Hai: phần nối `askReview` trong `ReviewRoute` (đọc `savedCount`, truyền vào `leaveReview`, `afterReviewAd`) nằm ngoài mọi test. Thay đổi so với phiên bản 1: tạo `core/review/consumer-rules.pro` rỗng như mọi module core, đã chạy thử bản R8 (mục 4.1); `InAppReview` thành interface công khai, lớp cài đặt `DefaultInAppReview` là `internal`, để test của feature cung cấp bản giả qua `LocalInAppReview`; thêm hai test vào `ReviewRouteTest.kt` của T-018 (lưu rồi rời màn thì hỏi đánh giá đúng một lần, sau khi đã về camera; không lưu thì không hỏi); `token_estimate` 14M lên 15M. Phần còn lại giữ nguyên phiên bản 1, kể cả câu trả lời ở mục 8.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-10 | Khi người dùng đã lưu ít nhất 3 ảnh (tính từ khi cài) và rời màn xem lại sau khi đã lưu ảnh ở lần xem lại đó thì app gọi hộp đánh giá của Google Play, nếu lần gọi trước đã cách ít nhất 30 ngày | Số ảnh đã lưu lấy từ `SavedPhotosRepository.savedCount` của T-016 (khóa `camera_saved_photo_count`, tính từ khi cài). Chỉ hỏi khi rời màn với `ReviewEvent.Leave(savedThisVisit = true)`, tức là lần xem lại đó đã lưu ảnh (spec đã sửa theo câu hỏi 1, PR #64); bỏ ảnh hay mở ảnh cũ từ ô ảnh vừa chụp thì không hỏi. "Gọi hộp đánh giá" là gọi Play In-App Review; Google quyết định có hiện hộp hay không (hạn mức của Play), app không biết. "Lần gọi trước" là lần gần nhất app gọi xong luồng đánh giá không lỗi; chưa gọi lần nào thì đủ điều kiện. Ba con số (3 ảnh, 30 ngày) đặt trong `RemoteSettings` theo ticket |
| KB-10 | Khi quảng cáo toàn màn hình vừa hiện ở cùng lần rời màn xem lại thì app không gọi hộp đánh giá lần đó | `afterReviewAd(shown = true, ...)` của T-018 không hỏi đánh giá; `shown = false` thì hỏi sau khi về camera |
| KB-10 | Khi cờ `feature_in_app_review_enabled` tắt thì app không gọi hộp đánh giá | Cờ mới trong `FeatureFlag`, mặc định tắt (ADR-0003). `:core:review` đọc cờ mỗi lần hỏi bằng `enabled(flag).first()`: đây là một quyết định tại một thời điểm, không phải màn hình theo dõi cờ |
| Ticket | Tối đa một lần mỗi 30 ngày, không cùng lúc với quảng cáo toàn màn hình | Như trên |

Thiết kế tham chiếu: Không có thiết kế riêng: hộp đánh giá do Google Play vẽ (docs/ops/design-workflow.md).

ADR liên quan: ADR-0001 (module core mới chỉ phụ thuộc core), ADR-0003 (cờ mặc định tắt; tham số kinh doanh qua `RemoteSettings`, feature không đọc `RemoteSettings`), ADR-0019 (ghi log qua `AppLogger`).

## 3. Giải pháp

### 3.1. Cách làm

**Vì sao một module core mới.** Ticket đặt tham số trong `RemoteSettings`, mà feature không được đọc `RemoteSettings` (chú thích của `RemoteSettings`: "Feature modules do not use this"). Việc hỏi đánh giá cũng có ích cho mọi app của nhóm. Đặt vào `:core:ads` thì sai vai trò của module. Vì vậy plan tạo `:core:review`, làm theo cách `:core:ads` đưa `AdsController` cho giao diện: một lớp singleton, một `CompositionLocal` do `MainActivity` cung cấp, một hàm `remember...` cho feature; trong test và preview không có gì được cung cấp nên không làm gì (spec câu hỏi 9: thư viện do dev thêm vào `gradle/libs.versions.toml` trước, đã có `play-review-ktx`).

**Module `:core:review`** (gói `com.vglstudio.camera.pose.line.guide.core.review`):

- `build.gradle.kts`: plugin `template.android.library`, `template.android.compose`, `template.hilt`; `namespace`; phụ thuộc `:core:config` (cờ, `RemoteSettings`), `:core:data` (DataStore dùng chung), `:core:logging`, `libs.play.review.ktx`, `libs.androidx.activity.compose` (`LocalActivity`).
- `ReviewSettings.kt`: `internal object ReviewSettings` với `MIN_SAVED_PHOTOS = "in_app_review_min_saved_photos"` (mặc định 3) và `MIN_DAYS_BETWEEN = "in_app_review_min_days_between"` (mặc định 30).
- `ReviewRules.kt`: `internal fun isReviewDue(savedPhotos: Int, minSavedPhotos: Long, lastAskedAtMillis: Long?, nowMillis: Long, minDaysBetween: Long): Boolean`. Đúng khi `savedPhotos >= minSavedPhotos` và (`lastAskedAtMillis == null`, hoặc `nowMillis - lastAskedAtMillis >= minDaysBetween * 86_400_000`, hoặc `lastAskedAtMillis > nowMillis` vì người dùng chỉnh lùi giờ máy: coi là đủ điều kiện để không bị kẹt mãi, Play vẫn giới hạn số lần hiện).
- `ReviewHistory.kt`: `internal interface ReviewHistory { suspend fun lastAskedAt(): Long?; suspend fun recordAsked(atMillis: Long) }` và `DataStoreReviewHistory` dùng `DataStore<Preferences>` của `:core:data`, khóa mới `in_app_review_last_asked_at` (`longPreferencesKey`). Khóa mới, không có dữ liệu cũ phải chuyển (không chạm điểm chặn 9). Đọc lỗi `IOException` thì coi như chưa hỏi lần nào và ghi log; ghi lỗi thì ghi log, không ném ra ngoài.
- `ReviewPrompt.kt`: `internal fun interface ReviewPrompt { suspend fun show(activity: Activity) }` và `PlayReviewPrompt(manager: ReviewManager)`: `val info = manager.requestReview()` rồi `manager.launchReview(activity, info)` (hai hàm `suspend` của `com.google.android.play.core.ktx`, đã chạy thử ở mục 4.1).
- `InAppReview.kt` (phiên bản 2: interface công khai và lớp cài đặt `internal`, để test của feature cung cấp bản giả):
  - `interface InAppReview { fun requestIfDue(activity: Activity, savedPhotos: Int) }`: trả về ngay, việc chạy trong `scope` của app (sống qua việc màn xem lại đóng).
  - `@Singleton internal class DefaultInAppReview @Inject constructor(prompt, flags: FeatureFlags, settings: RemoteSettings, history, clock: ReviewClock, logger: AppLogger, @ReviewScope scope: CoroutineScope) : InAppReview`; `requestIfDue` gọi `scope.launch { ask(activity, savedPhotos) }`.
  - `suspend fun ask(activity: Activity, savedPhotos: Int): Boolean` của `DefaultInAppReview` làm việc thật, trả `true` khi đã gọi luồng đánh giá xong; test gọi thẳng hàm này. Thứ tự: cờ tắt thì log `Review skipped: flag_off`, trả `false`; đọc hai con số từ `RemoteSettings` và `history.lastAskedAt()`; `isReviewDue` sai thì log `Review skipped: not_due saved=<n>`, trả `false`; gọi `prompt.show(activity)`; xong không lỗi thì `history.recordAsked(clock.nowMillis())`, log `Review asked`, trả `true`. Lỗi (mọi `Exception` trừ `CancellationException`, ví dụ máy không có Google Play) thì log `warn("Review failed", e)`, không ghi thời điểm, trả `false`; app không bao giờ dừng vì việc này.
  - Thẻ log `InAppReview` (`VGL.InAppReview` trên logcat, ADR-0019). Không ghi dữ liệu cá nhân; chỉ số ảnh đã lưu.
- `ReviewClock.kt`: `internal fun interface ReviewClock { fun nowMillis(): Long }`, bản thật là `System.currentTimeMillis()` (30 ngày là thời gian thật, phải giữ qua lần khởi động lại máy, nên không dùng đồng hồ đơn điệu).
- `InAppReviewHost.kt`: `val LocalInAppReview = staticCompositionLocalOf<InAppReview?> { null }` và `@Composable fun rememberRequestInAppReview(): (savedPhotos: Int) -> Unit`: lấy `LocalInAppReview.current` và `LocalActivity.current`; thiếu một trong hai (test, preview) thì hành động không làm gì.
- `di/ReviewModule.kt`: `ReviewManager` từ `ReviewManagerFactory.create(context)` (`@Singleton`); bind `InAppReview` vào `DefaultInAppReview`, `ReviewPrompt` vào `PlayReviewPrompt`, `ReviewHistory` vào `DataStoreReviewHistory`; `ReviewClock`; `@ReviewScope CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`, vì `launchReview` cần luồng chính.
- `consumer-rules.pro`: file rỗng, như mọi module core (`AndroidLibraryConventionPlugin.kt` khai báo `consumerProguardFiles("consumer-rules.pro")` cho mọi module thư viện). Thư viện review của Google Play tự mang luật R8 của nó; module không cần luật riêng (phiên bản 2, mục 4.1).
- `src/test/resources/robolectric.properties`: `sdk=35`, như các module khác (Robolectric 4.15 chưa có SDK 36; chạy thử đã gặp lỗi này khi thiếu file).

**Các module khác:**

- `settings.gradle.kts`: `include(":core:review")` sau `:core:poses`.
- `:core:config`, `FeatureFlag.kt`: `IN_APP_REVIEW("feature_in_app_review_enabled")`.
- `app/build.gradle.kts`: `implementation(project(":core:review"))`. `MainActivity.kt`: tiêm `InAppReview`, thêm `LocalInAppReview provides inAppReview` vào `CompositionLocalProvider` có sẵn của `LocalAdsController`.
- `app/src/staging/.../StagingFeatureFlagsModule.kt`: bật `FeatureFlag.IN_APP_REVIEW` trên staging (câu hỏi 2).
- `:feature:camera`:
  - `build.gradle.kts`: `implementation(project(":core:review"))`.
  - `ReviewExit.kt` (T-018 tạo): `leaveReview` và `afterReviewAd` nhận thêm `askReview: () -> Unit = {}` (mặc định rỗng, nên test của T-018 giữ nguyên). `leaveReview(false, ...)`: chỉ `goBack()`, không hỏi đánh giá (lần xem lại không lưu ảnh, câu hỏi 1); tham số có để test kiểm được là không gọi. `afterReviewAd(shown, ...)`: `goBack()`, rồi `askReview()` chỉ khi `shown == false`. Về camera trước, hỏi đánh giá sau, để hộp đánh giá hiện trên camera, không trên màn đã rời.
  - `ReviewViewModel.kt`: `val savedCount: StateFlow<Int> = repository.savedCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)`. Luồng đọc DataStore đã tự thử lại khi lỗi (T-016).
  - `ReviewScreen.kt`, trong `ReviewRoute`: `val requestReview = rememberRequestInAppReview()`; `val savedCount by viewModel.savedCount.collectAsStateWithLifecycle()`; `askReview = { requestReview(savedCount) }` (đọc giá trị mới nhất qua `rememberUpdatedState`), truyền vào cả `leaveReview` và `afterReviewAd`. Lần rời ngay sau khi lưu: `recordSaved` chạy trước `Leave` ở đường "Lưu ảnh" của hộp thoại; ở đường bấm nút sau khi lưu, người dùng bấm sau khi số đếm đã cập nhật (một lần ghi DataStore). Lệch một ảnh trong trường hợp hiếm chỉ làm lần hỏi đến chậm một lần rời màn.

Mẫu có sẵn: `LocalAdsController` và `rememberShowInterstitial` của `:core:ads` (singleton cho giao diện, không làm gì trong test); `DataStoreSavedPhotosRepository` (khóa mới trong DataStore dùng chung); `FullScreenAdGuard` (luật thuần Kotlin); `InMemoryDataStore`, `FakeRemoteSettings` trong test của `:core:poses` (chép thành bản riêng cho `:core:review`, vì test của module khác không dùng chung được).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `settings.gradle.kts` | `include(":core:review")` |
| Tạo | `core/review/build.gradle.kts` | Plugin, `namespace`, phụ thuộc (mục 3.1) |
| Tạo | `core/review/consumer-rules.pro` | File rỗng, như mọi module core (phiên bản 2) |
| Tạo | `core/review/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/review/ReviewSettings.kt` | Hai khóa và mặc định |
| Tạo | `core/review/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/review/ReviewRules.kt` | `isReviewDue` |
| Tạo | `core/review/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/review/ReviewHistory.kt` | `ReviewHistory`, `DataStoreReviewHistory` |
| Tạo | `core/review/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/review/ReviewPrompt.kt` | `ReviewPrompt`, `PlayReviewPrompt` |
| Tạo | `core/review/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/review/ReviewClock.kt` | `ReviewClock` |
| Tạo | `core/review/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/review/InAppReview.kt` | Interface `InAppReview`, lớp `DefaultInAppReview` |
| Tạo | `core/review/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/review/InAppReviewHost.kt` | `LocalInAppReview`, `rememberRequestInAppReview` |
| Tạo | `core/review/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/review/di/ReviewModule.kt` | Hilt: `ReviewManager`, bind `InAppReview` và hai interface nội bộ, đồng hồ, scope, qualifier `ReviewScope` |
| Tạo | `core/review/src/test/resources/robolectric.properties` | `sdk=35` |
| Tạo | `core/review/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/review/Fakes.kt` | `FakeReviewPrompt` (đếm lần gọi, có thể ném lỗi), `FakeFeatureFlags`, `FakeRemoteSettings`, `FakeReviewHistory`, `InMemoryDataStore` |
| Tạo | `core/review/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/review/ReviewRulesTest.kt` | Mục 4 |
| Tạo | `core/review/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/review/InAppReviewTest.kt` | Mục 4 (Robolectric, cần một `Activity`) |
| Tạo | `core/review/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/review/PlayReviewPromptTest.kt` | Mục 4 (`FakeReviewManager`) |
| Tạo | `core/review/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/review/DataStoreReviewHistoryTest.kt` | Mục 4 |
| Tạo | `core/review/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/review/RequestInAppReviewTest.kt` | Mục 4 (Compose, Robolectric) |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FeatureFlag.kt` | `IN_APP_REVIEW("feature_in_app_review_enabled")` |
| Sửa | `app/build.gradle.kts` | `implementation(project(":core:review"))` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | Tiêm `InAppReview`, cung cấp `LocalInAppReview` |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingFeatureFlagsModule.kt` | Bật `IN_APP_REVIEW` trên staging |
| Sửa | `feature/camera/build.gradle.kts` | `implementation(project(":core:review"))` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewExit.kt` | Tham số `askReview` (mục 3.1) |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewViewModel.kt` | `savedCount: StateFlow<Int>` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreen.kt` | `ReviewRoute` truyền `askReview` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewExitTest.kt` | Thêm test hỏi đánh giá; test của T-018 giữ nguyên |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewRouteTest.kt` | Thêm hai test hỏi đánh giá với `LocalInAppReview` giả (mục 4, phiên bản 2); test của T-018 giữ nguyên. T-018 dùng phương án dự phòng và không tạo file này thì plan này tạo nó, chỉ với hai test của mình |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewViewModelTest.kt` | Thêm `savedCount_followsRepository`; test có sẵn giữ nguyên |
| Sửa | `CLAUDE.md` | Thêm dòng `:core:review` vào bảng cấu trúc module |
| Sửa | `docs/features/camera/tickets/T-019-hoi-danh-gia.md` | Trường `plan:` trỏ tới plan này |

`ReviewExit.kt`, `ReviewExitTest.kt` và `ReviewRouteTest.kt` do T-018 tạo; plan này ghi Sửa (L-033).

Nơi đọc các file bị sửa (L-020):

- `settings.gradle.kts`: Gradle đọc; `scripts/check.sh` tự tìm mọi `core/*/` có `build.gradle.kts` và `src/test`; `pr-check.yml` lọc theo `core/**`, đã gồm module mới; `checkModuleDependencies` của `build.gradle.kts` gốc kiểm mọi module con, `:core:review` chỉ phụ thuộc core.
- `FeatureFlag.kt`: `DefaultFeatureFlagsTest.kt` chạy trên `FeatureFlag.entries` (mọi cờ mặc định tắt, khóa đúng dạng `feature_<tên>_enabled`), tự gồm cờ mới và phải qua; `StagingFeatureFlagsModule.kt` (trong danh sách). T-017 cũng thêm cờ `feature_pose_matching_enabled` vào file này.
- `app/build.gradle.kts`: `scripts/release.sh` đọc `applicationId`, `scripts/preflight.sh` đọc dòng `ASSETS_BASE_URL` của bản staging (`scripts/required-assets.txt` và test `preflight: the domain is read from app/build.gradle.kts` của `scripts/tests/run.sh` dựa vào dòng đó); plan chỉ thêm một dòng `implementation(project(":core:review"))`, hai dòng kia không đổi.
- `CLAUDE.md` (gốc repo): `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt`, `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt` (chú thích "CLAUDE.md, step 10"), `scripts/device-session.sh`, `scripts/status.py`, `scripts/required-assets.txt` (chỉ nhắc `docs/CLAUDE.md` trong chú thích hay câu in ra) chỉ dẫn tới mục khác của tài liệu, không đọc nội dung file; plan chỉ thêm một dòng vào bảng cấu trúc module.
- `MainActivity.kt`: `app/src/androidTest/.../AppLaunchTest.kt` dựng app thật với Hilt, sẽ dựng thêm `InAppReview` (không gọi Play khi chưa rời màn xem lại). Không có test JVM nào dựng `MainActivity`.
- `ReviewViewModel.kt`: `ReviewViewModelTest.kt` (trong danh sách) và `ReviewRouteTest.kt` của T-018 dựng lớp bằng tham số tên; không thêm tham số tiêm mới, nên không phải sửa chỗ dựng.
- `ReviewScreen.kt`: `ReviewScreenTest.kt`, `ReviewScreenScreenshotTest.kt` gọi `ReviewScreen` không trạng thái, không đổi; `ReviewRouteTest.kt` (trong danh sách): hai test của T-018 dựng `ReviewRoute` không có `LocalInAppReview`, hành động hỏi đánh giá không làm gì, nên giữ nguyên; hai test mới của plan này cung cấp bản giả.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: `feature_in_app_review_enabled` (`FeatureFlag.IN_APP_REVIEW`), mặc định tắt, đúng spec mục 8. Bật trên staging qua `StagingFeatureFlagsModule` (câu hỏi 2).
- Quảng cáo: Không chạm vị trí nào. Chỉ đọc kết quả `shown` của `rememberShowInterstitial(REVIEW_INTERSTITIAL, onFinished)` do T-018 thêm.
- Sự kiện phân tích: Không chạm. Spec không có sự kiện cho việc hỏi đánh giá.
- File R2: Không chạm.
- Dữ liệu trên máy: khóa DataStore mới `in_app_review_last_asked_at` (thời điểm gọi hộp đánh giá gần nhất, spec mục 6, "ngày gọi hộp đánh giá gần nhất"). Không gửi dữ liệu nào ra khỏi máy; luồng đánh giá do Google Play xử lý.
- Tham số `RemoteSettings` mới: `in_app_review_min_saved_photos` (3), `in_app_review_min_days_between` (30).
- **Quyền truy cập:** Không thêm. Đã so manifest gộp của `:app` (`stagingDebug`) trước và sau khi thêm thư viện (mục 4.1): danh sách `uses-permission` không đổi; thư viện chỉ thêm activity `com.google.android.play.core.common.PlayCoreDialogWrapperActivity` của Google Play.
- **Thư viện và SDK:** Thư viện đã có trong `gradle/libs.versions.toml`: `play-review-ktx` (`com.google.android.play:review-ktx:2.0.2`, giấy phép Play Core Software Development Kit Terms of Service của Google), thêm vào `:core:review`. Kéo theo thư viện review 2.0.2 và core-common 2.0.4 của Google Play (phụ thuộc bắc cầu, không khai báo trong catalog), cùng play-services-basement và play-services-tasks đã có trong app qua Firebase và AdMob. Đây là SDK bên thứ ba mới với app (điểm chặn 2); không sửa `gradle/libs.versions.toml`. Phụ thuộc module mới: `:core:review` vào `:app` và `:feature:camera`; `androidx.activity:activity-compose` (`libs.androidx.activity.compose`, đã có ở `:core:ads`) vào `:core:review`. `FakeReviewManager` nằm trong chính thư viện `review` (gói `com.google.android.play.core.review.testing`), không cần thư viện test mới.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `InAppReviewTest` và `RequestInAppReviewTest` cần một `Activity` thật của Robolectric và luồng chính; chạy thử chỉ gọi `FakeReviewManager` trực tiếp, chưa chạy qua `InAppReview` với `scope` trên `Dispatchers.Main.immediate`.
- **Điều kiện chuyển:** test của `requestIfDue` (qua `scope`) thất bại hay treo vì luồng chính của Robolectric sau 2 lần sửa.
- **Cách làm thay:** `InAppReview` nhận `scope` qua constructor như đã viết; test dùng `ask()` (hàm `suspend`, gọi thẳng trong `runTest`) cho mọi tiêu chí, còn `RequestInAppReviewTest` dựng `InAppReview` với `scope = TestScope(UnconfinedTestDispatcher())` thay cho `Dispatchers.Main.immediate`. Tiêu chí và test ở mục 4 giữ nguyên, chỉ đổi dispatcher của bản dựng trong test. Không thêm file mới.

## 4. Kiểm thử

Test viết trước code. Bước 3 (commit test): mọi test dưới đây, cùng khung rỗng để biên dịch được: `:core:review` (kể cả `consumer-rules.pro` rỗng) với các lớp, hàm ở mục 3.1 có thân `TODO()` hay trả giá trị sai (ví dụ `isReviewDue` trả `false`, `ask` trả `false` và không gọi `prompt`), `settings.gradle.kts`, `build.gradle.kts` của module, `FeatureFlag.IN_APP_REVIEW`, tham số `askReview` không dùng, `savedCount` trả `MutableStateFlow(0)`. Bước 4 (commit code): phần còn lại, cùng `MainActivity.kt`, `StagingFeatureFlagsModule.kt`, `CLAUDE.md`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-10, tiêu chí 1: ít nhất 3 ảnh | Unit test | `ReviewRulesTest.threeSaved_isDue_twoIsNot`: `isReviewDue(3, 3, null, now, 30)` đúng, `isReviewDue(2, 3, null, now, 30)` sai (đặt đúng cạnh sai, L-035) |
| KB-10, tiêu chí 1: cách ít nhất 30 ngày | Unit test | `ReviewRulesTest.thirtyDaysAfterLastAsk_isDue`: `last = now - 30 * 86_400_000` đúng; `last = now - 30 * 86_400_000 + 1` sai |
| KB-10, tiêu chí 1: chưa hỏi lần nào | Unit test | `ReviewRulesTest.neverAsked_isDue`: `lastAskedAtMillis = null`, đủ ảnh thì đúng |
| Giờ máy bị chỉnh lùi | Unit test | `ReviewRulesTest.lastAskInTheFuture_isDue`: `last = now + 1_000` thì đúng |
| KB-10, tiêu chí 1: gọi Play và ghi thời điểm | Unit test (Robolectric) | `InAppReviewTest` dựng `DefaultInAppReview` bằng bản giả. `InAppReviewTest.dueAndFlagOn_asksAndRecordsTime`: cờ bật, 3 ảnh, chưa hỏi, đồng hồ giả `now = 1_000_000`; `ask(activity, 3)` trả `true`, `FakeReviewPrompt.calls == 1`, `history.lastAskedAt() == 1_000_000`, log `Review asked` |
| KB-10, tiêu chí 1: lần sau trong 30 ngày không gọi | Unit test (Robolectric) | `InAppReviewTest.secondAskWithin30Days_isSkipped`: sau lần hỏi trên, tăng đồng hồ 29 ngày, `ask` trả `false`, `calls == 1`; tăng thêm 1 ngày, `ask` trả `true`, `calls == 2` |
| KB-10, tiêu chí 3: cờ tắt | Unit test (Robolectric) | `InAppReviewTest.flagOff_neverAsks`: cờ tắt, 10 ảnh; `ask` trả `false`, `calls == 0`, log `Review skipped: flag_off`. Đối chứng ngay trong test: bật cờ, `ask` trả `true` |
| Tham số từ `RemoteSettings` | Unit test (Robolectric) | `InAppReviewTest.remoteMinSaved_isUsed`: `in_app_review_min_saved_photos = 5`, 3 ảnh thì không hỏi, 5 ảnh thì hỏi; `InAppReviewTest.remoteMinDays_isUsed`: `in_app_review_min_days_between = 7`, lần hỏi thứ hai sau 7 ngày được gọi |
| Lỗi của Play không làm dừng app, không ghi thời điểm | Unit test (Robolectric) | `InAppReviewTest.promptFails_isLoggedAndNotRecorded`: `FakeReviewPrompt` ném `RuntimeException`; `ask` trả `false`, không ném ra ngoài, `history.lastAskedAt() == null`, log `warn` `Review failed` |
| Mặc định là quyết định kinh doanh | Unit test | `ReviewRulesTest.defaults`: `ReviewSettings` mặc định 3 và 30, khóa đúng tên ở mục 3.3 |
| Gọi đúng API của Play | Unit test (Robolectric) | `PlayReviewPromptTest.fakeManager_completes`: `PlayReviewPrompt(FakeReviewManager(context)).show(activity)` chạy xong không lỗi (đúng các hàm đã chạy thử ở mục 4.1) |
| Lưu thời điểm hỏi | Unit test | `DataStoreReviewHistoryTest.recordThenRead`: bản mới trả `null`; `recordAsked(42)` rồi `lastAskedAt() == 42` (ghi rồi đọc lại, L-035); `DataStoreReviewHistoryTest.readError_isNull`: `InMemoryDataStore.readFailures = 1` thì `lastAskedAt() == null`, không ném lỗi; `DataStoreReviewHistoryTest.writeError_isSwallowed`: `failWrites = true` thì `recordAsked` không ném lỗi |
| Không có `LocalInAppReview` (test, preview) | Compose UI test (Robolectric) | `RequestInAppReviewTest.withoutProvider_doesNothing`: gọi hành động không lỗi. `RequestInAppReviewTest.withProvider_asksThroughInAppReview`: cung cấp `DefaultInAppReview` dựng bằng bản giả (cờ bật, chưa hỏi), gọi hành động với 3, `waitForIdle`; `FakeReviewPrompt.calls == 1`. Đối chứng: khung rỗng không gọi `prompt`, test thứ hai thất bại |
| KB-10, tiêu chí 2: quảng cáo vừa hiện thì không hỏi | Unit test | `ReviewExitTest.adShown_doesNotAskReview`: `afterReviewAd(true, goBack, askReview)` gọi `goBack` một lần, `askReview` không lần nào |
| KB-10, tiêu chí 1 và 2: quảng cáo không hiện thì hỏi, sau khi về camera | Unit test | `ReviewExitTest.adNotShown_goesBackThenAsksReview`: `afterReviewAd(false, ...)`; danh sách thứ tự `["goBack", "askReview"]` |
| KB-10, tiêu chí 1: rời màn không lưu ảnh thì không hỏi (câu hỏi 1) | Unit test | `ReviewExitTest.notSaved_goesBackWithoutReview`: `leaveReview(false, showAd, goBack, askReview)`; thứ tự `["goBack"]`, `showAd` và `askReview` không gọi. Khung rỗng (tham số `askReview` không dùng) cũng cho kết quả này, nên cùng test có bước đối chứng (L-035): `leaveReview(true, ...)` rồi `afterReviewAd(false, goBack, askReview)` thì thứ tự là `["showAd", "goBack", "askReview"]`; trước bước 4 phần đối chứng thất bại vì thiếu `askReview` |
| Đã lưu: chưa hỏi đánh giá trước khi có kết quả quảng cáo | Unit test | `ReviewExitTest.saved_asksAdBeforeReview`: `leaveReview(true, ...)` chỉ gọi `showAd`, chưa gọi `askReview` |
| Số ảnh đã lưu cho việc hỏi | Unit test | `ReviewViewModelTest.savedCount_followsRepository`: `FakeSavedPhotosRepository.count = 2`, `savedCount.value == 2`; lưu ảnh xong `savedCount.value == 3` |
| KB-10, nối trong `ReviewRoute`: lưu rồi rời màn thì hỏi đánh giá, sau khi đã về camera (phiên bản 2) | Compose UI test (Robolectric) | `ReviewRouteTest.savedThenRetake_asksReviewOnceAfterLeaving`: `FakeInAppReview` (cài đặt `InAppReview`, ghi `requests: List<Int>` cùng số lần `onLeave` đã chạy lúc được gọi); `FakeSavedPhotosRepository.count = 3`; `ReviewViewModel` ở trạng thái `SAVED` như test của T-018; `setContent { CompositionLocalProvider(LocalInAppReview provides fake) { ReviewRoute(onLeave = { leaves++ }, viewModel = vm) } }`, không có `LocalAdsController` (quảng cáo báo không hiện); bấm "Chụp lại"; `waitForIdle`; `leaves == 1`, `requests == [3]`, và lúc được gọi `leaves` đã là 1. Trước bước 4, `ReviewRoute` chưa truyền `askReview` nên `requests` rỗng, test thất bại đúng lý do |
| KB-10, tiêu chí 1: rời màn không lưu ảnh thì không hỏi, ở `ReviewRoute` (phiên bản 2) | Compose UI test (Robolectric) | `ReviewRouteTest.discardThenLeave_doesNotAskReview`: cùng cách dựng, trạng thái `NOT_SAVED`, `count = 3`; bấm "Chụp lại", "Bỏ ảnh"; `leaves == 1`, `requests` rỗng. Khung rỗng cũng cho kết quả này, nên đối chứng là test ngay trên (cùng `FakeInAppReview`, cùng số ảnh, chỉ khác đã lưu): hai test chỉ cùng qua khi `ReviewRoute` hỏi đúng theo `savedThisVisit` (L-035) |
| Hộp đánh giá thật | Kiểm tra thủ công | Trên bản staging (cờ bật sẵn): lưu 3 ảnh, rời màn xem lại; logcat có `VGL.InAppReview` `Review asked` (hoặc `Review failed` nếu máy không có Google Play), lần rời sau có `Review skipped: not_due`. Hộp đánh giá thật chỉ hiện với bản cài từ Google Play: dev thử qua Internal app sharing hoặc kênh thử nghiệm nội bộ, theo hướng dẫn kiểm thử của Google (hộp hiện ở lần đầu, các lần sau Play có thể không hiện) |

Màn hình thấp, đúng thiết kế, chữ đúng bảng chữ: không áp dụng, task không đổi giao diện của app (không có ảnh chuẩn mới; ảnh có sẵn không đổi).

**Ngoại lệ viết test trước:** `MainActivity.kt` (một dòng `LocalInAppReview provides inAppReview` và một trường tiêm), `StagingFeatureFlagsModule.kt` (một hàm `@Provides`), `di/ReviewModule.kt` (Hilt) và `CLAUDE.md` không có unit test: Hilt kiểm đồ thị lúc biên dịch, `AppLaunchTest` của CI dựng app thật; phần hành vi đứng sau (`DefaultInAppReview`, `rememberRequestInAppReview`) và phần nối trong `ReviewRoute` (phiên bản 2, `ReviewRouteTest`) có test. Người duyệt cần đồng ý ngoại lệ này.

### 4.1. Chạy thử

- **Đã chạy thử:** Play In-App Review lần đầu dùng trong dự án. Ngày 2026-10-23, trên nhánh chính tại `fa65d75`, dựng tạm module `:core:review` (không commit) gồm `build.gradle.kts` như mục 3.1, một hàm gọi `ReviewManagerFactory.create(context)`, `manager.requestReview()` và `manager.launchReview(activity, info)`, và test Robolectric `SpikeReviewTest` gọi hàm đó với `FakeReviewManager` và `Robolectric.buildActivity(Activity::class.java)`; thêm `:core:review` vào `settings.gradle.kts` và `app/build.gradle.kts`.
- **Lệnh:** `scripts/check.sh`; `./gradlew :app:processStagingDebugMainManifest` trước và sau khi thêm module, so các dòng `uses-permission` của `app/build/intermediates/merged_manifest/stagingDebug/processStagingDebugMainManifest/AndroidManifest.xml`; `./gradlew :core:review:dependencies --configuration debugRuntimeClasspath`.
- **Kết quả:**
  - `scripts/check.sh` qua ("Cổng chất lượng: QUA", 15 phút 44 giây; 749 test JVM, 0 lỗi; `:core:review` 1 test qua; lint của module mới qua). `ReviewManagerFactory`, `requestReview`, `launchReview` và `FakeReviewManager` biên dịch được với `libs.play.review.ktx` ở `implementation` của module, `FakeReviewManager` chạy xong trên Robolectric với `Activity` dựng bằng `Robolectric.buildActivity`.
  - Phiên bản 2, `consumer-rules.pro` (2026-10-23, nhánh chính tại `14d86b1`): dựng tạm `:core:review` chỉ có `build.gradle.kts` (plugin `template.android.library`) và một hàm, không có `consumer-rules.pro`, thêm vào `settings.gradle.kts` và `app/build.gradle.kts`. `./gradlew :core:review:assembleRelease :core:review:exportReleaseConsumerProguardFiles` và `./gradlew :app:minifyStagingReleaseWithR8` đều qua (mã thoát 0): AGP bỏ qua file luật khai báo mà không có. Plan vẫn tạo file rỗng để module mới giống mọi module khác và người sau thêm luật đúng chỗ. Module tạm đã xóa.
  - Lỗi đã gặp: lần đầu thiếu `src/test/resources/robolectric.properties` thì Robolectric báo `Package targetSdkVersion=36 > maxSdkVersion=35`; thêm `sdk=35` như các module khác thì qua. File này có trong mục 3.2.
  - Manifest gộp: danh sách `uses-permission` trước và sau khi thêm module giống hệt nhau; thêm đúng một activity `com.google.android.play.core.common.PlayCoreDialogWrapperActivity`.
  - Phụ thuộc bắc cầu: review 2.0.2, core-common 2.0.4 của Google Play; play-services-basement, play-services-tasks đã có.
  - Phương án dự phòng ở mục 3.4 chưa cần thử: điểm chưa chắc là cách test qua `scope` trên luồng chính, không phải thư viện.
  - Module tạm đã xóa, không commit.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 2 (SDK bên thứ ba mới với app: Play In-App Review), 7 (module core mới `:core:review`, sửa `:core:config`, cấu hình build `settings.gradle.kts`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Module mới, `:core:config`, `:feature:camera`, `:app` |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, đủ tiêu chí; AI tự quyết vài chi tiết nhỏ (giờ máy chỉnh lùi, lỗi của Play không ghi thời điểm), nêu ở mục 8 |
| C. Khả năng kiểm chứng tự động | 2 | Luật thời gian và số ảnh có test với đồng hồ giả; hộp đánh giá thật chỉ hiện với bản cài từ Google Play, phải thử bằng tay |
| D. Ảnh hưởng chéo | 2 | Sửa `ReviewExit.kt`, `ReviewScreen.kt` của T-018; `FeatureFlag.kt` cũng được T-017 sửa; `MainActivity.kt` là vỏ app |
| E. Độ mới của kỹ thuật | 2 | Thư viện chưa dùng trong dự án nhưng phổ biến, có bản giả chính thức; đã chạy thử |
| **Tổng** | 10 | **Mức: Cao** (đồng thời chạm điểm chặn) |

`token_estimate`: số gợi ý cỡ S là 5.0M, mới đo 2 task. Plan tạo một module core mới với 8 file mã và 6 file test, sửa 4 module khác, nên ghi 15M (phiên bản 1 ghi 14M; phiên bản 2 thêm hai test Robolectric dựng `ReviewRoute`), gần cỡ M (15.4M).

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - Module mới `:core:review`; `settings.gradle.kts`; `FeatureFlag.kt` của `:core:config`; `MainActivity.kt`, `StagingFeatureFlagsModule.kt` của `:app`.
  - Có ích cho mọi app của nhóm (hỏi đánh giá theo luật đọc từ Remote Config): đề xuất về dự án mẫu theo `docs/ops/from-app.md`, ghi trong mô tả PR.
- Task khác trong sprint có thể xung đột:
  - T-018: plan này dựng trên `ReviewExit.kt`, `ReviewExitTest.kt` và dạng `rememberShowInterstitial(placement, onFinished)` của T-018. Phải chạy sau T-018; T-018 dừng thì bỏ qua T-019 (ticket ghi `depends_on: [T-016, T-018]`).
  - T-017: cùng thêm cờ vào `FeatureFlag.kt` và có thể vào `StagingFeatureFlagsModule.kt`; mỗi plan thêm một dòng riêng, chạy lần lượt, task sau gộp nhánh chính trước.
  - T-020 (máy gập): có thể sửa `ReviewScreen.kt` (bố cục); plan này chỉ sửa `ReviewRoute`.
- Đề xuất thứ tự chạy: ngay sau T-018 trong cùng phiên, hoặc phiên sau.

## 7. Điều kiện dừng riêng

- Manifest gộp sau khi thêm `:core:review` có quyền mới mà mục 3.3 không ghi: dừng.
- Hilt không dựng được `InAppReview` (bind vào `DefaultInAppReview`) trong `MainActivity` (lỗi đồ thị lúc biên dịch) mà phải sửa file ngoài danh sách: dừng.

## 8. Câu hỏi cho người duyệt

1. **Rời màn xem lại mà không lưu ảnh có hỏi đánh giá không?** Spec KB-10 bản trước chỉ ghi "đã lưu ít nhất 3 ảnh (tính từ khi cài) và rời màn xem lại". Đề xuất của AI: chỉ hỏi khi lần xem lại đó đã lưu ảnh, vì đó là lúc người dùng vừa hài lòng.
   - **Đã trả lời** (Tuấn, 2026-10-23): đồng ý đề xuất của AI. Spec camera đã sửa (PR #64): KB-10 thêm "sau khi đã lưu ảnh ở lần xem lại đó", spec xác nhận lại 2026-10-23. Plan đã theo: `leaveReview(false, ...)` không hỏi, test `ReviewExitTest.notSaved_goesBackWithoutReview`.
2. **Bật cờ trên staging?** Plan bật `feature_in_app_review_enabled` trên staging để nhóm thấy log `VGL.InAppReview` khi thử. Bản staging không cài từ Google Play nên Play không hiện hộp đánh giá; app vẫn ghi thời điểm như đã hỏi. Đề xuất của AI: bật.
3. **Lỗi của Play có tính là "lần gọi trước" không?** Plan chỉ ghi thời điểm khi luồng đánh giá chạy xong không lỗi; máy không có Google Play thì mỗi lần rời màn (từ ảnh thứ 3) app thử lại và ghi một dòng `warn`. Đề xuất của AI: giữ như vậy, vì thử lại không làm phiền người dùng.
4. **Module core mới `:core:review`** (điểm chặn 7): thay cho việc đặt vào `:core:ads` hay cho feature gọi Play trực tiếp với con số viết cứng (không đúng ticket, vì feature không đọc `RemoteSettings`). Có cần ADR riêng cho module này không? Đề xuất của AI: không cần ADR; module làm theo đúng mẫu `:core:ads` và ADR-0001, ADR-0003; ghi vào bảng module của `CLAUDE.md` và đề xuất về dự án mẫu.
5. **Người phụ trách sản phẩm** xác nhận hai mặc định 3 ảnh và 30 ngày (đúng spec KB-10), và tên hai khóa Remote Config `in_app_review_min_saved_photos`, `in_app_review_min_days_between`.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1: Duyệt (Nhật, duyệt chéo Tuấn, 2026-10-23). Ghi chú: Tuấn trả lời câu hỏi cùng AI, chấp nhận mọi đề xuất của AI ở mục 8; câu 1 chỉ hỏi đánh giá khi lần xem lại đó đã lưu ảnh (spec đã sửa ở PR #64). Thay bằng phiên bản 2 trước khi chạy vì rà soát độc lập trước RUN-20261023-03.

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-23, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Phiên bản 2 sửa các điểm của rà soát độc lập trước RUN-20261023-03.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
