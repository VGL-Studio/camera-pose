---
ticket: T-033
title: wait-run.sh nhận ra job GitHub không chạy (hạn mức Actions) hay PR xung đột, và phiên tự tiếp tục sau đó
status: approved                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.0M         # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                   # người duyệt
cross_reviewer: Tuấn              # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-033: WAIT-RUN.SH NHẬN RA JOB GITHUB KHÔNG CHẠY HAY PR XUNG ĐỘT, VÀ PHIÊN TỰ TIẾP TỤC SAU ĐÓ

## 1. Tóm tắt

`scripts/wait-run.sh` thấy lần chạy đỏ mà mọi job đỏ đều không chạy bước nào thì đọc chú thích của GitHub, in `HẠ TẦNG: ...` và thoát mã 4, thay vì `ĐỎ` mã 1. `docs/auto-run.md` nói phiên làm gì với mã 4:
- không sửa code, không revert;
- kết thúc phiên mà giữ danh sách chạy `running`;
- khi `resume-run.yml` kích hoạt lại (GitHub đã chạy lại được), chạy lại các lần chạy bị bỏ trước khi đi tiếp.

Kiểm chứng bằng test của script với `gh` giả, dựng theo đúng dữ liệu của lần hết hạn mức ngày 2026-10-23.

Plan chạy trong phiên có người theo dõi, không đưa vào danh sách chạy: nó đổi cách phiên đọc kết quả CI.

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là bài học L-046 (`docs/lessons.md`, `accepted`) và L-048 (`proposed`, phát hiện khi viết plan này; phần của L-048 chờ người duyệt bài học cùng lúc duyệt plan).

| Yêu cầu | Tiêu chí | Cách hiểu của AI |
| --- | --- | --- |
| Nhận ra job không chạy | Khi lần chạy kết thúc `failure` và mọi job `failure` có 0 bước thì `wait-run.sh` in một dòng bắt đầu bằng `HẠ TẦNG:` và thoát mã 4 | Job GitHub không cấp máy có `steps: []`, `runner_name` rỗng (mục 4.1). Job đã chạy dù chỉ một bước thì có `steps` khác rỗng. Có job đỏ đã chạy bước nào đó thì vẫn là `ĐỎ`, mã 1: đó là lỗi có thể do code |
| Nói rõ lý do | Dòng `HẠ TẦNG` chép chú thích mức `failure` của check run (ví dụ "The job was not started because recent account payments have failed or your spending limit needs to be increased") | Chú thích đọc ở `check-runs/<id của job>/annotations`; id của job là id của check run (mục 4.1). Không có chú thích thì ghi "GitHub không ghi lý do" |
| PR xung đột (L-048) | Khi `--pr` chưa thấy lần chạy nào và PR có `mergeable_state` `dirty` thì `wait-run.sh` in `XUNG ĐỘT: ...` và thoát mã 5 ngay, không chờ tới hết giờ | GitHub không chạy workflow `pull_request` cho PR đang xung đột với nhánh chính. PR #51 và PR #56 không có lần chạy nào vì xung đột ở `docs/STATUS.md`; với PR #56 `wait-run.sh` chờ trọn 40 phút (mục 4.1). `mergeable_state` còn `unknown` khi GitHub chưa tính xong: chờ tiếp như cũ |
| Phiên không làm hỏng việc | Khi gặp mã 4, phiên không sửa code, không chạy lại ngay, không revert, không dừng task; kết thúc phiên, giữ `running` | Theo mẫu "Hết hạn mức sử dụng" của mục 4: phần đã đẩy còn nguyên, `resume-run.yml` lo việc chờ. Chính `resume-run.yml` cũng chỉ chạy được khi GitHub cấp máy trở lại, nên lần kích hoạt lại đầu tiên đến sau khi người sửa hạn mức |
| Phiên tiếp tục đúng | Phiên tiếp tục chạy lại lần chạy `pr-check` hay `main-guard` đã báo `HẠ TẦNG`, chờ kết quả, rồi mới đi tiếp | `POST actions/runs/<id>/rerun-failed-jobs` qua REST (`gh pr` bị chặn ở cloud) |

Thiết kế tham chiếu: không có thiết kế riêng.

ADR liên quan: ADR-0010 (cổng chất lượng), ADR-0016 (routine, `start-run.yml`, `resume-run.yml`), ADR-1002 (revert chỉ khi `main-broken`).

## 3. Giải pháp

### 3.1. Cách làm

