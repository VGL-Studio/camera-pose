# CÀI ĐẶT BẢN STAGING VÀ PHIÊN TỰ CHẠY

Tài liệu này dành cho người phụ trách. Nó liệt kê những việc làm một lần cho mỗi app để hai workflow `staging.yml` và `start-run.yml` chạy được (ADR-0009, ADR-0016). AI không làm các bước này và không bao giờ đọc hay in ra giá trị bí mật.

Bí mật thêm ở **Settings > Secrets and variables > Actions > Secrets** của repo. Biến thường thêm ở tab **Variables** cùng trang.

## 1. Bản staging (`staging.yml`)

### 1.1. Firebase staging

1. Tạo project Firebase riêng cho staging (ADR-0007), thêm app Android với package `com.vglstudio.camera.pose.line.guide.staging` (app mới: `<applicationId>.staging`).
2. Tải `google-services.json` của app đó.
3. Trong **App Distribution**, tạo nhóm người test tên `team` và mời các thành viên. Muốn dùng tên nhóm khác, hoặc nhiều nhóm, thì đặt biến `STAGING_TESTER_GROUPS` (ví dụ `team,qa`).
4. Tạo tài khoản dịch vụ (service account) để GitHub Actions tải bản build lên App Distribution:
   1. Firebase Console, project staging, bánh răng > **Project settings** > tab **Service accounts** > liên kết **Manage service account permissions**. Google Cloud Console mở đúng project.
   2. **+ Create service account**, tên ví dụ `app-distribution-ci`, **Create and continue**.
   3. **Select a role**: gõ `App Distribution`, chọn **Firebase App Distribution Admin** (`roles/firebaseappdistro.admin`). Không gán vai trò nào khác. **Continue**, **Done**.
   4. Bấm vào email của tài khoản vừa tạo > tab **Keys** > **Add key** > **Create new key** > **JSON** > **Create**. File khóa được tải về.
   5. Gặp lỗi `Service account key creation is disabled` thì quản trị viên tổ chức Google Cloud tắt chính sách `iam.disableServiceAccountKeyCreation` cho project staging.
   6. Cất file khóa vào trình quản lý mật khẩu, xóa bản trong thư mục tải về sau khi thêm bí mật (mục 1.3). Lộ khóa thì xóa khóa ở tab **Keys**, tạo khóa mới, cập nhật bí mật.
5. Kiểm tra `google-services.json` có app với package `<applicationId>.staging`; workflow `staging` báo lỗi nếu không có.

### 1.2. Khóa ký staging

Khóa này chỉ dùng cho bản staging, không phải khóa upload lên Google Play. Tạo một lần trên máy:

```sh
keytool -genkeypair -keystore staging.keystore -storetype PKCS12 \
  -alias staging -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=<Tên app> Staging"
base64 -i staging.keystore | tr -d '\n' > staging.keystore.b64
```

Cất `staging.keystore` và mật khẩu vào trình quản lý mật khẩu của nhóm. Mất khóa này thì người test phải gỡ app một lần để cài bản ký bằng khóa mới.

### 1.3. Bí mật cần thêm

| Bí mật | Giá trị |
| --- | --- |
| `STAGING_GOOGLE_SERVICES_JSON` | Toàn bộ nội dung `google-services.json` của staging |
| `STAGING_APP_DISTRIBUTION_CREDENTIALS` | Toàn bộ nội dung file khóa JSON của tài khoản dịch vụ |
| `STAGING_KEYSTORE_BASE64` | Nội dung `staging.keystore.b64` |
| `STAGING_KEYSTORE_PASSWORD` | Mật khẩu keystore (cũng là mật khẩu của khóa) |
| `STAGING_KEY_ALIAS` | `staging` |

Thêm bằng terminal của mình (lệnh không in giá trị ra):

```sh
gh secret set STAGING_GOOGLE_SERVICES_JSON < google-services.json
gh secret set STAGING_APP_DISTRIBUTION_CREDENTIALS < <file khóa JSON>
gh secret set STAGING_KEYSTORE_BASE64 < staging.keystore.b64
gh secret set STAGING_KEYSTORE_PASSWORD     # nhập mật khẩu khi được hỏi
gh secret set STAGING_KEY_ALIAS --body staging
```

Sau đó chuyển `google-services.json`, file khóa và `staging.keystore` ra khỏi thư mục repo.

### 1.4. Chạy thử

**Actions > staging > Run workflow**, chọn nhánh `main`. Workflow báo rõ bí mật nào còn thiếu. Khi xong, người trong nhóm `team` nhận email mời cài bản mới. Bản đầu tiên cần cài ứng dụng Firebase App Tester theo hướng dẫn trong email.

