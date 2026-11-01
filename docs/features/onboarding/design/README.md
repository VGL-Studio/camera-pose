---
feature: onboarding
status: approved                # draft | approved
spec: features/onboarding/spec.md
tool: figma
link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=66-2284
designer: AI (Claude), Nhật
approved_by: Nhật
approved_at: 2026-10-31
---

# THIẾT KẾ: MÀN CHÀO VÀ LUỒNG GIỚI THIỆU

Màu, chữ, thành phần chung: [docs/design-system/sticker.md](../../../design-system/sticker.md).

Link Figma:

- Luồng giới thiệu 4 trang (yêu cầu 2026-10-31, cho T-082, T-083): section "Onboarding flow", https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=66-2284. Xuất ngày 2026-10-31.
- Màn chào một trang (bản cũ, T-012): section "Onboarding", https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=18-128.

## 1. Các màn hình và trạng thái

Luồng có 4 trang khi cờ `feature_premium_poses_enabled` bật; cờ tắt thì bỏ trang `premium`, còn 3 trang (hàng chấm còn 3 chấm). Mọi frame vẽ luồng 4 trang.

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Ticket | Kịch bản |
| --- | --- | --- | --- | --- |
| `OnboardingScreen` | Trang 1, giới thiệu | `OnboardingScreen_intro_light`, `OnboardingScreen_intro_dark`, `OnboardingScreen_intro_light_unfolded` | T-082 | KB-1, KB-5 |
| `OnboardingScreen` | Trang 2, tư thế | `OnboardingScreen_poses_light`, `OnboardingScreen_poses_dark` | T-082 | KB-5 |
| `OnboardingScreen` | Trang 3, tư thế premium (chỉ khi cờ premium bật) | `OnboardingScreen_premium_light`, `OnboardingScreen_premium_dark` | T-082 | KB-5 |
| `OnboardingScreen` | Trang cuối, bật camera | `OnboardingScreen_camera_light`, `OnboardingScreen_camera_dark`, `OnboardingScreen_camera_light_unfolded` | T-082 | KB-1, KB-2, KB-5 |
| `OnboardingScreen` | Trang 1 có thẻ native | `OnboardingScreen_intro-ad_light`, `OnboardingScreen_intro-ad_dark` | T-083 | KB-6 |
| `OnboardingScreen` | Trang 2 có thẻ native | `OnboardingScreen_poses-ad_light`, `OnboardingScreen_poses-ad_dark` | T-083 | KB-6 |
| `OnboardingScreen` | Trang 3 có thẻ native | `OnboardingScreen_premium-ad_light`, `OnboardingScreen_premium-ad_dark` | T-083 | KB-6 |
| `OnboardingScreen` | Trang cuối có thẻ native | `OnboardingScreen_camera-ad_light`, `OnboardingScreen_camera-ad_dark` | T-083 | KB-6 |
| `OnboardingScreen` | Trang 1 chờ quảng cáo: thẻ là khung chờ, nút "Tiếp" mờ | `OnboardingScreen_intro-ad-wait_light`, `OnboardingScreen_intro-ad-wait_dark` | T-083 | KB-6 (câu 8) |

Tên trạng thái `intro`, `poses`, `premium`, `camera` theo đề xuất của plan T-082, không đổi.

## 2. Bảng chữ

