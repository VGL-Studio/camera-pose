---
ticket: T-029
title: merge-if-green.sh tự cập nhật nhánh và chịu được job kẹt trạng thái
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

# PLAN T-029: MERGE-IF-GREEN.SH TỰ CẬP NHẬT NHÁNH VÀ CHỊU ĐƯỢC JOB KẸT TRẠNG THÁI

## 1. Tóm tắt

Ba thay đổi nhỏ ở `scripts/merge-if-green.sh`, mỗi thay đổi có test với `scripts/tests/fake-gh`:
- `--update`: nhánh chậm hơn nhánh chính thì gọi `PUT pulls/<số>/update-branch` rồi trả mã 2 ("chờ CI rồi gọi lại"), thay vì mã 3;
- check run còn `queued` hay `in_progress` mà check suite của nó đã `completed` và `success` thì coi là xanh;
- commit cuối của nhánh mới đẩy dưới 3 phút thì in cảnh báo kèm giờ đẩy.

Plan chạy trong phiên có người theo dõi, không đưa vào danh sách chạy: nó đổi cổng chất lượng mà phiên dựa vào.

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là bài học L-017 và L-018 (`docs/lessons.md`, `accepted`).

| Yêu cầu | Tiêu chí | Cách hiểu của AI |
| --- | --- | --- |
| L-017, cập nhật nhánh | Khi gọi `merge-if-green.sh <số> --update` và nhánh chậm hơn nhánh chính thì nhánh được cập nhật và script trả mã 2 | Dùng API `update-branch` của GitHub (gộp nhánh chính vào nhánh PR, có `expected_head_sha` là commit đã kiểm). Không chờ CI trong cùng lệnh; người gọi chờ bằng `scripts/wait-run.sh --pr <số>` rồi gọi lại |
| L-017, không `--update` | Khi không có `--update` thì giữ như cũ: mã 3 | Hành vi cũ không đổi |
| L-017, job kẹt | Khi check run bắt buộc chưa `completed` nhưng check suite chứa nó đã `completed` với `success` thì coi check run đó là xanh | Chỉ khi suite `success`; suite `failure` hay chưa xong thì như cũ |
| L-018, commit cuối | Khi commit cuối của nhánh được đẩy dưới 3 phút trước thì script in "CHÚ Ý" kèm mã commit và giờ, vẫn làm tiếp | Chỉ cảnh báo, không chặn (câu hỏi 2). Mã commit sẽ merge đã được in sẵn ở dòng SẴN SÀNG và ĐÃ MERGE |

Thiết kế tham chiếu: không có thiết kế riêng.

ADR liên quan: ADR-0010 (cổng chất lượng), ADR-1002 (ngoại lệ bản revert).

## 3. Giải pháp

### 3.1. Cách làm

**Tham số.** Thứ hai được phép là `--dry-run` hoặc `--update`, như cách kiểm chặt hiện có: mọi giá trị khác bị từ chối trước khi làm gì.

**Các kiểm tra hiện có và đường đi mới (L-044).** Theo thứ tự trong script:

| Kiểm tra hiện có | `--update` | Job kẹt | Commit mới đẩy |
| --- | --- | --- | --- |
| Tham số | Nhận thêm `--update` | Không đổi | Không đổi |
| PR mở, không nháp, không `needs-human`, nhắm `main` | Vẫn chặn trước khi cập nhật | Không đổi | Không đổi |
| `main-broken` mở (trừ bản revert đúng, ADR-1002) | Vẫn chặn trước khi cập nhật. Bản revert đúng luôn nằm ngay trên nhánh chính (`is-exact-revert.sh`), nên không bao giờ chậm và không bị cập nhật | Không đổi | Không đổi |
| Phiên đang chạy: chỉ PR của phiên | Vẫn chặn trước khi cập nhật | Không đổi | Không đổi |
| Nhánh chậm hơn nhánh chính (mã 3) | **Mới:** gọi `update-branch`, mã 2 | Không đổi | Không đổi |
| Có check thất bại | Không tới (đã trả mã 2) | Không đổi: check `completed` với `failure` vẫn chặn | Không đổi |
| `check_one` cho từng check bắt buộc | Không tới | **Mới:** chưa `completed` thì xem check suite | Không đổi |
| `--dry-run` dừng ở SẴN SÀNG | `--update` không đi cùng `--dry-run` | Không đổi | **Mới:** cảnh báo in trước dòng SẴN SÀNG |
| Merge ghim `sha` | Không tới | Không đổi | Cảnh báo in trước khi merge |

