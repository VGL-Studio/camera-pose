---
id: T-051
title: Chữ của thư viện tư thế và hệ thống thiết kế theo ngôn ngữ đã chọn
feature: settings
spec: features/settings/spec.md
scenarios: [KB-4]
owner: Nhật
status: planned
risk: medium
size: M
depends_on: []
sprint:
plan: features/settings/plans/T-051-dich-chu-thu-vien-va-thiet-ke.md
created: 2026-10-25
---

# T-051: CHỮ CỦA THƯ VIỆN TƯ THẾ VÀ HỆ THỐNG THIẾT KẾ THEO NGÔN NGỮ ĐÃ CHỌN

## Mô tả

Cùng lỗi với T-050, ở các module khác. `values/` của `:feature:poses` viết bằng tiếng Việt (30/30 chuỗi), của `:core:designsystem` cũng vậy (5/5 chuỗi: hộp thoại mất mạng và câu khẩu hiệu đi cùng logo). Cả hai không có thư mục ngôn ngữ nào khác, nên đổi ngôn ngữ trong cài đặt thì thư viện, chi tiết tư thế và hộp thoại mất mạng vẫn hiện tiếng Việt. Làm xong thì các màn này hiện đúng ngôn ngữ đã chọn trong 7 ngôn ngữ phát hành; ngôn ngữ máy chưa có thì hiện tiếng Anh.

## Phạm vi

- `feature/poses/src/main/res/values/strings.xml` và `core/designsystem/src/main/res/values/strings.xml` chuyển sang tiếng Anh; chữ tiếng Việt hiện có chuyển nguyên sang `values-vi/`.
- Thêm `values-hi`, `values-pt-rBR`, `values-b+es+419`, `values-in`, `values-th`: AI dịch, người biết tiếng duyệt trước khi phát hành (spec onboarding, câu hỏi 1). Câu khẩu hiệu `brand_tagline` là của app (ADR-0008): plan hỏi người chốt câu tiếng Anh.
- Test chặn kiểu `SettingsStringsTest` cho từng module: đủ 7 ngôn ngữ, mọi khóa có bản dịch khác tiếng Anh, `values/` không có chữ tiếng Việt.
- Test giao diện và screenshot test của thư viện, chi tiết tư thế và thành phần của `:core:designsystem` đang dựa vào chữ tiếng Việt thì thêm `vi` vào `qualifiers`, để ảnh chuẩn giữ nguyên và vẫn khớp ảnh thiết kế tiếng Việt. Plan phải ghi rõ danh sách file test được đổi và lý do, vì đây là sửa test.
- Một ảnh `_small` ở ngôn ngữ có chữ dài (ví dụ `hi` hay `pt-BR`) để kiểm chữ không bị cắt hay tràn; plan quyết ảnh nào.

**Không bao gồm:**

- Chữ của `:feature:camera` (T-050).
- Tên, bước, lỗi hay gặp của tư thế: đến từ catalog, đã theo ngôn ngữ máy.

## Rủi ro sơ bộ

Đổi tài nguyên chữ của một module core (`:core:designsystem`), mọi màn có hộp thoại mất mạng đều đổi chữ theo. Không đổi logic. Rủi ro nằm ở việc đổi `qualifiers` của nhiều test, như T-050. Bản dịch do AI làm cần người duyệt trước khi phát hành.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
