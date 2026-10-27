# HƯỚNG DẪN AI LÀM VIỆC VỚI THƯ MỤC DOCS

Tài liệu này dành cho AI. Nó nói rõ AI đọc gì, viết gì và không được làm gì với các tài liệu trong `docs/`. Tổng quan quy trình nằm ở [README.md](README.md), quy trình phiên tự chạy ở [auto-run.md](auto-run.md), thang điểm rủi ro ở [risk-scoring.md](risk-scoring.md).

Mã bài học `L-NNN` trong file này và trong `CLAUDE.md` là của sổ bài học của app ([lessons.md](lessons.md)); mã ghi "của mẫu" là của sổ bài học dự án mẫu. Các file lấy nguyên từ dự án mẫu (`docs/auto-run.md`, `docs/ops/`, `docs/templates/`, `scripts/`) dẫn sổ của dự án mẫu, trừ chỗ ghi "camera-pose" hay phần app áp lại.

## Đọc trước khi làm

Trước khi viết spec, plan hay code cho một ticket, đọc theo thứ tự:

1. Ticket trong `features/<feature>/tickets/`.
2. `features/<feature>/spec.md`, kể cả mục câu hỏi đã được trả lời.
3. Thiết kế trong `features/<feature>/design/`, nếu có.
4. [foundation.md](foundation.md): quy tắc của mọi ADR còn hiệu lực, sinh từ mục "Việc AI phải tuân thủ". Mở ADR đầy đủ trong `adr/` khi task đụng vào vùng của ADR đó hoặc định đổi quy tắc của nó.
5. Phản hồi trong `features/<feature>/feedback/` có trạng thái `triaged` hoặc `planned`, và phản hồi trong `inbox/` được liên kết tới ticket.
6. Các plan trước đó của cùng feature, nhất là mục nhật ký thực hiện, để biết những gì đã làm và đã gặp trở ngại.

Nếu tài liệu mâu thuẫn nhau, không tự chọn bên nào. Ghi mâu thuẫn vào mục câu hỏi của tài liệu đang viết và báo cho người.

## Khi khảo sát app mẫu (app clone)

Chỉ trong phiên có người theo dõi, theo [ops/clone-study.md](ops/clone-study.md) và ADR-0021.

- Điều khiển máy chỉ qua `scripts/app-explore/ax.sh`. Không gọi thẳng `adb shell input`, không vượt bộ chặn quảng cáo và nút mua, không đổi cài đặt hệ thống. Máy khóa, mất mạng hay từ chối thao tác thì dừng và nhờ người.
- Không bấm quảng cáo, không mua, không bắt đầu dùng thử, không đăng nhập bằng tài khoản cá nhân, không chia sẻ hay gửi gì. Mọi dòng mới trong `incidents.log` ghi vào báo cáo của app.
- Mỗi app một agent con, lần lượt vì chỉ có một máy; đọc APK và tìm trên web chạy song song. Các app cùng một mã nguồn chỉ khảo sát kỹ một app.
- Báo cáo theo [mẫu khảo sát](templates/reference-app.md); mỗi nhận xét dẫn về ảnh hay cây giao diện; chưa xem được thì ghi ở mục Chưa xem được, không đoán. Số liệu từ web có nguồn.
- Bản đề xuất theo [mẫu đề xuất](templates/app-proposal.md). Mỗi cải tiến có bằng chứng và cách đo. Quảng cáo, sự kiện phân tích, dữ liệu thu thập chỉ ghi thành câu hỏi. Đề xuất mới có trạng thái `draft`; chỉ dev ghi `approved`, chốt tên và tên gói.
- Ảnh chụp, APK, kết quả giải nén không vào git. Báo cáo và bản đề xuất nằm trong `research/` của thư mục làm việc, cùng cấu trúc với `docs/research/`, và được chép nguyên vào đó khi app đã có repo.
- Báo cáo không chứa mã quảng cáo, khóa API, token, mã định danh máy hay tên tài khoản của app mẫu; trích log thì che. Chạy `scripts/secret-scan.py` trước khi commit.
- Cuối mỗi app, `ax.sh newfiles` liệt kê file mới trên máy; ghi vào báo cáo để người dọn, không tự xóa.
- Không gọi API hay backend của app mẫu từ app mình, không chép code từ APK, không dùng tên, icon app hay ảnh store giống app mẫu.

## Khi thiết kế trên Figma (app clone)

Theo [ops/design-workflow.md](ops/design-workflow.md), mục 8.

