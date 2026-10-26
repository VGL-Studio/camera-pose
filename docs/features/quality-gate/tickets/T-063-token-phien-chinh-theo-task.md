---
id: T-063
title: Token của phiên chính khi làm task trên HEAD tách rời tính vào đúng task (lệnh `token-usage.py mark`)
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-083 (docs/lessons.md, accepted)
scenarios: []
owner: Tuấn
status: approved
risk: high
size: S
depends_on: []
sprint:
plan: features/quality-gate/plans/T-063-token-phien-chinh-theo-task.md
created: 2026-10-26
---

# T-063: TOKEN CỦA PHIÊN CHÍNH KHI LÀM TASK TÍNH VÀO ĐÚNG TASK

## Mô tả

Khi phiên con giữ nhánh task trong worktree, thư mục chính đứng ở `origin/main` tách rời. `scripts/token-usage.py` gán request của phiên chính theo nhánh của thư mục chính, nên phần phiên chính làm cho task (đọc plan, mở PR, chờ CI, merge, nhật ký, hay tự làm bước 3 đến 5 trong worktree) vào dòng Chung (L-083). Ở RUN-20261026-03, dòng Chung lên 33.2M; T-060 chỉ còn 0.5M, T-059 2.3M, nên số theo task không dùng được cho `token_estimate`.

Ticket này thêm lệnh `scripts/token-usage.py mark T-NNN` (task bắt đầu) và `mark chung` (task xong). Phiên chính chạy lệnh ở đầu và cuối mỗi task; `report` đọc lại lệnh đó từ transcript của phiên chính và gán các request không ở nhánh task nào trong khoảng giữa hai lệnh cho task.

## Phạm vi

- `scripts/token-usage.py`: lệnh con `mark`; `report` đọc lệnh `mark` trong transcript của phiên chính và gán theo nó; dấu mốc trước `--since` không tính.
- `scripts/tests/run.sh`: 11 test mới trong khối `token-usage`.
- `docs/auto-run.md` (bước 1 của task, Giao bước 3 đến 5, bước 10, mục 4 bước 0, Tiếp tục phiên bước 5) và `docs/ops/token-usage.md`: phiên chính chạy `mark T-NNN` khi bắt đầu hay chuyển sang một task, `mark chung` khi không còn task nào đang làm.
- `docs/lessons.md`: L-083 sang `applied`, thay dòng Áp dụng hiện có.

**Không bao gồm:**

- Sửa lại số đã ghi trong `docs/metrics/token-usage.csv` của các phiên trước.
- Gán theo giờ tạo nhánh và giờ merge trong Dòng thời gian của báo cáo (cách khác của L-083, plan mục 3.4).

## Rủi ro sơ bộ

Chạm điểm chặn 7 (quy trình phiên tự chạy): thêm một lệnh phiên chính phải chạy ở mỗi task trong `auto-run.md`, và đổi số token mà báo cáo phiên ghi. Phiên quên chạy lệnh thì số giống hôm nay (vào Chung), không hỏng gì.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí ở mục 2 của plan đạt.
- [ ] Có test tự động cho cách gán mới.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Báo cáo của phiên tự chạy kế tiếp: mỗi task có phiên con có số request lớn hơn tổng request của phiên con và phiên rà soát (plan mục 4, dòng kiểm tra thủ công).
