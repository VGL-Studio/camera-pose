---
ticket: T-068
title: Nhánh chưa đẩy được giữ thành patch trên nhánh báo cáo; báo người khi phiên tự chạy đứng im (run-watchdog)
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 7.6M         # cỡ M, số gợi ý của scripts/token-usage.py estimate (2026-10-27), không nâng tay (L-081)
blocking_points: [ci, process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-068: NHÁNH CHƯA ĐẨY ĐƯỢC GIỮ THÀNH PATCH; BÁO NGƯỜI KHI PHIÊN ĐỨNG IM

## 1. Tóm tắt

Hai việc, cùng mục đích là phiên tự chạy không mất việc và không đứng im mà không ai biết (đề xuất 3b, 4b sau RUN-20261027-01, Tuấn chọn ngày 2026-10-27):

- **3b.** `scripts/save-branch.sh` không đẩy được nhánh task thì, ngoài bundle như hôm nay, ghi các commit của nhánh thành một tệp mbox (`git format-patch --binary --stdout`) vào worktree của nhánh báo cáo, ở `docs/runs/patches/<RUN>/<nhánh>.mbox`, commit riêng tệp đó và đẩy nhánh báo cáo. Tệp văn bản dưới `docs/` đẩy được kể cả khi nhánh task sửa `.github/workflows/` (L-086), nên người lấy lại bằng `git am` mà không cần máy cloud. Patch lớn hơn 1 MB hay bị `scripts/secret-scan.py` bắt thì không ghi; mọi lỗi của phần này chỉ thành một dòng ghi chú, bundle vẫn giữ, mã thoát vẫn 3.
- **4b.** `scripts/run-watchdog.py` (mới) và `.github/workflows/run-watchdog.yml` (mới, mỗi 15 phút): danh sách chạy `running` mà các nhánh của phiên không có commit 45 phút (90 phút khi đang có workflow chạy mà phiên có thể đang chờ) thì mở một issue `session-stalled: RUN-...` có sẵn prompt tiếp tục để dán; cập nhật issue ở các lần sau; đóng khi phiên commit lại hay danh sách không còn `running`. Chỉ ghi issue mang nhãn `session-stalled`, không kích hoạt phiên hay routine.

Code và 45 test (27 của `save-branch`, 18 của `run-watchdog`) đã chạy thật trên nhánh chính `084d366` (mục 4.1): trên code hiện tại 26 test thất bại đúng lý do, 19 đối chứng qua; với bản cài thử cả 45 qua; 17 đột biến, 16 bị bắt, 1 tương đương. Script watchdog chạy thật với GitHub (chỉ đọc) và trong một workflow tạm trên runner.

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là đề xuất 3b và 4b mà Tuấn chọn sau RUN-20261027-01, cùng bài học L-084 (phiên đứng 2 giờ 30 phút ở RUN-20261026-03 vì không có gì báo người) và L-086 (T-062 làm xong nhưng không đẩy được; bundle nằm trên máy cloud).

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| 3b, giữ phần chưa đẩy ngoài máy cloud | Khi lệnh đẩy nhánh task thất bại (hay `--bundle-only`) và có worktree của nhánh báo cáo thì tệp `docs/runs/patches/<RUN>/<nhánh với / thành __>.mbox` có trên nhánh báo cáo ở GitHub, trong một commit riêng | Commit bằng `git commit -- <tệp>`: phần đang soạn khác trong worktree báo cáo (đã `add` hay chưa) không bị commit theo |
| 3b, người lấy lại được | Khi người chạy các lệnh script in ra trong một bản sao mới thì có nhánh cùng tên, cùng cây thư mục (`tree`) và cùng nội dung commit với nhánh gốc, kể cả file nhị phân và file `.github/workflows/` | `git am` tạo commit mới (khác mã), giữ tác giả, lời commit và nội dung |
| 3b, không làm lộ bí mật | Khi patch có chuỗi giống bí mật (`scripts/secret-scan.py`) thì không có tệp nào trên nhánh báo cáo hay trong worktree, script không in chuỗi đó | Quét bản patch trong thư mục tạm trước khi chép |
| 3b, không đẩy tệp lớn | Khi patch lớn hơn `SAVE_BRANCH_PATCH_MAX_BYTES` (mặc định 1048576) thì không ghi, báo lý do | Ảnh chuẩn screenshot vài trăm KB vẫn vừa |
| 3b, không làm hỏng điều đã có | Khi không có worktree của nhánh báo cáo, khi nhánh báo cáo cũng không đẩy được, hay khi commit lỗi thì vẫn có bundle, vẫn mã 3, có một dòng ghi chú và không in lệnh `git am` | Mọi test cũ của khối `save-branch` giữ nguyên |
| 4b, báo người khi phiên đứng im | Khi danh sách chạy trên nhánh báo cáo là `running` và các nhánh của phiên (nhánh báo cáo, `claude/T-NNN-...`, `claude/fix-T-NNN-...` của các ticket trong danh sách, như `scripts/resume-check.py`) không có commit 45 phút thì có một issue mở `session-stalled: RUN-...` mang nhãn `session-stalled`, thân có dòng "Danh sách chạy: docs/runs/RUN-....md." và dòng "Đây là phiên tiếp tục: ..." của `docs/auto-run.md` mục 4 | Nhãn chưa có thì tạo |
| 4b, không báo nhầm khi phiên đang chờ | Khi có lần chạy workflow `queued` hay `in_progress` tạo sau commit cuối (trừ 5 phút) thì chưa mở issue cho tới 90 phút | Chờ CI, `device-session`, `staging` đều là workflow; workflow tạo trước commit cuối không tính |
| 4b, một issue mỗi phiên | Khi đã có issue mở cho phiên thì chỉ cập nhật thân, không mở thêm | So theo tiêu đề `session-stalled: RUN-...`; bỏ PR |
| 4b, tự đóng | Khi phiên có commit trong 45 phút hay danh sách không còn `running` thì issue được đóng kèm một bình luận | |
| 4b, không đụng việc khác | Khi có issue khác (ví dụ `main-broken`) thì không ghi gì vào chúng; workflow không kích hoạt phiên hay routine, không cần bí mật | Quyền `contents: read`, `issues: write`, `actions: read` |
| 4b, đọc GitHub lỗi | Khi không đọc được GitHub thì mã 1, không đổi issue nào | Workflow đỏ, lần sau thử lại |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0009 (tài khoản, quyền nhỏ nhất), ADR-0010 (cổng chất lượng), ADR-0025 (nhánh chính tự hồi phục: watchdog không đụng `main-broken`), ADR-0029 (phạm vi). Bài học: L-008, L-081, L-084, L-086 của app.

## 3. Giải pháp

### 3.1. Cách làm

**`scripts/save-branch.sh`** (file của dự án mẫu; đề xuất về mẫu sau khi merge, mục 6). Phần mới chạy sau khi đã có bundle, nên mọi nhánh lỗi của nó chỉ đổi dòng ghi chú cuối đoạn "Nhánh chưa đẩy":

1. Tìm worktree của nhánh báo cáo: `SAVE_BRANCH_REPORT_DIR`, không có thì dòng `worktree` đứng trước `branch refs/heads/claude/RUN-*-report` trong `git worktree list --porcelain` (phiên làm nhánh báo cáo trong `../report`, `docs/auto-run.md` mục 2). Nhánh của worktree đó không khớp `claude/RUN-YYYYMMDD-NN-report` thì ghi chú "Không có worktree của nhánh báo cáo".
2. `git format-patch --binary --stdout <gốc>..<nhánh>` vào một thư mục tạm, với gốc là `git merge-base origin/main <nhánh>`. Lớn hơn giới hạn thì ghi chú; `SECRET_SCAN_ROOT=<thư mục tạm> scripts/secret-scan.py` báo lỗi thì ghi chú (không in giá trị). Thư mục tạm luôn bị xóa.
3. Chép vào `<worktree báo cáo>/docs/runs/patches/<RUN>/<nhánh, / thành __>.mbox`, `git add` rồi `git commit -m "<RUN>: patch của nhánh chưa đẩy <nhánh> (<mã>)" -- <tệp>`, đẩy `HEAD:<nhánh báo cáo>` một lần (không thử lại: nhánh báo cáo bị từ chối thì đó là chuyện của mục "Lệnh đẩy nhánh bị từ chối ở hai task liên tiếp").
4. Đẩy được thì in thêm dòng "Patch trên nhánh báo cáo" và bốn lệnh lấy lại; không thì in dòng ghi chú.

`diff` trên nhánh chính `084d366`:

```diff
diff --git a/scripts/save-branch.sh b/scripts/save-branch.sh
index 4d04bf7..479ca95 100755
--- a/scripts/save-branch.sh
+++ b/scripts/save-branch.sh
@@ -7,6 +7,13 @@
 # Exit:    0 pushed. 3 not pushed (with a bundle when the branch has commits beyond origin/main). 2 bad arguments.
 # Env:     SAVE_BRANCH_DIR            where bundles go (default $HOME/rescue, outside any worktree)
 #          SAVE_BRANCH_RETRY_SECONDS  waits between retries of a network error (default "60 120 240")
+#          SAVE_BRANCH_REPORT_DIR     worktree of the report branch (default: the worktree on claude/RUN-*-report)
+#          SAVE_BRANCH_PATCH_MAX_BYTES  largest patch kept on the report branch (default 1048576)
+# A branch that is not pushed is also written as one mbox file (git format-patch) on the report branch, which is
+# pushed (camera-pose L-086, T-068): the bundle lives on the cloud machine and is lost with it, while a text file
+# under docs/ is pushed even when the branch itself touches .github/workflows/. The mbox is left out when it is
+# larger than the limit or when scripts/secret-scan.py finds a secret in it; the bundle is kept either way.
+# A commit with no change gives an empty patch that `git am` stops at: skip it with `git am --skip`.
 # Never retries a push the environment refused: that call never reaches this script, so the session itself must
 # not push again in another form (auto-run.md, "Khi không đẩy được").
 # Runs in the current repository, so it works from a worktree.
@@ -68,6 +75,57 @@ if ! git bundle create "${bundle}" "${branch}" --not origin/main > /dev/null 2>&
 fi
 size="$(wc -c < "${bundle}" | tr -d ' ')"
 echo "CHƯA ĐẨY: ${branch} (${short_sha}). Đã giữ ${count} commit trong bundle."
+
+# The same commits as one mbox file on the report branch, which is pushed even when this branch is not.
+report_dir="${SAVE_BRANCH_REPORT_DIR:-}"
+if [ -z "${report_dir}" ]; then
+  report_dir="$(git worktree list --porcelain | awk '
+    /^worktree / { dir = substr($0, 10) }
+    /^branch refs\/heads\/claude\/RUN-[0-9]+-[0-9]+-report$/ { print dir; exit }')"
+fi
+report_branch=""
+if [ -n "${report_dir}" ]; then
+  report_branch="$(git -C "${report_dir}" symbolic-ref -q --short HEAD 2> /dev/null || true)"
+fi
+patch_note=""
+patch_lines=""
+if [[ ! "${report_branch}" =~ ^claude/(RUN-[0-9]{8}-[0-9]{2})-report$ ]]; then
+  patch_note="Không có worktree của nhánh báo cáo (claude/RUN-...-report), nên không ghi patch; chỉ có bundle."
+else
+  run_id="${BASH_REMATCH[1]}"
+  base="$(git merge-base origin/main "${branch}")"
+  rel="docs/runs/patches/${run_id}/${branch//\//__}.mbox"
+  max_bytes="${SAVE_BRANCH_PATCH_MAX_BYTES:-1048576}"
+  scan_dir="$(mktemp -d)"
+  git format-patch --binary --stdout "${base}..${branch}" > "${scan_dir}/patch.mbox"
+  patch_size="$(wc -c < "${scan_dir}/patch.mbox" | tr -d ' ')"
+  script_dir="$(cd "$(dirname "$0")" && pwd)"
+  if [ "${patch_size}" -gt "${max_bytes}" ]; then
+    patch_note="Patch ${patch_size} byte, lớn hơn giới hạn ${max_bytes} byte (SAVE_BRANCH_PATCH_MAX_BYTES), nên không ghi lên nhánh báo cáo; chỉ có bundle."
+  elif ! SECRET_SCAN_ROOT="${scan_dir}" python3 -I "${script_dir}/secret-scan.py" > /dev/null 2>&1; then
+    patch_note="scripts/secret-scan.py thấy chuỗi giống bí mật trong patch, nên không ghi lên nhánh báo cáo; chỉ có bundle. Người xem lại nhánh trước khi đẩy."
+  else
+    mkdir -p "$(dirname "${report_dir}/${rel}")"
+    cp "${scan_dir}/patch.mbox" "${report_dir}/${rel}"
+    git -C "${report_dir}" add -- "${rel}"
+    if ! git -C "${report_dir}" commit -q -m "${run_id}: patch của nhánh chưa đẩy ${branch} (${short_sha})" -- "${rel}"; then
+      patch_note="Không commit được patch trên nhánh báo cáo (${report_dir}); chỉ có bundle."
+    elif ! push_err="$(git -C "${report_dir}" push -q origin "HEAD:${report_branch}" 2>&1)"; then
+      echo "${push_err}" >&2
+      patch_note="Đã commit patch \`${rel}\` trên nhánh báo cáo, nhưng nhánh báo cáo cũng không đẩy được: patch chỉ có trên máy này, như bundle."
+    else
+      patch_lines="- Patch trên nhánh báo cáo: \`${rel}\` (${patch_size} byte, gốc \`$(git rev-parse --short=7 "${base}")\`). Lấy lại không cần máy cloud:
+
+\`\`\`
+git fetch origin ${report_branch} main
+git switch -c ${branch} ${base}
+git show origin/${report_branch}:${rel} | git am
+git push -u origin ${branch}
+\`\`\`"
+    fi
+  fi
+  rm -rf "${scan_dir}"
+fi
 cat << EOF
 
 ### Nhánh chưa đẩy
@@ -80,4 +138,9 @@ git fetch ${bundle} ${branch}:${branch}
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
15 minutes; it only opens, updates and closes issues labelled session-stalled, and never starts a session.

Usage: scripts/run-watchdog.py
A run is quiet when its run file on its report branch is "running" and none of its branches (the report branch,
claude/T-NNN-... and claude/fix-T-NNN-... of its tickets, as in scripts/resume-check.py) got a commit for
WATCHDOG_STALL_MINUTES (45). While a workflow run of the repository is queued or in progress and started after the
last commit, the session may be waiting for it (CI, device-session, staging), so the issue waits until
WATCHDOG_HARD_MINUTES (90). A quiet run gets one open issue "session-stalled: RUN-..." whose body has the prompt
to paste; the body is refreshed on later runs. The issue is closed when the run has a new commit or is no
longer "running".

Exit 0 when the check ran, 1 when GitHub could not be read. Prints what it did, and the issue body, on stdout.
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

STALL_MINUTES = int(os.environ.get("WATCHDOG_STALL_MINUTES", "45"))
HARD_MINUTES = int(os.environ.get("WATCHDOG_HARD_MINUTES", "90"))
LABEL = "session-stalled"
REPORT_BRANCH = re.compile(r"^claude/(RUN-\d{8}-\d{2})-report$")
TITLE = re.compile(r"^session-stalled: (RUN-\d{8}-\d{2})\b")


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
        raise LookupError(f"{method} {path}")
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
    except (LookupError, KeyError, TypeError):
        return None


def busy_workflows(since):
    """Names of workflow runs queued or in progress that started after `since`: the session may be waiting."""
    names = []
    for status in ("in_progress", "queued"):
        runs = (api(f"actions/runs?status={status}&per_page=100") or {}).get("workflow_runs", [])
        for run in runs:
            created = moment(run.get("created_at"))
            if created and created >= since - timedelta(minutes=5) and run.get("name") != "run-watchdog":
                names.append(run.get("name") or "?")
    return sorted(set(names))


def body_for(run_id, run_file, latest, quiet, note):
    resume = (
        "Bạn đang chạy một phiên tự chạy của repo này, mở bằng claude --cloud, không qua routine hay start-run.yml. "
        "Đọc CLAUDE.md, docs/CLAUDE.md và docs/auto-run.md, rồi làm đúng quy trình trong docs/auto-run.md.\n\n"
        f"Danh sách chạy: {run_file}.\n\n"
        "Đây là phiên tiếp tục: nhánh báo cáo đã có, danh sách chạy đang running. "
        "Làm theo docs/auto-run.md, mục 4, Tiếp tục phiên sau khi hết hạn mức."
    )
    last = latest.strftime("%Y-%m-%d %H:%M UTC") if latest else "không rõ"
    lines = [
        f"Phiên `{run_id}` vẫn `running` trên nhánh `claude/{run_id}-report`, nhưng không có commit nào "
        f"trong {quiet} phút (commit gần nhất: {last}).",
        "",
        "Người làm: xem phiên còn chạy không (claude.ai/code hoặc terminal đã mở `claude --cloud`). "
        "Phiên đã dừng thì mở lại bằng `claude --cloud` và dán đoạn dưới (docs/auto-run.md, mục 4):",
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
    except LookupError:
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
    for branch in sorted(branches):
        match = REPORT_BRANCH.match(branch)
        if not match:
            continue
        run_id = match.group(1)
        run_file = f"docs/runs/{run_id}.md"
        try:
            text = api(f"contents/{run_file}?ref={branch}", raw=True)
        except LookupError:
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
                close(number, f"Phiên `{run_id}` có commit lại ({quiet} phút trước). Đóng.")
            continue
        busy = busy_workflows(latest) if latest else []
        if busy and quiet < HARD_MINUTES:
            print(f"{run_id}: {quiet} phút không có commit, nhưng đang có workflow chạy ({', '.join(busy)}): "
                  f"có thể phiên đang chờ; báo khi tới {HARD_MINUTES} phút.")
            continue
        note = ""
        started = moment(fields.get("started_at"))
        if started and now - started > timedelta(hours=24):
            note = "Phiên bắt đầu hơn 24 giờ trước: `resume-run.yml` không tự tiếp tục; người quyết định."
        body = body_for(run_id, run_file, latest, quiet, note)
        if number:
            api(f"issues/{number}", method="PATCH", body={"body": body})
            print(f"{run_id}: {quiet} phút không có commit. Cập nhật issue #{number}.")
        else:
            ensure_label()
            api("issues", method="POST", body={"title": f"session-stalled: {run_id} không có commit {quiet} phút",
                                               "body": body, "labels": [LABEL]})
            print(f"{run_id}: {quiet} phút không có commit. Mở issue {LABEL}.")
        print(body)
    for run_id, number in sorted(open_issues.items()):
        if run_id not in running:
            close(number, f"Danh sách chạy `{run_id}` không còn `running`. Đóng.")
    return 0


def close(number, message):
    api(f"issues/{number}/comments", method="POST", body={"body": message})
    api(f"issues/{number}", method="PATCH", body={"state": "closed"})
    print(f"Đóng issue #{number}: {message}")


if __name__ == "__main__":
    REPO = repository()
    try:
        sys.exit(main())
    except (LookupError, json.JSONDecodeError) as error:
        print(f"Không đọc được GitHub ({error}). Không đổi issue nào.", file=sys.stderr)
        sys.exit(1)
```

**`.github/workflows/run-watchdog.yml`** (mới). Chạy ở phút 7, 22, 37, 52 (lệch `resume-run` ở phút 23, `env-canary` ở phút 0), chỉ trên nhánh chính, `persist-credentials: false`, không bí mật. Toàn văn:

```yaml
# Tells people when an auto-run session has gone quiet (T-068, camera-pose L-084). Every 15 minutes,
# scripts/run-watchdog.py looks for a run file that is still "running" on its report branch but whose branches have
# had no commit for 45 minutes (90 while a workflow run it may be waiting for is still going), and opens or updates
# one issue labelled session-stalled with the prompt to paste; it closes the issue when the session commits again or
# the run is no longer "running". A session opened with `claude --cloud` is not continued by resume-run.yml (L-008),
# so this issue is how a person learns it must be reopened.
# Only reads the repository and writes session-stalled issues: never starts a session or a routine, never touches
# main-broken or any other issue. Needs no secret.
# Changing this file is a high-risk plan; cross-review required.
name: run-watchdog

on:
  schedule:
    - cron: "7,22,37,52 * * * *"
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

**`scripts/tests/run.sh`**:

- Khối `save-branch`: thêm khối ở mục 4 ngay trước dòng `export GIT_CONFIG_COUNT="${sb_git_config[0]}" ...` cuối khối (khối cần kho git thật, L-082 của mẫu).
- Thêm khối `run-watchdog` ở mục 4 ngay sau test cuối của `resume-check` (`resume-check: finds a report branch on page 2 of the branches`).

**`docs/auto-run.md`** (file của mẫu; câu mới dẫn số bài học của app dạng "camera-pose L-0NN", theo đầu `docs/CLAUDE.md`). Chỉ sửa hai chỗ, không chạm mục 1, mục 2, bước 6 hay mục 5 (T-066, T-067 sửa các chỗ đó):

- Bước 7, "Khi không đẩy được", ý 2: thay câu "Bundle nằm ở `$HOME/rescue/`, ngoài worktree." bằng: "Bundle nằm ở `$HOME/rescue/`, ngoài worktree, và mất khi máy cloud bị thu hồi. Script cũng ghi các commit thành tệp `docs/runs/patches/<RUN>/<nhánh>.mbox` trên nhánh báo cáo (worktree `../report`) và đẩy nhánh báo cáo, trừ khi patch lớn hơn 1 MB hay có chuỗi giống bí mật; người lấy lại bằng `git am` theo lệnh script in ra (camera-pose L-086, T-068)."
- Mục 4, "Tiếp tục phiên sau khi hết hạn mức": sau ý 3 của danh sách đầu (trước "Phiên tiếp tục làm như sau:") thêm đoạn: "Phiên mở bằng `claude --cloud` không được `resume-run.yml` tiếp tục (camera-pose L-008). Mỗi 15 phút, `run-watchdog.yml` chạy `scripts/run-watchdog.py`: danh sách `running` mà các nhánh của phiên không có commit 45 phút (90 phút khi đang có workflow chạy, vì phiên có thể đang chờ CI, `device-session` hay `staging`) thì mở issue `session-stalled` có sẵn prompt tiếp tục; có commit lại hay danh sách xong thì tự đóng (camera-pose L-084, T-068). Vì vậy phiên không để quá 40 phút không có commit nào trên các nhánh của phiên mà không có workflow đang chạy: việc dài hơn (phiên con làm bước 3 đến 5 của task cỡ L, soạn plan ở bước 5 của mục 4) thì phiên chính ghi một dòng vào Dòng thời gian và đẩy nhánh báo cáo. Phiên không đóng, sửa hay bình luận issue `session-stalled`; người mở lại phiên thì issue tự đóng ở lần chạy sau."

**`docs/templates/run.md`**, mục "Nhánh chưa đẩy": thay chỗ giữ chỗ bằng "<Không có. Hoặc chép đoạn "Nhánh chưa đẩy" mà `scripts/save-branch.sh` in ra: nhánh, mã commit, đường dẫn bundle, lệnh lấy lại từ bundle, và dòng "Patch trên nhánh báo cáo" kèm lệnh `git am` (hay dòng ghi chú vì sao không có patch) (auto-run.md, bước 7).>"

**`docs/ops/staging-and-runs.md`**, đoạn "Khi phiên bị ngắt", ý 1: thêm vào đầu "Issue `session-stalled` (workflow `run-watchdog.yml`, mỗi 15 phút) báo khi phiên không có commit 45 phút; thân issue có sẵn prompt để dán (T-068)." Và đoạn "Nhánh chưa đẩy" của báo cáo (nếu có) có lệnh `git am` lấy nhánh từ nhánh báo cáo.

**`CLAUDE.md`** (gốc repo, mục Lệnh): thêm dòng "- Xem phiên tự chạy nào đứng im (danh sách `running`, không có commit 45 phút): `scripts/run-watchdog.py`; `run-watchdog.yml` chạy nó mỗi 15 phút và mở issue `session-stalled` (T-068)."

**`docs/template-sync.txt`** (file của app): thêm một dòng chú thích "# T-068: scripts/save-branch.sh (patch trên nhánh báo cáo) và khối test của nó trong scripts/tests/run.sh là phần của app chưa có ở mẫu: khi gộp xung đột, giữ phần này (đã đề xuất from-app).". `scripts/run-watchdog.py` và `run-watchdog.yml` không có ở mẫu nên lần đồng bộ không đè. T-065 cũng thêm chú thích vào file này: hai dòng ở chỗ khác nhau, task sau giữ dòng của task trước.

**Cần quyền `workflow` (L-086):** commit tạo `.github/workflows/run-watchdog.yml` chỉ đẩy được bằng token có quyền `workflow`. Danh sách chạy chỉ xếp T-068 vào phiên mở bằng routine (`start-run.yml`) hay phiên local, hoặc phiên `claude --cloud` sau khi Tuấn cấp lại quyền và lần thử đẩy đầu phiên qua (đề xuất 4c, T-066). Không đẩy được thì phiên làm theo "Khi không đẩy được", và chính phần 3b của task này giữ nhánh thành patch (chỉ khi phần 3b đã có trên nhánh chính, tức không áp dụng cho lần chạy T-068).

**Sau khi merge:** phiên chạy tay một lần `gh api -X POST "repos/{owner}/{repo}/actions/workflows/run-watchdog.yml/dispatches" -f ref=main` (cách gọi của phiên, L-063 của mẫu), chờ bằng `scripts/wait-run.sh --workflow run-watchdog.yml --since <giờ>`. Lúc đó danh sách của chính phiên đang `running` và vừa có commit, nên kết quả đúng là "đang chạy, commit gần nhất N phút trước" và không mở issue. Đỏ thì xem log: lỗi đọc GitHub (quyền) thì tắt workflow (`gh workflow disable run-watchdog.yml`), ghi vào báo cáo, không revert; lỗi của script thì revert theo PR thường (`claude/T-068-revert`, `git revert -m 1 <merge>`, CI, `merge-if-green.sh`).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/save-branch.sh` | Ghi mbox lên nhánh báo cáo khi không đẩy được (mục 3.1, `diff`) |
| Tạo | `scripts/run-watchdog.py` | Script watchdog (mục 3.1, toàn văn) |
| Tạo | `.github/workflows/run-watchdog.yml` | Workflow mỗi 15 phút (mục 3.1, toàn văn) |
| Sửa | `scripts/tests/run.sh` | Khối test `save-branch patch` (27 dòng `expect` mới) và `run-watchdog` (18), mục 4 |
| Sửa | `docs/auto-run.md` | Bước 7 "Khi không đẩy được" ý 2; mục 4 "Tiếp tục phiên sau khi hết hạn mức" đoạn watchdog và nhịp commit 40 phút |
| Sửa | `docs/templates/run.md` | Mục "Nhánh chưa đẩy": dòng patch |
| Sửa | `docs/ops/staging-and-runs.md` | "Khi phiên bị ngắt": issue `session-stalled` |
| Sửa | `CLAUDE.md` | Mục Lệnh: một dòng `run-watchdog.py` |
| Sửa | `docs/template-sync.txt` | Một dòng chú thích phần của app phải giữ khi đồng bộ |
| Sửa | `docs/features/quality-gate/tickets/T-068-patch-va-canh-bao-phien-dung.md` | Trạng thái |

Nơi đọc file bị đổi (L-020 của mẫu): `scripts/save-branch.sh` được gọi ở `docs/auto-run.md` (bước 3, 7, Giao bước 3 đến 5, mục 4 bước 5) và test của nó; tham số và mã thoát không đổi, chỉ thêm dòng in ra ở cuối đoạn "Nhánh chưa đẩy", nên không chỗ gọi nào phải sửa. `docs/runs/patches/` là thư mục mới: `scripts/status.py` chỉ đọc `docs/runs/RUN-*.md` (không đệ quy), `scripts/run-check.py` đọc một tệp danh sách chạy, `pr-check.yml` xếp `docs/**` vào đường chỉ tài liệu; `scripts/secret-scan.py` quét mọi tệp git theo dõi, gồm cả mbox, và mbox đã qua quét trước khi ghi. Không file nào đọc `run-watchdog.*`.

Các file nhắc tới tài liệu mà plan sửa (L-020 của mẫu, `scripts/plan-check.py`):

- `docs/auto-run.md` được nhắc trong chú thích hay thông báo của `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`: các file đó dẫn tên mục (mục 2, mục 4, bước 8, bước 9), không đọc nội dung; plan không đổi tên mục nào và chỉ thêm câu vào bước 7 và mục 4.
- `docs/ops/staging-and-runs.md` được nhắc trong `.github/workflows/device-session.yml`, `.github/workflows/env-canary.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/staging.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/env-canary.sh`, `scripts/setup-check.sh`: chỉ là đường dẫn trong chú thích hay thông báo; plan chỉ thêm một câu vào "Khi phiên bị ngắt".
- `scripts/tests/run.sh` được `scripts/check.sh` và `.github/workflows/pr-check.yml` chạy; plan chỉ thêm test.
- `CLAUDE.md` được nhắc trong `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt` và `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt` (chú thích về cách kiểm màn hình); plan chỉ thêm một dòng ở mục Lệnh.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm (app). Workflow mới chỉ có `contents: read`, `issues: write`, `actions: read` của `GITHUB_TOKEN`; không bí mật mới.
- **Thư viện và SDK:** Không thêm (Python chuẩn, `gh`, `git` có sẵn trên runner).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** trên runner hay máy cloud, `git format-patch --binary` của một nhánh có commit rỗng (`--allow-empty`) cho một patch rỗng, và `git am` dừng ở commit đó ("Patch is empty").
- **Điều kiện chuyển:** không chuyển trong phiên; đây là giới hạn đã biết. Task của phiên không tạo commit rỗng (mỗi commit là test hay code), nên không gặp trong thực tế.
- **Cách làm thay:** người chạy `git am --skip` ở commit rỗng, hoặc lấy từ bundle khi còn máy. Ghi vào chú thích đầu `save-branch.sh` và mục Giới hạn của mô tả PR.

## 4. Kiểm thử

Thứ tự commit (L-025 của mẫu): bước 3 commit hai khối test dưới đây vào `scripts/tests/run.sh` (khung rỗng: không cần, vì test gọi script qua dòng lệnh; trên code hiện tại `scripts/run-watchdog.py` chưa có nên `python3` trả mã 2, test thất bại đúng lý do, mục 4.1). Bước 4 commit `save-branch.sh`, `run-watchdog.py`, workflow và tài liệu.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| 3b, mbox trên nhánh báo cáo, commit riêng | Test script | `save-branch patch: a refused push still exits 3`, `... the mbox is on the pushed report branch`, `... the report branch got one commit`, `... the rescue section names the mbox` |
| 3b, người lấy lại được (nhị phân, workflow) | Test script | `save-branch patch: the printed commands rebuild the same tree`, `... the rebuilt branch keeps both commit messages` (nhánh có `.github/workflows/x.yml` và `b.bin` nhị phân; chạy đúng các lệnh in ra, trừ `git push`, trong một bản sao mới) |
| 3b, `--bundle-only` cũng ghi | Test script | `save-branch patch: --bundle-only writes the mbox too`, `... pushes the report branch`, `... never pushes the task branch` |
| 3b, không commit phần khác của worktree báo cáo | Test script | `save-branch patch: other staged changes of the report worktree`, `... stay staged, not committed` |
| 3b, giới hạn cỡ | Test script | `save-branch patch: a patch over the limit is left out`, `... over the limit, no mbox on the report branch`, `... over the limit says so` |
| 3b, bí mật | Test script | `save-branch patch: a patch with a secret exits 3`, `... a secret is never on the report branch`, `... never left in the report worktree`, `... says secret-scan`, `... the secret is not printed` (chuỗi token GitHub dựng lúc chạy, để `run.sh` không tự khớp) |
| 3b, nhánh báo cáo cũng bị từ chối | Test script | `save-branch patch: report branch refused too exits 3`, `... says the patch is only local`, `... prints no apply commands` |
| 3b, không có worktree báo cáo; `SAVE_BRANCH_REPORT_DIR` | Test script | `save-branch patch: no report worktree still bundles`, `... no report worktree says so`, `... SAVE_BRANCH_REPORT_DIR picks the worktree`, `... with SAVE_BRANCH_REPORT_DIR the mbox is pushed`; test cũ của khối `save-branch` giữ nguyên (đối chứng) |
| 4b, báo khi đứng im 45 phút | Test script (`fake-gh`) | `run-watchdog: a running session quiet for 60 minutes exits 0`, `... opens one session-stalled issue`, `... the issue body has the resume line`, `... the issue body names the run file`, `... the quiet time counts the latest commit of any branch of the run` |
| 4b, không báo nhầm | Test script | `run-watchdog: a session with a commit 30 minutes ago is left alone`, `... a workflow started after the last commit means the session may be waiting`, `... and says which workflow`, `... a workflow that started before the last commit is not a wait`, `... past 90 minutes a running workflow no longer holds the issue back` |
| 4b, một issue, tự đóng | Test script | `run-watchdog: an open issue is updated, not opened again`, `... a new commit closes the issue with a comment`, `... a finished run closes its issue`, `... a finished run without an issue changes nothing` |
| 4b, không đụng việc khác; nhãn; 24 giờ; lỗi đọc | Test script | `run-watchdog: issues that are not its own are never touched`, `... a run older than 24 hours is still reported, with a note`, `... a missing label is created before the issue`, `... GitHub that cannot be read exits 1` |
| 4b, workflow chạy được trên runner với `GITHUB_TOKEN` | Kiểm tra thủ công (người: chạy một lần sau khi merge, mục 3.1) | `run-watchdog.yml` chạy tay trên nhánh chính xanh, in "đang chạy, commit gần nhất N phút trước" cho danh sách của chính phiên, không mở issue |
| 3b trong phiên thật | Kiểm tra thủ công (người: chỉ có khi một lệnh đẩy bị từ chối) | Lần đầu báo cáo có dòng "Patch trên nhánh báo cáo", người chạy bốn lệnh in ra và mở PR |

Khối test `save-branch patch` (thêm trước dòng `export GIT_CONFIG_COUNT="${sb_git_config[0]}" ...` của khối `save-branch`):

```bash
# A branch that is not pushed is also written as an mbox on the report branch and pushed there (T-068).
# The report branch lives in its own worktree, as in a session (docs/auto-run.md, mục 2).
sb_report="$work/sb-report"
sb_hook_mode none
git -C "$sb_clone" worktree add -q -b claude/RUN-20261027-09-report "$sb_report" main
git -C "$sb_report" push -q -u origin claude/RUN-20261027-09-report
git -C "$sb_clone" checkout -q -b claude/T-997-patch main
printf 'one\n' > "$sb_clone/a.txt"
git -C "$sb_clone" add a.txt && git -C "$sb_clone" commit -q -m "T-997: a"
mkdir -p "$sb_clone/.github/workflows"
printf 'name: x\n' > "$sb_clone/.github/workflows/x.yml"
printf '\x00\x01\x02\xff' > "$sb_clone/b.bin"
git -C "$sb_clone" add .github/workflows/x.yml b.bin && git -C "$sb_clone" commit -q -m "T-997: workflow and binary"
sb_patch_head="$(git -C "$sb_clone" rev-parse HEAD)"
sb_mbox_rel="docs/runs/patches/RUN-20261027-09/claude__T-997-patch.mbox"
sb_hook_task() { # the remote refuses task branches only, like GitHub refusing a workflow file (L-086)
  rm -f "$sb_calls" "$sb_calls.count"
  printf '#!/bin/sh\nwhile read old new ref; do case "$ref" in refs/heads/claude/T-*) echo x >> "%s"; echo "remote: refusing to allow an OAuth App to create or update workflow without workflow scope" >&2; exit 1;; esac; done\nexit 0\n' \
    "$sb_calls" > "$sb_hook"
  chmod +x "$sb_hook"
}
sb_report_has() { git -C "$sb_origin" cat-file -e "refs/heads/claude/RUN-20261027-09-report:$1" 2> /dev/null && echo yes || echo no; }
sb_hook_task
expect "save-branch patch: a refused push still exits 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: the mbox is on the pushed report branch" yes "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: the report branch got one commit" 1 \
  "$(git -C "$sb_origin" rev-list --count main..claude/RUN-20261027-09-report)"
expect "save-branch patch: the rescue section names the mbox" yes "$(grep -qF -- "- Patch trên nhánh báo cáo: \`$sb_mbox_rel\`" "$work/sb.out" && echo yes || echo no)"
expect "save-branch patch: the bundle is still made" ok \
  "$(git -C "$sb_clone" bundle verify "$work/rescue/claude__T-997-patch.bundle" > /dev/null 2>&1 && echo ok || echo bad)"
sb_apply="$work/sb-apply"
git clone -q "$sb_origin" "$sb_apply" 2> /dev/null
git -C "$sb_apply" config user.email p@example.com
git -C "$sb_apply" config user.name p
sb_cmds="$(awk '/^- Patch trên nhánh báo cáo/ { on = 1; next } on && /^```$/ { n++; if (n == 2) exit; next } on && n == 1 && !/^git push/' "$work/sb.out")"
(cd "$sb_apply" && eval "$sb_cmds") > /dev/null 2>&1 || true
expect "save-branch patch: the printed commands rebuild the same tree" "$(git -C "$sb_clone" rev-parse "$sb_patch_head^{tree}")" \
  "$(git -C "$sb_apply" rev-parse -q --verify 'claude/T-997-patch^{tree}' || echo none)"
expect "save-branch patch: the rebuilt branch keeps both commit messages" "T-997: workflow and binary|T-997: a" \
  "$(git -C "$sb_apply" log --format=%s main..claude/T-997-patch 2> /dev/null | paste -sd '|' -)"
git -C "$sb_report" reset -q --hard main && git -C "$sb_report" push -q -f origin claude/RUN-20261027-09-report
rm -f "$sb_calls"
expect "save-branch patch: --bundle-only writes the mbox too" 3 "$(sb_run --bundle-only claude/T-997-patch)"
expect "save-branch patch: --bundle-only pushes the report branch" yes "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: --bundle-only never pushes the task branch" 0 "$(sb_calls_count)"
git -C "$sb_report" reset -q --hard main && git -C "$sb_report" push -q -f origin claude/RUN-20261027-09-report
printf 'session draft\n' > "$sb_report/notes.md"
git -C "$sb_report" add notes.md
expect "save-branch patch: other staged changes of the report worktree" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: ... stay staged, not committed" "A  notes.md" "$(git -C "$sb_report" status --short notes.md)"
git -C "$sb_report" rm -q --cached notes.md && rm -f "$sb_report/notes.md"
git -C "$sb_report" reset -q --hard main && git -C "$sb_report" push -q -f origin claude/RUN-20261027-09-report
expect "save-branch patch: a patch over the limit is left out" 3 "$(SAVE_BRANCH_PATCH_MAX_BYTES=100 sb_run claude/T-997-patch)"
expect "save-branch patch: over the limit, no mbox on the report branch" no "$(sb_report_has "$sb_mbox_rel")"
expect "save-branch patch: over the limit says so" 1 "$(grep -c 'lớn hơn giới hạn 100 byte' "$work/sb.out")"
git -C "$sb_clone" checkout -q -b claude/T-996-secret main
printf 'token=%s%s\n' "gh""p_" "abcdefghijklmnopqrstuvwxyz0123456789" > "$sb_clone/leak.txt"
git -C "$sb_clone" add leak.txt && git -C "$sb_clone" commit -q -m "T-996: leak"
expect "save-branch patch: a patch with a secret exits 3" 3 "$(sb_run claude/T-996-secret)"
expect "save-branch patch: a secret is never on the report branch" no \
  "$(sb_report_has docs/runs/patches/RUN-20261027-09/claude__T-996-secret.mbox)"
expect "save-branch patch: a secret is never left in the report worktree" no \
  "$([ -e "$sb_report/docs/runs/patches/RUN-20261027-09/claude__T-996-secret.mbox" ] && echo yes || echo no)"
expect "save-branch patch: a secret says secret-scan" 1 "$(grep -c 'secret-scan.py thấy chuỗi giống bí mật' "$work/sb.out")"
expect "save-branch patch: the secret is not printed" 0 "$(grep -c 'abcdefghijklmnopqrstuvwxyz0123456789' "$work/sb.out")"
git -C "$sb_clone" checkout -q claude/T-997-patch
sb_hook_mode reject
expect "save-branch patch: report branch refused too exits 3" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: report branch refused says the patch is only local" 1 "$(grep -c 'nhánh báo cáo cũng không đẩy được' "$work/sb.out")"
expect "save-branch patch: report branch refused prints no apply commands" 0 "$(grep -c '| git am' "$work/sb.out")"
git -C "$sb_report" reset -q --hard main
git -C "$sb_clone" worktree remove --force "$sb_report"
sb_hook_task
expect "save-branch patch: no report worktree still bundles" 3 "$(sb_run claude/T-997-patch)"
expect "save-branch patch: no report worktree says so" 1 "$(grep -c 'Không có worktree của nhánh báo cáo' "$work/sb.out")"
git -C "$sb_clone" worktree add -q "$sb_report" claude/RUN-20261027-09-report
expect "save-branch patch: SAVE_BRANCH_REPORT_DIR picks the worktree" 3 \
  "$(SAVE_BRANCH_REPORT_DIR="$sb_report" sb_run claude/T-997-patch)"
expect "save-branch patch: with SAVE_BRANCH_REPORT_DIR the mbox is pushed" yes "$(sb_report_has "$sb_mbox_rel")"
git -C "$sb_clone" checkout -q claude/T-999-thu
```

Khối test `run-watchdog` (thêm sau test cuối của `resume-check`):

```bash
# run-watchdog.py opens an issue when a running session has no commit for 45 minutes, so a person can
# continue a `claude --cloud` session that resume-run.yml does not continue (T-068).
watchdog() { # <run status> <minutes since the last commit> [busy: minutes since a workflow started, or -] [open issues JSON] [started_at]
  local last busy_runs="[]" code=0
  last="$(python3 -c "import datetime as d; print((d.datetime(2026,10,27,12,0,tzinfo=d.timezone.utc)-d.timedelta(minutes=$2)).strftime('%Y-%m-%dT%H:%M:%SZ'))")"
  if [ "${3:--}" != "-" ]; then
    busy_runs="[{\"name\": \"pr-check\", \"created_at\": \"$(python3 -c "import datetime as d; print((d.datetime(2026,10,27,12,0,tzinfo=d.timezone.utc)-d.timedelta(minutes=$3)).strftime('%Y-%m-%dT%H:%M:%SZ'))")\"}]"
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
  "actions/runs?status=in_progress&per_page=100": {"workflow_runs": $busy_runs},
  "actions/runs?status=queued&per_page=100": {"workflow_runs": []},
  "labels/session-stalled": {"name": "session-stalled"}
}}
JSON
  PATH="$work/bin:$PATH" FIXTURE="$work/wd.json" GITHUB_REPOSITORY=o/r WATCHDOG_NOW=2026-10-27T12:00:00Z \
    python3 -I scripts/run-watchdog.py > "$work/wd.out" 2>&1 || code=$?
  echo "${code:-0}"
}
wd_writes() { if [ -f "$work/wd.json.writes" ]; then paste -sd ';' - < "$work/wd.json.writes"; else echo none; fi; }
wd_open='[{"number": 41, "title": "session-stalled: RUN-20261027-05 không có commit 50 phút"}]'
expect "run-watchdog: a running session quiet for 60 minutes exits 0" 0 "$(watchdog running 60)"
expect "run-watchdog: ... opens one session-stalled issue" "POST issues" "$(wd_writes)"
expect "run-watchdog: the issue body has the resume line" 1 \
  "$(grep -c 'Đây là phiên tiếp tục: nhánh báo cáo đã có, danh sách chạy đang running.' "$work/wd.out")"