- Chưa có bản đề xuất `approved` thì chưa dựng thiết kế của app.
- Dựng 2 đến 3 hướng phong cách trước, mỗi hướng một bảng phong cách và hai màn hình; chờ dev chọn rồi mới dựng toàn bộ. Không tự chọn hướng.
- App mới phải tốt hơn app mẫu và có nhận diện riêng: giữ khuôn tương tác quen thuộc, đổi màu, icon, chữ, hình dạng. Không đặt ảnh chụp toàn màn hình của app mẫu vào thiết kế.
- Mọi màu, chữ, khoảng cách dùng biến; phần lặp lại là component. Tên frame và khung theo ADR-0020.
- Sau mỗi lượt dev làm mịn, quét file chỉ đọc và báo chỗ lệch (instance bị tách, giá trị viết cứng, tên hay khung frame bị đổi, chữ ngoài hệ thống, màn hình còn thiếu). Không tự sửa lại phần dev vừa làm mịn; đề xuất đưa giá trị mới vào biến.
- Tài sản mượn tạm đặt tên `borrowed_...` trong Figma và trong code, ghi vào `docs/borrowed-assets.md` trong cùng PR với file mượn ([mẫu](templates/borrowed-assets.md)).

## Khi sinh spec từ yêu cầu

- Feature mới thì tạo thư mục `features/<feature>/` với tên trùng tên module `:feature:<ten>` dự kiến. Đặt tên chưa chắc thì đề xuất tên trong mục câu hỏi.
- Yêu cầu nằm trong `inbox/` được chuyển vào `features/<feature>/requests/` khi đã xác định feature.
- Khi spec đã có, đổi `status` của yêu cầu từ `new` sang `specified`. Chỉ người ghi `done` hay `dropped`.
- Dùng đúng [mẫu spec](templates/spec.md), giữ nguyên các mục, mục nào không áp dụng thì ghi "Không có" thay vì xóa.
- Viết bằng ngôn ngữ nghiệp vụ. Mục tiêu, phạm vi và kịch bản sử dụng không chứa tên lớp, tên thư viện hay thuật ngữ kỹ thuật.
- Mỗi kịch bản có tiêu chí chấp nhận cụ thể, kiểm chứng được, viết theo dạng "Khi ... thì ...".
- Không tự thêm vị trí quảng cáo, sự kiện phân tích hay dữ liệu thu thập. Chỉ ghi những gì yêu cầu nêu; nếu thấy cần, đặt thành câu hỏi.
- Ghi quyền của app và dịch vụ bên ngoài mà feature cần ở mục 6 của spec. Hành vi trong yêu cầu mà trên một số phiên bản Android cần quyền (ví dụ lưu ảnh vào thư viện máy trên Android 9 trở xuống) thì nêu ngay ở spec, hoặc đặt thành câu hỏi kèm phương án, không để tới lúc viết plan hay chạy phiên mới thấy.
- Chỗ nào yêu cầu chưa rõ, ghi thành câu hỏi trong mục câu hỏi còn mở, kèm phương án AI đề xuất. Không đoán.
- Spec mới luôn có trạng thái `draft`. Spec đã `confirmed` mà có yêu cầu mới thì cập nhật nội dung, chuyển sang `changed` và ghi vào mục lịch sử. Chỉ người mới chuyển sang `confirmed`.

## Khi tách spec thành ticket

- Mỗi ticket là một phần giao được, kiểm chứng được, chạy xong trong một lượt của phiên tự chạy. Ticket quá lớn thì tách tiếp.
- Mã ticket lấy số tiếp theo của cả app: tìm mã lớn nhất trong mọi thư mục `features/*/tickets/` rồi cộng một.
- Ghi rõ ticket xử lý kịch bản nào của spec và phụ thuộc ticket nào.
- Chấm rủi ro sơ bộ và ước lượng cỡ (S, M, L) theo [mẫu ticket](templates/ticket.md).

## Khi viết plan

