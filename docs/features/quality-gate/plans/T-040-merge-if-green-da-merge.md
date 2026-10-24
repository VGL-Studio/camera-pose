---
ticket: T-040
title: merge-if-green.sh nhận ra PR đã vào nhánh chính khi API merge báo lỗi
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.0M         # cỡ S, số gợi ý của scripts/token-usage.py estimate
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-040: MERGE-IF-GREEN.SH NHẬN RA PR ĐÃ VÀO NHÁNH CHÍNH KHI API MERGE BÁO LỖI

## 1. Tóm tắt

Khi lệnh merge của GitHub trả lỗi, `scripts/merge-if-green.sh` không báo "TỪ CHỐI" ngay. Nó tìm trên nhánh chính commit squash của chính PR này: dòng đầu là `<tiêu đề> (#<số>)` và cây file trùng cây file của commit đầu PR. Tìm thấy thì script báo `ĐÃ MERGE (GitHub báo lỗi)`, thoát 0 và yêu cầu chạy `main-guard` bằng `workflow_dispatch`, vì GitHub có thể không phát sự kiện `push` (L-054). Gọi lại script cho một PR đã vào nhánh chính như vậy cũng nhận ra, báo `ĐÃ MERGE TRƯỚC ĐÓ`, và không bao giờ merge lần hai. `main-guard.yml` có `workflow_dispatch` từ T-041, chạy trước task này. Dòng PB-04 của bảng tra lỗi (`docs/ops/failure-playbook.md`, T-039) đổi việc phải làm theo mã thoát mới. Mọi kiểm tra hiện có của script giữ nguyên thứ tự và hành vi; mỗi kiểm tra có một test cho trường hợp mới (L-044).

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là bài học L-054 (`docs/lessons.md`), từ PR #67 ngày 2026-10-23.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| L-054, lỗi mà đã merge | Khi API merge trả lỗi nhưng nhánh chính đã có commit squash của PR thì script in `ĐÃ MERGE (GitHub báo lỗi)` kèm commit, thoát 0 | Chỉ xét sau khi mọi kiểm tra đã qua (đây là bước cuối). Nhận biết bằng dữ liệu, không bằng thông báo lỗi (thông báo lần trước là `unexpected end of JSON input`, lần sau có thể khác) |
| L-054, không có sự kiện push | Khi nhận ra như trên thì script yêu cầu chạy `main-guard` trên nhánh chính (`workflow_dispatch`) và in cách chờ (`scripts/wait-run.sh --workflow main-guard.yml --since <giờ>`) | Yêu cầu chạy không được thì vẫn thoát 0 (đã merge), in `CHÚ Ý` và hướng dẫn để người chạy tay |
| L-054, lỗi thật | Khi API merge trả lỗi và nhánh chính không có commit đó thì vẫn `TỪ CHỐI: GitHub từ chối merge`, thoát 1 như hiện nay | Có thể GitHub ghi commit chậm vài giây: thử đọc lại 3 lần, cách 10 giây (`MERGE_IF_GREEN_POLL_SECONDS` thay trong test) |
| L-054, gọi lại | Khi PR còn mở mà nhánh chính đã có commit squash của nó thì script không merge lần hai; in `ĐÃ MERGE TRƯỚC ĐÓ`, thoát 0 | Lần #67, gọi lại sẽ thấy nhánh chậm 1 commit (mã 3), rồi `--update` gộp nhánh chính vào và merge thêm một commit rỗng. Kiểm tra mới đặt ngay sau kiểm tra PR còn mở |

Bằng chứng về cách nhận biết (2026-10-24, đọc trên GitHub và git):

