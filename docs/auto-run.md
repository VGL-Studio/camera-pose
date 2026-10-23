# QUY TRÌNH PHIÊN AI TỰ CHẠY

Phiên tự chạy là khoảng thời gian AI tự thực hiện các plan đã duyệt mà không cần người theo dõi. Phiên không gắn với giờ cố định: phiên bắt đầu khi dev đã duyệt xong và kích hoạt, có thể ngay từ buổi chiều, và kết thúc khi hết việc hoặc gặp một điều kiện kết thúc. Thường phiên chạy qua đêm, để tận dụng thời gian ngoài giờ làm việc và hạn mức sử dụng được làm mới.

Bốn nguyên tắc của phiên tự chạy:

1. **Tuần tự.** Mỗi lúc chỉ làm một task. Task sau bắt đầu từ nhánh chính đã có kết quả của task trước.
2. **Test viết trước.** Test được viết từ plan và phải thất bại đúng lý do trước khi có code.
3. **Rà soát độc lập.** Một phiên AI khác, không tham gia viết code, rà soát thay đổi trước khi đẩy lên CI.
4. **Không cố định giờ.** Người quyết định lúc bắt đầu; thời điểm muốn có bản staging (nếu cần) ghi trong danh sách chạy.

## 1. Kích hoạt phiên

1. Sau họp đánh giá rủi ro, AI lập danh sách chạy `runs/RUN-YYYYMMDD-NN.md` theo [mẫu phiên tự chạy](templates/run.md): các plan đã duyệt, thứ tự chạy, số task tối đa, thời điểm muốn có bản staging (nếu có).
2. Dev xem lại danh sách cùng mục Kiểm tra trước khi duyệt ([ops/pre-run-review.md](ops/pre-run-review.md)), rồi chuyển trạng thái sang `approved`. Chỉ duyệt khi `scripts/run-check.py` qua và không còn điểm chặn nào của lần rà soát chưa xử lý.
3. Dev kích hoạt phiên bằng workflow `start-run.yml` trên GitHub, hoặc bằng Run now của routine. Muốn bắt đầu vào một thời điểm cụ thể (ví dụ lúc hạn mức sử dụng được làm mới) thì hẹn một lần chạy cho routine. Ưu tiên hai cách này: chỉ phiên của routine được `resume-run.yml` tự tiếp tục khi hết hạn mức. Phiên mở bằng `claude --cloud` từ terminal dừng hẳn khi hết hạn mức, người phải mở lại (L-008); chỉ dùng cách này khi có lý do, ví dụ dùng cloud session credits (L-020), và ghi lý do vào mục Kiểm tra đầu phiên.
4. Mỗi lúc chỉ có một phiên tự chạy. Trong khi phiên đang chạy, người không merge vào nhánh chính, kể cả PR chỉ có tài liệu: mỗi commit mới trên nhánh chính bắt task đang làm chạy lại CI một vòng. `merge-if-green.sh` từ chối mọi PR không phải của phiên khi danh sách chạy trên nhánh báo cáo đang `running`, kể cả khi phiên đang chờ hạn mức được làm mới (mục 4, Tiếp tục phiên sau khi hết hạn mức).
5. Một danh sách chạy chỉ chạy một lần. Danh sách đã `finished` không được kích hoạt lại; muốn chạy lại thì lập danh sách mới.
6. Phiên chạy dưới tài khoản Claude và tài khoản GitHub hiện có của người phụ trách (ADR-0009). Vì dùng chung hạn mức, nên tránh kích hoạt phiên khi tài khoản đó vừa dùng nhiều trong ngày.

## 2. Kiểm tra đầu phiên

AI tạo nhánh báo cáo `claude/RUN-YYYYMMDD-NN-report` từ nhánh chính, ghi trạng thái phiên `running` và giờ bắt đầu vào danh sách chạy trên nhánh đó, đẩy nhánh lên, rồi kiểm tra như bảng dưới.

Nhánh báo cáo đã có và danh sách chạy trên đó đang `running` nghĩa là đây là **phiên tiếp tục**: phiên trước dừng giữa chừng, thường vì hết hạn mức, và `resume-run.yml` vừa kích hoạt lại. Không tạo lại nhánh báo cáo; làm theo mục 4, Tiếp tục phiên sau khi hết hạn mức.

Các điều kiện:

