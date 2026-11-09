---
feature: onboarding
status: approved                # draft | approved
spec: features/onboarding/spec.md
tool: figma
link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=95-2761
designer: AI (Claude), Nhật
approved_by: Nhật
approved_at: 2026-11-09
---

# THIẾT KẾ: MÀN CHÀO VÀ LUỒNG GIỚI THIỆU

Màu, chữ, thành phần chung: [docs/design-system/sticker.md](../../../design-system/sticker.md).

Link Figma:

- Luồng giới thiệu 4 trang có ảnh minh họa (yêu cầu 2026-11-09): section "Onboarding flow v2", https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=95-2761. Xuất ngày 2026-11-09.
- Luồng giới thiệu 4 trang có trang premium (bản cũ, T-082, T-083): section "Onboarding flow", https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=66-2284.
- Màn chào một trang (bản cũ, T-012): section "Onboarding", https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=18-128.

## 1. Các màn hình và trạng thái

Luồng luôn có 4 trang, không phụ thuộc cờ `feature_premium_poses_enabled` (spec, câu 10).

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Ticket | Kịch bản |
| --- | --- | --- | --- | --- |
| `OnboardingScreen` | Trang 1, giới thiệu | `OnboardingScreen_intro_light`, `OnboardingScreen_intro_dark`, `OnboardingScreen_intro_light_unfolded` | T-091 | KB-1, KB-5 |
| `OnboardingScreen` | Trang 2, tư thế | `OnboardingScreen_poses_light`, `OnboardingScreen_poses_dark` | T-091 | KB-5 |
| `OnboardingScreen` | Trang 3, cách dùng nét viền | `OnboardingScreen_guide_light`, `OnboardingScreen_guide_dark` | T-091 | KB-5 |
| `OnboardingScreen` | Trang cuối, bật camera | `OnboardingScreen_camera_light`, `OnboardingScreen_camera_dark`, `OnboardingScreen_camera_light_unfolded` | T-091 | KB-1, KB-2, KB-5 |
| `OnboardingScreen` | Trang 1 có thẻ native | `OnboardingScreen_intro-ad_light`, `OnboardingScreen_intro-ad_dark` | T-091 | KB-6 |
| `OnboardingScreen` | Trang 2 có thẻ native | `OnboardingScreen_poses-ad_light`, `OnboardingScreen_poses-ad_dark` | T-091 | KB-6 |
| `OnboardingScreen` | Trang 3 có thẻ native | `OnboardingScreen_guide-ad_light`, `OnboardingScreen_guide-ad_dark` | T-091 | KB-6 |
| `OnboardingScreen` | Trang cuối có thẻ native | `OnboardingScreen_camera-ad_light`, `OnboardingScreen_camera-ad_dark` | T-091 | KB-6 |
| `OnboardingScreen` | Trang 1 chờ quảng cáo: thẻ là khung chờ, nút "Tiếp" mờ | `OnboardingScreen_intro-ad-wait_light`, `OnboardingScreen_intro-ad-wait_dark` | T-091 | KB-6 (câu 8) |
| `OnboardingScreen` | Trang 3 chờ quảng cáo cho trang 4: thẻ của trang 3 vẫn hiện, nút "Tiếp" mờ | `OnboardingScreen_guide-ad-wait_light`, `OnboardingScreen_guide-ad-wait_dark` | T-092 | KB-6 (câu 12) |

Tên trạng thái `intro`, `poses`, `guide`, `camera`. Trạng thái `premium` của bản cũ thay bằng `guide`; ảnh `premium` trong `screens/` và ảnh chuẩn cùng tên được bỏ trong plan của ticket làm bản này (như plan T-082, mục 3.2).

## 2. Bảng chữ

