# HỆ THỐNG THIẾT KẾ STICKER CỦA CAMERA POSE

Tài liệu dùng chung cho mọi gói bàn giao `docs/features/<feature>/design/` của app. Nguồn là trang [Design system](https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=12-2) trên Figma, hướng thiết kế Sticker do người phụ trách sản phẩm chọn ngày 2026-10-21 (`docs/research/proposal.md`, mục 10).

Code vẫn là gốc (ADR-0008). Hệ thống thiết kế trong code hiện là của dự án mẫu (phông Inter, màu xanh lục, `tokens.json`). Đổi `:core:designsystem` sang Sticker là một plan rủi ro cao, làm trước mọi màn hình. Sau plan đó, `tokens.json` được sinh lại và các file `context/` được xuất lại cho khớp. Trước lúc đó, mọi giá trị trong `context/` đều hiện "ngoài hệ thống".

## 1. Màu

Bộ biến `color` có hai chế độ Light và Dark. Tên biến trùng vai trò Material 3 trong `tokens.json`. Bảng dưới là giá trị cho plan đổi `:core:designsystem`.

| Vai trò | Sáng | Tối | Dùng cho |
| --- | --- | --- | --- |
| `primary`, `onPrimary` | #2F6BFF, #FFFFFF | #6E9BFF, #0A0F2A | Công tắc đang bật, vòng chờ |
| `primaryContainer`, `onPrimaryContainer` | #DCE6FF, #0A2A7A | #1C3277, #DCE6FF | Chưa dùng trên màn hình |
| `secondary`, `onSecondary` | #111111, #FFFFFF | #FFFFFF, #111111 | Nền và chữ của nút chính |
| `secondaryContainer`, `onSecondaryContainer` | #FFC83D, #111111 | #FFC83D, #111111 | Màu vàng "đang chọn, đang bật": chip, ô tư thế, nút camera bật, dấu ✓, số bước, vòng biểu tượng |
| `tertiary`, `onTertiary` | #FF5C8A, #111111 | #FF8AAA, #3A0A18 | Chưa dùng trên màn hình |
| `tertiaryContainer`, `onTertiaryContainer` | #FFD6E2, #4A0F22 | #6B1D35, #FFD6E2 | Khối "Lỗi hay gặp" |
| `error`, `onError` | #C8322B, #FFFFFF | #FF8A80, #3D0704 | Thanh tiến trình và nhãn khi lỗi |
| `errorContainer`, `onErrorContainer` | #FFDAD5, #5C0F0A | #5C1A14, #FFDAD5 | Rãnh thanh tiến trình khi lỗi |
| `background`, `onBackground` | #FFFDF8, #111111 | #121212, #FFFFFF | Nền màn hình |
| `surface`, `onSurface` | #FFFFFF, #111111 | #1E1E1E, #FFFFFF | Thẻ, hộp thoại, chip thường |
| `surfaceVariant`, `onSurfaceVariant` | #F3F0E8, #5A5650 | #2A2A2A, #B5B0A8 | Nền ô tư thế, chữ phụ |
| `outline` | #111111 | #FFFFFF | Viền dày 2 dp của mọi thẻ, chip, nút |
| `outlineVariant` | #D9D4C8 | #4A4744 | Đường kẻ mảnh, viền khung xương |
| `inverseSurface`, `inverseOnSurface`, `inversePrimary` | #1E1E1E, #FFFFFF, #9DB8FF | #F3F0E8, #111111, #2F6BFF | Thông báo ngắn (snackbar) |
| `surfaceContainer*`, `surfaceBright`, `surfaceDim`, `surfaceTint` | xem Figma | xem Figma | `surfaceContainerHigh` cho vùng quảng cáo, `surfaceContainerHighest` cho khung xương |

Màu riêng của app (thêm vào hệ thống thiết kế, ngoài vai trò Material 3):

| Biến | Giá trị (cả hai chế độ) | Dùng cho |
| --- | --- | --- |
| `success`, `onSuccess` | #1F9D63, #FFFFFF (sáng); #3DDC97, #0B2E1E (tối) | Nét viền khi đạt điểm |
| `successContainer`, `onSuccessContainer` | #3DDC97, #111111 | Lời nhắc "Đúng dáng rồi", viền nút chụp khi đạt |
| `shadowSticker` | #FFC83D | Bóng đổ cứng của nút chính |
| `category/women`, `men`, `couple`, `friends`, `family`, `kids`, `travel`, `cafe` | #FF5C8A, #FFC83D, #3DDC97, #4D8DFF, #5ED3F3, #FFA8D0, #FF8A3D, #B8E986 | Nền ảnh tư thế theo nhóm |
| `category/onCategory` | #111111 | Nét viền trên nền nhóm |
| `camera/background`, `camera/surface` | #0A0A0B, #1E1E1E | Nền camera và màn xem lại (luôn tối) |
| `camera/onCamera`, `camera/onSurfaceVariant` | #FFFFFF, #B5B0A8 | Chữ, lưới, nút chụp trên camera |
| `camera/control`, `camera/onControl` | #FFFFFF, #111111 | Nút tròn trắng viền đen trên camera, thẻ bước, bảng chỉnh |
| `camera/overlayStroke` | #FFFFFF | Nét viền tư thế |
| `camera/scrim` | #000000 | Lớp mờ (độ mờ 35% khi đếm ngược, 45% sau nút zoom, 50% dưới hộp thoại); quầng tối của nét viền |

Camera và màn xem lại luôn dùng chế độ Dark, kể cả khi app ở giao diện sáng.

## 2. Chữ

