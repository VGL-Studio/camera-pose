---
id: T-052
title: Quy tắc 11 của plan-check thành lỗi; `fresh` sau khi trả máy không phải mở lại app
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là đề xuất của báo cáo RUN-20261025-02 và FB-20261025-02
scenarios: []
owner: Tuấn
status: backlog
risk: high
size: S
depends_on: [T-049]
sprint:
plan: features/quality-gate/plans/T-052-quy-tac-11-va-fresh-sau-tra-may.md
created: 2026-10-25
---

# T-052: QUY TẮC 11 CỦA PLAN-CHECK THÀNH LỖI; `fresh` SAU KHI TRẢ MÁY KHÔNG PHẢI MỞ LẠI APP

## Mô tả

`device-session` cuối RUN-20261025-02 có cả 3 kịch bản emulator không `lỗi` (T-022 đạt, T-024 đạt, T-020 bỏ qua vì môi trường), đúng điều kiện mà plan T-048 mục 6 đặt ra để quy tắc 11 của `scripts/plan-check.py` (dòng "Kiểm tra thủ công" phải ghi nơi chạy, tệp kịch bản phải qua `check`) thành lỗi với plan `draft`. Cùng lần chạy đó, `fresh` ở tệp thứ hai và thứ ba phải mở app lại (FB-20261025-02), mỗi tệp tốn thêm khoảng 20 giây. Sau ticket này, plan nháp thiếu nơi chạy của dòng kiểm thủ công bị `plan-check.py` chặn, và `fresh` của tệp đứng sau một lần trả máy mở được app ngay lần đầu.

## Phạm vi

- `scripts/plan-check.py`: quy tắc 11 là lỗi với plan `draft`, vẫn là chú ý với plan `approved` (như quy tắc 7). Bốn test có sẵn của quy tắc 11 đổi kết quả mong đợi từ `pass` sang `stop`: đây là thay đổi hành vi mà ticket đặt ra, plan liệt kê từng test.
- `scripts/device-scenario.py`: `fresh` của tệp đứng sau một lần trả máy chờ `DEVICE_SCENARIO_STOP_SETTLE` giây (mặc định 3) sau khi dừng app rồi mới mở (FB-20261025-02).
- Chú thích dòng 14 của kịch bản T-024: khung mở gập mới không đóng khung app, thư viện 3 cột.
- Hai ghi chú rẻ của rà soát T-049: câu "Sau mỗi tệp, trình chạy trả máy về gốc" của `docs/ops/test-sessions.md` nhắc cài đặt giới thiệu đóng khung; hai dòng chú thích quá dài của `scripts/tests/fake-adb`.

**Không bao gồm:**

- Lệnh đếm số cột: cú pháp `count` hiện tại không đếm được cột (plan mục 2).
- Các ghi chú khác của rà soát T-049 (plan mục 2).
- Tệp `.github/`, `scripts/device-session.sh`, app.

## Rủi ro sơ bộ

Chạm điểm chặn 7 (CI và quy trình phiên tự chạy): `plan-check.py` chạy với mọi plan, trình chạy kịch bản chạy ở `device-session` cuối mỗi phiên. Cách sửa `fresh` chỉ kiểm thật được ở `device-session` sau khi merge.

## Định nghĩa hoàn thành

- [ ] Plan nháp có dòng kiểm thủ công thiếu nơi chạy: `plan-check.py` thoát 1, dòng `LỖI: quy tắc 11`; plan đã duyệt chỉ có `CHÚ Ý`.
- [ ] `fresh` của tệp sau một lần trả máy ghi `note ... fresh: chờ 3 giây sau khi dừng app` và không phải mở lại ở `device-session` đầu sau khi merge.
- [ ] Có test tự động cho phần logic chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