- Repo merge kiểu squash, tiêu đề commit là `<tiêu đề PR> (#<số>)` (`merge-if-green.sh` dòng 175 đến 178; 77 commit trên nhánh chính kết thúc bằng `(#<số>)`).
- PR #67: commit đầu `b3709ca`, cây file `7914ab8`. Commit squash `c49a682` có tiêu đề `Danh sách chạy RUN-20261023-03 (draft) và sửa run-check.py đọc nhầm bảng rà soát (#67)`, cùng cây `7914ab8`, cha `29df46e` là nhánh chính lúc gọi. Cây trùng vì script chỉ merge khi nhánh không chậm so với nhánh chính (`behind_by` bằng 0), nên squash không đổi file nào.
- Sau đó người merge trên web tạo `c83777a` (commit merge thường, cha `c49a682` và `b3709ca`); `pulls/67` giờ ghi `merged: true`. Trường `merged` của PR không dùng được làm dấu hiệu lúc lỗi: lúc đó PR còn `open`.

Thiết kế tham chiếu: Không có thiết kế riêng (script, docs/ops/design-workflow.md).

ADR liên quan: ADR-0010 (cổng chất lượng, chỉ merge qua script), ADR-1002 (main tự hồi phục, `main-guard`).

## 3. Giải pháp

### 3.1. Cách làm

**`scripts/merge-if-green.sh`:**

1. Đọc thêm cây file của commit đầu PR: `HEAD_TREE="$(gh api "repos/$REPO/git/commits/$HEAD_SHA" --jq .tree.sha)"`.
2. Hàm `merged_on_main`: đọc `repos/$REPO/commits?sha=$BASE_BRANCH&per_page=30`. Tìm commit đầu tiên có:
   - dòng đầu của `commit.message` đúng bằng `"$TITLE (#$PR)"`;
   - `commit.tree.sha` bằng `HEAD_TREE`.

   Có thì in sha của commit đó, không có thì in rỗng. Cả hai điều kiện là bắt buộc: chỉ tiêu đề thì có thể trùng với một PR khác cùng số trong tiêu đề sao chép; chỉ cây file thì không biết là PR nào.
3. **Kiểm tra mới, ngay sau kiểm tra PR còn mở (dòng 60).** `merged_on_main` có kết quả thì in `ĐÃ MERGE TRƯỚC ĐÓ: PR #<số> đã nằm trên nhánh chính ở commit <7 ký tự> (GitHub chưa đóng PR). Không merge lần hai; đóng PR trên web.` và thoát 0. Với `--dry-run`: cùng thông báo, thoát 0. Đặt trước các kiểm tra khác vì script không ghi gì lên GitHub ở nhánh này, nên mọi điều kiện về merge (nhãn, nhánh chính đỏ, phiên đang chạy, CI) không áp dụng. Bảng ở mục 4 có test cho từng điều kiện.
4. **Bước merge (dòng 175 đến 179).** Lệnh `gh api -X PUT .../merge` lỗi thì:
   - gọi `merged_on_main` tối đa 3 lần, cách `${MERGE_IF_GREEN_POLL_SECONDS:-10}` giây;
   - có kết quả: xóa nhánh `claude/*` như khi merge bình thường, gọi `gh api -X POST "repos/$REPO/actions/workflows/main-guard.yml/dispatches" -f ref=$BASE_BRANCH`, in `ĐÃ MERGE (GitHub báo lỗi): PR #<số> (<tiêu đề>) tại commit <7 ký tự>; GitHub báo lỗi nhưng commit đã vào nhánh chính (L-054). Đã yêu cầu chạy main-guard; chờ: scripts/wait-run.sh --workflow main-guard.yml --since <giờ UTC>.` và thoát 0;
   - yêu cầu chạy `main-guard` lỗi thì thay câu cuối bằng `CHÚ Ý: không yêu cầu được main-guard; chạy tay trên tab Actions.` và vẫn thoát 0;
   - không có kết quả: `TỪ CHỐI: GitHub từ chối merge PR #<số>.` và thoát 1 như hiện nay.
5. Chú thích đầu file: mã 0 gồm cả "đã merge mà GitHub báo lỗi" và "đã merge trước đó".

**`scripts/tests/fake-gh`:** hiện đọc đường dẫn ở tham số thứ hai (`args[1]`). Lệnh merge của script là `gh api -X PUT "repos/..."`, tức `-X` đứng trước đường dẫn, nên bản giả đọc nhầm `-X` thành đường dẫn. Chưa test nào đi tới lệnh merge thật, vì mọi test đều dùng `--dry-run`. Sửa: đường dẫn là tham số đầu tiên sau `api` không bắt đầu bằng `-` và không phải giá trị của `-X`, `-f`, `-H`, `--jq`.

