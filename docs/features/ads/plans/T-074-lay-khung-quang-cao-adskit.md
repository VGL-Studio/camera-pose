---
ticket: T-074
title: Lấy khung quảng cáo AdsKit của dự án mẫu về app (chưa bật AppsFlyer), chuyển ba vị trí quảng cáo hiện có sang khung mới, bật quảng cáo mở app và quay lại app
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 54M          # cỡ L, ba lượt: lượt 1 cỡ L (39M), lượt 2 cỡ M (8.6M), lượt 3 cỡ S (6.4M); số gợi ý của scripts/token-usage.py estimate
blocking_points: [ads, sdk, permission, analytics, consent, core, build]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Tuấn               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-074: LẤY KHUNG QUẢNG CÁO ADSKIT CỦA DỰ ÁN MẪU VỀ APP (CHƯA BẬT APPSFLYER), CHUYỂN BA VỊ TRÍ, BẬT QUẢNG CÁO MỞ APP VÀ QUAY LẠI APP

## 1. Tóm tắt

App thay bộ quảng cáo cũ (`core/ads` ở mốc `0eaeadb` của dự án mẫu, ADR-1004) bằng khung AdsKit PARTNER của dự án mẫu ở mốc `0444fbe` (base của `docs/template-sync.txt`), cùng `core/analytics` mới nhưng không có SDK AppsFlyer (một sink rỗng thay chỗ gọi SDK duy nhất). Ba vị trí của app (`review_interstitial`, `poses_banner`, `poses_detail_banner`) chạy qua `AdPlacement` mới, khóa `is_show_<vị trí>`, giữ hai luật đếm ảnh của `review_interstitial` và nhịp chung `ads_interval`; quảng cáo mở app hiện từ màn loading qua `StartupAds` (từ lần mở thứ hai), quảng cáo khi quay lại app hiện ở mọi màn, có đánh dấu các lần rời app có chủ đích. Làm trong ba lượt của cùng một danh sách chạy, mỗi lượt một PR có CI xanh: (1) lấy code và build qua, giữ hành vi hiện có; (2) nối vỏ app, bật `open_app` và `resume`; (3) bảng khóa, ADR, tài liệu.

## 2. Hiểu yêu cầu

