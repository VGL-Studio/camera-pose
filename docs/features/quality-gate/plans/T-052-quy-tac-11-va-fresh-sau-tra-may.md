---
ticket: T-052
title: Quy tắc 11 của plan-check thành lỗi; `fresh` sau khi trả máy không phải mở lại app
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5M           # cỡ S (4.8M, scripts/token-usage.py estimate); 5 test mới, 4 test đổi kết quả mong đợi
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-25
---

# PLAN T-052: QUY TẮC 11 CỦA PLAN-CHECK THÀNH LỖI; `fresh` SAU KHI TRẢ MÁY KHÔNG PHẢI MỞ LẠI APP

## 1. Tóm tắt

Ba việc nhỏ còn lại của trình chạy kịch bản emulator, theo đề xuất của báo cáo RUN-20261025-02:

- **Quy tắc 11 thành lỗi.** `device-session` của RUN-20261025-02 có cả 3 kịch bản không `lỗi`, đúng điều kiện của plan T-048 mục 6. `scripts/plan-check.py` báo quy tắc 11 là lỗi với plan `draft`, chú ý với plan `approved`, như quy tắc 7. Bốn test có sẵn của quy tắc 11 đổi kết quả mong đợi từ `pass` sang `stop`; plan liệt kê từng test (mục 3.2).
- **FB-20261025-02.** Giả thuyết của phản hồi (lệnh `wm reset-letterbox-style` của T-049) không có bằng chứng trực tiếp. Logcat của lần chạy CI 40208468872 cho thấy cơ chế khác: dừng app ngay sau khi trả máy làm hệ thống chờ Activity cũ hủy xong, hết khoảng 1 giây thì `Destroy timeout of remove-task` giết tiến trình của app, đúng lúc lần mở mới đang khởi động (mục 2). `fresh` của tệp đứng sau một lần trả máy chờ `DEVICE_SCENARIO_STOP_SETTLE` giây (mặc định 3) sau khi dừng app rồi mới mở. Tệp đầu và `fresh` giữa tệp không chờ.
- **Kịch bản T-024.** Sửa chú thích dòng 14 (khung mở gập mới không đóng khung, 3 cột). Không thêm lệnh đếm cột: `count` không đếm được cột (mục 2).

Thêm hai ghi chú rẻ của rà soát T-049: câu trả máy của `docs/ops/test-sessions.md` nhắc cài đặt giới thiệu, hai dòng chú thích quá dài của `fake-adb`.

Không sửa `.github/`, `scripts/device-session.sh` hay app.

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ. Yêu cầu là mục Đề xuất và Phát hiện của báo cáo RUN-20261025-02, FB-20261025-02, plan T-048 mục 6 và plan T-049 mục 6.

| Lỗi hay đề xuất | Tiêu chí chấp nhận | Cách hiểu của AI (bằng chứng) |
| --- | --- | --- |
| Quy tắc 11 thành lỗi (đề xuất 1 của báo cáo) | Plan `draft` có vấn đề của quy tắc 11: `plan-check.py` thoát 1, in `LỖI: quy tắc 11 ...`. Plan `approved`: thoát 0, in `CHÚ Ý: quy tắc 11 ...`. Plan `done` hay `stopped`: chú ý như cũ | Docstring của `plan-check.py` (quy tắc 11) đã ghi điều kiện và cách đổi: "a separate one-line change with its test". Điều kiện đạt: `summary.md` của TS-20261025-02 có T-022 `đạt`, T-024 `đạt`, T-020 `bỏ qua (môi trường)`; `bỏ qua (môi trường)` tính là không lỗi (Tuấn, 2026-10-24, plan T-048 mục 8 câu 3). Hiện mọi plan trong repo là `done` (48 plan), nên không plan nào đang có bị chặn. Plan T-050, T-051, T-053 viết song song sẽ `approved` trước khi T-052 merge, nên chỉ nhận chú ý |
| FB-20261025-02: `fresh` của tệp thứ hai, thứ ba phải mở lại | `fresh` của một tệp đứng sau một lần trả máy chờ `DEVICE_SCENARIO_STOP_SETTLE` giây (mặc định 3), tính từ lúc lệnh `am force-stop` xong, rồi mới `am start -W`; ghi `note ... fresh: chờ 3 giây sau khi dừng app (máy vừa trả về gốc)`. Tệp đầu của lượt và lệnh `fresh` thứ hai trong cùng tệp không chờ | Phần Chẩn đoán dưới bảng |
| Chú thích dòng 14 của T-024 (đề xuất 3) | Chú thích nói đúng điều đã thấy: ở `1536x1919` 365 dpi (673 x 841 dp, T-049) app không bị đóng khung, thư viện 3 cột; số cột kiểm bằng ảnh `unfolded`, vì `count` không đếm được cột | `count <bộ chọn> <n>` đếm mọi phần tử khớp sau một lần dump (`docs/ops/test-sessions.md` mục 5). Số thẻ thấy được phụ thuộc chiều cao màn và số hàng, không phải số cột; bộ lọc chỉ có `in=`, `index=`, `selected=`, `clickable=`. Đếm cột cần cú pháp mới của trình chạy, ngoài phạm vi. `docs/auto-run.md` mục 4 đã yêu cầu phiên mở ảnh `shot` trước khi ghi tiêu chí bố cục là đạt |
| Ghi chú T-049: câu "Sau mỗi tệp, trình chạy trả máy về gốc" chưa nhắc cài đặt giới thiệu | Câu liệt kê cả cài đặt giới thiệu đóng khung | `docs/ops/test-sessions.md` dòng "Sau mỗi tệp, trình chạy trả máy về gốc: kích thước và mật độ, xoay màn hình, chế độ tối, ngôn ngữ riêng của app, và mạng ..." |
| Ghi chú T-049: dòng chú thích dài của `fake-adb` | Hai dòng chú thích dài hơn 120 ký tự (dòng 5: 163, dòng 12: 152) được ngắt, nội dung giữ nguyên | `awk 'length>120'` trên `scripts/tests/fake-adb` |