- Dùng đúng [mẫu plan](templates/plan.md).
- Danh sách file sẽ tạo và sửa phải đầy đủ và cụ thể. Danh sách này là ranh giới: trong phiên tự chạy, AI chỉ được chạm vào những file đã liệt kê.
- Plan đổi một file mà nơi khác đọc (`docs/design-system/tokens.json`, ảnh chuẩn, cấu hình dùng chung): tìm mọi nơi đọc file đó (`grep -rn <tên file> scripts/ .github/ */src/test`) và đưa các file bị ảnh hưởng vào danh sách, hoặc ghi vì sao không ảnh hưởng (L-020 của mẫu).
- Plan sửa hay xóa code Kotlin (ngoài thư mục test): viết mục 3.5 "Hành vi đã có bị đổi". Chạy `scripts/existing-tests.py <plan>`, đọc từng test ở nhóm "Cần xem trước" và "Nên xem", ghi mỗi hành vi plan đổi kèm test có sẵn khẳng định nó, và câu khẳng định giữ nguyên hay câu mới viết sẵn; chỗ chưa quyết thì đặt câu hỏi ở mục 8. Không cần chạy test. `scripts/plan-check.py` (quy tắc 13) báo lỗi khi plan `draft` thiếu mục này (ADR-0031, L-106 của mẫu).
- Mỗi tiêu chí chấp nhận trong spec phải có cách kiểm chứng tương ứng trong mục kiểm thử. Test được viết trước code, nên mục kiểm thử phải đủ cụ thể để viết được test ngay từ plan.
- Plan sửa lỗi giao diện hay lỗi đo được: trước khi viết mục kiểm thử, chạy thử điều test sẽ kiểm tra trên code hiện tại, bằng một test tạm không commit, và ghi số đo thật vào mục 4. Khung test phải tái hiện được lỗi gốc (cùng chiều rộng, chiều cao như nơi phát hiện). Không đoán test sẽ thất bại vì đâu (L-029 của mẫu).
- Kỹ thuật hay thư viện lần đầu dùng trong dự án (tiêu chí E từ 2): chạy thử trước khi duyệt, bằng một test tạm không commit và bằng đúng lệnh phiên dùng (`scripts/check.sh`) trên nhánh có code liên quan; ghi lệnh và kết quả vào mục 4.1 của plan. Chạy thử bằng lệnh khác (ví dụ chỉ `./gradlew :module:testDebugUnitTest`) không đủ: quy tắc của `check.sh` và cách Robolectric vẽ có thể khác (L-001, L-010). `scripts/plan-check.py` báo lỗi khi plan `draft` thiếu mục này.
- Quyết quyền và thư viện ngay trong plan, ghi ở hai dòng cuối của mục 3.3, vì phiên gặp quyền hay thư viện mà plan chưa nêu thì phải dừng task (`risk-scoring.md`, mục 4, điều kiện 2). Cách tìm:
  - Với mỗi API Android mà plan gọi, tra quyền nó cần trên mọi phiên bản từ `minSdk` tới `targetSdk`, kể cả quyền chỉ cần ở bản cũ (ghi `maxSdkVersion`).
  - Với mỗi lớp mà code mới dùng, tra thư viện chứa nó và xem thư viện đó có trên classpath biên dịch của module không, không chỉ có ở runtime (L-007). Thư viện chưa có trong `gradle/libs.versions.toml` là thư viện mới.
  - Thêm thư viện mới thì so danh sách `uses-permission` của manifest gộp trước và sau khi thêm (`./gradlew :app:processStagingDebugMainManifest`, file trong `app/build/intermediates/merged_manifest/`), ghi quyền nó kéo vào.
  - Thêm thư viện hay module mới thì chạy `./gradlew :app:assembleStagingRelease` trên nhánh có code liên quan và ghi lệnh, kết quả vào mục 4.1: R8 chỉ chạy ở bản release, nên thư viện thiếu luật giữ lớp vẫn qua `check.sh` (L-053). Luật R8 cần thêm thì ghi file `consumer-rules.pro` (hay `proguard-rules.pro`) của module vào mục 3.2, vì phiên gặp file này ngoài mục 3.2 thì phải dừng task. `pr-check` cũng build bản release khi PR đổi thư viện, file build hay luật R8.
  - Phần chạy thử (mục 4.1) gọi đúng các API và lớp đó, để lỗi thiếu thư viện hay thiếu quyền lộ ra trước khi duyệt.
  - Quyền mới phải có trong spec (mục 6, Quyền của app). Spec chưa có thì đề xuất sửa spec và đặt câu hỏi ở mục 8; plan chờ spec được xác nhận lại rồi mới duyệt. Ghi `permission` hay `sdk` vào `blocking_points`.
