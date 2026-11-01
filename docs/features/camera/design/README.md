---
feature: camera
status: approved                # draft | approved
spec: features/camera/spec.md
tool: figma
link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=16-24
designer: AI (Claude), Nhật
approved_by: Nhật
approved_at: 2026-10-31
---

# THIẾT KẾ: CAMERA VÀ MÀN XEM LẠI

Màu, chữ, thành phần chung: [docs/design-system/sticker.md](../../../design-system/sticker.md). Màn xem lại ở section Review trên Figma (node `18-23`). Camera và màn xem lại luôn dùng chế độ tối, nên chỉ có ảnh `_dark`. Tư thế premium (spec poses KB-8, plan T-086) ở section "Premium camera": https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=78-2729 (Nhật duyệt ngày 2026-10-31); dấu, hộp thoại mở khóa và chữ của chúng ở gói `poses`.

## 1. Các màn hình và trạng thái

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Kịch bản |
| --- | --- | --- | --- |
| `CameraScreen` | Bình thường: có tư thế, thẻ bước, hẹn giờ 3s và lưới đang bật | `CameraScreen_default_dark` | KB-1, KB-2, KB-4 |
| `CameraScreen` | Chưa chọn tư thế | `CameraScreen_no-pose_dark` | KB-1 |
| `CameraScreen` | Bảng chỉnh nét viền đang mở | `CameraScreen_adjust_dark` | KB-3 |
| `CameraScreen` | Nét viền đã khóa | `CameraScreen_locked_dark` | KB-3 |
| `CameraScreen` | Thẻ bước đã ẩn, nút "Bước" | `CameraScreen_steps-hidden_dark` | KB-2 |
| `CameraScreen` | Có người, đang chấm điểm | `CameraScreen_matching_dark` | KB-5 |
| `CameraScreen` | Đạt điểm | `CameraScreen_matched_dark` | KB-5 |
| `CameraScreen` | Không thấy người | `CameraScreen_no-person_dark` | KB-5 |
| `CameraScreen` | Đang đếm ngược | `CameraScreen_countdown_dark` | KB-6 |
| `CameraScreen` | Đang mở camera | `CameraScreen_starting_dark` | KB-8 |
| `CameraScreen` | Chưa có quyền camera | `CameraScreen_permission-denied_dark` | KB-7 |
| `CameraScreen` | Không mở được camera | `CameraScreen_error_dark` | KB-8 |
| `CameraScreen` | Máy gập mở ra | `CameraScreen_default_dark_unfolded` | KB-11 |
| `CameraScreen` | Dải tư thế có ô tư thế premium chưa mở khóa (ô 4 và 6) và một ô đang chọn không khóa (ô 2) | `CameraScreen_premium_dark` | KB-1 |
| `ReviewScreen` | Bình thường | `ReviewScreen_default_dark` | KB-6 |
| `ReviewScreen` | Đang lưu | `ReviewScreen_saving_dark` | KB-6 |
| `ReviewScreen` | Lưu lỗi | `ReviewScreen_save-error_dark` | KB-6 |
| `ReviewScreen` | Hỏi "Bỏ ảnh này?" | `ReviewScreen_discard_dark` | KB-6 |
| `ReviewScreen` | Máy gập mở ra | `ReviewScreen_default_dark_unfolded` | KB-11 |

Hộp thoại mất mạng thuộc gói `network`.

