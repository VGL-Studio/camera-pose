---
id: T-050
title: Chữ của camera và màn xem lại theo ngôn ngữ đã chọn
feature: settings
spec: features/settings/spec.md
scenarios: [KB-4]
owner: Nhật
status: planned
risk: medium
size: M
depends_on: []
sprint:
plan: features/settings/plans/T-050-dich-chu-camera.md
created: 2026-10-25
---

# T-050: CHỮ CỦA CAMERA VÀ MÀN XEM LẠI THEO NGÔN NGỮ ĐÃ CHỌN

## Mô tả

Đổi ngôn ngữ trong cài đặt thì màn camera vẫn hiện tiếng Việt ("Bước", "Tất cả"), nên người dùng tưởng đổi không được. Thử trên máy thật ngày 2026-10-25 (Xiaomi, Android 15): hệ thống đã nhận `en-US` cho app (`cmd locale get-app-locales`), màn cài đặt đã sang tiếng Anh, còn màn camera vẫn tiếng Việt. Nguyên nhân: `values/` của `:feature:camera` viết bằng tiếng Việt (64/69 chuỗi trong `strings.xml`, 18/19 trong `strings_review.xml`) và không có thư mục ngôn ngữ nào khác. Làm xong thì camera và màn xem lại hiện đúng ngôn ngữ đã chọn trong 7 ngôn ngữ phát hành; ngôn ngữ máy chưa có thì hiện tiếng Anh.

## Phạm vi

- `feature/camera/src/main/res/values/strings.xml` và `strings_review.xml` chuyển sang tiếng Anh; chữ tiếng Việt hiện có chuyển nguyên sang `values-vi/`.
- Thêm `values-hi`, `values-pt-rBR`, `values-b+es+419`, `values-in`, `values-th`, theo cách của settings và onboarding: AI dịch, người biết tiếng duyệt trước khi phát hành (spec onboarding, câu hỏi 1).
- Test chặn kiểu `SettingsStringsTest`: đủ 7 ngôn ngữ, mọi khóa có bản dịch khác tiếng Anh, `values/` không có chữ tiếng Việt.
- Test giao diện và screenshot test của camera và màn xem lại đang dựa vào chữ tiếng Việt thì thêm `vi` vào `qualifiers` (như `SettingsScreenScreenshotTest`), để ảnh chuẩn giữ nguyên và vẫn khớp ảnh thiết kế tiếng Việt. Plan phải ghi rõ danh sách file test được đổi và lý do, vì đây là sửa test.
- Một ảnh `_small` ở ngôn ngữ có chữ dài (ví dụ `hi` hay `pt-BR`) để kiểm chữ không bị cắt hay tràn ở chip và nút; plan quyết ảnh nào.

**Không bao gồm:**

- Chữ của `:feature:poses` và `:core:designsystem` (T-051).
- Tên nhóm và nội dung tư thế: đến từ catalog, đã theo ngôn ngữ máy.

## Rủi ro sơ bộ

Chỉ đổi tài nguyên chữ và test trong `:feature:camera`, không đổi logic. Rủi ro nằm ở việc đổi `qualifiers` của nhiều test: làm sai thì ảnh chuẩn đổi, hoặc test qua mà không còn kiểm đúng chữ. Bản dịch do AI làm cần người duyệt trước khi phát hành.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