- Test của plan phải qua được các quy tắc của `scripts/check.sh` (bảng ở [auto-run.md](auto-run.md), bước 5), nhất là lệnh `captureRoboImage` ghi thẳng đường dẫn `"src/test/screenshots/<Tên>.png"` và mỗi ảnh chuẩn mới có trong mục 3.2. `scripts/plan-check.py` dò các mẫu này (L-013).
- Điều kiện về file trên R2 (file phải có trước task, địa chỉ trả `200`) không ghi thành lệnh `curl` mà phiên tự chạy: proxy của môi trường `claude --cloud` chặn máy chủ R2 và trả 403. Kiểm bằng một workflow trên GitHub, hoặc ghi rõ "dev kiểm trước phiên" để người kiểm và ghi kết quả vào danh sách chạy (L-005).
- Plan thêm kho Maven hay máy chủ tải mới (`settings.gradle.kts`, `repositories`): chạy thử phần tải trong chính môi trường cloud của phiên, không chỉ trên máy dev, và ghi kết quả vào mục 4.1. Proxy của môi trường chỉ cho các máy chủ trong chính sách mạng; bị chặn (403, `connect_rejected`) thì người thêm máy chủ vào chính sách trước khi duyệt plan (L-067 của mẫu).
- Thay đổi về trợ năng (ẩn khỏi trình đọc màn hình, gộp node, thứ tự đọc) cũng là kỹ thuật phải chạy thử: thuộc tính ngữ nghĩa của Compose không tự lan từ node cha xuống node con. Test tạm đo trên cây `AccessibilityNodeInfo` hay trên chính node đích, không suy ra từ node cha (L-004).
- Điểm kỹ thuật vẫn còn chưa chắc sau khi chạy thử: ghi phương án dự phòng ở mục 3.4 (điều kiện chuyển cụ thể, cách làm thay), đưa file của phương án đó vào mục 3.2 với ghi chú "(dự phòng)", để phiên chuyển sang thay vì dừng task (`risk-scoring.md`, mục 4). Phương án dự phòng không được hạ tiêu chí chấp nhận hay đổi test viết trước.
- Chấm rủi ro theo [risk-scoring.md](risk-scoring.md), ghi lý do cho từng tiêu chí. Không hạ điểm để plan dễ được duyệt.
- Ghi `token_estimate` theo cỡ của ticket, lấy số gợi ý từ `scripts/token-usage.py estimate`; task khác thường thì chỉnh và ghi lý do. Không nâng số gợi ý theo một hai task của một phiên: mẫu của script đã gồm phiên con, và task thật thường dùng 0,4 đến 0,7 lần số nâng tay (L-081). Ước tính chỉ để lập kế hoạch, không chặn task ([ops/token-usage.md](ops/token-usage.md)).
- Nêu thẳng điều chưa chắc chắn trong mục câu hỏi cho người duyệt.
- Tiêu chí kiểm thủ công chạy được trên emulator thì viết thành kịch bản (`docs/ops/test-sessions.md`, mục 5): tệp `<module>/src/deviceScenarios/<T-NNN>-<tên>.txt`, đưa vào mục 3.2, chạy `scripts/device-scenario.py check <tệp>`. Mỗi dòng "Kiểm tra thủ công" của mục 4 ghi `(emulator: <tệp>)`, `(máy thật: <lý do>)` hay `(người: <lý do>)` (L-062; `scripts/plan-check.py`, quy tắc 11: lỗi khi plan còn `draft`). Tệp kịch bản là test chạy ở `device-session` cuối phiên, không "thất bại trước" được, nên ghi vào ngoại lệ viết test trước.
- Test kiểm giá trị mặc định ("chưa lưu gì thì trả mặc định", "tệp hỏng thì trả `null`") thường qua ngay trên khung rỗng của bước 3. Ghi sẵn trong mục 4 một bước đối chứng: ghi rồi đọc lại, hoặc đặt dữ liệu đúng cạnh dữ liệu sai, để test thất bại đúng lý do (L-035).
- Plan ghi số liệu hay trạng thái theo vị trí hay theo đối tượng: liệt kê mọi cách đối tượng đó được dùng hiện nay (một yêu cầu, nhiều yêu cầu song song, kho dùng chung) và nói rõ cách tính cho từng cách; mục kiểm thử có một test cho hai yêu cầu chồng nhau (L-068 của mẫu).
- Tiêu chí chấp nhận áp cho mọi lượt gửi hay mọi lần chạy (ví dụ "không gửi khi app ở nền", "không chạy trước khi đồng ý"): liệt kê ở mục 4 mọi đường dẫn tới lượt đó (lượt đầu, nạp sẵn, thử lại, nạp lại, nạp bù) và cho mỗi đường một test; hoặc đưa điều kiện vào một hàm thuần mà mọi đường cùng gọi, rồi test hàm đó và test mỗi đường có gọi nó. Không để đường nào chỉ nằm trong ngoại lệ viết test trước (phần nối) (L-072 của mẫu).
- Plan thêm trạng thái giữ ở vỏ app (`MainActivity`, `AppNavHost`): mục 4 có test dựng lại activity (`ActivityScenario.recreate()`, hoặc khung hình đầu khi chưa có back stack). Plan giữ tài nguyên theo cờ hay khóa Remote Config (quảng cáo đã tải, kết nối): mục 4 có test cờ tắt giữa chừng. Rà soát độc lập từng bắt các lỗi này mà test viết trước bỏ sót (L-061 của mẫu). Plan có bước chờ có giới hạn trước khi hiện thứ gì đó (quảng cáo, hộp thoại, điều hướng): nêu điều xảy ra khi app ra nền giữa lúc chờ, và mục 4 có test đưa activity ra nền rồi trở lại (`ActivityScenario.moveToState(CREATED)` rồi `RESUMED`) (L-091 của mẫu).
- Test quét mã nguồn (không file nào ngoài một source set chứa một chuỗi): quét theo ký hiệu (import, tên lớp đầy đủ, khai báo manifest), không theo chuỗi con, vì chú thích hay ví dụ KDoc của task khác cũng chứa chuỗi đó. Không tránh được thì ghi ở mục 6 các plan cùng danh sách chạy có thể thêm chuỗi đó (L-062 của mẫu).
- Plan đổi hàm `suspend` mà vỏ app gọi (`AppNavHost`, `MainActivity`): thêm test Robolectric dựng vỏ app với bản giả, không để phần đó vào ngoại lệ viết test trước; lỗi luồng ở đó chỉ lộ ra ở `ui-test`, vòng CI dài nhất (L-030).
- File dùng chung mà plan khác chưa merge cũng tạo (tài nguyên, thành phần của `:core:designsystem`): thống nhất với plan đó, một plan ghi Tạo, plan kia ghi Sửa hoặc dùng lại; chú thích kiểu "task sau nối" thì ghi tên file cho task sau (L-033).
- Plan đổi `scripts/merge-if-green.sh` hay `.github/workflows/main-guard.yml`: liệt kê mọi kiểm tra hiện có mà trường hợp mới phải đi qua (nhãn, `main-broken`, phiên đang chạy, nhánh cập nhật, CI) và nói trường hợp mới qua hay bị chặn ở từng kiểm tra, mỗi kiểm tra một test (L-044).
- Hàm phụ trợ nằm trong file test mà cũng phải đổi (ví dụ danh sách mà test so sánh): ghi rõ phần nào vào commit test của bước 3, phần nào vào commit code của bước 4, để test viết trước thất bại đúng lý do (L-025 của mẫu).
- Test của script chạy trên chuỗi giả hay tệp mẫu (adb giả, dump giả, repo giả) thì chạy thật trên code hiện tại ngay lúc viết plan, trong một khối tạm không commit, và ghi kết quả thật của từng test vào mục 4.1 (lý do thất bại, hay "đối chứng, qua"); không suy kết quả bằng cách đọc code. Chỉ chạy các test mới của script, vài giây, không chạy cả bộ test; quy tắc riêng của app, dự án mẫu không nhận (L-075).
- Lệnh trong plan ghi tệp tạm vào `${TMPDIR:-/tmp}`, không ghi `$TMPDIR` hay `${TMPDIR}` trống: phiên `claude --cloud` không đặt biến này, nên tệp bị ghi vào gốc ổ đĩa. Lệnh không mở đầu bằng `sleep` (công cụ chặn); chờ bằng `scripts/wait-run.sh` hay một vòng có giới hạn nằm trong lệnh. `scripts/plan-check.py`, quy tắc 12, báo lỗi khi plan `draft` ghi `$TMPDIR` không có giá trị mặc định (L-071).
- Plan dịch chữ hiển thị: liệt kê các chuỗi đi cùng con số hay phần trăm (con trượt, nhãn đo), ghi giá trị lớn nhất nghĩa là gì, và thêm một test chặn từ ngược nghĩa khi có rủi ro (ví dụ "độ đậm" dịch thành "độ trong suốt"). Từ lấy lại từ feature khác có thể đổi nghĩa khi đi cùng số (L-077).
- Plan chép code từ dự án mẫu về app: dán nguyên đoạn code (hay `diff`) vào mục 3.1, kèm commit nguồn. Phiên `claude --cloud` của app chỉ có quyền đọc repo của app, không đọc được repo mẫu (L-078, `docs/ops/from-app.md`).
- Plan thêm file trên R2 mà app bắt buộc phải có (catalog, ảnh của bộ khởi đầu): thêm đường dẫn vào `docs/ops/required-assets.txt` và ghi file này ở mục 3.2, để `scripts/setup-check.sh` kiểm file đó mỗi sáng (`env-canary.yml`, job `setup-check`) và trước mỗi phiên (`start-run.yml`) (L-011, `docs/ops/staging-and-runs.md`).
- Viết xong thì chạy `scripts/plan-check.py <plan>`, sửa hết lỗi trước khi đưa người duyệt, dán kết quả vào mô tả PR. Phiên tự chạy chạy lại script này ngay trước task và bỏ qua plan có lỗi.
- Plan mới có trạng thái `draft`. Không bao giờ tự chuyển sang `approved`.

