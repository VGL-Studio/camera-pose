# TIẾN ĐỘ DỰ ÁN

<!-- Sinh bởi scripts/status.py từ trạng thái trong các tài liệu. Không sửa tay: sửa tài liệu gốc rồi chạy lại script (docs/CLAUDE.md, mục Trang tiến độ). -->

## 1. Đang chờ người

- Phản hồi chờ phân loại: [FB-20261022-03-loi-bao-kiem-tra-mang.md](features/loading/feedback/FB-20261022-03-loi-bao-kiem-tra-mang.md) (Màn loading bảo kiểm tra mạng khi máy có mạng nhưng máy chủ không tới được)
- Phản hồi chờ phân loại: [FB-20261022-04-log-ly-do-tai-file.md](features/loading/feedback/FB-20261022-04-log-ly-do-tai-file.md) (Log tải file thất bại không cho biết lý do cụ thể)
- Danh sách chạy `draft` chờ duyệt: [RUN-20261021-04.md](runs/RUN-20261021-04.md)
- Bài học `proposed` chờ retro: L-001 (Kỹ thuật đo lần đầu dùng phải chạy thử trước khi duyệt plan)
- Bài học `proposed` chờ retro: L-002 (Bí mật Firebase staging là file giả, cuối phiên không phát được)
- Bài học `proposed` chờ retro: L-003 (Quy tắc đếm `captureRoboImage(` của `check.sh` chặn ảnh tạm để đo điểm ảnh)
- Bài học `proposed` chờ retro: L-004 (Thuộc tính trợ năng của Compose không lan từ node cha xuống node con)
- Bài học `proposed` chờ retro: L-005 (Môi trường `claude --cloud` chặn máy chủ R2 staging, plan không kiểm được file bằng `curl`)
- Bài học `proposed` chờ retro: L-006 (`captureToImage()` hết giờ trên Robolectric; đo điểm ảnh bằng cách vẽ view gốc ra Bitmap)
- Bài học `proposed` chờ retro: L-007 (Hàm CameraX trả `ListenableFuture` không gọi được vì Guava không có trên classpath biên dịch)
- Bài học `proposed` chờ retro: L-008 (Phiên mở bằng `claude --cloud` không có `resume-run.yml` tiếp tục, và `token-usage.py` cần thư mục gốc của checkout chính)
- Bài học `proposed` chờ retro: L-009 (Hộp hỏi quyền camera còn mở làm hỏng lần đo khởi động nguội của `device-session`)
- Bài học `proposed` chờ retro: L-010 (Chạy thử kỹ thuật mới bằng đúng lệnh của phiên, ghi vào mục riêng của plan)
- Bài học `proposed` chờ retro: L-011 (Tài nguyên R2 bắt buộc của app chưa có, PR có code đầu tiên đỏ ở emulator)
- Bài học `proposed` chờ retro: L-012 (Kiểm tra bí mật staging ở đầu phiên, không để tới cuối)
- Bài học `proposed` chờ retro: L-013 (Quy tắc của `check.sh` phải hiện ra lúc viết plan)
- Bài học `proposed` chờ retro: L-014 (Plan đẩy việc cho nhau, hoặc cùng sửa một file mà không phụ thuộc nhau)
- Bài học `proposed` chờ retro: L-015 (Danh sách chạy chỉ một chuỗi phụ thuộc thì một task dừng làm cả phiên trống)
- Bài học `proposed` chờ retro: L-016 (`docs/STATUS.md` xung đột ở mọi PR)
- Bài học `proposed` chờ retro: L-017 (`merge-if-green.sh` cần tự cập nhật nhánh và chịu được job kẹt)
- Bài học `proposed` chờ retro: L-018 (PR bị merge trước khi nhận commit cuối)
- Bài học `proposed` chờ retro: L-019 (Trạng thái ticket và plan đổi tay, hay lệch nhau)
- Bài học `proposed` chờ retro: L-020 (Routine không dùng cloud session credits; mở phiên bằng `claude --cloud` thì dùng được)
- Bài học `proposed` chờ retro: L-021 (Số ước tính token tạm quá cao)
- Bài học `proposed` chờ retro: L-022 (Java của máy dev quá mới làm Gradle không chạy)
- Bài học `proposed` chờ retro: L-023 (Lỗi thiết kế lọt vào gói bàn giao)
- Bài học `proposed` chờ retro: L-024 (Cải tiến đã làm ở app, cần đưa về dự án mẫu)
- Bài học `proposed` chờ retro: L-025 (Tài liệu của app còn chỗ cũ sau khi quyết định đổi)
- Bài học `proposed` chờ retro: L-026 (Mã phiên thử trên máy thật trùng mã phiên emulator của phiên tự chạy)
- Bài học `proposed` chờ retro: L-027 (Lỗi DNS của tên miền file tĩnh chỉ lộ ra trên máy thật)
- Bài học `proposed` chờ retro: L-028 (Phiên kiểm `main-broken` trước khi `main-guard` của lần merge vừa rồi chạy xong)
- Bài học `proposed` chờ retro: L-029 (Screenshot test của trạng thái có vòng chờ đỏ trên `main-guard` dù xanh ở PR và cục bộ)
- Bài học `proposed` chờ retro: L-030 (Điều hướng sau một hàm `suspend` đọc DataStore chạy ngoài luồng chính dưới Compose test)
- Bài học `proposed` chờ retro: L-031 (Giao bước 3 đến 5 của task lớn cho một phiên AI viết code riêng)
- Bài học `proposed` chờ retro: L-032 (Nút chữ trong thành phần có nền khác màu tự đặt màu chữ, đè màu thiết kế)
- Bài học `proposed` chờ retro: L-033 (Các plan viết song song cùng ghi "Tạo" cho một file mà task chạy trước trong phiên tạo ra)
- Bài học `proposed` chờ retro: L-034 (Phiên con viết code trong thư mục chính làm phiên chính không làm gì song song được)
- Bài học `proposed` chờ retro: L-035 (Thêm một bước đối chứng để test viết trước không qua ngay trên khung rỗng)
- Bài học `proposed` chờ retro: L-036 (Chạy lại `main-guard` trên đúng commit bị đổ lỗi, xanh, nhưng issue `main-broken` không tự đóng)
- Bài học `proposed` chờ retro: L-037 (Một test chập chờn trên `main-guard` kết thúc cả phiên)
- Bài học `proposed` chờ retro: L-038 (Phiên không tự đưa nhánh chính về trạng thái xanh được, kể cả bằng một bản revert đúng)
- Bài học `proposed` chờ retro: L-039 (Plan ghi sẵn phương án dự phòng đã duyệt, để phiên chuyển sang thay vì dừng task)
- Bài học `proposed` chờ retro: L-040 (Mâu thuẫn và test yếu chỉ được tìm thấy khi phiên đã chạy, hoặc nhờ người tình cờ thấy)
- Bài học `proposed` chờ retro: L-041 (Quyền và thư viện cần thêm chỉ lộ ra khi viết code, phiên phải dừng hoặc đi vòng)
- Bài học `proposed` chờ retro: L-042 (Bài học thêm ngoài phiên trùng số với bài học mà phiên thêm cùng lúc)
- Bài học `proposed` chờ retro: L-043 (Trang tiến độ đếm 0 bài học vì sổ của app viết khác mẫu)
- Bài học `proposed` chờ retro: L-044 (Plan đổi cổng chất lượng bỏ sót một kiểm tra mà trường hợp mới phải đi qua)
- Bài học `proposed` chờ retro: L-045 (Plan đã duyệt nhưng ticket vẫn `ready`, mục 9 còn chữ mẫu)
- ADR `proposed` chờ quyết định: [0020-design-handoff-and-conformance.md](adr/0020-design-handoff-and-conformance.md)
- ADR `proposed` chờ quyết định: [0021-clone-reference-apps.md](adr/0021-clone-reference-apps.md)

