---
ticket: T-039
title: Bảng tra lỗi đã biết và việc phải làm
status: done             # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 12.9M        # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-24
---

# PLAN T-039: BẢNG TRA LỖI ĐÃ BIẾT VÀ VIỆC PHẢI LÀM

## 1. Tóm tắt

Tạo `docs/ops/failure-playbook.md`: một bảng, mỗi dòng gồm mẫu thông báo lỗi đã gặp thật (biểu thức chính quy), loại lỗi, việc phiên phải làm, và bài học nguồn. Bảng ban đầu có 10 dòng, lấy từ các báo cáo phiên và bài học L-002 đến L-054.

`scripts/failure-kind.sh` có thêm chế độ `--playbook <log>`: đọc bảng, tìm dòng đầu tiên khớp với log, in mã dòng, loại và việc phải làm. Chế độ cũ (`failure-kind.sh <build.log>`, in `tests` hay `other` cho `main-guard.yml`) giữ nguyên từng chữ.

`docs/auto-run.md` bước 8 bảo phiên tra bảng trước khi sửa code khi CI đỏ. Mục 6 bảo mỗi bài học môi trường mới thêm một dòng vào bảng.

Kiểm chứng: mỗi dòng của bảng có một test dùng đoạn log thật, kèm một test kiểm mọi dòng đều có test, và mọi test cũ của `failure-kind.sh` qua mà không sửa.

