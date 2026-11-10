---
id: T-092
title: Trang 3 của luồng giới thiệu chờ quảng cáo cho trang 4, tối đa 3 giây
feature: onboarding
spec: features/onboarding/spec.md   # confirmed 2026-11-09, câu 12
scenarios: [KB-6]
owner: Nhật
status: planned
risk: high
size: M
depends_on: [T-091]          # dùng trang GUIDE, chữ và bố cục mà T-091 làm
sprint:
plan: features/onboarding/plans/T-092-trang-3-cho-quang-cao-trang-4.md
auto_run:
created: 2026-11-09
---

# T-092: TRANG 3 CHỜ QUẢNG CÁO CHO TRANG 4, TỐI ĐA 3 GIÂY

## Mô tả

Ở trang 3 của luồng giới thiệu, khi quảng cáo được phép mà pool chưa có sẵn quảng cáo cho trang 4, nút "Tiếp" mờ đi tối đa 3 giây. Có quảng cáo thì nút dùng được ngay; hết giờ thì người dùng đi tiếp bình thường. Mục đích là trang 4 có thẻ native nhiều hơn khi người dùng bấm nhanh.

## Phạm vi

- `:core:ads` cho app hỏi pool `native_step` có sẵn quảng cáo cho bước kế tiếp không.
- `:feature:onboarding` giữ nút "Tiếp" ở trang 3 theo tín hiệu đó, một lần mỗi luồng, tối đa 3 giây.
- Screenshot test và ảnh chuẩn của 2 ảnh thiết kế `guide-ad-wait`.
- Kịch bản emulator chờ nút dùng được ở trang 3.

**Không bao gồm:**

- Đổi cỡ pool hay cách nạp bù.
- Đổi chờ 5 giây của trang 1.
- Giữ nút "Bật camera" ở trang cuối.

## Rủi ro sơ bộ

Chạm điểm chặn quảng cáo và module core dùng chung (`:core:ads`). Thay đổi trong `:core:ads` có ích cho mọi app dùng thẻ bước, nên đề xuất về dự án mẫu (`docs/ops/from-app.md`).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