Chữ mới chốt ngày 2026-11-09 (spec, câu 10). Tiêu đề một dòng, lời hai dòng ở khung 411, để ảnh minh họa lớn khi có thẻ.

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| Trang 1 `intro` | Tiêu đề | Dáng đẹp, không cần đoán | `headlineMedium` |
| Trang 1 `intro` | Lời | Chưa biết tạo dáng thế nào? App gợi ý dáng và chỉ bạn đứng ra sao. | `bodyLarge` |
| Trang 2 `poses` | Tiêu đề | Hơn 200 tư thế có sẵn | `headlineMedium` |
| Trang 2 `poses` | Lời | Một người, cặp đôi hay nhóm bạn đều có. Lưu dáng yêu thích để chụp lại sau. | `bodyLarge` |
| Trang 3 `guide` | Tiêu đề | Khớp nét viền, bấm chụp | `headlineMedium` |
| Trang 3 `guide` | Lời | Chỉnh độ mờ, cỡ và vị trí nét viền cho vừa khung hình. Ảnh lưu ra không kèm nét viền. | `bodyLarge` |
| Trang 3 `guide` | Chữ trong bong bóng của ảnh | Đứng vào đây! | `labelMedium`, co giãn theo ảnh |
| Trang cuối `camera` | Tiêu đề | Sẵn sàng chụp rồi! | `headlineMedium` |
| Trang cuối `camera` | Lời | Chọn một dáng, đứng vào nét viền và chụp tấm ảnh đầu tiên của bạn. | `bodyLarge` |
| Trang cuối `camera` | Ghi chú | App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn. | `bodySmall` |
| Trang cuối `camera` | Nút | Bật camera | chữ của nút chính (`titleMedium`) |
| Các trang trước trang cuối | Nút (mới) | Tiếp | chữ của nút chính (`titleMedium`) |
| Mọi trang | Mô tả hàng chấm cho trình đọc màn hình (mới, không hiện trên ảnh) | Trang %1$d trên %2$d | |
| Có thẻ native | Nhãn, chữ trong thẻ | Nhãn "Ad" và chữ của thẻ thuộc `:core:ads` và của nhà quảng cáo, không phải chữ của app; "Headline of the ad", "Advertiser or short body", "Install" là chữ giả của ảnh chuẩn `NativeAdCard_step_*` | theo khung |

Tiếng Anh của chữ mới (ngôn ngữ gốc của `strings.xml`): `Great poses, no guessing`, `Not sure how to pose? The app suggests a pose and shows you where to stand.`, `200+ poses ready to go`, `Solo, couples or friends, it's all here. Save your favorites to shoot again later.`, `Match the outline, shoot`, `Adjust the outline's opacity, size and position to fit the frame. Saved photos don't include the outline.`, `Stand here!`, `Ready to shoot!`, `Pick a pose, step into the outline and take your first photo.`. Giữ nguyên: `Next`, `Page %1$d of %2$d`, nút và ghi chú của trang cuối.

## 3. Ánh xạ sang hệ thống thiết kế

Bố cục từ trên xuống, giống nhau trên mọi trang: một cột lấp màn, nền `background`, lề trái, phải và dưới `Spacing.lg`, lề trên 56 (`Spacing.xl + Spacing.lg`, như màn chào cũ).

