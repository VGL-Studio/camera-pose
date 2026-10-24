---
ticket: T-047
title: Mục "Sẵn sàng phát hành" trên trang tiến độ
status: approved             # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.3M         # cỡ S, số gợi ý của scripts/token-usage.py estimate
blocking_points: [ci]        # status.py chạy trong status-page.yml (contents: write), như T-034
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-24
---

# PLAN T-047: MỤC "SẴN SÀNG PHÁT HÀNH" TRÊN TRANG TIẾN ĐỘ

## 1. Tóm tắt

`scripts/status.py` sinh thêm mục "## 6. Sẵn sàng phát hành" ở cuối `docs/STATUS.md`. Chữ cố định của mục 6 không ghi mã bài học (`L-0NN`): test có sẵn `new-app.sh regenerates the progress page without the template's lessons` đếm `L-0[0-9][0-9]` trên trang của app mới phải bằng 0; test đó chỉ chạy ở dự án mẫu (`scripts/new-app.sh`), nên ràng buộc có tác dụng khi đưa về dự án mẫu (#133) (phiên bản 2). Mục này gom từ các nguồn đã có trong repo những việc còn phải xong trước bản production:

- vị trí quảng cáo cần mã thật, đọc từ `AdPlacement.kt`;
- mục "Cần người kiểm tra" chưa đánh dấu trong báo cáo các phiên;
- ticket chưa lên staging;
- phản hồi đang mở;
- đường dẫn tới issue đang mở trên GitHub.

Script không đọc cấu hình production, `scripts/release.sh` hay thư mục phát hành, và không gọi mạng. Trang vẫn sinh lại giống hệt trên cùng tài liệu. Kiểm chứng bằng test của `status.py` trong `scripts/tests/run.sh`, trên một repo giả có đủ các nguồn. Một test ghi lại mọi file mà script mở, để chứng minh script không đọc file production.

Chạy thử trên dữ liệu thật (mục 4.1): 38 mục kiểm thủ công chưa đánh dấu trong 8 báo cáo phiên.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là bài học L-064 (`docs/lessons.md`), từ báo cáo RUN-20261023-04. Đề xuất cùng nội dung đã gửi về dự án mẫu: VGL-Studio/app-project-template#133.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| L-064, quảng cáo | Khi `AdPlacement` có vị trí nào thì trang liệt kê khóa của vị trí đó và nhắc rằng bản production cần một mã thật cho mỗi khóa | L-064 đề xuất so "vị trí trong `AdPlacement` với mã đã cấu hình ở staging". So như vậy không cho biết gì: staging dùng mã thử của Google cho mọi vị trí (`GoogleTestAdUnitIds`, `StagingAdsModule`). Mã thật chỉ có lúc build bản release (`ConfiguredAdUnitIds`, ADR-0009), không nằm trong repo. Vì vậy trang liệt kê mọi khóa của `AdPlacement` (hiện có 4) và dẫn tới `docs/ops/release.md` mục 1.2, để người phát hành đối chiếu với trình quản lý mật khẩu. Không đọc `app/src/production/`, `release.sh`, `~/.vgl-release/` (Tuấn chọn cách này, mục 8 câu 1) |
| L-064, kiểm thủ công | Khi báo cáo phiên `finished` có mục "Cần người kiểm tra" còn `- [ ]` thì trang liệt kê mục đó theo phiên | Mục ghi "không có" ngay sau dấu hai chấm đầu tiên (mọi cách viết hoa thường) không phải việc, nên bỏ qua. Dòng chữ mẫu cũng bỏ qua: dòng còn `<...>` ở ngoài backtick sau khi xóa các đoạn trong backtick. Đường dẫn trong backtick như `` `feature/<tên>/src/test/screenshots/` `` là việc thật, giữ lại. Người đánh dấu `[x]` trong báo cáo khi đã kiểm, trang tự bỏ mục đó ở lần sinh sau. Mục có dòng con (dòng thụt lề ngay sau) hiện dòng cha kèm số dòng con |
| L-064, issue mở | Trang cho biết chỗ xem issue đang mở | `status.py` chạy không mạng. `status-page.yml` chỉ chạy `python3 scripts/status.py`, bước đó không có token đọc issue, và sửa workflow là plan rủi ro cao. Vì vậy trang chỉ ghi đường dẫn tới danh sách issue đang mở và issue `main-broken`. Đường dẫn dựng từ `<chủ>/<repo>` đọc trong `remote.origin.url`; đây là chỗ duy nhất script đọc ngoài `docs/` và `AdPlacement.kt`, ngoài cấu hình git của repo. Không đọc được remote thì ghi một câu không có đường dẫn (mục 8 câu 2) |
| L-064, ticket chưa xong | Khi có ticket chưa `on-staging`, `accepted`, `released` (và không `cancelled`) thì trang liệt kê ticket đó theo feature | Dùng lại `TICKET_DONE` của script. Nhóm ticket của mọi feature, kể cả feature quy trình như `quality-gate`; dòng tổng ghi "(gồm ticket quy trình)" khi có ticket không phải của feature người dùng thấy, không mã hóa cứng tên feature (xem mục 3.1 bước 5). Phản hồi `new`, `triaged`, `planned` (`FEEDBACK_OPEN`) cũng được liệt kê, vì đó là lỗi hay góp ý chưa xử lý |
| Không đọc cấu hình production | Script không mở file nào ngoài `docs/` và `AdPlacement.kt` | Test ghi mọi lệnh mở file của script, kèm test chuỗi đánh dấu (mục 4) |
| Giới hạn đã biết | Trang trên nhánh `status` có thể chậm một lần đẩy | `status-page.yml` chỉ chạy khi lần đẩy đổi `docs/**` hay `scripts/status.py`. Đổi riêng `AdPlacement.kt` thì trang chưa sinh lại, tới lần đẩy kế tiếp có đổi `docs/`. Vị trí quảng cáo mới luôn đi kèm spec và plan trong `docs/` (ADR-0013), nên thực tế ít gặp. Ghi giới hạn này vào `docs/CLAUDE.md`. Không sửa workflow |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0004, ADR-0009, ADR-0013 (vị trí quảng cáo và mã thật), ADR-0017 (phát hành do người làm).

## 3. Giải pháp

### 3.1. Cách làm

Giữ đúng mẫu của `status.py`: mỗi nguồn một hàm đọc trả danh sách dict, `render()` ghép trang, trang không có ngày giờ.

Mọi hàm đọc mới không được ném lỗi ra ngoài. `render()` chạy cả ở `--sync`, `--check` và trong `status-page.yml`; một báo cáo viết lệch không được làm hỏng cả trang. Lỗi đọc (`OSError`, `UnicodeDecodeError`, regex không khớp) thì bỏ phần đó và ghi một câu báo trên trang, ví dụ "Không đọc được `<file>`".

1. **`ad_placements()`**
   - Tìm `AdPlacement.kt` bằng `ROOT.glob("core/ads/src/main/**/AdPlacement.kt")`, lấy file đầu theo thứ tự đường dẫn.
   - Đọc các dòng `TÊN("khóa", AdFormat.ĐỊNH_DẠNG)` bằng regex `^\s+([A-Z_]+)\("([a-z0-9_]+)",\s*AdFormat\.([A-Z]+)\)`. Kết quả là danh sách `{"key", "format"}`.
   - Không có file thì trả rỗng, trang ghi "Không tìm thấy `AdPlacement.kt`". Có file mà không đọc được khóa nào thì ghi "Không đọc được khóa nào trong `AdPlacement.kt`".
2. **`manual_checks()`**: với mỗi báo cáo `runs/RUN-*.md` có `status: finished`, cắt đoạn từ dòng `### Cần người kiểm tra` tới tiêu đề `##` hay `###` kế tiếp, hoặc tới cuối file nếu không có tiêu đề nào. Mục rỗng thì không có gì.
   - **Mục:** lấy các dòng `- [ ] ...` ở đầu dòng.
   - **Dòng con:** dòng thụt lề (`  - ...`) ngay sau được đếm vào mục cha. Gặp dòng `- [x]`, `- [X]` hay bất kỳ dòng không thụt lề nào thì mục cha kết thúc, nên dòng con của một mục `[x]` không bị tính cho mục `- [ ]` đứng trước.
   - **Bỏ mục "không có":** lấy phần sau dấu hai chấm đầu tiên (cả dòng nếu không có dấu hai chấm); phần đó bắt đầu bằng "không có", không phân biệt hoa thường, thì bỏ.
   - **Bỏ chữ mẫu:** xóa mọi đoạn `` `...` `` của dòng, rồi tìm `<[^<>]+>`; còn thì là chữ mẫu và bỏ. Dòng mẫu của `docs/templates/run.md`, như `<màn hình, đường dẫn ảnh>`, đều có `<...>` ngoài backtick nên bị bỏ. Đường dẫn trong backtick được giữ.
   - **Kết quả:** `{"run", "path", "text", "children"}`. `text` qua `cell()`, dài hơn 160 ký tự thì cắt ở khoảng trắng gần nhất trước ký tự 160 nằm ngoài backtick, rồi thêm "...". Không có khoảng trắng như vậy thì cắt trước đoạn backtick đang mở.
3. **`issue_links()`**
   - Chạy `git -C ROOT config --get remote.origin.url` (`subprocess.run`, `capture_output=True`, `check=False`, timeout 5 giây, bắt mọi `OSError` và `TimeoutExpired`).
   - Lấy riêng `<chủ>/<repo>` bằng `github\.com[:/]([^/\s]+)/([^/\s]+?)(?:\.git)?/?$` và dựng `https://github.com/<chủ>/<repo>/issues?q=is%3Aissue+is%3Aopen` cùng `...+label%3Amain-broken`. Không bao giờ in URL gốc, nên `https://user:tok@github.com/o/r.git` không làm lộ `tok`. Trong mẫu, `[^/\s]+` của chủ repo đứng sau `github.com`, nên phần `user:tok@` nằm trước và bị bỏ.
   - Lỗi hay không khớp thì trả rỗng, trang ghi "Không đọc được repo GitHub từ `remote.origin.url`; xem issue trên trang của repo."
   - Trong `status-page.yml`, `actions/checkout` đặt `origin` là repo thật, nên trang trên nhánh `status` có đường dẫn đúng.
4. **`render()`** thêm mục cuối:

   ```text
   ## 6. Sẵn sàng phát hành

   <!-- Chỉ từ tài liệu trong repo. Mã quảng cáo thật, cấu hình production và tình trạng trên Google Play
   không nằm trong repo: người phát hành kiểm theo docs/ops/release.md. -->

   ### Quảng cáo
   - Bản production cần một mã quảng cáo thật cho mỗi khóa dưới đây (docs/ops/release.md, mục 1.2): `sample_bottom_banner` (banner), ...
   ### Kiểm thủ công chưa đánh dấu
   38 mục trong 8 phiên. Người kiểm xong thì đánh dấu [x] trong báo cáo phiên.
   - [RUN-20261024-01](runs/RUN-20261024-01.md): <mục> (2 dòng con)
   ### Ticket chưa lên staging
   12 ticket (gồm ticket quy trình).
   - <feature>: T-NNN `ready`, T-NNN `approved`
   ### Phản hồi đang mở
   - FB-... `triaged`, mức `nang`: <tên>
   ### Issue đang mở
   - [Mọi issue đang mở](...), [issue main-broken](...)
   ```

   - Nhóm nào không có gì thì ghi "Không có.".
   - Mục kiểm thủ công sắp theo phiên mới nhất trước, như mục 4 của trang. Tên phiên là liên kết tới báo cáo.
   - Mục 1 đến 5 của trang giữ nguyên, không đổi số mục.
5. **"(gồm ticket quy trình)"**: ghi khi có ticket chưa xong thuộc một feature không có `spec.md`, hoặc ticket có `spec:` rỗng. Đây là dấu hiệu của ticket quy trình, như T-036 đến T-047 của `quality-gate`. Không so tên feature.
6. **`docs/CLAUDE.md`**, mục Trang tiến độ: thêm một gạch đầu dòng gồm:
   - mục 6 lấy từ đâu;
   - người đánh dấu `[x]` vào báo cáo phiên khi đã kiểm xong một mục;
   - mã quảng cáo thật và cấu hình production không bao giờ hiện trên trang;
   - đổi riêng `AdPlacement.kt` thì trang chỉ sinh lại ở lần đẩy kế tiếp có đổi `docs/`.

Không sửa `.github/workflows/status-page.yml`: workflow vẫn chạy `python3 scripts/status.py` như cũ.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/status.py` | Thêm `ad_placements()`, `manual_checks()`, `issue_links()`, mục 6 trong `render()`; docstring đầu file thêm nguồn mới |
| Sửa | `scripts/tests/run.sh` | Khối test của `status.py`, sau dòng `expect "status: check passes on a fresh page"` và trước `# status.py --sync`: dựng thêm `AdPlacement.kt`, báo cáo phiên có mục Cần người kiểm tra, file production giả trong repo giả; các test ở mục 4 |
| Sửa | `docs/CLAUDE.md` | Mục Trang tiến độ: một gạch đầu dòng về mục 6 |

L-020, các file nhắc tới file mà plan sửa:

- `scripts/status.py` được đọc bởi:
  - `scripts/plan-check.py` (quy tắc 10, `sync_target()`);
  - `.github/workflows/status-page.yml` (chạy `python3 scripts/status.py`);
  - `docs/auto-run.md` (nhắc trang khi đổi số bài học).

  Plan không đổi `sync_target()`, tham số dòng lệnh hay tên file ra, nên không file nào phải đổi.
- `scripts/main-guard-status.sh` và `scripts/merge-if-green.sh` khớp tên `run.sh` chỉ vì nhắc `wait-run.sh`, không đọc `scripts/tests/run.sh`.
- `scripts/device-session.sh` và `scripts/required-assets.txt` chỉ nhắc `docs/CLAUDE.md` trong chú thích dẫn tới mục khác. Plan chỉ thêm một gạch đầu dòng vào mục Trang tiến độ, không đổi tên mục nào.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm. Chỉ đọc `AdPlacement.kt` để liệt kê khóa; không thêm, bớt hay đổi vị trí
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm. Chỉ dùng thư viện chuẩn của Python (`re`, `subprocess`, `pathlib`; test dùng thêm `runpy`, `sys.addaudithook`)

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `git -C <thư mục không phải repo> config --get remote.origin.url` trong repo giả của test. `scripts/tests/run.sh` đặt `GIT_CONFIG_*` toàn cục trỏ `origin` tới `https://github.com/o/r.git` (L-068), nên lệnh này có thể trả URL giả cả ngoài repo, hoặc trả lỗi.
- **Điều kiện chuyển:** test "issue link" cho kết quả khác nhau giữa macOS và CI, hoặc `git` báo lỗi ngoài repo.
- **Cách làm thay:** `issue_links()` đọc biến môi trường `STATUS_REPO_URL` trước, rồi mới tới `git`. Test đặt biến này rõ ràng. Hai test "không đọc được remote" và "URL có `user:tok@`" vẫn chạy qua đường `git`, hoặc qua `STATUS_REPO_URL` với cùng giá trị. Cùng file `scripts/status.py`, `scripts/tests/run.sh`; test ở mục 4 giữ nguyên.

## 4. Kiểm thử

Mọi test nằm trong khối `status.py` của `scripts/tests/run.sh` và dùng repo giả `$work/status-repo` (`STATUS_ROOT`). Phần dựng dữ liệu và các dòng `expect` vào commit test (bước 3). Trên `status.py` hiện tại, mọi test dưới đây thất bại vì trang chưa có mục 6. Hai test về file production qua ngay trên khung cũ, nên có thêm bước đối chứng.

**Báo cáo giả `RUN-20261004-01`** (`finished`, đã có trong khối test) được thêm mục "### Cần người kiểm tra" với các dòng sau, theo thứ tự:

| Dòng | Trên trang |
| --- | --- |
| `- [x] Đã xem ảnh` kèm hai dòng con `  - a`, `  - b` | Không (đã đánh dấu) |
| `- [ ] Kiểm FPS trên máy thật` | Có, không có dòng con |
| `` - [ ] Ảnh chuẩn mới: `feature/<tên>/src/test/screenshots/A.png` `` | Có |
| `- [ ] Ảnh chuẩn screenshot mới: <màn hình, đường dẫn ảnh>` (dòng mẫu) | Không |
| `- [ ] Test chập chờn: Không có (issue flaky không mở).` | Không |
| `- [ ] Ảnh chuẩn screenshot mới: không có (dừng trước bước ghi ảnh).` | Không |
| `- [ ] Tiêu chí kiểm tra thủ công (máy thật, người): không có` | Không |
| `- [ ] Kịch bản emulator lỗi hay bỏ qua: Không có` | Không |
| `- [ ] T-040: ghi chú, không có test cho trường hợp revert` | Có (dấu hai chấm đầu tiên đứng sau "T-040") |
| `- [ ] Điểm rà soát mức ghi chú:` kèm hai dòng con | Có, "(2 dòng con)" |

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Mục 6 có mặt, mục 1 đến 5 giữ nguyên | Test của script | `status: page ends with the release readiness section`: `grep -c '^## 6. Sẵn sàng phát hành$'` bằng 1, `grep -c '^## [1-5]\. '` vẫn bằng 5 |
| L-064, quảng cáo | Test của script | `status: release readiness lists every ad placement key`: `AdPlacement.kt` giả có `POSES_BANNER("poses_banner", AdFormat.BANNER)` và `REVIEW_INTERSTITIAL("review_interstitial", AdFormat.INTERSTITIAL)`. Dòng quảng cáo có cả hai khóa kèm `(banner)`, `(interstitial)` và dẫn `docs/ops/release.md` |
| L-064, quảng cáo, không có file | Test của script | `status: release readiness says when AdPlacement.kt is missing`: xóa file giả, sinh lại, có "Không tìm thấy" |
| L-064, kiểm thủ công | Test của script | `status: unchecked manual checks of finished runs are listed`: trang có đúng 4 mục của `RUN-20261004-01`, khớp cột "Trên trang" của bảng trên. Tên phiên là liên kết `(runs/RUN-20261004-01.md)` |
| Đường dẫn trong backtick là việc thật | Test của script | `status: a path with <...> inside backticks is kept`: có `feature/<tên>/src/test/screenshots/A.png` |
| Dòng mẫu bị bỏ | Test của script | `status: a template line is skipped`: không có `<màn hình, đường dẫn ảnh>` |
| Dòng con kết thúc ở `[x]` | Test của script | `status: children of a checked item are not counted`: "Kiểm FPS trên máy thật" không có "(dòng con)"; "Điểm rà soát mức ghi chú:" có "(2 dòng con)" |
| Dấu hai chấm đầu tiên | Test của script | `status: only the text after the first colon decides "không có"`: có dòng "T-040: ghi chú, không có test ..." |
| Mẫu báo cáo thật vẫn có định dạng mà script đọc | Test của script | `status: the run template still has the manual check section`: `docs/templates/run.md` có dòng `### Cần người kiểm tra` và ít nhất một dòng bắt đầu bằng `- [ ] ` sau dòng đó (chống việc đổi mẫu làm mục 6 mất mà test giả không thấy) |
| Bỏ phiên chưa kết thúc | Test của script | `status: manual checks skip runs not finished`: báo cáo `RUN-20261005-01` `running` có `- [ ] Kiểm A`, không có trên trang |
| Báo cáo viết lệch không làm hỏng trang | Test của script | `status: an empty or unterminated manual check section does not break the page`: báo cáo `finished` `RUN-20261006-01` có `### Cần người kiểm tra` ở cuối file (không có tiêu đề kế tiếp) với `- [ ] Kiểm B`; báo cáo `RUN-20261006-02` có mục rỗng ngay trước `### Task bị dừng`. Script thoát 0, có "Kiểm B", `--check` qua |
| Đánh dấu thì mục biến mất | Test của script | `status: a checked item leaves the page`: đổi `- [ ] Kiểm FPS` thành `- [x]`, sinh lại, mục đó không còn |
| L-064, ticket chưa xong | Test của script | `status: tickets not on staging are listed by feature`: repo giả có T-001 `on-staging` và T-002 `approved`. Dòng của `demo` có `T-002 .approved.`, không có `T-001`. Thêm feature `proc` không có `spec.md` với một ticket `ready`: dòng tổng có "(gồm ticket quy trình)" |
| L-064, phản hồi mở | Test của script | `status: open feedback is listed in release readiness`: FB-20261003-01 `new` có trong mục 6 |
| L-064, issue | Test của script | `status: release readiness links open issues of the origin repo`: với `GIT_CONFIG_*` của `run.sh` (`https://github.com/o/r.git`), trang có `https://github.com/o/r/issues?q=is%3Aissue+is%3Aopen` và `label%3Amain-broken` |
| Không đọc được remote | Test của script | `status: without a readable remote there is a sentence and no link`: chạy với `GIT_CONFIG_COUNT=0 GIT_CONFIG_GLOBAL=/dev/null`. Trang có "Không đọc được repo GitHub" và không có `github.com/o/r` |
| URL có thông tin đăng nhập | Test của script | `status: a remote URL with credentials does not leak them`: `GIT_CONFIG_VALUE_0=https://user:tok@github.com/o/r.git`. Trang có `https://github.com/o/r/issues`, `grep -c tok` bằng 0 |
| Trang không đổi giữa hai lần chạy | Test của script | `status: check passes after the release readiness section`: thêm một lần `--check` sau khi dựng các nguồn mới |
| Không đọc cấu hình production: file đã mở | Test của script, có đối chứng | `status: release readiness opens only docs and AdPlacement.kt`. Một lệnh `python3 -I -c` đăng ký `sys.addaudithook` ghi đường dẫn của mọi sự kiện `open`, rồi chạy `scripts/status.py` bằng `runpy.run_path(..., run_name="__main__")` với `STATUS_ROOT` của repo giả, và ghi danh sách khi thoát (`atexit`). Kiểm: mọi đường dẫn mở bên dưới `STATUS_ROOT` đều nằm trong `docs/` hoặc kết thúc bằng `/AdPlacement.kt`; không đường dẫn nào chứa `app/src/production` hay `release`. File thư viện của Python nằm ngoài `STATUS_ROOT` nên không xét. Đối chứng: danh sách có `docs/lessons.md` và `AdPlacement.kt`, chứng tỏ hook ghi được |
| Không đọc cấu hình production: nội dung | Test của script, có đối chứng | `status: release readiness never reads production config`: repo giả có `app/src/production/ProdAds.kt` và `release.properties` ở gốc, chứa `PROD-MARKER-T047`; `grep -c PROD-MARKER-T047` trên trang bằng 0. Đối chứng: cùng chuỗi đặt trong một mục `- [ ]` của báo cáo phiên thì có trên trang (bằng 1) |
| Dự án mẫu và app mới | Test có sẵn | `new-app.sh leaves pages that match the documents` vẫn qua: app mới có `AdPlacement.kt` của mẫu và không có báo cáo phiên |
| Trang thật | Kiểm tra thủ công | Sau khi merge, mở `docs/STATUS.md` trên nhánh `status`. Mục 6 có 4 khóa quảng cáo, các mục kiểm thủ công của RUN-20261024-01 và đường dẫn issue của `VGL-Studio/camera-pose` |

**Ngoại lệ viết test trước:** câu thêm vào `docs/CLAUDE.md` (tài liệu, không có hành vi để test).

### 4.1. Chạy thử

- **Đã chạy thử:** đọc mục "Cần người kiểm tra" của mọi báo cáo thật bằng một đoạn Python tạm, không commit, ngày 2026-10-24, trên nhánh chính 795028c.
- **Lệnh:** `python3 - <<'EOF' ... EOF` với các quy tắc của mục 3.1 bước 2.
- **Kết quả:**
  - 9 báo cáo `finished` đều có mục này; RUN-20261021-04 (`draft`) không có.
  - 48 dòng `- [ ]` và 6 dòng `[x]`; chỉ RUN-20261022-01 có đánh dấu.
  - Bỏ 10 dòng "không có" (mọi cách viết hoa thường), không dòng nào bị bỏ vì chữ mẫu. **Còn 38 mục trong 8 phiên**; RUN-20261023-03 không còn mục nào.
  - Hai việc thật có `<...>` trong backtick được giữ: đường dẫn R2 `borrowed/poses/img/v1/<w01,...>/...` của RUN-20261021-01, và `feature/<tên>/src/test/screenshots/` của RUN-20261023-04.
  - Lần đầu chạy, bản bỏ mọi dòng có `<` và `>` làm mất hai việc này và chỉ còn 36 mục. Bản xét chữ hoa còn giữ "Tiêu chí kiểm tra thủ công: không có". Cả hai đã sửa ở mục 3.1.
  - Có một dòng bị bỏ đúng quy tắc mà vẫn mang thông tin: RUN-20261023-04, "Test chập chờn: không có issue `flaky`. `ui-test` của T-022 đỏ hai lần vì R2 hết giờ chờ ...". Phần sau là ghi chú, đã thành bài học L-050 (applied), nên chấp nhận bỏ.
  - Nhiều mục cũ (T-007 ở RUN-20261021-*) có lẽ đã xử lý nhưng không ai đánh dấu; người rà một lần (mục 8 câu 3).
- Tiêu chí E dưới 2, không có kỹ thuật mới, nên không bắt buộc chạy thử bằng `scripts/check.sh`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** `ci` (điểm chặn 7 của `risk-scoring.md`). `scripts/status.py` chạy trong `.github/workflows/status-page.yml`, workflow có quyền `contents: write` và đẩy nhánh `status`. Script hỏng hay đọc nhầm file thì ảnh hưởng thẳng một workflow có quyền ghi. Tuấn xếp Cao ngày 2026-10-24, nhất quán với T-034 (tạo `status-page.yml`). Rủi ro Cao, bắt buộc duyệt chéo; không tính tổng điểm.

Điểm tham khảo khi không xét điểm chặn:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script, khối test của nó, một câu tài liệu |
| B. Độ rõ của yêu cầu | 1 | Các câu hỏi về nguồn đã được Tuấn trả lời (mục 8); còn chi tiết nhỏ AI tự quyết: cắt chữ, câu báo lỗi |
| C. Khả năng kiểm chứng tự động | 1 | Mọi nguồn có test trên repo giả, kể cả file đã mở; trang thật trên nhánh `status` xem bằng mắt |
| D. Ảnh hưởng chéo | 2 | `scripts/tests/run.sh` mà T-044, T-045, T-046 cũng sửa; định dạng báo cáo mà T-045, T-046 đổi mẫu; trang do `status-page.yml` dùng |
| E. Độ mới của kỹ thuật | 1 | Biến thể của các hàm đọc tài liệu có sẵn; `subprocess` gọi `git` và `sys.addaudithook` lần đầu trong test của `status.py` |
| **Tổng** | **6 (tham khảo)** | **Mức: Cao (điểm chặn `ci`)** |

## 6. Ảnh hưởng chéo

- **File dùng chung bị chạm:**
  - `scripts/tests/run.sh`: chỉ khối `status.py`, giữa dòng `expect "status: check passes on a fresh page"` và chú thích `# status.py --sync moves each ticket's status after its plan's (L-019).`
  - `docs/CLAUDE.md`: chỉ mục Trang tiến độ.
  - Không sửa `docs/auto-run.md`, `docs/templates/run.md` hay `.github/`.
- **Task có thể xung đột:**
  - **T-044** (`scripts/wait-run.sh`, khối test `wait-run` của `scripts/tests/run.sh`): khác đoạn, không phụ thuộc.
  - **T-045:** chỉ thêm khóa `plan_queue` vào mẫu danh sách chạy và mục "### Plan soạn sẵn và câu hỏi cho người" vào báo cáo; không đổi mục "Cần người kiểm tra". `manual_checks()` dừng ở tiêu đề `###` kế tiếp, nên mục mới của T-045 đứng sau không bị đọc vào. Muốn đưa câu hỏi của T-045 lên trang thì làm ở ticket sau.
  - **T-046:** đổi dòng mẫu "Tiêu chí kiểm tra thủ công" thành "Tiêu chí kiểm tra thủ công (máy thật, người)" và thêm dòng "Kịch bản emulator lỗi hay bỏ qua" vào mục "Cần người kiểm tra" của `docs/templates/run.md`. Cả hai vẫn là `- [ ] <tên>: ...`; khi ghi "không có" thì bị bỏ (báo cáo giả có hai dòng này). Test "mẫu báo cáo thật" của T-047 đọc `docs/templates/run.md` thật, nên chạy sau T-046 thì kiểm luôn mẫu mới.
  - Không plan nào trong ba plan trên ghi Tạo một file mà T-047 đọc.
- **Liên quan dự án mẫu:** đề xuất cùng nội dung VGL-Studio/app-project-template#133.
- **Đề xuất thứ tự chạy:** T-046 trước T-047; T-045 trước T-047. T-047 độc lập với T-044.

## 7. Điều kiện dừng riêng

- Cần đọc bất kỳ file nào trong `app/src/production/`, `scripts/release.sh`, `release-output/`, `~/.vgl-release/` hay bí mật để làm xong một tiêu chí: dừng task, không tìm đường khác. Hai trường hợp không tính: file giả trong repo giả của test (`$work/status-repo/app/src/production/ProdAds.kt`, `release.properties`) do chính test tạo; và tên file hiện ra trong kết quả tìm kiếm (`grep -l`, `ls`) mà không mở nội dung.
- Cần sửa `.github/workflows/status-page.yml` (ví dụ để lấy danh sách issue bằng token): dừng task. Việc đó cần plan rủi ro cao riêng.

## 8. Câu hỏi cho người duyệt

Tuấn đã trả lời ngày 2026-10-24, theo đề xuất của rà soát độc lập:

1. **Mã quảng cáo thật.** AI không thể biết vị trí nào đã có mã thật, vì mã nằm trong trình quản lý mật khẩu (ADR-0009). Có hai cách:
   - (a) chỉ liệt kê mọi khóa của `AdPlacement` kèm lời nhắc;
   - (b) thêm một file do người giữ ghi "đã có mã: có/chưa".

   **Trả lời:** (a). Liệt kê mọi khóa và dẫn `docs/ops/release.md` mục 1.2. Khác ý "so với mã staging" của L-064: staging luôn dùng mã thử của Google cho mọi vị trí, nên so như vậy không cho biết gì (mục 2).
2. **Issue đang mở.** Chỉ để đường dẫn, hay mở ticket để `status-page.yml` đếm số issue?

   **Trả lời:** chỉ để đường dẫn, không mở ticket đếm số.
3. **Mục kiểm thủ công cũ chưa đánh dấu (38 mục).** Giữ hết, hay chỉ hiện 3 phiên gần nhất?

   **Trả lời:** giữ đủ, không lọc 3 phiên. Người rà một lần và đánh dấu các mục đã xong; AI không đánh dấu thay người.
4. **Mức rủi ro.** **Trả lời:** Cao, điểm chặn `ci`, duyệt chéo, nhất quán với T-034 (mục 5).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1: Duyệt (Nhật, duyệt chéo Tuấn, 2026-10-24); ghi chú: Ngày 2026-10-24, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Rà soát độc lập (phiên mới, chỉ đọc) trước khi duyệt; mọi điểm chặn và nên sửa đã sửa trong bản nháp. Rủi ro Cao, duyệt chéo (Tuấn chọn). Thay bằng phiên bản 2 vì rà soát độc lập trước RUN-20261024-02 (docs/ops/pre-run-review.md, mục 2) tìm chữ của mục 6 có thể ghi mã bài học, làm đỏ test có sẵn `new-app.sh regenerates the progress page without the template's lessons` (đếm `L-0[0-9][0-9]` trên trang của app mới phải bằng 0), và dữ liệu giả dùng tên dòng mẫu khác plan T-046.

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-24, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Phiên bản 2 sửa theo hai lượt rà soát độc lập trước RUN-20261024-02 (lượt 2: không còn điểm chặn); Tuấn đồng ý duyệt.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
