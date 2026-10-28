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

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

<!--
Quyền và thư viện phải được quyết ở đây, lúc lập plan: phiên gặp quyền hay thư viện mà plan chưa nêu thì phải dừng task (risk-scoring.md, mục 4, điều kiện 2).
Cách tìm: docs/CLAUDE.md, mục Khi viết plan. scripts/plan-check.py kiểm hai dòng cuối.
-->

- Cờ tính năng: <tên cờ và cách dùng, hoặc "Không có">
- Quảng cáo: <vị trí đã có trong spec, hoặc "Không chạm">
- Sự kiện phân tích: <sự kiện đã có trong spec, hoặc "Không chạm">
- File R2: <hoặc "Không chạm">
- **Quyền truy cập:** <quyền thêm, mỗi quyền: tên như `WRITE_EXTERNAL_STORAGE`, `maxSdkVersion` nếu có, phần spec cho phép (KB-n hoặc mục 6); rồi "không thêm:" và các quyền plan có nhắc tới nhưng không thêm. Hoặc "Không thêm">
- **Thư viện và SDK:** <thư viện mới, mỗi thư viện: tọa độ `group:name:version`, giấy phép, quyền mà nó kéo vào manifest gộp; thư viện đã có thì ghi tên trong `gradle/libs.versions.toml` và module nào thêm phụ thuộc. Hoặc "Không thêm">

### 3.4. Phương án dự phòng

<!--
Cho điểm kỹ thuật chưa chắc chắn (thường là điểm E từ 2), để phiên không phải dừng task khi phương án chính không chạy (risk-scoring.md, mục 4).
Người duyệt duyệt cả phương án này. File của phương án dự phòng phải có trong mục 3.2, cột Nội dung ghi "(dự phòng)".
Ghi "Không có" nếu không có.
-->

- **Điểm chưa chắc:** <phần nào của mục 3.1 có thể không chạy>
- **Điều kiện chuyển:** <dấu hiệu cụ thể, ví dụ test viết trước vẫn thất bại vì cùng một lỗi sau 2 lần sửa, hay một lỗi biết trước như hết thời gian chờ>
- **Cách làm thay:** <hướng kỹ thuật thay thế; tiêu chí chấp nhận và test ở mục 4 giữ nguyên>

### 3.5. Hành vi đã có bị đổi

<!--
Bắt buộc khi plan sửa hay xóa code Kotlin ngoài thư mục test (scripts/plan-check.py, quy tắc 13; ADR-0031).
Bắt đầu từ `scripts/existing-tests.py <plan>`: đọc từng test của nhóm "Cần xem trước" và "Nên xem".
Mỗi dòng một hành vi plan đổi (vị trí, kích thước, thứ tự, giá trị, luồng). Câu khẳng định phải đổi thì viết sẵn câu mới,
để phiên dùng như test viết trước; chưa quyết được thì đặt câu hỏi ở mục 8. Không cần chạy test.
Ghi "Không có" kèm một câu lý do nếu plan không đổi hành vi nào mà test có sẵn kiểm.
-->

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| <ví dụ: thẻ nằm dưới pager> | <thẻ đè ở đáy vùng bước> | `XxxTest.card_belowPager` | <câu mới: khung thẻ ở đáy vùng bước, thẻ không che chữ của bước> |

## 4. Kiểm thử

