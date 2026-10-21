---
id: T-011
title: Nhận ra mất mạng và hộp thoại mất mạng
feature: network
spec: features/network/spec.md
scenarios: [KB-1, KB-2, KB-3, KB-4]
owner: Nhật
status: backlog
risk: high
size: L
depends_on: [T-007]
sprint:
plan:
created: 2026-10-21
---

# T-011: NHẬN RA MẤT MẠNG VÀ HỘP THOẠI MẤT MẠNG

## Mô tả

Tạo `:core:network` để nhận ra mất mạng (trạng thái mạng của máy và lỗi tải vì mạng), thành phần `NetworkDialog` trong `:core:designsystem`, và vỏ app hiện hộp thoại phủ lên mọi màn sau màn loading.

## Phạm vi

- Theo dõi trạng thái mạng của máy, thời gian chờ 3 giây trong `RemoteSettings`; nhận lỗi tải vì mạng từ `:core:assets` và `:core:poses`.
- `NetworkDialog`: chặn thao tác phía sau, nút "Thử lại" có vòng chờ tối đa 2 giây và dòng "Vẫn chưa có mạng".
- Vỏ app (`:app`) hiện hộp thoại, nút quay lại đưa app ra nền; cờ `feature_network_dialog_enabled`.
- Quyền `ACCESS_NETWORK_STATE` trong manifest.
- Screenshot test `NetworkDialogScreen_default_light`, `_dark`.

**Không bao gồm:**

- Dừng đếm ngược khi mất mạng ở camera (T-013 dùng tín hiệu của module này).
- Đưa về dự án mẫu: không làm (spec `network`, câu hỏi 1).

## Rủi ro sơ bộ

Điểm chặn 3 (quyền mới) và 7 (module core mới): rủi ro cao, cần duyệt chéo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
