---
ticket: T-030
title: Kiểm tra môi trường trước phiên trên GitHub (bí mật staging, file R2, DNS) và phiên bản Java của check.sh
status: approved                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 20M          # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [ci, process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                    # người duyệt
cross_reviewer: Tuấn              # bắt buộc khi risk_level là high
approved_at: 2026-10-22
---

# PLAN T-030: KIỂM TRA MÔI TRƯỜNG TRƯỚC PHIÊN TRÊN GITHUB VÀ PHIÊN BẢN JAVA CỦA CHECK.SH

## 1. Tóm tắt

Một script mới, `scripts/preflight.sh`, kiểm bốn điều mà trước đây chỉ lộ ra khi phiên đã chạy:
- bí mật Firebase staging là file thật và có app `.staging`;
- nhóm người test của App Distribution đã có;
- tên miền file tĩnh phân giải được qua 1.1.1.1 và 8.8.8.8;
- mọi file R2 bắt buộc trả `200`.

Script chạy trên GitHub, vì môi trường `claude --cloud` không gọi được R2 (L-005). Workflow mới `preflight.yml` chạy script mỗi sáng và mở issue `setup-broken` khi có lỗi. `start-run.yml` chạy cùng script và từ chối bắt đầu phiên khi có lỗi. `scripts/check.sh` gọi `scripts/java-check.sh` trước Gradle.

Kiểm chứng bằng test của script trong `scripts/tests/run.sh` (lệnh `dig`, `curl`, `java`, `firebase` giả). Phần workflow kiểm bằng tay sau khi merge.

Plan này chạy trong phiên có người theo dõi, không đưa vào danh sách chạy: nó đổi cách bắt đầu phiên.

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là các bài học `accepted` L-002, L-011, L-012, L-022 và phần DNS của L-027. L-016 chỉ được nêu ở câu hỏi 4. Mục tiêu chung: phiên tự chạy không bao giờ dừng giữa chừng vì một lỗi môi trường mà lẽ ra đã tìm được trước khi phiên bắt đầu.

| Bài học | Tiêu chí | Cách hiểu của AI |
| --- | --- | --- |
| L-002, L-012 | Khi bí mật `STAGING_GOOGLE_SERVICES_JSON` là file giả, hoặc không có client cho gói `com.vglstudio.camera.pose.line.guide.staging`, thì không bắt đầu phiên và có issue báo | Cùng hai điều kiện mà bước "Đưa cấu hình staging vào" của `staging.yml` đã kiểm: `project_info.project_id` khác `fake-staging-project`, và `.client[]` có `package_name` đúng. File giả hiện có trong repo cũng có client `.staging`, nên phải kiểm cả hai điều (mục 4.1). Chỉ in "đúng" hay "sai" kèm tên điều kiện, không in `project_id` hay bất kỳ giá trị nào |
| L-012 | Khi nhóm người test trong `STAGING_TESTER_GROUPS` (mặc định `team`) chưa có trong App Distribution thì cũng báo trước phiên | Liệt kê nhóm bằng `firebase-tools` với tài khoản dịch vụ `STAGING_APP_DISTRIBUTION_CREDENTIALS`. Lệnh chưa chạy thử (mục 3.4) |
| L-011 | Khi một file R2 bắt buộc chưa có thì báo trước phiên | Danh sách trong `scripts/required-assets.txt`, mỗi dòng một đường dẫn tương đối dưới địa chỉ gốc staging. `curl -I` mỗi file phải ra `200`. Địa chỉ gốc lấy từ dòng `ASSETS_BASE_URL` của bản staging trong `app/build.gradle.kts`, để không có hai nơi ghi cùng một giá trị |
| L-027 | Khi tên miền file tĩnh không phân giải được qua 1.1.1.1 hoặc 8.8.8.8 thì báo, dù DNS của máy chạy vẫn phân giải được | `dig +short @<máy chủ> <tên miền> A` phải ra ít nhất một địa chỉ ở cả hai máy chủ |
| L-022 | Khi Java của máy không hợp với Gradle 8.14.3 thì `check.sh` dừng trước Gradle, nói rõ phiên bản và gợi ý `JAVA_HOME` | Chấp nhận Java 17 tới 24 (Gradle 8.14.3 chạy trên 17 tới 24; CI dùng 17). Thiếu `java` cũng dừng với gợi ý |
| L-016 | Xung đột `docs/STATUS.md` | Không làm trong plan này; câu hỏi 4 |

Thiết kế tham chiếu: không có thiết kế riêng.

