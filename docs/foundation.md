# QUY TẮC NỀN TẢNG

Trang này do `scripts/foundation.py` sinh từ mục "Việc AI phải tuân thủ" của các ADR, không sửa tay. Muốn đổi một quy tắc thì sửa hoặc thay ADR, rồi chạy lại script.

AI đọc trang này trước mọi task. Chỉ mở ADR đầy đủ (bối cảnh, phương án, hệ quả) khi task đụng vào vùng của ADR đó hoặc định đổi quy tắc của nó: lý do nằm ở đó.

ADR có `origin: template` đến từ dự án mẫu: trong app, không sửa; muốn khác thì viết ADR riêng của app (đánh số từ 1001) ghi rõ thay quy tắc nào và vì sao, và đề xuất về dự án mẫu nếu có ích chung (`docs/ops/from-app.md`).

## Quy tắc từ dự án mẫu

### [ADR-0001](adr/0001-kotlin-compose-feature-modules.md): Dùng Kotlin, Jetpack Compose và chia module theo feature

- code của feature nằm trong module của feature đó
- cần dùng chung thì đề xuất đưa vào core, không import chéo giữa các feature

### [ADR-0002](adr/0002-merge-to-main-after-quality-gate.md): Merge thẳng vào nhánh chính, chỉ sau khi qua cổng chất lượng

- không bao giờ tìm cách qua cổng chất lượng bằng cách sửa, xóa test hay hạ tiêu chí kiểm tra

### [ADR-0003](adr/0003-feature-flags-remote-config.md): Dùng cờ tính năng qua Firebase Remote Config

- mỗi feature mới có cờ riêng khai báo trong `:core:config`, tên theo dạng `feature_<ten>_enabled`
- không đọc Remote Config trực tiếp từ module feature

### [ADR-0004](adr/0004-ads-admob-mediation.md): Dùng AdMob làm nền tảng điều phối mediation, quảng cáo qua một lớp trung gian

- không tự thêm vị trí quảng cáo ngoài spec
- không đưa mã quảng cáo thật vào code hay tài liệu
- khởi tạo SDK quảng cáo theo luật đồng ý của `docs/ops/ads-business-rules.md`, mục 1 (ADR-0026)

### [ADR-0005](adr/0005-no-child-audience.md): Không nhắm tới trẻ em

- khi sinh spec hoặc nội dung, không định hướng giao diện, hình ảnh, nội dung chữ theo hướng dành cho trẻ em
- gặp yêu cầu có dấu hiệu này thì đặt câu hỏi

### [ADR-0006](adr/0006-no-backend-immutable-r2-assets.md): Không có backend, file tĩnh trên R2 chỉ thêm mới

- không viết code hoặc script ghi đè, xóa file trên R2
- mọi đường dẫn R2 lấy từ `:core:assets`, không viết cứng trong feature

### [ADR-0007](adr/0007-separate-staging-production.md): Tách hai môi trường staging và production cho mọi dịch vụ bên ngoài

- không viết cứng giá trị cấu hình môi trường trong code
- không bao giờ đọc, sửa hoặc tạo cấu hình production
- mọi thao tác trong phiên tự chạy chỉ trên môi trường staging

### [ADR-0008](adr/0008-design-system-source-of-truth.md): Hệ thống thiết kế lấy code làm gốc, Claude Design đồng bộ từ code

- chỉ dùng màu, chữ, khoảng cách, thành phần từ `:core:designsystem`
- không viết cứng mã màu hay cỡ chữ trong feature

### [ADR-0009](adr/0009-auto-run-infrastructure.md): Phiên tự chạy dùng Claude Code routine, staging chạy trên GitHub Actions, phát hành production ngoài GitHub

