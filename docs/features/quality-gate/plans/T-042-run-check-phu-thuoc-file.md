---
ticket: T-042
title: run-check.py báo plan phụ thuộc file của plan khác
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.0M         # cỡ S, số gợi ý của scripts/token-usage.py estimate
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-24
---

# PLAN T-042: RUN-CHECK.PY BÁO PLAN PHỤ THUỘC FILE CỦA PLAN KHÁC

## 1. Tóm tắt

`scripts/run-check.py` thêm hai kiểm tra trên cả danh sách chạy.

1. **Đề xuất thứ tự chạy.** Script đọc dòng "Đề xuất thứ tự chạy" ở mục 6 của mỗi plan: "A trước B", "sau A", "trong nhóm: A, B, C", "A, rồi B". Thứ tự trong danh sách ngược với đề xuất là lỗi, trừ khi danh sách ghi rõ "Bỏ đề xuất thứ tự: A trước B" kèm lý do.
2. **File của plan khác.** Plan nhắc tới một file (ở mục 3 hay 4) mà plan khác trong danh sách ghi Tạo, không có file đó trong mục 3.2 của mình, và không phụ thuộc plan kia:
   - đứng trước plan kia thì là lỗi;
   - đứng sau thì là chú ý, kèm "nếu <plan trước> dừng thì <plan sau> thiếu <file>".

Chạy thử trên dữ liệu thật: danh sách bản đầu của RUN-20261023-03 (T-020 trước T-017) bị báo 3 lỗi; RUN-20261023-04 và các danh sách cũ không có lỗi nào ngoài một đề xuất thật bị bỏ qua ở RUN-20261022-01 (mục 4.1).

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là bài học L-060 (`docs/lessons.md`), từ rà soát độc lập trước RUN-20261023-03.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| L-060, thứ tự | Khi plan sau nhắc tới file mà plan khác ghi Tạo, hoặc plan ghi đề xuất thứ tự chạy, mà danh sách xếp ngược thì `run-check.py` báo lỗi | Điểm chặn T-017, T-020 không lộ qua tên file: plan T-017 không hề nhắc `CameraUnfolded.kt`. Nó lộ qua dòng "Đề xuất thứ tự chạy" của ba plan T-015, T-017, T-020, đều ghi T-017 trước T-020. Vì vậy cần cả hai kiểm tra: đề xuất thứ tự (bắt được trường hợp đã gặp) và tên file (bắt trường hợp plan dùng file của plan khác) |
| L-060, plan trước dừng | Với mỗi cặp, `run-check.py` in file nào plan sau sẽ phải sửa thêm hay sẽ thiếu nếu plan trước dừng | Chỉ in khi biết chắc từ tài liệu: plan sau nhắc tới file mà plan trước tạo, ngoài mục 3.2 của mình, và không có `depends_on` tới plan trước (có `depends_on` thì phiên đã bỏ qua plan sau khi plan trước dừng) |
| L-060, không báo nhầm | Danh sách đã đúng thứ tự (RUN-20261023-04) không có lỗi mới | Đo trên mọi danh sách cũ ở mục 4.1 |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: không có. Script này chạy lúc lập danh sách (`docs/ops/pre-run-review.md`) và ở kiểm tra đầu phiên (`docs/auto-run.md`).

## 3. Giải pháp

### 3.1. Cách làm

Theo mẫu của các kiểm tra hiện có trong `check()` của `scripts/run-check.py`. Chúng đọc mục 3.2 bằng `PLAN_CHECK.file_list`, mục của plan bằng `chapter()`, và `depends_on` của ticket.

**Kiểm tra 1, đề xuất thứ tự** (hàm mới `order_hints(ticket, plan_text) -> list[(trước, sau)]`):

