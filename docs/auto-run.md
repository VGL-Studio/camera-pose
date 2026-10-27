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
3. Dev kích hoạt phiên bằng workflow `start-run.yml` trên GitHub, hoặc bằng Run now của routine. Muốn bắt đầu vào một thời điểm cụ thể (ví dụ lúc hạn mức sử dụng được làm mới) thì hẹn một lần chạy cho routine. Ưu tiên hai cách này: chỉ phiên của routine được `resume-run.yml` tự tiếp tục khi hết hạn mức. Phiên mở bằng `claude --cloud` từ terminal dừng hẳn khi hết hạn mức, người phải mở lại (L-044); chỉ dùng cách này khi có lý do, ví dụ dùng cloud session credits (L-044), và ghi lý do vào mục Kiểm tra đầu phiên.
4. Mỗi lúc chỉ có một phiên tự chạy. Trong khi phiên đang chạy, người không merge vào nhánh chính, kể cả PR chỉ có tài liệu: mỗi commit mới trên nhánh chính bắt task đang làm chạy lại CI một vòng. `merge-if-green.sh` từ chối mọi PR không phải của phiên khi danh sách chạy trên nhánh báo cáo đang `running`, kể cả khi phiên đang chờ hạn mức được làm mới (mục 4, Tiếp tục phiên sau khi hết hạn mức).
5. Một danh sách chạy chỉ chạy một lần. Danh sách đã `finished` không được kích hoạt lại; muốn chạy lại thì lập danh sách mới.
6. Trước khi duyệt danh sách dài, dev xem hạn mức GitHub Actions còn lại (Billing and plans của tổ chức; token của phiên không đọc được trang này) và so với số phút CI của danh sách: mỗi task khoảng 10 phút `pr-check` và 10 phút `main-guard`, gấp ba khi task đổi thư viện hay file build (build bản release), cộng khoảng 35 phút `device-session` và `staging` cuối phiên. Hết hạn mức giữa phiên thì mọi workflow, kể cả `resume-run.yml`, đều không chạy (L-094). Ghi kết quả ở dòng "Hạn mức Actions" của mục Kiểm tra trước khi duyệt.
7. Phiên chạy dưới tài khoản Claude và tài khoản GitHub hiện có của người phụ trách (ADR-0009). Vì dùng chung hạn mức, nên tránh kích hoạt phiên khi tài khoản đó vừa dùng nhiều trong ngày.

## 2. Kiểm tra đầu phiên

AI tạo nhánh báo cáo `claude/RUN-YYYYMMDD-NN-report` từ nhánh chính, ghi trạng thái phiên `running` và giờ bắt đầu vào danh sách chạy trên nhánh đó, đẩy nhánh lên, rồi kiểm tra như bảng dưới.

Nhánh báo cáo đã có và danh sách chạy trên đó đang `running` nghĩa là đây là **phiên tiếp tục**: phiên trước dừng giữa chừng, thường vì hết hạn mức, và `resume-run.yml` vừa kích hoạt lại. Không tạo lại nhánh báo cáo; làm theo mục 4, Tiếp tục phiên sau khi hết hạn mức.

Các điều kiện:

| Điều kiện | Không thỏa thì |
| --- | --- |
| Danh sách chạy trên nhánh chính có trạng thái `approved` | Không chạy task nào, kết thúc phiên, báo cáo lý do |
| Môi trường cloud dựng từ `scripts/cloud-setup.sh` mới nhất và dùng được: `scripts/cloud-env-check.sh` qua (phiên bản setup script, Java chạy được Gradle của dự án, `./gradlew --version` khởi động được, SDK có nền tảng `compileSdk`). Chạy trước `scripts/check.sh` | Không chờ, không thử lại: môi trường cũ không tự khỏi, và Java hay Gradle hỏng ở bước này thì `check.sh` cũng hỏng. Không chạy task nào, kết thúc phiên ngay, chép thông báo của script vào báo cáo để người dán lại setup script hoặc sửa biến môi trường |
| Môi trường build được: `scripts/check.sh` qua trên nhánh chính, để lỗi môi trường lộ ra trước khi viết code và để thư viện được tải sẵn. Chạy ở chế độ nền, song song với phần đầu của task đầu tiên (mục Kiểm tra build ở chế độ nền, dưới bảng này) | Lỗi hạ tầng: thử lại theo lịch ở bước 5. Lỗi khác: không chạy task nào, kết thúc phiên, báo cáo lý do |
| Nhánh chính đang xanh: `scripts/main-guard-status.sh` thoát 0 (`main-guard` qua trên commit gần nhất có đổi code của nhánh chính, L-076), và không có issue hay PR nào đang mở mang nhãn `main-broken` | Mã 1 (đỏ: `main-guard` không qua và có issue `main-broken` đang mở): không chạy task nào, kết thúc phiên, báo cáo lý do. Mã 2 (đang chạy): chờ bằng `scripts/wait-run.sh --workflow main-guard.yml --since <giờ bắt đầu mà script in>` (không muộn hơn giờ đó, để không bỏ sót lần chạy đang chạy), rồi gọi lại script. Mã 3 (có code mà chưa có lần chạy, thường vì GitHub không phát sự kiện push sau một lần merge; lần chạy bị hủy; hoặc lần chạy thất bại mà không có issue `main-broken` đang mở, tức lỗi hạ tầng như checkout, Java, mạng, không phải code đỏ): chạy `main-guard` một lần bằng REST, `gh api -X POST repos/{owner}/{repo}/actions/workflows/main-guard.yml/dispatches -f ref=main`, ghi giờ lúc gọi, chờ như mã 2 với `--since` là giờ đó, rồi gọi lại; vẫn không xanh thì kết thúc phiên như mã 1. Mã 4 (không đọc được GitHub): lỗi hạ tầng, thử lại theo bước 5 (sau 1, 2, 4, 8 rồi 15 phút), vẫn lỗi thì kết thúc phiên |
| Không có issue nào đang mở mang nhãn `env-broken` (kiểm tra môi trường hằng ngày, `docs/ops/staging-and-runs.md`, mục 2.4). `start-run.yml` đã kiểm tra; phiên kiểm tra lại vì phiên bắt đầu bằng Run now không qua workflow | Không chạy task nào, kết thúc phiên, báo cáo lý do |
| Không có issue nào đang mở mang nhãn `setup-broken` (kiểm tra thiết lập hằng ngày của `env-canary.yml`: bí mật Firebase staging không phải file giả và có app staging, nhóm người test có trên App Distribution, file R2 bắt buộc, DNS của tên miền file tĩnh). `start-run.yml` chạy chính kiểm tra này (`scripts/setup-check.sh --staging`) trước khi kích hoạt; phiên không chạy lại được vì môi trường cloud không tới được R2 và không có bí mật | Không chạy task nào, kết thúc phiên, báo cáo lý do: thiếu thiết lập thì cuối phiên không phát được bản staging (L-059) |
| Danh sách qua `scripts/run-check.py <danh sách chạy>`, chạy một lần ở đầu phiên: spec đã chốt và trả lời hết câu hỏi, phụ thuộc đúng thứ tự, mọi kịch bản có test ([ops/pre-run-review.md](ops/pre-run-review.md)) | Lỗi về spec, ticket hay phụ thuộc của một task: bỏ qua task đó và các task phụ thuộc nó, chép thông báo vào báo cáo. Lỗi của `plan-check` để dòng `plan-check.py` bên dưới quyết định ngay trước task. Dòng chú ý chép vào báo cáo |
| Plan có trạng thái `approved` và nằm trong danh sách chạy | Bỏ qua task, ghi lý do |
| Ticket có trạng thái `approved` | Bỏ qua task, ghi lý do |
| Plan qua `scripts/plan-check.py <plan>`. Chạy ngay trước khi bắt đầu từng task, không chạy chung ở đầu phiên: plan có thể sửa file mà task trước trong cùng phiên tạo ra | Bỏ qua task trước khi tạo nhánh, chép thông báo của script vào báo cáo; plan và ticket giữ `approved`. Không tự sửa plan |
| Các ticket phụ thuộc đã `on-staging`, hoặc được merge trước đó trong cùng phiên | Bỏ qua task, ghi lý do |

### Kiểm tra build ở chế độ nền

`scripts/check.sh` trên cloud mất khoảng 8 phút vì Gradle bắt đầu với bộ nhớ đệm trống. Phiên không ngồi chờ:

