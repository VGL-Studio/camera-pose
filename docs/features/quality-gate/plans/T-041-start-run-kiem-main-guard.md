---
ticket: T-041
title: start-run.yml kiểm main-guard xanh trên commit đầu nhánh chính
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.0M         # cỡ S, số gợi ý của scripts/token-usage.py estimate (tạm, chưa đủ 5 task)
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-24
---

# PLAN T-041: START-RUN.YML KIỂM MAIN-GUARD XANH TRÊN COMMIT ĐẦU NHÁNH CHÍNH

## 1. Tóm tắt

Thêm `scripts/main-guard-status.sh`. Script đi ngược lịch sử first-parent từ commit đầu nhánh chính, bỏ qua commit chỉ đổi tài liệu, dừng ở commit đầu tiên có lần chạy `main-guard` hay có đổi code.

- Script trả "xanh" khi commit đó có lần chạy `guard` xanh. Mọi trường hợp còn lại (đang chạy, đỏ, có code mà chưa có lần chạy nào) trả lý do và việc phải làm.
- `start-run.yml` gọi script trước khi kích hoạt routine, và phiên dùng cùng script ở kiểm tra đầu phiên.
- `main-guard.yml` thêm `workflow_dispatch`, để người (hay phiên) chạy được `main-guard` cho commit không có sự kiện `push` (L-054).

Test với `gh` giả, viết trước; tái hiện đúng trường hợp c49a682/c83777a của RUN-20261023-03.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là bài học L-055 (`docs/lessons.md`), cùng L-054 (vì sao có commit không có lần chạy).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| L-055 | Khi commit đầu nhánh chính đổi file ngoài tài liệu mà chưa có lần chạy `main-guard` xanh cho chính commit đó thì `start-run.yml` không kích hoạt routine | "Ngoài tài liệu" theo đúng `paths-ignore` của `main-guard.yml`: không thuộc `docs/**` và không có đuôi `.md`. Giống `docs_only` của `find-breaking-commit.sh` |
| L-055 | Commit đầu chỉ đổi tài liệu thì xét commit gần nhất có đổi code | Đi ngược first-parent: commit không có lần chạy và chỉ đổi tài liệu thì bỏ qua. Commit có lần chạy (kể cả commit tài liệu được chạy bằng `workflow_dispatch`) thì lấy kết quả của lần chạy đó, vì nó kiểm cả code của các commit trước |
| L-055 | Từ chối thì in hướng dẫn chạy `main-guard` | Hướng dẫn: Actions, `main-guard`, Run workflow trên `main`, hoặc `gh workflow run main-guard.yml --ref main`, rồi kích hoạt lại phiên. Cần thêm `workflow_dispatch` vào `main-guard.yml` (hiện chỉ có `push`), xem mục 8, câu 1 |
| L-055 | `main-guard` đang chạy cho commit đó thì không bắt đầu | Từ chối, in "đang chạy, chờ xong rồi kích hoạt lại". Không chờ trong workflow (giới hạn 10 phút của job) |
| Hành vi cũ | Các kiểm tra hiện có của `start-run.yml` giữ nguyên | Bảng ở mục 3.1 |

Thiết kế tham chiếu: Không có thiết kế riêng (script và workflow, docs/ops/design-workflow.md).

ADR liên quan: ADR-0010 (cổng chất lượng), ADR-0016 (kích hoạt phiên bằng routine), ADR-1002 (nhánh chính tự hồi phục).

## 3. Giải pháp

### 3.1. Cách làm

**`scripts/main-guard-status.sh [<sha>]`** (mặc định `origin/main` sau `git fetch`; trong workflow là `GITHUB_SHA`). Theo mẫu `find-breaking-commit.sh`, chỉ dùng `gh api`:

