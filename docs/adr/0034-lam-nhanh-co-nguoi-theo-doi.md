---
id: ADR-0034
title: Việc nhỏ, rủi ro thấp của code app được làm ngay ban ngày theo đường làm nhanh, AI làm cùng dev, vẫn đủ yêu cầu, ticket, plan, nhật ký, CI và merge-if-green.sh
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-29
deciders: [Nhật]
supersedes:                  # không thay ADR nào; thêm một đường đi bên cạnh phiên tự chạy (docs/auto-run.md), thêm ngoại lệ cho quy tắc người duyệt của docs/risk-scoring.md mục 3 và quy tắc xác nhận spec của docs/CLAUDE.md
superseded_by:
---

# ADR-0034: VIỆC NHỎ, RỦI RO THẤP CỦA CODE APP ĐƯỢC LÀM NGAY BAN NGÀY THEO ĐƯỜNG LÀM NHANH, AI LÀM CÙNG DEV, VẪN ĐỦ YÊU CẦU, TICKET, PLAN, NHẬT KÝ, CI VÀ MERGE-IF-GREEN.SH

## Bối cảnh

Code của app hiện chỉ có một đường lên nhánh chính: phiên tự chạy, thường chạy qua đêm. Ban ngày dev hay muốn sửa ngay một chỗ nhỏ hay thêm một tính năng nhỏ. Đi đủ đường của phiên tự chạy thì chậm:

