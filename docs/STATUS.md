# TIẾN ĐỘ DỰ ÁN

<!-- Sinh bởi scripts/status.py từ trạng thái trong các tài liệu. Không sửa tay: sửa tài liệu gốc rồi chạy lại script (docs/CLAUDE.md, mục Trang tiến độ). -->

## 1. Đang chờ người

- Danh sách chạy `draft` chờ duyệt: [RUN-20261021-04.md](runs/RUN-20261021-04.md)
- Danh sách chạy đã duyệt, chờ kích hoạt: [RUN-20261031-01.md](runs/RUN-20261031-01.md)
- ADR `proposed` chờ quyết định: [0020-design-handoff-and-conformance.md](adr/0020-design-handoff-and-conformance.md)
- ADR `proposed` chờ quyết định: [0021-clone-reference-apps.md](adr/0021-clone-reference-apps.md)

## 2. Yêu cầu

| Yêu cầu | Ngày | Feature | Trạng thái | Ticket xong |
| --- | --- | --- | --- | --- |
| Camera chụp theo tư thế và màn xem lại ([2026-10-21-camera-theo-tu-the.md](features/camera/requests/2026-10-21-camera-theo-tu-the.md)) | 2026-10-21 | camera | `specified` đã có spec | 17/17 |
| Màn hình loading có thanh tiến trình ([2026-10-15-man-hinh-loading.md](features/loading/requests/2026-10-15-man-hinh-loading.md)) | 2026-10-15 | loading | `specified` đã có spec | 11/11 |
| Màn loading cho camera pose ([2026-10-21-loading-cua-camera-pose.md](features/loading/requests/2026-10-21-loading-cua-camera-pose.md)) | 2026-10-21 | loading | `specified` đã có spec | 11/11 |
| App luôn cần mạng, báo mất mạng khi đang dùng ([2026-10-21-hop-thoai-mat-mang.md](features/network/requests/2026-10-21-hop-thoai-mat-mang.md)) | 2026-10-21 | network | `specified` đã có spec | 4/4 |
| Màn chào và xin quyền camera ([2026-10-21-man-chao-xin-quyen-camera.md](features/onboarding/requests/2026-10-21-man-chao-xin-quyen-camera.md)) | 2026-10-21 | onboarding | `specified` đã có spec | 1/1 |
| Thư viện tư thế và chi tiết tư thế ([2026-10-21-thu-vien-tu-the.md](features/poses/requests/2026-10-21-thu-vien-tu-the.md)) | 2026-10-21 | poses | `specified` đã có spec | 6/6 |
| Cài đặt ([2026-10-21-cai-dat.md](features/settings/requests/2026-10-21-cai-dat.md)) | 2026-10-21 | settings | `specified` đã có spec | 4/4 |

## 3. Feature

### ads

Spec: [spec.md](features/ads/spec.md) `confirmed`. Ticket: 2 `backlog`, 1 `approved`, 1 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-074](features/ads/tickets/T-074-lay-khung-quang-cao-adskit.md) | Lấy khung quảng cáo AdsKit của dự án mẫu về app (chưa bật AppsFlyer), chuyển ba vị trí quảng cáo hiện có sang khung mới, bật quảng cáo mở app và quay lại app | `on-staging` | `done` (bản 1) | 54M | RUN-20261029-01, RUN-20261029-01, RUN-20261029-01 |
| [T-075](features/ads/tickets/T-075-phat-hanh-voi-quang-cao-moi.md) | Lấy release.sh và release.md của dự án mẫu theo khung quảng cáo mới (mã quảng cáo thật; chưa có AppsFlyer) | `backlog` | chưa có | - | - |
| [T-079](features/ads/tickets/T-079-thanh-day-native-va-vi-tri-dot-2.md) | Thanh đáy native thay hai banner ở thư viện và chi tiết tư thế; interstitial khi chụp với tư thế (tắt lúc phát hành) | `approved` | `approved` (bản 2) | 15.7M | RUN-20261031-01, RUN-20261031-01 |
| [T-080](features/ads/tickets/T-080-o-quang-cao-trong-luoi-thu-vien.md) | Ô quảng cáo trong lưới thư viện (`poses_in_collection`, tắt lúc phát hành); khung bỏ ô khi pool cạn | `backlog` | chưa có | - | - |

Phản hồi: 1 `fixed`.

### camera

Spec: [spec.md](features/camera/spec.md) `confirmed`. Ticket: 13 `on-staging`, 4 `accepted`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-013](features/camera/tickets/T-013-camera-co-ban.md) | Camera: khung xem trước, quyền, công cụ chụp, chụp ảnh | `on-staging` | `done` (bản 1) | 40.0M | RUN-20261021-04, RUN-20261022-01 |
| [T-014](features/camera/tickets/T-014-camera-chon-tu-the.md) | Camera: chọn tư thế, nét viền, thẻ bước | `on-staging` | `done` (bản 1) | 30M | RUN-20261023-01, RUN-20261023-02 |
| [T-015](features/camera/tickets/T-015-camera-chinh-net-vien.md) | Camera: chỉnh nét viền | `on-staging` | `done` (bản 1) | 35M | RUN-20261023-03, RUN-20261023-04 |
| [T-016](features/camera/tickets/T-016-man-xem-lai.md) | Màn xem lại, lưu, chia sẻ, tư thế tiếp | `on-staging` | `done` (bản 2) | 28M | RUN-20261023-01, RUN-20261023-02 |
| [T-017](features/camera/tickets/T-017-camera-diem-khop.md) | Nhận dạng dáng và điểm khớp | `on-staging` | `done` (bản 2) | 45M | RUN-20261023-03, RUN-20261023-04 |
| [T-018](features/camera/tickets/T-018-quang-cao-xem-lai.md) | Quảng cáo toàn màn hình khi rời màn xem lại | `on-staging` | `done` (bản 2) | 11M | RUN-20261023-03, RUN-20261023-04 |
| [T-019](features/camera/tickets/T-019-hoi-danh-gia.md) | Hỏi đánh giá app trong app | `on-staging` | `done` (bản 2) | 15M | RUN-20261023-03, RUN-20261023-04 |
| [T-020](features/camera/tickets/T-020-camera-may-gap.md) | Camera và màn xem lại trên máy gập | `on-staging` | `done` (bản 1) | 30M | RUN-20261023-03, RUN-20261023-04 |
| [T-026](features/camera/tickets/T-026-khong-ghi-quyen-tu-choi-vinh-vien.md) | Không ghi camera_permission_result khi quyền camera đã bị từ chối vĩnh viễn | `on-staging` | `done` (bản 1) | 10.0M | RUN-20261023-01 |
| [T-031](features/camera/tickets/T-031-camerax-listenablefuture.md) | Gọi thẳng CameraControl.setZoomRatio, bỏ reflection | `on-staging` | `done` (bản 1) | 5.0M | - |
| [T-056](features/camera/tickets/T-056-huong-man-lon.md) | Màn lớn theo hướng của máy, điện thoại khóa dọc | `on-staging` | `done` (bản 1) | 18M | RUN-20261026-02 |
| [T-058](features/camera/tickets/T-058-camera-mo-tu-thu-vien.md) | Camera mở từ "Chụp với tư thế này" chạy và chụp được | `accepted` | `done` (bản 1) | 9M | RUN-20261026-03 |
| [T-059](features/camera/tickets/T-059-cham-the-buoc.md) | Chạm thẻ bước để thu gọn | `accepted` | `done` (bản 1) | 6M | RUN-20261026-03 |
| [T-060](features/camera/tickets/T-060-cat-net-vien-theo-khung.md) | Nét viền không vẽ ra ngoài khung xem trước | `accepted` | `done` (bản 1) | 5.8M | RUN-20261026-03 |
| [T-061](features/camera/tickets/T-061-khoa-nut-khi-dem-nguoc.md) | Khóa nút và dừng đếm khi đang đếm ngược | `accepted` | `done` (bản 1) | 7.5M | RUN-20261026-03 |
| [T-064](features/camera/tickets/T-064-mot-camera.md) | Máy chỉ có một camera thì nút đổi camera mờ đi, camera mở bằng camera máy có | `on-staging` | `done` (bản 1) | 5.5M | RUN-20261027-01 |
| [T-072](features/camera/tickets/T-072-log-camera-test-chu.md) | Ghi log khi không hỏi được danh sách camera, làm chặt test chữ của camera và loading | `on-staging` | `done` (bản 1) | 5.8M | RUN-20261028-02 |

