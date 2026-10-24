---
id: T-036
title: Diễn tập đầu phiên trong đúng môi trường cloud
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-056, L-051, L-002 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: on-staging
risk: high
size: S
depends_on: [T-030, T-035]
sprint:
plan: features/quality-gate/plans/T-036-dien-tap-dau-phien.md
created: 2026-10-24
---

# T-036: DIỄN TẬP ĐẦU PHIÊN TRONG ĐÚNG MÔI TRƯỜNG CLOUD

## Mô tả

RUN-20261023-03 kết thúc sau 15 phút mà không task nào chạy, vì lỗi Java chỉ lộ khi `check.sh` chạy trên máy cloud (L-049, L-056). RUN-20261023-04 mất nhánh T-017 vì lệnh đẩy bị từ chối ở cuối task (L-051). Sau ticket này, `scripts/cloud-env-check.sh` diễn tập trong vài phút đầu phiên những gì `scripts/check.sh` sẽ cần: Java chạy được Gradle của dự án (`java-check.sh`), Gradle wrapper khởi động được (`./gradlew --version`), Android SDK có nền tảng `compileSdk`. Bước nào hỏng thì script thoát 1 với lý do và việc người cần làm, phiên kết thúc ở kiểm tra đầu phiên, và `env-canary` mở issue `env-broken`, trước khi tốn thời gian cho task. Phần đẩy và xóa nhánh thử đã bỏ khi duyệt plan (mục 8, câu 1): lần từ chối của L-051 đến từ bộ phân loại lệnh của phiên, script không tái hiện được; T-037 lo phần này.

## Phạm vi

- `scripts/cloud-env-check.sh`: gọi `java-check.sh`, `./gradlew --version` (giới hạn 300 giây), kiểm `platforms;android-<compileSdk>`; thông báo theo từng vấn đề.
- `scripts/env-canary.sh`: câu lý do khi `cloud-env-check.sh` đỏ.
- Test trong `scripts/tests/run.sh` với `java`, `gradlew` giả.
- `docs/auto-run.md`, mục 2: dòng `cloud-env-check.sh` của bảng kiểm tra đầu phiên.

**Không bao gồm:**

- Kiểm tra bí mật staging thật (đã có ở `preflight.sh`).
- Đẩy và xóa nhánh thử (bỏ khi duyệt plan, xem Mô tả).
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: cổng chất lượng và kiểm tra đầu phiên (điểm chặn 7).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
