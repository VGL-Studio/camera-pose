# CÁCH MỞ PHIÊN LÀM ĐƯỢC GÌ

Phiên tự chạy mở theo một trong ba cách. Mỗi cách chạy trong một môi trường khác, với token GitHub, mạng và công cụ khác. Plan cần điều mà cách mở phiên không có thì task dừng giữa chừng, thường sau khi đã làm xong việc. Ví dụ ở app camera-pose: token của phiên `claude --cloud` không có quyền `workflow`, task sửa workflow làm xong mà không đẩy được (L-112). Tài liệu này để người lập danh sách chạy chọn cách mở trước khi duyệt (`docs/CLAUDE.md`, mục Khi lập danh sách chạy).

| Cách mở (`open_with`) | Là gì |
| --- | --- |
| `cloud` | `claude --cloud` từ terminal của dev, dùng cloud session credits. Không tự tiếp tục khi hết hạn mức (L-044) |
| `routine` | `start-run.yml` hay Run now của routine (`docs/auto-run.md`, mục 1). `resume-run.yml` tự tiếp tục khi hết hạn mức |
| `local` | Claude Code chạy trên máy của dev, trong thư mục repo, dùng git và `gh` của máy đó. Kiểm môi trường đầu phiên bằng `scripts/local-setup.sh --check`, không bằng `cloud-env-check.sh` (`docs/auto-run.md`, mục 2; L-116). Issue `env-broken` nói về môi trường cloud nên không chặn phiên `local`; phiên ghi nó vào báo cáo (L-139). Đây là cách mở nên dùng khi môi trường cloud đang lỗi |

Danh sách chạy ghi cách mở ở dòng `open_with:` (`docs/templates/run.md`). `scripts/run-check.py` (quy tắc 8) đọc bảng dưới. Plan có đường dẫn ở mục 3.2 khớp cột Mẫu của một khả năng mà cách mở ghi `không` là lỗi. Ghi `thử` là chú ý, và phiên thử khả năng đó ở đầu phiên (`docs/auto-run.md`, mục 2). Script chỉ tính các hàng của bảng file ở mục 3.2 (như `scripts/plan-check.py --touches`), không tính chữ. Dòng có Mẫu `-` là để người đọc, script không kiểm. Mẫu viết sai thì script báo chú ý và bỏ qua dòng đó. Giá trị: `có`, `không`, `thử`.

Phiên biết cách mở của mình từ lời giao mở phiên (prompt của `claude --cloud` ghi "mở bằng claude --cloud"; routine và `start-run.yml` có lời giao riêng; local là phiên chạy trên máy của dev) và ghi vào mục Kiểm tra đầu phiên. Phiên tiếp tục (sau khi hết hạn mức, hay người mở lại) chạy lại `scripts/push-probe.sh` khi cần, vì môi trường mới có thể dùng token khác. Mỗi lần thử dùng một nhánh mới nên không vướng nhánh của lần trước.

<!-- capabilities:start -->
| Khả năng | Mẫu đường dẫn ở mục 3.2 | cloud | routine | local | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `workflow-push` | `^\.github/workflows/` | thử | thử | thử | Đẩy commit có đổi file workflow cần token có quyền `workflow`. Phiên cloud và routine dùng kết nối GitHub của tài khoản Claude: tài khoản này đẩy được, tài khoản khác thì không (L-112), nên phải thử bằng `scripts/push-probe.sh`. Local đẩy được khi git của máy dùng SSH, hay `gh auth status` có quyền `workflow`; máy không chắc thì cũng thử. Kết quả thử ở camera-pose: tài khoản của Nhật đẩy được (PR #179), của Tuấn ở RUN-20261027-01 thì không (camera-pose L-086); giữ `thử` tới khi `push-probe.sh` qua hai phiên liền sau khi Tuấn cấp lại quyền |
| `cross-repo-read` | - | không | không | có | Đọc repo khác của tổ chức (ví dụ app đọc dự án mẫu). Phiên cloud chỉ có quyền với repo của chính nó (L-101): plan dán nguyên đoạn code cần chép vào mục 3.1 |
| `r2-network` | - | không | không | có | Gọi R2 hay máy chủ ngoài bằng `curl`: proxy của cloud trả 403 (L-050). Kiểm bằng workflow trên GitHub |
| `emulator` | - | không | không | thử | Cloud không có emulator; test trên emulator và `device-session` chạy trong CI. Local có khi máy có emulator hay máy thật nối `adb` |
| `repo-secrets` | - | không | không | không | Bí mật của repo chỉ có trong GitHub Actions. Việc cần bí mật làm bằng workflow; người tạo bí mật trước phiên |
| `auto-resume` | - | không | có | không | Tự tiếp tục khi hết hạn mức (`resume-run.yml`; lịch được `status-page.yml` bật khi danh sách được duyệt, T-051). Cách khác: người mở lại với dòng "phiên tiếp tục" |
| `workflow-enable` | - | thử | thử | có | Bật lịch của `resume-run.yml` và `run-watchdog.yml` bằng `scripts/schedule-idle.py --enable` ở đầu phiên (`docs/auto-run.md`, mục 2; T-051). `GITHUB_TOKEN` và `gh` của máy dev bật được; token của phiên cloud và routine chưa thử. Không được thì ghi vào Cần người kiểm tra, người bật tay. Kết quả thử: phiên đầu tiên sau T-051 ghi vào đây |
<!-- capabilities:end -->

Ô `thử` của `workflow-push` tùy tài khoản Claude và token mà mỗi app dùng, nên là giá trị riêng của app (`docs/ops/to-app.md`, mục 6). Khi một lần thử cho kết quả chắc chắn (ví dụ `scripts/push-probe.sh` qua ở hai phiên liên tiếp), người của app đổi ô `thử` thành `có` hay `không`, ghi ngày và lần chạy ở cột Ghi chú. Dự án mẫu giữ `thử`.

Thêm một khả năng mới: thêm một dòng, Mẫu là biểu thức chính quy kiểu Python trên đường dẫn của mục 3.2, và một test trong `scripts/tests/run.sh` (khối `run-check`).