## Khi lập danh sách chạy

- Sau họp đánh giá rủi ro, lập `runs/RUN-YYYYMMDD-NN.md` theo [mẫu phiên tự chạy](templates/run.md), sắp các plan đã duyệt theo thứ tự đã thống nhất.
- Gom việc của cả nhóm vào một phiên: mặc định đưa vào danh sách **mọi plan `approved` qua `plan-check.py`**, của mọi dev, thay vì chia thành nhiều phiên ít task. Mỗi phiên tốn khoảng 30 phút cố định (kiểm tra đầu phiên, emulator, staging, báo cáo) trong khi mỗi task khoảng 15 phút, nên phiên nhiều task rẻ hơn nhiều phiên một task. Mỗi lúc chỉ một phiên chạy (auto-run.md, mục 1).
- Số task của phiên do dev quyết định khi duyệt danh sách, dựa trên ước tính; AI không đặt trần cố định. AI ghi trong mục Lý do của thứ tự chạy những gì dev cần để quyết định: tổng `token_estimate` so với tổng token thật của vài phiên gần nhất (`docs/metrics/token-usage.csv`), và thời gian dự kiến (khoảng 15 phút mỗi task cộng khoảng 30 phút cố định) so với `staging_needed_by` nếu có. `max_tasks` mặc định bằng số task trong danh sách; dev giảm nếu muốn. Plan nào để phiên sau thì ghi tên và lý do.
- Một task dừng không được làm hỏng cả đêm: task phụ thuộc task bị dừng sẽ bị bỏ qua. Xếp các task độc lập trước, chuỗi task phụ thuộc nhau sau cùng. Danh sách chỉ có một chuỗi phụ thuộc thì ghi điều đó trong mục Lý do của thứ tự chạy và đề xuất thêm task độc lập đã sẵn sàng (ticket `ready` có plan duyệt được), nếu có.
- Các plan trong danh sách đều qua `scripts/plan-check.py`.
- Plan có test của script chạy trên chuỗi giả hay tệp mẫu có kết quả thật ở mục 4.1 (đã chạy trên code hiện tại), không phải kết quả suy từ code (L-075). Thiếu thì ghi điểm chặn vào mục Kiểm tra trước khi duyệt.
- Lập xong thì kiểm tra cả danh sách theo [ops/pre-run-review.md](ops/pre-run-review.md): chạy `scripts/run-check.py <danh sách chạy>` và sửa hết lỗi, rồi giao một phiên AI riêng (ngữ cảnh mới, chỉ đọc) rà soát mâu thuẫn, test chưa đủ tốt và khả năng chạy được trong phiên. Ghi kết quả vào mục Kiểm tra trước khi duyệt của danh sách. Điểm chặn thì soạn plan phiên bản mới hay đề xuất sửa spec, hoặc đề xuất bỏ task; không tự sửa plan `approved` hay spec `confirmed`.
- Ghi tổng `token_estimate` của danh sách trong mục Lý do của thứ tự chạy, so với tổng token thật của vài phiên gần nhất (`docs/metrics/token-usage.csv`), để người duyệt thấy danh sách có vừa một phiên không. Chỉ để tham khảo, không bỏ task vì vượt.
- `plan_queue` mặc định 2 (số plan soạn sẵn cuối phiên); dev đổi hay đặt `0`.
- Hàng chờ plan cuối phiên (`scripts/plan-queue.py`) nhận ticket `backlog` hay `ready`, miễn là ticket chưa có plan, spec đã chốt và không còn câu hỏi mở, phụ thuộc đã có plan và không dừng hay hủy. Dev không cần chuyển ticket sang `ready` để nó vào hàng chờ; `ready` chỉ là dấu tùy chọn. Muốn giữ một ticket ra khỏi hàng chờ thì để câu hỏi mở trong spec, hay đặt `plan_queue: 0`.
- Danh sách chạy có trạng thái `draft`. Chỉ dev mới chuyển sang `approved` và kích hoạt phiên.

