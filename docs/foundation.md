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
- không khởi tạo SDK quảng cáo trước khi xử lý xong sự đồng ý của người dùng

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
- thấy nhãn `main-broken` thì dừng phiên

### [ADR-0011](adr/0011-toolchain-and-code-checks.md): Bộ công cụ build và kiểm tra code của dự án mẫu

- chạy `scripts/check.sh` trước khi đẩy code
- chạy `./gradlew ktlintFormat` khi ktlint báo lỗi, không tắt quy tắc
- không cập nhật ảnh chuẩn screenshot ngoài plan

### [ADR-0012](adr/0012-firebase-wiring.md): Nối Firebase vào các module core, bản staging build bằng file cấu hình giả khi không có file thật

- không gọi Firebase từ module feature
- không sửa hay xóa file `google-services.json` giả, trừ khi đổi `applicationId` theo plan đã duyệt
- không tạo, đọc hay chép file `google-services.json` thật vào bất kỳ đâu

### [ADR-0013](adr/0013-ads-consent-and-guardrails.md): Quảng cáo AdMob chỉ khởi động sau hộp thoại đồng ý UMP, mã quảng cáo production đưa vào lúc build phát hành

- feature chỉ dùng `AdSlot`, `rememberShowInterstitial` và `PrivacyOptionsButton`
- không gọi `AdsController` hay SDK quảng cáo từ feature
- không viết mã quảng cáo thật vào code, tài liệu hay tham số build trong repo
- gọi quảng cáo toàn màn hình chỉ ở điểm dừng tự nhiên, không bao giờ lúc mở hay đóng app

### [ADR-0014](adr/0014-ads-default-values.md): Giá trị mặc định của tham số quảng cáo, thay bảng mặc định trong ADR-0013

- không đổi các giá trị mặc định này khi không có quyết định mới của người phụ trách sản phẩm
- vị trí mới vẫn mặc định tắt

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

## Quyết định riêng của app

### [ADR-1001](adr/1001-loading-timeout-15s.md): Màn loading của Camera Pose chờ tối đa 15 giây cho mỗi thứ cần tải, kể cả Remote Config

- ở app này, màn loading chờ tối đa 15 giây cho cấu hình, catalog và từng file của bộ khởi đầu
- các quy tắc khác của ADR-0018 vẫn áp dụng

### [ADR-1002](adr/1002-main-tu-hoi-phuc.md): Nhánh chính tự hồi phục khi đỏ: chạy lại test hỏng một lần, tự đóng issue khi chạy lại xanh, merge bản revert được kiểm bằng nội dung

- ở app này, `main-guard` chạy lại `check.sh` một lần khi chỉ có test thất bại và ghi issue `flaky` nếu lần chạy lại xanh
- khi `main-broken` mở, chỉ merge PR mà `scripts/is-exact-revert.sh` xác nhận là bản revert đúng của commit bị đổ lỗi
- phiên chỉ revert commit merge của chính phiên
- AI vẫn không đóng, sửa hay bình luận để gỡ issue `main-broken`
- các quy tắc khác của ADR-0010 vẫn áp dụng

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
