---
feature: loading
status: approved                # draft | approved
spec: features/loading/spec.md
tool: figma
link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=25-2036
designer: AI (Claude), Nhật
approved_by: Nhật
approved_at: 2026-10-21
---

# THIẾT KẾ: MÀN LOADING

Thiết kế của Camera Pose theo hướng Sticker, thay thiết kế cũ của dự án mẫu (xanh lục, Inter, ngày 2026-10-16). Màu, chữ, thành phần chung: [docs/design-system/sticker.md](../../../design-system/sticker.md).

## 1. Các màn hình và trạng thái

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Kịch bản |
| --- | --- | --- | --- |
| `LoadingScreen` | Đang tải, 68% | `LoadingScreen_progress_light` | KB-1 |
| `LoadingScreen` | Đang tải, 68%, chế độ tối | `LoadingScreen_progress_dark` | KB-1 |
| `LoadingScreen` | Lỗi, dừng ở 68% | `LoadingScreen_failed_light` | KB-2, KB-3 |
| `LoadingScreen` | Lỗi, dừng ở 68%, chế độ tối | `LoadingScreen_failed_dark` | KB-2, KB-3 |

## 2. Bảng chữ

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| Mọi trạng thái | Tên app | Camera Pose: Lines and Guides (`app_name`), xuống hai dòng, căn giữa | `headlineLarge` |
| Mọi trạng thái | Khẩu hiệu | Tạo dáng theo nét viền, chụp đẹp ngay. | `bodyMedium` |
| Đang tải | Nhãn tiến trình | Đang tải dữ liệu | `labelLarge` |
| Mọi trạng thái | Phần trăm | 68% (theo tiến độ, `%1$d%%`) | `bodyMedium` |
| Đang tải | Dòng hướng dẫn | Đang chuẩn bị bộ tư thế… | `bodySmall` |
| Lỗi | Nhãn tiến trình | Tiến độ đã dừng | `labelLarge` |
| Lỗi | Tiêu đề lỗi | Không thể tải dữ liệu | `titleMedium` |
| Lỗi, máy không có mạng hay mạng chưa được hệ thống xác nhận | Lời hướng dẫn | Kết nối đang gián đoạn. Vui lòng kiểm tra mạng và thử lại để tiếp tục. | `bodyMedium` |
| Lỗi, máy vào được Internet (T-057, spec câu hỏi 11 đã xác nhận 2026-10-26) | Lời hướng dẫn | Máy chủ chưa phản hồi. Vui lòng thử lại sau ít phút. Cùng bố cục với ảnh `LoadingScreen_failed_*`, không có frame riêng | `bodyMedium` |
| Lỗi | Nút | Thử lại | chữ của nút chính |
| Mọi trạng thái | Chân trang | Ảnh chỉ lưu trên máy của bạn | `labelSmall` |

Dòng hướng dẫn dùng dấu ba chấm một ký tự (`…`).

## 3. Ánh xạ sang hệ thống thiết kế

| Phần tử trên thiết kế | Thành phần trong code | Màu (vai trò) | Kiểu chữ | Khoảng cách, kích thước |
| --- | --- | --- | --- | --- |
| Logo | `BrandLogo` (logo tạm) | `secondaryContainer`, `outline`, `onSecondaryContainer`, bóng `shadow/sticker` | | Ô 112, bo `sheet`; cách tên app 24 (`Spacing.lg`) |
| Tên app, khẩu hiệu | `Text`, căn giữa | `onBackground`, `onSurfaceVariant` | `headlineLarge`, `bodyMedium` | Cách nhau `Spacing.sm`; khẩu hiệu cách nhãn tiến trình 40 (`Spacing.xl + Spacing.sm`) |
| Nhãn và phần trăm | `Row`, hai đầu | Đang tải: `onSurfaceVariant`, `onSurface`. Lỗi: `error` cả hai | `labelLarge`, `bodyMedium` | |
| Thanh tiến trình | `ProgressBar` | Đang tải: `secondaryContainer` trên `surfaceVariant`. Lỗi: `error` trên `errorContainer`. Viền `outline` 1 | | Cao 8, bo `full`; cách nhãn `Spacing.md` |
| Dòng hướng dẫn khi đang tải | `Text`, căn giữa | `onSurfaceVariant` | `bodySmall` | Cách thanh `Spacing.lg` |
| Khối lỗi | `Column`, căn giữa | | | Cách thanh `Spacing.lg`; các phần cách nhau `Spacing.sm` |
| Biểu tượng cảnh báo và tiêu đề lỗi | `Row`, icon `warning` 20 | `error`, `onSurface` | `titleMedium` | Cách nhau `Spacing.sm` |
| Nút "Thử lại" | `PrimaryButton` có icon `refresh` | `secondary`, `onSecondary`, bóng `shadow/sticker` | `titleMedium` | Rộng hết vùng nội dung, cao 56 |
| Chân trang | `Row`, icon `lock` 14 và `Text` | `onSurfaceVariant` | `labelSmall` | Cách đáy `Spacing.xl` |
| Vùng nội dung | | | | Rộng 318, căn giữa (lề hai bên khoảng 46) |

