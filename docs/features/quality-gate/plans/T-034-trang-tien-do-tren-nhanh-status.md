---
ticket: T-034
title: Trang tiến độ sinh trên nhánh status sau mỗi lần merge, PR không commit docs/STATUS.md
status: done                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.0M         # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [ci, process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                   # người duyệt
cross_reviewer: Tuấn              # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-034: TRANG TIẾN ĐỘ SINH TRÊN NHÁNH STATUS SAU MỖI LẦN MERGE

## 1. Tóm tắt

`docs/STATUS.md` ra khỏi git (`.gitignore`). Workflow mới `status-page.yml` chạy sau mỗi lần đẩy lên nhánh chính có đổi tài liệu:
- sinh trang bằng `scripts/status.py`;
- đẩy lên nhánh `status`, gồm nhánh chính cộng đúng một commit chứa trang.

Người đọc trang ở `blob/status/docs/STATUS.md`; các liên kết tương đối của trang vẫn đúng, vì cây file của nhánh `status` là cây của nhánh chính. PR không còn sửa file này nên hết xung đột, và `merge-if-green.sh --update` dùng được (L-047, L-016).

Kiểm chứng: test của script kiểm file không còn trong git và workflow chỉ đẩy nhánh `status`; lần chạy đầu của workflow kiểm bằng tay.

Plan chạy trong phiên có người theo dõi, không đưa vào danh sách chạy.

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là bài học L-047 (`accepted`), cách (a) của L-016.

| Yêu cầu | Tiêu chí | Cách hiểu của AI |
| --- | --- | --- |
| Hết xung đột | Khi hai PR cùng đổi trạng thái tài liệu thì không PR nào xung đột vì trang tiến độ | `docs/STATUS.md` không còn trong git; `git add docs/STATUS.md` bị bỏ qua |
| Trang vẫn mới | Khi một PR đổi tài liệu được merge thì trong vài phút trang trên nhánh `status` phản ánh trạng thái mới | `status-page.yml` chạy khi đẩy lên `main` với `paths` `docs/**` và `scripts/status.py`, cùng `workflow_dispatch`. Hai lần merge sát nhau: lần sau hủy lần trước (`concurrency`, `cancel-in-progress`), vì trang luôn sinh lại toàn bộ |
| Không đụng nhánh chính | Workflow không đẩy lên `main` | ADR-0010 và quy tắc "chỉ merge qua `merge-if-green.sh`" giữ nguyên. Workflow chỉ `git push -f origin status`, như `main-guard.yml` đẩy nhánh `revert/<sha>` |
| Lỗi của sổ bài học vẫn lộ ra | Số bài học dùng hai lần (L-042) làm lần chạy đỏ | `status.py` thoát mã 1 khi trùng số, nên bước sinh trang đỏ |
| Xem trước trên máy | Người hay phiên vẫn chạy `scripts/status.py` để xem trang; git bỏ qua file vừa sinh | Không đổi `status.py`, chỉ đổi chú thích và thông báo của `--check` (không còn câu "rồi commit") |

Thiết kế tham chiếu: không có thiết kế riêng.

ADR liên quan: ADR-0010 (cổng chất lượng; plan không đổi), ADR-1002 (`main-guard.yml` đẩy nhánh `revert/`, cùng cách đẩy nhánh).

## 3. Giải pháp

### 3.1. Cách làm

**`.gitignore`:** thêm `docs/STATUS.md` kèm chú thích "sinh bởi scripts/status.py; bản đọc được nằm trên nhánh status (status-page.yml)". Commit `git rm --cached docs/STATUS.md`.