| Điều kiện | Không thỏa thì |
| --- | --- |
| Danh sách chạy trên nhánh chính có trạng thái `approved` | Không chạy task nào, kết thúc phiên, báo cáo lý do |
| Môi trường cloud dựng từ `scripts/cloud-setup.sh` mới nhất: `scripts/cloud-env-check.sh` qua. Chạy trước `scripts/check.sh` | Không chờ, không thử lại: môi trường cũ không tự khỏi. Không chạy task nào, kết thúc phiên ngay, chép thông báo của script vào báo cáo để người dán lại setup script |
| Môi trường build được: `scripts/check.sh` qua trên nhánh chính, để lỗi môi trường lộ ra trước khi viết code và để thư viện được tải sẵn. Chạy ở chế độ nền, song song với phần đầu của task đầu tiên (mục Kiểm tra build ở chế độ nền, dưới bảng này) | Lỗi hạ tầng: thử lại theo lịch ở bước 5. Lỗi khác: không chạy task nào, kết thúc phiên, báo cáo lý do |
| Nhánh chính đang xanh: lần chạy gần nhất của `main-guard.yml` qua, và không có issue hay PR nào đang mở mang nhãn `main-broken` | Không chạy task nào, kết thúc phiên, báo cáo lý do |
| Không có issue nào đang mở mang nhãn `env-broken` (kiểm tra môi trường hằng ngày, `docs/ops/staging-and-runs.md`, mục 2.4). `start-run.yml` đã kiểm tra; phiên kiểm tra lại vì phiên bắt đầu bằng Run now không qua workflow | Không chạy task nào, kết thúc phiên, báo cáo lý do |
| Danh sách qua `scripts/run-check.py <danh sách chạy>`, chạy một lần ở đầu phiên: spec đã chốt và trả lời hết câu hỏi, phụ thuộc đúng thứ tự, mọi kịch bản có test ([ops/pre-run-review.md](ops/pre-run-review.md)) | Lỗi về spec, ticket hay phụ thuộc của một task: bỏ qua task đó và các task phụ thuộc nó, chép thông báo vào báo cáo. Lỗi của `plan-check` để dòng `plan-check.py` bên dưới quyết định ngay trước task. Dòng chú ý chép vào báo cáo |
| Plan có trạng thái `approved` và nằm trong danh sách chạy | Bỏ qua task, ghi lý do |
| Ticket có trạng thái `approved` | Bỏ qua task, ghi lý do |
| Plan qua `scripts/plan-check.py <plan>`. Chạy ngay trước khi bắt đầu từng task, không chạy chung ở đầu phiên: plan có thể sửa file mà task trước trong cùng phiên tạo ra | Bỏ qua task trước khi tạo nhánh, chép thông báo của script vào báo cáo; plan và ticket giữ `approved`. Không tự sửa plan |
| Các ticket phụ thuộc đã `on-staging`, hoặc được merge trước đó trong cùng phiên | Bỏ qua task, ghi lý do |

### Kiểm tra build ở chế độ nền

`scripts/check.sh` trên cloud mất khoảng 8 phút vì Gradle bắt đầu với bộ nhớ đệm trống. Phiên không ngồi chờ:

1. Làm các kiểm tra nhanh trong bảng trên trước: danh sách chạy `approved`, `cloud-env-check.sh`, nhánh chính xanh, không có `env-broken`. Không thỏa thì xử lý như bảng, không chạy `check.sh`.
2. Chạy `check.sh` trong một worktree riêng của nhánh chính, ở chế độ nền: `git worktree add ../start-check origin/main`, rồi `(cd ../start-check && scripts/check.sh) > ../start-check.log 2>&1`. Worktree riêng là bắt buộc: thư mục chính sắp đổi sang nhánh của task, và đổi file dưới một bản build đang chạy làm kết quả kiểm tra sai. Thư viện và bộ nhớ đệm build nằm ở `~/.gradle`, dùng chung cho thư mục chính.
3. Trong lúc chờ: chạy `plan-check.py` cho task đầu, làm bước 1 và 2 của task đó (tạo nhánh, đọc tài liệu) và soạn test của bước 3. Chưa chạy Gradle.
4. Trước lần chạy Gradle đầu tiên của task, đợi `check.sh` xong và đọc phần cuối của `../start-check.log`. Qua thì `git worktree remove ../start-check` và đi tiếp. Không qua thì xử lý như dòng "Môi trường build được" của bảng; task đầu chưa đẩy gì lên nên không cần dọn.
5. Môi trường không chạy được lệnh ở chế độ nền thì làm như trước: chạy `check.sh` trên nhánh chính, xong mới bắt đầu task. Ghi điều này vào mục Bài học của báo cáo.

Ghi giờ kiểm tra đầu phiên xong vào bảng Dòng thời gian của báo cáo (mẫu phiên, mục Số liệu).

## 3. Các bước cho từng task

Mỗi task đi qua 10 bước theo thứ tự. Điều kiện dừng ở mục 4 của [risk-scoring.md](risk-scoring.md) áp dụng ở mọi bước.

| Bước | Việc làm | Kết quả |
| --- | --- | --- |
| 1. Tạo nhánh | Tạo nhánh `claude/T-NNN-<tu-khoa>` từ nhánh chính mới nhất. Không dùng nhánh mặc định mà routine tạo cho phiên | Nhánh riêng của task |
| 2. Đọc tài liệu | Đọc theo thứ tự trong [CLAUDE.md](CLAUDE.md) | Đủ ngữ cảnh |
| 3. Viết test trước | Viết test theo mục Kiểm thử của plan, chạy và xác nhận test thất bại đúng lý do | Commit chứa test |
| 4. Viết code | Viết code cho đến khi test qua, chỉ trong danh sách file của plan | Code chạy được |
| 5. Kiểm tra cục bộ | Build, lint, unit test, screenshot test, quy tắc phụ thuộc module, đối chiếu danh sách file | Sẵn sàng rà soát |
| 6. Rà soát độc lập | Phiên AI khác rà soát thay đổi | Danh sách điểm phát hiện đã xử lý |
| 7. Commit và đẩy | Commit theo quy ước, đẩy nhánh, mở PR. PR của task chỉ có code, test, ảnh chuẩn và ảnh so sánh thiết kế | PR liên kết tới plan |
| 8. CI đầy đủ | `pr-check.yml` chạy cổng chất lượng, thêm test trên emulator khi PR chạm vào code chạy trong app | CI xanh |
| 9. Merge | Merge bằng `scripts/merge-if-green.sh` | Nhánh chính có thay đổi của task |
| 10. Ghi nhật ký | Khi task có kết quả cuối, ghi vào cuối plan và cập nhật trạng thái trên nhánh báo cáo | Người đọc được kết quả |

### Bước 3. Viết test trước