Ticket xử lý các câu trả lời đã chốt ở mục 9 của `features/ads/spec.md` (spec ads không có kịch bản riêng, mục 3) và các vị trí ở mục 5 của `features/camera/spec.md`, `features/loading/spec.md`, `features/poses/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Spec ads, câu 1 | `review_interstitial` không hiện trước ảnh lưu thứ 2 của phiên, tối đa 1 lần mỗi 3 ảnh, nhịp chung `ads_interval` (mặc định 45 giây); không còn luật 120 giây riêng | Hai luật đếm là của app, không có trong khung: giữ `InterstitialBreaks` của app (bỏ phần 120 giây), làm điều kiện trước khi gọi `AdsController.showInterstitial` của khung. Nhịp 45 giây là cổng `ads_interval` của khung. Đếm trong bộ nhớ, như hiện nay (camera spec mục 6). |
| Spec ads, câu 2 | Khóa bật tắt `is_show_review_interstitial`, `is_show_poses_banner`, `is_show_poses_detail_banner`; giữ `ads_review_interstitial_first_break`, `ads_review_interstitial_breaks_between`; bỏ `ads_review_interstitial_min_interval_seconds`, `ads_interstitial_min_interval_seconds` | `is_show_<mã>` là khóa suy ra của `AdsSettings.placementEnabled`; hai khóa đếm khai báo bằng `RemoteKey.long` trong một file riêng của app ở `core/ads`, có trong bảng khóa. Người tạo khóa mới trên Firebase là việc ngoài phiên (mục 8, câu 9). |
| Spec ads, câu 3 | Không có SDK, quyền hay dữ liệu gửi tới AppsFlyer | Lấy khung `core/analytics` của mẫu, bỏ `implementation(libs.appsflyer)`, thay `AppsFlyerLibSink` (file duy nhất import SDK, `AppsFlyerSink.kt:4`) bằng `NoOpAppsFlyerSink`; `:app` cấp `AppsFlyerConfig("")`. Hai lớp: không có SDK nên không gọi được, không có khóa nên `AppsFlyerTracker` không khởi động. Làm được (mục 3.1), không phải giữ `core/analytics` cũ. |
| Spec ads, câu 4 | Không có `AdGalleryActivity`, không có `SAMPLE_BOTTOM_BANNER` | Không lấy `app/src/staging/.../adgallery/`, `app/src/staging/AndroidManifest.xml`, `AdGallery*Test`; bỏ dòng `AdSlot(placement = AdPlacement.SAMPLE_BOTTOM_BANNER, ...)` ở `SampleScreen.kt:72`. Các vị trí `gallery_*` và `sample_native_small` của khung vẫn khai báo trong `AdPlacement` vì 35 test của khung dùng chúng; không màn nào gọi, staging không bật (mục 8, câu 1). |
| Spec ads, câu 5 | Một ticket, ba lượt, mỗi lượt một PR, cùng danh sách chạy | Mục 3.1, "Ba lượt". Mỗi lượt merge độc lập, CI xanh, nhánh chính vẫn chạy đúng sau từng lượt. |
| Spec ads, câu 6; loading mục 5 | Quảng cáo mở app (`open_app`) hiện một lần khi loading xong, trước màn chính; không ở lần mở đầu (`show_open_ads_first_open` tắt cả ở staging); cần `feature_loading_enabled` | Cổng tại lượt 2: `LoadingViewModel` gọi `StartupAds.awaitStartupAd()` sau khi xong các việc và hết 2 giây tối thiểu, như T-024 của mẫu. Giới hạn chờ (đồng ý 10 giây, SDK 35 giây, quảng cáo 40 giây) tính từ lúc `:core:ads` khởi động trong `Application.onCreate`, không cộng thêm sau thời gian tải (mục 3.1, "Thời gian của màn loading"; mục 8, câu 6). Loading tắt thì không có quảng cáo mở app. |
| Spec ads, câu 6; camera, poses mục 5 | Quảng cáo khi quay lại app (`resume`) hiện ở mọi màn, kể cả camera; không hiện ngay sau khi rời app có chủ đích (hộp thoại quyền, chia sẻ); đóng quảng cáo thì camera mở lại | Cổng A-11 của khung, không có cổng theo màn, không sửa `core/ads` cho việc này. App đánh dấu các lần rời app có chủ đích (G-03, G-05) ở lượt 2. Camera đóng ở `onStop` và mở lại ở `onStart` như mọi lần ra nền (`CameraViewModel.onScreenStopped`, `onScreenStarted`). |
| Spec ads, câu 6, staging | `StagingRemoteValuesModule` của app bật `show_open_ads`, `show_resum_ads`; không bật `show_open_ads_first_open`, `is_show_native_after_inter`, `is_show_native_bottom`; giữ `is_log_ad_fullscreen_events`, `is_log_ad_slot_events`; có test | Module của app, không chép bản mẫu. Bật thêm `is_show_` của ba vị trí của app để staging có quảng cáo thử mà không phụ thuộc Firebase staging (mục 8, câu 2). Firebase staging thắng giá trị cục bộ (`DefaultRemoteSettings`, mục 3.1, "Firebase staging"). |
| Poses KB-6 | Cờ tắt hoặc banner không tải được thì không chừa khoảng trống; banner không che nội dung | Khung giữ chiều cao khung banner sau lỗi tải để thử lại (B-02); app cần thu về 0 dp như hiện nay (`AdSlotTest.failedLoad_collapsesBannerToZero_andHidesSeparator`). Thêm tham số `separator` vào `AdSlot` của khung và thu khung khi lần tải gần nhất lỗi (mục 3.1, "Banner của app"). |
| Camera KB-9 | Rời màn xem lại sau khi lưu ảnh và đủ điều kiện thì có quảng cáo toàn màn hình trước khi về camera; chưa lưu thì không; cờ tắt hay chưa đồng ý thì không | Giữ `leaveReview`, `afterReviewAd` và chữ ký `rememberShowInterstitial(placement) { shown -> }` mà `ReviewScreen.kt:79` gọi. Tiêu chí 4 ("ở camera ... không có quảng cáo nào") trái câu 6: mục 8, câu 3. |
| DoD của ticket | Manifest gộp chỉ có quyền spec cho phép; R8 staging release build qua; `docs/remote-config-keys.md` có; `scripts/ads-plan.py` chạy được; ADR-1004 `superseded`; `docs/template-sync.txt` không còn `hold=` cho đường dẫn đã lấy | Mục 4. Quyền mới duy nhất là `VIBRATE` (AppLovin), chưa có trong spec: mục 8, câu 4. |

Thiết kế tham chiếu: Không có thiết kế riêng (`features/ads/spec.md`, mục 4; docs/ops/design-workflow.md). Banner ở thư viện và chi tiết giữ thiết kế của `features/poses/design/README.md`, mục 5 (ảnh `PosesScreen_ad_*`, `PoseDetailScreen_ad_*` không có test chụp quảng cáo: không có `LocalAdsController` trong test).

ADR liên quan: ADR-0001, ADR-0003, ADR-0004, ADR-0013, ADR-0014, ADR-0019, ADR-0023, ADR-0024, ADR-0026, ADR-0027 (không áp dụng phần AppsFlyer), ADR-0029, ADR-0030, ADR-0031, ADR-1001 (15 giây), ADR-1004 (chuyển `superseded`).

## 3. Giải pháp

### 3.1. Cách làm

**Nguồn.** Code của dự án mẫu ở `0444fbe` (base hiện tại của app, `docs/template-sync.txt`). Từ `0444fbe` tới `origin/main` (`5dc79f2`) dự án mẫu không đổi `app/`, `core/`, `feature/`, `gradle/`, `settings.gradle.kts`, `build-logic/` (đã kiểm `git diff --stat 0444fbe origin/main`), nên đợt `--take` lên đúng base là đủ. Đổi tên gói `com.vglstudio.template` thành `com.vglstudio.camera.pose.line.guide` do `scripts/template-sync.sh` làm (cả thư mục nguồn và chữ trong file).

**Điểm bắt đầu: nhánh `template-sync/take-...` do người làm trước phiên** (`docs/ops/to-app.md`, mục 5). `template-sync.sh` chạy trong repo dự án mẫu và cần đọc cả hai repo; phiên tự chạy của app không làm được (`cross-repo-read`, `docs/ops/session-capabilities.md`). Tuấn (hay AI có Tuấn theo dõi) làm, trên máy có cả hai repo:

1. Trong app, nhánh `template-sync/prep-T-074` từ nhánh chính: thay dòng `hold=core 0eaeadb...` của `docs/template-sync.txt` bằng tám dòng `hold=core/ads`, `hold=core/analytics`, `hold=core/assets`, `hold=core/config`, `hold=core/data`, `hold=core/designsystem`, `hold=core/logging`, `hold=core/network`, cùng commit `0eaeadb...`. `--take` chỉ nhận đúng một dòng `hold=<đường dẫn>` (`template-sync.sh`, đoạn `held_at`), nên không lấy riêng `core/ads` được khi chỉ có `hold=core`. Đợt đồng bộ thường sau đó vẫn bỏ qua cả `core/` như trước (mỗi dòng `hold` bị loại khỏi bản xuất). `core/poses`, `core/review` chỉ có ở app nên không cần dòng. Commit "Tách hold=core theo module cho T-074".
2. Trong dự án mẫu: `scripts/template-sync.sh --app ../camera-pose --take core/ads`. Script tạo nhánh `template-sync/take-core-ads-0444fbe` từ nhánh đang mở của app, bỏ dòng `hold=core/ads`. Xung đột (dự kiến ở `AdPlacement.kt`, `AdSlot.kt`, `AdsController.kt`, `AdsSettings.kt`, `AdUnitIdsTest.kt`, `AdsSettingsTest.kt`, là các file app đã sửa so với `0eaeadb`): lấy bản mẫu cho mọi file (`git checkout --theirs -- <file>`), không sửa tay gì thêm; phần riêng của app làm ở lượt 1, theo plan. Các file chỉ có ở app (`InterstitialBreaks.kt`, `InterstitialFlow.kt`, test `AdSlotTest.kt`, `InterstitialBreaksTest.kt`, `InterstitialFlowTest.kt`, `PrivacyOptionsButtonTest.kt`, `ShowInterstitialTest.kt`) git không đụng tới; để nguyên. Chạy `python3 scripts/foundation.py`, commit bằng lời commit script đã soạn.
3. Ngay trên nhánh đó: `scripts/template-sync.sh --app ../camera-pose --take core/analytics` (app chưa sửa `core/analytics` so với `0eaeadb`, dự kiến không xung đột). Script tạo `template-sync/take-core-analytics-0444fbe` từ nhánh của bước 2.
4. Đẩy nhánh `template-sync/take-core-analytics-0444fbe` (chứa cả ba commit), ghi tên nhánh và commit cuối vào danh sách chạy. Không mở PR, không cần build qua: nhánh này chưa biên dịch được (code app còn gọi API cũ), lượt 1 làm cho nó qua.

Phần còn lại phiên làm như task thường. Phần chép tay từ mẫu (core/config, `InternetConnectivity`, loading, các file `app/`) phiên đọc từ repo mẫu bằng `git -C ../app-project-template show 0444fbe:<đường dẫn>`, nên phiên mở bằng `local` (mục 6). Các đoạn chép tay ngắn dán ở dưới; đoạn dài ghi đường dẫn nguồn ở commit `0444fbe`.

**Ba lượt.** Danh sách chạy có ba dòng cùng ticket và plan này, ghi chú `lượt 1`, `lượt 2`, `lượt 3`, lượt sau phụ thuộc lượt trước. Mỗi lượt đi đủ 10 bước của `docs/auto-run.md`, mục 3, với nhánh và PR riêng.

- **Lượt 1, nhánh `claude/T-074-1-lay-khung`**, tạo từ `origin/template-sync/take-core-analytics-0444fbe` rồi `git merge origin/main` (xung đột ngoài `docs/template-sync.txt` và `docs/foundation.md` thì dừng, mục 7). Mục tiêu: build và mọi test qua, hành vi của app như hiện nay với khung mới. Không bật `open_app`, `resume` (khóa mặc định tắt, chưa có giá trị cục bộ).
- **Lượt 2, nhánh `claude/T-074-2-noi-vo-app`**, từ nhánh chính sau lượt 1. Mục tiêu: `StartupAds` ở loading, `setFullScreenAdShowing` của popup mạng, đánh dấu rời app có chủ đích, bật `show_open_ads`, `show_resum_ads` ở staging.
- **Lượt 3, nhánh `claude/T-074-3-tai-lieu`**, từ nhánh chính sau lượt 2. Mục tiêu: `AppRemoteKeys`, `RemoteConfigKeysTest`, bảng khóa sinh ra, ADR, `docs/template-sync.txt`, tài liệu. Chữ của spec đổi trên nhánh báo cáo của phiên để Nhật xác nhận lại (mục 8, câu 3).

#### Lượt 1: lấy code, build qua, giữ hành vi

**`core/config`, gộp tay T-007, T-008 của mẫu** (không `--take`, vì app có `AppLinks`, `PoseMatchingSettings`, 7 cờ thêm và `REFRESH_TIMEOUT_MILLIS = 15_000` theo ADR-1001, mẫu là `10_000`):

- Tạo `RemoteKey.kt`, `LocalRemoteValues.kt`: nguyên bản `core/config/src/main/kotlin/com/vglstudio/template/core/config/RemoteKey.kt` và `LocalRemoteValues.kt` ở `0444fbe` (đã đổi gói).
- `RemoteSettings.kt`: nguyên bản mẫu, interface chỉ còn `fun <T : Any> value(key: RemoteKey<T>): Flow<T>`; `DefaultRemoteSettings` nhận thêm `@LocalRemoteValues localValues: Set<@JvmSuppressWildcards LocalRemoteValue>`; thứ tự: giá trị Firebase, rồi giá trị cục bộ, rồi `RemoteKey.default`.
- `RemoteFlagSource.kt`: thêm `fun double(key: String): Flow<Double?>`. `FirebaseRemoteFlagSource.kt`: thêm `double`, tách `activeValueOrNull` như mẫu; giữ `string` (app đã có).
- `FeatureFlag.kt`: thêm `description` và `val remoteKey: RemoteKey<Boolean> = RemoteKey.boolean(key, defaultValue, description)` cho cả 9 cờ. Mô tả của 7 cờ app (một câu tiếng Anh, tối đa 256 ký tự): `NETWORK_DIALOG` "Shows the dialog that blocks the app while the device has no usable Internet.", `CAMERA` "Shows the camera screen with pose outlines as the start screen.", `ONBOARDING` "Shows the welcome screens and the camera permission request on first open.", `POSES` "Shows the pose library button on the camera and the library screens.", `SETTINGS` "Shows the settings button and the settings screen.", `IN_APP_REVIEW` "Asks for a Play review after enough saved photos (core/review rules).", `POSE_MATCHING` "Shows the live pose match score on the camera." `SAMPLE`, `LOADING` giữ mô tả của mẫu.
- `DefaultFeatureFlags.kt`: đọc `flag.remoteKey.name`, mặc định `flag.remoteKey.default` (như mẫu).
- `di/ConfigModule.kt`: thêm `@Multibinds @LocalRemoteValues abstract fun localRemoteValues(): Set<LocalRemoteValue>`; giữ hai `@Binds` của `AppLinks`, `PoseMatchingSettings`.
- `RemoteConfigRefresher.kt`: không đổi (giữ 15 giây).
- Sáu chỗ gọi API cũ đổi sang `value(RemoteKey)`, mỗi khóa khai báo một lần ở module sở hữu, tên, kiểu và mặc định giữ nguyên:
  - `core/config/.../AppLinks.kt`: `PRIVACY_POLICY_URL = RemoteKey.string("privacy_policy_url", "https://studio.vgl.io.vn/privacy-policy", "Address of the privacy policy page opened from settings.")`; giữ hằng chuỗi cũ cho test có sẵn.
  - `core/config/.../PoseMatchingSettings.kt`: `THRESHOLD = RemoteKey.long("pose_match_threshold", 80, ...)`, `MIN_IN_FRAME = RemoteKey.long("pose_match_min_in_frame_percent", 50, ...)`; giữ phần kẹp giá trị (`MAX_THRESHOLD`, `MAX_MIN_IN_FRAME`).
  - `core/poses/.../internal/PosesSettings.kt`: giữ nguyên hai dòng `internal const val CATALOG_PATH_KEY = "poses_catalog_path"` và `internal const val DEFAULT_CATALOG_PATH = "..."` (`scripts/setup-check-app.sh`, `scripts/tests/app.sh` đọc bằng `sed`), thêm `internal val CATALOG_PATH = RemoteKey.string(CATALOG_PATH_KEY, DEFAULT_CATALOG_PATH, "Path on R2 of the pose catalog; an invalid path falls back to the default.")`; `DefaultPoseCatalogRepository.kt:127` đọc `settings.value(CATALOG_PATH).first()`.
  - `core/review/.../ReviewSettings.kt`: `MIN_SAVED_PHOTOS_KEY = RemoteKey.long("in_app_review_min_saved_photos", 3, ...)`, `MIN_DAYS_BETWEEN_KEY = RemoteKey.long("in_app_review_min_days_between", 30, ...)`, `fun all()`; `InAppReview.kt:47,51` đọc qua `value`.
  - `core/network/.../NetworkSettings.kt`: `LOST_DELAY = RemoteKey.long("network_lost_delay_seconds", <mặc định hiện có>, ...)`, `fun all()`; `NetworkDialogController.kt:123` đọc qua `value`.
  - `core/ads`: thay hẳn bằng code của khung.
- Bản giả của test: `FakeRemoteSettings` (`core/poses`), `Fakes.kt` (`core/network`, `core/review`) đổi sang `value(key)` trả `MutableStateFlow` theo `key.name`, mặc định `key.default`. Đây là cập nhật dữ liệu của test theo API mới (ADR-0029), câu khẳng định không đổi.

**`core/network`, chỉ hai phần** (không lấy `core/network` của mẫu; `LostAfterDelay`, tự đóng popup, `network_lost_shown`, `ScreenName` để lại):

- Tạo `InternetConnectivity.kt`: nguyên bản mẫu (interface `connected: Flow<Boolean>` và `DefaultInternetConnectivity @Inject constructor(source: ConnectivitySource)`, `connected = source.connectivity()`). `ConnectivitySource` của app đã có `connectivity()` (dùng ở `NetworkDialogController.kt`).
- `di/NetworkModule.kt`: thêm `@Binds abstract fun internetConnectivity(connectivity: DefaultInternetConnectivity): InternetConnectivity` vào `NetworkBindingsModule`.
- Ghi chú: `ConnectivitySource` của app hiện coi "có mạng" theo `INTERNET`, chưa theo `VALIDATED`; T-076 đổi điều này và đổi luôn điều `core/ads` thấy. T-074 không phụ thuộc T-076.
- `setFullScreenAdShowing` làm ở lượt 2.

**`core/ads`, sau `--take`.** Bản mẫu giữ nguyên, trừ các chỗ riêng của app sau (mỗi chỗ là thay đổi của module dùng chung: ghi trong mô tả PR và đề xuất về dự án mẫu theo `docs/ops/from-app.md`, mục 2, trừ dòng vị trí):

- `AdPlacement.kt`: giữ mọi dòng của mẫu (`SAMPLE_NATIVE_SMALL`, năm vị trí toàn app, mười ba vị trí `GALLERY_*`), thêm ba dòng của app, mỗi dòng một dòng như `release.sh` đọc, có chú thích KDoc nêu spec:
  ```kotlin
  /** Bottom of the pose library (spec poses, section 5). */
  POSES_BANNER("poses_banner", AdFormat.BANNER),
  /** Bottom of the pose detail, below the main button (spec poses, section 5). */
  POSES_DETAIL_BANNER("poses_detail_banner", AdFormat.BANNER),
  /** Leaving the review screen after a saved photo (spec camera, section 5; breaks: InterstitialBreaks). */
  REVIEW_INTERSTITIAL("review_interstitial", AdFormat.INTERSTITIAL),
  ```
  Không có `SAMPLE_BOTTOM_BANNER`.
- `AdSlot.kt`, banner của app (poses KB-6): `fun AdSlot(placement: AdPlacement, modifier: Modifier = Modifier, separator: (@Composable () -> Unit)? = null)`. Khi `separator` khác `null`: vẽ `separator` phía trên khung chỉ khi khung đang có quảng cáo; khi lần tải gần nhất lỗi (`AdListener.onAdFailedToLoad`) khung thu về 0 dp, không còn dải trống; lần làm mới sau tải được thì khung và đường kẻ hiện lại. Cách nối: `BannerSlotHost` báo kết quả mỗi lần tải lên `BannerSlot` (thêm tham số `onLoadResult: (loaded: Boolean) -> Unit = {}` truyền tới chỗ `SdkBannerAdView` nhận `onAdLoaded`, `onAdFailedToLoad`); `BannerSlot` giữ `failed` trong `remember` và đặt chiều cao 0 khi `failed`. Không có `separator` thì hành vi như mẫu (giữ khung để thử lại, B-02). Lịch thử lại của khung (B-02) không đổi: khung 0 dp vẫn ở trong cây, nên lần thử lại vẫn chạy.
- `AdSlot.kt`, nút quyền riêng tư dạng hàng của app (T-025 của app, `SettingsScreen.kt:107`): thêm lại overload `fun PrivacyOptionsButton(modifier: Modifier = Modifier, content: @Composable (onClick: () -> Unit) -> Unit)`, nguyên văn bản hiện có của app (`core/ads/.../AdSlot.kt`, dòng 168 tới 186 trên nhánh chính), và cho `PrivacyOptionsButton(modifier)` của mẫu gọi qua nó.
- Tạo `BreakInterstitial.kt` (chỉ có ở app): hai luật đếm của `review_interstitial` (spec ads, câu 1) trước cổng của khung.
  ```kotlin
  /** The break gate of :app, provided by MainActivity; null in tests and previews (no limit then). */
  val LocalInterstitialBreakGate = staticCompositionLocalOf<InterstitialBreakGate?> { null }

  /**
   * Shows the interstitial of [placement] at a natural break if its break limits (InterstitialBreaks) and then the
   * full-screen gate of :core:ads allow it. [onFinished] runs once per call, on the main thread: with false at once
   * when skipped (limits, gate, no ad, no controller), with true after the ad has closed.
   */
  @Composable
  fun rememberShowInterstitial(placement: AdPlacement, onFinished: (shown: Boolean) -> Unit): () -> Unit
  ```
  Cài đặt: lấy `LocalAdsController.current`, `LocalActivity.current`, `LocalInterstitialBreakGate.current`; không có controller hay activity thì `onFinished(false)` ngay (như `ShowInterstitialTest.withoutAdsController_reportsNotShownOnce`). Có thì `scope.launch(Dispatchers.Main.immediate)`: `val ticket = gate?.onBreak(placement)` (đếm một lần dừng, đọc hai khóa, trả `null` khi luật không cho); `gate != null && ticket == null` thì `onFinished(false)`; không thì `val closed = CompletableDeferred<Unit>()`, `val shown = ads.showInterstitial(activity, placement) { closed.complete(Unit) }`, `if (shown) { gate?.onShown(ticket); closed.await() }`, rồi `onFinished(shown)`. `require(placement.format == AdFormat.INTERSTITIAL && !placement.appWide)`.
- `InterstitialBreaks.kt` (đã có ở app): `BreakCap(firstBreak: Int, breaksBetween: Int)` bỏ `minIntervalSeconds`; `InterstitialBreaks.allows` bỏ điều kiện thời gian; bỏ `interstitialAllowed` (cổng đồng ý, khóa và popup mạng nay là của khung). Thêm:
  ```kotlin
  /** Remote keys of the break limits (spec ads, questions 1 and 2): names set by the app, not AdsKit keys. */
  object InterstitialBreakSettings {
      val REVIEW_FIRST_BREAK = RemoteKey.long("ads_review_interstitial_first_break", 2,
          "First saved photo of the session (counted in items) that may show review_interstitial. " +
              "Name set by the app, not an AdsKit key.")
      val REVIEW_BREAKS_BETWEEN = RemoteKey.long("ads_review_interstitial_breaks_between", 3,
          "Saved photos (items) between two review_interstitial ads. Name set by the app, not an AdsKit key.")
      fun all(): List<RemoteKey<*>> = listOf(REVIEW_FIRST_BREAK, REVIEW_BREAKS_BETWEEN)
      fun capOf(placement: AdPlacement): Pair<RemoteKey<Long>, RemoteKey<Long>>? = when (placement) {
          AdPlacement.REVIEW_INTERSTITIAL -> REVIEW_FIRST_BREAK to REVIEW_BREAKS_BETWEEN
          else -> null
      }
  }
  /** One counted break that may show an ad. */
  class BreakTicket internal constructor(val placement: AdPlacement, val breakNumber: Int)
  interface InterstitialBreakGate {
      suspend fun onBreak(placement: AdPlacement): BreakTicket?
      fun onShown(ticket: BreakTicket)
  }
  @Singleton
  internal class DefaultInterstitialBreakGate @Inject constructor(private val settings: RemoteSettings) :
      InterstitialBreakGate
  ```
  `onBreak` luôn đếm (như `runInterstitial` cũ: "Every call is a break, counted even when no ad follows"); vị trí không có trong `capOf` thì không giới hạn (`BreakCap.NONE`). Main thread only, như hiện nay.
- Tạo `di/InterstitialBreakModule.kt`: `@Binds` `DefaultInterstitialBreakGate` vào `InterstitialBreakGate`.
- Xóa `InterstitialFlow.kt` (thứ tự bước nay là của khung: `decideFullScreenTurn`), các test riêng của app ở mục 3.5.
- Ảnh chuẩn: 41 ảnh trong `core/ads/src/test/screenshots/` của mẫu được chụp với theme của mẫu (Inter, màu của mẫu); app có `AppTheme` riêng (Bricolage Grotesque, `AppColors`), nên ghi lại cả 41 ảnh bằng `./gradlew :core:ads:recordRoborazziDebug --rerun`, rồi xem từng ảnh (chỉ khác chữ và màu theo theme của app, bố cục giữ nguyên) trước khi commit.

**`core/analytics`, không có AppsFlyer:**

- `build.gradle.kts`: bỏ dòng `implementation(libs.appsflyer)`.
- `AppsFlyerSink.kt`: giữ interface `AppsFlyerSink`, thay `AppsFlyerLibSink` (dòng import `com.appsflyer.AppsFlyerLib` là chỗ duy nhất của repo dùng SDK) bằng:
  ```kotlin
  /**
   * AppsFlyer is not in this app (spec ads, question 3): no SDK, no permission, nothing sent. Every call does
   * nothing. Turning AppsFlyer on needs its own spec and ticket, which bring back the SDK sink of the template.
   */
  internal class NoOpAppsFlyerSink @Inject constructor() : AppsFlyerSink {
      override fun init(devKey: String) = Unit
      override fun start() = Unit
      override fun logEvent(name: String, params: Map<String, Any>?) = Unit
  }
  ```
- `di/AnalyticsModule.kt`: `bindAppsFlyerSink(impl: NoOpAppsFlyerSink)`; phần còn lại như mẫu (`FanOutAdMetrics` vẫn gửi tới `AppsFlyerTracker`, tracker không khởi động vì không có khóa và sink không làm gì).
- `src/main/AndroidManifest.xml` của mẫu (ba dòng `tools:node="remove"` cho quyền của Samsung, Huawei và gói `mapsagent`) giữ nguyên: không có SDK thì các dòng này không gỡ gì, nhưng chặn được nếu một SDK khác kéo các quyền đó vào.
- Xóa `AppsFlyerSdkUseTest.kt` (khẳng định đúng một file import SDK và `enableTCFDataCollection` trước `init`, trái câu 3 của spec); thay bằng `NoAppsFlyerSdkTest` (mục 4).

**`gradle/libs.versions.toml`, `settings.gradle.kts`** (plan rủi ro cao này, duyệt chéo, là plan cho phép sửa `gradle/libs.versions.toml`, `CLAUDE.md`, mục Không được làm): chép tay phần thêm của mẫu ở `0eaeadb..0444fbe`, **trừ** `appsflyer = "6.18.1"` và `appsflyer = { group = "com.appsflyer", ... }`: chín phiên bản `admobMediation*`, `unityAds`, mười thư viện `admob-mediation-*`, `unity-ads`. `settings.gradle.kts`: hai kho Maven của Pangle (`https://artifact.bytedance.com/repository/pangle/`, chỉ nhóm `com.pangle.global`) và Mintegral (`https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea`, nhóm `com\.mbridge.*`), như mẫu. `build-logic/` không đổi (mẫu không đổi `build-logic` từ `0eaeadb`).