| Phần tử trên thiết kế | Thành phần trong code | Màu (vai trò) | Kiểu chữ | Khoảng cách, kích thước |
| --- | --- | --- | --- | --- |
| Hàng chấm (cố định, không cuộn) | Hàng chấm của `:feature:onboarding` (component Figma `PageDot`) | Chấm của trang hiện tại `secondary`, chấm khác `outlineVariant` | | Chấm hiện tại 24 x 8 (`Spacing.lg` x `Spacing.sm`), chấm khác 8 x 8, bo tròn; cách nhau `Spacing.sm` |
| Vùng trang (cuộn, `weight(1f)`) | `Column` cuộn dọc | | | Lề trên `Spacing.lg` và lề dưới `Spacing.lg` nằm **trong** phần cuộn; các khối cách nhau `Spacing.lg` |
| Ảnh minh họa (mọi trang) | `Image` của 4 tệp WebP trong `res/` (nguồn ở `design/handoff/`), không có mô tả cho trình đọc màn hình | Ảnh bitmap, không theo chế độ tối | | Rộng hết, căn giữa, giữ tỉ lệ. Không có thẻ: khung cao 360. Có thẻ: lấp chỗ còn lại, 289 ở trang 1 tới 3, 233 ở trang cuối. Co trước khi vùng trang thiếu chỗ (như `ShrinkFirstLayout`), thấp nhất 160, rồi vùng trang cuộn |
| Bong bóng (trang 3) | `Text` đặt đè lên ảnh `guide`, theo tỉ lệ ảnh: tâm ở x 76,8% và y 15,2% của ảnh, rộng tối đa 41% | `category/onCategory` (bong bóng luôn trắng) | `labelMedium` ở ảnh cao 289, co giãn theo ảnh | Một dòng, căn giữa |
| Tiêu đề, lời | `Text` | `onBackground`, `onSurfaceVariant` | `headlineMedium`, `bodyLarge` | Rộng hết, căn trái |
| Khe thẻ native (chỉ frame `-ad`) | `NativeStepAd` của `:core:ads` (component Figma `Native/Step`, `State=Ad`; khung chờ `State=Loading`) | Theo khung: nền `surfaceContainerLow`, viền `outlineVariant` 1, media `surfaceContainer`, icon `secondaryContainer`, nhãn "Ad" `tertiary`, CTA `tertiaryContainer`; các khối của khung chờ `surfaceContainerHighest` | Theo khung | Rộng hết, cao 250 (`NativeStepAdDefaults.height()`), **không có lề ngoài**: khoảng cách phía trên thẻ là lề dưới `Spacing.lg` của vùng trang |
| Ghi chú quyền (chỉ trang cuối) | `Text`, căn giữa | `onSurfaceVariant` | `bodySmall` | Dưới khe thẻ, trên nút; cách phía trên `Spacing.lg` |
| Nút chính (ghim đáy) | `PrimaryButton`: "Tiếp" không icon; "Bật camera" icon `photo_camera` | `secondary`, `onSecondary`, bóng `shadowSticker` | `titleMedium` | Rộng hết, cách phía trên `Spacing.lg`, đáy cách đáy màn `Spacing.lg`. Đỉnh nút ở y 807 trên mọi frame 411 x 891 (y 757 ở frame `_unfolded`), có hay không có thẻ |
| Nút "Tiếp" mờ (frame `intro-ad-wait`, `guide-ad-wait`) | `PrimaryButton(enabled = false)` (component Figma `Button`, `Variant=Primary, State=Disabled`) | Như `PrimaryButton` tắt trong code: nền `onSurface` mờ 0.12, chữ `onSurface` mờ 0.38, viền `outlineVariant`, không bóng | `titleMedium` | Cùng chỗ, cùng cỡ với nút thường |

Giá trị `context/` đánh dấu "ngoài hệ thống":

- Khoảng cách 56: lề trên của màn, `Spacing.xl + Spacing.lg` như màn chào cũ.
- Khoảng cách 12 và 2, bo 12, chữ 13: số của khung `:core:ads` (`NativeAdDimens.InnerPadding`, `Step*`, khung chờ), không phải số của app.
- Độ mờ của nút tắt nằm ở lớp (`độ mờ của lớp 0.12` cho khối nền "container", `0.38` cho chữ), không ở lớp tô, vì Figma bỏ độ mờ của lớp tô gắn biến trên instance (L-023). Trong code là màu tắt của `PrimaryButton`, không đổi.
- Nút trên Figma cao 60, trong code 56 (T-007 đã chốt 56): giữ 56.

- **Thành phần mới cần thêm vào hệ thống thiết kế:** không có trong `:core:designsystem`. Bản này không dùng `IconBadge` và `FeatureRow`; `PageDot`, `Native/Step`, `Button` giữ như bản trước. Component Figma mới `OnboardingArt/intro`, `poses`, `guide`, `camera` chỉ chứa ảnh, riêng của feature. Bộ thiết kế quảng cáo của dự án mẫu không gắn vào file Figma của app, nên `Native/Step` được vẽ lại theo số của `NativeAdDimens.Step*` và ảnh chuẩn `NativeAdCard_step_*`, `NativeAdPlaceholder_step_light` (Nhật chọn ngày 2026-10-31).