expect "run-watchdog: the issue body names the run file" 1 "$(grep -c '^Danh sách chạy: docs/runs/RUN-20261027-05.md.$' "$work/wd.out")"
expect "run-watchdog: the quiet time counts the latest commit of any branch of the run" 1 \
  "$(grep -c 'không có commit nào trong 60 phút' "$work/wd.out")"
watchdog running 30 > /dev/null
expect "run-watchdog: a session with a commit 30 minutes ago is left alone" none "$(wd_writes)"
watchdog running 60 20 > /dev/null
expect "run-watchdog: a workflow started after the last commit means the session may be waiting" none "$(wd_writes)"
expect "run-watchdog: ... and says which workflow" 1 "$(grep -c 'đang có workflow chạy (pr-check)' "$work/wd.out")"
watchdog running 60 70 > /dev/null
expect "run-watchdog: a workflow that started before the last commit is not a wait" "POST issues" "$(wd_writes)"
watchdog running 95 20 > /dev/null
expect "run-watchdog: past 90 minutes a running workflow no longer holds the issue back" "POST issues" "$(wd_writes)"
watchdog running 60 - "$wd_open" > /dev/null
expect "run-watchdog: an open issue is updated, not opened again" "PATCH issues/41" "$(wd_writes)"
watchdog running 10 - "$wd_open" > /dev/null
expect "run-watchdog: a new commit closes the issue with a comment" "POST issues/41/comments;PATCH issues/41" "$(wd_writes)"
watchdog finished 300 - "$wd_open" > /dev/null
expect "run-watchdog: a finished run closes its issue" "POST issues/41/comments;PATCH issues/41" "$(wd_writes)"
watchdog finished 300 > /dev/null
expect "run-watchdog: a finished run without an issue changes nothing" none "$(wd_writes)"
watchdog running 60 - '[{"number": 7, "title": "main-broken: abc1234"}, {"number": 8, "title": "session-stalled: RUN-20261027-05 x", "pull_request": {}}]' > /dev/null
expect "run-watchdog: issues that are not its own are never touched" "POST issues" "$(wd_writes)"
watchdog running 60 - "[]" 2026-10-26T06:00:00Z > /dev/null
expect "run-watchdog: a run older than 24 hours is still reported, with a note" 1 "$(grep -c 'hơn 24 giờ trước' "$work/wd.out")"
python3 -I - "$work/wd.json" << 'PY'
import json, sys
fx = json.load(open(sys.argv[1]))
fx["routes"].pop("labels/session-stalled")
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

