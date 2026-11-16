---
id: ADR-0049
title: Phiên tự chạy dùng Claude Code routine, kích hoạt qua start-run.yml bằng tay hoặc tự động khi danh sách start auto được duyệt lên nhánh chính; staging build trên GitHub Actions bằng khóa staging riêng; phát hành production ngoài GitHub bằng script chỉ build bản ký; script khởi tạo chép dự án mẫu thành repo mới
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-11-12
deciders: [Nhật]
supersedes: [ADR-0009, ADR-0016, ADR-0017]   # ADR hợp nhất (ADR-0044): chép mọi quy tắc của ba ADR, chỉ đổi cách chạy start-run.yml (mục 1, dòng start-run.yml của bảng mục 6, mục 7)
superseded_by:
---

# ADR-0049: PHIÊN TỰ CHẠY DÙNG CLAUDE CODE ROUTINE, KÍCH HOẠT QUA START-RUN.YML BẰNG TAY HOẶC TỰ ĐỘNG; STAGING TRÊN GITHUB ACTIONS; PHÁT HÀNH PRODUCTION NGOÀI GITHUB; SCRIPT PHÁT HÀNH VÀ SCRIPT KHỞI TẠO

## Bối cảnh

ADR-0009 chọn hạ tầng của phiên tự chạy (routine, tài khoản, môi trường cloud, nơi kiểm thử, tên nhánh, các workflow, phát hành production). ADR-0016 ghi cách làm cụ thể của hai workflow dùng bí mật, `staging.yml` và `start-run.yml`. Cả hai ghi `start-run.yml` là workflow **chạy tay**.

Đo ngày 2026-11-12 (yêu cầu `features/quy-trinh/requests/2026-11-12-giam-thoi-gian-cho.md`): từ ticket tới merge mất từ 4 giờ tới hơn 200 giờ, phần lớn là chờ người. Một trong các khoảng chờ là danh sách chạy đã duyệt và merge vẫn nằm chờ dev quay lại bấm **Actions > start-run > Run workflow**. Ticket T-104 cho phiên tự bắt đầu khi danh sách được duyệt lên nhánh chính, giữ cách bấm tay cho người muốn chọn giờ (ví dụ chờ hạn mức được làm mới).

Việc này đổi một quy tắc của cả ADR-0009 lẫn ADR-0016. Theo ADR-0044, ADR sửa một phần ADR còn hiệu lực phải hợp nhất, nên ADR này chép nguyên mọi quy tắc khác của hai ADR và chỉ đổi đoạn về cách chạy `start-run.yml`. ADR-0017 (cách làm của `scripts/release.sh` và `scripts/new-app.sh`) dẫn ADR-0009 ngay trong dòng "Việc AI phải tuân thủ"; ADR `accepted` không sửa được, và tài liệu quy tắc không được dẫn ADR đã bị thay, nên ADR này chép nguyên cả ADR-0017 (mục 11, 12), không đổi quy tắc nào của nó.

Bối cảnh gốc của ADR-0009 vẫn đúng: tổ chức dùng gói GitHub Free. Với repo private trên gói này, GitHub không có environment, bí mật gắn với environment hay người duyệt bắt buộc. Mọi bí mật đặt trên GitHub đều là bí mật chung của repo, và workflow chạy trên bất kỳ nhánh nào trong repo đều đọc được. Nghĩa là AI chỉ cần đẩy một workflow lên nhánh của mình là có thể chạm tới mọi bí mật đặt trên GitHub.

API kích hoạt routine không chống trùng: mỗi lần gọi thành công là một phiên mới. Bấm tay thì người thấy phiên trước còn chạy hay không; tự chạy thì không có người nhìn, nên workflow phải tự chặn việc mở hai phiên.

## Quyết định

**1. Nơi chạy phiên tự chạy: Claude Code routine** trên hạ tầng cloud của Anthropic.