1. Làm các kiểm tra nhanh trong bảng trên trước: danh sách chạy `approved`, `cloud-env-check.sh`, nhánh chính xanh, không có `env-broken` hay `setup-broken`. Không thỏa thì xử lý như bảng, không chạy `check.sh`.
2. Chạy `check.sh` trong một worktree riêng của nhánh chính, ở chế độ nền: `git worktree add ../start-check origin/main`, rồi `(cd ../start-check && scripts/check.sh) > ../start-check.log 2>&1`. Worktree riêng là bắt buộc: thư mục chính sắp đổi sang nhánh của task, và đổi file dưới một bản build đang chạy làm kết quả kiểm tra sai. Thư viện và bộ nhớ đệm build nằm ở `~/.gradle`, dùng chung cho thư mục chính.
3. Trong lúc chờ: chạy `plan-check.py` cho task đầu, làm bước 1 và 2 của task đó (tạo nhánh, đọc tài liệu) và soạn test của bước 3. Chưa chạy Gradle.
4. Trước lần chạy Gradle đầu tiên của task, đợi `check.sh` xong và đọc phần cuối của `../start-check.log`. Qua thì `git worktree remove ../start-check` và đi tiếp. Không qua thì xử lý như dòng "Môi trường build được" của bảng; task đầu chưa đẩy gì lên nên không cần dọn.
5. Môi trường không chạy được lệnh ở chế độ nền thì làm như trước: chạy `check.sh` trên nhánh chính, xong mới bắt đầu task. Ghi điều này vào mục Bài học của báo cáo.

Ghi giờ kiểm tra đầu phiên xong vào bảng Dòng thời gian của báo cáo (mẫu phiên, mục Số liệu).

## 3. Các bước cho từng task

Mỗi task đi qua 10 bước theo thứ tự. Điều kiện dừng ở mục 4 của [risk-scoring.md](risk-scoring.md) áp dụng ở mọi bước.

| Bước | Việc làm | Kết quả |
| --- | --- | --- |
| 1. Tạo nhánh | Phiên chính chạy `scripts/token-usage.py mark T-NNN` khi bắt đầu task, kể cả khi tiếp tục trên nhánh có sẵn (mục Tiếp tục task đã dừng), trước khi tạo hay checkout nhánh, để token của phiên chính cho task tính vào task cả khi thư mục chính ở HEAD tách rời (camera-pose L-083). Tạo nhánh `claude/T-NNN-<tu-khoa>` từ nhánh chính mới nhất. Không dùng nhánh mặc định mà routine tạo cho phiên. Đẩy ngay bằng `scripts/save-branch.sh <nhánh>`, trước khi viết dòng test nào, để lệnh đẩy bị từ chối lộ ra trước khi tốn công viết code | Nhánh riêng của task, đã có trên GitHub |
| 2. Đọc tài liệu | Đọc theo thứ tự trong [CLAUDE.md](CLAUDE.md) | Đủ ngữ cảnh |
| 3. Viết test trước | Viết test theo mục Kiểm thử của plan, chạy và xác nhận test thất bại đúng lý do | Commit chứa test |
| 4. Viết code | Viết code cho đến khi test qua, theo danh sách file của plan; phần ngoài danh sách cần cho task thì sửa tiếp và ghi lại (ADR-0029, `risk-scoring.md` mục 4) | Code chạy được |
| 5. Kiểm tra cục bộ | Build, lint, unit test, screenshot test, quy tắc phụ thuộc module, đối chiếu danh sách file | Sẵn sàng rà soát |
| 6. Rà soát độc lập | Phiên AI khác rà soát thay đổi | Danh sách điểm phát hiện đã xử lý |
| 7. Commit và đẩy | Commit theo quy ước, đẩy nhánh bằng `scripts/save-branch.sh`, mở PR. PR của task chỉ có code, test, ảnh chuẩn và ảnh so sánh thiết kế | PR liên kết tới plan |
| 8. CI đầy đủ | `pr-check.yml` chạy cổng chất lượng, thêm test trên emulator khi PR chạm vào code chạy trong app | CI xanh |
| 9. Merge | Merge bằng `scripts/merge-if-green.sh` | Nhánh chính có thay đổi của task |
| 10. Ghi nhật ký | Khi task có kết quả cuối, ghi vào cuối plan và cập nhật trạng thái trên nhánh báo cáo | Người đọc được kết quả |

### Bước 3. Viết test trước

- Viết toàn bộ test được nêu trong mục Kiểm thử của plan, trước khi viết bất kỳ dòng code nào của chức năng.
- Chạy test và xác nhận test **thất bại đúng lý do**: vì chức năng chưa có, không phải vì test viết sai cú pháp hay lỗi môi trường. Với màn hình hay lớp chưa tồn tại, AI tạo khung rỗng (hàm trả về giá trị mặc định) để test biên dịch được và thất bại ở phần kiểm tra.
- Test nào qua ngay khi chưa có code là test không kiểm tra gì, phải viết lại, trừ test chặn mà plan nêu là qua ngay. Cách mặc định: thêm một bước đối chứng trước khi khẳng định (ghi rồi đọc lại, đặt dữ liệu đúng cạnh dữ liệu sai), rồi chạy lại để thấy test thất bại đúng lý do. Ghi số test được viết chặt như vậy vào nhật ký (L-055).
- Khi test viết trước đã biên dịch được (khung rỗng), chạy test JVM của mọi module, không chỉ module của plan: `./gradlew testDebugUnitTest testStagingDebugUnitTest --continue` (ADR-0029, quyết định 4). Test đỏ ngoài mục 3.2 được xếp ngay theo `risk-scoring.md` mục 4, đoạn "Không phải dừng: phần ngoài mục 3.2": test chỉ đỏ vì dữ liệu chưa có phần plan thêm thì cập nhật dữ liệu đó trong commit test; test đỏ vì hành vi cũ bị hỏng thì ghi lại, bước 4 sửa code cho nó qua.
- Commit test riêng, trước commit code, để lịch sử cho thấy test có trước.
- Đẩy nhánh của task lên ngay sau mỗi commit, ở bước này và bước 4, không đợi tới bước 7, bằng `scripts/save-branch.sh <nhánh>` (thử lại khi lỗi mạng; không đẩy được thì giữ nhánh thành bundle và trả mã 3, xem "Khi không đẩy được" sau bước 7). Hết hạn mức thì phiên dừng ngay, không kịp làm gì thêm; phần chưa đẩy lên là phần mất. Commit trên nhánh cũng là dấu hiệu phiên còn sống cho `resume-run.yml`.
- Ngoại lệ: commit đầu tiên của task sau chỉ đẩy khi đã có kết quả `main-guard` của lần merge trước (bước 9). Trong lúc chờ, commit cục bộ và làm tiếp; có kết quả xanh, không có issue `main-broken`, thì đẩy ngay (L-099).
- Chỉ được bỏ qua bước này với phần thay đổi nằm trong mục ngoại lệ đã được người duyệt đồng ý trong plan.
- Screenshot test cho màn hình mới: mỗi lệnh `captureRoboImage` ghi rõ đường dẫn ảnh, dạng `"src/test/screenshots/<Ten>.png"`. Chạy cục bộ, `scripts/check.sh` tự ghi ảnh chuẩn cho những ảnh còn thiếu và giữ nguyên ảnh đã có. Commit ảnh chuẩn cùng PR (CI báo lỗi cho từng ảnh thiếu) và đánh dấu trong báo cáo để người xem. Ảnh chuẩn của màn hình cũ chỉ được cập nhật khi plan nêu rõ.

### Giao bước 3 đến 5 cho phiên con

Task lớn (nhiều file, cỡ L) thì phiên chính giao bước 3 đến 5 cho một phiên AI con, để giữ ngữ cảnh của phiên chính cho các task sau (L-052).