## Trong phiên tự chạy

- Làm đúng các bước trong [auto-run.md](auto-run.md), lần lượt từng task, không chạy song song.
- Chỉ thực hiện plan có trạng thái `approved` có trong danh sách chạy đã duyệt.
- Tuân thủ điều kiện dừng ở mục 4 của [risk-scoring.md](risk-scoring.md).
- Đặt tên nhánh theo dạng `claude/T-NNN-<tu-khoa>`, trừ nhánh plan soạn sẵn cuối phiên: `claude/plan-T-NNN-<tu-khoa>`.
- Chỉ merge bằng `scripts/merge-if-green.sh`. Không dùng nút merge, `gh pr merge` hay gọi thẳng API để merge. Không đóng hay sửa issue mang nhãn `main-broken`; việc đó thuộc về người. Nhánh chính đỏ vì commit merge của chính phiên thì revert theo `auto-run.md`, mục 3, Khi nhánh chính đỏ vì task của phiên (ADR-0025).
- Sau mỗi task, ghi nhật ký vào cuối plan và cập nhật trạng thái ticket: `on-staging` khi đã merge, `stopped` khi dừng.
- Cuối phiên, viết báo cáo vào file phiên chạy, rồi soạn plan `draft` cho hàng chờ theo `auto-run.md`, mục 4, bước 5. PR plan là nháp có nhãn `needs-human`; phiên không chuyển nó sang sẵn sàng, không duyệt, không merge.

