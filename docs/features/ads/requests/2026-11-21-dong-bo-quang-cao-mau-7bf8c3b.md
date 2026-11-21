---
date: 2026-11-21
author: Nhật
feature: ads
status: specified            # new | specified | done | dropped. AI ghi specified khi đã có spec; người ghi done hay dropped
---

# YÊU CẦU: ĐỒNG BỘ KHUNG QUẢNG CÁO CỦA DỰ ÁN MẪU LÊN MỐC 7BF8C3B

Nhật nói trong phiên ngày 2026-11-21 (AI ghi lại từ lời nói và các lựa chọn của Nhật khi được hỏi).

## Mình muốn gì

"Đồng bộ ads từ dự án mẫu về." Đợt đồng bộ dự án mẫu `bc3e80e..7bf8c3b` (PR #372) giữ `core/ads`, `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md` ở mốc cũ. Lần này đưa cả ba lên mốc `7bf8c3b`, cùng với kích thước native và các luật thanh đáy mới của khung.

## Vì sao

Giữ app theo khung quảng cáo chung của dự án mẫu (ADR-0045), để các lần đồng bộ sau không phải gộp tay phần quảng cáo. App đang chạy kích thước và luật cũ của khung.

## Người dùng sẽ trải nghiệm thế nào

Thanh quảng cáo native ở đáy thư viện, chi tiết tư thế, cài đặt và màn xem lại thấp hơn (106 dp thay 126 dp ở cỡ chữ mặc định), bên trong vẽ theo thẻ mới của khung. Thư viện không có lớp quảng cáo nổi thu gọn vì đã có quảng cáo trong lưới.

Các lựa chọn của Nhật khi được hỏi:

- Kích thước native mới: chọn "Nhận như mẫu".
- Thư viện không có lớp nổi vì có quảng cáo trong lưới: chọn "Nhận".
- Thiết kế trạng thái quảng cáo: chọn "Vẽ lại trên Figma trước" (trước khi duyệt plan).

## Những gì chưa cần làm lần này

Không thêm vị trí quảng cáo, khóa Remote Config hay sự kiện phân tích. Không bật MAX.

## Quảng cáo, dữ liệu, file tĩnh

Không có vị trí mới. Nhánh gộp có sẵn: `template-sync/take-ads-7bf8c3b` (dựng ngày 2026-11-21, `:core:ads` qua 2084 test; 25 test của feature còn đỏ vì kích thước mới).

## Tham khảo

- Dự án mẫu: T-090, T-091, T-092 (thanh đáy), T-108, T-109, T-110 (thẻ native theo JBase), PR #526, #531 của mẫu.
- `docs/template-sync.txt`, `docs/ops/to-app.md` mục 5 (phần lớn: giữ lại rồi đưa vào bằng plan).