- Phiên con làm trong worktree riêng của nhánh task: `git worktree add ../task-T-NNN claude/T-NNN-<tu-khoa>`. Thư mục chính giữ sạch, để phiên chính làm việc khác song song: chạy `check.sh` nền, chờ `main-guard`, chuẩn bị task sau, viết báo cáo. Bộ nhớ đệm Gradle ở `~/.gradle` dùng chung nên không tải lại. Thư mục chính ở HEAD tách rời trong lúc này; dấu mốc `mark T-NNN` của bước 1 gán phần phiên chính làm cho task vào task. Phiên chính chuyển sang làm việc của task khác (chuẩn bị bước 1, 2 của task sau trong lúc phiên con làm) thì chạy `mark` cho task đó, rồi `mark` lại task cũ khi quay về; phiên chính khởi động lại thì chạy lại `mark` cho task đang làm. Không giao lệnh `mark` cho phiên con: chỉ dấu mốc trong transcript của phiên chính được tính (camera-pose L-083).
- Nội dung giao việc tối thiểu: một dòng riêng `Task: T-NNN` (để `scripts/token-usage.py` tính token của phiên con vào task, L-071; lời giao rà soát độc lập ở bước 6 cũng có dòng này); đường dẫn plan và worktree; danh sách file của mục 3.2 là phạm vi chính; phần ngoài danh sách cần cho task thì sửa tiếp theo ADR-0029 (`risk-scoring.md` mục 4), phiên con báo mọi file ngoài mục 3.2 kèm lý do; chạy test JVM của mọi module khi test viết trước đã biên dịch được (bước 3); thứ tự commit (test trước, rồi code); phiên con chỉ commit, không đẩy nhánh; lệnh nào bị hệ thống phân quyền từ chối thì dừng ngay và báo, không làm tiếp; điều kiện dừng (`risk-scoring.md` mục 4 và mục 7 của plan); báo cáo theo các dòng của nhật ký plan (Test viết trước, Lệch so với plan); không xóa test cũ nào, kể cả test thêm sau khi plan viết: hành vi cũ bị plan thay thì chỉ bỏ câu khẳng định của hành vi đó theo ADR-0031, phần còn lại giữ (L-095); mọi thay đổi ở file test sau commit test của bước 3 liệt kê riêng trong báo cáo (tên test, vì sao), kể cả thay đổi phiên con coi là chi tiết (L-093). Phần giao việc chung, dùng lại cho mọi task, không thêm việc về một file cụ thể: mọi việc về file lấy từ plan của task (L-060).
- Phiên chính đẩy nhánh task (`scripts/save-branch.sh claude/T-NNN-<tu-khoa>`, worktree dùng chung kho git nên commit của phiên con có sẵn) ngay khi phiên con báo xong mỗi commit hoặc báo dừng, để commit không chỉ nằm trong container. Phiên con chạy nền chỉ báo khi xong, nên phiên chính chạy nền `scripts/push-on-commit.sh claude/T-NNN-<tu-khoa> --minutes <thời gian dự kiến của bước 3 đến 5>`: script đọc nhánh mỗi phút, đẩy khi có commit mới, tự dừng khi hết giờ, trả mã 3 khi không đẩy được (L-090). Bắt đầu sau khi có kết quả `main-guard` của task trước (L-099). Dừng sớm bằng công cụ dừng tác vụ nền của phiên; không tự viết vòng đẩy, không dừng bằng `pkill -f` (L-082, L-096). Lệnh đẩy của phiên con có lúc bị bộ phân loại quyền chặn ("Unrequested Commit in a Connected App"), lệnh của phiên chính thì không, vì việc chạy danh sách là yêu cầu của người (L-070). Phiên chính cũng bị chặn, hay script trả mã 3, thì làm theo "Khi không đẩy được" sau bước 7.
- Phiên con xong: phiên chính kiểm `git diff --name-only origin/main...` khớp mục 3.2 cộng phần ngoài mục 3.2 mà phiên con đã báo, đọc báo cáo, rồi giao rà soát độc lập (bước 6) cho một phiên khác. Xóa worktree sau khi merge (`git worktree remove`); không xóa worktree của nhánh chưa đẩy được.
- Phiên con kết thúc mà không có báo cáo (lỗi API như `oauth_org_not_allowed`, hết giờ, tiến trình khởi động lại; bảng tra lỗi, PB-12): không tính là task dừng. Phiên chính đọc `git status` và `git log origin/main..` của worktree, chạy test của phần đã viết, đối chiếu với mục 4 của plan (test viết trước có thất bại đúng lý do không), rồi tự làm tiếp từ phần đã có hoặc giao một phiên con mới với lời dặn tiếp từ commit và file chưa commit đó. Không viết lại phần đã có. Ghi vào nhật ký plan và mục Tiếp tục của báo cáo (L-109).
- Phiên chính tự làm bước 3 đến 5 của một task (task tiếp tục từ phiên trước, phiên con kết thúc không báo cáo, sửa sau rà soát) thì vẫn có thể làm trong worktree của task: dấu mốc `mark T-NNN` của bước 1 gán phần đó vào task (L-111, camera-pose L-083).
- Không sửa file trong thư mục đang chạy `check.sh` nền: lần chạy đó kiểm một trạng thái lẫn lộn và phải chạy lại.

### Bước 4. Viết code

- Tạo và sửa các file trong danh sách file của plan. Phần ngoài danh sách cần để thay đổi của plan chạy đúng thì sửa tiếp, không dừng task (ADR-0029, `risk-scoring.md` mục 4); vẫn dừng với file trong mục "Không được làm" của `CLAUDE.md` và với điều kiện dừng 2 tới 9.
- Phương án chính không chạy và plan có phương án dự phòng đã duyệt (mục 3.4 của plan): chuyển sang phương án đó theo `risk-scoring.md`, mục 4, thay vì dừng task. Ghi vào nhật ký lúc chuyển, điều kiện chuyển đã gặp và lỗi của phương án chính; báo cáo phiên liệt kê ở mục Cần người kiểm tra.
- Không sửa test đã viết ở bước 3 để làm cho test qua. Nếu test sai ở chi tiết không làm thay đổi điều nó kiểm tra (ví dụ sai tên một hàm), được sửa và ghi lý do vào nhật ký. Nếu cần thay đổi điều test kiểm tra thì dừng task.
- Test cũ ngoài mục 3.2: chỉ cập nhật dữ liệu mà phần plan thêm làm thay đổi, và test vẫn kiểm phần mới như phần cũ; không xóa test hay câu khẳng định, không nới phép so sánh, không thêm `@Ignore` (ADR-0029, quyết định 2). Câu khẳng định cũ trái trực tiếp điều plan đã duyệt nói rõ (mục 3.1, 3.5, câu trả lời ở mục 8): viết lại khi đủ sáu điều của ADR-0031 (`risk-scoring.md` mục 4), kèm câu ý định và lần chạy đối chứng; thiếu một điều thì dừng.

### Bước 5. Kiểm tra cục bộ

- Chạy `scripts/check.sh`. Script gồm quy tắc phụ thuộc module, ktlint, unit test, test giao diện trên JVM, screenshot test, Android Lint và build bản staging. CI dùng đúng script này, nên qua ở bước 5 thì thường cũng qua ở bước 8.
- Những gì `scripts/check.sh` từ chối, để người viết plan và phiên thấy trước (L-050). Dòng có dấu * được `scripts/plan-check.py` dò trong plan (quy tắc 9): lỗi với plan `draft`, chú ý với plan `approved`.

  | `check.sh` từ chối khi | Cách làm đúng |
  | --- | --- |
  | * Một lệnh `captureRoboImage` không ghi đường dẫn `"src/test/screenshots/<Tên>.png"` ngay sau dấu mở ngoặc, trên cùng dòng (không có đường dẫn, đường dẫn khác, tham số có tên, biến, xuống dòng) | Ghi thẳng chuỗi đường dẫn trong lệnh, dạng `captureRoboImage("src/test/screenshots/<Tên>.png")`. Không gói lệnh vào hàm phụ nhận đường dẫn |
  | * Trên CI, thiếu ảnh chuẩn mà test chụp | Chạy `scripts/check.sh` trên máy để ghi ảnh mới, xem ảnh, commit cùng PR. Plan ghi ảnh mới ở mục 3.2 |
  | * Ảnh chuẩn nằm ngoài `<module>/src/test/screenshots/` | Script chỉ tìm ảnh ở thư mục đó, kể cả với test trong `src/testStaging` |
  | Ảnh chuẩn đã có lệch với ảnh vẽ ra (`verifyRoborazzi`) | Chỉ ghi lại ảnh chuẩn khi plan cho phép |
  | Module phụ thuộc sai quy tắc (`checkModuleDependencies`, ADR-0001) | Feature chỉ phụ thuộc core, core chỉ phụ thuộc core |
  | Code sai định dạng (`ktlintCheck`) | Định dạng theo `.editorconfig` |
  | Unit test hay test giao diện trên JVM thất bại (`testDebugUnitTest`, `testStagingDebugUnitTest`) | Sửa code, không sửa test |
  | Android Lint báo lỗi (`:app:lintStagingDebug`) | Sửa code, không tắt quy tắc |
  | Bản staging không build được (`:app:assembleStagingDebug`) | |
  | Test của script thất bại (`scripts/tests/run.sh`); trên CI thiếu `python3` hay `jq` | |
- So sánh danh sách file thay đổi thực tế với danh sách file trong plan. Mỗi file ngoài danh sách có lý do trong nhật ký và mô tả PR (ADR-0029); file trong mục "Không được làm" của `CLAUDE.md` thì dừng task.
- Feature có thiết kế (`docs/features/<feature>/design/screens/`): chạy `scripts/design-compare.sh <feature>` sau `scripts/check.sh`. Kết quả trong `design/review/` để rà soát, không phải cổng chặn (ADR-0020). Ảnh thiết kế nào báo thiếu ảnh chuẩn screenshot là thiếu test, phải thêm theo plan. Ảnh so sánh hay `summary.md` đổi mà ảnh chuẩn của task không đổi, và `design/review/` không có trong mục 3.2: không commit các file đó (`git checkout -- docs/features/<feature>/design/review/`), ghi vào mô tả PR; không tính là file ngoài danh sách (L-110).
- Lỗi tải thư viện hay công cụ (mã 429, 5xx, hết thời gian chờ, mất kết nối; bảng tra lỗi, PB-09) là lỗi hạ tầng, không phải lỗi của code, và không tính vào giới hạn 3 lần sửa. Phiên chạy qua đêm có thời gian, nên chờ lâu thay vì bỏ cuộc sớm: chạy lại sau 1, 2, 4, 8 rồi 15 phút (tổng khoảng 30 phút). Vẫn lỗi thì kết thúc phiên theo mục 4.
- Mỗi lần chạy lại sau lỗi tải, dùng `GRADLE_REFRESH_DEPENDENCIES=true scripts/check.sh`. Gradle ghi nhớ lần tải lỗi và các lần sau báo "Could not find" mà không gọi lại mạng; biến này (đọc trong init script của `scripts/cloud-setup.sh`) buộc Gradle hỏi lại (RUN-20261016-01).
- Môi trường cloud tải mọi thư viện Maven Central qua mirror của Google, nhờ init script do `scripts/cloud-setup.sh` ghi: kho `mavenCentral()`, thư viện mà plugin tải qua plugin portal, và Robolectric. Log còn thấy `repo.maven.apache.org` hay `repo1.maven.org` nghĩa là init script chưa phủ đường tải đó: ghi vào báo cáo kèm dòng log, đề xuất bài học. Phiên không tự thêm init script, không tự đổi kho tải hay biến môi trường để đi vòng: môi trường chặn các lệnh đó (RUN-20261016-01), và cấu hình tải phải nằm ở setup script để mọi phiên giống nhau.