**`--update`.** Ở đoạn `BEHIND_BY != 0`:
```bash
if [[ "$UPDATE" == "yes" ]]; then
  gh api -X PUT "repos/$REPO/pulls/$PR/update-branch" -f expected_head_sha="$HEAD_SHA" >/dev/null \
    || die "GitHub không cập nhật được nhánh '$HEAD_REF' (xung đột với nhánh chính?). Cập nhật bằng tay."
  wait_ "đã cập nhật nhánh '$HEAD_REF' theo '$BASE_BRANCH'. Chờ CI của commit mới (scripts/wait-run.sh --pr $PR) rồi gọi lại."
fi
```
Xung đột thì GitHub trả lỗi 422 và script từ chối (mã 1), không tự gộp.

**Job kẹt.** Trong `check_one`, khi `status != completed`:
```bash
suite="$(jq -r '.check_suite.id // empty' <<<"$latest")"
if [[ -n "$suite" ]] && gh api "repos/$REPO/check-suites/$suite" --jq '[.status, .conclusion] | join(" ")' \
    | grep -qx 'completed success'; then
  echo "CHÚ Ý: kiểm tra '$name' còn ghi '$status' nhưng lần chạy chứa nó đã xong và xanh; coi là xanh." >&2
  return 0
fi
```
Chạy thử ngày 2026-10-23 (mục 4.1): check run của `pr-check.yml` có `check_suite.id`, và `check-suites/<id>` trả `status`, `conclusion`.

**Commit mới đẩy.** Trước dòng SẴN SÀNG: đọc `commits/$HEAD_SHA` lấy `.commit.committer.date`, so với giờ hiện tại (`date -u +%s`; giờ dạng ISO đổi bằng `jq` `fromdateiso8601`); dưới 180 giây thì in `CHÚ Ý: commit cuối ${HEAD_SHA:0:7} của '$HEAD_REF' đẩy lúc <giờ>, chưa tới 3 phút. Còn commit sắp đẩy thì chờ, vì commit đẩy sau khi merge không vào nhánh chính (L-018).`. Biến `MERGE_IF_GREEN_NOW` thay giờ hiện tại trong test.

