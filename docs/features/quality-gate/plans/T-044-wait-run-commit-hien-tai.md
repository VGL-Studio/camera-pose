---
ticket: T-044
title: wait-run.sh --pr chỉ nhận lần chạy của commit đầu hiện tại
status: draft                # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.3M         # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-044: WAIT-RUN.SH --PR CHỈ NHẬN LẦN CHẠY CỦA COMMIT ĐẦU HIỆN TẠI

## 1. Tóm tắt

Ở chế độ `--pr`, `scripts/wait-run.sh` đọc lại commit đầu của PR ở mỗi vòng chờ thay vì chỉ một lần lúc bắt đầu. Script chỉ nhận lần chạy `pr-check` của đúng commit đó.

- Lần chạy của commit cũ bị hủy (`cancelled`) vì có commit mới thì không còn bị báo `ĐỎ`: script chuyển sang chờ commit mới.
- Tham số mới `--sha <commit>` cho phiên chờ đúng commit vừa đẩy, kể cả khi GitHub chưa kịp cập nhật PR.

Kiểm chứng bằng test của script với `gh` giả. Fixture dựng theo dữ liệu thật của PR #100 (L-066).

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là bài học L-066 (`docs/lessons.md`, `accepted`).

| Yêu cầu | Tiêu chí | Cách hiểu của AI |
| --- | --- | --- |
| Theo commit đầu hiện tại | Khi commit đầu của PR đổi trong lúc chờ, `wait-run.sh --pr` chờ lần chạy của commit mới và báo kết quả của commit mới; dòng kết quả ghi mã commit mới | Mỗi vòng đọc `pulls/<số>` một lần, lấy `head.sha` và `mergeable_state` cùng lúc. Vòng đã có lần chạy (đang chạy) tốn thêm một lượt gọi so với hiện nay; mỗi vòng cách 30 giây nên không đáng kể. Vòng chưa có lần chạy vẫn một lượt như hiện nay (đã đọc `mergeable_state`) |
| `cancelled` của commit cũ không phải đỏ | Lần chạy `cancelled` của một commit không còn là commit đầu của PR thì coi như chưa có lần chạy; không in `ĐỎ`, không thoát mã 1 | Ở PR #100 (mục 4.1), lần chạy 40066418977 của `e35ae86` bị hủy lúc 05:32:16, sau khi `eba5f98` được đẩy (05:31:27) và lần chạy mới 40066593456 đã tạo (05:31:35). GitHub hủy lần chạy cũ khi đã có commit mới, nhưng `pulls/<số>` có thể trả commit cũ thêm vài giây. Vì vậy, khi gặp `cancelled` của commit đang là đầu, script đọc lại commit đầu tối đa `WAIT_RUN_CANCEL_RECHECKS` lần (mặc định 3, mỗi lần cách một vòng chờ) trước khi kết luận `ĐỎ (cancelled)` |
| `cancelled` thật vẫn là đỏ | Lần chạy của chính commit đầu bị hủy, và commit đầu không đổi sau các lần đọc lại, thì vẫn báo `ĐỎ (cancelled)`, mã 1, như hiện nay | Người hủy tay, hay job hết giờ kiểu `cancelled`: phiên đọc log như mọi lần đỏ |
| Chờ đúng commit vừa đẩy | `--sha <commit>`: lần chạy của commit khác (kể cả đã xong, kể cả xanh) không được nhận; commit đầu của PR chưa bằng `<commit>` thì chờ tiếp; hết giờ thì mã 2 với dòng nói PR chưa nhận commit đó | Ngay sau `git push`, `pulls/<số>` có thể còn trả commit trước. Commit trước đã xanh thì script hiện nay báo `XANH` ngay cho commit cũ. `merge-if-green.sh` còn chặn được nhờ kiểm giờ đẩy (L-018), nhưng phiên đã tin là CI xanh. `--sha` nhận mã đầy đủ hay rút gọn (7 tới 40 ký tự hex) và so theo tiền tố |
| Không đổi phần khác | Chế độ `--workflow`, mã 4 (`HẠ TẦNG`), mã 5 (`XUNG ĐỘT`) giữ nguyên hành vi; mọi test `wait-run` hiện có qua mà không sửa | Ticket ghi "Không bao gồm: chế độ `--workflow`" |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0010 (cổng chất lượng), ADR-0016 (routine).