ADR liên quan: ADR-0009 (bí mật chỉ người thêm, AI không đọc hay in), ADR-0010, ADR-0012 (file Firebase giả trong repo), ADR-0015 (đường dẫn R2), ADR-0016 (routine và `start-run.yml`).

## 3. Giải pháp

### 3.1. Cách làm

**`scripts/preflight.sh`**. Không tham số; mọi đầu vào qua biến môi trường. Mã thoát: 0 khi qua, 1 khi có ít nhất một điều sai. Mỗi điều in một dòng `QUA: ...`, `LỖI: ...` hoặc `CHÚ Ý: ...` bằng tiếng Việt. Có `set -euo pipefail`, và mỗi kiểm tra chạy riêng để một lỗi không che lỗi khác.
- `GOOGLE_SERVICES_FILE`: đường dẫn tới file mà workflow ghi từ bí mật (`umask 077`, trong `$RUNNER_TEMP`). Rỗng hoặc không có file: `LỖI: thiếu bí mật STAGING_GOOGLE_SERVICES_JSON`.
  - `jq -e .` không đọc được: `LỖI: bí mật không phải JSON`.
  - `jq -e '.project_info.project_id == "fake-staging-project"'`: `LỖI: ... đang là file cấu hình giả`.
  - Không có client với `package_name == $STAGING_APPLICATION_ID`: `LỖI: ... không có app <gói>`. Tên gói không phải bí mật, được in.
  - `jq` chạy với `-e` và kết quả chỉ dùng làm điều kiện, không bao giờ in ra.
- `TESTER_GROUPS` (mặc định `team`) và `DISTRIBUTION_CREDENTIALS_FILE`: khi có file, chạy `${FIREBASE_CMD:-npx --yes firebase-tools@15.32.1} appdistribution:groups:list --project <project_number> --json`, rồi so tên nhóm (`alias`) với từng nhóm cần có.
  - `project_number` đọc từ `project_info.project_number` của file Firebase. Đây là số định danh, không phải khóa, nhưng vẫn không in.
  - Lệnh không chạy được (thiếu `npx`, lệnh không có ở phiên bản này, lỗi quyền): `CHÚ Ý: không kiểm được nhóm người test`, không tính là lỗi (mục 3.4).
  - Thiếu nhóm: `LỖI: App Distribution chưa có nhóm <tên>`.
- `ASSETS_BASE_URL`: mặc định đọc từ `app/build.gradle.kts` bằng `sed` (dòng `buildConfigField("String", "ASSETS_BASE_URL", "\"https://...\"")` đầu tiên, của bản staging). Tên miền là phần giữa `https://` và `/` tiếp theo.
- `DNS_SERVERS` (mặc định `1.1.1.1 8.8.8.8`): với mỗi máy chủ, `${DIG_CMD:-dig} +short +time=5 +tries=2 @<máy chủ> <tên miền> A`. Không có dòng nào là địa chỉ IPv4: `LỖI: <tên miền> không phân giải được qua <máy chủ> (L-027, docs/ops/setup-new-app.md mục 3)`.
- `REQUIRED_ASSETS` (mặc định `scripts/required-assets.txt`): bỏ dòng trống và dòng bắt đầu bằng `#`. Với mỗi đường dẫn, `${CURL_CMD:-curl} -sS -o /dev/null -I --max-time 20 -w '%{http_code}' <gốc><đường dẫn>`. Khác `200`: `LỖI: <đường dẫn> trả <mã>`. Mã `000` (không kết nối được) cũng là lỗi; thử lại một lần sau 5 giây (`PREFLIGHT_RETRY_SECONDS`) trước khi kết luận, để một lần mạng chập chờn không chặn phiên.
- Cuối: tóm tắt số điều qua và lỗi; trong GitHub Actions thì ghi thêm vào `$GITHUB_STEP_SUMMARY`.
- `DIG_CMD`, `CURL_CMD`, `FIREBASE_CMD`, `PREFLIGHT_RETRY_SECONDS` chỉ để test thay lệnh thật.

**`scripts/required-assets.txt`**: chú thích ở đầu file nói ai thêm dòng (plan thêm file R2 bắt buộc mới thì thêm vào đây) và dòng đầu tiên: `poses/catalog/v1/catalog-587b5394.json` (`DEFAULT_CATALOG_PATH` của `:core:poses`). Có kiểm cả ảnh của bộ khởi đầu ghi trong catalog không: câu hỏi 3.

