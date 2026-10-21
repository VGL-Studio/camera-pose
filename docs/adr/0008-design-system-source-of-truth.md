---
id: ADR-0008
title: Hệ thống thiết kế lấy code làm gốc, Claude Design đồng bộ từ code
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-14
deciders: [Nhóm phát triển, Người phụ trách sản phẩm]
---

# ADR-0008: HỆ THỐNG THIẾT KẾ LẤY CODE LÀM GỐC, CLAUDE DESIGN ĐỒNG BỘ TỪ CODE

## Bối cảnh

Nhóm không có designer và dùng Claude Design để dựng bản mẫu giao diện. Hệ thống thiết kế (màu, chữ, thành phần) tồn tại ở hai nơi: trong code và trên Claude Design. AI trong phiên tự chạy đọc code, nên nếu hai nơi lệch nhau, giao diện làm ra sẽ khác bản mẫu đã duyệt.

## Quyết định

- Module `:core:designsystem` (theme Compose và các thành phần dùng chung) là bản gốc của hệ thống thiết kế.
- Claude Design đồng bộ hệ thống thiết kế từ repo. Khi hai nơi lệch nhau, code thắng.
- Bản mẫu Claude Design là ý đồ thiết kế. AI viết lại bằng Compose theo hệ thống thiết kế, không chép code web của bản mẫu.
- Gói chuyển giao và ảnh màn hình đã duyệt lưu trong `docs/features/<feature>/design/`.
- Screenshot test trong CI giữ cho giao diện đã duyệt không bị thay đổi ngoài ý muốn.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Code làm gốc | AI và giao diện thật luôn khớp | Thay đổi hệ thống thiết kế phải đi qua plan và code |
| Claude Design làm gốc | Thiết kế linh hoạt hơn | Code dễ lệch, AI không đọc trực tiếp được |

## Hệ quả

- **Tích cực:** giao diện nhất quán giữa các feature và giữa các app clone từ dự án mẫu.
- **Phải chấp nhận:** thêm thành phần mới vào hệ thống thiết kế là thay đổi module core, rủi ro cao.
- **Việc AI phải tuân thủ:** chỉ dùng màu, chữ, khoảng cách, thành phần từ `:core:designsystem`; không viết cứng mã màu hay cỡ chữ trong feature.
