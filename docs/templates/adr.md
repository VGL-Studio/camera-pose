---
id: ADR-NNNN
title: <quyết định, viết thành một câu khẳng định>
status: proposed             # proposed | accepted | superseded
origin: app                  # template: ADR của dự án mẫu (mã dưới 1000) | app: ADR riêng của app (mã từ 1001)
date: YYYY-MM-DD
deciders: [<tên>, <tên>]
supersedes:                  # ADR bị thay thế, nếu có
superseded_by:               # điền khi ADR này bị thay thế
---

# ADR-NNNN: <QUYẾT ĐỊNH>

## Bối cảnh

<Vấn đề cần quyết định, các ràng buộc, điều gì xảy ra nếu không quyết định. Viết ngắn, đủ để người đọc sau một năm hiểu vì sao phải chọn.>

## Quyết định

<Nhóm chọn gì. Viết ở thể khẳng định: "Dùng ...", "Không ...".>

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| <đã chọn> | | |
| <phương án khác> | | |

## Hệ quả

- **Tích cực:** <...>
- **Phải chấp nhận:** <...>
- **Việc AI phải tuân thủ:** <quy tắc cụ thể rút ra từ quyết định này, viết thành các vế ngắn nối bằng dấu chấm phẩy; scripts/foundation.py chép từng vế vào docs/foundation.md>
