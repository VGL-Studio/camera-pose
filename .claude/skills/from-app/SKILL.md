---
name: from-app
description: Ở repo dự án mẫu, xem một issue from-app (cải tiến từ một app), nhận hay đề xuất từ chối, rồi tổng quát hóa thành PR cho dự án mẫu kèm bài học. Ở repo app, soạn issue from-app gửi về dự án mẫu.
argument-hint: "<số issue> | new"
---

# /from-app: cải tiến từ app về dự án mẫu

Tham số: `$ARGUMENTS`. Đọc `docs/ops/from-app.md` trước.

Xác định repo: có `docs/ops/new-app.md` là dự án mẫu.

**Ở dự án mẫu (`<số issue>`; không có số thì liệt kê `gh issue list --label from-app --state open`)**
1. Đọc issue và PR nguồn ở app bằng `gh api` (diff của PR, file theo commit).
2. Đề xuất với dev: nhận hay đóng (chỉ đúng với app đó, đã có cách khác, chưa đủ bằng chứng). Chờ dev quyết.
3. Nhận: bài học mới trong `docs/lessons.md`, `accepted`, nguồn ghi app và số issue (số theo `/lesson`). Số bài học, PB hay ADR của app trùng số đã có ở mẫu thì số của mẫu giữ nguyên, đổi số của phần đưa về.
4. Làm PR từ `origin/main` trong worktree riêng: bỏ tên app, tên gói, giá trị riêng; giữ quy tắc của mẫu; file cấm sửa thì ghi rõ trong mô tả PR. Kiểm bằng `scripts/tests/run.sh`, `scripts/check.sh` khi có code.
5. Gọi `/pr-review`. Sau khi merge: bài học `applied`; hỏi dev trước khi đóng issue kèm số PR.

**Ở repo app (`new`)**
1. Tìm bài học phạm vi `chung` chưa có issue, và commit ở file thuộc loại "của dự án mẫu" (`docs/ops/to-app.md`, mục 2).
2. Soạn nội dung issue theo mục 2 của `docs/ops/from-app.md`. Đưa dev xem trước khi mở, vì issue mở ở repo khác. Ghi số issue vào dòng Áp dụng của bài học.
