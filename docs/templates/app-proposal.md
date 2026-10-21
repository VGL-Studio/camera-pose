---
title: <tên tạm của app>
date: YYYY-MM-DD
author: AI, theo yêu cầu của <dev>
reference_apps: [<package>, <package>]
status: draft                  # draft | approved. Chỉ dev ghi approved
approved_by:
approved_at:
design_direction:              # tên hướng thiết kế dev đã chọn (mục 10), điền sau bước chọn hướng
---

# ĐỀ XUẤT APP: <TÊN TẠM>

<!--
Bản đề xuất app clone (ADR-0021, docs/ops/clone-study.md). AI viết từ báo cáo khảo sát; dev duyệt trước khi sang Figma.
Mỗi cải tiến dẫn về bài học có bằng chứng. Quảng cáo và dữ liệu thu thập chỉ ghi thành câu hỏi.
File này nằm cạnh các báo cáo (research/ của thư mục làm việc, sau là docs/research/): link tới báo cáo là link tới file bên cạnh,
ví dụ [PoseMate](com.example.posemate.md). Tên ảnh NN-ten là ảnh trong captures/<package>/ ngoài repo.
-->

## 1. Thông tin dev cung cấp

Ý tưởng, thị trường, cách kiếm tiền, giới hạn, danh sách app mẫu.

## 2. Tóm tắt thị trường

Các app mẫu (bảng: tên, lượt cài, điểm, cách kiếm tiền, điểm mạnh nổi bật), nhóm người dùng, quốc gia, xu hướng, khoảng trống. Nguồn: báo cáo khảo sát trong `research/`.

## 3. Tên và tên gói đề nghị

| Tên | Tên gói | Lý do | Kiểm tra trùng trên Play |
| --- | --- | --- | --- |

Dev chốt tên và tên gói trước khi chạy `scripts/new-app.sh`. Tên gói không đổi được sau khi phát hành.

## 4. Bài học từ app mẫu và cải tiến

| Bài học | Bằng chứng | Cải tiến | Cách đo |
| --- | --- | --- | --- |

Bằng chứng là ảnh khảo sát, trích đánh giá có nguồn, hay số đo. Cách đo dùng được làm tiêu chí chấp nhận trong spec sau này.

## 5. Tính năng

| Tính năng | Nhóm | Có ở app mẫu nào | Ghi chú |
| --- | --- | --- | --- |

Nhóm: `bắt buộc` (app nào cũng có), `khác biệt` (lý do người dùng chọn app mình), `sau` (để bản sau), `không làm`. Đề xuất phạm vi bản đầu tiên và tên feature dự kiến (`:feature:<ten>`).

## 6. Luồng sử dụng

Luồng mở đầu, luồng chính, luồng phụ, dạng `A > B > C`, kèm số bước từ lúc mở app tới chức năng chính.

## 7. Danh sách màn hình

| Màn hình | Tên frame | Trạng thái cần dựng | Feature |
| --- | --- | --- | --- |

Tên frame theo ADR-0020: `<Ten>Screen_<trạng thái>_<light|dark>`. Nếu hỗ trợ điện thoại màn hình gập, liệt kê thêm frame khung mở ra 673 x 841, hậu tố `_unfolded`, cho màn hình đổi bố cục khi rộng (`docs/ops/design-workflow.md`, mục 2).

## 8. Điểm nổi bật

Vài điều làm app khác và tốt hơn, viết như câu giới thiệu cho người dùng.

## 9. Quảng cáo và dữ liệu (câu hỏi cho dev)

Vị trí quảng cáo app mẫu dùng và mật độ, đề xuất mật độ cho app mình, sự kiện phân tích, dữ liệu thu thập. Ghi thành câu hỏi kèm phương án AI đề xuất; dev trả lời ở đây.

## 10. Hướng thiết kế

Giữ lại: các khuôn tương tác đã quen với người dùng. Đổi: màu, bộ icon (có giấy phép rõ ràng), phông chữ, hình dạng, giọng văn.

Sau khi dev duyệt các mục trên, AI dựng 2 đến 3 hướng trên Figma (`docs/ops/design-workflow.md`, mục Thiết kế app clone):

| Hướng | Ý chính | Màu | Chữ | Icon | Link Figma |
| --- | --- | --- | --- | --- | --- |

Dev chọn: <hướng, hoặc ghép từ các hướng>, ngày <YYYY-MM-DD>. Ghi tên hướng vào `design_direction`.

## 11. Tài sản mượn tạm dự kiến

Ảnh, icon, âm thanh, nội dung định mượn từ app mẫu để code sớm, và ai sẽ làm bản thay thế (`docs/templates/borrowed-assets.md`).

## 12. Câu hỏi còn mở

Mỗi câu hỏi kèm phương án AI đề xuất.
