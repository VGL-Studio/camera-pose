---
ticket: T-036
title: Diễn tập đầu phiên trong đúng môi trường cloud
status: draft                # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.0M         # cỡ S, số gợi ý của scripts/token-usage.py estimate (tạm, chưa đủ 5 task)
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-036: DIỄN TẬP ĐẦU PHIÊN TRONG ĐÚNG MÔI TRƯỜNG CLOUD

## 1. Tóm tắt

`scripts/cloud-env-check.sh` đang chỉ so phiên bản setup script. Plan này cho nó kiểm thêm, trong vài giây đầu phiên, ba điều mà `scripts/check.sh` sẽ cần:

- Java chạy được Gradle của dự án (`scripts/java-check.sh`);
- Gradle wrapper khởi động được (`./gradlew --version`, có tải bản phân phối qua proxy);
- Android SDK có đúng nền tảng `compileSdk`.

Mỗi điều sai là một dòng lỗi có lý do và việc người cần làm, nên phiên kết thúc ngay ở kiểm tra đầu phiên thay vì sau khoảng 15 phút `check.sh` (L-049, L-056). Test với `java` và `gradlew` giả, viết trước, thất bại trên code hiện tại.

**Phiên bản 2** sửa theo rà soát độc lập trước RUN-20261024-01:

- Hàm `fake_java` của `scripts/tests/run.sh` được định nghĩa ở khối `java-check` (dòng 1034), sau khối `cloud-env-check.sh` (dòng 516). Test mới gọi nó sớm hơn sẽ gặp `command not found` và `set -e` dừng cả `run.sh`, tức thất bại sai lý do. Phiên bản 2 chuyển định nghĩa `fake_java` lên trước khối `cloud-env-check.sh`, trong commit test của bước 3 (mục 3.2).
- Ticket ghi đúng câu trả lời đã duyệt ở phiên bản 1 (bỏ nhánh thử, `./gradlew --version`).
- Ghi rủi ro cho phiên tiếp tục và việc chạy thử trên máy cloud ở bước 5 (mục 6, mục 7).
- T-041 và T-038 làm trong phiên có người theo dõi trước phiên, không còn trong danh sách chạy (mục 6).

## 2. Hiểu yêu cầu

Ticket không có spec nghiệp vụ; yêu cầu là bài học L-056, L-051, L-002 (`docs/lessons.md`).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| L-056 | Khi Java của môi trường cloud không chạy được Gradle của dự án thì `cloud-env-check.sh` báo lỗi ngay, trước `check.sh` | Gọi `scripts/java-check.sh` (đã đọc đúng dòng `Picked up`, T-035); thoát khác 0 thì thêm một dòng lỗi kèm phần thông báo của `java-check.sh` |
| L-056 | Khi Gradle wrapper không khởi động được (tải bản phân phối bị chặn, JVM lỗi vì `JAVA_TOOL_OPTIONS`) thì báo lỗi ngay | `./gradlew --version -q` (không chạy task nào của dự án, không đụng bộ nhớ đệm build); thoát khác 0 thì báo lỗi với 5 dòng cuối của đầu ra |
| L-056 | Khi SDK thiếu nền tảng của `compileSdk` thì báo lỗi ngay | Đọc `compileSdk` trong `gradle/libs.versions.toml` (chỉ đọc), kiểm `$ANDROID_HOME/platforms/android-<số>` (hoặc `$HOME/android-sdk` khi chưa đặt `ANDROID_HOME`, như `env-canary.sh`) |
| L-051 | Nhánh task không bị kẹt trên máy cloud vì lệnh đẩy | Không làm ở đây (phiên bản 1, mục 8, câu 1, đã duyệt; ticket đã ghi): lần từ chối của L-051 đến từ bộ phân loại lệnh của phiên, không đến từ git; lệnh đẩy chạy bên trong script không gặp bộ phân loại đó, nên không phát hiện được. Quyền đẩy của git đã được kiểm khi phiên đẩy nhánh báo cáo ở đầu mục 2 của `auto-run.md`. T-037 lo phần nhánh task |
| L-002 | Bí mật staging là file giả | Ngoài phạm vi theo ticket: `preflight.sh` đã kiểm |
| Hành vi cũ | Phiên bản setup script cũ, thiếu init script mirror: vẫn từ chối như nay | Giữ nguyên hai kiểm tra đầu và thông báo của chúng |

