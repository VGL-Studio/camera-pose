---
ticket: T-068
title: Nhánh bị GitHub từ chối vì quyền workflow được giao cho người thành patch trên nhánh báo cáo; báo người khi phiên tự chạy đứng im (run-watchdog)
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 7.6M         # cỡ M, số gợi ý của scripts/token-usage.py estimate (2026-10-27), không nâng tay (L-081)
blocking_points: [ci, process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-27
---

# PLAN T-068: NHÁNH BỊ TỪ CHỐI VÌ QUYỀN WORKFLOW GIAO CHO NGƯỜI THÀNH PATCH; BÁO NGƯỜI KHI PHIÊN ĐỨNG IM

## 1. Tóm tắt

Hai việc, cùng mục đích là phiên tự chạy không mất việc và không đứng im mà không ai biết (đề xuất 3b, 4b sau RUN-20261027-01, Tuấn chọn ngày 2026-10-27):

- **3b.** Khi chính GitHub từ chối lệnh đẩy nhánh task vì token không có quyền `workflow` (thông báo "refusing to allow an OAuth App to create or update workflow ... without `workflow` scope", L-086), `scripts/save-branch.sh` ngoài bundle như hôm nay còn ghi các commit thành một tệp văn bản trên nhánh báo cáo (`docs/runs/patches/<RUN>/<nhánh>.mbox`, hay `.diff` khi nhánh có commit merge) và đẩy nhánh báo cáo. Đây là giao nội dung cho người xem rồi tự quyết áp hay không, không phải đẩy nhánh dưới dạng khác: không bao giờ ghi khi `--bundle-only` (môi trường từ chối lời gọi đẩy) hay với lần từ chối khác. Patch được kiểm bằng cách áp lại trong một worktree tạm (cây phải trùng nhánh), không quá 1 MB, phải là UTF-8 và qua `scripts/secret-scan.py`; mọi lỗi của phần này chỉ thành một dòng ghi chú, bundle vẫn giữ, mã thoát vẫn 3.
- **4b.** `scripts/run-watchdog.py` (mới) và `.github/workflows/run-watchdog.yml` (mới, mỗi 30 phút, khoảng 48 phút Actions mỗi ngày): danh sách chạy `running` mà các nhánh của phiên không có commit 60 phút (90 phút khi đang có lần chạy workflow của chính phiên, hay trên nhánh chính do đẩy hoặc chạy tay) thì mở một issue `session-stalled: RUN-...` có sẵn prompt tiếp tục của `docs/ops/staging-and-runs.md` mục 2.7; cập nhật ở các lần sau; đóng khi phiên commit lại hay danh sách không còn `running`. Đọc hết rồi mới ghi, lỗi đọc nào khác 404 thì mã 1 và không đổi issue nào. Chỉ ghi issue mang nhãn `session-stalled`, không kích hoạt phiên hay routine.

Code và 99 test (64 của `save-branch`, 35 của `run-watchdog`) đã chạy thật trên nhánh chính `084d366` (mục 4.1): trên code hiện tại 53 test thất bại đúng lý do, 46 đối chứng qua; với bản cài thử cả 99 qua, toàn bộ `scripts/tests/run.sh` qua; 41 đột biến, 40 bị bắt, 1 tương đương. Script watchdog chạy thật với GitHub (chỉ đọc) và trong một workflow tạm trên runner.

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là đề xuất 3b và 4b mà Tuấn chọn sau RUN-20261027-01, cùng bài học L-084 (phiên đứng 2 giờ 30 phút ở RUN-20261026-03 vì không có gì báo người) và L-086 (T-062 làm xong nhưng không đẩy được; bundle nằm trên máy cloud).

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| 3b, chỉ đúng trường hợp quyền `workflow` | Khi `git push` thật bị GitHub từ chối với thông báo thiếu quyền `workflow` thì có patch; khi bị từ chối vì lý do khác, hay khi chạy `--bundle-only`, thì không có tệp nào trên nhánh báo cáo, worktree báo cáo sạch, và có một dòng nói vì sao | `--bundle-only` là lúc bộ phân loại của môi trường chặn lời gọi đẩy: ghi nội dung đó sang chỗ khác là đẩy lại dưới dạng khác (`docs/auto-run.md`, Khi không đẩy được, ý 1), nên không làm |
| 3b, giữ phần chưa đẩy ngoài máy cloud | Khi có patch thì tệp `docs/runs/patches/<RUN>/<nhánh với / thành __>.mbox` (hay `.diff`) có trên nhánh báo cáo ở GitHub, trong một commit riêng; chạy lại lần nữa với cùng patch thì không thêm commit mà vẫn đẩy và vẫn in lệnh | Commit bằng `git commit -- <tệp>`: phần đang soạn khác trong worktree báo cáo không bị commit theo; commit lỗi thì bỏ `add`, trả tệp về bản đã commit nếu có (không xóa patch cũ), không thì xóa tệp |
| 3b, người lấy lại được | Khi người chạy các lệnh script in ra trong một bản sao mới thì có nhánh cùng tên, cùng cây thư mục với nhánh gốc, kể cả file nhị phân và file `.github/workflows/`; nhánh có commit merge của nhánh chính thì một bản `git diff` gộp, áp bằng `git apply --index` | Patch chỉ được ghi khi áp thử trong worktree tạm cho đúng cây (`write-tree`); không dựng được thì ghi chú |
| 3b, không làm lộ bí mật | Khi patch có chuỗi giống bí mật, hay có byte không phải UTF-8 (`secret-scan.py` bỏ qua tệp như vậy) thì không có tệp nào trên nhánh báo cáo hay trong worktree, script không in chuỗi đó | Quét bản patch trong thư mục tạm trước khi chép |
| 3b, không đẩy tệp lớn | Khi patch lớn hơn `SAVE_BRANCH_PATCH_MAX_BYTES` (mặc định 1048576) thì không ghi, báo lý do | |
| 3b, chỗ ghi | Có đúng một worktree của nhánh báo cáo thì dùng nó; không có mà có đúng một nhánh `claude/RUN-*-report` cục bộ thì mở một worktree tạm rồi xóa (kể cả khi script dừng giữa chừng: `trap`, `git worktree prune`); có nhiều thì ghi chú, không chọn; không có nhánh báo cáo, nhánh chưa đẩy là chính nhánh báo cáo, hay nhánh báo cáo trên máy và trên GitHub đã khác nhau thì ghi chú | Trong phiên, nhánh báo cáo nằm trong `../report` (prompt của phiên `claude --cloud`, `docs/ops/staging-and-runs.md` mục 2.7) |
| 3b, không làm hỏng điều đã có | Khi nhánh báo cáo cũng không đẩy được, hay khi commit lỗi thì vẫn có bundle, vẫn mã 3, có một dòng ghi chú và không in lệnh lấy lại | Mọi test cũ của khối `save-branch` giữ nguyên |
| 4b, báo người khi phiên đứng im | Khi danh sách chạy trên nhánh báo cáo là `running` và các nhánh của phiên (nhánh báo cáo, `claude/T-NNN-...`, `claude/fix-T-NNN-...` của các ticket trong danh sách, như `scripts/resume-check.py`) không có commit từ 60 phút trở lên thì có một issue mở `session-stalled: RUN-...` mang nhãn `session-stalled`; thân có cả prompt của `staging-and-runs.md` mục 2.7 với tên danh sách và nhánh báo cáo của phiên, dòng "Đây là phiên tiếp tục: ...", và lời dặn không mở thêm phiên tay khi phiên mở bằng routine | 59 phút chưa báo, 60 phút báo; nhãn chưa có thì tạo |
| 4b, không báo nhầm khi phiên đang chờ | Khi có lần chạy workflow `queued` hay `in_progress` tạo sau commit cuối (cho dư 5 phút), trên một nhánh của phiên hay trên nhánh chính do `push` hoặc `workflow_dispatch`, thì chưa mở issue cho tới 90 phút; lần chạy trên nhánh khác, theo lịch, hay của chính `run-watchdog` không tính | Chờ CI (`pull_request` trên nhánh task), `main-guard` (push), `device-session`, `staging` (chạy tay) đều là workflow |
| 4b, một issue mỗi phiên, tự đóng | Khi đã có issue mở cho phiên thì chỉ cập nhật thân; khi phiên có commit trong 60 phút hay danh sách không còn `running` thì đóng trước, rồi bình luận (đóng lỗi thì không có bình luận, để lần sau không thêm bình luận) | So theo tiêu đề `session-stalled: RUN-...`; bỏ PR. Danh sách cũ còn kẹt ở `running` thì issue của nó còn mở tới khi người sửa thành `finished` |
| 4b, không đụng việc khác | Khi có issue khác (ví dụ `main-broken`) thì không ghi gì vào chúng; workflow không kích hoạt phiên hay routine, không cần bí mật | Quyền `contents: read`, `issues: write`, `actions: read` |
| 4b, đọc hay ghi GitHub lỗi | Khi một lần đọc lỗi khác 404 (ví dụ 500 khi đọc danh sách chạy của phiên thứ hai, 502 khi đọc lần chạy workflow) thì mã 1, không đổi issue nào, kể cả issue của phiên đọc trước; khi một lần ghi lỗi thì mã 1 và dòng "Ghi issue lỗi" | Đọc hết rồi mới ghi; 404 của tệp, nhánh hay nhãn là bình thường |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0009 (tài khoản, quyền nhỏ nhất), ADR-0010 (cổng chất lượng), ADR-0025 (nhánh chính tự hồi phục: watchdog không đụng `main-broken`), ADR-0029 (phạm vi). Bài học: L-008, L-046 (hết phút Actions), L-081, L-084, L-086 của app.

## 3. Giải pháp

### 3.1. Cách làm

**`scripts/save-branch.sh`** (file của dự án mẫu; đề xuất về mẫu sau khi merge, mục 6). Phần mới chạy sau khi đã có bundle, nên mọi nhánh lỗi của nó chỉ đổi dòng ghi chú cuối đoạn "Nhánh chưa đẩy":

1. Chỉ khi lệnh `git push` thật của script bị từ chối với thông báo khớp `refusing to allow an? (OAuth App|GitHub App|Personal Access Token)[^)]*workflow` (đúng câu GitHub trả ở RUN-20261027-01). `--bundle-only` và mọi lần từ chối khác: ghi chú, không ghi patch.
2. Tìm worktree của nhánh báo cáo: `SAVE_BRANCH_REPORT_DIR`; không có thì worktree đang ở nhánh `claude/RUN-*-report` trong `git worktree list --porcelain`; không có thì mở một worktree tạm trên nhánh `claude/RUN-*-report` cục bộ. Có hơn một (worktree hay nhánh): ghi chú, không chọn. Nhánh không khớp `claude/RUN-YYYYMMDD-NN-report`, hay chính là nhánh chưa đẩy: ghi chú. Worktree và thư mục tạm được dọn bằng `trap ... EXIT` và `git worktree prune`, kể cả khi script dừng giữa chừng.
3. Gốc là `git merge-base origin/main <nhánh>`. Dựng patch trong thư mục tạm, kiểm bằng cách áp vào một worktree tạm ở gốc: trước hết `git format-patch --binary --stdout` rồi `git am`; cây khác nhánh (nhánh có commit merge, merge bỏ bớt thay đổi) hay `git am` lỗi thì thử `git diff --binary <gốc> <nhánh>` rồi `git apply --index`; cả hai không ra đúng cây (ví dụ chỉ có commit rỗng) thì ghi chú. Kiểm mã thoát và tệp không rỗng; `git am` chạy với `-c commit.gpgsign=false` và tên người commit tạm. Không mở được worktree tạm thì ghi chú riêng (khác "không dựng được").
4. Lớn hơn giới hạn, không đọc được bằng UTF-8, hay `SECRET_SCAN_ROOT=<thư mục tạm> scripts/secret-scan.py` báo lỗi: ghi chú, không in giá trị. Thư mục tạm luôn bị xóa.
5. `git fetch origin <nhánh báo cáo>`: bản trên GitHub có commit không có trên máy (hai bên đã khác nhau) thì ghi chú, để phiên gộp nhánh báo cáo trước. Tệp đã có và giống hệt thì không commit lại. Không thì chép vào `<worktree báo cáo>/docs/runs/patches/<RUN>/<nhánh, / thành __>.<mbox|diff>`, `git add`, `git commit -m "<RUN>: patch của nhánh chưa đẩy <nhánh> (<mã>)" -- <tệp>`; commit lỗi thì `git reset -q -- <tệp>`, rồi `git checkout -- <tệp>` khi tệp đã được theo dõi (giữ patch cũ), không thì xóa tệp. Đẩy `HEAD:<nhánh báo cáo>` một lần (không thử lại).
6. Đẩy được thì in dòng "Patch trên nhánh báo cáo, để người xem rồi tự quyết áp hay không" và bốn lệnh lấy lại (`git fetch origin <nhánh báo cáo> main`, `git switch -c <nhánh> <gốc>`, `git show ... | git am` hay `| git apply --index && git commit`, `git push`), cùng câu "Sau khi PR báo cáo đã merge, thay `origin/<nhánh báo cáo>` bằng `origin/main`"; không thì in dòng ghi chú.

`diff` trên nhánh chính `084d366`:

```diff
diff --git a/scripts/save-branch.sh b/scripts/save-branch.sh
index 4d04bf7..ba3159b 100755
--- a/scripts/save-branch.sh
+++ b/scripts/save-branch.sh
@@ -7,6 +7,17 @@
 # Exit:    0 pushed. 3 not pushed (with a bundle when the branch has commits beyond origin/main). 2 bad arguments.
 # Env:     SAVE_BRANCH_DIR            where bundles go (default $HOME/rescue, outside any worktree)
 #          SAVE_BRANCH_RETRY_SECONDS  waits between retries of a network error (default "60 120 240")
+#          SAVE_BRANCH_REPORT_DIR     worktree of the report branch (default: the worktree on claude/RUN-*-report,
+#                                     or a temporary one on the local claude/RUN-*-report branch)
+#          SAVE_BRANCH_PATCH_MAX_BYTES  largest patch kept on the report branch (default 1048576)
+# Hand-off as text (camera-pose L-086, T-068): when GitHub itself refuses the push because the token may not change
+# .github/workflows/ (the message below), the commits are also written as a patch on the report branch, which is
+# pushed, so a person can review and apply them after the cloud machine is gone; the bundle stays on that machine.
+# This is the only case: never with --bundle-only (the environment refused the push call, and writing the same
+# content elsewhere would be pushing it again in another form), never for any other refusal. The patch is an mbox
+# (git format-patch) when replaying it rebuilds the branch exactly, else one combined diff (a branch with a merge
+# of main); it is checked by applying it in a temporary worktree. It is left out when that check fails, when it
+# is larger than the limit, when it is not UTF-8 text, or when scripts/secret-scan.py finds a secret in it.
 # Never retries a push the environment refused: that call never reaches this script, so the session itself must
 # not push again in another form (auto-run.md, "Khi không đẩy được").
 # Runs in the current repository, so it works from a worktree.
@@ -15,6 +26,9 @@
 set -uo pipefail
 
 bundle_only=no
+patch_handoff=no
+# GitHub's refusal of a push that changes .github/workflows/ without the workflow scope (L-086).
+workflow_scope_pattern='refusing to allow an? (OAuth App|GitHub App|Personal Access Token)[^)]*workflow'
 if [ "${1:-}" = "--bundle-only" ]; then
   bundle_only=yes
   shift
@@ -49,6 +63,9 @@ if [ "${bundle_only}" = "no" ]; then
       continue
     fi
     echo "${err}" >&2
+    if grep -qE "${workflow_scope_pattern}" <<< "${err}"; then
+      patch_handoff=yes
+    fi
     break
   done
 fi
@@ -68,6 +85,159 @@ if ! git bundle create "${bundle}" "${branch}" --not origin/main > /dev/null 2>&
 fi
 size="$(wc -c < "${bundle}" | tr -d ' ')"
 echo "CHƯA ĐẨY: ${branch} (${short_sha}). Đã giữ ${count} commit trong bundle."
+
+# Writes the hand-off patch into $1 (a temporary folder) and prints its mode, mbox or diff, when replaying it on
+# the base in a scratch worktree gives exactly the branch's tree; prints nothing otherwise.
+make_patch() {
+  local dir="$1" scratch mode
+  scratch="${dir}/verify"
+  for mode in mbox diff; do
+    rm -rf "${scratch}"
+    if ! git worktree add -q --detach "${scratch}" "${base}" > /dev/null 2>&1; then
+      echo "noworktree"
+      return 0
+    fi
+    if [ "${mode}" = mbox ]; then
+      git format-patch --binary --stdout "${base}..${branch}" > "${dir}/patch" 2> /dev/null || { rm -f "${dir}/patch"; }
+      [ -s "${dir}/patch" ] && git -C "${scratch}" -c user.name=save-branch -c user.email=save-branch@localhost \
+        -c commit.gpgsign=false am -q < "${dir}/patch" > /dev/null 2>&1 \
+        || { git -C "${scratch}" am --abort > /dev/null 2>&1; rm -f "${dir}/patch"; }
+    else
+      git diff --binary "${base}" "${branch}" > "${dir}/patch" 2> /dev/null || rm -f "${dir}/patch"
+      [ -s "${dir}/patch" ] && git -C "${scratch}" apply --index "${dir}/patch" > /dev/null 2>&1 || rm -f "${dir}/patch"
+    fi
+    if [ -s "${dir}/patch" ] && [ "$(git -C "${scratch}" write-tree 2> /dev/null)" = "$(git rev-parse "${branch}^{tree}")" ]; then
+      git worktree remove --force "${scratch}" > /dev/null 2>&1
+      echo "${mode}"
+      return 0
+    fi
+    rm -f "${dir}/patch"
+    git worktree remove --force "${scratch}" > /dev/null 2>&1
+  done
+}
+
+patch_note=""
+patch_lines=""
+temp_report=""
+work_dir=""
+# Temporary worktrees and folders go even when the script is stopped half way; a stale worktree entry is pruned.
+cleanup() {
+  if [ -n "${work_dir}" ]; then
+    git worktree remove --force "${work_dir}/verify" > /dev/null 2>&1
+    rm -rf "${work_dir}"
+  fi
+  if [ -n "${temp_report}" ]; then
+    git worktree remove --force "${temp_report}" > /dev/null 2>&1
+    rm -rf "$(dirname "${temp_report}")"
+  fi
+  git worktree prune > /dev/null 2>&1
+}
+trap cleanup EXIT
+if [ "${bundle_only}" = "yes" ]; then
+  patch_note="Không ghi patch: lệnh đẩy bị môi trường từ chối (--bundle-only), không đẩy lại dưới dạng khác (auto-run.md, Khi không đẩy được)."
+elif [ "${patch_handoff}" = "no" ]; then
+  patch_note="Không ghi patch: chỉ ghi khi GitHub từ chối vì quyền workflow (L-086); lần từ chối này khác."
+else
+  report_dir="${SAVE_BRANCH_REPORT_DIR:-}"
+  many_reports=no
+  if [ -z "${report_dir}" ]; then
+    found="$(git worktree list --porcelain | awk '
+      /^worktree / { dir = substr($0, 10) }
+      /^branch refs\/heads\/claude\/RUN-[0-9]+-[0-9]+-report$/ { print dir }')"
+    if [ "$(grep -c . <<< "${found}")" -gt 1 ]; then
+      many_reports=yes
+    else
+      report_dir="${found}"
+    fi
+  fi
+  if [ -z "${report_dir}" ] && [ "${many_reports}" = "no" ]; then
+    local_reports="$(git for-each-ref --format='%(refname:short)' 'refs/heads/claude/RUN-*-report')"
+    if [ "$(grep -c . <<< "${local_reports}")" -gt 1 ]; then
+      many_reports=yes
+    elif [ -n "${local_reports}" ]; then
+      temp_report="$(mktemp -d)/report"
+      git worktree add -q "${temp_report}" "${local_reports}" > /dev/null 2>&1 && report_dir="${temp_report}"
+    fi
+  fi
+  report_branch=""
+  if [ -n "${report_dir}" ]; then
+    report_branch="$(git -C "${report_dir}" symbolic-ref -q --short HEAD 2> /dev/null || true)"
+  fi
+  if [ "${many_reports}" = "yes" ]; then
+    patch_note="Có nhiều nhánh báo cáo (claude/RUN-...-report) trên máy này, không biết ghi vào nhánh nào; đặt SAVE_BRANCH_REPORT_DIR. Chỉ có bundle."
+  elif [[ ! "${report_branch}" =~ ^claude/(RUN-[0-9]{8}-[0-9]{2})-report$ ]]; then
+    patch_note="Không có nhánh báo cáo (claude/RUN-...-report) trên máy này, nên không ghi patch; chỉ có bundle."
+  elif [ "${branch}" = "${report_branch}" ]; then
+    patch_note="Nhánh chưa đẩy là chính nhánh báo cáo: không ghi patch lên nó; chỉ có bundle."
+  else
+    run_id="${BASH_REMATCH[1]}"
+    base="$(git merge-base origin/main "${branch}")"
+    work_dir="$(mktemp -d)"
+    mode="$(make_patch "${work_dir}")"
+    max_bytes="${SAVE_BRANCH_PATCH_MAX_BYTES:-1048576}"
+    script_dir="$(cd "$(dirname "$0")" && pwd)"
+    if [ "${mode}" = "noworktree" ]; then
+      patch_note="Không mở được worktree tạm để kiểm patch; chỉ có bundle."
+    elif [ -z "${mode}" ]; then
+      patch_note="Không dựng được patch áp lại đúng nhánh (git format-patch hay git diff, kiểm trong worktree tạm); chỉ có bundle."
+    else
+      rel="docs/runs/patches/${run_id}/${branch//\//__}.${mode}"
+      patch_size="$(wc -c < "${work_dir}/patch" | tr -d ' ')"
+      mkdir -p "${work_dir}/scan" && mv "${work_dir}/patch" "${work_dir}/scan/patch"
+      if [ "${patch_size}" -gt "${max_bytes}" ]; then
+        patch_note="Patch ${patch_size} byte, lớn hơn giới hạn ${max_bytes} byte (SAVE_BRANCH_PATCH_MAX_BYTES), nên không ghi lên nhánh báo cáo; chỉ có bundle."
+      elif ! python3 -I -c 'import sys; open(sys.argv[1], encoding="utf-8").read()' "${work_dir}/scan/patch" 2> /dev/null; then
+        patch_note="Patch có byte không phải UTF-8 nên scripts/secret-scan.py không đọc được; không ghi lên nhánh báo cáo, chỉ có bundle. Người xem lại nhánh trước khi đẩy."
+      elif ! SECRET_SCAN_ROOT="${work_dir}/scan" python3 -I "${script_dir}/secret-scan.py" > /dev/null 2>&1; then
+        patch_note="scripts/secret-scan.py thấy chuỗi giống bí mật trong patch, nên không ghi lên nhánh báo cáo; chỉ có bundle. Người xem lại nhánh trước khi đẩy."
+      elif git -C "${report_dir}" fetch -q origin "${report_branch}" > /dev/null 2>&1 \
+          && ! git -C "${report_dir}" merge-base --is-ancestor FETCH_HEAD HEAD > /dev/null 2>&1; then
+        patch_note="Nhánh báo cáo trên máy này và trên GitHub đã khác nhau (có commit không có ở bên kia): không ghi patch, để phiên gộp nhánh báo cáo trước; chỉ có bundle."
+      else
+        target="${report_dir}/${rel}"
+        committed=yes
+        was_tracked=no
+        git -C "${report_dir}" ls-files --error-unmatch -- "${rel}" > /dev/null 2>&1 && was_tracked=yes
+        if ! cmp -s "${work_dir}/scan/patch" "${target}" 2> /dev/null || ! git -C "${report_dir}" ls-files --error-unmatch -- "${rel}" > /dev/null 2>&1; then
+          mkdir -p "$(dirname "${target}")"
+          cp "${work_dir}/scan/patch" "${target}"
+          git -C "${report_dir}" add -- "${rel}"
+          if ! git -C "${report_dir}" commit -q -m "${run_id}: patch của nhánh chưa đẩy ${branch} (${short_sha})" -- "${rel}" > /dev/null 2>&1; then
+            git -C "${report_dir}" reset -q -- "${rel}" > /dev/null 2>&1
+            if [ "${was_tracked}" = "yes" ]; then
+              git -C "${report_dir}" checkout -q -- "${rel}" > /dev/null 2>&1
+            else
+              rm -f "${target}"
+            fi
+            committed=no
+          fi
+        fi
+        if [ "${committed}" = "no" ]; then
+          patch_note="Không commit được patch trên nhánh báo cáo (${report_dir}); chỉ có bundle."
+        elif ! push_err="$(git -C "${report_dir}" push -q origin "HEAD:${report_branch}" 2>&1)"; then
+          echo "${push_err}" >&2
+          patch_note="Đã commit patch \`${rel}\` trên nhánh báo cáo, nhưng nhánh báo cáo cũng không đẩy được: patch chỉ có trên máy này, như bundle."
+        else
+          if [ "${mode}" = mbox ]; then
+            apply_cmd="git show origin/${report_branch}:${rel} | git am"
+          else
+            apply_cmd="git show origin/${report_branch}:${rel} | git apply --index && git commit -m \"${branch}: ${count} commit gộp từ patch\""
+          fi
+          patch_lines="- Patch trên nhánh báo cáo, để người xem rồi tự quyết áp hay không: \`${rel}\` (${patch_size} byte, gốc \`$(git rev-parse --short=7 "${base}")\`). Lấy lại không cần máy cloud:
+
+\`\`\`
+git fetch origin ${report_branch} main
+git switch -c ${branch} $(git rev-parse --short=12 "${base}")
+${apply_cmd}
+git push -u origin ${branch}
+\`\`\`
+
+  Sau khi PR báo cáo đã merge, thay \`origin/${report_branch}\` bằng \`origin/main\` trong lệnh \`git show\`."
+        fi
+      fi
+    fi
+  fi
+fi
 cat << EOF
 
 ### Nhánh chưa đẩy
@@ -80,4 +250,9 @@ git fetch ${bundle} ${branch}:${branch}
 git push -u origin ${branch}
 \`\`\`
 EOF
+if [ -n "${patch_lines}" ]; then
+  printf '%s\n' "${patch_lines}"
+else
+  echo "- ${patch_note}"
+fi
 exit 3
```

**`scripts/run-watchdog.py`** (mới, viết theo khuôn `scripts/resume-check.py`: cùng cách đọc nhánh, danh sách chạy, giờ commit; không import file đó để đổi một bên không làm hỏng bên kia). Toàn văn:

```python
#!/usr/bin/env python3
"""Tells people when an auto-run session has gone quiet, so someone can continue it (camera-pose L-084, T-068).

A session opened with `claude --cloud` is not continued by resume-run.yml (L-008): when it stops in the middle
(an API error, the quota, the process restarting), the run file stays "running" and nobody knows until they look.
RUN-20261026-03 waited 2 hours 30 minutes that way. .github/workflows/run-watchdog.yml runs this script every
30 minutes; it only opens, updates and closes issues labelled session-stalled, and never starts a session.

Usage: scripts/run-watchdog.py
A run is quiet when its run file on its report branch is "running" and none of its branches (the report branch,
claude/T-NNN-... and claude/fix-T-NNN-... of its tickets, as in scripts/resume-check.py) got a commit for
WATCHDOG_STALL_MINUTES (60). While a workflow run of the repository is queued or in progress and started after the
last commit on one of the session's branches (or on main, by push or by hand: device-session, staging), the
session may be waiting for it, so the issue waits until WATCHDOG_HARD_MINUTES (90). A quiet run gets one open
issue "session-stalled: RUN-..." whose body has the prompt to paste (docs/ops/staging-and-runs.md, 2.7); the body
is refreshed on later runs. The issue is closed when the run has a new commit or is no longer "running". A run file
left "running" on an old report branch keeps its issue open until a person sets it to "finished".

Exit 0 when the check ran, 1 when GitHub could not be read (any error but a 404 on a file, a branch or the
label): then no issue is changed. Prints what it did, and the issue body, on stdout.
Uses only `gh api` (REST); GITHUB_REPOSITORY or the origin remote gives the repository.
WATCHDOG_STALL_MINUTES, WATCHDOG_HARD_MINUTES and WATCHDOG_NOW (ISO time, tests) override the defaults.
Messages are in Vietnamese for the team; comments are in English.
"""

import json
import os
import re
import subprocess
import sys
import tempfile
from datetime import datetime, timedelta, timezone

STALL_MINUTES = int(os.environ.get("WATCHDOG_STALL_MINUTES", "60"))
HARD_MINUTES = int(os.environ.get("WATCHDOG_HARD_MINUTES", "90"))
LABEL = "session-stalled"
REPORT_BRANCH = re.compile(r"^claude/(RUN-\d{8}-\d{2})-report$")
TITLE = re.compile(r"^session-stalled: (RUN-\d{8}-\d{2})\b")


class NotFound(LookupError):
    """GitHub answered 404."""


def api(path, raw=False, method="GET", body=None):
    command = ["gh", "api", f"repos/{REPO}/{path}"]
    if method != "GET":
        command += ["-X", method]
    if raw:
        command += ["-H", "Accept: application/vnd.github.raw"]
    handle = None
    if body is not None:
        handle = tempfile.NamedTemporaryFile("w", suffix=".json", delete=False)
        json.dump(body, handle)
        handle.close()
        command += ["--input", handle.name]
    try:
        result = subprocess.run(command, capture_output=True, text=True)
    finally:
        if handle:
            os.unlink(handle.name)
    if result.returncode != 0:
        if "HTTP 404" in result.stderr:
            raise NotFound(f"{method} {path}")
        raise LookupError(f"{method} {path}: {result.stderr.strip()[:200]}")
    return result.stdout if raw else json.loads(result.stdout or "null")


def api_all(path):
    """Every item of a list endpoint, page by page (the path ends in "per_page=100")."""
    items = []
    for page in range(1, 51):
        batch = api(f"{path}&page={page}") or []
        items.extend(batch)
        if len(batch) < 100:
            break
    return items


def repository():
    if os.environ.get("GITHUB_REPOSITORY"):
        return os.environ["GITHUB_REPOSITORY"]
    url = subprocess.run(["git", "config", "--get", "remote.origin.url"], capture_output=True, text=True).stdout
    return re.sub(r"^(git@github\.com:|https://github\.com/)|\.git\s*$", "", url.strip())


def moment(text):
    """ISO time, or the started_at form of a run file: '2026-10-17 16:19 UTC'."""
    try:
        value = datetime.fromisoformat(re.sub(r"\s*(UTC|Z)$", "+00:00", (text or "").strip()))
    except ValueError:
        return None
    return value if value.tzinfo else value.replace(tzinfo=timezone.utc)


def front_matter(text):
    match = re.match(r"^---\n(.*?)\n---\n", text or "", re.S)
    fields = {}
    for line in (match.group(1).splitlines() if match else []):
        if ":" in line:
            key, value = line.split(":", 1)
            fields[key.strip()] = value.split("#", 1)[0].strip()
    return fields


def last_commit(branch):
    try:
        return moment(api(f"commits/{branch}")["commit"]["committer"]["date"])
    except NotFound:
        return None
    except (KeyError, TypeError):
        return None


def busy_workflows(since, own):
    """Names of workflow runs queued or in progress that the session may be waiting for: created after `since`
    (5 minutes of slack), on one of its branches `own` or on main by push or by hand; never this watchdog."""
    names = []
    for status in ("in_progress", "queued"):
        runs = (api(f"actions/runs?status={status}&per_page=100") or {}).get("workflow_runs", [])
        for run in runs:
            created = moment(run.get("created_at"))
            branch = run.get("head_branch")
            ours = branch in own or (branch == "main" and run.get("event") in ("push", "workflow_dispatch"))
            if created and created >= since - timedelta(minutes=5) and ours and run.get("name") != "run-watchdog":
                names.append(run.get("name") or "?")
    return sorted(set(names))


def body_for(run_id, run_file, latest, quiet, note):
    # The prompt of docs/ops/staging-and-runs.md, section 2.7, plus its "phiên tiếp tục" line ("Khi phiên bị ngắt").
    resume = (
        "Bạn đang chạy một phiên tự chạy của repo này, mở bằng claude --cloud, không qua routine hay start-run.yml. "
        "Đọc CLAUDE.md, docs/CLAUDE.md và docs/auto-run.md, rồi làm đúng quy trình trong docs/auto-run.md.\n\n"
        f"Danh sách chạy: {run_file}. Danh sách không có trạng thái approved thì không làm task nào, "
        "kết thúc phiên và báo lý do.\n\n"
        "Nhánh: không làm việc trên nhánh mặc định của phiên này. Tạo nhánh báo cáo và nhánh riêng cho từng task "
        "đúng như docs/auto-run.md. Làm nhánh báo cáo trong một worktree riêng "
        f"(git worktree add ../report claude/{run_id}-report), để không đổi nhánh dưới bản build của task.\n\n"
        "Ghi vào mục Kiểm tra đầu phiên: phiên mở bằng claude --cloud để dùng cloud session credits.\n\n"
        "Đây là phiên tiếp tục: nhánh báo cáo đã có, danh sách chạy đang running. "
        "Làm theo docs/auto-run.md, mục 4, Tiếp tục phiên sau khi hết hạn mức."
    )
    last = latest.strftime("%Y-%m-%d %H:%M UTC") if latest else "không rõ"
    lines = [
        f"Phiên `{run_id}` vẫn `running` trên nhánh `claude/{run_id}-report`, nhưng không có commit nào "
        f"trong {quiet} phút (commit gần nhất: {last}).",
        "",
        "Người làm: xem phiên còn chạy không (claude.ai/code hoặc terminal đã mở `claude --cloud`). "
        "Phiên mở bằng routine (`start-run.yml`) thì `resume-run.yml` tự tiếp tục sau 90 phút không có commit: "
        "không mở thêm phiên tay, kẻo hai phiên cùng chạy. Phiên `claude --cloud` đã dừng thì mở lại bằng "
        "`claude --cloud` và dán đoạn dưới (docs/ops/staging-and-runs.md, mục 2.7, Khi phiên bị ngắt); "
        "phiên mới đẩy nhánh báo cáo ngay ở bước đầu (giờ tiếp tục), nên `resume-run.yml` không kích hoạt thêm.",
        "",
        "```text",
        resume,
        "```",
        "",
        "Issue này tự đóng khi phiên có commit mới hoặc danh sách chạy không còn `running` "
        "(scripts/run-watchdog.py, T-068).",
    ]
    if note:
        lines += ["", note]
    return "\n".join(lines)


