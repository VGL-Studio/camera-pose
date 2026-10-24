---
ticket: T-037
title: Nhánh task không chỉ nằm trên máy cloud
status: done             # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.0M         # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-24
---

# PLAN T-037: NHÁNH TASK KHÔNG CHỈ NẰM TRÊN MÁY CLOUD

## 1. Tóm tắt

Phiên đẩy nhánh task ngay lúc tạo, trước khi viết dòng test nào, để lần từ chối đẩy (nếu có) xảy ra khi chưa có gì để mất. Mọi lần đẩy nhánh task đi qua `scripts/save-branch.sh <nhánh>`. Script đẩy, và thử lại theo lịch khi lỗi mạng. Đẩy không được thì gói nhánh thành một `git bundle` trong `$HOME/rescue/`, ngoài worktree, và in sẵn đoạn "Nhánh chưa đẩy" để chép vào báo cáo phiên: đường dẫn bundle, mã commit, lệnh lấy lại.

Khi chính lệnh đẩy bị bộ phân loại của môi trường từ chối, phiên không đẩy lại dưới bất kỳ dạng nào. Phiên gọi `save-branch.sh --bundle-only`, dừng task, và báo cáo đưa đoạn đó lên đầu.

Kiểm chứng bằng test của script, trên repo thật dựng tạm (remote là một bare repo cục bộ). Cách lấy lại từ bundle đã chạy thử với 4 commit thật của T-017 (mục 4.1).

**Phiên bản 2** sửa theo rà soát độc lập trước RUN-20261024-01:

- Đẩy hỏng mà nhánh chưa có commit nào ngoài `origin/main` (đúng trường hợp của bước 1) thì script thoát 3 (`CHƯA ĐẨY`), không thoát 0. Phiên bản 1 in `KHÔNG CẦN BUNDLE` và thoát 0, nên phiên tưởng đã đẩy được và mất đúng mục tiêu "lộ sớm" (mục 3.1, mục 4).
- Danh sách chuỗi lỗi mạng thêm `hung up`, khớp với hook mô phỏng lỗi mạng ở mục 4 (`fatal: The remote end hung up unexpectedly`). Phiên bản 1 thiếu chuỗi này, nên test thử lại sẽ đỏ hay chạm điều kiện dừng 1 của plan.
- Mục 6 bỏ nhắc nhánh thử của T-036 (đã bỏ khi duyệt T-036). Mục 6 cũng ghi T-038 làm trước phiên, và các câu của `auto-run.md` mà T-043 (ADR-1003) vừa đổi.
- Ticket ghi đúng câu trả lời đã duyệt ở phiên bản 1 (bundle ở `$HOME/rescue/`, không vào nhánh báo cáo hay artifact).

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là bài học L-051 (`docs/lessons.md`, `proposed`): phiên con của T-017 làm xong 4 commit, `check.sh` qua, nhưng `git push` bị bộ phân loại tự động từ chối ("dangerous", không giải thích). Nhánh chỉ nằm trên máy cloud tạm thời, người phải vào phiên cloud để đẩy tay (2026-10-24).