- Viết toàn bộ test được nêu trong mục Kiểm thử của plan, trước khi viết bất kỳ dòng code nào của chức năng.
- Chạy test và xác nhận test **thất bại đúng lý do**: vì chức năng chưa có, không phải vì test viết sai cú pháp hay lỗi môi trường. Với màn hình hay lớp chưa tồn tại, AI tạo khung rỗng (hàm trả về giá trị mặc định) để test biên dịch được và thất bại ở phần kiểm tra.
- Test nào qua ngay khi chưa có code là test không kiểm tra gì, phải viết lại.
- Commit test riêng, trước commit code, để lịch sử cho thấy test có trước.
- Đẩy nhánh của task lên ngay sau mỗi commit, ở bước này và bước 4, không đợi tới bước 7. Hết hạn mức thì phiên dừng ngay, không kịp làm gì thêm; phần chưa đẩy lên là phần mất. Commit trên nhánh cũng là dấu hiệu phiên còn sống cho `resume-run.yml`.
- Chỉ được bỏ qua bước này với phần thay đổi nằm trong mục ngoại lệ đã được người duyệt đồng ý trong plan.
- Screenshot test cho màn hình mới: mỗi lệnh `captureRoboImage` ghi rõ đường dẫn ảnh, dạng `"src/test/screenshots/<Ten>.png"`. Chạy cục bộ, `scripts/check.sh` tự ghi ảnh chuẩn cho những ảnh còn thiếu và giữ nguyên ảnh đã có. Commit ảnh chuẩn cùng PR (CI báo lỗi cho từng ảnh thiếu) và đánh dấu trong báo cáo để người xem. Ảnh chuẩn của màn hình cũ chỉ được cập nhật khi plan nêu rõ.

### Bước 4. Viết code

- Chỉ tạo và sửa các file trong danh sách file của plan.
- Phương án chính không chạy và plan có phương án dự phòng đã duyệt (mục 3.4 của plan): chuyển sang phương án đó theo `risk-scoring.md`, mục 4, thay vì dừng task. Ghi vào nhật ký lúc chuyển, điều kiện chuyển đã gặp và lỗi của phương án chính; báo cáo phiên liệt kê ở mục Cần người kiểm tra.
- Không sửa test đã viết ở bước 3 để làm cho test qua. Nếu test sai ở chi tiết không làm thay đổi điều nó kiểm tra (ví dụ sai tên một hàm), được sửa và ghi lý do vào nhật ký. Nếu cần thay đổi điều test kiểm tra thì dừng task.

### Bước 5. Kiểm tra cục bộ

- Chạy `scripts/check.sh`. Script gồm quy tắc phụ thuộc module, ktlint, unit test, test giao diện trên JVM, screenshot test, Android Lint và build bản staging. CI dùng đúng script này, nên qua ở bước 5 thì thường cũng qua ở bước 8.
- So sánh danh sách file thay đổi thực tế với danh sách file trong plan. Có file ngoài danh sách thì dừng task.
- Feature có thiết kế (`docs/features/<feature>/design/screens/`): chạy `scripts/design-compare.sh <feature>` sau `scripts/check.sh`. Kết quả trong `design/review/` để rà soát, không phải cổng chặn (ADR-0020). Ảnh thiết kế nào báo thiếu ảnh chuẩn screenshot là thiếu test, phải thêm theo plan.
- Lỗi tải thư viện hay công cụ (mã 429, 5xx, hết thời gian chờ, mất kết nối) là lỗi hạ tầng, không phải lỗi của code, và không tính vào giới hạn 3 lần sửa. Phiên chạy qua đêm có thời gian, nên chờ lâu thay vì bỏ cuộc sớm: chạy lại sau 1, 2, 4, 8 rồi 15 phút (tổng khoảng 30 phút). Vẫn lỗi thì kết thúc phiên theo mục 4.
- Mỗi lần chạy lại sau lỗi tải, dùng `GRADLE_REFRESH_DEPENDENCIES=true scripts/check.sh`. Gradle ghi nhớ lần tải lỗi và các lần sau báo "Could not find" mà không gọi lại mạng; biến này (đọc trong init script của `scripts/cloud-setup.sh`) buộc Gradle hỏi lại (RUN-20261016-01).
- Môi trường cloud tải mọi thư viện Maven Central qua mirror của Google, nhờ init script do `scripts/cloud-setup.sh` ghi: kho `mavenCentral()`, thư viện mà plugin tải qua plugin portal, và Robolectric. Log còn thấy `repo.maven.apache.org` hay `repo1.maven.org` nghĩa là init script chưa phủ đường tải đó: ghi vào báo cáo kèm dòng log, đề xuất bài học. Phiên không tự thêm init script, không tự đổi kho tải hay biến môi trường để đi vòng: môi trường chặn các lệnh đó (RUN-20261016-01), và cấu hình tải phải nằm ở setup script để mọi phiên giống nhau.

### Bước 6. Rà soát độc lập

Một phiên AI riêng được khởi tạo để rà soát. Phiên này chỉ nhận plan, spec, thiết kế, ADR và phần thay đổi; không nhận quá trình làm việc của phiên viết code, để đánh giá với con mắt mới.

Nội dung rà soát:

- Thay đổi có đáp ứng đủ tiêu chí chấp nhận trong spec không.
- Test có thực sự kiểm tra tiêu chí chấp nhận không, hay chỉ kiểm tra những gì code đang làm.
- Có tuân thủ mục "Việc AI phải tuân thủ" của các ADR không.
- Có chạm vào phần ngoài phạm vi plan không.
- Có cấu hình production, khóa bí mật, mã quảng cáo thật trong thay đổi không.
- Code có theo đúng mẫu đã có trong dự án không.
- Feature có thiết kế: giao diện có đúng thiết kế không. Xem từng ảnh ghép trong `design/review/` theo mục 7 của mẫu thiết kế; chữ sai, thiếu trạng thái, sai thứ tự hay vai trò màu là mức chặn; lệch nhỏ về khoảng cách là mức ghi chú. Tỷ lệ điểm ảnh khác chỉ để tham khảo.

