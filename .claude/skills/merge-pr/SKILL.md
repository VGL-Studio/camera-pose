---
name: merge-pr
description: Merge một PR chỉ bằng scripts/merge-if-green.sh, chờ CI bằng wait-run.sh, tra failure-kind.sh khi đỏ, rồi chờ main-guard của commit merge.
argument-hint: <số PR>
---

# /merge-pr: merge qua cổng chất lượng

Tham số: `$ARGUMENTS` (số PR). Thiếu thì hỏi.

1. Đọc `docs/auto-run.md`, bước 8 và 9. Không merge bằng nút, `gh pr merge` hay API; không đóng, sửa hay bình luận issue `main-broken`.
2. Chờ CI của commit đầu: `scripts/wait-run.sh --pr <số>`, thêm `--sha "$(git rev-parse HEAD)"` khi chính phiên vừa đẩy (chạy nền nếu có việc khác). Mã 2 (hết giờ chờ): gọi lại. Mã 1 (đỏ): `gh run view <id> --log-failed > "${TMPDIR:-/tmp}/ci.log"; [ -s "${TMPDIR:-/tmp}/ci.log" ] || gh run view <id> > "${TMPDIR:-/tmp}/ci.log"` (log rỗng thì lấy tóm tắt có chú thích của job, nơi duy nhất có câu hết giờ, L-138), rồi `scripts/failure-kind.sh --playbook "${TMPDIR:-/tmp}/ci.log"`, làm theo dòng in ra, báo dev trước khi sửa code. Mã 4 (hạ tầng), mã 5 (xung đột): theo bước 8.
3. Xanh thì `scripts/merge-if-green.sh <số>`. Mã 3 (nhánh chậm hơn nhánh chính): `scripts/merge-if-green.sh <số> --update` ở chế độ nền. Mã 2 (check còn chạy): gọi lại sau. Script in `ĐÃ MERGE (GitHub báo lỗi)` hay `ĐÃ MERGE TRƯỚC ĐÓ`: làm theo bước 9, không merge lại. Script từ chối vì lý do khác: báo dev nguyên văn, không tìm cách vượt.
4. Sau khi merge: PR đổi code thì `scripts/wait-run.sh --workflow main-guard.yml --since <giờ merge UTC>`; PR chỉ đổi tài liệu thì `main-guard` không chạy. Rồi kiểm không có issue `main-broken` đang mở.
5. Báo dev: commit merge, kết quả `main-guard`. Gợi ý dọn worktree và nhánh cục bộ của PR.
