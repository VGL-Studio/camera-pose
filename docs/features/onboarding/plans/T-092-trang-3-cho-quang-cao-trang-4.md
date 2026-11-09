---
ticket: T-092
title: Trang 3 của luồng giới thiệu chờ quảng cáo cho trang 4, tối đa 3 giây
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 10.9M        # cỡ M theo số gợi ý của scripts/token-usage.py estimate (2026-11-09); không nâng tay
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-092: TRANG 3 CHỜ QUẢNG CÁO CHO TRANG 4, TỐI ĐA 3 GIÂY

## 1. Tóm tắt

`:core:ads` thêm một hàm đọc: pool `native_step` có sẵn quảng cáo rảnh cho bước kế tiếp không. `:feature:onboarding` dùng lại cơ chế chờ của trang 1 để giữ nút "Tiếp" ở trang 3 tối đa 3 giây, một lần mỗi luồng. Kiểm chứng bằng unit test pool, test giao diện của cơ chế chờ theo đồng hồ, 2 screenshot test cùng tên với ảnh thiết kế `guide-ad-wait`, và kịch bản emulator.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/onboarding/spec.md` (confirmed 2026-11-09, câu 12):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-6 | Trang 3 hiện lần đầu, quảng cáo được phép, pool chưa có sẵn quảng cáo cho trang 4 thì "Tiếp" mờ tối đa 3 giây | "Được phép" là `rememberNativeStepAdAllowed`, như trang 1. "Có sẵn" là pool còn ít nhất một quảng cáo rảnh; quảng cáo đang hiện ở thẻ trang 3 không tính |
| KB-6 | Có quảng cáo thì nút dùng được ngay; hết 3 giây thì nút dùng được, trang 4 có thể chưa có thẻ | Cơ chế chờ kết thúc khi tín hiệu đúng hoặc hết giờ |
| KB-6 | Đã chờ hay đã rời trang 3 thì quay lại không chờ; ra nền thì hết chờ | Giống trang 1: chờ một lần, `begun` lưu qua dựng lại activity |
| KB-6 | Nút "Tiếp" cùng một chỗ, thẻ không che nút | Không đổi; chỉ đổi `enabled` |

Thiết kế tham chiếu: `features/onboarding/design/README.md` (`approved`, Nhật, 2026-11-09), các ảnh trong `design/screens/`: `OnboardingScreen_guide-ad-wait_light`, `OnboardingScreen_guide-ad-wait_dark`.

ADR liên quan: ADR-0013 (quảng cáo qua `:core:ads`), ADR-0019 (log), ADR-0020 (ảnh thiết kế và screenshot cùng tên), ADR-0022 (thẻ không che điều khiển).

## 3. Giải pháp

### 3.1. Cách làm

**`:core:ads`.** Thêm ba chỗ, chỉ đọc, không nạp gì:

```kotlin
// NativeAdPool
/** A free ad is waiting, ready for the next cell. Only reads the state, never loads. */
fun hasFreeAd(): Boolean = free.isNotEmpty()

// AdsController
internal fun poolHasFreeAd(placement: AdPlacement): Boolean = nativePools.pool(placement)?.hasFreeAd() ?: false

// NativeFlowStep.kt
/**
 * Whether the pool of [placement] holds a free ad for the next step of the flow, read every [NEXT_AD_POLL_MILLIS].
 * False with no ads controller. An app that holds its Next button until the next step has an ad uses this.
 */