Kết quả rà soát chia ba mức:

| Mức | Xử lý |
| --- | --- |
| Chặn | Quay lại bước 4. Tính vào giới hạn 3 lần sửa cho cùng một lỗi. |
| Nên sửa | Sửa nếu nằm trong phạm vi plan, nếu không thì ghi vào báo cáo. |
| Ghi chú | Ghi vào báo cáo để người xem. |

Nếu phiên rà soát và phiên viết code hiểu spec khác nhau, AI không tự chọn bên nào mà dừng task, vì đây là dấu hiệu spec hoặc plan chưa rõ.

### Bước 7. Commit và đẩy

- Nội dung commit theo dạng `<loại>(T-NNN): <mô tả ngắn>`, với loại là `test`, `feat`, `fix`, `refactor` hoặc `docs`. Mỗi commit có dòng `Co-Authored-By: Claude`.
- PR có tiêu đề `T-NNN: <tên task>`, phần mô tả gồm đường dẫn tới plan, tóm tắt thay đổi, kết quả rà soát độc lập và đường dẫn tới phiên chạy. Vì PR mang tên tài khoản của người, các dấu hiệu này giúp phân biệt việc của AI.
- Feature có thiết kế: commit `design/review/` cùng PR và dán `review/summary.md` vào mô tả PR, để người duyệt xem ảnh so sánh ngay trên PR.
- PR của task không chứa nhật ký plan, trạng thái plan, ticket hay báo cáo phiên. Các tài liệu đó đi qua PR báo cáo cuối phiên (mục 4), để chúng lên nhánh chính kể cả khi task bị dừng.
- Trong môi trường cloud của routine, `gh pr` và `gh issue` không chạy được (proxy GitHub chặn GraphQL, trả lỗi 403). Mọi thao tác với GitHub trong phiên đều dùng REST qua `gh api`, kể cả kiểm tra issue `main-broken` ở mục 2, bước 9 và khởi động `staging.yml` ở mục 4. Ví dụ:
  - Mở PR: `gh api -X POST repos/{owner}/{repo}/pulls -f title=... -f head=<nhánh> -f base=main -f body=...`
  - Tìm issue `main-broken` đang mở: `gh api "repos/{owner}/{repo}/issues?state=open&labels=main-broken"`
  - Khởi động `staging.yml`: `gh api -X POST repos/{owner}/{repo}/actions/workflows/staging.yml/dispatches -f ref=main`
  - Khởi động `device-session.yml`: `gh api -X POST repos/{owner}/{repo}/actions/workflows/device-session.yml/dispatches -f ref=main -f "inputs[reason]=RUN-YYYYMMDD-NN"`; tải artifact: `gh api repos/{owner}/{repo}/actions/runs/<id>/artifacts`, rồi tệp zip theo `archive_download_url`
  - Chờ một workflow chạy xong: `scripts/wait-run.sh`, không hỏi trạng thái nhiều lần (bước 8, mục Chờ workflow)
  - Mở PR nháp sửa nguyên nhân khi task dừng: `gh api -X POST repos/{owner}/{repo}/pulls -f title=... -f head=<nhánh> -f base=main -f body=... -F draft=true`, rồi gắn nhãn: `gh api -X POST repos/{owner}/{repo}/issues/<số PR>/labels -f "labels[]=needs-human"`

### Bước 8. CI đầy đủ

- `pr-check.yml` chạy toàn bộ kiểm tra ở bước 5. Test trên emulator chỉ chạy khi PR chạm vào code giao diện hoặc test giao diện (ADR-0010).
- AI chờ CI bằng `scripts/wait-run.sh --pr <số PR>` cho đến khi `pr-check` kết thúc (mục Chờ workflow bên dưới), rồi gọi `merge-if-green.sh`.

**Chờ workflow.** Mỗi lần hỏi GitHub trạng thái là một lượt gọi model đọc lại toàn bộ ngữ cảnh của phiên, nên không hỏi lặp lại bằng tay. `scripts/wait-run.sh` tự chờ trong một lệnh và in một dòng kết quả: mã 0 xanh, 1 đỏ (kèm tên job đỏ), 2 hết thời gian chờ thì gọi lại. Chạy lệnh ở chế độ nền và làm việc khác trong lúc chờ nếu có (mục 4); không có việc khác thì chạy thường. Môi trường giới hạn một lệnh 10 phút thì dùng `--timeout 9` và gọi lại khi nhận mã 2.
- CI đỏ thì AI đọc log để phân biệt lỗi thật với lỗi chập chờn. Lỗi chập chờn được chạy lại một lần. Lỗi thật thì quay lại bước 4, tối đa 3 lần cho cùng một lỗi, sau đó dừng task.
- Khi `ui-test` đỏ, log của job có bước "Chi tiết test trên emulator" in toàn văn lỗi của từng test và log của app trên emulator (ADR-0019). Đọc bước đó trước khi kết luận. Bản đầy đủ nằm trong artifact `ui-test-report` của lần chạy: `gh api repos/{owner}/{repo}/actions/runs/<id>/artifacts`, rồi tải tệp zip theo `archive_download_url`.
- Không thêm lệnh in chẩn đoán vào test để tìm lỗi trên CI; đọc chi tiết và log nói trên.

### Bước 9. Merge