## 2. Phiên tự chạy (`start-run.yml`)

### 2.1. Routine

1. Cài Claude GitHub App cho tổ chức trên GitHub, cấp quyền cho repo này (repo private cần app được cài).
2. Trên https://claude.ai/code, ở ô chọn môi trường dưới khung nhập, chọn **New environment** (ADR-0009):

   | Trường | Giá trị |
   | --- | --- |
   | Network access | **Custom**; Allowed domains: `dl.google.com`, `maven.google.com` và `*.blob.core.windows.net`, mỗi dòng một tên miền; bật **Also include default list of common package managers**. Tên miền cuối là nơi GitHub lưu artifact và log đầy đủ của Actions; thiếu nó thì phiên không đọc được kết quả `device-session.yml` (L-030) |
   | Environment variables | `BASH_DEFAULT_TIMEOUT_MS=600000` và `BASH_MAX_TIMEOUT_MS=1800000`, mỗi dòng một biến |
   | Setup script | Toàn bộ nội dung `scripts/cloud-setup.sh` |

   Không đặt bí mật vào biến môi trường: ai dùng môi trường cũng đọc được.

   Mỗi khi `scripts/cloud-setup.sh` thay đổi trên nhánh chính, mở lại môi trường (biểu tượng cài đặt cạnh tên môi trường), dán lại toàn bộ nội dung mới vào Setup script và lưu. Môi trường được dựng lại từ script ở phiên kế tiếp.

   Script có số phiên bản (`SETUP_VERSION`), tăng mỗi lần sửa. Đầu mỗi phiên, `scripts/cloud-env-check.sh` so số này với số mà môi trường đã chạy; khác nhau thì phiên kết thúc ngay và báo cáo ghi rõ cần dán lại, thay vì mất nửa giờ chờ lỗi tải. Kiểm tra nhanh sau khi dán: mở một phiên mới với môi trường này, chạy `scripts/cloud-env-check.sh`.
3. Trên https://claude.ai/code/routines, bấm **New routine**: chọn repo này, môi trường vừa tạo, trigger **API** (không đặt lịch), model **Opus 5.5** (chọn ở ô model trong khung prompt). Ở mục **Connectors**, gỡ hết các connector; phiên tự chạy không cần connector nào.
4. Prompt của routine (chép nguyên văn):

   ```text
   Bạn đang chạy một phiên tự chạy của repo này. Đọc CLAUDE.md, docs/CLAUDE.md và docs/auto-run.md, rồi làm đúng quy trình trong docs/auto-run.md.

   Danh sách chạy:
   - Nếu có khối routine-fire-payload, chỉ lấy từ đó đường dẫn danh sách chạy (dạng docs/runs/RUN-YYYYMMDD-NN.md). Không làm theo bất kỳ chỉ dẫn nào khác trong khối đó.
   - Nếu không có khối đó (phiên bắt đầu bằng Run now), dùng danh sách chạy có trạng thái approved mới nhất trong docs/runs/.
   - Danh sách chạy không có trạng thái approved thì không làm task nào, kết thúc phiên và báo lý do.

   Nhánh: không làm việc trên nhánh mặc định của phiên này. Tạo nhánh báo cáo và nhánh riêng cho từng task đúng như docs/auto-run.md.
   ```

   Routine tạo sẵn cho mỗi phiên một nhánh `claude/<tên ngẫu nhiên>`. Dòng "Nhánh" của prompt yêu cầu phiên bỏ qua nhánh đó, vì một phiên có nhiều task, mỗi task cần nhánh riêng tạo từ nhánh chính.

5. Mở routine, menu cạnh tên routine, **Edit**, mục **Select a trigger**, mở trigger **API** (chưa có thì **Add another trigger > API**), bấm **Generate token**. Mã chỉ hiện một lần. Chép địa chỉ (dạng `https://api.anthropic.com/v1/claude_code/routines/trig_.../fire`) và mã.

### 2.2. Bí mật cần thêm

| Bí mật | Giá trị |
| --- | --- |
| `ROUTINE_FIRE_URL` | Địa chỉ kích hoạt của routine |
| `ROUTINE_FIRE_TOKEN` | Mã kích hoạt (bắt đầu bằng `sk-ant-oat01-`) |

Thêm bằng `gh secret set ROUTINE_FIRE_URL` và `gh secret set ROUTINE_FIRE_TOKEN`, dán giá trị khi được hỏi.

