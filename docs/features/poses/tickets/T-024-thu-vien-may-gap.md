---
id: T-024
title: Thư viện và chi tiết tư thế trên máy gập
feature: poses
spec: features/poses/spec.md
scenarios: [KB-7]
owner: Nhật
status: ready
risk: medium
size: S
depends_on: [T-021, T-022]
sprint:
plan: features/poses/plans/T-024-thu-vien-may-gap.md
created: 2026-10-21
---

# T-024: THƯ VIỆN VÀ CHI TIẾT TƯ THẾ TRÊN MÁY GẬP

## Mô tả

Khi máy gập mở ra, thư viện hiện 3 cột; chi tiết chia hai cột ảnh và hướng dẫn, nút ở đáy cột phải.

## Phạm vi

- Bố cục theo ảnh `_unfolded`; giữ chip, vị trí cuộn khi gập mở.
- Screenshot test khung `w673dp-h841dp-xxhdpi`.

**Không bao gồm:**

- Không có.

## Rủi ro sơ bộ

Chỉ trong `:feature:poses`.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
