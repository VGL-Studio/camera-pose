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

**Transcript nào thuộc repo:** dòng nào có thư mục làm việc là thư mục repo, một worktree của repo (`git worktree list`), hoặc một thư mục mang tên repo (tên lấy từ remote `origin`, không có thì lấy tên thư mục; đặt `TOKEN_USAGE_REPO` để chọn tên khác). Nhờ vậy chạy script trong worktree của nhánh báo cáo (`../report`) vẫn thấy transcript của thư mục chính và ngược lại (L-008 của camera-pose).

**Gán cho task theo nhánh:** request trên nhánh `claude/T-NNN-...` hoặc `claude/fix-T-NNN-...` thuộc task T-NNN, kể cả phiên rà soát độc lập chạy trên nhánh đó. Phiên phụ làm trong worktree bằng đường dẫn vẫn ghi nhánh của phiên chính, nên lời giao việc có một dòng riêng `Task: T-NNN`: mọi request của transcript trong `subagents/` có dòng đó ở tin nhắn đầu thuộc task T-NNN (L-071). Lời giao của phiên con soạn plan cuối phiên không có dòng này, nên token soạn plan vào dòng Chung (L-102). Việc cuối phiên chạy khi thư mục chính đã rời nhánh task (`docs/auto-run.md`, mục 4, bước 0), nên cũng vào dòng Chung (L-103). Phiên chính làm task khi thư mục chính ở HEAD tách rời (phiên con giữ nhánh task trong worktree) thì chạy `scripts/token-usage.py mark T-NNN` khi bắt đầu hay chuyển sang task đó và `mark chung` khi không còn task nào đang làm (`docs/auto-run.md`, bước 1 và 10). Lệnh chỉ in một dòng; `report` đọc lại dòng lệnh trong transcript của phiên chính và gán mọi request của phiên chính không ở nhánh task nào, từ dấu mốc đó tới dấu mốc kế tiếp, cho task. Dấu mốc ở transcript trước vẫn tính cho phiên tiếp tục trong cùng môi trường; dấu mốc trước `--since` không tính (`--since` như `mark chung`); dấu mốc trong transcript của phiên con không được tính (camera-pose L-083). Mọi request khác (kiểm tra đầu phiên, nhánh báo cáo, emulator, viết báo cáo) vào dòng **Chung**.

## 2. Ước tính khi viết plan

- Plan ghi `token_estimate` ở phần đầu, dạng `20M` (triệu token tổng), cạnh cỡ `size` của ticket.
- Lấy số gợi ý bằng `scripts/token-usage.py estimate`: trung vị tổng token của các task đã đo cùng cỡ, đọc từ `docs/metrics/token-usage.csv`. Task chạy qua nhiều phiên (dừng rồi tiếp tục) có một dòng mỗi phiên; `estimate` cộng các dòng đó thành một mẫu (L-111). Cỡ nào chưa đủ 5 task đã đo thì dùng số khởi đầu: S 5M, M 15M, L 30M. Các số này đặt tạm, sẽ hiệu chỉnh khi có dữ liệu.
- Số khởi đầu cũ (S 10M, M 20M, L 40M) quá cao so với số đo của sáu phiên đầu ở camera-pose (L-021 của app): trung vị S khoảng 5M (2 task), M khoảng 15M (5 task), L khoảng 11M (10 task, từ 2M tới 60M). Task L xong trọn trong một phiên thường tốn khoảng 40M, task L dừng giữa chừng chỉ vài triệu, nên số L để 30M thay cho trung vị.
- Task có điểm khác thường (nhiều màn hình, nhiều vòng CI dự kiến, đọc nhiều tài liệu) thì chỉnh số gợi ý và ghi lý do một dòng trong mục 1 của plan.
- `scripts/plan-check.py` chỉ nhắc (CHÚ Ý) khi thiếu `token_estimate`, không báo lỗi; plan thiếu ước tính vẫn chạy.

## 3. Ghi số thật cuối phiên

Ở bước viết báo cáo (`docs/auto-run.md`, mục 4, bước 2), phiên chạy:

```bash
scripts/token-usage.py report --since <started_at> --run RUN-YYYYMMDD-NN --csv docs/metrics/token-usage.csv
```

- Dán bảng in ra vào mục Token của báo cáo phiên. Bảng có ước tính, số thật, tỉ lệ thật trên ước tính và bốn loại token cho từng task.
- Script thêm các dòng của phiên vào `docs/metrics/token-usage.csv`; chạy lại thì thay các dòng cũ của cùng phiên. File CSV đi qua PR báo cáo.
- Phiên mở bằng `claude --cloud` có dùng cloud session credits: sau phiên, người ghi tiền đã dùng (số dư trước trừ số dư sau, đô la Mỹ) bằng `scripts/token-usage.py credit --run RUN-YYYYMMDD-NN --usd <số>` (`docs/ops/staging-and-runs.md`, mục 2.7). Số này nằm ở cột `credit_usd` của dòng Chung, một lần cho cả phiên; chạy lại `report` không làm mất nó. File CSV cũ chưa có cột này vẫn đọc được, lần ghi sau thêm cột với giá trị trống.
- Không tìm thấy transcript (ví dụ môi trường đổi nơi lưu), script in "Không đo được" và trả mã 0. Phiên ghi "không đo được" kèm thông báo đó vào báo cáo và đi tiếp.
- Task lệch ước tính từ 2 lần trở lên, cao hay thấp: ghi một dòng lý do có thể (nhiều vòng sửa, CI đỏ, đọc lại tài liệu lớn) trong mục Token của báo cáo. Đây là đầu vào cho retro, không phải lỗi.

