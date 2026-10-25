---
id: T-049
title: Lệnh chạm sau `device unfold` tới được app; ba điểm còn lại của T-048
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là FB-20261025-01 và các điểm rà soát còn lại của T-048 (báo cáo RUN-20261025-01)
scenarios: []
owner: Tuấn
status: on-staging
risk: high
size: S
depends_on: [T-048]
sprint:
plan: features/quality-gate/plans/T-049-cham-sau-mo-gap.md
created: 2026-10-25
---

# T-049: LỆNH CHẠM SAU `device unfold` TỚI ĐƯỢC APP; BA ĐIỂM CÒN LẠI CỦA T-048

## Mô tả

Ở `device-session` cuối RUN-20261025-01, kịch bản T-024 lỗi ở bước 14. Lệnh chạm đầu tiên sau `device unfold` không tới chip "Đã lưu" (FB-20261025-01). Đã kiểm trên một emulator giống CI (API 34, màn 320x640, 160 dpi): nguyên nhân là hộp thoại giới thiệu chế độ đóng khung "See and do more" của hệ thống. Android hiện hộp thoại này lần đầu một app khóa hướng dọc bị đóng khung trên màn lớn, và lệnh chạm đầu tiên chỉ đóng hộp thoại. `uiautomator dump` chỉ đọc cửa sổ của app nên không thấy nó. Tọa độ chạm thì đúng: giả thuyết lệch tọa độ trong FB bị loại.

Sau ticket này, `device unfold` tắt hộp thoại giới thiệu đó trước khi đổi kích thước, và chọn kích thước nằm trong giới hạn mà Android cho phép trên màn nhỏ. Kịch bản T-024 chạy qua bước 14 thì quy tắc 11 của `plan-check.py` mới đổi thành lỗi được (plan T-048, mục 6).

## Phạm vi

- `device unfold` chạy `wm set-letterbox-style --isEducationEnabled false` trước `wm size`; bước trả máy sau mỗi tệp chạy `wm reset-letterbox-style isEducationEnabled`.
- `device unfold` chọn kích thước và mật độ cho khung 673 x 841 dp nằm trong giới hạn 3 lần cạnh dài của màn thật. Trên emulator 320x640, Android cắt `1766x2208` thành `1766x1920`.
- Ba điểm còn lại của rà soát T-048 (báo cáo RUN-20261025-01, Cần người kiểm tra):
  - câu chữ của quy tắc số mơ hồ (tài liệu, docstring, câu báo lỗi);
  - `consent decline` thấy hộp thoại sau khi đã từ chối trong cùng `fresh` thì lỗi;
  - `fresh` chỉ coi đầu ra toàn số của `pidof` là có tiến trình.

**Không bao gồm:**

- Bỏ khóa hướng dọc trên màn lớn (FB-20261024-01, chủ feature quyết).
- Kiểm thêm tiêu chí còn thiếu của T-020, T-022, T-024 (plan T-048, mục 6).
- Đổi quy tắc 11 của `plan-check.py` thành lỗi: làm sau khi một `device-session` có cả 3 kịch bản không `lỗi`.

## Rủi ro sơ bộ

Chạm điểm chặn 7 (CI và quy trình phiên tự chạy): trình chạy kịch bản chạy cuối mỗi phiên. Không sửa `.github/`, không sửa app.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận ở mục 2 của plan đạt.
- [ ] Có test tự động với `adb` giả cho từng thay đổi của trình chạy.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] `device-session` đầu tiên sau khi merge: T-024 qua bước 14 (ghi vào báo cáo phiên).
