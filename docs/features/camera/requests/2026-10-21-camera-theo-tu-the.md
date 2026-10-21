---
date: 2026-10-21
author: AI, từ bản đề xuất đã duyệt (docs/research/proposal.md)
feature: camera
status: specified
---

# YÊU CẦU: CAMERA CHỤP THEO TƯ THẾ VÀ MÀN XEM LẠI

## Mình muốn gì

Camera là màn chính của app. Người dùng chọn một tư thế ở dải ngay trên nút chụp, nét viền tư thế phủ lên khung xem trước, thẻ bước chỉ cách tạo dáng. Người dùng chỉnh được nét viền (độ mờ, cỡ, kéo, lật, khóa, ẩn, đặt lại), dùng hẹn giờ, lưới, flash, đổi camera, zoom. App nhận ra dáng người trong khung và chấm điểm khớp với tư thế. Chụp xong mở màn xem lại: lưu, chụp lại, chia sẻ, sang tư thế tiếp.

## Vì sao

Đây là lý do người dùng cài app. Bản đề xuất chỉ ra các điểm app mẫu làm kém: quảng cáo che điều khiển camera (bài học 3), nét viền khó nhìn hoặc không chỉnh được (bài học 5), không có màn xem lại (bài học 6), ảnh tư thế không kèm hướng dẫn (bài học 10), lưu ảnh lộn xộn (bài học 13). Dev chốt làm nhận dạng dáng và điểm khớp ngay trong bản đầu (2026-10-21).

## Người dùng sẽ trải nghiệm thế nào

Mở app, camera hiện ngay với tư thế dùng lần trước. Người dùng chọn chip "Cặp đôi", chạm một ô ở dải, nét viền đổi theo. Bật hẹn giờ 3 giây, đặt máy, đứng vào nét viền. Điểm khớp tăng dần, tới ngưỡng thì báo "Đúng dáng rồi". Bấm chụp, màn xem lại hiện ảnh, bấm "Lưu vào thư viện", rồi "Tư thế tiếp" để chụp dáng kế.

## Những gì chưa cần làm lần này

Thư viện ảnh đã chụp trong app, sửa ảnh, so sánh trước sau, bộ lọc màu, lưu bản ảnh kèm nét viền (bản đề xuất, câu 12.11), tự chụp khi đạt điểm.

## Quảng cáo, dữ liệu, file tĩnh

- Quảng cáo mức B (câu 9.1): `review_interstitial`, toàn màn hình khi rời màn xem lại sau khi đã lưu ảnh. Không quảng cáo nào trên camera và trong lúc xem lại.
- Sự kiện (câu 9.2): `camera_permission_result`, `pose_selected`, `overlay_adjusted`, `photo_captured`, `photo_saved`, `content_load_failed`.
- File trên R2: ảnh nhỏ và nét viền của tư thế (qua module dữ liệu tư thế dùng chung).
- Hỏi đánh giá trong app (câu 12.8): Play In-App Review sau khi đã lưu ít nhất 3 ảnh.

## Tham khảo

- Bản đề xuất: mục 4 (bài học 1, 3, 5, 6, 10, 13, 16), mục 5 (tính năng và ghi chú nhận dạng dáng), mục 6, mục 7, mục 9, mục 12 (câu 6, 8, 10, 11, 13, 14).
- Thiết kế: trang Screens trên Figma, section Camera và Review.
