---
ticket: T-100
title: Gửi sự kiện phễu quảng cáo sang Tenjin, mỗi sự kiện một công tắc từ xa
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn: để trống
risk_level: high             # low | medium | high
token_estimate: 7.4M         # cỡ S theo scripts/token-usage.py estimate (2026-11-17), không nâng tay
blocking_points: [data, core]
run_order:                   # AI điền khi lập danh sách chạy
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-11-17
lane:                        # để trống: phiên tự chạy
---

# PLAN T-100: GỬI SỰ KIỆN PHỄU QUẢNG CÁO SANG TENJIN, MỖI SỰ KIỆN MỘT CÔNG TẮC TỪ XA

## 1. Tóm tắt

`TenjinTracker` của T-099 cài thêm interface `AdFunnel` và gửi mười loại sự kiện phễu quảng cáo theo ADR-1010 mục 5 (tên của AppsFlyer bỏ `af_`, không tham số) qua hàm `eventWithName` của SDK Tenjin, đi qua đúng cổng của doanh thu (khóa SDK, công tắc, đã khởi động, đồng ý đã rõ, đồng ý TCF đọc ngay trước mỗi sự kiện). Mười khóa `is_log_tenjin_*` mới, mặc định bật, đọc ở mỗi sự kiện; `AdFunnel` của app thành một lớp chia `FanOutAdFunnel` gửi cho AppsFlyer (vẫn tắt, ADR-1007) rồi Tenjin. Kiểm bằng unit test JVM trên bản giả của SDK, không cần máy thật; `:core:ads` không đổi.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-T1 | Khi một sự kiện phễu quảng cáo xảy ra (đủ điều kiện hiện, nạp xong, đã hiện, theo danh sách ở câu 21 ý b), thì Tenjin nhận sự kiện đó, trừ khi công tắc từ xa của sự kiện đang tắt (câu 21 ý c). | Sự kiện phễu là `AdFunnelEvent` mà `:core:ads` (`AdTelemetry`) đã phát cho AppsFlyer theo `ads-business-rules.md` mục 8.3; T-100 không đổi lúc phát. Tên Tenjin là tên AppsFlyer bỏ `af_` (ADR-1010 mục 5), không tham số. Mỗi sự kiện một khóa `is_log_tenjin_<tên>`; mười `inters_displayed_{N}_times` dùng chung một khóa `is_log_tenjin_inters_displayed_n_times`, như `is_log_af_inters_displayed_n_times` (câu 1 của mục 8). |
| KB-T1 | Khi người dùng ở vùng cần hỏi đồng ý từ chối cho phép theo dõi, thì app không gửi gì sang Tenjin (câu 21 ý d). | Sự kiện phễu đọc đồng ý TCF (`applyConsent()`) ngay trước mỗi lượt gửi, như doanh thu; `false` thì không gửi. |
| KB-T1 | Khi người dùng mở app lần đầu ở vùng cần hỏi đồng ý và chưa trả lời hộp thoại đồng ý (kể cả khi hết 10 giây chờ), thì app chưa gửi gì sang Tenjin; khi họ đồng ý thì app bắt đầu gửi, khi họ từ chối thì không gửi (câu 21 ý i). | Sự kiện phễu trước tín hiệu `onConsentKnown()` của T-099, hay trước `start()`, bị bỏ (không giữ lại để gửi sau), như doanh thu và như AppsFlyer (câu 2 của mục 8). |
| KB-T1 | Khi bản build không có khóa Tenjin, thì app không gửi gì sang Tenjin và vẫn chạy như hôm nay. | Không khóa thì không gọi SDK, kể cả với sự kiện phễu. |
| KB-T1 | Khi Tenjin lỗi hay không trả lời, thì quảng cáo, số liệu Firebase và mọi màn của app vẫn chạy như hôm nay. | Lỗi của `eventWithName` bị nuốt, log tên lớp lỗi; lớp chia bắt lỗi riêng từng đích, nên AppsFlyer không bị chặn. |

Các tiêu chí khác của KB-T1 (lượt cài, doanh thu) thuộc T-099, đã merge; T-100 giữ nguyên hành vi đó (mục 3.5).

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-1010 (mục 5, 6 và Việc AI phải tuân thủ), ADR-0027 (khung AppsFlyer, phễu mục 8.3), ADR-1007 (AppsFlyer tắt, sink rỗng), ADR-0003 (cờ đọc theo `Flow`), ADR-0046 (khai báo khóa Remote Config), ADR-0019 (log), ADR-0001 (phụ thuộc module: chỉ sửa `:core:analytics` và test của `:app`).

Đã đọc theo `docs/CLAUDE.md`, mục Đọc trước khi làm: ticket; spec ads (câu 21 ý a tới i, KB-T1, mục 6, mục 8); không có thiết kế; `docs/foundation.md` và ADR-1010 đầy đủ; không có phản hồi `triaged` hay `planned` nào liên kết T-100; plan T-099 (mục 3.1, 3.2, 10 kèm ghi chú rà soát).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu phễu của `AppsFlyerTracker` (hàm thuần `toAfEvent()`, `switch()` trong `AppsFlyerEvents.kt`, một cổng `send` chung) và mẫu lớp chia `FanOutAdMetrics`. Mọi lớp ở gói `com.vglstudio.camera.pose.line.guide.core.analytics` của `:core:analytics`.