## 2. Bảng chữ

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| Camera, có tư thế | Thẻ bước, số bước | 2/4 (`%1$d/%2$d`) | `labelLarge` |
| Camera, có tư thế | Thẻ bước, nội dung | Xoay vai 45° về phía máy, cằm hơi hạ. (từ catalog) | `bodyMedium` |
| Camera, thẻ bước ẩn | Nút hiện thẻ bước | Bước | `labelLarge` |
| Camera | Nút hẹn giờ đang bật | 3s, 10s | `titleMedium` |
| Camera | Nút zoom | 0.5, 1x, 2 | `labelLarge` |
| Camera | Chip nhóm | Tất cả; Nữ; Nam; Cặp đôi; Bạn bè; Gia đình; Trẻ em; Du lịch; Cà phê (theo catalog) | `labelLarge` |
| Camera, tư thế premium | Nhãn trợ năng của ô khóa (`camera_cd_thumb_premium`, không hiện trên ảnh) | %1$s, premium, chạm để mở khóa (`%1$s` là tên tư thế) | |
| Camera, chưa chọn tư thế | Lời nhắc | Chọn một tư thế ở dải bên dưới | `labelLarge` |
| Camera, đã khóa | Lời nhắc | Đã khóa lớp phủ | `labelLarge` |
| Camera, bảng chỉnh | Tiêu đề | Chỉnh lớp phủ | `titleMedium` |
| Camera, bảng chỉnh | Thanh trượt | Độ đậm, 70%; Cỡ, 100% (`%1$d%%`) | `labelLarge` |
| Camera, bảng chỉnh | Nút | Lật; Khóa; Ẩn; Đặt lại | `labelMedium` |
| Camera, chấm điểm | Điểm, lời nhắc | 68; Nâng tay trái cao hơn (lời nhắc sinh theo khớp lệch nhiều nhất) | `titleSmall`, `labelLarge` |
| Camera, đạt điểm | Điểm, lời nhắc | 92; Đúng dáng rồi, bấm chụp! | `titleSmall`, `labelLarge` |
| Camera, không thấy người | Lời nhắc | Lùi lại để cả người vào khung | `labelLarge` |
| Camera, đếm ngược | Số | 3 (rồi 2, 1) | `displayLarge` |
| Camera, đang mở | Chữ | Đang mở camera… | `bodyMedium` |
| Camera, chưa có quyền | Tiêu đề, lời hướng dẫn, nút | Chưa có quyền dùng camera; Cho phép camera trong Cài đặt máy để chụp theo tư thế.; Mở cài đặt | `titleLarge`, `bodyMedium`, nút chính |
| Camera, lỗi | Tiêu đề, lời hướng dẫn, nút | Không mở được camera; Camera có thể đang được ứng dụng khác dùng. Đóng ứng dụng đó rồi thử lại.; Thử lại | `titleLarge`, `bodyMedium`, nút chính |
| Xem lại | Tiêu đề | Xem lại | `titleLarge` |
| Xem lại | Nút phụ | Chụp lại; Chia sẻ; Tư thế tiếp | `labelLarge` |
| Xem lại | Nút chính | Lưu vào thư viện (máy gập: Lưu ảnh) | chữ của nút chính |
| Xem lại, đang lưu | Nút chính | Đang lưu… | chữ của nút chính |
| Xem lại, lưu lỗi | Thông báo, nút | Không lưu được ảnh: bộ nhớ máy đã đầy.; Thử lại | `bodyMedium`, `labelLarge` |
| Xem lại, hỏi bỏ ảnh | Tiêu đề, lời, nút | Bỏ ảnh này?; Ảnh chưa được lưu vào thư viện.; Bỏ ảnh; Lưu ảnh | `titleLarge`, `bodyMedium`, chữ của nút |

Dấu ba chấm là một ký tự (`…`).

## 3. Ánh xạ sang hệ thống thiết kế

