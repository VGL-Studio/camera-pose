---
ticket: T-083
title: Thẻ native `native_onboarding` ở luồng giới thiệu
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 9.0M         # cỡ M theo số gợi ý của scripts/token-usage.py estimate (2026-10-31); không nâng tay (L-081)
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-083: THẺ NATIVE `NATIVE_ONBOARDING` Ở LUỒNG GIỚI THIỆU

## 1. Tóm tắt

Thêm vị trí `native_onboarding` (định dạng `native_step`) và đặt `NativeStepAd` vào khe `onboarding_ad_slot` mà T-082 để sẵn giữa vùng trang và nút đáy, ở mọi trang kể cả trang cuối. Ở trang 1, nút "Tiếp" mờ tối đa 5 giây khi quảng cáo được phép và thẻ chưa có quảng cáo; trạng thái "được phép" lấy từ một hàm công khai mới của `:core:ads` (`rememberNativeStepAdAllowed`), trạng thái "đã có quảng cáo" từ `onAdAvailabilityChange` có sẵn của `NativeStepAd`, và phần chờ là một composable của feature (`rememberIntroAdWait`) đã chạy thử với đồng hồ dừng, ra nền rồi trở lại, cờ tắt giữa chừng và dựng lại activity. Vị trí chỉ bật ở lần mở đầu tiên (thuộc tính mới `firstOpenOnly` của `AdPlacement`), để pool `native_step` không nạp ở những lần mở sau khi không còn luồng giới thiệu; staging bật `is_show_native_onboarding`.

**Điểm chặn trước khi duyệt:** chưa có thiết kế của luồng có thẻ (mục 8, câu 1), và plan T-082 còn là nháp (mục 8, câu 2). Hai phần sửa khung (`rememberNativeStepAdAllowed`, `firstOpenOnly`) có ích cho mọi app có luồng giới thiệu: đề xuất về dự án mẫu (`docs/ops/from-app.md`), ghi vào mô tả PR.

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

Thiết kế tham chiếu: chưa có. Spec mục 4 nói luồng cần thiết kế mới trên Figma có trạng thái `ad` và trang 1 lúc chờ. Plan T-082 đã liệt kê các frame cho T-083: `OnboardingScreen_<trang>-ad_<light|dark>` và `OnboardingScreen_intro-ad-wait_<light|dark>`. Điểm chặn ở mục 8, câu 1.

ADR liên quan: ADR-0001 (phụ thuộc module), ADR-0003, ADR-0004, ADR-0013 (đồng ý), ADR-0014 (vị trí mặc định tắt), ADR-0019 (log), ADR-0020 (thiết kế), ADR-0023 (tên khóa), ADR-0026 (AdsKit PARTNER), ADR-0029, ADR-0031.

## 3. Giải pháp

### 3.1. Cách làm

