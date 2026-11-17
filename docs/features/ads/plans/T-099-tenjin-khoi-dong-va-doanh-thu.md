---
ticket: T-099
title: Thêm SDK Tenjin vào :core:analytics, khởi động sau đồng ý khi build có khóa, gửi doanh thu từng lượt quảng cáo
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn: để trống
risk_level: high             # low | medium | high
token_estimate: 15.4M        # cỡ M theo scripts/token-usage.py estimate (2026-11-17), không nâng tay
blocking_points: [sdk, data, consent, core]
run_order:                   # AI điền khi lập danh sách chạy
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy
---

# PLAN T-099: THÊM SDK TENJIN, KHỞI ĐỘNG SAU ĐỒNG Ý, GỬI DOANH THU QUẢNG CÁO

## 1. Tóm tắt

Thêm SDK Tenjin `com.tenjin:android-sdk:2.0.2` vào `:core:analytics` theo ADR-1010. Một lớp theo dõi Tenjin mới nằm cạnh `AppsFlyerTracker`: khởi động cùng lúc khung khởi động AppsFlyer (qua một lớp chia `AppsFlyerStartup` cho cả hai), áp đồng ý của UMP trước mỗi lần kết nối và trước mỗi lượt gửi, chỉ kết nối và gửi khi `:core:ads` đã báo "đồng ý đã rõ" (spec ads câu 21 ý i), và gửi doanh thu của mỗi lượt AdMob có doanh thu qua hàm doanh thu AdMob của Tenjin. `:core:ads` chỉ thêm tín hiệu đồng ý đã rõ, qua interface `AttributionConsent` của `:core:analytics`. Kiểm bằng unit test trên bản giả của SDK, test quét mã nguồn, test manifest gộp có sẵn và chạy thử `scripts/check.sh`, `assembleStagingRelease`.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-T1 | Khi người dùng cài app từ một chiến dịch và mở app lần đầu, với bản phát hành có khóa Tenjin, thì Tenjin ghi một lượt cài gắn với chiến dịch đó | App gọi `connect()` của Tenjin sau đồng ý; SDK tự đọc Install Referrer và gửi. Code kiểm được: có khóa, sau `start()` thì gọi kết nối đúng một lần cho lượt mở, rồi mỗi lần app quay lại foreground. Ghi nhận lượt cài trên Tenjin là việc kiểm trên máy thật với khóa thử (T-101) |
| KB-T1 | Khi một quảng cáo hiện và mạng quảng cáo báo doanh thu của lượt hiện, thì Tenjin nhận doanh thu của đúng lượt đó, một lần, không nhân đôi | Mỗi `AdMetricEvent.ImpressionRevenue` của AdMob có `value > 0` gọi hàm doanh thu AdMob của Tenjin đúng một lần; lượt dự phòng giá trị 0 không gửi (câu 21 ý a); lượt MAX chưa gửi (MAX chưa bật, spec ads câu 16; câu 8.2 của plan) |
| KB-T1 | Khi một sự kiện phễu quảng cáo xảy ra ... thì Tenjin nhận sự kiện đó, trừ khi công tắc từ xa của sự kiện đang tắt | Phần phễu thuộc T-100. Ticket này chỉ làm công tắc của doanh thu `is_log_tenjin_ad_revenue` |
| KB-T1 | Khi người dùng mở app lần đầu ở vùng cần hỏi đồng ý và chưa trả lời hộp thoại (kể cả khi hết 10 giây chờ), thì app chưa gửi gì sang Tenjin; đồng ý thì bắt đầu gửi, từ chối thì không | `:core:ads` gọi `AttributionConsent.onConsentKnown()` đúng một lần khi `attributionConsentKnown(finished, gateState)` thành `true`: vòng hỏi xong và chốt (`finished && settled`, gồm từ chối và "không cần hỏi"), hoặc đường `CACHED_EARLY` (còn giữ đồng ý của phiên trước). Đường hết 10 giây, `ERROR_ALLOWED` (lỗi mạng, cho tạm), `INTERRUPTED_REGATHER` không phải đồng ý đã rõ. `TenjinTracker` chỉ kết nối và gửi khi đã có cả `start()` lẫn `onConsentKnown()` |
| KB-T1 | Khi người dùng ở vùng cần hỏi đồng ý từ chối cho phép theo dõi, thì app không gửi gì sang Tenjin | Trước mỗi lần kết nối và mỗi lượt doanh thu, gọi `optInOutUsingCMP()`; trả `false` (chuỗi TCF có mà mục đích 1, lưu và truy cập thông tin trên máy, không đồng ý) thì lớp theo dõi không gọi `connect()` và không gửi lượt đó. Không dựa vào chế độ opt out của SDK: ở chế độ đó SDK vẫn gửi lượt kết nối kèm mã cài đặt, nguồn cài và cờ `opt_out` (đọc bytecode `ConsentParamFilter`, mục 4.1). Không có chuỗi TCF (ngoài vùng hỏi đồng ý) thì Tenjin tự cho gửi (đọc bytecode `GDPRHelper.d()` của 2.0.2, mục 4.1) |
| KB-T1 | Khi bản build không có khóa Tenjin, thì app không gửi gì sang Tenjin và vẫn chạy như hôm nay | Khóa rỗng: không gọi SDK nào, kể cả `getInstance`; một dòng log `Tenjin off: no SDK key in this build` |
| KB-T1 | Khi Tenjin lỗi hay không trả lời, thì quảng cáo, số liệu Firebase và mọi màn của app vẫn chạy như hôm nay | Mọi lời gọi SDK bọc `try/catch`, ghi log theo tên lớp lỗi; `FanOutAdMetrics` đã cô lập từng đích; lớp chia khởi động cũng cô lập từng đích |

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện).

ADR liên quan: ADR-1010 (proposed, Tenjin), ADR-1007 (AppsFlyer tắt, giữ `NoOpAppsFlyerSink`), ADR-0027 (vị trí SDK số liệu trong `:core:analytics`, khởi động sau đồng ý), ADR-0019 (log), ADR-0046 (khóa Remote Config), ADR-0001 (phụ thuộc module).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu `AppsFlyerTracker` của `:core:analytics` (sink sau interface, config từ `:app`, công tắc đọc ở mỗi sự kiện). Mọi lớp mới ở gói `com.vglstudio.camera.pose.line.guide.core.analytics`.