- Chỉ đọc các dòng của mục 6 có chữ "thứ tự", tách theo `;` và `. `.
- Trong mỗi vế, đọc các mẫu sau:
  - `A trước B[, C| và C]`, chủ ngữ mặc định là ticket của plan khi vế bắt đầu bằng "trước": cho ra (A, B), (A, C);
  - `sau A`, kể cả "ngay sau A": cho ra (A, ticket);
  - `nhóm ...: A, B, C`, `nhóm ... (A, B, C)` hay `thứ tự ...: A, B, C`: các cặp liền nhau;
  - `A, rồi B, rồi C`: các cặp liền nhau.
- Danh sách kiểu "T-012, T-016, T-025 không phụ thuộc T-021" không theo mẫu nào, nên không thành ràng buộc.
- Chỉ xét cặp mà cả hai ticket đều có trong danh sách.

`check()` báo lỗi khi vị trí của "trước" lớn hơn vị trí của "sau":

```
T-017 đứng sau T-020, nhưng plan T-017 (mục 6) đề xuất T-017 trước T-020 (L-060). Đổi thứ tự, hoặc ghi "Bỏ đề xuất thứ tự: T-017 trước T-020" kèm lý do vào mục Lý do của thứ tự chạy.
```

Nhiều plan cùng đề xuất một cặp thì mỗi cặp chỉ báo một lần, nêu tên mọi plan đã đề xuất.

Danh sách chứa dòng khớp `Bỏ đề xuất thứ tự:\s*(T-\d+) trước (T-\d+)` thì cặp đó chuyển thành chú ý, có ghi lý do người lập danh sách đã nêu.

**Kiểm tra 2, file của plan khác:**

- Với mỗi cặp (A, B) trong danh sách, xét các file A ghi Tạo mà không có trong mục 3.2 của B.
- B "nhắc" file đó khi mục 3 hay mục 4 của B có đường dẫn đầy đủ trong dấu backtick, hoặc tên file trong dấu backtick nếu tên đó là duy nhất. Duy nhất nghĩa là không file nào khác trong repo (`git ls-files`) và không file Tạo nào khác của danh sách mang cùng tên. Nhờ vậy các tên chung như `strings.xml`, `build.gradle.kts`, `Fakes.kt` không làm báo nhầm.
- Mục 6 không tính: đó là chỗ plan ghi ảnh hưởng chéo kiểu "plan kia có thể thêm `ic_refresh.xml`, không ảnh hưởng".
- B có A trong chuỗi `depends_on` thì bỏ qua cặp đó: phụ thuộc đã kiểm ở kiểm tra 3 hiện có.
- B đứng trước A là lỗi:

  ```
  T-B nhắc `X.kt` mà T-A (đứng sau) tạo (L-060).
  ```

- B đứng sau A là chú ý:

  ```
  T-B nhắc `X.kt` mà T-A tạo; nếu T-A dừng thì T-B thiếu file này. Cân nhắc thêm T-A vào depends_on của T-B, hoặc ghi phương án ở mục 3.4 của T-B (L-060).
  ```

- Không có repo git (thư mục test) thì `git ls-files` trả rỗng, chỉ đếm tên trong các plan.

