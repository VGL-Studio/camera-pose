---
feature: settings
status: approved                # draft | approved
spec: features/settings/spec.md
tool: figma
link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=20-670
designer: AI (Claude), Nhật
approved_by: Nhật
approved_at: 2026-10-21
---

# THIẾT KẾ: CÀI ĐẶT

Màu, chữ, thành phần chung: [docs/design-system/sticker.md](../../../design-system/sticker.md).

## 1. Các màn hình và trạng thái

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Kịch bản |
| --- | --- | --- | --- |
| `SettingsScreen` | Bình thường | `SettingsScreen_default_light` | KB-1, KB-3, KB-5, KB-6 |
| `SettingsScreen` | Bình thường, chế độ tối | `SettingsScreen_default_dark` | KB-1, KB-3, KB-5, KB-6 |
| `SettingsScreen` | Hộp chọn giao diện đang mở | `SettingsScreen_theme-picker_light` | KB-2 |
| `SettingsScreen` | Hộp chọn giao diện, chế độ tối | `SettingsScreen_theme-picker_dark` | KB-2 |

Hộp chọn ngôn ngữ (máy dưới Android 13, KB-4) dùng cùng thành phần `ChoiceDialog`, tiêu đề "Ngôn ngữ", các lựa chọn "Theo hệ thống" và 7 ngôn ngữ; không có ảnh riêng.

## 2. Bảng chữ

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| Mọi trạng thái | Tiêu đề | Cài đặt | `titleLarge` |
| Mọi trạng thái | Nhóm 1 | Chung | `labelLarge` |
| Mọi trạng thái | Dòng | Giao diện, giá trị "Theo hệ thống" (hoặc "Sáng", "Tối") | `bodyLarge`, `bodyMedium` |
| Mọi trạng thái | Dòng | Âm khi chụp | `bodyLarge` |
| Mọi trạng thái | Dòng | Ngôn ngữ, giá trị là tên ngôn ngữ đang dùng viết bằng chính ngôn ngữ đó ("Tiếng Việt") | `bodyLarge`, `bodyMedium` |
| Mọi trạng thái | Nhóm 2 | Quyền riêng tư | `labelLarge` |
| Mọi trạng thái | Dòng | Tùy chọn quyền riêng tư | `bodyLarge` |
| Mọi trạng thái | Dòng | Chính sách quyền riêng tư | `bodyLarge` |
| Mọi trạng thái | Nhóm 3 | Thông tin | `labelLarge` |
| Mọi trạng thái | Dòng | Phiên bản, giá trị là số phiên bản ("1.0.0") | `bodyLarge`, `bodyMedium` |
| Hộp chọn giao diện | Tiêu đề | Giao diện | `titleLarge` |
| Hộp chọn giao diện | Lựa chọn | Theo hệ thống; Sáng; Tối | `bodyLarge` |
| Hộp chọn giao diện | Nút | Hủy | `labelLarge` |

## 3. Ánh xạ sang hệ thống thiết kế

| Phần tử trên thiết kế | Thành phần trong code | Màu (vai trò) | Kiểu chữ | Khoảng cách, kích thước |
| --- | --- | --- | --- | --- |
| Thanh trên | `TopAppBar`, nút quay lại | `onSurface` trên `background` | `titleLarge` | Cao 64 |
| Tên nhóm | `SectionHeader` | `onSurfaceVariant` | `labelLarge` | Lề trái `Spacing.md`, trên `Spacing.lg`, dưới `Spacing.sm` |
| Dòng có giá trị | `SettingsRow`, kiểu `Value` | Icon `onSurfaceVariant`; chữ `onSurface`, giá trị `onSurfaceVariant`; mũi tên `chevron_right` | `bodyLarge`, `bodyMedium` | Cao ít nhất 64, lề `Spacing.md`, icon cách chữ `Spacing.md` |
| Dòng có công tắc | `SettingsRow`, kiểu `Switch` | Công tắc bật `primary`, viền `outline` | `bodyLarge` | |
| Dòng mở trang ngoài | `SettingsRow`, kiểu `Link`, icon `open_in_new` | | `bodyLarge` | |
| Dòng "Phiên bản" | `SettingsRow`, kiểu `Info`: không mũi tên, không bấm được | | `bodyLarge`, `bodyMedium` | |
| Icon đầu dòng | `palette`, `volume_up`, `language`, `shield_person`, `policy`, `info` | `onSurfaceVariant` | | 24 |
| Hộp chọn | `ChoiceDialog` với `RadioRow` | `surface`, viền `outline` 2; nút chọn đang chọn `secondaryContainer` | `titleLarge`, `bodyLarge` | Rộng 331, bo `sheet`, lề `Spacing.lg`; mỗi lựa chọn cao 48 |
| Lớp mờ dưới hộp chọn | | `camera/scrim`, độ mờ 50% | | Phủ cả màn hình |

- **Thành phần mới cần thêm vào hệ thống thiết kế:** `SettingsRow` (bốn kiểu), `SectionHeader`.

## 4. Luồng thao tác

`CameraScreen` hoặc `PosesScreen`, bấm bánh răng, sang `SettingsScreen`. Bấm "Giao diện", hộp chọn mở; chọn một lựa chọn thì áp dụng và đóng; "Hủy" đóng không đổi. "Tùy chọn quyền riêng tư" mở hộp thoại đồng ý của `:core:ads`. "Chính sách quyền riêng tư" mở trình duyệt. Nút quay lại về màn trước.

## 5. Vị trí quảng cáo

Không có.

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục, thứ tự thành phần, chữ (mục 2), vai trò màu và kiểu chữ (mục 3).
- **Được phép khác:** cách vẽ chữ và bóng của hệ thống, hiệu ứng mở hộp thoại, chênh lệch nhỏ theo lưới 4 dp.
- **Màn hình thấp (360 x 640):** danh sách cuộn được; hộp chọn nằm giữa, không bị cắt.

## 7. Tiêu chí khi duyệt

- [ ] Dùng đúng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế.
- [ ] Đủ các trạng thái trong mục 1, kể cả chế độ tối.
- [ ] Chữ đúng bảng chữ, không bị cắt hay tràn.
- [ ] Bố cục và thứ tự thành phần giống bản mẫu; lệch về vị trí không quá một bước lưới 4 dp.
- [ ] Vùng chạm đủ lớn (ít nhất 48 dp), chữ dễ đọc.
- [ ] Dùng được ở màn hình thấp (360 x 640): nội dung cuộn được, chữ không chồng nhau.
- [ ] Giao diện và nội dung không được thiết kế để thu hút chủ yếu trẻ em.

## 8. Kết quả đối chiếu

Chưa có.
