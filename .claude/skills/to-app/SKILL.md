---
name: to-app
description: Đồng bộ một đợt thay đổi của dự án mẫu về một app bằng scripts/template-sync.sh (ADR-0030), gộp xung đột theo bảng phân loại file, kiểm và mở PR trong repo app. Chạy ở repo dự án mẫu khi có người theo dõi.
argument-hint: <thư mục app> [--to <commit>] [--take <đường dẫn>]
disable-model-invocation: true
---

# /to-app: đưa dự án mẫu về một app

Tham số: `$ARGUMENTS` (thư mục repo app, tùy chọn `--to <commit>`, `--take <đường dẫn>`).

1. Kiểm đang ở repo dự án mẫu (có `docs/ops/new-app.md`). Đọc `docs/ops/to-app.md` (bảng phân loại file ở mục 2, các bước ở mục 4, phần giữ lại ở mục 5, giá trị riêng ở mục 6).
2. **Bước 1, phần chung của app về trước:** liệt kê bài học `chung` chưa có issue và commit của app ở file "của dự án mẫu" kể từ mốc `base=` trong `docs/template-sync.txt` của app. Phần nào chưa về thì gợi ý `/from-app new` trong repo app, hoặc ghi lại để giữ khi gộp. Báo dev trước khi chạy tiếp.
3. Chỉ đồng bộ giữa hai phiên tự chạy của app (mục 1): repo app không có nhánh `claude/RUN-*-report` nào có `status: running`, và đợt này merge xong trước phiên sau. Repo app sạch, ở nhánh chính mới nhất; dự án mẫu `git fetch`.
   Phần lớn (thay gần hết một module, cần SDK hay quyền mới, đổi ảnh chuẩn): giữ lại bằng `hold=` rồi đưa vào bằng plan với `--take`, theo mục 5.
4. `scripts/template-sync.sh --app <thư mục app> [--to <commit>] [--take <đường dẫn>]`. Đợt `--take` tạo nhánh `template-sync/take-...`, là điểm bắt đầu của plan trong app (mục 5, bước 3, 4).
5. Xung đột: gộp theo cột cuối của bảng ở mục 2; giá trị riêng của app đặt lại theo mục 6. Rồi `python3 scripts/foundation.py`, `git add` từng file, `git commit`.
6. Kiểm trong app: `scripts/tests/run.sh`; `scripts/check.sh` khi đợt có code.
7. Hỏi dev trước khi đẩy nhánh và mở PR trong repo app (repo khác). Mô tả PR: khoảng commit, cách gộp từng file xung đột, phần riêng của app bị thay. Người phụ trách app duyệt và merge bằng `scripts/merge-if-green.sh` của app.