## 4. Luồng thao tác

Màn loading xong ở lần mở đầu: trang 1 `intro`; bấm "Tiếp" sang trang 2 `poses`, trang 3 `guide`, rồi trang cuối `camera`; bấm "Bật camera", hộp hỏi quyền của hệ thống, sang `CameraScreen` (cho phép hay từ chối đều sang). Nút quay lại của máy ở trang 2 trở đi về trang trước; ở trang 1 đóng app. Không có nút "Bỏ qua". Trang đổi ngay, không hiệu ứng trượt.

## 5. Vị trí quảng cáo

`native_onboarding`, định dạng `native_step` (spec mục 5): thẻ 250 dp ở mọi trang, kể cả trang cuối, giữa vùng trang và ghi chú hay nút. Thẻ có ranh giới (viền `outlineVariant`), nhãn "Ad" luôn thấy, CTA màu hồng khác nút chính đen của app; thẻ cách nút chính và ghi chú `Spacing.lg` (ADR-0022 cần ít nhất 16). Trang 1 chờ quảng cáo tối đa 5 giây: thẻ hiện khung chờ cùng chiều cao, nút "Tiếp" mờ (`intro-ad-wait`). Trang 3 chờ quảng cáo cho trang 4 tối đa 3 giây: thẻ của trang 3 giữ nguyên, nút "Tiếp" mờ (`guide-ad-wait`). Vị trí tắt hay không có quảng cáo thì không có khe, không có khoảng trống (các frame không có `-ad`).

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục, thứ tự thành phần, chữ (mục 2), vai trò màu và kiểu chữ (mục 3); nút cùng một chỗ trên mọi trang.
- **Được phép khác:** cách vẽ chữ và bóng của hệ thống, chênh lệch nhỏ theo lưới 4 dp, nút cao 56 thay vì 60, chữ trong thẻ quảng cáo (của nhà quảng cáo).
- **Phần ghim và phần cuộn:** hàng chấm ghim trên cùng; vùng trang cuộn và co theo chỗ còn lại; khe thẻ, ghi chú quyền (trang cuối) và nút ghim ở đáy, không cuộn. Thẻ không bao giờ che ghi chú hay nút. Ở mọi trang, ảnh minh họa co trước (`ShrinkFirstLayout`), sau đó vùng trang cuộn.
- **Màn hình thấp (360 x 640), tính từ số đo của bản trước** (chưa dựng frame; screenshot test `_small` kiểm; giả định tiêu đề một dòng, lời hai dòng; ở rộng 312 các tiêu đề dài xuống hai dòng, khi đó nội dung cần thêm 36 và ảnh ở dòng không thẻ thấp đi 36):

| Trang ở 360 x 640 | Vùng trang (cuộn) | Trừ lề trong 24 trên, 24 dưới | Nội dung cần khi ảnh thấp nhất 160 (không kể lề) | Kết quả |
| --- | --- | --- | --- | --- |
| Trang 1 tới 3, có thẻ | 218 | 170 | 292 | Ảnh 160, vùng trang cuộn |
| Trang cuối `camera`, có thẻ | 162 | 114 | 292 | Ảnh 160, vùng trang cuộn |
| Trang 1 tới 3, không thẻ | 468 | 420 | 292 | Ảnh co còn khoảng 288, không cuộn |
| Trang cuối `camera`, không thẻ | 412 | 364 | 292 | Ảnh co còn khoảng 232, không cuộn |

  Không ẩn thẻ, không giảm lề ở khung thấp (plan T-083, mục 8, câu 7): vùng trang còn ít nhưng cuộn được, nút và ghi chú luôn thấy.
- **Khi mở gập (673 x 841):** cùng bố cục một cột, rộng hết; khung ảnh cao 360, ảnh căn giữa.

## 7. Tiêu chí khi duyệt

