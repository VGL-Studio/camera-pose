# CAMERA POSE: LINES AND GUIDES

App Camera Pose: Lines and Guides, tạo từ dự án mẫu của nhóm ngày 2026-10-21 (commit 9475da7), theo quy trình phát triển lấy AI làm lõi.

## Bắt đầu

1. Mở thư mục dự án bằng Android Studio (JDK 17).
2. Chạy app với biến thể `stagingDebug`.
3. Kiểm tra toàn bộ, giống hệt CI: `scripts/check.sh`.

## Tài liệu

| Muốn biết | Đọc |
| --- | --- |
| Tiến độ: yêu cầu, ticket, phiên chạy, việc chờ người | [docs/STATUS.md trên nhánh status](../../blob/status/docs/STATUS.md) |
| Quy trình làm việc, cấu trúc tài liệu | [docs/README.md](docs/README.md) |
| Phiên AI tự chạy | [docs/auto-run.md](docs/auto-run.md) |
| Thang điểm rủi ro, điều kiện dừng | [docs/risk-scoring.md](docs/risk-scoring.md) |
| Các quyết định kỹ thuật | [docs/adr/](docs/adr/) |
| Hướng dẫn cho AI về code | [CLAUDE.md](CLAUDE.md) |
| Hướng dẫn cho AI về tài liệu | [docs/CLAUDE.md](docs/CLAUDE.md) |
| Thiết lập app mới từ đầu tới phiên tự chạy đầu tiên | [docs/ops/setup-new-app.md](docs/ops/setup-new-app.md) |
| Cài đặt staging và phiên tự chạy | [docs/ops/staging-and-runs.md](docs/ops/staging-and-runs.md) |
| Phát hành bản production | [docs/ops/release.md](docs/ops/release.md) |
| Đưa cải tiến từ app về dự án mẫu | [docs/ops/from-app.md](docs/ops/from-app.md) |
| Thiết kế giao diện và đối chiếu với thiết kế | [docs/ops/design-workflow.md](docs/ops/design-workflow.md) |
| Khảo sát app mẫu để làm app clone | [docs/ops/clone-study.md](docs/ops/clone-study.md) |
| Quảng cáo: định dạng, quy chuẩn native, thêm vị trí, đo lường | [docs/ops/ads-guide.md](docs/ops/ads-guide.md) |
| Remote Config: đặt tên khóa, thêm khóa, đổi giá trị trên Firebase | [docs/ops/remote-config.md](docs/ops/remote-config.md) |

## Merge

Mọi PR vào `main` chỉ được merge bằng `scripts/merge-if-green.sh <số PR>` (ADR-0010).
