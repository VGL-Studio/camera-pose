---
id: T-026
title: Không ghi camera_permission_result khi quyền camera đã bị từ chối vĩnh viễn
feature: camera
spec: features/camera/spec.md
scenarios: [KB-7]
owner: Nhật
status: backlog
risk: low
size: S
depends_on: [T-013]
sprint:
plan:
created: 2026-10-22
---

# T-026: KHÔNG GHI CAMERA_PERMISSION_RESULT KHI QUYỀN CAMERA ĐÃ BỊ TỪ CHỐI VĨNH VIỄN

## Mô tả

Hiện camera gọi hộp hỏi quyền mỗi lần mở khi chưa có quyền. Khi quyền đã bị từ chối vĩnh viễn, máy không hiện hộp hỏi mà trả "từ chối" ngay, và app vẫn ghi `camera_permission_result: denied`, làm tỷ lệ cho phép thấp đi (báo cáo RUN-20261022-01, Cần người kiểm tra). Sau ticket này, camera vào thẳng trạng thái chưa có quyền và không ghi sự kiện trong trường hợp đó.

## Phạm vi

- Phân biệt "hộp hỏi đã hiện và người dùng chọn" với "máy trả từ chối mà không hiện hộp hỏi" (`CameraScreen.kt`, `CameraViewModel.kt`).
- Không ghi `camera_permission_result` ở trường hợp thứ hai; vẫn vào `PermissionDenied`, nút "Mở cài đặt" như cũ.
- Unit test cho hai trường hợp, test giao diện cho luồng hỏi quyền.

**Không bao gồm:**

- Sự kiện quyền ở màn chào (T-012, `source` `onboarding`): plan của T-012 dùng cùng quy tắc.

## Rủi ro sơ bộ

Thấp: chỉ đổi điều kiện ghi một sự kiện đã có trong spec, không thêm sự kiện hay quyền mới. Cách nhận ra "không hiện hộp hỏi" phụ thuộc hành vi Android (`shouldShowRequestPermissionRationale`), cần test trên máy thật.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