- **Đã chạy thử:** 2026-10-27, nhánh chính `084d366`, trong worktree `../wt-t068`, code của mục 3.1 cài thử, không commit; sau đó hoàn tác. Chạy riêng khối test (phần đầu `run.sh` dòng 2 đến 26, khối `save-branch` cộng khối mới; khối `run-watchdog` riêng), rồi toàn bộ `scripts/tests/run.sh`.
- **Lệnh:** `bash scripts/tests/run.sh` (JDK 21, macOS, bash 5); `scripts/secret-scan.py`.
- **Kết quả trên code hiện tại** (`save-branch.sh` của nhánh chính, chưa có `run-watchdog.py`):
  - `save-branch patch`: 11 thất bại đúng lý do (không có mbox trên nhánh báo cáo, nhánh báo cáo 0 commit, không có dòng patch, lệnh lấy lại không có nên nhánh `none`, không có các dòng ghi chú "lớn hơn giới hạn", "secret-scan.py thấy", "nhánh báo cáo cũng không đẩy được", "Không có worktree"), 16 đối chứng qua (mã 3, bundle, không đẩy nhánh task, phần đã `add` vẫn ở đó, không có mbox khi quá cỡ hay có bí mật, không in bí mật, không in lệnh `git am`).
  - `run-watchdog`: 15 thất bại đúng lý do (`python3` mã 2 vì chưa có script; không có lần ghi nào), 3 đối chứng qua (không ghi khi có commit 30 phút trước, khi đang có workflow, khi danh sách đã xong mà không có issue).