**`.github/workflows/status-page.yml` (mới):**
```yaml
# Builds the progress page (scripts/status.py) after every push to main that changes the documents, and pushes it
# to the `status` branch: main plus one commit holding docs/STATUS.md (L-047). The page is not in git on main, so PRs
# never conflict on it. Never pushes to main. Changing this file is a high-risk plan; cross-review required.
name: status-page
on:
  push:
    branches: [main]
    paths: ['docs/**', 'scripts/status.py']
  workflow_dispatch:
concurrency:
  group: status-page
  cancel-in-progress: true
permissions:
  contents: write
jobs:
  page:
    runs-on: ubuntu-latest
    timeout-minutes: 5
    steps:
      - uses: actions/checkout@v5
      - name: Sinh trang tiến độ và đẩy lên nhánh status
        run: |
          python3 scripts/status.py
          git config user.name "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
          git checkout -B status
          git add -f docs/STATUS.md
          git commit -q -m "Trang tiến độ tại ${GITHUB_SHA::7}"
          git push -f origin status
```
- `git push -f`: nhánh `status` luôn là nhánh chính cộng một commit, không giữ lịch sử của trang (lịch sử trạng thái nằm trong các tài liệu gốc).
- Bước chạy `bash` mặc định có `-e`, nên `status.py` đỏ thì không đẩy gì.
- Đẩy nhánh `status` không khởi động workflow nào: `pr-check.yml` chạy theo `pull_request` vào `main`, `main-guard.yml` và `status-page.yml` theo `push` vào `main`.

**Tài liệu:**
- `docs/CLAUDE.md`, mục Trang tiến độ, viết lại hai gạch đầu dòng:
  - Trang nằm trên nhánh `status` (`https://github.com/VGL-Studio/camera-pose/blob/status/docs/STATUS.md`), do `status-page.yml` sinh sau mỗi lần merge. PR không commit `docs/STATUS.md`; git bỏ qua file này. Xem trước trên máy: `scripts/status.py`.
  - Bỏ câu về gộp nhánh khi xung đột `docs/STATUS.md` (L-016), vì không còn xung đột đó.
  - `--sync` giữ nguyên: commit các ticket mà nó đổi, không commit trang.
  - Plan cũ còn ghi "Ghi lại `docs/STATUS.md`" trong mục 3.2 thì bỏ qua dòng đó.
- `docs/auto-run.md` mục 4, PR báo cáo: bỏ `docs/STATUS.md` khỏi danh sách file của PR báo cáo. Phiên vẫn chạy `scripts/status.py` trên máy để đọc việc đang chờ người, nếu cần cho báo cáo.
- `README.md`: liên kết Tiến độ trỏ tới `https://github.com/VGL-Studio/camera-pose/blob/status/docs/STATUS.md`.
- `docs/README.md`: dòng `STATUS.md` trong cây thư mục ghi "sinh trên nhánh status bởi status-page.yml, không có trong git của nhánh chính".
- `scripts/status.py`: chú thích đầu file và thông báo của `--check` ("Chạy scripts/status.py để sinh lại trang", không còn "rồi commit").

**Các kiểm tra hiện có mà trường hợp mới đi qua (L-044):**

| Kiểm tra | Ảnh hưởng |
| --- | --- |
| `merge-if-green.sh`: nhánh cập nhật, CI, nhãn, `main-broken`, phiên đang chạy | Không đổi. PR không còn sửa trang, nên `--update` chỉ gặp xung đột thật |
| `main-guard.yml` | Không đổi; vẫn bỏ qua `docs/**` |
| `pr-check.yml` | Không đổi. Không test nào so `docs/STATUS.md` của repo với tài liệu: `scripts/tests/run.sh` chỉ kiểm `status.py` trên repo giả (`STATUS_ROOT`) và trang của app mới do `new-app.sh` tạo, phần này chỉ chạy ở dự án mẫu |
| `scripts/plan-check.py` quy tắc 10 | Chỉ nhắc `status.py --sync`, không đọc trang |
| PR đang mở lúc merge plan này mà có sửa `docs/STATUS.md` | Xung đột một lần ("sửa ở nhánh, xóa ở nhánh chính"): gộp nhánh chính và chọn xóa (`git rm docs/STATUS.md`). Ghi vào mô tả PR |

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `.gitignore` | Thêm `docs/STATUS.md` |
| Xóa | `docs/STATUS.md` | Chỉ xóa khỏi git (`git rm --cached`); file trên máy giữ nguyên, git bỏ qua |
| Tạo | `.github/workflows/status-page.yml` | Sinh trang và đẩy nhánh `status` |
| Sửa | `scripts/status.py` | Chú thích đầu file, thông báo của `--check` |
| Sửa | `scripts/tests/run.sh` | Test ở mục 4 |
| Sửa | `docs/CLAUDE.md` | Mục Trang tiến độ |
| Sửa | `docs/auto-run.md` | Mục 4, PR báo cáo |
| Sửa | `README.md` | Liên kết Tiến độ |
| Sửa | `docs/README.md` | Dòng `STATUS.md` của cây thư mục |
| Sửa | `docs/lessons.md` | Dòng Áp dụng của L-016, L-047 |
| Sửa | `docs/features/quality-gate/plans/T-034-trang-tien-do-tren-nhanh-status.md` | Nhật ký thực hiện |
| Sửa | `docs/features/quality-gate/tickets/T-034-trang-tien-do-tren-nhanh-status.md` | Trạng thái |

