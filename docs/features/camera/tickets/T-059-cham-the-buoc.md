---
id: T-059
title: Chạm thẻ bước để thu gọn
feature: camera
spec: features/camera/spec.md
scenarios: [KB-2]
owner: Nhật
status: approved
risk: low
size: S
depends_on: [T-014]
sprint:
plan: features/camera/plans/T-059-cham-the-buoc.md
created: 2026-10-26
---

# T-059: CHẠM THẺ BƯỚC ĐỂ THU GỌN

## Mô tả

Thẻ bước che khung xem trước và chỉ ẩn được bằng cách vuốt lên, không có dấu hiệu nào trên màn (FB-20261026-02). Sau task này, chạm vào thẻ ở bất kỳ chỗ nào ngoài hai mũi tên cũng ẩn thẻ, như vuốt lên; nút "Bước" hiện lại thẻ như cũ (spec KB-2).

## Phạm vi

- `StepCard` nhận chạm ở cả thẻ trừ vùng hai mũi tên; vuốt lên giữ nguyên; trạng thái ẩn vẫn lưu như KB-2.
- Mô tả cho TalkBack: thẻ có hành động "Ẩn thẻ bước" là hành động chạm chính.
- Test giao diện cho chạm thẻ, chạm mũi tên không ẩn thẻ, chạm ở bố cục mở gập.

**Không bao gồm:** nút thu gọn riêng, đổi hình dáng thẻ.

## Rủi ro sơ bộ

Thấp: chỉ đổi thao tác của một thành phần trên camera. Chạm thẻ không được đi xuống nét viền (kéo nét viền nằm ngay dưới thẻ).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
