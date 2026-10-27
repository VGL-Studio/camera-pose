---
id: T-067
title: Phiên tự quyết các lệch nhỏ đã liệt kê, bỏ qua loại task mà môi trường không làm được, rồi báo người sau
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là đề xuất 2a, 2b, 3a sau RUN-20261027-01, Tuấn chọn ngày 2026-10-27
scenarios: []
owner: Tuấn
status: approved
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
- `scripts/plan-check.py`: cách gọi `--touches <tiền tố> <plan>` (mã 0 khi một hàng của bảng mục 3.2 có đường dẫn dưới tiền tố, 1 khi không, 2 khi sai đối số), để biết task nào thuộc loại bị chặn mà không tính chữ của mục 3.2.
- `scripts/tests/run.sh`: 18 test (8 cho bảng tra lỗi: PB-13 ba cách viết, đối chứng một lần từ chối khác, câu liệt kê loại, dòng `bo-qua-loai` có lệnh `--touches`; 10 cho `--touches`: hàng Tạo, Sửa, Xóa, chữ, giữa đường dẫn, bảng của mục khác, tiền tố rỗng, plan không có, T-062, T-067).
- `docs/adr/1005-lech-nho-tu-lam-roi-bao.md` (ADR mới của app, bổ sung ADR-0029, lệch nhỏ L1 tới L10) và `docs/foundation.md` sinh lại.
- `docs/lessons.md`: thêm phần T-067 vào cuối dòng Áp dụng của L-086, không thay chữ đã có (T-066 cũng ghi dòng này).
- `docs/auto-run.md`: mục Khi không đẩy được (tra bảng, bỏ qua theo loại, không kết thúc phiên), dòng kết thúc phiên ở mục 4, mục 5 Báo cáo phiên, bước 6 (rà soát xét lệch).
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

- [ ] PB-13 nhận đúng thông báo thật của RUN-20261027-01, cách viết của GitHub App và personal access token; một lần từ chối khác không khớp.
- [ ] `scripts/plan-check.py --touches` chỉ tính hàng của bảng mục 3.2 (test có plan chỉ nhắc workflow trong chữ).
- [ ] Mọi loại trong bảng tra lỗi có trong câu liệt kê loại (test).
- [ ] ADR-1005 có trong `docs/foundation.md`; `scripts/foundation.py --check` qua.
- [ ] `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/templates/run.md` dẫn ADR-1005 như plan mục 3.1.
- [ ] `scripts/tests/run.sh` và `scripts/check.sh` qua; CI qua, đã merge.
- [ ] Đề xuất về dự án mẫu ghi ở Cần người kiểm tra của báo cáo phiên.