### Bước 6. Rà soát độc lập

Một phiên AI riêng được khởi tạo để rà soát. Phiên này chỉ nhận plan, spec, thiết kế, ADR và phần thay đổi; không nhận quá trình làm việc của phiên viết code, để đánh giá với con mắt mới. Có phần ngoài mục 3.2 (ADR-0029) thì phiên rà soát nhận thêm danh sách file đó, lý do của từng file và `git diff --stat origin/main...HEAD -- <các file đó>`. Phiên rà soát luôn nhận thêm mọi thay đổi ở file test sau commit test của bước 3 (`git diff <commit test>..HEAD -- <các file test>`) và danh sách test bị xóa so với nhánh chính (`git diff --diff-filter=D --name-only origin/main...HEAD`, cộng các hàm `@Test` có trên nhánh chính mà nhánh task không còn); test viết trước bị đổi điều nó kiểm hay test cũ bị xóa là mức chặn (L-093, L-095).

Nội dung rà soát:

- Thay đổi có đáp ứng đủ tiêu chí chấp nhận trong spec không.
- Test có thực sự kiểm tra tiêu chí chấp nhận không, hay chỉ kiểm tra những gì code đang làm.
- Có tuân thủ mục "Việc AI phải tuân thủ" của các ADR không.
- Phần ngoài mục 3.2 (ADR-0029, `risk-scoring.md` mục 4): có thật cần cho thay đổi của plan không; có file trong mục "Không được làm" của `CLAUDE.md`, thư viện, quyền hay điểm chặn 1 tới 6, 8, 9 mà plan chưa nêu không; test cũ có bị xóa, nới phép so sánh hay đổi điều nó kiểm về hành vi đã có không. Vi phạm là mức chặn.
- Câu khẳng định cũ viết lại (ADR-0031): có câu của plan nói điều mới không; câu mới có kiểm đúng ý định đã ghi không; có bị nới không; đối chứng có đỏ khi ý định bị phá không; có quá 3 câu, có chạm ảnh chuẩn, test viết trước hay tiêu chí của spec không. Vi phạm là mức chặn. Phiên rà soát nhận câu cũ, câu mới, câu của plan, câu ý định và lần chạy đối chứng.
- Có cấu hình production, khóa bí mật, mã quảng cáo thật trong thay đổi không.
- Code có theo đúng mẫu đã có trong dự án không.
- Task dịch chữ: nghĩa của từng nhãn đi cùng con số hay phần trăm có đúng chiều không (giá trị lớn nhất nghĩa là gì), kể cả từ lấy lại từ feature khác. Test chữ chỉ kiểm đủ bản dịch và tham số, không kiểm nghĩa (L-100).
- Feature có thiết kế: giao diện có đúng thiết kế không. Xem từng ảnh ghép trong `design/review/` theo mục 7 của mẫu thiết kế; chữ sai, thiếu trạng thái, sai thứ tự hay vai trò màu là mức chặn; lệch nhỏ về khoảng cách là mức ghi chú. Tỷ lệ điểm ảnh khác chỉ để tham khảo.

Kết quả rà soát chia ba mức:

| Mức | Xử lý |
| --- | --- |
| Chặn | Quay lại bước 4. Tính vào giới hạn 3 lần sửa cho cùng một lỗi. |
| Nên sửa | Phải sửa (ADR-0029, quyết định 3) khi đủ ba điều: phần sửa chỉ chạm file của mục 3.2 hay phần ngoài mục 3.2 được phép theo ADR-0029; có test viết trước thất bại đúng lý do; không đổi điều mà test của mục 4 của plan kiểm. Điểm chỉ đòi thêm test cho hành vi đã đúng (test không thể thất bại trước): thêm khi test nằm trong file của mục 3.2 và thất bại khi tạm bỏ dòng code đó (đột biến, như bước đối chứng của L-055), ghi kết quả đột biến vào nhật ký; không thì ghi vào báo cáo (L-108). Điểm nằm ngoài file được phép, hay không viết được test: không sửa, ghi vào báo cáo. Điểm mâu thuẫn với plan, spec hay một test của plan: không sửa, ghi vào báo cáo kèm phương án để người quyết. Sửa xong thì chạy lại bước 5 và giao rà soát độc lập lần nữa cho phần vừa sửa. Giới hạn 3 lần sửa tính riêng cho từng điểm nên sửa (như cho cùng một lỗi), không cộng dồn giữa các điểm; một điểm hết 3 lần thì hoàn tác phần sửa của điểm đó, ghi vào báo cáo, làm tiếp các điểm khác. Mỗi task tối đa 2 vòng rà soát lại vì điểm nên sửa; sau vòng thứ 2, điểm nên sửa còn lại (kể cả điểm mới vòng đó tìm ra) không sửa nữa, ghi vào báo cáo để người quyết. |
| Ghi chú | Ghi vào báo cáo để người xem. |

Phần ngoài mục 3.2 và từng điểm đã sửa theo rà soát được ghi trong mô tả PR, mục "Ngoài mục 3.2" (kèm `git diff --stat`) và "Đã sửa theo rà soát" (ADR-0029). Câu khẳng định cũ viết lại được ghi ở mục "Test cũ viết lại (ADR-0031)": test, câu cũ, câu mới, câu của plan nói điều mới, câu ý định, lần chạy đối chứng.

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
  - Đọc PR hay file của repo khác (ví dụ plan lấy code từ app): repo đó phải được gắn vào phiên ở chế độ chỉ đọc, rồi `gh api repos/<owner>/<repo>/pulls/<số> -H "Accept: application/vnd.github.v3.diff"` hoặc `gh api "repos/<owner>/<repo>/contents/<đường dẫn>?ref=<commit>" -H "Accept: application/vnd.github.raw"`. Không dùng `gh pr diff -R` và không clone: môi trường chặn cả hai, còn repo chưa gắn thì trả 403 (L-063).

### Khi không đẩy được

Lệnh đẩy của phiên chính bị môi trường từ chối (bộ phân loại lệnh của phiên chặn, L-081), hoặc `scripts/save-branch.sh` của phiên chính trả mã 3. Phiên con không đẩy nên không vào mục này: phiên chính đẩy thay như thường lệ (mục Giao bước 3 đến 5 cho phiên con, L-070).

1. Sau khi lệnh đẩy của chính phiên chính bị từ chối, không đẩy lại dưới dạng khác: không đổi tên nhánh, không gọi API để tạo ref, không giao lần đẩy đó cho phiên con hay phiên khác. Bộ phân loại từ chối lời gọi lệnh nên script không thấy được lần từ chối; chỉ phiên thấy.
2. Chạy `scripts/save-branch.sh --bundle-only <nhánh>` (nếu chưa có bundle). Bundle nằm ở `$HOME/rescue/`, ngoài worktree. Phần chưa commit thì commit trước (L-069).
3. Dừng task theo `risk-scoring.md` mục 4. Plan và ticket giữ `approved`, vì không phải lỗi của plan. Nhật ký plan ghi mã commit.
4. Chép đoạn "Nhánh chưa đẩy" mà script in ra, cùng nguyên văn lệnh bị từ chối và thông báo, vào báo cáo phiên.
5. Làm tiếp task sau. Lần đẩy nhánh của task sau (bước 1) cũng bị từ chối thì kết thúc phiên (mục 4), vì mọi task đều cần đẩy.

### Bước 8. CI đầy đủ

- `pr-check.yml` chạy toàn bộ kiểm tra ở bước 5. Test trên emulator chỉ chạy khi PR chạm vào code giao diện hoặc test giao diện (ADR-0010).
- AI chờ CI bằng `scripts/wait-run.sh --pr <số> --sha "$(git rev-parse HEAD)"` sau khi chính phiên đẩy commit lên nhánh của PR, cho đến khi `pr-check` kết thúc (mục Chờ workflow bên dưới), rồi gọi `merge-if-green.sh`.

