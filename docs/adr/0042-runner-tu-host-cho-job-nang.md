---
id: ADR-0042
title: Job nặng không đọc bí mật chạy được trên runner tự host chạy một lần, chọn bằng biến HEAVY_RUNNER của repo private; xóa biến thì quay về runner của GitHub
status: accepted             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-30
deciders: [Nhật]
supersedes:
superseded_by:
---

# ADR-0042: JOB NẶNG KHÔNG ĐỌC BÍ MẬT CHẠY ĐƯỢC TRÊN RUNNER TỰ HOST CHẠY MỘT LẦN, CHỌN BẰNG BIẾN HEAVY_RUNNER CỦA REPO PRIVATE; XÓA BIẾN THÌ QUAY VỀ RUNNER CỦA GITHUB

Nguồn: yêu cầu giảm chi phí GitHub Actions (`docs/features/quy-trinh/requests/2026-10-29-giam-phut-actions.md`, cách 1), ticket T-078, plan `docs/features/quy-trinh/plans/T-078-runner-tu-host.md`.

## Bối cảnh

Tổ chức VGL-Studio dùng gói GitHub Free. Tháng 10 năm 2026, tổ chức vượt 2.000 phút miễn phí và trả tiền cho phần vượt. Sau T-074 tới T-077, phần lớn số phút còn lại là của các job chạy Gradle hay emulator:

- `build-and-test` của `pr-check`;
- `guard` và `ui-test` của `main-guard` (`ui-test` có `needs: guard`, do T-075 thêm);
- `device-session`;
- `staging`.

Những điều đã tra trong tài liệu GitHub ngày 2026-10-30 (nguồn ở mục 8 của plan T-078):

- Runner tự host không tính phút. GitHub công bố ngày 2025-12-16 phí nền tảng 0,002 USD mỗi phút cho runner tự host, dự kiến từ 2026-03-01. Phí này đã bị hoãn, chưa có ngày mới.
- Runner chuẩn của GitHub miễn phí cho repo công khai.
- Ở gói Free, biến và bí mật cấp tổ chức không dùng được cho repo private. Biến chọn runner vì vậy phải đặt ở từng repo.
- Chỉ gói Team trở lên tạo được nhóm runner (runner group) thêm. Theo mặc định, nhóm runner chỉ cho repo private dùng.
- GitHub khuyên chỉ dùng runner tự host cho repo private. Runner chạy nhiều job có thể bị code của một job làm hỏng lâu dài. Runner chạy một lần (just-in-time, ephemeral) thì mỗi máy ảo hay container chỉ nhận một job.

Runner tự host chạy lâu thì dùng chung một máy cho code của PR và cho job có quyền ghi. Một PR có thể để lại init script trong `~/.gradle/init.d`. Init script đó chạy trong mọi lần build Gradle sau, kể cả lần build của `main-guard` có `contents: write`, hay của `staging` có khóa ký. Bản thân `scripts/cloud-setup.sh` đã đặt init script vào `~/.gradle`, nên đây là cơ chế có thật.

Với sự kiện `pull_request`, GitHub đọc workflow và script từ code của PR. Vì vậy một kiểm tra nằm trong repo không chặn được PR cố ý: PR sửa được chính kiểm tra đó, hay ghi thẳng nhãn runner vào `runs-on`.

## Quyết định

**1. Chỉ job nặng không đọc bí mật mới được chạy trên runner tự host.** Job nặng là job chạy `scripts/check.sh`, Gradle hay emulator: `build-and-test`, `guard`, `ui-test`. Các job này khai báo:

`runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}`

Biến `HEAVY_RUNNER` của repo (Settings, Secrets and variables, Actions, Variables) là nhãn của runner tự host, ví dụ `vgl-android`. Không đặt biến thì job chạy trên runner của GitHub như trước. Máy hỏng hay bảo trì thì người xóa biến, không cần sửa workflow.

- Job có dùng `secrets.` giữ `runs-on: ubuntu-latest`: `staging`, `device-session`, `env-canary`, `start-run`, `resume-run`.
- Job nhẹ cũng giữ `runs-on: ubuntu-latest`: lịch định kỳ, `status-page`, các job chỉ gọi API.
- Runner cho bản production và `scripts/release.sh` nằm ngoài quyết định này (ADR-0009, ADR-0017).