- `guard_state <sha>`: lần chạy check `guard` mới nhất của commit, trả `success`, `failure`, `pending` hay `none`. Hàm giống hệt hàm cùng tên của `find-breaking-commit.sh`. Plan không tách ra file chung, để không sửa `find-breaking-commit.sh` (mục 8, câu 3).
- `docs_only <sha>`: mọi file đổi đều thuộc `docs/` hoặc có đuôi `.md`.
- Vòng lặp, tối đa `MAX_DEPTH` (mặc định 30) commit:
  - `success`: in `XANH: main-guard qua trên <sha ngắn> (<lần chạy>)`, thoát 0.
  - `pending`: in `CHỜ: main-guard đang chạy trên <sha>. Chờ xong rồi kích hoạt lại.`, thoát 2.
  - `failure`: in `ĐỎ: main-guard không qua trên <sha>.`, thoát 1.
  - `none` và chỉ đổi tài liệu: sang commit cha thứ nhất.
  - `none` và có code: in `CHƯA KIỂM: <sha> có thay đổi code nhưng main-guard chưa chạy (GitHub có thể đã không phát sự kiện push, L-054). Chạy: gh workflow run main-guard.yml --ref main, chờ xanh rồi kích hoạt lại.`, thoát 3.
- Hết `MAX_DEPTH` mà chưa gặp commit nào có lần chạy: thoát 3 với cùng hướng dẫn.

Thông báo tiếng Việt, chú thích tiếng Anh. Trong GitHub Actions thì thêm `::error::` trước, như `java-check.sh`.

**`start-run.yml`:** thêm bước "Kiểm tra main-guard trên nhánh chính" ngay sau bước "Kiểm tra trước khi bắt đầu phiên", trước bước staging:

- chạy `scripts/main-guard-status.sh "${GITHUB_SHA}"`;
- mọi mã khác 0 đều đỏ, nên routine không được kích hoạt;
- cần `checks: read` thêm vào `permissions` (đọc check run), cùng `contents: read` đã có.

**`main-guard.yml`:** thêm `workflow_dispatch:` (không input) vào `on:`. Lần chạy bằng tay chạy trên `GITHUB_SHA` là commit đầu `main`, và tạo check run `guard` trên commit đó. Mọi bước khác không đổi.

**`docs/auto-run.md`, mục 2:** sửa dòng "Nhánh chính đang xanh" của bảng:

- Điều kiện: `scripts/main-guard-status.sh` thoát 0, thay cho "lần chạy gần nhất của `main-guard.yml` qua", và không có issue hay PR `main-broken`.
- Xử lý theo mã thoát:
  - mã 2 (đang chạy): chờ bằng `scripts/wait-run.sh --workflow main-guard.yml --since <giờ>`, rồi gọi lại script;
  - mã 3 (chưa kiểm): phiên chạy `gh workflow run main-guard.yml --ref main` một lần, chờ như mã 2, rồi gọi lại;
  - mã 1: kết thúc phiên, như nay.

**Kiểm tra mà trường hợp mới phải đi qua (L-044).** Trường hợp mới: commit đầu có code nhưng không có lần chạy `main-guard`. Bảng dưới là các kiểm tra hiện có và sau plan.

| Kiểm tra | Ở đâu | Trường hợp mới | Test |
| --- | --- | --- | --- |
| Đường dẫn danh sách chạy đúng dạng, có trên nhánh chính, `approved` | `start-run.yml`, bước 1 | Đi qua, không đổi; bước mới chạy sau bước này | Không đổi workflow phần này; kiểm cấu trúc: test `start-run: main-guard step runs after the run-file checks and before the routine` (mục 4) |
| Không có issue `main-broken` | bước 1 | Đi qua (không có issue vì `main-guard` chưa chạy). Đây chính là khe hở của L-055 | `main-guard-status: code commit without a run is refused` |
| Không có issue `env-broken`, `setup-broken` | bước 1 | Đi qua, không đổi | Không đổi |
| `preflight.sh` | bước staging | Đi qua, không đổi | Không đổi |
| **Mới:** `main-guard` xanh trên commit gần nhất có code | bước mới | Bị chặn, mã 3, có hướng dẫn | Các test `main-guard-status` ở mục 4 |
| Kích hoạt routine, không thử lại | bước cuối | Không tới | Kiểm cấu trúc ở trên |
| `main-guard` push trên mọi commit có code | `main-guard.yml` | Không đổi; thêm `workflow_dispatch` | `main-guard: has workflow_dispatch and keeps the push trigger with the same paths-ignore` |
| Đóng hay mở `main-broken`, revert, chạy lại test chập chờn | `main-guard.yml` | Lần chạy bằng tay dùng cùng `GITHUB_SHA` và cùng các bước; `find-breaking-commit.sh` đi từ `GITHUB_SHA` như cũ | Không đổi code của các bước; mục 8, câu 2 |
| Phiên: nhánh chính xanh ở kiểm tra đầu phiên | `auto-run.md` mục 2 | Bị chặn như workflow; phiên tự chạy `main-guard` một lần (mã 3) thay vì kết thúc ngay | Tài liệu; kiểm ở phiên sau |