**Chờ workflow.** Mỗi lần hỏi GitHub trạng thái là một lượt gọi model đọc lại toàn bộ ngữ cảnh của phiên, nên không hỏi lặp lại bằng tay. `scripts/wait-run.sh` tự chờ trong một lệnh và in một dòng kết quả: mã 0 xanh, 1 đỏ (kèm tên job đỏ), 2 hết thời gian chờ thì gọi lại, 4 GitHub không chạy job (`HẠ TẦNG`), 5 PR xung đột với nhánh chính (`XUNG ĐỘT`). Chạy lệnh ở chế độ nền và làm việc khác trong lúc chờ nếu có (mục 4); không có việc khác thì chạy thường. Môi trường giới hạn một lệnh 10 phút thì dùng `--timeout 9` và gọi lại khi nhận mã 2.
- Mã 4 (`HẠ TẦNG`): GitHub không cấp máy cho job, thường vì hết hạn mức Actions; job không chạy bước nào nên không có log. Không sửa code, không chạy lại, không tính vào 3 lần sửa của cùng một lỗi: làm theo mục 4, dòng "GitHub không chạy job" (L-064).
- Mã 5 (`XUNG ĐỘT`): GitHub không chạy `pr-check` cho PR xung đột với nhánh chính, nên chờ thêm cũng không có lần chạy nào. Gộp nhánh chính vào nhánh (`git merge origin/main`), sửa xung đột trong phạm vi file của plan, đẩy, rồi chờ lại; không tính vào 3 lần sửa (L-064).
- `--pr` chờ commit đầu hiện tại của PR, đọc lại mỗi vòng. Lần chạy của commit cũ bị hủy vì có commit mới thì không phải đỏ (L-087). `--sha` để không nhận nhầm kết quả của commit trước khi GitHub chưa cập nhật PR (vài giây sau lệnh đẩy). Sau `merge-if-green.sh --update` (commit do GitHub tạo, phiên không có mã) thì gọi không kèm `--sha`. `ĐỎ (cancelled)` chỉ còn khi chính commit đầu bị hủy: tra bảng tra lỗi như mọi lần đỏ.
- CI đỏ (`wait-run.sh` mã 1) thì tra [bảng tra lỗi](ops/failure-playbook.md) trước khi sửa code (L-079):
  1. Lấy log của job đỏ rồi tra bảng:
     ```bash
     gh run view <id> --log-failed > ci.log
     scripts/failure-kind.sh --playbook ci.log
     ```
     Làm theo dòng script in ra. Không dùng `gh api .../jobs/<job id>/logs`: tùy phiên bản `gh`, lệnh đó đòi cờ `--allow-escape-sequences` hay không biết cờ này, và ghi thông báo lỗi vào `ci.log`; script khi đó in `log-hong` (L-104). Không tải được log (địa chỉ chuyển hướng sang kho lưu trữ khác, proxy có thể chặn như với artifact): chạy `failure-kind.sh --playbook` trên đầu ra của `wait-run.sh`; vẫn không khớp thì xử lý như `khong-co` và ghi "không tải được log job" vào báo cáo.
  2. Loại `chay-lai`, `lich-thu-lai`, `cho`, `gop-nhanh`, `kiem-da-merge` không tính vào 3 lần sửa.
  3. `khong-co` và `that` thì như cũ: lỗi chập chờn chạy lại một lần, lỗi thật quay lại bước 4, tối đa 3 lần cho cùng một lỗi, sau đó dừng task.
  4. Báo cáo phiên ghi mã dòng bảng của mỗi lần chạy lại (mục Số liệu, "Số lần thử lại vì lỗi hạ tầng").
- Bước "Build bản release (R8)" của `build-and-test` đỏ (PR đổi thư viện, file build hay luật R8) là lỗi thật của task: sửa trong luật R8 của module có trong mục 3.2 (`consumer-rules.pro`, `proguard-rules.pro`); file đó không có trong mục 3.2 thì dừng task theo điều kiện 1 (L-083).
- Khi `ui-test` đỏ, log của job có bước "Chi tiết test trên emulator" in toàn văn lỗi của từng test và log của app trên emulator (ADR-0019). Đọc bước đó trước khi kết luận. Bản đầy đủ nằm trong artifact `ui-test-report` của lần chạy: `gh api repos/{owner}/{repo}/actions/runs/<id>/artifacts`, rồi tải tệp zip theo `archive_download_url`.
- Không thêm lệnh in chẩn đoán vào test để tìm lỗi trên CI; đọc chi tiết và log nói trên.

### Bước 9. Merge

- Chỉ merge bằng `scripts/merge-if-green.sh <số PR>`. Script tự kiểm tra mọi điều kiện của cổng chất lượng và từ chối merge nếu thiếu điều kiện nào (ADR-0010). AI không merge bằng bất kỳ cách nào khác.
- Script báo nhánh chưa cập nhật theo nhánh chính (mã 3) thì gọi lại với `--update`: script nhờ GitHub gộp nhánh chính vào nhánh của PR, chờ CI trên commit mới (tối đa 30 phút, chạy ở chế độ nền) rồi merge; hết giờ chờ thì trả mã 2, gọi lại. Mọi điều kiện được kiểm lại sau mỗi lần chờ (L-059).
- Script in `ĐÃ MERGE (GitHub báo lỗi)` thì GitHub trả lỗi nhưng commit squash của PR đã vào nhánh chính (L-080): coi như đã merge, chờ `main-guard` bằng lệnh script in ra; script không yêu cầu được `main-guard` thì ghi vào báo cáo để người chạy tay. Script in `ĐÃ MERGE TRƯỚC ĐÓ` thì không merge lại; ghi vào báo cáo để người đóng PR.
- Script in mã commit sẽ merge và báo chú ý khi commit cuối mới đẩy vài phút trước. Thay đổi sau khi PR đã gửi người duyệt hay đã gọi merge thì làm trên nhánh mới, không đẩy thêm vào nhánh cũ: commit đẩy sau khi merge không vào nhánh chính.
- Script từ chối vì lý do khác thì không tìm cách vượt qua, mà dừng task và ghi lý do vào nhật ký.
- Lệnh của phiên không mở đầu bằng `sleep`: công cụ chặn lệnh đó. Chờ bằng `scripts/wait-run.sh`, hoặc một vòng có giới hạn nằm trong lệnh (`for i in 1 2 3; do ...; sleep 30; done`). Tệp tạm ghi vào `${TMPDIR:-/tmp}`, không ghi `$TMPDIR` trống: trên cloud biến này không được đặt, nên `"$TMPDIR/x"` ghi vào gốc ổ đĩa (L-088).
- Sau khi merge, chờ `main-guard` của commit merge: `scripts/wait-run.sh --workflow main-guard.yml --since <giờ merge>` ở chế độ nền, trong lúc làm bước 10 và bước 1, 2 của task sau. Kết quả đọc từ tệp đầu ra của lệnh nền (hoặc thông báo khi lệnh xong); không tự viết vòng chờ bằng `pgrep -f` hay dọn bằng `pkill -f` theo chuỗi lệnh: `pgrep -f` khớp cả dòng lệnh của chính vòng đó nên chờ mãi, `pkill -f` giết luôn shell của phiên (L-082). Commit merge chỉ đổi tài liệu thì `main-guard` không chạy (`paths-ignore`), không cần chờ. Lần chờ này trả mã 4 thì nhánh chính chưa được kiểm, không phải đỏ: không revert (ADR-0025 chỉ áp dụng khi có issue `main-broken`), làm theo mục 4, dòng "GitHub không chạy job".
- Trước khi đẩy commit đầu tiên của task sau, phải có kết quả của lần chờ đó, rồi kiểm tra lại không có issue hay PR nào đang mở mang nhãn `main-broken`. Kiểm tra ngay sau khi merge là quá sớm: `main-guard` mất khoảng 5 đến 12 phút (gấp đôi khi nó chạy lại test thất bại), và issue chỉ được mở khi nó xong (L-043).
- Có issue `main-broken`: commit gây lỗi (dòng `<!-- culprit:<sha> -->` trong thân issue) là commit merge của một task chính phiên đã merge thì làm theo mục Khi nhánh chính đỏ vì task của phiên; commit khác thì kết thúc phiên (mục 4).
- `main-guard` xanh sau khi chạy lại test thất bại thì có issue mang nhãn `flaky` (ADR-0025): phiên làm tiếp như bình thường, chép issue vào báo cáo.

### Khi nhánh chính đỏ vì task của phiên

Theo ADR-0025, mục 4 (L-049). Phiên chỉ revert commit merge của chính phiên, không bao giờ revert commit của người hay của phiên khác, và không đóng, sửa hay bình luận vào issue `main-broken`.

