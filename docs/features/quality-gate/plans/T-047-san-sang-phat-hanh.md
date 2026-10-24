---
ticket: T-047
title: Mục "Sẵn sàng phát hành" trên trang tiến độ
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 7                # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: medium           # low | medium | high
token_estimate: 5.3M         # cỡ S, số gợi ý của scripts/token-usage.py estimate
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-047: MỤC "SẴN SÀNG PHÁT HÀNH" TRÊN TRANG TIẾN ĐỘ

## 1. Tóm tắt

`scripts/status.py` sinh thêm mục "## 6. Sẵn sàng phát hành" ở cuối `docs/STATUS.md`. Mục này gom từ các nguồn đã có trong repo những việc còn phải xong trước bản production:

- vị trí quảng cáo cần mã thật (đọc từ `AdPlacement.kt`);
- mục "Cần người kiểm tra" chưa đánh dấu trong báo cáo các phiên;
- ticket chưa lên staging;
- phản hồi đang mở;
- đường dẫn tới issue đang mở trên GitHub.

Script không đọc cấu hình production, `scripts/release.sh` hay thư mục phát hành, và không gọi mạng. Trang vẫn sinh lại giống hệt trên cùng tài liệu. Kiểm chứng bằng test của `status.py` trong `scripts/tests/run.sh`, trên một repo giả có đủ các nguồn. Chạy thử trên dữ liệu thật (mục 4.1): 36 mục kiểm thủ công chưa đánh dấu trong 8 báo cáo phiên.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là bài học L-064 (`docs/lessons.md`), từ báo cáo RUN-20261023-04.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| L-064, quảng cáo | Khi `AdPlacement` có vị trí nào thì trang liệt kê khóa của vị trí đó và nhắc rằng bản production cần một mã thật cho mỗi khóa | Mã thật không nằm trong repo (ADR-0009, `AdUnitIds.kt`: `ConfiguredAdUnitIds` nhận mã lúc build bản release). Vì vậy AI không biết vị trí nào đã có mã. Trang chỉ liệt kê mọi khóa của `AdPlacement` (hiện có 4) và dẫn tới `docs/ops/release.md` mục 1.2, để người đối chiếu với trình quản lý mật khẩu. Lần đầu có vị trí mới, khóa của nó hiện ngay trên trang. Không đọc `app/src/production/`, `release.sh`, `~/.vgl-release/` (câu hỏi 1) |
| L-064, kiểm thủ công | Khi báo cáo phiên `finished` có mục "Cần người kiểm tra" còn `- [ ]` thì trang liệt kê mục đó theo phiên | Mục ghi "Không có" (mọi cách viết: "Không có.", "không có (T-007 dừng ...)", "Test chập chờn: Không có (...)") không phải việc, nên bỏ qua. Dòng còn chữ mẫu `<...>` cũng bỏ qua. Người đánh dấu `[x]` trong báo cáo khi đã kiểm, trang tự bỏ mục đó ở lần sinh sau. Mục có dòng con (ví dụ "Điểm rà soát mức ghi chú:" rồi các dòng `  - ...`) hiện dòng cha kèm số dòng con |
| L-064, issue mở | Trang cho biết chỗ xem issue đang mở | `status.py` chạy không mạng (`status-page.yml` chỉ chạy `python3 scripts/status.py`, không có token đọc issue trong bước đó; sửa workflow là plan rủi ro cao). Trang ghi đường dẫn tới danh sách issue đang mở và issue `main-broken`, lấy chủ repo từ `remote.origin.url`. Không đọc được remote thì ghi câu không có đường dẫn (câu hỏi 2) |
| L-064, ticket chưa xong | Khi có ticket chưa `on-staging`, `accepted`, `released` (và không `cancelled`) thì trang liệt kê ticket đó theo feature | Dùng lại `TICKET_DONE` của script. Phản hồi `new`, `triaged`, `planned` (`FEEDBACK_OPEN`) cũng được liệt kê, vì đó là lỗi hay góp ý chưa xử lý |
| Không đọc cấu hình production | Script không mở file nào ngoài `docs/` và `AdPlacement.kt` | Test dựng repo giả có thêm `app/src/production/` và file `release.properties` chứa chuỗi đánh dấu; trang không được có chuỗi đó |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0004, ADR-0009, ADR-0013 (vị trí quảng cáo và mã thật), ADR-0017 (phát hành do người làm).

## 3. Giải pháp

### 3.1. Cách làm

Giữ đúng mẫu của `status.py`: mỗi nguồn một hàm đọc trả danh sách dict, `render()` ghép trang, không có ngày giờ trên trang.

