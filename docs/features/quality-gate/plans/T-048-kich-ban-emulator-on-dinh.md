---
ticket: T-048
title: Trình chạy kịch bản emulator chạy ổn định trên CI (sửa sau lần device-session đầu tiên)
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 9M           # cỡ M (5.2M, scripts/token-usage.py estimate) cộng khoảng 70% vì nhiều test với adb giả (T-046: 52 expect) và phần SIGTERM
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-24
---

# PLAN T-048: TRÌNH CHẠY KỊCH BẢN EMULATOR CHẠY ỔN ĐỊNH TRÊN CI

## 1. Tóm tắt

Lần `device-session` đầu tiên có trình chạy kịch bản của T-046 lỗi ở cả 3 tệp. Đã đọc artifact của hai lần chạy (40116129967 và 40117751068 của RUN-20261024-02): ảnh, dump, `scenarios.txt`, log của app và logcat hệ thống. Kết luận: 4 nguyên nhân, 3 nằm ở trình chạy, 1 nằm ở kịch bản. Không nguyên nhân nào là lỗi của app. Riêng chuyện màn mở gập bị đóng khung thì ghi thành phản hồi FB-20261024-01.

Plan sửa trong `scripts/device-scenario.py`:
- `fresh` mở lại app khi lần mở đầu không có tiến trình;
- từ chối hộp thoại đồng ý hiện muộn;
- `selected=true` đọc nút cha gần;
- chờ màn hình ổn định sau lệnh `device`;
- ghi dấu vết từng bước (dòng `note`, phần đáng xem lên `summary.md`);
- sửa 5 điểm "nên sửa" của rà soát T-046.

Plan cũng nâng ngân sách mặc định lên 900 giây. Kịch bản T-022 mở chi tiết trước khi tắt mạng. `device-session.sh` đổi ba chỗ: `timeout -k 30`, ngân sách mặc định, ghi chú của trình chạy trong `summary.md`.

Kiểm chứng:
- test với `adb` giả, gồm bản tái hiện từng lỗi đã thấy (cấu trúc dump thật của chip);
- chạy thử SIGTERM bằng `scripts/check.sh`;
- lần `device-session` đầu tiên sau khi merge.

Không sửa `.github/`, không sửa app.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ. Yêu cầu là kết quả `device-session` của RUN-20261024-02 (báo cáo, mục Kiểm thử trên máy ảo) và các điểm "nên sửa" của rà soát độc lập T-046 (báo cáo, Cần người kiểm tra).

| Lỗi đã thấy hay điểm rà soát | Tiêu chí chấp nhận | Cách hiểu của AI (bằng chứng) |
| --- | --- | --- |
| T-024, lần 2, bước 2: `consent decline 30 FAIL không thấy hộp thoại đồng ý hay màn hình đầu của app` | `fresh` mở app bằng `am start -W` có hạn riêng 15 giây. Sau đó không có tiến trình app (hay `am start -W` hết hạn) thì: ghi lý do vào dòng `note`; `FAIL app crash ở lần mở đầu` nếu bộ đệm crash có gói app; không thì dừng app và mở lại một lần. Vẫn không có tiến trình thì `FAIL app không chạy sau 2 lần mở` | Logcat: tiến trình cũ 9674 đã chết lúc 18:08:57.111 (`clear data`), lệnh `START` lúc 57.927. Sau đó ActivityManager hủy hai lần khởi động mới: `start not valid, killing pid=10782, seq=102,expected=101`, rồi `pid=10785`. Đây là một cuộc đua số thứ tự khởi động của ActivityManager, xảy ra ngay sau lần dừng do xóa dữ liệu và sau `AIRPLANE_OFF` của bước trả máy cho tệp trước (18:08:57.437). Tiến trình cũ đã chết trước khi mở, nên chờ `pidof` rỗng không giúp gì; mở lại mới là cách sửa. Lúc 18:09:14 hệ thống ghi `Skip zero pid ANR`: app không có tiến trình, nên `am start -W` có thể treo |
| T-020, lần 2, bước 3: `wait id=onboarding_screen \| id=camera_screen 60 FAIL` | Bước `consent decline` thấy màn đầu mà chưa thấy hộp thoại thì bật cờ "chờ hộp thoại muộn". Khi cờ bật, bước đọc màn hình sau đó gặp hộp thoại thì tự từ chối, tối đa một lần mỗi `fresh`, và ghi `note`. Bước `consent decline` đã chạm "Do not consent" mà hộp thoại hiện lại thì `FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối` | `fail.xml` chỉ có cửa sổ WebView của hộp thoại (`fc-focus-trap-pre-div`), `fail.png` là hộp thoại UMP phủ lên màn chào. Lần 1 của T-022 cho số đo: `Onboarding shown` lúc 18:05:44.330, UMP nhận lệnh đầu tiên lúc 18:05:46.012, tức hộp thoại hiện khoảng 1,7 giây sau màn chào. Nếu hộp thoại hiện lại sau khi đã từ chối thì app có thể chưa lưu lựa chọn đồng ý (ADR-0013); trường hợp này phải lỗi, không được che đi |
| T-024, lần 1, bước 14: `wait str=poses_chip_saved selected=true 5 FAIL` | `selected=true` (và `checked`) khớp theo quy tắc ở mục 3.1: chính nút đó, hoặc với nút không chạm được thì tối đa 2 nút cha gần nhất, dừng ở nút cha chạm được đầu tiên | Dump thật: nút chữ "Tất cả" `selected="false"`; nút cha trực tiếp `View` `[288,321][475,447]` có `selected="true"`, `clickable="false"`. Chip "Đã lưu": nút cha `clickable="true"`, `selected="false"`. Chạy thử trên code hiện tại (mục 4.1): `str=poses_chip_all selected=true` không khớp, dù chip đó đang chọn |
| T-024, lần 1: sau `device unfold`, chạm chip "Đã lưu" mà chip không đổi | Sau `device unfold`, `fold`, `rotate`, `night`, `locale`, trình chạy chờ tới khi hai lần dump liên tiếp giống nhau, tối đa `DEVICE_SCENARIO_SETTLE` (mặc định 10 giây). Hết giờ không phải lỗi, chỉ ghi `note ... không ổn định` | Đổi `wm size`, `wm density` làm Activity dựng lại, nên bản dump đọc ngay sau đó có thể là bố cục cũ và tọa độ chạm sai. Đây là suy luận: log không có tọa độ và thời điểm chạm, nên mới thêm dòng `note` |
| Không biết bước chạm vào đâu, lúc nào | Mỗi bước ghi thêm một dòng `plan <tệp> note step=<n> t=<giây> <chi tiết>`. Các ghi chú về hộp thoại đồng ý tự từ chối và về `fresh` mở lại hiện trong `summary.md` | Dạng dòng `step=` có sẵn giữ nguyên, để `cmd_summary` và test có sẵn không đổi |
| T-022, cả hai lần, bước 40: `wait id=pose_detail_image 30 FAIL` | Kịch bản mở chi tiết (`wait id=pose_detail_screen`) trước `network off`. Hộp thoại mạng hiện trên chi tiết; có mạng lại thì "Thử lại" đóng hộp thoại và ảnh chi tiết hiện. Ảnh lớn có thể đã tải xong trước khi mất mạng; `criterion` và chú thích của tệp ghi đúng điều đó (Tuấn, 2026-10-24, mục 8 câu 1) | App không ghi `VGL.Poses: Pose detail opened` sau bước 35. `fail.xml` là `poses_screen`; `offline.png` là hộp thoại mạng phủ lên thư viện. App ghi `VGL.Network: Dialog shown: device_offline delay=3s` lúc 18:08:18: hộp thoại hiện trên mọi màn 3 giây sau khi mất mạng (T-011). Suy luận (log không có thời điểm chạm): bước chạm rơi vào lớp phủ của hộp thoại. Kịch bản chưa từng kiểm chi tiết đã mở |
| Rà soát T-046 (2): `check` quét cả `src/test` | `id=`, `idp=`, `in=` không tìm trong `*.kt` nằm dưới một thư mục ngay sau `src/` có tên bắt đầu bằng `test` hay `androidTest`. Các tệp `*.kt` khác vẫn được tìm. Tag chỉ còn trong test thì `check` báo lỗi | `source_files` hiện trả cả `app/src/test/.../MainScreenTest.kt` (mục 4.1) |
| Rà soát T-046 (3): ngân sách không tính thời gian trả máy về gốc | Trả máy về gốc có hạn tổng `DEVICE_SCENARIO_RESET_LIMIT` (mặc định 30 giây), cộng 45 giây để bật lại mạng khi tệp có lệnh `network`. Hạn của mỗi tệp chừa phần đó. Bước đọc `wm size`/`wm density` đầu lượt có hạn 5 giây mỗi lệnh và tính vào ngân sách. Ngân sách mặc định là 900 giây (tính toán ở mục 3.1) | Hiện mỗi lệnh trả máy có 10 giây riêng (6 lệnh) cộng 45 giây cho mạng, nằm ngoài ngân sách, tức tới 105 giây mỗi tệp. Ba tệp hiện có `timeout` 150 + 240 + 180 = 570 trên ngân sách 600 |
| Rà soát T-046 (4): `timeout` chỉ gửi SIGTERM | `device-session.sh` gọi `timeout -k 30 ...`. Trình chạy bắt SIGTERM: giết nhóm tiến trình `adb` đang chạy, chờ nó (`wait`), ghi `plan runner_terminated`, thoát mã 143 | `run_process` đã giết cả nhóm khi một lệnh hết giờ. Nhưng SIGTERM từ ngoài chỉ giết Python, còn `adb` con ở nhóm riêng (`start_new_session=True`) thì sống tiếp |
| Rà soát T-046 (5a): tên tệp không được làm sạch | Tên tệp phải khớp `[A-Za-z0-9][A-Za-z0-9._-]*\.txt`. Tên bắt đầu bằng `T-<số>-` thì `ticket:` phải cùng mã. Sai thì `parse_file` báo lỗi: `check` mã 1, `run` ghi `result=lỗi tệp` và không gửi lệnh `adb` nào cho tệp đó | Tên tệp vào thẳng dòng log, đường dẫn ảnh và bảng Markdown của `summary.md` |
| Rà soát T-046 (5b): số cuối dòng bị tách thành số giây (`wait text=Page 2`) | Giá trị đặt được trong ngoặc kép (`text="Page 2"`). Giá trị `text=` hay `desc=` không có ngoặc, có dấu cách, mà dòng kết thúc bằng một số thì `parse_file` báo "mơ hồ, ghi giá trị trong ngoặc kép": `check` mã 1, `run` ghi `lỗi tệp` | Chạy thử: `split_number("text=Page 2")` trả `('text=Page', 2)`. `run_file` chỉ gọi `parse_file`, không gọi `check_files`, và quy tắc 11 chỉ là chú ý; vì vậy kiểm tra phải nằm trong `parse_file` |
| Rà soát T-046 (1): kịch bản chưa kiểm hết tiêu chí của ticket | Không làm ở đây | Đúng như plan T-046 mục 3.1. Thêm sau khi 3 tệp chạy ổn định |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0013 (hộp thoại đồng ý khi mới cài), ADR-0019 (log), ADR-0012 (cấu hình staging giả).

