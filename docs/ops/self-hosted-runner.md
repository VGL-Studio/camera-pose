# RUNNER TỰ HOST CHO JOB NẶNG

Tài liệu này dành cho người phụ trách hạ tầng: dựng một máy chạy runner tự host cho các job nặng của CI, chọn máy đó bằng biến `HEAVY_RUNNER` của repo, và quay về runner của GitHub khi máy hỏng (ADR-0042). AI không dựng máy, không đăng ký runner, không đặt hay xóa biến này.

## 1. Khi nào dùng

- Chỉ cho repo private. Repo công khai dùng runner của GitHub: vừa miễn phí, vừa không cho người lạ chạy code trên máy của nhóm qua PR từ fork. `scripts/runner-check.sh` báo lỗi khi repo không phải private.
- Job lên máy khi biến được đặt: `build-and-test` của `pr-check`, `guard` và `ui-test` của `main-guard`. Chúng khai báo `runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}`.
- Job giữ runner của GitHub: job đọc bí mật (`staging`, `device-session`, `env-canary`, `start-run`, `resume-run`) và job nhẹ (lịch định kỳ, `status-page`, `run-watchdog`).
- Hàng rào an toàn là cài đặt phía GitHub (mục 5) và container chạy một lần (mục 3, mục 4). `scripts/runner-check.sh` chỉ bắt cấu hình nhầm, vì một PR sửa được chính script đó.

## 2. Máy

- Linux x86_64 có KVM: máy chủ riêng, hay máy ảo có bật ảo hóa lồng. Emulator x86_64 của job `ui-test` cần KVM, nên Mac Apple Silicon không dùng được.
- Đề xuất: 8 nhân, 32 tới 64 GB RAM, 500 GB SSD, Ubuntu LTS, Docker. Hai container chạy cùng lúc; một `ui-test` dùng khoảng 4 nhân và 8 GB RAM.
- Luật udev trên máy chủ: `/dev/kvm` thuộc nhóm `kvm`, quyền `0660`. Ghi lại GID số của nhóm `kvm` trên máy chủ; container dùng số đó (mục 4).

## 3. Image của container

- Nền là image runner chính thức của GitHub (`ghcr.io/actions/actions-runner`).
- Người dùng của runner không có `sudo`. Image chính thức cho người dùng `runner` dùng `sudo` không cần mật khẩu, nên image của nhóm gỡ quyền đó.
- Android SDK với các gói mà `scripts/cloud-setup.sh` cài, cộng gói `emulator` và `system-images;android-34;google_apis;x86_64` cài sẵn. `ANDROID_HOME` trỏ tới SDK đó và thuộc người dùng của runner (chủ sở hữu), vì bước emulator gọi `sdkmanager` ghi vào đó.
- Công cụ mà workflow và script gọi: `git`, `gh`, `jq`, `python3`, `curl`, `unzip`.
- `ENV VGL_RUNNER_EPHEMERAL=1`: dấu cho biết container dựng theo tài liệu này. `scripts/runner-check.sh` đòi dấu này; dấu không bảo đảm runner chạy một lần.
- Không đặt sẵn gì trong `~/.gradle` (`init.d/`, `init.gradle`, `init.gradle.kts`, `gradle.properties`).

## 4. Dịch vụ tạo runner

- Một dịch vụ `systemd` chạy N bản (đề xuất 2) của cùng một vòng lặp:
  1. xin cấu hình runner chạy một lần (just-in-time): `POST /orgs/<tổ chức>/actions/runners/generate-jitconfig` với nhãn `vgl-android`, hay `POST /repos/<tổ chức>/<repo>/actions/runners/generate-jitconfig` khi đăng ký runner cho một repo;
  2. chạy `docker run --rm --network runners --device /dev/kvm --group-add <GID số của nhóm kvm trên máy chủ> --security-opt no-new-privileges --cap-drop ALL --cpus <n> --memory <m>` với cấu hình đó, không mount socket Docker;
  3. container thoát sau một job thì quay lại bước 1.
