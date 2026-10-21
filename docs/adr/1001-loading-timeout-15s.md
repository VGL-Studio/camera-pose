---
id: ADR-1001
title: Màn loading của Camera Pose chờ tối đa 15 giây cho mỗi thứ cần tải, kể cả Remote Config
status: proposed
origin: app
date: 2026-10-21
deciders: [Nhật]
supersedes: ADR-0018
superseded_by:
---

# ADR-1001: MÀN LOADING CỦA CAMERA POSE CHỜ TỐI ĐA 15 GIÂY CHO MỖI THỨ CẦN TẢI, KỂ CẢ REMOTE CONFIG

## Bối cảnh

ADR-0018 (dự án mẫu) cho màn loading chờ tải Remote Config tối đa 10 giây; file trên R2 dùng mức 15 giây sẵn có của `:core:assets`. Ở Camera Pose, màn loading tải thêm catalog tư thế và bộ tư thế khởi đầu (spec `loading`, bản đổi ngày 2026-10-21). Người phụ trách sản phẩm trả lời câu hỏi 3 của spec `loading`: chờ 15 giây cho mỗi thứ cần tải. Các thị trường phát hành (Ấn Độ, Indonesia, Philippines…) nhiều nơi mạng chậm, nên một mức chờ chung, dài hơn, giảm số lần báo lỗi nhầm.

ADR này chỉ thay một chi tiết của ADR-0018 cho app này: thời gian chờ Remote Config. Mọi phần khác của ADR-0018 giữ nguyên.

## Quyết định

- Ở Camera Pose, `RemoteConfigRefresher.refresh()` chờ tối đa 15 giây thay vì 10 giây.
- Mọi thứ màn loading tải (cấu hình, catalog, từng file của bộ khởi đầu) dùng cùng mức chờ 15 giây.
- Phần còn lại của ADR-0018 giữ nguyên: chỉ màn loading dùng `RemoteConfigRefresher`; tải thẳng từ máy chủ mỗi lần mở app; bị Firebase giới hạn thì coi là thành công; cấu hình Firebase giả thì thành công ngay.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| 15 giây cho mọi thứ (đã chọn) | Một quy tắc dễ hiểu; ít báo lỗi nhầm khi mạng chậm | Khi máy chủ không trả lời, người dùng chờ lâu hơn 5 giây mới thấy lỗi |
| Giữ 10 giây cho cấu hình như ADR-0018 | Không lệch dự án mẫu | Trái câu trả lời của người phụ trách sản phẩm |

## Hệ quả

- **Tích cực:** ít lần màn loading báo lỗi khi mạng chỉ chậm chứ không mất.
- **Phải chấp nhận:** app lệch dự án mẫu ở một con số; khi dự án mẫu đổi `RemoteConfigRefresher`, kiểm tra lại mức chờ của app. Không đề xuất đưa về dự án mẫu (quyết định riêng của app).
- **Việc AI phải tuân thủ:** ở app này, màn loading chờ tối đa 15 giây cho cấu hình, catalog và từng file của bộ khởi đầu; các quy tắc khác của ADR-0018 vẫn áp dụng.
