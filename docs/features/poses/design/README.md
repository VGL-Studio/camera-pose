---
feature: poses
status: approved                # draft | approved
spec: features/poses/spec.md
tool: figma
link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=20-23
designer: AI (Claude), Nhật
approved_by: Nhật
approved_at: 2026-10-31
---

# THIẾT KẾ: THƯ VIỆN TƯ THẾ VÀ CHI TIẾT TƯ THẾ

Màu, chữ, thành phần chung: [docs/design-system/sticker.md](../../../design-system/sticker.md). Chi tiết tư thế ở section PoseDetail trên Figma (node `20-501`). Tư thế premium (spec KB-8, plan T-086) ở section "Premium poses": https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=75-2531 (Nhật duyệt ngày 2026-10-31); dải tư thế của camera ở gói `camera`.

## 1. Các màn hình và trạng thái

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Kịch bản |
| --- | --- | --- | --- |
| `PosesScreen` | Bình thường, chip "Tất cả" | `PosesScreen_default_light`, `PosesScreen_default_dark` | KB-1, KB-2, KB-3 |
| `PosesScreen` | Đang tải thêm, khung xương | `PosesScreen_loading_light`, `PosesScreen_loading_dark` | KB-5 |
| `PosesScreen` | Đọc catalog lỗi | `PosesScreen_error_light`, `PosesScreen_error_dark` | KB-5 |
| `PosesScreen` | Chip "Đã lưu" chưa có tư thế | `PosesScreen_saved-empty_light`, `PosesScreen_saved-empty_dark` | KB-3 |
| `PosesScreen` | Có thanh đáy native | `PosesScreen_ad_light`, `PosesScreen_ad_dark` | KB-6 |
| `PosesScreen` | Có ô quảng cáo trong lưới, sau 8 tư thế | `PosesScreen_ad-collection_light` | KB-6 |
| `PosesScreen` | Máy gập mở ra, 3 cột | `PosesScreen_default_light_unfolded`, `PosesScreen_default_dark_unfolded` | KB-7 |
| `PoseDetailScreen` | Bình thường | `PoseDetailScreen_default_light`, `PoseDetailScreen_default_dark` | KB-4 |
| `PoseDetailScreen` | Đang tải ảnh lớn | `PoseDetailScreen_loading_light`, `PoseDetailScreen_loading_dark` | KB-5 |
| `PoseDetailScreen` | Tải ảnh lỗi | `PoseDetailScreen_error_light`, `PoseDetailScreen_error_dark` | KB-5 |
| `PoseDetailScreen` | Có thanh đáy native, dưới nút chính | `PoseDetailScreen_ad_light`, `PoseDetailScreen_ad_dark` | KB-6 |
| `PoseDetailScreen` | Máy gập mở ra, hai cột | `PoseDetailScreen_default_light_unfolded`, `PoseDetailScreen_default_dark_unfolded` | KB-7 |
| `PosesScreen` | Tư thế premium chưa mở khóa: chip "Tất cả", ba thẻ có dấu premium, một thẻ vừa có dấu vừa đã lưu | `PosesScreen_premium_light`, `PosesScreen_premium_dark` | KB-8 |
| `PoseDetailScreen` | Tư thế premium chưa mở khóa: dấu đầu dòng phụ, nút mở khóa | `PoseDetailScreen_premium_light`, `PoseDetailScreen_premium_dark` | KB-8 |
| Hộp thoại mở khóa (`RewardedAdDialog` của khung với nội dung của app) | Bình thường | `PoseUnlockDialogScreen_default_light`, `PoseUnlockDialogScreen_default_dark` | KB-8 |

Hộp thoại mất mạng thuộc gói `network`.

**Frame hộp thoại mở khóa.** Khung 411 x 891 (để `scripts/figma-export.py` xuất được), nền `background` trơn, chỉ có thẻ hộp thoại ở giữa: không vẽ màn phía sau, không lớp mờ, giống ảnh chuẩn `RewardedAdDialog_default_*` của `:core:ads`. Thẻ thật rộng 312, cao 288, ở (50, 302). Tên frame có `Screen` (`PoseUnlockDialogScreen_*`, như `NetworkDialogScreen_*`) vì script chỉ nhận tên dạng `<Ten>Screen_<trạng thái>_<light|dark>`; plan T-086 đề xuất `PoseUnlockDialog_default_*`, nên plan đổi tên ảnh chuẩn theo.