Chữ cũ của màn chào giữ nguyên, chia sang các trang. Chữ mới (tiêu đề trang 2, 3, cuối, lời trang 3, nút "Tiếp", mô tả của hàng chấm) theo đề xuất của plan T-082 (mục 8, câu 2), Nhật chốt ngày 2026-10-31.

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| Trang 1 `intro` | Tiêu đề | Chụp dáng đẹp, không cần đoán | `headlineMedium` |
| Trang 1 `intro` | Giới thiệu | Chọn một tư thế, đứng vào nét viền rồi bấm chụp. Ảnh lưu vào thư viện máy, không kèm nét viền. | `bodyLarge` |
| Trang 2 `poses` | Tiêu đề (mới) | Tư thế cho mọi kiểu ảnh | `headlineMedium` |
| Trang 2 `poses` | Điểm 1 | Hơn 200 tư thế: một người, cặp đôi, nhóm bạn | `bodyMedium` |
| Trang 2 `poses` | Điểm 3 | Lưu tư thế yêu thích để chụp lại sau | `bodyMedium` |
| Trang 3 `premium` | Tiêu đề (mới) | Tư thế premium | `headlineMedium` |
| Trang 3 `premium` | Lời (mới) | Xem một quảng cáo để mở một tư thế premium, rồi dùng mãi về sau. | `bodyLarge` |
| Trang cuối `camera` | Tiêu đề (mới) | Sẵn sàng chụp | `headlineMedium` |
| Trang cuối `camera` | Điểm 2 | Chỉnh độ mờ, cỡ và vị trí nét viền | `bodyMedium` |
| Trang cuối `camera` | Ghi chú | App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn. | `bodySmall` |
| Trang cuối `camera` | Nút | Bật camera | chữ của nút chính (`titleMedium`) |
| Các trang trước trang cuối | Nút (mới) | Tiếp | chữ của nút chính (`titleMedium`) |
| Mọi trang | Mô tả hàng chấm cho trình đọc màn hình (mới, không hiện trên ảnh) | Trang %1$d trên %2$d | |
| Có thẻ native | Nhãn, chữ trong thẻ | Nhãn "Ad" và chữ của thẻ thuộc `:core:ads` và của nhà quảng cáo, không phải chữ của app; "Headline of the ad", "Advertiser or short body", "Install" là chữ giả của ảnh chuẩn `NativeAdCard_step_*` | theo khung |

Tiếng Anh của chữ mới theo bảng của plan T-082, mục 3.1: `Next`, `A pose for every shot`, `Premium poses`, `Watch one ad to unlock a premium pose, then keep it for good.`, `Ready to shoot`, `Page %1$d of %2$d`.

## 3. Ánh xạ sang hệ thống thiết kế

Bố cục từ trên xuống, giống nhau trên mọi trang: một cột lấp màn, nền `background`, lề trái, phải và dưới `Spacing.lg`, lề trên 56 (`Spacing.xl + Spacing.lg`, như màn chào cũ).

