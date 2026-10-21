# THIẾT LẬP APP MỚI: TỪ LÚC TẠO REPO TỚI PHIÊN TỰ CHẠY ĐẦU TIÊN

Tài liệu này dành cho dev của mọi nhóm. Làm lần lượt từ trên xuống, mỗi phần có bước kiểm tra để biết đã xong. Chi tiết từng phần nằm ở các tài liệu được dẫn tới; tài liệu này là thứ tự và danh sách kiểm tra.

Quy ước: `<applicationId>` là tên gói của app, ví dụ `com.vglstudio.notes`. Bản staging có tên gói `<applicationId>.staging`.

AI không làm các bước trong tài liệu này. Mọi bước có bí mật (khóa, mật khẩu, mã kích hoạt) do người làm, và AI không bao giờ đọc hay in ra bí mật (ADR-0009).

## Tổng quan

| Phần | Việc | Ai làm | Cần cho |
| --- | --- | --- | --- |
| 1 | Chuẩn bị máy dev | Mỗi dev | Mọi việc |
| 2 | Tạo app từ dự án mẫu, tạo repo GitHub | Người phụ trách | Mọi việc |
| 3 | Thư mục staging trên R2 | Người phụ trách | Build staging, file tĩnh |
| 4 | Firebase staging và App Distribution | Người phụ trách | Bản staging cho nhóm test |
| 5 | Khóa ký staging, bí mật GitHub, chạy `staging` | Người phụ trách | Bản staging cho nhóm test |
| 6 | Routine Claude Code, chạy `start-run` | Người phụ trách tài khoản Claude | Phiên tự chạy |
| 7 | Phiên tự chạy đầu tiên | Dev chủ feature | |
| 8 | Phát hành production | Người phụ trách phát hành | Khi có bản đầu tiên lên Google Play |

## 1. Chuẩn bị máy dev

1. Cài Android Studio. Dùng JDK đi kèm (JDK 17 trở lên):
   - macOS: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
   - Mở Android Studio một lần để cài Android SDK. Đặt `export ANDROID_HOME=$HOME/Library/Android/sdk` (macOS) hoặc `$HOME/Android/Sdk` (Linux).
2. Cài `git`, `gh` (GitHub CLI), `jq`, `python3`. Trên macOS: `brew install gh jq`.
3. Đăng nhập GitHub CLI: `gh auth login`, chọn GitHub.com, HTTPS, đăng nhập bằng trình duyệt. `scripts/merge-if-green.sh` dùng phiên đăng nhập này.

Kiểm tra: trong thư mục repo, `scripts/check.sh` in `== Cổng chất lượng: QUA`.

Lưu ý: test trên emulator (`app/src/androidTest`) chạy trong CI trên API 34. Trên emulator API 37 đã gặp lỗi `InputManager.getInstance` của bản Espresso hiện tại; muốn chạy trên máy thì dùng emulator API 34.

## 2. Tạo app và repo GitHub

1. Tạo app từ dự án mẫu theo `docs/ops/new-app.md` của repo dự án mẫu (tài liệu đó chỉ có trong repo mẫu). Cần sẵn địa chỉ R2 staging ở phần 3; chưa có thì làm phần 3 trước.
2. Tạo repo **private** trong tổ chức trên GitHub, không tạo README hay `.gitignore` (app mới đã có).
3. Đẩy lên: `git remote add origin git@github.com:<tổ chức>/<repo>.git`, rồi `git push -u origin main`.
4. Trong **Settings > Actions > General** của repo, kiểm tra Actions được phép chạy.

Kiểm tra: tab **Actions** có workflow `main-guard` chạy cho commit đầu tiên và xanh. Nhãn `main-broken` được workflow tự tạo khi cần, không phải tạo tay.

Quy tắc: mọi PR vào `main` chỉ merge bằng `scripts/merge-if-green.sh <số PR>`, cả người lẫn AI (ADR-0010). Gói GitHub Free không chặn nút merge, nên đây là quy ước nhóm phải giữ.

## 3. Thư mục staging trên R2

App đọc file tĩnh từ một địa chỉ https công khai (ADR-0006, ADR-0015).

1. Trên Cloudflare, mục **R2**, tạo bucket cho app (hoặc dùng bucket chung của nhóm với một thư mục riêng cho app).
2. Trong **Settings** của bucket, mục **Public access**, nối một tên miền riêng (**Custom Domains**, ví dụ `<app>-staging.vgl.io.vn`). Không dùng địa chỉ `r2.dev` cho app thật.
3. Tải file lên theo đường dẫn có phiên bản, ví dụ `test/v1/hello.txt`. Không bao giờ ghi đè một file đã đưa lên; nội dung đổi thì dùng đường dẫn mới.
4. Địa chỉ gốc (kết thúc bằng `/`) chính là tham số `--staging-assets-url` của script tạo app, nằm trong `app/build.gradle.kts`.

