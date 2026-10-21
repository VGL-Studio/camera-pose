---
id: T-002
title: Hệ thống thiết kế theo thương hiệu mới (xanh lục, Inter, logo)
feature: loading
spec: features/loading/spec.md
scenarios: [KB-1, KB-2]
owner: Nhật
status: on-staging
risk: high
size: L
depends_on: []
sprint:
plan: features/loading/plans/T-002-thuong-hieu-xanh-luc.md
created: 2026-10-16
---

# T-002: HỆ THỐNG THIẾT KẾ THEO THƯƠNG HIỆU MỚI (XANH LỤC, INTER, LOGO)

## Mô tả

Thiết kế màn hình loading (`features/loading/design/`) dùng màu thương hiệu xanh lục, phông Inter và logo, chưa có trong hệ thống thiết kế. Ticket này đổi `:core:designsystem` theo thiết kế đó, để mọi màn hình (màn hình mẫu, loading, các app tạo sau) dùng chung. Sau khi xong, màn hình mẫu và màn hình loading hiện tại đổi màu và phông, chưa đổi bố cục.

## Phạm vi

- Bảng màu sáng và tối xanh lục, đủ các vai trò trong `tokens.json`, thêm `errorContainer`.
- Phông Inter (Regular, Medium, SemiBold) cho mọi kiểu chữ; `titleMedium` đậm 600.
- `PrimaryButton`: cao 52, bo góc 16, chữ đậm 600, có thể có biểu tượng.
- Thành phần mới: `BrandLogo`, `ProgressBar`. Khẩu hiệu mặc định của thương hiệu.
- Catalog, `tokens.json`, ảnh chuẩn screenshot của catalog, màn hình mẫu và màn hình loading.
- Hướng dẫn tạo app mới: những gì của thương hiệu phải thay.

**Không bao gồm:**

- Bố cục mới của màn hình loading (T-003).

## Rủi ro sơ bộ

Chạm điểm chặn 7 (module core dùng chung). Đổi giao diện của mọi màn hình đang có; thêm phông chữ (tài nguyên, không phải SDK). Rủi ro cao, cần duyệt chéo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