- Routine có hai cách kích hoạt: **Run now** trên giao diện, và **kích hoạt qua API**. Không đặt lịch lặp cố định.
- Workflow `start-run.yml` trên GitHub gọi API của routine, chạy tay hoặc tự chạy theo mục 7. Nhờ vậy cả hai dev khởi động được phiên từ GitHub, không cần đăng nhập tài khoản Claude sở hữu routine. Mã kích hoạt của routine được lưu thành bí mật chung của repo.
- Prompt của routine yêu cầu AI thực hiện danh sách chạy đã duyệt theo `docs/auto-run.md`.

**2. Tài khoản: dùng tài khoản Claude và tài khoản GitHub hiện có** của người phụ trách, trong giai đoạn chạy thử với các app đầu tiên.

- Routine thuộc tài khoản Claude hiện có, dùng chung hạn mức với việc dùng Claude hằng ngày của tài khoản đó.
- Commit và PR do AI tạo mang tên tài khoản GitHub hiện có. Việc của AI được nhận biết qua ba dấu hiệu: nhánh có tiền tố `claude/`, commit có dòng `Co-Authored-By: Claude`, mô tả PR có đường dẫn tới phiên chạy.
- Chuyển sang tài khoản riêng khi quy trình đã ổn định, hoặc khi phiên thường xuyên dừng vì hết hạn mức. Việc chuyển chỉ cần tạo routine trên tài khoản mới và đổi mã kích hoạt trong `start-run.yml`, không phải sửa gì khác trong repo.

**3. Môi trường cloud của routine:**

- Mức mạng **Custom**: giữ danh sách tên miền mặc định, thêm `dl.google.com` để tải Android SDK.
- Setup script cài Android SDK (command-line tools, platform, build-tools). JDK 21 và Gradle có sẵn.
- Biến môi trường tăng thời gian chờ lệnh (`BASH_DEFAULT_TIMEOUT_MS`, `BASH_MAX_TIMEOUT_MS`) vì build Android có thể vượt 2 phút.
- Không đưa vào môi trường bất kỳ cấu hình production nào.

**4. Kiểm thử chia hai nơi:**

- Trong phiên tự chạy: unit test, test giao diện bằng Robolectric, screenshot test bằng Roborazzi. Các test này chạy trên JVM, không cần emulator.
- Trong GitHub Actions: toàn bộ kiểm tra trên, thêm test trên emulator.

**5. Tên nhánh:** `claude/T-NNN-<tu-khoa>`, khớp với tiền tố mặc định của routine.

**6. GitHub Actions chỉ dùng cho kiểm tra và staging:**

| Workflow | Khi nào chạy | Việc làm | Bí mật dùng |
| --- | --- | --- | --- |
| `pr-check.yml` | Mọi PR vào nhánh chính | Cổng chất lượng (xem ADR-0010) | Không |
| `main-guard.yml` | Mỗi lần nhánh chính thay đổi | Kiểm tra lại nhánh chính, xử lý khi đỏ (xem ADR-0010) | Không |
| `staging.yml` | Cuối phiên tự chạy, hoặc dev chạy tay | Build biến thể staging, phát qua Firebase App Distribution | Cấu hình staging |
| `start-run.yml` | Dev chạy tay, hoặc tự chạy khi lần push lên nhánh chính chuyển một danh sách `start: auto` sang `approved` (mục 7) | Kích hoạt phiên tự chạy qua API của routine | Mã kích hoạt routine |

Bí mật trên GitHub chỉ gồm cấu hình staging và mã kích hoạt routine. Nếu bị lộ, thiệt hại giới hạn trong môi trường staging và hạn mức sử dụng Claude.

**7. `start-run.yml`:**

- **Hai cách chạy.**
  - Chạy tay, nhận đường dẫn danh sách chạy `docs/runs/RUN-YYYYMMDD-NN.md`.
  - Tự chạy khi có push lên nhánh chính đổi `docs/runs/RUN-*.md`. Workflow chỉ chọn danh sách mà chính lần push đó chuyển sang `approved` (ở commit trước chưa có file hay khác `approved`), có `start: auto` và `open_with: routine`. Lần push sau đổi cùng file không kích hoạt lại. Danh sách ghi `start: manual` hay không ghi trường `start` thì chỉ chạy tay.
