---
id: T-016
title: Màn xem lại, lưu, chia sẻ, tư thế tiếp
feature: camera
spec: features/camera/spec.md
scenarios: [KB-6]
owner: Nhật
status: planned
risk: high
size: L
depends_on: [T-013]
sprint:
plan: features/camera/plans/T-016-man-xem-lai.md
created: 2026-10-21
---

# T-016: MÀN XEM LẠI, LƯU, CHIA SẺ, TƯ THẾ TIẾP

## Mô tả

Sau khi chụp mở màn xem lại: lưu vào thư mục ảnh riêng của app qua MediaStore, chia sẻ, chụp lại, tư thế tiếp; rời mà chưa lưu thì hỏi "Bỏ ảnh này?". Ô ảnh vừa chụp ở camera mở ảnh đã lưu gần nhất.

## Phạm vi

- Bố cục theo các ảnh `ReviewScreen_*`; trạng thái đang lưu, lưu lỗi hết bộ nhớ với "Thử lại".
- Không xin quyền đọc ảnh; ảnh không kèm nét viền.
- Sự kiện `photo_saved`; đếm số ảnh đã lưu (cho T-018, T-019).

**Không bao gồm:**

- Quảng cáo khi rời màn (T-018), hỏi đánh giá (T-019), máy gập (T-020).

## Rủi ro sơ bộ

Điểm chặn 4 (sự kiện phân tích): rủi ro cao. Chia sẻ dùng hộp chia sẻ của hệ thống.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
