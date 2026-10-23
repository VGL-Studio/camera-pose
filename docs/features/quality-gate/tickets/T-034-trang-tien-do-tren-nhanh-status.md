---
id: T-034
title: Trang tiến độ sinh trên nhánh status sau mỗi lần merge, PR không commit docs/STATUS.md
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-047, L-016 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: planned
risk: high
size: S
depends_on: [T-029]
sprint:
plan: features/quality-gate/plans/T-034-trang-tien-do-tren-nhanh-status.md
created: 2026-10-23
---

# T-034: TRANG TIẾN ĐỘ SINH TRÊN NHÁNH STATUS SAU MỖI LẦN MERGE

## Mô tả

Mỗi PR sửa trạng thái đều commit `docs/STATUS.md` sinh lại, nên PR thứ hai trở đi luôn xung đột: `merge-if-green.sh --update` thất bại ở mọi PR, mỗi PR tốn thêm một vòng gộp tay và một lần CI (L-047, L-016). Sau ticket này `docs/STATUS.md` không còn trong git. Workflow `status-page.yml` sinh trang sau mỗi lần merge vào nhánh chính và đẩy lên nhánh `status`; người đọc trang ở đó. Trên máy, `scripts/status.py` vẫn sinh trang để xem, git bỏ qua file này.

## Phạm vi

- `.gitignore`, bỏ `docs/STATUS.md` khỏi git.
- `.github/workflows/status-page.yml` mới.
- Tài liệu nói về trang tiến độ: `README.md`, `docs/README.md`, `docs/CLAUDE.md` (mục Trang tiến độ), `docs/auto-run.md` (PR báo cáo).
- `scripts/status.py`: chỉ chú thích và thông báo của `--check`.

**Không bao gồm:**

- Đổi nội dung trang hay cách `status.py` đọc tài liệu.
- `merge-if-green.sh`, `main-guard.yml`: không đổi.
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: CI và quy trình phiên tự chạy (điểm chặn 7). Workflow mới có quyền ghi vào repo, như `main-guard.yml`.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