| Phần tử trên thiết kế | Thành phần trong code | Màu (vai trò) | Kiểu chữ | Khoảng cách, kích thước |
| --- | --- | --- | --- | --- |
| Hàng chấm (cố định, không cuộn) | Hàng chấm của `:feature:onboarding` (component Figma `PageDot`) | Chấm của trang hiện tại `secondary`, chấm khác `outlineVariant` | | Chấm hiện tại 24 x 8 (`Spacing.lg` x `Spacing.sm`), chấm khác 8 x 8, bo tròn; cách nhau `Spacing.sm` |
| Vùng trang (cuộn, `weight(1f)`) | `Column` cuộn dọc | | | Lề trên `Spacing.lg` và lề dưới `Spacing.lg` nằm **trong** phần cuộn; các khối cách nhau `Spacing.lg` |
| Hình minh họa (trang 1) | `OnboardingIllustration` giữ nguyên | như màn chào cũ | | Cao 280, rộng hết, căn giữa; co đều cả hình khi thiếu chỗ (như `ShrinkFirstLayout` hiện có). Ở frame có thẻ, hình còn 229 |
| Huy hiệu (trang 2, 3, cuối) | Huy hiệu của `:feature:onboarding` (component Figma `IconBadge`) | Nền `secondaryContainer`, viền `outline` dày `Stroke.sticker`, icon `onSecondaryContainer` | | Tròn 96, icon 48, căn trái: `accessibility_new` (trang 2), `workspace_premium` (trang 3), `photo_camera` (trang cuối) |
| Tiêu đề, lời | `Text` | `onBackground`, `onSurfaceVariant` | `headlineMedium`, `bodyLarge` | Rộng hết, căn trái |
| Hàng điểm | `FeatureRow` hiện có (component Figma `FeatureRow`) | Vòng `secondaryContainer`, viền `outline`; chữ `onSurface` | `bodyMedium` | Vòng 40, icon 22, cách chữ `Spacing.md`; các hàng cách nhau `Spacing.md`; icon `accessibility_new`, `bookmark` (trang 2), `tune` (trang cuối) |
| Khe thẻ native (chỉ frame `-ad`) | `NativeStepAd` của `:core:ads` (component Figma `Native/Step`, `State=Ad`; khung chờ `State=Loading`) | Theo khung: nền `surfaceContainerLow`, viền `outlineVariant` 1, media `surfaceContainer`, icon `secondaryContainer`, nhãn "Ad" `tertiary`, CTA `tertiaryContainer`; các khối của khung chờ `surfaceContainerHighest` | Theo khung | Rộng hết, cao 250 (`NativeStepAdDefaults.height()`), **không có lề ngoài**: khoảng cách phía trên thẻ là lề dưới `Spacing.lg` của vùng trang |
| Ghi chú quyền (chỉ trang cuối) | `Text`, căn giữa | `onSurfaceVariant` | `bodySmall` | Dưới khe thẻ, trên nút; cách phía trên `Spacing.lg` |
| Nút chính (ghim đáy) | `PrimaryButton`: "Tiếp" không icon; "Bật camera" icon `photo_camera` | `secondary`, `onSecondary`, bóng `shadowSticker` | `titleMedium` | Rộng hết, cách phía trên `Spacing.lg`, đáy cách đáy màn `Spacing.lg`. Đỉnh nút ở y 807 trên mọi frame 411 x 891 (y 757 ở frame `_unfolded`), có hay không có thẻ |
| Nút "Tiếp" mờ (frame `-ad-wait`) | `PrimaryButton(enabled = false)` (component Figma `Button`, `Variant=Primary, State=Disabled`) | Như `PrimaryButton` tắt trong code: nền `onSurface` mờ 0.12, chữ `onSurface` mờ 0.38, viền `outlineVariant`, không bóng | `titleMedium` | Cùng chỗ, cùng cỡ với nút thường |

Giá trị `context/` đánh dấu "ngoài hệ thống":

- Khoảng cách 56: lề trên của màn, `Spacing.xl + Spacing.lg` như màn chào cũ.
- Khoảng cách 12 và 2, bo 12, chữ 13: số của khung `:core:ads` (`NativeAdDimens.InnerPadding`, `Step*`, khung chờ), không phải số của app.
- Kiểu chữ 23/23 (frame `intro-ad`, `intro-ad-wait`): icon camera của hình minh họa sau khi hình co, không phải kiểu chữ riêng.
- Độ mờ của nút tắt nằm ở lớp (`độ mờ của lớp 0.12` cho khối nền "container", `0.38` cho chữ), không ở lớp tô, vì Figma bỏ độ mờ của lớp tô gắn biến trên instance (L-023). Trong code là màu tắt của `PrimaryButton`, không đổi.
- Nút trên Figma cao 60, trong code 56 (T-007 đã chốt 56): giữ 56.

- **Thành phần mới cần thêm vào hệ thống thiết kế:** không có trong `:core:designsystem`. Component Figma mới đều vẽ phần đã có trong code hoặc riêng của feature: `PageDot`, `IconBadge`, `FeatureRow` vẽ phần tử của `:feature:onboarding` (plan T-082 quyết chúng nằm ở đâu); `Native/Step` vẽ lại thẻ `NativeAdSize.STEP` của `:core:ads` (trang Design system, cạnh `NativeAdCard`), không phải thành phần mới; biến thể `Button` `State=Disabled` vẽ `PrimaryButton(enabled = false)` đã có. Bộ thiết kế quảng cáo của dự án mẫu không gắn vào file Figma của app, nên `Native/Step` được vẽ lại theo số của `NativeAdDimens.Step*` và ảnh chuẩn `NativeAdCard_step_*`, `NativeAdPlaceholder_step_light` (Nhật chọn ngày 2026-10-31).

