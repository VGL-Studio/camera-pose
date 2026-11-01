---
name: spec
description: Từ yêu cầu của người (file trong requests/ hay inbox/, hoặc lời dev nói trong phiên) sinh hay cập nhật spec của một feature, trạng thái draft, có câu hỏi còn mở. Dùng khi có yêu cầu mới cần thành spec.
argument-hint: <feature> [đường dẫn yêu cầu]
---

# /spec: từ yêu cầu ra spec

Tham số: `$ARGUMENTS` (tên feature, có thể kèm file yêu cầu). Thiếu feature thì hỏi dev, hoặc đề xuất tên trong mục câu hỏi.

1. Đọc `docs/CLAUDE.md`, mục Đọc trước khi làm và mục Khi sinh spec từ yêu cầu. Làm đúng các quy tắc ở đó; skill này không thay chúng.
2. Đọc yêu cầu: `docs/features/<feature>/requests/` (trạng thái `new`) và `docs/inbox/`. Lời dev nói trong phiên thì ghi thành file yêu cầu theo `docs/templates/request.md` trước. Yêu cầu ở `inbox/` đã xác định feature thì chuyển vào `features/<feature>/requests/`.
3. Spec đã có thì đọc cả lịch sử và câu hỏi đã trả lời. Feature mới thì tạo thư mục theo cấu trúc ở `docs/README.md`.
4. Viết theo `docs/templates/spec.md`, giữ đủ mục. Không tự thêm quảng cáo, sự kiện phân tích, dữ liệu thu thập hay quyền; thấy cần thì đặt câu hỏi kèm phương án.
5. Spec mới `draft`; spec `confirmed` có yêu cầu mới thì `changed` kèm mục lịch sử. Không tự ghi `confirmed`, trừ đường làm nhanh khi dev xác nhận rõ trong phiên (ADR-0034).
6. Yêu cầu đã có spec thì đổi `status` của nó từ `new` sang `specified`.
7. Làm trên nhánh mới từ `origin/main` (worktree riêng nếu thư mục chính đang có việc khác), commit theo đường dẫn, mở PR. Báo dev: tóm tắt spec và các câu hỏi cần trả lời.
