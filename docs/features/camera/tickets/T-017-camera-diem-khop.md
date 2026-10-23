---
id: T-017
title: Nhận dạng dáng và điểm khớp
feature: camera
spec: features/camera/spec.md
scenarios: [KB-5]
owner: Nhật
status: approved
risk: high
size: L
depends_on: [T-014, T-015]
sprint:
plan: features/camera/plans/T-017-camera-diem-khop.md
created: 2026-10-21
---

# T-017: NHẬN DẠNG DÁNG VÀ ĐIỂM KHỚP

## Mô tả

App nhận dạng người trong khung bằng ML Kit Pose Detection trên máy, chấm điểm khớp với tư thế, hiện lời nhắc chỉnh dáng; đạt ngưỡng 80 thì báo "Đúng dáng rồi, bấm chụp!".

## Phạm vi

- Thành phần mới: `MatchHint`; nét viền và viền nút chụp đổi màu khi đạt.
- Ngưỡng tin cậy để không báo nhầm khi không có người; ngưỡng điểm trong `RemoteSettings`.
- Không chạy khi tư thế không có điểm khớp hoặc nét viền đang ẩn.
- Cờ `feature_pose_matching_enabled`; tham số `matched` của `photo_captured`.
- Đo khung hình mỗi giây và dung lượng APK, ghi vào báo cáo phiên.

**Không bao gồm:**

- Tự chụp khi đạt điểm (ngoài phạm vi).

## Rủi ro sơ bộ

Kỹ thuật mới, khó kiểm chứng tự động (cần người thật, máy tầm thấp). ML Kit do dev thêm trước. Rủi ro cao.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