| Yêu cầu | Tiêu chí | Cách hiểu của AI |
| --- | --- | --- |
| Lộ sớm | Khi tạo nhánh task (bước 1), phiên đẩy nhánh ngay, trước commit test | `auto-run.md` đã bắt đẩy sau mỗi commit ở bước 3 và 4, nhưng phiên con của T-017 chỉ đẩy ở bước 7. Đẩy lúc tạo nhánh làm lần từ chối xảy ra khi chưa có commit nào. Bị từ chối lúc này thì không mất gì: dừng task, task sau cũng sẽ cần đẩy (xem mục 3.1) |
| Không mất việc | Khi `git push` thất bại, script tạo bundle chứa mọi commit của nhánh chưa có trên `origin/main`, ở `$HOME/rescue/<nhánh>.bundle`, và `git bundle verify` qua | `$HOME` nằm ngoài thư mục làm việc, nên còn sau `git worktree remove`. Bundle không thay được việc đẩy; nó chỉ giữ nhánh thành một file, gom ở một chỗ biết trước, cho người lấy lại |
| Người lấy lại được | Script in lệnh lấy lại đã thử ở mục 4.1, với tên nhánh và mã commit đúng | `git fetch <bundle> <ref>:<nhánh>` trong một bản sao có nhánh chính, rồi `git push -u origin <nhánh>` (người chạy). `git clone <bundle>` không được, vì bundle cần commit gốc (mục 4.1) |
| Lỗi mạng không phải từ chối | `git push` lỗi kết nối, hết giờ chờ, 5xx thì thử lại sau 1, 2, 4 phút; vẫn lỗi thì bundle | Theo lịch thử lại lỗi hạ tầng của `auto-run.md` bước 5, rút ngắn vì đây là một lệnh đẩy |
| Không vòng qua lần từ chối | Lệnh đẩy bị môi trường từ chối thì phiên không đẩy lại (không đổi tên nhánh, không dùng API để tạo ref, không giao cho phiên khác) | Bộ phân loại từ chối lời gọi lệnh, nên script không chạy và không thấy được lần từ chối. Phiên thấy, và chỉ được chạy `save-branch.sh --bundle-only` (không đẩy) |
| Báo cáo có chỗ | Báo cáo phiên có mục "Nhánh chưa đẩy" ngay dưới Tóm tắt khi có bundle | Người đọc báo cáo sáng ra thấy ngay việc cần làm trước khi máy cloud bị thu hồi |

Thiết kế tham chiếu: không có thiết kế riêng.

ADR liên quan: ADR-0016 (routine, phiên cloud). Không đổi ADR.

## 3. Giải pháp

### 3.1. Cách làm

**`scripts/save-branch.sh` (mới):**

```
scripts/save-branch.sh <nhánh>                 đẩy (thử lại khi lỗi mạng); không được thì bundle
scripts/save-branch.sh --bundle-only <nhánh>   chỉ bundle, không đẩy (lệnh đẩy đã bị môi trường từ chối)
```

- Kiểm `<nhánh>` có dạng `claude/...` và là một nhánh cục bộ; sai thì in `LỖI: ...`, mã 2.
- Đẩy: `git push -u origin <nhánh>`. Mã 0 thì in `ĐÃ ĐẨY: <nhánh> (<mã commit ngắn>)`, mã 0.
- Lỗi: đọc stderr. Có `Could not resolve host`, `Connection timed out`, `Connection reset`, `The requested URL returned error: 5`, `RPC failed`, `early EOF`, `hung up` thì là lỗi mạng: chờ `SAVE_BRANCH_RETRY_SECONDS` (mặc định `60 120 240`) rồi thử lại. Lỗi khác (từ chối của GitHub, không có quyền) thì không thử lại.
- Không đẩy được: tạo bundle.
  ```bash
  mkdir -p "$HOME/rescue"
  git bundle create "$HOME/rescue/<tên tệp>.bundle" "<nhánh>" --not origin/main
  git bundle verify "$HOME/rescue/<tên tệp>.bundle"
  ```
  - Tên tệp lấy từ tên nhánh, đổi `/` thành `__`.
  - Không có commit nào ngoài `origin/main` thì không tạo bundle (git từ chối "empty bundle"), in `CHƯA ĐẨY: <nhánh> (không cần bundle: nhánh chưa có commit nào ngoài origin/main)`, mã 3. Không có gì để mất, nhưng lệnh đẩy đã hỏng: phiên phải biết để dừng task như mọi lần đẩy hỏng (đây là trường hợp của bước 1, đẩy lúc tạo nhánh).
  - In đoạn Markdown để chép vào báo cáo: tên nhánh, mã commit đầu nhánh, số commit, đường dẫn bundle, cỡ tệp, và lệnh lấy lại:
    ```
    git fetch <đường dẫn bundle> <nhánh>:<nhánh>
    git push -u origin <nhánh>
    ```
  - Mã thoát 3 (`CHƯA ĐẨY`), để phiên biết phải dừng task.
