---
id: T-032
title: Log tải file thất bại ghi loại lỗi và lý do
feature: loading
spec:
scenarios: []                # không đổi hành vi của app; sửa log để tìm nguyên nhân (L-027, FB-20261022-04)
owner: Tuấn
status: on-staging
risk: high
size: S
depends_on: []
sprint:
plan: features/loading/plans/T-032-log-ten-loi-tai-file.md
created: 2026-10-23
---

# T-032: LOG TẢI FILE THẤT BẠI GHI LOẠI LỖI VÀ LÝ DO

## Mô tả

Khi tải file tĩnh thất bại, `:core:assets` chỉ ghi `Assets: Could not download <đường dẫn>`. Android bỏ stack trace của `UnknownHostException`, nên log không cho biết là mất mạng, không phân giải được tên miền, hết giờ hay lỗi HTTP (FB-20261022-04, L-027). Sau ticket này, dòng log ghi loại lỗi, chuỗi nguyên nhân, lý do (`NETWORK`, `NOT_FOUND`, `OTHER`) và mã HTTP nếu có. Log đi qua `AppLogger` (ADR-0019) với tag `VGL.Assets`, nên `adb logcat | grep ' VGL\.'` thấy được. Người dùng không thấy gì khác.

## Phạm vi

- Một hàm dựng câu log và một hàm ghi log có unit test trong `:core:assets`.
- `AssetsModule` ghi log qua `AppLogger` thay cho `android.util.Log`.

**Không bao gồm:**

- Kiểm phân giải tên miền trong `env-canary` (L-027, phần còn lại; chạm `.github/`).
- Đổi cách tải file hay cách phân loại lỗi.

## Rủi ro sơ bộ

Cao vì chạm module core dùng chung `:core:assets` (điểm chặn 7), dù thay đổi nhỏ và chỉ ở log.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
