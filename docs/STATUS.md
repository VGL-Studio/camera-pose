# TIẾN ĐỘ DỰ ÁN

<!-- Sinh bởi scripts/status.py từ trạng thái trong các tài liệu. Không sửa tay: sửa tài liệu gốc rồi chạy lại script (docs/CLAUDE.md, mục Trang tiến độ). -->

## 1. Đang chờ người

- Ticket đã duyệt, chưa vào danh sách chạy nào: [T-006-cuon-man-hinh-rat-thap.md](features/loading/tickets/T-006-cuon-man-hinh-rat-thap.md)
- ADR `proposed` chờ quyết định: [0020-design-handoff-and-conformance.md](adr/0020-design-handoff-and-conformance.md)
- ADR `proposed` chờ quyết định: [0021-clone-reference-apps.md](adr/0021-clone-reference-apps.md)

## 2. Yêu cầu

| Yêu cầu | Ngày | Feature | Trạng thái | Ticket xong |
| --- | --- | --- | --- | --- |
| Camera chụp theo tư thế và màn xem lại ([2026-10-21-camera-theo-tu-the.md](features/camera/requests/2026-10-21-camera-theo-tu-the.md)) | 2026-10-21 | camera | `specified` đã có spec | - |
| Màn hình loading có thanh tiến trình ([2026-10-15-man-hinh-loading.md](features/loading/requests/2026-10-15-man-hinh-loading.md)) | 2026-10-15 | loading | `specified` đã có spec | 5/6 |
| Màn loading cho camera pose ([2026-10-21-loading-cua-camera-pose.md](features/loading/requests/2026-10-21-loading-cua-camera-pose.md)) | 2026-10-21 | loading | `specified` đã có spec | 5/6 |
| App luôn cần mạng, báo mất mạng khi đang dùng ([2026-10-21-hop-thoai-mat-mang.md](features/network/requests/2026-10-21-hop-thoai-mat-mang.md)) | 2026-10-21 | network | `specified` đã có spec | - |
| Màn chào và xin quyền camera ([2026-10-21-man-chao-xin-quyen-camera.md](features/onboarding/requests/2026-10-21-man-chao-xin-quyen-camera.md)) | 2026-10-21 | onboarding | `specified` đã có spec | - |
| Thư viện tư thế và chi tiết tư thế ([2026-10-21-thu-vien-tu-the.md](features/poses/requests/2026-10-21-thu-vien-tu-the.md)) | 2026-10-21 | poses | `specified` đã có spec | - |
| Cài đặt ([2026-10-21-cai-dat.md](features/settings/requests/2026-10-21-cai-dat.md)) | 2026-10-21 | settings | `specified` đã có spec | - |

## 3. Feature

### camera

Spec: [spec.md](features/camera/spec.md) `confirmed`. Ticket: chưa có.

Phản hồi: chưa có.

### loading

Spec: [spec.md](features/loading/spec.md) `confirmed`. Ticket: 5 `on-staging`, 1 `approved`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-001](features/loading/tickets/T-001-man-hinh-loading.md) | Màn hình loading có thanh tiến trình khi mở app | `on-staging` | `done` (bản 2) | - | - |
| [T-002](features/loading/tickets/T-002-thuong-hieu-xanh-luc.md) | Hệ thống thiết kế theo thương hiệu mới (xanh lục, Inter, logo) | `on-staging` | `done` (bản 2) | - | - |
| [T-003](features/loading/tickets/T-003-giao-dien-loading-theo-thiet-ke.md) | Màn hình loading theo thiết kế Figma | `on-staging` | `done` (bản 1) | - | - |
| [T-004](features/loading/tickets/T-004-mau-con-thieu-va-gian-chu.md) | Đủ vai trò màu thương hiệu và giãn cách chữ như thiết kế | `on-staging` | `done` (bản 1) | - | - |
| [T-005](features/loading/tickets/T-005-man-hinh-thap.md) | Màn hình loading dùng được trên màn hình thấp | `on-staging` | `done` (bản 1) | - | - |
| [T-006](features/loading/tickets/T-006-cuon-man-hinh-rat-thap.md) | Test cuộn tới nút "Thử lại" trên màn hình rất thấp | `approved` | `approved` (bản 1) | - | - |

Phản hồi: 1 `fixed`, 1 `wontfix`.

### network

Spec: [spec.md](features/network/spec.md) `confirmed`. Ticket: chưa có.

Phản hồi: chưa có.

### onboarding

Spec: [spec.md](features/onboarding/spec.md) `confirmed`. Ticket: chưa có.

Phản hồi: chưa có.

### poses

Spec: [spec.md](features/poses/spec.md) `confirmed`. Ticket: chưa có.

Phản hồi: chưa có.

### settings

Spec: [spec.md](features/settings/spec.md) `confirmed`. Ticket: chưa có.

Phản hồi: chưa có.

## 4. Phiên tự chạy

Chưa có phiên nào.

## 5. Bài học và ADR

- Bài học ([lessons.md](lessons.md)): 0 bài; chưa có.
- ADR: 22; 20 `accepted`, 2 `proposed`.