**Tài liệu.**
- `docs/auto-run.md` bước 9: "Script báo nhánh chưa cập nhật" đổi thành gọi `scripts/merge-if-green.sh <số> --update`, chờ `wait-run.sh --pr`, gọi lại.
- Chú thích đầu `merge-if-green.sh`: cách dùng và mã thoát mới.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/merge-if-green.sh` | `--update`, xét check suite, cảnh báo commit mới đẩy |
| Sửa | `scripts/tests/run.sh` | Test ở mục 4 |
| Sửa | `scripts/tests/fake-gh` | Khóa `fail_writes` của fixture: lệnh ghi có trong danh sách thì trả mã 1 (vẫn ghi vào `$FIXTURE.writes`) |
| Sửa | `docs/auto-run.md` | Bước 9 dùng `--update` |
| Sửa | `docs/features/quality-gate/plans/T-029-merge-if-green-tu-cap-nhat.md` | Nhật ký thực hiện |
| Sửa | `docs/features/quality-gate/tickets/T-029-merge-if-green-tu-cap-nhat.md` | Trạng thái |
| Sửa | `docs/lessons.md` | Dòng Áp dụng của L-017, L-018 |
| Ghi lại | `docs/STATUS.md` | `scripts/status.py` |

`scripts/tests/fake-gh`: route cố định phục vụ được `check-suites/<id>` và `commits/<sha>`; chỉ thêm `fail_writes`, các test hiện có không dùng khóa này nên không đổi. `scripts/wait-run.sh` không đổi.

Các file sau nhắc tới file mà plan sửa nhưng không bị ảnh hưởng (L-020): `scripts/release.sh` không gọi `merge-if-green.sh`; `.github/workflows/device-session.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/device-session.sh`, `scripts/resume-check.py` chỉ nhắc `docs/auto-run.md` hay `scripts/tests/run.sh` trong chú thích hoặc thông báo. `.github/workflows/main-guard.yml` và `.github/workflows/pr-check.yml` chỉ nhắc tên `merge-if-green.sh` và `docs/auto-run.md` trong chú thích hay nội dung issue, không gọi script. `scripts/is-exact-revert.sh` được `merge-if-green.sh` gọi với ba tham số như cũ; plan không đổi đoạn gọi đó.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** API `update-branch` trong môi trường `claude --cloud` (proxy GitHub chặn GraphQL; API này là REST nên dự kiến qua, chưa thử vì nó đổi một PR thật).
- **Điều kiện chuyển:** lần đầu phiên gọi `--update` thì `gh api -X PUT .../update-branch` trả 403 từ proxy.
- **Cách làm thay:** script in hướng dẫn cũ (cập nhật bằng `git merge origin/main` rồi đẩy) và trả mã 3 như trước; test ở mục 4 giữ nguyên với lỗi giả 403.

## 4. Kiểm thử

Test trong `scripts/tests/run.sh`, viết trước code, dùng `scripts/tests/fake-gh` và hàm dựng fixture như `merge_during_run`.

| Tiêu chí | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Tham số | Test script | Thêm `12 --update extra`, `12 --updat`, `12 --update --dry-run` vào vòng lặp `merge-if-green rejects '<args>'` hiện có |
| L-017, `--update` khi chậm | Test script | `merge-if-green --update updates a branch behind main`: fixture `compare` có `behind_by` 2; mã 2; `$FIXTURE.writes` có dòng `PUT pulls/12/update-branch` |
| L-017, không `--update` | Test script | Các test `merge_during_run` hiện có vẫn trả mã 3, `writes` không có `update-branch` |
| L-017, `--update` khi GitHub từ chối | Test script | `merge-if-green --update refuses when GitHub cannot update`: fixture có `"fail_writes": ["PUT pulls/12/update-branch"]`, `fake-gh` trả mã 1 cho lệnh ghi đó; mã 1 và thông báo "không cập nhật được nhánh" |
| L-017, `--update` không vượt chặn cũ | Test script | `merge-if-green --update still refuses a person's PR while a run is running` (mã 1, `writes` không có `update-branch`); `--update still refuses while main is broken` (mã 1) |
| L-017, job kẹt, suite xanh | Test script | `merge-if-green treats a stuck check as green when its suite succeeded`: nhánh không chậm (`behind_by` 0), check run `changes` `in_progress` với `check_suite.id` 77, route `check-suites/77` là `completed success`, các check khác `completed success`; `--dry-run` trả mã 0 và in `CHÚ Ý: kiểm tra 'changes'` |
| L-017, job kẹt, suite chưa xong | Test script | `merge-if-green waits for a check whose suite is still running`: suite `in_progress`; mã 2 |
| L-017, suite đỏ | Test script | `merge-if-green does not trust a failed suite`: suite `completed failure`; mã khác 0 |
| L-018, commit mới đẩy | Test script | `merge-if-green warns about a commit pushed a minute ago`: `commits/<sha>` có `committer.date` 60 giây trước `MERGE_IF_GREEN_NOW`; `--dry-run` mã 0, có dòng `CHÚ Ý: commit cuối`; `does not warn about an older commit` (10 phút): không có dòng đó |
| Không đổi hành vi cũ | Test script | Mọi test `merge-if-green` hiện có (tham số, phiên đang chạy, nhãn, bản revert đúng của ADR-1002) qua mà không sửa |
| `--update` trên GitHub thật | Kiểm tra thủ công | Lần đầu dùng: một PR của người có nhánh chậm 1 commit; `scripts/merge-if-green.sh <số> --update` cập nhật nhánh, CI chạy lại, gọi lại thì merge. Ghi vào nhật ký |