def ensure_label():
    try:
        api(f"labels/{LABEL}")
    except NotFound:
        try:
            api("labels", method="POST", body={"name": LABEL, "color": "d93f0b",
                                               "description": "Phiên tự chạy không có commit mới (T-068)"})
        except LookupError:
            pass  # created meanwhile, or no right to create: the issue is still opened with the name


def main():
    now = moment(os.environ["WATCHDOG_NOW"]) if os.environ.get("WATCHDOG_NOW") else datetime.now(timezone.utc)
    branches = [b["name"] for b in api_all("branches?per_page=100")]
    open_issues = {}
    for issue in api(f"issues?state=open&labels={LABEL}&per_page=100") or []:
        match = TITLE.match(issue.get("title", ""))
        if match and "pull_request" not in issue:
            open_issues[match.group(1)] = issue["number"]
    running = set()
    # Everything is read first and written after, so a read that fails changes no issue.
    writes = []
    for branch in sorted(branches):
        match = REPORT_BRANCH.match(branch)
        if not match:
            continue
        run_id = match.group(1)
        run_file = f"docs/runs/{run_id}.md"
        try:
            text = api(f"contents/{run_file}?ref={branch}", raw=True)
        except NotFound:
            continue
        fields = front_matter(text)
        if fields.get("status") != "running":
            continue
        running.add(run_id)
        tickets = set(re.findall(r"^\|\s*\d+\s*\|\s*(T-\d+)\s*\|", text, re.M))
        own = [branch] + [b for b in branches
                          if (m := re.match(r"^claude/(?:fix-)?(T-\d+)-", b)) and m.group(1) in tickets]
        dates = [d for d in (last_commit(b) for b in own) if d]
        latest = max(dates) if dates else moment(fields.get("started_at"))
        quiet = int((now - latest).total_seconds() // 60) if latest else HARD_MINUTES
        number = open_issues.get(run_id)
        if quiet < STALL_MINUTES:
            print(f"{run_id}: đang chạy, commit gần nhất {quiet} phút trước.")
            if number:
                writes.append(lambda n=number, q=quiet, r=run_id: close(n, f"Phiên `{r}` có commit lại ({q} phút trước). Đóng."))
            continue
        busy = busy_workflows(latest, set(own)) if latest else []
        if busy and quiet < HARD_MINUTES:
            print(f"{run_id}: {quiet} phút không có commit, nhưng đang có workflow chạy ({', '.join(busy)}): "
                  f"có thể phiên đang chờ; báo khi tới {HARD_MINUTES} phút.")
            continue
        note = ""
        started = moment(fields.get("started_at"))
        if started and now - started > timedelta(hours=24):
            note = "Phiên bắt đầu hơn 24 giờ trước: `resume-run.yml` không tự tiếp tục; người quyết định."
        body = body_for(run_id, run_file, latest, quiet, note)
        writes.append(lambda n=number, b=body, q=quiet, r=run_id: report(n, b, q, r))
    for run_id, number in sorted(open_issues.items()):
        if run_id not in running:
            writes.append(lambda n=number, r=run_id: close(n, f"Danh sách chạy `{r}` không còn `running`. Đóng."))
    try:
        for write in writes:
            write()
    except LookupError as error:
        print(f"Ghi issue lỗi ({error}). Lần chạy sau thử lại.", file=sys.stderr)
        return 1
    return 0


def report(number, body, quiet, run_id):
    if number:
        api(f"issues/{number}", method="PATCH", body={"body": body})
        print(f"{run_id}: {quiet} phút không có commit. Cập nhật issue #{number}.")
    else:
        ensure_label()
        api("issues", method="POST", body={"title": f"session-stalled: {run_id} không có commit {quiet} phút",
                                           "body": body, "labels": [LABEL]})
        print(f"{run_id}: {quiet} phút không có commit. Mở issue {LABEL}.")
    print(body)


def close(number, message):
    # Closed first: a close that fails leaves no comment behind, so a retry every 30 minutes adds none.
    api(f"issues/{number}", method="PATCH", body={"state": "closed"})
    api(f"issues/{number}/comments", method="POST", body={"body": message})
    print(f"Đóng issue #{number}: {message}")


if __name__ == "__main__":
    REPO = repository()
    try:
        sys.exit(main())
    except (LookupError, json.JSONDecodeError) as error:
        print(f"Không đọc được GitHub ({error}). Không đổi issue nào.", file=sys.stderr)
        sys.exit(1)
```

**`.github/workflows/run-watchdog.yml`** (mới). Chạy ở phút 7 và 37 (lệch `resume-run` ở phút 23, `env-canary` ở phút 0), chỉ trên nhánh chính, `persist-credentials: false`, không bí mật. Toàn văn:

```yaml
# Tells people when an auto-run session has gone quiet (T-068, camera-pose L-084). Every 30 minutes,
# scripts/run-watchdog.py looks for a run file that is still "running" on its report branch but whose branches have
# had no commit for 60 minutes (90 while a workflow run it may be waiting for is still going), and opens or updates
# one issue labelled session-stalled with the prompt to paste; it closes the issue when the session commits again or
# the run is no longer "running". A session opened with `claude --cloud` is not continued by resume-run.yml (L-008),
# so this issue is how a person learns it must be reopened.
# Only reads the repository and writes session-stalled issues: never starts a session or a routine, never touches
# main-broken or any other issue. Needs no secret. About 1 Actions minute a run, 48 a day (L-046 of camera-pose:
# the Actions quota once ran out).
# Changing this file is a high-risk plan; cross-review required.
name: run-watchdog

on:
  schedule:
    - cron: "7,37 * * * *"
  workflow_dispatch:

concurrency:
  group: run-watchdog
  cancel-in-progress: false

defaults:
  run:
    shell: bash

permissions:
  contents: read
  issues: write
  actions: read

jobs:
  watch:
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    timeout-minutes: 5
    steps:
      - uses: actions/checkout@v5
        with:
          persist-credentials: false

      - name: Tìm phiên không có commit mới
        env:
          GH_TOKEN: ${{ github.token }}
        run: python3 -I scripts/run-watchdog.py | tee -a "${GITHUB_STEP_SUMMARY}"
```

**`scripts/tests/fake-gh`** (công cụ test dùng chung): thêm `"errors"` trong fixture (đường dẫn → thông báo lỗi, ví dụ `HTTP 500: Server Error`, thoát 1) và in `HTTP 404: Not Found` khi không có tệp `contents/...`, như GitHub. Các test cũ không đọc thông báo lỗi đó (đã chạy cả `run.sh`).

```diff
diff --git a/scripts/tests/fake-gh b/scripts/tests/fake-gh
index 0671469..be2e5f5 100755
--- a/scripts/tests/fake-gh
+++ b/scripts/tests/fake-gh
@@ -30,6 +30,9 @@ if method != "GET":
     if f"{method} {route}" in fx.get("fail_writes", []):
         sys.stderr.write("HTTP 422\n"); sys.exit(1)
     sys.stdout.write("{}"); sys.exit(0)
+# "errors" maps a route to the message of a failed read (for example "HTTP 500: Server Error"): exit 1 with it.
+if route in fx.get("errors", {}):
+    sys.stderr.write(fx["errors"][route] + "\n"); sys.exit(1)
 if route in fx.get("routes", {}):
     answer = fx["routes"][route]
     # {"__sequence__": [a, b, ...]} answers a, then b, ... one per query, then keeps the last (a run that finishes).
@@ -43,7 +46,7 @@ if route in fx.get("routes", {}):
     out = subprocess.run(["jq", "-r", jq], input=json.dumps(answer), capture_output=True, text=True)
     sys.stdout.write(out.stdout); sys.exit(out.returncode)
 if route.startswith("contents/"):
-    sys.exit(1)
+    sys.stderr.write("HTTP 404: Not Found\n"); sys.exit(1)
 # A path the fixture does not serve answers like GitHub: 404 and exit 1 (not a Python error).
 if not re.match(r"repos/[^/]+/[^/]+/commits/[0-9a-f]+(/check-runs)?($|\?)", path):
     sys.stderr.write("HTTP 404: Not Found\n"); sys.exit(1)
```

**`scripts/tests/run.sh`**:

- Khối `save-branch`: thêm khối ở mục 4 ngay trước dòng `export GIT_CONFIG_COUNT="${sb_git_config[0]}" ...` cuối khối (khối cần kho git thật, L-082 của mẫu).
- Thêm khối `run-watchdog` ở mục 4 ngay sau test cuối của `resume-check` (`resume-check: finds a report branch on page 2 of the branches`).

**`docs/auto-run.md`** (file của mẫu; câu mới dẫn số bài học của app dạng "camera-pose L-0NN", theo đầu `docs/CLAUDE.md`). Chỉ sửa hai chỗ:

- Bước 7, "Khi không đẩy được", ý 2: thay câu "Bundle nằm ở `$HOME/rescue/`, ngoài worktree." bằng: "Bundle nằm ở `$HOME/rescue/`, ngoài worktree, và mất khi máy cloud bị thu hồi. Riêng khi chính GitHub từ chối vì token không có quyền `workflow` (camera-pose L-086), `scripts/save-branch.sh` (không có `--bundle-only`) còn ghi các commit thành tệp `docs/runs/patches/<RUN>/<nhánh>.mbox` (hay `.diff`) trên nhánh báo cáo và đẩy nhánh báo cáo, để người xem rồi tự quyết áp hay không; đây là ngoại lệ duy nhất của ý 1, vì phiên không đẩy nhánh task dưới dạng khác mà giao nội dung cho người (T-068). Script không ghi patch khi chạy `--bundle-only`, với lần từ chối khác, hay khi patch lớn hơn 1 MB, không phải UTF-8 hay có chuỗi giống bí mật. Phiên không chạy lại script không có `--bundle-only` để có patch."
- Mục 4, "Tiếp tục phiên sau khi hết hạn mức": sau ý 3 của danh sách đầu (trước "Phiên tiếp tục làm như sau:") thêm đoạn: "Phiên mở bằng `claude --cloud` không được `resume-run.yml` tiếp tục (camera-pose L-008). Mỗi 30 phút, `run-watchdog.yml` chạy `scripts/run-watchdog.py`: danh sách `running` mà các nhánh của phiên không có commit 60 phút (90 phút khi đang có lần chạy workflow của phiên, vì phiên có thể đang chờ CI, `device-session` hay `staging`) thì mở issue `session-stalled` có sẵn prompt tiếp tục; có commit lại hay danh sách xong thì tự đóng (camera-pose L-084, T-068). Vì vậy phiên chính ghi một dòng vào Dòng thời gian và đẩy nhánh báo cáo khi giao bước 3 đến 5 cho phiên con và khi nhận kết quả, và không để quá 50 phút không có commit nào trên các nhánh của phiên mà không có workflow đang chạy: trong lúc chờ phiên con (một phiên con từng chạy 40 phút, T-046 ở RUN-20261024-02), cứ 50 phút đẩy một dòng Dòng thời gian. Phiên không đóng, sửa hay bình luận issue `session-stalled`; người mở lại phiên thì issue tự đóng ở lần chạy sau. Phiên mở bằng routine thì `resume-run.yml` tự tiếp tục sau 90 phút: người không mở thêm phiên tay."

**`docs/templates/run.md`**, mục "Nhánh chưa đẩy": thay chỗ giữ chỗ bằng "<Không có. Hoặc chép đoạn "Nhánh chưa đẩy" mà `scripts/save-branch.sh` in ra: nhánh, mã commit, đường dẫn bundle, lệnh lấy lại từ bundle, và dòng "Patch trên nhánh báo cáo" kèm lệnh lấy lại (khi GitHub từ chối vì quyền `workflow`), hay dòng ghi chú vì sao không có patch (auto-run.md, bước 7).>"

**`docs/ops/staging-and-runs.md`**, mục 2.7, đoạn "Khi phiên bị ngắt", ý 1: thêm vào đầu "Issue `session-stalled` (workflow `run-watchdog.yml`, mỗi 30 phút) báo khi phiên không có commit 60 phút; thân issue có sẵn prompt dưới đây kèm dòng tiếp tục (T-068)." Ý 1 cũng thêm: "Đoạn Nhánh chưa đẩy của báo cáo có dòng Patch trên nhánh báo cáo thì người xem patch rồi tự quyết lấy lại nhánh bằng lệnh in ở đó."

**`CLAUDE.md`** (gốc repo, mục Lệnh):

- Dòng `save-branch.sh` (hiện ở dòng 46, "Đẩy nhánh task, thử lại khi lỗi mạng, giữ thành bundle khi không đẩy được"): thêm "; GitHub từ chối vì quyền `workflow` thì còn ghi patch lên nhánh báo cáo cho người xem (T-068)".
- Thêm dòng ngay sau: "- Xem phiên tự chạy nào đứng im (danh sách `running`, không có commit 60 phút): `scripts/run-watchdog.py`; `run-watchdog.yml` chạy nó mỗi 30 phút và mở issue `session-stalled` (T-068)."

**`docs/template-sync.txt`** (file của app): thêm một dòng chú thích "# T-068: scripts/save-branch.sh (patch trên nhánh báo cáo), scripts/tests/fake-gh (errors, 404) và khối test của chúng trong scripts/tests/run.sh là phần của app chưa có ở mẫu: khi gộp xung đột, giữ phần này (đã đề xuất from-app).". `scripts/run-watchdog.py` và `run-watchdog.yml` không có ở mẫu nên lần đồng bộ không đè.

**Cần quyền `workflow` (L-086):** commit tạo `.github/workflows/run-watchdog.yml` chỉ đẩy được bằng token có quyền `workflow`. Danh sách chạy chỉ xếp T-068 vào phiên mở bằng routine (`start-run.yml`) hay phiên local, hoặc phiên `claude --cloud` sau khi Tuấn cấp lại quyền và lần thử đẩy đầu phiên qua (đề xuất 4c, T-066). Không đẩy được thì phiên làm theo "Khi không đẩy được" (phần 3b chưa có trên nhánh chính nên chỉ có bundle).

**Sau khi merge:** phiên chạy tay một lần `gh api -X POST "repos/{owner}/{repo}/actions/workflows/run-watchdog.yml/dispatches" -f ref=main` (cách gọi của phiên, L-063 của mẫu), chờ bằng `scripts/wait-run.sh --workflow run-watchdog.yml --since <giờ>`. Lúc đó danh sách của chính phiên đang `running` và vừa có commit (phiên đẩy nhánh báo cáo ngay trước lần chạy này), nên kết quả đúng là "đang chạy, commit gần nhất N phút trước" và không mở issue. Đỏ thì xem log: lỗi đọc GitHub (quyền) thì tắt workflow (`gh workflow disable run-watchdog.yml`), ghi vào báo cáo, không revert; lỗi của script thì revert theo PR thường (`claude/T-068-revert`, `git revert -m 1 <merge>`, CI, `merge-if-green.sh`).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/save-branch.sh` | Giao patch lên nhánh báo cáo khi GitHub từ chối vì quyền `workflow` (mục 3.1, `diff`) |
| Tạo | `scripts/run-watchdog.py` | Script watchdog (mục 3.1, toàn văn) |
| Tạo | `.github/workflows/run-watchdog.yml` | Workflow mỗi 30 phút (mục 3.1, toàn văn) |
| Sửa | `scripts/tests/fake-gh` | `"errors"` trong fixture; 404 có thông báo (mục 3.1, `diff`) |
| Sửa | `scripts/tests/run.sh` | Khối test `save-branch patch` (64 dòng `expect` mới) và `run-watchdog` (35), mục 4 |
| Sửa | `docs/auto-run.md` | Bước 7 "Khi không đẩy được" ý 2; mục 4 "Tiếp tục phiên sau khi hết hạn mức" đoạn watchdog và nhịp commit (giao, nhận bước 3 đến 5; tối đa 50 phút) |
| Sửa | `docs/templates/run.md` | Mục "Nhánh chưa đẩy": dòng patch |
| Sửa | `docs/ops/staging-and-runs.md` | Mục 2.7, "Khi phiên bị ngắt": issue `session-stalled`, dòng patch |
| Sửa | `CLAUDE.md` | Mục Lệnh: dòng `save-branch.sh`, một dòng `run-watchdog.py` |
| Sửa | `docs/template-sync.txt` | Một dòng chú thích phần của app phải giữ khi đồng bộ |
| Sửa | `docs/features/quality-gate/tickets/T-068-patch-va-canh-bao-phien-dung.md` | Trạng thái |

Nơi đọc file bị đổi (L-020 của mẫu): `scripts/save-branch.sh` được gọi ở `docs/auto-run.md` (bước 3, 7, Giao bước 3 đến 5, mục 4 bước 5) và test của nó; tham số và mã thoát không đổi, chỉ thêm dòng in ra ở cuối đoạn "Nhánh chưa đẩy", nên không chỗ gọi nào phải sửa. `scripts/tests/fake-gh` được mọi khối test dùng `gh` đọc; phần thêm chỉ chạy khi fixture có `"errors"`, và câu 404 chỉ ra stderr (toàn bộ `run.sh` qua). `docs/runs/patches/` là thư mục mới: `scripts/status.py` chỉ đọc `docs/runs/RUN-*.md` (không đệ quy), `scripts/run-check.py` đọc một tệp danh sách chạy, `pr-check.yml` xếp `docs/**` vào đường chỉ tài liệu; `scripts/secret-scan.py` quét mọi tệp git theo dõi, gồm cả patch, và patch đã qua quét (và kiểm UTF-8) trước khi ghi. Không file nào đọc `run-watchdog.*`.

Các file nhắc tới tài liệu mà plan sửa (L-020 của mẫu, `scripts/plan-check.py`):

- `docs/auto-run.md` được nhắc trong chú thích hay thông báo của `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`: các file đó dẫn tên mục (mục 2, mục 4, bước 8, bước 9), không đọc nội dung; plan không đổi tên mục nào và chỉ thêm câu vào bước 7 và mục 4.
- `docs/ops/staging-and-runs.md` được nhắc trong `.github/workflows/device-session.yml`, `.github/workflows/env-canary.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/staging.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/env-canary.sh`, `scripts/setup-check.sh`: chỉ là đường dẫn trong chú thích hay thông báo; plan chỉ thêm câu vào "Khi phiên bị ngắt".
- `scripts/tests/run.sh` được `scripts/check.sh` và `.github/workflows/pr-check.yml` chạy; plan chỉ thêm test.
- `CLAUDE.md` được nhắc trong `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt` và `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt` (chú thích về cách kiểm màn hình); plan chỉ sửa mục Lệnh.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm (app). Workflow mới chỉ có `contents: read`, `issues: write`, `actions: read` của `GITHUB_TOKEN`; không bí mật mới.
- **Thư viện và SDK:** Không thêm (Python chuẩn, `gh`, `git` có sẵn trên runner).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** thông báo GitHub trả khi thiếu quyền `workflow` có thể đổi chữ (ví dụ với GitHub App thay vì OAuth App); khi đó mẫu không khớp và script coi là "lần từ chối khác".
- **Điều kiện chuyển:** không chuyển trong phiên: kết quả là chỉ có bundle như hôm nay (không xấu hơn), kèm dòng "lần từ chối này khác" và nguyên văn thông báo trong báo cáo.
- **Cách làm thay:** người thêm câu mới vào mẫu `workflow_scope_pattern` (và dòng bảng tra lỗi của T-067) bằng một plan nhỏ.

## 4. Kiểm thử

Thứ tự commit (L-025 của mẫu): bước 3 commit hai khối test vào `scripts/tests/run.sh` và phần `scripts/tests/fake-gh` (công cụ test; test cũ vẫn qua với nó); trên code hiện tại `scripts/run-watchdog.py` chưa có nên `python3` trả mã 2 và `save-branch.sh` chưa ghi patch, test thất bại đúng lý do (mục 4.1). Bước 4 commit `save-branch.sh`, `run-watchdog.py`, workflow và tài liệu.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| 3b, có patch khi GitHub từ chối vì quyền `workflow`, commit riêng | Test script | `save-branch patch: a workflow-scope refusal still exits 3`, `... the mbox is on the pushed report branch`, `... the report branch got one commit`, `... the rescue section names the patch for a person to review`, `... the bundle is still made` |
| 3b, người lấy lại được (nhị phân, workflow) | Test script | `save-branch patch: the printed commands rebuild the same tree`, `... the rebuilt branch keeps both commit messages` (nhánh có `.github/workflows/x.yml` và `b.bin`; chạy đúng các lệnh in ra, trừ `git push`, trong một bản sao mới), `... the check worktree is removed` |
| 3b, chạy lại không thêm commit | Test script | `save-branch patch: the same patch again exits 3`, `... adds no second commit`, `... still prints the commands` |
| 3b, chỉ đúng trường hợp | Test script | `save-branch patch: another refusal exits 3`, `... another refusal writes no patch`, `... another refusal says why`, `... --bundle-only exits 3`, `... --bundle-only never writes the patch`, `... --bundle-only leaves the report worktree clean`, `... --bundle-only says it does not push in another form`, `... --bundle-only never pushes the task branch` |
| 3b, không commit phần khác; commit lỗi thì dọn | Test script | `save-branch patch: other staged changes of the report worktree`, `... stay staged, not committed`, `... a commit that fails exits 3`, `... a failed commit leaves no file and nothing staged`, `... a failed commit says so` (hook `pre-commit` tạm trả 1), `... a newer patch whose commit fails exits 3`, `... leaves the report worktree clean`, `... keeps the patch already committed` |
| 3b, giới hạn cỡ | Test script | `save-branch patch: a patch over the limit is left out`, `... over the limit, no patch on the report branch`, `... over the limit says so` |
| 3b, bí mật; không phải UTF-8 | Test script | `save-branch patch: a patch with a secret exits 3`, `... a secret is never on the report branch`, `... never left in the report worktree`, `... says secret-scan`, `... the secret is not printed`; `... a patch that is not UTF-8 exits 3`, `... is never written`, `... says so` (tệp latin-1 có cùng token: `secret-scan.py` bỏ qua tệp không đọc được, đột biến M10) |
| 3b, nhánh có commit merge | Test script | `save-branch patch: a branch with a merge of main exits 3`, `... gets one combined diff`, `... the diff commands rebuild the same tree` (merge bỏ tệp mới của nhánh chính: `git am` các commit thường cho sai cây) |
| 3b, patch không dựng lại được | Test script | `save-branch patch: a branch whose patch cannot rebuild it exits 3`, `... writes no patch and says so` (nhánh chỉ có commit rỗng) |
| 3b, nhánh báo cáo cũng bị từ chối; chính nhánh báo cáo | Test script | `save-branch patch: report branch refused too exits 3`, `... says the patch is only local`, `... prints no apply commands`; `... the report branch itself is never patched onto itself`, `... and says so` |
| 3b, chỗ ghi | Test script | `save-branch patch: a local report branch without a worktree still gets the patch`, `... pushed through a temporary worktree`, `... which is removed`, `... no report branch at all still bundles`, `... no report branch at all says so`, `... SAVE_BRANCH_REPORT_DIR picks the worktree`, `... with SAVE_BRANCH_REPORT_DIR the patch is pushed`, `... two report worktrees exit 3`, `... two local report branches without worktrees exit 3`, `... pick neither` (hai lần), `... and leave no worktree`, `... a report branch that diverged from GitHub exits 3`, `... writes no patch`, `... says the branches differ`, `... the commands say how to read the patch after the report PR merges`; test cũ của khối `save-branch` giữ nguyên (đối chứng) |
| 4b, báo khi đứng im từ 60 phút | Test script (`fake-gh`) | `run-watchdog: a running session quiet for 70 minutes exits 0`, `... opens one session-stalled issue`, `... the issue body has the resume line`, `... has the whole prompt of staging-and-runs 2.7`, `... the prompt names this run's report branch`, `... warns that a routine session is resumed by resume-run`, `... counts the latest commit of any branch of the run`, `... 59 minutes is not quiet yet`, `... 60 minutes is quiet` |
| 4b, không báo nhầm, chỉ tính workflow của phiên | Test script | `run-watchdog: CI on a branch of the session means it may be waiting`, `... and says which workflow`, `... a queued run on main started by hand counts too`, `... CI of a branch outside the session does not count`, `... a scheduled run on main does not count`, `... its own run does not count`, `... created 4 minutes before the last commit counts (5 minutes of slack)`, `... created 6 minutes before the last commit is not a wait`, `... past 90 minutes a running workflow no longer holds the issue back` |
| 4b, một issue, tự đóng | Test script | `run-watchdog: an open issue is updated, not opened again`, `... a new commit closes the issue with a comment`, `... a finished run closes its issue`, `... a finished run without an issue changes nothing` |
| 4b, không đụng việc khác; nhãn; 24 giờ; lỗi đọc | Test script | `run-watchdog: issues that are not its own are never touched`, `... a run older than 24 hours is still reported, with a note`, `... a server error on the run file exits 1`, `... and changes no issue`, `... a server error on the workflow runs exits 1`, `... and opens no issue`, `... a later run that cannot be read exits 1`, `... before anything is written for the first run`, `... a write that fails exits 1`, `... and says the write failed`, `... a close that fails adds no comment`, `... a missing label is created before the issue`, `... GitHub that cannot be read exits 1` |
| 4b, workflow chạy được trên runner với `GITHUB_TOKEN` | Kiểm tra thủ công (người: chạy một lần sau khi merge, mục 3.1) | `run-watchdog.yml` chạy tay trên nhánh chính xanh, in "đang chạy, commit gần nhất N phút trước" cho danh sách của chính phiên, không mở issue |
| 3b trong phiên thật | Kiểm tra thủ công (người: chỉ có khi GitHub từ chối vì quyền `workflow`) | Lần đầu báo cáo có dòng "Patch trên nhánh báo cáo", người xem patch rồi tự quyết chạy bốn lệnh in ra và mở PR |

Khối test `save-branch patch` (thêm trước dòng `export GIT_CONFIG_COUNT="${sb_git_config[0]}" ...` của khối `save-branch`):

```bash
# A push that GitHub refuses for lack of the workflow scope is also handed off as a patch on the report branch,
# pushed there for a person to review and apply (T-068, L-086). Never with --bundle-only, never for another refusal.
sb_report="$work/sb-report"
sb_report_branch=claude/RUN-20261027-09-report
sb_hook_mode none
git -C "$sb_clone" worktree add -q -b "$sb_report_branch" "$sb_report" main
git -C "$sb_report" push -q -u origin "$sb_report_branch"
git -C "$sb_clone" checkout -q -b claude/T-997-patch main
printf 'one\n' > "$sb_clone/a.txt"
git -C "$sb_clone" add a.txt && git -C "$sb_clone" commit -q -m "T-997: a"
mkdir -p "$sb_clone/.github/workflows"
printf 'name: x\n' > "$sb_clone/.github/workflows/x.yml"
printf '\x00\x01\x02\xff' > "$sb_clone/b.bin"
git -C "$sb_clone" add .github/workflows/x.yml b.bin && git -C "$sb_clone" commit -q -m "T-997: workflow and binary"
sb_patch_head="$(git -C "$sb_clone" rev-parse HEAD)"
sb_mbox_rel="docs/runs/patches/RUN-20261027-09/claude__T-997-patch.mbox"
sb_hook_scope() { # <task | all | task-and-deny-report>: GitHub refusing a workflow change without the workflow scope
  rm -f "$sb_calls" "$sb_calls.count"
  local task="echo 'remote: refusing to allow an OAuth App to create or update workflow .github/workflows/x.yml without workflow scope' >&2; exit 1"
  local report=':'
  case "$1" in
    all) report="$task" ;;
    task-and-deny-report) report="echo 'remote: denied' >&2; exit 1" ;;
  esac
  printf '#!/bin/sh\nwhile read old new ref; do case "$ref" in refs/heads/claude/T-*) echo x >> "%s"; %s;; refs/heads/claude/RUN-*) %s;; esac; done\nexit 0\n' \
    "$sb_calls" "$task" "$report" > "$sb_hook"
  chmod +x "$sb_hook"
}
sb_report_has() { git -C "$sb_origin" cat-file -e "refs/heads/$sb_report_branch:$1" 2> /dev/null && echo yes || echo no; }
sb_report_commits() { git -C "$sb_origin" rev-list --count "main..$sb_report_branch"; }
sb_report_reset() { git -C "$sb_report" reset -q --hard main && git -C "$sb_report" push -q -f origin "$sb_report_branch"; }
sb_apply_check() { # <file of printed output> <branch>: runs the printed commands (but git push) in a fresh clone; prints the tree
  rm -rf "$work/sb-apply"
  git clone -q "$sb_origin" "$work/sb-apply" 2> /dev/null
  git -C "$work/sb-apply" config user.email p@example.com
  git -C "$work/sb-apply" config user.name p
  local cmds
  cmds="$(awk '/^- Patch trên nhánh báo cáo/ { on = 1; next } on && /^```$/ { n++; if (n == 2) exit; next } on && n == 1 && !/^git push/' "$1")"
  (cd "$work/sb-apply" && eval "$cmds") > /dev/null 2>&1 || true
  git -C "$work/sb-apply" rev-parse -q --verify "$2^{tree}" || echo none
}
sb_hook_scope task
expect "save-branch patch: a workflow-scope refusal still exits 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: the mbox is on the pushed report branch" yes "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: the report branch got one commit" 1 "$(sb_report_commits)"
expect "save-branch patch: the rescue section names the patch for a person to review" yes \
  "$(grep -qF -- "- Patch trên nhánh báo cáo, để người xem rồi tự quyết áp hay không: \`$sb_mbox_rel\`" "$work/sb.out" && echo yes || echo no)"
expect "save-branch patch: the bundle is still made" ok \
  "$(git -C "$sb_clone" bundle verify "$work/rescue/claude__T-997-patch.bundle" > /dev/null 2>&1 && echo ok || echo bad)"
expect "save-branch patch: the printed commands rebuild the same tree" "$(git -C "$sb_clone" rev-parse "$sb_patch_head^{tree}")" \
  "$(sb_apply_check "$work/sb.out" claude/T-997-patch)"
expect "save-branch patch: the rebuilt branch keeps both commit messages" "T-997: workflow and binary|T-997: a" \
  "$(git -C "$work/sb-apply" log --format=%s main..claude/T-997-patch 2> /dev/null | paste -sd '|' -)"
expect "save-branch patch: the check worktree is removed" 2 "$(git -C "$sb_clone" worktree list | grep -c .)"
expect "save-branch patch: the same patch again exits 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: ... adds no second commit" 1 "$(sb_report_commits)"
expect "save-branch patch: ... still prints the commands" 1 "$(grep -c '| git am$' "$work/sb.out")"
sb_report_reset
sb_hook_mode reject
expect "save-branch patch: another refusal exits 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: another refusal writes no patch" no "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: another refusal says why" 1 "$(grep -c 'chỉ ghi khi GitHub từ chối vì quyền workflow' "$work/sb.out")"
sb_hook_scope task
rm -f "$sb_calls"
expect "save-branch patch: --bundle-only exits 3" 3 "$(sb_run --bundle-only claude/T-997-patch)"
expect "save-branch patch: --bundle-only never writes the patch" no "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: --bundle-only leaves the report worktree clean" "" "$(git -C "$sb_report" status --short)"
expect "save-branch patch: --bundle-only says it does not push in another form" 1 "$(grep -c 'không đẩy lại dưới dạng khác' "$work/sb.out")"
expect "save-branch patch: --bundle-only never pushes the task branch" 0 "$(sb_calls_count)"
printf 'session draft\n' > "$sb_report/notes.md"
git -C "$sb_report" add notes.md
expect "save-branch patch: other staged changes of the report worktree" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: ... stay staged, not committed" "A  notes.md" "$(git -C "$sb_report" status --short notes.md)"
git -C "$sb_report" rm -q --cached notes.md && rm -f "$sb_report/notes.md"
sb_report_reset
printf '#!/bin/sh\nexit 1\n' > "$sb_clone/.git/hooks/pre-commit" && chmod +x "$sb_clone/.git/hooks/pre-commit"
expect "save-branch patch: a commit that fails exits 3" 3 "$(sb_run claude/T-997-patch)"
rm -f "$sb_clone/.git/hooks/pre-commit"
expect "save-branch patch: a failed commit leaves no file and nothing staged" "" "$(git -C "$sb_report" status --short --untracked-files=all)"
expect "save-branch patch: a failed commit says so" 1 "$(grep -c 'Không commit được patch' "$work/sb.out")"
expect "save-branch patch: a patch over the limit is left out" 3 "$(SAVE_BRANCH_PATCH_MAX_BYTES=100 sb_run claude/T-997-patch)"
expect "save-branch patch: over the limit, no patch on the report branch" no "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: over the limit says so" 1 "$(grep -c 'lớn hơn giới hạn 100 byte' "$work/sb.out")"
sb_token="$(printf '%s%s' "gh""p_" "abcdefghijklmnopqrstuvwxyz0123456789")"
git -C "$sb_clone" checkout -q -b claude/T-996-secret main
printf 'token=%s\n' "$sb_token" > "$sb_clone/leak.txt"
git -C "$sb_clone" add leak.txt && git -C "$sb_clone" commit -q -m "T-996: leak"
expect "save-branch patch: a patch with a secret exits 3" 3 "$(sb_run claude/T-996-secret)"
expect "save-branch patch: a secret is never on the report branch" no \
  "$(sb_report_has docs/runs/patches/RUN-20261027-09/claude__T-996-secret.mbox)"
expect "save-branch patch: a secret is never left in the report worktree" "" "$(git -C "$sb_report" status --short --untracked-files=all)"
expect "save-branch patch: a secret says secret-scan" 1 "$(grep -c 'secret-scan.py thấy chuỗi giống bí mật' "$work/sb.out")"
expect "save-branch patch: the secret is not printed" 0 "$(grep -c "$sb_token" "$work/sb.out")"
git -C "$sb_clone" checkout -q -b claude/T-995-latin main
printf 'caf\xe9 token=%s\n' "$sb_token" > "$sb_clone/latin.txt"
git -C "$sb_clone" add latin.txt && git -C "$sb_clone" commit -q -m "T-995: latin-1"
expect "save-branch patch: a patch that is not UTF-8 exits 3" 3 "$(sb_run claude/T-995-latin)"
expect "save-branch patch: a patch that is not UTF-8 is never written" "" "$(git -C "$sb_report" status --short --untracked-files=all)"
expect "save-branch patch: a patch that is not UTF-8 says so" 1 "$(grep -c 'không phải UTF-8' "$work/sb.out")"
git -C "$sb_clone" checkout -q -b claude/T-994-merge main
printf 'on branch\n' > "$sb_clone/c.txt"
git -C "$sb_clone" add c.txt && git -C "$sb_clone" commit -q -m "T-994: c"
git -C "$sb_clone" checkout -q main
printf 'main moved\n' > "$sb_clone/m.txt"
git -C "$sb_clone" add m.txt && git -C "$sb_clone" commit -q -m "main: m"
git -C "$sb_clone" checkout -q claude/T-994-merge
# The merge of main also drops main's new file: replaying only the non-merge commits (git am) would keep it.
git -C "$sb_clone" merge -q --no-commit main > /dev/null 2>&1
git -C "$sb_clone" rm -q -f m.txt
git -C "$sb_clone" commit -q -m "merge main, without m.txt"
git -C "$sb_clone" push -q origin main 2> /dev/null; git -C "$sb_clone" fetch -q origin
sb_hook_scope task
expect "save-branch patch: a branch with a merge of main exits 3" 3 "$(sb_run claude/T-994-merge)"
expect "save-branch patch: a branch with a merge gets one combined diff" yes \
  "$(sb_report_has docs/runs/patches/RUN-20261027-09/claude__T-994-merge.diff)"
expect "save-branch patch: the diff commands rebuild the same tree" "$(git -C "$sb_clone" rev-parse 'claude/T-994-merge^{tree}')" \
  "$(sb_apply_check "$work/sb.out" claude/T-994-merge)"
git -C "$sb_clone" checkout -q -b claude/T-993-empty main
git -C "$sb_clone" commit -q --allow-empty -m "T-993: nothing"
expect "save-branch patch: a branch whose patch cannot rebuild it exits 3" 3 "$(sb_run claude/T-993-empty)"
expect "save-branch patch: ... writes no patch and says so" 1 "$(grep -c 'Không dựng được patch' "$work/sb.out")"
git -C "$sb_clone" checkout -q claude/T-997-patch
sb_report_reset
sb_hook_scope task-and-deny-report
expect "save-branch patch: report branch refused too exits 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: report branch refused says the patch is only local" 1 "$(grep -c 'nhánh báo cáo cũng không đẩy được' "$work/sb.out")"
expect "save-branch patch: report branch refused prints no apply commands" 0 "$(grep -c '| git am' "$work/sb.out")"
git -C "$sb_report" reset -q --hard main
git -C "$sb_report" commit -q --allow-empty -m "report: timeline"
sb_hook_scope all
expect "save-branch patch: the report branch itself is never patched onto itself" 3 "$(sb_run "$sb_report_branch")"
expect "save-branch patch: ... and says so" 1 "$(grep -c 'chính nhánh báo cáo' "$work/sb.out")"
git -C "$sb_clone" worktree remove --force "$sb_report"
sb_hook_scope task
sb_report_reset_remote() { git -C "$sb_clone" push -q -f origin "main:$sb_report_branch"; }
git -C "$sb_clone" branch -q -f "$sb_report_branch" main && sb_report_reset_remote
expect "save-branch patch: a local report branch without a worktree still gets the patch" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: ... pushed through a temporary worktree" yes "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: ... which is removed" 1 "$(git -C "$sb_clone" worktree list | grep -c .)"
git -C "$sb_clone" branch -q -D "$sb_report_branch"
expect "save-branch patch: no report branch at all still bundles" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: no report branch at all says so" 1 "$(grep -c 'Không có nhánh báo cáo' "$work/sb.out")"
git -C "$sb_clone" worktree add -q -b "$sb_report_branch" "$sb_report" main && sb_report_reset_remote
expect "save-branch patch: SAVE_BRANCH_REPORT_DIR picks the worktree" 3 \
  "$(SAVE_BRANCH_REPORT_DIR="$sb_report" sb_run claude/T-997-patch)"
expect "save-branch patch: with SAVE_BRANCH_REPORT_DIR the patch is pushed" yes "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: the commands say how to read the patch after the report PR merges" 1 \
  "$(grep -c 'thay `origin/claude/RUN-20261027-09-report` bằng `origin/main`' "$work/sb.out")"
sb_tracked="$(git -C "$sb_report" rev-parse -q --verify "HEAD:$sb_mbox_rel" || echo none)"
printf 'two\n' > "$sb_clone/a.txt"
git -C "$sb_clone" checkout -q claude/T-997-patch && git -C "$sb_clone" commit -q -am "T-997: a again"
printf '#!/bin/sh\nexit 1\n' > "$sb_clone/.git/hooks/pre-commit" && chmod +x "$sb_clone/.git/hooks/pre-commit"
expect "save-branch patch: a newer patch whose commit fails exits 3" 3 "$(SAVE_BRANCH_REPORT_DIR="$sb_report" sb_run claude/T-997-patch)"
rm -f "$sb_clone/.git/hooks/pre-commit"
expect "save-branch patch: ... leaves the report worktree clean" "" "$(git -C "$sb_report" status --short --untracked-files=all)"
expect "save-branch patch: ... keeps the patch already committed" "$sb_tracked" "$(git -C "$sb_report" hash-object "$sb_mbox_rel" 2> /dev/null || echo gone)"
git -C "$sb_clone" push -q -f origin "main:$sb_report_branch"
git -C "$sb_clone" fetch -q origin
git -C "$sb_origin" update-ref "refs/heads/$sb_report_branch" "$(git -C "$sb_origin" commit-tree -p "refs/heads/$sb_report_branch" -m remote "refs/heads/$sb_report_branch^{tree}")"
git -C "$sb_report" reset -q --hard main
git -C "$sb_report" commit -q --allow-empty -m "local timeline"
expect "save-branch patch: a report branch that diverged from GitHub exits 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: ... writes no patch" "" "$(git -C "$sb_report" status --short --untracked-files=all)"
expect "save-branch patch: ... says the branches differ" 1 "$(grep -c 'đã khác nhau' "$work/sb.out")"
git -C "$sb_clone" worktree add -q -b claude/RUN-20261027-10-report "$work/sb-report2" main
expect "save-branch patch: two report worktrees exit 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: ... pick neither" 1 "$(grep -c 'Có nhiều nhánh báo cáo' "$work/sb.out")"
git -C "$sb_clone" worktree remove --force "$work/sb-report2"
git -C "$sb_clone" worktree remove --force "$sb_report"
expect "save-branch patch: two local report branches without worktrees exit 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: ... pick neither" 1 "$(grep -c 'Có nhiều nhánh báo cáo' "$work/sb.out")"
expect "save-branch patch: ... and leave no worktree" 1 "$(git -C "$sb_clone" worktree list | grep -c .)"
git -C "$sb_clone" branch -q -D claude/RUN-20261027-10-report
git -C "$sb_clone" checkout -q claude/T-999-thu
```

