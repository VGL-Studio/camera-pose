---
ticket: T-082
title: Luồng giới thiệu 4 trang thay màn chào (chưa có quảng cáo)
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 39.4M        # cỡ L theo số gợi ý của scripts/token-usage.py estimate (2026-10-31); không nâng tay
blocking_points: [analytics]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-082: LUỒNG GIỚI THIỆU 4 TRANG THAY MÀN CHÀO (CHƯA CÓ QUẢNG CÁO)

## 1. Tóm tắt

Màn chào một trang của `:feature:onboarding` đổi thành luồng giới thiệu 3 hoặc 4 trang: giới thiệu, tư thế, tư thế premium (chỉ khi cờ `feature_premium_poses_enabled` bật), trang cuối xin quyền camera. ViewModel giữ danh sách trang và trang hiện tại; màn hiện một trang một lúc, không dùng pager; nút "Tiếp" (trang cuối là "Bật camera") ghim ở đáy màn, cùng một chỗ trên mọi trang; nút quay lại của máy về trang trước; mỗi trang ghi `onboarding_step_viewed` một lần. Màn có sẵn một khe quảng cáo (tham số composable, mặc định rỗng) giữa vùng trang và nút, để T-083 đặt `NativeStepAd` mà nút không nhảy; kiểm chứng bằng unit test ViewModel, test giao diện Robolectric (vị trí nút có và không có khối 250 dp trong khe), screenshot test, kịch bản emulator.

**Điểm chặn trước khi duyệt:** chưa có thiết kế Figma của luồng (mục 8, câu 1). Số đo bố cục, chữ của các trang mới và tên frame trong plan là đề xuất; plan phiên bản 2 ghi lại theo gói bàn giao.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/onboarding/spec.md` (confirmed 2026-10-31, câu 4, 5, 7):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi màn loading xong ở lần mở đầu tiên thì trang 1 của luồng giới thiệu hiện, đúng thiết kế; không có quảng cáo toàn màn hình | `:app` vẫn chọn `MainScreen.ONBOARDING` như hiện nay (`MainScreen.kt` không đổi); ViewModel mới mở ở trang 1. Thẻ native là T-083 |
| KB-1 | Khi bấm "Bật camera" thì máy hiện hộp hỏi quyền camera | Như cũ, chỉ ở trang cuối |
| KB-1 | Khi người dùng cho phép thì app mở camera | Không đổi (`AppNavHost`, `OnboardingEvent.Finished`) |
| KB-1 | Số chạm bằng số trang + 1 | Trang 1 tới trang kế cuối mỗi trang một "Tiếp"; trang cuối "Bật camera"; "Cho phép". Không có "Bỏ qua" (câu 5) |
| KB-1 | Bấm "Bật camera" ghi `onboarding_complete` một lần; hộp hỏi quyền hiện và người dùng chọn thì ghi `camera_permission_result` | Không đổi; chỉ khác là "Bật camera" chỉ có ở trang cuối |
| KB-1 | Back ở trang 1 thì app đóng; trang khác theo KB-5 | `BackHandler` chỉ bật từ trang 2 |
| KB-1 | Xoay máy không cắt chữ hay nút; màn thấp cuộn được, nút luôn thấy | Mỗi trang cuộn trong vùng trang; nút ghim ở đáy |
| KB-1 | Ngôn ngữ chưa có thì hiện tiếng Anh | Chữ mới có ở `values/` (tiếng Anh) và 6 thư mục dịch |
| KB-2 | Từ chối quyền vẫn sang camera; "không hỏi lại" thì sang thẳng, không ghi `camera_permission_result` | Không đổi: phần quyền của ViewModel và `PermissionPrompt.kt` giữ nguyên, chỉ chạy ở trang cuối |
| KB-3 | Đã bấm "Bật camera" thì lần sau vào thẳng camera; đóng app trước khi bấm thì lần sau màn chào hiện lại; xóa dữ liệu thì hiện lại | Không đổi: `:app` chỉ đánh dấu xong khi nhận `OnboardingEvent.Started`, mà "Tiếp" không gửi sự kiện này |
| KB-4 | Cờ `feature_onboarding_enabled` tắt thì vào thẳng camera | Không đổi (`MainScreen.kt`, `MainScreenTest`) |
| KB-5 | Màn loading xong thì trang 1 hiện, đúng thiết kế | Như KB-1, tiêu chí 1; cần ảnh thiết kế (mục 8, câu 1) |
| KB-5 | Bấm "Tiếp" thì trang kế hiện; nút "Tiếp" cùng một chỗ trên mọi trang, không nhảy khi thẻ quảng cáo hiện hay ẩn | Nút ghim ở đáy màn. Khe quảng cáo nằm trên nút, dưới vùng trang: thẻ hiện thì vùng trang thấp đi, nút không đổi chỗ |
| KB-5 | Tới trang cuối thì "Bật camera" thay "Tiếp"; bấm thì theo KB-1, KB-2 | Cùng chỗ, cùng cỡ; tag đổi từ `onboarding_next` sang `onboarding_enable_camera` |
| KB-5 | Cờ premium bật: 4 trang, trang 3 là tư thế premium; tắt: 3 trang, không nhắc premium | Danh sách trang theo cờ (mục 3.1, Danh sách trang). Giả định: cờ đổi trong lúc luồng chạy thì danh sách chỉ theo cờ khi người dùng còn ở trang 1 hay 2 (mục 8, câu 3) |
| KB-5 | Trang hiện lần đầu trong luồng thì ghi `onboarding_step_viewed` với số thứ tự trang | `step` là chuỗi `"1"`, `"2"`... (tham số của `AnalyticsEvent` là chuỗi). Quay lại rồi tới lại không ghi lần nữa; activity dựng lại không ghi lại |
| KB-5 | Back ở trang 2 trở đi về trang trước; trang 1 thì app đóng | `BackHandler(enabled = trang > 1)` |
| KB-5 | Xoay máy hay mở gập thì trang đang xem giữ nguyên, không cắt chữ, nút; trang dài thì cuộn | ViewModel sống qua đổi cấu hình; trang hiện tại cũng ghi vào `SavedStateHandle` cho lần hệ thống dừng tiến trình |
| KB-5 | Đóng app giữa luồng thì lần sau bắt đầu lại từ trang 1 | "Tiếp" không gửi `Started`, nên `:app` không đánh dấu xong; ViewModel mới mở ở trang 1 |

Thiết kế tham chiếu: `features/onboarding/design/README.md` hiện chỉ có màn chào một trang (`OnboardingScreen_default_light`, `OnboardingScreen_default_dark`). Luồng nhiều trang chưa có thiết kế; đây là điểm chặn (mục 8, câu 1). Plan đề xuất tên frame theo quy ước `<Ten>Screen_<trạng thái>_<light|dark>[_unfolded]` của `docs/ops/design-workflow.md`.

ADR liên quan: ADR-0001 (feature chỉ phụ thuộc core), ADR-0003 (cờ là `Flow`), ADR-0019 (log), ADR-0020 (ảnh thiết kế và screenshot cùng tên, khung gập), ADR-0029, ADR-0031 (test cũ của màn một trang, mục 3.5).

## 3. Giải pháp

### 3.1. Cách làm

**Phụ thuộc.** Plan dùng `FeatureFlag.PREMIUM_POSES` (khóa `feature_premium_poses_enabled`) do T-085 thêm vào `:core:config` (ticket T-085, mục Phạm vi, dòng Cờ). T-082 chạy sau khi T-085 đã merge; tên hằng phải khớp plan T-085 (mục 8, câu 6). `:feature:onboarding` đã có `:core:config` qua convention plugin `template.android.feature`, không thêm phụ thuộc.

**Không đổi ở `:app`.** `MainScreen.kt` (`chooseMainScreen`) và `AppNavHost.kt` không đổi: luồng vẫn là một đích `OnboardingDestination`, vẫn gửi `onStarted` khi bấm "Bật camera" và `onFinished` khi xong quyền. Các trang là trạng thái bên trong một màn, không phải đích điều hướng, nên nút quay lại ở trang 1 vẫn đóng app như hiện nay.

**Danh sách trang.** Thêm `OnboardingPage.kt`:

```kotlin
/** A page of the onboarding flow, in display order (spec onboarding, question 4). */
enum class OnboardingPage(val logName: String) {
    INTRO("intro"),
    POSES("poses"),
    /** Only while feature_premium_poses_enabled is on (spec onboarding, KB-5). */
    PREMIUM("premium"),
    /** The last page: "Turn on camera" and the permission note. */
    CAMERA("camera"),
}

