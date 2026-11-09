---
ticket: T-091
title: Luồng giới thiệu luôn 4 trang, có ảnh minh họa và chữ mới
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 10.9M        # cỡ M theo số gợi ý của scripts/token-usage.py estimate (2026-11-09); không nâng tay
blocking_points: [ads]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-091: LUỒNG GIỚI THIỆU LUÔN 4 TRANG, CÓ ẢNH MINH HỌA VÀ CHỮ MỚI

## 1. Tóm tắt

Luồng giới thiệu của `:feature:onboarding` thành danh sách cố định 4 trang (`intro`, `poses`, `guide`, `camera`), bỏ theo dõi cờ premium trong ViewModel. Mọi trang dùng chung một bố cục: ảnh WebP đóng gói trong `res/drawable-nodpi`, tiêu đề, lời; ảnh co trước khi thiếu chỗ. Kiểm chứng bằng unit test ViewModel, test giao diện Robolectric theo số đo của `design/context/`, screenshot test cùng tên với 20 ảnh thiết kế, và kịch bản emulator.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/onboarding/spec.md` (confirmed 2026-11-09, câu 10, 11):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Màn loading xong thì trang 1 hiện, đúng thiết kế; số chạm bằng số trang + 1 | Luôn 5 chạm: ba "Tiếp", "Bật camera", "Cho phép". `:app` không đổi |
| KB-1 | Xoay máy không cắt chữ hay nút; màn thấp cuộn được, nút luôn thấy | Ảnh co trước, thấp nhất 160, rồi vùng trang cuộn. Nút ghim ở đáy như hiện nay |
| KB-5 | Bấm "Tiếp" thì trang kế hiện; nút cùng một chỗ; trang cuối có "Bật camera" | Không đổi cấu trúc màn của T-082 |
| KB-5 | Cờ `feature_premium_poses_enabled` bật hay tắt thì đều 4 trang, 4 chấm, không trang nào nhắc premium | ViewModel không đọc cờ này nữa. Cờ vẫn dùng ở `:feature:poses`, `:feature:camera` |
| KB-5 | Trang hiện lần đầu thì ghi `onboarding_step_viewed` với số thứ tự trang | Không đổi; `step` luôn từ 1 tới 4 |
| KB-5 | Xoay máy, mở gập giữ trang; đóng app giữa luồng thì bắt đầu lại | Không đổi (`KEY_PAGE_INDEX`) |
| KB-6 | Thẻ native 250 dp ở mọi trang; trang 1 chờ tối đa 5 giây; thẻ không che nút và ghi chú | Không đổi: `adSlot`, `IntroAdWait`, `NativeStepAd` giữ nguyên. Ảnh lấp chỗ còn lại phía trên thẻ |

Thiết kế tham chiếu: `features/onboarding/design/README.md` (`approved`, Nhật, 2026-11-09; Figma section "Onboarding flow v2"), các ảnh trong `design/screens/`: `OnboardingScreen_<intro|poses|guide|camera>_<light|dark>`, `OnboardingScreen_<intro|poses|guide|camera>-ad_<light|dark>`, `OnboardingScreen_intro-ad-wait_<light|dark>`, `OnboardingScreen_<intro|camera>_light_unfolded`.

ADR liên quan: ADR-0001 (feature chỉ phụ thuộc core), ADR-0003 (cờ là `Flow`; plan bỏ một chỗ đọc), ADR-0015 (file tĩnh trên R2; ảnh này đóng gói trong app theo spec, câu 10), ADR-0019 (log), ADR-0020 (ảnh thiết kế và screenshot cùng tên), ADR-0022 (thẻ không che điều khiển), ADR-0029, ADR-0031 (test cũ, mục 3.5).

## 3. Giải pháp

### 3.1. Cách làm

**Danh sách trang** (`OnboardingPage.kt`). `PREMIUM("premium")` đổi thành `GUIDE("guide")`, giữ thứ tự; `onboardingPages(premium)` đổi thành hằng:

```kotlin
/** The pages of the flow, always four (spec onboarding, question 10). */
internal val OnboardingPages: List<OnboardingPage> = OnboardingPage.entries
```

**ViewModel** (`OnboardingViewModel.kt`). Bỏ tham số `featureFlags`, các trường `premium`, `frozen`, hằng `KEY_PREMIUM`, `FREEZE_INDEX`, khối `viewModelScope.launch` theo dõi cờ và các import thừa; `initialState()` và `onNextClick()` dùng `OnboardingPages`. Phần quyền, `onPageShown`, `KEY_PAGE_INDEX`, `KEY_VIEWED_STEPS` giữ nguyên. Khóa `premium_page` còn trong `SavedStateHandle` của tiến trình cũ không ai đọc, vô hại.

**Màn hình** (`OnboardingScreen.kt`). `OnboardingPageContent` chỉ còn một nhánh cho mọi trang:

```kotlin
ShrinkFirstLayout(
    viewportHeight = viewportHeight,
    illustration = { OnboardingIllustration(page, Modifier.fillMaxSize().testTag(OnboardingTestTags.ILLUSTRATION)) },
    text = {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            PageTitle(page.title)
            PageBody(page.body)
        }
    },
)
```

`page.title`, `page.body` là hai hàm mở rộng `private` ánh xạ trang sang `@StringRes`. Bỏ `IconBadge`, `FeatureRows`, `FeatureRow`, các kích thước và tag của chúng (`BADGE`, `FEATURES`, `FEATURE_*`); `PAGE_PREMIUM` đổi thành `PAGE_GUIDE` với giá trị `onboarding_page_guide`; thêm `BUBBLE = "onboarding_bubble"`. `MaxIllustrationHeight` đổi từ 280 sang 360; `MinIllustrationHeight` giữ 160.

**Ảnh** (`OnboardingIllustration.kt`, viết lại). `OnboardingIllustration(page, modifier)` vẽ `Image(painterResource(page.art), contentDescription = null, contentScale = ContentScale.Fit)` trong `BoxWithConstraints`. Riêng `GUIDE`, tính khung ảnh sau khi `Fit` theo tỉ lệ 951 : 1080, rồi đặt `Text` chữ bong bóng có tâm ở x 76,8% và y 15,2% của khung ảnh, `softWrap = false`, `maxLines = 1`, màu `MaterialTheme.appColors.onCategory`, kiểu cỡ chữ và chiều cao dòng lấy từ `labelMedium` nhưng tính bằng dp của ảnh rồi đổi sang sp (`with(density) { (12.dp * chiều cao ảnh / 289.dp).toSp() }`), để chữ trong bong bóng co theo ảnh, không theo cỡ chữ của máy. `onCategory` giống nhau ở hai chế độ vì bong bóng luôn trắng.

Bốn tệp WebP chép nguyên từ `docs/features/onboarding/design/handoff/onboarding_art_<intro|poses|guide|camera>.webp` sang `feature/onboarding/src/main/res/drawable-nodpi/`. `nodpi` để hệ thống không co giãn thêm; ảnh cao 1080 đủ cho khung 360 dp ở mật độ 3x.

**Chữ** (`values*/strings.xml`, 7 thư mục). Tiếng Việt và tiếng Anh theo `design/README.md`, mục 2. Sửa `onboarding_title`, `onboarding_body`, `onboarding_poses_title`, `onboarding_camera_title`; thêm `onboarding_poses_body`, `onboarding_guide_title`, `onboarding_guide_body`, `onboarding_guide_bubble`, `onboarding_camera_body`; xóa `onboarding_point_poses`, `onboarding_point_adjust`, `onboarding_point_saved`, `onboarding_premium_title`, `onboarding_premium_body`. Năm ngôn ngữ còn lại AI dịch trong task (spec, câu 1), giữ nghĩa bảng tiếng Anh. Tiêu đề ngắn nhất có thể; `onboarding_guide_bubble` phải vừa bong bóng (test ở mục 4).

**Ảnh thiết kế cũ.** Bỏ 4 ảnh `premium` trong `design/screens/`, `design/context/`, `design/review/` và 4 ảnh chuẩn cùng tên, như plan T-082 đã bỏ frame `default`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingPage.kt` | `GUIDE`, danh sách cố định |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingViewModel.kt` | Bỏ cờ premium |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreen.kt` | Bố cục chung, bỏ huy hiệu và hàng điểm |
| Sửa | `feature/onboarding/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingIllustration.kt` | Vẽ ảnh, chữ bong bóng |
| Tạo | `feature/onboarding/src/main/res/drawable-nodpi/onboarding_art_intro.webp` | Chép từ `design/handoff/` |
| Tạo | `feature/onboarding/src/main/res/drawable-nodpi/onboarding_art_poses.webp` | Chép từ `design/handoff/` |
| Tạo | `feature/onboarding/src/main/res/drawable-nodpi/onboarding_art_guide.webp` | Chép từ `design/handoff/` |
| Tạo | `feature/onboarding/src/main/res/drawable-nodpi/onboarding_art_camera.webp` | Chép từ `design/handoff/` |
| Xóa | `feature/onboarding/src/main/res/drawable/onboarding_pose_outline.xml` | Không còn dùng |
| Xóa | `feature/onboarding/src/main/res/drawable/ic_onboarding_workspace_premium.xml` | Không còn dùng |
| Xóa | `feature/onboarding/src/main/res/drawable/ic_onboarding_bookmark.xml` | Không còn dùng |
| Xóa | `feature/onboarding/src/main/res/drawable/ic_onboarding_tune.xml` | Không còn dùng |
| Xóa | `feature/onboarding/src/main/res/drawable/ic_onboarding_accessibility_new.xml` | Không còn dùng |
| Sửa | `feature/onboarding/src/main/res/values/strings.xml` | Chữ tiếng Anh |
| Sửa | `feature/onboarding/src/main/res/values-vi/strings.xml` | Chữ tiếng Việt |
| Sửa | `feature/onboarding/src/main/res/values-hi/strings.xml` | Bản dịch |
| Sửa | `feature/onboarding/src/main/res/values-in/strings.xml` | Bản dịch |
| Sửa | `feature/onboarding/src/main/res/values-th/strings.xml` | Bản dịch |
| Sửa | `feature/onboarding/src/main/res/values-pt-rBR/strings.xml` | Bản dịch |
| Sửa | `feature/onboarding/src/main/res/values-b+es+419/strings.xml` | Bản dịch |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingViewModelTest.kt` | Mục 3.5, mục 4 |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreenTest.kt` | Mục 3.5, mục 4 |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreenScreenshotTest.kt` | `premium` thành `guide` |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingStringsTest.kt` | Bảng chữ mới |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingAdContentTest.kt` | `PREMIUM` thành `GUIDE` |
| Sửa | `feature/onboarding/src/deviceScenarios/T-082-luong-gioi-thieu.txt` | `onboarding_page_guide`, `shot guide` |
| Sửa | `feature/onboarding/src/deviceScenarios/T-083-gioi-thieu-cho-trang-1.txt` | `onboarding_page_guide` |
| Sửa | `feature/poses/src/deviceScenarios/T-086-mo-khoa-chi-tiet.txt` | Dòng chờ trang 3 đổi sang `onboarding_page_guide`, sửa chú thích "3 or 4 pages" |
| Xóa | `feature/onboarding/src/test/screenshots/OnboardingScreen_premium_light.png` | Trạng thái đã bỏ |
| Xóa | `feature/onboarding/src/test/screenshots/OnboardingScreen_premium_dark.png` | Trạng thái đã bỏ |
| Xóa | `feature/onboarding/src/test/screenshots/OnboardingScreen_premium-ad_light.png` | Trạng thái đã bỏ |
| Xóa | `feature/onboarding/src/test/screenshots/OnboardingScreen_premium-ad_dark.png` | Trạng thái đã bỏ |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_guide_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_guide_dark.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_guide-ad_light.png` | Ảnh chuẩn mới |
| Tạo | `feature/onboarding/src/test/screenshots/OnboardingScreen_guide-ad_dark.png` | Ảnh chuẩn mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro_light.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro_dark.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro_small.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro_light_unfolded.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad_light.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad_dark.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad_small.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad-wait_light.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_intro-ad-wait_dark.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_poses_light.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_poses_dark.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_poses-ad_light.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_poses-ad_dark.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera_light.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera_dark.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera_small.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera_light_unfolded.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera-ad_light.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera-ad_dark.png` | Bố cục mới |
| Ghi lại | `feature/onboarding/src/test/screenshots/OnboardingScreen_camera-ad_small.png` | Bố cục mới |
| Xóa | `docs/features/onboarding/design/screens/OnboardingScreen_premium_light.png` | Trạng thái đã bỏ |
| Xóa | `docs/features/onboarding/design/screens/OnboardingScreen_premium_dark.png` | Trạng thái đã bỏ |
| Xóa | `docs/features/onboarding/design/screens/OnboardingScreen_premium-ad_light.png` | Trạng thái đã bỏ |
| Xóa | `docs/features/onboarding/design/screens/OnboardingScreen_premium-ad_dark.png` | Trạng thái đã bỏ |
| Xóa | `docs/features/onboarding/design/context/OnboardingScreen_premium_light.md` | Trạng thái đã bỏ |
| Xóa | `docs/features/onboarding/design/context/OnboardingScreen_premium_dark.md` | Trạng thái đã bỏ |
| Xóa | `docs/features/onboarding/design/context/OnboardingScreen_premium-ad_light.md` | Trạng thái đã bỏ |
| Xóa | `docs/features/onboarding/design/context/OnboardingScreen_premium-ad_dark.md` | Trạng thái đã bỏ |
| Ghi lại | `docs/features/onboarding/design/review/` | `scripts/design-compare.sh onboarding` sinh lại, gồm bỏ 4 ảnh so sánh `premium` |
| Sửa | `docs/features/onboarding/design/README.md` | Thêm kết quả đối chiếu của bản này ở mục 8 |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: `:feature:onboarding` thôi đọc `feature_premium_poses_enabled`; cờ giữ nguyên ở `:core:config` và các feature khác. `feature_onboarding_enabled` không đổi.
- Quảng cáo: không đổi vị trí, định dạng, khóa hay pool của `native_onboarding`. Khi cờ premium tắt, luồng từ 3 lên 4 trang nên có thêm một lượt thẻ (spec, câu 10), vì vậy ghi điểm chặn `ads`.
- Sự kiện phân tích: không chạm.
- File R2: không chạm.
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có. WebP đã chạy thử dưới Robolectric (mục 4.1).

### 3.5. Hành vi đã có bị đổi

Từ `scripts/existing-tests.py` (2026-11-09): 6 test ở nhóm "Cần xem trước" và "Nên xem", cộng các test của cờ premium. Mọi câu khẳng định viết lại đều do spec, câu 10 và thiết kế 2026-11-09 nói rõ (ADR-0031).

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Số trang theo cờ premium: 3 hoặc 4, trang 3 là premium | Luôn 4 trang, trang 3 là `guide` | `OnboardingViewModelTest.premiumOff_threePages_withoutPremium`, `premiumOn_fourPages_thirdIsPremium` | Câu mới, thay hai test: `fourPages_inOrder`: `pages` bằng `[INTRO, POSES, GUIDE, CAMERA]` |
| Danh sách trang đổi theo cờ khi ở trang 1, 2; đứng yên từ trang 3 | Không còn theo cờ | `OnboardingViewModelTest.premiumTurnsOn_onFirstOrSecondPage_listFollows`, `premiumChanges_afterThirdPage_listStays` | Bỏ hai test: hành vi không còn tồn tại, spec câu 10 nói rõ. `fourPages_inOrder` giữ ý định "danh sách trang đúng" |
| Khôi phục ở trang premium giữ 4 trang dù cờ tắt | Khôi phục ở trang 3 là `guide` | `OnboardingViewModelTest.restoredOnPremiumPage_keepsFourPages_evenIfFlagOff` | Câu mới: `restoredOnThirdPage_isGuide`: `page_index` là 2 và `viewed_steps` là `intArrayOf(1, 2, 3)` thì `page` là `GUIDE` và `analytics.events` rỗng |
| ViewModel nhận `FeatureFlags` | Không nhận | Hàm dựng trong `OnboardingViewModelTest.viewModel`, lớp giả ở cuối tệp | Bỏ tham số và lớp giả. Các câu khẳng định dùng cờ xem bốn dòng dưới |
| Đi tới trang cuối sau 2 lần "Tiếp" | Sau 3 lần | `OnboardingViewModelTest.toLastPage` (khoảng 12 test gọi) | Đổi dữ liệu (ADR-0029): `repeat(3)`; câu "on the last page" giữ nguyên |
| Sự kiện đến trang cuối là 3 bước | 4 bước | `OnboardingViewModelTest` dòng 70, 85, 128, 143, 158, `permissionGranted_logsResultWithOnboardingSource_andFinishes` (dòng 98, 99) | Đổi dữ liệu (ADR-0029): danh sách thêm `step(4)` trước `COMPLETE`; `assertEquals(6, analytics.events.size)`, `analytics.events[5]` |
| Lần "Tiếp" thứ 3 không đổi trang | Lần thứ 3 sang trang 4, lần thứ 4 không đổi | `OnboardingViewModelTest.next_movesOnePage_untilLast` | Câu mới: thêm một lần `onNextClick()`, `assertEquals(3, index)` trước khi lấy `last`; phần "không đổi trang, không ghi sự kiện" giữ nguyên |
| Lùi từ trang cuối là 2, 1, 0 | Là 3, 2, 1, 0 | `OnboardingViewModelTest.back_movesToPreviousPage_notBeforeFirst` | Câu mới: `assertEquals(3, index)`, lùi một lần `assertEquals(2, index)`, rồi các câu cũ giữ nguyên |
| Khôi phục khi đang hỏi quyền ở trang chỉ số 2 | Trang cuối là chỉ số 3 | `OnboardingViewModelTest.restoredWhileRequesting_acceptsResult_withoutSecondComplete` | Đổi dữ liệu: bỏ `premium_page`, `"page_index" to 3`, `viewed_steps` là `intArrayOf(1, 2, 3, 4)`. Câu đối chứng thành `assertEquals(listOf(step(4)), analytics.events)`; các câu còn lại giữ |
| Khôi phục ở trang cuối chỉ số 2 | Trang cuối là chỉ số 3 | `OnboardingViewModelTest.restoredOnLastPage_withCompletionLogged_tapDoesNotLogAgain` | Đổi dữ liệu: bỏ `premium_page`, `"page_index" to 3`, `viewed_steps` đủ 4; câu đối chứng thành `Browsing(PAGES_4, 3)`; các câu còn lại giữ |
| Log "Premium page on/off" khi cờ đổi | Không còn log này | `OnboardingViewModelTest.logs_pageAndBack` | Bỏ `flags.premium.value = true` và hai câu `Premium page`. Câu mới: `assertFalse(messages.any { it.startsWith("Premium page") })`. Hai câu về `Page shown` và `Back to page 1` giữ nguyên |
| Log ở trang thứ 3 là camera | Trang 3 là guide, trang 4 là camera | `OnboardingViewModelTest.logs_useOnboardingTag` | Đổi dữ liệu: thay `"Page shown: 3 camera"` bằng `"Page shown: 3 guide"`, `"Page shown: 4 camera"` |
| Bước ghi 1 tới 3, đối chứng 4 trang bằng cờ | Luôn 1 tới 4 | `OnboardingViewModelTest.stepViewed_loggedOncePerStep` | Câu mới: sau đi tới, lùi về, đi tới lại thì `analytics.events` đúng bằng `[step(1), step(2), step(3), step(4)]`. Bỏ nhánh `flags`; phần đi lại trong cùng test giữ vai đối chứng "không ghi lần hai" |
| Số chạm theo hai giá trị cờ | Một luồng 4 trang | `OnboardingViewModelTest.taps_equalPagesPlusOne` | Bỏ vòng lặp cờ. Câu mới: `assertEquals(4, state.pages.size)`, `assertEquals(state.pages.size + 1, taps)`; câu `Finished` giữ |
| Danh sách `PAGES_3` | Luồng thật dùng `PAGES_4`, trang cuối là chỉ số 3 | Các test dùng `PAGES_3` trong `OnboardingViewModelTest` và `OnboardingScreenTest` (dòng 610, 654 và các test `premium*`) | Đổi dữ liệu (ADR-0029); câu khẳng định giữ. Riêng `pageIndicator_describesCurrentPage` (dòng 296) và `pageIndicator_layoutAndColors` (dòng 325) kiểm số chấm theo số trang: giữ một danh sách 3 phần tử cục bộ `THREE_PAGES`, câu khẳng định giữ nguyên, vì màn vẫn nhận danh sách bất kỳ |
| Phần tử cuối của trang theo từng trang, gồm hàng điểm | Mọi trang là lời giới thiệu | `OnboardingScreenTest.LAST_ELEMENT`, `small_withCard_fontScale2_buttonAndNoteInWindow`, `assertShortWithCard`, `assertShortScreen`, `assertLandscape` | `LAST_ELEMENT` đổi thành `BODY` cho cả 4 trang, khóa `PREMIUM` thành `GUIDE`; hai dòng dùng `FEATURE_ADJUST` đổi sang `BODY`. Câu "cuộn tới được phần tử cuối" giữ |
| Trang 2, 3, cuối có huy hiệu; trang 2, cuối có hàng điểm; trang 3 có huy hiệu premium | Mọi trang là ảnh, tiêu đề, lời | `OnboardingScreenTest.poses_layoutMatchesDesign`, `premium_layoutMatchesDesign`, `camera_layoutMatchesDesign`, `posesAd_layoutMatchesDesign`, `premiumAd_layoutMatchesDesign` | Câu mới theo số đo ở mục 4, giữ ý định "đúng số đo của `context/`"; `premium*` đổi tên thành `guide*`. `intro*` đổi số đo; `cameraAd_layoutMatchesDesign` thêm câu về ảnh, tiêu đề, lời theo mục 4 |
| Ảnh minh họa cao 280, nhiều màu do thành phần vẽ | Ảnh WebP cao tới 360 | `OnboardingScreenTest.intro_layoutMatchesDesign`, `unfolded_layoutMatchesDesign`, `intro_illustrationColors_light`, `assertIllustrationRule` | Số đo theo mục 4. `intro_illustrationColors_light` thay bằng `eachPage_drawsItsArt`, giữ ý định "hình có vẽ ra"; câu `assertColor("background", 0xFFFFFDF8, ...)` chuyển nguyên sang test mới. `assertIllustrationRule` viết lại với trần 360: `coerceIn(160.dp, 360.dp)`, `height <= 360.dp`, `shrinks` là `height < 360.dp`. `introAd_layoutMatchesDesign` đổi đối chứng `< 280.dp` thành `< 360.dp`. Bỏ điều kiện `if (state.page == INTRO)` để luật co kiểm trên mọi trang |
| Màu huy hiệu và vòng icon | Không còn phần tử này | `OnboardingScreenTest.poses_iconCircleColor_light`, `premium_badgeColor_light`, `camera_colors_dark` | Bỏ hai test đầu vì phần tử không còn. `camera_colors_dark` giữ nguyên: test này không có câu nào về huy hiệu |
| Cờ tắt: 3 trang, không có chữ premium | Không trang nào có chữ premium | `OnboardingScreenTest.threePages_noPremiumText_anywhere` | Câu mới: `noPage_mentionsPremium`: trên cả 4 trang, `onAllNodes(hasText("premium", substring = true, ignoreCase = true)).assertCountEquals(0)` và `pageDot(4)` tồn tại. Đối chứng: `OnboardingStringsTest` khẳng định không chuỗi nào của `ENGLISH`, `VIETNAMESE` chứa "premium", và tạm đặt một chuỗi có chữ này thì test đỏ |
| Chữ mỗi trang theo bảng cũ | Theo bảng mới | `OnboardingScreenTest.eachPage_showsItsTexts`, `OnboardingStringsTest` | Đổi dữ liệu theo README, mục 2 (ADR-0029) |
| Danh sách trang của test có `PREMIUM` | `GUIDE` | `OnboardingAdContentTest.PAGES_4`, các test `OnboardingScreenTest` dùng `PAGES`, `OnboardingScreenScreenshotTest.premium*` | Đổi dữ liệu và tên ảnh (ADR-0029); câu khẳng định giữ nguyên |

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-5, luôn 4 trang theo mọi giá trị của cờ | Unit test | `OnboardingViewModelTest.fourPages_inOrder`, không nhận `FeatureFlags` |
| KB-1, số chạm | Unit test | `OnboardingViewModelTest.taps_equalPagesPlusOne` (đổi dữ liệu: 4 trang) |
| KB-5, `step` từ 1 tới 4 | Unit test | `OnboardingViewModelTest.stepViewed_loggedOncePerStep` |
| KB-5, chữ mỗi trang, không có chữ "premium" | Compose UI test | `OnboardingScreenTest.eachPage_showsItsTexts`, `noPage_mentionsPremium` |
| Bố cục không thẻ: ảnh y 88, cao 360; tiêu đề y 472; lời y 532; đỉnh nút 811 (mọi trang trừ ghi chú trang cuối) | Compose UI test | `OnboardingScreenTest.<intro\|poses\|guide\|camera>_layoutMatchesDesign`, sai số 4 dp |
| Bố cục có thẻ: trang 1 tới 3 ảnh cao 293, tiêu đề y 405, lời y 465, đỉnh thẻ 537; trang cuối ảnh cao 237, tiêu đề y 349, lời y 409, đỉnh thẻ 481 | Compose UI test | `OnboardingScreenTest.<intro\|poses\|guide\|camera>Ad_layoutMatchesDesign`, sai số 4 dp. Số cao hơn Figma 4 dp vì nút 56 thay 60 |
| Khi mở gập: ảnh cao 360, căn giữa | Compose UI test | `OnboardingScreenTest.unfolded_layoutMatchesDesign` |
| Ảnh có vẽ ra ở mọi trang | Compose UI test | `OnboardingScreenTest.eachPage_drawsItsArt`: vẽ `decorView` ra `Bitmap`, đếm điểm màu cam trong khung ảnh của từng trang |
| Chữ bong bóng đúng màu, vừa bong bóng ở mọi ngôn ngữ | Compose UI test | `OnboardingScreenTest.guideBubble_text_dark` đo màu chữ tối ở chế độ tối; `guideBubble_fits_<ngôn ngữ>` cho 7 ngôn ngữ: rộng không quá 41% rộng ảnh; `guideBubble_fits_fontScale2`: cỡ chữ hệ thống 2,0 không làm chữ rộng hơn |
| KB-1, màn thấp, nút luôn thấy, cuộn được ở 7 ngôn ngữ; ảnh co trước | Compose UI test | `OnboardingScreenTest.small_everyPage_buttonVisible_contentScrolls_<ngôn ngữ>`, `small_withCard_buttonInWindow_pageScrolls` |
| KB-6, thẻ, chờ 5 giây, không che nút | Compose UI test | Giữ nguyên `IntroAdWaitTest`, `OnboardingAdContentTest`, `adSlot_*`, `nextButton_touchArea_notOverCard_*` |
| Chữ đúng bảng chữ của thiết kế, đủ 7 ngôn ngữ | Compose UI test | `OnboardingStringsTest` (bảng mới) |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` | `OnboardingScreen_intro_small.png`, `OnboardingScreen_camera_small.png`, `OnboardingScreen_intro-ad_small.png`, `OnboardingScreen_camera-ad_small.png` |
| Đúng thiết kế | Screenshot test cùng tên với từng ảnh thiết kế, ảnh so sánh `scripts/design-compare.sh onboarding`, rà soát độc lập theo mục 7 của mẫu thiết kế | `OnboardingScreen_<trạng thái>_<light\|dark>.png`, 20 ảnh của mục 2 |
| KB-5 trên máy, qua đủ 4 trang, xoay, mở gập, quay lại | Kiểm tra thủ công (emulator: `feature/onboarding/src/deviceScenarios/T-082-luong-gioi-thieu.txt`) | Tệp kịch bản; `device-session` cuối phiên chạy |
| KB-6 trên máy, trang 1 chờ quảng cáo | Kiểm tra thủ công (emulator: `feature/onboarding/src/deviceScenarios/T-083-gioi-thieu-cho-trang-1.txt`) | Tệp kịch bản |
| Ảnh thiết kế của ticket khác | Ảnh thiết kế của ticket khác (T-092): `OnboardingScreen_guide-ad-wait_light`, `OnboardingScreen_guide-ad-wait_dark` | Vẽ ở T-092 |
| Luồng dẫn tới màn tư thế premium vẫn qua được | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-086-mo-khoa-chi-tiet.txt`) | Tệp kịch bản |
| Trang 4 có thẻ khi bấm "Tiếp" với nhịp thường | Kiểm tra thủ công (máy thật: quảng cáo thật) | Trên bản staging, cài mới, đi qua 4 trang với nhịp đọc bình thường, xem trang 4 có thẻ không |
| Bản dịch của 5 ngôn ngữ | Kiểm tra thủ công (người: một người biết tiếng duyệt, spec, câu 1) | Đọc `values-*/strings.xml` |
| Đối tượng của app (spec, câu 11) | Kiểm tra thủ công (người: khai báo trên Play Console, trước khi phát hành) | Kiểm khai báo đối tượng mục tiêu và nội dung |

Ghi ảnh chuẩn: `./gradlew :feature:onboarding:recordRoborazziDebug :feature:onboarding:testDebugUnitTest --rerun`, rồi `git status --short -- '*/src/test/screenshots/*'` chỉ được đổi các ảnh của mục 3.2.

**Ngoại lệ viết test trước:** tệp kịch bản emulator (test chạy ở `device-session` cuối phiên); chép 4 tệp WebP (tài nguyên). Xóa 4 ảnh chuẩn `premium` đi cùng commit đổi code.

### 4.1. Chạy thử

- **Đã chạy thử:** ảnh WebP trong `res/drawable-nodpi` vẽ bằng `painterResource` dưới Robolectric `NATIVE` (lần đầu dự án có ảnh bitmap trong `res/`). Test tạm `TmpWebpArtTest`, không commit, trong worktree tạm từ `c697a41`, ngày 2026-11-09: vẽ `onboarding_art_poses.webp` vào khung 360 dp, vẽ `decorView` ra `Bitmap`, đếm điểm màu cam của áo.
- **Lệnh:** `scripts/check.sh`
- **Kết quả:** cổng chất lượng QUA, 2870 test JVM, 0 lỗi; test tạm qua, bitmap 1233 x 2673, 12176 điểm màu cam trên lưới lấy mẫu mỗi 4 điểm ảnh, tức ảnh giải mã và vẽ đúng. Không cần phương án dự phòng ở mục 3.4.
- **Đo thêm số dòng của tiêu đề** (test tạm `TmpTitleLinesTest`, không commit, `./gradlew :feature:onboarding:testDebugUnitTest`, Robolectric `NATIVE`, 2026-11-09): ở rộng 363 cả 4 tiêu đề tiếng Việt và 4 tiếng Anh đều một dòng, nên số đo ở mục 4 đúng. "Dáng đẹp, không cần đoán" rộng 362,8 trên 363, vừa khít: máy thật hay ngôn ngữ khác có thể xuống dòng, khi đó ảnh co thêm 36 theo `ShrinkFirstLayout`, không vỡ bố cục. Ở rộng 312 (khung 360) ba tiêu đề tiếng Việt và ba tiếng Anh xuống hai dòng; các test `small_*` kiểm theo luật co, không theo số cứng.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 1, quảng cáo: khi cờ premium tắt, luồng có thêm một lượt thẻ native.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Nhiều phần trong một module, đổi hàm dựng của ViewModel |
| B. Độ rõ của yêu cầu | 0 | Spec đã xác nhận, thiết kế đã duyệt, có số đo |
| C. Khả năng kiểm chứng tự động | 1 | Phần lớn có test; trang 4 có quảng cáo thật hay không phải kiểm trên máy |
| D. Ảnh hưởng chéo | 2 | T-092 cùng sửa `OnboardingScreen.kt`, `OnboardingViewModel.kt`, test và kịch bản; thêm một dòng trong kịch bản của `:feature:poses` |
| E. Độ mới của kỹ thuật | 2 | Ảnh bitmap đầu tiên trong `res/` của dự án |
| **Tổng** | 7 | **Mức: Cao (chạm điểm chặn)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: không sửa module core. Sửa một dòng trong kịch bản `feature/poses/src/deviceScenarios/T-086-mo-khoa-chi-tiet.txt`.
- Task khác trong sprint có thể xung đột: T-090 chạm `:core:ads`, `MainActivity`, không chạm `:feature:onboarding`; không xung đột.
- Đề xuất thứ tự chạy: chạy trước T-092, vì T-092 dùng trang `GUIDE` và các file mà ticket này tạo.

## 7. Điều kiện dừng riêng

- `git status --short -- '*/src/test/screenshots/*'` có ảnh của feature khác thì dừng.
- Chữ bong bóng của một ngôn ngữ không thể ngắn đủ để qua `guideBubble_fits_*` mà vẫn đúng nghĩa thì dừng, hỏi người.

## 8. Câu hỏi cho người duyệt

1. **Ai đọc bản dịch của 5 ngôn ngữ?** Đề xuất của AI: AI dịch trong task, một người biết tiếng duyệt trước khi phát hành, như spec, câu 1. Trả lời: Nhật phụ trách duyệt bản dịch (Nhật, 2026-11-09).
2. Đã trả lời: trang 3 giữ nút "Tiếp" tối đa 3 giây (Nhật, 2026-11-09; spec, câu 12). Phần này làm ở T-092, chạy sau ticket này.

3. **Plan bỏ hẳn 4 test vì hành vi hay phần tử chúng kiểm không còn tồn tại:** `premiumTurnsOn_onFirstOrSecondPage_listFollows`, `premiumChanges_afterThirdPage_listStays`, `poses_iconCircleColor_light`, `premium_badgeColor_light`. **Và đổi tên hay thay thế 11 test** (mục 3.5): `premiumOff_threePages_withoutPremium`, `premiumOn_fourPages_thirdIsPremium` thành `fourPages_inOrder`; `restoredOnPremiumPage_keepsFourPages_evenIfFlagOff` thành `restoredOnThirdPage_isGuide`; `premium_layoutMatchesDesign`, `premiumAd_layoutMatchesDesign` thành `guide*`; `intro_illustrationColors_light` thành `eachPage_drawsItsArt`; `threePages_noPremiumText_anywhere` thành `noPage_mentionsPremium`; screenshot `premium_light`, `premium_dark`, `premiumAd_light`, `premiumAd_dark` thành `guide*`. ADR-0029 không cho phiên tự xóa test, và bước 6 coi hàm `@Test` không còn là test bị xóa, nên người duyệt cần đồng ý rõ ở đây. Đề xuất của AI: đồng ý, vì spec câu 10 bỏ hành vi và thiết kế bỏ phần tử.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