**2. Hàng rào an toàn nằm ở GitHub và ở máy, không nằm trong repo.**

- Phía GitHub:
  - nhóm runner không cho repo công khai dùng runner, hay dùng runner đăng ký ở cấp repo;
  - tắt chạy workflow từ fork của repo private.
- Phía máy: runner chạy một lần, mỗi job một container mới. Máy chạy một dịch vụ lặp lại ba bước: xin cấu hình runner just-in-time từ GitHub, chạy container mới với cấu hình đó, xóa container khi job xong.
  - Container không mount socket Docker và không có `sudo`.
  - Quyền dùng `/dev/kvm` do máy chủ cấp (luật udev, `--group-add`).
  - Token xin cấu hình không vào container.
  - Mỗi job có `~/.gradle` và thư mục làm việc mới. Không job nào để lại gì cho job sau.
- Cách dựng máy và cài đặt GitHub ghi ở `docs/ops/self-hosted-runner.md`. Người kiểm cài đặt nhóm runner trước khi đặt biến lần đầu.

**3. Job nặng kiểm cấu hình runner trước khi build.**

- Mục đích: bắt cấu hình nhầm, không phải hàng rào an toàn (mục 2).
- Vị trí: bước đứng ngay sau `actions/checkout` của mỗi job nặng là `scripts/runner-check.sh` (`--emulator` cho `ui-test`), chỉ chạy khi `runner.environment == 'self-hosted'`.
- Các bước đứng trước bước kiểm, ví dụ bước lọc đường dẫn trước checkout, không ghi vào `~/.gradle`. Trong thư mục làm việc, chúng chỉ được ghi `build.log`, trên đường lỗi, cho bước bình luận lỗi đọc.
- Script báo lỗi (job đỏ, thông báo `RUNNER KHÔNG DÙNG ĐƯỢC:`) khi:
  - repo không phải private;
  - container không có dấu `VGL_RUNNER_EPHEMERAL=1`. Dấu này chỉ cho biết container dựng theo tài liệu, không bảo đảm runner chạy một lần;
  - `~/.gradle` có `init.d/`, `init.gradle`, `init.gradle.kts` hay `gradle.properties` từ trước;
  - thư mục làm việc có file ngoài git, kể cả file bị bỏ qua như `build/`;
  - đĩa trống dưới mức tối thiểu;
  - với `--emulator`: không mở được `/dev/kvm` để đọc và ghi, hay `ANDROID_HOME` không ghi được.

**4. Bước giả định máy sạch của GitHub chỉ chạy trên máy của GitHub.** Bước dùng `sudo` (giải phóng đĩa, bật KVM) có `if: runner.environment == 'github-hosted'`. `actions/setup-java` và `gradle/actions/setup-gradle` chạy trên cả hai loại máy.

**5. Job chờ runner tự host là việc của người, không phải lỗi của code, và không làm phiên chạy lại.**

- `scripts/wait-run.sh` có mã thoát riêng 7 (`CHỜ RUNNER`). Mã 4 giữ nghĩa cũ: lần chạy đã xong mà GitHub không cấp máy.
- Mã 7 ra khi đủ bốn điều:
  - không job nào của lần chạy đang chạy;
  - một job đang chờ (`queued`), chưa có runner;
  - job đó mang nhãn không phải của GitHub;
  - job đó được phép chạy từ quá `WAIT_RUN_RUNNER_QUEUE_MINUTES` phút (mặc định 75, lớn hơn thời gian tối đa của `ui-test`).
- "Được phép chạy" tính từ lúc job cuối mà nó chờ (`needs`) xong.
- Phiên nhận mã 7 thì không chạy lại, ghi "Cần người kiểm tra" và dừng như `docs/auto-run.md` mục 4.
- `scripts/resume-check.py` không mở lại phiên khi repo có bất kỳ job nào đang chờ (`queued`) mang nhãn không phải của GitHub, không cần ngưỡng thời gian: phiên cũ có thể vẫn đang chờ chính job đó.
- Người bật lại máy hay xóa biến, rồi hủy và chạy lại lần chạy đó.