- `--bundle-only`: bỏ bước đẩy, làm phần bundle như trên, mã 3 (cả khi không cần bundle).
- Tóm lại mã thoát: 0 chỉ khi đã đẩy được; 3 mọi trường hợp chưa đẩy (có hay không có bundle); 2 tham số sai.
- Chú thích đầu file (tiếng Anh): cách dùng, mã thoát, và câu "never retries a push the environment refused: that call never reaches this script".

**`docs/auto-run.md`** (phần của plan này; T-036, T-038, T-039, T-040 sửa các đoạn khác của cùng file, xem mục 6):

- Bảng các bước, bước 1: "Tạo nhánh ... rồi đẩy ngay bằng `scripts/save-branch.sh <nhánh>`".
- Bước 3, gạch đầu dòng "Đẩy nhánh của task lên ngay sau mỗi commit": đẩy bằng `scripts/save-branch.sh <nhánh>`. Mã 3 thì làm theo đoạn mới "Khi không đẩy được" (dưới).
- Giao bước 3 đến 5 cho phiên con: nội dung giao việc tối thiểu thêm "đẩy bằng `scripts/save-branch.sh` sau mỗi commit; lệnh đẩy bị từ chối hay script trả mã 3 thì dừng ngay, chạy `scripts/save-branch.sh --bundle-only <nhánh>` và báo lại đoạn script in ra". Câu "Xóa worktree sau khi merge" thêm: "không xóa worktree của nhánh chưa đẩy được".
- Bước 7: đẩy bằng `scripts/save-branch.sh`.
- Đoạn mới "Khi không đẩy được", sau bước 7. Lệnh đẩy bị môi trường từ chối, hoặc `save-branch.sh` trả mã 3:
  1. Không đẩy lại dưới dạng khác: không đổi tên nhánh, không gọi API tạo ref, không giao cho phiên khác.
  2. Chạy `save-branch.sh --bundle-only <nhánh>` (nếu chưa có bundle).
  3. Dừng task theo `risk-scoring.md` mục 4: plan và ticket giữ `approved`, vì không phải lỗi của plan.
  4. Chép đoạn script in ra vào báo cáo.
  5. Làm tiếp task sau. Lần đẩy nhánh của task sau (ở bước 1) cũng bị từ chối thì kết thúc phiên (mục 4), vì mọi task đều cần đẩy.
- Mục 4, bảng kết thúc phiên, dòng mới: "Lệnh đẩy nhánh bị từ chối ở hai task liên tiếp: đẩy nhánh báo cáo nếu được; báo cáo ghi các bundle".
- Mục 5 (báo cáo phiên): có bundle thì mục "Nhánh chưa đẩy" ngay dưới Tóm tắt, chép đoạn của script.

