---
id: ADR-0043
title: Test trên emulator chạy một lần cho mỗi commit đổi code app của nhánh chính, trong job ui-test của main-guard, không chạy ở PR
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-30
deciders: [Nhật]
supersedes: ADR-0010         # chỉ thay ý emulator của mục 1 và hai ý emulator của mục 4 của ADR-0010, và nơi chạy chẩn đoán emulator của ADR-0019; các ADR đó vẫn còn hiệu lực
superseded_by:
---

# ADR-0043: TEST TRÊN EMULATOR CHẠY MỘT LẦN CHO MỖI COMMIT ĐỔI CODE APP CỦA NHÁNH CHÍNH, TRONG JOB UI-TEST CỦA MAIN-GUARD, KHÔNG CHẠY Ở PR

Nguồn: yêu cầu giảm chi phí GitHub Actions (`docs/features/quy-trinh/requests/2026-10-29-giam-phut-actions.md`, cách 3), ticket T-075.

## Bối cảnh

Job `ui-test` của `pr-check.yml` chạy test emulator (`app/src/androidTest`) ở mọi PR chạm code chạy trong app. Từ 2026-10-22 tới 2026-10-29 nó tốn 637 phút Actions của dự án mẫu, khoảng 18% số phút. `pr-check` chạy trung bình 1,7 lần mỗi PR (đẩy thêm commit, `merge-if-green.sh --update`), nên khoảng 41% số lần chạy emulator là chạy lại trên một PR đã kiểm. ADR-0010 mục 1 cho phép `ui-test` ở trạng thái bỏ qua khi PR không chạm code app; mục 4 nói `main-guard` không chạy test emulator vì commit đã qua cổng ở PR.

Sau ADR-0039, `main-guard` đọc kết quả `pr-check` của PR và không chạy lại `check.sh` khi cây file đã được kiểm. Nếu emulator rời khỏi PR mà không chạy ở đâu khác thì lỗi chỉ lộ ra trên emulator (L-051: điều hướng sau hàm `suspend` chạy trên luồng nền làm app dừng) sẽ đi thẳng tới bản staging và bản production.

Các nơi đọc kết quả `main-guard` hiện nay:

- `scripts/find-breaking-commit.sh` và `scripts/main-guard-status.sh` chỉ đọc check run `guard`.
- Bước mở issue `main-broken`, đẩy nhánh `revert/<sha>` và bước tự đóng issue nằm trong job `guard`.
- `scripts/release.sh` (không sửa, ADR-0009, ADR-0017) chỉ build bản production từ commit có một lần chạy workflow `main-guard.yml` kết luận `success`.
- Phiên tự chạy chờ cả workflow `main-guard.yml` sau mỗi lần merge (`scripts/wait-run.sh --workflow main-guard.yml`, `docs/auto-run.md` bước 9).

Từ 2026-10-30 `app-project-template` và `camera-pose` là repo công khai, phút Actions của hai repo không tính tiền. Phần tiết kiệm của quyết định này nằm ở các app private tạo từ dự án mẫu và nhận thay đổi qua `scripts/template-sync.sh` (ADR-0030); dự án mẫu giữ cùng cách để mẫu và app không lệch nhau.

Emulator trên máy của GitHub hay đỏ vì hạ tầng: tải file R2 hết giờ chờ (PB-01), kho Maven trả 403 (PB-11), file R2 không có (PB-13), gói Android Emulator tải lỗi nên emulator không khởi động (PB-15). Ở PR, phiên chạy lại job hay đẩy commit rỗng. Sau merge không còn nhánh để đẩy commit rỗng, và một lần đỏ vì hạ tầng mà mở `main-broken` thì chặn mọi lần merge.

## Quyết định

**1. Bỏ `ui-test` khỏi `pr-check.yml`.** `pr-check` chỉ còn job `changes` (bộ lọc `code`, `release`; bộ lọc `ui` bỏ vì không còn ai đọc) và `build-and-test`. `scripts/merge-if-green.sh` không còn đòi hay chờ kiểm tra `ui-test`; kiểm tra nào đỏ trên commit cuối của PR vẫn bị từ chối như trước, kể cả `ui-test` còn sót từ lần chạy trước quyết định này.