Docstring của script thêm điểm 7 cho hai kiểm tra này. `docs/ops/pre-run-review.md` mục 1 thêm hai dòng vào bảng kiểm tra.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/run-check.py` | `order_hints`, kiểm tra đề xuất thứ tự và cách bỏ qua có lý do; kiểm tra file của plan khác; docstring điểm 7 |
| Sửa | `scripts/tests/run.sh` | `write_run_plan` nhận thêm tham số thứ 5 (đoạn mục 3 và mục 6 viết thêm vào plan giả); 8 test mới ở mục 4, đặt sau test L-033 của `run-check` |
| Sửa | `docs/ops/pre-run-review.md` | Mục 1: hai dòng mới trong bảng kiểm tra; cách ghi "Bỏ đề xuất thứ tự" |

L-020: `scripts/run-check.py` được đọc bởi `scripts/tests/run.sh` (test), `docs/ops/pre-run-review.md`, `docs/auto-run.md` (kiểm tra đầu phiên gọi script) và `docs/CLAUDE.md` (mục Khi lập danh sách chạy). `auto-run.md` và `docs/CLAUDE.md` chỉ gọi script và sửa hết lỗi, không liệt kê từng kiểm tra, nên không phải đổi. Không workflow nào trong `.github/` gọi script này (`grep -rn run-check .github/` rỗng). `scripts/merge-if-green.sh` và `scripts/wait-run.sh` chỉ nhắc `scripts/wait-run.sh`, không đọc `scripts/tests/run.sh`. Plan chỉ thêm test vào khối `run-check` của file test, không đổi phần test của hai script đó.

Ở kiểm tra đầu phiên (`auto-run.md`, bảng kiểm tra), lỗi của `run-check.py` về một task làm phiên bỏ qua task đó. Lỗi L-060 mới không tới được bước này: danh sách chỉ được duyệt khi `run-check.py` qua (`auto-run.md`, mục 1, bước 2), và plan đã duyệt không đổi sau đó. Nên `auto-run.md` không phải đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Chỉ dùng thư viện chuẩn Python (`re`, `subprocess` cho `git ls-files`).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** cách đọc câu tiếng Việt ở mục 6 có thể báo nhầm với một plan viết theo cách khác các plan hiện có.
- **Điều kiện chuyển:** khi chạy `run-check.py` trên các danh sách cũ ở bước 5 (mục 4, dòng cuối), có lỗi đề xuất thứ tự nào khác ba lỗi RUN-20261023-03 và lỗi T-008 của RUN-20261022-01 đã biết.
- **Cách làm thay:** kiểm tra đề xuất thứ tự chỉ báo chú ý, không báo lỗi, với câu đọc được bằng mẫu "nhóm" và "rồi"; giữ lỗi cho mẫu `A trước B` và `sau A`. Test ở mục 4 giữ nguyên (các test lỗi dùng mẫu `A trước B`).

## 4. Kiểm thử

Mọi test vào `scripts/tests/run.sh`, khối `run-check` hiện có, dùng repo giả `$rr`. Tham số thứ 5 của `write_run_plan`, chuỗi `printf` được chèn trước `## 4. Kiểm thử`, vào commit test của bước 3 cùng các test (L-025). Trên code hiện tại, mọi test "stop" và "warn" dưới đây thất bại vì script chưa có kiểm tra; các test "pass" và "không báo" qua ngay, nên mỗi test đó đi cùng một test đối chứng cùng dữ liệu nhưng sai thứ tự (L-035).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| L-060, thứ tự: đề xuất `A trước B` bị ngược | Script test | `run-check: a plan's order hint broken by the list stops`: T-001 mục 6 "Đề xuất thứ tự chạy: T-001 trước T-002", danh sách T-002, T-001: `stop`, thông báo có "T-001 trước T-002" và "L-060" |
| L-060, thứ tự: đúng thứ tự thì qua (đối chứng của dòng trên) | Script test | `run-check: a list following the order hint passes`: cùng plan, danh sách T-001, T-002: `pass`, không có "L-060" |
| L-060, thứ tự: mẫu `sau A` và mẫu nhóm | Script test | `run-check: 'sau' and group order hints are read`: T-002 "Đề xuất thứ tự chạy: sau T-001"; danh sách T-002, T-001: `stop`. Rồi T-001 "trong nhóm demo: T-001, T-002", T-002 không có đề xuất, cùng danh sách: `stop` |
| L-060, không báo nhầm: danh sách tên không theo mẫu | Script test | `run-check: a list of tickets that is not an order is no hint`: T-001 "Đề xuất thứ tự chạy: T-002, T-003 không phụ thuộc T-001"; danh sách T-002, T-001: `pass` |
| L-060, bỏ đề xuất có lý do | Script test | `run-check: a dropped order hint with a reason warns`: danh sách ngược của dòng đầu, thêm dòng "Bỏ đề xuất thứ tự: T-001 trước T-002 (lý do)" vào danh sách: `pass`, có một dòng "CHÚ Ý" nhắc "T-001 trước T-002" |
| L-060, file của plan khác: plan nhắc file mà plan sau tạo | Script test | `run-check: a plan using a file another later plan creates stops`: T-002 Tạo `feature/demo/Unfolded.kt`; T-001 mục 3 "nối `Unfolded.kt`" và không có file này ở mục 3.2; danh sách T-001, T-002: `stop`, thông báo "nhắc `Unfolded.kt` mà T-002 (đứng sau) tạo" |
| L-060, plan trước dừng: in file bị thiếu | Script test | `run-check: a later plan using a created file warns what it misses`: cùng plan, danh sách T-002, T-001: `pass`, một dòng "CHÚ Ý" có "nếu T-002 dừng thì T-001 thiếu" và `Unfolded.kt`. Đối chứng: thêm `T-002` vào `depends_on` của T-001: không còn dòng này |
| L-060, không báo nhầm: tên file chung, mục 6 | Script test | `run-check: a common file name or a section 6 note is no use`: T-002 Tạo `feature/demo/strings.xml`, đã có `feature/other/strings.xml` trong `$rr` (repo giả được `git init`); T-001 mục 3 nhắc `strings.xml`: không có "L-060". T-001 chỉ nhắc `Unfolded.kt` ở mục 6: không có "L-060" |
| L-060, không báo nhầm trên dữ liệu thật | Chạy tay ở bước 5, ghi vào nhật ký | `scripts/run-check.py` trên `docs/runs/RUN-20261023-04.md` và các danh sách cũ: không có lỗi L-060 nào ngoài các lỗi đã biết ở mục 4.1 (các lỗi khác về trạng thái plan là do plan đã `done`, bỏ qua); danh sách tạm có T-020 trước T-017 (chép RUN-20261023-04, đổi hai dòng, không commit): đúng 1 lỗi L-060 (một cặp, ba plan) |