Khối test `run-watchdog` (thêm sau test cuối của `resume-check`):

```bash
# run-watchdog.py opens an issue when a running session has no commit for 60 minutes, so a person can
# continue a `claude --cloud` session that resume-run.yml does not continue (T-068).
wd_time() { python3 -c "import datetime as d; print((d.datetime(2026,10,27,12,0,tzinfo=d.timezone.utc)-d.timedelta(minutes=$1)).strftime('%Y-%m-%dT%H:%M:%SZ'))"; }
watchdog() { # <run status> <minutes since the last commit> [workflow runs: "min:branch:event:status:name,..." or -] [open issues JSON] [started_at] [errors JSON]
  local last in_progress="" queued="" code=0 spec min branch event status name entry errors="${6:-}"
  [ -n "$errors" ] || errors="{}"
  last="$(wd_time "$2")"
  if [ "${3:--}" != "-" ]; then
    IFS=',' read -r -a specs <<< "$3"
    for spec in "${specs[@]}"; do
      IFS=':' read -r min branch event status name <<< "$spec"
      entry="{\"name\": \"$name\", \"head_branch\": \"$branch\", \"event\": \"$event\", \"created_at\": \"$(wd_time "$min")\"}"
      if [ "$status" = queued ]; then queued="${queued:+$queued, }$entry"; else in_progress="${in_progress:+$in_progress, }$entry"; fi
    done
  fi
  rm -f "$work/wd.json.writes"
  cat > "$work/wd.json" << JSON
{"routes": {
  "branches?per_page=100&page=1": [{"name": "main"}, {"name": "claude/RUN-20261027-05-report"}, {"name": "claude/T-062-x"}, {"name": "claude/T-099-other"}],
  "contents/docs/runs/RUN-20261027-05.md?ref=claude/RUN-20261027-05-report": "---\nid: RUN-20261027-05\nstatus: $1\nstarted_at: ${5:-2026-10-27T09:00:00Z}\n---\n\n## 1. Danh sách chạy\n\n| 1 | T-062 | demo |\n",
  "commits/claude/RUN-20261027-05-report": {"commit": {"committer": {"date": "2026-10-27T09:05:00Z"}}},
  "commits/claude/T-062-x": {"commit": {"committer": {"date": "$last"}}},
  "commits/claude/T-099-other": {"commit": {"committer": {"date": "2026-10-27T11:59:00Z"}}},
  "issues?state=open&labels=session-stalled&per_page=100": ${4:-[]},
  "actions/runs?status=in_progress&per_page=100": {"workflow_runs": [$in_progress]},
  "actions/runs?status=queued&per_page=100": {"workflow_runs": [$queued]},
  "labels/session-stalled": {"name": "session-stalled"}
}, "errors": $errors}
JSON
  PATH="$work/bin:$PATH" FIXTURE="$work/wd.json" GITHUB_REPOSITORY=o/r WATCHDOG_NOW=2026-10-27T12:00:00Z \
    python3 -I scripts/run-watchdog.py > "$work/wd.out" 2>&1 || code=$?
  echo "$code"
}
wd_writes() { if [ -f "$work/wd.json.writes" ]; then paste -sd ';' - < "$work/wd.json.writes"; else echo none; fi; }
wd_open='[{"number": 41, "title": "session-stalled: RUN-20261027-05 không có commit 50 phút"}]'
expect "run-watchdog: a running session quiet for 70 minutes exits 0" 0 "$(watchdog running 70)"
expect "run-watchdog: ... opens one session-stalled issue" "POST issues" "$(wd_writes)"
expect "run-watchdog: the issue body has the resume line" 1 \
  "$(grep -c 'Đây là phiên tiếp tục: nhánh báo cáo đã có, danh sách chạy đang running.' "$work/wd.out")"
expect "run-watchdog: the issue body has the whole prompt of staging-and-runs 2.7" 1 \
  "$(grep -c '^Danh sách chạy: docs/runs/RUN-20261027-05.md. Danh sách không có trạng thái approved' "$work/wd.out")"
expect "run-watchdog: the prompt names this run's report branch" 1 "$(grep -c 'git worktree add ../report claude/RUN-20261027-05-report' "$work/wd.out")"
expect "run-watchdog: the body warns that a routine session is resumed by resume-run" 1 "$(grep -c 'không mở thêm phiên tay' "$work/wd.out")"
expect "run-watchdog: the quiet time counts the latest commit of any branch of the run" 1 \
  "$(grep -c 'không có commit nào trong 70 phút' "$work/wd.out")"
watchdog running 59 > /dev/null
expect "run-watchdog: 59 minutes is not quiet yet" none "$(wd_writes)"
watchdog running 60 > /dev/null
expect "run-watchdog: 60 minutes is quiet" "POST issues" "$(wd_writes)"
watchdog running 70 "20:claude/T-062-x:pull_request:in_progress:pr-check" > /dev/null
expect "run-watchdog: CI on a branch of the session means it may be waiting" none "$(wd_writes)"
expect "run-watchdog: ... and says which workflow" 1 "$(grep -c 'đang có workflow chạy (pr-check)' "$work/wd.out")"
watchdog running 70 "20:main:workflow_dispatch:queued:device-session" > /dev/null
expect "run-watchdog: a queued run on main started by hand counts too" none "$(wd_writes)"
watchdog running 70 "20:claude/T-099-other:pull_request:in_progress:pr-check" > /dev/null
expect "run-watchdog: CI of a branch outside the session does not count" "POST issues" "$(wd_writes)"
watchdog running 70 "20:main:schedule:in_progress:env-canary" > /dev/null
expect "run-watchdog: a scheduled run on main does not count" "POST issues" "$(wd_writes)"
watchdog running 70 "10:main:workflow_dispatch:in_progress:run-watchdog" > /dev/null
expect "run-watchdog: its own run does not count" "POST issues" "$(wd_writes)"
watchdog running 70 "74:claude/T-062-x:pull_request:in_progress:pr-check" > /dev/null
expect "run-watchdog: a run created 4 minutes before the last commit counts (5 minutes of slack)" none "$(wd_writes)"
watchdog running 70 "76:claude/T-062-x:pull_request:in_progress:pr-check" > /dev/null
expect "run-watchdog: a run created 6 minutes before the last commit is not a wait" "POST issues" "$(wd_writes)"
watchdog running 95 "20:claude/T-062-x:pull_request:in_progress:pr-check" > /dev/null
expect "run-watchdog: past 90 minutes a running workflow no longer holds the issue back" "POST issues" "$(wd_writes)"
watchdog running 70 - "$wd_open" > /dev/null
expect "run-watchdog: an open issue is updated, not opened again" "PATCH issues/41" "$(wd_writes)"
watchdog running 10 - "$wd_open" > /dev/null
expect "run-watchdog: a new commit closes the issue with a comment" "PATCH issues/41;POST issues/41/comments" "$(wd_writes)"
watchdog finished 300 - "$wd_open" > /dev/null
expect "run-watchdog: a finished run closes its issue" "PATCH issues/41;POST issues/41/comments" "$(wd_writes)"
watchdog finished 300 > /dev/null
expect "run-watchdog: a finished run without an issue changes nothing" none "$(wd_writes)"
watchdog running 70 - '[{"number": 7, "title": "main-broken: abc1234"}, {"number": 8, "title": "session-stalled: RUN-20261027-05 x", "pull_request": {}}]' > /dev/null
expect "run-watchdog: issues that are not its own are never touched" "POST issues" "$(wd_writes)"
watchdog running 70 - "[]" 2026-10-26T06:00:00Z > /dev/null
expect "run-watchdog: a run older than 24 hours is still reported, with a note" 1 "$(grep -c 'hơn 24 giờ trước' "$work/wd.out")"
expect "run-watchdog: a server error on the run file exits 1" 1 \
  "$(watchdog running 10 - "$wd_open" "" '{"contents/docs/runs/RUN-20261027-05.md?ref=claude/RUN-20261027-05-report": "HTTP 500: Server Error"}')"
expect "run-watchdog: ... and changes no issue" none "$(wd_writes)"
expect "run-watchdog: a server error on the workflow runs exits 1" 1 \
  "$(watchdog running 70 - "[]" "" '{"actions/runs?status=in_progress&per_page=100": "HTTP 502: Bad Gateway"}')"
expect "run-watchdog: ... and opens no issue" none "$(wd_writes)"
watchdog running 70 > /dev/null
python3 -I - "$work/wd.json" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1]))
fx["routes"]["branches?per_page=100&page=1"].append({"name": "claude/RUN-20261027-06-report"})
fx["errors"] = {"contents/docs/runs/RUN-20261027-06.md?ref=claude/RUN-20261027-06-report": "HTTP 500: Server Error"}
json.dump(fx, open(sys.argv[1], "w"))
PY
rm -f "$work/wd.json.writes"
expect "run-watchdog: a later run that cannot be read exits 1" 1 \
  "$(PATH="$work/bin:$PATH" FIXTURE="$work/wd.json" GITHUB_REPOSITORY=o/r WATCHDOG_NOW=2026-10-27T12:00:00Z python3 -I scripts/run-watchdog.py > /dev/null 2>&1; echo $?)"
expect "run-watchdog: ... before anything is written for the first run" none "$(wd_writes)"
watchdog running 70 > /dev/null
python3 -I - "$work/wd.json" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1]))
fx["fail_writes"] = ["POST issues"]
json.dump(fx, open(sys.argv[1], "w"))
PY
expect "run-watchdog: a write that fails exits 1" 1 \
  "$(PATH="$work/bin:$PATH" FIXTURE="$work/wd.json" GITHUB_REPOSITORY=o/r WATCHDOG_NOW=2026-10-27T12:00:00Z python3 -I scripts/run-watchdog.py 2> "$work/wd.err" > /dev/null; echo $?)"
expect "run-watchdog: ... and says the write failed" 1 "$(grep -c 'Ghi issue lỗi' "$work/wd.err")"
watchdog running 10 - "$wd_open" > /dev/null
python3 -I - "$work/wd.json" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1]))
fx["fail_writes"] = ["PATCH issues/41"]
json.dump(fx, open(sys.argv[1], "w"))
PY
rm -f "$work/wd.json.writes"
PATH="$work/bin:$PATH" FIXTURE="$work/wd.json" GITHUB_REPOSITORY=o/r WATCHDOG_NOW=2026-10-27T12:00:00Z python3 -I scripts/run-watchdog.py > /dev/null 2>&1 || true
expect "run-watchdog: a close that fails adds no comment" "PATCH issues/41" "$(wd_writes)"
watchdog running 70 > /dev/null
python3 -I - "$work/wd.json" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1]))
fx["routes"].pop("labels/session-stalled")
fx["errors"] = {}
json.dump(fx, open(sys.argv[1], "w"))
PY
rm -f "$work/wd.json.writes"
PATH="$work/bin:$PATH" FIXTURE="$work/wd.json" GITHUB_REPOSITORY=o/r WATCHDOG_NOW=2026-10-27T12:00:00Z \
  python3 -I scripts/run-watchdog.py > /dev/null 2>&1 || true
expect "run-watchdog: a missing label is created before the issue" "POST labels;POST issues" "$(wd_writes)"
python3 -I - "$work/wd.json" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1]))
fx["routes"].pop("branches?per_page=100&page=1")
json.dump(fx, open(sys.argv[1], "w"))
PY
expect "run-watchdog: GitHub that cannot be read exits 1" 1 \
  "$(PATH="$work/bin:$PATH" FIXTURE="$work/wd.json" GITHUB_REPOSITORY=o/r WATCHDOG_NOW=2026-10-27T12:00:00Z python3 -I scripts/run-watchdog.py > /dev/null 2>&1; echo $?)"
```