1. **`TenjinConfig(sdkKey: String)`** (public, như `AppsFlyerConfig`): `enabled = sdkKey.isNotBlank()`; `toString()` không in khóa (`TenjinConfig(sdkKey=set|none)`).
2. **`TenjinSink`** (internal interface), bốn hàm: `init(sdkKey: String)`, `applyConsent(): Boolean`, `connect()`, `adMobImpression(payload: Map<String, Any>)`. **`TenjinLibSink`** là file duy nhất import `com.tenjin.android`:
   - `init`: `TenjinSDK.getInstance(applicationContext, sdkKey)`, rồi `setAppStore(TenjinSDK.AppStoreType.googleplay)`.
   - `applyConsent`: trả `optInOutUsingCMP()` (SDK tự gọi `optIn()` hay `optOut()`).
   - `connect`: `connect()`.
   - `adMobImpression`: `eventAdImpressionAdMob(JSONObject(payload))`. `org.json` chỉ dùng ở đây, vì unit test JVM của module không có Robolectric (`org.json` là bản rỗng).
3. **`tenjinAdMobPayload(event: AdMetricEvent.ImpressionRevenue): Map<String, Any>?`** (internal, hàm thuần, file `TenjinEvents.kt`): trả `null` khi `event.platform != AdMetricPlatform.ADMOB`, `event.value <= 0.0` hay `event.currency` rỗng. Còn lại trả `ad_unit_id` = `adUnitName`, `currency_code` = `currency`, `value_micros` = `Math.round(value * 1_000_000)` (Long), `mediation_adapter_class_name` = `adapterClassName` khi khác `null`. Trường theo tài liệu AdMob của Tenjin (chỉ `value_micros` bắt buộc); `response_id` và `precision_type` không có trong sự kiện của khung nên không gửi (câu 8.1).
4. **`TenjinSettings`** (public object, như `AppsFlyerSettings`): `IS_LOG_TENJIN_AD_REVENUE = RemoteKey.boolean("is_log_tenjin_ad_revenue", default = true, description = "Sends the revenue of every paid AdMob impression to Tenjin. Tenjin event switch (ADR-1010). Name set by the app, not an AdsKit key.")`; `all()`, `switches()`. Mô tả kết thúc bằng "Name set by the app, not an AdsKit key." như khóa AppsFlyer. T-100 thêm mười khóa của phễu vào đây.
5. **`TenjinSwitches`** (internal, như `AppsFlyerSwitches`): `stateIn` từng khóa của `TenjinSettings.switches()`, `isOn(key)` đọc giá trị mới nhất ở mỗi sự kiện.
6. **`AppForeground`** (internal interface): `fun onEachReturn(listener: () -> Unit)`; gọi `listener` mỗi lần process quay lại foreground. Quyết định đếm nằm trong **`ForegroundCounter`** (internal, thuần Kotlin): `onActivityStarted(): Boolean` trả `true` khi số activity started đi từ 0 lên 1 mà không phải lần đầu của process, `onActivityStopped()` giảm, không xuống dưới 0. **`ActivityCountForeground`** (`@Singleton`, để không đăng ký hai bộ callback) chỉ chuyển callback của `Application.registerActivityLifecycleCallbacks` sang `ForegroundCounter`, và **đăng ký ngay trong constructor** (dựng ở `Application.onCreate` qua `TemplateApplication` → `AppsFlyerStartup` → `TenjinTracker`, trước activity đầu tiên), để activity đang hiện lúc `start()` đã được đếm. Không thêm thư viện (không dùng `lifecycle-process`).
7. **`TenjinTracker`** (internal, `@Singleton`; `started`, `consentKnown`, `firstConnectDone` là `AtomicBoolean` như `AppsFlyerTracker`, vì tín hiệu tới từ coroutine `Main.immediate`, callback vòng đời, còn `log()` có thể tới từ luồng khác), nhận `TenjinConfig`, `TenjinSink`, `TenjinSwitches`, `AppForeground`, `AppLogger`; cài `AppsFlyerStartup` và `AdMetrics`:
   - `prepare()`: lần đầu, khóa có thì `sink.init(key)`; khóa rỗng thì log `Tenjin off: no SDK key in this build` và không gọi SDK. Lần sau không làm gì.
   - `start()`: chỉ lần đầu và khi `prepare` đã thành công; đặt `started`, đăng ký `foreground.onEachReturn { if (consentKnown) connectNow() }`, rồi `connectIfReady()`.
   - `onConsentKnown()` (cài `AttributionConsent`): lần đầu đặt `consentKnown`, rồi `connectIfReady()`; lần sau không làm gì. Khóa rỗng thì không làm gì.
   - `connectIfReady()`: khi `started && consentKnown` và chưa kết nối lần đầu thì `connectNow()`. Thứ tự tới của hai tín hiệu không quan trọng; kết nối lần đầu đúng một lần.
   - `connectNow()`: `sink.applyConsent()`; `true` thì `sink.connect()` và log `Tenjin connected`; `false` thì không gọi `connect()`, log `Tenjin not connected: no consent`. Cùng một hàm cho `start()` và cho mỗi lần quay lại foreground, nên cổng được kiểm ngay trước mỗi lời gọi kết nối, kể cả khi đồng ý bị rút rồi cấp lại.
   - `log(event)`: chỉ `ImpressionRevenue`; một cổng chung theo thứ tự: khóa, công tắc `IS_LOG_TENJIN_AD_REVENUE`, đã `start`, `consentKnown`, `tenjinAdMobPayload` khác `null`, rồi `sink.applyConsent()` ngay trước lời gọi; `false` thì log `Tenjin ad revenue opted out` và bỏ. Đủ cả thì `sink.adMobImpression(payload)`.
   - Mọi lời gọi sink bọc `try/catch (e: Exception)`, log `Tenjin <bước> failed: <tên lớp lỗi>`. Thẻ log `Analytics`; không bao giờ log khóa, mã đơn vị hay giá trị.
7a. **`AttributionConsent`** (public interface mới của `:core:analytics`, file `AttributionConsent.kt`): `fun onConsentKnown()`, gọi bởi `:core:ads` đúng một lần mỗi process. `AnalyticsModule` thêm `@Binds bindAttributionConsent(impl: TenjinTracker): AttributionConsent`. **`:core:ads`**:
   - `AttributionStart.kt` thêm hàm thuần `internal fun attributionConsentKnown(finished: Boolean, state: ConsentGateState): Boolean = finished && (state.settled || state.path == ConsentPath.CACHED_EARLY)`: chỉ tính khi vòng hỏi đã xong, hoặc chốt, hoặc xong vì cập nhật lỗi mà UMP vẫn giữ đồng ý của phiên trước. Không tính `CACHED_EARLY` lúc vòng còn chạy (khung đặt nó ngay khi gửi yêu cầu, trước khi UMP trả lời, và có thể chuyển sang `DENIED_OR_REQUIRED` khi đồng ý cũ hết hạn).
   - `AttributionStart.kt` thêm `internal suspend fun awaitAttributionConsentKnown(finished: Flow<Boolean>, state: Flow<ConsentGateState>) { combine(finished, state, ::attributionConsentKnown).first { it } }`, để phần chờ có test bằng `MutableStateFlow` như các test của `awaitAttributionStart`.
   - `AdsController`: nhận thêm `private val attributionConsent: AttributionConsent` (sau `attribution`); trong `start()`, cạnh khối chờ khởi động AppsFlyer, thêm `scope.launch { awaitAttributionConsentKnown(coordinator.finished, gate.state); logger.info(TAG, "Attribution consent known"); attributionConsent.onConsentKnown() }`. Không đổi `awaitAttributionStart` hay thời điểm `attribution.start()`; AppsFlyer giữ đúng hành vi cũ.
   - KDoc của `AdsController` thêm một câu về tín hiệu này.
