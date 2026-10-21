---
id: T-004
title: Đủ vai trò màu thương hiệu và giãn cách chữ như thiết kế
feature: loading
spec: features/loading/spec.md
scenarios: [KB-1, KB-2]
owner: Nhật
status: on-staging
risk: high
size: S
depends_on: []
sprint:
plan: features/loading/plans/T-004-mau-con-thieu-va-gian-chu.md
created: 2026-10-17
---

# T-004: ĐỦ VAI TRÒ MÀU THƯƠNG HIỆU VÀ GIÃN CÁCH CHỮ NHƯ THIẾT KẾ

## Mô tả

Hai chỗ còn lệch sau T-002, thấy ở retro RUN-20261017-01. Một: `tertiaryContainer` và `onTertiaryContainer` còn màu hồng tím mặc định của Material 3 (L-024). Hai: kiểu chữ giữ giãn cách chữ của Material 3 (tới 0,5 sp), còn thiết kế Figma dùng Inter với giãn cách 0, nên chữ nhỏ trên màn hình loading rộng hơn thiết kế. Sau khi xong, mọi vai trò màu đều là màu thương hiệu, và chữ khớp thiết kế hơn.

## Phạm vi

- Gán màu cho `tertiaryContainer`, `onTertiaryContainer` ở hai giao diện sáng và tối.
- Giãn cách chữ 0 cho mọi kiểu chữ.
- `tokens.json` liệt kê đủ mọi vai trò màu của `ColorScheme` (trừ `scrim`), có test giữ điều này.
- Ghi lại ảnh chuẩn screenshot và ảnh so sánh thiết kế của màn hình loading.

**Không bao gồm:**

- Đổi bố cục hay chữ của màn hình nào.

## Rủi ro sơ bộ

Chạm điểm chặn 7 (module core `:core:designsystem`). Đổi giao diện của mọi màn hình có chữ (giãn cách chữ). Rủi ro cao, cần duyệt chéo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