internal fun onboardingPages(premium: Boolean): List<OnboardingPage> =
    listOfNotNull(OnboardingPage.INTRO, OnboardingPage.POSES, OnboardingPage.PREMIUM.takeIf { premium }, OnboardingPage.CAMERA)
```

**Trạng thái màn** (`OnboardingUiState.kt`, thay ba `data object` cũ). Mọi trạng thái mang danh sách trang và trang hiện tại, vì màn vẫn vẽ trang cuối trong lúc hỏi quyền và lúc chuyển sang camera:

```kotlin
sealed interface OnboardingUiState {
    val pages: List<OnboardingPage>
    val index: Int
    val page: OnboardingPage get() = pages[index]
    val isLastPage: Boolean get() = index == pages.lastIndex

    /** The user reads page [index]; on the last page, waits for "Turn on camera". */
    data class Browsing(override val pages: List<OnboardingPage>, override val index: Int) : OnboardingUiState

    /** On the last page, the system permission prompt is open. */
    data class RequestingPermission(override val pages: List<OnboardingPage>) : OnboardingUiState {
        override val index: Int get() = pages.lastIndex
    }

    /** On the last page, going to the camera with or without the permission. */
    data class Finished(override val pages: List<OnboardingPage>) : OnboardingUiState {
        override val index: Int get() = pages.lastIndex
    }
}
```

`OnboardingEvent` giữ nguyên (`Started`, `RequestPermission`, `Finished`).

**ViewModel** (`OnboardingViewModel.kt`). Thêm tham số `featureFlags: FeatureFlags`. Phần quyền (`onEnableCameraClick`, `onPermissionResult`, hai khóa `requesting_permission`, `completion_logged`) giữ nguyên, chỉ thêm điều kiện "đang ở trang cuối". Phần mới (phác thảo, chưa chạy thử; mục 4.1):

```kotlin
private var premium = savedStateHandle.get<Boolean>(KEY_PREMIUM) ?: false
// The page list stops following the flag once the user reaches the third page (question 3 of section 8).
private var frozen = savedStateHandle.contains(KEY_PREMIUM)
private val viewedSteps = savedStateHandle.get<IntArray>(KEY_VIEWED_STEPS)?.toMutableSet() ?: mutableSetOf()

init {
    logger.info(LOG_TAG, "Onboarding shown")
    onPageShown()
    viewModelScope.launch {
        featureFlags.enabled(FeatureFlag.PREMIUM_POSES).distinctUntilChanged().collect { on ->
            if (frozen || on == premium) return@collect
            val current = state.value as? OnboardingUiState.Browsing ?: return@collect
            premium = on
            state.value = current.copy(pages = onboardingPages(on))
            logger.info(LOG_TAG, "Premium page ${if (on) "on" else "off"}")
        }
    }
}

fun onNextClick() {
    val current = state.value as? OnboardingUiState.Browsing ?: return
    if (current.isLastPage) return
    val next = current.index + 1
    if (next >= FREEZE_INDEX && !frozen) {
        frozen = true
        savedStateHandle[KEY_PREMIUM] = premium
    }
    savedStateHandle[KEY_PAGE_INDEX] = next
    state.value = OnboardingUiState.Browsing(onboardingPages(premium), next)
    onPageShown()
}

fun onBackClick() {
    val current = state.value as? OnboardingUiState.Browsing ?: return
    if (current.index == 0) return
    savedStateHandle[KEY_PAGE_INDEX] = current.index - 1
    state.value = current.copy(index = current.index - 1)
    logger.info(LOG_TAG, "Back to page ${current.index}")
}

/** Logs onboarding_step_viewed the first time a step is shown in this flow (spec onboarding, KB-5, question 7). */
private fun onPageShown() {
    val current = state.value
    val step = current.index + 1
    logger.info(LOG_TAG, "Page shown: $step ${current.page.logName}")
    if (viewedSteps.add(step)) {
        analytics.log(AnalyticsEvent(EVENT_STEP_VIEWED, mapOf(PARAM_STEP to step.toString())))
        savedStateHandle[KEY_VIEWED_STEPS] = viewedSteps.toIntArray()
    }
}

