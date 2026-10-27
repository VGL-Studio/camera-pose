---
id: T-062
title: Emulator của `device-session` có camera sau giả, để kịch bản camera chạy được trên CI
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là mục Đề xuất của báo cáo RUN-20261026-03 và câu hỏi 2 của plan T-058
scenarios: []
owner: Tuấn
status: on-staging
risk: high
size: S
depends_on: [T-058]
sprint:
plan: features/quality-gate/plans/T-062-camera-gia-tren-emulator-ci.md
created: 2026-10-26
---

# T-062: EMULATOR CỦA `DEVICE-SESSION` CÓ CAMERA SAU GIẢ, ĐỂ KỊCH BẢN CAMERA CHẠY ĐƯỢC TRÊN CI

## Mô tả

Emulator của `device-session.yml` chạy với `-camera-back none`, nên màn camera báo không mở được camera và hai tệp kịch bản cần camera (`T-020-camera-mo-gap.txt`, `T-058-chup-tu-chi-tiet.txt`) luôn `bỏ qua (môi trường)`. Lỗi mức chặn FB-20261026-01 vì vậy chỉ kiểm được trên máy thật. Sau ticket này emulator có camera sau giả (`-camera-back emulated`), hai tệp chạy thật ở `device-session` cuối mỗi phiên, và log của phiên không còn hàng trăm dòng "Camera could not open". Tuấn chọn làm ngày 2026-10-26.

## Phạm vi

- `.github/workflows/device-session.yml`: `-camera-back none` thành `-camera-back emulated`.
- Bỏ câu "cần camera sau, emulator CI bỏ qua" ở dòng `criterion:` của `T-058-chup-tu-chi-tiet.txt`.
- `docs/ops/test-sessions.md`: ghi emulator CI có camera sau giả, ảnh xem trước là cảnh giả, không nhận dạng được dáng người.

**Không bao gồm:**

- `pr-check.yml` (job `ui-test`): giữ `-camera-back none` (plan mục 8).
- Kịch bản cần nhận dạng dáng người (điểm khớp, lời nhắc): camera giả không có người; vẫn kiểm trên máy thật.
- Đề xuất về dự án mẫu: ghi vào mô tả PR theo `docs/ops/from-app.md`, người mở issue.

## Rủi ro sơ bộ

Chạm CI (`.github/`), điểm chặn 7 của `risk-scoring.md`: rủi ro cao, duyệt chéo. Thay đổi một tham số của emulator; `device-session` đỏ chỉ khi app crash hay ANR.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