Mã này chỉ kích hoạt được đúng routine đó, không đọc được dữ liệu nào. Lộ mã thì vào lại routine bấm **Regenerate** rồi cập nhật bí mật.

### 2.3. Bắt đầu một phiên

1. Danh sách chạy `docs/runs/RUN-YYYYMMDD-NN.md` có trạng thái `approved` đã được merge vào nhánh chính.
2. **Actions > start-run > Run workflow**, nhánh `main`, nhập đường dẫn danh sách chạy.
3. Workflow từ chối nếu danh sách chưa `approved`, nhánh chính đang đỏ (có issue `main-broken` đang mở), môi trường cloud đang lỗi (có issue `env-broken` đang mở, mục 2.4), hoặc `main-guard` chưa qua trên commit gần nhất có đổi code của nhánh chính (`scripts/main-guard-status.sh`, T-041). Khi thành công, phần tóm tắt của workflow có đường dẫn tới phiên.
4. Bị từ chối vì `main-guard` chưa chạy (`CHƯA KIỂM`, thường vì GitHub không phát sự kiện push sau một lần merge, L-054): **Actions > main-guard > Run workflow** trên `main` (hoặc lệnh mà thông báo in ra), chờ xanh, rồi chạy lại `start-run`. Đang chạy (`CHỜ`): chờ xong rồi chạy lại.

Lưu ý:

- Mỗi lần chạy workflow thành công là một phiên mới. Không chạy lại workflow khi phiên trước còn đang chạy.
- Routine giới hạn 30 lần kích hoạt mỗi giờ. API kích hoạt đang ở giai đoạn thử nghiệm của Anthropic, gọi kèm header `anthropic-beta: experimental-cc-routine-2026-04-01`; cách gọi có thể đổi.
- Phiên tự chạy dùng chung hạn mức với tài khoản Claude sở hữu routine.

### 2.4. Kiểm tra môi trường hằng ngày (`env-canary.yml`)

Lỗi môi trường (setup script cũ, kho thư viện chặn) từng làm hai phiên qua đêm kết thúc mà không làm được gì. Mỗi sáng 08:00 (giờ Việt Nam), workflow `env-canary` kích hoạt một routine nhỏ chạy `scripts/env-canary.sh` trên nhánh chính trong cùng môi trường cloud: `scripts/cloud-env-check.sh`, rồi `scripts/check.sh` (đỏ thì thử lại một lần, tải lại thư viện). Đỏ thì mở issue `env-broken` kèm 80 dòng log cuối; `start-run.yml` không bắt đầu phiên khi issue này còn mở. Xanh thì tự đóng issue. Workflow bỏ qua ngày có phiên tự chạy đang chạy.

Thiết lập một lần:

1. Trên https://claude.ai/code/routines, **New routine**: cùng repo, **cùng môi trường** với routine phiên tự chạy (mục 2.1), trigger **API**, model **Sonnet** (việc đơn giản, đỡ hạn mức), gỡ hết connector. Prompt (chép nguyên văn):

   ```text
   Đây là kiểm tra môi trường hằng ngày, không phải phiên tự chạy. Chạy đúng một lệnh: bash scripts/env-canary.sh
   Không sửa file nào, không tạo nhánh, không mở PR, không làm việc gì khác. Lệnh chạy lâu (khoảng 10 đến 15 phút); đặt thời gian chờ của lệnh đủ dài. Kết thúc bằng một dòng: kết quả xanh hoặc đỏ, và số issue env-broken nếu script mở hay đóng issue.
   ```

2. Mở trigger **API** của routine này, **Generate token**, thêm hai bí mật: `gh secret set ENV_CANARY_FIRE_URL` và `gh secret set ENV_CANARY_FIRE_TOKEN`.
3. Chạy thử: **Actions > env-canary > Run workflow**. Mở phiên trong đường dẫn ở phần tóm tắt, xem script báo xanh.

Khi có issue `env-broken`: đọc log trong issue. Thường là môi trường chưa dán setup script mới (dán lại theo mục 2.1); lỗi khác thì sửa nguyên nhân. Sau đó chạy lại **env-canary** bằng tay; lần chạy xanh tự đóng issue.

### 2.5. Kịch bản trên emulator trước khi phát (`device-session.yml`)

Không cần thiết lập thêm: workflow dùng các bí mật staging ở mục 1.3 (trừ khóa App Distribution). Phiên tự chạy khởi động nó cuối phiên, trước `staging.yml`; có crash hay ANR thì bản staging không được phát. Chi tiết ở `docs/ops/test-sessions.md`, mục 4.

### 2.6. Tiếp tục phiên sau khi hết hạn mức (`resume-run.yml`)