Từ T-079 (câu hỏi 5 của spec, 2026-10-30), trạng thái `ad` vẽ thanh đáy native nhỏ của khung thay banner 320 x 50. `PosesScreen_ad-collection_light` là lưới cuộn tới tư thế thứ 7: hàng thứ năm có ô quảng cáo (sau 8 tư thế) và tư thế thứ 9; frame này không vẽ thanh đáy, để thấy riêng thẻ quảng cáo cạnh thẻ tư thế. Tên tư thế trên frame này chỉ để minh họa.

## 2. Bảng chữ

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| Thư viện | Tiêu đề | Thư viện tư thế | `titleLarge` |
| Thư viện | Dòng tổng số | 214 tư thế trong 8 nhóm (`%1$d tư thế trong %2$d nhóm`) | `bodySmall` |
| Thư viện | Chip | Tất cả; Đã lưu; rồi tên nhóm theo catalog (Nữ, Nam, Cặp đôi, Bạn bè, Gia đình, Trẻ em, Du lịch, Cà phê) | `labelLarge` |
| Thư viện | Thẻ: tên, dòng phụ | Tên tư thế từ catalog; "Một người · 4 bước" (loại · `%1$d bước`) | `titleMedium`, `bodySmall` |
| Thư viện, lỗi | Tiêu đề, lời hướng dẫn, nút | Không tải được thư viện; Đã có lỗi khi tải thư viện. Thử lại sau ít phút.; Thử lại | `titleLarge`, `bodyMedium`, nút chính |
| Thư viện, đã lưu rỗng | Tiêu đề, lời hướng dẫn, nút | Chưa lưu tư thế nào; Chạm biểu tượng lưu trên thẻ tư thế để giữ lại ở đây.; Xem tất cả tư thế | `titleLarge`, `bodyMedium`, nút chính |
| Có quảng cáo native | Nhãn, chữ trong thẻ | Nhãn "Ad" và chữ của thẻ thuộc `:core:ads` (`strings.xml` của khung) và của nhà quảng cáo, không phải chữ của app; chữ trên thiết kế (Headline of the ad, Advertiser or short body, Install) là chữ giả của ảnh chuẩn `NativeAdCard` | Theo khung |
| Chi tiết | Tiêu đề | Tên tư thế từ catalog | `titleLarge` |
| Chi tiết | Dòng phụ | Một người · Nữ · Toàn thân · 4 bước (loại · nhóm · khung hình · số bước) | `bodyMedium` |
| Chi tiết | Tiêu đề khối | Các bước | `titleMedium` |
| Chi tiết | Các bước | Nội dung từ catalog, đánh số 1, 2, 3… | `bodyMedium`, số `labelLarge` |
| Chi tiết | Khối lỗi | Lỗi hay gặp; nội dung từ catalog | `titleSmall`, `bodyMedium` |
| Chi tiết | Nút chính | Chụp với tư thế này | chữ của nút chính |
| Chi tiết, lỗi ảnh | Tiêu đề, lời hướng dẫn, nút | Không tải được ảnh; Đã có lỗi khi tải ảnh. Thử lại sau ít phút.; Thử lại | `titleLarge`, `bodyMedium`, nút chính |
| Tư thế premium | Nhãn trợ năng của dấu (`premium_badge_label`, không hiện trên ảnh) | Tư thế premium | |
| Chi tiết, tư thế premium | Nút chính (`poses_detail_unlock`) | Xem quảng cáo để dùng (icon `smart_display`) | chữ của nút chính |
| Hộp thoại mở khóa | Tiêu đề (`premium_unlock_title`) | Mở khóa tư thế | `headlineSmall` |
| Hộp thoại mở khóa | Tên tư thế | Tên tư thế từ catalog (trên ảnh: Tựa lan can) | `titleMedium` |
| Hộp thoại mở khóa | Lời giải thích (`premium_unlock_message`) | Xem một quảng cáo ngắn để mở tư thế này, rồi dùng mãi về sau trên máy này. | `bodyMedium` |
| Hộp thoại mở khóa | Hai nút của khung (`ads_reward_watch`, `ads_reward_no_thanks` của `:core:ads`, app dịch) | Xem quảng cáo; Để sau | `labelLarge` |

