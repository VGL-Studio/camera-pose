# PHIÊN THỬ TRÊN MÁY: GHI LOG, ĐỂ AI PHÂN TÍCH

Tài liệu này dành cho dev và người kiểm thử. Một phiên thử là một khoảng thời gian dùng thử bản staging trên một máy, trong lúc toàn bộ log của máy được ghi lại. Sau đó AI đọc log cùng ghi chú của người thử để tìm vấn đề và đề xuất cải tiến. Có hai nguồn, cùng một công cụ và một định dạng:

| Nguồn | Ai chạy | Khi nào | Bắt được gì |
| --- | --- | --- | --- |
| Máy thật | Dev hoặc người kiểm thử | Sau khi nhận bản staging, khi muốn thử một feature | Mọi thứ người dùng thật gặp: tốc độ, giật, mạng thật, lỗi riêng của hãng máy, chỗ khó dùng |
| Test emulator viết sẵn (job `ui-test` của `main-guard.yml`) | GitHub Actions, sau merge | Mỗi commit đổi code app trên nhánh chính | Test trong `app/src/androidTest` đỏ, app dừng khi mở (L-051); đỏ thật mở `main-broken` (ADR-0043) |
| Emulator trong CI (`device-session.yml`) | Phiên tự chạy | Cuối phiên, trước khi phát bản staging | Crash, ANR, lỗi rõ ràng trong kịch bản chuẩn và khi bấm ngẫu nhiên |

Hai nguồn bổ sung cho nhau: emulator lọc lỗi rõ ràng trước khi người thử mất thời gian, máy thật tìm phần còn lại. Job `ui-test` của `main-guard` và `device-session.yml` là hai lớp emulator khác nhau: `ui-test` chạy test viết sẵn trên bản staging debug với cấu hình Firebase giả cho từng commit code app; `device-session.yml` cài bản staging release với cấu hình thật, chạy kịch bản chuẩn và bấm ngẫu nhiên cuối phiên (ADR-0043 mục 6).

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

Muốn thử cả app theo từng bước có kết quả mong đợi thì dùng [kịch bản kiểm thử thủ công](manual-test.md).

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

Emulator có camera sau giả (`-camera-back emulated`, T-062): màn camera mở và chụp được, ảnh xem trước là cảnh giả không có người, nên tiêu chí cần nhận dạng dáng (điểm khớp, lời nhắc) vẫn kiểm trên máy thật.

- **Có crash hoặc ANR:** workflow đỏ, phiên không phát bản staging, ghi phát hiện vào báo cáo phiên và soạn sẵn phản hồi mức chặn. ANR chỉ nằm trong màn quảng cáo của SDK (`com.google.android.gms.ads.AdActivity`, quảng cáo thử khi monkey chạm vào) được đếm riêng ở mức `xem`, không chặn: nó lặp lại trên emulator không GPU mà không do code của app (FB-20261025-02). ANR ở activity của app vẫn chặn.
- **Không có:** phiên phát staging, ghi tóm tắt và các phát hiện khác (lỗi đã bắt, lệch kịch bản) vào báo cáo phiên.
- Kịch bản mất mạng tắt chế độ máy bay, Wi-Fi và dữ liệu di động, rồi chờ tới khi máy không còn mạng mặc định mới mở app (L-031). Không tắt được thì kịch bản bị bỏ qua: `scenarios.txt` ghi `network=still_on skipped`, `summary.md` có dòng "Kịch bản mất mạng bị bỏ qua", và không có `offline.png`. Không dùng ảnh hay log của lần đó làm bằng chứng cho trạng thái lỗi.
- **Kịch bản theo plan:** sau kịch bản chuẩn, `scenarios` chạy mọi tệp `feature/*/src/deviceScenarios/*.txt` và `app/src/deviceScenarios/*.txt` qua `scripts/device-scenario.py` (mục 5), mỗi tệp từ trạng thái mới cài. `summary.md` có bảng "Kịch bản theo plan". Một tệp `lỗi` là phát hiện để phân tích, không chặn phát staging; `bỏ qua (môi trường)` (thiếu camera, không tắt được mạng) không phải lỗi của app. Crash và ANR trong lúc chạy kịch bản vẫn chặn như mọi crash.
- Số liệu thời gian và khung hình trên emulator không có GPU chỉ để so giữa các lần chạy, không dùng để chặn.
- Hành vi đã biết của emulator: sau khi xóa dữ liệu, các lần mở trước khi tải cấu hình xong lần đầu (thường là lần đầu, có khi cả lần thứ hai) hết 10 giây tải cấu hình (TS-20261017-01, TS-20261018-01); trên máy thật không lặp lại (TS-20261017-02, FB-20261017-02). Không ghi thành phát hiện mới, trừ khi xảy ra sau khi app đã tải cấu hình xong một lần.

