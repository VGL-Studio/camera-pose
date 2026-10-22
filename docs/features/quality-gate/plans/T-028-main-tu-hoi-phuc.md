---
ticket: T-028
title: Nhánh chính tự hồi phục khi đỏ (ADR-1002)
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 15M          # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [ci, process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-028: NHÁNH CHÍNH TỰ HỒI PHỤC KHI ĐỎ (ADR-1002)

## 1. Tóm tắt

Làm ba điểm của ADR-1002 bằng hai script nhỏ có test, rồi nối vào `main-guard.yml` và `merge-if-green.sh`:
- `failure-kind.sh` phân biệt "chỉ test thất bại" với lỗi khác, để `main-guard` chạy lại `check.sh` một lần và ghi issue `flaky` thay vì `main-broken` khi lần chạy lại xanh;
- `main-guard` đóng `main-broken` khi lần chạy xanh là chính commit bị đổ lỗi;
- `is-exact-revert.sh` kiểm bằng `git` rằng một PR đúng là bản revert của commit bị đổ lỗi, để `merge-if-green.sh` merge được bản đó khi `main-broken` đang mở.

Plan này chạy trong phiên có người theo dõi, không đưa vào danh sách chạy: nó đổi chính cổng chất lượng mà phiên dựa vào.

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là ADR-1002 (`docs/adr/1002-main-tu-hoi-phuc.md`), cùng bài học L-032, L-033, L-034.

| Điểm của ADR-1002 | Tiêu chí | Cách hiểu của AI |
| --- | --- | --- |
| 1. Chạy lại khi chỉ có test thất bại | Khi `check.sh` đỏ chỉ vì task test thất bại thì `main-guard` chạy lại `check.sh` một lần; lần chạy lại xanh thì không mở `main-broken` mà ghi issue `flaky`. Lỗi khác hoặc chạy lại vẫn đỏ thì làm như cũ | "Task test": tên task Gradle kết thúc bằng `UnitTest` (`testDebugUnitTest`, `testStagingDebugUnitTest`) hoặc bắt đầu bằng `verifyRoborazzi`, và lỗi của task là "There were failing tests". Lỗi lint, ktlint, biên dịch, quy tắc của `check.sh` là "lỗi khác" |
| 2. Đóng issue khi chạy lại xanh trên commit bị đổ lỗi | Khi `main-guard` xanh trên chính commit ghi trong issue thì issue tự đóng | So sánh `compare/<culprit>...<sha>` trả `ahead` hoặc `identical` thì đóng; `behind` và `diverged` thì không |
| 3. Merge bản revert đúng | Khi `main-broken` mở, `merge-if-green.sh` chỉ cho qua PR có đúng một commit nằm ngay trên nhánh chính mới nhất, mà cây file trùng với `git revert <culprit>` dựng trên nhánh chính đó; mọi kiểm tra CI vẫn bắt buộc | Có hai issue `main-broken` mở, hoặc issue không ghi commit bị đổ lỗi: từ chối như cũ |
| 4. Phiên tự chạy | Phiên chỉ revert commit merge của chính phiên, chuyển task đó sang `stopped`, rồi làm tiếp các task không phụ thuộc | Viết vào `docs/auto-run.md` mục 4 và bước 9 |

Thiết kế tham chiếu: không có thiết kế riêng.

ADR liên quan: ADR-0010, ADR-1002, ADR-0009.

## 3. Giải pháp

### 3.1. Cách làm

**`scripts/failure-kind.sh <build.log>`** in `tests` hoặc `other`, luôn trả mã 0:
- Đọc mọi dòng `Execution failed for task ':<đường dẫn>'.` trong log.
- In `tests` khi có ít nhất một dòng như vậy, mọi task đều là task test (theo bảng ở mục 2), và log có "There were failing tests". Còn lại in `other`, kể cả log không có dòng nào (lỗi trước Gradle, lỗi quy tắc của `check.sh`).
- Viết bằng bash và `grep`/`sed`, giống các script khác của cổng chất lượng.

**`scripts/is-exact-revert.sh <culprit sha> <head sha> <base ref>`** trả mã 0 khi đúng là bản revert, 1 khi không, và in lý do bằng tiếng Việt:
1. `git rev-list --count <base>..<head>` phải là 1, và `git rev-parse <head>^` phải bằng `git rev-parse <base>`.
2. Tạo worktree tạm ở `<base>` (`git worktree add --detach`), chạy `git revert --no-edit --no-commit <culprit>`. Xung đột thì không phải bản revert đúng.
3. So `git write-tree` của worktree với `git rev-parse <head>^{tree}`. Dọn worktree bằng `trap`.

