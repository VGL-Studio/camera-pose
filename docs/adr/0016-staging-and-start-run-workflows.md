---
id: ADR-0016
title: Bản staging build bằng biến thể release ký khóa staging riêng, phiên tự chạy kích hoạt qua API của routine
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-15
deciders: [Nhóm phát triển]
---

# ADR-0016: BẢN STAGING BUILD BẰNG BIẾN THỂ RELEASE KÝ KHÓA STAGING RIÊNG, PHIÊN TỰ CHẠY KÍCH HOẠT QUA API CỦA ROUTINE

## Bối cảnh

ADR-0009 quyết định có hai workflow dùng bí mật: `staging.yml` phát bản staging qua Firebase App Distribution, và `start-run.yml` kích hoạt phiên tự chạy qua API của routine. ADR này ghi cách làm cụ thể. Cách cài đặt từng bước nằm ở `docs/ops/staging-and-runs.md`.

## Quyết định

**`staging.yml`:**

- Chạy tay (dev, hoặc phiên tự chạy ở cuối phiên), luôn từ nhánh chính. Mỗi lúc chỉ một lần chạy, lần sau chờ lần trước.
- Build `stagingRelease`: thu gọn bằng R8 như bản production, nên lỗi do R8 lộ ra ở staging trước khi phát hành.
- Ký bằng một khóa chỉ dành cho staging, giữ cố định giữa các bản, để bản mới cài đè bản cũ mà người test không mất dữ liệu. Khóa là bí mật của repo; không có khóa thì `stagingRelease` không được ký (build trên máy vẫn chạy).
- Số build: `versionCode` là số thứ tự lần chạy của workflow, tên phiên bản có hậu tố `-staging.<số>`.
- File cấu hình Firebase staging thật được ghi vào `app/src/staging/release/` (ADR-0012). Workflow từ chối nếu bí mật vẫn là file giả.
- Phát bằng `firebase-tools` (phiên bản cố định) qua tài khoản dịch vụ, tới nhóm người test trong biến `STAGING_TESTER_GROUPS` (mặc định `team`). Không thêm plugin Gradle.
- Không ghi bộ nhớ đệm Gradle, vì PR đọc lại bộ nhớ đệm đó. Bí mật được xóa khỏi máy chạy ở cuối.

**`start-run.yml`:**

- Chạy tay, nhận đường dẫn danh sách chạy `docs/runs/RUN-YYYYMMDD-NN.md`.
- Từ chối nếu danh sách không có trên nhánh chính, chưa `approved`, hoặc có issue `main-broken` đang mở.
- Gọi `POST <ROUTINE_FIRE_URL>` với mã kích hoạt, gửi đường dẫn danh sách chạy trong trường `text`. Prompt của routine chỉ lấy đường dẫn từ đó (nội dung `text` được Anthropic đánh dấu là dữ liệu không tin cậy).
- Không bao giờ tự thử lại: API không chống trùng, mỗi lần gọi thành công là một phiên mới.
- Đường dẫn tới phiên ghi vào phần tóm tắt của workflow.

**Bí mật của repo** (đều thuộc staging hoặc routine, đúng phạm vi ADR-0009):

| Bí mật | Dùng ở |
| --- | --- |
| `STAGING_GOOGLE_SERVICES_JSON` | `staging.yml` |
| `STAGING_APP_DISTRIBUTION_CREDENTIALS` | `staging.yml` |
| `STAGING_KEYSTORE_BASE64`, `STAGING_KEYSTORE_PASSWORD`, `STAGING_KEY_ALIAS` | `staging.yml` |
| `ROUTINE_FIRE_URL`, `ROUTINE_FIRE_TOKEN` | `start-run.yml` |

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| `stagingRelease` ký khóa staging trong bí mật | Gần bản production; cài đè được | Thêm ba bí mật |
| Commit keystore staging vào repo | Không cần bí mật | Ai đọc được repo cũng ký được bản staging giả |
| `stagingDebug` ký khóa debug ngẫu nhiên của máy CI | Không cần gì thêm | Mỗi bản có chữ ký khác, người test phải gỡ app và mất dữ liệu |
| Plugin Gradle của App Distribution | Gọn trong Gradle | Thêm plugin vào build của mọi bản |

## Hệ quả

- **Tích cực:** người test cài đè bản mới; lỗi R8 lộ ra ở staging; dev bắt đầu phiên tự chạy từ GitHub mà không cần tài khoản Claude của routine.
- **Phải chấp nhận:**
  - Lộ khóa staging thì ai đó ký được bản staging giả; thiệt hại giới hạn trong nhóm người test.
  - Trên gói GitHub Free, workflow chạy trên bất kỳ nhánh nào cũng đọc được bí mật (ADR-0009). Điều kiện "chỉ chạy từ nhánh chính" trong workflow chỉ chặn việc chạy nhầm, không chặn được một workflow bị sửa trên nhánh khác.
  - API kích hoạt routine đang thử nghiệm; khi Anthropic đổi cách gọi, phải sửa `start-run.yml`.
  - Chưa kiểm chứng phiên tự chạy có quyền khởi động `staging.yml` hay không (`docs/ops/staging-and-runs.md`, mục 3).
- **Việc AI phải tuân thủ:** không đọc, in ra hay chuyển đi giá trị bí mật; không đổi `staging.yml` hay `start-run.yml` ngoài plan rủi ro cao đã duyệt chéo; cuối phiên tự chạy chỉ khởi động `staging.yml` trên nhánh chính.