- Từ chối nếu danh sách không có trên nhánh chính, chưa `approved`, hoặc có issue `main-broken` đang mở.
- **Chống mở hai phiên**, cho cả hai cách chạy: từ chối khi danh sách trên đầu nhánh chính không còn `approved`, khi danh sách đã có nhánh báo cáo, khi một danh sách khác đang `running`, hay khi một lần chạy `start-run.yml` khác đang kích hoạt hoặc vừa kích hoạt routine cho một danh sách chưa có nhánh báo cáo. Kiểm ngay trước lời gọi API, trong job nằm trong hàng chờ `start-run` mà `resume-run.yml` cũng dùng. Lần chạy tay có ô `force` để bỏ qua điều "vừa kích hoạt" khi dev biết chắc phiên trước không bắt đầu, và để mở lại phiên của chính danh sách đó khi nhánh báo cáo của nó đang `running`; `force` không bỏ qua hai điều "danh sách khác đang `running`" và "không còn `approved` trên nhánh chính".
- Lần chạy bị từ chối thì đỏ và ghi lý do trong tóm tắt của job, để dev không tưởng phiên đã chạy.
- Gọi `POST <ROUTINE_FIRE_URL>` với mã kích hoạt, gửi đường dẫn danh sách chạy trong trường `text`. Prompt của routine chỉ lấy đường dẫn từ đó (nội dung `text` được Anthropic đánh dấu là dữ liệu không tin cậy).
- Không bao giờ tự thử lại: API không chống trùng, mỗi lần gọi thành công là một phiên mới.
- Đường dẫn tới phiên ghi vào phần tóm tắt của workflow.
- Với `start: auto`, lần merge danh sách `approved` là lần kích hoạt. Lời duyệt danh sách phải gồm việc bắt đầu phiên khi merge; không thì danh sách ghi `start: manual`.

**8. `staging.yml`:**

- Chạy tay (dev, hoặc phiên tự chạy ở cuối phiên), luôn từ nhánh chính. Mỗi lúc chỉ một lần chạy, lần sau chờ lần trước.
- Build `stagingRelease`: thu gọn bằng R8 như bản production, nên lỗi do R8 lộ ra ở staging trước khi phát hành.
- Ký bằng một khóa chỉ dành cho staging, giữ cố định giữa các bản, để bản mới cài đè bản cũ mà người test không mất dữ liệu. Khóa là bí mật của repo; không có khóa thì `stagingRelease` không được ký (build trên máy vẫn chạy).
- Số build: `versionCode` là số thứ tự lần chạy của workflow, tên phiên bản có hậu tố `-staging.<số>`.
- File cấu hình Firebase staging thật được ghi vào `app/src/staging/release/` (ADR-0012). Workflow từ chối nếu bí mật vẫn là file giả.
- Phát bằng `firebase-tools` (phiên bản cố định) qua tài khoản dịch vụ, tới nhóm người test trong biến `STAGING_TESTER_GROUPS` (mặc định `team`). Không thêm plugin Gradle.
- Không ghi bộ nhớ đệm Gradle, vì PR đọc lại bộ nhớ đệm đó. Bí mật được xóa khỏi máy chạy ở cuối.

**9. Bí mật của repo** (đều thuộc staging hoặc routine, đúng phạm vi của mục 6):

| Bí mật | Dùng ở |
| --- | --- |
| `STAGING_GOOGLE_SERVICES_JSON` | `staging.yml` |
| `STAGING_APP_DISTRIBUTION_CREDENTIALS` | `staging.yml` |
| `STAGING_KEYSTORE_BASE64`, `STAGING_KEYSTORE_PASSWORD`, `STAGING_KEY_ALIAS` | `staging.yml` |
| `ROUTINE_FIRE_URL`, `ROUTINE_FIRE_TOKEN` | `start-run.yml` |

**10. Phát hành production ngoài GitHub.**

- Người phụ trách phát hành build bản ký và đẩy lên Google Play từ máy của mình, bằng script `scripts/release.sh` trong repo.
- Khóa upload, file cấu hình Firebase production, mã quảng cáo thật và khóa R2 production được cất trong trình quản lý mật khẩu của nhóm. Chúng không bao giờ nằm trong repo hay trên GitHub.
- Script phát hành chỉ build từ commit đã có trên nhánh chính và đã qua `main-guard.yml`.