## 2. Yêu cầu

| Yêu cầu | Ngày | Feature | Trạng thái | Ticket xong |
| --- | --- | --- | --- | --- |
| Camera chụp theo tư thế và màn xem lại ([2026-10-21-camera-theo-tu-the.md](features/camera/requests/2026-10-21-camera-theo-tu-the.md)) | 2026-10-21 | camera | `specified` đã có spec | 4/9 |
| Màn hình loading có thanh tiến trình ([2026-10-15-man-hinh-loading.md](features/loading/requests/2026-10-15-man-hinh-loading.md)) | 2026-10-15 | loading | `specified` đã có spec | 9/9 |
| Màn loading cho camera pose ([2026-10-21-loading-cua-camera-pose.md](features/loading/requests/2026-10-21-loading-cua-camera-pose.md)) | 2026-10-21 | loading | `specified` đã có spec | 9/9 |
| App luôn cần mạng, báo mất mạng khi đang dùng ([2026-10-21-hop-thoai-mat-mang.md](features/network/requests/2026-10-21-hop-thoai-mat-mang.md)) | 2026-10-21 | network | `specified` đã có spec | 1/1 |
| Màn chào và xin quyền camera ([2026-10-21-man-chao-xin-quyen-camera.md](features/onboarding/requests/2026-10-21-man-chao-xin-quyen-camera.md)) | 2026-10-21 | onboarding | `specified` đã có spec | 1/1 |
| Thư viện tư thế và chi tiết tư thế ([2026-10-21-thu-vien-tu-the.md](features/poses/requests/2026-10-21-thu-vien-tu-the.md)) | 2026-10-21 | poses | `specified` đã có spec | 2/5 |
| Cài đặt ([2026-10-21-cai-dat.md](features/settings/requests/2026-10-21-cai-dat.md)) | 2026-10-21 | settings | `specified` đã có spec | 1/1 |

