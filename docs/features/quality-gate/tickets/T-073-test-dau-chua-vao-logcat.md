---
id: T-073
title: Test nhánh "dấu của lần chạy chưa vào logcat.txt" khi trình chạy kịch bản tìm app bị hệ thống giết
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là ghi chú rà soát T-054 (RUN-20261026-02) và BR-20261028-01, dòng 6
scenarios: []
owner: Nhật
status: approved
risk: high
size: S
depends_on: []
sprint:
plan: features/quality-gate/plans/T-073-test-dau-chua-vao-logcat.md
created: 2026-10-28
---

# T-073: TEST NHÁNH "DẤU CỦA LẦN CHẠY CHƯA VÀO LOGCAT.TXT" KHI TRÌNH CHẠY KỊCH BẢN TÌM APP BỊ HỆ THỐNG GIẾT

## Mô tả

`scripts/device-scenario.py`, hàm `killed_by`, bỏ qua dòng "app bị giết" khi dấu của lần chạy hiện tại chưa vào `logcat.txt` (`if len(starts) < attempt`), để lần chạy thứ hai không nhận nhầm cái chết của lần đầu. Nhánh này đúng nhưng chưa có test riêng (ghi chú rà soát T-054, mức nên sửa). Sau task này `fake-adb` có biến làm mất dòng `log -t` sau một số dòng, và một test khóa hành vi của nhánh đó.

## Phạm vi

- `scripts/tests/fake-adb`: biến `FAKE_ADB_LOG_LINES`, chỉ ghi N dòng `log -t` đầu vào `$FAKE_ADB_LOGCAT_FILE`.
- `scripts/tests/run.sh`: một test trong khối T-054 của `device-scenario`.

**Không bao gồm:** sửa code của `scripts/device-scenario.py` (code đúng); các ghi chú khác của rà soát T-054 (`first_run` tính cả thời gian chụp ảnh, tiến trình phụ của app).

## Rủi ro sơ bộ

Chạm điểm chặn 7 (CI, công cụ của phiên): rủi ro cao, cần duyệt chéo. Chỉ thêm biến cho bản giả và một test; không đổi script mà CI chạy.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