**Chẩn đoán FB-20261025-02** (artifact `device-session` của lần chạy 40208468872, TS-20261025-02, `logcat.txt`; so với TS-20261025-01 của lần chạy 40179423644):

- **Kết luận:** ở tệp T-022, `am force-stop` lúc 16:04:34.045 giết tiến trình cũ (pid 7636), `wm_finish_activity ... proc died without state saved`; `pm clear` lúc 16:04:34.226; task cũ #12 bị gỡ lúc 16:04:34.496. Lần mở mới: `START u0` lúc 16:04:35.196, Zygote tạo tiến trình 8778 lúc 16:04:35.278. Giữa hai việc đó, 16:04:35.261: `ActivityTaskManager: Destroy timeout of remove-task, attempt to kill Task{2135c25 #12 ...}`, rồi 16:04:35.342: `ActivityManager: Killing 0:com.vglstudio.camera.pose.line.guide.staging/u0a192 (adj -10000): remove task`. Hệ thống giết theo tên tiến trình của task cũ, nên trúng tiến trình đang khởi động của lần mở mới (`Killing 0:` là bản ghi tiến trình chưa gắn xong).
- **Kết luận:** T-024 giống hệt: dừng app 16:07:08.656, task cũ #15 gỡ 16:07:09.392, `START` 16:07:09.780, `Destroy timeout` và `Killing 0: ... remove task` cùng lúc 16:07:09.915.
- **Kết luận:** `Destroy timeout` xuất hiện khi gỡ task chậm, vì Activity chưa hủy xong lúc app bị dừng. Logcat có thêm một lần ngoài trình chạy, ở phần khởi động nguội của `device-session`, không có lần trả máy trước: `pm clear` lúc 16:01:13.535, task #10 gỡ 16:01:14.128, timeout 16:01:14.934; không ai bị giết vì lần mở sau tới 16:01:20.706 mới có. Trong trình chạy, nó chỉ thấy sau một lần trả máy (lệnh `wm size`, `wm density`, `uimode`, ngôn ngữ của app làm Activity đang dựng lại). Tệp đầu (T-020, sau monkey, không có lần trả máy trước) và `fresh` giữa tệp T-022 (bước 27, 16:06:00) không có dòng này và mở được ngay.
- **Kết luận:** RUN-20261025-01 (chưa có lệnh `reset-letterbox-style`) cũng có `Destroy timeout` sau lần trả máy (T-024: dừng 09:24:26.039, task gỡ 09:24:26.120, timeout 09:24:27.047), nhưng lần mở mới (`START` 09:24:26.611) đã có tiến trình trước lúc đó, nên không bị giết. Khoảng từ dừng app tới `START` ở RUN-20261025-01 là 0,3 đến 0,6 giây; ở RUN-20261025-02 là 1,1 giây, rơi đúng vào lúc hệ thống giết.
- **Kết luận:** thời điểm timeout, tính từ lúc dừng app: 1,22 và 1,26 giây ở hai lần giết (RUN-20261025-02; dòng `Killing` sau đó 0 đến 0,08 giây), 1,40 giây ở lần khởi động nguội; tính từ lúc gỡ task: 0,77 đến 0,93 giây ở cả 5 lần thấy (3 ở RUN-20261025-02, 2 ở RUN-20261025-01).
- **Vì sao chờ cố định, không dò:** tiến trình cũ chết ngay lúc dừng app (`pidof` rỗng), và task rời danh sách (`dumpsys activity`) khoảng 0,8 giây trước lúc giết. Cả hai đều báo "xong" quá sớm, nên không có tín hiệu nào để dò; chờ một khoảng cố định từ lúc dừng app.
- **Giả thuyết (chưa kiểm):** vì sao khoảng dừng tới mở dài hơn ở RUN-20261025-02 thì chưa rõ; lệnh `reset-letterbox-style` có thể làm việc dựng lại Activity lâu hơn, nhưng logcat không ghi gì về lệnh này. Cách sửa của plan không phụ thuộc giả thuyết này: mở sau khi lúc hệ thống giết đã qua thì lần mở không bị giết, dù lý do kéo dài là gì.

