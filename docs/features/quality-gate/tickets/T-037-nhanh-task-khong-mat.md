---
id: T-037
title: Nhánh task không chỉ nằm trên máy cloud
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-051 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: planned
risk: medium
size: S
depends_on: []
sprint:
plan: features/quality-gate/plans/T-037-nhanh-task-khong-mat.md
created: 2026-10-24
---

# T-037: NHÁNH TASK KHÔNG CHỈ NẰM TRÊN MÁY CLOUD

## Mô tả

Phiên con của T-017 làm xong 4 commit nhưng lệnh đẩy bị từ chối, nhánh chỉ nằm trên máy cloud tạm thời (L-051). Sau ticket này, phiên đẩy nhánh task ngay lúc tạo và sau mỗi commit; đẩy bị từ chối thì ghi `git bundle` của nhánh vào nhánh báo cáo của phiên (hoặc artifact của workflow) và ghi lệnh lấy lại vào báo cáo, để người không phải vào máy cloud.

## Phạm vi

- `docs/auto-run.md`, bước 3, 4, 7: đẩy nhánh lúc tạo và sau mỗi commit.
- Script nhỏ `scripts/save-branch.sh <nhánh>`: thử đẩy; không được thì tạo bundle và in đường dẫn, lệnh lấy lại. Có test.

**Không bao gồm:**

- Tìm vì sao bộ phân loại từ chối lệnh đẩy.
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Trung bình: quy trình phiên tự chạy, một script mới không thuộc cổng chất lượng.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