1. **`TenjinSink`** thêm hàm `fun event(name: String)` (KDoc: "Sends one event by name, with no value or param (ADR-1010, section 5)"). `TenjinLibSink.event` gọi `requireNotNull(sdk).eventWithName(name)` (có trong `com.tenjin.android.TenjinSDK` 2.0.2: `public void eventWithName(java.lang.String)`, đã xem bằng `javap` trên AAR trong bộ đệm Gradle). Không dùng `eventWithNameAndValue`: ADR-1010 nói không tham số. `TenjinLibSink` thêm `@Singleton` (ghi chú rà soát T-099: lớp giữ trạng thái `sdk`; hôm nay chỉ `TenjinTracker` `@Singleton` nhận nó nên chưa lỗi, chú thích ở mục 4, ngoại lệ viết test trước).
2. **`TenjinEvents.kt`** thêm hai hàm thuần (internal):
   - `fun AdFunnelEvent.toTenjinEvent(): String? = toAfEvent()?.name?.removePrefix("af_")`: đúng quy tắc "bỏ tiền tố `af_`" của ADR-1010 mục 5 và đúng điều kiện gửi của AppsFlyer (N ngoài 1 tới 20 thì `null`), không chép lại bảng tên. Tham số của `AfEvent` bị bỏ.
   - `fun AdFunnelEvent.tenjinSwitch(): RemoteKey<Boolean>`: `when` đủ nhánh, không `else`, như `switch()` của AppsFlyer, trả khóa `TenjinSettings` của từng sự kiện.
3. **`TenjinSettings`** thêm mười khóa, cùng dạng mô tả với khóa doanh thu qua một hàm `private fun key(name, description)`; mô tả của `is_log_tenjin_ad_revenue` giữ nguyên từng chữ (dòng của nó trong `docs/remote-config-keys.md` không đổi). Mô tả mỗi khóa mới: `"Sends <tên sự kiện> to Tenjin, with no param. Tenjin event switch (ADR-1010). Name set by the app, not an AdsKit key."`. `switches()` theo thứ tự: doanh thu, rồi thứ tự của `AppsFlyerSettings`:

   | Hằng | Khóa | Sự kiện Tenjin |
   | --- | --- | --- |
   | `IS_LOG_TENJIN_INTERS_AD_ELIGIBLE` | `is_log_tenjin_inters_ad_eligible` | `inters_ad_eligible` |
   | `IS_LOG_TENJIN_INTERS_API_CALLED` | `is_log_tenjin_inters_api_called` | `inters_api_called` |
   | `IS_LOG_TENJIN_INTERS_DISPLAYED` | `is_log_tenjin_inters_displayed` | `inters_displayed` |
   | `IS_LOG_TENJIN_REWARDED_AD_ELIGIBLE` | `is_log_tenjin_rewarded_ad_eligible` | `rewarded_ad_eligible` |
   | `IS_LOG_TENJIN_REWARDED_API_CALLED` | `is_log_tenjin_rewarded_api_called` | `rewarded_api_called` |
   | `IS_LOG_TENJIN_REWARDED_AD_DISPLAYED` | `is_log_tenjin_rewarded_ad_displayed` | `rewarded_ad_displayed` |
   | `IS_LOG_TENJIN_NATIVE_AD_ELIGIBLE` | `is_log_tenjin_native_ad_eligible` | `native_ad_eligible` |
   | `IS_LOG_TENJIN_NATIVE_API_CALLED` | `is_log_tenjin_native_api_called` | `native_api_called` |
   | `IS_LOG_TENJIN_NATIVE_AD_DISPLAYED` | `is_log_tenjin_native_ad_displayed` | `native_ad_displayed` |
   | `IS_LOG_TENJIN_INTERS_DISPLAYED_N_TIMES` | `is_log_tenjin_inters_displayed_n_times` | `inters_displayed_{N}_times`, N từ 1 tới 20 |

   `TenjinSwitches` không đổi: nó `stateIn` mọi khóa của `switches()`, nên khóa mới được theo dõi từ đầu và đọc ở mỗi sự kiện.
4. **`TenjinTracker`** cài thêm `AdFunnel`:
   - Tách cổng của `log(AdMetricEvent)` thành một hàm riêng `private fun <T : Any> send(label: String, switch: RemoteKey<Boolean>, prepare: () -> T?, block: (T) -> Unit)`, giữ đúng thứ tự hôm nay: không khóa thì về; công tắc tắt thì debug `Tenjin <label> switched off`; chưa `started` thì debug `Tenjin <label> before start`; chưa `consentKnown` thì debug `Tenjin <label> before consent known`; rồi `val value = prepare() ?: return` (chạy sau các cổng trên và trước khi đọc đồng ý, `null` thì về không log, như payload `null` hôm nay); `call("consent") { sink.applyConsent() } != true` thì debug `Tenjin <label> opted out`; còn lại `call(label) { block(value) }`. Doanh thu gọi `send("ad revenue", REVENUE_SWITCH, prepare = { tenjinAdMobPayload(event) }) { sink.adMobImpression(it) }`: payload vẫn chỉ tính khi đã qua bốn cổng đầu, và `null` thì không đọc đồng ý, đúng như hôm nay; chữ của mọi dòng debug và WARN của doanh thu giữ nguyên (`Tenjin ad revenue switched off`, `... before start`, `... before consent known`, `... opted out`, `Tenjin ad revenue failed: <Lớp>`).
   - `override fun log(event: AdFunnelEvent)`: `val name = event.toTenjinEvent() ?: return` (N ngoài 1 tới 20 thì về trước mọi cổng, không log), rồi `send(name, event.tenjinSwitch(), prepare = { name }) { sink.event(it) }`. Log chỉ ghi tên sự kiện, không mã đơn vị hay nguồn (ADR-0019, ADR-1010).
   - KDoc của lớp thêm một gạch về phễu.
5. **`FanOutAdFunnel(targets: List<AdFunnel>, logger: AppLogger)`** (internal, file mới, theo `FanOutAdMetrics`): gửi từng sự kiện cho từng đích theo thứ tự, bắt lỗi riêng từng đích, log WARN thẻ `Analytics`: `Ad funnel event not logged by <Lớp đích>: <Lớp lỗi>`.
6. **`AnalyticsModule.kt`**: `AnalyticsModule` bỏ `@Binds bindAdFunnel(impl: AppsFlyerTracker): AdFunnel`; `AdMetricsModule` thêm `@Provides fun adFunnel(appsFlyer: AppsFlyerTracker, tenjin: TenjinTracker, logger: AppLogger): AdFunnel = FanOutAdFunnel(listOf(appsFlyer, tenjin), logger)`. KDoc của `AdMetricsModule` thêm "the ad funnel reaches AppsFlyer, then Tenjin, in the same way". AppsFlyer vẫn nhận phễu (sink rỗng, ADR-1007), để đợt đồng bộ mẫu dễ gộp. Bỏ `bindAdFunnel` và thêm `adFunnel` phải nằm trong cùng một commit (kể cả commit khung của test viết trước, mục 4): hai binding `AdFunnel` cùng lúc thì Hilt của `:app` báo lỗi.
7. **Bảng khóa**: `AppRemoteKeys` đã có `TenjinSettings.all()` từ T-099, không sửa. Sinh lại `docs/remote-config-keys.md` và `docs/remote-config-template.json` bằng `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`; chỉ có mười dòng, mười khóa mới.
8. **`:core:ads` không đổi**: `AdTelemetry.logFunnel` đã gọi `AdFunnel` ở mọi điểm của mục 8.3 và đã bắt lỗi.