Vì vậy plan không đổi lệnh `reset-letterbox-style` (chỉ trả máy khi tệp có `device unfold`, như ticket gợi ý): không có bằng chứng nó là nguyên nhân, và test có sẵn của T-049 (`the reset after the file and at the end turns the letterbox education back to the default`, đúng 2 lần) sẽ phải sửa.

Các ghi chú còn lại của rà soát T-049 không làm ở đây:
- danh sách trả máy 7 lệnh có thể vượt hạn tổng 30 giây khi vài lệnh treo: chưa thấy ở `device-session` nào, plan T-049 đã chọn không đổi hạn;
- `reset-letterbox-style` chạy sau mọi tệp: đúng câu 2 mục 8 của plan T-049 (Tuấn đã duyệt);
- `adb` in cảnh báo trước pid thì lần đọc `pidof` đó tính là chưa có tiến trình: vòng chờ trong `start_app` đọc lại, chưa thấy ở `device-session` nào.

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0020 (khung `_unfolded` 673 x 841).

## 3. Giải pháp

### 3.1. Cách làm

**Quy tắc 11.** `scripts/plan-check.py`, hàm `check`: dòng `warnings += manual_test_problems(text, listed)` thành

```python
    (errors if status == "draft" else warnings).extend(manual_test_problems(text, listed))
```

(`status` đã có ở mấy dòng trên). Docstring đầu tệp, quy tắc 11: thay hai câu "Only a warning, for draft and approved plans alike (decision of 2026-10-24). It becomes an error ... with its test." bằng "Errors for a draft plan, warnings for an approved one, like rule 7 (since plan T-052: the device-session of RUN-20261025-02 had no failing scenario file)."

**`fresh` sau khi trả máy.** `scripts/device-scenario.py`:
- `__init__`: `self.stop_settle = env_seconds("DEVICE_SCENARIO_STOP_SETTLE", 3)` (sau `evidence_reserve`) và `self.device_reset = False` với chú thích "the device was given back since the last fresh, so fresh waits after stopping the app".
- `reset_device`: dòng đầu của hàm đặt `self.device_reset = True`.
- `step_fresh`: ghi `stopped = time.monotonic()` ngay sau lệnh `am force-stop` đầu tiên. Sau khi có `self.activity` và trước `problem = self.start_app()`:

  ```python
        if self.device_reset:
            # Stopping an app whose Activity the reset is still rebuilding makes the system kill the next start
            # about a second later ("Destroy timeout of remove-task", FB-20261025-02): start after that.
            self.device_reset = False
            self.note(f"fresh: chờ {seconds_text(self.stop_settle)} giây sau khi dừng app (máy vừa trả về gốc)")
            time.sleep(max(0.0, min(stopped + self.stop_settle, self.file_deadline) - time.monotonic()))
  ```

  Lần mở lại (sau `force-stop` thứ hai) không chờ: lúc đó không còn Activity nào đang dựng lại.
- Docstring đầu tệp: lệnh `fresh` thêm "after the device was given back it waits DEVICE_SCENARIO_STOP_SETTLE seconds (default 3) after stopping the app"; dòng Settings thêm `DEVICE_SCENARIO_STOP_SETTLE`.
- Mặc định 3 giây: timeout tới sau 1,22 đến 1,40 giây kể từ lúc dừng app ở các lần thấy (mục 2), nên 3 giây chừa khoảng 1,6 giây. Mỗi lượt có 2 lần chờ (tệp thứ hai, thứ ba), tức 6 giây, thay cho khoảng 40 giây của hai lần mở lại. Tổng của ba tệp (348 giây ở RUN-20261025-02) còn xa ngân sách 900.
- Test có sẵn chạy một tệp mỗi lần nên không chờ; test chạy nhiều tệp mà tệp sau có `fresh` thì chờ 3 giây (bản cài thử: toàn bộ `run.sh` chạy 2 phút 31 giây, so với 2 phút 47 giây trên code hiện tại; không test nào đo thời gian của các tệp này).

**Kịch bản T-024.** `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt` dòng 14 thành:

```text
# Mở gập trên emulator CI là 1536x1919 ở 365 dpi (673 x 841 dp, T-049): app không bị đóng khung, thư viện 3 cột. count không đếm được cột, nên số cột kiểm bằng ảnh unfolded.
```

Chỉ đổi chú thích; các bước giữ nguyên. Tệp vẫn qua `scripts/device-scenario.py check` (dòng chú thích bắt đầu bằng `#`).