Chữ premium Nhật chốt ngày 2026-10-31: dùng "premium" như trang 3 của luồng giới thiệu ("Tư thế premium"), không dùng "cao cấp" của plan T-086. Dấu trên ô của dải camera đọc là `camera_cd_thumb_premium` (gói `camera`). Lỗi ghi mở khóa (`poses_unlock_failed`, plan T-086 câu 7) không có frame: "Chưa mở khóa được tư thế. Thử lại." trong `Snackbar` như các thông báo khác.

Loại tư thế: Một người, Cặp đôi, Nhóm. Khung hình: Toàn thân, Nửa người, Cận mặt. Tên, bước, lỗi hay gặp đến từ catalog theo ngôn ngữ của máy; chữ mẫu trên thiết kế chỉ để minh họa.

## 3. Ánh xạ sang hệ thống thiết kế

| Phần tử trên thiết kế | Thành phần trong code | Màu (vai trò) | Kiểu chữ | Khoảng cách, kích thước |
| --- | --- | --- | --- | --- |
| Thanh trên | `TopAppBar`: quay lại, tiêu đề, cài đặt (thư viện) hoặc lưu (chi tiết) | `onSurface` trên `background` | `titleLarge` | Cao 64 |
| Dòng tổng số | `Text` | `onSurfaceVariant` | `bodySmall` | Lề trái `Spacing.md` |
| Hàng chip | `StickerChip` cuộn ngang | Đang chọn `secondaryContainer` và `SelectedBadge` | `labelLarge` | Lề `Spacing.md`, cách nhau `Spacing.sm` |
| Lưới | `LazyVerticalGrid` 2 cột (3 khi mở gập) | | | Lề `Spacing.md`, khoảng cách `Spacing.md` |
| Thẻ tư thế | `PoseCard` | `surface`, viền `outline` 2; nền ảnh `category/*` theo nhóm của tư thế | `titleMedium`, `bodySmall` | Bo `card`, lề trong `Spacing.sm`; ảnh cao 210 (theo tỷ lệ), bo `image` |
| Nút lưu trên thẻ | Vòng 36 trong vùng chạm 48, icon `bookmark` hoặc `bookmark_added` | Chưa lưu `surface`; đã lưu `secondaryContainer`; viền `outline` | | Góc trên phải của ảnh |
| Khung xương | `PoseCardSkeleton` | `surfaceContainerHighest`, viền `outlineVariant` | | Cùng cỡ thẻ |
| Trạng thái lỗi, rỗng | `StateMessage` | Vòng icon `secondaryContainer` | `titleLarge`, `bodyMedium` | Giữa vùng còn lại |
| Ảnh lớn ở chi tiết | Ảnh `preview_1200.webp` | Nền `category/*`; đang tải `surfaceContainerHighest` và vòng chờ; lỗi `surfaceVariant` và `StateMessage` | | Rộng hết trừ lề, cao 380, bo `card`, viền `outline` 2 |
| Các bước | `Row`: số trong vòng 28 và chữ | Vòng `secondaryContainer`, viền `outline` | `labelLarge`, `bodyMedium` | Cách nhau `Spacing.sm` |
| Lỗi hay gặp | `Row`: icon `warning` và chữ | `tertiaryContainer`, `onTertiaryContainer`, viền `outline` | `titleSmall`, `bodyMedium` | Lề trong `Spacing.md`, bo `card` |
| Thanh nút đáy ở chi tiết | Đường kẻ 2 và `PrimaryButton` `photo_camera` | Đường kẻ `outlineVariant`; nền `background` | | Cao 96; nút rộng hết trừ lề `Spacing.md` |
| Thanh đáy native | `BottomAdBarScaffold` của `:core:ads` (component Figma `NativeAdSmall`, theo `NativeAdCard` cỡ `SMALL`) | Theo khung: nền `surfaceContainerLow`, viền `outlineVariant` 1, icon `secondaryContainer`, nhãn "Ad" `tertiary`, nút `tertiaryContainer` | Theo khung | Khe 80, cách mép trái, phải, đáy `Spacing.md`; nội dung chừa 96 ở đáy |
| Ô quảng cáo trong lưới | `itemsWithInListAds` của `:core:ads` (component Figma `NativeAdCard`, theo `NativeAdCard` cỡ `CARD`) | Như thanh đáy; ảnh `surfaceContainer` | Theo khung | Chiếm một ô, cùng cỡ thẻ tư thế; lề trong `Spacing.sm` |
| Dấu premium | `PremiumBadge` (mới, `:core:designsystem`; component Figma `PremiumBadge`, ba cỡ) | Nền `tertiaryContainer`, viền `outline` `Stroke.sticker`, icon `workspace_premium` màu `onTertiaryContainer` | | `Size=Card` 32, icon 20: góc trên trái ảnh của thẻ, ở (6, 10) trong ảnh, tâm đối xứng nút lưu 36 qua trục giữa; `Size=Inline` 20, icon 14: đầu dòng phụ của chi tiết, cách chữ `Spacing.sm`; `Size=Thumb` 22, icon 14: ô 64 của camera (gói `camera`). Không làm mờ hay che ảnh |
| Thẻ tư thế premium | `PoseCard(premium = true)` (Figma `PoseCard`, `Premium=Yes`) | Như thẻ thường | | Dấu và nút lưu cùng lúc được |
| Nút mở khóa ở chi tiết | `PrimaryButton` icon `smart_display`, cùng chỗ nút "Chụp với tư thế này" | `secondary`, `onSecondary` | `titleMedium` | Như nút chính |
| Hộp thoại mở khóa | `RewardedAdDialog` của `:core:ads` (thẻ `RewardedAdDialogCard`), tiêu đề của app, nội dung `PremiumUnlockContent` (mới, `:core:designsystem`); component Figma `PremiumUnlockDialog` vẽ lại thẻ của khung | Thẻ `surfaceContainerHigh`; tiêu đề, tên `onSurface`; lời `onSurfaceVariant`; nút "Xem quảng cáo" `primary`, `onPrimary`; "Để sau" `onSurfaceVariant` | `headlineSmall`; tên `titleMedium`; lời `bodyMedium`; nút `labelLarge` | Theo khung: rộng tối đa 312, bo 28 (`shapes.extraLarge`), lề `Spacing.lg`, các phần cách `Spacing.md`; nút 56 bo 28; nút chữ 40; hai nút cách `Spacing.sm`. Nội dung của app: tên và lời cách `Spacing.sm` |

