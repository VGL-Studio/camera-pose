---
ticket: T-048
title: Trình chạy kịch bản emulator chạy ổn định trên CI (sửa sau lần device-session đầu tiên)
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 8M           # cỡ M (5.2M, scripts/token-usage.py estimate) cộng khoảng 50% vì có nhiều test với adb giả (T-046: 52 expect)
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-048: TRÌNH CHẠY KỊCH BẢN EMULATOR CHẠY ỔN ĐỊNH TRÊN CI

## 1. Tóm tắt

Lần `device-session` đầu tiên có trình chạy kịch bản của T-046 lỗi ở cả 3 tệp. Đã đọc artifact của hai lần chạy (40116129967 và 40117751068 của RUN-20261024-02): ảnh, dump, `scenarios.txt`, log của app và logcat hệ thống. Kết luận: 4 nguyên nhân, 3 nằm ở trình chạy, 1 nằm ở kịch bản; không lỗi nào là lỗi của app.

Plan sửa trong `scripts/device-scenario.py`:
- `fresh` mở app chắc chắn;
- từ chối hộp thoại đồng ý hiện muộn;
- `selected=true` đọc nút cha;
- chờ màn hình ổn định sau lệnh `device`;
- ghi dấu vết từng bước;
- sửa 5 điểm "nên sửa" của rà soát T-046.

Kịch bản T-022 đổi để mở chi tiết trước khi tắt mạng. `device-session.sh` chỉ đổi một dòng: `timeout -k`.

Kiểm chứng:
- test với `adb` giả, gồm cả bản tái hiện từng lỗi đã thấy (dump thật của chip);
- lần `device-session` đầu tiên sau khi merge.

Không sửa `.github/`, không sửa app.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ. Yêu cầu là kết quả `device-session` của RUN-20261024-02 (báo cáo, mục Kiểm thử trên máy ảo) và các điểm "nên sửa" của rà soát độc lập T-046 (báo cáo, Cần người kiểm tra).