**Phiên bản 2** sửa theo rà soát độc lập trước RUN-20261024-01:
- PB-01 có đường lui khi lệnh chạy lại job bị proxy của môi trường cloud từ chối (403): đẩy một commit rỗng lên nhánh task, như `auto-run.md` mục 4 đã làm cho `pr-check`.
- Bước 8 có đường lui khi không tải được log của job (`actions/jobs/<id>/logs` chuyển hướng sang kho lưu trữ khác, có thể bị proxy chặn như artifact, L-030): dùng đầu ra của `wait-run.sh` và xử lý như `khong-co`.
- PB-05 thêm mẫu cho Java ngoài khoảng (`Java NN không chạy được Gradle`, `java-check.sh` dòng 42); sửa chữ ở bảng cột (dấu `\|` đổi thành `|`).
- PB-08 thống nhất với `auto-run.md` bước 8 do T-038 viết (T-038 làm trong phiên có người theo dõi trước phiên): một cách nói, dừng task theo điều kiện 1 khi luật R8 cần ở file ngoài mục 3.2. T-039 giữ dòng PB-08; không plan nào khác sửa dòng này.
- Phối hợp: T-038, T-041 không còn trong danh sách chạy; ADR-1003 đã có hiệu lực (`auto-run.md` bước 4, 5, 6, 10 đổi ở PR #91), các đoạn plan này sửa không đổi chữ.

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là bài học L-057 (`proposed`), cùng L-050 và L-054 mà bảng xử lý.

| Yêu cầu | Tiêu chí | Cách hiểu của AI |
| --- | --- | --- |
| Một chỗ để tra | Khi CI hay một script đỏ, phiên chạy `scripts/failure-kind.sh --playbook <log>` và nhận một dòng: `<mã> <loại>: <việc phải làm> (<bài học>)` | Bảng nằm trong tài liệu để người đọc được; script đọc chính bảng đó, không có bản sao thứ hai để lệch nhau |
| Lỗi đã biết không tốn vòng sửa | Log `ui-test` có `VGL.Loading: Task STARTER_SET failed` hay `VGL.Assets: Could not download ... reason=NETWORK` thì loại `chay-lai`: chạy lại job đỏ, không tính vào 3 lần sửa (L-050) | Lần chạy 39997301164: lần 1 có `Task STARTER_SET failed` mà không có dòng `VGL.Assets`; lần 2 có cả ba dòng `Could not download ... SocketTimeoutException`. Mẫu phải khớp cả hai (mục 4.1) |
| Không khớp thì như cũ | Log không khớp dòng nào thì in `khong-co: ...` và phiên xử lý như lỗi thật (bước 8, tối đa 3 lần sửa) | Bảng chỉ thêm lối tắt cho lỗi đã biết, không đổi cách xử lý lỗi chưa biết |
| Giữ hành vi của `main-guard` | `failure-kind.sh <build.log>` (không có `--playbook`) in đúng `tests` hay `other` như bây giờ cho mọi log | `main-guard.yml` dòng 54 gọi chế độ này (ADR-1002, điểm 1). 8 test hiện có trong `scripts/tests/run.sh` qua mà không sửa |
| Bảng lớn dần | Mỗi bài học loại `môi trường` mới có dòng trong bảng, trừ khi lỗi không có thông báo để dò | `auto-run.md` mục 6 và test "mọi dòng đều có test" giữ bảng có kiểm chứng |

Thiết kế tham chiếu: không có thiết kế riêng.

ADR liên quan: ADR-1002 (điểm 1: `failure-kind.sh` cho `main-guard`; plan không đổi điểm này), ADR-0010 (cổng chất lượng).

## 3. Giải pháp

### 3.1. Cách làm

**`docs/ops/failure-playbook.md` (mới).** Một đoạn mở đầu, rồi bảng giữa hai dấu `<!-- playbook:start -->` và `<!-- playbook:end -->`. Script chỉ đọc phần giữa hai dấu này.

| Cột | Nội dung |
| --- | --- |
| Mã | `PB-NN`, không dùng lại khi xóa dòng |
| Mẫu | Biểu thức chính quy kiểu `grep -E`, trong dấu backtick. Dấu `\|` trong mẫu viết `\|` (Markdown); script đổi thành `|` trước khi dò |
| Thấy ở | Log hay đầu ra nào (job `ui-test`, `check.sh`, `merge-if-green.sh`, `wait-run.sh`, chú thích của check run) |
| Loại | Một trong: `chay-lai`, `lich-thu-lai`, `cho`, `gop-nhanh`, `kiem-da-merge`, `moi-truong`, `nguoi`, `that` |
| Việc phải làm | Câu ngắn, tiếng Việt, đủ để làm ngay |
| Nguồn | Bài học, lần chạy |

Bảng ban đầu, theo thứ tự dò (dòng cụ thể trước dòng chung):

| Mã | Mẫu | Thấy ở | Loại | Việc phải làm | Nguồn |
| --- | --- | --- | --- | --- | --- |
| PB-01 | `VGL\.Loading: Task [A-Z_]+ failed\|VGL\.Assets: Could not download .*reason=NETWORK` | job `ui-test` | `chay-lai` | Tải file R2 hết giờ chờ trên runner. Chạy lại job đỏ (`gh api -X POST "repos/{owner}/{repo}/actions/runs/<id>/rerun-failed-jobs"`), tối đa 2 lần, không tính vào 3 lần sửa. Lệnh chạy lại bị proxy từ chối (403): đẩy một commit rỗng lên nhánh task (`git commit --allow-empty`, `auto-run.md` mục 4), tính như một lần chạy lại. Lần thứ 3 vẫn đỏ cùng mẫu: PR không đổi `core/assets`, `core/poses`, `feature/loading` thì ghi vào báo cáo và dừng task, không sửa code; PR có đổi thì xử lý như lỗi thật | L-050, lần chạy 39997301164 |
| PB-02 | `HẠ TẦNG:\|The job was not started because` | `wait-run.sh` mã 4, chú thích của check run | `cho` | GitHub không cấp máy. Làm theo `auto-run.md` mục 4, dòng "GitHub không chạy job" | L-046, lần chạy 39916343646 |
| PB-03 | `XUNG ĐỘT:\|merge conflict between base and head \(HTTP 422\)` | `wait-run.sh` mã 5, `merge-if-green.sh --update` | `gop-nhanh` | `git merge origin/main`, sửa xung đột trong file của plan, đẩy, chờ CI lại. Không tính vào 3 lần sửa | L-047, L-048 |
| PB-04 | `unexpected end of JSON input` | `merge-if-green.sh` | `kiem-da-merge` | Không gọi lại merge. Tìm trên `origin/main` commit có `(#<số PR>)` ở cuối tiêu đề (`git log origin/main --oneline -20`). Có: coi là đã merge, chờ `main-guard` (nếu không có lần chạy sau 10 phút thì ghi báo cáo để người chạy tay), ghi để người đóng PR. Không có: làm như `TỪ CHỐI` (bước 9) | L-054, PR #67 |
| PB-05 | `không đọc được phiên bản Java\|không chạy được java\|Java [0-9]+ không chạy được Gradle` | `check.sh` (`java-check.sh`) | `moi-truong` | Môi trường build hỏng, không phải lỗi của code. Không sửa script. Kết thúc phiên, báo cáo nguyên văn dòng lỗi và `java -version` | L-049, L-022 |
| PB-06 | `CONNECT tunnel failed, response 403` | lệnh `curl` trong môi trường cloud | `moi-truong` | Proxy cloud chặn máy chủ. Không thử lại, không tìm đường khác. Kiểm việc đó bằng workflow trên GitHub, hoặc ghi cho người kiểm | L-005 |
| PB-07 | `No matching client found for package name` | `staging.yml`, `device-session.yml` | `nguoi` | Bí mật Firebase staging là file giả. Không phát staging; ghi vào Cần người kiểm tra | L-002, lần chạy 39589924059 |
| PB-08 | `Missing classes detected while running R8\|Missing class .* \(referenced from` | build bản release | `that` | Lỗi thật của task vừa thêm thư viện, không phải lỗi hạ tầng (`auto-run.md` bước 8, gạch đầu dòng R8 của T-038). Thêm `-dontwarn` hay `-keep` theo `missing_rules.txt` vào `consumer-rules.pro` hay `proguard-rules.pro` có trong mục 3.2, tính vào 3 lần sửa. File cần sửa không có trong mục 3.2 thì dừng task theo điều kiện 1 (luật R8 không phải phần nối chỗ gọi của ADR-1003); nhánh và PR giữ nguyên cho người | L-053, lần chạy 40031369521 |
| PB-09 | `Could not (GET\|HEAD\|resolve\|download) .*(status code (429\|5[0-9][0-9])\|timed out)\|Read timed out\|Connect timed out\|Connection reset` | `check.sh` (Gradle tải thư viện) | `lich-thu-lai` | Lỗi tải thư viện. Chạy lại theo lịch 1, 2, 4, 8, 15 phút của bước 5, không tính vào 3 lần sửa. Hết lịch thì kết thúc phiên (mục 4) | `auto-run.md` bước 5 |
| PB-10 | `ComposeTimeoutException: Condition still not satisfied` | job `ui-test` | `that` | Test trên emulator chờ hết giờ. Tra PB-01 trước (dòng trên khớp thì là R2). Không khớp: lỗi thật, đọc bước "Chi tiết test trên emulator" (bước 8) | L-050 (lần 1 có cả hai mẫu) |

PB-10 đặt sau PB-01. Log lần 1 của 39997301164 có cả `ComposeTimeoutException` và `Task STARTER_SET failed`, và phải ra PB-01.

**`scripts/failure-kind.sh --playbook <log> [<bảng>]`:**

- Tham số đầu là `--playbook`. Không có thì đi đúng đường cũ: không đổi dòng nào của phần cũ, chỉ thêm một khối `if` ở đầu file.
- Đọc bảng: `docs/ops/failure-playbook.md` tính từ gốc repo (`$(dirname "$0")/..`), hoặc đường dẫn thứ hai (dùng cho test).
- Lấy các dòng bảng giữa hai dấu, bỏ hai dòng tiêu đề. Tách cột theo `|` không đứng sau `\`. Lấy mẫu trong backtick và đổi `\|` thành `|`. Phần này viết bằng `python3 -I` trong heredoc: `scripts/tests/run.sh` đã cần python3, và bash tách cột Markdown có `\|` rất dễ sai.
- Với từng dòng theo thứ tự, `grep -Eq -- "<mẫu>" <log>`. Dòng đầu tiên khớp thì in `PB-NN <loại>: <việc phải làm> (<nguồn>)`.
- Không khớp dòng nào: `khong-co: lỗi chưa có trong docs/ops/failure-playbook.md. Đọc log, xử lý như lỗi thật (auto-run.md bước 8, tối đa 3 lần sửa).`
- Log không có: `khong-co: không có log ...`.
- Luôn mã 0, như chế độ cũ.
- Mẫu không hợp lệ với `grep -E` (mã 2): in `LỖI BẢNG: PB-NN ...` ra stderr, bỏ dòng đó, dò tiếp. Test bắt lỗi này trước khi lên nhánh chính.

**Cách phiên lấy log.** `wait-run.sh` mã 1 chỉ in tên job đỏ. Bước 8 ghi lệnh lấy log của job đỏ, đã thử ở mục 4.1:
```bash
gh api "repos/{owner}/{repo}/actions/runs/<id>/jobs" --jq '.jobs[] | select(.conclusion == "failure") | .id'
gh api --allow-escape-sequences "repos/{owner}/{repo}/actions/jobs/<job id>/logs" > ci.log
scripts/failure-kind.sh --playbook ci.log
```
Không có `--allow-escape-sequences` thì `gh` không in log vì log có mã màu (mục 4.1). Địa chỉ `actions/jobs/<id>/logs` chuyển hướng sang kho lưu trữ khác của GitHub; proxy của môi trường cloud có thể chặn nơi đó như với artifact (L-030), mới thử trên máy dev. Lệnh tải lỗi hay tệp rỗng: dùng đầu ra của `wait-run.sh` (tên job đỏ, chú thích) làm log, chạy `failure-kind.sh --playbook` trên đó; vẫn không khớp thì xử lý như `khong-co`, và ghi "không tải được log job" vào báo cáo.

**`docs/auto-run.md`** (phần của plan này; T-036, T-037, T-038, T-040 sửa các đoạn khác, xem mục 6):

- Bước 5, gạch đầu dòng "Lỗi tải thư viện hay công cụ": thêm "(bảng tra lỗi, PB-09)".
- Bước 8, gạch đầu dòng "CI đỏ thì AI đọc log...": viết lại. CI đỏ (`wait-run.sh` mã 1):
  1. Lấy log của job đỏ bằng các lệnh trên, chạy `scripts/failure-kind.sh --playbook`, làm theo dòng in ra. Không tải được log: dùng đầu ra của `wait-run.sh`, không khớp thì như `khong-co` (trên).
  2. Loại `chay-lai`, `lich-thu-lai`, `cho`, `gop-nhanh`, `kiem-da-merge` không tính vào 3 lần sửa.
  3. `khong-co` và `that` thì như cũ: lỗi chập chờn chạy lại một lần, lỗi thật quay lại bước 4, tối đa 3 lần.
  4. Báo cáo phiên ghi mã dòng bảng của mỗi lần chạy lại (mục Số liệu, "Số lần thử lại vì lỗi hạ tầng").
- Mục 6 (bài học): bài học loại `môi trường` có thông báo lỗi dò được thì PR cải tiến thêm một dòng vào bảng, kèm test với đoạn log thật.

**`docs/ops/staging-and-runs.md`:** không sửa. Bảng tra lỗi là tài liệu riêng, `auto-run.md` trỏ tới.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `docs/ops/failure-playbook.md` | Bảng ở mục 3.1 (vào commit test của bước 3, vì test đọc bảng; L-025) |
| Sửa | `scripts/failure-kind.sh` | Chế độ `--playbook`; chú thích đầu file. Phần cũ không đổi |
| Sửa | `scripts/tests/run.sh` | Test ở mục 4, ngay sau khối `failure-kind` hiện có (sau dòng `failure-kind: missing log -> other`) |
| Tạo | `scripts/tests/playbook/` | Một tệp đoạn log cho mỗi dòng bảng, chép từ log thật (mục 4.1), tên `PB-NN.log`; cộng `PB-01-attempt1.log`, `PB-05-range.log` (vế thứ hai của một dòng) và `none.log` (log đỏ không khớp dòng nào) |
| Sửa | `docs/auto-run.md` | Bước 5 (một cụm từ), bước 8 (đoạn CI đỏ), mục 6 (một gạch đầu dòng) |
| Sửa | `docs/lessons.md` | Dòng Áp dụng của L-057, L-050 (phần bảng; phần sửa `AppLaunchTest` không thuộc plan này) |
| Sửa | `docs/features/quality-gate/plans/T-039-bang-tra-loi.md` | Nhật ký thực hiện |
| Sửa | `docs/features/quality-gate/tickets/T-039-bang-tra-loi.md` | Trạng thái |

Các file đọc file mà plan sửa (L-020):
- `.github/workflows/main-guard.yml` dòng 54 gọi `scripts/failure-kind.sh build.log` (không có `--playbook`), ghi `kind` cho bước chạy lại. Chế độ cũ giữ nguyên từng chữ; 8 test cũ chạy không sửa. Không sửa workflow.
- `docs/adr/1002-main-tu-hoi-phuc.md` mô tả chế độ cũ; không đổi, vì quy tắc của ADR không đổi.
- Plan T-028, T-030 nhắc `failure-kind.sh` trong phần đã làm (lịch sử), không sửa.
- `scripts/check.sh` chạy `scripts/tests/run.sh`. Test mới chỉ đọc tệp trong `scripts/tests/playbook/`, cần `python3` (đã cần).
- `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/resume-check.py`, `scripts/merge-if-green.sh`, `scripts/wait-run.sh` nhắc `docs/auto-run.md` trong chú thích hay thông báo, trỏ tới mục 1, 2, 4, bước 3, bước 9 và phần mã thoát của bước 8. Plan không đổi các đoạn đó: chỉ viết lại gạch đầu dòng "CI đỏ" của bước 8, giữ nguyên câu liệt kê mã thoát.
- `scripts/status.py` đọc `docs/lessons.md` (tiêu đề và dòng Trạng thái để đếm bài học) và nhắc `docs/auto-run.md` trong chú thích. Plan chỉ điền dòng Áp dụng của L-057, L-050, không đổi tiêu đề hay Trạng thái, nên trang tiến độ không đổi cách đếm.
- Các tệp `scripts/tests/playbook/*.log` có dòng log của app; không có mã định danh máy, tên tài khoản hay dữ liệu người dùng (chỉ đường dẫn file R2 và tên task). `scripts/secret-scan.py` quét được thư mục này.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm.

### 3.4. Phương án dự phòng

Không có. Đọc bảng Markdown và `grep -E` là việc thường. Mục 4.1 đã thử mẫu PB-01 trên log thật.

## 4. Kiểm thử

Test trong `scripts/tests/run.sh`, viết trước code, ngay sau khối `failure-kind` hiện có. Bảng (`docs/ops/failure-playbook.md`) và các tệp log vào cùng commit test. Khung rỗng: `--playbook` chỉ in `khong-co: ...` cho mọi log, nên mọi test khớp dòng thất bại đúng lý do.

| Tiêu chí | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Mỗi dòng của bảng khớp log thật của nó | Test script | Mỗi dòng một test `playbook: PB-NN <tên ngắn>`, ví dụ `playbook: PB-01 R2 download timeout in ui-test -> chay-lai`. Đầu ra bắt đầu bằng `PB-NN <loại>:` |
| PB-01, cả hai lần của 39997301164 | Test script | `playbook: PB-01 also matches the first attempt (Task STARTER_SET failed, no Assets line)`: tệp `PB-01-attempt1.log` chỉ có dòng `VGL.Loading` và `ComposeTimeoutException`, ra PB-01 chứ không ra PB-10 (kiểm thứ tự dò) |
| PB-05, Java ngoài khoảng | Test script | `playbook: PB-05 also matches Java out of range`: tệp `PB-05-range.log` có dòng `LỖI: Java 26 không chạy được Gradle 8.14.3 của dự án (cần 17 tới 24).` (thông báo của `java-check.sh` dòng 42, chạy với `java` giả như test T-035); ra `PB-05 moi-truong:` |
| PB-10 khi không có dấu hiệu R2 | Test script | `playbook: PB-10 a UI timeout without R2 lines is a real failure`: chỉ có `ComposeTimeoutException`; ra `PB-10 that:` |
| Dấu `\|` trong mẫu | Test script | `playbook: an escaped pipe is an alternative`: bảng tạm (tham số thứ hai) có mẫu `aaa\|bbb`; log chỉ có `bbb` thì khớp |
| Không khớp | Test script | `playbook: an unknown failure says khong-co`: `none.log` (lỗi biên dịch Kotlin thật); đầu ra bắt đầu bằng `khong-co:`. Đối chứng (L-035): test này qua cả trên khung rỗng, nên chỉ có giá trị cùng các test dòng ở trên, vốn thất bại trên khung rỗng |
| Không có log | Test script | `playbook: a missing log says khong-co`; mã 0 |
| Mẫu hỏng không làm hỏng cả bảng | Test script | `playbook: a broken pattern is skipped and reported`: bảng tạm có một mẫu `(` và một mẫu đúng; đầu ra là dòng của mẫu đúng, stderr có `LỖI BẢNG` |
| Bảng thật không có mẫu hỏng | Test script | `playbook: every pattern of the real table is valid`: chạy với một log rỗng, stderr không có `LỖI BẢNG` |
| Mọi dòng đều có test | Test script | `playbook: every row has a log file`: mỗi `PB-NN` trong bảng có `scripts/tests/playbook/PB-NN.log`, và mỗi tệp `PB-NN*.log` có một dòng bảng cùng mã |
| Chế độ cũ không đổi | Test script | 8 test `failure-kind:` hiện có qua mà không sửa. Thêm `failure-kind: a playbook-only log still says other`: log `PB-01.log` qua chế độ cũ in `other` (chế độ cũ không đọc bảng) |
| Phiên dùng bảng | Kiểm tra thủ công | Người duyệt chéo đọc diff `docs/auto-run.md`. Lần R2 hết giờ chờ tới, báo cáo phiên ghi `PB-01` ở Số liệu |

Các tệp log lấy từ log thật, chỉ giữ các dòng cần (mục 4.1):
- PB-01 và PB-10 từ lần chạy 39997301164 (job 120564114832, 120569140220);
- PB-02 từ fixture của T-033 (lần chạy 39916343646);
- PB-04 từ đầu ra ghi trong L-054;
- PB-05 từ `start-check.log` ghi trong L-049;
- PB-06 từ L-005;
- PB-07 từ L-002;
- PB-08 từ lần chạy 40031369521 (đoạn trong báo cáo RUN-20261023-04);
- PB-09 viết theo thông báo chuẩn của Gradle (mục 8, câu 3);
- PB-03 từ thông báo của `wait-run.sh` mã 5 và L-047.

**Ngoại lệ viết test trước:** `docs/auto-run.md`, `docs/lessons.md` là tài liệu, không có test. Bảng `docs/ops/failure-playbook.md` là dữ liệu test đọc, vào commit test.

### 4.1. Chạy thử

Không cần (E dưới 2): thêm một chế độ cho script có sẵn, `grep -E` và python3 đều đã dùng trong `scripts/tests/run.sh`. Ghi thêm, chỉ đọc, ngày 2026-10-24, để mẫu PB-01 và cách lấy log có căn cứ:

- `gh api "repos/VGL-Studio/camera-pose/actions/runs/39997301164/attempts/1/jobs"`: job đỏ `120564114832 ui-test`. Lần thử mới nhất (`.../jobs`) cho `120569140220 ui-test failure 2`.
- `gh api "repos/.../actions/jobs/<id>/logs"` không có `--allow-escape-sequences` thì in `the response contains terminal escape sequences; pass --allow-escape-sequences to output it anyway` và không in log. Có cờ đó thì tải được: 1 326 và 1 432 dòng.
- `grep -cE 'VGL\.Assets: Could not download .*reason=NETWORK'`: lần 1 ra 0, lần 2 ra 3 (dòng thật: `W VGL.Assets: Could not download borrowed/poses/img/v1/m02/preview_540.webp (reason=NETWORK, error=SocketTimeoutException <- SocketException)`).
- Lần 1 chỉ có `W VGL.Loading: Task STARTER_SET failed`, cùng `ComposeTimeoutException: Condition still not satisfied after 60000 ms`. Vì vậy PB-01 có thêm vế `VGL\.Loading: Task [A-Z_]+ failed`, và đứng trước PB-10.
- Log lần chạy 39589924059 (L-002) không còn tải được (`--log-failed` rỗng, log đã hết hạn lưu); mẫu PB-07 lấy từ dòng ghi trong L-002.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7:
- `scripts/failure-kind.sh` do `main-guard.yml` gọi (cổng chất lượng);
- `docs/auto-run.md` đổi cách phiên xử lý CI đỏ (quy trình phiên tự chạy).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script (thêm chế độ), một tài liệu mới, test và dữ liệu test, ba đoạn tài liệu |
| B. Độ rõ của yêu cầu | 1 | L-057 rõ; còn câu hỏi số lần chạy lại của PB-01 (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Mỗi dòng có test với log thật; phần phiên làm theo bảng chỉ kiểm được khi lỗi xảy ra lại |
| D. Ảnh hưởng chéo | 2 | Mọi lần CI đỏ của mọi phiên đi qua bảng; `auto-run.md` cùng sửa với T-036, T-037, T-038, T-040 |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của `failure-kind.sh` và các script đọc Markdown đã có (`plan-check.py`, `status.py`) |
| **Tổng** | 7 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/failure-kind.sh` (cổng chất lượng qua `main-guard.yml`), `docs/auto-run.md`, `scripts/tests/run.sh`.
- Task khác có thể xung đột:
  - T-038 (làm trong phiên có người theo dõi trước RUN-20261024-01) thêm gạch đầu dòng riêng về R8 ở bước 8. T-039 chỉ viết lại gạch đầu dòng "CI đỏ thì AI đọc log..." và giữ nguyên gạch đầu dòng R8 của T-038. Dòng PB-08 của bảng trỏ về đó và do T-039 giữ.
  - T-040 (sau T-039 trong danh sách) sửa bước 9 và `merge-if-green.sh` (L-054). Khi T-040 merge, nó sửa dòng PB-04 thành "làm theo đầu ra của `merge-if-green.sh`" (mục 3.1 của plan T-040).
  - T-036 sửa mục 2, T-037 sửa bảng các bước, bước 3, 4, 7, mục Giao bước 3 đến 5, mục 4 và mục 5. Không chạm đoạn của plan này (bước 5 gạch đầu dòng "Lỗi tải thư viện", bước 8 gạch đầu dòng "CI đỏ", mục 6).
  - `scripts/tests/run.sh`: khối mới nằm ngay sau khối `failure-kind`, giữa dòng 196 và khối `is-exact-revert`. Các plan khác thêm khối ở chỗ khác.
- Đề xuất thứ tự chạy: trước T-040, vì T-040 sửa dòng PB-04 của bảng này.

## 7. Điều kiện dừng riêng

1. Một test `failure-kind:` hiện có phải sửa mới qua được: dừng (chế độ cũ phải giữ nguyên).
2. Cần sửa `main-guard.yml` để dùng bảng: dừng, viết plan riêng (mục 8, câu 2).

## 8. Câu hỏi cho người duyệt

1. **PB-01 chạy lại tối đa mấy lần?** Hiện "lỗi chập chờn chạy lại một lần". L-050: lần chạy lại thứ nhất cũng đỏ vì R2. Đề xuất của AI: 2 lần cho PB-01. Lần thứ 3 vẫn đỏ cùng mẫu thì ghi báo cáo và dừng task, không sửa code (trừ khi PR đổi code tải file). Mỗi lần `ui-test` khoảng 10 phút, nên tốn thêm tối đa khoảng 20 phút mỗi task. Đổi `AppLaunchTest` để bấm "Thử lại" là cách sửa gốc, đề xuất ticket riêng.
2. **`main-guard` có dùng bảng không?** Đề xuất của AI: chưa. `main-guard` chỉ chạy `check.sh`, không có emulator, nên lỗi của bảng ít gặp ở đó, trừ PB-09 (tải thư viện). Đổi `main-guard.yml` là plan rủi ro cao riêng, chỉ làm khi có lần `main-broken` vì lỗi tải thư viện.
3. **PB-09 chưa có log thật của dự án.** Mẫu viết theo thông báo chuẩn của Gradle (`Could not GET '...'. Received status code 429 from server`, `Read timed out`). Đề xuất của AI: giữ dòng này, vì bước 5 đã có quy tắc cho lỗi này. Lần đầu gặp thật thì thay đoạn log của test bằng log thật.
4. **Có thêm dòng cho lần lệnh đẩy bị bộ phân loại từ chối (L-051) không?** Đề xuất của AI: không. Lần từ chối là kết quả của lời gọi lệnh trong phiên, không nằm trong log nào để dò. T-037 đã có đoạn "Khi không đẩy được" trong `auto-run.md`.
5. **Commit rỗng khi lệnh chạy lại bị 403 (PB-01) có tính vào 2 lần chạy lại không.** Đề xuất của AI: có, để một lỗi R2 kéo dài vẫn dừng sau tối đa 2 lần thử thêm.
6. **Không tải được log job.** Đề xuất của AI: xử lý như `khong-co` (lỗi thật, tối đa 3 lần sửa), không coi là lỗi hạ tầng, để phiên không chạy lại mãi một lỗi thật chỉ vì không đọc được log.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1: Duyệt (Nhật, duyệt chéo Tuấn, 2026-10-24); ghi chú: Tuấn chấp nhận mọi đề xuất của AI ở mục 8. Thay bằng phiên bản 2 vì rà soát độc lập trước RUN-20261024-01 tìm hai điểm nên sửa (PB-01 và cách lấy log chưa có đường lui khi proxy cloud chặn) và ba ghi chú (PB-05, chữ ở bảng cột, PB-08).

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-24, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Phiên bản 2.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261024-01, phiên bản plan 2

- **Kết quả:** đã merge
- **Nhánh, PR:** `claude/T-039-bang-tra-loi`, PR #104 (commit merge 74462ee)
- **Đã làm:** `docs/ops/failure-playbook.md` (10 dòng PB-01 đến PB-10); `failure-kind.sh --playbook <log> [<bảng>]` đọc bảng bằng `python3 -I`; `auto-run.md` bước 5, bước 8 (lệnh lấy log, đường lui khi không tải được log, 4 điểm), mục 6; 13 tệp log mẫu trong `scripts/tests/playbook/`.
- **Test viết trước:** khoảng 30 khẳng định; ở khung cũ 18 thất bại ở phần khẳng định (script chưa có `--playbook`). Test "chế độ cũ không đổi" và 8 test `failure-kind:` cũ qua không sửa.
- **Rà soát độc lập:** không chặn. Một điểm nên sửa đã xử lý: bỏ test khóa cứng đúng 10 dòng (đổi thành ít nhất 10, để bảng lớn dần). Ghi chú: test "log chỉ có trong bảng vẫn ra other" yếu, vì `PB-01.log` vốn không có dòng lỗi Gradle; giữ nguyên.
- **Kết quả CI:** `check.sh` cục bộ qua; `pr-check` xanh; `main-guard` xanh (lần chạy 40075166615).
- **Lệch so với plan:** `docs/lessons.md` không sửa (dòng Áp dụng của L-057 và L-050 đã ghi T-039). Mẫu PB-09 viết theo thông báo chuẩn của Gradle, chưa có log thật (mục 8, câu 3 của plan).
- **Việc còn lại:** `main-guard.yml` chưa dùng bảng (mục 8, câu 2).
- **Cần người xem:** các dòng việc phải làm của bảng, nhất là PB-01 (tối đa 2 lần chạy lại) và PB-08.
