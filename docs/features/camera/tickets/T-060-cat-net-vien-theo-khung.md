---
id: T-060
title: Nét viền không vẽ ra ngoài khung xem trước
feature: camera
spec: features/camera/spec.md
scenarios: [KB-3]
owner: Nhật
status: accepted
risk: low
size: S
depends_on: [T-015]
sprint:
plan: features/camera/plans/T-060-cat-net-vien-theo-khung.md
created: 2026-10-26
---

# T-060: NÉT VIỀN KHÔNG VẼ RA NGOÀI KHUNG XEM TRƯỚC

## Mô tả

Trên điện thoại, nét viền phóng to hay kéo ra mép thì vẽ tràn ra ngoài khung xem trước, đè lên thanh trên và lộ qua các hàng điều khiển (FB-20261026-03). Bố cục mở gập đã cắt theo khung. Sau task này, phần nét viền ngoài khung không được vẽ ở mọi bố cục; giới hạn cỡ và kéo giữ như spec KB-3.

## Phạm vi

- Chỉ lớp nét viền (cả quầng tối) bị cắt theo khung xem trước, ở mọi bố cục; các thành phần khác trong khung không bị cắt (cột công cụ ở cách 4 của T-015 được tràn khỏi đáy khung).
- Test chặn: ảnh vẽ ra có nét viền ở cỡ 160% và lệch tối đa, điểm ảnh ở thanh trên và hàng nút chụp không mang màu nét viền; screenshot test mới nếu plan thấy cần.

**Không bao gồm:** đổi giới hạn cỡ hay giới hạn kéo.

## Rủi ro sơ bộ

Thấp: một thay đổi bố cục ở camera. Cẩn thận các thành phần đang cố ý vẽ ra ngoài khung (ví dụ bóng đổ, lời nhắc khớp dáng): plan liệt kê và kiểm.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