Chạy tay: **Actions > device-session > Run workflow** trên `main`. Mỗi lần khoảng 25 phút CI.

## 5. Kịch bản theo plan

Tiêu chí kiểm thủ công chạy được trên emulator thì viết thành tệp kịch bản (L-085). Tệp nằm ngoài `docs/` để `pr-check` và `main-guard` chạy khi tệp hay `testTag` bị đổi: `feature/<feature>/src/deviceScenarios/<T-NNN>-<tên>.txt`, hoặc `app/src/deviceScenarios/` cho luồng của vỏ app. Gradle không đọc thư mục này.

**Cách chạy.** Cuối phiên tự chạy, `device-session.yml` tự chạy chúng. Trên máy dev:

```text
scripts/device-session.sh start <tên>
scripts/device-session.sh scenarios --yes
scripts/device-session.sh stop
scripts/device-scenario.py check <tệp>...      # không cần máy: cú pháp, testTag và chuỗi có thật
```

Kết quả nằm trong thư mục phiên: `scenarios.txt` (mỗi bước một dòng `plan <tệp> step=<n> <lệnh> ok|FAIL|SKIP|RETRY`, cuối tệp `result=` và `seconds=`), `scenarios/<tên tệp bỏ .txt>/<tên>.png` (lệnh `shot`), và `fail.png`, `fail.xml` của bước lỗi đầu tiên; tệp được chạy lại (app bị hệ thống giết, đoạn Hạn chót) có thêm `fail-1.png`, `fail-1.xml` của lần chạy đầu. `summary.md` in bảng "Kịch bản theo plan": tệp, ticket, tiêu chí, kết quả (`đạt`, `lỗi`, `bỏ qua (môi trường)`, `bỏ qua (hết thời gian)`, `lỗi tệp`), bước lỗi, số giây, số crash và ANR trong tệp.

**Dòng `note`.** Ngay trước dòng `step=` của mỗi bước, trình chạy ghi `plan <tệp> note step=<n> t=<giây từ đầu tệp> <chi tiết>`, để biết bước chạm vào đâu, lúc nào:
- `tap`, `tap_if` (khi có chạm), `scroll` ghi tọa độ và nhãn của nút (`resource-id`, không có thì chữ, rồi `content-desc`, tối đa 40 ký tự);
- `fresh`, `consent decline` và lệnh `device` ghi điều đã xảy ra (mở lại, đã chạm từ chối, ổn định sau bao nhiêu giây);
- các bước khác chỉ có `t=`.

Dòng `note` có `hộp thoại đồng ý hiện muộn`, `mở lại` hay `bảng của hệ thống` (chuyện bất thường) được chép vào `summary.md`, mục "Ghi chú của trình chạy:", ngay sau bảng, mỗi dòng `- <tệp>, bước <n>: <chi tiết>`. Không có thì mục này không hiện. Khi đọc một tệp lỗi, xem các dòng `note` của nó cùng `fail.png`.

**Phần đầu** (ba dòng, trước mọi lệnh): `ticket: T-NNN`, `criterion: <điều tệp chứng minh>`, `timeout: <giây>` (mặc định 180, tối đa 300). Dòng trống và dòng bắt đầu bằng `#` bị bỏ qua.