Các file sau nhắc tới file mà plan sửa nhưng không bị ảnh hưởng (L-020):
- `scripts/plan-check.py` nhắc `scripts/status.py --sync` trong thông báo của quy tắc 10; `--sync` không đổi.
- `docs/lessons.md` nhắc `docs/STATUS.md` trong mô tả các bài học cũ (L-016, L-042, L-043); mô tả không sửa, chỉ dòng Áp dụng.
- Các plan và danh sách chạy đã `done` có dòng "Ghi lại `docs/STATUS.md`": là lịch sử, không sửa.
- `scripts/tests/run.sh` gọi `status.py` với `STATUS_ROOT` trỏ tới repo giả; không đọc `docs/STATUS.md` của repo. `scripts/check.sh` chạy `scripts/tests/run.sh`; test mới chỉ đọc git và file workflow. `scripts/merge-if-green.sh` và `scripts/wait-run.sh` chỉ nhắc `scripts/tests/run.sh` (dùng `gh` giả) hay `docs/auto-run.md` trong chú thích và thông báo.
- `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/resume-check.py` chỉ nhắc `docs/auto-run.md` trong chú thích hay thông báo, trỏ tới các mục mà plan không đổi; plan chỉ đổi danh sách file của PR báo cáo ở mục 4.
- `scripts/device-session.sh` và `scripts/required-assets.txt` chỉ nhắc `docs/CLAUDE.md` trong chú thích (phân tích phiên thử, plan thêm file R2); plan chỉ đổi mục Trang tiến độ.
- `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenTest.kt`, `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenTest.kt`, `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenTest.kt`, `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingStringsTest.kt`, `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingStringsTest.kt` nhắc `README.md` của thư mục thiết kế (`docs/features/<feature>/design/README.md`), không phải `README.md` ở gốc repo mà plan sửa.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Workflow chỉ dùng `actions/checkout@v5`, đã có trong các workflow khác, và `python3` có sẵn trên runner (`status.py` chỉ dùng thư viện chuẩn).

### 3.4. Phương án dự phòng