**11. `scripts/release.sh`:**

- Cấu hình production được người chép từ trình quản lý mật khẩu vào một thư mục ngoài repo (mặc định `~/.vgl-release/<applicationId>/`): `google-services.json`, khóa upload `upload.jks`, và `release.properties` gồm mã ứng dụng AdMob, mã quảng cáo từng vị trí, địa chỉ R2 production, tên khóa upload. Script không gọi CLI của trình quản lý mật khẩu nào. Mật khẩu khóa upload được hỏi khi chạy, không ghi vào thư mục.
- Script chỉ build bản ký (Android App Bundle). Người tải lên Google Play bằng tay, vào kênh Internal testing trước, rồi chuyển sang Production trên Play Console. Kế hoạch ban đầu ghi "đẩy lên Google Play"; nhóm chọn tải lên tay để không cần thêm tài khoản dịch vụ Google Play hay plugin Gradle.
- Chỉ build commit nằm trên `main` mà `main-guard.yml` đã báo xanh. Commit chỉ sửa tài liệu không có lần chạy `main-guard.yml` riêng, nên script dùng kết quả của commit có code gần nhất bên dưới (theo nhánh chính, `--first-parent`). Script từ chối khi có issue `main-broken` đang mở.
- Từ chối trước khi build nếu: thiếu file, thư mục phát hành nằm trong repo, có mã thử của Google, địa chỉ R2 không phải https, `google-services.json` không có `applicationId` của commit, hoặc một vị trí trong `AdPlacement` không có mã quảng cáo (ADR-0013).
- Build trong một git worktree sạch của commit đó, nên thay đổi chưa commit trên máy không lọt vào bản phát hành. Giá trị production đi qua biến môi trường của Gradle, không hiện trong danh sách tiến trình.
- `versionCode` và `versionName` nằm trong `app/build.gradle.kts`, tăng bằng PR thường. Sau khi build, script gắn và đẩy tag `release/<versionCode>`; script từ chối `versionCode` đã có tag.
- Bản production ký bằng khóa upload qua `signingConfigs.upload`, chỉ có khi script truyền khóa vào. Google Play ký lại bằng khóa ký app (Play App Signing, ADR-0007).

**12. `scripts/new-app.sh`:**

- Tạo app mới ở một thư mục mới, thành repo git riêng với một commit đầu tiên ghi mã commit của dự án mẫu. Chỉ chép file đã commit (`git archive`), nên file build và cấu hình trên máy không bị chép theo.
- Đổi tên gói và `applicationId` (một giá trị), tên app, tên project Gradle, địa chỉ R2 staging. Đổi cả `google-services.json` giả của staging và `STAGING_APPLICATION_ID` trong `staging.yml`. ADR giữ nguyên văn.
- Bỏ `TEMPLATE-ROADMAP.md`, `docs/ops/new-app.md` và chính script này.
- Giữ `:feature:sample`, vì `CLAUDE.md` hướng dẫn mọi feature làm theo mẫu này. Feature mẫu được xóa bằng một ticket khi feature thật đầu tiên thay màn hình bắt đầu.

ADR-0044 (mục 5, mục 7) bổ sung những thứ `scripts/new-app.sh` bỏ hay làm trống khi tạo app, kể cả ADR `superseded`; quy tắc đó nằm ở ADR-0044.

## Các phương án đã cân nhắc

Cho cách bắt đầu phiên (mới trong ADR này):

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Tự chạy khi lần push chuyển danh sách `start: auto` sang `approved`, chống mở hai phiên trong workflow, giữ chạy tay (đã chọn) | Bỏ một lần dev phải quay lại; ai muốn chọn giờ vẫn ghi `manual` | Workflow phức tạp hơn; các đường mở hai phiên phải có test |
| Giữ chạy tay | Đơn giản, người thấy phiên trước | Danh sách đã duyệt có thể nằm chờ nhiều ngày |
| Tự chạy mọi danh sách `approved` chưa có nhánh báo cáo | Không cần đọc commit trước | Nhánh báo cáo bị xóa sau merge, nên danh sách cũ bị mở lại ở một lần push về sau |
| Tự chạy chỉ khi không có phiên nào trong 24 giờ qua | Ít đường mở hai phiên | Danh sách thứ hai trong ngày vẫn phải bấm tay |
| Hẹn giờ bằng lịch của routine | Không sửa workflow | Không qua các kiểm tra của `start-run.yml` (`main-broken`, thiết lập staging, chống mở hai phiên) |