**6. Không đặt `HEAVY_RUNNER` cho repo công khai.** Repo công khai dùng runner của GitHub, vừa miễn phí vừa không cho người lạ chạy code trên máy của nhóm qua fork.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Runner chạy một lần trong container, chỉ cho job không đọc bí mật, chọn bằng biến của repo (đã chọn) | Không job nào để lại gì cho job sau; quay về runner của GitHub bằng cách xóa một biến; job có bí mật không bao giờ lên máy của nhóm | Người phải dựng dịch vụ tạo container và giữ token tạo runner trên máy; `staging` và `device-session` (khoảng 13% số phút) vẫn tính phút |
| Runner chạy lâu, dọn `~/.gradle` và thư mục làm việc sau mỗi job | Dựng đơn giản | GitHub nói không bảo đảm được runner chỉ chạy một job; dọn sau job không chặn được tiến trình còn chạy nền hay file ngoài các thư mục đã dọn |
| Tách hai runner: một cho PR, một cho job có quyền ghi và bí mật | Đưa được cả `staging` lên máy của nhóm | Bí mật staging nằm trên máy của nhóm; hai dịch vụ phải giữ; vẫn cần runner chạy một lần cho phần PR |
| Dựa vào kiểm tra trong repo (`runner-check.sh`) làm hàng rào | Không cần cài đặt phía GitHub | PR sửa được chính kiểm tra đó; chỉ bắt được cấu hình nhầm |
| Biến cấp tổ chức | Đặt một lần cho mọi app | Gói Free không cho repo private đọc biến cấp tổ chức |
| Ghi thẳng nhãn runner vào workflow | Đơn giản | Máy hỏng thì phải sửa workflow ở mọi app, mà sửa workflow là plan rủi ro cao |

## Hệ quả

- **Tích cực:**
  - Job nặng của repo private không còn tính phút khi máy chạy.
  - Máy hỏng thì xóa một biến là CI chạy lại như cũ.
  - Workflow giống nhau ở mọi app; app chỉ khác ở biến.
- **Phải chấp nhận:**
  - Máy, điện, mạng, dịch vụ tạo container và cài đặt nhóm runner là việc của người. Máy tắt mà biến còn thì job chờ tới khi người xử lý, và phiên tự chạy dừng (mã 7).
  - Token xin cấu hình runner nằm trên máy. Token chỉ có quyền quản lý runner, không có quyền đọc code hay bí mật.
  - Máy có ít chỗ chạy và dùng chung nhiều repo. Một job có thể chờ lâu chính đáng; mã 7 có thể báo sớm khi máy bận lâu hơn ngưỡng.
  - Ở cách `skip` (ADR-0039), `guard` vẫn giữ một chỗ trên máy khoảng 1 phút.
  - `staging`, `device-session` và các job nhẹ vẫn tính phút.
  - Kết quả trên runner tự host có thể khác runner của GitHub (phiên bản SDK, CPU, emulator). Lỗi chỉ có trên một loại máy thì so bằng cách xóa biến rồi chạy lại.
- **Việc AI phải tuân thủ:** chỉ job chạy `scripts/check.sh`, Gradle hay emulator mà không đọc `secrets.` mới khai báo `runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}`; job có `secrets.` và job nhẹ giữ `runs-on: ubuntu-latest`; job nặng gọi `scripts/runner-check.sh` ở bước ngay sau `actions/checkout`, với `if: runner.environment == 'self-hosted'`, trước `setup-java`, `setup-gradle` và mọi bước build; bước đứng trước bước kiểm runner không ghi vào `~/.gradle`, và trong thư mục làm việc chỉ ghi `build.log` trên đường lỗi; không coi `runner-check.sh` là hàng rào an toàn, nó chỉ bắt cấu hình nhầm; bước dùng `sudo` trong job nặng có `if: runner.environment == 'github-hosted'`; không đặt `clean: false` cho `actions/checkout` của job nặng; `wait-run.sh` mã 7 (`CHỜ RUNNER`) không phải lỗi của code: không sửa code, không chạy lại, ghi "Cần người kiểm tra"; không cài, đăng ký hay đổi cấu hình runner, không đặt hay xóa biến `HEAVY_RUNNER` trong phiên; không đề xuất đặt `HEAVY_RUNNER` cho repo công khai