Kiểm tra lúc lập plan: `pulls/67` và `git cat-file` như ở mục 2. Chưa chạy thử script mới (E dưới 2).

**`.github/workflows/main-guard.yml`:** T-040 không sửa file này. `workflow_dispatch` do plan T-041 thêm (mục 3.1 và 3.2 của plan đó), nên T-041 phải merge trước T-040. Workflow không đọc `github.event.before` hay dữ liệu riêng của sự kiện push (`grep -n "github.event" .github/workflows/main-guard.yml` không có kết quả). Nó kiểm commit đầu nhánh chính lúc chạy, nên chạy tay cho kết quả giống như chạy do push.

**`docs/ops/failure-playbook.md` (tạo ở T-039):** dòng PB-04 (`unexpected end of JSON input`, `merge-if-green.sh`) hiện bảo phiên tự tìm commit `(#<số PR>)` trên `origin/main`. Sửa cột việc phải làm thành: làm theo đầu ra của `merge-if-green.sh`. `ĐÃ MERGE (GitHub báo lỗi)` hay `ĐÃ MERGE TRƯỚC ĐÓ` thì coi là đã merge và chờ `main-guard` bằng lệnh script in ra (bước 9); `TỪ CHỐI` thì làm như bước 9. Cột nguồn thêm T-040. Phần này của T-040 chỉ là dòng PB-04; T-038 sửa dòng PB-08 (plan T-039, mục 6).