**`.github/workflows/preflight.yml`** (mới):
- `on: schedule` lúc `45 0 * * *` (07:45 giờ Việt Nam, trước `env-canary` 01:00 UTC), và `workflow_dispatch` với một input `extra_asset` (mặc định rỗng): một đường dẫn thêm vào danh sách cho lần chạy đó, chỉ để người kiểm đường đỏ của workflow (mục 4).
- `permissions: contents: read, issues: write`.
- Một job `ubuntu-latest`, `timeout-minutes: 10`: `actions/checkout@v5`, `actions/setup-node@v4` (cho `npx`), rồi bước "Kiểm tra môi trường trước phiên":
  - ghi hai bí mật vào `$RUNNER_TEMP` với `umask 077`, gọi `scripts/preflight.sh`, xóa hai file ở `trap`;
  - `continue-on-error: true` và `id: preflight`.
- Bước "Ghi kết quả":
  - đỏ: `gh label create setup-broken --force`; có issue `setup-broken` đang mở thì bình luận, không thì tạo issue "setup-broken: môi trường chưa sẵn sàng cho phiên tự chạy" với kết quả của script (chỉ các dòng `LỖI`, `CHÚ Ý`, `QUA`, không có giá trị bí mật), link lần chạy, và câu "`start-run.yml` từ chối bắt đầu phiên khi issue này mở";
  - xanh: đóng mọi issue `setup-broken` đang mở, kèm bình luận.
- Bước cuối `exit 1` khi `steps.preflight.outcome == 'failure'`, để lần chạy hiện đỏ.
- Nhãn riêng `setup-broken`, không dùng `env-broken`: `scripts/env-canary.sh` đóng mọi issue `env-broken` khi môi trường cloud xanh, nên sẽ đóng nhầm issue của workflow này.

**`.github/workflows/start-run.yml`**: trong bước "Kiểm tra trước khi bắt đầu phiên", sau đoạn kiểm `env-broken`:
- Thêm đoạn kiểm issue `setup-broken` đang mở, cùng cách với `env-broken`: `LỖI` và `exit 1` kèm câu "Sửa theo issue, chạy lại workflow preflight".
- Thêm bước mới "Kiểm tra môi trường staging" trước bước "Kích hoạt routine": `actions/setup-node@v4`, rồi chạy `scripts/preflight.sh` với hai bí mật như `preflight.yml`. Đỏ thì `exit 1` và phiên không bắt đầu. Bước này chỉ đọc, không mở issue (`permissions` của `start-run.yml` giữ `issues: read`).
- `timeout-minutes` từ 5 lên 10 (`npx firebase-tools` tải gói lần đầu).

Các kiểm tra hiện có của `start-run.yml` và bước mới đi qua từng kiểm tra ra sao (L-044):

| Kiểm tra hiện có | Thứ tự | Ảnh hưởng của thay đổi |
| --- | --- | --- |
| `if: github.ref == 'refs/heads/main'` | 1 | Giữ nguyên; bước mới nằm trong cùng job |
| Đường dẫn danh sách chạy đúng dạng, có trên nhánh chính, trạng thái `approved` | 2 | Giữ nguyên, chạy trước bước mới |
| Không có issue `main-broken` đang mở | 3 | Giữ nguyên |
| Không có issue `env-broken` đang mở | 4 | Giữ nguyên; thêm kiểm `setup-broken` ngay sau |
| (mới) Không có issue `setup-broken` đang mở | 5 | Mới |
| (mới) `scripts/preflight.sh` qua | 6 | Bước riêng, trước bước kích hoạt; đỏ thì không gọi routine |
| Có bí mật `ROUTINE_FIRE_URL`, `ROUTINE_FIRE_TOKEN` | 7 | Giữ nguyên, trong bước kích hoạt |
| Gọi API routine, kiểm mã `200` | 8 | Giữ nguyên; không bao giờ thử lại (không có khóa chống trùng) |

Không sửa: `resume-run.yml` (phiên tiếp tục không bị chặn vì `setup-broken`: lỗi staging không làm hỏng các task đang làm, và mục tiêu là phiên không dừng giữa chừng), `env-canary.yml`, `scripts/env-canary.sh` (nhãn khác), `staging.yml` (vẫn tự kiểm như cũ), `merge-if-green.sh` và `main-guard.yml` (không đọc nhãn mới).

**`scripts/java-check.sh`** (mới): đọc `${JAVA_HOME:+$JAVA_HOME/bin/}java -version` (hay `${JAVA_CMD}` khi test), lấy số phiên bản chính bằng `sed -nE '1s/.*version "([0-9]+)(\.[0-9]+)*.*/\1/p'` (dạng `1.8.0_392` ra 1, coi là 8). Ngoài 17 tới 24, hoặc không có `java`: in `LỖI: Java <n> không chạy được Gradle 8.14.3 của dự án (cần 17 tới 24). Đặt JAVA_HOME tới JDK của Android Studio, ví dụ ...` (hai dòng macOS và Linux, giống `docs/ops/setup-new-app.md` mục 1), mã thoát 1. Trong CI in kèm `::error::`. Khoảng phiên bản ghi một chỗ ở đầu script, kèm chú thích "đổi khi nâng Gradle".

