---
id: ADR-0024
title: App chỉ dùng được khi có Internet; mất mạng thì app chặn bằng một popup chung có nút thử lại
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-21
deciders: [Nhóm phát triển, Người phụ trách sản phẩm]
supersedes:                  # thay ý "dùng được khi mất mạng" trong phần hệ quả của ADR-0015
superseded_by:
---

# ADR-0024: APP CHỈ DÙNG ĐƯỢC KHI CÓ INTERNET; MẤT MẠNG THÌ APP CHẶN BẰNG MỘT POPUP CHUNG CÓ NÚT THỬ LẠI

## Bối cảnh

Các app của nhóm kiếm tiền bằng quảng cáo (ADR-0004, ADR-0022). Không có mạng thì không tải được quảng cáo, không đo được, không tối ưu được doanh thu; Remote Config và nội dung trên R2 cũng không cập nhật. Người phụ trách sản phẩm quyết định ngày 2026-10-21: app luôn cần mạng; không có mạng thì không cho vào app; đang dùng mà mất mạng thì hiện popup báo lỗi mạng có nút thử lại.

Hiện trạng: màn hình loading đã chờ tải Remote Config mỗi lần mở app và báo lỗi có nút "Thử lại" khi không có mạng (ADR-0018, spec `loading`), nhưng khi cờ `feature_loading_enabled` tắt thì app vào được mà không cần mạng, và trong lúc dùng không có gì báo mất mạng. ADR-0015 coi việc file đã tải dùng được khi mất mạng là một ưu điểm; khảo sát app mẫu cũng cho thấy "Wi-Fi bật" không có nghĩa là có Internet (máy thử bật Wi-Fi nhưng không có mạng suốt một phiên).

## Quyết định

1. **"Có mạng" là có Internet dùng được:** hệ thống báo mạng mặc định có khả năng Internet và đã được xác nhận (`NET_CAPABILITY_INTERNET` và `NET_CAPABILITY_VALIDATED`). Wi-Fi bật mà chưa xác nhận được Internet là không có mạng.
2. **Một bộ theo dõi kết nối dùng chung** ở module core, đưa trạng thái có mạng hay không dưới dạng `Flow`. Feature không tự đọc trạng thái mạng của hệ thống.
3. **Lúc mở app:** không có mạng thì không vào app. Màn hình loading (khi bật) giữ cách báo lỗi và thử lại của nó; khi không có màn hình loading thì popup chung (mục 4) chặn ngay từ màn hình đầu tiên.
4. **Trong lúc dùng:** mất mạng thì `:app` hiện một popup chung trên mọi màn hình: báo lỗi mạng, có nút "Thử lại", không đóng được bằng chạm ra ngoài hay nút quay lại, chặn thao tác với màn hình bên dưới. Có mạng trở lại thì popup tự đóng; "Thử lại" kiểm tra lại kết nối ngay. Popup không hiện chồng lên màn hình loading (màn hình đó tự báo lỗi) và không chen vào lúc quảng cáo toàn màn hình đang hiện; nó hiện ngay khi quảng cáo đóng.
5. **Feature không làm chế độ ngoại tuyến** (danh sách "đang dùng bộ có sẵn", nút "dùng khi không có mạng"...). Tải file thất bại vì lỗi mạng (không kết nối được, quá thời gian) thì popup chung hiện ngay, kể cả khi hệ thống vẫn báo có mạng (`:core:assets` báo cho bộ điều khiển popup; quyết định 2026-10-23). Máy chủ trả lỗi thì vẫn đưa vào `UiState` và có nút thử lại như trước.
6. **File đã tải từ R2 vẫn giữ trên máy** (ADR-0015) để mở nhanh và đỡ tải lại; ý "dùng được khi mất mạng" trong phần hệ quả của ADR-0015 không còn là mục tiêu.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| App luôn cần mạng, popup chung ở cấp app (đã chọn) | Đúng mô hình doanh thu; một chỗ xử lý cho mọi feature; feature đơn giản hơn | Người dùng mất mạng không dùng được gì; mạng chập chờn làm popup hiện, ẩn |
| Cho dùng khi mất mạng với nội dung có sẵn | Trải nghiệm tốt hơn khi mạng kém | Không có quảng cáo trong lúc đó; mỗi feature phải làm thêm trạng thái ngoại tuyến |
| Mỗi feature tự báo mất mạng | Linh hoạt | Lặp code, cách báo không đồng nhất, dễ sót |
| Chỉ xét Wi-Fi hay dữ liệu di động đang bật | Đơn giản | Báo có mạng khi thực ra không vào được Internet |

## Hệ quả

- **Tích cực:** mọi phiên dùng app đều có điều kiện để hiện và đo quảng cáo; feature không phải làm trạng thái ngoại tuyến.
- **Phải chấp nhận:** người dùng ở nơi mạng yếu bị chặn; mạng chập chờn có thể làm popup hiện rồi tự đóng liên tục (bộ theo dõi nên chờ một khoảng ngắn trước khi báo mất mạng, plan chốt con số); kịch bản mất mạng của `device-session.sh` sẽ thấy popup thay vì nội dung.
- **Việc AI phải tuân thủ:** không làm chế độ ngoại tuyến trong feature; feature không tự đọc trạng thái mạng của hệ thống, mất mạng do `:app` xử lý bằng popup chung; lỗi máy chủ trả về vẫn báo trong `UiState` kèm thử lại; spec mới không ghi tiêu chí "dùng được khi không có mạng".