**Ngoại lệ viết test trước:** `docs/ops/pre-run-review.md` (chỉ là tài liệu).

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-24, một script Python tạm, không commit, nhập `scripts/run-check.py`, cài đúng hai kiểm tra của mục 3.1. Chạy trên commit `b3459bf` (nhánh chính).
- **Lệnh:** `python3 -I spike.py <repo> <danh sách> ...`, chạy trên:
  - danh sách bản đầu của RUN-20261023-03, dựng lại từ RUN-20261023-04 với T-020 ở vị trí 6 và T-017 ở vị trí 7 (bản đầu chưa từng được commit vì rà soát độc lập sửa thứ tự trước);
  - RUN-20261023-04, RUN-20261023-02, RUN-20261023-01, RUN-20261022-01, RUN-20261021-01 đến 04.
- **Kết quả:**
  - **Đề xuất thứ tự:**
    - Bản đầu RUN-20261023-03 có 3 lỗi, cùng một cặp "T-017 trước T-020" do T-015, T-017 và T-020 cùng đề xuất. Vì vậy mục 3.1 gộp mỗi cặp thành một lỗi.
    - RUN-20261023-04 không có lỗi; 16 ràng buộc đọc được đều thỏa.
    - RUN-20261022-01 có 1 lỗi thật: plan T-008 ghi "trước T-010" nhưng danh sách xếp T-010 trước. Phiên vẫn chạy qua vì hai task chỉ cùng sửa vài dòng. Đây là trường hợp dùng "Bỏ đề xuất thứ tự" kèm lý do.
    - Các danh sách còn lại không có lỗi.
  - **File của plan khác:**
    - Lần đầu khớp theo mọi tên file: 15 báo nhầm (`Fakes.kt`, `build.gradle.kts`, `strings.xml` của module khác).
    - Lần hai thêm điều kiện tên duy nhất và bỏ mục 6. Còn lại các cặp đã có `depends_on` (T-023 với T-022, T-016 với T-014), nên thêm điều kiện bỏ cặp có phụ thuộc; còn hai báo nhầm ở mục 6 (T-008 với `ic_refresh.xml`, T-014 với `strings_review.xml`), đều là ghi chú "không ảnh hưởng".
    - Bản cuối không còn báo nhầm nào trên các danh sách cũ. Nó cũng không có lần báo đúng nào trên dữ liệu thật: plan T-017 không nhắc `CameraUnfolded.kt`, nên trường hợp đã gặp chỉ bắt được qua đề xuất thứ tự (mục 8, câu 2).
  - Script tạm đếm tên file bằng `git ls-files` trừ các file mà danh sách ghi Tạo, vì sau khi merge chúng đã có trong repo.
  - Không chạy `scripts/check.sh`: E bằng 1, script Python thuần, đúng mẫu của các kiểm tra sẵn có.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (quy trình phiên tự chạy). Kiểm tra đầu phiên chạy `run-check.py`; một lỗi mới làm phiên không bắt đầu task nào.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một script, test của nó và một tài liệu |
