---
id: ADR-0025
title: Nhánh chính tự hồi phục khi đỏ: chạy lại test hỏng một lần, tự đóng issue khi chạy lại xanh, merge bản revert được kiểm bằng nội dung
status: accepted
origin: template           # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-23
deciders: [Nhóm phát triển]
supersedes: ADR-0010         # chỉ thay một phần mục 2 của ADR-0010, ở bốn điểm dưới đây; ADR-0010 vẫn còn hiệu lực
superseded_by:
---

# ADR-0025: NHÁNH CHÍNH TỰ HỒI PHỤC KHI ĐỎ

ADR này chỉ thay một phần mục 2 của ADR-0010, ở bốn điểm nêu dưới đây. Mọi quy tắc khác của ADR-0010 vẫn áp dụng: merge chỉ qua `scripts/merge-if-green.sh`, `main-guard.yml` chạy cho từng commit, tìm commit gây lỗi bằng `scripts/find-breaking-commit.sh`, AI không đóng, sửa hay bình luận để gỡ issue `main-broken`.

## Bối cảnh

Ở app camera-pose, một phiên tự chạy kết thúc khi mới xong 2 trên 7 task (L-047, L-048, L-049; issue #78 của dự án mẫu). Lý do là một screenshot test chập chờn đỏ một lần trên `main-guard`, trong khi cùng code đó xanh ở PR, ở cục bộ và khi chạy lại. Nguyên nhân của test đó đã được tìm ra và sửa (vòng chờ chạy mãi; `CLAUDE.md` bước 10 ghi cách chụp với đồng hồ dừng), nhưng test chập chờn vì lý do khác vẫn có thể xuất hiện. App đã làm quyết định này thành ADR-1002 của app (PR VGL-Studio/camera-pose#40, #47); ADR này đưa nó về dự án mẫu. Theo ADR-0010:

- Một lần đỏ mở issue `main-broken`, phiên dừng hẳn. Không ai sửa được cho tới sáng.
- Dev chạy lại `main-guard` trên đúng commit bị đổ lỗi và lần chạy lại xanh, nhưng issue không tự đóng: quy tắc đóng chỉ nhận commit nằm *sau* commit bị đổ lỗi (so sánh trả `ahead`), cùng một commit thì trả `identical` (L-047).
- Khi lỗi là thật và do chính phiên merge, phiên vẫn không được merge bản revert mà `main-guard` đã dựng sẵn. ADR-0010 có lý do đúng: tên nhánh `revert/` không phải bằng chứng tin cậy. Hệ quả là mọi task còn lại phải chờ người (L-049).

## Quyết định

**1. Chạy lại một lần khi chỉ có test thất bại.**
- Khi bước kiểm tra của `main-guard.yml` đỏ, `scripts/failure-kind.sh` đọc log. Nếu mọi task Gradle thất bại đều là task chạy test (`test...UnitTest`, `verifyRoborazzi...`) và đều thất bại vì "There were failing tests", workflow chạy lại `scripts/check.sh` một lần trên cùng commit.
- Lần chạy lại xanh: commit được coi là xanh. Workflow mở issue nhãn `flaky`, hoặc bình luận vào issue `flaky` đang mở, ghi tên test và lần chạy, để sửa trong một ticket thường. Workflow không mở `main-broken`.
- Lần chạy lại vẫn đỏ, hoặc lỗi không phải test thất bại (biên dịch, lint, ktlint, quy tắc của `check.sh`, test của script): làm như ADR-0010.

**2. Lần chạy xanh trên chính commit bị đổ lỗi cũng đóng issue.** Bước đóng issue nhận cả trường hợp so sánh trả `identical`, tức người chạy lại `main-guard` trên đúng commit đó. Kết quả xanh của một commit cũ hơn, đến muộn (`behind`), hay của một commit không liên quan (`diverged`), vẫn không đóng issue.

**3. Bản revert đúng được merge khi `main-broken` đang mở.**
- `merge-if-green.sh` vẫn từ chối mọi PR khi `main-broken` mở, trừ một trường hợp: đúng một issue `main-broken` đang mở, thân issue ghi commit gây lỗi (dòng `<!-- culprit:<sha> -->` do `main-guard.yml` ghi), và `scripts/is-exact-revert.sh` xác nhận chỉ bằng `git`, không dựa vào tên nhánh, rằng PR có đúng một commit nằm ngay trên nhánh chính mới nhất, và cây file của commit đó trùng khớp với kết quả `git revert <commit gây lỗi>` dựng trên nhánh chính mới nhất.
- Mọi điều kiện khác của cổng chất lượng vẫn áp dụng: CI xanh trên đúng commit, nhánh đã cập nhật theo nhánh chính, PR không nháp, không mang nhãn `needs-human`. Bản revert không được cập nhật bằng `--update`: commit gộp nhánh chính làm nó không còn là đúng một commit revert, nên bị từ chối.
- Bản revert đã kiểm đúng cũng qua được kiểm tra "phiên đang chạy" của `merge-if-green.sh`, để phiên merge được nó (điểm 4).
- Issue vẫn chỉ được đóng bởi `main-guard` khi commit revert xanh (điểm 2 hoặc quy tắc cũ), không do AI.

**4. Phiên tự chạy.**
- `main-broken` mở mà commit gây lỗi là commit merge của một task trong chính phiên đang chạy: phiên mở PR từ nhánh `revert/<sha>` có sẵn, chờ CI, merge bằng `merge-if-green.sh`, chuyển task đó sang `stopped` và chuẩn bị cho người như một task dừng. Phiên chờ `main-guard` của commit revert xanh rồi làm tiếp các task không phụ thuộc task bị revert.
- Commit gây lỗi không phải của phiên, không có nhánh `revert/`, hoặc bản revert không merge được: phiên dừng như ADR-0010.
- Issue `flaky` đang mở được chép vào mục Cần người kiểm tra của báo cáo.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Bốn điểm trên (đã chọn) | Phiên không mất cả đêm vì một test chập chờn hay vì một lần merge hỏng của chính nó; kiểm bằng nội dung nên không mở cửa cho PR bất kỳ | Thêm một lần chạy `check.sh` khi đỏ vì test (vài phút Actions); một lỗi thật nhưng chập chờn có thể lọt qua, được ghi vào issue `flaky` để sửa |
| Giữ ADR-0010 | Đơn giản, chặt | Mỗi lần đỏ, kể cả chập chờn, làm phiên dừng tới khi có người |
| Cho phép merge PR từ nhánh `revert/` | Dễ làm | Tên nhánh ai cũng đặt được, đúng điều ADR-0010 cảnh báo |
| Plugin chạy lại test của Gradle cho mọi lần chạy | Không đổi workflow | Đổi `build-logic/`, che test chập chờn ở cả PR, không để lại dấu vết |

## Hệ quả

- **Tích cực:** test chập chờn không làm dừng phiên nữa mà thành một issue để sửa; một lần merge hỏng của phiên được phiên tự gỡ, các task độc lập vẫn chạy; dev không phải đóng issue bằng tay sau khi chạy lại xanh.
- **Phải chấp nhận:** `main-guard` dài gấp đôi khi đỏ vì test; issue `flaky` phải được xem ở mỗi retro, nếu không test chập chờn sẽ tích lại. `merge-if-green.sh` cần `git fetch` được commit của PR (`refs/pull/<số>/head`, không được thì theo tên nhánh) khi `main-broken` mở; chưa thử trong phiên `claude --cloud`.
- **Việc AI phải tuân thủ:** `main-guard` chạy lại `check.sh` một lần khi chỉ có test thất bại và ghi issue `flaky` nếu lần chạy lại xanh; khi `main-broken` mở, chỉ merge PR mà `scripts/is-exact-revert.sh` xác nhận là bản revert đúng của commit gây lỗi; không sửa hay cập nhật nhánh `revert/` trước khi merge; phiên chỉ revert commit merge của chính phiên, theo `docs/auto-run.md` mục Khi nhánh chính đỏ vì task của phiên; AI vẫn không đóng, sửa hay bình luận để gỡ issue `main-broken`; chép issue `flaky` đang mở vào báo cáo phiên; các quy tắc khác của ADR-0010 vẫn áp dụng.
