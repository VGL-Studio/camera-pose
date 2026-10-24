---
id: T-037
title: Nhánh task không chỉ nằm trên máy cloud
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-051 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: on-staging
risk: medium
size: S
depends_on: []
sprint:
plan: features/quality-gate/plans/T-037-nhanh-task-khong-mat.md
created: 2026-10-24
---

# T-037: NHÁNH TASK KHÔNG CHỈ NẰM TRÊN MÁY CLOUD

## Mô tả

Phiên con của T-017 làm xong 4 commit nhưng lệnh đẩy bị từ chối, nhánh chỉ nằm trên máy cloud tạm thời (L-051). Sau ticket này, phiên đẩy nhánh task ngay lúc tạo và sau mỗi commit; mọi lần đẩy đi qua `scripts/save-branch.sh`. Đẩy không được thì script gói nhánh thành `git bundle` trong `$HOME/rescue/` trên máy cloud và in đoạn "Nhánh chưa đẩy" (đường dẫn, mã commit, lệnh lấy lại) để chép vào báo cáo; người lấy lại trước khi máy bị thu hồi. Bundle không vào nhánh báo cáo hay artifact (đã quyết khi duyệt plan, mục 8, câu 1: tệp nhị phân vào nhánh chính và vòng qua lần từ chối). Lệnh đẩy bị môi trường từ chối thì phiên không đẩy lại dưới dạng khác, dừng task; hai task liên tiếp bị từ chối thì kết thúc phiên.

## Phạm vi

- `docs/auto-run.md`: bước 1 đẩy nhánh lúc tạo; bước 3, giao việc cho phiên con, bước 7 đẩy qua script; đoạn mới "Khi không đẩy được"; mục 4, mục 5.
- `scripts/save-branch.sh <nhánh>` (và `--bundle-only`): thử đẩy, thử lại khi lỗi mạng; không được thì tạo bundle trong `$HOME/rescue/`, in đường dẫn và lệnh lấy lại; thoát 0 chỉ khi đã đẩy. Có test.
- `docs/templates/run.md`: mục "Nhánh chưa đẩy".

**Không bao gồm:**

- Tìm vì sao bộ phân loại từ chối lệnh đẩy.
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao (plan, điểm chặn 7): đổi quy trình phiên tự chạy; một script mới mà mọi lần đẩy nhánh task đi qua.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