Mọi đường tới một lượt gửi phễu sang Tenjin (gạch "Tiêu chí áp cho mọi lượt gửi" của `docs/CLAUDE.md`): chỉ một đường, `AdTelemetry.logFunnel` → `AdFunnel` (Hilt: `FanOutAdFunnel`) → `TenjinTracker.log(AdFunnelEvent)` → `send` → `sink.event`. Cổng nằm trong một hàm `send` mà doanh thu và phễu cùng gọi; cổng có thể mở rồi đóng (công tắc đổi giữa phiên, đồng ý bị rút): đồng ý và công tắc đọc ngay trước mỗi lời gọi, mỗi trường hợp có test ở mục 4.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinSink.kt` | Hàm `event(name)`, `TenjinLibSink.event` gọi `eventWithName`; `@Singleton` cho `TenjinLibSink` (bước 1) |
| Sửa | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinEvents.kt` | `toTenjinEvent()`, `tenjinSwitch()` (bước 2) |
| Sửa | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinSettings.kt` | Mười khóa phễu, hàm `key` (bước 3) |
| Sửa | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinTracker.kt` | Cài `AdFunnel`, cổng chung `send` (bước 4) |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/FanOutAdFunnel.kt` | Bước 5 |
| Sửa | `core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/di/AnalyticsModule.kt` | Bỏ `bindAdFunnel`, thêm `AdMetricsModule.adFunnel` (bước 6) |
| Sửa | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/AnalyticsTestFakes.kt` | `FakeTenjinSink.event` ghi `event` vào `calls` và tên vào `events`; `failOn = "event"` ném lỗi; KDoc liệt kê thêm `event` |
| Sửa | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinTrackerTest.kt` | Test phễu của mục 4 |
| Sửa | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinEventsTest.kt` | Test tên và công tắc của mục 4 |
| Sửa | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinSettingsTest.kt` | Test mười một khóa của mục 4 |
| Tạo | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/FanOutAdFunnelTest.kt` | Mục 4 |
| Tạo | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/di/AdMetricsModuleTest.kt` | Mục 4: `AdMetricsModule.adFunnel` đưa sự kiện tới AppsFlyer và Tenjin |
| Sửa | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` | `analyticsKeys_areListed`: số khóa Tenjin 1 thành 11 (mục 3.5, ADR-0029) |
| Sửa | `docs/remote-config-keys.md` | Sinh lại: mười dòng `is_log_tenjin_*` mới |
| Sửa | `docs/remote-config-template.json` | Sinh lại: mười khóa `is_log_tenjin_*` mới |

Nơi đọc các file dùng chung bị đổi (đã `grep -rn` tên file trong `scripts/`, `.github/`, `*/src/test`):

- `docs/remote-config-keys.md`, `docs/remote-config-template.json`: `RemoteConfigKeysTest` so với code (sinh lại bằng lệnh của bước 7); `scripts/ads-plan.py` đọc mọi dòng của bảng khóa, dòng mới cùng dạng, không sửa; `scripts/main-guard-verified.sh` coi hai file là tài liệu Gradle đọc (`GRADLE_READ_DOCS`), nên `main-guard` kiểm commit như code, đúng mong muốn; `scripts/tests/run.sh` chỉ dùng tên file làm dữ liệu.
  `app/build.gradle.kts` chỉ đặt đường dẫn của hai file cho task sinh lại và so bảng khóa (biến `remoteConfigKeysMarkdown`, `remoteConfigKeysTemplate`), không đọc nội dung từng dòng; không bị ảnh hưởng, không sửa.
- `TenjinSink.kt`: `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinSdkUseTest.kt` quét import `com.tenjin` và chỉ cho phép file này; plan không thêm import nào ở file khác nên test giữ nguyên, không sửa (câu 4 của mục 8).
- `AnalyticsModule.kt`: Hilt của `:app` dựng đồ thị; binding `AdFunnel` cũ bỏ cùng commit với binding mới, nên không có hai binding.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Mười công tắc từ xa `is_log_tenjin_*` mới (bảng ở bước 3), boolean, mặc định bật (spec ads câu 21 ý c; ADR-1010 mục 6).
- Quảng cáo: Không chạm vị trí, định dạng, tần suất hay thời điểm phát sự kiện phễu; `:core:ads` không đổi.
- Sự kiện phân tích: dữ liệu mới gửi ra ngoài theo spec ads câu 21 ý b và ADR-1010 mục 5: mười loại sự kiện phễu sang Tenjin, chỉ tên, không tham số. Không sự kiện Firebase mới, không đổi sự kiện AppsFlyer.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Dùng `tenjin-android-sdk` (`com.tenjin:android-sdk:2.0.2`) đã có trong `gradle/libs.versions.toml` và `core/analytics/build.gradle.kts` từ T-099; hàm mới gọi là `TenjinSDK.eventWithName(String)` của cùng thư viện.

### 3.4. Phương án dự phòng

Không có. Mọi bước là biến thể của mẫu đã có trong module (phễu AppsFlyer, lớp chia, khóa Tenjin); hàm `eventWithName` đã có trong AAR 2.0.2.

### 3.5. Hành vi đã có bị đổi

