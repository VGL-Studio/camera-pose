---
id: ADR-0046
title: Khóa Remote Config phẳng, có kiểu, khai báo một chỗ trong code, sinh bảng khóa và file mẫu; staging có giá trị cục bộ
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-11-01
deciders: [Nhật]
supersedes: ADR-0023         # ADR hợp nhất (ADR-0044, mục 4); cách đặt tên khóa quảng cáo theo ADR-0045
superseded_by:
---

# ADR-0046: KHÓA REMOTE CONFIG PHẲNG, CÓ KIỂU, KHAI BÁO MỘT CHỖ TRONG CODE, SINH BẢNG KHÓA VÀ FILE MẪU; STAGING CÓ GIÁ TRỊ CỤC BỘ

## Bối cảnh

ADR-0023 đặt quy ước cho khóa Remote Config: khóa phẳng có kiểu, khai báo một chỗ, bảng khóa sinh từ code, giá trị cục bộ cho staging. Lý do lúc đó: giá trị mặc định nằm rải trong code từng module, không có bảng chung; bản staging dùng file Firebase giả nên không thử được quảng cáo; các nguồn cũ có nhiều bảng khóa lệch tên, và app mẫu khảo sát dồn cấu hình vào một khối JSON khó kiểm.

ADR-0026 thay mục 1 của ADR-0023 cho khóa quảng cáo: tên theo AdsKit. ADR-0044, mục 4, yêu cầu hợp nhất để một quy tắc không phải đọc từ hai ADR. ADR này chép lại mọi quy tắc còn đúng của ADR-0023; phần đặt tên khóa quảng cáo nay ở ADR-0045, mục 6. Không quyết định nào được thêm, bỏ hay đổi nghĩa.

## Quyết định

1. **Khóa phẳng, chữ thường, nối bằng gạch dưới, có tiền tố theo chủ:** `feature_<ten>_enabled` cho cờ tính năng (ADR-0003), `<module>_<tham số>` cho tham số của module. Đơn vị nằm ở cuối tên (`_seconds`, `_count`, `_percent`). Khóa quảng cáo đặt tên theo ADR-0045, mục 6 (tên AdsKit, đơn vị ghi trong mô tả).
2. **Bốn kiểu:** boolean, long, double, string. Không dùng khối JSON làm cấu hình; danh sách ngắn thì dùng string có định dạng ghi rõ trong mô tả khóa.
3. **Mỗi khóa khai báo đúng một chỗ trong code** của module sở hữu nó, kèm kiểu, giá trị mặc định và mô tả. Giá trị mặc định là giá trị an toàn khi chưa tải được cấu hình (vị trí quảng cáo và cờ tính năng: tắt).
4. **Bảng khóa và file mẫu sinh từ code:** một test (chạy trong cổng chất lượng) sinh và kiểm `docs/remote-config-keys.md` (khóa, kiểu, mặc định, module, mô tả) và một file mẫu để nhập vào Firebase; test báo lỗi khi bảng không khớp code, như `docs/foundation.md`. Không ai sửa tay hai file này.
5. **Staging có giá trị cục bộ:** bản staging đọc một bộ giá trị khai báo trong `app/src/staging` (ví dụ bật mọi vị trí quảng cáo, dùng mã thử của Google), xếp sau giá trị từ Firebase và trước giá trị mặc định, như cách `StagingFeatureFlagsModule` bật cờ. Production không có bộ này.
6. **Đổi giá trị trên Firebase là một thay đổi có ghi lại:** dùng điều kiện theo quốc gia, phiên bản app hay phần trăm người dùng để bật dần; ghi mỗi lần đổi (khóa, giá trị cũ, giá trị mới, điều kiện, lý do, người đổi) vào sổ đổi cấu hình của app; quay về bằng lịch sử phiên bản của Firebase.
7. **Khóa đã phát hành không đổi tên, không đổi kiểu.** Đổi tên là khóa mới; khóa cũ giữ tới khi không còn bản app nào đọc.

| Mục mới | Nguồn |
| --- | --- |
| 1 | ADR-0023 mục 1 (phần không phải quảng cáo); phần quảng cáo theo ADR-0026 quyết định 9, nay ADR-0045 mục 6 |
| 2 tới 6 | ADR-0023 mục 2 tới 6 |
| 7 | ADR-0023 Hệ quả |

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Khóa phẳng có kiểu, khai báo một chỗ, sinh bảng (đã chọn) | Một nguồn sự thật; đọc được trên Firebase; test bắt lệch | Nhiều khóa hơn một khối JSON |
| Một khối JSON cho mỗi nhóm cấu hình | Ít khóa, đổi nhiều giá trị một lần | Không có kiểu, sai một dấu là hỏng cả khối, khó xem lịch sử từng giá trị |
| Bảng khóa viết tay trong tài liệu | Không cần script | Lệch với code |
| Staging dùng project Firebase thật | Giống production | Cần bí mật, thêm hạ tầng; ADR-0012 đã chọn file giả |

## Hệ quả

- **Tích cực:** thử được quảng cáo trên staging; người vận hành có một bảng khóa đúng với bản app đang chạy; đổi cấu hình có dấu vết.
- **Phải chấp nhận:** đổi `:core:config` và test sinh bảng khóa là plan rủi ro cao; khóa đã phát hành không đổi tên được.
- **Việc AI phải tuân thủ:** đặt tên khóa theo quy ước của ADR-0046, khóa quảng cáo theo ADR-0045, mục 6; không dùng khối JSON làm cấu hình; khai báo khóa mới kèm kiểu, mặc định an toàn và mô tả ở module sở hữu, rồi sinh lại bảng khóa trong cùng PR; không đổi tên hay đổi kiểu một khóa đã phát hành; không đổi giá trị trên Firebase (việc của người).
