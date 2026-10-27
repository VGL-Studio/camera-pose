---
id: T-066
title: Kiểm khả năng của cách mở phiên trước khi duyệt và ở đầu phiên (`open_with`, bảng khả năng, `push-probe.sh`)
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-086 (docs/lessons.md, accepted)
scenarios: []
owner: Tuấn
status: approved
risk: high
size: M
depends_on: [T-067]          # PB-13, plan-check.py --touches, ADR-1005/L8
sprint:
plan: features/quality-gate/plans/T-066-kha-nang-cach-mo-phien.md
created: 2026-10-27
---

# T-066: KIỂM KHẢ NĂNG CỦA CÁCH MỞ PHIÊN

## Mô tả

Ở RUN-20261027-01, T-062 làm xong, `check.sh` qua, rà soát không có điểm nào, nhưng GitHub từ chối lệnh đẩy vì token của phiên `claude --cloud` không có quyền `workflow`; T-065 bị bỏ qua cùng lý do (L-086). `run-check.py`, `plan-check.py` và cả hai vòng rà soát trước khi duyệt đều không hỏi "phiên mở bằng cách này có làm được việc mà plan cần không", và lệnh đẩy đầu tiên của task chỉ lộ lỗi sau khi việc đã xong.

Ticket này làm ba việc Tuấn chọn ngày 2026-10-27 (đề xuất 1a, 1b, 4c):

- Danh sách chạy ghi cách mở phiên (`open_with: cloud | routine | local`); bảng khả năng của từng cách mở (`docs/ops/session-capabilities.md`); `scripts/run-check.py` quy tắc 8 đối chiếu file ở mục 3.2 của từng plan với bảng.
- Mẫu rà soát danh sách (`docs/ops/pre-run-review.md`) có câu hỏi bắt buộc về khả năng của cách mở phiên.
- Đầu phiên, khi danh sách có plan sửa `.github/workflows/`, phiên thử đẩy một nhánh tạm có đổi workflow (`scripts/push-probe.sh`); bị từ chối thì bỏ qua ngay các task đó, làm các task khác.

## Phạm vi

- `scripts/run-check.py` (quy tắc 8, biến `RUN_CHECK_OPEN_WITH`), `scripts/push-probe.sh` (mới; tra lời từ chối bằng bảng tra lỗi, PB-13 của T-067), `scripts/tests/run.sh` (46 test mới).
- `docs/ops/session-capabilities.md` (mới), `docs/templates/run.md`, `docs/CLAUDE.md` (Khi lập danh sách chạy), `docs/ops/pre-run-review.md`, `docs/auto-run.md` (mục 1 bước 3, mục 2), `CLAUDE.md` (mục Lệnh).
- `docs/lessons.md`: L-086 sang `accepted` (PR plan), rồi `applied` khi task merge.

**Không bao gồm:**

- Dòng bảng tra lỗi PB-13, `plan-check.py --touches`, ADR-1005/L8: T-067 (chạy trước, T-066 phụ thuộc).
- Ghi patch của nhánh không đẩy được vào nhánh báo cáo, workflow theo dõi phiên đứng im: T-068.
- Cấp lại quyền `workflow` cho kết nối GitHub của tài khoản Claude: việc của Tuấn.
- Ghi `open_with` vào danh sách nháp RUN-20261027-02.

## Rủi ro sơ bộ

Chạm điểm chặn 7 (quy trình phiên tự chạy: `docs/auto-run.md` mục 2 thêm một bước đầu phiên; `run-check.py` thêm lỗi có thể chặn duyệt danh sách). `push-probe.sh` đẩy và xóa một nhánh `claude/probe-<RUN>-<giờ>-<pid>` trên GitHub; không workflow nào chạy theo push lên nhánh đó (có test).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí ở mục 2 của plan đạt.
- [ ] Có test tự động cho quy tắc 8 và `push-probe.sh`.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Phiên đầu tiên có plan sửa `.github/workflows/` chạy `push-probe.sh` ở đầu phiên và ghi kết quả vào mục Kiểm tra đầu phiên (plan mục 4, dòng kiểm tra thủ công).