**Tài liệu.** `docs/ops/test-sessions.md` mục 5:
- hàng `fresh` của bảng lệnh: thêm "Tệp đứng sau một lần trả máy thì chờ `DEVICE_SCENARIO_STOP_SETTLE` giây (mặc định 3) sau khi dừng app rồi mới mở: dừng app khi Activity còn đang dựng lại sau lần trả máy làm hệ thống giết lần mở ngay sau đó (FB-20261025-02)";
- câu "Sau mỗi tệp, trình chạy trả máy về gốc: ..." thêm "cài đặt giới thiệu đóng khung (`wm reset-letterbox-style`)";
- đoạn "Các biến khác": thêm `DEVICE_SCENARIO_STOP_SETTLE`.

`docs/CLAUDE.md`, mục Khi viết plan, câu về kịch bản emulator: "(L-062; `scripts/plan-check.py`, quy tắc 11, chỉ nhắc)" thành "(L-062; `scripts/plan-check.py`, quy tắc 11: lỗi khi plan còn `draft`)". `docs/templates/plan.md`: "`scripts/plan-check.py` nhắc dòng thiếu (quy tắc 11)" thành "`scripts/plan-check.py` báo lỗi dòng thiếu khi plan còn `draft` (quy tắc 11)".

**`fake-adb`.** Ngắt dòng chú thích 5 (sau "`find /sdcard` prints $FAKE_ADB_FILES.") và dòng 12 (sau "screen at 420 dpi."), mỗi dòng không quá 120 ký tự; chữ giữ nguyên.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/plan-check.py` | Quy tắc 11 vào `errors` khi plan `draft`; docstring quy tắc 11 |
| Sửa | `scripts/device-scenario.py` | `stop_settle`, `device_reset` trong `__init__`; `reset_device` đặt `device_reset`; `step_fresh` chờ sau khi dừng app; docstring đầu tệp |
| Sửa | `scripts/tests/run.sh` | (1) Khối quy tắc 11: 2 test mới (mục 4), thêm ngay trước dòng chú thích bắt đầu bằng `# Rule 12: a cloud session has no TMPDIR`, với chú thích `# Plan T-052: ...`. (2) Bốn test có sẵn đổi kết quả mong đợi, vì hành vi đổi theo ticket: `plan-check: a manual row without where it runs is a warning` (dòng 1052, `"pass 1 1"` thành `"stop 1 1"`, tên thành `... is an error for a draft plan`), `plan-check: an emulator row needs a scenario file in the file list or the repository` (dòng 1060, `"pass 1"` thành `"stop 1"`), `plan-check: a scenario file that fails check is a warning` (dòng 1064, `"pass 1"` thành `"stop 1"`, tên thành `... is an error for a draft plan`), `plan-check: máy thật and người need a reason` (dòng 1068, `"pass 2"` thành `"stop 2"`). Không đổi: `a manual row of an approved plan without where it runs is a warning too` (dòng 1056, plan `approved`, vẫn `"pass 1"`) và đối chứng `a correct plan has no line of rule 11` (dòng 1076, vẫn đếm 1 dòng quy tắc 11 của T-940). Bốn test này dùng plan `draft`, nên trên code mới thoát 1; số dòng quy tắc 11 mà chúng đếm không đổi. (3) Chú thích đầu khối quy tắc 11 ("Only a warning, for draft and approved plans alike.") thành "An error for a draft plan, a warning for an approved one (plan T-052)."; chú thích của test `run-check: the latest run list has no new error from rule 11` ("(it only warns)") thành "(the plans of a run list are approved, so rule 11 only warns)". (4) Khối `device-scenario`: 3 test mới (mục 4), thêm ngay trước dòng `# Plan T-048: notes of the runner about a late consent form or a restarted app are listed under the table.`, với chú thích `# Plan T-052: ...`; tệp kịch bản tạm ghi bằng `ds_script`, không thêm fixture |
| Sửa | `scripts/tests/fake-adb` | Ngắt hai dòng chú thích dài |
| Sửa | `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt` | Chú thích dòng 14 |
| Sửa | `docs/ops/test-sessions.md` | Mục 5: hàng `fresh`, câu trả máy, biến `DEVICE_SCENARIO_STOP_SETTLE` |
| Sửa | `docs/CLAUDE.md` | Mục Khi viết plan: quy tắc 11 là lỗi khi plan còn `draft` |
| Sửa | `docs/templates/plan.md` | Câu nhắc quy tắc 11 ở mục 4 |