## 4. Luồng thao tác

Màn loading xong ở lần mở đầu: trang 1 `intro`; bấm "Tiếp" sang trang 2 `poses`; bấm "Tiếp" sang trang 3 `premium` (khi cờ premium bật) rồi trang cuối `camera`; bấm "Bật camera", hộp hỏi quyền của hệ thống, sang `CameraScreen` (cho phép hay từ chối đều sang). Nút quay lại của máy ở trang 2 trở đi về trang trước; ở trang 1 đóng app. Không có nút "Bỏ qua". Trang đổi ngay, không hiệu ứng trượt.

## 5. Vị trí quảng cáo

`native_onboarding`, định dạng `native_step` (spec mục 5): thẻ 250 dp ở mọi trang, kể cả trang cuối, giữa vùng trang và ghi chú hay nút. Thẻ có ranh giới (viền `outlineVariant`), nhãn "Ad" luôn thấy, CTA màu hồng khác nút chính đen của app; thẻ cách nút chính và ghi chú `Spacing.lg` (ADR-0022 cần ít nhất 16). Trang 1 chờ quảng cáo tối đa 5 giây: thẻ hiện khung chờ cùng chiều cao, nút "Tiếp" mờ (`intro-ad-wait`). Vị trí tắt hay không có quảng cáo thì không có khe, không có khoảng trống (các frame không có `-ad`).

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục, thứ tự thành phần, chữ (mục 2), vai trò màu và kiểu chữ (mục 3); nút cùng một chỗ trên mọi trang.
- **Được phép khác:** cách vẽ chữ và bóng của hệ thống, chênh lệch nhỏ theo lưới 4 dp, nút cao 56 thay vì 60, chữ trong thẻ quảng cáo (của nhà quảng cáo).
- **Phần ghim và phần cuộn:** hàng chấm ghim trên cùng; vùng trang cuộn và co theo chỗ còn lại; khe thẻ, ghi chú quyền (trang cuối) và nút ghim ở đáy, không cuộn. Thẻ không bao giờ che ghi chú hay nút. Ở trang 1, hình minh họa co trước (`ShrinkFirstLayout`), sau đó vùng trang cuộn.
- **Màn hình thấp (360 x 640), đo trên Figma ngày 2026-10-31** bằng bản sao tạm của frame đổi cỡ (nút cao 60 như Figma; trong code nút 56 nên vùng trang thêm 4):

| Trang ở 360 x 640 | Vùng trang (cuộn) | Trừ lề trong 24 trên, 24 dưới | Nội dung trang cần (không kể lề) | Nút |
| --- | --- | --- | --- | --- |
| Trang 1 `intro`, có thẻ | 218 | 170 | 421 khi hình 229; khoảng 352 khi hình co tới 160: cuộn | Đỉnh 556, đáy 616 |
| Trang cuối `camera`, có thẻ | 162 | 114 | 220: cuộn | Đỉnh 556, đáy 616 |
| Trang 1 `intro`, không thẻ | 468 | 420 | 472 khi hình 280: hình co còn khoảng 228, không cần cuộn | Đỉnh 556, đáy 616 |
| Trang cuối `camera`, không thẻ | 412 | 364 | 220: không cuộn | Đỉnh 556, đáy 616 |

  Ở trang cuối có thẻ, phần ghim gồm thẻ 250, ghi chú 56 (hai dòng 32 và cách 24), nút 84 (60 và cách 24); thẻ ở y 226 tới 476, ghi chú y 500 tới 532. Không ẩn thẻ, không giảm lề ở khung thấp (plan T-083, mục 8, câu 7): vùng trang còn ít nhưng cuộn được, nút và ghi chú luôn thấy.
- **Khi mở gập (673 x 841):** cùng bố cục một cột, rộng hết; hình minh họa giữ 280, căn giữa.

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
