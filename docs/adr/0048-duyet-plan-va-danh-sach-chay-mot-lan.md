---
id: ADR-0048
title: Đường làm nhanh giữ, thêm bước gộp nhánh chính; plan và danh sách chạy của một đợt được duyệt một lần trong một PR, không cần họp; AI chỉ ghi trạng thái duyệt theo lời đồng ý rõ ràng của dev, ở một quy tắc chung
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-11-12
deciders: [Nhật]
supersedes: [ADR-0034]       # ADR hợp nhất (ADR-0044): chép mọi quy tắc của ADR-0034 (phần A), thêm duyệt cả đợt (phần B) và gom quy tắc ghi trạng thái duyệt về một chỗ (phần C)
superseded_by:
---

# ADR-0048: ĐƯỜNG LÀM NHANH GIỮ, THÊM BƯỚC GỘP NHÁNH CHÍNH; PLAN VÀ DANH SÁCH CHẠY CỦA MỘT ĐỢT ĐƯỢC DUYỆT MỘT LẦN TRONG MỘT PR, KHÔNG CẦN HỌP; AI CHỈ GHI TRẠNG THÁI DUYỆT THEO LỜI ĐỒNG Ý RÕ RÀNG CỦA DEV, Ở MỘT QUY TẮC CHUNG

## Bối cảnh

**Đường làm nhanh.** ADR-0034 mở đường làm nhanh cho việc nhỏ của code app khi dev ngồi cùng AI: vẫn đủ yêu cầu, ticket, plan, nhật ký, CI và `merge-if-green.sh`, bỏ họp, các PR duyệt riêng, danh sách chạy và báo cáo phiên. Đường này chạy tốt và giữ trong ADR này (phần A), chỉ thêm một bước ở A7: gộp nhánh chính vào nhánh làm nhanh trước khi kiểm các danh sách chạy chưa chạy, vì danh sách của một đợt có thể được merge trong lúc PR làm nhanh còn mở. ADR-0034 được thay chỉ vì phần C dưới đây gom quy tắc ghi trạng thái duyệt, mà ADR-0034 đang giữ một phần, về một chỗ: theo ADR-0044, một quy tắc không được nằm rải ở hai ADR còn hiệu lực.

**Hai vòng duyệt cho một đợt.** Từ lúc có ticket tới lúc merge, máy chạy mỗi task khoảng 45 tới 60 phút (dòng thời gian của năm phiên gần nhất); phần lớn thời gian còn lại là chờ người. Một đợt việc của phiên tự chạy hiện đi qua hai vòng duyệt nối tiếp: mỗi plan một PR, dev duyệt và PR được merge; sau đó AI mới lập danh sách chạy (`run-check.py` đòi plan đã `approved`), rà soát độc lập, rồi một PR nữa để dev duyệt danh sách. Ví dụ:

