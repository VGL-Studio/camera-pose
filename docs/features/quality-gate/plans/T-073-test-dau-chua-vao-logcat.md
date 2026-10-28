---
ticket: T-073
title: Test nhánh "dấu của lần chạy chưa vào logcat.txt" khi trình chạy kịch bản tìm app bị hệ thống giết
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.8M         # cỡ S (scripts/token-usage.py estimate, 2026-10-28); code của test có sẵn ở mục 3.1
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-28
---

# PLAN T-073: TEST NHÁNH "DẤU CỦA LẦN CHẠY CHƯA VÀO LOGCAT.TXT" KHI TRÌNH CHẠY KỊCH BẢN TÌM APP BỊ HỆ THỐNG GIẾT

## 1. Tóm tắt

`killed_by` của `scripts/device-scenario.py` trả `None` khi dấu `scenario=plan file=<tệp>` của lần chạy hiện tại chưa vào `logcat.txt` (`if len(starts) < attempt`, dòng 1090), để lần chạy thứ hai không nhận dòng "app bị giết" của lần đầu. Rà soát T-054 (RUN-20261026-02, mức nên sửa) ghi nhánh này không có test và cần một biến mới cho `fake-adb`. Plan thêm biến `FAKE_ADB_LOG_LINES` và một test; không đổi code của trình chạy. Đã chạy thật trên code hiện tại: test qua; bỏ nhánh đó thì test đỏ (mục 4.1).

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ (ticket quality-gate). Yêu cầu là ghi chú rà soát T-054 và BR-20261028-01, dòng 6:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Ghi chú rà soát T-054 | Khi lần chạy thứ hai của một tệp lỗi mà dấu của lần đó chưa vào `logcat.txt` thì trình chạy không coi dòng "app bị giết" sau dấu của lần đầu là lý do, tệp kết thúc `lỗi` | Mô phỏng logcat chậm: `fake-adb` không ghi dòng `log -t` thứ hai trở đi, nên `logcat.txt` chỉ có dấu của lần đầu và dòng giết ngay sau nó |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-1003 (test cho hành vi đã đúng, đối chứng bằng đột biến), ADR-0032 (lệch nhỏ của phiên), ADR-1006 (L11).

## 3. Giải pháp

### 3.1. Cách làm

**`scripts/tests/fake-adb`, nhánh `"log -t "*`**, ngay sau dòng `[[ -z "${FAKE_ADB_LOGCAT_FILE:-}" ]] && exit 0`:

```bash
        if [[ -n "${FAKE_ADB_LOG_LINES:-}" ]]; then
          state="${FAKE_ADB_STATE_DIR:-${TMPDIR:-/tmp}}"
          mkdir -p "$state"
          logged="$(cat "$state/log.n" 2>/dev/null || echo 0)"
          echo $(( logged + 1 )) > "$state/log.n"
          (( logged >= FAKE_ADB_LOG_LINES )) && exit 0
        fi
```

Dòng thứ `FAKE_ADB_LOG_LINES + 1` trở đi không vào `$FAKE_ADB_LOGCAT_FILE`, kể cả `FAKE_ADB_KILL_LINE` đi kèm. Thêm vào chú thích đầu tệp, sau câu về `FAKE_ADB_KILL_ONCE`: "With $FAKE_ADB_LOG_LINES set, only the first that many `log -t` lines reach the file, as a logcat that lags behind (counted in $FAKE_ADB_STATE_DIR/log.n; camera-pose T-073)." Không đặt biến thì hành vi như cũ.

**`scripts/tests/run.sh`**, ngay sau test `"device-scenario: the kill of the first run does not count in the second run"` của khối T-054:

```bash
# camera-pose T-073: the marker of the second run has not reached logcat.txt yet (FAKE_ADB_LOG_LINES=1): the kill after the
# marker of the first run is not this run's, so the file fails instead of being skipped.
expect "device-scenario: a second run whose marker is not in logcat.txt yet does not take the kill of the first run" "0 1 1 1 0 2 1" \
  "$(ds_run "camera.xml" "$ds_files/killed.txt" FAKE_ADB_KILL_LINE="$ds_kill_app" FAKE_ADB_KILL_ONCE=1 FAKE_ADB_LOG_LINES=1 FAKE_ADB_DEAD_STARTS=9 DEVICE_SCENARIO_PROCESS_WAIT=0.1) $(ds_count '^plan killed.txt step=1 fresh RETRY ') $(ds_count '^plan killed.txt step=1 fresh FAIL app không chạy sau 2 lần mở') $(ds_count '^plan killed.txt result=lỗi') $(ds_count 'SKIP') $(ds_count "scenario=plan file=killed.txt" "$ds_calls") $(ds_count "scenario=plan file=killed.txt" "$work/ds-session/logcat.txt")"
```