- đặt tên nhánh theo `claude/T-NNN-<tu-khoa>`
- ghi đường dẫn phiên chạy vào mô tả PR
- không cố cài hay chạy emulator trong phiên
- không đọc, in ra hay chuyển đi bất kỳ bí mật nào của repo
- không viết workflow mới có dùng bí mật, trừ khi đó chính là plan rủi ro cao đã được duyệt chéo
- không chạm vào `scripts/release.sh`

### [ADR-0010](adr/0010-soft-quality-gate-github-free.md): Cổng chất lượng mềm trên GitHub Free, merge chỉ qua script kiểm tra

- chỉ merge bằng `scripts/merge-if-green.sh`
- không sửa script này, các workflow trong `.github/` hay nội dung kiểm tra của cổng chất lượng, trừ khi đó chính là plan rủi ro cao đã được duyệt chéo
- thấy nhãn `main-broken` thì dừng phiên, trừ trường hợp phiên tự revert commit merge của chính phiên theo ADR-0025

### [ADR-0011](adr/0011-toolchain-and-code-checks.md): Bộ công cụ build và kiểm tra code của dự án mẫu

- chạy `scripts/check.sh` trước khi đẩy code
- chạy `./gradlew ktlintFormat` khi ktlint báo lỗi, không tắt quy tắc
- không cập nhật ảnh chuẩn screenshot ngoài plan

### [ADR-0012](adr/0012-firebase-wiring.md): Nối Firebase vào các module core, bản staging build bằng file cấu hình giả khi không có file thật

- không gọi Firebase từ module feature
- không sửa hay xóa file `google-services.json` giả, trừ khi đổi `applicationId` theo plan đã duyệt
- không tạo, đọc hay chép file `google-services.json` thật vào bất kỳ đâu

### [ADR-0013](adr/0013-ads-consent-and-guardrails.md): Quảng cáo AdMob chỉ khởi động sau hộp thoại đồng ý UMP, mã quảng cáo production đưa vào lúc build phát hành

- feature chỉ dùng composable và hàm của `:core:ads` và `PrivacyOptionsButton`
- không gọi `AdsController` hay SDK quảng cáo từ feature
- không viết mã quảng cáo thật vào code, tài liệu hay tham số build trong repo
- feature chỉ gọi quảng cáo toàn màn hình ở hành động của người dùng
- quảng cáo lúc mở app và khi quay lại app do `:core:ads` và vỏ app làm theo `docs/ops/ads-business-rules.md` (ADR-0026)

### [ADR-0014](adr/0014-ads-default-values.md): Giá trị mặc định của tham số quảng cáo, thay bảng mặc định trong ADR-0013

- vị trí mới mặc định tắt
- giá trị mặc định của khóa quảng cáo theo bảng khóa của `docs/ops/ads-business-rules.md`, mục 10 (ADR-0026), chỉ đổi khi người phụ trách sản phẩm quyết

### [ADR-0015](adr/0015-r2-assets-module.md): Module `:core:assets` tải file tĩnh từ R2 một lần, giữ trên máy, đường dẫn bắt buộc có phiên bản

- feature chỉ dùng `RemoteAssets`, không gọi HTTP hay viết địa chỉ R2
- mỗi thay đổi nội dung file là một đường dẫn mới
- không viết code hay script ghi đè, xóa file trên R2

### [ADR-0016](adr/0016-staging-and-start-run-workflows.md): Bản staging build bằng biến thể release ký khóa staging riêng, phiên tự chạy kích hoạt qua API của routine

- không đọc, in ra hay chuyển đi giá trị bí mật
- không đổi `staging.yml` hay `start-run.yml` ngoài plan rủi ro cao đã duyệt chéo
- cuối phiên tự chạy chỉ khởi động `staging.yml` trên nhánh chính

### [ADR-0017](adr/0017-release-and-new-app-scripts.md): Script phát hành chỉ build bản ký, người tải lên Google Play; script khởi tạo chép dự án mẫu thành repo mới

