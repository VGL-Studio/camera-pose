---
feature: <ten-feature>
status: draft                # draft | approved
spec: features/<ten-feature>/spec.md
tool: <claude-design | figma>
link: <đường dẫn tới dự án trên Claude Design hoặc file Figma, chỉ để người tham khảo>
designer: <chủ feature>
approved_by: <người phụ trách sản phẩm>
approved_at: YYYY-MM-DD
---

# THIẾT KẾ: <TÊN FEATURE>

<!--
Quy trình: docs/ops/design-workflow.md. Quyết định: ADR-0008, ADR-0020.
Thư mục features/<ten-feature>/design/ chứa:
- README.md này.
- screens/: ảnh PNG 1233 x 2673 (khung 411 x 891, xuất 3x), tên <Ten>Screen_<trạng thái>_<light|dark>.png;
  app hỗ trợ điện thoại gập thêm ảnh 2019 x 2523 (khung 673 x 841) tên <Ten>Screen_<trạng thái>_<light|dark>_unfolded.png,
  trùng tên ảnh chuẩn screenshot mà plan sẽ tạo.
- context/: số liệu của từng màn hình (nguồn, biến dùng, cấu trúc và số liệu), cùng tên với ảnh, đuôi .md.
- review/: do scripts/design-compare.sh sinh ra, không sửa tay.
- handoff/ (tùy chọn): gói xuất từ công cụ, giữ nguyên như khi xuất.
Phiên tự chạy chỉ đọc thư mục này, không mở được Claude Design hay Figma.
Bản mẫu là ý đồ thiết kế. AI viết lại bằng Compose theo hệ thống thiết kế, không chép code web.
-->

## 1. Các màn hình và trạng thái

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Kịch bản |
| --- | --- | --- | --- |
| `<Ten>Screen` | Bình thường | `<Ten>Screen_default_light` | KB-1 |
| `<Ten>Screen` | Đang tải | `<Ten>Screen_loading_light` | KB-1 |
| `<Ten>Screen` | Lỗi | `<Ten>Screen_error_light` | KB-2 |
| `<Ten>Screen` | Bình thường, chế độ tối | `<Ten>Screen_default_dark` | KB-1 |

## 2. Bảng chữ

<!-- Mọi chữ hiển thị trên thiết kế, để test giao diện kiểm tra và để đưa vào strings.xml. -->

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| `<Ten>Screen`, bình thường | Tiêu đề | <chữ> | `titleLarge` |

## 3. Ánh xạ sang hệ thống thiết kế

<!-- Mỗi phần tử trên thiết kế dùng gì trong code. Tên lấy từ docs/design-system/tokens.json và :core:designsystem; số liệu lấy từ context/. Giá trị "ngoài hệ thống" trong context/ ghi rõ cách xử lý. -->

| Phần tử trên thiết kế | Thành phần trong code | Màu (vai trò) | Kiểu chữ | Khoảng cách |
| --- | --- | --- | --- | --- |
| Nút chính | `PrimaryButton` | `primary`, `onPrimary` | `labelLarge` | `Spacing.md` phía trên |

- **Thành phần mới cần thêm vào hệ thống thiết kế:** <tên, mô tả, hoặc "Không có">. Thêm thành phần mới là thay đổi module core, plan sẽ được xếp rủi ro cao.

## 4. Luồng thao tác

<Từ màn hình nào sang màn hình nào, bằng thao tác gì. Ví dụ: `<Ten>Screen`, bấm "Bắt đầu", sang `<Khac>Screen`.>

## 5. Vị trí quảng cáo

<Trùng với mục Quảng cáo trong spec. Đánh dấu vị trí trên ảnh màn hình, dùng component native của hệ thống thiết kế. Theo docs/ops/ads-guide.md, mục 4: vùng quảng cáo có ranh giới nhìn thấy được, cách phần tử bấm được của app ít nhất 16 dp, CTA khác nút chính của app, giữ chỗ khi đang tải. Ghi "Không có" nếu không có.>

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục, thứ tự thành phần, chữ (mục 2), vai trò màu và kiểu chữ (mục 3).
- **Được phép khác:** cách vẽ chữ và bóng của hệ thống, hiệu ứng chuyển động, chênh lệch nhỏ theo lưới 4 dp, <khác>.

## 7. Tiêu chí khi duyệt

Dùng khi duyệt thiết kế (bước 3) và khi rà soát ảnh so sánh `review/` (bước 6, 7 của quy trình):

- [ ] Dùng đúng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế.
- [ ] Đủ các trạng thái trong mục 1, kể cả chế độ tối.
- [ ] Chữ đúng bảng chữ, không bị cắt hay tràn.
- [ ] Bố cục và thứ tự thành phần giống bản mẫu; lệch về vị trí không quá một bước lưới 4 dp.
- [ ] Vùng chạm đủ lớn (ít nhất 48 dp), chữ dễ đọc.
- [ ] Dùng được ở màn hình thấp (360 x 640): nội dung cuộn được, chữ không chồng nhau, nút chính không bị che hay cắt.
- [ ] Vùng quảng cáo tách bạch, không gây bấm nhầm.
- [ ] Giao diện và nội dung không được thiết kế để thu hút chủ yếu trẻ em.

## 8. Kết quả đối chiếu

<!-- AI dán review/summary.md sau mỗi lần chạy scripts/design-compare.sh, kèm nhận xét của rà soát độc lập. -->
