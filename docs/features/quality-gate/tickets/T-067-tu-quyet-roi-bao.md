---
id: T-067
title: Phiên tự quyết các lệch nhỏ đã liệt kê, bỏ qua loại task mà môi trường không làm được, rồi báo người sau
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là đề xuất 2a, 2b, 3a sau RUN-20261027-01, Tuấn chọn ngày 2026-10-27
scenarios: []
owner: Tuấn
status: planned
risk: high
size: S
depends_on: []
sprint:
plan: features/quality-gate/plans/T-067-tu-quyet-roi-bao.md
created: 2026-10-27
---

# T-067: PHIÊN TỰ QUYẾT CÁC LỆCH NHỎ ĐÃ LIỆT KÊ, RỒI BÁO NGƯỜI SAU

## Mô tả

RUN-20261027-01 dừng T-062 vì GitHub từ chối lệnh đẩy (token của phiên `claude --cloud` không có quyền `workflow`, L-086). Phiên tự bỏ qua T-065 (cũng sửa workflow) và làm tiếp, nhưng tài liệu không nói việc đó được phép. Nhiều lệch nhỏ khác (ngân sách kịch bản, `need` thành `wait`, mốc tài liệu dời chỗ sau đồng bộ dự án mẫu) cũng phải hỏi người hay soạn plan phiên bản mới.

Ticket này cho phiên một danh sách lệch nhỏ được tự làm, cách bỏ qua đúng loại task mà môi trường không làm được, và một mục trong báo cáo để người xem lại các quyết định đó sau phiên.

## Phạm vi

- `docs/ops/failure-playbook.md`: loại mới `bo-qua-loai`; dòng PB-13 cho lệnh đẩy bị từ chối vì thiếu quyền `workflow`; `scripts/tests/playbook/PB-13.log` từ log thật.
- `scripts/tests/run.sh`: 4 test (PB-13 hai cách viết của GitHub, đối chứng một lần từ chối khác, mọi loại trong bảng có trong câu liệt kê loại).
- `docs/adr/1005-lech-nho-tu-lam-roi-bao.md` (ADR mới của app, bổ sung ADR-0029) và `docs/foundation.md` sinh lại.
- `docs/auto-run.md`: mục Khi không đẩy được (tra bảng, bỏ qua theo loại, không kết thúc phiên), mục 5 Báo cáo phiên, bước 6 (rà soát xét lệch).
- `docs/risk-scoring.md` mục 4: đoạn "Không phải dừng: lệch nhỏ đã liệt kê".
- `docs/templates/run.md`: mục "Quyết định AI đã tự làm" và một dòng ở Cần người kiểm tra.

**Không bao gồm:**

- Trường `open_with`, bảng khả năng của cách mở phiên, kiểm `run-check.py`, thử đẩy ở đầu phiên (T-066).
- Ghi patch của nhánh không đẩy được vào nhánh báo cáo, `run-watchdog.yml` (T-068).
- `scripts/local-setup.sh` (T-069).
- `scripts/status.py`: không đổi; dòng `- [ ]` ở mục Cần người kiểm tra đã đưa việc xem lại lên trang tiến độ.

## Rủi ro sơ bộ

Chạm điểm chặn 7 (quy trình phiên tự chạy): đổi khi nào phiên dừng hay đi tiếp. Danh sách lệch được phép viết hẹp, rà soát độc lập kiểm từng lệch; các việc cấm (quyền, SDK, quảng cáo, production, spec, làm yếu test) giữ nguyên.

## Định nghĩa hoàn thành

- [ ] PB-13 nhận đúng thông báo thật của RUN-20261027-01 và cách viết của GitHub App; một lần từ chối khác không khớp.
- [ ] Mọi loại trong bảng tra lỗi có trong câu liệt kê loại (test).
- [ ] ADR-1005 có trong `docs/foundation.md`; `scripts/foundation.py --check` qua.
- [ ] `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/templates/run.md` dẫn ADR-1005 như plan mục 3.1.
- [ ] `scripts/tests/run.sh` và `scripts/check.sh` qua; CI qua, đã merge.
- [ ] Đề xuất về dự án mẫu ghi ở Cần người kiểm tra của báo cáo phiên.