| Lỗi đã thấy hay điểm rà soát | Tiêu chí chấp nhận | Cách hiểu của AI (bằng chứng) |
| --- | --- | --- |
| T-024, lần 2, bước 2: `consent decline 30 FAIL không thấy hộp thoại đồng ý hay màn hình đầu của app` | `fresh` chỉ xong khi tiến trình app đang chạy; tiến trình mới bị hệ thống giết thì mở lại một lần; vẫn không chạy thì `FAIL app không chạy sau 2 lần mở` | Logcat 18:08:58: `ActivityManager: ProcessRecord{... staging} start not valid, killing pid=10782 ... expected=101`, rồi `pid=10785`. Ứng dụng không ghi `VGL.Loading: Loading started`; `fail.png` là màn khởi động có biểu tượng. `fresh` hiện chạy `pm clear` rồi `am start` (không `-W`) ngay, trong lúc hệ thống còn dừng tiến trình cũ |
| T-020, lần 2, bước 3: `wait id=onboarding_screen \| id=camera_screen 60 FAIL` | Sau khi tệp đã có bước `consent decline`, mọi bước đọc màn hình gặp hộp thoại đồng ý thì chạm "Do not consent", ghi dòng `note`, rồi đọc lại; thời gian vẫn tính vào bước đó | `fail.xml` chỉ có cửa sổ WebView của hộp thoại (`fc-focus-trap-pre-div`), `fail.png` là hộp thoại UMP phủ lên màn chào. Bước 2 đã `ok` vì thấy `onboarding_screen` trước khi hộp thoại hiện |
| T-024, lần 1, bước 14: `wait str=poses_chip_saved selected=true 5 FAIL` | `selected=true` (và `checked`) khớp khi chính nút đó hay một nút cha (tới nút cha gần nhất có thể chạm, gồm cả nút đó) có `selected="true"` hay `checked="true"` | Dump thật: nút chữ "Tất cả" `selected="false"`, nút cha `View` `[288,321][475,447]` `selected="true"`. Chạy thử trên code hiện tại (mục 4.1): `str=poses_chip_all selected=true` không khớp, dù chip đó đang chọn |
| T-024, lần 1: sau `device unfold`, chạm chip "Đã lưu" mà chip không đổi | Sau `device unfold`, `fold`, `rotate`, `night`, `locale`, trình chạy chờ tới khi hai lần dump liên tiếp giống nhau, tối đa `DEVICE_SCENARIO_SETTLE` (mặc định 10 giây). Hết giờ không phải lỗi, chỉ ghi `note ... không ổn định` | Đổi `wm size` và `wm density` làm Activity dựng lại. Bản dump đọc ngay sau đó có thể là bố cục cũ, nên tọa độ chạm sai. Đây là giả thuyết: log không ghi tọa độ, nên mới thêm dòng `note` (dòng dưới) |
| Không biết bước chạm vào đâu, lúc nào | Mỗi bước ghi thêm một dòng `plan <tệp> note step=<n> t=<giây từ đầu tệp> <chi tiết>`. Chi tiết của `tap`: tọa độ và `resource-id` hay chữ của nút khớp; của `fresh`: số lần mở; của bước chờ: hộp thoại đồng ý đã từ chối; của lệnh `device`: thời gian chờ ổn định | Dạng dòng `step=` có sẵn giữ nguyên, để `cmd_summary` và test có sẵn không đổi. Dòng `note` không khớp mẫu `^plan [^ ]+ step=` |
| T-022, cả hai lần, bước 40: `wait id=pose_detail_image 30 FAIL` | Kịch bản mở chi tiết (`wait id=pose_detail_screen`) trước `network off`; hộp thoại mạng hiện trên chi tiết; bật mạng, "Thử lại": ảnh chi tiết hiện | App không ghi `VGL.Poses: Pose detail opened` sau bước 35. `fail.xml` là `poses_screen`; `offline.png` là hộp thoại mạng phủ lên thư viện. App ghi `VGL.Network: Dialog shown: device_offline delay=3s` (18:08:18), tức hộp thoại hiện trên mọi màn hình 3 giây sau khi mất mạng (T-011). Chạm thẻ lúc hộp thoại đã hiện thì chạm vào lớp phủ của hộp thoại. Kịch bản chưa từng kiểm chi tiết đã mở (câu hỏi 1, mục 8) |
| Rà soát T-046 (2): `check` quét cả `src/test` | `id=`, `idp=`, `in=` chỉ tìm trong `*.kt` của `src/main` (thư mục `src/` có tên bắt đầu bằng `test` hay `androidTest` bị bỏ); tag chỉ còn trong test thì `check` báo lỗi | `source_files` hiện gồm `app/src/test/.../MainScreenTest.kt` (mục 4.1) |
| Rà soát T-046 (3): ngân sách không tính thời gian trả máy về gốc | Trả máy về gốc có hạn tổng `DEVICE_SCENARIO_RESET_LIMIT` (mặc định 30 giây, cộng 45 giây cho bật lại mạng khi tệp có `network`), và hạn của mỗi tệp chừa phần đó: `min(timeout, ngân sách còn lại - hạn trả máy)`. Cả lượt kết thúc trước ngân sách cộng một lần trả máy | Hiện mỗi lệnh trả máy có 10 giây riêng (6 lệnh) cộng 45 giây cho mạng, ngoài ngân sách: tới 105 giây mỗi tệp |
| Rà soát T-046 (4): `timeout` chỉ gửi SIGTERM | `device-session.sh` gọi `timeout -k 30 ...`. Trình chạy bắt SIGTERM: giết nhóm tiến trình `adb` đang chạy, ghi `plan runner_terminated`, thoát mã 143 | `run_process` đã giết cả nhóm khi một lệnh hết giờ, nhưng SIGTERM từ ngoài giết Python mà không giết `adb` con (`start_new_session=True` tách nhóm) |
| Rà soát T-046 (5a): tên tệp không được làm sạch | Tên tệp phải khớp `[A-Za-z0-9][A-Za-z0-9._-]*\.txt`. Tên khác: `run` ghi `result=lỗi tệp` (tên trong log thay ký tự lạ bằng `_`), `check` báo lỗi. Tệp có tên bắt đầu bằng `T-NNN-` mà `ticket:` khác thì `check` báo lỗi | Tên tệp vào thẳng dòng log, đường dẫn ảnh và bảng Markdown của `summary.md` |
| Rà soát T-046 (5b): số cuối dòng bị tách thành số giây (`wait text=Page 2`) | Giá trị `text=` và `desc=` đặt được trong ngoặc kép (`text="Page 2"`). Giá trị không có ngoặc, có dấu cách, mà dòng kết thúc bằng một số: `check` báo "mơ hồ, ghi giá trị trong ngoặc kép" | Chạy thử: `split_number("text=Page 2")` trả `('text=Page', 2)` |
| Rà soát T-046 (1): kịch bản chưa kiểm hết tiêu chí của ticket | Không làm ở đây | Đúng như plan T-046 mục 3.1. Thêm kiểm tra cho từng tiêu chí sau khi 3 tệp chạy ổn định |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0013 (hộp thoại đồng ý khi mới cài), ADR-0019 (log), ADR-0012 (cấu hình staging giả).

## 3. Giải pháp

### 3.1. Cách làm

Mọi thay đổi theo mẫu có sẵn của `scripts/device-scenario.py`:
- lớp `Runner`, mỗi lệnh một hàm `step_<lệnh>`;
- `run_process` có hạn chót;
- `adb` giả `scripts/tests/fake-adb`.

Dạng các dòng `step=` và `result=` trong `scenarios.txt` không đổi.

**`fresh`** (thứ tự giống kịch bản chuẩn của `device-session.sh`, dòng 201 đến 228):
1. `am force-stop <gói>`, rồi `pm clear <gói>`.
2. Chờ tới khi `pidof <gói>` không in gì, tối đa 10 giây, đọc mỗi `DEVICE_SCENARIO_POLL` giây.
3. Cấp quyền (`helper grant-permissions`, như hiện nay).
4. Mở app bằng `am start -W -n <activity>`.
5. Chờ `pidof <gói>` in một số, tối đa 5 giây.
6. Không có tiến trình: ghi `note`, `am force-stop`, rồi làm lại bước 4 và 5 một lần.
7. Vẫn không có: `FAIL app không chạy sau 2 lần mở`.

Mọi lệnh nằm trong hạn của tệp.

**Hộp thoại đồng ý hiện muộn.**
- Mỗi tệp có cờ `consent_seen`, bật khi bước `consent decline` chạy xong, dù đã chạm nút từ chối hay không.
- Khi cờ bật, mọi bước đọc màn hình (`wait_for` và `dump_ready`, nên gồm `wait`, `need`, `tap`, `tap_if`, `scroll`, `expect`, `expect_no`, `count`) làm như sau nếu bản dump có nút khớp `CONSENT_DECLINE` mà không có phần tử bước đó tìm:
  - chạm nút từ chối;
  - ghi `note step=<n> t=<s> hộp thoại đồng ý hiện muộn, đã từ chối`;
  - đọc lại màn hình, trong hạn của bước.
