---
id: FB-20261026-02
title: Thẻ bước che khung xem trước, thu gọn thẻ khó
reporter: Tuấn
source: staging
build: bản staging trên máy thật của Tuấn (chưa ghi số phiên bản)
feature: camera
ticket: T-059
severity: nang
status: triaged
created: 2026-10-26
---

# FB-20261026-02: THẺ BƯỚC CHE KHUNG XEM TRƯỚC, THU GỌN THẺ KHÓ

## Điều đã xảy ra

Thẻ mô tả các bước che khuất màn camera. Thao tác thu nhỏ thẻ không tiện.

## Các bước làm lại

1. Mở camera với một tư thế có thẻ bước.
2. Thử ẩn thẻ bước.

## Mong đợi

Thu gọn và mở lại thẻ bước dễ dàng, để thẻ không che camera khi không cần.

## Ảnh chụp, video

Không có.

## Thiết bị

- Máy: điện thoại của Tuấn (chưa ghi dòng máy)
- Phiên bản Android: chưa ghi
- Ngôn ngữ, chế độ tối: chưa ghi

## Phân loại

- **Loại:** thay đổi yêu cầu. Code đúng spec KB-2 ("Khi vuốt thẻ bước lên thì thẻ ẩn"), nhưng cách duy nhất để ẩn là vuốt lên quá 24 dp (`PoseOverlay.kt:58`, `229-246`), thẻ không có chỗ nào để chạm. Trên màn thấp 360 x 640, thẻ có thể che khoảng 47% khung xem trước (plan T-015).
- **Xử lý:** sửa ở phiên tự chạy kế tiếp (T-059). Tuấn chọn ngày 2026-10-26: không thêm nút thu gọn; chạm vào thẻ (ngoài hai mũi tên) thì thẻ thu gọn thành nút "Bước", chạm nút "Bước" thì thẻ mở lại; giữ cách vuốt lên. Spec camera KB-2 và câu hỏi 2 đổi theo.
- **Lý do:** vuốt không có dấu hiệu nào trên màn hình nên người dùng không biết; chạm là thao tác ai cũng thử.
- **Liên kết:** [T-059](../tickets/T-059-cham-the-buoc.md)