@Composable
fun rememberNativeStepNextAdReady(placement: AdPlacement): Boolean
```

Hàm composable kiểm định dạng bằng `requireStepFormat`, và trong `LaunchedEffect(ads, placement)` đọc `ads.poolHasFreeAd(placement)` mỗi 200 ms vào một state. Đọc định kỳ để chỉ có một đường đọc, không phải thêm thông báo vào mọi đường đổi pool (lấy, trả, nạp xong, hết hạn), cùng cách làm của ô chờ pool (P-06).

**Cơ chế chờ** (`IntroAdWait.kt`). Tách thân hàm thành `rememberPageAdWait(onPage, allowed, hasAd, maxMillis, usedUpBeforeFirstVisit, onStarted, onEnded)`. `rememberIntroAdWait` giữ nguyên chữ ký, gọi hàm mới với `usedUpBeforeFirstVisit = true`, nên `IntroAdWaitTest` không đổi. Điểm khác: cơ chế cũ coi đã dùng hết ngay khi màn không ở trang đó, nên trang 3 sẽ không bao giờ chờ, vì luồng bắt đầu ở trang 1:

```kotlin
var visited by rememberSaveable { mutableStateOf(false) }
LaunchedEffect(onPage) {
    if (onPage) visited = true else if (visited || usedUpBeforeFirstVisit) begun = true
}
```

Thêm `internal const val GUIDE_AD_WAIT_MILLIS = 3_000L`.

**Màn hình** (`OnboardingScreen.kt`). `OnboardingAdContent` nhận thêm `nextAdReady: Boolean = false`, `onGuideAdWaitStarted`, `onGuideAdWaitEnded`; gọi `rememberPageAdWait(onPage = trang là GUIDE và trạng thái Browsing, allowed = adsAllowed, hasAd = nextAdReady, maxMillis = GUIDE_AD_WAIT_MILLIS, usedUpBeforeFirstVisit = false, ...)`; `nextEnabled = !introHeld && !guideHeld`. `OnboardingRoute` truyền `rememberNativeStepNextAdReady(AdPlacement.NATIVE_ONBOARDING)` và hai hàm log của ViewModel.

**ViewModel.** Thêm `onGuideAdWaitStarted()`, `onGuideAdWaitEnded(reason)`, log thẻ `Onboarding`: `Guide ad wait started`, `Guide ad wait ended: <lý do>`. Nút "Bật camera" không bao giờ bị giữ.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdPool.kt` | `hasFreeAd()` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsController.kt` | `poolHasFreeAd()` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeFlowStep.kt` | `rememberNativeStepNextAdReady` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdPoolTest.kt` | Test `hasFreeAd` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeFlowStepTest.kt` | Test hàm mới |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/IntroAdWait.kt` | `rememberPageAdWait` |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreen.kt` | Giữ nút ở trang 3 |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingViewModel.kt` | Hai hàm log |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/IntroAdWaitTest.kt` | Thêm test của cơ chế mới; test cũ giữ nguyên |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingAdContentTest.kt` | Test giữ nút ở trang 3 |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingViewModelTest.kt` | Test log |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreenScreenshotTest.kt` | Hai ảnh `guide-ad-wait` |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_guide-ad-wait_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_guide-ad-wait_dark.png` | Ảnh chuẩn mới |
| Sửa | `feature/onboarding/src/deviceScenarios/T-082-luong-gioi-thieu.txt` | Chờ nút dùng được ở trang 3 |
| Sửa | `feature/onboarding/src/deviceScenarios/T-083-gioi-thieu-cho-trang-1.txt` | Chờ nút dùng được ở trang 3 |
| Ghi lại | `docs/features/onboarding/design/review/` | `scripts/design-compare.sh onboarding` sinh lại |
| Sửa | `docs/features/onboarding/design/README.md` | Kết quả đối chiếu của 2 ảnh ở mục 8 |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Theo `is_show_native_onboarding` qua `rememberNativeStepAdAllowed`.
- Quảng cáo: vị trí `native_onboarding` đã có trong spec (KB-6, câu 12); không đổi định dạng, pool hay khóa. Đổi lúc người dùng được đi tiếp, nên ghi điểm chặn `ads`.
- Sự kiện phân tích: không chạm; chỉ thêm log của `AppLogger`.
- File R2: không chạm.
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có.

### 3.5. Hành vi đã có bị đổi

