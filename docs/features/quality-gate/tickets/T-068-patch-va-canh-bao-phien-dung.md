---
id: T-068
title: Nhánh bị GitHub từ chối vì quyền workflow được giao cho người thành patch trên nhánh báo cáo; báo người khi phiên tự chạy đứng im (run-watchdog)
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là đề xuất 3b, 4b sau RUN-20261027-01 (Tuấn chọn ngày 2026-10-27), bài học L-084, L-086
scenarios: []
owner: Tuấn
status: approved
risk: high
size: M
depends_on: []
sprint:
plan: features/quality-gate/plans/T-068-patch-va-canh-bao-phien-dung.md
created: 2026-10-27
---

# T-068: NHÁNH BỊ TỪ CHỐI VÌ QUYỀN WORKFLOW GIAO CHO NGƯỜI THÀNH PATCH; BÁO NGƯỜI KHI PHIÊN ĐỨNG IM

## Mô tả

Hai chỗ làm phiên tự chạy mất việc hay đứng lâu mà không ai biết:

1. Lệnh đẩy nhánh task bị từ chối thì `scripts/save-branch.sh` chỉ giữ một bundle trên máy cloud (`$HOME/rescue/`). Máy bị thu hồi là mất. Ở RUN-20261027-01, T-062 làm xong, rà soát sạch, nhưng GitHub từ chối vì token không có quyền `workflow` (L-086); bundle nằm trên máy cloud, người không lấy được.
2. Phiên mở bằng `claude --cloud` dừng giữa chừng (lỗi API, hết hạn mức, tiến trình khởi động lại) thì không có gì báo người: `resume-run.yml` không tiếp tục kiểu phiên này (L-008). RUN-20261026-03 đứng 2 giờ 30 phút tới khi người tình cờ thấy (L-084).

Ticket này làm hai việc: (3b) khi chính GitHub từ chối lệnh đẩy vì token không có quyền `workflow`, `save-branch.sh` ghi thêm các commit của nhánh thành một tệp văn bản (`git format-patch`, hay một `git diff` gộp khi nhánh có commit merge) trên nhánh báo cáo rồi đẩy nhánh báo cáo, để người xem rồi tự quyết áp hay không: tệp văn bản dưới `docs/` đẩy được kể cả khi nhánh task sửa `.github/workflows/`. Không bao giờ làm với `--bundle-only` (môi trường chặn lời gọi đẩy) hay lần từ chối khác. (4b) Workflow mới `run-watchdog.yml` chạy mỗi 30 phút: danh sách chạy đang `running` mà các nhánh của phiên không có commit 60 phút (90 phút khi đang có lần chạy workflow của phiên) thì mở một issue `session-stalled` có sẵn prompt tiếp tục để dán; có commit lại hay danh sách xong thì tự đóng.

## Phạm vi

- `scripts/save-branch.sh`: chỉ khi GitHub từ chối vì quyền `workflow`, ghi patch lên nhánh báo cáo (worktree `claude/RUN-*-report`, worktree tạm, hay `SAVE_BRANCH_REPORT_DIR`); patch được áp thử trong worktree tạm, giới hạn cỡ, kiểm UTF-8, quét bằng `scripts/secret-scan.py`; in lệnh lấy lại.
- `scripts/run-watchdog.py` (mới) và `.github/workflows/run-watchdog.yml` (mới).
- `scripts/tests/run.sh`, `scripts/tests/fake-gh`: test cho cả hai.
- `docs/auto-run.md` (bước 7, Khi không đẩy được; mục 4, Tiếp tục phiên sau khi hết hạn mức), `docs/templates/run.md` (Nhánh chưa đẩy), `docs/ops/staging-and-runs.md` (Khi phiên bị ngắt).

**Không bao gồm:**

- Tự tiếp tục một phiên `claude --cloud` (không có API để mở phiên kiểu này).
- Đổi `resume-run.yml` hay `scripts/resume-check.py`.
- Cấp quyền `workflow` cho phiên cloud (Tuấn làm, L-086).

## Rủi ro sơ bộ

Cao: chạm điểm chặn 7 (CI, `.github/workflows/` mới; quy trình phiên tự chạy). `save-branch.sh` chạy ở mọi lần đẩy của phiên. Phần mới chỉ chạy khi lệnh đẩy đã thất bại, và mọi lỗi của nó chỉ thành một dòng ghi chú, bundle vẫn giữ, mã thoát vẫn 3. Watchdog chỉ đọc repo và ghi issue `session-stalled`; tốn khoảng 48 phút Actions mỗi ngày (nhịp do Tuấn chọn, plan mục 8).

## Định nghĩa hoàn thành

- Test mới trong `scripts/tests/run.sh` qua; `scripts/check.sh` qua.
- Sau khi merge: phiên chạy tay `run-watchdog.yml` một lần trên nhánh chính (plan mục 3.1, Sau khi merge): xanh, in "đang chạy, commit gần nhất N phút trước" cho danh sách của chính phiên, không mở issue.
- Lần đầu GitHub từ chối một lệnh đẩy vì quyền `workflow`, báo cáo có dòng "Patch trên nhánh báo cáo" và người xem patch rồi lấy lại được bằng các lệnh in ra.