- không chạm vào `scripts/release.sh` (ADR-0009)
- không đọc, tạo hay sửa thư mục phát hành
- khi đổi tên một file mà `scripts/new-app.sh` sửa (README, CLAUDE.md, `strings.xml`, `settings.gradle.kts`, `app/build.gradle.kts`), cập nhật script và test của nó trong cùng PR

### [ADR-0018](adr/0018-loading-waits-for-remote-config.md): Màn hình loading chờ tải Remote Config khi mở app, các màn hình khác vẫn không chờ

- chỉ màn hình loading dùng `RemoteConfigRefresher`. Màn hình khác muốn chờ Remote Config cần spec và ADR riêng

### [ADR-0019](adr/0019-app-logging-and-device-test-diagnostics.md): App ghi log hành vi qua :core:logging; CI giữ lại chi tiết lỗi và log khi test trên emulator đỏ

- ghi log theo mục Quyết định, không ghi dữ liệu cá nhân
- không gọi `android.util.Log` trong feature
- khi test emulator đỏ, đọc bước "Chi tiết test trên emulator" trước khi sửa

### [ADR-0023](adr/0023-remote-config-conventions.md): Khóa Remote Config phẳng, có kiểu, khai báo một chỗ trong code, sinh bảng khóa và file mẫu; staging có giá trị cục bộ

- đặt tên khóa theo quy ước của ADR này (khóa quảng cáo theo ADR-0026 nếu ADR đó được duyệt)
- không dùng khối JSON làm cấu hình
- khai báo khóa mới kèm kiểu, mặc định an toàn và mô tả ở module sở hữu, rồi sinh lại bảng khóa trong cùng PR
- không đổi tên hay đổi kiểu một khóa đã phát hành
- không đổi giá trị trên Firebase (việc của người)

### [ADR-0024](adr/0024-network-required.md): App chỉ dùng được khi có Internet; mất mạng thì app chặn bằng một popup chung có nút thử lại

- không làm chế độ ngoại tuyến trong feature
- feature không tự đọc trạng thái mạng của hệ thống, mất mạng do `:app` xử lý bằng popup chung
- lỗi máy chủ trả về vẫn báo trong `UiState` kèm thử lại
- spec mới không ghi tiêu chí "dùng được khi không có mạng"

### [ADR-0025](adr/0025-main-self-healing.md): Nhánh chính tự hồi phục khi đỏ: chạy lại test hỏng một lần, tự đóng issue khi chạy lại xanh, merge bản revert được kiểm bằng nội dung

- `main-guard` chạy lại `check.sh` một lần khi chỉ có test thất bại và ghi issue `flaky` nếu lần chạy lại xanh
- khi `main-broken` mở, chỉ merge PR mà `scripts/is-exact-revert.sh` xác nhận là bản revert đúng của commit gây lỗi
- không sửa hay cập nhật nhánh `revert/` trước khi merge
- phiên chỉ revert commit merge của chính phiên, theo `docs/auto-run.md` mục Khi nhánh chính đỏ vì task của phiên
- AI vẫn không đóng, sửa hay bình luận để gỡ issue `main-broken`
- chép issue `flaky` đang mở vào báo cáo phiên
- các quy tắc khác của ADR-0010 vẫn áp dụng

### [ADR-0026](adr/0026-ads-follow-adskit-partner.md): Hành vi kinh doanh của quảng cáo theo bộ PARTNER của AdsKit, cách làm kỹ thuật theo cấu trúc của dự án mẫu