1. Đọc issue bằng REST (bước 7) và lấy commit gây lỗi. So với các commit merge mà phiên ghi ở dòng thời gian của báo cáo; không khớp thì kết thúc phiên.
2. Mở PR từ nhánh `revert/<7 ký tự đầu>` mà `main-guard` đã đẩy: `gh api -X POST "repos/{owner}/{repo}/pulls" -f head=revert/<sha> -f base=main -f title="Revert T-NNN (main-broken #<số issue>)" -f body="..."`. Không sửa nhánh đó và không gọi `merge-if-green.sh --update` cho PR này: `merge-if-green.sh` chỉ cho qua đúng một commit `git revert` nằm ngay trên nhánh chính mới nhất (`scripts/is-exact-revert.sh`). Không có nhánh `revert/` (issue ghi "Không tự revert được"), hoặc nhánh chính đã có commit mới sau commit gây lỗi khiến bản revert không còn nằm ngay trên nhánh chính: kết thúc phiên.
3. Chờ CI bằng `scripts/wait-run.sh --pr <số> --sha "$(git rev-parse HEAD)"`, rồi merge bằng `scripts/merge-if-green.sh <số>`. Script từ chối thì kết thúc phiên, không tìm cách khác.
4. Chờ `main-guard` của commit revert (`scripts/wait-run.sh --workflow main-guard.yml --since <giờ merge>`). Xanh thì issue tự đóng; kiểm lại không còn issue `main-broken` đang mở rồi mới làm task sau. Còn đỏ thì kết thúc phiên.
5. Task bị revert: ticket chuyển `stopped`, nhật ký plan ghi "đã merge rồi revert" kèm issue và PR revert, chuẩn bị cho người như task dừng (mục Chuẩn bị cho người khi task dừng). Task phụ thuộc task đó bị bỏ qua.

### Bước 10. Ghi nhật ký

- Chỉ ghi khi task đã có kết quả cuối: đã merge, hoặc đã dừng. Không ghi trước khi biết kết quả CI. Khi task có kết quả cuối (merge, dừng, hay bỏ qua sau khi đã tạo nhánh), chạy `scripts/token-usage.py mark chung` nếu không còn task nào đang làm; còn task đang làm (đã tạo nhánh) thì chạy `mark` cho task đó (camera-pose L-083).
- Ghi giờ các khâu của task vào bảng Dòng thời gian của báo cáo (mẫu phiên, mục Số liệu): tạo nhánh, test viết trước xong, đẩy PR, CI xong, merge hoặc dừng. Giờ lấy từ commit, PR và kết quả `wait-run.sh`, ghi tới phút theo UTC.
- Ghi trên nhánh báo cáo, không ghi trên nhánh của task. Ghi vào mục Nhật ký thực hiện ở cuối plan theo mẫu, cập nhật dòng của task trong báo cáo phiên, rồi đẩy nhánh báo cáo lên. Nhờ vậy nếu phiên dừng đột ngột (hết hạn mức), người vẫn đọc được kết quả các task đã xong trên nhánh báo cáo.
- Nhật ký ghi cả phần ngoài mục 3.2 (file, vì sao cần, loại: nối chỗ gọi, dữ liệu test, code, tài liệu) và từng điểm đã sửa theo rà soát, với test của nó (ADR-0029). Task đã merge có phần ngoài mục 3.2 thì báo cáo phiên có một dòng `- [ ]` ở mục Cần người kiểm tra, để người xem sau. Câu khẳng định cũ viết lại (ADR-0031) ghi ở dòng "Lệch so với plan" của nhật ký và cũng có một dòng `- [ ]` như vậy.
- Plan chuyển sang `done`, ticket chuyển sang `on-staging`.
- Task bị dừng: plan và ticket chuyển sang `stopped`, nhánh và PR của task được giữ nguyên, không merge.
- Task bị dừng: chuẩn bị sẵn cho người, để sáng hôm sau người chỉ cần xem và duyệt (mục Chuẩn bị cho người khi task dừng).

Sau đó AI chuyển sang task tiếp theo trong danh sách. Task phụ thuộc vào task bị dừng được bỏ qua.

### Chuẩn bị cho người khi task dừng

Phiên không tự xoay xở khi dừng, nhưng soạn sẵn cách tiếp tục, để một lần dừng chỉ tốn vài phút của người thay vì một vòng làm lại. Phần code đã viết mà chưa qua kiểm tra vẫn được commit lên nhánh task (thông điệp ghi rõ chưa build được hay chưa qua test; nhánh không có PR), không để trong container hay tệp patch tạm, vì container bị thu hồi sau phiên; nội dung giao việc cho phiên con nói rõ điều này (L-069).

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
| GitHub không chạy job (`wait-run.sh` mã 4, L-064) | Đẩy hết những gì đang có: nhánh của task đang làm, nhánh báo cáo với dòng thời gian ghi giờ, tên workflow và mã lần chạy bị bỏ. Giữ danh sách chạy `running`, plan và ticket giữ trạng thái; không bắt đầu task mới. `resume-run.yml` kích hoạt lại phiên khi GitHub chạy lại được (nó cũng chỉ chạy được từ lúc đó) |
| Lệnh đẩy nhánh bị từ chối ở hai task liên tiếp | Đẩy nhánh báo cáo nếu được; báo cáo ghi các bundle (bước 7, "Khi không đẩy được") |
| Nhánh chính đỏ sau một lần merge (xuất hiện issue mang nhãn `main-broken`) | Dừng ngay, không làm thêm task nào. Trừ khi commit gây lỗi là commit merge của một task chính phiên: revert theo mục 3, Khi nhánh chính đỏ vì task của phiên, rồi làm tiếp các task không phụ thuộc task đó (ADR-0025) |

### Tiếp tục phiên sau khi hết hạn mức

Phiên dùng hạn mức của tài khoản Claude (ADR-0009). Hết hạn mức thì phiên dừng giữa chừng, không tự chờ được. Việc chờ do GitHub làm:

1. Mỗi giờ, `resume-run.yml` chạy `scripts/resume-check.py`: tìm danh sách chạy đang `running` trên nhánh báo cáo mà mọi nhánh của phiên (nhánh báo cáo, nhánh task và nhánh `claude/fix-T-NNN-...` của các ticket trong danh sách) không có commit nào trong 90 phút. Phiên còn sống luôn có commit dày hơn thế: đẩy sau mỗi commit của task (bước 3), đẩy nhánh báo cáo sau mỗi task (bước 10) và trong lúc viết báo cáo (bước 2 bên dưới); lần chờ dài nhất (CI, emulator) dưới 40 phút.
2. Tìm thấy thì workflow kích hoạt lại routine với cùng danh sách chạy. Hạn mức chưa được làm mới thì phiên mới không làm được gì, không đẩy gì, và giờ sau workflow thử lại. Hạn mức được làm mới thì phiên tiếp tục.
3. Không tiếp tục khi: phiên bắt đầu hơn 24 giờ trước (người quyết định), nhánh chính đỏ (`main-broken`), môi trường lỗi (`env-broken`).

Phiên tiếp tục làm như sau:

1. Lấy nhánh báo cáo có sẵn, đọc danh sách chạy, dòng thời gian, nhật ký các plan trên nhánh đó. Ghi vào dòng thời gian: giờ tiếp tục, và giờ commit cuối của phiên trước làm giờ dừng.
2. Làm lại các kiểm tra đầu phiên (mục 2), trừ việc tạo nhánh báo cáo. Kiểm tra build vẫn chạy nền như mục 2.
3. Dòng thời gian ghi lần chạy bị GitHub bỏ (mã 4): chạy lại từng lần một lần, `gh api -X POST "repos/{owner}/{repo}/actions/runs/<id>/rerun-failed-jobs"`, rồi chờ bằng `wait-run.sh`. `main-guard` của commit merge của phiên phải xanh, và không có issue `main-broken`, trước khi làm task tiếp theo. Lại mã 4 thì kết thúc phiên như lần trước. Lệnh chạy lại bị proxy từ chối (403): lần chạy `pr-check` thì đẩy một commit rỗng lên nhánh task (`git commit --allow-empty`); lần chạy `main-guard` thì kết thúc phiên, báo cáo ghi lần chạy cần người chạy lại (L-064).
4. Task đã có nhật ký (đã merge, đã dừng, đã bỏ qua) thì không làm lại.
5. Task đang làm dở: tiếp tục trên nhánh của nó. Chạy `scripts/token-usage.py mark T-NNN` cho task đó trước khi làm tiếp: môi trường mới không có transcript của phiên trước nên không có dấu mốc của nó (camera-pose L-083). Đọc các commit đã có để biết đã tới bước nào. PR đã merge mà chưa có nhật ký thì chỉ ghi nhật ký. PR đang mở thì xem CI rồi đi tiếp từ bước 8. Chưa có commit test thì làm lại task từ bước 3 trên nhánh đó.
6. Các task còn lại làm như thường. Gần tới `staging_needed_by` thì áp dụng điều kiện kết thúc như thường.
7. Báo cáo phiên ghi mục Tiếp tục: số lần dừng vì hết hạn mức, giờ dừng, giờ tiếp tục, thời gian chờ.

Muốn dừng hẳn một phiên đang chờ (ví dụ để merge việc gấp): người sửa trạng thái danh sách chạy trên nhánh báo cáo sang `finished`, ghi lý do "người dừng" vào báo cáo, rồi lập danh sách chạy mới cho các task chưa xong. Hoặc tắt workflow `resume-run`.

Sau đó AI:

