---
feature: network
status: approved                # draft | approved
spec: features/network/spec.md
tool: figma
link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=29-2169
designer: AI (Claude), Nhật
approved_by: Nhật
approved_at: 2026-10-21
---

# THIẾT KẾ: HỘP THOẠI MẤT MẠNG

Màu, chữ, thành phần chung: [docs/design-system/sticker.md](../../../design-system/sticker.md).

Hộp thoại do vỏ app hiện phủ lên mọi màn (spec `network`, câu hỏi 1), không thuộc composable của màn nào. Vì vậy ảnh thiết kế chỉ có hộp thoại trên lớp mờ, đúng phần mà module chứa hộp thoại sẽ chụp screenshot test. Section Network trên Figma còn ba frame ví dụ hộp thoại phủ lên camera và thư viện; chúng chỉ để người xem hình dung, không xuất và không có screenshot test.

## 1. Các màn hình và trạng thái

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Kịch bản |
| --- | --- | --- | --- |
| `NetworkDialogScreen` (hộp thoại trên lớp mờ) | Bình thường | `NetworkDialogScreen_default_light` | KB-1, KB-2 |
| `NetworkDialogScreen` | Bình thường, chế độ tối | `NetworkDialogScreen_default_dark` | KB-1, KB-2 |

Trên camera và màn xem lại (luôn tối), hộp thoại hiện ở chế độ tối.

## 2. Bảng chữ

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| Bình thường | Tiêu đề | Mất kết nối mạng | `titleLarge` |
| Bình thường | Lời hướng dẫn | App cần mạng để hoạt động. Kiểm tra kết nối Internet rồi thử lại. | `bodyMedium` |
| Bình thường | Nút | Thử lại | chữ của nút chính |
| Thử lại, vẫn chưa có mạng | Dòng dưới nút | Vẫn chưa có mạng | `bodySmall` |

Dòng "Vẫn chưa có mạng" theo câu trả lời câu hỏi 3 của spec; không có trên ảnh thiết kế, đặt dưới nút, màu `onSurfaceVariant`, căn giữa.

## 3. Ánh xạ sang hệ thống thiết kế

| Phần tử trên thiết kế | Thành phần trong code | Màu (vai trò) | Kiểu chữ | Khoảng cách, kích thước |
| --- | --- | --- | --- | --- |
| Lớp mờ | | `camera/scrim`, độ mờ 50% | | Phủ cả màn hình |
| Hộp thoại | `NetworkDialog` (mới) | `surface`, viền `outline` 2 | | Rộng 331, căn giữa, bo `sheet`, lề `Spacing.lg`, các phần cách nhau `Spacing.md` |
| Vòng biểu tượng | icon `wifi_off` 32 trong vòng 64 | `secondaryContainer`, `onSecondaryContainer`, viền `outline` | | |
| Tiêu đề, lời hướng dẫn | `Text`, căn giữa | `onSurface`, `onSurfaceVariant` | `titleLarge`, `bodyMedium` | |
| Nút "Thử lại" | `PrimaryButton` có icon `refresh` | `secondary`, `onSecondary`, bóng `shadow/sticker` | `titleMedium` | Rộng hết hộp thoại; khi đang kiểm tra thì hiện vòng chờ |

- **Thành phần mới cần thêm vào hệ thống thiết kế:** `NetworkDialog`.

## 4. Luồng thao tác

Mất mạng ở bất kỳ màn nào sau màn loading, hộp thoại hiện. Bấm "Thử lại": có mạng thì hộp thoại đóng, màn hình tải lại phần dở; chưa có mạng thì hộp thoại vẫn hiện. Nút quay lại của máy đưa app ra nền.

## 5. Vị trí quảng cáo

Không có. Khi hộp thoại đang hiện thì không hiện quảng cáo toàn màn hình.

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục, thứ tự thành phần, chữ (mục 2), vai trò màu và kiểu chữ (mục 3).
- **Được phép khác:** hiệu ứng mở hộp thoại, chênh lệch nhỏ theo lưới 4 dp.
- **Màn hình thấp (360 x 640):** hộp thoại nằm giữa, không bị cắt; chữ dài xuống dòng.

## 7. Tiêu chí khi duyệt

- [ ] Dùng đúng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế.
- [ ] Đủ các trạng thái trong mục 1, kể cả chế độ tối.
- [ ] Chữ đúng bảng chữ, không bị cắt hay tràn.
- [ ] Vùng chạm đủ lớn (ít nhất 48 dp), chữ dễ đọc.
- [ ] Dùng được ở màn hình thấp (360 x 640).
- [ ] Giao diện và nội dung không được thiết kế để thu hút chủ yếu trẻ em.

## 8. Kết quả đối chiếu

Chưa có.
