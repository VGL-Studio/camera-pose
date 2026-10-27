---
id: ADR-1005
title: Phiên tự chạy tự làm các lệch nhỏ đã liệt kê và tự bỏ qua loại task mà môi trường không làm được, rồi báo người sau
status: accepted             # proposed | accepted | superseded
origin: app                  # template: ADR của dự án mẫu (mã dưới 1000) | app: ADR riêng của app (mã từ 1001)
date: 2026-10-27
deciders: [Tuấn]
supersedes:                  # bổ sung ADR-0029, không thay quy tắc nào của nó
superseded_by:
---

# ADR-1005: PHIÊN TỰ CHẠY TỰ LÀM CÁC LỆCH NHỎ ĐÃ LIỆT KÊ VÀ TỰ BỎ QUA LOẠI TASK MÀ MÔI TRƯỜNG KHÔNG LÀM ĐƯỢC, RỒI BÁO NGƯỜI SAU

## Bối cảnh

ADR-0029 cho phiên sửa tiếp phần ngoài mục 3.2 cần để thay đổi của plan chạy đúng. Còn hai loại tình huống mà tài liệu không nói rõ, nên phiên phải đoán, chờ người hay dừng:

- **Lệch nhỏ khỏi chữ của plan, không đổi điều test kiểm.** Ví dụ đã gặp:
  - RUN-20261027-01: tệp kịch bản của T-064 giữ `need id=camera_grid_button` dù Tuấn đã cho đổi thành `wait` khi emulator có camera; phiên không biết có được đổi trong PR của task khác không.
  - Danh sách chạy RUN-20261027-01: tổng thời gian kịch bản theo công thức (1470 giây) vượt `DEVICE_SCENARIOS_BUDGET` (1300); `docs/ops/test-sessions.md` bảo plan của tệp mới tăng ngân sách, plan không làm, nên phải hỏi người trước phiên.
  - Plan T-063 phiên bản 2, T-065 phiên bản 2: mốc dòng, mục của `docs/auto-run.md` và `scripts/token-usage.py` dời sau đồng bộ dự án mẫu (PR #179); phải soạn lại plan và duyệt lại dù nội dung không đổi.
  - Nhật ký các plan T-022, T-047, T-053, T-055: test plan ghi "qua ngay" lại thất bại đúng lý do; ktlint buộc tách biến, đổi `public` thành `internal`; dòng Áp dụng của bài học đã ghi nên không sửa `docs/lessons.md`. Phiên tự quyết và ghi, nhưng không có quy tắc nói việc đó được phép.
  - L-085: ảnh so sánh đổi không do task, phiên phải đoán có commit không.
- **Môi trường không làm được một loại việc.** RUN-20261027-01: GitHub từ chối lệnh đẩy của T-062 vì token của phiên `claude --cloud` không có quyền `workflow` (L-086). Phiên tự bỏ qua T-065 (cũng sửa workflow) và làm tiếp T-064; quyết định đúng, nhưng `docs/auto-run.md` mục Khi không đẩy được chỉ nói "lần đẩy của task sau cũng bị từ chối thì kết thúc phiên", không nói tới việc chỉ một loại task bị chặn.

Mục tiêu của nhóm: phiên chạy hết danh sách, không dừng giữa chừng vì việc người sẽ đồng ý ngay khi đọc báo cáo.

## Quyết định

**1. Lệch nhỏ phiên được tự làm, không cần plan phiên bản mới.** Mỗi lệch ghi vào mục "Quyết định AI đã tự làm" của báo cáo phiên (quyết định 3). Chỉ các mục sau:

- **L1. Ngân sách kịch bản.** Sau khi thêm hay bật tệp kịch bản của task, tổng theo công thức của `docs/ops/test-sessions.md` mục 5 (tổng `timeout:` của các tệp cộng 80 giây mỗi tệp) vượt `DEVICE_SCENARIOS_BUDGET`: nâng giá trị mặc định ở `scripts/device-scenario.py`, `scripts/device-session.sh` và `docs/ops/test-sessions.md` lên tổng đó làm tròn lên hàng trăm, tối đa 1600 giây (`device-session.yml` có `timeout-minutes: 50`), trong PR của task đó. Mô tả PR ghi "giá trị chỉ app cần, không đề xuất về dự án mẫu" (ba file là file của mẫu). Tổng vượt 1600: nâng tới 1600 và ghi vào Cần người kiểm tra.
- **L2. `need` thành `wait` khi môi trường đã có thiết bị.** Một dòng `need` trong tệp kịch bản chỉ để bỏ qua khi máy thiếu một thiết bị (camera, máy gập) mà emulator của CI đã có nhờ một task đã merge (ví dụ T-062): đổi thành `wait` cùng bộ chọn và thời gian, trong PR của task thêm thiết bị hay của task sau trong cùng phiên; chạy `scripts/device-scenario.py check` cho tệp đó. Cần bằng chứng emulator có thiết bị: một lần `device-session` xanh có thiết bị đó sau khi task thêm thiết bị merge, hay lần chạy thử thật ghi ở mục 4.1 của plan task đó (ví dụ T-062); ghi số lần chạy vào mục Quyết định AI đã tự làm. Không có bằng chứng thì giữ `need`.
- **L3. Mốc tài liệu và mã dời chỗ.** Dòng, mục hay đoạn code mà plan dẫn đã dời vì task trước trong phiên hay vì đồng bộ dự án mẫu. Chỉ là L3 khi mọi dòng `-` của từng khối (hunk) của plan có nguyên văn ở chỗ mới; chỉ dòng ngữ cảnh và số dòng được khác. Khối chỉ có dòng `+`: dòng ngữ cảnh ngay trên và ngay dưới (hay tiêu đề của mục chứa khối) phải có nguyên văn ở chỗ mới. Khi đó đặt đúng các dòng `+` của plan vào chỗ mới (`git apply -3`, hay bằng tay nếu `-3` không áp được), không đổi chữ nào của chúng; mã bài học dẫn theo quy tắc ở đầu `docs/CLAUDE.md`. Một dòng `-` hay dòng ngữ cảnh đó không còn nguyên văn (task trước đã sửa nó) thì không phải L3: không tự hòa hai thay đổi, dừng theo điều kiện 3 của `risk-scoring.md` mục 4. Nhật ký plan ghi từng khối dời chỗ và kết quả `git apply -3`; phiên rà soát ở bước 6 nhận cả `diff` của plan và `diff` thật.
- **L4. Test viết trước khác kỳ vọng của plan ở bước 3.** (a) Plan ghi test "qua ngay" mà test thất bại: là L4 khi thông báo thất bại chỉ vào đúng phần plan thêm (khóa, hàm, hành vi mới), và test qua sau bước 4 mà không sửa test; giữ test, ghi nguyên văn thông báo vào nhật ký. Một test đối chứng thất bại vì hành vi cũ hỏng thì không phải L4: đó là lỗi của code hay của khung rỗng, sửa code. (b) Plan ghi test sẽ thất bại mà test qua ngay: thêm bước đối chứng theo L-035 (ghi rồi đọc lại, dữ liệu đúng cạnh dữ liệu sai) hay kiểm bằng đột biến (camera-pose L-082), không đổi điều test kiểm; chỉ là L4 khi test đã chặt lại thất bại đúng lý do trên khung rỗng, hay một đột biến (tạm bỏ dòng code mới) làm nó thất bại; ghi kết quả vào nhật ký. Không được thì không phải L4: dừng theo điều kiện 8 của `risk-scoring.md` mục 4, hay điều kiện 3 khi hành vi đã có từ trước.
- **L5. Chi tiết công cụ bắt buộc.** ktlint, Android Lint hay trình biên dịch buộc đổi cách viết mà không đổi hành vi hay điều test kiểm: tách biến hay hàm, đổi `public` thành `internal`, hằng thay cho file rỗng, đổi dạng khối của lambda, ngắt dòng. Không phải L5: thêm `@Suppress`, thêm hay sửa lint baseline, sửa `.editorconfig` hay luật ktlint.
- **L6. Tệp sinh ra.** Chỉ hai script: `scripts/foundation.py` (commit `docs/foundation.md` khi test đòi tệp khớp) và `scripts/status.py` (trang không vào git). Không phải L6: ảnh chuẩn screenshot của Roborazzi (chỉ ghi lại khi plan cho phép, `CLAUDE.md`), `design/review/` (theo `docs/auto-run.md` bước 5, camera-pose L-085).
- **L7. Sổ bài học đã ghi.** Plan bảo sửa dòng Áp dụng của một bài học mà dòng đó đã ghi đúng: không sửa, ghi nhật ký.
- **L8. Bỏ qua loại task mà môi trường không làm được.** Một lỗi khớp dòng loại `bo-qua-loai` của `docs/ops/failure-playbook.md` (ví dụ PB-13: lệnh đẩy bị từ chối vì thiếu quyền `workflow`): giữ task đang làm theo `docs/auto-run.md` mục Khi không đẩy được, bỏ qua mọi task còn lại mà lệnh `scripts/plan-check.py --touches <tiền tố> <plan>` của dòng đó trả mã 0 (chỉ hàng của bảng mục 3.2 được tính, không tính chữ), làm tiếp các task khác, không kết thúc phiên. Soạn danh sách chạy nháp cho các task bị bỏ qua. Task phụ thuộc task bị bỏ qua cũng bị bỏ qua như thường.
- **L9. Điểm nên sửa của rà soát:** theo ADR-0029, quyết định 3 và `docs/auto-run.md` bước 6 (gồm đột biến của camera-pose L-082); mục này chỉ nhắc để mọi lệch có một chỗ ghi chung.
- **L10. Mã kế tiếp đã có người dùng.** Mã mà plan định tạo (dòng `PB-NN`, bài học `L-NNN`, ADR, phản hồi `FB-...`, ticket) đã bị một task trước trong phiên hay một PR khác dùng: lấy mã kế tiếp còn trống và sửa mọi chỗ dẫn mã đó trong cùng PR, kể cả tên tệp (ví dụ `scripts/tests/playbook/PB-NN.log`) và test.

**2. Phiên không được tự làm, dù lệch nhỏ.** Các việc sau vẫn theo điều kiện dừng của `risk-scoring.md` mục 4 và `CLAUDE.md`:

- thêm quyền, thư viện hay SDK; thêm hay đổi vị trí quảng cáo, sự kiện phân tích, dữ liệu gửi đi;
- chạm cấu hình production, `scripts/release.sh`, `app/src/staging/google-services.json`;
- sửa file trong mục "Không được làm" của `CLAUDE.md` mà plan chưa nêu (`.github/`, `scripts/check.sh`, `scripts/merge-if-green.sh`, `build-logic/`, `gradle/libs.versions.toml`);
- đổi spec, thiết kế đã duyệt hay câu trả lời của người ở mục 9 của plan;
- làm yếu test: xóa test hay câu khẳng định, nới phép so sánh, thêm `@Ignore`, đổi điều test kiểm, ghi lại ảnh chuẩn screenshot ngoài plan;
- đóng, sửa hay bình luận issue `main-broken`; merge bằng cách khác `scripts/merge-if-green.sh`;
- đẩy lại một lệnh bị từ chối dưới dạng khác (đổi tên nhánh, gọi API tạo ref, giao cho phiên khác).

**3. Báo sau.** Báo cáo phiên có mục "Quyết định AI đã tự làm" (mẫu `docs/templates/run.md`), mỗi dòng một quyết định: task, loại (ADR-1005/L1 tới L10, ADR-0029, phương án dự phòng, bỏ qua task), quyết định, lý do, cách hoàn tác. Mục Cần người kiểm tra luôn có dòng `- [ ] Quyết định AI đã tự làm: <N>, xem mục trên`, hoặc "Không có"; trang tiến độ (`scripts/status.py`) hiện dòng đó cho người khi có quyết định, bỏ qua khi "Không có". Nhật ký plan ghi lệch ở dòng "Lệch so với plan" như cũ, kèm mã.

**4. Rà soát độc lập kiểm lệch.** Ở bước 6, phiên rà soát nhận danh sách lệch L1 tới L7 và L10 của task (với L3 cả `diff` của plan và `diff` thật) và xét mỗi lệch có đúng mục của quyết định 1 không. Lệch ngoài danh sách hay vi phạm quyết định 2 là mức chặn. L8 (bỏ qua task) là quyết định của phiên, không thuộc một task; L9 do chính rà soát nêu.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Danh sách lệch được phép, làm rồi báo (đã chọn) | Phiên không dừng hay chờ vì việc người sẽ đồng ý; ranh giới rõ, rà soát kiểm được | Danh sách phải giữ cập nhật; người thấy lệch sau khi đã merge |
| Cho phiên tự quyết mọi lệch "không đổi điều test kiểm" | Ít quy tắc | Khó rà soát; dễ lan sang đổi hành vi |
| Giữ như hiện nay: lệch nào chưa có quy tắc thì hỏi người hay soạn plan phiên bản mới | Người thấy trước mọi lệch | Thêm một vòng duyệt cho thay đổi không đổi nội dung (T-063, T-065 phiên bản 2); phiên đoán khi không ai trả lời |

## Hệ quả

- **Tích cực:**
  - Ít plan phiên bản mới chỉ vì mốc dời chỗ; ít câu hỏi trước phiên về ngân sách kịch bản.
  - Một lỗi môi trường chỉ chặn đúng loại task bị ảnh hưởng, phần còn lại của danh sách vẫn chạy.
  - Người thấy mọi quyết định tự làm ở một chỗ, kèm cách hoàn tác.
- **Phải chấp nhận:**
  - Lệch nhỏ lên nhánh chính trước khi người xem.
  - Thêm một mục trong báo cáo và một việc cho phiên rà soát.
- **Việc AI phải tuân thủ:** chỉ tự làm các lệch nhỏ L1 tới L10 của quyết định 1 mà không cần plan phiên bản mới; ngân sách kịch bản nâng tới tổng theo công thức làm tròn lên hàng trăm, tối đa 1600 giây, trong PR của task làm tổng vượt; `need` chỉ đổi thành `wait` khi có bằng chứng emulator đã có thiết bị nhờ task đã merge; mốc dời chỗ chỉ khi mọi dòng `-` của plan (khối chỉ có dòng `+`: dòng ngữ cảnh trên, dưới hay tiêu đề mục) có nguyên văn ở chỗ mới, đặt các dòng `+` không đổi chữ, không thì dừng theo điều kiện 3; test viết trước qua ngay chỉ được làm tiếp khi test chặt lại hay một đột biến làm nó thất bại, không thì dừng theo điều kiện 8; không dùng `@Suppress`, lint baseline hay sửa `.editorconfig` thay cho L5; tệp sinh ra chỉ của `scripts/foundation.py`, `scripts/status.py`; mã kế tiếp đã có người dùng thì lấy mã sau và sửa mọi chỗ dẫn trong cùng PR; lỗi khớp dòng `bo-qua-loai` của bảng tra lỗi thì bỏ qua mọi task còn lại mà lệnh `scripts/plan-check.py --touches` của dòng đó trả mã 0, làm tiếp các task khác, không kết thúc phiên, soạn danh sách chạy nháp cho task bị bỏ qua; không tự thêm quyền, thư viện, SDK, quảng cáo, sự kiện phân tích, không chạm production, không đổi spec, thiết kế hay câu trả lời của người, không làm yếu test, không đẩy lại lệnh bị từ chối dưới dạng khác; mỗi quyết định tự làm ghi vào mục "Quyết định AI đã tự làm" của báo cáo (task, loại, quyết định, lý do, cách hoàn tác), và mục Cần người kiểm tra luôn có dòng `- [ ] Quyết định AI đã tự làm: <N>`; rà soát độc lập xét mỗi lệch L1 tới L7, L10, lệch ngoài danh sách là mức chặn.
