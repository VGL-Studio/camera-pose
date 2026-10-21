---
date: 2026-10-21
author: AI, từ bản đề xuất đã duyệt (docs/research/proposal.md)
feature: poses
status: specified
---

# YÊU CẦU: THƯ VIỆN TƯ THẾ VÀ CHI TIẾT TƯ THẾ

## Mình muốn gì

Từ camera, một chạm mở thư viện tư thế: lọc theo nhóm bằng chip, lưới thẻ có ảnh, tên, số bước. Người dùng lưu tư thế yêu thích và xem lại ở chip "Đã lưu". Chạm một thẻ mở chi tiết: ảnh lớn, các bước tạo dáng, lỗi hay gặp, nút "Chụp với tư thế này" đưa về camera với tư thế đó.

## Vì sao

App mẫu có quá ít tư thế hoặc danh mục rỗng (bài học 4), tên tư thế sai hay thô (bài học 9), thiếu tư thế cho nam (bài học 11), tải lỗi thì màn hình trống mãi (bài học 7). Đích ra mắt 200 tư thế, mỗi nhóm hiển thị ít nhất 15 tư thế.

## Người dùng sẽ trải nghiệm thế nào

Ở camera, bấm nút thư viện. Chọn chip "Du lịch", lướt lưới, bấm lưu một tư thế, mở chi tiết một tư thế khác, đọc các bước, bấm "Chụp với tư thế này", camera mở với nét viền tư thế đó.

## Những gì chưa cần làm lần này

Tìm kiếm theo chữ, nhiều bộ sưu tập, chọn nhiều tư thế để chụp lần lượt, bài mẹo chụp ảnh.

## Quảng cáo, dữ liệu, file tĩnh

- Quảng cáo mức B (câu 9.1): `poses_banner` (banner neo đáy thư viện), `poses_detail_banner` (banner đáy chi tiết).
- Sự kiện (câu 9.2): `pose_selected`, `pose_detail_opened`, `pose_saved`, `content_load_failed`.
- File trên R2: catalog tư thế, ảnh nhỏ, ảnh lớn, nét viền (bản đề xuất, mục 11).

## Tham khảo

- Bản đề xuất: mục 4 (bài học 4, 7, 9, 10, 11), mục 5, mục 6 (luồng duyệt), mục 7, mục 11 (cách lưu trên R2), mục 12 (câu 7, 15).
- Thiết kế: trang Screens trên Figma, section Poses và PoseDetail.
