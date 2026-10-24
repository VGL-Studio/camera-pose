---
id: T-047
title: Mục "Sẵn sàng phát hành" trên trang tiến độ
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-064 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: planned
risk: high
size: S
depends_on: []
sprint:
plan: features/quality-gate/plans/T-047-san-sang-phat-hanh.md
created: 2026-10-24
---

# T-047: MỤC "SẴN SÀNG PHÁT HÀNH" TRÊN TRANG TIẾN ĐỘ

## Mô tả

Việc cần cho bản production nằm rải rác trong báo cáo các phiên: mã quảng cáo thật còn thiếu, kiểm thủ công chưa làm, lỗi đang mở (L-064). Sau ticket này, `scripts/status.py` sinh mục "Sẵn sàng phát hành" trên `docs/STATUS.md` từ các nguồn đã có trong repo: vị trí quảng cáo trong `AdPlacement` so với danh sách đã ghi cần mã thật, mục "Cần người kiểm tra" chưa đánh dấu trong báo cáo phiên, issue mở, ticket chưa `on-staging`. AI không đọc cấu hình production.

## Phạm vi

- `scripts/status.py` và test.
- `docs/CLAUDE.md` mục Trang tiến độ: một câu.

**Không bao gồm:**

- Đọc `scripts/release.sh` hay cấu hình production.
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Trung bình: script sinh trang, không chặn CI.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
