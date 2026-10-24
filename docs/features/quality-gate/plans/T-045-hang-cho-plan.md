---
ticket: T-045
title: Cuối phiên soạn sẵn plan cho ticket kế tiếp
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
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

Thêm bước 5 vào mục 4 (Kết thúc phiên) của `docs/auto-run.md`. Sau khi PR báo cáo đã merge, phiên soạn plan `draft` cho tối đa `plan_queue` ticket kế tiếp (mặc định 3, trong tối đa 60 phút). Các ticket do script mới `scripts/plan-queue.py` chọn: `ready`, chưa có plan, spec đã chốt, phụ thuộc đã xong hoặc đã có plan.

Với mỗi plan, phiên:
- chạy thử mục 4.1 khi cần;
- chạy `plan-check.py`;
- giao một phiên rà soát độc lập.

Mọi câu hỏi cần người được gom vào mục mới "Plan soạn sẵn và câu hỏi cho người" của danh sách chạy. Tất cả nằm trên nhánh `claude/RUN-YYYYMMDD-NN-plans`, trong một PR mà phiên không merge. Người trả lời câu hỏi rồi duyệt, merge như PR duyệt plan thường.

Kiểm chứng:
- `plan-queue.py` có test với cây tài liệu giả trong `scripts/tests/run.sh`.
- Phần quy trình kiểm bằng rà soát độc lập và ở phiên kế tiếp.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là bài học L-061 (`docs/lessons.md`).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| L-061, soạn plan | Khi phiên đã viết và merge báo cáo, và còn ticket `ready` chưa có plan, thì phiên soạn plan `draft` cho các ticket kế tiếp | Bước mới chạy **sau** khi PR báo cáo đã merge, để báo cáo không bị chậm và hết hạn mức giữa chừng không làm mất báo cáo. Danh sách chạy đã `finished`, nên bước này không giữ nhánh chính: người merge việc khác được ngay |
| L-061, chọn ticket | Ticket được chọn theo thứ tự ưu tiên của backlog | Backlog chưa có trường ưu tiên. Thứ tự: ticket có `sprint` (số nhỏ trước) đứng trước ticket không có `sprint`, rồi theo mã ticket. Chỉ chọn ticket `ready`, `plan:` rỗng, spec `confirmed` và không còn câu hỏi mở (hoặc không có spec, như ticket của `quality-gate`), và mọi `depends_on` đã merge (`on-staging`, `accepted`, `released`) hoặc đã có plan. Mục 8, câu 1 |
| L-061, chạy thử, kiểm plan | Plan soạn sẵn đã chạy thử mục 4.1 (khi tiêu chí E từ 2) và qua `plan-check.py` | Làm đúng như khi viết plan trong phiên có người theo dõi (`docs/CLAUDE.md`, mục Khi viết plan). Chạy thử cần thiết bị hay emulator thì không làm được trên cloud: ghi vào mục 8 của plan, để `plan-check.py` báo lỗi, không đoán kết quả |
| L-061, câu hỏi | Mọi câu hỏi cần người được gom vào một mục của báo cáo | Mục mới của mẫu danh sách chạy liệt kê từng plan với rủi ro, kết quả `plan-check.py`, rà soát độc lập, và câu hỏi của mục 8 chép nguyên văn kèm phương án AI đề xuất. Người đọc một chỗ là trả lời được hết |
| L-061, giới hạn | Không ăn vào hạn mức mà phiên cần, không tự duyệt | Số plan tối đa `plan_queue` (dev đặt khi duyệt danh sách chạy, mặc định 3, `0` là tắt), tối đa 60 phút. Quá 45 phút thì không bắt đầu plan mới. Phiên không ghi `approved`, không điền mục 9, không lập danh sách chạy, không merge PR plan |
| Phạm vi: không bao gồm | Không soạn spec, không tách ticket | Ticket thiếu spec chốt hay spec còn câu hỏi thì không chọn. `plan-queue.py` liệt kê nó ở phần "bỏ qua" kèm lý do, để báo cáo ghi cho người |

Thiết kế tham chiếu: Không có thiết kế riêng (tài liệu quy trình và script, docs/ops/design-workflow.md).

ADR liên quan: ADR-0009 (phiên dùng tài khoản và hạn mức chung); ADR-1003 (không đổi, plan soạn sẵn theo mẫu hiện hành).

## 3. Giải pháp

### 3.1. Cách làm

