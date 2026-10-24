---
id: T-042
title: run-check.py báo plan phụ thuộc file của plan khác
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-060 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: approved
risk: high
size: S
depends_on: []
sprint:
plan: features/quality-gate/plans/T-042-run-check-phu-thuoc-file.md
created: 2026-10-24
---

# T-042: RUN-CHECK.PY BÁO PLAN PHỤ THUỘC FILE CỦA PLAN KHÁC

## Mô tả

Điểm chặn T-017, T-020 trước RUN-20261023-03 chỉ được rà soát độc lập thấy: plan sau giả định file mà plan trước tạo (L-060). Sau ticket này, `scripts/run-check.py` đọc mục 3.2 của mọi plan trong danh sách: plan nhắc tới file mà plan khác ghi Tạo thì phải xếp sau plan đó (lỗi), và in, cho mỗi cặp, file nào plan sau sẽ phải sửa thêm nếu plan trước dừng.

## Phạm vi

- `scripts/run-check.py` và test trong `scripts/tests/run.sh`.
- `docs/ops/pre-run-review.md`: dòng mới cho kiểm tra này.

**Không bao gồm:**

- Tự sắp lại thứ tự danh sách chạy.
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: kiểm tra đầu phiên chạy script này (điểm chặn 7, quy trình phiên tự chạy); plan chấm lại từ mức trung bình sơ bộ.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