**`scripts/check.sh`**: một dòng `scripts/java-check.sh` ngay sau `GRADLE=(...)`, trước "== Script tests" (test của script cũng gọi `java` cho `DesignCompare`). Không đổi gì khác của `check.sh`.

**Tài liệu:**
- `docs/auto-run.md` mục 2, thêm dòng: "Không có issue `setup-broken` đang mở, và lần chạy gần nhất của `preflight.yml` trong 26 giờ qua xanh". Không thỏa thì không chạy task nào, kết thúc phiên. Phiên mở bằng `claude --cloud` không qua `start-run.yml` (L-008), nên phải tự kiểm. Tìm lần chạy bằng `gh api ".../actions/workflows/preflight.yml/runs?branch=main&per_page=1"`.
- `docs/ops/staging-and-runs.md`: mục mới 2.8 "Kiểm tra môi trường trước phiên (`preflight.yml`)": kiểm gì, nhãn `setup-broken`, cách thêm file vào `scripts/required-assets.txt`, chạy tay bằng nút Run workflow.
- `docs/CLAUDE.md` mục Khi viết plan: plan thêm file R2 mà app bắt buộc có thì thêm đường dẫn vào `scripts/required-assets.txt` (ghi ở mục 3.2).
- `docs/ops/setup-new-app.md`: mục 5 thêm bước cuối "chạy workflow `preflight` một lần, phải xanh"; mục 1 thêm câu "`scripts/check.sh` tự báo khi Java không hợp".

