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
- **Kịch bản theo plan:** sau kịch bản chuẩn, `scenarios` chạy mọi tệp `feature/*/src/deviceScenarios/*.txt` và `app/src/deviceScenarios/*.txt` qua `scripts/device-scenario.py` (mục 5), mỗi tệp từ trạng thái mới cài. `summary.md` có bảng "Kịch bản theo plan". Một tệp `lỗi` là phát hiện để phân tích, không chặn phát staging; `bỏ qua (môi trường)` (thiếu camera, không tắt được mạng) không phải lỗi của app. Crash và ANR trong lúc chạy kịch bản vẫn chặn như mọi crash.
- Số liệu thời gian và khung hình trên emulator không có GPU chỉ để so giữa các lần chạy, không dùng để chặn.
- Hành vi đã biết của emulator: sau khi xóa dữ liệu, các lần mở trước khi tải cấu hình xong lần đầu (thường là lần đầu, có khi cả lần thứ hai) hết 10 giây tải cấu hình (TS-20261017-01, TS-20261018-01); trên máy thật không lặp lại (TS-20261017-02, FB-20261017-02). Không ghi thành phát hiện mới, trừ khi xảy ra sau khi app đã tải cấu hình xong một lần.

Chạy tay: **Actions > device-session > Run workflow** trên `main`. Mỗi lần khoảng 25 phút CI.

## 5. Kịch bản theo plan

Tiêu chí kiểm thủ công chạy được trên emulator thì viết thành tệp kịch bản (L-062, T-046). Tệp nằm ngoài `docs/` để `pr-check` và `main-guard` chạy khi tệp hay `testTag` bị đổi: `feature/<feature>/src/deviceScenarios/<T-NNN>-<tên>.txt`, hoặc `app/src/deviceScenarios/` cho luồng của vỏ app. Gradle không đọc thư mục này.

**Cách chạy.** Cuối phiên tự chạy, `device-session.yml` tự chạy chúng. Trên máy dev:

```text
scripts/device-session.sh start <tên>
scripts/device-session.sh scenarios --yes
scripts/device-session.sh stop
scripts/device-scenario.py check <tệp>...      # không cần máy: cú pháp, testTag và chuỗi có thật
```

Kết quả nằm trong thư mục phiên: `scenarios.txt` (mỗi bước một dòng `plan <tệp> step=<n> <lệnh> ok|FAIL|SKIP`, cuối tệp `result=` và `seconds=`), `scenarios/<tên tệp bỏ .txt>/<tên>.png` (lệnh `shot`), và `fail.png`, `fail.xml` của bước lỗi đầu tiên. `summary.md` in bảng "Kịch bản theo plan": tệp, ticket, tiêu chí, kết quả (`đạt`, `lỗi`, `bỏ qua (môi trường)`, `bỏ qua (hết thời gian)`, `lỗi tệp`), bước lỗi, số giây, số crash và ANR trong tệp.

**Phần đầu** (ba dòng, trước mọi lệnh): `ticket: T-022`, `criterion: <điều tệp chứng minh>`, `timeout: <giây>` (mặc định 180, tối đa 300). Dòng trống và dòng bắt đầu bằng `#` bị bỏ qua.

**Bộ chọn phần tử** (đọc từ `uiautomator dump`):

| Từ khóa | Khớp với |
| --- | --- |
| `id=<testTag>` | `resource-id` đúng bằng tag |
| `idp=<tiền tố>` | `resource-id` bắt đầu bằng tiền tố, ví dụ `idp=poses_card_` |
| `desc=<chuỗi>` | `content-desc` |
| `text=<chuỗi>` | chữ hiển thị |
| `str=<tên chuỗi>` | giá trị trong `res/values/strings.xml` của mọi module, khớp cả `text` lẫn `content-desc` |

Bộ lọc thêm: `in=<testTag>` (trong cây con của phần tử có tag đó), `index=<n>` (phần tử thứ n, đếm từ 1), `selected=true` (thuộc tính `selected` hoặc `checked`), `clickable=true`. Nhiều bộ chọn ngăn bằng ` | ` nghĩa là khớp một trong số đó.

**Lệnh:**

