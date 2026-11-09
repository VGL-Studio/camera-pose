---
id: T-091
title: Luồng giới thiệu luôn 4 trang, có ảnh minh họa và chữ mới
feature: onboarding
spec: features/onboarding/spec.md   # confirmed 2026-11-09, câu 10, 11
scenarios: [KB-1, KB-5, KB-6]
owner: Nhật
status: approved
risk: high
size: M
depends_on: []
sprint:
plan: features/onboarding/plans/T-091-luong-gioi-thieu-4-trang-co-anh.md
auto_run:
created: 2026-11-09
---

# T-091: LUỒNG GIỚI THIỆU LUÔN 4 TRANG, CÓ ẢNH MINH HỌA VÀ CHỮ MỚI

## Mô tả

Luồng giới thiệu của `:feature:onboarding` luôn có 4 trang (`intro`, `poses`, `guide`, `camera`), không còn phụ thuộc cờ `feature_premium_poses_enabled` và không còn trang tư thế premium. Mỗi trang có một ảnh minh họa lớn, tiêu đề một dòng và lời giới thiệu hai dòng, theo thiết kế đã duyệt ngày 2026-11-09. Người dùng thấy cùng một luồng 4 trang, mỗi trang có thẻ native như hiện nay.

## Phạm vi

- Danh sách trang cố định 4 trang; bỏ phần theo dõi cờ premium trong ViewModel.
- Bố cục mới của mọi trang: ảnh, tiêu đề, lời. Bỏ huy hiệu, hàng điểm và hình minh họa vẽ bằng thành phần.
- Bốn ảnh WebP đóng gói trong `res/` của feature, lấy từ `docs/features/onboarding/design/handoff/`.
- Chữ trong bong bóng của ảnh trang 3, co giãn theo ảnh.
- Chữ mới ở 7 ngôn ngữ phát hành (spec, câu 1).
- Screenshot test và ảnh chuẩn theo 20 ảnh thiết kế; bỏ ảnh thiết kế và ảnh chuẩn `premium`.
- Kịch bản emulator của luồng đổi từ trang `premium` sang trang `guide`.

**Không bao gồm:**

- Đổi định dạng, cỡ, pool hay khóa của `native_onboarding`.
- Trang thứ 5 về chia sẻ ảnh (spec, câu 10).
- Tính năng tư thế premium ở các màn khác.

## Rủi ro sơ bộ

Chạm điểm chặn quảng cáo: khi cờ premium tắt, luồng tăng từ 3 lên 4 trang nên có thêm một lượt thẻ native. Ảnh bitmap đầu tiên trong `res/` của feature. Trước khi phát hành cần kiểm khai báo đối tượng trên Play Console (spec, câu 11).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
