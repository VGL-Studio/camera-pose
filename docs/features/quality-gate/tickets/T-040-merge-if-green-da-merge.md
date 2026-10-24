---
id: T-040
title: merge-if-green.sh nhận ra PR đã vào nhánh chính khi API merge báo lỗi
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-054 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: on-staging
risk: high
size: S
depends_on: [T-029, T-039, T-041]
sprint:
plan: features/quality-gate/plans/T-040-merge-if-green-da-merge.md
created: 2026-10-24
---

# T-040: MERGE-IF-GREEN.SH NHẬN RA PR ĐÃ VÀO NHÁNH CHÍNH KHI API MERGE BÁO LỖI

## Mô tả

API merge của GitHub trả lỗi nhưng commit đã vào nhánh chính; script báo "TỪ CHỐI", PR vẫn mở, không có workflow `push` (L-054, PR #67). Sau ticket này, gặp lỗi từ API merge, `merge-if-green.sh` kiểm nhánh chính đã chứa commit đầu của PR chưa; có rồi thì in `ĐÃ MERGE (GitHub báo lỗi)`, thoát 0 (câu 1 của plan, đã duyệt) và chạy `main-guard` bằng `workflow_dispatch`.

## Phạm vi

- `scripts/merge-if-green.sh`: nhánh xử lý lỗi của lệnh merge.
- Test với `gh` giả trong `scripts/tests/run.sh`, đi qua mọi kiểm tra hiện có (L-044).
- `docs/ops/failure-playbook.md` (T-039): dòng PB-04 theo mã thoát mới.
- `docs/auto-run.md` bước 9: thông báo mới.

**Không bao gồm:**

- Đóng PR còn mở sau sự cố (người làm).
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: `merge-if-green.sh` (điểm chặn 7), duyệt chéo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