8. **`FanOutAppsFlyerStartup(targets: List<AppsFlyerStartup>, logger)`** (internal): `prepare()` và `start()` gọi lần lượt từng đích, mỗi đích bắt lỗi riêng. `AnalyticsModule` (abstract class) bỏ `bindAppsFlyerStartup(impl: AppsFlyerTracker)`; `AdMetricsModule` (object) thêm `@Provides fun attributionStartup(appsFlyer: AppsFlyerTracker, tenjin: TenjinTracker, logger: AppLogger): AppsFlyerStartup = FanOutAppsFlyerStartup(listOf(appsFlyer, tenjin), logger)`; `AdMetricsModule` đổi danh sách thành `listOf(firebase, appsFlyer, tenjin)`. Thêm `@Binds bindTenjinSink(impl: TenjinLibSink)`, `@Binds bindAppForeground(impl: ActivityCountForeground)`. `bindAppsFlyerSink(impl: NoOpAppsFlyerSink)` giữ nguyên (ADR-1007). Tên `AppsFlyerStartup` là của khung, `:core:ads` và `:app` dùng; không đổi tên để đợt đồng bộ mẫu dễ gộp.
9. **`:app`**:
   - `app/build.gradle.kts`: staging đọc `tenjin.sdkKey` của `local.properties`, lọc `[A-Za-z0-9_-]+` đúng như `ads.maxSdkKey` (sai ký tự thì bỏ, cảnh báo, không in giá trị), `buildConfigField("String", "TENJIN_SDK_KEY", ...)`. Production đọc `-PtenjinSdkKey`, tùy chọn; có mà sai ký tự thì task `preProduction*Build` dừng với thông báo tiếng Việt không in giá trị. Thêm dòng `-PtenjinSdkKey=<Tenjin SDK key> (optional, ADR-1010)` vào chú thích danh sách tham số.
   - `AnalyticsConfigModule`: thêm `@Provides fun tenjinConfig(): TenjinConfig = TenjinConfig(BuildConfig.TENJIN_SDK_KEY)`.
   - `AppRemoteKeys`: `":core:analytics" to AppsFlyerSettings.all() + TenjinSettings.all()`; sinh lại `docs/remote-config-keys.md` và `docs/remote-config-template.json` bằng `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`.
   - `TemplateApplication`: không đổi code; sửa chú thích dòng `appsFlyer.prepare()` thành "prepares the attribution SDKs: AppsFlyer (off, spec ads question 3) and Tenjin (ADR-1010); with no key neither starts".
