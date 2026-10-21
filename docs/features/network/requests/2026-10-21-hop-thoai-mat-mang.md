---
date: 2026-10-21
author: Nhật (người phụ trách sản phẩm); AI ghi lại
feature: network
status: specified
---

# YÊU CẦU: APP LUÔN CẦN MẠNG, BÁO MẤT MẠNG KHI ĐANG DÙNG

## Mình muốn gì

Các app của chúng ta luôn luôn yêu cầu có mạng thì mới dùng được. Ngay từ đầu, nếu không có mạng thì không cho vào app. Trong quá trình sử dụng, nếu không có mạng thì hiển thị popup thông báo lỗi mạng, có nút "Thử lại".

Nhận ra mất mạng bằng cả hai cách: theo dõi trạng thái mạng của máy, và khi một lần tải thất bại vì mạng.

## Vì sao

Quyết định chung cho mọi app của nhóm (2026-10-21).

## Người dùng sẽ trải nghiệm thế nào

Đang dùng app thì mất Wi-Fi. Hộp thoại "Mất kết nối mạng" hiện lên. Người dùng bật lại mạng, bấm "Thử lại", hộp thoại đóng, app dùng tiếp.

## Những gì chưa cần làm lần này

Chế độ dùng khi không có mạng.

## Quảng cáo, dữ liệu, file tĩnh

Không có.

## Tham khảo

- Bản đề xuất: mục 6 (luồng phụ), mục 7 (trạng thái `offline`), mục 12 (câu 13, 14).
- Chặn lúc mở app đã có ở feature `loading` (ADR-0018).
- Thiết kế: component `NetworkDialog` trên trang Design system; frame `CameraScreen_offline_dark`, `PosesScreen_offline_light`, `PosesScreen_offline_dark`.
