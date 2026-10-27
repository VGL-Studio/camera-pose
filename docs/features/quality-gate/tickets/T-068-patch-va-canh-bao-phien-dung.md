---
id: T-068
title: Nhánh chưa đẩy được giữ thành patch trên nhánh báo cáo; báo người khi phiên tự chạy đứng im (run-watchdog)
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là đề xuất 3b, 4b sau RUN-20261027-01 (Tuấn chọn ngày 2026-10-27), bài học L-084, L-086
scenarios: []
owner: Tuấn
status: planned
risk: high
size: M
depends_on: []
sprint:
plan: features/quality-gate/plans/T-068-patch-va-canh-bao-phien-dung.md
created: 2026-10-27
---

# T-068: NHÁNH CHƯA ĐẨY ĐƯỢC GIỮ THÀNH PATCH; BÁO NGƯỜI KHI PHIÊN ĐỨNG IM

## Mô tả

Hai chỗ làm phiên tự chạy mất việc hay đứng lâu mà không ai biết:

1. Lệnh đẩy nhánh task bị từ chối thì `scripts/save-branch.sh` chỉ giữ một bundle trên máy cloud (`$HOME/rescue/`). Máy bị thu hồi là mất. Ở RUN-20261027-01, T-062 làm xong, rà soát sạch, nhưng GitHub từ chối vì token không có quyền `workflow` (L-086); bundle nằm trên máy cloud, người không lấy được.
2. Phiên mở bằng `claude --cloud` dừng giữa chừng (lỗi API, hết hạn mức, tiến trình khởi động lại) thì không có gì báo người: `resume-run.yml` không tiếp tục kiểu phiên này (L-008). RUN-20261026-03 đứng 2 giờ 30 phút tới khi người tình cờ thấy (L-084).

Ticket này làm hai việc: (3b) `save-branch.sh` ghi thêm các commit của nhánh chưa đẩy thành một tệp mbox (`git format-patch`) trên nhánh báo cáo rồi đẩy nhánh báo cáo: tệp văn bản dưới `docs/` đẩy được kể cả khi nhánh task sửa `.github/workflows/`; người lấy lại bằng `git am` mà không cần máy cloud. (4b) Workflow mới `run-watchdog.yml` chạy mỗi 15 phút: danh sách chạy đang `running` mà các nhánh của phiên không có commit 45 phút (90 phút khi đang có workflow phiên có thể đang chờ) thì mở một issue `session-stalled` có sẵn prompt tiếp tục để dán; có commit lại hay danh sách xong thì tự đóng.

## Phạm vi

- `scripts/save-branch.sh`: ghi mbox lên nhánh báo cáo (tự tìm worktree `claude/RUN-*-report`, hay `SAVE_BRANCH_REPORT_DIR`), giới hạn cỡ, quét bằng `scripts/secret-scan.py`, in lệnh lấy lại.
- `scripts/run-watchdog.py` (mới) và `.github/workflows/run-watchdog.yml` (mới).
- `scripts/tests/run.sh`: test cho cả hai.
- `docs/auto-run.md` (bước 7, Khi không đẩy được; mục 4, Tiếp tục phiên sau khi hết hạn mức), `docs/templates/run.md` (Nhánh chưa đẩy), `docs/ops/staging-and-runs.md` (Khi phiên bị ngắt).

**Không bao gồm:**

- Tự tiếp tục một phiên `claude --cloud` (không có API để mở phiên kiểu này).
- Đổi `resume-run.yml` hay `scripts/resume-check.py`.
- Cấp quyền `workflow` cho phiên cloud (Tuấn làm, L-086).

## Rủi ro sơ bộ

Cao: chạm điểm chặn 7 (CI, `.github/workflows/` mới; quy trình phiên tự chạy). `save-branch.sh` chạy ở mọi lần đẩy của phiên. Phần mới chỉ chạy khi lệnh đẩy đã thất bại, và mọi lỗi của nó chỉ thành một dòng ghi chú, bundle vẫn giữ, mã thoát vẫn 3. Watchdog chỉ đọc repo và ghi issue `session-stalled`.

## Định nghĩa hoàn thành

- Test mới trong `scripts/tests/run.sh` qua; `scripts/check.sh` qua.
- Sau khi merge: một lần `run-watchdog.yml` chạy tay trên nhánh chính xanh, không mở issue nào khi không có phiên `running`.
- Phiên tự chạy có lệnh đẩy bị từ chối thì báo cáo có đoạn "Patch trên nhánh báo cáo" và người lấy lại được bằng các lệnh in ra (kiểm khi gặp lần đầu).