`python3 scripts/existing-tests.py` (2026-11-17) trên plan này: 450 test dùng tên hay tag của các file plan sửa; nhóm "Cần xem trước" 16 test, nhóm "Nên xem" 344 test. Đã đọc danh sách: gần như mọi dòng khớp chỉ vì tên chung `start`, `log`, `all`, `switches` (hàm của lớp khác: `IntroAdWaitTest`, `OnboardingAdContentTest`, `NativeAdCardTest`, `RewardedActionTest`...), không đọc `TenjinTracker`, `TenjinSettings`, `TenjinSink`, `TenjinEvents` hay binding `AdFunnel`; 16 test đo vị trí, kích thước đều là test giao diện của `:core:ads`, `:feature:*`, plan không đổi giao diện. Các test thật sự đọc phần plan đổi là `TenjinTrackerTest`, `TenjinSettingsTest`, `TenjinEventsTest`, `TenjinSdkUseTest` của `:core:analytics` và `RemoteConfigKeysTest` của `:app`, xử lý ở bảng dưới:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `TenjinSettings` có một khóa | Mười một khóa (bước 3) | `RemoteConfigKeysTest.analyticsKeys_areListed` (`assertEquals(1, TenjinSettings.all().size)`) | Dữ liệu của phần plan thêm (ADR-0029): `assertEquals(11, TenjinSettings.all().size)`; câu `AppsFlyerSettings.all() + TenjinSettings.all()` giữ nguyên |
| `TenjinSettings.all()` chỉ có khóa doanh thu | Doanh thu đứng đầu, rồi mười khóa phễu theo bảng bước 3 | `TenjinSettingsTest.keys_areBooleanSwitches_onByDefault` (đối chứng `assertEquals(listOf("is_log_tenjin_ad_revenue"), keys.map { it.name })`), `TenjinSettingsTest.switches_matchAll` (đối chứng `assertEquals(listOf(TenjinSettings.IS_LOG_TENJIN_AD_REVENUE), switches)`) | Dữ liệu của phần plan thêm (ADR-0029), giữ ý định đối chứng: câu thứ nhất thành `assertEquals(listOf("is_log_tenjin_ad_revenue", "is_log_tenjin_inters_ad_eligible", ..., "is_log_tenjin_inters_displayed_n_times"), keys.map { it.name })` (đủ mười một tên theo thứ tự bảng bước 3); câu thứ hai thành danh sách mười một hằng cùng thứ tự. Mọi câu khác (boolean, mặc định bật, mô tả có `Tenjin`, kết thúc bằng `Name set by the app, not an AdsKit key.`) giữ nguyên và nay áp cho cả mười một khóa |
| `AdFunnel` của app là `AppsFlyerTracker` | `FanOutAdFunnel(listOf(appsFlyer, tenjin))` (bước 6) | Không test nào khẳng định binding Hilt này (test của `:core:ads` dùng `RecordingAdFunnel`) | Test mới `AdMetricsModuleTest` |
| Cổng doanh thu nằm trong `log(AdMetricEvent)` | Cổng tách ra `send`, cùng thứ tự, cùng chữ log (bước 4) | `TenjinTrackerTest` (mọi test doanh thu, nhất là `sdkThrows_isSwallowedAndLogged` so đúng dòng WARN, `revenueSwitchOff_notSent_thenOnAgain`, `consentWithdrawnAfterStart_nextRevenueNotSent`) | Giữ nguyên, không sửa câu khẳng định nào; test phải qua như cũ |
| `FakeTenjinSink.calls` chỉ có bốn loại lời gọi | Thêm `event` | Các test so `sink.calls` của `TenjinTrackerTest` | Giữ: test doanh thu không phát sự kiện phễu nên `calls` của chúng không đổi |

## 4. Kiểm thử