- **Kết quả với bản cài thử:** 27/27 và 18/18 qua; toàn bộ `scripts/tests/run.sh` 787 ok, "All script tests passed" (trước đó 742); `secret-scan.py` sạch.
- **Đột biến** (mỗi lần một chỗ trên bản cài thử, chạy lại khối test):

| # | Đột biến | Test thất bại |
| --- | --- | --- |
| M1 | Bỏ bước quét `secret-scan.py` | 4 |
| M2 | Bỏ giới hạn cỡ | 3 |
| M3 | `git commit` không có `-- <tệp>` (commit cả phần đã `add` khác) | 1 |
| M4 | Bỏ `--binary` | 0: tương đương, `format-patch` mặc định đã ghi nhị phân; giữ cờ để rõ ý |
| M5 | Không đẩy nhánh báo cáo | 8 |
| M6 | Không tự tìm worktree báo cáo | 9 |
| M7 | Vẫn in lệnh `git am` khi nhánh báo cáo không đẩy được | 2 |
| W1 | Không xem workflow đang chạy | 2 |
| W2 | Bỏ mốc 90 phút | 1 |
| W3 | Tính cả workflow tạo trước commit cuối | 1 |
| W4 | Mở issue mới dù đã có | 1 |
| W5 | Không đóng khi có commit lại | 1 |
| W6 | Không đóng khi danh sách xong | 1 |
| W7 | Nhận cả PR làm issue của mình | 1 |
| W8 | Chỉ xem nhánh báo cáo, bỏ nhánh task | 5 |
| W9 | Không tạo nhãn | 1 |
| W10 | Bỏ ghi chú 24 giờ | 1 |

