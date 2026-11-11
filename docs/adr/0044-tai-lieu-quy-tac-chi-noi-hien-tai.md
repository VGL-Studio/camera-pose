---
id: ADR-0044
title: Tài liệu quy tắc chỉ nói quy tắc hiện tại; lịch sử nằm ở những chỗ dành riêng và không sang app
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-11-01
deciders: [Nhật]
supersedes:                  # bổ sung ADR-0017 (thứ script khởi tạo bỏ) và ADR-0030 (bản dự án mẫu mà script đồng bộ dựng); hai ADR này được hợp nhất trong đợt dọn (mục 4)
superseded_by:
---

# ADR-0044: TÀI LIỆU QUY TẮC CHỈ NÓI QUY TẮC HIỆN TẠI

## Bối cảnh

Repo này vừa là dự án mẫu vừa là nơi chạy quy trình hằng ngày. Mỗi bài học, sự cố hay lần sửa đều để lại dấu vết trong tài liệu quy tắc: mã dẫn chiếu trong ngoặc như `(L-114)` hay `(T-084, L-143)`, và những câu kể lại cách làm cũ ("trước đây…", "đã gặp với…").

Số liệu ngày 2026-11-01, đếm theo số lần xuất hiện của mã bài học, mã ticket và mã phiên (`L-` hay `T-` cộng ba chữ số, `RUN-` cộng tám chữ số):

- **Tài liệu quy tắc** (`CLAUDE.md`, `README.md`, `docs/CLAUDE.md`, `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/ops/`, `docs/templates/`, `.claude/skills/`): khoảng 430 lần, trong khoảng 25 file. Bảy file chiếm khoảng 80%: `docs/ops/ads-guide.md`, `docs/auto-run.md`, `docs/ops/ads-business-rules.md`, `docs/ops/failure-playbook.md`, `docs/CLAUDE.md`, `docs/ops/pre-run-review.md`, `docs/ops/staging-and-runs.md`.
- **Chú thích trong code và script** (không tính test): khoảng 670 lần, trong khoảng 140 file. Riêng `core/ads/src/main` có khoảng 420 lần.

Khi tạo app, `scripts/new-app.sh` đã bỏ sổ bài học, báo cáo phiên, cùng ticket, plan, phản hồi và yêu cầu (`docs/ops/new-app.md`, bước 5). Vì vậy ở app, các mã trên trỏ tới những thứ không có. AI của app vẫn đọc mọi chữ đó trong mỗi phiên: tốn token, và dễ làm theo một quyết định đã cũ.

Script vẫn giữ một số dấu vết lịch sử của dự án mẫu: `docs/metrics/token-usage.csv` (số liệu các phiên của mẫu) và phần Bối cảnh của mọi ADR.

ADR còn có những chuỗi sửa chồng nhau, ví dụ:

- cổng chất lượng và nhánh chính: ADR-0010, 0025, 0039;
- quảng cáo: ADR-0026, 0036, 0037, 0038;
- lệch phạm vi: ADR-0029, 0032, 0033 (ADR-0028 đã `superseded`).

Các ADR trước của mỗi chuỗi vẫn `accepted` nhưng chỉ còn đúng một phần, nên người đọc phải đọc cả chuỗi mới biết quy tắc hiện tại.

Hiện có hai app tạo từ repo này, cả hai nhận thay đổi qua `scripts/template-sync.sh`. Bản app nhận phải ổn định từ commit này sang commit khác, để đồng bộ không sinh xung đột giả.

## Quyết định

### 1. Chỗ dành cho lịch sử và tài liệu quy tắc

**Chỗ dành cho lịch sử:**

- `docs/lessons.md`;
- `docs/runs/`;
- ticket, plan, phản hồi và yêu cầu trong `docs/features/`;
- `docs/metrics/`, `docs/inbox/`, `docs/test-sessions/`;
- phần Bối cảnh, Các phương án đã cân nhắc và Hệ quả (trừ dòng "Việc AI phải tuân thủ") của ADR;
- mô tả PR và git log.