## 3. Giải pháp

### 3.1. Cách làm

**`scripts/wait-run.sh`, chế độ `--pr`:**

- Tham số mới `--sha <commit>`, chỉ dùng cùng `--pr`. Giá trị phải khớp `^[0-9a-f]{7,40}$`, nếu không thì `LỖI`, mã 3. Dùng cùng `--workflow` thì `LỖI`, mã 3.
- Đầu script vẫn đọc `pulls/<số>` một lần để báo `LỖI` mã 3 khi không đọc được PR, như hiện nay.
- Mỗi vòng chờ gọi một hàm `read_head`:
  - đọc `gh api "repos/$repo/pulls/$pr" --jq '[.head.sha, (.mergeable_state // "")] | @tsv'`;
  - đặt `sha` và `state`;
  - lỗi đọc trong vòng chờ (mạng), hay `sha` đọc được không khớp `^[0-9a-f]{7,40}$` (ví dụ rỗng), thì giữ cả `sha` và `state` cũ và chờ tiếp, không thoát.
- Có `--sha` (biến `want`) mà `sha` chưa bắt đầu bằng giá trị đó: coi như chưa có lần chạy (`run=""`), ghi nhớ để dòng hết giờ nói rõ: `CHƯA XONG sau <n> phút: PR #<số> chưa nhận commit <7 ký tự> (commit đầu đang là <7 ký tự>). Kiểm tra lệnh đẩy, rồi gọi lại script.`, mã 2.
  - Trong lúc chờ như vậy, **không kiểm xung đột** (mã 5): `mergeable_state` đọc được thuộc commit cũ, còn commit vừa đẩy (thường là bản gộp nhánh chính để sửa xung đột) chưa được GitHub tính.
  - Commit đầu đã một lần khớp `want` rồi tiến tiếp (người hay `merge-if-green.sh --update` đẩy thêm) thì bỏ ràng buộc (`want=""`) và theo commit đầu như khi không có `--sha`.
- `query` và `what` dựng lại theo `sha` mỗi vòng (hàm `set_pr_query`), nên dòng kết quả luôn ghi commit đang được chờ.
- Lần chạy tìm được đã `completed` với `conclusion` `cancelled`:
  - nếu số lần đọc lại chưa đủ `WAIT_RUN_CANCEL_RECHECKS` (mặc định 3), tăng bộ đếm, rồi đi tiếp xuống phần kiểm hết giờ và `sleep "$poll"` của vòng (không bỏ qua `deadline`), sang vòng sau đọc lại commit đầu. Commit đầu đổi thì bộ đếm về 0 và chờ commit mới;
  - hết giờ trong lúc đang đọc lại thì mã 2, dòng riêng: `CHƯA XONG sau <n> phút: $what, lần chạy <id> bị hủy, đang kiểm commit đầu của PR. Gọi lại script.`;
  - đủ số lần mà commit đầu không đổi thì ra khỏi vòng và báo `ĐỎ (cancelled)` như hiện nay.
- Kết luận khác (`success`, `failure`, ...) giữ nguyên đường đi hiện tại: lần chạy được nhận vì nó thuộc commit vừa đọc ở chính vòng đó.
- Chú thích đầu file: thêm `--sha`, sửa dòng mã 1 thành "the run of the PR's current head finished red, or was cancelled while that commit stayed the head", thêm `WAIT_RUN_CANCEL_RECHECKS` vào dòng biến cho test.