**Tên tệp** chỉ gồm chữ, số, `.`, `_`, `-`, bắt đầu bằng chữ hay số, đuôi `.txt`. Tên bắt đầu bằng `T-<số>-` thì dòng `ticket:` phải cùng mã. Sai thì là lỗi tệp: `check` trả mã 1, `run` ghi `lỗi tệp` và không gửi lệnh `adb` nào cho tệp đó.

**Bộ chọn phần tử** (đọc từ `uiautomator dump`):

| Từ khóa | Khớp với |
| --- | --- |
| `id=<testTag>` | `resource-id` đúng bằng tag |
| `idp=<tiền tố>` | `resource-id` bắt đầu bằng tiền tố, ví dụ `idp=gallery_card_` |
| `desc=<chuỗi>` | `content-desc` |
| `text=<chuỗi>` | chữ hiển thị |
| `str=<tên chuỗi>` | giá trị trong `res/values/strings.xml` của mọi module, khớp cả `text` lẫn `content-desc` |

Bộ lọc thêm: `in=<testTag>` (trong cây con của phần tử có tag đó), `index=<n>` (phần tử thứ n, đếm từ 1), `selected=true`, `clickable=true`. Nhiều bộ chọn ngăn bằng ` | ` nghĩa là khớp một trong số đó.

`selected=true` khớp khi thuộc tính `selected` hay `checked` là `true` ở:
- chính nút đó, nếu nút chạm được (`clickable="true"`);
- nếu nút không chạm được: chính nó, rồi tối đa 2 nút cha gần nhất, dừng ngay sau nút cha chạm được đầu tiên.

Lý do: chữ của chip Compose nằm trong một nút con, còn `selected` nằm ở nút cha (dump thật của app camera-pose).

**Ngoặc kép.** Giá trị có dấu cách ghi trong ngoặc kép, `wait text="Hands in Pockets" 5`: dấu cách trong ngoặc được giữ, số sau ngoặc đóng là số giây hay số lần. Mọi giá trị `text=` hay `desc=` không có ngoặc trên một dòng kết thúc bằng một số là mơ hồ, kể cả một từ (`wait text=Page 2` là chữ "Page 2" hay chờ 2 giây? `wait desc=Back 10` cũng vậy), nên là lỗi tệp: ghi giá trị trong ngoặc kép, ví dụ `wait desc="Back" 10`. Quy tắc này cũng bắt `count text=A b 2` và `wait text=A b selected=true 5`: ghi `text="A b"`.

**Lệnh:**

