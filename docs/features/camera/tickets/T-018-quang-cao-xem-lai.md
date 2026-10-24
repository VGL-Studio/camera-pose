---
id: T-018
title: Quảng cáo toàn màn hình khi rời màn xem lại
feature: camera
spec: features/camera/spec.md
scenarios: [KB-9]
owner: Nhật
status: on-staging
risk: high
size: S
depends_on: [T-016]
sprint:
plan: features/camera/plans/T-018-quang-cao-xem-lai.md
created: 2026-10-21
---

# T-018: QUẢNG CÁO TOÀN MÀN HÌNH KHI RỜI MÀN XEM LẠI

## Mô tả

Thêm vị trí `review_interstitial`: quảng cáo toàn màn hình khi rời màn xem lại sau khi đã lưu ảnh, theo các giới hạn trong `RemoteSettings`.

## Phạm vi

- Vị trí trong `AdPlacement`, cờ `ads_review_interstitial_enabled` mặc định tắt (ADR-0013, ADR-0014).
- Giới hạn: không trước ảnh lưu thứ 2 của phiên, cách nhau ít nhất 120 giây, tối đa 1 lần mỗi 3 ảnh lưu.
- Không hiện khi hộp thoại mất mạng đang mở.

**Không bao gồm:**

- Banner ở thư viện (T-023).

## Rủi ro sơ bộ

Điểm chặn 1 (vị trí quảng cáo mới): rủi ro cao; người phụ trách sản phẩm biết trước khi duyệt.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
