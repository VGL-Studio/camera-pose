# KIỂM TRA DANH SÁCH CHẠY TRƯỚC KHI DUYỆT

Phiên tự chạy dừng hay merge sai thường vì những lỗi đã nằm sẵn trong tài liệu từ trước khi phiên bắt đầu: spec mâu thuẫn với plan, hai plan hiểu một file khác nhau, kỹ thuật chưa chạy thử, test không bắt được lỗi thật. Tài liệu này mô tả hai lớp kiểm tra sau khi AI lập danh sách chạy và trước khi dev duyệt (`docs/CLAUDE.md`, mục Khi lập danh sách chạy). Mục tiêu: mọi điều cần người quyết định được quyết trước, để phiên chạy suốt đêm mà không phải dừng.

## 1. Kiểm tra bằng script

`scripts/run-check.py docs/runs/RUN-YYYYMMDD-NN.md` kiểm những gì `scripts/plan-check.py` không thấy khi xem từng plan riêng:

| Kiểm tra | Mức |
| --- | --- |
| Plan có thật, qua `plan-check.py`; plan và ticket đều `approved` (L-019) | Lỗi |
| Spec của ticket `confirmed`, mọi câu hỏi ở mục 9 đã có trả lời | Lỗi |
| Ticket phụ thuộc đã merge, hoặc đứng trước trong danh sách | Lỗi |
| Mọi kịch bản của ticket có ít nhất một dòng ở mục 4 của plan | Lỗi |
| Dòng lịch sử của spec còn ghi "Chờ xác nhận lại" khi spec đã `confirmed` | Chú ý |
| Spec đổi sau ngày plan được duyệt: plan có thể đã cũ | Chú ý |
| Kịch bản có ít dòng test hơn số tiêu chí chấp nhận trong spec | Chú ý |
| Ngoại lệ viết test trước gồm vỏ app (`AppNavHost`, `MainActivity`), nơi lỗi chỉ lộ ra ở `ui-test` (L-030) | Chú ý |
| Hai plan cùng sửa một file mà không phụ thuộc nhau (L-014) | Chú ý |
| Danh sách chỉ có một chuỗi phụ thuộc (L-015) | Chú ý |
| Hai plan cùng ghi Tạo một file, kể cả khi phụ thuộc nhau (L-033) | Chú ý |

Lỗi phải sửa trước khi đưa danh sách cho dev. Mỗi dòng chú ý được xem ở lớp thứ hai và ghi kết luận.

## 2. Rà soát độc lập

Một phiên AI riêng làm việc này, ví dụ một agent con, với ngữ cảnh mới. Phiên này chỉ nhận: danh sách chạy, kết quả `run-check.py`, các plan, ticket, spec, thiết kế và ADR liên quan, cùng nội dung hiện tại của các file trong mục 3.2 của mỗi plan. Phiên không nhận quá trình viết plan, để không mang theo giả định của người viết, giống bước 6 của `docs/auto-run.md`.

Nội dung giao việc tối thiểu: "Rà soát danh sách chạy `<đường dẫn>` theo `docs/ops/pre-run-review.md`, mục 2. Chỉ đọc, không sửa file. Trả về bảng kết quả theo mục 3."

### 2.1. Mâu thuẫn

- **Spec với plan:** mỗi tiêu chí chấp nhận của các kịch bản trong ticket được plan làm đúng nghĩa. Câu trả lời ở mục 9 của plan không trái spec. Plan không làm thêm điều spec không có, nhất là sự kiện phân tích, quyền truy cập, quảng cáo.
- **Plan với plan trong cùng danh sách:** plan sau dựa vào lớp, hàm, file mà plan trước tạo ra thì tên, chữ ký và hành vi phải khớp. Hai plan sửa cùng một file thì xem plan sau có mô tả file theo trạng thái sau khi plan trước đã merge không (L-014).
- **Plan với code hiện tại:** lớp, hàm, khóa chuỗi mà plan nhắc tới phải có thật trên nhánh chính, hoặc do plan phụ thuộc tạo ra.
- **Plan với ADR và thiết kế:** không trái mục "Việc AI phải tuân thủ" của ADR (`docs/foundation.md`); chữ, trạng thái, thứ tự đúng gói bàn giao thiết kế.
- **Spec với spec:** hai feature cùng nói về một hành vi (ví dụ quyền camera ở màn chào và ở camera) thì phải nói giống nhau.

### 2.2. Test chưa đủ tốt

