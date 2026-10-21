---
id: T-025
title: Màn cài đặt
feature: settings
spec: features/settings/spec.md
scenarios: [KB-1, KB-2, KB-3, KB-4, KB-5, KB-6]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-007]
sprint:
plan:
created: 2026-10-21
---

# T-025: MÀN CÀI ĐẶT

## Mô tả

Tạo `:feature:settings`: giao diện sáng tối, âm khi chụp, ngôn ngữ, tùy chọn quyền riêng tư, chính sách quyền riêng tư, phiên bản. Mở từ nút bánh răng ở camera và thư viện.

## Phạm vi

- Thành phần mới: `SettingsRow` (bốn kiểu), `SectionHeader`; hộp chọn giao diện và ngôn ngữ.
- Đổi ngôn ngữ: trang ngôn ngữ của app trên Android 13 trở lên, hộp chọn trong app dưới Android 13.
- "Tùy chọn quyền riêng tư" mở lại hộp thoại đồng ý của `:core:ads`, ẩn khi không cần.
- Đường dẫn chính sách trong `RemoteSettings`, mặc định https://studio.vgl.io.vn/privacy-policy.
- Camera và màn xem lại luôn tối; cờ `feature_settings_enabled`.

**Không bao gồm:**

- Không có.

## Rủi ro sơ bộ

Điểm chặn 5 (hộp thoại đồng ý), 6 (liên kết ra ngoài) và 7 (thành phần mới trong core): rủi ro cao.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