**`docs/templates/run.md`:** thêm mục "Nhánh chưa đẩy" (ghi "Không có" khi không có) dưới Tóm tắt của mục 3.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/save-branch.sh` | Script ở mục 3.1 |
| Sửa | `scripts/tests/run.sh` | Test ở mục 4, một khối mới ở cuối file, trước dòng tổng kết |
| Sửa | `docs/auto-run.md` | Bảng các bước (bước 1), bước 3, Giao bước 3 đến 5, bước 7, đoạn mới "Khi không đẩy được", mục 4 (một dòng của bảng), mục 5 |
| Sửa | `docs/templates/run.md` | Mục "Nhánh chưa đẩy" |
| Sửa | `docs/lessons.md` | Dòng Áp dụng của L-051 |
| Sửa | `docs/features/quality-gate/plans/T-037-nhanh-task-khong-mat.md` | Nhật ký thực hiện |
| Sửa | `docs/features/quality-gate/tickets/T-037-nhanh-task-khong-mat.md` | Trạng thái |

Các file nhắc tới file mà plan sửa nhưng không bị ảnh hưởng (L-020):
- `scripts/check.sh` chạy `scripts/tests/run.sh`. Test mới chỉ dùng `git` thật trên repo tạm và bare repo cục bộ, không cần mạng.
- Mọi test git của `run.sh` dùng `GIT_CONFIG_COUNT=1` với remote giả `https://github.com/o/r.git`. Test mới đặt thêm `url.<bare repo>.insteadOf` như test `merge_while_broken`, nên không ra mạng.
- Không script nào đọc `docs/templates/run.md` (`grep -n "templates/run" scripts/*.py` rỗng). `scripts/status.py` và `scripts/run-check.py` đọc danh sách chạy, chỉ phần frontmatter và bảng mục 1; mục mới nằm ở mục 3.
- `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/resume-check.py` nhắc `docs/auto-run.md` (hay tên `run.md` của danh sách chạy) trong chú thích hoặc thông báo, trỏ tới mục 1, 2, bước 5, bước 8 và phần hạn mức của mục 4. Plan không đổi các đoạn đó.
- `scripts/merge-if-green.sh` và `scripts/wait-run.sh` nhắc `docs/auto-run.md` trong thông báo (bước 8, 9) và được `scripts/tests/run.sh` kiểm. Plan không đổi bước 8, 9 và không sửa test hiện có của hai script.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** môi trường cloud có thể không cho ghi vào `$HOME/rescue` (chưa thấy giới hạn này, `$HOME` là `/home/user` và các phiên trước đã ghi `~/.gradle` ở đó).
- **Điều kiện chuyển:** `mkdir -p "$HOME/rescue"` thất bại trong test trên CI hay trong phiên.
- **Cách làm thay:** script dùng `SAVE_BRANCH_DIR`, mặc định `$HOME/rescue`; không ghi được thì dùng thư mục cha của checkout chính (`$(git rev-parse --git-common-dir)/../../rescue`), cũng nằm ngoài worktree của task. Test ở mục 4 không đổi (test đặt `SAVE_BRANCH_DIR`).

## 4. Kiểm thử

Test trong `scripts/tests/run.sh`, viết trước code. Khung rỗng: `scripts/save-branch.sh` chỉ có phần đọc tham số, luôn in `ĐÃ ĐẨY` và trả 0. Khung test:
- một bare repo `$work/sb-origin.git` làm GitHub giả, trỏ tới bằng `url.<đường dẫn>.insteadOf=https://github.com/o/r.git`;
- một bản sao có nhánh `main` và nhánh `claude/T-999-thu` hai commit;
- `SAVE_BRANCH_DIR=$work/rescue` và `SAVE_BRANCH_RETRY_SECONDS=0`.

Đẩy thất bại được dựng bằng hook `pre-receive` của bare repo:
- `exit 1` để mô phỏng từ chối;
- hoặc in `fatal: The remote end hung up unexpectedly` kèm `exit 1` để mô phỏng lỗi mạng. stderr của `git push` khi đó có `hung up` (chuỗi có trong danh sách lỗi mạng ở mục 3.1); bước 3 ghi đúng chuỗi git in ra vào nhật ký.