**2. Job `ui-test` trong `main-guard.yml`, sau job `guard`.**

- Job `guard` thêm một bước gọi `scripts/ui-test-needed.sh <sha> <sự kiện>`, in `yes` hay `no`, đưa ra thành output `ui` của job. `yes` khi commit có thay đổi so với commit cha ở code chạy trong app hay cách build nó (`app/`, `core/`, `feature/`, `build-logic/`, `gradle/`, `*.gradle.kts` ở gốc, `gradle.properties`, `gradlew`), ở các script mà job `ui-test` gọi, hay ở `.github/workflows/main-guard.yml`; khi chạy tay; và khi script không đọc được thay đổi. Danh sách đường dẫn là bộ lọc `ui` cũ của `pr-check.yml`.
- Job `ui-test` có `needs: guard` và `if: needs.guard.outputs.ui != 'no'`: output rỗng (bước `ui` không ghi được) vẫn chạy emulator. `guard` đỏ thì `ui-test` bị bỏ qua: nhánh chính đã đỏ, chạy emulator chỉ tốn phút. Job bị bỏ qua vẫn có check run kết luận `skipped`, và workflow vẫn kết luận `success`.
- Job chạy test emulator tối đa hai lần trên cùng emulator (`scripts/ui-test-on-emulator.sh`): lần đầu đỏ thì chạy lại một lần, như ADR-0025 mục 1 cho `check.sh`.
- `scripts/ui-test-verdict.sh` đọc log và cho một trong bốn kết luận:
  - `green`: lần chạy cuối xanh, lần đầu xanh hay đỏ vì hạ tầng.
  - `flaky`: lần đầu đỏ không vì hạ tầng, lần chạy lại xanh. Job xanh, ghi issue `flaky` như `guard`.
  - `infra`: không có log của Gradle (emulator không khởi động), kết quả lạ (`cancelled`, rỗng), bước emulator xanh mà không test nào chạy, 0 test kèm dấu hiệu thiết bị hỏng, hay log khớp một dòng trong danh sách trắng của script (PB-01, PB-11, PB-13, PB-15 của `docs/ops/failure-playbook.md`). Job đỏ, không mở `main-broken`; bước kết quả in `EMULATOR HẠ TẦNG (PB-NN): ...` cùng dòng log khớp mẫu, để tra bảng ra đúng dòng (tiền tố khác `HẠ TẦNG:` của PB-02).
  - `red`: mọi trường hợp đỏ khác, gồm tiến trình của app dừng (`Instrumentation run failed due to 'Process crashed`, `FATAL EXCEPTION` của tiến trình app; tiến trình hệ thống thì không tính) dù 0 test chạy, không có log mà commit đổi chính script hay workflow của job, 0 test không rõ lý do, và lần đầu đỏ thật mà lần chạy lại đỏ vì hạ tầng. Job đỏ, xử lý như mục 3.
- `infra` chỉ dành cho các lỗi không do code đã biết của job `ui-test` (PB-11 và PB-13 không riêng gì emulator). Một lỗi thật có log giống hệt lỗi hạ tầng (ví dụ code tải R2 hỏng giống PB-01) vẫn để job và workflow đỏ, và bị giới hạn ở mục 4.

**3. Emulator đỏ thật là nhánh chính đỏ**, như `guard` đỏ (ADR-0010, ADR-0025):