Phiên có người theo dõi (viết spec, plan, retro) cũng dùng được lệnh này với `--since` để xem mức dùng, không cần `--run`.

**Đường làm nhanh** (ADR-0034, [quick-lane.md](quick-lane.md)): phiên chính chạy `scripts/token-usage.py mark T-NNN` khi bắt đầu task và `mark chung` khi xong. Nhánh `claude/quick-T-NNN-...` không phải nhánh task theo mục 1, nên request của phiên chính trên nhánh đó tính vào task nhờ dấu mốc; lời giao cho agent rà soát có dòng `Task: T-NNN`. Xong task thì chạy `report --since <giờ bắt đầu> --run QUICK-YYYYMMDD-T-NNN --csv docs/metrics/token-usage.csv`; file CSV đi cùng PR tài liệu kế tiếp. `estimate` đọc các dòng này như dòng của phiên tự chạy. Chạy `mark T-NNN` ngay khi có mã ticket thì token viết ticket và plan cũng tính vào task, khác với phiên tự chạy; phần trước đó vào dòng Chung. Mã ticket phải đổi vì trùng với PR song song thì chạy lại `mark` với mã mới; request trước đó vẫn nằm ở mã cũ. Agent con không có dòng `Task: T-NNN` thì vào dòng Chung, vì dấu mốc chỉ áp cho phiên chính.

## 4. Dùng số liệu ở retro và họp đánh giá rủi ro

- **Lập danh sách chạy:** cộng `token_estimate` của các plan để xem danh sách có vừa một phiên không, so với tổng thật của các phiên trước.
- **Hiệu chỉnh:** mỗi vài phiên, chạy `scripts/token-usage.py estimate`; số trung vị theo cỡ thay cho số khởi đầu.
- **Credit:** khi có phiên ghi `credit_usd`, `estimate` in thêm số USD trung bình cho mỗi 1M token của các phiên đó. Nhân với tổng `token_estimate` của danh sách để biết phiên tốn khoảng bao nhiêu credit (camera-pose: khoảng 10 USD cho 24M token).
- **Tối ưu:** dòng Chung lớn nghĩa là chi phí cố định của phiên (đọc tài liệu đầu phiên, chờ CI, emulator) cao. Đọc cache lớn nghĩa là ngữ cảnh dài: tài liệu luôn được nạp (`CLAUDE.md`, `docs/CLAUDE.md`, `docs/auto-run.md`) và kết quả lệnh dài. Cách giảm được đề xuất như bài học (`docs/auto-run.md`, mục 6), không tự sửa trong phiên.

## 5. Giới hạn

- Chỉ đo được trên máy có transcript. Phiên tự chạy trên cloud đo trong chính môi trường đó, nên phải chạy lệnh trước khi phiên kết thúc; request sau lúc chạy lệnh (đẩy báo cáo, merge) không được tính.
- Token của việc viết spec và plan trong phiên có người theo dõi không gán vào task; chỉ phần thực hiện trong phiên tự chạy được tính.
- Tổng token không phản ánh đúng mức dùng hạn mức, vì mỗi loại token tính khác nhau. Nhóm đã chọn đếm nguyên, không quy đổi. Cột `credit_usd` là số tiền thật người đọc từ số dư, không phải số script tính từ token.
- Việc chung mà phiên chính làm trong lúc một task có dấu mốc (chờ `main-guard` của task trước, `check.sh` đầu phiên chạy song song với bước 1 đến 3 của task đầu) tính vào task đó. Phiên chính quên `mark` khi chuyển giữa hai task thì phần đó tính vào task của dấu mốc trước.
- Phiên `claude --cloud` mở lại trong môi trường mới không còn transcript của phần trước: phần token đó mất, cùng với dấu mốc của nó; phiên tiếp tục chạy lại `mark` cho task đang làm dở.
- Dòng lệnh có tên `scripts/token-usage.py mark T-NNN` đứng sau khoảng trắng mà không nằm ngay sau dấu nháy (ví dụ trong heredoc của commit hay mô tả PR) cũng được đọc là dấu mốc. Trong chữ viết, đặt tên lệnh trong dấu nháy ngược hay dấu nháy.
- Các dòng của `docs/metrics/token-usage.csv` ghi trước T-063 thiếu phần phiên chính của task có phiên con (camera-pose L-083), nên trung vị của `estimate` thấp hơn thật cho tới khi đủ task đo sau T-063.