Chỉ dùng `git`, không gọi GitHub, nên test được bằng repo `git` dựng tạm trong test.

**`.github/workflows/main-guard.yml`:**
- Bước "Cổng chất lượng" đổi `id` thành `first` và thêm `continue-on-error: true`.
- Thêm bước `kind` khi `steps.first.outcome == 'failure'`: ghi `kind=$(scripts/failure-kind.sh build.log)` vào `$GITHUB_OUTPUT`.
- Thêm bước `retry` khi `kind` là `tests`, với `continue-on-error: true`: chép `build.log` sang `first.log`, rồi chạy lại `scripts/check.sh 2>&1 | tee build.log`.
- Thêm bước `check` ("Kết quả cổng chất lượng"):
  - `first` xanh: qua.
  - `retry` xanh: qua, ghi `flaky=true` vào output.
  - Còn lại: `exit 1`.

  Các bước sau giữ điều kiện `steps.check.outcome == 'failure'` như cũ, nên lỗi hạ tầng vẫn không bị tính là đỏ.
- Thêm bước "Ghi test chập chờn" khi `steps.check.outputs.flaky == 'true'`:
  - `gh label create flaky --force`;
  - lấy tên test thất bại từ `first.log` (dòng kết thúc bằng ` FAILED`);
  - bình luận vào issue `flaky` đang mở, hoặc tạo issue mới "flaky: <tên test đầu tiên>", kèm commit, link lần chạy, 40 dòng cuối của `first.log`.
- Bước đóng issue: `[ "${status}" = "ahead" ] || [ "${status}" = "identical" ]`. Sửa câu trong thân issue mới: "Issue tự đóng khi một lần chạy main-guard xanh trên commit chứa bản sửa, hoặc chạy lại xanh trên chính commit này".
- Sửa chú thích đầu file theo ADR-1002.

**`scripts/merge-if-green.sh`:** đoạn kiểm `main-broken` đổi như sau.
- Lấy `number` và `body` của các issue mở. Hơn một issue, hoặc không đọc được `<!-- culprit:<sha> -->`: từ chối như cũ.
- Có đúng một issue: `git fetch -q origin "+refs/pull/$PR/head:refs/merge-if-green/pr" "+refs/heads/main:refs/merge-if-green/main"`, rồi gọi `${IS_EXACT_REVERT:-scripts/is-exact-revert.sh} "$CULPRIT" "$HEAD_SHA" refs/merge-if-green/main`.
  - Qua: in "BẢN REVERT ĐÚNG: PR #<số> là bản revert của <sha>; vẫn kiểm mọi điều kiện còn lại." rồi đi tiếp tới các kiểm tra còn lại (phiên đang chạy, nhánh cập nhật, CI).
  - Không qua: từ chối với thông báo cũ, thêm lý do của `is-exact-revert.sh`.
- `IS_EXACT_REVERT` chỉ để test thay script bằng bản giả. Sửa chú thích "with no exceptions" theo ADR-1002.

**Tài liệu:**
- `docs/auto-run.md` mục 4, dòng "Nhánh chính đỏ", và bước 9: theo ADR-1002 mục 4. Mở PR từ `revert/<sha>` bằng `gh api -X POST .../pulls`, chờ CI bằng `wait-run.sh`, merge bằng `merge-if-green.sh`. Task bị revert chuyển `stopped`, chuẩn bị cho người như task dừng. Chờ `main-guard` của commit revert xanh rồi mới làm tiếp. Chỉ làm khi commit bị đổ lỗi là commit merge của một task trong chính phiên.
- `docs/auto-run.md` mục 5: báo cáo chép các issue `flaky` đang mở.
- `docs/CLAUDE.md` mục Trong phiên tự chạy: một dòng dẫn tới ADR-1002.
- `docs/templates/run.md`: mục Cần người kiểm tra có dòng cho issue `flaky`.