- Tìm commit gây lỗi bằng `scripts/find-breaking-commit.sh --check ui-test <sha>`: đọc check run `ui-test`, coi là chưa biết và đi tiếp qua commit có `ui-test` kết luận `skipped` (không đổi code app, hay `guard` đỏ nên emulator không chạy) commit có `ui-test` `failure` kèm annotation bắt đầu bằng `EMULATOR HẠ TẦNG (` (kết luận `infra`, không đổ lỗi cho nó), và commit có `ui-test` kết luận khác (`cancelled`, `timed_out`...) không chờ; job gọi script với thời gian chờ tối đa 15 phút để kịp mở issue trước khi hết giờ, dừng ở commit mà `main-guard.yml` của chính commit đó chưa có job `ui-test` (emulator của commit đó đã chạy ở PR).
- Mở issue `main-broken` có thêm dòng `<!-- check:ui-test -->` cạnh dòng `<!-- culprit:<sha> -->`, đẩy nhánh `revert/<sha>`. Đã có issue `main-broken` của `ui-test` đang mở thì chỉ bình luận.
- Mỗi job chỉ bình luận vào issue của chính nó và chỉ tự đóng issue của chính nó: `guard` bỏ qua issue có dòng `check:ui-test`, `ui-test` chỉ xét issue có dòng đó. `guard` xanh không đóng issue của emulator; hai job cùng đỏ thì có hai issue, và `merge-if-green.sh` từ chối mọi PR, kể cả bản revert, như khi có hai issue bất kỳ (ADR-0025 mục 3).
- `merge-if-green.sh`, bản revert đúng, quy tắc phiên tự chạy revert commit của chính phiên (ADR-0025 mục 3, 4) giữ nguyên: issue vẫn có dòng `culprit`.

**4. Đỏ vì hạ tầng sau merge.** Không mở `main-broken`. Người hay phiên chạy lại riêng job đỏ của lần chạy đó (`rerun-failed-jobs`; `guard` xanh không chạy lại), theo dòng của `failure-playbook.md`; không còn cách đẩy commit rỗng. Không chạy lại được thì ghi cho người, nhánh chính không bị coi là đỏ. Lần chạy `ui-test` của commit code app kế tiếp kiểm lại toàn bộ app. Giới hạn: hai commit đổi code app liên tiếp có kết luận `infra` mà chưa chạy lại xanh thì phiên tự chạy không merge thêm task đổi code app (`docs/auto-run.md` bước 9).

**5. Nơi đọc kết quả.**

- `release.sh` đọc kết luận của cả workflow `main-guard.yml` trên commit đổi code (ngoài `docs/`, `*.md`) mới nhất: `ui-test` của commit đó đỏ, thật hay vì hạ tầng, chặn phát hành tới khi chạy lại xanh. Commit code app cũ hơn có `ui-test` đỏ vì hạ tầng mà commit mới nhất chỉ đổi script (`ui-test` bỏ qua) thì không chặn; PR tăng `versionCode` đổi `app/build.gradle.kts` nên emulator luôn chạy trên commit phát hành, và `docs/ops/release.md` dặn chạy lại tới khi xanh. Người duyệt ADR này chấp nhận nghĩa mới của "qua `main-guard.yml`": gồm cả test emulator của commit đó.
- `main-guard-status.sh` và `start-run.yml` vẫn chỉ đọc `guard`. Emulator đỏ thật được chặn qua issue `main-broken`, mà `start-run.yml` và đầu phiên đã kiểm riêng. Emulator đang chạy hay đỏ vì hạ tầng không chặn bắt đầu phiên.
- Phiên tự chạy vẫn chờ cả workflow sau mỗi lần merge (`docs/auto-run.md` bước 9), nên thấy emulator đỏ của chính task trước khi đẩy task sau.

