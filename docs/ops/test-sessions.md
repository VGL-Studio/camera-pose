# PHIÊN THỬ TRÊN MÁY: GHI LOG, ĐỂ AI PHÂN TÍCH

Tài liệu này dành cho dev và người kiểm thử. Một phiên thử là một khoảng thời gian dùng thử bản staging trên một máy, trong lúc toàn bộ log của máy được ghi lại. Sau đó AI đọc log cùng ghi chú của người thử để tìm vấn đề và đề xuất cải tiến. Có hai nguồn, cùng một công cụ và một định dạng:

| Nguồn | Ai chạy | Khi nào | Bắt được gì |
| --- | --- | --- | --- |
| Máy thật | Dev hoặc người kiểm thử | Sau khi nhận bản staging, khi muốn thử một feature | Mọi thứ người dùng thật gặp: tốc độ, giật, mạng thật, lỗi riêng của hãng máy, chỗ khó dùng |
| Emulator trong CI (`device-session.yml`) | Phiên tự chạy | Cuối phiên, trước khi phát bản staging | Crash, ANR, lỗi rõ ràng trong kịch bản chuẩn và khi bấm ngẫu nhiên |

Hai nguồn bổ sung cho nhau: emulator lọc lỗi rõ ràng trước khi người thử mất thời gian, máy thật tìm phần còn lại.

## 1. Chuẩn bị (một lần)

1. Trên máy dev: Android SDK platform-tools (`adb`), có sẵn khi cài Android Studio. Kiểm tra: `adb version`.
2. Trên điện thoại: **Cài đặt > Giới thiệu điện thoại**, chạm 7 lần vào **Số hiệu bản tạo** để bật tùy chọn nhà phát triển; vào **Tùy chọn nhà phát triển**, bật **Gỡ lỗi USB**.
3. Nối máy:
   - Có cáp: cắm cáp, đồng ý cho máy tính gỡ lỗi khi điện thoại hỏi.
   - Không cáp (Android 11 trở lên, cùng mạng Wi-Fi): **Tùy chọn nhà phát triển > Gỡ lỗi không dây > Ghép nối thiết bị bằng mã**, rồi trên máy dev: `adb pair <ip>:<cổng ghép nối>` (nhập mã), `adb connect <ip>:<cổng>`.
4. Kiểm tra: `adb devices` liệt kê đúng một máy ở trạng thái `device`. Nhiều máy thì đặt `export ANDROID_SERIAL=<mã máy>`.
5. Cài bản staging từ Firebase App Distribution (email mời của nhóm `team`).

## 2. Ghi một phiên thử

Trong thư mục repo:

```sh
scripts/device-session.sh start loading-buoi-sang   # tên ngắn, không dấu
# ... thử app, ghi chú vào test-sessions/TS-.../notes.md ...
scripts/device-session.sh stop
```

- `start` xóa log cũ của máy, bắt đầu ghi toàn bộ logcat (mọi bộ đệm, kể cả crash và log hệ thống), tạo thư mục `test-sessions/TS-YYYYMMDD-NN/` với `session.md` (máy, Android, phiên bản app) và `notes.md` để ghi chú.
- Trong lúc thử, ghi vào `notes.md`: định thử gì, đã làm gì lúc mấy giờ, thấy lạ lúc mấy giờ. Giờ giúp AI tìm đúng đoạn log. Không ghi dữ liệu cá nhân. `stop` nhắc nếu file còn trống.
- Muốn thử lần mở đầu như vừa cài: `scripts/device-session.sh reset` (sau `start`). Lệnh xóa dữ liệu của app và ghi dấu `action=clear_data` vào log, để AI biết từ đâu là lần mở đầu. Đừng xóa dữ liệu bằng tay trong Cài đặt: log không ghi lại việc đó.
- Mã phiên `TS-YYYYMMDD-NN` không trùng thư mục nào trên máy, báo cáo nào trong `docs/test-sessions/`, và mã nào được nhắc trong báo cáo phiên tự chạy `docs/runs/*.md` (phiên emulator của CI có khi chỉ được ghi trong báo cáo phiên, không có báo cáo riêng; L-026 của camera-pose). Kéo nhánh chính mới nhất trước khi `start` để script thấy các báo cáo này.
- `stop` dừng ghi, lấy thêm số liệu khung hình (`gfxinfo`), bộ nhớ (`meminfo`), crash và ANR của hệ thống, rồi viết `summary.md` đếm crash, ANR, lỗi của app. Lệnh trả mã 1 khi có crash hay ANR.

Muốn chạy kịch bản chuẩn trên chính máy này (khởi động nguội ba lần, mở lần đầu khi mất mạng, bấm ngẫu nhiên vài phút), giữa `start` và `stop`:

```sh
scripts/device-session.sh scenarios --yes
```

Lệnh này **xóa dữ liệu của app** và bật, tắt chế độ máy bay; chỉ chạy trên máy thử.