- hành vi quảng cáo theo `docs/ops/ads-business-rules.md` (bộ PARTNER của AdsKit), không theo trí nhớ về AdsKit hay về ADR-0022
- không chép code AdsKit và không chép khóa nào từ AdsKit
- luật thuần của quảng cáo tách thành hàm có unit test với đồng hồ giả, đo thời gian bằng đồng hồ đơn điệu
- khóa Remote Config quảng cáo dùng đúng tên trong bảng khóa của `ads-business-rules.md`, khóa mới cùng kiểu đặt tên và ghi rõ là tên do ta đặt
- vị trí mới khai báo theo mô hình chung (mã, định dạng, nhóm giãn cách) với khóa suy ra theo mẫu, không thêm cờ, hàm hay composable mang tên tính năng của một app vào `:core:ads`
- không ghi `ad_impression` từ app, doanh thu theo vị trí ghi bằng `ad_impression_placement`
- giữ các quy tắc ở mục 10 của ADR-0026 kể cả khi spec yêu cầu khác, gặp yêu cầu như vậy thì ghi thành câu hỏi
- không thêm vị trí, định dạng hay sự kiện ngoài spec đã duyệt

### [ADR-0027](adr/0027-appsflyer.md): Dùng AppsFlyer trong :core:analytics cho doanh thu quảng cáo và phễu quảng cáo, khóa dev đưa vào lúc build, staging không gửi

- chỉ gọi SDK AppsFlyer trong `:core:analytics`
- không viết khóa dev AppsFlyer vào code, tài liệu, tham số build hay file trong repo
- không gửi `af_revenue`, doanh thu quảng cáo chỉ qua `af_ad_revenue` bằng hàm ghi sự kiện thường
- build không có khóa thì không khởi động AppsFlyer
- không thêm sự kiện AppsFlyer ngoài `ads-business-rules.md` mục 8.3 và spec đã duyệt
- mỗi sự kiện AppsFlyer đi qua công tắc từ xa của nó
- không gửi dữ liệu cá nhân hay chữ người dùng nhập sang AppsFlyer

### [ADR-0029](adr/0029-lech-pham-vi-lam-tiep-va-merge.md): Phiên tự chạy sửa tiếp phần ngoài plan cần cho task, merge rồi báo sau, thay vì dừng task

- phần ngoài mục 3.2 cần để thay đổi của plan chạy đúng thì sửa tiếp và merge, không dừng task, không giới hạn số file hay module
- vẫn dừng khi phải sửa file trong mục "Không được làm" của `CLAUDE.md`, khi phần ngoài plan thêm thư viện, quyền hay chạm điểm chặn 1 tới 6, 8, 9 của `risk-scoring.md` mà plan chưa nêu, hay khi gặp điều kiện dừng 3, 4, 6, 7, 8, 9
- test cũ đỏ chỉ vì dữ liệu chưa có phần plan thêm thì cập nhật dữ liệu đó và vẫn kiểm dữ liệu mới
- test cũ đỏ vì hành vi cũ bị hỏng thì sửa code, không sửa test
- không xóa test hay câu khẳng định, không nới phép so sánh, không thêm `@Ignore`
- ảnh chuẩn screenshot ngoài mục 3.2 lệch thì dừng
- điều kiện dừng riêng ở mục 7 của plan vẫn áp dụng
- ở bước 3 chạy test JVM của mọi module với `--continue` khi test viết trước đã biên dịch được
- mỗi phần ngoài mục 3.2 ghi vào nhật ký plan, mô tả PR (mục "Ngoài mục 3.2" kèm `git diff --stat`) và một dòng `- [ ]` ở mục Cần người kiểm tra của báo cáo phiên
- rà soát độc lập xét riêng phần đó, vi phạm là mức chặn
- ở bước 6 phải sửa điểm nên sửa có test viết trước, giới hạn 3 lần sửa mỗi điểm và 2 vòng rà soát lại mỗi task

### [ADR-0030](adr/0030-dong-bo-du-an-mau-ve-app.md): Đưa thay đổi của dự án mẫu về app theo từng đợt, bằng PR đồng bộ dựng từ diff của dự án mẫu

