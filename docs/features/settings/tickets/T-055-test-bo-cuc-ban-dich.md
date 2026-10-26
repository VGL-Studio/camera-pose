---
id: T-055
title: Bố cục và nghĩa của bản dịch ở bảng chỉnh nét viền và nút chụp; "Độ mờ" thành "Độ đậm"
feature: settings
spec: features/settings/spec.md
scenarios: [KB-4]
owner: Nhật
status: planned
risk: medium
size: S
depends_on: []
sprint:
plan: features/settings/plans/T-055-test-bo-cuc-ban-dich.md
created: 2026-10-26
---

# T-055: BỐ CỤC VÀ NGHĨA CỦA BẢN DỊCH Ở BẢNG CHỈNH NÉT VIỀN VÀ NÚT CHỤP; "ĐỘ MỜ" THÀNH "ĐỘ ĐẬM"

## Mô tả

Rà soát độc lập của T-050 và T-051 (RUN-20261026-01) để lại bốn ghi chú về bản dịch, Tuấn chọn làm ngày 2026-10-26:

1. Test bố cục bảng chỉnh nét viền của camera chỉ chạy ở `pt-BR`, trong khi nhãn `es-419` "Restablecer" và `id` "Sembunyikan" dài hơn.
2. Test `opacityLabel_isNotTransparency` dò từ "trong suốt" theo cả từ, nên ở tiếng Thái (không có dấu cách giữa các từ) không bắt được cụm "ความโปร่งใส" ghép sau chữ khác.
3. Test nút chính tiếng Bồ Đào Nha của màn chi tiết tư thế chỉ kiểm nút nằm trong màn, không kiểm chữ có xuống dòng.
4. Nhãn con trượt tiếng Việt "Độ mờ" đọc ngược nghĩa: 100% là nét viền đậm nhất. Tuấn chọn đổi thành "Độ đậm", sửa cả Figma.

## Phạm vi

- Test bố cục bảng chỉnh ở `es-419`, `id`, `hi`, `th` trên khung thấp, cùng cách đo với test `pt-BR` của T-050.
- Test nhãn con trượt chặn từ ngược nghĩa ở mọi ngôn ngữ, gồm tiếng Thái ở bất kỳ vị trí nào và tiếng Việt "mờ", "trong suốt".
- Test nút chính tiếng Bồ Đào Nha một dòng trên khung thấp.
- `values-vi` của camera: "Độ mờ" thành "Độ đậm"; ảnh chuẩn có bảng chỉnh ở tiếng Việt ghi lại.
- Sửa bản dịch nào test mới bắt được lỗi (chạy thử đã thấy `id` "Sembunyikan" đè lên "Kunci").

**Không bao gồm:**

- Sửa Figma, ảnh thiết kế, `design/context/`, bảng chữ trong `design/README.md` và câu chữ của spec camera (KB-3): phiên có người theo dõi làm trong một PR riêng trước phiên.
- Duyệt bản dịch bởi người biết tiếng (spec onboarding, câu hỏi 1).
- Bố cục của `AdjustAction` trong `:core:designsystem`.

## Rủi ro sơ bộ

Chỉ đổi hai chuỗi và test của hai module feature, cùng hai ảnh chuẩn (plan chấm 5, trung bình, vì chạm hai module). Phụ thuộc PR thiết kế: chưa merge thì ảnh so sánh thiết kế lệch chữ và spec camera còn ghi "Độ mờ".

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
