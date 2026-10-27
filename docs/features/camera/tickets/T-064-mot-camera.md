---
id: T-064
title: Máy chỉ có một camera thì nút đổi camera mờ đi, camera mở bằng camera máy có
feature: camera
spec: features/camera/spec.md
scenarios: [KB-4]
owner: Nhật
status: on-staging
risk: medium
size: S
depends_on: [T-013]
sprint:
plan: features/camera/plans/T-064-mot-camera.md
created: 2026-10-27
---

# T-064: MÁY CHỈ CÓ MỘT CAMERA THÌ NÚT ĐỔI CAMERA MỜ ĐI

## Mô tả

Trên máy chỉ có một camera, nút đổi camera vẫn bấm được; bấm vào thì camera báo lỗi, và lỗi còn mãi vì lựa chọn camera đã lưu (FB-20261027-01). Sau task này, máy chỉ có một camera thì nút đổi camera mờ đi, không bấm được, đọc "đã tắt" với TalkBack; camera luôn mở bằng camera máy có, kể cả khi lựa chọn đã lưu là camera kia. Máy có hai camera không đổi gì.

## Phạm vi

- `CameraController` hỏi máy có những camera nào (`ProcessCameraProvider.hasCamera`, phần thuần `lensesOf` có test JVM); `CameraViewModel` chọn camera mở và đặt `canFlip` vào trạng thái `Ready`.
- Nút đổi camera ở bố cục điện thoại và bố cục mở gập mờ đi khi `canFlip` là `false`.
- Lựa chọn đã lưu không bị ghi đè khi app mở camera kia thay.
- Tệp kịch bản `feature/camera/src/deviceScenarios/T-064-mot-camera.txt`: trên emulator CI một camera, bấm nút đổi camera rồi rời màn và quay lại, camera vẫn sẵn sàng. Nên chạy sau T-062 trong cùng phiên để kịch bản chạy được (không phải phụ thuộc: trước T-062 tệp `bỏ qua (môi trường)`).

**Không bao gồm:** thêm camera trước cho emulator CI (plan T-062, câu hỏi 3); máy không có camera nào (màn lỗi như hiện nay).

## Rủi ro sơ bộ

Không chạm điểm chặn: chỉ `:feature:camera`, không quyền, không thư viện mới, không đổi ảnh chuẩn. Spec camera đổi KB-4, chờ Tuấn xác nhận lại trước khi duyệt plan.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Kịch bản `T-064-mot-camera.txt` đạt ở `device-session` cuối phiên.