- [ ] Dùng đúng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế.
- [ ] Đủ các trạng thái trong mục 1, kể cả chế độ tối.
- [ ] Chữ đúng bảng chữ, không bị cắt hay tràn.
- [ ] Bố cục và thứ tự thành phần giống bản mẫu; lệch về vị trí không quá một bước lưới 4 dp.
- [ ] Nút chính cùng một chỗ trên mọi trang, có hay không có thẻ.
- [ ] Vùng chạm đủ lớn (ít nhất 48 dp), chữ dễ đọc.
- [ ] Dùng được ở màn hình thấp (360 x 640): nội dung cuộn được, chữ không chồng nhau, nút chính và ghi chú không bị che hay cắt.
- [ ] Vùng quảng cáo tách bạch, không gây bấm nhầm; nhãn "Ad" thấy rõ.
- [ ] Giao diện và nội dung không được thiết kế để thu hút chủ yếu trẻ em.

## 8. Kết quả đối chiếu

Đối chiếu bản 4 trang có ảnh (T-091) bằng `scripts/design-compare.sh onboarding` ngày 2026-11-09 (RUN-20261109-01, macOS): ảnh chuẩn chụp bằng tiếng Việt, luồng 4 trang, frame `-ad` dùng thẻ giả `NativeAdCard(NativeAdSize.STEP)` như T-083. Hai frame `guide-ad-wait` còn "thiếu ảnh chuẩn screenshot" tới T-092. Bốn frame `premium` đã bỏ cùng ảnh chuẩn cùng tên (plan T-091, mục 3.2).

| Màn hình | Điểm ảnh khác | Ảnh so sánh (thiết kế, thật, khác biệt) |
| --- | --- | --- |
| OnboardingScreen_intro_light | 2.3% | `OnboardingScreen_intro_light_compare.png` |
| OnboardingScreen_intro_dark | 2.3% | `OnboardingScreen_intro_dark_compare.png` |
| OnboardingScreen_intro_light_unfolded | 1.5% | `OnboardingScreen_intro_light_unfolded_compare.png` |
| OnboardingScreen_poses_light | 1.6% | `OnboardingScreen_poses_light_compare.png` |
| OnboardingScreen_poses_dark | 1.6% | `OnboardingScreen_poses_dark_compare.png` |
| OnboardingScreen_guide_light | 2.2% | `OnboardingScreen_guide_light_compare.png` |
| OnboardingScreen_guide_dark | 2.1% | `OnboardingScreen_guide_dark_compare.png` |
| OnboardingScreen_camera_light | 2.3% | `OnboardingScreen_camera_light_compare.png` |
| OnboardingScreen_camera_dark | 2.3% | `OnboardingScreen_camera_dark_compare.png` |
| OnboardingScreen_camera_light_unfolded | 1.7% | `OnboardingScreen_camera_light_unfolded_compare.png` |
| OnboardingScreen_intro-ad_light | 6.8% | `OnboardingScreen_intro-ad_light_compare.png` |
| OnboardingScreen_intro-ad_dark | 7.8% | `OnboardingScreen_intro-ad_dark_compare.png` |
| OnboardingScreen_poses-ad_light | 5.9% | `OnboardingScreen_poses-ad_light_compare.png` |
| OnboardingScreen_poses-ad_dark | 6.8% | `OnboardingScreen_poses-ad_dark_compare.png` |
| OnboardingScreen_guide-ad_light | 6.3% | `OnboardingScreen_guide-ad_light_compare.png` |
| OnboardingScreen_guide-ad_dark | 7.0% | `OnboardingScreen_guide-ad_dark_compare.png` |
| OnboardingScreen_camera-ad_light | 5.2% | `OnboardingScreen_camera-ad_light_compare.png` |
| OnboardingScreen_camera-ad_dark | 6.0% | `OnboardingScreen_camera-ad_dark_compare.png` |
| OnboardingScreen_intro-ad-wait_light | 5.6% | `OnboardingScreen_intro-ad-wait_light_compare.png` |
| OnboardingScreen_intro-ad-wait_dark | 5.9% | `OnboardingScreen_intro-ad-wait_dark_compare.png` |

Nhận xét của phiên viết code (T-091; rà soát độc lập bổ sung ở bước 6):