- Chỉ merge bằng `scripts/merge-if-green.sh <số PR>`. Script tự kiểm tra mọi điều kiện của cổng chất lượng và từ chối merge nếu thiếu điều kiện nào (ADR-0010). AI không merge bằng bất kỳ cách nào khác.
- Script báo nhánh chưa cập nhật theo nhánh chính thì cập nhật nhánh, đợi CI chạy lại rồi gọi lại script.
- Script từ chối vì lý do khác thì không tìm cách vượt qua, mà dừng task và ghi lý do vào nhật ký.
- Sau khi merge, chờ `main-guard` của commit merge: `scripts/wait-run.sh --workflow main-guard.yml --since <giờ merge>` ở chế độ nền, trong lúc làm bước 10 và bước 1, 2 của task sau. Commit merge chỉ đổi tài liệu thì `main-guard` không chạy (`paths-ignore`), không cần chờ.
- Trước khi đẩy commit đầu tiên của task sau, phải có kết quả của lần chờ đó, rồi kiểm tra lại không có issue hay PR nào đang mở mang nhãn `main-broken`. Kiểm tra ngay sau khi merge là quá sớm: `main-guard` mất khoảng 5 đến 12 phút (gấp đôi khi nó chạy lại test thất bại), và issue chỉ được mở khi nó xong (L-028).
- Có issue `main-broken`: commit gây lỗi (dòng `<!-- culprit:<sha> -->` trong thân issue) là commit merge của một task chính phiên đã merge thì làm theo mục Khi nhánh chính đỏ vì task của phiên; commit khác thì kết thúc phiên (mục 4).
- `main-guard` xanh sau khi chạy lại test thất bại thì có issue mang nhãn `flaky` (ADR-1002): phiên làm tiếp như bình thường, chép issue vào báo cáo.

### Khi nhánh chính đỏ vì task của phiên

Theo ADR-1002, mục 4. Phiên chỉ revert commit merge của chính phiên, không bao giờ revert commit của người hay của phiên khác, và không đóng, sửa hay bình luận vào issue `main-broken`.

1. Đọc issue bằng REST (bước 7) và lấy commit gây lỗi. So với các commit merge mà phiên ghi ở dòng thời gian của báo cáo; không khớp thì kết thúc phiên.
2. Mở PR từ nhánh `revert/<7 ký tự đầu>` mà `main-guard` đã đẩy: `gh api -X POST "repos/{owner}/{repo}/pulls" -f head=revert/<sha> -f base=main -f title="Revert T-NNN (main-broken #<số issue>)" -f body="..."`. Không sửa nhánh đó: `merge-if-green.sh` chỉ cho qua đúng một commit `git revert` trên nhánh chính mới nhất (`scripts/is-exact-revert.sh`). Không có nhánh `revert/` (issue ghi "Không tự revert được"), hoặc nhánh chính đã có commit mới sau commit gây lỗi khiến bản revert không còn nằm ngay trên nhánh chính: kết thúc phiên.
3. Chờ CI bằng `scripts/wait-run.sh --pr <số>`, rồi merge bằng `scripts/merge-if-green.sh <số>`. Script từ chối thì kết thúc phiên, không tìm cách khác.
4. Chờ `main-guard` của commit revert (`scripts/wait-run.sh --workflow main-guard.yml --since <giờ merge>`). Xanh thì issue tự đóng; kiểm lại không còn issue `main-broken` đang mở rồi mới làm task sau. Còn đỏ thì kết thúc phiên.
5. Task bị revert: ticket chuyển `stopped`, nhật ký plan ghi "đã merge rồi revert" kèm issue và PR revert, chuẩn bị cho người như task dừng (mục Chuẩn bị cho người khi task dừng). Task phụ thuộc task đó bị bỏ qua.

### Bước 10. Ghi nhật ký

- Chỉ ghi khi task đã có kết quả cuối: đã merge, hoặc đã dừng. Không ghi trước khi biết kết quả CI.
- Ghi giờ các khâu của task vào bảng Dòng thời gian của báo cáo (mẫu phiên, mục Số liệu): tạo nhánh, test viết trước xong, đẩy PR, CI xong, merge hoặc dừng. Giờ lấy từ commit, PR và kết quả `wait-run.sh`, ghi tới phút theo UTC.
- Ghi trên nhánh báo cáo, không ghi trên nhánh của task. Ghi vào mục Nhật ký thực hiện ở cuối plan theo mẫu, cập nhật dòng của task trong báo cáo phiên, rồi đẩy nhánh báo cáo lên. Nhờ vậy nếu phiên dừng đột ngột (hết hạn mức), người vẫn đọc được kết quả các task đã xong trên nhánh báo cáo.
- Plan chuyển sang `done`, ticket chuyển sang `on-staging`.
- Task bị dừng: plan và ticket chuyển sang `stopped`, nhánh và PR của task được giữ nguyên, không merge.
- Task bị dừng: chuẩn bị sẵn cho người, để sáng hôm sau người chỉ cần xem và duyệt (mục Chuẩn bị cho người khi task dừng).

Sau đó AI chuyển sang task tiếp theo trong danh sách. Task phụ thuộc vào task bị dừng được bỏ qua.

### Chuẩn bị cho người khi task dừng

Phiên không tự xoay xở khi dừng, nhưng soạn sẵn cách tiếp tục, để một lần dừng chỉ tốn vài phút của người thay vì một vòng làm lại:

