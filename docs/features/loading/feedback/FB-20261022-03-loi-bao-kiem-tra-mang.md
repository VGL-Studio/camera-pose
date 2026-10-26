---
id: FB-20261022-03
title: Màn loading bảo kiểm tra mạng khi máy có mạng nhưng máy chủ không tới được
reporter: AI (phân tích phiên thử TS-20261022-02)
source: staging
build: 0.1.0-staging.3
feature: loading
ticket: T-008
severity: gop-y
status: planned
created: 2026-10-22
---

# FB-20261022-03: MÀN LOADING BẢO KIỂM TRA MẠNG KHI MÁY CÓ MẠNG NHƯNG MÁY CHỦ KHÔNG TỚI ĐƯỢC

## Điều đã xảy ra

Trong phiên thử TS-20261022-02 máy có mạng (cấu hình tải xong), hộp thoại mất mạng không hiện, nhưng catalog không tải được vì máy chủ file không tới được (FB-20261022-02). Màn loading hiện lời "The connection was interrupted. Check your network and try again to continue." Người thử bấm "Thử lại" 4 lần trong 5 giây, lần nào cũng thất bại ngay, rồi dừng.

## Các bước làm lại

1. Tạo tình huống máy có mạng nhưng máy chủ file không tới được (ví dụ FB-20261022-02 còn lỗi, hoặc chặn tên miền file tĩnh bằng DNS riêng).
2. Mở app lần đầu.
3. Xem lời báo lỗi trên màn loading.

## Mong đợi

Lời hướng dẫn dễ hiểu và đúng với tình huống (spec loading, KB-2): khi máy đang có mạng, nói rằng máy chủ chưa phản hồi và gợi ý thử lại sau một lúc, thay vì bảo kiểm tra mạng.

## Ảnh chụp, video

Không có.

## Thiết bị

- Máy: Xiaomi 23021RAAEG
- Phiên bản Android: 15 (API 35)
- Ngôn ngữ, chế độ tối: không ghi

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

Đề xuất của AI: góp ý mới, cần đổi spec loading (thêm lời báo thứ hai và chữ dịch cho 7 ngôn ngữ). Ưu tiên thấp hơn FB-20261022-02.

- **Xử lý:** Tuấn đưa vào phiên sau ngày 2026-10-26: ticket T-057, plan `features/loading/plans/T-057-loi-bao-may-chu.md`; đổi spec loading (KB-2, câu hỏi 11), chờ xác nhận lại.
- **Liên kết:** FB-20261022-02 (máy chủ file không tới được), TS-20261022-02.