Phông Bricolage Grotesque (giấy phép OFL, đủ dấu tiếng Việt), thay Inter. Kiểu chữ trùng tên và cỡ với `tokens.json`; chỉ đổi độ đậm:

| Kiểu chữ | Cỡ, chiều cao dòng | Độ đậm |
| --- | --- | --- |
| `display*`, `headline*`, `titleLarge` | như `tokens.json` | ExtraBold (800) |
| `titleMedium` | 16/24 | SemiBold (600) |
| `titleSmall`, `label*` | như `tokens.json` | Medium (500) |
| `body*` | như `tokens.json` | Regular (400) |

Icon: Material Symbols Rounded (giấy phép Apache 2.0), cỡ 20 hoặc 24 dp, một bộ cho cả app. Trên Figma, icon là chữ ligature (ví dụ `photo_camera`); trong code dùng icon vector cùng tên.

## 3. Khoảng cách, bo góc, viền

| Bộ biến | Giá trị |
| --- | --- |
| `spacing` | `xs` 4, `sm` 8, `md` 16, `lg` 24, `xl` 32 (như `Spacing` trong code) |
| `radius` | `image` 10, `button` 14, `thumb` 14, `card` 16, `chip` 18, `sheet` 24, `full` 999 |
| `stroke` | `thin` 1, `sticker` 2, `selected` 3 |
| Hiệu ứng `shadow/sticker` | bóng đổ cứng lệch 4, 4 dp, không mờ, màu `shadowSticker` |

## 4. Thành phần

Thành phần trên Figma và tên đề xuất trong `:core:designsystem`. Thành phần mới là thay đổi module core, rủi ro cao.

| Figma | Biến thể | Trong code (đề xuất) | Ghi chú |
| --- | --- | --- | --- |
| `Button` | `Variant=Primary`, `Secondary`; chữ, icon, có icon hay không | `PrimaryButton` (đổi kiểu), `SecondaryButton` (mới) | Cao 56, bo `button`, viền 2, nút chính có `shadow/sticker` |
| `IconButton` | icon | `IconButton` của Material 3 | Vùng chạm 48 |
| `CameraIconButton` | `State=Default`, `Active`; `Content=Icon`, `Text` | `CameraIconButton` (mới) | Tròn 48, trắng viền đen; đang bật thì vàng; hẹn giờ hiện "3s" |
| `Chip` | `State=Default`, `Selected` | `StickerChip` (mới) | Cao 36, bo `chip`; đang chọn có `SelectedBadge` |
| `SelectedBadge` | | `SelectedBadge` (mới) | Tròn 22, vàng, viền `outline`, dấu ✓ |
| `PoseThumb` | `State=Default`, `Selected` | `PoseThumb` (mới) | Ô 64, bo `thumb`; đang chọn viền vàng 3 và dấu ✓ |
| `PoseCard` | `Saved=No`, `Yes` | `PoseCard` (mới) | Thẻ 2 cột, ảnh trên nền nhóm, nút lưu 48 |
| `SkeletonCard` | | `PoseCardSkeleton` (mới) | Khung xương khi đang tải |
| `StepCard` | | `StepCard` (mới) | Thẻ bước trên camera |
| `ZoomControl` | | `ZoomControl` (mới) | 0.5x, 1x, 2x |
| `MatchHint` | `State=Matching`, `Matched`, `NoPerson` | `MatchHint` (mới) | Điểm khớp và lời nhắc |
| `Slider` | | `Slider` của Material 3, đổi màu | Rãnh 8, núm 24 |
| `AdjustPanel` | | `AdjustPanel` (mới) | Bảng chỉnh nét viền |
| `GridOverlay` | | `GridOverlay` (mới) | Lưới chia ba trên khung 3:4 |
| `TopAppBar` | có nút phải hay không | `TopAppBar` của Material 3 | Cao 64 |
| `StateMessage` | icon, tiêu đề, lời hướng dẫn, có nút hay không | `StateMessage` (mới) | Trạng thái rỗng và lỗi |
| `Snackbar` | | `Snackbar` của Material 3, đổi màu | |
| `SectionHeader`, `SettingsRow` | `Type=Value`, `Switch`, `Link`, `Info` | `SettingsRow` (mới) | |
| `Switch` | `State=On`, `Off` | `Switch` của Material 3, đổi màu | |
| `RadioRow`, `ChoiceDialog` | | `RadioButton` và `AlertDialog` của Material 3, đổi màu | Hộp chọn giao diện, ngôn ngữ |
| `ConfirmDialog` | | `AlertDialog` của Material 3, đổi màu | "Bỏ ảnh này?" |
| `NetworkDialog` | | `NetworkDialog` (mới) | Hộp thoại mất mạng (spec `network`) |
| `ProgressBar` | `State=Loading`, `Failed` | `ProgressBar` (đổi kiểu) | Màn loading |
| `BrandLogo` | | `BrandLogo` (đổi hình) | Logo tạm, chờ icon app |
| `Spinner` | | `CircularProgressIndicator` của Material 3 | |
| `AdSlot` | | `AdSlot` của `:core:ads` | Chỉ là chỗ đặt banner, nội dung quảng cáo không thiết kế |

## 5. Hình tạm trên thiết kế

Ảnh tư thế trên mọi màn hình là một hình nét viền tạm dùng chung (`pose-outline`), không phải nội dung thật. Khối "camera-feed (ảnh tạm)", "Ảnh vừa chụp (ảnh tạm)", "Last photo (ảnh tạm)" là chỗ của hình camera và ảnh của người dùng. Khi so ảnh thiết kế với ảnh thật (`scripts/design-compare.sh`), các vùng này luôn khác; rà soát bỏ qua chúng.
