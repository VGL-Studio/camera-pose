---
id: T-012
title: Màn chào và xin quyền camera
feature: onboarding
spec: features/onboarding/spec.md
scenarios: [KB-1, KB-2, KB-3, KB-4]
owner: Nhật
status: planned
risk: high
size: M
depends_on: [T-007, T-010]
sprint:
plan: features/onboarding/plans/T-012-man-chao.md
created: 2026-10-21
---

# T-012: MÀN CHÀO VÀ XIN QUYỀN CAMERA

## Mô tả

Tạo `:feature:onboarding`: một màn chào hiện ở lần mở đầu tiên, nút "Bật camera" hỏi quyền camera rồi sang camera. Lần sau vào thẳng camera.

## Phạm vi

- Màn chào theo `features/onboarding/design/README.md`, chữ đủ 7 ngôn ngữ.
- Hỏi quyền camera; cho phép hay từ chối đều sang camera; lưu "đã qua màn chào".
- Sự kiện `onboarding_complete`, `camera_permission_result` (`source`: `onboarding`).
- Cờ `feature_onboarding_enabled`; đăng ký trong `AppNavHost`.

**Không bao gồm:**

- Camera ở trạng thái chưa có quyền (T-013).

## Rủi ro sơ bộ

Điểm chặn 4 (sự kiện phân tích mới): rủi ro cao. Quyền `CAMERA` do dev thêm trước phiên tự chạy (spec `camera`, câu hỏi 9).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
