---
id: T-005
title: Màn hình loading dùng được trên màn hình thấp
feature: loading
spec: features/loading/spec.md
scenarios: [KB-2, KB-3]
owner: Nhật
status: on-staging
risk: low
size: S
depends_on: []
sprint:
plan: features/loading/plans/T-005-man-hinh-thap.md
created: 2026-10-17
---

# T-005: MÀN HÌNH LOADING DÙNG ĐƯỢC TRÊN MÀN HÌNH THẤP

## Mô tả

Sửa phản hồi FB-20261017-01: trên màn hình thấp (360 x 640 dp trở xuống), ở trạng thái lỗi, chân trang đè lời hướng dẫn và nút "Thử lại" bị cắt. Sau khi xong, nút "Thử lại" luôn thấy và bấm được, chữ không chồng nhau, và màn hình trên khung chuẩn 411 x 891 giữ nguyên như thiết kế. Đây cũng là lần đầu áp dụng quy tắc khung thấp của L-028.

## Phạm vi

- Bố cục của `LoadingScreen`: nội dung và chân trang chung một cột cuộn được; khoảng trống phía trên co lại trên màn hình thấp.
- Test giao diện và screenshot test ở khung `w360dp-h640dp-xxhdpi`.
- Ghi quy tắc màn hình thấp vào mục 6, mục 7 của `design/README.md`.

**Không bao gồm:**

- FB-20261017-02 (tải cấu hình lần đầu), chờ xác nhận trên máy thật.
- Đổi thiết kế ở khung chuẩn.

## Rủi ro sơ bộ

Không chạm điểm chặn: chỉ sửa bố cục trong `:feature:loading`. Rủi ro chính là làm xê dịch màn hình ở khung chuẩn; bốn ảnh chuẩn hiện có phải giữ nguyên.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