File `README.md` hướng dẫn trong các thư mục trên (như `docs/runs/README.md`, `docs/test-sessions/README.md`) không phải lịch sử mà là tài liệu quy tắc.

**Tài liệu quy tắc** là mọi file Markdown của repo nằm ngoài chỗ dành cho lịch sử, cùng dòng "Việc AI phải tuân thủ" của ADR chưa `superseded`. Ba thứ không tính:

- spec và thiết kế của feature, vì chúng có mục lịch sử riêng;
- phần còn lại của ADR;
- trang do script sinh ra, vì chúng được kiểm qua nguồn.

Phạm vi nêu theo quy tắc này, không theo danh sách file, để tài liệu quy tắc thêm sau cũng được kiểm.

### 2. Tài liệu quy tắc viết thế nào

- Viết quy tắc ở thì hiện tại. Lý do cần thiết viết thành một vế câu, ví dụ "thêm `--rerun`, vì không có nó Gradle có thể bỏ qua test và giữ ảnh cũ".
- Không kể lại cách làm cũ.
- Không ghi mã bài học, mã ticket hay mã phiên. Máy kiểm theo mẫu `\b(L-\d{3}|T-\d{3}|RUN-\d{8})\b`.
- Vẫn được dùng:
  - chữ giữ chỗ (`L-NNN`, `T-NNN`, `RUN-YYYYMMDD-NN`, nhánh `claude/T-`);
  - mã dòng `PB-NN` của bảng tra lỗi (`scripts/failure-kind.sh` đọc nó);
  - mã luật của `docs/ops/ads-business-rules.md` (như `T-01`, hai chữ số);
  - dẫn chiếu `ADR-NNNN` tới ADR chưa `superseded`.
- Cột Nguồn của bảng tra lỗi ghi bằng lời (app, job, lần chạy, PR), không ghi mã bài học.

### 3. Tra ngược lý do của một quy tắc

Đi từ quy tắc về lý do: `git blame` dòng quy tắc ra PR đã sửa nó, rồi đọc mã bài học trong mô tả PR đó. Vì vậy:

- PR áp dụng bài học (`/lesson apply`) sửa quy tắc theo mục 2 và ghi mã bài học trong mô tả PR;
- PR dọn ghi trong mô tả, cho mỗi quy tắc đã viết lại, các mã đã gỡ khỏi quy tắc đó, vì sau đợt dọn `git blame` trỏ vào PR dọn.

### 4. Hợp nhất ADR

ADR mới sửa một phần một ADR chưa `superseded` thì là ADR hợp nhất:

- nó chép lại mọi quy tắc còn đúng của ADR cũ, cùng phần mới;
- nó ghi `supersedes`;
- ADR cũ chuyển `superseded`.

Không để một quy tắc phải đọc từ hai ADR chưa `superseded` trở lên. ADR `accepted` vẫn không bị sửa nội dung (`docs/CLAUDE.md`). Quy tắc này áp cho ADR viết sau khi ADR-0044 được duyệt. Các chuỗi đang có, kể cả ADR-0017 và ADR-0030 với ADR này, được hợp nhất trong đợt dọn.

### 5. Bản cho app bỏ ADR `superseded`

- **`new-app.sh`** bỏ ADR `superseded` khi tạo app.
- **`template-sync.sh`**:
  - lọc mỗi phía theo trạng thái ở chính commit của phía đó, nên ADR vừa bị thay thành lệnh xóa ở app;
  - sau khi gộp, xóa thêm ở app mọi ADR `origin: template` đã `superseded` ở commit đích mà app còn giữ. Bước này dọn luôn các ADR đã bị thay từ trước, như ADR-0022 và ADR-0028.
