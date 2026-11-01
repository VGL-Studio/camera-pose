---
id: ADR-0036
title: Quảng cáo lệch có chủ đích khỏi PARTNER ở bốn điểm học từ JBase, ghi rõ trong luật nghiệp vụ
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-29
deciders: [Nhật]
supersedes:                  # thay luật D-04 và danh sách định dạng MC-02 của docs/ops/ads-business-rules.md; bổ sung ADR-0026, không thay ADR nào
superseded_by:
---

# ADR-0036: QUẢNG CÁO LỆCH CÓ CHỦ ĐÍCH KHỎI PARTNER Ở BỐN ĐIỂM HỌC TỪ JBASE

## Bối cảnh

ADR-0026 chọn hành vi theo bộ PARTNER của AdsKit; mọi chỗ lệch phải ghi rõ (ADR-0026, mục Hệ quả; `ads-business-rules.md`, mục 12). Ngày 2026-10-29 AI đối chiếu `:core:ads` với gói JBase (Unity Android, cùng gốc với AdsKit: cùng hằng số, cùng tên sự kiện `af_inters_*`). JBase có vài cách làm mà PARTNER không có, và khi rà lại AI thấy vài chỗ hở của chính dự án mẫu (yêu cầu `features/ads/requests/2026-10-29-hoc-tu-jbase.md`). Bốn điểm dưới đây đổi hành vi so với PARTNER nên cần một quyết định; các điểm còn lại là sửa lỗi so với luật đã có, không cần ADR.

## Quyết định

1. **Quảng cáo trong màn hình dừng làm mới và dừng gửi yêu cầu khi một quảng cáo toàn màn hình vẽ trong app đang hiện** (native sau inter, native toàn màn hình độc lập). Quảng cáo đã có giữ nguyên, chạy lại khi màn che đóng. Interstitial, app open, rewarded là Activity riêng của SDK nên đã dừng theo vòng đời (B-02). JBase dừng tự làm mới banner và MREC khi có quảng cáo toàn màn hình; PARTNER không có luật này.
2. **Thanh đáy chừa chỗ ngay lúc khe đang tải**, đúng chiều cao của khe (80 dp với native, chiều cao khung với banner), thay luật D-04 (chỉ chừa khi khe đã có quảng cáo). Nạp lỗi thì co về 0 như cũ. Người phụ trách sản phẩm chọn ngày 2026-10-29: nội dung không bị đẩy khi quảng cáo về; chấp nhận một khoảng trống tạm khi không có quảng cáo.
3. **Thêm định dạng `banner_mrec`** (banner 300 x 250) vào danh sách đóng của MC-02, thay ý "native medium thay MREC" của KB-2: native medium vẫn là lựa chọn mặc định, MREC dùng khi spec của app cần. Người phụ trách sản phẩm chọn ngày 2026-10-29.
4. **Có cổng "không hiện quảng cáo"** (người dùng đã mua gói bỏ quảng cáo): `:core:ads` nhận tín hiệu từ app; khi bật thì không tải, không hiện mọi định dạng như công tắc tổng `is_show_ads` tắt, hành động có điều kiện làm thẳng. Dự án mẫu chưa có mua hàng; tín hiệu mặc định là tắt.

Chi tiết từng điểm (giá trị, trường hợp biên) chốt ở spec `ads`, câu hỏi 36 tới 39, rồi ghi vào `ads-business-rules.md` ở plan của từng ticket.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Bốn điểm lệch, ghi rõ trong luật (đã chọn) | Bịt rủi ro chính sách; mỗi điểm có lý do | Xa PARTNER hơn; luật và test phải sửa (D-04, MC-02) |
| Giữ đúng PARTNER | Không đổi luật đã chốt | Banner làm mới dưới màn che, nội dung nhảy; không có MREC hay cổng bỏ quảng cáo |
| Thêm hạn dùng 1 giờ cho interstitial | Không hiện quảng cáo tải quá lâu | JBase, chuẩn nghiệp vụ mà người phụ trách sản phẩm chọn ngày 2026-10-29, không có; bỏ (spec `ads`, câu hỏi 35) |

## Hệ quả

- **Tích cực:** không có lượt hiển thị dưới màn che; thanh đáy không làm nội dung nhảy; app có thêm MREC và một chỗ nối gói bỏ quảng cáo.
- **Phải chấp nhận:** khi thanh đáy không có quảng cáo thì có lúc hở khoảng trống tới khi nạp lỗi; luật D-04 và MC-02 của bản đặc tả AdsKit không còn đúng nguyên văn; thêm một khe và một mã thử (`banner_mrec`); test của D-04 viết lại theo ADR-0031.
- **Việc AI phải tuân thủ:** quảng cáo trong màn hình không gửi yêu cầu và không làm mới khi quảng cáo toàn màn hình vẽ trong app đang hiện; thanh đáy chừa chỗ đúng chiều cao của khe từ lúc khe đang tải, nạp lỗi thì co về 0; `banner_mrec` là một định dạng của danh sách đóng, chỉ dùng khi spec của app khai báo; cổng không hiện quảng cáo bật thì mọi định dạng như công tắc tổng tắt và hành động có điều kiện làm thẳng; ghi mỗi điểm lệch PARTNER vào `ads-business-rules.md` mục 12 kèm mã ADR.
