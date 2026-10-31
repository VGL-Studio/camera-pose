---
date: 2026-10-31
author: Nhật (lời nói trong phiên làm nhanh, AI ghi lại)
feature: loading
status: specified
---

# YÊU CẦU: BIỂU TƯỢNG APP

## Mình muốn gì

App có biểu tượng riêng trên màn hình chính của máy, thay biểu tượng Android mặc định. Biểu tượng lấy từ ảnh Nhật đã chuẩn bị: máy ảnh vàng cam phía trước, một ảnh tư thế cô gái phía sau, nền cam chuyển màu.

Nhật đưa ba ảnh trong phiên:

- `app_icon.png` (512 x 512): biểu tượng cho trang Google Play, giữ nguyên.
- Lớp hình (máy ảnh, ảnh tư thế, hai ngôi sao) trên nền trong suốt, do AI tạo theo `app_icon.png`.
- Lớp nền cam chuyển màu, từ vàng cam ở góc trên trái tới cam đậm ở góc dưới phải.

AI đo lớp hình: hình chiếm khoảng 95 dp của khung 108 dp, vượt vùng an toàn 66 dp, thân hình còn hơi trong suốt và có chấm mờ quanh hình. AI đã thu nhỏ hình vào vùng an toàn, làm thân hình đục hẳn, xóa chấm mờ. Nhật xem ảnh xem trước (hình tròn, vuông bo góc, cỡ thật) rồi yêu cầu triển khai theo đường làm nhanh.

## Vì sao

App sắp lên Google Play. Biểu tượng mặc định của Android không cho người dùng nhận ra app, và bản đề xuất yêu cầu app có icon riêng, không giống app mẫu (`docs/research/proposal.md`, mục tên app).

## Người dùng sẽ trải nghiệm thế nào

Cài app xong, người dùng thấy biểu tượng máy ảnh cam trên màn hình chính và trong danh sách app. Biểu tượng hiện trọn hình dù máy cắt biểu tượng theo hình tròn, vuông bo góc hay hình khác. Khi mở app trên Android 12 trở lên, màn hình chào của hệ thống cũng hiện biểu tượng này.

## Những gì chưa cần làm lần này

- Biểu tượng một màu cho chế độ biểu tượng theo chủ đề màu (Android 13 trở lên): chưa có ảnh.
- Logo trên màn hình loading (`BrandLogo`, logo tạm): đổi sau, vì nằm ở `:core:designsystem`.
- Tải biểu tượng lên Google Play: việc của người khi phát hành.

## Quảng cáo, dữ liệu, file tĩnh

Không có.

## Tham khảo

Ảnh gốc lưu ở `docs/brand/app-icon/`: `play-store-512.png` (biểu tượng Google Play), `foreground-1024.png` (lớp hình đã sửa, bản gốc 1024 x 1024).