Mọi test là unit test JVM của `:core:analytics` (không Robolectric), dựng như `TenjinTrackerTest` hôm nay: `TenjinTracker` thật với `FakeTenjinSink`, `FakeRemoteSettings`, `FakeAppForeground`, `RecordingAppLogger`, `runTest(UnconfinedTestDispatcher())`; "đã khởi động" là `prepare()`, `start()`, `onConsentKnown()` qua hàm `started()` có sẵn. Hàm `funnel(...)` trong test dựng `AdFunnelEvent` với mã giả (`"ca-app-pub-test/1"`, `"interstitial"`, `"com.google.ads.mediation.Adapter"`) để test log không chứa mã đó.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-T1, phễu: tên đúng, bỏ `af_` | Unit test | `TenjinEventsTest.funnelNames_areAfNamesWithoutPrefix`: ba bước (`Eligible`, `ApiCalled`, `Displayed`) × ba loại (`INTERS`, `REWARDED`, `NATIVE`) cho đúng danh sách viết tay `inters_ad_eligible`, `inters_api_called`, `inters_displayed`, `rewarded_ad_eligible`, `rewarded_api_called`, `rewarded_ad_displayed`, `native_ad_eligible`, `native_api_called`, `native_ad_displayed` (không suy từ `toAfEvent`); không tên nào bắt đầu bằng `af_` |
| KB-T1, phễu: N từ 1 tới 20 | Unit test | `TenjinEventsTest.interDisplayedTimes_1to20_only`: N = 1 cho `inters_displayed_1_times`, N = 20 cho `inters_displayed_20_times`, N = 0, 21, -1 cho `null` |
| KB-T1, công tắc từng sự kiện | Unit test | `TenjinEventsTest.everyFunnelEvent_hasItsOwnTenjinSwitch`: chín sự kiện cùng `InterDisplayedTimes` cho mười khóa khác nhau, đúng bảng của bước 3, đều thuộc `TenjinSettings.switches()`, không khóa nào thuộc `AppsFlyerSettings.all()`; mọi `InterDisplayedTimes(N)` cho cùng `IS_LOG_TENJIN_INTERS_DISPLAYED_N_TIMES` |
| KB-T1, khóa từ xa: tên, mặc định bật | Unit test | Hai test có sẵn của `TenjinSettingsTest` với câu đối chứng mới (mục 3.5): mười một tên, mười một hằng, theo thứ tự; mọi khóa boolean, mặc định bật, mô tả đúng dạng. Test mới `TenjinSettingsTest.revenueKey_descriptionUnchanged`: mô tả của `IS_LOG_TENJIN_AD_REVENUE` đúng chữ của T-099 (đối chứng: qua cả trước và sau, test chặn đổi nhầm). Danh sách hôm nay có một tên nên hai câu đối chứng mới đỏ đúng lý do |
| KB-T1, phễu: gửi sau đồng ý, không tham số | Unit test | `TenjinTrackerTest.funnelEvent_sentByNameOnly_afterConsentCheck`: sau `started()`, `log(Eligible(INTERS))` thì đuôi `sink.calls` là `[applyConsent, event]`, `sink.events == ["inters_ad_eligible"]`, `sink.payloads` rỗng (tham số không có đường đi: `TenjinSink.event` chỉ nhận tên) |
| KB-T1, phễu: mọi sự kiện | Unit test | `TenjinTrackerTest.everyFunnelEvent_sentWithItsName`: chín sự kiện cùng `InterDisplayedTimes(1)`, `InterDisplayedTimes(20)` cho `sink.events` đúng mười một tên theo thứ tự; mỗi `event` trong `calls` đứng ngay sau một `applyConsent` |
| KB-T1, phễu: N ngoài khoảng | Unit test | `TenjinTrackerTest.interDisplayedTimesOutOfRange_notSent`: `InterDisplayedTimes(0)`, `(21)` không thêm lời gọi nào (kể cả `applyConsent`); đối chứng `InterDisplayedTimes(20)` ngay sau thì gửi |
| KB-T1, công tắc tắt thì không gửi, đổi giữa phiên có hiệu lực từ sự kiện kế | Unit test | `TenjinTrackerTest.funnelSwitchOff_notSent_thenOnAgain`: tắt `IS_LOG_TENJIN_INTERS_AD_ELIGIBLE` thì `Eligible(INTERS)` không gửi và không đọc đồng ý, `Eligible(NATIVE)` vẫn gửi (đối chứng: khóa riêng từng sự kiện); bật lại thì `Eligible(INTERS)` kế tiếp gửi. Thêm một khối cho `IS_LOG_TENJIN_INTERS_DISPLAYED_N_TIMES` tắt: `InterDisplayedTimes(3)` không gửi, bật lại thì gửi |
| KB-T1, công tắc của doanh thu và phễu độc lập | Unit test | `TenjinTrackerTest.revenueSwitchOff_funnelStillSent`: tắt `IS_LOG_TENJIN_AD_REVENUE` thì doanh thu không gửi mà `Displayed(INTERS)` vẫn gửi |
| KB-T1, không khóa thì không gửi | Unit test | `TenjinTrackerTest.noKey_funnelNeverTouchesSdk`: `TenjinConfig("")`, `prepare`, `start`, `onConsentKnown`, rồi mọi sự kiện phễu: `sink.calls` rỗng. Bước đối chứng (như `noKey_prepareAndStart_neverTouchSdk`): cùng chuỗi lời gọi với `TenjinConfig(SDK_KEY)` trên một `FakeTenjinSink` khác thì `events` của sink đó chứa `inters_ad_eligible`; nhờ vậy test đỏ trên khung rỗng thay vì qua ngay |
| KB-T1 ý i: chưa khởi động thì không gửi | Unit test | `TenjinTrackerTest.funnelBeforeStart_dropped`: `prepare()` và `onConsentKnown()` mà chưa `start()`: sự kiện phễu không gọi sink sau `init`; đối chứng: sau `start()` sự kiện kế tiếp gửi, sự kiện cũ không được gửi bù (`events` chỉ có một tên) |
| KB-T1 ý i: chưa có tín hiệu đồng ý đã rõ thì không gửi (kể cả đường 10 giây) | Unit test | `TenjinTrackerTest.funnelBeforeConsentKnown_dropped`: `prepare()`, `start()` mà chưa `onConsentKnown()` (đúng trạng thái của đường hết 10 giây, plan T-099): không gọi `applyConsent` hay `event`; sau `onConsentKnown()` sự kiện kế tiếp gửi, sự kiện cũ không gửi bù |
| KB-T1 ý d: từ chối thì không gửi | Unit test | `TenjinTrackerTest.funnelConsentRefused_notSent`: `sink.consent = false` từ đầu: mọi sự kiện phễu chỉ gọi `applyConsent`, `events` rỗng, log debug `Tenjin inters_ad_eligible opted out` |
| KB-T1 ý d: đọc đồng ý ngay trước mỗi sự kiện (cổng mở rồi đóng rồi mở) | Unit test | `TenjinTrackerTest.funnelConsentWithdrawnThenGranted_followsEachEvent`: sau `started()` gửi được; đặt `sink.consent = false` thì sự kiện kế không gửi; đặt lại `true` thì sự kiện kế gửi; số `applyConsent` tăng đúng một mỗi sự kiện |
| KB-T1: Tenjin lỗi không làm hỏng gì | Unit test | `TenjinTrackerTest.funnelSdkThrows_isSwallowedAndLogged`: `failOn = "event"`: `log` không ném, WARN đúng `Tenjin inters_ad_eligible failed: IllegalStateException`; `failOn = "applyConsent"` đặt sau `started()` (tracker đã kết nối), rồi gửi một sự kiện: không gọi `event`, WARN đúng `Tenjin consent failed: IllegalStateException`. Không dòng log nào chứa mã đơn vị, định dạng hay nguồn giả |
| KB-T1: Tenjin lỗi không chặn AppsFlyer | Unit test | `FanOutAdFunnelTest.eachTargetGetsEvent_inOrder` (hai đích giả trong test, `appsFlyer` và `tenjin`, ghi vào cùng một danh sách `(tên đích, sự kiện)`; sau một sự kiện danh sách đúng `[(appsFlyer, e), (tenjin, e)]`, nên kiểm được cả việc mỗi đích nhận lẫn thứ tự) và `FanOutAdFunnelTest.throwingTarget_doesNotStopOthers` (đích đầu là lớp có tên trong test, `ThrowingAdFunnel`, ném `IllegalStateException`, đích sau vẫn nhận; WARN đúng `Ad funnel event not logged by ThrowingAdFunnel: IllegalStateException`; chiều ngược lại, đích sau ném, đích đầu vẫn nhận, như `FanOutAdMetricsTest`) |
| KB-T1: phễu của app tới được Tenjin (phần nối Hilt) | Unit test | `AdMetricsModuleTest.adFunnel_reachesAppsFlyerAndTenjin`: gọi thẳng `AdMetricsModule.adFunnel(appsFlyer, tenjin, logger)` với `AppsFlyerTracker` dựng trên `FakeAppsFlyerSink` có khóa giả (đã `prepare`, `start`) và `TenjinTracker` trên `FakeTenjinSink` (đã `started`); một `Displayed(NATIVE)` thì `FakeAppsFlyerSink` có `af_native_ad_displayed` và `FakeTenjinSink.events == ["native_ad_displayed"]`. Đồ thị Hilt của `:app` (không hai binding `AdFunnel`) được kiểm khi biên dịch `:app` trong `scripts/check.sh` |
| Bảng khóa khớp code | Unit test có sẵn | `RemoteConfigKeysTest` của `:app` (sau khi sinh lại hai file tài liệu) |
| Phễu thật tới bảng Tenjin | Kiểm tra thủ công (máy thật: khác, cần build có khóa Tenjin thử, thuộc T-101) | Ở T-101: bản có khóa thử, đồng ý quảng cáo, mở một interstitial; bảng sự kiện của Tenjin có `inters_ad_eligible`, `inters_api_called`, `inters_displayed`, `inters_displayed_1_times` |