| Lệnh | Việc |
| --- | --- |
| `fresh` | Dừng app, xóa dữ liệu, cấp quyền runtime, mở app bằng `am start -W` rồi chờ tiến trình của app (`pidof`, chỉ nhận đầu ra toàn số). Không có tiến trình thì ghi `note`; bộ đệm crash có tên app thì `FAIL app crash ở lần mở đầu`, không thì dừng app và mở lại một lần. Vẫn không có thì `FAIL app không chạy sau 2 lần mở`. Tệp đứng sau một lần trả máy thì chờ `DEVICE_SCENARIO_STOP_SETTLE` giây (mặc định 3) sau khi dừng app rồi mới mở: dừng app khi Activity còn đang dựng lại sau lần trả máy làm hệ thống giết lần mở ngay sau đó (`Destroy timeout of remove-task`). Cũng chờ sau khi dừng app khi tệp đã có lệnh `device` trước đó. Trước khi mở, đặt ngôn ngữ riêng của app là tiếng Anh (`en`), vì `str=` đọc `res/values/` (tiếng Anh); dưới Android 13 máy phải để tiếng Anh, không thì tệp `bỏ qua (môi trường)` (camera-pose T-054). Ngay trước khi mở, đóng bảng của hệ thống (`cmd statusbar collapse`, camera-pose T-070) |
| `consent decline [giây]` | Chạm nút từ chối của hộp thoại đồng ý quảng cáo (ADR-0013) khi nó hiện, rồi chờ hộp thoại đóng; hoặc thấy luôn app mà không có hộp thoại. "Thấy app" là bộ chọn `DEVICE_SCENARIO_FIRST_SCREENS`, mặc định `id=app_root` (tag của `Surface` gốc trong `MainActivity`); app có màn đầu riêng thì đặt bộ chọn theo màn đó. App hỏi đồng ý không đồng bộ nên hộp thoại có thể hiện sau màn đầu vài giây: thấy app rồi, lệnh vẫn tìm hộp thoại thêm `DEVICE_SCENARIO_CONSENT_GRACE` giây (mặc định 8) và từ chối nếu nó hiện. Hộp thoại hiện muộn hơn nữa: xem Hộp thoại đồng ý, dưới |
| `wait <bộ chọn> [giây]` | Chờ tới khi khớp (mặc định 20 giây) |
| `need <bộ chọn> [giây]` | Như `wait`, nhưng hết giờ thì tệp kết thúc `bỏ qua (môi trường)`. Dùng cho điều kiện mà emulator CI có thể không có (camera trên máy không có camera, mạng) |
| `tap <bộ chọn>`, `tap_if <bộ chọn>` | Chạm vào tâm của phần tử; `tap_if` bỏ qua khi không thấy |
| `scroll <bộ chọn> [lần]` | Vuốt lên trong vùng của phần tử (mặc định 3 lần) |
| `expect`, `expect_no`, `count <bộ chọn> <n>` | Kiểm có, không có, hay đúng n phần tử, sau một lần dump |
| `network off\|on` | Tắt hay bật cả Wi-Fi và dữ liệu, rồi chờ (có giới hạn) tới khi máy hết hay có lại mạng mặc định; không được thì `bỏ qua (môi trường)` |
| `device rotate landscape\|portrait`, `device night on\|off` | Xoay màn hình, chế độ tối |
| `device unfold`, `device fold` | Giả lập gập mở bằng đổi kích thước (673 × 841 dp, khung `_unfolded`) rồi trả về như cũ. Trên màn nhỏ, Android cắt kích thước ép ở 3 lần cạnh dài của màn thật (màn 320x640 cắt `1766x2208` thành `1766x1920`), nên `device unfold` dùng kích thước nhỏ hơn với cùng khung dp (`1536x1919` ở 365 dpi). `device unfold` cũng tắt hộp thoại giới thiệu đóng khung của hệ thống ("See and do more"): lần chạm đầu sau khi mở gập chỉ đóng hộp thoại này, và `uiautomator` không thấy nó (camera-pose FB-20261025-01). Trả máy đặt cài đặt giới thiệu về mặc định của máy, kể cả trên máy thật mà dev đã tự đổi nó |
| `device locale <thẻ>` | Ngôn ngữ riêng của app (Android 13 trở lên). Mỗi `fresh` đặt lại `en` |
| `back`, `shot <tên>` | Phím Back; chụp màn hình |

Sau mỗi lệnh `device`, trình chạy đọc màn hình tới khi hai lần dump liên tiếp giống nhau, tối đa `DEVICE_SCENARIO_SETTLE` giây (mặc định 10), vì đổi kích thước hay mật độ làm Activity dựng lại. Không ổn định thì chỉ ghi `note ... không ổn định`, không phải lỗi: bước sau tự chờ phần tử của nó. Bản dump cuối của lệnh `device` và của `consent decline` được dùng lại cho bước ngay sau, chỉ khi bước đó đọc màn hình (`wait`, `need`, `tap`, `tap_if`, `scroll`, `expect`, `expect_no`, `count`); mọi bước khác bỏ bản đó.

