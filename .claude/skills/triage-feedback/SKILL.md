---
name: triage-feedback
description: Đề xuất phân loại các phản hồi mới (lỗi so với spec, thay đổi yêu cầu, góp ý mới), chuyển phản hồi từ inbox về feature, và nêu bước tiếp cho từng loại. Dùng sau khi dev thử staging, đọc báo cáo phiên hay phân tích phiên thử.
argument-hint: "[feature hoặc FB-...]"
---

# /triage-feedback: phân loại phản hồi

Tham số: `$ARGUMENTS` (tùy chọn: feature hay mã phản hồi; mặc định mọi phản hồi `new`).

1. Đọc `docs/CLAUDE.md`, mục Khi xử lý phản hồi, và `docs/templates/feedback.md`.
2. Liệt kê phản hồi `new` trong `docs/features/*/feedback/` và `docs/inbox/`.
3. Với mỗi phản hồi, đề xuất: feature, loại (lỗi so với spec, thay đổi yêu cầu, góp ý mới), lý do ngắn, bước tiếp:
   - Lỗi so với spec: plan phiên bản mới có mục sửa lỗi (`/write-plan`), qua họp đánh giá rủi ro.
   - Thay đổi yêu cầu: đề xuất cập nhật spec (`/spec`), chờ người phụ trách sản phẩm xác nhận; không sửa thẳng code.
   - Góp ý mới: yêu cầu mới hay ticket mới.
4. Đưa dev bảng đề xuất. Chủ feature quyết; chỉ ghi trạng thái `triaged` và loại sau khi dev đồng ý. Mở PR cho các thay đổi.