1. **Plan phiên bản mới, trạng thái `draft`:** trên nhánh báo cáo, ngay sau mục nhật ký của lần dừng. Tăng `version`, xóa `reviewer`, `cross_reviewer`, `approved_at`, `run_order`; mục 1 ghi "vì sao plan chưa đủ tốt" theo cách hiểu của phiên và thay đổi so với phiên bản trước; mục 3.1 ghi cách tiếp tục (thường là tiếp tục trên nhánh cũ); mục 9 thêm chỗ trống cho phiên bản mới. Plan mới phải qua `scripts/plan-check.py`. Plan vẫn ở trạng thái `stopped` cho tới khi người duyệt phiên bản mới.
2. **PR sửa nguyên nhân, khi nguyên nhân nằm ngoài plan** (ví dụ test của script đọc file mà plan đổi, L-020) và cách sửa nhỏ, rõ: nhánh `claude/fix-T-NNN-<tu-khoa>` tạo từ nhánh chính, mở PR ở dạng nháp (`draft: true`), gắn nhãn `needs-human`, mô tả ghi lý do và cách đã thử. Phiên không merge PR này và không đổi nó sang sẵn sàng; `merge-if-green.sh` từ chối PR nháp và PR mang nhãn `needs-human`. PR này không được chạm cấu hình production, bí mật, `scripts/release.sh` hay tài liệu quy trình (tài liệu quy trình đổi qua retro, mục 6).
3. **Danh sách chạy tiếp theo, trạng thái `draft`:** `runs/RUN-YYYYMMDD-NN.md` mới với task bị dừng (plan phiên bản mới) và các task bị bỏ qua vì phụ thuộc, ghi tên nhánh để tiếp tục.

Ba thứ này đi qua PR báo cáo (trừ PR sửa nguyên nhân) và được liệt kê trong báo cáo phiên, mục Task bị dừng.

### Tiếp tục task đã dừng

Task bị dừng không được chạy lại với cùng phiên bản plan. Task chưa xong vì hết hạn mức hay lỗi hạ tầng thì không bị coi là dừng. Hết hạn mức thì chính phiên đó tiếp tục khi hạn mức được làm mới (mục 4). Lỗi hạ tầng, hoặc phiên không được tiếp tục (quá 24 giờ, người dừng hẳn): plan không cần sửa, đưa cùng phiên bản vào danh sách chạy mới, ghi trong danh sách chạy tên nhánh để phiên tiếp tục trên đó.

1. Nhóm trả lời "vì sao plan chưa đủ tốt" (risk-scoring.md, mục 4), AI sửa plan, tăng `version`, ghi rõ cách tiếp tục trong mục 3.1 của plan. Thường phiên đã soạn sẵn bản này (mục Chuẩn bị cho người khi task dừng); người xem, sửa nếu cần, merge PR sửa nguyên nhân (gỡ nhãn `needs-human` trước), rồi duyệt:
   - **Tiếp tục trên nhánh cũ:** ghi tên nhánh và số PR. Phiên cập nhật nhánh theo nhánh chính, chỉ làm phần plan thay đổi, bước 3 chỉ áp dụng cho test mới hoặc test đổi điều kiểm tra.
   - **Làm lại từ đầu:** người đóng PR cũ trước khi chạy. Phiên làm đủ 10 bước trên nhánh mới.
2. Plan phiên bản mới được duyệt như plan mới, ticket chuyển lại `approved`.
3. Plan được đưa vào một danh sách chạy mới.

## 4. Kết thúc phiên

Phiên kết thúc khi gặp một trong các điều kiện sau:

| Điều kiện | Xử lý task đang làm |
| --- | --- |
| Hết task trong danh sách, hoặc đạt số task tối đa | Không có |
| Gần đến thời điểm muốn có bản staging, không đủ thời gian cho task tiếp theo | Không bắt đầu task mới |
| Hết hạn mức sử dụng | Phiên dừng ngay, không làm được gì thêm. Phần đã đẩy lên còn nguyên (bước 3). `resume-run.yml` kích hoạt lại phiên khi hạn mức được làm mới (mục Tiếp tục phiên sau khi hết hạn mức) |
| Lỗi hạ tầng còn sau lịch thử lại ở bước 5 | Như trên. Plan và ticket giữ `approved`; báo cáo ghi tên nhánh để lần sau tiếp tục trên nhánh đó |
| Nhánh chính đỏ sau một lần merge (xuất hiện issue mang nhãn `main-broken`) | Dừng ngay, không làm thêm task nào. Trừ khi commit gây lỗi là commit merge của một task chính phiên: revert theo mục 3, Khi nhánh chính đỏ vì task của phiên, rồi làm tiếp các task không phụ thuộc task đó (ADR-1002) |

### Tiếp tục phiên sau khi hết hạn mức

Phiên dùng hạn mức của tài khoản Claude (ADR-0009). Hết hạn mức thì phiên dừng giữa chừng, không tự chờ được. Việc chờ do GitHub làm:

1. Mỗi giờ, `resume-run.yml` chạy `scripts/resume-check.py`: tìm danh sách chạy đang `running` trên nhánh báo cáo mà mọi nhánh của phiên (nhánh báo cáo, nhánh task và nhánh `claude/fix-T-NNN-...` của các ticket trong danh sách) không có commit nào trong 90 phút. Phiên còn sống luôn có commit dày hơn thế: đẩy sau mỗi commit của task (bước 3), đẩy nhánh báo cáo sau mỗi task (bước 10) và trong lúc viết báo cáo (bước 2 bên dưới); lần chờ dài nhất (CI, emulator) dưới 40 phút.
2. Tìm thấy thì workflow kích hoạt lại routine với cùng danh sách chạy. Hạn mức chưa được làm mới thì phiên mới không làm được gì, không đẩy gì, và giờ sau workflow thử lại. Hạn mức được làm mới thì phiên tiếp tục.
3. Không tiếp tục khi: phiên bắt đầu hơn 24 giờ trước (người quyết định), nhánh chính đỏ (`main-broken`), môi trường lỗi (`env-broken`).

Phiên tiếp tục làm như sau:

