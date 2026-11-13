---
date: 2026-11-13
author: Nhật (AI ghi từ lời trong phiên)
feature: ads
status: specified
---

# YÊU CẦU: QUẢNG CÁO Ở MÀN CÀI ĐẶT VÀ MÀN XEM LẠI ẢNH

## Mình muốn gì

Lời của Nhật: "Thêm ticket để chèn thêm quảng cáo vào 2 trang sau: Trang settings, trang preview ảnh."

Khi được hỏi trong phiên, Nhật chọn:

- Màn Cài đặt: thanh đáy native nhỏ, như thanh đáy của thư viện tư thế.
- Màn xem lại ảnh (sau khi chụp): thanh đáy native nhỏ, dưới hàng nút.
- Lúc phát hành cả hai tắt; nhóm bật sau trên Remote Config.

## Vì sao

Thêm chỗ có doanh thu quảng cáo. Màn xem lại là màn người dùng mở sau mỗi lần chụp.

## Người dùng sẽ trải nghiệm thế nào

- Mở Cài đặt: đáy màn có một thanh quảng cáo native nhỏ; các dòng cài đặt cuộn phía trên, không bị che.
- Chụp xong, màn xem lại mở: đáy màn có thanh quảng cáo native nhỏ, dưới các nút Lưu, Chụp lại, Chia sẻ, Tư thế tiếp; ảnh co lại cho vừa phần còn lại.

## Những gì chưa cần làm lần này

- Không đổi `review_interstitial` và combo native sau nó.
- Không thêm quảng cáo ở màn camera.

## Quảng cáo, dữ liệu, file tĩnh

Hai vị trí mới, mặc định tắt. Không thêm sự kiện phân tích, không có file tĩnh.

## Tham khảo

Thanh đáy `poses_bottom`, `pose_detail_bottom` (spec poses, mục 5).
