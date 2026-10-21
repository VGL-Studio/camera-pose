---
feature: loading
status: changed
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-15-man-hinh-loading.md
  - requests/2026-10-21-loading-cua-camera-pose.md
design: features/loading/design/README.md
created: 2026-10-15
confirmed: 2026-10-15
---

# SPEC: MÀN HÌNH LOADING

## 1. Mục tiêu

- **Người dùng được gì:** mỗi lần mở app, người dùng thấy app đang chuẩn bị và tiến độ tới đâu. Khi vào màn hình chính, app đã có cấu hình mới nhất, catalog tư thế và bộ tư thế khởi đầu.
- **Vì sao làm bây giờ:** các app sau này cần chuẩn bị cấu hình và dữ liệu trước khi vào màn hình chính. Dự án mẫu cần một màn hình loading làm mẫu cho việc này. Đây cũng là task của phiên tự chạy thật đầu tiên.
- **Đo kết quả bằng gì:** sự kiện `loading_completed` cho biết bao nhiêu lần mở app chuẩn bị xong và mất bao lâu.

## 2. Phạm vi

**Trong phạm vi:**

- Màn hình loading hiện mỗi lần mở app, có thanh tiến trình ngang và phần trăm từ 0% đến 100%.
- Màn hình có nhận diện của app: logo, tên app, khẩu hiệu, và một dòng chân trang nói rằng ảnh chỉ lưu trên máy của người dùng.
- Ba phần việc chuẩn bị, mỗi phần chiếm một phần thanh tiến trình (câu hỏi 8):
  1. Tải cấu hình mới nhất của app (Firebase Remote Config).
  2. Tải catalog tư thế từ R2, theo đường dẫn trong cấu hình.
  3. Tải bộ tư thế khởi đầu từ R2 (60 tư thế: ảnh nhỏ và nét viền), những file chưa có trên máy.
- Lần mở đầu tiên tải cả bộ khởi đầu nên lâu hơn; các lần sau chỉ tải phần mới. Không có tư thế nào đóng gói trong APK (bản đề xuất, câu 12.15).
- Màn hình hiện ít nhất 2 giây.
- Chuẩn bị xong thì tự chuyển sang màn chào (lần mở đầu tiên, spec `onboarding`) hoặc camera (spec `camera`). Trong spec này, "màn hình chính" là một trong hai màn đó.
- Không chuẩn bị được thì báo lỗi, có nút "Thử lại", không vào màn hình chính.

**Ngoài phạm vi lần này:**

- Màn hình chào của hệ thống Android (biểu tượng app hiện trong lúc app khởi động) giữ nguyên.
- Tải ảnh lớn của tư thế: tải khi mở chi tiết tư thế (spec `poses`).
- Tải ảnh của tư thế ngoài bộ khởi đầu: tải khi cần ở camera và thư viện.
- Loading trong từng màn hình khác, hoạt ảnh riêng cho màn hình loading.

## 3. Kịch bản sử dụng

### KB-1. Mở app, chuẩn bị thành công

Người dùng mở app khi có mạng.

Tiêu chí chấp nhận:

- [ ] Khi mở app thì màn hình loading hiện đầu tiên, có logo, tên app, khẩu hiệu, nhãn "Đang tải dữ liệu", thanh tiến trình ngang, phần trăm, dòng chữ cho biết app đang chuẩn bị, và dòng chân trang, đúng thiết kế.
- [ ] Khi một phần việc tiến triển thì thanh tiến trình tăng theo phần của việc đó (bộ khởi đầu tăng dần theo từng file tải xong), không bao giờ lùi.
- [ ] Khi cả ba phần việc xong và màn hình đã hiện đủ 2 giây thì app tự chuyển sang màn hình chính, không cần bấm gì.
- [ ] Khi cả ba phần việc xong trước 2 giây thì thanh tiến trình đứng ở 100% cho tới đủ 2 giây rồi mới chuyển.
- [ ] Khi chuyển sang màn hình chính thì app ghi sự kiện `loading_completed` một lần.
- [ ] Khi đã vào màn hình chính và người dùng bấm nút quay lại thì app đóng, không quay lại màn hình loading.

Trường hợp đặc biệt:

- [ ] Khi người dùng xoay máy trong lúc loading thì tiến trình không bắt đầu lại từ đầu và thời gian 2 giây không tính lại.

### KB-2. Mở app lần đầu khi không có mạng

Người dùng cài app và mở lần đầu khi không có mạng.

Tiêu chí chấp nhận:

- [ ] Khi một phần việc không xong được thì màn hình loading báo lỗi gồm tiêu đề "Không thể tải dữ liệu" và lời hướng dẫn dễ hiểu; nhãn của thanh tiến trình đổi thành "Tiến độ đã dừng", thanh tiến trình và phần trăm chuyển màu báo lỗi, giữ ở mức đã đạt; có nút "Thử lại". App không vào màn hình chính.
- [ ] Khi bấm "Thử lại" thì app chỉ làm lại những phần việc chưa xong, thanh tiến trình tiếp tục từ chỗ đã đạt.
- [ ] Khi có mạng trở lại và bấm "Thử lại" thì app chuẩn bị xong và vào màn hình chính.
- [ ] Khi tải cấu hình không có phản hồi sau 10 giây, hoặc tải một file không có phản hồi sau 15 giây, thì app coi phần việc đó là không xong được và báo lỗi như trên.

### KB-3. Mở lại app khi không có mạng

Người dùng đã từng mở app thành công, nay mở lại khi không có mạng.

Tiêu chí chấp nhận:

- [ ] Khi catalog và bộ khởi đầu đã có trên máy từ lần trước thì hai phần việc đó xong ngay, không cần mạng.
- [ ] Khi không tải được cấu hình thì app báo lỗi và có nút "Thử lại" như KB-2, không vào màn hình chính, kể cả khi cấu hình đã từng tải thành công ở lần trước.

### KB-4. Tắt màn hình loading

Người phụ trách tắt màn hình loading từ xa khi có sự cố.

Tiêu chí chấp nhận:

- [ ] Khi cờ `feature_loading_enabled` tắt thì app mở thẳng vào màn hình chính như hiện nay, không ghi `loading_completed` (câu hỏi 9).

## 4. Giao diện

- **Thiết kế:** [design/README.md](design/README.md), dựng trên Figma, ba trạng thái: đang tải (sáng, tối) và lỗi (sáng). Thiết kế đi kèm màu thương hiệu mới (xanh lục), phông Inter, logo, nên hệ thống thiết kế của dự án mẫu đổi theo trước (T-002), rồi màn hình loading làm theo thiết kế (T-003).
- **Thiết kế của Camera Pose (2026-10-21):** dựng lại theo hướng Sticker trên Figma của app, section Loading của trang Screens: `LoadingScreen_progress_light`, `LoadingScreen_progress_dark`, `LoadingScreen_failed_light`, `LoadingScreen_failed_dark`. Gói bàn giao trong `design/` sẽ thay bằng thiết kế này sau khi spec được xác nhận. Hệ thống thiết kế đổi theo hướng Sticker trước (phông Bricolage Grotesque, màu mới), là thay đổi `:core:designsystem` (ADR-0008).
- **Chữ của Camera Pose:** tên app "Camera Pose: Lines and Guides"; khẩu hiệu "Tạo dáng theo nét viền, chụp đẹp ngay."; dòng hướng dẫn khi đang tải "Đang chuẩn bị bộ tư thế…"; chân trang "Ảnh chỉ lưu trên máy của bạn"; các chữ khác giữ như mục 4 của thiết kế cũ. Logo là logo tạm cho tới khi có icon app.
- **Các màn hình:**

| Màn hình | Mục đích | Kịch bản liên quan |
| --- | --- | --- |
| Loading | Cho người dùng thấy app đang chuẩn bị và tiến độ | KB-1, KB-2, KB-3 |
| Màn chào hoặc camera | Màn hình chính sau khi chuẩn bị xong | KB-1, KB-4 |

## 5. Quảng cáo

Không có. Màn hình loading không hiện quảng cáo.

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:**

| Dữ liệu | Mục đích | Giữ trong bao lâu |
| --- | --- | --- |
| Catalog và bộ khởi đầu tải từ R2 | Lần mở sau không phải tải lại; camera và thư viện hiện ngay | Đến khi người dùng xóa dữ liệu app |

**Sự kiện phân tích:**

| Tên sự kiện | Khi nào ghi | Tham số | Dùng để trả lời câu hỏi gì |
| --- | --- | --- | --- |
| `loading_completed` | Khi màn hình loading chuyển sang màn hình chính | `duration_ms`: thời gian từ lúc hiện màn hình loading tới lúc chuyển, tính cả 2 giây tối thiểu; `retry_count`: số lần bấm "Thử lại" | Bao nhiêu lần mở app chuẩn bị xong, mất bao lâu, có phải thử lại không |

## 7. File tĩnh trên R2

| Nhóm file | Dung lượng ước tính | Khi nào tải | Khi không tải được |
| --- | --- | --- | --- |
| Catalog tư thế (`poses/catalog/v<n>/catalog-<sha8>.json`) | 100 đến 300 KB | Mỗi lần đường dẫn trong cấu hình đổi; lần sau dùng bản trên máy | Báo lỗi, cho thử lại (KB-2) |
| Bộ khởi đầu: ảnh nhỏ và nét viền của 60 tư thế (`poses/img/<poseId>/<sha8>/...`) | 3 đến 8 MB | Lần mở đầu tiên; lần sau chỉ tải tư thế mới của bộ | Báo lỗi, cho thử lại (KB-2); thử lại chỉ tải các file còn thiếu |

## 8. Cờ tính năng