**Ngoại lệ viết test trước:** tài liệu (`docs/auto-run.md`, `docs/templates/run.md`, `docs/ops/staging-and-runs.md`, `CLAUDE.md`, `docs/template-sync.txt`) và tệp workflow `run-watchdog.yml` (chỉ gọi script; kiểm bằng lần chạy tay sau khi merge, như các workflow trước).

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-27, nhánh chính `084d366`, trong worktree `../wt-t068`, code của mục 3.1 cài thử, không commit; sau đó hoàn tác. Chạy riêng khối test (phần đầu `run.sh` dòng 2 đến 26, khối `save-branch` cộng khối mới; khối `run-watchdog` riêng), rồi toàn bộ `scripts/tests/run.sh`. Bản này đã sửa theo rà soát độc lập lần 1 (3 điểm chặn, 9 điểm nên sửa) và lần 2 (0 chặn, 4 nên sửa).
- **Lệnh:** `bash scripts/tests/run.sh` (JDK 21, macOS, bash 5); `scripts/secret-scan.py`.
- **Kết quả trên code hiện tại** (`save-branch.sh` của nhánh chính, chưa có `run-watchdog.py`; `fake-gh` đã có phần thêm của bước 3):
  - `save-branch patch`: 26 thất bại đúng lý do (không có patch trên nhánh báo cáo, nhánh báo cáo 0 commit, không có dòng patch, lệnh lấy lại không có nên nhánh `none`, không có `.diff`, không có các dòng ghi chú), 38 đối chứng qua (mã 3, bundle, không đẩy nhánh task, không có patch ở các trường hợp không được ghi, worktree báo cáo sạch, phần đã `add` vẫn ở đó, không in bí mật, không in lệnh lấy lại).
  - `run-watchdog`: 27 thất bại đúng lý do (`python3` mã 2 vì chưa có script; không có lần ghi nào), 8 đối chứng qua (không ghi khi 59 phút, khi có CI của phiên, khi có lần chạy tay đang chờ, khi lần chạy trong 5 phút dư, khi danh sách xong mà không có issue, và các "không ghi gì" khi lỗi đọc).