| Phần tử trên thiết kế | Thành phần trong code | Màu (vai trò) | Kiểu chữ | Khoảng cách, kích thước |
| --- | --- | --- | --- | --- |
| Nền camera | | `camera/background` | | |
| Thanh công cụ trên | `Row`: `CameraIconButton` thư viện bên trái; flash, hẹn giờ, lưới, cài đặt bên phải | Thường: `camera/control`, `camera/onControl`. Đang bật: `secondaryContainer` | | Nút 48, cách nhau `Spacing.sm`; lề `Spacing.md`; đỉnh ở 32 |
| Khung xem trước | `Box` tỷ lệ 3:4, rộng hết | `camera/surface` khi chờ hình | | Đỉnh ở 92, cao 548 |
| Lưới | `GridOverlay` | `camera/onCamera`, độ mờ 18% | | Chia ba, nét 1 |
| Nét viền | Ảnh `outline.webp` của tư thế, tô `camera/overlayStroke`, quầng `camera/scrim` mờ 6 | Đạt điểm: `success` | | Mặc định rộng khoảng một nửa khung, căn giữa |
| Thẻ bước | `StepCard` | `camera/control`, `camera/onControl`; số bước `secondaryContainer` | `labelLarge`, `bodyMedium` | Rộng khung trừ 2 x `Spacing.md`, cách mép `Spacing.md`, bo `card`; mũi tên vùng chạm 40 x 48; chạm thẻ ngoài mũi tên thì thẻ ẩn, như vuốt lên (spec KB-2, FB-20261026-02) |
| Nút "Bước" | `Row` icon `format_list_numbered` và chữ | `camera/control`, `camera/onControl` | `labelLarge` | Cao 48, bo `full`, chỗ của thẻ bước |
| Cột công cụ nét viền | `CameraIconButton` chỉnh, lật, khóa, hiện tư thế | Như thanh trên | | Cách mép phải `Spacing.md`, đỉnh ở 150 trong khung |
| Zoom | `ZoomControl` | Nền `camera/scrim` 45%; mức đang chọn `secondaryContainer` | `labelLarge` | Mỗi nút 40; cách đáy khung `Spacing.md`, căn giữa |
| Bảng chỉnh | `AdjustPanel`, hai `Slider`, bốn nút tròn 48 | `camera/control`, `camera/onControl`; phần đã chạy của thanh `secondaryContainer` | | Cách mép `Spacing.md`, bo `sheet` |
| Lời nhắc trên khung | `Row` icon và chữ | `camera/control`, `camera/onControl` | `labelLarge` | Bo `full` |
| Điểm khớp | `MatchHint` | Đang chấm: vòng điểm `secondaryContainer`. Đạt: nền `successContainer` | `titleSmall`, `labelLarge` | Đặt trên nút zoom, cách `Spacing.md` |
| Đếm ngược | Lớp mờ và vòng số | Lớp mờ `camera/scrim` 35%; vòng `secondaryContainer` | `displayLarge` | Vòng 128, giữa khung. Trong lúc đếm, flash, lưới, hẹn giờ, hàng nhóm, dải tư thế, cột công cụ nét viền và mũi tên thẻ bước mờ 38%; nút zoom, nút đổi camera, nút hủy giữ nguyên (spec KB-6, FB-20261026-04) |
| Trạng thái chờ, lỗi, chưa có quyền | `CircularProgressIndicator` hoặc `StateMessage` | `onSurface` trên nền tối | | Giữa khung |
| Hàng chip | `StickerChip` cuộn ngang | Đang chọn `secondaryContainer` và `SelectedBadge` | `labelLarge` | Đỉnh ở 656, cách nhau `Spacing.sm` |
| Dải tư thế | `PoseThumb` cuộn ngang | Đang chọn viền `secondaryContainer` 3 và `SelectedBadge` | | Ô 64, đỉnh ở 708, cách nhau `Spacing.sm` |
| Ô tư thế premium chưa mở khóa | `PoseThumb(premium = true)` (Figma `PoseThumb`, `State=Default, Premium=Yes`) với `PremiumBadge` cỡ `Thumb` | Dấu: nền `tertiaryContainer`, viền `outline`, icon `workspace_premium` `onTertiaryContainer` (chế độ tối) | | Dấu 22, icon 14, lấn góc trên trái ô ở (-7, -7), đối xứng `SelectedBadge` ở góc trên phải; ảnh của ô không mờ. Chạm ô mở hộp thoại mở khóa (gói `poses`), tư thế đang dùng không đổi |
| Nút chụp | Vòng 76 viền 4 và tròn 62 | `camera/onCamera`; đạt điểm viền `successContainer`; đếm ngược hiện icon `close` | | Tâm ở (205.5, 822) |
| Ô ảnh vừa chụp | Ảnh thu nhỏ | Viền `camera/onCamera` 2 | | 48, bo `thumb`, trái |
| Nút đổi camera | `CameraIconButton` `flip_camera_android` | Mờ (độ mờ 38%, không bấm được) khi camera chưa sẵn sàng: `starting`, `permission-denied`, `error` (quyết định 2026-10-22); và khi máy chỉ có một camera (spec KB-4, câu hỏi 17, 2026-10-27) | | 48, phải |
| Xem lại: thanh trên | `TopAppBar`, nút `close` | `onSurface` | `titleLarge` | |
| Xem lại: ảnh | Ảnh vừa chụp, tỷ lệ 3:4 | Viền `outline` 2 | | Rộng khung trừ 2 x `Spacing.md`, bo `card` |
| Xem lại: nút phụ | Cột icon tròn 48 và chữ | `camera/control`, `camera/onControl`; chữ `onSurface` | `labelLarge` | Ba nút chia đều |
| Xem lại: nút chính | `PrimaryButton` `download` | `secondary` (tối: trắng), `onSecondary` | | Rộng hết, cách đáy `Spacing.lg` |
| Xem lại: lưu lỗi | `Snackbar` | `inverseSurface`, `inverseOnSurface`, `inversePrimary` | | Trên nút chính `Spacing.md` |
| Xem lại: hỏi bỏ ảnh | `ConfirmDialog` trên lớp mờ 50% | `surface`, viền `outline` | | Rộng 331 |