- Mọi trang: ảnh, tiêu đề, lời đúng thứ tự, chữ đúng bảng chữ (mục 2); trên macOS cả 4 tiêu đề tiếng Việt vừa một dòng ở rộng 363. Không thẻ: ảnh y 88 cao 360, tiêu đề 472, lời 532, nút 811 (test `*_layoutMatchesDesign`). Mở gập: ảnh cao 360, căn giữa trong khung 625.
- Có thẻ: thẻ ở 537 (trang 1 tới 3) và 481 (trang cuối), thấp hơn thiết kế 4 dp vì nút 56 thay 60 (mục 6), nên ảnh cao 293 thay 289 (237 thay 233 ở trang cuối), tiêu đề và lời dời theo. Phần khác của các frame `-ad` là ảnh co giãn lệch 4 dp ấy, dải 4 dp của thẻ và nút, và chữ giả "Advertiser" của thẻ thay "Advertiser or short body" của Figma (chữ của nhà quảng cáo, mục 2).
- Bong bóng trang 3: chữ `onCategory` đè lên ảnh, tâm ở 76,8% và 15,2% của ảnh, cỡ chữ theo ảnh (15 ở ảnh 360, 12 ở ảnh 289), không theo cỡ chữ của máy; vừa 41% rộng ảnh ở 7 ngôn ngữ (test `guideBubble_fits_*`).

Các bảng dưới là kết quả của bản trước (T-082, T-083), giữ để tham khảo.

Đối chiếu bằng `scripts/design-compare.sh onboarding` ngày 2026-11-01 (T-082, RUN-20261031-02), ảnh chuẩn chụp bằng tiếng Việt, trạng thái `Browsing` của luồng 4 trang, khe quảng cáo rỗng. Hai frame `default` của màn chào một trang đã bỏ cùng ảnh chuẩn cùng tên (plan T-082, mục 3.2). 10 frame `-ad`, `-ad-wait` còn "thiếu ảnh chuẩn screenshot" tới T-083 (`review/summary.md`).

| Màn hình | Điểm ảnh khác | Ảnh so sánh (thiết kế, thật, khác biệt) |
| --- | --- | --- |
| OnboardingScreen_intro_light | 3.3% | `OnboardingScreen_intro_light_compare.png` |
| OnboardingScreen_intro_dark | 3.3% | `OnboardingScreen_intro_dark_compare.png` |
| OnboardingScreen_intro_light_unfolded | 18.3% | `OnboardingScreen_intro_light_unfolded_compare.png` |
| OnboardingScreen_poses_light | 1.8% | `OnboardingScreen_poses_light_compare.png` |
| OnboardingScreen_poses_dark | 1.8% | `OnboardingScreen_poses_dark_compare.png` |
| OnboardingScreen_premium_light | 1.3% | `OnboardingScreen_premium_light_compare.png` |
| OnboardingScreen_premium_dark | 1.3% | `OnboardingScreen_premium_dark_compare.png` |
| OnboardingScreen_camera_light | 1.7% | `OnboardingScreen_camera_light_compare.png` |
| OnboardingScreen_camera_dark | 1.7% | `OnboardingScreen_camera_dark_compare.png` |
| OnboardingScreen_camera_light_unfolded | 1.3% | `OnboardingScreen_camera_light_unfolded_compare.png` |

Nhận xét của phiên viết code (T-082; rà soát độc lập bổ sung ở bước 6):