- Đợt T-101, T-102: plan merge lúc 03:36 (PR #457) và 03:43 (PR #456); PR danh sách chạy mở 04:14, merge 04:41; phiên bắt đầu 05:09.
- Đợt T-097 tới T-099: plan cuối merge lúc 14:11 (PR #442); PR danh sách mở 15:46, merge 16:17.
- Danh sách RUN-20261101-02 nằm 136 giờ trước khi merge (PR #383): một phần vì nó chờ một phiên khác xong, một phần vì rà soát lại tìm ra điểm chặn sau khi plan đã `approved`, nên phải soạn plan phiên bản mới.

Khoảng giữa hai vòng gồm việc AI lập và rà soát danh sách (30 tới 95 phút ở hai đợt trên), việc này vẫn phải làm. Phần bỏ được là vòng chờ duyệt thứ hai: khoảng 27 tới 31 phút mỗi đợt khi dev đang có mặt, và có thể nhiều ngày khi dev phải quay lại lần nữa. Điểm chặn của rà soát danh sách hiện đến khi plan đã `approved`, nên phải soạn plan phiên bản mới; kiểm trên bản `draft` thì sửa ngay được.

Quy trình còn ghi "họp đánh giá rủi ro" trước khi duyệt. Nhóm hiện chỉ có một dev, duyệt trên PR, không có buổi họp thật.

**Ghi trạng thái duyệt.** `docs/CLAUDE.md` cấm AI tự chuyển trạng thái thay cho người, và ADR-0034 ghi trường hợp làm nhanh là ngoại lệ duy nhất. Thực tế khi dev duyệt plan hay danh sách chạy bằng lời trong phiên, AI vẫn ghi `approved` kèm lời của dev (ví dụ mục 9 của plan T-101, danh sách RUN-20261108-02: "AI ghi theo lời anh Nhật"). Quy tắc thành văn và cách làm đang lệch nhau; cần một quy tắc chung nói rõ khi nào AI được ghi và ghi thế nào.

## Quyết định

### Phần A. Đường làm nhanh (quy tắc của ADR-0034, thêm bước gộp nhánh chính ở A7)

**A1. Có đường làm nhanh bên cạnh phiên tự chạy, cho code app.** Dev và AI làm cùng nhau, ngay trên máy dev, trong phiên có người theo dõi. AI làm gần như mọi việc; dev trả lời câu hỏi, đồng ý plan một lần và xem kết quả.

**A2. Điều kiện vào đường làm nhanh.** Đủ mọi điều sau; thiếu một điều thì đi đường của phiên tự chạy.

- Dev ngồi cùng suốt task, tới khi `main-guard` của commit merge xanh, và trả lời được ngay.
- Plan không chạm điểm chặn nào ở mục 1 của `risk-scoring.md`, kể cả điểm 7 (module `:core:*`, cấu hình build, CI, quy trình phiên tự chạy). Không thêm quyền, thư viện, SDK.
- Mức rủi ro Thấp (tổng 0 đến 4), tiêu chí E dưới 2: không có kỹ thuật mới cần chạy thử (mục 4.1 của plan).
- Ticket cỡ S, hoặc M khi dev đồng ý.
- Kịch bản đã có trong spec `confirmed`, hoặc dev ngồi cùng là người xác nhận spec của feature đó (A4).
- Lúc bắt đầu: không có phiên tự chạy nào đang chạy (danh sách `running` trên một nhánh `claude/RUN-*-report`, cách tra như `scripts/merge-if-green.sh`), không có issue `main-broken` hay `env-broken` đang mở.

**A3. Vẫn làm như đường chính thống.**

- **Yêu cầu:** lời dev nói trong phiên được ghi thành file yêu cầu trong `features/<feature>/requests/`, giữ nguyên ý của dev; có spec thì yêu cầu chuyển `specified`.
- **Ticket** theo mẫu ticket, **plan** theo đúng mẫu plan, đủ các mục, kể cả mục 3.5 "Hành vi đã có bị đổi" (`scripts/existing-tests.py`) và chấm rủi ro; frontmatter có `lane: quick`. Chạy `scripts/plan-check.py` khi plan còn `draft` và sửa hết lỗi. Mã ticket và số bài học đã có người dùng (PR song song) thì lấy số sau và sửa mọi chỗ dẫn, như L10 của ADR-0032.
- **Test viết trước**, thất bại đúng lý do, commit trước code (`docs/auto-run.md`, bước 3).
- **Kiểm tra cục bộ** bằng `scripts/check.sh` (bước 5).
- **Rà soát độc lập** bằng một agent con ngữ cảnh mới, chỉ nhận plan, spec, thiết kế, ADR và phần thay đổi, theo nội dung và ba mức của bước 6. Lời giao có dòng `Task: T-NNN` để token tính vào task.
- **CI** (`pr-check.yml`) và **merge** chỉ bằng `scripts/merge-if-green.sh`, rồi chờ `main-guard` như bước 8, 9.
- **Nhật ký thực hiện** ở cuối plan, plan `done`, ticket `on-staging`.
- **Bài học** vào `docs/lessons.md` khi gặp điều đáng ghi, như mọi phiên.

**A4. Duyệt trong phiên.**

- **Plan:** dev ngồi cùng đọc tóm tắt plan (phạm vi, danh sách file, test, điểm rủi ro, câu hỏi) và đồng ý rõ ràng. Dev nào cũng duyệt được, không cần là chủ feature; đây là ngoại lệ của `risk-scoring.md` mục 3 cho đường làm nhanh. AI chuyển plan sang `approved`, ghi `reviewer` là dev, `approved_at`, mục 9 ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý của dev, và ghi khi người duyệt không phải `owner` của ticket. Ticket theo plan (`scripts/status.py --sync`).
- **Spec:** việc chưa có trong spec chỉ đi đường làm nhanh khi dev ngồi cùng là người xác nhận spec của feature đó. AI sửa spec, dev xác nhận rõ ràng trong phiên, AI ghi `status: confirmed`, `confirmed:` và mục lịch sử kèm lời xác nhận của dev.
- Cách ghi lời duyệt theo phần C.

**A5. Được bỏ.** PR riêng cho ticket, plan, danh sách chạy; họp đánh giá rủi ro; `scripts/run-check.py` và rà soát trước khi chạy (`docs/ops/pre-run-review.md`); kích hoạt phiên (`start-run.yml`, routine, `claude --cloud`), kiểm tra đầu phiên, nhánh báo cáo, báo cáo phiên, hàng chờ plan cuối phiên.

**A6. Một PR cho cả task.** Ngoại lệ của `docs/auto-run.md` bước 7 (PR của task không chứa tài liệu) và bước 10 (nhật ký ghi trên nhánh báo cáo).

- Nhánh `claude/quick-T-NNN-<tu-khoa>`, tạo từ nhánh chính mới nhất. Tên nhánh không bắt đầu bằng `claude/T-` để `merge-if-green.sh` từ chối PR khi có phiên tự chạy đang chạy, như mọi PR không thuộc phiên.
- PR chứa yêu cầu, spec nếu đổi, ticket, plan, test, code, ảnh chuẩn và nhật ký. Tiêu đề `T-NNN: <tên task>`. Mô tả ghi "Đường làm nhanh (ADR-0048)", người duyệt plan, kết quả `plan-check.py`, kết quả rà soát độc lập, và những gì đường thường ghi vào báo cáo phiên: mục "Ngoài mục 3.2", "Đã sửa theo rà soát", "Test cũ viết lại (ADR-0031)", điểm nên sửa chưa sửa, việc cần người kiểm.
- Nhật ký (tiêu đề `Làm nhanh YYYY-MM-DD, phiên bản plan N`), plan `done` và ticket `on-staging` ghi trong lần đẩy cuối, sau rà soát độc lập và trước CI, để CI chỉ chạy một vòng. Dòng kết quả ghi "merge bằng PR này", dòng CI ghi "CI của PR này". CI đỏ thì sửa và cập nhật nhật ký trong cùng lần đẩy sửa. PR không merge thì các trạng thái đó cũng không vào nhánh chính.

**A7. Trước khi merge, kiểm danh sách chạy chưa chạy.** Ngay trước khi kiểm, `git fetch origin` rồi gộp nhánh chính vào nhánh làm nhanh (`git merge origin/main`), để thấy cả danh sách vừa merge trong lúc PR làm nhanh còn mở. Commit gộp chỉ dùng để kiểm trên máy, không đẩy, để CI chỉ chạy một vòng (A6); `merge-if-green.sh` tự cập nhật nhánh khi cần. Gộp có xung đột thì dừng và báo dev. Trên nhánh làm nhanh, chạy `scripts/plan-check.py` cho mọi plan của các danh sách chạy `draft` hay `approved` chưa chạy, và `scripts/run-check.py` cho các danh sách đó. Có lỗi mới so với nhánh chính (file plan định tạo đã có, test hay script đọc file đã đổi, test có sẵn ở mục 3.5 đã đổi) thì báo dev; dev chọn sửa plan đó trước, merge rồi soạn plan phiên bản mới, hay chờ. Danh sách `draft` đã rà soát trước khi chạy thì báo dev nó cần rà soát lại. Nhắc dev không kích hoạt phiên tự chạy, kể cả không merge danh sách `start: auto`, khi PR làm nhanh còn mở.

**A8. Trong lúc làm, hỏi dev thay vì tự quyết.** Các lệch mà phiên tự chạy tự làm rồi báo (ADR-0029, ADR-0032, ADR-0033) được hỏi dev trước, rồi ghi vào dòng "Lệch so với plan" của nhật ký.

**A9. Khi dừng hay bỏ task.** Gặp điều kiện dừng (`risk-scoring.md`, mục 4), thấy task chạm điểm chặn, hay dev bỏ task:

- Đóng PR làm nhanh, giữ nhánh.
- Mở một PR tài liệu từ nhánh chính, merge bằng `merge-if-green.sh`, gồm: yêu cầu, spec nếu đã xác nhận, ticket `stopped`, plan với nhật ký của lần dừng. Muốn làm tiếp thì plan có phiên bản mới `draft` như `docs/auto-run.md`, mục Chuẩn bị cho người khi task dừng, và mục 3.1 ghi cách tiếp tục trên nhánh `claude/T-NNN-<tu-khoa>` tạo từ nhánh làm nhanh, hay làm lại từ đầu; task đi đường thường.

**A10. Nhánh chính đỏ sau khi merge.** `main-guard` mở issue `main-broken` mà commit gây lỗi là commit merge của PR làm nhanh: AI cùng dev revert như `docs/auto-run.md`, mục Khi nhánh chính đỏ vì task của phiên (PR từ nhánh `revert/<sha>`, merge bằng `merge-if-green.sh`, ADR-0025). Sau đó một PR tài liệu chuyển ticket sang `stopped` và ghi "đã merge rồi revert" vào nhật ký plan. Commit gây lỗi không phải của PR làm nhanh thì không revert, báo dev.

**A11. Số liệu và kiểm tra sau merge.**

- Token: `scripts/token-usage.py mark T-NNN` khi bắt đầu, `mark chung` khi xong, rồi `scripts/token-usage.py report --since <giờ bắt đầu> --run QUICK-YYYYMMDD-T-NNN --csv docs/metrics/token-usage.csv`; tệp CSV đi cùng PR tài liệu kế tiếp (`docs/ops/token-usage.md`).
- Dòng kiểm tra thủ công của plan: `(emulator: ...)` thì chạy tay `device-session.yml` sau khi merge, hoặc dev thử ngay trên máy; `(máy thật: ...)` và `(người: ...)` thì dev kiểm trong phiên khi được, kết quả ghi vào nhật ký. Muốn có bản staging ngay thì chạy tay `staging.yml`.


### Phần B. Duyệt cả đợt trong một PR

**B1. Một PR cho cả đợt.** Danh sách của đợt gồm các plan `draft` của đợt và, như thường lệ, mọi plan đã `approved` trên nhánh chính chưa chạy. Khi các plan `draft` của một đợt đã qua `scripts/plan-check.py` và rà soát độc lập của từng plan, AI lập danh sách chạy `draft` trong cùng PR với các plan đó, trên nhánh `claude/batch-YYYYMMDD-NN`. Plan đã có PR riêng thì chép vào nhánh của đợt; mô tả PR của đợt ghi số các PR cũ; PR cũ chỉ đóng sau khi dev đồng ý, để không mất bình luận dev đã để trên đó.

**B2. Spec trong đợt.** Spec mà plan của đợt cần sửa (`changed`) đi cùng PR của đợt. Người xác nhận spec của feature xác nhận spec trong cùng lời duyệt với plan; tóm tắt của đợt đưa phần spec đổi lên trước. Spec chưa được xác nhận thì plan dựa vào nó không được duyệt (quy tắc hiện có), và bị bỏ khỏi danh sách như ý B4.

**B3. Kiểm trên bản `draft`.** `scripts/run-check.py --batch <danh sách>` kiểm danh sách khi plan còn `draft`. Ở chế độ này, script bỏ qua lỗi "plan, ticket chưa `approved`" và "spec chưa `confirmed`" chỉ cho plan, ticket và spec có trong `git diff --name-only origin/main...HEAD` sau `git fetch` (tức đổi trong cùng PR, so với điểm tách), với plan `draft` và ticket `planned`; mọi kiểm tra khác giữ nguyên mức. Rà soát độc lập theo `docs/ops/pre-run-review.md`, mục 2, chạy trên bản đó. Điểm chặn thì sửa plan `draft` ngay trong PR; sau mỗi lần sửa plan hay spec, chạy lại cả hai lớp kiểm tra như `docs/ops/pre-run-review.md` đã quy định.

**B4. Duyệt một lần.** AI đưa người duyệt một tóm tắt cho cả đợt: phần spec đổi, từng plan (phạm vi, mức rủi ro, điểm chặn, câu hỏi cần trả lời), thứ tự chạy, số task, `open_with`, ước tính token và thời gian, việc dev phải kiểm trước phiên, điều xảy ra khi chỉ duyệt một phần (plan nào bỏ khỏi danh sách), kèm câu duyệt có sẵn. Lời duyệt chỉ có hiệu lực với plan và spec mà người nói có quyền duyệt (chủ feature của từng plan, duyệt chéo khi rủi ro Cao, người xác nhận spec của từng feature); phần còn lại coi như chưa duyệt. Lời duyệt cả đợt mà chỉ có hiệu lực một phần vì quyền của người nói thì không phải lời duyệt một phần: AI đưa lại danh sách đã bỏ các plan đó và hỏi lại. Người duyệt duyệt cả đợt, duyệt một phần, hoặc yêu cầu sửa. Lời duyệt một phần nêu rõ các plan được duyệt; nó duyệt luôn danh sách đã bỏ đúng các plan còn lại, vì tóm tắt đã nói trước cách bỏ này. AI bỏ các plan đó khỏi danh sách, chuyển plan, ticket và spec chưa duyệt ra khỏi PR của đợt về một PR riêng, rồi chạy lại `run-check.py --batch`. Việc bỏ làm danh sách đổi thêm (ví dụ một plan được duyệt phụ thuộc plan bị bỏ, `run_order` phải đổi) hay `run-check.py` có lỗi mới thì đó là nội dung đổi sau lời đồng ý theo C3: đưa lại và hỏi lại.

**B5. Ghi trạng thái trong một commit.** Sau lời duyệt, theo phần C: AI chuyển spec sang `confirmed`, plan sang `approved`, chạy `scripts/status.py --sync` cho ticket, chuyển danh sách sang `approved`, rồi chạy `scripts/run-check.py <danh sách>` không có `--batch`; còn lỗi thì không đẩy, báo dev. Ngay trước khi gọi `scripts/merge-if-green.sh`, `git fetch origin` rồi gộp nhánh chính vào nhánh của đợt (`git merge origin/main`, chỉ để kiểm trên máy, không đẩy; xung đột thì báo người duyệt) và chạy lại `scripts/plan-check.py` cho từng plan cùng `scripts/run-check.py`; có lỗi mới thì báo người duyệt, không merge. PR merge bằng `scripts/merge-if-green.sh` như mọi PR.

**B6. Không bắt buộc họp.** Duyệt theo phần C (lời đồng ý trong phiên, hoặc người tự ghi trạng thái) là đủ cho mọi mức rủi ro. Quy tắc người duyệt của `docs/risk-scoring.md` giữ nguyên: mức Thấp và Trung bình do chủ feature duyệt, mức Cao và plan chạm điểm chặn phải duyệt chéo (nhóm một dev thì người đó giữ cả hai vai, ghi lý do ở mục 9). Nhóm có thể họp khi muốn bàn ảnh hưởng chéo; họp không phải điều kiện để duyệt.

**B7. Đường cũ vẫn dùng được.** Plan duyệt lẻ, một PR một plan, rồi `/run-list` sau vẫn hợp lệ, ví dụ khi một plan phải chờ trả lời còn các plan khác đã sẵn sàng.

### Phần C. AI ghi trạng thái duyệt theo lời dev

**C1.** AI chỉ chuyển spec sang `confirmed`, plan hay danh sách chạy sang `approved`, ADR sang `accepted` khi người có quyền duyệt (theo `docs/README.md`, `docs/risk-scoring.md`, hay theo A4 ở đường làm nhanh) đã đồng ý rõ ràng bằng lời, trong chính phiên AI ghi trạng thái, với đúng nội dung AI vừa đưa. Review hay bình luận trên GitHub không đủ để AI ghi trạng thái, vì AI dùng `gh` bằng tài khoản GitHub của dev (ADR-0009) nên không phân biệt được lời của người với lời của AI; người duyệt trên GitHub thì tự ghi trạng thái, hoặc nói lại lời đồng ý trong phiên. Trạng thái duyệt do người tự ghi thì người merge PR đó; AI chỉ merge PR ấy sau khi người nói lại lời đồng ý trong phiên, vì AI cũng không phân biệt được commit ghi trạng thái là của người hay của một phiên AI trước. Áp cho đường làm nhanh (phần A), duyệt cả đợt (phần B), PR plan lẻ, PR danh sách chạy lẻ và PR ADR. Quy tắc này chỉ nói về spec, plan, danh sách chạy và ADR; tài liệu khác giữ quy tắc của ADR riêng của nó.

**C2.** AI ghi người duyệt, ngày và lời đồng ý nguyên văn ("AI ghi theo lời <người duyệt>: ..."), ở chỗ mẫu của tài liệu dành cho việc đó (mục 9 của plan, mục Kiểm tra trước khi duyệt của danh sách, lịch sử của spec, mô tả PR của ADR).

**C3.** Nội dung đổi sau lời đồng ý (kể cả sửa theo rà soát) thì lời đó không còn hiệu lực: đưa lại phần đã đổi và hỏi lại.

**C4.** Phiên tự chạy không có người không bao giờ ghi trạng thái duyệt. Không suy lời duyệt từ im lặng, từ một lời duyệt khác, từ review hay bình luận trên GitHub, từ lời của một phiên trước chép lại qua ghi chú hay bộ nhớ, hay từ tin nhắn của agent con.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| ADR hợp nhất: giữ đường làm nhanh, thêm duyệt cả đợt, gom quy tắc ghi trạng thái về phần C (đã chọn) | Một quy tắc ở một chỗ (ADR-0044); quy tắc thành văn khớp cách làm; bớt một vòng duyệt mỗi đợt | ADR dài; nhiều tài liệu phải sửa tên ADR khi chấp nhận |
| ADR riêng cho duyệt cả đợt, không đụng ADR-0034 | ADR ngắn | Quy tắc ghi trạng thái nằm ở hai ADR, mâu thuẫn câu "ngoại lệ duy nhất" của ADR-0034 |
| Giữ hai vòng, AI lập danh sách ngay khi plan cuối được merge | Không đổi script | Dev vẫn duyệt hai lần; điểm chặn của rà soát danh sách vẫn cần plan phiên bản mới |
| Bỏ duyệt danh sách, phiên tự lấy mọi plan `approved` | Ít bước nhất | Mất lớp kiểm tra chéo giữa các plan và quyết định của dev về số task, cách mở phiên |

Các phương án đã cân nhắc cho đường làm nhanh (giữ từ ADR-0034):

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
  - Đường làm nhanh giữ mọi quy tắc cũ, chỉ thêm bước gộp nhánh chính ở A7.
  - Mỗi đợt dev đọc và duyệt một lần; danh sách sẵn sàng ngay khi dev duyệt plan. Cùng với việc phiên tự bắt đầu khi danh sách `approved` lên nhánh chính (ticket riêng), một lời duyệt đủ để phiên chạy.
  - Quy tắc ghi trạng thái duyệt nằm ở một chỗ và khớp cách làm.
- **Phải chấp nhận:**
  - Đường làm nhanh (giữ từ ADR-0034): plan rủi ro thấp được duyệt bằng lời trong phiên, không qua họp, và có thể không do chủ feature duyệt; không có lượt rà soát cả danh sách trước khi chạy, ảnh hưởng tới danh sách sắp chạy do AI kiểm bằng script và báo dev; không có báo cáo phiên, muốn xem thì đọc nhật ký plan và PR (`lane: quick`); dừng hay revert cần thêm một PR tài liệu.
  - PR của đợt dài hơn PR một plan.
  - Dev duyệt trên GitHub phải nói lại lời đồng ý trong phiên, hoặc tự ghi trạng thái và tự merge.
  - `scripts/run-check.py` có thêm chế độ `--batch` (cần test riêng).
  - Khi ADR được chấp nhận, ADR-0034 chuyển `superseded`, và một PR làm theo ADR sửa:
    - `docs/README.md`: bảng người tạo và xác nhận (spec, danh sách chạy "lập sau họp"), bước 3, 6, 7, 9 và đoạn đường làm nhanh.
    - `docs/CLAUDE.md`: mục Khi sinh spec từ yêu cầu (ý cuối), Khi viết plan (ý cuối), Khi lập danh sách chạy (kể cả "khoảng 15 phút mỗi task", nay đo được 45 tới 60 phút), Khi làm nhanh có người theo dõi, Khi xử lý phản hồi ("qua họp"), Những điều không được làm.
    - `docs/risk-scoring.md`: dòng mở đầu ("trước khi đưa vào họp"), cột Trong họp, dòng đường làm nhanh.
    - `docs/auto-run.md`, `docs/ops/pre-run-review.md`, `docs/ops/quick-lane.md`, `docs/ops/token-usage.md`.
    - `docs/templates/plan.md` (`run_order`, `lane`, chú thích mục 9 và mục 10), `docs/templates/run.md` (lời dẫn "lập sau họp", mục Đề xuất cho buổi họp, `approved_by`).
    - `.claude/skills/run-list/SKILL.md`, `write-plan`, `quick-lane`, `spec`, `triage-feedback`, `adr` (bước 5, ghi lời đồng ý theo C2).
    - Chuỗi "Đường làm nhanh (ADR-0034)" ở mọi nơi, và mọi câu "sau họp", "qua họp", "chỉ dev (người) chuyển sang" trong `docs/` và `.claude/skills/`.
  - ADR-0035 (`accepted`) còn nhắc ADR-0034 trong phần Quyết định; ADR `accepted` không sửa, người đọc theo `superseded_by` của ADR-0034 sang ADR này.
- **Việc AI phải tuân thủ:** đường làm nhanh chỉ cho code app, chỉ khi dev ngồi cùng suốt task tới khi `main-guard` xanh, plan không chạm điểm chặn nào kể cả điểm 7, không thêm quyền, thư viện hay SDK, mức rủi ro Thấp và tiêu chí E dưới 2, ticket cỡ S hoặc M khi dev đồng ý, kịch bản có trong spec `confirmed` hoặc dev ngồi cùng là người xác nhận spec của feature; không bắt đầu khi có phiên tự chạy đang chạy hay issue `main-broken`, `env-broken` đang mở; vẫn ghi yêu cầu, ticket, plan đủ mục theo mẫu với `lane: quick`, chạy `scripts/plan-check.py` khi plan còn `draft` và sửa hết lỗi; dev nào ngồi cùng cũng duyệt được plan làm nhanh, ghi khi người duyệt không phải `owner` của ticket; vẫn viết test trước, chạy `scripts/check.sh`, rà soát độc lập bằng agent con ngữ cảnh mới theo bước 6 của `docs/auto-run.md`, lời giao có dòng `Task: T-NNN`; chờ CI và merge chỉ bằng `scripts/merge-if-green.sh`, chờ `main-guard`; một PR cho cả task trên nhánh `claude/quick-T-NNN-<tu-khoa>`, mô tả ghi "Đường làm nhanh (ADR-0048)" và những gì đường thường ghi vào báo cáo phiên; nhật ký, plan `done`, ticket `on-staging` ghi trong lần đẩy cuối trước CI; trước khi merge gộp nhánh chính vào nhánh làm nhanh rồi chạy `scripts/plan-check.py` và `scripts/run-check.py` cho các danh sách chạy chưa chạy và báo dev lỗi mới; hỏi dev trước mọi lệch mà phiên tự chạy được tự làm rồi báo, ghi vào nhật ký; dừng hay bỏ task thì đóng PR làm nhanh và đưa yêu cầu, ticket `stopped`, plan có nhật ký lần dừng lên nhánh chính bằng PR tài liệu, làm tiếp thì theo đường thường trên nhánh `claude/T-NNN-<tu-khoa>`; nhánh chính đỏ vì commit merge của PR làm nhanh thì revert theo ADR-0025 cùng dev, rồi ticket `stopped` và nhật ký ghi "đã merge rồi revert"; đánh dấu token bằng `scripts/token-usage.py mark` và ghi CSV với `--run QUICK-YYYYMMDD-T-NNN`; khi duyệt cả đợt, lập danh sách chạy `draft` cùng PR với các plan `draft` của đợt trên nhánh `claude/batch-YYYYMMDD-NN`, sau khi các plan đã qua `plan-check.py` và rà soát độc lập của từng plan; spec cần sửa của đợt đi cùng PR và được người xác nhận spec của feature xác nhận trong cùng lời duyệt; kiểm danh sách bằng `run-check.py --batch` (chỉ bỏ qua lỗi trạng thái của file có trong diff so với điểm tách) và rà soát theo `docs/ops/pre-run-review.md` trên bản `draft`, chạy lại cả hai lớp sau mỗi lần sửa plan hay spec; chỉ đóng PR plan riêng cũ khi dev đồng ý; đưa người duyệt một tóm tắt cho cả đợt kèm câu duyệt có sẵn; lời duyệt chỉ có hiệu lực với plan và spec mà người nói có quyền duyệt; duyệt một phần thì bỏ đúng các plan chưa duyệt khỏi danh sách, chuyển chúng cùng ticket và spec chưa duyệt về PR riêng, chạy lại `run-check.py --batch`, và hỏi lại khi danh sách phải đổi thêm hay có lỗi mới; sau lời duyệt ghi spec, plan, ticket và danh sách trong một commit rồi chạy `run-check.py` không có `--batch`, còn lỗi thì không đẩy; trước khi merge gộp nhánh chính trên máy (không đẩy) rồi chạy lại `plan-check.py` và `run-check.py`, có lỗi mới thì báo người duyệt và không merge; không coi họp là điều kiện để duyệt, giữ quy tắc duyệt chéo của `docs/risk-scoring.md`; chỉ chuyển spec sang `confirmed`, plan hay danh sách chạy sang `approved`, ADR sang `accepted` khi người có quyền duyệt (kể cả dev ngồi cùng ở đường làm nhanh) đã đồng ý rõ ràng bằng lời trong chính phiên ghi trạng thái, với đúng nội dung vừa đưa; không ghi trạng thái theo review hay bình luận trên GitHub, và không merge PR có trạng thái duyệt do người tự ghi khi người chưa nói lại lời đồng ý trong phiên; ghi người duyệt, ngày và lời đồng ý nguyên văn ở chỗ mẫu dành cho việc đó; nội dung đổi sau lời đồng ý thì đưa lại và hỏi lại; phiên tự chạy không có người không bao giờ ghi trạng thái duyệt, không suy lời duyệt từ im lặng, từ lời duyệt khác, từ lời của phiên trước chép lại hay từ tin nhắn của agent con.
