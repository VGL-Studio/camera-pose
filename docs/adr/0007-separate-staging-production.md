---
id: ADR-0007
title: Tách hai môi trường staging và production cho mọi dịch vụ bên ngoài
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-14
deciders: [Nhóm phát triển]
---

# ADR-0007: TÁCH HAI MÔI TRƯỜNG STAGING VÀ PRODUCTION CHO MỌI DỊCH VỤ BÊN NGOÀI

## Bối cảnh

App không có backend, nên "staging" là một bản build phát cho nhóm. Nhưng app vẫn dùng dịch vụ bên ngoài: Firebase, mạng quảng cáo, R2. Nếu bản test dùng chung cấu hình với bản thật, số liệu bị lẫn, và nhóm có thể vô tình bấm quảng cáo thật.

## Quyết định

| Dịch vụ | Staging | Production |
| --- | --- | --- |
| Firebase | Project riêng | Project riêng |
| Quảng cáo | Mã thử nghiệm | Mã thật |
| R2 | Bucket hoặc thư mục staging | Bucket production |

- Hai môi trường gắn với hai biến thể build (build flavor) `staging` và `production`.
- Cấu hình production (file cấu hình Firebase, mã quảng cáo, khóa truy cập R2) không nằm trong repo hay trên GitHub, được đưa vào ở bước build phát hành do người thực hiện trên máy của mình (xem ADR-0009).
- Bản staging được phân phối qua Firebase App Distribution hoặc kênh Internal testing của Google Play.
- Khóa ký ứng dụng giao cho Play App Signing quản lý.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Tách hai môi trường | Số liệu sạch, an toàn cho tài khoản quảng cáo | Phải tạo và quản lý gấp đôi cấu hình |
| Một môi trường, lọc theo phiên bản | Ít cấu hình | Dễ lẫn số liệu, rủi ro bấm quảng cáo thật |

## Hệ quả

- **Tích cực:** số liệu, crash, cờ tính năng của người dùng thật không bị lẫn dữ liệu test.
- **Phải chấp nhận:** khi khởi tạo app mới, phải tạo hai bộ cấu hình.
- **Việc AI phải tuân thủ:** không viết cứng giá trị cấu hình môi trường trong code; không bao giờ đọc, sửa hoặc tạo cấu hình production; mọi thao tác trong phiên tự chạy chỉ trên môi trường staging.
