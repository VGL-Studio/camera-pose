---
ticket: T-045
title: Cuối phiên soạn sẵn plan cho ticket kế tiếp
status: draft                # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 10.4M        # cỡ M, số gợi ý của scripts/token-usage.py estimate
blocking_points: [process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-045: CUỐI PHIÊN SOẠN SẴN PLAN CHO TICKET KẾ TIẾP

## 1. Tóm tắt

Cuối phiên, sau khi PR báo cáo đã merge, phiên soạn plan `draft` cho tối đa `plan_queue` ticket kế tiếp (mặc định 3, lần đầu 2).

- **Chọn ticket:** script mới `scripts/plan-queue.py`, chạy ngay lúc viết báo cáo. Hàng chờ và danh sách ticket bị bỏ qua được ghi vào báo cáo trước khi merge.
- **Mỗi plan một nhánh và một PR:** nhánh `claude/plan-T-NNN-<từ khóa>`, như nhóm đang làm. PR nháp mở ngay sau commit đầu.
- **Kiểm tra mỗi plan:** chạy mọi bước chạy thử của mục Khi viết plan, chạy `plan-check.py`, rồi giao rà soát độc lập ngay.
- **Tổng kết:** một PR tóm tắt nhỏ ghi kết quả và câu hỏi cho người vào mục mới "Plan soạn sẵn và câu hỏi cho người" của danh sách chạy, dẫn tới các PR plan.
- **Việc phiên không làm:** không merge PR plan, không duyệt plan.

Kiểm chứng: `plan-queue.py` có test với cây tài liệu giả và repo git giả trong `scripts/tests/run.sh`; phần quy trình kiểm bằng rà soát độc lập và ở phiên kế tiếp.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là bài học L-061 (`docs/lessons.md`).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| L-061, soạn plan | Khi phiên đã viết và merge báo cáo, và còn ticket `ready` chưa có plan, thì phiên soạn plan `draft` cho các ticket kế tiếp | Bước mới chạy **sau** khi PR báo cáo đã merge, để báo cáo không bị chậm và hết hạn mức giữa chừng không làm mất báo cáo. Danh sách chạy đã `finished`, nên bước này không giữ nhánh chính |
| L-061, chọn ticket | Ticket được chọn theo thứ tự ưu tiên của backlog | Ticket chưa có trường ưu tiên. Thứ tự là `sprint` rồi mã ticket. `sprint` dạng số so theo số; dạng chữ xếp sau mọi số, so theo chuỗi; rỗng xếp cuối. Hiện không ticket nào có `sprint`, nên thực tế là theo mã ticket. Điều kiện chọn ở mục 3.1, ý 1 |
| L-061, chạy thử, kiểm plan | Plan soạn sẵn đã chạy thử khi cần và qua `plan-check.py` | Làm mọi bước chạy thử của `docs/CLAUDE.md`, mục Khi viết plan: đo trên code hiện tại cho plan sửa lỗi (L-029), kỹ thuật mới bằng `scripts/check.sh` (L-001, L-010), trợ năng (L-004), `:app:assembleStagingRelease` khi thêm thư viện (L-053). Bước nào không chạy được trên cloud (cần thiết bị, emulator) thì ghi `Chạy thử: chưa chạy (<lý do>)` ở mục 4.1, ở cột Chạy thử của bảng mới và ở mục 8. Không dựa vào `plan-check.py` tự báo |
| L-061, câu hỏi | Mọi câu hỏi cần người được gom vào một mục của báo cáo | Mục mới của danh sách chạy liệt kê từng plan: PR, rủi ro ("cần duyệt chéo" với plan rủi ro cao), `plan-check.py`, rà soát độc lập, chạy thử. Dưới bảng là câu hỏi ở mục 8 của từng plan, chép nguyên văn kèm phương án AI đề xuất. Người đọc một chỗ là trả lời được hết |
| L-061, giới hạn | Không ăn vào hạn mức mà phiên cần, không tự duyệt | Chi tiết ở mục 3.1, ý 2 |
| Phạm vi: không bao gồm | Không soạn spec, không tách ticket | Ticket thiếu spec chốt hay spec còn câu hỏi thì không chọn; báo cáo ghi lý do cho người |

Thiết kế tham chiếu: Không có thiết kế riêng (tài liệu quy trình và script, docs/ops/design-workflow.md).

ADR liên quan: ADR-0009 (phiên dùng tài khoản và hạn mức chung); ADR-1003 (không đổi, plan soạn sẵn theo mẫu hiện hành).

## 3. Giải pháp

### 3.1. Cách làm

**1. `scripts/plan-queue.py` (mới), Python 3, không ghi file nào.**

Cách dùng: `scripts/plan-queue.py [--max N]`. `PLAN_QUEUE_ROOT` đổi gốc repo cho test, như `RUN_CHECK_ROOT`.

*Đọc tài liệu.*
- Script nạp `scripts/run-check.py` bằng `importlib`, như `run-check.py` nạp `plan-check.py`. Trước khi nạp, nó đặt `os.environ["RUN_CHECK_ROOT"]` bằng gốc repo của mình, để `run-check.py` đọc cùng cây tài liệu.
- Nó dùng lại ba hàm của module đó:
  - `front_matter()`: cũng là hàm đọc frontmatter của `plan-check.py`;
  - `spec_problems(spec_path, {})`, với `plan_fields={}` vì ticket chưa có plan;
  - `ids()`.

  Quy tắc đọc frontmatter và spec chỉ nằm một chỗ.
- Script đọc mọi `docs/features/*/tickets/T-*.md`. Ticket không có khối frontmatter hay không có `id`, `status`: in `LỖI` và thoát mã 2.

*Bỏ ticket đang có plan ở nơi khác.*
- Nhánh: đọc `git -C <PLAN_QUEUE_ROOT> for-each-ref --format='%(refname:short)' refs/remotes/origin/claude/plan-` (mọi lệnh `git` của script chạy bằng `git -C` ở gốc tài liệu, không ở thư mục hiện tại, để test trên repo giả không đọc ref của repo thật). Phiên chạy `git fetch origin` trước khi gọi script. Ticket có nhánh `origin/claude/plan-T-NNN` hay `origin/claude/plan-T-NNN-*` thì bỏ qua: `T-NNN: bỏ qua, đang có plan trên nhánh <nhánh>`.
- PR mở: đọc bằng `gh api "repos/<owner>/<repo>/pulls?state=open&per_page=100"`, rồi `gh api repos/<owner>/<repo>/pulls/<số>/files` cho từng PR. `owner/repo` lấy từ remote `origin`, như `wait-run.sh`. Ticket có file nằm trong một PR mở thì bỏ qua: `T-NNN: bỏ qua, đang có PR mở #<số> sửa ticket`.
- Lệnh `git` hay `gh` lỗi: in `LỖI` kèm lệnh và thoát mã 2. Không đoán.

*Chọn trong ticket còn lại.* Ticket phải `status: ready` và `plan:` rỗng. Mỗi ticket bị bỏ qua có một dòng lý do trên stderr, giống thông báo của `run-check.py`:
- `spec:` trỏ tới file không có: `T-NNN: bỏ qua, spec <đường dẫn> không có`.
- `spec:` rỗng mà `scenarios` không rỗng: `T-NNN: bỏ qua, có kịch bản mà không có spec`.
- `spec_problems()` báo lỗi (spec chưa `confirmed`, còn câu hỏi chưa trả lời): `T-NNN: bỏ qua, <thông báo của spec_problems>`.
- Ticket không có spec và `scenarios: []` (như ticket của `quality-gate`): được chọn.

*Phụ thuộc.* Mỗi `depends_on` phải thuộc một trong ba trường hợp:
- đã merge (`on-staging`, `accepted`, `released`);
- đã có `plan:`;
- đã được chọn trước nó trong cùng lượt.

Còn lại thì bỏ qua với lý do riêng:
- `T-NNN: bỏ qua, chờ T-MMM (ready, chưa có plan)`;
- `T-NNN: bỏ qua, phụ thuộc T-MMM đang stopped` (cũng vậy với `cancelled`);
- `T-NNN: bỏ qua, phụ thuộc T-MMM không có ticket`.

*Thứ tự.* Theo `sprint`, rồi theo số ticket:
- `sprint` dạng số so theo số (`2` trước `10`);
- dạng chữ xếp sau mọi số, so theo chuỗi;
- rỗng xếp cuối.

Script duyệt theo thứ tự này, nên phụ thuộc được chọn trong lượt luôn đứng trước ticket phụ thuộc nó.

*Đầu ra.*
- stdout: tối đa `N` dòng `T-NNN<TAB><đường dẫn ticket>`, mặc định 3.
- stderr: mỗi ticket bị bỏ qua một dòng.
- Mã thoát: 0 khi đọc được, kể cả khi danh sách rỗng; 2 chỉ khi lỗi đọc tài liệu, frontmatter, `git` hay `gh`.

**2. `docs/auto-run.md`, mục 4.**

*Bước 2 (viết báo cáo):* thêm một gạch đầu dòng sau "Viết các mục không phụ thuộc emulator và staging". Nội dung:
- `plan_queue` của danh sách chạy khác `0` thì chạy `git fetch origin` và `scripts/plan-queue.py --max <plan_queue>`.
- Ghi hàng chờ và danh sách bỏ qua vào mục "Plan soạn sẵn và câu hỏi cho người".
- Bước 5 sẽ không chạy thì ghi "Không soạn" kèm lý do: `plan_queue: 0`, hàng chờ rỗng, phiên kết thúc vì lý do khác hết danh sách, phiên quá 24 giờ, `staging_needed_by`. Mã 2 của script cũng ghi vào đây, kèm thông báo.

*Bước 5 mới "Soạn plan cho hàng chờ (L-061)", sau bước 4.*

Điều kiện chạy:
- PR báo cáo đã merge, và hàng chờ ở bước 2 không rỗng;
- phiên kết thúc vì hết danh sách hay đạt số task tối đa. Không chạy khi phiên kết thúc vì nhánh chính đỏ, lỗi hạ tầng, `env-broken` hay lệnh đẩy bị từ chối;
- phiên bắt đầu chưa quá 24 giờ, như giới hạn của `resume-run.yml`;
- không gần `staging_needed_by`: có `staging_needed_by` mà bản staging của phiên chưa phát xong thì không bắt đầu.

Ghi giờ bắt đầu. Với từng ticket của hàng chờ, theo thứ tự:
1. Tạo nhánh `claude/plan-T-NNN-<từ khóa>` từ nhánh chính mới nhất.
2. Viết plan theo `docs/CLAUDE.md`, mục Khi viết plan. Chuyển ticket sang `planned`, điền `plan:`.
3. Commit, đẩy bằng `scripts/save-branch.sh`. Mở PR nháp `Plan T-NNN (draft): <tên>` ngay sau commit đầu, để hết hạn mức giữa chừng người vẫn thấy phần đã soạn.
4. Làm mọi bước chạy thử của mục Khi viết plan, mỗi bước trong một worktree riêng. Bước không chạy được thì ghi `Chạy thử: chưa chạy (<lý do>)` ở mục 4.1 và mục 8.
5. Chạy `scripts/plan-check.py`, sửa tới khi không lỗi. Lỗi chỉ người gỡ được thì giữ lại và ghi vào mục 8.
6. Ngay sau đó, giao một phiên rà soát độc lập (ngữ cảnh mới, chỉ đọc) cho plan này, như rà soát trước khi duyệt (`docs/ops/pre-run-review.md`). Sửa plan theo điểm chặn và điểm nên sửa một lần; điểm còn lại chép vào mục 8.
7. Đẩy, cập nhật mô tả PR (kết quả `plan-check.py`, rà soát, chạy thử), rồi chuyển PR khỏi trạng thái nháp.

Giới hạn thời gian:
- Quá 45 phút từ giờ bắt đầu thì không bắt đầu plan mới.
- Quá 60 phút thì dừng plan đang soạn: đẩy phần đã có, ghi "chưa xong".
- Mỗi plan được rà soát ngay sau `plan-check.py`, nên phần cuối không dồn rà soát. Plan chưa kịp rà soát ghi "chưa rà soát" ở mục mới và trong mô tả PR.

Kết thúc bước 5:
- Mở một PR tóm tắt nhỏ `RUN-YYYYMMDD-NN: plan soạn sẵn` (nhánh `claude/RUN-YYYYMMDD-NN-plans`). PR chỉ sửa mục "Plan soạn sẵn và câu hỏi cho người" của danh sách chạy: bảng các PR plan và câu hỏi cho người.
- Merge PR tóm tắt bằng `scripts/merge-if-green.sh` như PR báo cáo. Các PR plan để người duyệt và merge.

Việc phiên không làm ở bước này:
- không merge PR plan, không ghi `approved` hay mục 9;
- không lập danh sách chạy mới, ngoài danh sách nháp của mục Chuẩn bị cho người khi task dừng;
- không sửa plan đã `approved`, spec, hay ticket ngoài trạng thái và trường `plan:` của ticket được chọn.

*Mục 6 (đoạn "Phiên không tự sửa quy trình"):* thêm một câu. Plan `draft` soạn ở bước 5 không phải tài liệu quy trình.

**3. `docs/auto-run.md`, mục 5 (Báo cáo phiên):** thêm gạch đầu dòng "Plan soạn sẵn và câu hỏi cho người: hàng chờ và ticket bị bỏ qua (mục 4, bước 2), các PR plan và câu hỏi (bước 5)".

**4. `docs/templates/run.md`.**
- Frontmatter có thêm dòng `plan_queue: 2`, kèm chú thích: số plan soạn sẵn cuối phiên, `0` là tắt (`auto-run.md`, mục 4, bước 5). Giá trị 2 dùng cho những phiên đầu; tăng lên 3 sau 2 phiên tốt (mục 8, câu 5).
- Thêm mục `### Plan soạn sẵn và câu hỏi cho người` sau `### Đề xuất cho buổi họp tiếp theo`, gồm:
  - chú thích: hàng chờ và ticket bỏ qua điền ở bước 2, phần còn lại điền qua PR tóm tắt ở bước 5;
  - dòng "Hàng chờ" và dòng "Ticket bỏ qua", chép từ `plan-queue.py`, hoặc "Không soạn" kèm lý do;
  - bảng: Ticket | PR plan | Rủi ro | `plan-check.py` | Rà soát độc lập | Chạy thử;
  - danh sách "Câu hỏi cho người", mỗi câu ghi mã plan, nguyên văn câu hỏi và phương án AI đề xuất;
  - dòng "Dừng vì giới hạn".
- Dòng thời gian của mẫu có thêm hai dòng: "Soạn plan hàng chờ: bắt đầu", "Mở PR tóm tắt plan soạn sẵn".

**5. `docs/CLAUDE.md`.**
- Mục Trong phiên tự chạy:
  - dòng "Đặt tên nhánh theo dạng `claude/T-NNN-<tu-khoa>`" thêm: "trừ nhánh plan soạn sẵn cuối phiên, `claude/plan-T-NNN-<tu-khoa>`";
  - sau "Cuối phiên, viết báo cáo vào file phiên chạy." thêm: "rồi soạn plan `draft` cho hàng chờ theo `auto-run.md`, mục 4, bước 5; không tự duyệt, không merge PR plan".
- Mục Khi lập danh sách chạy: thêm "`plan_queue` mặc định 2; dev đổi hay đặt `0`".

**6. `CLAUDE.md` (gốc), mục Lệnh:** thêm dòng "Chọn ticket cho hàng chờ plan cuối phiên: `scripts/plan-queue.py [--max N]` (`docs/auto-run.md`, mục 4)".

Mẫu làm theo: `run-check.py`. Script đọc tài liệu, đổi được gốc repo bằng biến môi trường, in thông báo tiếng Việt, và có test với cây tài liệu giả ở `scripts/tests/run.sh` từ dòng 974 trở đi. Lệnh `gh api` dùng `scripts/tests/fake-gh`, như test của `wait-run.sh`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/plan-queue.py` | Chọn ticket cho hàng chờ plan (mục 3.1, ý 1); đọc frontmatter và spec bằng `front_matter()`, `spec_problems()`, `ids()` của `run-check.py` |
| Sửa | `scripts/tests/run.sh` | Khối test mới `plan-queue`, đặt sau khối `run-check.py`. Dựng cây tài liệu giả riêng (`$work/pq`) có repo git và nhánh giả, cùng fixture của `fake-gh` cho PR mở |
| Sửa | `docs/auto-run.md` | Mục 4: một gạch đầu dòng ở bước 2, bước 5 mới sau bước 4; mục 5: một gạch đầu dòng; mục 6: một câu trong đoạn "Phiên không tự sửa quy trình" |
| Sửa | `docs/templates/run.md` | Frontmatter `plan_queue`; mục "Plan soạn sẵn và câu hỏi cho người"; hai dòng của Dòng thời gian |
| Sửa | `docs/CLAUDE.md` | Mục Trong phiên tự chạy: ngoại lệ đặt tên nhánh `claude/plan-T-NNN-<tu-khoa>` và câu soạn plan `draft` ở bước 5; mục Khi lập danh sách chạy: một câu về `plan_queue` |
| Sửa | `CLAUDE.md` | Mục Lệnh: dòng `scripts/plan-queue.py` |
| Sửa | `docs/lessons.md` | L-061: dòng Áp dụng khi merge |
| Sửa | `docs/features/quality-gate/tickets/T-045-hang-cho-plan.md` | Trạng thái theo phiên |

Nơi khác đọc các file bị sửa (`grep -rn "auto-run.md\|templates/run.md\|tickets/" scripts/ .github/`):

- **Đọc frontmatter của danh sách chạy:**
  - `scripts/status.py`, `scripts/resume-check.py` và `.github/workflows/env-canary.yml` đọc frontmatter theo từng khóa (`status`, `started_at`, `staging_build`). Khóa mới `plan_queue` không ảnh hưởng.
  - `scripts/run-check.py` đọc bảng của mục 1 và bảng rà soát. Mục mới nằm ở mục 3 của danh sách chạy, nên không bị đọc thành task.
- **Đọc ticket:** `scripts/status.py` (trang tiến độ đã có dòng "Plan `draft` chờ duyệt") và `scripts/plan-check.py` quy tắc 10 (ticket `planned` với plan `draft` là hợp lệ). `plan-queue.py` không ghi ticket.
- **Dẫn tới `auto-run.md`:** các script và workflow dẫn mục 4 đều dẫn tới bảng điều kiện kết thúc hay đoạn Tiếp tục phiên (`resume-check.py`, `wait-run.sh`, `.github/workflows/device-session.yml`, `.github/workflows/resume-run.yml`). Bước 5 mới nằm sau bước 4, không đổi số các bước đó. Gạch đầu dòng mới ở bước 2 không đổi thứ tự các bước.
- **Chỉ nhắc tên tài liệu trong chú thích hay thông báo:** các file sau chỉ nhắc `docs/auto-run.md`, `run.md`, `docs/CLAUDE.md`, `CLAUDE.md` hay `scripts/tests/run.sh` trong chú thích hay thông báo. Chúng dẫn tới mục, bước hay quy tắc mà plan không đổi, và không đọc nội dung các file đó lúc chạy:
  - `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`;
  - `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/device-session.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/required-assets.txt`;
  - `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt`, `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt` (dẫn quy tắc test trên emulator của `CLAUDE.md` bước 10, plan chỉ thêm một dòng ở mục Lệnh).

  Plan không ảnh hưởng tới chúng.
- **Hàm dùng lại:** `scripts/run-check.py` không bị sửa; `plan-queue.py` chỉ gọi `front_matter()`, `spec_problems()` và `ids()` của nó.
- **Không trùng với `merge-if-green.sh`:** nhánh `claude/plan-T-NNN-*` và `claude/RUN-YYYYMMDD-NN-plans` được tạo sau khi danh sách chạy đã `finished`. `merge-if-green.sh` không bị sửa.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm (`plan-queue.py` chỉ dùng thư viện chuẩn của Python, `git` và `gh` có sẵn)

### 3.4. Phương án dự phòng

Không có.

## 4. Kiểm thử

Test của `plan-queue.py` viết trước, trong `scripts/tests/run.sh`. Cây tài liệu giả `$work/pq` là một repo git. Khối test **giữ** `GIT_CONFIG_*` của `run.sh` (URL `https://github.com/o/r.git`), vì `plan-queue.py` đọc `owner/repo` từ `remote.origin.url` để gọi `fake-gh`; tắt nó thì `origin` thành đường dẫn cục bộ và mọi test ra mã 2. Nhánh `claude/plan-T-919-x` tạo thẳng bằng `git update-ref refs/remotes/origin/claude/plan-T-919-x HEAD`, không cần bare repo hay `git fetch`. Script không gọi `git fetch` (phiên gọi trước khi chạy script). Repo giả đặt danh tính commit `GIT_AUTHOR_*`, `GIT_COMMITTER_*` như hàm `git_t` của `run.sh`, vì máy CI không có sẵn (phiên bản 2). `fake-gh` trả về PR mở #77, sửa file ticket T-920.

| Ticket | Thiết lập | Kết quả mong đợi |
| --- | --- | --- |
| T-901 | `ready`, sprint 1, spec confirmed | Được chọn |
| T-902 | `ready`, sprint 2 | Được chọn |
| T-903 | `ready`, không sprint, không spec, `scenarios: []` | Được chọn |
| T-904 | `ready`, đã có `plan:` | Không được chọn |
| T-905 | `ready`, spec `draft` | Bỏ qua |
| T-906 | `ready`, spec confirmed nhưng còn câu hỏi chưa trả lời | Bỏ qua |
| T-907 | `ready`, phụ thuộc T-905 | Bỏ qua |
| T-908 | `ready`, không sprint, phụ thuộc T-904 (đã có plan) | Được chọn |
| T-909 | `approved` | Không được chọn |
| T-910 | `ready`, sprint 1 | Được chọn |
| T-911 | `ready`, sprint 10 | Được chọn |
| T-912 | `ready`, sprint `beta` | Được chọn |
| T-913 | `ready`, không sprint, phụ thuộc T-903 (được chọn trong cùng lượt) | Được chọn |
| T-914 | `stopped` | Không được chọn |
| T-915 | `ready`, phụ thuộc T-914 | Bỏ qua |
| T-916 | `ready`, phụ thuộc T-999 (không có ticket) | Bỏ qua |
| T-917 | `ready`, không spec mà `scenarios: [KB-1]` | Bỏ qua |
| T-918 | `ready`, `spec:` trỏ tới file không có | Bỏ qua |
| T-919 | `ready`, có nhánh `origin/claude/plan-T-919-x` | Bỏ qua |
| T-920 | `ready`, nằm trong PR mở #77 | Bỏ qua |

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Chỉ ticket `ready` chưa có plan | Test script | `plan-queue: an approved ticket is not queued` (không có T-909), `plan-queue: a ticket with a plan is not queued` (không có T-904), `plan-queue: a stopped ticket is not queued` (không có T-914) |
| Spec chưa chốt, còn câu hỏi, sai đường dẫn hay thiếu, có lý do | Test script | `plan-queue: a draft spec is skipped with the reason` (stderr có `T-905: bỏ qua` và `chưa confirmed`); `plan-queue: an open spec question is skipped` (T-906, `câu hỏi còn mở`); `plan-queue: a missing spec file is skipped` (T-918, `không có`); `plan-queue: scenarios without spec are skipped` (T-917, `có kịch bản mà không có spec`) |
| Ticket không có spec, không kịch bản được chọn | Test script | `plan-queue: a ticket without spec is queued` (T-903 có trong stdout) |
| Phụ thuộc | Test script | `plan-queue: a dependency without plan skips the ticket` (T-907, `chờ T-905`); `plan-queue: a dependency that has a plan is enough` (T-908 có); `plan-queue: a dependency queued earlier is enough` (T-913 có, đứng sau T-903); `plan-queue: a stopped dependency is skipped` (T-915, `T-914 đang stopped`); `plan-queue: a missing dependency is skipped` (T-916, `T-999 không có ticket`) |
| Đang có plan ở nơi khác | Test script | `plan-queue: a ticket with a plan branch is skipped` (T-919, `đang có plan trên nhánh origin/claude/plan-T-919-x`); `plan-queue: a ticket in an open PR is skipped` (T-920, `đang có PR mở #77`) |
| Thứ tự theo sprint (số, chữ, rỗng) rồi mã | Test script | `plan-queue: order is sprint, then ticket number`, với `--max 20`: stdout đúng thứ tự `T-901, T-910, T-902, T-911, T-912, T-903, T-908, T-913`. Đối chứng: so sprint theo chuỗi sẽ đặt T-911 (`10`) trước T-902 (`2`); sắp chỉ theo mã sẽ đặt T-902 trước T-910; T-912 (`beta`) phải sau mọi sprint số và trước sprint rỗng |
| Giới hạn số ticket | Test script | `plan-queue: --max limits the list` (`--max 2`: đúng `T-901`, `T-910`); `plan-queue: default is 3` (đúng `T-901`, `T-910`, `T-902`) |
| Mã 2 chỉ cho lỗi đọc | Test script | `plan-queue: a ticket without front matter exits 2` (cây giả thứ hai có một ticket không có khối `---`); `plan-queue: a gh error exits 2` (`fake-gh` không có route `pulls?state=open&per_page=100`, trả 404); `plan-queue: skipped tickets do not change the exit code` (cây chính thoát 0) |
| Không ghi file | Test script | `plan-queue: writes nothing`: `find "$work/pq" -newer <mốc> -not -path '*/.git/*'` rỗng sau khi chạy |
| Chạy trên repo thật | Test script | `plan-queue: runs on the repository`: mã thoát khác 2 khi chạy trên gốc repo với `fake-gh` trả danh sách PR rỗng |
| Bước 2 và bước 5 có đủ điều kiện, giới hạn và việc không làm | Kiểm tra tài liệu | Rà soát độc lập đối chiếu với từng dòng của mục 2 và mục 3.1, ý 2. Kiểm: hàng chờ ghi vào báo cáo trước khi merge; bước 5 chỉ chạy sau khi PR báo cáo merge; điều kiện 24 giờ và `staging_needed_by`; mỗi plan một nhánh và PR nháp ngay sau commit đầu; chạy thử theo mục Khi viết plan, ghi `Chạy thử: chưa chạy (...)`; rà soát ngay sau `plan-check.py`; 45 và 60 phút; PR tóm tắt; không merge PR plan, không duyệt, không lập danh sách |
| Có hiệu lực thật | Kiểm tra ở phiên sau | Phiên kế tiếp có ticket `ready` thì có nhánh và PR plan, có mục mới trong danh sách chạy. Dòng Kiểm chứng của L-061 ghi số plan soạn xong, số plan qua rà soát không có điểm chặn, và thời gian người duyệt |

**Ngoại lệ viết test trước:** phần tài liệu quy trình (`auto-run.md`, mẫu danh sách chạy, `docs/CLAUDE.md`, `CLAUDE.md`) không có test tự động; kiểm bằng rà soát độc lập (bước 6). Người duyệt đồng ý với ngoại lệ này.

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm chặn số 7 của `risk-scoring.md`, mục 1 (quy trình phiên tự chạy). Mức Cao, không cần tính điểm; điểm dưới đây chỉ để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script mới, test của script, bốn tài liệu quy trình |
| B. Độ rõ của yêu cầu | 1 | Bài học rõ; các câu hỏi ở mục 8 đã được trả lời |
| C. Khả năng kiểm chứng tự động | 1 | Script có test; phần quy trình chỉ kiểm được ở phiên sau |
| D. Ảnh hưởng chéo | 2 | Đổi phần cuối của mọi phiên; cùng sửa `scripts/tests/run.sh`, `docs/auto-run.md` với T-044, T-046 |
| E. Độ mới của kỹ thuật | 1 | Script đọc tài liệu theo mẫu `run-check.py`; đọc nhánh và PR như `wait-run.sh` |
| **Tổng** | 7 (tham khảo) | **Mức: Cao** (điểm chặn số 7) |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:** `docs/auto-run.md`, `docs/templates/run.md`, `docs/CLAUDE.md`, `CLAUDE.md` (mọi phiên đọc); `scripts/tests/run.sh` (mọi task sửa script).
- **Task khác trong sprint có thể xung đột (đang viết plan song song):**
  - **T-044** sửa `scripts/wait-run.sh`, khối test `wait-run` của `scripts/tests/run.sh`, có thể cả `docs/auto-run.md` bước 8. Plan này chỉ sửa mục 4 (bước 2, bước 5 mới), mục 5, mục 6 của `auto-run.md` và thêm khối test riêng sau khối `run-check.py`, nên không trùng đoạn nào.
  - **T-046** sửa mẫu plan (`docs/templates/plan.md`: tiêu chí kiểm thủ công "máy thật" hay "emulator") và kịch bản `device-session`, có thể cả `auto-run.md` mục 4 bước 1 và mục Cần người kiểm tra của `docs/templates/run.md`. Plan này không sửa `docs/templates/plan.md`.
    - Ở `auto-run.md` mục 4, plan này chỉ thêm một gạch đầu dòng ở bước 2 và thêm bước 5.
    - Ở `docs/templates/run.md`, plan này chỉ thêm khóa `plan_queue`, mục mới sau "Đề xuất cho buổi họp tiếp theo" và hai dòng cuối của Dòng thời gian.

    Plan soạn sẵn tự theo mẫu plan hiện hành, nên T-046 merge trước hay sau đều được.
  - **T-047** sửa `scripts/status.py` (mục "Sẵn sàng phát hành"). Plan này không sửa `status.py`; `plan-queue.py` không nhập `status.py`.
- **Đề xuất thứ tự chạy:** không bắt buộc. Nếu cùng danh sách thì T-044 trước (độc lập, nhỏ), rồi T-045. T-045 chạy trước T-046 thì phiên đó đã soạn được plan cho các ticket còn lại.

## 7. Điều kiện dừng riêng

Đọc spec theo cách của `run-check.py` mà phải sửa `run-check.py` (ví dụ `spec_problems()` cần thêm tham số) thì dừng: `run-check.py` không có trong mục 3.2. Plan phiên bản sau sẽ quyết.

## 8. Câu hỏi cho người duyệt

Tuấn đã trả lời cả 5 câu ngày 2026-10-24, theo đề xuất của rà soát độc lập.

1. **Thứ tự ưu tiên của backlog.** Theo `sprint` rồi theo mã ticket, chưa thêm trường `priority`.
   - `sprint` dạng số so theo số, dạng chữ xếp sau và so theo chuỗi, rỗng xếp cuối.
   - Hiện không ticket nào có `sprint`, nên thực tế là theo mã ticket.

   **Trả lời:** đồng ý.
2. **Nơi đặt plan soạn sẵn.** Mỗi plan một nhánh `claude/plan-T-NNN-<từ khóa>` và một PR, như nhóm đang làm; không dùng một PR chung. Mục "Plan soạn sẵn và câu hỏi cho người" ghi trong báo cáo (hàng chờ, trước khi merge) và trong một PR tóm tắt nhỏ (kết quả, câu hỏi), dẫn tới các PR plan.

   **Trả lời:** mỗi plan một PR (Tuấn quyết).
3. **Giới hạn.**
   - Tối đa 3 plan, không bắt đầu plan mới sau 45 phút, dừng ở 60 phút. Mỗi plan được rà soát ngay sau `plan-check.py`, nên phần cuối không dồn rà soát; plan chưa rà soát ghi "chưa rà soát".
   - Không bắt đầu bước 5 khi phiên đã quá 24 giờ, hay khi có `staging_needed_by` mà bản staging của phiên chưa phát xong.
   - Hết hạn mức giữa chừng thì `resume-run.yml` không tiếp tục, vì danh sách đã `finished`; người thấy phần đã soạn trên các PR nháp.

   **Trả lời:** đồng ý.
4. **Ticket rủi ro cao.** Vẫn soạn, ghi "cần duyệt chéo" ở mục mới và trong mô tả PR.

   **Trả lời:** đồng ý.
5. **Lần đầu dùng.** `plan_queue: 2`. Dòng Kiểm chứng của L-061 ghi số plan soạn xong, số plan qua rà soát không có điểm chặn, và thời gian người duyệt. Sau 2 phiên tốt thì tăng lên 3.

   **Trả lời:** đồng ý.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1: Duyệt (Nhật, duyệt chéo Tuấn, 2026-10-24); ghi chú: Ngày 2026-10-24, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Rà soát độc lập (phiên mới, chỉ đọc) trước khi duyệt; mọi điểm chặn và nên sửa đã sửa trong bản nháp. Mỗi plan một PR (Tuấn chọn). Thay bằng phiên bản 2 vì rà soát độc lập trước RUN-20261024-02 (docs/ops/pre-run-review.md, mục 2) tìm đoạn cây tài liệu giả của mục 4 tắt `GIT_CONFIG_*` theo L-068, trong khi `plan-queue.py` đọc `owner/repo` từ `remote.origin.url`: mọi test sẽ ra mã 2 (mục 4).

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <ghi chú, nếu có>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
