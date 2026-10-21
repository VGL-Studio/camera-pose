---
id: T-003
title: Màn hình loading theo thiết kế Figma
feature: loading
spec: features/loading/spec.md
scenarios: [KB-1, KB-2, KB-3]
owner: Nhật
status: on-staging
risk: medium
size: M
depends_on: [T-002]
sprint:
plan: features/loading/plans/T-003-giao-dien-loading-theo-thiet-ke.md
created: 2026-10-16
---

# T-003: MÀN HÌNH LOADING THEO THIẾT KẾ FIGMA

## Mô tả

Vẽ lại màn hình loading theo thiết kế trong `features/loading/design/`: logo, tên app, khẩu hiệu, nhãn và thanh tiến trình, dòng hướng dẫn, khối lỗi có tiêu đề, lời hướng dẫn và nút "Thử lại" có biểu tượng, chân trang. Luồng xử lý (tải cấu hình, ba file, 2 giây, thử lại) giữ nguyên như T-001. Đây là lần đầu chạy trọn đường thiết kế, Compose, ảnh so sánh (ADR-0020).

## Phạm vi

- `:feature:loading`: `LoadingScreen` theo thiết kế, chữ mới trong `strings.xml`, ba biểu tượng vector.
- Tên app lấy từ `app_name` của app đang chạy, truyền vào màn hình.
- Screenshot test cùng tên bốn ảnh thiết kế; test giao diện kiểm tra chữ theo bảng chữ.
- `scripts/design-compare.sh loading`, đưa `design/review/` vào PR.

**Không bao gồm:**

- Màu, phông, `BrandLogo`, `ProgressBar`, biểu tượng của `PrimaryButton`: thuộc T-002.
- Thay đổi luồng xử lý, sự kiện phân tích, cờ tính năng.

## Rủi ro sơ bộ

Không chạm điểm chặn: chỉ sửa giao diện trong một module feature, dùng thành phần của T-002. Rủi ro chính là bố cục lệch so với thiết kế, có ảnh so sánh và rà soát độc lập để bắt.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