**`scripts/wait-run.sh`, nhánh lần chạy không `success`.** Hiện script gọi `actions/runs/$id/jobs?per_page=50` với `--jq` để lấy tên job đỏ. Đổi thành một lần gọi lấy cả JSON (cùng đường dẫn), rồi:
```bash
jobs="$(gh api "repos/$repo/actions/runs/$id/jobs?per_page=50" 2> /dev/null || true)"
failed="$(jq -r '[.jobs[]? | select(.conclusion == "failure" or .conclusion == "cancelled" or .conclusion == "timed_out") | .name] | join(", ")' <<< "${jobs:-{}}" 2> /dev/null || true)"
# A job GitHub never started (no runner: spending limit, billing) has no steps (L-046).
unstarted="$(jq -r '[.jobs[]? | select(.conclusion == "failure")] as $f
  | if ($f | length) > 0 and ($f | all(.steps? == [])) then ($f | map(.id) | join(" ")) else "" end' <<< "${jobs:-{}}" 2> /dev/null || true)"
```
- `unstarted` khác rỗng: đọc chú thích `failure` đầu tiên của job đầu tiên, `gh api "repos/$repo/check-runs/<id>/annotations" --jq '[.[] | select(.annotation_level == "failure") | .message] | first // empty'`. In:
  `HẠ TẦNG: GitHub không chạy job <tên> của <what>, lần chạy <id>: <chú thích hoặc "GitHub không ghi lý do">. Không phải lỗi của code: không sửa code, không revert; người kiểm hạn mức GitHub Actions (Billing & plans), phiên làm theo docs/auto-run.md, mục 4 (L-046). <url>`, mã thoát 4.
- Ngược lại: dòng `ĐỎ` như cũ, mã 1.
- Job thiếu khóa `steps` (các fixture hiện có) không được coi là chưa chạy: `.steps? == []` sai khi khóa không có.

**PR xung đột (L-048):** với `--pr`, mỗi lần chưa thấy lần chạy nào thì đọc `pulls/<số>` lấy `mergeable_state` (cùng lần gọi đã lấy `head.sha` ở đầu script, gọi lại trong vòng chờ). `dirty`: in `XUNG ĐỘT: PR #<số> xung đột với nhánh chính, GitHub không chạy pr-check cho PR này. Gộp nhánh chính vào nhánh (git merge origin/main), sửa xung đột, đẩy, rồi gọi lại (L-048).`, mã thoát 5. Giá trị khác (`unknown`, `behind`, `blocked`, `clean`...) thì chờ như cũ. Dòng hết giờ không có lần chạy giữ nguyên, mã 2.

**Chú thích đầu script:** thêm mã 4 vào bảng mã thoát.

**`docs/auto-run.md`:**
- Mục Chờ workflow (bước 8): thêm mã 4 vào câu liệt kê mã, và một gạch đầu dòng: mã 4 thì làm theo mục 4, dòng "GitHub không chạy job". Không đọc log (không có), không chạy lại, không sửa code; không tính vào 3 lần sửa của cùng một lỗi.
- Mục Chờ workflow: mã 5 (PR xung đột) thì gộp nhánh chính vào nhánh, sửa xung đột trong phạm vi file của plan, đẩy, rồi chờ lại; không tính vào 3 lần sửa của cùng một lỗi (L-048).
- Bước 9, đoạn chờ `main-guard` sau khi merge: `wait-run.sh` trả mã 4 thì nhánh chính chưa được kiểm, không phải đỏ: không revert (ADR-1002 chỉ áp dụng khi có issue `main-broken`), làm theo mục 4.
- Mục 4, bảng kết thúc phiên, dòng mới "GitHub không chạy job (`wait-run.sh` mã 4)": dừng sau khi đẩy hết những gì đang có (task đang làm, nhánh báo cáo với dòng thời gian ghi giờ và lần chạy bị bỏ); giữ danh sách chạy `running`, plan và ticket giữ trạng thái; không bắt đầu task mới. `resume-run.yml` kích hoạt lại khi GitHub chạy lại được.
- Mục 4, Tiếp tục phiên, bước 4 (task đang làm dở) và bước mới trước bước 3: với mỗi lần chạy mà dòng thời gian ghi là bị bỏ (`pr-check` của task đang làm, `main-guard` của commit merge của phiên), chạy lại một lần bằng `gh api -X POST "repos/{owner}/{repo}/actions/runs/<id>/rerun-failed-jobs"` rồi chờ bằng `wait-run.sh`. Lại mã 4 thì kết thúc phiên như trên. `main-guard` của commit merge của phiên phải xanh, và không có `main-broken`, trước khi làm task tiếp theo.