## 3. Giải pháp

### 3.1. Cách làm

Mọi thay đổi theo mẫu có sẵn của `scripts/device-scenario.py`:
- lớp `Runner`, mỗi lệnh một hàm `step_<lệnh>`;
- `run_process` có hạn chót;
- `adb` giả `scripts/tests/fake-adb`.

Dạng dòng `step=` và `result=` trong `scenarios.txt` không đổi. Quy ước khi viết code và test: không `sed -i` thiếu đuôi sao lưu (L-072), không `pgrep -f` hay `pkill -f` (L-067), tệp tạm ở `${TMPDIR:-/tmp}` hay `$work` (L-071), chạy được trên bash 3.2 (L-069).

**Màn hình dùng lại.** Một số bước đọc màn hình cho tới khi màn đạt một điều kiện:
- bước `consent decline`, sau khi chạm "Do not consent", chờ hộp thoại đóng;
- bước `device` chờ màn hình ổn định.

Chỉ hai bước này đặt bản giữ. Lần tự từ chối bên trong một bước đọc màn hình không đặt (mục Hộp thoại đồng ý). `run_file` và `fresh` xóa bản giữ.

Bản dump cuối cùng của hai bước đó được giữ trong `Runner.cached_screen`, với quy tắc hết hạn sau:
- Chỉ bước ngay sau dùng được bản giữ, và chỉ khi bước đó đọc màn hình (`wait`, `need`, `tap`, `tap_if`, `scroll`, `expect`, `expect_no`, `count`). Bước đó dùng nó thay cho lần dump đầu tiên của mình, đúng một lần, rồi xóa.
- Mọi bước khác (`fresh`, `back`, `network`, `shot`, `device`, `consent decline`) xóa `cached_screen` ngay khi bắt đầu, vì chúng đổi màn hình hoặc tự đọc màn hình.
  - `device` và `consent decline` có thể đặt lại một bản mới khi kết thúc.
  - `fresh` xóa nó cùng lúc với `consent_state`.
- Vì vậy, `consent decline 5`, `back`, `expect id=camera_screen` không bao giờ đọc màn hình trước khi bấm Back.

Nhờ vậy, test có sẵn (mỗi lần dump lấy một tệp của chuỗi) không đổi số lần dump.

**`fresh`** (mở app giống kịch bản chuẩn của `device-session.sh`, dòng 201 đến 228):
1. `logcat -b crash -c`, `am force-stop <gói>`, `pm clear <gói>`, cấp quyền (`helper grant-permissions`, như hiện nay).
2. `am start -W -n <activity>` với hạn riêng `min(DEVICE_SCENARIO_START_WAIT, phần còn lại của tệp)` (mặc định 15 giây; test đặt 1). Hết hạn (`AdbTimeout`) thì nhớ lý do `am start -W hết <n> giây` và vẫn làm bước 3, để app khởi động chậm mà vẫn sống không bị dừng.
3. Chờ `pidof <gói>` in một số, tối đa `DEVICE_SCENARIO_PROCESS_WAIT` giây (mặc định 5; test đặt 0.3), đọc mỗi `DEVICE_SCENARIO_POLL` giây. Có tiến trình thì bước `ok`; nếu bước 2 đã hết hạn thì dòng `note` vẫn ghi lý do đó.
4. Không có tiến trình:
   - ghi `note step=<n> t=<s> fresh: lần mở 1 không có tiến trình (<am start -W hết <n> giây | pidof rỗng>), mở lại`;
   - đọc `logcat -b crash -d`: có tên gói thì `FAIL app crash ở lần mở đầu` (không mở lại, để lỗi khởi động của app không bị che);
   - không thì `am force-stop`, làm lại bước 2 và 3 một lần.
5. Vẫn không có: `FAIL app không chạy sau 2 lần mở`.
6. Ngay đầu bước (trước 1): `cached_screen = None`, `consent_state = None`.

Mọi lệnh nằm trong hạn của tệp.

**Hộp thoại đồng ý.** Mỗi tệp giữ `consent_state`: `None`, `"declined"` (đã chạm từ chối) hay `"late"` (thấy màn đầu mà chưa thấy hộp thoại).
- **`consent decline`.**
  - Thấy nút từ chối: chạm một lần, rồi đọc màn hình tới khi không còn nút khớp `CONSENT_DECLINE`. Thời gian chờ tối đa là `DEVICE_SCENARIO_CONSENT_CLOSE` (mặc định 5 giây), trong hạn của tệp; giữa hai lần đọc nghỉ `DEVICE_SCENARIO_POLL` giây. Không chạm lại. Dump lỗi hay `could not get idle state` trong lúc chờ thì đọc lại, không tính là đã đóng.
  - Bước chỉ giữ một bản dump: bản xác nhận hộp thoại đã đóng sau lần chạm (thành `cached_screen`). Nhánh `late` (thấy màn đầu trước) và nhánh không có hộp thoại không giữ gì.
  - Hộp thoại không đóng thì `FAIL hộp thoại đồng ý không đóng sau khi chạm từ chối`.
  - Đóng rồi thì `consent_state = "declined"`.
  - Thấy màn đầu mà chưa thấy hộp thoại thì `consent_state = "late"`.
- **Các bước đọc màn hình** (`wait_for` và `dump_ready`, nên gồm `wait`, `need`, `tap`, `tap_if`, `scroll`, `expect`, `expect_no`, `count`): nếu bản dump có nút từ chối mà không có phần tử bước đó tìm, thì:
  - `consent_state == "declined"`: `FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối`;
  - Hai lỗi này (`hiện lại sau khi đã từ chối`, `không đóng sau khi chạm từ chối`) dùng lớp ngoại lệ riêng `ConsentFail`, không phải `Fail`. `step_need` chỉ đổi `Fail` thành `Skip`, nên trong một bước `need` các lỗi này vẫn là `lỗi`, không thành `bỏ qua (môi trường)` (T-020 dùng `need`; lỗi lưu lựa chọn đồng ý, ADR-0013, không được che);
  - `consent_state == "late"`: chạm nút một lần, chờ hộp thoại đóng như trên, ghi `note step=<n> t=<s> hộp thoại đồng ý hiện muộn, đã từ chối`, đặt `consent_state = "declined"`. Bản dump xác nhận hộp thoại đã đóng chính là màn hình tiếp theo của bước: bước tìm phần tử của nó trên bản đó trước, không dump lại ngay. Không thấy thì bước chờ tiếp như thường (dump mới mỗi `DEVICE_SCENARIO_POLL` giây). Bản đó không được giữ cho bước sau.
  - Vì trạng thái thành `"declined"`, hộp thoại hiện lần nữa trong cùng `fresh` là `FAIL ... hiện lại sau khi đã từ chối`. Vậy mỗi `fresh` có tối đa một lần tự từ chối, không cần biến đếm riêng.
  - `None` (tệp chưa có bước `consent decline`, hoặc vừa `fresh`): không chạm. Bước chạy như hiện nay và thường lỗi vì không thấy phần tử. Vì vậy kịch bản kiểm chính hộp thoại vẫn viết được.
- **Không gì đi qua ranh giới tệp.** `run_file`, cạnh dòng `self.last_xml = None`, đặt `cached_screen = None`, `consent_state = None`.
- **`dump_ready`.** Hạn của nó vẫn là `min(5 giây, phần còn lại của tệp)`, như hiện nay. Lần tự từ chối nằm trong hạn đó, và nếu chờ hộp thoại đóng làm hết hạn thì bước `FAIL` với lý do cuối.

**Bộ lọc `selected=true`.** Nút `n` khớp khi một nút trong tập sau có `selected="true"` hay `checked="true"`:
- `n` chạm được (`clickable="true"`): chỉ `n`;
- `n` không chạm được: `n`, rồi tối đa 2 nút cha gần nhất theo thứ tự từ dưới lên, dừng ngay sau nút cha đầu tiên chạm được;
- không có nút cha nào trong 2 bậc: vẫn xét 2 bậc đó, không đi xa hơn.

`Screen` giữ bảng nút cha khi đọc dump (`ElementTree` không có con trỏ cha). Bộ lọc `clickable=true` không đổi.

Với dump thật: chip "Tất cả" khớp ở bậc 1; chip "Đã lưu" dừng ở nút cha chạm được, không chọn, nên không khớp.

**Chờ ổn định sau lệnh `device`.**
- Sau khi gửi lệnh, đọc dump tới khi hai bản liên tiếp có chữ XML giống nhau, tối đa `DEVICE_SCENARIO_SETTLE` giây (mặc định 10), trong hạn của tệp; giữa hai lần đọc nghỉ `DEVICE_SCENARIO_POLL` giây.
- Dump lỗi `could not get idle state` thì đọc lại.
- Bản cuối thành `cached_screen`.
- Ghi `note ... ổn định sau <x> giây` hay `không ổn định sau <x> giây`. Không ổn định không phải lỗi: bước sau tự chờ phần tử của nó.

**Dòng `note`.**
- Dạng: `plan <tệp> note step=<n> t=<giây từ đầu tệp, một chữ số thập phân> <chi tiết>`, ghi ngay trước dòng `step=` của cùng bước.
- `tap`, `tap_if` (khi có chạm), `scroll` ghi tọa độ và nhãn của nút: `resource-id`, nếu rỗng thì `text`, rồi `content-desc`, cắt còn 40 ký tự.
- Các bước khác chỉ ghi `t=` cùng ghi chú của bước (nếu có).
- `cmd_summary` của `device-session.sh`: sau bảng "Kịch bản theo plan", nếu có dòng `note` chứa `đồng ý` hay `mở lại` thì in mục `Ghi chú của trình chạy:`, mỗi dòng `- <tệp>, bước <n>: <chi tiết>`. Không có thì không in. Bộ lọc khớp đúng hai cụm `hộp thoại đồng ý hiện muộn` và `mở lại`. Dòng `note` của một bước `consent decline` bình thường chỉ ghi `t=` (hoặc `đã chạm từ chối`, không có cụm nào trong hai cụm đó). Vì vậy mục này chỉ hiện khi có chuyện bất thường: tự từ chối hộp thoại muộn, hay `fresh` phải mở lại.