1. Lấy nhánh báo cáo có sẵn, đọc danh sách chạy, dòng thời gian, nhật ký các plan trên nhánh đó. Ghi vào dòng thời gian: giờ tiếp tục, và giờ commit cuối của phiên trước làm giờ dừng.
2. Làm lại các kiểm tra đầu phiên (mục 2), trừ việc tạo nhánh báo cáo. Kiểm tra build vẫn chạy nền như mục 2.
3. Task đã có nhật ký (đã merge, đã dừng, đã bỏ qua) thì không làm lại.
4. Task đang làm dở: tiếp tục trên nhánh của nó. Đọc các commit đã có để biết đã tới bước nào. PR đã merge mà chưa có nhật ký thì chỉ ghi nhật ký. PR đang mở thì xem CI rồi đi tiếp từ bước 8. Chưa có commit test thì làm lại task từ bước 3 trên nhánh đó.
5. Các task còn lại làm như thường. Gần tới `staging_needed_by` thì áp dụng điều kiện kết thúc như thường.
6. Báo cáo phiên ghi mục Tiếp tục: số lần dừng vì hết hạn mức, giờ dừng, giờ tiếp tục, thời gian chờ.

Muốn dừng hẳn một phiên đang chờ (ví dụ để merge việc gấp): người sửa trạng thái danh sách chạy trên nhánh báo cáo sang `finished`, ghi lý do "người dừng" vào báo cáo, rồi lập danh sách chạy mới cho các task chưa xong. Hoặc tắt workflow `resume-run`.

Sau đó AI:

1. Nếu trong phiên có ít nhất một task được merge, chạy kịch bản chuẩn trên emulator trước khi phát (`docs/ops/test-sessions.md`, mục 4; `docs/ops/staging-and-runs.md`, mục 2.5):
   - Ghi giờ hiện tại (UTC), khởi động `device-session.yml` trên nhánh chính (`reason` là mã phiên), rồi chờ bằng `scripts/wait-run.sh --workflow device-session.yml --since <giờ vừa ghi>` ở chế độ nền (khoảng 12 phút). **Trong lúc chờ, làm bước 2.** Xong thì tải artifact `device-session`.
   - Không tải được artifact (thường do mạng của môi trường chặn nơi lưu artifact, L-030): đọc bản tóm tắt trong cuối log của job, ghi vào báo cáo "chưa phân tích được" kèm lỗi tải, và vẫn quyết định phát hay không theo crash và ANR của bản tóm tắt. Người tải artifact và nhờ AI phân tích sau, trong phiên có người theo dõi.
   - Phân tích thư mục phiên theo `docs/CLAUDE.md`, mục Khi phân tích một phiên thử, nhưng ghi kết quả vào mục Kiểm thử trên máy ảo của báo cáo phiên thay vì một file `docs/test-sessions/` riêng. Phản hồi `FB-...` soạn sẵn đi qua PR báo cáo.
   - **Có crash hoặc ANR** (workflow đỏ, `summary.md` ghi "chặn"): không khởi động `staging.yml`. Ghi vào báo cáo dấu vết lỗi và commit nghi gây ra, soạn phản hồi mức `chan`. Nhánh chính vẫn xanh nên không mở `main-broken`; người quyết định sửa trước hay phát.
   - Workflow đỏ mà không có `summary.md` (lỗi hạ tầng của emulator): chạy lại một lần; vẫn lỗi thì ghi vào báo cáo và vẫn phát staging, vì cổng chất lượng đã qua.
   - Không có crash hay ANR: ghi giờ, khởi động `staging.yml` để build bản staging từ nhánh chính và phát cho nhóm qua Firebase App Distribution, rồi chờ bằng `scripts/wait-run.sh --workflow staging.yml --since <giờ vừa ghi>` ở chế độ nền (khoảng 7 phút). Trong lúc chờ, viết mục Kiểm thử trên máy ảo của báo cáo.
2. Viết báo cáo phiên vào file danh sách chạy, **trong lúc chờ các workflow ở bước 1** chứ không chờ xong mới viết. Không có task nào merge thì viết ngay. Thứ tự:
   - Cập nhật nhánh báo cáo theo nhánh chính.
   - Viết các mục không phụ thuộc emulator và staging: tóm tắt, kết quả từng task, cần người kiểm tra, task bị dừng, thời gian, số liệu và dòng thời gian, token, bài học, kiểm chứng bài học cũ, đề xuất.
   - Khi workflow xong: điền mục Kiểm thử trên máy ảo, Bản staging và các dòng còn lại của Dòng thời gian, sửa phần tóm tắt nếu kết quả làm nó đổi.
   - Chuyển trạng thái phiên sang `finished`, ghi giờ kết thúc.
   - Commit và đẩy nhánh báo cáo sau mỗi phần viết xong, không đợi cuối: hết hạn mức giữa chừng thì phần đã viết còn lại, và commit cho `resume-run.yml` biết phiên còn sống.

   Đo token khi viết mục Token: `scripts/token-usage.py report --since <started_at> --run RUN-YYYYMMDD-NN --csv docs/metrics/token-usage.csv`, dán bảng vào mục Token (`docs/ops/token-usage.md`). Không đo được thì ghi lý do và đi tiếp; việc đo không bao giờ chặn phiên.
3. Mở PR báo cáo, tiêu đề `RUN-YYYYMMDD-NN: báo cáo phiên`. PR này chỉ có tài liệu: danh sách chạy, nhật ký và trạng thái của plan, trạng thái ticket, `docs/metrics/token-usage.csv`, `docs/STATUS.md` sinh lại bằng `scripts/status.py` ngay trước khi mở PR, bài học mới trong `docs/lessons.md` (mục 6), plan phiên bản mới và danh sách chạy tiếp theo ở trạng thái `draft` (mục 3, Chuẩn bị cho người khi task dừng).
4. Merge PR báo cáo bằng `scripts/merge-if-green.sh`. Script từ chối (ví dụ nhánh chính đang đỏ) thì để PR mở và ghi lý do vào phần mô tả PR; người merge sau.

