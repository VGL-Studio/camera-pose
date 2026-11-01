---
id: ADR-0039
title: main-guard không chạy lại cổng chất lượng cho commit có nội dung đã được pr-check kiểm đầy đủ, chỉ chạy test của script khi phần khác chỉ là tài liệu
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-29
deciders: [Nhật]
supersedes: ADR-0010         # chỉ thay một phần mục 2 của ADR-0010 ("chạy lại cổng chất lượng cho từng commit") và một phần ADR-0025 mục 1 (không ghi flaky ở cách skip); hai ADR đó vẫn còn hiệu lực
superseded_by:
---

# ADR-0039: MAIN-GUARD KHÔNG CHẠY LẠI CỔNG CHẤT LƯỢNG CHO COMMIT CÓ NỘI DUNG ĐÃ ĐƯỢC PR-CHECK KIỂM ĐẦY ĐỦ, CHỈ CHẠY TEST CỦA SCRIPT KHI PHẦN KHÁC CHỈ LÀ TÀI LIỆU

Nguồn: yêu cầu giảm chi phí GitHub Actions (`docs/features/quy-trinh/requests/2026-10-29-giam-phut-actions.md`), ticket T-074.

## Bối cảnh

ADR-0010 mục 2: `main-guard.yml` chạy lại `scripts/check.sh` cho mọi commit trên nhánh chính, trừ commit chỉ đổi tài liệu. Từ 2026-10-22 tới 2026-10-29 việc này tốn 757 phút Actions của dự án mẫu, khoảng 21% số phút, trong khi gói Free chỉ có 2.000 phút mỗi tháng cho cả tổ chức và tổ chức đã trả tiền cho phần vượt.

`pr-check` kiểm cây file của `refs/pull/N/merge`, tức nhánh của PR ghép với nhánh chính lúc kiểm. `scripts/merge-if-green.sh` (ADR-0010 mục 1) chỉ merge khi nhánh của PR không chậm commit nào so với nhánh chính (`behind_by == 0`) và `pr-check` xanh trên đúng commit cuối, rồi merge kiểu squash kèm mã commit đó. Khi nhánh đã chứa nhánh chính, cây được kiểm, cây của commit cuối và cây của commit merge là một. Chạy lại `check.sh` trên cùng một cây file chỉ bắt được test chập chờn (ADR-0025), không bắt được lỗi mới.

Nhưng "`pr-check` xanh" không phải lúc nào cũng có nghĩa đã chạy `check.sh`. PR không đổi code chỉ chạy `scripts/tests/run.sh` (L-040). Và PR sửa `pr-check.yml` được kiểm bằng chính bản đã sửa.

## Quyết định

**1. Ba cách kiểm.** Job `guard` của `main-guard.yml` gọi `scripts/main-guard-verified.sh` trước khi kiểm. Script chọn một trong ba cách:

- `skip`: không chạy `check.sh`, job xanh, log ghi "đã kiểm ở PR #N, commit <sha>". Chọn cách này khi đủ ba điều:
  - commit là commit merge của một PR vào nhánh chính;
  - lần `pr-check` mới nhất trên commit cuối của PR xanh, và bước "Cổng chất lượng (scripts/check.sh)" của job `build-and-test` có kết quả `success`, tức PR đã chạy cổng đầy đủ;
  - cây file của commit trên nhánh chính giống hệt cây file của commit cuối của PR.
- `tests`: chỉ chạy `scripts/tests/run.sh`. Chọn cách này khi đủ hai điều đầu, và mọi file khác nhau giữa hai cây đều nằm trong `docs/**` hay là `**/*.md`, trừ các file trong `docs/` mà Gradle đọc khi chạy test (`docs/remote-config-keys.md`, `docs/remote-config-template.json`, `docs/design-system/tokens.json`). Hiện nay trường hợp này hiếm: nhánh chính có thêm commit tài liệu trong khoảng giữa lần `merge-if-green.sh` kiểm `behind_by` và lệnh merge squash, hay PR được merge bằng cách khác. Nó thành đường thường khi T-077 cho PR code merge mà không cập nhật nhánh lúc nhánh chính chỉ đổi tài liệu. `run.sh` kiểm cả tài liệu (số bài học, `foundation.md`, danh sách chạy, quét bí mật), và tài liệu của PR ghép với tài liệu mới của nhánh chính chưa được kiểm ở đâu khác.
- `full`: chạy `check.sh` như trước. Đây là cách cho mọi trường hợp khác, kể cả khi script không đọc được GitHub hay không lấy được commit đã kiểm.

**2. Luôn chạy đầy đủ khi commit đổi chính cổng kiểm, khi chạy tay và khi chạy lại.**