Phác thảo vòng chờ của `--pr` (chế độ `--workflow` không đổi):
```bash
while :; do
  rechecking=0 waiting_for_push=0
  if [ -n "$pr" ]; then
    read_head                       # sha, state; keeps both old values on a read error or a sha that is not hex
    if [ -n "$want" ]; then
      if [[ "$sha" == "$want"* ]]; then seen_want=1
      elif [ "$seen_want" = 1 ]; then want=""   # the head moved on past the commit asked for: follow it
      fi
    fi
    set_pr_query                    # query, what, from $sha
    if [ -n "$want" ] && [[ "$sha" != "$want"* ]]; then
      run="" waiting_for_push=1
    else
      run="$(gh api "repos/$repo/$query" --jq "$pick | {id, status, conclusion, html_url} | @json" 2> /dev/null || true)"
    fi
  else
    run="$(...)"                    # unchanged
  fi
  if [ -n "$run" ] && [ "$(jq -r .status <<< "$run")" = "completed" ]; then
    if [ -z "$pr" ] || [ "$(jq -r .conclusion <<< "$run")" != "cancelled" ]; then
      break
    fi
    if [[ "$sha" != "$cancelled_sha" ]]; then cancelled_sha="$sha" rechecks=0; fi
    if [ "$rechecks" -ge "$max_rechecks" ]; then
      break                         # the head stayed: a real cancel, reported as ĐỎ (cancelled)
    fi
    rechecks=$((rechecks + 1)) rechecking=1
  fi
  if [ -z "$run" ] && [ -n "$pr" ] && [ "$waiting_for_push" = 0 ] && [ "$state" = "dirty" ]; then
    ...                             # XUNG ĐỘT, exit 5: as today, but not while waiting for --sha
  fi
  if [ "$(date +%s)" -ge "$deadline" ]; then
    ...                             # exit 2; own lines for rechecking=1 and waiting_for_push=1
  fi
  sleep "$poll"
done
```
Bộ đếm `rechecks` tính theo commit: commit đầu đổi thì về 0.

**`docs/auto-run.md`:**

- Bước 8, dòng "AI chờ CI bằng `scripts/wait-run.sh --pr <số PR>` cho đến khi `pr-check` kết thúc": đổi thành `scripts/wait-run.sh --pr <số> --sha "$(git rev-parse HEAD)"` sau khi chính phiên đẩy commit lên nhánh của PR.
- Bước 8, mục Chờ workflow: thêm một gạch đầu dòng sau dòng mã 5:
  - `--pr` chờ commit đầu hiện tại của PR, đọc lại mỗi vòng. Lần chạy của commit cũ bị hủy vì có commit mới thì không phải đỏ (L-066).
  - `--sha` để không nhận nhầm kết quả của commit trước khi GitHub chưa cập nhật PR (vài giây sau lệnh đẩy).
  - Sau `merge-if-green.sh --update` (commit do GitHub tạo, phiên không có mã) thì gọi không kèm `--sha`.
  - `ĐỎ (cancelled)` chỉ còn khi chính commit đầu bị hủy: tra bảng tra lỗi như mọi lần đỏ.
- Mục 3 (Khi nhánh chính đỏ vì task của phiên), bước 3: "Chờ CI bằng `scripts/wait-run.sh --pr <số>`" đổi thành `--pr <số> --sha "$(git rev-parse HEAD)"`, vì nhánh revert do chính phiên tạo và đẩy.
- Các chỗ khác gọi `wait-run.sh --pr` sau `merge-if-green.sh --update` (bước 9) giữ nguyên, không kèm `--sha`.

**`CLAUDE.md`, mục Lệnh:** dòng `wait-run.sh` thêm `[--sha <commit>]` sau `--pr <số>`.

Các kiểm tra hiện có mà trường hợp mới đi qua (L-044). Plan không đổi `merge-if-green.sh`; bảng ghi để người duyệt thấy không có đường vòng:

| Kiểm tra | Khi `wait-run.sh` chuyển sang commit mới |
| --- | --- |
| `merge-if-green.sh`: đọc commit đầu và check của chính commit đó | Không đổi; script tự đọc PR, không dùng kết quả của `wait-run.sh` |
| `merge-if-green.sh`: commit đẩy chưa tới 3 phút thì in `CHÚ Ý` (L-018) | Không đổi |
| `auto-run.md` bước 8: sửa tối đa 3 lần cho cùng một lỗi | `cancelled` của commit cũ không còn ra mã 1 nên không tính nhầm. `cancelled` thật vẫn ra mã 1 như hiện nay |
| Mã 5 (`XUNG ĐỘT`) | Dùng `mergeable_state` đọc trong cùng lần gọi `pulls/<số>` của vòng đó. Không có `--sha`: điều kiện (chưa có lần chạy và `dirty`) không đổi. Có `--sha` mà commit đầu chưa khớp: không kiểm, vì trạng thái thuộc commit cũ |

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/wait-run.sh` | `--sha`, đọc lại commit đầu mỗi vòng, đọc lại khi gặp `cancelled`, chú thích đầu file |
| Sửa | `scripts/tests/run.sh` | Test ở mục 4, trong khối `wait-run` (sau test `a run that exists is waited for even if the PR became dirty`, trước `dispatch_routes`) |
| Sửa | `docs/auto-run.md` | Bước 8: dòng "AI chờ CI bằng ..." và một gạch đầu dòng sau dòng mã 5 của mục Chờ workflow; mục 3, bước 3 (luồng revert) |
| Sửa | `CLAUDE.md` | Mục Lệnh, dòng `wait-run.sh` |
| Sửa | `docs/lessons.md` | L-066: dòng Áp dụng; câu Chuyện gì ghi đúng thứ tự theo số đo ở mục 4.1 (lần chạy của commit mới tạo trước, 05:31:35Z; lần chạy của commit cũ bị hủy sau, 05:32:16Z) |
| Sửa | `docs/features/quality-gate/plans/T-044-wait-run-commit-hien-tai.md` | Nhật ký thực hiện |
| Sửa | `docs/features/quality-gate/tickets/T-044-wait-run-commit-hien-tai.md` | Trạng thái |

Các file sau nhắc tới file mà plan sửa nhưng không bị ảnh hưởng (L-020):

- `scripts/merge-if-green.sh` nhắc `scripts/wait-run.sh --pr $PR` (thông báo mã 2 của `--update`) và `--workflow main-guard.yml --since` trong thông báo. Lời gọi không kèm `--sha` vẫn đúng: commit do GitHub tạo, và script theo commit đầu mới.
- `scripts/main-guard-status.sh` chỉ nhắc `wait-run.sh --since` (chế độ `--workflow`, không đổi).
- `docs/ops/failure-playbook.md`, dòng PB-02, PB-03, đọc mã 4, 5 của `wait-run.sh`; hai mã không đổi.
- `docs/templates/run.md` chỉ ghi "CI xong (`wait-run.sh`)" ở dòng thời gian.
- `scripts/check.sh` chạy `scripts/tests/run.sh`. Test mới chỉ dùng `gh` giả (`scripts/tests/fake-gh`, route `pulls/7` với `__sequence__` có sẵn), không cần mạng hay công cụ mới; `scripts/tests/fake-gh` không sửa.
- `.github/workflows/*.yml` không gọi `wait-run.sh`.
- Các file chỉ nhắc `docs/auto-run.md` trong chú thích hay thông báo, trỏ tới mục khác (mục 1, 2, 4, bước 3, 5, 7, bảng tra lỗi) mà plan không đổi:
  - `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`;
  - `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/resume-check.py`, `scripts/save-branch.sh`.
- Các file chỉ nhắc `CLAUDE.md` ở mục khác mục Lệnh (bước 10 về test trên emulator, quy tắc file R2, lệnh xem log): `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt`, `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt`, `scripts/device-session.sh`, `scripts/required-assets.txt`. Plan chỉ thêm `[--sha <commit>]` vào một dòng của mục Lệnh.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm.

### 3.4. Phương án dự phòng

Không có. Kỹ thuật (REST `pulls/<số>`, `actions/workflows/.../runs?head_sha=`) script đã dùng; chỉ đổi thứ tự và số lần gọi.

## 4. Kiểm thử

Test trong `scripts/tests/run.sh`, viết trước code. Test dùng hàm `wait_run` có sẵn (đặt `WAIT_RUN_POLL_SECONDS=0`, xóa `wait.json.seen`), và một hàm mới `head_routes`, dựng fixture với:

- `pulls/7` là `__sequence__` các commit đầu;
- route lần chạy riêng cho commit cũ `e35ae86` và commit mới `eba5f98` (theo PR #100, mục 4.1).

Test nào cần đọc lại khi gặp `cancelled` thì đặt `WAIT_RUN_CANCEL_RECHECKS` rõ trong lệnh.

Lưu ý khi dựng `__sequence__` của `pulls/7`: lần đọc ở đầu script (kiểm PR đọc được, mục 3.1) cũng tiêu một phần tử. Vì vậy chuỗi commit đầu luôn lặp commit cũ dư một lần so với số vòng chờ muốn thấy commit cũ.

Hàm `wait_run` có sẵn đặt cứng `WAIT_RUN_TIMEOUT_SECONDS=2` và chỉ in mã thoát cùng chữ đầu của dòng. Test kiểm nội dung dòng (mã commit, câu "chưa nhận commit", "bị hủy") dùng một hàm mới `wait_line`, như `infra_line`: ghi fixture, chạy script với cùng biến, in cả dòng kết quả, rồi `grep -c`. Khác `infra_line`, `wait_line` luôn `rm -f "$work/wait.json.seen"` trước khi chạy (để `__sequence__` của test trước không lẫn sang) và luôn đặt `WAIT_RUN_TIMEOUT_SECONDS="${2:-10}"` (không để mặc định 40 phút). Test nào kết quả không phụ thuộc hạn chờ dùng 10 giây thay cho 2 giây của `wait_run`, để máy CI chậm không làm test chập chờn (phiên bản 2). Test cần thời gian chờ khác 2 giây đặt `WAIT_RUN_TIMEOUT_SECONDS` qua `wait_line` (tham số thứ hai, như `conflict_run`), không dùng `wait_run`.

| Tiêu chí | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| `cancelled` của commit cũ, theo commit mới (L-066) | Test script | `wait-run: follows the PR head when the old commit's run is cancelled`. Commit đầu: `e35ae86`, `e35ae86`, rồi `eba5f98`. Lần chạy của `e35ae86`: `cancelled`. Của `eba5f98`: `in_progress` rồi `success`. Kết quả `0 XANH`. Code cũ cho `1 ĐỎ` |
| Dòng kết quả ghi commit mới | Test script | `wait-run: the green line names the new head`, qua `wait_line` và `grep -c`: dòng in ra có `eba5f98` (1), không có `e35ae86` (0) |
| Commit đầu đổi khi lần chạy cũ còn chạy | Test script | `wait-run: a new head while the old run is going waits for the new commit`. Commit đầu: `e35ae86`, `e35ae86`, rồi `eba5f98`. Lần chạy cũ mãi `in_progress`; lần chạy mới `success`. Kết quả `0 XANH`. Code cũ cho `2 CHƯA` |
| `cancelled` thật vẫn đỏ (đối chứng) | Test script | `wait-run: a cancelled run of a head that stays is red after the re-reads`. Commit đầu không đổi, lần chạy `cancelled`, `WAIT_RUN_CANCEL_RECHECKS=2`. Kết quả `1 ĐỎ`. Xanh trên cả code cũ và mới |
| Đọc lại khi `cancelled` vẫn theo hạn chờ | Test script | `wait-run: re-reading after a cancel stops at the deadline`. Commit đầu không đổi, lần chạy `cancelled`, `WAIT_RUN_CANCEL_RECHECKS=99`, `WAIT_RUN_TIMEOUT_SECONDS=2` (qua `wait_line`). Kết quả `2 CHƯA`, dòng có `bị hủy`. Code cũ cho `1 ĐỎ` |
| Lỗi đọc commit đầu giữ giá trị cũ | Test script | `wait-run: an unreadable head keeps the last one`. `pulls/7`: `e35ae86`, `""`, `e35ae86` (`head.sha` rỗng ở lần đọc thứ hai). Lần chạy của `e35ae86` `success`. Kết quả `0 XANH`. Không có bước này thì vòng thứ hai dựng truy vấn `head_sha=` rỗng |
| `--sha` không nhận lần chạy của commit khác | Test script | `wait-run: --sha ignores a finished run of the previous head`. `--sha eba5f98`. Commit đầu: `e35ae86`, `e35ae86`, rồi `eba5f98`. Lần chạy của `e35ae86`: `success`. Của `eba5f98`: `failure`, job `ui-test`. Kết quả `1 ĐỎ`. Code cũ cho `3 LỖI` (tham số lạ) |
| Đối chứng của `--sha` | Test script | `wait-run: without --sha the same fixture is green`: cùng fixture với test trên, bỏ `--sha`. Kết quả `0 XANH`, đúng lỗi mà `--sha` chặn. Xanh trên cả code cũ và mới |
| `--sha` không báo xung đột của commit cũ | Test script | `wait-run: --sha does not report a conflict of the previous head`. `--sha eba5f98`. `pulls/7`: `{e35ae86, dirty}` hai lần, rồi `{eba5f98, clean}`. Lần chạy của `e35ae86` không có; của `eba5f98` `success`. Kết quả `0 XANH`. Kiểm xung đột trong lúc chờ `--sha` sẽ cho `5 XUNG` |
| Commit đầu tiến qua `--sha` | Test script | `wait-run: a head that moves past --sha is followed`. `--sha eba5f98`. `pulls/7`: `eba5f98` hai lần, rồi `c0ffee1`. Lần chạy của `eba5f98` `cancelled`; của `c0ffee1` `success`. `WAIT_RUN_CANCEL_RECHECKS=3`. Kết quả `0 XANH`, dòng có `c0ffee1`. Giữ ràng buộc `--sha` thì script chờ `eba5f98` mãi và cho `2 CHƯA` |
| `--sha` mà PR chưa nhận | Test script | `wait-run: --sha the PR never reaches asks to call again`. Commit đầu mãi `e35ae86`, `--sha eba5f98`, `WAIT_RUN_TIMEOUT_SECONDS=1` (qua `wait_line`). Kết quả `2 CHƯA`, dòng có `chưa nhận commit eba5f98` |
| `--sha` sai dạng | Test script | `wait-run: rejects a --sha that is not a commit`: `--sha main`, kết quả `3 LỖI` |
| `--sha` với `--workflow` | Test script | `wait-run: refuses --sha with --workflow`, kết quả `3 LỖI` |
| Xung đột vẫn nhận ra khi đọc commit đầu mỗi vòng | Test script | Ba test `XUNG ĐỘT` hiện có qua mà không sửa (route `pulls/7` tĩnh có `mergeable_state`) |
| Không đổi hành vi cũ | Test script | Mọi test `wait-run` hiện có qua mà không sửa |
| Phiên dùng `--sha` | Kiểm tra thủ công | Người duyệt chéo đọc diff `docs/auto-run.md`. Phiên sau ghi lệnh chờ trong dòng thời gian của báo cáo |

**Ngoại lệ viết test trước:** `docs/auto-run.md`, `CLAUDE.md`, `docs/lessons.md` là tài liệu, không có test.

### 4.1. Chạy thử

Không cần (E dưới 2). Dữ liệu dựng fixture, đọc ngày 2026-10-24 (chỉ đọc):

- `gh api repos/VGL-Studio/camera-pose/actions/runs/40066418977`: `head_sha` `e35ae86...`, `pull_request`, `completed`/`cancelled`, tạo 05:29:02Z, cập nhật (bị hủy) 05:32:16Z.
- `.../actions/runs/40066593456`: `head_sha` `eba5f98...`, `completed`/`success`, tạo 05:31:35Z.
- `.../pulls/100/commits`: `e35ae86` commit lúc 05:28:55Z, `eba5f98` lúc 05:31:27Z. Lần chạy cũ bị hủy sau khi commit mới đã có lần chạy, nên `wait-run.sh` đang chờ commit cũ (đọc commit đầu một lần lúc bắt đầu) thấy `cancelled` và báo `ĐỎ`.
- Độ trễ của `pulls/<số>` sau một lần đẩy, đo ngày 2026-10-24 trên chính PR #112 của plan này:
  - đẩy commit rỗng `38d1703` rồi đọc `pulls/112` mỗi giây;
  - lần đọc thứ nhất, 1,1 giây sau lệnh đẩy, còn trả commit trước `99bb59b`;
  - lần đọc thứ hai, 3,5 giây sau, trả `38d1703`.
- Lần chạy `pr-check` của `99bb59b` (40101055973) đã xong `success` lúc 14:07:10Z, trước khi lần chạy của `38d1703` (40101078161) được tạo lúc 14:07:13Z.
- Kết luận: `wait-run.sh --pr 112` gọi ngay sau lệnh đẩy sẽ báo `XANH` cho commit cũ. Đây đúng là trường hợp mà `--sha` chặn (mục 8, câu 1).
- Độ trễ vài giây nằm trong một vòng chờ (30 giây), nên 3 lần đọc lại khi gặp `cancelled` là dư.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (quy trình phiên tự chạy): đổi cách phiên đọc kết quả CI (`scripts/wait-run.sh`, `docs/auto-run.md` bước 8).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script, test, hai đoạn tài liệu |
| B. Độ rõ của yêu cầu | 1 | L-066 rõ; còn câu hỏi về `--sha` và số lần đọc lại |
| C. Khả năng kiểm chứng tự động | 1 | Script có test với `gh` giả; độ trễ thật của GitHub chỉ thấy khi chạy |
| D. Ảnh hưởng chéo | 2 | Mọi bước chờ CI của phiên đi qua script này |
| E. Độ mới của kỹ thuật | 0 | Cùng REST API, cùng `gh` giả |
| **Tổng** | 5 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/wait-run.sh`; `scripts/tests/run.sh` (khối `wait-run`); `docs/auto-run.md` (mục Chờ workflow của bước 8); `CLAUDE.md` (mục Lệnh).
- Task khác có thể xung đột (các plan đang viết song song):
  - T-045 (hàng chờ plan) sửa `docs/auto-run.md` ở phần cuối phiên và mẫu báo cáo `docs/templates/run.md`. Đoạn khác với mục Chờ workflow của plan này; plan này không sửa `docs/templates/run.md`.
  - T-046 (kiểm tay trên emulator) sửa mẫu plan `docs/templates/plan.md` và kịch bản của `device-session`. Không chung file với plan này, trừ khi T-046 cũng thêm test vào `scripts/tests/run.sh`; nếu có thì ở khối khác, không chạm khối `wait-run`.
  - T-047 (sẵn sàng phát hành) sửa `scripts/status.py`. Không chung file; test của nó (nếu có trong `scripts/tests/run.sh`) ở khối khác.
- Đề xuất thứ tự chạy: không phụ thuộc T-045, T-046, T-047; chạy trước hay sau đều được. Task chạy lần lượt, mỗi task tạo nhánh từ nhánh chính mới nhất.
- Khi chạy trong phiên tự chạy, chính phiên dùng `wait-run.sh` để chờ PR của task này (nhật ký T-033: bash đọc script dần, sửa giữa chừng thì lỗi cú pháp):
  - lệnh chờ chạy từ bản chép của `origin/main` ở ngoài worktree: `git show origin/main:scripts/wait-run.sh > "$TMPDIR/wait-run.sh" && chmod +x "$TMPDIR/wait-run.sh"`, rồi `"$TMPDIR/wait-run.sh" --pr <số>`;
  - bản chép là bản cũ, chưa có `--sha` và chưa theo commit đầu. Với PR của chính T-044, `ĐỎ (cancelled)` thì gọi lại lệnh chờ, không tính vào 3 lần sửa (đúng lỗi L-066 mà task này sửa).
- Đề xuất về dự án mẫu: VGL-Studio/app-project-template#129 (cùng L-066). Merge xong thì dòng Áp dụng của L-066 ghi số PR để issue đó dẫn tới.

## 7. Điều kiện dừng riêng

1. Một test `wait-run` hiện có phải sửa điều nó kiểm tra mới qua được.
2. Cần sửa `scripts/merge-if-green.sh`, `scripts/tests/fake-gh` hay một workflow để phần mới chạy được: dừng, viết plan riêng.

## 8. Câu hỏi cho người duyệt

1. **Thêm `--sha` hay chỉ theo commit đầu?** Đề xuất của AI: thêm, không bắt buộc. Ngay sau `git push`, `pulls/<số>` còn trả commit trước trong vài giây (đo được 1,1 giây ở PR #112, mục 4.1). Nếu commit trước đã xanh, `wait-run.sh` báo `XANH` ngay cho commit cũ. Chỉ theo commit đầu (yêu cầu của ticket) không chặn được trường hợp này. Lời gọi không kèm `--sha` vẫn chạy như hiện nay, cộng phần theo commit đầu.
- Trả lời: Thêm `--sha`, không bắt buộc, theo đề xuất của rà soát độc lập (Tuấn, 2026-10-24).
2. **Đọc lại bao nhiêu lần khi gặp `cancelled` của commit đang là đầu?** Đề xuất của AI: 3 lần, mỗi lần cách một vòng chờ (30 giây), tổng khoảng 90 giây, đủ cho độ trễ của GitHub thấy ở PR #100 (lần chạy cũ bị hủy 41 giây sau khi lần chạy mới đã tạo). Lâu hơn chỉ làm chậm báo `ĐỎ (cancelled)` thật, vốn hiếm.
- Trả lời: Đọc lại 3 lần, theo đề xuất của rà soát độc lập (Tuấn, 2026-10-24).
3. **Làm trong phiên tự chạy hay phiên có người theo dõi?** Đề xuất của AI: phiên tự chạy được, như T-040 (sửa `merge-if-green.sh` trong RUN-20261024-01), với lệnh chờ chạy từ bản chép của `origin/main` (mục 6). Plan không sửa `.github/`.
- Trả lời: Làm được trong phiên tự chạy, theo đề xuất của rà soát độc lập (Tuấn, 2026-10-24).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1: Duyệt (Nhật, duyệt chéo Tuấn, 2026-10-24); ghi chú: Ngày 2026-10-24, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Rà soát độc lập (phiên mới, chỉ đọc) trước khi duyệt; mọi điểm chặn và nên sửa đã sửa trong bản nháp. Thay bằng phiên bản 2 vì rà soát độc lập trước RUN-20261024-02 (docs/ops/pre-run-review.md, mục 2) tìm hàm `wait_line` chưa xóa `wait.json.seen` và không đặt hạn chờ: phần tử `__sequence__` của test trước lẫn sang test sau, và logic sai thì test quay vòng 40 phút (mục 4).

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <ghi chú, nếu có>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