**`:app`, lượt 1:**

- `TemplateApplication.kt`: như mẫu, `appsFlyer.prepare()` (không làm gì khi không có khóa) và `ads.start(this, AppActivityFilter.inPackageOf(MainActivity::class.java))`.
- `MainActivity.kt`: thay `CompositionLocalProvider(LocalAdsController provides ads, LocalInAppReview provides inAppReview)` bằng `AdsHost(ads) { CompositionLocalProvider(LocalInAppReview provides inAppReview, LocalInterstitialBreakGate provides interstitialBreakGate) { ... } }` (inject `InterstitialBreakGate`); giữ `ads.gatherConsent(this)`; bỏ `onUserInteraction()` (khung không có, việc chờ người dùng ngừng chạm của `FullScreenAdGuard` cũ không còn: mục 3.5). Thứ tự còn lại (theme, ngôn ngữ, popup mạng, `AppNavHost`) giữ nguyên. Không thêm `FullScreenAdHost()` (native toàn màn hình, không có vị trí nào của app; `NATIVE_AFTER_INTER` tắt): mục 8, câu 7.
- Tạo `di/AnalyticsConfigModule.kt`: `fun appsFlyerConfig(): AppsFlyerConfig = AppsFlyerConfig("")`, chú thích nêu câu 3 của spec. Không thêm `APPSFLYER_DEV_KEY` vào `app/build.gradle.kts`, không thêm `appsFlyerDevKey` vào danh sách tham số bắt buộc của bản production.
- `ProductionAdsModule.kt`: thêm `fun consentShortcutConfig(): ConsentShortcutConfig = ConsentShortcutConfig.OFF`. `StagingAdsModule.kt`: thêm `ConsentShortcutConfig(enabledInBuild = true)` như mẫu (khóa `is_enable_consent_shortcut` vẫn mặc định tắt, S-12).
- Tạo `StagingRemoteValuesModule.kt` (staging), lượt 1: hai tập `@ElementsIntoSet @LocalRemoteValues`: `appPlacementsShown()` = `is_show_` của `POSES_BANNER`, `POSES_DETAIL_BANNER`, `REVIEW_INTERSTITIAL` là `true` (liệt kê tên, không lấy `AdPlacement.entries`, để `gallery_*` và `sample_native_small` không bật); `adLogsOn()` = `IS_LOG_AD_FULLSCREEN_EVENTS`, `IS_LOG_AD_SLOT_EVENTS` là `true`. Lượt 2 thêm `show_open_ads`, `show_resum_ads`.
- Tạo `app/lint.xml` như mẫu (bỏ qua `NotificationPermission` chỉ cho lớp `com.squareup.picasso.`, thư viện mà InMobi kéo vào): mục 8, câu 8.
- `AndroidManifest.xml` của app không đổi: `tools:replace="android:allowBackup"` của mẫu chỉ cần cho AppsFlyer. Có SDK nào khác đặt `allowBackup="true"` làm trình gộp manifest báo lỗi thì dùng mục 3.4.

**`feature/sample`:** `SampleScreen.kt` bỏ dòng 72 (`AdSlot(placement = AdPlacement.SAMPLE_BOTTOM_BANNER, ...)`) và import `AdSlot`, `AdPlacement`; giữ `PrivacyOptionsButton()`. Ảnh chuẩn của màn mẫu không đổi: trong test không có `LocalAdsController` nên `AdSlot` không vẽ gì, kể cả trước khi bỏ.

**Các feature khác ở lượt 1:** không đổi. `PosesComponents.kt:245` gọi `AdSlot(..., separator = ...)`, `ReviewScreen.kt:79` gọi `rememberShowInterstitial(AdPlacement.REVIEW_INTERSTITIAL) { shown -> ... }`, `SettingsScreen.kt:107` gọi `PrivacyOptionsButton(content = row)`: cả ba chữ ký giữ nguyên nhờ các chỗ riêng của app ở trên.

#### Lượt 2: nối vỏ app, bật quảng cáo mở app và quay lại app

**Loading, chép tay T-024 của mẫu** (`feature/loading/src/main/.../LoadingViewModel.kt`, `LoadingScreen.kt` ở `0444fbe`, diff ở trên của mục này đã xem): `LoadingViewModel` nhận thêm `startupAds: StartupAds`; thêm `pendingAd: StateFlow<StartupAdTurn?>`, `takePendingAd()`, `onAdFinished(interShown)`, hai `AtomicBoolean` (`adFinished`, `finished`); cuối `runPending()`, sau `delay(remaining)`, gọi `startupAd()` (bắt mọi `Exception` trừ `CancellationException`, ghi `warn`, coi như không có quảng cáo); không có thì `finish()`, có thì đặt `pending` và giữ màn ở 100%. `finish()` ghi `loading_completed` một lần rồi đặt `Finished`, như hiện nay. Giữ nguyên phần riêng của app: ba việc, `InternetCheck`, tiến độ theo file, `LoadingFailure`. `LoadingRoute` nhận `showAd: (StartupAdTurn, (Boolean) -> Unit) -> Unit = rememberShowStartupAd()` và chạy `LaunchedEffect(pendingAd) { viewModel.takePendingAd()?.let { currentShowAd(it, viewModel::onAdFinished) } }` như mẫu. Màn loading tắt (`feature_loading_enabled`) thì `Finished` ngay, không gọi `StartupAds`.

**Thời gian của màn loading.** `awaitStartupAd` tính hạn chót từ lúc `:core:ads` khởi động (`Application.onCreate`), không từ lúc loading xong (`StartupAdWait.kt`: "Deadlines count from [startedAtMillis]"): đồng ý 10 giây, SDK 35 giây, quảng cáo 40 giây, dò mỗi 250 ms. Khóa tắt (lần mở đầu: `show_open_ads_first_open` tắt) thì trả `null` ngay ở nhịp đầu. Vì vậy loading của app (Remote Config tối đa 15 giây theo ADR-1001, catalog và bộ khởi đầu 15 giây mỗi file) dài nhất là `max(thời gian tải, 40 giây kể từ lúc mở)` khi đã có đồng ý và SDK sẵn sàng; tải xong sau 40 giây thì chỉ hiện quảng cáo khi nó đã sẵn sàng, không chờ thêm. Mục 5 của spec loading ghi "cộng sau thời gian tải": mục 8, câu 6.

**Ra nền trong lúc chờ** (L-091 của mẫu): `awaitStartupAd` chạy trong `viewModelScope`, nên app ra nền khi đang chờ thì vẫn chờ tới hạn; `pendingAd` chỉ được lấy trong `LaunchedEffect` khi màn được dựng lại, và `takePendingAd()` trả lượt một lần. Khung không hiện app open khi activity không ở trạng thái resumed (luật của `showStartupAd`, gỡ kẹt F-03); lượt quay lại app đầu tiên sau đó là cổng `STARTUP` của resume (chưa xong khởi động thì không có resume). Test ở mục 4.

**Popup mạng chờ quảng cáo toàn màn hình** (phần của T-017 của mẫu): `NetworkDialogController` của app thêm `fun setFullScreenAdShowing(showing: Boolean)`. Cài theo kiểu của app (`MutableStateFlow`, `compareAndSet`), không theo khóa `lock` của mẫu: giữ `adShowing = MutableStateFlow(false)`; `show(reason)` khi `adShowing` là `true` thì chỉ ghi `pendingShow = reason` và log `Dialog waits: full_screen_ad`; `setFullScreenAdShowing(false)` có `pendingShow` thì gọi `show(pendingShow)`; `setFullScreenAdShowing(true)` khi popup đang hiện thì ẩn popup, hủy lần thử lại đang chạy, và đặt `pendingShow` để hiện lại khi quảng cáo đóng (mẫu: "the dialog waits for it and shows when it closes"); `disarm()` xóa `pendingShow`. `MainActivity`: `val fullScreenAdShowing by ads.fullScreenAdShowing.collectAsState()` và `LaunchedEffect(fullScreenAdShowing) { networkDialog.setFullScreenAdShowing(fullScreenAdShowing) }`, trong `AdsHost`.

**Đánh dấu rời app có chủ đích** (G-03, G-05; `AdSuppression.kt` của khung, `rememberAdSuppression`, `rememberMarkLeavingApp`). Hai hàm trợ giúp mới trong `core/ads` (chỉ có ở app, đề xuất về mẫu), để mỗi chỗ gọi viết một dòng và test được ở `core/ads`:

```kotlin
/**
 * rememberLauncherForActivityResult that holds the ads that show by themselves while the system screen it opens is
 * up (G-03, way 1): hold right before launch, release on the first line of the result, also when launch throws.
 */
@Composable
fun <I, O> rememberAdHoldingLauncher(contract: ActivityResultContract<I, O>, onResult: (O) -> Unit):
    ManagedActivityResultLauncher<I, O>

/**
 * Starts [intent] outside the app and, when it started, marks the leave so the next return shows no resume ad
 * (G-03, way 3; G-05). Returns false when no activity handles it (ActivityNotFoundException), with no mark.
 */
@Composable
fun rememberStartOutsideApp(): (Intent) -> Boolean
```

Chỗ dùng (mục 3.2):

| Chỗ | Việc | Cách |
| --- | --- | --- |
| `CameraScreen.kt:121` | Hộp thoại quyền camera | `rememberAdHoldingLauncher(ActivityResultContracts.RequestPermission())` thay `rememberLauncherForActivityResult` |
| `CameraScreen.kt:162` | Mở cài đặt app của hệ thống | `startOutsideApp(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))` |
| `OnboardingScreen.kt:76` | Hộp thoại quyền camera ở màn chào | `rememberAdHoldingLauncher(...)` |
| `ReviewScreen.kt:83` | Hộp thoại quyền lưu (Android 9 trở xuống) | `rememberAdHoldingLauncher(...)` |
| `ReviewScreen.kt:103` | Mở cài đặt app | `startOutsideApp(...)` |
| `ReviewScreen.kt:139` | Chia sẻ ảnh | `startOutsideApp(shareChooser(uri, title))`; `share()` nhận hàm mở thay cho `Context` |
| `SettingsScreen.kt:86`, `:78` | Trang chính sách, cài đặt ngôn ngữ của hệ thống | `openOrReport` dùng `startOutsideApp`, `false` thì gọi `onUnavailable` |
| `core/review/.../InAppReviewHost.kt` | Hộp đánh giá của Play | `val suppression = rememberAdSuppression()`; `hold()` trước `requestIfDue`, `release()` khi lần hỏi xong (thêm `onDone` vào `InAppReview.requestIfDue`, gọi sau `ask`, kể cả khi lỗi). `core/review` thêm `implementation(project(":core:ads"))` (core phụ thuộc core, ADR-0001) |

Form đồng ý UMP và lần bấm quảng cáo do khung tự chặn (A-14, G-04). Thiếu một chỗ trong bảng thì quảng cáo quay lại app hiện ngay sau hộp thoại quyền: đây là rủi ro chính của lượt 2, kiểm ở mục 4.

**Staging bật hai vị trí toàn app:** `StagingRemoteValuesModule.globalAdSwitchesOn()` thêm `SHOW_OPEN_ADS`, `SHOW_RESUM_ADS` là `true`. Không có `SHOW_OPEN_ADS_FIRST_OPEN`, `IS_SHOW_NATIVE_AFTER_INTER`, `IS_SHOW_NATIVE_BOTTOM` (mặc định của khung, tắt). `OPEN_AD_TYPE`, `RESUME_AD_TYPE` không đặt (mặc định `aoa`).

**Firebase staging.** Bản staging của app dùng project Firebase staging thật (bí mật `STAGING_GOOGLE_SERVICES_JSON`, `staging.yml`), khác dự án mẫu. `DefaultRemoteSettings` đọc giá trị Firebase trước, giá trị cục bộ sau (`RemoteSettings.kt` của mẫu; `docs/ops/remote-config.md`, mục 4), nên một khóa đặt trên Firebase staging thắng `StagingRemoteValuesModule`. Việc của người, trước khi phát bản staging có lượt 2 (ghi vào danh sách chạy, mục "Kiểm tra trước khi duyệt"): trên Firebase staging không đặt `show_open_ads`, `show_resum_ads`, `is_show_poses_banner`, `is_show_poses_detail_banner`, `is_show_review_interstitial` là `false`, không đặt `show_open_ads_first_open`, `is_show_native_after_inter`, `is_show_native_bottom` là `true`; các khóa cũ `ads_*_enabled`, `ads_interstitial_min_interval_seconds`, `ads_review_interstitial_min_interval_seconds` không còn ai đọc. Không nhập `docs/remote-config-template.json` vào project staging (`remote-config.md`, mục 5, bước 7). Test của app chỉ kiểm giá trị cục bộ; điều Firebase staging thật trả là kiểm thủ công.

#### Lượt 3: bảng khóa, ADR, tài liệu

- `app/src/main/.../config/AppRemoteKeys.kt` theo mẫu, thêm các module của app:
  ```kotlin
  ":core:config" to FeatureFlag.entries.map { it.remoteKey } + AppLinks.KEYS + PoseMatchingSettings.KEYS,
  ":core:ads" to AdsSettings.all() + InterstitialBreakSettings.all(),
  ":core:analytics" to AppsFlyerSettings.all(),
  ":core:network" to NetworkSettings.all(),
  ":core:poses" to PosesRemoteKeys.all(),
  ":core:review" to ReviewSettings.all(),
  ```
  (`PosesRemoteKeys` là `object` công khai mới của `core/poses` bọc khóa `internal`; `AppLinks.KEYS`, `PoseMatchingSettings.KEYS` là `companion` của hai interface.) Khóa `is_log_af_*` của `AppsFlyerSettings` vẫn liệt kê: khung đọc chúng, không có SDK nên không gửi gì; mô tả của bảng sinh ra không đổi.