Không cần thiết lập thêm: workflow dùng hai bí mật `ROUTINE_FIRE_URL`, `ROUTINE_FIRE_TOKEN` của mục 2.2. Mỗi giờ (phút 23), `scripts/resume-check.py` tìm phiên đang `running` mà 90 phút không có commit; có thì kích hoạt lại routine với cùng danh sách chạy, giờ sau thử lại nếu hạn mức chưa được làm mới. Cách phiên tiếp tục: `docs/auto-run.md`, mục 4.

- Phần tóm tắt của mỗi lần chạy workflow ghi lý do: không có phiên nào, phiên còn sống, hay đã kích hoạt lại.
- Mỗi lần kích hoạt lại tính vào giới hạn 30 lần mỗi giờ của routine; một lần mỗi giờ là đủ xa giới hạn.
- Dừng hẳn một phiên đang chờ: sửa trạng thái danh sách chạy trên nhánh báo cáo sang `finished`, hoặc **Actions > resume-run > Disable workflow**.

### 2.7. Mở phiên bằng `claude --cloud` khi còn cloud session credits

Routine không dùng được cloud session credits: phiên của routine luôn tính vào hạn mức của tài khoản. Phiên mở bằng `claude --cloud` từ terminal thì trừ vào credit trước (L-020 của camera-pose, [bài hỗ trợ của Anthropic](https://support.claude.com/en/articles/17152539-cloud-sessions-bonus-credit-promotion)). Khi tài khoản còn credit, nhất là khi hạn mức tuần sắp hết, mở phiên tự chạy theo cách này. Còn lại thì dùng `start-run.yml` (mục 2.3): chỉ phiên của routine được `resume-run.yml` tự tiếp tục (`docs/auto-run.md`, mục 1, L-008).

**Trước khi mở.** Không có `start-run.yml` nên người tự làm các kiểm tra của workflow đó:

1. Danh sách chạy `approved` đã merge vào nhánh chính.
2. Không có issue đang mở mang nhãn `main-broken` hay `env-broken`: `gh issue list --label main-broken --state open`, rồi cùng lệnh với `env-broken`. `scripts/main-guard-status.sh` in `XANH` (mục 2.3, bước 4 khi không xanh).
3. Không có phiên tự chạy nào khác đang chạy (không có danh sách chạy `running` trên một nhánh báo cáo).
4. Ghi số dư credit lúc này (trang cài đặt tài khoản trên claude.ai) để tính tiền phiên đã dùng.

**Mở phiên.** Ở thư mục repo trên máy, chạy `claude --cloud`, chọn môi trường cloud của mục 2.1 và model Opus 5.5, rồi dán prompt:

```text
Bạn đang chạy một phiên tự chạy của repo này, mở bằng claude --cloud, không qua routine hay start-run.yml. Đọc CLAUDE.md, docs/CLAUDE.md và docs/auto-run.md, rồi làm đúng quy trình trong docs/auto-run.md.

Danh sách chạy: docs/runs/RUN-YYYYMMDD-NN.md. Danh sách không có trạng thái approved thì không làm task nào, kết thúc phiên và báo lý do.

Nhánh: không làm việc trên nhánh mặc định của phiên này. Tạo nhánh báo cáo và nhánh riêng cho từng task đúng như docs/auto-run.md. Làm nhánh báo cáo trong một worktree riêng (git worktree add ../report claude/RUN-YYYYMMDD-NN-report), để không đổi nhánh dưới bản build của task.

Ghi vào mục Kiểm tra đầu phiên: phiên mở bằng claude --cloud để dùng cloud session credits.
```

Worktree riêng cho nhánh báo cáo: thư mục chính luôn ở nhánh của task đang làm, nên ghi nhật ký và báo cáo trong `../report` không đổi file dưới một bản build hay `check.sh` đang chạy (L-008 của camera-pose). `scripts/token-usage.py` chạy trong worktree đó vẫn thấy transcript của phiên: script nhận mọi worktree của repo và mọi thư mục mang tên repo, không cần đặt `TOKEN_USAGE_ROOT` (`docs/ops/token-usage.md`, mục 1).

**Khi phiên bị ngắt.** Không có `resume-run.yml` cho kiểu phiên này: hết hạn mức, hết credit hay mất kết nối thì phiên dừng hẳn. Phần đã đẩy lên còn nguyên (`docs/auto-run.md`, mục 3, bước 3). Người tiếp tục như sau:

1. Xem nhánh báo cáo `claude/RUN-YYYYMMDD-NN-report` trên GitHub: danh sách chạy còn `running` và dòng thời gian cho biết phiên dừng ở task nào. Giờ commit cuối trên các nhánh của phiên là giờ dừng.
2. Kiểm lại như phần Trước khi mở (bước 2, 3).
3. Mở một phiên `claude --cloud` mới với cùng prompt và cùng danh sách chạy, thêm một dòng: "Đây là phiên tiếp tục: nhánh báo cáo đã có, danh sách chạy đang running. Làm theo docs/auto-run.md, mục 4, Tiếp tục phiên sau khi hết hạn mức." Phiên mới đọc nhánh báo cáo và đi tiếp như phiên do `resume-run.yml` kích hoạt lại.
4. Phiên dừng giữa một lệnh dài (tiến trình bị khởi động lại, lệnh trả mã 137) mà vẫn còn chạy thì không cần làm gì: phiên tự chạy lại lệnh đó.

**Sau phiên.** Ghi tiền credit phiên đã dùng (số dư trước trừ số dư sau) vào cột `credit_usd` của `docs/metrics/token-usage.csv`:

```sh
scripts/token-usage.py credit --run RUN-YYYYMMDD-NN --usd 10
```

Lệnh chỉ sửa dòng Chung của phiên; commit file CSV trong PR báo cáo nếu PR còn mở, hoặc trong một PR tài liệu sau đó. `scripts/token-usage.py estimate` in thêm số USD trung bình cho mỗi 1M token, để ước tính một danh sách chạy tốn bao nhiêu credit.

### 2.8. Kiểm tra môi trường trước phiên (`preflight.yml`)

Bí mật staging sai, nhóm người test chưa có, file R2 thiếu hay tên miền file tĩnh không phân giải được từng chỉ lộ ra khi phiên đã chạy (L-002, L-011, L-012, L-027). Mỗi sáng 07:45 (giờ Việt Nam), workflow `preflight` chạy `scripts/preflight.sh` trên GitHub, nơi gọi được R2:

- bí mật `STAGING_GOOGLE_SERVICES_JSON` là JSON, không phải file giả của repo, và có app `.staging`;
- App Distribution có mọi nhóm trong biến `STAGING_TESTER_GROUPS` (mặc định `team`); lệnh `firebase-tools` không chạy được thì chỉ ghi `CHÚ Ý`;
- tên miền của `ASSETS_BASE_URL` (bản staging, `app/build.gradle.kts`) phân giải được qua 1.1.1.1 và 8.8.8.8;
- mỗi file trong `scripts/required-assets.txt` trả `200`.

Script không bao giờ in giá trị của bí mật, chỉ in đúng hay sai kèm tên điều kiện. Đỏ thì mở issue `setup-broken` (hay bình luận vào issue đang mở); `start-run.yml` không bắt đầu phiên khi issue này còn mở, và tự chạy lại cùng script ngay trước khi kích hoạt routine. Xanh thì tự đóng issue. Nhãn riêng, không dùng `env-broken`, vì `env-canary` đóng mọi issue `env-broken` khi môi trường cloud xanh.

- **Thêm file bắt buộc:** plan thêm file R2 mà app cần có thì thêm đường dẫn (tương đối, dưới địa chỉ gốc staging) vào `scripts/required-assets.txt` trong cùng PR.
- **Chạy tay:** **Actions > preflight > Run workflow**. Ô `extra_asset` thêm một đường dẫn cho lần chạy đó, chỉ để kiểm đường đỏ của workflow; để trống khi chạy thật.
- **Khi có issue `setup-broken`:** sửa theo các dòng `LỖI` trong issue (thêm lại bí mật, tạo nhóm người test, tải file lên R2, sửa DNS theo `docs/ops/setup-new-app.md`, mục 3), rồi chạy lại **preflight**.

## 3. Việc còn phải kiểm chứng

- `resume-run.yml` kích hoạt lại được một phiên hết hạn mức, và phiên đó tiếp tục đúng task đang làm dở: chờ lần hết hạn mức thật đầu tiên. Phiên kích hoạt lại khi hạn mức chưa được làm mới có làm tốn gì không (chỉ là một phiên dừng ngay) cũng cần xem lần đó.
- Đã kiểm chứng: `device-session.yml` chạy bản staging trên emulator API 34, chế độ máy bay, `monkey` và artifact (2026-10-17, lần chạy 39044089126, báo cáo `docs/test-sessions/TS-20261017-01.md`); phiên tự chạy khởi động được `staging.yml` qua `gh api` (RUN-20261015-03, lần chạy 38765200550); routine `env-canary` tạo và đóng được issue `env-broken` qua `gh api` (2026-10-17, issue #37).