**Phụ thuộc T-082.** Plan T-082 (nháp, nhánh `claude/plan-T-082-onboarding-pager`, PR #270) để sẵn: `enum class OnboardingPage { INTRO, POSES, PREMIUM, CAMERA }`; `OnboardingScreen(uiState, onNext, onBack, onEnableCamera, modifier, adSlot: @Composable (page: OnboardingPage) -> Unit = {})`; khe `onboarding_ad_slot` (một `Box(Modifier.fillMaxWidth())` gọi `adSlot(uiState.page)`, giữa vùng trang `onboarding_page_area` và ghi chú hay nút); nút `onboarding_next` ("Tiếp") và `onboarding_enable_camera` ("Bật camera") ghim đáy; trạng thái `OnboardingUiState.Browsing(pages, index)`. Plan này dùng đúng các tên đó. Plan T-082 lên phiên bản mới thì plan này phải khớp lại (mục 8, câu 2).

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

Dòng của vị trí (một dòng, `scripts/release.sh` đọc được, `AdPlacementSourceTest`):

```kotlin
/** The step card of the onboarding flow, on every page, first open only (spec onboarding, KB-6; spec ads, question 12). */
NATIVE_ONBOARDING("native_onboarding", AdFormat.NATIVE_STEP, firstOpenOnly = true),
```

Đặt sau `REVIEW_INTERSTITIAL` (và sau dòng mà T-080, T-084, T-086 thêm, nếu đã merge).

**Feature, phần chờ ở trang 1** (`IntroAdWait.kt`, mới; code đã chạy thử, mục 4.1):

```kotlin
internal const val INTRO_AD_WAIT_MILLIS = 5_000L

internal enum class IntroAdWaitEnd(val logName: String) {
    AD("ad"), TIMEOUT("timeout"), NOT_ALLOWED("not allowed"), LEFT_PAGE("left page"), BACKGROUND("background"),
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
- Dựng lại activity (xoay, gập): `begun` lưu bằng `rememberSaveable`, `running` thì không, nên lần chờ đang chạy kết thúc và không bắt đầu lại. Thẻ cũng dựng lại và nạp lại như mọi khe native (O-01).
- Khung hình đầu: nút mờ ngay khi có thể chờ (`!begun`), không sáng một khung rồi mới mờ. Đối chứng ở mục 4.1.
- Có quảng cáo sẵn trong pool: thẻ có quảng cáo ở khung hình sau, nên nút mờ khoảng một khung hình. Chấp nhận.

**Feature, Route** (`OnboardingScreen.kt`, `OnboardingRoute`). Sau T-082, Route gọi `OnboardingScreen(...)`; plan này thêm:

```kotlin
var cardHasAd by remember { mutableStateOf(false) }
val adsAllowed = rememberNativeStepAdAllowed(AdPlacement.NATIVE_ONBOARDING)
val nextHeld = rememberIntroAdWait(
    onIntro = uiState is OnboardingUiState.Browsing && uiState.page == OnboardingPage.INTRO,
    allowed = adsAllowed,
    hasAd = cardHasAd,
    onStarted = viewModel::onIntroAdWaitStarted,
    onEnded = viewModel::onIntroAdWaitEnded,
)
OnboardingScreen(
    uiState = uiState,
    // ... onNext, onBack, onEnableCamera as in T-082
    nextEnabled = !nextHeld,
    adSlot = { page -> OnboardingAdCard(page, onAdAvailabilityChange = { cardHasAd = it }) },
)
```

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

**Khe.** Plan T-082 gợi ý `Modifier.fillMaxWidth().padding(top = Spacing.lg)` cho thẻ. Plan này bỏ `padding`: khi không được phép hay không có `LocalAdsController`, `NativeStepAd` vẽ `Box(modifier = modifier)`, nên `padding` để lại 24 dp trống, trái tiêu chí "không có chỗ trống của thẻ". Khoảng cách giữa vùng trang và thẻ theo thiết kế (mục 8, câu 1); thiết kế cần khoảng cách thì đặt ở phần cuối nội dung cuộn của trang, không ở thẻ. Tag mới `onboarding_ad_card` trên thẻ.

**Màn hình** (`OnboardingScreen`): thêm tham số `nextEnabled: Boolean = true` (vị trí như plan T-082 đã nêu), truyền cho `PrimaryButton(enabled = nextEnabled)` của nút `onboarding_next`. Nút "Bật camera" không đổi: chỉ trang 1 chờ. Màu mờ là màu `disabled` của `PrimaryButton` trong `:core:designsystem`, không viết cứng.

**ViewModel** (`OnboardingViewModel.kt`): hai hàm ghi log, không đổi trạng thái:

```kotlin
fun onIntroAdWaitStarted() = logger.info(LOG_TAG, "Intro ad wait started")

fun onIntroAdWaitEnded(reason: IntroAdWaitEnd) = logger.info(LOG_TAG, "Intro ad wait ended: ${reason.logName}")
```

Log của khung (`VGL.Ads`): `Pool native_step created for native_onboarding`, `Step native_onboarding: got a new ad`, `Step native_onboarding: step changed`, `Step native_onboarding: no ad (pool exhausted)`.

**Màn thấp (KB-6, L-028).** Chưa đo được vì T-082 chưa có code; số dưới đây là ước lượng theo số của plan T-082 (lề trên 56, chỉ báo 8 và cách 24, ghi chú khoảng 40 và cách 24, cách 24 trước nút, nút 56, lề dưới 24), thẻ 250 không lề:

| Khung 360 x 640 | Phần cố định | Vùng trang còn lại |
| --- | --- | --- |
| Trang 1 tới kế cuối, có thẻ | 56 + 8 + 24 + 250 + 24 + 56 + 24 = 442 | khoảng 198 dp, cuộn |
| Trang cuối, có thẻ | 56 + 8 + 24 + 250 + 24 + 40 + 24 + 56 + 24 = 506 | khoảng 134 dp, cuộn |
| Trang cuối, có thẻ, cỡ chữ 2.0 (thẻ 270) | trên 526 | dưới 114 dp, cuộn |

Cách xử lý theo KB-6: vùng trang cuộn (`onboarding_content` của T-082 đã cuộn), thẻ, ghi chú và nút ghim, không ẩn thẻ. Hình minh họa ở trang 1 co trước (`ShrinkFirstLayout`). Test đo thật trong phiên (mục 4): nút nằm trọn trong cửa sổ, ghi chú không bị thẻ che, cuộn được tới phần tử cuối của trang. Thiết kế muốn giảm lề trên ở khung thấp thì ghi trong gói thiết kế (mục 8, câu 7).

**Bản staging.** `StagingRemoteValuesModule.appPlacementsShown()` thêm `AdPlacement.NATIVE_ONBOARDING`, cập nhật KDoc (spec onboarding, mục 8). Không đặt `show_open_ads_first_open`: lần mở đầu vẫn không có quảng cáo toàn màn hình (KB-1).

**Bảng khóa.** `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`: `docs/remote-config-keys.md` và `docs/remote-config-template.json` thêm `is_show_native_onboarding`. Không sửa tay. Khóa trùng tên AdsKit (`AdsSettings.ADSKIT_KEY_NAMES` đã có), nên mô tả không có hậu tố tên mẫu.

**Tài liệu.**

- `docs/ops/manual-test.md`: mục 2 thêm bước luồng giới thiệu có thẻ; mục 11 thêm `is_show_native_onboarding` vào danh sách "không đặt `false` trên Firebase staging" và mục 11.5 (mục 4, Kiểm tra thủ công).
- `docs/ops/ads-guide.md`: dòng 49 ghi vị trí của app `native_onboarding`; thêm `rememberNativeStepAdAllowed` và `firstOpenOnly` vào phần thẻ bước (dòng 78) và phần khai báo vị trí (mục 5).
- `scripts/ads-plan.py`: không sửa. Script đọc mô tả từ chú thích ngay trên dòng của `AdPlacement`; dòng mẫu `native_onboarding` của `EXAMPLE_PLACEMENTS` chỉ dùng khi app chưa có vị trí đó. Việc của người sau khi merge: `python3 scripts/ads-plan.py --keep <file trên Drive>`, `native_onboarding` vào danh sách mã cần tạo của T-075 (mã "Native 5 - Step" Nhật đã tạo; giá trị không vào repo).

**Thứ tự commit trong phiên.** Bước 3 (test viết trước): test của mục 4, cùng phần dữ liệu test cũ theo dòng enum (`AdPlacementAppTest`, `AdUnitIdsTest`, `StagingRemoteValuesModuleTest`) ở commit test, vì các test đó kiểm phần mới. Bước 4: code, bảng khóa, tài liệu, ảnh chuẩn.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeFlowStep.kt` | `rememberNativeStepAdAllowed(placement)` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | Thuộc tính `firstOpenOnly` (enum và `AdPlacementDeclaration`, kiểm khai báo); dòng `NATIVE_ONBOARDING` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/StartupAdChoice.kt` | `placementSwitch`: vị trí `firstOpenOnly` cộng dấu lần mở đầu |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeFlowStepTest.kt` | Test của `rememberNativeStepAdAllowed` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/StartupAdChoiceTest.kt` | Test `placementSwitch_firstOpenOnly_*` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementTest.kt` | Test kiểm khai báo `firstOpenOnly` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementAppTest.kt` | Dòng `native_onboarding` vào tập vị trí của app; test `nativeOnboarding_firstOpenOnly` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | Thêm `native_onboarding=unit/<số kế tiếp>` vào chuỗi mã (dữ liệu của test, ADR-0029) |
| Tạo | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/IntroAdWait.kt` | `INTRO_AD_WAIT_MILLIS`, `IntroAdWaitEnd`, `rememberIntroAdWait` |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreen.kt` | `nextEnabled`; `OnboardingAdCard`; tag `onboarding_ad_card`; Route nối `adSlot`, `rememberNativeStepAdAllowed`, `rememberIntroAdWait` |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingViewModel.kt` | `onIntroAdWaitStarted`, `onIntroAdWaitEnded` (log) |
| Tạo | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/IntroAdWaitTest.kt` | Test của phần chờ, đồng hồ dừng (mục 4) |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreenTest.kt` | Test thẻ, nút, khung thấp (mục 4) |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingViewModelTest.kt` | Test log của hai hàm mới |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreenScreenshotTest.kt` | Ảnh trạng thái `ad` và `ad-wait` (dưới) |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_poses-ad_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_poses-ad_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_premium-ad_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_premium-ad_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera-ad_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera-ad_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad-wait_light.png` | Trang 1 lúc chờ: khung chờ tĩnh của thẻ, nút "Tiếp" mờ |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad-wait_dark.png` | Như trên, tối |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad_small.png` | Khung thấp, trang 1 có thẻ: nút "Tiếp" không bị che |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera-ad_small.png` | Khung thấp, trang cuối có thẻ (nhiều phần ghim nhất) |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera-ad_light_unfolded.png` | Khung mở gập 673 x 841, khi gói thiết kế có frame này (ADR-0020) |
| Tạo | `feature/onboarding/src/deviceScenarios/T-083-gioi-thieu-tu-choi-dong-y.txt` | Kịch bản emulator (mục 4) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModule.kt` | `appPlacementsShown()` thêm `NATIVE_ONBOARDING`; KDoc |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModuleTest.kt` | Tập mong đợi thêm `is_show_native_onboarding=true` (mục 3.5) |
| Sửa | `docs/remote-config-keys.md` | Sinh lại bằng `-PremoteConfigKeys.update=true` |
| Sửa | `docs/remote-config-template.json` | Sinh lại cùng lệnh |
| Sửa | `docs/ops/manual-test.md` | Mục 2 và mục 11 (mục 3.1, Tài liệu) |
| Sửa | `docs/ops/ads-guide.md` | Vị trí của app `native_onboarding`; `rememberNativeStepAdAllowed`; `firstOpenOnly` |
| Tạo | `docs/features/onboarding/design/review/OnboardingScreen_intro-ad_light_compare.png` | Sinh bằng `scripts/design-compare.sh onboarding`; cùng cách cho mọi ảnh thiết kế `-ad` và `-ad-wait` có trong gói |
| Sửa | `docs/features/onboarding/design/review/summary.md` | Bảng kết quả của `design-compare.sh` |

Không đổi, đã kiểm:

- `scripts/release.sh`, `scripts/status.py`, `scripts/ads-plan.py` đọc `AdPlacement.kt` theo luật một dòng; dòng mới là một dòng, thuộc tính sau định dạng như KDoc của enum cho phép. `AdPlacementSourceTest` kiểm luật này và giữ nguyên. Plan không chạm `scripts/release.sh`.
- `AdsSettings.kt`: `is_show_native_onboarding` đã có trong `ADSKIT_KEY_NAMES`; khóa sinh từ khai báo.
- `AdTelemetryRules.kt`: `NATIVE_STEP` đã ghi `NATIVE_ONBOARDING` cho `ad_format`; không đổi.
- `app/build.gradle.kts` chỉ nêu đường dẫn hai tệp bảng khóa; không đổi.
- `docs/features/onboarding/design/review/summary.md`: `scripts/design-compare/DesignCompare.java` ghi file này; plan chỉ chạy script, không sửa (như plan T-080, T-082).
- `MainActivity`, `AppNavHost.kt`, `MainScreen.kt`: không đổi; luồng vẫn là một đích.
- `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` đọc `docs/remote-config-keys.md` và `docs/remote-config-template.json` để so với code: không sửa; qua sau khi sinh lại hai tệp bằng `-PremoteConfigKeys.update=true`.
- `scripts/tests/run.sh` nhắc `docs/remote-config-keys.md` (khối `ads-plan`, đọc code thật, không đếm số khóa cố định) và một `summary.md` trong thư mục tạm của test thiết kế: không sửa, không bị ảnh hưởng (như plan T-079, T-080).
- `scripts/device-session.sh` và `.github/workflows/device-session.yml` đọc `summary.md` của thư mục `test-sessions/` của phiên thử, không đọc `docs/features/onboarding/design/review/summary.md`: không sửa.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: không thêm. Khóa `is_show_native_onboarding` (mặc định tắt, ADR-0014) sinh từ khai báo vị trí. Staging bật. Production bật trên Firebase lúc phát hành (việc của người, `docs/ops/manual-test.md` mục 11 và danh sách phát hành T-075).
- Quảng cáo: vị trí mới `native_onboarding` (`native_step`), có trong spec onboarding mục 5 và spec ads câu 12. Không có trang toàn trang (câu 6). Thuộc tính mới `firstOpenOnly` của khung; không đổi định dạng, nhịp hay pool của vị trí khác.
- Sự kiện phân tích: không thêm. Khung ghi sự kiện quảng cáo của vị trí như mọi vị trí native (`af_native_ad_eligible`, sự kiện tải và hiện; P-07: thẻ bước không ghi `ad_opportunity`).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `LifecycleEventEffect`, `rememberSaveable`, `delay` không cần quyền. Không thêm: `CAMERA` (plan chỉ nhắc nút "Bật camera" có sẵn của T-012, T-082; không gọi API nào cần quyền này).
- **Thư viện và SDK:** Không thêm. `LifecycleEventEffect` và `LocalLifecycleOwner` thuộc `androidx.lifecycle:lifecycle-runtime-compose` (`libs.androidx.lifecycle.runtime.compose`, 2.9.2), đã có trên classpath biên dịch của `:feature:onboarding` qua plugin `template.android.feature` (bản chạy thử biên dịch được). `:core:ads` đã là phụ thuộc của mọi feature qua cùng plugin.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** test chờ 5 giây trên màn đầy đủ của T-082 (Route, ViewModel Hilt giả) với `mainClock.autoAdvance = false` có thể không ổn định: bản chạy thử chỉ đo `rememberIntroAdWait` trong một màn nhỏ.
- **Điều kiện chuyển:** test thời gian của `OnboardingScreenTest` dựng cả màn hết giờ chờ hay đỏ chập chờn 2 lần liên tiếp vì cùng một lỗi của đồng hồ.
- **Cách làm thay:** test thời gian chỉ chạy trên `rememberIntroAdWait` trong một màn nhỏ (`IntroAdWaitTest`, như bản chạy thử); test của màn đầy đủ dùng `autoAdvance` mặc định, kiểm nút theo `nextEnabled` truyền vào và kiểm Route truyền đúng `nextEnabled = !held` (một test với `held` giả qua tham số). Tiêu chí và câu khẳng định của mục 4 giữ nguyên; không thêm file.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (kết quả ở mục 4.1, dòng Test có sẵn). Hành vi đổi:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Bản staging bật đúng tập vị trí của T-079, T-080 | Tập thêm `is_show_native_onboarding=true` | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet` | Dữ liệu của test thêm một dòng (ADR-0029): `"is_show_native_onboarding" to true`. Ý định giữ: staging chỉ bật điều spec đã chốt |
| `firstOpenAndCombo_areNotSet`: `show_open_ads_first_open`, `is_show_native_after_inter` không đặt | Không đổi | `StagingRemoteValuesModuleTest.firstOpenAndCombo_areNotSet` | Giữ |
| Tập vị trí của app (5 tới 7 dòng, tùy T-080, T-084, T-086) | Thêm `native_onboarding` (`NATIVE_STEP`) | `AdPlacementAppTest.appPlacements_declaredWithSpecFormats` | Dữ liệu thêm `"native_onboarding" to AdFormat.NATIVE_STEP` (ADR-0029) |
| Chuỗi mã đơn vị của mọi vị trí app trong `AdUnitIdsTest` | Thêm `native_onboarding` | `AdUnitIdsTest` (hai chuỗi dòng 15, 22) | Dữ liệu thêm `native_onboarding=unit/<số kế tiếp>` (ADR-0029) |
| Vị trí không phải app-wide đi theo `is_show_<mã>`, không chờ dấu lần mở đầu | Vẫn vậy trừ vị trí `firstOpenOnly` | `StartupAdChoiceTest.placementSwitch_otherPlacements_unchanged` | Giữ: test dùng `SAMPLE_NATIVE_SMALL`, không `firstOpenOnly` |
| Nút "Tiếp" luôn bấm được (T-082) | Mờ ở trang 1 khi chờ quảng cáo | Test của T-082 trong `OnboardingScreenTest` dựng màn với giá trị mặc định | Giữ: `nextEnabled` mặc định `true` |
| `bottomButton_samePlace_onEveryPage_withAndWithoutAdSlot` (T-082) | Không đổi | `OnboardingScreenTest` (T-082) | Giữ, chạy cả với `OnboardingAdCard` thật (cao 0) |

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
| Dựng lại activity giữa lúc chờ | Compose UI test | `IntroAdWaitTest.rebuiltMidway_notHeldAgain`: `StateRestorationTester`, 1 000 ms (nút mờ), `emulateSavedInstanceStateRestore()`: bấm được, 6 000 ms sau vẫn vậy |
| Rời trang 1 rồi quay lại | Compose UI test | `IntroAdWaitTest.leftPage_released_notHeldWhenBack`: `onIntro = false` rồi `true`: bấm được cả hai lần; `[start, left page]` |
| Ra nền trước khi chờ | Compose UI test | `IntroAdWaitTest.startedInBackground_notHeldUntilForeground`: dựng khi activity `CREATED`, nút không mờ, không sự kiện; `RESUMED`: bắt đầu chờ (`[start]`) |
| Cổng "được phép" của khung | Compose UI test (`:core:ads`) | `NativeFlowStepTest.nativeStepAdAllowed_noController_false`: không có `LocalAdsController` thì `false`. `nativeStepAdAllowed_otherFormat_throws`: `GALLERY_NATIVE_MEDIUM` ném `IllegalArgumentException`. `rememberStepAllowed` đã có test (đồng ý, công tắc, mã đơn vị) và giữ |
| Chỉ ở lần mở đầu (mục 5 spec) | Unit test (`:core:ads`) | `StartupAdChoiceTest.placementSwitch_firstOpenOnly_followsKeyAndFirstOpen`: khai báo giả `firstOpenOnly`, `is_show_<mã>` bật và dấu `true`: `true`; dấu `false`: `false`; khóa tắt và dấu `true`: `false`. `placementSwitch_firstOpenOnly_waitsForFirstOpen`: dấu chưa đọc thì không phát giá trị (như `placementSwitch_waitsForFirstOpen`). Đối chứng: cùng khai báo không `firstOpenOnly`, dấu `false`: `true` |
| Khai báo `firstOpenOnly` | Unit test | `AdPlacementTest.firstOpenOnly_notForAppWide`: `AdPlacementDeclaration(code, APP_OPEN, appWide = true, firstOpenOnly = true)` ném `IllegalArgumentException`; khai báo `NATIVE_STEP` với `firstOpenOnly` thì không |
| Vị trí của app | Unit test | `AdPlacementAppTest.appPlacements_declaredWithSpecFormats` (dữ liệu thêm dòng); `nativeOnboarding_firstOpenOnly`: `NATIVE_ONBOARDING.firstOpenOnly` là `true`, `key` là `native_onboarding`, khóa `AdsSettings.placementEnabled(it).name` là `is_show_native_onboarding` |
| KB-6 tiêu chí 1: thẻ ở mọi trang, đúng chiều cao | Compose UI test (`NATIVE`, khung 411 x 891) | `OnboardingScreenTest.adSlot_card_onEveryPage_heightOfStepCard`: mỗi trang của danh sách 4 trang, `adSlot` vẽ `NativeAdCard(assets, NativeAdSize.STEP)` giả: khe `onboarding_ad_slot` cao `NativeStepAdDefaults.height()` (250 dp, sai số 1 dp), đáy khe ở trên ghi chú (trang cuối) hay trên nút |
| KB-6 tiêu chí 4: thẻ không che nút và ghi chú (trang cuối) | Compose UI test | `OnboardingScreenTest.camera_withCard_noteAndButtonBelowCard`: biên của thẻ, `onboarding_note`, `onboarding_enable_camera` không giao nhau, theo thứ tự trên xuống |
| KB-6 tiêu chí 4, khung thấp | Compose UI test (`w360dp-h640dp-xxhdpi`) | `OnboardingScreenTest.small_withCard_buttonInWindow_pageScrolls`, trang 1 và trang cuối: nút nằm trọn trong cửa sổ; trang cuối: ghi chú nằm trọn trong cửa sổ và dưới thẻ; `performScrollTo()` phần tử cuối của trang rồi phần tử đó hiện trong `onboarding_page_area`. Ghi số đo vùng trang vào nhật ký (so với ước lượng của mục 3.1) |
| KB-6 tiêu chí 7: vùng chạm của nút không chồng thẻ | Compose UI test | `OnboardingScreenTest.nextButton_touchArea_notOverCard`: biên chạm (`touchBoundsInRoot`) của `onboarding_next` không giao biên thẻ, ở khung 411 x 891 và 360 x 640 |
| KB-6 tiêu chí 2: nút mờ khi được bảo | Compose UI test | `OnboardingScreenTest.nextDisabled_whenNextEnabledFalse`: `nextEnabled = false`: `onboarding_next` `assertIsNotEnabled()`, bấm không gọi `onNext`; đối chứng `true` gọi một lần. Nút "Bật camera" ở trang cuối không đổi theo `nextEnabled` |
| KB-6 tiêu chí 6, 8: không được phép thì không chỗ trống | Compose UI test | `OnboardingScreenTest.realCard_noAdsController_takesNoSpace`: `adSlot = { OnboardingAdCard(it) }` không có `LocalAdsController`: khe cao 0 dp, nút cùng chỗ với khi `adSlot` rỗng (đối chứng: một `Box` 250 dp làm nút không đổi chỗ, test của T-082) |
| Log của phần chờ (ADR-0019) | Unit test | `OnboardingViewModelTest.introAdWait_logsStartAndEnd`: `RecordingAppLogger` có `Intro ad wait started` và `Intro ad wait ended: timeout` |
| Giá trị staging | Unit test (testStaging) | `StagingRemoteValuesModuleTest.localValues_areExactlyTheAppSet` (dữ liệu thêm `is_show_native_onboarding=true`); `firstOpenAndCombo_areNotSet` giữ |
| Bảng khóa khớp code | Unit test sẵn có | `:app:testStagingDebugUnitTest` (test bảng khóa) qua sau khi sinh lại |
| Giao diện trạng thái `ad` | Screenshot test (Roborazzi, khung `w411dp-h891dp-xxhdpi`) | `OnboardingScreenScreenshotTest`: `captureRoboImage("src/test/screenshots/OnboardingScreen_intro-ad_light.png")` và các ảnh `poses-ad`, `premium-ad`, `camera-ad`, sáng và tối, với `NativeAdCard(assets, NativeAdSize.STEP)` giả trong `adSlot` |
| Trang 1 lúc chờ | Screenshot test | `captureRoboImage("src/test/screenshots/OnboardingScreen_intro-ad-wait_light.png")`, `_dark`: `adSlot` vẽ `NativeAdPlaceholder(NativeAdSize.STEP, animated = false)`, `nextEnabled = false`. Khung chờ tĩnh, không có hiệu ứng chạy mãi (L-029) |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` | `captureRoboImage("src/test/screenshots/OnboardingScreen_intro-ad_small.png")`, `captureRoboImage("src/test/screenshots/OnboardingScreen_camera-ad_small.png")` |
| Mở gập (ADR-0020) | Screenshot test khung `w673dp-h841dp-xxhdpi` | `captureRoboImage("src/test/screenshots/OnboardingScreen_camera-ad_light_unfolded.png")`, khi gói thiết kế có frame này |
| Đúng thiết kế | Screenshot test cùng tên với từng ảnh thiết kế, `scripts/design-compare.sh onboarding`, rà soát độc lập theo mục 7 của mẫu thiết kế | Các ảnh `-ad` và `-ad-wait` trên. Gói thiết kế của luồng chưa có (mục 8, câu 1); frame trang không có thẻ là của T-082, plan phiên bản 2 liệt kê theo gói. Ảnh thiết kế của ticket khác (T-012): `OnboardingScreen_default_light`, `OnboardingScreen_default_dark` |
| KB-6: từ chối đồng ý thì không chờ, luồng đi được, không có thẻ | Kiểm tra thủ công (emulator: `feature/onboarding/src/deviceScenarios/T-083-gioi-thieu-tu-choi-dong-y.txt`) | `fresh`, `consent decline 30`, `wait id=onboarding_next 60`, `expect_no id=onboarding_ad_card`, `tap id=onboarding_next`, `wait id=onboarding_page_poses 3` (không chờ 5 giây), `expect_no id=onboarding_ad_card`, chạm "Tiếp" tới `onboarding_enable_camera`, chạm, `need id=camera_poses 15` |
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
- **Test có sẵn (`scripts/existing-tests.py`, chạy trên nhánh plan 2026-10-31):** 552 test dùng tên của các file plan sửa, đa số chỉ vì dùng `AdPlacement`. Nhóm "Cần xem trước" (9): các test `..._withoutAdsController_takesNoSpace`, `InListAdsTest`, `NativeAdCellTest`, `NativeFullScreenOverlayTest`, `RewardedActionTest`, `PosesScreenTest`, `PoseDetailScreenTest` đo khe của vị trí khác; plan không đổi vị trí hay khe nào của chúng, giữ. Nhóm "Nên xem" liên quan: `StartupAdChoiceTest.placementSwitch_*` (giữ; vị trí của chúng không `firstOpenOnly`), `AdPlacementTest.*` (giữ; thêm một test), `AdPlacementSourceTest.releaseScript_readsEveryShippedPlacement` (giữ; dòng mới một dòng), `AdPreloadPlanTest.fromPlacements_*` (giữ; `NATIVE_STEP` đã có qua `GALLERY_NATIVE_STEP`), `NativeFlowStepTest.stepAllowed_*` (giữ), `RemoteConfigKeysTest.*` (qua sau khi sinh lại bảng khóa), `StagingRemoteValuesModuleTest.*` (mục 3.5), `OnboardingScreenScreenshotTest.default_*` (T-082 bỏ; không thuộc plan này).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (thêm vị trí quảng cáo `native_onboarding`, ở lần mở đầu cạnh trang xin quyền camera); 7 (sửa `:core:ads`: hàm công khai mới, thuộc tính mới của `AdPlacement`, công tắc của vị trí). Hộp thoại đồng ý (điểm 5) không đổi: plan chỉ đọc cổng có sẵn.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | `:core:ads`, `:feature:onboarding`, `:app` (staging), bảng khóa, tài liệu |
| B. Độ rõ của yêu cầu | 2 | Spec đã chốt, nhưng chưa có thiết kế và còn câu hỏi về lúc bắt đầu, ra nền, pool cạn (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Phần chờ, cổng, công tắc có test JVM; quảng cáo thật, đồng ý chấp nhận và thứ tự nạp chỉ kiểm trên máy thật |
| D. Ảnh hưởng chéo | 3 | Sửa `placementSwitch` mà mọi vị trí đi qua; T-080, T-084, T-086 cùng sửa `AdPlacement.kt`, staging, bảng khóa; phụ thuộc chữ ký của T-082 |
| E. Độ mới của kỹ thuật | 2 | Lần đầu dự án giữ nút theo thời gian trong composition với sự kiện lifecycle và đồng hồ dừng; đã chạy thử |
| **Tổng** | 12 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdPlacement.kt` (khai báo, thuộc tính mới), `StartupAdChoice.kt` (`placementSwitch` của mọi vị trí), `NativeFlowStep.kt`, `StagingRemoteValuesModule.kt` và test, `docs/remote-config-keys.md`, `docs/remote-config-template.json`, `docs/ops/manual-test.md`, `docs/ops/ads-guide.md`, `AdPlacementAppTest.kt`, `AdUnitIdsTest.kt`, `AdPlacementTest.kt`.
- Task khác có thể xung đột:
  - T-082: tạo màn nhiều trang mà plan này nối vào (`OnboardingScreen.kt`, `OnboardingScreenTest.kt`, `OnboardingScreenScreenshotTest.kt`, `OnboardingViewModel.kt`, `OnboardingViewModelTest.kt`). Phải merge trước.
  - T-080 (approved, RUN-20261031-02): cùng sửa `AdPlacement.kt`, `AdPlacementAppTest.kt`, `AdPlacementTest.kt`, `AdUnitIdsTest.kt` (T-080 dùng `unit/12`), `StagingRemoteValuesModule.kt` và test, bảng khóa, `manual-test.md` mục 11, `ads-guide.md`. Chạy sau T-080.
  - T-081, T-084, T-086 (nháp): cùng sửa `AdPlacement.kt`, `StagingRemoteValuesModule(Test).kt`, bảng khóa, có thể cả `AdUnitIdsTest.kt` và `AdPlacementAppTest.kt`. Số `unit/<n>` của `native_onboarding` lấy số kế tiếp lúc chạy; dòng vị trí đặt sau các dòng đã có.
  - T-085 (premium): trang `PREMIUM` của T-082 phụ thuộc cờ của T-085; plan này chỉ dùng `OnboardingPage`.
- Đề xuất thứ tự chạy: sau T-080 và T-082; nếu cùng danh sách với T-084, T-086 thì chạy sau chúng hay ngay trước, miễn mỗi task đọc `AdPlacement.kt` ở trạng thái lúc chạy.

## 7. Điều kiện dừng riêng

- Plan T-082 đã merge mà chữ ký `OnboardingScreen`, tên `OnboardingPage`, tag `onboarding_ad_slot`, `onboarding_next`, `onboarding_enable_camera` hay vị trí của khe (giữa vùng trang và ghi chú hay nút) khác mục 3.1: dừng (điều kiện 3), soạn plan phiên bản mới.
- Đo ở khung thấp mà nút hay ghi chú không nằm trọn trong cửa sổ khi có thẻ: dừng, không tự giảm lề hay ẩn thẻ (quyết định của thiết kế, mục 8, câu 7).
- Bảng khóa sinh lại có thay đổi ngoài `is_show_native_onboarding` (và khóa của task đã merge trước): dừng.
- Test có sẵn của `placementSwitch` cho vị trí app-wide hay vị trí thường đỏ: dừng, không sửa test.

## 8. Câu hỏi cho người duyệt

1. **Thiết kế (điểm chặn).** Chưa có frame `OnboardingScreen_<intro|poses|premium|camera>-ad_<light|dark>`, `OnboardingScreen_intro-ad-wait_<light|dark>`, khung thấp và mở gập có thẻ. Gói thiết kế cần chốt: khoảng cách giữa vùng trang và thẻ (plan để thẻ không có lề ngoài, mục 3.1, Khe), khung chờ của thẻ ở trang 1 (đề xuất: `NativeAdPlaceholder` tĩnh của khung), độ mờ của nút (đề xuất: màu `disabled` của `PrimaryButton`). Đề xuất của AI: làm thiết kế cùng gói của T-082; plan lên phiên bản 2 ghi tên frame và số đo theo gói.
2. **Khớp plan T-082 (điểm chặn).** Plan này dựa trên plan T-082 phiên bản 1 (nháp): `OnboardingPage`, `OnboardingScreen(..., adSlot)`, khe `onboarding_ad_slot`, nút ghim đáy, tham số `nextEnabled` mà T-082 nói T-083 thêm. Plan T-082 lên phiên bản mới thì plan này phải khớp lại trước khi duyệt. Đề xuất của AI: T-082 bỏ gợi ý `padding(top = Spacing.lg)` cho thẻ (lý do ở mục 3.1, Khe).
3. **Lúc bắt đầu chờ.** Spec: "Khi trang 1 hiện mà quảng cáo được phép". Người dùng trả lời hộp thoại đồng ý khi trang 1 đã hiện (hộp thoại hiện muộn) thì có chờ không? Đề xuất của AI: có, 5 giây tính từ lúc quảng cáo được phép, vì pool chỉ bắt đầu nạp sau khi đồng ý. Phương án khác: 5 giây tính từ lúc trang 1 hiện, đồng ý muộn thì chỉ chờ phần còn lại.
4. **Ra nền giữa lúc chờ.** Đề xuất của AI: kết thúc chờ, quay lại thì nút bấm được ngay, thẻ hiện khi có quảng cáo. Phương án khác: tạm dừng và chờ nốt phần còn lại khi quay lại (code dài hơn, phải lưu thời gian đã chờ).
5. **Pool cạn trong lúc chờ.** Khi mọi lần nạp của pool `native_step` thất bại, khung báo "không có quảng cáo" (`Unavailable`). Đề xuất của AI: hiện plan chỉ kết thúc chờ khi có quảng cáo, hết giờ, thôi được phép, rời trang hay ra nền; `Unavailable` thì vẫn chờ tới 5 giây vì pool vẫn thử lại. Có muốn kết thúc chờ ngay khi pool cạn không? Nếu có, `onAdAvailabilityChange(false)` cũng kết thúc chờ (thêm lý do `no ad`, một test).
6. **"Chỉ ở lần mở đầu tiên" là dấu lần mở đầu của khung quảng cáo.** `firstOpenOnly` dùng dấu `ads_first_open_done` (A-01), không dùng trạng thái "đã qua màn chào". Hệ quả: người dùng thoát app giữa luồng ở lần mở đầu, mở lại thì luồng hiện lại nhưng không có thẻ; process bị hệ thống dừng giữa luồng rồi khôi phục cũng vậy. Đề xuất của AI: chấp nhận, vì ít người gặp và đúng chữ của spec mục 5. Phương án khác: bỏ `firstOpenOnly`, vị trí bật ở mọi lần mở; khi đó pool `native_step` nạp 3 quảng cáo ở mỗi lần mở mà không màn nào dùng (tỷ lệ hiện trên lượt nạp giảm), và nhóm nạp sẵn đầu chờ pool này tới 6 giây trước các định dạng khác.
7. **Màn thấp.** Ước lượng vùng trang còn khoảng 134 dp ở trang cuối khung 360 x 640 có thẻ (mục 3.1). Đề xuất của AI: giữ cuộn, không ẩn thẻ, không giảm lề, trừ khi gói thiết kế có frame thấp khác.
8. **Đề xuất về dự án mẫu.** `rememberNativeStepAdAllowed` và `firstOpenOnly` có ích cho mọi app có luồng giới thiệu. Đề xuất của AI: ghi vào mô tả PR theo `docs/ops/from-app.md`; dự án mẫu quyết tên.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:**
- **Người duyệt:** , **duyệt chéo:**
- **Ghi chú:**

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