- Tệp không có bước `consent decline` thì không tự chạm, để kịch bản kiểm chính hộp thoại vẫn làm được.

**Bộ lọc `selected=true`.**
- Một nút khớp khi chính nó, hay một nút cha của nó, có `selected="true"` hoặc `checked="true"`. Nút cha được xét lần lượt từ dưới lên, tới nút cha đầu tiên có `clickable="true"` (gồm cả nút đó) và dừng ở đó.
- `Screen` giữ bảng nút cha khi đọc dump (`ElementTree` không có con trỏ cha).
- Bộ lọc `clickable=true` không đổi.

**Chờ ổn định sau lệnh `device`.**
- Sau khi gửi lệnh, đọc dump tới khi hai bản liên tiếp có chữ XML giống nhau, tối đa `DEVICE_SCENARIO_SETTLE` giây (mặc định 10), trong hạn của tệp.
- Dump lỗi `could not get idle state` thì đọc lại.
- Ghi `note step=<n> t=<s> ổn định sau <x> giây` hay `không ổn định sau <x> giây`. Không ổn định không phải lỗi: bước sau tự chờ phần tử của nó.

**Dòng `note`.**
- Dạng: `plan <tệp> note step=<n> t=<giây từ đầu tệp, một chữ số thập phân> <chi tiết>`, ghi trước dòng `step=` của cùng bước.
- `tap`, `tap_if` (khi có chạm), `scroll` ghi tọa độ và nhãn của nút: `resource-id`, nếu rỗng thì `text`, `content-desc`, cắt còn 40 ký tự.
- Các bước khác chỉ ghi `t=`.

**Ngoặc kép và số cuối dòng.**
- `SELECTOR` nhận giá trị `"..."` (không có `"` bên trong) cho mọi loại. Giá trị trong ngoặc giữ nguyên dấu cách; số sau dấu ngoặc đóng là số giây hay số lần.
- `check` báo lỗi ở dòng có `text=` hay `desc=` không ngoặc, giá trị có dấu cách, và `split_number` tách được một số ở cuối.
- `run` vẫn đọc như hiện nay, vì tệp qua `check` mới tới được `run`. Ba kịch bản có sẵn không có dòng nào như vậy.

**`check` chỉ đọc mã chính.**
- Hàm `source_files(root, "*.kt")` của `check` bỏ đường dẫn có một thư mục ngay dưới `src/` mà tên bắt đầu bằng `test` hay `androidTest`.
- `strings.xml` đã chỉ đọc `res/values/` của mọi nguồn; giữ nguyên.
- Gốc tìm kiếm lấy từ `DEVICE_SCENARIO_ROOT` nếu đặt (cho test), không thì là gốc repo như hiện nay.
- Chữ ký `check_files(paths, root=ROOT)` không đổi, vì `plan-check.py` (quy tắc 11) gọi hàm này.

**Tên tệp.**
- Có hằng `SAFE_NAME = r"[A-Za-z0-9][A-Za-z0-9._-]*\.txt"`.
- `run_file`: tên không khớp thì ghi `plan <tên đã thay ký tự lạ bằng _> step=0 check FAIL tên tệp chỉ gồm chữ, số, . _ -` và `result=lỗi tệp`, không gửi lệnh `adb` nào cho tệp đó.
- `check` báo cùng lỗi đó, và báo khi tên bắt đầu bằng `T-<số>-` mà dòng `ticket:` khác.

**Ngân sách gồm thời gian trả máy.**
- `reset_device` có một hạn tổng: `DEVICE_SCENARIO_RESET_LIMIT` (mặc định 30 giây). Mỗi lệnh lấy `min(5, phần còn lại)`. Bật lại mạng thêm tối đa 45 giây, chỉ khi tệp có lệnh `network`.
- Hạn của tệp là `min(timeout, ngân sách còn lại - hạn trả máy của tệp đó)`. Phần còn lại không dương thì tệp ghi `bỏ qua (hết thời gian)`.
- Lần trả máy cuối lượt dùng cùng hạn tổng.
- Vì vậy, lần chạy với lệnh `adb` treo kết thúc trong khoảng ngân sách cộng một lần trả máy.

**SIGTERM.**
- `signal.signal(SIGTERM, ...)` đổi tín hiệu thành ngoại lệ `Terminated` (lớp con của `BaseException`).
- `run_process` bắt mọi `BaseException` trong lúc chờ, giết nhóm tiến trình con (`os.killpg(..., SIGKILL)`), rồi ném tiếp.
- `main` bắt `Terminated`: ghi `plan runner_terminated` vào `scenarios.txt`, trả máy về gốc với hạn 10 giây, thoát mã 143.
- `device-session.sh` dòng 252: `timeout -k 30 "$((... + 120))" ...`. Mã 124 hay 143 vẫn đi vào nhánh `plan runner_error` có sẵn.

**Kịch bản T-022** (`feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`). Thay khối từ `network off` tới `wait id=pose_detail_image 30` bằng:

```text
# (5) đang ở chi tiết của tư thế thứ ba thì mất mạng: hộp thoại mạng; bật mạng, "Thử lại": ảnh hiện
tap idp=poses_card_ in=poses_grid index=3
wait id=pose_detail_screen 15
network off
wait id=network_dialog 30
shot offline
network on
tap id=network_dialog_retry
wait id=pose_detail_image 30
```

Các dòng (6) và (7) sau đó giữ nguyên. Kịch bản T-020 và T-024 không đổi: các lỗi của chúng là lỗi của trình chạy.

**Tài liệu.** `docs/ops/test-sessions.md` mục 5 ghi:
- `fresh` chờ tiến trình app;
- hộp thoại đồng ý hiện muộn được tự từ chối sau bước `consent decline`;
- `selected=true` đọc nút cha;
- chờ ổn định sau lệnh `device`;
- ngoặc kép;
- dòng `note`;
- quy ước tên tệp;
- ngân sách gồm thời gian trả máy, và các biến `DEVICE_SCENARIO_SETTLE`, `DEVICE_SCENARIO_RESET_LIMIT`, `DEVICE_SCENARIO_ROOT`.

Docstring đầu `device-scenario.py` cập nhật cùng nội dung.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/device-scenario.py` | `fresh` (`force-stop`, chờ `pidof` rỗng, `am start -W`, chờ `pidof`, mở lại một lần); cờ `consent_seen` và tự từ chối trong `wait_for`, `dump_ready`; `selected=true` theo nút cha tới nút cha có thể chạm đầu tiên (bảng cha trong `Screen`); chờ ổn định sau `device`; dòng `note`; ngoặc kép trong `SELECTOR`, `check` báo số cuối dòng mơ hồ; `check` bỏ `src/test*`, `src/androidTest*`, `DEVICE_SCENARIO_ROOT`; `SAFE_NAME`, `ticket:` khớp tên; hạn tổng của `reset_device` và hạn tệp chừa phần đó; `Terminated`, `run_process` giết nhóm con khi bị ngắt; docstring |
| Sửa | `scripts/device-session.sh` | Dòng 252: `timeout -k 30`; chú thích khối biến môi trường (dòng 30 đến 33) thêm `DEVICE_SCENARIO_SETTLE`, `DEVICE_SCENARIO_RESET_LIMIT` |
| Sửa | `scripts/tests/fake-adb` | Giữ mọi hành vi có sẵn. Thêm: `pidof <gói>` in `4242` khi tệp `$FAKE_ADB_STATE_DIR/alive` có; `am start -W` tăng `$FAKE_ADB_STATE_DIR/starts.n` và tạo `alive` khi `starts.n` lớn hơn `$FAKE_ADB_DEAD_STARTS` (mặc định 0); `pm clear`, `am force-stop` xóa `alive`; nhánh `$FAKE_ADB_HANG` ghi `$$` vào `$FAKE_ADB_STATE_DIR/hang.pid` trước `exec sleep 300`; chú thích đầu tệp |
| Tạo | `scripts/tests/fixtures/device-scenario/chips.xml` | Dump theo cấu trúc dump thật của T-024: hai chip, mỗi chip là `View` có thể chạm bọc một `TextView`; chip "Tất cả" có `selected="true"` ở nút cha, chip "Đã lưu" không |
| Tạo | `scripts/tests/fixtures/device-scenario/chips-saved.xml` | Như `chips.xml` nhưng chip "Đã lưu" được chọn (đối chứng) |
| Sửa | `scripts/tests/run.sh` | Khối `device-scenario` (sau dòng `an empty glob exits before any adb call`): test của mục 4; tệp kịch bản tạm ghi vào `$work`, không thêm fixture tệp kịch bản; khối `device-session`: test `timeout -k` |
| Sửa | `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt` | Khối (5): mở chi tiết trước `network off` (mục 3.1) |
| Sửa | `docs/ops/test-sessions.md` | Mục 5: các điểm ở cuối mục 3.1 |

**Nơi khác đọc các file plan này sửa (L-020), và vì sao không bị ảnh hưởng:**
- **`scripts/plan-check.py`** nạp `scripts/device-scenario.py` (`scenario_runner()`) và gọi `check_files`. Chữ ký và dạng thông báo `<tệp> dòng <n>: <lý do>` giữ nguyên. Lỗi mới (tên tệp, số mơ hồ, tag chỉ có trong test) chỉ hiện ở quy tắc 11, vẫn là chú ý. Test có sẵn của quy tắc 11 dùng tệp `T-940-good.txt` (tên hợp lệ, `ticket: T-940`) và `T-941-bad.txt`: cần kiểm `ticket:` của hai tệp đó khớp tên. Nếu không khớp, thông báo thêm chỉ làm tăng số dòng chú ý; test đếm `quy tắc 11.*T-941-bad.txt dòng 4` vẫn đúng. Bước 3 chạy lại khối test này.
- **`.github/workflows/device-session.yml`** gọi `scripts/device-session.sh` (`start ci`, `scenarios --yes`, `stop`) và đọc `summary.md`. Lệnh và tên tệp không đổi. Workflow có `timeout-minutes: 50`; ngân sách 600 giây cộng 120 giây cộng 30 giây không đổi bậc.
- **`scripts/device-session.sh` `cmd_summary`** đọc các dòng `^plan [^ ]+ step=[0-9]+ .* FAIL` và `^plan [^ ]+ result=`. Dòng `note` có chữ `note` ngay sau tên tệp, nên không khớp hai mẫu đó. Dòng `plan runner_terminated` không khớp mẫu `result=`, giống dòng `plan runner_error` có sẵn.
- **`scripts/tests/fake-adb`** cũng được test của `scripts/app-explore/` và của kịch bản chuẩn `device-session` dùng. Hành vi mới chỉ thêm lệnh (`pidof`) hoặc thêm tác dụng phụ trong `$FAKE_ADB_STATE_DIR` (`starts.n`, `alive`, `hang.pid`). Đầu ra của `am start -W`, `pm clear`, `am force-stop` không đổi.
- **`app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ui/UiAutomatorTagsTest.kt`** nhắc `scripts/device-scenario.py` trong chú thích (lý do bật `testTagsAsResourceId`), không chạy nó. Plan không đổi cách đọc `resource-id`.
- **`scripts/tests/run.sh`** được `scripts/check.sh` chạy (cả tệp), và được `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/wait-run.sh` nhắc trong chú thích. Plan chỉ thêm test vào khối `device-scenario`, `device-session` và một dòng mẫu `note`; cách gọi và dòng tổng kết không đổi.
- **`docs/ops/test-sessions.md`** được `docs/CLAUDE.md` và `docs/templates/plan.md` dẫn tới "mục 5". Tên mục giữ nguyên.
- **`feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`** được đọc bởi test `the scenarios of the repository pass check`, chạy lại ở bước 5. Plan T-022 và T-046 nhắc tên tệp này nhưng không đọc nội dung.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm. Trình chạy chỉ chạm nút từ chối của hộp thoại đồng ý có sẵn (ADR-0013), trên emulator, như T-046.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `fresh` vẫn cấp quyền runtime đã khai báo qua `helper grant-permissions`.
- **Thư viện và SDK:** Không thêm. Trình chạy dùng Python chuẩn (`signal`, `subprocess`, `xml.etree`).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** chỉ biết ở `device-session` đầu tiên sau khi merge:
  - `pidof` có trên emulator API 34 (`toybox pidof`, có từ Android 7);
  - hai bản dump liên tiếp của màn đứng yên có giống nhau từng ký tự;
  - chip "Đã lưu" có đổi sau khi màn đã ổn định.
- **Điều kiện chuyển:** báo cáo của lần chạy đầu cho thấy một trong các dấu hiệu sau:
  - `fresh` lỗi `app không chạy` trong khi `fail.png` là màn của app;
  - mọi dòng `note` của lệnh `device` ghi `không ổn định`.

  Phiên không dừng vì điều này (mục 7, điều 3).
- **Cách làm thay:** sửa ở một task sau.
  - Đọc tiến trình bằng `ps -A -o NAME` thay cho `pidof`.
  - So dump sau khi bỏ thuộc tính `bounds` của nút có hiệu ứng, hoặc chỉ chờ `wm size` báo kích thước mới rồi đợi 2 giây.

  Tiêu chí và test của mục 4 giữ nguyên. Plan này không cài sẵn phần dự phòng.

## 4. Kiểm thử

Test viết trước code: mọi dòng "Test script" dưới đây, trong khối `device-scenario` của `scripts/tests/run.sh`, chạy với `adb` giả. Bước 3 (khung rỗng) chỉ thêm `fixtures` và các nhánh mới của `fake-adb`, không đổi `device-scenario.py`, nên các test mới thất bại vì hành vi cũ.

Mỗi test kèm lý do thất bại dự kiến trên code hiện tại:
- dòng `fresh`: không có `am start -W`, không đọc `pidof`;
- dòng `selected`: không khớp nút cha, đã đo ở mục 4.1;
- dòng `note`: không có dòng nào;
- dòng ngoặc kép: lỗi cú pháp.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| `fresh` chờ và mở bằng `-W` | Test script | `device-scenario: fresh stops the app, waits for it to be gone and starts it with am start -W`: `$FAKE_ADB_CALLS` có, theo thứ tự, `shell am force-stop`, `shell pm clear`, `shell pidof com.example.app.staging`, `shell am start -W -n com.example.app/.MainActivity`; bước `ok` |
| Tiến trình bị giết thì mở lại | Test script | `device-scenario: fresh starts the app again when the first start left no process`: `FAKE_ADB_DEAD_STARTS=1`: hai lệnh `am start -W`, bước `ok`, có dòng `note step=1` nhắc lần mở thứ 2 |
| Không mở được | Test script | `device-scenario: fresh fails when the app never runs`: `FAKE_ADB_DEAD_STARTS=5`: `step=1 fresh FAIL app không chạy sau 2 lần mở`, đúng hai lệnh `am start -W` |
| Đồng ý hiện muộn | Test script | `device-scenario: a consent form that shows after consent decline is declined by the next step`: tệp `consent decline 5`, `wait id=onboarding_screen 10`, `expect id=onboarding_screen`; chuỗi dump `onboarding.xml consent.xml onboarding.xml`: cả tệp `đạt`, một lệnh chạm tâm nút `Do not consent`, có dòng `note` "hiện muộn" |
| Đối chứng: không có bước đồng ý | Test script | `device-scenario: without consent decline a consent form is not tapped`: cùng chuỗi, tệp chỉ có `wait id=onboarding_screen 10` và `expect id=onboarding_screen`: không có lệnh `input tap` nào; `expect` lỗi trên `consent.xml` |
| `selected=true` theo nút cha | Test script | `device-scenario: selected=true matches a text whose clickable parent is selected`: `chips.xml`, `expect str=poses_chip_all selected=true` đạt |
| Đối chứng nút cha | Test script | `device-scenario: selected=true does not match a chip whose parent is not selected`: `chips.xml`, `expect str=poses_chip_saved selected=true` lỗi; `chips-saved.xml` thì đạt (L-035) |
| `selected=true` có sẵn | Test script | Hai test có sẵn `selected=true matches selected or checked` và `does not match an unselected chip` giữ nguyên, phải còn qua |
| Ổn định sau `device` | Test script | `device-scenario: a device command waits until two dumps agree`: tệp `device unfold`, `expect id=poses_grid`; chuỗi `camera.xml grid.xml grid.xml`: đạt, `ui-seq.n` là 3 sau bước 1 (ba lần dump); có `note step=1 ... ổn định` |
| Không ổn định không phải lỗi | Test script | `device-scenario: a screen that never settles is noted, not failed`: `DEVICE_SCENARIO_SETTLE=2`, `DEVICE_SCENARIO_POLL=0.2`, chuỗi xen kẽ `camera.xml grid.xml camera.xml grid.xml ...` (20 tệp): bước 1 `ok`, dòng `note` có `không ổn định` |
| Dòng `note` của chạm | Test script | `device-scenario: a tap notes its coordinates and the element`: `ok.txt` có sẵn: dòng `note step=<n> t=` có `540,1806` và `onboarding_enable_camera`; số dòng `^plan ok.txt step=[0-9]* .* ok$` vẫn là 6 (test có sẵn không đổi) |
| `summary.md` không đổi | Test script | Test có sẵn `device-session: summary lists the plan scenarios` chạy với `scenarios.txt` có thêm dòng `note`: bảng như cũ (thêm dòng `note` vào mẫu ở dòng 2010 đến 2017 của `run.sh`) |
| Ngoặc kép | Test script | `device-scenario: a quoted value keeps its spaces and the number after it is the timeout`: tệp tạm `wait text="Hands in Pockets" 3` trên `grid.xml` có thẻ chữ đó: đạt; `check` qua |
| Số cuối dòng mơ hồ | Test script | `device-scenario: check reports a text with spaces followed by a number`: `wait text=Page 2`: `check` mã 1, dòng `dòng 4` nhắc "ngoặc kép". Đối chứng: `wait id=poses_grid 2` qua |
| `check` không đọc test | Test script | `device-scenario: check ignores tags that only tests have`: `DEVICE_SCENARIO_ROOT` trỏ cây tạm `feature/x/src/main/A.kt` (có `"main_tag"`) và `feature/x/src/test/B.kt` (có `"test_only_tag"`): tệp có `expect id=test_only_tag` thì `check` mã 1; `expect id=main_tag` thì mã 0 |
| Tên tệp lạ | Test script | `device-scenario: a file name with odd characters is a file error and sends no adb command`: tệp `$work/ds-names/a b|c.txt` (nội dung đúng): `scenarios.txt` có `plan a_b_c.txt step=0 check FAIL tên tệp` và `result=lỗi tệp`; `$FAKE_ADB_CALLS` không có lệnh nào cho tệp đó (không `log -t`); `check` mã 1 |
| `ticket:` khớp tên | Test script | `device-scenario: check reports a T-NNN file whose ticket differs`: `T-901-x.txt` có `ticket: T-902`: mã 1. Đối chứng: `ok.txt` (không bắt đầu bằng `T-`) qua |
| Ngân sách gồm trả máy | Test script | `device-scenario: the reset time is taken from the budget`: `FAKE_ADB_HANG="wm"` (trả máy treo), `DEVICE_SCENARIO_RESET_LIMIT=2`, `DEVICE_SCENARIOS_BUDGET=6`, một tệp `timeout: 300` có `wait id=absent 30` trên `blank.xml`: cả lượt xong dưới 13 giây; dòng `result=lỗi`. Trên code hiện tại, mỗi lệnh `wm` trả máy chờ 10 giây, nên lượt kéo dài hơn 30 giây |
| SIGTERM | Test script | `device-scenario: a terminated runner kills its adb child and says so`: `FAKE_ADB_HANG=uiautomator`, `DEVICE_SCENARIO_ADB_TIMEOUT=100`, chạy trình chạy ở nền, gửi `kill -TERM` sau 2 giây, chờ nó thoát: mã 143; `scenarios.txt` có `plan runner_terminated`; `kill -0 "$(cat <state>/hang.pid)"` thất bại (tiến trình `sleep` đã chết). Không dùng `pgrep -f` (L-067) |
| `timeout -k` | Test script | `device-session: the runner is called with timeout -k`: `grep -c 'timeout -k 30' scripts/device-session.sh` là 1 |
| Kịch bản của repo | Test script | Test có sẵn `device-scenario: the scenarios of the repository pass check`, chạy với `check` mới |
| Quy tắc 11 không đổi | Test script | Các test có sẵn `plan-check: ... rule 11` (dòng 1036 đến 1077 của `run.sh`) phải còn qua |
| Ba kịch bản chạy thật | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`, `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`, `feature/camera/src/deviceScenarios/T-020-camera-mo-gap.txt`) | Lần `device-session` đầu tiên sau khi merge. Phiên đọc bảng "Kịch bản theo plan" và các dòng `note`, mở `fail.png` của tệp lỗi, ghi vào báo cáo lỗi đó của app hay của kịch bản. T-020 trên CI có thể vẫn `bỏ qua (môi trường)` vì không có camera sau (câu hỏi 3) |
| Phiên làm theo tài liệu | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt chéo đọc diff `docs/ops/test-sessions.md` mục 5 |