**1. `scripts/plan-queue.py` (mới), Python 3, chỉ đọc.**
- Cách dùng: `scripts/plan-queue.py [--max N]`. `PLAN_QUEUE_ROOT` đổi gốc repo cho test, như `RUN_CHECK_ROOT`.
- Các bước:
  - Đọc mọi `docs/features/*/tickets/T-*.md` bằng cùng cách đọc frontmatter như `status.py` (bỏ chú thích sau `#`).
  - Lấy ticket `status: ready` và `plan:` rỗng.
  - Bỏ ticket có spec chưa chốt: dùng lại `spec_problems()` của `scripts/run-check.py`, nạp bằng `importlib` như `run-check.py` nạp `plan-check.py`, để quy tắc spec chỉ nằm một chỗ.
  - Bỏ ticket có phụ thuộc chưa xong. Phụ thuộc xong là `on-staging`, `accepted`, `released`, hoặc đã có `plan:`.
  - Sắp theo `sprint` (số, rỗng xếp cuối) rồi theo số ticket.
- Đầu ra:
  - In lên stdout tối đa `N` dòng `T-NNN<TAB><đường dẫn ticket>`, mặc định 3.
  - In lên stderr các ticket bỏ qua, mỗi ticket một dòng lý do bằng tiếng Việt: `T-NNN: bỏ qua, spec ... chưa confirmed`, `T-NNN: bỏ qua, chờ T-MMM (ready, chưa có plan)`.
- Mã thoát: 0 khi chạy được (danh sách có thể rỗng); 2 khi lỗi đọc file.
- Không ghi file nào.

**2. `docs/auto-run.md`, mục 4, sau bước 4: thêm bước 5 "Soạn plan cho hàng chờ (L-061)".**
- Điều kiện: chỉ chạy khi PR báo cáo đã merge, `plan_queue` của danh sách chạy khác `0`, và phiên kết thúc vì hết danh sách hay đạt số task tối đa. Không chạy khi phiên kết thúc vì nhánh chính đỏ, lỗi hạ tầng, `env-broken`, hay lệnh đẩy bị từ chối.
- Các bước:
  1. Ghi giờ bắt đầu. Tạo nhánh `claude/RUN-YYYYMMDD-NN-plans` từ nhánh chính mới nhất, đẩy ngay bằng `scripts/save-branch.sh`.
  2. Chạy `scripts/plan-queue.py --max <plan_queue>` và chép phần "bỏ qua" vào mục mới của danh sách chạy.
  3. Với từng ticket theo thứ tự:
     - viết plan theo `docs/CLAUDE.md`, mục Khi viết plan;
     - chạy thử mục 4.1 khi tiêu chí E từ 2, bằng `scripts/check.sh` trong worktree riêng;
     - chạy `scripts/plan-check.py` và sửa tới khi không lỗi. Lỗi nào chỉ người gỡ được (chạy thử cần thiết bị, quyền chưa có trong spec) thì giữ lại và ghi vào mục 8;
     - chuyển ticket sang `planned`, điền `plan:`;
     - commit, đẩy bằng `save-branch.sh`.
  4. Sau plan đầu tiên, mở PR nháp `RUN-YYYYMMDD-NN: plan soạn sẵn (draft)`, rồi cập nhật mô tả sau mỗi plan. Hết hạn mức giữa chừng thì người vẫn thấy phần đã soạn.
  5. Giao một phiên rà soát độc lập (ngữ cảnh mới, chỉ đọc), như rà soát trước khi duyệt (`docs/ops/pre-run-review.md`), cho mọi plan đã soạn. Phiên sửa plan theo điểm chặn và điểm nên sửa một lần. Điểm còn lại chép vào mục 8 của plan và vào mục mới.
  6. Viết mục "Plan soạn sẵn và câu hỏi cho người" trong danh sách chạy trên nhánh này, rồi chuyển PR khỏi trạng thái nháp.
- Điểm dừng:
  - Quá 45 phút từ giờ bắt đầu thì không bắt đầu plan mới; quá 60 phút thì dừng plan đang soạn (đẩy phần đã có, ghi "chưa xong").
  - Phiên không merge PR này, không ghi `approved` hay mục 9, không lập danh sách chạy mới.
- Thêm một câu vào mục 6 (đoạn "Phiên không tự sửa quy trình"): plan `draft` soạn ở bước 5 không phải tài liệu quy trình. Phiên không sửa plan đã `approved`, spec hay ticket ngoài trạng thái và trường `plan:` của ticket được chọn.

**3. `docs/auto-run.md`, mục 5 (Báo cáo phiên):** thêm gạch đầu dòng "Plan soạn sẵn và câu hỏi cho người (mục 4, bước 5), trên nhánh và PR plan soạn sẵn".