Thứ tự commit (bước 3 và 4 của `auto-run.md`): commit test gồm mọi file test của mục 3.2, `FakeTenjinSink.event`, và khung rỗng của code chính để biên dịch được (`TenjinSink.event` trong interface, `TenjinLibSink.event` rỗng, `toTenjinEvent()` trả `null`, `tenjinSwitch()` trả khóa doanh thu, `TenjinSettings` khai báo mười hằng khóa phễu mới (để test biên dịch) nhưng `switches()` và `all()` vẫn chỉ trả khóa doanh thu (nên `TenjinSettingsTest`, `RemoteConfigKeysTest` mới đỏ đúng lý do; `TenjinSwitches.isOn` trả mặc định bật cho khóa chưa theo dõi, nên test công tắc phễu cũng đỏ đúng lý do), `TenjinTracker.log(AdFunnelEvent)` rỗng, `FanOutAdFunnel` không gửi, `AdMetricsModule.adFunnel` trả `AppsFlyerTracker` như cũ). Commit test này phải bỏ `AnalyticsModule.bindAdFunnel` cùng lúc thêm `AdMetricsModule.adFunnel`, không thì Hilt của `:app` báo hai binding `AdFunnel` và không biên dịch. `TenjinSettingsTest.revenueKey_descriptionUnchanged` là test chặn: qua ngay trên khung rỗng, ghi vào nhật ký như vậy. `RemoteConfigKeysTest` mới (11) đỏ trên khung rỗng; hai file tài liệu sinh lại trong commit code. Test phải đỏ đúng lý do: tên `null`, sink không được gọi, danh sách khóa một tên.

**Ngoại lệ viết test trước:** `@Singleton` của `TenjinLibSink` (bước 1): phạm vi Hilt không kiểm được bằng unit test JVM của module; hôm nay chỉ `TenjinTracker` (`@Singleton`) nhận `TenjinSink`, nên hành vi không đổi. `TenjinLibSink.event` gọi SDK thật, không có test JVM (như các hàm khác của lớp, T-099); phần gửi thật kiểm ở T-101.

### 4.1. Chạy thử

Không cần (E dưới 2). Không thêm thư viện hay module; hàm `eventWithName(String)` đã thấy trong `classes.jar` của AAR `com.tenjin:android-sdk:2.0.2` (bộ đệm Gradle trên máy dev, `javap`, 2026-11-17).

Thử thêm, không bắt buộc, cho phần dựng của `AdMetricsModuleTest` (mục 4):

- **Đã chạy thử:** test tạm `core/analytics/src/test/.../analytics/di/TryAdMetricsModuleTest.kt`, không commit, trong worktree tạm từ `b885060`, 2026-11-17: dựng `AppsFlyerTracker` (`FakeAppsFlyerSink`, `AppsFlyerSwitches(settings, backgroundScope)`, `FixedCountry`) và `TenjinTracker` (`FakeTenjinSink`, `FakeAppForeground`) từ gói `di`, gọi `AdMetricsModule.attributionStartup(...)`, gửi `Displayed(NATIVE)` cho `AppsFlyerTracker`, kiểm `toAfEvent()?.name?.removePrefix("af_") == "native_ad_displayed"`.
- **Lệnh:** `./gradlew -q :core:analytics:testDebugUnitTest --tests '*TryAdMetricsModuleTest' --offline` (không phải `scripts/check.sh`: không cần theo quy tắc vì E dưới 2).
- **Kết quả:** qua, 1 test, 0 lỗi: test ở gói `di` dùng được các bản giả `internal` của gói cha và gọi thẳng hàm của `AdMetricsModule`; `FakeAppsFlyerSink.events` có `af_native_ad_displayed`; `FakeTenjinSink.calls` là `[init, applyConsent, connect]`. Worktree tạm đã xóa.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 4 (thu thập dữ liệu: sự kiện mới gửi sang Tenjin), 7 (module core dùng chung `:core:analytics`). Không chạm 5: không đổi hộp thoại đồng ý hay thứ tự khởi tạo SDK, chỉ dùng lại cổng đồng ý của T-099.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Nhiều phần trong một module (`:core:analytics`), có đổi interface nội bộ `TenjinSink` và binding Hilt của `AdFunnel`; ngoài module chỉ test và tài liệu sinh lại |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, ADR-1010 liệt kê tên; AI tự quyết vài chi tiết nhỏ (một khóa chung cho `inters_displayed_{N}_times`, bỏ sự kiện trước đồng ý đã rõ thay vì giữ lại) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi điều kiện gửi có unit test trên bản giả; việc SDK thật gửi tới bảng Tenjin chỉ kiểm bằng tay ở T-101 |
| D. Ảnh hưởng chéo | 2 | Sửa `TenjinTracker`, `AnalyticsModule.kt` mà T-099 vừa tạo; mọi sự kiện phễu của `:core:ads` đi qua lớp chia mới |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu phễu AppsFlyer và lớp chia đã có; một hàm SDK mới của thư viện đã có |
| **Tổng** | 7 (không dùng, chạm điểm chặn) | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `:core:analytics` (`TenjinTracker`, `TenjinSink`, `TenjinSettings`, `AnalyticsModule.kt`); bảng khóa `docs/remote-config-keys.md`, `docs/remote-config-template.json`.
- Task khác trong sprint có thể xung đột: T-101 (người làm, khóa Tenjin thử, không sửa file của plan này). Không plan `approved` hay `draft` nào khác đang sửa các file ở mục 3.2 (đã xem `docs/features/*/plans/`).
- `scripts/ads-plan.py` chỉ có sheet sự kiện AppsFlyer viết tay, nên file Excel kế hoạch quảng cáo sẽ thiếu sự kiện Tenjin (khóa `is_log_tenjin_*` vẫn có trong sheet Remote Config vì đọc từ bảng khóa). Script của dự án mẫu, ngoài phạm vi plan này; đề xuất một ticket riêng hay issue from-app.
- Đề xuất thứ tự chạy: sau T-099 (đã merge). Độc lập với task khác; chạy trước T-101 để lần kiểm trên máy thật của T-101 thấy cả phễu.