**Ngoại lệ viết test trước:**
- `docs/ops/test-sessions.md` là tài liệu.
- Tệp kịch bản T-022 là test chạy trên emulator. Phiên chỉ kiểm được bằng `check`; chạy thật ở `device-session` cuối phiên.
- Dòng `timeout -k` của `device-session.sh` chỉ có test `grep`, vì môi trường test không có emulator và lệnh `timeout` có thể thiếu trên macOS.

### 4.1. Chạy thử

Không cần (E dưới 2). Plan sửa lỗi đã đo, nên dưới đây ghi số đo thật trên code hiện tại (L-029):

- **Ngày 2026-10-24, nhánh chính `e3f02b8`, máy dev.** Lệnh Python tạm (không commit) nạp `scripts/device-scenario.py`, dựng `Screen` từ `fail.xml` thật của T-024 (lần chạy 40116129967, artifact `device-session`):
  - `str=poses_chip_saved selected=true` trả `None`;
  - `str=poses_chip_all selected=true` cũng trả `None`, dù chip "Tất cả" đang chọn, vì `selected="true"` nằm ở nút cha;
  - `str=poses_chip_saved` (không lọc) tìm thấy nút.
- **`split_number("text=Page 2")`** trả `('text=Page', 2)`: số bị đọc thành số giây.
- **`source_files(ROOT, "*.kt")`** trả cả `app/src/test/kotlin/.../navigation/MainScreenTest.kt`.
- **Logcat và log app của lần chạy 40117751068:**
  - `start not valid, killing pid=10782` và `pid=10785` lúc 18:08:58, ngay sau `pm clear`;
  - không có `VGL.Loading` nào của T-024 (18:08:56 đến 18:09:30);
  - `VGL.Network: Dialog shown: device_offline delay=3s` lúc 18:08:18 và không có `Pose detail opened` sau bước 35 của T-022.