Các kiểm tra hiện có mà trường hợp mới đi qua (L-044; plan không đổi `merge-if-green.sh` hay `main-guard.yml`, bảng ghi để người duyệt thấy không có đường vòng):

| Kiểm tra | Khi GitHub không chạy job |
| --- | --- |
| `merge-if-green.sh`: có check `failure` | Vẫn từ chối. Phiên không gọi merge khi `wait-run.sh` trả 4 |
| `main-guard.yml`: mở `main-broken` khi `check.sh` đỏ | Không mở, vì bước `check.sh` không chạy. Phiên không coi mã 4 là `main-broken` |
| `auto-run.md` bước 8: sửa tối đa 3 lần cho cùng một lỗi | Không áp dụng cho mã 4 |
| `resume-run.yml`: không tiếp tục khi phiên bắt đầu hơn 24 giờ trước, khi có `main-broken` hay `env-broken` | Giữ nguyên. Hạn mức sửa sau 24 giờ thì người lập danh sách chạy mới |
| `resume-check.py`: phiên không có commit trong 90 phút | Giữ nguyên; phiên đã kết thúc nên điều kiện sẽ thỏa |

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/wait-run.sh` | Mã 4 và dòng `HẠ TẦNG`; gợi ý hạn mức khi hết giờ mà chưa có lần chạy; chú thích đầu file |
| Sửa | `scripts/tests/run.sh` | Test ở mục 4 |
| Sửa | `docs/auto-run.md` | Mục Chờ workflow, bước 9, mục 4 (bảng kết thúc, Tiếp tục phiên) |
| Sửa | `docs/lessons.md` | Dòng Áp dụng của L-046 |
| Sửa | `docs/features/quality-gate/plans/T-033-wait-run-loi-ha-tang.md` | Nhật ký thực hiện |
| Sửa | `docs/features/quality-gate/tickets/T-033-wait-run-loi-ha-tang.md` | Trạng thái |

`docs/STATUS.md`: sinh lại bằng `scripts/status.py` nếu T-034 chưa merge; T-034 đã merge thì không còn file này trong git.

Các file sau nhắc tới file mà plan sửa nhưng không bị ảnh hưởng (L-020):
- `scripts/merge-if-green.sh` chỉ nhắc `scripts/wait-run.sh --pr` trong thông báo mã 2 của `--update`; không đọc mã thoát của nó.
- `.github/workflows/*.yml` không gọi `wait-run.sh`. `docs/ops/*.md` không nhắc tới nó.
- `scripts/resume-check.py` và `.github/workflows/resume-run.yml` chỉ nhắc `docs/auto-run.md` trong chú thích; plan không đổi điều kiện tiếp tục mà chúng cài đặt.
- `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh` chỉ nhắc `docs/auto-run.md` trong chú thích hay thông báo, trỏ tới các mục 1, 2, 3 bước 5 và bước 8 phần CI đỏ; plan không đổi nội dung các mục đó.
- `scripts/check.sh` chạy `scripts/tests/run.sh`; test mới chỉ dùng `gh` giả, không cần mạng hay công cụ mới.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `POST actions/runs/<id>/rerun-failed-jobs` từ môi trường `claude --cloud` (proxy GitHub chặn GraphQL; đây là REST nên dự kiến qua, chưa thử vì nó chạy lại một lần chạy thật).
- **Điều kiện chuyển:** lần đầu phiên tiếp tục gọi lệnh này thì nhận 403 từ proxy.
- **Cách làm thay:** với `pr-check`, đẩy một commit rỗng lên nhánh task (`git commit --allow-empty`) để GitHub tạo lần chạy mới. Với `main-guard` của commit merge, không có cách đẩy lên nhánh chính: phiên kết thúc, báo cáo ghi lần chạy cần người chạy lại. Test ở mục 4 không đổi.

## 4. Kiểm thử

Test trong `scripts/tests/run.sh`, viết trước code, dùng hàm `wait_run` và `pr_routes` có sẵn. Fixture của job lấy theo dữ liệu thật ở mục 4.1: `{"id": 120, "name": "guard", "conclusion": "failure", "steps": []}`, chú thích `failure` có câu "The job was not started because recent account payments have failed or your spending limit needs to be increased", và một chú thích `notice` không liên quan.

| Tiêu chí | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Job không chạy | Test script | `wait-run: a job GitHub never started is an infrastructure failure`: `--pr 7`, lần chạy đỏ, job như trên; kết quả `4 HẠ` |
| Lý do từ chú thích | Test script | `wait-run: the infrastructure line quotes GitHub's annotation`: dòng in ra có `spending limit`, không có câu của chú thích `notice` |
| Không có chú thích | Test script | `wait-run: an unstarted job without annotations is still infrastructure`: route chú thích trả `[]`; `4 HẠ`, dòng có `GitHub không ghi lý do` |
| Job đã chạy bước nào vẫn là đỏ | Test script | `wait-run: a failed job that ran steps stays red`: `steps: [{"name": "Set up job"}]`; `1 ĐỎ` |
| Lẫn lộn vẫn là đỏ | Test script | `wait-run: one unstarted and one failed job that ran stays red`; `1 ĐỎ` |
| Theo workflow trên nhánh chính | Test script | `wait-run: an unstarted main-guard run is an infrastructure failure`: `--workflow main-guard.yml --since ...`; `4 HẠ` |
| PR xung đột | Test script | `wait-run: a conflicting PR without runs stops at once`: route `pulls/7` có `mergeable_state` `dirty`, không có lần chạy; kết quả `5 XUNG`, với `WAIT_RUN_TIMEOUT_SECONDS` lớn để test không qua nhờ hết giờ |
| Chưa tính xong vẫn chờ | Test script | `wait-run: an unknown mergeable state keeps waiting`: `mergeable_state` `unknown`, không có lần chạy, `--timeout 1`; `2 CHƯA` |
| Có lần chạy thì không xét xung đột | Test script | `wait-run: a run that exists is waited for even if the PR became dirty`: `dirty` nhưng có lần chạy xanh; `0 XANH` |
| Đối chứng (L-035) | Test script | Test `HẠ TẦNG` đầu tiên và test "đã chạy bước nào" dùng cùng fixture, chỉ khác `steps`: test đầu đỏ trên code cũ vì ra `1 ĐỎ`, test sau xanh trên cả code cũ và mới |
| Không đổi hành vi cũ | Test script | Mọi test `wait-run` hiện có (job không có khóa `steps`) qua mà không sửa |
| Phiên dùng mã 4 | Kiểm tra thủ công | Người duyệt chéo đọc diff `docs/auto-run.md`. Lần hết hạn mức tới (nếu có), báo cáo phiên ghi dòng `HẠ TẦNG` và lần tiếp tục |

**Ngoại lệ viết test trước:** `docs/auto-run.md` và `docs/lessons.md` là tài liệu, không có test.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-23, chỉ đọc, trên hai lần chạy bị GitHub bỏ trong lúc hết hạn mức:
  - `gh api repos/VGL-Studio/camera-pose/actions/runs/39916343646/attempts/1/jobs`: job `guard` có `conclusion: failure`, `steps` 0 phần tử, `runner_name` rỗng.
  - `gh api .../check-runs/120290639115/annotations` (id của job ở trên): chú thích `failure` "The job was not started because recent account payments have failed or your spending limit needs to be increased. Please check the 'Billing & plans' section in your settings", cùng một chú thích `notice` về nhãn `ubuntu-latest`.
  - `gh api .../actions/runs/39916343646/jobs` (không chỉ lần thử): trả lần thử mới nhất (lần 2, xanh, 15 bước). Trong lúc hết hạn mức thì lần thử mới nhất chính là lần bị bỏ, nên đường dẫn script đang dùng cho đúng dữ liệu.
  - `gh api .../actions/runs/39916715279/attempts/1/jobs` (`pr-check` của PR #50): `changes` `failure` 0 bước; `build-and-test`, `ui-test` `skipped` 0 bước. Job `skipped` không bị tính vì chỉ xét job `failure`.
  - PR #56 (commit 347472b): `actions/runs?head_sha=347472b...` trả 0 lần chạy; `pulls/56` có `mergeable_state` `dirty`. `wait-run.sh --pr 56` chờ đủ 40 phút rồi trả mã 2.
- **Chưa thử:** `rerun-failed-jobs` từ môi trường cloud (mục 3.4). Trên máy dev, `gh run rerun 39916343646` đã chạy lại được lần chạy bị bỏ (cùng API).
- **Lệnh:** `scripts/tests/run.sh` sau khi viết test.
- **Kết quả:** dữ liệu cần cho cả hai điều kiện (0 bước, chú thích) đọc được bằng REST, đúng dạng mà mục 3.1 dùng.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (quy trình phiên tự chạy): đổi cách phiên đọc kết quả CI và kết thúc phiên (`docs/auto-run.md`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script, test, ba đoạn tài liệu |
| B. Độ rõ của yêu cầu | 1 | L-046 rõ; còn câu hỏi về kết thúc phiên hay chờ |
| C. Khả năng kiểm chứng tự động | 1 | Script có test; phần phiên làm theo tài liệu chỉ kiểm khi sự cố xảy ra lại |
| D. Ảnh hưởng chéo | 2 | Mọi phiên chờ CI qua script này |
| E. Độ mới của kỹ thuật | 1 | REST API đã dùng; thêm một endpoint chú thích |
| **Tổng** | 6 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/wait-run.sh`, `docs/auto-run.md`.
- Task khác có thể xung đột: T-034 cũng sửa `docs/auto-run.md` (mục 4, PR báo cáo, đoạn khác); làm lần lượt.
- Đề xuất thứ tự chạy: phiên có người theo dõi, trước hay sau T-034 đều được.

## 7. Điều kiện dừng riêng

1. Một test `wait-run` hiện có phải sửa điều nó kiểm tra mới qua được.
2. Cần đổi `merge-if-green.sh` hay một workflow để phiên xử lý được mã 4: dừng, viết plan riêng.

## 8. Câu hỏi cho người duyệt

1. **Gặp mã 4 thì kết thúc phiên hay chờ trong phiên?** Đề xuất của AI: kết thúc, giữ `running`, để `resume-run.yml` kích hoạt lại. Sửa hạn mức cần người và có thể mất hàng giờ; phiên chờ trong lúc đó tốn hạn mức Claude mà không làm được gì, vì mọi task đều cần CI.
- Trả lời: Đồng ý với đề xuất của AI.
2. **Chỉ báo `HẠ TẦNG` khi mọi job đỏ đều chưa chạy bước nào?** Đề xuất của AI: có. Lẫn một job đã chạy mà đỏ thì có thể là lỗi của code; báo `ĐỎ` để phiên đọc log như thường.
- Trả lời: Đồng ý với đề xuất của AI.
3. **PR xung đột: mã thoát riêng (5) hay dùng chung mã 1?** Đề xuất của AI: mã riêng. Mã 1 nghĩa là CI đỏ, phiên sẽ đọc log và sửa code (bước 8); với PR xung đột, việc đúng là gộp nhánh chính (bước 9), không tính vào 3 lần sửa lỗi.
- Trả lời: Đồng ý với đề xuất của AI.## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Tuấn trả lời mục 8 cùng AI ngày 2026-10-23 và ghi quyết định duyệt.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### 2026-10-23, phiên có người theo dõi (Tuấn)

- **Bước 3, test viết trước:** 10 test mới trong `scripts/tests/run.sh`. Trên code cũ 6 test đỏ, đúng lý do (`1 ĐỎ` thay vì `4 HẠ`, không có dòng chú thích, `2 CHƯA` sau 30 giây thay vì `5 XUNG`). Các test đối chứng (job đã chạy bước nào, lẫn lộn, `unknown` vẫn chờ, có lần chạy dù PR `dirty`) xanh trên cả code cũ và mới.
- **Bước 4, code:** `scripts/wait-run.sh` theo mục 3.1, cộng phần `XUNG ĐỘT` (L-048); `docs/auto-run.md` mục Chờ workflow, bước 9, bảng kết thúc phiên, Tiếp tục phiên (bước mới 3, các bước sau đánh số lại; không nơi nào trỏ tới số bước cũ).
- **Bước 5:** `scripts/tests/run.sh` qua 319 test. `scripts/check.sh` để CI của PR chạy.
- **Sự cố khi làm:** một lệnh chờ chạy nền đang chạy `scripts/wait-run.sh` của cây làm việc trong lúc file được sửa; bash đọc script dần nên báo lỗi cú pháp và PR #57 không được merge tự động. Merge lại bằng tay ngay sau đó. Chạy lệnh chờ từ một bản chép của script khi đang sửa chính script đó.
- **Còn lại:** kiểm tay ở lần GitHub không chạy job tới (nếu có): báo cáo phiên ghi dòng `HẠ TẦNG` và lần tiếp tục.