- **Thành phần mới cần thêm vào hệ thống thiết kế:** `PoseCard`, `PoseCardSkeleton`, `StickerChip`, `SelectedBadge`, `StateMessage` (xem `sticker.md`, mục 4). Tư thế premium (T-086): `PremiumBadge` (mới), `PremiumUnlockContent` (mới), tham số `premium` của `PoseCard` và `PoseThumb`; thay đổi module core, rủi ro cao. Giá trị icon 20, 14 và cỡ vòng 32, 22, 20 là số của dấu, `context/` ghi kiểu chữ icon "ngoài hệ thống" như các icon khác.
- **Component quảng cáo trên Figma:** `NativeAdSmall`, `NativeAdCard` (trang Design system, cạnh `AdSlot`) chỉ vẽ lại giao diện của khung `:core:ads` để đặt vào màn hình; không phải thành phần mới của app. Bo góc 12, bo ảnh và nút 8, khoảng cách 12, cỡ chữ 13 và 10 là số của khung (`NativeAdDimens`), `context/` ghi "ngoài hệ thống".

**Máy gập mở ra (673 x 841).** Thư viện: 3 cột. Chi tiết: ảnh 315 x 440 bên trái; cột phải rộng 310 có dòng phụ, các bước, lỗi hay gặp; thanh nút đáy chỉ ở cột phải.