**Ngoặc kép, tên tệp và `ticket:`** (trong `parse_file`, nên cả `check` lẫn `run` đều thấy):
- `SELECTOR` nhận giá trị `"..."` (không có `"` bên trong) cho mọi loại; giá trị trong ngoặc giữ nguyên dấu cách; số sau dấu ngoặc đóng là số giây hay số lần.
- Dòng có `text=` hay `desc=` không ngoặc, giá trị có dấu cách, mà `split_number` tách được một số ở cuối: lỗi `giá trị có dấu cách rồi tới một số là mơ hồ; ghi giá trị trong ngoặc kép`. Quy tắc này cũng bắt `count text=A b 2` và `wait text=A b selected=true 5`: các dòng đó cũng phải ghi `text="A b"`. `docs/ops/test-sessions.md` ghi điều này.
- `SAFE_NAME = r"[A-Za-z0-9][A-Za-z0-9._-]*\.txt"`. Tên không khớp thì lỗi ở dòng 0. Tên bắt đầu bằng `T-<số>-` mà `ticket:` khác mã đó cũng là lỗi.
- `run_file`: có lỗi thì ghi như dòng lỗi tệp có sẵn (`step=0 check FAIL dòng <n>: <lý do>`, `result=lỗi tệp`). Tên trong log thay ký tự ngoài `SAFE_NAME` bằng `_`. Không có lệnh `adb` nào cho tệp đó, kể cả dấu `log -t`.
- Ba kịch bản của repo và mọi tệp fixture có sẵn đều qua các quy tắc này (đã kiểm: `T-020`, `T-022`, `T-024` có `ticket:` trùng tên; `ok.txt`, `fail.txt`, `need.txt` không bắt đầu bằng `T-`).

