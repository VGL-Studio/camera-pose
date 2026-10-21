---
id: ADR-0006
title: Không có backend, file tĩnh trên R2 chỉ thêm mới
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-14
deciders: [Nhóm phát triển]
---

# ADR-0006: KHÔNG CÓ BACKEND, FILE TĨNH TRÊN R2 CHỈ THÊM MỚI

## Bối cảnh

Các app chạy độc lập trên máy. Một số file tĩnh như ảnh được để trên Cloudflare R2 để giảm dung lượng cài đặt. Người dùng không cập nhật app đồng loạt, nên các phiên bản cũ vẫn tải file theo đường dẫn cũ trong thời gian dài.

## Quyết định

- Không xây dựng backend. Dữ liệu người dùng lưu trên máy.
- File trên R2 được truy cập qua module `:core:assets`, có lưu đệm và xử lý khi không tải được.
- File đã phát hành thì không sửa, không xóa. Đường dẫn gắn phiên bản hoặc mã băm nội dung, mọi thay đổi là thêm file mới.
- CI chỉ có quyền ghi vào phần staging trên R2. Đẩy file lên production thuộc bước phát hành thủ công.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Chỉ thêm mới, đường dẫn có phiên bản | Phiên bản cũ không bao giờ hỏng | Dung lượng lưu trữ tăng dần |
| Ghi đè file cùng tên | Gọn | Phiên bản cũ hiển thị sai, lỗi khó tái hiện |

## Hệ quả

- **Tích cực:** không có máy chủ phải vận hành; phiên bản app cũ luôn hoạt động đúng.
- **Phải chấp nhận:** cần dọn dẹp định kỳ các file không còn phiên bản nào dùng, việc này làm thủ công.
- **Việc AI phải tuân thủ:** không viết code hoặc script ghi đè, xóa file trên R2; mọi đường dẫn R2 lấy từ `:core:assets`, không viết cứng trong feature.
