---
ticket: T-066
title: Kiểm khả năng của cách mở phiên trước khi duyệt và ở đầu phiên (`open_with`, bảng khả năng, `push-probe.sh`)
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 7.6M         # cỡ M, số gợi ý của scripts/token-usage.py estimate (2026-10-27), không nâng tay (L-081)
blocking_points: [process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-27
---

# PLAN T-066: KIỂM KHẢ NĂNG CỦA CÁCH MỞ PHIÊN

## 1. Tóm tắt

Danh sách chạy ghi cách mở phiên (`open_with: cloud | routine | local`). Một bảng trong `docs/ops/session-capabilities.md` ghi mỗi cách mở làm được gì (đẩy file workflow, đọc repo khác, gọi R2, emulator, bí mật, tự tiếp tục), với một cột mẫu đường dẫn để `scripts/run-check.py` (quy tắc 8) đối chiếu với hàng của bảng file ở mục 3.2 của từng plan: cách mở ghi `không` là lỗi, `thử` là chú ý. Mẫu rà soát danh sách có câu hỏi bắt buộc về khả năng của cách mở phiên. Đầu phiên, khi danh sách có plan sửa `.github/workflows/`, phiên chạy `scripts/push-probe.sh <RUN>`: đẩy một nhánh tạm (tên mới mỗi lần) có một dòng chú thích trong một file workflow rồi xóa; lời từ chối được tra bằng bảng tra lỗi (PB-13 của T-067), nên bị từ chối vì thiếu quyền `workflow` thì phiên làm đúng thủ tục ADR-1005/L8 của T-067 (bỏ qua mọi task mà `scripts/plan-check.py --touches .github/workflows/` trả 0), chỉ là sớm hơn, trước khi tạo nhánh. Plan chạy sau T-067 trong cùng danh sách. Code và 46 test đã chạy thật trên code hiện tại, trên bản cài thử và với 20 đột biến (mục 4.1).

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là bài học L-086 (`docs/lessons.md`) và đề xuất 1a, 1b, 4c mà Tuấn chọn ngày 2026-10-27 sau RUN-20261027-01 (T-062 làm xong mà không đẩy được vì token của phiên `claude --cloud` không có quyền `workflow`; T-065 bỏ qua cùng lý do).

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Danh sách ghi cách mở | Khi lập danh sách chạy thì front matter có `open_with:` với một trong `cloud`, `routine`, `local` | Dòng mới trong `docs/templates/run.md`; `docs/CLAUDE.md` mục Khi lập danh sách chạy bảo ghi |
| Bảng khả năng | Khi người hay AI cần biết cách mở nào làm được gì thì `docs/ops/session-capabilities.md` có một bảng: đẩy `.github/workflows/`, đọc repo khác, R2 qua `curl`, emulator, bí mật, tự tiếp tục khi hết hạn mức; mỗi ô `có`, `không` hay `thử`, kèm bằng chứng | File của app (dự án mẫu không có file cùng tên, đồng bộ không đè). Cột đọc theo tên ở hàng tiêu đề; dòng có mẫu đường dẫn thì script kiểm, dòng `-` để người đọc |
| `run-check.py` báo lỗi | Khi một plan có hàng ở bảng file của mục 3.2 (không tính chữ) khớp mẫu của một khả năng mà cách mở ghi `không` thì `run-check.py` báo LỖI, nêu task, khả năng, đường dẫn, cách mở | Quy tắc 8, cùng nghĩa với `plan-check.py --touches` của T-067 (chỉ hàng của bảng); mọi plan cần khả năng đó đều được nêu |
| ... chú ý khi phải thử | Khi ô là `thử` thì CHÚ Ý: phiên thử ở đầu phiên, không được thì task bị bỏ qua | `workflow-push` là `thử` ở cả ba cách mở (câu hỏi 1, 5) |
| ... khi chưa ghi cách mở | Khi `open_with` trống hay sai mà có plan cần khả năng thì CHÚ Ý nêu giá trị nhận được và các task; không plan nào cần thì không báo gì | Danh sách cũ không có trường này vẫn qua, không nhiễu |
| ... cách mở thật khác danh sách | Khi phiên được mở bằng cách khác `open_with` thì `RUN_CHECK_OPEN_WITH=<cách mở thật> scripts/run-check.py ...` đối chiếu theo cách mở thật | Phiên biết cách mở từ lời giao mở phiên, ghi vào mục Kiểm tra đầu phiên |
| ... bảng viết sai | Khi một mẫu trong bảng không đọc được thì CHÚ Ý nêu dòng, dòng đó không được kiểm | Không làm hỏng cả quy tắc |
| Rà soát hỏi về cách mở | Khi giao rà soát danh sách thì `docs/ops/pre-run-review.md` mục 2.3 có câu hỏi bắt buộc: phiên làm được từng bước của mỗi plan trong cách mở này không (đẩy file, mạng, bí mật, công cụ, repo khác, tự tiếp tục); bước không làm được mà plan không có cách thay là mức chặn | Thêm một dòng vào bảng mục 1 cho quy tắc 8; mẫu danh sách có dòng Cách mở phiên ở mục Kiểm tra trước khi duyệt |
| Thử đẩy ở đầu phiên | Khi danh sách có plan với `.github/workflows/` ở bảng file của mục 3.2 và bảng khả năng ghi `thử` (hay `open_with` chưa rõ) thì phiên chạy `scripts/push-probe.sh <RUN>` ngay sau khi đẩy nhánh báo cáo, kể cả ở phiên tiếp tục | `docs/auto-run.md` mục 2, một dòng mới của bảng điều kiện và bước 1 của mục Kiểm tra build ở chế độ nền |
| ... thử thật thay đổi workflow | Khi thử thì commit đẩy lên chỉ đổi đúng một file trong `.github/workflows/` (một dòng chú thích ở cuối), ưu tiên workflow không có `on: push`, trên nền `origin/main`, ở nhánh `claude/probe-<RUN>-<giờ>-<pid>` | Test của repo kiểm mọi workflow có `on: push` chỉ chạy trên `main`, nên nhánh thử không chạy workflow nào |
| ... không làm phiền phiên | Khi thử xong (qua hay bị từ chối) thì thư mục làm việc giữ nguyên nhánh, file, danh sách worktree; nhánh thử đã xóa; `main` trên GitHub không đổi | Worktree tạm trong `mktemp -d`, xóa bằng `trap`; commit `--no-verify`, không ký |
| ... đọc đúng kết quả | Khi lời từ chối khớp một dòng `bo-qua-loai` của bảng tra lỗi (PB-13, cả cách viết của OAuth App và GitHub App) thì mã 3, `TỪ CHỐI` kèm mã dòng; lỗi mạng thì thử lại như `save-branch.sh`; vẫn không được hay lỗi khác thì mã 4, `CHƯA RÕ`, chỉ in tối đa 3 dòng lỗi của git; qua thì mã 0, `QUA`; tham số sai hay không có file workflow thì mã 2 và không đẩy gì | Một mẫu duy nhất cho lỗi thiếu quyền: PB-13 (`scripts/failure-kind.sh --playbook`). Mã 3: thủ tục L8 của T-067, sớm hơn. Mã 4: ghi vào báo cáo, làm các task như thường, không đẩy lại dưới dạng khác |
| ... xóa nhánh bị từ chối | Khi GitHub không cho xóa nhánh thử thì vẫn mã 0, kèm CHÚ Ý để người xóa; lần thử sau vẫn chạy được | Mỗi lần thử một nhánh mới, không cần xóa trước, không dùng `--force` |
| L-086 | Khi duyệt plan thì L-086 sang `accepted`, Áp dụng "làm ở ticket T-066"; khi task merge thì sang `applied` | T-067 thêm phần của nó vào cùng dòng Áp dụng (mục 6) |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-1005 của T-067 (L8: bỏ qua loại task mà môi trường không làm được; plan này dùng, không đổi), ADR-0029 (mở rộng phạm vi có kiểm soát; không đổi), ADR-0025 (nhánh chính tự hồi phục; không đổi), ADR-0009 (tài khoản của người phụ trách).

## 3. Giải pháp

### 3.1. Cách làm

**Plan chạy sau T-067.** Phần đầu phiên dựa vào ba thứ T-067 thêm: dòng PB-13 (loại `bo-qua-loai`, mẫu `refusing to allow an? [A-Za-z ]+ to create or update workflow`) của `docs/ops/failure-playbook.md`, lệnh `scripts/plan-check.py --touches <tiền tố> <plan>` (mã 0 khi một hàng của bảng file ở mục 3.2 có đường dẫn dưới tiền tố, 1 khi không, 2 khi sai tham số), và quy tắc ADR-1005/L8 với mục "Quyết định AI đã tự làm" của báo cáo. T-067 không merge thì T-066 bị bỏ qua (`depends_on` của ticket ghi T-067).

**Thứ tự commit (L-025).** Bước 3: hai khối test mới trong `scripts/tests/run.sh` (mục 4). Trên code hiện tại, 27 test thất bại đúng lý do (quy tắc 8 chưa có; `scripts/push-probe.sh` chưa có, mã 127), 19 đối chứng qua (mục 4.1). Bước 4: `scripts/run-check.py`, `scripts/push-probe.sh`, `docs/ops/session-capabilities.md` và các tài liệu; có test đọc bảng thật của repo nên bảng thuộc bước 4. Test `push-probe: the real playbook (PB-13 of T-067) classifies the refusal` chỉ qua khi PB-13 của T-067 đã có trên nhánh chính.

**`scripts/run-check.py`** (`diff` trên nhánh chính `084d366`, đã áp và chạy, mục 4.1):

```diff
diff --git a/scripts/run-check.py b/scripts/run-check.py
index 72e7164..337ce60 100755
--- a/scripts/run-check.py
+++ b/scripts/run-check.py
@@ -22,7 +22,13 @@ documents behind it, the mistakes that stopped or slowed earlier sessions:
      "Bỏ đề xuất thứ tự: A trước B" (then a warning). A plan that names a file another plan of the list creates,
      without having it in its own section 3.2 and without depending on that plan: an error when it comes first,
      a warning (what it misses if the other plan stops) when it comes after.
-RUN_CHECK_ROOT overrides the repository root (tests). Messages are in Vietnamese for the team.
+  8. What the way of opening the session can do (L-086 of camera-pose): the list's `open_with` (cloud, routine,
+     local) against the table of docs/ops/session-capabilities.md. A plan whose file table (section 3.2, rows only,
+     like plan-check.py --touches) has a path matching a capability the method lacks ("không") is an error; one the
+     session must probe at its start ("thử") is a warning. A missing or unknown `open_with` is a warning only when
+     some plan needs a capability. A row of the table with a broken pattern is a warning.
+RUN_CHECK_ROOT overrides the repository root (tests). RUN_CHECK_OPEN_WITH overrides `open_with` (a session opened
+another way than the list says). Messages are in Vietnamese for the team.
 """
 
 import importlib.util
@@ -194,6 +200,77 @@ def repo_basenames():
     return [Path(line).name for line in out.splitlines()]
 
 
+OPEN_METHODS = ("cloud", "routine", "local")
+CAPABILITIES = DOCS / "ops" / "session-capabilities.md"
+
+
+def capability_rows():
+    """(rows, warnings): rows are (name, path pattern, {method: value}) of the table in session-capabilities.md.
+
+    Columns are found by their header (Khả năng, Mẫu, cloud, routine, local). Rows without a path pattern ("-")
+    are for people only; a row with a broken pattern is skipped with a warning.
+    """
+    if not CAPABILITIES.is_file():
+        return [], []
+    match = re.search(r"<!-- capabilities:start -->(.*?)<!-- capabilities:end -->",
+                      CAPABILITIES.read_text(encoding="utf-8"), re.S)
+    rows, warnings, columns = [], [], None
+    for line in (match.group(1) if match else "").splitlines():
+        if not line.lstrip().startswith("|"):
+            continue
+        cells = [c.strip() for c in line.strip().strip("|").split("|")]
+        if columns is None:
+            lowered = [c.lower() for c in cells]
+            if not all(m in lowered for m in OPEN_METHODS):
+                continue
+            columns = {m: lowered.index(m) for m in OPEN_METHODS}
+            columns["pattern"] = next((i for i, c in enumerate(lowered) if c.startswith("mẫu")), 1)
+            continue
+        if len(cells) <= max(columns.values()) or not cells[0].startswith("`"):
+            continue
+        name = cells[0].strip("`")
+        pattern = re.search(r"`([^`]+)`", cells[columns["pattern"]])
+        if not pattern:
+            continue
+        try:
+            compiled = re.compile(pattern.group(1))
+        except re.error as error:
+            warnings.append(f"docs/ops/session-capabilities.md: mẫu của '{name}' không đọc được ({error}); "
+                            "dòng này không được kiểm.")
+            continue
+        rows.append((name, compiled, {m: cells[columns[m]].strip("` ").lower() for m in OPEN_METHODS}))
+    return rows, warnings
+
+
+def capability_problems(run_text, files_of, in_list):
+    """Errors and warnings of rule 8: what the plans need against what the way of opening the session can do."""
+    rows, warnings = capability_rows()
+    errors = []
+    needs = []  # (ticket, capability, first matching path, values)
+    for name, pattern, values in rows:
+        for ticket in in_list:
+            hit = sorted(path for path in files_of.get(ticket, ()) if pattern.search(path))
+            if hit:
+                needs.append((ticket, name, hit[0], values))
+    if not needs:
+        return errors, warnings
+    method = (os.environ.get("RUN_CHECK_OPEN_WITH") or front_matter(run_text).get("open_with", "")).lower()
+    if method not in OPEN_METHODS:
+        tickets = ", ".join(dict.fromkeys(ticket for ticket, *_ in needs))
+        warnings.append(f"danh sách chưa ghi open_with (cloud, routine hay local; nhận '{method}') mà {tickets} cần "
+                        "khả năng riêng của cách mở phiên (docs/ops/session-capabilities.md, L-086 của camera-pose).")
+        return errors, warnings
+    for ticket, name, path, values in needs:
+        value = values.get(method, "")
+        if value == "không":
+            errors.append(f"{ticket} cần '{name}' (`{path}` ở mục 3.2) mà phiên mở bằng {method} không có "
+                          "(docs/ops/session-capabilities.md). Đổi open_with, hoặc để task sang phiên khác.")
+        elif value != "có":
+            warnings.append(f"{ticket} cần '{name}' (`{path}` ở mục 3.2): phiên mở bằng {method} phải thử ở đầu phiên "
+                            "(docs/auto-run.md, mục 2); không được thì task bị bỏ qua.")
+    return errors, warnings
+
+
 def check(run_path):
     errors, warnings = [], []
     rows = run_rows(run_path.read_text())
@@ -318,6 +395,9 @@ def check(run_path):
                     warnings.append(f"{user} nhắc `{mention}` mà {creator} tạo; nếu {creator} dừng thì {user} thiếu "
                                     f"`{mention}`. Cân nhắc thêm {creator} vào depends_on của {user}, hoặc ghi phương án "
                                     "ở mục 3.4 của plan (L-078).")
+    capability_errors, capability_warnings = capability_problems(run_text, files_of, in_list)
+    errors += capability_errors
+    warnings += capability_warnings
     independent = [t for t in in_list if not set(depends.get(t, [])) & set(in_list)]
     if len(in_list) > 1 and len(independent) == 1:
         warnings.append("danh sách chỉ có một chuỗi phụ thuộc: một task dừng thì các task sau bị bỏ qua (L-045).")
```

**`scripts/push-probe.sh`** (tạo mới, nguyên văn bản đã chạy thử):

```bash
#!/usr/bin/env bash
# Checks at the start of a session that this environment may push a change to .github/workflows/ (L-086 of
# camera-pose). A token without the GitHub `workflow` scope pushes ordinary commits but refuses any commit that
# touches a workflow file, so a task that edits a workflow only learns it after all its work is done.
#
# Usage:   scripts/push-probe.sh <RUN-YYYYMMDD-NN>
# Exit:    0 the push went through (the probe branch is deleted again, or a warning says it is left).
#          3 the refusal is a `bo-qua-loai` row of the failure playbook (PB-13: no `workflow` scope): the session
#            skips the tasks of that kind (docs/auto-run.md, mục 2).
#          4 no clear answer (another refusal, or a network error that did not clear): the session records it and
#            runs its tasks as usual. 2 bad arguments, or nothing to probe.
# What it does: commits one comment line to a workflow of origin/main that runs on no branch push (dispatch or
#          schedule only, else the first one), in a temporary worktree, and pushes it to a new branch
#          claude/probe-<RUN>-<time>-<pid>. No workflow runs on a push to that branch. The current checkout, branch
#          and files never change. Each call uses a new branch, so a probe branch left by a refused delete never
#          blocks the next call.
# Env:     PUSH_PROBE_PLAYBOOK        failure playbook table (default docs/ops/failure-playbook.md; tests)
#          PUSH_PROBE_RETRY_SECONDS   waits between retries of a network error (default "60 120 240")
# Runs in the current repository, so it works from a worktree.
# Messages are in Vietnamese for the team; comments are in English.

set -uo pipefail

run="${1:-}"
if ! [[ "${run}" =~ ^RUN-[0-9]{8}-[0-9]{2}$ ]]; then
  echo "LỖI: cách dùng: scripts/push-probe.sh RUN-YYYYMMDD-NN (nhận '${run}')." >&2
  exit 2
fi
here="$(cd "$(dirname "$0")" && pwd)"
branch="claude/probe-${run}-$(date -u +%H%M%S)-$$"

if ! git rev-parse --verify -q origin/main > /dev/null; then
  echo "LỖI: không có origin/main; chạy git fetch origin main trước." >&2
  exit 2
fi
workflows="$(git ls-tree --name-only origin/main .github/workflows/ | grep -E '\.ya?ml$')"
if [ -z "${workflows}" ]; then
  echo "LỖI: origin/main không có file nào trong .github/workflows/, không có gì để thử." >&2
  exit 2
fi
# Prefer a workflow without a push trigger: even a stray trigger can then never run on the probe branch.
workflow=""
while read -r candidate; do
  if ! git show "origin/main:${candidate}" | grep -qE '^[[:space:]]+push:'; then
    workflow="${candidate}"
    break
  fi
done <<< "${workflows}"
[ -n "${workflow}" ] || workflow="$(head -n 1 <<< "${workflows}")"

tmp="$(mktemp -d)"
cleanup() {
  git worktree remove --force "${tmp}/probe" > /dev/null 2>&1 || true
  rm -rf "${tmp}"
}
trap cleanup EXIT

git worktree add -q --detach "${tmp}/probe" origin/main || { echo "LỖI: không tạo được worktree tạm." >&2; exit 4; }
printf '# push probe %s (scripts/push-probe.sh); this branch is deleted right after the push\n' "${run}" \
  >> "${tmp}/probe/${workflow}"
git -C "${tmp}/probe" -c user.name="push-probe" -c user.email="push-probe@invalid" -c commit.gpgsign=false \
  commit -q --no-verify -m "Thử đẩy thay đổi workflow (${run}), nhánh xóa ngay" -- "${workflow}" \
  || { echo "LỖI: không commit được trong worktree tạm." >&2; exit 4; }

# Messages git prints for a network problem, as in save-branch.sh; anything else is not retried.
network_pattern='Could not resolve host|Connection timed out|Connection reset|The requested URL returned error: 5|RPC failed|early EOF|hung up'
read -r -a waits <<< "${PUSH_PROBE_RETRY_SECONDS:-60 120 240}"
attempt=0
while :; do
  output="$(git -C "${tmp}/probe" push origin "HEAD:refs/heads/${branch}" 2>&1)"
  code=$?
  [ "${code}" -ne 0 ] || break
  # One pattern for every refusal the session knows: the failure playbook (PB-13 for the workflow scope).
  printf '%s\n' "${output}" > "${tmp}/push.log"
  kind="$("${here}/failure-kind.sh" --playbook "${tmp}/push.log" ${PUSH_PROBE_PLAYBOOK:+"${PUSH_PROBE_PLAYBOOK}"} 2> /dev/null)"
  if [[ "${kind}" =~ ^PB-[0-9]+\ bo-qua-loai ]]; then
    echo "TỪ CHỐI: ${kind%%:*}: GitHub không nhận thay đổi .github/workflows/ từ phiên này."
    echo "Bỏ qua mọi task mà scripts/plan-check.py --touches .github/workflows/ <plan> trả 0, trước khi tạo nhánh; làm các task khác (auto-run.md, mục 2)."
    grep -m1 -E 'refusing to allow|remote rejected' <<< "${output}"
    exit 3
  fi
  if grep -qE "${network_pattern}" <<< "${output}" && [ "${attempt}" -lt "${#waits[@]}" ]; then
    sleep "${waits[${attempt}]}"
    attempt=$((attempt + 1))
    continue
  fi
  echo "CHƯA RÕ: không đẩy được nhánh thử ${branch}, không phải lỗi đã biết trong bảng tra lỗi. Ghi vào mục Kiểm tra đầu phiên, làm các task như thường; không đẩy lại dưới dạng khác."
  grep -E '^ ?! |^(error|fatal|remote):' <<< "${output}" | tail -n 3
  exit 4
done

if git -C "${tmp}/probe" push -q origin --delete "${branch}" > /dev/null 2>&1; then
  echo "QUA: phiên đẩy được thay đổi .github/workflows/ (đã thử với ${workflow}, nhánh ${branch} đã xóa)."
else
  echo "QUA: phiên đẩy được thay đổi .github/workflows/ (đã thử với ${workflow})."
  echo "CHÚ Ý: không xóa được nhánh ${branch}; người xóa sau phiên (git push origin --delete ${branch})."
fi
exit 0
```

**`docs/ops/session-capabilities.md`** (tạo mới, nguyên văn bản đã chạy thử; test `the table of the repository asks a cloud session to probe workflow pushes` đọc chính file này):

```markdown
# CÁCH MỞ PHIÊN LÀM ĐƯỢC GÌ

Phiên tự chạy mở theo một trong ba cách. Mỗi cách chạy trong một môi trường khác, với token GitHub, mạng và công cụ khác. Một plan cần điều mà cách mở phiên không có thì task dừng giữa chừng, thường sau khi đã làm xong việc (camera-pose L-086: token của phiên `claude --cloud` không có quyền `workflow`, T-062 làm xong mà không đẩy được). Tài liệu này để người lập danh sách chạy chọn cách mở trước khi duyệt (`docs/CLAUDE.md`, mục Khi lập danh sách chạy).

| Cách mở (`open_with`) | Là gì |
| --- | --- |
| `cloud` | `claude --cloud` từ terminal của dev, dùng cloud session credits (camera-pose L-020). Không tự tiếp tục khi hết hạn mức (camera-pose L-008) |
| `routine` | `start-run.yml` hay Run now của routine (`docs/auto-run.md`, mục 1). `resume-run.yml` tự tiếp tục khi hết hạn mức |
| `local` | Claude Code chạy trên máy của dev, trong thư mục repo, dùng git và `gh` của máy đó |

Danh sách chạy ghi cách mở ở dòng `open_with:` (`docs/templates/run.md`). `scripts/run-check.py` (quy tắc 8) đọc bảng dưới: plan có đường dẫn ở mục 3.2 khớp cột Mẫu của một khả năng mà cách mở ghi `không` là lỗi; ghi `thử` là chú ý, và phiên thử khả năng đó ở đầu phiên (`docs/auto-run.md`, mục 2). Script chỉ tính các hàng của bảng file ở mục 3.2 (như `scripts/plan-check.py --touches`), không tính chữ. Dòng có Mẫu `-` là để người đọc, script không kiểm; mẫu viết sai thì script báo chú ý và bỏ qua dòng đó. Giá trị: `có`, `không`, `thử`.

Phiên biết cách mở của mình từ lời giao mở phiên (prompt của `claude --cloud` ghi "mở bằng claude --cloud"; routine và `start-run.yml` có lời giao riêng; local là phiên chạy trên máy của dev) và ghi vào mục Kiểm tra đầu phiên. Phiên tiếp tục (sau khi hết hạn mức, hay người mở lại) chạy lại `scripts/push-probe.sh` khi cần, vì môi trường mới có thể dùng token khác; mỗi lần thử dùng một nhánh mới nên không vướng nhánh của lần trước.

<!-- capabilities:start -->
| Khả năng | Mẫu đường dẫn ở mục 3.2 | cloud | routine | local | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `workflow-push` | `^\.github/workflows/` | thử | thử | thử | Đẩy commit có đổi file workflow cần token có quyền `workflow`. Phiên cloud và routine dùng kết nối GitHub của tài khoản Claude: tài khoản của Nhật đẩy được (PR #179), của Tuấn ở RUN-20261027-01 thì không (L-086), nên phải thử bằng `scripts/push-probe.sh`. Local đẩy được khi git của máy dùng SSH, hay `gh auth status` có quyền `workflow`; máy không chắc thì cũng thử |
| `cross-repo-read` | - | không | không | có | Đọc repo khác của tổ chức (dự án mẫu). Phiên cloud chỉ có quyền với repo của app (camera-pose L-078): plan dán nguyên đoạn code cần chép vào mục 3.1 |
| `r2-network` | - | không | không | có | Gọi R2 hay máy chủ ngoài bằng `curl`: proxy của cloud trả 403 (camera-pose L-005). Kiểm bằng workflow trên GitHub |
| `emulator` | - | không | không | thử | Cloud không có emulator; test trên emulator và `device-session` chạy trong CI. Local có khi máy có emulator hay máy thật nối `adb` |
| `repo-secrets` | - | không | không | không | Bí mật của repo chỉ có trong GitHub Actions. Việc cần bí mật làm bằng workflow; người tạo bí mật trước phiên |
| `auto-resume` | - | không | có | không | Tự tiếp tục khi hết hạn mức (`resume-run.yml`). Cách khác: người mở lại với dòng "phiên tiếp tục" |
<!-- capabilities:end -->

Khi một lần thử cho kết quả chắc chắn (ví dụ `scripts/push-probe.sh` qua ở hai phiên liên tiếp sau khi Tuấn cấp lại quyền), người đổi ô `thử` thành `có` hay `không`, ghi ngày và lần chạy ở cột Ghi chú. Thêm một khả năng mới: thêm một dòng, Mẫu là biểu thức chính quy kiểu Python trên đường dẫn của mục 3.2, và một test trong `scripts/tests/run.sh` (khối `run-check`).
```

**Tài liệu** (chữ thêm, đúng như dưới):

`docs/templates/run.md`, front matter, ngay sau dòng `plan_queue:`:

```text
open_with:                   # cloud | routine | local: cách mở phiên (docs/ops/session-capabilities.md); run-check.py quy tắc 8
```

`docs/templates/run.md`, mục Kiểm tra trước khi duyệt, ngay sau dòng `- **Rà soát độc lập:** ...`:

```text
- **Cách mở phiên:** <open_with và lý do>; khả năng mà plan cần (quy tắc 8 của `run-check.py`, `docs/ops/session-capabilities.md`): <từng dòng và cách xử lý, hoặc "Không có">
```

`docs/CLAUDE.md`, mục Khi lập danh sách chạy, gạch mới ngay sau gạch "Gom việc của cả nhóm vào một phiên ...":

```text
- Ghi cách mở phiên vào `open_with:` (`cloud`, `routine` hay `local`) và đối chiếu với [ops/session-capabilities.md](ops/session-capabilities.md): plan cần một khả năng mà cách mở ghi `không` (`run-check.py` báo lỗi, quy tắc 8) thì đổi cách mở, hoặc để plan sang phiên khác và ghi lý do; ghi `thử` thì phiên thử ở đầu phiên và bỏ qua task khi không được (L-086).
```

`docs/ops/pre-run-review.md`, bảng mục 1, dòng mới cuối bảng:

```text
| Plan có hàng ở bảng file của mục 3.2 cần một khả năng mà cách mở phiên (`open_with`) không có, theo `docs/ops/session-capabilities.md`: lỗi khi ô ghi `không`, chú ý khi ghi `thử` (phiên thử ở đầu phiên); `open_with` trống hay sai mà có plan cần khả năng: chú ý; mẫu của bảng viết sai: chú ý (camera-pose L-086) | Lỗi hoặc chú ý |
```

`docs/ops/pre-run-review.md`, mục 2, câu "Nội dung giao việc tối thiểu": thêm vào cuối phần trong ngoặc kép, trước dấu ngoặc kép đóng:

```text
 Trả lời câu hỏi về cách mở phiên ở mục 2.3 cho từng plan.
```

`docs/ops/pre-run-review.md`, mục 2.3, gạch mới đứng đầu:

```text
- **Cách mở phiên (bắt buộc, trả lời cho từng plan):** với `open_with` của danh sách, phiên làm được mọi bước của plan không: đẩy file (`.github/workflows/` cần quyền `workflow`), mạng (R2 hay máy chủ ngoài qua proxy bị 403), bí mật (chỉ có trong GitHub Actions), công cụ (emulator chỉ có trong CI), đọc repo khác, tự tiếp tục khi hết hạn mức. Đối chiếu `docs/ops/session-capabilities.md`, cả với bước không có đường dẫn ở mục 3.2 (lệnh, workflow phải chạy, file phải đọc). Bước không làm được mà plan không có cách thay ở mục 3.4 là mức chặn (camera-pose L-086).
```

`docs/auto-run.md`, mục 1, bước 3, thêm vào cuối đoạn:

```text
 Cách mở phải khớp `open_with` của danh sách. Phiên biết cách mở của mình từ lời giao mở phiên và ghi vào mục Kiểm tra đầu phiên; mở cách khác thì chạy `run-check.py` với `RUN_CHECK_OPEN_WITH=<cách mở thật>` (`docs/ops/session-capabilities.md`).
```

`docs/auto-run.md`, mục 2, bảng điều kiện, dòng mới ngay sau dòng "Danh sách qua `scripts/run-check.py` ...":

```text
| Cách mở phiên làm được việc mà plan cần (`docs/ops/session-capabilities.md`, quy tắc 8 của `run-check.py`). Có task mà `scripts/plan-check.py --touches .github/workflows/ <plan>` trả 0, và ô `workflow-push` của cách mở thật là `thử` (hay `open_with` chưa rõ): chạy `scripts/push-probe.sh <RUN-YYYYMMDD-NN>` ngay sau khi đẩy nhánh báo cáo, trước `check.sh`; phiên tiếp tục cũng chạy lại, vì môi trường mới có thể dùng token khác (camera-pose L-086) | Mã 0: ghi dòng `QUA` (và dòng CHÚ Ý nếu không xóa được nhánh thử) vào mục Kiểm tra đầu phiên. Mã 3 (dòng `bo-qua-loai` của bảng tra lỗi, PB-13): làm như ADR-1005/L8 và mục Khi không đẩy được bước 5, chỉ là sớm hơn: bỏ qua mọi task mà lệnh `--touches` trên trả 0 (cùng các task phụ thuộc chúng) trước khi tạo nhánh, plan và ticket giữ `approved`; ghi vào Task bị bỏ qua và mục Quyết định AI đã tự làm, kèm dòng của GitHub; soạn danh sách chạy nháp cho các task đó; làm các task khác. Mã 4 (chưa rõ): ghi vào mục Kiểm tra đầu phiên, không đẩy lại dưới dạng khác, làm các task như thường. Mã 2: ghi vào báo cáo, đi tiếp. Lỗi của quy tắc 8 (ô `không`): bỏ qua task đó như lỗi về ticket |
```

`docs/auto-run.md`, mục Kiểm tra build ở chế độ nền, bước 1: sau chữ "không có `env-broken` hay `setup-broken`" thêm ", `push-probe.sh` khi cần (dòng Cách mở phiên của bảng)".

`CLAUDE.md`, mục Lệnh, dòng mới ngay sau dòng `scripts/save-branch.sh <nhánh>`:

```text
- Thử ở đầu phiên có plan sửa `.github/workflows/` xem phiên đẩy được thay đổi workflow không: `scripts/push-probe.sh RUN-YYYYMMDD-NN` (`docs/ops/session-capabilities.md`, `docs/auto-run.md` mục 2).
```

`docs/lessons.md`, L-086: PR plan này chuyển `proposed` sang `accepted`, dòng Áp dụng "làm ở ticket T-066 (...); dòng bảng tra lỗi ở T-067" (mục 6). Bước 10 của task: `applied`, ghi số PR vào phần của T-066, giữ nguyên phần của T-067.

**`scripts/tests/run.sh`**: hai khối của mục 4, đặt như `diff` dưới. Khối `run-check` ngay trước dòng `# Files of other plans: a plan using a file that a later plan creates`; khối `push-probe` ngay sau dòng `export GIT_CONFIG_COUNT=...` đóng khối `save-branch`, với `unset` và khôi phục `GIT_CONFIG_*` riêng như khối đó (L-082 của mẫu). Hai khối dùng `$rr` và `$work` có sẵn, không trùng tên tệp với khối khác. Khối `push-probe` dùng một bảng tra lỗi riêng có đúng dòng PB-13 của T-067, trừ một test đọc bảng thật.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/run-check.py` | Quy tắc 8, `capability_rows` (cột theo tiêu đề, mẫu sai thì chú ý), `capability_problems`, `RUN_CHECK_OPEN_WITH`, docstring |
| Tạo | `scripts/push-probe.sh` | Thử đẩy thay đổi workflow trên nhánh tạm tên mới mỗi lần; tra lời từ chối bằng `scripts/failure-kind.sh --playbook`; thử lại lỗi mạng; mã 0/2/3/4 |
| Sửa | `scripts/tests/run.sh` | 20 test quy tắc 8, 26 test `push-probe` (gồm kiểm mọi workflow có `on: push` chỉ chạy trên `main`) |
| Tạo | `docs/ops/session-capabilities.md` | Ba cách mở, bảng khả năng có dấu `capabilities:start`/`end`, cách phiên biết cách mở của mình, phiên tiếp tục |
| Sửa | `docs/templates/run.md` | `open_with:`, dòng Cách mở phiên ở mục Kiểm tra trước khi duyệt |
| Sửa | `docs/CLAUDE.md` | Mục Khi lập danh sách chạy: ghi `open_with`, đối chiếu bảng |
| Sửa | `docs/ops/pre-run-review.md` | Bảng mục 1, câu giao việc mục 2, câu hỏi bắt buộc mục 2.3 |
| Sửa | `docs/auto-run.md` | Mục 1 bước 3, mục 2 (dòng điều kiện, bước 1 chế độ nền) |
| Sửa | `CLAUDE.md` | Mục Lệnh: `push-probe.sh` |
| Sửa | `docs/lessons.md` | L-086: `accepted` ở PR plan, `applied` ở bước 10 |
| Sửa | `docs/features/quality-gate/plans/T-066-kha-nang-cach-mo-phien.md` | Nhật ký thực hiện (bước 10) |
| Sửa | `docs/features/quality-gate/tickets/T-066-kha-nang-cach-mo-phien.md` | Trạng thái `on-staging` (bước 10) |

Nơi đọc các file bị đổi (L-020): `grep -rn 'run-check.py' scripts/ .github/ docs/ops docs/auto-run.md`: `scripts/tests/run.sh` (khối `run-check`, dòng "the latest run list has no new error from rule 11" chỉ đếm lỗi của quy tắc 11, quy tắc 8 không đổi kết quả), `docs/auto-run.md`, `docs/ops/pre-run-review.md`, `docs/CLAUDE.md`, `CLAUDE.md`; không workflow nào gọi `run-check.py`. `docs/templates/run.md`: `scripts/status.py` đọc `status`, `started_at`, `finished_at` của danh sách chạy, không đọc khóa lạ; `scripts/resume-check.py` đọc `status`. `scripts/push-probe.sh` mới, chỉ phiên gọi; nó đọc `docs/ops/failure-playbook.md` qua `scripts/failure-kind.sh` mà không sửa. Danh sách nháp RUN-20261027-02 (T-062, T-065, không có `open_with`) sau khi merge sẽ có một dòng CHÚ Ý "chưa ghi open_with", không lỗi.

- `docs/auto-run.md` được nhắc trong chú thích hay thông báo của `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/wait-run.sh`; `docs/CLAUDE.md` trong chú thích của `scripts/device-session.sh`, `scripts/merge-if-green.sh`; `CLAUDE.md` trong chú thích của `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt`, `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt`; `scripts/run-check.py` trong chú thích của `scripts/plan-queue.py`; `scripts/tests/run.sh` được `.github/workflows/pr-check.yml` chạy (qua `scripts/check.sh`). Không file nào đọc nội dung hay số mục của các tài liệu đó; plan chỉ thêm câu và dòng, không đổi tên mục, tên lệnh hay mã thoát của `run-check.py` (0 qua, 1 lỗi), nên không file nào phải đổi.

File của dự án mẫu (`docs/CLAUDE.md`, đầu file): `scripts/run-check.py`, `scripts/tests/run.sh`, `docs/auto-run.md`, `docs/ops/pre-run-review.md`, `docs/templates/run.md` là phần của mẫu. Thay đổi có ích chung, đề xuất về dự án mẫu (`docs/ops/from-app.md`) sau khi merge: quy tắc 8, `push-probe.sh`, câu hỏi rà soát, `open_with`; giá trị trong bảng khả năng là của app (tài khoản), mẫu dùng bảng của mình. Đợt đồng bộ sau mà gặp xung đột ở các file này thì giữ phần của T-066 (ghi vào mô tả PR của task).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- `push-probe.sh` đẩy và xóa nhánh `claude/probe-<RUN>-<giờ>-<pid>` bằng git của phiên; không dùng `--force`, không đụng `main`, không tạo PR; commit `--no-verify`, `commit.gpgsign=false` trong worktree tạm. Ưu tiên sửa một workflow không có `on: push`; test của repo kiểm mọi workflow có `on: push` chỉ chạy trên `main` (hôm nay `main-guard.yml`, `status-page.yml`), nên nhánh thử không chạy workflow nào.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Script dùng `bash`, `git`, `grep`, `mktemp`, `date` có sẵn và `scripts/failure-kind.sh`; `run-check.py` chỉ dùng thư viện chuẩn của Python.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc 1:** bộ phân loại lệnh của môi trường cloud từng từ chối `git push` của phiên con mà không giải thích (RUN-20261023-04, T-017). Lệnh đẩy nhánh thử có thể bị từ chối trước khi tới GitHub.
- **Điều kiện chuyển:** lời gọi `scripts/push-probe.sh` bị môi trường từ chối (không có mã thoát của script).
- **Cách làm thay:** coi như mã 4 (chưa rõ): ghi vào mục Kiểm tra đầu phiên, không chạy lại lệnh dưới dạng khác; làm các task theo thứ tự như thường, task sửa workflow đẩy bằng `scripts/save-branch.sh` và nếu bị từ chối thì làm như mục Khi không đẩy được bước 5 (T-067). Tiêu chí và test ở mục 4 giữ nguyên.
- **Điểm chưa chắc 2:** proxy hay quy tắc nhánh của GitHub không cho xóa nhánh thử.
- **Điều kiện chuyển:** dòng `CHÚ Ý: không xóa được nhánh claude/probe-...`.
- **Cách làm thay:** đã có trong script: mã 0 kèm CHÚ Ý, người xóa nhánh sau phiên (mục Cần người kiểm tra); lần thử sau dùng nhánh mới nên không vướng (test `a probe branch left by a refused delete does not block the next probe`).
- **Điểm chưa chắc 3:** lời từ chối thật của GitHub đã thấy một lần (chữ "refusing to allow an OAuth App to create or update workflow ... without workflow scope", RUN-20261027-01). Một cách mở khác có thể dùng chữ khác.
- **Điều kiện chuyển:** lần thử thật mã 4 mà dòng của GitHub có chữ `refusing` và `workflow`.
- **Cách làm thay:** mẫu PB-13 của T-067 nhận cả "an OAuth App" và "a GitHub App" (test `the GitHub App wording of the same refusal exits 3`). Chữ khác nữa: phiên làm như mã 3 với các task sửa workflow, ghi vào bài học để sửa dòng PB-13 (một mẫu cho cả `push-probe.sh` và mục Khi không đẩy được).

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Ô `thử`: chú ý, không lỗi | Script test | `run-check: a workflow plan in a cloud session that must probe passes` (đối chứng), `run-check: ... and warns that the session probes at its start` |
| Ô `không`: lỗi nêu task, khả năng, đường dẫn, cách mở | Script test | `run-check: a workflow plan in a session that cannot push workflows stops`, `run-check: the error names the task, the capability, the path and the way of opening`, `run-check: a plan without a workflow path gets no capability message` (đối chứng) |
| Ô `có`: không báo gì | Script test | `run-check: the same plan in a local session passes`, `run-check: a local session that can push workflows says nothing about it` (đối chứng) |
| Cách mở thật khác danh sách | Script test | `run-check: RUN_CHECK_OPEN_WITH overrides open_with` |
| `open_with` trống hay sai | Script test | `run-check: a list without open_with whose plan needs a capability passes` (đối chứng), `run-check: ... and warns that open_with is missing`, `run-check: an unknown open_with is read as missing` |
| Không plan nào cần thì không nhiễu | Script test | `run-check: a list without open_with and without such plans has no open_with message` (đối chứng), và test có sẵn `run-check: a ready list has no warning` |
| Mọi plan cần đều được nêu | Script test | `run-check: every plan that needs the missing capability is an error` |
| Chỉ hàng của bảng file, không tính chữ | Script test | `run-check: a workflow path only in the prose of a plan needs nothing` (đối chứng) |
| Cột theo tiêu đề; mẫu sai là chú ý | Script test | `run-check: columns are read by their header`, `run-check: a pattern that does not compile is a warning`, `run-check: the same reordered table lets a local session pass` (đối chứng) |
| Chỉ dòng có mẫu, trong dấu đánh dấu, được kiểm | Script test | `run-check: a row outside the markers and a row without a pattern are not checked` (đối chứng) |
| Bảng thật của repo | Script test | `run-check: the table of the repository asks a cloud session to probe workflow pushes` |
| Không có bảng | Script test | `run-check: without the table no capability is checked` (đối chứng) |
| Nhánh thử không chạy workflow nào | Script test | `push-probe: the trigger check finds a push trigger for every branch`, `push-probe: no workflow of the repository runs on a push to another branch than main` (đối chứng trên repo thật) |
| Thử qua: mã 0, `QUA`, chỉ đổi workflow không có `on: push`, nhánh xóa, thư mục không đổi, `main` không đổi | Script test | `push-probe: a push that goes through exits 0`, `push-probe: says QUA`, `push-probe: the probe commit changes only the workflow without a push trigger`, `push-probe: the probe branch is deleted again` (đối chứng), `push-probe: the checkout keeps its branch, files and worktrees` (đối chứng), `push-probe: main on the remote does not change` (đối chứng) |
| Xóa nhánh bị từ chối | Script test | `push-probe: a refused delete still says the push went through`, `push-probe: a probe branch left by a refused delete does not block the next probe` |
| Thiếu quyền `workflow` (PB-13): mã 3, `TỪ CHỐI` kèm mã dòng, lệnh `--touches`, dòng của GitHub, không thử lại | Script test | `push-probe: a push refused for the workflow scope exits 3`, `push-probe: says TỪ CHỐI with the playbook row and what the session does`, `push-probe: quotes GitHub's line`, `push-probe: a refusal is not retried`, `push-probe: a refused probe leaves no branch` (đối chứng), `push-probe: a refused probe leaves the checkout as it was` (đối chứng) |
| Cách viết của GitHub App | Script test | `push-probe: the GitHub App wording of the same refusal exits 3` |
| Bảng tra lỗi thật (PB-13 của T-067) | Script test | `push-probe: the real playbook (PB-13 of T-067) classifies the refusal` (thất bại tới khi T-067 merge) |
| Lỗi khác: mã 4, `CHƯA RÕ`, tối đa 3 dòng lỗi | Script test | `push-probe: another refusal exits 4`, `push-probe: another refusal is not called a missing workflow scope`, `push-probe: an unclear refusal shows at most the last 3 git or remote lines` |
| Lỗi mạng: thử lại | Script test | `push-probe: a network error is retried, then unclear`, `push-probe: a network error that clears goes through` |
| Tham số sai, không có workflow | Script test | `push-probe: refuses an argument that is not a run id`, `push-probe: a bad argument pushes nothing` (đối chứng), `push-probe: a repository without workflows has nothing to probe` |
| Câu hỏi rà soát có trong mẫu, tài liệu khớp | Kiểm tra thủ công (người: đọc tài liệu) | Rà soát độc lập ở bước 6 đọc `pre-run-review.md` mục 2.3, `templates/run.md`, `docs/CLAUDE.md`, `auto-run.md` mục 1, 2 và đối chiếu với mục 3.1 |
| Thử thật ở đầu phiên | Kiểm tra thủ công (người: chỉ chạy được trong phiên cloud thật) | Phiên đầu tiên có plan sửa `.github/workflows/` sau khi T-066 merge chạy `scripts/push-probe.sh` và ghi mã, dòng `QUA`, `TỪ CHỐI` hay `CHƯA RÕ` vào mục Kiểm tra đầu phiên; người đọc dòng đó trong báo cáo |

Khối test (bước 3), đúng như `diff` đã chạy trên `084d366`:

```diff
diff --git a/scripts/tests/run.sh b/scripts/tests/run.sh
index 910d4dd..67214ea 100755
--- a/scripts/tests/run.sh
+++ b/scripts/tests/run.sh
@@ -1433,6 +1433,84 @@ plan3 '- Đề xuất thứ tự chạy: T-003 chạy trước T-002.'
 expect "run-check: a hint with this plan as subject that the list follows passes" pass "$(run_check)"
 rm -f "$rr/docs/features/demo/tickets/T-003-x.md" "$rr/docs/features/demo/plans/T-003-x.md" "$rr/feature/demo/C.kt"
 
+# Rule 8: what the way of opening the session can do (L-086 of camera-pose).
+mkdir -p "$rr/docs/ops" "$rr/.github/workflows"
+touch "$rr/.github/workflows/ci.yml"
+cap_table() { # <cloud> <routine> <local>: the value of workflow-push for each way of opening
+  printf -- '# x\n\n| `workflow-push` | `^docs/` | không | không | không | outside the markers |\n\n<!-- capabilities:start -->\n| Khả năng | Mẫu đường dẫn ở mục 3.2 | cloud | routine | local | Ghi chú |\n| --- | --- | --- | --- | --- | --- |\n| `workflow-push` | `^\\.github/workflows/` | %s | %s | %s | x |\n| `r2-network` | - | không | không | có | x |\n<!-- capabilities:end -->\n\n| Khả năng | Mẫu | cloud | routine | local | Ghi chú |\n| --- | --- | --- | --- | --- | --- |\n| `workflow-push` | `^docs/` | không | không | không | another table after the markers |\n' \
+    "$1" "$2" "$3" > "$rr/docs/ops/session-capabilities.md"
+}
+open_with() { # <value>: sets open_with in the front matter of the run list written by write_run
+  perl -0pi -e "s/^status: draft\n/status: draft\nopen_with: $1\n/m" "$rr/docs/runs/RUN-1.md"
+}
+high_risk() { # <id>: a plan that touches .github/ is high risk with a cross reviewer (risk-scoring.md, mục 1)
+  perl -0pi -e 's/^risk_level: medium\n/risk_level: high\nblocking_points: [ci]\ncross_reviewer: B\n/m' "$rr/docs/features/demo/plans/$1-x.md"
+}
+write_ticket T-004 ''
+write_run_plan T-004 .github/workflows/ci.yml '| KB-1 | Unit | `cTest` |'
+high_risk T-004
+cap_table thử thử có
+write_run "$(run_row 1 T-001)$(run_row 2 T-004)"
+open_with cloud
+expect "run-check: a workflow plan in a cloud session that must probe passes" pass "$(run_check)"
+expect "run-check: ... and warns that the session probes at its start" 1 \
+  "$(grep -c "CHÚ Ý: T-004 cần 'workflow-push' (\`.github/workflows/ci.yml\` ở mục 3.2): phiên mở bằng cloud phải thử ở đầu phiên" "$work/run-check.out" || true)"
+cap_table không thử có
+expect "run-check: a workflow plan in a session that cannot push workflows stops" stop "$(run_check)"
+expect "run-check: the error names the task, the capability, the path and the way of opening" 1 \
+  "$(grep -c "LỖI: T-004 cần 'workflow-push' (\`.github/workflows/ci.yml\` ở mục 3.2) mà phiên mở bằng cloud không có" "$work/run-check.out" || true)"
+expect "run-check: a plan without a workflow path gets no capability message" 0 "$(grep -c 'T-001 cần' "$work/run-check.out" || true)"
+write_run "$(run_row 1 T-001)$(run_row 2 T-004)"
+open_with local
+expect "run-check: the same plan in a local session passes" pass "$(run_check)"
+expect "run-check: a local session that can push workflows says nothing about it" 0 "$(grep -c 'workflow-push' "$work/run-check.out" || true)"
+write_run "$(run_row 1 T-001)$(run_row 2 T-004)"
+open_with cloud
+expect "run-check: RUN_CHECK_OPEN_WITH overrides open_with" pass "$(RUN_CHECK_OPEN_WITH=local run_check)"
+write_run "$(run_row 1 T-001)$(run_row 2 T-004)"
+expect "run-check: a list without open_with whose plan needs a capability passes" pass "$(run_check)"
+expect "run-check: ... and warns that open_with is missing" 1 "$(grep -c "CHÚ Ý: danh sách chưa ghi open_with.*nhận ''.*T-004" "$work/run-check.out" || true)"
+open_with clould
+expect "run-check: an unknown open_with is read as missing" 1 "$(run_check > /dev/null; grep -c "chưa ghi open_with.*nhận 'clould'" "$work/run-check.out" || true)"
+write_run "$(run_row 1 T-001)$(run_row 2 T-002)"
+expect "run-check: a list without open_with and without such plans has no open_with message" 0 \
+  "$(run_check > /dev/null; grep -c 'open_with' "$work/run-check.out" || true)"
+write_run_plan T-002 .github/workflows/ci.yml '| KB-1 | Unit | `bTest` |'
+high_risk T-002
+write_run "$(run_row 1 T-002)$(run_row 2 T-004)"
+open_with cloud
+expect "run-check: every plan that needs the missing capability is an error" 2 "$(run_check > /dev/null; grep -c "LỖI: T-00[24] cần 'workflow-push'" "$work/run-check.out" || true)"
+write_run_plan T-002 feature/demo/B.kt '| KB-1 | Unit | `bTest` |'
+write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |' Sửa 'Cách làm: xem `.github/workflows/ci.yml`, không sửa.'
+write_run "$(run_row 1 T-001)"
+open_with cloud
+expect "run-check: a workflow path only in the prose of a plan needs nothing" "pass 0" "$(run_check) $(grep -c 'workflow-push' "$work/run-check.out" || true)"
+printf -- '<!-- capabilities:start -->\n| Khả năng | Mẫu đường dẫn ở mục 3.2 | local | routine | cloud |\n| --- | --- | --- | --- | --- |\n| `workflow-push` | `^\\.github/workflows/` | có | có | không |\n| `broken` | `(` | có | có | có |\n<!-- capabilities:end -->\n' > "$rr/docs/ops/session-capabilities.md"
+write_run "$(run_row 1 T-004)"
+open_with cloud
+expect "run-check: columns are read by their header" stop "$(run_check)"
+expect "run-check: a pattern that does not compile is a warning" 1 "$(grep -c "CHÚ Ý: docs/ops/session-capabilities.md: mẫu của 'broken' không đọc được" "$work/run-check.out" || true)"
+write_run "$(run_row 1 T-004)"
+open_with local
+expect "run-check: the same reordered table lets a local session pass" pass "$(run_check)"
+cap_table không thử có
+touch "$rr/docs/x.md"
+write_run_plan T-001 docs/x.md '| KB-1 | Unit | `aTest` |'
+write_run "$(run_row 1 T-001)"
+open_with cloud
+expect "run-check: a row outside the markers and a row without a pattern are not checked" pass "$(run_check)"
+write_run_plan T-001 feature/demo/A.kt '| KB-1 | Unit | `aTest` |'
+cp docs/ops/session-capabilities.md "$rr/docs/ops/session-capabilities.md" 2> /dev/null || rm -f "$rr/docs/ops/session-capabilities.md"
+write_run "$(run_row 1 T-004)"
+open_with cloud
+expect "run-check: the table of the repository asks a cloud session to probe workflow pushes" 1 \
+  "$(run_check > /dev/null; grep -c "CHÚ Ý: T-004 cần 'workflow-push'.*cloud phải thử" "$work/run-check.out" || true)"
+rm -f "$rr/docs/ops/session-capabilities.md"
+write_run "$(run_row 1 T-004)"
+open_with cloud
+expect "run-check: without the table no capability is checked" "pass 0" "$(run_check) $(grep -c 'workflow-push' "$work/run-check.out" || true)"
+rm -rf "$rr/docs/x.md" "$rr/docs/features/demo/tickets/T-004-x.md" "$rr/docs/features/demo/plans/T-004-x.md" "$rr/.github" "$rr/docs/ops"
+
 # Files of other plans: a plan using a file that a later plan creates (and not in its own section 3.2).
 git init -q "$rr"
 mkdir -p "$rr/feature/other"
@@ -2969,6 +3047,125 @@ expect "save-branch: a branch outside claude/is never pushed" 0 "$(sb_calls_coun
 expect "save-branch: refuses a branch that does not exist" 2 "$(sb_run claude/T-000-none)"
 export GIT_CONFIG_COUNT="${sb_git_config[0]}" GIT_CONFIG_KEY_0="${sb_git_config[1]}" GIT_CONFIG_VALUE_0="${sb_git_config[2]}"
 
+# push-probe.sh tells at the start of a session whether it may push a change to .github/workflows/ (L-086 of
+# camera-pose). A bare repository stands in for GitHub; its pre-receive hook answers as GitHub does.
+# The probe branch never runs a workflow: every workflow of the repository that runs on push runs on main only.
+push_on_other_branches() { # <dir>: prints the workflows of <dir> with a push trigger not limited to main
+  python3 -I - "$1" <<'PY'
+import glob, re, sys
+for f in sorted(glob.glob(sys.argv[1] + "/*.yml")):
+    text = open(f, encoding="utf-8").read()
+    on = re.search(r"^on:\n((?:[ #].*\n|\n)*)", text, re.M)
+    push = re.search(r"^  push:\n((?:    .*\n)*)", on.group(1) if on else "", re.M)
+    if push and "branches: [main]" not in push.group(1):
+        print(f.rsplit("/", 1)[-1])
+PY
+}
+mkdir -p "$work/pp-wf"
+printf 'on:\n  push:\n    paths: [x]\n' > "$work/pp-wf/bad.yml"
+printf 'on:\n  push:\n    branches: [main]\n  workflow_dispatch:\n' > "$work/pp-wf/good.yml"
+expect "push-probe: the trigger check finds a push trigger for every branch" bad.yml "$(push_on_other_branches "$work/pp-wf")"
+expect "push-probe: no workflow of the repository runs on a push to another branch than main" "" "$(push_on_other_branches .github/workflows)"
+pp_git_config=("$GIT_CONFIG_COUNT" "$GIT_CONFIG_KEY_0" "$GIT_CONFIG_VALUE_0")
+unset GIT_CONFIG_COUNT GIT_CONFIG_KEY_0 GIT_CONFIG_VALUE_0
+pp_root="$PWD"
+pp_origin="$work/pp-origin.git"
+pp_clone="$work/pp-clone"
+git init -q --bare "$pp_origin"
+git init -q -b main "$pp_clone"
+git -C "$pp_clone" config user.email t@example.com
+git -C "$pp_clone" config user.name t
+mkdir -p "$pp_clone/.github/workflows"
+printf 'on:\n  push:\n    branches: [main]\n' > "$pp_clone/.github/workflows/a.yml"
+printf 'on:\n  workflow_dispatch:\n' > "$pp_clone/.github/workflows/b.yml"
+printf 'x\n' > "$pp_clone/README.md"
+git -C "$pp_clone" add -A
+git -C "$pp_clone" commit -q -m base
+git -C "$pp_clone" remote add origin "$pp_origin"
+git -C "$pp_clone" push -q origin main
+git -C "$pp_clone" fetch -q origin
+git -C "$pp_clone" checkout -q -b claude/T-999-task
+# The playbook row of T-067 (PB-13, kind bo-qua-loai) in a table of its own, so the block does not depend on the
+# real table; one test below reads the real table.
+pp_table="$work/pp-playbook.md"
+printf '%s\n' '<!-- playbook:start -->' '| Mã | Mẫu | Thấy ở | Loại | Việc phải làm | Nguồn |' '| --- | --- | --- | --- | --- | --- |' \
+  '| PB-13 | `refusing to allow an? [A-Za-z ]+ to create or update workflow` | lệnh đẩy | `bo-qua-loai` | bỏ qua loại task | L-086 |' \
+  '<!-- playbook:end -->' > "$pp_table"
+pp_hook="$pp_origin/hooks/pre-receive"
+pp_files="$work/pp-pushed.files"
+pp_calls="$work/pp-hook.calls"
+pp_hook_mode() { # record | nodelete | scope | app | other | network | flaky
+  rm -f "$pp_files" "$pp_calls" "$pp_calls.count"
+  local refuse='echo x >> "'"$pp_calls"'"'
+  case "$1" in
+    record) printf '#!/bin/sh\n%s\nwhile read old new ref; do [ "$new" = 0000000000000000000000000000000000000000 ] || git diff-tree --no-commit-id --name-only -r "$new" >> "%s"; done\nexit 0\n' "$refuse" "$pp_files" > "$pp_hook" ;;
+    nodelete) printf '#!/bin/sh\n%s\nwhile read old new ref; do if [ "$new" = 0000000000000000000000000000000000000000 ]; then echo "deletion refused" >&2; exit 1; fi; done\nexit 0\n' "$refuse" > "$pp_hook" ;;
+    scope) printf '#!/bin/sh\n%s\necho %s >&2\nexit 1\n' "$refuse" "'refusing to allow an OAuth App to create or update workflow \`.github/workflows/b.yml\` without \`workflow\` scope'" > "$pp_hook" ;;
+    app) printf '#!/bin/sh\n%s\necho %s >&2\nexit 1\n' "$refuse" "'refusing to allow a GitHub App to create or update workflow \`.github/workflows/b.yml\` without \`workflows\` permission'" > "$pp_hook" ;;
+    other) printf '#!/bin/sh\n%s\necho "protected branch hook declined" >&2\necho "line two" >&2\necho "line three" >&2\necho "line four" >&2\nexit 1\n' "$refuse" > "$pp_hook" ;;
+    network) printf '#!/bin/sh\n%s\necho "fatal: The remote end hung up unexpectedly" >&2\nexit 1\n' "$refuse" > "$pp_hook" ;;
+    flaky) printf '#!/bin/sh\n%s\nif [ -f "%s.count" ]; then exit 0; fi\ntouch "%s.count"\necho "fatal: The remote end hung up unexpectedly" >&2\nexit 1\n' \
+      "$refuse" "$pp_calls" "$pp_calls" > "$pp_hook" ;;
+  esac
+  chmod +x "$pp_hook"
+}
+pp_run() { # <args>; runs push-probe.sh in the clone with the test playbook; output in $work/pp.out, prints the exit code
+  local code=0
+  (cd "$pp_clone" && PUSH_PROBE_PLAYBOOK="${PP_TABLE-$pp_table}" PUSH_PROBE_RETRY_SECONDS="${PP_RETRY:-0 0}" \
+    "$pp_root/scripts/push-probe.sh" "$@") > "$work/pp.out" 2>&1 || code=$?
+  echo "$code"
+}
+pp_probe_branches() { git -C "$pp_origin" for-each-ref --format='%(refname)' refs/heads/claude/ | grep -c 'probe-RUN-20261027-02' || true; }
+pp_calls_count() { if [ -f "$pp_calls" ]; then wc -l < "$pp_calls" | tr -d ' '; else echo 0; fi; }
+pp_untouched() { echo "$(git -C "$pp_clone" branch --show-current) $(git -C "$pp_clone" status --porcelain | wc -l | tr -d ' ') $(git -C "$pp_clone" worktree list | wc -l | tr -d ' ')"; }
+
+pp_hook_mode record
+expect "push-probe: a push that goes through exits 0" 0 "$(pp_run RUN-20261027-02)"
+expect "push-probe: says QUA" 1 "$(grep -c '^QUA: phiên đẩy được thay đổi .github/workflows/' "$work/pp.out" || true)"
+expect "push-probe: the probe commit changes only the workflow without a push trigger" ".github/workflows/b.yml" "$(cat "$pp_files")"
+expect "push-probe: the probe branch is deleted again" 0 "$(pp_probe_branches)"
+expect "push-probe: the checkout keeps its branch, files and worktrees" "claude/T-999-task 0 1" "$(pp_untouched)"
+expect "push-probe: main on the remote does not change" "$(git -C "$pp_clone" rev-parse origin/main)" "$(git -C "$pp_origin" rev-parse main)"
+pp_hook_mode nodelete
+expect "push-probe: a refused delete still says the push went through" "0 1" \
+  "$(pp_run RUN-20261027-02) $(grep -c '^CHÚ Ý: không xóa được nhánh claude/probe-RUN-20261027-02-' "$work/pp.out" || true)"
+expect "push-probe: a probe branch left by a refused delete does not block the next probe" "0 2" \
+  "$(pp_run RUN-20261027-02) $(pp_probe_branches)"
+for ref in $(git -C "$pp_origin" for-each-ref --format='%(refname)' refs/heads/claude/); do git -C "$pp_origin" update-ref -d "$ref"; done
+pp_hook_mode scope
+expect "push-probe: a push refused for the workflow scope exits 3" 3 "$(pp_run RUN-20261027-02)"
+expect "push-probe: says TỪ CHỐI with the playbook row and what the session does" "1 1" \
+  "$(grep -c '^TỪ CHỐI: PB-13 bo-qua-loai' "$work/pp.out" || true) $(grep -c 'plan-check.py --touches .github/workflows/' "$work/pp.out" || true)"
+expect "push-probe: quotes GitHub's line" 1 "$(grep -c 'without .workflow. scope' "$work/pp.out" || true)"
+expect "push-probe: a refusal is not retried" 1 "$(pp_calls_count)"
+expect "push-probe: a refused probe leaves no branch" 0 "$(pp_probe_branches)"
+expect "push-probe: a refused probe leaves the checkout as it was" "claude/T-999-task 0 1" "$(pp_untouched)"
+pp_hook_mode app
+expect "push-probe: the GitHub App wording of the same refusal exits 3" 3 "$(pp_run RUN-20261027-02)"
+pp_hook_mode scope
+expect "push-probe: the real playbook (PB-13 of T-067) classifies the refusal" 3 "$(PP_TABLE='' pp_run RUN-20261027-02)"
+pp_hook_mode other
+expect "push-probe: another refusal exits 4" 4 "$(pp_run RUN-20261027-02)"
+expect "push-probe: another refusal is not called a missing workflow scope" "0 1" \
+  "$(grep -c 'TỪ CHỐI' "$work/pp.out" || true) $(grep -c '^CHƯA RÕ: không đẩy được nhánh thử' "$work/pp.out" || true)"
+expect "push-probe: an unclear refusal shows at most the last 3 git or remote lines" "3 1" \
+  "$(grep -c '^remote:\|^ ! \|^error:\|^fatal:' "$work/pp.out" || true) $(grep -c 'remote: line four' "$work/pp.out" || true)"
+pp_hook_mode network
+expect "push-probe: a network error is retried, then unclear" "4 3" "$(pp_run RUN-20261027-02) $(pp_calls_count)"
+pp_hook_mode flaky
+expect "push-probe: a network error that clears goes through" "0 3" "$(pp_run RUN-20261027-02) $(pp_calls_count)"
+pp_hook_mode record
+expect "push-probe: refuses an argument that is not a run id" 2 "$(pp_run RUN-abc)"
+expect "push-probe: a bad argument pushes nothing" 0 "$(pp_calls_count)"
+git -C "$pp_clone" checkout -q main
+git -C "$pp_clone" rm -q -r .github
+git -C "$pp_clone" commit -q -m "no workflows"
+git -C "$pp_clone" push -q origin main
+git -C "$pp_clone" fetch -q origin
+git -C "$pp_clone" checkout -q claude/T-999-task
+expect "push-probe: a repository without workflows has nothing to probe" 2 "$(pp_run RUN-20261027-02)"
+export GIT_CONFIG_COUNT="${pp_git_config[0]}" GIT_CONFIG_KEY_0="${pp_git_config[1]}" GIT_CONFIG_VALUE_0="${pp_git_config[2]}"
+
 # In-place sed without a backup suffix is GNU only: BSD sed on macOS stops with "invalid command code" (L-088).
 # Scripts write `sed -i.bak ... && rm -f file.bak`, or use perl or python. The pattern is built from two
 # parts so this file does not match itself.
```

**Ngoại lệ viết test trước:** tài liệu (`docs/ops/session-capabilities.md` trừ phần bảng mà test đọc, `docs/templates/run.md`, `docs/CLAUDE.md`, `docs/ops/pre-run-review.md`, `docs/auto-run.md`, `CLAUDE.md`, `docs/lessons.md`) không có test; rà soát độc lập đọc lại (mục 4, dòng kiểm tra thủ công). Lần thử thật trong phiên cloud chỉ có ở phiên sau khi merge.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-27, trên nhánh chính `084d366`, trong worktree `../wt-t066`, khối tạm không commit: hai khối test của mục 4 cùng code của mục 3.1 (`scripts/run-check.py`, `scripts/push-probe.sh`, `docs/ops/session-capabilities.md`). Để kiểm phần dựa vào T-067, chép tạm dòng PB-13 từ plan T-067 vào `docs/ops/failure-playbook.md` (và một tệp `scripts/tests/playbook/PB-13.log` tạm). Sau đó hoàn tác hết, nhánh plan chỉ có ticket, plan và `lessons.md`.
- **Lệnh:** `bash scripts/tests/run.sh` (toàn bộ, JDK 21 để chạy khối `design-compare`), trên code hiện tại và trên bản cài thử; đột biến chạy riêng hai khối mới (đầu `run.sh` dòng 1 đến 26 cộng khối).
- **Kết quả:**
  - Code hiện tại cộng test mới: 761 ok, 27 thất bại, đúng lý do. Quy tắc 8 (9 test): `expected 'stop', got 'pass'`, hay số dòng `LỖI`/`CHÚ Ý` mong 1 hoặc 2 mà ra 0, vì `run-check.py` chưa đọc `open_with`. `push-probe` (18 test): mã 127 (script chưa có), không có `QUA`, `TỪ CHỐI`, `CHƯA RÕ`, không commit nào được đẩy, số lần gọi hook 0. 19 đối chứng qua: 11 của quy tắc 8 (danh sách qua khi không có lỗi, không nhiễu khi không plan nào cần, chữ của plan không tính), 8 của `push-probe` (kiểm `on: push` của repo và của khung test, không có nhánh thừa, thư mục và `main` không đổi, tham số sai không đẩy gì); các đối chứng này có nghĩa nhờ các test thất bại cùng nhóm.
  - Bản cài thử, chưa có PB-13: 787 ok, 1 thất bại, đúng lý do: `push-probe: the real playbook (PB-13 of T-067) classifies the refusal` ra mã 4 vì bảng tra lỗi chưa có dòng `bo-qua-loai`.
  - Bản cài thử cùng dòng PB-13 của T-067: 788 ok, 0 thất bại ("All script tests passed"); 742 test có sẵn không đổi kết quả, gồm `run-check: a ready list has no warning` và `run-check: the latest run list has no new error from rule 11`.
  - Lỗi của chính khung test lúc viết (đã sửa trong khối của mục 4): plan mẫu sửa `.github/workflows/ci.yml` với `risk_level: medium` nên `plan-check` báo chạm điểm chặn (hàm `high_risk`); plan sửa `docs/x.md` mà file chưa có (`touch`); dấu backtick trong `echo "..."` của hook `sh` thành lệnh (nháy đơn); bảng mẫu có cột đổi chỗ mà `cloud` vẫn ở cột 3 không bắt được đột biến cột theo vị trí (đổi thứ tự cột); một bảng thứ hai nằm sau dấu kết thúc để bắt đột biến đọc cả file.
  - 20 đột biến, đều bị bắt. Số test thất bại của `push-probe` tính khi chưa có PB-13 (so với 1 thất bại sẵn có):

| # | Đột biến | Test thất bại thêm |
| --- | --- | --- |
| M1 | Ô `thử` thành lỗi | 3 |
| M2 | Ô `không` thành chú ý | 4 |
| M3 | Bỏ `RUN_CHECK_OPEN_WITH` | 1 |
| M4 | Đọc cả file, không theo dấu `capabilities:start`/`end` | 1 |
| M5 | Báo thiếu `open_with` cả khi không plan nào cần | 2 |
| M6 | Chỉ nêu plan đầu tiên cần khả năng | 1 |
| M7 | Dòng có Mẫu `-` cũng được kiểm | 1 |
| M8 | Cột đọc theo vị trí, không theo tiêu đề | 2 |
| M9 | Mẫu sai bỏ qua mà không báo | 1 |
| MA | Tính cả đường dẫn trong chữ của plan | 1 |
| P1 | Nhánh thử đổi `README.md` thay vì file workflow | 1 |
| P2 | Không xóa nhánh thử sau khi đẩy được | 4 |
| P3 | Mọi lỗi đẩy là `bo-qua-loai` | 4 |
| P5 | Commit trong thư mục làm việc thay vì worktree tạm | 1 |
| P6 | Không kiểm tham số | 2 |
| P7 | Tên nhánh thử cố định | 2 |
| P8 | Luôn sửa workflow đầu tiên, kể cả có `on: push` | 1 |
| P9 | Không thử lại lỗi mạng | 2 |
| P10 | Mẫu riêng chỉ nhận "an OAuth App" thay cho bảng tra lỗi | 1: cách viết GitHub App ra mã 4 (tổng vẫn 1 vì test bảng thật lại qua với mẫu riêng) |
| P11 | In 3 dòng cuối không lọc | 1 |

  - Chưa chạy thử trong phiên `claude --cloud` thật (cần token và mạng của phiên): mục 4, dòng cuối; mục 3.4.
  - `scripts/check.sh` không chạy: plan không đổi code Kotlin hay Gradle; `scripts/tests/run.sh` là phần của `check.sh` và đã chạy toàn bộ.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (quy trình phiên tự chạy): `docs/auto-run.md` mục 2 thêm một bước đầu phiên; `run-check.py` thêm một lỗi chặn duyệt danh sách. Mức Cao, duyệt chéo; `risk_score` để trống.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Hai script (một mới), test, sáu tài liệu quy trình; không đổi code app |
| B. Độ rõ của yêu cầu | 1 | Tuấn chọn đề xuất 1a, 1b, 4c; giá trị của các ô `workflow-push` là câu hỏi 1 và 5 |
| C. Khả năng kiểm chứng tự động | 2 | 46 test, 20 đột biến; nhưng lời từ chối thật, quyền xóa nhánh thật và token của phiên cloud chỉ kiểm được ở lần thử đầu tiên trong phiên |
| D. Ảnh hưởng chéo | 2 | Mọi danh sách chạy sau đi qua quy tắc 8; mọi phiên có plan sửa workflow chạy `push-probe.sh`; dựa vào PB-13, `--touches` và L8 của T-067 |
| E. Độ mới của kỹ thuật | 1 | Đẩy và xóa nhánh bằng git, thử lại lỗi mạng, đã có trong `save-branch.sh`; hook `pre-receive` làm GitHub giả như khối `save-branch` |
| **Tổng** | 8 | **Mức: Cao** (chạm điểm chặn 7) |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `scripts/run-check.py`, `scripts/tests/run.sh` (khối `run-check` và một khối mới sau khối `save-branch`), `docs/auto-run.md` mục 1, 2, `docs/ops/pre-run-review.md`, `docs/templates/run.md`, `docs/CLAUDE.md`, `CLAUDE.md`, `docs/lessons.md`.
- Task khác có thể xung đột:
  - T-067 (PB-13 loại `bo-qua-loai`, `plan-check.py --touches`, ADR-1005 với L8, mục "Quyết định AI đã tự làm"): chạy trước T-066 trong cùng danh sách (quyết định của người điều phối), và T-066 phụ thuộc T-067 (`depends_on`). T-066 dùng ba thứ đó như T-067 viết; dòng mục 2 của T-066 nhắc PB-13 và L8 theo nội dung. Cùng sửa `docs/auto-run.md` (T-067: mục Khi không đẩy được bước 5; T-066: mục 1 bước 3, mục 2), `docs/templates/run.md` (T-067: mục Quyết định AI đã tự làm; T-066: front matter và mục Kiểm tra trước khi duyệt), `scripts/tests/run.sh` (khối khác). Task sau tạo nhánh từ nhánh chính mới nhất và đặt chữ của mình theo nội dung sau T-067.
  - `docs/lessons.md`, L-086: PR plan này đặt `accepted` và dòng Áp dụng "làm ở ticket T-066 (...)"; phiên của T-067 thêm "; T-067: ..." vào cuối cùng dòng. T-066 giữ nguyên chữ của mình và của T-067; ở bước 10 chỉ chuyển trạng thái sang `applied` khi cả hai phần đã merge, và thêm số PR vào phần của T-066.
  - T-068 (`save-branch.sh` ghi patch, `run-watchdog.yml`): cùng sửa `scripts/tests/run.sh`. Khối `push-probe` đặt sau dòng đóng khối `save-branch`; nếu T-068 thêm test vào cuối khối `save-branch` thì đặt trước dòng đó. `run-watchdog.yml` của T-068 phải chạy theo lịch hay bằng tay, không `on: push` cho nhánh khác `main`, nếu không test `no workflow of the repository runs on a push to another branch than main` đỏ.
  - T-065 (danh sách RUN-20261027-02): sửa `scripts/tests/run.sh` ở khối `setup-check`, và `docs/template-sync.txt`; không chung khối.
- Đề xuất thứ tự chạy: sau T-067. Nên merge trước khi lập lại danh sách cho T-062 và T-065, để danh sách đó có `open_with` và phiên thử đẩy ở đầu phiên.

## 7. Điều kiện dừng riêng

Không có.

## 8. Câu hỏi cho người duyệt

1. Ô `workflow-push` của `cloud` và `routine`: AI đề xuất `thử` (tài khoản của Nhật đẩy được ở PR #179, của Tuấn ở RUN-20261027-01 thì không; Tuấn đang cấp lại quyền), để `run-check.py` chỉ chú ý và phiên thử ở đầu phiên. Người đổi thành `có` hay `không` khi đã thử chắc chắn ở hai phiên liên tiếp, kèm bằng chứng.
2. `open_with` trống: AI đề xuất chỉ chú ý khi có plan cần khả năng, không bắt buộc ở mọi danh sách, để danh sách cũ và danh sách không có plan đặc biệt không nhiễu. Cách khác: lỗi khi trống.
3. Nhánh thử thêm một dòng chú thích vào cuối một workflow không có `on: push`, đẩy rồi xóa ngay. AI đề xuất đồng ý. Cách khác (thử bằng một file workflow mới) để lại một workflow trong danh sách workflow của repo.
4. Đề xuất về dự án mẫu: AI ghi vào mô tả PR của task và mục Cần người kiểm tra của báo cáo; người mở issue `from-app` (phiên không mở issue ở repo khác).
5. Ô `workflow-push` của `local`: AI đề xuất `thử` (đẩy được khi git của máy dùng SSH, hay `gh auth status` có quyền `workflow`; máy này đẩy được qua `gh`, nhưng SSH bị từ chối khóa). Cách khác: `có` kèm điều kiện, khi đó phiên local không thử.
6. GitHub không cho xóa nhánh thử: AI đề xuất vẫn coi là qua (mã 0) kèm dòng CHÚ Ý cho người xóa sau phiên, vì quyền đẩy đã được chứng minh và lần thử sau dùng nhánh mới. Cách khác: mã 4 (chưa rõ), phiên làm các task như thường.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-27, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8: ô `workflow-push` của cloud, routine, local để `thử` tới khi hai phiên liền nhau cho cùng kết quả; `open_with` trống chỉ CHÚ Ý khi có plan cần khả năng; nhánh thử sửa một dòng chú thích của workflow có sẵn, mỗi lần một tên mới; đề xuất về dự án mẫu qua mô tả PR và mục Cần người kiểm tra.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261027-02, phiên bản plan 1

- **Kết quả:** đã merge (2026-10-27 18:35 UTC, commit `2bbe19f` trên nhánh chính).
- **Nhánh, PR:** `claude/T-066-kha-nang-cach-mo-phien`, PR #197.
- **Đã làm:** phiên chính tự làm. `diff` của `scripts/run-check.py` và khối test áp sạch (`git apply --check`) trên nhánh chính `e263212` (sau T-067, T-069); `scripts/push-probe.sh`, `docs/ops/session-capabilities.md` chép nguyên khối của mục 3.1; chín câu tài liệu đặt đúng chỗ của mục 3.1 (rà soát so từng chỗ). L-086 sang `applied`, thêm PR #197 vào phần của T-066, giữ phần của T-067 (bước 10, trên nhánh báo cáo).
- **Test viết trước:** 46 test (commit `e52da31`): 27 thất bại đúng lý do (quy tắc 8 chưa có; `push-probe.sh` chưa có, mã 127), 19 đối chứng qua, đúng mục 4.1 (test bảng tra lỗi thật qua vì T-067 đã merge, nằm trong số 27 vì `push-probe.sh` chưa có). Sau code: `scripts/tests/run.sh` 868 ok.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 2 ghi chú: `push-probe.sh` gọi từ thư mục con thì `git ls-tree origin/main .github/workflows/` rỗng, báo mã 2 với lời sai (an toàn; đề xuất `--full-tree` hay `cd` về gốc repo); script bị giết hẳn giữa chừng để lại mục trong `.git/worktrees` (`git worktree prune` dọn được).
- **Kết quả CI:** `scripts/check.sh` trên máy: QUA. `pr-check` lần chạy 40560591036: xanh ở vòng đầu. Tổng 1 vòng CI.
- **Lệch so với plan:** không có. Ngoài mục 3.2: không có. Lúc áp, phiên ghép nhầm thứ tự ba khối chữ của `docs/auto-run.md` và `docs/ops/pre-run-review.md` trên thư mục làm việc, thấy ngay ở `git diff`, hoàn tác và đặt lại trước khi commit; commit không có bản sai.
- **Việc còn lại:** kiểm tra thủ công của mục 4 (lần thử thật ở đầu phiên: chính phiên này chạy `push-probe.sh` trước nhánh của T-068, kết quả ở mục Kiểm tra đầu phiên của báo cáo). Đề xuất về dự án mẫu. Người xóa tay nhánh `claude/T-066-kha-nang-cach-mo-phien` (L-092).
- **Cần người xem:** hai ghi chú của rà soát ở trên.