**`check` không đọc test.**
- Hàm tìm `*.kt` bỏ tệp có một thư mục ngay sau `src/` mà tên bắt đầu bằng `test` hay `androidTest`. Đây cũng là quy tắc của dự án mẫu (PR VGL-Studio/app-project-template#152).
- Tệp `*.kt` không nằm dưới `src/` vẫn được tìm. Ví dụ `feature/demo/DemoTags.kt` của test quy tắc 11.
- `DEVICE_SCENARIO_ROOT` (cho test) chỉ được `main` đọc, cho lệnh `check`, rồi truyền vào `check_files(paths, root=...)`. Hằng `ROOT` của module không đổi, nên `plan-check.py` và `run` vẫn dùng gốc repo.
- Bộ lọc `src/test*` xét đường dẫn tương đối so với gốc đó, như bộ lọc thư mục `build` có sẵn.
- Chữ ký `check_files(paths, root=ROOT)` không đổi, vì `plan-check.py` (quy tắc 11) gọi hàm này.

**Ngân sách gồm thời gian trả máy.**
- `reset_device` có hạn tổng `DEVICE_SCENARIO_RESET_LIMIT` (mặc định 30 giây), mỗi lệnh `min(5, phần còn lại)`. Khi phần còn lại không dương, các lệnh còn lại không được gửi (bỏ qua, không lỗi); lượt cuối cũng vậy. Bật lại mạng thêm tối đa 45 giây, chỉ khi tệp có lệnh `network`.
- **Phần chừa của mỗi tệp còn gồm `DEVICE_SCENARIO_EVIDENCE_RESERVE` giây (mặc định 20; test đặt 0) cho bằng chứng và dấu log:** `log -t` đầu tệp 10 giây, `screencap` của `fail.png` 10 giây. Hai việc này có hạn 10 giây riêng, nằm ngoài hạn của tệp.
- `read_display` (đầu lượt): mỗi lệnh `wm` có 5 giây, tính vào ngân sách như mọi thời gian khác (ngân sách đo từ lúc trình chạy bắt đầu).
- Hạn của tệp là `min(timeout, ngân sách còn lại - RESET_LIMIT - (45 nếu tệp có network) - EVIDENCE_RESERVE)`. Phần còn lại không dương thì tệp ghi `bỏ qua (hết thời gian)`.
- Lần trả máy cuối lượt có cùng hạn tổng và chỉ phụ thuộc `RESET_LIMIT`, không phụ thuộc ngân sách còn lại. Vì vậy test có sẵn `files beyond the budget are skipped` (`DEVICE_SCENARIOS_BUDGET=0`) vẫn thấy `wm size reset`. Lần đó chỉ bật lại mạng khi có tệp đã dùng lệnh `network` (hiện nay luôn bật lại).

Tính toán với ba tệp hiện có:

| Phần | Giây |
| --- | --- |
| `read_display` (tối đa) | 10 |
| T-020 `timeout` 150 + trả máy 30 + bằng chứng 20 | 200 |
| T-022 `timeout` 240 + trả máy 30 + mạng 45 + bằng chứng 20 | 335 |
| T-024 `timeout` 180 + trả máy 30 + bằng chứng 20 | 230 |
| Trả máy cuối lượt (có mạng) | 75 |
| **Tổng tối đa** | **850** |

- **Ngân sách mặc định 900 giây,** ở cả `device-scenario.py` và `device-session.sh`, chừa khoảng 50 giây. Trước đây là 600, nhỏ hơn 850, nên tệp cuối có thể bị cắt.
- **Hạn ngoài của `device-session.sh`:** `900 + 120 = 1020` giây, cộng 30 giây của `-k`. Sau lần trả máy cuối, trong trường hợp xấu nhất, còn khoảng 1020 - 900 - 75 = 45 giây cho việc thoát và ghi tệp.
- **Thời gian của job:** trước đây job chạy 15 đến 25 phút, trong đó phần kịch bản khoảng 4 phút (33 + 77 + 127 giây ở lần 2). Trường hợp xấu nhất là 25 - 4 + 18 ≈ 39 phút, dưới giới hạn 50 phút của `device-session.yml`.
- **Khi tổng vượt 900 giây:** tệp mới làm tổng vượt thì plan của tệp đó tăng `DEVICE_SCENARIOS_BUDGET` cùng lúc. Điều này được ghi trong `docs/ops/test-sessions.md`.

**SIGTERM.**
- `signal.signal(SIGTERM, ...)` đổi tín hiệu thành ngoại lệ `Terminated` (lớp con của `BaseException`).
- `run_process` bắt mọi `BaseException` trong lúc chờ, giết nhóm tiến trình con (`os.killpg(..., SIGKILL)`), gọi `process.wait()`, rồi ném tiếp.
- `main` bắt `Terminated`: ghi `plan runner_terminated` vào `scenarios.txt`, trả máy về gốc với hạn tổng 10 giây, thoát mã 143.
- `device-session.sh` dòng 252: `timeout -k 30 "$((${DEVICE_SCENARIOS_BUDGET:-900} + 120))" ...`. Số 30 giống dự án mẫu. Mã 124 hay 143 vẫn đi vào nhánh `plan runner_error` có sẵn.

**Kịch bản T-022** (`feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`):
- Dòng `criterion:` thành `Luồng thật trên máy, bước 1 đến 7 (plan T-022, mục 4); bước 5: đang ở chi tiết thì mất mạng, ảnh lớn có thể đã tải`.
- Xóa dòng chú thích cũ `# (5) mất mạng, mở chi tiết chưa tải ảnh lớn: hộp thoại mạng; bật mạng, Thử lại: ảnh hiện` (dòng 34, nằm trên `fresh`, ngoài khối bị thay). Tệp chỉ còn đúng một chú thích (5), là chú thích mới dưới đây.
- Thay khối từ `network off` tới `wait id=pose_detail_image 30` bằng:

```text
# (5) đang ở chi tiết thì mất mạng: hộp thoại mạng hiện trên chi tiết; có mạng lại, "Thử lại": hộp thoại đóng, ảnh lớn hiện.
# Ảnh lớn có thể đã tải xong trước khi tắt mạng; trường hợp ảnh lỗi vì mất mạng do unit test của T-022 kiểm.
tap idp=poses_card_ in=poses_grid index=3
wait id=pose_detail_screen 15
network off
wait id=network_dialog 30
shot offline
network on
tap id=network_dialog_retry
wait id=pose_detail_image 30
```

Các dòng (6) và (7) sau đó giữ nguyên.

**Kịch bản T-024** (`feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`): chỉ thêm chú thích trước khối (1). Kịch bản chụp và kiểm theo những gì emulator thật sự hiện, không kiểm số cột (Tuấn, 2026-10-24, mục 8 câu 2). Chú thích:

```text
# Emulator API 34 đóng khung app khóa hướng dọc khi mở gập (2 cột, FB-20261024-01): không kiểm số cột ở đây.
```

Kịch bản T-020 không đổi.

**Tài liệu.** `docs/ops/test-sessions.md` mục 5 ghi:
- `fresh` mở lại khi không có tiến trình, và lỗi khi bộ đệm crash có app;
- quy tắc hộp thoại đồng ý: tự từ chối hộp thoại hiện muộn một lần; hộp thoại hiện lại sau khi đã từ chối là lỗi;
- quy tắc `selected=true`;
- chờ ổn định sau lệnh `device`;
- ngoặc kép;
- dòng `note` và mục "Ghi chú của trình chạy";
- quy ước tên tệp và `ticket:`;
- ngân sách 900 giây gồm thời gian trả máy, và quy tắc "tệp làm tổng vượt thì tăng ngân sách";
- các biến `DEVICE_SCENARIO_SETTLE`, `DEVICE_SCENARIO_RESET_LIMIT`, `DEVICE_SCENARIO_CONSENT_CLOSE`, `DEVICE_SCENARIO_START_WAIT`, `DEVICE_SCENARIO_EVIDENCE_RESERVE`, `DEVICE_SCENARIO_PROCESS_WAIT`, `DEVICE_SCENARIO_ROOT`.

Docstring đầu `device-scenario.py` cập nhật cùng nội dung.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/device-scenario.py` | `fresh` (xóa `cached_screen` và cờ đồng ý, `logcat -b crash -c`, `force-stop`, `pm clear`, `am start -W` hạn `DEVICE_SCENARIO_START_WAIT`, chờ `pidof` kể cả khi `am start -W` hết hạn, bộ đệm crash, mở lại một lần); `consent_state` (tự từ chối đặt `"declined"`), chờ hộp thoại đóng, `run_file` đặt lại `cached_screen` và `consent_state`; `DEVICE_SCENARIO_EVIDENCE_RESERVE`; `cached_screen` (chỉ bước đọc màn hình ngay sau dùng, mọi bước khác xóa); `selected=true` theo quy tắc nút cha (bảng cha trong `Screen`); chờ ổn định sau `device`; dòng `note`; ngoặc kép, số mơ hồ, `SAFE_NAME`, `ticket:` khớp tên trong `parse_file`; `check` bỏ `src/test*`, `src/androidTest*`, `DEVICE_SCENARIO_ROOT`; hạn tổng của `reset_device`, `read_display` 5 giây, hạn tệp chừa phần trả máy, ngân sách mặc định 900; `Terminated`, `run_process` giết nhóm con và `wait()`; docstring |
| Sửa | `scripts/device-session.sh` | Dòng 252: `timeout -k 30`, mặc định 900; `cmd_summary`: mục "Ghi chú của trình chạy"; chú thích khối biến môi trường (dòng 30 đến 33): mặc định 900 |
| Sửa | `scripts/tests/fake-adb` | Giữ mọi hành vi có sẵn. Mọi hành vi mới về trạng thái chỉ chạy khi `FAKE_ADB_STATE_DIR` được đặt; không đặt thì `pidof` không in gì (như hiện nay, `*) exit 0`), nên test `device-session` (dòng 1758 đến 1836 của `run.sh`) và `scripts/app-explore/ax.sh` (đọc `pidof`) không đổi. Khi đặt: `pidof <gói>` in `4242` khi có tệp `$FAKE_ADB_STATE_DIR/alive`; `am start -W` chạy `mkdir -p "$FAKE_ADB_STATE_DIR"` rồi tăng `starts.n` và tạo `alive` khi `starts.n` lớn hơn `$FAKE_ADB_DEAD_STARTS` (mặc định 0); `pm clear`, `am force-stop` xóa `alive`; nhánh `$FAKE_ADB_HANG` (trong nhánh `shell)`, sau `shift`, trước `case "$*"`): chỉ khi `FAKE_ADB_STATE_DIR` được đặt thì `mkdir -p` thư mục đó, ghi `$$` vào `hang.pid`, và tạo `alive` nếu `FAKE_ADB_ALIVE_AFTER_HANG=1`; rồi `exec sleep 300`. Không dùng `mapfile` (bash 3.2, L-069). Không cần trạng thái: `logcat -b crash -d` in `$FAKE_ADB_CRASH_LOG` (mặc định không gì) rồi thoát, thay vì chờ như `logcat -v`. Chú thích đầu tệp |
| Tạo | `scripts/tests/fixtures/device-scenario/chips.xml` | Theo cấu trúc dump thật của T-024: chip "Tất cả" (`TextView` trong `View` `clickable="false"` `selected="true"`), chip "Đã lưu" (`TextView` trong `View` `clickable="true"` `selected="false"`); nút `clickable="true"` `content-desc="deep_clickable"` trong một cha `selected="true"`; `TextView` "Sâu" có ông cố (bậc 3) `selected="true"`, hai bậc trên không chạm được, không chọn; `TextView` "Hands in Pockets" `[96,1200][984,1300]` ngoài chip |
| Tạo | `scripts/tests/fixtures/device-scenario/chips-saved.xml` | Như `chips.xml` nhưng nút cha của "Đã lưu" có `selected="true"` (đối chứng) |
| Sửa | `scripts/tests/run.sh` | Khối `device-scenario` (sau dòng `an empty glob exits before any adb call`): test của mục 4, tệp kịch bản tạm ghi vào `$work` (`ds_script`), không thêm fixture tệp kịch bản; khối `device-session`: test `timeout -k`, test "Ghi chú của trình chạy" |
| Sửa | `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt` | `criterion:` và khối (5) (mục 3.1) |
| Sửa | `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt` | Một dòng chú thích trước khối (1) (mục 3.1) |
| Sửa | `docs/ops/test-sessions.md` | Mục 5: các điểm ở cuối mục 3.1; câu "mặc định 600" thành 900 |

**Nơi khác đọc các file plan này sửa (L-020), và vì sao không bị ảnh hưởng:**
- **`scripts/plan-check.py`** nạp `scripts/device-scenario.py` (`scenario_runner()`) và gọi `check_files`.
  - Chữ ký và dạng thông báo `<tệp> dòng <n>: <lý do>` giữ nguyên. Lỗi mới chỉ hiện ở quy tắc 11, vẫn là chú ý.
  - Đã kiểm test có sẵn của quy tắc 11 (`run.sh` dòng 1036 đến 1045): `T-940-good.txt` có `ticket: T-940`, `T-941-bad.txt` có `ticket: T-941`, đều khớp tên. Không dòng nào có `text=`/`desc=` mơ hồ. Tag `demo_tag` nằm ở `feature/demo/DemoTags.kt`, không dưới `src/test`. Vì vậy các test đó không đổi kết quả.
- **`.github/workflows/device-session.yml`** gọi `scripts/device-session.sh` (`start ci`, `scenarios --yes`, `stop`) và đọc `summary.md`, không đặt `DEVICE_SCENARIOS_BUDGET`. Lệnh và tên tệp không đổi. Thời gian xấu nhất tính ở mục 3.1 (khoảng 39 phút) dưới `timeout-minutes: 50`.
- **`scripts/device-session.sh` `cmd_summary`** đọc các dòng `^plan [^ ]+ step=[0-9]+ .* FAIL` và `^plan [^ ]+ result=`. Dòng `note` có chữ `note` ngay sau tên tệp, nên không khớp hai mẫu đó. Mục "Ghi chú của trình chạy" là phần thêm sau bảng. Dòng `plan runner_terminated` không khớp mẫu `result=`, giống dòng `plan runner_error` có sẵn.
- **`scripts/tests/fake-adb`** cũng được test của `scripts/app-explore/` và của `device-session` dùng. Biến `FAKE_ADB_STATE_DIR` được đặt ở những chỗ sau:
  - khối `device-session` đầu (khoảng dòng 1758 đến 1836) và test của `app-explore` không đặt, nên `pidof` không in gì, như hiện nay;
  - khối `device-session` có trình chạy (khoảng dòng 1976 đến 2068) `export FAKE_ADB_STATE_DIR="$work/adb-state-session"`, và mọi `ds_run` đặt `"$work/adb-state"`.

  Ở hai chỗ đặt biến, kịch bản chuẩn (`device-session.sh`) chỉ gọi `pm clear`, `am force-stop`, `am start -W`, không gọi `pidof`. Tệp `alive` mà `am start -W` tạo chỉ có tác dụng với `fresh` của trình chạy, và `fresh` tự gọi `am start -W` trước khi đọc `pidof`. Vì vậy test có sẵn của hai chỗ đó không đổi kết quả. Đầu ra của `am start -W`, `pm clear`, `am force-stop` không đổi. `logcat -b crash -d` không có trong test có sẵn nào.
- **`app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ui/UiAutomatorTagsTest.kt`** nhắc `scripts/device-scenario.py` trong chú thích (lý do bật `testTagsAsResourceId`), không chạy nó. Plan không đổi cách đọc `resource-id`.
- **`scripts/tests/run.sh`** được `scripts/check.sh` chạy (cả tệp), và được `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/wait-run.sh` nhắc trong chú thích. Plan chỉ thêm test vào khối `device-scenario` và `device-session`; cách gọi và dòng tổng kết không đổi. Hàm `ds_run` không đổi; biến mới truyền qua tham số `VAR=value` có sẵn.
- **`docs/ops/test-sessions.md`** được `docs/CLAUDE.md` và `docs/templates/plan.md` dẫn tới "mục 5". Tên mục giữ nguyên.
- **Hai tệp kịch bản** được test `the scenarios of the repository pass check` đọc, chạy lại ở bước 5. Plan T-022, T-024, T-046 nhắc tên tệp nhưng không đọc nội dung.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm. Trình chạy chỉ chạm nút từ chối của hộp thoại đồng ý có sẵn (ADR-0013), trên emulator, như T-046.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `fresh` vẫn cấp quyền runtime đã khai báo qua `helper grant-permissions`.
- **Thư viện và SDK:** Không thêm. Trình chạy dùng Python chuẩn (`signal`, `subprocess`, `xml.etree`).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc,** chỉ biết ở `device-session` đầu tiên sau khi merge:
  - `pidof` có trên emulator API 34 (`toybox pidof`, có từ Android 7);
  - hai bản dump liên tiếp của màn đứng yên có giống nhau từng ký tự;
  - chip "Đã lưu" có đổi sau khi màn đã ổn định;
  - `logcat -b crash` có trên emulator CI.
- **Điều kiện chuyển:** báo cáo của lần chạy đầu cho thấy một trong các dấu hiệu sau:
  - `fresh` lỗi `app không chạy` trong khi `fail.png` là màn của app;
  - mọi dòng `note` của lệnh `device` ghi `không ổn định`;
  - job `device-session` chạy quá 45 phút.

  Phiên không dừng vì điều này (mục 7, điều 3).
- **Cách làm thay:** sửa ở một task sau.
  - Đọc tiến trình bằng `ps -A -o NAME` thay cho `pidof`.
  - So dump sau khi bỏ thuộc tính `bounds` của nút có hiệu ứng, hoặc chờ `wm size` báo kích thước mới rồi đợi 2 giây.
  - Hạ `timeout` của từng tệp hay hạ ngân sách.

  Tiêu chí và test của mục 4 giữ nguyên. Plan này không cài sẵn phần dự phòng.

## 4. Kiểm thử

Test viết trước code: mọi dòng "Test script" dưới đây, trong khối `device-scenario` và `device-session` của `scripts/tests/run.sh`, chạy với `adb` giả. Bước 3 (khung rỗng) chỉ thêm fixture và các nhánh mới của `fake-adb`, không đổi `device-scenario.py` hay `device-session.sh`, nên các test mới thất bại vì hành vi cũ.

Lý do thất bại dự kiến trên code hiện tại:
- `fresh`: không có `am start -W`, không đọc `pidof`;
- `selected`: không khớp nút cha (đã đo ở mục 4.1);
- `note`: không có dòng nào;
- ngoặc kép: code cũ giữ dấu ngoặc trong giá trị, nên `check` qua, còn `wait` lỗi `không thấy phần tử`;
- ngân sách: trả máy chờ 10 giây mỗi lệnh;
- SIGTERM: tiến trình `sleep` còn sống;
- hộp thoại đồng ý và bản giữ: code cũ không chờ hộp thoại đóng, không tự từ chối, không giữ bản dump, nên số lần chạm, `ui-seq.n` hay lý do lỗi khác với code mới;
- lệnh `device`: code cũ không dump sau lệnh, nên `ui-seq.n` là 0 và không có dòng `note`.

Số thật của từng test trên code hiện tại nằm ở mục 4.1 (bảng "Chạy các test mới trên code hiện tại"). Giá trị mong đợi trên code mới suy ra từ mục 3.1 và không vượt giới hạn của `fake-adb`: `ui-seq.n` không lớn hơn số tệp của chuỗi, vì khi hết chuỗi `fake-adb` lặp lại tệp cuối và `ui-seq.n` đứng yên.

Các dòng ghi **(đối chứng)** qua được cả trên code cũ. Chúng giữ cho phần sửa không quá tay (L-035), và không tính vào số test thất bại trước.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| `fresh` mở bằng `-W` | Test script | `device-scenario: fresh clears the app and starts it with am start -W`: `$FAKE_ADB_CALLS` có, theo thứ tự, `shell am force-stop`, `shell pm clear`, `shell am start -W -n com.example.app/.MainActivity`, `shell pidof com.example.app.staging`; bước `ok`; một lệnh `am start -W` |
| Tiến trình bị giết thì mở lại | Test script | `device-scenario: fresh starts the app again when the first start left no process`: `FAKE_ADB_DEAD_STARTS=1 DEVICE_SCENARIO_PROCESS_WAIT=0.3`: hai lệnh `am start -W`, một lệnh `logcat -b crash -d`, bước `ok`, dòng `note step=1` có `pidof rỗng, mở lại` |
| `am start -W` treo | Test script | `device-scenario: a hanging am start counts as no process and is retried`: `FAKE_ADB_HANG="am start"`, `DEVICE_SCENARIO_START_WAIT=1`, `DEVICE_SCENARIO_PROCESS_WAIT=0.3`, `timeout: 60`: dòng `note` có `am start -W hết 1 giây`, đúng hai lệnh `am start -W`, `step=1 fresh FAIL app không chạy sau 2 lần mở`, cả lượt xong dưới 15 giây |
| App chậm mà sống không bị dừng | Test script | `device-scenario: a slow am start with a live process is not restarted`: `FAKE_ADB_HANG="am start"`, `DEVICE_SCENARIO_START_WAIT=1`, `FAKE_ADB_ALIVE_AFTER_HANG=1` (nhánh treo của `am start` tạo `alive` trước khi `exec sleep`, như app khởi động chậm mà vẫn sống): một lệnh `am start -W`, bước `ok`, `note` có `am start -W hết 1 giây`, không có lệnh `am force-stop` thứ hai |
| Không mở được | Test script | `device-scenario: fresh fails when the app never runs`: `FAKE_ADB_DEAD_STARTS=5 DEVICE_SCENARIO_PROCESS_WAIT=0.3`: `step=1 fresh FAIL app không chạy sau 2 lần mở`, đúng hai lệnh `am start -W` |
| Crash lần đầu không bị che | Test script | `device-scenario: fresh fails on a first-start crash instead of starting again`: `FAKE_ADB_DEAD_STARTS=1`, `DEVICE_SCENARIO_PROCESS_WAIT=0.3`, `FAKE_ADB_CRASH_LOG` là tệp `$work/ds-crash.txt` ghi bằng `printf` (không thêm fixture), có `FATAL EXCEPTION` và `Process: com.example.app.staging`: `step=1 fresh FAIL app crash ở lần mở đầu`, đúng một lệnh `am start -W` |
| Đồng ý hiện muộn | Test script | `device-scenario: a consent form that shows after consent decline saw the app is declined once`: tệp `consent decline 5`, `wait id=onboarding_screen 10`, `expect id=camera_screen`; chuỗi dump `onboarding.xml consent.xml onboarding.xml camera.xml`: cả tệp `đạt`, đúng một lệnh chạm tâm nút `Do not consent` (`540 1970`), có `note` "hiện muộn", `ui-seq.n` là 4. Lần theo: bước 1 dump 1 `onboarding` (`late`); bước 2 dump 2 `consent` (tự chạm), dump 3 `onboarding` xác nhận đóng và là màn của bước 2 (thấy); bước 3 dump 4 `camera`. Code cũ: `đạt`, 0 lần chạm (thất bại vì số lần chạm) |
| Chỉ một lần | Test script | `device-scenario: a second late consent form is not declined again`: chuỗi `onboarding.xml consent.xml onboarding.xml consent.xml`, tệp `consent decline 5`, `wait id=onboarding_screen 3`, `wait id=camera_screen 1`: đúng một lệnh chạm; `step=3 ... FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối`; `ui-seq.n` là 4. Lần theo: bước 1 dump 1 (`late`); bước 2 dump 2 `consent` (tự chạm, thành `declined`), dump 3 `onboarding` (thấy); bước 3 dump 4 `consent`, trạng thái `declined` thì lỗi ngay |
| Hiện lại sau khi từ chối | Test script | `device-scenario: a consent form that shows again after decline is a failure`: chuỗi `consent.xml onboarding.xml consent.xml`, tệp `consent decline 5`, `wait id=camera_screen 3`: một lệnh chạm, `step=2 ... FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối`, `ui-seq.n` là 3. Lần theo: bước 1 dump 1 `consent` (chạm), dump 2 `onboarding` (đóng, giữ lại); bước 2 dùng bản giữ (không có camera, không có hộp thoại), dump 3 `consent`, lỗi |
| Đối chứng: không có bước đồng ý | Test script (đối chứng) | `device-scenario: without consent decline a consent form is not tapped`: chuỗi `consent.xml`, tệp chỉ có `expect id=onboarding_screen`: không có lệnh `input tap` nào; `expect` lỗi |
| `fresh` đặt lại cờ | Test script | `device-scenario: fresh resets the consent state`: tệp `fresh`, `consent decline 5`, `fresh`, `wait id=onboarding_screen 3`; chuỗi `consent.xml onboarding.xml consent.xml onboarding.xml`: `đạt`, một lệnh chạm, `ui-seq.n` là 4. Lần theo: bước 2 dump 1 `consent` (chạm), dump 2 `onboarding` (đóng, `declined`); bước 3 `fresh` xóa bản giữ và trạng thái; bước 4 dump 3 `consent` với trạng thái `None` (không chạm, chờ tiếp), dump 4 `onboarding` (thấy). Thiếu phần đặt lại thì bước 4 lỗi `hiện lại sau khi đã từ chối` ở dump 3 |
| Không gì đi qua tệp | Test script | `device-scenario: nothing kept crosses into the next file`: hai tệp ghi thẳng vào `$work/ds-two` bằng `printf` (không qua `ds_script`, vì `ds_script` luôn ghi vào `$ds_files`), `DEVICE_SCENARIOS_GLOB="$work/ds-two/*.txt"`: `a.txt` chỉ có `consent decline 5`, `b.txt` chỉ có `expect id=camera_screen`; chuỗi `consent.xml onboarding.xml camera.xml`: cả hai `đạt`, `ui-seq.n` là 3. Lần theo: `a.txt` dump 1 `consent` (chạm), dump 2 `onboarding` (giữ); trả máy không dump; `b.txt` bắt đầu bằng xóa bản giữ, dump 3 `camera` (thấy). Thiếu phần đặt lại thì `b.txt` dùng `onboarding` và lỗi |
| Bản giữ hết hạn | Test script | `device-scenario: a kept screen is not used after back`: tệp `consent decline 5`, `back`, `expect id=camera_screen`; chuỗi `consent.xml onboarding.xml camera.xml`: `đạt`, `ui-seq.n` là 3 (`back` xóa bản `onboarding.xml` đã giữ, `expect` dump lại) |
| Chờ hộp thoại đóng | Test script | `device-scenario: consent decline waits for the form to close and taps once`: chuỗi `consent.xml consent.xml consent.xml onboarding.xml`, tệp `consent decline 5`, `expect id=onboarding_screen`: một lệnh chạm, `đạt`, `ui-seq.n` là 4 (bản `onboarding` cuối dùng lại cho `expect`). Code cũ: `expect` dump `consent` thứ hai, lỗi |
| Lỗi đồng ý trong `need` | Test script | `device-scenario: a consent error inside need is a failure, not a skip`: tệp `consent decline 5`, `need id=x 3`; chuỗi `consent.xml onboarding.xml consent.xml`: `result=lỗi`, dòng `step=2 ... FAIL hộp thoại đồng ý hiện lại sau khi đã từ chối`, `ui-seq.n` là 3. Code cũ: `result=bỏ qua (môi trường)` |
| Hộp thoại không đóng | Test script | `device-scenario: a consent form that does not close after the tap is a failure`: tệp có sẵn `consent.txt` (`consent decline 5`), chuỗi chỉ `consent.xml`, `DEVICE_SCENARIO_CONSENT_CLOSE=1`: `step=1 ... FAIL hộp thoại đồng ý không đóng sau khi chạm từ chối`, một lệnh chạm. Đây là phần thất bại trên code cũ (code cũ trả `ok` ngay sau khi chạm) |
| Test có sẵn của đồng ý | Test script | `consent decline taps Do not consent when shown` (chỉ kiểm mã 0 và một lệnh chạm), `waits for a late consent form`, `taps nothing when there is no form`, `fails when nothing shows` giữ nguyên, phải còn qua. `consent decline taps Do not consent when shown` (chuỗi chỉ `consent.xml`) nay chờ hộp thoại đóng 5 giây rồi `FAIL`; test chỉ kiểm mã 0 và một lệnh chạm nên vẫn qua, chỉ chậm thêm 5 giây. Plan để nguyên test đó, không truyền `DEVICE_SCENARIO_CONSENT_CLOSE`, vì sửa test có sẵn là điều kiện dừng (mục 7). Test `a passing file is reported as passed` (`ok.txt` trên `consent.xml onboarding.xml camera.xml`) cũng vậy: bản `onboarding.xml` xác nhận hộp thoại đóng được dùng lại cho `tap_if`, nên lệnh `shell input tap 540 1806` của test `tap uses the centre of the bounds` vẫn có |
| `selected=true` theo nút cha | Test script | `device-scenario: selected=true matches a text whose parent chip is selected`: `chips.xml`, `expect str=poses_chip_all selected=true` đạt |
| Đối chứng chip | Test script | `device-scenario: selected=true stops at the clickable parent of an unselected chip`: `chips.xml`, `expect str=poses_chip_saved selected=true` lỗi (đối chứng, qua cả trên code cũ); `chips-saved.xml` thì đạt (phần thất bại trên code cũ) |
| Nút chạm được chỉ xét chính nó | Test script (đối chứng) | `device-scenario: a clickable node is selected only by itself`: `chips.xml`, `expect desc=deep_clickable selected=true` lỗi (cha chọn, nút không) |
| Không quá 2 bậc | Test script (đối chứng) | `device-scenario: selected=true looks at most two parents up`: `chips.xml`, `expect text=Sâu selected=true` lỗi (ông cố chọn) |
| `selected=true` có sẵn | Test script | Hai test có sẵn `selected=true matches selected or checked` và `does not match an unselected chip` giữ nguyên, phải còn qua |
| Ổn định sau `device` | Test script | `device-scenario: a device command waits until two dumps agree`: tệp chỉ có `device unfold`; chuỗi `camera.xml grid.xml grid.xml`: đạt, `ui-seq.n` là 3 khi lượt xong; có `note step=1 ... ổn định`. Thêm: tệp `device unfold`, `expect id=poses_grid` trên chuỗi `camera.xml grid.xml grid.xml camera.xml`: `đạt`, `ui-seq.n` là 3 (bước 2 dùng lại bản `grid` cuối; không dùng lại thì `expect` gặp `camera` và lỗi) |
| Không ổn định không phải lỗi | Test script | `device-scenario: a screen that never settles is noted, not failed`: `DEVICE_SCENARIO_SETTLE=1`, chuỗi xen kẽ `camera.xml grid.xml` 100 tệp: bước 1 `ok`, `note` có `không ổn định`, `ui-seq.n` nhỏ hơn 100 (vòng chờ ổn định nghỉ `DEVICE_SCENARIO_POLL` = 0.05 giây giữa hai lần dump, nên khoảng 20 lần trong 1 giây và không chạm tệp cuối lặp lại) |
| Dòng `note` của chạm | Test script | `device-scenario: a tap notes its coordinates and the element`: `ok.txt` có sẵn: dòng `note step=3 t=` có `540,1806` và `onboarding_enable_camera`; số dòng `^plan ok.txt step=[0-9]* .* ok$` vẫn là 6 |
| Ghi chú lên `summary.md` | Test script | `device-session: summary lists the runner notes about consent and restarts`: `mkplan` với dòng mẫu có sẵn cộng `plan ok.txt note step=2 t=3.1 hộp thoại đồng ý hiện muộn, đã từ chối` và một dòng `note` chỉ có `t=`: `summary.md` có `Ghi chú của trình chạy:` và `- ok.txt, bước 2: hộp thoại đồng ý hiện muộn, đã từ chối`, không có dòng `t=` trơn. Đối chứng: test có sẵn `summary lists the plan scenarios` (không có `note`) không có `Ghi chú của trình chạy` |
| Ngoặc kép | Test script | `device-scenario: a quoted value keeps its spaces and the number after it is the timeout`: `wait text="Hands in Pockets" 3` trên `chips.xml`: đạt; `check` qua |
| Số cuối dòng mơ hồ | Test script | `device-scenario: a text with spaces followed by a number is a file error`: `wait text=Page 2`: `check` mã 1, dòng `dòng 4` có "ngoặc kép"; `run` ghi `result=lỗi tệp`, không có lệnh `input`. Đối chứng: `wait id=poses_grid 2` qua `check` |
| `check` không đọc test | Test script | `device-scenario: check ignores tags that only tests have`: `DEVICE_SCENARIO_ROOT` trỏ cây tạm `$work/ds-root/feature/x/src/main/A.kt` (`"main_tag"`) và `$work/ds-root/feature/x/src/test/B.kt` (`"test_only_tag"`): `expect id=test_only_tag` thì `check` mã 1; `expect id=main_tag` thì mã 0 |
| Tên tệp lạ | Test script | `device-scenario: a file name with odd characters is a file error and sends no adb command`: tệp `$work/ds-names/a b|c.txt` (nội dung đúng), `DEVICE_SCENARIOS_GLOB="$work/ds-names/*.txt"` (glob một mẫu, không có dấu cách): `scenarios.txt` có `plan a_b_c.txt step=0 check FAIL` có "tên tệp" và `result=lỗi tệp`; `$FAKE_ADB_CALLS` không có `log -t`; `check` mã 1 |
| `ticket:` khớp tên | Test script | `device-scenario: a T-NNN file whose ticket differs is a file error`: `T-901-x.txt` có `ticket: T-902`: `check` mã 1. Đối chứng: `ok.txt` qua |
| Ngân sách gồm trả máy | Test script | `device-scenario: the reset time is taken from the budget`: `FAKE_ADB_HANG="settings"` (chỉ trả máy treo; `read_display` dùng `wm` nên không treo), `DEVICE_SCENARIO_RESET_LIMIT=2`, `DEVICE_SCENARIO_EVIDENCE_RESERVE=0`, `DEVICE_SCENARIOS_BUDGET=6`, một tệp `timeout: 300` có `wait id=absent 30` trên `blank.xml` (không có lệnh `network`): cả lượt xong dưới 13 giây, có `result=lỗi`. Tính: hạn tệp `min(300, 6 - 2 - 0)` ≈ 4 giây, trả máy sau tệp 2 giây, trả máy cuối 2 giây (không bật mạng), tổng khoảng 8 giây. Trên code hiện tại: hai lệnh `settings` của mỗi lần trả máy chờ 10 giây, hai lần trả máy, nên lượt kéo dài hơn 40 giây |
| SIGTERM | Test script | `device-scenario: a terminated runner kills its adb child and says so`. Viết đúng cho `set -euo pipefail`: `env PATH=... FAKE_ADB_STATE_DIR="$work/adb-term" FAKE_ADB_HANG=uiautomator DEVICE_SCENARIO_ADB_TIMEOUT=100 DEVICE_SCENARIOS_GLOB="$ds_files/term.txt"` (tệp ghi bằng `ds_script term.txt 'wait id=x 90'`; `ds_script` đặt `timeout: 30`, đủ dài để tín hiệu tới trong lúc `uiautomator` treo), rồi `python3 -I scripts/device-scenario.py run --session "$work/ds-term" --package com.example.app.staging &`, `pid=$!`; vòng `for _ in $(seq 1 100)` chờ `hang.pid` có (mỗi vòng `sleep 0.1`, không chờ cứng); `kill -TERM "$pid"`; `code=0; wait "$pid" \|\| code=$?`. Đầu ra của lệnh nền ghi vào `$work/ds-term.out`. Kiểm: `code` là 143; `scenarios.txt` có `plan runner_terminated`; `kill -0 "$(cat "$work/adb-term/hang.pid")"` thất bại. Cuối test: `kill -9 "$(cat "$work/adb-term/hang.pid")" 2>/dev/null \|\| true`. Không dùng `pgrep -f` (L-067) |
| `timeout -k` | Test script | `device-session: the runner is called with timeout -k`: `grep -c 'timeout -k 30' scripts/device-session.sh` là 1 |
| Kịch bản của repo | Test script | Test có sẵn `device-scenario: the scenarios of the repository pass check`, chạy với `check` mới trên hai tệp đã sửa |
| Quy tắc 11 không đổi | Test script | Các test có sẵn `plan-check: ... rule 11` (dòng 1036 đến 1077 của `run.sh`) phải còn qua |
| Ba kịch bản chạy thật | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`, `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`, `feature/camera/src/deviceScenarios/T-020-camera-mo-gap.txt`) | Lần `device-session` đầu tiên sau khi merge. Phiên đọc bảng "Kịch bản theo plan" và "Ghi chú của trình chạy", mở `fail.png` của tệp lỗi, ghi vào báo cáo lỗi đó của app hay của kịch bản. T-020 trên CI có thể `bỏ qua (môi trường)` vì không có camera sau; kết quả đó không tính là lỗi (mục 8 câu 3) |
| Phiên làm theo tài liệu | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt chéo đọc diff `docs/ops/test-sessions.md` mục 5 |

