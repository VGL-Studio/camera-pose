---
feature: poses
status: approved                # draft | approved
spec: features/poses/spec.md
tool: figma
link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=20-23
designer: AI (Claude), Nhật
approved_by: Nhật
approved_at: 2026-10-21
---

# THIẾT KẾ: THƯ VIỆN TƯ THẾ VÀ CHI TIẾT TƯ THẾ

Màu, chữ, thành phần chung: [docs/design-system/sticker.md](../../../design-system/sticker.md). Chi tiết tư thế ở section PoseDetail trên Figma (node `20-501`).

## 1. Các màn hình và trạng thái

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Kịch bản |
| --- | --- | --- | --- |
| `PosesScreen` | Bình thường, chip "Tất cả" | `PosesScreen_default_light`, `PosesScreen_default_dark` | KB-1, KB-2, KB-3 |
| `PosesScreen` | Đang tải thêm, khung xương | `PosesScreen_loading_light`, `PosesScreen_loading_dark` | KB-5 |
| `PosesScreen` | Đọc catalog lỗi | `PosesScreen_error_light`, `PosesScreen_error_dark` | KB-5 |
| `PosesScreen` | Chip "Đã lưu" chưa có tư thế | `PosesScreen_saved-empty_light`, `PosesScreen_saved-empty_dark` | KB-3 |
| `PosesScreen` | Có banner đáy | `PosesScreen_ad_light`, `PosesScreen_ad_dark` | KB-6 |
| `PosesScreen` | Máy gập mở ra, 3 cột | `PosesScreen_default_light_unfolded`, `PosesScreen_default_dark_unfolded` | KB-7 |
| `PoseDetailScreen` | Bình thường | `PoseDetailScreen_default_light`, `PoseDetailScreen_default_dark` | KB-4 |
| `PoseDetailScreen` | Đang tải ảnh lớn | `PoseDetailScreen_loading_light`, `PoseDetailScreen_loading_dark` | KB-5 |
| `PoseDetailScreen` | Tải ảnh lỗi | `PoseDetailScreen_error_light`, `PoseDetailScreen_error_dark` | KB-5 |
| `PoseDetailScreen` | Có banner đáy | `PoseDetailScreen_ad_light`, `PoseDetailScreen_ad_dark` | KB-6 |
| `PoseDetailScreen` | Máy gập mở ra, hai cột | `PoseDetailScreen_default_light_unfolded`, `PoseDetailScreen_default_dark_unfolded` | KB-7 |

Hộp thoại mất mạng thuộc gói `network`.

## 2. Bảng chữ

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| Thư viện | Tiêu đề | Thư viện tư thế | `titleLarge` |
| Thư viện | Dòng tổng số | 214 tư thế trong 8 nhóm (`%1$d tư thế trong %2$d nhóm`) | `bodySmall` |
| Thư viện | Chip | Tất cả; Đã lưu; rồi tên nhóm theo catalog (Nữ, Nam, Cặp đôi, Bạn bè, Gia đình, Trẻ em, Du lịch, Cà phê) | `labelLarge` |
| Thư viện | Thẻ: tên, dòng phụ | Tên tư thế từ catalog; "Một người · 4 bước" (loại · `%1$d bước`) | `titleMedium`, `bodySmall` |
| Thư viện, lỗi | Tiêu đề, lời hướng dẫn, nút | Không tải được thư viện; Đã có lỗi khi tải thư viện. Thử lại sau ít phút.; Thử lại | `titleLarge`, `bodyMedium`, nút chính |
| Thư viện, đã lưu rỗng | Tiêu đề, lời hướng dẫn, nút | Chưa lưu tư thế nào; Chạm biểu tượng lưu trên thẻ tư thế để giữ lại ở đây.; Xem tất cả tư thế | `titleLarge`, `bodyMedium`, nút chính |
| Có banner | Nhãn | Quảng cáo (nhãn của AdMob, không phải chữ của app) | |
| Chi tiết | Tiêu đề | Tên tư thế từ catalog | `titleLarge` |
| Chi tiết | Dòng phụ | Một người · Nữ · Toàn thân · 4 bước (loại · nhóm · khung hình · số bước) | `bodyMedium` |
| Chi tiết | Tiêu đề khối | Các bước | `titleMedium` |
| Chi tiết | Các bước | Nội dung từ catalog, đánh số 1, 2, 3… | `bodyMedium`, số `labelLarge` |
| Chi tiết | Khối lỗi | Lỗi hay gặp; nội dung từ catalog | `titleSmall`, `bodyMedium` |
| Chi tiết | Nút chính | Chụp với tư thế này | chữ của nút chính |
| Chi tiết, lỗi ảnh | Tiêu đề, lời hướng dẫn, nút | Không tải được ảnh; Đã có lỗi khi tải ảnh. Thử lại sau ít phút.; Thử lại | `titleLarge`, `bodyMedium`, nút chính |

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
| Banner | `AdSlot` của `:core:ads` | Vùng chờ `surfaceContainerHigh` | | Neo đáy, rộng hết; nội dung phía trên không bị che |

- **Thành phần mới cần thêm vào hệ thống thiết kế:** `PoseCard`, `PoseCardSkeleton`, `StickerChip`, `SelectedBadge`, `StateMessage` (xem `sticker.md`, mục 4).

**Máy gập mở ra (673 x 841).** Thư viện: 3 cột. Chi tiết: ảnh 315 x 440 bên trái; cột phải rộng 310 có dòng phụ, các bước, lỗi hay gặp; thanh nút đáy chỉ ở cột phải.

## 4. Luồng thao tác

- `CameraScreen`, nút thư viện, sang `PosesScreen`; quay lại về `CameraScreen`.
- `PosesScreen`, chạm thẻ, sang `PoseDetailScreen`; quay lại về `PosesScreen` giữ chip và vị trí cuộn.
- `PoseDetailScreen`, "Chụp với tư thế này", sang `CameraScreen` với tư thế đó.
- `PosesScreen`, nút cài đặt, sang `SettingsScreen`.
- Đã lưu rỗng, "Xem tất cả tư thế", chuyển về chip "Tất cả".

## 5. Vị trí quảng cáo

- `poses_banner`: banner neo đáy `PosesScreen`; lưới cuộn phía trên (ảnh `PosesScreen_ad_*`).
- `poses_detail_banner`: banner ở đáy `PoseDetailScreen`, dưới thanh nút chính; nút không bị che (ảnh `PoseDetailScreen_ad_*`).
- Cờ tắt hoặc chưa có quảng cáo thì không chừa khoảng trống (ảnh `default`). Vùng quảng cáo tách khỏi nút "Chụp với tư thế này" bằng đường kẻ và nền khác màu.

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục, thứ tự thành phần, chữ (mục 2), vai trò màu và kiểu chữ (mục 3).
- **Được phép khác:** ảnh tư thế (ảnh tạm trên thiết kế), cách vẽ chữ và bóng của hệ thống, hiệu ứng chuyển động, chênh lệch nhỏ theo lưới 4 dp, chiều cao thẻ theo tỷ lệ ảnh thật.
- **Màn hình thấp (360 x 640):** lưới cuộn được; ở chi tiết nội dung cuộn được, thanh nút đáy luôn thấy được, không che chữ cuối; banner không đẩy nút chính ra ngoài màn hình.

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