Từ `scripts/existing-tests.py` (2026-11-09). Plan chỉ thêm tham số có giá trị mặc định và thêm hàm; `rememberIntroAdWait` giữ nguyên chữ ký và hành vi. Bốn test ở nhóm "Nên xem" (`NativeFlowStepTest.nativeStepAdAllowed_noController_false`, `nativeStepAdAllowed_otherFormat_throws`, `nativeStepAdAllowed_withController_followsConsentSwitchAndAdFree`, `requireStepFormat_rejectsOtherFormats`) kiểm hai hàm mà plan chỉ gọi, không sửa, nên giữ nguyên. `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingStringsTest.kt` chỉ nhắc `docs/features/onboarding/design/README.md` trong chú thích, không đọc tệp này, nên không bị ảnh hưởng khi plan thêm kết quả đối chiếu vào README.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Chỉ trang 1 giữ nút "Tiếp" | Trang 3 cũng giữ khi `adsAllowed` và `nextAdReady` sai | `OnboardingAdContentTest.pageTwo_notHeld` (trang 2 không giữ), `notAllowed_notHeld` | Giữ cả hai, trang 2 vẫn không giữ |
| Dùng hết lượt chờ khi không ở trang đó | `rememberIntroAdWait` giữ nguyên qua `usedUpBeforeFirstVisit = true` | `IntroAdWaitTest.allowedAfterLeavingPageOne_noWait`, `leftPage_released_notHeldWhenBack` | Giữ nguyên |
| Kịch bản bấm "Tiếp" ở trang 3 ngay | Chờ nút dùng được rồi mới bấm | Tệp kịch bản T-082, T-083 | Thêm dòng `wait id=onboarding_next enabled=true 15` trước lần bấm ở trang 3; `T-086` đã có dòng chờ này |

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Pool báo có quảng cáo rảnh | Unit test | `NativeAdPoolTest.hasFreeAd_falseWhenEmpty_trueAfterLoad_falseAfterClaim` |
| Hàm chỉ đọc, không nạp | Unit test | `NativeAdPoolTest.hasFreeAd_doesNotStartLoad` |
| Tín hiệu cho app | Compose UI test | `NativeFlowStepTest.nextAdReady_noController_false`, `nextAdReady_followsPool`, `nextAdReady_wrongFormat_throws` |
| KB-6, trang 3 giữ tối đa 3 giây | Compose UI test | `IntroAdWaitTest.pageWait_notUsedUpOnEarlierPages_heldThreeSeconds`, đồng hồ dừng: tại 2999 ms nút mờ, 3001 ms nút dùng được |
| KB-6, có quảng cáo thì dùng được ngay | Compose UI test | `IntroAdWaitTest.pageWait_nextAdComes_releasedAtOnce` |
| KB-6, đã có sẵn thì không chờ | Compose UI test | `IntroAdWaitTest.pageWait_readyOnArrival_neverHeld` |
| KB-6, rời trang rồi quay lại không chờ | Compose UI test | `IntroAdWaitTest.pageWait_leftAndBack_notHeldAgain` |
| KB-6, ra nền hết chờ (L-091) | Compose UI test | `IntroAdWaitTest.pageWait_backgroundMidway_releasedOnReturn` |
| Dựng lại activity không chờ lại (L-061) | Compose UI test | `IntroAdWaitTest.pageWait_recreatedActivity_notHeldAgain` |
| Trang 3 giữ nút, các trang khác không | Compose UI test | `OnboardingAdContentTest.guide_allowedNoNextAd_nextHeld_threeSeconds`, `guide_nextAdReady_notHeld`, `guide_notAllowed_notHeld`, `camera_neverHeld` |
| Log | Unit test | `OnboardingViewModelTest.guideAdWait_logsStartAndEnd` |
| Đúng thiết kế | Screenshot test cùng tên với từng ảnh thiết kế, ảnh so sánh `scripts/design-compare.sh onboarding`, rà soát độc lập theo mục 7 của mẫu thiết kế; đồng hồ dừng vì không có hiệu ứng lặp | `OnboardingScreen_guide-ad-wait_light.png`, `OnboardingScreen_guide-ad-wait_dark.png` |
| Ảnh thiết kế của ticket khác | Ảnh thiết kế của ticket khác (T-091): `OnboardingScreen_intro_light`, `OnboardingScreen_intro_dark`, `OnboardingScreen_intro_light_unfolded`, `OnboardingScreen_intro-ad_light`, `OnboardingScreen_intro-ad_dark`, `OnboardingScreen_intro-ad-wait_light`, `OnboardingScreen_intro-ad-wait_dark`, `OnboardingScreen_poses_light`, `OnboardingScreen_poses_dark`, `OnboardingScreen_poses-ad_light`, `OnboardingScreen_poses-ad_dark`, `OnboardingScreen_guide_light`, `OnboardingScreen_guide_dark`, `OnboardingScreen_guide-ad_light`, `OnboardingScreen_guide-ad_dark`, `OnboardingScreen_camera_light`, `OnboardingScreen_camera_dark`, `OnboardingScreen_camera_light_unfolded`, `OnboardingScreen_camera-ad_light`, `OnboardingScreen_camera-ad_dark`, `OnboardingScreen_premium_light`, `OnboardingScreen_premium_dark`, `OnboardingScreen_premium-ad_light`, `OnboardingScreen_premium-ad_dark` | Vẽ ở T-091; bốn ảnh premium do T-091 xóa |
| Luồng qua đủ 4 trang khi trang 3 có thể chờ | Kiểm tra thủ công (emulator: `feature/onboarding/src/deviceScenarios/T-082-luong-gioi-thieu.txt`) | Tệp kịch bản |
| Tỷ lệ trang 4 có thẻ khi bấm nhanh | Kiểm tra thủ công (máy thật: quảng cáo thật) | Trên bản staging, cài mới, bấm "Tiếp" nhanh qua 4 trang vài lần, đọc log `VGL.Onboarding` và xem trang 4 có thẻ không |