**Ngoại lệ viết test trước:**
- `docs/ops/test-sessions.md` là tài liệu.
- Hai tệp kịch bản là test chạy trên emulator. Phiên chỉ kiểm được bằng `check`; chạy thật ở `device-session` cuối phiên.
- Dòng `timeout -k` của `device-session.sh` chỉ có test `grep`, vì môi trường test không có emulator và macOS có thể không có lệnh `timeout`.

### 4.1. Chạy thử

Tiêu chí E là 2: bắt SIGTERM bằng `signal.signal` rồi `os.killpg` nhóm con là lần đầu trong `scripts/`.

- **Rà soát độc lập (2026-10-24, macOS):** chạy luồng SIGTERM → hàm bắt tín hiệu → `killpg` → thoát 143. 10/10 lần tiến trình `sleep` con đã chết, dù có hay không có `wait()`. Plan vẫn gọi `process.wait()` sau `killpg` để không để lại tiến trình zombie.
- **Chạy thử bằng `scripts/check.sh`:**
  - **Ngày, nhánh:** 2026-10-24, nhánh `claude/plan-T-048-kich-ban-on-dinh` (từ nhánh chính `e3f02b8`), máy dev (macOS, bash 3.2, JDK 21).
  - **Test tạm (không commit, đã xóa):** một khối ở cuối khối `device-scenario` của `scripts/tests/run.sh`. Khối này chạy 10 lần một chương trình Python tạm ở scratchpad (`trial-t048/sigterm.py`):
    - chương trình đặt hàm bắt SIGTERM, mở `sh -c 'echo $$ > hang.pid; exec sleep 300'` với `start_new_session=True`;
    - khi nhận SIGTERM: `killpg(SIGKILL)`, `wait()`, ghi `plan runner_terminated`, thoát 143.

    Khối shell chạy chương trình ở nền, chờ `hang.pid` bằng vòng có giới hạn, `kill -TERM`, `wait "$pid" || code=$?`, rồi kiểm mã 143, `kill -0` trên tiến trình con thất bại, và dòng `runner_terminated` có trong tệp.
  - **Lệnh:** `scripts/check.sh`.
  - **Kết quả:** `ok   - TRIAL T-048: SIGTERM kills the child 10 of 10 times`; `All script tests passed`; `Test JVM: 1050 test, 0 lỗi`; `== Cổng chất lượng: QUA` (5 phút 55 giây). Khối test tạm đã xóa (`git checkout -- scripts/tests/run.sh`) trước khi commit plan.
