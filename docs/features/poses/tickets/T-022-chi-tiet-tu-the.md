---
id: T-022
title: Chi tiết tư thế và "Chụp với tư thế này"
feature: poses
spec: features/poses/spec.md
scenarios: [KB-3, KB-4, KB-5]
owner: Nhật
status: approved
risk: high
size: M
depends_on: [T-021]
sprint:
plan: features/poses/plans/T-022-chi-tiet-tu-the.md
created: 2026-10-21
---

# T-022: CHI TIẾT TƯ THẾ VÀ "CHỤP VỚI TƯ THẾ NÀY"

## Mô tả

Chạm thẻ mở chi tiết: ảnh lớn, dòng phụ, các bước, lỗi hay gặp, nút lưu, nút "Chụp với tư thế này" ghim ở đáy đưa về camera với tư thế đó.

## Phạm vi

- Ảnh lớn đang tải và lỗi có "Thử lại"; nội dung cuộn, nút luôn thấy được.
- Sự kiện `pose_detail_opened`, `pose_selected` (`source`: `detail`).

**Không bao gồm:**

- Banner chi tiết (T-023).

## Rủi ro sơ bộ

Điểm chặn 4 (sự kiện phân tích): rủi ro cao.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