## Khi phân tích một phiên thử

Phiên thử là log của một lần dùng thử bản staging trên máy thật hoặc emulator, ghi bằng `scripts/device-session.sh` vào `test-sessions/TS-YYYYMMDD-NN/` (`docs/ops/test-sessions.md`).

- Đọc theo thứ tự: `summary.md`, `session.md`, `notes.md` của người thử, `app-log.txt`, `scenarios.txt` và ảnh nếu có, rồi các đoạn cần trong `logcat.txt`. Dòng `VGL.Session` chia log theo bước.
- Đọc spec của feature đã thử. Dựng lại diễn biến theo thời gian và đối chiếu với từng kịch bản: luồng lệch kịch bản là một phát hiện.
- Mỗi phát hiện phải có bằng chứng là dòng log có giờ. Tìm đoạn code liên quan và ghi rõ đâu là kết luận, đâu là giả thuyết. Không kết luận về tốc độ hay giật khung hình từ emulator.
- Ngoài lỗi, tìm chỗ người dùng có vẻ lúng túng (bấm lại nhiều lần, quay ra quay vào, chờ lâu) và đề xuất cải tiến.
- Log không đủ để kết luận thì ghi vào mục Log còn thiếu, không đoán.
- Viết báo cáo `docs/test-sessions/TS-YYYYMMDD-NN.md` theo [mẫu phiên thử](templates/test-session.md). Mỗi phát hiện và đề xuất thành một phản hồi theo [mẫu phản hồi](templates/feedback.md), trạng thái `new`, trong `features/<feature>/feedback/`, liên kết về báo cáo.
- Chỉ trích những dòng log làm bằng chứng. Bỏ mã định danh máy, tên tài khoản, tên mạng, đường dẫn có tên người và mọi dữ liệu người dùng. Không đưa log thô vào repo.

## Khi xử lý phản hồi

- Phản hồi mới trong `inbox/` mà xác định được feature thì đề xuất chuyển vào `features/<feature>/feedback/`.
- Đề xuất phân loại: lỗi so với spec, thay đổi yêu cầu, hay góp ý mới. Chủ feature quyết định.
- Phản hồi là thay đổi yêu cầu thì không sửa thẳng vào code. Đề xuất cập nhật spec, chờ người phụ trách sản phẩm xác nhận.
- Phản hồi là lỗi so với spec thì cập nhật plan (tăng phiên bản plan, thêm mục sửa lỗi). Plan sửa đi qua họp đánh giá rủi ro như plan mới.

## Khi viết ADR

- Dùng đúng [mẫu ADR](templates/adr.md). Mục "Việc AI phải tuân thủ" viết thành các vế ngắn nối bằng dấu chấm phẩy: mỗi vế thành một dòng của `foundation.md`.
- Mã ADR: ở dự án mẫu (repo có `docs/ops/new-app.md`) lấy số tiếp theo dưới 1000 và ghi `origin: template`. Ở app, lấy số tiếp theo từ 1001 và ghi `origin: app`, để không trùng với ADR mà dự án mẫu thêm sau này.
- Trong app, không sửa ADR có `origin: template`. Muốn khác thì viết ADR của app, ghi rõ thay quy tắc nào và vì sao (`supersedes`), và đề xuất về dự án mẫu nếu có ích chung (`docs/ops/from-app.md`).
- PR thêm hay đổi trạng thái ADR thì chạy `scripts/foundation.py` và commit `docs/foundation.md` cùng PR. Test của script báo lỗi khi trang chưa khớp.

## Trang tiến độ

`docs/STATUS.md` cho thấy tiến độ cả dự án: việc đang chờ người, yêu cầu và trạng thái, ticket của từng feature, phiên tự chạy, bài học và ADR. Trang do `scripts/status.py` sinh từ trạng thái trong các tài liệu, không sửa tay. Bản đọc được nằm trên nhánh `status`: https://github.com/VGL-Studio/camera-pose/blob/status/docs/STATUS.md.

