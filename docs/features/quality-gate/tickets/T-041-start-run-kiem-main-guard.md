---
id: T-041
title: start-run.yml kiểm main-guard xanh trên commit đầu nhánh chính
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-055 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: on-staging
risk: high
size: S
depends_on: [T-030]
sprint:
plan: features/quality-gate/plans/T-041-start-run-kiem-main-guard.md
created: 2026-10-24
---

# T-041: START-RUN.YML KIỂM MAIN-GUARD XANH TRÊN COMMIT ĐẦU NHÁNH CHÍNH

## Mô tả

RUN-20261023-03 được kích hoạt khi commit đầu nhánh chính có code chưa qua `main-guard` (L-055). Sau ticket này, `start-run.yml` từ chối bắt đầu phiên khi commit đầu nhánh chính đổi file ngoài `docs/` mà chưa có lần chạy `main-guard` xanh cho chính commit đó (commit chỉ đổi `docs/` thì xét commit gần nhất có đổi code), và in hướng dẫn chạy `main-guard`.

## Phạm vi

- `.github/workflows/start-run.yml`: bước kiểm mới.
- Script đọc được và có test: `scripts/main-guard-status.sh` (gọi từ workflow).

**Không bao gồm:**

- Tự chạy `main-guard` thay người.
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: sửa `.github/` (điểm chặn 7), duyệt chéo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