- **Kết quả với bản cài thử:** 64/64 và 35/35 qua; toàn bộ `scripts/tests/run.sh` 841 ok, "All script tests passed" (trước đó 742); `secret-scan.py` sạch.
- **Đột biến** (mỗi lần một chỗ trên bản cài thử, chạy lại khối test):

| # | Đột biến | Test thất bại |
| --- | --- | --- |
| M1 | Bỏ bước quét `secret-scan.py` | 2 |
| M2 | Bỏ giới hạn cỡ | 2 |
| M3 | `git commit` không có `-- <tệp>` | 1 |
| M4 | Bỏ `--binary` | 0: tương đương, `format-patch` mặc định đã ghi nhị phân; giữ cờ để rõ ý |
| M5 | Không đẩy nhánh báo cáo | 11 |
| M6 | Không tự tìm worktree báo cáo | 16 |
| M7 | Vẫn in lệnh khi nhánh báo cáo không đẩy được | 2 |
| M8 | Ghi patch cả khi `--bundle-only` | 1 |
| M9 | Ghi patch với mọi lần từ chối | 1 |
| M10 | Bỏ kiểm UTF-8 | 1 (xác nhận `secret-scan.py` bỏ qua tệp latin-1 có token) |
| M11 | Bỏ bước áp thử và so cây | 2 |
| M12 | Không bỏ `add` khi commit lỗi | 5 |
| M13 | Commit lại khi patch giống hệt | 1 |
| M14 | Không mở worktree tạm cho nhánh báo cáo cục bộ | 1 |
| M15 | Không chặn chính nhánh báo cáo | 1 |
| M16 | Chỉ có mbox, không có `diff` | 2 |
| M17 | Commit lỗi thì xóa cả patch đã theo dõi | 2 |
| M18 | Có nhiều nhánh báo cáo cục bộ thì lấy nhánh đầu | 1 |
| M19 | Có nhiều worktree báo cáo thì lấy cái đầu | 1 |
| M20 | Không so nhánh báo cáo với bản trên GitHub | 1 |
| M21 | Bỏ `trap` dọn dẹp | khối test dừng (worktree tạm còn lại, lệnh `worktree add` sau đó lỗi) |
| W1 | Không xem workflow đang chạy | 6 |
| W2 | Bỏ mốc 90 phút | 1 |
| W3 | Bỏ 5 phút dư | 1 |
| W4 | Mở issue mới dù đã có | 1 |
| W5 | Không đóng khi có commit lại | 2 |
| W6 | Không đóng khi danh sách xong | 1 |
| W7 | Nhận cả PR làm issue của mình | 1 |
| W8 | Chỉ xem nhánh báo cáo, bỏ nhánh task | 7 |
| W9 | Không tạo nhãn | 1 |
| W10 | Bỏ ghi chú 24 giờ | 1 |
| W11 | 60 phút thành `<=` | 1 |
| W12 | Mọi workflow của repo đều tính | 2 |
| W13 | Tính cả chính `run-watchdog` | 1 |
| W14 | Bỏ hàng `queued` | 1 |
| W15 | Lỗi đọc danh sách chạy coi như 404 | 4 |
| W16 | Không ghi gì (bỏ vòng ghi) | 21 |
| W17 | Lần chạy theo lịch trên nhánh chính cũng tính | 1 |
| W18 | Ghi ngay trong vòng đọc | 2 |
| W19 | Bình luận trước khi đóng | 3 |
| W20 | Lỗi ghi không báo | 1 |