**`docs/auto-run.md`:**
- Bước 9 thêm một gạch đầu dòng: script in `ĐÃ MERGE (GitHub báo lỗi)` thì coi như đã merge, chờ `main-guard` bằng lệnh script in ra; in `ĐÃ MERGE TRƯỚC ĐÓ` thì không merge lại, ghi vào báo cáo để người đóng PR.
- Phần của T-040 chỉ là gạch đầu dòng này ở bước 9 (L-014). T-036, T-037, T-038 và T-039 sửa đoạn khác của `auto-run.md` (kiểm tra đầu phiên, bước 3, 4, 7 và bước 8).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/merge-if-green.sh` | `HEAD_TREE`, hàm `merged_on_main`, kiểm tra "đã merge trước đó" sau dòng 60, xử lý lỗi của lệnh merge (mục 3.1), chú thích mã thoát |
| Sửa | `scripts/tests/fake-gh` | Đọc đường dẫn đúng khi `-X` đứng trước (mục 3.1). Vào commit test của bước 3, cùng các test mới: test cũ vẫn qua với bản giả mới (L-025) |
| Sửa | `scripts/tests/run.sh` | Các test của mục 4, trong một khối mới sau khối "merge-if-green.sh lets an exact revert through" (khoảng dòng 226 đến 265). T-036, T-037, T-039 thêm khối riêng của chúng ở chỗ khác; không plan nào khác sửa các khối `merge-if-green` có sẵn |
| Sửa | `docs/ops/failure-playbook.md` | Dòng PB-04: việc phải làm theo mã thoát mới (mục 3.1) |
| Sửa | `docs/auto-run.md` | Bước 9: một gạch đầu dòng (mục 3.1) |
| Sửa | `docs/lessons.md` | L-054: dòng Áp dụng ghi T-040 khi merge |
| Sửa | `docs/features/quality-gate/tickets/T-040-merge-if-green-da-merge.md` | Trạng thái theo phiên |

Nơi khác đọc các file plan sửa (`grep -rn "merge-if-green\|fake-gh\|main-guard.yml" scripts/ .github/`):

- `scripts/wait-run.sh` và `scripts/find-breaking-commit.sh` dùng `fake-gh` trong test. Chúng gọi `gh api <đường dẫn> ...` với đường dẫn đứng ngay sau `api`, nên cách đọc mới cho cùng kết quả. Toàn bộ `scripts/tests/run.sh` chạy ở bước 3 để kiểm điều này.
- `start-run.yml` và `preflight.yml` không gọi `merge-if-green.sh`.
- `.github/workflows/pr-check.yml` có chú thích về tên job mà `scripts/merge-if-green.sh` kiểm; plan không đổi `REQUIRED_CHECKS`.
- `scripts/is-exact-revert.sh` được gọi từ `merge-if-green.sh`, không đổi.
- `scripts/status.py` đọc `docs/lessons.md`; plan chỉ thêm chữ vào dòng Áp dụng.
- `scripts/check.sh` chạy `scripts/tests/run.sh` (dòng 32) và nhắc `main-guard.yml` trong chú thích (dòng 5). `check.sh` không đổi; test mới chạy cùng các test cũ ở đó.
- `scripts/release.sh` đọc lần chạy `main-guard.yml` theo commit (`gh run list --workflow main-guard.yml --commit`, dòng 114) không lọc theo sự kiện. Lần chạy bằng `workflow_dispatch` trên commit đầu nhánh chính vì thế cũng được tính. Đó đúng là điều cần sau sự cố của L-054, khi commit không có lần chạy do push.
- `scripts/failure-kind.sh` chỉ nhắc `main-guard.yml` trong chú thích (dòng 6, 8); workflow vẫn gọi nó như cũ.
- Các file nhắc `docs/auto-run.md` chỉ dẫn tới bước hay mục của nó, không đọc nội dung. Đó là `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/start-run.yml`, `.github/workflows/resume-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh` và `scripts/resume-check.py`. Plan chỉ thêm một gạch đầu dòng ở bước 9, không đổi tên hay số của bước, mục nào.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền của app. Token của phiên cần quyền chạy workflow (`actions: write`, cùng quyền mà `rerun-failed-jobs` của T-033 đã dùng); thiếu thì script in `CHÚ Ý` như mục 3.1
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có.

## 4. Kiểm thử

Mọi test là test script trong `scripts/tests/run.sh`, với `fake-gh` và một fixture mới:

- tiêu đề `t`, `pulls/12` mở, `git/commits/abc1234` có `tree.sha` là `tree1`;
- `commits?sha=main&per_page=30` là danh sách commit, mỗi commit có `sha`, `commit.message`, `commit.tree.sha`;
- `fail_writes: ["PUT pulls/12/merge"]` để lệnh merge lỗi;
- `MERGE_IF_GREEN_POLL_SECONDS=0`.

Test không dùng `--dry-run` ghi tên lệnh vào `.writes`, nên kiểm được script đã gọi gì.

**Theo L-044: trường hợp mới đi qua từng kiểm tra hiện có thế nào.**

| Kiểm tra hiện có (dòng) | "Đã merge trước đó" (đầu script) | "Lỗi mà đã merge" (bước merge) | Test |
| --- | --- | --- | --- |
| Tham số lạ (32 đến 41) | Bị chặn trước, như cũ | Bị chặn trước | Test có sẵn `merge-if-green rejects '...'` |
| Không có `gh`, `jq` (44, 45) | Bị chặn trước | Bị chặn trước | Không đổi |
| PR không mở (60) | Bị chặn trước: PR đã đóng thì vẫn `TỪ CHỐI ... không ở trạng thái mở` | Bị chặn trước | `merge-if-green still refuses a closed PR that is on main` (1) |
| PR nháp (61) | Kiểm tra mới đứng trước: in `ĐÃ MERGE TRƯỚC ĐÓ` | Bị chặn trước | `merge-if-green reports a draft PR already on main without merging` (0, không có dòng `PUT` trong `.writes`) |
| Nhãn `needs-human` (64) | Đi qua (không merge gì) | Bị chặn trước | `merge-if-green reports a needs-human PR already on main without merging` (0, không `PUT`); `merge-if-green does not reach the merge of a needs-human PR` (1) |
| Nhánh đích khác `main` (67) | Không xảy ra: `merged_on_main` đọc nhánh `main`, PR nhắm nhánh khác không có commit squash trên đó | Bị chặn trước | `merge-if-green still refuses a PR into another branch` (1) |
| Nhánh chính đỏ, bản revert đúng (74 đến 93) | Đi qua (không merge) | Bị chặn trước như cũ; bản revert đúng đi tới bước merge, nên nhánh mới áp dụng cho nó | `merge-if-green reports a PR already on main while main is broken` (0, không `PUT`); `merge-if-green sees an exact revert merged despite an error` (0) |
| Phiên đang chạy (99 đến 111) | Đi qua (không merge) | PR không phải của phiên bị chặn trước; PR của phiên đi tới bước merge | `merge-if-green reports a person's PR already on main while a run is running` (0, không `PUT`); `merge-if-green still refuses a person's PR while a run is running` (test có sẵn, 1) |
| Nhánh chậm so với nhánh chính (114 đến 125) | Đi qua: lần #67 đúng là trường hợp này; trả 0 thay vì mã 3, không gọi `update-branch` | Bị chặn trước (đã cập nhật thì mới tới bước merge) | `merge-if-green does not update a PR already on main` (0, `.writes` không có `update-branch`) |
| Kiểm tra CI đỏ, chưa xong (128 đến 157) | Đi qua (không merge) | Bị chặn trước | `merge-if-green does not merge a red PR when the API fails` (1, không có `PUT`) |
| Commit đẩy chưa tới 3 phút (161 đến 167) | Đi qua | Vẫn in `CHÚ Ý` như cũ, rồi tới bước merge | `merge-if-green still warns about a fresh commit before the merge` (có dòng `CHÚ Ý: commit cuối`) |
| `--dry-run` (169 đến 172) | `ĐÃ MERGE TRƯỚC ĐÓ`, thoát 0, không ghi gì | Không tới bước merge (dừng trước) | `merge-if-green --dry-run never calls the merge` (không có `PUT`) |

**Hành vi mới:**

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Lỗi mà đã merge | Test script | `merge-if-green reports a merge that GitHub answered with an error` (0); có dòng `^ĐÃ MERGE (GitHub báo lỗi): PR #12`; `.writes` có `PUT pulls/12/merge`, `POST actions/workflows/main-guard.yml/dispatches`, `DELETE git/refs/heads/claude/template-x`. Thất bại trên code hiện tại (thoát 1) |
| Yêu cầu chạy `main-guard` lỗi | Test script | `merge-if-green still reports the merge when main-guard cannot be started` (0, có `CHÚ Ý: không yêu cầu được main-guard`), với `fail_writes` thêm dòng `POST .../dispatches` |
| Lỗi thật | Test script | `merge-if-green refuses when the API fails and main lacks the commit` (1, `TỪ CHỐI: GitHub từ chối merge`, không có `POST .../dispatches`). Đối chứng cho test đầu (L-035): trên nhánh chính có một commit cùng tiêu đề nhưng cây khác, và một commit cùng cây nhưng tiêu đề của PR khác |
| GitHub ghi chậm | Test script | `merge-if-green finds the commit on a later read`: `commits?sha=main...` là `__sequence__` (lần đầu không có, lần hai có), thoát 0 |
| Gọi lại | Test script | `merge-if-green reports a PR already on main without merging` (0, `ĐÃ MERGE TRƯỚC ĐÓ`, `.writes` rỗng) |
| Merge bình thường không đổi | Test script | `merge-if-green merges a green PR`: lệnh merge thành công, thoát 0, `ĐÃ MERGE: PR #12`, không đọc `commits?sha=main` sau merge, không có `POST .../dispatches`. Test này cũng chứng minh `fake-gh` mới đọc đúng `-X PUT` |
| Bảng tra lỗi khớp script | Test script có sẵn của T-039 | Test bảng tra lỗi của T-039 (mỗi dòng có mẫu và việc phải làm) vẫn qua sau khi sửa PB-04 |
| Test cũ giữ nguyên | Test script có sẵn | Toàn bộ `scripts/tests/run.sh` qua ("All script tests passed") với `fake-gh` mới |