- không chép tay code hay tài liệu của dự án mẫu về phần dùng chung của app, dùng `scripts/template-sync.sh` theo `docs/ops/to-app.md`
- không sửa `base=` hay `hold=` của `docs/template-sync.txt` ngoài PR đồng bộ
- trước khi đồng bộ, đưa cải tiến chung của app chưa về dự án mẫu qua `from-app.md`
- xung đột ở phần của dự án mẫu thì lấy bản mẫu, trừ phần app chưa đưa về và giá trị riêng của app ở mục 6 của `to-app.md`
- phần app cũng sửa thì gộp tay, giữ phần riêng của app
- mô tả PR đồng bộ liệt kê cách gộp từng file xung đột
- phần giữ lại vì lớn thì đưa vào bằng một task có plan

### [ADR-0031](adr/0031-test-cu-mau-thuan-voi-plan.md): Test có sẵn kiểm hành vi mà plan đổi được tìm từ lúc soạn plan, và phiên tự viết lại câu khẳng định khi plan đã duyệt rõ điều mới

- plan sửa hay xóa code Kotlin ngoài thư mục test có mục 3.5 "Hành vi đã có bị đổi", liệt kê test có sẵn kiểm hành vi plan đổi (bắt đầu từ `scripts/existing-tests.py <plan>`) và câu khẳng định giữ nguyên hay câu mới viết sẵn, chỗ chưa quyết thì đặt câu hỏi ở mục 8
- trong phiên, câu khẳng định của test JVM có sẵn đỏ vì trái trực tiếp điều plan đã duyệt nói rõ thì viết lại khi đủ sáu điều: có câu của plan nói điều mới, có câu ý định và câu mới kiểm đúng ý định đó, không nới (không bớt câu, không đổi bằng thành khoảng hay chứa, không tăng sai số, không bỏ đối chứng, không `@Ignore`), có đối chứng đỏ khi ý định bị phá, không phải ảnh chuẩn hay test viết trước của plan hay câu kiểm tiêu chí của spec hay luật nghiệp vụ mà plan không đổi, tối đa 3 câu mỗi task
- thiếu một điều thì dừng như cũ
- mỗi câu viết lại ghi vào nhật ký plan, mục "Test cũ viết lại (ADR-0031)" của mô tả PR và một dòng `- [ ]` ở mục Cần người kiểm tra
- rà soát độc lập xét riêng từng câu, vi phạm là mức chặn

### [ADR-0032](adr/0032-lech-nho-tu-lam-roi-bao.md): Phiên tự chạy tự làm các lệch nhỏ đã liệt kê và tự bỏ qua loại task mà môi trường không làm được, rồi báo người sau

- chỉ tự làm các lệch nhỏ L1 tới L10 của quyết định 1 mà không cần plan phiên bản mới
- ngân sách kịch bản nâng tới tổng theo công thức làm tròn lên hàng trăm, không quá trần tính từ `timeout-minutes` của `device-session.yml`, trong PR của task làm tổng vượt
- `need` chỉ đổi thành `wait` khi có bằng chứng emulator đã có thiết bị nhờ task đã merge
- mốc dời chỗ chỉ khi mọi dòng `-` của plan (khối chỉ có dòng `+`: dòng ngữ cảnh trên, dưới hay tiêu đề mục) có nguyên văn ở chỗ mới, đặt các dòng `+` không đổi chữ, không thì dừng theo điều kiện 3
- test viết trước qua ngay chỉ được làm tiếp khi test chặt lại hay một đột biến làm nó thất bại, không thì dừng theo điều kiện 8
- không dùng `@Suppress`, lint baseline hay sửa `.editorconfig` thay cho L5
- tệp sinh ra chỉ của `scripts/foundation.py`, `scripts/status.py`
- mã kế tiếp đã có người dùng thì lấy mã sau và sửa mọi chỗ dẫn trong cùng PR
- lỗi khớp dòng `bo-qua-loai` của bảng tra lỗi thì bỏ qua mọi task còn lại mà lệnh `scripts/plan-check.py --touches` của dòng đó trả mã 0, làm tiếp các task khác, không kết thúc phiên, soạn danh sách chạy nháp cho task bị bỏ qua
- không tự thêm quyền, thư viện, SDK, quảng cáo, sự kiện phân tích, không chạm production, không đổi spec, thiết kế hay câu trả lời của người, không làm yếu test, không đẩy lại lệnh bị từ chối dưới dạng khác
- mỗi quyết định tự làm ghi vào mục "Quyết định AI đã tự làm" của báo cáo (task, loại, quyết định, lý do, cách hoàn tác), và mục Cần người kiểm tra luôn có dòng `- [ ] Quyết định AI đã tự làm: <N>`
- rà soát độc lập xét mỗi lệch L1 tới L7, L10, lệch ngoài danh sách là mức chặn

