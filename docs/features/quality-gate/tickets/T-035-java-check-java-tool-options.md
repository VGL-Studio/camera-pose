---
id: T-035
title: java-check.sh đọc đúng phiên bản Java khi môi trường đặt JAVA_TOOL_OPTIONS
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-049 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: on-staging
risk: high
size: S
depends_on: [T-030]
sprint:
plan: features/quality-gate/plans/T-035-java-check-java-tool-options.md
created: 2026-10-23
---

# T-035: JAVA-CHECK.SH ĐỌC ĐÚNG PHIÊN BẢN JAVA KHI MÔI TRƯỜNG ĐẶT JAVA_TOOL_OPTIONS

## Mô tả

Phiên RUN-20261023-03 kết thúc ngay ở kiểm tra đầu phiên, không task nào chạy. Môi trường cloud đặt `JAVA_TOOL_OPTIONS`, nên `java -version` in `Picked up JAVA_TOOL_OPTIONS: ...` trước dòng phiên bản. `scripts/java-check.sh` (T-030) chỉ đọc dòng đầu, không thấy phiên bản và dừng `scripts/check.sh`, dù Java thật là 21 (L-049). Sau ticket này, `java-check.sh` đọc dòng có phiên bản, bỏ qua các dòng `Picked up ...` mà JVM in ra, và `check.sh` chạy được trên môi trường cloud.

## Phạm vi

- `scripts/java-check.sh`: lấy dòng có `version "`, không lấy dòng đầu.
- Test trong `scripts/tests/run.sh`: đầu ra có dòng `Picked up JAVA_TOOL_OPTIONS` và `Picked up _JAVA_OPTIONS`.

**Không bao gồm:**

- Bỏ `JAVA_TOOL_OPTIONS` của môi trường cloud (cấu hình proxy và truststore cho việc tải).
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: cổng chất lượng (`scripts/check.sh` gọi script này, điểm chặn 7). Thay đổi một dòng, có test với `java` giả.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