Sau khi merge: chuyển L-002, L-011, L-012, L-022 sang `applied`, ghi L-027 phần DNS đã làm, kèm số PR. Sinh lại `docs/STATUS.md`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/preflight.sh` | Kiểm bí mật Firebase staging, nhóm người test, DNS, file R2 bắt buộc |
| Tạo | `scripts/required-assets.txt` | Danh sách file R2 bắt buộc |
| Tạo | `scripts/java-check.sh` | Kiểm phiên bản Java trước Gradle |
| Tạo | `.github/workflows/preflight.yml` | Chạy `preflight.sh` mỗi sáng, mở hay đóng issue `setup-broken` |
| Sửa | `.github/workflows/start-run.yml` | Kiểm `setup-broken`, chạy `preflight.sh` trước khi kích hoạt routine, tăng `timeout-minutes` |
| Sửa | `scripts/check.sh` | Gọi `scripts/java-check.sh` trước Gradle |
| Sửa | `scripts/tests/run.sh` | Test cho `preflight.sh` và `java-check.sh` |
| Sửa | `docs/auto-run.md` | Mục 2: dòng `setup-broken` và `preflight.yml` |
| Sửa | `docs/CLAUDE.md` | Khi viết plan: plan thêm file R2 bắt buộc thì thêm dòng vào `scripts/required-assets.txt` |
| Sửa | `docs/ops/staging-and-runs.md` | Mục 2.8 mới |
| Sửa | `docs/ops/setup-new-app.md` | Bước chạy `preflight`, câu về Java |
| Sửa | `docs/lessons.md` | Dòng Áp dụng của L-002, L-011, L-012, L-022, L-027 |
| Sửa | `docs/features/quality-gate/plans/T-030-kiem-tra-truoc-phien.md` | Nhật ký thực hiện |
| Sửa | `docs/features/quality-gate/tickets/T-030-kiem-tra-truoc-phien.md` | Trạng thái |
| Ghi lại | `docs/STATUS.md` | `scripts/status.py` |

Các file sau nhắc tới file mà plan sửa nhưng không bị ảnh hưởng (L-020):
- `scripts/env-canary.sh` gọi `scripts/check.sh`. Dòng mới chỉ dừng sớm khi Java sai. Môi trường cloud có sẵn JDK (`scripts/cloud-setup.sh` chỉ cài Android SDK); phiên bản chưa xác nhận, nhưng `check.sh` đang xanh ở đó, mà Gradle 8.14.3 chỉ chạy trên Java 17 tới 24, nên bước mới cũng qua. Nếu Java sai thì `env-canary` đỏ đúng lý do và mở `env-broken`, như mong muốn.
- `.github/workflows/pr-check.yml` và `.github/workflows/main-guard.yml` gọi `scripts/check.sh` sau `actions/setup-java` bản 17: qua bước mới.
- `scripts/failure-kind.sh` đọc log của `scripts/check.sh` trên `main-guard`. Lỗi của `java-check.sh` không có dòng `Execution failed for task`, nên được phân loại `other`: `main-guard` không chạy lại và báo đỏ, đúng vì đó là lỗi môi trường, không phải test chập chờn. Không cần sửa.
- `scripts/design-compare.sh` chỉ nhắc `scripts/check.sh` trong chú thích (ảnh chuẩn do `check.sh` ghi); không đọc phần plan đổi.
- `scripts/merge-if-green.sh` chỉ nhắc `docs/auto-run.md` mục 1 và mục 3 trong chú thích và thông báo; plan đổi mục 2.
- `scripts/cloud-setup.sh`, `scripts/cloud-env-check.sh`, `scripts/device-session.sh`, `scripts/wait-run.sh`, `scripts/resume-check.py`, `.github/workflows/device-session.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/env-canary.yml`, `.github/workflows/staging.yml` chỉ nhắc `scripts/check.sh`, `docs/auto-run.md`, `docs/ops/staging-and-runs.md`, `docs/ops/setup-new-app.md` hay `scripts/tests/run.sh` trong chú thích, thông báo hay đường dẫn tài liệu; chúng không đọc nội dung các mục mà plan đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Chỉ đọc (`curl -I`) các file đã có; không đổi tên hay đường dẫn trên R2
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm vào app. Workflow dùng `firebase-tools@15.32.1` qua `npx`, cùng phiên bản `staging.yml` đang dùng, và action `actions/setup-node@v4` (phương án dự phòng: `google-github-actions/auth@v2`); không chạm `gradle/libs.versions.toml`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** lệnh `firebase-tools appdistribution:groups:list` (tên lệnh, tham số `--project` nhận số hay mã project, dạng `--json`) chưa chạy thử, vì máy viết plan không có `npx` và không có tài khoản dịch vụ.
- **Điều kiện chuyển:** ở lần chạy tay đầu tiên của `preflight.yml` sau khi merge, lệnh trả lỗi "unknown command", không nhận tham số, hoặc trả về dạng không có tên nhóm.
- **Cách làm thay:** gọi REST API `GET https://firebaseappdistribution.googleapis.com/v1/projects/<project_number>/groups` với token lấy từ tài khoản dịch vụ (`google-github-actions/auth@v2` với `credentials_json`). Nếu cách đó cũng không được: bỏ điều kiện nhóm người test khỏi `preflight.sh` (giữ dòng `CHÚ Ý`), ghi vào nhật ký, và đề xuất ticket riêng. Ba kiểm tra còn lại và mọi test ở mục 4 giữ nguyên.

## 4. Kiểm thử

Test của script trong `scripts/tests/run.sh`, viết trước code. Lệnh ngoài được thay bằng script giả do test viết vào `$work/bin` (`DIG_CMD`, `CURL_CMD`, `FIREBASE_CMD`, `JAVA_CMD`), như cách `fake-gh` đang làm. File Firebase của test là JSON dựng trong test, có trường `api_key` mang giá trị đánh dấu `MARKER-SECRET-1` và `project_id` `proj-test-1`.

