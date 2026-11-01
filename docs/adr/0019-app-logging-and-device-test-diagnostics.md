---
id: ADR-0019
title: App ghi log hành vi qua :core:logging; CI giữ lại chi tiết lỗi và log khi test trên emulator đỏ
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-15
deciders: [Nhóm phát triển]
---

# ADR-0019: APP GHI LOG HÀNH VI QUA :CORE:LOGGING; CI GIỮ LẠI CHI TIẾT LỖI VÀ LOG KHI TEST TRÊN EMULATOR ĐỎ

> Ghi chú 2026-10-30: chẩn đoán test trên emulator nay ở job `ui-test` của `main-guard.yml`, sau merge, không còn ở `pr-check.yml` ([ADR-0043](0043-test-emulator-sau-merge.md)); các bước và artifact `ui-test-report` giữ nguyên, artifact thêm log của lần chạy đầu khi job chạy lại.

## Bối cảnh

Phiên tự chạy RUN-20261015-01 dừng vì test trên emulator đỏ ba lần. Phiên không đọc được lý do đầy đủ: Gradle cắt mất phần lớn thông báo lỗi, app không ghi gì về việc mình đang làm, và CI không giữ lại log của emulator. Phiên phải thêm lệnh in chẩn đoán vào test rồi chạy lại, tốn thời gian và vẫn không rõ. Một phiên chạy qua đêm cần tự đọc được vì sao test đỏ. Khi app lỗi trên máy người test, nhóm cũng cần biết app đã làm gì trước đó.

## Quyết định

**Ghi log trong app:**

- Module mới `:core:logging` với interface `AppLogger`: bốn mức `debug`, `info`, `warn`, `error`. Feature và core dùng `AppLogger`, không gọi `android.util.Log` trực tiếp. Convention plugin của feature tự thêm module này.
- Mọi mục log vào logcat với thẻ `VGL.<thẻ>`, để lọc được log của app.
- Mục từ mức `info` trở lên được giữ trong Crashlytics và chỉ được gửi đi kèm báo cáo crash (ADR-0012). Chỉ gửi tên loại lỗi, không gửi thông báo lỗi, vì thông báo lỗi có thể chứa đường dẫn hay dữ liệu.
- Ghi: đổi trạng thái chính, kết quả hành động của người dùng, lỗi đã bắt, lần thử lại. Không ghi dữ liệu cá nhân, nội dung file, mã định danh hay chữ người dùng nhập. Rà soát độc lập kiểm tra điều này.
- Unit test dùng `RecordingAppLogger` để kiểm tra ViewModel ghi đúng log khi cần.

**Chẩn đoán test trên emulator (`pr-check.yml`, job `ui-test`):**

- Ghi toàn bộ logcat của emulator trong lúc test.
- Khi job đỏ: bước "Chi tiết test trên emulator" in toàn văn lỗi của từng test (từ file kết quả XML) và log của app (thẻ `VGL.*`, lỗi dừng app) ngay trong log của job.
- Lưu artifact `ui-test-report` trong 7 ngày: báo cáo test, kết quả XML, logcat, log Gradle.
- Phiên tự chạy đọc bước đó trước khi kết luận lỗi thật hay chập chờn, không thêm lệnh in chẩn đoán vào test (docs/auto-run.md, bước 8).

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Logcat và Crashlytics | CI và dev đọc được; crash trên máy người test có kèm diễn biến trước đó | Thêm một module core; log đi kèm báo cáo crash rời khỏi máy |
| Chỉ logcat | Log không bao giờ rời khỏi máy | Crash trên máy người test chỉ có stack trace |
| Gọi thẳng `android.util.Log` | Không thêm module | Không thống nhất thẻ, không kiểm tra được trong unit test, không đưa được vào Crashlytics |

## Hệ quả

- **Tích cực:** khi test emulator đỏ, phiên tự chạy và người đọc được lý do ngay từ log của job; crash trên bản staging hay production có kèm diễn biến trước đó.
- **Phải chấp nhận:** mỗi ViewModel nhận thêm `AppLogger`; khai báo Data safety trên Google Play vẫn là "dữ liệu sự cố" như ADR-0012, vì log chỉ đi kèm báo cáo crash.
- **Việc AI phải tuân thủ:** ghi log theo mục Quyết định, không ghi dữ liệu cá nhân; không gọi `android.util.Log` trong feature; khi test emulator đỏ, đọc bước "Chi tiết test trên emulator" trước khi sửa.
