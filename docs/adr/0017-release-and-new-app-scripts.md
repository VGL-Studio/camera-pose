---
id: ADR-0017
title: Script phát hành chỉ build bản ký, người tải lên Google Play; script khởi tạo chép dự án mẫu thành repo mới
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-15
deciders: [Nhóm phát triển]
---

# ADR-0017: SCRIPT PHÁT HÀNH CHỈ BUILD BẢN KÝ, NGƯỜI TẢI LÊN GOOGLE PLAY; SCRIPT KHỞI TẠO CHÉP DỰ ÁN MẪU THÀNH REPO MỚI

## Bối cảnh

ADR-0009 quyết định phát hành production từ máy của người phụ trách bằng `scripts/release.sh`, với cấu hình production cất trong trình quản lý mật khẩu của nhóm. Kế hoạch hoàn thiện dự án mẫu (phần 5) còn yêu cầu một script tạo app mới từ dự án mẫu. ADR này ghi cách làm cụ thể của hai script. Hướng dẫn từng bước nằm ở `docs/ops/release.md` và `docs/ops/new-app.md`.

## Quyết định

**`scripts/release.sh`:**

- Cấu hình production được người chép từ trình quản lý mật khẩu vào một thư mục ngoài repo (mặc định `~/.vgl-release/<applicationId>/`): `google-services.json`, khóa upload `upload.jks`, và `release.properties` gồm mã ứng dụng AdMob, mã quảng cáo từng vị trí, địa chỉ R2 production, tên khóa upload. Script không gọi CLI của trình quản lý mật khẩu nào. Mật khẩu khóa upload được hỏi khi chạy, không ghi vào thư mục.
- Script chỉ build bản ký (Android App Bundle). Người tải lên Google Play bằng tay, vào kênh Internal testing trước, rồi chuyển sang Production trên Play Console. Kế hoạch ban đầu ghi "đẩy lên Google Play"; nhóm chọn tải lên tay để không cần thêm tài khoản dịch vụ Google Play hay plugin Gradle.
- Chỉ build commit nằm trên `main` mà `main-guard.yml` đã báo xanh. Commit chỉ sửa tài liệu không có lần chạy `main-guard.yml` riêng, nên script dùng kết quả của commit có code gần nhất bên dưới (theo nhánh chính, `--first-parent`). Script từ chối khi có issue `main-broken` đang mở.
- Từ chối trước khi build nếu: thiếu file, thư mục phát hành nằm trong repo, có mã thử của Google, địa chỉ R2 không phải https, `google-services.json` không có `applicationId` của commit, hoặc một vị trí trong `AdPlacement` không có mã quảng cáo (ADR-0013).
- Build trong một git worktree sạch của commit đó, nên thay đổi chưa commit trên máy không lọt vào bản phát hành. Giá trị production đi qua biến môi trường của Gradle, không hiện trong danh sách tiến trình.
- `versionCode` và `versionName` nằm trong `app/build.gradle.kts`, tăng bằng PR thường. Sau khi build, script gắn và đẩy tag `release/<versionCode>`; script từ chối `versionCode` đã có tag.
- Bản production ký bằng khóa upload qua `signingConfigs.upload`, chỉ có khi script truyền khóa vào. Google Play ký lại bằng khóa ký app (Play App Signing, ADR-0007).

**`scripts/new-app.sh`:**

- Tạo app mới ở một thư mục mới, thành repo git riêng với một commit đầu tiên ghi mã commit của dự án mẫu. Chỉ chép file đã commit (`git archive`), nên file build và cấu hình trên máy không bị chép theo.
- Đổi tên gói và `applicationId` (một giá trị), tên app, tên project Gradle, địa chỉ R2 staging. Đổi cả `google-services.json` giả của staging và `STAGING_APPLICATION_ID` trong `staging.yml`. ADR giữ nguyên văn.
- Bỏ `TEMPLATE-ROADMAP.md`, `docs/ops/new-app.md` và chính script này.
- Giữ `:feature:sample`, vì `CLAUDE.md` hướng dẫn mọi feature làm theo mẫu này. Feature mẫu được xóa bằng một ticket khi feature thật đầu tiên thay màn hình bắt đầu.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Đọc thư mục do người chép ra | Không phụ thuộc trình quản lý mật khẩu nào | Người phải chép tay và xóa thư mục sau khi phát hành |
| Gọi CLI của 1Password hoặc Bitwarden | Không có file production nằm trên đĩa | Buộc cả nhóm dùng một trình quản lý mật khẩu có CLI |
| Tải lên tay trên Play Console | Không cần tài khoản dịch vụ Google Play, không thêm plugin | Thêm một bước tay mỗi lần phát hành |
| Gradle Play Publisher | Tải lên tự động | Thêm plugin vào build, thêm một khóa tài khoản dịch vụ cần giữ |
| Script khởi tạo xóa luôn feature mẫu | App mới gọn ngay từ đầu | Phải sửa test của core và màn hình bắt đầu bằng script; `CLAUDE.md` mất feature mẫu để chỉ tới |

## Hệ quả

- **Tích cực:** cấu hình production chỉ có trên máy người phát hành trong lúc phát hành; một bản phát hành luôn khớp một commit đã xanh của `main` và có tag tương ứng; app mới build được ngay sau khi khởi tạo.
- **Phải chấp nhận:** người phát hành phải chép thư mục phát hành và xóa nó sau đó; tải lên Google Play là bước tay; lần build phát hành cần mạng để Crashlytics tải file mapping lên project production.
- **Việc AI phải tuân thủ:** không chạm vào `scripts/release.sh` (ADR-0009); không đọc, tạo hay sửa thư mục phát hành; khi đổi tên một file mà `scripts/new-app.sh` sửa (README, CLAUDE.md, `strings.xml`, `settings.gradle.kts`, `app/build.gradle.kts`), cập nhật script và test của nó trong cùng PR.