- **Thành phần mới cần thêm vào hệ thống thiết kế:** `CameraIconButton`, `StickerChip`, `SelectedBadge`, `PoseThumb`, `StepCard`, `ZoomControl`, `MatchHint`, `AdjustPanel`, `GridOverlay`, `StateMessage`, `SecondaryButton` (xem `sticker.md`, mục 4).

**Máy gập mở ra (673 x 841).** Camera: khung 3:4 cao 600 bên trái (lề `Spacing.md`); cột phải có đổi camera, nút chụp, ô ảnh vừa chụp, zoom; hàng chip và dải tư thế dưới khung. Xem lại: ảnh 450 x 600 bên trái; nút phụ xếp dọc và nút chính "Lưu ảnh" ở cột phải.

## 4. Luồng thao tác

- `CameraScreen`, bấm nút chụp (hoặc hết đếm ngược), sang `ReviewScreen`.
- `ReviewScreen`: "Lưu vào thư viện" lưu tại chỗ; "Chụp lại", "Tư thế tiếp", nút quay lại về `CameraScreen` (chưa lưu thì hỏi "Bỏ ảnh này?").
- `CameraScreen`, chạm ô tư thế premium chưa mở khóa: hộp thoại mở khóa (gói `poses`, `PoseUnlockDialogScreen_*`); tư thế đang dùng không đổi. Xem hết quảng cáo thì camera dùng tư thế đó; "Để sau" thì không đổi gì.
- `CameraScreen`: nút thư viện sang `PosesScreen`; nút cài đặt sang `SettingsScreen`; ô ảnh vừa chụp sang `ReviewScreen` với ảnh đã lưu gần nhất.
- Chưa có quyền: "Mở cài đặt" mở trang cài đặt app của máy.

## 5. Vị trí quảng cáo

`review_interstitial`: toàn màn hình, hiện khi rời `ReviewScreen` sau khi đã lưu ảnh, trước khi về camera (spec mục 5). Không có vùng quảng cáo nào trên `CameraScreen` và `ReviewScreen`.

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục, thứ tự thành phần, chữ (mục 2), vai trò màu và kiểu chữ (mục 3).
- **Được phép khác:** hình camera và ảnh tư thế (ảnh tạm trên thiết kế), cách vẽ chữ và bóng của hệ thống, hiệu ứng chuyển động, chênh lệch nhỏ theo lưới 4 dp.
- **Màn hình thấp (360 x 640):** khung xem trước co lại theo chiều cao còn lại (giữ 3:4, căn giữa), không co hàng chip, dải tư thế và hàng nút chụp; thẻ bước và bảng chỉnh nằm trong khung. Màn xem lại: ảnh co lại, nút chính luôn thấy được.

## 7. Tiêu chí khi duyệt

- [ ] Dùng đúng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế.
- [ ] Đủ các trạng thái trong mục 1.
- [ ] Chữ đúng bảng chữ, không bị cắt hay tràn.
- [ ] Bố cục và thứ tự thành phần giống bản mẫu; lệch về vị trí không quá một bước lưới 4 dp.
- [ ] Vùng chạm đủ lớn (ít nhất 48 dp), chữ dễ đọc trên nền camera.
- [ ] Dùng được ở màn hình thấp (360 x 640): nút chụp, dải tư thế không bị che hay cắt.
- [ ] Không có quảng cáo nào trên camera và màn xem lại.
- [ ] Giao diện và nội dung không được thiết kế để thu hút chủ yếu trẻ em.

## 8. Kết quả đối chiếu

Chưa có.
