# THEO DÕI TOKEN AI

Tài liệu này nói cách nhóm ước tính token khi lập plan và ghi lại số token thật mỗi task đã dùng trong phiên tự chạy. Mục đích là **lập kế hoạch**: biết một phiên chứa được bao nhiêu task, task cỡ nào tốn bao nhiêu, phần nào tốn bất thường để tối ưu. Ước tính **không bao giờ chặn** việc thực hiện: task vượt ước tính vẫn làm tiếp tới khi xong hoặc gặp điều kiện dừng thường lệ (`docs/risk-scoring.md`, mục 4).

## 1. Số liệu lấy từ đâu

Claude Code ghi mỗi phiên vào một transcript trên máy chạy phiên: `~/.claude/projects/<thư mục repo>/<phiên>.jsonl`, phiên phụ (rà soát độc lập, tìm kiếm) ở `<phiên>/subagents/`. Mỗi lần gọi model có một dòng `usage` với bốn loại token, kèm nhánh git lúc gọi.

| Loại | Nghĩa |
| --- | --- |
| Input | Token đầu vào mới, không qua cache |
| Ghi cache | Token đầu vào được đưa vào cache để dùng lại |
| Đọc cache | Token đầu vào đọc lại từ cache (ngữ cảnh đã có của phiên). Thường chiếm hơn 90% tổng |
| Output | Token model viết ra, kể cả phần suy nghĩ |

**Tổng** là tổng bốn loại, đếm nguyên, không quy ra tiền. Một lần gọi model ghi nhiều dòng trong transcript; script chỉ tính một lần cho mỗi request.

**Gán cho task theo nhánh:** request trên nhánh `claude/T-NNN-...` hoặc `claude/fix-T-NNN-...` thuộc task T-NNN, kể cả phiên rà soát độc lập chạy trên nhánh đó. Mọi request khác (kiểm tra đầu phiên, nhánh báo cáo, emulator, viết báo cáo) vào dòng **Chung**.

## 2. Ước tính khi viết plan

- Plan ghi `token_estimate` ở phần đầu, dạng `20M` (triệu token tổng), cạnh cỡ `size` của ticket.
- Lấy số gợi ý bằng `scripts/token-usage.py estimate`: trung vị tổng token của các task đã đo cùng cỡ, đọc từ `docs/metrics/token-usage.csv`. Cỡ nào chưa đủ 5 task đã đo thì dùng số khởi đầu: S 10M, M 20M, L 40M. Các số này đặt tạm, sẽ hiệu chỉnh khi có dữ liệu.
- Task có điểm khác thường (nhiều màn hình, nhiều vòng CI dự kiến, đọc nhiều tài liệu) thì chỉnh số gợi ý và ghi lý do một dòng trong mục 1 của plan.
- `scripts/plan-check.py` chỉ nhắc (CHÚ Ý) khi thiếu `token_estimate`, không báo lỗi; plan thiếu ước tính vẫn chạy.

## 3. Ghi số thật cuối phiên

Ở bước viết báo cáo (`docs/auto-run.md`, mục 4, bước 2), phiên chạy:

```bash
scripts/token-usage.py report --since <started_at> --run RUN-YYYYMMDD-NN --csv docs/metrics/token-usage.csv
```

- Dán bảng in ra vào mục Token của báo cáo phiên. Bảng có ước tính, số thật, tỉ lệ thật trên ước tính và bốn loại token cho từng task.
- Script thêm các dòng của phiên vào `docs/metrics/token-usage.csv`; chạy lại thì thay các dòng cũ của cùng phiên. File CSV đi qua PR báo cáo.
- Không tìm thấy transcript (ví dụ môi trường đổi nơi lưu), script in "Không đo được" và trả mã 0. Phiên ghi "không đo được" kèm thông báo đó vào báo cáo và đi tiếp.
- Task lệch ước tính từ 2 lần trở lên, cao hay thấp: ghi một dòng lý do có thể (nhiều vòng sửa, CI đỏ, đọc lại tài liệu lớn) trong mục Token của báo cáo. Đây là đầu vào cho retro, không phải lỗi.

Phiên có người theo dõi (viết spec, plan, retro) cũng dùng được lệnh này với `--since` để xem mức dùng, không cần `--run`.

## 4. Dùng số liệu ở retro và họp đánh giá rủi ro

- **Lập danh sách chạy:** cộng `token_estimate` của các plan để xem danh sách có vừa một phiên không, so với tổng thật của các phiên trước.
- **Hiệu chỉnh:** mỗi vài phiên, chạy `scripts/token-usage.py estimate`; số trung vị theo cỡ thay cho số khởi đầu.
- **Tối ưu:** dòng Chung lớn nghĩa là chi phí cố định của phiên (đọc tài liệu đầu phiên, chờ CI, emulator) cao. Đọc cache lớn nghĩa là ngữ cảnh dài: tài liệu luôn được nạp (`CLAUDE.md`, `docs/CLAUDE.md`, `docs/auto-run.md`) và kết quả lệnh dài. Cách giảm được đề xuất như bài học (`docs/auto-run.md`, mục 6), không tự sửa trong phiên.

## 5. Giới hạn

- Chỉ đo được trên máy có transcript. Phiên tự chạy trên cloud đo trong chính môi trường đó, nên phải chạy lệnh trước khi phiên kết thúc; request sau lúc chạy lệnh (đẩy báo cáo, merge) không được tính.
- Token của việc viết spec và plan trong phiên có người theo dõi không gán vào task; chỉ phần thực hiện trong phiên tự chạy được tính.
- Tổng token không phản ánh đúng mức dùng hạn mức, vì mỗi loại token tính khác nhau. Nhóm đã chọn đếm nguyên, không quy đổi.