| B. Độ rõ của yêu cầu | 1 | L-060 nêu cách sửa; cách đọc câu tiếng Việt do AI chọn, đã đo trên dữ liệu thật |
| C. Khả năng kiểm chứng tự động | 0 | Mọi tiêu chí có script test; thêm lần chạy trên các danh sách thật |
| D. Ảnh hưởng chéo | 1 | Mọi lần lập danh sách và kiểm tra đầu phiên; không ai khác sửa file này |
| E. Độ mới của kỹ thuật | 1 | Biểu thức chính quy trên tài liệu, như `plan-check.py` |
| **Tổng** | 4 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/run-check.py` (kiểm tra trước khi duyệt và đầu phiên), `scripts/tests/run.sh`.
- Task khác trong sprint có thể xung đột:
  - T-036, T-037, T-039, T-040 và T-041 cũng thêm test vào `scripts/tests/run.sh`, mỗi plan một khối riêng. Plan này chỉ sửa khối `run-check` và hàm `write_run_plan` của khối đó, nên chỉ có thể xung đột dòng khi gộp.
  - Không plan nào khác sửa `run-check.py`.
- Đề xuất thứ tự chạy: không phụ thuộc task nào; nên chạy trước khi lập danh sách chạy tiếp theo có nhiều task cùng feature.

## 7. Điều kiện dừng riêng

1. Chạy `run-check.py` mới trên RUN-20261023-04 có lỗi L-060: dừng, vì danh sách đó đã chạy đúng thứ tự (trừ khi chuyển sang phương án ở mục 3.4).

## 8. Câu hỏi cho người duyệt

1. **Mức của đề xuất thứ tự bị ngược.** Đề xuất của AI: báo lỗi, vì trường hợp T-017, T-020 là điểm chặn. Lỗi được bỏ có kiểm soát: dòng "Bỏ đề xuất thứ tự: A trước B" kèm lý do trong danh sách chạy, như trường hợp T-008, T-010 (chú ý thay cho lỗi). Cách khác: chỉ báo chú ý cho mọi trường hợp, để rà soát độc lập quyết.
2. **Giữ kiểm tra file của plan khác.** Trên dữ liệu thật, kiểm tra này chưa bắt đúng lần nào và cũng không báo nhầm. Nó bắt được trường hợp plan dùng file của plan khác, như T-023 và T-024 dùng file của T-022 nếu thiếu `depends_on`. Đề xuất của AI: giữ, ở mức như mục 3.1.
3. **Đưa vào mẫu plan một trường máy đọc được.** Đề xuất của AI: chưa. Mẫu plan có thể có thêm trường `run_after: [T-015]` thay cho việc đọc câu, nhưng các plan đã duyệt không sửa được, nên script vẫn phải đọc câu; quyết sau khi chạy vài danh sách.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-24, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
