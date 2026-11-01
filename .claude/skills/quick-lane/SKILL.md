---
name: quick-lane
description: Đường làm nhanh (ADR-0034) cho một việc nhỏ của code app khi dev ngồi cùng suốt task, từ yêu cầu tới merge trong một PR.
argument-hint: <việc cần làm hoặc T-NNN>
disable-model-invocation: true
---

# /quick-lane: làm nhanh có dev ngồi cùng

Tham số: `$ARGUMENTS` (lời mô tả việc, hoặc mã ticket đã có).

Làm đúng `docs/CLAUDE.md`, mục Khi làm nhanh có người theo dõi; hướng dẫn cho dev ở `docs/ops/quick-lane.md`. Thứ tự:

1. **Kiểm điều kiện vào** theo gạch đầu dòng đầu của mục đó và mục 1 của `docs/ops/quick-lane.md`. Việc về script, tài liệu, quy trình không đi đường này: làm PR thường. Thiếu điều nào thì nói rõ và đề xuất `/write-plan`.
2. **Kiểm lúc bắt đầu:** không có phiên tự chạy đang chạy, không có issue `main-broken`, `env-broken`. Nhắc dev không kích hoạt phiên tự chạy khi PR còn mở. Lấy mã ticket (số tiếp theo, như `/tickets`), rồi `python3 scripts/token-usage.py mark T-NNN`.
3. **Tài liệu:** yêu cầu, spec nếu đổi (chỉ ghi `confirmed` khi dev ngồi cùng là người xác nhận spec của feature và đã xác nhận rõ), ticket, plan đủ mục với `lane: quick`, `scripts/existing-tests.py`, `scripts/plan-check.py`.
4. **Duyệt plan:** đưa tóm tắt (phạm vi, file, test, rủi ro, câu hỏi), chờ dev đồng ý rõ ràng, rồi ghi `approved`, `reviewer`, `approved_at`, mục 9; `python3 scripts/status.py --sync`.
5. **Làm:** bước 3 tới 9 của `docs/auto-run.md` trên nhánh `claude/quick-T-NNN-<tu-khoa>` (worktree riêng): test viết trước, `scripts/check.sh`, `/pr-review` (lời giao có dòng `Task: T-NNN`), một PR cho cả task, nhật ký trong lần đẩy cuối, `scripts/wait-run.sh`, kiểm các danh sách chạy chưa chạy, `/merge-pr`, chờ `main-guard`.
6. **Lệch so với plan:** hỏi dev trước, ghi vào nhật ký.
7. **Dừng:** đóng PR, giữ nhánh, PR tài liệu theo mục đó của `docs/CLAUDE.md`.
8. **Xong:** `token-usage.py mark chung`, `report --since <giờ bắt đầu> --run QUICK-YYYYMMDD-T-NNN --csv docs/metrics/token-usage.csv`; tệp CSV đi cùng PR tài liệu kế tiếp. Báo dev kết quả và việc kiểm thủ công còn lại.