| Tên cờ | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `feature_loading_enabled` | tắt | Bật để hiện màn hình loading khi mở app. Bản staging bật sẵn |

## 9. Câu hỏi còn mở

1. **Mở lại app khi không có mạng, mà cấu hình đã từng tải thành công, có cho vào màn hình chính không?**
   - Đề xuất của AI: có.
   - Trả lời: không cho vào màn hình chính (KB-3).
2. **Tham số của `loading_completed`:**
   - Đề xuất của AI: `duration_ms` và `retry_count`.
   - Trả lời: `duration_ms` và `retry_count`.
3. **Thời gian chờ tối đa cho mỗi phần việc:**
   - Đề xuất của AI: 10 giây cho tải cấu hình; ba file dùng mức 15 giây sẵn có của việc tải file.
   - Trả lời: đồng ý.
4. **Bản build trên máy dev và trong CI dùng cấu hình Firebase giả, không bao giờ tải được cấu hình:**
   - Đề xuất của AI: với cấu hình giả, phần việc tải cấu hình được tính là xong ngay. Bản staging phát cho nhóm và bản production luôn tải thật.
   - Trả lời: đồng ý.
5. **Hai file `content.txt` và `goodbye.txt` chưa có trên R2 staging:**
   - Trả lời: đã đưa lên. AI kiểm tra lại ngày 2026-10-15: cả ba file trong `test/v1/` đều tải được.
6. **Tên app trên màn hình loading:** thiết kế ghi "Todo App".
   - Đề xuất của AI: màn hình hiện đúng tên của app đang chạy (với dự án mẫu là "App Template"); mỗi app tạo từ dự án mẫu tự có tên của mình. "Todo App" chỉ là chữ mẫu trên thiết kế, screenshot test dùng chữ này để so với ảnh thiết kế.
   - Trả lời: Tên app là "App Template"
7. **Khẩu hiệu "Mọi việc vào đúng nhịp." và logo:** thuộc thương hiệu, mỗi app thay bằng của mình.
   - Đề xuất của AI: dự án mẫu dùng khẩu hiệu và logo này làm mặc định, đặt trong hệ thống thiết kế cùng chỗ với màu thương hiệu; hướng dẫn tạo app mới ghi rõ phải thay.
   - Trả lời: OK, theo đề xuất của AI.

8. **Chia thanh tiến trình cho ba phần việc thế nào?** Bộ khởi đầu lớn hơn hẳn hai phần còn lại.
   - Đề xuất của AI: cấu hình 10%, catalog 10%, bộ khởi đầu 80% tăng dần theo từng file tải xong. Các lần mở sau bộ khởi đầu đã có nên thanh chạy nhanh.
   - Trả lời:
9. **Có giữ cờ `feature_loading_enabled` không?** App luôn cần mạng (bản đề xuất, câu 12.13) và tư thế không còn đóng gói, nên tắt màn loading thì app vào camera mà không có cấu hình mới, không có catalog.
   - Đề xuất của AI: giữ cờ để tắt khẩn cấp, nhưng bật sẵn ở cả staging và production; khi cờ tắt, camera và thư viện tự tải catalog và báo lỗi theo spec của mình.
   - Trả lời:
10. **Tên app trên màn loading của Camera Pose:** câu 6 trả lời "App Template" cho dự án mẫu.
   - Đề xuất của AI: hiện "Camera Pose: Lines and Guides" (`app_name` của app này); tên dài nên xuống hai dòng, căn giữa, như thiết kế.
   - Trả lời:

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-15 | AI | Tạo bản đầu từ yêu cầu |
| 2026-10-15 | AI | Cập nhật theo câu trả lời của anh Nhật: đo Remote Config và ba file R2, thanh ngang có phần trăm, báo lỗi và thử lại khi mất mạng lần đầu, hiện mỗi lần mở app, tối thiểu 2 giây, sự kiện `loading_completed`, tên feature `loading` |
| 2026-10-15 | AI | Cập nhật theo câu trả lời mục 9: mở lại khi không có mạng cũng báo lỗi, tham số của `loading_completed`, thời gian chờ, cấu hình giả trên máy dev và CI, ba file đã có trên R2 |
| 2026-10-16 | AI | Nhận thiết kế Figma theo quyết định của anh Nhật: thêm logo, tên app, khẩu hiệu, chân trang, tiêu đề lỗi, nhãn tiến trình (phạm vi, KB-1, KB-2, mục 4); thêm câu hỏi 6, 7. Chuyển sang `changed`, chờ xác nhận lại |
| 2026-10-21 | AI | Áp dụng cho Camera Pose: thay ba file mẫu bằng catalog và bộ tư thế khởi đầu (phạm vi, KB-1, KB-3, mục 6, mục 7); chuyển sang màn chào hoặc camera; nhận diện và thiết kế theo hướng Sticker (mục 4); thêm câu hỏi 8 đến 10. Chuyển sang `changed`, chờ xác nhận lại |
