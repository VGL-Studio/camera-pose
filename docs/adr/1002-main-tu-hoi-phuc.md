---
id: ADR-1002
title: Nhánh chính tự hồi phục khi đỏ: chạy lại test hỏng một lần, tự đóng issue khi chạy lại xanh, merge bản revert được kiểm bằng nội dung
status: accepted
origin: app
date: 2026-10-22
deciders: [Nhật, Tuấn]
supersedes: ADR-0010
superseded_by:
---

# ADR-1002: NHÁNH CHÍNH TỰ HỒI PHỤC KHI ĐỎ

ADR này chỉ thay mục 2 của ADR-0010 cho app Camera Pose, ở ba điểm nêu dưới đây. Mọi quy tắc khác của ADR-0010 vẫn áp dụng: merge chỉ qua `scripts/merge-if-green.sh`, `main-guard.yml` chạy cho từng commit, tìm commit gây lỗi bằng `scripts/find-breaking-commit.sh`, AI không đóng hay sửa issue `main-broken`.

## Bối cảnh

Phiên RUN-20261023-01 kết thúc khi mới xong 2 trên 7 task. Lý do là một screenshot test chập chờn (`CameraScreenScreenshotTest > starting_dark`) đỏ một lần trên `main-guard`, trong khi cùng code đó xanh ở PR, ở cục bộ và khi chạy lại (L-037, L-029). Nguyên nhân của test này đã được xác nhận (vòng chờ chạy mãi, Robolectric vẽ tới khi máy test hết bộ nhớ) và đã sửa ở RUN-20261023-02 (T-014, T-016 chụp với đồng hồ dừng); quyết định dưới đây vẫn cần, vì test chập chờn vì lý do khác vẫn có thể xuất hiện. Theo ADR-0010:

- Một lần đỏ mở issue `main-broken`, phiên dừng hẳn. Không ai sửa được cho tới sáng.
- Dev chạy lại `main-guard` trên đúng commit bị đổ lỗi, và lần chạy lại xanh. Dù vậy issue vẫn không tự đóng, vì quy tắc đóng chỉ nhận commit nằm *sau* commit bị đổ lỗi (L-036).
- Khi lỗi là thật và do chính phiên merge, phiên vẫn không được merge bản revert mà `main-guard` đã dựng sẵn. ADR-0010 có lý do đúng: tên nhánh `revert/` không phải bằng chứng tin cậy. Hệ quả là mọi task còn lại phải chờ người (L-038).

## Quyết định

**1. Chạy lại một lần khi chỉ có test thất bại.**
- Khi bước kiểm tra của `main-guard.yml` đỏ, `scripts/failure-kind.sh` đọc log. Nếu mọi task Gradle thất bại đều là task chạy test (`test...UnitTest`, `verifyRoborazzi...`) và lỗi là "There were failing tests", workflow chạy lại `scripts/check.sh` một lần trên cùng commit.
- Lần chạy lại xanh: commit được coi là xanh. Workflow mở issue nhãn `flaky`, hoặc bình luận vào issue `flaky` đang mở, ghi tên test và lần chạy, để sửa trong một ticket thường. Workflow không mở `main-broken`.
- Lần chạy lại vẫn đỏ, hoặc lỗi không phải test thất bại (biên dịch, lint, ktlint, quy tắc của `check.sh`): làm như ADR-0010.

**2. Lần chạy xanh trên chính commit bị đổ lỗi cũng đóng issue.** Bước đóng issue nhận cả trường hợp so sánh trả `identical`, tức người chạy lại `main-guard` trên đúng commit đó. Kết quả xanh của một commit cũ hơn, đến muộn (`behind`), vẫn không đóng issue.

**3. Bản revert đúng được merge khi `main-broken` đang mở.**
- `merge-if-green.sh` vẫn từ chối mọi PR khi `main-broken` mở, trừ một trường hợp. Script tự kiểm bằng `scripts/is-exact-revert.sh`, không dựa vào tên nhánh, rằng PR có đúng một commit, và cây file của commit đó trùng khớp với kết quả `git revert <commit bị đổ lỗi>` dựng trên nhánh chính mới nhất. Commit bị đổ lỗi lấy từ thân issue.
- Mọi điều kiện khác của cổng chất lượng vẫn áp dụng: CI xanh trên đúng commit, nhánh đã cập nhật theo nhánh chính.
- Issue vẫn chỉ được đóng bởi `main-guard` khi commit revert xanh (điểm 2 hoặc quy tắc cũ), không do AI.

**4. Phiên tự chạy.**
- `main-broken` mở mà commit bị đổ lỗi là commit merge của một task trong chính phiên đang chạy: phiên mở PR từ nhánh `revert/<sha>` có sẵn, chờ CI, merge bằng `merge-if-green.sh`, chuyển task đó sang `stopped` và chuẩn bị cho người như một task dừng. Phiên chờ `main-guard` của commit revert xanh rồi làm tiếp các task không phụ thuộc task bị revert.
- Commit bị đổ lỗi không phải của phiên, hoặc revert không merge được: phiên dừng như ADR-0010.
- Issue `flaky` đang mở được chép vào mục Cần người kiểm tra của báo cáo.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Ba điểm trên (đã chọn) | Phiên không mất cả đêm vì một test chập chờn hay vì một lần merge hỏng của chính nó; kiểm bằng nội dung nên không mở cửa cho PR bất kỳ | Thêm một lần chạy `check.sh` khi đỏ vì test (vài phút Actions); một lỗi thật nhưng chập chờn có thể lọt qua, được ghi vào issue `flaky` để sửa |
| Giữ ADR-0010 | Đơn giản, chặt | Mỗi lần đỏ, kể cả chập chờn, làm phiên dừng tới khi có người |
| Cho phép merge PR từ nhánh `revert/` | Dễ làm | Tên nhánh ai cũng đặt được, đúng điều ADR-0010 cảnh báo |
| Plugin chạy lại test của Gradle cho mọi lần chạy | Không đổi workflow | Đổi `build-logic/`, che test chập chờn ở cả PR, không để lại dấu vết |

## Hệ quả

- **Tích cực:** test chập chờn không làm dừng phiên nữa mà thành một issue để sửa; một lần merge hỏng của phiên được phiên tự gỡ, các task độc lập vẫn chạy; dev không phải đóng issue bằng tay sau khi chạy lại xanh.
- **Phải chấp nhận:** `main-guard` dài thêm một lần `check.sh` khi đỏ vì test; issue `flaky` phải được xem ở mỗi retro, nếu không test chập chờn sẽ tích lại. Đây là quyết định riêng của app; đã đề xuất về dự án mẫu (`docs/ops/from-app.md`).
- **Việc AI phải tuân thủ:** ở app này, `main-guard` chạy lại `check.sh` một lần khi chỉ có test thất bại và ghi issue `flaky` nếu lần chạy lại xanh; khi `main-broken` mở, chỉ merge PR mà `scripts/is-exact-revert.sh` xác nhận là bản revert đúng của commit bị đổ lỗi; phiên chỉ revert commit merge của chính phiên; AI vẫn không đóng, sửa hay bình luận để gỡ issue `main-broken`; các quy tắc khác của ADR-0010 vẫn áp dụng.
