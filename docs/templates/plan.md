---
ticket: T-NNN
title: <tên task>
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: low              # low | medium | high
token_estimate:              # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-NNN: <TÊN TASK>

## 1. Tóm tắt

<Hai đến ba câu: làm gì, làm theo cách nào, kết quả kiểm chứng ra sao.>

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/<ten-feature>/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi ... thì ... | <diễn giải ngắn, nêu giả định nếu có> |

Thiết kế tham chiếu: `features/<ten-feature>/design/README.md`, các ảnh trong `design/screens/`: <...>, hoặc "Không có thiết kế riêng" (docs/ops/design-workflow.md)

ADR liên quan: <ADR-NNNN, ...>

## 3. Giải pháp

### 3.1. Cách làm

<!-- Plan phiên bản 2 trở lên của task đã dừng: ghi thêm dòng "Cách tiếp tục: tiếp tục trên nhánh <tên>, PR #<số>" hoặc "Cách tiếp tục: làm lại từ đầu" (auto-run.md, mục Tiếp tục task đã dừng). -->

<Mô tả hướng kỹ thuật: cấu trúc ViewModel, nguồn dữ liệu, luồng trạng thái, điều hướng. Nêu mẫu có sẵn trong dự án mà plan sẽ làm theo.>

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `feature/<ten>/src/main/.../XxxScreen.kt` | <...> |
| Sửa | `feature/<ten>/src/main/.../XxxViewModel.kt` | <...> |
| Tạo | `feature/<ten>/src/test/.../XxxViewModelTest.kt` | <...> |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: <tên cờ và cách dùng, hoặc "Không có">
- Quảng cáo: <vị trí đã có trong spec, hoặc "Không chạm">
- Sự kiện phân tích: <sự kiện đã có trong spec, hoặc "Không chạm">
- File R2: <hoặc "Không chạm">

## 4. Kiểm thử

<!--
Test được viết TRƯỚC code và phải thất bại đúng lý do trước khi viết code (xem auto-run.md).
Mỗi dòng dưới đây phải đủ cụ thể để viết được test ngay từ plan.
Plan sửa lỗi: ghi số đo thật trên code hiện tại (chạy test tạm, không commit) và lý do thất bại đã thấy, không đoán.
Khung test tái hiện được lỗi gốc: cùng chiều rộng, chiều cao như nơi phát hiện (L-029).
Test trên emulator chỉ kiểm tra trạng thái ổn định sau khi chờ có giới hạn, không khẳng định màn hình thoáng qua (CLAUDE.md, bước 10).
-->

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, tiêu chí 1 | Unit test | `XxxViewModelTest.whenXxx_thenYyy` |
| KB-1, tiêu chí 2 | Compose UI test | <...> |
| Giao diện màn hình X | Screenshot test | <...> |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `<Ten>Screen_<trạng thái>_small.png` |
| Đúng thiết kế (khi có `design/screens/`) | Screenshot test cùng tên với từng ảnh thiết kế, ảnh so sánh `scripts/design-compare.sh <feature>`, rà soát độc lập theo mục 7 của mẫu thiết kế | `<Ten>Screen_<trạng thái>_<light\|dark>.png` |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | <...> |
| <tiêu chí không tự động được> | Kiểm tra thủ công | <các bước dev làm khi kiểm tra bản staging> |

**Ngoại lệ viết test trước:** <phần thay đổi không thể viết test trước, ví dụ chỉ đổi chuỗi văn bản hoặc tài nguyên, kèm lý do. Ghi "Không có" nếu không có. Người duyệt phải đồng ý với ngoại lệ.>

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** <liệt kê, hoặc "Không có">

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | | |
| B. Độ rõ của yêu cầu | | |
| C. Khả năng kiểm chứng tự động | | |
| D. Ảnh hưởng chéo | | |
| E. Độ mới của kỹ thuật | | |
| **Tổng** | | **Mức: <Thấp / Trung bình / Cao>** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: <...>
- Task khác trong sprint có thể xung đột: <T-xxx, lý do>
- Đề xuất thứ tự chạy: <chạy trước hoặc sau task nào, vì sao>

## 7. Điều kiện dừng riêng

<Ngoài các điều kiện dừng chung trong risk-scoring.md, những tình huống riêng của task này khiến AI phải dừng. Ghi "Không có" nếu không có.>

## 8. Câu hỏi cho người duyệt

1. <câu hỏi kèm phương án AI đề xuất>

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-YYYYMMDD-NN, phiên bản plan 1

- **Kết quả:** đã merge / dừng / CI thất bại
- **Nhánh, PR:** <...>
- **Đã làm:** <...>
- **Test viết trước:** <số test, xác nhận đã thất bại đúng lý do trước khi viết code>
- **Rà soát độc lập:** <số điểm phát hiện, đã xử lý những gì, điểm nào còn để người xem>
- **Kết quả CI:** <build, lint, unit test, UI test, screenshot test>
- **Lệch so với plan:** <chỗ nào và vì sao, hoặc "Không có">
- **Việc còn lại:** <...>
- **Cần người xem:** <điểm dev nên kiểm tra sáng nay>