1. **`ad_placements()`**: tìm `AdPlacement.kt` bằng `ROOT.glob("core/ads/src/main/**/AdPlacement.kt")` (lấy file đầu theo thứ tự đường dẫn) và đọc các dòng `TÊN("khóa", AdFormat.ĐỊNH_DẠNG)` bằng regex `^\s+([A-Z_]+)\("([a-z0-9_]+)",\s*AdFormat\.([A-Z]+)\)`. Kết quả là danh sách `{"key", "format"}`. Không có file thì trả rỗng, và trang ghi "Không tìm thấy `AdPlacement.kt`".
2. **`manual_checks()`**: với mỗi báo cáo `runs/RUN-*.md` có `status: finished`, cắt đoạn từ `### Cần người kiểm tra` tới tiêu đề `##` hay `###` kế tiếp.
   - Lấy các dòng `- [ ] ...` ở đầu dòng (không thụt lề). Dòng con thụt lề `  - ...` ngay sau được đếm vào mục cha.
   - Bỏ mục mà phần sau dấu hai chấm cuối cùng (hay cả dòng nếu không có dấu hai chấm) bắt đầu bằng "không có", không phân biệt hoa thường. Bỏ cả mục còn `<` và `>` của chữ mẫu.
   - Kết quả là `{"run", "path", "text", "children"}`; `text` cắt ở 160 ký tự, thêm "..." nếu dài hơn, qua `cell()`.
3. **`issue_links()`**: chạy `git -C ROOT config --get remote.origin.url` (`subprocess.run`, `check=False`, timeout 5 giây) và đọc `github.com[:/]<chủ>/<repo>(.git)?`. Có thì trả hai đường dẫn: `https://github.com/<chủ>/<repo>/issues?q=is%3Aissue+is%3Aopen` và `...+label%3Amain-broken`. Lỗi hay không khớp thì trả rỗng. Trong `status-page.yml`, `actions/checkout` đặt `origin` là repo thật, nên trang trên nhánh `status` có đường dẫn đúng.
4. **`render()`** thêm mục cuối:

   ```text
   ## 6. Sẵn sàng phát hành

   <!-- Chỉ từ tài liệu trong repo. Mã quảng cáo thật, cấu hình production và tình trạng trên Google Play
   không nằm trong repo: người phát hành kiểm theo docs/ops/release.md. -->

   ### Quảng cáo
   - Bản production cần một mã quảng cáo thật cho mỗi khóa dưới đây (docs/ops/release.md, mục 1.2): `sample_bottom_banner` (banner), ...
   ### Kiểm thủ công chưa đánh dấu
   - RUN-20261024-01: <mục> (2 dòng con)
   ... (Người kiểm xong thì đánh dấu [x] trong báo cáo phiên.)
   ### Ticket chưa lên staging
   - <feature>: T-NNN `ready`, T-NNN `approved`
   ### Phản hồi đang mở
   - FB-... `triaged`, mức `nang`: <tên>
   ### Issue đang mở
   - [Mọi issue đang mở](...), [issue main-broken](...)
   ```

   Nhóm nào không có gì thì ghi "Không có.". Mục kiểm thủ công sắp theo phiên mới nhất trước, như mục 4 của trang. Mỗi nhóm có dòng tổng ở đầu (ví dụ "36 mục trong 8 phiên"). Mục 1 đến 5 của trang giữ nguyên, không đổi số mục.
5. **`docs/CLAUDE.md`**, mục Trang tiến độ: thêm một gạch đầu dòng nói mục 6 lấy từ đâu; người đánh dấu `[x]` vào báo cáo phiên khi đã kiểm xong một mục; mã quảng cáo thật và cấu hình production không bao giờ hiện trên trang.

Không sửa `.github/workflows/status-page.yml`: workflow vẫn chạy `python3 scripts/status.py` như cũ.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/status.py` | Thêm `ad_placements()`, `manual_checks()`, `issue_links()`, mục 6 trong `render()`; docstring đầu file thêm nguồn mới |
| Sửa | `scripts/tests/run.sh` | Khối test của `status.py` (sau dòng `expect "status: check passes on a fresh page"`, trước `# status.py --sync`): dựng thêm `AdPlacement.kt`, báo cáo phiên có mục Cần người kiểm tra, file production giả trong repo giả; các test ở mục 4 |
| Sửa | `docs/CLAUDE.md` | Mục Trang tiến độ: một gạch đầu dòng về mục 6 |

