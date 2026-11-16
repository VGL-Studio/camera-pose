---
name: merge-pr
description: Merge một PR chỉ bằng scripts/merge-if-green.sh, chờ CI bằng wait-run.sh, tra failure-kind.sh khi đỏ, rồi chờ main-guard của commit merge.
argument-hint: <số PR>
---

# /merge-pr: merge qua cổng chất lượng

Tham số: `$ARGUMENTS` (số PR). Thiếu thì hỏi.

1. Đọc `docs/auto-run.md`, bước 8 và 9. Không merge bằng nút, `gh pr merge` hay API; không đóng, sửa hay bình luận issue `main-broken`. PR có spec `confirmed`, plan hay danh sách chạy `approved`, ADR `accepted` do người tự ghi (không phải AI ghi theo lời đồng ý trong phiên này): không merge khi người chưa nói lại lời đồng ý trong phiên; báo dev để người merge (`docs/CLAUDE.md`, mục Những điều không được làm).
2. Chờ CI của commit đầu: `scripts/wait-run.sh --pr <số>`, thêm `--sha "$(git rev-parse HEAD)"` khi chính phiên vừa đẩy (chạy nền nếu có việc khác). Mã 2 (hết giờ chờ): gọi lại; dòng kết quả "chưa thấy lần chạy nào" thì theo bước 8. Mã 1 (đỏ): `gh run view <id> --log-failed > "${TMPDIR:-/tmp}/ci.log"; [ -s "${TMPDIR:-/tmp}/ci.log" ] || gh run view <id> > "${TMPDIR:-/tmp}/ci.log"` (log rỗng thì lấy tóm tắt có chú thích của job, nơi duy nhất có câu hết giờ), rồi `scripts/failure-kind.sh --playbook "${TMPDIR:-/tmp}/ci.log"`, làm theo dòng in ra, báo dev trước khi sửa code. Mã 3 với `--sha` (mã commit sai hay chưa lên GitHub), mã 4 (hạ tầng), mã 5 (xung đột): theo bước 8. Mã 6 (PR nháp, `pr-check` không chạy): báo dev; người chuyển PR sang sẵn sàng rồi gọi lại. Mã 7 (`CHỜ RUNNER`, job chờ runner tự host): không chạy lại, không sửa code; báo dev kiểm máy runner hay xóa biến `HEAVY_RUNNER` của repo (`docs/ops/self-hosted-runner.md`, mục 8).
3. Xanh thì `scripts/merge-if-green.sh <số>`. Mã 3 (nhánh chậm hơn nhánh chính): `scripts/merge-if-green.sh <số> --update` ở chế độ nền, với `timeout` của công cụ lớn hơn 30 phút (`docs/auto-run.md` bước 8, mục Chờ workflow). Mã 2 (check còn chạy): gọi lại sau; cùng một thông báo lặp lại nhiều lần thì tra `scripts/failure-kind.sh --playbook` với đầu ra của script. Script in `KHÔNG CẬP NHẬT NHÁNH`: nhánh chính chỉ đổi tài liệu từ điểm tách, script merge không cập nhật nhánh, `main-guard` của commit merge chạy `scripts/tests/run.sh` (ADR-0041). Script in `ĐÃ MERGE (GitHub báo lỗi)` hay `ĐÃ MERGE TRƯỚC ĐÓ`: làm theo bước 9, không merge lại. Script từ chối vì lý do khác: báo dev nguyên văn, không tìm cách vượt.
4. Sau khi merge: PR đổi code thì `scripts/wait-run.sh --workflow main-guard.yml --since <giờ merge UTC>`; PR chỉ đổi tài liệu thì `main-guard` không chạy. Lần chờ này trả mã 7: như bước 2. Rồi kiểm không có issue `main-broken` đang mở.
5. Báo dev: commit merge, kết quả `main-guard`. Gợi ý dọn worktree và nhánh cục bộ của PR.