**Nơi khác đọc các file plan này sửa (L-020), và vì sao không bị ảnh hưởng:**
- **`scripts/run-check.py`** gọi `plan-check.py` cho các plan của danh sách chạy. Plan của danh sách chạy đã `approved`, nên quy tắc 11 vẫn chỉ là chú ý; test có sẵn `run-check: the latest run list has no new error from rule 11` còn qua (bản cài thử, mục 4.1).
- **`docs/auto-run.md`** chạy `plan-check.py` cho plan nháp ở hai chỗ: plan phiên bản mới của task bị dừng (mục 5, bước 1, khoảng dòng 233) và bước soạn plan cuối phiên (mục 4, bước 5, "sửa tới khi không lỗi", khoảng dòng 319). Sau T-052, plan nháp thiếu nơi chạy của dòng kiểm thủ công ra lỗi ở đó. AI tự sửa được (ghi `(emulator: ...)`, `(máy thật: ...)` hay `(người: ...)`), nên không phải lỗi chỉ người gỡ được và không làm phiên dừng; không sửa `docs/auto-run.md`.
- **`scripts/status.py`, `scripts/plan-queue.py`** không gọi `plan-check.py` theo quy tắc nào (chỉ đọc trạng thái plan); không đổi.
- **`scripts/device-session.sh`** gọi trình chạy và đọc `scenarios.txt`. Dạng dòng log không đổi; dòng `note` mới không có chữ mà bộ lọc "Ghi chú của trình chạy" của `summary.md` tìm (`hộp thoại đồng ý hiện muộn|mở lại`), nên không thêm dòng vào `summary.md`.
- **`scripts/tests/fake-adb`** dùng ở các khối test khác (`device-session`, `app-explore`): chỉ chú thích đổi.
- **`.github/workflows/device-session.yml`** chạy `device-session.sh`; lệnh không đổi. Mỗi lượt chờ thêm khoảng 6 giây, bớt khoảng 40 giây mở lại.
- **`app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ui/UiAutomatorTagsTest.kt`** nhắc `scripts/device-scenario.py` trong chú thích (lý do bật `testTagsAsResourceId`), không chạy nó. Plan không đổi cách đọc `resource-id`.
- **`scripts/required-assets.txt`** nhắc `docs/CLAUDE.md` (mục Khi viết plan) trong chú thích; plan chỉ đổi câu về quy tắc 11 của mục đó.
- **Kịch bản của repo**: chỉ chú thích của T-024 đổi; test có sẵn `the scenarios of the repository pass check` chạy trên tệp mới.
- **`scripts/tests/run.sh`** được `scripts/check.sh` chạy (cả tệp), và được `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/wait-run.sh` nhắc trong chú thích. Plan thêm test vào hai khối, đổi 4 kết quả mong đợi và 2 chú thích; cách gọi, hàm `ds_run`, `plan_check` và dòng tổng kết không đổi.
- Đã chạy toàn bộ `scripts/tests/run.sh` với bản cài thử của plan (mục 4.1): `All script tests passed`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** thời điểm timeout (0,77 đến 0,93 giây sau khi gỡ task) đo được ở 5 lần trên emulator CI; trên máy chậm hơn có thể dài hơn 3 giây.
- **Điều kiện chuyển:** ở `device-session` cuối phiên hay lần đầu sau khi merge, `scenarios.txt` vẫn có `fresh: lần mở 1 không có tiến trình` ở tệp thứ hai hay thứ ba, kèm `Destroy timeout of remove-task` trong `logcat.txt`.
- **Cách làm thay** (task sau, không cài sẵn): dừng app trước các lệnh trả máy, để lần trả máy không gặp Activity nào còn sống; chỉ ở lần trả máy sau một tệp còn tệp khác chạy sau, vì `meminfo.txt` và `gfxinfo.txt` của phiên đọc tiến trình của tệp cuối. Phiên không dừng vì điều này (mục 7, điều 3); trình chạy vẫn mở lại như T-048.

## 4. Kiểm thử

