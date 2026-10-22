---
feature: onboarding
status: approved                # draft | approved
spec: features/onboarding/spec.md
tool: figma
link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=18-128
designer: AI (Claude), Nhật
approved_by: Nhật
approved_at: 2026-10-21
---

# THIẾT KẾ: MÀN CHÀO VÀ XIN QUYỀN CAMERA

Màu, chữ, thành phần chung: [docs/design-system/sticker.md](../../../design-system/sticker.md).

## 1. Các màn hình và trạng thái

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Kịch bản |
| --- | --- | --- | --- |
| `OnboardingScreen` | Bình thường | `OnboardingScreen_default_light` | KB-1, KB-2 |
| `OnboardingScreen` | Bình thường, chế độ tối | `OnboardingScreen_default_dark` | KB-1, KB-2 |

## 2. Bảng chữ

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| Bình thường | Tiêu đề | Chụp dáng đẹp, không cần đoán | `headlineMedium` |
| Bình thường | Giới thiệu | Chọn một tư thế, đứng vào nét viền rồi bấm chụp. Ảnh lưu vào thư viện máy, không kèm nét viền. | `bodyLarge` |
| Bình thường | Điểm 1 | Hơn 200 tư thế: một người, cặp đôi, nhóm bạn | `bodyMedium` |
| Bình thường | Điểm 2 | Chỉnh độ mờ, cỡ và vị trí nét viền | `bodyMedium` |
| Bình thường | Điểm 3 | Lưu tư thế yêu thích để chụp lại sau | `bodyMedium` |
| Bình thường | Nút | Bật camera | chữ của nút chính |
| Bình thường | Ghi chú | App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn. | `bodySmall` |

## 3. Ánh xạ sang hệ thống thiết kế

| Phần tử trên thiết kế | Thành phần trong code | Màu (vai trò) | Kiểu chữ | Khoảng cách, kích thước |
| --- | --- | --- | --- | --- |
| Hình minh họa | Ba thẻ nghiêng (8°, 0°, -8°) có nét viền tư thế, huy hiệu camera | `category/women`, `category/couple`, `category/friends`, `category/onCategory`, `outline`; huy hiệu `secondaryContainer` | | Vùng cao 280, rộng hết; thẻ 150 x 200, bo `card`, viền 2 |
| Tiêu đề, giới thiệu | `Text` | `onBackground`, `onSurfaceVariant` | `headlineMedium`, `bodyLarge` | Các khối cách nhau `Spacing.lg` |
| Ba điểm chính | `Row`: vòng icon 40 và `Text` | Vòng `secondaryContainer`, viền `outline`; chữ `onSurface` | `bodyMedium` | Hàng cách nhau `Spacing.md`; icon `accessibility_new`, `tune`, `bookmark` 22 |
| Nút "Bật camera" | `PrimaryButton` có icon `photo_camera` | `secondary`, `onSecondary` | `titleMedium` | Rộng hết, ghim phía dưới |
| Ghi chú | `Text`, căn giữa | `onSurfaceVariant` | `bodySmall` | Dưới nút `Spacing.lg` |
| Lề màn hình | | | | Trái phải `Spacing.lg`, trên 56, dưới `Spacing.lg` |

- **Thành phần mới cần thêm vào hệ thống thiết kế:** không có ngoài các thành phần chung của hướng Sticker.

## 4. Luồng thao tác

`OnboardingScreen`, bấm "Bật camera", hộp hỏi quyền của hệ thống, sang `CameraScreen` (cho phép hay từ chối đều sang). Nút quay lại của máy đóng app.

## 5. Vị trí quảng cáo

Không có.

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục, thứ tự thành phần, chữ (mục 2), vai trò màu và kiểu chữ (mục 3).
- **Được phép khác:** cách vẽ chữ và bóng của hệ thống, chênh lệch nhỏ theo lưới 4 dp, góc nghiêng của thẻ minh họa.
- **Màn hình thấp (360 x 640):** phần trên (minh họa, chữ, ba điểm) cuộn được; nút "Bật camera" và ghi chú ghim ở đáy, luôn thấy được; hình minh họa co lại trước tiên.

## 7. Tiêu chí khi duyệt

- [ ] Dùng đúng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế.
- [ ] Đủ các trạng thái trong mục 1, kể cả chế độ tối.
- [ ] Chữ đúng bảng chữ, không bị cắt hay tràn.
- [ ] Bố cục và thứ tự thành phần giống bản mẫu; lệch về vị trí không quá một bước lưới 4 dp.
- [ ] Vùng chạm đủ lớn (ít nhất 48 dp), chữ dễ đọc.
- [ ] Dùng được ở màn hình thấp (360 x 640): nội dung cuộn được, chữ không chồng nhau, nút chính không bị che hay cắt.
- [ ] Giao diện và nội dung không được thiết kế để thu hút chủ yếu trẻ em.

## 8. Kết quả đối chiếu

Đối chiếu bằng `scripts/design-compare.sh onboarding` ngày 2026-10-22 (T-012, RUN-20261023-01), ảnh chuẩn chụp bằng tiếng Việt ở khung 411 x 891, trạng thái `Ready`:

| Màn hình | Điểm ảnh khác | Ảnh so sánh (thiết kế, thật, khác biệt) |
| --- | --- | --- |
| OnboardingScreen_default_dark | 4.3% | `OnboardingScreen_default_dark_compare.png` |
| OnboardingScreen_default_light | 4.3% | `OnboardingScreen_default_light_compare.png` |

Nhận xét của phiên viết code (rà soát độc lập bổ sung ở bước 6):

- `default_light`, `default_dark`: đủ các phần, đúng thứ tự, chữ đúng bảng chữ. Hình minh họa trùng vị trí với thiết kế: ba thẻ, góc nghiêng, nét viền tư thế (lấy từ `handoff/pose_outline.svg`) và huy hiệu camera. Viền thẻ, vòng icon và huy hiệu đổi màu theo chế độ (`outline`: đen ở chế độ sáng, trắng ở chế độ tối); nét viền tư thế luôn #111111 (`onCategory`).
- Tiêu đề vẫn hai dòng nhưng ngắt khác: "không" ở dòng một, "cần đoán" ở dòng hai (thiết kế: "không cần" ở dòng một). Ghi chú ngắt dòng sau "lưu trên" thay vì sau "lưu". Do cách đo chữ của hệ thống, chiều cao khối chữ không đổi.
- Nút cao 56 so với 60 của thiết kế (T-007 đã chốt 56), nên nút và ghi chú thấp hơn khoảng 2 đến 4 dp. Không có lệch chặn.
- Ảnh khung 360 x 640 (`_small`): hình minh họa co còn 160, nút "Bật camera" và ghi chú ghim ở đáy, phần chữ cuộn được.