## Quyết định riêng của app

### [ADR-1001](adr/1001-loading-timeout-15s.md): Màn loading của Camera Pose chờ tối đa 15 giây cho mỗi thứ cần tải, kể cả Remote Config

- ở app này, màn loading chờ tối đa 15 giây cho cấu hình, catalog và từng file của bộ khởi đầu
- các quy tắc khác của ADR-0018 vẫn áp dụng

### [ADR-1004](adr/1004-chua-ap-dung-quang-cao-adskit.md): App chưa áp dụng quảng cáo theo AdsKit và AppsFlyer cho tới đợt đồng bộ core, trong lúc đó giữ quy tắc quảng cáo cũ

- chưa áp dụng ADR-0026, ADR-0027, các vế mà ghi chú ngày 2026-10-23 đổi trong ADR-0004, ADR-0013, ADR-0014, và vế "sinh lại bảng khóa" của ADR-0023, cho tới khi `core/ads` được đồng bộ qua plan đã duyệt
- không khởi tạo SDK quảng cáo trước khi xử lý xong sự đồng ý của người dùng
- feature chỉ dùng `AdSlot`, `rememberShowInterstitial` và `PrivacyOptionsButton`
- gọi quảng cáo toàn màn hình chỉ ở điểm dừng tự nhiên, không bao giờ lúc mở hay đóng app
- khóa quảng cáo dạng `ads_<vị trí>_enabled`, vị trí mới mặc định tắt
- khai báo khóa Remote Config mới như các khóa hiện có trong `:core:config` cho tới khi app có `AppRemoteKeys`
- không thêm AppsFlyer khi chưa có plan đồng bộ `core` được duyệt

## Đang đề xuất, chưa duyệt chính thức (vẫn làm theo khi tài liệu quy trình dẫn tới)

### [ADR-0020](adr/0020-design-handoff-and-conformance.md): Thiết kế trên Claude Design hoặc Figma, bàn giao bằng ảnh khung chuẩn trong repo, đối chiếu bằng ảnh so sánh

- chỉ dùng gói trong `design/`
- đặt tên screenshot test đúng tên ảnh trong `screens/`
- ảnh `_unfolded` có screenshot test ở khung `w673dp-h841dp-xxhdpi`
- chạy `scripts/design-compare.sh` và đưa `design/review/` vào PR khi feature có thiết kế
- không đổi giao diện để giảm tỷ lệ khác biệt bằng cách viết cứng giá trị

### [ADR-0021](adr/0021-clone-reference-apps.md): App mới có thể bắt đầu bằng việc AI khảo sát các app mẫu trên Play, đề xuất app tốt hơn, rồi thiết kế trên Figma cùng dev

- làm theo `docs/CLAUDE.md`, mục Khi khảo sát app mẫu và mục Khi thiết kế trên Figma
- chỉ điều khiển máy qua `scripts/app-explore/ax.sh`
- không vượt bộ chặn
- không tự chuyển trạng thái bản đề xuất hay thiết kế
- ghi mọi tài sản mượn vào `docs/borrowed-assets.md` trong cùng PR với file mượn