0. Chuyển thư mục chính khỏi nhánh task vừa merge, trước mọi việc cuối phiên: `git checkout --detach origin/main` và `scripts/token-usage.py mark chung` (camera-pose L-083) (thư mục chính đã sạch vì phiên con làm trong worktree). `scripts/token-usage.py` gán request của phiên chính theo nhánh của thư mục chính; để trên nhánh task thì token của `device-session`, staging và báo cáo tính vào task đó (L-103).
1. Nếu trong phiên có ít nhất một task được merge, chạy kịch bản chuẩn trên emulator trước khi phát (`docs/ops/test-sessions.md`, mục 4; `docs/ops/staging-and-runs.md`, mục 2.5):
   - Ghi giờ hiện tại (UTC), khởi động `device-session.yml` trên nhánh chính (`reason` là mã phiên), rồi chờ bằng `scripts/wait-run.sh --workflow device-session.yml --since <giờ vừa ghi>` ở chế độ nền (khoảng 12 phút). **Trong lúc chờ, làm bước 2.** Xong thì tải artifact `device-session`.
   - Không tải được artifact (thường do mạng của môi trường chặn nơi lưu artifact, L-030): đọc bản tóm tắt trong cuối log của job, ghi vào báo cáo "chưa phân tích được" kèm lỗi tải, và vẫn quyết định phát hay không theo crash và ANR của bản tóm tắt. Người tải artifact và nhờ AI phân tích sau, trong phiên có người theo dõi.
   - Phân tích thư mục phiên theo `docs/CLAUDE.md`, mục Khi phân tích một phiên thử, nhưng ghi kết quả vào mục Kiểm thử trên máy ảo của báo cáo phiên thay vì một file `docs/test-sessions/` riêng. Phản hồi `FB-...` soạn sẵn đi qua PR báo cáo.
   - Đọc bảng "Kịch bản theo plan" của `summary.md` (`docs/ops/test-sessions.md`, mục 5). Dòng `lỗi` là một phát hiện: ghi bước lỗi, mở `fail.png`, `fail.xml` và đoạn log sau dấu `scenario=plan file=<tệp>` làm bằng chứng, soạn phản hồi `FB-...` mức `nang` (`docs/templates/feedback.md`; app bị đóng hay ANR thì `chan`), và ghi vào "Cần người kiểm tra". Dòng `bỏ qua (môi trường)` ghi vào báo cáo, không soạn phản hồi; dòng kiểm thủ công tương ứng vẫn chờ người. Dòng `lỗi tệp`, `bỏ qua (hết thời gian)` và dòng `plan runner_error` là lỗi của kịch bản hay của trình chạy: ghi vào báo cáo. Tệp có dòng `RETRY` (app bị hệ thống giết theo tiến trình khác, camera-pose T-054): kết quả là của lần chạy lại; `fail-1.png`, `fail-1.xml` là của lần đầu, không phải phát hiện. Tệp đạt ở lần chạy lại thì ghi lần chạy lại vào báo cáo, không soạn phản hồi.
   - Tiêu chí về bố cục (vị trí, số cột, giao diện tối) chỉ ghi là đạt sau khi phiên mở ảnh `shot` của bước đó và đối chiếu với tiêu chí, hoặc khi kịch bản có lệnh `count` hay `expect` kiểm đúng điều đó.
   - Kịch bản theo plan lỗi không chặn phát staging; crash và ANR thì vẫn chặn. ANR chỉ trong `AdActivity` của SDK quảng cáo là mức `xem` (dòng riêng của `summary.md`, FB-20261025-02): phát staging, ghi số lần vào mục Kiểm thử trên máy ảo của báo cáo.
   - **Có crash hoặc ANR** (workflow đỏ, `summary.md` ghi "chặn"): không khởi động `staging.yml`. Ghi vào báo cáo dấu vết lỗi và commit nghi gây ra, soạn phản hồi mức `chan`. Nhánh chính vẫn xanh nên không mở `main-broken`; người quyết định sửa trước hay phát.
   - Workflow đỏ mà không có `summary.md` (lỗi hạ tầng của emulator): chạy lại một lần; vẫn lỗi thì ghi vào báo cáo và vẫn phát staging, vì cổng chất lượng đã qua.
   - Không có crash hay ANR: ghi giờ, khởi động `staging.yml` để build bản staging từ nhánh chính và phát cho nhóm qua Firebase App Distribution, rồi chờ bằng `scripts/wait-run.sh --workflow staging.yml --since <giờ vừa ghi>` ở chế độ nền (khoảng 7 phút). Trong lúc chờ, viết mục Kiểm thử trên máy ảo của báo cáo.
2. Viết báo cáo phiên vào file danh sách chạy, **trong lúc chờ các workflow ở bước 1** chứ không chờ xong mới viết. Không có task nào merge thì viết ngay. Thứ tự:
   - Cập nhật nhánh báo cáo theo nhánh chính.
   - Viết các mục không phụ thuộc emulator và staging: tóm tắt, kết quả từng task, cần người kiểm tra, task bị dừng, thời gian, số liệu và dòng thời gian, token, bài học, kiểm chứng bài học cũ, đề xuất.
   - Hàng chờ plan (L-084): `plan_queue` của danh sách chạy khác `0` thì chạy `git fetch origin` và `scripts/plan-queue.py --max <plan_queue>`. Ghi hàng chờ và danh sách ticket bị bỏ qua vào mục "Plan soạn sẵn và câu hỏi cho người". Bước 5 sẽ không chạy thì ghi "Không soạn" kèm lý do: `plan_queue: 0`, hàng chờ rỗng, phiên kết thúc vì lý do khác hết danh sách, phiên quá 24 giờ, `staging_needed_by`. Mã 2 của script cũng ghi vào đây, kèm thông báo.
   - Khi workflow xong: điền mục Kiểm thử trên máy ảo, Bản staging và các dòng còn lại của Dòng thời gian, sửa phần tóm tắt nếu kết quả làm nó đổi.
   - Chuyển trạng thái phiên sang `finished`, ghi giờ kết thúc.
   - Commit và đẩy nhánh báo cáo sau mỗi phần viết xong, không đợi cuối: hết hạn mức giữa chừng thì phần đã viết còn lại, và commit cho `resume-run.yml` biết phiên còn sống.

   Đo token khi viết mục Token: `scripts/token-usage.py report --since <started_at> --run RUN-YYYYMMDD-NN --csv docs/metrics/token-usage.csv`, dán bảng vào mục Token (`docs/ops/token-usage.md`). Không đo được thì ghi lý do và đi tiếp; việc đo không bao giờ chặn phiên.