| Lệnh | Việc |
| --- | --- |
| `fresh` | Xóa dữ liệu, cấp quyền runtime, mở app |
| `consent decline [giây]` | Chạm nút từ chối của hộp thoại đồng ý quảng cáo (ADR-0013) khi nó hiện, hoặc thấy luôn màn hình đầu của app |
| `wait <bộ chọn> [giây]` | Chờ tới khi khớp (mặc định 20 giây) |
| `need <bộ chọn> [giây]` | Như `wait`, nhưng hết giờ thì tệp kết thúc `bỏ qua (môi trường)`. Dùng cho điều kiện mà emulator CI có thể không có (camera) |
| `tap <bộ chọn>`, `tap_if <bộ chọn>` | Chạm vào tâm của phần tử; `tap_if` bỏ qua khi không thấy |
| `scroll <bộ chọn> [lần]` | Vuốt lên trong vùng của phần tử (mặc định 3 lần) |
| `expect`, `expect_no`, `count <bộ chọn> <n>` | Kiểm có, không có, hay đúng n phần tử, sau một lần dump |
| `network off\|on` | Tắt hay bật cả Wi-Fi và dữ liệu; không tắt được thì `bỏ qua (môi trường)` |
| `device rotate landscape\|portrait`, `device night on\|off` | Xoay màn hình, chế độ tối |
| `device unfold`, `device fold` | Giả lập gập mở bằng đổi kích thước (673 × 841 dp, khung `_unfolded`) rồi trả về như cũ |
| `device locale <thẻ>` | Ngôn ngữ riêng của app (Android 13 trở lên) |
| `back`, `shot <tên>` | Phím Back; chụp màn hình |

Ví dụ ngắn:

```text
ticket: T-022
criterion: Chi tiết tư thế mở được từ thư viện
timeout: 120
fresh
consent decline 30
wait id=onboarding_screen | id=camera_screen 60
tap_if id=onboarding_enable_camera
need id=camera_poses 15
tap id=camera_poses
wait id=poses_grid 20
tap idp=poses_card_ in=poses_grid index=1
wait id=pose_detail_screen 15
shot detail
```

**Hạn chót.** Cả lượt có `DEVICE_SCENARIOS_BUDGET` giây (mặc định 600); mỗi tệp có `min(timeout của tệp, thời gian còn lại)`; mỗi lệnh `adb` có `DEVICE_SCENARIO_ADB_TIMEOUT` giây (mặc định 30). Một lệnh `adb` treo làm bước đó `FAIL adb_timeout` và bỏ phần còn lại của tệp. Sau mỗi tệp, trình chạy trả máy về gốc: kích thước và mật độ, xoay màn hình, chế độ tối, mạng, ngôn ngữ riêng của app. Trình chạy hỏng thì `scenarios` ghi dòng `plan runner_error` và vẫn kết thúc bình thường.

**Cửa sổ riêng.** `testTag` chỉ thành `resource-id` trong cửa sổ có `testTagsAsResourceId`: vỏ app đặt nó ở gốc giao diện (`MainActivity`) và ở hộp thoại mạng (`NetworkDialogHost`). Hộp thoại hay popup khác (mỗi cái một cửa sổ) mà kịch bản cần tìm theo `id=` thì gắn `Modifier.exposeTestTagsForUiAutomator()` (`app/.../ui/UiAutomatorTags.kt`) ở gốc nội dung của nó, nơi mở chúng.

**Đổi tên `testTag`** thì sửa kịch bản: `scripts/device-scenario.py check` chạy trong `scripts/tests/run.sh` ở mọi PR đổi `feature/**` hay `app/**` và báo tag hay chuỗi không còn.

## 6. Quy tắc về dữ liệu

- Chỉ thử bản staging. Không ghi phiên trên bản production hay trên máy có dữ liệu thật của người dùng khác.
- App không ghi dữ liệu cá nhân vào log (ADR-0019), nhưng log hệ thống có thể có tên máy, tài khoản, tên mạng Wi-Fi. Vì vậy log thô không vào git; báo cáo chỉ trích dòng làm bằng chứng, đã bỏ các thông tin đó.
- Artifact của CI chỉ có log của emulator, không có dữ liệu thật.