**Quyền của app trong kịch bản chuẩn.** Xóa dữ liệu cũng thu hồi các quyền runtime (camera, micro, thông báo...). App hỏi quyền ở màn đầu thì hộp hỏi quyền còn mở sau một lần mở nguội sẽ che lần mở sau: lần đo trả `total_ms=0` và log không có dòng nào của app (L-009 của camera-pose). Vì vậy, sau mỗi lần xóa dữ liệu, kịch bản cấp sẵn mọi quyền runtime mà app khai báo (đọc mục `runtime permissions:` của `adb shell dumpsys package <gói>`, rồi `adb shell pm grant <gói> <quyền>` cho từng quyền) và ghi vào `scenarios.txt` một dòng `permissions granted=CAMERA,...`. Quyền nào máy từ chối cấp thì ghi ở `failed=...`; khi đó các lần mở nguội có thể vẫn bị hộp hỏi quyền che.

Muốn thử chính luồng hỏi quyền (người dùng đồng ý, từ chối, từ chối hẳn) thì chạy `scripts/device-session.sh scenarios --yes --ask-permissions`: kịch bản không cấp quyền, `scenarios.txt` ghi `permissions ask`. Khi đó số đo khởi động nguội sau lần đầu không dùng được. Thử tay luồng này thì dùng `reset` (lệnh này không cấp quyền), rồi ghi lựa chọn của mình vào `notes.md`.

Thư mục `test-sessions/` không vào git: log thô lớn và có thể chứa thông tin của máy. Giữ trên máy dev cho tới khi phân tích xong.

## 3. Nhờ AI phân tích

Trong một phiên Claude Code có người theo dõi, ở thư mục repo:

```text
Phân tích phiên thử test-sessions/TS-20261018-01
```

AI làm theo `docs/CLAUDE.md`, mục Khi phân tích một phiên thử:

1. Đọc `summary.md`, `notes.md`, `app-log.txt`, rồi `logcat.txt` ở các đoạn cần.
2. Dựng lại diễn biến, đối chiếu với kịch bản trong spec của feature đã thử.
3. Viết báo cáo `docs/test-sessions/TS-YYYYMMDD-NN.md` theo [mẫu](../templates/test-session.md): phát hiện có bằng chứng, nguyên nhân có thể trong code, đề xuất cải tiến, log còn thiếu.
4. Mỗi phát hiện và đề xuất thành một phản hồi `FB-...` trạng thái `new` trong `docs/features/<feature>/feedback/`. Từ đó đi tiếp đường thường: chủ feature phân loại, AI viết plan, phiên tự chạy sửa.

Người xem báo cáo trước khi commit, nhất là các trích đoạn log.

## 4. Kịch bản trên emulator trước khi phát staging

Cuối mỗi phiên tự chạy có task được merge, trước `staging.yml`, phiên khởi động `device-session.yml` (`docs/auto-run.md`, mục 4). Workflow build đúng bản staging sẽ phát, cài lên emulator API 34, chạy `start`, `scenarios --yes` (cấp sẵn quyền runtime, mục 2), `stop`, và lưu thư mục phiên thành artifact `device-session` trong 7 ngày.

- **Có crash hoặc ANR:** workflow đỏ, phiên không phát bản staging, ghi phát hiện vào báo cáo phiên và soạn sẵn phản hồi mức chặn.
- **Không có:** phiên phát staging, ghi tóm tắt và các phát hiện khác (lỗi đã bắt, lệch kịch bản) vào báo cáo phiên.
- Kịch bản mất mạng tắt chế độ máy bay, Wi-Fi và dữ liệu di động, rồi chờ tới khi máy không còn mạng mặc định mới mở app (L-031). Không tắt được thì kịch bản bị bỏ qua: `scenarios.txt` ghi `network=still_on skipped`, `summary.md` có dòng "Kịch bản mất mạng bị bỏ qua", và không có `offline.png`. Không dùng ảnh hay log của lần đó làm bằng chứng cho trạng thái lỗi.
- Số liệu thời gian và khung hình trên emulator không có GPU chỉ để so giữa các lần chạy, không dùng để chặn.
- Hành vi đã biết của emulator: sau khi xóa dữ liệu, các lần mở trước khi tải cấu hình xong lần đầu (thường là lần đầu, có khi cả lần thứ hai) hết 10 giây tải cấu hình (TS-20261017-01, TS-20261018-01); trên máy thật không lặp lại (TS-20261017-02, FB-20261017-02). Không ghi thành phát hiện mới, trừ khi xảy ra sau khi app đã tải cấu hình xong một lần.

Chạy tay: **Actions > device-session > Run workflow** trên `main`. Mỗi lần khoảng 25 phút CI.

## 5. Quy tắc về dữ liệu

- Chỉ thử bản staging. Không ghi phiên trên bản production hay trên máy có dữ liệu thật của người dùng khác.
- App không ghi dữ liệu cá nhân vào log (ADR-0019), nhưng log hệ thống có thể có tên máy, tài khoản, tên mạng Wi-Fi. Vì vậy log thô không vào git; báo cáo chỉ trích dòng làm bằng chứng, đã bỏ các thông tin đó.
- Artifact của CI chỉ có log của emulator, không có dữ liệu thật.
