---
ticket: T-083
title: Thẻ native `native_onboarding` ở luồng giới thiệu
status: approved             # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 9.0M         # cỡ M theo số gợi ý của scripts/token-usage.py estimate (2026-10-31); không nâng tay (L-081)
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Tuấn               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-11-01
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-083: THẺ NATIVE `NATIVE_ONBOARDING` Ở LUỒNG GIỚI THIỆU

## 1. Tóm tắt

Thêm vị trí `native_onboarding` (định dạng `native_step`) và đặt `NativeStepAd` vào khe `onboarding_ad_slot` mà T-082 để sẵn giữa vùng trang và nút đáy, ở mọi trang kể cả trang cuối. Ở trang 1, nút "Tiếp" mờ tối đa 5 giây khi quảng cáo được phép và thẻ chưa có quảng cáo; trạng thái "được phép" lấy từ một hàm công khai mới của `:core:ads` (`rememberNativeStepAdAllowed`), trạng thái "đã có quảng cáo" từ `onAdAvailabilityChange` có sẵn của `NativeStepAd`, và phần chờ là một composable của feature (`rememberIntroAdWait`) đã chạy thử với đồng hồ dừng, ra nền rồi trở lại, cờ tắt giữa chừng và dựng lại activity. Vị trí chỉ bật ở lần mở đầu tiên (thuộc tính mới `firstOpenOnly` của `AdPlacement`), để pool `native_step` không nạp ở những lần mở sau khi không còn luồng giới thiệu; staging bật `is_show_native_onboarding`.

Hai phần sửa khung (`rememberNativeStepAdAllowed`, `firstOpenOnly`) có ích cho mọi app có luồng giới thiệu: đề xuất về dự án mẫu (`docs/ops/from-app.md`), ghi vào mô tả PR.

