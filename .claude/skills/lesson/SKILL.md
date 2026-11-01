---
name: lesson
description: Ghi một bài học L-NNN vào docs/lessons.md (proposed), thêm dòng bảng tra lỗi khi là lỗi môi trường dò được, hoặc làm các bài học accepted thành PR cải tiến rồi ghi applied. Dùng sau sự cố, khi phải đoán vì tài liệu thiếu, hay ở retro.
argument-hint: "<chuyện đã xảy ra> | apply [L-NNN ...]"
---

# /lesson: ghi và làm bài học

Tham số: `$ARGUMENTS`. Bắt đầu bằng `apply` thì làm bài học đã nhận; không thì ghi bài học mới.

Đọc `docs/auto-run.md`, mục 6, và phần Cách ghi ở đầu `docs/lessons.md`.

**Ghi bài học mới**
1. Số: số lớn nhất của sổ trên nhánh chính mới nhất cộng một. Trước khi merge, gộp nhánh chính và đổi số nếu PR khác đã lấy (`python3 scripts/status.py` báo số dùng hai lần).
2. Ghi đủ dòng: Ngày, nguồn; Loại, phạm vi; Chuyện gì (kèm bằng chứng: log, PR, thời gian); Cải tiến (sửa tài liệu nào, sửa gì); Trạng thái `proposed`.
3. Loại `môi trường` có thông báo lỗi dò được: thêm dòng vào `docs/ops/failure-playbook.md` kèm tệp log thật trong `scripts/tests/playbook/`, kiểm bằng `scripts/failure-kind.sh --playbook <log>`.
4. Ở repo app, phạm vi `chung`: nhắc dev mở issue `from-app` về dự án mẫu (`docs/ops/from-app.md`, mục 2).

**apply**
1. Lấy các bài học `accepted` (hay các mã đã nêu). Chỉ người chuyển `accepted`: bài học còn `proposed` thì hỏi dev; dev đồng ý rõ thì ghi `accepted` theo lời dev và nêu lời đồng ý trong mô tả PR.
2. Làm thành một PR cải tiến, theo `CLAUDE.md`, mục Không được làm (file cấm sửa thì cần plan rủi ro cao đã duyệt chéo). Chạy `scripts/tests/run.sh` khi chạm `scripts/`, `scripts/check.sh` khi chạm code.
3. Gọi `/pr-review`. Sau khi merge, ghi `applied` và số PR ở dòng Áp dụng.