<!--
Test được viết TRƯỚC code và phải thất bại đúng lý do trước khi viết code (xem auto-run.md).
Mỗi dòng dưới đây phải đủ cụ thể để viết được test ngay từ plan.
Plan sửa lỗi: ghi số đo thật trên code hiện tại (chạy test tạm, không commit) và lý do thất bại đã thấy, không đoán.
Khung test tái hiện được lỗi gốc: cùng chiều rộng, chiều cao như nơi phát hiện (L-029).
Test kiểm giá trị mặc định thường qua trên khung rỗng: ghi sẵn bước đối chứng (ghi rồi đọc lại, dữ liệu đúng cạnh dữ liệu sai) (L-055).
Nút chữ trên nền khác màu: có test đo màu chữ của nút trên ảnh vẽ ra, không chỉ dựa vào ảnh chuẩn (L-053).
Test trên emulator chỉ kiểm tra trạng thái ổn định sau khi chờ có giới hạn, không khẳng định màn hình thoáng qua (CLAUDE.md, bước 10).
-->

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, tiêu chí 1 | Unit test | `XxxViewModelTest.whenXxx_thenYyy` |
| KB-1, tiêu chí 2 | Compose UI test | <...> |
| Giao diện màn hình X | Screenshot test | <...> |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `<Ten>Screen_<trạng thái>_small.png` |
| Đúng thiết kế (khi có `design/screens/`) | Screenshot test cùng tên với từng ảnh thiết kế, ảnh so sánh `scripts/design-compare.sh <feature>`, rà soát độc lập theo mục 7 của mẫu thiết kế. Ticket chỉ làm một phần feature thì ghi một dòng `Ảnh thiết kế của ticket khác (T-NNN): \`<Tên>\`, ...` cho các ảnh ticket khác vẽ; `scripts/plan-check.py` bỏ qua các ảnh đó | `<Ten>Screen_<trạng thái>_<light\|dark>.png` |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | <...> |
| <tiêu chí chạy được trên emulator> | Kiểm tra thủ công (emulator: `<module>/src/deviceScenarios/<T-NNN>-<tên>.txt`) | <tệp kịch bản; `device-session` cuối phiên chạy tệp này> |
| <tiêu chí cần máy thật> | Kiểm tra thủ công (máy thật: <lý do>) | <các bước dev làm khi kiểm tra bản staging> |
| <tiêu chí cần người xem hay đọc> | Kiểm tra thủ công (người: <lý do>) | <việc người làm> |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-085); `scripts/plan-check.py` báo lỗi dòng thiếu khi plan còn `draft` (quy tắc 11).
- `emulator`: tiêu chí chạy được trên emulator thành tệp kịch bản trong `src/deviceScenarios/` của module (`app/` cho luồng của vỏ app). Đưa tệp vào mục 3.2 và chạy `scripts/device-scenario.py check <tệp>` (cú pháp: `docs/ops/test-sessions.md`, mục 5).
- `máy thật`: lý do thuộc một nhóm sau, hoặc "khác" kèm lời giải thích: camera thật; FPS, khung hình; Play (cài từ Play, In-App Review); Remote Config; quảng cáo thật; gập có bản lề; cử chỉ nhiều ngón; TalkBack.
- `người`: việc không phải chạy app, như đọc tài liệu hay xem ảnh chuẩn.

**Ngoại lệ viết test trước:** <phần thay đổi không thể viết test trước, ví dụ chỉ đổi chuỗi văn bản hoặc tài nguyên, kèm lý do. Ghi "Không có" nếu không có. Người duyệt phải đồng ý với ngoại lệ.>

### 4.1. Chạy thử

<!--
Bắt buộc khi tiêu chí E ở mục 5 từ 2 trở lên: kỹ thuật hay thư viện lần đầu dùng trong dự án (L-042).
Chạy thử bằng một test tạm không commit, bằng đúng lệnh phiên dùng (`scripts/check.sh`) trên nhánh có code liên quan, trước khi duyệt.
Robolectric không mô phỏng cửa sổ chồng nhau: phần trợ năng và chạm qua cửa sổ thêm bước kiểm tra thủ công.
Ghi "Không cần (E dưới 2)" nếu không áp dụng. scripts/plan-check.py báo lỗi khi plan draft có E từ 2 mà thiếu mục này.
-->

- **Đã chạy thử:** <kỹ thuật, test tạm, ngày, commit>
- **Lệnh:** `scripts/check.sh` <và lệnh khác nếu có>
- **Kết quả:** <số đo, lỗi gặp, cách xử lý; phương án dự phòng ở mục 3.4 đã thử chưa>

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

<Ngoài các điều kiện dừng chung trong risk-scoring.md, những tình huống riêng của task này khiến AI phải dừng. Ghi "Không có" nếu không có. Không ghi điều kiện dừng chỉ vì một file hay test nằm ngoài mục 3.2: phiên sửa tiếp phần đó theo ADR-0029. Ghi hành vi hay phạm vi cần giữ, ví dụ "bảng khóa sinh tự động đổi thì dừng".>

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