internal companion object {
    // ... LOG_TAG, EVENT_COMPLETE, EVENT_PERMISSION_RESULT, SOURCE, KEY_REQUESTING_PERMISSION, KEY_COMPLETION_LOGGED as today
    const val EVENT_STEP_VIEWED = "onboarding_step_viewed"
    const val PARAM_STEP = "step"
    const val KEY_PAGE_INDEX = "page_index"
    const val KEY_PREMIUM = "premium_page"
    const val KEY_VIEWED_STEPS = "viewed_steps"
    /** Index of the third page: from there on the page list no longer follows the flag. */
    const val FREEZE_INDEX = 2
}
```

Chi tiết:

- Trạng thái đầu: `requesting_permission` có trong `SavedStateHandle` thì `RequestingPermission(onboardingPages(premium))`, không thì `Browsing(onboardingPages(premium), index)` với `index` đọc từ `page_index` (mặc định 0, ép vào khoảng của danh sách).
- Trước lần đầu cờ trả giá trị, danh sách là 3 trang. Trang 1 và 2 giống nhau ở cả hai danh sách, nên cờ về trễ không đổi trang đang xem, chỉ đổi số trang của chỉ báo trang.
- `onEnableCameraClick` thêm điều kiện đầu `state.value is Browsing && isLastPage`; phần sau giữ nguyên, trạng thái mới là `RequestingPermission(pages)` hay `Finished(pages)`. `onPermissionResult` chỉ đổi kiểu so sánh (`state.value is RequestingPermission`).
- Thứ tự gọi: `onPageShown()` của trang 1 chạy trong `init`, trước khi cờ được đọc: `step = 1` không phụ thuộc cờ.
- Log (ADR-0019): "Onboarding shown" như cũ; mới: "Page shown: <số> <tên trang>", "Back to page <số>", "Premium page on|off". Không có dữ liệu cá nhân.
- Sự kiện mới đặt là hằng `EVENT_STEP_VIEWED` để `scripts/ads-plan.py` đọc được (câu 7).

**Màn hình** (`OnboardingScreen.kt`). Một màn hiện một trang một lúc; không dùng `HorizontalPager`, vì người dùng chỉ đi bằng "Tiếp" và nút quay lại, và ViewModel là nơi duy nhất giữ trang hiện tại (không có vuốt để đồng bộ). Trang đổi ngay, không hiệu ứng chuyển (mục 8, câu 4). Chữ ký mới, dùng cho T-083:

```kotlin
@Composable
internal fun OnboardingScreen(
    uiState: OnboardingUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onEnableCamera: () -> Unit,
    modifier: Modifier = Modifier,
    /** The ad card of T-083, drawn between the page and the bottom button; nothing by default. */
    adSlot: @Composable (page: OnboardingPage) -> Unit = {},
)
```

Cấu trúc, từ trên xuống, trong một `Column` lấp màn (`testTag` `onboarding_screen`, nền `background`, lề trái phải `Spacing.lg`, trên `Spacing.xl + Spacing.lg` như hiện nay, dưới `Spacing.lg`):

1. Chỉ báo trang (`onboarding_page_indicator`): một hàng chấm, số chấm bằng số trang, chấm của trang hiện tại nổi bật; một node ngữ nghĩa "Trang 2 trên 4" (`onboarding_page_indicator_description`). Đặt ở trên cùng, cố định, không cuộn (mục 8, câu 4).
2. Vùng trang (`onboarding_page_area`), `Modifier.weight(1f)`: một `Column` cuộn dọc (`onboarding_content`) chứa nội dung của trang, bọc trong `key(page)` để mỗi trang mở từ đầu. Nội dung có tag theo trang: `onboarding_page_intro`, `onboarding_page_poses`, `onboarding_page_premium`, `onboarding_page_camera`.
3. Khe quảng cáo (`onboarding_ad_slot`): `Box(Modifier.fillMaxWidth())` gọi `adSlot(uiState.page)`. Ở T-082 khe rỗng, cao 0.
4. Trang cuối: `Spacer(Spacing.lg)` rồi ghi chú quyền (`onboarding_note`), đặt **trên** nút.
5. `Spacer(Spacing.lg)`, rồi nút chính `PrimaryButton` rộng hết, ghim ở đáy: trang trước trang cuối là "Tiếp" (`onboarding_next`, không icon); trang cuối là "Bật camera" (`onboarding_enable_camera`, icon `photo_camera`, `loading` khi trạng thái không phải `Browsing`).

Vì nút là phần tử cuối của cột lấp màn và các phần trên nó có chiều cao cố định hay co theo `weight`, đáy của nút luôn cách đáy màn `Spacing.lg`, ở mọi trang, có hay không có thẻ trong khe. Khi T-083 đặt thẻ 250 dp vào khe, vùng trang thấp đi 250 dp và cuộn; nút không đổi chỗ (KB-5, KB-6). Ghi chú ở trang cuối nằm giữa khe và nút, nên thẻ không che nút hay ghi chú (KB-6, tiêu chí 4). Cùng lý do, nút "Bật camera" ở trang cuối cùng chỗ và cùng cỡ với nút "Tiếp" ở các trang khác.

Ước lượng cho T-083 ở khung thấp 360 x 640 có thẻ 250 dp, trang cuối (chưa đo, theo `Spacing` hiện có): lề trên 56, chỉ báo 8 và cách 24, khe 250, ghi chú khoảng 32 tới 48 và cách 24, nút 56 và cách 16, lề dưới 24; vùng trang còn khoảng 130 tới 150 dp và cuộn. T-083 đo thật và quyết có cần giảm lề trên ở khung thấp không.

Nội dung từng trang (bảng chữ của spec, mục 4; chữ của trang 2, 3, cuối chờ gói thiết kế, mục 8, câu 1):

| Trang | Tag | Nội dung |
| --- | --- | --- |
| 1 `INTRO` | `onboarding_page_intro` | Hình minh họa `OnboardingIllustration` (giữ nguyên, co trước ở màn thấp bằng `ShrinkFirstLayout` như hiện nay), tiêu đề `onboarding_title`, giới thiệu `onboarding_body` |
| 2 `POSES` | `onboarding_page_poses` | Huy hiệu icon `accessibility_new`, tiêu đề mới `onboarding_poses_title`, hai hàng điểm `onboarding_point_poses`, `onboarding_point_saved` (tag `onboarding_feature_poses`, `onboarding_feature_saved` như cũ) |
| 3 `PREMIUM` | `onboarding_page_premium` | Huy hiệu icon mới `workspace_premium` (Material Symbols, cùng giấy phép `third_party/material-symbols/LICENSE.txt`), tiêu đề mới `onboarding_premium_title`, lời mới `onboarding_premium_body` |
| Cuối `CAMERA` | `onboarding_page_camera` | Huy hiệu icon `photo_camera`, tiêu đề mới `onboarding_camera_title`, hàng điểm `onboarding_point_adjust` (tag `onboarding_feature_adjust`); dưới vùng trang: ghi chú `onboarding_note`, nút "Bật camera" |

Huy hiệu là vòng tròn 96 dp nền `secondaryContainer`, viền `Stroke.sticker` màu `outline`, icon 48 dp màu `onSecondaryContainer`, cùng kiểu với huy hiệu camera của hình minh họa; tiêu đề `headlineMedium`, lời `bodyLarge`, hàng điểm như `FeatureRow` hiện có. Mọi số này là đề xuất tới khi có gói thiết kế. Trang chung một composable riêng tư `OnboardingPageContent(illustration, title, body, points)`; tag `onboarding_title`, `onboarding_body`, `onboarding_features` gắn cho tiêu đề, lời, khối điểm của trang đang hiện (chỉ một trang có mặt mỗi lúc).

Chữ mới (tiếng Anh ở `values/`, tiếng Việt ở `values-vi/`; năm ngôn ngữ còn lại AI dịch trong phiên theo câu 1 của spec):

| Khóa | Tiếng Anh | Tiếng Việt |
| --- | --- | --- |
| `onboarding_next` | Next | Tiếp |
| `onboarding_poses_title` | A pose for every shot | Tư thế cho mọi kiểu ảnh |
| `onboarding_premium_title` | Premium poses | Tư thế premium |
| `onboarding_premium_body` | Watch one ad to unlock a premium pose, then keep it for good. | Xem một quảng cáo để mở một tư thế premium, rồi dùng mãi về sau. |
| `onboarding_camera_title` | Ready to shoot | Sẵn sàng chụp |
| `onboarding_page_indicator` | Page %1$d of %2$d | Trang %1$d trên %2$d |

Chuỗi đi cùng số (L-077): chỉ `onboarding_page_indicator`; `%1$d` là trang hiện tại (từ 1), `%2$d` là tổng số trang (3 hoặc 4). Không có giá trị lớn nhất mang nghĩa riêng.

**Route** (`OnboardingRoute`): truyền `onNext = viewModel::onNextClick`, `onBack = viewModel::onBackClick`; phần quyền giữ nguyên. Sửa KDoc ("No BackHandler" thành "Back goes to the page before, from the second page"). `BackHandler(enabled = uiState is Browsing && uiState.index > 0) { onBack() }` đặt trong `OnboardingScreen`, để test giao diện kiểm được không cần ViewModel.

**Điểm cho T-083** (cấu trúc mà T-083 dẫn theo):

- Không có pager. Khóa bước cho `NativeStepAd(placement, step)` là `page` mà `adSlot` nhận (`OnboardingPage`, đổi mỗi khi trang đổi; trang đứng yên ngay vì không có vuốt).
- T-083 truyền `adSlot = { page -> NativeStepAd(AdPlacement.NATIVE_ONBOARDING, step = page, modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg)) }` từ `OnboardingRoute`; screenshot test truyền `NativeAdCard(assets, NativeAdSize.STEP)` giả. Khung cố định nếu cần thì cao `NativeStepAdDefaults.height()`, không viết cứng 250 dp (`docs/ops/ads-guide.md`, dòng 78).
- Nút "Tiếp" mờ khi chờ quảng cáo ở trang 1: T-083 thêm tham số `nextEnabled: Boolean = true` vào `OnboardingScreen`, truyền cho `PrimaryButton(enabled = ...)`.
- Tag ổn định: `onboarding_screen`, `onboarding_page_area`, `onboarding_content`, `onboarding_ad_slot`, `onboarding_note`, `onboarding_next`, `onboarding_enable_camera`, `onboarding_page_<intro|poses|premium|camera>`.
- Test `OnboardingScreenTest.bottomButton_samePlace_onEveryPage_withAndWithoutAdSlot` của plan này đã kiểm nút không nhảy với khối 250 dp; T-083 giữ test đó.

**Test cũ của `:app` và kịch bản emulator.** `AppLaunchTest`, `SettingsNavigationTest` (emulator, CI) và 7 tệp `deviceScenarios` bấm thẳng `onboarding_enable_camera` ở màn chào. Sau T-082, nút đó chỉ có ở trang cuối, nên các tệp này bấm "Tiếp" trước:

- Hai test emulator: thêm hàm `passOnboarding()`: lặp tối đa 4 lần, mỗi lần chờ có giới hạn (5 giây) tới khi có `onboarding_next` hoặc `onboarding_enable_camera`; có `onboarding_next` thì bấm, có `onboarding_enable_camera` thì bấm rồi thoát vòng. Chỉ kiểm trạng thái ổn định (CLAUDE.md, bước 10).
- 7 tệp kịch bản: chèn ba dòng `tap_if id=onboarding_next` ngay trước dòng `tap_if id=onboarding_enable_camera`. Nút "Tiếp" và "Bật camera" cùng chỗ, nên lần dump cũ còn thấy "Tiếp" ở trang kế cuối vẫn chạm đúng nút ở trang cuối; luồng 3 trang thì dòng thứ ba không thấy "Tiếp" và bỏ qua.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingPage.kt` | `enum class OnboardingPage`, `onboardingPages(premium)` |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingUiState.kt` | `Browsing`, `RequestingPermission`, `Finished` mang danh sách trang; `OnboardingEvent` giữ nguyên |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingViewModel.kt` | `FeatureFlags`, trang hiện tại, `onNextClick`, `onBackClick`, danh sách trang theo cờ premium và dừng theo ở trang 3, `onboarding_step_viewed`, ba khóa `SavedStateHandle` mới, log; quyền chỉ ở trang cuối |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreen.kt` | Chỉ báo trang, vùng trang cuộn, nội dung bốn trang, khe `adSlot`, ghi chú trên nút ở trang cuối, nút "Tiếp" hay "Bật camera" ghim ở đáy, `BackHandler`, tag mới; Route nối `onNext`, `onBack` |
| Sửa | `feature/onboarding/src/main/res/values/strings.xml` | 6 chuỗi mới (mục 3.1) |
| Sửa | `feature/onboarding/src/main/res/values-vi/strings.xml` | 6 chuỗi mới |
| Sửa | `feature/onboarding/src/main/res/values-hi/strings.xml` | 6 chuỗi mới (AI dịch) |
| Sửa | `feature/onboarding/src/main/res/values-pt-rBR/strings.xml` | 6 chuỗi mới (AI dịch) |
| Sửa | `feature/onboarding/src/main/res/values-b+es+419/strings.xml` | 6 chuỗi mới (AI dịch) |
| Sửa | `feature/onboarding/src/main/res/values-in/strings.xml` | 6 chuỗi mới (AI dịch) |
| Sửa | `feature/onboarding/src/main/res/values-th/strings.xml` | 6 chuỗi mới (AI dịch) |
| Tạo | `feature/onboarding/src/main/res/drawable/ic_onboarding_workspace_premium.xml` | Icon `workspace_premium` của Material Symbols (Rounded, 24), như các icon `ic_onboarding_*` đã có |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingViewModelTest.kt` | Test mới của luồng (mục 4); test cũ viết lại theo mục 3.5; `FakeFeatureFlags` |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreenTest.kt` | Test mới (mục 4); test cũ viết lại theo mục 3.5 |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreenScreenshotTest.kt` | Bỏ ba test `default_*`; thêm test cho từng ảnh dưới đây |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingStringsTest.kt` | Thêm 6 khóa mới vào `ENGLISH`, `VIETNAMESE` (dữ liệu của test) |
| Xóa | `feature/onboarding/src/test/screenshots/OnboardingScreen_default_light.png` | Màn một trang không còn |
| Xóa | `feature/onboarding/src/test/screenshots/OnboardingScreen_default_dark.png` | Màn một trang không còn |
| Xóa | `feature/onboarding/src/test/screenshots/OnboardingScreen_default_small.png` | Màn một trang không còn |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_poses_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_poses_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_premium_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_premium_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro_small.png` | Khung thấp, trang 1 (hình minh họa co) |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera_small.png` | Khung thấp, trang cuối (nhiều phần ghim nhất: ghi chú và nút) |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro_light_unfolded.png` | Khung mở gập 673 x 841 |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera_light_unfolded.png` | Khung mở gập 673 x 841 |
| Tạo | `feature/onboarding/src/deviceScenarios/T-082-luong-gioi-thieu.txt` | Kịch bản emulator (mục 4) |
| Sửa | `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt` | `passOnboarding()`: bấm "Tiếp" tới trang cuối rồi "Bật camera"; KDoc |
| Sửa | `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt` | Như trên |
| Sửa | `app/src/deviceScenarios/T-056-man-lon-xoay-ngang.txt` | Ba dòng `tap_if id=onboarding_next` trước `tap_if id=onboarding_enable_camera` |
| Sửa | `app/src/deviceScenarios/T-076-co-mang-khong-hien-hop-thoai.txt` | Như trên |
| Sửa | `feature/camera/src/deviceScenarios/T-020-camera-mo-gap.txt` | Như trên |
| Sửa | `feature/camera/src/deviceScenarios/T-058-chup-tu-chi-tiet.txt` | Như trên |
| Sửa | `feature/camera/src/deviceScenarios/T-064-mot-camera.txt` | Như trên |
| Sửa | `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt` | Như trên |
| Sửa | `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt` | Như trên |
| Xóa | `docs/features/onboarding/design/review/OnboardingScreen_default_light_compare.png` | Ảnh so sánh của màn cũ (gói thiết kế mới không còn frame `default`) |
| Xóa | `docs/features/onboarding/design/review/OnboardingScreen_default_dark_compare.png` | Như trên |
| Tạo | `docs/features/onboarding/design/review/OnboardingScreen_intro_light_compare.png` | Sinh bằng `scripts/design-compare.sh onboarding`; cùng cách cho mọi ảnh thiết kế có trong gói (8 ảnh trang, 2 ảnh `_unfolded`) |
| Sửa | `docs/features/onboarding/design/review/summary.md` | Bảng kết quả của `design-compare.sh` |
| Sửa | `docs/features/onboarding/design/README.md` | Mục 8, kết quả đối chiếu (phần còn lại của README do phiên thiết kế viết) |

Không đổi, đã kiểm: `MainScreen.kt`, `MainScreenTest.kt`, `AppNavHost.kt`, `OnboardingNavigation.kt` (chữ ký `onboardingScreen` giữ nguyên), `PermissionPrompt.kt`, `PermissionPromptTest.kt`, `DataStoreOnboardingStatusTest.kt`. Các plan cũ có dòng `tap_if id=onboarding_enable_camera` (T-056, T-058, T-064, T-076) là bản ghi lịch sử, không sửa. Ảnh chuẩn `OnboardingScreen_default_*.png` không được script hay workflow nào đọc theo tên (`git grep` chỉ thấy test của chính module và gói thiết kế). `docs/features/onboarding/design/review/summary.md`: `scripts/design-compare/DesignCompare.java` ghi file này, plan không sửa script, chỉ chạy nó; `scripts/tests/run.sh` kiểm `summary.md` của một thư mục tạm của test; `scripts/device-session.sh` và `.github/workflows/device-session.yml` đọc một `summary.md` khác (thư mục `test-sessions/` của phiên thử trên máy). Không file nào trong số đó đọc file của gói thiết kế, nên không sửa (như plan T-080).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: đọc `FeatureFlag.PREMIUM_POSES` (`feature_premium_poses_enabled`, T-085 thêm, mặc định tắt) bằng `FeatureFlags.enabled()` dạng `Flow` (ADR-0003). Không thêm cờ. `feature_onboarding_enabled` vẫn do `:app` đọc như cũ.
- Quảng cáo: không chạm. Khe `adSlot` mặc định rỗng; vị trí `native_onboarding` là T-083.
- Sự kiện phân tích: thêm `onboarding_step_viewed` (`step`: số thứ tự trang, từ `"1"`), đã có trong spec (mục 6, câu 7). Điểm chặn 4 của `risk-scoring.md`. `onboarding_complete`, `camera_permission_result` không đổi.
- File R2: không chạm.
- Dữ liệu lưu: không thêm khóa DataStore. Ba khóa mới chỉ ở `SavedStateHandle` của ViewModel (`page_index`, `premium_page`, `viewed_steps`), mất khi người dùng đóng app.
- **Quyền truy cập:** Không thêm. Quyền camera (`CAMERA`) đã có, dùng như cũ ở trang cuối.
- **Thư viện và SDK:** Không thêm. `BackHandler` (`androidx.activity.compose`) đã dùng ở `:feature:camera` (`ReviewScreen.kt`) và có trên classpath của module qua convention plugin; `FeatureFlags` từ `:core:config` cũng vậy.

### 3.4. Phương án dự phòng

Không có. Mọi phần lặp lại mẫu đã có (ViewModel đọc cờ như `PosesViewModel`, `BackHandler` như `ReviewScreen`, cột ghim nút ở đáy như màn chào hiện nay).

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (2026-10-31) tìm 254 test dùng tên hay tag của ba file plan sửa; plan đã nhắc tới 29 test, tức mọi test của `:feature:onboarding` và hai test emulator của `:app`. 14 test "Cần xem trước" và 124 test "Nên xem" còn lại đều ở module khác (`:feature:camera`, `:feature:poses`, `:feature:settings`, `:core:*`) và chỉ trùng tên với `Ready`, `RequestPermission`, `onPermissionResult` của lớp khác (ví dụ `CameraUiState.Ready`, `CameraViewModel.onPermissionResult`); không test nào trong đó đọc lớp của `:feature:onboarding` (ADR-0001: feature không phụ thuộc feature khác), nên không bị đổi. Câu mới dưới đây là test viết trước của phiên (bước 3).

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Màn mở ở trạng thái `Ready`, không ghi sự kiện nào | Trạng thái đầu là `Browsing(pages, 0)`; trang 1 ghi `onboarding_step_viewed` | `OnboardingViewModelTest.shown_logsShown_withoutEventOrAnalytics` | Đổi tên `shown_logsShownAndStepOne_withoutEvent`: `expectNoEvents()` giữ; `assertEquals(Browsing(listOf(INTRO, POSES, CAMERA), 0), uiState.value)`; `assertEquals(listOf(step(1)), analytics.events)`; log "Onboarding shown" giữ |
| "Bật camera" bấm được ngay | Chỉ ở trang cuối | `enableCamera_withoutPermission_logsComplete_thenStarted_thenRequestPermission`, `enableCamera_tappedTwice_logsOnce_andRequestsOnce`, `twoTaps_reachFinished_withNothingInBetween`, `enableCamera_permissionAlreadyGranted_finishesWithoutPromptOrResultEvent`, `permissionDeniedWithDialog_logsDenied_andStillFinishes`, `permissionDeniedWithoutDialog_logsNoResult_andStillFinishes`, `permissionGranted_logsResultWithOnboardingSource_andFinishes` | Mỗi test gọi `toLastPage(viewModel)` (hai lần `onNextClick()`, cờ tắt) trước bước đầu; thứ tự sự kiện `OnboardingEvent` giữ nguyên. Danh sách sự kiện phân tích có thêm ba bước đầu: `listOf(step(1), step(2), step(3), COMPLETE)`, `... COMPLETE, resultEvent("denied")`; `permissionGranted_*`: `assertEquals(5, analytics.events.size)` và `events[4] == resultEvent("granted")`. Trạng thái: `RequestingPermission(PAGES_3)`, `Finished(PAGES_3)` |
| Kết quả quyền lạc khi chưa hỏi bị bỏ qua, ở `Ready` | Ở `Browsing(…, 0)` | `permissionResult_whenNotRequesting_isIgnored` | `assertEquals(listOf(step(1)), analytics.events)` thay `isEmpty()`; `assertEquals(Browsing(PAGES_3, 0), uiState.value)`; phần "lần bấm sau vẫn hỏi quyền" gọi `toLastPage` trước; cuối: `analytics.events.filter { it.name != STEP } == listOf(COMPLETE)` |
| Khôi phục khi đang hỏi quyền | Khôi phục cả danh sách trang đã dừng theo cờ, không ghi lại bước | `restoredWhileRequesting_acceptsResult_withoutSecondComplete` | `SavedStateHandle` thêm `"page_index" to 2, "premium_page" to false, "viewed_steps" to intArrayOf(1, 2, 3)`; `assertEquals(RequestingPermission(PAGES_3), …)`; `Finished(PAGES_3)`; thêm `assertTrue(analytics.events.isEmpty())` (không ghi lại bước, không `onboarding_complete`) |
| Khôi phục ở `Ready` đã ghi `onboarding_complete` | Khôi phục ở trang cuối | `restoredReady_withCompletionLogged_tapDoesNotLogAgain` | Đổi tên `restoredOnLastPage_withCompletionLogged_tapDoesNotLogAgain`; `SavedStateHandle` thêm ba khóa như trên; câu khẳng định giữ |
| Thứ tự log đúng 4 dòng | Có thêm log trang | `logs_useOnboardingTag` | Danh sách mới: `"Onboarding shown", "Page shown: 1 intro", "Page shown: 2 poses", "Page shown: 3 camera", "Enable camera tapped", "Asking for the camera permission", "Camera permission granted"`; `all { it.tag == "Onboarding" }` giữ |
| Bố cục một màn theo `design/context/OnboardingScreen_default_*` (minh họa trên 56, chữ, ba điểm, nút trên 753, ghi chú đáy 869) | Bố cục mỗi trang theo gói thiết kế mới | `OnboardingScreenTest.default_layoutMatchesDesign` | Thay bằng `intro_layoutMatchesDesign`, `poses_layoutMatchesDesign`, `premium_layoutMatchesDesign`, `camera_layoutMatchesDesign`: số đo lấy từ `design/context/OnboardingScreen_<trang>_light.md`, sai số 4 dp như cũ. Số cụ thể ghi ở plan phiên bản 2 (mục 8, câu 1). Ý định giữ: bố cục khớp thiết kế trong một bước lưới |
| Màu của hình minh họa, vòng icon điểm đầu, nền | Hình minh họa ở trang 1; vòng icon điểm đầu ở trang 2 | `default_illustrationColors_light` | Đổi tên `intro_illustrationColors_light`: các câu thẻ, huy hiệu, nền giữ nguyên (đo theo tag `onboarding_illustration`, `onboarding_screen`). Câu "first icon circle" chuyển sang test mới `poses_iconCircleColor_light` (`FEATURE_POSES`, 4 dp, 20 dp, `0xFFFFC83D`) |
| Nền tối và nút trắng | Như cũ, ở trang cuối và trang 1 | `default_colors_dark` | Đổi tên `camera_colors_dark`, dựng trang cuối; câu giữ. Thêm câu cùng điểm đo cho `onboarding_next` ở trang 1 (`0xFFFFFFFF`) |
| Bảy chữ của bảng thiết kế trên một màn | Chữ chia theo trang | `default_showsDesignTexts` | Thay bằng `eachPage_showsItsTexts`: mỗi trang có đúng các chữ của trang đó theo bảng ở mục 3.1 (tiếng Việt), và không có chữ của trang khác; hợp các trang chứa đủ 7 chữ cũ (câu `assertEquals(DESIGN_TEXTS.toSet(), union)`) |
| Bấm "Bật camera" gọi callback một lần | Ở trang cuối | `enableCamera_click_callsCallbackOnce` | Dựng `Browsing(PAGES_3, 2)`; câu giữ |
| Đang hỏi quyền: vòng chờ, bỏ qua chạm | Ở trang cuối | `requesting_showsSpinner_andIgnoresClicks` | Dựng `RequestingPermission(PAGES_3)`; câu giữ |
| Màn không chặn nút quay lại | Trang 1 không chặn; từ trang 2 chặn và gọi `onBack` | `screen_doesNotInterceptBack` | Đổi tên `firstPage_doesNotInterceptBack`, dựng `Browsing(PAGES_3, 0)`; câu giữ. Phần mới ở test mới `secondPage_backCallsOnBack` (mục 4) |
| Màn thấp, 7 ngôn ngữ: nút và ghi chú trong màn, nội dung cuộn, minh họa co, điểm cuối cuộn tới được | Nút ghim ở mọi trang; ghi chú chỉ ở trang cuối; minh họa chỉ ở trang 1 | `small_buttonAndNoteVisible_contentScrolls_<en, vi, hi, ptBR, es419, in, th>` | `assertShortScreen()` dựng màn với trạng thái `mutableStateOf` và đi qua cả 4 trang (`PAGES_4`): ở mỗi trang, nút chính đáy ≤ 640 và đỉnh ≥ đáy của `onboarding_content`, cao ≥ 48 dp, phần tử cuối của trang `performScrollTo().assertIsDisplayed()`; trang 1: minh họa ≥ 160 và < 280; trang cuối: ghi chú đáy ≤ 640, đỉnh ≥ đáy của `onboarding_content`, bấm "Bật camera" đúng 1 lần. Tên test đổi `small_everyPage_buttonVisible_contentScrolls_<ngôn ngữ>` |
| Màn lớn nằm ngang: nút và ghi chú trong cửa sổ | Như trên, ở mọi trang | `landscape_buttonAndNoteVisible_w960h600`, `_w960h520` | `assertLandscape()` đi qua 4 trang như trên, so với `onRoot()`; tên test giữ |
| Ảnh chuẩn `OnboardingScreen_default_light`, `_dark`, `_small` | Không còn màn một trang | `OnboardingScreenScreenshotTest.default_light`, `default_dark`, `default_small` | Xóa ba test và ba ảnh (mục 3.2); ảnh mới ở mục 4. Đây là ảnh chuẩn mà plan cho phép đổi, không phải viết lại theo ADR-0031 |
| Test emulator bấm "Bật camera" ở màn chào | Bấm "Tiếp" tới trang cuối trước | `AppLaunchTest.appStarts_passesOnboarding_andReachesCamera`, `SettingsNavigationTest` | Câu khẳng định (tới `camera_screen` trong 10 giây) giữ; chỉ đổi bước dựng (`passOnboarding()`, mục 3.1) |
| Kịch bản emulator bấm `onboarding_enable_camera` | Bấm `onboarding_next` trước | 7 tệp `deviceScenarios` (mục 3.2) | Thêm ba dòng `tap_if id=onboarding_next`; các bước kiểm sau đó giữ |

Giữ nguyên: `OnboardingStringsTest` (chỉ thêm dữ liệu), `PermissionPromptTest`, `MainScreenTest`, `DataStoreOnboardingStatusTest`.

## 4. Kiểm thử

Hằng của test: `PAGES_3 = listOf(INTRO, POSES, CAMERA)`, `PAGES_4 = listOf(INTRO, POSES, PREMIUM, CAMERA)`, `step(n) = AnalyticsEvent("onboarding_step_viewed", mapOf("step" to "$n"))`. `FakeFeatureFlags(premium = MutableStateFlow(false))` trả `premium` cho `PREMIUM_POSES`, `false` cho cờ khác. ViewModel test dùng `MainDispatcherRule` có sẵn.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-5, cờ premium tắt: 3 trang | Unit test | `OnboardingViewModelTest.premiumOff_threePages_withoutPremium`: `uiState.value.pages == PAGES_3`; hai lần `onNextClick()` tới `CAMERA`, `isLastPage` |
| KB-5, cờ premium bật: 4 trang, trang 3 là premium | Unit test | `premiumOn_fourPages_thirdIsPremium`: `pages == PAGES_4`; sau hai lần `onNextClick()`, `page == PREMIUM`; lần thứ ba `page == CAMERA` |
| KB-5, cờ về trễ khi đang ở trang 1, 2 | Unit test | `premiumTurnsOn_onFirstOrSecondPage_listFollows`: cờ `false`, `onNextClick()` (trang 2), đặt cờ `true`: `pages == PAGES_4`, `index == 1`; đối chứng: cờ về `false` thì `PAGES_3` |
| KB-5, cờ đổi khi đã tới trang 3 (giả định mục 8, câu 3) | Unit test | `premiumChanges_afterThirdPage_listStays`: cờ `true`, hai lần `onNextClick()` (trang premium), đặt cờ `false`: `pages == PAGES_4`, `page == PREMIUM`; `onBackClick()` hai lần rồi đặt cờ `true` lần nữa vẫn `PAGES_4` (không theo lại) |
| KB-5, "Tiếp" sang trang kế | Unit test | `next_movesOnePage_untilLast`: mỗi `onNextClick()` tăng `index` 1; ở trang cuối `onNextClick()` không đổi gì, không sự kiện |
| KB-5, Back về trang trước; trang 1 thì không đổi | Unit test | `back_movesToPreviousPage_notBeforeFirst`: tới trang 3, `onBackClick()` hai lần về 0; lần nữa vẫn 0, không log "Back to page" |
| KB-5, Back trong lúc hỏi quyền | Unit test | `back_whileRequestingPermission_isIgnored`: tới trang cuối, `onEnableCameraClick(false)`, `onBackClick()`: vẫn `RequestingPermission` |
| KB-5, `onboarding_step_viewed` khi trang hiện lần đầu, mỗi trang một lần | Unit test | `stepViewed_loggedOncePerStep`: tới cuối, lùi về 1, tới cuối lần nữa: `analytics.events == listOf(step(1), step(2), step(3))`; đối chứng: 4 trang thì có `step(4)` |
| KB-5, không ghi lại khi activity dựng lại hay tiến trình được khôi phục | Unit test | `restoredOnSecondPage_keepsPage_doesNotLogAgain`: `SavedStateHandle(mapOf("page_index" to 1, "viewed_steps" to intArrayOf(1, 2)))`: `index == 1`, `analytics.events.isEmpty()`; `onNextClick()` ghi đúng `step(3)` |
| KB-5, trang khôi phục giữ danh sách đã dừng theo cờ | Unit test | `restoredOnPremiumPage_keepsFourPages_evenIfFlagOff`: `page_index` 2, `premium_page` `true`, cờ `false`: `pages == PAGES_4`, `page == PREMIUM` |
| KB-5, đóng app giữa luồng thì lần sau bắt đầu từ trang 1 | Unit test | `next_doesNotSendStarted`: đi tới trang cuối bằng "Tiếp": `events` không có `Started` (nên `:app` không đánh dấu xong, `DataStoreOnboardingStatusTest` đã kiểm phần lưu); `newViewModel_startsOnFirstPage`: `SavedStateHandle()` rỗng thì `index == 0` |
| KB-1, số chạm bằng số trang + 1; "Bật camera" chỉ ở trang cuối | Unit test | `enableCamera_beforeLastPage_isIgnored`: ở trang 1, 2 `onEnableCameraClick(false)` không gửi sự kiện, không `onboarding_complete`. `taps_equalPagesPlusOne` (3 và 4 trang): đếm số lời gọi người dùng (`onNextClick` tới trang cuối, `onEnableCameraClick`, `onPermissionResult`) tới `Finished` bằng `pages.size + 1` |
| KB-1, KB-2: quyền, `onboarding_complete`, `camera_permission_result` | Unit test | Các test cũ viết lại theo mục 3.5 |
| Log (ADR-0019) | Unit test | `logs_useOnboardingTag` viết lại (mục 3.5); `logs_pageAndBack`: `"Page shown: 2 poses"`, `"Back to page 1"`, `"Premium page on"` |
| KB-5, nút "Tiếp" cùng chỗ trên mọi trang, không nhảy khi thẻ hiện hay ẩn | Compose UI test | `OnboardingScreenTest.bottomButton_samePlace_onEveryPage_withAndWithoutAdSlot`: trạng thái và khe trong `mutableStateOf`; với 4 trang và hai khe (rỗng; `Box(Modifier.fillMaxWidth().height(250.dp))`), nút chính (`onboarding_next` hay `onboarding_enable_camera`) có cùng `top`, `bottom`, `left`, `width` (bằng nhau đúng, không sai số); đối chứng trong test: `onboarding_content` thấp đi 250 dp khi khe có khối |
| KB-6 chuẩn bị cho T-083: khe nằm giữa vùng trang và nút, không che ghi chú | Compose UI test | `adSlot_betweenPageAndButton_belowNoteNever`: khe có khối 250 dp, trang cuối: đỉnh khe ≥ đáy `onboarding_page_area`, đáy khe ≤ đỉnh `onboarding_note`, đáy ghi chú ≤ đỉnh nút. `adSlot_receivesCurrentPage`: khe ghi lại `page` nó nhận; sau mỗi lần đổi trạng thái, giá trị cuối bằng `uiState.page` |
| KB-5, tới trang cuối thì "Bật camera" thay "Tiếp" | Compose UI test | `lastPage_showsEnableCamera_notNext`: trang cuối có `onboarding_enable_camera`, không có `onboarding_next`; trang 1, 2, 3 ngược lại. `next_click_callsOnNextOnce` |
| KB-5, Back từ trang 2 gọi `onBack`; trang 1 không chặn | Compose UI test | `secondPage_backCallsOnBack`: `Browsing(PAGES_3, 1)`, `activity.onBackPressedDispatcher.hasEnabledCallbacks()` là `true`, `onBackPressed()` gọi `onBack` đúng 1 lần; `firstPage_doesNotInterceptBack` (mục 3.5); `requesting_doesNotInterceptBack`: `RequestingPermission` thì không có callback bật |
| KB-5, trang premium chỉ khi cờ bật, không nhắc premium khi tắt | Compose UI test | `threePages_noPremiumText_anywhere`: đi qua `PAGES_3`, không trang nào có `onboarding_premium_title`, `onboarding_premium_body`; chỉ báo trang có 3 chấm |
| Chỉ báo trang | Compose UI test | `pageIndicator_describesCurrentPage`: `Browsing(PAGES_4, 1)` thì node `onboarding_page_indicator_description` có mô tả "Trang 2 trên 4" (tiếng Việt) |
| KB-1, KB-5, màn thấp và xoay: nút luôn thấy, trang cuộn | Compose UI test | `small_everyPage_buttonVisible_contentScrolls_<7 ngôn ngữ>`, `landscape_buttonAndNoteVisible_w960h600`, `_w960h520` (mục 3.5) |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | `eachPage_showsItsTexts` (mục 3.5); `OnboardingStringsTest` thêm 6 khóa vào `ENGLISH`, `VIETNAMESE`; `everyReleaseLanguage_isTranslated` tự kiểm 6 khóa mới ở 7 ngôn ngữ |
| Màu theo hệ thống thiết kế | Compose UI test | `intro_illustrationColors_light`, `poses_iconCircleColor_light`, `camera_colors_dark` (mục 3.5); `premium_badgeColor_light`: tâm huy hiệu trang premium màu `secondaryContainer` sáng (`0xFFFFC83D`) |
| Bố cục khớp thiết kế | Compose UI test | `<intro, poses, premium, camera>_layoutMatchesDesign` (mục 3.5), số đo từ `design/context/` khi có gói (mục 8, câu 1) |
| Giao diện từng trang | Screenshot test | `OnboardingScreenScreenshotTest`, khung `vi-w411dp-h891dp-xxhdpi`, mỗi test một trạng thái `Browsing` cố định: `captureRoboImage("src/test/screenshots/OnboardingScreen_intro_light.png") { ... }`, cùng mẫu cho `_intro_dark`, `_poses_light`, `_poses_dark`, `_premium_light`, `_premium_dark` (dựng `PAGES_4`), `_camera_light`, `_camera_dark` (dựng `PAGES_4`, trang cuối `Browsing`, không vòng chờ) |
| Màn hình thấp (L-028) | Screenshot test khung `vi-w360dp-h640dp-xxhdpi` | `captureRoboImage("src/test/screenshots/OnboardingScreen_intro_small.png") { ... }` (trang 1, minh họa co), `captureRoboImage("src/test/screenshots/OnboardingScreen_camera_small.png") { ... }` (trang cuối, `PAGES_4`, nhiều phần ghim nhất) |
| Màn gập (ADR-0020) | Screenshot test khung `vi-w673dp-h841dp-xxhdpi` | `captureRoboImage("src/test/screenshots/OnboardingScreen_intro_light_unfolded.png") { ... }`, `captureRoboImage("src/test/screenshots/OnboardingScreen_camera_light_unfolded.png") { ... }` |
| Đúng thiết kế (khi có `design/screens/`) | Screenshot test cùng tên với từng ảnh thiết kế, ảnh so sánh `scripts/design-compare.sh onboarding`, rà soát độc lập theo mục 7 của mẫu thiết kế. Hai ảnh thiết kế cũ `OnboardingScreen_default_light`, `OnboardingScreen_default_dark` được phiên Figma thay bằng gói mới; ảnh chuẩn cùng tên bị xóa (mục 3.2). Ảnh `ad` của T-083 khai báo ở plan phiên bản 2, khi các ảnh đó đã có trong `design/screens/` | `OnboardingScreen_<intro\|poses\|premium\|camera>_<light\|dark>.png`, `OnboardingScreen_<intro\|camera>_light_unfolded.png` |
| KB-5, đi luồng, Back, mở gập giữ trang, trang cuối, Back ở trang 1 đóng app | Kiểm tra thủ công (emulator: `feature/onboarding/src/deviceScenarios/T-082-luong-gioi-thieu.txt`) | Tệp kịch bản dưới đây; `device-session` cuối phiên chạy tệp này |
| KB-1, KB-2 trên máy thật: cho phép và từ chối camera sau khi đi hết luồng | Kiểm tra thủ công (máy thật: camera thật) | Bản staging, xóa dữ liệu app: đi hết 4 trang, "Bật camera", cho phép: camera có tư thế; xóa dữ liệu lần nữa, từ chối: camera ở trạng thái chưa có quyền. Đếm chạm: 5 với 4 trang |
| KB-5, 3 trang khi cờ premium tắt, trên máy | Kiểm tra thủ công (máy thật: Remote Config) | Bản staging bật cờ sẵn (`StagingFeatureFlagsModule`, T-085) nên tắt trên Firebase staging không có tác dụng (mục 8, câu 5). Đề xuất: kiểm 3 trang bằng unit test và test giao diện ở trên; trên máy chỉ kiểm 4 trang |
| KB-5, mở gập thật | Kiểm tra thủ công (máy thật: gập có bản lề) | Máy gập: đang ở trang 2, mở máy và gập lại: vẫn trang 2, chữ và nút không bị cắt |
| Chữ dịch ở 5 ngôn ngữ AI dịch | Kiểm tra thủ công (người: đọc bản dịch) | Người biết tiếng đọc 6 chuỗi mới trước khi phát hành (câu 1 của spec) |

Kịch bản emulator `feature/onboarding/src/deviceScenarios/T-082-luong-gioi-thieu.txt` (bản staging có cờ premium bật sẵn sau T-085, nên 4 trang):

```text
ticket: T-082
criterion: Luồng giới thiệu: Tiếp sang trang sau, Back về trang trước, mở gập giữ trang, trang cuối có Bật camera, Back ở trang 1 đóng app (plan T-082, mục 4)
timeout: 240
fresh
consent decline 30
wait id=onboarding_page_intro 60
expect id=onboarding_next
expect_no id=onboarding_enable_camera
tap id=onboarding_next
wait id=onboarding_page_poses 10
device unfold
wait id=onboarding_page_poses 15
shot poses-unfolded
device fold
wait id=onboarding_page_poses 15
back
wait id=onboarding_page_intro 10
tap id=onboarding_next
wait id=onboarding_page_poses 10
tap id=onboarding_next
wait id=onboarding_page_premium 10
shot premium
tap id=onboarding_next
wait id=onboarding_page_camera 10
expect id=onboarding_enable_camera
expect_no id=onboarding_next
shot camera-page
back
wait id=onboarding_page_premium 10
back
back
wait id=onboarding_page_intro 10
back
expect_no id=onboarding_screen
```

Phiên chạy `scripts/device-scenario.py check feature/onboarding/src/deviceScenarios/T-082-luong-gioi-thieu.txt` sau khi tạo tệp (đã chạy trên bản nháp, mục 4.1).

Thứ tự commit (L-025): commit test của bước 3 có `OnboardingPage.kt` và `OnboardingUiState.kt` mới cùng khung rỗng của `onNextClick`, `onBackClick`, tham số `featureFlags`, `onNext`, `onBack`, `adSlot` (để test biên dịch được rồi đỏ ở câu khẳng định), dữ liệu 6 khóa của `OnboardingStringsTest` cùng 6 chuỗi tiếng Anh và tiếng Việt (test so chữ cần chuỗi có thật). Với khung rỗng, các test giữ hành vi cũ sẽ xanh ngay (`requesting_showsSpinner_andIgnoresClicks`, `firstPage_doesNotInterceptBack`, `PermissionPromptTest`); các test còn lại đỏ vì chưa có trang, chưa có sự kiện bước.

**Ngoại lệ viết test trước:** chuỗi dịch của 5 ngôn ngữ còn lại và icon `ic_onboarding_workspace_premium.xml` là tài nguyên, kiểm sau bằng `everyReleaseLanguage_isTranslated` và ảnh chuẩn. Hai test emulator của `:app` và 7 tệp kịch bản đã có chỉ đổi bước dựng, chạy ở CI (`ui-test`) và `device-session`, không "thất bại trước" được trên JVM. Tệp kịch bản mới chạy ở `device-session` cuối phiên. Phần nối `OnboardingRoute` (`onNext`, `onBack` tới ViewModel) không có test JVM riêng, như hiện nay; kịch bản emulator đi qua nó.

### 4.1. Chạy thử

Không cần (E dưới 2): không có kỹ thuật mới; mọi phần là biến thể của mẫu đã có (mục 3.4). Lúc soạn plan (2026-10-31) đã chạy:

- `scripts/existing-tests.py docs/features/onboarding/plans/T-082-luong-gioi-thieu-nhieu-trang.md`: 254 test, 29 đã nhắc trong plan; phần còn lại chỉ trùng tên ở module khác (mục 3.5).
- `scripts/device-scenario.py check` trên bản nháp của tệp kịch bản ở mục 4, chép vào thư mục tạm của phiên soạn plan, không commit: không có lỗi cú pháp; 16 dòng báo tag `onboarding_page_intro`, `onboarding_page_poses`, `onboarding_page_premium`, `onboarding_page_camera`, `onboarding_next` "không có trong mã nguồn", đúng vì code chưa viết. Phiên chạy lại lệnh này sau bước 4 và phải qua.

Đoạn code ở mục 3.1 là phác thảo, chưa biên dịch.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** thu thập dữ liệu (thêm sự kiện `onboarding_step_viewed`, điểm 4). Rủi ro Cao, cần duyệt chéo; ticket ghi sơ bộ "trung bình" vì chưa tính sự kiện. Sự kiện đã có trong spec đã chốt (câu 7). Điểm dưới đây chỉ để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Code trong một module, đổi giao diện công khai của `OnboardingUiState` và `OnboardingViewModel`; test emulator của `:app` và kịch bản emulator của ba module chỉ đổi bước dựng |
| B. Độ rõ của yêu cầu | 2 | Spec đã chốt nhưng chưa có thiết kế của luồng (bố cục, chữ trang 2 tới cuối, chỉ báo trang); câu 3 (cờ đổi giữa luồng) ảnh hưởng cách làm |
| C. Khả năng kiểm chứng tự động | 1 | ViewModel, bố cục, Back, chữ có test JVM; đi luồng có kịch bản emulator; quyền camera thật và máy gập kiểm bằng tay |
| D. Ảnh hưởng chéo | 2 | Sửa test emulator và kịch bản của `:app`, `:feature:camera`, `:feature:poses`; T-083 dựa vào cấu trúc màn; phụ thuộc cờ của T-085 |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu đã có: cờ dạng `Flow` như `PosesViewModel`, `BackHandler` như `ReviewScreen`, nút ghim đáy như màn chào |
| **Tổng** | 8 (tham khảo) | **Mức: Cao** (điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: không sửa module core. Dùng `PrimaryButton`, `Spacing`, `Radius`, `Stroke` của `:core:designsystem` và `FeatureFlags` của `:core:config` mà không sửa. Sửa `app/src/androidTest/.../AppLaunchTest.kt`, `SettingsNavigationTest.kt` và 7 tệp `deviceScenarios` của `:app`, `:feature:camera`, `:feature:poses` (chỉ bước qua màn chào).
- Task khác trong sprint có thể xung đột:
  - T-085 (cờ `FeatureFlag.PREMIUM_POSES`, `StagingFeatureFlagsModule`): T-082 phụ thuộc; chạy sau khi T-085 merge.
  - T-083 (thẻ native ở luồng): sửa cùng `OnboardingScreen.kt`, `OnboardingScreenTest.kt`, `OnboardingScreenScreenshotTest.kt`; chạy sau T-082, plan T-083 dẫn theo mục 3.1 (Điểm cho T-083).
  - T-080 (đã duyệt, chạy ở RUN-20261031-02): không có file trùng (đã so mục 3.2 của plan T-080).
  - Plan khác thêm tệp kịch bản emulator có `tap_if id=onboarding_enable_camera` trước khi T-082 merge thì cần thêm ba dòng `tap_if id=onboarding_next` như mục 3.1 (phần ngoài mục 3.2 theo ADR-0029).
- Đề xuất thứ tự chạy: T-085, rồi T-082, rồi T-083; không cùng một lúc. Thiết kế Figma của luồng (mục 8, câu 1) phải có trước khi duyệt plan phiên bản 2.

## 7. Điều kiện dừng riêng

- `git status --short -- '*/src/test/screenshots/'` có ảnh chuẩn đổi ngoài các ảnh `OnboardingScreen_*` của mục 3.2: dừng (màn khác không được đổi).
- `FeatureFlag.PREMIUM_POSES` chưa có trên nhánh chính lúc bắt đầu task (T-085 chưa merge hay đặt tên khác): dừng, không tự thêm cờ.
- Test `MainScreenTest` hay `DataStoreOnboardingStatusTest` đỏ: dừng; luồng không được đổi cách `:app` chọn màn và đánh dấu xong.

## 8. Câu hỏi cho người duyệt

1. **Chưa có thiết kế của luồng (điểm chặn).** `docs/features/onboarding/design/` chỉ có màn một trang. Plan không tự dựng Figma. Cần một phiên Figma có người theo dõi (`docs/CLAUDE.md`, mục Khi thiết kế trên Figma) dựng các frame dưới đây, khung 411 x 891 (frame `_unfolded` 673 x 841), hướng Sticker, rồi lập gói bàn giao (`figma-export.py`, `context/`, README mục 1, 2, 3, 6) thay cho hai frame `default`:
   - Của T-082: `OnboardingScreen_intro_light`, `OnboardingScreen_intro_dark`, `OnboardingScreen_poses_light`, `OnboardingScreen_poses_dark`, `OnboardingScreen_premium_light`, `OnboardingScreen_premium_dark`, `OnboardingScreen_camera_light`, `OnboardingScreen_camera_dark`, `OnboardingScreen_intro_light_unfolded`, `OnboardingScreen_camera_light_unfolded`. Khung thấp 360 x 640 không cần frame (design-workflow, mục 2): README mục 6 ghi phần nào cuộn, phần nào ghim; app kiểm bằng `OnboardingScreen_intro_small.png`, `OnboardingScreen_camera_small.png`.
   - Của T-083 (dựng cùng phiên để T-083 dùng): `OnboardingScreen_intro-ad_light`, `OnboardingScreen_intro-ad_dark`, `OnboardingScreen_poses-ad_light`, `OnboardingScreen_poses-ad_dark`, `OnboardingScreen_premium-ad_light`, `OnboardingScreen_premium-ad_dark`, `OnboardingScreen_camera-ad_light`, `OnboardingScreen_camera-ad_dark` (thẻ 250 dp trong khe), `OnboardingScreen_intro-ad-wait_light`, `OnboardingScreen_intro-ad-wait_dark` (trang 1 chờ quảng cáo, nút "Tiếp" mờ, khung chờ của thẻ).
   - Ràng buộc cho thiết kế từ spec và plan: nút chính ghim ở đáy, cùng chỗ trên mọi trang; khe thẻ nằm giữa vùng trang và nút; ở trang cuối ghi chú quyền nằm giữa khe và nút; không có nút "Bỏ qua".
   - Đề xuất của AI: dựng gói trước, rồi AI sửa plan thành phiên bản 2 với số đo của `context/` (mục 3.5, dòng `*_layoutMatchesDesign`), chữ đã chốt và dòng khai báo các ảnh `ad` mà T-083 vẽ (quy tắc 4 của `scripts/plan-check.py`). Tên trạng thái `intro`, `poses`, `premium`, `camera` là đề xuất; phiên Figma đổi thì plan đổi theo.
2. **Chữ của trang 2, 3 và trang cuối.** Spec chỉ có nội dung (mục 4); tiêu đề mới là đề xuất ở mục 3.1 ("Tư thế cho mọi kiểu ảnh", "Tư thế premium", "Xem một quảng cáo để mở một tư thế premium, rồi dùng mãi về sau.", "Sẵn sàng chụp", "Tiếp", "Trang 2 trên 4"). Đề xuất của AI: phiên Figma chốt chữ trong bảng chữ của gói; plan phiên bản 2 chép lại. Chữ cũ (tiêu đề, giới thiệu, ba điểm, nút, ghi chú) giữ nguyên, chỉ chia sang các trang.
3. **Cờ premium đổi khi luồng đang chạy.** Remote Config có thể trả giá trị sau khi trang 1 đã hiện. Đề xuất của AI: danh sách trang theo cờ khi người dùng còn ở trang 1 hay 2 (hai trang này giống nhau ở cả hai danh sách); từ lúc tới trang 3 thì danh sách giữ nguyên tới hết luồng, kể cả khi quay lại. Như vậy người dùng không bao giờ thấy trang premium biến mất dưới tay, và vẫn theo cờ như ADR-0003 trong phần lớn trường hợp.
4. **Chỉ báo trang và hiệu ứng chuyển trang.** Spec không nói. Đề xuất của AI: có hàng chấm ở trên cùng (cố định, không cuộn, không chiếm chỗ ở đáy nơi đã có khe thẻ, ghi chú và nút); trang đổi ngay, không hiệu ứng trượt. Thiết kế muốn hiệu ứng trượt thì thêm ở plan phiên bản 2 (ảnh chuẩn chụp trạng thái đứng yên nên không đổi).
5. **Kiểm 3 trang trên máy.** Ticket (Định nghĩa hoàn thành) ghi "tắt cờ premium trên Firebase staging thì còn 3 trang". Nếu T-085 bật cờ ở `StagingFeatureFlagsModule` (như ticket T-085 ghi) thì cờ luôn bật trên staging (`DefaultFeatureFlags` trả `true` cho cờ bật tại chỗ, không đọc Firebase), nên không tắt được từ Firebase. Đề xuất của AI: kiểm 3 trang bằng unit test và test giao diện (mục 4); bỏ ý đó khỏi kiểm tra trên máy. Cách khác: T-085 bật cờ bằng giá trị mặc định của staging trong `StagingRemoteValuesModule` (Firebase ghi đè được); đó là quyết định của plan T-085.
6. **Tên cờ phải khớp plan T-085.** Plan này dùng `FeatureFlag.PREMIUM_POSES` với khóa `feature_premium_poses_enabled`, theo ticket T-085. Plan T-085 đang soạn song song: nếu đặt tên khác, plan này sửa theo trước khi duyệt. Đề xuất: giữ đúng tên của ticket.
7. **Tham số của `onboarding_step_viewed`.** Spec chỉ có `step`. Với cờ premium tắt, `step` 3 là trang xin quyền; bật thì `step` 3 là trang premium và `step` 4 là trang xin quyền. Báo cáo "người dùng bỏ app ở trang nào" phải tách theo thời kỳ bật cờ. Đề xuất của AI: làm đúng spec (chỉ `step`) trong plan này. Nếu Nhật muốn rõ hơn, sửa spec trước để thêm tham số `page` (`intro`, `poses`, `premium`, `camera`), rồi plan phiên bản 2 thêm một câu vào `onPageShown` và test.
8. **Mức rủi ro.** Ticket ghi "trung bình"; plan xếp Cao vì thêm sự kiện phân tích là điểm chặn 4. Đề xuất: giữ Cao, duyệt chéo Tuấn và Nhật; sự kiện đã có trong spec đã chốt nên không cần hỏi thêm người phụ trách sản phẩm.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:**
- **Người duyệt:**
- **Ghi chú:**

## 10. Nhật ký thực hiện
