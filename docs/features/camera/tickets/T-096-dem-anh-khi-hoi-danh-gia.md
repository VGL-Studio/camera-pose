---
id: T-096
title: Hỏi đánh giá app tính cả ảnh vừa lưu từ hộp thoại khi rời màn xem lại
feature: camera
spec: features/camera/spec.md
scenarios: [KB-10]
owner: Nhật
status: backlog
risk: medium
size: S
depends_on: []
sprint:
plan: features/camera/plans/T-096-dem-anh-khi-hoi-danh-gia.md
auto_run:
created: 2026-11-13
---

# T-096: HỎI ĐÁNH GIÁ APP TÍNH CẢ ẢNH VỪA LƯU TỪ HỘP THOẠI KHI RỜI MÀN XEM LẠI

## Mô tả

Người dùng bấm "Tư thế tiếp" (hay Chụp lại, đóng màn) khi chưa lưu rồi chọn lưu trong hộp thoại: app lưu ảnh và rời màn ngay, nhưng phần hỏi đánh giá nhận số ảnh đã lưu chưa tính ảnh vừa lưu (FB-20261113-01, phiên thử TS-20261113-01). Ở lần đủ 3 ảnh, hộp đánh giá của Google Play không được gọi. Sau task này, lần rời màn đó dùng số ảnh đã tính cả ảnh vừa lưu, đúng KB-10, tiêu chí 1.

## Phạm vi

- Màn xem lại (`feature/camera`, `review/ReviewScreen.kt`, `ReviewViewModel`): số ảnh đưa cho phần hỏi đánh giá lấy theo kết quả của lần lưu vừa xong, không theo giá trị màn hình đọc qua `stateIn` và `rememberUpdatedState` (giả thuyết ở FB-20261113-01; plan xác nhận nguyên nhân bằng một test tạm trên code hiện tại và ghi số đo vào mục 4).
- Test: lưu từ hộp thoại khi rời màn ở lần đủ 3 ảnh thì phần hỏi đánh giá nhận 3; các đường rời màn khác (đã lưu bằng nút Lưu rồi rời, có quảng cáo `review_interstitial`, không lưu) giữ hành vi hiện tại.

**Không bao gồm:**

- Đổi luật hỏi đánh giá (3 ảnh, 30 ngày, cờ) hay `core/review`.
- Thanh đáy `review_bottom` của màn xem lại: T-094. Hai ticket cùng sửa `ReviewScreen.kt`; danh sách chạy xếp chúng nối nhau hoặc ở hai phiên.

## Rủi ro sơ bộ

Trung bình: sửa luồng rời màn xem lại (luồng chính sau mỗi lần chụp), cạnh quảng cáo `review_interstitial` và lời hỏi đánh giá. Không chạm module core, không thêm quyền, thư viện hay sự kiện phân tích.

## Định nghĩa hoàn thành

- [ ] KB-10, tiêu chí 1 đạt cả khi lưu từ hộp thoại lúc rời màn.
- [ ] Có test tự động cho đường lưu từ hộp thoại; test có sẵn của `review_interstitial`, luật đếm ảnh và hỏi đánh giá vẫn qua.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật theo các bước của FB-20261113-01: log `Review asked` (hay hộp đánh giá hiện) ở lần đủ 3 ảnh.