**Phiên bản 2 (2026-10-31).** Phiên bản 1 chưa duyệt được vì hai điểm chặn: chưa có thiết kế của luồng có thẻ (mục 8, câu 1 cũ) và plan T-082 còn là nháp phiên bản 1 (câu 2 cũ), nên số đo ở khung thấp là ước lượng và tên ảnh là đề xuất. Gói thiết kế đã merge (PR #276, `docs/features/onboarding/design/`, README `approved` ngày 2026-10-31) và plan T-082 lên phiên bản 2 (nháp, PR #270). Thay đổi so với phiên bản 1:

- Mục 2, 3.1: số đo thật từ `design/context/OnboardingScreen_<trang>-ad_light.md` và `OnboardingScreen_intro-ad-wait_light.md` (thẻ 363 x 250 không lề ngoài, đỉnh thẻ 533 ở trang 1 tới 3 và 477 ở trang cuối, nút đỉnh 807 mọi frame, hình trang 1 co còn 229), đổi sang số của code (nút 56 nên cộng 4); bảng khung thấp thay ước lượng 198 / 134 dp bằng 222 / 166 dp (README mục 6, plan T-082 phiên bản 2 mục 3.1).
- Mục 3.1: khung chờ của thẻ là khung chờ của khung (`State=Loading`), nút mờ là `PrimaryButton(enabled = false)`, như thiết kế; tên và cấu trúc màn khớp plan T-082 phiên bản 2 (`OnboardingPage`, `adSlot`, `onboarding_ad_slot`, `onboarding_next`, `onboarding_enable_camera`, `nextEnabled`, khe không lề).
- Mục 4: test `<trang>Ad_layoutMatchesDesign` theo `context/`; mỗi ảnh thiết kế của T-083 có screenshot test cùng tên (ADR-0020), ảnh `intro-ad-wait` chụp với đồng hồ dừng vì khung chờ có hiệu ứng chạy mãi (L-029); bỏ ảnh `camera-ad_light_unfolded` (gói không có frame `-ad` mở gập).
- Mục 3.2, 3.5, 6: dữ liệu test cũ theo trạng thái sau các plan của RUN-20261031-02 (T-080 `unit/12`, T-081 đổi `SAMPLE_NATIVE_SMALL` thành `POSES_BOTTOM`), số mã `unit/13`.
- Mục 5: tiêu chí B từ 2 xuống 1. Mục 8: câu 1, 2, 7 đã được thiết kế và plan T-082 trả lời; câu 3, 4, 5, 6, 8 giữ đề xuất.
- Phần chạy thử của phiên bản 1 giữ nguyên; phiên bản 2 không đổi kỹ thuật (mục 4.1).
- Sửa theo rà soát độc lập (2026-11-01, 0 chặn, 5 nên sửa, 9 ghi chú) và câu trả lời của Nhật cho mục 8 (2026-11-01): composable nối `OnboardingAdContent` có test; test của `rememberNativeStepAdAllowed` có controller; `StartupAdChoiceTest` dùng `NATIVE_ONBOARDING`; hai hàm log của ViewModel `internal`; hệ quả của `firstOpenOnly` cho pool vị trí khác (mục 3.5); rời trang 1 là đã dùng lượt chờ; lý do kết thúc `disposed` khi dựng lại; kịch bản emulator bỏ `expect_no`. Bảng đầy đủ ở mục 8.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/onboarding/spec.md` (confirmed 2026-10-31, câu 6, 8) và câu 12 của `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-6 | Vị trí bật và có quảng cáo thì thẻ native 250 dp hiện ở đáy vùng trang, ở mọi trang; nhãn "Ad" luôn thấy rõ | `NativeStepAd(AdPlacement.NATIVE_ONBOARDING, step = page)` trong khe `onboarding_ad_slot` của T-082, ở cả 3 hoặc 4 trang. Cao `NativeStepAdDefaults.height()` (250 dp ở cỡ chữ mặc định, 270 dp ở 2.0). Nhãn "Ad" do khung vẽ (`NativeAdSize.STEP`) |
| KB-6 | Trang 1 hiện, quảng cáo được phép, thẻ chưa có quảng cáo: nút "Tiếp" mờ, không bấm được, tối đa 5 giây; quảng cáo về thì nút dùng được ngay; hết 5 giây thì nút dùng được, thẻ hiện khi quảng cáo về | Một lần chờ mỗi luồng, bắt đầu ở khung hình đầu tiên mà trang 1 hiện với quảng cáo được phép. Kết thúc hẳn khi: có quảng cáo, hết 5 giây, quảng cáo thôi được phép, rời trang 1, app ra nền (mục 8, câu 3, 4, 5). Đã kết thúc thì không chờ lại |
| KB-6 | Quảng cáo không được phép (vị trí tắt, từ chối hay chưa trả lời đồng ý) thì trang 1 không chờ | "Được phép" là đúng cổng mà `NativeStepAd` dùng: đồng ý, `is_show_ads` và `is_show_native_onboarding`, có mã đơn vị (`rememberStepAllowed`). Chưa trả lời đồng ý thì `canRequestAds` là `false`, nên không chờ |
| KB-6 | Trang cuối: thẻ không che nút "Bật camera" và ghi chú; khung thấp 360 x 640 cuộn được, nút luôn thấy | Bố cục của T-082 đặt khe trên ghi chú và nút; plan này đo trên khung thấp và kiểm bằng test (mục 3.1, Màn thấp) |
| KB-6 | Chuyển trang thì thẻ đổi quảng cáo khác nếu pool còn | Khung đã làm (`stepAdStates`, OB-02): `step` là `OnboardingPage`, đổi mỗi lần trang đổi |
| KB-6 | Chưa có, không có quảng cáo hay chưa đồng ý: chỗ của thẻ không làm nút "Tiếp" nhảy, luồng đi tiếp | Nút ghim đáy (T-082) nên không nhảy dù khe cao 0 hay 250 dp. Trang 1 chờ tối đa 5 giây rồi luồng đi tiếp, có hay không có quảng cáo (pool cạn: mục 8, câu 5) |
| KB-6 | Chạm vào thẻ chỉ mở quảng cáo; vùng chạm của "Tiếp" không chồng lên thẻ | Thẻ và nút là hai phần tử riêng của cột, không chồng; test so biên |
| KB-6 | Vị trí tắt thì không có thẻ, không có chỗ trống | `NativeStepAd` không chiếm chỗ khi không được phép (`Box(modifier)`); vì thế modifier của thẻ không có `padding` (mục 3.1, Khe) |
| Mục 5 spec onboarding | Chỉ ở lần mở đầu tiên | `firstOpenOnly = true`: công tắc của vị trí là `is_show_native_onboarding` và dấu lần mở đầu (A-01). Lần mở sau pool `native_step` không nạp (mục 8, câu 6) |
| Ticket, Nạp sẵn | Pool `native_step` ở nhóm nạp sẵn đầu chỉ ở lần mở đầu tiên; kiểm log `Preload group` và thứ tự với hộp thoại đồng ý | Kế hoạch nạp mặc định đã có `NATIVE_STEP` ở nhóm đầu (`AdPreloadPlan`, HD-02); app không đặt kế hoạch riêng. Kế hoạch chỉ chạy sau khi đồng ý (S-22). Lần mở sau, nhóm đầu vẫn ghi `NATIVE_STEP` trong log nhưng không còn pool nào bật nên nhóm qua ngay. Kiểm trên máy thật |

Thiết kế tham chiếu: `docs/features/onboarding/design/` (README `approved`, Nhật, 2026-10-31; PR #276). Frame của ticket này, ảnh trong `screens/`, số liệu trong `context/` cùng tên:

- `OnboardingScreen_intro-ad_light`, `OnboardingScreen_intro-ad_dark` (trang 1 có thẻ);
- `OnboardingScreen_poses-ad_light`, `OnboardingScreen_poses-ad_dark` (trang 2);
- `OnboardingScreen_premium-ad_light`, `OnboardingScreen_premium-ad_dark` (trang 3, cờ premium bật);
- `OnboardingScreen_camera-ad_light`, `OnboardingScreen_camera-ad_dark` (trang cuối);
- `OnboardingScreen_intro-ad-wait_light`, `OnboardingScreen_intro-ad-wait_dark` (trang 1 chờ quảng cáo: thẻ là khung chờ `State=Loading`, nút "Tiếp" là `PrimaryButton(enabled = false)`).

Gói không có frame `-ad` ở khung mở gập; khung thấp có số đo trong README mục 6. Frame không có thẻ (`OnboardingScreen_<trang>_<light|dark>`, `_unfolded`) là của T-082. Chữ trong thẻ là chữ của nhà quảng cáo và của khung (README mục 2, dòng cuối), không có chữ mới của app.

ADR liên quan: ADR-0001 (phụ thuộc module), ADR-0003, ADR-0004, ADR-0013 (đồng ý), ADR-0014 (vị trí mặc định tắt), ADR-0019 (log), ADR-0020 (thiết kế), ADR-0023 (tên khóa), ADR-0026 (AdsKit PARTNER), ADR-0029, ADR-0031.

## 3. Giải pháp

### 3.1. Cách làm

**Phụ thuộc T-082.** Plan T-082 phiên bản 2 (nháp, nhánh `claude/plan-T-082-onboarding-pager`, PR #270, mục 3.1, "Điểm cho T-083") để sẵn: `enum class OnboardingPage(val logName: String) { INTRO("intro"), POSES("poses"), PREMIUM("premium"), CAMERA("camera") }` (công khai, `OnboardingPage.kt`); `OnboardingScreen(uiState, onNext, onBack, onEnableCamera, modifier, adSlot: @Composable (page: OnboardingPage) -> Unit = {})`; khe `onboarding_ad_slot` (một `Box(Modifier.fillMaxWidth())` gọi `adSlot(uiState.page)`, giữa vùng trang `onboarding_page_area` và ghi chú hay nút); nút `onboarding_next` ("Tiếp") và `onboarding_enable_camera` ("Bật camera") ghim đáy; trạng thái `OnboardingUiState.Browsing(pages, index)` (mọi trạng thái mang `pages`, `index`, `page`). Khe là `Box(Modifier.fillMaxWidth())` không lề: khoảng cách phía trên thẻ là lề dưới `Spacing.lg` nằm trong phần cuộn của vùng trang (README mục 3). Plan T-082 phiên bản 2 nói T-083 thêm `nextEnabled: Boolean = true` vào `OnboardingScreen` và giữ test `bottomButton_samePlace_onEveryPage_withAndWithoutAdSlot`. Plan này dùng đúng các tên đó. Plan T-082 đổi các tên này trước khi duyệt thì plan này lên phiên bản mới (mục 7).

**Khung, phần 1: trạng thái "được phép" của thẻ** (`NativeFlowStep.kt`, đặt ngay sau `NativeStepAd`; code đã chạy thử, mục 4.1). `onAdAvailabilityChange` của `NativeStepAd` có sẵn: `true` khi có quảng cáo, `false` khi pool cạn, `false` khi thôi được phép sau khi đã báo `true`. Nhưng nó không báo gì khi chưa bao giờ được phép, nên feature không phân biệt được "đang chờ quảng cáo" với "không được phép". Hàm mới chỉ trả lại đúng cổng mà thẻ dùng:

```kotlin
/**
 * Whether the step card of [placement] may take an ad now: consent given, `is_show_<placement>` on, an ad unit (the
 * gate [NativeStepAd] reads before it takes any space). False with no ads controller, and false as soon as ads stop
 * being allowed (L-061). An app that holds its Next button while the card of the first step waits for an ad (plan
 * T-083) waits only while this is true. Placements of another format are an [IllegalArgumentException].
 */
@Composable
fun rememberNativeStepAdAllowed(placement: AdPlacement): Boolean {
    requireStepFormat(placement.format)
    val ads = LocalAdsController.current ?: return false
    return rememberStepAllowed(placement, ads.canRequestAds, ads::isEnabled) { ads.unitId(it) }
}
```

`rememberStepAllowed` đọc công tắc với giá trị đầu `false`, nên ở khung hình đầu hàm trả `false` rồi mới `true`. Hệ quả: lần chờ bắt đầu chậm một khung hình, không sai.

**Khung, phần 2: vị trí chỉ ở lần mở đầu tiên** (`AdPlacement.kt`, `StartupAdChoice.kt`). Thêm thuộc tính cuối của `AdPlacement` và `AdPlacementDeclaration`:

```kotlin
/**
 * Shown only in the first open since install (A-01): `is_show_<code>` and the first-open mark; a later open reads
 * it as off, so its pool is not filled (plan T-083). Not for an [appWide] placement, which reads the mark its own way.
 */
val firstOpenOnly: Boolean = false,
```

Kiểm khai báo trong `AdPlacementDeclaration.init`: `require(!firstOpenOnly || !appWide) { "Placement $code: firstOpenOnly is not for an app-wide placement" }`. `placementSwitch` (dòng đầu):

```kotlin
if (!placement.appWide) {
    if (!placement.firstOpenOnly) return flags.placementEnabled(placement)
    return combine(flags.placementEnabled(placement), firstOpen) { on, first -> on && first }.distinctUntilChanged()
}
```

`isEnabled` của `AdsController` đi qua `placementSwitch`, nên một chỗ sửa áp cho mọi đường dùng công tắc của vị trí: cổng của thẻ (`rememberStepAllowed`), pool (`NativeAdPools`, `enabledNow`), nạp sẵn (`NativePoolPreloadSlot`), nạp bù khi activity quay lại. Dấu lần mở đầu đọc một lần mỗi process (`firstOpenResult`), trước khi đọc xong thì công tắc chưa có giá trị (như vị trí mở app, test `placementSwitch_waitsForFirstOpen`).

**Hệ quả cho pool của vị trí khác (rà soát độc lập).** `NativeAdPools.init` gộp công tắc của mọi vị trí có pool bằng `combine`, nên chưa phát lần nào khi một công tắc chưa có giá trị. Với `native_onboarding` trong danh sách, `enabledNow` của mọi vị trí có pool (thanh đáy, ô trong lưới) chỉ có giá trị sau khi dấu lần mở đầu đọc xong; trước đó các pool coi là tắt, như lúc công tắc Remote Config chưa về. Chấp nhận: dấu đọc một lần từ DataStore ở đầu process, cùng lúc khung chọn quảng cáo mở app (cũng chờ dấu này), và kế hoạch nạp sẵn chỉ chạy sau khi đồng ý, nên chậm thêm không đáng kể. Không đổi `NativeAdPools`. Mục 3.5 ghi hành vi đổi; mục 4 có test `NativeAdPoolsTest.switchNotKnownYet_poolsWait_thenFollowSwitches`.

Dòng của vị trí (một dòng, đúng luật mà `AdPlacementSourceTest` kiểm):

```kotlin
/** The step card of the onboarding flow, on every page, first open only (spec onboarding, KB-6; spec ads, question 12). */
NATIVE_ONBOARDING("native_onboarding", AdFormat.NATIVE_STEP, firstOpenOnly = true),
```

Đặt cuối nhóm vị trí của app, sau `POSES_IN_COLLECTION` mà T-080 thêm (T-081 đã bỏ `SAMPLE_NATIVE_SMALL`; T-084 không thêm dòng vị trí). T-086 đã merge trước thì đặt sau dòng của T-086.

**Feature, phần chờ ở trang 1** (`IntroAdWait.kt`, mới; code đã chạy thử ở phiên bản 1, mục 4.1; phiên bản 2 thêm hai dòng `DisposableEffect` và `LaunchedEffect(onIntro)` cùng lý do `DISPOSED`, chưa chạy thử, test viết trước ở mục 4 bắt):

```kotlin
internal const val INTRO_AD_WAIT_MILLIS = 5_000L

internal enum class IntroAdWaitEnd(val logName: String) {
    AD("ad"), TIMEOUT("timeout"), NOT_ALLOWED("not allowed"), LEFT_PAGE("left page"), BACKGROUND("background"),
    DISPOSED("disposed"),
}

@Composable
internal fun rememberIntroAdWait(
    onIntro: Boolean,
    allowed: Boolean,
    hasAd: Boolean,
    onStarted: () -> Unit = {},
    onEnded: (IntroAdWaitEnd) -> Unit = {},
    maxMillis: Long = INTRO_AD_WAIT_MILLIS,
): Boolean {
    // Saved: a hold that began is not begun again after the activity is built again.
    var begun by rememberSaveable { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    val currentOnStarted by rememberUpdatedState(onStarted)
    val currentOnEnded by rememberUpdatedState(onEnded)
    val end: (IntroAdWaitEnd) -> Unit = { reason ->
        if (running) {
            running = false
            currentOnEnded(reason)
        }
    }
    // Lifecycle events, not a state read in composition: no frame is drawn in the background (L-091).
    LifecycleEventEffect(Lifecycle.Event.ON_START) { foreground = true }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        foreground = false
        end(IntroAdWaitEnd.BACKGROUND)
    }
    // Built again (rotation, fold) or the screen left while holding: the hold ends with the composition.
    DisposableEffect(Unit) { onDispose { end(IntroAdWaitEnd.DISPOSED) } }
    // Leaving page one uses up the hold, even one that never began (ads allowed only later): back on page one,
    // the button is never held (plan T-083, review; Nhật 2026-11-01).
    LaunchedEffect(onIntro) { if (!onIntro) begun = true }
    val canWait = onIntro && allowed && !hasAd && foreground
    LaunchedEffect(canWait) {
        if (canWait && !begun) {
            begun = true
            running = true
            currentOnStarted()
            delay(maxMillis)
            end(IntroAdWaitEnd.TIMEOUT)
        } else if (!canWait) {
            end(
                when {
                    hasAd -> IntroAdWaitEnd.AD
                    !allowed -> IntroAdWaitEnd.NOT_ALLOWED
                    !onIntro -> IntroAdWaitEnd.LEFT_PAGE
                    else -> IntroAdWaitEnd.BACKGROUND
                },
            )
        }
    }
    // Held from the first frame the hold may begin, so the button never shows enabled for one frame first.
    return canWait && (!begun || running)
}
```

KDoc của hàm như bản chạy thử (mục 4.1). Điểm đã chọn và lý do:

- Giữ ở composable, không ở ViewModel: hai đầu vào (`allowed`, `hasAd`) chỉ có trong composition, và test thời gian chạy bằng `mainClock` của Compose. ViewModel chỉ ghi log.
- Ra nền: dùng sự kiện `ON_STOP` của lifecycle, không đọc `currentStateFlow` bằng `collectAsStateWithLifecycle`. Cách sau ngừng thu đúng lúc app dừng, nên không bao giờ thấy trạng thái nền; khung hình cũng không vẽ khi app ở nền. Đối chứng ở mục 4.1: bỏ phần `ON_STOP` thì test ra nền đỏ. Ra nền là kết thúc chờ, không tạm dừng (mục 8, câu 4). `ON_PAUSE` (hộp thoại của hệ thống, hộp thoại đồng ý) không kết thúc chờ.
- Dựng lại activity (xoay, gập): `begun` lưu bằng `rememberSaveable`, `running` thì không, nên lần chờ đang chạy kết thúc và không bắt đầu lại. Lý do kết thúc ghi vào log là `disposed` (composition cũ bị hủy, `onDispose`); phiên bản 1 không ghi gì ở trường hợp này. Lý do chọn kết thúc thay vì chờ tiếp: thẻ cũng dựng lại và nạp lại như mọi khe native (O-01), nên quảng cáo đã chờ mất; chờ lại 5 giây làm người dùng chờ hai lần.
- Quảng cáo được phép muộn (đồng ý muộn, Nhật 2026-11-01, câu 3): đang ở trang 1 thì chờ 5 giây tính từ lúc được phép. Đã rời trang 1 thì lượt chờ coi như đã dùng (`LaunchedEffect(onIntro)`), quay lại trang 1 nút không mờ.
- Khung hình đầu: trong `rememberIntroAdWait`, nút mờ ngay khi có thể chờ (`!begun`), không sáng một khung rồi mới mờ (đối chứng ở mục 4.1). Trên màn thật, `rememberNativeStepAdAllowed` trả `false` ở khung hình đầu (giá trị đầu của công tắc), nên nút sáng một khung hình rồi mới mờ. Chấp nhận: một khung hình (khoảng 16 ms) không bấm kịp, và nếu bấm kịp thì chỉ là sang trang 2 không chờ.
- Có quảng cáo sẵn trong pool: thẻ có quảng cáo ở khung hình sau, nên nút mờ khoảng một khung hình. Chấp nhận.

**Feature, nối dây** (`OnboardingScreen.kt`). Một composable không trạng thái mới giữa Route và Screen, để test được phần nối mà không cần `LocalAdsController` hay ViewModel (rà soát độc lập):

```kotlin
/**
 * The onboarding screen with the ad card of plan T-083: holds Next on page one while the card waits for an ad
 * (spec onboarding, KB-6). [adsAllowed] is the gate of the card; [adSlot] draws the card and reports whether it has
 * an ad (the real card by default, a fake in tests).
 */
@Composable
internal fun OnboardingAdContent(
    uiState: OnboardingUiState,
    adsAllowed: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onEnableCamera: () -> Unit,
    modifier: Modifier = Modifier,
    onIntroAdWaitStarted: () -> Unit = {},
    onIntroAdWaitEnded: (IntroAdWaitEnd) -> Unit = {},
    adSlot: @Composable (page: OnboardingPage, onAdAvailabilityChange: (Boolean) -> Unit) -> Unit =
        { page, onChange -> OnboardingAdCard(page, onChange) },
) {
    var cardHasAd by remember { mutableStateOf(false) }
    val nextHeld = rememberIntroAdWait(
        onIntro = uiState is OnboardingUiState.Browsing && uiState.page == OnboardingPage.INTRO,
        allowed = adsAllowed,
        hasAd = cardHasAd,
        onStarted = onIntroAdWaitStarted,
        onEnded = onIntroAdWaitEnded,
    )
    OnboardingScreen(
        uiState = uiState,
        onNext = onNext,
        onBack = onBack,
        onEnableCamera = onEnableCamera,
        modifier = modifier,
        nextEnabled = !nextHeld,
        adSlot = { page -> adSlot(page) { cardHasAd = it } },
    )
}
```

`OnboardingRoute` (sau T-082) gọi `OnboardingAdContent(uiState, adsAllowed = rememberNativeStepAdAllowed(AdPlacement.NATIVE_ONBOARDING), onNext = viewModel::onNextClick, onBack = viewModel::onBackClick, onEnableCamera = ..., onIntroAdWaitStarted = viewModel::onIntroAdWaitStarted, onIntroAdWaitEnded = viewModel::onIntroAdWaitEnded)` thay cho `OnboardingScreen(...)`; phần quyền của Route giữ như T-082.

`OnboardingAdCard` (riêng tư trong `OnboardingScreen.kt`, `internal` để test gọi):

```kotlin
/** The step card of [page] (spec onboarding, KB-6): no padding, so it leaves no space while it takes none. */
@Composable
internal fun OnboardingAdCard(page: OnboardingPage, onAdAvailabilityChange: (Boolean) -> Unit = {}) {
    NativeStepAd(
        placement = AdPlacement.NATIVE_ONBOARDING,
        step = page,
        modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.AD_CARD),
        onAdAvailabilityChange = onAdAvailabilityChange,
    )
}
```

**Khe.** Thẻ không có `padding`, như thiết kế (README mục 3, dòng Khe thẻ native: "không có lề ngoài") và plan T-082 phiên bản 2: khi không được phép hay không có `LocalAdsController`, `NativeStepAd` vẽ `Box(modifier = modifier)`, nên `padding` sẽ để lại 24 dp trống, trái tiêu chí "không có chỗ trống của thẻ". Khoảng cách 24 phía trên thẻ là lề dưới của vùng cuộn (T-082), khoảng 24 phía dưới thẻ là `Spacer` trước ghi chú hay nút (T-082). Thẻ rộng hết cột (363 ở khung 411), cao `NativeStepAdDefaults.height()`; khung vẽ viền, bo góc, lề trong 12 (số của `NativeAdDimens`, README mục 3, "ngoài hệ thống"). Tag mới `onboarding_ad_card` trên thẻ.

**Màn hình** (`OnboardingScreen`): thêm tham số `nextEnabled: Boolean = true` (vị trí như plan T-082 đã nêu), truyền cho `PrimaryButton(enabled = nextEnabled)` của nút `onboarding_next`. Nút "Bật camera" không đổi: chỉ trang 1 chờ. Nút mờ là `PrimaryButton(enabled = false)` của `:core:designsystem`, như frame `intro-ad-wait` (README mục 3: nền `onSurface` 0.12, chữ 0.38, viền `outlineVariant`, không bóng; cùng chỗ, cùng cỡ với nút thường). Không viết cứng màu; không đổi `PrimaryButton`.

**ViewModel** (`OnboardingViewModel.kt`): hai hàm ghi log, không đổi trạng thái. Khai báo `internal` vì `IntroAdWaitEnd` là `internal` mà lớp ViewModel công khai (hàm công khai không được lộ kiểu `internal`, rà soát độc lập):

```kotlin
internal fun onIntroAdWaitStarted() = logger.info(LOG_TAG, "Intro ad wait started")

internal fun onIntroAdWaitEnded(reason: IntroAdWaitEnd) = logger.info(LOG_TAG, "Intro ad wait ended: ${reason.logName}")
```

Log của khung (`VGL.Ads`): `Pool native_step created for native_onboarding`, `Step native_onboarding: got a new ad`, `Step native_onboarding: step changed`, `Step native_onboarding: no ad (pool exhausted)`.

**Số đo theo thiết kế (KB-6).** Số của `context/OnboardingScreen_<trang>-ad_light.md` (khung 411 x 891, nút Figma cao 60) và số mà test đo trong code (nút 56, T-007; mọi phần dưới vùng trang ghim theo đáy nên dời xuống 4, vùng trang cao thêm 4, như plan T-082 phiên bản 2 mục 3.1):

| Frame | Phần tử | Thiết kế (y, cao) | Code (y, cao) |
| --- | --- | --- | --- |
| `intro-ad`, `intro-ad-wait` | Vùng trang (gồm lề trong 24 trên, 24 dưới) | 64, 469 | 64, 473 |
| `intro-ad`, `intro-ad-wait` | Hình minh họa (`onboarding_illustration`), rộng 363 | 88, 229 | 88, khoảng 233 (`ShrinkFirstLayout`: 473 - 48 - 168 chữ - 24) |
| `intro-ad`, `intro-ad-wait` | Tiêu đề, giới thiệu | 341, 437 | khoảng 345, 441 |
| `intro-ad`, `poses-ad`, `premium-ad` | Thẻ (`onboarding_ad_card`), rộng 363, trái 24 | 533, 250 | 537, 250 |
| `poses-ad`, `premium-ad`, `camera-ad` | Huy hiệu, tiêu đề, điểm hay lời | 88, 208, 268 | 88, 208, 268 (không đổi so với frame không thẻ: nội dung ngắn hơn vùng trang) |
| `camera-ad` | Vùng trang | 64, 413 | 64, 417 |
| `camera-ad` | Thẻ | 477, 250 | 481, 250 |
| `camera-ad` | Ghi chú (`onboarding_note`), hai dòng | 751, 32 | 755, 32 |
| Mọi frame | Nút (`onboarding_next`, `onboarding_enable_camera`), rộng 363 | 807, 60 | 811, 56 (đáy 867) |

Cỡ chữ 2.0: thẻ 270 (`NativeStepAdDefaults.height()`); ghi chú và chữ của nút cũng lớn hơn, nên plan không ghi số; test `small_withCard_fontScale2_buttonAndNoteInWindow` (mục 4) kiểm phần ghim vẫn nằm trọn trong cửa sổ.

**Màn thấp (KB-6, L-028).** Số đo của README mục 6 (Figma, khung 360 x 640, đo trên bản sao đổi cỡ của frame) và số trong code, khớp plan T-082 phiên bản 2 (mục 3.1, "Phần cho T-083 ở khung thấp có thẻ"). Phiên bản 1 ước 198 / 134 dp vì tính khoảng 24 dưới hàng chấm nằm ngoài phần cuộn; ở thiết kế và ở T-082 phiên bản 2, khoảng này là lề trên của vùng cuộn.

| Khung 360 x 640 | Vùng trang Figma (trừ lề trong 48) | Vùng trang code (trừ lề trong 48) | Nội dung trang cần | Thẻ (code) | Nút (code) |
| --- | --- | --- | --- | --- | --- |
| Trang 1, có thẻ | 218 (170) | 222 (174) | 352 khi hình co tới 160 | y 286 tới 536 | 560 tới 616 |
| Trang cuối, có thẻ | 162 (114) | 166 (118) | 220 | y 230 tới 480; ghi chú 504 tới 536 | 560 tới 616 |

Cách xử lý theo KB-6 và thiết kế (README mục 6: "Không ẩn thẻ, không giảm lề ở khung thấp"): vùng trang cuộn (`onboarding_content` của T-082 đã cuộn), thẻ, ghi chú và nút ghim, không ẩn thẻ. Ở trang 1, hình minh họa co trước (`ShrinkFirstLayout`, tới 160), rồi vùng trang cuộn. Test đo thật trong phiên (mục 4): nút nằm trọn trong cửa sổ, ghi chú không bị thẻ che, cuộn được tới phần tử cuối của trang.

**Bản staging.** `StagingRemoteValuesModule.appPlacementsShown()` thêm `AdPlacement.NATIVE_ONBOARDING` (sau `POSES_IN_COLLECTION` của T-080), cập nhật KDoc (spec onboarding, mục 8: bật lúc phát hành). Không đặt `show_open_ads_first_open`: lần mở đầu vẫn không có quảng cáo toàn màn hình (KB-1).

**Bảng khóa.** `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`: `docs/remote-config-keys.md` và `docs/remote-config-template.json` thêm `is_show_native_onboarding`. Không sửa tay. Khóa trùng tên AdsKit (`AdsSettings.ADSKIT_KEY_NAMES` đã có), nên mô tả không có hậu tố tên mẫu.

**Tài liệu.**

- `docs/ops/manual-test.md`: mục 2 thêm bước luồng giới thiệu có thẻ; mục 11 thêm `is_show_native_onboarding` vào danh sách "không đặt `false` trên Firebase staging" và mục 11.5 (mục 4, Kiểm tra thủ công).
- `docs/ops/ads-guide.md`: dòng 49 ghi vị trí của app `native_onboarding`; thêm `rememberNativeStepAdAllowed` và `firstOpenOnly` vào phần thẻ bước (dòng 78) và phần khai báo vị trí (mục 5).
- `scripts/ads-plan.py`: không sửa. Script đọc mô tả từ chú thích ngay trên dòng của `AdPlacement`; dòng mẫu `native_onboarding` của `EXAMPLE_PLACEMENTS` chỉ dùng khi app chưa có vị trí đó. Việc của người sau khi merge: `python3 scripts/ads-plan.py --keep <file trên Drive>`, `native_onboarding` vào danh sách mã cần tạo của T-075 (mã "Native 5 - Step" Nhật đã tạo; giá trị không vào repo).

**Thứ tự commit trong phiên (L-025).** Bước 3 (test viết trước), một commit gồm test của mục 4, phần dữ liệu test cũ theo dòng enum (`AdPlacementAppTest`, `AdUnitIdsTest`, `StagingRemoteValuesModuleTest`, vì các test đó kiểm phần mới), và khung rỗng để test biên dịch được rồi đỏ ở câu khẳng định:

- `AdPlacement.kt`: thuộc tính `firstOpenOnly` (enum và `AdPlacementDeclaration`, chưa có `require`) và dòng `NATIVE_ONBOARDING`. Đỏ nhờ khung rỗng: `AdPlacementTest.firstOpenOnly_notForAppWide` (chưa ném lỗi), `StartupAdChoiceTest.placementSwitch_firstOpenOnly_followsKeyAndFirstOpen` và `_waitsForFirstOpen` (`placementSwitch` chưa đọc dấu), `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet` (staging chưa bật), `RemoteConfigKeysTest` (bảng khóa chưa sinh lại). `AdPlacementAppTest` (dòng dữ liệu, `nativeOnboarding_firstOpenOnly`) và `AdUnitIdsTest` qua ngay vì chỉ cần dòng enum: test chặn.
- `NativeFlowStep.kt`: `rememberNativeStepAdAllowed` trả `false`. Đỏ: `nativeStepAdAllowed_withController_followsConsentSwitchAndUnit`, `nativeStepAdAllowed_otherFormat_throws`. `_noController_false` qua ngay (test chặn).
- `IntroAdWait.kt`: `INTRO_AD_WAIT_MILLIS`, `IntroAdWaitEnd`, `rememberIntroAdWait` trả `false`. Đỏ: mọi test giữ nút của `IntroAdWaitTest` (`allowedNoAd_*`, `allowedLate_*`, `adComes_*`, `switchOffMidway_*`, `backgroundMidway_*`, `rebuiltMidway_*`, `leftPage_*`, `startedInBackground_*`). `notAllowed_neverHeld`, `allowedAfterLeavingPageOne_noWait` qua ngay (test chặn; đối chứng là `allowedLate_*` và `allowedNoAd_*`).
- `OnboardingScreen.kt`: tham số `nextEnabled` (chưa truyền cho nút), `OnboardingAdContent` gọi thẳng `OnboardingScreen` không giữ nút, `OnboardingAdCard`, tag `onboarding_ad_card`. Đỏ: `nextDisabled_whenNextEnabledFalse`, `OnboardingAdContentTest.pageOne_allowed_nextHeld_releasedWhenCardHasAd`. Bố cục có thẻ (`*Ad_layoutMatchesDesign`, `small_withCard_*`) qua ngay nếu khe của T-082 đúng: test chặn.
- `OnboardingViewModel.kt`: hai hàm rỗng. Đỏ: `introAdWait_logsStartAndEnd`.
- `NativeAdPoolsTest.switchNotKnownYet_poolsWait_thenFollowSwitches` không cần code mới (khẳng định hành vi có sẵn của `combine`): test chặn, ghi trong nhật ký.

Dữ liệu test cũ viết theo trạng thái của nhánh chính lúc chạy: tên `POSES_BOTTOM` thay `SAMPLE_NATIVE_SMALL` (T-081), chuỗi của `AdUnitIdsTest` có `poses_in_collection=unit/12` (T-080). Hàm dựng màn `Screen(state, dark, adSlot, nextEnabled)` của `OnboardingScreenScreenshotTest`: T-082 chưa có hàm này hay chưa có hai tham số thì tạo hay thêm trong commit test.

Bước 4: code, bảng khóa, tài liệu, ảnh chuẩn.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeFlowStep.kt` | `rememberNativeStepAdAllowed(placement)` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | Thuộc tính `firstOpenOnly` (enum và `AdPlacementDeclaration`, kiểm khai báo); dòng `NATIVE_ONBOARDING` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/StartupAdChoice.kt` | `placementSwitch`: vị trí `firstOpenOnly` cộng dấu lần mở đầu |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeFlowStepTest.kt` | Test của `rememberNativeStepAdAllowed`, có và không có controller (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/StartupAdChoiceTest.kt` | Test `placementSwitch_firstOpenOnly_*` với `AdPlacement.NATIVE_ONBOARDING`, đối chứng `POSES_BOTTOM` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdPoolsTest.kt` | Test `switchNotKnownYet_poolsWait_thenFollowSwitches` (mục 3.1, Hệ quả cho pool; mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementTest.kt` | Test kiểm khai báo `firstOpenOnly` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementAppTest.kt` | Dòng `native_onboarding` vào tập vị trí của app; test `nativeOnboarding_firstOpenOnly` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | Thêm `;native_onboarding=unit/13` vào cuối hai chuỗi của `configuredUnits_areParsedPerPlacementKey`, sau `poses_in_collection=unit/12` của T-080 (dữ liệu của test, ADR-0029; số kế tiếp lúc chạy nếu task khác đã dùng 13) |
| Tạo | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/IntroAdWait.kt` | `INTRO_AD_WAIT_MILLIS`, `IntroAdWaitEnd`, `rememberIntroAdWait` |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreen.kt` | `nextEnabled`; `OnboardingAdContent` (giữ `cardHasAd`, gọi `rememberIntroAdWait`); `OnboardingAdCard`; tag `onboarding_ad_card`; Route gọi `OnboardingAdContent` với `rememberNativeStepAdAllowed` |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingViewModel.kt` | `internal` `onIntroAdWaitStarted`, `onIntroAdWaitEnded` (log) |
| Tạo | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/IntroAdWaitTest.kt` | Test của phần chờ, đồng hồ dừng (mục 4) |
| Tạo | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingAdContentTest.kt` | Test phần nối giữa cổng, thẻ và nút, đồng hồ dừng (mục 4) |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreenTest.kt` | Test thẻ, nút, khung thấp, bố cục theo `context/` (mục 4) |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingViewModelTest.kt` | Test log của hai hàm mới |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreenScreenshotTest.kt` | 12 test ảnh mới (mục 4): 10 ảnh cùng tên với frame của T-083, hai ảnh khung thấp |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_poses-ad_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_poses-ad_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_premium-ad_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_premium-ad_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera-ad_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera-ad_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad-wait_light.png` | Trang 1 lúc chờ: khung chờ của thẻ, nút "Tiếp" mờ; chụp với đồng hồ dừng (L-029) |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad-wait_dark.png` | Như trên, tối |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad_small.png` | Khung thấp, trang 1 có thẻ: nút "Tiếp" không bị che |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera-ad_small.png` | Khung thấp, trang cuối có thẻ (nhiều phần ghim nhất) |
| Tạo | `feature/onboarding/src/deviceScenarios/T-083-gioi-thieu-tu-choi-dong-y.txt` | Kịch bản emulator (mục 4) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModule.kt` | `appPlacementsShown()` thêm `NATIVE_ONBOARDING`; KDoc |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModuleTest.kt` | Tập mong đợi thêm `is_show_native_onboarding=true` (mục 3.5) |
| Sửa | `docs/remote-config-keys.md` | Sinh lại bằng `-PremoteConfigKeys.update=true` |
| Sửa | `docs/remote-config-template.json` | Sinh lại cùng lệnh |
| Sửa | `docs/ops/manual-test.md` | Mục 2 và mục 11 (mục 3.1, Tài liệu) |
| Sửa | `docs/ops/ads-guide.md` | Vị trí của app `native_onboarding`; `rememberNativeStepAdAllowed`; `firstOpenOnly` |
| Tạo | `docs/features/onboarding/design/review/OnboardingScreen_intro-ad_light_compare.png` | Sinh bằng `scripts/design-compare.sh onboarding` |
| Tạo | `docs/features/onboarding/design/review/OnboardingScreen_intro-ad_dark_compare.png` | Sinh bằng `scripts/design-compare.sh onboarding` |
| Tạo | `docs/features/onboarding/design/review/OnboardingScreen_poses-ad_light_compare.png` | Sinh bằng `scripts/design-compare.sh onboarding` |
| Tạo | `docs/features/onboarding/design/review/OnboardingScreen_poses-ad_dark_compare.png` | Sinh bằng `scripts/design-compare.sh onboarding` |
| Tạo | `docs/features/onboarding/design/review/OnboardingScreen_premium-ad_light_compare.png` | Sinh bằng `scripts/design-compare.sh onboarding` |
| Tạo | `docs/features/onboarding/design/review/OnboardingScreen_premium-ad_dark_compare.png` | Sinh bằng `scripts/design-compare.sh onboarding` |
| Tạo | `docs/features/onboarding/design/review/OnboardingScreen_camera-ad_light_compare.png` | Sinh bằng `scripts/design-compare.sh onboarding` |
| Tạo | `docs/features/onboarding/design/review/OnboardingScreen_camera-ad_dark_compare.png` | Sinh bằng `scripts/design-compare.sh onboarding` |
| Tạo | `docs/features/onboarding/design/review/OnboardingScreen_intro-ad-wait_light_compare.png` | Sinh bằng `scripts/design-compare.sh onboarding` |
| Tạo | `docs/features/onboarding/design/review/OnboardingScreen_intro-ad-wait_dark_compare.png` | Sinh bằng `scripts/design-compare.sh onboarding` |
| Sửa | `docs/features/onboarding/design/review/summary.md` | `scripts/design-compare/DesignCompare.java` ghi lại khi chạy `scripts/design-compare.sh onboarding`; phiên không sửa tay (như plan T-080, T-082) |
| Sửa | `docs/features/onboarding/design/README.md` | Mục 8, Kết quả đối chiếu: thêm 10 dòng của frame `-ad`, `-ad-wait` (tỷ lệ khác, ảnh so sánh) và nhận xét của phiên, như T-082 ghi dòng của nó. Không sửa mục 1 tới 7 |

Không đổi, đã kiểm:

- `scripts/status.py`, `scripts/ads-plan.py` đọc `AdPlacement.kt` theo luật một dòng; dòng mới là một dòng, đúng luật mà `AdPlacementSourceTest` kiểm (giữ nguyên). Regex của `scripts/release.sh` không nhận mã có `_` hay dòng có thuộc tính: lỗi có từ trước, đã có ở mọi vị trí của app, thuộc T-075 (rà soát độc lập); plan không chạm `scripts/release.sh`.
- `AdsSettings.kt`: `is_show_native_onboarding` đã có trong `ADSKIT_KEY_NAMES`; khóa sinh từ khai báo.
- `AdTelemetryRules.kt`: `NATIVE_STEP` đã ghi `NATIVE_ONBOARDING` cho `ad_format`; không đổi.
- `app/build.gradle.kts` chỉ nêu đường dẫn hai tệp bảng khóa; không đổi.
- `MainActivity`, `AppNavHost.kt`, `MainScreen.kt`: không đổi; luồng vẫn là một đích.
- `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingStringsTest.kt` chỉ nhắc `docs/features/onboarding/design/README.md` (mục 2, bảng chữ) trong KDoc; plan chỉ thêm dòng vào mục 8 của README, không đổi bảng chữ và không thêm chữ của app: không sửa, không bị ảnh hưởng.
- Ảnh chuẩn của T-082 (`OnboardingScreen_<trang>_<light|dark>`, `_small`, `_unfolded`): không đổi; test của chúng dùng `adSlot` mặc định (rỗng) và `nextEnabled` mặc định `true`.
- Test của T-084 trong `AdUnitIdsTest` (`missingLayers_*`, `bottomBarLayers_*`): `NATIVE_STEP` không phải thanh đáy (`hasBottomBarLayers` chỉ đúng với native nhỏ không có `nativeEvery`), nên `native_onboarding` không có lớp; không sửa (mục 3.5).
- `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` đọc `docs/remote-config-keys.md` và `docs/remote-config-template.json` để so với code: không sửa; qua sau khi sinh lại hai tệp bằng `-PremoteConfigKeys.update=true`.
- `scripts/tests/run.sh` nhắc `docs/remote-config-keys.md` (khối `ads-plan`, đọc code thật, không đếm số khóa cố định) và một `summary.md` trong thư mục tạm của test thiết kế: không sửa, không bị ảnh hưởng (như plan T-079, T-080).
- `scripts/device-session.sh` và `.github/workflows/device-session.yml` đọc `summary.md` của thư mục `test-sessions/` của phiên thử, không đọc `docs/features/onboarding/design/review/summary.md`: không sửa.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: không thêm. Khóa `is_show_native_onboarding` (mặc định tắt, ADR-0014) sinh từ khai báo vị trí. Staging bật. Production bật trên Firebase lúc phát hành (việc của người, `docs/ops/manual-test.md` mục 11 và danh sách phát hành T-075).
- Quảng cáo: vị trí mới `native_onboarding` (`native_step`), có trong spec onboarding mục 5 và spec ads câu 12. Không có trang toàn trang (câu 6). Thuộc tính mới `firstOpenOnly` của khung; không đổi định dạng, nhịp hay mã của vị trí khác. Pool của vị trí khác chờ thêm tới khi dấu lần mở đầu đọc xong (mục 3.1, Hệ quả cho pool; mục 3.5).
- Sự kiện phân tích: không thêm. Khung ghi sự kiện quảng cáo của vị trí như mọi vị trí native (`af_native_ad_eligible`, sự kiện tải và hiện; P-07: thẻ bước không ghi `ad_opportunity`).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `LifecycleEventEffect`, `rememberSaveable`, `delay` không cần quyền. Không thêm: `CAMERA` (plan chỉ nhắc nút "Bật camera" có sẵn của T-012, T-082; không gọi API nào cần quyền này).
- **Thư viện và SDK:** Không thêm. `LifecycleEventEffect` và `LocalLifecycleOwner` thuộc `androidx.lifecycle:lifecycle-runtime-compose` (`libs.androidx.lifecycle.runtime.compose`, 2.9.2), đã có trên classpath biên dịch của `:feature:onboarding` qua plugin `template.android.feature` (bản chạy thử biên dịch được). `:core:ads` đã là phụ thuộc của mọi feature qua cùng plugin.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `OnboardingAdContentTest` chạy đồng hồ dừng trên màn đầy đủ của T-082 (cột lấp màn, vùng cuộn, `ShrinkFirstLayout`); bản chạy thử chỉ đo `rememberIntroAdWait` trong một màn nhỏ.
- **Điều kiện chuyển:** một test của `OnboardingAdContentTest` hết giờ chờ hay đỏ chập chờn 2 lần liên tiếp vì cùng một lỗi của đồng hồ.
- **Cách làm thay:** phần thời gian (5 giây, ra nền, dựng lại) chỉ kiểm ở `IntroAdWaitTest` (màn nhỏ, như bản chạy thử). `OnboardingAdContentTest` chạy với `autoAdvance` mặc định và chỉ kiểm phần nối: trang 1 được phép thì nút mờ sau khi rảnh; khe giả gọi `onAdAvailabilityChange(true)` thì nút sáng; trang 2 không mờ; không được phép thì không mờ. Tiêu chí và câu khẳng định giữ nguyên; không thêm file.
- **Điểm chưa chắc thứ hai:** ảnh `intro-ad-wait` chụp khung chờ có hiệu ứng (`NativeAdPlaceholder(..., animated = true)`) với đồng hồ dừng, theo mẫu `PoseDetailScreenScreenshotTest.loading_*` đã có. Mẫu đó chụp vòng chờ, chưa chụp khung chờ của `:core:ads`.
- **Điều kiện chuyển:** test ảnh `intro-ad-wait_*` chạy quá 60 giây, hay ảnh khác nhau giữa hai lần chạy `recordRoborazziDebug` liên tiếp.
- **Cách làm thay:** khe vẽ `NativeAdPlaceholder(NativeAdSize.STEP, animated = false)` (khung chờ tĩnh, độ mờ 1, như ảnh chuẩn `NativeAdPlaceholder_step_light` của khung), vẫn trong `runComposeUiTest` với đồng hồ dừng. Ảnh cùng tên, cùng đường dẫn; không thêm file. Ghi lần chuyển vào nhật ký.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (kết quả ở mục 4.1, dòng Test có sẵn). Hành vi đổi:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Bản staging bật đúng tập vị trí của T-079, T-080 | Tập thêm `is_show_native_onboarding=true` | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet` | Dữ liệu của test thêm một dòng (ADR-0029): `"is_show_native_onboarding" to true`. Ý định giữ: staging chỉ bật điều spec đã chốt |
| `firstOpenAndCombo_areNotSet`: `show_open_ads_first_open`, `is_show_native_after_inter` không đặt | Không đổi | `StagingRemoteValuesModuleTest.firstOpenAndCombo_areNotSet` | Giữ |
| Staging không bật vị trí `gallery_*` | Không đổi; `native_onboarding` không `stagingOnly` | `StagingRemoteValuesModuleTest.noGalleryPlacement_isShown` (tên sau T-081), `firstOpen_choosesNoStartupAd` | Giữ |
| Tập vị trí của app sau T-080 và T-081: `poses_bottom`, `pose_detail_bottom`, `review_interstitial`, `inter_pose_detail`, `poses_in_collection` (cộng dòng của T-086 nếu đã merge) | Thêm `native_onboarding` (`NATIVE_STEP`) | `AdPlacementAppTest.appPlacements_declaredWithSpecFormats` | Dữ liệu thêm `"native_onboarding" to AdFormat.NATIVE_STEP` (ADR-0029); các câu của vòng lặp (không `appWide`, không `stagingOnly`, khóa `is_show_<key>`) giữ và áp cho dòng mới. `noSamplePlacement` (T-081) giữ |
| Chuỗi mã đơn vị của mọi vị trí app trong `AdUnitIdsTest` | Thêm `native_onboarding` | `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` (hai chuỗi, đã theo T-081 và có `poses_in_collection=unit/12` của T-080), `placementWithoutUnit_isReportedMissingAndShowsNoAd` | Dữ liệu thêm `;native_onboarding=unit/13` vào cuối hai chuỗi (ADR-0029). `placementWithoutUnit_...` so với `entries.filterNot { stagingOnly }` nên tự đúng: giữ. Phiên tìm theo chuỗi `poses_in_collection=unit/12`, không theo số dòng |
| Vị trí không phải app-wide đi theo `is_show_<mã>`, không chờ dấu lần mở đầu | Vẫn vậy trừ vị trí `firstOpenOnly` | `StartupAdChoiceTest.placementSwitch_otherPlacements_unchanged` | Giữ: sau T-081 test dùng `POSES_BOTTOM` (đổi tên của T-081), không `firstOpenOnly` |
| Nút "Tiếp" luôn bấm được (T-082) | Mờ ở trang 1 khi chờ quảng cáo | Test của T-082 trong `OnboardingScreenTest` dựng màn với giá trị mặc định | Giữ: `nextEnabled` mặc định `true` |
| `bottomButton_samePlace_onEveryPage_withAndWithoutAdSlot` (T-082) | Không đổi | `OnboardingScreenTest` (T-082) | Giữ, chạy cả với `OnboardingAdCard` thật (cao 0) |
| Bố cục không thẻ khớp thiết kế (T-082: `<trang>_layoutMatchesDesign`, `adSlot_betweenPageAndButton_belowNoteNever`, `adSlot_receivesCurrentPage`) | Không đổi | `OnboardingScreenTest` (T-082) | Giữ: dùng `adSlot` mặc định hay khối 250 dp, `nextEnabled` mặc định |
| Pool của mọi vị trí có pool (thanh đáy, ô trong lưới) có công tắc ngay khi Remote Config có giá trị | Có công tắc sau khi cả Remote Config và dấu lần mở đầu có giá trị (`combine` trong `NativeAdPools.init` chờ công tắc của `native_onboarding`). Chấp nhận: dấu đọc một lần ở đầu process, quảng cáo mở app cũng chờ nó, kế hoạch nạp sẵn chỉ chạy sau khi đồng ý (mục 3.1) | `NativeAdPoolsTest.*` dùng hàm `enabled` giả, mọi công tắc có giá trị ngay | Giữ. Test mới `switchNotKnownYet_poolsWait_thenFollowSwitches` ghi lại hành vi này |

Không câu khẳng định nào phải viết lại theo ADR-0031.

## 4. Kiểm thử

Test viết trước code (bước 3), qua các quy tắc của `scripts/check.sh`. Đồng hồ dừng: `composeRule.mainClock.autoAdvance = false`, đổi trạng thái của test rồi `Snapshot.sendApplyNotifications()` và `mainClock.advanceTimeByFrame()` (bản chạy thử: thiếu `sendApplyNotifications` thì một khung hình không thấy thay đổi). Dựng lại bằng `StateRestorationTester`, bật `autoAdvance` riêng cho lúc dựng lại (mục 4.1).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-6 tiêu chí 2: chờ tối đa 5 giây | Compose UI test (Robolectric, đồng hồ dừng) | `IntroAdWaitTest.allowedNoAd_heldFromFirstFrame_releasedAfterFiveSeconds`: màn nhỏ có `Button(enabled = !held)`; ngay sau `setContent` nút mờ (đối chứng: trả `canWait && running` thì đỏ ở đây); sau 4 900 ms vẫn mờ; sau thêm 200 ms và một khung hình thì bấm được; sự kiện `[start, timeout]` |
| KB-6 tiêu chí 2: quảng cáo về thì dùng được ngay | Compose UI test | `IntroAdWaitTest.adComes_releasedAtOnce_neverHeldAgain`: 1 000 ms rồi `hasAd = true`, một khung hình: bấm được; `hasAd = false` (quảng cáo mất) thì vẫn bấm được; `[start, ad]` |
| KB-6 tiêu chí 3: không được phép thì không chờ | Compose UI test | `IntroAdWaitTest.notAllowed_neverHeld`: `allowed = false`, bấm được từ khung đầu, sau 6 000 ms vẫn vậy, không có sự kiện. Đối chứng: test đầu (được phép thì chờ) |
| Cờ tắt giữa chừng (L-061) | Compose UI test | `IntroAdWaitTest.switchOffMidway_releasedAtOnce_notHeldWhenBackOn`: 1 000 ms rồi `allowed = false`: bấm được ngay; `allowed = true` lại: không chờ lại; `[start, not allowed]` |
| Ra nền giữa lúc chờ (L-091) | Compose UI test (`createAndroidComposeRule<ComponentActivity>`) | `IntroAdWaitTest.backgroundMidway_releasedOnReturn_notHeldAgain`: 1 000 ms, `activityRule.scenario.moveToState(CREATED)`: sự kiện `[start, background]` ngay; `moveToState(RESUMED)`, một khung hình: bấm được; 6 000 ms sau vẫn vậy |
| Dựng lại activity giữa lúc chờ | Compose UI test | `IntroAdWaitTest.rebuiltMidway_notHeldAgain`: `StateRestorationTester`, 1 000 ms (nút mờ), `emulateSavedInstanceStateRestore()`: bấm được, 6 000 ms sau vẫn vậy; sự kiện `[start, disposed]` |
| Đồng ý muộn khi đang ở trang 1 (câu 3, Nhật 2026-11-01) | Compose UI test | `IntroAdWaitTest.allowedLate_onPageOne_heldFiveSecondsFromThen`: `allowed = false` lúc đầu, bấm được; 2 000 ms sau `allowed = true`, một khung hình: nút mờ, `[start]`; 4 900 ms sau đó vẫn mờ; thêm 200 ms: bấm được, `[start, timeout]` |
| Được phép sau khi đã rời trang 1 (rà soát độc lập, Nhật 2026-11-01) | Compose UI test | `IntroAdWaitTest.allowedAfterLeavingPageOne_noWait`: `allowed = false`, `onIntro = true` rồi `false` (sang trang 2); `allowed = true`; `onIntro = true` (quay lại trang 1): bấm được, 6 000 ms sau vẫn vậy, không có sự kiện. Đối chứng: `allowedLate_onPageOne_*` (không rời trang thì chờ). Bỏ `LaunchedEffect(onIntro)` thì đỏ |
| Rời trang 1 rồi quay lại | Compose UI test | `IntroAdWaitTest.leftPage_released_notHeldWhenBack`: `onIntro = false` rồi `true`: bấm được cả hai lần; `[start, left page]` |
| Ra nền trước khi chờ | Compose UI test | `IntroAdWaitTest.startedInBackground_notHeldUntilForeground`: dựng khi activity `CREATED`, nút không mờ, không sự kiện; `RESUMED`: bắt đầu chờ (`[start]`) |
| Cổng "được phép" của khung | Compose UI test (`:core:ads`) | `NativeFlowStepTest.nativeStepAdAllowed_noController_false`: không có `LocalAdsController` thì `false`. `nativeStepAdAllowed_withController_followsConsentSwitchAndUnit`, theo mẫu `NativeFullScreenStepRecreateTest` (`testAdsController(flags = DefaultAdsFlags(settings))`, `CompositionLocalProvider(LocalAdsController provides controller)`), vị trí `GALLERY_NATIVE_STEP` (có mã thử ở staging, không `firstOpenOnly`): chưa mở cổng đồng ý thì `false`; `openConsentGateForTest()` và `is_show_gallery_native_step` bật thì `true`; đặt khóa `false` giữa chừng thì `false` (L-061). `nativeStepAdAllowed_otherFormat_throws`: `GALLERY_NATIVE_MEDIUM` ném `IllegalArgumentException`. `rememberStepAllowed` đã có test (mã đơn vị) và giữ |
| Chỉ ở lần mở đầu (mục 5 spec) | Unit test (`:core:ads`) | `StartupAdChoiceTest.placementSwitch_firstOpenOnly_followsKeyAndFirstOpen` với `AdPlacement.NATIVE_ONBOARDING` (enum không dựng được giá trị giả): `is_show_native_onboarding` bật và dấu `true`: `true`; dấu `false`: `false`; khóa tắt và dấu `true`: `false`. Đối chứng cùng test: `POSES_BOTTOM` (không `firstOpenOnly`, tên sau T-081), khóa bật, dấu `false`: `true`. `placementSwitch_firstOpenOnly_waitsForFirstOpen`: `NATIVE_ONBOARDING`, dấu chưa đọc thì không phát giá trị (như `placementSwitch_waitsForFirstOpen`) |
| Pool của vị trí khác chờ dấu (mục 3.1, 3.5) | Unit test (`:core:ads`) | `NativeAdPoolsTest.switchNotKnownYet_poolsWait_thenFollowSwitches`: hai vị trí có pool, công tắc của vị trí thứ nhất là `MutableSharedFlow` chưa phát, vị trí thứ hai bật, đồng ý có: pool của vị trí thứ hai chưa mở; vị trí thứ nhất phát `false`: pool thứ hai mở. Test chặn (khẳng định hành vi có sẵn) |
| Khai báo `firstOpenOnly` | Unit test | `AdPlacementTest.firstOpenOnly_notForAppWide`: `AdPlacementDeclaration(code, APP_OPEN, appWide = true, firstOpenOnly = true)` ném `IllegalArgumentException`; khai báo `NATIVE_STEP` với `firstOpenOnly` thì không |
| Vị trí của app | Unit test | `AdPlacementAppTest.appPlacements_declaredWithSpecFormats` (dữ liệu thêm dòng); `nativeOnboarding_firstOpenOnly`: `NATIVE_ONBOARDING.firstOpenOnly` là `true`, `key` là `native_onboarding`, khóa `AdsSettings.placementEnabled(it).name` là `is_show_native_onboarding` |
| KB-6 tiêu chí 1: thẻ ở mọi trang, đúng chiều cao | Compose UI test (`NATIVE`, khung `vi-w411dp-h891dp-xxhdpi`) | `OnboardingScreenTest.adSlot_card_onEveryPage_heightOfStepCard`: mỗi trang của danh sách 4 trang, `adSlot` vẽ `NativeAdCard(assets, NativeAdSize.STEP)` giả: khe `onboarding_ad_slot` cao `NativeStepAdDefaults.height()` (250 dp, sai số 1 dp), đáy khe ở trên ghi chú (trang cuối) hay trên nút |
| Bố cục có thẻ khớp thiết kế, trang 1 | Compose UI test (khung `vi-w411dp-h891dp-xxhdpi`, `NATIVE`, sai số 4 dp như T-082) | `OnboardingScreenTest.introAd_layoutMatchesDesign` (`context/OnboardingScreen_intro-ad_light.md`), khe vẽ thẻ giả: `onboarding_page_area` đỉnh 64, cao 473; `onboarding_illustration` đỉnh 88, rộng 363, cao 233 (thiết kế 229; đối chứng: nhỏ hơn 280 của trang không thẻ); `onboarding_title` đỉnh 345; `onboarding_body` đỉnh 441; `onboarding_ad_card` trái 24, đỉnh 537, rộng 363, cao `NativeStepAdDefaults.height()`; `onboarding_next` đỉnh 811, đáy 867 |
| Bố cục có thẻ khớp thiết kế, trang 2 và 3 | Compose UI test | `posesAd_layoutMatchesDesign`, `premiumAd_layoutMatchesDesign` (`context/OnboardingScreen_<poses\|premium>-ad_light.md`): `onboarding_badge` đỉnh 88, `onboarding_title` đỉnh 208, `onboarding_features` (trang 2) hay `onboarding_body` (trang 3) đỉnh 268; thẻ đỉnh 537, cao 250; nút đỉnh 811 |
| Bố cục có thẻ khớp thiết kế, trang cuối | Compose UI test | `cameraAd_layoutMatchesDesign` (`context/OnboardingScreen_camera-ad_light.md`): `onboarding_page_area` cao 417; thẻ đỉnh 481, đáy 731; `onboarding_note` đỉnh 755, đáy 787; `onboarding_enable_camera` đỉnh 811, đáy 867; khoảng đáy thẻ tới đỉnh ghi chú và đáy ghi chú tới đỉnh nút đều 24 (sai số 1) |
| Trang 1 chờ khớp thiết kế | Compose UI test | `introAdWait_layoutMatchesDesign` (`context/OnboardingScreen_intro-ad-wait_light.md`): khe vẽ `NativeAdPlaceholder(NativeAdSize.STEP, animated = false)`, `nextEnabled = false`: khung chờ đỉnh 537, cao 250; `onboarding_next` cùng biên với `introAd_layoutMatchesDesign` (đỉnh 811, rộng 363), `assertIsNotEnabled()` |
| KB-6 tiêu chí 4: thẻ không che nút và ghi chú (trang cuối) | Compose UI test | `OnboardingScreenTest.camera_withCard_noteAndButtonBelowCard`: biên của thẻ, `onboarding_note`, `onboarding_enable_camera` không giao nhau, theo thứ tự trên xuống |
| KB-6 tiêu chí 4, khung thấp | Compose UI test (`vi-w360dp-h640dp-xxhdpi`, `NATIVE`, sai số 4 dp) | `OnboardingScreenTest.small_withCard_buttonInWindow_pageScrolls`, trang 1 và trang cuối (`PAGES_4`), khe vẽ `NativeAdCard(assets, NativeAdSize.STEP)` giả: nút đỉnh 560, đáy 616, nằm trọn trong cửa sổ; `onboarding_page_area` cao 222 (trang 1) và 166 (trang cuối) (README mục 6: 218 / 162 cộng 4); thẻ y 286 tới 536 (trang 1), 230 tới 480 (trang cuối); trang cuối: ghi chú y 504 tới 536, nằm trọn trong cửa sổ, đỉnh ghi chú ≥ đáy thẻ + 24; `performScrollTo()` phần tử cuối của trang (`onboarding_body` trang 1, `onboarding_features` trang cuối) rồi phần tử đó nằm trong biên của `onboarding_page_area`. Đối chứng trong test: trước khi cuộn, phần tử cuối nằm ngoài biên (nội dung 352 / 220 lớn hơn 174 / 118) |
| Khung thấp, cỡ chữ 2.0 | Compose UI test (`vi-w360dp-h640dp-xxhdpi`, `fontScale = 2f`) | `OnboardingScreenTest.small_withCard_fontScale2_buttonAndNoteInWindow`: trang cuối có thẻ giả cao `NativeStepAdDefaults.height()` (270): `onboarding_enable_camera` và `onboarding_note` nằm trọn trong cửa sổ, đỉnh ghi chú ≥ đáy thẻ; `onboarding_page_area` cao hơn 0 và cuộn được tới `onboarding_features`. Ghi số đo vào nhật ký |
| KB-6 tiêu chí 7: vùng chạm của nút không chồng thẻ | Compose UI test | `OnboardingScreenTest.nextButton_touchArea_notOverCard`: biên chạm (`touchBoundsInRoot`) của `onboarding_next` không giao biên thẻ, ở khung 411 x 891 và 360 x 640 |
| Nối cổng, thẻ và nút (rà soát độc lập) | Compose UI test (đồng hồ dừng, `vi-w411dp-h891dp-xxhdpi`) | `OnboardingAdContentTest.pageOne_allowed_nextHeld_releasedWhenCardHasAd`: `OnboardingAdContent(Browsing(PAGES_4, 0), adsAllowed = true, adSlot = { _, report -> fake card; report được giữ lại })`: một khung hình sau `setContent`, `onboarding_next` `assertIsNotEnabled()`; gọi `report(true)`, một khung hình: `assertIsEnabled()`, sự kiện `[start, ad]`. `pageTwo_notHeld`: `Browsing(PAGES_4, 1)`, `adsAllowed = true`: bấm được, 6 000 ms sau vẫn vậy. `notAllowed_notHeld`: trang 1, `adsAllowed = false`: bấm được. Đối chứng: nối `nextEnabled = true` cứng thì test đầu đỏ; nối `hasAd = false` cứng thì câu "sáng ngay" đỏ |
| KB-6 tiêu chí 2: nút mờ khi được bảo | Compose UI test | `OnboardingScreenTest.nextDisabled_whenNextEnabledFalse`: `nextEnabled = false`: `onboarding_next` `assertIsNotEnabled()`, bấm không gọi `onNext`; đối chứng `true` gọi một lần. Nút "Bật camera" ở trang cuối không đổi theo `nextEnabled` |
| KB-6 tiêu chí 6, 8: không được phép thì không chỗ trống | Compose UI test | `OnboardingScreenTest.realCard_noAdsController_takesNoSpace`: `adSlot = { OnboardingAdCard(it) }` không có `LocalAdsController`: khe cao 0 dp, nút cùng chỗ với khi `adSlot` rỗng (đối chứng: một `Box` 250 dp làm nút không đổi chỗ, test của T-082) |
| Log của phần chờ (ADR-0019) | Unit test | `OnboardingViewModelTest.introAdWait_logsStartAndEnd`: `RecordingAppLogger` có `Intro ad wait started` và `Intro ad wait ended: timeout` |
| Giá trị staging | Unit test (testStaging) | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet` (dữ liệu thêm `is_show_native_onboarding=true`); `firstOpenAndCombo_areNotSet` giữ |
| Bảng khóa khớp code | Unit test sẵn có | `:app:testStagingDebugUnitTest` (test bảng khóa) qua sau khi sinh lại |
| Giao diện có thẻ, cùng tên với ảnh thiết kế (ADR-0020) | Screenshot test (Roborazzi, khung `vi-w411dp-h891dp-xxhdpi`, như T-082) | `OnboardingScreenScreenshotTest`, mỗi test một trạng thái `Browsing(PAGES_4, i)` cố định, `adSlot` vẽ `NativeAdCard(assets, NativeAdSize.STEP)` giả (chữ giả như ảnh chuẩn `NativeAdCard_step_*`), `Modifier.fillMaxWidth()`: `introAd_light` gọi `captureRoboImage("src/test/screenshots/OnboardingScreen_intro-ad_light.png") { ... }`; cùng mẫu: `introAd_dark` (`OnboardingScreen_intro-ad_dark.png`), `posesAd_light` (`OnboardingScreen_poses-ad_light.png`, `i = 1`), `posesAd_dark` (`OnboardingScreen_poses-ad_dark.png`), `premiumAd_light` (`OnboardingScreen_premium-ad_light.png`, `i = 2`), `premiumAd_dark` (`OnboardingScreen_premium-ad_dark.png`), `cameraAd_light` (`OnboardingScreen_camera-ad_light.png`, `i = 3`), `cameraAd_dark` (`OnboardingScreen_camera-ad_dark.png`). Thẻ giả là thẻ tĩnh: không có hiệu ứng chạy mãi |
| Trang 1 lúc chờ, cùng tên với ảnh thiết kế (ADR-0020, L-029) | Screenshot test, đồng hồ dừng | Khung chờ của thẻ có hiệu ứng nhấp nháy chạy mãi (`placeholderAlpha`, `infiniteRepeatable`), nên không chụp bằng dạng có khối composable của `captureRoboImage`. Theo mẫu `PoseDetailScreenScreenshotTest.loading_*` (cùng lớp, không thêm `createComposeRule()`; `Screen` là hàm dựng màn của lớp do T-082 viết, plan này thêm hai tham số `adSlot`, `nextEnabled` truyền thẳng cho `OnboardingScreen`): `@OptIn(ExperimentalTestApi::class) fun introAdWait_light() = runComposeUiTest { mainClock.autoAdvance = false; setContent { Screen(Browsing(PAGES_4, 0), dark = false, adSlot = { NativeAdPlaceholder(NativeAdSize.STEP, Modifier.fillMaxWidth()) }, nextEnabled = false) }; mainClock.advanceTimeBy(WAIT_PULSE_CYCLE_MS); onRoot().captureRoboImage("src/test/screenshots/OnboardingScreen_intro-ad-wait_light.png") }`; `introAdWait_dark` cùng mẫu (`OnboardingScreen_intro-ad-wait_dark.png`). `WAIT_PULSE_CYCLE_MS = 1_600L`: một vòng nhấp nháy của khung (`PULSE_MILLIS` 800 lên và xuống), nên khối của khung chờ ở độ mờ 1 như frame `State=Loading`. Khung `vi-w411dp-h891dp-xxhdpi` của lớp áp cho `runComposeUiTest`. Dự phòng ở mục 3.4 |
| Màn hình thấp (L-028) | Screenshot test khung `vi-w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `introAd_small`: `captureRoboImage("src/test/screenshots/OnboardingScreen_intro-ad_small.png") { ... }` (trang 1 có thẻ giả, hình co tới 160, vùng trang cuộn); `cameraAd_small`: `captureRoboImage("src/test/screenshots/OnboardingScreen_camera-ad_small.png") { ... }` (trang cuối có thẻ giả, `PAGES_4`, nhiều phần ghim nhất) |
| Mở gập (ADR-0020) | Không có | Gói thiết kế không có frame `-ad` mở gập (README mục 1); ảnh `_unfolded` không thẻ là của T-082. Bố cục có thẻ ở khung 673 x 841 giống khung 411 (một cột, rộng hết): không thêm ảnh |
| Đúng thiết kế | Screenshot test cùng tên với từng ảnh thiết kế của T-083 (10 ảnh trên), `scripts/design-compare.sh onboarding`, rà soát độc lập theo mục 7 của mẫu thiết kế; README mục 8 ghi kết quả | `OnboardingScreen_<intro\|poses\|premium\|camera>-ad_<light\|dark>.png`, `OnboardingScreen_intro-ad-wait_<light\|dark>.png`. Lệch được phép (README mục 6): nút cao 56 thay 60 (phần dưới vùng trang dời 4), chữ trong thẻ (README mục 6; thẻ giả dùng chữ giả của ảnh chuẩn `NativeAdCard_step_*`, như Figma). Ảnh thiết kế của ticket khác (T-082): `OnboardingScreen_intro_light`, `OnboardingScreen_intro_dark`, `OnboardingScreen_poses_light`, `OnboardingScreen_poses_dark`, `OnboardingScreen_premium_light`, `OnboardingScreen_premium_dark`, `OnboardingScreen_camera_light`, `OnboardingScreen_camera_dark`, `OnboardingScreen_intro_light_unfolded`, `OnboardingScreen_camera_light_unfolded`; T-012 (bỏ ở T-082): `OnboardingScreen_default_light`, `OnboardingScreen_default_dark` |
| KB-6: từ chối đồng ý thì không chờ, luồng đi được, không có thẻ | Kiểm tra thủ công (emulator: `feature/onboarding/src/deviceScenarios/T-083-gioi-thieu-tu-choi-dong-y.txt`) | `fresh`, `consent decline 30`, `wait id=onboarding_next 60`, `tap id=onboarding_next`, `wait id=onboarding_page_poses 3` (không chờ 5 giây: nút bấm được ngay), chạm "Tiếp" tới `onboarding_enable_camera`, chạm, `need id=camera_poses 15` |
| KB-6: đồng ý thì trang 1 chờ không quá 5 giây, thẻ ở mọi trang, đổi quảng cáo khi chuyển trang, nhãn "Ad" rõ | Kiểm tra thủ công (máy thật: quảng cáo thật; trình chạy kịch bản chưa có lệnh chấp nhận đồng ý) | Bản staging, xóa dữ liệu app, mở, đồng ý. Đếm thời gian nút "Tiếp" mờ (không quá 5 giây); thẻ ở cả 3 hoặc 4 trang, quảng cáo khác nhau giữa hai trang kề; log `VGL.Onboarding` `Intro ad wait started` và `ended: ad` hay `timeout`. Không bấm vào quảng cáo |
| Nạp sẵn và thứ tự với đồng ý | Kiểm tra thủ công (máy thật: quảng cáo thật) | Lần mở đầu: log `VGL.Ads` có hộp thoại đồng ý trước, rồi `Preload group 1/2 started: APP_OPEN, NATIVE_STEP`, `Pool native_step created for native_onboarding`. Mở lại lần 2 (đã qua luồng): không có `Pool native_step created for native_onboarding` |
| Ra nền giữa lúc chờ trên máy | Kiểm tra thủ công (máy thật: quảng cáo thật) | Ở trang 1 lúc nút mờ, bấm Home rồi mở lại: nút bấm được ngay; log `Intro ad wait ended: background` |
| Mục 2 và 11 của `manual-test.md` đúng | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc hai mục |

**Ngoại lệ viết test trước:** bảng khóa sinh lại (`docs/remote-config-keys.md`, `docs/remote-config-template.json`), `manual-test.md`, `ads-guide.md`: tài liệu sinh từ code hay tài liệu; tệp kịch bản emulator (chạy ở `device-session` cuối phiên, không thất bại trước được); ảnh chuẩn và ảnh so sánh (ghi ở bước 4).

### 4.1. Chạy thử

- **Đã chạy thử:** `rememberNativeStepAdAllowed` (khung) và `rememberIntroAdWait` (feature) đúng như mục 3.1, cùng test tạm `IntroAdWaitTryTest` (8 test, không commit) trên nhánh tạm `tmp/T-083-try` từ `origin/main` (4a5ee7e), ngày 2026-10-31. Chạy trong worktree của agent (không tạo được worktree thứ hai vì agent bị giới hạn trong worktree của mình); commit tạm chỉ ở nhánh tạm, đã xóa. T-082 chưa có code nên test chạy trên màn nhỏ (một `Button(enabled = !held)`), không trên màn giới thiệu.
- **Lệnh:** `./gradlew :feature:onboarding:testDebugUnitTest --tests '*IntroAdWaitTryTest'`, rồi `scripts/check.sh` (qua khóa chung của các phiên soạn plan).
- **Kết quả:**
  - Lần 1 (17:53 UTC): 4 trên 8 đỏ. Đổi trạng thái từ test rồi `advanceTimeByFrame()` không thấy thay đổi; thêm `Snapshot.sendApplyNotifications()` trước mỗi khung hình thì qua. Bài học cho phiên: ghi ở mục 4.
  - Lần 2: `rebuiltMidway_notHeldAgain` đỏ: với đồng hồ dừng, `emulateSavedInstanceStateRestore()` không dựng lại (không có lần vẽ nào sau lệnh). Bật `autoAdvance` riêng cho lệnh dựng lại: qua; đồng hồ chỉ đi thêm 56 ms (1 056 ms so với 1 000 ms), không nhảy qua 5 giây.
  - Thiết kế đầu đọc nền bằng `lifecycle.currentStateFlow.collectAsStateWithLifecycle()`: sai vì luồng ngừng thu đúng lúc app dừng. Đổi sang `LifecycleEventEffect(ON_STOP)` trước lần chạy đầu.
  - Lần 3 (17:56 UTC): 8 trên 8 qua. `backgroundMidway` chạy khoảng 4,9 giây thật (Robolectric `moveToState`), các test khác dưới 0,1 giây.
  - Đối chứng (mỗi lần một thay đổi rồi trả lại): `begun` dùng `remember` thay `rememberSaveable`: `rebuiltMidway_notHeldAgain` đỏ. Bỏ hai `LifecycleEventEffect`: `backgroundMidway_releasedOnReturn_notHeldAgain` đỏ ở câu `[start, background]`. Trả `canWait && running`: `allowedNoAd_heldFromFirstFrame_releasedAfterFiveSeconds` đỏ ở câu nút mờ ngay khung đầu. Cho chờ lại (`!running` thay `!begun`): `switchOffMidway`, `leftPage`, `adComes`, `rebuiltMidway` đỏ.
  - `scripts/check.sh` (qua `check-locked.sh`, xong 18:19 UTC, nhánh tạm có code và test tạm): `BUILD SUCCESSFUL in 5m 27s`, `Test JVM: 2432 test, 0 lỗi. Ảnh screenshot: 138`, `./feature/onboarding: 47 test, 0 lỗi` (39 test có sẵn và 8 test tạm), `./core/ads: 1100 test, 0 lỗi`, `== Cổng chất lượng: QUA`, `check.sh exit=0`. Không có lỗi ktlint hay lint ở hai file mới.
  - Chưa chạy thử: `firstOpenOnly` (biến thể nhỏ của `placementSwitch` có sẵn, E 1) và màn đầy đủ của T-082 (phương án dự phòng ở mục 3.4).
- **Sau rà soát (2026-11-01):** hai dòng mới của `rememberIntroAdWait` (`DisposableEffect` ghi `disposed`, `LaunchedEffect(onIntro)` dùng lượt chờ khi rời trang 1) và `OnboardingAdContent` chưa chạy thử: chỉ dùng API đã chạy thử ở phiên bản 1 (`LaunchedEffect`, `remember`) cộng `DisposableEffect` có sẵn trong module khác; test viết trước ở mục 4 bắt (`allowedAfterLeavingPageOne_noWait`, `rebuiltMidway_notHeldAgain`, `OnboardingAdContentTest`), phương án dự phòng ở mục 3.4.
- **Phiên bản 2 (2026-10-31): không chạy thử thêm.** Kỹ thuật không đổi so với phiên bản 1: phần chờ và cổng "được phép" như trên; số đo mới là số của bố cục T-082 (cột lấp màn, `weight(1f)`, `ShrinkFirstLayout` có từ T-012) cộng một khe cao 250, suy từ `context/` và luật cộng 4 của nút 56 mà plan T-082 phiên bản 2 dùng; ảnh `intro-ad-wait` chụp với đồng hồ dừng theo mẫu đã có trong dự án (`PoseDetailScreenScreenshotTest.loading_*`, `CameraScreenScreenshotTest.starting_*`), dự phòng ở mục 3.4. Màn đầy đủ chưa có code (T-082 chưa chạy) nên số đo của mục 4 chưa đo được trên code; phiên ghi số đo thật vào nhật ký.
- **Test có sẵn (`scripts/existing-tests.py`, chạy lại trên nhánh plan phiên bản 2, 2026-10-31):** 552 test, 8 "Cần xem trước" (cùng các test của phiên bản 1, đếm `NativeAdCellTest` một lần ít hơn), 238 "Nên xem"; kết luận như phiên bản 1, cộng các test đổi tên của T-081 ở mục 3.5. Phiên bản 1: 552 test dùng tên của các file plan sửa, đa số chỉ vì dùng `AdPlacement`. Nhóm "Cần xem trước" (9): các test `..._withoutAdsController_takesNoSpace`, `InListAdsTest`, `NativeAdCellTest`, `NativeFullScreenOverlayTest`, `RewardedActionTest`, `PosesScreenTest`, `PoseDetailScreenTest` đo khe của vị trí khác; plan không đổi vị trí hay khe nào của chúng, giữ. Nhóm "Nên xem" liên quan: `StartupAdChoiceTest.placementSwitch_*` (giữ; vị trí của chúng không `firstOpenOnly`), `AdPlacementTest.*` (giữ; thêm một test), `AdPlacementSourceTest.releaseScript_readsEveryShippedPlacement` (giữ; dòng mới một dòng), `AdPreloadPlanTest.fromPlacements_*` (giữ; `NATIVE_STEP` đã có qua `GALLERY_NATIVE_STEP`), `NativeFlowStepTest.stepAllowed_*` (giữ), `RemoteConfigKeysTest.*` (qua sau khi sinh lại bảng khóa), `StagingRemoteValuesModuleTest.*` (mục 3.5), `OnboardingScreenScreenshotTest.default_*` (T-082 bỏ; không thuộc plan này).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (thêm vị trí quảng cáo `native_onboarding`, ở lần mở đầu cạnh trang xin quyền camera); 7 (sửa `:core:ads`: hàm công khai mới, thuộc tính mới của `AdPlacement`, công tắc của vị trí). Hộp thoại đồng ý (điểm 5) không đổi: plan chỉ đọc cổng có sẵn.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | `:core:ads`, `:feature:onboarding`, `:app` (staging), bảng khóa, tài liệu |
| B. Độ rõ của yêu cầu | 1 | Spec đã chốt (câu 6, 8), thiết kế đã duyệt có đủ 10 frame và số đo khung thấp; còn câu hỏi về lúc bắt đầu, ra nền, pool cạn, lần mở đầu (mục 8, câu 3 tới 6) |
| C. Khả năng kiểm chứng tự động | 2 | Phần chờ, cổng, công tắc có test JVM; quảng cáo thật, đồng ý chấp nhận và thứ tự nạp chỉ kiểm trên máy thật |
| D. Ảnh hưởng chéo | 3 | Sửa `placementSwitch` mà mọi vị trí đi qua; T-080, T-084, T-086 cùng sửa `AdPlacement.kt`, staging, bảng khóa; phụ thuộc chữ ký của T-082 |
| E. Độ mới của kỹ thuật | 2 | Lần đầu dự án giữ nút theo thời gian trong composition với sự kiện lifecycle và đồng hồ dừng; đã chạy thử |
| **Tổng** | 11 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdPlacement.kt` (khai báo, thuộc tính mới), `StartupAdChoice.kt` (`placementSwitch` của mọi vị trí), `NativeFlowStep.kt`, `StagingRemoteValuesModule.kt` và test, `docs/remote-config-keys.md`, `docs/remote-config-template.json`, `docs/ops/manual-test.md`, `docs/ops/ads-guide.md`, `AdPlacementAppTest.kt`, `AdUnitIdsTest.kt`, `AdPlacementTest.kt`.
- Task khác có thể xung đột (theo danh sách `docs/runs/RUN-20261031-02.md` trên nhánh `claude/run-20261031-02`: T-080, T-084, T-081, T-085, có thể thêm T-088 và T-082; trạng thái các plan trên nhánh chính ngày 2026-10-31):
  - **T-082** (plan phiên bản 2, nháp, PR #270): tạo màn nhiều trang mà plan này nối vào. File trùng: `OnboardingScreen.kt`, `OnboardingScreenTest.kt`, `OnboardingScreenScreenshotTest.kt`, `OnboardingViewModel.kt`, `OnboardingViewModelTest.kt`, `docs/features/onboarding/design/README.md` (mục 8), `review/summary.md`. Phải merge trước. T-082 thêm `passOnboarding()` vào `AppLaunchTest`, `SettingsNavigationTest` và dòng `tap_if id=onboarding_next` vào 7 tệp kịch bản: sau plan này vẫn đúng, vì bản staging trên emulator chưa trả lời hay từ chối đồng ý (`consent decline 30` ở mọi tệp), nên quảng cáo không được phép và trang 1 không chờ.
  - **T-080** (approved, phiên bản 3, chạy đầu RUN-20261031-02): thêm `POSES_IN_COLLECTION("poses_in_collection", AdFormat.NATIVE_IN_COLLECTION, nativeEvery = 8)` vào `AdPlacement.kt`; `poses_in_collection=unit/12` vào cuối hai chuỗi của `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey`; dòng của `AdPlacementAppTest`; tập `inList` của `AdPlacementTest.inListGalleryPlacements_declareNativeEvery8`; `appPlacementsShown()` và tập mong đợi của `StagingRemoteValuesModuleTest`; bảng khóa; `manual-test.md` mục 11.1; `ads-guide.md`. Plan này chạy sau, viết dữ liệu theo trạng thái đó (mục 3.1, 3.2, 3.5): dòng enum sau `POSES_IN_COLLECTION`, `native_onboarding=unit/13`.
  - **T-084** (approved): `AdUnitIds.kt` (`hasBottomBarLayers`, `missingLayers`), bảy test mới trong `AdUnitIdsTest`, `ads-guide.md`, `manual-test.md` mục 11.5. Không thêm vị trí, không sửa `configuredUnits_areParsedPerPlacementKey`. `native_step` không có lớp nên test của T-084 không đổi. `manual-test.md`: plan này thêm `is_show_native_onboarding` vào danh sách của mục 11 và mục con kế tiếp còn trống nếu T-084 đã dùng 11.5.
  - **T-081** (approved): bỏ `SAMPLE_NATIVE_SMALL` và đổi tên test sang `POSES_BOTTOM` (kể cả `StartupAdChoiceTest`, plan T-081 mục 3.1 nhắc T-083); hai chuỗi của `AdUnitIdsTest` viết lại không còn `sample_native_small`; `AdPlacementAppTest.noSamplePlacement`; `StagingRemoteValuesModuleTest.noGalleryPlacement_isShown`; bảng khóa. Plan này không dùng `SAMPLE_NATIVE_SMALL`; test mới của `StartupAdChoiceTest` dùng khai báo giả.
  - **T-085** (approved): `FeatureFlag.PREMIUM_POSES`, `StagingFeatureFlagsModule`, bảng khóa (`feature_premium_poses_enabled`). Không trùng file Kotlin với plan này; chỉ trùng bảng khóa sinh lại. Trang `PREMIUM` có hay không theo cờ là việc của T-082; plan này chỉ dùng `OnboardingPage`.
  - **T-088** (có plan, PR #277, approved theo phiên chính ngày 2026-11-01): bỏ `FeatureFlag.CAMERA`, sửa `StagingFeatureFlagsModule`, sinh lại bảng khóa. Chỉ trùng bảng khóa.
  - **T-086** (nháp): có thể thêm vị trí rewarded vào `AdPlacement.kt`, `AdPlacementAppTest`, `AdUnitIdsTest`, staging, bảng khóa. Đã merge trước thì dòng `NATIVE_ONBOARDING` đặt sau dòng của T-086 và số mã lấy số kế tiếp.
- Bảng khóa (`docs/remote-config-keys.md`, `docs/remote-config-template.json`) sinh lại ở T-080, T-081, T-085 và có thể T-088 trước plan này; plan này chỉ thêm `is_show_native_onboarding` (mục 7).
- Đề xuất thứ tự chạy: sau mọi task của RUN-20261031-02 và sau T-082, trong một phiên sau (T-082 chưa có trong danh sách đã duyệt). Cùng phiên với T-082 thì ngay sau T-082. Mỗi task đọc `AdPlacement.kt`, `AdUnitIdsTest.kt` ở trạng thái lúc chạy và tìm theo chuỗi, không theo số dòng.

## 7. Điều kiện dừng riêng

- Plan T-082 đã merge mà chữ ký `OnboardingScreen`, tên `OnboardingPage`, tag `onboarding_ad_slot`, `onboarding_next`, `onboarding_enable_camera` hay vị trí của khe (giữa vùng trang và ghi chú hay nút) khác mục 3.1: dừng (điều kiện 3), soạn plan phiên bản mới.
- Đo ở khung thấp mà nút hay ghi chú không nằm trọn trong cửa sổ khi có thẻ, hay vùng trang lệch số của mục 3.1 (222 / 166 dp) quá 4 dp: dừng, không tự giảm lề hay ẩn thẻ (thiết kế đã chốt giữ cuộn, README mục 6).
- Bảng khóa sinh lại có thay đổi ngoài `is_show_native_onboarding` (và khóa của task đã merge trước): dừng.
- Test có sẵn của `placementSwitch` cho vị trí app-wide hay vị trí thường đỏ: dừng, không sửa test.

## 8. Câu hỏi cho người duyệt

1. **Thiết kế: đã trả lời** (PR #276, README `approved`, Nhật 2026-10-31). Có đủ 10 frame `-ad`, `-ad-wait`; thẻ không lề ngoài, khoảng cách trên thẻ là lề dưới của vùng cuộn; khung chờ của thẻ là `State=Loading` của khung; nút mờ là `PrimaryButton(enabled = false)`. Không có frame `-ad` mở gập. Plan theo đúng gói (mục 2, 3.1, 4).
2. **Khớp plan T-082: đã trả lời** theo plan T-082 phiên bản 2 (PR #270): cùng tên `OnboardingPage`, `adSlot`, `onboarding_ad_slot`, `onboarding_next`, `onboarding_enable_camera`, `nextEnabled`; khe không lề; số khung thấp 222 / 166 dp. Còn điều kiện, không phải câu hỏi: plan T-082 được duyệt và merge trước; T-082 đổi tên trước khi duyệt thì plan này lên phiên bản mới (mục 7).
3. **Lúc bắt đầu chờ: đã trả lời** (Nhật, 2026-11-01): đồng ý đề xuất. 5 giây tính từ lúc quảng cáo được phép, khi đang ở trang 1. Trường hợp rà soát nêu thêm (được phép khi người dùng đã rời trang 1): theo đề xuất của rà soát, rời trang 1 là đã dùng lượt chờ, quay lại trang 1 nút không mờ (mục 3.1; test `allowedLate_onPageOne_heldFiveSecondsFromThen`, `allowedAfterLeavingPageOne_noWait`).
4. **Ra nền giữa lúc chờ: đã trả lời** (Nhật, 2026-11-01): đồng ý, ra nền thì dừng hẳn việc chờ.
5. **Pool cạn trong lúc chờ: đã trả lời** (Nhật, 2026-11-01): đồng ý, pool cạn vẫn chờ đủ 5 giây (pool vẫn thử lại); không thêm lý do `no ad`.
6. **"Chỉ ở lần mở đầu tiên": đã trả lời** (Nhật, 2026-11-01): đồng ý, `firstOpenOnly` theo dấu `ads_first_open_done` (A-01). Người dùng thoát app giữa luồng ở lần mở đầu thì lần sau luồng hiện lại không có thẻ: chấp nhận.
7. **Màn thấp: đã trả lời** (README mục 6): không ẩn thẻ, không giảm lề ở khung 360 x 640; vùng trang còn 218 / 162 dp trên Figma (222 / 166 trong code) và cuộn, nút và ghi chú luôn thấy. Plan theo đúng (mục 3.1, 4, 7).
8. **Đề xuất về dự án mẫu: đã trả lời** (Nhật, 2026-11-01): đồng ý. `rememberNativeStepAdAllowed` và `firstOpenOnly` ghi vào mô tả PR theo `docs/ops/from-app.md`; dự án mẫu quyết tên.

**Rà soát độc lập phiên bản 2 (2026-11-01, agent ngữ cảnh mới, chỉ đọc): 0 chặn, 5 nên sửa, 9 ghi chú.** Đã sửa trong phiên bản 2:

| Loại | Điểm | Đã sửa ở |
| --- | --- | --- |
| Nên sửa 1 | `OnboardingViewModel` công khai lộ `internal enum IntroAdWaitEnd` | Mục 3.1, ViewModel: hai hàm `internal` |
| Nên sửa 2 | `StartupAdChoiceTest` không dựng được `AdPlacement` giả | Mục 3.2, 4: dùng `NATIVE_ONBOARDING`, đối chứng `POSES_BOTTOM` |
| Nên sửa 3 | `rememberNativeStepAdAllowed` và phần nối ở Route chưa có test đỏ khi sai | Mục 3.1 (`OnboardingAdContent`), 3.2, 3.4, 4 (`nativeStepAdAllowed_withController_*`, `OnboardingAdContentTest`) |
| Nên sửa 4 | Kịch bản emulator `expect_no id=onboarding_ad_card` sai vì node cao 0 vẫn có tag | Mục 4: bỏ, dựa vào `wait id=onboarding_page_poses 3` |
| Nên sửa 5 | `firstOpenOnly` làm `NativeAdPools.init` chờ dấu lần mở đầu | Mục 3.1 (Hệ quả cho pool), 3.3, 3.5, 4 (`NativeAdPoolsTest.switchNotKnownYet_*`) |
| Ghi chú | Câu "`release.sh` đọc được" sai (regex không nhận `_` hay thuộc tính, lỗi có từ trước, T-075) | Mục 3.1, 3.2: "theo `AdPlacementSourceTest`" |
| Ghi chú | Câu "không sáng một khung" chỉ đúng trong `rememberIntroAdWait` | Mục 3.1: trên màn thật nút sáng một khung rồi mới mờ, chấp nhận |
| Ghi chú | Dòng cỡ chữ 2.0 ở bảng khung thấp là số đoán | Mục 3.1 bỏ số; mục 4 thêm `small_withCard_fontScale2_buttonAndNoteInWindow` |
| Ghi chú | Hàm dựng `Screen(state, dark, adSlot, nextEnabled)` có thể chưa có ở T-082 | Mục 3.1, Thứ tự commit |
| Ghi chú | Chữ ký `OnboardingPage` của T-082 phiên bản 2 có `logName` | Mục 3.1, Phụ thuộc |
| Ghi chú | Khung rỗng của commit test và test nào đỏ nhờ chúng chưa đủ | Mục 3.1, Thứ tự commit |
| Ghi chú | T-088 đã có plan | Mục 6 |
| Ghi chú | `review/summary.md` ghi hai chỗ trái nhau | Mục 3.2: một dòng |
| Ghi chú | Chưa ghi lý do kết thúc chờ khi dựng lại activity | Mục 3.1: lý do `disposed`, test `rebuiltMidway_notHeldAgain` |
| Ghi chú | Trường hợp được phép sau khi rời trang 1 | Câu 3 trên |

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

Phiên bản 1: chưa duyệt (chờ thiết kế và plan T-082 phiên bản 2).

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Tuấn, **duyệt chéo:** Nhật
- **Ghi chú:** Nhật chọn "Duyệt, thêm vào phiên" khi AI hỏi, ngày 2026-11-01; T-083 là task 7 của RUN-20261031-02, sau T-082. Nhật biết ba phần code mới chưa chạy thử (`OnboardingAdContent`, lý do `DISPOSED` khi dựng lại activity, rời trang 1 là đã dùng lượt chờ): test viết trước bắt lỗi, phương án dự phòng ở mục 3.4. Câu 1 tới 8 ở mục 8 đã trả lời.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