| Tiêu chí | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| L-002, file giả | Test script | `preflight: a fake google-services is refused`: `project_id` là `fake-staging-project`, có client `.staging`; mã 1, có dòng "file cấu hình giả" |
| L-002, thiếu app staging | Test script | `preflight: google-services without the staging app is refused`: chỉ có client gói khác; mã 1, dòng nêu tên gói |
| L-002, thiếu bí mật hay JSON hỏng | Test script | `preflight: missing secret is refused` (biến rỗng); `preflight: a secret that is not JSON is refused` |
| L-002, không in giá trị | Test script | `preflight: never prints secret values`: chạy với mọi trường hợp trên và trường hợp qua; `grep -c 'MARKER-SECRET-1\|proj-test-1'` trên toàn bộ đầu ra (và trên `$GITHUB_STEP_SUMMARY` giả) bằng 0 |
| L-012, nhóm người test | Test script | `preflight: a missing tester group is refused` (firebase giả trả `[{"alias":"other"}]`, `TESTER_GROUPS=team`; mã 1); `preflight: existing tester groups pass` (`team,qa` đều có); `preflight: a groups command that cannot run only warns` (firebase giả thoát 127; mã 0, có dòng `CHÚ Ý`) |
| L-027, DNS | Test script | `preflight: a domain one public DNS cannot resolve is refused`: dig giả trả rỗng với `@8.8.8.8`; mã 1, dòng nêu `8.8.8.8` và tên miền; `preflight: the domain is read from app/build.gradle.kts`: dig giả ghi tham số nó nhận, có `camera-pose.vgl.io.vn` |
| L-011, file R2 | Test script | `preflight: a missing R2 file is refused` (curl giả trả `404` cho một đường dẫn; mã 1, dòng nêu đường dẫn); `preflight: comments and blank lines of the list are skipped`; `preflight: a connection that fails once is retried` (curl giả trả `000` rồi `200`; mã 0); `preflight: a connection that keeps failing is refused` |
| Toàn bộ qua | Test script | `preflight: a ready environment passes`: mã 0, mỗi điều một dòng `QUA` |
| Một lỗi không che lỗi khác | Test script | `preflight: reports every failing check`: file giả và một file R2 `404` cùng lúc; cả hai dòng `LỖI` |
| L-022, Java | Test script | `java-check: Java 17 passes`, `java-check: Java 21 passes`, `java-check: Java 26 is refused with a JAVA_HOME hint` (có `JAVA_HOME` trong đầu ra), `java-check: Java 1.8 is refused`, `java-check: no java is refused` (`JAVA_CMD` trỏ tới file không có) |
| `check.sh` gọi kiểm tra Java trước Gradle | Test script | `check.sh calls java-check.sh before the first Gradle call`: số dòng của `scripts/java-check.sh` trong `scripts/check.sh` nhỏ hơn số dòng của `"${GRADLE[@]}"` đầu tiên |
| Không đổi hành vi cũ | Test script | Mọi test hiện có của `scripts/tests/run.sh` vẫn qua |
| `preflight.yml` trên GitHub | Kiểm tra thủ công | Sau khi merge, chạy tay `preflight`: (a) xanh với bí mật thật, không có issue mới; (b) tạm thêm một dòng không có thật vào bản sao của danh sách bằng `workflow_dispatch` với input `extra_asset` (chỉ để kiểm, mặc định rỗng), thấy issue `setup-broken` được mở rồi đóng ở lần chạy xanh tiếp theo; (c) đọc log, không có giá trị bí mật nào |
| `start-run.yml` | Kiểm tra thủ công | Lần bắt đầu phiên đầu tiên sau khi merge: log có bước "Kiểm tra môi trường staging" xanh trước "Kích hoạt routine"; khi có issue `setup-broken` mở, workflow dừng ở bước kiểm và không gọi routine |
| Workflow hợp lệ | Kiểm tra thủ công | Người duyệt chéo đọc diff của hai workflow; chạy `actionlint` nếu máy có |

**Ngoại lệ viết test trước:** `.github/workflows/preflight.yml`, `.github/workflows/start-run.yml` và các tài liệu không có test tự động: workflow chỉ chạy trên GitHub, `start-run.yml` chỉ chạy trên nhánh chính. Logic nằm trong `scripts/preflight.sh` đã có test; workflow chỉ ghi bí mật ra file, gọi script và ghi issue. Kiểm bằng tay theo ba dòng cuối của bảng.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-23, trên máy dev (macOS), không commit, bằng lệnh tay:
  - `dig +short @1.1.1.1 camera-pose.vgl.io.vn A` và `@8.8.8.8`: cả hai trả hai địa chỉ (`172.67.153.9`, `104.21.40.144`), `status: NOERROR`.
  - `curl -sS -o /dev/null -I -w '%{http_code}' https://camera-pose.vgl.io.vn/poses/catalog/v1/catalog-587b5394.json`: `200`.
  - `jq` trên file Firebase giả trong repo (`app/src/staging/google-services.json`, ADR-0012, không phải bí mật): `project_id` là `fake-staging-project` và có client `com.vglstudio.camera.pose.line.guide.staging`. Vì vậy kiểm riêng client `.staging` không đủ để nhận ra file giả; phải kiểm cả `project_id`, như `staging.yml` đang làm.
  - Lấy số phiên bản chính từ `java -version` bằng `sed` ở mục 3.1 trên JDK 21.0.13: ra `21`.
- **Chưa chạy thử:**
  - `firebase-tools appdistribution:groups:list`: máy không có `npx`, không có tài khoản dịch vụ (mục 3.4).
  - Hai workflow: chỉ chạy được trên GitHub.
  - `dig` trên runner `ubuntu-latest`: có sẵn trong ảnh của runner (gói `dnsutils`); chưa xác nhận bằng một lần chạy.
  - `scripts/check.sh` với dòng mới: chưa chạy; phiên chạy ở bước 5 và CI của PR chạy lại.
