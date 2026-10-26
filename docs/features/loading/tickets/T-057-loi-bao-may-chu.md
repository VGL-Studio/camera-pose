---
id: T-057
title: Màn loading báo "máy chủ chưa phản hồi" khi máy có mạng mà vẫn không tải được
feature: loading
spec: features/loading/spec.md
scenarios: [KB-2, KB-5]
owner: Nhật
status: on-staging
risk: medium
size: M
depends_on: []
sprint:
plan: features/loading/plans/T-057-loi-bao-may-chu.md
created: 2026-10-26
---

# T-057: MÀN LOADING BÁO "MÁY CHỦ CHƯA PHẢN HỒI" KHI MÁY CÓ MẠNG MÀ VẪN KHÔNG TẢI ĐƯỢC

## Mô tả

Hiện màn loading luôn bảo "Kết nối đang gián đoạn. Vui lòng kiểm tra mạng..." khi một phần việc không xong, kể cả khi máy đang có mạng mà máy chủ không trả lời (FB-20261022-03: người thử bấm "Thử lại" 4 lần trong 5 giây rồi bỏ). Sau task này, khi hệ thống xác nhận máy có Internet mà phần việc vẫn không xong, màn hình hiện lời thứ hai "Máy chủ chưa phản hồi. Vui lòng thử lại sau ít phút." bằng ngôn ngữ của app (7 ngôn ngữ); khi máy không có mạng thì giữ lời cũ.

## Phạm vi

- Khi một phần việc không xong, đọc trạng thái mạng của hệ thống: mạng mặc định có Internet và đã được hệ thống xác nhận thì là lỗi máy chủ, còn lại là mất mạng.
- Trạng thái lỗi của màn loading mang lý do; lời hướng dẫn chọn theo lý do; tiêu đề, nhãn, thanh tiến trình và nút "Thử lại" giữ nguyên.
- Chuỗi mới `loading_error_server` cho 7 ngôn ngữ (tiếng Anh mặc định, tiếng Việt, `hi`, `pt-BR`, `es-419`, `in`, `th`).
- Log cảnh báo của phần việc không xong ghi thêm lý do (`cause=OFFLINE` hay `cause=SERVER`).
- Test: ViewModel, đọc trạng thái mạng, màn hình, chữ 7 ngôn ngữ, màn thấp ở 7 ngôn ngữ, 2 ảnh chuẩn mới.

**Không bao gồm:**

- Đổi `:core:assets`, `:core:poses` hay `:core:network`: lý do lấy từ trạng thái mạng của hệ thống lúc lỗi, không từ loại lỗi của lần tải.
- Chờ hay khóa nút "Thử lại" sau mỗi lần thất bại (plan T-057, mục 8, câu 4).
- Frame Figma mới cho lời báo thứ hai (plan T-057, mục 8, câu 3).

## Rủi ro sơ bộ

Trung bình: chỉ trong `:feature:loading`, không chạm module core, không thêm quyền (`ACCESS_NETWORK_STATE` đã có). Cần đổi spec loading (KB-2, kịch bản mới KB-5) và người phụ trách sản phẩm xác nhận lại trước khi duyệt plan.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