Sau khi merge: chuyển L-032, L-033, L-034 sang `applied` kèm số PR. Sinh lại `docs/foundation.md` và `docs/STATUS.md`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/failure-kind.sh` | Phân loại log của `check.sh`: `tests` hoặc `other` |
| Tạo | `scripts/is-exact-revert.sh` | Kiểm PR đúng là bản revert của commit bị đổ lỗi, chỉ bằng `git` |
| Sửa | `.github/workflows/main-guard.yml` | Chạy lại một lần, issue `flaky`, đóng issue khi `identical` |
| Sửa | `scripts/merge-if-green.sh` | Ngoại lệ cho bản revert đúng khi `main-broken` mở |
| Sửa | `scripts/tests/run.sh` | Test cho hai script mới và ngoại lệ của `merge-if-green.sh` |
| Sửa | `docs/auto-run.md` | Mục 4, bước 9, mục 5 theo ADR-1002 |
| Sửa | `docs/CLAUDE.md` | Mục Trong phiên tự chạy |
| Sửa | `docs/templates/run.md` | Dòng issue `flaky` ở mục Cần người kiểm tra |
| Sửa | `docs/adr/1002-main-tu-hoi-phuc.md` | Chỉ khi người duyệt sửa ADR; trạng thái do người chuyển |
| Ghi lại | `docs/foundation.md` | `scripts/foundation.py` |
| Sửa | `docs/lessons.md` | Dòng Áp dụng của L-032, L-033, L-034 |
| Ghi lại | `docs/STATUS.md` | `scripts/status.py` |

`scripts/check.sh` không đổi: `main-guard` gọi nó hai lần, không sửa nội dung kiểm tra. `scripts/find-breaking-commit.sh` không đổi. Commit được coi là xanh sau lần chạy lại vẫn có kết quả job `guard` là `success`, nên `find-breaking-commit.sh` đọc đúng. `.github/workflows/pr-check.yml` không đổi.

Các file sau nhắc tới file mà plan sửa nhưng không bị ảnh hưởng (L-020):
- `scripts/release.sh` đọc kết luận của lần chạy `main-guard.yml` của commit (`gh run list --workflow main-guard.yml`). Commit xanh sau lần chạy lại vẫn có kết luận `success`, nên vẫn được phát hành. Đây đúng là ý của ADR-1002: commit đó được coi là xanh, test chập chờn được ghi ở issue `flaky`. Plan không sửa file này (CLAUDE.md, mục Không được làm).
- `.github/workflows/device-session.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/device-session.sh`, `scripts/resume-check.py`, `scripts/wait-run.sh` chỉ nhắc `docs/auto-run.md`, `docs/CLAUDE.md`, `docs/templates/run.md` hay `scripts/tests/run.sh` trong chú thích hoặc thông báo; chúng không đọc nội dung các mục mà plan đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `git fetch` của `refs/pull/<số>/head` trong môi trường cloud của phiên (proxy GitHub, L-005).
- **Điều kiện chuyển:** `git fetch` của `refs/pull/...` bị từ chối trong môi trường cloud ở lần chạy thử.
- **Cách làm thay:** lấy nhánh của PR theo tên (`git fetch origin "+refs/heads/$HEAD_REF:..."`) rồi kiểm `rev-parse` khớp `HEAD_SHA` của PR; điều kiện của `is-exact-revert.sh` giữ nguyên.

## 4. Kiểm thử

Test của script trong `scripts/tests/run.sh`, viết trước code. Test giả `gh` bằng `scripts/tests/fake-gh` như các test hiện có.

| Tiêu chí | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| ADR-1002 điểm 1, log chỉ có test thất bại | Test script | `failure-kind: only failing unit tests -> tests`: log có `Execution failed for task ':feature:camera:testDebugUnitTest'.` và "There were failing tests" |
| ADR-1002 điểm 1, screenshot | Test script | `failure-kind: failing verifyRoborazzi -> tests` |
| ADR-1002 điểm 1, lỗi khác | Test script | `failure-kind: compile error -> other` (`:app:compileStagingDebugKotlin`); `lint -> other`; `test and lint both failed -> other`; `no Gradle failure line -> other` |
| ADR-1002 điểm 3, revert đúng | Test script | `is-exact-revert: git revert of the culprit on main passes`: repo tạm với A, B (culprit), C (main); nhánh có một commit `git revert B` trên C |
| ADR-1002 điểm 3, revert có thêm thay đổi | Test script | `is-exact-revert: a revert with an extra change fails` |
| ADR-1002 điểm 3, hai commit | Test script | `is-exact-revert: two commits fail` |
| ADR-1002 điểm 3, không nằm trên main mới nhất | Test script | `is-exact-revert: a revert on an older main fails` |
| ADR-1002 điểm 3, revert commit khác | Test script | `is-exact-revert: the revert of another commit fails` |
| ADR-1002 điểm 3, `merge-if-green` cho qua | Test script | `merge-if-green lets an exact revert through while main is broken`: fixture có một issue `main-broken` với `<!-- culprit:... -->`, `IS_EXACT_REVERT` giả trả 0; mong đợi mã 3 (đi tới bước kiểm nhánh chậm, như các test hiện có) |
| ADR-1002 điểm 3, `merge-if-green` từ chối | Test script | `refuses a PR that is not the revert` (bản giả trả 1, mã 1); `refuses with two main-broken issues` (mã 1); `refuses an issue without culprit` (mã 1) |
| Không đổi hành vi cũ | Test script | Các test `merge-if-green` hiện có vẫn qua |
| ADR-1002 điểm 1, 2 trên GitHub | Kiểm tra thủ công | Sau khi merge: (a) lần `main-guard` đỏ vì test tiếp theo, xem log có bước `retry`, và nếu lần chạy lại xanh thì có issue `flaky`, không có `main-broken`; (b) khi có issue `main-broken`, chạy lại `main-guard` trên commit bị đổ lỗi, xanh thì issue tự đóng. Ghi kết quả vào nhật ký |
| Workflow hợp lệ | Kiểm tra thủ công | `main-guard` chỉ chạy trên nhánh chính, nên CI của PR không chạy nó. Người duyệt đọc kỹ diff của workflow; chạy `actionlint` nếu máy có. Lần merge đầu tiên sau PR chạy `main-guard` mới; xem log lần đó |

**Ngoại lệ viết test trước:** `.github/workflows/main-guard.yml` và các tài liệu không có test tự động; workflow chỉ chạy trên nhánh chính. Người duyệt đọc kỹ diff; kiểm tra thủ công ở dòng cuối bảng.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-22, trên máy dev (macOS, git 2.x), không commit:
  - Cách kiểm cây file: repo `git` tạm với A, B (culprit), C; nhánh có `git revert B` trên C. Worktree tạm ở C, `git revert --no-edit --no-commit B`, `git write-tree` cho `bb5b43b…`, đúng bằng `rev-parse <head>^{tree}`. `rev-list --count main..head` là 1, `head^` bằng `main`.
  - `git fetch origin "+refs/pull/39/head:refs/tmp/pr39"` trên repo này lấy đúng commit đầu của PR #39 (`80eeb4a`).
  - Chưa chạy thử `git fetch` của `refs/pull/...` trong phiên `claude --cloud`. Làm khi bắt đầu task; không được thì dùng phương án dự phòng ở mục 3.4.
- **Lệnh:** `scripts/tests/run.sh` sau khi viết test. `scripts/check.sh` không cần chạy, vì plan không đổi code của app; nhưng CI của PR vẫn chạy `check.sh`.
- **Kết quả:** cách kiểm bằng cây file chạy đúng; lấy được nhánh PR theo `refs/pull` trên máy dev.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI, quy trình phiên tự chạy). Sửa `.github/` và `scripts/merge-if-green.sh` (CLAUDE.md, mục Không được làm; ADR-0010).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Hai script mới, một workflow, script merge, tài liệu quy trình |
| B. Độ rõ của yêu cầu | 1 | ADR-1002 nêu rõ ba điểm; chờ duyệt |
| C. Khả năng kiểm chứng tự động | 2 | Script có test; workflow chỉ kiểm được trên nhánh chính |
| D. Ảnh hưởng chéo | 3 | Đổi cổng chất lượng mà mọi PR và mọi phiên dựa vào |
| E. Độ mới của kỹ thuật | 1 | `git worktree`, `git revert --no-commit`, `continue-on-error` của Actions: phổ biến |
| **Tổng** | 9 | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `main-guard.yml`, `merge-if-green.sh`, `docs/auto-run.md` (cùng PR #39 đang sửa `auto-run.md` bước 9 và mục 1).
- Task khác trong sprint có thể xung đột: PR #39 (quy trình), merge PR #39 trước.
- Đề xuất thứ tự chạy: sau khi RUN-20261023-02 kết thúc và PR #39 đã merge; làm trong phiên có người theo dõi.

## 7. Điều kiện dừng riêng

1. Chạy thử cho thấy `git fetch` của nhánh PR không làm được cả theo `refs/pull` lẫn theo tên nhánh: dừng, giữ điểm 1 và 2, đề xuất bỏ điểm 3.
2. Một test `merge-if-green` hiện có phải sửa điều nó kiểm tra mới qua được.

## 8. Câu hỏi cho người duyệt

1. **Chạy lại cả `check.sh` hay chỉ task test hỏng?** Chạy lại cả `check.sh` đơn giản và giống CI nhất; đệm Gradle làm lần thứ hai nhanh hơn (khoảng 4 đến 7 phút).
   - Đề xuất của AI: chạy lại cả `check.sh`.
2. **Issue `flaky` có chặn gì không?**
   - Đề xuất của AI: không chặn merge hay phiên. Retro xem và tạo ticket sửa test. Phiên chép issue vào báo cáo.
3. **Phiên có được tự revert không, hay chỉ người?**
   - Đề xuất của AI: phiên được revert, nhưng chỉ commit merge của chính phiên, và qua đúng kiểm tra nội dung của `is-exact-revert.sh`.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