Test viết trước code: 5 dòng "Test script" mới dưới đây và 4 test có sẵn đổi kết quả mong đợi, trong `scripts/tests/run.sh`, chạy với `adb` giả và repo giả của khối `plan-check`. Kết quả thật của từng test trên code hiện tại nằm ở mục 4.1. Dòng ghi **(đối chứng)** qua được cả trên code cũ (L-035) và không tính vào số test thất bại trước.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Quy tắc 11 là lỗi với plan nháp | Test script | `plan-check: rule 11 stops a draft plan, as an error`: `write_plan "$plans/T-949-a.md" medium draft "$(row Sửa feature/demo/A.kt)"`, `manual_rows "$plans/T-949-a.md" 'Kiểm tra thủ công'`; mong đợi `"stop 1 0"`: `plan_check` ra `stop`, 1 dòng `LỖI:.*quy tắc 11`, 0 dòng `CHÚ Ý:.*quy tắc 11` |
| Quy tắc 11 chỉ là chú ý với plan đã duyệt | Test script (đối chứng) | `plan-check: control: rule 11 only warns for an approved plan`: `T-950-a.md` `approved` với `"$decision_v1"`, cùng dòng; mong đợi `"pass 1 0"` (1 dòng `CHÚ Ý:.*quy tắc 11`, 0 dòng `LỖI:`) |
| Bốn test có sẵn của quy tắc 11 với plan nháp | Test script (đổi kết quả mong đợi) | Bốn test liệt kê ở mục 3.2, điểm (2): kết quả đầu đổi từ `pass` sang `stop`, số dòng quy tắc 11 giữ nguyên |
| `fresh` sau khi trả máy chờ | Test script | `device-scenario: fresh after a reset waits after stopping the app, the first file does not`: `ds_script settle-1.txt 'fresh'`, `ds_script settle-2.txt 'fresh'`, `ds_run "camera.xml" "$ds_files/settle-*.txt" DEVICE_SCENARIO_STOP_SETTLE=2`, đo giây bằng `date +%s` trước và sau; mong đợi `"0 1 0 yes"`: mã 0, 1 dòng `^plan settle-2.txt note step=1 .*fresh: chờ 2 giây sau khi dừng app`, 0 dòng `^plan settle-1.txt note step=1 .*fresh: chờ`, thời gian ít nhất 2 giây |
| Một tệp không chờ | Test script (đối chứng) | `device-scenario: control: a single file does not wait in fresh`: `settle-1.txt` một mình, `DEVICE_SCENARIO_STOP_SETTLE=2`; mong đợi `"0 0 1"` (0 dòng `fresh: chờ`, `step=1 fresh ok`) |
| `fresh` giữa tệp không chờ | Test script (đối chứng) | `device-scenario: control: a second fresh inside one file does not wait`: `ds_script settle-mid.txt 'fresh' 'fresh'`; mong đợi `"0 0 2"` (0 dòng `fresh: chờ`, 2 dòng `^plan settle-mid.txt step=[12] fresh ok$`) |
| Test có sẵn | Test script | Toàn bộ `scripts/tests/run.sh` có sẵn phải còn qua (trừ 4 test đổi kết quả mong đợi ở trên), gồm các test `fresh` của T-048, T-049, `the scenarios of the repository pass check`, `run-check: the latest run list has no new error from rule 11` |
| `fresh` không mở lại ở tệp 2, 3 | Kiểm tra thủ công (emulator: `feature/poses/src/deviceScenarios/T-022-chi-tiet-sang-camera.txt`, `feature/poses/src/deviceScenarios/T-024-thu-vien-mo-gap.txt`) | `device-session` cuối phiên (hoặc lần đầu sau khi merge): `scenarios.txt` có `fresh: chờ 3 giây sau khi dừng app` ở bước 1 của T-022 và T-024, không có `fresh: lần mở 1 không có tiến trình`; `logcat.txt` không có `Killing 0:... remove task` sau `START` của hai tệp đó. Còn mở lại thì ghi vào báo cáo theo mục 3.4, không dừng (mục 7, điều 3) |
| Phiên làm theo tài liệu | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt chéo đọc diff `docs/ops/test-sessions.md` mục 5, `docs/CLAUDE.md`, `docs/templates/plan.md` |

**Ngoại lệ viết test trước:** tài liệu (`docs/ops/test-sessions.md`, `docs/CLAUDE.md`, `docs/templates/plan.md`), docstring và chú thích (`scripts/plan-check.py`, `scripts/device-scenario.py`, `scripts/tests/fake-adb`, `scripts/tests/run.sh`), chú thích của kịch bản T-024.

### 4.1. Chạy thử

Tiêu chí E là 0: không có kỹ thuật mới; `time.sleep` và `env_seconds` đã có trong trình chạy.

**Chạy các test mới trên code hiện tại** (L-075; 2026-10-25, nhánh chính `0dfd2a4`, macOS, `/bin/bash` 3.2, `scratchpad/env.sh`). Hai khối tạm chèn vào bản `scripts/tests/run.sh` của worktree đúng chỗ ở mục 3.2, chạy cả tệp; không commit, đã trả về.

| Test | Code hiện tại | Lý do |
| --- | --- | --- |
| quy tắc 11 dừng plan nháp | thất bại `'pass 0 1'` | quy tắc 11 chỉ ra `CHÚ Ý`, plan qua |
| đối chứng plan đã duyệt chỉ chú ý | qua `pass 1 0` | |
| `fresh` sau khi trả máy chờ | thất bại `'0 0 0 no'` | không có dòng `fresh: chờ`, cả lượt dưới 2 giây |
| đối chứng một tệp | qua `0 0 1` | |
| đối chứng `fresh` giữa tệp | qua `0 0 2` | |
| 4 test có sẵn với kết quả mong đợi mới | thất bại: `FAIL - plan-check: a manual row without where it runs is a warning: expected 'stop 1 1', got 'pass 1 1'`; `FAIL - plan-check: an emulator row needs a scenario file in the file list or the repository: expected 'stop 1', got 'pass 1'`; `FAIL - plan-check: a scenario file that fails check is a warning: expected 'stop 1', got 'pass 1'`; `FAIL - plan-check: máy thật and người need a reason: expected 'stop 2', got 'pass 2'` | quy tắc 11 chỉ là chú ý, plan nháp qua |

