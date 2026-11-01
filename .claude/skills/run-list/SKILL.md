---
name: run-list
description: Lập danh sách chạy RUN-YYYYMMDD-NN từ các plan approved, chọn cách mở phiên, kiểm bằng run-check.py và một lần rà soát độc lập trước khi dev duyệt. Dùng sau họp đánh giá rủi ro, khi có plan đã duyệt chờ phiên tự chạy.
argument-hint: "[T-NNN ...]"
---

# /run-list: lập và kiểm danh sách chạy

Tham số: `$ARGUMENTS` (tùy chọn: các ticket muốn đưa vào; mặc định mọi plan `approved` qua `plan-check.py`).

1. Đọc `docs/CLAUDE.md`, mục Khi lập danh sách chạy; `docs/templates/run.md`; `docs/ops/pre-run-review.md`; `docs/ops/session-capabilities.md`.
2. Kiểm không có phiên đang chạy (nhánh `claude/RUN-*-report` có `status: running`) và không có issue `main-broken`, `env-broken` đang mở. `env-broken` nói về môi trường cloud: danh sách `open_with: local` vẫn lập được, ghi issue vào mục Lý do (L-139).
3. Gom plan `approved` chưa chạy của mọi dev, mỗi plan qua `python3 scripts/plan-check.py <plan>`. Sắp: task độc lập trước, chuỗi phụ thuộc sau cùng.
4. Ghi `open_with:` và đối chiếu khả năng của cách mở. Plan sửa `.github/workflows/` thì nhắc `scripts/push-probe.sh` ở đầu phiên.
5. Mục Lý do của thứ tự chạy: tổng `token_estimate` so với vài phiên gần nhất (`docs/metrics/token-usage.csv`), thời gian dự kiến, plan để phiên sau và lý do.
6. Chạy `python3 scripts/run-check.py docs/runs/RUN-YYYYMMDD-NN.md`, sửa hết lỗi.
7. Giao một agent con ngữ cảnh mới, chỉ đọc, rà soát theo mục 2 của `docs/ops/pre-run-review.md`. Lời giao gồm đường dẫn danh sách chạy, kết quả `run-check.py`, các plan, ticket, spec, thiết kế, ADR liên quan và các file trong mục 3.2 của mỗi plan, cùng nội dung giao việc tối thiểu của mục 2 (trả bảng theo mục 3, trả lời mục 2.3 cho từng plan). Không đưa quá trình viết plan hay kết luận của mình. Ghi kết quả vào mục Kiểm tra trước khi duyệt.
8. Điểm chặn: soạn plan phiên bản mới hay đề xuất sửa spec, hoặc đề xuất bỏ task; không sửa plan `approved` hay spec `confirmed`.
9. Danh sách `draft`. Mở PR; báo dev: số task, ước tính, điểm chặn, việc dev phải kiểm trước phiên. Chỉ dev duyệt và kích hoạt phiên.
10. Nhắc dev: sau khi merge danh sách `approved`, `gh workflow list --all` phải thấy `resume-run` và `run-watchdog` ở `active`; không thì chạy `scripts/schedule-idle.py --enable` (`docs/auto-run.md`, mục 1).