Không có. Cách đẩy nhánh bằng `GITHUB_TOKEN` với `contents: write` đã chạy được trong `main-guard.yml` (nhánh `revert/e9bf4bb`, issue #35). Repo không có luật bảo vệ nhánh (API `branches/main/protection` trả 404).

## 4. Kiểm thử

Test trong `scripts/tests/run.sh`, viết trước code.

| Tiêu chí | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Trang không còn trong git | Test script | `status page: docs/STATUS.md is not tracked`: `git ls-files docs/STATUS.md` rỗng |
| Git bỏ qua trang | Test script | `status page: git ignores docs/STATUS.md`: `git check-ignore -q docs/STATUS.md` mã 0 |
| Workflow chỉ đẩy nhánh `status` | Test script | `status-page.yml pushes only the status branch`: mọi dòng `git push` của `.github/workflows/status-page.yml` kết thúc bằng `origin status`, và có ít nhất một dòng như vậy |
| Workflow sinh trang bằng `status.py` | Test script | `status-page.yml builds the page with status.py`: có dòng `python3 scripts/status.py` trước dòng `git push` |
| Workflow chạy khi đổi tài liệu | Test script | `status-page.yml runs on pushes to main that change docs`: khối `on.push` có `branches: [main]` và `docs/**` |
| Thông báo `--check` | Test script | `status: check without the page asks to run status.py, not to commit`: repo giả chưa có trang; đầu ra không có "commit" |
| Liên kết của trang | Chạy thử | Mục 4.1: trên cây của nhánh `status`, mọi liên kết tương đối của trang trỏ tới file có thật |
| Không đổi hành vi cũ | Test script | Mọi test `status` hiện có qua mà không sửa |
| Workflow trên GitHub | Kiểm tra thủ công | Sau khi merge: lần chạy `status-page` của commit merge xanh; nhánh `status` có đúng một commit trên nhánh chính; mở `blob/status/docs/STATUS.md`, bấm vài liên kết. Merge thêm một PR tài liệu: trang đổi theo |
| Hết xung đột | Kiểm tra thủ công | Hai PR tài liệu tiếp theo đổi trạng thái: PR thứ hai không xung đột; nếu nhánh chậm thì `merge-if-green.sh --update` cập nhật được (mã 2), không còn 422 vì trang |

**Ngoại lệ viết test trước:** tài liệu (`docs/CLAUDE.md`, `docs/auto-run.md`, `README.md`, `docs/README.md`, `docs/lessons.md`) không có test. Workflow chỉ chạy trên GitHub; logic của nó là ba lệnh, phần có thể kiểm tĩnh đã có test ở trên.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-23, trên một bản clone tạm (không đẩy):
  - `git checkout -B status origin/main`, `git rm --cached docs/STATUS.md`, thêm dòng vào `.gitignore`, `scripts/status.py`, `git add -f docs/STATUS.md`: trang được đưa vào commit dù bị bỏ qua.
  - Kiểm 59 liên kết tương đối của trang trên cây đó: không liên kết nào trỏ tới file không có.
  - `gh api repos/VGL-Studio/camera-pose/branches/main/protection`: 404, repo không có luật bảo vệ nhánh.
- **Chưa thử:** workflow trên GitHub (chỉ chạy sau khi merge). Cách đẩy nhánh bằng `GITHUB_TOKEN` đã chạy được ở `main-guard.yml`.
- **Lệnh:** `scripts/tests/run.sh` sau khi viết test.
- **Kết quả:** trang sinh trên nhánh `status` đọc được và liên kết đúng.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI, quy trình phiên tự chạy): workflow mới có quyền ghi vào repo, đổi quy tắc commit trang tiến độ (`docs/CLAUDE.md`, `docs/auto-run.md`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một workflow, `.gitignore`, chú thích của một script, vài đoạn tài liệu |
| B. Độ rõ của yêu cầu | 1 | L-047 rõ; còn câu hỏi chọn cách |
| C. Khả năng kiểm chứng tự động | 2 | Workflow chỉ kiểm được bằng tay sau khi merge |
| D. Ảnh hưởng chéo | 2 | Mọi PR đổi tài liệu và PR báo cáo của phiên |
| E. Độ mới của kỹ thuật | 1 | Cùng cách đẩy nhánh như `main-guard.yml` |
| **Tổng** | 7 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `.gitignore`, `docs/CLAUDE.md`, `docs/auto-run.md`, `scripts/status.py`.
- Task khác có thể xung đột: T-033 cũng sửa `docs/auto-run.md` mục 4 (đoạn khác); làm lần lượt. Mọi PR đang mở có sửa `docs/STATUS.md` xung đột một lần sau khi plan này merge (mục 3.1, dòng cuối của bảng).
- Đề xuất thứ tự chạy: phiên có người theo dõi; merge khi không có PR nào khác đang mở, để không ai phải gộp lại.

## 7. Điều kiện dừng riêng

1. Một test hiện có của `scripts/tests/run.sh` phải sửa điều nó kiểm tra mới qua được.
2. Lần chạy đầu của `status-page.yml` không đẩy được nhánh `status` (403): dừng, revert phần `.gitignore` và `git rm --cached` để trang trở lại nhánh chính cho tới khi rõ nguyên nhân.

## 8. Câu hỏi cho người duyệt

1. **Chọn cách nào?**
   - (A) Plan này: trang ra khỏi git, sinh trên nhánh `status`. Hết xung đột hẳn; trang mới sau mỗi lần merge tài liệu. Người đọc trang qua một liên kết khác.
   - (B) Giữ trang trong git; `merge-if-green.sh --update` gặp 422 mà xung đột chỉ ở `docs/STATUS.md` thì tự gộp, sinh lại trang, đẩy lên nhánh PR. Vẫn tốn một lần CI mỗi PR; thêm logic git vào cổng chất lượng.
   - (C) Workflow sinh trang rồi commit thẳng lên nhánh chính. Trái quy tắc chỉ merge qua `merge-if-green.sh`, cần ADR; mỗi lần merge thêm một commit trên nhánh chính.
   - Đề xuất của AI: (A).
   - Trả lời: Chọn (A), đúng đề xuất của AI.
2. **Có cần ADR không?** Không ADR nào quy định trang nằm trong git; quy tắc ở `docs/CLAUDE.md`, và nhánh chính không bị ghi. Đề xuất của AI: không viết ADR; ghi lý do vào bài học L-047 và đề xuất về dự án mẫu.
   - Trả lời: Đồng ý với đề xuất của AI.
3. **Nhánh `status` chỉ có một commit trên nhánh chính, đẩy đè mỗi lần?** Đề xuất của AI: có. Lịch sử trạng thái đã nằm trong các tài liệu gốc; giữ lịch sử của trang chỉ làm nhánh dài thêm.
   - Trả lời: Đồng ý với đề xuất của AI.## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Tuấn trả lời mục 8 cùng AI ngày 2026-10-23 và ghi quyết định duyệt.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### 2026-10-23, phiên có người theo dõi (Tuấn)

- **Bước 3, test viết trước:** 6 test mới trong `scripts/tests/run.sh`. Trên code cũ cả 6 đỏ, đúng lý do: `docs/STATUS.md` còn trong git và không bị bỏ qua, chưa có `status-page.yml`, thông báo `--check` còn chữ "commit".
- **Bước 4, code:** `.gitignore`, `git rm --cached docs/STATUS.md`, `.github/workflows/status-page.yml` theo mục 3.1, chú thích và thông báo `--check` của `scripts/status.py`, `docs/CLAUDE.md` mục Trang tiến độ, `docs/auto-run.md` PR báo cáo, `README.md`, `docs/README.md`, `docs/lessons.md` (L-016, L-047).
- **Bước 5:** `scripts/tests/run.sh` qua 309 test. `scripts/check.sh` để CI của PR chạy, vì plan không đổi code của app.
- **Còn lại sau khi merge** (mục 4, hai dòng kiểm tay; điều kiện dừng 2): lần chạy `status-page` của commit merge xanh, nhánh `status` có một commit trên nhánh chính, liên kết của trang mở được; hai PR tài liệu sau không xung đột vì trang.

### 2026-10-23, sau khi merge

- PR #57 merge bằng `merge-if-green.sh` (commit 09e55f7 trên nhánh chính).
- `status-page` lần chạy 39936153612 xanh. Nhánh `status` là nhánh chính (09e55f7) cộng đúng một commit "Trang tiến độ tại 09e55f7" chứa `docs/STATUS.md`; GitHub hiện trang ở `blob/status/docs/STATUS.md`. Điều kiện dừng 2 không xảy ra.
- Còn lại: hai PR tài liệu tiếp theo không xung đột vì trang (PR của T-033 là PR đầu tiên).