## 7. Điều kiện dừng riêng

- Sinh lại bảng khóa mà `docs/remote-config-keys.md` hay `docs/remote-config-template.json` đổi ngoài mười khóa `is_log_tenjin_*` mới: dừng.
- Phải sửa file của `:core:ads` hay đổi thời điểm phát `AdFunnelEvent` để test qua: dừng (ngoài phạm vi, đổi hành vi AppsFlyer và mục 8.3).
- Phải đổi chữ log hay thứ tự cổng của doanh thu (test doanh thu có sẵn đỏ): dừng.

## 8. Câu hỏi cho người duyệt

1. **Một khóa chung cho `inters_displayed_{N}_times`.** Ticket nói "mười khóa còn lại", ADR-1010 mục 6 nói "mỗi sự kiện Tenjin có khóa riêng". AI hiểu hai mươi sự kiện `inters_displayed_{N}_times` là một sự kiện có tham số N trong tên và dùng chung `is_log_tenjin_inters_displayed_n_times`, như khung dùng `is_log_af_inters_displayed_n_times`; tổng mười khóa phễu, khớp ticket. Đề xuất: giữ. Nếu muốn khóa riêng từng N thì thêm 19 khóa và sửa ticket. Trả lời (Nhật, 2026-11-17): giữ khóa chung.
2. **Sự kiện phễu trước đồng ý đã rõ bị bỏ, không gửi bù.** Lần mở đầu ở vùng cần hỏi đồng ý, các sự kiện `*_ad_eligible`, `*_api_called` trước khi người dùng trả lời hộp thoại sẽ không có trên Tenjin (như doanh thu của T-099 và như AppsFlyer trước `start()`). Đề xuất: giữ (đơn giản, đúng câu 21 ý i "chưa gửi gì"). Phương án khác: giữ hàng đợi trong bộ nhớ rồi gửi khi đồng ý, phức tạp hơn và cần spec nói rõ. Trả lời (Nhật, 2026-11-17): giữ, không gửi bù.
3. **AppsFlyer vẫn nhận phễu qua lớp chia.** AppsFlyer tắt (sink rỗng, ADR-1007), nên giữ nó trong `FanOutAdFunnel` không gửi gì, chỉ để đợt đồng bộ mẫu dễ gộp. Đề xuất: giữ. Trả lời (Nhật, 2026-11-17): giữ AppsFlyer trong lớp chia.
4. **`TenjinSdkUseTest` không bỏ qua `.claude/`** (ghi chú rà soát T-099): trên máy dev có worktree lồng trong `.claude/worktrees/`, test quét cả bản chép và đỏ. Plan này không đưa vào phạm vi (không phải điều ticket yêu cầu, và là sửa phần dựng của test có sẵn). Đề xuất: sửa ở một ticket nhỏ riêng, thêm `.claude` vào `SKIPPED_DIRS` của cả `TenjinSdkUseTest` và `NoAppsFlyerSdkTest`; hoặc người duyệt cho phép thêm `TenjinSdkUseTest.kt` vào mục 3.2 của plan này với đúng một thay đổi đó. Phiên `local` dùng worktree ngang hàng (`../task-T-NNN`), nên `.claude/worktrees` rỗng không ảnh hưởng tới phiên, miễn phiên không dùng `isolation: worktree`. Trả lời (Nhật, 2026-11-17): sửa ở ticket nhỏ riêng, không đưa vào plan này.
5. **`@Singleton` của `TenjinLibSink`** đưa vào plan dù T-100 không tiêm `TenjinSink` chỗ khác (phòng lỗi về sau, ngoại lệ viết test trước ở mục 4). Đề xuất: giữ; nếu người duyệt muốn plan chỉ gồm phần ticket nêu thì bỏ dòng đó. Trả lời (Nhật, 2026-11-17): giữ `@Singleton`.
6. **Đồng ý cấp giữa phiên sau lần kết nối bị từ chối** (kế thừa T-099): lần kết nối đầu bị từ chối đồng ý (`connectNow` không gọi `connect`), rồi người dùng đồng ý giữa phiên qua form quyền riêng tư; từ đó sự kiện phễu và doanh thu qua cổng (`applyConsent` trả `true`) và được gửi dù SDK chưa `connect` tới lần app quay lại từ nền. Plan này làm số lượt gửi như vậy nhiều hơn. Đề xuất: chấp nhận, T-101 kiểm trên máy thật SDK xử lý sự kiện trước `connect` ra sao. Trả lời (Nhật, 2026-11-17): chấp nhận, T-101 kiểm trên máy thật.
7. **Duyệt chéo:** plan rủi ro Cao (điểm chặn 4 và 7), `cross_reviewer` để trống; đề nghị Tuấn duyệt chéo như T-099. Trả lời (Nhật, 2026-11-17): Tuấn duyệt chéo.