Kiểm tra: `curl -I https://<địa chỉ gốc>/test/v1/hello.txt` trả `200`.

Khóa truy cập R2 không đưa vào repo hay GitHub. Bucket production làm tương tự khi phát hành (phần 8).

## 4. Firebase staging và App Distribution

Làm theo `docs/ops/staging-and-runs.md`, mục 1.1. Tóm tắt:

1. Tạo project Firebase riêng cho staging, thêm app Android `<applicationId>.staging`, tải `google-services.json`. Không chép file này vào repo; app đã có file giả để build (ADR-0012).
2. **Release & Monitor > App Distribution > Get started**, tạo nhóm người test `team`, mời thành viên.
3. Tạo tài khoản dịch vụ có vai trò **Firebase App Distribution Admin** và khóa JSON (các bước chi tiết ở mục 1.1).
4. Remote Config của project staging: thêm khóa khi cần, ví dụ `ads_<vị trí>_enabled = true` để thấy quảng cáo thử trên bản staging (vị trí mặc định tắt, ADR-0014). Cờ tính năng đang làm thì đã bật sẵn cho staging trong code, không cần thêm.

## 5. Khóa ký staging, bí mật GitHub, chạy `staging`

Làm theo `docs/ops/staging-and-runs.md`, mục 1.2 đến 1.4: tạo khóa ký staging, thêm 5 bí mật, chạy **Actions > staging** trên `main`.

Kiểm tra: workflow `staging` xanh, người trong nhóm `team` nhận email mời cài bản staging.

## 6. Routine Claude Code

Làm theo `docs/ops/staging-and-runs.md`, mục 2.1 và 2.2: cài Claude GitHub App, tạo môi trường cloud, tạo routine, tạo mã kích hoạt, thêm 2 bí mật.

Điểm hay bị bỏ sót:

- Setup script của môi trường là toàn bộ nội dung `scripts/cloud-setup.sh`. Mỗi khi file này đổi trên `main`, dán lại vào môi trường. Thiếu bản mới, Gradle trong phiên tải thẳng từ Maven Central và có thể bị chặn (lỗi 429).
- Prompt của routine chép nguyên văn, kể cả dòng "Nhánh".
- Gỡ hết connector của routine.
- Model: Opus 5.5 cho phiên chạy qua đêm (Sonnet trừ hạn mức chậm hơn nhưng dễ sai hơn với task rủi ro cao).

Kiểm tra: bấm **Run now** trên routine. Khi chưa có danh sách chạy `approved`, phiên chỉ kiểm tra đầu phiên rồi kết thúc và báo lý do. Phiên đó phải chạy được `scripts/check.sh`.

Sau đó tạo routine kiểm tra môi trường hằng ngày và hai bí mật của nó (`docs/ops/staging-and-runs.md`, mục 2.4), chạy thử workflow `env-canary`.

## 6b. Hệ thống thiết kế trên công cụ thiết kế

Khi app có feature cần thiết kế: dựng hệ thống thiết kế trên Claude Design hoặc Figma từ ảnh catalog và `docs/design-system/tokens.json` của app, theo `docs/ops/design-workflow.md`, mục 1. Trước đó, đổi màu thương hiệu của app trong `:core:designsystem` (plan riêng).

## 7. Phiên tự chạy đầu tiên

Quy trình đầy đủ ở `docs/README.md` và `docs/auto-run.md`. Thứ tự một vòng:

1. Viết yêu cầu (`docs/inbox/` hoặc `docs/features/<feature>/requests/`). AI sinh spec `draft`, người phụ trách sản phẩm trả lời câu hỏi và chuyển sang `confirmed`.
2. AI sinh ticket và plan. Người duyệt trả lời câu hỏi ở mục 8 của plan, điền mục 9, chuyển plan và ticket sang `approved`. Plan rủi ro cao cần người duyệt chéo.
3. AI lập danh sách chạy `docs/runs/RUN-YYYYMMDD-NN.md`. Dev chuyển sang `approved`, điền `approved_by`, merge.
4. **Actions > start-run > Run workflow**, nhánh `main`, nhập đường dẫn danh sách chạy. Phần tóm tắt của workflow có đường dẫn tới phiên.
5. Trong lúc phiên chạy, không merge gì vào `main`.
6. Sáng hôm sau, đọc báo cáo trong danh sách chạy (đã lên `main` qua PR báo cáo của phiên), xem ảnh chuẩn screenshot mới, làm các kiểm tra thủ công trên bản staging.
7. Retro 15 phút: quyết định các bài học mới trong `docs/lessons.md`, nhờ AI làm các bài học được nhận thành PR cải tiến (`docs/auto-run.md`, mục 6).