- **Ảnh `fail.png` của T-024 lần 1:** sau `wm size 1766x2208`, app bị đóng khung ở giữa (`[246..1521]`), lưới 2 cột (câu hỏi 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI và quy trình phiên tự chạy): plan đổi `scripts/device-session.sh`, mà `device-session.yml` chạy cuối mỗi phiên, và trình chạy mà nó gọi.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script sửa nhiều chỗ, một dòng của `device-session.sh`, `fake-adb`, hai fixture, một kịch bản, một tài liệu |
| B. Độ rõ của yêu cầu | 1 | Mỗi lỗi có bằng chứng trong artifact; còn câu hỏi 1 về ý của tiêu chí T-022 (5) |
| C. Khả năng kiểm chứng tự động | 2 | Mọi thay đổi của trình chạy có test với `adb` giả; phần trên emulator thật chỉ kiểm được sau khi merge |
| D. Ảnh hưởng chéo | 1 | `plan-check.py` dùng `check_files` (chữ ký giữ nguyên); `fake-adb` dùng chung với test kịch bản chuẩn và `app-explore` |
| E. Độ mới của kỹ thuật | 1 | Mọi kỹ thuật đã có trong repo (`am start -W`, `pidof` qua `adb`, `killpg`); chỉ đổi cách dùng |
| **Tổng** | 7 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:**
  - `scripts/device-scenario.py` (dùng ở `device-session` cuối mỗi phiên và ở `plan-check.py` quy tắc 11);
  - `scripts/device-session.sh` (máy dev và CI);
  - `scripts/tests/fake-adb`, `scripts/tests/run.sh`.
