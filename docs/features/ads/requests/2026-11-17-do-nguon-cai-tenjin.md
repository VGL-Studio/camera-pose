---
date: 2026-11-17
author: Nhật
feature: ads
status: specified            # new | specified | done | dropped. AI ghi specified khi đã có spec; người ghi done hay dropped
---

# YÊU CẦU: ĐO NGUỒN CÀI VÀ DOANH THU QUẢNG CÁO BẰNG TENJIN

Nhật nói trong phiên ngày 2026-11-17 (AI ghi lại từ lời nói và các lựa chọn của Nhật khi được hỏi).

## Mình muốn gì

Tích hợp Tenjin vào app. Tenjin dùng cho ba việc:

- đo nguồn cài đặt của các chiến dịch thu hút người dùng (UA);
- nhận doanh thu của từng lượt quảng cáo hiện, để nhóm UA tính doanh thu trên chi phí (ROAS) theo chiến dịch;
- nhận các sự kiện phễu quảng cáo, tương đương danh sách sự kiện mà khung gửi sang AppsFlyer (`docs/ops/ads-business-rules.md`, mục 8.3).

## Vì sao

App chưa có công cụ đo nguồn cài nào: AppsFlyer đang tắt (spec ads, câu 3; ADR-1007). Không đo được thì nhóm UA không biết chiến dịch nào mang lại người dùng có doanh thu.

## Người dùng sẽ trải nghiệm thế nào

Không có gì thay đổi trên màn hình. Người dùng ở vùng cần hỏi đồng ý vẫn trả lời hộp thoại đồng ý quảng cáo như hôm nay.

## Những gì chưa cần làm lần này

- Không bật AppsFlyer. Tenjin thay AppsFlyer, không chạy song song.
- Không đề xuất về dự án mẫu: đây là quyết định riêng của app.

## Quảng cáo, dữ liệu, file tĩnh

- Không thêm vị trí quảng cáo.
- Dữ liệu mới gửi ra ngoài: Tenjin (nguồn cài, doanh thu quảng cáo, sự kiện phễu quảng cáo). Chi tiết và câu hỏi ở spec ads, câu 21.

## Tham khảo

- SDK Android của Tenjin: https://github.com/tenjin/tenjin-android-sdk
