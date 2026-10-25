---
ticket: T-053
title: Token của phiên con tính vào đúng task (dòng `Task: T-NNN` trong lời giao việc)
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 4.8M         # cỡ S, số gợi ý của scripts/token-usage.py estimate
blocking_points: [process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-25
---

# PLAN T-053: TOKEN CỦA PHIÊN CON TÍNH VÀO ĐÚNG TASK

## 1. Tóm tắt

Chép cách sửa của dự án mẫu (bài học L-071 của mẫu) vào `scripts/token-usage.py`. Transcript trong `subagents/` có dòng riêng `Task: T-NNN` ở tin nhắn đầu thì mọi request của nó thuộc task T-NNN, thay vì theo `gitBranch` của từng dòng. `docs/auto-run.md` bảo phiên chính ghi dòng đó trong mọi lời giao việc cho phiên con. Phần sửa script chép nguyên bản của mẫu: 22 dòng thêm, 1 dòng đổi; 4 test mới đã chạy thật trên code hiện tại và trên bản cài thử (mục 4.1).

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là bài học L-073 (`docs/lessons.md`, `accepted`, Tuấn nhận ngày 2026-10-25): chép bản `scripts/token-usage.py` của dự án mẫu qua một task có test.

| Yêu cầu | Tiêu chí | Cách hiểu của AI |
| --- | --- | --- |
| Phiên con tính vào task | Transcript trong một thư mục `subagents/` mà tin nhắn `user` đầu tiên có một dòng đúng bằng `Task: T-NNN` thì mọi request của transcript đó vào task T-NNN, dù `gitBranch` là nhánh báo cáo | Như mẫu: `BRIEF_TASK = ^Task: (T-\d+)\s*$` (cờ `re.M`), đọc tin nhắn `user` đầu tiên, gán nhánh `claude/T-NNN` để `TASK_BRANCH` có sẵn nhận ra |
| Lời giao dạng danh sách | Tin nhắn đầu có `content` là danh sách đoạn chữ (`[{"type":"text","text":...}]`) cũng đọc được dòng `Task:` | `message_text` của mẫu nối các đoạn `text`. Mẫu chỉ test dạng danh sách cho trường hợp không khớp; plan thêm một test khớp |
| Nhắc qua trong câu không tính | Dòng có `Task: T-NNN` nằm giữa câu (không đứng riêng một dòng) thì không gán; transcript của phiên chính (ngoài `subagents/`) không bao giờ gán theo dòng này, kể cả khi tin nhắn đầu của nó là đúng `Task: T-NNN` | Như mẫu: chỉ khớp cả dòng, chỉ xét tệp có `subagents` trong đường dẫn |
| Không đổi phần khác | Mọi test `token-usage` hiện có qua mà không sửa: gán theo nhánh, `fix-`, credit, worktree, tên repo, CSV cũ | Bản của mẫu là bản của app cộng thêm phần này (`diff` có 22 dòng thêm và 1 dòng đổi là dòng `branch`, mục 3.1) |
| Phiên chính ghi dòng đó | `docs/auto-run.md`: lời giao bước 3 đến 5, lời giao rà soát độc lập (bước 6) và lời giao phiên con ở mục 4, bước 5 (Soạn plan cho hàng chờ) đều có một dòng riêng, đúng `Task: T-NNN`; một lời giao cho một task. `docs/ops/token-usage.md` ghi cách gán mới | Theo chữ của mẫu (`auto-run.md` của mẫu dòng 88 và 327, `ops/token-usage.md` dòng 20), đổi mã bài học sang L-073 |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0010 (cổng chất lượng, test của script).

## 3. Giải pháp

### 3.1. Cách làm

**`scripts/token-usage.py`:** chép nguyên phần thêm của bản mẫu (VGL-Studio/app-project-template, nhánh chính, commit `a76b3cd`). So hai bản bằng `diff` thì có 22 dòng thêm và đúng 1 dòng đổi (dòng `"branch"`, cuối danh sách); không dòng nào khác của app bị xóa hay đổi:

- hằng `BRIEF_TASK = re.compile(r"^Task: (T-\d+)\s*$", re.M)` cùng chú thích, đặt sau `TASK_BRANCH`;
- hàm `message_text(message)`: `content` là chuỗi thì trả nguyên; là danh sách thì nối `part.get("text", "")` của các phần là `dict` bằng `\n`; khác thì trả `""`;
- trong `requests(files, since, until)`, mỗi tệp đặt `brief_task, first_user = None, "subagents" in path.parts`. Khi `first_user` còn đúng và dòng có `"user"`, đọc JSON; dòng có `type` là `user` thì `first_user = False` và `brief_task = f"claude/{match.group(1)}"` nếu `BRIEF_TASK` khớp `message_text(entry.get("message"))`, không thì `None`. JSON hỏng thì bỏ qua dòng đó;
- `"branch": brief_task or entry.get("gitBranch") or ""`.

Nhánh `claude/T-NNN` không có hậu tố vẫn khớp `TASK_BRANCH` (`(?:-|$)`), nên phần tổng hợp theo task không đổi.

Thêm nửa câu vào docstring đầu tệp, sau câu "attributes it to a task by the git branch ...": "or, for a subagent transcript, by a `Task: T-NNN` line of its first user message". Đây là dòng duy nhất không có ở bản của mẫu.

Cách làm: chép đúng ba khối trên vào bản của app bằng tay, theo `diff`; không chép đè cả tệp. Phần riêng của app (`credit_usd`, nhận worktree và tên repo) đã có ở cả hai bản và giống nhau.

**`scripts/tests/run.sh`:** thêm khối test của mục 4 ngay sau test `token-usage: the csv keeps the estimate and the total`. Khối dùng lại `usage_line`, `$tokens` và các tệp ticket, plan T-031 có sẵn ở đầu khối `token-usage`. Mỗi nhóm test có thư mục transcript riêng (`$work/projects-brief`, `$work/projects-brief2`, `$work/projects-brief3`) nên không đổi số của các test đứng sau.

**`docs/auto-run.md`:**

- Mục Giao bước 3 đến 5 cho phiên con, dòng "Nội dung giao việc tối thiểu:": thêm ở đầu danh sách "một dòng riêng, đúng `Task: T-NNN` (đặt ở dòng đầu của lời giao; không gạch đầu dòng, không định dạng, không thêm chữ sau), để `scripts/token-usage.py` tính token của phiên con vào task (L-073). Một lời giao cho một task: phiên con được giao tiếp việc của task khác (gửi thêm tin nhắn cho phiên con cũ) vẫn tính vào task đầu, nên task khác thì giao một phiên con mới. Lời giao rà soát độc lập ở bước 6 cũng có dòng này;".
- Bước 6, đoạn đầu: thêm câu "Lời giao việc cho phiên rà soát có một dòng riêng, đúng `Task: T-NNN`, như mục Giao bước 3 đến 5 (L-073)."
- Mục 4, bước 5 (Soạn plan cho hàng chờ), sau bước 7 của danh sách con: thêm đoạn "Giao việc cho phiên con (chạy thử, rà soát độc lập): lời giao có một dòng riêng, đúng `Task: T-NNN` của ticket đang soạn plan, như mục Giao bước 3 đến 5 (L-073)."

**`docs/ops/token-usage.md`, đoạn "Gán cho task theo nhánh":** thêm sau câu đầu: "Phiên con làm trong worktree bằng đường dẫn vẫn ghi nhánh của phiên chính, nên lời giao việc có một dòng riêng, đúng `Task: T-NNN` (không gạch đầu dòng, không định dạng, không thêm chữ sau): mọi request của transcript trong `subagents/` có dòng đó ở tin nhắn đầu thuộc task T-NNN. Một lời giao cho một task: tin nhắn gửi tiếp cho phiên con cũ vẫn tính vào task đầu. Transcript của phiên chính không bao giờ gán theo dòng này (L-073)."

**`docs/lessons.md`, L-073:** chuyển trạng thái sang `applied`, mục Áp dụng ghi các file trên và PR của task, giữ câu "Tuấn nhận ngày 2026-10-25" và dòng Dự án mẫu. Mục Kiểm chứng để trống cho báo cáo phiên sau.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/token-usage.py` | `BRIEF_TASK`, `message_text`, đọc tin nhắn đầu của transcript phiên con (chép từ dự án mẫu); nửa câu trong docstring |
| Sửa | `scripts/tests/run.sh` | 4 test mới sau test `the csv keeps the estimate and the total` |
| Sửa | `docs/auto-run.md` | Dòng `Task: T-NNN` trong lời giao việc (Giao bước 3 đến 5, bước 6, mục 4 bước 5) |
| Sửa | `docs/ops/token-usage.md` | Cách gán theo dòng `Task:` |
| Sửa | `docs/lessons.md` | L-073 sang `applied`, mục Áp dụng |

L-020, các file nhắc tới file mà plan sửa:

- `docs/auto-run.md` được nhắc trong chú thích hay câu thông báo của `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/resume-check.py`, `scripts/save-branch.sh`, `scripts/status.py` và `scripts/wait-run.sh`. Các file đó chỉ dẫn tới mục 1 đến 4, bước 5 và 8, "Khi dừng", "Khi không đẩy được", "Tiếp tục phiên"; không file nào đọc nội dung `auto-run.md`. Plan chỉ thêm câu vào mục Giao bước 3 đến 5, bước 6 và bước soạn plan cuối phiên, không đổi tên hay số mục nào, nên không file nào phải đổi.
- `scripts/tests/run.sh` được chạy bởi `scripts/check.sh` (dòng 32). Plan chỉ thêm test, không đổi cách chạy. `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh` và `scripts/wait-run.sh` khớp tên `run.sh` chỉ vì nhắc `wait-run.sh`, không đọc `scripts/tests/run.sh`.
- `docs/lessons.md` được `scripts/status.py` đọc (dòng "Trạng thái" của mỗi bài học, để đếm). L-073 chuyển từ `accepted` sang `applied`, là một trạng thái có sẵn, nên `status.py` không phải đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có. Code chép nguyên từ dự án mẫu và đã chạy thử trên app (mục 4.1).

## 4. Kiểm thử

Test của script trong `scripts/tests/run.sh`, khối `token-usage`, với transcript giả:

| Tiêu chí | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Phiên con tính vào task | Test của script | `token-usage: a subagent whose brief has a Task line counts for that task`: `subagents/agent-1.jsonl` có tin nhắn đầu là chuỗi `"Plan: x\nTask: T-031\nWorktree: ../task-T-031"` và một request `b1` trên nhánh `claude/RUN-20261018-01-report`; bảng có `\| T-031 \| M \| 2k \| 1k \| 0.6x \| 1 \|` |
| Nhắc qua trong câu không tính | Test của script | `token-usage: a task named in passing and the main session stay shared`: `subagents/agent-2.jsonl` có tin nhắn đầu dạng danh sách `"Review the run, see Task: T-031 notes"` (request `b2`), cộng `s.jsonl` của phiên chính (request `b3`), cả hai trên nhánh báo cáo; dòng Chung là `\| Chung (ngoài task) \| - \| - \| 2k \| - \| 2 \|` |
| Lời giao dạng danh sách | Test của script | `token-usage: a Task line in a brief made of text parts counts for that task`: `$work/projects-brief2/p/s/subagents/agent-3.jsonl` có tin nhắn đầu `[{"type":"text","text":"Review only.\nTask: T-031"}]` và request `c1` trên nhánh báo cáo; kỳ vọng `"1 0"` là `$(grep -c '^\| T-031 \| M \| 2k \| 1k \| 0.6x \| 1 \|' ...) $(grep -c '^\| Chung' ...)` trên bảng ra |
| Phiên chính không gán theo `Task:` | Test của script (đối chứng) | `token-usage: a Task line in the main session's transcript does not count for the task`: `$work/projects-brief3/p/s.jsonl` (phiên chính, ngoài `subagents/`) có tin nhắn đầu là chuỗi `"Task: T-031"` và request `d1` trên nhánh báo cáo; kỳ vọng `"0 1"` là `$(grep -c '^\| T-031 \|' ...) $(grep -c '^\| Chung (ngoài task) \| - \| - \| 1k \| - \| 1 \|' ...)` |
| Không đổi phần khác | Test của script có sẵn | Mọi test `token-usage` khác qua mà không sửa |
| Phiên chính ghi dòng `Task:` | Kiểm tra thủ công (người: đọc báo cáo phiên) | Báo cáo của phiên tự chạy kế tiếp, sau phiên có T-053: task có phiên con có dòng riêng gần với số đếm tay, dòng Chung không còn chứa phần của phiên con |

Hai test đầu là của mẫu (`scripts/tests/run.sh` của mẫu, dòng 1472 đến 1490), đổi mã bài học trong chú thích sang L-073. Test thứ ba và thứ tư là của plan này.

**Ngoại lệ viết test trước:** phần sửa `docs/auto-run.md`, `docs/ops/token-usage.md`, `docs/lessons.md` là tài liệu, không có test. Dòng kiểm tra thủ công ở trên kiểm phần `auto-run.md`.

### 4.1. Chạy thử

- **Đã chạy thử (L-075):** 2026-10-25, worktree của nhánh plan trên `origin/main` `0dfd2a4`. Chèn tạm khối 4 test vào `scripts/tests/run.sh` sau test `the csv keeps the estimate and the total`, không commit, rồi trả lại tệp gốc.
- **Lệnh:** `bash scripts/tests/run.sh` (bash 3.2.57 của macOS).
- **Kết quả trên code hiện tại:** 3 test thất bại đúng lý do, 1 đối chứng qua, mọi test khác qua (`3 script test(s) failed`):
  - `a subagent whose brief has a Task line counts for that task`: `expected '1', got '0'`. Không có dòng T-031: request `b1` theo nhánh báo cáo vào Chung.
  - `a task named in passing and the main session stay shared`: `expected '1', got '0'`. Dòng Chung có cả `b1`, nên là 3k, 3 request, không phải 2k, 2. Test này không phải đối chứng: nó chỉ qua khi `b1` được gán cho T-031.
  - `a Task line in a brief made of text parts counts for that task`: `expected '1 0', got '0 1'`. Không có dòng T-031, có dòng Chung.
  - `a Task line in the main session's transcript does not count for the task`: **đối chứng, qua** (`ok`). Code hiện tại không đọc dòng `Task:` ở đâu cả, nên phiên chính vào Chung; test giữ để bản mới không gán nhầm phiên chính.
  - Mọi test `token-usage` có sẵn (gán theo nhánh, `fix-`, credit, CSV cũ, worktree, tên repo) qua.
- **Kết quả trên bản cài thử:** chép tạm `scripts/token-usage.py` của mẫu đè lên bản của app, cùng khối test: `All script tests passed`, 644 dòng `ok`, cả 4 test mới qua. Đã trả lại cả hai tệp; `git status` sạch.
- Đối chứng: chỉ test thứ tư. Ba test kia thất bại trên code hiện tại vì cùng một thiếu sót (không đọc lời giao việc).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (quy trình phiên tự chạy): đổi lời giao việc cho phiên con trong `docs/auto-run.md`, và đổi cách tính số token mà báo cáo phiên ghi. `risk_score` để trống, mức cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script (22 dòng thêm, 1 dòng đổi), test của nó và ba tài liệu |
| B. Độ rõ của yêu cầu | 0 | L-073 và bản của mẫu cho sẵn cách sửa |
| C. Khả năng kiểm chứng tự động | 0 | Test của script với transcript giả |
| D. Ảnh hưởng chéo | 1 | Báo cáo token của mọi phiên sau; `auto-run.md` mà T-052 cũng có thể đọc |
| E. Độ mới của kỹ thuật | 0 | Chép nguyên, đã chạy thử |
| **Tổng** | 2 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:** `scripts/token-usage.py` (bước Token của báo cáo mọi phiên), `docs/auto-run.md`, `scripts/tests/run.sh`.
- **Task khác trong cùng danh sách:**
  - T-052 (PR #140: quy tắc 11 của `plan-check.py`, sửa trình chạy kịch bản) sửa `scripts/tests/run.sh`, `docs/ops/test-sessions.md`, `docs/CLAUDE.md`, `docs/templates/plan.md`, `scripts/plan-check.py`, `scripts/device-scenario.py`, `scripts/tests/fake-adb`. File chung với plan này chỉ có `scripts/tests/run.sh`: T-053 thêm vào khối `token-usage`, T-052 vào các khối `plan-check` và `device-scenario`, khác đoạn. Task chạy lần lượt, mỗi task tạo nhánh từ nhánh chính mới nhất.
  - T-050, T-051 (dịch chữ) không chạm file nào của plan này.
- **Đề xuất thứ tự chạy:** chạy đầu tiên, dù không bắt buộc. Phiên chính đọc `docs/auto-run.md` một lần lúc bắt đầu, nên lời giao của các task sau trong cùng phiên chưa chắc có dòng `Task:`; lợi ích chắc chắn là từ phiên tự chạy kế tiếp. Nếu phiên vẫn ghi dòng đó (đã đọc PR của T-053), báo cáo của chính phiên này tính đúng phần đó, vì phiên chính chạy `scripts/token-usage.py` của nhánh chính khi viết báo cáo.
- **Việc sau task này, không làm ở đây:** sửa lại số của RUN-20261025-01 trong `docs/metrics/token-usage.csv` (không cần: transcript ở môi trường cloud không còn).

## 7. Điều kiện dừng riêng

1. Một test `token-usage` có sẵn phải sửa thì mới qua: dừng.
2. Bản `scripts/token-usage.py` của mẫu lúc chạy khác bản đã so ở mục 3.1 (commit `a76b3cd`) ở phần khác ngoài ba khối trên: chỉ chép ba khối của mục 3.1, không chép thêm; ghi khác biệt vào báo cáo.

## 8. Câu hỏi cho người duyệt

1. **Mức rủi ro cao vì điểm chặn 7.** Phần đổi nhỏ và chép nguyên từ mẫu, nhưng đổi lời giao việc trong `auto-run.md`. **Đề xuất của AI:** giữ mức cao, duyệt chéo như các plan quy trình khác (T-044, T-045).
2. **Thêm test thứ ba (lời giao dạng danh sách có dòng `Task:`)**, mẫu không có. **Đề xuất của AI:** thêm, vì lời giao rà soát của công cụ giao việc có thể là danh sách đoạn chữ. Đề xuất ngược về mẫu sau khi merge (`docs/ops/from-app.md`).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-25, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Câu 1: giữ mức cao, duyệt chéo. Câu 2: giữ test thứ ba và test đối chứng của transcript phiên chính, đề xuất ngược về dự án mẫu sau khi merge. Plan qua một rà soát độc lập (0 chặn, 3 nên sửa, đã sửa hết).

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
