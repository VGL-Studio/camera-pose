---
id: T-053
title: Token của phiên con tính vào đúng task (dòng `Task: T-NNN` trong lời giao việc)
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-073 (docs/lessons.md, accepted)
scenarios: []
owner: Tuấn
status: planned
risk: high
size: S
depends_on: []
sprint:
plan: features/quality-gate/plans/T-053-token-phien-con.md
created: 2026-10-25
---

# T-053: TOKEN CỦA PHIÊN CON TÍNH VÀO ĐÚNG TASK

## Mô tả

Ở RUN-20261025-01, `scripts/token-usage.py` gán toàn bộ token của hai phiên con của T-048 (viết code trong worktree, rà soát độc lập) vào dòng Chung. T-048 hiện 589k, bằng 0,1 lần ước tính, còn số đếm tay là khoảng 12,4M (L-073). Phiên con làm trong worktree bằng đường dẫn, nên transcript của nó vẫn ghi nhánh của thư mục chính, mà script lại gán theo nhánh.

Dự án mẫu đã sửa đúng lỗi này (bài học L-071 của mẫu). Lời giao việc cho phiên con có một dòng riêng `Task: T-NNN`, và script gán mọi request của transcript phiên con theo dòng đó. Ticket này chép cách sửa về app, để số theo task dùng được cho `token_estimate`.

## Phạm vi

- `scripts/token-usage.py`: chép phần `BRIEF_TASK`, `message_text` và cách đọc tin nhắn đầu của transcript trong `subagents/` từ dự án mẫu. Phần riêng của app (credit, worktree, tên repo) giữ nguyên.
- `scripts/tests/run.sh`: hai test của mẫu, cộng một test cho lời giao dạng danh sách đoạn chữ và một đối chứng cho transcript của phiên chính.
- `docs/auto-run.md` (giao bước 3 đến 5 cho phiên con, bước 6 rà soát độc lập, soạn plan cuối phiên) và `docs/ops/token-usage.md`: lời giao việc cho phiên con có một dòng riêng `Task: T-NNN`.
- `docs/lessons.md`: điền mục Áp dụng của L-073.

**Không bao gồm:**

- Sửa lại số đã ghi trong `docs/metrics/token-usage.csv` của các phiên trước.
- Gán theo lệnh `cd` hay `git -C` trong transcript (cách khác mà L-073 nêu).

## Rủi ro sơ bộ

Chạm điểm chặn 7 (quy trình phiên tự chạy): đổi lời giao việc trong `auto-run.md`, và đổi số token mà báo cáo phiên dùng. Code chép nguyên từ dự án mẫu, nơi nó đã chạy qua nhiều phiên.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí ở mục 2 của plan đạt.
- [ ] Có test tự động cho cách gán mới.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Báo cáo của phiên tự chạy kế tiếp (sau phiên có T-053) có dòng riêng cho task mà phiên con đã làm.