- Emulator: người dùng của runner trong container phải mở được `/dev/kvm` để đọc và ghi. `--device /dev/kvm` đưa thiết bị vào container, `--group-add` với đúng GID số của nhóm `kvm` trên máy chủ cho người dùng quyền đó (tên nhóm trong container có thể khác). Action emulator kiểm quyền này trước khi chạy và không dùng `sudo`; `scripts/runner-check.sh --emulator` cũng kiểm. `--cap-drop ALL` chưa thử với emulator: emulator cần thêm quyền nào thì chỉ thêm đúng quyền đó (`--cap-add`) và ghi lại ở đây.
- Mạng: container chạy trong mạng Docker riêng, không bật IPv6 (`docker network create --subnet <dải riêng> runners`, không có `--ipv6`; bật IPv6 thì cần các luật tương ứng bằng `ip6tables`), và không tới được máy chủ, mạng nội bộ hay địa chỉ dữ liệu máy (`169.254.169.254`). Ví dụ với iptables, chạy đúng thứ tự này (mỗi lệnh `-I` chèn lên đầu chuỗi, nên luật ACCEPT phải đứng trên luật DROP trong chuỗi, tức chạy sau):
  1. `iptables -I DOCKER-USER -s <dải riêng> -d 10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,169.254.0.0/16 -j DROP` (mạng nội bộ và địa chỉ dữ liệu máy);
  2. `iptables -I INPUT -s <dải riêng> -j DROP` (chính máy chủ, kể cả cổng của cầu Docker);
  3. chỉ khi máy chủ DNS nằm trong mạng nội bộ: `iptables -I DOCKER-USER -s <dải riêng> -d <IP máy DNS> -p udp --dport 53 -j ACCEPT` (và cùng luật với `-p tcp`), chạy sau lệnh 1 để đứng trên nó; máy DNS là chính máy chủ thì luật ACCEPT cổng 53 đặt ở chuỗi `INPUT`, chạy sau lệnh 2.

  Kiểm thứ tự bằng `iptables -L DOCKER-USER -n --line-numbers`. Lưu luật để còn sau khi khởi động lại.
- Token xin cấu hình: lưu trong tệp chỉ người dùng của dịch vụ đọc được, ngoài container; token không bao giờ vào container. Vòng lặp xin cấu hình cho mỗi job, nên token nằm trên máy suốt thời gian dịch vụ chạy.
  - Runner cấp tổ chức: fine-grained token chỉ có quyền "Self-hosted runners: write" của tổ chức. Đây là cách nên dùng.
  - Runner cấp repo (khi gói Free không giới hạn được nhóm runner): API cấp repo đòi quyền "Administration: write" của repo, rộng hơn quản lý runner (đổi cài đặt repo, kể cả cài đặt Actions). Đánh đổi chấp nhận khi không có cách khác: token fine-grained chỉ cho đúng repo đó, hạn ngắn (tối đa 90 ngày) và thay khi hết hạn, không quyền nào khác; máy chỉ người phụ trách đăng nhập được.

## 5. Cài đặt GitHub, bắt buộc trước khi đặt biến

1. Nhóm runner không cho repo công khai dùng runner: kiểm ở Organization settings, Actions, Runner groups và ghi lại kết quả. Gói Free không chỉnh được điều này thì đăng ký runner ở cấp repo.
2. Tắt chạy workflow từ fork của repo private (Settings, Actions, phần Fork pull request workflows, ở tổ chức và ở repo).
3. Rồi mới đặt biến ở từng repo app private: Settings, Secrets and variables, Actions, Variables; hay `gh variable set HEAVY_RUNNER --body vgl-android --repo <tổ chức>/<repo>`. Không dùng biến cấp tổ chức: gói Free không cho repo private đọc biến cấp tổ chức.

## 6. Kiểm sau khi dựng

Ghi lệnh, mã lần chạy và kết quả của từng bước vào nhật ký của plan đã dựng máy.