- `app/src/test/.../config/RemoteConfigKeysTest.kt` theo mẫu, với một chỗ riêng: `everyNumberKey_endsWithUnit` có tập `RELEASED_BEFORE_ADR_0023 = setOf("in_app_review_min_saved_photos", "in_app_review_min_days_between", "pose_match_threshold")` (khóa đã có trên Firebase trước quy ước, `remote-config.md` mục 2: không đổi tên khóa đã phát hành), có câu đối chứng là tập này chỉ gồm khóa có thật trong `AppRemoteKeys`. Mục 8, câu 5.
- `app/build.gradle.kts`: khối `remoteConfigKeys.*` của mẫu (ba `systemProperty`, `inputs.files`).
- Sinh `docs/remote-config-keys.md`, `docs/remote-config-template.json` bằng `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`.
- `docs/template-sync.txt`: bỏ `hold=core/config`, `hold=core/data`, `hold=core/logging`, `hold=gradle`, `hold=settings.gradle.kts`, `hold=build-logic`, `hold=build.gradle.kts`, `hold=gradle.properties` (đã gộp tay, hoặc mẫu không đổi từ `0eaeadb`: `core/data`, `core/logging`, `build-logic`, `build.gradle.kts`, `gradle.properties`). Giữ `hold=app`, `hold=feature`, `hold=core/assets`, `hold=core/designsystem`, `hold=core/network`, `hold=scripts/release.sh`, `hold=docs/ops/release.md` (T-075). Thêm chú thích: `app/` và `feature/loading` đã lấy tay phần của T-007, T-008, T-024 (liệt kê file); `core/network` chỉ lấy `InternetConnectivity` và `setFullScreenAdShowing`; `core/analytics` không có SDK AppsFlyer (`NoOpAppsFlyerSink`); `gradle/libs.versions.toml` không có `appsflyer`, nên đợt đồng bộ sau gặp dòng đó thì bỏ. Ghi chú `release.sh` để lại cho T-075 giữ nguyên ý.
- Tạo `docs/adr/1007-khung-adskit-chua-bat-appsflyer.md` (`origin: app`): app dùng khung AdsKit; các chỗ lệch có chủ đích (không AppsFlyer, sink rỗng; vị trí `gallery_*`, `sample_native_small` giữ chỉ cho test của khung; luật đếm của `review_interstitial`; banner thu về 0 dp khi lỗi; Firebase staging thật thắng giá trị cục bộ). ADR-1004: `status: superseded`, `superseded_by: ADR-1007`. Sinh lại `docs/foundation.md` bằng `scripts/foundation.py`.
- `docs/ops/manual-test.md` (chỉ có ở app): thêm mục "Quảng cáo (T-074)" với các bước thử trên máy thật ở mục 4 và đoạn Firebase staging ở trên.
- Trên nhánh báo cáo của phiên, không trong PR của lượt 3 (PR của task chỉ có code, test, ảnh chuẩn: `auto-run.md`, bước 7): chữ đổi của `features/camera/spec.md`, `features/loading/spec.md`, `features/poses/spec.md` (bỏ "Áp dụng từ T-074", "trước đó ..."; tên khóa mới ở KB-6, KB-9 và mục 8; mục 6: sự kiện phân tích mà khung ghi và dữ liệu lưu trên máy), mỗi spec chuyển `changed` chờ Nhật xác nhận lại. Danh sách sự kiện và dữ liệu, để chép vào mục 6:
  - Sự kiện Firebase Analytics do khung ghi (`FirebaseAdMetrics`, `ads-business-rules.md` mục 8.2), mỗi sự kiện có `ad_format`, `ad_placement`: `ad_impression_placement` (luôn gửi, có `value`, `currency`, `ad_source`, `ad_unit_name`, `ad_platform`), `ad_opportunity` (luôn gửi, `opportunity_result`, `load_state`), `ad_load_success`, `ad_load_fail`, `ad_show_fail`, `ad_clicked` (theo `is_log_ad_fullscreen_events`, `is_log_ad_slot_events`, mặc định tắt, staging bật). `ad_reward_earned`, `reward_dialog` không có vì app không có vị trí rewarded. Không gửi gì tới AppsFlyer.
  - Dữ liệu lưu trên máy mới (DataStore chung của `core/data`, không cần chuyển dữ liệu cũ): `ads_first_open_done` (đã mở lần đầu, luật A-01), `ads_inter_display_count` (số interstitial đã hiện từ khi cài, N-05). Bản đang cài nâng lên: lần mở đầu sau khi cập nhật được tính là lần mở đầu, nên không có quảng cáo mở app ở lần đó.

### 3.2. Danh sách file

Ký hiệu `<P>` trong cột Nội dung là `com/vglstudio/camera/pose/line/guide`; cột Đường dẫn ghi đủ. Dòng thư mục (kết thúc bằng `/`) là phần lấy nguyên từ nhánh `template-sync/take-...`; các file app sửa thêm trong đó có dòng riêng.

**Trước phiên (người, bước 1 tới 4 của mục 3.1):**

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `docs/template-sync.txt` | Người, trên nhánh prep: tách `hold=core` thành tám dòng theo module; `--take` bỏ `hold=core/ads`, `hold=core/analytics`. Lượt 3 sửa tiếp (dưới) |

