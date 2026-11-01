---
id: ADR-0041
title: merge-if-green.sh merge PR code mà không cập nhật nhánh khi nhánh chính chỉ đổi tài liệu từ điểm tách, và PR nháp không chạy pr-check
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-30
deciders: [Nhật]
supersedes: ADR-0010         # chỉ thay một phần mục 1 ("nhánh của PR đã cập nhật theo nhánh chính mới nhất") và thêm vào mục 4; ADR-0010 vẫn còn hiệu lực. Bỏ một giả định ở mục Hệ quả của ADR-0039 (xem Bối cảnh)
superseded_by:
---

# ADR-0041: MERGE-IF-GREEN.SH MERGE PR CODE MÀ KHÔNG CẬP NHẬT NHÁNH KHI NHÁNH CHÍNH CHỈ ĐỔI TÀI LIỆU TỪ ĐIỂM TÁCH, VÀ PR NHÁP KHÔNG CHẠY PR-CHECK

Nguồn: yêu cầu giảm chi phí GitHub Actions (`docs/features/quy-trinh/requests/2026-10-29-giam-phut-actions.md`, cách 5), ticket T-077.

## Bối cảnh

`pr-check` chạy trung bình 1,7 lần mỗi PR. Ngoài lần đẩy commit mới, có hai nguồn chạy lại không bắt được lỗi mới:

- `scripts/merge-if-green.sh` chỉ merge khi nhánh của PR không chậm commit nào so với nhánh chính (ADR-0010 mục 1). Nhánh chính có thêm một commit, kể cả commit chỉ đổi tài liệu (báo cáo phiên, bài học, plan), thì `--update` gộp nhánh chính vào nhánh của PR và chạy lại toàn bộ `pr-check` (khoảng 6 tới 12 phút) cho một tổ hợp code giống hệt lần đã kiểm.
- `pr-check` chạy cho mọi PR, kể cả PR nháp. Phiên tự chạy mở PR plan và PR sửa nguyên nhân ở dạng nháp; người chỉ merge chúng sau khi chuyển sang sẵn sàng, và mỗi lần đẩy lên PR nháp đều tốn một lần chạy.

ADR-0039 cho `main-guard` ba cách kiểm. Cách `tests` (chỉ chạy `scripts/tests/run.sh`) dành cho commit merge mà cây file chỉ khác commit cuối của PR ở tài liệu. Mục Hệ quả của ADR-0039 giả định nhánh của PR không bao giờ chậm hơn nhánh chính lúc merge, nên cách `tests` hiếm. ADR này bỏ giả định đó: PR code merge mà không cập nhật thì cây của commit merge bằng cây đã kiểm cộng phần tài liệu mà nhánh chính đổi từ điểm tách, và `main-guard` chạy cách `tests` cho nó.

## Quyết định

**1. Merge không cập nhật nhánh.** Khi nhánh của PR chậm hơn nhánh chính, `merge-if-green.sh` merge mà không cập nhật nhánh khi đủ mọi điều sau. Thiếu một điều, hay script không đọc được dữ liệu để quyết, thì cập nhật nhánh như cũ (mã 3, hay `--update`).

- **PR code:** lần `pr-check` mới nhất trên commit cuối của PR xanh, và bước "Cổng chất lượng (scripts/check.sh)" của job `build-and-test` có kết quả `success`. Đây đúng là điều kiện mà `scripts/main-guard-verified.sh` dùng cho cách `skip` và `tests`. PR không có code (chỉ tài liệu, hay chỉ file ngoài bộ lọc code) chỉ chạy `scripts/tests/run.sh` ở `pr-check`, nên luôn cập nhật: commit merge của PR chỉ tài liệu không kích hoạt `main-guard` (`paths-ignore`), nên lần chạy lại `pr-check` sau khi cập nhật là lần duy nhất tổ hợp tài liệu hai bên được kiểm (ví dụ bài học trùng số).
- **Nhánh chính chỉ đổi tài liệu từ điểm tách:** mọi file khác nhau giữa điểm tách (merge base) và đầu nhánh chính, kể cả tên cũ của file đổi tên, nằm trong `docs/**` hay là `**/*.md` (đúng `paths-ignore` của `main-guard.yml`), và không file nào là file trong `docs/` mà Gradle đọc (`GRADLE_READ_DOCS` của `main-guard-verified.sh`). So cây ở hai đầu, như `main-guard-verified.sh` so cây commit merge với cây đã kiểm. GitHub trả từ 300 file trở lên thì coi là không chỉ tài liệu.
- **Không file nào đổi ở cả hai bên:** không file nào vừa nằm trong danh sách trên vừa nằm trong danh sách file của PR. Hai bên cùng sửa `docs/lessons.md`, `docs/foundation.md` hay một plan là chỗ tổ hợp tài liệu dễ hỏng nhất; khi đó cập nhật để `pr-check` kiểm trước khi merge.
- **Không xung đột:** GitHub báo PR `mergeable: true`. Xung đột (`false`) thì cập nhật như cũ. Chưa tính xong (`null`, hay gặp ngay sau khi nhánh chính đổi) thì đọc lại vài lần, rồi chờ như CI chưa xong; không cập nhật vội.