- Mỗi tiêu chí chấp nhận có test sẽ **thất bại nếu hành vi sai**, không chỉ kiểm điều code đang làm. Đọc từng tên test và điều nó khẳng định: nếu bỏ phần code chính đi thì test này có đỏ không?
- Test kiểm kết quả mà người dùng thấy hay dữ liệu được ghi lại, không chỉ kiểm rằng một hàm được gọi.
- Có test cho nhánh lỗi, giá trị biên, trạng thái rỗng, lần thử lại, khi spec nêu các trường hợp đó.
- Tiêu chí về hình ảnh (màu, viền, vị trí) phải kiểm bằng hình ảnh hay số đo, không chỉ bằng cây ngữ nghĩa (L-004, L-010). Nút chữ trên nền khác màu có test đo màu chữ (L-032).
- Test kiểm giá trị mặc định có bước đối chứng, để không qua ngay trên khung rỗng (L-035).
- Khung test tái hiện được nơi phát hiện lỗi: cùng chiều rộng, chiều cao, chế độ đồ họa (`docs/CLAUDE.md`, mục Khi viết plan).
- **Nguy cơ chập chờn:**
  - screenshot của trạng thái có hiệu ứng chạy mãi mà không dừng đồng hồ (L-029, `CLAUDE.md` bước 10);
  - đồng hồ thật, `Thread.sleep`, mạng thật;
  - thứ tự không cố định;
  - test trên emulator khẳng định màn hình thoáng qua (`CLAUDE.md`, bước 10).
  Test chập chờn trên `main-guard` kết thúc cả phiên (L-037), nên đây là mức chặn.
- Ngoại lệ viết test trước có lý do thật; phần vỏ app không có test JVM thì đề xuất test Robolectric (L-030).
- Tiêu chí chỉ kiểm thủ công: xem có cách tự động không; nếu không thì bước kiểm thủ công phải đủ cụ thể để dev làm được trên bản staging.

### 2.3. Chạy được trong phiên

- Kỹ thuật lần đầu dùng (tiêu chí E từ 2) đã chạy thử bằng `scripts/check.sh`, ghi ở mục 4.1 (L-001, L-010).
- Điểm còn chưa chắc có phương án dự phòng ở mục 3.4 với điều kiện chuyển cụ thể (L-039).
- Plan không dùng cách mà `scripts/check.sh` từ chối (L-003, L-013).
- Quyền và thư viện đã được quyết trong mục 3.3:
  - mỗi API Android mà plan gọi có quyền cần thiết trên mọi phiên bản từ `minSdk` tới `targetSdk`;
  - mỗi lớp mới dùng có thư viện trên classpath biên dịch của module (L-007);
  - thư viện mới đã ghi quyền mà nó kéo vào manifest gộp;
  - quyền mới có trong spec.

  Thiếu một trong các điểm này là mức chặn: phiên sẽ dừng ở điều kiện 2 của `risk-scoring.md`, mục 4.
- File R2, bí mật, cấu hình mà task cần đã có trước phiên, và được kiểm bằng cách phiên kiểm được (L-005, L-011, L-012).
- Điều kiện dừng riêng ở mục 7 rõ, kiểm được, không mơ hồ.

## 3. Kết quả và xử lý

Phiên rà soát trả về một bảng:

| Mức | Ticket | Chỗ | Vấn đề | Đề xuất |
| --- | --- | --- | --- | --- |
| Chặn | T-NNN | plan mục 4, dòng `...` | <điều sai và vì sao phiên sẽ dừng hay merge sai> | <sửa plan, sửa spec, hay bỏ task> |

- **Chặn:** phiên gần như chắc chắn sẽ dừng task, hoặc sẽ merge một hành vi sai mà test không bắt được. Phải xử lý trước khi duyệt danh sách:
  - Plan `approved` thì AI soạn phiên bản mới ở trạng thái `draft` để người duyệt lại.
  - Spec sai thì chuyển spec sang `changed` và chờ xác nhận lại.
  - Không kịp xử lý thì bỏ task khỏi danh sách, kèm các task phụ thuộc nó.
- **Nên sửa:** người quyết định sửa ngay hay để phiên chạy như plan đã duyệt.
- **Ghi chú:** ghi lại để người xem khi duyệt.

AI rà soát không sửa plan `approved`, spec `confirmed` hay danh sách chạy. Khi hai tài liệu mâu thuẫn, AI không tự chọn bên nào mà đặt thành câu hỏi cho người (`docs/CLAUDE.md`, mục Đọc trước khi làm).

Kết quả của cả hai lớp được ghi vào mục "Kiểm tra trước khi duyệt" của danh sách chạy ([mẫu](../templates/run.md)): kết quả `run-check.py`, số điểm theo từng mức, và cách xử lý từng điểm chặn. Dev chỉ chuyển danh sách sang `approved` khi `run-check.py` qua và không còn điểm chặn nào chưa xử lý. Plan hay spec đổi sau lần kiểm tra thì chạy lại cả hai lớp.
