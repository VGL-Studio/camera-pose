---
id: ADR-0020
title: Thiết kế trên Claude Design hoặc Figma, bàn giao bằng ảnh khung chuẩn trong repo, đối chiếu bằng ảnh so sánh
status: proposed
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-15
deciders: [Nhóm phát triển, Người phụ trách sản phẩm]
---

# ADR-0020: THIẾT KẾ TRÊN CLAUDE DESIGN HOẶC FIGMA, BÀN GIAO BẰNG ẢNH KHUNG CHUẨN TRONG REPO, ĐỐI CHIẾU BẰNG ẢNH SO SÁNH

## Bối cảnh

ADR-0008 quyết định code là gốc của hệ thống thiết kế và bản mẫu chỉ là ý đồ thiết kế, nhưng chưa nói cách làm cụ thể. Nhóm dùng Claude Design hoặc Figma tùy dự án. Theo tài liệu chính thức (tra ngày 2026-10-15):

- Cả hai công cụ chỉ đăng nhập bằng trình duyệt (OAuth). Phiên tự chạy trên cloud không có connector nên không đọc trực tiếp được thiết kế.
- Claude Design nhập hệ thống thiết kế từ code, nhưng chỉ ghi rõ cho React; không ghi cách xuất ảnh PNG. Figma MCP mặc định sinh code React; Code Connect (gói Professional trở lên) có hỗ trợ Compose. Figma Variables qua API cần gói Enterprise.
- Không công cụ nào sinh Compose trực tiếp.

## Quyết định

1. **Khung chuẩn:** mọi màn hình thiết kế dựng ở khung 411 x 891 dp, xuất PNG ở mức 3x (1233 x 2673 điểm ảnh). Đây đúng là khung của screenshot test (`@Config(qualifiers = "w411dp-h891dp-xxhdpi")`), nên ảnh thiết kế và ảnh màn hình thật so được với nhau. App hỗ trợ điện thoại màn hình gập dựng thêm khung mở ra 673 x 841 dp (khung `Devices.FOLDABLE` của Compose, ảnh 2019 x 2523, screenshot test `w673dp-h841dp-xxhdpi`) cho các màn hình đổi bố cục khi rộng, tên có hậu tố `_unfolded`. Trạng thái trong tên viết thường, có thể nối bằng gạch nối (`permission-denied`).
2. **Hệ thống thiết kế đi từ code sang công cụ:** `:core:designsystem` có catalog (màu, chữ, khoảng cách, thành phần) được chụp bằng screenshot test, và `docs/design-system/tokens.json` liệt kê các giá trị; test giữ file này khớp với code. Người thiết kế dựng hệ thống thiết kế trên Claude Design hoặc Figma từ hai nguồn này.
3. **Bàn giao bằng một gói trong repo, giống nhau cho hai công cụ:** `docs/features/<feature>/design/` gồm `README.md` theo mẫu, ảnh từng màn hình và trạng thái trong `screens/`, đặt tên đúng bằng tên ảnh chuẩn screenshot sẽ có (`<Ten>Screen_<trạng thái>_<light|dark>.png`), và số liệu của từng màn hình trong `context/` cùng tên (bố cục, khoảng cách, màu, chữ, đối chiếu với `tokens.json`; với Figma do `scripts/figma-export.py` sinh). Phiên tự chạy chỉ dùng gói này, không nối với công cụ thiết kế: repo giữ đúng bản đã duyệt, phiên không phụ thuộc công cụ lúc chạy, và không có quyền sửa thiết kế. Link tới công cụ chỉ để người tham khảo.
4. **Đưa vào Compose:** AI viết lại bằng Compose theo token của hệ thống thiết kế (ADR-0008); không chép code web, không viết cứng giá trị.
5. **Đối chiếu, ba lớp, không có cổng chặn theo điểm ảnh:**
   - Tự động, chặn được: screenshot test cho mọi ảnh trong `screens/`, test giao diện kiểm tra chữ theo bảng chữ của thiết kế, quy tắc không viết cứng giá trị.
   - Ảnh so sánh để tham khảo: `scripts/design-compare.sh <feature>` ghép ảnh thiết kế, ảnh màn hình thật, ảnh khác biệt và tỷ lệ điểm ảnh khác, ghi vào `design/review/`. Không chặn merge, vì phông chữ và cách vẽ luôn lệch vài điểm ảnh.
   - Rà soát: phiên rà soát độc lập xem từng cặp ảnh theo danh sách trong mẫu thiết kế; người phụ trách sản phẩm duyệt cuối trên ảnh so sánh và bản staging. Sau khi duyệt, ảnh chuẩn screenshot giữ giao diện đó.
6. **Lấy gói từ Figma:** `scripts/figma-export.py` gọi REST API của Figma bằng token chỉ đọc của người, sinh ảnh và `context/` cho mọi frame đặt tên đúng quy tắc; chạy được với mọi gói Figma, kể cả Starter (MCP của gói Starter chỉ có vài lượt mỗi tháng). Figma MCP dùng thêm trong phiên có người theo dõi khi cần. Token và connector không vào routine của phiên tự chạy.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Gói ảnh khung chuẩn trong repo | Phiên tự chạy đọc được; một quy trình cho hai công cụ; so được với screenshot test | Người phải xuất ảnh mỗi lần thiết kế đổi |
| Phiên tự chạy đọc trực tiếp Claude Design hoặc Figma qua connector | Luôn mới nhất, không phải lập gói | Có thể làm theo bản chưa duyệt; thêm điểm hỏng giữa phiên (lỗi, hết lượt, hết hạn đăng nhập); connector có quyền sửa mọi file thiết kế của nhóm |
| Cổng chặn theo tỷ lệ điểm ảnh khác | Tự động hoàn toàn | Báo sai vì phông chữ và cách vẽ; phiên qua đêm dừng vì lỗi không có thật |

## Hệ quả

- **Tích cực:** thiết kế từ hai công cụ đi cùng một đường; mỗi màn hình có bằng chứng so sánh với thiết kế; giao diện đã duyệt được screenshot test giữ lại.
- **Phải chấp nhận:** xuất ảnh là việc tay; với Claude Design phải chụp bằng trình duyệt vì chưa có xuất PNG; đổi hệ thống thiết kế trong code thì người thiết kế cập nhật lại trên công cụ.
- **Việc AI phải tuân thủ:** chỉ dùng gói trong `design/`; đặt tên screenshot test đúng tên ảnh trong `screens/`; ảnh `_unfolded` có screenshot test ở khung `w673dp-h841dp-xxhdpi`; chạy `scripts/design-compare.sh` và đưa `design/review/` vào PR khi feature có thiết kế; không đổi giao diện để giảm tỷ lệ khác biệt bằng cách viết cứng giá trị.