- **Chạy các test mới trên code hiện tại** (rà soát lượt 4, ngày 2026-10-24, nhánh chính `e3f02b8`, macOS, bash 3.2).
  - Mọi test mới hay đã đổi của mục 4 được viết thành một khối tạm trong `scripts/tests/run.sh` của worktree, kèm hai fixture `chips.xml`, `chips-saved.xml` đúng như mục 3.2.
  - Khối chạy bằng `bash scripts/tests/run.sh` trên `device-scenario.py`, `device-session.sh` và `fake-adb` hiện tại. Mỗi test in mã thoát, `ui-seq.n`, số lệnh `input tap`, số lệnh `am start -W`, `pidof`, và các dòng `result=`, `FAIL`, `SKIP`, `note`.
  - Khối và fixture đã xóa (`git checkout -- scripts/tests/run.sh`) trước khi commit. Mọi test có sẵn vẫn qua (`All script tests passed`).

  Kết quả thật (mã thoát của trình chạy luôn là 0, vì `main` trả 0 cả khi một tệp lỗi):

  | Test (mục 4) | Trên code hiện tại | Vì sao test thất bại trên code hiện tại |
  | --- | --- | --- |
  | `fresh` mở bằng `-W` | `đạt`; 0 `am start -W`, 0 `pidof` | không có `am start -W`, `pidof` |
  | Tiến trình bị giết thì mở lại | `đạt`; 0 `am start -W` | như trên |
  | `am start -W` treo | `step=1 fresh FAIL adb_timeout` sau 31 giây (lệnh `am start -n` treo tới hạn `adb` 30 giây) | lý do và thời gian khác (không thử lại, không có `note`) |
  | Không mở được | `đạt` | không thấy app không chạy |
  | Crash lần đầu không bị che | `đạt`; 0 `logcat -b crash -d` | không đọc bộ đệm crash |
  | App chậm mà sống | `FAIL adb_timeout` sau 31 giây | bị coi là lỗi |
  | Đồng ý hiện muộn | `đạt`, n=4, 0 lần chạm | không tự từ chối (cần 1 lần chạm) |
  | Chỉ một lần | `step=3 FAIL không thấy phần tử`, n=4, 0 lần chạm | lý do và số lần chạm khác |
  | Hiện lại sau khi từ chối | `step=2 FAIL không thấy phần tử`, n=3, 1 lần chạm | lý do khác (`hiện lại sau khi đã từ chối`) |
  | Không có bước đồng ý (đối chứng) | `step=1 FAIL`, n=1, 0 lần chạm | qua cả trên code cũ |
  | `fresh` đặt lại cờ | `đạt`, n=2, 1 lần chạm | không chờ hộp thoại đóng (n cần 4) |
  | Bản giữ hết hạn | `step=3 expect FAIL`, n=2 | không giữ bản dump |
  | Không gì đi qua tệp | `b.txt step=1 FAIL`, n=2 | như trên |
  | Chờ hộp thoại đóng | `step=2 expect FAIL`, n=2 | không chờ hộp thoại đóng |
  | Hộp thoại không đóng | `đạt`, n=1, 1 lần chạm | trả `ok` ngay sau khi chạm |
  | Lỗi đồng ý trong `need` | `step=2 need SKIP`, `result=bỏ qua (môi trường)`, n=3 | lỗi bị đổi thành bỏ qua |
  | `selected=true` theo nút cha | `FAIL`, n=1 | không xét nút cha |
  | Đối chứng chip trên `chips.xml` (đối chứng) | `FAIL` | qua cả trên code cũ |
  | Chip "Đã lưu" trên `chips-saved.xml` | `FAIL` | không xét nút cha |
  | Nút chạm được chỉ xét chính nó (đối chứng) | `FAIL` | qua cả trên code cũ |
  | Không quá 2 bậc (đối chứng) | `FAIL` | qua cả trên code cũ |
  | Ổn định sau `device` | `đạt`, n=0 | không dump sau lệnh |
  | Ổn định rồi dùng lại | `step=2 expect FAIL`, n=1 | `expect` gặp `camera` |
  | Không ổn định không phải lỗi | `đạt`, n=0, không có `note` | không có dòng `note` |
  | Dòng `note` của chạm (`ok.txt`) | `đạt`, n=3, 2 lần chạm, 0 dòng `note`, 6 dòng `ok`, có `input tap 540 1806` | không có dòng `note` |
  | Ghi chú lên `summary.md` | không có `Ghi chú của trình chạy` | chưa có mục này |
  | Ngoặc kép | `check` 0; `run`: `FAIL không thấy phần tử` (giá trị giữ dấu ngoặc) | `wait` không khớp |
  | Số cuối dòng mơ hồ | `check` 0; `run`: `FAIL không thấy phần tử` sau 2 giây (số 2 thành số giây) | `check` không báo lỗi |
  | `wait id=poses_grid 2` (đối chứng) | `check` 0 | qua cả trên code cũ |
  | `check` không đọc test | `check` 1 cho cả `test_only_tag` lẫn `main_tag` | `DEVICE_SCENARIO_ROOT` chưa được đọc, nên `main_tag` cũng lỗi (cần 1 rồi 0) |
  | Tên tệp lạ | `đạt`, có 1 lệnh `log -t`; `check` 0 | tên lạ vẫn chạy |
  | `ticket:` khớp tên | `check` 0; `ok.txt` (đối chứng) 0 | không kiểm `ticket:` |
  | Ngân sách gồm trả máy | `FAIL hết hạn của tệp`, cả lượt 46 giây | vượt 13 giây |
  | SIGTERM | mã 143, không có `hang.pid`, không có `runner_terminated`; sau đó còn một tiến trình `sleep 300` mồ côi (cha là 1), đã giết bằng tay | đúng lỗi rà soát T-046 (4) |
  | `timeout -k 30` | 0 dòng | chưa có |

