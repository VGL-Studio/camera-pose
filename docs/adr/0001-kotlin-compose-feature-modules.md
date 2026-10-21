---
id: ADR-0001
title: Dùng Kotlin, Jetpack Compose và chia module theo feature
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-14
deciders: [Nhóm phát triển]
---

# ADR-0001: DÙNG KOTLIN, JETPACK COMPOSE VÀ CHIA MODULE THEO FEATURE

## Bối cảnh

AI viết phần lớn code trong các phiên tự chạy, hai dev phụ trách hai luồng feature chạy song song và cùng merge vào nhánh chính. Cần một nền công nghệ mà AI làm việc ổn định, và một cấu trúc dự án hạn chế tối đa việc hai luồng chạm vào cùng file.

## Quyết định

- Ngôn ngữ Kotlin, giao diện Jetpack Compose.
- Một Activity, Navigation Compose, ViewModel với StateFlow, luồng dữ liệu một chiều.
- Hilt cho tiêm phụ thuộc, Room và DataStore cho lưu trữ, Coil cho ảnh.
- Chia module: `:app`, các module `:core:*` dùng chung, mỗi feature một module `:feature:<ten>`.
- Feature chỉ phụ thuộc core. Feature không phụ thuộc feature khác. Quy tắc này được kiểm tra tự động trong CI.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Compose, module theo feature | Chuẩn hiện nay, AI có nhiều mẫu tham chiếu, ít xung đột | Cấu hình build ban đầu phức tạp hơn |
| Một module duy nhất | Dựng nhanh | Hai luồng dễ xung đột, khó kiểm soát ranh giới |
| View truyền thống (XML) | Nhiều mã nguồn cũ | Không còn là hướng chính của Android |

## Hệ quả

- **Tích cực:** hai luồng feature ít chạm nhau, ranh giới của plan dễ xác định.
- **Phải chấp nhận:** sửa module core ảnh hưởng mọi feature, nên bị xếp rủi ro cao.
- **Việc AI phải tuân thủ:** code của feature nằm trong module của feature đó; cần dùng chung thì đề xuất đưa vào core, không import chéo giữa các feature.
