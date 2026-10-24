---
id: T-045
title: Cuối phiên soạn sẵn plan cho ticket kế tiếp
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-061 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: on-staging
risk: medium
size: M
depends_on: []
sprint:
plan: features/quality-gate/plans/T-045-hang-cho-plan.md
created: 2026-10-24
---

# T-045: CUỐI PHIÊN SOẠN SẴN PLAN CHO TICKET KẾ TIẾP

## Mô tả

Sau mỗi phiên, phải một phiên có người theo dõi để viết plan, trả lời câu hỏi, rà soát rồi mới lập được danh sách kế tiếp; thời gian giữa hai phiên chủ yếu là chờ plan (L-061). Sau ticket này, khi còn hạn mức sau báo cáo, phiên soạn plan `draft` cho các ticket `ready` kế tiếp (theo thứ tự ưu tiên của backlog), chạy thử mục 4.1, chạy `plan-check.py`, và gom mọi câu hỏi cần người vào một mục mới của báo cáo phiên; người chỉ trả lời và duyệt.

## Phạm vi

- `docs/auto-run.md` mục 4 (Kết thúc phiên): bước soạn plan, giới hạn số plan và thời gian.
- `docs/templates/run.md`: mục "Plan soạn sẵn và câu hỏi cho người".
- Không tự duyệt plan, không lập danh sách chạy.

**Không bao gồm:**

- Soạn spec hay tách ticket.
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Trung bình: quy trình phiên tự chạy, không đổi script hay CI.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