**Hộp thoại đồng ý.** Hộp thoại có thể hiện sau màn đầu của app vài giây, muộn hơn cả `DEVICE_SCENARIO_CONSENT_GRACE` của bước `consent decline`.
- `consent decline` thấy màn đầu mà hết `DEVICE_SCENARIO_CONSENT_GRACE` vẫn chưa thấy hộp thoại: bước đọc màn hình sau đó gặp hộp thoại (mà không thấy phần tử nó tìm) thì tự chạm từ chối, chờ hộp thoại đóng, ghi `note ... hộp thoại đồng ý hiện muộn, đã từ chối`. Tối đa một lần mỗi `fresh`.
- Hộp thoại hiện lại sau khi đã từ chối là `FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối`: app có thể chưa lưu lựa chọn (ADR-0013). Một bước `consent decline` thứ hai trong cùng `fresh` thấy hộp thoại sau khi đã từ chối cũng lỗi như vậy, không chạm lần nữa. Lỗi này, và `hộp thoại đồng ý không đóng sau khi chạm từ chối`, vẫn là `lỗi` trong bước `need`, không thành `bỏ qua (môi trường)`.
- Tệp chưa có bước `consent decline` (hoặc vừa `fresh`) thì trình chạy không chạm hộp thoại, nên vẫn viết được kịch bản kiểm chính hộp thoại.
- Trạng thái này không đi qua ranh giới tệp, và `fresh` đặt lại nó.

**Bảng của hệ thống.** Monkey chạy ngay trước các tệp kịch bản và có thể vuốt từ mép trên, để bảng thông báo hay cài đặt nhanh mở đè lên app; khi đó mọi lần dump chỉ có node của `com.android.systemui` (camera-pose FB-20261027-02). Màn hình khóa cũng chỉ có node của `com.android.systemui`: khi đó lệnh đóng bảng không có tác dụng, dòng `note` vẫn ghi, và tệp lỗi như trước.
- `fresh` đóng bảng (`adb shell cmd statusbar collapse`) trước khi mở app.
- Một lần dump chỉ có node của `com.android.systemui`: trình chạy đóng bảng một lần, đọc lại màn hình và ghi `note ... bảng của hệ thống che app, đã đóng (cmd statusbar collapse)` (một lần mỗi bước). Bảng vẫn còn thì bước xử lý màn hình đọc lại như mọi màn hình không có phần tử nó tìm.

Ví dụ ngắn:

```text
ticket: T-NNN
criterion: Chọn "Đã lưu" ở thư viện tư thế thì nút lọc được chọn, xoay màn hình vẫn giữ lựa chọn
timeout: 120
fresh
consent decline 30
wait id=onboarding_screen | id=camera_screen 60
tap_if id=onboarding_next
tap_if id=onboarding_next
tap_if id=onboarding_next
tap_if id=onboarding_enable_camera
tap id=camera_poses
wait id=poses_grid 20
tap str=poses_chip_saved in=poses_screen
device rotate landscape
expect str=poses_chip_saved selected=true
shot landscape
```

**Hạn chót.** Cả lượt có `DEVICE_SCENARIOS_BUDGET` giây (mặc định 1800), tính cả thời gian trả máy về gốc. Mỗi lệnh `adb` có `DEVICE_SCENARIO_ADB_TIMEOUT` giây (mặc định 30). Một lệnh `adb` treo làm bước đó `FAIL adb_timeout` và bỏ phần còn lại của tệp.
- Sau mỗi tệp, kể cả khi trình chạy lỗi hay bị dừng bằng SIGTERM, trình chạy trả máy về như trước lượt chạy: kích thước và mật độ, cài đặt giới thiệu đóng khung (`wm reset-letterbox-style`), xoay màn hình và chế độ tối (giá trị đọc trước tệp đầu), ngôn ngữ riêng của app, và mạng (chờ có lại mạng mặc định) khi tệp có lệnh `network`. Việc này có hạn tổng `DEVICE_SCENARIO_RESET_LIMIT` giây (mặc định 60): mỗi lệnh tối đa 10 giây, bật lại mạng tối đa 45 giây, tất cả trong hạn tổng đó.
- Mỗi tệp có `min(timeout của tệp, thời gian còn lại - DEVICE_SCENARIO_RESET_LIMIT - DEVICE_SCENARIO_EVIDENCE_RESERVE)`. Phần chừa cuối (mặc định 20 giây) dành cho dấu log đầu tệp và `fail.png`. Còn lại không dương thì tệp `bỏ qua (hết thời gian)`.
- Tệp mới làm tổng thời gian của các tệp (cộng 80 giây mỗi tệp cho trả máy và bằng chứng) vượt `DEVICE_SCENARIOS_BUDGET` thì plan của tệp đó tăng ngân sách (mặc định ở `scripts/device-scenario.py` và `scripts/device-session.sh`) cùng lúc. Plan không làm thì phiên tự nâng, có trần, theo ADR-0032/L1.
- `device-session.sh` gọi trình chạy qua `timeout -k 30` với ngân sách cộng 120 giây. Khi nhận SIGTERM, trình chạy giết lệnh `adb` đang chạy (cả nhóm tiến trình), tệp đang chạy ghi `lỗi`, trả máy về gốc trong 25 giây, ghi `plan runner_terminated` và thoát mã 143. Trình chạy hỏng thì `scenarios` ghi dòng `plan runner_error` và vẫn kết thúc bình thường.
- **App bị hệ thống giết theo tiến trình khác** (camera-pose FB-20261025-03, T-054): bước lỗi mà `logcat.txt` có `Killing ...: depends on provider ... in dying proc ...` của app sau dấu của tệp ghi `RETRY`; trình chạy chạy lại tệp một lần (bằng chứng của lần đầu là `fail-1.png`, `fail-1.xml`). Lần chạy lại cũng bị giết, hay không còn thời gian, thì `SKIP` và `bỏ qua (môi trường)`. Thường gặp trên emulator: Google Play services tự cập nhật hay khởi động lại, và Android giết mọi app đang giữ provider phông chữ của nó. Máy chạy kịch bản dưới Android 13 phải để tiếng Anh.

