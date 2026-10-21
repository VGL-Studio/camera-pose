---
id: T-007
title: Hệ thống thiết kế theo hướng Sticker
feature: loading
spec: features/loading/spec.md
scenarios: []
owner: Nhật
status: planned
risk: high
size: L
depends_on: []
sprint:
plan: features/loading/plans/T-007-he-thong-thiet-ke-sticker.md
created: 2026-10-21
---

# T-007: HỆ THỐNG THIẾT KẾ THEO HƯỚNG STICKER

## Mô tả

Đổi `:core:designsystem` từ hệ thống của dự án mẫu (Inter, xanh lục) sang hướng Sticker đã duyệt. Sau ticket này mọi màn hình dùng màu, phông, kiểu chữ, bo góc và viền mới; ảnh catalog và `tokens.json` sinh lại theo.

## Phạm vi

- Bảng màu sáng và tối theo `docs/design-system/sticker.md`, mục 1, gồm màu riêng của app (`success*`, màu nhóm, màu camera).
- Phông Bricolage Grotesque (OFL) trong `res/font`, kiểu chữ theo mục 2 của `sticker.md`.
- Bo góc, độ dày viền, bóng đổ cứng `shadow/sticker` thành giá trị dùng chung (mục 3).
- Đổi kiểu `PrimaryButton`, `ProgressBar`, `BrandLogo` (logo tạm); thêm `SecondaryButton`.
- Sinh lại `docs/design-system/tokens.json`, ghi lại ảnh catalog; xuất lại `context/` của các gói thiết kế bằng `scripts/figma-export.py` (phiên có người theo dõi).

**Không bao gồm:**

- Thành phần riêng của từng màn (chip, ô tư thế, thẻ tư thế, nút camera…): làm ở ticket của màn đó.
- Giao diện màn loading theo thiết kế mới (T-008).

## Rủi ro sơ bộ

Điểm chặn 7 (module core dùng chung): rủi ro cao, cần duyệt chéo. Mọi screenshot test hiện có đổi ảnh chuẩn; plan phải liệt kê đủ (L-020).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