- Trang không có trong git của nhánh chính (`.gitignore`), nên PR không bao giờ xung đột vì nó (L-016, L-047). Cụ thể:
  - Sau mỗi lần đẩy lên nhánh chính có đổi `docs/` hay `scripts/status.py`, workflow `status-page.yml` chạy `scripts/status.py` và đẩy đè nhánh `status`: nhánh chính cộng đúng một commit chứa trang. Workflow không bao giờ đẩy lên nhánh chính. Đọc trang ở [nhánh status](../../../blob/status/docs/STATUS.md); các liên kết tương đối của trang đúng vì cây file là cây của nhánh chính.
  - PR không commit `docs/STATUS.md`, kể cả PR báo cáo của phiên. `scripts/merge-if-green.sh` báo chú ý khi PR có file này (ví dụ thêm bằng `git add -f`).
  - Muốn xem trước trên máy thì chạy `scripts/status.py`; git bỏ qua file vừa sinh. `scripts/status.py --check` cho biết trang trên máy đã khớp với tài liệu chưa.
  - Lần chạy `status-page` đỏ (ví dụ `status.py` báo số bài học dùng hai lần) thì trang trên nhánh `status` giữ bản cũ: sửa tài liệu gốc trong một PR thường.
  - Trước đây trang đi vào PR báo cáo và một PR trang riêng (`claude/status-YYYYMMDD`): trang trên nhánh chính chậm tới khi có người mở PR trang, và mỗi lần xem trang mới tốn một PR. Không dùng `.gitattributes` với `merge=ours`: merge trên GitHub không chạy driver tùy chỉnh, nên PR vẫn báo xung đột.
- Trạng thái ticket đi theo plan (L-019, L-045). Người duyệt hay dừng plan chạy `scripts/status.py --sync`: plan `approved` thì ticket đang `backlog`, `ready`, `planned` hay `stopped` chuyển sang `approved`; plan `stopped` thì ticket chuyển sang `stopped` (trừ ticket đã `cancelled` hay đã qua phiên). Script chỉ làm theo quyết định của người về plan, rồi sinh lại trang trên máy; commit các ticket nó đổi, trang không có trong git. Không chuyển ticket sang `approved` khi plan còn `draft`. `scripts/plan-check.py` báo chú ý khi hai trạng thái lệch nhau.
- Mục 6 của trang, "Sẵn sàng phát hành", liệt kê việc còn lại trước bản production, chỉ từ tài liệu trong repo (L-086 của mẫu): khóa vị trí quảng cáo cần mã thật (đọc `AdPlacement.kt` theo đúng luật một dòng của `scripts/release.sh`, bỏ khóa `gallery_` chỉ có ở staging), mục `- [ ]` của "Cần người kiểm tra" trong báo cáo phiên `finished`, ticket chưa lên staging, phản hồi đang mở và đường dẫn tới issue đang mở của repo `origin`. Người kiểm xong một mục thì đánh dấu `[x]` trong báo cáo phiên; lần sinh sau trang bỏ mục đó, AI không đánh dấu thay người. Script không mở cấu hình production, `scripts/release.sh` hay thư mục phát hành, và không dùng mạng. Đổi riêng `AdPlacement.kt` thì trang chỉ sinh lại ở lần đẩy kế tiếp có đổi `docs/` hay `scripts/status.py`.
- Trang thiếu thông tin hay sai thì sửa tài liệu gốc hoặc script, không sửa trang.

## Những điều không được làm

- Không sửa nội dung đã xác nhận: spec `confirmed`, plan `approved`, ADR `accepted`. Muốn thay đổi thì thêm mục mới hoặc tạo bản thay thế.
- Không xóa tài liệu. Tài liệu hết hiệu lực được chuyển trạng thái, không bị xóa.
- Không đưa vào tài liệu bất kỳ khóa bí mật, mã quảng cáo thật, thông tin đăng nhập hay dữ liệu cá nhân của người dùng.
- Không tự cấp quyền duyệt, không tự chuyển trạng thái thay cho người.

## Cách viết

- Tiếng Việt tự nhiên, câu ngắn, dễ hiểu. Không dùng từ hiếm, không dịch từng chữ từ tiếng Anh.
- Thuật ngữ kỹ thuật thông dụng giữ tiếng Anh: module, test, build, staging, commit, merge.
- Không dùng dấu gạch ngang dài, không dùng biểu tượng cảm xúc.
- Ngày viết theo dạng `YYYY-MM-DD`.