3. Mở PR báo cáo, tiêu đề `RUN-YYYYMMDD-NN: báo cáo phiên`. PR này chỉ có tài liệu: danh sách chạy, nhật ký và trạng thái của plan, trạng thái ticket, `docs/metrics/token-usage.csv`, bài học mới trong `docs/lessons.md` (mục 6), plan phiên bản mới và danh sách chạy tiếp theo ở trạng thái `draft` (mục 3, Chuẩn bị cho người khi task dừng).
4. Merge PR báo cáo bằng `scripts/merge-if-green.sh <số PR> --update`. PR báo cáo chỉ có tài liệu vẫn có CI khoảng 2 phút: job `build-and-test` chạy `scripts/tests/run.sh`, bộ test này kiểm cả tài liệu (số bài học, `foundation.md`, quét mã thật, danh sách chạy) (L-040); `--update` để script chờ kiểm tra đang chạy. CI đỏ thì đọc lỗi bằng `scripts/wait-run.sh --pr <số PR>` (dòng `FAIL - ` nói test nào), sửa tài liệu trên nhánh báo cáo, đẩy lại rồi gọi lại script. Script từ chối vì lý do khác (ví dụ nhánh chính đang đỏ) thì để PR mở và ghi lý do vào phần mô tả PR; người merge sau.
5. **Soạn plan cho hàng chờ (L-084).** Chỉ sau khi PR báo cáo đã merge, nên báo cáo không bị chậm và hết hạn mức giữa chừng không làm mất báo cáo. Danh sách chạy đã `finished`, bước này không giữ nhánh chính.

   Điều kiện chạy:
   - PR báo cáo đã merge, và hàng chờ ở bước 2 không rỗng;
   - phiên kết thúc vì hết danh sách hay đạt số task tối đa. Không chạy khi phiên kết thúc vì nhánh chính đỏ, lỗi hạ tầng, `env-broken` hay lệnh đẩy bị từ chối;
   - phiên bắt đầu chưa quá 24 giờ, như giới hạn của `resume-run.yml`;
   - không gần `staging_needed_by`: có `staging_needed_by` mà bản staging của phiên chưa phát xong thì không bắt đầu.

   Ghi giờ bắt đầu. Với từng ticket của hàng chờ, theo thứ tự:
   1. Tạo nhánh `claude/plan-T-NNN-<từ khóa>` từ nhánh chính mới nhất.
   2. Viết plan theo `docs/CLAUDE.md`, mục Khi viết plan. Chuyển ticket sang `planned`, điền `plan:`.
   3. Commit, rồi đẩy ngay bằng `scripts/save-branch.sh claude/plan-T-NNN-<từ khóa>`. Script trả mã 3, hay lệnh đẩy bị từ chối, thì làm theo "Khi không đẩy được" (mục 3, sau bước 7; L-081): không đẩy lại dưới dạng khác, giữ bundle, chép đoạn "Nhánh chưa đẩy" vào mục "Plan soạn sẵn và câu hỏi cho người", rồi dừng bước 5. Đẩy được thì mở PR nháp `Plan T-NNN (draft): <tên>` ngay sau commit đầu và gắn nhãn `needs-human` (lệnh `gh api` như PR nháp sửa nguyên nhân, mục 3, bước 7), để hết hạn mức giữa chừng người vẫn thấy phần đã soạn.
   4. Làm mọi bước chạy thử của mục Khi viết plan, mỗi bước trong một worktree riêng. Bước không chạy được, hay chưa chạy xong khi hết giờ, thì ghi `Chạy thử: chưa chạy xong (<lý do>)` ở mục 4.1 và đặt câu hỏi cho người ở mục 8. Không ghi kết quả mà phiên chưa thấy.
   5. Chạy `scripts/plan-check.py`, sửa tới khi không lỗi. Lỗi chỉ người gỡ được thì giữ lại và ghi vào mục 8.
   6. Ngay sau đó, giao một phiên rà soát độc lập (ngữ cảnh mới, chỉ đọc) cho plan này, như rà soát trước khi duyệt (`docs/ops/pre-run-review.md`). Sửa plan theo điểm chặn và điểm nên sửa một lần; điểm còn lại chép vào mục 8.
   7. Commit, đẩy bằng `scripts/save-branch.sh`, cập nhật mô tả PR (kết quả `plan-check.py`, rà soát, chạy thử). PR vẫn là nháp và giữ nhãn `needs-human`, như PR sửa nguyên nhân ở mục Chuẩn bị cho người khi task dừng. Người duyệt plan, gỡ nhãn, chuyển PR sang sẵn sàng rồi merge.

   Giao việc cho phiên con (chạy thử, rà soát độc lập) theo mục Giao bước 3 đến 5 cho phiên con, nhưng lời giao **không** có dòng `Task: T-NNN`: token soạn plan vào dòng Chung, như phần của phiên chính trên nhánh `claude/plan-T-NNN-...`; có dòng đó thì `scripts/token-usage.py` ghi một dòng chỉ có token soạn plan vào CSV, và `estimate` lấy nó làm mẫu ước tính của task (L-102). Phiên con chỉ commit trong worktree, phiên chính đẩy nhánh (L-070).

   Giới hạn thời gian, tính riêng cho từng plan:
   - Mỗi plan có 45 phút, kể cả chạy thử (`scripts/check.sh`, và `./gradlew :app:assembleStagingRelease` khi plan thêm thư viện) và rà soát. Cả bước 5 có tối đa `plan_queue` × 45 phút, cộng 15 phút cho PR tóm tắt.
   - Plan dùng hết 45 phút thì dừng: đẩy phần đã có. Chạy thử chưa xong thì ghi như ý 4 ở trên; plan chưa kịp rà soát ghi "chưa rà soát" ở mục 8 và trong mô tả PR. PR vẫn là nháp.
   - Phần thời gian còn lại của cả bước không đủ 45 phút thì không bắt đầu plan mới.

   Giới hạn token: trước mỗi plan, xem tổng token của phiên bằng `scripts/token-usage.py report --since <started_at>`. Token đã dùng cộng số token của plan trước trong bước này (plan đầu thì lấy task nhỏ nhất của phiên) vượt phần hạn mức còn lại thì dừng bước 5 và ghi lý do. Không đo được thì chỉ soạn một plan.

   Kết thúc bước 5:
   - Mở một PR tóm tắt nhỏ `RUN-YYYYMMDD-NN: plan soạn sẵn` (nhánh `claude/RUN-YYYYMMDD-NN-plans`). PR chỉ sửa mục "Plan soạn sẵn và câu hỏi cho người" của danh sách chạy: bảng các PR plan và câu hỏi cho người.
   - Merge PR tóm tắt bằng `scripts/merge-if-green.sh` như PR báo cáo. Các PR plan là nháp có nhãn `needs-human`, để người duyệt và merge.

   Việc phiên không làm ở bước này:
   - không chuyển PR plan sang sẵn sàng, không gỡ nhãn `needs-human`, không duyệt, không merge PR plan, không ghi `approved` hay mục 9;
   - không lập danh sách chạy mới, ngoài danh sách nháp của mục Chuẩn bị cho người khi task dừng;
   - không sửa plan đã `approved`, spec, hay ticket ngoài trạng thái và trường `plan:` của ticket được chọn.

## 5. Báo cáo phiên

Báo cáo phiên là thứ dev đọc đầu tiên khi bắt đầu làm việc lại. Báo cáo gồm:

- Nhánh chưa đẩy, ngay dưới Tóm tắt khi có bundle: chép đoạn `scripts/save-branch.sh` in ra.
- Kết quả từng task: đã merge, bị dừng, bị bỏ qua, kèm lý do.
- Những gì người cần tự kiểm tra: ảnh chuẩn screenshot mới, tiêu chí kiểm tra thủ công máy thật và người, kịch bản emulator lỗi hay bị bỏ qua, điểm rà soát mức ghi chú.
- Task bị dừng: dừng ở bước nào, vì sao, đề xuất hướng xử lý.
- Thông tin bản staging: phiên bản, kênh phát.
- Thời gian chạy và mức sử dụng của phiên, token của từng task so với ước tính trong plan (`docs/ops/token-usage.md`).
- Số liệu của phiên (mẫu danh sách chạy, mục Số liệu).
- Bài học của phiên, và kết quả kiểm chứng các bài học `applied` liên quan tới phiên này (mục 6).
- Issue mang nhãn `flaky` đang mở (số, tên test) và task bị revert trong phiên, nếu có (ADR-0025).
- Plan soạn sẵn và câu hỏi cho người: hàng chờ và ticket bị bỏ qua (mục 4, bước 2), các PR plan và câu hỏi (bước 5).

## 6. Bài học và cải tiến quy trình

Mỗi phiên là một lần học. Bài học được ghi trong phiên, quyết định ở retro, làm thành PR cải tiến, rồi kiểm chứng ở phiên sau. Sổ bài học là [lessons.md](lessons.md).

**Trong phiên, AI ghi bài học** vào mục Bài học của báo cáo phiên khi gặp một trong các tình huống sau, kể cả khi task thành công:

- Task dừng, hoặc phiên kết thúc sớm.
- Lỗi hạ tầng, kể cả khi thử lại thì qua.
- Phải đoán vì tài liệu, plan hay mẫu không nói rõ, hoặc tài liệu nói sai so với thực tế.
- Một bước tốn thời gian bất thường so với mục Số liệu của các phiên trước.
- Một cách làm hiệu quả đáng ghi thành hướng dẫn cho lần sau.

Bài học loại `môi trường` có thông báo lỗi dò được thì PR cải tiến thêm một dòng vào [bảng tra lỗi](ops/failure-playbook.md), kèm tệp log thật trong `scripts/tests/playbook/`.

Mỗi bài học ghi: chuyện gì đã xảy ra, bằng chứng (log, PR, thời gian), đề xuất sửa tài liệu nào và sửa gì, loại và phạm vi theo [lessons.md](lessons.md). PR báo cáo thêm các bài học này vào sổ với trạng thái `proposed`.

**Số bài học:** bài học thêm ngoài phiên (phiên có người theo dõi, PR cải tiến) có thể trùng số với bài học mà phiên thêm cùng lúc. Trước khi merge PR thêm bài học, gộp nhánh chính mới nhất và đổi số bài học của PR (cùng mọi chỗ nhắc tới nó) thành số tiếp theo của sổ. `scripts/status.py` báo lỗi khi một số dùng hai lần (L-056).

**Phiên kiểm chứng bài học cũ:** đọc các bài học `applied` trong sổ; bài học nào liên quan tới việc phiên vừa làm thì ghi vào báo cáo cải tiến đó có hiệu quả hay không, kèm bằng chứng, và cập nhật dòng **Kiểm chứng** trong sổ.

**Phiên không tự sửa quy trình.** Trong phiên tự chạy, AI không sửa `CLAUDE.md`, `docs/CLAUDE.md`, `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/templates/`, `scripts/` hay `.github/`, dù thấy chỗ cần sửa, trừ khi plan đã duyệt nêu rõ. AI chỉ đề xuất qua bài học. Như vậy các hàng rào an toàn không bị nới lỏng khi không có người theo dõi. Plan `draft` soạn ở mục 4, bước 5 không phải tài liệu quy trình.

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
- Vì repo dùng gói GitHub Free, GitHub không chặn merge ở mức hệ thống. Lớp bảo vệ là script merge và workflow `main-guard.yml` (ADR-0010, ADR-0025).
- Mọi việc phiên làm đều để lại dấu vết trong nhánh, PR, nhật ký plan và báo cáo phiên.

## 8. Những điểm sẽ hiệu chỉnh

Các con số dưới đây được đặt tạm và sẽ hiệu chỉnh qua retro (mục 6) dựa trên mục Số liệu của các phiên:

- Số task tối đa mỗi phiên.
- Giới hạn 3 lần sửa cho cùng một lỗi.
- Thời gian dự trù để build và phát bản staging cuối phiên.
- Lịch thử lại khi lỗi hạ tầng (bước 5).