- **Test có sẵn `consent decline taps Do not consent when shown`:** kiểm `"0 1"` (mã thoát và một lần chạm), và trình chạy trả 0 cả khi tệp lỗi (`main`, khoảng dòng 656 đến 657). Vì vậy trên code mới test vẫn qua dù bước chờ hộp thoại đóng lỗi sau 5 giây; test không đổi.
- **Số đo trên code hiện tại** (L-029), ngày 2026-10-24, nhánh chính `e3f02b8`. Lệnh Python tạm (không commit) nạp `scripts/device-scenario.py` và dựng `Screen` từ `fail.xml` thật của T-024 (lần chạy 40116129967):
  - `str=poses_chip_saved selected=true` trả `None`;
  - `str=poses_chip_all selected=true` cũng trả `None`, dù chip "Tất cả" đang chọn;
  - `str=poses_chip_saved` (không lọc) tìm thấy nút;
  - `split_number("text=Page 2")` trả `('text=Page', 2)`;
  - `source_files(ROOT, "*.kt")` trả cả `app/src/test/kotlin/.../navigation/MainScreenTest.kt`.
- **Logcat và log app của lần chạy 40117751068:**
  - tiến trình 9674 chết lúc 18:08:57.111, `START` lúc 57.927, `start not valid, killing pid=10782, seq=102,expected=101` và `pid=10785` lúc 18:08:58, `Skip zero pid ANR` lúc 18:09:14;
  - không có `VGL.Loading` nào của T-024;
  - `Dialog shown: device_offline delay=3s` lúc 18:08:18 và không có `Pose detail opened` sau bước 35 của T-022;
  - `Onboarding shown` lúc 18:05:44.330, UMP nhận lệnh đầu lúc 18:05:46.012.
- **Ảnh `fail.png` của T-024, lần 1:** sau `wm size 1766x2208`, app bị đóng khung ở giữa (`[246..1521]`), lưới 2 cột (FB-20261024-01).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI và quy trình phiên tự chạy). Plan đổi `scripts/device-session.sh`, mà `device-session.yml` chạy cuối mỗi phiên, và trình chạy mà nó gọi.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script sửa nhiều chỗ, ba chỗ của `device-session.sh`, `fake-adb`, hai fixture, hai kịch bản, một tài liệu |
| B. Độ rõ của yêu cầu | 1 | Mỗi lỗi có bằng chứng trong artifact; câu hỏi ở mục 8 đã được trả lời |
| C. Khả năng kiểm chứng tự động | 2 | Mọi thay đổi của trình chạy có test với `adb` giả; phần trên emulator thật chỉ kiểm được sau khi merge |
| D. Ảnh hưởng chéo | 1 | `plan-check.py` dùng `check_files` (chữ ký giữ nguyên); `fake-adb` dùng chung, hành vi mới chỉ khi có `FAKE_ADB_STATE_DIR` |
| E. Độ mới của kỹ thuật | 2 | Bắt SIGTERM và `killpg` lần đầu trong `scripts/`; đã chạy thử (mục 4.1). Phần còn lại (`am start -W`, `pidof`) đã có trong repo |
| **Tổng** | 8 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:**
  - `scripts/device-scenario.py` (dùng ở `device-session` cuối mỗi phiên và ở `plan-check.py` quy tắc 11);
  - `scripts/device-session.sh` (máy dev và CI);
  - `scripts/tests/fake-adb`, `scripts/tests/run.sh`.
- **Task khác có thể xung đột:** chưa có ticket nào khác `ready` (2026-10-24). Nếu danh sách chạy có thêm plan sửa `scripts/tests/run.sh`, mỗi plan thêm khối riêng; task chạy lần lượt, mỗi task tạo nhánh từ nhánh chính mới nhất.
- **Việc sau task này, không làm ở đây:**
  - **Quy tắc 11 thành lỗi.** Khi một `device-session` có cả 3 kịch bản không `lỗi`, một dòng sửa ở `scripts/plan-check.py` đổi quy tắc 11 thành lỗi với plan `draft`, kèm test (plan T-046, mục 6). `bỏ qua (môi trường)` từ lệnh `need` tính là không lỗi (Tuấn, 2026-10-24, mục 8 câu 3). Báo cáo phiên ghi vào Đề xuất cho buổi họp tiếp theo.
  - **Kiểm thêm tiêu chí** còn thiếu của T-020, T-022, T-024 (rà soát T-046, điểm 1).
  - **Khóa hướng dọc trên màn lớn:** phản hồi FB-20261024-01 (PR riêng), chủ feature quyết.
