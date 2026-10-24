---
id: T-036
title: Diễn tập đầu phiên trong đúng môi trường cloud
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-056, L-051, L-002 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: approved
risk: high
size: S
depends_on: [T-030, T-035]
sprint:
plan: features/quality-gate/plans/T-036-dien-tap-dau-phien.md
created: 2026-10-24
---

# T-036: DIỄN TẬP ĐẦU PHIÊN TRONG ĐÚNG MÔI TRƯỜNG CLOUD

## Mô tả

RUN-20261023-03 kết thúc sau 15 phút mà không task nào chạy, vì lỗi Java chỉ lộ khi `check.sh` chạy trên máy cloud (L-049, L-056). RUN-20261023-04 mất nhánh T-017 vì lệnh đẩy bị từ chối ở cuối task (L-051). Sau ticket này, `scripts/cloud-env-check.sh` diễn tập trong vài phút đầu phiên mọi thao tác mà phiên sẽ cần: kiểm tra Java (`java-check.sh`), Gradle khởi động được, đẩy rồi xóa một nhánh thử, bí mật staging không phải file giả; bước nào hỏng thì báo `env-broken` với lý do rõ, trước khi tốn thời gian cho task.

## Phạm vi

- `scripts/cloud-env-check.sh`: gọi `java-check.sh`, `./gradlew help`, đẩy và xóa nhánh `claude/probe-<mã phiên>`.
- Test trong `scripts/tests/run.sh` với `java`, `git` giả.
- `docs/auto-run.md`, kiểm tra đầu phiên: gặp mã lỗi mới thì làm gì.

**Không bao gồm:**

- Kiểm tra bí mật staging thật (đã có ở `preflight.sh`).
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: cổng chất lượng và kiểm tra đầu phiên (điểm chặn 7).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
