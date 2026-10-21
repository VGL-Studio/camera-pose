---
id: ADR-0004
title: Dùng AdMob làm nền tảng điều phối mediation, quảng cáo qua một lớp trung gian
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-14
deciders: [Nhóm phát triển, Người phụ trách sản phẩm]
---

# ADR-0004: DÙNG ADMOB LÀM NỀN TẢNG ĐIỀU PHỐI MEDIATION, QUẢNG CÁO QUA MỘT LỚP TRUNG GIAN

## Bối cảnh

App kiếm tiền chủ yếu bằng quảng cáo, dùng nhiều mạng như AdMob, AppLovin, Unity. Quảng cáo là nguồn doanh thu duy nhất, nên lỗi ở đây ảnh hưởng trực tiếp tới doanh thu và có thể khiến tài khoản quảng cáo bị hạn chế.

## Quyết định

- AdMob là nền tảng điều phối mediation. AppLovin, Unity và các mạng khác chạy như mạng con qua adapter.
- Module `:core:ads` là lớp trung gian. Feature chỉ yêu cầu "hiển thị quảng cáo ở vị trí X", không gọi trực tiếp SDK nào.
- Vị trí quảng cáo khai báo tập trung trong `:core:ads`, mỗi vị trí có tên, định dạng, cờ bật tắt và tham số tần suất qua Remote Config.
- Rào chắn chính sách nằm trong module: giới hạn tần suất quảng cáo toàn màn hình, không hiển thị khi người dùng đang thao tác, không đặt sát nút bấm.
- Bản staging chỉ dùng mã quảng cáo thử nghiệm. Mã thật chỉ được đưa vào khi build bản phát hành, do người thực hiện.
- Quảng cáo cá nhân hóa ở châu Âu được bật tắt qua Remote Config, sau khi có sự đồng ý của người dùng qua hộp thoại được Google chứng nhận.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| AdMob điều phối | Tích hợp sẵn với Firebase, quản lý tập trung | Phụ thuộc hệ sinh thái Google |
| AppLovin MAX điều phối | Đấu giá mạnh | Thêm một hệ thống quản lý riêng |

## Hệ quả

- **Tích cực:** đổi mạng, thêm mạng, nâng cấp SDK chỉ sửa một chỗ; người phụ trách sản phẩm tự điều chỉnh vị trí và tần suất.
- **Phải chấp nhận:** mọi thay đổi trong `:core:ads` là rủi ro cao, cần duyệt chéo.
- **Việc AI phải tuân thủ:** không tự thêm vị trí quảng cáo ngoài spec; không đưa mã quảng cáo thật vào code hay tài liệu; không khởi tạo SDK quảng cáo trước khi xử lý xong sự đồng ý của người dùng.