L-020, các file nhắc tới file mà plan sửa:
- `scripts/status.py` được đọc bởi `scripts/plan-check.py` (quy tắc 10, `sync_target()`), `.github/workflows/status-page.yml` (chạy `python3 scripts/status.py`) và `docs/auto-run.md` (nhắc trang khi đổi số bài học). Plan không đổi `sync_target()`, tham số dòng lệnh hay tên file ra, nên không file nào phải đổi.
- `scripts/main-guard-status.sh` và `scripts/merge-if-green.sh` khớp tên `run.sh` chỉ vì nhắc `wait-run.sh`, không đọc `scripts/tests/run.sh`.
- `scripts/device-session.sh` và `scripts/required-assets.txt` chỉ nhắc `docs/CLAUDE.md` trong chú thích dẫn tới mục khác. Plan chỉ thêm một gạch đầu dòng vào mục Trang tiến độ, không đổi tên mục nào.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm. Chỉ đọc `AdPlacement.kt` để liệt kê khóa; không thêm, bớt hay đổi vị trí
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm. Chỉ dùng thư viện chuẩn của Python (`re`, `subprocess`, `pathlib`)

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `git -C <thư mục không phải repo> config --get remote.origin.url` trong repo giả của test. `scripts/tests/run.sh` đặt `GIT_CONFIG_*` toàn cục trỏ `origin` tới `https://github.com/o/r.git` (L-068), nên lệnh này có thể trả URL giả cả ngoài repo, hoặc trả lỗi.
- **Điều kiện chuyển:** test "issue link" cho kết quả khác nhau giữa macOS và CI, hoặc `git` báo lỗi ngoài repo.
- **Cách làm thay:** `issue_links()` đọc biến môi trường `STATUS_REPO_URL` trước, rồi mới tới `git`. Test đặt biến này rõ ràng, và một test riêng kiểm rằng không có biến lẫn remote thì trang ghi câu không có đường dẫn. Cùng file `scripts/status.py`, `scripts/tests/run.sh`; test ở mục 4 giữ nguyên.

## 4. Kiểm thử

Mọi test nằm trong khối `status.py` của `scripts/tests/run.sh` và dùng repo giả `$work/status-repo` (`STATUS_ROOT`). Phần dựng dữ liệu và các dòng `expect` vào commit test (bước 3). Trên `status.py` hiện tại, mọi test dưới đây thất bại vì trang chưa có mục 6. Riêng test "không đọc file production" qua ngay trên khung cũ, nên có thêm bước đối chứng (dòng cuối bảng).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Mục 6 có mặt, mục 1 đến 5 giữ nguyên | Test của script | `status: page ends with the release readiness section`: `grep -c '^## 6. Sẵn sàng phát hành$'` bằng 1, và `grep -c '^## [1-5]\. '` vẫn bằng 5 |
| L-064, quảng cáo | Test của script | `status: release readiness lists every ad placement key`: `AdPlacement.kt` giả có `POSES_BANNER("poses_banner", AdFormat.BANNER)` và `REVIEW_INTERSTITIAL("review_interstitial", AdFormat.INTERSTITIAL)`; dòng quảng cáo có cả hai khóa kèm `(banner)`, `(interstitial)` và dẫn `docs/ops/release.md` |
| L-064, quảng cáo, không có file | Test của script | `status: release readiness says when AdPlacement.kt is missing`: xóa file giả, sinh lại, có "Không tìm thấy" |
| L-064, kiểm thủ công | Test của script | `status: unchecked manual checks of finished runs are listed`: báo cáo giả `RUN-20261004-01` (`finished`) có mục "### Cần người kiểm tra" với 1 dòng `- [ ] Kiểm FPS trên máy thật`, 1 dòng `- [x] Đã xem ảnh`, 1 dòng `- [ ] Test chập chờn: Không có (issue flaky không mở).`, 1 dòng `- [ ] Ảnh chuẩn screenshot mới: không có (dừng trước bước ghi ảnh).`, 1 dòng `- [ ] Điểm rà soát mức ghi chú:` có hai dòng con; trang có đúng 2 mục của phiên đó: "Kiểm FPS trên máy thật" và "Điểm rà soát mức ghi chú: (2 dòng con)" |
| Bỏ phiên chưa kết thúc và chữ mẫu | Test của script | `status: manual checks skip runs not finished and template text`: báo cáo `RUN-20261005-01` `running` có `- [ ] Kiểm A`, và báo cáo `finished` có `- [ ] Ảnh chuẩn: <màn hình, đường dẫn>`; cả hai không có trên trang |
| Đánh dấu thì mục biến mất | Test của script | `status: a checked item leaves the page`: đổi `- [ ] Kiểm FPS` thành `- [x]`, sinh lại, mục đó không còn |
| L-064, ticket chưa xong | Test của script | `status: tickets not on staging are listed by feature`: repo giả có T-001 `on-staging` và T-002 `approved`; dòng của `demo` có `T-002 .approved.` và không có `T-001` |
| L-064, phản hồi mở | Test của script | `status: open feedback is listed in release readiness`: FB-20261003-01 `new` có trong mục 6 |
| L-064, issue | Test của script | `status: release readiness links open issues of the origin repo`: với `GIT_CONFIG_*` của `run.sh` (`https://github.com/o/r.git`), trang có `https://github.com/o/r/issues?q=is%3Aissue+is%3Aopen` và `label%3Amain-broken` |
| Trang không đổi giữa hai lần chạy | Test của script | `status: check passes after the release readiness section` (đã có test `--check` trên trang mới; thêm một lần `--check` sau khi dựng các nguồn mới) |
| Không đọc cấu hình production | Test của script, có đối chứng | `status: release readiness never reads production config`: repo giả có `app/src/production/ProdAds.kt` và `release.properties` ở gốc, chứa `PROD-MARKER-T047`; `grep -c PROD-MARKER-T047` trên trang bằng 0. Đối chứng: cùng chuỗi đặt trong một mục `- [ ]` của báo cáo phiên thì có trên trang (bằng 1), chứng tỏ test đọc đúng trang |
| Dự án mẫu và app mới | Test có sẵn | `new-app.sh leaves pages that match the documents` vẫn qua: app mới có `AdPlacement.kt` của mẫu và không có báo cáo phiên |
| Trang thật | Kiểm tra thủ công | Sau khi merge, mở `docs/STATUS.md` trên nhánh `status`; mục 6 có 4 khóa quảng cáo, các mục kiểm thủ công của RUN-20261024-01 và đường dẫn issue của `VGL-Studio/camera-pose` |

