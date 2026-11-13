---
id: FB-20261113-01
title: Lưu ảnh từ hộp thoại khi rời màn xem lại thì phần hỏi đánh giá app nhận số ảnh đã lưu chậm một ảnh
reporter: AI
source: staging
build: 0.1.0-staging.24
feature: camera
ticket: T-096
severity: nhe
status: triaged
created: 2026-11-13
---

# FB-20261113-01: LƯU ẢNH TỪ HỘP THOẠI KHI RỜI MÀN XEM LẠI THÌ PHẦN HỎI ĐÁNH GIÁ NHẬN SỐ ẢNH CHẬM MỘT ẢNH

Phát hiện P-1 của phiên thử [TS-20261113-01](../../../test-sessions/TS-20261113-01.md).

## Điều đã xảy ra

Ở lần mở đầu sau khi cài, người thử lưu ảnh thứ hai bằng cách bấm "Tư thế tiếp" khi chưa lưu, rồi chọn lưu trong hộp thoại. App lưu ảnh và rời màn ngay. Quảng cáo không hiện vì giãn cách (`THROTTLED`). Phần hỏi đánh giá được gọi 17 ms sau khi lưu, nhưng nhận `saved=1` thay vì 2:

```text
08:49:32.548 I VGL.Review: Photo saved
08:49:32.553 I VGL.Review: Review left: next_pose
08:49:32.570 I VGL.InAppReview: Review skipped: not_due saved=1
```

Có thể do `ReviewScreen.kt:75-79` đọc số đếm qua `stateIn` rồi `rememberUpdatedState`: số đó chỉ có giá trị mới sau khi DataStore phát lại và màn hình vẽ lại. Đây là giả thuyết, xem báo cáo phiên.

## Các bước làm lại

1. Xóa dữ liệu app, bật `feature_in_app_review_enabled`, để quảng cáo `review_interstitial` không hiện ở lần rời màn đang thử (khóa tắt, hoặc đang trong giãn cách).
2. Chụp và lưu 2 ảnh theo cách thường (bấm Lưu, rồi rời màn).
3. Chụp ảnh thứ 3, bấm "Tư thế tiếp" khi chưa lưu, chọn lưu trong hộp thoại.
4. Xem log: `Review skipped: not_due saved=2`, và hộp đánh giá không hiện.

## Mong đợi

Phần hỏi đánh giá nhận số ảnh đã lưu tính cả ảnh vừa lưu (`saved=3`) và gọi hộp đánh giá của Google Play (KB-10, tiêu chí 1).

## Ảnh chụp, video

Không có.

## Thiết bị

- Máy: Xiaomi 23021RAAEG
- Phiên bản Android: 15 (API 35)
- Ngôn ngữ, chế độ tối: không ghi trong phiên

## Phân loại

- **Loại:** lỗi so với spec. KB-10, tiêu chí 1 của `features/camera/spec.md`: người dùng đã lưu đủ 3 ảnh và rời màn sau khi lưu thì app gọi hộp đánh giá; ở đây số ảnh đưa vào chậm một ảnh nên lần gọi bị bỏ.
- **Xử lý:** sửa, ticket mới T-096 (T-019 đã xong). Nhật chọn ngày 2026-11-13.
- **Lý do:** lệch tiêu chí của spec đã chốt; người dùng lưu từ hộp thoại khi rời màn có thể không bao giờ được hỏi đánh giá ở lần đủ điều kiện.
- **Liên kết:** [T-096](../tickets/T-096-dem-anh-khi-hoi-danh-gia.md)
