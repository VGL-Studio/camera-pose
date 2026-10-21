---
id: T-008
title: Màn loading theo thiết kế Sticker và chữ của app
feature: loading
spec: features/loading/spec.md
scenarios: [KB-1, KB-2]
owner: Nhật
status: ready
risk: medium
size: M
depends_on: [T-007]
sprint:
plan:
created: 2026-10-21
---

# T-008: MÀN LOADING THEO THIẾT KẾ STICKER VÀ CHỮ CỦA APP

## Mô tả

Màn loading hiện đúng thiết kế Sticker của Camera Pose: logo tạm, tên app, khẩu hiệu, dòng hướng dẫn và chân trang mới, ở cả trạng thái đang tải và lỗi.

## Phạm vi

- Bố cục, chữ, màu theo `features/loading/design/README.md`.
- Chữ mới trong `strings.xml`, đủ 7 ngôn ngữ phát hành (spec `onboarding`, câu hỏi 1).
- Screenshot test 4 ảnh thiết kế và ảnh màn hình thấp.

**Không bao gồm:**

- Đổi các phần việc tải (T-010).

## Rủi ro sơ bộ

Chỉ đổi giao diện trong `:feature:loading`, phụ thuộc T-007.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
