---
id: ADR-0002
title: Merge thẳng vào nhánh chính, chỉ sau khi qua cổng chất lượng
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-14
deciders: [Nhóm phát triển]
amended_by: ADR-0010
---

# ADR-0002: MERGE THẲNG VÀO NHÁNH CHÍNH, CHỈ SAU KHI QUA CỔNG CHẤT LƯỢNG

## Bối cảnh

Phiên AI tự chạy tự động merge code của AI để cuối phiên có bản staging. Nếu merge trước rồi mới kiểm tra, một task lỗi sẽ làm hỏng nhánh chính và kéo theo các task khác.

## Quyết định

- Mỗi task AI làm trên một nhánh riêng.
- CI chạy trên nhánh của task: build, lint, unit test, screenshot test, test giao diện trên emulator.
- Chỉ khi qua toàn bộ cổng chất lượng, nhánh mới được tự động merge vào nhánh chính.
- Task không qua thì không merge, được đánh dấu trong báo cáo phiên để dev xem.
- Nhánh chính được bảo vệ, không nhận merge chưa qua kiểm tra. (Trên gói GitHub Free, việc bảo vệ được thực hiện theo ADR-0010.)

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Kiểm tra trên nhánh task rồi merge | Nhánh chính luôn ổn định | CI chạy nhiều lần hơn |
| Merge rồi mới kiểm tra | Nhanh | Một lỗi làm hỏng cả phiên |
| Nhánh tích hợp riêng | Tách rủi ro | Thêm một bước, thêm việc đồng bộ |

## Hệ quả

- **Tích cực:** nhánh chính luôn build được và luôn có thể phát hành, nhờ kết hợp với cờ tính năng (ADR-0003).
- **Phải chấp nhận:** thời lượng CI, nhất là test trên emulator, giới hạn số task chạy được trong mỗi phiên.
- **Việc AI phải tuân thủ:** không bao giờ tìm cách qua cổng chất lượng bằng cách sửa, xóa test hay hạ tiêu chí kiểm tra.