Mọi kiểm tra khác của `merge-if-green.sh` giữ nguyên và đứng trước điều này: PR mở, không nháp, không nhãn `needs-human`, nhắm vào `main`, không có `main-broken` (bản revert đúng vẫn phải nằm ngay trên nhánh chính mới nhất), không có phiên đang chạy chặn PR, `pr-check` xanh trên đúng commit cuối, merge kiểu squash ghim mã commit đó.

**2. Một chỗ quyết "chỉ tài liệu" và "PR đã chạy check.sh".** `merge-if-green.sh` không tự viết lại hai quy tắc này mà gọi `scripts/main-guard-verified.sh` (`--docs-only` đọc danh sách đường dẫn, `--gate-ran <sha>` đọc lần `pr-check` của commit). Vì vậy PR được merge theo mục 1 được `main-guard` kiểm bằng cách `tests` (hay `full` khi PR đổi một file của cổng kiểm), không `skip` khi còn phần tài liệu chưa kiểm. Nhánh chính đổi rồi đổi lại (khác ròng rỗng) thì cây giống hệt và `skip` vẫn đúng. `merge-if-green.sh` cũng nhận commit squash của đường này là "đã merge" khi GitHub trả lỗi (L-080): cùng tiêu đề và cây chỉ khác commit của PR ở tài liệu; cùng tiêu đề mà khác ngoài tài liệu thì từ chối, không merge lần hai.

**3. Tổ hợp tài liệu của PR code được kiểm sau merge.** Với PR merge theo mục 1, tài liệu của PR ghép với tài liệu mới của nhánh chính chỉ được kiểm bằng `scripts/tests/run.sh` ở cách `tests` của `main-guard`. Đỏ ở đó là nhánh chính đỏ (ADR-0039 mục 4): issue `main-broken`, chặn mọi lần merge tới khi người xử lý.

**4. PR nháp không chạy `pr-check`.** `pr-check.yml` khai báo `types: [opened, synchronize, reopened, ready_for_review]` (khai báo `types` thì bộ mặc định mất, nên phải có đủ `synchronize`) và job `build-and-test` có `if:` cấp job bỏ PR nháp. PR chuyển sang sẵn sàng thì `pr-check` chạy trên commit cuối. `merge-if-green.sh` vẫn từ chối PR nháp như trước. `scripts/wait-run.sh --pr` gặp PR nháp thì báo ngay (mã riêng), không chờ một lần chạy sẽ không tới, và không coi lần chạy bị bỏ qua lúc PR còn nháp là kết quả của commit.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Không cập nhật khi nhánh chính chỉ đổi tài liệu, PR đã chạy `check.sh`, không trùng file, không xung đột (đã chọn) | Bớt một lần `pr-check` đầy đủ mỗi khi nhánh chính chỉ có thêm báo cáo, bài học, plan; code vào nhánh chính vẫn là code đã kiểm; dùng chung quy tắc với `main-guard` | Tổ hợp tài liệu của PR code chỉ được kiểm sau merge; thêm hai lệnh gọi API cho PR chậm hơn nhánh chính |
| Như trên nhưng không cần "không trùng file" | Bớt nhiều lần chạy hơn | Bài học trùng số, `foundation.md` lệch thành `main-broken` thường xuyên hơn, chặn mọi lần merge |
| Áp cho mọi PR, kể cả PR chỉ tài liệu | Bớt thêm khoảng 2 phút mỗi lần | Commit merge của PR chỉ tài liệu không kích hoạt `main-guard`: tổ hợp tài liệu không được kiểm ở đâu |
| Xét "từ lúc `pr-check` chạy" thay vì "từ điểm tách" | Bớt cả khi nhánh chính có code đã nằm trong `refs/pull/N/merge` lúc kiểm | `main-guard` so với commit cuối của PR nên thấy code khác và chạy `full`: không bớt phút, và hai bên dùng hai quy tắc |
| `if:` cấp workflow hay `paths` để bỏ PR nháp | Không có lần chạy bị bỏ qua | `pull_request` không lọc được theo nháp ở cấp workflow; chỉ `if:` của job làm được |

