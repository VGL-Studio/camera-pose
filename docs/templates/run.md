---
id: RUN-YYYYMMDD-NN
status: draft                # draft | approved | running | finished
sprint: <tên hoặc số sprint>
approved_by: <dev duyệt danh sách và kích hoạt phiên>
max_tasks: <số task tối đa>      # dev quyết định theo ước tính; mặc định bằng số task trong danh sách (docs/CLAUDE.md, mục Khi lập danh sách chạy)
staging_needed_by:           # tùy chọn: thời điểm muốn có bản staging, ví dụ 2026-10-21 08:00
started_at:
finished_at:
staging_build:               # phiên bản bản staging được phát cuối phiên
---

# PHIÊN TỰ CHẠY RUN-YYYYMMDD-NN

## 1. Danh sách chạy

<!-- AI lập sau họp đánh giá rủi ro. Dev xem lại và chuyển trạng thái sang approved. -->

| Thứ tự | Ticket | Feature | Plan (phiên bản) | Rủi ro | Phụ thuộc | Ghi chú |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | T-NNN | <feature> | `features/<feature>/plans/T-NNN-....md` (1) | Thấp | | |
| 2 | T-NNN | <feature> | `...` (2) | Cao | T-NNN | Đã duyệt chéo |

**Lý do của thứ tự chạy:** <vì sao task này chạy trước task kia: phụ thuộc, ảnh hưởng chéo, rủi ro.>

### Kiểm tra trước khi duyệt

<!-- AI điền sau khi lập danh sách, theo docs/ops/pre-run-review.md. Dev chỉ duyệt khi script qua và không còn điểm chặn chưa xử lý. Plan hay spec đổi sau đó thì kiểm tra lại. -->

- **`scripts/run-check.py`:** <qua, hoặc các lỗi đã sửa>; chú ý: <từng dòng và kết luận>
- **Rà soát độc lập:** <ngày, số điểm chặn, nên sửa, ghi chú>

| Mức | Ticket | Vấn đề | Xử lý |
| --- | --- | --- | --- |
| Chặn | T-NNN | <...> | <plan phiên bản mới, spec chờ xác nhận lại, hay bỏ task> |

## 2. Kiểm tra đầu phiên

<!-- AI điền khi bắt đầu phiên. -->

- [ ] Nhánh chính đang xanh, không có issue hay PR đang mở mang nhãn `main-broken`.
- [ ] Mọi plan và ticket trong danh sách có trạng thái `approved`.
- [ ] Các ticket phụ thuộc đã sẵn sàng.

Task bị bỏ qua: <mã ticket và lý do, hoặc "Không có">

## 3. Báo cáo phiên

<!-- AI viết khi kết thúc phiên. Đây là thứ dev đọc đầu tiên khi bắt đầu làm việc lại. -->

### Tóm tắt

<Hai đến ba câu: bao nhiêu task merge, bao nhiêu task dừng, điều quan trọng nhất cần người xem.>

### Nhánh chưa đẩy

<Không có. Hoặc chép đoạn "Nhánh chưa đẩy" mà `scripts/save-branch.sh` in ra: nhánh, mã commit, đường dẫn bundle, lệnh lấy lại (auto-run.md, bước 7).>

### Kết quả từng task

| Ticket | Kết quả | PR | Test viết trước | Rà soát độc lập | CI | Ghi chú |
| --- | --- | --- | --- | --- | --- | --- |
| T-NNN | Đã merge | <liên kết> | 8 test, đã thất bại đúng lý do | 0 chặn, 1 ghi chú | Xanh | |
| T-NNN | Dừng ở bước 8 | <liên kết> | | | Đỏ sau 3 lần sửa | Xem nhật ký plan |

### Cần người kiểm tra

- [ ] Ảnh chuẩn screenshot mới: <màn hình, đường dẫn ảnh>
- [ ] Ảnh so sánh với thiết kế: <feature, `docs/features/<feature>/design/review/`, nhận xét của rà soát độc lập>
- [ ] Tiêu chí kiểm tra thủ công: <ticket, tiêu chí, các bước>
- [ ] Điểm rà soát mức ghi chú: <ticket, nội dung>
- [ ] Test chập chờn: <issue `flaky` đang mở, tên test>, hoặc "Không có" (ADR-1002)
- [ ] Task bị revert: <ticket, issue `main-broken`, PR revert>, hoặc "Không có" (ADR-1002)