## 4. Luồng thao tác

- `CameraScreen`, nút thư viện, sang `PosesScreen`; quay lại về `CameraScreen`.
- `PosesScreen`, chạm thẻ, sang `PoseDetailScreen`; quay lại về `PosesScreen` giữ chip và vị trí cuộn.
- `PoseDetailScreen`, "Chụp với tư thế này", sang `CameraScreen` với tư thế đó.
- `PosesScreen`, nút cài đặt, sang `SettingsScreen`.
- Tư thế premium chưa mở khóa: chạm thẻ vẫn sang `PoseDetailScreen`; nút "Xem quảng cáo để dùng" mở hộp thoại mở khóa. "Xem quảng cáo" xem hết thì mở khóa và về `CameraScreen` với tư thế đó; "Để sau" hay đóng quảng cáo sớm thì ở lại chi tiết, tư thế vẫn khóa. Lưu và bỏ lưu vẫn được.
- Đã lưu rỗng, "Xem tất cả tư thế", chuyển về chip "Tất cả".

## 5. Vị trí quảng cáo

- `poses_bottom`: thanh đáy native nhỏ ở đáy `PosesScreen`; lưới cuộn phía trên, thẻ cuối không bị che (ảnh `PosesScreen_ad_*`).
- `pose_detail_bottom`: thanh đáy native ở đáy `PoseDetailScreen`, dưới thanh nút chính; nút không bị che (ảnh `PoseDetailScreen_ad_*`).
- `poses_in_collection`: ô quảng cáo trong lưới sau mỗi 8 tư thế, không ở đầu, không ở cuối; thẻ quảng cáo có nhãn "Ad", viền mảnh và nút riêng, khác rõ thẻ tư thế viền đậm (ảnh `PosesScreen_ad-collection_light`).
- `inter_pose_detail` là quảng cáo toàn màn hình của khung, không có thiết kế riêng.
- Khóa tắt, quảng cáo đang tải hay không có thì không chừa khoảng trống, lưới không có ô trống (ảnh `default`). Thanh đáy tách khỏi nút "Chụp với tư thế này" bằng đường kẻ của thanh nút, viền và nền khác màu của thẻ quảng cáo.
- Trước T-079 hai vị trí là banner `poses_banner`, `poses_detail_banner` (câu hỏi 1 của spec).

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục, thứ tự thành phần, chữ (mục 2), vai trò màu và kiểu chữ (mục 3).
- **Được phép khác:** ảnh tư thế (ảnh tạm trên thiết kế), cách vẽ chữ và bóng của hệ thống, hiệu ứng chuyển động, chênh lệch nhỏ theo lưới 4 dp, chiều cao thẻ theo tỷ lệ ảnh thật.
- **Màn hình thấp (360 x 640):** lưới cuộn được; ở chi tiết nội dung cuộn được, thanh nút đáy luôn thấy được, không che chữ cuối; thanh đáy native không đẩy nút chính ra ngoài màn hình.

## 7. Tiêu chí khi duyệt

- [ ] Dùng đúng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế.
- [ ] Đủ các trạng thái trong mục 1, kể cả chế độ tối.
- [ ] Chữ đúng bảng chữ, không bị cắt hay tràn; tên tư thế dài xuống dòng.
- [ ] Bố cục và thứ tự thành phần giống bản mẫu; lệch về vị trí không quá một bước lưới 4 dp.
- [ ] Vùng chạm đủ lớn (ít nhất 48 dp), chữ dễ đọc.
- [ ] Dùng được ở màn hình thấp (360 x 640): nội dung cuộn được, chữ không chồng nhau, nút chính không bị che hay cắt.
- [ ] Vùng quảng cáo tách bạch, không gây bấm nhầm.
- [ ] Giao diện và nội dung không được thiết kế để thu hút chủ yếu trẻ em.

## 8. Kết quả đối chiếu

Chưa có.
