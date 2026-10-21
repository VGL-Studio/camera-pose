---
id: FB-20261017-01
title: Màn hình loading trên máy thấp, chân trang đè lời hướng dẫn, nút "Thử lại" bị cắt
reporter: AI (phiên thử TS-20261017-01)
source: staging
build: 0.1.0-staging, commit caf0746
feature: loading
ticket: T-003
severity: nang
status: fixed
created: 2026-10-17
---

# FB-20261017-01: MÀN HÌNH LOADING TRÊN MÁY THẤP, CHÂN TRANG ĐÈ LỜI HƯỚNG DẪN, NÚT "THỬ LẠI" BỊ CẮT

## Điều đã xảy ra

Trên màn hình 320 x 640 dp, ở trạng thái lỗi, dòng "Không gian riêng cho việc của bạn" nằm chồng lên dòng cuối của lời hướng dẫn, và nút "Thử lại" chỉ còn mép trên ở đáy màn hình. Phát hiện trong phiên thử [TS-20261017-01](../../../test-sessions/TS-20261017-01.md), mục P-1.

## Các bước làm lại

1. Dùng máy hoặc emulator có màn hình thấp (khoảng 640 dp chiều cao), cài bản staging.
2. Bật chế độ máy bay, xóa dữ liệu app, mở app.
3. Xem màn hình lỗi.

## Mong đợi

Nút "Thử lại" luôn thấy và bấm được, chữ không chồng lên nhau (KB-2, KB-3). Trên màn hình thấp, nội dung cuộn được hoặc chân trang nhường chỗ.

## Ảnh chụp, video

Ảnh `offline.png` trong artifact `device-session` của lần chạy 39044089126 (giữ 7 ngày). Chạy lại `device-session.yml` để có ảnh mới.

## Thiết bị

- Máy: emulator Google sdk_gphone64_x86_64
- Phiên bản Android: 14 (API 34)
- Ngôn ngữ, chế độ tối: tiếng Việt theo app, sáng

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

- **Loại:** lỗi so với spec
- **Xử lý:** sửa trong sprint này
- **Lý do:** 
- **Liên kết:** T-005 (PR #51), kiểm chứng ở TS-20261018-01

AI đề xuất: lỗi so với spec (KB-2). Nguyên nhân trong `LoadingScreen.kt`: cột nội dung bắt đầu ở 25% chiều cao, không cuộn, chân trang ghim riêng ở đáy. Sửa bằng ticket mới T-005 (`features/loading/plans/T-005-man-hinh-thap.md`), vì T-003 đã xong: nội dung và chân trang cùng một cột cuộn được, khoảng trống trên co lại khi màn hình thấp; thêm screenshot test ở khung thấp (ví dụ `w360dp-h640dp`), không đổi ảnh chuẩn của khung 411 x 891.

**Cập nhật 2026-10-18, AI:** đã kiểm chứng bản sửa T-005 (PR #51) trên emulator 320 x 640. Phiên thử [TS-20261018-01](../../../test-sessions/TS-20261018-01.md): kịch bản mất mạng có hiệu lực (L-031), màn hình lỗi hiện trọn, chân trang nằm dưới nút "Thử lại", không chồng lên lời hướng dẫn. Trên JVM, test khung thấp của T-005 và số đo của plan T-006 cho cùng kết quả. AI đề xuất: chuyển sang `fixed`, liên kết T-005 (PR #51).