10. **Thư viện**: `gradle/libs.versions.toml` thêm `tenjin = "2.0.2"` và `tenjin-android-sdk = { group = "com.tenjin", name = "android-sdk", version.ref = "tenjin" }`; `core/analytics/build.gradle.kts` thêm `implementation(libs.tenjin.android.sdk)`. SDK mang luật R8 của nó (`proguard.txt` trong AAR); không thêm luật.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `gradle/libs.versions.toml` | Phiên bản `tenjin = "2.0.2"` và thư viện `tenjin-android-sdk` (file được bảo vệ: plan rủi ro cao, duyệt chéo) |
| Sửa | `core/analytics/build.gradle.kts` | `implementation(libs.tenjin.android.sdk)` |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinConfig.kt` | Mục 3.1, bước 1 |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinSink.kt` | Interface `TenjinSink` và `TenjinLibSink`, file duy nhất import SDK (bước 2) |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinEvents.kt` | `tenjinAdMobPayload` (bước 3) |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinSettings.kt` | Khóa `is_log_tenjin_ad_revenue` (bước 4) |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinSwitches.kt` | Bước 5 |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AppForeground.kt` | `AppForeground`, `ActivityCountForeground` (bước 6) |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinTracker.kt` | Bước 7 |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/FanOutAppsFlyerStartup.kt` | Bước 8 |
| Sửa | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/di/AnalyticsModule.kt` | Bước 8: lớp chia khởi động, thêm Tenjin vào `AdMetricsModule`, bind sink và foreground |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AttributionConsent.kt` | Bước 7a |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AttributionStart.kt` | Hàm `attributionConsentKnown` (bước 7a) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsController.kt` | Tham số `attributionConsent`, khối gọi `onConsentKnown()` trong `start()`, KDoc (bước 7a) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsControllerFixtures.kt` | Tham số `attributionConsent` của `testAdsController` (mặc định `FakeAttributionConsent()`), lớp `FakeAttributionConsent` đếm `known` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AttributionStartTest.kt` | Test của `attributionConsentKnown` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/FullScreenAdsTest.kt` | Test đường 10 giây không báo đồng ý đã rõ (mục 4) |
| Sửa | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AppsFlyerStartup.kt` | Chỉ chú thích: khởi động mọi SDK số liệu của module, kể cả Tenjin (ADR-1010) |
| Sửa | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AnalyticsTestFakes.kt` | Thêm `FakeTenjinSink` (ghi lời gọi, `consent` đặt được, `failOn` để ném lỗi) và `FakeAppForeground` (gọi lại bằng tay) |
| Tạo | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinTrackerTest.kt` | Mục 4 |
| Tạo | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinEventsTest.kt` | Mục 4 |
| Tạo | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinSettingsTest.kt` | Mục 4 |
| Tạo | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/FanOutAppsFlyerStartupTest.kt` | Mục 4 |
| Tạo | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinSdkUseTest.kt` | Quét mã nguồn theo import (mục 4) |
| Sửa | `app/build.gradle.kts` | Bước 9: `TENJIN_SDK_KEY` của staging (`local.properties`) và production (`-PtenjinSdkKey` tùy chọn, kiểm ký tự ở `preProduction*Build`) |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/di/AnalyticsConfigModule.kt` | `TenjinConfig` từ `BuildConfig` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/config/AppRemoteKeys.kt` | Thêm `TenjinSettings.all()` vào `:core:analytics` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/TemplateApplication.kt` | Chỉ chú thích (bước 9) |
| Sửa | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` | `analyticsKeys_areListed`: dữ liệu khóa mới (mục 3.5, ADR-0029) |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/NoMetaReferrerResourceTest.kt` | Tài nguyên gộp của app không có `facebook_app_id`, để phần đo nguồn cài của Meta trong SDK Tenjin không chạy (câu 21 ý g; mục 4) |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/ForegroundCounter.kt` | Bước 6 |
| Tạo | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/ForegroundCounterTest.kt` | Mục 4 |
| Sửa | `docs/remote-config-keys.md` | Sinh lại: dòng `is_log_tenjin_ad_revenue` |
| Sửa | `docs/remote-config-template.json` | Sinh lại: khóa `is_log_tenjin_ad_revenue` |

Nơi đọc các file dùng chung bị đổi (L-020; đã `grep -rn` tên từng file trong `scripts/`, `.github/`, `build-logic/`, `*/src/test`):

- `gradle/libs.versions.toml`: `.github/workflows/pr-check.yml` dùng nó trong bộ lọc đường dẫn để build bản release khi PR đổi file build (mong muốn, không sửa); `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt`, `build-logic/settings.gradle.kts` đọc catalog theo tên `libs`, plan chỉ thêm dòng, không đổi dòng có sẵn; `scripts/cloud-env-check.sh` chỉ đọc dòng `compileSdk`, không đổi; `scripts/tests/run.sh` (dòng 557, 1713) chỉ dùng tên file làm dữ liệu cho bộ lọc đường dẫn giả. `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/NoAppsFlyerDependencyTest.kt` quét file build tìm chữ `appsflyer`: dòng mới là `tenjin`, không khớp.
- `core/analytics/build.gradle.kts`: `NoAppsFlyerDependencyTest` như trên; `scripts/tests/run.sh` (dòng 3829 tới 3851, 3981) dựng file build giả hay chép file thật để kiểm `scripts/release.sh` dò SDK AppsFlyer (`libs\.appsflyer`, `"com\.appsflyer:`): dòng `libs.tenjin.android.sdk` không khớp, kết quả không đổi.
- `app/build.gradle.kts`: `scripts/release.sh`, `scripts/setup-check.sh`, `scripts/ui-test-verdict.sh` đọc `applicationId`, `versionCode`, `ASSETS_BASE_URL`: plan không đổi các dòng đó. `scripts/tests/run.sh` (dòng 3941 tới 3959) kiểm mọi tham số `release.sh` truyền đều được `app/build.gradle.kts` đọc, và đọc khối `missingProductionProperties`: plan thêm `gradleProperty("tenjinSdkKey")` (một chiều, không phá điều kiện) và không thêm vào `missingProductionProperties` (khóa tùy chọn). Khối dòng 1783 tới 1896 dựng `app/build.gradle.kts` giả.
- `docs/remote-config-keys.md`, `docs/remote-config-template.json`: `RemoteConfigKeysTest` so với code (sinh lại bằng lệnh của mục 3.1, bước 9); `scripts/ads-plan.py` đọc mọi dòng của bảng khóa, dòng mới cùng dạng; `scripts/main-guard-verified.sh` coi hai file là tài liệu Gradle đọc (`GRADLE_READ_DOCS`), nên `main-guard` kiểm commit này như code, đúng mong muốn; `scripts/tests/run.sh` (dòng 1002, 1039) chỉ dùng tên file làm dữ liệu.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Công tắc từ xa `is_log_tenjin_ad_revenue`, boolean, mặc định bật (spec ads câu 21 ý c, mục 8).
- Quảng cáo: Không chạm vị trí, định dạng hay tần suất. `:core:ads` chỉ thêm tín hiệu đồng ý đã rõ (bước 7a), không đổi luật đồng ý, nạp hay hiện quảng cáo; doanh thu đọc từ `AdMetricEvent.ImpressionRevenue` có sẵn.
- Sự kiện phân tích: dữ liệu mới gửi ra ngoài theo spec ads câu 21 ý a, d, mục 6: lượt cài và phiên (SDK tự gửi khi `connect()`), doanh thu từng lượt AdMob có doanh thu sang Tenjin. Không sự kiện Firebase mới. Phễu thuộc T-100.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. AAR Tenjin 2.0.2 không khai quyền nào; thư viện kéo theo `com.android.installreferrer:installreferrer:2.2` khai `BIND_GET_INSTALL_REFERRER_SERVICE`, đã có trong manifest gộp. Không thêm: `READ_PHONE_STATE` (chỉ cho chợ khác Play, tài liệu Tenjin), `com.google.android.gms.permission.AD_ID` (đã có từ `play-services-ads`), khai báo `queries` cho Facebook, Instagram (câu 21 ý g). Danh sách `uses-permission` trước và sau khi thêm trùng nhau (mục 4.1); `MergedManifestTest.mergedManifest_permissionsAreExactlyTheAllowedSet` giữ nguyên.
- **Thư viện và SDK:** mới: `com.tenjin:android-sdk:2.0.2` (phát hành 2026-10-23 trên Maven Central; giấy phép MIT theo POM của 2.0.2), module `:core:analytics`; kéo theo ở runtime `com.android.installreferrer:installreferrer:2.2`, `androidx.room:room-runtime:2.5.1`, `com.google.code.gson:gson:2.13.1`, `org.jetbrains.kotlin:kotlin-stdlib:1.9.24` (Gradle chọn bản cao hơn app đã có: Room 2.5.2, Kotlin 2.2.10); không quyền mới, không `queries` mới (mục 4.1). Không thêm `play-services-appset`, `play-services-ads-identifier` riêng: SDK quảng cáo đã kéo vào (mục 4.1 kiểm có trên classpath runtime).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `ActivityCountForeground` (bước 6) dùng `applicationContext as Application`; trong unit test không dùng tới (bản giả), nhưng phiên có thể gặp lỗi Hilt khi tiêm `@ApplicationContext` vào lớp này.
- **Điều kiện chuyển:** build Hilt hay test dựng `TemplateApplication` (Robolectric của `:app`) lỗi vì `ActivityCountForeground` sau 2 lần sửa.
- **Cách làm thay:** `TenjinTracker` không tự kết nối lại khi quay lại foreground; chỉ kết nối ở `start()`. Tenjin vẫn ghi lượt cài và doanh thu; số phiên trên Tenjin thấp hơn thật. Ghi vào "Lệch so với plan" và mục Việc còn lại để T-100 nối lại. Test `start_thenEachReturnToForeground_connectsAgain` khi đó đánh dấu là phần còn lại, không xóa: phương án này chỉ được dùng khi người duyệt đồng ý ở câu 8.3.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` liệt kê test của `:core:analytics`, `:app` có đọc các file mà plan sửa. Kết quả và cách xử lý:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `:core:analytics` chỉ có khóa AppsFlyer | Thêm `is_log_tenjin_ad_revenue` (bước 4, 9) | `RemoteConfigKeysTest.analyticsKeys_areListed`: `assertEquals(AppsFlyerSettings.all(), AppRemoteKeys.byModule[":core:analytics"])` và `assertEquals(11, AppsFlyerSettings.all().size)` | Dữ liệu khóa mới (ADR-0029), câu mới: `assertEquals(AppsFlyerSettings.all() + TenjinSettings.all(), AppRemoteKeys.byModule[":core:analytics"])`; giữ `assertEquals(11, AppsFlyerSettings.all().size)`; thêm `assertEquals(1, TenjinSettings.all().size)` |
| `AppsFlyerStartup` bind thẳng `AppsFlyerTracker` | Bind `FanOutAppsFlyerStartup(listOf(appsFlyer, tenjin))` (bước 8) | `NoAppsFlyerSdkTest.sink_isNoOp` đọc chữ `bindAppsFlyerSink(impl: NoOpAppsFlyerSink)` trong `AnalyticsModule.kt` | Giữ: dòng đó không đổi |
| `AdMetrics` chia cho Firebase và AppsFlyer | Chia thêm cho Tenjin (bước 8) | `FanOutAdMetricsTest` dựng `FanOutAdMetrics` với danh sách của test, không đọc `AdMetricsModule` | Giữ |
| Manifest gộp | Không đổi (mục 3.3) | `MergedManifestTest` | Giữ |
| `AdsController` nhận 15 tham số | Thêm `attributionConsent` (bước 7a) | Mọi test dựng `AdsController` qua `testAdsController` của `AdsControllerFixtures.kt` (không test nào gọi constructor trực tiếp, đã `grep`) | Giữ: tham số mới có mặc định `FakeAttributionConsent()` trong fixture |
| Thời điểm khởi động AppsFlyer (đường a, b, c) | Không đổi; khối mới chạy song song, không chờ cùng | `FullScreenAdsTest.start_startsAppsFlyerAt10s_notBefore`, `AttributionStartTest` (8 test của `awaitAttributionStart`), `AdFreeGateTest.adFree_consentSdkAndAppsFlyerUnchanged` (`starts == [0, 1, 1]`) | Giữ nguyên câu khẳng định |
| Test mở cổng đồng ý bằng `openConsentGateForTest` (đặt `GRANTED` settled qua `onPrivacyFormClosed`) | Có thể làm `attributionConsentKnown` thành `true` khi `coordinator.finished`; bản giả chỉ đếm | Các test `*ConsentGateForTest` của `AdsControllerMax*Test`, `RewardedActionTest`, `AdsControllerInListRowTest` | Giữ: không test nào đọc `FakeAttributionConsent` |
| `AnalyticsConfigModule` chỉ cấp `AppsFlyerConfig("")` | Cấp thêm `TenjinConfig` (bước 9) | `NoAppsFlyerDependencyTest.appsFlyerConfig_isEmpty` gọi `AnalyticsConfigModule.appsFlyerConfig()`; `noBuildFile_namesAppsFlyer` quét file build tìm `appsflyer` | Giữ: `appsFlyerConfig()` không đổi, file build không có chữ `appsflyer` mới |
| Test dùng tên `start` (bộ lọc của script khớp tên hàm `start()`): `BannerAdSizeTest`, `NativeAdCardTest`, `RewardedActionTest` và các test khác của `:core:ads`, feature | `AdsController.start()` chỉ thêm một khối chạy song song gọi `onConsentKnown()` (bước 7a); không đổi luật đồng ý, nạp, hiện quảng cáo hay thời điểm khởi động AppsFlyer; không đổi hàm `start` nào khác | 16 test "Cần xem trước", 288 test "Nên xem" của `existing-tests.py`, đều khớp chữ `start` | Giữ |
| Bảng khóa sinh tự động | Thêm một dòng | `RemoteConfigKeysTest` (so bảng với file) | Giữ: sinh lại file bằng lệnh `-PremoteConfigKeys.update=true` |

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-T1, không khóa thì không gửi | Unit test | `TenjinTrackerTest.noKey_prepareAndStart_neverTouchSdk`: `TenjinConfig("")`, gọi `prepare()`, `start()`, `log(revenue)`, gọi lại foreground: `FakeTenjinSink.calls` rỗng; log có `Tenjin off: no SDK key in this build`. Đối chứng cùng test: với khóa `"KEY"` thì `calls` bắt đầu bằng `init` |
| Câu 21 ý g, không đo nguồn cài của Meta | Unit test (Robolectric `:app`) | `NoMetaReferrerResourceTest.mergedResources_haveNoFacebookAppId`: `resources.getIdentifier("facebook_app_id", "string", packageName) == 0`; đối chứng cùng test: `getIdentifier("app_name", "string", packageName) != 0` |
| KB-T1, lượt cài: kết nối sau đồng ý | Unit test | `TenjinTrackerTest.start_appliesConsentThenConnects_once`: sau `prepare()`, `start()`, `start()`: `calls == [init, applyConsent, connect]` (đúng thứ tự, một lần) |
| KB-T1, chưa `prepare` | Unit test | `TenjinTrackerTest.startBeforePrepare_doesNothing`: `start()` không `prepare()`: `calls` rỗng |
| KB-T1, lần mở đầu chưa có đồng ý: không kết nối khi chỉ có `start()` | Unit test | `TenjinTrackerTest.startWithoutConsentKnown_noConnect_thenKnown_connectsOnce`: `prepare()`, `start()`: `calls == [init]`; `fire()` (quay lại foreground): vẫn `[init]`; `onConsentKnown()`: thêm `applyConsent, connect`; `onConsentKnown()` lần nữa: không thêm |
| KB-T1, tín hiệu đồng ý tới trước `start()` | Unit test | `TenjinTrackerTest.consentKnownBeforeStart_connectsAtStart_once`: `prepare()`, `onConsentKnown()`: `[init]`; `start()`: thêm `applyConsent, connect`; `start()` lần nữa: không thêm |
| KB-T1, doanh thu trước khi đồng ý rõ | Unit test | `TenjinTrackerTest.revenueBeforeConsentKnown_dropped`: `start()` không `onConsentKnown()`: lượt doanh thu không gọi `adMobImpression`; sau `onConsentKnown()` lượt kế tiếp có |
| KB-T1, khóa rỗng: tín hiệu đồng ý không chạm SDK | Unit test | Trong `noKey_prepareAndStart_neverTouchSdk`, gọi thêm `onConsentKnown()`: `calls` vẫn rỗng |
| Đồng ý đã rõ theo trạng thái của khung | Unit test | `AttributionStartTest.consentKnown_byGateState`: `true` cho (`finished`, `GRANTED`, settled), (`finished`, `DENIED_OR_REQUIRED`, settled), (`finished`, `CACHED_EARLY` chưa settled); `false` cho `INITIAL`, (`finished = false`, `CACHED_EARLY`), (`finished`, `ERROR_ALLOWED` temporary chưa settled), (`finished`, `INTERRUPTED_REGATHER`), (`finished`, `DENIED_OR_REQUIRED` chưa settled), (`finished = false`, `GRANTED`, settled) |
| Chờ đồng ý đã rõ | Unit test | `AttributionStartTest`, bằng `MutableStateFlow` và thời gian ảo: `consentKnown_returnsWhenRoundSettles` (vòng xong và chốt ở 3 giây: trả về ở 3 giây); `consentKnown_notAt10s_withoutAnswer` (không đổi gì tới 60 giây: chưa trả về); `consentKnown_cachedEarlyThenRequired_waits` (`CACHED_EARLY` lúc chưa xong, rồi `DENIED_OR_REQUIRED` chưa chốt và `finished`: chưa trả về); `consentKnown_errorAllowedThenRegatherSettles_returns` (`ERROR_ALLOWED` rồi vòng hỏi lại chốt: trả về lúc chốt); `consentKnown_staleFinished_newRoundUnsettled_waits` (`finished` còn `true` mà trạng thái mới chưa chốt và không phải `CACHED_EARLY`: chưa trả về) |
| Đường 10 giây không báo đồng ý | Unit test (Robolectric `:core:ads`) | `FullScreenAdsTest.start_10sPath_startsAppsFlyer_butConsentNotKnown`: như `start_startsAppsFlyerAt10s_notBefore` (không activity, không form); ở 10 giây `attribution.starts == 1` mà `attributionConsent.known == 0`; thêm 60 giây vẫn `0` |
| KB-T1, từ chối đồng ý: không kết nối lúc `start()` | Unit test | `TenjinTrackerTest.consentRefused_atStart_noConnect`: `consent = false`, `prepare()`, `onConsentKnown()`, `start()`: `calls == [init, applyConsent]`, không có `connect`; log `Tenjin not connected: no consent` |
| KB-T1, từ chối đồng ý: không kết nối khi quay lại, cổng mở rồi đóng | Unit test | `TenjinTrackerTest.consentWithdrawn_thenReturnToForeground_noConnect_thenGrantedAgain_connects`: `start()` với `consent = true` (có `connect`), đặt `false`, `fire()`: thêm `applyConsent`, không thêm `connect`; đặt `true`, `fire()`: thêm `applyConsent, connect` |
| Đếm lần quay lại foreground | Unit test | `ForegroundCounterTest`: activity đầu của process không tính là quay lại; 1 → 0 → 1 tính một lần; hai activity chồng nhau (A start, B start, A stop) không tính; stop thừa không làm bộ đếm âm: sau lần đầu của process, stop khi đang 0 rồi start thì tính là một lần quay lại và bộ đếm bằng 1 |
| KB-T1, phiên khi quay lại | Unit test | `TenjinTrackerTest.start_thenEachReturnToForeground_connectsAgain`: sau `start()`, `FakeAppForeground.fire()` hai lần: thêm hai cặp `applyConsent, connect`; trước `start()` `fire()` không gọi gì |
| KB-T1, doanh thu một lần | Unit test | `TenjinTrackerTest.paidAdMobImpression_sentOnce`: một `ImpressionRevenue(value = 0.0123, currency = "USD", platform = ADMOB)`: đúng một `adMobImpression`, payload `value_micros == 12300L` |
| KB-T1, lượt 0 không gửi | Unit test | `TenjinEventsTest.zeroValue_isNull` (`value = 0.0` trả `null`), đối chứng `paidValue_hasFields` (`ad_unit_id`, `currency_code`, `value_micros`, `mediation_adapter_class_name`); `TenjinEventsTest.nullAdapter_omitsAdapterField`; `TenjinEventsTest.blankCurrency_isNull` |
| KB-T1, lượt MAX chưa gửi | Unit test | `TenjinEventsTest.maxPlatform_isNull`: `platform = APPLOVIN` trả `null` |
| KB-T1, chỉ doanh thu | Unit test | `TenjinTrackerTest.nonRevenueMetrics_ignored`: `LoadSuccess`, `LoadFail`, `Clicked`, `Opportunity`, `ShowFail` không gọi sink |
| KB-T1, trước `start` | Unit test | `TenjinTrackerTest.revenueBeforeStart_dropped`: sau `prepare()`, trước `start()`: không `adMobImpression` |
| KB-T1, từ chối đồng ý | Unit test | `TenjinTrackerTest.consentRefused_revenueNotSent`: `FakeTenjinSink.consent = false`: `applyConsent` có, `adMobImpression` không; log `Tenjin ad revenue opted out` |
| KB-T1, đồng ý rút giữa phiên (cổng mở rồi đóng) | Unit test | `TenjinTrackerTest.consentWithdrawnAfterStart_nextRevenueNotSent`: `start()` với `consent = true`, gửi một lượt (có), đặt `consent = false`, gửi lượt nữa (không); đặt lại `true`, lượt thứ ba có. Kiểm `applyConsent` được gọi ngay trước mỗi `adMobImpression` |
| KB-T1, công tắc từ xa | Unit test | `TenjinTrackerTest.revenueSwitchOff_notSent_thenOnAgain`: `FakeRemoteSettings.set(IS_LOG_TENJIN_AD_REVENUE, false)`: không gửi; đặt `true` giữa phiên: lượt kế tiếp gửi |
| KB-T1, khóa từ xa đúng quy ước | Unit test | `TenjinSettingsTest.keys_areBooleanSwitches_onByDefault` (mọi khóa `is_log_tenjin_*`, boolean, mặc định `true`, mô tả có "Tenjin"); `TenjinSettingsTest.switches_matchAll` |
| KB-T1, Tenjin lỗi | Unit test | `TenjinTrackerTest.sdkThrows_isSwallowedAndLogged`: `failOn = init`, rồi `failOn = connect`, rồi `failOn = adMobImpression`: không ném ra ngoài; log `Tenjin ... failed: IllegalStateException`, không có khóa hay giá trị trong log (`RecordingAppLogger`: không dòng nào chứa `"KEY"`, `"12300"`) |
| KB-T1, Tenjin lỗi không chặn AppsFlyer | Unit test | `FanOutAppsFlyerStartupTest.oneTargetThrows_othersStillCalled`: đích đầu ném ở `prepare()` và `start()`, đích sau vẫn được gọi; log tên lớp lỗi. `FanOutAppsFlyerStartupTest.callsEveryTarget_inOrder` |
| KB-T1, không lộ khóa | Unit test | `TenjinTrackerTest.configToString_hidesKey`: `TenjinConfig("SECRETKEY").toString()` không chứa `SECRETKEY` |
| SDK chỉ trong một file | Unit test (quét theo import) | `TenjinSdkUseTest.onlyTenjinLibSink_importsTheSdk`: quét mọi `src/main/**/*.kt` từ gốc repo, dòng bắt đầu bằng `import com.tenjin`: đúng một file `core/analytics/.../TenjinSink.kt`; đối chứng: thấy được file của `core/analytics/src/main/` |
| Không quyền mới | Unit test có sẵn | `MergedManifestTest.mergedManifest_permissionsAreExactlyTheAllowedSet` giữ nguyên danh sách |
| Bảng khóa | Unit test có sẵn | `RemoteConfigKeysTest` sau khi sinh lại; `analyticsKeys_areListed` theo mục 3.5 |
| Bản release build được (R8) | Lệnh | `./gradlew :app:assembleStagingRelease` qua (CI `pr-check` cũng build vì PR đổi thư viện) |
| Production có khóa sai ký tự thì dừng | Kiểm tra thủ công (người: không chạy build production trong phiên, ADR-0049) | Người chạy `./gradlew :app:preProductionReleaseBuild -PtenjinSdkKey='a"b' -PadmobAppId=x -PadUnits=x -PassetsBaseUrl=x` trên máy, thấy thông báo tiếng Việt không in giá trị khóa (T-101) |
| Lượt cài, doanh thu thật trên Tenjin | Kiểm tra thủ công (máy thật: Play, quảng cáo thật) | T-101: bản có khóa thử nghiệm của app Tenjin riêng, cài, xem quảng cáo, thấy lượt cài và doanh thu trên bảng Tenjin |
| Tín hiệu đồng ý đã rõ trên staging | Kiểm tra thủ công (người: đọc `app-log.txt` của `device-session` cuối phiên) | Sau khi kịch bản trả lời hộp thoại đồng ý, `app-log.txt` có dòng `VGL.Ads` chứa `Attribution consent known` (ghi ở khối của bước 7a); lần mở app chưa trả lời thì chưa có dòng đó |
| Log khi không khóa trên staging | Kiểm tra thủ công (người: ngôn ngữ kịch bản emulator không có bước chờ log; đọc `app-log.txt` của `device-session` cuối phiên) | Trong `app-log.txt` có dòng `VGL.Analytics` chứa `Tenjin off: no SDK key in this build` sau mỗi lần mở app (ghi ở `TenjinTracker.prepare()`, gọi từ `TemplateApplication.onCreate` qua `FanOutAppsFlyerStartup`) |

**Ngoại lệ viết test trước:** `TenjinLibSink` (gọi thẳng SDK, cần Context và mạng; test JVM không có Robolectric ở module này), `ActivityCountForeground` (chỉ chuyển callback vòng đời sang `ForegroundCounter`, có test), phần đọc khóa trong `app/build.gradle.kts`, và nối Hilt ở `AnalyticsModule` (`TenjinTracker` mang `@Singleton` nên `FanOutAppsFlyerStartup` và `FanOutAdMetrics` nhận cùng một bản, như `AppsFlyerTracker` hôm nay; module không có Robolectric để dựng đồ thị Hilt). Các phần này chỉ nối, không có quyết định; mọi quyết định (khóa, đồng ý, công tắc, payload, thứ tự) nằm trong `TenjinTracker` và `tenjinAdMobPayload`, đều có test.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-11-17, worktree tạm từ commit `7d287bd` (không commit): thêm `com.tenjin:android-sdk:2.0.2` vào `libs.versions.toml` và `core/analytics`; một lớp tạm gọi `TenjinSDK.getInstance`, `setAppStore(TenjinSDK.AppStoreType.googleplay)`, `optInOutUsingCMP()`, `connect()`, `eventAdImpressionAdMob(JSONObject)`; một unit test tạm cho payload dạng `Map` (`value_micros == 12300L`). Đọc AAR: manifest không khai quyền; `proguard.txt` có sẵn luật giữ `TenjinSDK`; `GDPRHelper.d()` trả `true` khi không có `IABTCF_PurposeConsents`, có chuỗi thì cần mục đích 1 là `1`.
- **Lệnh:** `scripts/check.sh`; `./gradlew :app:assembleStagingRelease`; `./gradlew :app:processStagingDebugMainManifest` trước và sau, so `uses-permission` và `queries`.
- **Kết quả:**
  - Manifest gộp `stagingDebug`: danh sách `uses-permission`, khai báo `queries` và thành phần (`activity`, `provider`, `receiver`, `service`) trước và sau khi thêm SDK trùng nhau. Không quyền mới.
  - `scripts/check.sh` trên máy dev macOS, lần 1: thoát 1 ở test script `pr-check: the filter output check fails on a copy reading an undefined output` (`scripts/tests/run.sh`, dòng 543). Đây là lỗi môi trường có sẵn, không do SDK: lệnh `sed '0,/re/s//.../'` là cú pháp GNU, sed BSD của macOS chạy mà không thay chữ (đã thử riêng), nên bản chép hỏng mà test cần không được tạo. CI và phiên cloud chạy Linux (GNU sed) không gặp. `scripts/failure-kind.sh --playbook` trả PB-02, không khớp. Lần 2 chạy với GNU sed đứng trước trong `PATH`: mọi test script qua, Gradle dừng ở ktlint của chính test tạm (`argument-list-wrapping`). Lần 3, sau khi sửa định dạng test tạm: **thoát 0**, `Test JVM: 3533 test, 0 lỗi`, `:core:analytics: 45 test, 0 lỗi` (gồm test tạm), `MergedManifestTest` 16 test qua, `Cổng chất lượng: QUA`.
  - `./gradlew :app:assembleStagingRelease`: `BUILD SUCCESSFUL`. R8 chỉ cảnh báo lớp thiếu của Pangle (`com.bytedance.sdk.openadsdk`), có từ trước; không cảnh báo nào của `com.tenjin`. Không cần luật R8 riêng (AAR mang `proguard.txt`).
  - Classpath runtime `stagingRelease`: `com.tenjin:android-sdk:2.0.2`, `installreferrer:2.2`, `gson:2.13.1`; `play-services-appset` và `play-services-ads-identifier` đã có từ SDK quảng cáo; `room-runtime` được Gradle nâng lên 2.5.2 (bản app đã có), không xung đột.
  - Phương án dự phòng (mục 3.4) chưa thử: `ActivityCountForeground` không có trong lần chạy thử.
  - Đọc bytecode 2.0.2 (theo rà soát độc lập PR #360): `optOut()` chỉ đặt `ConsentParamFilter` sang `EXPLICIT_OPTOUT`; ở trạng thái đó `ConsentParamFilter.a(HashMap)` vẫn dựng tham số (danh sách trắng gồm `session_id`, `analytics_installation_id`, `referrer`, `bundle_id`, `app_version`...) và thêm `opt_out`; `connect(String, String)` không thoát sớm khi opt out. Vì vậy app tự không gọi `connect()` khi không đồng ý (mục 3.1, bước 7). SDK còn gửi `firebase_app_instance_id` (đọc qua reflection, chờ tối đa 2 giây ở luồng nền), tham số DMA (`ad_personalization`, `ad_user_data`, `iab_tcf`), nguồn cài Huawei; đã ghi vào mục 6 của spec ads. `getInstance` ở `prepare()` chỉ dựng tham số, SharedPreferences và CSDL cục bộ, không gửi và không tự đăng ký callback vòng đời. `MetaReferrerParams` luôn chạy và đọc tài nguyên `facebook_app_id`; app không có tài nguyên này (test `NoMetaReferrerResourceTest` giữ điều đó).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 2 (SDK bên thứ ba mới: Tenjin), 4 (dữ liệu gửi ra khỏi máy: lượt cài, phiên, doanh thu quảng cáo), 5 (thứ tự khởi tạo SDK theo đồng ý), 7 (module core `:core:analytics`, cấu hình build `app/build.gradle.kts`, `gradle/libs.versions.toml`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | `:core:analytics` (lớp mới, đổi binding Hilt), `:core:ads` (tín hiệu đồng ý đã rõ trong `AdsController`, `AttributionStart.kt`) và `:app` (build, DI, khóa Remote Config) |
| B. Độ rõ của yêu cầu | 1 | Câu 21 (ý a tới i) đã trả lời, spec đã xác nhận, ADR-1010 đã nhận; vài chi tiết AI tự quyết (trường payload, kết nối khi quay lại foreground) đã hỏi ở mục 8 |
| C. Khả năng kiểm chứng tự động | 2 | Mọi quyết định có unit test; lượt cài và doanh thu thật trên Tenjin chỉ kiểm được trên máy thật với khóa (T-101) |
| D. Ảnh hưởng chéo | 2 | Đổi binding `AppsFlyerStartup` và `AdMetrics` mà `:core:ads` dùng; T-100 sửa cùng file |
| E. Độ mới của kỹ thuật | 2 | SDK chưa có trong dự án; cách nối lặp lại mẫu AppsFlyer |
| **Tổng** | 10 | **Mức: Cao (chạm điểm chặn)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt` (thêm một tham số và một khối chờ; mọi test dựng nó qua `testAdsController`), `AnalyticsModule.kt` (binding `AppsFlyerStartup`, `AdMetrics` mà `AdsController` nhận), `gradle/libs.versions.toml`, `app/build.gradle.kts`, `AppRemoteKeys.kt`, bảng khóa sinh tự động.
- Task khác trong sprint có thể xung đột: T-100 sửa `TenjinTracker.kt`, `TenjinSettings.kt`, `AnalyticsModule.kt` (phễu, `AdFunnel`), nên chạy sau. Đợt đồng bộ dự án mẫu kế tiếp phải giữ phần Tenjin của `core/analytics` và phần riêng của `core/ads` (tham số `attributionConsent` và khối gọi `onConsentKnown()` trong `AdsController.kt`, hai hàm mới của `AttributionStart.kt`, fixture) (ADR-1010).
- Đề xuất thứ tự chạy: trước T-100; không có task nào khác đang chờ sửa `core/analytics`.

