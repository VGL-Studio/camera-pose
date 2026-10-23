---
id: T-031
title: Gọi thẳng CameraControl.setZoomRatio, bỏ reflection
feature: camera
spec: features/camera/spec.md
scenarios: [KB-4]
owner: Nhật
status: planned
risk: high
size: S
depends_on: [T-013]
sprint:
plan: features/camera/plans/T-031-camerax-listenablefuture.md
created: 2026-10-23
---

# T-031: GỌI THẲNG CAMERACONTROL.SETZOOMRATIO, BỎ REFLECTION

## Mô tả

T-013 gọi `CameraControl.setZoomRatio` qua reflection, vì hàm này trả `ListenableFuture` của Guava mà Guava chỉ có ở runtime, không có trên classpath biên dịch của `:feature:camera`, và plan T-013 không cho thêm thư viện (L-007). Reflection cần một luật keep trong `consumer-rules.pro`; quên luật đó thì zoom chết lặng lẽ ở bản minify. Sau ticket này, camera gọi thẳng hàm của CameraX, có unit test cho phần zoom. Người dùng không thấy gì khác.

## Phạm vi

- Thêm Guava (`com.google.guava:guava`, đúng phiên bản đã có ở runtime) vào classpath biên dịch của `:feature:camera`.
- Gọi thẳng `setZoomRatio`, tách thành một hàm nhỏ có unit test; bỏ reflection và luật keep.

**Không bao gồm:**

- Đổi hành vi zoom (mức zoom, cách chọn ống kính góc rộng).
- Dùng `ListenableFuture` cho phần khác của CameraX.

## Rủi ro sơ bộ

Cao vì thêm một phụ thuộc trực tiếp (điểm chặn 2), dù thư viện đã có sẵn trong APK qua Firebase.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