Phản hồi: 5 `fixed`.

### loading

Spec: [spec.md](features/loading/spec.md) `confirmed`. Ticket: 11 `on-staging`.

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
| [T-032](features/loading/tickets/T-032-log-ten-loi-tai-file.md) | Log tải file thất bại ghi loại lỗi và lý do | `on-staging` | `done` (bản 1) | 5.0M | - |
| [T-057](features/loading/tickets/T-057-loi-bao-may-chu.md) | Màn loading báo "máy chủ chưa phản hồi" khi máy có mạng mà vẫn không tải được | `on-staging` | `done` (bản 1) | 15M | RUN-20261026-02 |

Phản hồi: 3 `fixed`, 2 `wontfix`, 1 `planned`.
- Đang mở: [FB-20261022-03-loi-bao-kiem-tra-mang.md](features/loading/feedback/FB-20261022-03-loi-bao-kiem-tra-mang.md) `planned`, mức `gop-y`: Màn loading bảo kiểm tra mạng khi máy có mạng nhưng máy chủ không tới được

### network

Spec: [spec.md](features/network/spec.md) `confirmed`. Ticket: 4 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-011](features/network/tickets/T-011-core-network-hop-thoai.md) | Nhận ra mất mạng và hộp thoại mất mạng | `on-staging` | `done` (bản 2) | 40M | RUN-20261021-01, RUN-20261021-02, RUN-20261021-03, RUN-20261021-04, RUN-20261022-01 |
| [T-076](features/network/tickets/T-076-co-mang-khi-da-xac-nhan.md) | Chốt định nghĩa "có mạng" theo ADR-0024 điều 1 (INTERNET và VALIDATED) hay ghi ngoại lệ của app | `on-staging` | `done` (bản 1) | 6.4M | RUN-20261030-01 |
| [T-077](features/network/tickets/T-077-popup-khi-quay-lai-app.md) | Hộp thoại mất mạng hiện ngay khi quay lại app lúc không có mạng | `on-staging` | `done` (bản 1) | 6.4M | RUN-20261030-01 |
| [T-078](features/network/tickets/T-078-hop-thoai-sau-khi-he-thong-dong-app.md) | Hộp thoại mất mạng hiện ngay khi mở lại app sau khi hệ thống đóng app ở nền | `on-staging` | `done` (bản 1) | 7.0M | RUN-20261030-02 |

Phản hồi: chưa có.

### onboarding

Spec: [spec.md](features/onboarding/spec.md) `confirmed`. Ticket: 1 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-012](features/onboarding/tickets/T-012-man-chao.md) | Màn chào và xin quyền camera | `on-staging` | `done` (bản 2) | 20.0M | RUN-20261023-01, RUN-20261023-02 |

Phản hồi: chưa có.

### poses

Spec: [spec.md](features/poses/spec.md) `confirmed`. Ticket: 6 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-009](features/poses/tickets/T-009-core-poses.md) | Module dữ liệu tư thế dùng chung `:core:poses` | `on-staging` | `done` (bản 1) | 40M | RUN-20261021-01 |
| [T-021](features/poses/tickets/T-021-thu-vien-tu-the.md) | Thư viện tư thế: lưới, nhóm, đã lưu, tải và lỗi | `on-staging` | `done` (bản 1) | 45M | RUN-20261023-01, RUN-20261023-02 |
| [T-022](features/poses/tickets/T-022-chi-tiet-tu-the.md) | Chi tiết tư thế và "Chụp với tư thế này" | `on-staging` | `done` (bản 1) | 35M | RUN-20261023-03, RUN-20261023-04 |
| [T-023](features/poses/tickets/T-023-banner-thu-vien.md) | Banner ở thư viện và chi tiết tư thế | `on-staging` | `done` (bản 1) | 25M | RUN-20261023-03, RUN-20261023-04 |
| [T-024](features/poses/tickets/T-024-thu-vien-may-gap.md) | Thư viện và chi tiết tư thế trên máy gập | `on-staging` | `done` (bản 1) | 20M | RUN-20261023-03, RUN-20261023-04 |
| [T-071](features/poses/tickets/T-071-catalog-mac-dinh-v3.md) | Catalog mặc định của app là catalog tư thế thật (v3), bỏ 6 tư thế mượn | `on-staging` | `done` (bản 1) | 5.8M | RUN-20261028-02 |

Phản hồi: 1 `fixed`.

### quality-gate