1. Runner nhận job: mở một PR code nhỏ (không nháp: PR nháp không chạy `pr-check`). `gh api "repos/<tổ chức>/<repo>/actions/runs/<id>/jobs" --jq '.jobs[] | {name, labels, runner_name}'` cho `build-and-test` có `labels` là `["vgl-android"]` và `runner_name` là tên container; log có dòng `RUNNER DÙNG ĐƯỢC`; job xanh. Merge PR đó: `guard` của commit merge kiểm như vậy.
2. Emulator: một commit đổi code app trên nhánh chính. `ui-test` chạy trên `vgl-android` sau `guard` và xanh; log có dòng `RUNNER DÙNG ĐƯỢC` của `--emulator`.
3. Cách ly giữa job: trên một PR thử không merge, thêm tạm vào `build-and-test` một bước ghi `~/.gradle/init.d/probe.gradle`. Rồi chạy một job nặng khác: bước kiểm runner của job sau vẫn in `RUNNER DÙNG ĐƯỢC` (container mới). Đóng PR thử.
4. Quay về runner của GitHub: `gh variable delete HEAVY_RUNNER --repo <tổ chức>/<repo>`, rồi `gh workflow run main-guard.yml --repo <tổ chức>/<repo> --ref main`. Job có `labels` là `["ubuntu-latest"]`, `runner_name` bắt đầu bằng `GitHub Actions`, bước kiểm runner bị bỏ qua, bước bật KVM chạy khi `ui-test` chạy. Đặt lại biến.
5. Máy tắt: dừng dịch vụ tạo runner, đẩy một PR code. `WAIT_RUN_RUNNER_QUEUE_MINUTES=5 scripts/wait-run.sh --pr <số>` trả mã 7 (`CHỜ RUNNER`); `python3 scripts/resume-check.py` ghi "đang chờ runner tự host" khi có danh sách chạy `running`. Dừng một container giữa job đang chạy và ghi lại dòng log của GitHub cho bảng tra lỗi. Rồi làm như mục 8, ghi lại lần chạy lại nhận nhãn mới hay giữ nhãn cũ.
6. Mạng và quyền, từ một container của dịch vụ (người dùng `runner` không phải root nên `CapEff` luôn 0, không chứng minh gì):
   - `grep CapBnd /proc/self/status` ra `0000000000000000` (có `--cap-drop ALL`; khác thì chỉ các quyền đã ghi ở mục 4);
   - `timeout 5 bash -c '</dev/tcp/<IP máy chủ trên cầu Docker>/22'` hết giờ (luật DROP), không phải bị từ chối hay kết nối được; dùng một cổng chắc chắn mở trên máy chủ (ở đây SSH), vì cổng đóng cũng thất bại khi chưa có luật;
   - cùng lệnh tới một máy trong mạng nội bộ có cổng mở: hết giờ;
   - `curl -m 5 http://169.254.169.254/` thất bại: chỉ có nghĩa khi nhà cung cấp máy có dịch vụ dữ liệu máy ở địa chỉ đó;
   - bước 2 vẫn xanh.
7. Sau vài ngày: bảng tính tiền của tổ chức cho thấy phút `Actions Linux` của repo giảm tương ứng với các job đã lên máy.

## 7. Vận hành

- Dọn image cũ (`docker image prune`), cập nhật image khi runner hay SDK đổi, xem đĩa trống.
- Token xin cấu hình có hạn (tối đa 90 ngày với runner cấp repo): đặt lịch thay token trước ngày hết hạn. Gặp mã 7 hay runner không nhận job thì xem token còn hạn trước, rồi mới xem máy.
- Job chờ máy: `scripts/wait-run.sh` trả mã 7 (`CHỜ RUNNER`) khi một job chờ runner tự host quá `WAIT_RUN_RUNNER_QUEUE_MINUTES` phút (mặc định 75) mà không còn job nào của lần chạy đang chạy. `scripts/resume-check.py` không tiếp tục phiên tự chạy khi repo còn job chờ runner tự host. Bước kiểm runner của `pr-check` đỏ thì bình luận lỗi trên PR có các dòng `RUNNER KHÔNG DÙNG ĐƯỢC:` (`scripts/runner-check.sh` ghi chúng vào `build.log`); bước đó của `main-guard` đỏ thì xem log của bước trên tab Actions.
- Issue `session-stalled` có sẵn lệnh tiếp tục phiên, và ghi sẵn job chờ runner tự host quá 75 phút, nếu có (`scripts/run-watchdog.py`). Job chờ dưới ngưỡng đó thì issue không ghi: trước khi dán lệnh, xem hàng chờ: `gh api "repos/<tổ chức>/<repo>/actions/runs?status=queued"`, rồi `jobs` của từng lần chạy. Có job mang nhãn tự host thì phiên cũ có thể vẫn đang chờ máy: sửa máy hay xóa biến trước, nếu không sẽ có hai phiên chồng nhau.

## 8. Máy hỏng

1. Xóa biến: `gh variable delete HEAVY_RUNNER --repo <tổ chức>/<repo>`.
2. Hủy lần chạy đang chờ rồi chạy lại: Re-run, `gh workflow run`, hay đẩy một commit mới khi lần chạy lại vẫn giữ nhãn cũ.
3. Sửa hay bật lại máy, kiểm theo mục 6, rồi đặt lại biến.

## 9. Ngoài phạm vi

- Runner cho bản production và `scripts/release.sh` (ADR-0009, ADR-0017).
- `staging`, `device-session`: đọc bí mật, giữ runner của GitHub.