**Ngoại lệ viết test trước:** `docs/auto-run.md` là tài liệu, không có test.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-23, chỉ đọc, trên PR #47 đã merge (commit e00cd7d): `gh api repos/VGL-Studio/camera-pose/commits/e00cd7d.../check-runs` trả ba check run cùng `check_suite.id` 108152535158; `gh api .../check-suites/108152535158 --jq '[.status, .conclusion]'` trả `completed success`.
- **Chưa thử:** `PUT pulls/<số>/update-branch`, vì lệnh này đổi một PR thật; thử ở bước kiểm tra thủ công cuối mục 4, có phương án dự phòng ở mục 3.4. `scripts/check.sh` không cần chạy thử: plan không đổi code của app; CI của PR vẫn chạy `check.sh`.
- **Lệnh:** `scripts/tests/run.sh` sau khi viết test.
- **Kết quả:** API check suite đọc được bằng REST, đúng dạng cần cho `check_one`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI, quy trình phiên tự chạy): sửa `scripts/merge-if-green.sh` (CLAUDE.md, mục Không được làm; ADR-0010).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script, test, một đoạn tài liệu |
| B. Độ rõ của yêu cầu | 1 | L-017, L-018 nói rõ; còn câu hỏi về chặn hay cảnh báo |
| C. Khả năng kiểm chứng tự động | 1 | Mọi đường đi có test với `gh` giả; API `update-branch` thật chỉ kiểm tay |
| D. Ảnh hưởng chéo | 3 | Đổi cổng chất lượng mà mọi PR và mọi phiên dựa vào |
| E. Độ mới của kỹ thuật | 1 | REST API của GitHub đã dùng khắp script; hai endpoint mới |
| **Tổng** | 7 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/merge-if-green.sh`, `docs/auto-run.md` bước 9.
- Task khác có thể xung đột: T-030 (kiểm tra trước phiên) cũng sửa `docs/auto-run.md` và `scripts/tests/run.sh`; làm lần lượt.
- Đề xuất thứ tự chạy: phiên có người theo dõi, không trong danh sách chạy; trước hay sau T-030 đều được.

## 7. Điều kiện dừng riêng

1. Một test `merge-if-green` hiện có phải sửa điều nó kiểm tra mới qua được.
2. Thêm `fail_writes` vào `fake-gh` làm một test hiện có đổi kết quả.

## 8. Câu hỏi cho người duyệt

1. **`--update` có tự merge luôn sau khi CI xanh không?** Đề xuất của AI: không. Lệnh của phiên chỉ chạy được khoảng 10 phút, CI mất 8 đến 13 phút; script trả mã 2 và người gọi chờ bằng `wait-run.sh` như hiện nay.
- Trả lời: Đồng ý với đề xuất của AI.
2. **Commit mới đẩy: cảnh báo hay chặn?** Đề xuất của AI: chỉ cảnh báo. Commit đẩy sau khi merge mới là vấn đề của L-018, và script không thấy được commit chưa đẩy; chặn 3 phút chỉ làm chậm mọi lần merge của phiên.
- Trả lời: Đồng ý với đề xuất của AI.
3. **Phiên có được dùng `--update` không?** Đề xuất của AI: có, ở bước 9; nhánh của phiên đã được tạo từ nhánh chính mới nhất, nên `--update` chỉ dùng khi nhánh chính đi trước trong lúc task chạy (thường do bản revert của ADR-1002 hay PR báo cáo).
- Trả lời: Đồng ý với đề xuất của AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** 

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### 2026-10-23, phiên có người theo dõi (Tuấn)

- **Bước 3, test viết trước** (commit c2ded0b): 21 test mới trong `scripts/tests/run.sh` và khóa `fail_writes` của `scripts/tests/fake-gh`. Trên code cũ có 10 test đỏ, đúng lý do: `--update` bị từ chối là tham số lạ, không có `update-branch`, không có dòng `CHÚ Ý`, job kẹt làm script chờ. Ba test tham số mới (`12 --update extra`, `12 --updat`, `12 --update --dry-run`) và test suite đỏ đã xanh trên code cũ, như mong đợi: chúng giữ hành vi từ chối.
- **Bước 4, code:** `scripts/merge-if-green.sh` theo mục 3.1; `docs/auto-run.md` bước 9. Hai chỗ chỉnh khi chạy test:
  - `fake-gh` đọc đường dẫn API ở tham số đầu tiên sau `api`. Lệnh `update-branch` vì vậy viết đường dẫn trước `-X PUT`, như `scripts/env-canary.sh`. Lệnh merge và xóa nhánh có sẵn vẫn để `-X` trước; không test nào chạy tới đó.
  - Hàm `update_writes` của test in `0` khi chưa có file `writes`, thay vì in rỗng. Đây là lỗi của hàm phụ trợ, không đổi điều test kiểm tra.
- **Bước 5:** `scripts/tests/run.sh` qua 271 test. `scripts/check.sh` để CI của PR chạy, vì plan không đổi code của app (mục 4.1).
- **Còn lại:** kiểm tay `--update` trên GitHub thật ở lần đầu dùng (mục 4, dòng cuối). Sau khi merge, ghi kết quả vào đây.

### 2026-10-23, sau khi merge

- PR #49 merge bằng `merge-if-green.sh` (commit 1024974 trên nhánh chính).
- main-guard 39916343646 trên 1024974: lần chạy đầu GitHub không cấp máy (hết hạn mức Actions, L-046), chạy lại sau khi người nâng hạn mức thì xanh.
- `--update` trên GitHub thật, hai lần (PR #51, PR #52): GitHub trả `422 merge conflict` vì `docs/STATUS.md`; script từ chối với mã 1 và hướng dẫn gộp tay, đúng mục 3.1. Đường cập nhật thành công chưa gặp, vì PR nào cũng xung đột ở trang tiến độ (L-047). Không cần phương án dự phòng 3.4: proxy không chặn, lỗi là xung đột thật.
- Ba lần merge sau (#50, #51, #52) qua script mới không có cảnh báo hay lỗi.
