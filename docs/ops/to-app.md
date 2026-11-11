# ĐƯA THAY ĐỔI CỦA DỰ ÁN MẪU VỀ APP

Chiều ngược của [from-app.md](from-app.md). App nhận sửa lỗi quy trình, script, CI và module core mới của dự án mẫu qua một PR đồng bộ, dựng bằng `scripts/template-sync.sh` của dự án mẫu (ADR-0030).

Người làm: AI trong phiên có người theo dõi, trên máy có cả repo dự án mẫu và repo app, theo yêu cầu của người phụ trách. Phiên tự chạy của app không làm việc này: nó không đọc được repo mẫu.

## 1. Khi nào đồng bộ

- App cần một phần mới của dự án mẫu, ví dụ sửa lỗi của `merge-if-green.sh` hay bộ quảng cáo mới.
- Sau mỗi đợt lớn của dự án mẫu, ít nhất hai tuần một lần, để mỗi đợt nhỏ và ít xung đột.
- Chỉ giữa hai phiên tự chạy của app: không có phiên nào đang chạy, và đợt đồng bộ merge xong trước phiên sau.

## 2. Phân loại file

| Loại | File | Khi xung đột |
| --- | --- | --- |
| Không bao giờ đồng bộ | `scripts/new-app.sh`, `scripts/template-sync.sh`, `scripts/history-refs.py`, `scripts/tests/history-refs-allow.txt`, `docs/ops/new-app.md`, `.github/ISSUE_TEMPLATE/`, `docs/lessons.md`, `docs/runs/`, `docs/features/`, `docs/metrics/`, `docs/inbox/`, `docs/test-sessions/`, `docs/design-system/tokens.json`, `docs/ops/required-assets.txt`, trang sinh ra (`docs/foundation.md`, `docs/STATUS.md`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`) | Không xảy ra: script bỏ các file này ở cả hai phía. Trang sinh ra được sinh lại trong app |
| Của dự án mẫu, app không sửa | `scripts/`, `.github/`, `.claude/skills/` (command của quy trình; app cần command riêng thì đặt tên khác), `build-logic/`, `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/templates/`, `docs/ops/`, ADR `origin: template`, `:core:logging`, `:core:network`, `:core:assets`, `:core:ads` trừ `AdPlacement.kt` | Lấy bản mẫu (`git checkout --theirs -- <file>`), sau khi chắc phần app sửa đã về dự án mẫu (mục 4, bước 1); rồi đặt lại giá trị riêng của app (mục 6) |
| Cả hai cùng sửa | `CLAUDE.md`, `docs/CLAUDE.md`, `README.md`, `.gitignore`, `:app`, `:core:designsystem`, `:core:config`, `:core:analytics`, `AdPlacement.kt`, `feature/sample`, `feature/loading`, cấu hình build ở gốc | Gộp tay: lấy phần mới của mẫu, giữ phần riêng của app (module, vị trí quảng cáo, cờ, màu) |
| Chỉ của app | Feature của app, module core riêng của app, ADR từ 1001, `scripts/setup-check-app.sh`, `scripts/tests/app.sh` (mục 6), kịch bản thử tay trên máy thật `docs/ops/manual-test.md` (theo nghiệp vụ của app, mẫu không có file này) | Không xảy ra: dự án mẫu không có các file này |

Test và ảnh chuẩn của `:core:ads` vẽ trong theme cố định của test (`AdsTestTheme`, chép giá trị của dự án mẫu, phông Inter riêng trong `src/test/resources/ads-test-fonts/`), không theo `AppTheme` của app. App giữ tên và chữ ký `AppTheme(darkTheme, content)`: `AdsTestThemeMatchTest.kt` vẫn được biên dịch ở app dù bị bỏ qua. App đổi màu, chữ hay phông trong `:core:designsystem` không cần ghi lại ảnh của `:core:ads`; xung đột ở các ảnh đó lấy bản của mẫu như phần còn lại của module. Còn phụ thuộc: `Spacing` (mã chính của `:core:ads` dùng). App đổi giá trị `Spacing` thì ghi lại ảnh của `:core:ads` và ghi ngoại lệ trong ADR của app. Ảnh của `:app` có quảng cáo (`AdGalleryScreen_*`) vẫn theo theme của app.

ADR `superseded` của dự án mẫu không đồng bộ (ADR-0044). Script bỏ chúng ở cả hai phía theo trạng thái ở commit của mỗi phía, nên ADR vừa bị thay bị xóa khỏi app. Sau khi gộp, script xóa thêm ở app mọi ADR của dự án mẫu đã `superseded` ở mốc đích mà app còn giữ. Đây là chỗ duy nhất được xóa tài liệu ở app. Script in thêm danh sách tài liệu quy tắc của app còn nhắc mã ADR vừa xóa; PR đồng bộ sửa các chỗ đó sang ADR thay nó. ADR mà app đã sửa vẫn bị xóa; script in tên nó với dòng `CHÚ Ý`, và mô tả PR đồng bộ ghi phần sửa đã mất. ADR riêng của app (từ 1001) không bao giờ bị đụng.

Script in danh sách xung đột theo hai nhóm giữa: "Phần của dự án mẫu" và "Phần app cũng sửa".

## 3. File mốc `docs/template-sync.txt` của app

```
base=<commit đầy đủ của dự án mẫu mà app đang theo>
package=<tên gói của app>
hold=<đường dẫn như trong dự án mẫu> <commit của dự án mẫu>
```

- `new-app.sh` ghi `base=` và `package=` cho app mới.
- App tạo trước ADR-0030: tạo tay trong PR đồng bộ đầu tiên. `base=` là commit ghi trong commit đầu tiên của app ("Khởi tạo ... từ dự án mẫu (<commit>)"). Commit đó nằm trên một nhánh đã merge kiểu squash thì script tự tìm commit của nhánh chính có cùng nội dung.
- `hold=`: phần app tạm giữ ở mốc cũ, mỗi dòng một đường dẫn (ví dụ `hold=core/ads <commit>`). Đợt thường bỏ qua phần đó. `--take <đường dẫn>` đưa nó lên đúng mốc `base=` rồi xóa dòng.

Chỉ PR đồng bộ được sửa `base=` và `hold=`. Script tự sửa, trong cùng commit với thay đổi.

## 4. Các bước

1. **Đưa phần chung của app về trước.** Xem bài học phạm vi `chung` của app chưa có issue `from-app`, và commit của app ở file thuộc loại "của dự án mẫu" kể từ đợt trước (`git log <ngày đợt trước>.. -- scripts .github docs/ops docs/auto-run.md`). Phần nào chưa về dự án mẫu thì mở issue `from-app` (`from-app.md`, mục 2). Gấp thì ghi lại để giữ khi gộp (bước 4).
2. **Cập nhật hai repo.** Repo app sạch, ở nhánh chính mới nhất. Dự án mẫu `git fetch`.
3. **Chạy script** từ thư mục dự án mẫu:

   ```bash
   scripts/template-sync.sh --app ../<app> [--to <commit>]   # mặc định origin/main
   ```

   Script tạo nhánh `template-sync/<commit>` trong app. Không xung đột thì nó commit luôn. Có xung đột thì nó in danh sách theo mục 2 và dừng giữa chừng.
4. **Gộp xung đột** theo cột cuối của bảng ở mục 2. Đợt có đổi `scripts/cloud-setup.sh` thì xem ghi chú về `SETUP_VERSION` ở mục 7. Phần của app ghi lại ở bước 1 thì áp lại sau khi lấy bản mẫu (`git show <commit của app> -- <file> | git apply -3`). Xong thì:
   - chạy `python3 scripts/foundation.py`;
   - `git add` từng file;
   - chạy `git commit` (lời commit đã soạn sẵn).
5. **Kiểm** trong app:
   - `scripts/tests/run.sh`;
   - `scripts/check.sh` khi đợt có code;
   - file test mới của mẫu nhắc tới file chỉ có ở mẫu (ví dụ thẻ của `MainActivity`): sửa app cho khớp, hoặc ghi lại cho người quyết.
6. **Đẩy nhánh, mở PR** trong repo app.
   - Mô tả PR ghi khoảng commit của dự án mẫu và cách gộp từng file xung đột (lấy bản mẫu, gộp tay giữ gì, áp lại gì).
   - Ghi thêm phần riêng của app bị thay theo quyết định của dự án mẫu (ví dụ ADR của app bị ADR mới của mẫu thay).
   - Người phụ trách app duyệt. Merge bằng `scripts/merge-if-green.sh` của chính nhánh đồng bộ, chạy trong worktree của nhánh đó: khi đợt đồng bộ đổi `pr-check.yml`, bản script trên nhánh chính của app còn đòi kiểm tra cũ và chờ mãi.

## 5. Phần lớn: giữ lại rồi đưa vào bằng plan

Phần thay gần hết một module, cần SDK hay quyền mới, hay đổi ảnh chuẩn screenshot (ví dụ bộ quảng cáo theo AdsKit) thì không gộp trong một đợt thường:

1. Đợt thường: thêm `hold=<đường dẫn> <base hiện tại>` vào `docs/template-sync.txt` trước khi chạy.
2. Soạn ticket và plan rủi ro cao trong app. Spec của app phải được duyệt cho SDK, quyền và vị trí mới.
3. Người (hay AI có người theo dõi) chạy `scripts/template-sync.sh --app ../<app> --take <đường dẫn>`, gộp xung đột, đẩy nhánh `template-sync/take-...`.
4. Plan ghi nhánh đó là điểm bắt đầu. Phần còn lại (khai báo lại vị trí của app, nối vào `:app`, test, ảnh chuẩn) phiên tự chạy làm như task thường.

## 6. Giá trị riêng của app nằm trong file của dự án mẫu

Lấy bản mẫu thì các giá trị này về giá trị của mẫu. Đặt lại sau mỗi đợt, cho tới khi dự án mẫu chuyển chúng ra chỗ cấu hình riêng:

| Giá trị | File | Ở dự án mẫu |
| --- | --- | --- |
| Hạn chót cả lượt kịch bản emulator, `DEVICE_SCENARIOS_BUDGET` | `scripts/device-scenario.py`, `scripts/device-session.sh`, `docs/ops/test-sessions.md` | 600 giây; camera-pose dùng 1800 (từ T-076, trước đó 1500) |
| Camera sau của emulator trong `device-session`, `emulator-options` | `.github/workflows/device-session.yml` (bước Kịch bản chuẩn trên emulator), câu về camera ở `docs/ops/test-sessions.md` mục 4 | `-camera-back none`; camera-pose dùng `-camera-back emulated` (kịch bản camera chạy trên CI) |
| Bí mật Remote Config staging cho kiểm tra thiết lập (`REMOTE_CONFIG_CREDENTIALS: ${{ secrets.STAGING_REMOTE_CONFIG_CREDENTIALS }}` ở bước kiểm tra thiết lập) | `.github/workflows/env-canary.yml`, `.github/workflows/start-run.yml`, `docs/ops/staging-and-runs.md` mục 1 | Không có; camera-pose dùng cho `scripts/setup-check-app.sh` |
| Thời hạn của job có kiểm tra thiết lập, `timeout-minutes` | `.github/workflows/env-canary.yml` (job `setup-check`), `.github/workflows/start-run.yml` (job `check`) | 5 phút; camera-pose dùng 15 và 10 (kiểm catalog tư thế) |
| Ô của bảng khả năng theo kết quả thử của app (ví dụ `workflow-push` sau khi `scripts/push-probe.sh` qua hai phiên liền), cùng ngày và lần chạy ở cột Ghi chú | `docs/ops/session-capabilities.md` | `thử` ở cả ba cách mở; app tự điền |

Thấy thêm giá trị như vậy thì thêm vào bảng này qua issue `from-app`.

Kiểm tra và test riêng của app không đặt trong file của mẫu. Mẫu có hai chỗ móc, gọi file của app nếu có:

- `scripts/setup-check-app.sh`: `scripts/setup-check.sh` nạp file này ở cuối. File dùng được `ok`, `problem`, `note`, `http_status` (hỏi lại khi 000, 429, 5xx), `cdn_hint`, `$STAGING`, `$work`, `$base_url`. Bí mật riêng mà file cần thì thêm vào bước kiểm tra thiết lập của hai workflow (bảng trên).
- `scripts/tests/app.sh`: `scripts/tests/run.sh` nạp file này trước dòng tổng kết. File dùng `expect`, `$work` và các bản giả của `run.sh`; lỗi tính vào cùng tổng. Test của `setup-check.sh` trong `run.sh` đặt `SETUP_CHECK_APP_CHECKS=none`, nên test của `setup-check-app.sh` nằm trong `app.sh`.

## 7. Lưu ý

- Banner cố định (cả lớp banner của thanh đáy), MREC và banner thu gọn do `:core:ads` tải lại theo Remote Config (`time_reload_banner`, `time_reload_banner_mrec`, `time_reload_banner_collap`), không theo tự làm mới của đơn vị. Đợt đồng bộ đưa thay đổi này về app thì người ở app kiểm và tắt tự làm mới trên từng đơn vị production của AdMob và MAX trước bản phát hành sau (`ads-guide.md`, mục 9), vì mã thử ở staging không cho thấy lỗi làm mới hai lần.
- Chữ của module mẫu: `values/` là tiếng Anh, `values-vi/` là tiếng Việt. Android chọn chuỗi theo từng thư mục ngôn ngữ, nên một bản ở `values/` không đè bản ở `values-vi/`.
  - Quy tắc cho mọi app: chuỗi nào app đổi thì đặt giá trị của app ở cả `values/` và `values-vi/` của module chứa chuỗi đó: bản ngôn ngữ mặc định của app (thường là tiếng Anh) ở `values/`, bản tiếng Việt ở `values-vi/`; app chỉ một ngôn ngữ đặt cùng chữ ở cả hai (`brand_tagline` ở `:core:designsystem`; chuỗi của `:app` trong `app/src/main/res/values*/strings.xml`; chuỗi `ads_*` theo `docs/ops/ads-guide.md`, mục 5, bước 4). Đợt đồng bộ gộp ba chiều các tệp này như mọi tệp app cùng sửa với mẫu.
  - App chỉ tiếng Anh thêm `androidResources { localeFilters += listOf("en") }` vào `android { }` của `app/build.gradle.kts`, để bản build bỏ mọi `values-vi/`, kể cả của module mẫu thêm sau này; với bộ lọc, chuỗi app đổi chỉ cần ở `values/` (riêng tệp chuỗi của `:app`, xem ý dưới về lint). Bộ lọc bỏ cả tài nguyên ngôn ngữ khác của mọi thư viện (Play Services, Material, AppCompat, Compose), chấp nhận được với app một ngôn ngữ; `:core:ads` chỉ có `values/`. Bộ lọc cũng áp vào unit test của `:app`: ảnh chuẩn `NetworkLostScreen_default_light`, `NetworkLostScreen_default_dark` (ghim tiếng Việt, ngôn ngữ của thiết kế) vẽ ra tiếng Anh và lệch; ghi lại hai ảnh này trong plan của PR đặt bộ lọc; sau mỗi đợt đồng bộ đổi hai ảnh này, giải xung đột bằng bản nào cũng được, rồi ghi lại hai ảnh bằng lệnh ghi ảnh của `:app` (`CLAUDE.md`, mục Lệnh), chỉ giữ hai ảnh đó; đợt đồng bộ cần plan thì theo `docs/ops/to-app.md`, mục 5.
  - App chỉ tiếng Việt: trên máy để ngôn ngữ khác, chuỗi của mẫu mà app chưa đổi hiện tiếng Anh. Muốn tiếng Việt ở đó thì áp quy tắc trên cho chuỗi đó (đặt tiếng Việt ở `values/` của module chứa chuỗi).
  - `:app` của mẫu có `app/src/main/res/values-vi/strings.xml`, nên lint đòi bản `vi` cho mọi chuỗi trong tệp chuỗi khác của app trong `:app` mà không mang `tools:ignore="MissingTranslation"` (ví dụ `ads_strings.xml`), kể cả khi đặt `localeFilters`: thêm bản ở `values-vi/` (app chỉ một ngôn ngữ: cùng chữ).
  - `values-en/` của `:app` không còn; bản tiếng Anh app đã sửa ở đó chuyển sang `values/` (app đã sửa tệp đó gặp xung đột sửa và xóa khi đồng bộ: giữ việc xóa, đưa chữ của app sang `values/`).
  - Không xóa các tệp `values-vi/` của mẫu: đợt đồng bộ sau mà mẫu sửa tệp đó thì git báo xung đột sửa và xóa, và module mới của mẫu lại mang `values-vi/` vào.
  - Sau mỗi đợt đồng bộ, đối chiếu từng tệp `values/`, `values-vi/` mới hay đổi của module mẫu với các chuỗi app đã đổi: tệp mới vào không gây xung đột mà có thể chứa chuỗi app cần đổi.
  - Đợt đồng bộ đổi ảnh chuẩn có chữ của module mẫu.
- Tài liệu lấy từ dự án mẫu dẫn số bài học của dự án mẫu (`L-0NN`), khác số trong sổ bài học của app. Tra ở `docs/lessons.md` của repo dự án mẫu.
- Bảng ở mục 2 khớp với `NEVER_SYNCED` và `TEMPLATE_OWNED` trong `scripts/template-sync.sh`. Đổi một chỗ thì đổi cả hai.
- `SETUP_VERSION` của `scripts/cloud-setup.sh`: đợt đồng bộ có đổi file này, kể cả khi không xung đột, mà app đã từng tăng số riêng, thì đặt số bằng số lớn nhất app từng dùng cộng một, rồi dán lại script (`docs/ops/staging-and-runs.md`, mục 2.1). Hai bên cùng tăng lên một số với nội dung khác thì git gộp êm, số không đổi, và `cloud-env-check.sh` không báo phải dán lại.