**Ngoại lệ viết test trước:** câu thêm vào `docs/CLAUDE.md` (tài liệu, không có hành vi để test).

### 4.1. Chạy thử

- **Đã chạy thử:** đọc mục "Cần người kiểm tra" của mọi báo cáo thật bằng một đoạn Python tạm (không commit), 2026-10-24, nhánh chính 795028c.
- **Lệnh:** `python3 - <<'EOF' ... EOF` với regex của mục 3.1 bước 2 (bản đầu, chỉ khớp "Không có" có chữ hoa).
- **Kết quả:**
  - 9 báo cáo `finished` đều có mục này. RUN-20261021-04 (`draft`) không có.
  - Có 48 dòng `- [ ]` và 6 dòng `[x]` (chỉ RUN-20261022-01 có đánh dấu).
  - Bản đầu còn 40 mục, vì vẫn giữ "Tiêu chí kiểm tra thủ công: không có" và "Ảnh chuẩn screenshot mới: không có (T-007 dừng ...)", vì regex chỉ khớp chữ hoa. Mục 3.1 đã sửa thành không phân biệt hoa thường và xét phần sau dấu hai chấm cuối cùng; chạy lại thì còn 36 mục trong 8 phiên (RUN-20261023-03 không còn mục nào). Test ở mục 4 có cả hai cách viết.
  - Nhiều mục cũ (T-007 ở RUN-20261021-*) có lẽ đã xử lý nhưng không ai đánh dấu (câu hỏi 3).
- Tiêu chí E dưới 2: không có kỹ thuật mới, nên không bắt buộc chạy thử bằng `scripts/check.sh`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có. Script chỉ đọc `AdPlacement.kt`, không đổi vị trí quảng cáo (điểm 1). `status.py` sinh trang tiến độ, không phải cổng chất lượng hay bước của phiên tự chạy (điểm 7), và không sửa `.github/`. Người duyệt thấy khác thì xếp Cao (câu hỏi 4).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script, khối test của nó, một câu tài liệu |
| B. Độ rõ của yêu cầu | 2 | Nguồn "mã quảng cáo" và "issue mở" không đọc được trực tiếp trong repo; cách thay ở mục 2 cần người duyệt đồng ý (câu hỏi 1, 2, 3) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi nguồn có test trên repo giả; trang thật trên nhánh `status` xem bằng mắt |
| D. Ảnh hưởng chéo | 2 | `scripts/tests/run.sh` mà T-044, T-045, T-046 cũng có thể sửa; trang do `status-page.yml` dùng |
| E. Độ mới của kỹ thuật | 1 | Biến thể của các hàm đọc tài liệu có sẵn; `subprocess` gọi `git` lần đầu trong `status.py` |
| **Tổng** | **7** | **Mức: Trung bình** |