- **Lệnh:** `scripts/tests/run.sh` sau khi viết test; `scripts/check.sh` ở bước 5.
- **Kết quả:** các lệnh nền tảng (`dig`, `curl -I`, `jq`, `sed` cho Java) cho đúng kết quả mong đợi trên máy dev. Điểm chưa chắc duy nhất là lệnh liệt kê nhóm người test, có phương án dự phòng.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (CI, quy trình phiên tự chạy): sửa `.github/workflows/start-run.yml`, thêm workflow, sửa `scripts/check.sh` (CLAUDE.md, mục Không được làm).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Ba script mới, một workflow mới, sửa một workflow và `check.sh`, tài liệu |
| B. Độ rõ của yêu cầu | 1 | Bài học nêu rõ; còn câu hỏi về mức chặn và danh sách file R2 |
| C. Khả năng kiểm chứng tự động | 2 | Script có test với lệnh giả; workflow chỉ kiểm bằng tay |
| D. Ảnh hưởng chéo | 3 | `start-run.yml` và `check.sh` mọi phiên và mọi PR đều dùng |
| E. Độ mới của kỹ thuật | 2 | `dig`, `curl`, `jq` quen thuộc; liệt kê nhóm App Distribution bằng `firebase-tools` lần đầu dùng |
| **Tổng** | 10 | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `start-run.yml`, `scripts/check.sh`, `docs/auto-run.md` mục 2.
- Task khác có thể xung đột: PR #48 (đang mở) sửa `docs/auto-run.md` ở mục 3, mục 6 và bước 5, cùng `scripts/tests/run.sh`; merge PR #48 trước, rồi cập nhật nhánh.
- Một ticket ở `:core:assets` ghi tên lớp lỗi trong log (phần còn lại của L-027) độc lập với ticket này.
- Đề xuất thứ tự chạy: sau khi PR #48 merge; làm trong phiên có người theo dõi; không chạy khi có phiên tự chạy đang chạy (đổi `start-run.yml` giữa phiên không ảnh hưởng phiên đó, nhưng `check.sh` mới sẽ chạy trên `main-guard`).

## 7. Điều kiện dừng riêng

1. Một test hiện có của `scripts/tests/run.sh` phải sửa điều nó kiểm tra mới qua được.
2. Để kiểm bí mật cần in, ghi ra log hay đưa vào issue bất kỳ giá trị nào của bí mật: dừng.
3. Lần chạy tay đầu tiên của `preflight.yml` với bí mật thật đỏ ở điều kiện bí mật Firebase hay DNS trong khi `staging.yml` gần nhất vẫn xanh (kiểm tra mới sai, không phải môi trường sai): dừng, không merge thay đổi của `start-run.yml` cho tới khi rõ nguyên nhân.

## 8. Câu hỏi cho người duyệt

1. **Lỗi nào chặn bắt đầu phiên?** L-012 đề xuất chặn mọi lỗi. Nhưng bí mật staging hay nhóm người test sai chỉ làm hỏng bước phát staging ở cuối phiên; các task vẫn làm và merge được. File R2 thiếu hay DNS lỗi thì làm `ui-test` của PR đỏ, nên task dừng.
   - Đề xuất của AI: chặn với mọi lỗi, đúng L-012, để người sửa trong ngày. Nếu muốn phiên vẫn chạy khi chỉ phần staging lỗi, thì đổi hai kiểm tra đó thành `CHÚ Ý`, phiên chạy tiếp và ghi vào báo cáo "không phát staging".
   - Trả lời: Đồng ý với đề xuất của AI.
2. **Nhãn mới `setup-broken` hay dùng chung `env-broken`?**
   - Đề xuất của AI: nhãn mới. `scripts/env-canary.sh` đóng mọi issue `env-broken` khi môi trường cloud xanh, nên dùng chung thì issue của `preflight` bị đóng nhầm. Dùng chung thì phải sửa `env-canary.sh`, một file nữa của cổng.
   - Trả lời: Đồng ý với đề xuất của AI.
3. **Danh sách file R2 bắt buộc gồm những gì?** Hiện chỉ có catalog mặc định.
   - Đề xuất của AI: ghi tay vào `scripts/required-assets.txt`. Mỗi plan thêm file R2 bắt buộc thì thêm dòng (ghi vào `docs/CLAUDE.md` ở PR này). Chưa đọc catalog để kiểm từng ảnh của bộ khởi đầu: 60 tư thế nhân 3 file là 180 lần `curl` mỗi sáng. Nếu muốn, ticket sau thêm tùy chọn kiểm các ảnh mà catalog ghi.
   - Trả lời: Đồng ý với đề xuất của AI.