- **Task khác có thể xung đột:** chưa có ticket nào khác `ready` (2026-10-24). Danh sách chạy có thêm plan sửa `scripts/tests/run.sh` thì mỗi plan thêm khối riêng; task chạy lần lượt, mỗi task tạo nhánh từ nhánh chính mới nhất.
- **Việc sau task này, không làm ở đây:**
  - **Quy tắc 11 thành lỗi.** Khi một `device-session` có cả 3 kịch bản không `lỗi`, một dòng sửa ở `scripts/plan-check.py` đổi quy tắc 11 thành lỗi với plan `draft`, kèm test (plan T-046, mục 6). Báo cáo phiên ghi vào Đề xuất cho buổi họp tiếp theo. Có tính `bỏ qua (môi trường)` là không lỗi không: câu hỏi 3.
  - **Kiểm thêm tiêu chí.** Thêm kiểm tra cho từng tiêu chí còn thiếu của T-020, T-022, T-024 (rà soát T-046, điểm 1).
  - **Dự án mẫu.** PR VGL-Studio/app-project-template#152 (chuyển T-046 về mẫu, issue #133) đã tự sửa bốn điểm. Plan này lấy lại theo cùng cách:
    - (a) `check` bỏ tag chỉ có dưới thư mục test: cùng quy tắc `src/test*`, `src/androidTest*` như hàm kiểm thư mục test của mẫu;
    - (b) `timeout -k 30`: dùng đúng số 30 của mẫu;
    - (c) `fresh` mở app qua `helper launcher-activity` (`cmd package query-activities`, chọn `MainActivity`), vì bản staging của mẫu có hai activity LAUNCHER nên `resolve-activity` mở hộp chọn của hệ thống. App này chỉ có một LAUNCHER (`MainActivity`), và `resolve-activity` đã chạy đúng ở cả hai lần `device-session`, nên plan không chép phần này; app thêm activity LAUNCHER thứ hai thì chép;
    - (d) `consent decline` chờ tag gốc `app_root` (`DEVICE_SCENARIO_FIRST_SCREENS`): không chép, vì app chưa có tag `app_root`, và tag gốc hiện cả ở màn tải, trước khi hộp thoại đồng ý kịp hiện, nên hộp thoại hiện muộn còn hay gặp hơn. Plan dùng cách tự từ chối ở mọi bước (mục 3.1), nên đề xuất ngược về mẫu sau khi merge.

  PR của plan này ghi vào mô tả: các điểm còn lại (`fresh` chờ tiến trình, tự từ chối, `selected` theo nút cha, chờ ổn định, `note`, ngoặc kép, tên tệp, ngân sách) đề xuất về mẫu (`docs/ops/from-app.md`).
