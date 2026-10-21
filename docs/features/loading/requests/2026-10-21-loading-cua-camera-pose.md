---
date: 2026-10-21
author: AI, từ bản đề xuất đã duyệt (docs/research/proposal.md)
feature: loading
status: specified
---

# YÊU CẦU: MÀN LOADING CHO CAMERA POSE

## Mình muốn gì

Màn loading của dự án mẫu dùng cho app Camera Pose: Lines and Guides.

- Thay ba file mẫu (`hello.txt`, `content.txt`, `goodbye.txt`) bằng catalog tư thế và bộ khởi đầu 60 tư thế. Bộ khởi đầu tải ở lần mở đầu tiên rồi giữ trên máy; lần sau chỉ tải phần mới (bản đề xuất, câu 12.15).
- Giao diện theo hướng Sticker: logo, tên app, khẩu hiệu, chân trang của app.
- Chuẩn bị xong thì sang màn chào (lần đầu) hoặc camera, thay cho màn hình mẫu.
- Giữ nguyên quy tắc: không có mạng thì báo lỗi, có nút "Thử lại", không vào app (app luôn cần mạng, câu 12.13).

## Vì sao

Tư thế không còn đóng gói trong APK, nên phải có trước khi vào camera. Màn loading là chỗ duy nhất app chờ chuẩn bị và đã có sẵn cách báo lỗi, thử lại.

## Người dùng sẽ trải nghiệm thế nào

Lần mở đầu: màn loading chạy lâu hơn vì tải bộ khởi đầu, rồi vào màn chào. Các lần sau: loading chỉ kiểm tra cấu hình và catalog mới, rồi vào thẳng camera.

## Những gì chưa cần làm lần này

Icon app chính thức (logo trên màn loading còn là logo tạm).

## Quảng cáo, dữ liệu, file tĩnh

- Không quảng cáo.
- Sự kiện `loading_completed` giữ nguyên.
- File trên R2: catalog và bộ khởi đầu (bản đề xuất, mục 11).

## Tham khảo

- Bản đề xuất: mục 6, mục 7 (frame `LoadingScreen_*`), mục 11, mục 12 (câu 13, 15).
- Thiết kế: trang Screens trên Figma, section Loading.