## 8. Phát hành production

Khi có bản đầu tiên lên Google Play: `docs/ops/release.md` (khóa upload, Firebase production, AdMob, R2 production, Play Console).

## Sự cố đã gặp và cách xử lý

| Dấu hiệu | Nguyên nhân | Xử lý |
| --- | --- | --- |
| Phiên tự chạy kết thúc vì Gradle nhận 429 từ `repo.maven.apache.org` | Môi trường cloud dùng setup script cũ, chưa có mirror | Dán lại `scripts/cloud-setup.sh` vào môi trường (phần 6), lập danh sách chạy mới |
| Phiên nhận 429 từ `repo1.maven.org` khi chạy test Robolectric | Môi trường dùng setup script cũ, chưa có mirror cho Robolectric | Dán lại `scripts/cloud-setup.sh` vào môi trường (phần 6) |
| Phiên kết thúc ngay đầu phiên, báo cáo ghi "Môi trường cloud chưa dùng scripts/cloud-setup.sh mới nhất" | `scripts/cloud-env-check.sh` thấy số phiên bản setup script của môi trường khác nhánh chính | Dán lại `scripts/cloud-setup.sh` vào môi trường (phần 6), lập danh sách chạy mới |
| Gradle báo `Could not find ...kotlin-stdlib...` khi build `build-logic`, chỉ tìm ở `plugins.gradle.org` | Setup script phiên bản 1: mirror chưa phủ thư viện tải qua plugin portal | Dán lại `scripts/cloud-setup.sh` (phiên bản 2 trở lên) |
| `start-run` báo môi trường cloud đang lỗi (issue `env-broken`) | Kiểm tra môi trường hằng ngày đỏ | Đọc log trong issue, sửa (thường là dán lại `scripts/cloud-setup.sh`), chạy lại workflow `env-canary` |
| Gradle báo "SDK location not found" trong môi trường cloud | Setup script phiên bản 2 trở xuống, và lệnh chạy từ shell không đọc `~/.bashrc` | Dán lại `scripts/cloud-setup.sh` (phiên bản 3 trở lên ghi `sdk.dir` vào `local.properties`) |
| `merge-if-green.sh` báo PR mang nhãn `needs-human` | PR do phiên tự chạy soạn sẵn khi task dừng | Xem PR, sửa nếu cần, gỡ nhãn, đổi sang sẵn sàng rồi merge |
| `merge-if-green.sh` báo "phiên ... đang chạy" | Có phiên tự chạy đang chạy; chỉ PR của phiên đó được merge | Chờ phiên kết thúc |
| Phiên làm việc trên nhánh `claude/<tên ngẫu nhiên>` | Prompt routine thiếu dòng "Nhánh" | Chép lại prompt ở `docs/ops/staging-and-runs.md`, mục 2.1 |
| `start-run` báo lỗi 401 | Mã kích hoạt sai hoặc đã tạo lại | Tạo mã mới trong routine, cập nhật `ROUTINE_FIRE_TOKEN` |
| `start-run` báo danh sách chưa `approved` hoặc nhánh chính đỏ | Đúng thiết kế | Duyệt danh sách chạy; hoặc xử lý issue `main-broken` trước |
| `staging` báo thiếu bí mật hoặc "file cấu hình giả" | Chưa thêm đủ bí mật, hoặc dán nhầm file giả | Xem bảng bí mật ở mục 1.3 của `staging-and-runs.md` |
| `ui-test` đỏ ở test có màn hình vừa mở | Hộp thoại đồng ý quảng cáo phủ lên app mới cài trên staging | Test chỉ kiểm tra màn hình có trong cây giao diện (`CLAUDE.md`, bước 10) |
| Không biết vì sao `ui-test` đỏ | | Đọc bước "Chi tiết test trên emulator" của job, hoặc tải artifact `ui-test-report` (ADR-0019) |
| Cài bản build mới lên máy báo `INSTALL_FAILED_VERSION_DOWNGRADE` | Máy đang có bản staging từ App Distribution (`versionCode` lớn hơn) | Gỡ app staging trên máy rồi cài lại |
| Xem app đã làm gì trên máy | | `adb logcat -v threadtime \| grep ' VGL\.'` (ADR-0019) |