## 5. Báo cáo phiên

Báo cáo phiên là thứ dev đọc đầu tiên khi bắt đầu làm việc lại. Báo cáo gồm:

- Kết quả từng task: đã merge, bị dừng, bị bỏ qua, kèm lý do.
- Những gì người cần tự kiểm tra: ảnh chuẩn screenshot mới, tiêu chí kiểm tra thủ công, điểm rà soát mức ghi chú.
- Task bị dừng: dừng ở bước nào, vì sao, đề xuất hướng xử lý.
- Thông tin bản staging: phiên bản, kênh phát.
- Thời gian chạy và mức sử dụng của phiên, token của từng task so với ước tính trong plan (`docs/ops/token-usage.md`).
- Số liệu của phiên (mẫu danh sách chạy, mục Số liệu).
- Bài học của phiên, và kết quả kiểm chứng các bài học `applied` liên quan tới phiên này (mục 6).
- Issue mang nhãn `flaky` đang mở (số, tên test) và task bị revert trong phiên, nếu có (ADR-1002).

## 6. Bài học và cải tiến quy trình

Mỗi phiên là một lần học. Bài học được ghi trong phiên, quyết định ở retro, làm thành PR cải tiến, rồi kiểm chứng ở phiên sau. Sổ bài học là [lessons.md](lessons.md).

**Trong phiên, AI ghi bài học** vào mục Bài học của báo cáo phiên khi gặp một trong các tình huống sau, kể cả khi task thành công:

- Task dừng, hoặc phiên kết thúc sớm.
- Lỗi hạ tầng, kể cả khi thử lại thì qua.
- Phải đoán vì tài liệu, plan hay mẫu không nói rõ, hoặc tài liệu nói sai so với thực tế.
- Một bước tốn thời gian bất thường so với mục Số liệu của các phiên trước.
- Một cách làm hiệu quả đáng ghi thành hướng dẫn cho lần sau.

Mỗi bài học ghi: chuyện gì đã xảy ra, bằng chứng (log, PR, thời gian), đề xuất sửa tài liệu nào và sửa gì, loại và phạm vi theo [lessons.md](lessons.md). PR báo cáo thêm các bài học này vào sổ với trạng thái `proposed`.

**Phiên kiểm chứng bài học cũ:** đọc các bài học `applied` trong sổ; bài học nào liên quan tới việc phiên vừa làm thì ghi vào báo cáo cải tiến đó có hiệu quả hay không, kèm bằng chứng, và cập nhật dòng **Kiểm chứng** trong sổ.

**Phiên không tự sửa quy trình.** Trong phiên tự chạy, AI không sửa `CLAUDE.md`, `docs/CLAUDE.md`, `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/templates/`, `scripts/` hay `.github/`, dù thấy chỗ cần sửa, trừ khi plan đã duyệt nêu rõ. AI chỉ đề xuất qua bài học. Như vậy các hàng rào an toàn không bị nới lỏng khi không có người theo dõi.

**Retro sau mỗi phiên**, khoảng 15 phút, khi dev đọc báo cáo phiên:

1. Với mỗi bài học `proposed`: nhận (`accepted`) hoặc bỏ (`rejected`, ghi lý do).
2. Với mỗi bài học có bằng chứng kiểm chứng: xác nhận `verified`, hoặc mở bài học mới nếu cải tiến chưa đủ.
3. AI, trong phiên có người theo dõi, làm các bài học `accepted` thành một PR cải tiến. Sau khi merge, AI cập nhật bài học sang `applied` và ghi số PR.
4. Ở repo app, bài học phạm vi `chung` còn được đưa về dự án mẫu qua issue `from-app` (`docs/ops/from-app.md`).
5. Mỗi vài phiên, xem mục Số liệu của các phiên để hiệu chỉnh các con số ở mục 8.

## 7. Vùng an toàn của phiên

- Phiên chỉ có cấu hình staging. Không có mã quảng cáo thật, cấu hình Firebase production, khóa R2 production hay khóa ký ứng dụng.
- Phiên không đọc, in ra hay chuyển đi bất kỳ bí mật nào của repo. Bí mật trên GitHub chỉ gồm cấu hình staging và mã kích hoạt routine.
- Phiên không chạm vào `scripts/release.sh`. Phát hành production do người làm từ máy của mình (ADR-0009).
- Phiên không đẩy thẳng lên nhánh chính. Mọi thay đổi đi qua nhánh của task, PR, CI và script merge.
- Phiên không sửa `.github/`, `scripts/merge-if-green.sh` hay quyền truy cập, trừ khi đó chính là nội dung của một plan rủi ro cao đã được duyệt chéo. Phiên không sửa tài liệu quy trình (mục 6).
- Vì repo dùng gói GitHub Free, GitHub không chặn merge ở mức hệ thống. Lớp bảo vệ là script merge và workflow `main-guard.yml` (ADR-0010).
- Mọi việc phiên làm đều để lại dấu vết trong nhánh, PR, nhật ký plan và báo cáo phiên.

## 8. Những điểm sẽ hiệu chỉnh

Các con số dưới đây được đặt tạm và sẽ hiệu chỉnh qua retro (mục 6) dựa trên mục Số liệu của các phiên:

- Số task tối đa mỗi phiên.
- Giới hạn 3 lần sửa cho cùng một lỗi.
- Thời gian dự trù để build và phát bản staging cuối phiên.
- Lịch thử lại khi lỗi hạ tầng (bước 5).