Thiết kế tham chiếu: Không có thiết kế riêng (script, docs/ops/design-workflow.md).

ADR liên quan: ADR-0010 (cổng chất lượng), ADR-0016 (phiên tự chạy bằng routine).

## 3. Giải pháp

### 3.1. Cách làm

Trong `scripts/cloud-env-check.sh`, sau hai kiểm tra hiện có:

1. Đặt `ANDROID_HOME` từ `$HOME/android-sdk` khi chưa có và thư mục tồn tại. Chép đúng đoạn của `env-canary.sh` (issue #37): routine chạy script từ shell không đọc `~/.bashrc`.
2. `JAVA_CHECK_CMD` (mặc định `scripts/java-check.sh`): thoát khác 0 thì thêm vấn đề "Java của môi trường không chạy được Gradle của dự án: <dòng `LỖI:` của java-check>".
3. `GRADLEW_CMD` (mặc định `./gradlew`) chạy `--version -q`, có giới hạn thời gian `GRADLEW_TIMEOUT` (mặc định 300 giây, dùng `timeout` nếu có). Thoát khác 0 thì thêm vấn đề "Gradle wrapper không khởi động được", kèm 5 dòng cuối.
4. `compileSdk` đọc bằng `sed` từ `gradle/libs.versions.toml`. Thiếu thư mục `platforms/android-<số>` thì thêm vấn đề "Android SDK thiếu platforms;android-<số>".
5. Hiện script in một câu chung "chưa dùng setup script mới nhất". Plan tách thông báo theo từng vấn đề, và việc người cần làm theo loại:
   - setup script cũ hay thiếu SDK: dán lại `cloud-setup.sh`, như nay;
   - Java hay Gradle hỏng: xem biến môi trường của môi trường cloud (`JAVA_TOOL_OPTIONS`, proxy) và `scripts/cloud-setup.sh`, kèm thông báo gốc.

   Thoát 1 khi có bất kỳ vấn đề nào, như nay.

`env-canary.sh` đang ghi lý do "Môi trường cloud chưa dùng `scripts/cloud-setup.sh` mới nhất" cho mọi lần `cloud-env-check.sh` đỏ. Plan sửa thành "`scripts/cloud-env-check.sh` báo lỗi môi trường cloud", vì giờ còn các lý do khác; log đã kèm thông báo chi tiết.

`docs/auto-run.md`, mục 2, chỉ sửa dòng "Môi trường cloud dựng từ `scripts/cloud-setup.sh` mới nhất" của bảng: nêu các kiểm tra mới; cột xử lý giữ "không chờ, không thử lại, kết thúc phiên, chép thông báo vào báo cáo". Lý do: Java hay Gradle hỏng ở bước này thì `check.sh` cũng hỏng, không tự khỏi.

Kiểm tra lúc lập plan (2026-10-24, máy dev, JDK 21 trong scratchpad):

- `./gradlew --version -q` thoát 0 sau khoảng 2 giây, khi bản phân phối đã tải sẵn.
- `JAVA_TOOL_OPTIONS="-Dx=1" scripts/java-check.sh` thoát 0 sau T-035.

Thời gian trên cloud với bộ nhớ đệm trống (phải tải bản phân phối Gradle khoảng 130 MB) chưa đo (mục 8, câu 2).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/cloud-env-check.sh` | Ba kiểm tra mới, `ANDROID_HOME` mặc định, thông báo theo từng vấn đề (mục 3.1); biến `JAVA_CHECK_CMD`, `GRADLEW_CMD`, `GRADLEW_TIMEOUT` để test thay; chú thích đầu file ghi rõ đây là cổng của kiểm tra đầu phiên |
| Sửa | `scripts/env-canary.sh` | Chỉ câu lý do khi `ENV_CHECK_CMD` đỏ (mục 3.1) |
| Sửa | `scripts/tests/run.sh` | Chuyển nguyên định nghĩa hàm `fake_java` (6 dòng, hiện ở khối `java-check`, dòng 1034) lên ngay trước khối `cloud-env-check.sh` (dòng 516), không đổi thân hàm; khối `java-check` vẫn gọi nó như cũ (`$work/bin` đã có từ dòng 8). Hàm `env_check` hiện có nhận thêm `JAVA_CHECK_CMD`, `GRADLEW_CMD`, `ANDROID_HOME` (mặc định là bản giả đều qua, để ba test cũ giữ nguyên ý); bảy test mới ở mục 4, đặt ngay sau ba test `cloud-env-check.sh` hiện có. Việc chuyển `fake_java`, hàm phụ trợ và test đều vào commit test của bước 3 (L-025): chạy `run.sh` sau commit đó thì test mới thất bại vì `cloud-env-check.sh` chưa kiểm, không vì thiếu hàm. Chỉ đụng khối test `cloud-env-check.sh`, định nghĩa `fake_java` và một dòng của khối `env-canary` (mục 6) |
| Sửa | `docs/auto-run.md` | Chỉ dòng `cloud-env-check.sh` của bảng mục 2 (mục 3.1) |
| Sửa | `docs/ops/staging-and-runs.md` | Mục 2.1, bước 2 (đoạn "Kiểm tra nhanh sau khi dán"): nhắc các kiểm tra mới |
| Sửa | `docs/ops/setup-new-app.md` | Bảng xử lý sự cố (dòng `cloud-env-check.sh`): thêm hai dòng cho Java, Gradle |
| Sửa | `docs/lessons.md` | L-056: dòng Áp dụng ghi T-036 khi merge |
| Sửa | `docs/features/quality-gate/tickets/T-036-dien-tap-dau-phien.md` | Trạng thái theo phiên |

Nơi khác đọc `cloud-env-check.sh` (`grep -rn cloud-env-check scripts/ .github/ docs/ops`):

- `scripts/env-canary.sh` gọi nó, có sửa ở trên;
- `scripts/cloud-setup.sh` chỉ nhắc trong chú thích, không đổi;
- `scripts/tests/run.sh`, có sửa ở trên.

Không workflow nào gọi trực tiếp. `env-canary.sh` có test trong `run.sh`; các test đó thay `ENV_CHECK_CMD` bằng script giả, nên không bị kiểm tra mới ảnh hưởng. Riêng test "old setup script is red" chỉ so lệnh ghi lên GitHub, không so câu lý do, nên giữ nguyên.

`gradle/libs.versions.toml` chỉ được đọc, không sửa.

Nơi khác nhắc tới file tài liệu và test mà plan sửa, không bị ảnh hưởng (L-020):

- Các workflow chỉ nhắc `docs/auto-run.md` hay `docs/ops/staging-and-runs.md` trong chú thích, để dẫn người đọc tới mục tương ứng: `.github/workflows/device-session.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/preflight.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/staging.yml`, `.github/workflows/start-run.yml`, `.github/workflows/env-canary.yml`. Plan không đổi tên hay số mục nào.
- `.github/workflows/env-canary.yml` kích hoạt routine chạy `scripts/env-canary.sh`, không đọc đầu ra của script; plan chỉ đổi một câu lý do trong thân issue. `.github/workflows/preflight.yml` chỉ nhắc `scripts/env-canary.sh` trong chú thích (nhãn issue riêng).
- `scripts/merge-if-green.sh`, `scripts/wait-run.sh`, `scripts/resume-check.py`, `scripts/status.py` nhắc `docs/auto-run.md` (và `scripts/tests/run.sh`, nơi có test của chúng) trong chú thích hay thông báo theo số mục. Plan chỉ sửa một dòng của bảng mục 2, không đổi số mục, và không đụng khối test của các script đó.
- `scripts/status.py` đọc `docs/lessons.md` để đếm bài học theo dòng Trạng thái. Plan chỉ thêm chữ vào dòng Áp dụng của L-056.
- `scripts/preflight.sh` nhắc `docs/ops/setup-new-app.md` trong thông báo. Plan chỉ thêm dòng vào bảng xử lý sự cố, không đổi số mục.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `./gradlew --version` trên cloud lần đầu phải tải bản phân phối Gradle, có thể lâu hơn giới hạn 300 giây.
- **Điều kiện chuyển:** test thủ công ở mục 4 (phiên đầu tiên sau khi merge) cho thấy bước này quá giới hạn, trong khi `check.sh` sau đó vẫn qua.
- **Cách làm thay:** không phải trong phiên này. Ghi vào báo cáo, và người tăng `GRADLEW_TIMEOUT` bằng một plan sửa. Phiên không tự đổi giới hạn.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Hành vi cũ giữ nguyên | Test script có sẵn | `cloud-env-check.sh passes on the current version`, `stops on an older version`, `stops without the mirror init script`: qua với Java, Gradle, SDK giả đều qua |
| L-056, Java qua | Test script | `cloud-env-check.sh passes with Java 21 after a Picked up line`: `JAVA_CHECK_CMD` là `java-check.sh` thật với `JAVA_CMD` trỏ `fake_java 'openjdk version "21.0.12.1"' 'Picked up JAVA_TOOL_OPTIONS: -Dx=1'`; thoát 0. Đối chứng cho test dưới |
| L-056, Java sai | Test script | `cloud-env-check.sh stops on Java 26 and names Java`: `fake_java 'openjdk version "26"'`; thoát 1, đầu ra có `Java 26`. Thất bại trên code hiện tại (thoát 0: script chưa gọi `java-check.sh`) |
| L-056, Gradle wrapper hỏng | Test script | `cloud-env-check.sh stops when the Gradle wrapper fails`: `GRADLEW_CMD` là script giả in `Could not install Gradle distribution` và thoát 1; thoát 1, đầu ra có `Gradle wrapper` và dòng của script giả. Thất bại trên code hiện tại |
| L-056, Gradle wrapper treo | Test script | `cloud-env-check.sh stops when the Gradle wrapper hangs`: script giả `sleep 5`, `GRADLEW_TIMEOUT=1`; thoát 1 trong dưới 4 giây. Bỏ qua (in `skip`) khi máy test không có `timeout` |
| L-056, SDK thiếu nền tảng | Test script | `cloud-env-check.sh stops without the compileSdk platform`: `ANDROID_HOME` là thư mục rỗng; thoát 1, đầu ra có `platforms;android-36` (số đọc từ `libs.versions.toml` thật). Đối chứng: thêm `platforms/android-36` thì thoát 0 (`cloud-env-check.sh passes with the compileSdk platform`) |
| Thông báo theo loại | Test script | `cloud-env-check.sh tells to paste the setup script only for setup problems`: phiên bản cũ thì đầu ra có `dán lại toàn bộ scripts/cloud-setup.sh`; chỉ Java sai thì không có câu đó mà có `JAVA_TOOL_OPTIONS` |
| `env-canary.sh` ghi lý do mới | Test script có sẵn, thêm một kiểm | `env-canary: old setup script is red` giữ nguyên lệnh ghi. Thêm `env-canary: env check red names cloud-env-check.sh`: thân issue (fake-gh ghi lại) có `cloud-env-check.sh báo lỗi` |
| Chạy được trên cloud thật | Kiểm tra ở phiên sau | Báo cáo của danh sách chạy kế tiếp, mục Kiểm tra đầu phiên: `cloud-env-check.sh` in đủ bốn dòng qua, kèm thời gian của bước Gradle |

**Ngoại lệ viết test trước:** Không có.

### 4.1. Chạy thử

Không cần (E dưới 2): các lệnh đều đã dùng trong dự án (`java-check.sh`, `gradlew`, `sed` trên `libs.versions.toml`).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI, cổng chất lượng): kiểm tra đầu phiên quyết định phiên có chạy task nào không, và `env-canary.sh` mở issue `env-broken` chặn `start-run.yml` dựa trên script này.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Hai script, một dòng tài liệu ở ba file |
| B. Độ rõ của yêu cầu | 1 | Rõ cho Java, Gradle, SDK; phần đẩy nhánh thử lệch ticket (mục 8, câu 1) |
| C. Khả năng kiểm chứng tự động | 1 | Test với bản giả; thời gian thật trên cloud chỉ kiểm ở phiên sau |
| D. Ảnh hưởng chéo | 2 | Mọi phiên và mọi lần `env-canary`; báo sai thì phiên không chạy task nào |
| E. Độ mới của kỹ thuật | 0 | Lệnh đã dùng trong dự án |
| **Tổng** | 5 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `scripts/cloud-env-check.sh`, `scripts/env-canary.sh`;
  - `scripts/tests/run.sh`: chỉ khối `cloud-env-check.sh` và một kiểm trong khối `env-canary`;
  - `docs/auto-run.md`: chỉ dòng `cloud-env-check.sh` của mục 2.
- Task khác có thể xung đột:
  - T-037, T-039, T-040 cùng sửa `docs/auto-run.md` trong danh sách chạy: T-037 các bước 3, 4, 7; T-039 bước 8; T-040 bước 9. T-041 (dòng "Nhánh chính đang xanh" của mục 2) và T-038 (bước 8) làm trong phiên có người theo dõi trước phiên, nên đã có trên nhánh chính khi task này chạy. Không plan nào khác sửa dòng `cloud-env-check.sh`.
  - T-039 và T-042 cũng thêm khối test vào `scripts/tests/run.sh`; không plan nào khác dùng hay chuyển `fake_java`.
  - Mỗi plan thêm khối test riêng vào `scripts/tests/run.sh`; plan này không sửa khối của plan khác.
  - Chỉ xung đột dòng khi gộp. Task chạy sau gộp nhánh chính trước khi đẩy.
- **Rủi ro cho phiên tiếp tục:** phiên tiếp tục sau khi hết hạn mức làm lại mục 2 của `auto-run.md` (`auto-run.md`, mục 4, Tiếp tục phiên), nên sẽ chạy bản `cloud-env-check.sh` mới nếu task này đã merge. Bản mới báo sai thì phiên tiếp tục kết thúc mà không làm task còn lại. Vì vậy bước 5 chạy bản mới ngay trên máy cloud của phiên (mục 7).
- Đề xuất thứ tự chạy:
  - Đứng đầu danh sách chạy có nó. Kiểm tra mới chỉ có tác dụng từ phiên sau.
  - Nếu cloud có lỗi Java hay Gradle thì `check.sh` nền của chính phiên đó đã dừng phiên trước khi tới task, như RUN-20261023-03. Vì vậy làm trong phiên có người theo dõi cũng được (mục 8, câu 3).

## 7. Điều kiện dừng riêng

- Bước 5: chạy `scripts/cloud-env-check.sh` của nhánh task ngay trên máy cloud của phiên (ghi thời gian của bước Gradle vào nhật ký). Bản mới báo lỗi trong khi `check.sh` nền của đầu phiên qua (kiểm báo sai): dừng task, không merge, ghi cả hai đầu ra vào nhật ký.

## 8. Câu hỏi cho người duyệt

1. **Nhánh thử (ticket ghi "đẩy và xóa nhánh `claude/probe-<mã phiên>`").** Lần từ chối của L-051 do bộ phân loại lệnh của phiên Claude Code chặn lệnh `git push` mà phiên con gõ, không phải do git. Một lệnh đẩy nằm trong script thì bộ phân loại chỉ thấy lệnh `scripts/cloud-env-check.sh`, nên không tái hiện được lần từ chối đó. Quyền đẩy của git đã được kiểm ở đầu mục 2 (phiên đẩy nhánh báo cáo). Đề xuất của AI: bỏ phần này khỏi T-036, để T-037 xử lý hậu quả (đẩy sớm, giữ bundle khi bị từ chối). Ghi vào ticket khi duyệt.
2. **Giới hạn 300 giây cho `./gradlew --version`.** Chưa đo được trên cloud với bộ nhớ đệm trống. Đề xuất của AI: 300 giây (lâu hơn nhiều mức tải bình thường, ngắn hơn một lần `check.sh`). Phiên đầu ghi thời gian thật vào báo cáo, sửa sau nếu cần (mục 3.4).
3. **`env-canary`.** Phiên bản 1 ghi routine chưa chạy được vì thiếu bí mật. Ngày 2026-10-24, Tuấn đã tạo routine và hai bí mật; lần chạy 40061910624 kích hoạt được routine. Không còn câu hỏi.
4. **Làm ở đâu.** Đề xuất của AI: đưa vào danh sách chạy kế tiếp, đứng đầu. Task chỉ đổi script và tài liệu, không cần máy hay bí mật.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1: Duyệt (Nhật, duyệt chéo Tuấn, 2026-10-24); ghi chú: Tuấn chấp nhận mọi đề xuất của AI ở mục 8. Thay bằng phiên bản 2 vì rà soát độc lập trước RUN-20261024-01 tìm hàm `fake_java` được gọi trước chỗ định nghĩa (test thất bại sai lý do) và ticket chưa ghi câu trả lời đã duyệt (mục 1).

Phiên bản 2:

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <ghi chú, nếu có>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