Kiểm tra lúc lập plan (2026-10-24):

- `gh run list --workflow main-guard.yml` không có lần chạy nào cho c49a682 hay c83777a.
- `main-guard.yml` chỉ có `on: push` với `paths-ignore: ['docs/**', '**/*.md']`.
- `find-breaking-commit.sh` đã xử lý "không có lần chạy, chỉ tài liệu" như trên.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/main-guard-status.sh` | Mục 3.1. Bước 3 tạo khung rỗng (in `XANH`, thoát 0) để test thất bại đúng lý do |
| Sửa | `.github/workflows/start-run.yml` | Bước mới, `checks: read` (mục 3.1) |
| Sửa | `.github/workflows/main-guard.yml` | Chỉ thêm `workflow_dispatch:` vào `on:`; một dòng chú thích đầu file |
| Sửa | `scripts/tests/run.sh` | Khối test mới `main-guard-status` (mục 4), đặt ngay sau khối `find-breaking-commit.sh`, dùng cùng `fake-gh` và dạng fixture `{"<sha>": {"parent", "files", "states"}}` (đã có, không sửa `fake-gh`). Hai test cấu trúc đọc file workflow như test `status-page` hiện có. Tất cả vào commit test của bước 3 |
| Sửa | `docs/auto-run.md` | Chỉ dòng "Nhánh chính đang xanh" của bảng mục 2 (mục 3.1) |
| Sửa | `docs/ops/staging-and-runs.md` | Mục 2.3 (Bắt đầu một phiên): `start-run` từ chối khi `main-guard` chưa xanh, cách chạy `main-guard` bằng tay; mục 2.7 (mở bằng `claude --cloud`): thêm `scripts/main-guard-status.sh` vào các kiểm tra người tự làm |
| Sửa | `docs/lessons.md` | L-055: dòng Áp dụng ghi T-041 khi merge |
| Sửa | `docs/features/quality-gate/tickets/T-041-start-run-kiem-main-guard.md` | Trạng thái theo phiên |

Nơi khác đọc file bị đổi (`grep -rn "main-guard.yml\|start-run.yml" scripts/ .github/ docs/ops`):

- `scripts/check.sh` chỉ nhắc `main-guard.yml` trong chú thích.
- `scripts/merge-if-green.sh` đọc issue `main-broken`, không đọc workflow.
- `scripts/wait-run.sh --workflow main-guard.yml` lọc lần chạy theo `created_at`, không theo sự kiện, nên lần chạy bằng tay cũng được tính.
- `scripts/failure-kind.sh` và `scripts/find-breaking-commit.sh` do `main-guard.yml` gọi, không đổi.
- `docs/ops/release.md` nhắc `main-guard`, không đổi.
- `env-canary.yml`, `resume-run.yml` không gọi `start-run.yml`.

Nơi khác nhắc tới file mà plan sửa, không bị ảnh hưởng (L-020):

- `.github/workflows/preflight.yml`, `.github/workflows/resume-run.yml`, `scripts/env-canary.sh`, `scripts/preflight.sh` nhắc `.github/workflows/start-run.yml` trong chú thích, vì `start-run.yml` cũng kiểm nhãn issue mà chúng mở, hay vì `resume-run.yml` thay `start-run.yml` khi tiếp tục phiên. Plan không đổi đầu vào `run_file`, nhãn hay bước nào hiện có của `start-run.yml`. `resume-run.yml` kích hoạt lại routine trực tiếp, không qua bước mới; phiên tiếp tục không chạy lại kiểm tra đầu phiên (`auto-run.md`, mục 2).
- `scripts/release.sh` có nhắc `.github/workflows/main-guard.yml`. AI không đọc file này (ADR-0009, CLAUDE.md). Plan chỉ thêm một cách kích hoạt, giữ tên workflow, tên job `guard` và mọi bước, nên một lần chạy bằng tay giống lần chạy `push` ở mọi chỗ đọc kết quả theo commit. Còn lại phải người kiểm, mục 8, câu 2.
- Các workflow và script chỉ nhắc `docs/auto-run.md` hay `docs/ops/staging-and-runs.md` theo số mục, trong chú thích hay thông báo: `.github/workflows/device-session.yml`, `.github/workflows/env-canary.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/staging.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/env-canary.sh`, `scripts/resume-check.py`, `scripts/status.py`. Plan chỉ sửa một dòng của bảng mục 2 của `auto-run.md` và nội dung mục 2.3, 2.7 của `staging-and-runs.md`, không đổi số mục.
- `scripts/status.py` đọc `docs/lessons.md` để đếm bài học theo dòng Trạng thái. Plan chỉ thêm chữ vào dòng Áp dụng của L-055.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm (quyền của app). Quyền của workflow: `start-run.yml` thêm `checks: read`
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có.

## 4. Kiểm thử

Commit trong fixture: A=aaaaaa1, B=bbbbbb2, C=ccccc03, D=ddddd04 (D là đầu `main`), như khối `find-breaking-commit.sh`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Đầu có code, có lần chạy xanh | Test script | `main-guard-status: green run on the head passes`: D `states: ["success"]`; thoát 0, in `XANH`. Đối chứng cho các test từ chối |
| L-055, trường hợp c49a682/c83777a | Test script | `main-guard-status: code commit without a run is refused`: D chỉ đổi tài liệu, không có lần chạy; C có `scripts/run-check.py`, không có lần chạy; thoát 3, in `CHƯA KIỂM` và `gh workflow run main-guard.yml`. Thất bại trên khung rỗng (thoát 0) |
| Đầu chỉ tài liệu, commit code trước đó xanh | Test script | `main-guard-status: docs-only head after a green code commit passes`: D chỉ tài liệu, không có lần chạy; C xanh; thoát 0, in `ccccc03` |
| Lần chạy bằng tay trên commit tài liệu | Test script | `main-guard-status: a run on a docs-only head counts`: D chỉ tài liệu, `states: ["success"]`; C không có lần chạy; thoát 0 (không đi tiếp tới C) |
| Đang chạy | Test script | `main-guard-status: pending run asks to wait`: D `states: ["pending"]`; thoát 2, in `CHỜ` |
| Đỏ | Test script | `main-guard-status: red run is refused`: D `states: ["failure"]`; thoát 1, in `ĐỎ` |
| Không tìm thấy commit nào có lần chạy | Test script | `main-guard-status: stops after MAX_DEPTH`: `MAX_DEPTH=2`, D và C chỉ tài liệu, không có lần chạy; thoát 3 |
| `start-run.yml` gọi bước mới đúng chỗ | Test script (cấu trúc) | `start-run: main-guard step runs after the run-file checks and before the routine`: thứ tự dòng của `scripts/main-guard-status.sh` trong `start-run.yml` sau `main-broken` và trước `Kích hoạt routine`; `permissions` có `checks: read` |
| `main-guard.yml` chạy được bằng tay, push giữ nguyên | Test script (cấu trúc) | `main-guard: has workflow_dispatch and keeps the push trigger with the same paths-ignore`: có dòng `workflow_dispatch:` dưới `on:`; `paths-ignore` vẫn đúng hai mẫu `docs/**`, `**/*.md` |
| Workflow chạy thật | Kiểm tra thủ công sau merge | (1) Ngay sau merge: chạy `start-run` với một danh sách chạy `draft` giả, thấy đỏ ở bước cũ (trạng thái), không tới routine. (2) Chạy `gh workflow run main-guard.yml --ref main`, thấy một lần chạy `workflow_dispatch` xanh trên commit đầu. (3) Lần kích hoạt phiên kế tiếp: bước mới in `XANH` |

**Ngoại lệ viết test trước:** sửa `start-run.yml`, `main-guard.yml` không chạy được trong test cục bộ. Hai test cấu trúc ở trên viết trước và thất bại trên file hiện tại; phần chạy thật kiểm thủ công sau merge.

### 4.1. Chạy thử

Không cần (E dưới 2): `gh api` check run và lịch sử first-parent đã dùng trong `find-breaking-commit.sh`; `workflow_dispatch` đã dùng trong `start-run.yml`, `staging.yml`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI, cổng chất lượng): sửa `.github/workflows/start-run.yml` và `main-guard.yml`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script mới, hai workflow (một bước, một dòng), tài liệu |
| B. Độ rõ của yêu cầu | 1 | Rõ; cần thêm `workflow_dispatch` ngoài ticket (mục 8, câu 1) |
| C. Khả năng kiểm chứng tự động | 1 | Script có test đủ nhánh; workflow chỉ kiểm cấu trúc, chạy thật kiểm tay |
| D. Ảnh hưởng chéo | 2 | Mọi lần bắt đầu phiên; `main-guard.yml` là cổng của nhánh chính |
| E. Độ mới của kỹ thuật | 0 | Theo mẫu `find-breaking-commit.sh` |
| **Tổng** | 5 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `start-run.yml`, `main-guard.yml`, `scripts/tests/run.sh` (khối mới riêng), `docs/auto-run.md` (một dòng của mục 2).
- Task khác có thể xung đột:
  - T-036 sửa dòng `cloud-env-check.sh` của cùng bảng mục 2. T-037, T-038, T-039, T-040 sửa các bước 3, 4, 7, 8, 9 của `auto-run.md`. Chỉ xung đột dòng khi gộp.
  - T-040 (`merge-if-green.sh` nhận ra đã merge) ghi trong plan của nó "chạy `main-guard` bằng `workflow_dispatch`", nên cần `workflow_dispatch` mà plan này thêm. T-040 nên chạy sau T-041, hoặc plan T-040 ghi việc thêm dòng đó: một plan ghi Sửa, plan kia dùng lại (L-033). Ghi để người lập danh sách chạy xếp.
  - Mỗi plan thêm khối test riêng vào `scripts/tests/run.sh`.
- Đề xuất thứ tự chạy: trước T-040. Không phụ thuộc T-036.

## 7. Điều kiện dừng riêng

- Trên phiên, chính bước kiểm tra đầu phiên với script mới báo `CHƯA KIỂM` hay `ĐỎ` trên commit mà `main-guard` đã xanh (kiểm báo sai). Dừng task, không merge, ghi đầu ra vào nhật ký.

## 8. Câu hỏi cho người duyệt

1. **Thêm `workflow_dispatch` vào `main-guard.yml`.** Ticket không ghi. Nhưng `main-guard.yml` hiện chỉ chạy khi có `push`, nên với commit không có sự kiện `push` (L-054) không ai chạy lại được `main-guard`; hướng dẫn "chạy `main-guard`" sẽ không làm được. Đề xuất của AI: thêm, trong plan này.
2. **Lần chạy bằng tay và ADR-1002.** Chạy bằng tay trên commit tài liệu ở đầu `main`, mà kết quả đỏ, thì `find-breaking-commit.sh` vẫn tìm commit gây lỗi từ đầu đó trở về, như với lần chạy `push`. Lần chạy xanh thì đóng issue `main-broken` theo cùng quy tắc (commit chứa commit bị đổ lỗi). Đề xuất của AI: không đổi bước nào khác của `main-guard.yml`, ghi điều này vào chú thích đầu file. Người duyệt xem giúp `scripts/release.sh` (AI không đọc) có lọc lần chạy `main-guard` theo sự kiện `push` không; nếu có, lần chạy bằng tay không được nó tính, nhưng cũng không làm nó sai.
3. **Hàm `guard_state` chép từ `find-breaking-commit.sh`.** Tách ra file chung thì phải sửa `find-breaking-commit.sh` (cổng chất lượng). Đề xuất của AI: chép, ghi chú thích "giữ giống `find-breaking-commit.sh`"; gộp lại khi một plan khác cần sửa file đó.
4. **Phiên tự chạy `main-guard` (mã 3).** Đề xuất của AI: phiên được chạy `gh workflow run main-guard.yml --ref main` một lần ở kiểm tra đầu phiên, vì đó là kiểm tra chỉ đọc nhánh chính, không merge gì. Muốn chỉ người chạy thì đổi dòng `auto-run.md` thành "kết thúc phiên, báo cáo".

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-24, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Câu 2 (scripts/release.sh có lọc lần chạy main-guard theo sự kiện push không): Tuấn xem release.sh trước khi task chạy, ghi trong danh sách chạy.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
