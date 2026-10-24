---
id: T-044
title: wait-run.sh --pr chỉ nhận lần chạy của commit đầu hiện tại
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-066 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: on-staging
risk: high
size: S
depends_on: [T-033]
sprint:
plan: features/quality-gate/plans/T-044-wait-run-commit-hien-tai.md
created: 2026-10-24
---

# T-044: WAIT-RUN.SH --PR CHỈ NHẬN LẦN CHẠY CỦA COMMIT ĐẦU HIỆN TẠI

## Mô tả

Đẩy commit mới lên nhánh PR thì GitHub hủy lần chạy `pr-check` của commit trước. `wait-run.sh --pr` gọi ngay sau lần đẩy thấy lần chạy cũ `cancelled` và báo `ĐỎ`, mã 1 (L-066; ba lần trong một buổi, PR #98, #100). Sau ticket này, `wait-run.sh --pr` đọc commit đầu hiện tại của PR mỗi vòng, chỉ nhận lần chạy có `head_sha` bằng commit đó, và coi lần chạy `cancelled` của commit cũ là chưa có lần chạy.

## Phạm vi

- `scripts/wait-run.sh`: chế độ `--pr`.
- Test với `gh` giả trong `scripts/tests/run.sh`: commit cũ `cancelled`, commit mới đang chạy rồi xanh.
- `docs/auto-run.md` bước 8: ghi hành vi mới.

**Không bao gồm:**

- Chế độ `--workflow`.
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: `wait-run.sh` do mọi bước chờ CI của phiên dùng (cổng chất lượng, điểm chặn 7).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
