# CAMERA POSE: LINES AND GUIDES

App Camera Pose: Lines and Guides, tạo từ dự án mẫu của nhóm ngày 2026-10-21 (commit 9475da7), theo quy trình phát triển lấy AI làm lõi.

## Bắt đầu

1. Chuẩn bị máy theo [docs/ops/setup-new-app.md](docs/ops/setup-new-app.md), mục 1: JDK 17 tới 24 (dùng JDK đi kèm Android Studio; Java quá mới làm Gradle dừng), Android SDK, `gh` đã đăng nhập. Không cài Android Studio thì `scripts/local-setup.sh` cài JDK và Android SDK vào thư mục người dùng; `scripts/local-setup.sh --check` xem còn thiếu gì.
2. Mở thư mục dự án bằng Android Studio, chạy app với biến thể `stagingDebug`. Không có Android Studio thì `source` tệp `env.sh` mà `scripts/local-setup.sh` in ra, rồi làm bước 3.
3. Kiểm tra toàn bộ, giống hệt CI: `scripts/check.sh`.

## Tài liệu

Quy trình làm việc:

| Muốn biết | Đọc |
| --- | --- |
| Tiến độ: yêu cầu, ticket, phiên chạy, việc chờ người | [docs/STATUS.md trên nhánh status](../../blob/status/docs/STATUS.md) |
| Quy trình làm việc, cấu trúc tài liệu | [docs/README.md](docs/README.md) |
| Quy tắc nền tảng mà mọi app phải theo, gom từ các ADR | [docs/foundation.md](docs/foundation.md) |
| Các quyết định kỹ thuật và lý do | [docs/adr/](docs/adr/) |
| Kiểm tra danh sách chạy trước khi duyệt | [docs/ops/pre-run-review.md](docs/ops/pre-run-review.md) |
| Phiên AI tự chạy | [docs/auto-run.md](docs/auto-run.md) |
| Chọn cách mở phiên tự chạy (cloud, routine, local) | [docs/ops/session-capabilities.md](docs/ops/session-capabilities.md) |
| Làm ngay việc nhỏ trong ngày cùng AI, không chờ phiên tự chạy | [docs/ops/quick-lane.md](docs/ops/quick-lane.md) |
| Thang điểm rủi ro, điều kiện dừng | [docs/risk-scoring.md](docs/risk-scoring.md) |
| Thử bản staging trên máy, ghi log để AI phân tích | [docs/ops/test-sessions.md](docs/ops/test-sessions.md) |
| Tra lỗi đã biết của CI hay script và việc phải làm | [docs/ops/failure-playbook.md](docs/ops/failure-playbook.md) |
| Ước tính và đếm token AI theo task | [docs/ops/token-usage.md](docs/ops/token-usage.md) |
| Hướng dẫn cho AI về code | [CLAUDE.md](CLAUDE.md) |
| Hướng dẫn cho AI về tài liệu | [docs/CLAUDE.md](docs/CLAUDE.md) |

Thiết lập, đồng bộ, phát hành:

| Muốn biết | Đọc |
| --- | --- |
| Tạo app mới từ dự án mẫu | [docs/ops/new-app.md](docs/ops/new-app.md) |
| Thiết lập app mới từ đầu tới phiên tự chạy đầu tiên | [docs/ops/setup-new-app.md](docs/ops/setup-new-app.md) |
| Cài đặt staging và phiên tự chạy | [docs/ops/staging-and-runs.md](docs/ops/staging-and-runs.md) |
| Phát hành bản production | [docs/ops/release.md](docs/ops/release.md) |
| Đưa cải tiến từ app về dự án mẫu | [docs/ops/from-app.md](docs/ops/from-app.md) |
| Đưa thay đổi của dự án mẫu về app | [docs/ops/to-app.md](docs/ops/to-app.md) |

Sản phẩm:

| Muốn biết | Đọc |
| --- | --- |
| Khảo sát app mẫu để làm app clone | [docs/ops/clone-study.md](docs/ops/clone-study.md) |
| Thiết kế giao diện và đối chiếu với thiết kế | [docs/ops/design-workflow.md](docs/ops/design-workflow.md) |
| Luật nghiệp vụ quảng cáo (AdsKit PARTNER) | [docs/ops/ads-business-rules.md](docs/ops/ads-business-rules.md) |
| Quảng cáo trong code: định dạng, quy chuẩn native, thêm vị trí, đo lường | [docs/ops/ads-guide.md](docs/ops/ads-guide.md) |
| File Excel kế hoạch quảng cáo, Remote Config và sự kiện | [docs/ops/ads-plan.md](docs/ops/ads-plan.md) |
| Remote Config: đặt tên khóa, thêm khóa, đổi giá trị trên Firebase | [docs/ops/remote-config.md](docs/ops/remote-config.md) |

## Merge

Mọi PR vào `main` chỉ được merge bằng `scripts/merge-if-green.sh <số PR>` (ADR-0010).