4. **Xung đột `docs/STATUS.md` ở mọi PR (L-016) xử lý thế nào?** Plan này không làm. Các cách:
   - (a) PR thường không commit `STATUS.md`; chỉ PR báo cáo của phiên và một workflow trên nhánh chính sau mỗi lần merge sinh lại trang. Test "status matches" chuyển sang chỉ chạy trên nhánh chính. Workflow này phải đẩy commit lên nhánh chính, trái với quy tắc chỉ merge qua `merge-if-green.sh`, nên cần một ADR.
   - (b) `.gitattributes` với `docs/STATUS.md merge=ours`. Cách này không làm GitHub hết báo `DIRTY`, vì kiểm tra gộp của GitHub không dùng merge driver riêng, nên không giải quyết được xung đột trên PR.
   - (c) Giữ như hiện nay. `merge-if-green.sh` báo nhánh chậm thì người hay phiên gộp nhánh chính rồi chạy `scripts/status.py`. Có thể thêm hướng dẫn một lệnh làm việc này.
   - Đề xuất của AI: tạm làm (c) bằng một câu trong `docs/CLAUDE.md`. Chọn (a) thì viết ADR và ticket riêng.
   - Trả lời: Đồng ý với đề xuất của AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** 

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### 2026-10-23, phiên có người theo dõi (Tuấn)

- **Bước 3, test viết trước** (commit 01e6671): 32 test mới trong `scripts/tests/run.sh`, với `dig`, `curl`, `firebase`, `java` giả. Trên code cũ có 30 test đỏ, đúng lý do: không có `scripts/preflight.sh`, `scripts/java-check.sh` (mã 127), `check.sh` chưa gọi kiểm tra Java. Hai test xanh ngay, như mong đợi vì chúng kiểm điều không được xảy ra: "never prints secret values" và "comments and blank lines of the list are skipped". Chúng được kiểm lại sau bước 4.
- **Bước 4, code** (commit 6d06e43): theo mục 3.1.
  - `scripts/preflight.sh`, `scripts/java-check.sh`, `scripts/required-assets.txt` (chỉ catalog mặc định, câu hỏi 3);
  - `.github/workflows/preflight.yml`, `.github/workflows/start-run.yml`;
  - một dòng trong `scripts/check.sh`;
  - các tài liệu ở mục 3.2.
  - Câu hỏi 4 (L-016), cách (c): một câu ở `docs/CLAUDE.md`, mục Trang tiến độ.
  - Nhóm người test đọc tên từ `alias` hoặc từ phần cuối của `name` (`projects/<số>/groups/<tên>`), vì chưa biết dạng `--json` thật của `firebase-tools` (mục 3.4).
- **Chỉnh trong lúc viết test:** ba dòng của test dùng `grep` trên file có thể chưa có được thêm `|| true`. Thiếu nó, `set -e` của `run.sh` dừng cả bộ test ở test đỏ đầu tiên, thay vì báo đỏ. Điều test kiểm tra không đổi.
- **Bước 5:**
  - `scripts/tests/run.sh` qua 303 test, sau khi gộp nhánh chính có T-029 (PR #49).
  - `scripts/java-check.sh` trên JDK 21.0.13 in "Java 21: dùng được".
  - Không có `actionlint` trên máy. Hai workflow được đọc bằng YAML của Ruby: cú pháp đúng, đủ 5 bước. Cần người duyệt chéo đọc diff.
  - `scripts/check.sh` đầy đủ để CI của PR chạy, vì một phiên khác đang dùng Gradle trên máy.
- **Còn lại sau khi merge** (mục 4, ba dòng kiểm tay; điều kiện dừng 3):
  - (a) chạy tay `preflight` với bí mật thật, phải xanh, đọc log không có giá trị bí mật;
  - (b) chạy với `extra_asset` là một đường dẫn không có thật, thấy issue `setup-broken` được mở, rồi đóng ở lần chạy xanh sau;
  - (c) lần `start-run` đầu tiên có bước "Kiểm tra môi trường staging" xanh trước "Kích hoạt routine".
  - Lần chạy (a) đỏ ở điều kiện bí mật hay DNS trong khi `staging.yml` gần nhất xanh thì dừng: revert phần `start-run.yml` cho tới khi rõ nguyên nhân.
  - Lệnh `appdistribution:groups:list` chạy thử ở lần (a); không được thì làm theo mục 3.4.