| Tiêu chí | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Đẩy được | Test script | `save-branch: pushes the branch and says so`: mã 0, dòng `ĐÃ ĐẨY`, `git -C sb-origin.git rev-parse claude/T-999-thu` bằng đầu nhánh |
| Đối chứng (L-035) | Test script | `save-branch: a refused push is not reported as pushed`: hook từ chối; mã 3, không có dòng `ĐÃ ĐẨY`, bare repo không có nhánh. Trên khung rỗng test này thất bại (khung in `ĐÃ ĐẨY`), còn test trên qua: hai test dùng cùng repo, chỉ khác hook |
| Bundle khi không đẩy được | Test script | `save-branch: a refused push leaves a verified bundle`: có tệp `$work/rescue/claude__T-999-thu.bundle`, `git bundle verify` qua, `git bundle list-heads` có `refs/heads/claude/T-999-thu` với đúng mã commit |
| Bundle chỉ chứa phần mới | Test script | `save-branch: the bundle needs main as its base`: `git bundle verify` trong một bản sao không có `main` báo thiếu commit gốc (đối chứng rằng `--not origin/main` có tác dụng) |
| Lấy lại được | Test script | `save-branch: the printed commands restore the branch`: trong một bản sao mới của bare repo, chạy đúng lệnh `git fetch` mà script in ra; nhánh có đúng mã commit đầu và đủ 2 commit |
| Đoạn cho báo cáo | Test script | `save-branch: prints the rescue section`: có `Nhánh chưa đẩy`, tên nhánh, mã commit ngắn, `2 commit`, đường dẫn bundle |
| Chỉ bundle | Test script | `save-branch: --bundle-only never pushes`: hook ghi lại mỗi lần được gọi; sau `--bundle-only`, tệp ghi rỗng; có bundle; mã 3 |
| Lỗi mạng thì thử lại | Test script | `save-branch: a network error is retried, then bundled`: hook lỗi mạng, `SAVE_BRANCH_RETRY_SECONDS="0 0"`; hook được gọi 3 lần; có bundle; mã 3 |
| Lỗi mạng rồi được | Test script | `save-branch: a network error that clears is pushed`: hook lỗi lần đầu (đếm qua tệp), lần hai cho qua; mã 0, `ĐÃ ĐẨY` |
| Từ chối thì không thử lại | Test script | `save-branch: a refusal by GitHub is not retried`: hook từ chối; hook được gọi đúng 1 lần |
| Không có gì để giữ, vẫn chưa đẩy | Test script | `save-branch: a refused push with nothing beyond main exits 3 without a bundle`: nhánh trỏ đúng `main`, hook từ chối; mã 3, dòng `CHƯA ĐẨY` có `không cần bundle`, không có dòng `ĐÃ ĐẨY`, không có tệp. Đối chứng: cùng nhánh, không có hook thì mã 0 và `ĐÃ ĐẨY` (trên khung rỗng test này thất bại vì khung luôn trả 0) |
| Tham số sai | Test script | `save-branch: refuses a branch outside claude/`: `main`; mã 2, không đẩy (hook không được gọi) |
| Phiên dùng script | Kiểm tra thủ công | Người duyệt chéo đọc diff `docs/auto-run.md`; lần từ chối tới (nếu có), báo cáo phiên có mục "Nhánh chưa đẩy" |

**Ngoại lệ viết test trước:** `docs/auto-run.md`, `docs/templates/run.md`, `docs/lessons.md` là tài liệu, không có test.

### 4.1. Chạy thử

Không cần (E dưới 2): `git bundle` là lệnh git chuẩn, test của script đã dùng git thật trên repo tạm. Ghi thêm để người duyệt thấy cách lấy lại chạy được:

- **Đã chạy thử:** ngày 2026-10-24, trên máy dev (git 2.50.1), với 4 commit thật của T-017 (`9db10b7` đến `16db766`, gốc `660d006`).
- **Lệnh và kết quả:**
  1. `git bundle create t017.bundle 660d006..16db766` báo `fatal: Refusing to create empty bundle.`: bundle cần tên ref. Vì vậy script tạo bundle từ tên nhánh (`<nhánh> --not origin/main`).
  2. `git branch probe 16db766 && git bundle create t017.bundle probe ^660d006`: tệp 2 540 254 byte (4 commit có ảnh chuẩn PNG). `git bundle list-heads` cho `16db766e... refs/heads/probe`.
  3. Trong một bản sao của repo (có `660d006`): `git fetch t017.bundle "probe:claude/T-017-camera-diem-khop"` rồi `git log --oneline -5`: đủ 4 commit, đúng mã, nằm trên `660d006`.
  4. `git clone t017.bundle clone2` báo `error: 660d006... fatal: remote transport reported error`: bundle chỉ phần mới không clone được nếu không có gốc. Lệnh lấy lại vì vậy là `git fetch` trong bản sao có nhánh chính, không phải `git clone`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (quy trình phiên tự chạy): đổi cách phiên đẩy nhánh và xử lý khi không đẩy được (`docs/auto-run.md`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script mới, test, vài đoạn tài liệu |
| B. Độ rõ của yêu cầu | 1 | L-051 rõ; còn câu hỏi nơi giữ bundle (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Script có test trên repo thật; phần phiên làm theo tài liệu chỉ kiểm được khi bị từ chối lần nữa |
| D. Ảnh hưởng chéo | 2 | Mọi task của mọi phiên đẩy qua script này; `auto-run.md` cùng sửa với T-036, T-038, T-039, T-040 |
| E. Độ mới của kỹ thuật | 1 | `git bundle` lần đầu dùng, nhưng là lệnh git chuẩn, đã chạy thử (mục 4.1) |
| **Tổng** | 6 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `docs/auto-run.md`, `scripts/tests/run.sh`.
- Task khác có thể xung đột:
  - T-036 sửa `auto-run.md` mục 2 (dòng `cloud-env-check.sh`). Plan này không sửa mục 2.
  - T-039 sửa bước 5 và 8 (bảng tra lỗi), T-040 sửa bước 9. T-038 (bước 8, lỗi R8) làm trong phiên có người theo dõi trước phiên, đã có trên nhánh chính khi task này chạy.
  - T-043 (ADR-1003, đã merge, PR #91) vừa đổi bảng các bước (dòng bước 4), câu "Nội dung giao việc tối thiểu" (thêm "trừ phần nối chỗ gọi theo ADR-1003 ..., phiên con báo mọi file nối chỗ gọi") và câu "Phiên con xong: ... khớp mục 3.2 cộng phần nối chỗ gọi mà phiên con đã báo". Plan này thêm vào hai câu đó, giữ nguyên phần T-043 viết; câu "Xóa worktree sau khi merge" vẫn còn nguyên.
  - Plan này chỉ sửa bảng các bước (dòng bước 1), bước 3, Giao bước 3 đến 5, bước 7, đoạn mới sau bước 7, một dòng của bảng mục 4 và mục 5. Chỉ xung đột dòng khi gộp, không xung đột ý.
  - `scripts/tests/run.sh`: mỗi plan thêm một khối riêng; plan này thêm khối `save-branch` ở cuối file, trước dòng tổng kết `if [[ "$failures" -gt 0 ]]`.
- Đề xuất thứ tự chạy: phiên có người theo dõi (đổi quy trình phiên), trước hay sau các plan kia đều được.

## 7. Điều kiện dừng riêng

1. Test thật cho thấy hook `pre-receive` không mô phỏng được lỗi mạng (git in thông báo khác hẳn mọi chuỗi ở mục 3.1): dừng, hỏi người có bỏ phần thử lại khi lỗi mạng không.
2. Cần đổi một workflow (`.github/`) để giữ bundle: dừng, viết plan riêng.

## 8. Câu hỏi cho người duyệt

1. **Giữ bundle ở đâu?** Có ba chỗ đã xét:
   - **(a)** `$HOME/rescue/` trên máy cloud, kèm đoạn hướng dẫn trong báo cáo.
   - **(b)** Commit bundle vào nhánh báo cáo của phiên.
   - **(c)** Artifact của một workflow.

   Đề xuất của AI: (a).
   - (b) đưa tệp nhị phân hàng MB (2,5 MB với T-017) vào nhánh chính khi PR báo cáo merge. Nó cũng là một cách đưa đúng nội dung vừa bị từ chối lên GitHub qua một lệnh đẩy khác, tức là vòng qua lần từ chối.
   - (c) thì phiên cloud không tạo artifact được nếu không qua workflow, tức là phải đẩy trước.
   - (a) không lên GitHub, nhưng gom ở một chỗ biết trước, còn sau khi xóa worktree, và báo cáo cho người lệnh lấy lại ngay. Người vẫn phải vào phiên cloud trước khi máy bị thu hồi, như lần T-017.
2. **Mọi lần đẩy nhánh task đều qua script, hay chỉ khi lỗi?** Đề xuất của AI: mọi lần. Một lệnh duy nhất cho phiên chính và phiên con thì dễ làm đúng (L-051: phiên con quên đẩy sau mỗi commit). Đường thử lại khi lỗi mạng cũng luôn có.
3. **Lệnh đẩy lúc tạo nhánh bị từ chối ở hai task liên tiếp thì kết thúc phiên?** Đề xuất của AI: có. Mọi task đều phải đẩy mới qua CI được, nên làm tiếp chỉ tốn hạn mức. Một task bị từ chối thì vẫn làm task sau, vì lần T-017, sáu lệnh đẩy khác cùng phiên đều qua.
4. **Có tìm vì sao bộ phân loại từ chối không?** Đề xuất của AI: không trong plan này (ticket ghi ở Không bao gồm). Báo cáo phiên ghi nguyên văn lệnh bị từ chối và thông báo, để người gửi phản hồi về Claude Code nếu lặp lại.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1: Duyệt (Nhật, duyệt chéo Tuấn, 2026-10-24); ghi chú: Tuấn chấp nhận mọi đề xuất của AI ở mục 8. Thay bằng phiên bản 2 vì rà soát độc lập trước RUN-20261024-01 tìm script thoát 0 khi đẩy hỏng mà không cần bundle, và danh sách lỗi mạng không khớp hook của test (mục 1).

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-24, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Phiên bản 2.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261024-01, phiên bản plan 2

- **Kết quả:** đã merge
- **Nhánh, PR:** `claude/T-037-nhanh-task-khong-mat`, PR #105 (commit merge c151c43)
- **Đã làm:** `scripts/save-branch.sh` (đẩy, thử lại lỗi mạng theo lịch, bundle ở `$HOME/rescue/` khi không đẩy được, mã 3, `--bundle-only`); `auto-run.md` bước 1, 3, 7, Giao bước 3 đến 5, đoạn "Khi không đẩy được", mục 4, 5; mẫu danh sách chạy có mục "Nhánh chưa đẩy".
- **Test viết trước:** 30 test trên bare repo cục bộ. Khung rỗng luôn in `ĐÃ ĐẨY` và trả 0: 22 test thất bại ở phần khẳng định.
- **Sửa test sau khi viết (chi tiết, không đổi điều kiểm):** `git bundle verify` chạy trong bản sao của test (không phải thư mục gốc repo); đếm commit so với `origin/main`; khối test tắt `GIT_CONFIG_*` của đầu file để dùng bare repo thật.
- **Rà soát độc lập:** không chặn. Một điểm nên sửa đã xử lý: đoạn "Khi không đẩy được" chuyển xuống cuối bước 7.
- **Kết quả CI:** `check.sh` cục bộ qua; `pr-check` xanh.
- **Lệch so với plan:** `docs/lessons.md` không sửa (dòng Áp dụng của L-051 đã ghi T-037). Phương án dự phòng thư mục bundle (mục 3.4) chưa cài.
- **Việc còn lại:** không.
- **Cần người xem:** lần từ chối đẩy tới (nếu có) báo cáo có mục "Nhánh chưa đẩy".
