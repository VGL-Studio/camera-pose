---
id: ADR-0023
title: Khóa Remote Config phẳng, có kiểu, khai báo một chỗ trong code, sinh bảng khóa và file mẫu; staging có giá trị cục bộ
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-21
deciders: [Nhóm phát triển, Người phụ trách sản phẩm]
supersedes:
superseded_by:
---

# ADR-0023: KHÓA REMOTE CONFIG PHẲNG, CÓ KIỂU, KHAI BÁO MỘT CHỖ TRONG CODE, SINH BẢNG KHÓA VÀ FILE MẪU; STAGING CÓ GIÁ TRỊ CỤC BỘ

> **Ghi chú 2026-10-23:** ADR-0026 (duyệt 2026-10-24) thay mục 1 (cách đặt tên khóa) cho khóa quảng cáo: tên giống hệt AdsKit, khóa mới cùng kiểu (`is_show_<vị trí>`, `ads_interval`...). Mục 2 tới 6 giữ nguyên.

## Bối cảnh

ADR-0003 dùng Remote Config cho cờ tính năng và tham số kinh doanh; ADR-0012 nối Firebase; ADR-0018 chờ cấu hình ở màn hình loading. Hiện trạng (2026-10-21):

- `RemoteSettings` chỉ đọc kiểu boolean và long. Giá trị mặc định nằm rải trong code từng module; không có bảng chung liệt kê mọi khóa.
- Bản staging dùng file Firebase giả nên Remote Config không trả gì: quảng cáo không bao giờ hiện trên staging, không thử được vị trí quảng cáo trước khi phát hành.
- Repo `vgl-docs` có ít nhất 5 bảng khóa lệch tên nhau. Các app mẫu khảo sát ở ADR-0021 dồn cấu hình quảng cáo vào một khối JSON (`ad_config`), khó kiểm và khó đọc trên Firebase.

## Quyết định

1. **Khóa phẳng, chữ thường, nối bằng gạch dưới, có tiền tố theo chủ:** `feature_<ten>_enabled` (ADR-0003), `ads_enabled` và `ads_<vị trí>_enabled`, `ads_<vị trí hay định dạng>_<tham số>`, `<module>_<tham số>` cho tham số của module khác. Đơn vị nằm ở cuối tên (`_seconds`, `_count`, `_percent`).
2. **Bốn kiểu:** boolean, long, double, string. Không dùng khối JSON làm cấu hình; danh sách ngắn thì dùng string có định dạng ghi rõ trong mô tả khóa.
3. **Mỗi khóa khai báo đúng một chỗ trong code** của module sở hữu nó, kèm kiểu, giá trị mặc định và mô tả. Giá trị mặc định là giá trị an toàn khi chưa tải được cấu hình (vị trí quảng cáo và cờ tính năng: tắt).
4. **Bảng khóa và file mẫu sinh từ code:** một test (chạy trong cổng chất lượng) sinh và kiểm `docs/remote-config-keys.md` (khóa, kiểu, mặc định, module, mô tả) và một file mẫu để nhập vào Firebase; test báo lỗi khi bảng không khớp code, như `docs/foundation.md`. Không ai sửa tay hai file này.
5. **Staging có giá trị cục bộ:** bản staging đọc một bộ giá trị khai báo trong `app/src/staging` (ví dụ bật mọi vị trí quảng cáo, dùng mã thử của Google), xếp sau giá trị từ Firebase và trước giá trị mặc định, như cách `StagingFeatureFlagsModule` bật cờ. Production không có bộ này.
6. **Đổi giá trị trên Firebase là một thay đổi có ghi lại:** dùng điều kiện theo quốc gia, phiên bản app hay phần trăm người dùng để bật dần; ghi mỗi lần đổi (khóa, giá trị cũ, giá trị mới, điều kiện, lý do, người đổi) vào sổ đổi cấu hình của app; quay về bằng lịch sử phiên bản của Firebase.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Khóa phẳng có kiểu, khai báo một chỗ, sinh bảng (đã chọn) | Một nguồn sự thật; đọc được trên Firebase; test bắt lệch | Nhiều khóa hơn một khối JSON |
| Một khối JSON cho mỗi nhóm cấu hình | Ít khóa, đổi nhiều giá trị một lần | Không có kiểu, sai một dấu là hỏng cả khối, khó xem lịch sử từng giá trị |
| Bảng khóa viết tay trong tài liệu | Không cần script | Lệch với code, như `vgl-docs` |
| Staging dùng project Firebase thật | Giống production | Cần bí mật, thêm hạ tầng; ADR-0012 đã chọn file giả |

## Hệ quả

- **Tích cực:** thử được quảng cáo trên staging; người vận hành có một bảng khóa đúng với bản app đang chạy; đổi cấu hình có dấu vết.
- **Phải chấp nhận:** đổi `:core:config` và thêm test sinh bảng khóa là plan rủi ro cao; khóa đã phát hành không đổi tên (đổi tên là khóa mới, khóa cũ giữ tới khi không còn bản app nào đọc).
- **Việc AI phải tuân thủ:** đặt tên khóa theo quy ước của ADR này (khóa quảng cáo theo ADR-0026 nếu ADR đó được duyệt); không dùng khối JSON làm cấu hình; khai báo khóa mới kèm kiểu, mặc định an toàn và mô tả ở module sở hữu, rồi sinh lại bảng khóa trong cùng PR; không đổi tên hay đổi kiểu một khóa đã phát hành; không đổi giá trị trên Firebase (việc của người).
