---
id: T-009
title: Module dữ liệu tư thế dùng chung `:core:poses`
feature: poses
spec: features/poses/spec.md
scenarios: []
owner: Nhật
status: approved
risk: high
size: L
depends_on: []
sprint:
plan: features/poses/plans/T-009-core-poses.md
created: 2026-10-21
---

# T-009: MODULE DỮ LIỆU TƯ THẾ DÙNG CHUNG `:core:poses`

## Mô tả

Tạo module core `:core:poses` cho camera và thư viện dùng chung: mô hình tư thế, đọc catalog, tải catalog, ảnh nhỏ, ảnh lớn và nét viền qua `:core:assets`, danh sách đã lưu, danh sách đã dùng gần đây và thứ tự hiển thị.

## Phạm vi

- Mô hình tư thế và catalog theo khuôn ở `docs/research/proposal.md`, mục 11 (nhóm, loại, khung hình, tên, bước, lỗi hay gặp theo ngôn ngữ, `keypoints` tùy chọn).
- Đọc catalog theo đường dẫn trong `RemoteSettings`; tải file qua `RemoteAssets.load`; bộ khởi đầu và tải phần còn thiếu.
- Đã lưu và đã dùng gần đây (DataStore), thứ tự: đã lưu, gần đây, còn lại (spec `poses` câu hỏi 2, spec `camera` câu hỏi 1).
- Ẩn nhóm không có tư thế; chờ tối đa 15 giây cho mỗi lần tải.
- Catalog mẫu nhỏ trên R2 staging để thử (đường dẫn `poses/...`).

**Không bao gồm:**

- Giao diện (T-021, T-022, T-014).
- Nội dung tư thế thật (người làm nội dung).

## Rủi ro sơ bộ

Điểm chặn 7 (module core mới) và 8 (đường dẫn file trên R2): rủi ro cao, cần duyệt chéo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