- **Dự án mẫu.** PR VGL-Studio/app-project-template#152 (chuyển T-046 về mẫu, issue #133) đã tự sửa bốn điểm.
  - (a) `check` bỏ tag chỉ có dưới thư mục test: plan dùng cùng quy tắc `src/test*`, `src/androidTest*`.
  - (b) `timeout -k 30`: dùng đúng số 30 của mẫu.
  - (c) `fresh` mở qua `helper launcher-activity`: mẫu cần vì bản staging của mẫu có hai activity LAUNCHER, nên `resolve-activity` mở hộp chọn. App này chỉ có một LAUNCHER (`MainActivity`), và `resolve-activity` đã chạy đúng ở cả hai lần `device-session`, nên không chép. App thêm activity LAUNCHER thứ hai thì chép.
  - (d) `consent decline` chờ tag gốc `app_root`: không chép. App chưa có tag `app_root`, và tag gốc hiện cả ở màn tải, trước khi hộp thoại kịp hiện, nên hộp thoại hiện muộn còn hay gặp hơn. Quy tắc tự từ chối có giới hạn của plan này đề xuất ngược về mẫu sau khi merge.

  PR ghi vào mô tả: các điểm còn lại (`fresh` mở lại, quy tắc hộp thoại đồng ý, `selected` theo nút cha, chờ ổn định, `note`, ngoặc kép, tên tệp, ngân sách) đề xuất về mẫu (`docs/ops/from-app.md`).
- **Đề xuất thứ tự chạy:** độc lập. Nên chạy trong một phiên có thay đổi app (để `device-session` cuối phiên chạy và có kết quả ngay), hoặc chạy `device-session.yml` bằng tay sau khi merge.

## 7. Điều kiện dừng riêng

1. Cần sửa `.github/workflows/device-session.yml` (ví dụ đổi API của emulator, thêm camera sau, tăng `timeout-minutes`): dừng, viết plan riêng.
2. Một test có sẵn của khối `device-scenario`, `device-session` hay `plan-check` (quy tắc 11) phải sửa thì mới qua: dừng.
3. Không phải điều kiện dừng: ở `device-session` cuối chính phiên này hay lần đầu sau khi merge, kịch bản vẫn lỗi vì một dấu hiệu ở mục 3.4. Phiên chỉ ghi vào báo cáo, kèm các dòng `note`; crash và ANR vẫn chặn staging như cũ.

## 8. Câu hỏi cho người duyệt

Đã trả lời (Tuấn, 2026-10-24):

1. **Tiêu chí (5) của T-022.** Hộp thoại mạng hiện trên mọi màn 3 giây sau khi mất mạng (T-011) và chặn chạm, nên không mở được chi tiết sau khi đã mất mạng. **Trả lời:** mở chi tiết trước rồi mới tắt mạng (mục 3.1).
   - Kịch bản ghi rõ ảnh lớn có thể đã tải.
   - Trường hợp "ảnh lỗi vì mất mạng thì hộp thoại thay thông báo riêng" vẫn do unit test của T-022 kiểm (`PoseDetailViewModelTest.imageFailsNetwork_dialogShown_staysLoading`, `networkRecovered_reloadsFailedImage`).
2. **App khóa hướng dọc, màn mở gập bị đóng khung** (`screenOrientation="portrait"`; emulator API 34, 2 cột thay vì 3). **Trả lời:** ghi phản hồi riêng FB-20261024-01 (`status: new`, PR riêng, không có quyết định sản phẩm).
   - Kịch bản T-024 kiểm theo những gì emulator thật sự hiện, không kiểm số cột.
   - Ticket này không sửa app.
3. **T-020 trên CI và điều kiện đổi quy tắc 11.** Emulator CI không có camera sau (`-camera-back none`), nên T-020 thường `bỏ qua (môi trường)`. **Trả lời:** `bỏ qua (môi trường)` từ lệnh `need` tính là không lỗi khi xét đổi quy tắc 11.
4. **Tự từ chối hộp thoại đồng ý.** **Trả lời:** tự từ chối ở mọi bước đọc màn hình, với giới hạn của rà soát độc lập (mục 3.1):
   - chỉ khi bước `consent decline` thấy app mà chưa thấy hộp thoại;
   - tối đa một lần mỗi `fresh`;
   - hộp thoại hiện lại sau khi đã chạm từ chối là lỗi, để không che lỗi app không lưu lựa chọn đồng ý (ADR-0013);
   - có dòng ghi chú trong `summary.md`.

   Số đo: hộp thoại hiện khoảng 1,7 giây sau màn chào.

Không còn câu hỏi mở.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-24, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Câu trả lời mục 8: T-022 mở chi tiết trước rồi mới tắt mạng; khóa hướng dọc ghi phản hồi riêng (FB-20261024-01); T-020 bỏ qua có lý do tính là qua; tự từ chối hộp thoại đồng ý ở mọi bước. Plan qua 5 vòng rà soát độc lập, vòng cuối không còn điểm chặn.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261025-01, phiên bản plan 1

- **Kết quả:** đã merge (PR #129, commit merge `bb3b286`, 2026-10-25 09:04 UTC).
- **Nhánh, PR:** `claude/T-048-kich-ban-emulator-on-dinh`, https://github.com/VGL-Studio/camera-pose/pull/129. Commit: `da52176` (test), `bfd4839` (code), `0e1e099` (tài liệu, hai kịch bản).
- **Đã làm:** đúng mục 3.1 và 3.2. Bước 3 đến 5 giao cho một phiên con trong worktree `../task-T-048` (L-031).
  - `scripts/device-scenario.py`: `fresh` mở bằng `am start -W`, kiểm `pidof`, mở lại một lần, lỗi khi bộ đệm crash có app; `consent_state`, tự từ chối hộp thoại hiện muộn, `ConsentFail`; `cached_screen`; `selected=true` theo nút cha; chờ ổn định sau `device`; dòng `note`; ngoặc kép, số mơ hồ, `SAFE_NAME`, `ticket:`; `check` bỏ `src/test*`, `DEVICE_SCENARIO_ROOT`; ngân sách 900 giây gồm trả máy và `EVIDENCE_RESERVE`; SIGTERM.
  - `scripts/device-session.sh`: `timeout -k 30`, mặc định 900, mục "Ghi chú của trình chạy".
  - Kịch bản T-022 (khối (5) mới), T-024 (chú thích), `docs/ops/test-sessions.md` mục 5.
- **Test viết trước:** 32 test mới (30 trong khối `device-scenario`, 2 trong khối `device-session`), commit trước code. Trên code cũ: 29 thất bại đúng lý do, khớp bảng 4.1 (`29 script test(s) failed`); 3 test đối chứng qua ngay như plan ghi (không có bước đồng ý, nút chạm được chỉ xét chính nó, không quá 2 bậc). Không phải viết chặt thêm test nào (L-035). `scripts/tests/run.sh` chỉ thêm dòng (`158 0`), không test có sẵn nào đổi.
- **Rà soát độc lập:** 0 chặn, 1 nên sửa, 10 ghi chú. Phiên rà soát chạy lại bộ test trên code cũ và mới, kết quả khớp.
  - Nên sửa (không sửa): tài liệu `docs/ops/test-sessions.md` và câu báo lỗi của quy tắc số mơ hồ chỉ nói "giá trị có dấu cách", trong khi code báo lỗi cho mọi `text=`/`desc=` không ngoặc trên dòng kết thúc bằng một số, kể cả một từ (`wait desc=Back 10`). Đây là cách hiểu duy nhất làm test `wait text=Page 2` của plan thất bại đúng lý do. Chỉ đổi câu chữ, không viết được test thất bại trước, nên theo bước 6 ghi vào báo cáo kèm đề xuất.
  - Ghi chú đáng xem: tự từ chối trong bước dùng `dump_ready` có cả khung 5 giây, gồm lần dump đầu, nên trên máy chậm có thể lỗi `không đóng sau khi chạm từ chối`; `consent decline` thứ hai sau khi đã từ chối vẫn chạm lại mà không lỗi; `pidof` coi mọi chữ số trong đầu ra là có tiến trình; giá trị ngoặc một phần (`text="A" extra 5`) không bị báo lỗi cú pháp.
- **Kết quả CI:** `pr-check.yml` xanh lần đầu (lần chạy 40178581317, commit `0e1e099`). `scripts/check.sh` cục bộ qua (`Test JVM: 1050 test, 0 lỗi`, `All script tests passed`).
- **Lệch so với plan:** không đổi điều test kiểm. Chi tiết nhỏ:
  - Test "tiến trình bị giết thì mở lại" dò `pidof rỗng.*mở lại`, khớp cả dạng ở mục 3.1 (`(pidof rỗng), mở lại`) lẫn mục 4.
  - Chữ của dòng `note`: `hộp thoại đồng ý hiện muộn, đã từ chối (chạm x,y)`; `consent decline` thường ghi `đã chạm từ chối x,y` hay `chưa thấy hộp thoại đồng ý`; lần mở thứ hai thất bại có thêm `fresh: lần mở 2 không có tiến trình (...)`. Không dòng nào ngoài hai trường hợp bất thường lên `summary.md`.
  - `fake-adb`: khi có `FAKE_ADB_STATE_DIR` mà app chưa chạy thì `pidof` trả mã 1 như máy thật; không đặt biến thì giữ như cũ.
- **Ngoài mục 3.2:** không có.
- **Việc còn lại:** `device-session` cuối phiên này là lần chạy thật đầu tiên của trình chạy mới (mục 6); kết quả ghi ở báo cáo phiên. Đổi quy tắc 11 thành lỗi khi một `device-session` có cả 3 kịch bản không `lỗi` (mục 6).
- **Cần người xem:** điểm nên sửa về câu chữ của quy tắc số mơ hồ; các ghi chú của rà soát; kết quả "Kịch bản theo plan" và "Ghi chú của trình chạy" của `device-session` cuối phiên.