Tổng (lần chạy thứ hai, khối tạm gồm cả 5 test mới và 4 kết quả mong đợi mới): 645 test, 639 `ok` (gồm 3 đối chứng), 6 `FAIL` (2 test mới, 4 test đổi kết quả), đúng lý do, không `FAIL` nào khác. Lần chạy đầu (chỉ 5 test mới, kết quả mong đợi cũ): 645 test, 643 `ok`, 2 `FAIL`.

**Bản cài thử** (không commit, đã trả về bằng `git checkout`): `scripts/plan-check.py` và `scripts/device-scenario.py` sửa như mục 3.1 (không gồm docstring), 4 kết quả mong đợi đổi như mục 3.2. Kết quả: 5/5 test mới và 4 test đổi kết quả qua; toàn bộ `scripts/tests/run.sh`: 645 test, 645 `ok`, `All script tests passed`, 2 phút 31 giây.

Không chạy thử trên emulator: emulator của máy dev đã xóa theo yêu cầu (2026-10-25). Lần kiểm thật là `device-session` cuối phiên (mục 4, dòng kiểm thủ công).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI và quy trình phiên tự chạy). `plan-check.py` chạy với mọi plan và ở `run-check.py`; trình chạy kịch bản chạy ở `device-session` cuối mỗi phiên.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một dòng của `plan-check.py`, vài dòng của `device-scenario.py`, test, chú thích, tài liệu |
| B. Độ rõ của yêu cầu | 0 | Điều kiện của quy tắc 11 có sẵn trong docstring; cơ chế của FB-20261025-02 đọc thẳng từ logcat |
| C. Khả năng kiểm chứng tự động | 2 | Mọi thay đổi có test; việc hệ thống giết lần mở chỉ kiểm được ở `device-session` sau khi merge |
| D. Ảnh hưởng chéo | 2 | Quy tắc 11 chặn mọi plan nháp sau này thiếu nơi chạy của dòng kiểm thủ công |
| E. Độ mới của kỹ thuật | 0 | Không có |
| **Tổng** | 5 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- **File hoặc thành phần dùng chung bị chạm:** `scripts/plan-check.py` (mọi plan, `run-check.py`), `scripts/device-scenario.py` (`device-session` cuối mỗi phiên), `scripts/tests/run.sh`, `scripts/tests/fake-adb`, `docs/CLAUDE.md`, `docs/templates/plan.md`, `docs/ops/test-sessions.md`.
- **Task khác có thể xung đột:** danh sách chạy dự kiến có 4 task theo thứ tự T-053 (chép cách gán token của phiên con từ dự án mẫu, L-073), T-052, T-050, T-051. T-053 sửa `scripts/tests/run.sh` (khối `token-usage`), `docs/auto-run.md`, `docs/ops/token-usage.md`, `docs/lessons.md`; chung với T-052 chỉ có `scripts/tests/run.sh`, ở khối khác, task chạy lần lượt, mỗi task tạo nhánh từ nhánh chính mới nhất và giữ phần của task trước. T-050, T-051 sửa module `:feature:camera`, `:feature:poses`, `:core:designsystem` và test của chúng, không chạm file của plan này.
- **Plan viết song song:** plan T-050, T-051, T-053 đã `approved` khi phiên chạy, nên quy tắc 11 mới chỉ cho chúng chú ý; `run-check.py` không ra lỗi mới. Plan viết sau khi T-052 merge phải ghi nơi chạy cho mọi dòng "Kiểm tra thủ công", không thì `plan-check.py` chặn.
- **Việc sau task này, không làm ở đây:**
  - **FB-20261025-02:** phiên làm T-052 ghi kết luận của mục 2 và liên kết plan này vào mục Phân loại của phản hồi khi chủ quality-gate đồng ý (mục 8 câu 3).
  - **Lệnh đếm cột:** nếu cần kiểm số cột tự động, một ticket sau thêm cú pháp cho trình chạy (ví dụ đếm phần tử cùng hàng theo `bounds`).
  - **FB-20261024-01** (khóa hướng dọc): chủ feature xem lại với kết quả của plan T-049, không thuộc plan này.