- Mọi trang: đủ các phần, đúng thứ tự, chữ đúng bảng chữ (mục 2). Hàng chấm, huy hiệu 96, tiêu đề, điểm và lời ở đúng vị trí của `context/` trong sai số 4 dp (test `*_layoutMatchesDesign`). Nút "Tiếp" và "Bật camera" cùng một chỗ trên mọi trang.
- Nút cao 56 so với 60 của thiết kế (T-007 đã chốt 56), nên đỉnh nút thấp hơn 4 dp (811 thay 807; 761 thay 757 ở khung mở gập) và ghi chú ở trang cuối thấp hơn 4 dp. Phần lớn tỷ lệ khác ở các frame là viền chữ khi vẽ và dải 4 dp này.
- `intro_light_unfolded` (18.3%): `context/OnboardingScreen_intro_light_unfolded.md` đặt hình minh họa sát trái trong khung 625, lệch với mục 3 và mục 6 của README ("căn giữa"). Theo câu trả lời câu 9 của plan T-082 (Nhật, 2026-11-01), code theo README: hình 363 x 280 căn giữa trong khung 625. Phần khác gần như toàn bộ là vị trí ngang của hình; chữ và nút khớp. Frame Figma chưa căn lại.
- Icon huy hiệu trang premium là `workspace_premium` của Material Symbols Outlined, cùng bộ với các icon `ic_onboarding_*` có sẵn.

Đối chiếu các frame có thẻ của T-083 (`scripts/design-compare.sh onboarding`, 2026-11-02, RUN-20261101-01): ảnh chuẩn chụp bằng tiếng Việt, luồng 4 trang, thẻ giả `NativeAdCard(NativeAdSize.STEP)` với chữ giả của ảnh chuẩn `NativeAdCard_step_*` của khung; frame `intro-ad-wait` dùng khung chờ `NativeAdPlaceholder(NativeAdSize.STEP)` chụp với đồng hồ dừng sau một vòng nhấp nháy (1 600 ms) và nút "Tiếp" `PrimaryButton(enabled = false)`.

| Màn hình | Điểm ảnh khác | Ảnh so sánh (thiết kế, thật, khác biệt) |
| --- | --- | --- |
| OnboardingScreen_intro-ad_light | 6.1% | `OnboardingScreen_intro-ad_light_compare.png` |
| OnboardingScreen_intro-ad_dark | 6.8% | `OnboardingScreen_intro-ad_dark_compare.png` |
| OnboardingScreen_poses-ad_light | 2.7% | `OnboardingScreen_poses-ad_light_compare.png` |
| OnboardingScreen_poses-ad_dark | 3.4% | `OnboardingScreen_poses-ad_dark_compare.png` |
| OnboardingScreen_premium-ad_light | 2.2% | `OnboardingScreen_premium-ad_light_compare.png` |
| OnboardingScreen_premium-ad_dark | 3.0% | `OnboardingScreen_premium-ad_dark_compare.png` |
| OnboardingScreen_camera-ad_light | 2.6% | `OnboardingScreen_camera-ad_light_compare.png` |
| OnboardingScreen_camera-ad_dark | 3.3% | `OnboardingScreen_camera-ad_dark_compare.png` |
| OnboardingScreen_intro-ad-wait_light | 4.9% | `OnboardingScreen_intro-ad-wait_light_compare.png` |
| OnboardingScreen_intro-ad-wait_dark | 4.9% | `OnboardingScreen_intro-ad-wait_dark_compare.png` |

Nhận xét của phiên viết code (T-083; rà soát độc lập bổ sung ở bước 6):

- Thẻ rộng hết cột (363), không lề ngoài, đỉnh 537 ở trang 1 tới 3 và 481 ở trang cuối, cao 250; ghi chú và nút ở dưới thẻ, cách 24 (test `*Ad_layoutMatchesDesign`). Các số này thấp hơn thiết kế 4 dp vì nút cao 56 thay 60 (mục 6, như T-082).
- `intro-ad` (6.1%, 6.8%): thêm vào dải 4 dp ở trên, tiêu đề xuống dòng sau "không" thay vì sau "cần" (như frame `intro` không thẻ của T-082), nên hình minh họa cao 233 thay 229 và tiêu đề, lời giới thiệu lệch vài dp; thẻ giả ghi "Advertiser" thay "Advertiser or short body" của Figma (chữ trong thẻ là chữ của nhà quảng cáo, mục 2).
- `intro-ad-wait` (4.9%): khung chờ của khung `State=Loading` và nút mờ đúng màu của thiết kế (nền `onSurface` 0.12, chữ 0.38, không bóng); khác biệt là dải 4 dp và chữ của trang 1 như trên.