## 3. Feature

### camera

Spec: [spec.md](features/camera/spec.md) `confirmed`. Ticket: 5 `ready`, 4 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-013](features/camera/tickets/T-013-camera-co-ban.md) | Camera: khung xem trước, quyền, công cụ chụp, chụp ảnh | `on-staging` | `done` (bản 1) | 40.0M | RUN-20261021-04, RUN-20261022-01 |
| [T-014](features/camera/tickets/T-014-camera-chon-tu-the.md) | Camera: chọn tư thế, nét viền, thẻ bước | `on-staging` | `done` (bản 1) | 30M | RUN-20261023-01, RUN-20261023-02 |
| [T-015](features/camera/tickets/T-015-camera-chinh-net-vien.md) | Camera: chỉnh nét viền | `ready` | chưa có | - | - |
| [T-016](features/camera/tickets/T-016-man-xem-lai.md) | Màn xem lại, lưu, chia sẻ, tư thế tiếp | `on-staging` | `done` (bản 2) | 28M | RUN-20261023-01, RUN-20261023-02 |
| [T-017](features/camera/tickets/T-017-camera-diem-khop.md) | Nhận dạng dáng và điểm khớp | `ready` | chưa có | - | - |
| [T-018](features/camera/tickets/T-018-quang-cao-xem-lai.md) | Quảng cáo toàn màn hình khi rời màn xem lại | `ready` | chưa có | - | - |
| [T-019](features/camera/tickets/T-019-hoi-danh-gia.md) | Hỏi đánh giá app trong app | `ready` | chưa có | - | - |
| [T-020](features/camera/tickets/T-020-camera-may-gap.md) | Camera và màn xem lại trên máy gập | `ready` | chưa có | - | - |
| [T-026](features/camera/tickets/T-026-khong-ghi-quyen-tu-choi-vinh-vien.md) | Không ghi camera_permission_result khi quyền camera đã bị từ chối vĩnh viễn | `on-staging` | `done` (bản 1) | 10.0M | RUN-20261023-01 |

Phản hồi: chưa có.

### loading

Spec: [spec.md](features/loading/spec.md) `confirmed`. Ticket: 9 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-001](features/loading/tickets/T-001-man-hinh-loading.md) | Màn hình loading có thanh tiến trình khi mở app | `on-staging` | `done` (bản 2) | - | - |
| [T-002](features/loading/tickets/T-002-thuong-hieu-xanh-luc.md) | Hệ thống thiết kế theo thương hiệu mới (xanh lục, Inter, logo) | `on-staging` | `done` (bản 2) | - | - |
| [T-003](features/loading/tickets/T-003-giao-dien-loading-theo-thiet-ke.md) | Màn hình loading theo thiết kế Figma | `on-staging` | `done` (bản 1) | - | - |
| [T-004](features/loading/tickets/T-004-mau-con-thieu-va-gian-chu.md) | Đủ vai trò màu thương hiệu và giãn cách chữ như thiết kế | `on-staging` | `done` (bản 1) | - | - |
| [T-005](features/loading/tickets/T-005-man-hinh-thap.md) | Màn hình loading dùng được trên màn hình thấp | `on-staging` | `done` (bản 1) | - | - |
| [T-006](features/loading/tickets/T-006-cuon-man-hinh-rat-thap.md) | Test cuộn tới nút "Thử lại" trên màn hình rất thấp | `on-staging` | `done` (bản 1) | - | RUN-20261023-01 |
| [T-007](features/loading/tickets/T-007-he-thong-thiet-ke-sticker.md) | Hệ thống thiết kế theo hướng Sticker | `on-staging` | `done` (bản 3) | 40.0M | RUN-20261021-01, RUN-20261021-02, RUN-20261021-03 |
| [T-008](features/loading/tickets/T-008-loading-giao-dien-sticker.md) | Màn loading theo thiết kế Sticker và chữ của app | `on-staging` | `done` (bản 1) | 20.0M | RUN-20261021-04, RUN-20261022-01 |
| [T-010](features/loading/tickets/T-010-loading-tai-catalog-bo-khoi-dau.md) | Màn loading tải catalog và bộ tư thế khởi đầu | `on-staging` | `done` (bản 1) | 20M | RUN-20261021-04, RUN-20261022-01 |