Spec: chưa có spec. Ticket: 32 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-028](features/quality-gate/tickets/T-028-main-tu-hoi-phuc.md) | Nhánh chính tự hồi phục khi đỏ (ADR-1002) | `on-staging` | `done` (bản 1) | 15M | - |
| [T-029](features/quality-gate/tickets/T-029-merge-if-green-tu-cap-nhat.md) | merge-if-green.sh tự cập nhật nhánh và chịu được job kẹt trạng thái | `on-staging` | `done` (bản 1) | 5.0M | - |
| [T-030](features/quality-gate/tickets/T-030-kiem-tra-truoc-phien.md) | Kiểm tra môi trường trước phiên trên GitHub (bí mật staging, file R2, DNS) và phiên bản Java của check.sh | `on-staging` | `done` (bản 1) | 20M | - |
| [T-033](features/quality-gate/tickets/T-033-wait-run-loi-ha-tang.md) | wait-run.sh nhận ra job GitHub không chạy (hạn mức Actions) hay PR xung đột, và phiên tự tiếp tục sau đó | `on-staging` | `done` (bản 1) | 5.0M | - |
| [T-034](features/quality-gate/tickets/T-034-trang-tien-do-tren-nhanh-status.md) | Trang tiến độ sinh trên nhánh status sau mỗi lần merge, PR không commit docs/STATUS.md | `on-staging` | `done` (bản 1) | 5.0M | - |
| [T-035](features/quality-gate/tickets/T-035-java-check-java-tool-options.md) | java-check.sh đọc đúng phiên bản Java khi môi trường đặt JAVA_TOOL_OPTIONS | `on-staging` | `done` (bản 1) | 5.0M | - |
| [T-036](features/quality-gate/tickets/T-036-dien-tap-dau-phien.md) | Diễn tập đầu phiên trong đúng môi trường cloud | `on-staging` | `done` (bản 2) | 5.0M | RUN-20261024-01 |
| [T-037](features/quality-gate/tickets/T-037-nhanh-task-khong-mat.md) | Nhánh task không chỉ nằm trên máy cloud | `on-staging` | `done` (bản 2) | 5.0M | RUN-20261024-01 |
| [T-038](features/quality-gate/tickets/T-038-pr-check-ban-release.md) | pr-check build bản release khi đổi thư viện hay module | `on-staging` | `done` (bản 1) | 5.0M | - |
| [T-039](features/quality-gate/tickets/T-039-bang-tra-loi.md) | Bảng tra lỗi đã biết và việc phải làm | `on-staging` | `done` (bản 2) | 12.9M | RUN-20261024-01 |
| [T-040](features/quality-gate/tickets/T-040-merge-if-green-da-merge.md) | merge-if-green.sh nhận ra PR đã vào nhánh chính khi API merge báo lỗi | `on-staging` | `done` (bản 2) | 5.0M | RUN-20261024-01 |
| [T-041](features/quality-gate/tickets/T-041-start-run-kiem-main-guard.md) | start-run.yml kiểm main-guard xanh trên commit đầu nhánh chính | `on-staging` | `done` (bản 1) | 5.0M | - |
| [T-042](features/quality-gate/tickets/T-042-run-check-phu-thuoc-file.md) | run-check.py báo plan phụ thuộc file của plan khác | `on-staging` | `done` (bản 1) | 5.0M | RUN-20261024-01 |
| [T-043](features/quality-gate/tickets/T-043-ap-dung-adr-1003.md) | Áp dụng ADR-1003 vào thang rủi ro và quy trình phiên tự chạy | `on-staging` | `done` (bản 1) | 5.0M | - |
| [T-044](features/quality-gate/tickets/T-044-wait-run-commit-hien-tai.md) | wait-run.sh --pr chỉ nhận lần chạy của commit đầu hiện tại | `on-staging` | `done` (bản 2) | 5.3M | RUN-20261024-02 |
| [T-045](features/quality-gate/tickets/T-045-hang-cho-plan.md) | Cuối phiên soạn sẵn plan cho ticket kế tiếp | `on-staging` | `done` (bản 2) | 10.4M | RUN-20261024-02 |
| [T-046](features/quality-gate/tickets/T-046-kiem-tay-tren-emulator.md) | Tiêu chí kiểm thủ công chạy được trên emulator thành kịch bản device-session | `on-staging` | `done` (bản 2) | 13M | RUN-20261024-02 |
| [T-047](features/quality-gate/tickets/T-047-san-sang-phat-hanh.md) | Mục "Sẵn sàng phát hành" trên trang tiến độ | `on-staging` | `done` (bản 2) | 5.3M | RUN-20261024-02 |
| [T-048](features/quality-gate/tickets/T-048-kich-ban-emulator-on-dinh.md) | Trình chạy kịch bản emulator chạy ổn định trên CI (sửa sau lần device-session đầu tiên) | `on-staging` | `done` (bản 1) | 9M | RUN-20261025-01 |
| [T-049](features/quality-gate/tickets/T-049-cham-sau-mo-gap.md) | Lệnh chạm sau `device unfold` tới được app; ba điểm còn lại của T-048 | `on-staging` | `done` (bản 1) | 7M | RUN-20261025-02 |
| [T-052](features/quality-gate/tickets/T-052-quy-tac-11-va-fresh-sau-tra-may.md) | Quy tắc 11 của plan-check thành lỗi; `fresh` sau khi trả máy không phải mở lại app | `on-staging` | `done` (bản 1) | 5M | RUN-20261026-01 |
| [T-053](features/quality-gate/tickets/T-053-token-phien-con.md) | Token của phiên con tính vào đúng task (dòng `Task: T-NNN` trong lời giao việc) | `on-staging` | `done` (bản 1) | 4.8M | RUN-20261026-01 |
| [T-054](features/quality-gate/tickets/T-054-trinh-chay-kich-ban-gms-ngon-ngu.md) | Trình chạy kịch bản chạy lại tệp khi app bị giết theo Google Play services, đặt app về tiếng Anh, ghi đúng thời gian chờ của `fresh` | `on-staging` | `done` (bản 1) | 10M | RUN-20261026-02 |
| [T-062](features/quality-gate/tickets/T-062-camera-gia-tren-emulator-ci.md) | Emulator của `device-session` có camera sau giả, để kịch bản camera chạy được trên CI | `on-staging` | `done` (bản 1) | 5.5M | RUN-20261027-01, RUN-20261027-02 |
| [T-063](features/quality-gate/tickets/T-063-token-phien-chinh-theo-task.md) | Token của phiên chính khi làm task trên HEAD tách rời tính vào đúng task (lệnh `token-usage.py mark`) | `on-staging` | `done` (bản 2) | 5.5M | RUN-20261027-01 |
| [T-065](features/quality-gate/tickets/T-065-setup-check-kiem-catalog-that.md) | "`setup-check` kiểm catalog tư thế mà máy tải (catalog mặc định và khóa `poses_catalog_path` trên Remote Config) cùng các file khởi đầu của nó" | `on-staging` | `done` (bản 2) | 7.6M | RUN-20261027-01, RUN-20261027-02 |
| [T-066](features/quality-gate/tickets/T-066-kha-nang-cach-mo-phien.md) | Kiểm khả năng của cách mở phiên trước khi duyệt và ở đầu phiên (`open_with`, bảng khả năng, `push-probe.sh`) | `on-staging` | `done` (bản 1) | 7.6M | RUN-20261027-02 |
| [T-067](features/quality-gate/tickets/T-067-tu-quyet-roi-bao.md) | Phiên tự quyết các lệch nhỏ đã liệt kê, bỏ qua loại task mà môi trường không làm được, rồi báo người sau | `on-staging` | `done` (bản 1) | 6.3M | RUN-20261027-02 |
| [T-068](features/quality-gate/tickets/T-068-patch-va-canh-bao-phien-dung.md) | Nhánh bị GitHub từ chối vì quyền workflow được giao cho người thành patch trên nhánh báo cáo; báo người khi phiên tự chạy đứng im (run-watchdog) | `on-staging` | `done` (bản 1) | 7.6M | RUN-20261027-02 |
| [T-069](features/quality-gate/tickets/T-069-local-setup.md) | "`scripts/local-setup.sh`: cài JDK và Android SDK trên máy dev để chạy thử bằng đúng `scripts/check.sh`" | `on-staging` | `done` (bản 1) | 6.3M | RUN-20261027-02 |
| [T-070](features/quality-gate/tickets/T-070-dong-bang-he-thong.md) | Trình chạy kịch bản đóng bảng của hệ thống che app (bảng thông báo, cài đặt nhanh) | `on-staging` | `done` (bản 1) | 8M | RUN-20261028-01 |
| [T-073](features/quality-gate/tickets/T-073-test-dau-chua-vao-logcat.md) | Test nhánh "dấu của lần chạy chưa vào logcat.txt" khi trình chạy kịch bản tìm app bị hệ thống giết | `on-staging` | `done` (bản 1) | 5.8M | RUN-20261028-02 |

