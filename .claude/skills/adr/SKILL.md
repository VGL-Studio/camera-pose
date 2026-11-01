---
name: adr
description: Viết một ADR mới theo mẫu (mã đúng dải của dự án mẫu hay app, mục Việc AI phải tuân thủ), hoặc ghi trạng thái ADR mà dev đã quyết, rồi sinh lại docs/foundation.md. Dùng khi một quyết định kỹ thuật có ảnh hưởng lâu dài vừa được đưa ra.
argument-hint: <chủ đề hoặc ADR-NNNN>
---

# /adr: ghi một quyết định

Tham số: `$ARGUMENTS` (chủ đề của quyết định mới, hoặc mã ADR cần đổi trạng thái).

1. Đọc `docs/CLAUDE.md`, mục Khi viết ADR, và `docs/templates/adr.md`. Đọc các ADR liên quan trong `docs/adr/` để ghi `supersedes` hay liên kết.
2. Mã: repo có `docs/ops/new-app.md` (dự án mẫu) lấy số tiếp theo dưới 1000, `origin: template`; repo app lấy số tiếp theo từ 1001, `origin: app`. Lấy số trên nhánh chính mới nhất; PR song song đã lấy số thì lấy số sau.
3. Ở app: không sửa ADR `origin: template`; muốn khác thì ADR của app có `supersedes`, và gợi ý `/lesson` để đề xuất về dự án mẫu.
4. Mục "Việc AI phải tuân thủ": các vế ngắn nối bằng dấu chấm phẩy, mỗi vế là một dòng của `docs/foundation.md`.
5. ADR mới `proposed`. Chỉ đổi trạng thái khi dev đã quyết trong phiên. Không sửa ADR `accepted`; khi có ADR thay thế thì ở ADR cũ chỉ ghi `status: superseded` và `superseded_by`.
6. Chạy `python3 scripts/foundation.py`, commit `docs/foundation.md` cùng PR. Gọi `/pr-review`, rồi báo dev quyết định cần duyệt.