**Ngoại lệ viết test trước:** tệp kịch bản emulator (test chạy ở `device-session` cuối phiên).

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 1, quảng cáo (đổi lúc người dùng được đi tiếp vì quảng cáo); điểm 7, module core dùng chung (`:core:ads`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module, thêm một hàm công khai của `:core:ads` |
| B. Độ rõ của yêu cầu | 0 | Spec đã xác nhận, thiết kế đã duyệt |
| C. Khả năng kiểm chứng tự động | 1 | Cơ chế chờ có test theo đồng hồ; tỷ lệ có thẻ thật phải xem trên máy |
| D. Ảnh hưởng chéo | 1 | Chỉ thêm code trong `:core:ads`, không đổi hành vi có sẵn |
| E. Độ mới của kỹ thuật | 1 | Biến thể của cơ chế chờ trang 1 và cách đọc định kỳ của P-06 |
| **Tổng** | 6 | **Mức: Cao (chạm điểm chặn)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `:core:ads`. Thay đổi có ích cho mọi app dùng thẻ bước: ghi đề xuất về dự án mẫu vào mô tả PR (`docs/ops/from-app.md`).
- Task khác trong sprint có thể xung đột: T-090 cùng sửa `AdsController.kt`, khác vùng; T-091 sửa các file của `:feature:onboarding`, nên phải chạy trước.
- Đề xuất thứ tự chạy: sau T-091.

## 7. Điều kiện dừng riêng

- Cần đổi cỡ pool hay cách nạp bù để đạt tiêu chí thì dừng, hỏi người.

## 8. Câu hỏi cho người duyệt

1. **Pool không thể nạp thêm (lỗi mạng, hết quảng cáo) thì trang 3 vẫn chờ đủ 3 giây.** Đề xuất của AI: giữ đúng spec câu 12 cho đơn giản, vì chỉ chờ một lần và tối đa 3 giây. Trả lời: theo đề xuất của AI (Nhật, 2026-11-09).

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
