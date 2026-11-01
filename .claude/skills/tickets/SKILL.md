---
name: tickets
description: Tách spec của một feature thành các ticket giao được, có mã T-NNN tiếp theo, kịch bản, phụ thuộc, cỡ và rủi ro sơ bộ. Dùng khi spec đã confirmed và cần chia việc.
argument-hint: <feature>
---

# /tickets: tách spec thành ticket

Tham số: `$ARGUMENTS` (tên feature).

1. Đọc `docs/CLAUDE.md`, mục Khi tách spec thành ticket, và `docs/templates/ticket.md`.
2. Kiểm spec `docs/features/<feature>/spec.md` có `status: confirmed` và không còn câu hỏi mở. Chưa đủ thì dừng, báo dev những câu hỏi còn lại (gợi ý `/spec`).
3. Mã ticket: lấy mã lớn nhất trong mọi `docs/features/*/tickets/` của nhánh chính mới nhất (`git fetch` trước), cộng một. PR song song đã lấy số đó thì lấy số sau.
4. Mỗi ticket một phần giao được, chạy xong trong một lượt của phiên tự chạy; ghi kịch bản của spec nó xử lý, `depends_on`, `size`, `risk` sơ bộ theo `docs/risk-scoring.md`. Việc phiên tự chạy không được làm thì ghi `auto_run: false`.
5. Feature có thiết kế: ticket giao diện chỉ `ready` khi thiết kế đã duyệt (`docs/README.md`, bước 5).
6. Chạy `python3 scripts/plan-queue.py` để xem ticket nào vào hàng chờ plan. Mở PR, báo dev danh sách ticket và thứ tự phụ thuộc.