- T-048 (dọn rewarded, cỡ S) cần 5 PR: ticket (#231), plan (#239), danh sách chạy (#241), code (#242), báo cáo phiên (#245).
- Mỗi phiên tốn khoảng 30 phút cố định (kiểm đầu phiên, emulator, staging, báo cáo), chưa kể thời gian soạn, rà soát và duyệt plan, danh sách chạy.
- Các bước đó có vì phiên chạy không có người (ADR-0031): mọi quyết định phải nằm sẵn trong tài liệu đã duyệt. Khi dev ngồi cùng AI, dev trả lời và quyết ngay được, nên phần lớn các bước duyệt và rà soát trước khi chạy không còn cần.

Việc về script, tài liệu và quy trình đã làm ban ngày trong phiên có người theo dõi, bằng PR thường (ví dụ #217, #246, #247). ADR này không đổi cách làm đó; nó chỉ mở đường cho code app.

Không thể bỏ tài liệu: không có yêu cầu, ticket, plan và nhật ký thì không theo dõi được lịch sử phát triển app, `scripts/status.py`, retro và bài học mất dấu, và sau này không ai biết vì sao code thành như vậy. Không thể bỏ CI và `merge-if-green.sh`: test trên emulator (`ui-test`) và bản release có R8 chỉ chạy trong CI, và nhánh chính đỏ thì phiên đêm sau hỏng cả đêm.

## Quyết định

**1. Có đường làm nhanh bên cạnh phiên tự chạy, cho code app.** Dev và AI làm cùng nhau, ngay trên máy dev, trong phiên có người theo dõi. AI làm gần như mọi việc; dev trả lời câu hỏi, đồng ý plan một lần và xem kết quả.

**2. Điều kiện vào đường làm nhanh.** Đủ mọi điều sau; thiếu một điều thì đi đường của phiên tự chạy.

- Dev ngồi cùng suốt task, tới khi `main-guard` của commit merge xanh, và trả lời được ngay.
- Plan không chạm điểm chặn nào ở mục 1 của `risk-scoring.md`, kể cả điểm 7 (module `:core:*`, cấu hình build, CI, quy trình phiên tự chạy). Không thêm quyền, thư viện, SDK.
- Mức rủi ro Thấp (tổng 0 đến 4), tiêu chí E dưới 2: không có kỹ thuật mới cần chạy thử (mục 4.1 của plan).
- Ticket cỡ S, hoặc M khi dev đồng ý.
- Kịch bản đã có trong spec `confirmed`, hoặc dev ngồi cùng là người xác nhận spec của feature đó (quyết định 4).
- Lúc bắt đầu: không có phiên tự chạy nào đang chạy (danh sách `running` trên một nhánh `claude/RUN-*-report`, cách tra như `scripts/merge-if-green.sh`), không có issue `main-broken` hay `env-broken` đang mở.

**3. Vẫn làm như đường chính thống.**

- **Yêu cầu:** lời dev nói trong phiên được ghi thành file yêu cầu trong `features/<feature>/requests/`, giữ nguyên ý của dev; có spec thì yêu cầu chuyển `specified`.
- **Ticket** theo mẫu ticket, **plan** theo đúng mẫu plan, đủ các mục, kể cả mục 3.5 "Hành vi đã có bị đổi" (`scripts/existing-tests.py`) và chấm rủi ro; frontmatter có `lane: quick`. Chạy `scripts/plan-check.py` khi plan còn `draft` và sửa hết lỗi. Mã ticket và số bài học đã có người dùng (PR song song) thì lấy số sau và sửa mọi chỗ dẫn, như L10 của ADR-0032.
- **Test viết trước**, thất bại đúng lý do, commit trước code (`docs/auto-run.md`, bước 3).
- **Kiểm tra cục bộ** bằng `scripts/check.sh` (bước 5).
- **Rà soát độc lập** bằng một agent con ngữ cảnh mới, chỉ nhận plan, spec, thiết kế, ADR và phần thay đổi, theo nội dung và ba mức của bước 6. Lời giao có dòng `Task: T-NNN` để token tính vào task.
- **CI** (`pr-check.yml`) và **merge** chỉ bằng `scripts/merge-if-green.sh`, rồi chờ `main-guard` như bước 8, 9.
- **Nhật ký thực hiện** ở cuối plan, plan `done`, ticket `on-staging`.
- **Bài học** vào `docs/lessons.md` khi gặp điều đáng ghi, như mọi phiên.

**4. Duyệt trong phiên.**

- **Plan:** dev ngồi cùng đọc tóm tắt plan (phạm vi, danh sách file, test, điểm rủi ro, câu hỏi) và đồng ý rõ ràng. Dev nào cũng duyệt được, không cần là chủ feature; đây là ngoại lệ của `risk-scoring.md` mục 3 cho đường làm nhanh. AI chuyển plan sang `approved`, ghi `reviewer` là dev, `approved_at`, mục 9 ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý của dev, và ghi khi người duyệt không phải `owner` của ticket. Ticket theo plan (`scripts/status.py --sync`).
- **Spec:** việc chưa có trong spec chỉ đi đường làm nhanh khi dev ngồi cùng là người xác nhận spec của feature đó. AI sửa spec, dev xác nhận rõ ràng trong phiên, AI ghi `status: confirmed`, `confirmed:` và mục lịch sử kèm lời xác nhận của dev.
- Đây là các ngoại lệ duy nhất của quy tắc "AI không tự chuyển trạng thái thay cho người", và chỉ khi dev đã đồng ý trong chính phiên đó.

**5. Được bỏ.** PR riêng cho ticket, plan, danh sách chạy; họp đánh giá rủi ro; `scripts/run-check.py` và rà soát trước khi chạy (`docs/ops/pre-run-review.md`); kích hoạt phiên (`start-run.yml`, routine, `claude --cloud`), kiểm tra đầu phiên, nhánh báo cáo, báo cáo phiên, hàng chờ plan cuối phiên.

**6. Một PR cho cả task.** Ngoại lệ của `docs/auto-run.md` bước 7 (PR của task không chứa tài liệu) và bước 10 (nhật ký ghi trên nhánh báo cáo).

- Nhánh `claude/quick-T-NNN-<tu-khoa>`, tạo từ nhánh chính mới nhất. Tên nhánh không bắt đầu bằng `claude/T-` để `merge-if-green.sh` từ chối PR khi có phiên tự chạy đang chạy, như mọi PR không thuộc phiên.
- PR chứa yêu cầu, spec nếu đổi, ticket, plan, test, code, ảnh chuẩn và nhật ký. Tiêu đề `T-NNN: <tên task>`. Mô tả ghi "Đường làm nhanh (ADR-0034)", người duyệt plan, kết quả `plan-check.py`, kết quả rà soát độc lập, và những gì đường thường ghi vào báo cáo phiên: mục "Ngoài mục 3.2", "Đã sửa theo rà soát", "Test cũ viết lại (ADR-0031)", điểm nên sửa chưa sửa, việc cần người kiểm.
- Nhật ký (tiêu đề `Làm nhanh YYYY-MM-DD, phiên bản plan N`), plan `done` và ticket `on-staging` ghi trong lần đẩy cuối, sau rà soát độc lập và trước CI, để CI chỉ chạy một vòng. Dòng kết quả ghi "merge bằng PR này", dòng CI ghi "CI của PR này". CI đỏ thì sửa và cập nhật nhật ký trong cùng lần đẩy sửa. PR không merge thì các trạng thái đó cũng không vào nhánh chính.

**7. Trước khi merge, kiểm danh sách chạy chưa chạy.** Trên nhánh làm nhanh, chạy `scripts/plan-check.py` cho mọi plan của các danh sách chạy `draft` hay `approved` chưa chạy, và `scripts/run-check.py` cho các danh sách đó. Có lỗi mới so với nhánh chính (file plan định tạo đã có, test hay script đọc file đã đổi, test có sẵn ở mục 3.5 đã đổi) thì báo dev; dev chọn sửa plan đó trước, merge rồi soạn plan phiên bản mới, hay chờ. Danh sách `draft` đã rà soát trước khi chạy thì báo dev nó cần rà soát lại. Nhắc dev không kích hoạt phiên tự chạy khi PR làm nhanh còn mở.

**8. Trong lúc làm, hỏi dev thay vì tự quyết.** Các lệch mà phiên tự chạy tự làm rồi báo (ADR-0029, ADR-0032, ADR-0033) được hỏi dev trước, rồi ghi vào dòng "Lệch so với plan" của nhật ký.

**9. Khi dừng hay bỏ task.** Gặp điều kiện dừng (`risk-scoring.md`, mục 4), thấy task chạm điểm chặn, hay dev bỏ task:

- Đóng PR làm nhanh, giữ nhánh.
- Mở một PR tài liệu từ nhánh chính, merge bằng `merge-if-green.sh`, gồm: yêu cầu, spec nếu đã xác nhận, ticket `stopped`, plan với nhật ký của lần dừng. Muốn làm tiếp thì plan có phiên bản mới `draft` như `docs/auto-run.md`, mục Chuẩn bị cho người khi task dừng, và mục 3.1 ghi cách tiếp tục trên nhánh `claude/T-NNN-<tu-khoa>` tạo từ nhánh làm nhanh, hay làm lại từ đầu; task đi đường thường.

**10. Nhánh chính đỏ sau khi merge.** `main-guard` mở issue `main-broken` mà commit gây lỗi là commit merge của PR làm nhanh: AI cùng dev revert như `docs/auto-run.md`, mục Khi nhánh chính đỏ vì task của phiên (PR từ nhánh `revert/<sha>`, merge bằng `merge-if-green.sh`, ADR-0025). Sau đó một PR tài liệu chuyển ticket sang `stopped` và ghi "đã merge rồi revert" vào nhật ký plan. Commit gây lỗi không phải của PR làm nhanh thì không revert, báo dev.

**11. Số liệu và kiểm tra sau merge.**

- Token: `scripts/token-usage.py mark T-NNN` khi bắt đầu, `mark chung` khi xong, rồi `scripts/token-usage.py report --since <giờ bắt đầu> --run QUICK-YYYYMMDD-T-NNN --csv docs/metrics/token-usage.csv`; tệp CSV đi cùng PR tài liệu kế tiếp (`docs/ops/token-usage.md`).
- Dòng kiểm tra thủ công của plan: `(emulator: ...)` thì chạy tay `device-session.yml` sau khi merge, hoặc dev thử ngay trên máy; `(máy thật: ...)` và `(người: ...)` thì dev kiểm trong phiên khi được, kết quả ghi vào nhật ký. Muốn có bản staging ngay thì chạy tay `staging.yml`.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Đường làm nhanh, đủ tài liệu, dev duyệt trong phiên, giữ CI và `merge-if-green.sh` (đã chọn) | Một PR, không chờ phiên; lịch sử đầy đủ; cổng chất lượng như cũ | AI có thêm một quy trình phải nhớ; dev phải ngồi cùng |
| Giữ như cũ, việc nhỏ cũng chờ phiên tự chạy | Một đường duy nhất | Việc 15 phút chờ tới hôm sau, tốn nhiều PR |
| Làm nhanh không ticket, không plan, chỉ mô tả PR | Nhanh nhất | Mất lịch sử; `status.py`, retro, bài học không thấy; plan sau không đọc được việc trước |
| Làm nhanh, bỏ cả CI, chỉ chạy `check.sh` trên máy | Không chờ CI | `ui-test` và R8 chỉ chạy trong CI; trái quy tắc chỉ merge bằng `merge-if-green.sh`; nhánh chính đỏ làm hỏng phiên đêm |
| Phiên tự chạy ban ngày với danh sách một task | Không cần quy tắc mới | Vẫn đủ chi phí cố định của phiên và các PR duyệt |
| Chỉ chủ feature duyệt plan làm nhanh | Khớp `risk-scoring.md` mục 3 | Chậm khi chủ feature không ngồi cùng; người quyết chọn dev nào cũng duyệt được |

## Hệ quả

- **Tích cực:**
  - Việc nhỏ lên nhánh chính trong cùng buổi, bằng một PR và một vòng CI.
  - Mọi việc, dù đi đường nào, đều có yêu cầu, ticket, plan và nhật ký.
  - Cổng chất lượng không đổi; `merge-if-green.sh` tự chặn đường làm nhanh khi phiên tự chạy đang chạy.
- **Phải chấp nhận:**
  - Plan rủi ro thấp được duyệt bằng lời trong phiên, không qua họp, và có thể không do chủ feature duyệt.
  - Không có lượt rà soát cả danh sách trước khi chạy; ảnh hưởng tới danh sách sắp chạy do AI kiểm bằng script và báo dev.
  - Không có báo cáo phiên; muốn xem việc làm nhanh thì đọc nhật ký plan và PR (`lane: quick`).
  - Dừng hay revert cần thêm một PR tài liệu.
- **Việc AI phải tuân thủ:** đường làm nhanh chỉ cho code app, chỉ khi dev ngồi cùng suốt task tới khi `main-guard` xanh, plan không chạm điểm chặn nào kể cả điểm 7, không thêm quyền, thư viện hay SDK, mức rủi ro Thấp và tiêu chí E dưới 2, ticket cỡ S hoặc M khi dev đồng ý, kịch bản có trong spec `confirmed` hoặc dev ngồi cùng là người xác nhận spec của feature; không bắt đầu khi có phiên tự chạy đang chạy hay issue `main-broken`, `env-broken` đang mở; vẫn ghi yêu cầu, ticket, plan đủ mục theo mẫu với `lane: quick`, chạy `scripts/plan-check.py` khi plan còn `draft` và sửa hết lỗi; chỉ chuyển plan sang `approved`, ghi mục 9, hay chuyển spec sang `confirmed` sau khi dev đồng ý rõ ràng trong chính phiên đó, ghi người duyệt, ngày và lời đồng ý; dev nào ngồi cùng cũng duyệt được plan làm nhanh, ghi khi người duyệt không phải `owner` của ticket; vẫn viết test trước, chạy `scripts/check.sh`, rà soát độc lập bằng agent con ngữ cảnh mới theo bước 6 của `docs/auto-run.md`, lời giao có dòng `Task: T-NNN`; chờ CI và merge chỉ bằng `scripts/merge-if-green.sh`, chờ `main-guard`; một PR cho cả task trên nhánh `claude/quick-T-NNN-<tu-khoa>`, mô tả ghi "Đường làm nhanh (ADR-0034)" và những gì đường thường ghi vào báo cáo phiên; nhật ký, plan `done`, ticket `on-staging` ghi trong lần đẩy cuối trước CI; trước khi merge chạy `scripts/plan-check.py` và `scripts/run-check.py` cho các danh sách chạy chưa chạy trên nhánh làm nhanh và báo dev lỗi mới; hỏi dev trước mọi lệch mà phiên tự chạy được tự làm rồi báo, ghi vào nhật ký; dừng hay bỏ task thì đóng PR làm nhanh và đưa yêu cầu, ticket `stopped`, plan có nhật ký lần dừng lên nhánh chính bằng PR tài liệu, làm tiếp thì theo đường thường trên nhánh `claude/T-NNN-<tu-khoa>`; nhánh chính đỏ vì commit merge của PR làm nhanh thì revert theo ADR-0025 cùng dev, rồi ticket `stopped` và nhật ký ghi "đã merge rồi revert"; đánh dấu token bằng `scripts/token-usage.py mark` và ghi CSV với `--run QUICK-YYYYMMDD-T-NNN`.