Các biến khác (test đặt nhỏ để chạy nhanh): `DEVICE_SCENARIO_START_WAIT` (hạn của `am start -W`, mặc định 15 giây; hết hạn mà app có tiến trình thì vẫn tính là đã mở), `DEVICE_SCENARIO_PROCESS_WAIT` (chờ tiến trình của app, mặc định 5 giây), `DEVICE_SCENARIO_CONSENT_CLOSE` (chờ hộp thoại đồng ý đóng sau khi chạm, mặc định 5 giây), `DEVICE_SCENARIO_POLL` (giữa hai lần dump, mặc định 1 giây), `DEVICE_SCENARIO_SETTLE` (chờ màn hình ổn định sau lệnh `device`, mặc định 10 giây), `DEVICE_SCENARIO_STOP_SETTLE` (`fresh` sau một lần trả máy chờ sau khi dừng app, mặc định 3 giây), `DEVICE_SCENARIO_ROOT` (gốc repo mà `check` đọc, mặc định repo này).

**Cửa sổ riêng.** `testTag` chỉ thành `resource-id` trong cửa sổ có `testTagsAsResourceId`: vỏ app đặt nó ở gốc giao diện (`MainActivity`) và ở hộp thoại mạng (`NetworkDialogHost`). Hộp thoại hay popup khác (mỗi cái một cửa sổ) mà kịch bản cần tìm theo `id=` thì gắn `Modifier.exposeTestTagsForUiAutomator()` (`app/src/main/kotlin/<gói>/ui/UiAutomatorTags.kt`) ở gốc nội dung của nó, nơi mở chúng.

**Đổi tên `testTag`** thì sửa kịch bản: `scripts/device-scenario.py check` chạy trong `scripts/tests/run.sh` ở mọi PR đổi `feature/**` hay `app/**` và báo tag hay chuỗi không còn. `check` không tìm tag trong tệp `*.kt` nằm dưới một thư mục ngay sau `src/` có tên bắt đầu bằng `test` hay `androidTest`: tag chỉ còn trong test là tag không còn trong app.

## 6. Quy tắc về dữ liệu

- Chỉ thử bản staging. Không ghi phiên trên bản production hay trên máy có dữ liệu thật của người dùng khác.
- App không ghi dữ liệu cá nhân vào log (ADR-0019), nhưng log hệ thống có thể có tên máy, tài khoản, tên mạng Wi-Fi. Vì vậy log thô không vào git; báo cáo chỉ trích dòng làm bằng chứng, đã bỏ các thông tin đó.
- Artifact của CI chỉ có log của emulator, không có dữ liệu thật.
