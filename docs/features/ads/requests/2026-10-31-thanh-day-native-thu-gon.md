---
date: 2026-10-31
author: Nhật (AI ghi từ lời trong phiên)
feature: ads
status: specified
---

# YÊU CẦU: NATIVE THU GỌN Ở THANH ĐÁY, CHỌN BẰNG REMOTE CONFIG

## Mình muốn gì

Lời của Nhật: "có thể sử dụng native collapsible ở vị trí thanh đáy ở dưới, thay cho banner hoặc native small banner (có thể lựa chọn hiển thị banner hay collapsible thông qua remote config)".

## Vì sao

Có thêm cách hiện quảng cáo ở thanh đáy của thư viện và chi tiết tư thế. Nhóm chọn cách hiện từ xa theo số liệu, không cần phát bản mới.

## Người dùng sẽ trải nghiệm thế nào

Tùy giá trị trên Remote Config, đáy màn thư viện và chi tiết hiện một trong ba thứ:

- banner;
- native nhỏ;
- native thu gọn (thẻ 250 dp nổi lên, người dùng thu gọn được).

## Những gì chưa cần làm lần này

Chưa nêu.

## Quảng cáo, dữ liệu, file tĩnh

- Dùng lại vị trí thanh đáy `poses_bottom`, `pose_detail_bottom`.
- Bản production cần mã quảng cáo cho lớp banner và lớp native thu gọn. Nhật đã tạo hai mã này trên AdMob ngày 2026-10-31: "Banner" và "Native 1 - Banner Collapsible". Mã không ghi vào repo.

## Tham khảo

- Spec ads, câu 8 và 10.
- `docs/ops/ads-guide.md`, mục 5, bước 6: production chưa có mã cho từng lớp của thanh đáy.
