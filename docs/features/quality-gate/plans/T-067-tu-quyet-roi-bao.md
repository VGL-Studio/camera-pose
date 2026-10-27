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

- **2a. Lỗi môi trường chỉ chặn đúng loại task.** Bảng tra lỗi có loại mới `bo-qua-loai` và dòng PB-13 cho lệnh đẩy bị từ chối vì thiếu quyền `workflow`. `docs/auto-run.md` mục Khi không đẩy được tra bảng: lỗi loại này thì bỏ qua mọi task còn lại cùng loại, làm tiếp các task khác, không kết thúc phiên.
- **2b. Danh sách lệch nhỏ được tự làm.** ADR-1005 của app (bổ sung ADR-0029) liệt kê chín lệch nhỏ L1 tới L9 mà phiên tự làm không cần plan phiên bản mới, và những việc vẫn cấm.
- **3a. Báo sau ở một chỗ.** Báo cáo phiên có mục "Quyết định AI đã tự làm" (task, loại, quyết định, lý do, cách hoàn tác) và một dòng `- [ ]` ở Cần người kiểm tra.

4 test mới trong `scripts/tests/run.sh` đã chạy thật trên nhánh chính: 2 thất bại đúng lý do, 2 đối chứng qua; với bản cài thử cả 4 qua, toàn bộ `scripts/tests/run.sh` qua (746 ok); 5 đột biến đều bị bắt (mục 4.1).

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là đề xuất 2a, 2b, 3a của phiên chính sau RUN-20261027-01 (Tuấn chọn ngày 2026-10-27), theo hướng Tuấn nêu: "trao thêm quyền cho AI để tự ra quyết định" và "cứ triển khai trước rồi báo cáo con người sau, thay vì phải đợi quyết định từ con người".

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| 2a. Lỗi đẩy vì thiếu quyền `workflow` | Khi `scripts/failure-kind.sh --playbook` đọc thông báo thật của RUN-20261027-01 thì in `PB-13 bo-qua-loai: ...` | Thông báo của token OAuth App (`claude --cloud`) |
| 2a | Khi thông báo là cách viết của GitHub App ("... a GitHub App ... without `workflows` permission") thì cũng ra PB-13 | Phiên mở bằng routine hay app khác có thể dùng token của GitHub App; cùng một nghĩa |
| 2a | Khi lệnh đẩy bị từ chối vì lý do khác (ví dụ `protected branch hook declined`) thì không ra PB-13 | Không bỏ qua nhầm cả loại task |
| 2a | Khi một loại trong bảng không có trong câu liệt kê loại thì test đỏ | Loại mới phải được mô tả cho phiên đọc |
| 2a | Khi lỗi khớp dòng `bo-qua-loai`, phiên bỏ qua mọi task còn lại thuộc loại đó, làm tiếp task khác, không kết thúc phiên, soạn danh sách chạy nháp | `docs/auto-run.md` mục Khi không đẩy được, bước 5 mới; RUN-20261027-01 phiên đã làm đúng như vậy với T-065 nhưng không có quy tắc |
| 2b | ADR-1005 liệt kê L1 tới L9 và các việc cấm; `docs/foundation.md` có quy tắc của nó | Danh sách hẹp, rà soát kiểm được (mục 3.1) |
| 2b | `docs/risk-scoring.md` mục 4 có đoạn "Không phải dừng: lệch nhỏ đã liệt kê" | Phiên đọc điều kiện dừng ở đây trước tiên |
| 2b | Bước 6: phiên rà soát nhận danh sách lệch; lệch ngoài danh sách là mức chặn | Như phần ngoài mục 3.2 của ADR-0029 |
| 3a | Mẫu báo cáo có mục "Quyết định AI đã tự làm"; mục Cần người kiểm tra có dòng `- [ ] Xem lại N quyết định AI đã tự làm` | Dòng `- [ ]` đưa việc xem lại lên trang tiến độ qua `scripts/status.py` mà không đổi script |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0029 (phần ngoài mục 3.2, sửa điểm nên sửa), ADR-0025 (nhánh chính tự hồi phục), ADR-1004 (chưa áp dụng quảng cáo AdsKit). Bài học: camera-pose L-086 (thiếu quyền `workflow`, proposed), L-084, L-085, L-082, L-081, L-035.

**Nguồn của danh sách lệch L1 tới L9** (đọc mục "Lệch so với plan" của các plan đã chạy, báo cáo RUN-20261023-01 tới RUN-20261027-01, bài học L-062 tới L-086):

