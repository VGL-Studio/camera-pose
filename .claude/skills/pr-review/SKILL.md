---
name: pr-review
description: Rà soát độc lập một PR hay nhánh bằng agent con ngữ cảnh mới, theo nội dung rà soát của auto-run.md bước 6, rồi xử lý theo ba mức chặn, nên sửa, ghi chú. Dùng trước khi báo bất kỳ PR nào là xong, kể cả PR tài liệu, ADR, quy trình.
argument-hint: "[số PR hoặc nhánh]"
---

# /pr-review: rà soát độc lập trước khi báo PR xong

Tham số: `$ARGUMENTS` (số PR hay nhánh; mặc định nhánh hiện tại so với `origin/main`).

1. Đọc `docs/auto-run.md`, bước 6 (nội dung rà soát, ba mức, cách xử lý).
2. Gom đầu vào: `git diff origin/main...HEAD` (hay diff của PR), plan và spec liên quan, ADR mà thay đổi chạm vào, và với task có plan: thay đổi ở file test sau commit test, danh sách test bị xóa, phần ngoài mục 3.2 kèm lý do.
3. Giao một agent con ngữ cảnh mới, chỉ đọc. Lời giao gồm: đường dẫn các file trên, nội dung rà soát của bước 6, yêu cầu trả từng điểm kèm mức và file, dòng. Không đưa quá trình làm việc hay kết luận của mình. Task có ticket thì lời giao có dòng `Task: T-NNN`.
4. Xử lý kết quả:
   - **Chặn:** sửa rồi rà soát lại phần vừa sửa.
   - **Nên sửa:** sửa khi trong phạm vi được phép, có test thất bại trước nếu là code; không thì ghi lại.
   - **Ghi chú:** ghi vào mô tả PR.
   - Mỗi task tối đa 2 vòng rà soát lại vì điểm nên sửa.
5. Ghi vào mô tả PR: kết quả rà soát (số điểm mỗi mức), "Đã sửa theo rà soát", điểm nên sửa chưa sửa và lý do.
6. Chỉ sau bước này mới báo dev PR đã xong.