## Hệ quả

- **Tích cực:**
  - Ngày có nhiều PR tài liệu (báo cáo phiên, plan, bài học), PR code không phải chạy lại `pr-check` sau mỗi lần nhánh chính đổi.
  - PR plan và PR sửa nguyên nhân của phiên tự chạy (nháp) không tốn phút tới khi người chuyển chúng sang sẵn sàng.
- **Phải chấp nhận:**
  - Bài học trùng số hay `foundation.md` lệch giữa một PR code và tài liệu mới của nhánh chính, ở file khác nhau, chỉ lộ ra sau merge, thành `main-broken`.
  - Trong khoảng giữa lần kiểm và lệnh merge, nhánh chính có thể nhận thêm commit code: như trước ADR này, `main-guard` thấy cây khác ngoài tài liệu và chạy `full`.
  - PR chậm hơn nhánh chính và còn đang chạy `pr-check` thì `merge-if-green.sh` không có `--update` trả mã 2 (chờ CI) thay vì mã 3, vì script chỉ biết PR có chạy `check.sh` không khi CI xong.
  - `main-guard` đỏ ở cách `tests` chỉ vì tổ hợp tài liệu thì theo `docs/auto-run.md` bước 9 hiện nay, phiên revert cả task code dù code không sai. Người xử lý `main-broken` loại này nên sửa bằng một PR tài liệu thay vì revert; đổi bước 9 cho phiên là việc của ticket khác.
  - Repo không bật "Require branches to be up to date before merging" (gói Free, ADR-0010); bật thì GitHub từ chối lệnh merge của đường này và script báo từ chối.
  - Kiểm tra `build-and-test` bị bỏ qua trên PR không nháp (lần chạy lúc PR còn nháp) là "đang chờ", không phải đỏ hay xanh.
  - PR nháp không có kết quả CI. Người muốn xem CI của PR nháp thì chuyển nó sang sẵn sàng.
  - Đổi `paths-ignore` của `main-guard.yml`, quy tắc tài liệu hay `GRADLE_READ_DOCS` của `main-guard-verified.sh`, tên job `build-and-test` hay tên bước cổng chất lượng thì đổi mọi nơi cùng PR; test của `scripts/tests/run.sh` giữ chúng khớp nhau.
- **Việc AI phải tuân thủ:** `merge-if-green.sh` chỉ merge nhánh chậm hơn nhánh chính mà không cập nhật khi PR đã chạy bước cổng chất lượng `check.sh` xanh, nhánh chính từ điểm tách chỉ đổi file trong `docs/**` hay `**/*.md` không phải file Gradle đọc, không file nào đổi ở cả hai bên và GitHub báo không xung đột; thiếu một điều hay không đọc được thì cập nhật nhánh như cũ; PR chỉ tài liệu luôn cập nhật nhánh trước khi merge; `merge-if-green.sh` quyết "chỉ tài liệu" và "PR đã chạy check.sh" chỉ qua `scripts/main-guard-verified.sh`, không viết lại quy tắc; đổi `paths-ignore` của `main-guard.yml` hay quy tắc tài liệu của `main-guard-verified.sh` thì sửa cả hai và test cùng PR; `pr-check.yml` khai báo đủ `opened`, `synchronize`, `reopened`, `ready_for_review` và chỉ bỏ PR nháp bằng `if:` cấp job; muốn CI cho PR nháp thì chuyển PR sang sẵn sàng, không bỏ điều kiện nháp
