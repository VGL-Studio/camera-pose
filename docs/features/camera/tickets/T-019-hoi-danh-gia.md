---
id: T-019
title: Hỏi đánh giá app trong app
feature: camera
spec: features/camera/spec.md
scenarios: [KB-10]
owner: Nhật
status: ready
risk: high
size: S
depends_on: [T-016, T-018]
sprint:
plan:
created: 2026-10-21
---

# T-019: HỎI ĐÁNH GIÁ APP TRONG APP

## Mô tả

Sau khi người dùng đã lưu ít nhất 3 ảnh, lúc rời màn xem lại, app gọi hộp đánh giá của Google Play, tối đa một lần mỗi 30 ngày, không cùng lúc với quảng cáo toàn màn hình.

## Phạm vi

- Gọi Play In-App Review; tham số trong `RemoteSettings`; cờ `feature_in_app_review_enabled`.

**Không bao gồm:**

- Thêm thư viện In-App Review (dev làm trước).

## Rủi ro sơ bộ

Dùng SDK mới (do dev thêm) và điều kiện thời gian khó kiểm chứng: rủi ro cao.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