### Task bị dừng

| Ticket | Dừng ở bước | Lý do | Đề xuất hướng xử lý | Đã chuẩn bị cho người |
| --- | --- | --- | --- | --- |
| | | | | Plan phiên bản <n> (`draft`); PR sửa nguyên nhân #<số> (nháp, `needs-human`) hoặc "Không cần"; danh sách chạy `RUN-...` (`draft`) |

### Kiểm thử trên máy ảo

<!-- docs/ops/test-sessions.md, mục 4. Ghi "Không chạy (không có task merge)" khi không có task nào merge. -->

- Workflow `device-session.yml`: <lần chạy, commit, kết quả>
- Tín hiệu: crash <n>, ANR <n>, lỗi của app <n>, cảnh báo <n>; khởi động nguội <ms, ms, ms>
- Phát hiện: <mỗi dòng một phát hiện, mức, bằng chứng ngắn, phản hồi FB-... đã soạn; hoặc "Không có">
- Quyết định: phát staging / không phát vì <crash hoặc ANR>

### Bản staging

- Phiên bản: <...>
- Kênh phát: Firebase App Distribution / Internal testing
- Gồm các ticket: <...>

### Thời gian và mức sử dụng

- Bắt đầu: <...>, kết thúc: <...>, lý do kết thúc: <hết danh sách / đến giờ / hết hạn mức / nhánh chính đỏ>
- Mức sử dụng: <nếu đo được>

### Tiếp tục sau khi hết hạn mức

<!-- docs/auto-run.md, mục 4. Ghi "Không có" khi phiên chạy liền một mạch. -->

| Lần | Dừng lúc (commit cuối) | Tiếp tục lúc | Chờ | Đang làm task nào, ở bước nào |
| --- | --- | --- | --- | --- |
| | | | | |

### Token

<!-- docs/ops/token-usage.md. Dán bảng của scripts/token-usage.py report; ghi "không đo được" kèm thông báo nếu script không tìm thấy transcript. Ước tính chỉ để lập kế hoạch, không phải giới hạn. -->

<bảng của scripts/token-usage.py>

Task lệch ước tính từ 2 lần trở lên: <task, lý do có thể; hoặc "Không có">

### Số liệu

<!-- Đầu vào để hiệu chỉnh auto-run.md, mục 8. Ghi "không đo được" nếu không có. -->

| Chỉ số | Giá trị |
| --- | --- |
| Thời gian kiểm tra đầu phiên (gồm `scripts/check.sh`) | |
| Thời gian từng task, từ tạo nhánh tới merge hoặc dừng | T-NNN: tổng; AI làm (tạo nhánh tới đẩy PR); chờ CI |
| Thời gian chờ ở cuối phiên: `device-session`, `staging`; phần báo cáo viết trong lúc chờ | |
| Số vòng CI của từng task | T-NNN: |
| Số lần sửa vì lỗi thật | |
| Số lần thử lại vì lỗi hạ tầng, tổng thời gian chờ | |
| Thời gian build và phát staging | |
| Mức sử dụng | |
| Tổng token của phiên, phần chung ngoài task | |

**Dòng thời gian** (giờ UTC, ghi ngay khi xong mỗi khâu, kể cả khi phiên dừng giữa chừng):

| Giờ | Khâu |
| --- | --- |
| | Bắt đầu phiên |
| | Kiểm tra đầu phiên xong (`check.sh` chạy nền, auto-run.md mục 2) |
| | T-NNN: tạo nhánh |
| | T-NNN: test viết trước xong |
| | T-NNN: đẩy PR |
| | T-NNN: CI xong (`wait-run.sh`) |
| | T-NNN: merge / dừng |
| | Khởi động `device-session`; xong |
| | Khởi động `staging`; xong |
| | Mở PR báo cáo |

### Bài học

<!-- auto-run.md, mục 6. Mỗi bài học cũng được thêm vào docs/lessons.md với trạng thái proposed trong PR báo cáo. Ghi "Không có" nếu không có. -->

| Mã | Chuyện gì | Bằng chứng | Đề xuất sửa | Loại, phạm vi |
| --- | --- | --- | --- | --- |
| L-NNN | | | | |

**Kiểm chứng bài học cũ:** <bài học `applied` nào liên quan tới phiên này, có hiệu quả không, bằng chứng; hoặc "Không có">

### Đề xuất cho buổi họp tiếp theo

1. <...>