- **Ngoại lệ của quy tắc "Không xóa tài liệu":** trong app, ADR `superseded` của dự án mẫu bị xóa, và chỉ `template-sync.sh` xóa chúng. Trong repo này ADR `superseded` vẫn được giữ.
- Tài liệu quy tắc không dẫn tới ADR `superseded`, kể cả nhắc mã bằng chữ.
- Ở app, ADR đã xóa không còn file để máy đọc trạng thái. Vì vậy `template-sync.sh` in danh sách tài liệu quy tắc (mục 1) của app còn nhắc mã ADR vừa xóa, và PR đồng bộ sửa các chỗ đó (dẫn sang ADR hợp nhất).

### 6. Kiểm bằng máy

Kiểm trong `scripts/tests/`, chạy ở CI, chỉ ở repo dự án mẫu (gác như test của `new-app.sh`, theo `scripts/new-app.sh`). App có tài liệu và ADR riêng ghi mã của chính app, những mã đó không gãy, nên app không chạy phần kiểm này. Danh sách chưa dọn là `scripts/tests/history-refs-allow.txt`, mỗi dòng một đường dẫn file hay một mã `ADR-NNNN`. PR đầu tiên tạo danh sách này cùng phần kiểm, với mọi file và ADR đang vi phạm lúc đó, trong đó có ADR-0040 (`T-080` trong "Việc AI phải tuân thủ") và ADR-0026 (dẫn ADR-0022). Danh sách chỉ được ngắn đi: mỗi PR dọn xóa dòng của thứ mình đã dọn, không PR nào thêm dòng.

Báo đỏ khi:

- tài liệu quy tắc ngoài danh sách chưa dọn có mã theo mẫu ở mục 2;
- tài liệu quy tắc ngoài danh sách chưa dọn nhắc mã một ADR `superseded`;
- bản app do `new-app.sh` dựng có link tương đối gãy.

Phần kiểm link bỏ qua hai loại link:

- link ra ngoài cây file (như `../../blob/status/...` tới nhánh `status` trên GitHub);
- link nằm trong khối code hay chữ code. Ví dụ trong mẫu viết thành chữ code, không viết thành link.

### 7. Dọn từng đợt

- Mỗi PR dọn vài file. Trong một PR dọn, mọi quy tắc của bản cũ phải còn ở bản mới và không bị nới. Người rà soát đối chiếu từng quy tắc.
- Thứ tự:
  1. PR cài đặt: các file kèm theo dưới đây, phần kiểm và danh sách chưa dọn;
  2. `CLAUDE.md` và `docs/CLAUDE.md`;
  3. `docs/auto-run.md`;
  4. bộ tài liệu quảng cáo cùng ADR hợp nhất của chuỗi quảng cáo;
  5. các file và chuỗi ADR còn lại.
- **Thử với app thật:** sau mỗi đợt lớn, đồng bộ sang một trong hai app theo `docs/ops/to-app.md`: phiên có người theo dõi, giữa hai phiên tự chạy của app, mở PR và merge ở app như một đợt đồng bộ thường. PR đồng bộ ghi những chỗ tài liệu sạch gây xung đột giả hay làm mất quy tắc.
- **Chú thích trong code và script** (khoảng 670 lần) không thuộc đợt dọn này. Việc đó đổi code, nên quyết riêng sau khi dọn xong tài liệu, theo quy trình plan.

**File phải sửa kèm, trong PR cài đặt:**

- `.claude/skills/lesson/SKILL.md`: bước apply theo mục 2 và 3;
- `docs/CLAUDE.md`: mục Khi viết ADR thêm quy tắc hợp nhất; mục Những điều không được làm thêm ngoại lệ của mục 5;
- `docs/ops/to-app.md`, mục 2: ADR `superseded` không đồng bộ, và bị xóa ở app;
- `docs/ops/new-app.md`, bước 5: ghi các thứ bị bỏ thêm;
- `docs/templates/app-proposal.md`: link ví dụ (`com.example.posemate.md`) viết thành chữ code;
- `docs/risk-scoring.md`: bỏ quy định ghi lý do vào mục Lịch sử thay đổi và bỏ bảng lịch sử ở cuối; lý do mỗi lần đổi thang điểm ghi trong mô tả PR, tra qua git log;
- `scripts/new-app.sh` và `scripts/template-sync.sh`, cùng test của chúng;
- `new-app.sh` làm trống thêm `docs/metrics/token-usage.csv`, chỉ giữ dòng đầu.