**6. Hai lớp emulator khác nhau.** `ui-test` của `main-guard` chạy test viết sẵn trong `app/src/androidTest` trên bản staging debug với cấu hình Firebase giả, cho từng commit đổi code app; đỏ thật là nhánh chính đỏ. `device-session.yml` cuối phiên tự chạy cài bản staging release với cấu hình staging thật, chạy kịch bản chuẩn và bấm ngẫu nhiên tìm crash, ANR; đỏ thì không phát bản staging và soạn phản hồi, không mở `main-broken` (`docs/auto-run.md` mục 4). Lớp thứ hai vẫn giữ.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Job `ui-test` trong `main-guard`, sau `guard`, mỗi commit đổi code app (đã chọn) | Mỗi commit một lần, khoảng 5 phút; bớt khoảng 41% phút emulator; emulator đỏ chặn phát hành qua `release.sh` mà không sửa script đó; phiên thấy lỗi của chính task trước task sau; commit gây lỗi thường là chính commit đỏ | Lỗi chỉ lộ trên emulator vào nhánh chính rồi mới bị bắt; thời gian chờ `main-guard` dài thêm khoảng 5 phút cho commit code app |
| Workflow riêng mỗi đêm cho commit mới nhất có đổi code | Rẻ nhất: khoảng 7 lần mỗi tuần thay vì một lần mỗi commit code app, bớt thêm khoảng 300 phút mỗi tuần ở dự án mẫu | Phát hiện chậm tới một ngày; một lần chạy gộp nhiều commit nên không biết commit gây lỗi, không có bản revert đúng; phiên tự chạy (chạy đêm) không dừng được khi lỗi xảy ra; `release.sh` không thấy kết quả vì chỉ đọc `main-guard.yml` |
| Workflow riêng chạy theo `push` với bộ lọc `paths`, mỗi commit | Không tốn máy cho commit không đổi code app | `release.sh` không thấy kết quả, phát hành được commit có emulator đỏ; thêm một workflow phải chờ và đọc |
| Emulator đỏ chỉ mở nhãn nhẹ hơn (ví dụ `ui-broken`), không chặn merge | Một lần đỏ không chặn mọi PR | Merge tiếp lên một app dừng khi mở; phiên cần quy tắc dừng mới; không dùng được bản revert đúng của ADR-0025 |
| Giữ `ui-test` ở PR | Bắt lỗi trước merge | Khoảng 18% số phút Actions, phần lớn là chạy lại |

## Hệ quả

- **Tích cực:**
  - PR không chạy emulator; phút emulator giảm khoảng 41% ở dự án mẫu, nhiều hơn ở app có PR được cập nhật nhiều lần.
  - Emulator đỏ thật vẫn mở `main-broken`, có commit gây lỗi và bản revert; phiên tự chạy dùng lại đúng quy trình của ADR-0025.
  - Một lần chập chờn hay lỗi hạ tầng của emulator không chặn mọi lần merge.
- **Phải chấp nhận:**
  - Lỗi chỉ lộ ra trên emulator vào nhánh chính rồi mới bị bắt và revert. Plan vẫn phải có test Robolectric cho vỏ app (L-051, `docs/CLAUDE.md` mục Khi viết plan).
  - `main-guard` của commit đổi code app dài thêm khoảng 5 phút ở dự án mẫu (lâu hơn ở app có nhiều test emulator), thêm một lần chạy khi lần đầu đỏ.
  - Kết luận `infra` cho một lỗi thật có log giống lỗi hạ tầng để job đỏ mà không mở `main-broken`; người đọc log và quyết định; quá hai lần liên tiếp thì phiên dừng merge code app.
  - `main-guard-status.sh` không đọc `ui-test`: phiên có thể bắt đầu khi emulator của commit cuối còn đang chạy; đỏ thật sau đó mở `main-broken` và phiên dừng ở lần kiểm kế tiếp.
  - PR đang mở có `ui-test` đỏ từ trước quyết định này vẫn bị từ chối tới khi có commit mới.
  - Danh sách file mà job `guard` gọi hay dựa vào (ADR-0039 mục 2) thêm `scripts/ui-test-needed.sh`.
- **Việc AI phải tuân thủ:** test emulator chạy ở job `ui-test` của `main-guard.yml` sau job `guard`, không chạy ở `pr-check.yml`; `merge-if-green.sh` không đòi hay chờ `ui-test`; chỉ kết luận `red` của `scripts/ui-test-verdict.sh` mở `main-broken`; `infra` chỉ cho danh sách trắng PB của script và thiết bị hỏng, app dừng luôn là `red`; `find-breaking-commit.sh --check ui-test` không đổ lỗi cho commit có annotation `EMULATOR HẠ TẦNG (`, không chờ commit có `ui-test` bị hủy hay hết giờ; issue `main-broken` của emulator có dòng `<!-- check:ui-test -->`, và mỗi job chỉ bình luận, chỉ tự đóng issue của chính nó; emulator đỏ vì hạ tầng sau merge thì chạy lại riêng job đỏ theo `docs/ops/failure-playbook.md`, không mở `main-broken`, không đẩy commit rỗng lên nhánh chính; thêm đường dẫn code app hay script mà job `ui-test` gọi thì sửa danh sách của `scripts/ui-test-needed.sh` và test của nó cùng PR