**Lượt 1:**

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/` | Lấy nguyên từ nhánh take (254 file đổi từ `0eaeadb`: code, test, 41 ảnh chuẩn, `consumer-rules.pro`, `src/main/AndroidManifest.xml`, `build.gradle.kts`) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | Bản mẫu cộng ba dòng của app, không có `SAMPLE_BOTTOM_BANNER` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdSlot.kt` | `AdSlot(..., separator)`, thu khung 0 dp khi lỗi tải; overload `PrivacyOptionsButton(modifier, content)` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InterstitialBreaks.kt` | `BreakCap` hai luật, `InterstitialBreakSettings`, `InterstitialBreakGate`, `DefaultInterstitialBreakGate`, `BreakTicket` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/BreakInterstitial.kt` | `LocalInterstitialBreakGate`, `rememberShowInterstitial(placement, onFinished: (Boolean) -> Unit)` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/di/InterstitialBreakModule.kt` | `@Binds` của cổng đếm |
| Xóa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InterstitialFlow.kt` | Thứ tự bước nay của khung |
| Xóa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InterstitialFlowTest.kt` | Đi cùng `InterstitialFlow.kt` (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InterstitialBreaksTest.kt` | Bỏ hai test của luật bị bỏ, thêm test cổng đếm (mục 3.5, mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/ShowInterstitialTest.kt` | Giữ hai test, thêm test cổng đếm (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdSlotTest.kt` | Viết lại trên `BannerSlotHost` của khung, giữ năm ý của test cũ (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/PrivacyOptionsButtonTest.kt` | Giữ; đổi import nếu cần |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsSettingsTest.kt` | Bản mẫu (từ take), cộng dữ liệu ba vị trí của app nếu test liệt kê vị trí (ADR-0029) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | Bản mẫu (từ take), như trên |
| Ghi lại | `core/ads/src/test/screenshots/` | 41 ảnh của khung chụp lại với theme của app |
| Sửa | `core/analytics/` | Lấy nguyên từ nhánh take, trừ `src/test/kotlin/<P>/core/analytics/AppsFlyerSdkUseTest.kt`: xóa khỏi nhánh (trái câu 3 của spec, mục 3.5) |
| Sửa | `core/analytics/build.gradle.kts` | Bỏ `implementation(libs.appsflyer)` |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AppsFlyerSink.kt` | File đến từ nhánh take (chưa có trên nhánh chính); phiên sửa: `NoOpAppsFlyerSink` thay `AppsFlyerLibSink` |
| Sửa | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/di/AnalyticsModule.kt` | Gắn `NoOpAppsFlyerSink` |
| Tạo | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/NoAppsFlyerSdkTest.kt` | Mục 4 |
| Tạo | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/RemoteKey.kt` | Bản mẫu |
| Tạo | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/LocalRemoteValues.kt` | Bản mẫu |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/RemoteSettings.kt` | `value(RemoteKey)`, giá trị cục bộ |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/RemoteFlagSource.kt` | `double` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FirebaseRemoteFlagSource.kt` | `double`, `activeValueOrNull` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FeatureFlag.kt` | `description`, `remoteKey` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/DefaultFeatureFlags.kt` | Đọc `remoteKey` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/di/ConfigModule.kt` | `@Multibinds @LocalRemoteValues` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/AppLinks.kt` | `RemoteKey`, `KEYS` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/PoseMatchingSettings.kt` | `RemoteKey`, `KEYS` |
| Sửa | `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/DefaultRemoteSettingsTest.kt` | Bản mẫu (`0444fbe`), giữ test riêng của app nếu có |
| Sửa | `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FakeRemoteFlagSource.kt` | `double` |
| Sửa | `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/DefaultFeatureFlagsTest.kt` | Thêm test của mẫu (mô tả không trống) |
| Sửa | `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/DefaultAppLinksTest.kt` | Bản giả theo API mới (ADR-0029) |
| Sửa | `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/DefaultPoseMatchingSettingsTest.kt` | Như trên |
| Tạo | `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/RemoteKeyTest.kt` | Bản mẫu |
| Tạo | `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FirebaseRemoteValueTest.kt` | Bản mẫu |
| Sửa | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/PosesSettings.kt` | `CATALOG_PATH` là `RemoteKey` |
| Sửa | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/DefaultPoseCatalogRepository.kt` | Dòng 127 đọc qua `value` |
| Sửa | `core/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/FakeRemoteSettings.kt` | API mới |
| Sửa | `core/review/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/review/ReviewSettings.kt` | `RemoteKey`, `all()` |
| Sửa | `core/review/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/review/InAppReview.kt` | Dòng 47, 51 đọc qua `value` |
| Sửa | `core/review/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/review/Fakes.kt` | API mới |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkSettings.kt` | `RemoteKey`, `all()` |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogController.kt` | Dòng 123 đọc qua `value` (lượt 2 sửa tiếp) |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/InternetConnectivity.kt` | Bản mẫu |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/di/NetworkModule.kt` | `@Binds` `InternetConnectivity` |
| Sửa | `core/network/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/network/Fakes.kt` | API mới |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/network/DefaultInternetConnectivityTest.kt` | Mục 4 |
| Sửa | `gradle/libs.versions.toml` | Chín adapter, Unity Ads; không có AppsFlyer |
| Sửa | `settings.gradle.kts` | Kho Pangle, Mintegral |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/TemplateApplication.kt` | `ads.start`, `appsFlyer.prepare` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | `AdsHost`, `LocalInterstitialBreakGate`, bỏ `onUserInteraction` (lượt 2 sửa tiếp) |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/di/AnalyticsConfigModule.kt` | `AppsFlyerConfig("")` |
| Sửa | `app/src/production/kotlin/com/vglstudio/camera/pose/line/guide/di/ProductionAdsModule.kt` | `ConsentShortcutConfig.OFF` |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingAdsModule.kt` | `ConsentShortcutConfig(enabledInBuild = true)` |
| Tạo | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModule.kt` | Lượt 1: ba vị trí, hai khóa log. Lượt 2 sửa tiếp: `SHOW_OPEN_ADS`, `SHOW_RESUM_ADS` |
| Tạo | `app/src/testStaging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModuleTest.kt` | Mục 4; lượt 2 sửa tiếp tập giá trị mong đợi ở commit test của lượt 2 |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/LocalRemoteValuesSourceSetTest.kt` | Bản mẫu |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/MergedManifestTest.kt` | Bản mẫu, danh sách quyền và `queries` của app, không có phần AppsFlyer (mục 4) |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/NoAppsFlyerDependencyTest.kt` | Mục 4 |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/AppAdsRootTest.kt` | Mục 4 |
| Tạo | `app/lint.xml` | Bản mẫu (Picasso) |
| Sửa | `app/src/main/AndroidManifest.xml` | (dự phòng) `tools:replace="android:allowBackup"`, chỉ theo mục 3.4 |
| Sửa | `feature/sample/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/sample/SampleScreen.kt` | Bỏ `AdSlot` của `SAMPLE_BOTTOM_BANNER` |
| Sửa | `feature/sample/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/sample/SampleScreenTest.kt` | Test `noSampleBottomBanner` (mục 4) |

**Lượt 2:**

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/LeaveAppActions.kt` | `rememberAdHoldingLauncher`, `rememberStartOutsideApp` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/LeaveAppActionsTest.kt` | Mục 4 |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogController.kt` | `setFullScreenAdShowing` |
| Sửa | `core/network/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/network/NetworkDialogControllerTest.kt` | Mục 4 |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingViewModel.kt` | `StartupAds`, `pendingAd` |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreen.kt` | `LoadingRoute(showAd)` |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingViewModelTest.kt` | Bản giả `StartupAds` trong hàm dựng (ADR-0029), test mới |
| Tạo | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingRouteTest.kt` | Theo `LoadingRouteTest` của mẫu |
| Tạo | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/FakeStartupAds.kt` | Bản giả |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | Hai chỗ của bảng ở mục 3.1 |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreen.kt` | Ba chỗ |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreen.kt` | Một chỗ |
| Sửa | `feature/settings/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsScreen.kt` | `openOrReport` qua `startOutsideApp` |
| Sửa | `core/review/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/review/InAppReviewHost.kt` | Giữ chặn trong lúc hỏi đánh giá |
| Sửa | `core/review/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/review/InAppReview.kt` | `onDone` của `requestIfDue` |
| Sửa | `core/review/build.gradle.kts` | `implementation(project(":core:ads"))` |
| Sửa | `core/review/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/review/RequestInAppReviewTest.kt` | Mục 4 |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | `setFullScreenAdShowing` |

Lượt 2 còn sửa tiếp hai file Tạo ở lượt 1: `StagingRemoteValuesModule.kt` và `StagingRemoteValuesModuleTest.kt` (dòng của lượt 1).

**Lượt 3:**

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/config/AppRemoteKeys.kt` | Mục 3.1 |
| Tạo | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/PosesRemoteKeys.kt` | `object` công khai bọc khóa của `core/poses` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` | Bản mẫu, tập khóa đặt tên trước ADR-0023 |
| Sửa | `app/build.gradle.kts` | Khối `remoteConfigKeys.*` |
| Tạo | `docs/remote-config-keys.md` | Sinh từ code |
| Tạo | `docs/remote-config-template.json` | Sinh từ code |
| Sửa | `docs/template-sync.txt` | Bỏ tám dòng `hold`, thêm chú thích |
| Tạo | `docs/adr/1007-khung-adskit-chua-bat-appsflyer.md` | ADR của app |
| Sửa | `docs/adr/1004-chua-ap-dung-quang-cao-adskit.md` | `superseded`, `superseded_by: ADR-1007` |
| Ghi lại | `docs/foundation.md` | `scripts/foundation.py` |
| Sửa | `docs/ops/manual-test.md` | Mục "Quảng cáo (T-074)" |
| Sửa | `docs/features/camera/spec.md` | Nhánh báo cáo: mục 5, 6, KB-9, mục 8; `changed` |
| Sửa | `docs/features/loading/spec.md` | Nhánh báo cáo: mục 5, 6; `changed` |
| Sửa | `docs/features/poses/spec.md` | Nhánh báo cáo: mục 5, 6, KB-6, mục 8; `changed` |

Nơi đọc các file dùng chung bị đổi (L-020):

- `docs/template-sync.txt`: `scripts/template-sync.sh` của mẫu (không có ở app), `scripts/tests/run.sh` và `scripts/foundation.py` không đọc dòng `hold` (đã `grep -n "template-sync" scripts/ .github/`); khối `release.sh` của `scripts/tests/run.sh` khớp `scripts/release.sh` đang giữ, không đổi.
- `gradle/libs.versions.toml`, `settings.gradle.kts`: `pr-check.yml` build bản release khi PR đổi file build (`docs/CLAUDE.md`, mục Khi viết plan); `scripts/cloud-setup.sh` không đọc danh sách thư viện.
- `core/ads/.../AdPlacement.kt`: `scripts/release.sh` (giữ ở `0eaeadb`, không đọc được khai báo nhiều thuộc tính và không biết tiền tố `gallery_`: việc của T-075, mục 6), `scripts/ads-plan.py` (đọc mọi dòng enum, khối `ads-plan` của `scripts/tests/run.sh` chạy khi có `docs/remote-config-keys.md`), `AdPlacementSourceTest` của khung.
- `docs/remote-config-keys.md`: `scripts/tests/run.sh` (khối `ads-plan`, dòng 4185: hết bỏ qua), `scripts/ads-plan.py`, `RemoteConfigKeysTest`.
- Ảnh chuẩn của `core/ads`: chỉ `verifyRoborazziDebug` trong `scripts/check.sh` đọc.
- `.github/workflows/pr-check.yml` nhắc `gradle/libs.versions.toml`, `settings.gradle.kts` (bộ lọc đường dẫn để build bản release khi PR đổi file build) và `docs/foundation.md` (kiểm tài liệu): không sửa; lượt 1 vì vậy có thêm job build release, như mong muốn.
- `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt`, `build-logic/settings.gradle.kts` đọc catalog theo tên `libs`; plan chỉ thêm dòng, không đổi tên hay dòng có sẵn: không ảnh hưởng.
- `scripts/cloud-env-check.sh` đọc dòng `compileSdk = "..."` của `gradle/libs.versions.toml`: dòng đó không đổi.
- `scripts/setup-check-app.sh` chỉ nhắc `docs/template-sync.txt` trong lời báo (plan T-065), không đọc dòng `hold`; còn đọc `PosesSettings.kt` bằng `sed` hai dòng `CATALOG_PATH_KEY = "..."` và `DEFAULT_CATALOG_PATH = "..."`; `scripts/tests/app.sh` (dòng 64) đọc cùng file. Plan giữ nguyên hai hằng `internal const val` này đúng dạng một dòng, `RemoteKey` mới (`CATALOG_PATH`) dựng từ chúng, nên hai script không ảnh hưởng.
- `scripts/setup-check.sh` đọc `applicationId` và `ASSETS_BASE_URL` của `app/build.gradle.kts`: lượt 3 chỉ thêm khối `remoteConfigKeys.*`, không đổi hai dòng đó.
- `scripts/status.py` đọc trạng thái (`status:`) của `docs/features/camera/spec.md`, `docs/features/loading/spec.md`, `docs/features/poses/spec.md`: lượt 3 chuyển các spec sang `changed` trên nhánh báo cáo, là giá trị hợp lệ mà trang tiến độ hiển thị; không sửa script.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: không có cờ mới. `FeatureFlag` có thêm `description` và `remoteKey`; tên, mặc định tắt giữ nguyên. Quảng cáo mở app cần `feature_loading_enabled` bật.
- Quảng cáo: ba vị trí có sẵn chuyển sang khung (`review_interstitial`, `poses_banner`, `poses_detail_banner`, khóa `is_show_<mã>` mặc định tắt); hai vị trí toàn app của khung bật theo câu 6 (`open_app` qua `show_open_ads`, `resume` qua `show_resum_ads`, mặc định tắt; `open_app_inter`, `resume_inter` chỉ khi Remote Config đổi `open_ad_type`, `resume_ad_type`); bỏ `sample_bottom_banner`. Vị trí `gallery_*`, `sample_native_small`, `native_combo` khai báo nhưng không màn nào gọi, không bật ở staging (mục 8, câu 1). Mã quảng cáo: staging dùng mã thử của Google (`GoogleTestAdUnitIds`); production không đổi ở ticket này (T-075).
- Sự kiện phân tích: khung ghi `ad_impression_placement`, `ad_opportunity` (luôn gửi) và `ad_load_success`, `ad_load_fail`, `ad_show_fail`, `ad_clicked` (theo hai khóa `is_log_ad_*`, staging bật) qua Firebase Analytics; không gửi gì tới AppsFlyer. Chưa có trong mục 6 của spec màn: chép ở lượt 3, Nhật xác nhận lại (mục 8, câu 3). `loading_completed` giữ nguyên, ghi lúc chuyển sang màn chính, sau quảng cáo mở app nếu có.
- File R2: Không chạm.
- Dữ liệu trên máy: hai khóa DataStore mới của khung, `ads_first_open_done`, `ads_inter_display_count`, không cần chuyển dữ liệu cũ. Luật đếm của `review_interstitial` vẫn chỉ trong bộ nhớ.
- **Quyền truy cập:** `android.permission.VIBRATE` (adapter AppLovin, SDK `applovin-sdk-13.6.4` rung mà không kiểm quyền; mẫu giữ quyền này, plan T-015 của mẫu); spec chưa có: đề xuất thêm vào `features/ads/spec.md` mục 6 trước khi duyệt (mục 8, câu 4). Không thêm: `CHECK_LICENSE`, `ACCESS_WIFI_STATE` (gỡ bằng `core/ads/src/main/AndroidManifest.xml` của khung), hai quyền đọc nguồn cài đặt của Samsung và Huawei mà AppsFlyer kéo vào (không có SDK; `core/analytics/src/main/AndroidManifest.xml` vẫn gỡ), `POST_NOTIFICATIONS`. Quyền khác mà các SDK mới khai báo đã có trong manifest gộp hiện nay qua `play-services-ads` và Firebase: `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID` (`com.google.android.gms.permission.AD_ID`), `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_TOPICS`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `BIND_GET_INSTALL_REFERRER_SERVICE` (của `play-services-measurement`, không phải AppsFlyer). Đã tra trong manifest AAR ở bộ đệm Gradle của máy dev (phiên bản đúng như `libs.versions.toml` của mẫu): AppLovin `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `VIBRATE`, `CHECK_LICENSE`, `AD_ID`; Meta, InMobi, Mintegral, Moloco, ironSource (`mediation-sdk-9.6.1`, `adquality-sdk`) `INTERNET`, `ACCESS_NETWORK_STATE`, một số `AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`; Unity `ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_TOPICS`, `AD_ID`; Pangle (`pag-sdk`), DT Exchange (`marketplace-sdk`) không thêm quyền nào khác. Danh sách cuối kiểm bằng `MergedManifestTest` (mục 4) và đo lại ở mục 4.1.
- **Thư viện và SDK:** thêm vào `gradle/libs.versions.toml` (đúng phiên bản của mẫu ở `0444fbe`, đã chạy trên staging của mẫu tới `0.1.0-staging.18`), module dùng: `core/ads`.
  - `com.google.ads.mediation:applovin:13.6.4.2`
  - `com.google.ads.mediation:facebook:6.22.0.1`
  - `com.google.ads.mediation:fyber:8.5.0.0`
  - `com.google.ads.mediation:inmobi:11.5.0.0`
  - `com.google.ads.mediation:ironsource:9.6.1.0`
  - `com.google.ads.mediation:mintegral:17.1.81.1`
  - `com.google.ads.mediation:moloco:4.13.0.0`
  - `com.google.ads.mediation:pangle:8.3.0.4.0`
  - `com.google.ads.mediation:unity:4.21.0.0`
  - `com.unity3d.ads:unity-ads:4.21.0`

  Giấy phép: điều khoản SDK của từng mạng (độc quyền, miễn phí), như mẫu đã nhận ở T-015, T-034. Quyền kéo vào: xem dòng trên. Hai kho Maven mới (Pangle, Mintegral) ở `settings.gradle.kts`. Đã có, thêm module dùng: `libs.roborazzi` (plugin, cho ảnh chuẩn của `core/ads`, đã có ở các feature), `core/ads` phụ thuộc `:core:analytics`, `:core:data`, `:core:designsystem`, `:core:logging` (đều đã có). Không thêm `com.appsflyer:af-android-sdk`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc 1:** trình gộp manifest báo xung đột `android:allowBackup` khi không có AppsFlyer mà một SDK mediation khác đặt `allowBackup="true"`.
  - **Điều kiện chuyển:** `:app:processStagingDebugMainManifest` lỗi `Attribute application@allowBackup value=(false) ... is also present at [<thư viện>]`.
  - **Cách làm thay:** thêm `xmlns:tools` và `tools:replace="android:allowBackup"` vào `<application>` của `app/src/main/AndroidManifest.xml`, chú thích ghi tên thư viện thật thấy trong lỗi. `MergedManifestTest.mergedManifest_keepsBackupOff` giữ nguyên.
- **Điểm chưa chắc 2:** banner thu về 0 dp khi lỗi tải (`AdSlot(..., separator)`) cần nối kết quả tải từ `SdkBannerAdView` lên `BannerSlot`; cấu trúc `BannerSlotHost` có thể khiến khung 0 dp bị coi là không hiện (`bannerShouldRun` đọc `visible` theo vòng đời, không theo kích thước), nhưng nếu lần thử lại B-02 không chạy khi khung 0 dp.
  - **Điều kiện chuyển:** test `AdSlotTest.loadedAfterFailure_showsBannerAndSeparatorAgain` (mục 4) vẫn đỏ sau 2 lần sửa vì lần tải sau không được gửi.
  - **Cách làm thay:** không đổi chiều cao khung; `BannerSlot` vẽ khung ở chiều cao 1 px trong suốt khi lỗi (vẫn đo được, vẫn thử lại), test đo chiều cao nhỏ hơn 1 dp thay cho 0 dp, cùng ý "không chừa khoảng trống".
- **Điểm chưa chắc 3:** ảnh chuẩn của `core/ads` chụp lại với theme của app có thể không ổn định (hiệu ứng nạp của native, L-029).
  - **Điều kiện chuyển:** `verifyRoborazziDebug` lệch trên CI ở ảnh vừa ghi, hai lần liên tiếp.
  - **Cách làm thay:** không có; dừng task (mục 7). Ảnh của mẫu đã ổn định trên CI của mẫu với cùng test, nên chỉ theme đổi.

### 3.5. Hành vi đã có bị đổi

Kết quả `python3 scripts/existing-tests.py docs/features/ads/plans/T-074-lay-khung-quang-cao-adskit.md` ở mục cuối của mục này. Các dòng dưới là hành vi đổi và câu khẳng định đi cùng.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `review_interstitial` cách lần trước ít nhất 120 giây (`BreakCap.minIntervalSeconds`) | "`BreakCap(firstBreak: Int, breaksBetween: Int)` bỏ `minIntervalSeconds`" | `InterstitialBreaksTest.afterShown_waitsMinInterval` | Xóa hàm này: luật bị bỏ theo câu 1 của spec, không còn ý định để giữ. Nhịp 45 giây là `ads_interval` của khung, có test của khung (`FullScreenShowDecision`, `AdsSettingsTest`). Ghi vào nhật ký như một test xóa theo plan đã duyệt (mục 8, câu 10). |
| Cổng đồng ý, khóa và popup mạng gộp trong `interstitialAllowed` | "bỏ `interstitialAllowed` (cổng đồng ý, khóa và popup mạng nay là của khung)" | `InterstitialBreaksTest.allowed_onlyWithConsentFlagAndNoNetworkDialog` | Xóa: hàm không còn. Ý "không hiện khi chưa đồng ý hay khóa tắt" có test của khung (`FullScreenAdsTest`, `AdsControllerFunnelTest`); ý "không hiện khi popup mạng đang mở" đổi (dòng dưới). |
| Interstitial không hiện khi popup mạng đang mở (`AdsController.kt:125`, `networkStatus.dialogShown`) | Khung chỉ theo `InternetConnectivity`: mất mạng thì không nạp, có quảng cáo đã nạp thì vẫn có thể hiện; popup chờ quảng cáo đóng rồi mới hiện (`setFullScreenAdShowing`, lượt 2) | `InterstitialFlowTest` (cả file, các test `networkDialog...`) | Xóa cả file cùng `InterstitialFlow.kt`. Hành vi mới có test ở lượt 2 (`NetworkDialogControllerTest`, mục 4). Đổi có chủ đích (mục 8, câu 11). |
| Chờ người dùng ngừng chạm rồi mới hiện interstitial (`FullScreenAdGuard.waitUntilQuiet`, `MainActivity.onUserInteraction`) | "bỏ `onUserInteraction()`" | `InterstitialFlowTest` (bước `waitUntilQuiet`); `FullScreenAdGuardTest` của app | `FullScreenAdGuardTest` lấy bản của khung (cùng tên file, nội dung của mẫu sau take); ý "chờ yên" không còn trong luật của khung (`ads-business-rules.md`). |
| Đếm lần dừng, lần đầu bỏ, sau khi hiện chờ 3 lần, mỗi vị trí đếm riêng, `NONE` cho mọi lần | Giữ (`InterstitialBreaks` của app) | `InterstitialBreaksTest.firstBreak_isSkipped_secondAllowed`, `afterShown_nextAllowedThreeBreaksLater`, `placements_countSeparately`, `noneCap_allowsEveryBreak` | Giữ ý và câu khẳng định; chỉ bỏ đối số `nowMillis` và `minIntervalSeconds` khỏi lời gọi (đổi chữ ký theo plan). `placements_countSeparately` dùng `SAMPLE_BOTTOM_BANNER`: đổi sang `POSES_BANNER` (dữ liệu, ADR-0029). |
| Không có controller thì `rememberShowInterstitial(placement) { shown -> }` báo `false` đúng một lần; vị trí banner bị từ chối | Giữ | `ShowInterstitialTest.withoutAdsController_reportsNotShownOnce`, `bannerPlacement_isRejected` | Giữ nguyên câu khẳng định; `bannerPlacement_isRejected` dùng `SAMPLE_BOTTOM_BANNER`: đổi sang `POSES_BANNER` (dữ liệu). |
| Banner giữ chiều cao trước khi tải, có đường kẻ; lỗi tải thì 0 dp và mất đường kẻ; tải lại được thì hiện lại; không có controller thì không chiếm chỗ | Giữ, nối trên `BannerSlot` của khung | `AdSlotTest.beforeLoad_keepsBannerHeight_withSeparator`, `failedLoad_collapsesBannerToZero_andHidesSeparator`, `loadedAfterFailure_showsBannerAndSeparatorAgain`, `withoutAdsController_takesNoSpace_andDrawsNoSeparator`, `bannerListener_reportsFailedThenLoaded` | Giữ tên và ý của năm hàm. Bốn hàm đầu viết lại phần dựng trên `BannerSlotHost` (internal của khung, cùng module) với `SdkBannerAdView` giả, vì `BannerView(adView, heightDp, separator)` cũ không còn; câu khẳng định (chiều cao `> 0`, `== 0`, có hay không có đường kẻ) giữ nguyên. `bannerListener_reportsFailedThenLoaded` đổi thành kiểm hàm báo kết quả tải mới (`onLoadResult`), cùng ý. Đây là viết lại phần dựng, không đổi câu khẳng định; ghi vào nhật ký. |
| Khóa `ads_<mã>_enabled`, `ads_<mã>_first_break`, `ads_<mã>_min_interval_seconds`, `ads_interstitial_min_interval_seconds` | `is_show_<mã>`, `ads_interval`, hai khóa đếm của `InterstitialBreakSettings` | `AdsSettingsTest` (bản app: `placementEnabledKey`, `breakCapDefaults`), `AdUnitIdsTest` (bản app) | Thay bằng bản của khung (từ take). Bản app khẳng định tên khóa cũ, trái câu 2 của spec. Ý mặc định "vị trí tắt, giới hạn 2 và 3" chuyển sang `InterstitialBreaksTest.settings_defaultsAreTwoAndThree` và test của khung `AdsSettingsTest` (mọi `is_show_` mặc định `false`). |
| `RemoteSettings.long/string/boolean(key, default)` | `value(RemoteKey)`; giá trị cục bộ của staging khi Firebase không có | `DefaultRemoteSettingsTest` (bản app), `DefaultAppLinksTest`, `DefaultPoseMatchingSettingsTest`, `DefaultPoseCatalogRepositoryTest`, `InAppReviewTest`, `RequestInAppReviewTest`, `NetworkDialogControllerTest` | `DefaultRemoteSettingsTest`: bản mẫu (cùng ý: có giá trị Firebase thì dùng, không thì mặc định; thêm giá trị cục bộ). Các test khác: chỉ đổi bản giả (dữ liệu, ADR-0029), câu khẳng định giữ. |
| `FeatureFlag(key, defaultValue)` | Thêm `description`, `remoteKey` | `DefaultFeatureFlagsTest` | Giữ; thêm test mô tả không trống của mẫu. |
| Vị trí `SAMPLE_BOTTOM_BANNER` ở màn mẫu | Không có | Các test bản app trong `core/ads` dùng nó (trên) | Như trên. Ảnh chuẩn `SampleScreen_*` giữ (không có `LocalAdsController` trong test). |
| `AppsFlyerSdkUseTest` của mẫu khẳng định một file dùng SDK | Không file nào dùng SDK | `AppsFlyerSdkUseTest` (đến từ take, chưa có trên nhánh chính) | Xóa; thay bằng `NoAppsFlyerSdkTest.noMainSource_importsTheSdk` (mục 4), ý ngược theo câu 3 của spec. |
| Màn loading xong thì chuyển ngay sang màn chính | Lượt 2: xong việc thì hỏi `StartupAds`; có quảng cáo thì giữ 100% tới khi quảng cáo đóng | `LoadingViewModelTest` (các test kết thúc bằng `Finished`), `LoadingScreenTest` | Giữ: bản giả `StartupAds` trả `null` thì luồng như cũ. Thêm test cho trường hợp có quảng cáo (mục 4). |
| Popup mạng hiện ngay khi mất mạng quá thời gian chờ | Lượt 2: khi quảng cáo toàn màn hình đang hiện thì chờ quảng cáo đóng | `NetworkDialogControllerTest` (các test `show...`) | Giữ: `setFullScreenAdShowing` mặc định `false`. Thêm test mới (mục 4). |
| `openOrReport`, chia sẻ, xin quyền gọi thẳng hệ thống | Lượt 2: qua `rememberAdHoldingLauncher`, `rememberStartOutsideApp` | Các test của `SettingsScreen`, `ReviewScreen`, `CameraScreen`, `OnboardingScreen` (route không có controller trong test) | Giữ: không có `LocalAdsController` thì hai hàm chỉ mở như cũ. |

| Banner thật không có controller thì không chiếm chỗ ở thư viện và chi tiết | Giữ: `AdSlot` của khung không có `LocalAdsController` vẽ `Box(modifier)` rỗng, cao 0 | `PosesScreenTest.realBottomBanner_withoutAds_takesNoSpace`, `PoseDetailScreenTest.realBottomBanner_withoutAds_takesNoSpace` (đo chiều cao) | Giữ nguyên câu khẳng định; nếu đỏ là lỗi của `AdSlot(..., separator)`, sửa code |
| `RemoteSettings.string` có giá trị Firebase thì dùng, không thì mặc định | Giữ, qua `value(RemoteKey.string(...))` | `DefaultRemoteSettingsTest.stringRemoteValue_replacesDefault`, `stringWithoutRemoteValue_usesDefault` | Giữ tên và ý; lời gọi đổi sang `value(RemoteKey.string("x", "d", "..."))` (đổi chữ ký theo plan), câu khẳng định giữ |

Kết quả `python3 scripts/existing-tests.py docs/features/ads/plans/T-074-lay-khung-quang-cao-adskit.md` (2026-10-28, trên nhánh chính `dbcb588`): 2466 test dùng tên của các file plan đổi; "Cần xem trước" 84 dòng (lặp theo từng file, 20 test khác nhau), "Nên xem" 473 dòng (khoảng 75 test khác nhau); plan nhắc 55 test. Hai file chưa có trên nhánh chính (`AppsFlyerSink.kt`, `StagingRemoteValuesModule.kt`) bị bỏ qua. Đã đọc:

- Cần xem trước: `LoadingScreenTest` (sáu test đo bố cục: `failed_layoutMatchesDesign`, `inProgress_layoutMatchesDesign`, `failed_onShortScreen_*`, `failedServer_onShortScreen_retryReachable_th`, `appName_staysInPlace_whenFailedServer`), `CameraScreen*Test` (đo theo `CameraTestTags`), `SettingsScreenScreenshotTest.languagePicker_small`: giữ, vì plan chỉ đổi `LoadingRoute`, `CameraRoute`, `ReviewRoute`, `SettingsRoute` (phần có trạng thái), không đổi các composable không trạng thái mà các test này dựng. `PosesScreenTest`, `PoseDetailScreenTest`: dòng trên.
- Nên xem: `DefaultFeatureFlagsTest` (7, giữ, thêm test mô tả), `InAppReviewTest` (6, chỉ đổi bản giả), `MainScreenTest` (5, dùng `FeatureFlag`, giữ), `FullScreenAdGuardTest` (5, lấy bản của khung), `AdUnitIdsTest` (5, bản của khung), `InterstitialFlowTest` (3, xóa cùng file), `LoadingViewModelTest` (2, giữ, đổi hàm dựng), `DefaultRemoteSettingsTest` (2, dòng trên), `NetworkDialogControllerTest.flagOff_neverShows` (giữ), `PrivacyOptionsButtonTest` (giữ), `RequestInAppReviewTest` (giữ, thêm test), các test chữ (`*StringsTest`) và ảnh chuẩn của màn (`*ScreenshotTest`, `SampleScreenTest`): giữ, plan không đổi chữ hay giao diện của màn.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Câu 1: lần dừng đầu bỏ, lần 2 cho, sau khi hiện chờ 3 lần (đọc hai khóa) | Unit test (lượt 1) | `InterstitialBreaksTest.gate_readsBothKeys_firstBreakSkipped_secondAllowed`: `FakeRemoteSettings` trả 2 và 3; `onBreak` lần 1 `null`, lần 2 có vé; `onShown(vé)`; lần 3, 4 `null`, lần 5 có vé. Đối chứng: đặt `ads_review_interstitial_first_break` là 1 thì lần 1 có vé |
| Câu 1: mặc định 2 và 3 | Unit test | `InterstitialBreaksTest.settings_defaultsAreTwoAndThree`: `REVIEW_FIRST_BREAK.default == 2L`, `REVIEW_BREAKS_BETWEEN.default == 3L`, tên đúng chuỗi của spec; đối chứng: `capOf(POSES_BANNER) == null` |
| Câu 1: không còn 120 giây | Unit test | `InterstitialBreaksTest.noTimeRule_secondAllowedRightAfterFirstShown`: hiện ở lần 2, lần 5 cho ngay dù đồng hồ không chạy (cổng thời gian chỉ còn `ads_interval` của khung) |
| Câu 1: vị trí bị luật chặn thì không gọi khung, báo `false` một lần | Compose UI test | `ShowInterstitialTest.gateSaysNo_reportsNotShownOnce_withoutCallingController`: `LocalInterstitialBreakGate` giả trả `null`, `LocalAdsController` là `null` cũng như có; `onFinished(false)` đúng một lần |
| Câu 1: luật chỉ cho rồi khung hiện thì `onShown` được gọi | Unit test | `InterstitialBreaksTest.onShown_movesTheLastShownBreak` (sau `onShown` ở lần 2, lần 3 `null`); phần gọi `onShown` khi `showInterstitial` trả `true` kiểm trong `BreakInterstitial` bằng hàm thuần `runBreakInterstitial(gate, show)` mà composable gọi: `BreakInterstitialTest.shown_callsOnShown_thenFinishesTrueAfterClose`, `notShown_finishesFalse_noOnShown` |
| Câu 2: `is_show_<mã>` của ba vị trí, mặc định tắt | Unit test | `AdsSettingsTest` của khung (mọi vị trí không phải toàn app có `is_show_<mã>` mặc định `false`); thêm `AdPlacementAppTest.appPlacements_declaredWithSpecFormats`: `POSES_BANNER` banner, `POSES_DETAIL_BANNER` banner, `REVIEW_INTERSTITIAL` interstitial, không `appWide`, `AdsSettings.placementEnabled(it).name == "is_show_${it.key}"` |
| Câu 3: không có SDK AppsFlyer trong mã nguồn | Unit test | `NoAppsFlyerSdkTest.noMainSource_importsTheSdk`: quét mọi `src/main/**/*.kt` của repo (như `AppsFlyerSdkUseTest` của mẫu, theo ký hiệu `import com.appsflyer`), danh sách rỗng; đối chứng: lần quét thấy ít nhất một file của `core/analytics/src/main/` |
| Câu 3: sink không làm gì | Unit test | `NoAppsFlyerSdkTest.sink_isNoOp`: `AnalyticsModule` gắn `NoOpAppsFlyerSink` (đọc nguồn `di/AnalyticsModule.kt` theo ký hiệu `bindAppsFlyerSink(impl: NoOpAppsFlyerSink)`); gọi `init`, `start`, `logEvent` không ném |
| Câu 3: không có thư viện AppsFlyer | Unit test | `NoAppsFlyerDependencyTest.noBuildFile_namesAppsFlyer`: `gradle/libs.versions.toml` không có bảng nào có `group = "com.appsflyer"`, không file `build.gradle.kts` nào có `libs.appsflyer`; đối chứng: tệp toml đọc được và có `play-services-ads` |
| Câu 3: không có khóa AppsFlyer, tracker không khởi động | Unit test | `NoAppsFlyerDependencyTest.appsFlyerConfig_isEmpty`: `AnalyticsConfigModule.appsFlyerConfig().enabled == false` |
| Câu 3: manifest gộp không có thành phần, quyền, `queries` của AppsFlyer | Unit test | `MergedManifestTest.mergedManifest_hasNoAppsFlyerStartupComponent`, `mergedManifest_hasNoAppsFlyerStorePermissions` (bản mẫu), và `mergedManifest_queriesAreExactlyTheAllowedSet` không có `com.instagram.android`, `com.facebook.lite`, `com.appsflyer.referrer.INSTALL_PROVIDER` (đo lại ở mục 4.1) |
| Manifest gộp chỉ có quyền đã duyệt | Unit test | `MergedManifestTest.mergedManifest_permissionsAreExactlyTheAllowedSet`: danh sách của mẫu cộng `android.permission.CAMERA`, `android.permission.WRITE_EXTERNAL_STORAGE` (kèm `MergedManifestTest.writeStorage_hasMaxSdk28`); tên gói `com.vglstudio.camera.pose.line.guide.staging.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` đo ở mục 4.1. Cùng file: `mergedManifest_containsActivityOfEveryNetwork`, `hasNoAppLovinInitProvider`, `hasNoMetaAudienceNetworkProvider`, `hasNoMintegralNetworkReceiver`, `hasNoAppLovinSdkKey`, `keepsVibratePermission`, `hasNoCheckLicensePermission`, `hasNoWifiStatePermission`, `keepsBackupOff` |
| Câu 4: không có màn xem thử | Unit test | `MergedManifestTest.mergedManifest_hasNoAdGalleryActivity`: không `activity` nào tên kết thúc bằng `.AdGalleryActivity`; đối chứng: có `MainActivity` |
| Câu 4: không có `SAMPLE_BOTTOM_BANNER` | Unit test | `AdPlacementAppTest.noSampleBottomBanner`: `AdPlacement.entries.none { it.key == "sample_bottom_banner" }`; `SampleScreenTest.noSampleBottomBanner`: đọc nguồn `SampleScreen.kt` theo ký hiệu `AdPlacement.SAMPLE_BOTTOM_BANNER`, không có |
| Câu 6, staging lượt 1: ba vị trí và hai khóa log | Unit test (testStaging) | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet`: tập (tên khóa, giá trị) của hai hàm của module đúng bằng `{is_show_poses_banner=true, is_show_poses_detail_banner=true, is_show_review_interstitial=true, is_log_ad_fullscreen_events=true, is_log_ad_slot_events=true}`; `noGalleryNorSamplePlacement_isShown` |
| Câu 6, staging lượt 2: thêm `show_open_ads`, `show_resum_ads`; không `show_open_ads_first_open`, `is_show_native_after_inter`, `is_show_native_bottom` | Unit test (testStaging) | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet` đổi tập mong đợi ở commit test của lượt 2 (cộng hai khóa); `firstOpenAndNativeCombos_areNotSet`: ba khóa không có trong tập; `everyLocalValue_isADeclaredKey` (bản mẫu: mọi khóa có trong `AdsSettings.all()`) |
| Giá trị cục bộ chỉ ở staging; Firebase thắng | Unit test | `LocalRemoteValuesSourceSetTest.onlyStagingSourceSet_declaresLocalRemoteValues` (bản mẫu); `DefaultRemoteSettingsTest` của mẫu (giá trị Firebase thắng giá trị cục bộ, cục bộ thắng mặc định) |
| Câu 6: lần mở đầu không có quảng cáo mở app | Unit test | Test của khung `StartupAdChoiceTest` (lần đầu đọc `show_open_ads_first_open`); thêm ở app `StagingRemoteValuesModuleTest.firstOpen_choosesNoStartupAd`: với tập giá trị cục bộ của staging và lần mở đầu, hàm chọn của khung (`startupAdChoice`, internal của `core/ads`, gọi qua một hàm công khai nhỏ nếu cần) trả không vị trí; đối chứng: lần mở thứ hai trả `open_app` |
| Câu 6: quảng cáo mở app hiện từ loading, sau khi xong việc, một lần | Unit test | `LoadingViewModelTest.tasksDone_withStartupAd_staysAt100_untilAdFinished_thenFinishedOnce`: `FakeStartupAds` trả `StartupAdTurn(OPEN_APP)`; trạng thái `InProgress(1f)`, `pendingAd` khác `null`; `onAdFinished(false)` hai lần thì `Finished` và một `loading_completed` |
| Câu 6: không có quảng cáo thì vào màn chính như cũ | Unit test | `LoadingViewModelTest.tasksDone_noStartupAd_finishesAtOnce`; `startupAdThrows_finishesWithoutAd_logsWarn` |
| Câu 6: loading tắt thì không hỏi `StartupAds` | Unit test | `LoadingViewModelTest.loadingOff_neverAsksStartupAds`: `FakeStartupAds.calls == 0` |
| Câu 6: chỉ hỏi sau khi xong việc và hết 2 giây | Unit test | `LoadingViewModelTest.startupAd_askedOnlyAfterTasksAndMinimum`: việc thứ ba lỗi thì `calls == 0`; thử lại xong thì `calls == 1` |
| Câu 6: route hiện quảng cáo một lần, dựng lại activity không hiện lại, kết thúc từ luồng nền vẫn điều hướng trên luồng chính | Robolectric | `LoadingRouteTest.pendingAd_isShownOnce_thenNavigatesAfterFinish`, `recreateWhileAdShows_doesNotShowAgain`, `finishFromBackgroundThread_navigatesOnMainThread` (bản mẫu, đổi bản giả theo `PreparationRepository` và `InternetCheck` của app) |
| Câu 6: ra nền trong lúc chờ quảng cáo rồi quay lại (L-091) | Robolectric | `LoadingRouteTest.backgroundWhileWaiting_thenResumed_showsOnce`: `FakeStartupAds` chờ một `CompletableDeferred`; `moveToState(CREATED)`, hoàn tất lượt, `moveToState(RESUMED)`: `showAd` đúng một lần sau khi resumed, không lần nào khi ở `CREATED`; `onFinished` một lần |
| Câu 6: quảng cáo quay lại app không hiện ngay sau hộp thoại quyền, chia sẻ, mở link | Robolectric (lượt 2) | `LeaveAppActionsTest.launcher_holdsBeforeLaunch_releasesOnFirstLineOfResult`: `AdsHostContent` với `ResumeSuppression`, `ForegroundAdSuppressor` thật của khung; `suppressed` là `true` từ ngay trước `launch` tới dòng đầu của callback; `launch` ném thì vẫn nhả. `startOutsideApp_marksLeave_onlyWhenStarted`: mở được thì dấu rời app bật; `ActivityNotFoundException` thì trả `false`, không có dấu. `noController_justOpens`: không có `AdsHost` thì mở như cũ |
| Câu 6: hộp đánh giá giữ chặn | Robolectric (lượt 2) | `RequestInAppReviewTest.request_holdsAdsUntilDone`: `InAppReview` giả chạy `onDone` sau một bước; chặn bật trong lúc đó, nhả sau `onDone`, kể cả khi `ask` ném |
| Câu 6: popup mạng chờ quảng cáo toàn màn hình | Unit test (lượt 2) | `NetworkDialogControllerTest.lostWhileAdShowing_dialogWaits_thenShowsWhenAdCloses`; `adStartsWhileDialogShown_hidesThenShowsAgainOnClose`; `adCloses_withoutLoss_staysHidden`; `inactiveWhileWaiting_dropsPending` (đối chứng: không có quảng cáo thì hiện ngay như cũ) |
| Câu 6: popup mạng nối từ vỏ app | Robolectric (lượt 2) | `AppAdsRootTest.fullScreenAdShowing_isReportedToNetworkDialog`: hàm composable `AppAdsRoot(ads, networkDialog, inAppReview, breakGate, content)` tách từ `MainActivity`, với `StateFlow` giả của `fullScreenAdShowing`; đổi `true` rồi `false` thì bản giả của controller nhận đúng hai lần gọi |
| Vỏ app: `AdsHost`, cổng đếm và đánh giá có mặt, sau khi dựng lại activity (L-061) | Robolectric (lượt 1) | `AppAdsRootTest.providesBreakGateAndInAppReview_afterRecreate`: nội dung đọc được `LocalInterstitialBreakGate.current`, `LocalInAppReview.current` khác `null`, cả sau `ActivityScenario.recreate()` |
| Resume đè lên camera, đóng thì camera mở lại | Unit test có sẵn | `CameraViewModelTest.stoppedThenStarted_closesThenReopensWithSameTools` (app open che activity, `onStop` rồi `onStart`); không đổi code camera |
| Poses KB-6: lỗi tải thì không chừa khoảng trống | Robolectric (lượt 1) | Năm hàm của `AdSlotTest` ở mục 3.5, chạy với `@GraphicsMode(NATIVE)`, đo chiều cao qua `testTag` của phần tử bọc |
| Camera KB-9: chưa lưu thì không hiện; lưu rồi thì gọi quảng cáo | Unit test có sẵn | `ReviewExitTest` (`leaveReview`, `afterReviewAd`) giữ nguyên |
| Popup mạng không chồng quảng cáo; khung theo `InternetConnectivity` | Unit test (lượt 1) | `DefaultInternetConnectivityTest.connected_followsSource` (bản mẫu, nguồn giả của app) |
| Mọi test và ảnh chuẩn của khung qua trong app | Unit, Robolectric, screenshot | `scripts/check.sh` ở lượt 1: toàn bộ test lấy từ mẫu (118 file test của `core/ads`, test của `core/analytics`) qua; 41 ảnh ghi lại, `git status --short -- '*/src/test/screenshots/'` chỉ đổi ảnh dưới `core/ads/src/test/screenshots/` |
| Bảng khóa có, khớp code (lượt 3) | Unit test | `RemoteConfigKeysTest` (bản mẫu và tập khóa đặt tên trước ADR-0023); `keysFile_matchesCode`, `templateFile_matchesCode`, `everyKeyName_isUniqueAcrossModules`, `everyFeatureFlagAndPlacement_isListed`, `everyPlacementShowKey_isInTableOff`, `networkKeys_areListed`, `analyticsKeys_areListed`; thêm `appModuleKeys_areListed` (`:core:poses`, `:core:review`, `InterstitialBreakSettings.all()` có mặt) |
| `scripts/ads-plan.py` chạy được trên app (lượt 3) | Test của script | `scripts/tests/run.sh`, khối `ads-plan`: hai test đọc code thật ("every placement of this repo is read", "this repo lists its app-wide placements") chạy, không còn dòng `skip - ads-plan: repo chưa có docs/remote-config-keys.md` |
| R8 bản staging release | Build | `./gradlew :app:assembleStagingRelease` qua ở lượt 1 (mục 4.1) và trong `pr-check` (PR đổi thư viện) |
| Banner ở thư viện và chi tiết, interstitial khi rời màn xem lại theo luật 2 và 3 | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging sau lượt 2: đồng ý quảng cáo; thư viện và chi tiết có banner thử của Google ở đáy, nút chính không bị che; lưu ảnh 1 rồi rời màn xem lại: không có interstitial; ảnh 2: có (log `VGL.Ads`); ảnh 3, 4: không; ảnh 5: có nếu đã qua 45 giây. Không bấm quảng cáo |
| Quảng cáo mở app từ lần mở thứ hai | Kiểm tra thủ công (máy thật: quảng cáo thật) | Xóa dữ liệu, mở app: loading rồi màn chào, không có app open (log `startup ad: key off`); tắt app hẳn, mở lại: loading tới 100%, app open thử, đóng thì vào camera; tắt `feature_loading_enabled` trên Firebase staging: không có app open |
| Quảng cáo quay lại app ở thư viện và camera; camera mở lại; không có sau hộp thoại quyền, chia sẻ | Kiểm tra thủ công (máy thật: camera thật) | Ở thư viện: Home, mở lại: app open thử (log `Resume ad SHOW: resume`); ở camera: Home, mở lại: app open, đóng thì khung camera chạy lại; xóa quyền camera, mở app, cho quyền: không có app open (`Resume ad SUPPRESSED`); chia sẻ ảnh rồi quay lại: không có; lần quay lại sau đó: có |
| Popup mạng trong lúc có quảng cáo toàn màn hình | Kiểm tra thủ công (máy thật: quảng cáo thật) | Mở app open, bật chế độ máy bay khi quảng cáo đang hiện: popup không đè quảng cáo; đóng quảng cáo: popup hiện |
| Firebase staging không ghi đè giá trị cục bộ trái ý | Kiểm tra thủ công (máy thật: Remote Config) | Trước khi phát staging của lượt 2: người xem các khóa ở mục 3.1, "Firebase staging" trên console Firebase staging, ghi kết quả vào danh sách chạy |
| Chữ spec, ADR, tài liệu | Kiểm tra thủ công (người: đọc tài liệu) | Nhật đọc các spec `changed` trên nhánh báo cáo và ADR-1007, xác nhận lại |

**Ngoại lệ viết test trước:**

- Phần lấy nguyên từ nhánh take (`core/ads/`, `core/analytics/`) và test đi kèm: test của mẫu có trước code trong cùng nhánh; ở bước 3 của lượt 1, commit test gồm các test mới của app ở bảng trên, chạy trên nhánh take đã sửa đủ để biên dịch (core/config, chỗ gọi), và phải đỏ đúng lý do (ví dụ `NoAppsFlyerSdkTest` đỏ vì `AppsFlyerSink.kt` còn import SDK; `StagingRemoteValuesModuleTest` đỏ vì chưa có module).
- 41 ảnh chuẩn của `core/ads` ghi lại: không đỏ trước được ngoài lý do theme; xem bằng mắt (mục 3.1).
- `lint.xml`, `libs.versions.toml`, `settings.gradle.kts`, `consumer-rules.pro`: cấu hình build, kiểm bằng `scripts/check.sh` và `assembleStagingRelease`.
- Lượt 3: `docs/` sinh từ code và tài liệu.

### 4.1. Chạy thử

- **Đã chạy thử:** một phần, ngày 2026-10-28 (Kết quả bên dưới). Trước đó: Lần chạy thử cần nhánh `template-sync/take-core-analytics-0444fbe` mà người làm ở bước 1 tới 4 của mục 3.1; nhánh chưa có lúc soạn plan (2026-10-28). Đã làm khi soạn plan, không cần nhánh: xuất cây của mẫu ở `0eaeadb` và `0444fbe` theo tên gói của app vào thư mục tạm và so với app (danh sách file app đã sửa trong `core/ads`, `core/config`, `app/`; lời gọi chéo module của `core/ads`: `AdFunnel`, `AdMetrics`, `AppsFlyerStartup`, `RemoteKey`, `RemoteSettings`, `AppTheme`, `Spacing`, `PrimaryButton`, `AppLogger`, `InternetConnectivity`; API của `PrimaryButton`, `Spacing` của app khớp); tra quyền trong manifest AAR của các SDK ở bộ đệm Gradle của máy dev (mục 3.3).
- **Lệnh** (người chạy trên máy dev, trước khi duyệt, trên nhánh tạm từ nhánh take với phần lượt 1 viết nháp, không commit; ghi kết quả vào đây):
  - `scripts/local-setup.sh --check`, `source` tệp `env.sh` mà script in ra.
  - `./gradlew :app:processStagingDebugMainManifest`, rồi đọc `app/build/intermediates/merged_manifest/stagingDebug/processStagingDebugMainManifest/AndroidManifest.xml`: danh sách `uses-permission` và `queries` (so với nhánh chính, chỉ khác `VIBRATE`), có lỗi `allowBackup` không (mục 3.4).
  - `./gradlew :core:ads:testDebugUnitTest :core:analytics:testDebugUnitTest :core:config:testDebugUnitTest` để thấy các test của khung chạy với theme và `core/config` của app.
  - `scripts/check.sh`.
  - `./gradlew :app:assembleStagingRelease` (R8, L-053).
  - Trong môi trường `claude --cloud` của app: `curl -sI https://artifact.bytedance.com/repository/pangle/ https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea/` (L-067 của mẫu). Bị chặn thì phiên lượt 1 vẫn chạy được (`local`), nhưng mọi phiên `cloud` và `routine` sau khi merge không tải được hai SDK: người thêm hai máy chủ vào chính sách mạng của môi trường trước khi duyệt (mục 8, câu 12).
- **Kết quả (2026-10-28, AI có Nhật theo dõi, máy dev, phần nhỏ; Nhật chọn để `scripts/check.sh`, test của khung và R8 cho lượt 1):**
  - **Nhánh take:** `template-sync/take-core-analytics-0444fbe`, commit cuối `9edbacedb0e1a672614a7f1588a3c52ea69bbb29`, đã đẩy, không PR. Ba commit: tách `hold=core` thành tám dòng; `--take core/ads` (248 file, xung đột ở `build.gradle.kts`, `AdPlacement.kt`, `AdSlot.kt`, `AdsController.kt`, `AdsSettings.kt`, `AdUnitIdsTest.kt`, đều lấy bản mẫu); `--take core/analytics` (25 file, không xung đột). Mỗi lần chỉ đổi file trong module đó và bỏ đúng một dòng `hold=`; `docs/foundation.md` không đổi. Script chạy từ một worktree của dự án mẫu ở `origin/main` (`5dc79f2`, script giống `0444fbe`); app cần `.claude/` trong `.git/info/exclude` vì script từ chối khi có tệp chưa theo dõi.
  - **Nháp dùng để chạy thử, không commit:** chín adapter và `unity-ads` vào `gradle/libs.versions.toml`, hai kho Maven vào `settings.gradle.kts` (theo `0444fbe`), bỏ `implementation(libs.appsflyer)` ở `core/analytics/build.gradle.kts`. Kotlin chưa biên dịch (không cần cho bước gộp manifest).
  - **Manifest gộp** (`:app:processStagingReleaseMainManifest`, so với nhánh chính `dbcb588` cùng lệnh): `uses-permission` chỉ thêm `android.permission.VIBRATE` (AppLovin), đúng mục 6 của spec ads. Không có quyền nào của AppsFlyer; `BIND_GET_INSTALL_REFERRER_SERVICE` đã có ở nhánh chính. `android:allowBackup="false"` giữ nguyên, không lỗi gộp: phương án dự phòng của mục 3.4 không cần.
  - **`queries` thêm nhiều mục** (mới thấy, plan chưa nêu): gói `com.facebook.katana` (Meta Audience Network); intent `com.attribution.REFERRAL_PROVIDER` và tám gói chợ ứng dụng (`com.android.vending`, `com.google.market`, `com.amazon.venezia`, `com.sec.android.app.samsungapps`, `com.xiaomi.market`, `com.huawei.appmarket`, `com.oppo.market`, `com.bbk.appstore`); intent `android.intent.action.MAIN` (thấy mọi app có màn chính) và năm gói dò root (`com.topjohnwu.magisk`, `eu.chainfire.supersu`, …); `com.digitalturbine.ignite.cl.IgniteRemoteService` và `VIEW` chung (DT Exchange); `CustomTabsService`, `VIEW` `http`/`market` (Unity, InMobi). Nguồn theo báo cáo gộp manifest: AppLovin, Meta, DT Exchange (`com.fyber:marketplace-sdk`), InMobi, Unity. Đây không phải quyền nhưng là khai báo thấy gói khác, Play xét theo chính sách Package visibility và có thể hỏi ở bài khai An toàn dữ liệu. Câu hỏi 14 ở mục 8.
  - **Hai kho Maven từ máy dev:** tải được (`pag-apm-2.0.0.11.pom` của Pangle và `mbridge_android_sdk-17.1.81.pom` của Mintegral trả 200; gốc thư mục trả 403 và 404 là bình thường với kho Maven). Môi trường `claude --cloud` chưa kiểm (câu hỏi 12 vẫn mở cho phần cloud).
  - **Chưa chạy:** `scripts/check.sh`, `:core:ads:testDebugUnitTest` và các test khác, `:app:assembleStagingRelease` (R8). Lượt 1 chạy các lệnh này ở bước 5; lỗi R8 hay test của khung không qua với `core/config` của app sau ba lần sửa thì dừng theo mục 7.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: chuyển ba vị trí, thêm `open_app`, `resume`, đổi luật tần suất và khóa), 2 (chín adapter mediation, Unity Ads), 3 (`VIBRATE`), 4 (sự kiện phân tích của khung, hai khóa dữ liệu trên máy), 5 (thứ tự khởi tạo: `ads.start` trong `Application`, consent shortcut), 7 (`core/ads`, `core/analytics`, `core/config`, `core/network`, `core/poses`, `core/review`, `gradle/libs.versions.toml`, `settings.gradle.kts`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Sáu module core, bốn feature, `:app`, cấu hình build; khoảng 37 nghìn dòng chép từ mẫu |
| B. Độ rõ của yêu cầu | 1 | Spec ads đã xác nhận, sáu câu đã trả lời; còn vài điểm AI phải đề xuất (mục 8: vị trí `gallery_*`, `VIBRATE`, KB-9 tiêu chí 4, thời gian loading) |
| C. Khả năng kiểm chứng tự động | 3 | Quảng cáo thật, vòng đời app (quay lại app, ra nền khi chờ), Firebase staging thật chỉ kiểm trên máy |
| D. Ảnh hưởng chéo | 3 | Đổi API `RemoteSettings` mà sáu module dùng; vỏ app; popup mạng; mọi màn có thể bị quảng cáo quay lại app đè |
| E. Độ mới của kỹ thuật | 2 | Khung đã chạy ở mẫu nhưng lần đầu ở app; mediation, app open, chặn theo bộ đếm chưa có ở app |
| **Tổng** | 12 | **Mức: Cao (chạm điểm chặn 1, 2, 3, 4, 5, 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `RemoteSettings` (mọi module core đọc tham số), `AdSlot`, `PrivacyOptionsButton`, `rememberShowInterstitial` (feature poses, camera, settings, sample), `NetworkDialogController`, `MainActivity`, `TemplateApplication`, `LoadingViewModel`. `release.sh` (giữ ở `0eaeadb`) đọc `AdPlacement.kt`: dòng mới có thuộc tính (`OPEN_APP("open_app", AdFormat.APP_OPEN, appWide = true),`) và vị trí `gallery_*`; bản production chưa phát được tới khi T-075 xong (T-075 phụ thuộc T-074). Không phát production giữa T-074 và T-075.
- Phần chung của mẫu bị sửa ở app (đề xuất về dự án mẫu theo `docs/ops/from-app.md`, mục 2, ghi trong mô tả PR của từng lượt): `AdSlot(..., separator)` và thu khung khi lỗi tải; overload `PrivacyOptionsButton(content)`; `rememberShowInterstitial(placement, onFinished: (Boolean) -> Unit)` có cổng đếm; `rememberAdHoldingLauncher`, `rememberStartOutsideApp`; `NoOpAppsFlyerSink` (cách chạy khung không có AppsFlyer); `setFullScreenAdShowing` theo kiểu `StateFlow`. `InterstitialBreakSettings` và ba dòng vị trí là riêng của app.
- Task khác có thể xung đột: T-075 (`release.sh` mới, phụ thuộc T-074); T-076 (`VALIDATED` trong `ConnectivitySource`, đổi điều `InternetConnectivity` báo cho `core/ads`; chạy sau T-074 thì không xung đột file, chỉ cần test của T-076 kiểm cả `InternetConnectivity`); T-077 (`onAppForeground` của popup mạng, cùng sửa `NetworkDialogController.kt` và `MainActivity.kt`: chạy sau T-074, hoặc ghi rõ một plan Tạo, plan kia Sửa). Không plan `approved` nào khác đang sửa `core/ads`, `core/config`.
- Cách mở phiên: `open_with: local` (máy của dev có cả hai repo): lượt 1 và 2 chép tay từ mẫu (`cross-repo-read`: `cloud`, `routine` ghi `không`). Plan không sửa `.github/workflows/` nên không cần `workflow-push`.
- Hạn mức Actions: lượt 1 build bản release trong `pr-check` và chạy `ui-test` (code chạy trong app), lượt 2 cũng vậy; dự kiến hai lượt CI dài.
- Đề xuất thứ tự chạy: ba lượt liền nhau, cuối danh sách (chuỗi phụ thuộc), sau các task độc lập. Người làm nhánh take ngay trước khi phiên bắt đầu, để `git merge origin/main` ở lượt 1 ít xung đột.

## 7. Điều kiện dừng riêng

- Nhánh `template-sync/take-...` chưa có, hay commit cuối khác commit ghi trong danh sách chạy: dừng trước lượt 1.
- `git merge origin/main` vào nhánh take có xung đột ngoài `docs/template-sync.txt`, `docs/foundation.md`: dừng, vì phần gộp xung đột là việc người làm ở bước 2 của mục 3.1.
- Manifest gộp có quyền ngoài danh sách đã duyệt ở mục 3.3 (sau khi spec có `VIBRATE`): dừng (điều kiện 2 của `risk-scoring.md`).
- Bất kỳ file nào ngoài `core/analytics/src/main/` cần `import com.appsflyer`, hay build cần thư viện AppsFlyer để biên dịch: dừng, không thêm thư viện.
- Ảnh chuẩn của `core/ads` ghi lại mà khác bản mẫu ở bố cục (vị trí, kích thước), không chỉ ở chữ và màu theo theme: dừng, ghi tên ảnh.
- Test của khung (lấy từ mẫu) đỏ vì hành vi, không vì dữ liệu của app (ba dòng vị trí): dừng, không sửa test của khung.

## 8. Câu hỏi cho người duyệt

1. **Vị trí `gallery_*` và `sample_native_small` của khung.** 35 file test của `core/ads` dùng chúng (`AdPlacementTest` đòi vị trí `gallery_*` phủ mọi định dạng). Đề xuất: giữ nguyên trong `AdPlacement`, không màn nào gọi, staging không bật, ghi ở ADR-1007; `release.sh` mới (T-075) bỏ qua `gallery_*` và dùng mã thử cho `sample_native_small` (`docs/ops/release.md` của mẫu, dòng 72). Phương án khác: bỏ `SAMPLE_NATIVE_SMALL` và đổi 49 chỗ dùng trong 17 file test sang `GALLERY_NATIVE_SMALL` (sửa test của khung, xa mẫu hơn), hoặc xóa các test đó như `docs/ops/new-app.md` của mẫu gợi ý (mất độ phủ).
   - Trả lời (Nhật, 2026-10-28): theo đề xuất: giữ `gallery_*` và `sample_native_small` trong `AdPlacement`, không màn nào gọi, staging không bật.
2. **Staging bật ba vị trí của app bằng giá trị cục bộ** (`is_show_poses_banner`, `is_show_poses_detail_banner`, `is_show_review_interstitial`), ngoài các khóa câu 6 nêu. Đề xuất: bật, để staging có quảng cáo thử ở ba vị trí như bản mẫu, không phụ thuộc Firebase staging; Firebase staging vẫn tắt được từng vị trí.
   - Trả lời (Nhật, 2026-10-28): theo đề xuất: staging bật ba vị trí của app bằng giá trị cục bộ.
3. **Camera KB-9, tiêu chí 4** ("Khi ở camera và trong lúc xem lại thì không có quảng cáo nào trên màn hình") trái câu 6 (quảng cáo quay lại app có thể đè camera). Đề xuất: Nhật sửa tiêu chí thành "không có vị trí quảng cáo nào của màn camera và màn xem lại; quảng cáo toàn app khi quay lại app có thể phủ lên (mục 5)" trước khi duyệt plan, để phiên không gặp mâu thuẫn (điều kiện dừng 3). Cùng lúc: các spec `changed` ở lượt 3 (khóa mới ở KB-6, KB-9, mục 8; mục 6) Nhật xác nhận sau phiên.
   - Trả lời (Nhật, 2026-10-28): sửa spec trước khi duyệt plan, PR #222 (camera KB-9 tiêu chí 4).
4. **Quyền `VIBRATE`** (AppLovin, quyền thường, không cần hỏi người dùng, không thuộc mục An toàn dữ liệu). Đề xuất: Nhật thêm vào `features/ads/spec.md` mục 6 câu "Quyền mà SDK quảng cáo kéo vào: `VIBRATE` (AppLovin rung mà không kiểm quyền; gỡ thì app có thể dừng đột ngột khi hiện quảng cáo AppLovin)" và xác nhận lại spec trước khi duyệt. Phương án khác: bỏ adapter AppLovin (đổi câu của spec về tám mạng, giảm doanh thu).
   - Trả lời (Nhật, 2026-10-28): ghi `VIBRATE` vào mục 6 của spec ads, PR #222. Không bỏ adapter AppLovin.
5. **Khóa số đặt tên trước ADR-0023** (`in_app_review_min_saved_photos`, `in_app_review_min_days_between`, `pose_match_threshold`) không có đuôi đơn vị mà `RemoteConfigKeysTest.everyNumberKey_endsWithUnit` của mẫu đòi. Đề xuất: giữ tên (đã có trên Firebase staging), test có tập ngoại lệ ghi rõ ba tên; khóa mới vẫn theo quy ước.
   - Trả lời (Nhật, 2026-10-28): theo đề xuất: giữ ba tên khóa cũ, test có tập ngoại lệ ghi rõ ba tên.
6. **Thời gian của màn loading.** Mục 5 của spec loading ghi thời gian chờ quảng cáo "cộng sau thời gian tải"; khung tính hạn chót từ lúc mở app (tối đa 40 giây kể từ lúc mở, không cộng thêm). Đề xuất: giữ cách của khung, sửa câu của spec ở lượt 3.
   - Trả lời (Nhật, 2026-10-28): theo cách của khung; câu của spec loading đã sửa ở PR #222, không đợi lượt 3.
7. **`FullScreenAdHost`** (native toàn màn hình) không đặt vào `MainActivity` vì app không có vị trí native toàn màn hình và combo tắt. Bật `is_show_native_after_inter` trên Firebase thì combo không hiện được (khung ghi `NOT_READY`). Đề xuất: không đặt; ticket bật combo đặt sau. Phương án khác: đặt luôn như mẫu (vô hại khi không có lượt).
   - Trả lời (Nhật, 2026-10-28): theo đề xuất: không đặt `FullScreenAdHost`.
8. **`app/lint.xml`** bỏ qua `NotificationPermission` chỉ cho lớp `com.squareup.picasso.` (InMobi kéo vào, không ai gọi). `CLAUDE.md` cấm tắt quy tắc lint để test qua; đây là quyết định của mẫu ở T-034, phạm vi một gói. Đề xuất: nhận, ghi trong ADR-1007.
   - Trả lời (Nhật, 2026-10-28): theo đề xuất: nhận `app/lint.xml`, ghi trong ADR-1007.
9. **Khóa trên Firebase production** (`is_show_*` thay `ads_*_enabled`, theo câu 2) là việc của người trước khi phát production (T-075); T-074 không chạm cấu hình production. Đúng không?
   - Trả lời (Nhật, 2026-10-28): đúng: khóa trên Firebase production là việc của người trước khi phát production (T-075).
10. **Test xóa theo plan:** `InterstitialBreaksTest.afterShown_waitsMinInterval`, `allowed_onlyWithConsentFlagAndNoNetworkDialog`, cả `InterstitialFlowTest`, `AppsFlyerSdkUseTest` (từ take). Đề xuất: người duyệt đồng ý ở mục 9 rằng các test này xóa vì hành vi bị bỏ theo câu 1, câu 3 của spec, không phải để làm test qua.
   - Trả lời (Nhật, 2026-10-28): theo đề xuất: các test đó xóa vì hành vi bị bỏ theo câu 1, câu 3 của spec, không phải để làm test qua. Người duyệt ghi lại ở mục 9 khi duyệt.
11. **Interstitial và popup mạng.** Bộ cũ không mở interstitial khi popup mạng đang hiện; khung chỉ theo `InternetConnectivity` (mất mạng thì không nạp; quảng cáo đã nạp vẫn có thể hiện), còn popup chờ quảng cáo đóng (lượt 2). Đề xuất: nhận hành vi của khung.
   - Trả lời (Nhật, 2026-10-28): theo đề xuất: nhận hành vi của khung.
12. **Hai kho Maven trong môi trường cloud.** Phiên `cloud` và `routine` của app sau khi merge lượt 1 cần tải từ `artifact.bytedance.com` và `dl-maven-android.mintegral.com`. Người kiểm (mục 4.1) và thêm vào chính sách mạng nếu bị chặn, trước khi duyệt.
   - Trả lời (Nhật, 2026-10-28): theo đề xuất: người kiểm ở mục 4.1, thêm vào chính sách mạng của môi trường cloud nếu bị chặn, trước khi duyệt.
13. **Chạy thử (mục 4.1)** cần nhánh take; đề xuất Tuấn làm nhánh take và chạy các lệnh của mục 4.1 trước họp duyệt, ghi kết quả, rồi mới duyệt.
   - Trả lời (Nhật, 2026-10-28): AI có Nhật theo dõi làm nhánh take và chạy thử phần nhỏ của mục 4.1; `scripts/check.sh`, test của khung và R8 để lượt 1.

14. **`queries` mà các SDK mediation kéo vào** (mục 4.1, Kết quả): chợ ứng dụng, Facebook, intent `MAIN` (thấy mọi app có màn chính), gói dò root, DT Ignite. Đề xuất: nhận như dự án mẫu đang có, ghi vào mục 6 của spec ads cùng `VIBRATE`, và người kiểm phần Package visibility khi khai Play Console trước production. Phương án khác: gỡ từng mục bằng `tools:node="remove"` trong manifest của `:app` (SDK có thể hỏng chức năng đo hay chống gian lận; cần chạy thử từng mục).
   - Trả lời (Nhật, 2026-10-28): nhận; danh sách ghi vào mục 6 của spec ads (PR spec riêng), người kiểm Package visibility khi khai Play Console trước production.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