Phản hồi: 4 `fixed`.

### settings

Spec: [spec.md](features/settings/spec.md) `confirmed`. Ticket: 4 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-025](features/settings/tickets/T-025-man-cai-dat.md) | Màn cài đặt | `on-staging` | `done` (bản 1) | 20.0M | RUN-20261023-01, RUN-20261023-02 |
| [T-050](features/settings/tickets/T-050-dich-chu-camera.md) | Chữ của camera và màn xem lại theo ngôn ngữ đã chọn | `on-staging` | `done` (bản 1) | 8M | RUN-20261026-01 |
| [T-051](features/settings/tickets/T-051-dich-chu-thu-vien-va-thiet-ke.md) | Chữ của thư viện tư thế và hệ thống thiết kế theo ngôn ngữ đã chọn | `on-staging` | `done` (bản 1) | 8M | RUN-20261026-01 |
| [T-055](features/settings/tickets/T-055-test-bo-cuc-ban-dich.md) | Bố cục và nghĩa của bản dịch ở bảng chỉnh nét viền và nút chụp; "Độ mờ" thành "Độ đậm" | `on-staging` | `done` (bản 1) | 10M | RUN-20261026-02 |

Phản hồi: chưa có.

## 4. Phiên tự chạy

| Phiên | Trạng thái | Bắt đầu | Ticket | Bản staging |
| --- | --- | --- | --- | --- |
| [RUN-20261031-01](runs/RUN-20261031-01.md) | `approved` |  | T-079, T-079 | - |
| [RUN-20261030-02](runs/RUN-20261030-02.md) | `finished` | 2026-10-30T09:04:19Z | T-078 | 0.1.0-staging.19 |
| [RUN-20261030-01](runs/RUN-20261030-01.md) | `finished` | 2026-10-30T02:20:31Z | T-076, T-077 | 0.1.0-staging.18 |
| [RUN-20261029-01](runs/RUN-20261029-01.md) | `finished` | 2026-10-29T06:31Z | T-074, T-074, T-074 | 0.1.0-staging.17 |
| [RUN-20261028-02](runs/RUN-20261028-02.md) | `finished` | 2026-10-28T15:48Z | T-072, T-073, T-071 | 0.1.0-staging.16 |
| [RUN-20261028-01](runs/RUN-20261028-01.md) | `finished` | 2026-10-28T10:56Z | T-070 | 0.1.0-staging.15 |
| [RUN-20261027-02](runs/RUN-20261027-02.md) | `finished` | 2026-10-27T17:28Z | T-067, T-069, T-066, T-068, T-062, T-065 | 0.1.0-staging.14 |
| [RUN-20261027-01](runs/RUN-20261027-01.md) | `finished` | 2026-10-27T04:53:54Z | T-063, T-062, T-064, T-065 | 0.1.0-staging.13 |
| [RUN-20261026-03](runs/RUN-20261026-03.md) | `finished` | 2026-10-26T09:28Z | T-058, T-060, T-059, T-061 | 0.1.0-staging.12 |
| [RUN-20261026-02](runs/RUN-20261026-02.md) | `finished` | 2026-10-26T03:52Z | T-054, T-055, T-057, T-056 | 0.1.0-staging.11 |
| [RUN-20261026-01](runs/RUN-20261026-01.md) | `finished` | 2026-10-25T17:30Z | T-053, T-052, T-050, T-051 | 0.1.0-staging.10 |
| [RUN-20261025-02](runs/RUN-20261025-02.md) | `finished` | 2026-10-25T15:16:06Z | T-049 | 5ce6f51 |
| [RUN-20261025-01](runs/RUN-20261025-01.md) | `finished` | 2026-10-25T08:18Z | T-048 | bb3b286 |
| [RUN-20261024-02](runs/RUN-20261024-02.md) | `finished` | 2026-10-24T15:11Z | T-044, T-045, T-046, T-047 | 5443ecd |
| [RUN-20261024-01](runs/RUN-20261024-01.md) | `finished` | 2026-10-24T06:33Z | T-036, T-039, T-037, T-042, T-040 | 0.1.0-staging.6 (6) |
| [RUN-20261023-04](runs/RUN-20261023-04.md) | `finished` | 2026-10-23 17:16 UTC | T-022, T-018, T-015, T-023, T-024, T-017, T-020, T-019 | 0.1.0-staging.5 (5) |
| [RUN-20261023-03](runs/RUN-20261023-03.md) | `finished` | 2026-10-23T16:53Z | T-022, T-018, T-015, T-023, T-024, T-017, T-020, T-019 | - |
| [RUN-20261023-02](runs/RUN-20261023-02.md) | `finished` | 2026-10-22T23:14Z | T-012, T-014, T-021, T-016, T-025 | 0.1.0-staging.4 (4), đã phát tới nhóm qua App Distribution |
| [RUN-20261023-01](runs/RUN-20261023-01.md) | `finished` | 2026-10-22T15:45Z | T-006, T-026, T-012, T-014, T-021, T-016, T-025 | - |
| [RUN-20261022-01](runs/RUN-20261022-01.md) | `finished` | 2026-10-22T01:29Z | T-011, T-010, T-008, T-013 | 0.1.0-staging.3 (3), đã tải lên App Distribution, chưa phát tới nhóm (nhóm `team` chưa có) |
| [RUN-20261021-04](runs/RUN-20261021-04.md) | `draft` |  | T-008, T-011, T-010, T-013 | - |
| [RUN-20261021-03](runs/RUN-20261021-03.md) | `finished` | 2026-10-21T15:01Z | T-007, T-011 | - |
| [RUN-20261021-02](runs/RUN-20261021-02.md) | `finished` | 2026-10-21T14:14Z | T-007, T-011 | không phát (không có task nào merge) |
| [RUN-20261021-01](runs/RUN-20261021-01.md) | `finished` | 2026-10-21T10:30Z | T-007, T-009, T-011 | không phát được (xem mục Bản staging) |