## 6. Ảnh hưởng chéo

- **File dùng chung bị chạm:**
  - `scripts/tests/run.sh`: chỉ khối `status.py`, giữa dòng `expect "status: check passes on a fresh page"` và chú thích `# status.py --sync moves each ticket's status after its plan's (L-019).`
  - `docs/CLAUDE.md`: chỉ mục Trang tiến độ.
  - Không sửa `docs/auto-run.md`.
- **Task có thể xung đột:**
  - **T-044** (`scripts/wait-run.sh`, khối test `wait-run` của `scripts/tests/run.sh`): khác đoạn, không phụ thuộc.
  - **T-045** (`docs/auto-run.md` cuối phiên, `docs/templates/run.md`): T-047 đọc mục "### Cần người kiểm tra" của báo cáo phiên và dòng `- [ ]`. T-045 thêm mục câu hỏi cho người vào mẫu báo cáo thì giữ tên mục "Cần người kiểm tra" và kiểu `- [ ]`. Đổi tên mục thì `manual_checks()` không còn thấy, và test của T-047 không bắt được vì nó dùng báo cáo giả. Nếu T-045 thêm một mục riêng (ví dụ "Câu hỏi cho người"), có thể đưa mục đó vào trang ở ticket sau.
  - **T-046** (`docs/templates/plan.md`, kịch bản `device-session.yml`): tiêu chí kiểm thủ công "máy thật" vẫn được ghi vào mục "Cần người kiểm tra" của báo cáo với `- [ ]`, nên trang vẫn thấy. Phần "emulator" mà phiên tự kiểm xong thì không còn trong mục đó. Không phụ thuộc file.
  - Các plan này viết song song, chưa plan nào ghi Tạo một file mà T-047 đọc.
- **Đề xuất thứ tự chạy:** không bắt buộc. Nếu cùng danh sách thì T-045 trước T-047, để mẫu báo cáo mới đã có khi viết test; T-047 độc lập với T-044, T-046.

## 7. Điều kiện dừng riêng

- Cần đọc bất kỳ file nào trong `app/src/production/`, `scripts/release.sh`, `release-output/`, `~/.vgl-release/` hay bí mật để làm xong một tiêu chí: dừng task, không tìm đường khác.
- Cần sửa `.github/workflows/status-page.yml` (ví dụ để lấy danh sách issue bằng token): dừng task. Việc đó là plan rủi ro cao riêng (câu hỏi 2).

## 8. Câu hỏi cho người duyệt

1. **Mã quảng cáo thật.**
   - AI không thể biết vị trí nào đã có mã thật, vì mã nằm trong trình quản lý mật khẩu (ADR-0009).
   - Đề xuất: trang chỉ liệt kê mọi khóa của `AdPlacement` kèm lời nhắc, như mục 3.1.
   - Phương án khác: thêm một file người giữ (ví dụ `docs/ops/release-readiness.md`) ghi "đã có mã: có/chưa" cho từng khóa, không ghi mã. `status.py` so file này với `AdPlacement` và báo khóa thiếu. Cách này thêm một file vào mục 3.2 và một việc cho người phát hành.
   - Anh chọn cách nào?
2. **Issue đang mở.**
   - Đề xuất: chỉ ghi đường dẫn, không đếm. Muốn trang đếm issue thì `status-page.yml` phải gọi `gh issue list` và truyền kết quả cho `status.py`. Đó là sửa `.github/`, cần plan rủi ro cao duyệt chéo, nên nằm ngoài ticket này.
   - Có cần mở ticket riêng cho việc đó không?
3. **36 mục kiểm thủ công cũ chưa đánh dấu.**
   - Trên dữ liệu thật, phần lớn mục của RUN-20261021-* (T-007) có lẽ đã xử lý nhưng chưa ai đánh dấu `[x]`.
   - Đề xuất: giữ đúng dữ liệu, người rà một lần và đánh dấu các mục đã xong (AI không tự đánh dấu thay người).
   - Phương án khác: chỉ hiện mục của 3 phiên `finished` gần nhất, kèm số mục cũ hơn.
4. **Mức rủi ro.**
   - AI xếp Trung bình (điểm 7), không chạm điểm chặn 7 vì `status.py` không phải cổng chất lượng hay bước của phiên.
   - Các plan khác của feature quality-gate (T-042) xếp Cao vì sửa script phiên dùng để kiểm. Nếu anh coi trang tiến độ là một phần quy trình phiên, plan chuyển sang Cao và cần duyệt chéo.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