- Commit có thay đổi so với commit cha ở một file mà job `guard` gọi hay dựa vào luôn dùng `full`. Danh sách gồm `.github/workflows/pr-check.yml`, `.github/workflows/main-guard.yml`, `scripts/check.sh`, `scripts/failure-kind.sh`, `scripts/find-breaking-commit.sh`, `scripts/main-guard-verified.sh`, `scripts/tests/run.sh` và `scripts/tests/fake-gh`. Lần chạy sau khi merge là lần đầu cổng mới chạy thật trên nhánh chính.
- Chạy tay (`workflow_dispatch`) dùng `full`.
- Lần chạy lại của cùng một lần chạy (`GITHUB_RUN_ATTEMPT` lớn hơn 1) dùng `full`. Người bấm chạy lại để xem commit có thật xanh không, nhất là để đóng issue `main-broken` (ADR-0025 mục 2); lần đó phải chạy `check.sh` thật.

**3. Kết quả vẫn là check run `guard`.** Cách `skip` là một bước bên trong job `guard`, không phải điều kiện `if:` ở cấp job, để check run `guard` có kết luận `success`. Các nơi đọc kết quả `main-guard` không đổi:

- `scripts/find-breaking-commit.sh` và `scripts/main-guard-status.sh` đọc check run `guard`;
- `start-run.yml` gọi `main-guard-status.sh`;
- `scripts/release.sh` chỉ build bản production từ commit có `main-guard.yml` kết luận `success`.

Commit xanh theo cách `skip` vì vậy cũng mở cổng cho bản production. Người duyệt ADR này chấp nhận nghĩa mới của "xanh": cây file đã qua `check.sh` đầy đủ ở PR, không nhất thiết chạy lại sau merge.

**4. Đỏ ở cách `tests` là nhánh chính đỏ**, như đỏ ở cách `full`: tìm commit gây lỗi, mở issue `main-broken`, đẩy nhánh revert (ADR-0010, ADR-0025).

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Tin lần kiểm đầy đủ của PR khi cây file giống hệt (đã chọn) | Bớt khoảng 6 phút cho mỗi lần merge code; giữ check run `guard` và mọi nơi đọc nó; không đổi `merge-if-green.sh` | Mất lần chạy lại bắt test chập chờn trên nhánh chính; thêm một script phải giữ đúng |
| Giữ như cũ | Không đổi gì | Khoảng 21% số phút Actions cho một lần kiểm trùng |
| Bỏ hẳn `main-guard`, chỉ dựa vào `merge-if-green.sh` | Rẻ nhất | Mất lớp bắt commit đẩy thẳng lên main, PR sửa cổng, và `main-broken` (ADR-0010 mục 2, ADR-0025) |
| `if:` ở cấp job để bỏ qua | Không tốn runner | Check run `guard` có kết luận `skipped`: `find-breaking-commit.sh` chờ như còn đang chạy, `main-guard-status.sh` báo "CHƯA KIỂM", phiên tự chạy không bắt đầu được |

## Hệ quả

- **Tích cực:**
  - Lần merge code của một PR đã kiểm tốn khoảng 1 phút `main-guard` thay vì 5 tới 12 phút.
  - Mọi nơi đọc kết quả `main-guard` giữ nguyên, kể cả `release.sh`.
- **Phải chấp nhận:**
  - Test chập chờn chỉ được ghi `flaky` khi `main-guard` chạy `full` (ADR-0025 mục 1). Ở cách `skip`, test chập chờn lộ ra ở `pr-check` của PR.
  - Commit xanh theo cách `skip` mở cổng phát hành mà không chạy lại `check.sh` sau merge.
  - Script so cây của commit cuối của PR, không so cây `refs/pull/N/merge` mà `pr-check` đã kiểm. Hai cây chỉ khác khi lúc kiểm nhánh của PR còn chậm hơn nhánh chính; nhánh chính chỉ đi tới, nên lúc merge nhánh vẫn chậm và `merge-if-green.sh` từ chối. Merge bằng cách khác `merge-if-green.sh` (cấm, ADR-0010 mục 1) thì giả định này không còn.
  - Danh sách file `docs/` mà Gradle đọc chỉ được kiểm qua `rootProject.file("docs/...")` của các `*.gradle.kts`. Một test Kotlin đọc thẳng file `docs/` bằng đường dẫn tương đối sẽ không được nhận ra; hiện chưa có test nào như vậy.
  - Đổi tên bước "Cổng chất lượng (scripts/check.sh)" hay tên job `build-and-test` của `pr-check.yml`, hay thêm file trong `docs/` mà Gradle đọc, thì phải sửa `main-guard-verified.sh` theo. Test của script giữ các file khớp nhau.
- **Việc AI phải tuân thủ:** `main-guard.yml` chỉ bỏ qua `check.sh` qua `scripts/main-guard-verified.sh`, bằng một bước bên trong job `guard`, không bằng `if:` ở cấp job; script không chắc thì chọn `full`; commit đổi một file mà job `guard` gọi hay dựa vào, lần chạy tay và lần chạy lại luôn dùng `full`; đổi tên bước cổng chất lượng hay tên job `build-and-test` của `pr-check.yml`, hay thêm file trong `docs/` mà Gradle đọc, thì sửa `main-guard-verified.sh` và test của nó cùng PR
