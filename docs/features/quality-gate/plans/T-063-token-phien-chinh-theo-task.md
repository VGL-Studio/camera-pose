---
ticket: T-063
title: Token của phiên chính khi làm task trên HEAD tách rời tính vào đúng task (lệnh `token-usage.py mark`)
status: draft                # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.5M         # cỡ S, số gợi ý của scripts/token-usage.py estimate (2026-10-27), không nâng tay (L-081)
blocking_points: [process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-063: TOKEN CỦA PHIÊN CHÍNH KHI LÀM TASK TÍNH VÀO ĐÚNG TASK

## 1. Tóm tắt

Thêm lệnh `scripts/token-usage.py mark T-NNN` và `mark chung`. Phiên chính chạy `mark T-NNN` khi bắt đầu một task (hay khi chuyển sang làm việc của một task khác) và `mark chung` khi không còn task nào đang làm. Lệnh chỉ in một dòng xác nhận; `report` đọc lại chính dòng lệnh đó trong transcript của phiên chính (khối `tool_use` của công cụ Bash) và gán mọi request của phiên chính không ở nhánh task nào, từ dấu mốc `mark T-NNN` gần nhất tới dấu mốc kế tiếp, cho T-NNN. Dấu mốc trước `--since` không tính: `--since` (giờ bắt đầu phiên) có tác dụng như `mark chung`, nên dấu mốc mà một phiên trước bỏ dở không lan sang phiên sau. Dấu mốc nằm trong transcript nên sống đúng bằng transcript; phiên tiếp tục trong môi trường mới chạy lại `mark` cho task đang làm dở. Code và 11 test đã chạy thật trên code hiện tại, trên bản cài thử và với 7 đột biến (mục 4.1).

**Phiên bản 2** viết lại theo nhánh chính sau PR #179 (đồng bộ dự án mẫu, đợt 1, `1fc049e`), cùng thiết kế, cùng test và câu trả lời của Tuấn ở mục 8 của phiên bản 1:

- PR #179 đổi docstring đầu tệp, chú thích của `BRIEF_TASK` và hàm `message_text` của `scripts/token-usage.py`, nên `diff` của phiên bản 1 không áp được (hunk docstring hỏng). `diff` ở mục 3.1 viết lại trên `1fc049e`, `git apply --check` sạch; phần code không đổi so với phiên bản 1.
- `scripts/tests/run.sh` của mẫu có 18 test `token-usage` có sẵn (phiên bản 1: 20), và một khối test `plan-check` mới nằm giữa test `a worktree of the repository is counted` và `# status.py builds the progress page`; khối test mới đặt ngay sau test `a worktree ...`.
- `docs/auto-run.md` và `docs/ops/token-usage.md` là file lấy từ mẫu, dẫn sổ bài học của mẫu (`docs/CLAUDE.md`, đầu tệp); câu mới ghi `camera-pose L-083`, như các phần app áp lại (`camera-pose L-084`). Mục 4 bước 0 giữ L-103 (của mẫu, cùng nội dung với camera-pose L-080). Mục Giao bước 3 đến 5 có thêm gạch "Phiên chính đẩy nhánh task" trước đoạn L-084; câu mới vào gạch đầu dòng đầu, trước cả hai.
- Chạy lại L-075 trên `1fc049e` (mục 4.1): kết quả và 7 đột biến giống phiên bản 1.

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là bài học L-083 (`docs/lessons.md`; Tuấn nhận ngày 2026-10-26, chọn làm ticket riêng): request của phiên chính cho task, khi thư mục chính đứng ở HEAD tách rời vì phiên con giữ nhánh task, tính vào task thay vì dòng Chung.

| Yêu cầu | Tiêu chí | Cách hiểu của AI |
| --- | --- | --- |
| Phiên chính sau dấu mốc tính vào task | Transcript của phiên chính (ngoài `subagents/`) có lệnh Bash `cd /repo && scripts/token-usage.py mark T-031` lúc 03:00; các request lúc 03:01, 03:07, 03:09:30 trên nhánh `HEAD` vào T-031 (3 request) | Gán khi nhánh của request không khớp `TASK_BRANCH`; nhánh `HEAD`, nhánh báo cáo hay `main` đều được gán |
| Trước dấu mốc đầu, sau `mark chung` vẫn là Chung | Request lúc 02:59 (trước mọi dấu mốc) và lúc 03:11 (sau `mark chung` lúc 03:10) vào Chung, cùng request của phiên con không có dòng `Task:` | `mark chung` kết thúc task; trước dấu mốc đầu không có task |
| Nhánh task thắng dấu mốc | Request trên `claude/T-032-other` lúc 03:02, trong lúc dấu mốc T-031 có hiệu lực, vẫn vào T-032 | Gán theo nhánh như cũ trước, dấu mốc chỉ lấp chỗ trống |
| Chỉ lệnh thật của phiên chính là dấu mốc | Lệnh `git commit -m 'chạy scripts/token-usage.py mark T-035'` lúc 03:08 (tên lệnh ngay trước dấu nháy đóng) và `grep -En 'scripts/token-usage.py mark T-033\|mark chung' ...` lúc 03:09 (tên lệnh ngay sau dấu nháy mở) không phải dấu mốc: request lúc 03:09:30 vẫn vào T-031. Lệnh `mark T-036` chạy ở một thư mục ngoài repo (`/elsewhere`) không phải dấu mốc. Lệnh `mark T-034` mà phiên con chạy lúc 03:05 không phải dấu mốc: request của phiên chính lúc 03:07 vẫn vào T-031 | Mẫu khớp chỉ nhận tên script ở đầu lệnh hay sau khoảng trắng, `;`, `&`, `\|`, `(`; chỉ đọc dấu mốc trong transcript ngoài `subagents/`; phiên con vẫn gán theo dòng `Task:` (L-073) |
| Dấu mốc xếp theo giờ | Transcript `a.jsonl` có `mark T-031` lúc 03:00; `b.jsonl` có `mark chung` lúc 02:00 và request lúc 04:00: request vào T-031 | Script đọc transcript theo tên tệp (tên phiên là UUID), không theo giờ; dấu mốc của mọi transcript xếp theo giờ trước khi gán |
| Gọi bằng đường dẫn tuyệt đối | `python3 /home/user/camera-pose/scripts/token-usage.py mark T-031` là dấu mốc | Trước `scripts/` được có một đường dẫn không chứa khoảng trắng hay dấu nháy |
| Phiên tiếp tục trong cùng môi trường | `--since` 00:30; dấu mốc ở transcript `s.jsonl` lúc 01:00; request lúc 01:01 (`s.jsonl`) và lúc 05:00 ở transcript mới `s2.jsonl` (không có dấu mốc riêng) đều vào T-031 | Dấu mốc đọc ở mọi transcript của repo, xếp theo giờ. Phiên tiếp tục dùng lại `started_at` của phiên (`--since`), nên dấu mốc của nó vẫn tính |
| Dấu mốc trước `--since` không tính | Cùng transcript, `--since` 04:00: request lúc 05:00 vào Chung | Dấu mốc mà phiên trước mở mà không đóng (phiên chết sau `mark T-NNN`) không lan sang phiên sau |
| Lệnh `mark` | `mark T-031` in một dòng có `T-031`, mã 0; `mark T031` in dòng bắt đầu `LỖI: `, mã 2 | Lệnh không ghi tệp nào, không đọc transcript |
| Không đổi phần khác | Mọi test `token-usage` có sẵn qua mà không sửa | Request không có dấu mốc nào đi qua đúng đường cũ |
| Phiên chính chạy lệnh | `docs/auto-run.md`: bước 1 của task chạy `mark T-NNN` (kể cả khi tiếp tục trên nhánh có sẵn); chuyển sang làm việc của task khác thì `mark` task đó; bước 10 chạy `mark chung` khi không còn task nào đang làm, còn thì `mark` task đó; mục 4 bước 0 chạy `mark chung`; phiên tiếp tục chạy `mark` cho task đang làm dở. `docs/ops/token-usage.md` ghi cách gán và giới hạn | Chạy thừa không hại: dấu mốc sau thay dấu mốc trước |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0010 (cổng chất lượng, test của script).

## 3. Giải pháp

### 3.1. Cách làm

**Chọn cách gán (L-083 nêu hai cách):**

- **Dấu mốc do phiên chính ghi (chọn):** phiên chính chạy một lệnh ở đầu và cuối task; script đọc lại lệnh đó từ transcript. Dấu mốc nằm cùng chỗ với số token mà nó gán, nên không thể còn transcript mà mất dấu mốc. Không cần tệp trạng thái trong `.git` hay `$HOME`, không cần commit gì. Phiên quên chạy lệnh thì số giống hôm nay (vào Chung), không hỏng gì. Phiên tiếp tục trong môi trường mới thì transcript trước cũng mất (cả token lẫn dấu mốc); phiên đó chạy lại `mark` cho task đang làm dở.
- **Suy ra từ giờ tạo nhánh và giờ merge (không chọn, mục 3.4):** đọc bảng Dòng thời gian của báo cáo phiên. Không cần thêm lệnh, nhưng bảng nằm trên nhánh báo cáo (worktree `../report`), không phải ở thư mục chạy script; các phiên trước ghi không đều (RUN-20261023-04, RUN-20261024-01 không có dòng "tạo nhánh"; RUN-20261023-02 ghi "tạo nhánh; phiên con bắt đầu bước 2"); giờ chỉ tới phút và không có ngày, phải đoán khi phiên qua nửa đêm.

**`scripts/token-usage.py`:** áp đúng `diff` dưới đây (đã chạy thử, mục 4.1). Tóm tắt:

- hằng `MARK_COMMAND` (tên script ở đầu lệnh hay sau khoảng trắng, `;`, `&`, `|`, `(`, có thể có đường dẫn đứng trước không chứa khoảng trắng hay dấu nháy; sau tên nhiệm vụ là hết dòng, khoảng trắng hay `;&|)`) và `MARK_NAME`;
- `marks_of(entry)`: các dấu mốc trong khối `tool_use` tên `Bash` của một dòng `assistant`;
- trong `requests`, với tệp ngoài `subagents/`, dòng có chuỗi `token-usage.py mark` được đọc JSON và thêm `(giờ, tên)` vào danh sách dấu mốc (có lọc theo thư mục của repo); mỗi request ghi thêm `main` và `time`;
- `apply_marks(found, marks, since)`: bỏ dấu mốc trước `since`, xếp theo giờ; request của phiên chính mà nhánh không khớp `TASK_BRANCH` lấy dấu mốc gần nhất có giờ không sau giờ request; dấu mốc là T-NNN thì nhánh thành `claude/T-NNN`;
- lệnh con `mark` (in dòng xác nhận, mã 0; tên sai thì `LỖI: `, mã 2); docstring đầu tệp thêm `mark` và cách gán.

```diff
diff --git a/scripts/token-usage.py b/scripts/token-usage.py
index b4e9da3..849933c 100755
--- a/scripts/token-usage.py
+++ b/scripts/token-usage.py
@@ -5,15 +5,22 @@ Usage:
   scripts/token-usage.py report [--since ISO] [--until ISO] [--run RUN-ID] [--csv FILE] [--transcripts FILE ...]
   scripts/token-usage.py estimate [--csv FILE]
   scripts/token-usage.py credit --run RUN-ID --usd AMOUNT [--csv FILE]
+  scripts/token-usage.py mark T-NNN|chung
 
 report: reads every transcript (subagents included) of this repository, keeps one usage per API request,
   attributes it to a task by the git branch the request was made on (claude/T-NNN-..., claude/fix-T-NNN-...),
-  and prints a Markdown table with the plan's estimate next to the actual count. With --run and --csv it also
-  writes the rows of that run into the CSV, replacing rows of the same run so it can run again.
+  and prints a Markdown table with the plan's estimate next to the actual count. A request of a main session
+  transcript on no task branch counts for the task of the last `mark` a main session ran before it, from --since
+  on (any transcript): --since acts as `mark chung`, so a mark left open by an earlier session never reaches this
+  one. With --run and --csv it also writes the rows of that run into the CSV, replacing rows of the same run so it
+  can run again.
 estimate: median total per ticket size from the CSV, for writing token_estimate in a new plan, and the cloud
   session credit spent per million tokens over the runs that recorded it.
 credit: records the cloud session credit (US dollars) a run opened with `claude --cloud` spent, read by a person
   from the balance before and after the run. It goes on the shared row of the run, once per run.
+mark: the main session runs `mark T-NNN` when it starts a task and `mark chung` when the task ends
+  (L-083 of camera-pose). It only prints what it marks: report reads the command back from the transcript,
+  so the mark lasts as long as it.
 
 A transcript belongs to this repository when its working directory is the repository, any of its git worktrees
 (the report branch is often checked out in one, L-008 of camera-pose), or a folder named like the repository
@@ -52,6 +59,14 @@ TASK_BRANCH = re.compile(r"^claude/(?:fix-)?(T-\d+)(?:-|$)")
 # A subagent brief names its task on a line of its own (auto-run.md, L-071): the subagent runs in the task worktree by
 # path, so its transcript still carries the report branch of the main session.
 BRIEF_TASK = re.compile(r"^Task: (T-\d+)\s*$", re.M)
+# While a subagent worktree holds the task branch, the main checkout is on a detached HEAD, so the main session's
+# own requests for the task carry no task branch. It runs `scripts/token-usage.py mark T-NNN` at the start of the
+# task and `mark chung` at its end; the command, read back from its transcript, is the mark (L-083 of camera-pose).
+# Only a command that runs the script counts, by any path (./scripts/..., /abs/repo/scripts/...): the name inside
+# quotes (a grep, a commit message) does not.
+MARK_COMMAND = re.compile(
+    r"(?:^|[\s;&|(])(?:[^\s'\"]*/)?scripts/token-usage\.py mark (T-\d+|chung)(?=$|[\s;&|)])", re.M)
+MARK_NAME = re.compile(r"^(T-\d+|chung)$")
 
 
 def parse_amount(text):
@@ -161,9 +176,32 @@ def message_text(message):
     return ""
 
 
+def marks_of(entry):
+    """The `mark` commands of one assistant line of a main session transcript."""
+    found = []
+    content = (entry.get("message") or {}).get("content")
+    for part in content if isinstance(content, list) else []:
+        if isinstance(part, dict) and part.get("type") == "tool_use" and part.get("name") == "Bash":
+            found += MARK_COMMAND.findall(str((part.get("input") or {}).get("command") or ""))
+    return found
+
+
+def apply_marks(found, marks, since):
+    """A main session request on no task branch counts for the task of the last mark before it, since `since`."""
+    marks = sorted(mark for mark in marks if not since or mark[0] >= since)
+    for request in found:
+        main, moment = request.pop("main"), request.pop("time")
+        if not main or TASK_BRANCH.match(request["branch"]):
+            continue
+        current = [name for at, name in marks if at <= moment]
+        if current and current[-1] != SHARED:
+            request["branch"] = f"claude/{current[-1]}"
+    return found
+
+
 def requests(files, since, until):
     """One usage per API request: the last line of a request carries its final counts."""
-    found = {}
+    found, marks = {}, []
     in_repo = repo_matcher()
     for path in files:
         try:
@@ -182,6 +220,14 @@ def requests(files, since, until):
                         first_user = False
                         match = BRIEF_TASK.search(message_text(entry.get("message")))
                         brief_task = f"claude/{match.group(1)}" if match else None
+                if "subagents" not in path.parts and "token-usage.py mark" in line:
+                    try:
+                        entry = json.loads(line)
+                    except json.JSONDecodeError:
+                        entry = {}
+                    moment = timestamp(entry.get("timestamp"))
+                    if entry.get("type") == "assistant" and moment and in_repo(entry.get("cwd")):
+                        marks += [(moment, name) for name in marks_of(entry)]
                 if '"usage"' not in line:
                     continue
                 try:
@@ -202,8 +248,10 @@ def requests(files, since, until):
                     "cache_write": usage.get("cache_creation_input_tokens") or 0,
                     "cache_read": usage.get("cache_read_input_tokens") or 0,
                     "output": usage.get("output_tokens") or 0,
+                    "main": "subagents" not in path.parts,
+                    "time": moment,
                 }
-    return found.values()
+    return apply_marks(list(found.values()), marks, since)
 
 
 def group(found):
@@ -344,6 +392,17 @@ def estimate(args):
     return 0
 
 
+def mark(args):
+    if not MARK_NAME.match(args.name):
+        print(f"LỖI: mark nhận T-NNN (task bắt đầu) hoặc chung (task xong); nhận được: {args.name}")
+        return 2
+    if args.name == SHARED:
+        print("Đã đánh dấu: từ giờ request của phiên chính vào dòng Chung.")
+    else:
+        print(f"Đã đánh dấu: từ giờ request của phiên chính không ở nhánh task nào tính vào {args.name}.")
+    return 0
+
+
 def main(argv):
     parser = argparse.ArgumentParser(description="Đếm token AI theo task của phiên tự chạy.")
     commands = parser.add_subparsers(dest="command", required=True)
@@ -359,8 +418,10 @@ def main(argv):
     credit_parser.add_argument("--run", required=True)
     credit_parser.add_argument("--usd", required=True)
     credit_parser.add_argument("--csv", default=DEFAULT_CSV)
+    mark_parser = commands.add_parser("mark")
+    mark_parser.add_argument("name")
     args = parser.parse_args(argv[1:])
-    return {"report": report, "estimate": estimate, "credit": credit}[args.command](args)
+    return {"report": report, "estimate": estimate, "credit": credit, "mark": mark}[args.command](args)
 
 
 if __name__ == "__main__":
```

**`scripts/tests/run.sh`:** thêm khối test của mục 4 ngay sau test `token-usage: a worktree of the repository is counted` (cuối khối `token-usage`, trước khối `# In a git checkout, plan-check.py only searches tracked files ...`). Khối dùng lại `usage_line`, `expect`, `token_usage`, `$tokens` và ticket, plan T-031 có sẵn ở đầu khối; thêm hàm `mark_line`. Mỗi nhóm có thư mục transcript riêng (`$work/projects-mark`, `$work/projects-mark2`, `$work/projects-mark4`, `$work/projects-mark5`) nên không đổi số của test nào khác.

**`docs/auto-run.md`** (lệnh `mark` viết thành một lệnh riêng, hoặc nối sau lệnh khác bằng `;`, không đứng đầu chuỗi `&&`; trong câu văn, tên lệnh đặt trong dấu nháy ngược):

- Mục 3, bảng các bước, dòng `1. Tạo nhánh`: thêm vào đầu cột Việc làm "Phiên chính chạy `scripts/token-usage.py mark T-NNN` khi bắt đầu task, kể cả khi tiếp tục trên nhánh có sẵn (mục Tiếp tục task đã dừng), trước khi tạo hay checkout nhánh, để token của phiên chính cho task tính vào task cả khi thư mục chính ở HEAD tách rời (camera-pose L-083)."
- Mục Giao bước 3 đến 5 cho phiên con, gạch đầu dòng đầu (sau câu "Thư mục chính giữ sạch, ..."): thêm "Thư mục chính ở HEAD tách rời trong lúc này; dấu mốc `mark T-NNN` của bước 1 gán phần phiên chính làm cho task vào task. Phiên chính chuyển sang làm việc của task khác (chuẩn bị bước 1, 2 của task sau trong lúc phiên con làm) thì chạy `mark` cho task đó, rồi `mark` lại task cũ khi quay về; phiên chính khởi động lại thì chạy lại `mark` cho task đang làm. Không giao lệnh `mark` cho phiên con: chỉ dấu mốc trong transcript của phiên chính được tính (camera-pose L-083)." Câu này vào cuối gạch đầu dòng đầu, trước gạch "Phiên chính đẩy nhánh task ..." và đoạn "Phiên con kết thúc mà không có báo cáo ..." (camera-pose L-084); không đổi chữ của hai gạch đó.
- Bước 10, gạch đầu dòng đầu: thêm "Khi task có kết quả cuối (merge, dừng, hay bỏ qua sau khi đã tạo nhánh), chạy `scripts/token-usage.py mark chung` nếu không còn task nào đang làm; còn task đang làm (đã tạo nhánh) thì chạy `mark` cho task đó (camera-pose L-083)."
- Mục 4, bước 0: thêm "và `scripts/token-usage.py mark chung` (camera-pose L-083)" sau `git checkout --detach origin/main`; giữ dẫn L-103 có sẵn (của mẫu).
- Mục 4, Tiếp tục phiên sau khi hết hạn mức, bước 5 ("Task đang làm dở: ..."): thêm câu "Chạy `scripts/token-usage.py mark T-NNN` cho task đó trước khi làm tiếp: môi trường mới không có transcript của phiên trước nên không có dấu mốc của nó (camera-pose L-083)."

**`docs/ops/token-usage.md`:**

- Mục 1, đoạn "Gán cho task theo nhánh", sau câu "Việc cuối phiên chạy khi thư mục chính đã rời nhánh task ... (L-103).", trước câu "Mọi request khác ...": thêm "Phiên chính làm task khi thư mục chính ở HEAD tách rời (phiên con giữ nhánh task trong worktree) thì chạy `scripts/token-usage.py mark T-NNN` khi bắt đầu hay chuyển sang task đó và `mark chung` khi không còn task nào đang làm (`docs/auto-run.md`, bước 1 và 10). Lệnh chỉ in một dòng; `report` đọc lại dòng lệnh trong transcript của phiên chính và gán mọi request của phiên chính không ở nhánh task nào, từ dấu mốc đó tới dấu mốc kế tiếp, cho task. Dấu mốc ở transcript trước vẫn tính cho phiên tiếp tục trong cùng môi trường; dấu mốc trước `--since` không tính (`--since` như `mark chung`); dấu mốc trong transcript của phiên con không được tính (camera-pose L-083)."
- Mục 5, Giới hạn, thêm bốn gạch đầu dòng:
  - "Việc chung mà phiên chính làm trong lúc một task có dấu mốc (chờ `main-guard` của task trước, `check.sh` đầu phiên chạy song song với bước 1 đến 3 của task đầu) tính vào task đó. Phiên chính quên `mark` khi chuyển giữa hai task thì phần đó tính vào task của dấu mốc trước."
  - "Phiên `claude --cloud` mở lại trong môi trường mới không còn transcript của phần trước: phần token đó mất, cùng với dấu mốc của nó; phiên tiếp tục chạy lại `mark` cho task đang làm dở."
  - "Dòng lệnh có tên `scripts/token-usage.py mark T-NNN` đứng sau khoảng trắng mà không nằm ngay sau dấu nháy (ví dụ trong heredoc của commit hay mô tả PR) cũng được đọc là dấu mốc. Trong chữ viết, đặt tên lệnh trong dấu nháy ngược hay dấu nháy."
  - "Các dòng của `docs/metrics/token-usage.csv` ghi trước T-063 thiếu phần phiên chính của task có phiên con (camera-pose L-083), nên trung vị của `estimate` thấp hơn thật cho tới khi đủ task đo sau T-063."

**`docs/lessons.md`, L-083:** chuyển `applied`; thay dòng Áp dụng hiện có ("làm ở ticket T-063 ...") bằng các file trên và PR của task, giữ câu "Tuấn nhận ngày 2026-10-26" và "Dự án mẫu: VGL-Studio/app-project-template#206"; không thêm dòng Áp dụng thứ hai. Mục Kiểm chứng để trống cho báo cáo phiên sau.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/token-usage.py` | Lệnh con `mark`; `report` gán request của phiên chính theo dấu mốc, bỏ dấu mốc trước `--since` (`diff` ở mục 3.1) |
| Sửa | `scripts/tests/run.sh` | 11 test mới sau test `a worktree of the repository is counted` |
| Sửa | `docs/auto-run.md` | `mark T-NNN` ở bước 1; câu ở mục Giao bước 3 đến 5; `mark chung` hay `mark` task còn mở ở bước 10; `mark chung` ở mục 4 bước 0; `mark` task dở ở mục Tiếp tục phiên, bước 5 |
| Sửa | `docs/ops/token-usage.md` | Cách gán theo dấu mốc (mục 1), bốn giới hạn (mục 5) |
| Sửa | `docs/lessons.md` | L-083 sang `applied`, thay dòng Áp dụng |

L-020, các file nhắc tới file mà plan sửa:

- `scripts/token-usage.py` được nhắc trong `scripts/plan-check.py` (chỉ trong thông báo về `token_estimate`) và `scripts/tests/run.sh`. Plan không đổi lệnh `report`, `estimate`, `credit` hay định dạng CSV, nên `plan-check.py` không phải đổi.
- `docs/auto-run.md` được nhắc trong chú thích hay thông báo của `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/plan-check.py`, `scripts/resume-check.py`, `scripts/save-branch.sh`, `scripts/status.py`, `scripts/wait-run.sh`; không file nào đọc nội dung của nó. Plan chỉ thêm câu, không đổi tên hay số mục, nên không file nào phải đổi.
- `scripts/tests/run.sh` được `scripts/check.sh` chạy; plan chỉ thêm test.
- `docs/lessons.md` được `scripts/status.py` đọc (dòng Trạng thái); `applied` là trạng thái có sẵn.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** transcript của phiên `claude --cloud` có thể ghi khối `tool_use` của Bash khác dạng đã đo trên máy (`{"type":"assistant","message":{"content":[{"type":"tool_use","name":"Bash","input":{"command":...}}]}}`, transcript trên máy dev ngày 2026-10-26). Code có sẵn đã dựa vào cùng các trường `type`, `timestamp`, `cwd`, `message` của transcript cloud.
- **Điều kiện chuyển:** không áp dụng trong phiên: test dùng transcript giả nên không lộ điều này. Nếu báo cáo của phiên sau vẫn có dòng task thiếu phần phiên chính dù phiên đã chạy `mark`, người mở ticket mới.
- **Cách làm thay:** gán theo bảng Dòng thời gian của báo cáo phiên (giờ "T-NNN: tạo nhánh" tới giờ "T-NNN: merge" hay "dừng"), đọc từ tệp do tham số `--timeline` chỉ ra. Không làm trong task này.

## 4. Kiểm thử

Test của script trong `scripts/tests/run.sh`, khối `token-usage`, với transcript giả. Khối nguyên văn (đã chạy thử, mục 4.1):

```bash
# The main session marks the task it works on with `scripts/token-usage.py mark T-NNN` and ends it with
# `mark chung`: its own requests between the two count for the task, even on a detached HEAD (L-083 of camera-pose).
mark_line() { # <time> <command> [cwd]
  printf '{"type":"assistant","timestamp":"%s","cwd":"%s","message":{"content":[{"type":"tool_use","name":"Bash","input":{"command":"%s"}}]}}\n' \
    "$1" "${3:-$tokens}" "$2"
}
mkdir -p "$work/projects-mark/p/s/subagents"
{
  usage_line m0 HEAD 2026-10-18T02:59:00Z 90
  mark_line 2026-10-18T03:00:00Z 'cd /repo && scripts/token-usage.py mark T-031'
  usage_line m1 HEAD 2026-10-18T03:01:00Z 90
  usage_line m2 claude/T-032-other 2026-10-18T03:02:00Z 90
  usage_line m5 HEAD 2026-10-18T03:07:00Z 90
  mark_line 2026-10-18T03:08:00Z "git commit -m 'chạy scripts/token-usage.py mark T-035'"
  mark_line 2026-10-18T03:09:00Z "grep -En 'scripts/token-usage.py mark T-033|mark chung' docs/auto-run.md"
  usage_line m6 HEAD 2026-10-18T03:09:30Z 90
  mark_line 2026-10-18T03:10:00Z 'git checkout --detach origin/main; scripts/token-usage.py mark chung'
  usage_line m3 HEAD 2026-10-18T03:11:00Z 90
} > "$work/projects-mark/p/s.jsonl"
{
  mark_line 2026-10-18T03:05:00Z 'scripts/token-usage.py mark T-034'
  usage_line m4 HEAD 2026-10-18T03:06:00Z 90
} > "$work/projects-mark/p/s/subagents/agent-1.jsonl"
TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/projects-mark" python3 -I scripts/token-usage.py report \
  --since 2026-10-18T00:00:00Z > "$work/tokens-mark.out"
expect "token-usage: main-session requests after a mark count for the marked task" 1 \
  "$(grep -c '^| T-031 | M | 2k | 4k | 1.8x | 3 |' "$work/tokens-mark.out")"
expect "token-usage: before the first mark and after mark chung stay shared" 1 \
  "$(grep -c '^| Chung (ngoài task) | - | - | 4k | - | 3 |' "$work/tokens-mark.out")"
expect "token-usage: a request on a task branch keeps its branch's task during a mark" 1 \
  "$(grep -c '^| T-032 | - | - | 1k | - | 1 |' "$work/tokens-mark.out")"
expect "token-usage: a mark named in quotes or run by a subagent is not a mark" "0 0 0" \
  "$(grep -c '^| T-033 |' "$work/tokens-mark.out") $(grep -c '^| T-034 |' "$work/tokens-mark.out") $(grep -c '^| T-035 |' "$work/tokens-mark.out")"
mkdir -p "$work/projects-mark2/p"
{
  mark_line 2026-10-18T01:00:00Z 'scripts/token-usage.py mark T-031'
  usage_line n1 HEAD 2026-10-18T01:01:00Z 90
} > "$work/projects-mark2/p/s.jsonl"
usage_line n2 HEAD 2026-10-18T05:00:00Z 90 > "$work/projects-mark2/p/s2.jsonl"
TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/projects-mark2" python3 -I scripts/token-usage.py report \
  --since 2026-10-18T00:30:00Z > "$work/tokens-mark2.out"
expect "token-usage: a mark in an earlier transcript of the run still counts (resumed session)" "1 0" \
  "$(grep -c '^| T-031 | M | 2k | 2k | 1.2x | 2 |' "$work/tokens-mark2.out") $(grep -c '^| Chung' "$work/tokens-mark2.out")"
TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/projects-mark2" python3 -I scripts/token-usage.py report \
  --since 2026-10-18T04:00:00Z > "$work/tokens-mark3.out"
expect "token-usage: a mark before --since does not count (left open by an earlier session)" "0 1" \
  "$(grep -c '^| T-031 |' "$work/tokens-mark3.out") $(grep -c '^| Chung (ngoài task) | - | - | 1k | - | 1 |' "$work/tokens-mark3.out")"
mkdir -p "$work/projects-mark4/p"
{
  mark_line 2026-10-18T00:50:00Z 'scripts/token-usage.py mark T-036' /elsewhere
  usage_line q0 HEAD 2026-10-18T00:55:00Z 90
  mark_line 2026-10-18T01:00:00Z 'python3 /home/user/camera-pose/scripts/token-usage.py mark T-031'
  usage_line q1 HEAD 2026-10-18T01:01:00Z 90
} > "$work/projects-mark4/p/s.jsonl"
TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/projects-mark4" python3 -I scripts/token-usage.py report \
  --since 2026-10-18T00:00:00Z > "$work/tokens-mark4.out"
expect "token-usage: a mark run by an absolute path counts" 1 \
  "$(grep -c '^| T-031 | M | 2k | 1k | 0.6x | 1 |' "$work/tokens-mark4.out")"
expect "token-usage: a mark run in another folder is not a mark" 0 "$(grep -c '^| T-036 |' "$work/tokens-mark4.out")"
# Transcripts are read in file name order, not in time order: the marks of all of them are sorted by time.
mkdir -p "$work/projects-mark5/p"
mark_line 2026-10-18T03:00:00Z 'scripts/token-usage.py mark T-031' > "$work/projects-mark5/p/a.jsonl"
{
  mark_line 2026-10-18T02:00:00Z 'scripts/token-usage.py mark chung'
  usage_line r1 HEAD 2026-10-18T04:00:00Z 90
} > "$work/projects-mark5/p/b.jsonl"
TOKEN_USAGE_ROOT="$tokens" TOKEN_USAGE_PROJECTS_DIR="$work/projects-mark5" python3 -I scripts/token-usage.py report \
  --since 2026-10-18T00:00:00Z > "$work/tokens-mark5.out"
expect "token-usage: the latest mark wins whatever the order of the transcript files" "1 0" \
  "$(grep -c '^| T-031 | M | 2k | 1k | 0.6x | 1 |' "$work/tokens-mark5.out") $(grep -c '^| Chung' "$work/tokens-mark5.out")"
expect "token-usage: mark prints the task and exits 0" "0 1" \
  "$(token_usage mark T-031 > "$work/mark.out"; echo $?) $(grep -c 'T-031' "$work/mark.out")"
expect "token-usage: mark with a name that is not T-NNN or chung exits 2 and says why" "2 1" \
  "$(token_usage mark T031 > "$work/mark-bad.out" 2>&1; echo $?) $(grep -c '^LỖI: ' "$work/mark-bad.out")"
```

| Tiêu chí | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Phiên chính sau dấu mốc tính vào task | Test của script | `token-usage: main-session requests after a mark count for the marked task`: `m1` (03:01), `m5` (03:07, sau dấu mốc của phiên con), `m6` (03:09:30, sau tên lệnh trong `git commit -m` và `grep`) trên `HEAD`; bảng có `\| T-031 \| M \| 2k \| 4k \| 1.8x \| 3 \|` |
| Trước dấu mốc đầu, sau `mark chung` | Test của script | `token-usage: before the first mark and after mark chung stay shared`: `m0` (02:59), `m3` (03:11, sau `mark chung` 03:10), `m4` (phiên con không có dòng `Task:`); dòng Chung `\| Chung (ngoài task) \| - \| - \| 4k \| - \| 3 \|` |
| Nhánh task thắng dấu mốc | Test của script (đối chứng) | `token-usage: a request on a task branch keeps its branch's task during a mark`: `m2` trên `claude/T-032-other` lúc 03:02; bảng có `\| T-032 \| - \| - \| 1k \| - \| 1 \|` |
| Chỉ lệnh thật của phiên chính là dấu mốc | Test của script (đối chứng, bắt được đột biến 1, 2 và 6 của mục 4.1) | `token-usage: a mark named in quotes or run by a subagent is not a mark`: không có dòng T-033, T-034, T-035; kỳ vọng `"0 0 0"` |
| Dấu mốc ở thư mục khác | Test của script (đối chứng, bắt được đột biến 5) | `token-usage: a mark run in another folder is not a mark`: `mark T-036` có `cwd` `/elsewhere` lúc 00:50, request `q0` lúc 00:55; không có dòng T-036 |
| Dấu mốc xếp theo giờ | Test của script (bắt được đột biến 7) | `token-usage: the latest mark wins whatever the order of the transcript files`: kỳ vọng `"1 0"` (dòng T-031 1 request, không có dòng Chung) |
| Phiên tiếp tục | Test của script | `token-usage: a mark in an earlier transcript of the run still counts (resumed session)`: `--since` 00:30; bảng có `\| T-031 \| M \| 2k \| 2k \| 1.2x \| 2 \|`, không có dòng Chung; kỳ vọng `"1 0"` |
| Dấu mốc trước `--since` | Test của script (đối chứng, bắt được đột biến 3) | `token-usage: a mark before --since does not count (left open by an earlier session)`: cùng transcript, `--since` 04:00; không có dòng T-031, dòng Chung 1 request; kỳ vọng `"0 1"` |
| Đường dẫn tuyệt đối | Test của script | `token-usage: a mark run by an absolute path counts`: bảng có `\| T-031 \| M \| 2k \| 1k \| 0.6x \| 1 \|` |
| Lệnh `mark` | Test của script | `token-usage: mark prints the task and exits 0` (`"0 1"`); `token-usage: mark with a name that is not T-NNN or chung exits 2 and says why` (`"2 1"`, dòng bắt đầu `LỖI: `) |
| Không đổi phần khác | Test của script có sẵn | 18 test `token-usage` có sẵn qua mà không sửa |
| Phiên chính chạy lệnh | Kiểm tra thủ công (người: đọc báo cáo phiên) | Phiên chạy T-063 (hay phiên kế tiếp nếu T-063 là task cuối), khi viết mục Token của báo cáo: với mỗi task giao bước 3 đến 5 cho phiên con, chạy `scripts/token-usage.py report --since <started_at> --transcripts <các tệp trong subagents/ có dòng Task: T-NNN của task đó>` và dán số request của dòng task vào mục Token. Đạt: số request của dòng task trong bảng của cả phiên lớn hơn số đó (có phần phiên chính). Dòng Chung dưới khoảng 15M (RUN-20261026-03: 33.2M) là số tham khảo, không phải tiêu chí đạt |

**Ngoại lệ viết test trước:** phần sửa `docs/auto-run.md`, `docs/ops/token-usage.md`, `docs/lessons.md` là tài liệu, không có test. Dòng kiểm tra thủ công ở trên kiểm phần `auto-run.md`.

### 4.1. Chạy thử

- **Đã chạy thử (L-075), phiên bản 2:** 2026-10-27, worktree của nhánh `plan/T-063-v2` trên nhánh chính `1fc049e` (sau PR #179). Áp `diff` của mục 3.1 (`git apply --check` sạch), chèn tạm khối test trên sau test `a worktree of the repository is counted`, không commit, rồi trả lại cả hai tệp.
- **Lệnh:** phần đầu của `scripts/tests/run.sh` (dòng 1 tới 26: `set -euo pipefail`, `$work`, `expect`) cùng khối `token-usage` (dòng 1582 tới 1667) và khối test mới, chạy bằng `bash` của macOS. Không chạy cả `run.sh` ở bước này; phiên tự chạy chạy cả `run.sh` qua `scripts/check.sh`.
- **Kết quả trên code hiện tại (`1fc049e`):** 7 test thất bại đúng lý do, 4 đối chứng qua, 18 test có sẵn qua (`failures=7`, 22 dòng `ok`):
  - `main-session requests after a mark count for the marked task`: `expected '1', got '0'`. Không có dòng T-031: `m1`, `m5`, `m6` vào Chung.
  - `before the first mark and after mark chung stay shared`: `expected '1', got '0'`. Dòng Chung có 6 request.
  - `a mark in an earlier transcript of the run still counts (resumed session)`: `expected '1 0', got '0 1'`.
  - `a mark run by an absolute path counts`: `expected '1', got '0'`.
  - `the latest mark wins whatever the order of the transcript files`: `expected '1 0', got '0 1'`.
  - `mark prints the task and exits 0`: `expected '0 1', got '2 0'` (`argparse`: `invalid choice: 'mark'`).
  - `mark with a name that is not T-NNN or chung exits 2 and says why`: `expected '2 1', got '2 0'`. Mã 2 có sẵn từ `argparse` nhưng không có dòng `LỖI: `; vì vậy test kiểm cả dòng thông báo.
  - Đối chứng, qua: `a request on a task branch keeps its branch's task during a mark`, `a mark named in quotes or run by a subagent is not a mark`, `a mark before --since does not count`, `a mark run in another folder is not a mark`. Code hiện tại không đọc dấu mốc nào; các test giữ để bản mới không gán nhầm. Mỗi đối chứng (trừ test nhánh task, giữ cho quy tắc "nhánh thắng dấu mốc") bắt được ít nhất một đột biến dưới đây.
- **Kết quả trên bản cài thử:** `failures=0`, 29 dòng `ok` (18 có sẵn, 11 mới). Test có sẵn của dòng `Task: T-NNN` ở phiên con (`a subagent brief ...`) vẫn qua: `message_text` mới của PR #179 vẫn đọc tin nhắn đầu dạng chuỗi và dạng danh sách phần, và `diff` không đụng tới cách gán đó.
- **Đột biến trên bản cài thử** (mỗi lần đổi một chỗ, chạy lại khối, rồi trả lại; kết quả giống phiên bản 1):
  1. Đọc dấu mốc cả trong `subagents/` (bỏ điều kiện `"subagents" not in path.parts`): `failures=2`: `main-session requests after a mark ...` và `a mark named in quotes or run by a subagent ...` (`expected '0 0 0', got '0 1 0'`).
  2. Cho mẫu khớp ngay sau dấu nháy (thêm `'` vào nhóm ký tự đứng trước): `failures=2`: `main-session requests after a mark ...` và `a mark named in quotes ...` (`expected '0 0 0', got '1 0 0'`).
  3. Không bỏ dấu mốc trước `--since` (`marks = sorted(marks)`): `failures=1`: `a mark before --since does not count ...` (`expected '0 1', got '1 0'`).
  4. Không cho đường dẫn đứng trước `scripts/` (chỉ `./`): `failures=1`: `a mark run by an absolute path counts`.
  5. Bỏ lọc cwd của dấu mốc (bỏ `in_repo(entry.get("cwd"))`): `failures=1`: `a mark run in another folder is not a mark` (`expected '0', got '1'`).
  6. Bỏ phần nhìn sau tên nhiệm vụ (`(?=$|[\s;&|)])`): `failures=2`: `main-session requests after a mark ...` và `a mark named in quotes ...` (`expected '0 0 0', got '0 0 1'`).
  7. Không xếp dấu mốc theo giờ (`list` thay cho `sorted` trong `apply_marks`): `failures=1`: `the latest mark wins whatever the order of the transcript files` (`expected '1 0', got '0 1'`).
  - Đột biến `!= SHARED` (bỏ so với `chung`) cho kết quả tương đương (nhánh `claude/chung` không khớp `TASK_BRANCH`, vẫn vào Chung), nên không có test riêng.
- **Phiên bản 1** (2026-10-26, trên `f747428`): cùng kết quả với 20 test có sẵn (`failures=7`, 24 dòng `ok` trên code cũ; 31 dòng `ok` trên bản cài thử).
- **Dạng transcript:** đo trên transcript thật của máy dev (`~/.claude/projects/-Users-vgldev-Projects-camera-pose/`): lệnh Bash nằm ở dòng `"type":"assistant"`, `message.content[]` có `{"type":"tool_use","name":"Bash","input":{"command":...}}`, cùng `timestamp`, `cwd`, `gitBranch`. Thư mục ở HEAD tách rời: `git rev-parse --abbrev-ref HEAD` in `HEAD`; giá trị `gitBranch` mà Claude Code ghi lúc đó chưa đo được, nên code không dựa vào nó (gán mọi nhánh không khớp `TASK_BRANCH`).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (quy trình phiên tự chạy): thêm lệnh phiên chính chạy ở mỗi task trong `docs/auto-run.md`, và đổi cách tính số token mà báo cáo phiên ghi. `risk_score` để trống, mức cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script (khoảng 70 dòng thêm, 5 dòng đổi), test của nó và ba tài liệu |
| B. Độ rõ của yêu cầu | 0 | L-083 nêu vấn đề và cách sửa; plan chọn một trong hai cách, ghi lý do |
| C. Khả năng kiểm chứng tự động | 0 | Test của script với transcript giả, đã chạy thử và thử đột biến |
| D. Ảnh hưởng chéo | 1 | Báo cáo token của mọi phiên sau; thêm một lệnh vào mỗi task của phiên |
| E. Độ mới của kỹ thuật | 1 | Lần đầu đọc khối `tool_use` của transcript; dạng đã đo trên transcript thật |
| **Tổng** | 3 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:** `scripts/token-usage.py` (mục Token của báo cáo mọi phiên), `docs/auto-run.md`, `docs/ops/token-usage.md`, `scripts/tests/run.sh`.
- **PR trước đó:** PR #176 (`f747428`) áp dụng L-081, L-082, L-084, L-085 và chuyển L-083 sang `accepted`. PR #179 (`1fc049e`, đồng bộ dự án mẫu, đợt 1) thay `docs/auto-run.md`, `docs/ops/token-usage.md`, `scripts/token-usage.py`, `scripts/tests/run.sh` bằng bản mẫu, áp lại đoạn camera-pose L-084 và ghi dòng Áp dụng của L-083 với "Dự án mẫu: VGL-Studio/app-project-template#206". Phiên bản 2 viết theo nhánh chính sau PR #179.
- **Task khác:** T-062 (camera giả của emulator CI) sửa `.github/workflows/device-session.yml`, có thể cả `scripts/tests/run.sh`; khác khối. Chạy lần lượt, mỗi task tạo nhánh từ nhánh chính mới nhất.
- **Đề xuất thứ tự chạy:** chạy đầu danh sách. Phiên chính đọc `docs/auto-run.md` lúc bắt đầu, nên các task sau trong cùng phiên chưa chắc có lệnh `mark`; nếu phiên vẫn chạy lệnh (đã đọc PR của T-063), báo cáo của chính phiên đó tính đúng, vì `report` chạy bản script trên nhánh chính khi viết báo cáo.
- **Đề xuất về dự án mẫu:** sau khi merge, đề xuất lệnh `mark` và test về VGL-Studio/app-project-template (`docs/ops/from-app.md`); dán nguyên `diff` vì phiên mẫu không đọc được repo app (L-078).

## 7. Điều kiện dừng riêng

1. Một test `token-usage` có sẵn phải sửa thì mới qua: dừng.
2. Áp `diff` của mục 3.1 không sạch (nhánh chính đã đổi `scripts/token-usage.py` sau `1fc049e`): viết lại theo phần tóm tắt của mục 3.1, giữ nguyên test của mục 4; ghi khác biệt vào nhật ký. Không dừng.

## 8. Câu hỏi cho người duyệt

1. **Cách gán: dấu mốc do phiên chính chạy lệnh, hay suy ra từ bảng Dòng thời gian.** **Đề xuất của AI:** dấu mốc (mục 3.1): sống cùng transcript; bảng Dòng thời gian ghi không đều và nằm trên nhánh khác.
2. **Việc chung trong lúc có dấu mốc, và hai task chồng nhau.** Chờ `main-guard` của task trước, `check.sh` đầu phiên chạy song song với bước 1 đến 3 của task đầu tính vào task đang có dấu mốc. Phiên chính chuẩn bị task sau trong lúc phiên con làm task trước thì `mark` task nó chuyển sang; bước 10 chỉ `mark chung` khi không còn task đang làm. **Đề xuất của AI:** chấp nhận, ghi ở mục Giới hạn của `token-usage.md`; phần lệch này nhỏ so với phần đang mất (T-060 0.5M thay vì khoảng 5M).
3. **Mức rủi ro cao vì điểm chặn 7.** **Đề xuất của AI:** giữ mức cao, duyệt chéo như T-053.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1: Duyệt (Nhật, duyệt chéo Tuấn, 2026-10-26); ghi chú: Tuấn trả lời câu hỏi cùng AI, chấp nhận mọi đề xuất của AI ở mục 8. Câu 1: dùng lệnh `mark`, không suy từ bảng Dòng thời gian. Câu 2: việc chung và task chồng nhau lúc đang đánh dấu một task tính vào task đó (mục Giới hạn). Câu 3: giữ rủi ro cao, duyệt chéo. Thay bằng phiên bản 2 vì PR #179 đổi `scripts/token-usage.py` và tài liệu, `diff` không còn áp được (mục 1).

Phiên bản 2:

- **Quyết định:**
- **Người duyệt:**
- **Ghi chú:**

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