Cho hạ tầng (giữ từ ADR-0009):

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Routine trên cloud của Anthropic | Không cần máy riêng, dùng hạn mức gói Claude, kích hoạt linh hoạt | Môi trường giới hạn tài nguyên, không có emulator |
| Claude Code chạy trong GitHub Actions | Gần CI | Tính phí theo token qua API, tốn thêm phút Actions |
| Tài khoản riêng cho phiên tự chạy | Lịch sử tách bạch, hạn mức riêng | Tốn thêm chỗ ngồi và công quản lý tài khoản |
| Phát hành production bằng GitHub Actions | Tự động hoàn toàn | Trên gói Free, khóa phát hành thành bí mật chung, AI chạm tới được |

Cho bản staging (giữ từ ADR-0016):

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| `stagingRelease` ký khóa staging trong bí mật | Gần bản production; cài đè được | Thêm ba bí mật |
| Commit keystore staging vào repo | Không cần bí mật | Ai đọc được repo cũng ký được bản staging giả |
| `stagingDebug` ký khóa debug ngẫu nhiên của máy CI | Không cần gì thêm | Mỗi bản có chữ ký khác, người test phải gỡ app và mất dữ liệu |
| Plugin Gradle của App Distribution | Gọn trong Gradle | Thêm plugin vào build của mọi bản |

Cho script phát hành và script khởi tạo (giữ từ ADR-0017):

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Đọc thư mục do người chép ra | Không phụ thuộc trình quản lý mật khẩu nào | Người phải chép tay và xóa thư mục sau khi phát hành |
| Gọi CLI của 1Password hoặc Bitwarden | Không có file production nằm trên đĩa | Buộc cả nhóm dùng một trình quản lý mật khẩu có CLI |
| Tải lên tay trên Play Console | Không cần tài khoản dịch vụ Google Play, không thêm plugin | Thêm một bước tay mỗi lần phát hành |
| Gradle Play Publisher | Tải lên tự động | Thêm plugin vào build, thêm một khóa tài khoản dịch vụ cần giữ |
| Script khởi tạo xóa luôn feature mẫu | App mới gọn ngay từ đầu | Phải sửa test của core và màn hình bắt đầu bằng script; `CLAUDE.md` mất feature mẫu để chỉ tới |

## Hệ quả

- **Tích cực:**
  - Không tốn thêm chi phí tài khoản; cấu hình production hoàn toàn nằm ngoài tầm với của AI; cả nhóm khởi động được phiên tự chạy.
  - Người test cài đè bản mới; lỗi R8 lộ ra ở staging; dev bắt đầu phiên tự chạy từ GitHub mà không cần tài khoản Claude của routine.
  - Với `start: auto`, một lời duyệt danh sách đủ để phiên bắt đầu, không cần dev quay lại bấm.
  - Cấu hình production chỉ có trên máy người phát hành trong lúc phát hành; một bản phát hành luôn khớp một commit đã xanh của `main` và có tag tương ứng; app mới build được ngay sau khi khởi tạo.
