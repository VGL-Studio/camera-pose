---
name: write-plan
description: Viết plan draft cho một ticket theo mẫu plan, có danh sách file đầy đủ, test viết trước, điểm rủi ro, mục 3.5 từ existing-tests.py, token_estimate, và qua plan-check.py. Dùng khi một ticket cần plan để đưa vào họp đánh giá rủi ro.
argument-hint: <T-NNN>
---

# /write-plan: viết plan cho một ticket

Tham số: `$ARGUMENTS` (mã ticket). Không có thì chạy `python3 scripts/plan-queue.py` và hỏi dev chọn ticket.

1. Đọc theo `docs/CLAUDE.md`, mục Đọc trước khi làm (ticket, spec, thiết kế, `docs/foundation.md`, phản hồi, plan trước của feature).
2. Đọc `docs/CLAUDE.md`, mục Khi viết plan, và `docs/templates/plan.md`. Mỗi gạch đầu dòng của mục Khi viết plan là một điều phải kiểm; đi hết danh sách, ghi lại điều nào áp dụng cho plan này.
3. Kiểm ticket chưa có plan và không có nhánh `origin/claude/plan-T-NNN*` hay PR nào đang giữ plan của nó.
4. Viết plan:
   - Mục 3.2 đủ và cụ thể: đó là ranh giới file của phiên.
   - Sửa code Kotlin ngoài test: chạy `python3 scripts/existing-tests.py <plan>` và viết mục 3.5.
   - Sửa lỗi giao diện hay lỗi đo được: chạy test tạm trên code hiện tại, khung tái hiện được lỗi gốc, ghi số đo thật vào mục 4 (L-029).
   - Kỹ thuật hay thư viện lần đầu dùng (E từ 2): chạy thử bằng `scripts/check.sh` trên nhánh có code liên quan, ghi vào mục 4.1 (L-042).
   - Thêm thư viện hay module: thêm `./gradlew :app:assembleStagingRelease`, ghi vào mục 4.1 (L-083).
   - Rủi ro theo `docs/risk-scoring.md`, lý do cho từng tiêu chí, không hạ điểm.
   - `token_estimate` từ `python3 scripts/token-usage.py estimate`.
   - Điều chưa chắc ghi ở mục câu hỏi cho người duyệt.
5. Chạy `python3 scripts/plan-check.py <plan>` tới khi không còn lỗi.
6. Plan `draft`. Không tự chuyển `approved` (trừ `/quick-lane`). Nhánh `claude/plan-T-NNN-<tu-khoa>` từ `origin/main`, commit theo đường dẫn, mở PR có kết quả `plan-check.py` trong mô tả.
7. Gọi `/pr-review` trước khi báo dev PR xong. Báo dev: phạm vi, file, test, điểm rủi ro, câu hỏi.
