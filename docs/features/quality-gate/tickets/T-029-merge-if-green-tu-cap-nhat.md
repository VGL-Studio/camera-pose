---
id: T-029
title: merge-if-green.sh tự cập nhật nhánh và chịu được job kẹt trạng thái
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-017, L-018 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: planned
risk: high
size: S
depends_on: [T-028]
sprint:
plan: features/quality-gate/plans/T-029-merge-if-green-tu-cap-nhat.md
created: 2026-10-23
---

# T-029: MERGE-IF-GREEN.SH TỰ CẬP NHẬT NHÁNH VÀ CHỊU ĐƯỢC JOB KẸT TRẠNG THÁI

## Mô tả

Hiện `merge-if-green.sh` từ chối PR có nhánh chậm hơn nhánh chính (mã 3); người hay phiên phải tự cập nhật nhánh, chờ CI, gọi lại. Một lần GitHub ghi job là `in_progress` dù lần chạy đã xong và xanh, làm script chờ mãi (L-017). Sau ticket này, `--update` cập nhật nhánh qua API của GitHub và trả "chờ CI" (mã 2); job kẹt trạng thái được xét theo kết quả của cả lần chạy; script cảnh báo khi commit cuối vừa được đẩy (L-018). Phiên tự chạy bớt một vòng thao tác tay mỗi khi nhánh chính đi trước.

## Phạm vi

- `scripts/merge-if-green.sh`: tham số `--update`; xét check suite khi check run chưa `completed`; cảnh báo commit cuối mới đẩy.
- Test trong `scripts/tests/run.sh`.
- `docs/auto-run.md` bước 9: dùng `--update`.

**Không bao gồm:**

- Tự merge sau khi cập nhật (script không chờ CI trong cùng lệnh, vì lệnh của phiên chỉ chạy được khoảng 10 phút).
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: đổi cổng chất lượng (`merge-if-green.sh`, điểm chặn 7). Thay đổi nhỏ, mọi đường đi có test.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