- **Phải chấp nhận:**
  - Phiên tự chạy và việc dùng Claude hằng ngày của chủ tài khoản cùng trừ vào một hạn mức; PR của AI mang tên người; phát hành production cần một người thao tác trên máy có cấu hình production.
  - Người phát hành phải chép thư mục phát hành và xóa nó sau đó; tải lên Google Play là bước tay; lần build phát hành cần mạng để Crashlytics tải file mapping lên project production.
  - Lộ khóa staging thì ai đó ký được bản staging giả; thiệt hại giới hạn trong nhóm người test.
  - Trên gói GitHub Free, workflow chạy trên bất kỳ nhánh nào cũng đọc được bí mật. Điều kiện "chỉ chạy từ nhánh chính" trong workflow chỉ chặn việc chạy nhầm, không chặn được một workflow bị sửa trên nhánh khác.
  - API kích hoạt routine đang thử nghiệm; khi Anthropic đổi cách gọi, phải sửa `start-run.yml`.
  - Chưa kiểm chứng phiên tự chạy có quyền khởi động `staging.yml` hay không (`docs/ops/staging-and-runs.md`, mục 3).
  - Phần tự chạy của mục 7 có hiệu lực từ khi PR làm T-104 merge; trước đó `start-run.yml` chỉ chạy tay và trường `start` chưa có tác dụng.
  - Mỗi push đổi danh sách chạy làm `start-run.yml` chạy khoảng 1 phút, kể cả khi không chọn được danh sách nào.
  - Chống mở hai phiên dựa vào lịch sử các lần chạy trên GitHub; một số trường hợp hẹp (GitHub không phát sự kiện push, lần chờ trong hàng bị thay và thành `cancelled`) cần dev bấm tay, và dev đọc lý do trong tóm tắt của lần chạy đỏ.
  - Khi ADR được chấp nhận, ADR-0009, ADR-0016 và ADR-0017 chuyển `superseded`, và chính PR chuyển trạng thái đó đổi các tài liệu quy tắc đang dẫn chúng sang ADR này (không thì `scripts/history-refs.py` báo đỏ): `CLAUDE.md` (mục Không được làm, hai chỗ), `docs/auto-run.md` (ba chỗ), `docs/ops/setup-new-app.md`, `docs/ops/release.md`, `docs/ops/new-app.md`, `docs/ops/staging-and-runs.md` (hai chỗ), `docs/ops/self-hosted-runner.md`. Chú thích trong `scripts/cloud-setup.sh` và `scripts/new-app.sh` sửa theo; chú thích trong `scripts/release.sh` giữ nguyên vì AI không chạm file đó, chú thích trong `.github/workflows/staging.yml` và `app/build.gradle.kts` giữ nguyên vì sửa chúng cần plan rủi ro cao hay không đáng một PR code; chú thích ở dòng đầu `.github/workflows/start-run.yml` đổi trong PR làm T-104, vì PR đó viết lại chú thích đầu file. Chú thích trong code không thuộc phần kiểm của ADR-0044. Nếu lúc đó ADR-0048 còn `proposed`, sửa chỗ dẫn ADR-0009 trong C1 của nó. Phần đổi cách chạy `start-run.yml` vào tài liệu cùng PR làm T-104.
  - ADR `accepted` khác còn nhắc ADR-0009 hay ADR-0016 trong phần Quyết định (ví dụ ADR-0007, ADR-0012, ADR-0017) không sửa; người đọc theo `superseded_by` sang ADR này.
- **Việc AI phải tuân thủ:** đặt tên nhánh theo `claude/T-NNN-<tu-khoa>`; ghi đường dẫn phiên chạy vào mô tả PR; không cố cài hay chạy emulator trong phiên; không đọc, in ra hay chuyển đi bất kỳ bí mật nào của repo; không viết workflow mới có dùng bí mật, và không đổi `staging.yml` hay `start-run.yml`, trừ khi đó chính là plan rủi ro cao đã được duyệt chéo; không chạm vào `scripts/release.sh`; không đọc, tạo hay sửa thư mục phát hành; khi đổi tên một file mà `scripts/new-app.sh` sửa (README, CLAUDE.md, `strings.xml`, `settings.gradle.kts`, `app/build.gradle.kts`), cập nhật script và test của nó trong cùng PR; cuối phiên tự chạy chỉ khởi động `staging.yml` trên nhánh chính; danh sách chạy ghi `start: auto` chỉ khi lời duyệt của dev gồm việc bắt đầu phiên khi merge, không thì ghi `start: manual`, và AI không merge danh sách `start: auto` khi lời duyệt chưa gồm điều đó; không dùng Run now hay lịch hẹn của routine cho danh sách `start: auto`; không "Re-run" một lần chạy `start-run.yml` đã gọi API kích hoạt.
