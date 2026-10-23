---
id: FB-20261022-04
title: Log tải file thất bại không cho biết lý do cụ thể
reporter: AI (phân tích phiên thử TS-20261022-02)
source: staging
build: 0.1.0-staging.3
feature: loading
ticket: T-032                # trước đây T-010 (ticket gây ra log cũ)
severity: gop-y
status: fixed
created: 2026-10-22
---

# FB-20261022-04: LOG TẢI FILE THẤT BẠI KHÔNG CHO BIẾT LÝ DO CỤ THỂ

## Điều đã xảy ra

Khi tải catalog thất bại, log chỉ có `Assets: Could not download <đường dẫn>` và `VGL.Poses: Could not load catalog: network`. Lỗi được truyền vào `Log.w` (`core/assets/.../di/AssetsModule.kt:36`) nhưng Android bỏ stack trace của `UnknownHostException`, nên log không cho biết là mất mạng, không phân giải được tên miền, hết giờ hay lỗi HTTP. Phải kiểm DNS bằng tay mới tìm ra nguyên nhân (TS-20261022-02, P-1).

## Các bước làm lại

1. Làm cho tên miền file tĩnh không phân giải được (FB-20261022-02).
2. Mở app lần đầu, xem `adb logcat | grep -E ' VGL\.| Assets'`.

## Mong đợi

Một dòng log cho biết loại lỗi (ví dụ `cause=UnknownHostException`) và mã HTTP nếu có, không chứa dữ liệu cá nhân (ADR-0019).

## Ảnh chụp, video

Không có.

## Thiết bị

- Máy: Xiaomi 23021RAAEG
- Phiên bản Android: 15 (API 35)
- Ngôn ngữ, chế độ tối: không ghi

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

Đề xuất của AI: góp ý mới cho `:core:assets`, phần dùng chung; làm ở dự án mẫu rồi kéo về (`docs/ops/from-app.md`).

- **Loại:** góp ý mới
- **Xử lý:** sửa trong sprint này, làm ở app trước (plan T-032 rủi ro cao, Nhật duyệt, Tuấn duyệt chéo), đề xuất về dự án mẫu sau
- **Lý do:** log không cho biết lý do tải file thất bại làm mất thời gian tìm lỗi DNS (TS-20261022-02, P-1; L-027)
- **Liên kết:** T-032, PR #51 (đã merge 2026-10-23). Tuấn chuyển `fixed` ngày 2026-10-23; kiểm tay trên staging còn chờ (plan T-032, mục 10)