- **Thành phần mới cần thêm vào hệ thống thiết kế:** không có; `BrandLogo` và `ProgressBar` đổi kiểu theo hướng Sticker trong plan đổi `:core:designsystem`.

**Vị trí theo chiều dọc.** Logo bắt đầu ở khoảng 200 trên 891; khối lỗi hiện thêm bên dưới thanh tiến trình, không đẩy logo lên. Chân trang bám đáy.

## 4. Luồng thao tác

`LoadingScreen` chuẩn bị xong, sang `OnboardingScreen` (lần mở đầu tiên) hoặc `CameraScreen`. Trạng thái lỗi, bấm "Thử lại", làm tiếp các phần việc chưa xong.

## 5. Vị trí quảng cáo

Không có.

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục, thứ tự thành phần, chữ (mục 2), vai trò màu và kiểu chữ (mục 3).
- **Được phép khác:** cách vẽ chữ và bóng của hệ thống, hiệu ứng chuyển động, chênh lệch nhỏ theo lưới 4 dp, hình logo (logo tạm).
- **Màn hình thấp (360 x 640):** nội dung cuộn được; khi lỗi, nút "Thử lại" luôn thấy được; chân trang nằm dưới nút, không chồng lên lời hướng dẫn (bài học L-028 của dự án mẫu).

## 7. Tiêu chí khi duyệt

- [ ] Dùng đúng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế.
- [ ] Đủ các trạng thái trong mục 1, kể cả chế độ tối.
- [ ] Chữ đúng bảng chữ, không bị cắt hay tràn; tên app dài xuống hai dòng gọn gàng.
- [ ] Bố cục và thứ tự thành phần giống bản mẫu; lệch về vị trí không quá một bước lưới 4 dp.
- [ ] Vùng chạm đủ lớn (ít nhất 48 dp), chữ dễ đọc.
- [ ] Dùng được ở màn hình thấp (360 x 640): nội dung cuộn được, chữ không chồng nhau, nút chính không bị che hay cắt.
- [ ] Giao diện và nội dung không được thiết kế để thu hút chủ yếu trẻ em.

## 8. Kết quả đối chiếu

Đối chiếu bằng `scripts/design-compare.sh loading` ngày 2026-10-22 (T-008, RUN-20261022-01), ảnh chuẩn chụp bằng tiếng Việt, tên app thật, tiến độ 68%:

| Màn hình | Điểm ảnh khác | Ảnh so sánh (thiết kế, thật, khác biệt) |
| --- | --- | --- |
| LoadingScreen_failed_dark | 2.7% | `LoadingScreen_failed_dark_compare.png` |
| LoadingScreen_failed_light | 2.7% | `LoadingScreen_failed_light_compare.png` |
| LoadingScreen_progress_dark | 1.6% | `LoadingScreen_progress_dark_compare.png` |
| LoadingScreen_progress_light | 1.6% | `LoadingScreen_progress_light_compare.png` |

Nhận xét của rà soát độc lập (mục 7):

- `progress_light`, `progress_dark`: đủ các phần, đúng thứ tự, chữ đúng bảng chữ, đúng vai trò màu (nền `background`, nhãn `onSurfaceVariant`, phần trăm `onSurface`, thanh vàng trên `surfaceVariant`, viền `outline`). Khác biệt chỉ ở logo tạm, nét chữ và đầu thanh tiến trình lệch khoảng 1 dp. Không có lệch chặn.
- `failed_light`, `failed_dark`: nhãn, phần trăm và thanh màu `error` trên `errorContainer`; biểu tượng `warning` tam giác, tiêu đề `onSurface`, lời hướng dẫn hai dòng, nút "Thử lại" có `refresh`; logo không đổi chỗ. Ngoài logo và nét chữ, khác biệt lớn nhất là nút cao 56 so với 60 của thiết kế (T-007 đã chốt 56). Không có lệch chặn.
- Ảnh khung 360 x 640 (`_small`): tên app hai dòng, nút "Thử lại" thấy trọn, chân trang dưới nút.

Logo vẫn là `brand_mark.xml` tạm (plan T-008, câu 8), thay khi có icon app. `handoff/` giữ lại làm lịch sử của thiết kế cũ.
