---
id: T-056
title: Màn lớn theo hướng của máy, điện thoại khóa dọc
feature: camera
spec: features/camera/spec.md
scenarios: [KB-11]
owner: Nhật
status: planned
risk: high
size: M
depends_on: [T-020, T-024]
sprint:
plan: features/camera/plans/T-056-huong-man-lon.md
created: 2026-10-26
---

# T-056: MÀN LỚN THEO HƯỚNG CỦA MÁY, ĐIỆN THOẠI KHÓA DỌC

## Mô tả

App khóa chiều dọc ở mọi màn (manifest). Trên Android 12L đến 15, màn lớn nằm ngang (màn trong của máy gập, máy tính bảng) vì thế đóng khung app thành một cột dọc giữa hai dải đen, và thư viện không có 3 cột như spec poses KB-7 (FB-20261024-01). Sau task này, điện thoại vẫn khóa dọc; màn có cạnh ngắn từ 600 dp dùng cả màn hình theo hướng của máy, như Android 16 vốn đã làm.

## Phạm vi

- Một hàm của vỏ app chọn hướng theo cạnh ngắn của màn, gọi trong `MainActivity.onCreate`; manifest giữ khóa dọc cho điện thoại.
- Test Robolectric cho cách chọn, test chặn bố cục khi màn lớn nằm ngang (camera, xem lại, thư viện, chi tiết tư thế).
- Kịch bản emulator `app/src/deviceScenarios/T-056-man-lon-xoay-ngang.txt`.
- Đề xuất đổi spec camera (mục 2, KB-11, câu 7, câu 15).

**Không bao gồm:**

- Ảnh chụp đúng phần khung xem trước khi màn lớn nằm ngang (`ViewPort` của CameraX): ticket riêng nếu người phụ trách sản phẩm chọn (spec camera, câu 15).
- Thiết kế riêng cho màn lớn nằm ngang: dùng lại bố cục mở gập.

## Rủi ro sơ bộ

Cao: đổi hướng màn hình của cả app ở vỏ app (`MainActivity`), mọi feature dựa vào. Không thêm quyền hay thư viện. Đóng khung của hệ thống chỉ thấy trên máy (emulator hoặc máy thật), JVM không mô phỏng.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