- **Dự án mẫu.** Quy tắc 11 thành lỗi và chờ sau khi dừng app đề xuất về mẫu (`docs/ops/from-app.md`) cùng các điểm T-048, T-049 (issue VGL-Studio/app-project-template#163); ghi vào mô tả PR.
- **Đề xuất thứ tự chạy:** sau T-053, trước T-050 và T-051, để `device-session` cuối phiên có cả cách sửa `fresh` lẫn các thay đổi chữ của app.

## 7. Điều kiện dừng riêng

1. Cần sửa `.github/workflows/device-session.yml` hay `scripts/device-session.sh`: dừng, viết plan riêng.
2. Một test có sẵn của khối `device-scenario`, `device-session`, `plan-check` hay `run-check` phải sửa thì mới qua, ngoài 4 test và 2 chú thích liệt kê ở mục 3.2: dừng.
3. Không phải điều kiện dừng: ở `device-session` cuối chính phiên này, `fresh` vẫn phải mở lại, hay một kịch bản lỗi. Phiên ghi vào báo cáo kèm dòng `note` và đoạn logcat (mục 3.4); crash và ANR vẫn chặn staging như cũ.

## 8. Câu hỏi cho người duyệt

1. **Đổi kết quả mong đợi của 4 test có sẵn** (mục 3.2, điểm 2). Ticket đổi đúng hành vi mà các test này kiểm (quy tắc 11 với plan nháp), nên không đổi test thì không làm được. **Đề xuất của AI:** chấp nhận; test giữ nguyên plan, dòng và số dòng quy tắc 11 mà chúng đếm, chỉ đổi `pass` thành `stop`, cùng 2 test mới kiểm cả hai phía (nháp lỗi, đã duyệt chú ý).
2. **Cách sửa FB-20261025-02: chờ 3 giây sau khi dừng app**, chỉ ở `fresh` của tệp đứng sau một lần trả máy. **Đề xuất của AI:** chấp nhận; rẻ, không phụ thuộc giả thuyết về `reset-letterbox-style`, còn mở lại của T-048 làm lưới an toàn. Phương án khác: dừng app trước các lệnh trả máy (mục 3.4); đụng tới `meminfo.txt`, `gfxinfo.txt` của tệp cuối nên phải phân biệt lần trả máy cuối, phức tạp hơn.
3. **FB-20261025-02.** **Đề xuất của AI:** phiên làm T-052 ghi kết luận của mục 2 và liên kết vào mục Phân loại của phản hồi (Tuấn là chủ quality-gate), như FB-20261025-01 ở T-049.
4. **Không thêm lệnh đếm cột cho T-024** (mục 2). **Đề xuất của AI:** chấp nhận; chỉ sửa chú thích, phiên mở ảnh `unfolded` để kiểm 3 cột như `docs/auto-run.md` mục 4 đã yêu cầu.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-25, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Câu 1: chấp nhận đổi kết quả mong đợi của 4 test có sẵn của quy tắc 11 (mục 3.2). Câu 2: chờ 3 giây sau khi dừng app ở fresh sau lần trả máy. Câu 3: phiên làm T-052 ghi kết luận vào mục Phân loại của FB-20261025-02. Câu 4: không thêm lệnh đếm cột cho T-024. Plan qua một rà soát độc lập (0 chặn, 4 nên sửa, đã sửa hết).

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261026-01, phiên bản plan 1

- **Kết quả:** đã merge (commit `f3baa0e`, 2026-10-25 18:18 UTC).
- **Nhánh, PR:** `claude/T-052-quy-tac-11-va-fresh`, PR #144.
- **Đã làm:** `scripts/plan-check.py` quy tắc 11 là lỗi với plan `draft`; `scripts/device-scenario.py` `fresh` sau lần trả máy chờ `DEVICE_SCENARIO_STOP_SETTLE` giây (mặc định 3) sau khi dừng app; 5 test mới, 4 test có sẵn đổi kết quả mong đợi, 2 chú thích trong `scripts/tests/run.sh`; chú thích kịch bản T-024 và `fake-adb`; `docs/ops/test-sessions.md`, `docs/CLAUDE.md`, `docs/templates/plan.md`. Mục Phân loại của FB-20261025-02 ghi trên nhánh báo cáo (mục 8, câu 3).
- **Test viết trước:** 9 test (5 mới, 4 đổi kết quả); 6 thất bại đúng lý do (4 test đổi kết quả `expected 'stop ...', got 'pass ...'`; `rule 11 stops a draft plan` `'pass 0 1'`; `fresh after a reset waits` `'0 0 0 no'`), 3 đối chứng qua, đúng như mục 4.1. Commit test `d185cce` trước commit code `3d5f562`.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 6 ghi chú (dòng `note` luôn ghi "chờ 3 giây" kể cả khi chờ thật ngắn hơn; tệp có `device ...` rồi `fresh` trong cùng tệp không chờ; test "ít nhất 2 giây" đo cả lượt; dòng 126 ký tự; không có test riêng cho plan `done`/`stopped`, đã kiểm tay; nhắc ghi FB-20261025-02).
- **Kết quả CI:** `pr-check` xanh ở vòng đầu (lần chạy 40219969975). `scripts/check.sh` cục bộ qua.
- **Lệch so với plan:** Không có.
- **Việc còn lại:** `device-session` cuối phiên kiểm `fresh` của T-022, T-024 không còn mở lại (mục 4, dòng kiểm thủ công emulator); đề xuất về dự án mẫu (issue VGL-Studio/app-project-template#163).
- **Cần người xem:** người duyệt chéo đọc diff tài liệu (`docs/ops/test-sessions.md` mục 5, `docs/CLAUDE.md`, `docs/templates/plan.md`).