**4. `docs/templates/run.md`.**
- Frontmatter có thêm dòng `plan_queue: 3`, kèm chú thích: số plan soạn sẵn cuối phiên, `0` là tắt (`auto-run.md`, mục 4, bước 5).
- Thêm mục `### Plan soạn sẵn và câu hỏi cho người` sau `### Đề xuất cho buổi họp tiếp theo`, gồm:
  - một chú thích: điền trên nhánh `claude/RUN-YYYYMMDD-NN-plans`, "Không soạn" kèm lý do khi bước 5 không chạy;
  - bảng: Ticket | Plan | Rủi ro | `plan-check.py` | Rà soát độc lập | Chạy thử 4.1;
  - danh sách "Câu hỏi cho người", mỗi câu ghi mã plan, nguyên văn câu hỏi và phương án AI đề xuất;
  - dòng "Ticket bỏ qua" (chép từ stderr của `plan-queue.py`);
  - dòng "Dừng vì giới hạn".
- Dòng thời gian của mẫu có thêm hai dòng: "Soạn plan hàng chờ: bắt đầu", "Mở PR plan soạn sẵn".

**5. `docs/CLAUDE.md`.**
- Mục Trong phiên tự chạy: thêm gạch đầu dòng sau "Cuối phiên, viết báo cáo vào file phiên chạy." Nội dung: rồi soạn plan `draft` cho hàng chờ theo `auto-run.md`, mục 4, bước 5; không tự duyệt.
- Mục Khi lập danh sách chạy: thêm câu "`plan_queue` mặc định 3; dev giảm hay đặt `0`".

**6. `CLAUDE.md` (gốc), mục Lệnh:** thêm một dòng `scripts/plan-queue.py`.

