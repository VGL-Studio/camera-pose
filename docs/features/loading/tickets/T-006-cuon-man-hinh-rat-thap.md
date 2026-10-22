---
id: T-006
title: Test cuộn tới nút "Thử lại" trên màn hình rất thấp
feature: loading
spec: features/loading/spec.md
scenarios: [KB-2, KB-3]
owner: Nhật
status: on-staging
risk: low
size: S
depends_on: [T-005]
sprint:
plan: features/loading/plans/T-006-cuon-man-hinh-rat-thap.md
created: 2026-10-18
---

# T-006: TEST CUỘN TỚI NÚT "THỬ LẠI" TRÊN MÀN HÌNH RẤT THẤP

## Mô tả

T-005 (PR #51) cho nội dung và chân trang của màn hình loading cuộn chung khi dài hơn màn hình. Các test hiện có ở khung 360 x 640 không cần cuộn, nên chưa test nào chạy qua đường cuộn. Rà soát độc lập của RUN-20261017-03 đề xuất thêm. Ticket này thêm test giao diện ở khung `w320dp-h480dp`, nơi nội dung dài hơn màn hình: cuộn tới nút "Thử lại" thì thấy và bấm được, chân trang nằm dưới nút. Đồng thời sửa chú thích lớp `LoadingScreenScreenshotTest`: chú thích nói mọi test trùng tên ảnh thiết kế, nhưng hai ảnh `_small` (L-028) không có ảnh thiết kế.

## Phạm vi

- Hai test giao diện ở khung `w320dp-h480dp-xxhdpi` trong `LoadingScreenTest`.
- Chú thích lớp `LoadingScreenScreenshotTest`.

**Không bao gồm:**

- Đổi bố cục hay code của màn hình (đo trên code hiện tại cho thấy đã đúng, plan mục 4).
- Ảnh chuẩn screenshot mới.

## Rủi ro sơ bộ

Không chạm điểm chặn: chỉ thêm test và sửa chú thích trong `:feature:loading`, không đổi code chạy.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