- **Chạy với GitHub thật:** `scripts/run-watchdog.py` chạy từ máy dev với token của người (2026-10-27 08:50 UTC): mã 0, không in gì, không ghi gì, vì không có danh sách nào `running` (đã kiểm mọi nhánh `claude/RUN-*-report` trước khi chạy). Nhãn `session-stalled` chưa có trên repo (lần đầu mở issue sẽ tạo).
- **Chạy trên runner:** workflow tạm `trial-t068` trên nhánh tạm `trial/T-068` (bản của mục 3.1, đổi `schedule` thành `push`, bỏ điều kiện nhánh chính, thêm một bước chỉ đọc), lần chạy 40476497684, xanh: `GITHUB_TOKEN` với `actions: read` đọc được `actions/runs` (in `busy: ['trial-t068']`, chính nó; workflow thật bỏ tên `run-watchdog`), đọc được issue theo nhãn (0). Lần chạy 40476449325 đỏ là lần đẩy đầu với tệp workflow rỗng (lỗi khi dựng tệp tạm), không phải của script. Nhánh tạm đã xóa, workflow tạm đã tắt.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7: `.github/workflows/` (workflow mới) và quy trình phiên tự chạy (`docs/auto-run.md`; `scripts/save-branch.sh` chạy ở mọi lần đẩy). Mức Cao, cần duyệt chéo; `risk_score` để trống.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script sửa, một script và một workflow mới, năm tài liệu |
| B. Độ rõ của yêu cầu | 1 | Tuấn chọn đề xuất 3b, 4b; ngưỡng 45 và 90 phút là đề xuất của AI (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | 45 test và 17 đột biến; workflow và lần đẩy bị từ chối thật chỉ kiểm được khi gặp |
| D. Ảnh hưởng chéo | 2 | `save-branch.sh` dùng ở mọi task; phần mới chỉ chạy sau khi lệnh đẩy đã thất bại và không đổi mã thoát |
| E. Độ mới của kỹ thuật | 1 | `format-patch`/`git am` và workflow theo lịch đã quen; khuôn script theo `resume-check.py` |
| **Tổng** | 7 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/save-branch.sh`, `scripts/tests/run.sh`, `docs/auto-run.md`, `docs/templates/run.md` (file của mẫu): đề xuất về dự án mẫu qua issue `from-app` sau khi merge (`docs/ops/from-app.md`), cùng `scripts/run-watchdog.py` và workflow; phiên tự chạy không mở issue ở repo khác, nên ghi vào mục Cần người kiểm tra của báo cáo để người mở.
- Task khác có thể xung đột: T-063 (đã merge) có lệnh `mark` ở mục 4 bước 5 của `auto-run.md`: đoạn mới đặt trước danh sách "Phiên tiếp tục làm như sau", không đụng ý 5. T-065 sửa `scripts/tests/run.sh` (khối `setup-check`), `docs/ops/staging-and-runs.md` (mục 1.1, 1.3, 2.3, 2.4) và `docs/template-sync.txt`: khối và đoạn khác nhau. T-066 (cách mở phiên, `run-check`) sửa `docs/auto-run.md` mục 1, 2 và `docs/ops/pre-run-review.md`; T-067 (bảng tra lỗi, danh sách được tự sửa, mục Quyết định trong báo cáo) sửa `docs/ops/failure-playbook.md`, ADR, `docs/templates/run.md` mục mới và có thể `docs/auto-run.md` bước 6, mục 5: plan này chỉ sửa bước 7 "Khi không đẩy được" ý 2, mục 4 "Tiếp tục phiên" và mục "Nhánh chưa đẩy" của mẫu báo cáo. T-069 (`local-setup.sh`) không chung file, trừ có thể `CLAUDE.md` mục Lệnh (dòng khác).
- Đề xuất thứ tự chạy: sau T-066 nếu cùng phiên (T-066 làm phần thử đẩy đầu phiên, để biết phiên có quyền `workflow` trước khi làm task này); trước các task sửa workflow khác, để lần đẩy bị từ chối sau đó đã có patch trên nhánh báo cáo.

## 7. Điều kiện dừng riêng

- Lệnh đẩy commit có `.github/workflows/run-watchdog.yml` bị từ chối vì quyền `workflow`: làm theo "Khi không đẩy được" (dừng task; plan và ticket giữ `approved`), không tách phần script ra merge riêng, vì phần ghi patch chỉ có ích khi đã có trên nhánh chính và tách ra làm hai PR cần người quyết.
- Lần chạy tay `run-watchdog.yml` sau khi merge mở một issue `session-stalled` cho chính phiên đang chạy: ngưỡng sai; tắt workflow, ghi báo cáo, không sửa ngưỡng trong phiên.

## 8. Câu hỏi cho người duyệt

1. Ngưỡng 45 phút không có commit (90 khi đang có workflow chạy) và nhịp "không quá 40 phút không commit" của phiên. Đề xuất của AI: giữ. Các khoảng lặng dài nhất trong các phiên gần đây có workflow chạy kèm (CI 10 đến 15 phút, `device-session` 25, `staging` 12); khoảng không có workflow dài nhất là phiên con làm bước 3 đến 5 (17 phút ở T-064). Ngắn hơn thì dễ báo nhầm, dài hơn thì người biết muộn.
2. Issue `session-stalled` có cần báo qua kênh khác (email của GitHub đã có cho người theo dõi repo; Slack hay thông báo đẩy cần bí mật mới) không. Đề xuất của AI: chưa, dùng thông báo issue của GitHub; Tuấn bật "Watch: All activity" hay "Issues" cho repo.
3. Mbox của nhánh chưa đẩy nằm trên nhánh báo cáo và vào nhánh chính khi PR báo cáo merge (`docs/runs/patches/`), như một bản ghi. Đề xuất của AI: giữ; nếu không muốn thì người xóa thư mục đó khỏi PR báo cáo sau khi đã lấy lại nhánh.
4. Phần mới đổi file của dự án mẫu (`save-branch.sh`, `run.sh`, `auto-run.md`, mẫu báo cáo). Đề xuất của AI: đề xuất về mẫu qua issue `from-app` sau khi merge, và ghi vào `docs/template-sync.txt` để lần đồng bộ sau giữ phần của app.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