Bảy giá trị: mã thoát 0; một `RETRY` (lần đầu thấy dòng giết); lần hai `FAIL app không chạy sau 2 lần mở`; kết quả `lỗi`; không `SKIP`; trình chạy gửi hai dấu (`$ds_calls`); `logcat.txt` chỉ có một dấu (hai giá trị cuối chứng minh điều kiện của nhánh đã được dựng).

Không sửa tài liệu: biến của `fake-adb` chỉ được mô tả trong chú thích đầu tệp.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/tests/fake-adb` | Biến `FAKE_ADB_LOG_LINES`, chú thích đầu tệp |
| Sửa | `scripts/tests/run.sh` | Một test sau test "the kill of the first run does not count in the second run" |

**Nơi khác đọc các file plan này sửa (L-020), và vì sao không bị ảnh hưởng:**

- **`.github/workflows/pr-check.yml`** và **`scripts/check.sh`** chạy cả tệp `scripts/tests/run.sh`; plan chỉ thêm một test, không đổi cách gọi hay công cụ cần có (`bash`, `python3`).
- **`scripts/tests/app.sh`** được `run.sh` nạp trong cùng shell; nó không dùng `FAKE_ADB_LOG_LINES` hay `log.n` (`grep` trả 0 dòng), và plan không thêm hàm hay biến shell mới.
- **`scripts/local-setup.sh`** nhắc `scripts/tests/run.sh` trong chú thích, không chạy nó.
- `fake-adb` chỉ đổi khi đặt `FAKE_ADB_LOG_LINES`; không test hay script nào khác đặt biến này.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có.

### 3.5. Hành vi đã có bị đổi

Không có: plan không sửa code Kotlin; `fake-adb` chỉ đổi khi đặt biến mới, mọi test có sẵn không đặt biến này.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Dấu của lần hai chưa vào `logcat.txt` thì không nhận dòng giết của lần đầu, tệp `lỗi` | Test của script (`scripts/tests/run.sh`) | `device-scenario: a second run whose marker is not in logcat.txt yet does not take the kill of the first run` (mục 3.1) |
| Không đặt `FAKE_ADB_LOG_LINES` thì `fake-adb` như cũ | Test của script | Mọi test `device-scenario` có sẵn của khối T-054 (`FAKE_ADB_KILL_LINE`) vẫn qua |

**Ngoại lệ viết test trước:** code của `killed_by` đã đúng nên test không thất bại trước được (ADR-1003). Đối chứng bằng đột biến: tạm xóa hai dòng `if len(starts) < attempt: return None` của `scripts/device-scenario.py`, chạy test mới, test phải đỏ; hoàn tác, ghi kết quả vào nhật ký plan (L-108 của mẫu). Đã chạy ở mục 4.1.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-28, trên `origin/main` 2b8d67e, phần `fake-adb` của mục 3.1 và test mục 3.1 trong một khối tạm (bản sao `ds_run`, `ds_count`), không commit (L-075).
- **Lệnh:** `bash <khối tạm>`, khoảng 5 giây; chỉ chạy test mới, không chạy cả bộ.
- **Kết quả:**
  - Code hiện tại: `0 1 1 1 0 2 1`, đúng giá trị mong đợi (đối chứng, qua).
  - Đột biến (xóa nhánh `len(starts) < attempt`): `0 1 0 0 1 2 1`; `scenarios.txt` ghi `step=1 fresh SKIP app bị hệ thống giết theo Google Play services (com.google.android.gms.persistent)` và `result=bỏ qua (môi trường)`: test đỏ đúng lý do.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI, công cụ kiểm tra của phiên: `scripts/tests/`)

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Hai file test của script |
| B. Độ rõ của yêu cầu | 0 | Ghi chú rà soát nêu rõ nhánh và cách làm |
| C. Khả năng kiểm chứng tự động | 0 | Test của script, đã chạy thật và đột biến |
| D. Ảnh hưởng chéo | 1 | `fake-adb` dùng chung cho mọi test của `device-session`, `device-scenario`; biến mới không đặt thì không đổi gì |
| E. Độ mới của kỹ thuật | 0 | Đúng mẫu `FAKE_ADB_KILL_ONCE` (đếm trong `$FAKE_ADB_STATE_DIR`) |
| **Tổng** | 2 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/tests/fake-adb`, `scripts/tests/run.sh`.
- Task khác trong sprint có thể xung đột: T-072 không chạm `scripts/`. Plan khác sửa `scripts/tests/run.sh` cùng phiên thì có thể xung đột chỗ chèn: chèn ngay sau test đã nêu, `git apply -3` (ADR-0032/L3).
- Đề xuất thứ tự chạy: độc lập.

## 7. Điều kiện dừng riêng

Không có.

## 8. Câu hỏi cho người duyệt

1. Dự án mẫu có cùng `killed_by`? Không: khối T-054 là phần riêng của app (chú thích `camera-pose T-054`). Đề xuất: không mở issue `from-app`.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-28, trả lời mục 8: không mở issue `from-app` (`killed_by` chỉ có ở app này).

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
