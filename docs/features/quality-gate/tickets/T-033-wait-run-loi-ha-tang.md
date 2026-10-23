---
id: T-033
title: wait-run.sh nhận ra job GitHub không chạy (hạn mức Actions) hay PR xung đột, và phiên tự tiếp tục sau đó
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-046 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: planned
risk: high
size: S
depends_on: [T-029]
sprint:
plan: features/quality-gate/plans/T-033-wait-run-loi-ha-tang.md
created: 2026-10-23
---

# T-033: WAIT-RUN.SH NHẬN RA JOB GITHUB KHÔNG CHẠY VÀ PHIÊN TỰ TIẾP TỤC SAU ĐÓ

## Mô tả

Ngày 2026-10-23 GitHub không cấp máy cho mọi job vì hết hạn mức Actions. `scripts/wait-run.sh` chỉ báo `ĐỎ` kèm tên job, nên một phiên tự chạy sẽ hiểu là CI hay nhánh chính đỏ thật: sửa code vô ích, chạy lại, có thể còn revert một task tốt (L-046). Sau ticket này:
- `wait-run.sh` thấy job đỏ mà không chạy bước nào thì báo `HẠ TẦNG` kèm chú thích của GitHub, mã thoát 4;
- `wait-run.sh --pr` thấy PR xung đột với nhánh chính (GitHub không chạy CI cho PR đó) thì báo `XUNG ĐỘT` ngay, mã thoát 5, thay vì chờ 40 phút (L-048);
- phiên gặp mã 4 thì không sửa code, không revert, kết thúc mà giữ danh sách chạy `running`;
- `resume-run.yml` kích hoạt lại phiên khi GitHub chạy lại được, và phiên chạy lại các lần chạy bị bỏ trước khi đi tiếp.

## Phạm vi

- `scripts/wait-run.sh`: mã thoát 4 (`HẠ TẦNG`) và 5 (`XUNG ĐỘT`).
- Test trong `scripts/tests/run.sh`.
- `docs/auto-run.md`: mục Chờ workflow, bước 9, mục 4 (bảng kết thúc phiên và Tiếp tục phiên).

**Không bao gồm:**

- `merge-if-green.sh`: vẫn từ chối khi có check thất bại, đúng như hiện nay.
- Báo cho người qua issue: chính workflow mở issue cũng không chạy được khi hết hạn mức.
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: quy trình phiên tự chạy (điểm chặn 7). Thay đổi nhỏ, logic có test với `gh` giả.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