**Ngoại lệ viết test trước:** Không có. Việc `main-guard` chạy được bằng tay thuộc T-041 (test cấu trúc và kiểm tay ở plan đó).

### 4.1. Chạy thử

Không cần (E dưới 2): dùng lại `gh api`, `jq` như các nhánh khác của script.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI, cổng chất lượng): `scripts/merge-if-green.sh`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script, bản giả của test, một dòng của bảng tra lỗi |
| B. Độ rõ của yêu cầu | 1 | Có một lần xảy ra thật (PR #67) với bằng chứng đầy đủ; thông báo lỗi lần sau có thể khác nên nhận biết bằng dữ liệu |
| C. Khả năng kiểm chứng tự động | 1 | Test với `gh` giả cho mọi nhánh; lỗi thật của GitHub không tái hiện được |
| D. Ảnh hưởng chéo | 2 | Mọi lần merge của người và phiên; `fake-gh` dùng chung cho test của ba script |
| E. Độ mới của kỹ thuật | 0 | Cùng API và cách test đã có |
| **Tổng** | 5 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/merge-if-green.sh`, `scripts/tests/fake-gh`, `scripts/tests/run.sh`, `docs/ops/failure-playbook.md`, `docs/auto-run.md`.
- Task khác có thể xung đột:
  - T-036, T-037, T-038, T-039 cùng sửa `docs/auto-run.md` và `scripts/tests/run.sh`, ở đoạn khác (mục 3.1, 3.2).
  - T-039 có thể đọc `main-guard.yml` (`failure-kind.sh`) nhưng không sửa phần `on:`.
  - T-041 sửa `start-run.yml` và có thể đọc lần chạy `main-guard` theo commit. Lần chạy bằng `workflow_dispatch` có `event: workflow_dispatch`; plan T-041 đã tính lần chạy loại này (mục 2 của plan đó).
- Đề xuất thứ tự chạy: sau T-039 (tạo `docs/ops/failure-playbook.md` có dòng PB-04) và T-041 (thêm `workflow_dispatch` vào `main-guard.yml`). Làm trong phiên có người theo dõi hoặc sớm trong danh sách chạy, vì mọi task sau đều merge qua script này.

## 7. Điều kiện dừng riêng

- `main-guard.yml` chưa có `workflow_dispatch` (T-041 chưa merge), hoặc chưa có `docs/ops/failure-playbook.md` (T-039 chưa merge): không bắt đầu task (phụ thuộc).
- Toàn bộ `scripts/tests/run.sh` không qua với `fake-gh` mới mà không cần sửa test cũ: dừng (dấu hiệu là bản giả đổi hành vi của test khác).

## 8. Câu hỏi cho người duyệt

1. **Mã thoát khi đã merge mà GitHub báo lỗi.** Ticket ghi "mã riêng". Đề xuất của AI: thoát 0, vì PR thật sự đã vào nhánh chính. Mọi nơi gọi script (phiên, `merge-loop`, người) đều coi 0 là xong. Dòng `ĐÃ MERGE (GitHub báo lỗi)` cùng lệnh chờ `main-guard` cho biết việc phải làm thêm. Một mã mới sẽ làm mọi nơi gọi phải sửa theo.
2. **Đóng PR còn mở.** Lần #67, PR vẫn mở sau lỗi. Đề xuất của AI: script không đóng PR và không bình luận, chỉ in hướng dẫn "đóng PR trên web". Nhánh `claude/*` vẫn bị xóa như khi merge thường, nên GitHub tự đóng PR (không ghi `merged`). Người không cần merge lại trên web như lần #67, vì lần đó tạo thêm commit rỗng `c83777a`.
3. **Đọc bao nhiêu commit của nhánh chính.** Đề xuất của AI: 30 commit gần nhất. Kiểm tra "đã merge trước đó" chỉ có ích ngay sau sự cố, khi nhánh chính mới có thêm vài commit. Đọc thêm thì tốn một lần gọi API lớn hơn cho mọi lần merge.
4. **Đưa về dự án mẫu.** Đề xuất của AI: có. Lỗi của GitHub không riêng app này. Issue `from-app` sau khi merge, cùng `fake-gh` đã sửa.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Duyệt có sửa / Yêu cầu làm lại / Hoãn
- **Người duyệt:** ..., **duyệt chéo:** ...
- **Ghi chú:**

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
