---
id: ADR-0005
title: Không nhắm tới trẻ em
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-14
deciders: [Người phụ trách sản phẩm]
---

# ADR-0005: KHÔNG NHẮM TỚI TRẺ EM

## Bối cảnh

App có đối tượng gồm trẻ em phải tuân thủ chính sách dành cho gia đình của Google Play: có màn hỏi tuổi, chỉ dùng SDK quảng cáo đã tự chứng nhận cho trẻ em, hai luồng quảng cáo. AppLovin không cho phép khởi tạo SDK với người dùng là trẻ em. Độ phức tạp này không tương xứng với giai đoạn đầu.

## Quyết định

- App không nhắm tới trẻ em. Trong Play Console, nhóm tuổi mục tiêu không bao gồm trẻ dưới 13 tuổi.
- Nội dung, biểu tượng, hình ảnh và trang store không được thiết kế để thu hút chủ yếu trẻ em.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Không nhắm trẻ em | Một luồng quảng cáo, dùng được AppLovin, code đơn giản | Bỏ qua phân khúc trẻ em |
| Mọi lứa tuổi kể cả trẻ em | Thị trường rộng hơn | Màn hỏi tuổi, hai luồng quảng cáo, giới hạn mạng quảng cáo |

## Hệ quả

- **Tích cực:** module quảng cáo đơn giản, không cần màn hỏi tuổi.
- **Phải chấp nhận:** nếu một app sau này muốn nhắm trẻ em, phải ra ADR mới và thiết kế lại module quảng cáo.
- **Việc AI phải tuân thủ:** khi sinh spec hoặc nội dung, không định hướng giao diện, hình ảnh, nội dung chữ theo hướng dành cho trẻ em; gặp yêu cầu có dấu hiệu này thì đặt câu hỏi.