## 9. Quyết định duyệt

### Phiên bản 1

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn (2026-11-17, cùng một phiên có người theo dõi, AI ghi theo ADR-0048 phần C)
- **Ghi chú:**
  - Bản được duyệt: plan trên PR #364 ở commit `901351f`, sau hai vòng rà soát độc lập (lần 1: 0 chặn, 4 nên sửa, đã sửa; lần 2: 0 chặn, 1 nên sửa, đã sửa cùng hai ghi chú về câu chữ test).
  - Lời của Nhật: "Tôi là Nhật, tôi duyệt plan T-100 phiên bản 1 (PR #364, commit 901351f), đồng ý mọi đề xuất ở mục 8."
  - Lời của Tuấn: "Tôi là Tuấn, tôi duyệt chéo plan T-100 phiên bản 1."; khi được hỏi lời duyệt chéo có đúng do Tuấn tự nói sau khi đã đọc bản này, kể cả mục 8 và ngoại lệ viết test trước, chọn "Đúng, Tuấn duyệt chéo".
  - Trả lời mục 8 (Nhật, 2026-11-17): đồng ý mọi đề xuất; câu 1 khóa chung, câu 2 không gửi bù, câu 3 giữ AppsFlyer trong lớp chia, câu 4 ticket nhỏ riêng, câu 5 giữ `@Singleton`, câu 6 chấp nhận và T-101 kiểm trên máy thật, câu 7 Tuấn duyệt chéo.
  - Người phụ trách sản phẩm (Nhật) đã biết trước khi duyệt, vì plan chạm dữ liệu gửi ra ngoài (`risk-scoring.md`, mục 3).

## 10. Nhật ký thực hiện

### RUN-20261117-02, phiên bản plan 1

- **Kết quả:** đã merge (PR #367, commit `53130c5`, 2026-11-18 07:15 UTC).
- **Nhánh, PR:** `claude/T-100-tenjin-phieu-quang-cao` (từ `origin/main` `6dd929e`), https://github.com/VGL-Studio/camera-pose/pull/367.
- **Đã làm:** bước 1 tới 8 của mục 3.1, đúng 15 file của mục 3.2; sinh lại `docs/remote-config-keys.md`, `docs/remote-config-template.json` (chỉ thêm mười khóa `is_log_tenjin_*`, không dòng nào khác đổi). Commit: `e8cd320` (test viết trước, kèm khung rỗng; bỏ `AnalyticsModule.bindAdFunnel` cùng lúc thêm `AdMetricsModule.adFunnel` trả `AppsFlyerTracker`), `7d8978e` (code). Dòng `bindAppsFlyerSink(impl: NoOpAppsFlyerSink)` giữ nguyên. Phiên chính tự làm bước 3 tới 5 trong worktree ngang hàng `../task-T-100` (cỡ S, không giao phiên con).
- **Test viết trước:** 20 test mới (`TenjinEventsTest` 3, `TenjinTrackerTest` 11, `FanOutAdFunnelTest` 2, `AdMetricsModuleTest` 1, `TenjinSettingsTest.revenueKey_descriptionUnchanged`) cùng ba câu dữ liệu theo mục 3.5 (`RemoteConfigKeysTest` 1 thành 11, hai câu đối chứng của `TenjinSettingsTest`). Trên khung rỗng 20 test đỏ đúng lý do (tên `null`, sink không được gọi, danh sách khóa một tên, lớp chia không gửi); `revenueKey_descriptionUnchanged` là test chặn, qua ngay như plan nói. Test viết chặt bằng đối chứng: `noKey_funnelNeverTouchesSdk`, `interDisplayedTimesOutOfRange_notSent`, `funnelSwitchOff_notSent_thenOnAgain`, `funnelBeforeStart_dropped`, `funnelBeforeConsentKnown_dropped` (5). Test JVM mọi module trên khung rỗng: chỉ các test trên đỏ. Không file test nào đổi sau commit test.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 3 ghi chú: (1) mỗi sự kiện phễu bị chặn ở cổng ghi một dòng debug, logcat có thể dày hơn khi một khóa bị tắt từ xa (đúng mẫu `AppsFlyerTracker`, giữ); (2) `@Singleton` và `TenjinLibSink.event` không có test JVM (ngoại lệ đã duyệt, phần gửi thật kiểm ở T-101); (3) nhật ký đi qua PR báo cáo.
- **Kết quả CI:** `scripts/check.sh` trên máy qua (3592 test JVM, 0 lỗi, 178 ảnh). Lần chạy đầu dừng ở 2 test script `device-scenario` (`a hanging am start counts as no process and is retried`, `the file deadline also leaves room for the evidence`), test đo thời gian không liên quan task; `scripts/tests/run.sh` chạy lại qua, `check.sh` lần hai qua. `pr-check` xanh trên `7d8978e` (lần chạy 44255503564), một vòng CI.
- **Lệch so với plan:** Không có. Chi tiết plan không nêu: hàm dựng sự kiện trong test tên `funnelSteps()`/`allFunnelEvents()` thay vì `funnel(...)`, cùng mã giả của mục 4; test kiểm thêm các dòng debug `Tenjin <tên> before start`, `... before consent known`. Ngoài mục 3.2: không có.
- **Việc còn lại:** T-101 (máy thật, bản có khóa Tenjin thử: bảng sự kiện Tenjin có `inters_ad_eligible`, `inters_api_called`, `inters_displayed`, `inters_displayed_1_times`; câu 6 của mục 8, sự kiện trước `connect` khi đồng ý cấp giữa phiên). Ticket nhỏ riêng cho `.claude` trong `SKIPPED_DIRS` của `TenjinSdkUseTest`, `NoAppsFlyerSdkTest` (câu 4 của mục 8).
- **Cần người xem:** kiểm tay ở T-101 như trên.