Mẫu làm theo: `run-check.py` (đọc tài liệu, gốc đổi được bằng biến môi trường, thông báo tiếng Việt, test với cây tài liệu giả ở `scripts/tests/run.sh` dòng 974 trở đi).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/plan-queue.py` | Chọn ticket cho hàng chờ plan (mục 3.1, ý 1) |
| Sửa | `scripts/tests/run.sh` | Khối test mới `plan-queue`, đặt sau khối `run-check.py`, dựng cây tài liệu giả riêng (`$work/pq`) |
| Sửa | `docs/auto-run.md` | Mục 4: bước 5 mới sau bước 4; mục 5: một gạch đầu dòng; mục 6: một câu trong đoạn "Phiên không tự sửa quy trình" |
| Sửa | `docs/templates/run.md` | Frontmatter `plan_queue`; mục "Plan soạn sẵn và câu hỏi cho người"; hai dòng của Dòng thời gian |
| Sửa | `docs/CLAUDE.md` | Mục Trong phiên tự chạy và Khi lập danh sách chạy: mỗi mục một câu |
| Sửa | `CLAUDE.md` | Mục Lệnh: dòng `scripts/plan-queue.py` |
| Sửa | `docs/lessons.md` | L-061: dòng Áp dụng khi merge |
| Sửa | `docs/features/quality-gate/tickets/T-045-hang-cho-plan.md` | Trạng thái theo phiên |

Nơi khác đọc các file bị sửa (`grep -rn "auto-run.md\|templates/run.md\|tickets/" scripts/ .github/`):

- **Đọc frontmatter của danh sách chạy:**
  - `scripts/status.py`, `scripts/resume-check.py` và `.github/workflows/env-canary.yml` đọc frontmatter theo từng khóa (`status`, `started_at`, `staging_build`). Khóa mới `plan_queue` không ảnh hưởng.
  - `scripts/run-check.py` đọc bảng của mục 1 và bảng rà soát. Mục mới nằm ở mục 3 của danh sách chạy, không có dòng bảng bắt đầu bằng mã ticket trong mục 1, nên không bị đọc thành task.
- **Đọc ticket:** `scripts/status.py` (trang tiến độ đã có dòng "Plan `draft` chờ duyệt") và `scripts/plan-check.py` quy tắc 10 (`planned` với plan `draft` là hợp lệ). `plan-queue.py` không ghi ticket.
- **Dẫn tới `auto-run.md`:** các script và workflow dẫn mục 4 đều dẫn tới bảng điều kiện kết thúc hay đoạn Tiếp tục phiên (`resume-check.py`, `wait-run.sh`, `device-session.yml`, `resume-run.yml`); bước 5 mới nằm sau bước 4, không đổi số các bước đó.
- **Chỉ nhắc tên tài liệu trong chú thích hay thông báo:** các file sau chỉ nhắc `docs/auto-run.md`, `run.md`, `docs/CLAUDE.md`, `CLAUDE.md` hay `scripts/tests/run.sh` trong chú thích hay thông báo. Chúng dẫn tới mục, bước hay quy tắc mà plan không đổi, và không đọc nội dung các file đó lúc chạy:
  - `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`;
  - `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/device-session.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/required-assets.txt`;
  - `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt`, `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt` (dẫn quy tắc test trên emulator của `CLAUDE.md` bước 10, plan chỉ thêm một dòng ở mục Lệnh).

  Plan không ảnh hưởng tới chúng.
- **Hàm dùng lại:** `scripts/run-check.py` không bị sửa. `plan-queue.py` chỉ gọi `spec_problems()` và `front_matter()`; T-042 không đổi hai hàm này.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm (`plan-queue.py` chỉ dùng thư viện chuẩn của Python)

### 3.4. Phương án dự phòng

Không có.

## 4. Kiểm thử

Test của `plan-queue.py` viết trước, trong `scripts/tests/run.sh`. Cây tài liệu giả gồm:
- T-901 `ready`, sprint 1, spec confirmed;
- T-902 `ready`, sprint 2;
- T-903 `ready`, không sprint, không spec;
- T-904 `ready`, đã có `plan:`;
- T-905 `ready`, spec `draft`;
- T-906 `ready`, spec confirmed nhưng còn câu hỏi chưa trả lời;
- T-907 `ready`, phụ thuộc T-905;
- T-908 `ready`, phụ thuộc T-904 (đã có plan);
- T-909 `approved`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Chỉ ticket `ready` chưa có plan | Test script | `plan-queue: an approved ticket is not queued` (T-909 không có), `plan-queue: a ticket with a plan is not queued` (T-904 không có) |
| Spec chưa chốt hay còn câu hỏi thì bỏ qua, có lý do | Test script | `plan-queue: a draft spec is skipped with the reason` (stderr có `T-905: bỏ qua` và `chưa confirmed`); `plan-queue: an open spec question is skipped` (T-906, stderr có `câu hỏi còn mở`) |
| Ticket không có spec được chọn | Test script | `plan-queue: a ticket without spec is queued` (T-903 có trong stdout) |
| Phụ thuộc | Test script | `plan-queue: a dependency without plan skips the ticket` (T-907, stderr có `chờ T-905`); `plan-queue: a dependency that has a plan is enough` (T-908 có trong stdout) |
| Thứ tự theo sprint rồi mã | Test script | `plan-queue: order is sprint, then ticket number`, với `--max 9`: stdout đúng `T-901, T-902, T-903, T-908` theo thứ tự. Đối chứng: T-902 có sprint 2 mà mã nhỏ hơn T-903 không sprint, và T-908 không sprint mà mã lớn hơn, nên sắp chỉ theo mã hay chỉ theo sprint đều cho thứ tự khác |
| Giới hạn số ticket | Test script | `plan-queue: --max limits the list` (`--max 2`: đúng 2 dòng `T-901`, `T-902`); `plan-queue: default is 3` |
| Không ghi file | Test script | `plan-queue: writes nothing`: `find "$work/pq" -newer <mốc>` rỗng sau khi chạy |
| Chạy trên repo thật | Test script | `plan-queue: runs on the repository`: mã thoát 0 khi chạy trên gốc repo |
| Bước 5 có đủ điều kiện, giới hạn và việc không làm | Kiểm tra tài liệu | Rà soát độc lập đối chiếu với từng dòng của mục 2: chỉ sau khi PR báo cáo merge; `plan_queue`; 45 và 60 phút; chạy thử 4.1; `plan-check.py`; rà soát độc lập; PR nháp mở sau plan đầu; không merge, không duyệt, không lập danh sách |
| Có hiệu lực thật | Kiểm tra ở phiên sau | Phiên kế tiếp có ticket `ready`: có nhánh `claude/RUN-...-plans`, PR plan soạn sẵn, mục mới của danh sách chạy; ghi vào mục Kiểm chứng của L-061 |

**Ngoại lệ viết test trước:** phần tài liệu quy trình (`auto-run.md`, mẫu danh sách chạy, `docs/CLAUDE.md`, `CLAUDE.md`) không có test tự động; kiểm bằng rà soát độc lập (bước 6). Người duyệt đồng ý với ngoại lệ này.

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (quy trình phiên tự chạy). Không cần tính điểm; điểm dưới đây chỉ để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script mới, test của script, bốn tài liệu quy trình |
| B. Độ rõ của yêu cầu | 1 | Bài học rõ; thứ tự ưu tiên, giới hạn và nơi đặt plan do AI đề xuất (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Script có test; phần quy trình chỉ kiểm được ở phiên sau |
| D. Ảnh hưởng chéo | 2 | Đổi phần cuối của mọi phiên; cùng sửa `scripts/tests/run.sh`, `docs/auto-run.md` với T-044, T-046 |
| E. Độ mới của kỹ thuật | 1 | Script đọc tài liệu theo mẫu `run-check.py` |
| **Tổng** | 7 | **Mức: Cao** (điểm chặn 7) |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:** `docs/auto-run.md`, `docs/templates/run.md`, `docs/CLAUDE.md`, `CLAUDE.md` (mọi phiên đọc); `scripts/tests/run.sh` (mọi task sửa script).
- **Task khác trong sprint có thể xung đột (đang viết plan song song):**
  - **T-044** sửa `scripts/wait-run.sh`, khối test `wait-run` của `scripts/tests/run.sh`, có thể cả `docs/auto-run.md` bước 8. Plan này chỉ sửa mục 4 (sau bước 4), mục 5, mục 6 của `auto-run.md` và thêm khối test riêng sau khối `run-check.py`, nên không trùng đoạn nào.
  - **T-046** sửa mẫu plan (`docs/templates/plan.md`: tiêu chí kiểm thủ công "máy thật" hay "emulator") và kịch bản `device-session`, có thể cả `auto-run.md` mục 4 bước 1 (chạy kịch bản emulator) và mục Cần người kiểm tra của `docs/templates/run.md`. Plan này không sửa `docs/templates/plan.md`. Ở `auto-run.md` mục 4 nó chỉ thêm bước 5; ở `docs/templates/run.md` chỉ thêm khóa `plan_queue`, mục mới sau "Đề xuất cho buổi họp tiếp theo" và hai dòng cuối của Dòng thời gian. Plan soạn sẵn tự theo mẫu plan hiện hành, nên T-046 merge trước hay sau đều được.
  - **T-047** sửa `scripts/status.py` (mục "Sẵn sàng phát hành"). Plan này không sửa `status.py`; `plan-queue.py` tự đọc frontmatter, không nhập `status.py`.
- **Đề xuất thứ tự chạy:** không bắt buộc. Nếu cùng danh sách thì T-044 trước (độc lập, nhỏ), rồi T-045. T-045 chạy trước T-046 thì phiên đó đã soạn được plan cho các ticket còn lại.

## 7. Điều kiện dừng riêng

Đọc spec theo cách của `run-check.py` mà phải sửa `run-check.py` (ví dụ `spec_problems()` cần thêm tham số) thì dừng: `run-check.py` không có trong mục 3.2. Plan phiên bản sau sẽ quyết.

## 8. Câu hỏi cho người duyệt

1. **Thứ tự ưu tiên của backlog.** Ticket chưa có trường ưu tiên. Đề xuất của AI: theo `sprint` rồi theo mã ticket, chưa thêm trường mới. Nếu cần thì thêm `priority` vào mẫu ticket ở một ticket riêng.
2. **Soạn plan sau khi PR báo cáo merge, trong một PR riêng mà phiên không merge.** Đề xuất của AI: làm như vậy, để báo cáo không chậm, nhánh chính không bị giữ, và người merge PR này cùng lúc duyệt plan. Phương án khác: đưa plan vào PR báo cáo (như plan phiên bản mới của task dừng). Cách này chậm báo cáo, và plan chưa ai xem đã nằm trên nhánh chính.
3. **Giới hạn.** Đề xuất của AI: `plan_queue` mặc định 3, tối đa 60 phút, không bắt đầu plan mới sau 45 phút. Hết hạn mức giữa chừng thì `resume-run.yml` không tiếp tục, vì danh sách đã `finished`; người thấy phần đã soạn trên PR nháp.
4. **Ticket rủi ro cao.** Đề xuất của AI: vẫn soạn, ghi rõ "cần duyệt chéo" trong mục mới; không bỏ qua, vì phần lớn ticket cải tiến quy trình đều rủi ro cao.
5. **Lần đầu dùng.** Đề xuất của AI: phiên đầu tiên sau khi merge đặt `plan_queue: 2` để xem chất lượng plan soạn sẵn, rồi tăng.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