Phản hồi: 2 `fixed`, 2 `new`, 2 `wontfix`.
- Đang mở: [FB-20261022-03-loi-bao-kiem-tra-mang.md](features/loading/feedback/FB-20261022-03-loi-bao-kiem-tra-mang.md) `new`, mức `gop-y`: Màn loading bảo kiểm tra mạng khi máy có mạng nhưng máy chủ không tới được
- Đang mở: [FB-20261022-04-log-ly-do-tai-file.md](features/loading/feedback/FB-20261022-04-log-ly-do-tai-file.md) `new`, mức `gop-y`: Log tải file thất bại không cho biết lý do cụ thể

### network

Spec: [spec.md](features/network/spec.md) `confirmed`. Ticket: 1 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-011](features/network/tickets/T-011-core-network-hop-thoai.md) | Nhận ra mất mạng và hộp thoại mất mạng | `on-staging` | `done` (bản 2) | 40M | RUN-20261021-01, RUN-20261021-02, RUN-20261021-03, RUN-20261021-04, RUN-20261022-01 |

Phản hồi: chưa có.

### onboarding

Spec: [spec.md](features/onboarding/spec.md) `confirmed`. Ticket: 1 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-012](features/onboarding/tickets/T-012-man-chao.md) | Màn chào và xin quyền camera | `on-staging` | `done` (bản 2) | 20.0M | RUN-20261023-01, RUN-20261023-02 |

Phản hồi: chưa có.

### poses

Spec: [spec.md](features/poses/spec.md) `confirmed`. Ticket: 3 `ready`, 2 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-009](features/poses/tickets/T-009-core-poses.md) | Module dữ liệu tư thế dùng chung `:core:poses` | `on-staging` | `done` (bản 1) | 40M | RUN-20261021-01 |
| [T-021](features/poses/tickets/T-021-thu-vien-tu-the.md) | Thư viện tư thế: lưới, nhóm, đã lưu, tải và lỗi | `on-staging` | `done` (bản 1) | 45M | RUN-20261023-01, RUN-20261023-02 |
| [T-022](features/poses/tickets/T-022-chi-tiet-tu-the.md) | Chi tiết tư thế và "Chụp với tư thế này" | `ready` | chưa có | - | - |
| [T-023](features/poses/tickets/T-023-banner-thu-vien.md) | Banner ở thư viện và chi tiết tư thế | `ready` | chưa có | - | - |
| [T-024](features/poses/tickets/T-024-thu-vien-may-gap.md) | Thư viện và chi tiết tư thế trên máy gập | `ready` | chưa có | - | - |

Phản hồi: chưa có.

### quality-gate

Spec: chưa có spec. Ticket: 1 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-028](features/quality-gate/tickets/T-028-main-tu-hoi-phuc.md) | Nhánh chính tự hồi phục khi đỏ (ADR-1002) | `on-staging` | `done` (bản 1) | 15M | - |

Phản hồi: chưa có.

### settings

Spec: [spec.md](features/settings/spec.md) `confirmed`. Ticket: 1 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-025](features/settings/tickets/T-025-man-cai-dat.md) | Màn cài đặt | `on-staging` | `done` (bản 1) | 20.0M | RUN-20261023-01, RUN-20261023-02 |

Phản hồi: chưa có.

## 4. Phiên tự chạy

| Phiên | Trạng thái | Bắt đầu | Ticket | Bản staging |
| --- | --- | --- | --- | --- |
| [RUN-20261023-02](runs/RUN-20261023-02.md) | `finished` | 2026-10-22T23:14Z | T-012, T-014, T-021, T-016, T-025 | 0.1.0-staging.4 (4), đã phát tới nhóm qua App Distribution |
| [RUN-20261023-01](runs/RUN-20261023-01.md) | `finished` | 2026-10-22T15:45Z | T-006, T-026, T-012, T-014, T-021, T-016, T-025 | - |
| [RUN-20261022-01](runs/RUN-20261022-01.md) | `finished` | 2026-10-22T01:29Z | T-011, T-010, T-008, T-013 | 0.1.0-staging.3 (3), đã tải lên App Distribution, chưa phát tới nhóm (nhóm `team` chưa có) |
| [RUN-20261021-04](runs/RUN-20261021-04.md) | `draft` |  | T-008, T-011, T-010, T-013 | - |
| [RUN-20261021-03](runs/RUN-20261021-03.md) | `finished` | 2026-10-21T15:01Z | T-007, T-011 | - |
| [RUN-20261021-02](runs/RUN-20261021-02.md) | `finished` | 2026-10-21T14:14Z | T-007, T-011 | không phát (không có task nào merge) |
| [RUN-20261021-01](runs/RUN-20261021-01.md) | `finished` | 2026-10-21T10:30Z | T-007, T-009, T-011 | không phát được (xem mục Bản staging) |

## 5. Bài học và ADR

- Bài học ([lessons.md](lessons.md)): 45 bài; 45 `proposed`.
- ADR: 23; 21 `accepted`, 2 `proposed`.
