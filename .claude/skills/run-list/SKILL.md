---
name: run-list
description: Lập danh sách chạy RUN-YYYYMMDD-NN từ các plan approved, chọn cách mở phiên, kiểm bằng run-check.py và một lần rà soát độc lập trước khi dev duyệt; hoặc lập cho một đợt cùng PR với các plan draft (run-check.py --batch). Dùng khi có plan đã duyệt chờ phiên tự chạy, hay một đợt plan draft sẵn sàng duyệt cả đợt.
argument-hint: "[T-NNN ...]"
---

# /run-list: lập và kiểm danh sách chạy

Tham số: `$ARGUMENTS` (tùy chọn: các ticket muốn đưa vào; mặc định mọi plan `approved` qua `plan-check.py`).

1. Đọc `docs/CLAUDE.md`, mục Khi lập danh sách chạy và mục Khi duyệt cả đợt; `docs/templates/run.md`; `docs/ops/pre-run-review.md`; `docs/ops/session-capabilities.md`.
2. Kiểm không có phiên đang chạy (nhánh `claude/RUN-*-report` có `status: running`) và không có issue `main-broken`, `env-broken` đang mở. `env-broken` nói về môi trường cloud: danh sách `open_with: local` vẫn lập được, ghi issue vào mục Lý do.
3. Gom plan `approved` chưa chạy của mọi dev, mỗi plan qua `python3 scripts/plan-check.py <plan>`. Sắp: task độc lập trước, chuỗi phụ thuộc sau cùng. Lập cho một đợt: thêm các plan `draft` của đợt (mỗi plan đã qua `plan-check.py` và rà soát độc lập riêng), cùng spec cần sửa, trên một nhánh `claude/batch-YYYYMMDD-NN` từ `origin/main`; plan đã có PR riêng thì chép vào và ghi số PR cũ trong mô tả PR.
4. Ghi `open_with:` và đối chiếu khả năng của cách mở. Ghi `start:` (mặc định `auto`, chỉ với `open_with: routine`; `manual` khi dev muốn chọn giờ). Plan sửa `.github/workflows/` thì nhắc `scripts/push-probe.sh` ở đầu phiên.
5. Chạy `git fetch origin` rồi `python3 scripts/plan-queue.py --max 2` (hay giá trị `plan_queue` định đặt). Không chọn ticket nào thì đề xuất `plan_queue: 0`. Luôn chép các dòng `Đang chặn hàng chờ:` vào mục Lý do của thứ tự chạy.
6. Mục Lý do của thứ tự chạy: tổng `token_estimate` so với vài phiên gần nhất (`docs/metrics/token-usage.csv`), thời gian dự kiến, plan để phiên sau và lý do.
7. Chạy `python3 scripts/run-check.py docs/runs/RUN-YYYYMMDD-NN.md`, sửa hết lỗi. Lập cho một đợt: `git fetch origin` rồi `python3 scripts/run-check.py --batch docs/runs/RUN-YYYYMMDD-NN.md`; ghi các dòng bỏ qua trạng thái và điểm so vào mục Kiểm tra trước khi duyệt.
8. Giao một agent con ngữ cảnh mới, chỉ đọc, rà soát theo mục 2 của `docs/ops/pre-run-review.md`. Lời giao gồm đường dẫn danh sách chạy, kết quả `run-check.py`, các plan, ticket, spec, thiết kế, ADR liên quan và các file trong mục 3.2 của mỗi plan, cùng nội dung giao việc tối thiểu của mục 2 (trả bảng theo mục 3, trả lời mục 2.3 cho từng plan). Không đưa quá trình viết plan hay kết luận của mình. Ghi kết quả vào mục Kiểm tra trước khi duyệt.
9. Điểm chặn: soạn plan phiên bản mới hay đề xuất sửa spec, hoặc đề xuất bỏ task; không sửa plan `approved` hay spec `confirmed`. Plan `draft` của đợt thì sửa ngay trong PR, rồi chạy lại bước 7 và 8.
10. Danh sách `draft`. Mở PR; báo dev: số task, ước tính, điểm chặn, việc dev phải kiểm trước phiên. Lập cho một đợt: đưa tóm tắt cả đợt và câu duyệt có sẵn theo `docs/CLAUDE.md`, mục Khi duyệt cả đợt. AI chỉ ghi `approved` theo lời đồng ý rõ ràng trong phiên của người có quyền duyệt (ADR-0048, phần C); dev kích hoạt phiên. Với `start: auto`, nói rõ merge danh sách `approved` là bắt đầu phiên, và câu duyệt có sẵn ghi "bắt đầu phiên khi merge"; lời duyệt không có ý đó thì đổi sang `start: manual` trước khi merge. Danh sách do dev tự ghi `approved` thì dev merge, hoặc AI merge sau khi dev nói lại lời đồng ý trong phiên.
11. Sau khi merge danh sách `start: auto`, xem lần chạy `start-run` của lần push đó: job `fire` xanh là phiên đã bắt đầu; không có lần chạy, `cancelled`, xanh mà `fire` bị bỏ qua, hay đỏ thì báo dev lý do (tóm tắt hay dòng lỗi của job `pick`, `check` hay `fire`) và cách bấm tay (`docs/auto-run.md`, mục 1, bước 3). Nhắc dev: sau khi merge danh sách `approved`, `gh workflow list --all` phải thấy `resume-run` và `run-watchdog` ở `active`; không thì chạy `scripts/schedule-idle.py --enable` (`docs/auto-run.md`, mục 1).
