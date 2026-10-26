---
id: T-058
title: Camera mở từ "Chụp với tư thế này" chạy và chụp được
feature: camera
spec: features/camera/spec.md
scenarios: [KB-1, KB-5, KB-6]
owner: Nhật
status: planned
risk: high
size: M
depends_on: [T-017, T-022]
sprint:
plan: features/camera/plans/T-058-camera-mo-tu-thu-vien.md
created: 2026-10-26
---

# T-058: CAMERA MỞ TỪ "CHỤP VỚI TƯ THẾ NÀY" CHẠY VÀ CHỤP ĐƯỢC

## Mô tả

Mở camera từ nút "Chụp với tư thế này" ở chi tiết tư thế thì khung xem trước đứng hình và không chụp được (FB-20261026-01, mức chặn). Nguyên nhân AI tìm thấy trong code: màn camera cũ bị hủy sau khi màn camera mới đã gắn camera, và `CameraXController.close()` gọi `provider.unbindAll()`, gỡ luôn camera của màn mới; `poseAnalyzer.release()` và `PoseMatchViewModel.onCleared()` của màn cũ cũng tắt nhận dạng dáng của màn mới. Sau task này, mở camera từ chi tiết tư thế thì camera chạy, nhận dạng dáng chạy và chụp được.

## Phạm vi

- `CameraXController` chỉ gỡ các use case của chính nó, không gọi `unbindAll()`.
- Bộ nhận dạng dáng (`MlKitPoseAnalyzer`) và luồng khung hình chỉ bị tắt bởi màn đang dùng chúng.
- Cân nhắc trong plan: quay về màn camera đang có và giao tư thế cho nó, thay vì tạo màn camera mới.
- Test hồi quy: unit test hai bộ điều khiển dùng chung một nhà cung cấp camera giả; test trên emulator hoặc kịch bản đi từ thư viện, chi tiết, "Chụp với tư thế này", bấm chụp, tới màn xem lại.

**Không bao gồm:** đổi giao diện.

## Rủi ro sơ bộ

Cao: đổi cách gắn và gỡ camera (CameraX) và vòng đời của nhận dạng dáng, chức năng chính của app. Không thêm quyền hay thư viện. Lỗi phụ thuộc thời điểm hủy màn, JVM chỉ mô phỏng được bằng nhà cung cấp giả; cần kiểm trên emulator.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