### 8. Khởi đầu lại repo chưa quyết

Khởi đầu lại repo nghĩa là bỏ lịch sử git và chuyển repo cũ thành repo lưu trữ. Việc này xét bằng một ADR riêng sau khi dọn xong. Nếu làm, theo thứ tự:

1. gỡ hết dòng `hold=` của cả hai app;
2. đồng bộ cả hai app lên commit cuối của repo cũ;
3. đổi `base=` của mỗi app sang commit gốc của repo mới.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Dọn tại chỗ, lịch sử ở chỗ dành riêng (đã chọn) | Một bản duy nhất; chính repo này cũng sạch; `template-sync` chỉ thêm phần lọc ADR | Một đợt dọn lớn; người bảo trì phải tra lý do qua `git blame` và mô tả PR |
| Repo mẫu riêng, giữ bằng tay | App nhận bản sạch nhất | Hai nguồn sự thật; repo mẫu không ai chạy thật nên cũ dần; phải định lại vai của `template-sync` và `from-app` |
| Lọc bằng script khi tạo và đồng bộ app | Một nguồn, không viết lại văn | Script chỉ cắt được mã dẫn chiếu, không cắt được câu kể chuyện hay chuỗi ADR |
| Thư mục lớp phủ chứa bản sạch của các tài liệu nhiều lịch sử | Bản sạch viết tốt, đồng bộ ổn định | Hai bản của mỗi tài liệu trong lớp phủ; cần kiểm cặp file để chúng không lệch |
| Bỏ ngay lịch sử git | Repo trông sạch | Không giúp gì cho app (app mới đã có repo git riêng); làm hỏng `base=` và `hold=` của hai app; mất đường tra ngược lý do |

## Hệ quả

- **Tích cực:**
  - App nhận tài liệu chỉ nói quy tắc đang dùng.
  - AI ở cả repo này lẫn app đọc ít chữ hơn mỗi phiên.
  - Quy tắc hiện tại đọc được từ một ADR, không phải ghép từ một chuỗi.
- **Phải chấp nhận:**
  - Một đợt dọn kéo dài nhiều PR.
  - Lý do chi tiết của một quy tắc phải tra qua `git blame` và mô tả PR.
  - Khi viết lại có thể làm mất một ý tinh tế; bước đối chiếu từng quy tắc ở mục 7 để chặn điều này.
  - Phần Bối cảnh của ADR còn mã dẫn chiếu, vì ADR `accepted` không được sửa.
  - Chú thích trong code còn mã dẫn chiếu tới khi có quyết định riêng.
- **Việc AI phải tuân thủ:** tài liệu quy tắc viết quy tắc ở thì hiện tại, lý do cần thiết viết thành một vế câu, không kể lại cách làm cũ; không ghi mã bài học, mã ticket hay mã phiên (`L-` hay `T-` cộng ba chữ số, `RUN-` cộng tám chữ số) trong tài liệu quy tắc, các mã đó chỉ ghi ở chỗ dành cho lịch sử của mục 1 của ADR-0044; chữ giữ chỗ, mã `PB-NN` của bảng tra lỗi và mã luật quảng cáo hai chữ số vẫn được dùng; PR áp dụng bài học sửa quy tắc theo cách này và ghi mã bài học trong mô tả PR; ADR mới sửa một phần ADR chưa `superseded` thì hợp nhất, chép lại quy tắc còn đúng và cho ADR cũ `superseded`; tài liệu quy tắc không dẫn tới hay nhắc mã ADR `superseded`; không thêm dòng vào `scripts/tests/history-refs-allow.txt`; PR dọn tài liệu giữ đủ mọi quy tắc của bản cũ, không nới quy tắc nào, và ghi trong mô tả PR các mã đã gỡ khỏi mỗi quy tắc.