## 5. Bài học và ADR

- Bài học ([lessons.md](lessons.md)): 96 bài; 84 `applied`, 8 `accepted`, 4 `verified`.
- ADR: 42; 33 `accepted`, 7 `superseded`, 2 `proposed`.

## 6. Sẵn sàng phát hành

<!-- Chỉ từ tài liệu trong repo. Mã quảng cáo thật, cấu hình production và tình trạng trên Google Play
không nằm trong repo: người phát hành kiểm theo docs/ops/release.md. -->

### Quảng cáo

- Bản production cần một mã quảng cáo thật cho mỗi khóa dưới đây (docs/ops/release.md, mục 1.2): `sample_native_small` (native_small), `open_app` (app_open), `open_app_inter` (interstitial), `native_combo` (native_full_screen), `resume` (app_open), `resume_inter` (interstitial), `poses_banner` (banner), `poses_detail_banner` (banner), `review_interstitial` (interstitial).

### Kiểm thủ công chưa đánh dấu

97 mục trong 20 phiên. Người kiểm xong thì đánh dấu [x] trong báo cáo phiên.
- [RUN-20261030-02](runs/RUN-20261030-02.md): Tiêu chí kiểm tra thủ công (máy thật): T-078, mục 4 của plan, dòng cuối, bốn bước trên bản staging, cờ `feature_network_dialog_enabled` bật, máy nối USB,...
- [RUN-20261030-02](runs/RUN-20261030-02.md): Phát hiện của `device-session`: FB-20261030-01 (`native_combo` tải lỗi mã 0, thử lại mỗi 5 giây, 75 cảnh báo), mức `nhe`, chủ feature ads phân loại
- [RUN-20261030-02](runs/RUN-20261030-02.md): Quyết định AI đã tự làm: 2, xem mục trên
- [RUN-20261030-02](runs/RUN-20261030-02.md): Điểm rà soát mức ghi chú, T-078: (1) `foregroundJob` có hai nơi ghi (luồng chính trong `onAppStarted`, scope của controller trong collector và `disarm`), cặp...
- [RUN-20261030-01](runs/RUN-20261030-01.md): Tiêu chí kiểm tra thủ công (máy thật, người), T-076: (1) điện thoại nối Wi-Fi của router đã rút dây WAN, tắt dữ liệu di động, mở app qua loading khi còn mạng...
- [RUN-20261030-01](runs/RUN-20261030-01.md): Tiêu chí kiểm tra thủ công (máy thật), T-077, mục 4 của plan: (1) mở camera, Home, tắt Wi-Fi và dữ liệu, mở lại trong vòng 3 giây: hộp thoại hiện trong khoảng...
- [RUN-20261030-01](runs/RUN-20261030-01.md): Kịch bản emulator lỗi hay bỏ qua: xem mục Kiểm thử trên máy ảo.
- [RUN-20261030-01](runs/RUN-20261030-01.md): Quyết định AI đã tự làm: 1, xem mục trên.
- [RUN-20261030-01](runs/RUN-20261030-01.md): Điểm rà soát mức ghi chú: T-076: (1) phần (3) của kịch bản bấm "Thử lại" ngay sau `network on`, chập chờn thì xem chỗ này trước (cách xếp lỗi theo quyết định...
- [RUN-20261030-01](runs/RUN-20261030-01.md): Ticket riêng cho khoảng hở đã chấp nhận của T-077 (hệ thống đóng tiến trình khi app ở nền rồi mở lại: hộp thoại hiện sau 3 giây thay vì 2): chưa soạn trong...
- [RUN-20261029-01](runs/RUN-20261029-01.md): Ảnh chuẩn screenshot mới: 41 ảnh `core/ads/src/test/screenshots/` (lượt 1), trong đó 8 ảnh `NativeFlowStep_*.png` lệch khoảng 4 dp vì nút 56 dp của app.
- [RUN-20261029-01](runs/RUN-20261029-01.md): Tiêu chí kiểm tra thủ công (máy thật, người): T-074, `docs/ops/manual-test.md` mục 11: danh sách kiểm Firebase staging trước khi phát; banner ở thư viện và chi...
- [RUN-20261029-01](runs/RUN-20261029-01.md): Kịch bản emulator lỗi hay bỏ qua: xem mục Kiểm thử trên máy ảo.
- [RUN-20261029-01](runs/RUN-20261029-01.md): Điểm rà soát mức ghi chú: T-074: khe tranh chấp hẹp trong `NetworkDialogController` (kiểm `armed` trước khi hiện); chặn của In-App Review không có hạn khi Play...
- [RUN-20261029-01](runs/RUN-20261029-01.md): `CLAUDE.md` gốc đã cũ sau T-074: dòng `:core:analytics`, `:core:ads` (ADR-1004), mục 7 (`ads_<vị trí>_enabled`), mục 9 ("khi app đã có `AppRemoteKeys`"). Cần...
- [RUN-20261029-01](runs/RUN-20261029-01.md): Môi trường cloud: hai kho Maven `artifact.bytedance.com`, `dl-maven-android.mintegral.com` (plan câu 12) trước phiên `cloud` hay `routine` tiếp theo.
- [RUN-20261028-02](runs/RUN-20261028-02.md): Tiêu chí kiểm tra thủ công (máy thật, người): T-071, bản staging mới: thư viện hiện 350 tư thế, 7 nhóm (nhóm `kids` không có tư thế nên không hiện), không còn...
- [RUN-20261028-02](runs/RUN-20261028-02.md): Phần ngoài mục 3.2 đã merge (ADR-0029): T-071, `scripts/tests/app.sh` (dữ liệu test của setup-check: catalog giả chỉ Remote Config trỏ tới chuyển sang `v9/`),...
- [RUN-20261028-02](runs/RUN-20261028-02.md): Câu khẳng định cũ viết lại (ADR-0031): T-071, `StagingSampleCatalogTest.defaultCatalogPath_isV1Folder_andEndsWithFirst8HexOfSha256OfStagingSample`, câu cũ `v1`...
- [RUN-20261028-02](runs/RUN-20261028-02.md): Quyết định AI đã tự làm: 3, xem mục trên
- [RUN-20261028-02](runs/RUN-20261028-02.md): Điểm rà soát mức ghi chú: T-072, `LoadingStringsTest` không chuẩn hóa NFC; nhánh `CancellationException` khi coroutine còn chạy không ghi log (code cũ). T-073,...
- [RUN-20261028-01](runs/RUN-20261028-01.md): Quyết định AI đã tự làm: 3, xem mục trên
- [RUN-20261028-01](runs/RUN-20261028-01.md): Điểm rà soát mức ghi chú: T-070, 5 ghi chú (nhật ký plan): đọc lại ngay sau khi đóng bảng có thể gặp bảng đóng dở (chỉ `expect` đọc một lần); màn hình khóa hay...
- [RUN-20261027-02](runs/RUN-20261027-02.md): Tiêu chí kiểm tra thủ công (máy thật, người): (4 dòng con)
- [RUN-20261027-02](runs/RUN-20261027-02.md): Dọn nhánh (L-092 của mẫu: `merge-if-green.sh` và `push-probe.sh` không xóa được nhánh): `claude/T-067-tu-quyet-roi-bao`, `claude/T-069-local-setup`,...
- [RUN-20261027-02](runs/RUN-20261027-02.md): Mở issue `from-app` ở VGL-Studio/app-project-template (`docs/ops/from-app.md`; phiên không mở issue ở repo khác): T-067 (loại `bo-qua-loai`, PB-13,...
- [RUN-20261027-02](runs/RUN-20261027-02.md): Kịch bản emulator lỗi hay bỏ qua: cả sáu tệp `lỗi` ở bước 2 `consent decline 30` vì bảng cài đặt nhanh của hệ thống che app (lỗi của trình chạy), phản hồi...
- [RUN-20261027-02](runs/RUN-20261027-02.md): Phần ngoài mục 3.2 đã merge: T-062, ADR-1005/L1 và L2 (không phải ADR-0029): `scripts/device-scenario.py`, `scripts/device-session.sh`,...
- [RUN-20261027-02](runs/RUN-20261027-02.md): Điểm rà soát mức ghi chú: xem dòng Cần người xem trong nhật ký từng plan. Đáng chú ý: T-069, test `env.sh gives a Java ...` phụ thuộc Java của máy chạy test;...
- [RUN-20261027-02](runs/RUN-20261027-02.md): Quyết định AI đã tự làm: 7, xem mục dưới.
- [RUN-20261027-01](runs/RUN-20261027-01.md): Tiêu chí kiểm tra thủ công (người): T-063, mục Token của báo cáo này (dòng T-064 có phần phiên chính). Kết quả của phiên ở mục Token: đạt.
- [RUN-20261027-01](runs/RUN-20261027-01.md): Kịch bản emulator lỗi hay bỏ qua: `T-020-camera-mo-gap.txt`, `T-058-chup-tu-chi-tiet.txt`, `T-064-mot-camera.txt`: `bỏ qua (môi trường)` ở bước...
- [RUN-20261027-01](runs/RUN-20261027-01.md): Điểm rà soát mức ghi chú: T-063: request trùng `requestId` giữa phiên chính và phiên con thì bản phiên con thắng (có từ trước); `report` không có `--since` thì...
- [RUN-20261026-02](runs/RUN-20261026-02.md): Ảnh chuẩn screenshot ghi lại (T-055, plan cho phép): `feature/camera/src/test/screenshots/CameraScreen_adjust_dark.png`, `CameraScreen_adjust_small.png` (nhãn...
- [RUN-20261026-02](runs/RUN-20261026-02.md): Ảnh chuẩn screenshot mới (T-057): `feature/loading/src/test/screenshots/LoadingScreen_failed-server_light.png`, `LoadingScreen_failed-server_small.png` (lời...
- [RUN-20261026-02](runs/RUN-20261026-02.md): Ảnh so sánh với thiết kế: camera, `docs/features/camera/design/review/CameraScreen_adjust_dark_compare.png` sinh lại, tỷ lệ giữ 7,3% (rà soát: phần khác đến từ...
- [RUN-20261026-02](runs/RUN-20261026-02.md): Tiêu chí kiểm tra thủ công (máy thật, người): (5 dòng con)
- [RUN-20261026-02](runs/RUN-20261026-02.md): Kịch bản emulator lỗi hay bỏ qua: không tệp nào `lỗi`. `feature/camera/src/deviceScenarios/T-020-camera-mo-gap.txt` bỏ qua (môi trường) ở bước 6...
- [RUN-20261026-02](runs/RUN-20261026-02.md): Điểm rà soát mức nên sửa không sửa được và ghi chú: (4 dòng con)
- [RUN-20261026-01](runs/RUN-20261026-01.md): Ảnh chuẩn screenshot mới: T-050 `feature/camera/src/test/screenshots/ReviewScreen_save-error-pt-br_small.png` (màn xem lại báo lỗi lưu, tiếng Bồ Đào Nha, khung...
- [RUN-20261026-01](runs/RUN-20261026-01.md): Ảnh so sánh với thiết kế: `docs/features/camera/design/review/` và `docs/features/poses/design/review/` sinh lại không đổi, vì mọi ảnh chuẩn cũ giữ nguyên...
- [RUN-20261026-01](runs/RUN-20261026-01.md): Bản dịch do AI làm (spec onboarding, câu hỏi 1): người biết tiếng đọc `values-hi`, `values-pt-rBR`, `values-b+es+419`, `values-in`, `values-th` của...
- [RUN-20261026-01](runs/RUN-20261026-01.md): Tiêu chí kiểm tra thủ công (máy thật): T-050, T-051 (KB-4): trên bản staging, đổi app sang English, Português, हिन्दी, rồi Tiếng Việt (cài đặt máy trên Android...
- [RUN-20261026-01](runs/RUN-20261026-01.md): Tiêu chí kiểm tra thủ công (người): T-052 người duyệt chéo đọc diff `docs/ops/test-sessions.md` mục 5, `docs/CLAUDE.md`, `docs/templates/plan.md` (PR #144)....
- [RUN-20261026-01](runs/RUN-20261026-01.md): Kịch bản emulator lỗi hay bỏ qua: `T-022-chi-tiet-sang-camera.txt` lỗi ở bước 2 (`consent decline 30`): Google Play services chết trên emulator và hệ thống...
- [RUN-20261026-01](runs/RUN-20261026-01.md): Điểm rà soát mức ghi chú: T-053 nên sửa không sửa vì mâu thuẫn với plan: dòng `Task:` trong lời giao soạn plan làm lệch mẫu ước tính (L-079); test chưa phủ...
- [RUN-20261025-02](runs/RUN-20261025-02.md): Tiêu chí kiểm tra thủ công (người): T-049, "Phiên làm theo tài liệu": người duyệt chéo đọc diff `docs/ops/test-sessions.md` mục 5 trong PR #136 (quy tắc số mơ...
- [RUN-20261025-02](runs/RUN-20261025-02.md): Kịch bản emulator bỏ qua: `feature/camera/src/deviceScenarios/T-020-camera-mo-gap.txt`, `bỏ qua (môi trường)` ở bước 6 (`need id=camera_grid_button`, emulator...
- [RUN-20261025-02](runs/RUN-20261025-02.md): Phản hồi mới FB-20261025-02 (mức nhẹ): `fresh` của tệp kịch bản thứ hai và thứ ba phải mở lại; chủ quality-gate phân loại.
- [RUN-20261025-02](runs/RUN-20261025-02.md): Điểm rà soát mức ghi chú (T-049, PR #136): (5 dòng con)
- [RUN-20261025-01](runs/RUN-20261025-01.md): Tiêu chí kiểm tra thủ công (người): T-048, "Phiên làm theo tài liệu": người duyệt chéo đọc diff `docs/ops/test-sessions.md` mục 5 (PR #129).
- [RUN-20261025-01](runs/RUN-20261025-01.md): Kịch bản emulator lỗi hay bỏ qua: (2 dòng con)
- [RUN-20261025-01](runs/RUN-20261025-01.md): Điểm rà soát mức nên sửa, chưa sửa: T-048. `docs/ops/test-sessions.md` mục 5, docstring đầu `scripts/device-scenario.py` và câu báo lỗi chỉ nói "giá trị có dấu...
- [RUN-20261025-01](runs/RUN-20261025-01.md): Điểm rà soát mức ghi chú: T-048. (7 dòng con)
- [RUN-20261024-02](runs/RUN-20261024-02.md): Tiêu chí kiểm tra thủ công: T-044 xem diff `docs/auto-run.md` bước 8 và dòng thời gian bên dưới (phiên đã chờ CI bằng `--sha "$(git rev-parse HEAD)"`); T-045...
- [RUN-20261024-02](runs/RUN-20261024-02.md): Điểm rà soát mức ghi chú, nên sửa chưa sửa (T-046): (1) ba kịch bản chưa kiểm hết tiêu chí của ticket T-020, T-022, T-024, đúng theo mục 3.1 của plan (T-022...
- [RUN-20261024-02](runs/RUN-20261024-02.md): Điểm rà soát mức ghi chú (T-047): `is_not_a_task` coi `<->` hay `a < b ... c > d` là chữ mẫu; regex `issue_links` không neo đầu chuỗi (không rò thông tin);...
- [RUN-20261024-02](runs/RUN-20261024-02.md): Điểm rà soát mức ghi chú (T-044, T-045): nhánh `gh` thoát lỗi trong `read_head` chưa có test riêng (`fake-gh` không có route lỗi); `plan-queue.py` không phân...
- [RUN-20261024-02](runs/RUN-20261024-02.md): Ảnh chuẩn hộp thoại mạng: `NetworkDialogHost` có thêm một `Box` bọc ngoài (T-046); `check.sh` và CI không báo lệch ảnh chuẩn.
- [RUN-20261024-01](runs/RUN-20261024-01.md): Tiêu chí kiểm tra thủ công: `docs/ops/failure-playbook.md`, cột "Việc phải làm", nhất là PB-01 (tối đa 2 lần chạy lại) và PB-08. PB-09 chưa có log thật.
- [RUN-20261024-01](runs/RUN-20261024-01.md): Điểm rà soát mức ghi chú: T-040 không viết test `merge-if-green sees an exact revert merged despite an error` của bảng L-044 (lý do trong nhật ký plan). T-042:...
- [RUN-20261024-01](runs/RUN-20261024-01.md): RUN-20261022-01 vẫn có lỗi L-060 (T-008/T-010) với `run-check.py` mới; chỉ ảnh hưởng khi chạy lại danh sách cũ đó.
- [RUN-20261024-01](runs/RUN-20261024-01.md): Dòng Áp dụng của các bài học L-051, L-054, L-056, L-057, L-060 đã ghi tên task từ trước; retro cập nhật `applied` và số PR (#103 đến #107).
- [RUN-20261024-01](runs/RUN-20261024-01.md): Đưa `merge-if-green.sh`, `fake-gh`, `save-branch.sh` về dự án mẫu qua issue `from-app` (T-040 câu 4); cần phiên có người theo dõi.
- [RUN-20261023-04](runs/RUN-20261023-04.md): Nhánh T-017: xem Task bị dừng.
- [RUN-20261023-04](runs/RUN-20261023-04.md): Ảnh chuẩn screenshot mới: poses `PoseDetailScreen_{default,loading,error}_{light,dark}`, `PoseDetailScreen_{default,error}_small` (T-022);...
- [RUN-20261023-04](runs/RUN-20261023-04.md): Ảnh so sánh với thiết kế: `docs/features/poses/design/review/` (T-022: 3,7% đến 18%; T-023: 13,9% đến 22,4%; T-024: thư viện 23,5%, chi tiết 3,7%, thẻ 3 cột...
- [RUN-20261023-04](runs/RUN-20261023-04.md): Tiêu chí kiểm tra thủ công (staging, máy thật): T-022 luồng 7 bước của mục 4 (chạm thẻ, cuộn, lưu, "Chụp với tư thế này", chế độ máy bay, tối, tiếng Anh);...
- [RUN-20261023-04](runs/RUN-20261023-04.md): Điểm rà soát chưa sửa (mức nên sửa): T-018 hủy composition khi chờ quảng cáo làm `onFinished` không được gọi (màn xem lại đứng yên), và `ReviewRouteTest` không...
- [RUN-20261023-04](runs/RUN-20261023-04.md): Đề xuất về dự án mẫu (`docs/ops/from-app.md`): thay đổi `AdSlot` (`separator`, thu về 0 dp khi lỗi, T-023) và module `:core:review` (T-019)....
- [RUN-20261023-02](runs/RUN-20261023-02.md): Ảnh chuẩn screenshot mới hoặc ghi lại: (4 dòng con)
- [RUN-20261023-02](runs/RUN-20261023-02.md): Ảnh so sánh với thiết kế: (3 dòng con)
- [RUN-20261023-02](runs/RUN-20261023-02.md): Quyết định của người: (4 dòng con)
- [RUN-20261023-02](runs/RUN-20261023-02.md): Tiêu chí kiểm tra thủ công (mục 4 của từng plan): (5 dòng con)
- [RUN-20261023-02](runs/RUN-20261023-02.md): Điểm rà soát mức ghi chú: (5 dòng con)
- [RUN-20261023-02](runs/RUN-20261023-02.md): Bản dịch: chữ mới của màn cài đặt và màn chào ở 6 ngôn ngữ do AI viết theo bảng của plan; chữ của camera, thư viện, xem lại chỉ có tiếng Việt như các màn...
- [RUN-20261023-01](runs/RUN-20261023-01.md): **Issue #35 `main-broken`** (e9bf4bb, T-026): `CameraScreenScreenshotTest > starting_dark` thất bại trên `main-guard` lần chạy 39817360705 với...
- [RUN-20261023-01](runs/RUN-20261023-01.md): Ảnh chuẩn screenshot mới (PR #36, chưa merge): `feature/onboarding/src/test/screenshots/OnboardingScreen_default_light.png`, `_default_dark.png`,...
- [RUN-20261023-01](runs/RUN-20261023-01.md): Ảnh so sánh với thiết kế (PR #36): `docs/features/onboarding/design/review/`, 4,3% khác ở cả hai ảnh; rà soát độc lập: đúng chữ, thứ tự, vai trò màu; lệch ở...
- [RUN-20261023-01](runs/RUN-20261023-01.md): Bản dịch 6 ngôn ngữ của màn chào (PR #36) do AI viết, khớp bảng plan 49/49 chuỗi.
- [RUN-20261023-01](runs/RUN-20261023-01.md): Tiêu chí kiểm tra thủ công T-026 (đã merge): bốn bước plan mục 4 trên máy thật, thêm (5) lần hỏi thứ hai, xoay máy khi hộp đang mở rồi từ chối, và (6) như (5)...
- [RUN-20261023-01](runs/RUN-20261023-01.md): Điểm rà soát mức ghi chú: T-026, test JVM cho `CameraRoute` bằng `ActivityResultRegistry` giả (ticket sau). T-006, test 1 không có khoảng dư ở đáy (480 ≤ 480)....
- [RUN-20261023-01](runs/RUN-20261023-01.md): Spec: dòng lịch sử ngày 2026-10-22 của spec `camera` và spec `onboarding` còn ghi "Chờ xác nhận lại" trong khi `status` là `confirmed`.
- [RUN-20261022-01](runs/RUN-20261022-01.md): Điểm rà soát mức ghi chú: (4 dòng con)
- [RUN-20261021-03](runs/RUN-20261021-03.md): T-007, ảnh chuẩn đã vào nhánh chính (PR #19): 15 ảnh ghi lại, 4 ảnh catalog mới. Xem `DesignSystem_typography_light.png` (nét Bricolage, đậm 800) và...
- [RUN-20261021-03](runs/RUN-20261021-03.md): T-007, ghi chú của rà soát: nền màn loading đang là `surface` (#FFFFFF, #1E1E1E) thay vì `background` (#FFFDF8, #121212) của thiết kế, do `LoadingScreen.kt`...
- [RUN-20261021-03](runs/RUN-20261021-03.md): T-007, ghi chú: `secondaryButton_hasNoShadow` chỉ khẳng định điểm dưới nút khác #FFC83D; có thể làm chặt hơn sau.
- [RUN-20261021-03](runs/RUN-20261021-03.md): T-007, việc sau merge trong phiên có người theo dõi (plan T-007, mục 6): biến Figma, `scripts/figma-export.py` cho sáu gói thiết kế, đoạn "Hệ thống thiết kế...
- [RUN-20261021-03](runs/RUN-20261021-03.md): T-011: chọn hướng (a), (b) hoặc (c) trong nhật ký plan T-011 (bản sửa đề xuất, phiên bản 2).
- [RUN-20261021-03](runs/RUN-20261021-03.md): Bí mật `STAGING_GOOGLE_SERVICES_JSON`: thay bằng file của project Firebase staging thật có app `...guide.staging` (L-002), rồi chạy `staging.yml` để phát T-007.
- [RUN-20261021-02](runs/RUN-20261021-02.md): Chọn hướng (a), (b) hoặc (c) trong plan T-007 phiên bản 3, rồi duyệt.
- [RUN-20261021-02](runs/RUN-20261021-02.md): Ảnh chuẩn screenshot mới và đổi trên nhánh T-007 (chưa vào nhánh chính): 15 ảnh đổi, 4 ảnh tạo mới, xem `core/designsystem/src/test/screenshots/`,...
- [RUN-20261021-01](runs/RUN-20261021-01.md): Bí mật `STAGING_GOOGLE_SERVICES_JSON`: thay bằng `google-services.json` của project Firebase staging có app `com.vglstudio.camera.pose.line.guide.staging`, rồi...
- [RUN-20261021-01](runs/RUN-20261021-01.md): Tiêu chí kiểm tra thủ công của T-009: sau khi đưa catalog mẫu lên R2 staging, `curl -I` địa chỉ staging cộng `poses/catalog/v1/catalog-587b5394.json` và...
- [RUN-20261021-01](runs/RUN-20261021-01.md): Việc dev làm sau T-009: đưa `core/poses/src/test/resources/catalog/staging-sample.json` lên R2 với tên `catalog-587b5394.json` và 18 ảnh mượn...
- [RUN-20261021-01](runs/RUN-20261021-01.md): Điểm rà soát mức ghi chú của T-009: `api(project(":core:assets"))` thay `implementation`; `onProgress` của `loadStarterSet` chạy trong khóa trên luồng bất kỳ;...
- [RUN-20261021-01](runs/RUN-20261021-01.md): Plan T-007 phiên bản 2 (`draft` trong nhật ký plan): chọn cách đo màu điểm ảnh (a, b hoặc c), rồi duyệt.

### Ticket chưa lên staging

3 ticket (gồm ticket quy trình).
- ads: T-075 `backlog`, T-079 `approved`, T-080 `backlog`

### Phản hồi đang mở

- FB-20261022-03 `planned`, mức `gop-y`: Màn loading bảo kiểm tra mạng khi máy có mạng nhưng máy chủ không tới được

### Issue đang mở

- [Mọi issue đang mở](https://github.com/VGL-Studio/camera-pose/issues?q=is%3Aissue+is%3Aopen), [issue main-broken](https://github.com/VGL-Studio/camera-pose/issues?q=is%3Aissue+is%3Aopen+label%3Amain-broken)