## 7. Điều kiện dừng riêng

- Manifest gộp có quyền hay khai báo `queries` mới so với mục 4.1: dừng, quay lại spec ads mục 6.
- Cần sửa `:core:ads` ngoài tín hiệu đồng ý của bước 7a (ví dụ cần `AdValue` hay `ResponseInfo` gốc để gửi doanh thu, hay đổi `awaitAttributionStart`): dừng.
- Một test có sẵn về thời điểm khởi động AppsFlyer (`FullScreenAdsTest.start_startsAppsFlyerAt10s_notBefore`, `AttributionStartTest`) đổi kết quả: dừng; AppsFlyer phải giữ hành vi cũ.
- Cần gọi SDK Tenjin ở file khác `TenjinSink.kt`: dừng (ADR-1010).

## 8. Câu hỏi cho người duyệt

1. **Trường của payload doanh thu.** Sự kiện của khung không giữ `response_id` và `precision_type` của AdMob; Tenjin chỉ bắt buộc `value_micros`. Đề xuất: gửi bốn trường có sẵn (mục 3.1, bước 3), không sửa `:core:ads`. Muốn đủ sáu trường thì cần một ticket sửa `AdTelemetry` của `:core:ads` (lệch thêm khỏi mẫu). Trả lời (Nhật, 2026-11-17): bốn trường có sẵn.
2. **Doanh thu của MAX.** MAX chưa bật nên lượt MAX không gửi sang Tenjin. Đề xuất: khi bật MAX (ticket riêng), thêm hàm `eventAdImpressionAppLovin` vào cùng ticket đó.
3. **Kết nối lại khi quay lại foreground.** Tài liệu Tenjin bảo gọi `connect()` ở mỗi `onResume` của activity chính để đếm phiên. Đề xuất: kết nối mỗi lần cả app quay lại foreground (không phải mỗi `onResume`, để xoay màn hình hay hộp thoại quyền không đếm thêm phiên). Đồng ý cả phương án dự phòng ở mục 3.4 (chỉ kết nối lúc `start()`) nếu phần này lỗi? Trả lời (Nhật, 2026-11-17): mỗi lần cả app quay lại foreground; dùng dự phòng nếu lỗi.
4. **Spec và ADR.** Nhật xác nhận spec ads (câu 21) và nhận ADR-1010 ngày 2026-11-17 (PR #360).
5. **Lần mở đầu chưa có đồng ý (spec ads câu 21 ý i).** Nhật chọn hoãn (2026-11-17); plan làm theo bước 7a. Hệ quả cần Nhật biết (đọc code của khung): bản production tắt lối tắt đồng ý (`ConsentShortcutConfig.OFF`), nên lần cập nhật đồng ý lỗi mạng mà không có đồng ý cũ cho ra `DENIED_OR_REQUIRED` chưa chốt và khung không hỏi lại trong process đó; lần hỏi kế tiếp là lần mở app sau. Người dùng đó không có số liệu Tenjin suốt phiên đầu, lượt cài gửi ở lần mở sau (muộn chứ không mất). `INTERRUPTED_REGATHER` (hộp thoại bị ngắt) cũng vậy. Còn khi UMP lỗi cấu hình mãi thì người dùng không bao giờ có tín hiệu nên không gửi gì. Đồng ý cách tính này? Trả lời (Nhật, 2026-11-17): chấp nhận.
6. **Duyệt chéo.** Rủi ro Cao: cần người duyệt chéo khác Nhật (đề xuất Tuấn, như T-098). Trả lời (Nhật, 2026-11-17): Tuấn duyệt chéo.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