- **Chạy với GitHub thật:** `scripts/run-watchdog.py` (bản trước khi sửa theo rà soát, cùng đường REST) chạy từ máy dev với token của người (2026-10-27 08:50 UTC): mã 0, không in gì, không ghi gì, vì không có danh sách nào `running` (đã kiểm mọi nhánh `claude/RUN-*-report` trước khi chạy). Nhãn `session-stalled` chưa có trên repo (lần đầu mở issue sẽ tạo).
- **Chạy trên runner:** workflow tạm `trial-t068` trên nhánh tạm `trial/T-068` (bản trước khi sửa, đổi `schedule` thành `push`, bỏ điều kiện nhánh chính, thêm một bước chỉ đọc), lần chạy 40476497684, xanh: `GITHUB_TOKEN` với `actions: read` đọc được `actions/runs` (thấy chính nó), đọc được issue theo nhãn (0). Lần chạy 40476449325 đỏ là lần đẩy đầu với tệp workflow rỗng (lỗi khi dựng tệp tạm). Nhánh tạm đã xóa, workflow tạm đã tắt. Bản sửa theo rà soát dùng thêm `head_branch`, `event` của cùng đường `actions/runs` (có trong mọi lần chạy của REST) và không đổi quyền, nên không chạy lại.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7: `.github/workflows/` (workflow mới) và quy trình phiên tự chạy (`docs/auto-run.md`; `scripts/save-branch.sh` chạy ở mọi lần đẩy). Mức Cao, cần duyệt chéo; `risk_score` để trống.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script sửa, một script và một workflow mới, công cụ test, năm tài liệu |
| B. Độ rõ của yêu cầu | 1 | Tuấn chọn đề xuất 3b, 4b; ngưỡng, nhịp chạy và ngoại lệ giao patch là đề xuất của AI (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | 99 test và 41 đột biến; workflow và lần GitHub từ chối thật chỉ kiểm được khi gặp |
| D. Ảnh hưởng chéo | 2 | `save-branch.sh` dùng ở mọi task; phần mới chỉ chạy sau khi GitHub đã từ chối vì quyền `workflow` và không đổi mã thoát. Workflow theo lịch tốn phút Actions của repo riêng: mỗi lần khoảng 1 phút tính tiền, 30 phút một lần là 48 phút mỗi ngày, khoảng 1.440 phút mỗi tháng (15 phút một lần thì gấp đôi, khoảng 2.900); từng hết phút Actions làm CI dừng (L-046), nên nhịp do Tuấn chọn (mục 8, câu 2) |
| E. Độ mới của kỹ thuật | 1 | `format-patch`/`git am`/`git apply` và workflow theo lịch đã quen; khuôn script theo `resume-check.py` |
| **Tổng** | 7 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/save-branch.sh`, `scripts/tests/run.sh`, `scripts/tests/fake-gh`, `docs/auto-run.md`, `docs/templates/run.md` (file của mẫu): đề xuất về dự án mẫu qua issue `from-app` sau khi merge (`docs/ops/from-app.md`), cùng `scripts/run-watchdog.py` và workflow; phiên tự chạy không mở issue ở repo khác, nên ghi vào mục Cần người kiểm tra của báo cáo để người mở.
- Task khác có thể xung đột:
  - T-063 (đã merge) có lệnh `mark` ở mục 4 "Tiếp tục phiên" ý 5 của `auto-run.md`: đoạn mới đặt trước danh sách "Phiên tiếp tục làm như sau", không đụng ý 5.
  - T-065 sửa `scripts/tests/run.sh` (khối `setup-check`), `docs/ops/staging-and-runs.md` (mục 1.1, 1.3, 2.3, 2.4) và `docs/template-sync.txt`: khối và đoạn khác nhau.
  - T-066 (cách mở phiên, thử đẩy đầu phiên, `run-check`) sửa `docs/auto-run.md` mục 1, 2, `CLAUDE.md` gần dòng 46 và `scripts/tests/run.sh` gần dòng `export GIT_CONFIG_COUNT` của khối `save-branch`: task chạy sau giữ chữ của task trước ở hai chỗ đó.
  - T-067 (bảng tra lỗi, danh sách được tự sửa, mục Quyết định trong báo cáo) sửa `docs/auto-run.md` "Khi không đẩy được" (thêm ý 5 mới, ý 5 cũ thành 6) và dòng "Lệnh đẩy nhánh bị từ chối ở hai task liên tiếp", cùng dòng PB-13 cho lời từ chối vì quyền `workflow` trong `docs/ops/failure-playbook.md`. Plan này chỉ sửa ý 2 của "Khi không đẩy được" và không đụng dòng "hai task liên tiếp"; dòng PB-13 cần nói "không đẩy lại dưới dạng khác; ngoại lệ duy nhất: `save-branch.sh` tự ghi patch lên nhánh báo cáo để người xem và tự quyết áp (T-068)", cho khớp câu mới ở ý 2.
  - T-069 (`local-setup.sh`) không chung file, trừ có thể `CLAUDE.md` mục Lệnh (dòng khác).
- Đề xuất thứ tự chạy: sau T-066 nếu cùng phiên (T-066 làm phần thử đẩy đầu phiên, để biết phiên có quyền `workflow` trước khi làm task này) và sau T-067 (dòng PB-13); trước các task sửa workflow khác, để lần từ chối sau đó đã có patch trên nhánh báo cáo.

## 7. Điều kiện dừng riêng

- Lệnh đẩy commit có `.github/workflows/run-watchdog.yml` bị từ chối vì quyền `workflow`: làm theo "Khi không đẩy được" (dừng task; plan và ticket giữ `approved`), không tách phần script ra merge riêng, vì tách thành hai PR cần người quyết.
- Lần chạy tay `run-watchdog.yml` sau khi merge mở một issue `session-stalled` cho chính phiên đang chạy: ngưỡng sai; tắt workflow, ghi báo cáo, không sửa ngưỡng trong phiên.

## 8. Câu hỏi cho người duyệt

1. **Ngoại lệ giao patch (điểm chặn của rà soát).** Khi chính GitHub từ chối vì token không có quyền `workflow`, phiên ghi nội dung nhánh thành tệp văn bản trên nhánh báo cáo để người xem và tự quyết áp hay không; phiên không đẩy nhánh task dưới dạng khác, không mở PR, không áp. Không bao giờ làm khi bộ phân loại của môi trường chặn lời gọi đẩy (`--bundle-only`) hay với lần từ chối khác. Đề xuất của AI: đồng ý ngoại lệ này, và ghi nó là ngoại lệ duy nhất trong `auto-run.md` (ý 2) và dòng PB-13 của T-067. Không đồng ý thì bỏ phần 3b, chỉ giữ 4b.
2. **Nhịp chạy và phút Actions.** 30 phút một lần: khoảng 48 phút mỗi ngày, 1.440 phút mỗi tháng; 15 phút: khoảng 2.900 phút mỗi tháng; mỗi giờ: khoảng 720. Đề xuất của AI: 30 phút (ngưỡng 60/90: người biết sau 60 đến 90 phút). Cách khác không tốn thêm phút: gộp bước của watchdog vào job theo giờ của `resume-run.yml` (cùng cách đọc nhánh), đổi lại người biết chậm hơn (tới 120 phút); việc đó sửa `resume-run.yml`, ngoài phạm vi ticket này, để Tuấn quyết. Cách khác không tốn phút khi không có phiên: phiên bật workflow ở đầu phiên (`gh workflow enable`) và workflow tự tắt khi không còn danh sách `running`; cách này cần thêm quyền `actions: write` cho workflow và một bước trong kiểm tra đầu phiên, và phiên chết trước khi bật thì không có gì báo, nên AI chưa đề xuất.
3. **Ngưỡng 60 phút không có commit (90 khi có workflow của phiên đang chạy) và nhịp commit của phiên** (đẩy một dòng Dòng thời gian khi giao và khi nhận bước 3 đến 5, và tối đa 50 phút không commit). Đề xuất của AI: giữ. Các khoảng lặng dài nhất gần đây có workflow chạy kèm (CI 10 đến 15 phút, `device-session` 25, `staging` 12); khoảng không có workflow dài nhất là phiên con làm bước 3 đến 5: 40 phút ở T-046 (RUN-20261024-02), 17 phút ở T-064. Ngưỡng 45 như bản trước thì phiên con 40 phút cộng phần chuẩn bị đã sát ngưỡng.
4. **Hai phiên cùng chạy.** Phiên mở bằng routine được `resume-run.yml` tự tiếp tục sau 90 phút; người thấy issue mà mở thêm phiên tay thì có thể có hai phiên. Thân issue dặn không mở tay với phiên routine; phiên tay mới đẩy nhánh báo cáo ngay ở bước đầu (giờ tiếp tục) nên `resume-run.yml` không kích hoạt thêm. Đề xuất của AI: như vậy là đủ; muốn chắc hơn thì người tắt `resume-run` trước khi mở tay.
5. **Báo qua kênh khác** (email của GitHub đã có cho người theo dõi repo; Slack hay thông báo đẩy cần bí mật mới). Đề xuất của AI: chưa, dùng thông báo issue của GitHub; Tuấn bật "Watch: All activity" hay "Issues" cho repo.
6. **Patch vào nhánh chính** khi PR báo cáo merge (`docs/runs/patches/`), như một bản ghi. Đề xuất của AI: giữ; nếu không muốn thì người xóa thư mục đó khỏi PR báo cáo sau khi đã lấy lại nhánh.
7. **File của mẫu** (`save-branch.sh`, `run.sh`, `fake-gh`, `auto-run.md`, mẫu báo cáo). Đề xuất của AI: đề xuất về mẫu qua issue `from-app` sau khi merge, và ghi vào `docs/template-sync.txt` để lần đồng bộ sau giữ phần của app.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-27, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8: chấp nhận ngoại lệ ghi patch lên nhánh báo cáo chỉ khi GitHub từ chối vì thiếu quyền `workflow`, để người xem và tự quyết áp; `run-watchdog.yml` chạy mỗi 30 phút (khoảng 1.440 phút Actions mỗi tháng); ngưỡng 60 phút, 90 phút khi đang chờ workflow, phiên chính đẩy mốc thời gian ít nhất mỗi 50 phút; giữ patch dưới `docs/runs/patches/`; đề xuất về dự án mẫu sau khi merge.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
