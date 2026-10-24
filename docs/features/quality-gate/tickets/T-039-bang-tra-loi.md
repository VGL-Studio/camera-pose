---
id: T-039
title: Bảng tra lỗi đã biết và việc phải làm
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-057, L-050, L-054 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: planned
risk: high
size: M
depends_on: [T-033]
sprint:
plan: features/quality-gate/plans/T-039-bang-tra-loi.md
created: 2026-10-24
---

# T-039: BẢNG TRA LỖI ĐÃ BIẾT VÀ VIỆC PHẢI LÀM

## Mô tả

Mỗi lỗi hạ tầng đã gặp được xử lý riêng, không có một chỗ để phiên tra "thông báo này nghĩa là gì, làm gì tiếp" (L-057). Lỗi R2 hết giờ chờ trong `ui-test` bị tính là lỗi thật (L-050). Sau ticket này, `docs/ops/failure-playbook.md` liệt kê mẫu thông báo lỗi và việc phải làm; `scripts/failure-kind.sh` đọc bảng, in loại lỗi và việc phải làm cho log của một lần chạy CI; `auto-run.md` bước 8 gọi script trước khi sửa code.

## Phạm vi

- `docs/ops/failure-playbook.md`: bảng mẫu (regex), loại, việc phải làm, bài học nguồn.
- `scripts/failure-kind.sh`: đọc bảng; test cho từng dòng của bảng.
- `docs/auto-run.md` bước 8: đỏ thì tra bảng trước; "chạy lại, không tính vào 3 lần sửa" cho lỗi hạ tầng đã biết.

**Không bao gồm:**

- Tự sửa `AppLaunchTest` (L-050, cách khác: ticket riêng nếu cần).
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: `failure-kind.sh` do `main-guard.yml` dùng (cổng chất lượng).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
