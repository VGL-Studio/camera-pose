---
ticket: T-067
title: Phiên tự quyết các lệch nhỏ đã liệt kê, bỏ qua loại task mà môi trường không làm được, rồi báo người sau
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 6.3M         # cỡ S, số gợi ý của scripts/token-usage.py estimate (2026-10-27), không nâng tay (L-081)
blocking_points: [process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-067: PHIÊN TỰ QUYẾT CÁC LỆCH NHỎ ĐÃ LIỆT KÊ, RỒI BÁO NGƯỜI SAU

## 1. Tóm tắt

Ba thay đổi, theo đề xuất 2a, 2b, 3a mà Tuấn chọn sau RUN-20261027-01:

- **2a. Lỗi môi trường chỉ chặn đúng loại task.** Bảng tra lỗi có loại mới `bo-qua-loai` và dòng PB-13 cho lệnh đẩy bị từ chối vì thiếu quyền `workflow`. `scripts/plan-check.py --touches <tiền tố> <plan>` cho biết task nào có file thuộc loại bị chặn trong bảng của mục 3.2 (chỉ hàng của bảng, không tính chữ). `docs/auto-run.md` mục Khi không đẩy được tra bảng: lỗi loại này thì bỏ qua mọi task còn lại mà lệnh đó trả mã 0, làm tiếp các task khác, không kết thúc phiên.
- **2b. Danh sách lệch nhỏ được tự làm.** ADR-1005 của app (bổ sung ADR-0029) liệt kê mười lệch nhỏ ADR-1005/L1 tới L10 mà phiên tự làm không cần plan phiên bản mới, ranh giới của từng mục và những việc vẫn cấm.
- **3a. Báo sau ở một chỗ.** Báo cáo phiên có mục "Quyết định AI đã tự làm" (task, loại, quyết định, lý do, cách hoàn tác); mục Cần người kiểm tra luôn có dòng `- [ ] Quyết định AI đã tự làm: <N>, xem mục trên`, hoặc "Không có".

18 test mới trong `scripts/tests/run.sh` (8 cho bảng tra lỗi, 10 cho `--touches`) đã chạy thật trên nhánh chính: 11 thất bại đúng lý do, 7 đối chứng qua; với bản cài thử cả 18 qua, toàn bộ `scripts/tests/run.sh` qua (760 ok); 13 đột biến đều bị bắt (mục 4.1).

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là đề xuất 2a, 2b, 3a của phiên chính sau RUN-20261027-01 (Tuấn chọn ngày 2026-10-27), theo hướng Tuấn nêu: "trao thêm quyền cho AI để tự ra quyết định" và "cứ triển khai trước rồi báo cáo con người sau, thay vì phải đợi quyết định từ con người".

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| 2a. Lỗi đẩy vì thiếu quyền `workflow` | Khi `scripts/failure-kind.sh --playbook` đọc thông báo thật của RUN-20261027-01 thì in `PB-13 bo-qua-loai: ...` | Thông báo của token OAuth App (`claude --cloud`) |
| 2a | Khi thông báo là cách viết của GitHub App (`workflows` permission) hay của personal access token thì cũng ra PB-13 | Cùng một nghĩa, phiên mở bằng cách khác dùng token khác; mẫu `refusing to allow an? [A-Za-z ]+ to create or update workflow` |
| 2a | Khi lệnh đẩy bị từ chối vì lý do khác (`protected branch hook declined`) thì không ra PB-13 | Không bỏ qua nhầm cả loại task |
| 2a | Khi một loại trong bảng không có trong câu liệt kê loại, hay câu đó mất, thì test đỏ | Loại mới phải được mô tả cho phiên đọc |
| 2a | Mỗi dòng `bo-qua-loai` có lệnh `scripts/plan-check.py --touches`; bảng có ít nhất một dòng như vậy | Phiên luôn có một lệnh cụ thể để biết task nào thuộc loại bị chặn |
| 2a | Khi plan có hàng Tạo, Sửa hay Xóa với đường dẫn bắt đầu bằng tiền tố thì `--touches` trả 0; khi tiền tố chỉ có trong chữ của mục 3.2, ở giữa một đường dẫn khác hay trong bảng của mục khác thì trả 1; plan không có hay tiền tố rỗng thì trả 2. Hàng thư mục (đường dẫn kết thúc bằng `/`) được tính, khác `run-check.py` (`files_of` bỏ hàng thư mục): bỏ qua một task có thể chạm tiền tố là phía an toàn | 26 plan nhắc `.github/workflows/` trong mục 3.2, chỉ 7 plan có hàng của bảng; tìm theo chữ thì bỏ qua nhầm cả T-067 |
| 2a | Khi lỗi khớp dòng `bo-qua-loai`, phiên bỏ qua mọi task còn lại mà `--touches` trả 0, làm tiếp task khác, không kết thúc phiên, soạn danh sách chạy nháp | `docs/auto-run.md` mục Khi không đẩy được, bước 5 mới; RUN-20261027-01 phiên đã làm đúng như vậy với T-065 nhưng không có quy tắc |
| 2b | ADR-1005 liệt kê L1 tới L10, ranh giới của từng mục và các việc cấm; `docs/foundation.md` có quy tắc của nó | Danh sách hẹp, rà soát kiểm được (mục 3.1) |
| 2b | `docs/risk-scoring.md` mục 4 có đoạn "Không phải dừng: lệch nhỏ đã liệt kê" với đủ L1 tới L10 | Phiên đọc điều kiện dừng ở đây trước tiên |
| 2b | Bước 6: phiên rà soát nhận danh sách lệch L1 tới L7, L10 (với L3 cả `diff` của plan và `diff` thật); lệch ngoài danh sách là mức chặn | L8 là quyết định của phiên, L9 do chính rà soát nêu, nên không qua rà soát của task |
| 3a | Mẫu báo cáo có mục "Quyết định AI đã tự làm"; mục Cần người kiểm tra luôn có dòng `- [ ] Quyết định AI đã tự làm: <N, xem mục trên>, hoặc "Không có"` | `scripts/status.py` (`is_not_a_task`) bỏ dòng "Không có" và dòng mẫu, hiện dòng có số; không đổi script |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0029 (phần ngoài mục 3.2, sửa điểm nên sửa), ADR-0025 (nhánh chính tự hồi phục), ADR-1004 (chưa áp dụng quảng cáo AdsKit). Bài học: camera-pose L-086 (thiếu quyền `workflow`, proposed), L-084, L-085, L-082, L-081, L-035.

**Nguồn của danh sách lệch L1 tới L10** (đọc mục "Lệch so với plan" của các plan đã chạy, báo cáo RUN-20261023-01 tới RUN-20261027-01, bài học L-062 tới L-086):

- L1: danh sách chạy RUN-20261027-01, câu hỏi 1 (1470 giây so với 1300); T-056, T-058 đã phải đưa việc nâng ngân sách vào plan.
- L2: RUN-20261027-01, dòng T-064 ("kịch bản giữ `need` vì T-062 dừng"); Tuấn cho đổi thành `wait` khi lập danh sách.
- L3: plan T-063 và T-065 phiên bản 2 (sau PR #179), chỉ vì mốc dời chỗ; nhật ký T-063 ("`token-usage.py` có 23 dòng thêm, plan ghi 22").
- L4: nhật ký T-055, T-057 ("thất bại ở bước 3, plan ghi sẽ qua"), T-044 (`beforeLoad_keepsBannerHeight_withSeparator`); L-035, L-082.
- L5: nhật ký T-022 (ktlint cấm file rỗng), T-047 (`AppNavHost` thành `internal`), T-061 (thân lambda dạng khối vì ktlint), T-064 (tách biến `selector` vì dòng dài).
- L6: các PR thêm ADR (`foundation.py --check`).
- L7: nhật ký T-036, T-037, T-039, T-053 ("`docs/lessons.md` không sửa, dòng Áp dụng đã ghi").
- L8: RUN-20261027-01 (T-062, T-065), L-086.
- L9: ADR-0029 quyết định 3; L-082.
- L10: T-066 và T-068 soạn cùng lúc với plan này và có thể cùng nhắm mã PB kế tiếp; PB-12 của app phải chọn mã sau khi đồng bộ dự án mẫu (PR #179).

Không đưa vào danh sách: ảnh so sánh `design/review/` (đã có quy tắc ở `docs/auto-run.md` bước 5, camera-pose L-085); dùng phương án dự phòng (đã có ở `risk-scoring.md` mục 4); phần ngoài mục 3.2 (ADR-0029). Ba việc này vẫn ghi vào mục Quyết định AI đã tự làm (quyết định 3 của ADR-1005).

**Vì sao loại mới `bo-qua-loai`, không dùng `moi-truong`:** `moi-truong` nghĩa là môi trường hỏng cho mọi task (Java, proxy chặn máy chủ, build), nên phiên kết thúc; thiếu quyền `workflow` chỉ chặn task sửa workflow, task khác vẫn đẩy được.

## 3. Giải pháp

### 3.1. Cách làm

**ADR mới `docs/adr/1005-lech-nho-tu-lam-roi-bao.md`**, `origin: app`, `deciders: [Tuấn]`. Trạng thái: `accepted` khi Tuấn nhận ADR này ở mục 9 (câu hỏi 1 của mục 8), `proposed` nếu không. Nội dung (bản cài thử, chép nguyên khi làm task; chỉ dòng `status:` đổi theo câu trả lời):

```markdown
---
id: ADR-1005
title: Phiên tự chạy tự làm các lệch nhỏ đã liệt kê và tự bỏ qua loại task mà môi trường không làm được, rồi báo người sau
status: proposed             # proposed | accepted | superseded
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
```

Sau khi tạo ADR: `scripts/foundation.py` rồi commit `docs/foundation.md` (bản thử thêm 15 dòng của ADR-1005; `scripts/foundation.py --check` qua).

**Các file còn lại**, `diff` của bản cài thử trên nhánh chính `084d366` (áp sạch bằng `git apply --check`; gồm dòng của bảng kết thúc phiên ở `docs/auto-run.md` mục 4; `docs/lessons.md` không có trong `diff`, xem ghi chú dưới):

```diff
diff --git a/docs/auto-run.md b/docs/auto-run.md
index ae63813..62162dd 100644
--- a/docs/auto-run.md
+++ b/docs/auto-run.md
@@ -124,7 +124,7 @@ Task lớn (nhiều file, cỡ L) thì phiên chính giao bước 3 đến 5 cho
 
 ### Bước 6. Rà soát độc lập
 
-Một phiên AI riêng được khởi tạo để rà soát. Phiên này chỉ nhận plan, spec, thiết kế, ADR và phần thay đổi; không nhận quá trình làm việc của phiên viết code, để đánh giá với con mắt mới. Có phần ngoài mục 3.2 (ADR-0029) thì phiên rà soát nhận thêm danh sách file đó, lý do của từng file và `git diff --stat origin/main...HEAD -- <các file đó>`.
+Một phiên AI riêng được khởi tạo để rà soát. Phiên này chỉ nhận plan, spec, thiết kế, ADR và phần thay đổi; không nhận quá trình làm việc của phiên viết code, để đánh giá với con mắt mới. Có phần ngoài mục 3.2 (ADR-0029) thì phiên rà soát nhận thêm danh sách file đó, lý do của từng file và `git diff --stat origin/main...HEAD -- <các file đó>`. Có lệch nhỏ của task (camera-pose ADR-1005/L1 tới L7, L10) thì phiên rà soát nhận danh sách lệch kèm mã; với L3 nhận thêm `diff` của plan và `diff` thật của mỗi chỗ dời (kết quả `git apply -3`). Lệch ngoài danh sách của ADR-1005 hay vi phạm quyết định 2 của nó là mức chặn. L8 (bỏ qua task) và L9 (điểm nên sửa, ADR-0029) không qua rà soát của task: L8 là quyết định của phiên, L9 đã do chính rà soát nêu.
 
 Nội dung rà soát:
 
@@ -172,7 +172,8 @@ Lệnh đẩy của phiên chính bị môi trường từ chối (bộ phân lo
 2. Chạy `scripts/save-branch.sh --bundle-only <nhánh>` (nếu chưa có bundle). Bundle nằm ở `$HOME/rescue/`, ngoài worktree. Phần chưa commit thì commit trước (L-069).
 3. Dừng task theo `risk-scoring.md` mục 4. Plan và ticket giữ `approved`, vì không phải lỗi của plan. Nhật ký plan ghi mã commit.
 4. Chép đoạn "Nhánh chưa đẩy" mà script in ra, cùng nguyên văn lệnh bị từ chối và thông báo, vào báo cáo phiên.
-5. Làm tiếp task sau. Lần đẩy nhánh của task sau (bước 1) cũng bị từ chối thì kết thúc phiên (mục 4), vì mọi task đều cần đẩy.
+5. Tra thông báo bị từ chối trong bảng tra lỗi. Chép nguyên văn dòng lỗi mà lệnh đẩy đã in ra (dòng có `[remote rejected]` hay `refusing`) vào một tệp, không chạy lại lệnh đẩy: `printf '%s\n' '<dòng lỗi>' > "${TMPDIR:-/tmp}/push-refused.log"`, rồi `scripts/failure-kind.sh --playbook "${TMPDIR:-/tmp}/push-refused.log"`. Dòng loại `bo-qua-loai` (ví dụ PB-13, thiếu quyền `workflow`): với mỗi task còn lại của danh sách, chạy lệnh `scripts/plan-check.py --touches <tiền tố> <plan>` mà dòng đó nêu; mã 0 thì bỏ qua task (cùng các task phụ thuộc nó), mã khác 0 thì làm như thường. Soạn danh sách chạy nháp cho các task bị bỏ qua, ghi vào mục Quyết định AI đã tự làm (camera-pose ADR-1005/L8). Không kết thúc phiên vì lỗi loại này.
+6. Làm tiếp task sau. Lần đẩy nhánh của task sau (bước 1) cũng bị từ chối, mà thông báo không khớp dòng `bo-qua-loai` nào, thì kết thúc phiên (mục 4), vì mọi task đều cần đẩy.
 
 ### Bước 8. CI đầy đủ
 
@@ -264,7 +265,7 @@ Phiên kết thúc khi gặp một trong các điều kiện sau:
 | Hết hạn mức sử dụng | Phiên dừng ngay, không làm được gì thêm. Phần đã đẩy lên còn nguyên (bước 3). `resume-run.yml` kích hoạt lại phiên khi hạn mức được làm mới (mục Tiếp tục phiên sau khi hết hạn mức) |
 | Lỗi hạ tầng còn sau lịch thử lại ở bước 5 | Như trên. Plan và ticket giữ `approved`; báo cáo ghi tên nhánh để lần sau tiếp tục trên nhánh đó |
 | GitHub không chạy job (`wait-run.sh` mã 4, L-064) | Đẩy hết những gì đang có: nhánh của task đang làm, nhánh báo cáo với dòng thời gian ghi giờ, tên workflow và mã lần chạy bị bỏ. Giữ danh sách chạy `running`, plan và ticket giữ trạng thái; không bắt đầu task mới. `resume-run.yml` kích hoạt lại phiên khi GitHub chạy lại được (nó cũng chỉ chạy được từ lúc đó) |
-| Lệnh đẩy nhánh bị từ chối ở hai task liên tiếp | Đẩy nhánh báo cáo nếu được; báo cáo ghi các bundle (bước 7, "Khi không đẩy được") |
+| Lệnh đẩy nhánh bị từ chối ở hai task liên tiếp, mà thông báo không khớp dòng `bo-qua-loai` nào của bảng tra lỗi (camera-pose ADR-1005/L8) | Đẩy nhánh báo cáo nếu được; báo cáo ghi các bundle (bước 7, "Khi không đẩy được") |
 | Nhánh chính đỏ sau một lần merge (xuất hiện issue mang nhãn `main-broken`) | Dừng ngay, không làm thêm task nào. Trừ khi commit gây lỗi là commit merge của một task chính phiên: revert theo mục 3, Khi nhánh chính đỏ vì task của phiên, rồi làm tiếp các task không phụ thuộc task đó (ADR-0025) |
 
 ### Tiếp tục phiên sau khi hết hạn mức
@@ -352,6 +353,7 @@ Báo cáo phiên là thứ dev đọc đầu tiên khi bắt đầu làm việc
 
 - Nhánh chưa đẩy, ngay dưới Tóm tắt khi có bundle: chép đoạn `scripts/save-branch.sh` in ra.
 - Kết quả từng task: đã merge, bị dừng, bị bỏ qua, kèm lý do.
+- Quyết định AI đã tự làm: mỗi lệch nhỏ (camera-pose ADR-1005/L1 tới L10), phần ngoài mục 3.2 (ADR-0029), lần chuyển sang phương án dự phòng, task bỏ qua vì lỗi môi trường; mỗi dòng có task, loại, quyết định, lý do, cách hoàn tác. Mục Cần người kiểm tra luôn có dòng `- [ ] Quyết định AI đã tự làm: <N>, xem mục trên`, hoặc "Không có".
 - Những gì người cần tự kiểm tra: ảnh chuẩn screenshot mới, tiêu chí kiểm tra thủ công máy thật và người, kịch bản emulator lỗi hay bị bỏ qua, điểm rà soát mức ghi chú.
 - Task bị dừng: dừng ở bước nào, vì sao, đề xuất hướng xử lý.
 - Thông tin bản staging: phiên bản, kênh phát.
diff --git a/docs/ops/failure-playbook.md b/docs/ops/failure-playbook.md
index 6672b1c..99984e2 100644
--- a/docs/ops/failure-playbook.md
+++ b/docs/ops/failure-playbook.md
@@ -4,7 +4,7 @@ Bảng này gom các lỗi đã gặp thật của phiên tự chạy: thông b
 
 Cách thêm một dòng: mỗi bài học loại `môi trường` có thông báo lỗi dò được thì thêm một dòng ở cuối, mã `PB-NN` kế tiếp (không dùng lại mã đã xóa), và một tệp `scripts/tests/playbook/PB-NN.log` chép đoạn log thật. Test báo lỗi khi dòng nào thiếu tệp log. Thứ tự dòng là thứ tự dò: dòng cụ thể đặt trước dòng chung. App tạo từ dự án mẫu thêm dòng cho lỗi riêng của mình theo cùng cách, ví dụ thẻ log của màn tải riêng.
 
-Cột Mẫu là biểu thức chính quy kiểu `grep -E` trong dấu backtick; dấu `|` trong mẫu viết `\|` vì Markdown. Cột Loại là một trong: `chay-lai`, `lich-thu-lai`, `cho`, `gop-nhanh`, `kiem-da-merge`, `moi-truong`, `nguoi`, `that`. Các loại `chay-lai`, `lich-thu-lai`, `cho`, `gop-nhanh`, `kiem-da-merge` không tính vào 3 lần sửa.
+Cột Mẫu là biểu thức chính quy kiểu `grep -E` trong dấu backtick; dấu `|` trong mẫu viết `\|` vì Markdown. Cột Loại là một trong: `chay-lai`, `lich-thu-lai`, `cho`, `gop-nhanh`, `kiem-da-merge`, `bo-qua-loai`, `moi-truong`, `nguoi`, `that`. Các loại `chay-lai`, `lich-thu-lai`, `cho`, `gop-nhanh`, `kiem-da-merge`, `bo-qua-loai` không tính vào 3 lần sửa. Loại `bo-qua-loai` (camera-pose ADR-1005/L8): môi trường của phiên không làm được một loại việc, nhưng các việc khác vẫn làm được; phiên giữ task đang làm theo `auto-run.md` mục Khi không đẩy được, bỏ qua mọi task còn lại của danh sách thuộc loại đó và làm tiếp các task khác, không kết thúc phiên. Cột Việc phải làm của mỗi dòng `bo-qua-loai` có lệnh `scripts/plan-check.py --touches <tiền tố> <plan>` cho biết task nào thuộc loại bị chặn. Khác `moi-truong`: `moi-truong` là môi trường hỏng cho mọi task (build, Java, proxy), nên phiên kết thúc.
 
 <!-- playbook:start -->
 | Mã | Mẫu | Thấy ở | Loại | Việc phải làm | Nguồn |
@@ -21,4 +21,5 @@ Cột Mẫu là biểu thức chính quy kiểu `grep -E` trong dấu backtick;
 | PB-09 | `Could not (GET\|HEAD\|resolve\|download) .*(status code (429\|5[0-9][0-9])\|timed out)\|Read timed out\|Connect timed out\|Connection reset` | `check.sh` (Gradle tải thư viện) | `lich-thu-lai` | Lỗi tải thư viện. Chạy lại theo lịch 1, 2, 4, 8, 15 phút của bước 5, không tính vào 3 lần sửa. Hết lịch thì kết thúc phiên (mục 4) | `auto-run.md` bước 5 |
 | PB-10 | `ComposeTimeoutException: Condition still not satisfied` | job `ui-test` | `that` | Test trên emulator chờ hết giờ. Tra PB-01 trước (dòng trên khớp thì là R2). Không khớp: lỗi thật, đọc bước "Chi tiết test trên emulator" (bước 8) | L-079 |
 | PB-11 | `oauth_org_not_allowed\|organization has disabled Claude subscription access` | kết quả của phiên con (lỗi API 403) | `cho` | Tài khoản Claude của tổ chức tạm bị chặn, không phải lỗi của code. Phiên con chết thì làm theo `auto-run.md`, mục Giao bước 3 đến 5 (phiên con kết thúc mà không có báo cáo): tiếp từ phần đã có trong worktree. Lỗi ở chính phiên chính thì phiên dừng; người mở lại theo mục 4, Tiếp tục phiên sau khi hết hạn mức, và báo người quản trị tổ chức. Không tính vào 3 lần sửa | camera-pose L-084, RUN-20261026-03 |
+| PB-13 | `refusing to allow an? [A-Za-z ]+ to create or update workflow` | lệnh đẩy (`scripts/save-branch.sh`, `git push`) | `bo-qua-loai` | Token đẩy của phiên không có quyền sửa file workflow. Không đẩy lại dưới dạng khác. Làm theo `auto-run.md` mục Khi không đẩy được cho task này. Loại bị chặn: task có file `.github/workflows/` trong bảng của mục 3.2; với mỗi task còn lại, `scripts/plan-check.py --touches .github/workflows/ <plan>` mã 0 thì bỏ qua task đó, mã khác 0 thì làm như thường. Ghi vào mục Quyết định AI đã tự làm và soạn danh sách chạy nháp cho các task bị bỏ qua (camera-pose ADR-1005/L8). Người cấp quyền `workflow` hay chạy các task đó bằng cách mở phiên đẩy được workflow | camera-pose L-086, RUN-20261027-01 |
 <!-- playbook:end -->
diff --git a/docs/risk-scoring.md b/docs/risk-scoring.md
index bfe35a0..84668db 100644
--- a/docs/risk-scoring.md
+++ b/docs/risk-scoring.md
@@ -65,6 +65,8 @@ Trong lúc thực hiện plan đã duyệt, AI **phải dừng** task, không t
 - Điều kiện dừng riêng ở mục 7 của plan vẫn áp dụng.
 - Ghi vào nhật ký plan (file, vì sao cần, loại), mô tả PR (mục "Ngoài mục 3.2", kèm `git diff --stat origin/main...HEAD -- <các file đó>`) và báo cáo phiên (một dòng `- [ ]` ở mục Cần người kiểm tra). Rà soát độc lập xét riêng phần này; vi phạm ADR-0029 là mức chặn (`auto-run.md`, bước 6).
 
+**Không phải dừng: lệch nhỏ đã liệt kê (camera-pose ADR-1005).** ADR-1005/L1 ngân sách kịch bản; L2 `need` thành `wait` khi emulator đã có thiết bị; L3 mốc tài liệu và mã dời chỗ (mọi dòng `-` của plan có nguyên văn ở chỗ mới); L4 test viết trước thất bại khi plan ghi qua ngay; L5 chi tiết do ktlint, lint hay trình biên dịch buộc; L6 tệp sinh bằng `scripts/foundation.py`, `scripts/status.py`; L7 dòng Áp dụng đã ghi; L8 bỏ qua loại task theo dòng `bo-qua-loai` của bảng tra lỗi; L9 điểm nên sửa (ADR-0029); L10 mã kế tiếp đã có người dùng. Ghi vào mục Quyết định AI đã tự làm của báo cáo. Không bao giờ là lý do để vượt điều kiện 1, 2, 3, 5, 7, 8 hay 9.
+
 **Không phải dừng: chuyển sang phương án dự phòng đã duyệt.** Plan có mục 3.4 (Phương án dự phòng) đã được duyệt cùng plan. Khi phương án chính gặp đúng điều kiện chuyển ghi trong mục đó, hoặc gặp điều kiện 4 ở trên, AI chuyển sang phương án dự phòng thay vì dừng: chỉ chạm file của mục 3.2 (cộng phần ngoài mục 3.2 theo ADR-0029, như với phương án chính), giữ nguyên test viết trước và tiêu chí chấp nhận, đếm lại giới hạn 3 lần sửa từ đầu, ghi vào nhật ký lúc chuyển và lý do. Chỉ chuyển một lần; phương án dự phòng cũng không qua thì dừng như bình thường. Phương án dự phòng không bao giờ là lý do để vượt điều kiện 1, 2, 3, 5, 7, 8 hay 9.
 
 Khi dừng, AI làm ba việc:
diff --git a/docs/templates/run.md b/docs/templates/run.md
index 47b31f8..1933675 100644
--- a/docs/templates/run.md
+++ b/docs/templates/run.md
@@ -64,6 +64,14 @@ Task bị bỏ qua: <mã ticket và lý do, hoặc "Không có">
 | T-NNN | Đã merge | <liên kết> | 8 test, đã thất bại đúng lý do | 0 chặn, 1 ghi chú | Xanh | |
 | T-NNN | Dừng ở bước 8 | <liên kết> | | | Đỏ sau 3 lần sửa | Xem nhật ký plan |
 
+### Quyết định AI đã tự làm
+
+<!-- auto-run.md, mục 5; camera-pose ADR-1005. Lệch nhỏ L1 tới L10, phần ngoài mục 3.2 (ADR-0029), phương án dự phòng, task bỏ qua vì lỗi môi trường. Ghi "Không có" nếu không có. -->
+
+| Task | Loại | Quyết định | Lý do | Cách hoàn tác |
+| --- | --- | --- | --- | --- |
+| T-NNN | ADR-1005/L1 | <ví dụ: `DEVICE_SCENARIOS_BUDGET` 1300 thành 1500> | <tổng theo công thức 1470 giây> | <đặt lại 1300 ở ba file, PR mới> |
+
 ### Cần người kiểm tra
 
 - [ ] Ảnh chuẩn screenshot mới: <màn hình, đường dẫn ảnh>
@@ -71,6 +79,7 @@ Task bị bỏ qua: <mã ticket và lý do, hoặc "Không có">
 - [ ] Tiêu chí kiểm tra thủ công (máy thật, người): <ticket, tiêu chí, các bước>
 - [ ] Kịch bản emulator lỗi hay bỏ qua: <tệp, kết quả, bước lỗi, phản hồi FB-...>, hoặc "Không có"
 - [ ] Phần ngoài mục 3.2 đã merge (ADR-0029): <ticket, file, vì sao cần, PR>, hoặc "Không có"
+- [ ] Quyết định AI đã tự làm: <N, xem mục trên>, hoặc "Không có"
 - [ ] Điểm rà soát mức ghi chú: <ticket, nội dung>
 - [ ] Test chập chờn: <issue `flaky` đang mở, tên test>, hoặc "Không có" (ADR-0025)
 - [ ] Task bị revert: <ticket, issue `main-broken`, PR revert>, hoặc "Không có" (ADR-0025)
diff --git a/scripts/plan-check.py b/scripts/plan-check.py
index b215269..2ca8d86 100755
--- a/scripts/plan-check.py
+++ b/scripts/plan-check.py
@@ -4,6 +4,14 @@
 Usage: scripts/plan-check.py <plan.md> [<plan.md> ...]
 Exit 0: no error (warnings may be printed). Exit 1: at least one error.
 
+Usage: scripts/plan-check.py --touches <path prefix> <plan.md>
+Exit 0: a row of the file table of section 3.2 (Tạo, Sửa, Ghi lại, Xóa) names a path under the prefix; exit 1: none
+does (prose of section 3.2 and tables of other sections do not count). A directory row (path ending in "/") counts,
+unlike in run-check.py: skipping a task that may touch the prefix is the safe side. Exit 2: wrong arguments, an empty
+prefix or no such plan. An auto-run session uses it to
+skip the remaining tasks a known environment error blocks (docs/ops/failure-playbook.md, kind bo-qua-loai;
+ADR-1005 of camera-pose).
+
 Finds, from the plan alone, the mistakes that otherwise stop a task in the middle of the night
 (RUN-20261016-02 stopped on a test that read a file the plan changed, L-020):
   1. The file list (section 3.2) matches the repository: files to change exist, files to create do not,
@@ -493,7 +501,18 @@ def check(plan_path):
     return errors, warnings
 
 
+def touches(argv):
+    if len(argv) != 4 or not argv[2] or not Path(argv[3]).is_file():
+        print("Cách dùng: scripts/plan-check.py --touches <tiền tố đường dẫn> <plan.md> (plan phải có).", file=sys.stderr)
+        return 2
+    prefix = argv[2]
+    rows = file_list(Path(argv[3]).read_text(encoding="utf-8"))
+    return 0 if any(path.startswith(prefix) for _, path in rows) else 1
+
+
 def main(argv):
+    if len(argv) > 1 and argv[1] == "--touches":
+        return touches(argv)
     if len(argv) < 2:
         print("Cách dùng: scripts/plan-check.py <plan.md> [<plan.md> ...]", file=sys.stderr)
         return 2
diff --git a/scripts/tests/playbook/PB-13.log b/scripts/tests/playbook/PB-13.log
new file mode 100644
index 0000000..96429ba
--- /dev/null
+++ b/scripts/tests/playbook/PB-13.log
@@ -0,0 +1 @@
+ ! [remote rejected] claude/T-062-camera-gia -> claude/T-062-camera-gia (refusing to allow an OAuth App to create or update workflow `.github/workflows/device-session.yml` without `workflow` scope)
diff --git a/scripts/tests/run.sh b/scripts/tests/run.sh
index 910d4dd..cb7942e 100755
--- a/scripts/tests/run.sh
+++ b/scripts/tests/run.sh
@@ -528,6 +528,13 @@ expect "playbook: PB-09 library download 429 -> lich-thu-lai" "PB-09 lich-thu-la
 expect "playbook: PB-10 a UI timeout without R2 lines is a real failure" "PB-10 that" "$(pb_head $pb_dir/PB-10.log)"
 expect "playbook: PB-11 Claude org access 403 in a subagent -> cho" "PB-11 cho" "$(pb_head $pb_dir/PB-11.log)"
 expect "playbook: PB-12 an R2 file missing (404) in ui-test -> nguoi, before the rerun of PB-01" "PB-12 nguoi" "$(pb_head $pb_dir/PB-12.log)"
+expect "playbook: PB-13 push refused for a workflow file (OAuth App, workflow scope) -> bo-qua-loai" "PB-13 bo-qua-loai" "$(pb_head $pb_dir/PB-13.log)"
+printf '%s\n' ' ! [remote rejected] claude/T-1 -> claude/T-1 (refusing to allow a GitHub App to create or update workflow `.github/workflows/ci.yml` without `workflows` permission)' > "$work/pb13-app.log"
+expect "playbook: PB-13 also matches the GitHub App wording (workflows permission)" "PB-13 bo-qua-loai" "$(pb_head "$work/pb13-app.log")"
+printf '%s\n' ' ! [remote rejected] claude/T-1 -> claude/T-1 (refusing to allow a Personal Access Token to create or update workflow `.github/workflows/ci.yml` without `workflow` scope)' > "$work/pb13-pat.log"
+expect "playbook: PB-13 also matches the personal access token wording" "PB-13 bo-qua-loai" "$(pb_head "$work/pb13-pat.log")"
+printf '%s\n' ' ! [remote rejected] claude/T-1 -> claude/T-1 (protected branch hook declined)' > "$work/pb13-other.log"
+expect "playbook: a push refused for another reason is not PB-13" "khong-co" "$(pb_head "$work/pb13-other.log")"
 expect "playbook: an unknown failure says khong-co" "khong-co" "$(pb_head $pb_dir/none.log)"
 expect "playbook: a missing log says khong-co" "khong-co" "$(pb_head "$work/no-such.log")"
 expect "playbook: a missing log exits 0" 0 "$(scripts/failure-kind.sh --playbook "$work/no-such.log" > /dev/null 2>&1; echo $?)"
@@ -560,6 +567,14 @@ expect "playbook: every pattern of the real table is valid" 0 "$(grep -c 'LỖI
 pb_rows="$(sed -n '/playbook:start/,/playbook:end/p' docs/ops/failure-playbook.md | grep -oE '^\| PB-[0-9]+' | tr -d '| ' | sort)"
 pb_files="$(ls $pb_dir | grep -oE '^PB-[0-9]+' | sort -u)"
 expect "playbook: every row has a log file and every log file a row" "$pb_rows" "$pb_files"
+pb_kinds="$(sed -n '/playbook:start/,/playbook:end/p' docs/ops/failure-playbook.md | grep -E '^\| PB-[0-9]+ ' | awk -F' \\| ' '{ print $4 }' | tr -d '`' | sort -u)"
+pb_known="$(grep -m1 '^Cột Mẫu là' docs/ops/failure-playbook.md | sed -nE 's/.*Cột Loại là một trong: ([^.]*)\..*/\1/p' | grep -oE '`[a-z-]+`' | tr -d '`' | sort -u || true)"
+expect "playbook: the kinds sentence lists the kinds (that among them)" yes "$(printf '%s\n' "$pb_known" | grep -qx that && echo yes || echo no)"
+expect "playbook: every kind in the table is listed in the kinds sentence" "" "$(comm -23 <(printf '%s\n' "$pb_kinds") <(printf '%s\n' "$pb_known"))"
+pb_skip_rows="$(sed -n '/playbook:start/,/playbook:end/p' docs/ops/failure-playbook.md | grep -E '^\| PB-[0-9]+ .*\| `bo-qua-loai` \|' || true)"
+expect "playbook: every bo-qua-loai row names the plan-check --touches command" "" \
+  "$(printf '%s\n' "$pb_skip_rows" | grep -v 'scripts/plan-check.py --touches ' || true)"
+expect "playbook: the table has at least one bo-qua-loai row" yes "$([ -n "$pb_skip_rows" ] && echo yes || echo no)"
 expect "playbook: the table has at least 10 rows" yes "$([ "$(printf '%s\n' "$pb_rows" | wc -l)" -ge 10 ] && echo yes || echo no)"
 expect "failure-kind: a playbook-only log still says other" other "$(scripts/failure-kind.sh $pb_dir/PB-01.log)"
 
@@ -1045,6 +1060,27 @@ row() { printf '| %s | `%s` | x |' "$1" "$2"; }
 plans="$repo/docs/features/demo/plans"
 write_plan "$plans/T-901-a.md" medium draft "$(row Sửa feature/demo/A.kt)"
 expect "plan-check: a correct plan passes" pass "$(plan_check "$plans/T-901-a.md")"
+# --touches: only a row of the file table counts, never prose of section 3.2 (ADR-1005 of camera-pose, PB-13).
+touches() { python3 -I scripts/plan-check.py --touches "$@" > /dev/null 2>&1; echo $?; }
+write_plan "$plans/T-931-a.md" high draft "$(row Sửa .github/workflows/ci.yml)"
+expect "plan-check --touches: a Sửa row under the prefix -> 0" 0 "$(touches .github/workflows/ "$plans/T-931-a.md")"
+write_plan "$plans/T-932-a.md" high draft "$(row Tạo .github/workflows/new.yml)\n$(row Sửa feature/demo/A.kt)"
+expect "plan-check --touches: a Tạo row among others -> 0" 0 "$(touches .github/workflows/ "$plans/T-932-a.md")"
+write_plan "$plans/T-933-a.md" medium draft "$(row Sửa feature/demo/A.kt)\n\nGhi chú: \`.github/workflows/ci.yml\` chỉ nhắc tới file này, không đọc."
+expect "plan-check --touches: workflows named only in prose -> 1" 1 "$(touches .github/workflows/ "$plans/T-933-a.md")"
+expect "plan-check --touches: no such plan -> 2" 2 "$(touches .github/workflows/ "$plans/T-999-none.md")"
+expect "plan-check --touches: an empty prefix -> 2" 2 "$(touches "" "$plans/T-931-a.md")"
+write_plan "$plans/T-934-a.md" high draft "$(row Xóa .github/workflows/old.yml)"
+expect "plan-check --touches: a Xóa row under the prefix -> 0" 0 "$(touches .github/workflows/ "$plans/T-934-a.md")"
+write_plan "$plans/T-935-a.md" medium draft "$(row Sửa docs/.github/workflows/x.md)"
+expect "plan-check --touches: the prefix in the middle of a path -> 1" 1 "$(touches .github/workflows/ "$plans/T-935-a.md")"
+write_plan "$plans/T-936-a.md" medium draft "$(row Sửa feature/demo/A.kt)" \
+  "$(printf '### 3.4. Phương án dự phòng\n\n| Thao tác | Đường dẫn | Nội dung |\n| --- | --- | --- |\n%s' "$(row Sửa .github/workflows/ci.yml)")"
+expect "plan-check --touches: a table outside section 3.2 -> 1" 1 "$(touches .github/workflows/ "$plans/T-936-a.md")"
+expect "plan-check --touches: the real T-067 plan (workflows only in prose) -> 1" 1 \
+  "$(touches .github/workflows/ docs/features/quality-gate/plans/T-067-tu-quyet-roi-bao.md)"
+expect "plan-check --touches: a real plan that changes a workflow (T-062) -> 0" 0 \
+  "$(touches .github/workflows/ docs/features/quality-gate/plans/T-062-camera-gia-tren-emulator-ci.md)"
 write_plan "$plans/T-902-a.md" medium draft "$(row Sửa feature/demo/Missing.kt)"
 expect "plan-check: changing a missing file stops" stop "$(plan_check "$plans/T-902-a.md")"
 write_plan "$plans/T-903-a.md" medium draft "$(row Sửa docs/data/values.json)"
```

Ghi chú về `diff`:

- `docs/auto-run.md`, `docs/ops/failure-playbook.md`, `docs/risk-scoring.md`, `docs/templates/run.md`, `scripts/plan-check.py`, `scripts/tests/run.sh` là file lấy từ dự án mẫu (PR #179). Câu mới ghi "camera-pose ADR-1005" theo quy tắc ở đầu `docs/CLAUDE.md`. Đề xuất về dự án mẫu (`docs/ops/from-app.md`), cũng ghi trong mô tả PR của plan này:
  - loại `bo-qua-loai` và dòng PB-13 (mã kế tiếp của mẫu), kèm tệp log và 8 test của khối `playbook`;
  - `scripts/plan-check.py --touches` và 5 test;
  - bước 5 mới của mục Khi không đẩy được và dòng bảng kết thúc phiên ở mục 4;
  - mục "Quyết định AI đã tự làm" của mẫu báo cáo;
  - danh sách lệch L1 tới L10 thành một ADR của mẫu.

  Phiên tự chạy không mở issue ở repo khác: ghi vào mục Cần người kiểm tra của báo cáo phiên để người mở issue `from-app`.
- Mẫu PB-13 `refusing to allow an? [A-Za-z ]+ to create or update workflow`: nhận cách viết của OAuth App, GitHub App và personal access token; không trùng dòng nào khác (đối chứng ở mục 4).
- `--touches` dùng lại `file_list()` của `scripts/plan-check.py` (hàng Tạo, Sửa, Ghi lại, Xóa của bảng mục 3.2), nên chữ trong mục 3.2 và bảng của mục khác không được tính. Hàng thư mục được tính (khác `files_of` của `run-check.py`), vì bỏ qua thừa an toàn hơn bỏ qua thiếu. Lệnh không có dấu `|`, nên chép được nguyên vào ô của bảng tra lỗi. Mã khác 0 (1 hay 2) thì phiên làm task như thường.
- `docs/lessons.md`, L-086: dòng Áp dụng có thể đã có phần của T-066 (PR #192 ghi "làm ở ticket T-066 ...; dòng bảng tra lỗi ở T-067 ..." và chuyển L-086 sang `accepted`). Cách làm, không phải `diff`: dòng Áp dụng đã có chữ thì thêm `; T-067: dòng PB-13 (loại `bo-qua-loai`) ở docs/ops/failure-playbook.md, docs/auto-run.md mục Khi không đẩy được bước 5, scripts/plan-check.py --touches, ADR-1005/L8` vào cuối dòng, không thay chữ đã có; dòng còn trống thì ghi `- **Áp dụng:** T-067: ...` với cùng nội dung. Trạng thái của L-086 không đổi.
- `scripts/status.py` không đổi: dòng `- [ ] Quyết định AI đã tự làm: ...` nằm trong mục Cần người kiểm tra; `is_not_a_task` bỏ dòng mẫu (`<N, xem mục trên>`) và dòng "Không có", hiện dòng có số.
- Thứ tự commit (L-025):
  - Bước 3, commit test: `scripts/tests/run.sh` (khối trên) và `scripts/tests/playbook/PB-13.log`, để test PB-13 thất bại vì thiếu dòng của bảng (`khong-co`), không vì thiếu tệp. Test "mỗi tệp log có dòng" cũng thất bại trên commit này, đúng lý do.
  - Bước 4, commit code: `scripts/plan-check.py`, `docs/ops/failure-playbook.md`, ADR-1005, `docs/foundation.md`, `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/templates/run.md`, `docs/lessons.md`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `docs/adr/1005-lech-nho-tu-lam-roi-bao.md` | ADR mới của app, nội dung ở mục 3.1 |
| Sửa | `docs/foundation.md` | Sinh lại bằng `scripts/foundation.py` (thêm mục ADR-1005) |
| Sửa | `docs/ops/failure-playbook.md` | Loại `bo-qua-loai` trong câu liệt kê loại, kèm cách hiểu; dòng PB-13 |
| Tạo | `scripts/tests/playbook/PB-13.log` | Một dòng thông báo thật của RUN-20261027-01 |
| Sửa | `scripts/plan-check.py` | Cách gọi `--touches <tiền tố> <plan>` (mã 0, 1, 2) và docstring |
| Sửa | `scripts/tests/run.sh` | 8 test trong khối `playbook`, 10 test `--touches` trong khối `plan-check` |
| Sửa | `docs/auto-run.md` | Bước 6 (rà soát xét lệch), mục Khi không đẩy được (bước 5 mới, bước 6), mục 4 (dòng "Lệnh đẩy nhánh bị từ chối ở hai task liên tiếp"), mục 5 Báo cáo phiên |
| Sửa | `docs/risk-scoring.md` | Mục 4: đoạn "Không phải dừng: lệch nhỏ đã liệt kê (camera-pose ADR-1005)" |
| Sửa | `docs/templates/run.md` | Mục "Quyết định AI đã tự làm"; dòng `- [ ] Quyết định AI đã tự làm: ...` |
| Sửa | `docs/lessons.md` | L-086: thêm phần T-067 vào cuối dòng Áp dụng, không thay chữ đã có (cách làm ở mục 3.1) |

Nơi đọc các file bị đổi (L-020): `grep -rn 'failure-playbook\|templates/run.md\|risk-scoring.md\|foundation.md\|plan-check.py' scripts/ .github/ */src/test`:

- `scripts/failure-kind.sh` đọc bảng tra lỗi theo dấu `<!-- playbook:start -->` tới `end` và 6 cột; dòng mới giữ đúng 6 cột, loại mới là chuỗi tự do (script không kiểm tên loại); lệnh `--touches` trong ô không có dấu `|`.
- `scripts/tests/run.sh`: test của bảng (mỗi dòng có tệp log, ít nhất 10 dòng, mẫu hợp lệ) và test `docs/templates/run.md` có dòng `- [ ]` ở mục Cần người kiểm tra (vẫn có).
- `scripts/plan-check.py` được gọi bởi `scripts/run-check.py`, `scripts/plan-queue.py`, `.github/workflows/pr-check.yml` (qua `scripts/tests/run.sh`) và phiên tự chạy, luôn theo cách cũ `plan-check.py <plan> ...`; cách gọi mới chỉ chạy khi đối số đầu là `--touches`, nên cách cũ không đổi (mọi test cũ của khối `plan-check` qua).
- `scripts/status.py` đọc mục Cần người kiểm tra của báo cáo `finished` (mục mới đứng trước, không xen vào mục đó); `scripts/run-check.py` không đọc `docs/templates/run.md`.
- `scripts/foundation.py --check` chạy trong `scripts/tests/run.sh` (repo app mới tạo từ mẫu, dòng 941): ADR mới của app không ảnh hưởng test đó vì nó dựng repo riêng.
- `docs/auto-run.md` chỉ được nhắc trong chú thích hay thông báo (dẫn mục, bước) của `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/main-guard-status.sh`, `scripts/resume-check.py`, `scripts/wait-run.sh`, `scripts/save-branch.sh`. Plan không đổi tên hay số của mục, bước nào mà các file đó dẫn (mục 2, 3, 4; bước 5, 8; mục Tiếp tục phiên; mục Khi không đẩy được): chỉ thêm bước 5 vào mục Khi không đẩy được (bước cũ thành 6), thêm câu vào bước 6, mục 5 và sửa một dòng của bảng mục 4. Không file nào trong số đó dẫn "Khi không đẩy được, bước 5".
- `docs/foundation.md` và `scripts/tests/run.sh` được `.github/workflows/pr-check.yml` dùng qua lệnh `scripts/tests/run.sh` (job kiểm script và tài liệu): plan chỉ thêm test và sinh lại trang bằng script, không đổi cách gọi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có. Thay đổi là tài liệu, một dòng bảng tra lỗi, một tệp log, một cách gọi mới của `plan-check.py` dùng lại hàm có sẵn, và 13 test bash đã chạy thật (mục 4.1).

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| 2a, thông báo thật ra PB-13 | Test script (`scripts/tests/run.sh`) | `playbook: PB-13 push refused for a workflow file (OAuth App, workflow scope) -> bo-qua-loai`. Trên nhánh chính (có `PB-13.log` từ commit test): `khong-co`, thất bại đúng lý do |
| 2a, cách viết GitHub App | Test script | `playbook: PB-13 also matches the GitHub App wording (workflows permission)`. Thất bại đúng lý do (`khong-co`) |
| 2a, cách viết personal access token | Test script | `playbook: PB-13 also matches the personal access token wording`. Thất bại đúng lý do (`khong-co`) |
| 2a, từ chối vì lý do khác | Test script, đối chứng | `playbook: a push refused for another reason is not PB-13`. Qua trước và sau; bắt đột biến "mẫu quá rộng" |
| 2a, câu liệt kê loại còn và có `that` | Test script, đối chứng | `playbook: the kinds sentence lists the kinds (that among them)`. Qua trước và sau; bắt đột biến "đổi câu liệt kê" |
| 2a, mọi loại trong bảng có trong câu | Test script, đối chứng | `playbook: every kind in the table is listed in the kinds sentence`. Qua trước và sau; bắt đột biến "quên thêm `bo-qua-loai` vào câu" |
| 2a, dòng `bo-qua-loai` có lệnh `--touches` | Test script | `playbook: every bo-qua-loai row names the plan-check --touches command` (đối chứng, rỗng trên nhánh chính) và `playbook: the table has at least one bo-qua-loai row` (thất bại đúng lý do: `no`) |
| 2a, mỗi dòng có tệp log | Test script có sẵn | `playbook: every row has a log file and every log file a row`. Trên commit test: thất bại đúng lý do (có `PB-13.log` mà chưa có dòng) |
| 2a, `--touches` theo hàng của bảng | Test script | Thất bại đúng lý do trên nhánh chính (`got '1'`, vì `--touches` bị coi là tên plan): `plan-check --touches: a Sửa row under the prefix -> 0`, `... a Tạo row among others -> 0`, `... a Xóa row under the prefix -> 0`, `... a real plan that changes a workflow (T-062) -> 0`, `... no such plan -> 2`, `... an empty prefix -> 2`. Đối chứng, qua trước và sau: `... workflows named only in prose -> 1`, `... the prefix in the middle of a path -> 1`, `... a table outside section 3.2 -> 1`, `... the real T-067 plan (workflows only in prose) -> 1` |
| 2a, phiên bỏ qua đúng loại và làm tiếp | Kiểm tra thủ công (người: đọc tài liệu) | `docs/auto-run.md` mục Khi không đẩy được có bước 5 mới như mục 3.1; ở phiên đầu tiên gặp PB-13, báo cáo có dòng ADR-1005/L8 ở mục Quyết định AI đã tự làm |
| 2b, ADR-1005 vào trang quy tắc | Test script có sẵn | `scripts/foundation.py --check` (kiểm riêng và trong `scripts/check.sh`); thiếu `docs/foundation.md` sinh lại thì báo "chưa khớp" (đã thấy ở bản thử) |
| 2b, `risk-scoring.md` và bước 6 dẫn ADR-1005 | Kiểm tra thủ công (người: đọc tài liệu) | Đoạn mới ở `docs/risk-scoring.md` mục 4 có đủ L1 tới L10; câu mới ở `docs/auto-run.md` bước 6 như mục 3.1 |
| 3a, mẫu báo cáo có mục mới và dòng `- [ ]` | Test script có sẵn, kiểm tra thủ công (người: đọc tài liệu) | Test `docs/templates/run.md` có dòng `- [ ]` ở mục Cần người kiểm tra vẫn qua; người đọc mục "Quyết định AI đã tự làm" |

**Ngoại lệ viết test trước:** các đoạn chữ trong `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/templates/run.md`, `docs/lessons.md` và nội dung ADR-1005: là tài liệu, không có hành vi để test thất bại trước; kiểm bằng đọc (dòng kiểm thủ công ở trên) và bằng rà soát độc lập. `docs/foundation.md` được `foundation.py --check` kiểm.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-27, worktree `../wt-t067` trên nhánh chính `084d366`, code tạm không commit (L-075).
- **Lệnh:**
  - khối `playbook` của `scripts/tests/run.sh` (dòng 1 tới 26 cùng khối từ `# failure-kind.sh --playbook` tới trước `# is-exact-revert`), chạy trên bản sao của nhánh chính có test mới và `PB-13.log` (đúng commit test của bước 3), rồi trên bản cài thử;
  - toàn bộ `bash scripts/tests/run.sh` trên cả hai (JDK 21 qua `env64.sh` vì khối `design-compare` cần Java);
  - 10 test `--touches` chạy riêng bằng cùng `write_plan` của khối `plan-check` (cho các đột biến);
  - `python3 scripts/foundation.py --check`; `python3 scripts/secret-scan.py`; `git apply --check` của `diff` ở mục 3.1 trên `084d366`.
- **Kết quả:**
  - Nhánh chính, 18 test mới: 11 thất bại đúng lý do (ba test PB-13 `got 'khong-co'`; "mỗi tệp log có dòng" có PB-13 thừa; "ít nhất một dòng `bo-qua-loai`" `got 'no'`; sáu test `--touches` `got '1'`), 7 đối chứng qua (lý do từ chối khác; câu liệt kê có `that`; mọi loại có trong câu; bốn test `--touches` trả 1). Bản sao của nhánh chính: 743 ok; ngoài 11 test trên còn 6 test đỏ vì bản sao dựng bằng `git archive` không có kho git (`plan-queue`, `status page`, 4 test `env-canary`; môi trường của bản sao, không liên quan; trên worktree thật các test đó qua).
  - Bản cài thử: khối `playbook` 33 ok, `failures=0`; toàn bộ `scripts/tests/run.sh` 760 ok, "All script tests passed". `foundation.py --check`: "khớp" sau khi sinh lại. `secret-scan.py`: sạch. `git apply --check`: áp sạch.
  - Hai lỗi của chính khối test đã sửa khi chạy thử: lệnh `grep` không có dòng khớp làm cả `run.sh` dừng vì `set -euo pipefail` (thêm `|| true`); `sed` không có `-n ... p` làm đột biến 9 sống.
  - Đột biến trên bản cài thử, mỗi cái một lần, rồi hoàn tác:

    | # | Đột biến | Test bắt được |
    | --- | --- | --- |
    | 1 | Bỏ `bo-qua-loai` khỏi câu liệt kê loại | `every kind in the table is listed in the kinds sentence` |
    | 2 | Mẫu chỉ còn `an OAuth App` | `... GitHub App wording`, `... personal access token wording` |
    | 3 | Mẫu rộng thành `remote rejected` | `a push refused for another reason is not PB-13` |
    | 4 | Loại của PB-13 là `moi-truong` | 3 test PB-13 và `at least one bo-qua-loai row` |
    | 5 | Xóa `PB-13.log` | test PB-13 thật và `every row has a log file` |
    | 6 | Ô Việc phải làm của PB-13 không có lệnh `--touches` | `every bo-qua-loai row names the plan-check --touches command` |
    | 7 | `--touches` tìm tiền tố trong cả chữ của mục 3.2 | `--touches: workflows named only in prose -> 1` (ra 0) |
    | 8 | `--touches` bỏ qua tiền tố (có hàng nào là trả 0) | `--touches: workflows named only in prose -> 1` (ra 0) |
    | 10 | `startswith` thành `in` | `--touches: the prefix in the middle of a path -> 1` (ra 0) |
    | 11 | Bỏ hàng Xóa | `--touches: a Xóa row under the prefix -> 0` (ra 1) |
    | 12 | `file_list()` đọc bảng của cả plan, không chỉ mục 3.2 | `--touches: a table outside section 3.2 -> 1` (ra 0) |
    | 13 | Nhận tiền tố rỗng | `--touches: an empty prefix -> 2` (ra 0) |
    | 9 | Câu "Cột Loại là một trong:" đổi chữ | `the kinds sentence lists the kinds (that among them)`, `every kind ... listed` |
  - Code thử đã hoàn tác; nhánh plan chỉ có plan và ticket. Chưa chạy `scripts/check.sh` (không có code Android đổi; `scripts/tests/run.sh` là phần của `check.sh` mà plan chạm).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm chặn 7 (quy trình phiên tự chạy): đổi khi nào phiên dừng hay đi tiếp (`docs/auto-run.md`, `docs/risk-scoring.md` mục 4), thêm cách gọi mới của `scripts/plan-check.py` mà phiên dùng, và thêm một ADR về quyền tự quyết của phiên. Không sửa file trong mục "Không được làm" của `CLAUDE.md`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | 9 file tài liệu, script và test, một tệp log; không đổi code app |
| B. Độ rõ của yêu cầu | 1 | Tuấn chọn 2a, 2b, 3a; danh sách L1 tới L10 do AI đề xuất từ bằng chứng, người duyệt quyết (câu hỏi 1, 2) |
| C. Khả năng kiểm chứng tự động | 2 | Bảng tra lỗi và `--touches` có test và đột biến; phần lớn thay đổi là chữ của quy trình và ADR, chỉ kiểm bằng đọc và rà soát |
| D. Ảnh hưởng chéo | 2 | Mọi phiên sau đọc `auto-run.md`, `risk-scoring.md`; T-066, T-068 sửa cùng file (mục 6) |
| E. Độ mới của kỹ thuật | 0 | Cùng cách với PB-11, PB-12, ADR-0029; `--touches` dùng lại `file_list()` |
| **Tổng** | 6 | **Mức: Cao** (chạm điểm chặn 7; `risk_score` để trống theo quy tắc) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/ops/failure-playbook.md`, `docs/templates/run.md`, `docs/lessons.md` (L-086), `scripts/plan-check.py`, `scripts/tests/run.sh` (khối `playbook`, `plan-check`), `docs/foundation.md`.
- Task khác có thể xung đột:
  - T-066 (`open_with`, khả năng của cách mở phiên, thử đẩy ở đầu phiên; PR #192): dẫn dòng PB-13 và loại `bo-qua-loai` của plan này; sửa `docs/auto-run.md` mục 2, có thể `docs/templates/run.md` (phần đầu), `scripts/run-check.py`, và dòng Áp dụng của L-086 (T-067 thêm phần của mình vào cuối, mục 3.1). Khối khác nhau; T-066 nên chạy sau T-067. Khi lần thử đẩy ở đầu phiên của T-066 bị từ chối (mã 3) và bỏ qua các task sửa workflow, T-066 dùng `scripts/plan-check.py --touches .github/workflows/ <plan>` của plan này (không tìm theo chữ của mục 3.2), và ghi cả mục "Task bị bỏ qua" lẫn một dòng ADR-1005/L8 ở mục Quyết định AI đã tự làm.
  - T-068 (patch của nhánh không đẩy được vào nhánh báo cáo, `run-watchdog.yml`): sửa `docs/auto-run.md` mục Khi không đẩy được (bước 2 tới 4) và có thể thêm dòng bảng tra lỗi. Plan nào merge sau thì đặt nội dung của mình vào chỗ mới (ADR-1005/L3) và lấy mã PB kế tiếp (ADR-1005/L10).
  - T-065 (approved, chưa chạy): sửa `scripts/tests/run.sh` khối `setup-check`, không chạm khối `playbook` hay `plan-check`.
  - T-062 (approved, chưa chạy): không chung file.
- Đề xuất thứ tự chạy: T-067 đầu danh sách, trước T-062, T-065, T-066, T-068: các task sửa `.github/workflows/` sau đó, nếu lệnh đẩy vẫn bị từ chối, được bỏ qua đúng theo quy tắc mới thay vì làm phiên dừng.

## 7. Điều kiện dừng riêng

- Sau khi T-067 merge, đọc lại `docs/auto-run.md`, `docs/risk-scoring.md` và ADR-1005 trên nhánh chính rồi mới làm task sau; không dừng.
- Không có điều kiện dừng riêng khác.

## 8. Câu hỏi cho người duyệt

1. ADR-1005 vào `accepted` khi task merge (Tuấn là người quyết, ghi ở mục 9), hay `proposed` chờ anh Nhật? Với phiên tự chạy hai cách gần như như nhau: `scripts/foundation.py` đưa quy tắc của ADR `proposed` vào `docs/foundation.md` như ADR `accepted`, và `docs/CLAUDE.md` bảo phiên đọc trang đó, nên phiên sẽ làm theo ADR-1005 ở cả hai trạng thái. Khác nhau chỉ ở hồ sơ: `accepted` ghi rằng người đã quyết. Đề xuất của AI: `accepted`, để hồ sơ khớp với việc phiên đã làm theo; anh Nhật xem lại qua đề xuất `from-app`.
2. Danh sách L1 tới L10: có thêm hay bớt mục nào không? Đề xuất của AI: giữ mười mục. Cân nhắc nhưng không đưa vào: tự sửa test cũ ngoài mục 3.2 (đã có ADR-0029, quyết định 2), tự ghi lại ảnh chuẩn screenshot (cấm theo `CLAUDE.md`), tự đổi câu trả lời của người khi thấy sai (phải hỏi).
3. L1, mức trần 1600 giây của ngân sách kịch bản: đề xuất của AI: giữ 1600 (job `device-session` 50 phút, phần còn lại của lượt khoảng 20 phút); vượt thì nâng tới 1600 và ghi vào Cần người kiểm tra, không dừng.
4. L8 chỉ áp dụng cho lỗi khớp một dòng `bo-qua-loai` đã có trong bảng; lỗi mới chưa có dòng thì phiên vẫn làm như cũ (bước 6: kết thúc phiên khi task sau cũng không đẩy được). Đề xuất của AI: giữ như vậy; mỗi lỗi môi trường mới thành bài học rồi thành dòng bảng (`docs/auto-run.md` mục 6).
5. L3 chỉ khi mọi dòng `-` của plan có nguyên văn ở chỗ mới (khối chỉ có dòng `+`: dòng ngữ cảnh trên, dưới hay tiêu đề mục); không thì dừng theo điều kiện 3, không tự hòa hai thay đổi. Đề xuất của AI: đồng ý; đây là ranh giới để một task không đè thay đổi của task trước.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