- **Đề xuất thứ tự chạy:** độc lập. Nên chạy trong một phiên có thay đổi app (để `device-session` cuối phiên chạy và có kết quả ngay), hoặc chạy `device-session.yml` bằng tay sau khi merge.

## 7. Điều kiện dừng riêng

1. Cần sửa `.github/workflows/device-session.yml` (ví dụ đổi API của emulator, thêm camera sau): dừng, viết plan riêng.
2. Một test có sẵn của khối `device-scenario`, `device-session` hay `plan-check` (quy tắc 11) phải sửa thì mới qua: dừng, trừ dòng mẫu `scenarios.txt` ở dòng 2010 đến 2017 mà mục 4 cho phép thêm dòng `note`.
3. Không phải điều kiện dừng: ở `device-session` cuối chính phiên này hay lần đầu sau khi merge, kịch bản vẫn lỗi vì một dấu hiệu ở mục 3.4. Phiên chỉ ghi vào báo cáo, kèm các dòng `note`; crash và ANR vẫn chặn staging như cũ.

## 8. Câu hỏi cho người duyệt

1. **Tiêu chí (5) của T-022.** Tiêu chí là "chế độ máy bay, mở chi tiết tư thế chưa tải ảnh lớn: hộp thoại mạng". Trên app thật, hộp thoại mạng hiện trên mọi màn 3 giây sau khi mất mạng (T-011) và chặn chạm. Vì vậy sau khi tắt mạng, người hay kịch bản chỉ mở được chi tiết nếu kịp chạm trong 3 giây.
   - Đề xuất của AI: kịch bản mở chi tiết trước rồi mới tắt mạng (mục 3.1).
   - Kịch bản khi đó kiểm: hộp thoại hiện trên chi tiết, và "Thử lại" sau khi có mạng làm ảnh hiện.
   - Trường hợp "ảnh lỗi vì mất mạng thì hộp thoại thay thông báo riêng" vẫn do unit test của T-022 kiểm (`PoseDetailViewModelTest.imageFailsNetwork_dialogShown_staysLoading`, `networkRecovered_reloadsFailedImage`), vì emulator không bảo đảm được ảnh chưa có trong bộ nhớ đệm.
   - Nếu tiêu chí cần đúng "mở chi tiết khi đã mất mạng", đó là câu hỏi sản phẩm cho T-011, T-022, không phải lỗi của trình chạy.
2. **App khóa hướng dọc, màn mở gập bị đóng khung (phát hiện, ngoài phạm vi).**
   - `AndroidManifest.xml` có `android:screenOrientation="portrait"`.
   - Trên emulator API 34, sau `wm size 1766x2208` (673 × 841 dp), Android đặt app vào một khung rộng khoảng 486 dp ở giữa màn. Lưới thư viện chỉ có 2 cột, trong khi T-024 cần 3 cột khi mở gập (`fail.png` của T-024, lần 1).
   - Từ Android 16 (API 36), hệ thống bỏ qua khóa hướng trên màn từ 600 dp, nên máy gập mới sẽ không bị. Máy gập chạy Android 14, 15 thì có thể bị, tùy hãng.
   - Đề xuất của AI: ghi phản hồi `FB-...` mức `nen-sua` cho T-024/T-020 và để người quyết định có bỏ khóa hướng trên màn lớn hay không. Ticket này không sửa app.
3. **T-020 trên CI và điều kiện đổi quy tắc 11.**
   - Emulator CI không có camera sau (`-camera-back none`), nên T-020 thường kết thúc `bỏ qua (môi trường)` ở `need id=camera_grid_button`.
   - Đề xuất của AI: điều kiện "cả 3 kịch bản đạt" của plan T-046 hiểu là "không tệp nào `lỗi`"; `bỏ qua (môi trường)` từ lệnh `need` không tính là lỗi.
   - Thêm camera sau cho emulator cần sửa `device-session.yml`, ticket riêng.
4. **Tự từ chối hộp thoại đồng ý ở mọi bước, sau bước `consent decline`.**
   - Đề xuất của AI: làm như mục 3.1, có dòng `note` mỗi lần.
   - Cách khác: chỉ cho `consent decline` chờ thêm vài giây sau khi thấy màn đầu. Cách này rẻ hơn, nhưng không đỡ được hộp thoại hiện muộn hơn khoảng chờ đó. Ở T-020 lần 2, hộp thoại hiện sau khi màn chào đã hiện, chưa đo được muộn bao lâu.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