- L1: danh sách chạy RUN-20261027-01, câu hỏi 1 (1470 giây so với 1300); T-056, T-058 đã phải đưa việc nâng ngân sách vào plan.
- L2: RUN-20261027-01, dòng T-064 ("kịch bản giữ `need` vì T-062 dừng"); Tuấn cho đổi thành `wait` khi lập danh sách.
- L3: plan T-063 và T-065 phiên bản 2 (sau PR #179), chỉ vì mốc dời chỗ; nhật ký T-063 ("`token-usage.py` có 23 dòng thêm, plan ghi 22").
- L4: nhật ký T-055, T-057 ("thất bại ở bước 3, plan ghi sẽ qua"), T-044 (`beforeLoad_keepsBannerHeight_withSeparator`); L-035, L-082.
- L5: nhật ký T-022 (ktlint cấm file rỗng), T-047 (`AppNavHost` thành `internal`), T-061 (thân lambda dạng khối vì ktlint), T-064 (tách biến `selector` vì dòng dài).
- L6: các PR thêm ADR (foundation.py --check).
- L7: nhật ký T-036, T-037, T-039, T-053 ("`docs/lessons.md` không sửa, dòng Áp dụng đã ghi").
- L8: RUN-20261027-01 (T-062, T-065), L-086.
- L9: ADR-0029 quyết định 3; L-082.

Không đưa vào danh sách: ảnh so sánh `design/review/` (đã có quy tắc ở `docs/auto-run.md` bước 5, camera-pose L-085); dùng phương án dự phòng (đã có ở `risk-scoring.md` mục 4); phần ngoài mục 3.2 (ADR-0029). Ba việc này vẫn ghi vào mục Quyết định AI đã tự làm (quyết định 3 của ADR-1005).

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

- **L1. Ngân sách kịch bản.** Sau khi thêm hay bật tệp kịch bản của task, tổng theo công thức của `docs/ops/test-sessions.md` mục 5 (tổng `timeout:` của các tệp cộng 80 giây mỗi tệp) vượt `DEVICE_SCENARIOS_BUDGET`: nâng giá trị mặc định ở `scripts/device-scenario.py`, `scripts/device-session.sh` và `docs/ops/test-sessions.md` lên tổng đó làm tròn lên hàng trăm, tối đa 1600 giây (`device-session.yml` có `timeout-minutes: 50`), trong PR của task đó. Tổng vượt 1600: nâng tới 1600 và ghi vào Cần người kiểm tra.
- **L2. `need` thành `wait` khi môi trường đã có thiết bị.** Một dòng `need` trong tệp kịch bản chỉ để bỏ qua khi máy thiếu một thiết bị (camera, máy gập) mà emulator của CI đã có nhờ một task đã merge (ví dụ T-062): đổi thành `wait` cùng bộ chọn và thời gian, trong PR của task thêm thiết bị hay của task sau trong cùng phiên; chạy `scripts/device-scenario.py check` cho tệp đó.
- **L3. Mốc tài liệu và mã dời chỗ.** Dòng, mục, câu hay đoạn code mà plan dẫn đã dời vì task trước trong phiên hay vì đồng bộ dự án mẫu: đặt đúng nội dung của plan vào chỗ tương ứng mới; `diff` của plan áp với `git apply -3` hay bằng tay, các dòng thêm giữ nguyên nội dung; mã bài học dẫn theo quy tắc ở đầu `docs/CLAUDE.md`. Không đổi nội dung vì chỗ mới khác nghĩa: khi đó dừng theo điều kiện 3 của `risk-scoring.md` mục 4.
- **L4. Test viết trước thất bại khi plan ghi "qua ngay".** Test thất bại đúng lý do của tiêu chí: giữ test, ghi nhật ký. Test plan ghi sẽ thất bại lại qua ngay: thêm bước đối chứng theo L-035 (ghi rồi đọc lại, dữ liệu đúng cạnh dữ liệu sai) hay kiểm bằng đột biến (camera-pose L-082), không đổi điều test kiểm; không làm được thì xử lý như cũ.
- **L5. Chi tiết công cụ bắt buộc.** ktlint, Android Lint hay trình biên dịch buộc đổi cách viết mà không đổi hành vi hay điều test kiểm: tách biến hay hàm, đổi `public` thành `internal`, hằng thay cho file rỗng, đổi dạng khối của lambda, ngắt dòng.
- **L6. Tệp sinh ra.** Chạy lại script sinh tệp khi test của script đòi tệp khớp (`scripts/foundation.py` cho `docs/foundation.md`) và commit kết quả. `design/review/` theo `docs/auto-run.md` bước 5 (camera-pose L-085), không theo mục này.
- **L7. Sổ bài học đã ghi.** Plan bảo sửa dòng Áp dụng của một bài học mà dòng đó đã ghi đúng: không sửa, ghi nhật ký.
- **L8. Bỏ qua loại task mà môi trường không làm được.** Một lỗi khớp dòng loại `bo-qua-loai` của `docs/ops/failure-playbook.md` (ví dụ PB-13: lệnh đẩy bị từ chối vì thiếu quyền `workflow`): giữ task đang làm theo `docs/auto-run.md` mục Khi không đẩy được, bỏ qua mọi task còn lại của danh sách thuộc loại mà dòng đó nêu (theo mục 3.2 của plan), làm tiếp các task khác, không kết thúc phiên. Soạn danh sách chạy nháp cho các task bị bỏ qua. Task phụ thuộc task bị bỏ qua cũng bị bỏ qua như thường.
- **L9. Điểm nên sửa của rà soát:** theo ADR-0029, quyết định 3 và `docs/auto-run.md` bước 6 (gồm đột biến của camera-pose L-082); mục này chỉ nhắc để mọi lệch có một chỗ ghi chung.

**2. Phiên không được tự làm, dù lệch nhỏ.** Các việc sau vẫn theo điều kiện dừng của `risk-scoring.md` mục 4 và `CLAUDE.md`:

- thêm quyền, thư viện hay SDK; thêm hay đổi vị trí quảng cáo, sự kiện phân tích, dữ liệu gửi đi;
- chạm cấu hình production, `scripts/release.sh`, `app/src/staging/google-services.json`;
- sửa file trong mục "Không được làm" của `CLAUDE.md` mà plan chưa nêu (`.github/`, `scripts/check.sh`, `scripts/merge-if-green.sh`, `build-logic/`, `gradle/libs.versions.toml`);
- đổi spec, thiết kế đã duyệt hay câu trả lời của người ở mục 9 của plan;
- làm yếu test: xóa test hay câu khẳng định, nới phép so sánh, thêm `@Ignore`, đổi điều test kiểm, ghi lại ảnh chuẩn screenshot ngoài plan;
- đóng, sửa hay bình luận issue `main-broken`; merge bằng cách khác `scripts/merge-if-green.sh`;
- đẩy lại một lệnh bị từ chối dưới dạng khác (đổi tên nhánh, gọi API tạo ref, giao cho phiên khác).

**3. Báo sau.** Báo cáo phiên có mục "Quyết định AI đã tự làm" (mẫu `docs/templates/run.md`), mỗi dòng một quyết định: task, loại (L1 tới L9, ADR-0029, phương án dự phòng, bỏ qua task), quyết định, lý do, cách hoàn tác. Mục Cần người kiểm tra có một dòng `- [ ]` "Xem lại N quyết định AI đã tự làm" khi N lớn hơn 0, để trang tiến độ (`scripts/status.py`) hiện việc đó cho người. Nhật ký plan ghi lệch ở dòng "Lệch so với plan" như cũ, kèm mã L1 tới L9.

**4. Rà soát độc lập kiểm lệch.** Ở bước 6, phiên rà soát nhận danh sách lệch L1 tới L7 của task và xét mỗi lệch có đúng mục của quyết định 1 không. Lệch ngoài danh sách hay vi phạm quyết định 2 là mức chặn.

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
- **Việc AI phải tuân thủ:** chỉ tự làm các lệch nhỏ L1 tới L9 của quyết định 1 mà không cần plan phiên bản mới; ngân sách kịch bản nâng tới tổng theo công thức làm tròn lên hàng trăm, tối đa 1600 giây, trong PR của task làm tổng vượt; `need` chỉ đổi thành `wait` khi emulator đã có thiết bị nhờ task đã merge; mốc dời chỗ thì đặt đúng nội dung của plan vào chỗ mới, chỗ mới khác nghĩa thì dừng theo điều kiện 3; lỗi khớp dòng `bo-qua-loai` của bảng tra lỗi thì bỏ qua mọi task còn lại thuộc loại đó, làm tiếp các task khác, không kết thúc phiên, soạn danh sách chạy nháp cho task bị bỏ qua; không tự thêm quyền, thư viện, SDK, quảng cáo, sự kiện phân tích, không chạm production, không đổi spec, thiết kế hay câu trả lời của người, không làm yếu test, không đẩy lại lệnh bị từ chối dưới dạng khác; mỗi quyết định tự làm ghi vào mục "Quyết định AI đã tự làm" của báo cáo (task, loại, quyết định, lý do, cách hoàn tác) và một dòng `- [ ]` ở mục Cần người kiểm tra; rà soát độc lập xét mỗi lệch, lệch ngoài danh sách là mức chặn.
```

Sau khi tạo ADR: `scripts/foundation.py` rồi commit `docs/foundation.md` (bản thử thêm 11 dòng của ADR-1005; `scripts/foundation.py --check` qua).

**Các file còn lại**, `diff` của bản cài thử trên nhánh chính `084d366` (áp sạch bằng `git apply`):

```diff
diff --git a/docs/auto-run.md b/docs/auto-run.md
index ae63813..c33aeda 100644
--- a/docs/auto-run.md
+++ b/docs/auto-run.md
@@ -124,7 +124,7 @@ Task lớn (nhiều file, cỡ L) thì phiên chính giao bước 3 đến 5 cho
 
 ### Bước 6. Rà soát độc lập
 
-Một phiên AI riêng được khởi tạo để rà soát. Phiên này chỉ nhận plan, spec, thiết kế, ADR và phần thay đổi; không nhận quá trình làm việc của phiên viết code, để đánh giá với con mắt mới. Có phần ngoài mục 3.2 (ADR-0029) thì phiên rà soát nhận thêm danh sách file đó, lý do của từng file và `git diff --stat origin/main...HEAD -- <các file đó>`.
+Một phiên AI riêng được khởi tạo để rà soát. Phiên này chỉ nhận plan, spec, thiết kế, ADR và phần thay đổi; không nhận quá trình làm việc của phiên viết code, để đánh giá với con mắt mới. Có phần ngoài mục 3.2 (ADR-0029) thì phiên rà soát nhận thêm danh sách file đó, lý do của từng file và `git diff --stat origin/main...HEAD -- <các file đó>`. Có lệch nhỏ L1 tới L7 (camera-pose ADR-1005) thì phiên rà soát nhận danh sách lệch kèm mã; lệch ngoài danh sách của ADR-1005 hay vi phạm quyết định 2 của nó là mức chặn.
 
 Nội dung rà soát:
 
@@ -172,7 +172,8 @@ Lệnh đẩy của phiên chính bị môi trường từ chối (bộ phân lo
 2. Chạy `scripts/save-branch.sh --bundle-only <nhánh>` (nếu chưa có bundle). Bundle nằm ở `$HOME/rescue/`, ngoài worktree. Phần chưa commit thì commit trước (L-069).
 3. Dừng task theo `risk-scoring.md` mục 4. Plan và ticket giữ `approved`, vì không phải lỗi của plan. Nhật ký plan ghi mã commit.
 4. Chép đoạn "Nhánh chưa đẩy" mà script in ra, cùng nguyên văn lệnh bị từ chối và thông báo, vào báo cáo phiên.
-5. Làm tiếp task sau. Lần đẩy nhánh của task sau (bước 1) cũng bị từ chối thì kết thúc phiên (mục 4), vì mọi task đều cần đẩy.
+5. Tra thông báo bị từ chối trong bảng tra lỗi: ghi nguyên văn vào một tệp tạm (`${TMPDIR:-/tmp}/push-refused.log`) rồi chạy `scripts/failure-kind.sh --playbook` với tệp đó. Dòng loại `bo-qua-loai` (ví dụ PB-13, thiếu quyền `workflow`): bỏ qua mọi task còn lại của danh sách thuộc loại mà dòng đó nêu, theo mục 3.2 của plan (`sed -n '/^### 3.2/,/^### 3.3/p' <plan> | grep -q '<loại>'`), làm tiếp các task khác, soạn danh sách chạy nháp cho các task bị bỏ qua, ghi vào mục Quyết định AI đã tự làm (camera-pose ADR-1005, L8). Không kết thúc phiên vì lỗi loại này.
+6. Làm tiếp task sau. Lần đẩy nhánh của task sau (bước 1) cũng bị từ chối, mà thông báo không khớp dòng `bo-qua-loai` nào, thì kết thúc phiên (mục 4), vì mọi task đều cần đẩy.
 
 ### Bước 8. CI đầy đủ
 
@@ -352,6 +353,7 @@ Báo cáo phiên là thứ dev đọc đầu tiên khi bắt đầu làm việc
 
 - Nhánh chưa đẩy, ngay dưới Tóm tắt khi có bundle: chép đoạn `scripts/save-branch.sh` in ra.
 - Kết quả từng task: đã merge, bị dừng, bị bỏ qua, kèm lý do.
+- Quyết định AI đã tự làm: mỗi lệch nhỏ L1 tới L9, phần ngoài mục 3.2 (ADR-0029), lần chuyển sang phương án dự phòng, task bỏ qua vì lỗi môi trường; mỗi dòng có task, loại, quyết định, lý do, cách hoàn tác (camera-pose ADR-1005). Có ít nhất một dòng thì thêm `- [ ] Xem lại N quyết định AI đã tự làm` vào mục Cần người kiểm tra.
 - Những gì người cần tự kiểm tra: ảnh chuẩn screenshot mới, tiêu chí kiểm tra thủ công máy thật và người, kịch bản emulator lỗi hay bị bỏ qua, điểm rà soát mức ghi chú.
 - Task bị dừng: dừng ở bước nào, vì sao, đề xuất hướng xử lý.
 - Thông tin bản staging: phiên bản, kênh phát.
diff --git a/docs/ops/failure-playbook.md b/docs/ops/failure-playbook.md
index 6672b1c..9b728d6 100644
--- a/docs/ops/failure-playbook.md
+++ b/docs/ops/failure-playbook.md
@@ -4,7 +4,7 @@ Bảng này gom các lỗi đã gặp thật của phiên tự chạy: thông b
 
 Cách thêm một dòng: mỗi bài học loại `môi trường` có thông báo lỗi dò được thì thêm một dòng ở cuối, mã `PB-NN` kế tiếp (không dùng lại mã đã xóa), và một tệp `scripts/tests/playbook/PB-NN.log` chép đoạn log thật. Test báo lỗi khi dòng nào thiếu tệp log. Thứ tự dòng là thứ tự dò: dòng cụ thể đặt trước dòng chung. App tạo từ dự án mẫu thêm dòng cho lỗi riêng của mình theo cùng cách, ví dụ thẻ log của màn tải riêng.
 
-Cột Mẫu là biểu thức chính quy kiểu `grep -E` trong dấu backtick; dấu `|` trong mẫu viết `\|` vì Markdown. Cột Loại là một trong: `chay-lai`, `lich-thu-lai`, `cho`, `gop-nhanh`, `kiem-da-merge`, `moi-truong`, `nguoi`, `that`. Các loại `chay-lai`, `lich-thu-lai`, `cho`, `gop-nhanh`, `kiem-da-merge` không tính vào 3 lần sửa.
+Cột Mẫu là biểu thức chính quy kiểu `grep -E` trong dấu backtick; dấu `|` trong mẫu viết `\|` vì Markdown. Cột Loại là một trong: `chay-lai`, `lich-thu-lai`, `cho`, `gop-nhanh`, `kiem-da-merge`, `bo-qua-loai`, `moi-truong`, `nguoi`, `that`. Các loại `chay-lai`, `lich-thu-lai`, `cho`, `gop-nhanh`, `kiem-da-merge`, `bo-qua-loai` không tính vào 3 lần sửa. Loại `bo-qua-loai` (camera-pose ADR-1005): môi trường của phiên không làm được một loại việc; phiên giữ task đang làm theo `auto-run.md` mục Khi không đẩy được, bỏ qua mọi task còn lại của danh sách thuộc loại đó (cột Việc phải làm nói loại nào, theo mục 3.2 của plan) và làm tiếp các task khác, không kết thúc phiên.
 
 <!-- playbook:start -->
 | Mã | Mẫu | Thấy ở | Loại | Việc phải làm | Nguồn |
@@ -21,4 +21,5 @@ Cột Mẫu là biểu thức chính quy kiểu `grep -E` trong dấu backtick;
 | PB-09 | `Could not (GET\|HEAD\|resolve\|download) .*(status code (429\|5[0-9][0-9])\|timed out)\|Read timed out\|Connect timed out\|Connection reset` | `check.sh` (Gradle tải thư viện) | `lich-thu-lai` | Lỗi tải thư viện. Chạy lại theo lịch 1, 2, 4, 8, 15 phút của bước 5, không tính vào 3 lần sửa. Hết lịch thì kết thúc phiên (mục 4) | `auto-run.md` bước 5 |
 | PB-10 | `ComposeTimeoutException: Condition still not satisfied` | job `ui-test` | `that` | Test trên emulator chờ hết giờ. Tra PB-01 trước (dòng trên khớp thì là R2). Không khớp: lỗi thật, đọc bước "Chi tiết test trên emulator" (bước 8) | L-079 |
 | PB-11 | `oauth_org_not_allowed\|organization has disabled Claude subscription access` | kết quả của phiên con (lỗi API 403) | `cho` | Tài khoản Claude của tổ chức tạm bị chặn, không phải lỗi của code. Phiên con chết thì làm theo `auto-run.md`, mục Giao bước 3 đến 5 (phiên con kết thúc mà không có báo cáo): tiếp từ phần đã có trong worktree. Lỗi ở chính phiên chính thì phiên dừng; người mở lại theo mục 4, Tiếp tục phiên sau khi hết hạn mức, và báo người quản trị tổ chức. Không tính vào 3 lần sửa | camera-pose L-084, RUN-20261026-03 |
+| PB-13 | `refusing to allow an? (OAuth App\|GitHub App) to create or update workflow` | lệnh đẩy (`scripts/save-branch.sh`, `git push`) | `bo-qua-loai` | Token đẩy của phiên không có quyền sửa file workflow. Không đẩy lại dưới dạng khác. Làm theo `auto-run.md` mục Khi không đẩy được cho task này; bỏ qua mọi task còn lại của danh sách có `.github/workflows/` trong mục 3.2 của plan, làm tiếp các task khác; ghi vào mục Quyết định AI đã tự làm và soạn danh sách chạy nháp cho các task bị bỏ qua (camera-pose ADR-1005, quyết định 1, L8). Người cấp quyền `workflow` hay chạy các task đó bằng cách mở phiên đẩy được workflow | camera-pose L-086, RUN-20261027-01 |
 <!-- playbook:end -->
diff --git a/docs/risk-scoring.md b/docs/risk-scoring.md
index bfe35a0..7b0eb3e 100644
--- a/docs/risk-scoring.md
+++ b/docs/risk-scoring.md
@@ -65,6 +65,8 @@ Trong lúc thực hiện plan đã duyệt, AI **phải dừng** task, không t
 - Điều kiện dừng riêng ở mục 7 của plan vẫn áp dụng.
 - Ghi vào nhật ký plan (file, vì sao cần, loại), mô tả PR (mục "Ngoài mục 3.2", kèm `git diff --stat origin/main...HEAD -- <các file đó>`) và báo cáo phiên (một dòng `- [ ]` ở mục Cần người kiểm tra). Rà soát độc lập xét riêng phần này; vi phạm ADR-0029 là mức chặn (`auto-run.md`, bước 6).
 
+**Không phải dừng: lệch nhỏ đã liệt kê (camera-pose ADR-1005).** Ngân sách kịch bản (L1), `need` thành `wait` khi emulator đã có thiết bị (L2), mốc tài liệu và mã dời chỗ (L3), test viết trước thất bại khi plan ghi qua ngay (L4), chi tiết do ktlint, lint hay trình biên dịch buộc (L5), tệp sinh ra (L6), dòng Áp dụng đã ghi (L7), bỏ qua loại task mà môi trường không làm được theo dòng `bo-qua-loai` của bảng tra lỗi (L8). Ghi vào mục Quyết định AI đã tự làm của báo cáo. Không bao giờ là lý do để vượt điều kiện 1, 2, 3, 5, 7, 8 hay 9.
+
 **Không phải dừng: chuyển sang phương án dự phòng đã duyệt.** Plan có mục 3.4 (Phương án dự phòng) đã được duyệt cùng plan. Khi phương án chính gặp đúng điều kiện chuyển ghi trong mục đó, hoặc gặp điều kiện 4 ở trên, AI chuyển sang phương án dự phòng thay vì dừng: chỉ chạm file của mục 3.2 (cộng phần ngoài mục 3.2 theo ADR-0029, như với phương án chính), giữ nguyên test viết trước và tiêu chí chấp nhận, đếm lại giới hạn 3 lần sửa từ đầu, ghi vào nhật ký lúc chuyển và lý do. Chỉ chuyển một lần; phương án dự phòng cũng không qua thì dừng như bình thường. Phương án dự phòng không bao giờ là lý do để vượt điều kiện 1, 2, 3, 5, 7, 8 hay 9.
 
 Khi dừng, AI làm ba việc:
diff --git a/docs/templates/run.md b/docs/templates/run.md
index 47b31f8..234caf4 100644
--- a/docs/templates/run.md
+++ b/docs/templates/run.md
@@ -64,6 +64,14 @@ Task bị bỏ qua: <mã ticket và lý do, hoặc "Không có">
 | T-NNN | Đã merge | <liên kết> | 8 test, đã thất bại đúng lý do | 0 chặn, 1 ghi chú | Xanh | |
 | T-NNN | Dừng ở bước 8 | <liên kết> | | | Đỏ sau 3 lần sửa | Xem nhật ký plan |
 
+### Quyết định AI đã tự làm
+
+<!-- auto-run.md, mục 5; camera-pose ADR-1005. Lệch nhỏ L1 tới L9, phần ngoài mục 3.2 (ADR-0029), phương án dự phòng, task bỏ qua vì lỗi môi trường. Ghi "Không có" nếu không có. -->
+
+| Task | Loại | Quyết định | Lý do | Cách hoàn tác |
+| --- | --- | --- | --- | --- |
+| T-NNN | L1 | <ví dụ: `DEVICE_SCENARIOS_BUDGET` 1300 thành 1500> | <tổng theo công thức 1470 giây> | <đặt lại 1300 ở ba file, PR mới> |
+
 ### Cần người kiểm tra
 
 - [ ] Ảnh chuẩn screenshot mới: <màn hình, đường dẫn ảnh>
@@ -71,6 +79,7 @@ Task bị bỏ qua: <mã ticket và lý do, hoặc "Không có">
 - [ ] Tiêu chí kiểm tra thủ công (máy thật, người): <ticket, tiêu chí, các bước>
 - [ ] Kịch bản emulator lỗi hay bỏ qua: <tệp, kết quả, bước lỗi, phản hồi FB-...>, hoặc "Không có"
 - [ ] Phần ngoài mục 3.2 đã merge (ADR-0029): <ticket, file, vì sao cần, PR>, hoặc "Không có"
+- [ ] Xem lại N quyết định AI đã tự làm (mục trên), hoặc "Không có"
 - [ ] Điểm rà soát mức ghi chú: <ticket, nội dung>
 - [ ] Test chập chờn: <issue `flaky` đang mở, tên test>, hoặc "Không có" (ADR-0025)
 - [ ] Task bị revert: <ticket, issue `main-broken`, PR revert>, hoặc "Không có" (ADR-0025)
diff --git a/scripts/tests/playbook/PB-13.log b/scripts/tests/playbook/PB-13.log
new file mode 100644
index 0000000..96429ba
--- /dev/null
+++ b/scripts/tests/playbook/PB-13.log
@@ -0,0 +1 @@
+ ! [remote rejected] claude/T-062-camera-gia -> claude/T-062-camera-gia (refusing to allow an OAuth App to create or update workflow `.github/workflows/device-session.yml` without `workflow` scope)
diff --git a/scripts/tests/run.sh b/scripts/tests/run.sh
index 910d4dd..c062927 100755
--- a/scripts/tests/run.sh
+++ b/scripts/tests/run.sh
@@ -528,6 +528,11 @@ expect "playbook: PB-09 library download 429 -> lich-thu-lai" "PB-09 lich-thu-la
 expect "playbook: PB-10 a UI timeout without R2 lines is a real failure" "PB-10 that" "$(pb_head $pb_dir/PB-10.log)"
 expect "playbook: PB-11 Claude org access 403 in a subagent -> cho" "PB-11 cho" "$(pb_head $pb_dir/PB-11.log)"
 expect "playbook: PB-12 an R2 file missing (404) in ui-test -> nguoi, before the rerun of PB-01" "PB-12 nguoi" "$(pb_head $pb_dir/PB-12.log)"
+expect "playbook: PB-13 push refused for a workflow file (OAuth App, workflow scope) -> bo-qua-loai" "PB-13 bo-qua-loai" "$(pb_head $pb_dir/PB-13.log)"
+printf '%s\n' ' ! [remote rejected] claude/T-1 -> claude/T-1 (refusing to allow a GitHub App to create or update workflow `.github/workflows/ci.yml` without `workflows` permission)' > "$work/pb13-app.log"
+expect "playbook: PB-13 also matches the GitHub App wording (workflows permission)" "PB-13 bo-qua-loai" "$(pb_head "$work/pb13-app.log")"
+printf '%s\n' ' ! [remote rejected] claude/T-1 -> claude/T-1 (protected branch hook declined)' > "$work/pb13-other.log"
+expect "playbook: a push refused for another reason is not PB-13" "khong-co" "$(pb_head "$work/pb13-other.log")"
 expect "playbook: an unknown failure says khong-co" "khong-co" "$(pb_head $pb_dir/none.log)"
 expect "playbook: a missing log says khong-co" "khong-co" "$(pb_head "$work/no-such.log")"
 expect "playbook: a missing log exits 0" 0 "$(scripts/failure-kind.sh --playbook "$work/no-such.log" > /dev/null 2>&1; echo $?)"
@@ -560,6 +565,9 @@ expect "playbook: every pattern of the real table is valid" 0 "$(grep -c 'LỖI
 pb_rows="$(sed -n '/playbook:start/,/playbook:end/p' docs/ops/failure-playbook.md | grep -oE '^\| PB-[0-9]+' | tr -d '| ' | sort)"
 pb_files="$(ls $pb_dir | grep -oE '^PB-[0-9]+' | sort -u)"
 expect "playbook: every row has a log file and every log file a row" "$pb_rows" "$pb_files"
+pb_kinds="$(sed -n '/playbook:start/,/playbook:end/p' docs/ops/failure-playbook.md | grep -E '^\| PB-[0-9]+ ' | awk -F' \\| ' '{ print $4 }' | tr -d '`' | sort -u)"
+pb_known="$(grep -m1 '^Cột Mẫu là' docs/ops/failure-playbook.md | sed -E 's/.*Cột Loại là một trong: ([^.]*)\..*/\1/' | grep -oE '`[a-z-]+`' | tr -d '`' | sort -u)"
+expect "playbook: every kind in the table is listed in the kinds sentence" "" "$(comm -23 <(printf '%s\n' "$pb_kinds") <(printf '%s\n' "$pb_known"))"
 expect "playbook: the table has at least 10 rows" yes "$([ "$(printf '%s\n' "$pb_rows" | wc -l)" -ge 10 ] && echo yes || echo no)"
 expect "failure-kind: a playbook-only log still says other" other "$(scripts/failure-kind.sh $pb_dir/PB-01.log)"
 
```

Ghi chú về `diff`:

- Thêm vào `diff` trên (cùng file `docs/auto-run.md`, mục 4, bảng lý do kết thúc phiên), dòng

  `| Lệnh đẩy nhánh bị từ chối ở hai task liên tiếp | Đẩy nhánh báo cáo nếu được; báo cáo ghi các bundle (bước 7, "Khi không đẩy được") |`

  đổi thành

  `| Lệnh đẩy nhánh bị từ chối ở hai task liên tiếp, mà thông báo không khớp dòng \`bo-qua-loai\` nào của bảng tra lỗi (camera-pose ADR-1005, L8) | Đẩy nhánh báo cáo nếu được; báo cáo ghi các bundle (bước 7, "Khi không đẩy được") |`

  để bảng kết thúc phiên khớp bước 6 mới. Dòng này chưa có trong bản cài thử; nó chỉ là chữ, không đổi test nào.
- `docs/auto-run.md`, `docs/ops/failure-playbook.md`, `docs/risk-scoring.md`, `docs/templates/run.md` là file lấy từ dự án mẫu (PR #179). Câu mới ghi "camera-pose ADR-1005" theo quy tắc ở đầu `docs/CLAUDE.md`. Đề xuất về dự án mẫu (`docs/ops/from-app.md`): loại `bo-qua-loai`, dòng PB-13, bước 5 mới của mục Khi không đẩy được, mục "Quyết định AI đã tự làm" của mẫu báo cáo, và danh sách lệch L1 tới L9 như một ADR của mẫu. Phiên tự chạy không mở issue ở repo khác: ghi vào mục Cần người kiểm tra của báo cáo phiên để người mở issue `from-app`.
- Dòng PB-13 đặt cuối bảng: mẫu `refusing to allow an? (OAuth App|GitHub App) to create or update workflow` không trùng dòng nào khác (đối chứng ở mục 4).
- `scripts/status.py` không đổi: dòng `- [ ] Xem lại N quyết định AI đã tự làm` nằm trong mục Cần người kiểm tra, nên trang tiến độ đã hiện nó (`status.py` đọc mọi dòng `- [ ]` của mục đó trong báo cáo `finished`).
- Thứ tự commit (L-025): bước 3 commit test (`scripts/tests/run.sh`, khối trên); bước 4 commit code: `docs/ops/failure-playbook.md`, `scripts/tests/playbook/PB-13.log`, ADR-1005, `docs/foundation.md`, `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/templates/run.md`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `docs/adr/1005-lech-nho-tu-lam-roi-bao.md` | ADR mới của app, nội dung ở mục 3.1 |
| Sửa | `docs/foundation.md` | Sinh lại bằng `scripts/foundation.py` (thêm mục ADR-1005) |
| Sửa | `docs/ops/failure-playbook.md` | Loại `bo-qua-loai` trong câu liệt kê loại; dòng PB-13 |
| Tạo | `scripts/tests/playbook/PB-13.log` | Một dòng thông báo thật của RUN-20261027-01 |
| Sửa | `scripts/tests/run.sh` | 4 test trong khối `playbook` |
| Sửa | `docs/auto-run.md` | Bước 6 (rà soát xét lệch), mục Khi không đẩy được (bước 5 mới, bước 6), mục 4 (dòng "Lệnh đẩy nhánh bị từ chối ở hai task liên tiếp" của bảng kết thúc phiên), mục 5 Báo cáo phiên |
| Sửa | `docs/risk-scoring.md` | Mục 4: đoạn "Không phải dừng: lệch nhỏ đã liệt kê (camera-pose ADR-1005)" |
| Sửa | `docs/templates/run.md` | Mục "Quyết định AI đã tự làm"; dòng `- [ ] Xem lại N quyết định AI đã tự làm` |
| Sửa | `docs/lessons.md` | L-086: dòng Áp dụng ghi phần T-067 làm (PB-13, mục Khi không đẩy được); trạng thái giữ như retro quyết, không tự đổi |

Nơi đọc các file bị đổi (L-020): `grep -rn 'failure-playbook\|templates/run.md\|risk-scoring.md\|foundation.md' scripts/ .github/ */src/test`:

- `scripts/failure-kind.sh` đọc bảng tra lỗi theo dấu `<!-- playbook:start -->` tới `end` và 6 cột; dòng mới giữ đúng 6 cột, loại mới là chuỗi tự do (script không kiểm tên loại).
- `scripts/tests/run.sh`: test của bảng (mỗi dòng có tệp log: có `PB-13.log`; ít nhất 10 dòng; mẫu hợp lệ) và test `docs/templates/run.md` có dòng `- [ ]` ở mục Cần người kiểm tra (dòng 1876, vẫn có).
- `scripts/status.py` đọc mục Cần người kiểm tra của báo cáo `finished` (mục mới đứng trước, không xen vào mục đó); `scripts/run-check.py` và `scripts/plan-check.py` không đọc `docs/templates/run.md`.
- `scripts/foundation.py --check` chạy trong `scripts/tests/run.sh` (dòng 941, repo app mới tạo từ mẫu): ADR mới của app không ảnh hưởng test đó vì nó dựng repo riêng.
- `docs/auto-run.md` chỉ được nhắc trong chú thích hay thông báo (dẫn mục, bước) của `.github/workflows/pr-check.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/main-guard-status.sh`, `scripts/resume-check.py`, `scripts/wait-run.sh`. Plan không đổi tên hay số của mục, bước nào mà các file đó dẫn (mục 2, 3, 4; bước 5, 8; mục Tiếp tục phiên): chỉ thêm bước 5 vào mục Khi không đẩy được (bước cũ thành 6) và thêm câu vào bước 6, mục 5. Không file nào trong số đó dẫn "Khi không đẩy được, bước 5".
- `docs/foundation.md` và `scripts/tests/run.sh` được `.github/workflows/pr-check.yml` dùng qua lệnh `scripts/tests/run.sh` (job kiểm script và tài liệu): plan chỉ thêm test và sinh lại trang bằng script, không đổi cách gọi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có. Thay đổi chỉ là tài liệu, một dòng bảng tra lỗi, một tệp log và 4 test bash đã chạy thật (mục 4.1).

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| 2a, thông báo thật của RUN-20261027-01 ra PB-13 | Test script (`scripts/tests/run.sh`) | `playbook: PB-13 push refused for a workflow file (OAuth App, workflow scope) -> bo-qua-loai`. Trên nhánh chính: `khong-co` (thất bại đúng lý do: chưa có dòng) |
| 2a, cách viết của GitHub App ra PB-13 | Test script | `playbook: PB-13 also matches the GitHub App wording (workflows permission)`, tệp tạm `$work/pb13-app.log`. Trên nhánh chính: `khong-co` (thất bại đúng lý do) |
| 2a, từ chối vì lý do khác không ra PB-13 | Test script, đối chứng | `playbook: a push refused for another reason is not PB-13` (`protected branch hook declined`). Qua cả trước và sau; bắt đột biến "mẫu quá rộng" (mục 4.1, đột biến 3) |
| 2a, mọi loại trong bảng có trong câu liệt kê loại | Test script, đối chứng | `playbook: every kind in the table is listed in the kinds sentence`. Qua trên nhánh chính (8 loại khớp 8 loại); bắt đột biến "thêm dòng `bo-qua-loai` mà quên câu liệt kê" (đột biến 1) |
| 2a, mỗi dòng có tệp log | Test script có sẵn | `playbook: every row has a log file and every log file a row` (bắt đột biến 5) |
| 2a, phiên bỏ qua đúng loại và làm tiếp | Kiểm tra thủ công (người: đọc tài liệu) | `docs/auto-run.md` mục Khi không đẩy được có bước 5 mới như mục 3.1; ở phiên đầu tiên gặp PB-13, báo cáo có dòng L8 ở mục Quyết định AI đã tự làm |
| 2b, ADR-1005 vào trang quy tắc | Test script có sẵn | `scripts/foundation.py --check` (trong `scripts/check.sh` qua `scripts/tests/run.sh`, và kiểm riêng); thiếu `docs/foundation.md` sinh lại thì báo "chưa khớp" (đã thấy ở bản thử trước khi sinh lại) |
| 2b, `risk-scoring.md` và bước 6 dẫn ADR-1005 | Kiểm tra thủ công (người: đọc tài liệu) | Đoạn mới ở `docs/risk-scoring.md` mục 4 và câu mới ở `docs/auto-run.md` bước 6 như mục 3.1 |
| 3a, mẫu báo cáo có mục mới và dòng `- [ ]` | Test script có sẵn, kiểm tra thủ công (người: đọc tài liệu) | Test `docs/templates/run.md` có dòng `- [ ]` ở mục Cần người kiểm tra vẫn qua; người đọc mục "Quyết định AI đã tự làm" |

**Ngoại lệ viết test trước:** các đoạn chữ trong `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/templates/run.md` và nội dung ADR-1005: là tài liệu, không có hành vi để test thất bại trước; kiểm bằng đọc (dòng kiểm thủ công ở trên) và bằng rà soát độc lập. `docs/foundation.md` được `foundation.py --check` kiểm.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-27, worktree `../wt-t067` trên nhánh chính `084d366`, khối test tạm không commit (L-075). Chạy khối `playbook` của `scripts/tests/run.sh` (dòng 1 tới 26 cùng khối từ `# failure-kind.sh --playbook` tới trước `# is-exact-revert`).
- **Lệnh:** khối `playbook` như trên; `bash scripts/tests/run.sh` (toàn bộ, JDK 21 qua `env64.sh` vì khối `design-compare` cần Java); `python3 scripts/foundation.py --check`; `python3 scripts/secret-scan.py`.
- **Kết quả:**
  - Code hiện tại, 4 test mới: 2 thất bại đúng lý do (`expected 'PB-13 bo-qua-loai', got 'khong-co'` cho tệp thật và cách viết GitHub App), 2 đối chứng qua; khối có 27 ok, `failures=2`.
  - Bản cài thử (mục 3.1): khối 29 ok, `failures=0`. Toàn bộ `scripts/tests/run.sh`: 746 ok, "All script tests passed". `foundation.py --check`: trước khi sinh lại báo "chưa khớp", sau khi sinh lại "khớp". `secret-scan.py`: sạch.
  - Đột biến trên bản cài thử, mỗi cái một lần, rồi hoàn tác:

    | # | Đột biến | Test bắt được |
    | --- | --- | --- |
    | 1 | Bỏ `bo-qua-loai` khỏi câu liệt kê loại | `every kind in the table is listed in the kinds sentence` |
    | 2 | Mẫu chỉ còn `OAuth App` | `PB-13 also matches the GitHub App wording` |
    | 3 | Mẫu rộng thành `remote rejected` | `a push refused for another reason is not PB-13` |
    | 4 | Loại của PB-13 là `moi-truong` | 2 test PB-13 |
    | 5 | Xóa `PB-13.log` | test PB-13 thật và `every row has a log file` |
  - Code thử đã hoàn tác; nhánh plan chỉ có plan và ticket.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm chặn 7 (quy trình phiên tự chạy): đổi khi nào phiên dừng hay đi tiếp (`docs/auto-run.md`, `docs/risk-scoring.md` mục 4) và thêm một ADR về quyền tự quyết của phiên. Không sửa file trong mục "Không được làm" của `CLAUDE.md`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | 8 file tài liệu và test, một tệp log; không đổi code app hay script |
| B. Độ rõ của yêu cầu | 1 | Tuấn chọn 2a, 2b, 3a; danh sách L1 tới L9 do AI đề xuất từ bằng chứng, người duyệt quyết (câu hỏi 1, 2) |
| C. Khả năng kiểm chứng tự động | 1 | Phần bảng tra lỗi có test và đột biến; phần chữ của quy trình chỉ kiểm bằng đọc và rà soát |
| D. Ảnh hưởng chéo | 2 | Mọi phiên sau đọc `auto-run.md`, `risk-scoring.md`; T-066, T-068 sửa cùng file (mục 6) |
| E. Độ mới của kỹ thuật | 0 | Cùng cách với PB-11, PB-12 và ADR-0029 |
| **Tổng** | 5 | **Mức: Cao** (chạm điểm chặn 7; `risk_score` để trống theo quy tắc) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/ops/failure-playbook.md`, `docs/templates/run.md`, `scripts/tests/run.sh` (khối `playbook`), `docs/foundation.md`.
- Task khác có thể xung đột:
  - T-066 (`open_with`, khả năng của cách mở phiên, thử đẩy ở đầu phiên): dẫn dòng PB-13 của plan này, sửa `docs/auto-run.md` mục 2 và có thể `docs/templates/run.md` (phần đầu). Khối khác nhau; T-066 nên chạy sau T-067 để dẫn đúng PB-13 và loại `bo-qua-loai`.
  - T-068 (patch của nhánh không đẩy được vào nhánh báo cáo, `run-watchdog.yml`): sửa `docs/auto-run.md` mục Khi không đẩy được (bước 2 tới 4) và có thể thêm dòng bảng tra lỗi. Plan nào merge sau thì đặt nội dung của mình vào chỗ mới (L3) và lấy mã PB kế tiếp; nếu T-068 cũng định dùng PB-13 thì T-068 đổi sang PB-14.
  - T-065 (approved, chưa chạy): sửa `scripts/tests/run.sh` khối `setup-check`, không chạm khối `playbook`.
  - T-062 (approved, chưa chạy): không chung file.
- Đề xuất thứ tự chạy: T-067 đầu danh sách, trước T-062, T-065, T-066, T-068: các task sửa `.github/workflows/` sau đó, nếu lệnh đẩy vẫn bị từ chối, được bỏ qua đúng theo quy tắc mới thay vì làm phiên dừng.

## 7. Điều kiện dừng riêng

- Danh sách chạy có task đứng sau T-067 mà chính phiên đang làm theo quy tắc cũ: sau khi T-067 merge, đọc lại `docs/auto-run.md` và `docs/risk-scoring.md` trên nhánh chính rồi mới làm task sau; không dừng.
- Không có điều kiện dừng riêng khác.

## 8. Câu hỏi cho người duyệt

1. ADR-1005 vào `accepted` khi task merge (Tuấn là người quyết, ghi ở mục 9), hay `proposed` chờ anh Nhật? Đề xuất của AI: `accepted` ngay, để phiên dùng được từ phiên sau; anh Nhật xem lại qua đề xuất `from-app`. Để `proposed` thì `docs/foundation.md` vẫn liệt kê quy tắc của nó (script không lọc theo trạng thái `proposed`), nên phiên có thể đọc nhầm là đã có hiệu lực.
2. Danh sách L1 tới L9: có thêm hay bớt mục nào không? Đề xuất của AI: giữ chín mục. Cân nhắc nhưng không đưa vào: tự sửa test cũ ngoài mục 3.2 (đã có ADR-0029, quyết định 2), tự ghi lại ảnh chuẩn screenshot (cấm theo `CLAUDE.md`), tự đổi câu trả lời của người khi thấy sai (phải hỏi).
3. L1, mức trần 1600 giây của ngân sách kịch bản: đề xuất của AI: giữ 1600 (job `device-session` 50 phút, phần còn lại của lượt khoảng 20 phút); vượt thì nâng tới 1600 và ghi vào Cần người kiểm tra, không dừng.
4. L8 chỉ áp dụng cho lỗi khớp một dòng `bo-qua-loai` đã có trong bảng; lỗi mới chưa có dòng thì phiên vẫn làm như cũ (bước 6: kết thúc phiên khi task sau cũng không đẩy được). Đề xuất của AI: giữ như vậy; mỗi lỗi môi trường mới thành bài học rồi thành dòng bảng (`docs/auto-run.md` mục 6).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
